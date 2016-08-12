package com.danikula.videocache;

import android.text.TextUtils;

import com.danikula.videocache.file.FileCache;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import static com.danikula.videocache.ProxyCacheUtils.DEFAULT_BUFFER_SIZE;

/**
 * {@link ProxyCache} that read http url and writes data to {@link Socket}
 *
 * 代理缓存的http实现
 * source指定了httpUrlSource
 * cache指定了fileCache
 * @author Alexey Danilov (danikula@gmail.com).
 */
class HttpProxyCache extends ProxyCache {

    private static final float NO_CACHE_BARRIER = .2f;

    private final HttpUrlSource source;
    private final FileCache cache;
    private CacheListener listener;

    public HttpProxyCache(HttpUrlSource source, FileCache cache) {
        super(source, cache);
        this.cache = cache;
        this.source = source;
    }

    //注册缓存监听器，当缓存发生变化的时候自动回调
    public void registerCacheListener(CacheListener cacheListener) {
        this.listener = cacheListener;
    }

    //处理请求
    public void processRequest(GetRequest request, Socket socket) throws IOException, ProxyCacheException {
        //获得socket的输出流
        OutputStream out = new BufferedOutputStream(socket.getOutputStream());
        //获得响应头
        String responseHeaders = newResponseHeaders(request);
        //将响应头先写到输出流中
        out.write(responseHeaders.getBytes("UTF-8"));

        long offset = request.rangeOffset;
        //再写响应体的内容，如果这个请求的内容使用缓存，就从缓存中取得数据，否则不使用缓存取数据
        if (isUseCache(request)) {
            responseWithCache(out, offset);
        } else {
            responseWithoutCache(out, offset);
        }
    }

    /**
     * 是否使用缓存
     * 当文件长度不大于0 或者 请求不是部分文件的请求 或者，请求的偏移量小于等于缓存可以获取的数量+文件长度*0.2
     * 返回true,也就是使用缓存
     * @param request
     * @return
     * @throws ProxyCacheException
     */
    private boolean isUseCache(GetRequest request) throws ProxyCacheException {
        int sourceLength = source.length();
        boolean sourceLengthKnown = sourceLength > 0;
        int cacheAvailable = cache.available();
        // do not use cache for partial requests which too far from available cache. It seems user seek video.
        return !sourceLengthKnown || !request.partial || request.rangeOffset <= cacheAvailable + sourceLength * NO_CACHE_BARRIER;
    }

    /**
     * 根据request来制作响应头
     * @param request
     * @return
     * @throws IOException
     * @throws ProxyCacheException
     */
    private String newResponseHeaders(GetRequest request) throws IOException, ProxyCacheException {
        //获取source的资源类型
        String mime = source.getMime();
        boolean mimeKnown = !TextUtils.isEmpty(mime);
        //根据缓存是否完成判断响应长度是缓存的已缓存的内容还是源数据的长度
        int length = cache.isCompleted() ? cache.available() : source.length();
        boolean lengthKnown = length >= 0;
        //根据request是不是partial判断content长度是长度-偏移量还是我们能获得的长度
        long contentLength = request.partial ? length - request.rangeOffset : length;
        boolean addRange = lengthKnown && request.partial;
        return new StringBuilder()
                .append(request.partial ? "HTTP/1.1 206 PARTIAL CONTENT\n" : "HTTP/1.1 200 OK\n")
                .append("Accept-Ranges: bytes\n")
                .append(lengthKnown ? String.format("Content-Length: %d\n", contentLength) : "")
                .append(addRange ? String.format("Content-Range: bytes %d-%d/%d\n", request.rangeOffset, length - 1, length) : "")
                .append(mimeKnown ? String.format("Content-Type: %s\n", mime) : "")
                .append("\n") // headers end
                .toString();
    }

    private void responseWithCache(OutputStream out, long offset) throws ProxyCacheException, IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int readBytes;
        //从缓存中读数据到buffer中，再从buffer中写数据到输出流中
        while ((readBytes = read(buffer, offset, buffer.length)) != -1) {
            out.write(buffer, 0, readBytes);
            offset += readBytes;
        }
        out.flush();
    }

    private void responseWithoutCache(OutputStream out, long offset) throws ProxyCacheException, IOException {
        //新建一个无缓存的源
        HttpUrlSource newSourceNoCache = new HttpUrlSource(this.source);
        try {
            //打开数据源，从中读取数据到buffer中，再从buffer中写到输出流中
            newSourceNoCache.open((int) offset);
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int readBytes;
            while ((readBytes = newSourceNoCache.read(buffer)) != -1) {
                out.write(buffer, 0, readBytes);
                //更新offset为原来的offset+readBytes
                offset += readBytes;
            }
            out.flush();
        } finally {
            newSourceNoCache.close();
        }
    }

    //复写父类的缓存可用比例变化的回调方法，这里通知所有的监听器新的可用的比例
    @Override
    protected void onCachePercentsAvailableChanged(int percents) {
        if (listener != null) {
            listener.onCacheAvailable(cache.file, source.getUrl(), percents);
        }
    }
}
