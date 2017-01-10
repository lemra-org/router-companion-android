package org.rm3l.router_companion.firmwares.impl.demo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.rm3l.router_companion.firmwares.AbstractRouterFirmwareConnector;
import org.rm3l.router_companion.firmwares.RemoteDataRetrievalListener;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by rm3l on 08/01/2017.
 */

public class DemoFirmwareConnector extends AbstractRouterFirmwareConnector {

    private final AtomicLong mGetWanPublicIpAddressCalls = new AtomicLong(0);

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

    @Nullable
    @Override
    public String getWanPublicIpAddress(@NonNull Context context,
                                        @NonNull Router router,
                                        @Nullable RemoteDataRetrievalListener dataRetrievalListener)
            throws Exception {

        final long nbRuns = mGetWanPublicIpAddressCalls.getAndIncrement();

        if (dataRetrievalListener != null) {
            dataRetrievalListener.onProgressUpdate(10);
        }

        final int a = 1 + new Random().nextInt(252);
        final int b = 1 + new Random().nextInt(252);
        final int c = 1 + new Random().nextInt(252);
        final int d = 1 + new Random().nextInt(252);

        final long modulo = nbRuns % 7;
        final String mWanPublicIP;

        if (modulo == 0) {
            mWanPublicIP = ("52.64." + c + "." + d);
        } else if (modulo == 1) {
            mWanPublicIP = ("8.8." + c + "." + d);
        } else if (modulo == 2) {
            mWanPublicIP = ("78.87." + c + "." + d);
        } else if (modulo == 3) {
            mWanPublicIP = ("34.56." + c + "." + d);
        } else if (modulo == 4) {
            mWanPublicIP = ("67.78." + c + "." + d);
        } else if (modulo == 5) {
            mWanPublicIP = (a + "." + b + "." + c + "." + d);
        } else {
            mWanPublicIP = null;
        }
        return mWanPublicIP;
    }

    @Override
    @Nullable
    public String goGetRouterModel(@NonNull Context context, @NonNull Router router) throws Exception {
        return "Demo Router Model";
    }
}
