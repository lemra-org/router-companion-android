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

package org.rm3l.ddwrt.fragments.admin;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.fragments.AbstractBaseFragment;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.admin.nvram.AdminNVRAMTile;

import java.util.Collections;
import java.util.List;


/**
 * 'Admin > NVRAM' fragment
 * <p/>
 */
public class AdminNVRAMFragment extends AbstractBaseFragment {

    private List<DDWRTTile> tiles = null;

    @Nullable
    @Override
    protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
        if (tiles == null) {
            tiles = Collections.<DDWRTTile>
                    singletonList(new AdminNVRAMTile(this, savedInstanceState, this.router));
        }
        return tiles;
    }

    @NonNull
    protected RootViewType getRootViewType() {
        return RootViewType.LINEAR_LAYOUT;
    }

    @Override
    protected boolean canChildScrollUp() {
        final List<DDWRTTile> tiles = this.getTiles(null);
        if (tiles == null || tiles.isEmpty()) {
            return false;
        }
        final DDWRTTile tile = tiles.get(0);
        return (tile instanceof AdminNVRAMTile &&
                ((AdminNVRAMTile) tile).canChildScrollUp());
    }

}
