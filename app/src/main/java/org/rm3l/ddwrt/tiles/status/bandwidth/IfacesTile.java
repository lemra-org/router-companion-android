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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Throwables;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.status.wan.WANConfigTile;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.SSHUtils;

import java.util.List;

import static org.rm3l.ddwrt.fragments.status.StatusWirelessFragment.SPLITTER;

/**
 *
 */
public class IfacesTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = IfacesTile.class.getSimpleName();

    public IfacesTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, Router router) {
        super(parentFragment, arguments, router, R.layout.tile_status_bandwidth_ifaces, R.id.tile_status_bandwidth_ifaces_togglebutton);
    }

    @Override
    public int getTileHeaderViewId() {
        return R.id.tile_status_bandwidth_ifaces_hdr;
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

                    final NVRAMInfo nvramInfo = new NVRAMInfo();

                    NVRAMInfo nvramInfoTmp = null;
                    try {
                        nvramInfoTmp = SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity, mRouter,
                                mGlobalPreferences, NVRAMInfo.LAN_IFNAME,
                                NVRAMInfo.WAN_IFNAME,
                                NVRAMInfo.LANDEVS);
                    } finally {
                        if (nvramInfoTmp != null) {
                            nvramInfo.putAll(nvramInfoTmp);
                        }

                        String landevs = nvramInfo.getProperty(NVRAMInfo.LANDEVS, null);
                        if (landevs != null) {
                            final List<String> splitToList =
                                    SPLITTER.splitToList(landevs);
                            if (splitToList != null && !splitToList.isEmpty()) {

                                for (final String landev : splitToList) {
                                    if (landev == null || !(landev.startsWith("wl") || landev.startsWith("ath"))) {
                                        continue;
                                    }
                                    //Also get Virtual Interfaces
                                    try {
                                        final String landevVifsKeyword = landev + "_vifs";
                                        final NVRAMInfo landevVifsNVRAMInfo = SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity, mRouter,
                                                mGlobalPreferences,
                                                landevVifsKeyword);
                                        if (landevVifsNVRAMInfo == null) {
                                            continue;
                                        }
                                        final String landevVifsNVRAMInfoProp = landevVifsNVRAMInfo.getProperty(landevVifsKeyword, DDWRTCompanionConstants.EMPTY_STRING);
                                        if (landevVifsNVRAMInfoProp == null) {
                                            continue;
                                        }
                                        final List<String> list = SPLITTER.splitToList(landevVifsNVRAMInfoProp);
                                        if (list == null) {
                                            continue;
                                        }
                                        for (final String landevVif : list) {
                                            if (landevVif == null || landevVif.isEmpty()) {
                                                continue;
                                            }
                                            landevs += (" " + landevVif);
                                        }
                                    } catch (final Exception e) {
                                        e.printStackTrace();
                                        //No worries
                                    }
                                }
                            }

                            nvramInfo.setProperty(NVRAMInfo.LANDEVS, landevs);
                        }

                    }

                    if (nvramInfo.isEmpty()) {
                        throw new DDWRTNoDataException("No Data");
                    }

                    return nvramInfo;

                } catch (@NonNull final Exception e) {
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
    public void onLoadFinished(@NonNull Loader<NVRAMInfo> loader, @Nullable NVRAMInfo data) {

        //Set tiles
        Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

        layout.findViewById(R.id.tile_status_bandwidth_ifaces_loading_view)
                .setVisibility(View.GONE);
        layout.findViewById(R.id.tile_status_bandwidth_ifaces_gridLayout)
                .setVisibility(View.VISIBLE);

        if (data == null) {
            data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
        }

        final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_bandwidth_ifaces_error);

        final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

            if (exception == null) {
                errorPlaceHolderView.setVisibility(View.GONE);
            }

            //LAN
            final TextView lanIfaceView = (TextView) this.layout.findViewById(R.id.tile_status_bandwidth_ifaces_lan);
            lanIfaceView.setText(data.getProperty(NVRAMInfo.LAN_IFNAME, "-"));

            //WAN
            final TextView wanIfaceView = (TextView) this.layout.findViewById(R.id.tile_status_bandwidth_ifaces_wan);
            wanIfaceView.setText(data.getProperty(NVRAMInfo.WAN_IFNAME, "-"));

            //Wireless
            final TextView wlIfaceView = (TextView) this.layout.findViewById(R.id.tile_status_bandwidth_ifaces_wireless);
            wlIfaceView.setText(data.getProperty(NVRAMInfo.LANDEVS, "-"));
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
    protected OnClickIntent getOnclickIntent() {
        //TODO
        return null;
    }
}
