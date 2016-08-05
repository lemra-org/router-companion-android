package org.rm3l.ddwrt.tasker;

import android.app.Application;

import com.twofortyfouram.log.Lumberjack;

/**
 * Implements an application object for the plug-in.
 */
/*
 * This application is non-essential for the plug-in's operation; it simply enables debugging
 * options globally for the app.
 */
public class DDWRTCompanionTaskerPluginApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Lumberjack.init(getApplicationContext());
    }
}
