package com.robotdreams;

import android.app.Application;

import timber.log.Timber;

/**
 *
 */
public class IrPOC extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());
    }
}
