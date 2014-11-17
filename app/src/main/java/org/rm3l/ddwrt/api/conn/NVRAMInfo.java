/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Armel S.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.rm3l.ddwrt.api.conn;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Properties;

/**
 * Wrapper around NVRAM infos fetched from a given {@link org.rm3l.ddwrt.api.conn.Router}.
 * Comes in as a Properties object.
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
    public static final String LAN_DOMAIN = "lan_domain";
    public static final String LAN_GATEWAY = "lan_gateway";
    public static final String LAN_HWADDR = "lan_hwaddr";
    public static final String LAN_NETMASK = "lan_netmask";
    public static final String LOCAL_DNS = "local_dns";
    public static final String LAN_PROTO = "lan_proto";
    public static final String DHCP_DNSMASQ = "dhcp_dnsmasq";
    public static final String DHCP_START = "dhcp_start";
    public static final String DHCP_NUM = "dhcp_num";
    public static final String DHCP_LEASE = "dhcp_lease";
    public static final String LANDEVS = "landevs";
    public static final String LAN_IFNAME = "lan_ifname";
    public static final String WAN_IFNAME = "wan_ifname";
    public static final String SYSLOG = "syslog";

    @Nullable
    private Exception exception;

    public NVRAMInfo() {
        super();
    }

    @Override
    public synchronized String toString() {
        return super.toString();
    }

    @Nullable
    public Exception getException() {
        return exception;
    }

    @NotNull
    public NVRAMInfo setException(@Nullable final Exception exception) {
        this.exception = exception;
        return this;
    }
}
