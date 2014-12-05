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

package org.rm3l.ddwrt.tiles.status.router;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.common.base.Splitter;
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

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 *
 */
public class StatusRouterMemoryTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = StatusRouterMemoryTile.class.getSimpleName();

    public StatusRouterMemoryTile(@NotNull SherlockFragmentActivity parentFragmentActivity, @NotNull Bundle arguments, @Nullable Router router) {
        super(parentFragmentActivity, arguments, router, R.layout.tile_status_router_router_mem, R.id.tile_status_router_router_mem_togglebutton);
    }

    @NotNull
    private static String getGrepProcMemInfo(@NotNull final String item) {
        return "grep \"" + item + "\" /proc/meminfo ";
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

            @NotNull
            @Override
            public NVRAMInfo loadInBackground() {

                try {
                    Log.d(LOG_TAG, "Init background loader for " + StatusRouterMemoryTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                        //Skip run
                        Log.d(LOG_TAG, "Skip loader run");
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    @NotNull final NVRAMInfo nvramInfo = new NVRAMInfo();

                    @Nullable final String[] otherCmds = SSHUtils.getManualProperty(mRouter,
                            getGrepProcMemInfo("MemTotal"), getGrepProcMemInfo("MemFree"));
                    if (otherCmds != null && otherCmds.length >= 2) {
                        //Total
                        @Nullable String memTotal = null;
                        List<String> strings = Splitter.on("MemTotal:").omitEmptyStrings()
                                .trimResults().splitToList(otherCmds[0].trim());
                        Log.d(LOG_TAG, "strings for MemTotal: " + strings);
                        if (strings != null && strings.size() >= 1) {
                            memTotal = strings.get(0);
                            nvramInfo.setProperty(NVRAMInfo.MEMORY_TOTAL, memTotal);

                        }

                        //Free
                        @Nullable String memFree = null;
                        strings = Splitter.on("MemFree:").omitEmptyStrings().trimResults()
                                .splitToList(otherCmds[1].trim());
                        Log.d(LOG_TAG, "strings for MemFree: " + strings);
                        if (strings != null && strings.size() >= 1) {
                            memFree = strings.get(0);
                            nvramInfo.setProperty(NVRAMInfo.MEMORY_FREE, strings.get(0));

                        }

                        //Mem used
                        @Nullable String memUsed = null;
                        if (!(isNullOrEmpty(memTotal) || isNullOrEmpty(memFree))) {
                            memUsed = Long.toString(
                                    Long.parseLong(memTotal.replaceAll(" kB", "")) - Long.parseLong(memFree.replaceAll(" kB", "")))
                                    + " kB";

                            nvramInfo.setProperty(NVRAMInfo.MEMORY_USED, memUsed);

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

        if (data == null) {
            data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
        }

        @NotNull final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_mem_error);

        @Nullable final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

            if (exception == null) {
                if (errorPlaceHolderView != null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }
            }

            //Total
            @NotNull final TextView memTotalView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_mem_total);
            if (memTotalView != null) {
                memTotalView.setText(data.getProperty(NVRAMInfo.MEMORY_TOTAL));
            }

            //Model
            @NotNull final TextView memFreeView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_mem_free);
            if (memFreeView != null) {
                memFreeView.setText(data.getProperty(NVRAMInfo.MEMORY_FREE, "N/A"));
            }

            //Cores Count
            @NotNull final TextView memUsedView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_mem_used);
            if (memUsedView != null) {
                memUsedView.setText(data.getProperty(NVRAMInfo.MEMORY_USED, "N/A"));
            }

        }

        if (exception != null) {
            errorPlaceHolderView.setText("Error: " + Throwables.getRootCause(exception).getMessage());
            errorPlaceHolderView.setVisibility(View.VISIBLE);
        }

        doneWithLoaderInstance(this, loader,
                R.id.tile_status_router_router_mem_togglebutton_title, R.id.tile_status_router_router_mem_togglebutton_separator);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");
    }

    @Nullable
    @Override
    protected Intent getOnclickIntent() {
        //TODO
        return null;
    }
}
