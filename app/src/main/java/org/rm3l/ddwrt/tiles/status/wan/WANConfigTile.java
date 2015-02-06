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

package org.rm3l.ddwrt.tiles.status.wan;

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

import com.google.common.base.Splitter;
import com.google.common.base.Throwables;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.utils.SSHUtils;

import java.util.List;

/**
 * TODO
 */
public class WANConfigTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = WANConfigTile.class.getSimpleName();

    public WANConfigTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_status_wan_config, R.id.tile_status_wan_config_togglebutton);
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_status_wan_config_title;
    }

    @Nullable
    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param id   The ID whose loader is to be created.
     * @param args Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Override
    protected Loader<NVRAMInfo> getLoader(final int id, final Bundle args) {
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
                        nvramInfoTmp = SSHUtils.getNVRamInfoFromRouter(mRouter,
                                mGlobalPreferences, NVRAMInfo.WAN_PROTO,
                                NVRAMInfo.WAN_3_G_SIGNAL,
                                NVRAMInfo.WAN_HWADDR,
                                NVRAMInfo.WAN_LEASE,
                                NVRAMInfo.WAN_IPADDR,
                                NVRAMInfo.WAN_NETMASK,
                                NVRAMInfo.WAN_GATEWAY,
                                NVRAMInfo.WAN_DNS);
                    } finally {
                        if (nvramInfoTmp != null) {
                            nvramInfo.putAll(nvramInfoTmp);
                        }

                        //Connection Uptime is stored in /tmp/.wanuptime and sys uptime from /proc/uptime
                        final String[] uptimes = SSHUtils.getManualProperty(mRouter, mGlobalPreferences, "cat /tmp/.wanuptime; echo; cat /proc/uptime");
                        if (uptimes != null && uptimes.length > 1) {
                            final String wanUptimeStr = uptimes[0];

                            /*
                             * /proc/uptime Shows how long the system has been on since it was last restarted.
                             * The first number is the total number of seconds the system has been up.
                             * The second number is how much of that time the machine has spent idle, in seconds.
                             * On multi core systems (and some linux versions), the second number is the sum of
                             * the idle time accumulated by each CPU.
                             */
                            final List<String> sysUptimeSplitList = Splitter.on(" ").
                                    trimResults().omitEmptyStrings().splitToList(uptimes[1]);
                            if (sysUptimeSplitList != null && !sysUptimeSplitList.isEmpty()) {
                                try {
                                    final float uptime = Float.valueOf(sysUptimeSplitList.get(0)) - Float.valueOf(wanUptimeStr);
                                    final int days = (int) uptime / (60 * 60 * 24);

                                    String wanConnectionUptimeStr = "";
                                    if (days > 0) {
                                        wanConnectionUptimeStr += String.format("%d day%s, ",
                                                days, (days == 1 ? "" : "s"));
                                    }
                                    final int minutes = (int) uptime / 60;
                                    wanConnectionUptimeStr += String.format("%d:%02d:%02d",
                                            (minutes / 60) % 24, minutes % 60, (int) uptime % 60);

                                    nvramInfo.setProperty(NVRAMInfo.WAN_CONNECTION_UPTIME, wanConnectionUptimeStr);

                                } catch (final NumberFormatException nfe) {
                                    nfe.printStackTrace();
                                    //No Worries - WAN Uptime will be marked as "-"
                                }
                            }
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

    /**
     * Called when a previously created loader has finished its load.  Note
     * that normally an application is <em>not</em> allowed to commit fragment
     * transactions while in this call, since it can happen after an
     * activity's state is saved.  See {@link android.support.v4.app.FragmentManager#beginTransaction()
     * FragmentManager.openTransaction()} for further discussion on this.
     * <p/>
     * <p>This function is guaranteed to be called prior to the release of
     * the last data that was supplied for this Loader.  At this point
     * you should remove all use of the old data (since it will be released
     * soon), but should not do your own release of the data since its Loader
     * owns it and will take care of that.  The Loader will take care of
     * management of its data so you don't have to.  In particular:
     * <p/>
     * <ul>
     * <li> <p>The Loader will monitor for changes to the data, and report
     * them to you through new calls here.  You should not monitor the
     * data yourself.  For example, if the data is a {@link android.database.Cursor}
     * and you place it in a {@link android.widget.CursorAdapter}, use
     * the {@link android.widget.CursorAdapter#CursorAdapter(android.content.Context,
     * android.database.Cursor, int)} constructor <em>without</em> passing
     * in either {@link android.widget.CursorAdapter#FLAG_AUTO_REQUERY}
     * or {@link android.widget.CursorAdapter#FLAG_REGISTER_CONTENT_OBSERVER}
     * (that is, use 0 for the flags argument).  This prevents the CursorAdapter
     * from doing its own observing of the Cursor, which is not needed since
     * when a change happens you will get a new Cursor throw another call
     * here.
     * <li> The Loader will release the data once it knows the application
     * is no longer using it.  For example, if the data is
     * a {@link android.database.Cursor} from a {@link android.content.CursorLoader},
     * you should not call close() on it yourself.  If the Cursor is being placed in a
     * {@link android.widget.CursorAdapter}, you should use the
     * {@link android.widget.CursorAdapter#swapCursor(android.database.Cursor)}
     * method so that the old Cursor is not closed.
     * </ul>
     *
     * @param loader The Loader that has finished.
     * @param data   The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(@NonNull Loader<NVRAMInfo> loader, @Nullable NVRAMInfo data) {

        //Set tiles
        Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

        layout.findViewById(R.id.tile_status_wan_config_loading_view)
                .setVisibility(View.GONE);
        layout.findViewById(R.id.tile_status_wan_config_gridLayout)
                .setVisibility(View.VISIBLE);

        if (data == null) {
            data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
        }

        final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_error);

        final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

            if (exception == null) {
                errorPlaceHolderView.setVisibility(View.GONE);
            }

            //Connection Type
            final TextView wanConnTypeView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_connection_type);
            final String wanProto = data.getProperty(NVRAMInfo.WAN_PROTO, "-");
            final String wanConnectionTypeManual;

            if ("ppoe".equalsIgnoreCase(wanProto)) {
                wanConnectionTypeManual = "PPoE";
            } else if ("3g".equalsIgnoreCase(wanProto)) {
                wanConnectionTypeManual = "3G/UMTS";

                //Unhide 3g signal text
                final TextView wan3gSignalFieldView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_3g_signal_field);
                final TextView wan3gSignalSeparatorView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_3g_signal_separator);
                final TextView wan3gSignalView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_3g_signal);
                wan3gSignalFieldView.setVisibility(View.VISIBLE);
                wan3gSignalSeparatorView.setVisibility(View.VISIBLE);
                wan3gSignalView.setVisibility(View.VISIBLE);
                wan3gSignalView.setText(data.getProperty(NVRAMInfo.WAN_3_G_SIGNAL, "-"));

            } else if ("heartbeat".equalsIgnoreCase(wanProto)) {
                wanConnectionTypeManual = "Heartbeat Signal";
            } else if ("disabled".equalsIgnoreCase(wanProto)) {
                wanConnectionTypeManual = "*Disabled*";
            } else {
                wanConnectionTypeManual = (wanProto != null ? wanProto.toUpperCase() : "-");
            }

            wanConnTypeView.setText(wanConnectionTypeManual);

            //Connection Uptime
            final TextView wanUptimeView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_connection_uptime);
            wanUptimeView.setText(data.getProperty(NVRAMInfo.WAN_CONNECTION_UPTIME, "-"));

            //MAC
            final TextView wanMacView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_wan_mac);
            wanMacView.setText(data.getProperty(NVRAMInfo.WAN_HWADDR, "-"));

            //IP
            final TextView wanIPView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_wan_ip);
            wanIPView.setText(data.getProperty(NVRAMInfo.WAN_IPADDR, "-"));

            //Subnet
            final TextView wanSubnetView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_subnet_mask);
            wanSubnetView.setText(data.getProperty(NVRAMInfo.WAN_NETMASK, "-"));

            //Gateway
            final TextView wanGatewayView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_subnet_gateway);
            wanGatewayView.setText(data.getProperty(NVRAMInfo.WAN_GATEWAY, "-"));

            //DNS
            final TextView wanDNSView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_dns);
            final String property = data.getProperty(NVRAMInfo.WAN_DNS, "-");
            wanDNSView.setText(property != null ? property.replaceAll(" ", ", ") : "-");
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
                R.id.tile_status_wan_config_togglebutton_title, R.id.tile_status_wan_config_togglebutton_separator);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");
    }

    @Nullable
    @Override
    protected OnClickIntent getOnclickIntent() {
        //TODO
        return null;
    }
}
