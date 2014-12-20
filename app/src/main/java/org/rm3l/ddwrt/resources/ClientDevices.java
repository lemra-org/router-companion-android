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

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.tiles.status.wireless.WirelessClientsTile;

import java.util.Set;

public class ClientDevices {
    @NotNull
    private final Set<Device> devices = Sets.newHashSet();

    @Nullable
    private Exception exception;

    @NotNull
    public Set<Device> getDevices() {
        return devices;
    }

    @NotNull
    public int getDevicesCount() {
        return devices.size();
    }

    @NotNull
    public Set<Device> getDevices(int max) {
        return FluentIterable
                .from(devices)
                .limit(max)
                .toSortedSet(WirelessClientsTile.COMPARATOR);
    }

    @NotNull
    public ClientDevices addDevice(Device device) {
        this.devices.add(device);
        return this;
    }

    @Nullable
    public Exception getException() {
        return exception;
    }

    @NotNull
    public ClientDevices setException(Exception exception) {
        this.exception = exception;
        return this;
    }

    @Override
    public String toString() {
        return "Devices{" +
                "devices=" + devices +
                ", exception=" + exception +
                '}';
    }
}
