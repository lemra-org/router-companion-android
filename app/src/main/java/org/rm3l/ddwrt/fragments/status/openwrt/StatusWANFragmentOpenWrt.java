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
package org.rm3l.ddwrt.fragments.status.openwrt;

import android.os.Bundle;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.fragments.status.StatusWANFragment;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.status.wan.WANMonthlyTrafficTile;
import org.rm3l.ddwrt.tiles.status.wan.openwrt.WANConfigTileOpenWrt;
import org.rm3l.ddwrt.tiles.status.wan.openwrt.WANTrafficTileOpenWrt;

import java.util.Arrays;
import java.util.List;

public class StatusWANFragmentOpenWrt extends StatusWANFragment {

    //TODO
    @Nullable
    @Override
    protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
        return Arrays.<DDWRTTile>asList(
                new WANConfigTileOpenWrt(this, savedInstanceState, this.router),
                new WANTrafficTileOpenWrt(this, savedInstanceState, this.router),
                new WANMonthlyTrafficTile(this, savedInstanceState, this.router)
        );
    }
}