package com.danikula.videocache.file;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 使用LRU策略来管理缓存
 * {@link DiskUsage} that uses LRU (Least Recently Used) strategy to trim cache.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
abstract class LruDiskUsage implements DiskUsage {

    private static final String LOG_TAG = "ProxyCache";
    //创建线程池，线程池只包含一个线程
    private final ExecutorService workerThread = Executors.newSingleThreadExecutor();

    //最后一次接触（读取或者修改）的时间
    @Override
    public void touch(File file) throws IOException {
        //由新线程执行一个新的任务，用来清理文件缓存
        workerThread.submit(new TouchCallable(file));
    }

    private void touchInBackground(File file) throws IOException {
        //设置最后修改时间
        Files.setLastModifiedNow(file);
        List<File> files = Files.getLruListFiles(file.getParentFile());
        trim(files);
    }

    //抽象类用来判断该文件是否被缓存空间接收
    protected abstract boolean accept(File file, long totalSize, int totalCount);

    //缩减缓存空间大小时，使用的是最近最少未使用的算法
    private void trim(List<File> files) {
        long totalSize = countTotalSize(files);
        int totalCount = files.size();
        for (File file : files) {
            boolean accepted = accept(file, totalSize, totalCount);
            if (!accepted) {
                long fileSize = file.length();
                boolean deleted = file.delete();
                if (deleted) {
                    totalCount--;
                    totalSize -= fileSize;
                    Log.i(LOG_TAG, "Cache file " + file + " is deleted because it exceeds cache limit");
                } else {
                    Log.e(LOG_TAG, "Error deleting file " + file + " for trimming cache");
                }
            }
        }
    }

    //计算总共的尺寸大小
    private long countTotalSize(List<File> files) {
        long totalSize = 0;
        for (File file : files) {
            totalSize += file.length();
        }
        return totalSize;
    }

    //可以在后台被回调的接口，在后头执行touchinbackground方法
    private class TouchCallable implements Callable<Void> {

        private final File file;

        public TouchCallable(File file) {
            this.file = file;
        }

        @Override
        public Void call() throws Exception {
            touchInBackground(file);
            return null;
        }
    }
}
