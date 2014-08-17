package org.lemra.dd_wrt.api.conn;

import java.io.Serializable;
import java.util.Properties;

/**
 * Created by armel on 8/16/14.
 */
public class NVRAMInfo extends Properties implements Serializable  {

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

    public NVRAMInfo() {
        super();
    }

    @Override
    public synchronized String toString() {
        return super.toString();
    }
}
