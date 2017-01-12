package org.rm3l.router_companion.firmwares.impl.tomato;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Strings;

import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.firmwares.AbstractRouterFirmwareConnector;
import org.rm3l.router_companion.firmwares.RemoteDataRetrievalListener;
import org.rm3l.router_companion.firmwares.RouterFirmwareConnectorManager;
import org.rm3l.router_companion.firmwares.impl.ddwrt.DDWRTFirmwareConnector;
import org.rm3l.router_companion.firmwares.impl.tomato.tile_data_workers.dashboard.network.NetworkTopologyMapTileWorker;
import org.rm3l.router_companion.resources.MonthlyCycleItem;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.dashboard.system.UptimeTile;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;

import java.util.Arrays;
import java.util.List;

import static org.rm3l.router_companion.utils.Utils.COMMA_SPLITTER;
import static org.rm3l.router_companion.utils.Utils.SPACE_SPLITTER;

/**
 * Created by rm3l on 08/01/2017.
 */

public class TomatoFirmwareConnector extends AbstractRouterFirmwareConnector {

    public static final String MODEL = "t_model_name";

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
