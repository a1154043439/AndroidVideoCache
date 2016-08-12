package com.danikula.videocache;

import java.io.File;

/**
 * Listener for cache availability.
 * 用来获取缓存是否可用的接口
 * @author Egor Makovsky (yahor.makouski@gmail.com)
 * @author Alexey Danilov (danikula@gmail.com).
 */
public interface CacheListener {

    void onCacheAvailable(File cacheFile, String url, int percentsAvailable);
}
