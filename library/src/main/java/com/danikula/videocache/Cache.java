package com.danikula.videocache;

/**
 * Cache for proxy.
 *
 * 用作代理缓存的接口信息
 * 当然包括缓存的通用操作:可用的数据，读取，附加(写),关闭,完成，是否完成
 * @author Alexey Danilov (danikula@gmail.com).
 */
public interface Cache {

    int available() throws ProxyCacheException;

    int read(byte[] buffer, long offset, int length) throws ProxyCacheException;

    void append(byte[] data, int length) throws ProxyCacheException;

    void close() throws ProxyCacheException;

    void complete() throws ProxyCacheException;

    boolean isCompleted();
}
