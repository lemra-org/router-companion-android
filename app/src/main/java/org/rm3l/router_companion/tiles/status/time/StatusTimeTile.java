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

package org.rm3l.router_companion.tiles.status.time;

import static org.rm3l.router_companion.utils.Utils.isDemoRouter;

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
import com.crashlytics.android.Crashlytics;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.utils.SSHUtils;

public class StatusTimeTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = StatusTimeTile.class.getSimpleName();

    private final Map<String, String> daylightMap = Maps.newHashMapWithExpectedSize(10);

    private long mLastSync;

    public StatusTimeTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments,
            @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_status_time, null);
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
    public int getTileHeaderViewId() {
        return R.id.tile_status_time_hdr;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_status_time_title;
    }

    @Override
    public void onLoadFinished(Loader<NVRAMInfo> loader, NVRAMInfo data) {
        try {
            //Set tiles
            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

            layout.findViewById(R.id.tile_status_time_loading_view).setVisibility(View.GONE);
            layout.findViewById(R.id.tile_status_time_grid_layout).setVisibility(View.VISIBLE);

            if (data == null) {
                data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
            }

            final TextView errorPlaceHolderView =
                    (TextView) this.layout.findViewById(R.id.tile_status_time_error);

            final Exception exception = data.getException();

            if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

                if (exception == null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }

                //Current Date
                final String currentDate = data.getProperty(NVRAMInfo.Companion.getCURRENT_DATE(), "-");
                ((TextView) layout.findViewById(R.id.tile_status_time_current_time)).setText(currentDate);

                //NTP Client
                final boolean ntpClientEnabled = "1".equals(data.getProperty(
                        NVRAMInfo.Companion.getNTP_ENABLE()));
                ((TextView) this.layout.findViewById(R.id.tile_status_time_ntp_client)).setText(
                        (ntpClientEnabled ? "En" : "Dis") + "abled");
                if (ntpClientEnabled) {
                    //NTP Server
                    ((TextView) this.layout.findViewById(R.id.tile_status_time_time_server)).setText(
                            data.getProperty(NVRAMInfo.Companion.getNTP_SERVER(), "-"));
                } else {
                    //NTP Server
                    ((TextView) this.layout.findViewById(R.id.tile_status_time_time_server)).setText("-");
                }

                //Time Zone
                final String timezone = data.getProperty(NVRAMInfo.Companion.getTIME_ZONE());
                final String tzValue;
                if (timezone == null || timezone.isEmpty()) {
                    tzValue = "-";
                } else {
                    tzValue = "UTC" + (timezone.contains(".") ? timezone.replaceAll(".25", ":15")
                            .replaceAll(".5", ":30")
                            .replaceAll(".75", ":45") : (timezone + ":00"));
                }
                ((TextView) this.layout.findViewById(R.id.tile_status_time_ntp_timezone)).setText(tzValue);

                //Daylight
                final String daylightPeriodString =
                        daylightMap.get(data.getProperty(NVRAMInfo.Companion.getDAYLIGHT_TIME()));
                ((TextView) this.layout.findViewById(R.id.tile_status_time_summertime)).setText(
                        daylightPeriodString != null ? daylightPeriodString : "-");

                //Update last sync
                final RelativeTimeTextView lastSyncView =
                        (RelativeTimeTextView) layout.findViewById(R.id.tile_last_sync);
                lastSyncView.setReferenceTime(mLastSync);
                lastSyncView.setPrefix("Last sync: ");
            }

            if (exception != null && !(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {
                //noinspection ThrowableResultOfMethodCallIgnored
                final Throwable rootCause = Throwables.getRootCause(exception);
                errorPlaceHolderView.setText(
                        "Error: " + (rootCause != null ? rootCause.getMessage() : "null"));
                final Context parentContext = this.mParentFragmentActivity;
                errorPlaceHolderView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        //noinspection ThrowableResultOfMethodCallIgnored
                        if (rootCause != null) {
                            Toast.makeText(parentContext, rootCause.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
                errorPlaceHolderView.setVisibility(View.VISIBLE);
                updateProgressBarWithError();
            } else if (exception == null) {
                updateProgressBarWithSuccess();
            }

            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished(): done loading!");
        } finally {
            mRefreshing.set(false);
            doneWithLoaderInstance(this, loader);
        }
    }

    @Nullable
    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {

                try {
                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Init background loader for "
                            + StatusTimeTile.class
                            + ": routerInfo="
                            + mRouter
                            + " / nbRunsLoader="
                            + nbRunsLoader);

                    if (mRefreshing.getAndSet(true)) {
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    updateProgressBarViewSeparator(0);

                    mLastSync = System.currentTimeMillis();

                    final NVRAMInfo nvramInfo = new NVRAMInfo();

                    NVRAMInfo nvramInfoTmp = null;
                    try {
                        updateProgressBarViewSeparator(10);
                        if (isDemoRouter(mRouter)) {
                            nvramInfoTmp = new NVRAMInfo().setProperty(NVRAMInfo.Companion.getNTP_ENABLE(), "1")
                                    .setProperty(NVRAMInfo.Companion.getNTP_MODE(), "")
                                    .setProperty(NVRAMInfo.Companion.getNTP_SERVER(), "time.serv.er")
                                    .setProperty(NVRAMInfo.Companion.getTIME_ZONE(), "+02:00")
                                    .setProperty(NVRAMInfo.Companion.getDAYLIGHT_TIME(), "3");
                        } else {
                            nvramInfoTmp = SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity, mRouter,
                                    mGlobalPreferences, NVRAMInfo.Companion.getNTP_ENABLE(),
                                    NVRAMInfo.Companion.getNTP_MODE(), NVRAMInfo.Companion.getNTP_SERVER(),
                                    NVRAMInfo.Companion.getTIME_ZONE(), NVRAMInfo.Companion.getDAYLIGHT_TIME());
                        }
                        updateProgressBarViewSeparator(55);
                    } finally {
                        if (nvramInfoTmp != null) {
                            nvramInfo.putAll(nvramInfoTmp);
                        }

                        final String[] currentDate;
                        if (isDemoRouter(mRouter)) {
                            currentDate = new String[1];
                            currentDate[0] =
                                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
                        } else {
                            currentDate =
                                    SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                            "date");
                        }
                        updateProgressBarViewSeparator(90);
                        if (currentDate != null && currentDate.length > 0) {
                            nvramInfo.setProperty(NVRAMInfo.Companion.getCURRENT_DATE(), currentDate[0]);
                        }
                    }

                    if (nvramInfo.isEmpty()) {
                        throw new DDWRTNoDataException("No Data!");
                    }

                    return nvramInfo;
                } catch (@NonNull final Exception e) {
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
}
