package org.rm3l.ddwrt.tiles.overview;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.resources.WANTrafficData;
import org.rm3l.ddwrt.resources.WANTrafficData.CycleItem;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.status.wan.WANMonthlyTrafficTile;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.ReportingUtils;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.WANTrafficUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.MB;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.WAN_CYCLE_DAY_PREF;
import static org.rm3l.ddwrt.utils.Utils.isDemoRouter;
import static org.rm3l.ddwrt.utils.WANTrafficUtils.DAILY_TRAFF_DATA_SPLITTER;
import static org.rm3l.ddwrt.utils.WANTrafficUtils.HIDDEN_;
import static org.rm3l.ddwrt.utils.WANTrafficUtils.MONTHLY_TRAFF_DATA_SPLITTER;
import static org.rm3l.ddwrt.utils.WANTrafficUtils.TOTAL_DL_CURRENT_MONTH;
import static org.rm3l.ddwrt.utils.WANTrafficUtils.TOTAL_DL_CURRENT_MONTH_MB;
import static org.rm3l.ddwrt.utils.WANTrafficUtils.TOTAL_UL_CURRENT_MONTH;
import static org.rm3l.ddwrt.utils.WANTrafficUtils.TOTAL_UL_CURRENT_MONTH_MB;
import static org.rm3l.ddwrt.utils.WANTrafficUtils.retrieveAndPersistMonthlyTrafficData;


public class WANTotalTrafficOverviewTile extends DDWRTTile<NVRAMInfo> implements PopupMenu.OnMenuItemClickListener {

    private static final String LOG_TAG = WANTotalTrafficOverviewTile.class.getSimpleName();
    public static final String PREVIOUS = "_PREVIOUS";
    public static final String CURRENT = "_CURRENT";
    public static final String NEXT = "_NEXT";
    public static final String TOTAL_DL_CURRENT_DAY = "TOTAL_DL_CURRENT_DAY";
    public static final String TOTAL_UL_CURRENT_DAY = "TOTAL_UL_CURRENT_DAY";
    public static final String TOTAL_DL_CURRENT_DAY_MB = "TOTAL_DL_CURRENT_DAY_MB";
    public static final String TOTAL_UL_CURRENT_DAY_MB = "TOTAL_UL_CURRENT_DAY_MB";
    public static final String CYCLE_MONTH = "M";
    public static final String CYCLE_DAY = "d";
    public static final String CYCLE = "cycle";
    public static final String WAN_TOTAL_TRAFFIC = "WAN Usage";
    public static final String TRAFF_PREFIX = "traff-";
    public static final SimpleDateFormat DDWRT_TRAFF_DATA_SIMPLE_DATE_FORMAT = new SimpleDateFormat("MM-yyyy", Locale.US);

    private boolean isThemeLight;
    private long mLastSync;
    private String mCurrentMonth;
    private String mCurrentMonthDisplayed;

    private int mCurrentDay;
    private String mCurrentDayDisplayed;

    private String mCycle;

    private NVRAMInfo mNvramInfo;

    private CycleItem mCycleItem;

    private final DDWRTCompanionDAO dao;
    private String mPrevMonth;
    private String mNextMonth;

    public WANTotalTrafficOverviewTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_overview_wan_total_traffic, null);

        dao = RouterManagementActivity.getDao(mParentFragmentActivity);
        isThemeLight = ColorUtils.isThemeLight(mParentFragmentActivity);
        //Create Options Menu
        final ImageButton tileMenu = (ImageButton) layout.findViewById(R.id.tile_overview_wan_total_traffic_menu);

        if (!ColorUtils.isThemeLight(mParentFragmentActivity)) {
            //Set menu background to white
            tileMenu.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
        }

        mCycleItem = WANTrafficData
                .getCurrentWANCycle(mParentFragmentActivity, mParentFragmentPreferences);

        mCycle = (mParentFragmentPreferences != null ?
                mParentFragmentPreferences.getString(
                        CYCLE, CYCLE_MONTH) : null);

        final boolean isDayCycle = CYCLE_DAY.equals(mCycle);
        final Date today = new Date();
        mCurrentMonth = DDWRT_TRAFF_DATA_SIMPLE_DATE_FORMAT.format(today);
        mCurrentMonthDisplayed = new SimpleDateFormat("MMM, yyyy", Locale.US).format(today);
        mCurrentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        mCurrentDayDisplayed = new SimpleDateFormat("MMM dd, yyyy", Locale.US).format(today);

        ((TextView) layout.findViewById(R.id.tile_overview_wan_total_traffic_title))
                .setText(WAN_TOTAL_TRAFFIC + ": " +
                        (isDayCycle ? mCurrentDayDisplayed :
                                (mCycleItem != null ? mCycleItem.getLabel() : mCurrentMonthDisplayed)));

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
                            CYCLE, CYCLE_MONTH);
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

                final Date today = new Date();
                final Calendar calendar = Calendar.getInstance();
                mCurrentDay = calendar.get(Calendar.DAY_OF_MONTH);
                mCurrentDayDisplayed = new SimpleDateFormat("MMM dd, yyyy",
                        Locale.US).format(today);
                dayMenuItem.setTitle(String.format("Today (%s)", mCurrentDayDisplayed));

                try {
                    mCycleItem = WANTrafficData
                            .getCurrentWANCycle(mParentFragmentActivity, mParentFragmentPreferences);
                    //Overwrite with effective period (for monthly)
                    monthMenuItem.setTitle(String.format("Month (%s)",
                            mCycleItem.getLabel()));

                } catch (final Exception e) {
                    e.printStackTrace();
                    ReportingUtils.reportException(mParentFragmentActivity, e);
                    menu.findItem(R.id.tile_overview_wan_total_traffic_options_change_cycle)
                        .setVisible(false);
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
                                CYCLE, CYCLE_MONTH) : null);

                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Init background loader for " + WANTotalTrafficOverviewTile.class + ": routerInfo=" +
                            mRouter + " / nbRunsLoader=" + nbRunsLoader);

                    if (mRefreshing.getAndSet(true)) {
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    mLastSync = System.currentTimeMillis();

                    mCycleItem = WANTrafficData
                            .getCurrentWANCycle(mParentFragmentActivity, mParentFragmentPreferences);

                    final Date today = new Date();
                    mCurrentMonth = DDWRT_TRAFF_DATA_SIMPLE_DATE_FORMAT.format(today);
                    mCurrentMonthDisplayed = new SimpleDateFormat("MMM, yyyy", Locale.US).format(today);
                    mCurrentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
                    mCurrentDayDisplayed = new SimpleDateFormat("MMM dd, yyyy", Locale.US).format(today);

                    //Also retrieve data for previous month and next month
                    final Calendar cal1 = Calendar.getInstance();
                    cal1.add(Calendar.MONTH, -1);
                    mPrevMonth = DDWRT_TRAFF_DATA_SIMPLE_DATE_FORMAT.format(cal1.getTime());

                    final Calendar cal2 = Calendar.getInstance();
                    cal2.add(Calendar.MONTH, 1);
                    mNextMonth = DDWRT_TRAFF_DATA_SIMPLE_DATE_FORMAT.format(cal2.getTime());

                    final String traffForPreviousMonthKey = \"fake-key\";
                    final String traffForCurrentMonthKey = \"fake-key\";
                    final String traffForNextMonthKey = \"fake-key\";

                    final NVRAMInfo nvramInfo = new NVRAMInfo();

                    NVRAMInfo nvramInfoTmp = null;
                    try {
                        nvramInfo.setProperty(PREVIOUS, mPrevMonth);
                        nvramInfo.setProperty(CURRENT, mCurrentMonth);
                        nvramInfo.setProperty(NEXT, mNextMonth);

                        if (isDemoRouter(mRouter)) {
                            final boolean ttraffEnabled = new Random().nextBoolean();
                            nvramInfo.setProperty(
                                    NVRAMInfo.TTRAFF_ENABLE,
                                    ttraffEnabled ? "1" : "0");
                        } else {
                            nvramInfoTmp = SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity,
                                    mRouter, mGlobalPreferences,
                                    NVRAMInfo.TTRAFF_ENABLE,
                                    traffForPreviousMonthKey,
                                    traffForCurrentMonthKey,
                                    traffForNextMonthKey);
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
                        final Random random = new Random();
                        final long totalDlMonth = (MB + MB * random.nextInt(500)) * MB;
                        nvramInfo.setProperty(TOTAL_DL_CURRENT_MONTH,
                                FileUtils
                                        .byteCountToDisplaySize(totalDlMonth));
                        nvramInfo.setProperty(TOTAL_DL_CURRENT_MONTH_MB,
                                HIDDEN_);
                        final long totalUlMonth = (1 + random.nextInt(100)) * MB;
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
                        retrieveAndPersistMonthlyTrafficData(mRouter, dao, nvramInfo);

                        nvramInfo.putAll(WANTrafficUtils.computeWANTrafficUsageBetweenDates(dao, mRouter.getUuid(),
                                mCycleItem.getStart(), mCycleItem.getEnd()));

//                        final String cycleStart = DDWRT_MONTHLY_TRAFFIC_DATE_WRITER
//                                .format(new Date(mCycleItem.getStart()));
//                        final String cycleEnd = DDWRT_MONTHLY_TRAFFIC_DATE_WRITER
//                                .format(new Date(mCycleItem.getEnd()));
//
//                        final List<WANTrafficData> wanTrafficDataByRouterBetweenDates =
//                                dao.getWANTrafficDataByRouterBetweenDates(mRouter.getUuid(), cycleStart, cycleEnd);
//                        //Compute total in/out
//                        long totalDownloadMBytes = 0l;
//                        long totalUploadMBytes = 0l;
//                        for (final WANTrafficData wanTrafficData : wanTrafficDataByRouterBetweenDates) {
//                            if (wanTrafficData == null) {
//                                continue;
//                            }
//                            totalDownloadMBytes += wanTrafficData.getTraffIn().doubleValue();
//                            totalUploadMBytes += wanTrafficData.getTraffOut().doubleValue();
//                        }
//
//                        final String inHumanReadable = FileUtils
//                                .byteCountToDisplaySize(totalDownloadMBytes * MB);
//                        nvramInfo.setProperty(TOTAL_DL_CURRENT_MONTH,
//                                inHumanReadable);
//                        if (inHumanReadable.equals(totalDownloadMBytes + " MB") ||
//                                inHumanReadable.equals(totalDownloadMBytes + " bytes")) {
//                            nvramInfo.setProperty(TOTAL_DL_CURRENT_MONTH_MB,
//                                    HIDDEN_);
//                        } else {
//                            nvramInfo.setProperty(TOTAL_DL_CURRENT_MONTH_MB,
//                                    String.valueOf(totalDownloadMBytes));
//                        }
//
//                        final String outHumanReadable = FileUtils
//                                .byteCountToDisplaySize(totalUploadMBytes * MB);
//                        nvramInfo.setProperty(TOTAL_UL_CURRENT_MONTH,
//                                outHumanReadable);
//                        if (outHumanReadable.equals(totalUploadMBytes + " MB") ||
//                                outHumanReadable.equals(totalUploadMBytes + " bytes")) {
//                            nvramInfo.setProperty(TOTAL_UL_CURRENT_MONTH_MB,
//                                    HIDDEN_);
//                        } else {
//                            nvramInfo.setProperty(TOTAL_UL_CURRENT_MONTH_MB,
//                                    String.valueOf(totalUploadMBytes));
//                        }

                        //Compute date for current day
                        final String trafficForCurrentMonth = nvramInfo.getProperty(traffForCurrentMonthKey);
                        if (trafficForCurrentMonth != null) {
                            final List<String> dailyTraffDataList = MONTHLY_TRAFF_DATA_SPLITTER
                                    .splitToList(trafficForCurrentMonth);
                            if (!(dailyTraffDataList == null || dailyTraffDataList.isEmpty())) {
                                if (mCurrentDay > 1 && dailyTraffDataList.size() >= mCurrentDay) {
                                    final String dailyInOutTraffData = dailyTraffDataList.get(mCurrentDay - 1);
                                    if (!(Strings.isNullOrEmpty(dailyInOutTraffData) || StringUtils.contains(dailyInOutTraffData, "["))) {
                                        //Day
                                        final List<String> currentDayTraffData = DAILY_TRAFF_DATA_SPLITTER.splitToList(dailyInOutTraffData);
                                        if (currentDayTraffData.size() >= 2) {
                                            final double totalDownloadMBytesForCurrentDay = Double.parseDouble(currentDayTraffData.get(0));
                                            final double totalUploadMBytesForCurrentDay = Double.parseDouble(currentDayTraffData.get(1));

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
        ///TODO
        return null;
    }

    @Override
    public void onLoadFinished(Loader<NVRAMInfo> loader, NVRAMInfo data) {
        try {
            //Set tiles
            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

            layout.findViewById(R.id.tile_overview_wan_total_traffic_loading_view)
                    .setVisibility(View.GONE);
            final View gridLayoutContainer = layout.findViewById(R.id.tile_overview_wan_total_traffic_gridLayout);
            gridLayoutContainer
                    .setVisibility(View.VISIBLE);


            final boolean isDayCycle = CYCLE_DAY.equals(mCycle);

            ((TextView) layout.findViewById(R.id.tile_overview_wan_total_traffic_title))
                    .setText(WAN_TOTAL_TRAFFIC + ": " + 
                            (isDayCycle ? mCurrentDayDisplayed :
                                    (mCycleItem != null ? mCycleItem.getLabel() : mCurrentMonthDisplayed)));

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
                    dlMB.setVisibility(View.INVISIBLE);
                } else {
                    dlMB.setVisibility(View.VISIBLE);
                }
                dlMB.setText(dlMBytesFromNvram != null ?
                        ("(" + dlMBytesFromNvram + " MB)") : "-");

                final TextView ulMB = (TextView) this.layout.findViewById(R.id.tile_overview_wan_total_traffic_ul_mb);
                final String ulMBytesFromNvram = data.getProperty(
                        isDayCycle ? TOTAL_UL_CURRENT_DAY_MB : TOTAL_UL_CURRENT_MONTH_MB);
                if (HIDDEN_.equals(ulMBytesFromNvram)) {
                    ulMB.setVisibility(View.INVISIBLE);
                } else {
                    ulMB.setVisibility(View.VISIBLE);
                }
                ulMB.setText(ulMBytesFromNvram != null ?
                        ("(" + ulMBytesFromNvram + " MB)") : "-");

                //Update last sync
                final RelativeTimeTextView lastSyncView = (RelativeTimeTextView) layout.findViewById(R.id.tile_last_sync);
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
                gridLayoutContainer.setOnClickListener(null);
            }

        }  finally {
            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished(): done loading!");
            mRefreshing.set(false);
            doneWithLoaderInstance(this, loader);
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
            case R.id.tile_overview_wan_total_traffic_options_change_cycle:
                final AlertDialog.Builder builder = new AlertDialog.Builder(mParentFragmentActivity);
                final LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());

                final View view = dialogInflater.inflate(R.layout.data_usage_cycle_editor, null, false);
                final NumberPicker cycleDayPicker = (NumberPicker) view.findViewById(R.id.wan_cycle_day);

                final int wanCycleDay;
                if (mParentFragmentPreferences != null) {
                    final int cycleDay = mParentFragmentPreferences.getInt(WAN_CYCLE_DAY_PREF, 1);
                    wanCycleDay = (cycleDay < 1 ? 1 : (cycleDay > 31 ? 31 : cycleDay));
                } else {
                    wanCycleDay = 1;
                }

                cycleDayPicker.setMinValue(1);
                cycleDayPicker.setMaxValue(31);
                cycleDayPicker.setValue(wanCycleDay);
                cycleDayPicker.setWrapSelectorWheel(true);

                builder.setTitle(R.string.data_usage_cycle_editor_title);
                builder.setView(view);

                builder.setCancelable(true);

                builder.setPositiveButton(R.string.data_usage_cycle_editor_positive,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // clear focus to finish pending text edits
                                cycleDayPicker.clearFocus();

                                final int wanCycleDay = cycleDayPicker.getValue();

                                //Update preferences
                                if (mParentFragmentPreferences == null) {
                                    return ;
                                }
                                mParentFragmentPreferences.edit()
                                        .putInt(WAN_CYCLE_DAY_PREF, wanCycleDay)
                                        .putString(CYCLE, CYCLE_MONTH)
                                        .apply();

                                final Calendar calendar = Calendar.getInstance();
                                mCurrentDay = calendar.get(Calendar.DAY_OF_MONTH);

                                mCycleItem = WANTrafficData
                                        .getCurrentWANCycle(mParentFragmentActivity, mParentFragmentPreferences);
                                
                                //Update title
                                ((TextView) layout.findViewById(R.id.tile_overview_wan_total_traffic_title))
                                        .setText(WAN_TOTAL_TRAFFIC + ": " + mCycleItem.getLabel());

                                //Update bandwidth data right away
                                if (mNvramInfo != null) {

                                    mNvramInfo.putAll(WANTrafficUtils.computeWANTrafficUsageBetweenDates(dao, mRouter.getUuid(),
                                            mCycleItem.getStart(), mCycleItem.getEnd()));

//                                    final String cycleStart = DDWRT_MONTHLY_TRAFFIC_DATE_WRITER
//                                            .format(new Date(mCycleItem.getStart()));
//                                    final String cycleEnd = DDWRT_MONTHLY_TRAFFIC_DATE_WRITER
//                                            .format(new Date(mCycleItem.getEnd()));
//
//                                    final List<WANTrafficData> wanTrafficDataByRouterBetweenDates =
//                                            dao.getWANTrafficDataByRouterBetweenDates(mRouter.getUuid(), cycleStart, cycleEnd);
//                                    //Compute total in/out
//                                    long totalDownloadMBytes = 0l;
//                                    long totalUploadMBytes = 0l;
//                                    for (final WANTrafficData wanTrafficData : wanTrafficDataByRouterBetweenDates) {
//                                        if (wanTrafficData == null) {
//                                            continue;
//                                        }
//                                        totalDownloadMBytes += wanTrafficData.getTraffIn().doubleValue();
//                                        totalUploadMBytes += wanTrafficData.getTraffOut().doubleValue();
//                                    }
//
//                                    final String inHumanReadable = FileUtils
//                                            .byteCountToDisplaySize(totalDownloadMBytes * MB);
//                                    mNvramInfo.setProperty(TOTAL_DL_CURRENT_MONTH,
//                                            inHumanReadable);
//                                    if (inHumanReadable.equals(totalDownloadMBytes + " MB") ||
//                                            inHumanReadable.equals(totalDownloadMBytes + " bytes")) {
//                                        mNvramInfo.setProperty(TOTAL_DL_CURRENT_MONTH_MB,
//                                                HIDDEN_);
//                                    } else {
//                                        mNvramInfo.setProperty(TOTAL_DL_CURRENT_MONTH_MB,
//                                                String.valueOf(totalDownloadMBytes));
//                                    }
//
//                                    final String outHumanReadable = FileUtils
//                                            .byteCountToDisplaySize(totalUploadMBytes * MB);
//                                    mNvramInfo.setProperty(TOTAL_UL_CURRENT_MONTH,
//                                            outHumanReadable);
//                                    if (outHumanReadable.equals(totalUploadMBytes + " MB") ||
//                                            outHumanReadable.equals(totalUploadMBytes + " bytes")) {
//                                        mNvramInfo.setProperty(TOTAL_UL_CURRENT_MONTH_MB,
//                                                HIDDEN_);
//                                    } else {
//                                        mNvramInfo.setProperty(TOTAL_UL_CURRENT_MONTH_MB,
//                                                String.valueOf(totalUploadMBytes));
//                                    }
                                    updateWANOverviewTile(CYCLE_MONTH);
                                }
                            }
                        });

                builder.create().show();

                return true;
            default:
                break;
        }

        if (cycle != null) {
            if (mParentFragmentPreferences != null) {
                mParentFragmentPreferences.edit()
                        .putString(CYCLE, cycle)
                        .apply();
            }

            updateWANOverviewTile(cycle);
        }

        return knownMenuItem;
    }

    public void updateWANOverviewTile(String cycle) {
        if (mNvramInfo != null) {
            final boolean isDayCycle = CYCLE_DAY.equals(cycle);

            //Update title
            ((TextView) layout.findViewById(R.id.tile_overview_wan_total_traffic_title))
                    .setText(WAN_TOTAL_TRAFFIC + ": " +
                            (isDayCycle ? mCurrentDayDisplayed : (mCycleItem != null ?
                                    mCycleItem.getLabel() : mCurrentMonthDisplayed)));

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
}