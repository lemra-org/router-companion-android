package org.rm3l.router_companion.firmwares.impl.demo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang3.StringUtils;
import org.rm3l.router_companion.firmwares.AbstractRouterFirmwareConnector;
import org.rm3l.router_companion.firmwares.RemoteDataRetrievalListener;
import org.rm3l.router_companion.firmwares.RouterFirmwareConnectorManager;
import org.rm3l.router_companion.firmwares.impl.ddwrt.DDWRTFirmwareConnector;
import org.rm3l.router_companion.resources.MonthlyCycleItem;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.Utils;

import static org.rm3l.router_companion.RouterCompanionAppConstants.MB;
import static org.rm3l.router_companion.RouterCompanionAppConstants.NOK;
import static org.rm3l.router_companion.RouterCompanionAppConstants.UNKNOWN;
import static org.rm3l.router_companion.tiles.dashboard.network.NetworkTopologyMapTile.INTERNET_CONNECTIVITY_PUBLIC_IP;
import static org.rm3l.router_companion.utils.WANTrafficUtils.HIDDEN_;
import static org.rm3l.router_companion.utils.WANTrafficUtils.TOTAL_DL_CURRENT_MONTH;
import static org.rm3l.router_companion.utils.WANTrafficUtils.TOTAL_DL_CURRENT_MONTH_MB;
import static org.rm3l.router_companion.utils.WANTrafficUtils.TOTAL_UL_CURRENT_MONTH;
import static org.rm3l.router_companion.utils.WANTrafficUtils.TOTAL_UL_CURRENT_MONTH_MB;

/**
 * Created by rm3l on 08/01/2017.
 */

public class DemoFirmwareConnector extends AbstractRouterFirmwareConnector {

  final AtomicInteger nbRunsStatusRouterStateTile = new AtomicInteger(0);
  private final AtomicLong mGetWanPublicIpAddressCalls = new AtomicLong(0);

  @Override public NVRAMInfo getDataForNetworkTopologyMapTile(@NonNull Context context,
      @NonNull Router router, @Nullable RemoteDataRetrievalListener dataRetrievalListener)
      throws Exception {
    try {
      if (dataRetrievalListener != null) {
        dataRetrievalListener.onProgressUpdate(10);
      }
      return new NVRAMInfo().setProperty(NVRAMInfo.Companion.getROUTER_NAME(), "Demo Router (Test Data)")
          .setProperty(NVRAMInfo.Companion.getWAN_IPADDR(), "1.2.3.4")
          .setProperty(NVRAMInfo.Companion.getLAN_IPADDR(), "192.168.78.254")
          .setProperty(NVRAMInfo.Companion.getOPENVPNCL_ENABLE(), Integer.toString(new Random().nextInt(2)))
          .setProperty(NVRAMInfo.Companion.getOPENVPNCL_REMOTEIP(), "my.remote.vpn.servi.ce")
          .setProperty(NVRAMInfo.Companion.getOPENVPNCL_REMOTEPORT(), "1234")
          .setProperty(NVRAMInfo.Companion.getNB_ACTIVE_CLIENTS(), Integer.toString(new Random().nextInt(20)))
          .setProperty(NVRAMInfo.Companion.getNB_DHCP_LEASES(), Integer.toString(new Random().nextInt(30)))
          .setProperty(NVRAMInfo.Companion.getINTERNET_CONNECTIVITY_PUBLIC_IP(), "52."
              + (1 + new Random().nextInt(252))
              + "."
              + (1 + new Random().nextInt(252))
              + "."
              + (1 + new Random().nextInt(252)));
    } finally {
      if (dataRetrievalListener != null) {
        dataRetrievalListener.doRegardlessOfStatus();
      }
    }
  }

  @Nullable @Override
  public String getWanPublicIpAddress(@NonNull Context context, @NonNull Router router,
      @Nullable RemoteDataRetrievalListener dataRetrievalListener) throws Exception {

    final long nbRuns = mGetWanPublicIpAddressCalls.getAndIncrement();

    if (dataRetrievalListener != null) {
      dataRetrievalListener.onProgressUpdate(10);
    }

    final int a = 1 + new Random().nextInt(252);
    final int b = 1 + new Random().nextInt(252);
    final int c = 1 + new Random().nextInt(252);
    final int d = 1 + new Random().nextInt(252);

    final long modulo = nbRuns % 7;
    final String mWanPublicIP;

    if (modulo == 0) {
      mWanPublicIP = ("52.64." + c + "." + d);
    } else if (modulo == 1) {
      mWanPublicIP = ("8.8." + c + "." + d);
    } else if (modulo == 2) {
      mWanPublicIP = ("78.87." + c + "." + d);
    } else if (modulo == 3) {
      mWanPublicIP = ("34.56." + c + "." + d);
    } else if (modulo == 4) {
      mWanPublicIP = ("67.78." + c + "." + d);
    } else if (modulo == 5) {
      mWanPublicIP = (a + "." + b + "." + c + "." + d);
    } else {
      mWanPublicIP = null;
    }
    return mWanPublicIP;
  }

  @Override @Nullable
  public String goGetRouterModel(@NonNull Context context, @NonNull Router router)
      throws Exception {
    return "Demo Router Model";
  }

  @Override public NVRAMInfo getDataForWANTotalTrafficOverviewTile(@NonNull Context context,
      @NonNull Router router, MonthlyCycleItem cycleItem,
      @Nullable RemoteDataRetrievalListener dataRetrievalListener) throws Exception {
    if (dataRetrievalListener != null) {
      dataRetrievalListener.onProgressUpdate(50);
    }

    final NVRAMInfo nvramInfo = new NVRAMInfo().setProperty(NVRAMInfo.Companion.getTTRAFF_ENABLE(),
        new Random().nextBoolean() ? "1" : "0");

    final Random random = new Random();
    final long totalDlMonth = (500 + MB * random.nextInt(500)) * MB;

    nvramInfo.setProperty(TOTAL_DL_CURRENT_MONTH,
        org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(totalDlMonth));
    nvramInfo.setProperty(TOTAL_DL_CURRENT_MONTH_MB, HIDDEN_);

    final long totalUlMonth = (1 + random.nextInt(100)) * MB;
    nvramInfo.setProperty(TOTAL_UL_CURRENT_MONTH,
        org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(totalUlMonth));
    nvramInfo.setProperty(TOTAL_UL_CURRENT_MONTH_MB, HIDDEN_);

    nvramInfo.setProperty(NVRAMInfo.Companion.getTOTAL_DL_CURRENT_DAY(),
        org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(totalDlMonth / 30));
    nvramInfo.setProperty(NVRAMInfo.Companion.getTOTAL_DL_CURRENT_DAY_MB(), HIDDEN_);

    nvramInfo.setProperty(NVRAMInfo.Companion.getTOTAL_UL_CURRENT_DAY(),
        org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(totalUlMonth / 30));
    nvramInfo.setProperty(NVRAMInfo.Companion.getTOTAL_UL_CURRENT_DAY_MB(), HIDDEN_);

    return nvramInfo;
  }

  @Override public NVRAMInfo getDataForUptimeTile(@NonNull Context context, @NonNull Router router,
      @Nullable RemoteDataRetrievalListener dataRetrievalListener) throws Exception {

    if (dataRetrievalListener != null) {
      dataRetrievalListener.onProgressUpdate(10);
    }

    final Random random = new Random();
    final String days = Integer.toString(random.nextInt(60));
    final String hours = Integer.toString(random.nextInt(23));
    final String minutes = Integer.toString(random.nextInt(59));
    return new NVRAMInfo().setProperty(NVRAMInfo.Companion.getUPTIME(), "22:31:45 up "
        + days
        + " days, "
        + hours
        + ":"
        + minutes
        + ", load average: 0.11, 0.10, 0.09")
        .setProperty(NVRAMInfo.Companion.getUPTIME_DAYS(), days)
        .setProperty(NVRAMInfo.Companion.getUPTIME_HOURS(), hours)
        .setProperty(NVRAMInfo.Companion.getUPTIME_MINUTES(), minutes);
  }

  @Override public List<String[]> getDataForMemoryAndCpuUsageTile(@NonNull Context context,
      @NonNull Router router, @Nullable RemoteDataRetrievalListener dataRetrievalListener)
      throws Exception {
    final String[] memUsageData = new String[3];
    final int memTotal = 4096;
    final int memFree = new Random().nextInt(memTotal + 1);
    memUsageData[0] = (memTotal + " kB"); //MemTotal
    memUsageData[1] = (memFree + " kB"); //MemFree
    memUsageData[2] = Integer.toString(new Random().nextInt(100)); //CPU Usage

    final String[] cpuUsageData = new String[2];
    cpuUsageData[0] = " 0.14, 0.24, 0.28";
    cpuUsageData[1] = "1";

    return Arrays.asList(memUsageData, cpuUsageData);
  }

  @Override
  public NVRAMInfo getDataForStorageUsageTile(@NonNull Context context, @NonNull Router router,
      @Nullable RemoteDataRetrievalListener dataRetrievalListener) throws Exception {
    final String[] nvramSize = new String[1];
    final Random random = new Random();

    final int totalSize = 44379 + random.nextInt(44379);

    nvramSize[0] = "size: " + totalSize + " bytes (" + random.nextInt(totalSize) + " left)";

    final String[] jffs2Size = new String[1];
    jffs2Size[0] = "/dev/mtdblock/5      jffs2          "
        + totalSize
        + "      "
        + random.nextInt(totalSize)
        + "     120000   30% /jffs";

    final String[] cifsSize = new String[1];
    cifsSize[0] = "/dev/mtdblock/5      cifs          "
        + totalSize
        + "      "
        + random.nextInt(totalSize)
        + "     91300   50% /cifs";

    return DDWRTFirmwareConnector.parseDataForStorageUsageTile(
        Arrays.asList(nvramSize, jffs2Size, cifsSize), dataRetrievalListener);
  }

  @Override
  public NVRAMInfo getDataForStatusRouterStateTile(@NonNull Context context, @NonNull Router router,
      @Nullable RemoteDataRetrievalListener dataRetrievalListener) throws Exception {

    final NVRAMInfo nvramInfo =
        new NVRAMInfo().setProperty(NVRAMInfo.Companion.getROUTER_NAME(), "Demo Router (Test Data)")
            .setProperty(NVRAMInfo.Companion.getWAN_IPADDR(), "1.2.3.4")
            .setProperty(NVRAMInfo.Companion.getMODEL(), "Router Model Family")
            .setProperty(NVRAMInfo.Companion.getDIST_TYPE(), "Linux 2.4.37 #7583 Sat Oct 10 mips")
            .setProperty(NVRAMInfo.Companion.getLAN_IPADDR(), "255.255.255.255")
            .setProperty(NVRAMInfo.Companion.getOS_VERSION(), Integer.toString(1 + new Random().nextInt(65535)));

    updateProgressBarViewSeparator(dataRetrievalListener, 50);

    final String[] otherCmds = new String[5];
    final Date date = new Date();
    otherCmds[0] = date.toString(); //current date
    final Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    cal.add(Calendar.DATE, -77);
    otherCmds[1] = cal.getTime().toString(); //date since last reboot
    otherCmds[2] = " 77 days, 11:00,  2 users, ";
    otherCmds[3] = "Linux 2.4.37 #7583 Sat Oct 10 mips";
    otherCmds[4] = "DD-WRT v24-sp2 (10/10/09) - rev 7583";

    //date
    nvramInfo.setProperty(NVRAMInfo.Companion.getCURRENT_DATE(), otherCmds[0]);

    //uptime
    String uptime = otherCmds[1];
    final String uptimeCmd = otherCmds[2];
    if (!Strings.isNullOrEmpty(uptimeCmd)) {
      final String elapsedFromUptime = Utils.removeLastChar(uptimeCmd.trim());
      if (!Strings.isNullOrEmpty(elapsedFromUptime)) {
        uptime += (" (up " + elapsedFromUptime + ")");
      }
    }
    nvramInfo.setProperty(NVRAMInfo.Companion.getUPTIME(), uptime);

    //Kernel
    nvramInfo.setProperty(NVRAMInfo.Companion.getKERNEL(),
        StringUtils.replace(StringUtils.replace(otherCmds[3], "GNU/Linux", ""),
            nvramInfo.getProperty(NVRAMInfo.Companion.getROUTER_NAME()), ""));

    //Firmware
    final String fwString = otherCmds[4];
    nvramInfo.setProperty(NVRAMInfo.Companion.getFIRMWARE(), fwString);

    final List<String> strings =
        Splitter.on("rev:").omitEmptyStrings().trimResults().splitToList(fwString);
    if (strings.size() >= 2) {
      try {
        nvramInfo.setProperty(NVRAMInfo.Companion.getOS_VERSION(),
            Long.toString(Long.parseLong(strings.get(1).trim())));
      } catch (final NumberFormatException nfe) {
        //No worries
      }
    }

    updateProgressBarViewSeparator(dataRetrievalListener, 75);

    final long nbRunsLoaderModulo = (nbRunsStatusRouterStateTile.getAndIncrement() % 5);
    if (nbRunsLoaderModulo == 0) {
      //nbRunsLoader = 5k
      nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP,
          "52.64." + (1 + new Random().nextInt(252)) + "." + (1 + new Random().nextInt(252)));
    } else if (nbRunsLoaderModulo == 1) {
      //nbRunsLoader = 5k + 1
      nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, NOK);
    } else if (nbRunsLoaderModulo == 2) {
      //nbRunsLoader = 5k + 2
      nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, UNKNOWN);
    }

    return nvramInfo;
  }

  @Override public String getScmChangesetUrl(String changeset) {
    final List<Router.RouterFirmware> valuesAsList =
        new ArrayList<>(Router.RouterFirmware.getValuesAsList());
    //Take any connector manager on file
    Collections.shuffle(valuesAsList);
    for (final Router.RouterFirmware routerFirmware : valuesAsList) {
      if (routerFirmware == null || Router.RouterFirmware.DEMO.equals(routerFirmware)) {
        //Avoid infinite recursion
        continue;
      }
      final AbstractRouterFirmwareConnector routerFirmwareConnector;
      try {
        routerFirmwareConnector = RouterFirmwareConnectorManager.getConnector(routerFirmware);
      } catch (final Exception e) {
        //ignored, e.g., if router firmware is not supported yet
        continue;
      }
      return routerFirmwareConnector.getScmChangesetUrl(changeset);
    }
    return null;
  }
}
