package org.rm3l.ddwrt.utils;

import android.support.annotation.Nullable;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import org.apache.commons.lang3.StringUtils;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.resources.WANTrafficData;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.EMPTY_STRING;

/**
 * Created by rm3l on 12/11/15.
 */
public final class WANTrafficUtils {

    public static final SimpleDateFormat DDWRT_MONTHLY_TRAFFIC_DATE_READER =
            new SimpleDateFormat("MM-yyyy/dd", Locale.US);
    public static final SimpleDateFormat DDWRT_MONTHLY_TRAFFIC_DATE_WRITER =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public static final Splitter MONTHLY_TRAFF_DATA_SPLITTER = Splitter.on(" ").omitEmptyStrings();
    public static final Splitter DAILY_TRAFF_DATA_SPLITTER = Splitter.on(":").omitEmptyStrings();
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("MM-yyyy", Locale.US);

    private WANTrafficUtils() {}

    public static void retrieveAndPersistMonthlyTrafficData(@Nullable final Router router,
                                                            @Nullable final DDWRTCompanionDAO dao,
                                                            @Nullable final NVRAMInfo nvramInfo) {
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

            if (!StringUtils.startsWithIgnoreCase(key.toString(), "traff-")) {
                continue;
            }

            final String monthYear = key.toString().replace("traff-", EMPTY_STRING);

            final String monthlyTraffData = value.toString();

            final List<String> dailyTraffDataList = MONTHLY_TRAFF_DATA_SPLITTER.splitToList(monthlyTraffData);
            if (dailyTraffDataList == null || dailyTraffDataList.isEmpty()) {
                continue;
            }

            int dayNum = 0;
            for (final String dailyInOutTraffData : dailyTraffDataList) {
                if (StringUtils.contains(dailyInOutTraffData, "[")) {
                    continue;
                }
                final List<String> dailyInOutTraffDataList = DAILY_TRAFF_DATA_SPLITTER.splitToList(dailyInOutTraffData);
                if (dailyInOutTraffDataList.size() < 2) {
                    continue;
                }
                ++dayNum;

                final String inTraff = dailyInOutTraffDataList.get(0);
                final String outTraff = dailyInOutTraffDataList.get(1);

                final String sqliteFormattedDate = getSqliteFormattedDate(monthYear, dayNum);
                if (Strings.isNullOrEmpty(sqliteFormattedDate)) {
                    continue;
                }

                // Always try to persist data in DB -
                // there is an "ON CONFLICT REPLACE" constraint that will make the DB update the record if needed
                final double inTraffDouble = Double.parseDouble(inTraff);
                final double outTraffDouble = Double.parseDouble(outTraff);

                final WANTrafficData wanTrafficData = new WANTrafficData(routerUuid,
                        sqliteFormattedDate,
                        inTraffDouble,
                        outTraffDouble);

                dao.insertWANTrafficData(wanTrafficData);
            }
        }
    }

    @Nullable
    public static String getSqliteFormattedDate(final String ddwrtRawMonthYear, final int dayNum) {
        final Date date;
        try {
            date = DDWRT_MONTHLY_TRAFFIC_DATE_READER.parse(String.format("%s/%s", ddwrtRawMonthYear, dayNum));
        } catch (final ParseException e) {
            Utils.reportException(null, e);
            e.printStackTrace();
            return null;
        }
        return DDWRT_MONTHLY_TRAFFIC_DATE_WRITER.format(date);
    }
}
