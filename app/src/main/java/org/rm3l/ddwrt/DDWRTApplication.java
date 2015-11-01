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
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.crashlytics.android.Crashlytics;
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader;
import com.mikepenz.materialdrawer.util.DrawerImageLoader;
import com.squareup.picasso.Picasso;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.apache.commons.lang3.StringUtils;
import org.rm3l.ddwrt.exceptions.DDWRTCompanionException;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.Utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.fabric.sdk.android.Fabric;

import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.ddwrt.BuildConfig.DEBUG;
import static org.rm3l.ddwrt.BuildConfig.FLAVOR;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.THEMING_PREF;
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

        //initialize and create the image loader logic (for MaterialDrawer, used throughout the app)
        DrawerImageLoader.init(new AbstractDrawerImageLoader() {
            @Override
            public void set(ImageView imageView, Uri uri, Drawable placeholder) {
                Picasso.with(imageView.getContext())
                        .load(uri)
                        .placeholder(placeholder)
                        .into(imageView);
            }

            @Override
            public void cancel(ImageView imageView) {
                Picasso.with(imageView.getContext())
                        .cancelRequest(imageView);
            }

            @Override
            public Drawable placeholder(Context ctx) {
                final int routerDrawable = R.drawable.router;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    return ctx.getResources().getDrawable(routerDrawable, getTheme());
                }
                return ctx.getResources().getDrawable(routerDrawable);
            }
        });

        // The following line triggers the initialization of ACRA
        ACRA.init(this);

        ACRA.getErrorReporter()
                .putCustomData(TRACEPOT_DEVELOP_MODE, DEBUG ? "1" : "0");

        if (isFirstLaunch(this)) {
            final String appOriginInstallerPackageName = Utils.getAppOriginInstallerPackageName(this);
            //Report specific exception: this is to help me analyze whom this app is used by, and provide better device support!
            final FirstLaunch firstLaunchReport;
            if (StringUtils.startsWithIgnoreCase(FLAVOR, "google")) {
                if (GOOGLE_INSTALLER_PACKAGE_NAMES.contains(
                        nullToEmpty(appOriginInstallerPackageName).toLowerCase())) {
                    firstLaunchReport = new GoogleFirstLaunch();
                } else {
                    firstLaunchReport = new GoogleUnknownOriginFirstLaunch(appOriginInstallerPackageName);
                }
            } else if (StringUtils.startsWithIgnoreCase(FLAVOR, "amazon")) {
                if (AMAZON_INSTALLER_PACKAGE_NAMES.contains(
                        nullToEmpty(appOriginInstallerPackageName).toLowerCase())) {
                    firstLaunchReport = new AmazonFirstLaunch();
                } else {
                    firstLaunchReport = new AmazonUnknownOriginFirstLaunch(appOriginInstallerPackageName);
                }
            } else if (StringUtils.startsWithIgnoreCase(FLAVOR, "fdroid")) {
                if (FDROID_INSTALLER_PACKAGE_NAMES.contains(
                        nullToEmpty(appOriginInstallerPackageName).toLowerCase())) {
                    firstLaunchReport = new FDroidFirstLaunch();
                } else {
                    firstLaunchReport = new FDroidUnknownOriginFirstLaunch(appOriginInstallerPackageName);
                }
            } else {
                firstLaunchReport = new FirstLaunch(FLAVOR);
            }
            Utils.reportException(this, firstLaunchReport);
        }

        final long currentTheme = getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
                .getLong(THEMING_PREF, DDWRTCompanionConstants.DEFAULT_THEME);
        if (currentTheme == ColorUtils.LIGHT_THEME) {
            //Light
            setTheme(R.style.AppThemeLight);
        } else {
            //Default is Dark
            setTheme(R.style.AppThemeDark);
        }

        Fabric.with(this, new Crashlytics());
        Crashlytics.setBool("DEBUG", BuildConfig.DEBUG);
        Crashlytics.setBool("WITH_ADS", BuildConfig.WITH_ADS);
    }

    private class FirstLaunch extends DDWRTCompanionException {
        private FirstLaunch(@Nullable String detailMessage) {
            super("[Build Flavor: " + FLAVOR + "]: " + detailMessage);
        }
    }

    private final class GoogleFirstLaunch extends FirstLaunch {
        private GoogleFirstLaunch() {
            super("google");
        }
    }

    private final class GoogleUnknownOriginFirstLaunch extends FirstLaunch {
        private GoogleUnknownOriginFirstLaunch(@Nullable final String installer) {
            super("google: " + installer);
        }
    }

    private final class AmazonFirstLaunch extends FirstLaunch {
        private AmazonFirstLaunch() {
            super("amazon");
        }
    }

    private final class AmazonUnknownOriginFirstLaunch extends FirstLaunch {
        private AmazonUnknownOriginFirstLaunch(@Nullable final String installer) {
            super("amazon: " + installer);
        }
    }

    private final class FDroidFirstLaunch extends FirstLaunch {
        private FDroidFirstLaunch() {
            super("fdroid");
        }
    }

    private final class FDroidUnknownOriginFirstLaunch extends FirstLaunch {
        private FDroidUnknownOriginFirstLaunch(@Nullable final String installer) {
            super("fdroid: " + installer);
        }
    }
}