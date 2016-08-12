package com.danikula.videocache.file;

import java.io.File;
import java.io.IOException;

/**
 * 无限制的磁盘使用
 * Unlimited version of {@link DiskUsage}.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class UnlimitedDiskUsage implements DiskUsage {

    @Override
    public void touch(File file) throws IOException {
        // do nothing
    }
}
