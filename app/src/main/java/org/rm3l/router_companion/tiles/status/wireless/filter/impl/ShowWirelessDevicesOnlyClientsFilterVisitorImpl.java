package org.rm3l.router_companion.tiles.status.wireless.filter.impl;

import androidx.annotation.NonNull;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import java.util.Set;
import org.rm3l.router_companion.resources.Device;
import org.rm3l.router_companion.tiles.status.wireless.filter.ClientsFilterVisitor;

/**
 * Created by rm3l on 30/08/15.
 */
public class ShowWirelessDevicesOnlyClientsFilterVisitorImpl implements ClientsFilterVisitor {

    private final boolean showWirelessOnly;

    public ShowWirelessDevicesOnlyClientsFilterVisitorImpl(boolean showWirelessOnly) {
        this.showWirelessOnly = showWirelessOnly;
    }

    @NonNull
    @Override
    public Set<Device> visit(@NonNull Set<Device> devices) {
        return Sets.filter(devices, new Predicate<Device>() {
            @Override
            public boolean apply(Device device) {
                if (!showWirelessOnly) {
                    return true;
                } else {
                    if (device.getWirelessConnectionInfo() != null) {
                        return true;
                    }
                }

                return false;
            }
        });
    }
}
