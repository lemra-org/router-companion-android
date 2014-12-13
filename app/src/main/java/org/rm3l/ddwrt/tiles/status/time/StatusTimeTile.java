/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Armel S.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.rm3l.ddwrt.tiles.status.time;

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

                    @Nullable final NVRAMInfo nvramInfo = new NVRAMInfo();

                    NVRAMInfo nvramInfoTmp = null;
                    try {
                        nvramInfoTmp =
                                SSHUtils.getNVRamInfoFromRouter(mRouter,
                                        NVRAMInfo.NTP_ENABLE,
                                        NVRAMInfo.NTP_MODE,
                                        NVRAMInfo.NTP_SERVER,
                                        NVRAMInfo.TIME_ZONE,
                                        NVRAMInfo.DAYLIGHT_TIME);
                    } finally {
                        if (nvramInfoTmp != null) {
                            nvramInfo.putAll(nvramInfoTmp);
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
    protected Intent getOnclickIntent() {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<NVRAMInfo> loader, NVRAMInfo data) {
//Set tiles
        Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

        if (data == null) {
            data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
        }

        @NotNull final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_time_error);

        @Nullable final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

            if (exception == null) {
                errorPlaceHolderView.setVisibility(View.GONE);
            }

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
                tzValue = "UTC"+(timezone
                        .replaceAll(".25", ":15")
                        .replaceAll(".5", ":30")
                        .replaceAll(".75", ":45"));
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
