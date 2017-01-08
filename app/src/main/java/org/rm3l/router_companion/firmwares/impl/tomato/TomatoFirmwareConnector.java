package org.rm3l.router_companion.firmwares.impl.tomato;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.rm3l.router_companion.firmwares.AbstractRouterFirmwareConnector;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;

/**
 * Created by rm3l on 08/01/2017.
 */

public class TomatoFirmwareConnector extends AbstractRouterFirmwareConnector {

    public static final String MODEL = "t_model_name";

    @Override
    @Nullable
    protected String goGetRouterModel(@NonNull Context context, @NonNull Router router)
            throws Exception {

        final String[] output = SSHUtils.getManualProperty(context, router,
                Utils.getGlobalSharedPreferences(context),
                String.format(
                        "/bin/nvram show 2>/dev/null | grep %s | awk -F'=' '{print $2}'",
                        MODEL));
        if (output != null && output.length > 0) {
            return output[0];
        }
        return null;
    }
}
