package com.danikula.videocache.file;

import com.danikula.videocache.Cache;
import com.danikula.videocache.ProxyCacheException;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * 文件缓存，实现了缓存接口,这里的设计缓存替换原则与缓存实现分开
 * 并不涉及到如何实现缓存文件删除的逻辑，当内容超过缓存的限制时，由
 * diskUsage接口实现的方法来打扫缓存空间
 * {@link Cache} that uses file for storing data.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class FileCache implements Cache {

    private static final String TEMP_POSTFIX = ".download";

    private final DiskUsage diskUsage;
    public File file;
    private RandomAccessFile dataFile;

    public FileCache(File file) throws ProxyCacheException {
        this(file, new UnlimitedDiskUsage());
    }

    public FileCache(File file, DiskUsage diskUsage) throws ProxyCacheException {
        try {
            if (diskUsage == null) {
                throw new NullPointerException();
            }
            this.diskUsage = diskUsage;
            File directory = file.getParentFile();
            //创建目录
            Files.makeDir(directory);
            //检查文件是否已经缓存完成，若是已完成，则是对应的文件名，未完成时，就保存.download的文件名
            boolean completed = file.exists();
            this.file = completed ? file : new File(file.getParentFile(), file.getName() + TEMP_POSTFIX);
            //创建随机读取的文件，若是已完成的只读，未完成的可读写
            this.dataFile = new RandomAccessFile(this.file, completed ? "r" : "rw");
        } catch (IOException e) {
            throw new ProxyCacheException("Error using file " + file + " as disc cache", e);
        }
    }

    @Override
    public synchronized int available() throws ProxyCacheException {
        try {
            //返回已缓存的可用数据
            return (int) dataFile.length();
        } catch (IOException e) {
            throw new ProxyCacheException("Error reading length of file " + file, e);
        }
    }

    @Override
    public synchronized int read(byte[] buffer, long offset, int length) throws ProxyCacheException {
        try {
            //先定位到偏移量，然后从指定的偏移量开始读取length的长度到buffer中
            dataFile.seek(offset);
            return dataFile.read(buffer, 0, length);
        } catch (IOException e) {
            String format = "Error reading %d bytes with offset %d from file[%d bytes] to buffer[%d bytes]";
            throw new ProxyCacheException(String.format(format, length, offset, available(), buffer.length), e);
        }
    }

    @Override
    public synchronized void append(byte[] data, int length) throws ProxyCacheException {
        try {
            //如果已经完成，不允许添加数据
            if (isCompleted()) {
                throw new ProxyCacheException("Error append cache: cache file " + file + " is completed!");
            }
            //未完成的情况下，先定位到文件最后的位置，也就是可获得的所有文件长度的位置
            //然后从data数组中写length长度到datafile中
            dataFile.seek(available());
            dataFile.write(data, 0, length);
        } catch (IOException e) {
            String format = "Error writing %d bytes to %s from buffer with size %d";
            throw new ProxyCacheException(String.format(format, length, dataFile, data.length), e);
        }
    }

    @Override
    public synchronized void close() throws ProxyCacheException {
        try {
            //datafile关闭
            dataFile.close();
            //更新diskUsage接口信息，其中实现diskUsage接口的具体类的TotalSizeLruDiskUsage的touch方法是
            //修改文件的最后更新时间，处理文件空间，也就是当内容数量或者尺寸超过缓存限制的时候，删除文件列表首元素
            diskUsage.touch(file);
        } catch (IOException e) {
            throw new ProxyCacheException("Error closing file " + file, e);
        }
    }

    @Override
    public synchronized void complete() throws ProxyCacheException {
        if (isCompleted()) {
            return;
        }

        //完成则修改文件名，将data文件变成只读权限
        close();

        String fileName = file.getName().substring(0, file.getName().length() - TEMP_POSTFIX.length());
        File completedFile = new File(file.getParentFile(), fileName);
        boolean renamed = file.renameTo(completedFile);
        if (!renamed) {
            throw new ProxyCacheException("Error renaming file " + file + " to " + completedFile + " for completion!");
        }
        file = completedFile;
        try {
            dataFile = new RandomAccessFile(file, "r");
        } catch (IOException e) {
            throw new ProxyCacheException("Error opening " + file + " as disc cache", e);
        }
    }

    @Override
    public synchronized boolean isCompleted() {
        //不是临时文件则表示缓冲完成
        return !isTempFile(file);
    }

    /**
     * Returns file to be used fo caching. It may as original file passed in constructor as some temp file for not completed cache.
     *
     * 返回用来缓存的文件，会得到完整文件或者是临时文件
     * @return file for caching.
     */
    public File getFile() {
        return file;
    }

    private boolean isTempFile(File file) {
        //检查是否带有临时文件的后缀
        return file.getName().endsWith(TEMP_POSTFIX);
    }

}
