/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014  Armel Soro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact Info: Armel Soro <apps+ddwrt@rm3l.org>
 */

package org.rm3l.ddwrt.resources.conn;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
    public static final String MEMORY_USED_PERCENT = (MEMORY_USED + "_percent");
    public static final String MEMORY_FREE = "memory_free";
    public static final String MEMORY_FREE_PERCENT = MEMORY_FREE + "_percent";
    public static final String MEMORY_TOTAL = "memory_total";
    public static final String MEMORY_TOTAL_PERCENT = MEMORY_TOTAL + "_percent";
    public static final String MEMORY_BUFFERS = "memory_buffers";
    public static final String MEMORY_BUFFERS_PERCENT = MEMORY_BUFFERS + "_percent";
    public static final String MEMORY_CACHED = "memory_cached";
    public static final String MEMORY_CACHED_PERCENT = MEMORY_CACHED+ "_percent";
    public static final String MEMORY_ACTIVE = "memory_active";
    public static final String MEMORY_ACTIVE_PERCENT = MEMORY_ACTIVE + "_percent";
    public static final String MEMORY_INACTIVE = "memory_inactive";
    public static final String MEMORY_INACTIVE_PERCENT = MEMORY_INACTIVE + "_percent";
    public static final String NVRAM_USED_PERCENT = "nvram_used_percent";
    public static final String CPU_USED_PERCENT = "cpu_used_percent";
    public static final String STORAGE_JFFS2_USED_PERCENT = "storage_jffs2_used_percent";
    public static final String STORAGE_CIFS_USED_PERCENT = "storage_cifs_used_percent";
    public static final String WAN_PROTO = "wan_proto";
    public static final String WAN_HWADDR = "wan_hwaddr";
    public static final String WAN_LEASE = "wan_lease";
    public static final String WAN_NETMASK = "wan_netmask";
    public static final String WAN_GATEWAY = "wan_gateway";
    public static final String WAN_GET_DNS = "wan_get_dns";
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
    public static final String WAN_IFACE = "wan_iface";
    public static final String SYSLOG = "syslog";
    public static final String SYSLOGD_ENABLE = "syslogd_enable";
    public static final String NTP_ENABLE = "ntp_enable";
    public static final String NTP_MODE = "ntp_mode";
    public static final String NTP_SERVER = "ntp_server";
    public static final String TIME_ZONE = "time_zone";
    public static final String DAYLIGHT_TIME = "daylight_time";
    public static final String CURRENT_DATE = "current_date";
    public static final String WAN_CONNECTION_UPTIME = "wan_connection_uptime";
    public static final String TTRAFF_ENABLE = "ttraff_enable";
    public static final String OPENVPNCL_ENABLE = "openvpncl_enable";
    public static final String OPENVPNCL_REMOTEIP = "openvpncl_remoteip";
    public static final String OPENVPNCL_REMOTEPORT = "openvpncl_remoteport";
    public static final String OPENVPNCL_TUNTAP = "openvpncl_tuntap";
    public static final String OPENVPNCL_PROTO = "openvpncl_proto";
    public static final String OPENVPNCL_CIPHER = "openvpncl_cipher";
    public static final String OPENVPNCL_AUTH = "openvpncl_auth";
    public static final String OPENVPNCL_TLSCIP = "openvpncl_tlscip";
    public static final String OPENVPNCL_LZO = "openvpncl_lzo";
    public static final String OPENVPNCL_NAT = "openvpncl_nat";
    public static final String OPENVPNCL_SEC = "openvpncl_sec";
    public static final String OPENVPNCL_BRIDGE = "openvpncl_bridge";
    public static final String OPENVPNCL_IP = "openvpncl_ip";
    public static final String OPENVPNCL_MASK = "openvpncl_mask";
    public static final String OPENVPNCL_MTU = "openvpncl_mtu";
    public static final String OPENVPNCL_FRAGMENT = "openvpncl_fragment";
    public static final String OPENVPNCL_MSSFIX = "openvpncl_mssfix";
    public static final String OPENVPNCL_CERTTYPE = "openvpncl_certtype";
    public static final String OPENVPNCL_TLSAUTH = "openvpncl_tlsauth";
    public static final String OPENVPNCL_CONFIG = "openvpncl_config";
    public static final String OPENVPNCL_ROUTE = "openvpncl_route";
    public static final String OPENVPNCL_PKCS_12 = "openvpncl_pkcs12";
    public static final String OPENVPNCL_STATIC = "openvpncl_static";
    public static final String OPENVPNCL_CA = "openvpncl_ca";
    public static final String OPENVPNCL_CLIENT = "openvpncl_client";
    public static final String OPENVPNCL_KEY = \"fake-key\";
    public static final String OPENVPNCL_ADV = "openvpncl_adv";
    public static final String SSHD_RSA_HOST_KEY = \"fake-key\";
    public static final String SSHD_DSS_HOST_KEY = \"fake-key\";
    public static final String OPENVPN_CA = "openvpn_ca";
    public static final String OPENVPN_CLIENT = "openvpn_client";
    public static final String OPENVPN_KEY = \"fake-key\";
    public static final String OPENVPN_TLSAUTH = "openvpn_tlsauth";
    public static final String OPENVPN_CRT = "openvpn_crt";
    public static final String OPENVPN_CRL = "openvpn_crl";
    public static final String OPENVPN_STATIC = "openvpn_static";
    public static final String LAN_IFNAMES = "lan_ifnames";
    public static final String WOL_HOSTS = "wol_hosts";
    public static final String MANUAL_WOL_MAC = "manual_wol_mac";
    public static final String MANUAL_WOL_PORT = "manual_wol_port";
    public static final String WOL_ENABLE = "wol_enable";
    public static final String WOL_INTERVAL = "wol_interval";
    public static final String WOL_HOSTNAME = "wol_hostname";
    public static final String WOL_PASSWD = "wol_passwd";
    public static final String WOL_MACS = "wol_macs";
    public static final String PPTPD_CLIENT_ENABLE = "pptpd_client_enable";
    public static final String PPTPD_CLIENT_SRVIP = "pptpd_client_srvip";
    public static final String PPTPD_CLIENT_SRVSUB = "pptpd_client_srvsub";
    public static final String PPTPD_CLIENT_SRVSUBMSK = "pptpd_client_srvsubmsk";
    public static final String PPTPD_CLIENT_SRVSEC = "pptpd_client_srvsec";
    public static final String PPTPD_CLIENT_SRVMTU = "pptpd_client_srvmtu";
    public static final String PPTPD_CLIENT_SRVMRU = "pptpd_client_srvmru";
    public static final String PPTPD_CLIENT_NAT = "pptpd_client_nat";
    public static final String PPTPD_CLIENT_SRVUSER = "pptpd_client_srvuser";
    public static final String PPTPD_CLIENT_SRVPASS = "pptpd_client_srvpass";
    public static final String PPTPD_ENABLE = "pptpd_enable";
    public static final String PPTPD_BCRELAY = "pptpd_bcrelay";
    public static final String PPTPD_FORCEMPPE = "pptpd_forcemppe";
    public static final String PPTPD_LIP = "pptpd_lip";
    public static final String PPTPD_RIP = "pptpd_rip";
    public static final String PPTPD_AUTH = "pptpd_auth";
    public static final String PPTPD_RADIUS = "pptpd_radius";
    public static final String PPTPD_RADSERVER = "pptpd_radserver";
    public static final String PPTPD_RADPORT = "pptpd_radport";
    public static final String PPTPD_ACCTPORT = "pptpd_acctport";
    public static final String PPTPD_RADPASS = "pptpd_radpass";
    public static final String OPENVPN_ENABLE = "openvpn_enable";
    public static final String OPENVPN_ONWAN = "openvpn_onwan";
    public static final String OPENVPN_DH = "openvpn_dh";
    public static final String OPENVPN_CONFIG = "openvpn_config";
    public static final String HTTP_ENABLE = "http_enable";
    public static final String HTTP_LANPORT = "http_lanport";
    public static final String HTTP_WANPORT = "http_wanport";
    public static final String HTTPS_ENABLE = "https_enable";
    public static final String REMOTE_MGT_HTTPS = "remote_mgt_https";
    public static final String WAN_DNS = "wan_dns";
    public static final String HTTP_USERNAME = "http_username";
    public static final String HTTP_PASSWD = "http_passwd";

    /**
     * Default constructor: initialized an empty Properties set
     */
    public NVRAMInfo() {
        super();
        super.setData(new Properties());
    }

    /**
     * Clear all properties
     */
    @SuppressWarnings("ConstantConditions")
    public void clear() {
        super.getData().clear();
    }

    /**
     * Set a property
     *
     * @param name  the property name
     * @param value the property value
     */
    @NonNull
    @SuppressWarnings("ConstantConditions")
    public NVRAMInfo setProperty(@NonNull final String name, @NonNull final String value) {
        super.getData().setProperty(name, value);
        return this;
    }

    /**
     * Get a property value
     *
     * @param name the property name
     * @return the property value
     */
    @Nullable
    @SuppressWarnings("ConstantConditions")
    public String getProperty(@NonNull final String name) {
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
    public String getProperty(@NonNull final String name, @Nullable final String defaultValue) {
        return super.getData().getProperty(name, defaultValue);
    }

    /**
     * @return the string representation
     */
    @NonNull
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
    @NonNull
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
