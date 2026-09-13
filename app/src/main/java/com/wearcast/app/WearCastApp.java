package com.wearcast.app;

import android.app.Application;

import com.wearcast.app.network.ApiClient;

public class WearCastApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ApiClient.init(this);
    }
}
