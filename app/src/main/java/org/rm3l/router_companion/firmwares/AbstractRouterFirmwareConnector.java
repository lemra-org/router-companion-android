package org.rm3l.router_companion.firmwares;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.Utils;

/**
 * Created by rm3l on 08/01/2017.
 */

public abstract class AbstractRouterFirmwareConnector {

    @Nullable
    public final String getRouterModel(@NonNull Context context, @NonNull Router router)
            throws Exception {
        final String routerModel = this.goGetRouterModel(context, router);
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

    @Nullable
    protected abstract String goGetRouterModel(@NonNull Context context, @NonNull Router router)
            throws Exception;
}
