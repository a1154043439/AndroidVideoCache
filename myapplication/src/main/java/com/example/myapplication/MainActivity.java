package com.example.myapplication;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

/**
 * Created by netease on 16/8/10.
 */
public class MainActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_video);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.containerView, new VideoFragment())
                    .commit();
        }
    }
}
