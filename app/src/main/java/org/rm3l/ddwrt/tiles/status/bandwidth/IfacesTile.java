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

package org.rm3l.ddwrt.tiles.status.bandwidth;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.common.base.Throwables;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.status.wan.WANConfigTile;
import org.rm3l.ddwrt.utils.SSHUtils;

/**
 *
 */
public class IfacesTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = IfacesTile.class.getSimpleName();

    public IfacesTile(@NotNull SherlockFragment parentFragment, @NotNull Bundle arguments, Router router) {
        super(parentFragment, arguments, router, R.layout.tile_status_bandwidth_ifaces, R.id.tile_status_bandwidth_ifaces_togglebutton);
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_status_bandwidth_ifaces_title;
    }

    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {

                try {
                    Log.d(LOG_TAG, "Init background loader for " + WANConfigTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                        //Skip run
                        Log.d(LOG_TAG, "Skip loader run");
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    return SSHUtils.getNVRamInfoFromRouter(mRouter,
                            NVRAMInfo.LAN_IFNAME,
                            NVRAMInfo.WAN_IFNAME,
                            NVRAMInfo.LANDEVS);

                } catch (@NotNull final Exception e) {
                    e.printStackTrace();
                    return new NVRAMInfo().setException(e);
                }
            }
        };
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Override
    public void onLoadFinished(@NotNull Loader<NVRAMInfo> loader, @Nullable NVRAMInfo data) {

        //Set tiles
        Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

        layout.findViewById(R.id.tile_status_bandwidth_ifaces_loading_view)
                .setVisibility(View.GONE);
        layout.findViewById(R.id.tile_status_bandwidth_ifaces_gridLayout)
                .setVisibility(View.VISIBLE);

        if (data == null) {
            data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
        }

        @NotNull final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_bandwidth_ifaces_error);

        @Nullable final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

            if (exception == null) {
                errorPlaceHolderView.setVisibility(View.GONE);
            }

            //LAN
            @NotNull final TextView lanIfaceView = (TextView) this.layout.findViewById(R.id.tile_status_bandwidth_ifaces_lan);
            lanIfaceView.setText(data.getProperty(NVRAMInfo.LAN_IFNAME, "N/A"));

            //WAN
            @NotNull final TextView wanIfaceView = (TextView) this.layout.findViewById(R.id.tile_status_bandwidth_ifaces_wan);
            wanIfaceView.setText(data.getProperty(NVRAMInfo.WAN_IFNAME, "N/A"));

            //Wireless
            @NotNull final TextView wlIfaceView = (TextView) this.layout.findViewById(R.id.tile_status_bandwidth_ifaces_wireless);
            wlIfaceView.setText(data.getProperty(NVRAMInfo.LANDEVS, "N/A"));
        }

        if (exception != null && !(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {
            //noinspection ThrowableResultOfMethodCallIgnored
            final Throwable rootCause = Throwables.getRootCause(exception);
            errorPlaceHolderView.setText("Error: " + (rootCause != null ? rootCause.getMessage() : "null"));
            final Context parentContext = this.mParentFragmentActivity;
            errorPlaceHolderView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    //noinspection ThrowableResultOfMethodCallIgnored
                    if (rootCause != null) {
                        Toast.makeText(parentContext,
                                rootCause.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
            errorPlaceHolderView.setVisibility(View.VISIBLE);
        }

        doneWithLoaderInstance(this, loader,
                R.id.tile_status_bandwidth_ifaces_togglebutton_title, R.id.tile_status_bandwidth_ifaces_togglebutton_separator);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");

    }

    @Nullable
    @Override
    protected Intent getOnclickIntent() {
        //TODO
        return null;
    }
}
