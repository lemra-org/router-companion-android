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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;

import java.util.Set;

public class ClientDevices {
    @NonNull
    private final Set<Device> devices = Sets.newHashSet();

    @Nullable
    private Exception exception;

    private int activeClientsNum;
    private int activeDhcpLeasesNum;
    private int activeIPConnections;

    public int getActiveClientsNum() {
        return activeClientsNum;
    }

    public ClientDevices setActiveClientsNum(int activeClientsNum) {
        this.activeClientsNum = activeClientsNum;
        return this;
    }

    public int getActiveDhcpLeasesNum() {
        return activeDhcpLeasesNum;
    }

    public ClientDevices setActiveDhcpLeasesNum(int activeDhcpLeasesNum) {
        this.activeDhcpLeasesNum = activeDhcpLeasesNum;
        return this;
    }

    @NonNull
    public Set<Device> getDevices() {
        return devices;
    }

    @NonNull
    public int getDevicesCount() {
        return devices.size();
    }

    @NonNull
    public Set<Device> getDevices(int max) {
        return FluentIterable
                .from(devices)
                .limit(max).toSet();
    }

    @NonNull
    public ClientDevices addDevice(Device device) {
        this.devices.add(device);
        return this;
    }

    @Nullable
    public Exception getException() {
        return exception;
    }

    @NonNull
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

    public int getActiveIPConnections() {
        return activeIPConnections;
    }

    public void setActiveIPConnections(int activeIPConnections) {
        this.activeIPConnections = activeIPConnections;
    }
}
