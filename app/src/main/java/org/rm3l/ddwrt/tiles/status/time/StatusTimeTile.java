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

package org.rm3l.ddwrt.tiles.status.time;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.utils.SSHUtils;

import java.util.Map;

public class StatusTimeTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = StatusTimeTile.class.getSimpleName();

    private final Map<String, String> daylightMap = Maps.newHashMapWithExpectedSize(10);

    public StatusTimeTile(@NotNull SherlockFragment parentFragment, @NotNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_status_time, R.id.tile_status_time_togglebutton);
        daylightMap.put("1", "none");
        daylightMap.put("2", "first Sun Apr - last Sun Oct");
        daylightMap.put("3", "last Sun Mar - last Sun Oct");
        daylightMap.put("4", "last Sun Oct - last Sun Mar");
        daylightMap.put("5", "2nd Sun Mar - first Sun Nov");
        daylightMap.put("6", "first Sun Oct - 3rd Sun Mar");
        daylightMap.put("7", "last Sun Sep - first Sun Apr");
        daylightMap.put("8", "3rd Sun Oct - 3rd Sun Mar");
        daylightMap.put("9", "first Sun Oct - first Sun Apr");
        daylightMap.put("10", "3rd Sun Oct - 3rd Sun Feb");
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_status_time_title;
    }

    @Nullable
    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {

                try {
                    Log.d(LOG_TAG, "Init background loader for " + StatusTimeTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                        //Skip run
                        Log.d(LOG_TAG, "Skip loader run");
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    @NotNull final NVRAMInfo nvramInfo = new NVRAMInfo();

                    NVRAMInfo nvramInfoTmp = null;
                    try {
                        nvramInfoTmp =
                                SSHUtils.getNVRamInfoFromRouter(mRouter,
                                        mGlobalPreferences, NVRAMInfo.NTP_ENABLE,
                                        NVRAMInfo.NTP_MODE,
                                        NVRAMInfo.NTP_SERVER,
                                        NVRAMInfo.TIME_ZONE,
                                        NVRAMInfo.DAYLIGHT_TIME);
                    } finally {
                        if (nvramInfoTmp != null) {
                            nvramInfo.putAll(nvramInfoTmp);
                        }

                        final String[] currentDate = SSHUtils.getManualProperty(mRouter, mGlobalPreferences, "date");
                        if (currentDate != null && currentDate.length > 0) {
                            nvramInfo.setProperty(NVRAMInfo.CURRENT_DATE, currentDate[0]);
                        }
                    }

                    if (nvramInfo.isEmpty()) {
                        throw new DDWRTNoDataException("No Data!");
                    }

                    return nvramInfo;
                } catch (@NotNull final Exception e) {
                    e.printStackTrace();
                    return new NVRAMInfo().setException(e);
                }

            }
        };
    }

    @Nullable
    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Nullable
    @Override
    protected OnClickIntent getOnclickIntent() {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<NVRAMInfo> loader, NVRAMInfo data) {
        //Set tiles
        Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

        layout.findViewById(R.id.tile_status_time_header_loading_view)
                .setVisibility(View.GONE);
        layout.findViewById(R.id.tile_status_time_loading_view)
                .setVisibility(View.GONE);
        layout.findViewById(R.id.tile_status_time_current_date)
                .setVisibility(View.VISIBLE);
        layout.findViewById(R.id.tile_status_time_grid_layout)
                .setVisibility(View.VISIBLE);

        if (data == null) {
            data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
        }

        @NotNull final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_time_error);

        @Nullable final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

            if (exception == null) {
                errorPlaceHolderView.setVisibility(View.GONE);
            }

            //Current Date
            ((TextView) this.layout.findViewById(R.id.tile_status_time_current_date))
                    .setText(data.getProperty(NVRAMInfo.CURRENT_DATE, "N/A"));

            //NTP Client
            final boolean ntpClientEnabled = "1".equals(data.getProperty(NVRAMInfo.NTP_ENABLE));
            ((TextView) this.layout.findViewById(R.id.tile_status_time_ntp_client)).setText((ntpClientEnabled ?
                "En": "Dis") + "abled");
            if (ntpClientEnabled) {
                //NTP Server
                ((TextView) this.layout.findViewById(R.id.tile_status_time_time_server)).setText(
                        data.getProperty(NVRAMInfo.NTP_SERVER, "N/A"));
            } else {
                //NTP Server
                ((TextView) this.layout.findViewById(R.id.tile_status_time_time_server)).setText("-");
            }

            //Time Zone
            final String timezone = data.getProperty(NVRAMInfo.TIME_ZONE);
            final String tzValue;
            if (timezone == null || timezone.isEmpty()) {
                tzValue = "N/A";
            } else {
                tzValue = "UTC"+(timezone.contains(".") ?
                        timezone
                            .replaceAll(".25", ":15")
                            .replaceAll(".5", ":30")
                            .replaceAll(".75", ":45") :
                        (timezone + ":00"));
            }
            ((TextView) this.layout.findViewById(R.id.tile_status_time_ntp_timezone)).setText(tzValue);

            //Daylight
            final String daylightPeriodString = daylightMap.get(data.getProperty(NVRAMInfo.DAYLIGHT_TIME));
            ((TextView) this.layout.findViewById(R.id.tile_status_time_summertime)).setText(
                    daylightPeriodString != null ? daylightPeriodString : "N/A");

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
                R.id.tile_status_time_togglebutton_title, R.id.tile_status_time_togglebutton_separator);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");
    }
}
