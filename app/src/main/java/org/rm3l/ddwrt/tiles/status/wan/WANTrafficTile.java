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
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * TODO
 */
public class WANTrafficTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = WANTrafficTile.class.getSimpleName();

    public WANTrafficTile(@NotNull Fragment parentFragment, @NotNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_status_wan_traffic, R.id.tile_status_wan_traffic_togglebutton);
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_status_wan_traffic_title;
    }

    @Nullable
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

                    //TODO
                    //Start by getting information about the WAN iface name
                    @Nullable final NVRAMInfo nvRamInfoFromRouter = SSHUtils.getNVRamInfoFromRouter(mRouter, mGlobalPreferences, NVRAMInfo.WAN_IFACE);
                    if (nvRamInfoFromRouter == null) {
                        throw new IllegalStateException("Whoops. WAN Iface could not be determined.");
                    }

                    final String wanIface = nvRamInfoFromRouter
                            .getProperty(NVRAMInfo.WAN_IFACE);

                    if (Strings.isNullOrEmpty(wanIface)) {
                        throw new IllegalStateException("Whoops. WAN Iface could not be determined.");
                    }

                    @Nullable final String[] netDevWanIfaces = SSHUtils.getManualProperty(mRouter, mGlobalPreferences, "cat /proc/net/dev | grep \"" + wanIface + "\"");
                    if (netDevWanIfaces == null || netDevWanIfaces.length == 0) {
                        return null;
                    }

                    String netDevWanIface = netDevWanIfaces[0];
                    if (netDevWanIface == null) {
                        return null;
                    }

                    netDevWanIface = netDevWanIface.replaceAll(wanIface + ":", "");

                    final List<String> netDevWanIfaceList = Splitter.on(" ").omitEmptyStrings().trimResults().splitToList(netDevWanIface);
                    if (netDevWanIfaceList == null || netDevWanIfaceList.size() <= 15) {
                        return null;
                    }

                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_bytes", netDevWanIfaceList.get(0));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_packets", netDevWanIfaceList.get(1));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_errs", netDevWanIfaceList.get(2));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_drop", netDevWanIfaceList.get(3));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_fifo", netDevWanIfaceList.get(4));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_frame", netDevWanIfaceList.get(5));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_compressed", netDevWanIfaceList.get(6));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_multicast", netDevWanIfaceList.get(7));

                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_bytes", netDevWanIfaceList.get(8));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_packets", netDevWanIfaceList.get(9));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_errs", netDevWanIfaceList.get(10));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_drop", netDevWanIfaceList.get(11));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_fifo", netDevWanIfaceList.get(12));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_colls", netDevWanIfaceList.get(13));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_carrier", netDevWanIfaceList.get(14));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_compressed", netDevWanIfaceList.get(15));

                    return nvRamInfoFromRouter;

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

        layout.findViewById(R.id.tile_status_wan_traffic_loading_view)
                .setVisibility(View.GONE);
        layout.findViewById(R.id.tile_status_wan_traffic_gridLayout)
                .setVisibility(View.VISIBLE);

        if (data == null) {
            data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
        }

        @NotNull final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_wan_traffic_error);

        @Nullable final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

            if (exception == null) {
                errorPlaceHolderView.setVisibility(View.GONE);
            }

            final String wanIface = data
                    .getProperty(NVRAMInfo.WAN_IFACE);

            //Iface Name
            @NotNull final TextView wanIfaceView = (TextView) this.layout.findViewById(R.id.tile_status_wan_traffic_iface);
            wanIfaceView.setText(Strings.isNullOrEmpty(wanIface) ? "-" : wanIface);

            @NotNull final TextView wanIngressView = (TextView) this.layout.findViewById(R.id.tile_status_wan_traffic_ingress);
            String text;
            final String wanRcvBytes = data.getProperty(wanIface + "_rcv_bytes", "-1");
            try {
                final double wanRcvMBytes = Double.parseDouble(wanRcvBytes) / (1024 * 1024);
                if (wanRcvMBytes < 0.) {
                    text = "-";
                } else {
                    text = Double.toString(new BigDecimal(wanRcvMBytes).setScale(2, RoundingMode.HALF_UP).doubleValue());
                }

            } catch (@NotNull final NumberFormatException nfe) {
                text = "-";
            }
            wanIngressView.setText(text);

            @NotNull final TextView wanEgressView = (TextView) this.layout.findViewById(R.id.tile_status_wan_traffic_egress);
            final String wanXmitBytes = data.getProperty(wanIface + "_xmit_bytes", "-1");
            try {
                final double wanXmitMBytes = Double.parseDouble(wanXmitBytes) / (1024 * 1024);
                if (wanXmitMBytes < 0.) {
                    text = "-";
                } else {
                    text = Double.toString(new BigDecimal(wanXmitMBytes).setScale(2, RoundingMode.HALF_UP).doubleValue());
                }

            } catch (@NotNull final NumberFormatException nfe) {
                text = "-";
            }
            wanEgressView.setText(text);

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
                R.id.tile_status_wan_traffic_togglebutton_title, R.id.tile_status_wan_traffic_togglebutton_separator);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");

    }

    @Nullable
    @Override
    protected OnClickIntent getOnclickIntent() {
        //TODO
        return null;
    }
}
