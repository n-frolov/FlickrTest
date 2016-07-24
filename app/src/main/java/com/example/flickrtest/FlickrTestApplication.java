package com.example.flickrtest;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

public class FlickrTestApplication extends Application {

    private static FlickrTestApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();

        // This pattern is not exactly recommended, but I have researched it pretty thoroughly,
        // including usage in live apps, and consider it pretty awesome when used correctly.
        instance = this;
    }

    public @NonNull static Context getContext() {
        return instance;
    }

}
