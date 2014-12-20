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

package org.rm3l.ddwrt.resources;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Device Type: represents a device reported as a Client (Wireless/DHCP/...)
 *
 * @author <a href="mailto:apps+ddwrt@rm3l.org">Armel S.</a>
 */
public class Device implements Comparable<Device> {

    @NotNull
    private final String macAddress;

    @Nullable
    private String alias;

    @Nullable
    private String ipAddress;

    @Nullable
    private State state = State.UNKNOWN;

    @Nullable
    private String systemName;

    /**
     * Constructor
     *
     * @param macAddress the device MAC
     */
    public Device(@NotNull final String macAddress) {
        this.macAddress = macAddress;
    }

    /**
     * @return the alias
     */
    @Nullable
    public String getAlias() {
        return alias;
    }

    /**
     * Set the alias
     *
     * @param alias the alias to set
     * @return this object
     */
    @NotNull
    public Device setAlias(@Nullable final String alias) {
        this.alias = alias;
        return this;
    }

    /**
     * @return the IP
     */
    @Nullable
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Set the IP
     *
     * @param ipAddress the IP to set
     * @return this object
     */
    @NotNull
    public Device setIpAddress(@Nullable final String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    /**
     * @return the MAC
     */
    @NotNull
    public String getMacAddress() {
        return macAddress;
    }

    /**
     * @return the state
     */
    @Nullable
    public State getState() {
        return state;
    }

    /**
     * Set the state
     *
     * @param state the state to set
     * @return this object
     */
    @NotNull
    public Device setState(@Nullable final State state) {
        this.state = state;
        return this;
    }

    /**
     * @return the system name
     */
    @Nullable
    public String getSystemName() {
        return systemName;
    }

    @NotNull
    public Device setSystemName(@Nullable final String systemName) {
        this.systemName = systemName;
        return this;
    }

    @Override
    public int compareTo(@NotNull final Device device) {
        if (this.alias == null) {
            if (device.alias == null) {
                return 0;
            }
            return -1;
        }

        if (device.alias == null) {
            return 1;
        }

        return this.alias.compareTo(device.alias);
    }

    public String getName() {
        if (!isNullOrEmpty(this.alias)) {
            return this.alias;
        }
        if (!isNullOrEmpty(this.systemName)) {
            return this.systemName;
        }
        return this.macAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Device device = (Device) o;

        if (!macAddress.equals(device.macAddress))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return macAddress.hashCode();
    }

    public boolean isEnabled() {
        return this.state != State.DISABLED;
    }

    @NotNull
    public String toString() {
        return "Device[alias=" + this.alias + ",mac=" + this.macAddress + ",ip=" + this.ipAddress + "]";
    }

    enum State {
        UNKNOWN,

        ENABLED,

        DISABLED;
    }
}
