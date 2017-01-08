package org.rm3l.router_companion.firmwares.impl.demo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.rm3l.router_companion.firmwares.AbstractRouterFirmwareConnector;
import org.rm3l.router_companion.firmwares.RemoteDataRetrievalListener;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;

import java.util.Random;

/**
 * Created by rm3l on 08/01/2017.
 */

public class DemoFirmwareConnector extends AbstractRouterFirmwareConnector {

    @Override
    public NVRAMInfo getDataForNetworkTopologyMapTile(
            @NonNull Context context, @NonNull Router router,
            @Nullable RemoteDataRetrievalListener dataRetrievalListener) throws Exception {
        try {
            if (dataRetrievalListener != null) {
                dataRetrievalListener.onProgressUpdate(10);
            }
            return new NVRAMInfo()
                    .setProperty(NVRAMInfo.ROUTER_NAME, "Demo Router (Test Data)")
                    .setProperty(NVRAMInfo.WAN_IPADDR, "1.2.3.4")
                    .setProperty(NVRAMInfo.LAN_IPADDR, "192.168.78.254")
                    .setProperty(NVRAMInfo.OPENVPNCL_ENABLE,
                            Integer.toString(new Random().nextInt(2)))
                    .setProperty(NVRAMInfo.OPENVPNCL_REMOTEIP, "my.remote.vpn.servi.ce")
                    .setProperty(NVRAMInfo.OPENVPNCL_REMOTEPORT, "1234")
                    .setProperty("NB_ACTIVE_CLIENTS", Integer.toString(new Random().nextInt(20)))
                    .setProperty("NB_DHCP_LEASES", Integer.toString(new Random().nextInt(30)))
                    .setProperty("INTERNET_CONNECTIVITY_PUBLIC_IP",
                            "52." + (1 + new Random().nextInt(252)) + "." +
                                    (1 + new Random().nextInt(252))
                                    + "." +
                                    (1 + new Random().nextInt(252)));
        } finally {
            if (dataRetrievalListener != null) {
                dataRetrievalListener.doRegardlessOfStatus();
            }
        }
    }

    @Override
    @Nullable
    public String goGetRouterModel(@NonNull Context context, @NonNull Router router) throws Exception {
        return "Demo Router Model";
    }
}
