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

package org.rm3l.router_companion

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.widget.ImageView
import com.airbnb.deeplinkdispatch.DeepLinkHandler
import com.avocarrot.sdk.Avocarrot
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import com.evernote.android.job.JobConfig
import com.evernote.android.job.JobManager
import com.facebook.stetho.Stetho
import com.facebook.stetho.Stetho.newInitializerBuilder
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import com.squareup.leakcanary.LeakCanary
import com.squareup.picasso.Picasso
import io.fabric.sdk.android.Fabric
import org.rm3l.ddwrt.BuildConfig
import org.rm3l.ddwrt.BuildConfig.FLAVOR
import org.rm3l.ddwrt.R
import org.rm3l.router_companion.RouterCompanionAppConstants.ACRA_ENABLE
import org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY
import org.rm3l.router_companion.deeplinks.DeepLinkActivity
import org.rm3l.router_companion.deeplinks.DeepLinkReceiver
import org.rm3l.router_companion.deeplinks.RouterActionsDeepLinkActivity
import org.rm3l.router_companion.job.RouterCompanionJobCreator
import org.rm3l.router_companion.job.RouterCompanionJobLogger
import org.rm3l.router_companion.job.firmware_update.FirmwareUpdateCheckerJob
import org.rm3l.router_companion.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteDAOImpl
import org.rm3l.router_companion.service.BackgroundService
import org.rm3l.router_companion.utils.ColorUtils
import org.rm3l.router_companion.utils.ReportingUtils
import org.rm3l.router_companion.utils.Utils
import org.rm3l.router_companion.utils.Utils.isFirstLaunch
import org.rm3l.router_companion.utils.notifications.createOrUpdateNotificationChannels
import org.rm3l.router_companion.welcome.GettingStartedActivity
import org.wordpress.passcodelock.AppLockManager
import java.lang.ref.WeakReference
import java.util.Arrays

//import com.facebook.stetho.dumpapp.DumperPlugin;
//import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;

/**
 * App Main Entry point.
 * Leverages ACRA for capturing eventual app crashes and sending the relevant metrics for further
 * analysis.
 */
class RouterCompanionApplication : Application(), Application.ActivityLifecycleCallbacks {

    override fun onCreate() {
        super.onCreate()

        val appPreferences = getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)

        // Set up Crashlytics, disabled for debug builds for if user has explicitly requested so
        val autoCrashReportingEnabled = appPreferences.getBoolean(ACRA_ENABLE, true)
        Log.i(TAG, "autoCrashReportingEnabled=" + autoCrashReportingEnabled)
        val crashlyticsKit = Crashlytics.Builder()
                .core(CrashlyticsCore.Builder()
                        .disabled(BuildConfig.DEBUG || !autoCrashReportingEnabled)
                        .build())
                .build()
        Fabric.with(this, crashlyticsKit)
        Crashlytics.setBool("DEBUG", BuildConfig.DEBUG)
        Crashlytics.setBool("WITH_ADS", BuildConfig.WITH_ADS)

        val acraEmailAddr = appPreferences.getString(RouterCompanionAppConstants.ACRA_USER_EMAIL, null)
        Crashlytics.setUserEmail(acraEmailAddr)

        mDebugResourceInspectorEnabled = appPreferences.getBoolean(DEBUG_RESOURCE_INSPECTOR_PREF_KEY, false)

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

        registerActivityLifecycleCallbacks(this)

        AppLockManager.getInstance().enableDefaultAppLockIfAvailable(this)
        if (AppLockManager.getInstance().isAppLockFeatureEnabled) {
            /* Disable lockscreen for some activities if needed */
            AppLockManager.getInstance().appLock.exemptActivities = arrayOf(SplashActivity::class.java.canonicalName,
                    GettingStartedActivity::class.java.canonicalName,
                    DeepLinkActivity::class.java.canonicalName,
                    DeepLinkActivity::class.java.canonicalName,
                    RouterActionsDeepLinkActivity::class.java.canonicalName)
        }

        if (BuildConfig.WITH_ADS || BuildConfig.WITH_INTERSTITIAL_ADS) {
            Avocarrot.setTestMode(BuildConfig.DEBUG) // enable test ads
            Avocarrot.setDebugMode(BuildConfig.DEBUG) // enable logger
        }

        if (BuildConfig.DEBUG && !IS_USING_ROBOLECTRIC_UNIT_TESTING) {
            // if using Robolectric in local unit test, thrown IOException.
            // see https://github.com/facebook/stetho/issues/440
            Stetho.initialize(
                    newInitializerBuilder(this)
                            .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                            .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this))
                            .build())
        }

        if (BuildConfig.DEBUG && appPreferences.getBoolean(DEBUG_LEAKCANARY_PREF_KEY, false)) {
            Log.d(TAG, "--> Start w/ LeakCanary...")
            LeakCanary.install(this)
        } else {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "--> Start w/o LeakCanary...")
            }
        }

        val intentFilter = IntentFilter(DeepLinkHandler.ACTION)
        LocalBroadcastManager.getInstance(this).registerReceiver(DeepLinkReceiver(), intentFilter)

        if (!DDWRTCompanionSqliteDAOImpl.isInitialized()) {
            DDWRTCompanionSqliteDAOImpl.initialize(applicationContext)
        }

        //initialize and create the image loader logic (for MaterialDrawer, used throughout the app)
        DrawerImageLoader.init(object : AbstractDrawerImageLoader() {
            override fun cancel(imageView: ImageView?) {
                if (imageView != null) {
                    Picasso.with(imageView.context).cancelRequest(imageView)
                }
            }

            override fun placeholder(ctx: Context?): Drawable? {
                return if (ctx == null) {
                    null
                } else ContextCompat.getDrawable(ctx, R.drawable.router)
            }

            override fun set(imageView: ImageView?, uri: Uri?, placeholder: Drawable?) {
                if (imageView != null) {
                    Picasso.with(imageView.context).load(uri).placeholder(placeholder).into(imageView)
                }
            }
        })

        //We must initialize Fabric prior to calling this
        if (isFirstLaunch(this)) {
            val appOriginInstallerPackageName = Utils.getAppOriginInstallerPackageName(this)
            //Report specific exception: this is to help me analyze whom this app is used by, and provide better device support!

            val eventMap = mutableMapOf<String, Any?>()
            val lastKnownVersionStr = appPreferences.getString(RouterCompanionAppConstants.LAST_KNOWN_VERSION, null)
            eventMap.put("PREVIOUS_VERSION", lastKnownVersionStr ?: "???")
            eventMap.put("UPDATE", lastKnownVersionStr != null)
            eventMap.put("FLAVOR", FLAVOR)
            eventMap.put("INSTALLER", appOriginInstallerPackageName)
            eventMap.put("VERSION_CODE", BuildConfig.VERSION_CODE)
            eventMap.put("VERSION_NAME", BuildConfig.VERSION_NAME)
            eventMap.put("DEBUG", BuildConfig.DEBUG)
            eventMap.put("WITH_ADS", BuildConfig.WITH_ADS)
            ReportingUtils.reportEvent(ReportingUtils.EVENT_FIRST_LAUNCH, eventMap)
        }

        ColorUtils.setAppTheme(this, null, false)
        //        if (ColorUtils.isThemeLight(getApplicationContext())) {
        //            //Light
        //            setTheme(R.style.AppThemeLight);
        //        } else {
        //            //Default is Dark
        //            setTheme(R.style.AppThemeDark);
        //        }

        org.osmdroid.config.Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID

        //General Notification Channel
        this.createOrUpdateNotificationChannels()

        //Job Scheduling
        try {
            JobConfig.addLogger(RouterCompanionJobLogger())
            JobManager.create(this).addJobCreator(RouterCompanionJobCreator())

            //Default jobs
            BackgroundService.schedule()
            FirmwareUpdateCheckerJob.schedule()
        } catch (e: Exception) {
            Crashlytics.log(Log.WARN, TAG, "JobManager reported an error => no job scheduling feature then: " + e.message)
            Crashlytics.logException(e)
        }

    }

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
        Crashlytics.log(Log.DEBUG, TAG, "onActivityCreated: " + activity?.javaClass?.canonicalName)
        mCurrentActivity = WeakReference(activity)
    }

    override fun onActivityDestroyed(activity: Activity?) {
        Crashlytics.log(Log.DEBUG, TAG,
                "onActivityDestroyed: " + activity?.javaClass?.canonicalName)
        mCurrentActivity?.clear()
//        if (mCurrentActivity != null) {
//            mCurrentActivity!!.clear()
//        }
    }

    override fun onActivityPaused(activity: Activity?) {
        Crashlytics.log(Log.DEBUG, TAG, "onActivityPaused: " + activity?.javaClass?.canonicalName)
        mCurrentActivity?.clear()
//        if (mCurrentActivity != null) {
//            mCurrentActivity!!.clear()
//        }
    }

    override fun onActivityResumed(activity: Activity?) {
        Crashlytics.log(Log.DEBUG, TAG, "onActivityResumed: " + activity?.javaClass?.canonicalName)
        mCurrentActivity = WeakReference(activity)
    }

    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
        Crashlytics.log(Log.DEBUG, TAG,
                "onActivitySaveInstanceState: " + activity?.javaClass?.canonicalName)
//        if (mCurrentActivity != null) {
//            mCurrentActivity!!.clear()
//        }
        mCurrentActivity?.clear()
    }

    override fun onActivityStarted(activity: Activity?) {
        Crashlytics.log(Log.DEBUG, TAG, "onActivityStarted: " + activity?.javaClass?.canonicalName)
        mCurrentActivity = WeakReference(activity)
    }

    override fun onActivityStopped(activity: Activity?) {
        Crashlytics.log(Log.DEBUG, TAG, "onActivityStopped: " + activity?.javaClass?.canonicalName)
//        if (mCurrentActivity != null) {
//            mCurrentActivity!!.clear()
//        }
        mCurrentActivity?.clear()
    }

    override fun onTerminate() {
        super.onTerminate()
        unregisterActivityLifecycleCallbacks(this)
    }

    companion object {

        @JvmField
        val DEBUG_LEAKCANARY_PREF_KEY = \"fake-key\";

        private var mDebugResourceInspectorEnabled = false

        @JvmStatic
        fun isDebugResourceInspectorEnabled() = mDebugResourceInspectorEnabled

        @JvmField
        val DEBUG_RESOURCE_INSPECTOR_PREF_KEY = \"fake-key\";

        private val TAG = RouterCompanionApplication::class.java.simpleName

        private val GOOGLE_INSTALLER_PACKAGE_NAMES = Arrays.asList("com.android.vending", "com.google.android.feedback")
        private val AMAZON_INSTALLER_PACKAGE_NAMES = listOf("com.amazon.venezia")
        private val FDROID_INSTALLER_PACKAGE_NAMES = listOf("org.fdroid.fdroid.installer")

        private var mCurrentActivity: WeakReference<Activity?>? = null

        @JvmStatic
        fun getCurrentActivity() = mCurrentActivity?.get()

        @JvmStatic
        val IS_USING_ROBOLECTRIC_UNIT_TESTING = ("robolectric" == Build.FINGERPRINT)
    }
}