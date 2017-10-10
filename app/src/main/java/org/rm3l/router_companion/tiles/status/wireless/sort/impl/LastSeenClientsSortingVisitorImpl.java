package org.rm3l.router_companion.tiles.status.wireless.sort.impl;

import static com.google.common.base.Strings.nullToEmpty;

import android.support.annotation.NonNull;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Ordering;
import java.util.Set;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.resources.Device;
import org.rm3l.router_companion.tiles.status.wireless.sort.ClientsSortingVisitor;

public class LastSeenClientsSortingVisitorImpl implements ClientsSortingVisitor {

    @NonNull
    private final Ordering<Device> lastSeenOrdering;

    public LastSeenClientsSortingVisitorImpl(final int lastSeenFlagSort) {
        switch (lastSeenFlagSort) {
            case R.id.tile_status_wireless_clients_sort_seen_recently:
                lastSeenOrdering = new Ordering<Device>() {
                    @Override
                    public int compare(Device left, Device right) {
                        if (left == right) {
                            return 0;
                        }
                        if (left == null) {
                            return 1;
                        }
                        if (right == null) {
                            return -1;
                        }
                        final int lastSeenComparison =
                                Long.valueOf(right.getLastSeen()).compareTo(left.getLastSeen());
                        if (lastSeenComparison == 0) {
                            return Ordering.natural()
                                    .compare(nullToEmpty(left.getName()).toLowerCase(),
                                            nullToEmpty(right.getName()).toLowerCase());
                        }
                        return lastSeenComparison;
                    }
                };
                break;
            case R.id.tile_status_wireless_clients_sort_not_seen_recently:
                lastSeenOrdering = new Ordering<Device>() {
                    @Override
                    public int compare(Device left, Device right) {
                        if (left == right) {
                            return 0;
                        }
                        if (left == null) {
                            return -1;
                        }
                        if (right == null) {
                            return 1;
                        }
                        final int lastSeenComparison =
                                Long.valueOf(left.getLastSeen()).compareTo(right.getLastSeen());
                        if (lastSeenComparison == 0) {
                            return Ordering.natural()
                                    .compare(nullToEmpty(left.getName()).toLowerCase(),
                                            nullToEmpty(right.getName()).toLowerCase());
                        }
                        return lastSeenComparison;
                    }
                };
                break;
            default:
                throw new IllegalArgumentException(
                        "Only 'Seen Recently' or 'Not Seen Recently' flag sorting are accepted here!");
        }
    }

    @NonNull
    @Override
    public Set<Device> visit(@NonNull Set<Device> devices) {
        return FluentIterable.from(devices).toSortedSet(lastSeenOrdering);
    }
}
