package com.example.myapplication;

import android.app.Application;
import android.content.Context;

import com.danikula.videocache.FileProxyCacheServer;
import com.danikula.videocache.HttpProxyCacheServer;

/**
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class App extends Application {

    private FileProxyCacheServer proxy;

    //单例的代理服务器
    public static FileProxyCacheServer getProxy(Context context) {
        //获得application的上下文
        App app = (App) context.getApplicationContext();
        return app.proxy == null ? (app.proxy = app.newProxy()) : app.proxy;
    }

    private FileProxyCacheServer newProxy() {
        //这里按照自己的需求定制HttpProxyCacheServer
        return new FileProxyCacheServer(this);
    }
}
