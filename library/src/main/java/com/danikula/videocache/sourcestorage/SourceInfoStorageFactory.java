package com.danikula.videocache.sourcestorage;

import android.content.Context;

/**
 * 简单工厂类
 * 用来产生请求的资源信息 和 空资源信息两种内容
 * 默认请求的资源信息存储在数据库中
 * Simple factory for {@link SourceInfoStorage}.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class SourceInfoStorageFactory {

    public static SourceInfoStorage newSourceInfoStorage(Context context) {
        return new DatabaseSourceInfoStorage(context);
    }

    public static SourceInfoStorage newEmptySourceInfoStorage() {
        return new NoSourceInfoStorage();
    }
}
