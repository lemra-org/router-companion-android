package org.rm3l.ddwrt.tiles.status.router.openwrt;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.status.router.StatusRouterStateTile;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

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
                                        NVRAMInfo.DIST_TYPE,
                                        NVRAMInfo.LAN_IPADDR);
                    } finally {
                        if (nvramInfoTmp != null) {
                            nvramInfo.putAll(nvramInfoTmp);
                        }

                        //WAN IPAddress, Router Model
                        final String[] infoFromUci = SSHUtils.getManualProperty(
                                mParentFragmentActivity, mRouter, mGlobalPreferences,
                                "/sbin/uci -P/var/state show network | grep \"network.wan.ipaddr\" | /usr/bin/awk -F'=' '{print $2}' ; " +
                                        "cat /proc/diag/model"
                        );
                        if (infoFromUci != null) {
                            if (infoFromUci.length >= 1) {
                                nvramInfo.setProperty(NVRAMInfo.WAN_IPADDR, infoFromUci[0]);
                            }
                            if (infoFromUci.length >= 2) {
                                nvramInfo.setProperty(NVRAMInfo.MODEL, infoFromUci[1]);
                            }
                        }

                        String firmwareInfo = "";
                        //FW
                        String[] fwInfo = SSHUtils.getManualProperty(
                                mParentFragmentActivity, mRouter, mGlobalPreferences,
                                "cat /etc/openwrt_release | grep -E \"DISTRIB_ID.*|DISTRIB_CODENAME.*\";"
                        );
                        String codename = "";
                        if (fwInfo != null) {
                            for (int i = 0; i < fwInfo.length; i++) {
                                final String str = fwInfo[i];
                                if (StringUtils.startsWith(str, "DISTRIB_ID")) {
                                    firmwareInfo = str.replace("DISTRIB_ID=", "")
                                            .replaceAll("\"", "");
                                } else if (StringUtils.startsWith(str, "DISTRIB_CODENAME")) {
                                    codename = str.replace("DISTRIB_CODENAME=", "")
                                            .replaceAll("\"", "");
                                }
                                if (!(isNullOrEmpty(codename) || isNullOrEmpty(firmwareInfo))) {
                                    break;
                                }
                            }
                        }
                        if (!isNullOrEmpty(codename)) {
                            fwInfo = SSHUtils.getManualProperty(
                                    mParentFragmentActivity, mRouter, mGlobalPreferences,
                                    "cat /etc/banner | grep -i \"" + codename + "\";"
                            );
                            if (fwInfo != null && fwInfo.length > 0) {
                                firmwareInfo += (nullToEmpty(fwInfo[0]).replaceAll("-", ""));
                            }
                        }
                        nvramInfo.setProperty(NVRAMInfo.FIRMWARE, firmwareInfo);

                        //Add Kernel and Uptime
                        final String[] otherCmds = SSHUtils
                                .getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                        //date
                                        "/bin/date",
                                        //date since last reboot
                                        "/bin/date -D '%s' -d $(( $(/bin/date +%s) - $(/usr/bin/cut -f1 -d. /proc/uptime) ))",
                                        //elapsed from current date
                                        "/usr/bin/uptime | /usr/bin/awk -F'up' '{print $2}' | /usr/bin/awk -F'load' '{print $1}'",
                                        "/bin/uname -a");

                        if (otherCmds != null) {
                            if (otherCmds.length >= 1) {
                                //date
                                nvramInfo.setProperty(NVRAMInfo.CURRENT_DATE, otherCmds[0]);
                            }
                            if (otherCmds.length >= 3) {
                                String uptime = otherCmds[1];
                                final String uptimeCmd = otherCmds[2];
                                if (!isNullOrEmpty(uptimeCmd)) {
                                    final String elapsedFromUptime = Utils.removeLastChar(uptimeCmd.trim());
                                    if (!isNullOrEmpty(elapsedFromUptime)) {
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
