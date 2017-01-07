package org.rm3l.router_companion.tasker;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.facebook.stetho.Stetho;
import com.twofortyfouram.log.Lumberjack;

import io.fabric.sdk.android.Fabric;
import java.lang.ref.WeakReference;

import static org.rm3l.router_companion.tasker.Constants.*;

/**
 * Implements an application object for the plug-in.
 */
/*
 * This application is non-essential for the plug-in's operation; it simply enables debugging
 * options globally for the app.
 */
public class RouterCompanionTaskerPluginApplication extends Application implements Application.ActivityLifecycleCallbacks {

    private static WeakReference<Activity> mCurrentActivity;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);

        if (BuildConfig.DEBUG) {
//            LeakCanary.install(this);
            Stetho.initializeWithDefaults(this);
        }

        Fabric.with(this, new Crashlytics());
        Crashlytics.setBool("DEBUG", BuildConfig.DEBUG);

        Lumberjack.init(getApplicationContext());
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        unregisterActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        Crashlytics.log(Log.DEBUG, TAG,
                "onActivityCreated: " + activity.getClass().getCanonicalName());
        mCurrentActivity = new WeakReference<>(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        Crashlytics.log(Log.DEBUG, TAG,
                "onActivityStarted: " + activity.getClass().getCanonicalName());
        mCurrentActivity = new WeakReference<>(activity);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        Crashlytics.log(Log.DEBUG, TAG,
                "onActivityResumed: " + activity.getClass().getCanonicalName());
        mCurrentActivity = new WeakReference<>(activity);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        Crashlytics.log(Log.DEBUG, TAG,
                "onActivityPaused: " + activity.getClass().getCanonicalName());
        mCurrentActivity.clear();
    }

    @Override
    public void onActivityStopped(Activity activity) {
        Crashlytics.log(Log.DEBUG, TAG,
                "onActivityStopped: " + activity.getClass().getCanonicalName());
        mCurrentActivity.clear();
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        Crashlytics.log(Log.DEBUG, TAG,
                "onActivitySaveInstanceState: " + activity.getClass().getCanonicalName());
        mCurrentActivity.clear();
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        Crashlytics.log(Log.DEBUG, TAG,
                "onActivityDestroyed: " + activity.getClass().getCanonicalName());
        mCurrentActivity.clear();
        // cancel all scheduled Croutons: Workaround until there's a way to detach the Activity from Crouton while
        // there are still some in the Queue.
//        Crouton.cancelAllCroutons();
    }

    @Nullable
    public static Activity getCurrentActivity() {
        return mCurrentActivity.get();
    }
}
