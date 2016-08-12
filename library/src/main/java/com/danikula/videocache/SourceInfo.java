package com.danikula.videocache;

/**
 * Stores source's info.
 * 存储的数据信息
 * 在HTTP中，MIME类型被定义在Content-Type header中
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class SourceInfo {

    public final String url;
    public final int length;
    public final String mime;

    public SourceInfo(String url, int length, String mime) {
        this.url = url;
        this.length = length;
        this.mime = mime;
    }

    @Override
    public String toString() {
        return "SourceInfo{" +
                "url='" + url + '\'' +
                ", length=" + length +
                ", mime='" + mime + '\'' +
                '}';
    }
}
