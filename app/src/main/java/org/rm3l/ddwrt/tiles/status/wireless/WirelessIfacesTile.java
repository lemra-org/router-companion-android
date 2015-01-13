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

package org.rm3l.ddwrt.tiles.status.wireless;

import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;

import com.actionbarsherlock.app.SherlockFragment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.status.bandwidth.IfacesTile;

public class WirelessIfacesTile extends IfacesTile {

    private static final String TAG = WirelessIfacesTile.class.getSimpleName();

    public WirelessIfacesTile(@NotNull SherlockFragment parentFragment, @NotNull Bundle arguments, Router router) {
        super(parentFragment, arguments, router);
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onLoadFinished(@NotNull Loader<NVRAMInfo> loader, @Nullable NVRAMInfo data) {
        super.onLoadFinished(loader, data);
        //Hide Non-wireless lines
        final int[] viewsToHide = new int[] {
                R.id.tile_status_bandwidth_ifaces_lan_title,
                R.id.tile_status_bandwidth_ifaces_lan_separator,
                R.id.tile_status_bandwidth_ifaces_lan,
                R.id.tile_status_bandwidth_ifaces_wan,
                R.id.tile_status_bandwidth_ifaces_wan_separator,
                R.id.tile_status_bandwidth_ifaces_wan_title
        };
        for (final int viewToHide : viewsToHide) {
            this.layout.findViewById(viewToHide).setVisibility(View.GONE);
        }
    }

    @Nullable
    @Override
    protected OnClickIntent getOnclickIntent() {
        return super.getOnclickIntent();
    }
}
