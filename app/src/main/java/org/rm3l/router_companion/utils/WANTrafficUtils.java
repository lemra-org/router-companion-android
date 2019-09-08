package org.rm3l.router_companion.utils;

import static org.rm3l.router_companion.RouterCompanionAppConstants.EMPTY_STRING;
import static org.rm3l.router_companion.RouterCompanionAppConstants.MB;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import com.crashlytics.android.Crashlytics;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.router_companion.resources.WANTrafficData;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;

/**
 * Created by rm3l on 12/11/15.
 */
public final class WANTrafficUtils {

    public static final String TAG = WANTrafficUtils.class.getSimpleName();

    public static final SimpleDateFormat DDWRT_MONTHLY_TRAFFIC_DATE_READER =
            new SimpleDateFormat("MM-yyyy/dd", Locale.US);

    public static final SimpleDateFormat DDWRT_MONTHLY_TRAFFIC_DATE_WRITER =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public static final Splitter MONTHLY_TRAFF_DATA_SPLITTER = Splitter.on(" ").omitEmptyStrings();

    public static final Splitter DAILY_TRAFF_DATA_SPLITTER = Splitter.on(":").omitEmptyStrings();

    public static final SimpleDateFormat SIMPLE_DATE_FORMAT =
            new SimpleDateFormat("MM-yyyy", Locale.US);

    public static final String TOTAL_DL_CURRENT_MONTH = "TOTAL_DL_CURRENT_MONTH";

    public static final String TOTAL_UL_CURRENT_MONTH = "TOTAL_UL_CURRENT_MONTH";

    public static final String TOTAL_DL_CURRENT_MONTH_MB = "TOTAL_DL_CURRENT_MONTH_MB";

    public static final String TOTAL_UL_CURRENT_MONTH_MB = "TOTAL_UL_CURRENT_MONTH_MB";

    public static final String HIDDEN_ = "_HIDDEN_";

    @NonNull
    public static NVRAMInfo computeWANTrafficUsageBetweenDates(@NonNull final DDWRTCompanionDAO dao,
            @NonNull final String router, final long start, final long end) {

        return computeWANTrafficUsageFromWANTrafficDataBreakdownBetweenDates(
                getWANTrafficDataByRouterBetweenDates(dao, router, start, end));
    }

    public static NVRAMInfo computeWANTrafficUsageFromWANTrafficDataBreakdownBetweenDates(
            List<WANTrafficData> wanTrafficDataByRouterBetweenDates) {
        final NVRAMInfo nvramInfo = new NVRAMInfo();
        //Compute total in/out
        long totalDownloadMBytes = 0l;
        long totalUploadMBytes = 0l;
        for (final WANTrafficData wanTrafficData : wanTrafficDataByRouterBetweenDates) {
            if (wanTrafficData == null) {
                continue;
            }
            totalDownloadMBytes += wanTrafficData.getTraffIn().doubleValue();
            totalUploadMBytes += wanTrafficData.getTraffOut().doubleValue();
        }

        final String inHumanReadable =
                org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(totalDownloadMBytes * MB);
        nvramInfo.setProperty(TOTAL_DL_CURRENT_MONTH, inHumanReadable);
        if (inHumanReadable.equals(totalDownloadMBytes + " MB") || inHumanReadable.equals(
                totalDownloadMBytes + " bytes")) {
            nvramInfo.setProperty(TOTAL_DL_CURRENT_MONTH_MB, HIDDEN_);
        } else {
            nvramInfo.setProperty(TOTAL_DL_CURRENT_MONTH_MB, String.valueOf(totalDownloadMBytes));
        }

        final String outHumanReadable =
                org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(totalUploadMBytes * MB);
        nvramInfo.setProperty(TOTAL_UL_CURRENT_MONTH, outHumanReadable);
        if (outHumanReadable.equals(totalUploadMBytes + " MB") || outHumanReadable.equals(
                totalUploadMBytes + " bytes")) {
            nvramInfo.setProperty(TOTAL_UL_CURRENT_MONTH_MB, HIDDEN_);
        } else {
            nvramInfo.setProperty(TOTAL_UL_CURRENT_MONTH_MB, String.valueOf(totalUploadMBytes));
        }

        return nvramInfo;
    }

    @Nullable
    public static String getSqliteFormattedDate(final String ddwrtRawMonthYear, final int dayNum) {
        final Date date;
        try {
            date = DDWRT_MONTHLY_TRAFFIC_DATE_READER.parse(
                    String.format("%s/%s", ddwrtRawMonthYear, dayNum));
        } catch (final ParseException e) {
            Utils.reportException(null, e);
            e.printStackTrace();
            return null;
        }
        return DDWRT_MONTHLY_TRAFFIC_DATE_WRITER.format(date);
    }

    @NonNull
    public static NVRAMInfo getTrafficDataNvramInfoAndPersistIfNeeded(Context ctx, Router router,
            SharedPreferences globalPreferences, DDWRTCompanionDAO dao) throws Exception {

        final NVRAMInfo nvramInfo = new NVRAMInfo();

        NVRAMInfo nvramInfoTmp = null;
        try {
            //noinspection ConstantConditions
            nvramInfoTmp = NVRAMParser.parseNVRAMOutput(
                    SSHUtils.getManualProperty(ctx, router, globalPreferences,
                            "/usr/sbin/nvram show 2>/dev/null | grep traff[-_]"));
        } finally {
            if (nvramInfoTmp != null) {
                nvramInfo.putAll(nvramInfoTmp);
            }
        }

        if (nvramInfo.isEmpty()) {
            throw new DDWRTNoDataException("No Data!");
        }

        retrieveAndPersistMonthlyTrafficData(router, dao, nvramInfo);

        return nvramInfo;
    }

    @NonNull
    public static List<WANTrafficData> getWANTrafficDataByRouterBetweenDates(
            @NonNull final DDWRTCompanionDAO dao, @NonNull final String router, final long start,
            final long end) {
        final String cycleStart = DDWRT_MONTHLY_TRAFFIC_DATE_WRITER.format(new Date(start));
        final String cycleEnd = DDWRT_MONTHLY_TRAFFIC_DATE_WRITER.format(new Date(end));
        Crashlytics.log(Log.DEBUG, TAG,
                "<cycleStart,cycleEnd>=<" + cycleStart + "," + cycleStart + ">");
        return dao.getWANTrafficDataByRouterBetweenDates(router, cycleStart, cycleEnd);
    }

    public static void retrieveAndPersistMonthlyTrafficData(@Nullable final Router router,
            @Nullable final DDWRTCompanionDAO dao, @Nullable final NVRAMInfo nvramInfo) {
        if (router == null || dao == null || nvramInfo == null || nvramInfo.isEmpty()) {
            return;
        }
        final Properties data = nvramInfo.getData();
        if (data == null) {
            return;
        }

        final String routerUuid = router.getUuid();

        for (final Map.Entry<Object, Object> dataEntrySet : data.entrySet()) {
            final Object key = dataEntrySet.getKey();
            final Object value = dataEntrySet.getValue();
            if (key == null || value == null) {
                continue;
            }

            if (!key.toString().startsWith("traff-")) {
                continue;
            }

            final String monthYear = key.toString().replace("traff-", EMPTY_STRING);

            final String monthlyTraffData = value.toString();

            final List<String> dailyTraffDataList =
                    MONTHLY_TRAFF_DATA_SPLITTER.splitToList(monthlyTraffData);
            if (dailyTraffDataList == null || dailyTraffDataList.isEmpty()) {
                continue;
            }

            final List<WANTrafficData> wanTrafficDataList = new java.util.ArrayList<>();

            int dayNum = 0;
            for (final String dailyInOutTraffData : dailyTraffDataList) {
                Crashlytics.log(Log.DEBUG, TAG, "dailyInOutTraffData=<" + dailyInOutTraffData + ">");
                if (dailyInOutTraffData == null) {
                    continue;
                }
                if (dailyInOutTraffData.contains("[") || dailyInOutTraffData.contains("]")) {
                    continue;
                }
                final List<String> dailyInOutTraffDataList =
                        DAILY_TRAFF_DATA_SPLITTER.splitToList(dailyInOutTraffData);
                if (dailyInOutTraffDataList.size() < 2) {
                    continue;
                }
                ++dayNum;

                final String inTraff = dailyInOutTraffDataList.get(0);
                final String outTraff = dailyInOutTraffDataList.get(1);
                if (inTraff == null || inTraff.trim().isEmpty() || outTraff == null || outTraff.trim().isEmpty()) {
                    continue;
                }

                final String sqliteFormattedDate = getSqliteFormattedDate(monthYear, dayNum);
                if (Strings.isNullOrEmpty(sqliteFormattedDate)) {
                    continue;
                }

                // Always try to persist data in DB -
                // there is an "ON CONFLICT REPLACE" constraint that will make the DB update the record if needed
                final double inTraffDouble = Double.parseDouble(inTraff);
                final double outTraffDouble = Double.parseDouble(outTraff);

                wanTrafficDataList.add(
                        new WANTrafficData(routerUuid, sqliteFormattedDate, inTraffDouble, outTraffDouble));
            }
            if (!wanTrafficDataList.isEmpty()) {
                dao.insertWANTrafficData(
                        wanTrafficDataList.toArray(new WANTrafficData[wanTrafficDataList.size()]));
            }
        }
    }

    private WANTrafficUtils() {
    }
}