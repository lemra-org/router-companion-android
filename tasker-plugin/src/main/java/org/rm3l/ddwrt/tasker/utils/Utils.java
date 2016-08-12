package org.rm3l.ddwrt.tasker.utils;

import android.content.pm.PackageManager;
import android.support.annotation.Nullable;

/**
 * Created by rm3l on 07/08/16.
 */
public final class Utils {

    private Utils() {
        throw new UnsupportedOperationException();
    }

    public static boolean isPackageInstalled(String packagename,
                                             PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (final PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Nullable
    public static String getDDWRTCompanionAppPackage(PackageManager packageManager) {
        final String ddwrtCompanionAppPackage;
        if (Utils.isPackageInstalled("org.rm3l.ddwrt", packageManager)) {
            ddwrtCompanionAppPackage = "org.rm3l.ddwrt";
        } else if (Utils.isPackageInstalled("org.rm3l.ddwrt.amzn.underground", packageManager)) {
            ddwrtCompanionAppPackage = "org.rm3l.ddwrt.amzn.underground";
        } else if (Utils.isPackageInstalled("org.rm3l.ddwrt.lite", packageManager)) {
            ddwrtCompanionAppPackage = "org.rm3l.ddwrt.lite";
        } else {
            ddwrtCompanionAppPackage = null;
        }
        return ddwrtCompanionAppPackage;
    }
}
