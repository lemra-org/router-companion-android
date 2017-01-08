package org.rm3l.router_companion.firmwares.impl.ddwrt;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.rm3l.router_companion.firmwares.RouterFirmwareConnector;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;

/**
 * Created by rm3l on 08/01/2017.
 */

public class DDWRTFirmwareConnector implements RouterFirmwareConnector {

    public static final String MODEL = "DD_BOARD";

    @Override
    @Nullable
    public String getRouterModel(@NonNull Context context, @NonNull Router router) throws Exception {
        final String[] output = SSHUtils.getManualProperty(context, router,
                Utils.getGlobalSharedPreferences(context),
                String.format(
                        "/usr/sbin/nvram show 2>/dev/null | grep %s | awk -F'=' '{print $2}'",
                        MODEL));
        final String routerModel;
        if (output != null && output.length > 0) {
            routerModel = output[0];
        } else {
            routerModel = null;
        }
        final SharedPreferences routerPreferences = router.getPreferences(context);
        if (routerPreferences != null) {
            routerPreferences
                    .edit()
                    .putString(NVRAMInfo.MODEL, routerModel)
                    .apply();
            Utils.requestBackup(context);
        }
        return routerModel;
    }
}
