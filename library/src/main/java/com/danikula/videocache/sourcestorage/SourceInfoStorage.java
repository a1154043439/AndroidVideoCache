package com.danikula.videocache.sourcestorage;

import com.danikula.videocache.SourceInfo;

/**
 * Storage for {@link SourceInfo}.
 * 用来存储源数据的接口所有实现必须实现这个接口
 * @author Alexey Danilov (danikula@gmail.com).
 */
public interface SourceInfoStorage {

    SourceInfo get(String url);

    void put(String url, SourceInfo sourceInfo);

    void release();
}
