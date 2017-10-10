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
package org.rm3l.router_companion.resources

import com.google.common.collect.FluentIterable
import com.google.common.collect.Sets

class ClientDevices {
    private val devices = Sets.newHashSet<Device>()

    private var exception: Exception? = null

    private var activeClientsNum: Int = 0
    private var activeDhcpLeasesNum: Int = 0
    var activeIPConnections: Int = 0

    fun getActiveClientsNum(): Int {
        return activeClientsNum
    }

    fun setActiveClientsNum(activeClientsNum: Int): ClientDevices {
        this.activeClientsNum = activeClientsNum
        return this
    }

    fun getActiveDhcpLeasesNum(): Int {
        return activeDhcpLeasesNum
    }

    fun setActiveDhcpLeasesNum(activeDhcpLeasesNum: Int): ClientDevices {
        this.activeDhcpLeasesNum = activeDhcpLeasesNum
        return this
    }

    fun getDevices(): Set<Device> {
        return devices
    }

    val devicesCount: Int
        get() = devices.size

    fun getDevices(max: Int): Set<Device> {
        return FluentIterable.from(devices).limit(max).toSet()
    }

    fun addDevice(device: Device): ClientDevices {
        this.devices.add(device)
        return this
    }

    fun getException(): Exception? {
        return exception
    }

    fun setException(exception: Exception): ClientDevices {
        this.exception = exception
        return this
    }

    override fun toString(): String {
        return "Devices{devices=$devices, exception=$exception}"
    }
}
