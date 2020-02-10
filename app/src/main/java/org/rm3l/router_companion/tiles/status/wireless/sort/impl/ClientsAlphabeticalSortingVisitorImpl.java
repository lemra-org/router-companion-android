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
package org.rm3l.router_companion.tiles.status.wireless.sort.impl;

import static com.google.common.base.Strings.nullToEmpty;

import androidx.annotation.NonNull;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Ordering;
import java.util.Set;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.resources.Device;
import org.rm3l.router_companion.tiles.status.wireless.sort.ClientsSortingVisitor;

public class ClientsAlphabeticalSortingVisitorImpl implements ClientsSortingVisitor {

  @NonNull private final Ordering<Device> aphabeticalOrdering;

  public ClientsAlphabeticalSortingVisitorImpl(int aphabeticalSort) {
    if (aphabeticalSort == R.id.tile_status_wireless_clients_sort_a_z) {
      aphabeticalOrdering =
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
              return Ordering.natural()
                  .compare(
                      nullToEmpty(left.getName()).toLowerCase(),
                      nullToEmpty(right.getName()).toLowerCase());
            }
          };

    } else if (aphabeticalSort == R.id.tile_status_wireless_clients_sort_z_a) {
      aphabeticalOrdering =
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
              return Ordering.natural()
                  .compare(
                      nullToEmpty(right.getName()).toLowerCase(),
                      nullToEmpty(left.getName()).toLowerCase());
            }
          };

    } else {
      throw new IllegalArgumentException("Only A-Z or Z-A are accepted for alphabetical sorting!");
    }
  }

  @NonNull
  public Set<Device> visit(@NonNull final Set<Device> devices) {
    return FluentIterable.from(devices).toSortedSet(aphabeticalOrdering);
  }
}
