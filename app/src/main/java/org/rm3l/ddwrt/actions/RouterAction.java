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
package org.rm3l.ddwrt.actions;

public enum RouterAction {

//    public static final int REBOOT = 1;
//    public static final int BACKUP = 2;
//    public static final int RESTORE = 3;
//    public static final int RESTORE_FACTORY_DEFAULTS = 4;
//    public static final int SET_NVRAM_VARIABLES = 5;
//    public static final int UPGRADE_FIRMWARE = 6;
//    public static final int WAKE_ON_LAN = 7;
//    public static final int RESET_COUNTERS = 8;
//    public static final int DISABLE_WAN_ACCESS = 9;
//    public static final int ENABLE_WAN_ACCESS = 10;
//    public static final int PING = 11;
//    public static final int ARPING = 12;
//    public static final int TRACEROUTE = 13;
//    public static final int NSLOOKUP = 14;
//    public static final int CMD_SHELL = 15;
//    public static final int WHOIS = 16;
//    public static final int MAC_OUI_LOOKUP = 17;
//    public static final int BACKUP_WAN_TRAFF = 18;
//    public static final int RESTORE_WAN_TRAFF = 19;
//    public static final int DELETE_WAN_TRAFF = 20;
//    public static final int TOGGLE_WL_RADIO = 21;
//    public static final int DHCP_RELEASE = 22;
//    public static final int DHCP_RENEW = 23;
//    public static final int TOGGLE_PHY_IFACE_STATE = 24;
//
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
    PING("Ping"),
    ARPING("Arping"),
    TRACEROUTE("Traceroute"),
    NSLOOKUP("NSLookup"),
    CMD_SHELL("Execute Command"),
    WHOIS("WHOIS"),
    MAC_OUI_LOOKUP("MAC OUI Lookup"),
    BACKUP_WAN_TRAFF("Backup WAN Monthly Traffic"),
    RESTORE_WAN_TRAFF("Restore WAN Monthly Traffic"),
    DELETE_WAN_TRAFF("Delete WAN Monthly Traffic"),
    TOGGLE_WL_RADIO("Toggle Wireless Radio"),
    DHCP_RELEASE("DHCP Release"),
    DHCP_RENEW("DHCP Renew"),
    TOGGLE_PHY_IFACE_STATE("Toggle Physical Interface State");

    private final String displayName;

    RouterAction(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
