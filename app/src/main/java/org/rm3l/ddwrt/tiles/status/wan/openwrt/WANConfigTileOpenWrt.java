package org.rm3l.ddwrt.tiles.status.wan.openwrt;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;

import com.google.common.base.Splitter;

import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.resources.conn.openwrt.UCIInfo;
import org.rm3l.ddwrt.tiles.status.wan.WANConfigTile;
import org.rm3l.ddwrt.utils.SSHUtils;

import java.util.List;

public class WANConfigTileOpenWrt extends WANConfigTile {

    private static final String LOG_TAG = WANConfigTileOpenWrt.class.getSimpleName();

    public WANConfigTileOpenWrt(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
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

            @SuppressWarnings("ConstantConditions")
            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {

                try {
                    Log.d(LOG_TAG, "Init background loader for " + WANConfigTile.class + ": routerInfo=" +
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
                        nvramInfoTmp = SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity, mRouter,
                                mGlobalPreferences, NVRAMInfo.WAN_PROTO,
                                NVRAMInfo.WAN_3_G_SIGNAL,
                                NVRAMInfo.WAN_HWADDR,
                                NVRAMInfo.WAN_LEASE,
                                NVRAMInfo.WAN_IPADDR,
                                NVRAMInfo.WAN_NETMASK,
                                NVRAMInfo.WAN_GATEWAY,
                                NVRAMInfo.WAN_DNS);
                    } finally {
                        if (nvramInfoTmp != null) {
                            nvramInfo.putAll(nvramInfoTmp);
                        }

                        final UCIInfo uciNetworkInfo = SSHUtils.getUCIInfoFromOpenWrtRouter(mParentFragmentActivity, mRouter,
                                mGlobalPreferences,
                                "/sbin/uci -P/var/state show network");

                        String wanUptimeStr = null;
                        if (uciNetworkInfo != null) {
                            nvramInfo.setProperty(NVRAMInfo.WAN_IPADDR, uciNetworkInfo.getProperty(UCIInfo.NETWORK_WAN_IPADDR, "-"));
                            nvramInfo.setProperty(NVRAMInfo.WAN_NETMASK, uciNetworkInfo.getProperty(UCIInfo.NETWORK_WAN_NETMASK, "-"));
                            nvramInfo.setProperty(NVRAMInfo.WAN_GATEWAY, uciNetworkInfo.getProperty(UCIInfo.NETWORK_WAN_GATEWAY, "-"));
                            nvramInfo.setProperty(NVRAMInfo.WAN_DNS, uciNetworkInfo.getProperty(UCIInfo.NETWORK_WAN_DNS, "-"));
                            wanUptimeStr = uciNetworkInfo.getProperty(UCIInfo.NETWORK_WAN_CONNECT_TIME);
                        }

                        //Connection Uptime is stored in /tmp/.wanuptime and sys uptime from /proc/uptime
                        final String[] uptimes = SSHUtils
                                .getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences, "/bin/cat /proc/uptime");
                        if (wanUptimeStr != null && uptimes != null && uptimes.length >= 1) {

                            /*
                             * /proc/uptime Shows how long the system has been on since it was last restarted.
                             * The first number is the total number of seconds the system has been up.
                             * The second number is how much of that time the machine has spent idle, in seconds.
                             * On multi core systems (and some linux versions), the second number is the sum of
                             * the idle time accumulated by each CPU.
                             */
                            final List<String> sysUptimeSplitList = Splitter.on(" ").
                                    trimResults().omitEmptyStrings().splitToList(uptimes[0]);
                            if (sysUptimeSplitList != null && !sysUptimeSplitList.isEmpty()) {
                                try {
                                    final float uptime = Float.valueOf(sysUptimeSplitList.get(0)) - Float.valueOf(wanUptimeStr);
                                    final int days = (int) uptime / (60 * 60 * 24);

                                    String wanConnectionUptimeStr = "";
                                    if (days > 0) {
                                        wanConnectionUptimeStr += String.format("%d day%s, ",
                                                days, (days == 1 ? "" : "s"));
                                    }
                                    final int minutes = (int) uptime / 60;
                                    wanConnectionUptimeStr += String.format("%d:%02d:%02d",
                                            (minutes / 60) % 24, minutes % 60, (int) uptime % 60);

                                    nvramInfo.setProperty(NVRAMInfo.WAN_CONNECTION_UPTIME, wanConnectionUptimeStr);

                                } catch (final NumberFormatException nfe) {
                                    nfe.printStackTrace();
                                    //No Worries - WAN Uptime will be marked as "-"
                                }
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
