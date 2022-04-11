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
 * Contact Info: Armel Soro <armel+router_companion@rm3l.org>
 */
package org.rm3l.router_companion.actions

enum class RouterAction constructor(private val displayName: String) {
    REBOOT("Reboot"),
    BACKUP("Backup"),
    RESTORE("Restore"),
    RESTORE_FACTORY_DEFAULTS("Restore Factory Defaults"),
    SET_NVRAM_VARIABLES("Set NVRAM Variables"),
    UPGRADE_FIRMWARE("Upgrade Firmware"),
    WAKE_ON_LAN("Wake On LAN (WOL)"),
    RESET_COUNTERS("Reset Bandwidth Counters"),
    DISABLE_WAN_ACCESS("Disable WAN Access"),
    ENABLE_WAN_ACCESS("Enable WAN Access"),
    DISABLE_WAN_ACCESS_POLICY("Disable WAN Access Policy"),
    ENABLE_WAN_ACCESS_POLICY("Enable WAN Access Policy"),
    PING("Ping"),
    ARPING("Arping"),
    TRACEROUTE("Traceroute"),
    NSLOOKUP("NSLookup"),
    CMD_SHELL("Execute Command"),
    WHOIS("WHOIS"),
    MAC_OUI_LOOKUP("MAC OUI Lookup"),
    SERVICE_NAMES_PORT_NUMBERS_LOOKUP("Service Names and Port Numbers Lookup"),
    BACKUP_WAN_TRAFF("Backup WAN Monthly Traffic"),
    RESTORE_WAN_TRAFF("Restore WAN Monthly Traffic"),
    DELETE_WAN_TRAFF("Delete WAN Monthly Traffic"),
    TOGGLE_WL_RADIO("Toggle Wireless Radio"),
    DHCP_RELEASE("DHCP Release"),
    DHCP_RENEW("DHCP Renew"),
    TOGGLE_PHY_IFACE_STATE("Toggle Physical Interface State"),
    CLEAR_ARP_CACHE("Clear ARP Cache"),
    CLEAR_DNS_CACHE("Clear DNS Cache"),
    START_HTTPD("Start HTTPd"),
    STOP_HTTPD("Stop HTTPd"),
    RESTART_HTTPD("Restart HTTPd"),
    HTTPD_UNKNOWN_ACTION("HTTPd : Unknown Action"),
    EXEC_CMD("Execute Command"),
    EXEC_FROM_FILE("Upload and run script");

    override fun toString(): String {
        return displayName
    }
}
