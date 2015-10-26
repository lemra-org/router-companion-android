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

package org.rm3l.ddwrt.tiles.status.router;

import android.content.Context;
import android.content.Intent;
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

import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.util.List;

import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.ddwrt.utils.Utils.isDemoRouter;

/**
 *
 */
public class StatusRouterMemoryTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = StatusRouterMemoryTile.class.getSimpleName();

    private String[] memInfoContents;
    private long mLastSync;

    public StatusRouterMemoryTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_status_router_router_mem, R.id.tile_status_router_router_mem_togglebutton);
    }

    @NonNull
    private static String getGrepProcMemInfo(@NonNull final String item) {
        return "grep \"" + item + "\" /proc/meminfo ";
    }

    @Override
    public int getTileHeaderViewId() {
        return R.id.tile_status_router_router_mem_hdr;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_status_router_router_mem_title;
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

            @NonNull
            @Override
            public NVRAMInfo loadInBackground() {

                try {
                    Log.d(LOG_TAG, "Init background loader for " + StatusRouterMemoryTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (mRefreshing.getAndSet(true)) {
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    if (!isForceRefresh()) {
                        //Force Manual Refresh
                        if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                            //Skip run
                            Log.d(LOG_TAG, "Skip loader run");
                            return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                        }
                    }
                    nbRunsLoader++;

                    mLastSync = System.currentTimeMillis();

                    memInfoContents = null;

                    final NVRAMInfo nvramInfo = new NVRAMInfo();

                    final String[] otherCmds;
                    if (isDemoRouter(mRouter)) {
                        otherCmds = new String[2];
                        otherCmds[0] = "13004 kB";
                        otherCmds[1] = "844 kB";
                    } else {
                        otherCmds = SSHUtils.getManualProperty(mParentFragmentActivity, mRouter,
                                mGlobalPreferences, getGrepProcMemInfo("MemTotal"), getGrepProcMemInfo("MemFree"));
                    }
                    if (otherCmds != null && otherCmds.length >= 2) {
                        //Total
                        String memTotal = null;
                        List<String> strings = Splitter.on("MemTotal:").omitEmptyStrings()
                                .trimResults().splitToList(otherCmds[0].trim());
                        if (strings != null && strings.size() >= 1) {
                            memTotal = strings.get(0);
                            nvramInfo.setProperty(NVRAMInfo.MEMORY_TOTAL, memTotal);

                        }

                        //Free
                        String memFree = null;
                        strings = Splitter.on("MemFree:").omitEmptyStrings().trimResults()
                                .splitToList(otherCmds[1].trim());
                        if (strings != null && strings.size() >= 1) {
                            memFree = strings.get(0);
                            nvramInfo.setProperty(NVRAMInfo.MEMORY_FREE, strings.get(0));

                        }

                        //Mem used
                        String memUsed = null;
                        if (!(isNullOrEmpty(memTotal) || isNullOrEmpty(memFree))) {
                            //noinspection ConstantConditions
                            memUsed = Long.toString(
                                    Long.parseLong(memTotal.replaceAll(" kB", "")) - Long.parseLong(memFree.replaceAll(" kB", "")))
                                    + " kB";

                            nvramInfo.setProperty(NVRAMInfo.MEMORY_USED, memUsed);

                        }

                        //Now cache whole /proc/cpuinfo, for detailed activity
                        if (isDemoRouter(mRouter)) {
                            memInfoContents = new String[2];
                            memInfoContents[0] = "Lorem Ipsum\n";
                            memInfoContents[1] = "Dolor sit amet...\n";
                        } else {
                            memInfoContents = SSHUtils.getManualProperty(mParentFragmentActivity, mRouter,
                                    mGlobalPreferences, "cat /proc/meminfo");
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
    public void onLoadFinished(@NonNull final Loader<NVRAMInfo> loader, @Nullable NVRAMInfo data) {
        try {
            //Set tiles
            Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

            layout.findViewById(R.id.tile_status_router_router_mem_header_loading_view)
                    .setVisibility(View.GONE);
            layout.findViewById(R.id.tile_status_router_router_mem_loading_view)
                    .setVisibility(View.GONE);
            layout.findViewById(R.id.tile_status_router_router_mem_total)
                    .setVisibility(View.VISIBLE);
            layout.findViewById(R.id.tile_status_router_router_mem_grid_layout)
                    .setVisibility(View.VISIBLE);

            if (data == null) {
                data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
            }

            final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_mem_error);

            final Exception exception = data.getException();

            if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

                if (exception == null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }

                //Total
                final TextView memTotalView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_mem_total);
                memTotalView.setText(data.getProperty(NVRAMInfo.MEMORY_TOTAL));

                //Model
                final TextView memFreeView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_mem_free);
                memFreeView.setText(data.getProperty(NVRAMInfo.MEMORY_FREE, "-"));

                //Cores Count
                final TextView memUsedView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_mem_used);
                memUsedView.setText(data.getProperty(NVRAMInfo.MEMORY_USED, "-"));

                //Update last sync
                final RelativeTimeTextView lastSyncView = (RelativeTimeTextView) layout.findViewById(R.id.tile_last_sync);
                lastSyncView.setReferenceTime(mLastSync);
                lastSyncView.setPrefix("Last sync: ");

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
                    R.id.tile_status_router_router_mem_togglebutton_title, R.id.tile_status_router_router_mem_togglebutton_separator);

            Log.d(LOG_TAG, "onLoadFinished(): done loading!");
        } finally {
            mRefreshing.set(false);
        }
    }

    @Nullable
    @Override
    protected OnClickIntent getOnclickIntent() {
        if (memInfoContents == null) {
            //Loading
            Utils.displayMessage(mParentFragmentActivity, "Loading data from router - please wait a few seconds.", Style.ALERT);
            return null;
        }

        if (memInfoContents.length == 0) {
            //No data!
            Utils.displayMessage(mParentFragmentActivity, "No data available - please retry later.", Style.ALERT);
            return null;
        }

        final String mRouterUuid = mRouter.getUuid();
        final Intent memInfoIntent =
                new Intent(mParentFragment.getActivity(), RouterMemInfoActivity.class);
        memInfoIntent.putExtra(RouterMemInfoActivity.MEM_INFO_OUTPUT, memInfoContents);
        memInfoIntent.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouterUuid);

        return new OnClickIntent("Loading Memory Info...", memInfoIntent, null);
    }
}
