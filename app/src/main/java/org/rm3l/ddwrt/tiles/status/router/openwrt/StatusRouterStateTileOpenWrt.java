package org.rm3l.ddwrt.tiles.status.router.openwrt;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;

import com.google.common.base.Strings;

import org.apache.commons.lang3.StringUtils;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.status.router.StatusRouterStateTile;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

public class StatusRouterStateTileOpenWrt extends StatusRouterStateTile {

    private static final String LOG_TAG = StatusRouterStateTileOpenWrt.class.getSimpleName();

    public StatusRouterStateTileOpenWrt(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router);
    }

    @Nullable
    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param id   The ID whose loader is to be created.
     * @param args Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Override
    protected Loader<NVRAMInfo> getLoader(final int id, final Bundle args) {
        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {

                try {
                    Log.d(LOG_TAG, "Init background loader for " + StatusRouterStateTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                        //Skip run
                        Log.d(LOG_TAG, "Skip loader run");
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    final NVRAMInfo nvramInfo = new NVRAMInfo();

                    NVRAMInfo nvramInfoTmp = null;
                    try {
                        nvramInfoTmp =
                                SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity, mRouter,
                                        mGlobalPreferences, NVRAMInfo.ROUTER_NAME,
                                        NVRAMInfo.WAN_IPADDR,
                                        NVRAMInfo.MODEL,
                                        NVRAMInfo.DIST_TYPE,
                                        NVRAMInfo.LAN_IPADDR);
                    } finally {
                        if (nvramInfoTmp != null) {
                            nvramInfo.putAll(nvramInfoTmp);
                        }
                        //date -d @$(( $(date +%s) - $(cut -f1 -d. /proc/uptime) ))
                        //date -d @$(sed -n '/^btime /s///p' /proc/stat)

                        //Add FW, Kernel and Uptime
                        final String[] otherCmds = SSHUtils
                                .getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                        //date
                                        "date",
                                        //date since last reboot
                                        "date -d @$(( $(date +%s) - $(cut -f1 -d. /proc/uptime) ))",
                                        //elapsed from current date
                                        "uptime | awk -F'up' '{print $2}' | awk -F'load' '{print $1}'",
                                        "uname -a",
                                        "echo \"`cat /tmp/loginprompt|grep DD-WRT|cut -d' ' -f1` `cat /tmp/loginprompt|grep DD-WRT|cut -d' ' -f2` (`cat /tmp/loginprompt|grep Release|cut -d' ' -f2`) " +
                                                "`cat /tmp/loginprompt|grep DD-WRT|cut -d' ' -f3` - SVN rev: `/sbin/softwarerevision`\"");

                        if (otherCmds != null) {
                            if (otherCmds.length >= 1) {
                                //date
                                nvramInfo.setProperty(NVRAMInfo.CURRENT_DATE, otherCmds[0]);
                            }
                            if (otherCmds.length >= 3) {
                                String uptime = otherCmds[1];
                                final String uptimeCmd = otherCmds[2];
                                if (!Strings.isNullOrEmpty(uptimeCmd)) {
                                    final String elapsedFromUptime = Utils.removeLastChar(uptimeCmd.trim());
                                    if (!Strings.isNullOrEmpty(elapsedFromUptime)) {
                                        uptime += ("\n(up " + elapsedFromUptime + ")");
                                    }
                                }
                                nvramInfo.setProperty(NVRAMInfo.UPTIME, uptime);
                            }

                            if (otherCmds.length >= 4) {
                                //Kernel
                                nvramInfo.setProperty(NVRAMInfo.KERNEL,
                                        StringUtils.replace(
                                                StringUtils.replace(otherCmds[3], "GNU/Linux", ""),
                                                nvramInfo.getProperty(NVRAMInfo.ROUTER_NAME), ""));
                            }

                            if (otherCmds.length >= 5) {
                                //Firmware
                                nvramInfo.setProperty(NVRAMInfo.FIRMWARE, otherCmds[4]);
                            }
                        }
                    }

                    if (nvramInfo.isEmpty()) {
                        throw new DDWRTNoDataException("No Data!");
                    }

                    return nvramInfo;
                } catch (@NonNull final Exception e) {
                    e.printStackTrace();
                    return new NVRAMInfo().setException(e);
                }

            }
        };
    }
}
