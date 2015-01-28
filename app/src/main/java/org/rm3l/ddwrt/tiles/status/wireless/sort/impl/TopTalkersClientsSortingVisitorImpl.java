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
package org.rm3l.ddwrt.tiles.status.wireless.sort.impl;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Ordering;

import org.jetbrains.annotations.NotNull;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.resources.Device;
import org.rm3l.ddwrt.tiles.status.wireless.sort.ClientsSortingVisitor;

import java.util.Set;

import static com.google.common.base.Strings.nullToEmpty;

public class TopTalkersClientsSortingVisitorImpl implements ClientsSortingVisitor {

    final Ordering<Device> topTalkersOrdering;

    public TopTalkersClientsSortingVisitorImpl(final int topTalkersSort) {
        switch (topTalkersSort) {
            case R.id.tile_status_wireless_clients_sort_top_receivers:
                topTalkersOrdering = new Ordering<Device>() {
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
                        final int rxTotalComparison = Double.valueOf(right.getRxTotal()).compareTo(left.getRxTotal());
                        if (rxTotalComparison == 0) {
                            return Ordering.natural().compare(nullToEmpty(left.getName()).toLowerCase(),
                                    nullToEmpty(right.getName()).toLowerCase());
                        }
                        return rxTotalComparison;
                    }
                };
                break;
            case R.id.tile_status_wireless_clients_sort_top_senders:
                topTalkersOrdering = new Ordering<Device>() {
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
                        final int txTotalComparison = Double.valueOf(right.getTxTotal()).compareTo(left.getTxTotal());
                        if (txTotalComparison == 0) {
                            return Ordering.natural().compare(nullToEmpty(left.getName()).toLowerCase(),
                                    nullToEmpty(right.getName()).toLowerCase());
                        }
                        return txTotalComparison;
                    }
                };
                break;
            default:
                throw new IllegalArgumentException("Only Senders or Receivers sorting are accepted here!");
        }
    }

    @NotNull
    @Override
    public Set<Device> visit(@NotNull Set<Device> devices) {
        return FluentIterable.from(devices).toSortedSet(topTalkersOrdering);
    }

}
