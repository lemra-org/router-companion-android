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
 * Contact Info: Armel Soro <armel+router_companion@rm3l.org>
 */

package org.rm3l.router_companion.fragments.admin;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import org.rm3l.router_companion.fragments.AbstractBaseFragment;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.tiles.admin.nvram.AdminNVRAMTile;

/**
 * 'Admin > NVRAM' fragment
 *
 * <p>
 */
public class AdminNVRAMFragment extends AbstractBaseFragment {

  private List<DDWRTTile> tiles = null;

  @Override
  protected boolean canChildScrollUp() {
    final List<DDWRTTile> tiles = this.getTiles(null);
    if (tiles == null || tiles.isEmpty()) {
      return false;
    }
    final DDWRTTile tile = tiles.get(0);
    return (tile instanceof AdminNVRAMTile && ((AdminNVRAMTile) tile).canChildScrollUp());
  }

  @NonNull
  protected int getRootViewType() {
    return RootViewType_LINEAR_LAYOUT;
  }

  @Nullable
  @Override
  protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
    if (tiles == null) {
      tiles = Collections.singletonList(new AdminNVRAMTile(this, savedInstanceState, this.router));
    }
    return tiles;
  }
}
