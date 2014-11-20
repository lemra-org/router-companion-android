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
import org.rm3l.ddwrt.api.ProcMountPoint;
import org.rm3l.ddwrt.api.conn.NVRAMInfo;
import org.rm3l.ddwrt.api.conn.Router;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
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

//    Drawable icon;

    public StatusRouterSpaceUsageTile(@NotNull SherlockFragmentActivity parentFragmentActivity, @NotNull Bundle arguments, @Nullable Router router) {
        super(parentFragmentActivity, arguments, router, R.layout.tile_status_router_router_space_usage, R.id.tile_status_router_router_space_usage_togglebutton);
//        // Parse the SVG file from the resource beforehand
//        try {
//            final SVG svg = SVGParser.getSVGFromResource(this.mParentFragmentActivity.getResources(), R.raw.disk);
//            // Get a drawable from the parsed SVG and set it as the drawable for the ImageView
//            this.icon = svg.createPictureDrawable();
//        } catch (final Exception e) {
//            e.printStackTrace();
//            this.icon = this.mParentFragmentActivity.getResources().getDrawable(R.drawable.ic_icon_state);
//        }
    }
//
//    @Nullable
//    @Override
//    public ViewGroup getViewGroupLayout() {
//        final LinearLayout layout = (LinearLayout) this.mParentFragmentActivity.getLayoutInflater().inflate(R.layout.tile_status_router_router_space_usage, null);
//        mToggleAutoRefreshButton = (ToggleButton) layout.findViewById(R.id.tile_status_router_router_space_usage_togglebutton);
//        mToggleAutoRefreshButton.setOnCheckedChangeListener(this);
//
//        return layout;
////        final ImageView imageView = (ImageView) layout.findViewById(R.id.ic_tile_status_router_router_space_usage);
////        imageView.setImageDrawable(this.icon);
////        imageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
////        return layout;
//    }

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
                            "nvram show 2>&1 1>/dev/null", "cat /proc/mounts");
                    Log.d(LOG_TAG, "catProcMounts: " + Arrays.toString(catProcMounts));
                    @Nullable String cifsMountPoint = null;
                    if (catProcMounts != null && catProcMounts.length >= 1) {
                        final List<String> nvramUsageList = Splitter.on("size: ").omitEmptyStrings().trimResults()
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
                                "df -h " + itemToDf + " | grep -v Filessytem | grep \"" + itemToDf + "\"");
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

        if (data == null) {
            data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
        }

        @NotNull final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_space_usage_error);

        @Nullable final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {
            if (exception == null) {
                if (errorPlaceHolderView != null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }
            }

            //NVRAM
            @NotNull final TextView nvramSpaceView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_space_usage_nvram);
            if (nvramSpaceView != null) {
                nvramSpaceView.setText(data.getProperty("nvram_space", "N/A"));
            }

            //NVRAM
            @NotNull final TextView cifsSpaceView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_space_usage_cifs);
            if (cifsSpaceView != null) {
                cifsSpaceView.setText(data.getProperty("cifs_space", "N/A"));
            }

            //NVRAM
            @NotNull final TextView jffsView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_space_usage_jffs2);
            if (jffsView != null) {
                jffsView.setText(data.getProperty("jffs_space", "N/A"));
            }

        }

        if (exception != null) {
            errorPlaceHolderView.setText(exception.getClass().getSimpleName() + ": " + Throwables.getRootCause(exception).getMessage());
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
