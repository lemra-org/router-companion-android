/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014-2022  Armel Soro
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
 * Contact Info: Armel Soro <armel+router_companion@rm3l.org>
 */
package org.rm3l.router_companion.tiles.status.wireless.sort.impl;

import static com.google.common.base.Strings.nullToEmpty;

import androidx.annotation.NonNull;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Ordering;
import java.util.Set;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.resources.Device;
import org.rm3l.router_companion.tiles.status.wireless.sort.ClientsSortingVisitor;

public class TopTalkersClientsSortingVisitorImpl implements ClientsSortingVisitor {

  @NonNull private final Ordering<Device> topTalkersOrdering;

  public TopTalkersClientsSortingVisitorImpl(final int topTalkersSort) {
    if (topTalkersSort == R.id.tile_status_wireless_clients_sort_top_receivers) {
      topTalkersOrdering =
          new Ordering<Device>() {
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
              final int rxTotalComparison =
                  Double.valueOf(right.getRxTotal()).compareTo(left.getRxTotal());
              if (rxTotalComparison == 0) {
                return Ordering.natural()
                    .compare(
                        nullToEmpty(left.getName()).toLowerCase(),
                        nullToEmpty(right.getName()).toLowerCase());
              }
              return rxTotalComparison;
            }
          };

    } else if (topTalkersSort == R.id.tile_status_wireless_clients_sort_top_senders) {
      topTalkersOrdering =
          new Ordering<Device>() {
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
              final int txTotalComparison =
                  Double.valueOf(right.getTxTotal()).compareTo(left.getTxTotal());
              if (txTotalComparison == 0) {
                return Ordering.natural()
                    .compare(
                        nullToEmpty(left.getName()).toLowerCase(),
                        nullToEmpty(right.getName()).toLowerCase());
              }
              return txTotalComparison;
            }
          };

    } else if (topTalkersSort
        == R.id.tile_status_wireless_clients_sort_top_receivers_current_rate) {
      topTalkersOrdering =
          new Ordering<Device>() {
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
              final int rxTotalComparison =
                  Double.valueOf(right.getRxRate()).compareTo(left.getRxRate());
              if (rxTotalComparison == 0) {
                return Ordering.natural()
                    .compare(
                        nullToEmpty(left.getName()).toLowerCase(),
                        nullToEmpty(right.getName()).toLowerCase());
              }
              return rxTotalComparison;
            }
          };

    } else if (topTalkersSort == R.id.tile_status_wireless_clients_sort_top_senders_current_rate) {
      topTalkersOrdering =
          new Ordering<Device>() {
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
              final int txTotalComparison =
                  Double.valueOf(right.getTxRate()).compareTo(left.getTxRate());
              if (txTotalComparison == 0) {
                return Ordering.natural()
                    .compare(
                        nullToEmpty(left.getName()).toLowerCase(),
                        nullToEmpty(right.getName()).toLowerCase());
              }
              return txTotalComparison;
            }
          };

    } else {
      throw new IllegalArgumentException("Only Senders or Receivers sorting are accepted here!");
    }
  }

  @NonNull
  @Override
  public Set<Device> visit(@NonNull Set<Device> devices) {
    return FluentIterable.from(devices).toSortedSet(topTalkersOrdering);
  }
}
