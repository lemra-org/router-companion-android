package org.rm3l.ddwrt.service.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.status.router.StatusRouterCPUTile;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

public class RouterInfoForFeedbackServiceTask extends AbstractBackgroundServiceTask {

    public static final String DEFAULT_VALUE = "-";

    public RouterInfoForFeedbackServiceTask(@NonNull Context ctx) {
        super(ctx);
    }

    @Override
    public void runBackgroundServiceTask(@NonNull Router router) throws Exception {

        final SharedPreferences routerPreferences = mCtx.getSharedPreferences(
                router.getUuid(), Context.MODE_PRIVATE);

        if (routerPreferences == null) {
            return;
        }

        final String[] manualProperty = SSHUtils.getManualProperty(mCtx, router, globalPreferences,
                "uname -a",
                StatusRouterCPUTile.GREP_MODEL_PROC_CPUINFO + "| uniq",
                StatusRouterCPUTile.GREP_MODEL_PROC_CPUINFO + "| wc -l",
                "/sbin/softwarerevision 2>&1",
                "cat /tmp/loginprompt 2>&1");

        if (manualProperty == null) {
            return;
        }

        String kernel = null;
        final String kernelFromPrefs =
                routerPreferences.getString(NVRAMInfo.KERNEL, DEFAULT_VALUE);
        if (manualProperty.length >= 1) {
            kernel = manualProperty[0];
        }
        String cpuModel = null;
        final String cpuModelFromPrefs =
                routerPreferences.getString(NVRAMInfo.CPU_MODEL, DEFAULT_VALUE);
        if (manualProperty.length >= 2) {
            cpuModel = manualProperty[1];
        }
        String cpuCoresCount = null;
        final String cpuCoresCountFromPrefs =
                routerPreferences.getString(NVRAMInfo.CPU_CORES_COUNT, DEFAULT_VALUE);
        if (manualProperty.length >= 3) {
            cpuCoresCount = manualProperty[2];
        }
        String ddwrtRev = null;
        final String ddwrtRevFromPrefs =
                routerPreferences.getString(NVRAMInfo.REVISION, DEFAULT_VALUE);
        if (manualProperty.length >= 4) {
            ddwrtRev = manualProperty[3];
        }
        String loginPrompt = "";
        final String loginPromptFromPrefs =
                routerPreferences.getString(NVRAMInfo.LOGIN_PROMPT, DEFAULT_VALUE);
        if (manualProperty.length >= 5) {
            for (int i = 3; i < manualProperty.length; i++) {
                loginPrompt += (manualProperty[i] + "\n");
            }
        }

        final SharedPreferences.Editor editor = routerPreferences
                .edit();
        boolean prefsEdited = false;
        if (!(DEFAULT_VALUE.equals(kernel) || kernelFromPrefs.equals(kernel))) {
            editor.putString(NVRAMInfo.KERNEL, kernel);
            prefsEdited = true;
        }
        if (!(DEFAULT_VALUE.equals(cpuModel) || cpuModelFromPrefs.equals(cpuModel))) {
            editor.putString(NVRAMInfo.CPU_MODEL, kernel);
            prefsEdited = true;
        }
        if (!(DEFAULT_VALUE.equals(cpuCoresCount) || cpuCoresCountFromPrefs.equals(cpuCoresCount))) {
            editor.putString(NVRAMInfo.CPU_CORES_COUNT, cpuCoresCount);
            prefsEdited = true;
        }
        if (!(DEFAULT_VALUE.equals(ddwrtRev) || ddwrtRevFromPrefs.equals(ddwrtRev))) {
            editor.putString(NVRAMInfo.REVISION, ddwrtRev);
            prefsEdited = true;
        }
        if (!(DEFAULT_VALUE.equals(loginPrompt) || loginPromptFromPrefs.equals(loginPrompt))) {
            editor.putString(NVRAMInfo.LOGIN_PROMPT, loginPrompt);
            prefsEdited = true;
        }

        if (prefsEdited) {
            editor.apply();
            Utils.requestBackup(mCtx);
        }
    }
}
