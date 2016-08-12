package com.danikula.videocache.file;

import java.io.File;
import java.io.IOException;

/**
 * 声明缓冲会用多少内存,方法表示从哪个文件可以获得缓存的信息
 * Declares how {@link FileCache} will use disc space.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public interface DiskUsage {

    void touch(File file) throws IOException;

}
