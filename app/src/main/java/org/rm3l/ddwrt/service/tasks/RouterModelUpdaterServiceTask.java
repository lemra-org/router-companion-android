package org.rm3l.ddwrt.service.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

/**
 * Created by rm3l on 30/07/15.
 */
public class RouterModelUpdaterServiceTask extends AbstractBackgroundServiceTask {

    public static final String DEFAULT_VALUE = "-";

    public RouterModelUpdaterServiceTask(@NonNull Context ctx) {
        super(ctx);
    }

    @Override
    public void runBackgroundServiceTask(@NonNull Router router) throws Exception {

        final SharedPreferences routerPreferences = mCtx.getSharedPreferences(
                router.getUuid(), Context.MODE_PRIVATE);

        if (routerPreferences == null) {
            return;
        }

        final NVRAMInfo nvramInfo = SSHUtils.getNVRamInfoFromRouter(
                mCtx,
                router,
                globalPreferences,
                NVRAMInfo.MODEL);

        if (nvramInfo == null) {
            return;
        }

        final String routerModel = nvramInfo.getProperty(NVRAMInfo.MODEL, DEFAULT_VALUE);

        final String routerModelFromPrefs =
                routerPreferences.getString(NVRAMInfo.MODEL, DEFAULT_VALUE);
        //noinspection ConstantConditions
        if (!(DEFAULT_VALUE.equals(routerModel) ||
                routerModelFromPrefs.equals(routerModel))) {
            routerPreferences
                    .edit()
                    .putString(NVRAMInfo.MODEL, routerModel)
                    .apply();
            Utils.requestBackup(mCtx);
        }
    }
}
