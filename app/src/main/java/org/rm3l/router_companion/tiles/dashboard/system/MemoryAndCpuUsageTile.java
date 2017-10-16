package org.rm3l.router_companion.tiles.dashboard.system;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.router_companion.mgmt.RouterManagementActivity.ROUTER_SELECTED;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.ColorRes;
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
import com.google.common.base.Throwables;
import java.util.List;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.firmwares.RemoteDataRetrievalListener;
import org.rm3l.router_companion.main.DDWRTMainActivity;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.tiles.status.router.StatusRouterMemoryTile;
import org.rm3l.router_companion.utils.ColorUtils;

/**
 * Created by rm3l on 03/01/16.
 */
public class MemoryAndCpuUsageTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = MemoryAndCpuUsageTile.class.getSimpleName();

    private boolean isThemeLight;

    private final ArcProgress mCpuArcProgress;

    private long mLastSync;

    private final ArcProgress mMemArcProgress;

    public MemoryAndCpuUsageTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments,
            @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_dashboard_mem_cpu, null);
        isThemeLight = ColorUtils.Companion.isThemeLight(mParentFragmentActivity);
        this.mMemArcProgress =
                (ArcProgress) layout.findViewById(R.id.tile_dashboard_mem_cpu_mem_arcprogress);
        this.mCpuArcProgress =
                (ArcProgress) layout.findViewById(R.id.tile_dashboard_mem_cpu_cpu_arcprogress);
        final View.OnClickListener clickListener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //Open Router State tab
                if (mParentFragmentActivity instanceof DDWRTMainActivity) {
                    ((DDWRTMainActivity) mParentFragmentActivity).selectItemInDrawer(2);
                } else {
                    //TODO Set proper flags ???
                    final Intent intent = new Intent(mParentFragmentActivity, DDWRTMainActivity.class);
                    intent.putExtra(ROUTER_SELECTED, mRouter.getUuid());
                    intent.putExtra(DDWRTMainActivity.SAVE_ITEM_SELECTED, 2);
                    mParentFragmentActivity.startActivity(intent);
                }
            }
        };
        this.mMemArcProgress.setOnClickListener(clickListener);
        this.mCpuArcProgress.setOnClickListener(clickListener);
    }

    @Override
    public int getTileHeaderViewId() {
        return -1;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_dashboard_mem_cpu_title;
    }

    @Override
    public void onLoadFinished(Loader<NVRAMInfo> loader, NVRAMInfo data) {
        try {
            //Set tiles
            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

            layout.findViewById(R.id.tile_dashboard_mem_cpu_loading_view).setVisibility(View.GONE);
            mMemArcProgress.setVisibility(View.VISIBLE);
            mCpuArcProgress.setVisibility(View.VISIBLE);

            if (data == null) {
                data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
            }

            Exception exception = data.getException();

            final TextView errorPlaceHolderView =
                    this.layout.findViewById(R.id.tile_dashboard_mem_cpu_error);

            if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

                if (exception == null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }

                Integer arcProgressFinishedColor = null;
                if (mRouter != null) {
                    final Integer primaryColor = ColorUtils.Companion.getPrimaryColor(mRouter.getRouterFirmware());
                    if (primaryColor != null) {
                        arcProgressFinishedColor = ContextCompat.getColor(mParentFragmentActivity, primaryColor);
                    }
                }
                if (arcProgressFinishedColor != null) {
                    mMemArcProgress.setFinishedStrokeColor(arcProgressFinishedColor);
                    mCpuArcProgress.setFinishedStrokeColor(arcProgressFinishedColor);
                }
                final int textColor = ContextCompat.getColor(mParentFragmentActivity,
                        isThemeLight ? R.color.black : R.color.white);
                mMemArcProgress.setTextColor(textColor);
                mCpuArcProgress.setTextColor(textColor);

                //Red
                final int red = ContextCompat.getColor(mParentFragmentActivity, R.color.win8_red);
                //Orange
                final int orange = ContextCompat.getColor(mParentFragmentActivity, R.color.win8_orange);

                try {
                    final String memUsedStr = data.getProperty(NVRAMInfo.Companion.getMEMORY_USED_PERCENT());
                    final int memUsed = Integer.parseInt(memUsedStr);

                    //Update colors as per the usage
                    //TODO Make these thresholds user-configurable (and perhaps display notifications if needed - cf. g service task)
                    if (memUsed >= 95) {
                        //Red
                        mMemArcProgress.setFinishedStrokeColor(red);
                    } else if (memUsed >= 80) {
                        //Orange
                        mMemArcProgress.setFinishedStrokeColor(orange);
                    }
                    mMemArcProgress.setProgress(memUsed);
                } catch (final NumberFormatException e) {
                    mMemArcProgress.setVisibility(View.GONE);
                }

                try {
                    final String cpuUsedStr = data.getProperty(NVRAMInfo.Companion.getCPU_USED_PERCENT());
                    final int cpuUsed = Integer.parseInt(cpuUsedStr);

                    //Update colors as per the usage
                    //TODO Make these thresholds user-configurable (and perhaps display notifications if needed - cf. g service task)
                    if (cpuUsed >= 95) {
                        //Red
                        mCpuArcProgress.setFinishedStrokeColor(red);
                    } else if (cpuUsed >= 80) {
                        //Orange
                        mCpuArcProgress.setFinishedStrokeColor(orange);
                    }
                    mCpuArcProgress.setProgress(cpuUsed);
                } catch (final NumberFormatException e) {
                    mCpuArcProgress.setVisibility(View.GONE);
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
        } finally {
            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished(): done loading!");
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
                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Init background loader for "
                            + StatusRouterMemoryTile.class
                            + ": routerInfo="
                            + mRouter
                            + " / nbRunsLoader="
                            + nbRunsLoader);

                    isThemeLight = ColorUtils.Companion.isThemeLight(mParentFragmentActivity);

                    if (mRefreshing.getAndSet(true)) {
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    updateProgressBarViewSeparator(0);

                    mLastSync = System.currentTimeMillis();

                    final NVRAMInfo nvramInfo = new NVRAMInfo();

                    updateProgressBarViewSeparator(10);

                    final List<String[]> dataForMemoryAndCpuUsageTile =
                            mRouterConnector.getDataForMemoryAndCpuUsageTile(mParentFragmentActivity, mRouter,
                                    new RemoteDataRetrievalListener() {
                                        @Override
                                        public void doRegardlessOfStatus() {

                                        }

                                        @Override
                                        public void onProgressUpdate(int progress) {
                                            updateProgressBarViewSeparator(progress);
                                        }
                                    });

                    if (dataForMemoryAndCpuUsageTile == null || dataForMemoryAndCpuUsageTile.isEmpty()) {
                        throw new DDWRTNoDataException();
                    }

                    final String[] memData = dataForMemoryAndCpuUsageTile.get(0);
                    if (memData != null && memData.length >= 2) {
                        //Total
                        String memTotal = null;
                        List<String> strings = Splitter.on("MemTotal:")
                                .omitEmptyStrings()
                                .trimResults()
                                .splitToList(memData[0].trim());
                        if (strings != null && strings.size() >= 1) {
                            memTotal = strings.get(0);
                            nvramInfo.setProperty(NVRAMInfo.Companion.getMEMORY_TOTAL(), memTotal);
                        }

                        updateProgressBarViewSeparator(35);

                        //Free
                        String memFree = null;
                        strings = Splitter.on("MemFree:")
                                .omitEmptyStrings()
                                .trimResults()
                                .splitToList(memData[1].trim());
                        if (strings != null && strings.size() >= 1) {
                            memFree = strings.get(0);
                            nvramInfo.setProperty(NVRAMInfo.Companion.getMEMORY_FREE(), strings.get(0));
                        }

                        updateProgressBarViewSeparator(40);

                        //Mem used
                        String memUsed = null;
                        if (!(isNullOrEmpty(memTotal) || isNullOrEmpty(memFree))) {
                            //noinspection ConstantConditions
                            final long memTotalLong = Long.parseLong(memTotal.replaceAll(" kB", "").trim());
                            final long memFreeLong = Long.parseLong(memFree.replaceAll(" kB", "").trim());
                            final long memUsedLong = memTotalLong - memFreeLong;
                            memUsed = (Long.toString(memUsedLong) + " kB");

                            nvramInfo.setProperty(NVRAMInfo.Companion.getMEMORY_USED(), memUsed);
                            if (memTotalLong > 0L) {
                                nvramInfo.setProperty(NVRAMInfo.Companion.getMEMORY_USED_PERCENT(),
                                        Long.toString(Math.min(100, 100 * memUsedLong / memTotalLong)));
                            }
                        }
                    }
                    updateProgressBarViewSeparator(50);

                    if (dataForMemoryAndCpuUsageTile.size() >= 2) {
                        final String[] cpuUsageData = dataForMemoryAndCpuUsageTile.get(1);

                        if (cpuUsageData != null && cpuUsageData.length >= 2) {
                            final String loadAvg = cpuUsageData[0];
                            if (loadAvg != null) {
                                final List<String> stringList =
                                        Splitter.on(",").omitEmptyStrings().trimResults().splitToList(loadAvg);
                                if (stringList.size() >= 3) {
                                    try {
                                        final float loadAvgTotal = Float.parseFloat(stringList.get(0))
                                                + Float.parseFloat(stringList.get(1))
                                                + Float.parseFloat(stringList.get(2));
                                        final int coresCount = Integer.parseInt(cpuUsageData[1]);

                                        if (coresCount > 0) {
                                            nvramInfo.setProperty(NVRAMInfo.Companion.getCPU_USED_PERCENT(),
                                                    Integer.toString(
                                                            Math.min(100,
                                                                    Double.valueOf(loadAvgTotal / coresCount * 33.3)
                                                                            .intValue())));
                                        }
                                    } catch (final NumberFormatException e) {
                                        e.printStackTrace();
                                        Crashlytics.logException(e);
                                    }
                                }
                            }
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
}