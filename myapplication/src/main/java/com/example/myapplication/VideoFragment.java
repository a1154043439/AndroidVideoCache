package com.example.myapplication;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.VideoView;

import com.danikula.videocache.CacheListener;
import com.danikula.videocache.FileProxyCacheServer;
import com.danikula.videocache.HttpProxyCacheServer;
import java.io.File;
import java.net.URL;

/**
 * Created by netease on 16/8/10.
 */
public class VideoFragment extends Fragment implements CacheListener {
    private static final String LOG_TAG = "VideoFragment";
    private final VideoProgressUpdater updater = new VideoProgressUpdater();
    private String url ;
    private String cachePath;

    private ImageView cacheStatusImageView;

    private VideoView videoView;

    private SeekBar progressBar;
   /* public static Fragment build(String url){
        VideoFragment videoFragment = new VideoFragment();
        Bundle args = new Bundle();
        args.putString("url",url);
        videoFragment.setArguments(args);
        return  videoFragment;
    }*/
    private void checkCachedState() {
        FileProxyCacheServer proxy = App.getProxy(getActivity());
        boolean fullyCached = proxy.isCached(url);
        setCachedState(fullyCached);
    }

    private void startVideo() {
        /*HttpProxyCacheServer proxy = App.getProxy(getActivity());
        proxy.registerCacheListener(this, url);
        String proxyUrl = proxy.getProxyUrl(url);
        Log.d("proxyUrl",proxyUrl);
        videoView.setVideoPath(proxyUrl);*/
        FileProxyCacheServer proxy = App.getProxy(getActivity());
        proxy.registerCacheListener(this, url);
        String proxyUrl = proxy.getProxyUrl(url);
        Log.d("proxyUrl",proxyUrl);
        videoView.setVideoPath(proxyUrl);
        //videoView.setVideoURI(Uri.parse("http://112.253.22.157/17/z/z/y/u/zzyuasjwufnqerzvyxgkuigrkcatxr/hc.yinyuetai.com/D046015255134077DDB3ACA0D7E68D45.flv"));
        videoView.start();
    }
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root =  inflater.inflate(R.layout.fragment_video, container, false);
        cacheStatusImageView = (ImageView)root.findViewById(R.id.cacheStatusImageView);
        videoView =(VideoView)root.findViewById(R.id.videoView);
        progressBar = (SeekBar)root.findViewById(R.id.progressBar);
        Video video  = Video.ORANGE_1;
        url = "/storage/emulated/0/demo2.flv";
        cachePath = video.getCacheFile(getActivity()).getAbsolutePath();
        Log.d("cachePath",cachePath);
        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int videoPosition = videoView.getDuration() * progressBar.getProgress() / 100;
                videoView.seekTo(videoPosition);
            }
        });
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        checkCachedState();
        startVideo();
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onResume() {
        super.onResume();
        updater.start();

    }

    @Override
    public void onPause() {
        super.onPause();
        updater.stop();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        videoView.stopPlayback();
        App.getProxy(getActivity()).unregisterCacheListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    @Override
    public void onCacheAvailable(File cacheFile, String url, int percentsAvailable) {
        progressBar.setSecondaryProgress(percentsAvailable);
        setCachedState(percentsAvailable == 100);
        Log.d(LOG_TAG, String.format("onCacheAvailable. percents: %d, file: %s, url: %s", percentsAvailable, cacheFile, url));
    }
    private void setCachedState(boolean cached) {
        int statusIconId = cached ? R.drawable.ic_cloud_done : R.drawable.ic_cloud_download;
        cacheStatusImageView.setImageResource(statusIconId);
    }
    private void updateVideoProgress() {
        int videoProgress = videoView.getCurrentPosition() * 100 / videoView.getDuration();
        progressBar.setProgress(videoProgress);
    }
    //更新进度条
    private final class VideoProgressUpdater extends Handler {

        public void start() {
            sendEmptyMessage(0);
        }

        public void stop() {
            removeMessages(0);
        }

        @Override
        public void handleMessage(Message msg) {
            updateVideoProgress();
            sendEmptyMessageDelayed(0, 500);
        }
    }
}
