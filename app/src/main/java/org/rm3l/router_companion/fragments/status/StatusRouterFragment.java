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

package org.rm3l.router_companion.fragments.status;

import android.os.Bundle;
import androidx.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import org.rm3l.router_companion.fragments.AbstractBaseFragment;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.tiles.status.router.StatusRouterCPUTile;
import org.rm3l.router_companion.tiles.status.router.StatusRouterMemoryTile;
import org.rm3l.router_companion.tiles.status.router.StatusRouterSpaceUsageTile;
import org.rm3l.router_companion.tiles.status.router.StatusRouterStateTile;

/** */
public class StatusRouterFragment extends AbstractBaseFragment {

  @Nullable
  @Override
  protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
    return Arrays.asList(
        new StatusRouterStateTile(this, savedInstanceState, this.router),
        new StatusRouterCPUTile(this, savedInstanceState, this.router),
        new StatusRouterMemoryTile(this, savedInstanceState, this.router),
        new StatusRouterSpaceUsageTile(this, savedInstanceState, this.router));
  }
}
