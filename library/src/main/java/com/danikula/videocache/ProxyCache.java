package com.danikula.videocache;

import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;

import static com.danikula.videocache.Preconditions.checkNotNull;
import static com.danikula.videocache.ProxyCacheUtils.LOG_TAG;

/**
 * Proxy for {@link Source} with caching support ({@link Cache}).
 * <p/>
 * Can be used only for sources with persistent data (that doesn't change with time).
 * Method {@link #read(byte[], long, int)} will be blocked while fetching data from source.
 * Useful for streaming something with caching e.g. streaming video/audio etc.
 *
 * 用来支持缓存的代理，可以用来持久化存储数据，read方法会在获取数据的时候阻塞
 * 主要用来缓存音视频流，实现边下边播
 * @author Alexey Danilov (danikula@gmail.com).
 */
class ProxyCache {

    /**
     * source和cache这两个变量最重要，指定了数据源和缓存对象
     */
    private static final int MAX_READ_SOURCE_ATTEMPTS = 1;

    private final Source source;
    private final Cache cache;
    //
    private final Object wc = new Object();
    //缓存结束的锁
    private final Object stopLock = new Object();
    private final AtomicInteger readSourceErrorsCount;
    private volatile Thread sourceReaderThread;
    private volatile boolean stopped;
    private volatile int percentsAvailable = -1;

    public ProxyCache(Source source, Cache cache) {
        this.source = checkNotNull(source);
        this.cache = checkNotNull(cache);
        //初始化读取数据错误的次数
        this.readSourceErrorsCount = new AtomicInteger();
    }

    public int read(byte[] buffer, long offset, int length) throws ProxyCacheException {
        //断言缓存，用于验证
        ProxyCacheUtils.assertBuffer(buffer, offset, length);

        //如果缓存未完成而且尚未停止，可获得的字节数小于偏移量+长度
        while (!cache.isCompleted() && cache.available() < (offset + length) && !stopped) {
            //异步的从源读取数据;
            readSourceAsync();
            //等待源数据
            waitForSourceData();
            //检查读取源数据错误的次数
            checkReadSourceErrorsCount();
        }
        //出循环表示所要求的内容是缓存好的，那么直接从缓存中取到buffer就好了
        int read = cache.read(buffer, offset, length);
        //如果缓存完成，可以获得的内容是全部的话，记录可获得的状态为100
        if (cache.isCompleted() && percentsAvailable != 100) {
            percentsAvailable = 100;
            onCachePercentsAvailableChanged(100);
        }
        return read;
    }

    private void checkReadSourceErrorsCount() throws ProxyCacheException {
        int errorsCount = readSourceErrorsCount.get();
        //大于最大尝试次数时那直接置0并且抛出错误
        if (errorsCount >= MAX_READ_SOURCE_ATTEMPTS) {
            readSourceErrorsCount.set(0);
            throw new ProxyCacheException("Error reading source " + errorsCount + " times");
        }
    }

    //关闭缓存，如果读数据线程未结束，那么中断线程
    public void shutdown() {
        synchronized (stopLock) {
            Log.d(LOG_TAG, "Shutdown proxy for " + source);
            try {
                stopped = true;
                if (sourceReaderThread != null) {
                    sourceReaderThread.interrupt();
                }
                cache.close();
            } catch (ProxyCacheException e) {
                onError(e);
            }
        }
    }

    /**
     * 异步获取数据源
     * @throws ProxyCacheException
     */
    private synchronized void readSourceAsync() throws ProxyCacheException {
        boolean readingInProgress = sourceReaderThread != null && sourceReaderThread.getState() != Thread.State.TERMINATED;
        if (!stopped && !cache.isCompleted() && !readingInProgress) {
            //启动读取数据源的线程
            sourceReaderThread = new Thread(new SourceReaderRunnable(), "Source reader for " + source);
            sourceReaderThread.start();
        }
    }

    /**
     * 当前线程阻塞1s,用来等待数据源
     * @throws ProxyCacheException
     */
    private void waitForSourceData() throws ProxyCacheException {
        synchronized (wc) {
            try {
                wc.wait(1000);
            } catch (InterruptedException e) {
                throw new ProxyCacheException("Waiting source data is interrupted!", e);
            }
        }
    }

    /**
     * 通知缓存数据变化，
     * @param cacheAvailable 缓存中已经有的文件长度
     * @param sourceAvailable 文件的总长度
     */
    private void notifyNewCacheDataAvailable(long cacheAvailable, long sourceAvailable) {
        onCacheAvailable(cacheAvailable, sourceAvailable);

        //唤醒所有因为数据源不足而阻塞的线程
        synchronized (wc) {
            wc.notifyAll();
        }
    }

    protected void onCacheAvailable(long cacheAvailable, long sourceLength) {
        boolean zeroLengthSource = sourceLength == 0;
        int percents = zeroLengthSource ? 100 : (int) (cacheAvailable * 100 / sourceLength);
        boolean percentsChanged = percents != percentsAvailable;
        boolean sourceLengthKnown = sourceLength >= 0;
        if (sourceLengthKnown && percentsChanged) {
            onCachePercentsAvailableChanged(percents);
        }
        percentsAvailable = percents;
    }

    protected void onCachePercentsAvailableChanged(int percentsAvailable) {
    }

    /**
     * 从数据源读取数据的方法
     */
    private void readSource() {
        int sourceAvailable = -1;
        int offset = 0;
        try {
            //偏移量为缓存已经有的数据的长度
            offset = cache.available();
            //数据源从偏移量的位置打开
            source.open(offset);
            //sourceAvailable为source的长度
            sourceAvailable = source.length();
            //定义一个新的buffer
            byte[] buffer = new byte[ProxyCacheUtils.DEFAULT_BUFFER_SIZE];
            //
            int readBytes;
            //未读完,读取数据到buffer中
            while ((readBytes = source.read(buffer)) != -1) {
                synchronized (stopLock) {
                    if (isStopped()) {
                        return;
                    }
                    //将buffer的数据加到cache中
                    cache.append(buffer, readBytes);
                }
                //偏移量增加了readBytes的长度
                offset += readBytes;
                //通知有新的缓存数据可以获取
                notifyNewCacheDataAvailable(offset, sourceAvailable);
            }
            tryComplete();
        } catch (Throwable e) {
            readSourceErrorsCount.incrementAndGet();
            onError(e);
        } finally {
            closeSource();
            notifyNewCacheDataAvailable(offset, sourceAvailable);
        }
    }

    /**
     * 检验是否完成
     */
    private void tryComplete() throws ProxyCacheException {
        synchronized (stopLock) {
            if (!isStopped() && cache.available() == source.length()) {
                cache.complete();
            }
        }
    }

    //当前线程是否中断或者stop标志位为ture
    private boolean isStopped() {
        return Thread.currentThread().isInterrupted() || stopped;
    }

    //关闭资源
    private void closeSource() {
        try {
            source.close();
        } catch (ProxyCacheException e) {
            onError(new ProxyCacheException("Error closing source " + source, e));
        }
    }

    protected final void onError(final Throwable e) {
        boolean interruption = e instanceof InterruptedProxyCacheException;
        if (interruption) {
            Log.d(LOG_TAG, "ProxyCache is interrupted");
        } else {
            Log.e(LOG_TAG, "ProxyCache error", e);
        }
    }

    //读取源数据的线程，run方法为readSource
    private class SourceReaderRunnable implements Runnable {

        @Override
        public void run() {
            readSource();
        }
    }
}
