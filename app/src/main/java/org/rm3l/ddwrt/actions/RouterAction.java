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

    REBOOT("Reboot"),
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
    NSLOOKUP("NSLookup");

    private final String displayName;

    RouterAction(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
