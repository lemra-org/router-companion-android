package org.rm3l.router_companion.firmwares.impl.tomato

import android.content.Context
import android.text.TextUtils
import android.util.Patterns
import com.crashlytics.android.Crashlytics
import com.google.common.base.Splitter
import com.google.common.base.Strings
import org.rm3l.router_companion.RouterCompanionAppConstants
import org.rm3l.router_companion.RouterCompanionAppConstants.NOK
import org.rm3l.router_companion.RouterCompanionAppConstants.UNKNOWN
import org.rm3l.router_companion.exceptions.DDWRTNoDataException
import org.rm3l.router_companion.firmwares.AbstractRouterFirmwareConnector
import org.rm3l.router_companion.firmwares.FirmwareRelease
import org.rm3l.router_companion.firmwares.RemoteDataRetrievalListener
import org.rm3l.router_companion.firmwares.RouterFirmwareConnectorManager
import org.rm3l.router_companion.firmwares.impl.tomato.tile_data_workers.dashboard.network.NetworkTopologyMapTileWorker
import org.rm3l.router_companion.resources.MonthlyCycleItem
import org.rm3l.router_companion.resources.conn.NVRAMInfo
import org.rm3l.router_companion.resources.conn.Router
import org.rm3l.router_companion.service.tasks.PublicIPChangesServiceTask
import org.rm3l.router_companion.tiles.admin.accessrestrictions.WANAccessPoliciesRouterData
import org.rm3l.router_companion.tiles.dashboard.network.NetworkTopologyMapTile.INTERNET_CONNECTIVITY_PUBLIC_IP
import org.rm3l.router_companion.tiles.dashboard.system.UptimeTile
import org.rm3l.router_companion.utils.SSHUtils
import org.rm3l.router_companion.utils.Utils
import org.rm3l.router_companion.utils.Utils.COMMA_SPLITTER
import org.rm3l.router_companion.utils.Utils.SPACE_SPLITTER
import java.util.Arrays

/**
 * Created by rm3l on 08/01/2017.
 */

class TomatoFirmwareConnector : AbstractRouterFirmwareConnector() {

    @Throws(Exception::class)
    override fun getDataForNetworkTopologyMapTile(context: Context,
                                                  router: Router, dataRetrievalListener: RemoteDataRetrievalListener?): NVRAMInfo {
        return NetworkTopologyMapTileWorker.getDataForNetworkTopologyMapTile(context, router,
                dataRetrievalListener)
    }

    @Throws(Exception::class)
    override fun getWanPublicIpAddress(context: Context, router: Router,
                                       dataRetrievalListener: RemoteDataRetrievalListener?): String? {
        return SSHUtils.loadWanPublicIPFrom(context, router, null, dataRetrievalListener)
    }

    @Throws(Exception::class)
    override fun goGetRouterModel(context: Context, router: Router): String? {

        val output = SSHUtils.getManualProperty(context, router,
                Utils.getGlobalSharedPreferences(context),
                String.format("/bin/nvram show 2>/dev/null | grep %s | awk -F'=' '{print $2}'", MODEL))
        if (output != null && output.size > 0) {
            return output[0]
        }
        return null
    }

    @Throws(Exception::class)
    override fun getDataForWANTotalTrafficOverviewTile(context: Context,
                                                       router: Router, cycleItem: MonthlyCycleItem,
                                                       dataRetrievalListener: RemoteDataRetrievalListener?): NVRAMInfo {
        return NVRAMInfo()
    }

    @Throws(Exception::class)
    override fun getDataForUptimeTile(context: Context, router: Router,
                                      dataRetrievalListener: RemoteDataRetrievalListener?): NVRAMInfo {
        //Same implementation as in DD-WRT
        return RouterFirmwareConnectorManager.getConnector(Router.RouterFirmware.DDWRT)
                .getDataFor(context, router, UptimeTile::class.java, dataRetrievalListener)
    }

    @Throws(Exception::class)
    override fun getDataForMemoryAndCpuUsageTile(context: Context,
                                                 router: Router, dataRetrievalListener: RemoteDataRetrievalListener?): List<Array<String>> {
        //Same implementation as in DD-WRT
        return RouterFirmwareConnectorManager.getConnector(Router.RouterFirmware.DDWRT)
                .getDataForMemoryAndCpuUsageTile(context, router, dataRetrievalListener)
    }

    @Throws(Exception::class)
    override fun getDataForStorageUsageTile(context: Context, router: Router,
                                            dataRetrievalListener: RemoteDataRetrievalListener?): NVRAMInfo {
        dataRetrievalListener?.onProgressUpdate(10)

        val globalSharedPreferences = Utils.getGlobalSharedPreferences(context)

        val nvramSize = SSHUtils.getManualProperty(context, router, globalSharedPreferences,
                "/bin/nvram show | tail -n 1 | grep \"free.\"")
        dataRetrievalListener?.onProgressUpdate(20)

        val jffs2Size = SSHUtils.getManualProperty(context, router, globalSharedPreferences,
                "/bin/df -T | grep \"jffs2\"")
        dataRetrievalListener?.onProgressUpdate(30)

        val cifsSize = SSHUtils.getManualProperty(context, router, globalSharedPreferences,
                "/bin/df -T | grep \"cifs\"")

        return parseDataForStorageUsageTile(
                Arrays.asList<Array<String>>(nvramSize, jffs2Size, cifsSize),
                dataRetrievalListener)
    }

    override fun getRouterName(context: Context, router: Router) = SSHUtils.getNVRamInfoFromRouter(context, router,
            Utils.getGlobalSharedPreferences(context), NVRAMInfo.ROUTER_NAME)?.getProperty(NVRAMInfo.ROUTER_NAME)

    override fun getLanIpAddress(context: Context, router: Router) = SSHUtils.getNVRamInfoFromRouter(context, router,
            Utils.getGlobalSharedPreferences(context), NVRAMInfo.LAN_IPADDR)?.getProperty(NVRAMInfo.LAN_IPADDR)

    override fun getWanIpAddress(context: Context, router: Router) = SSHUtils.getNVRamInfoFromRouter(context, router,
            Utils.getGlobalSharedPreferences(context), NVRAMInfo.WAN_IPADDR)?.getProperty(NVRAMInfo.WAN_IPADDR)


    @Throws(Exception::class)
    override fun getDataForStatusRouterStateTile(context: Context, router: Router,
                                                 dataRetrievalListener: RemoteDataRetrievalListener?): NVRAMInfo {

        val globalSharedPreferences = Utils.getGlobalSharedPreferences(context)

        updateProgressBarViewSeparator(dataRetrievalListener, 10)

        var nvramInfo = SSHUtils.getNVRamInfoFromRouter(context, router, globalSharedPreferences,
                NVRAMInfo.ROUTER_NAME, NVRAMInfo.WAN_IPADDR, MODEL,
                NVRAMInfo.DIST_TYPE, NVRAMInfo.LAN_IPADDR,
                NVRAMInfo.OS_VERSION)

        if (nvramInfo == null) {
            nvramInfo = NVRAMInfo()
        }

        val modelPropertyValue = nvramInfo.getProperty(MODEL)
        if (modelPropertyValue != null) {
            nvramInfo.setProperty(NVRAMInfo.MODEL, modelPropertyValue)
        }

        updateProgressBarViewSeparator(dataRetrievalListener, 50)
        //date -d @$(( $(date +%s) - $(cut -f1 -d. /proc/uptime) ))
        //date -d @$(sed -n '/^btime /s///p' /proc/stat)
        //Add FW, Kernel and Uptime
        val otherCmds = SSHUtils.getManualProperty(context, router, globalSharedPreferences,
                //date
                "date",
                //date since last reboot
                "date -d @$(( $(date +%s) - $(cut -f1 -d. /proc/uptime) )) || "
                        + " awk -vuptimediff=\"$(( $(date +%s) - $(cut -f1 -d. /proc/uptime) ))\" "
                        + " 'BEGIN { print strftime(\"%Y-%m-%d %H:%M:%S\", uptimediff); }' ",
                //elapsed from current date
                "uptime | awk -F'up' '{print $2}' | awk -F'users' '{print $1}' | awk -F'load' '{print $1}'",
                "uname -a", "cat /etc/motd 2>&1| tail -n 1")

        if (otherCmds != null) {
            if (otherCmds.size >= 1) {
                //date
                nvramInfo.setProperty(NVRAMInfo.CURRENT_DATE, otherCmds[0])
            }
            if (otherCmds.size >= 3) {
                var uptime = otherCmds[1]
                val uptimeCmd = otherCmds[2]
                if (!Strings.isNullOrEmpty(uptimeCmd)) {
                    val elapsedFromUptime = Utils.removeLastChar(uptimeCmd.trim { it <= ' ' })
                    if (!Strings.isNullOrEmpty(elapsedFromUptime)) {
                        uptime += " (up $elapsedFromUptime)"
                    }
                }
                nvramInfo.setProperty(NVRAMInfo.UPTIME, uptime)
            }

            if (otherCmds.size >= 4) {
                //Kernel
                otherCmds[3]?.let {
                    val valueWithoutGnuLinux = it.replace("GNU/Linux", "")
                    nvramInfo?.setProperty(NVRAMInfo.KERNEL,
                            valueWithoutGnuLinux.replace(nvramInfo?.getProperty(NVRAMInfo.ROUTER_NAME) ?: "", ""))
                }
//        nvramInfo.setProperty(NVRAMInfo.KERNEL,
//            StringUtils.replace(StringUtils.replace(otherCmds[3], "GNU/Linux", ""),
//                nvramInfo.getProperty(NVRAMInfo.ROUTER_NAME), ""))
            }

            if (otherCmds.size >= 5) {
                //Firmware
                val fwString = otherCmds[4]
                nvramInfo.setProperty(NVRAMInfo.FIRMWARE, fwString)
            }
        }

        val routerPreferences = router.getPreferences(context)
        val checkActualInternetConnectivity = routerPreferences == null || routerPreferences.getBoolean(
                RouterCompanionAppConstants.OVERVIEW_NTM_CHECK_ACTUAL_INTERNET_CONNECTIVITY_PREF,
                true)

        if (checkActualInternetConnectivity) {
            //Now get public IP Address
            updateProgressBarViewSeparator(dataRetrievalListener, 80)
            try {
                val wanPublicIp = this.getWanPublicIpAddress(context, router, null)
                if (TextUtils.isEmpty(wanPublicIp)) {
                    nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, NOK)
                } else {
                    if (Patterns.IP_ADDRESS.matcher(wanPublicIp!!).matches()) {
                        nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, wanPublicIp)

                        PublicIPChangesServiceTask.buildNotificationIfNeeded(context, router,
                                arrayOf(wanPublicIp), nvramInfo.getProperty(
                                NVRAMInfo.WAN_IPADDR), null)
                    } else {
                        nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, NOK)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, UNKNOWN)
            } finally {
                dataRetrievalListener?.doRegardlessOfStatus()
            }
        }

        return nvramInfo
    }

    override fun getScmChangesetUrl(changeset: String): String? {
        if (TextUtils.isEmpty(changeset)) {
            return null
        }
        //Assume version format is always: x.y.z
        val stringList = Splitter.on(".").omitEmptyStrings().trimResults().splitToList(changeset)
        if (stringList.size < 2) {
            return null
        }
        return String.format("%s%s%s", TOMATO_CHANGELOG_BASE_URL, stringList[0], stringList[1])
    }

    companion object {

        @JvmField
        val MODEL = "t_model_name"
        @JvmField
        val TOMATO_SCM_URL = "http://repo.or.cz/tomato.git"
        @JvmField
        val TOMATO_SCM_CHANGESET_URL_BASE = TOMATO_SCM_URL + "/tag/refs/tags/tomato-"
        @JvmField
        val TOMATO_WEBSITE = "http://polarcloud.com"
        @JvmField
        val TOMATO_CHANGELOG_BASE_URL = TOMATO_WEBSITE + "/tomato_"

        private fun parseDataForStorageUsageTile(
                dataForStorageUsageTile: List<Array<String>>?,
                dataRetrievalListener: RemoteDataRetrievalListener?): NVRAMInfo {
            if (dataForStorageUsageTile == null || dataForStorageUsageTile.isEmpty()) {
                throw DDWRTNoDataException()
            }

            val nvramInfo = NVRAMInfo()

            val nvramSize = dataForStorageUsageTile[0]

            val jffs2Size: Array<String>?
            if (dataForStorageUsageTile.size >= 2) {
                jffs2Size = dataForStorageUsageTile[1]
            } else {
                jffs2Size = null
            }

            val cifsSize: Array<String>?
            if (dataForStorageUsageTile.size >= 3) {
                cifsSize = dataForStorageUsageTile[2]
            } else {
                cifsSize = null
            }

            dataRetrievalListener?.onProgressUpdate(40)

            if (nvramSize != null && nvramSize.size >= 1) {
                val nvramSizeStr = nvramSize[0]
                if (nvramSizeStr != null && nvramSizeStr.endsWith("free.")) {
                    val stringList = COMMA_SPLITTER.splitToList(nvramSizeStr)
                    if (stringList.size >= 3) {
                        val nbEntriesStr = stringList[0].replace(" entries", "").trim { it <= ' ' }
                        val nvramUsedBytesStr = stringList[1].replace(" bytes used", "").trim { it <= ' ' }
                        val nvramFreeBytesStr = stringList[2].replace(" bytes free.", "").trim { it <= ' ' }

                        try {
                            val nvramUsedBytesLong = java.lang.Long.parseLong(nvramUsedBytesStr)
                            val nvramFreeBytesLong = java.lang.Long.parseLong(nvramFreeBytesStr)
                            val nvramTotalBytesLong = nvramUsedBytesLong + nvramFreeBytesLong
                            nvramInfo.setProperty(NVRAMInfo.NVRAM_USED_PERCENT,
                                    java.lang.Long.toString(
                                            Math.min(100, 100 * nvramUsedBytesLong / nvramTotalBytesLong)))
                        } catch (e: NumberFormatException) {
                            e.printStackTrace()
                            Crashlytics.logException(e)
                        }

                    }
                }
            }
            dataRetrievalListener?.onProgressUpdate(50)

            if (jffs2Size != null && jffs2Size.size >= 1) {
                //We may have more than one mountpoint - so sum everything up
                var totalUsed: Long = 0
                var totalSize: Long = 0
                for (i in jffs2Size.indices) {
                    val jffs2SizeStr = jffs2Size[i]
                    if (!Strings.isNullOrEmpty(jffs2SizeStr)) {
                        val stringList = SPACE_SPLITTER.splitToList(jffs2SizeStr)
                        if (stringList.size >= 7) {
                            try {
                                totalSize += java.lang.Long.parseLong(stringList[2])
                                totalUsed += java.lang.Long.parseLong(stringList[3])
                            } catch (e: NumberFormatException) {
                                e.printStackTrace()
                                Crashlytics.logException(e)
                            }

                        }
                    }
                    dataRetrievalListener?.onProgressUpdate(Math.min(70, 50 + 5 * i))
                }
                if (totalSize > 0) {
                    nvramInfo.setProperty(NVRAMInfo.STORAGE_JFFS2_USED_PERCENT,
                            java.lang.Long.toString(Math.min(100, 100 * totalUsed / totalSize)))
                }
            }
            dataRetrievalListener?.onProgressUpdate(75)

            if (cifsSize != null && cifsSize.size >= 1) {
                //We may have more than one mountpoint - so sum everything up
                var totalUsed: Long = 0
                var totalSize: Long = 0
                for (i in cifsSize.indices) {
                    val cifsSizeStr = cifsSize[i]
                    if (!Strings.isNullOrEmpty(cifsSizeStr)) {
                        val stringList = SPACE_SPLITTER.splitToList(cifsSizeStr)
                        if (stringList.size >= 7) {
                            try {
                                totalSize += java.lang.Long.parseLong(stringList[2])
                                totalUsed += java.lang.Long.parseLong(stringList[3])
                            } catch (e: NumberFormatException) {
                                e.printStackTrace()
                                Crashlytics.logException(e)
                            }

                        }
                    }
                    dataRetrievalListener?.onProgressUpdate(Math.min(87, 75 + 5 * i))
                }
                if (totalSize > 0) {
                    nvramInfo.setProperty(NVRAMInfo.STORAGE_CIFS_USED_PERCENT,
                            java.lang.Long.toString(Math.min(100, 100 * totalUsed / totalSize)))
                }
            }
            return nvramInfo
        }
    }

    override fun getWANAccessPolicies(context: Context, router: Router,
                                      dataRetrievalListener: RemoteDataRetrievalListener?): WANAccessPoliciesRouterData? {
        //Kotlin's TODO throws an Error
        throw UnsupportedOperationException("not implemented yet") //To change body of created functions use File | Settings | File Templates.
    }

    override fun manuallyCheckForFirmwareUpdateAndReturnDownloadLink(currentFwVer: String?): FirmwareRelease? {
        //Kotlin's TODO throws an Error
        throw UnsupportedOperationException("not implemented yet")
    }

}
