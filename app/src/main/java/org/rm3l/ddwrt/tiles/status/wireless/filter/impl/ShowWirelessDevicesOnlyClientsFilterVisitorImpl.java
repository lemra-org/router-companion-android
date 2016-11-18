package org.rm3l.ddwrt.tiles.status.wireless.filter.impl;

import android.support.annotation.NonNull;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

import org.rm3l.ddwrt.resources.Device;
import org.rm3l.ddwrt.tiles.status.wireless.filter.ClientsFilterVisitor;

import java.util.Set;

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