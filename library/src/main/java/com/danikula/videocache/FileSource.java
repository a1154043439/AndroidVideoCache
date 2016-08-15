package com.danikula.videocache;

import android.text.TextUtils;
import android.util.Log;

import com.danikula.videocache.sourcestorage.SourceInfoStorage;
import com.danikula.videocache.sourcestorage.SourceInfoStorageFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.file.Files;

import static com.danikula.videocache.Preconditions.checkNotNull;
import static com.danikula.videocache.ProxyCacheUtils.DEFAULT_BUFFER_SIZE;
import static com.danikula.videocache.ProxyCacheUtils.LOG_TAG;

/**
 * 这里将文件作为缓存的数据源，如果缓存没有数据，则来这里获取数据，测试逻辑是从文件
 * 按流到这个文件看能否打开
 * Created by netease on 16/8/12.
 */
public class FileSource implements Source {
    private SourceInfo sourceInfo;
    private InputStream inputStream;
    private final SourceInfoStorage sourceInfoStorage;
    public FileSource(String url) {
        this(url, SourceInfoStorageFactory.newEmptySourceInfoStorage());
    }

    public FileSource(String url, SourceInfoStorage sourceInfoStorage) {
        this.sourceInfoStorage = checkNotNull(sourceInfoStorage);
        SourceInfo sourceInfo = sourceInfoStorage.get(url);
        this.sourceInfo = sourceInfo != null ? sourceInfo :
                new SourceInfo(url, Integer.MIN_VALUE, ProxyCacheUtils.getLocalMime(url));

    }

    public FileSource(FileSource source) {
        this.sourceInfo = source.sourceInfo;
        this.sourceInfoStorage = source.sourceInfoStorage;
    }

    //打开文件
    @Override
    public void open(int offset) throws ProxyCacheException {
        try {
            inputStream = new BufferedInputStream(new FileInputStream(sourceInfo.url),DEFAULT_BUFFER_SIZE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int length() throws ProxyCacheException {
        if (sourceInfo.length == Integer.MIN_VALUE) {
            fetchContentInfo();
        }
        return sourceInfo.length;
    }

    @Override
    public int read(byte[] buffer) throws ProxyCacheException {
        if (inputStream == null) {
            throw new ProxyCacheException("Error reading data from " + sourceInfo.url + ": connection is absent!");
        }
        try {
            //从输入流中读取数据到buffer中
            return inputStream.read(buffer, 0, buffer.length);
        } catch (InterruptedIOException e) {
            throw new InterruptedProxyCacheException("Reading source " + sourceInfo.url + " is interrupted", e);
        } catch (IOException e) {
            throw new ProxyCacheException("Error reading data from " + sourceInfo.url, e);
        }

    }

    @Override
    public void close() throws ProxyCacheException {
        ProxyCacheUtils.close(inputStream);
    }
    /**
     * 获取文件内容信息,可以获得url,文件大小，文件类型等信息
     * @throws ProxyCacheException
     */
    private void fetchContentInfo() throws ProxyCacheException {
        Log.d(LOG_TAG, "Read content info from " + sourceInfo.url);

        try {
            File sourceFile = new File(sourceInfo.url);
            float length = sourceFile.length();
            String mime= "flv";
            //构造新的源数据类型
            this.sourceInfo = new SourceInfo(sourceInfo.url, (int)length, mime);
            //将url和对应的sourceInfo存储下来,这里使用了数据库存储
            this.sourceInfoStorage.put(sourceInfo.url, sourceInfo);
            Log.i(LOG_TAG, "Source info fetched: " + sourceInfo);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error fetching info from " + sourceInfo.url, e);
        }
    }

    public synchronized String getMime() throws ProxyCacheException {
        if (TextUtils.isEmpty(sourceInfo.mime)) {
            fetchContentInfo();
        }
        return sourceInfo.mime;
    }

    public String getUrl() {
        return sourceInfo.url;
    }

    @Override
    public String toString() {
        return "HttpUrlSource{sourceInfo='" + sourceInfo + "}";
    }
}
