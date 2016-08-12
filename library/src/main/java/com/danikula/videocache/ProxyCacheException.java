package com.danikula.videocache;

/**
 * 在缓存中会出现的异常
 * Indicates any error in work of {@link ProxyCache}.
 *
 * @author Alexey Danilov
 */
public class ProxyCacheException extends Exception {

    public ProxyCacheException(String message) {
        super(message);
    }

    public ProxyCacheException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProxyCacheException(Throwable cause) {
        super(cause);
    }
}
