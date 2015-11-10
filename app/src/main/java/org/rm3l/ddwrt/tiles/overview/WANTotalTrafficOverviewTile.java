package org.rm3l.ddwrt.tiles.overview;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.text.format.DateUtils;
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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;

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
import org.rm3l.ddwrt.utils.ReportingUtils;
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

import static android.text.format.DateUtils.FORMAT_ABBREV_MONTH;
import static android.text.format.DateUtils.FORMAT_SHOW_DATE;
import static org.rm3l.ddwrt.tiles.status.wan.WANMonthlyTrafficTile.DAILY_TRAFF_DATA_SPLITTER;
import static org.rm3l.ddwrt.tiles.status.wan.WANMonthlyTrafficTile.MONTHLY_TRAFF_DATA_SPLITTER;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.MB;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.WAN_CYCLE_DAY_PREF;
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
    public static final String WAN_TOTAL_TRAFFIC = "WAN Usage";

    private boolean isThemeLight;
    private long mLastSync;
    private String mCurrentMonth;
    private String mCurrentMonthDisplayed;

    private final Map<Integer, ArrayList<Double>> mCurrentTraffMonthlyData = new ConcurrentHashMap<>();
    private int mCurrentDay;
    private String mCurrentDayDisplayed;
    private String mCycle;

    private NVRAMInfo mNvramInfo;

    private CycleItem mCycleItem;

    public WANTotalTrafficOverviewTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_overview_wan_total_traffic, null);
        isThemeLight = ColorUtils.isThemeLight(mParentFragmentActivity);
        //Create Options Menu
        final ImageButton tileMenu = (ImageButton) layout.findViewById(R.id.tile_overview_wan_total_traffic_menu);

        if (!ColorUtils.isThemeLight(mParentFragmentActivity)) {
            //Set menu background to white
            tileMenu.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
        }

        final int wanCycleDay;
        if (mParentFragmentPreferences != null) {
            final int cycleDay = mParentFragmentPreferences.getInt(WAN_CYCLE_DAY_PREF, 1);
            wanCycleDay = (cycleDay < 1 ? 1 : (cycleDay > 31 ? 31 : cycleDay));
        } else {
            wanCycleDay = 1;
        }
        final Calendar calendar = Calendar.getInstance();
        final long start;
        final long end;
        if (mCurrentDay < wanCycleDay) {
            //Effective Period: [wanCycleDay-1M, wanCycleDay]
            calendar.set(Calendar.DAY_OF_MONTH, wanCycleDay);
            calendar.add(Calendar.DATE, -1);
            end = calendar.getTimeInMillis();

            calendar.add(Calendar.MONTH, -1);
            start = calendar.getTimeInMillis();
        } else {
            //Effective Period: [wanCycleDay, wanCycleDay + 1M]
            calendar.set(Calendar.DAY_OF_MONTH, wanCycleDay);
            start = calendar.getTimeInMillis();

            calendar.add(Calendar.MONTH, 1);
            calendar.add(Calendar.DATE, -1);
            end = calendar.getTimeInMillis();
        }
        mCycleItem = new CycleItem(mParentFragmentActivity, start, end);


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

                final Date today = new Date();
                final Calendar calendar = Calendar.getInstance();
                mCurrentDay = calendar.get(Calendar.DAY_OF_MONTH);
                mCurrentDayDisplayed = new SimpleDateFormat("MMM dd, yyyy",
                        Locale.getDefault()).format(today);
                dayMenuItem.setTitle(String.format("Today (%s)", mCurrentDayDisplayed));

                try {
                    if (mCycleItem == null) {
                        //1st of each month
                        calendar.set(Calendar.DAY_OF_MONTH, 1);
                        final long start = calendar.getTimeInMillis();

                        calendar.add(Calendar.MONTH, 1);
                        calendar.add(Calendar.DATE, -1);
                        final long end = calendar.getTimeInMillis();

                        mCycleItem = new CycleItem(mParentFragmentActivity, start, end);
                    }
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
                                getFormattedPrefKey(CYCLE), CYCLE_MONTH) : null);

                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Init background loader for " + WANTotalTrafficOverviewTile.class + ": routerInfo=" +
                            mRouter + " / nbRunsLoader=" + nbRunsLoader);

                    if (mRefreshing.getAndSet(true)) {
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    mCurrentTraffMonthlyData.clear();

                    mLastSync = System.currentTimeMillis();

                    final int wanCycleDay;
                    if (mParentFragmentPreferences != null) {
                        final int cycleDay = mParentFragmentPreferences.getInt(WAN_CYCLE_DAY_PREF, 1);
                        wanCycleDay = (cycleDay < 1 ? 1 : (cycleDay > 31 ? 31 : cycleDay));
                    } else {
                        wanCycleDay = 1;
                    }
                    final Calendar calendar = Calendar.getInstance();
                    final long start;
                    final long end;
                    if (mCurrentDay < wanCycleDay) {
                        //Effective Period: [wanCycleDay-1M, wanCycleDay]
                        calendar.set(Calendar.DAY_OF_MONTH, wanCycleDay);
                        calendar.add(Calendar.DATE, -1);
                        end = calendar.getTimeInMillis();

                        calendar.add(Calendar.MONTH, -1);
                        start = calendar.getTimeInMillis();
                    } else {
                        //Effective Period: [wanCycleDay, wanCycleDay + 1M]
                        calendar.set(Calendar.DAY_OF_MONTH, wanCycleDay);
                        start = calendar.getTimeInMillis();

                        calendar.add(Calendar.MONTH, 1);
                        calendar.add(Calendar.DATE, -1);
                        end = calendar.getTimeInMillis();
                    }
                    mCycleItem = new CycleItem(mParentFragmentActivity, start, end);


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

                                    //FIXME Persist in DB

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

                final NVRAMInfo dataCopy = data;
                gridLayoutContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String currentMonth = dataCopy.getProperty(CURRENT);
                        if (currentMonth == null) {
                            Utils.displayMessage(mParentFragmentActivity, "Internal Error. Please try again later.", Style.ALERT);
                            ReportingUtils.reportException(null, new IllegalStateException("currentMonth == null"));
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
                //TODO
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
                                        .apply();

                                final Calendar calendar = Calendar.getInstance();
                                mCurrentDay = calendar.get(Calendar.DAY_OF_MONTH);

                                final long start;
                                final long end;
                                if (mCurrentDay < wanCycleDay) {
                                    //Effective Period: [wanCycleDay-1M, wanCycleDay]
                                    calendar.set(Calendar.DAY_OF_MONTH, wanCycleDay);
                                    calendar.add(Calendar.DATE, -1);
                                    end = calendar.getTimeInMillis();

                                    calendar.add(Calendar.MONTH, -1);
                                    start = calendar.getTimeInMillis();
                                } else {
                                    //Effective Period: [wanCycleDay, wanCycleDay + 1M]
                                    calendar.set(Calendar.DAY_OF_MONTH, wanCycleDay);
                                    start = calendar.getTimeInMillis();

                                    calendar.add(Calendar.MONTH, 1);
                                    calendar.add(Calendar.DATE, -1);
                                    end = calendar.getTimeInMillis();
                                }

                                mCycleItem = new CycleItem(mParentFragmentActivity, start, end);

                                //FIXME Good to apply this new value right away
//                                final String cycleTimezone = new Time().timezone;
//                                editor.setPolicyCycleDay(template, cycleDay, cycleTimezone);
//                                target.updatePolicy(true);
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
                        .putString(getFormattedPrefKey(CYCLE), cycle)
                        .apply();
            }

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

        return knownMenuItem;
    }

    /**
     * List item that reflects a specific data usage cycle.
     */
    public static class CycleItem implements Comparable<CycleItem> {
        private CharSequence label;
        private long start;
        private long end;

        CycleItem(CharSequence label) {
            this.label = label;
        }

        public CycleItem(Context context, long start, long end) {
            this.label = formatDateRange(context, start, end);
            this.start = start;
            this.end = end;
        }

        public CharSequence getLabel() {
            return label;
        }

        public void setLabel(CharSequence label) {
            this.label = label;
        }

        public long getStart() {
            return start;
        }

        public void setStart(long start) {
            this.start = start;
        }

        public long getEnd() {
            return end;
        }

        public void setEnd(long end) {
            this.end = end;
        }

        @Override
        public String toString() {
            return label.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof CycleItem) {
                final CycleItem another = (CycleItem) o;
                return start == another.start && end == another.end;
            }
            return false;
        }

        @Override
        public int compareTo(@NonNull CycleItem another) {
            return Longs.compare(start, another.start);
        }
    }

    private static final StringBuilder sBuilder = new StringBuilder(50);
    private static final java.util.Formatter sFormatter = new java.util.Formatter(
            sBuilder, Locale.getDefault());

    public static String formatDateRange(Context context, long start, long end) {
        final int flags = FORMAT_SHOW_DATE | FORMAT_ABBREV_MONTH;

        synchronized (sBuilder) {
            sBuilder.setLength(0);
            return DateUtils.formatDateRange(context, sFormatter, start, end, flags, null)
                    .toString();
        }
    }
}