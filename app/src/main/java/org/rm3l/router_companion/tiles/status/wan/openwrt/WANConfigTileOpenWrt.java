package org.rm3l.router_companion.tiles.status.wan.openwrt;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import android.util.Log;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.common.base.Splitter;
import java.util.List;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.resources.conn.openwrt.UCIInfo;
import org.rm3l.router_companion.tiles.status.wan.WANConfigTile;
import org.rm3l.router_companion.utils.SSHUtils;

public class WANConfigTileOpenWrt extends WANConfigTile {

    private static final String LOG_TAG = WANConfigTileOpenWrt.class.getSimpleName();

    public WANConfigTileOpenWrt(@NonNull Fragment parentFragment, @NonNull Bundle arguments,
            @Nullable Router router) {
        super(parentFragment, arguments, router);
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
                    FirebaseCrashlytics.getInstance().log( "Init background loader for "
                            + WANConfigTile.class
                            + ": routerInfo="
                            + mRouter
                            + " / nbRunsLoader="
                            + nbRunsLoader);

                    if (mRefreshing.getAndSet(true)) {
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    final NVRAMInfo nvramInfo = new NVRAMInfo();

                    NVRAMInfo nvramInfoTmp = null;
                    try {
                        nvramInfoTmp = SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity, mRouter,
                                mGlobalPreferences, NVRAMInfo.Companion.getWAN_PROTO(),
                                NVRAMInfo.Companion.getWAN_3_G_SIGNAL(), NVRAMInfo.Companion.getWAN_HWADDR(),
                                NVRAMInfo.Companion.getWAN_LEASE(), NVRAMInfo.Companion.getWAN_IPADDR(),
                                NVRAMInfo.Companion.getWAN_NETMASK(), NVRAMInfo.Companion.getWAN_GATEWAY(),
                                NVRAMInfo.Companion.getWAN_GET_DNS());
                    } finally {
                        if (nvramInfoTmp != null) {
                            nvramInfo.putAll(nvramInfoTmp);
                        }

                        final UCIInfo uciNetworkInfo =
                                SSHUtils.getUCIInfoFromOpenWrtRouter(mParentFragmentActivity, mRouter,
                                        mGlobalPreferences, "/sbin/uci -P/var/state show network");

                        String wanUptimeStr = null;
                        if (uciNetworkInfo != null) {
                            nvramInfo.setProperty(NVRAMInfo.Companion.getWAN_IPADDR(),
                                    uciNetworkInfo.getProperty(UCIInfo.Companion.getNETWORK_WAN_IPADDR(), "-"));
                            nvramInfo.setProperty(NVRAMInfo.Companion.getWAN_NETMASK(),
                                    uciNetworkInfo.getProperty(UCIInfo.Companion.getNETWORK_WAN_NETMASK(), "-"));
                            nvramInfo.setProperty(NVRAMInfo.Companion.getWAN_GATEWAY(),
                                    uciNetworkInfo.getProperty(UCIInfo.Companion.getNETWORK_WAN_GATEWAY(), "-"));
                            nvramInfo.setProperty(NVRAMInfo.Companion.getWAN_GET_DNS(),
                                    uciNetworkInfo.getProperty(UCIInfo.Companion.getNETWORK_WAN_DNS(), "-"));
                            wanUptimeStr = uciNetworkInfo.getProperty(
                                    UCIInfo.Companion.getNETWORK_WAN_CONNECT_TIME());
                        }

                        //Connection Uptime is stored in /tmp/.wanuptime and sys uptime from /proc/uptime
                        final String[] uptimes =
                                SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                        "/bin/cat /proc/uptime");
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
                                    final float uptime =
                                            Float.valueOf(sysUptimeSplitList.get(0)) - Float.valueOf(wanUptimeStr);
                                    final int days = (int) uptime / (60 * 60 * 24);

                                    String wanConnectionUptimeStr = "";
                                    if (days > 0) {
                                        wanConnectionUptimeStr +=
                                                String.format("%d day%s, ", days, (days == 1 ? "" : "s"));
                                    }
                                    final int minutes = (int) uptime / 60;
                                    wanConnectionUptimeStr +=
                                            String.format("%d:%02d:%02d", (minutes / 60) % 24, minutes % 60,
                                                    (int) uptime % 60);

                                    nvramInfo.setProperty(NVRAMInfo.Companion.getWAN_CONNECTION_UPTIME(),
                                            wanConnectionUptimeStr);
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

    @Nullable
    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }
}
