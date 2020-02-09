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

package org.rm3l.router_companion.tiles.status.router;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.router_companion.utils.Utils.isDemoRouter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import java.util.List;
import java.util.Random;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.firmwares.impl.ddwrt.DDWRTFirmwareConnector;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.ReportingUtils;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style;

/**
 *
 */
public class StatusRouterMemoryTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = StatusRouterMemoryTile.class.getSimpleName();

    private final boolean isThemeLight;

    private long mLastSync;

    private String[] memInfoContents;

    public StatusRouterMemoryTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments,
            @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_status_router_router_mem, null);
        isThemeLight = ColorUtils.Companion.isThemeLight(mParentFragmentActivity);
    }

    @Override
    public Integer getTileHeaderViewId() {
        return R.id.tile_status_router_router_mem_hdr;
    }

    @Override
    public Integer getTileTitleViewId() {
        return R.id.tile_status_router_router_mem_title;
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
    public void onLoadFinished(@NonNull final Loader<NVRAMInfo> loader,
            @Nullable NVRAMInfo data) {
        try {
            //Set tiles
            FirebaseCrashlytics.getInstance().log( "onLoadFinished: loader=" + loader + " / data=" + data);

            layout.findViewById(R.id.tile_status_router_router_mem_loading_view).setVisibility(View.GONE);

            if (data == null) {
                data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
            }

            final TextView errorPlaceHolderView =
                    (TextView) this.layout.findViewById(R.id.tile_status_router_router_mem_error);

            Exception exception = data.getException();

            if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

                if (exception == null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }

                String property;
                int propertyUtilization;
                ProgressBar pb;
                TextView pbText;

                //Total
                property = data.getProperty(NVRAMInfo.Companion.getMEMORY_TOTAL());
                final String memTotalKb = property != null ? (property + " kB") : "-";

                ((TextView) layout.findViewById(
                        R.id.tile_status_router_router_mem_total_available)).setText(memTotalKb);

                //Free
                final TextView memFreeView =
                        (TextView) this.layout.findViewById(R.id.tile_status_router_router_mem_free);
                property = data.getProperty(NVRAMInfo.Companion.getMEMORY_FREE());
                final String memFreeKb = property != null ? (property + " kB") : "-";
                memFreeView.setText(memFreeKb + " / " + memTotalKb);

                pb = (ProgressBar) layout.findViewById(R.id.tile_status_router_router_mem_free_usage);
                pbText = (TextView) layout.findViewById(R.id.tile_status_router_router_mem_free_usage_text);
                try {
                    propertyUtilization = Integer.parseInt(data.getProperty(
                            NVRAMInfo.Companion.getMEMORY_FREE_PERCENT()));
                    if (propertyUtilization >= 0) {
                        pb.setProgress(propertyUtilization);
                        pbText.setText(propertyUtilization + "%");
                        pb.setVisibility(View.VISIBLE);
                        pbText.setVisibility(View.VISIBLE);
                    } else {
                        pb.setVisibility(View.GONE);
                        pbText.setVisibility(View.GONE);
                    }
                } catch (NumberFormatException e) {
                    ReportingUtils.reportException(mParentFragmentActivity, e);
                    pb.setVisibility(View.GONE);
                    pbText.setVisibility(View.GONE);
                }

                //Used
                final TextView memUsedView =
                        (TextView) this.layout.findViewById(R.id.tile_status_router_router_mem_used);
                property = data.getProperty(NVRAMInfo.Companion.getMEMORY_USED());
                final String memUsedKb = property != null ? (property + " kB") : "-";
                memUsedView.setText(memUsedKb + " / " + memTotalKb);
                pb = (ProgressBar) layout.findViewById(R.id.tile_status_router_router_mem_used_usage);
                pbText = (TextView) layout.findViewById(R.id.tile_status_router_router_mem_used_usage_text);
                try {
                    propertyUtilization = Integer.parseInt(data.getProperty(
                            NVRAMInfo.Companion.getMEMORY_USED_PERCENT()));
                    if (propertyUtilization >= 0) {
                        pb.setProgress(propertyUtilization);
                        pbText.setText(propertyUtilization + "%");
                        pb.setVisibility(View.VISIBLE);
                        pbText.setVisibility(View.VISIBLE);
                    } else {
                        pb.setVisibility(View.GONE);
                        pbText.setVisibility(View.GONE);
                    }
                } catch (NumberFormatException e) {
                    ReportingUtils.reportException(mParentFragmentActivity, e);
                    pb.setVisibility(View.GONE);
                    pbText.setVisibility(View.GONE);
                }

                //Buffers
                final TextView memBuffersView =
                        (TextView) this.layout.findViewById(R.id.tile_status_router_router_mem_buffers);
                property = data.getProperty(NVRAMInfo.Companion.getMEMORY_BUFFERS());
                final String memBuffersKb = property != null ? (property + " kB") : "-";
                memBuffersView.setText(memBuffersKb + " / " + memUsedKb);
                pb = (ProgressBar) layout.findViewById(R.id.tile_status_router_router_mem_buffers_usage);
                pbText =
                        (TextView) layout.findViewById(R.id.tile_status_router_router_mem_buffers_usage_text);
                try {
                    propertyUtilization =
                            Integer.parseInt(data.getProperty(NVRAMInfo.Companion.getMEMORY_BUFFERS_PERCENT()));
                    if (propertyUtilization >= 0) {
                        pb.setProgress(propertyUtilization);
                        pbText.setText(propertyUtilization + "%");
                        pb.setVisibility(View.VISIBLE);
                        pbText.setVisibility(View.VISIBLE);
                    } else {
                        pb.setVisibility(View.GONE);
                        pbText.setVisibility(View.GONE);
                    }
                } catch (NumberFormatException e) {
                    ReportingUtils.reportException(mParentFragmentActivity, e);
                    pb.setVisibility(View.GONE);
                    pbText.setVisibility(View.GONE);
                }

                //Cached
                final TextView memCachedView =
                        (TextView) this.layout.findViewById(R.id.tile_status_router_router_mem_cached);
                property = data.getProperty(NVRAMInfo.Companion.getMEMORY_CACHED());
                final String memCachedKb = property != null ? (property + " kB") : "-";
                memCachedView.setText(memCachedKb + " / " + memUsedKb);
                pb = (ProgressBar) layout.findViewById(R.id.tile_status_router_router_mem_cached_usage);
                pbText =
                        (TextView) layout.findViewById(R.id.tile_status_router_router_mem_cached_usage_text);
                try {
                    propertyUtilization = Integer.parseInt(data.getProperty(
                            NVRAMInfo.Companion.getMEMORY_CACHED_PERCENT()));
                    if (propertyUtilization >= 0) {
                        pb.setProgress(propertyUtilization);
                        pbText.setText(propertyUtilization + "%");
                        pb.setVisibility(View.VISIBLE);
                        pbText.setVisibility(View.VISIBLE);
                    } else {
                        pb.setVisibility(View.GONE);
                        pbText.setVisibility(View.GONE);
                    }
                } catch (NumberFormatException e) {
                    ReportingUtils.reportException(mParentFragmentActivity, e);
                    pb.setVisibility(View.GONE);
                    pbText.setVisibility(View.GONE);
                }

                //Active
                final TextView memActiveView =
                        (TextView) this.layout.findViewById(R.id.tile_status_router_router_mem_active);
                property = data.getProperty(NVRAMInfo.Companion.getMEMORY_ACTIVE());
                final String memActiveKb = property != null ? (property + " kB") : "-";
                memActiveView.setText(memActiveKb + " / " + memUsedKb);
                pb = (ProgressBar) layout.findViewById(R.id.tile_status_router_router_mem_active_usage);
                pbText =
                        (TextView) layout.findViewById(R.id.tile_status_router_router_mem_active_usage_text);
                try {
                    propertyUtilization = Integer.parseInt(data.getProperty(
                            NVRAMInfo.Companion.getMEMORY_ACTIVE_PERCENT()));
                    if (propertyUtilization >= 0) {
                        pb.setProgress(propertyUtilization);
                        pbText.setText(propertyUtilization + "%");
                        pb.setVisibility(View.VISIBLE);
                        pbText.setVisibility(View.VISIBLE);
                    } else {
                        pb.setVisibility(View.GONE);
                        pbText.setVisibility(View.GONE);
                    }
                } catch (NumberFormatException e) {
                    ReportingUtils.reportException(mParentFragmentActivity, e);
                    pb.setVisibility(View.GONE);
                    pbText.setVisibility(View.GONE);
                }

                //Inactive
                final TextView memInactiveView =
                        (TextView) this.layout.findViewById(R.id.tile_status_router_router_mem_inactive);
                property = data.getProperty(NVRAMInfo.Companion.getMEMORY_INACTIVE());
                final String memInactiveKb = property != null ? (property + " kB") : "-";
                memInactiveView.setText(memInactiveKb + " / " + memUsedKb);
                pb = (ProgressBar) layout.findViewById(R.id.tile_status_router_router_mem_inactive_usage);
                pbText =
                        (TextView) layout.findViewById(R.id.tile_status_router_router_mem_inactive_usage_text);
                try {
                    propertyUtilization =
                            Integer.parseInt(data.getProperty(NVRAMInfo.Companion.getMEMORY_INACTIVE_PERCENT()));
                    if (propertyUtilization >= 0) {
                        pb.setProgress(propertyUtilization);
                        pbText.setText(propertyUtilization + "%");
                        pb.setVisibility(View.VISIBLE);
                        pbText.setVisibility(View.VISIBLE);
                    } else {
                        pb.setVisibility(View.GONE);
                        pbText.setVisibility(View.GONE);
                    }
                } catch (NumberFormatException e) {
                    ReportingUtils.reportException(mParentFragmentActivity, e);
                    pb.setVisibility(View.GONE);
                    pbText.setVisibility(View.GONE);
                }

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
                    FirebaseCrashlytics.getInstance().log( "Init background loader for "
                            + StatusRouterMemoryTile.class
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

                    memInfoContents = null;

                    final NVRAMInfo nvramInfo = new NVRAMInfo();

                    updateProgressBarViewSeparator(10);
                    final String[] otherCmds;
                    if (isDemoRouter(mRouter)) {
                        otherCmds = new String[6];
                        final int memTotal = 40096;
                        final Random random = new Random();
                        final int memFree = random.nextInt(memTotal + 1);
                        otherCmds[0] = Integer.toString(memTotal); //MemTotal
                        otherCmds[1] = Integer.toString(memFree); //MemFree

                        final int memUsed = memTotal - memFree;
                        otherCmds[2] = Integer.toString(random.nextInt(memUsed + 1)); //Buffers
                        otherCmds[3] = Integer.toString(random.nextInt(memUsed + 1)); //Cached
                        otherCmds[4] = Integer.toString(random.nextInt(memUsed + 1)); //Active
                        otherCmds[5] = Integer.toString(random.nextInt(memUsed + 1)); //Inactive
                    } else {
                        otherCmds =
                                SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                        DDWRTFirmwareConnector.Companion.getGrepProcMemInfo("MemTotal:"),
                                        DDWRTFirmwareConnector.Companion.getGrepProcMemInfo("MemFree:"),
                                        DDWRTFirmwareConnector.Companion.getGrepProcMemInfo("Buffers:"),
                                        DDWRTFirmwareConnector.Companion.getGrepProcMemInfo("Cached:"),
                                        DDWRTFirmwareConnector.Companion.getGrepProcMemInfo("Active:"),
                                        DDWRTFirmwareConnector.Companion.getGrepProcMemInfo("Inactive:"));
                    }
                    updateProgressBarViewSeparator(30);
                    if (otherCmds != null && otherCmds.length >= 6) {
                        //Total
                        String memTotal = null;
                        List<String> strings = Splitter.on("MemTotal:")
                                .omitEmptyStrings()
                                .trimResults()
                                .splitToList(otherCmds[0].trim());
                        long memTotalLong = 0;
                        if (strings != null && strings.size() >= 1) {
                            memTotal = nullToEmpty(strings.get(0)).replaceAll(" kB", "").trim();
                            memTotalLong = Long.parseLong(memTotal);
                            nvramInfo.setProperty(NVRAMInfo.Companion.getMEMORY_TOTAL(), memTotal);
                        }

                        //Free
                        String memFree = null;
                        strings = Splitter.on("MemFree:")
                                .omitEmptyStrings()
                                .trimResults()
                                .splitToList(otherCmds[1].trim());
                        long memFreeLong = 0;
                        if (strings != null && strings.size() >= 1) {
                            memFree = nullToEmpty(strings.get(0)).replaceAll(" kB", "").trim();
                            memFreeLong = Long.parseLong(memFree);
                            nvramInfo.setProperty(NVRAMInfo.Companion.getMEMORY_FREE(), Long.toString(memFreeLong));

                            //Compute utlization
                            if (memTotalLong > 0L) {
                                nvramInfo.setProperty(NVRAMInfo.Companion.getMEMORY_FREE_PERCENT(),
                                        Long.toString(Math.min(100, 100 * memFreeLong / memTotalLong)));
                            }
                        }

                        //Mem used
                        String memUsed = null;
                        long memUsedLong = 0;
                        if (!(isNullOrEmpty(memTotal) || isNullOrEmpty(memFree))) {
                            //noinspection ConstantConditions
                            memUsedLong = memTotalLong - memFreeLong;
                            memUsed = Long.toString(memUsedLong);

                            nvramInfo.setProperty(NVRAMInfo.Companion.getMEMORY_USED(), memUsed);
                            if (memTotalLong > 0L) {
                                nvramInfo.setProperty(NVRAMInfo.Companion.getMEMORY_USED_PERCENT(),
                                        Long.toString(Math.min(100, 100 * memUsedLong / memTotalLong)));
                            }
                        }

                        //Buffers
                        String memBuffers = null;
                        strings = Splitter.on("Buffers:")
                                .omitEmptyStrings()
                                .trimResults()
                                .splitToList(otherCmds[2].trim());
                        long memBuffersLong = 0;
                        if (strings != null && strings.size() >= 1) {
                            memBuffers = nullToEmpty(strings.get(0)).replaceAll(" kB", "").trim();
                            memBuffersLong = Long.parseLong(memBuffers);
                            nvramInfo.setProperty(NVRAMInfo.Companion.getMEMORY_BUFFERS(), memBuffers);

                            //Compute utilization
                            if (memUsedLong > 0L) {
                                nvramInfo.setProperty(NVRAMInfo.Companion.getMEMORY_BUFFERS_PERCENT(),
                                        Long.toString(Math.min(100, 100 * memBuffersLong / memUsedLong)));
                            }
                        }

                        //Cached
                        String memCached = null;
                        strings = Splitter.on("Cached:")
                                .omitEmptyStrings()
                                .trimResults()
                                .splitToList(otherCmds[3].trim());
                        long memCachedLong = 0;
                        if (strings != null && strings.size() >= 1) {
                            memCached = nullToEmpty(strings.get(0)).replaceAll(" kB", "").trim();
                            memCachedLong = Long.parseLong(memCached);
                            nvramInfo.setProperty(NVRAMInfo.Companion.getMEMORY_CACHED(), memCached);
                            //Compute utilization
                            if (memUsedLong > 0L) {
                                nvramInfo.setProperty(NVRAMInfo.Companion.getMEMORY_CACHED_PERCENT(),
                                        Long.toString(Math.min(100, 100 * memCachedLong / memUsedLong)));
                            }
                        }

                        //Active
                        String memActive = null;
                        strings = Splitter.on("Active:")
                                .omitEmptyStrings()
                                .trimResults()
                                .splitToList(otherCmds[4].trim());
                        long memActiveLong = 0;
                        if (strings != null && strings.size() >= 1) {
                            memActive = nullToEmpty(strings.get(0)).replaceAll(" kB", "").trim();
                            memActiveLong = Long.parseLong(memActive);
                            nvramInfo.setProperty(NVRAMInfo.Companion.getMEMORY_ACTIVE(), memActive);
                            //Compute utilization
                            if (memUsedLong > 0L) {
                                nvramInfo.setProperty(NVRAMInfo.Companion.getMEMORY_ACTIVE_PERCENT(),
                                        Long.toString(Math.min(100, 100 * memActiveLong / memUsedLong)));
                            }
                        }

                        //Inactive
                        String memInactive = null;
                        strings = Splitter.on("Inactive:")
                                .omitEmptyStrings()
                                .trimResults()
                                .splitToList(otherCmds[5].trim());
                        long memInactiveLong = 0;
                        if (strings != null && strings.size() >= 1) {
                            memInactive = nullToEmpty(strings.get(0)).replaceAll(" kB", "").trim();
                            memInactiveLong = Long.parseLong(memInactive);
                            nvramInfo.setProperty(NVRAMInfo.Companion.getMEMORY_INACTIVE(), memInactive);
                            //Compute utilization
                            if (memUsedLong > 0L) {
                                nvramInfo.setProperty(NVRAMInfo.Companion.getMEMORY_INACTIVE_PERCENT(),
                                        Long.toString(Math.min(100, 100 * memInactiveLong / memUsedLong)));
                            }
                        }

                        updateProgressBarViewSeparator(60);

                        //Now cache whole /proc/cpuinfo, for detailed activity
                        if (isDemoRouter(mRouter)) {
                            memInfoContents = new String[2];
                            memInfoContents[0] = "Lorem Ipsum\n";
                            memInfoContents[1] = "Dolor sit amet...\n";
                        } else {
                            memInfoContents =
                                    SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                            "cat /proc/meminfo");
                        }
                        updateProgressBarViewSeparator(90);
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
        if (memInfoContents == null) {
            //Loading
            Utils.displayMessage(mParentFragmentActivity,
                    "Loading data from router - please wait a few seconds.", Style.ALERT);
            return null;
        }

        if (memInfoContents.length == 0) {
            //No data!
            Utils.displayMessage(mParentFragmentActivity, "No data available - please retry later.",
                    Style.ALERT);
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
