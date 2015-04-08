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
import android.support.annotation.Nullable;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.apache.commons.lang3.StringUtils;
import org.rm3l.ddwrt.exceptions.DDWRTCompanionException;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.Utils;

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
        buildConfigClass = BuildConfig.class
)
public class DDWRTApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // The following line triggers the initialization of ACRA
        ACRA.init(this);

        ACRA.getErrorReporter()
                .putCustomData(TRACEPOT_DEVELOP_MODE, DEBUG ? "1" : "0");

        if (isFirstLaunch(this)) {
            //Report specific exception: this is to help me analyze whom this app is used by, and provide better device support!
            final FirstLaunch firstLaunchReport;
            if (StringUtils.startsWithIgnoreCase(FLAVOR, "google")) {
                firstLaunchReport = new GoogleFirstLaunch();
            } else if (StringUtils.startsWithIgnoreCase(FLAVOR, "amazon")) {
                firstLaunchReport = new AmazonFirstLaunch();
            } else if (StringUtils.startsWithIgnoreCase(FLAVOR, "fdroid")) {
                firstLaunchReport = new FDroidFirstLaunch();
            } else {
                firstLaunchReport = new FirstLaunch(FLAVOR);
            }
            Utils.reportException(firstLaunchReport);
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

    private final class AmazonFirstLaunch extends FirstLaunch {
        private AmazonFirstLaunch() {
            super("amazon");
        }
    }

    private final class FDroidFirstLaunch extends FirstLaunch {
        private FDroidFirstLaunch() {
            super("fdroid");
        }
    }
}