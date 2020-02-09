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

package org.rm3l.router_companion.tiles.status.wan;

import static org.rm3l.router_companion.utils.Utils.isDemoRouter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.utils.SSHUtils;

/**
 * TODO
 */
public class WANTrafficTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = WANTrafficTile.class.getSimpleName();

    private long mLastSync;

    public WANTrafficTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments,
            @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_status_wan_traffic, null);
    }

    @Override
    public Integer getTileHeaderViewId() {
        return R.id.tile_status_wan_traffic_hdr;
    }

    @Override
    public Integer getTileTitleViewId() {
        return R.id.tile_status_wan_traffic_title;
    }

    public NVRAMInfo getWANTotalTrafficNvramInfo(FragmentActivity mParentFragmentActivity,
            Router mRouter, SharedPreferences mGlobalPreferences) throws Exception {

        //Start by getting information about the WAN iface name
        final NVRAMInfo nvRamInfoFromRouter;
        if (isDemoRouter(mRouter)) {
            nvRamInfoFromRouter = new NVRAMInfo().setProperty(NVRAMInfo.Companion.getWAN_IFACE(), "wan0");
        } else {
            nvRamInfoFromRouter =
                    SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity, mRouter, mGlobalPreferences,
                            NVRAMInfo.Companion.getWAN_IFACE());
        }

        updateProgressBarViewSeparator(45);

        if (nvRamInfoFromRouter == null) {
            throw new IllegalStateException("Whoops. WAN Iface could not be determined.");
        }

        final String wanIface = nvRamInfoFromRouter.getProperty(NVRAMInfo.Companion.getWAN_IFACE());

        if (Strings.isNullOrEmpty(wanIface)) {
            throw new IllegalStateException("Whoops. WAN Iface could not be determined.");
        }

        final String[] netDevWanIfaces;
        if (isDemoRouter(mRouter)) {
            netDevWanIfaces = new String[1];
            netDevWanIfaces[0] =
                    "  eth0: 3403368500 3103262    0    1    0     0          0         0 652048226 2056456    0    0    0     0       0          0";
        } else {
            netDevWanIfaces =
                    SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                            "cat /proc/net/dev | grep \"" + wanIface + "\"");
        }
        updateProgressBarViewSeparator(75);
        if (netDevWanIfaces == null || netDevWanIfaces.length == 0) {
            return null;
        }

        String netDevWanIface = netDevWanIfaces[0];
        if (netDevWanIface == null) {
            return null;
        }

        netDevWanIface = netDevWanIface.replaceAll(wanIface + ":", "");

        final List<String> netDevWanIfaceList =
                Splitter.on(" ").omitEmptyStrings().trimResults().splitToList(netDevWanIface);
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

        updateProgressBarViewSeparator(90);

        return nvRamInfoFromRouter;
    }

    /**
     * Called when a previously created loader has finished its load.  Note
     * that normally an application is <em>not</em> allowed to commit fragment
     * transactions while in this call, since it can happen after an
     * activity's state is saved.  See {@link androidx.fragment.app.FragmentManager#beginTransaction()
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
     * the {@link android.widget.CursorAdapter#CursorAdapter(android.content.Context, * android.database.Cursor, int)}
     * constructor <em>without</em> passing
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
    public void onLoadFinished(@NonNull Loader<NVRAMInfo> loader,
            @Nullable NVRAMInfo data) {
        try {
            //Set tiles
            FirebaseCrashlytics.getInstance().log( "onLoadFinished: loader=" + loader + " / data=" + data);

            layout.findViewById(R.id.tile_status_wan_traffic_loading_view).setVisibility(View.GONE);
            layout.findViewById(R.id.tile_status_wan_traffic_gridLayout).setVisibility(View.VISIBLE);

            if (data == null) {
                data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
            }

            final TextView errorPlaceHolderView =
                    (TextView) this.layout.findViewById(R.id.tile_status_wan_traffic_error);

            final Exception exception = data.getException();

            if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

                if (exception == null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }

                final String wanIface = data.getProperty(NVRAMInfo.Companion.getWAN_IFACE());

                //Iface Name
                final TextView wanIfaceView =
                        (TextView) this.layout.findViewById(R.id.tile_status_wan_traffic_iface);
                wanIfaceView.setText(Strings.isNullOrEmpty(wanIface) ? "-" : wanIface);

                final TextView wanIngressView =
                        (TextView) this.layout.findViewById(R.id.tile_status_wan_traffic_ingress);
                String text;
                final String wanRcvBytes = data.getProperty(wanIface + "_rcv_bytes", "-1");
                try {
                    final double wanRcvMBytes = Double.parseDouble(wanRcvBytes) / (1024 * 1024);
                    if (wanRcvMBytes < 0.) {
                        text = "-";
                    } else {
                        text = Double.toString(
                                new BigDecimal(wanRcvMBytes).setScale(2, RoundingMode.HALF_UP).doubleValue());
                    }
                } catch (@NonNull final NumberFormatException nfe) {
                    text = "-";
                }
                wanIngressView.setText(text);

                final TextView wanEgressView =
                        (TextView) this.layout.findViewById(R.id.tile_status_wan_traffic_egress);
                final String wanXmitBytes = data.getProperty(wanIface + "_xmit_bytes", "-1");
                try {
                    final double wanXmitMBytes = Double.parseDouble(wanXmitBytes) / (1024 * 1024);
                    if (wanXmitMBytes < 0.) {
                        text = "-";
                    } else {
                        text = Double.toString(
                                new BigDecimal(wanXmitMBytes).setScale(2, RoundingMode.HALF_UP).doubleValue());
                    }
                } catch (@NonNull final NumberFormatException nfe) {
                    text = "-";
                }
                wanEgressView.setText(text);

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

            FirebaseCrashlytics.getInstance().log( "onLoadFinished(): done loading!");
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
                    FirebaseCrashlytics.getInstance().log( "Init background loader for "
                            + WANConfigTile.class
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

                    updateProgressBarViewSeparator(10);
                    return getWANTotalTrafficNvramInfo(mParentFragmentActivity, mRouter, mGlobalPreferences);
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
        //TODO
        return null;
    }
}
