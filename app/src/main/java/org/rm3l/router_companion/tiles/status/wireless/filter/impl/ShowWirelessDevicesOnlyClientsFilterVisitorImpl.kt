package org.rm3l.router_companion.tiles.status.wireless.filter.impl

import com.google.common.base.Predicate
import com.google.common.collect.Sets
import org.rm3l.router_companion.resources.Device
import org.rm3l.router_companion.tiles.status.wireless.filter.ClientsFilterVisitor

/**
 * Created by rm3l on 30/08/15.
 */
class ShowWirelessDevicesOnlyClientsFilterVisitorImpl(private val showWirelessOnly: Boolean) :
    ClientsFilterVisitor {

    override fun visit(devices: Set<Device>) =
        devices.filter {
            when (showWirelessOnly) {
                false -> true
                true -> it.wirelessConnectionInfo != null
            }
        }.toSet()
}
