package org.rm3l.ddwrt.tiles.status.wireless.filter.impl;

import android.support.annotation.NonNull;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

import org.rm3l.ddwrt.resources.Device;
import org.rm3l.ddwrt.tiles.status.wireless.filter.ClientsFilterVisitor;

import java.util.Set;

public class ShowOnlyHostsWithWANAccessDisabledFilterVisitorImpl implements ClientsFilterVisitor {

    private boolean mShowOnlyWanAccessDisabledHosts;

    public ShowOnlyHostsWithWANAccessDisabledFilterVisitorImpl(boolean showOnlyWanAccessDisabledHosts) {
        this.mShowOnlyWanAccessDisabledHosts = showOnlyWanAccessDisabledHosts;
    }

    @NonNull
    @Override
    public Set<Device> visit(@NonNull Set<Device> devices) {
        return Sets.filter(devices, new Predicate<Device>() {
            @Override
            public boolean apply(Device device) {

                return (!mShowOnlyWanAccessDisabledHosts ||
                        (device.getWanAccessState() == Device.WANAccessState.WAN_ACCESS_DISABLED));
            }
        });
    }
}
