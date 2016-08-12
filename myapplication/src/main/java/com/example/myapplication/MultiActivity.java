package com.example.myapplication;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

/**
 * Created by netease on 16/8/10.
 */
public class MultiActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiple_videos);
        if (savedInstanceState == null) {
                addVideoFragment( R.id.videoContainer0);
                addVideoFragment( R.id.videoContainer1);
                addVideoFragment( R.id.videoContainer2);
                addVideoFragment( R.id.videoContainer3);
            }
        }

    private void addVideoFragment( int containerViewId) {
        getSupportFragmentManager()
                .beginTransaction()
                .add(containerViewId, new VideoFragment())
                .commit();
    }
}
