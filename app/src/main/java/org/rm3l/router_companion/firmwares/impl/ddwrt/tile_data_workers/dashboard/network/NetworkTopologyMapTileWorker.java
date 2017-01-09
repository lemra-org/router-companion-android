package org.rm3l.router_companion.firmwares.impl.ddwrt.tile_data_workers.dashboard.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Patterns;

import org.rm3l.router_companion.firmwares.RemoteDataRetrievalListener;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.service.tasks.PublicIPChangesServiceTask;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;

import static org.rm3l.router_companion.RouterCompanionAppConstants.NOK;
import static org.rm3l.router_companion.RouterCompanionAppConstants.UNKNOWN;

/**
 * Created by rm3l on 08/01/2017.
 */

public final class NetworkTopologyMapTileWorker {

    public static NVRAMInfo getDataForNetworkTopologyMapTile(@NonNull Context context,
                                                             @NonNull Router router,
                                                             @Nullable RemoteDataRetrievalListener dataRetrievalListener) 
            throws Exception {

        final SharedPreferences globalSharedPreferences = Utils.getGlobalSharedPreferences(context);

        final NVRAMInfo nvramInfo = new NVRAMInfo();

        NVRAMInfo nvramInfoTmp = null;
        try {
            nvramInfoTmp =
                    SSHUtils.getNVRamInfoFromRouter(context, router,
                            globalSharedPreferences,
                            NVRAMInfo.ROUTER_NAME,
                            NVRAMInfo.WAN_IPADDR,
                            NVRAMInfo.LAN_IPADDR,
                            NVRAMInfo.OPENVPNCL_ENABLE,
                            NVRAMInfo.OPENVPNCL_REMOTEIP,
                            NVRAMInfo.OPENVPNCL_REMOTEPORT);
        } finally {
            if (nvramInfoTmp != null) {
                nvramInfo.putAll(nvramInfoTmp);
            }
            if (dataRetrievalListener != null) {
                dataRetrievalListener.onProgressUpdate(45);
            }
            //Active clients
            final String[] activeClients = SSHUtils.getManualProperty(context, router, globalSharedPreferences,
                    "arp -a 2>/dev/null");
            if (activeClients != null) {
                nvramInfo.setProperty("NB_ACTIVE_CLIENTS", Integer.toString(activeClients.length));
            }

            if (dataRetrievalListener != null) {
                dataRetrievalListener.onProgressUpdate(60);
            }
            //Active DHCP Leases
            final String[] activeDhcpLeases = SSHUtils.getManualProperty(context, router, globalSharedPreferences,
                    "cat /tmp/dnsmasq.leases 2>/dev/null || echo \"N_A\"\"");
            if (activeDhcpLeases != null) {
                if (activeDhcpLeases.length == 0 ||
                        !"N_A".equals(activeDhcpLeases[0])) {
                    nvramInfo.setProperty("NB_DHCP_LEASES", Integer.toString(activeDhcpLeases.length));
                } else {
                    //File does not exist
                    nvramInfo.setProperty("NB_DHCP_LEASES", "-1");
                }
            }

            if (dataRetrievalListener != null) {
                dataRetrievalListener.onProgressUpdate(85);
            }

            try {

                //Check actual connections to the outside from the router
                final String[] wanPublicIpCmdStatus = SSHUtils.getManualProperty(context,
                        router, globalSharedPreferences,
                        Utils.getCommandForInternetIPResolution(context));
                if (wanPublicIpCmdStatus == null || wanPublicIpCmdStatus.length == 0) {
                    nvramInfo.setProperty("INTERNET_CONNECTIVITY_PUBLIC_IP", NOK);
                } else {
                    final String wanPublicIp = wanPublicIpCmdStatus[wanPublicIpCmdStatus.length - 1]
                            .trim();
                    if (Patterns.IP_ADDRESS.matcher(wanPublicIp).matches()) {
                        nvramInfo.setProperty("INTERNET_CONNECTIVITY_PUBLIC_IP", wanPublicIp);

                        PublicIPChangesServiceTask.buildNotificationIfNeeded(context,
                                router, router.getPreferences(context),
                                wanPublicIpCmdStatus,
                                nvramInfo.getProperty(NVRAMInfo.WAN_IPADDR), null);

                    } else {
                        nvramInfo.setProperty("INTERNET_CONNECTIVITY_PUBLIC_IP", NOK);
                    }
                }
            } catch (final Exception e) {
                e.printStackTrace();
                nvramInfo.setProperty("INTERNET_CONNECTIVITY_PUBLIC_IP", UNKNOWN);
            } finally {
                if (dataRetrievalListener != null) {
                    dataRetrievalListener.doRegardlessOfStatus();
                }
//                runBgServiceTaskAsync();
            }
        }

        return nvramInfo;
    }
}