package com.example.arduinoledmemory;

import android.app.Application;
import android.content.res.Resources;

public class MyApplication extends Application {

    private static MyApplication instance;
    public static Resources resources;

    public static MyApplication getInstance() {
        if (instance == null) {
            synchronized(MyApplication.class) {
                if (instance == null)
                    instance = new MyApplication();
            }
        }
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        resources = getResources();
    }

}