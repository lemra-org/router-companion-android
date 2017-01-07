package org.rm3l.router_companion.tasker.utils;

import android.app.Activity;
import android.app.backup.BackupManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Splitter;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.rm3l.router_companion.tasker.Constants;
import org.rm3l.router_companion.tasker.exception.DDWRTCompanionPackageVersionRequiredNotFoundException;

import java.util.List;
import java.util.Locale;

import static org.rm3l.router_companion.tasker.Constants.DDWRT_COMPANION_MIN_VERSION_REQUIRED;
import static org.rm3l.router_companion.tasker.Constants.DDWRT_COMPANION_MIN_VERSION_REQUIRED_STR;

/**
 * Created by rm3l on 07/08/16.
 */
public final class Utils {

    private Utils() {
        throw new UnsupportedOperationException();
    }

    public static PackageInfo getPackageInstalled(String packagename, PackageManager packageManager) {
        try {
            return packageManager.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);

        } catch (final PackageManager.NameNotFoundException e) {
            Crashlytics.log(Log.WARN, Constants.TAG, "Package not installed: " + packagename + ". " +
                    ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }

    @Nullable
    public static PackageInfo getDDWRTCompanionAppPackage(PackageManager packageManager) {
        PackageInfo ddwrtCompanionAppPackage = null;
        try {
            ddwrtCompanionAppPackage = getPackageInstalled("org.rm3l.ddwrt", packageManager);
            if (ddwrtCompanionAppPackage != null) {
                return ddwrtCompanionAppPackage;
            }

            ddwrtCompanionAppPackage = getPackageInstalled("org.rm3l.ddwrt.amzn.underground", packageManager);
            if (ddwrtCompanionAppPackage != null) {
                return ddwrtCompanionAppPackage;
            }

            ddwrtCompanionAppPackage = getPackageInstalled("org.rm3l.ddwrt.free", packageManager);
            if (ddwrtCompanionAppPackage != null) {
                return ddwrtCompanionAppPackage;
            }

            return null;

        } finally {
            Crashlytics.log(Log.DEBUG, Constants.TAG,
                    "ddwrtCompanionAppPackageInfo = " + ddwrtCompanionAppPackage);
        }
    }

    @Nullable
    public static PackageInfo
    getDDWRTCompanionAppPackageLeastRequiredVersion(PackageManager packageManager) {
        final PackageInfo ddwrtCompanionAppPackage = getDDWRTCompanionAppPackage(packageManager);
        if (ddwrtCompanionAppPackage == null) {
            return null;
        }
        if (ddwrtCompanionAppPackage.versionCode < DDWRT_COMPANION_MIN_VERSION_REQUIRED) {
            final List<String> versionNameSplit = Splitter.on(" ").splitToList(ddwrtCompanionAppPackage.versionName);
            throw new DDWRTCompanionPackageVersionRequiredNotFoundException(
                    String.format(Locale.US,
                            "Please upgrade DD-WRT Companion app. " +
                            "Expected at least version '%s', but found version '%s'",
                            DDWRT_COMPANION_MIN_VERSION_REQUIRED_STR,
                            versionNameSplit.isEmpty() ?
                                    ddwrtCompanionAppPackage.versionName : versionNameSplit.get(0)));
        }
        return ddwrtCompanionAppPackage;
    }

    public static void requestBackup(@Nullable final Activity ctx) {
        if (ctx == null) {
            return;
        }
        new BackupManager(ctx).dataChanged();
    }
}
