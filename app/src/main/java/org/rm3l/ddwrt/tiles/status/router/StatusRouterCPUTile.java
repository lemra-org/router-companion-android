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
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
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

import java.util.List;

import static com.google.common.base.Strings.nullToEmpty;

/**
 *
 */
public class StatusRouterCPUTile extends DDWRTTile<NVRAMInfo> {

    public static final String GREP_MODEL_NAME_PROC_CPUINFO = "grep \"model name\" /proc/cpuinfo ";
    private static final String LOG_TAG = StatusRouterCPUTile.class.getSimpleName();

//    Drawable icon;

    public StatusRouterCPUTile(@NotNull SherlockFragment parentFragment, @NotNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_status_router_router_cpu, R.id.tile_status_router_router_cpu_togglebutton);
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_status_router_router_cpu_title;
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

                    Log.d(LOG_TAG, "Init background loader for " + StatusRouterCPUTile.class + ": routerInfo=" +
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
                        nvramInfoTmp = SSHUtils.getNVRamInfoFromRouter(mRouter,
                                mGlobalPreferences, NVRAMInfo.CPU_CLOCK_FREQ);
                    } finally {
                        if (nvramInfoTmp != null) {
                            nvramInfo.putAll(nvramInfoTmp);
                        }

                        List<String> strings = Splitter.on(",")
                                .omitEmptyStrings()
                                .trimResults()
                                .splitToList(nullToEmpty(nvramInfo.getProperty(NVRAMInfo.CPU_CLOCK_FREQ)));
                        Log.d(LOG_TAG, "strings for cpu clock: " + strings);
                        if (strings != null && strings.size() > 0) {
                            nvramInfo.setProperty(NVRAMInfo.CPU_CLOCK_FREQ, strings.get(0));
                        }

                        @Nullable final String[] otherCmds = SSHUtils.getManualProperty(mRouter,
                                mGlobalPreferences, GREP_MODEL_NAME_PROC_CPUINFO +
                                        "| uniq", GREP_MODEL_NAME_PROC_CPUINFO + "| wc -l", "uptime");
                        if (otherCmds != null && otherCmds.length >= 3) {
                            //Model
                            final String modelNameLine = otherCmds[0];
                            if (modelNameLine != null) {
                                nvramInfo.setProperty(NVRAMInfo.CPU_MODEL,
                                        modelNameLine
                                                .replace(":", "")
                                                .replace("model name", "")
                                        .trim());
                            }
//                            strings = Splitter.on("model name\t:").omitEmptyStrings().trimResults().splitToList(modelNameLine);
//                            Log.d(LOG_TAG, "strings for model name: " + strings);
//                            if (strings != null && strings.size() >= 1) {
//                                nvramInfo.setProperty(NVRAMInfo.CPU_MODEL, strings.get(0));
//                            }

                            //Nb Cores
                            nvramInfo.setProperty(NVRAMInfo.CPU_CORES_COUNT, otherCmds[1]);

                            //Load Avg
                            strings = Splitter.on("load average").omitEmptyStrings().trimResults().splitToList(otherCmds[2]);
                            Log.d(LOG_TAG, "strings for load avg: " + strings);
                            if (strings != null && strings.size() >= 2) {
                                final String loadAvg = strings.get(1);
                                if (loadAvg != null) {
                                    nvramInfo.setProperty(NVRAMInfo.LOAD_AVERAGE,
                                            loadAvg.replace(":", "").trim());
                                }
                            }

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
    public void onLoadFinished(@NotNull final Loader<NVRAMInfo> loader, @Nullable NVRAMInfo data) {

        //Set tiles
        Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

        layout.findViewById(R.id.tile_status_router_router_cpu_speed_header_loading_view)
                .setVisibility(View.GONE);
        layout.findViewById(R.id.tile_status_router_router_cpu_loading_view)
                .setVisibility(View.GONE);
        layout.findViewById(R.id.tile_status_router_router_cpu_speed)
                .setVisibility(View.VISIBLE);
        layout.findViewById(R.id.tile_status_router_router_cpu_grid_layout)
                .setVisibility(View.VISIBLE);


        if (data == null) {
            data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
        }

        @NotNull final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_cpu_error);

        @Nullable final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {
            if (exception == null) {
                errorPlaceHolderView.setVisibility(View.GONE);
            }

            //Model
            @NotNull final TextView cpuSpeedView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_cpu_speed);
            final String property = data.getProperty(NVRAMInfo.CPU_CLOCK_FREQ);
            cpuSpeedView.setText(Strings.isNullOrEmpty(property) ? null : (property + " MHz"));

            //Model
            @NotNull final TextView cpuModelView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_cpu_model);
            cpuModelView.setText(data.getProperty(NVRAMInfo.CPU_MODEL, "-"));

            //Cores Count
            @NotNull final TextView cpuCountView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_cpu_cores);
            cpuCountView.setText(data.getProperty(NVRAMInfo.CPU_CORES_COUNT, "-"));

            //Load Avg
            @NotNull final TextView loadAvgView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_cpu_load_avg);
            loadAvgView.setText(data.getProperty(NVRAMInfo.LOAD_AVERAGE, "-"));

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
                R.id.tile_status_router_router_cpu_togglebutton_title, R.id.tile_status_router_router_cpu_togglebutton_separator);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");
    }

    @Nullable
    @Override
    protected OnClickIntent getOnclickIntent() {
        //TODO
        return null;
    }
}
