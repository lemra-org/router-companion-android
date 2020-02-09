package org.rm3l.router_companion.tasker.utils;

import static org.rm3l.router_companion.tasker.Constants.DDWRT_COMPANION_MIN_VERSION_REQUIRED;
import static org.rm3l.router_companion.tasker.Constants.DDWRT_COMPANION_MIN_VERSION_REQUIRED_STR;

import android.app.Activity;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import java.util.List;
import java.util.Locale;
import org.rm3l.router_companion.tasker.Constants;
import org.rm3l.router_companion.tasker.exception.DDWRTCompanionPackageVersionRequiredNotFoundException;

/**
 * Created by rm3l on 07/08/16.
 */
public final class Utils {

    @Nullable
    public static PackageInfo getDDWRTCompanionAppPackage(PackageManager packageManager) {
        PackageInfo ddwrtCompanionAppPackage = null;
        try {
            ddwrtCompanionAppPackage = getPackageInstalled("org.rm3l.ddwrt", packageManager);
            if (ddwrtCompanionAppPackage != null) {
                return ddwrtCompanionAppPackage;
            }

            ddwrtCompanionAppPackage =
                    getPackageInstalled("org.rm3l.ddwrt.amzn.underground", packageManager);
            if (ddwrtCompanionAppPackage != null) {
                return ddwrtCompanionAppPackage;
            }

            ddwrtCompanionAppPackage = getPackageInstalled("org.rm3l.ddwrt.free", packageManager);
            if (ddwrtCompanionAppPackage != null) {
                return ddwrtCompanionAppPackage;
            }

            return null;
        } finally {
            FirebaseCrashlytics.getInstance().log(
                    "ddwrtCompanionAppPackageInfo = " + ddwrtCompanionAppPackage);
        }
    }

    @Nullable
    public static PackageInfo getDDWRTCompanionAppPackageLeastRequiredVersion(
            PackageManager packageManager) {
        final PackageInfo ddwrtCompanionAppPackage = getDDWRTCompanionAppPackage(packageManager);
        if (ddwrtCompanionAppPackage == null) {
            return null;
        }
        if (ddwrtCompanionAppPackage.versionCode < DDWRT_COMPANION_MIN_VERSION_REQUIRED) {
            final List<String> versionNameSplit =
                    Splitter.on(" ").splitToList(ddwrtCompanionAppPackage.versionName);
            throw new DDWRTCompanionPackageVersionRequiredNotFoundException(String.format(Locale.US,
                    "Please upgrade DD-WRT Companion app. "
                            + "Expected at least version '%s', but found version '%s'",
                    DDWRT_COMPANION_MIN_VERSION_REQUIRED_STR,
                    versionNameSplit.isEmpty() ? ddwrtCompanionAppPackage.versionName
                            : versionNameSplit.get(0)));
        }
        return ddwrtCompanionAppPackage;
    }

    public static PackageInfo getPackageInstalled(String packagename, PackageManager packageManager) {
        try {
            return packageManager.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
        } catch (final PackageManager.NameNotFoundException e) {
            FirebaseCrashlytics.getInstance().log(
                    "Package not installed: " + packagename + ". " +
                            Throwables.getRootCause(e).getMessage());
            return null;
        }
    }

    public static void requestBackup(@Nullable final Activity ctx) {
        if (ctx == null) {
            return;
        }
        new BackupManager(ctx).dataChanged();
    }

    @NonNull
    public static String getDefaultSharedPreferencesName(@NonNull final Context context) {
        return context.getPackageName() + "_preferences";
    }

    private Utils() {
        throw new UnsupportedOperationException();
    }
}
