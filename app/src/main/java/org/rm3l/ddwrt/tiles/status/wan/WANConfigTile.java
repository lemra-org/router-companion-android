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

package org.rm3l.ddwrt.tiles.status.wan;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

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
import org.rm3l.ddwrt.utils.SSHUtils;

/**
 * TODO
 */
public class WANConfigTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = WANConfigTile.class.getSimpleName();

    public WANConfigTile(@NotNull SherlockFragmentActivity parentFragmentActivity, @NotNull Bundle arguments, @Nullable Router router) {
        super(parentFragmentActivity, arguments, router, R.layout.tile_status_wan_config, R.id.tile_status_wan_config_togglebutton);
    }
//
//    @Nullable
//    @Override
//    public ViewGroup getViewGroupLayout() {
//        final LinearLayout layout = (LinearLayout) this.mParentFragmentActivity.getLayoutInflater().inflate(R.layout.tile_status_wan_config, null);
//        mToggleAutoRefreshButton = (ToggleButton) layout.findViewById(R.id.tile_status_wan_config_togglebutton);
//        mToggleAutoRefreshButton.setOnCheckedChangeListener(this);
//
//        return layout;
//    }

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

                    return SSHUtils.getNVRamInfoFromRouter(mRouter,
                            NVRAMInfo.WAN_PROTO,
                            NVRAMInfo.WAN_3_G_SIGNAL,
                            NVRAMInfo.WAN_HWADDR,
                            NVRAMInfo.WAN_LEASE,
                            NVRAMInfo.WAN_IPADDR,
                            NVRAMInfo.WAN_NETMASK,
                            NVRAMInfo.WAN_GATEWAY,
                            NVRAMInfo.WAN_DNS);

                } catch (@NotNull final Exception e) {
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
    public void onLoadFinished(@NotNull Loader<NVRAMInfo> loader, @Nullable NVRAMInfo data) {

        //Set tiles
        Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

        if (data == null) {
            data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
        }

        @NotNull final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_error);

        @Nullable final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

            if (exception == null) {
                if (errorPlaceHolderView != null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }
            }

            //Connection Type
            @NotNull final TextView wanConnTypeView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_connection_type);
            if (wanConnTypeView != null) {
                final String wanProto = data.getProperty(NVRAMInfo.WAN_PROTO, "N/A");
                @NotNull final String wanConnectionTypeManual;

                if ("ppoe".equalsIgnoreCase(wanProto)) {
                    wanConnectionTypeManual = "PPoE";
                } else if ("3g".equalsIgnoreCase(wanProto)) {
                    wanConnectionTypeManual = "3G/UMTS";

                    //Unhide 3g signal text
                    @NotNull final TextView wan3gSignalFieldView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_3g_signal_field);
                    @NotNull final TextView wan3gSignalSeparatorView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_3g_signal_separator);
                    @NotNull final TextView wan3gSignalView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_3g_signal);
                    if (wan3gSignalFieldView != null && wan3gSignalSeparatorView != null && wan3gSignalView != null) {
                        wan3gSignalFieldView.setVisibility(View.VISIBLE);
                        wan3gSignalSeparatorView.setVisibility(View.VISIBLE);
                        wan3gSignalView.setVisibility(View.VISIBLE);
                        wan3gSignalView.setText(data.getProperty(NVRAMInfo.WAN_3_G_SIGNAL, "N/A"));
                    }

                } else if ("heartbeat".equalsIgnoreCase(wanProto)) {
                    wanConnectionTypeManual = "Heartbeat Signal";
                } else if ("disabled".equalsIgnoreCase(wanProto)) {
                    wanConnectionTypeManual = "*Disabled*";
                } else {
                    wanConnectionTypeManual = wanProto.toUpperCase();
                }

                wanConnTypeView.setText(wanConnectionTypeManual);
            }

            //Connection Uptime
            @NotNull final TextView wanUptimeView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_connection_uptime);
            if (wanUptimeView != null) {
                wanUptimeView.setText(data.getProperty("wan_connection_uptime", "N/A"));
            }

            //MAC
            @NotNull final TextView wanMacView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_wan_mac);
            if (wanMacView != null) {
                wanMacView.setText(data.getProperty(NVRAMInfo.WAN_HWADDR, "N/A"));
            }

            //IP
            @NotNull final TextView wanIPView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_wan_ip);
            if (wanIPView != null) {
                wanIPView.setText(data.getProperty(NVRAMInfo.WAN_IPADDR, "N/A"));
            }

            //Subnet
            @NotNull final TextView wanSubnetView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_subnet_mask);
            if (wanSubnetView != null) {
                wanSubnetView.setText(data.getProperty(NVRAMInfo.WAN_NETMASK, "N/A"));
            }

            //Gateway
            @NotNull final TextView wanGatewayView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_subnet_gateway);
            if (wanGatewayView != null) {
                wanGatewayView.setText(data.getProperty(NVRAMInfo.WAN_GATEWAY, "N/A"));
            }

            //DNS
            @NotNull final TextView wanDNSView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_dns);
            if (wanDNSView != null) {
                wanDNSView.setText(data.getProperty(NVRAMInfo.WAN_DNS, "N/A").replaceAll(" ", ", "));
            }

        }

        if (exception != null) {
            errorPlaceHolderView.setText("Error: " + Throwables.getRootCause(exception).getMessage());
            errorPlaceHolderView.setVisibility(View.VISIBLE);
        }

        doneWithLoaderInstance(this, loader,
                R.id.tile_status_wan_config_togglebutton_title, R.id.tile_status_wan_config_togglebutton_separator);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");
    }

    @Nullable
    @Override
    protected Intent getOnclickIntent() {
        //TODO
        return null;
    }
}
