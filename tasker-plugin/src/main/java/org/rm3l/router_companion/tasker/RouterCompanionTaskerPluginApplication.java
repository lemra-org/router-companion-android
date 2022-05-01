package org.rm3l.router_companion.tasker;

import static com.facebook.stetho.Stetho.newInitializerBuilder;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.multidex.MultiDexApplication;
import com.facebook.stetho.Stetho;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.twofortyfouram.log.Lumberjack;
import java.lang.ref.WeakReference;

/** Implements an application object for the plug-in. */
/*
 * This application is non-essential for the plug-in's operation; it simply enables debugging
 * options globally for the app.
 */
public class RouterCompanionTaskerPluginApplication extends MultiDexApplication
    implements Application.ActivityLifecycleCallbacks {

  private static WeakReference<Activity> mCurrentActivity;

  @Nullable
  public static Activity getCurrentActivity() {
    return mCurrentActivity.get();
  }

  @Override
  public void onCreate() {
    super.onCreate();
    registerActivityLifecycleCallbacks(this);

    if (BuildConfig.DEBUG) {
      //            LeakCanary.install(this);
      // if using Robolectric in local unit test, thrown IOException.
      // see https://github.com/facebook/stetho/issues/440
      Stetho.initialize(
          newInitializerBuilder(this)
              .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
              .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this))
              .build());
    }

    // Set up Crashlytics, disabled for debug builds
    FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG);
    FirebaseCrashlytics.getInstance().setCustomKey("DEBUG", BuildConfig.DEBUG);

    Lumberjack.init(getApplicationContext());
  }

  @Override
  public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    FirebaseCrashlytics.getInstance()
        .log("onActivityCreated: " + activity.getClass().getCanonicalName());
    mCurrentActivity = new WeakReference<>(activity);
  }

  @Override
  public void onActivityDestroyed(Activity activity) {
    FirebaseCrashlytics.getInstance()
        .log("onActivityDestroyed: " + activity.getClass().getCanonicalName());
    mCurrentActivity.clear();
    // cancel all scheduled Croutons: Workaround until there's a way to detach the Activity from
    // Crouton while
    // there are still some in the Queue.
    //        Crouton.cancelAllCroutons();
  }

  @Override
  public void onActivityPaused(Activity activity) {
    FirebaseCrashlytics.getInstance()
        .log("onActivityPaused: " + activity.getClass().getCanonicalName());
    mCurrentActivity.clear();
  }

  @Override
  public void onActivityResumed(Activity activity) {
    FirebaseCrashlytics.getInstance()
        .log("onActivityResumed: " + activity.getClass().getCanonicalName());
    mCurrentActivity = new WeakReference<>(activity);
  }

  @Override
  public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    FirebaseCrashlytics.getInstance()
        .log("onActivitySaveInstanceState: " + activity.getClass().getCanonicalName());
    mCurrentActivity.clear();
  }

  @Override
  public void onActivityStarted(Activity activity) {
    FirebaseCrashlytics.getInstance()
        .log("onActivityStarted: " + activity.getClass().getCanonicalName());
    mCurrentActivity = new WeakReference<>(activity);
  }

  @Override
  public void onActivityStopped(Activity activity) {
    FirebaseCrashlytics.getInstance()
        .log("onActivityStopped: " + activity.getClass().getCanonicalName());
    mCurrentActivity.clear();
  }

  @Override
  public void onTerminate() {
    super.onTerminate();
    unregisterActivityLifecycleCallbacks(this);
  }
}
