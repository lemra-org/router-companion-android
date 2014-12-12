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

package org.rm3l.ddwrt.tiles.syslog;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.SSHUtils;

import java.util.Random;

import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.SYSLOG;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.SYSLOGD_ENABLE;

/**
 *
 */
public class StatusSyslogTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = StatusSyslogTile.class.getSimpleName();
    public static final Joiner LOGS_JOINER = Joiner.on("\n").useForNull("");

    public StatusSyslogTile(@NotNull SherlockFragment parentFragment, @NotNull Bundle arguments, Router router) {
        super(parentFragment, arguments, router, R.layout.tile_status_router_syslog, R.id.tile_status_router_syslog_togglebutton);
    }

    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {
                try {
                    Log.d(LOG_TAG, "Init background loader for " + StatusSyslogTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                        //Skip run
                        Log.d(LOG_TAG, "Skip loader run");
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    @NotNull final NVRAMInfo nvramInfo = new NVRAMInfo();
                    if (DDWRTCompanionConstants.TEST_MODE) {
                        @NotNull final String syslogData = "Space suits go with future at the carnivorous alpha quadrant!\n" +
                                "Cur guttus mori? Ferox, clemens hippotoxotas acceleratrix " +
                                "anhelare de germanus, camerarius bubo. Always purely feel the magical lord.\n" +
                                "Refrigerate roasted lobsters in a cooker with hollandaise sauce for about an hour to enhance their thickness." +
                                "With escargots drink BBQ sauce.Yarr there's nothing like the misty amnesty screaming on the sea.\n" +
                                "Death is a stormy whale.The undead parrot smartly leads the anchor.\n\n\n";
                        nvramInfo.setProperty(SYSLOG, syslogData);
                        nvramInfo.setProperty(SYSLOGD_ENABLE,String.valueOf(new Random().nextInt()));
                    } else {
                        NVRAMInfo nvramInfoTmp = null;
                        try {
                            nvramInfoTmp = SSHUtils.getNVRamInfoFromRouter(mRouter, SYSLOGD_ENABLE);
                        } finally {
                            if (nvramInfoTmp != null) {
                                nvramInfo.putAll(nvramInfoTmp);
                            }

                            String[] logs = null;
                            try {
                                //Get last 10 lines
                                logs = SSHUtils.getManualProperty(mRouter, "tail -n 10 /tmp/var/log/messages");
                            } finally {
                                if (logs != null) {
                                    nvramInfo.setProperty(SYSLOG, LOGS_JOINER.join(logs));
                                }
                            }
                        }
                    }

                    return nvramInfo;

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

        if (data == null) {
            data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
        }

        @NotNull final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_error);

        @Nullable final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

            if (exception == null) {
                errorPlaceHolderView.setVisibility(View.GONE);
            }

            final String syslogdEnabledPropertyValue = data.getProperty(SYSLOGD_ENABLE);
            final boolean isSyslogEnabled = "1".equals(syslogdEnabledPropertyValue);

            final TextView syslogState = (TextView) this.layout.findViewById(R.id.tile_status_router_syslog_state);

            final View syslogContentView = this.layout.findViewById(R.id.tile_status_router_syslog_content);
            final View filterEditText = this.layout.findViewById(R.id.tile_status_router_syslog_filter);
            final View filterButton = this.layout.findViewById(R.id.tile_status_router_syslog_send_filter_cmd_button);
            syslogState.setText(syslogdEnabledPropertyValue == null ? "N/A" : (isSyslogEnabled ? "Enabled" : "Disabled"));

            if (isSyslogEnabled) {
                final TextView logTextView = (TextView) syslogContentView;
                logTextView
                        .setText(logTextView.getText() + "\n" + data.getProperty(SYSLOG, ""));
                syslogContentView.setVisibility(View.VISIBLE);
                filterEditText.setVisibility(View.VISIBLE);
                filterButton.setVisibility(View.VISIBLE);
            } else {
                syslogContentView.setVisibility(View.GONE);
                filterEditText.setVisibility(View.GONE);
                filterButton.setVisibility(View.GONE);
            }

        }

        if (exception != null && !(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {
            //noinspection ThrowableResultOfMethodCallIgnored
            errorPlaceHolderView.setText("Error: " + Throwables.getRootCause(exception).getMessage());
            errorPlaceHolderView.setVisibility(View.VISIBLE);
        }

        doneWithLoaderInstance(this, loader,
                R.id.tile_status_router_syslog_togglebutton_title, R.id.tile_status_router_syslog_togglebutton);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");
    }

    @Nullable
    @Override
    protected Intent getOnclickIntent() {
        //TODO
        return null;
    }
}
