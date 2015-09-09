package org.rm3l.ddwrt.tiles.overview;

import android.content.Context;
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

import com.google.common.base.Strings;
import com.google.common.base.Throwables;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.status.wan.WANMonthlyTrafficTile;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.SSHUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.rm3l.ddwrt.tiles.status.wan.WANMonthlyTrafficTile.DAILY_TRAFF_DATA_SPLITTER;
import static org.rm3l.ddwrt.tiles.status.wan.WANMonthlyTrafficTile.MONTHLY_TRAFF_DATA_SPLITTER;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.MB;

/**
 * Created by rm3l on 08/09/15.
 */
public class WANTotalTrafficOverviewTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = WANTotalTrafficOverviewTile.class.getSimpleName();
    public static final String CURRENT_MONTH = "CURRENT_MONTH";
    public static final String TOTAL_DL_CURRENT_MONTH = "TOTAL_DL_CURRENT_MONTH";
    public static final String TOTAL_UL_CURRENT_MONTH = "TOTAL_UL_CURRENT_MONTH";
    private boolean isThemeLight;

    private String mCurrentMonth;

    public WANTotalTrafficOverviewTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_overview_wan_total_traffic,
                R.id.tile_overview_wan_total_traffic_togglebutton);
        isThemeLight = ColorUtils.isThemeLight(mParentFragmentActivity);
    }

    @Override
    public int getTileHeaderViewId() {
        return 0;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_overview_wan_total_traffic_title;
    }

    @Nullable
    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {

                try {

                    isThemeLight = ColorUtils.isThemeLight(mParentFragmentActivity);

                    Log.d(LOG_TAG, "Init background loader for " + WANTotalTrafficOverviewTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                        //Skip run
                        Log.d(LOG_TAG, "Skip loader run");
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    mCurrentMonth = new SimpleDateFormat("MM-yyyy", Locale.US).format(new Date());
                    final NVRAMInfo nvramInfo = new NVRAMInfo();

                    NVRAMInfo nvramInfoTmp = null;
                    final String traffForCurrentMonthKey = \"fake-key\";

                    try {
                        nvramInfo.setProperty(CURRENT_MONTH, mCurrentMonth);

                        nvramInfoTmp = SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity,
                                mRouter, mGlobalPreferences,
                                NVRAMInfo.TTRAFF_ENABLE,
                                traffForCurrentMonthKey);

                    } finally {
                        if (nvramInfoTmp != null) {
                            nvramInfo.putAll(nvramInfoTmp);
                        }
                    }

                    if (nvramInfo.isEmpty()) {
                        throw new DDWRTNoDataException("No Data!");
                    }

                    //Compute total in/out
                    final String trafficForCurrentMonth = nvramInfo.getProperty(traffForCurrentMonthKey);
                    if (trafficForCurrentMonth != null) {
                        final List<String> dailyTraffDataList = MONTHLY_TRAFF_DATA_SPLITTER
                                .splitToList(trafficForCurrentMonth);
                        if (!(dailyTraffDataList == null || dailyTraffDataList.isEmpty())) {
                            long totalDownloadBytes = 0l;
                            long totalUploadBytes = 0l;
                            for (final String dailyInOutTraffData : dailyTraffDataList) {
                                if (Strings.isNullOrEmpty(dailyInOutTraffData)) {
                                    continue;
                                }
                                if (StringUtils.contains(dailyInOutTraffData, "[")) {
                                    continue;
                                }
                                final List<String> dailyInOutTraffDataList = DAILY_TRAFF_DATA_SPLITTER
                                        .splitToList(dailyInOutTraffData);
                                if (dailyInOutTraffDataList.size() < 2) {
                                    continue;
                                }
                                final String inTraff = dailyInOutTraffDataList.get(0);
                                final String outTraff = dailyInOutTraffDataList.get(1);

                                totalDownloadBytes += Long.parseLong(inTraff);
                                totalUploadBytes += Long.parseLong(outTraff);
                            }
                            nvramInfo.setProperty(TOTAL_DL_CURRENT_MONTH,
                                    FileUtils.byteCountToDisplaySize(totalDownloadBytes * MB));
                            nvramInfo.setProperty(TOTAL_UL_CURRENT_MONTH,
                                    FileUtils.byteCountToDisplaySize(totalUploadBytes * MB));
                        }
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
            Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

            layout.findViewById(R.id.tile_overview_wan_total_traffic_loading_view)
                    .setVisibility(View.GONE);
            layout.findViewById(R.id.tile_overview_wan_total_traffic_gridLayout)
                    .setVisibility(View.VISIBLE);

            ((TextView) layout.findViewById(R.id.tile_overview_wan_total_traffic_title))
                    .setText("WAN Total Traffic" +
                            (Strings.isNullOrEmpty(mCurrentMonth) ?
                                    "" : (": " + mCurrentMonth)));

            Exception preliminaryCheckException = null;
            if (data == null) {
                preliminaryCheckException = new DDWRTNoDataException("No Data!");
            } else //noinspection ThrowableResultOfMethodCallIgnored
                if (data.getException() == null) {
                    if (!"1".equals(data.getProperty(NVRAMInfo.TTRAFF_ENABLE))) {
                        preliminaryCheckException = new WANMonthlyTrafficTile.DDWRTTraffDataDisabled("Traffic monitoring disabled!");
                    }
                }

            if (preliminaryCheckException != null) {
                data = new NVRAMInfo().setException(preliminaryCheckException);
            }

            final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_overview_wan_total_traffic_error);

            final Exception exception = data.getException();

//            final RelativeLayout mapContainerView =
//                    (RelativeLayout) layout.findViewById(R.id.tile_overview_wan_total_traffic_container);

            if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

                if (exception == null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }

                final int dlDrawable;
                final int ulDrawable;
                if (isThemeLight) {
                    dlDrawable = R.drawable.ic_dl_dark;
                    ulDrawable = R.drawable.ic_ul_dark;
                } else {
                    dlDrawable = R.drawable.ic_dl_white;
                    ulDrawable = R.drawable.ic_ul_light;
                }

                final TextView wanDLView = (TextView) this.layout.findViewById(R.id.tile_overview_wan_total_traffic_dl);
                wanDLView.setCompoundDrawablesWithIntrinsicBounds(dlDrawable, 0, 0, 0);
                wanDLView.setText(data.getProperty(TOTAL_DL_CURRENT_MONTH, "-"));

                final TextView wanULView = (TextView) this.layout.findViewById(R.id.tile_overview_wan_total_traffic_ul);
                wanULView.setCompoundDrawablesWithIntrinsicBounds(ulDrawable, 0, 0, 0);
                wanULView.setText(data.getProperty(TOTAL_UL_CURRENT_MONTH, "-"));

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
//                //Hide NTM
//                mapContainerView.setVisibility(View.GONE);
            }
//            else {
//                mapContainerView.setVisibility(View.VISIBLE);
//            }

        }  finally {
            Log.d(LOG_TAG, "onLoadFinished(): done loading!");
            doneWithLoaderInstance(this, loader,
                    R.id.tile_overview_wan_total_traffic_togglebutton_title,
                    R.id.tile_overview_wan_total_traffic_togglebutton_separator);
        }
    }
}
