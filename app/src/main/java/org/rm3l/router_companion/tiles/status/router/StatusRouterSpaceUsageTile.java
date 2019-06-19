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

import static org.rm3l.router_companion.utils.Utils.SPACE_SPLITTER;

import android.content.Context;
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
import com.crashlytics.android.Crashlytics;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.resources.ProcMountPoint;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.utils.ReportingUtils;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;

/**
 *
 */
public class StatusRouterSpaceUsageTile extends DDWRTTile<NVRAMInfo> {

    public static final Splitter NVRAM_SIZE_SPLITTER =
            Splitter.on("size: ").omitEmptyStrings().trimResults();

    private static final String LOG_TAG = StatusRouterSpaceUsageTile.class.getSimpleName();

    private long mLastSync;

    public StatusRouterSpaceUsageTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments,
            @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_status_router_router_space_usage, null);
    }

    @Override
    public Integer getTileHeaderViewId() {
        return R.id.tile_status_router_router_space_usage_hdr;
    }

    @Override
    public Integer getTileTitleViewId() {
        return R.id.tile_status_router_router_space_usage_title;
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
            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

            layout.findViewById(R.id.tile_status_router_router_space_usage_loading_view)
                    .setVisibility(View.GONE);
            layout.findViewById(R.id.tile_status_router_router_space_usage_gridLayout)
                    .setVisibility(View.VISIBLE);

            if (data == null) {
                data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
            }

            final TextView errorPlaceHolderView =
                    (TextView) this.layout.findViewById(R.id.tile_status_router_router_space_usage_error);

            final Exception exception = data.getException();

            if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {
                if (exception == null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }

                //NVRAM
                final TextView nvramSpaceView =
                        (TextView) this.layout.findViewById(R.id.tile_status_router_router_space_usage_nvram);
                nvramSpaceView.setText(data.getProperty("nvram_space", "-"));

                //CIFS
                final TextView cifsSpaceView =
                        (TextView) this.layout.findViewById(R.id.tile_status_router_router_space_usage_cifs);
                cifsSpaceView.setText(data.getProperty("cifs_space", "-"));

                //JFFS
                final TextView jffsView =
                        (TextView) this.layout.findViewById(R.id.tile_status_router_router_space_usage_jffs2);
                jffsView.setText(data.getProperty("jffs_space", "-"));

                //Update usages as well
                ProgressBar pb = (ProgressBar) layout.findViewById(
                        R.id.tile_status_router_router_space_usage_nvram_usage);
                TextView pbText = (TextView) layout.findViewById(
                        R.id.tile_status_router_router_space_usage_nvram_usage_text);
                try {
                    final int propertyUtilization =
                            Integer.parseInt(data.getProperty(NVRAMInfo.Companion.getNVRAM_USED_PERCENT()));
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

                pb = (ProgressBar) layout.findViewById(
                        R.id.tile_status_router_router_space_usage_cifs_usage);
                pbText = (TextView) layout.findViewById(
                        R.id.tile_status_router_router_space_usage_cifs_usage_text);
                try {
                    final int propertyUtilization =
                            Integer.parseInt(data.getProperty(NVRAMInfo.Companion.getSTORAGE_CIFS_USED_PERCENT()));
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

                pb = (ProgressBar) layout.findViewById(
                        R.id.tile_status_router_router_space_usage_jffs2_usage);
                pbText = (TextView) layout.findViewById(
                        R.id.tile_status_router_router_space_usage_jffs2_usage_text);
                try {
                    final int propertyUtilization =
                            Integer.parseInt(data.getProperty(NVRAMInfo.Companion.getSTORAGE_JFFS2_USED_PERCENT()));
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
                    Crashlytics.log(Log.DEBUG, LOG_TAG, "NumberFormatException" + e.getMessage());
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

            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished(): done loading!");
        } finally {
            mRefreshing.set(false);
            doneWithLoaderInstance(this, loader);
        }
    }

    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @NonNull
            @Override
            public NVRAMInfo loadInBackground() {

                try {
                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Init background loader for "
                            + StatusRouterSpaceUsageTile.class
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

                    final NVRAMInfo nvramInfo = new NVRAMInfo();

                    final Map<String, ProcMountPoint> mountPointMap = new HashMap<String, ProcMountPoint>();
                    final Map<String, List<ProcMountPoint>> mountTypes =
                            new HashMap<String, List<ProcMountPoint>>();

                    updateProgressBarViewSeparator(10);

                    final String[] catProcMounts;
                    if (Utils.isDemoRouter(mRouter)) {
                        catProcMounts = new String[6];
                        catProcMounts[0] = "size: 23855 bytes (7 left)";
                        catProcMounts[1] = "rootfs / rootfs rw 0 0";
                        catProcMounts[2] = "/dev/root / squashfs ro 0 0";
                        catProcMounts[3] = "none /dev devfs rw 0 0";
                        catProcMounts[4] = "proc /proc proc rw 0 0";
                        catProcMounts[5] = "ramfs /tmp ramfs rw 0 0";
                    } else {
                        catProcMounts =
                                SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                        "/usr/sbin/nvram show 2>&1 1>/dev/null", "/bin/cat /proc/mounts");
                    }

                    updateProgressBarViewSeparator(20);

                    Crashlytics.log(Log.DEBUG, LOG_TAG, "catProcMounts: " + Arrays.toString(catProcMounts));
                    String cifsMountPoint = null;
                    if (catProcMounts != null && catProcMounts.length >= 1) {
                        final List<String> nvramUsageList = NVRAM_SIZE_SPLITTER.splitToList(catProcMounts[0]);
                        if (nvramUsageList != null && !nvramUsageList.isEmpty()) {
                            nvramInfo.setProperty("nvram_space", nvramUsageList.get(0));
                        }

                        int i = 0;
                        for (final String procMountLine : catProcMounts) {
                            if (i == 0 || procMountLine == null) {
                                i++;
                                continue;
                            }
                            final List<String> procMountLineItem =
                                    Splitter.on(" ").omitEmptyStrings().trimResults().splitToList(procMountLine);

                            if (procMountLineItem != null) {
                                if (procMountLineItem.size() >= 6) {
                                    final ProcMountPoint procMountPoint = new ProcMountPoint();
                                    procMountPoint.setDeviceType(procMountLineItem.get(0));
                                    procMountPoint.setMountPoint(procMountLineItem.get(1));
                                    procMountPoint.setFsType(procMountLineItem.get(2));

                                    if ("cifs".equalsIgnoreCase(procMountPoint.getFsType())) {
                                        cifsMountPoint = procMountPoint.getMountPoint();
                                    }

                                    final List<String> procMountLineItemPermissions = Splitter.on(",")
                                            .omitEmptyStrings()
                                            .trimResults()
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

                    final List<String> itemsToDf = new ArrayList<String>();

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

                    updateProgressBarViewSeparator(30);

                    for (final String itemToDf : itemsToDf) {
                        final String[] itemToDfResult;
                        if (Utils.isDemoRouter(mRouter)) {
                            itemToDfResult = new String[1];
                            itemToDfResult[0] =
                                    String.format("%s                 2.8M      2.8M         0 100% /", itemToDf);
                        } else {
                            itemToDfResult =
                                    SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                            "df -h " + itemToDf + " | grep -v Filessytem | grep \"" + itemToDf
                                                    + "\"");
                        }
                        Crashlytics.log(Log.DEBUG, LOG_TAG, "catProcMounts: " + Arrays.toString(catProcMounts));
                        if (itemToDfResult != null && itemToDfResult.length > 0) {
                            final List<String> procMountLineItem =
                                    Splitter.on(" ").omitEmptyStrings().trimResults().splitToList(itemToDfResult[0]);
                            if (procMountLineItem == null) {
                                continue;
                            }

                            if ("/jffs".equalsIgnoreCase(itemToDf)) {
                                if (procMountLineItem.size() >= 4) {
                                    nvramInfo.setProperty("jffs_space_max", procMountLineItem.get(1));
                                    nvramInfo.setProperty("jffs_space_used", procMountLineItem.get(2));
                                    nvramInfo.setProperty("jffs_space_available", procMountLineItem.get(3));
                                    nvramInfo.setProperty("jffs_space",
                                            procMountLineItem.get(1) + " (" + procMountLineItem.get(3) + " left)");
                                }
                            } else if (cifsMountPoint != null && cifsMountPoint.equalsIgnoreCase(itemToDf)) {
                                if (procMountLineItem.size() >= 3) {
                                    nvramInfo.setProperty("cifs_space_max", procMountLineItem.get(0));
                                    nvramInfo.setProperty("cifs_space_used", procMountLineItem.get(1));
                                    nvramInfo.setProperty("cifs_space_available", procMountLineItem.get(2));
                                    nvramInfo.setProperty("cifs_space",
                                            procMountLineItem.get(0) + " (" + procMountLineItem.get(2) + " left)");
                                }
                            }
                        }
                    }

                    //Compute space as well

                    final String[] nvramSize;
                    if (Utils.isDemoRouter(mRouter)) {
                        nvramSize = new String[1];
                        nvramSize[0] = "size: 21157 bytes (44379 left)";
                    } else {
                        nvramSize =
                                SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                        "/usr/sbin/nvram show 2>&1 1>/dev/null | grep \"size: \"");
                    }

                    updateProgressBarViewSeparator(40);
                    final String[] jffs2Size;
                    if (Utils.isDemoRouter(mRouter)) {
                        jffs2Size = new String[1];
                        jffs2Size[0] = (nbRunsLoader % 3 == 0 ? ""
                                : "/dev/mtdblock/5      jffs2          100123M      40000M     120000   30% /jffs");
                    } else {
                        jffs2Size =
                                SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                        "/bin/df -T | grep \"jffs2\"");
                    }

                    updateProgressBarViewSeparator(50);
                    final String[] cifsSize;
                    if (Utils.isDemoRouter(mRouter)) {
                        cifsSize = new String[1];
                        cifsSize[0] = (nbRunsLoader % 3 == 0 ? ""
                                : "/dev/mtdblock/5      cifs          93800      2400     91300   50% /cifs");
                    } else {
                        cifsSize =
                                SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                        "/bin/df -T | grep \"cifs\"");
                    }

                    updateProgressBarViewSeparator(60);

                    if (nvramSize != null && nvramSize.length >= 1) {
                        final String nvramSizeStr = nvramSize[0];
                        if (nvramSizeStr != null && nvramSizeStr.startsWith("size:")) {
                            final List<String> stringList = SPACE_SPLITTER.splitToList(nvramSizeStr);
                            if (stringList.size() >= 5) {
                                final String nvramTotalBytes = stringList.get(1);
                                final String nvramLeftBytes = stringList.get(3).replace("(", "");
                                try {
                                    final long nvramTotalBytesLong = Long.parseLong(nvramTotalBytes);
                                    final long nvramLeftBytesLong = Long.parseLong(nvramLeftBytes);
                                    final long nvramUsedBytesLong = nvramTotalBytesLong - nvramLeftBytesLong;
                                    nvramInfo.setProperty(NVRAMInfo.Companion.getNVRAM_USED_PERCENT(),
                                            Long.toString(
                                                    Math.min(100, 100 * nvramUsedBytesLong / nvramTotalBytesLong)));
                                } catch (final NumberFormatException e) {
                                    ReportingUtils.reportException(mParentFragmentActivity, e);
                                }
                            }
                        }
                    }
                    updateProgressBarViewSeparator(70);

                    if (jffs2Size != null && jffs2Size.length >= 1) {
                        //We may have more than one mountpoint - so sum everything up
                        long totalUsed = 0;
                        long totalSize = 0;
                        for (int i = 0; i < jffs2Size.length; i++) {
                            final String jffs2SizeStr = jffs2Size[i];
                            if (!Strings.isNullOrEmpty(jffs2SizeStr)) {
                                final List<String> stringList = SPACE_SPLITTER.splitToList(jffs2SizeStr);
                                if (stringList.size() >= 7) {
                                    try {
                                        totalSize += Long.parseLong(stringList.get(2));
                                        totalUsed += Long.parseLong(stringList.get(3));
                                    } catch (final NumberFormatException e) {
                                        ReportingUtils.reportException(mParentFragmentActivity, e);
                                    }
                                }
                            }
                            updateProgressBarViewSeparator(Math.min(80, 70 + 5 + i));
                        }
                        if (totalSize > 0) {
                            nvramInfo.setProperty(NVRAMInfo.Companion.getSTORAGE_JFFS2_USED_PERCENT(),
                                    Long.toString(Math.min(100, 100 * totalUsed / totalSize)));
                        }
                    }
                    updateProgressBarViewSeparator(80);

                    if (cifsSize != null && cifsSize.length >= 1) {
                        //We may have more than one mountpoint - so sum everything up
                        long totalUsed = 0;
                        long totalSize = 0;
                        for (int i = 0; i < cifsSize.length; i++) {
                            final String cifsSizeStr = cifsSize[i];
                            if (!Strings.isNullOrEmpty(cifsSizeStr)) {
                                final List<String> stringList = SPACE_SPLITTER.splitToList(cifsSizeStr);
                                if (stringList.size() >= 7) {
                                    try {
                                        totalSize += Long.parseLong(stringList.get(2));
                                        totalUsed += Long.parseLong(stringList.get(3));
                                    } catch (final NumberFormatException e) {
                                        ReportingUtils.reportException(mParentFragmentActivity, e);
                                    }
                                }
                            }
                            updateProgressBarViewSeparator(Math.min(87, 80 + i));
                        }
                        if (totalSize > 0) {
                            nvramInfo.setProperty(NVRAMInfo.Companion.getSTORAGE_CIFS_USED_PERCENT(),
                                    Long.toString(Math.min(100, 100 * totalUsed / totalSize)));
                        }
                    }
                    updateProgressBarViewSeparator(90);

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
        //TODO
        return null;
    }
}
