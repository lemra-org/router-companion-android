package org.rm3l.ddwrt.tiles.overview;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.status.wan.WANMonthlyTrafficActivity;
import org.rm3l.ddwrt.tiles.status.wan.WANMonthlyTrafficTile;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import de.keyboardsurfer.android.widget.crouton.Style;

import static org.rm3l.ddwrt.tiles.status.wan.WANMonthlyTrafficTile.DAILY_TRAFF_DATA_SPLITTER;
import static org.rm3l.ddwrt.tiles.status.wan.WANMonthlyTrafficTile.MONTHLY_TRAFF_DATA_SPLITTER;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.MB;
import static org.rm3l.ddwrt.utils.Utils.isDemoRouter;


public class WANTotalTrafficOverviewTile extends DDWRTTile<NVRAMInfo> implements PopupMenu.OnMenuItemClickListener {

    private static final String LOG_TAG = WANTotalTrafficOverviewTile.class.getSimpleName();
    public static final String CURRENT = "CURRENT";
    public static final String TOTAL_DL_CURRENT_MONTH = "TOTAL_DL_CURRENT_MONTH";
    public static final String TOTAL_UL_CURRENT_MONTH = "TOTAL_UL_CURRENT_MONTH";
    public static final String TOTAL_DL_CURRENT_MONTH_MB = "TOTAL_DL_CURRENT_MONTH_MB";
    public static final String TOTAL_UL_CURRENT_MONTH_MB = "TOTAL_UL_CURRENT_MONTH_MB";
    public static final String TOTAL_DL_CURRENT_DAY = "TOTAL_DL_CURRENT_DAY";
    public static final String TOTAL_UL_CURRENT_DAY = "TOTAL_UL_CURRENT_DAY";
    public static final String TOTAL_DL_CURRENT_DAY_MB = "TOTAL_DL_CURRENT_DAY_MB";
    public static final String TOTAL_UL_CURRENT_DAY_MB = "TOTAL_UL_CURRENT_DAY_MB";
    public static final String HIDDEN_ = "_HIDDEN_";
    public static final String CYCLE_MONTH = "M";
    public static final String CYCLE_DAY = "d";
    public static final String CYCLE = "cycle";
    public static final String WAN_TOTAL_TRAFFIC = "WAN Traffic";

    private boolean isThemeLight;
    private long mLastSync;
    private String mCurrentMonth;
    private String mCurrentMonthDisplayed;

    private final Map<Integer, ArrayList<Double>> mCurrentTraffMonthlyData = new ConcurrentHashMap<>();
    private int mCurrentDay;
    private String mCurrentDayDisplayed;
    private String mCycle;

    private NVRAMInfo mNvramInfo;

    public WANTotalTrafficOverviewTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_overview_wan_total_traffic,
                R.id.tile_overview_wan_total_traffic_togglebutton);
        isThemeLight = ColorUtils.isThemeLight(mParentFragmentActivity);
        //Create Options Menu
        final ImageButton tileMenu = (ImageButton) layout.findViewById(R.id.tile_overview_wan_total_traffic_menu);

        if (!ColorUtils.isThemeLight(mParentFragmentActivity)) {
            //Set menu background to white
            tileMenu.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
        }

        tileMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final PopupMenu popup = new PopupMenu(mParentFragmentActivity, v);
                popup.setOnMenuItemClickListener(WANTotalTrafficOverviewTile.this);
                final MenuInflater inflater = popup.getMenuInflater();
                final Menu menu = popup.getMenu();
                inflater.inflate(R.menu.tile_overview_wan_total_traffic_options, menu);
                final String cycle;
                if (mParentFragmentPreferences != null) {
                    cycle = mParentFragmentPreferences.getString(
                            getFormattedPrefKey(CYCLE), CYCLE_MONTH);
                } else {
                    cycle = CYCLE_MONTH;
                }
                //Month is the default
                final MenuItem dayMenuItem = menu.findItem(R.id.tile_overview_wan_total_traffic_options_selection_day);
                final MenuItem monthMenuItem = menu.findItem(R.id.tile_overview_wan_total_traffic_options_selection_month);
                switch (cycle) {
                    case CYCLE_DAY:
                        dayMenuItem
                                .setChecked(true);
                        monthMenuItem
                                .setChecked(false);
                        break;
                    default:
                        monthMenuItem
                                .setChecked(true);
                        dayMenuItem
                                .setChecked(false);
                        break;
                }

                popup.show();
            }
        });
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
                    mCycle = (mParentFragmentPreferences != null ?
                            mParentFragmentPreferences.getString(
                                getFormattedPrefKey(CYCLE), CYCLE_MONTH) : null);

                    Log.d(LOG_TAG, "Init background loader for " + WANTotalTrafficOverviewTile.class + ": routerInfo=" +
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

                    mCurrentTraffMonthlyData.clear();

                    mLastSync = System.currentTimeMillis();

                    final Date today = new Date();
                    mCurrentMonth = new SimpleDateFormat("MM-yyyy", Locale.US).format(today);
                    mCurrentMonthDisplayed = new SimpleDateFormat("MMM, yyyy", Locale.US).format(today);
                    mCurrentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
                    mCurrentDayDisplayed = new SimpleDateFormat("MMM dd, yyyy", Locale.US).format(today);

                    final NVRAMInfo nvramInfo = new NVRAMInfo();

                    NVRAMInfo nvramInfoTmp = null;
                    final String traffForCurrentMonthKey = \"fake-key\";

                    try {
                        nvramInfo.setProperty(CURRENT, mCurrentMonth);

                        if (isDemoRouter(mRouter)) {
                            final boolean ttraffEnabled = new Random().nextBoolean();
                            nvramInfo.setProperty(
                                    NVRAMInfo.TTRAFF_ENABLE,
                                    ttraffEnabled ? "1" : "0");
                        } else {
                            nvramInfoTmp = SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity,
                                    mRouter, mGlobalPreferences,
                                    NVRAMInfo.TTRAFF_ENABLE,
                                    traffForCurrentMonthKey);
                        }

                    } finally {
                        if (nvramInfoTmp != null) {
                            nvramInfo.putAll(nvramInfoTmp);
                        }
                    }

                    if (nvramInfo.isEmpty()) {
                        throw new DDWRTNoDataException("No Data!");
                    }

                    if (isDemoRouter(mRouter)) {
                        final long totalDlMonth = new Random().nextInt(200) * MB * MB;
                        nvramInfo.setProperty(TOTAL_DL_CURRENT_MONTH,
                                FileUtils
                                        .byteCountToDisplaySize(totalDlMonth));
                        nvramInfo.setProperty(TOTAL_DL_CURRENT_MONTH_MB,
                                HIDDEN_);
                        final long totalUlMonth = new Random().nextInt(100) * MB;
                        nvramInfo.setProperty(TOTAL_UL_CURRENT_MONTH,
                                FileUtils
                                        .byteCountToDisplaySize(totalUlMonth));
                        nvramInfo.setProperty(TOTAL_UL_CURRENT_MONTH_MB,
                                HIDDEN_);

                        nvramInfo.setProperty(TOTAL_DL_CURRENT_DAY,
                                FileUtils
                                        .byteCountToDisplaySize(totalDlMonth / 30));
                        nvramInfo.setProperty(TOTAL_DL_CURRENT_DAY_MB,
                                HIDDEN_);
                        nvramInfo.setProperty(TOTAL_UL_CURRENT_DAY,
                                FileUtils
                                        .byteCountToDisplaySize(totalUlMonth / 30));
                        nvramInfo.setProperty(TOTAL_UL_CURRENT_DAY_MB,
                                HIDDEN_);

                    } else {
                        //Compute total in/out
                        final String trafficForCurrentMonth = nvramInfo.getProperty(traffForCurrentMonthKey);
                        if (trafficForCurrentMonth != null) {
                            final List<String> dailyTraffDataList = MONTHLY_TRAFF_DATA_SPLITTER
                                    .splitToList(trafficForCurrentMonth);
                            if (!(dailyTraffDataList == null || dailyTraffDataList.isEmpty())) {
                                long totalDownloadMBytes = 0l;
                                long totalUploadMBytes = 0l;
                                int dayNum = 1;
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

                                    totalDownloadMBytes += Long.parseLong(inTraff);
                                    totalUploadMBytes += Long.parseLong(outTraff);

                                    mCurrentTraffMonthlyData.put(dayNum++, Lists.newArrayList(
                                            Double.parseDouble(inTraff), Double.parseDouble(outTraff)));
                                }
                                final String inHumanReadable = FileUtils
                                        .byteCountToDisplaySize(totalDownloadMBytes * MB);
                                nvramInfo.setProperty(TOTAL_DL_CURRENT_MONTH,
                                        inHumanReadable);
                                if (inHumanReadable.equals(totalDownloadMBytes + " MB") ||
                                        inHumanReadable.equals(totalDownloadMBytes + " bytes")) {
                                    nvramInfo.setProperty(TOTAL_DL_CURRENT_MONTH_MB,
                                            HIDDEN_);
                                } else {
                                    nvramInfo.setProperty(TOTAL_DL_CURRENT_MONTH_MB,
                                            String.valueOf(totalDownloadMBytes));
                                }

                                final String outHumanReadable = FileUtils
                                        .byteCountToDisplaySize(totalUploadMBytes * MB);
                                nvramInfo.setProperty(TOTAL_UL_CURRENT_MONTH,
                                        outHumanReadable);
                                if (outHumanReadable.equals(totalUploadMBytes + " MB") ||
                                        outHumanReadable.equals(totalUploadMBytes + " bytes")) {
                                    nvramInfo.setProperty(TOTAL_UL_CURRENT_MONTH_MB,
                                            HIDDEN_);
                                } else {
                                    nvramInfo.setProperty(TOTAL_UL_CURRENT_MONTH_MB,
                                            String.valueOf(totalUploadMBytes));
                                }

                                //Day
                                final ArrayList<Double> currentDayTraffData =
                                        mCurrentTraffMonthlyData.get(mCurrentDay);
                                double totalDownloadMBytesForCurrentDay = -1.;
                                double totalUploadMBytesForCurrentDay = -1.;
                                if (currentDayTraffData != null && currentDayTraffData.size() >= 2) {
                                    totalDownloadMBytesForCurrentDay =
                                            currentDayTraffData.get(0);
                                    totalUploadMBytesForCurrentDay =
                                            currentDayTraffData.get(1);
                                }

                                final String inHumanReadableCurrentDay = FileUtils
                                        .byteCountToDisplaySize(
                                                Double.valueOf(totalDownloadMBytesForCurrentDay * MB)
                                                    .longValue());
                                nvramInfo.setProperty(TOTAL_DL_CURRENT_DAY,
                                        inHumanReadableCurrentDay);
                                if (inHumanReadableCurrentDay.equals(totalDownloadMBytesForCurrentDay + " MB") ||
                                        inHumanReadableCurrentDay.equals(totalDownloadMBytesForCurrentDay + " bytes")) {
                                    nvramInfo.setProperty(TOTAL_DL_CURRENT_DAY_MB,
                                            HIDDEN_);
                                } else {
                                    nvramInfo.setProperty(TOTAL_DL_CURRENT_DAY_MB,
                                            String.valueOf(totalDownloadMBytesForCurrentDay));
                                }
                                final String outHumanReadableCurrentDay = FileUtils
                                        .byteCountToDisplaySize(
                                                Double.valueOf(totalUploadMBytesForCurrentDay * MB)
                                                    .longValue());
                                nvramInfo.setProperty(TOTAL_UL_CURRENT_DAY,
                                        outHumanReadableCurrentDay);
                                if (outHumanReadableCurrentDay.equals(totalUploadMBytesForCurrentDay + " MB") ||
                                        outHumanReadableCurrentDay.equals(totalUploadMBytesForCurrentDay + " bytes")) {
                                    nvramInfo.setProperty(TOTAL_UL_CURRENT_DAY_MB,
                                            HIDDEN_);
                                } else {
                                    nvramInfo.setProperty(TOTAL_UL_CURRENT_DAY_MB,
                                            String.valueOf(totalUploadMBytesForCurrentDay));
                                }
                            }
                        }
                    }

                    mNvramInfo = nvramInfo;

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
            final View gridLayoutContainer = layout.findViewById(R.id.tile_overview_wan_total_traffic_gridLayout);
            gridLayoutContainer
                    .setVisibility(View.VISIBLE);


            final boolean isDayCycle = CYCLE_DAY.equals(mCycle);

            ((TextView) layout.findViewById(R.id.tile_overview_wan_total_traffic_title))
                    .setText(WAN_TOTAL_TRAFFIC + ": " + 
                            (isDayCycle ? mCurrentDayDisplayed : mCurrentMonthDisplayed));

            final View menu = layout.findViewById(R.id.tile_overview_wan_total_traffic_menu);

            Exception preliminaryCheckException = null;
            if (data == null) {
                preliminaryCheckException = new DDWRTNoDataException("No Data!");
            } else //noinspection ThrowableResultOfMethodCallIgnored
                if (data.getException() == null) {
                    if (!"1".equals(data.getProperty(NVRAMInfo.TTRAFF_ENABLE))) {
                        preliminaryCheckException = new WANMonthlyTrafficTile.DDWRTTraffDataDisabled("Traffic monitoring disabled!");
                        menu.setVisibility(View.GONE);
                    } else {
                        menu.setVisibility(View.VISIBLE);
                    }
                }

            if (preliminaryCheckException != null) {
                data = new NVRAMInfo().setException(preliminaryCheckException);
            }

            final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_overview_wan_total_traffic_error);

            final Exception exception = data.getException();

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
                wanDLView.setText(data.getProperty(
                        isDayCycle ? TOTAL_DL_CURRENT_DAY : TOTAL_DL_CURRENT_MONTH , "-"));

                final TextView wanULView = (TextView) this.layout.findViewById(R.id.tile_overview_wan_total_traffic_ul);
                wanULView.setCompoundDrawablesWithIntrinsicBounds(ulDrawable, 0, 0, 0);
                wanULView.setText(data.getProperty(
                        isDayCycle ? TOTAL_UL_CURRENT_DAY : TOTAL_UL_CURRENT_MONTH, "-"));

                final TextView dlMB = (TextView) this.layout.findViewById(R.id.tile_overview_wan_total_traffic_dl_mb);
                final String dlMBytesFromNvram = data.getProperty(
                        isDayCycle ? TOTAL_DL_CURRENT_DAY_MB : TOTAL_DL_CURRENT_MONTH_MB);
                if (HIDDEN_.equals(dlMBytesFromNvram)) {
                    dlMB.setVisibility(View.GONE);
                } else {
                    dlMB.setVisibility(View.VISIBLE);
                }
                dlMB.setText(dlMBytesFromNvram != null ?
                        ("(" + dlMBytesFromNvram + " MB)") : "-");

                final TextView ulMB = (TextView) this.layout.findViewById(R.id.tile_overview_wan_total_traffic_ul_mb);
                final String ulMBytesFromNvram = data.getProperty(
                        isDayCycle ? TOTAL_UL_CURRENT_DAY_MB : TOTAL_UL_CURRENT_MONTH_MB);
                if (HIDDEN_.equals(ulMBytesFromNvram)) {
                    ulMB.setVisibility(View.GONE);
                } else {
                    ulMB.setVisibility(View.VISIBLE);
                }
                ulMB.setText(ulMBytesFromNvram != null ?
                        ("(" + ulMBytesFromNvram + " MB)") : "-");

                //Update last sync
                final RelativeTimeTextView lastSyncView = (RelativeTimeTextView) layout.findViewById(R.id.tile_last_sync);
                lastSyncView.setReferenceTime(mLastSync);
                lastSyncView.setPrefix("Last sync: ");

                final NVRAMInfo dataCopy = data;
                gridLayoutContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String currentMonth = dataCopy.getProperty(CURRENT);
                        if (currentMonth == null) {
                            Utils.displayMessage(mParentFragmentActivity, "Internal Error. Please try again later.", Style.ALERT);
                            Utils.reportException(null, new IllegalStateException("currentMonth == null"));
                            return;
                        }
                        if (mCurrentTraffMonthlyData == null || mCurrentTraffMonthlyData.isEmpty()) {
                            Toast.makeText(WANTotalTrafficOverviewTile.this.mParentFragmentActivity,
                                    String.format("No traffic data for '%s'", currentMonth),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            final Intent intent = new Intent(mParentFragmentActivity, WANMonthlyTrafficActivity.class);
                            intent.putExtra(RouterManagementActivity.ROUTER_SELECTED,
                                    mRouter != null ? mRouter.getRemoteIpAddress() : DDWRTCompanionConstants.EMPTY_STRING);
                            intent.putExtra(WANMonthlyTrafficActivity.MONTH_DISPLAYED, currentMonth);
                            intent.putExtra(WANMonthlyTrafficActivity.MONTHLY_TRAFFIC_DATA_UNSORTED,
                                    ImmutableMap.copyOf(mCurrentTraffMonthlyData));

                            //noinspection ConstantConditions
//                            final AlertDialog alertDialog = Utils.buildAlertDialog(mParentFragmentActivity, null,
//                                    String.format("Loading traffic data for '%s'", currentMonth), false, false);
//                            alertDialog.show();
//                            ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);

                            final ProgressDialog alertDialog = ProgressDialog.show(mParentFragmentActivity,
                                    String.format("Loading traffic data for '%s'", currentMonth), "Please Wait...",
                                    true);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mParentFragmentActivity.startActivity(intent);
                                    alertDialog.cancel();
                                }
                            }, 1000);
                        }
                    }
                });
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
                gridLayoutContainer.setOnClickListener(null);
            }

        }  finally {
            Log.d(LOG_TAG, "onLoadFinished(): done loading!");
            mRefreshing.set(false);
            doneWithLoaderInstance(this, loader,
                    R.id.tile_overview_wan_total_traffic_togglebutton_title,
                    R.id.tile_overview_wan_total_traffic_togglebutton_separator);
        }
    }

    @Override
    public boolean onMenuItemClick(final MenuItem menuItem) {
        final int itemId = menuItem.getItemId();
        String cycle = null;
        boolean knownMenuItem = false;
        switch (itemId) {
            case R.id.tile_overview_wan_total_traffic_options_selection_month:
                if (!menuItem.isChecked()) {
                    menuItem.setChecked(true);
                    cycle = CYCLE_MONTH;
                } else {
                    menuItem.setChecked(false);
                }
                knownMenuItem = true;
                break;
            case R.id.tile_overview_wan_total_traffic_options_selection_day:
                if (!menuItem.isChecked()) {
                    menuItem.setChecked(true);
                    cycle = CYCLE_DAY;
                } else {
                    menuItem.setChecked(false);
                }
                knownMenuItem = true;
                break;
            default:
                break;
        }

        if (cycle != null) {
            if (mParentFragmentPreferences != null) {
                mParentFragmentPreferences.edit()
                        .putString(getFormattedPrefKey(CYCLE), cycle)
                        .apply();
            }

            if (mNvramInfo != null) {
                final boolean isDayCycle = CYCLE_DAY.equals(cycle);

                //Update title
                ((TextView) layout.findViewById(R.id.tile_overview_wan_total_traffic_title))
                        .setText(WAN_TOTAL_TRAFFIC + ": " + 
                                (isDayCycle ? mCurrentDayDisplayed : mCurrentMonthDisplayed));

                final TextView wanDLView = (TextView) this.layout.findViewById(R.id.tile_overview_wan_total_traffic_dl);
                wanDLView.setText(mNvramInfo.getProperty(
                        isDayCycle ? TOTAL_DL_CURRENT_DAY : TOTAL_DL_CURRENT_MONTH, "-"));

                final TextView wanULView = (TextView) this.layout.findViewById(R.id.tile_overview_wan_total_traffic_ul);
                wanULView.setText(mNvramInfo.getProperty(
                        isDayCycle ? TOTAL_UL_CURRENT_DAY : TOTAL_UL_CURRENT_MONTH, "-"));

                final TextView dlMB = (TextView) this.layout.findViewById(R.id.tile_overview_wan_total_traffic_dl_mb);
                final String dlMBytesFromNvram = mNvramInfo.getProperty(
                        isDayCycle ? TOTAL_DL_CURRENT_DAY_MB : TOTAL_DL_CURRENT_MONTH_MB);
                if (HIDDEN_.equals(dlMBytesFromNvram)) {
                    dlMB.setVisibility(View.INVISIBLE);
                } else {
                    dlMB.setVisibility(View.VISIBLE);
                }
                dlMB.setText(dlMBytesFromNvram != null ?
                        ("(" + dlMBytesFromNvram + " MB)") : "-");

                final TextView ulMB = (TextView) this.layout.findViewById(R.id.tile_overview_wan_total_traffic_ul_mb);
                final String ulMBytesFromNvram = mNvramInfo.getProperty(
                        isDayCycle ? TOTAL_UL_CURRENT_DAY_MB : TOTAL_UL_CURRENT_MONTH_MB);
                if (HIDDEN_.equals(ulMBytesFromNvram)) {
                    ulMB.setVisibility(View.INVISIBLE);
                } else {
                    ulMB.setVisibility(View.VISIBLE);
                }
                ulMB.setText(ulMBytesFromNvram != null ?
                        ("(" + ulMBytesFromNvram + " MB)") : "-");
            }

        }

        return knownMenuItem;
    }
}