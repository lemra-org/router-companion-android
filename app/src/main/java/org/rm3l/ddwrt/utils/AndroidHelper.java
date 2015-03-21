package org.rm3l.ddwrt.utils;

import android.content.Context;
import android.content.pm.PackageManager;

public final class AndroidHelper {

    private AndroidHelper() {
    }

    public static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
