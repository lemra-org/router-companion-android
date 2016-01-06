package org.rm3l.ddwrt.tiles.dashboard.system;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.github.lzyzsd.circleprogress.ArcProgress;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.main.DDWRTMainActivity;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.status.router.StatusRouterMemoryTile;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.util.List;

/**
 * Created by rm3l on 03/01/16.
 */
public class StorageUsageTile extends DDWRTTile<NVRAMInfo>  {

    private static final String LOG_TAG = StorageUsageTile.class.getSimpleName();
    public static final Splitter SPACE_SPLITTER = Splitter.on(" ").omitEmptyStrings()
            .trimResults();

    private boolean isThemeLight;
    final int red;
    final int orange;

    private long mLastSync;

    private final ArcProgress mNVRAMArcProgress;
    private final ArcProgress mJFFS2ArcProgress;
    private final ArcProgress mCIFSArcProgress;

    public StorageUsageTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_dashboard_storage, null);
        isThemeLight = ColorUtils.isThemeLight(mParentFragmentActivity);

        red = ContextCompat.getColor(mParentFragmentActivity,
                R.color.win8_red);
        orange = ContextCompat.getColor(mParentFragmentActivity,
                R.color.win8_orange);

        final View.OnClickListener onClickListener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //Open Router State tab
                if (mParentFragmentActivity instanceof DDWRTMainActivity) {
                    ((DDWRTMainActivity) mParentFragmentActivity)
                            .selectItemInDrawer(2);
                } else {
                    //TODO Set proper flags ???
                    final Intent intent = new Intent(mParentFragmentActivity, DDWRTMainActivity.class);
                    intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouter.getUuid());
                    intent.putExtra(DDWRTMainActivity.SAVE_ITEM_SELECTED, 2);
                    mParentFragmentActivity.startActivity(intent);
                }
            }
        };

        this.mNVRAMArcProgress = (ArcProgress)
                layout.findViewById(R.id.tile_dashboard_storage_nvram_arcprogress);
        this.mJFFS2ArcProgress = (ArcProgress)
                layout.findViewById(R.id.tile_dashboard_storage_jffs2_arcprogress);
        this.mCIFSArcProgress = (ArcProgress)
                layout.findViewById(R.id.tile_dashboard_storage_cifs_arcprogress);

        this.mNVRAMArcProgress.setOnClickListener(onClickListener);
        this.mJFFS2ArcProgress.setOnClickListener(onClickListener);
        this.mCIFSArcProgress.setOnClickListener(onClickListener);
    }

    @Override
    public int getTileHeaderViewId() {
        return -1;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_dashboard_storage_title;
    }

    @Nullable
    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {

                try {
                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Init background loader for " + StatusRouterMemoryTile.class + ": routerInfo=" +
                            mRouter + " / nbRunsLoader=" + nbRunsLoader);

                    isThemeLight = ColorUtils.isThemeLight(mParentFragmentActivity);

                    if (mRefreshing.getAndSet(true)) {
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    updateProgressBarViewSeparator(0);

                    mLastSync = System.currentTimeMillis();

                    final NVRAMInfo nvramInfo = new NVRAMInfo();

                    updateProgressBarViewSeparator(10);
                    final String[] nvramSize;
                    if (Utils.isDemoRouter(mRouter)) {
                        nvramSize = new String[1];
                        nvramSize[0] = "size: 21157 bytes (44379 left)";
                    } else {
                        nvramSize = SSHUtils.getManualProperty(mParentFragmentActivity,
                                mRouter,
                                mGlobalPreferences,
                                "/usr/sbin/nvram show 2>&1 1>/dev/null | grep \"size: \"");
                    }

                    updateProgressBarViewSeparator(20);
                    final String[] jffs2Size;
                    if (Utils.isDemoRouter(mRouter)) {
                        jffs2Size = new String[1];
                        jffs2Size[0] = (nbRunsLoader % 3 == 0 ?
                                "" :
                                "/dev/mtdblock/5      jffs2          100123M      40000M     120000   30% /jffs");
                    } else {
                        jffs2Size = SSHUtils.getManualProperty(mParentFragmentActivity,
                                mRouter,
                                mGlobalPreferences,
                                "/bin/df -T | grep \"jffs2\"");
                    }

                    updateProgressBarViewSeparator(30);
                    final String[] cifsSize;
                    if (Utils.isDemoRouter(mRouter)) {
                        cifsSize = new String[1];
                        cifsSize[0] = (nbRunsLoader % 3 == 0 ?
                                "" :
                                "/dev/mtdblock/5      cifs          93800      2400     91300   50% /cifs");
                    } else {
                        cifsSize = SSHUtils.getManualProperty(mParentFragmentActivity,
                                mRouter,
                                mGlobalPreferences,
                                "/bin/df -T | grep \"cifs\"");
                    }

                    updateProgressBarViewSeparator(40);

                    if (nvramSize != null && nvramSize.length >= 1) {
                        final String nvramSizeStr = nvramSize[0];
                        if (nvramSizeStr != null && nvramSizeStr.startsWith("size:")) {
                            final List<String> stringList = SPACE_SPLITTER.splitToList(nvramSizeStr);
                            if (stringList.size() >= 5) {
                                final String nvramTotalBytes = stringList.get(1);
                                final String nvramLeftBytes = stringList.get(3).replace("(","");
                                try {
                                    final long nvramTotalBytesLong = Long.parseLong(nvramTotalBytes);
                                    final long nvramLeftBytesLong = Long.parseLong(nvramLeftBytes);
                                    final long nvramUsedBytesLong = nvramTotalBytesLong - nvramLeftBytesLong;
                                    nvramInfo.setProperty(NVRAMInfo.NVRAM_USED_PERCENT,
                                            Long.toString(
                                                    Math.min(100, 100 * nvramUsedBytesLong / nvramTotalBytesLong)
                                            ));
                                } catch (final NumberFormatException e) {
                                    e.printStackTrace();
                                    Crashlytics.logException(e);
                                }
                            }
                        }
                    }
                    updateProgressBarViewSeparator(50);

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
                                        e.printStackTrace();
                                        Crashlytics.logException(e);
                                    }
                                }
                            }
                            updateProgressBarViewSeparator(Math.min(70, 50 + 5 * i));
                        }
                        if (totalSize > 0) {
                            nvramInfo.setProperty(NVRAMInfo.STORAGE_JFFS2_USED_PERCENT,
                                    Long.toString(
                                            Math.min(100, 100 * totalUsed / totalSize)
                                    ));
                        }
                    }
                    updateProgressBarViewSeparator(75);

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
                                        e.printStackTrace();
                                        Crashlytics.logException(e);
                                    }
                                }
                            }
                            updateProgressBarViewSeparator(Math.min(87, 75 + 5 * i));
                        }
                        if (totalSize > 0) {
                            nvramInfo.setProperty(NVRAMInfo.STORAGE_CIFS_USED_PERCENT,
                                    Long.toString(
                                            Math.min(100, 100 * totalUsed / totalSize)
                                    ));
                        }
                    }
                    updateProgressBarViewSeparator(90);

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
        return null;
    }

    @Override
    public void onLoadFinished(Loader<NVRAMInfo> loader, NVRAMInfo data) {
        try {
            //Set tiles
            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

            layout.findViewById(R.id.tile_dashboard_storage_loading_view)
                    .setVisibility(View.GONE);
            mNVRAMArcProgress.setVisibility(View.VISIBLE);
            mJFFS2ArcProgress.setVisibility(View.VISIBLE);
            mCIFSArcProgress.setVisibility(View.VISIBLE);

            if (data == null) {
                data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
            }

            Exception exception = data.getException();

            final TextView errorPlaceHolderView = (TextView) this.layout
                    .findViewById(R.id.tile_dashboard_storage_error);

            if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

                if (exception == null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }

                if (isThemeLight) {
                    //Text: blue
                    //Finished stroke color: white
                    //Unfinished stroke color: white
                    final int finishedStrokeColor = ContextCompat.getColor(mParentFragmentActivity,
                            R.color.arcprogress_unfinished);
                    final int unfinishedStrokeColor = ContextCompat.getColor(mParentFragmentActivity,
                            R.color.arcprogress_finished);
                    mNVRAMArcProgress.setFinishedStrokeColor(
                            finishedStrokeColor);
                    mNVRAMArcProgress.setUnfinishedStrokeColor(
                            unfinishedStrokeColor);
                    mJFFS2ArcProgress.setFinishedStrokeColor(
                            finishedStrokeColor);
                    mJFFS2ArcProgress.setUnfinishedStrokeColor(
                            unfinishedStrokeColor);
                    mCIFSArcProgress.setFinishedStrokeColor(
                            finishedStrokeColor);
                    mCIFSArcProgress.setUnfinishedStrokeColor(
                            unfinishedStrokeColor);
                } else {
                    //Text: white
                    //Finished stroke color: white
                    //Unfinished stroke color: blue
                    final int textColor = ContextCompat
                            .getColor(mParentFragmentActivity,
                                    R.color.white);
                    mNVRAMArcProgress.setTextColor(textColor);
                    mJFFS2ArcProgress.setTextColor(textColor);
                    mCIFSArcProgress.setTextColor(textColor);
                }

                try {
                    final String nvramUsedStr = data.getProperty(NVRAMInfo.NVRAM_USED_PERCENT);
                    final int nvramUsed = Integer.parseInt(nvramUsedStr);

                    //Update colors as per the usage
                    //TODO Make these thresholds user-configurable (and perhaps display notifications if needed - cf. g service task)
                    if (nvramUsed >= 95) {
                        //Red
                        mNVRAMArcProgress.setFinishedStrokeColor(
                                red);
                    } else if (nvramUsed >= 80) {
                        //Orange
                        mNVRAMArcProgress.setFinishedStrokeColor(
                                orange);
                    }
                    mNVRAMArcProgress.setProgress(nvramUsed);

                } catch (final NumberFormatException e) {
                    mNVRAMArcProgress.setVisibility(View.GONE);
                }

                try {
                    final String jffs2UsedStr = data.getProperty(NVRAMInfo.STORAGE_JFFS2_USED_PERCENT);
                    final int jffs2Used = Integer.parseInt(jffs2UsedStr);

                    //Update colors as per the usage
                    //TODO Make these thresholds user-configurable (and perhaps display notifications if needed - cf. g service task)
                    if (jffs2Used >= 95) {
                        //Red
                        mJFFS2ArcProgress.setFinishedStrokeColor(
                                red);
                    } else if (jffs2Used >= 80) {
                        //Orange
                        mJFFS2ArcProgress.setFinishedStrokeColor(
                                orange);
                    }
                    mJFFS2ArcProgress.setProgress(jffs2Used);

                } catch (final NumberFormatException e) {
                    mJFFS2ArcProgress.setVisibility(View.GONE);
                }

                try {
                    final String cifsUsedStr = data.getProperty(NVRAMInfo.STORAGE_CIFS_USED_PERCENT);
                    final int cifsUsed = Integer.parseInt(cifsUsedStr);

                    //Update colors as per the usage
                    //TODO Make these thresholds user-configurable (and perhaps display notifications if needed - cf. g service task)
                    if (cifsUsed >= 95) {
                        //Red
                        mCIFSArcProgress.setFinishedStrokeColor(
                                red);
                    } else if (cifsUsed >= 80) {
                        //Orange
                        mCIFSArcProgress.setFinishedStrokeColor(
                                orange);
                    }
                    mCIFSArcProgress.setProgress(cifsUsed);

                } catch (final NumberFormatException e) {
                    mCIFSArcProgress.setVisibility(View.GONE);
                }

                //Update last sync
                final RelativeTimeTextView lastSyncView = (RelativeTimeTextView)
                        layout.findViewById(R.id.tile_last_sync);
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
                updateProgressBarWithError();
            } else if (exception == null) {
                updateProgressBarWithSuccess();
            }

        } finally {
            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished(): done loading!");
            mRefreshing.set(false);
            doneWithLoaderInstance(this, loader);
        }
    }
}