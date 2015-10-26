package org.rm3l.ddwrt.tiles.status.wan.openwrt;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.resources.conn.openwrt.UCIInfo;
import org.rm3l.ddwrt.tiles.status.wan.WANConfigTile;
import org.rm3l.ddwrt.tiles.status.wan.WANTrafficTile;
import org.rm3l.ddwrt.utils.SSHUtils;

import java.util.List;

public class WANTrafficTileOpenWrt extends WANTrafficTile {

    private static final String LOG_TAG = WANTrafficTileOpenWrt.class.getSimpleName();

    public WANTrafficTileOpenWrt(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router);
    }

    @Nullable
    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {

                try {
                    Log.d(LOG_TAG, "Init background loader for " + WANConfigTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (mRefreshing.getAndSet(true)) {
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    if (!isForceRefresh()) {
                        //Force Manual Refresh
                        if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                            //Skip run
                            Log.d(LOG_TAG, "Skip loader run");
                            return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                        }
                    }
                    nbRunsLoader++;

                    final NVRAMInfo nvRamInfoFromRouter = new NVRAMInfo();

                    //Start by getting information about the WAN iface name
                    final UCIInfo uciNetworkInfo = SSHUtils.getUCIInfoFromOpenWrtRouter(mParentFragmentActivity, mRouter,
                            mGlobalPreferences,
                            "/sbin/uci -P/var/state show network | grep \"" +
                                    UCIInfo.NETWORK_WAN_DEVICE +
                                    "\"");
                    if (uciNetworkInfo == null) {
                        throw new IllegalStateException("Whoops. WAN Iface could not be determined.");
                    }

                    final String property = uciNetworkInfo.getProperty(UCIInfo.NETWORK_WAN_DEVICE);
                    if (property != null) {
                        nvRamInfoFromRouter.setProperty(NVRAMInfo.WAN_IFACE, property);
                    }

                    final String wanIface = nvRamInfoFromRouter.getProperty(NVRAMInfo.WAN_IFACE);

                    if (Strings.isNullOrEmpty(wanIface)) {
                        throw new IllegalStateException("Whoops. WAN Iface could not be determined.");
                    }

                    final String[] netDevWanIfaces = SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences, "cat /proc/net/dev | grep \"" + wanIface + "\"");
                    if (netDevWanIfaces == null || netDevWanIfaces.length == 0) {
                        return null;
                    }

                    String netDevWanIface = netDevWanIfaces[0];
                    if (netDevWanIface == null) {
                        return null;
                    }

                    netDevWanIface = netDevWanIface.replaceAll(wanIface + ":", "");

                    final List<String> netDevWanIfaceList = Splitter.on(" ").omitEmptyStrings().trimResults().splitToList(netDevWanIface);
                    if (netDevWanIfaceList == null || netDevWanIfaceList.size() <= 15) {
                        return null;
                    }

                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_bytes", netDevWanIfaceList.get(0));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_packets", netDevWanIfaceList.get(1));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_errs", netDevWanIfaceList.get(2));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_drop", netDevWanIfaceList.get(3));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_fifo", netDevWanIfaceList.get(4));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_frame", netDevWanIfaceList.get(5));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_compressed", netDevWanIfaceList.get(6));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_multicast", netDevWanIfaceList.get(7));

                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_bytes", netDevWanIfaceList.get(8));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_packets", netDevWanIfaceList.get(9));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_errs", netDevWanIfaceList.get(10));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_drop", netDevWanIfaceList.get(11));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_fifo", netDevWanIfaceList.get(12));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_colls", netDevWanIfaceList.get(13));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_carrier", netDevWanIfaceList.get(14));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_compressed", netDevWanIfaceList.get(15));

                    return nvRamInfoFromRouter;

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
