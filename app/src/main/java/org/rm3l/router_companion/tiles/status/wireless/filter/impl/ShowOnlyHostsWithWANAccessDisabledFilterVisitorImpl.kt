package org.rm3l.router_companion.tiles.status.wireless.filter.impl

import org.rm3l.router_companion.resources.Device
import org.rm3l.router_companion.resources.Device.WANAccessState.WAN_ACCESS_DISABLED
import org.rm3l.router_companion.tiles.status.wireless.filter.ClientsFilterVisitor

class ShowOnlyHostsWithWANAccessDisabledFilterVisitorImpl(private val mShowOnlyWanAccessDisabledHosts: Boolean) :
    ClientsFilterVisitor {

    override fun visit(devices: Set<Device>) =
        devices.filter {
            !mShowOnlyWanAccessDisabledHosts || (it.wanAccessState
                    == WAN_ACCESS_DISABLED)
        }.toSet()
}
