package org.rm3l.ddwrt.tasker.utils;

import android.content.pm.PackageManager;

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
}
