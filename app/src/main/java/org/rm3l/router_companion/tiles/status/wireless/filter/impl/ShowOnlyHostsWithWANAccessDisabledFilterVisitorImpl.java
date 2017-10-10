package org.rm3l.router_companion.tiles.status.wireless.filter.impl;

import android.support.annotation.NonNull;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import java.util.Set;
import org.rm3l.router_companion.resources.Device;
import org.rm3l.router_companion.tiles.status.wireless.filter.ClientsFilterVisitor;

public class ShowOnlyHostsWithWANAccessDisabledFilterVisitorImpl implements ClientsFilterVisitor {

    private boolean mShowOnlyWanAccessDisabledHosts;

    public ShowOnlyHostsWithWANAccessDisabledFilterVisitorImpl(
            boolean showOnlyWanAccessDisabledHosts) {
        this.mShowOnlyWanAccessDisabledHosts = showOnlyWanAccessDisabledHosts;
    }

    @NonNull
    @Override
    public Set<Device> visit(@NonNull Set<Device> devices) {
        return Sets.filter(devices, new Predicate<Device>() {
            @Override
            public boolean apply(Device device) {

                return (!mShowOnlyWanAccessDisabledHosts || (device.getWanAccessState()
                        == Device.WANAccessState.WAN_ACCESS_DISABLED));
            }
        });
    }
}
