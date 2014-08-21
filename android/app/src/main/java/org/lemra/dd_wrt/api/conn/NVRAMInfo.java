package org.lemra.dd_wrt.api.conn;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Properties;

/**
 * Created by armel on 8/16/14.
 */
public class NVRAMInfo extends Properties implements Serializable {

    public static final String ROUTER_NAME = "router_name";
    public static final String WAN_IPADDR = "wan_ipaddr";
    public static final String MODEL = "DD_BOARD";
    public static final String DIST_TYPE = "dist_type";
    public static final String LAN_IPADDR = "lan_ipaddr";
    public static final String FIRMWARE = "firmware";
    public static final String KERNEL = "kernel";
    public static final String UPTIME = "uptime";
    public static final String CPU_MODEL = "cpu_model";
    public static final String CPU_CORES_COUNT = "cpu_cores_count";
    public static final String LOAD_AVERAGE = "load_average";
    public static final String CPU_CLOCK_FREQ = "clkfreq";
    public static final String MEMORY_USED = "memory_used";
    public static final String MEMORY_FREE = "memory_free";
    public static final String MEMORY_TOTAL = "memory_total";
    public static final String WAN_PROTO = "wan_proto";
    public static final String WAN_HWADDR = "wan_hwaddr";
    public static final String WAN_LEASE = "wan_lease";
    public static final String WAN_NETMASK = "wan_netmask";
    public static final String WAN_GATEWAY = "wan_gateway";
    public static final String WAN_DNS = "wan_get_dns";
    public static final String WAN_3_G_SIGNAL = "wan_3g_signal";
    public static final String WAN_DEFAULT = "wan_default";

    @Nullable
    private Exception exception;

    public NVRAMInfo() {
        super();
    }

    @Override
    public synchronized String toString() {
        return super.toString();
    }

    public Exception getException() {
        return exception;
    }

    public NVRAMInfo setException(@Nullable final Exception exception) {
        this.exception = exception;
        return this;
    }
}
