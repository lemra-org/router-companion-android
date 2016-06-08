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

package org.rm3l.ddwrt;

import android.app.Application;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.ImageView;

import com.airbnb.deeplinkdispatch.DeepLinkHandler;
import com.crashlytics.android.Crashlytics;
import com.facebook.stetho.Stetho;
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader;
import com.mikepenz.materialdrawer.util.DrawerImageLoader;
import com.squareup.picasso.Picasso;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteDAOImpl;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.ReportingUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.fabric.sdk.android.Fabric;

import static org.rm3l.ddwrt.BuildConfig.DEBUG;
import static org.rm3l.ddwrt.BuildConfig.FLAVOR;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.TRACEPOT_DEVELOP_MODE;
import static org.rm3l.ddwrt.utils.Utils.isFirstLaunch;

/**
 * App Main Entry point.
 * Leverages ACRA for capturing eventual app crashes and sending the relevant metrics for further analysis.
 */
@ReportsCrashes(
        formUri = DDWRTCompanionConstants.ACRA_BACKEND_URL,
        mode = ReportingInteractionMode.SILENT,
        sharedPreferencesName = DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY,
        sharedPreferencesMode = Context.MODE_PRIVATE,
        buildConfigClass = BuildConfig.class,
        additionalSharedPreferences = {DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY}
)
public class DDWRTApplication extends Application {

    private static final List<String> GOOGLE_INSTALLER_PACKAGE_NAMES =
            Arrays.asList("com.android.vending", "com.google.android.feedback");

    private static final List<String> AMAZON_INSTALLER_PACKAGE_NAMES =
            Collections.singletonList("com.amazon.venezia");

    private static final List<String> FDROID_INSTALLER_PACKAGE_NAMES =
            Collections.singletonList("org.fdroid.fdroid.installer");

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
//            LeakCanary.install(this);
            Stetho.initializeWithDefaults(this);
        }

        final IntentFilter intentFilter = new IntentFilter(DeepLinkHandler.ACTION);
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(new DeepLinkReceiver(), intentFilter);

        DDWRTCompanionSqliteDAOImpl.initialize(getApplicationContext());

        //initialize and create the image loader logic (for MaterialDrawer, used throughout the app)
        DrawerImageLoader.init(new AbstractDrawerImageLoader() {
            @Override
            public void set(ImageView imageView, Uri uri, Drawable placeholder) {
                if (imageView != null) {
                    Picasso.with(imageView.getContext())
                            .load(uri)
                            .placeholder(placeholder)
                            .into(imageView);
                }
            }

            @Override
            public void cancel(ImageView imageView) {
                if (imageView != null) {
                    Picasso.with(imageView.getContext())
                            .cancelRequest(imageView);
                }
            }

            @Override
            public Drawable placeholder(Context ctx) {
                if (ctx == null) {
                    return null;
                }
                return ContextCompat.getDrawable(ctx, R.drawable.router);
            }
        });

        // The following line triggers the initialization of ACRA
        ACRA.init(this);

        ACRA.getErrorReporter()
                .putCustomData(TRACEPOT_DEVELOP_MODE, DEBUG ? "1" : "0");

        Fabric.with(this, new Crashlytics());
        Crashlytics.setBool("DEBUG", BuildConfig.DEBUG);
        Crashlytics.setBool("WITH_ADS", BuildConfig.WITH_ADS);

        final String acraEmailAddr = getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, MODE_PRIVATE)
                .getString(DDWRTCompanionConstants.ACRA_USER_EMAIL, null);
        Crashlytics.setUserEmail(acraEmailAddr);

        //We must initialize Fabric prior to calling this
        if (isFirstLaunch(this)) {
            final String appOriginInstallerPackageName = Utils.getAppOriginInstallerPackageName(this);
            //Report specific exception: this is to help me analyze whom this app is used by, and provide better device support!

            final Map<String, Object> eventMap = new HashMap<>();
            final String lastKnownVersionStr =
                    getSharedPreferences(DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY,
                            Context.MODE_PRIVATE)
                        .getString(DDWRTCompanionConstants.LAST_KNOWN_VERSION, null);
            eventMap.put("PREVIOUS_VERSION",
                    lastKnownVersionStr != null ? lastKnownVersionStr : "???");
            eventMap.put("UPDATE", lastKnownVersionStr != null);
            eventMap.put("FLAVOR", FLAVOR);
            eventMap.put("INSTALLER", appOriginInstallerPackageName);
            eventMap.put("VERSION_CODE", BuildConfig.VERSION_CODE);
            eventMap.put("VERSION_NAME", BuildConfig.VERSION_NAME);
            eventMap.put("DEBUG", BuildConfig.DEBUG);
            eventMap.put("WITH_ADS", BuildConfig.WITH_ADS);
            ReportingUtils.reportEvent(ReportingUtils.EVENT_FIRST_LAUNCH, eventMap);
        }

        if (ColorUtils.isThemeLight(getApplicationContext())) {
            //Light
            setTheme(R.style.AppThemeLight);
        } else {
            //Default is Dark
            setTheme(R.style.AppThemeDark);
        }

    }

}