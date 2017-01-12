package org.rm3l.router_companion.firmwares.impl.ddwrt;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Patterns;

import com.google.common.base.Splitter;

import org.apache.commons.lang3.StringUtils;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.firmwares.AbstractRouterFirmwareConnector;
import org.rm3l.router_companion.firmwares.RemoteDataRetrievalListener;
import org.rm3l.router_companion.firmwares.impl.ddwrt.tile_data_workers.dashboard.network.NetworkTopologyMapTileWorker;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.router_companion.resources.MonthlyCycleItem;
import org.rm3l.router_companion.resources.PublicIPInfo;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.WANTrafficUtils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.rm3l.router_companion.resources.conn.NVRAMInfo.UPTIME_DAYS;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.UPTIME_HOURS;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.UPTIME_MINUTES;
import static org.rm3l.router_companion.tiles.dashboard.bandwidth.WANTotalTrafficOverviewTile.DDWRT_TRAFF_DATA_SIMPLE_DATE_FORMAT;
import static org.rm3l.router_companion.tiles.dashboard.bandwidth.WANTotalTrafficOverviewTile.TRAFF_PREFIX;
import static org.rm3l.router_companion.tiles.status.router.StatusRouterCPUTile.GREP_MODEL_PROC_CPUINFO;
import static org.rm3l.router_companion.utils.Utils.COMMA_SPLITTER;
import static org.rm3l.router_companion.utils.WANTrafficUtils.retrieveAndPersistMonthlyTrafficData;

/**
 * Created by rm3l on 08/01/2017.
 */

public class DDWRTFirmwareConnector extends AbstractRouterFirmwareConnector {

    public static final String MODEL = "DD_BOARD";


    @NonNull
    public static String getGrepProcMemInfo(@NonNull final String item) {
        return "grep \"^" + item + "\" /proc/meminfo ";
    }

    @Override
    public NVRAMInfo getDataForNetworkTopologyMapTile(
            @NonNull Context context, @NonNull Router router,
            @Nullable RemoteDataRetrievalListener dataRetrievalListener) throws Exception {
        return NetworkTopologyMapTileWorker
                .getDataForNetworkTopologyMapTile(context, router, dataRetrievalListener);
    }

    @Nullable
    @Override
    public String getWanPublicIpAddress(@NonNull Context context,
                                        @NonNull Router router,
                                        @Nullable RemoteDataRetrievalListener dataRetrievalListener)
            throws Exception {
        return SSHUtils.loadWanPublicIPFrom(context, router, null, dataRetrievalListener);
    }

    @Override
    @Nullable
    protected String goGetRouterModel(@NonNull Context context, @NonNull Router router)
            throws Exception {
        final String[] output = SSHUtils.getManualProperty(context, router,
                Utils.getGlobalSharedPreferences(context),
                String.format(
                        "/usr/sbin/nvram show 2>/dev/null | grep \"%s\" | awk -F'=' '{print $2}'",
                        MODEL));
        if (output != null && output.length > 0) {
            return output[0];
        }
        return null;
    }

    @Override
    public NVRAMInfo getDataForWANTotalTrafficOverviewTile(@NonNull Context context,
                                                              @NonNull Router router,
                                                              MonthlyCycleItem cycleItem,
                                                              @Nullable RemoteDataRetrievalListener dataRetrievalListener)
            throws Exception {

        final DDWRTCompanionDAO dao = RouterManagementActivity.getDao(context);
        final SharedPreferences globalSharedPreferences = Utils.getGlobalSharedPreferences(context);

        if (dataRetrievalListener != null) {
            dataRetrievalListener.onProgressUpdate(20);
        }

        final Date today = new Date();
        final String mCurrentMonth = DDWRT_TRAFF_DATA_SIMPLE_DATE_FORMAT.format(today);

        //Also retrieve data for previous month and next month
        final Calendar cal1 = Calendar.getInstance();
        cal1.add(Calendar.MONTH, -1);
        final String mPrevMonth = DDWRT_TRAFF_DATA_SIMPLE_DATE_FORMAT.format(cal1.getTime());

        final Calendar cal2 = Calendar.getInstance();
        cal2.add(Calendar.MONTH, 1);
        final String mNextMonth = DDWRT_TRAFF_DATA_SIMPLE_DATE_FORMAT.format(cal2.getTime());

        final String traffForPreviousMonthKey = \"fake-key\";
        final String traffForCurrentMonthKey = \"fake-key\";
        final String traffForNextMonthKey = \"fake-key\";

        final NVRAMInfo nvramInfo = new NVRAMInfo();

        NVRAMInfo nvramInfoTmp = null;
        try {
            if (dataRetrievalListener != null) {
                dataRetrievalListener.onProgressUpdate(50);
            }

            nvramInfoTmp = SSHUtils.getNVRamInfoFromRouter(context,
                    router, globalSharedPreferences,
                    NVRAMInfo.TTRAFF_ENABLE,
                    traffForPreviousMonthKey,
                    traffForCurrentMonthKey,
                    traffForNextMonthKey);

        }  finally {
            if (nvramInfoTmp != null) {
                nvramInfo.putAll(nvramInfoTmp);
            }
        }

        if (nvramInfo.isEmpty()) {
            throw new DDWRTNoDataException("No Data!");
        }

        retrieveAndPersistMonthlyTrafficData(router, dao, nvramInfo);

        if (dataRetrievalListener != null) {
            dataRetrievalListener.onProgressUpdate(75);
        }

        nvramInfo.putAll(WANTrafficUtils.computeWANTrafficUsageBetweenDates(dao, router.getUuid(),
                cycleItem.getStart(), cycleItem.getEnd()));

        return nvramInfo;
    }

    @Override
    protected NVRAMInfo getDataForUptimeTile(@NonNull Context context, @NonNull Router router, @Nullable RemoteDataRetrievalListener dataRetrievalListener) throws Exception {
        final SharedPreferences globalSharedPreferences = Utils.getGlobalSharedPreferences(context);

        if (dataRetrievalListener != null) {
            dataRetrievalListener.onProgressUpdate(10);
        }

        final NVRAMInfo nvramInfo = new NVRAMInfo();

        final String[] uptimeOutput = SSHUtils.getManualProperty(context, 
                router, globalSharedPreferences,
                "uptime 2>/dev/null | awk -F'up ' '{print $2}' | awk -F'users' '{print $1}' | awk -F'load' '{print $1}'");

        if (dataRetrievalListener != null) {
            dataRetrievalListener.onProgressUpdate(60);
        }

        if (uptimeOutput != null && uptimeOutput.length > 0) {
            final String uptime = uptimeOutput[uptimeOutput.length - 1];
            final List<String> uptimeList = COMMA_SPLITTER.splitToList(uptime);
            if (uptimeList != null) {
                final int uptimeListSize = uptimeList.size();
                if (uptimeListSize > 0) {
                    final String first = uptimeList.get(0).trim();
                    if (StringUtils.contains(first, "day")) {
                        //day
                        nvramInfo.setProperty(UPTIME_DAYS,
                                first
                                        .replaceAll("days", "")
                                        .replaceAll("day", "")
                                        .trim());

                        if (uptimeListSize >= 2) {
                            final String other = uptimeList.get(1);
                            if (other != null) {
                                if (other.contains(":")) {
                                    final List<String> otherList =
                                            Splitter.on(":").omitEmptyStrings()
                                                    .splitToList(other);
                                    if (otherList != null) {
                                        if (otherList.size() >= 1) {
                                            nvramInfo.setProperty(UPTIME_HOURS,
                                                    otherList.get(0).trim());
                                        }
                                        if (otherList.size() >= 2) {
                                            nvramInfo.setProperty(UPTIME_MINUTES,
                                                    otherList.get(1).trim());
                                        }
                                    }
                                } else if (StringUtils.contains(other, "hour")) {
                                    nvramInfo.setProperty(UPTIME_HOURS,
                                            other
                                                    .replaceAll("hours", "")
                                                    .replaceAll("hour", "").trim());
                                } else if (StringUtils.contains(other, "min")) {
                                    nvramInfo.setProperty(UPTIME_MINUTES,
                                            other
                                                    .replaceAll("mins", "")
                                                    .replaceAll("min", "").trim());
                                }
                            }
                        }
                    } else if (StringUtils.contains(first, ":")) {
                        final List<String> otherList =
                                Splitter.on(":").omitEmptyStrings()
                                        .splitToList(first);
                        if (otherList != null) {
                            if (otherList.size() >= 1) {
                                nvramInfo.setProperty(UPTIME_HOURS,
                                        otherList.get(0).trim());
                            }
                            if (otherList.size() >= 2) {
                                nvramInfo.setProperty(UPTIME_MINUTES,
                                        otherList.get(1).trim());
                            }
                        }
                    } else if (StringUtils.contains(first, "hour")) {
                        nvramInfo.setProperty(UPTIME_HOURS,
                                first
                                        .replaceAll("hours", "")
                                        .replaceAll("hour", "").trim());
                    } else if (StringUtils.contains(first, "min")) {
                        nvramInfo.setProperty(UPTIME_MINUTES,
                                first
                                        .trim()
                                        .replaceAll("mins", "")
                                        .replaceAll("min", "").trim());
                    }
                }
            }
        }

        return nvramInfo;
    }

    @Override
    public List<String[]> getDataForMemoryAndCpuUsageTile(@NonNull Context context,
                                                          @NonNull Router router,
                                                          @Nullable RemoteDataRetrievalListener dataRetrievalListener) throws Exception {

        if (dataRetrievalListener != null) {
            dataRetrievalListener.onProgressUpdate(10);
        }

        final SharedPreferences globalSharedPreferences =
                Utils.getGlobalSharedPreferences(context);

        final String[] memData = SSHUtils.getManualProperty(context, router,
                globalSharedPreferences,
                getGrepProcMemInfo("MemTotal"),
                getGrepProcMemInfo("MemFree"));

        if (dataRetrievalListener != null) {
            dataRetrievalListener.onProgressUpdate(30);
        }

        final String[] cpuUsageData = SSHUtils.getManualProperty(context, router,
                globalSharedPreferences,
                "uptime | awk -F'average:' '{ print $2}'",
                GREP_MODEL_PROC_CPUINFO + "| wc -l");

        return Arrays.asList(memData, cpuUsageData);
    }

}
