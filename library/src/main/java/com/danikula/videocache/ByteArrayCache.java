package com.danikula.videocache;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

/**
 * Simple memory based {@link Cache} implementation.
 * 简单的内存缓存实现，主要用作测试
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class ByteArrayCache implements Cache {

    private volatile byte[] data;
    private volatile boolean completed;

    public ByteArrayCache() {
        this(new byte[0]);
    }

    public ByteArrayCache(byte[] data) {
        this.data = Preconditions.checkNotNull(data);
    }

    @Override
    public int read(byte[] buffer, long offset, int length) throws ProxyCacheException {
        if (offset >= data.length) {
            return -1;
        }
        if (offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Too long offset for memory cache " + offset);
        }
        //  从data的input stream中读length的数据到buffer中
        return new ByteArrayInputStream(data).read(buffer, (int) offset, length);
    }

    @Override
    public int available() throws ProxyCacheException {
        return data.length;
    }

    @Override
    public void append(byte[] newData, int length) throws ProxyCacheException {
        Preconditions.checkNotNull(data);
        Preconditions.checkArgument(length >= 0 && length <= newData.length);

        //新copy一个数组
        byte[] appendedData = Arrays.copyOf(data, data.length + length);
        //将newData加到新数组的后面
        System.arraycopy(newData, 0, appendedData, data.length, length);
        //返回新数组
        data = appendedData;
    }

    @Override
    public void close() throws ProxyCacheException {
    }

    @Override
    public void complete() {
        completed = true;
    }

    @Override
    public boolean isCompleted() {
        return completed;
    }
}
