package org.rm3l.router_companion.firmwares.impl.tomato;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Patterns;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import org.apache.commons.lang3.StringUtils;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.firmwares.AbstractRouterFirmwareConnector;
import org.rm3l.router_companion.firmwares.RemoteDataRetrievalListener;
import org.rm3l.router_companion.firmwares.RouterFirmwareConnectorManager;
import org.rm3l.router_companion.firmwares.impl.ddwrt.DDWRTFirmwareConnector;
import org.rm3l.router_companion.firmwares.impl.tomato.tile_data_workers.dashboard.network.NetworkTopologyMapTileWorker;
import org.rm3l.router_companion.resources.MonthlyCycleItem;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.service.tasks.PublicIPChangesServiceTask;
import org.rm3l.router_companion.tiles.dashboard.system.UptimeTile;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;

import java.util.Arrays;
import java.util.List;

import static org.rm3l.router_companion.RouterCompanionAppConstants.NOK;
import static org.rm3l.router_companion.RouterCompanionAppConstants.UNKNOWN;
import static org.rm3l.router_companion.tiles.dashboard.network.NetworkTopologyMapTile.INTERNET_CONNECTIVITY_PUBLIC_IP;
import static org.rm3l.router_companion.utils.Utils.COMMA_SPLITTER;
import static org.rm3l.router_companion.utils.Utils.SPACE_SPLITTER;

/**
 * Created by rm3l on 08/01/2017.
 */

public class TomatoFirmwareConnector extends AbstractRouterFirmwareConnector {

    public static final String MODEL = "t_model_name";

    public static final String TOMATO_SCM_URL = "http://repo.or.cz/tomato.git";
    public static final String TOMATO_SCM_CHANGESET_URL_BASE = TOMATO_SCM_URL + "/tag/refs/tags/tomato-";

    public static final String TOMATO_WEBSITE = "http://polarcloud.com";
    public static final String TOMATO_CHANGELOG_BASE_URL = TOMATO_WEBSITE + "/tomato_";

    @Override
    public NVRAMInfo getDataForNetworkTopologyMapTile(@NonNull Context context,
                                                         @NonNull Router router,
                                                         @Nullable RemoteDataRetrievalListener dataRetrievalListener) throws Exception {
        return NetworkTopologyMapTileWorker.getDataForNetworkTopologyMapTile(context, router, dataRetrievalListener);
    }

    @Nullable
    @Override
    public String getWanPublicIpAddress(@NonNull Context context, @NonNull Router router, @Nullable RemoteDataRetrievalListener dataRetrievalListener) throws Exception {
        return SSHUtils.loadWanPublicIPFrom(context, router, null, dataRetrievalListener);
    }

    @Override
    @Nullable
    protected String goGetRouterModel(@NonNull Context context, @NonNull Router router)
            throws Exception {

        final String[] output = SSHUtils.getManualProperty(context, router,
                Utils.getGlobalSharedPreferences(context),
                String.format(
                        "/bin/nvram show 2>/dev/null | grep %s | awk -F'=' '{print $2}'",
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
        return null;
    }

    @Override
    protected NVRAMInfo getDataForUptimeTile(@NonNull Context context,
                                             @NonNull Router router,
                                             @Nullable RemoteDataRetrievalListener dataRetrievalListener) throws Exception {
        //Same implementation as in DD-WRT
        return RouterFirmwareConnectorManager.getConnector(Router.RouterFirmware.DDWRT)
                .getDataFor(context, router, UptimeTile.class, dataRetrievalListener);
    }

    @Override
    public List<String[]> getDataForMemoryAndCpuUsageTile(@NonNull Context context, @NonNull Router router, @Nullable RemoteDataRetrievalListener dataRetrievalListener) throws Exception {
        //Same implementation as in DD-WRT
        return RouterFirmwareConnectorManager.getConnector(Router.RouterFirmware.DDWRT)
                .getDataForMemoryAndCpuUsageTile(context, router, dataRetrievalListener);
    }

    @Override
    protected NVRAMInfo getDataForStorageUsageTile(@NonNull Context context, @NonNull Router router,
                                                   @Nullable RemoteDataRetrievalListener dataRetrievalListener) throws Exception {
        if (dataRetrievalListener != null) {
            dataRetrievalListener.onProgressUpdate(10);
        }

        final SharedPreferences globalSharedPreferences =
                Utils.getGlobalSharedPreferences(context);

        final String[] nvramSize = SSHUtils.getManualProperty(context,
                router,
                globalSharedPreferences,
                "/bin/nvram show | tail -n 1 | grep \"free.\"");
        if (dataRetrievalListener != null) {
            dataRetrievalListener.onProgressUpdate(20);
        }

        final String[] jffs2Size = SSHUtils.getManualProperty(context,
                router, globalSharedPreferences,
                "/bin/df -T | grep \"jffs2\"");
        if (dataRetrievalListener != null) {
            dataRetrievalListener.onProgressUpdate(30);
        }

        final String[] cifsSize = SSHUtils.getManualProperty(context,
                router,
                globalSharedPreferences,
                "/bin/df -T | grep \"cifs\"");

        return parseDataForStorageUsageTile(
                Arrays.asList(nvramSize, jffs2Size, cifsSize),
                dataRetrievalListener);
    }

    @Override
    protected NVRAMInfo getDataForStatusRouterStateTile(@NonNull Context context,
                                                        @NonNull Router router,
                                                        @Nullable RemoteDataRetrievalListener dataRetrievalListener)
            throws Exception {

        final SharedPreferences globalSharedPreferences =
                Utils.getGlobalSharedPreferences(context);

        updateProgressBarViewSeparator(dataRetrievalListener, 10);

        NVRAMInfo nvramInfo =
                SSHUtils.getNVRamInfoFromRouter(
                        context, router,
                        globalSharedPreferences,
                        NVRAMInfo.ROUTER_NAME,
                        NVRAMInfo.WAN_IPADDR,
                        MODEL,
                        NVRAMInfo.DIST_TYPE,
                        NVRAMInfo.LAN_IPADDR,
                        NVRAMInfo.OS_VERSION);

        if (nvramInfo == null) {
            nvramInfo = new NVRAMInfo();
        }

        final String modelPropertyValue = nvramInfo.getProperty(MODEL);
        if (modelPropertyValue != null) {
            nvramInfo.setProperty(NVRAMInfo.MODEL, modelPropertyValue);
        }

        updateProgressBarViewSeparator(dataRetrievalListener, 50);
        //date -d @$(( $(date +%s) - $(cut -f1 -d. /proc/uptime) ))
        //date -d @$(sed -n '/^btime /s///p' /proc/stat)
        //Add FW, Kernel and Uptime
        final String[] otherCmds = SSHUtils
                .getManualProperty(context, router, globalSharedPreferences,
                        //date
                        "date",
                        //date since last reboot
                        "date -d @$(( $(date +%s) - $(cut -f1 -d. /proc/uptime) )) || " +
                                " awk -vuptimediff=\"$(( $(date +%s) - $(cut -f1 -d. /proc/uptime) ))\" " +
                                " 'BEGIN { print strftime(\"%Y-%m-%d %H:%M:%S\", uptimediff); }' ",
                        //elapsed from current date
                        "uptime | awk -F'up' '{print $2}' | awk -F'users' '{print $1}' | awk -F'load' '{print $1}'",
                        "uname -a",
                        "cat /etc/motd 2>&1| tail -n 1");

        if (otherCmds != null) {
            if (otherCmds.length >= 1) {
                //date
                nvramInfo.setProperty(NVRAMInfo.CURRENT_DATE, otherCmds[0]);
            }
            if (otherCmds.length >= 3) {
                String uptime = otherCmds[1];
                final String uptimeCmd = otherCmds[2];
                if (!Strings.isNullOrEmpty(uptimeCmd)) {
                    final String elapsedFromUptime = Utils.removeLastChar(uptimeCmd.trim());
                    if (!Strings.isNullOrEmpty(elapsedFromUptime)) {
                        uptime += (" (up " + elapsedFromUptime + ")");
                    }
                }
                nvramInfo.setProperty(NVRAMInfo.UPTIME, uptime);
            }

            if (otherCmds.length >= 4) {
                //Kernel
                nvramInfo.setProperty(NVRAMInfo.KERNEL,
                        StringUtils.replace(
                                StringUtils.replace(otherCmds[3], "GNU/Linux", ""),
                                nvramInfo.getProperty(NVRAMInfo.ROUTER_NAME), ""));
            }

            if (otherCmds.length >= 5) {
                //Firmware
                final String fwString = otherCmds[4];
                nvramInfo.setProperty(NVRAMInfo.FIRMWARE, fwString);
            }
        }

        final SharedPreferences routerPreferences = router.getPreferences(context);
        final boolean checkActualInternetConnectivity = (routerPreferences == null || routerPreferences
                .getBoolean(RouterCompanionAppConstants.OVERVIEW_NTM_CHECK_ACTUAL_INTERNET_CONNECTIVITY_PREF, true));

        if (checkActualInternetConnectivity) {
            //Now get public IP Address
            updateProgressBarViewSeparator(dataRetrievalListener, 80);
            try {
                final String wanPublicIp = this.getWanPublicIpAddress(context, router, null);
                if (TextUtils.isEmpty(wanPublicIp)) {
                    nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, NOK);
                } else {
                    if (Patterns.IP_ADDRESS.matcher(wanPublicIp).matches()) {
                        nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, wanPublicIp);

                        PublicIPChangesServiceTask.buildNotificationIfNeeded(context,
                                router, routerPreferences,
                                new String[]{wanPublicIp},
                                nvramInfo.getProperty(NVRAMInfo.WAN_IPADDR), null);

                    } else {
                        nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, NOK);
                    }
                }
            } catch (final Exception e) {
                e.printStackTrace();
                nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, UNKNOWN);
            } finally {
                if (dataRetrievalListener != null) {
                    dataRetrievalListener.doRegardlessOfStatus();
                }
            }
        }

        return nvramInfo;
    }

    @Override
    public String getScmChangesetUrl(String changeset) {
        if (TextUtils.isEmpty(changeset)) {
            return null;
        }
        //Assume version format is always: x.y.z
        final List<String> stringList = Splitter.on(".")
                .omitEmptyStrings().trimResults()
                .splitToList(changeset);
        if (stringList.size() < 2) {
            return null;
        }
        return String.format("%s%s%s",
                TOMATO_CHANGELOG_BASE_URL,
                stringList.get(0),
                stringList.get(1));
    }

    private static NVRAMInfo parseDataForStorageUsageTile(@Nullable List<String[]> dataForStorageUsageTile,
                                                          @Nullable RemoteDataRetrievalListener dataRetrievalListener) {
        if (dataForStorageUsageTile == null || dataForStorageUsageTile.isEmpty()) {
            throw new DDWRTNoDataException();
        }

        final NVRAMInfo nvramInfo = new NVRAMInfo();

        final String[] nvramSize = dataForStorageUsageTile.get(0);

        final String[] jffs2Size;
        if (dataForStorageUsageTile.size() >= 2) {
            jffs2Size = dataForStorageUsageTile.get(1);
        } else {
            jffs2Size = null;
        }

        final String[] cifsSize;
        if (dataForStorageUsageTile.size() >= 3) {
            cifsSize = dataForStorageUsageTile.get(2);
        } else {
            cifsSize = null;
        }

        if (dataRetrievalListener != null) {
            dataRetrievalListener.onProgressUpdate(40);
        }

        if (nvramSize != null && nvramSize.length >= 1) {
            final String nvramSizeStr = nvramSize[0];
            if (nvramSizeStr != null && nvramSizeStr.endsWith("free.")) {
                final List<String> stringList = COMMA_SPLITTER.splitToList(nvramSizeStr);
                if (stringList.size() >= 3) {
                    final String nbEntriesStr = stringList.get(0).replace(" entries", "").trim();
                    final String nvramUsedBytesStr = stringList.get(1).replace(" bytes used", "").trim();
                    final String nvramFreeBytesStr = stringList.get(2).replace(" bytes free.", "").trim();

                    try {
                        final long nvramUsedBytesLong = Long.parseLong(nvramUsedBytesStr);
                        final long nvramFreeBytesLong = Long.parseLong(nvramFreeBytesStr);
                        final long nvramTotalBytesLong = nvramUsedBytesLong + nvramFreeBytesLong;
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
        if (dataRetrievalListener != null) {
            dataRetrievalListener.onProgressUpdate(50);
        }

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
                if (dataRetrievalListener != null) {
                    dataRetrievalListener.onProgressUpdate(Math.min(70, 50 + 5 * i));
                }
            }
            if (totalSize > 0) {
                nvramInfo.setProperty(NVRAMInfo.STORAGE_JFFS2_USED_PERCENT,
                        Long.toString(
                                Math.min(100, 100 * totalUsed / totalSize)
                        ));
            }
        }
        if (dataRetrievalListener != null) {
            dataRetrievalListener.onProgressUpdate(75);
        }

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
                if (dataRetrievalListener != null) {
                    dataRetrievalListener.onProgressUpdate(Math.min(87, 75 + 5 * i));
                }
            }
            if (totalSize > 0) {
                nvramInfo.setProperty(NVRAMInfo.STORAGE_CIFS_USED_PERCENT,
                        Long.toString(
                                Math.min(100, 100 * totalUsed / totalSize)
                        ));
            }
        }
        return nvramInfo;
    }
}
