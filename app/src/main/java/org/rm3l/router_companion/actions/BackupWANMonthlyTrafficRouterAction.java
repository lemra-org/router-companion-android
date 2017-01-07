package org.rm3l.router_companion.actions;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.io.Files;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.rm3l.router_companion.common.resources.audit.ActionLog;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.DDWRTCompanionConstants;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;

import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.rm3l.router_companion.utils.DDWRTCompanionConstants.CHARSET;
import static org.rm3l.router_companion.utils.DDWRTCompanionConstants.MB;
import static org.rm3l.router_companion.utils.WANTrafficUtils.DAILY_TRAFF_DATA_SPLITTER;
import static org.rm3l.router_companion.utils.WANTrafficUtils.MONTHLY_TRAFF_DATA_SPLITTER;

/**
 * Created by rm3l on 09/05/15.
 */
public class BackupWANMonthlyTrafficRouterAction extends AbstractRouterAction<String> {

    public static final int BackupFileType_RAW = 1;
    public static final int BackupFileType_CSV = 2;

    @NonNull
    private final Context mContext;
    private final int mBackupFileType;
    private File mLocalBackupFilePath = null;
    private Date mBackupDate = null;

    public BackupWANMonthlyTrafficRouterAction(Router router, @NonNull final int backupFileType,
                                               @NonNull Context context, @Nullable RouterActionListener listener,
                                               @NonNull final SharedPreferences globalSharedPreferences) {
        super(router, listener, RouterAction.BACKUP_WAN_TRAFF, globalSharedPreferences);
        this.mContext = context;
        this.mBackupFileType = backupFileType;
    }

    @Nullable
    @Override
    protected ActionLog getActionLog() {
        return new ActionLog()
                .setActionName(routerAction.toString())
                .setActionData(String.format("Backup type: %s\n" +
                        "Backup File: %s", mBackupFileType,
                        mLocalBackupFilePath != null ? mLocalBackupFilePath.getAbsolutePath() : "-"));
    }

    @Nullable
    @Override
    protected Context getContext() {
        return mContext;
    }

    @NonNull
    @Override
    protected RouterActionResult<String> doActionInBackground() {
        Exception exception = null;
        try {
            mBackupDate = new Date();

            final NVRAMInfo nvramInfo = SSHUtils.getNVRamInfoFromRouter(mContext,
                    router,
                    globalSharedPreferences,
                    "traff-.*");

            if (nvramInfo == null) {
                throw new IllegalStateException("Failed to fetch WAN Traffic Data from Router");
            }

            final NVRAMInfo nvramInfoWithTraffData = new NVRAMInfo();

            //{year: month: {day: [in, out], ...}, ...}
            final ImmutableTable.Builder<Long, Integer, Multimap<Integer, Long>> traffDataTableBuilder
                    = ImmutableTable.<Long, Integer, Multimap<Integer, Long>>builder()
                    .orderRowsBy(new Comparator<Long>() {
                        @Override
                        public int compare(Long o, Long t1) {
                            if (o == t1) {
                                return 0;
                            }
                            if (o == null) {
                                return -1;
                            }
                            if (t1 == null) {
                                return 1;
                            }
                            return o.compareTo(t1);
                        }
                    }).orderColumnsBy(new Comparator<Integer>() {
                        @Override
                        public int compare(Integer integer, Integer t1) {
                            if (integer == t1) {
                                return 0;
                            }
                            if (integer == null) {
                                return -1;
                            }
                            if (t1 == null) {
                                return 1;
                            }
                            return integer.compareTo(t1);
                        }
                    });

            final Splitter splitter = Splitter.on("-").omitEmptyStrings().trimResults();

            @SuppressWarnings("ConstantConditions")
            final Set<Map.Entry<Object, Object>> entries = nvramInfo.getData().entrySet();

            for (final Map.Entry<Object, Object> entry : entries) {
                final Object key;
                final Object value;
                if (entry == null || (key = entry.getKey()) == null || (value = entry.getValue()) == null) {
                    continue;
                }

                if (!StringUtils.startsWithIgnoreCase(key.toString(), "traff-")) {
                    continue;
                }

                nvramInfoWithTraffData.setProperty(key.toString(), value.toString());

                final String monthAndYear = key.toString().replace("traff-",
                        DDWRTCompanionConstants.EMPTY_STRING);

                final List<String> monthAndYearList = splitter.splitToList(monthAndYear);
                if (monthAndYearList.size() < 2) {
                    continue;
                }

                final String monthlyTraffData = value.toString();

                final List<String> dailyTraffDataList = MONTHLY_TRAFF_DATA_SPLITTER
                        .splitToList(monthlyTraffData);
                if (dailyTraffDataList.isEmpty()) {
                    continue;
                }

                final int month = Integer.parseInt(monthAndYearList.get(0));
                final long year = Long.parseLong(monthAndYearList.get(1));

                final Multimap<Integer, Long> dataMap = LinkedHashMultimap.create();

                int dayNum = 1;
                for (final String dailyInOutTraffData : dailyTraffDataList) {
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

                    dataMap.putAll(dayNum++, Lists.newArrayList(
                            Long.parseLong(inTraff) * MB, Long.parseLong(outTraff) * MB
                    ));
                }

                traffDataTableBuilder.put(year, month, dataMap);
            }

            final ImmutableTable<Long, Integer, Multimap<Integer, Long>> traffDataTable =
                    traffDataTableBuilder.build();

            if (traffDataTable.isEmpty()) {
                throw new IllegalStateException("No WAN Traffic Data found in the Router.");
            }

            String escapedFileName =
                    Utils.getEscapedFileName("traffdata_" + router.getDisplayName() + "_" +
                            router.getRemoteIpAddress() + "_" +
                            router.getUuid() + "__" + mBackupDate)
                            + "__.bak";

            switch (mBackupFileType) {
                case BackupFileType_CSV:
                    escapedFileName += ".csv";
                    break;
                default:
                    break;
            }

            //Write to app data storage on internal storage
            mLocalBackupFilePath = new File(mContext.getCacheDir(),
                    escapedFileName);

            switch (mBackupFileType) {
                case BackupFileType_CSV:
                    Files.write("Year,Month,Day,Inbound,Inbound (Readable),Outbound,Outbound (Readable)\n", mLocalBackupFilePath,
                            CHARSET);
                    final ImmutableSet<Table.Cell<Long, Integer, Multimap<Integer, Long>>> cells = traffDataTable.cellSet();
                    for (final Table.Cell<Long, Integer, Multimap<Integer, Long>> cell : cells) {
                        final Long year = cell.getRowKey();
                        final Integer month = cell.getColumnKey();
                        final Multimap<Integer, Long> dayInAndOutMultimap = cell.getValue();
                        if (dayInAndOutMultimap == null) {
                            continue;
                        }
                        final Map<Integer, Collection<Long>> dayInAndOutMap = dayInAndOutMultimap.asMap();
                        for (final Map.Entry<Integer, Collection<Long>> entry : dayInAndOutMap.entrySet()) {
                            final Integer day = entry.getKey();
                            final Collection<Long> inAndOut = entry.getValue();
                            if (inAndOut == null || inAndOut.size() < 2) {
                                continue;
                            }
                            final Long[] traffDataForDay = inAndOut.toArray(new Long[inAndOut.size()]);
                            Files.append(String.format("%d,%d,%d,%d,%s,%d,%s\n",
                                    year,
                                    month,
                                    day,
                                    traffDataForDay[0],
                                    FileUtils.byteCountToDisplaySize(traffDataForDay[0]),
                                    traffDataForDay[1],
                                    FileUtils.byteCountToDisplaySize(traffDataForDay[1])),
                                    mLocalBackupFilePath, CHARSET);
                        }

                    }
                    break;

                case BackupFileType_RAW:
                default:
                    Files.write("TRAFF-DATA\n", mLocalBackupFilePath, CHARSET);
                    //noinspection ConstantConditions
                    Files.append(Joiner
                                    .on("\n")
                                    .withKeyValueSeparator("=")
                                    .useForNull("\n")
                                    .join(nvramInfoWithTraffData.getData()),
                            mLocalBackupFilePath, CHARSET);
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
            exception = e;
        }

        return new RouterActionResult<>(null, exception);
    }

    @Nullable
    @Override
    protected Object getDataToReturnOnSuccess() {
        return new Object[]{mBackupDate, mLocalBackupFilePath};
    }
}
