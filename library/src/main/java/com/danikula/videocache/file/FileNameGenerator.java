package com.danikula.videocache.file;

/**
 * Generator for files to be used for caching.
 *
 * 产生文件名称的生成器接口
 * @author Alexey Danilov (danikula@gmail.com).
 */
public interface FileNameGenerator {

    String generate(String url);

}
