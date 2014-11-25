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

package org.rm3l.ddwrt.resources.conn;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.resources.RouterData;

import java.io.Serializable;
import java.util.Properties;

/**
 * Wrapper around NVRAM info fetched from a given {@link Router}.
 * Data comes in as a Properties object.
 *
 * @author <a href="mailto:apps+ddwrt@rm3l.org">Armel S.</a>
 */
public class NVRAMInfo extends RouterData<Properties> implements Serializable {

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

    /**
     * Default constructor: initialized an empty Properties set
     */
    public NVRAMInfo() {
        super();
        super.setData(new Properties());
    }

    /**
     * Set a property
     *
     * @param name  the property name
     * @param value the property value
     */
    @SuppressWarnings("ConstantConditions")
    public void setProperty(@NotNull final String name, @NotNull final String value) {
        super.getData().setProperty(name, value);
    }

    /**
     * Get a property value
     *
     * @param name the property name
     * @return the property value
     */
    @Nullable
    @SuppressWarnings("ConstantConditions")
    public String getProperty(@NotNull final String name) {
        return this.getProperty(name, null);
    }

    /**
     * Get a property value, defaulting to another value if none was found
     *
     * @param name         the property name
     * @param defaultValue the default value to return if none was found
     * @return the property value
     */
    @Nullable
    @SuppressWarnings("ConstantConditions")
    public String getProperty(@NotNull final String name, @Nullable final String defaultValue) {
        return super.getData().getProperty(name, defaultValue);
    }

    /**
     * @return the string representation
     */
    @NotNull
    @Override
    public String toString() {
        return super.toString();
    }

    /**
     * Set an exception
     *
     * @param exception the exception to set
     * @return the current object
     */
    @NotNull
    public NVRAMInfo setException(@Nullable final Exception exception) {
        super.setException(exception);
        return this;
    }

    /**
     * Import from an NVRAMInfo properties
     *
     * @param nvramInfo the nvram info properties to import
     */
    @SuppressWarnings("ConstantConditions")
    public void putAll(@Nullable final NVRAMInfo nvramInfo) {
        if (nvramInfo == null) {
            return;
        }
        super.getData().putAll(nvramInfo.getData());
    }

    /**
     * Check whether the current properties set is empty
     *
     * @return <code>true</code> if the properties are empty, <code>false</code> otherwise
     */
    @SuppressWarnings("ConstantConditions")
    public boolean isEmpty() {
        return super.getData().isEmpty();
    }
}
