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
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.resources.ProcMountPoint;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.utils.SSHUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class StatusRouterSpaceUsageTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = StatusRouterSpaceUsageTile.class.getSimpleName();
    public static final Splitter NVRAM_SIZE_SPLITTER = Splitter.on("size: ").omitEmptyStrings().trimResults();

    public StatusRouterSpaceUsageTile(@NotNull SherlockFragment parentFragment, @NotNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_status_router_router_space_usage, R.id.tile_status_router_router_space_usage_togglebutton);

    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_status_router_router_space_usage_title;
    }

    @Nullable
    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @NotNull
            @Override
            public NVRAMInfo loadInBackground() {

                try {
                    Log.d(LOG_TAG, "Init background loader for " + StatusRouterSpaceUsageTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                        //Skip run
                        Log.d(LOG_TAG, "Skip loader run");
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    @NotNull final NVRAMInfo nvramInfo = new NVRAMInfo();

                    @NotNull final Map<String, ProcMountPoint> mountPointMap = new HashMap<String, ProcMountPoint>();
                    @NotNull final Map<String, List<ProcMountPoint>> mountTypes = new HashMap<String, List<ProcMountPoint>>();

                    @Nullable final String[] catProcMounts = SSHUtils.getManualProperty(mRouter,
                            mGlobalPreferences, "nvram show 2>&1 1>/dev/null", "cat /proc/mounts");
                    Log.d(LOG_TAG, "catProcMounts: " + Arrays.toString(catProcMounts));
                    @Nullable String cifsMountPoint = null;
                    if (catProcMounts != null && catProcMounts.length >= 1) {
                        final List<String> nvramUsageList = NVRAM_SIZE_SPLITTER
                                .splitToList(catProcMounts[0]);
                        if (nvramUsageList != null && !nvramUsageList.isEmpty()) {
                            nvramInfo.setProperty("nvram_space", nvramUsageList.get(0));
                        }

                        int i = 0;
                        for (@Nullable final String procMountLine : catProcMounts) {
                            if (i == 0 || procMountLine == null) {
                                i++;
                                continue;
                            }
                            final List<String> procMountLineItem = Splitter.on(" ").omitEmptyStrings().trimResults()
                                    .splitToList(procMountLine);

                            if (procMountLineItem != null) {
                                if (procMountLineItem.size() >= 6) {
                                    @NotNull final ProcMountPoint procMountPoint = new ProcMountPoint();
                                    procMountPoint.setDeviceType(procMountLineItem.get(0));
                                    procMountPoint.setMountPoint(procMountLineItem.get(1));
                                    procMountPoint.setFsType(procMountLineItem.get(2));

                                    if ("cifs".equalsIgnoreCase(procMountPoint.getFsType())) {
                                        cifsMountPoint = procMountPoint.getMountPoint();
                                    }

                                    final List<String> procMountLineItemPermissions = Splitter.on(",").omitEmptyStrings().trimResults()
                                            .splitToList(procMountLineItem.get(3));
                                    if (procMountLineItemPermissions != null) {
                                        for (String procMountLineItemPermission : procMountLineItemPermissions) {
                                            procMountPoint.addPermission(procMountLineItemPermission);
                                        }
                                    }
                                    procMountPoint.addOtherAttr(procMountLineItem.get(4));

                                    mountPointMap.put(procMountPoint.getMountPoint(), procMountPoint);

                                    if (mountTypes.get(procMountPoint.getFsType()) == null) {
                                        mountTypes.put(procMountPoint.getFsType(), new ArrayList<ProcMountPoint>());
                                    }
                                }
                            }
                        }
                    }

                    @NotNull final List<String> itemsToDf = new ArrayList<String>();

                    //JFFS Space: "jffs_space"
                    final ProcMountPoint jffsProcMountPoint = mountPointMap.get("/jffs");
                    if (jffsProcMountPoint != null) {
                        itemsToDf.add(jffsProcMountPoint.getMountPoint());
                    }

                    //CIFS: "cifs_space"
                    if (cifsMountPoint != null) {
                        final ProcMountPoint cifsProcMountPoint = mountPointMap.get(cifsMountPoint);
                        if (cifsProcMountPoint != null) {
                            itemsToDf.add(cifsProcMountPoint.getMountPoint());
                        }
                    }

                    for (final String itemToDf : itemsToDf) {
                        @Nullable final String[] itemToDfResult = SSHUtils.getManualProperty(mRouter,
                                mGlobalPreferences, "df -h " + itemToDf + " | grep -v Filessytem | grep \"" + itemToDf + "\"");
                        Log.d(LOG_TAG, "catProcMounts: " + Arrays.toString(catProcMounts));
                        if (itemToDfResult != null && itemToDfResult.length > 0) {
                            final List<String> procMountLineItem = Splitter.on(" ").omitEmptyStrings().trimResults()
                                    .splitToList(itemToDfResult[0]);
                            if (procMountLineItem == null) {
                                continue;
                            }

                            if ("/jffs".equalsIgnoreCase(itemToDf)) {
                                if (procMountLineItem.size() >= 4) {
                                    nvramInfo.setProperty("jffs_space_max", procMountLineItem.get(1));
                                    nvramInfo.setProperty("jffs_space_used", procMountLineItem.get(2));
                                    nvramInfo.setProperty("jffs_space_available", procMountLineItem.get(3));
                                    nvramInfo.setProperty("jffs_space", procMountLineItem.get(1) + " (" + procMountLineItem.get(3) + " left)");
                                }
                            } else if (cifsMountPoint != null && cifsMountPoint.equalsIgnoreCase(itemToDf)) {
                                if (procMountLineItem.size() >= 3) {
                                    nvramInfo.setProperty("cifs_space_max", procMountLineItem.get(0));
                                    nvramInfo.setProperty("cifs_space_used", procMountLineItem.get(1));
                                    nvramInfo.setProperty("cifs_space_available", procMountLineItem.get(2));
                                    nvramInfo.setProperty("cifs_space", procMountLineItem.get(0) + " (" + procMountLineItem.get(2) + " left)");
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

        layout.findViewById(R.id.tile_status_router_router_space_usage_loading_view)
                .setVisibility(View.GONE);
        layout.findViewById(R.id.tile_status_router_router_space_usage_gridLayout)
                .setVisibility(View.VISIBLE);

        if (data == null) {
            data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
        }

        @NotNull final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_space_usage_error);

        @Nullable final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {
            if (exception == null) {
                 errorPlaceHolderView.setVisibility(View.GONE);
            }

            //NVRAM
            @NotNull final TextView nvramSpaceView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_space_usage_nvram);
            nvramSpaceView.setText(data.getProperty("nvram_space", "N/A"));

            //NVRAM
            @NotNull final TextView cifsSpaceView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_space_usage_cifs);
            cifsSpaceView.setText(data.getProperty("cifs_space", "N/A"));

            //NVRAM
            @NotNull final TextView jffsView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_space_usage_jffs2);
            jffsView.setText(data.getProperty("jffs_space", "N/A"));

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
                R.id.tile_status_router_router_space_usage_togglebutton_title, R.id.tile_status_router_router_space_usage_togglebutton_separator);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");

    }

    @Nullable
    @Override
    protected Intent getOnclickIntent() {
        //TODO
        return null;
    }
}
