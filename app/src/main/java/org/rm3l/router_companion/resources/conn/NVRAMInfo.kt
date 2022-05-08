/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014-2022  Armel Soro
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
 * Contact Info: Armel Soro <armel+router_companion@rm3l.org>
 */

package org.rm3l.router_companion.resources.conn

import org.rm3l.router_companion.resources.RouterData
import java.io.Serializable
import java.util.Properties

/**
 * Wrapper around NVRAM info fetched from a given [Router].
 * Data comes in as a Properties object.

 * @author [Armel S.](mailto:armel+router_companion@rm3l.org)
 */
open class NVRAMInfo : RouterData<Properties>(), Serializable {
    /**
     * Default constructor: initialized an empty Properties set
     */
    init {
        super.setData(Properties())
    }

    /**
     * Clear all properties
     */
    fun clear() {
        super.getData()?.clear()
    }

    /**
     * Set a property

     * @param name the property name
     * *
     * @param value the property value
     */
    fun setProperty(
        name: String,
        value: String
    ): NVRAMInfo {
        super.getData()?.setProperty(name, value)
        return this
    }

    /**
     * Get a property value

     * @param name the property name
     * *
     * @return the property value
     */
    fun getProperty(
        name: String
    ): String? {
        return this.getProperty(name, null)
    }

    /**
     * Get a property value, defaulting to another value if none was found

     * @param name the property name
     * *
     * @param defaultValue the default value to return if none was found
     * *
     * @return the property value
     */
    fun getProperty(
        name: String,
        defaultValue: String?
    ): String? {
        return super.getData()?.getProperty(name, defaultValue)
    }

    /**
     * @return the string representation
     */
    override fun toString(): String {
        return super.toString()
    }

    /**
     * Set an exception
     * @param exception the exception to set
     * *
     * @return the current object
     */
    override fun setException(exception: Exception?): NVRAMInfo {
        super.setException(exception)
        return this
    }

    /**
     * Import from an NVRAMInfo properties

     * @param nvramInfo the nvram info properties to import
     */
    fun putAll(nvramInfo: NVRAMInfo?) {
        val fromData: Properties? = nvramInfo?.getData()
        fromData?.let { super.getData()?.putAll(it) }
    }

    /**
     * Check whether the current properties set is empty

     * @return `true` if the properties are empty, `false` otherwise
     */
    val isEmpty: Boolean
        get() = super.getData()?.isEmpty ?: true

    companion object {

        val ROUTER_NAME = "router_name"
        val WAN_IPADDR = "wan_ipaddr"

        @JvmField
        val PUBLIC_IPADDR = "PUBLIC_IPADDR"
        val MODEL = "DD_BOARD"
        val OS_VERSION = "os_version"
        val DIST_TYPE = "dist_type"
        val LAN_IPADDR = "lan_ipaddr"
        val FIRMWARE = "firmware"
        val KERNEL = "kernel"
        val UPTIME = "uptime"
        val CPU_MODEL = "cpu_model"
        val CPU_CORES_COUNT = "cpu_cores_count"
        val LOAD_AVERAGE = "load_average"
        val CPU_CLOCK_FREQ = "clkfreq"
        val MEMORY_USED = "memory_used"
        val MEMORY_USED_PERCENT = MEMORY_USED + "_percent"
        val MEMORY_FREE = "memory_free"
        val MEMORY_FREE_PERCENT = MEMORY_FREE + "_percent"
        val MEMORY_TOTAL = "memory_total"
        val MEMORY_TOTAL_PERCENT = MEMORY_TOTAL + "_percent"
        val MEMORY_BUFFERS = "memory_buffers"
        val MEMORY_BUFFERS_PERCENT = MEMORY_BUFFERS + "_percent"
        val MEMORY_CACHED = "memory_cached"
        val MEMORY_CACHED_PERCENT = MEMORY_CACHED + "_percent"
        val MEMORY_ACTIVE = "memory_active"
        val MEMORY_ACTIVE_PERCENT = MEMORY_ACTIVE + "_percent"
        val MEMORY_INACTIVE = "memory_inactive"
        val MEMORY_INACTIVE_PERCENT = MEMORY_INACTIVE + "_percent"
        val NVRAM_USED_PERCENT = "nvram_used_percent"
        val CPU_USED_PERCENT = "cpu_used_percent"
        val STORAGE_JFFS2_USED_PERCENT = "storage_jffs2_used_percent"
        val STORAGE_CIFS_USED_PERCENT = "storage_cifs_used_percent"
        val WAN_PROTO = "wan_proto"
        val WAN_HWADDR = "wan_hwaddr"
        val WAN_LEASE = "wan_lease"
        val WAN_NETMASK = "wan_netmask"
        val WAN_GATEWAY = "wan_gateway"
        val WAN_GET_DNS = "wan_get_dns"
        val WAN_3_G_SIGNAL = "wan_3g_signal"
        val WAN_DEFAULT = "wan_default"
        val LAN_DOMAIN = "lan_domain"
        val LAN_GATEWAY = "lan_gateway"
        val LAN_HWADDR = "lan_hwaddr"
        val LAN_NETMASK = "lan_netmask"
        val LOCAL_DNS = "local_dns"
        val LAN_PROTO = "lan_proto"
        val DHCP_DNSMASQ = "dhcp_dnsmasq"
        val DHCP_START = "dhcp_start"
        val DHCP_NUM = "dhcp_num"
        val DHCP_LEASE = "dhcp_lease"
        val LANDEVS = "landevs"
        val LAN_IFNAME = "lan_ifname"
        val WAN_IFNAME = "wan_ifname"
        val WAN_IFACE = "wan_iface"
        val SYSLOG = "syslog"
        val SYSLOGD_ENABLE = "syslogd_enable"
        val NTP_ENABLE = "ntp_enable"
        val NTP_MODE = "ntp_mode"
        val NTP_SERVER = "ntp_server"
        val TIME_ZONE = "time_zone"
        val DAYLIGHT_TIME = "daylight_time"
        val CURRENT_DATE = "current_date"
        val WAN_CONNECTION_UPTIME = "wan_connection_uptime"
        val TTRAFF_ENABLE = "ttraff_enable"
        val OPENVPNCL_ENABLE = "openvpncl_enable"
        val OPENVPNCL_REMOTEIP = "openvpncl_remoteip"
        val OPENVPNCL_REMOTEPORT = "openvpncl_remoteport"
        val OPENVPNCL_TUNTAP = "openvpncl_tuntap"
        val OPENVPNCL_PROTO = "openvpncl_proto"
        val OPENVPNCL_CIPHER = "openvpncl_cipher"
        val OPENVPNCL_AUTH = "openvpncl_auth"
        val OPENVPNCL_TLSCIP = "openvpncl_tlscip"
        val OPENVPNCL_LZO = "openvpncl_lzo"
        val OPENVPNCL_NAT = "openvpncl_nat"
        val OPENVPNCL_SEC = "openvpncl_sec"
        val OPENVPNCL_BRIDGE = "openvpncl_bridge"
        val OPENVPNCL_IP = "openvpncl_ip"
        val OPENVPNCL_MASK = "openvpncl_mask"
        val OPENVPNCL_MTU = "openvpncl_mtu"
        val OPENVPNCL_FRAGMENT = "openvpncl_fragment"
        val OPENVPNCL_MSSFIX = "openvpncl_mssfix"
        val OPENVPNCL_CERTTYPE = "openvpncl_certtype"
        val OPENVPNCL_TLSAUTH = "openvpncl_tlsauth"
        val OPENVPNCL_CONFIG = "openvpncl_config"
        val OPENVPNCL_ROUTE = "openvpncl_route"
        val OPENVPNCL_PKCS_12 = "openvpncl_pkcs12"
        val OPENVPNCL_STATIC = "openvpncl_static"
        val OPENVPNCL_CA = "openvpncl_ca"
        val OPENVPNCL_CLIENT = "openvpncl_client"
        val OPENVPNCL_KEY = "openvpncl_key"
        val OPENVPNCL_ADV = "openvpncl_adv"
        val SSHD_RSA_HOST_KEY = "sshd_rsa_host_key"
        val SSHD_DSS_HOST_KEY = "sshd_dss_host_key"
        val OPENVPN_CA = "openvpn_ca"
        val OPENVPN_CLIENT = "openvpn_client"
        val OPENVPN_KEY = "openvpn_key"
        val OPENVPN_TLSAUTH = "openvpn_tlsauth"
        val OPENVPN_CRT = "openvpn_crt"
        val OPENVPN_CRL = "openvpn_crl"
        val OPENVPN_STATIC = "openvpn_static"
        val LAN_IFNAMES = "lan_ifnames"
        val WOL_HOSTS = "wol_hosts"
        val MANUAL_WOL_MAC = "manual_wol_mac"
        val MANUAL_WOL_PORT = "manual_wol_port"
        val WOL_ENABLE = "wol_enable"
        val WOL_INTERVAL = "wol_interval"
        val WOL_HOSTNAME = "wol_hostname"
        val WOL_PASSWD = "wol_passwd"
        val WOL_MACS = "wol_macs"
        val PPTPD_CLIENT_ENABLE = "pptpd_client_enable"
        val PPTPD_CLIENT_SRVIP = "pptpd_client_srvip"
        val PPTPD_CLIENT_SRVSUB = "pptpd_client_srvsub"
        val PPTPD_CLIENT_SRVSUBMSK = "pptpd_client_srvsubmsk"
        val PPTPD_CLIENT_SRVSEC = "pptpd_client_srvsec"
        val PPTPD_CLIENT_SRVMTU = "pptpd_client_srvmtu"
        val PPTPD_CLIENT_SRVMRU = "pptpd_client_srvmru"
        val PPTPD_CLIENT_NAT = "pptpd_client_nat"
        val PPTPD_CLIENT_SRVUSER = "pptpd_client_srvuser"
        val PPTPD_CLIENT_SRVPASS = "pptpd_client_srvpass"
        val PPTPD_ENABLE = "pptpd_enable"
        val PPTPD_BCRELAY = "pptpd_bcrelay"
        val PPTPD_FORCEMPPE = "pptpd_forcemppe"
        val PPTPD_LIP = "pptpd_lip"
        val PPTPD_RIP = "pptpd_rip"
        val PPTPD_AUTH = "pptpd_auth"
        val PPTPD_RADIUS = "pptpd_radius"
        val PPTPD_RADSERVER = "pptpd_radserver"
        val PPTPD_RADPORT = "pptpd_radport"
        val PPTPD_ACCTPORT = "pptpd_acctport"
        val PPTPD_RADPASS = "pptpd_radpass"
        val OPENVPN_ENABLE = "openvpn_enable"
        val OPENVPN_ONWAN = "openvpn_onwan"
        val OPENVPN_DH = "openvpn_dh"
        val OPENVPN_CONFIG = "openvpn_config"
        val HTTP_ENABLE = "http_enable"
        val HTTP_LANPORT = "http_lanport"
        val HTTP_WANPORT = "http_wanport"
        val HTTPS_ENABLE = "https_enable"
        val REMOTE_MGT_HTTPS = "remote_mgt_https"
        val WAN_DNS = "wan_dns"
        val HTTP_USERNAME = "http_username"
        val HTTP_PASSWD = "http_passwd"
        val REVISION = "rev"
        val LOGIN_PROMPT = "login_prompt"

        val INTERNET_CONNECTIVITY_PUBLIC_IP = "INTERNET_CONNECTIVITY_PUBLIC_IP"
        val NB_DHCP_LEASES = "NB_DHCP_LEASES"
        val NB_ACTIVE_CLIENTS = "NB_ACTIVE_CLIENTS"
        val TOTAL_DL_CURRENT_DAY = "TOTAL_DL_CURRENT_DAY"
        val TOTAL_UL_CURRENT_DAY = "TOTAL_UL_CURRENT_DAY"
        val TOTAL_DL_CURRENT_DAY_MB = "TOTAL_DL_CURRENT_DAY_MB"
        val TOTAL_UL_CURRENT_DAY_MB = "TOTAL_UL_CURRENT_DAY_MB"

        val UPTIME_DAYS = "UPTIME_DAYS"
        val UPTIME_HOURS = "UPTIME_HOURS"
        val UPTIME_MINUTES = "UPTIME_MINUTES"
    }
}
