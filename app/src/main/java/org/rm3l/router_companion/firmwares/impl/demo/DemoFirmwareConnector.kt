package org.rm3l.router_companion.firmwares.impl.demo

import android.content.Context
import com.google.common.base.Splitter
import com.google.common.base.Strings
import org.rm3l.router_companion.RouterCompanionAppConstants
import org.rm3l.router_companion.RouterCompanionAppConstants.MB
import org.rm3l.router_companion.RouterCompanionAppConstants.NOK
import org.rm3l.router_companion.RouterCompanionAppConstants.UNKNOWN
import org.rm3l.router_companion.firmwares.AbstractRouterFirmwareConnector
import org.rm3l.router_companion.firmwares.FirmwareRelease
import org.rm3l.router_companion.firmwares.RemoteDataRetrievalListener
import org.rm3l.router_companion.firmwares.RouterFirmwareConnectorManager
import org.rm3l.router_companion.firmwares.impl.ddwrt.DDWRTFirmwareConnector
import org.rm3l.router_companion.resources.MonthlyCycleItem
import org.rm3l.router_companion.resources.WANAccessPolicy
import org.rm3l.router_companion.resources.conn.NVRAMInfo
import org.rm3l.router_companion.resources.conn.Router
import org.rm3l.router_companion.resources.conn.Router.RouterFirmware
import org.rm3l.router_companion.tiles.admin.accessrestrictions.WANAccessPoliciesRouterData
import org.rm3l.router_companion.tiles.dashboard.network.NetworkTopologyMapTile.INTERNET_CONNECTIVITY_PUBLIC_IP
import org.rm3l.router_companion.utils.Utils
import org.rm3l.router_companion.utils.WANTrafficUtils.HIDDEN_
import org.rm3l.router_companion.utils.WANTrafficUtils.TOTAL_DL_CURRENT_MONTH
import org.rm3l.router_companion.utils.WANTrafficUtils.TOTAL_DL_CURRENT_MONTH_MB
import org.rm3l.router_companion.utils.WANTrafficUtils.TOTAL_UL_CURRENT_MONTH
import org.rm3l.router_companion.utils.WANTrafficUtils.TOTAL_UL_CURRENT_MONTH_MB
import java.util.ArrayList
import java.util.Arrays
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.Random
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Created by rm3l on 08/01/2017.
 */

class DemoFirmwareConnector : AbstractRouterFirmwareConnector() {

    private val nbRunsStatusRouterStateTile = AtomicInteger(0)
    private val mGetWanPublicIpAddressCalls = AtomicLong(0)

    @Throws(Exception::class)
    override fun getDataForNetworkTopologyMapTile(context: Context,
                                                  router: Router, dataRetrievalListener: RemoteDataRetrievalListener?): NVRAMInfo {
        try {
            dataRetrievalListener?.onProgressUpdate(10)
            return NVRAMInfo().setProperty(NVRAMInfo.ROUTER_NAME, "Demo Router (Test Data)")
                    .setProperty(NVRAMInfo.WAN_IPADDR, "1.2.3.4")
                    .setProperty(NVRAMInfo.LAN_IPADDR, "192.168.78.254")
                    .setProperty(NVRAMInfo.OPENVPNCL_ENABLE, Integer.toString(Random().nextInt(2)))
                    .setProperty(NVRAMInfo.OPENVPNCL_REMOTEIP, "my.remote.vpn.servi.ce")
                    .setProperty(NVRAMInfo.OPENVPNCL_REMOTEPORT, "1234")
                    .setProperty(NVRAMInfo.NB_ACTIVE_CLIENTS, Integer.toString(Random().nextInt(20)))
                    .setProperty(NVRAMInfo.NB_DHCP_LEASES, Integer.toString(Random().nextInt(30)))
                    .setProperty(NVRAMInfo.INTERNET_CONNECTIVITY_PUBLIC_IP, "52."
                            + (1 + Random().nextInt(252))
                            + "."
                            + (1 + Random().nextInt(252))
                            + "."
                            + (1 + Random().nextInt(252)))
        } finally {
            dataRetrievalListener?.doRegardlessOfStatus()
        }
    }

    @Throws(Exception::class)
    override fun getWanPublicIpAddress(context: Context, router: Router,
                                       dataRetrievalListener: RemoteDataRetrievalListener?): String? {

        val nbRuns = mGetWanPublicIpAddressCalls.getAndIncrement()

        dataRetrievalListener?.onProgressUpdate(10)

        val a = 1 + Random().nextInt(252)
        val b = 1 + Random().nextInt(252)
        val c = 1 + Random().nextInt(252)
        val d = 1 + Random().nextInt(252)

        val modulo = (nbRuns % 7).toInt()

        return if (modulo == 0) {
            "52.64.$c.$d"
        } else if (modulo == 1) {
            "8.8.$c.$d"
        } else if (modulo == 2) {
            "78.87.$c.$d"
        } else if (modulo == 3) {
            "34.56.$c.$d"
        } else if (modulo == 4) {
            "67.78.$c.$d"
        } else if (modulo == 5) {
            a.toString() + "." + b + "." + c + "." + d
        } else {
            null
        }
    }

    override fun getRouterName(context: Context, router: Router) = ROUTER_NAME
    override fun getWanIpAddress(context: Context, router: Router) = WAN_IPADDR
    override fun getLanIpAddress(context: Context, router: Router) = LAN_IPADDR

    @Throws(Exception::class)
    override fun goGetRouterModel(context: Context, router: Router): String? {
        return "Demo Router Model"
    }

    @Throws(Exception::class)
    override fun getDataForWANTotalTrafficOverviewTile(context: Context,
                                                       router: Router, cycleItem: MonthlyCycleItem,
                                                       dataRetrievalListener: RemoteDataRetrievalListener?): NVRAMInfo {
        dataRetrievalListener?.onProgressUpdate(50)

        val nvramInfo = NVRAMInfo().setProperty(NVRAMInfo.TTRAFF_ENABLE,
                if (Random().nextBoolean()) "1" else "0")

        val random = Random()
        val totalDlMonth = ((500 + MB * random.nextInt(500)) * MB).toLong()

        nvramInfo.setProperty(TOTAL_DL_CURRENT_MONTH,
                org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(totalDlMonth))
        nvramInfo.setProperty(TOTAL_DL_CURRENT_MONTH_MB, HIDDEN_)

        val totalUlMonth = ((1 + random.nextInt(100)) * MB).toLong()
        nvramInfo.setProperty(TOTAL_UL_CURRENT_MONTH,
                org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(totalUlMonth))
        nvramInfo.setProperty(TOTAL_UL_CURRENT_MONTH_MB, HIDDEN_)

        nvramInfo.setProperty(NVRAMInfo.TOTAL_DL_CURRENT_DAY,
                org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(totalDlMonth / 30))
        nvramInfo.setProperty(NVRAMInfo.TOTAL_DL_CURRENT_DAY_MB, HIDDEN_)

        nvramInfo.setProperty(NVRAMInfo.TOTAL_UL_CURRENT_DAY,
                org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(totalUlMonth / 30))
        nvramInfo.setProperty(NVRAMInfo.TOTAL_UL_CURRENT_DAY_MB, HIDDEN_)

        return nvramInfo
    }

    @Throws(Exception::class)
    override fun getDataForUptimeTile(context: Context, router: Router,
                                      dataRetrievalListener: RemoteDataRetrievalListener?): NVRAMInfo {

        dataRetrievalListener?.onProgressUpdate(10)

        val random = Random()
        val days = Integer.toString(random.nextInt(60))
        val hours = Integer.toString(random.nextInt(23))
        val minutes = Integer.toString(random.nextInt(59))
        return NVRAMInfo().setProperty(NVRAMInfo.UPTIME, "22:31:45 up "
                + days
                + " days, "
                + hours
                + ":"
                + minutes
                + ", load average: 0.11, 0.10, 0.09")
                .setProperty(NVRAMInfo.UPTIME_DAYS, days)
                .setProperty(NVRAMInfo.UPTIME_HOURS, hours)
                .setProperty(NVRAMInfo.UPTIME_MINUTES, minutes)
    }

    @Throws(Exception::class)
    override fun getDataForMemoryAndCpuUsageTile(context: Context,
                                                 router: Router, dataRetrievalListener: RemoteDataRetrievalListener?): List<Array<String>> {
        val memTotal = 4096
        val memFree = Random().nextInt(memTotal + 1)
        val memUsageData = Array(3, { i ->
            when (i) {
                0 -> "$memTotal kB" //MemTotal
                1 -> "$memFree kB" //MemFree
                2 -> "${Random().nextInt(100)}"
                else -> ""
            }
        })
        val cpuUsageData = Array(2, { i -> if (i == 1) " 0.14, 0.24, 0.28" else "1" })

        return Arrays.asList<Array<String>>(memUsageData, cpuUsageData)
    }

    @Throws(Exception::class)
    override fun getDataForStorageUsageTile(context: Context, router: Router,
                                            dataRetrievalListener: RemoteDataRetrievalListener?): NVRAMInfo {
        val random = Random()
        val totalSize = 44379 + random.nextInt(44379)

        val nvramSize = Array(1, { _ -> "size: $totalSize bytes ${random.nextInt(totalSize)} left)" })
        val jffs2Size = Array(1, { _ -> "/dev/mtdblock/5      jffs2          $totalSize      ${random.nextInt(totalSize)}     120000   30% /jffs" })
        val cifsSize = Array(1, { _ -> "/dev/mtdblock/5      cifs          $totalSize      ${random.nextInt(totalSize)}     91300   50% /cifs" })

        return DDWRTFirmwareConnector.parseDataForStorageUsageTile(
                Arrays.asList<Array<String>>(nvramSize, jffs2Size, cifsSize), dataRetrievalListener)
    }

    @Throws(Exception::class)
    override fun getDataForStatusRouterStateTile(context: Context, router: Router,
                                                 dataRetrievalListener: RemoteDataRetrievalListener?): NVRAMInfo {

        val nvramInfo = NVRAMInfo().setProperty(NVRAMInfo.ROUTER_NAME, ROUTER_NAME)
                .setProperty(NVRAMInfo.WAN_IPADDR, WAN_IPADDR)
                .setProperty(NVRAMInfo.MODEL, MODEL)
                .setProperty(NVRAMInfo.DIST_TYPE, DIST_TYPE)
                .setProperty(NVRAMInfo.LAN_IPADDR, LAN_IPADDR)
                .setProperty(NVRAMInfo.OS_VERSION, OS_VERSION)

        updateProgressBarViewSeparator(dataRetrievalListener, 50)

        val date = Date()
        val cal = Calendar.getInstance()
        cal.time = date
        cal.add(Calendar.DATE, -77)
        val otherCmds = Array(5,
                { i ->
                    when (i) {
                        0 -> date.toString() //current date
                        1 -> cal.time.toString() //date since last reboot
                        2 -> " 77 days, 11:00,  2 users, "
                        3 -> "Linux 2.4.37 #7583 Sat Oct 10 mips"
                        4 -> "DD-WRT v24-sp2 (10/10/09) - rev 7583"
                        else -> ""
                    }
                })

        //date
        nvramInfo.setProperty(NVRAMInfo.CURRENT_DATE, otherCmds[0])

        //uptime
        var uptime = otherCmds[1]
        val uptimeCmd = otherCmds[2]
        if (!Strings.isNullOrEmpty(uptimeCmd)) {
            val elapsedFromUptime = Utils.removeLastChar(uptimeCmd.trim { it <= ' ' })
            if (!Strings.isNullOrEmpty(elapsedFromUptime)) {
                uptime += " (up $elapsedFromUptime)"
            }
        }
        nvramInfo.setProperty(NVRAMInfo.UPTIME, uptime)

        //Kernel
        otherCmds[3].let {
            val valueWithoutGnuLinux = it.replace("GNU/Linux", "")
            nvramInfo.setProperty(NVRAMInfo.KERNEL,
                    valueWithoutGnuLinux.replace(nvramInfo.getProperty(NVRAMInfo.ROUTER_NAME) ?: "", ""))
        }
//    nvramInfo.setProperty(NVRAMInfo.KERNEL,
//        StringUtils.replace(StringUtils.replace(otherCmds[3], "GNU/Linux", ""),
//            nvramInfo.getProperty(NVRAMInfo.ROUTER_NAME), ""))

        //Firmware
        val fwString = otherCmds[4]
        nvramInfo.setProperty(NVRAMInfo.FIRMWARE, fwString)

        val strings = Splitter.on("rev:").omitEmptyStrings().trimResults().splitToList(fwString)
        if (strings.size >= 2) {
            try {
                nvramInfo.setProperty(NVRAMInfo.OS_VERSION,
                        java.lang.Long.toString(java.lang.Long.parseLong(strings[1].trim { it <= ' ' })))
            } catch (nfe: NumberFormatException) {
                //No worries
            }

        }

        updateProgressBarViewSeparator(dataRetrievalListener, 75)

        val nbRunsLoaderModulo = (nbRunsStatusRouterStateTile.getAndIncrement() % 5).toInt()
        if (nbRunsLoaderModulo == 0) {
            //nbRunsLoader = 5k
            nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP,
                    "52.64." + (1 + Random().nextInt(252)) + "." + (1 + Random().nextInt(252)))
        } else if (nbRunsLoaderModulo == 1) {
            //nbRunsLoader = 5k + 1
            nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, NOK)
        } else if (nbRunsLoaderModulo == 2) {
            //nbRunsLoader = 5k + 2
            nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, UNKNOWN)
        }

        return nvramInfo
    }

    override fun getScmChangesetUrl(changeset: String): String? {
        val valuesAsList = ArrayList<RouterFirmware>(Router.RouterFirmware.getValuesAsList())
        //Take any connector manager on file
        Collections.shuffle(valuesAsList)
        for (routerFirmware in valuesAsList) {
            if (Router.RouterFirmware.DEMO == routerFirmware) {
                //Avoid infinite recursion
                continue
            }
            val routerFirmwareConnector: AbstractRouterFirmwareConnector
            try {
                routerFirmwareConnector = RouterFirmwareConnectorManager.getConnector(routerFirmware)
            } catch (e: Exception) {
                //ignored, e.g., if router firmware is not supported yet
                continue
            }

            return routerFirmwareConnector.getScmChangesetUrl(changeset)
        }
        return null
    }

    override fun getWANAccessPolicies(context: Context, router: Router,
                                      dataRetrievalListener: RemoteDataRetrievalListener?): WANAccessPoliciesRouterData? {
        val wanAccessPolicies = ArrayList<WANAccessPolicy>()
        (1..10).mapTo(wanAccessPolicies) {
            val wanAccessPolicy = WANAccessPolicy()
            wanAccessPolicy.number = it
            wanAccessPolicy.name = "myWanPolicy $it"
            wanAccessPolicy
            //TODO Add other properties here
        }
        val routerData = WANAccessPoliciesRouterData().setData(
                wanAccessPolicies) as WANAccessPoliciesRouterData
        dataRetrievalListener?.onProgressUpdate(90)
        return routerData
    }

    override fun manuallyCheckForFirmwareUpdateAndReturnDownloadLink(currentFwVer: String?): FirmwareRelease? {
        val index = Random().nextInt(11)
        return when (index) {
            4 -> throw UnsupportedOperationException("not implemented yet") //not implemented
            2 -> throw IllegalStateException("Error simulated!")
            1, 3, 5, 6, 7, 8 -> DemoFirmwareRelease(index)
            else -> null //no update
        }
    }

    companion object {
        private val ROUTER_NAME = "Demo Router (Test Data)"
        private val WAN_IPADDR = "1.2.3.4"
        private val MODEL = "Router Model Family"
        private val DIST_TYPE = "Linux 2.4.37 #7583 Sat Oct 10 mips"
        private val LAN_IPADDR = "192.168.77.254"
        private val OS_VERSION = Integer.toString(1 + Random().nextInt(65535))
    }
}

data class DemoFirmwareRelease(val index: Int) : FirmwareRelease(index.toString()) {
    override fun getDirectLink(): String {
        return when (index) {
            1, 3, 5, 8 -> RouterCompanionAppConstants.DDWRT_RELEASE_BASE_URL
            6, 7 -> "http://ddwrt-companion.rm3l.org"
            else -> "N/A"
        }
    }

}
