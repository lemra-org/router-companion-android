/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014  Armel Soro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact Info: Armel Soro <apps+ddwrt@rm3l.org>
 */

package org.rm3l.router_companion;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ImageView;
import com.airbnb.deeplinkdispatch.DeepLinkActivity;
import com.airbnb.deeplinkdispatch.DeepLinkHandler;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.facebook.stetho.Stetho;
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader;
import com.mikepenz.materialdrawer.util.DrawerImageLoader;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.picasso.Picasso;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import io.fabric.sdk.android.Fabric;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.deeplinks.DeepLinkReceiver;
import org.rm3l.router_companion.deeplinks.RouterActionsDeepLinkActivity;
import org.rm3l.router_companion.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteDAOImpl;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.ReportingUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.welcome.GettingStartedActivity;
import org.wordpress.passcodelock.AppLockManager;

import static org.rm3l.ddwrt.BuildConfig.FLAVOR;
import static org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.router_companion.utils.Utils.isFirstLaunch;

/**
 * App Main Entry point.
 * Leverages ACRA for capturing eventual app crashes and sending the relevant metrics for further
 * analysis.
 */
public class RouterCompanionApplication extends Application
    implements Application.ActivityLifecycleCallbacks {

  public static final String DEBUG_LEAKCANARY_PREF_KEY = \"fake-key\";
  private static final String TAG = RouterCompanionApplication.class.getSimpleName();
  private static final List<String> GOOGLE_INSTALLER_PACKAGE_NAMES =
      Arrays.asList("com.android.vending", "com.google.android.feedback");
  private static final List<String> AMAZON_INSTALLER_PACKAGE_NAMES =
      Collections.singletonList("com.amazon.venezia");
  private static final List<String> FDROID_INSTALLER_PACKAGE_NAMES =
      Collections.singletonList("org.fdroid.fdroid.installer");
  @Nullable private static WeakReference<Activity> mCurrentActivity;

  @Nullable public static Activity getCurrentActivity() {
    return mCurrentActivity != null ? mCurrentActivity.get() : null;
  }

  @Override public void onCreate() {
    super.onCreate();

    final SharedPreferences appPreferences =
        getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, MODE_PRIVATE);

    //        if (BuildConfig.DEBUG) {
    //            //Enable Strict Mode in DEBUG mode
    //            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
    //                    .detectAll()
    //                    .detectDiskReads()
    //                    .detectDiskWrites()
    //                    .detectNetwork()
    //                    // alternatively .detectAll() for all detectable problems
    //                    .penaltyLog()
    //                    .penaltyDeath()
    //                    .build());
    //            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
    //                    .detectLeakedSqlLiteObjects()
    //                    .detectLeakedClosableObjects()
    //                    // alternatively .detectAll() for all detectable problems
    //                    .penaltyLog()
    //                    .penaltyDeath()
    //                    .build());
    //        }

    registerActivityLifecycleCallbacks(this);

    AppLockManager.getInstance().enableDefaultAppLockIfAvailable(this);
    if (AppLockManager.getInstance().isAppLockFeatureEnabled()) {
            /* Disable lockscreen for some activities if needed */
      AppLockManager.getInstance().getAppLock().setExemptActivities(new String[] {
          SplashActivity.class.getCanonicalName(), GettingStartedActivity.class.getCanonicalName(),
          DeepLinkActivity.class.getCanonicalName(),
          RouterActionsDeepLinkActivity.class.getCanonicalName()
      });
    }

    if (BuildConfig.DEBUG) {
      Stetho.initializeWithDefaults(this);
    }

    if (BuildConfig.DEBUG && appPreferences.getBoolean(DEBUG_LEAKCANARY_PREF_KEY, false)) {
      Log.d(TAG, "--> Start w/ LeakCanary...");
      LeakCanary.install(this);
    } else {
      if (BuildConfig.DEBUG) {
        Log.d(TAG, "--> Start w/o LeakCanary...");
      }
    }

    final IntentFilter intentFilter = new IntentFilter(DeepLinkHandler.ACTION);
    LocalBroadcastManager.getInstance(this).registerReceiver(new DeepLinkReceiver(), intentFilter);

    DDWRTCompanionSqliteDAOImpl.initialize(getApplicationContext());

    //initialize and create the image loader logic (for MaterialDrawer, used throughout the app)
    DrawerImageLoader.init(new AbstractDrawerImageLoader() {
      @Override public void set(ImageView imageView, Uri uri, Drawable placeholder) {
        if (imageView != null) {
          Picasso.with(imageView.getContext()).load(uri).placeholder(placeholder).into(imageView);
        }
      }

      @Override public void cancel(ImageView imageView) {
        if (imageView != null) {
          Picasso.with(imageView.getContext()).cancelRequest(imageView);
        }
      }

      @Override public Drawable placeholder(Context ctx) {
        if (ctx == null) {
          return null;
        }
        return ContextCompat.getDrawable(ctx, R.drawable.router);
      }
    });

    // Set up Crashlytics, disabled for debug builds
    final Crashlytics crashlyticsKit = new Crashlytics.Builder()
        .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
        .build();
    Fabric.with(this, crashlyticsKit);
    Crashlytics.setBool("DEBUG", BuildConfig.DEBUG);
    Crashlytics.setBool("WITH_ADS", BuildConfig.WITH_ADS);

    final String acraEmailAddr =
        appPreferences.getString(RouterCompanionAppConstants.ACRA_USER_EMAIL, null);
    Crashlytics.setUserEmail(acraEmailAddr);

    //We must initialize Fabric prior to calling this
    if (isFirstLaunch(this)) {
      final String appOriginInstallerPackageName = Utils.getAppOriginInstallerPackageName(this);
      //Report specific exception: this is to help me analyze whom this app is used by, and provide better device support!

      final Map<String, Object> eventMap = new HashMap<>();
      final String lastKnownVersionStr =
          appPreferences.getString(RouterCompanionAppConstants.LAST_KNOWN_VERSION, null);
      eventMap.put("PREVIOUS_VERSION", lastKnownVersionStr != null ? lastKnownVersionStr : "???");
      eventMap.put("UPDATE", lastKnownVersionStr != null);
      eventMap.put("FLAVOR", FLAVOR);
      eventMap.put("INSTALLER", appOriginInstallerPackageName);
      eventMap.put("VERSION_CODE", BuildConfig.VERSION_CODE);
      eventMap.put("VERSION_NAME", BuildConfig.VERSION_NAME);
      eventMap.put("DEBUG", BuildConfig.DEBUG);
      eventMap.put("WITH_ADS", BuildConfig.WITH_ADS);
      ReportingUtils.reportEvent(ReportingUtils.EVENT_FIRST_LAUNCH, eventMap);
    }

    ColorUtils.setAppTheme(this, null, false);
    //        if (ColorUtils.isThemeLight(getApplicationContext())) {
    //            //Light
    //            setTheme(R.style.AppThemeLight);
    //        } else {
    //            //Default is Dark
    //            setTheme(R.style.AppThemeDark);
    //        }

    OpenStreetMapTileProviderConstants.setUserAgentValue(BuildConfig.APPLICATION_ID);
  }

  @Override public void onTerminate() {
    super.onTerminate();
    unregisterActivityLifecycleCallbacks(this);
  }

  @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    Crashlytics.log(Log.DEBUG, TAG, "onActivityCreated: " + activity.getClass().getCanonicalName());
    mCurrentActivity = new WeakReference<>(activity);
  }

  @Override public void onActivityStarted(Activity activity) {
    Crashlytics.log(Log.DEBUG, TAG, "onActivityStarted: " + activity.getClass().getCanonicalName());
    mCurrentActivity = new WeakReference<>(activity);
  }

  @Override public void onActivityResumed(Activity activity) {
    Crashlytics.log(Log.DEBUG, TAG, "onActivityResumed: " + activity.getClass().getCanonicalName());
    mCurrentActivity = new WeakReference<>(activity);
  }

  @Override public void onActivityPaused(Activity activity) {
    Crashlytics.log(Log.DEBUG, TAG, "onActivityPaused: " + activity.getClass().getCanonicalName());
    if (mCurrentActivity != null) {
      mCurrentActivity.clear();
    }
  }

  @Override public void onActivityStopped(Activity activity) {
    Crashlytics.log(Log.DEBUG, TAG, "onActivityStopped: " + activity.getClass().getCanonicalName());
    if (mCurrentActivity != null) {
      mCurrentActivity.clear();
    }
  }

  @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    Crashlytics.log(Log.DEBUG, TAG,
        "onActivitySaveInstanceState: " + activity.getClass().getCanonicalName());
    if (mCurrentActivity != null) {
      mCurrentActivity.clear();
    }
  }

  @Override public void onActivityDestroyed(Activity activity) {
    Crashlytics.log(Log.DEBUG, TAG,
        "onActivityDestroyed: " + activity.getClass().getCanonicalName());
    if (mCurrentActivity != null) {
      mCurrentActivity.clear();
    }
    // cancel all scheduled Croutons: Workaround until there's a way to detach the Activity from Crouton while
    // there are still some in the Queue.
    Crouton.cancelAllCroutons();
  }
}