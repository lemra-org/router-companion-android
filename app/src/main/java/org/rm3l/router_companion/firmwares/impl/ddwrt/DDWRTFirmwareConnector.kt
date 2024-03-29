package org.rm3l.router_companion.firmwares.impl.ddwrt

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.util.Patterns
import com.google.common.base.Splitter
import com.google.common.base.Strings
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import org.rm3l.router_companion.RouterCompanionAppConstants
import org.rm3l.router_companion.RouterCompanionAppConstants.DDWRT_RELEASE_REMOTE_HOST
import org.rm3l.router_companion.RouterCompanionAppConstants.DDWRT_RELEASE_URL_FORMAT
import org.rm3l.router_companion.RouterCompanionAppConstants.NOK
import org.rm3l.router_companion.RouterCompanionAppConstants.UNKNOWN
import org.rm3l.router_companion.exceptions.DDWRTNoDataException
import org.rm3l.router_companion.firmwares.AbstractRouterFirmwareConnector
import org.rm3l.router_companion.firmwares.FirmwareRelease
import org.rm3l.router_companion.firmwares.NoNewFirmwareUpdate
import org.rm3l.router_companion.firmwares.RemoteDataRetrievalListener
import org.rm3l.router_companion.firmwares.impl.ddwrt.tile_data_workers.dashboard.network.NetworkTopologyMapTileWorker
import org.rm3l.router_companion.mgmt.RouterManagementActivity
import org.rm3l.router_companion.resources.MonthlyCycleItem
import org.rm3l.router_companion.resources.WANAccessPolicy
import org.rm3l.router_companion.resources.conn.NVRAMInfo
import org.rm3l.router_companion.resources.conn.NVRAMInfo.Companion.NVRAM_USED_PERCENT
import org.rm3l.router_companion.resources.conn.Router
import org.rm3l.router_companion.service.tasks.PublicIPChangesServiceTask
import org.rm3l.router_companion.tiles.admin.accessrestrictions.AccessRestrictionsWANAccessTile
import org.rm3l.router_companion.tiles.admin.accessrestrictions.WANAccessPoliciesRouterData
import org.rm3l.router_companion.tiles.dashboard.bandwidth.WANTotalTrafficOverviewTile.DDWRT_TRAFF_DATA_SIMPLE_DATE_FORMAT
import org.rm3l.router_companion.tiles.dashboard.bandwidth.WANTotalTrafficOverviewTile.TRAFF_PREFIX
import org.rm3l.router_companion.tiles.dashboard.network.NetworkTopologyMapTile.INTERNET_CONNECTIVITY_PUBLIC_IP
import org.rm3l.router_companion.tiles.status.router.StatusRouterCPUTile.GREP_MODEL_PROC_CPUINFO
import org.rm3l.router_companion.utils.SSHUtils
import org.rm3l.router_companion.utils.Utils
import org.rm3l.router_companion.utils.Utils.COMMA_SPLITTER
import org.rm3l.router_companion.utils.Utils.SPACE_SPLITTER
import org.rm3l.router_companion.utils.WANTrafficUtils
import org.rm3l.router_companion.utils.WANTrafficUtils.retrieveAndPersistMonthlyTrafficData
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Properties

const val SSH_LOGIN_ALWAYS_ROOT_MESSAGE = "SSH Login is always 'root' in DD-WRT firmwares, " +
    "even if you changed the username via the Web Interface"

class DDWRTFirmwareConnector : AbstractRouterFirmwareConnector() {

    @Throws(Exception::class)
    override fun getDataForNetworkTopologyMapTile(
        context: Context,
        router: Router,
        dataRetrievalListener: RemoteDataRetrievalListener?
    ): NVRAMInfo {
        return NetworkTopologyMapTileWorker.getDataForNetworkTopologyMapTile(
            context, router,
            dataRetrievalListener
        )
    }

    @Throws(Exception::class)
    override fun getWanPublicIpAddress(
        context: Context,
        router: Router,
        dataRetrievalListener: RemoteDataRetrievalListener?
    ): String? {
        return SSHUtils.loadWanPublicIPFrom(context, router, null, dataRetrievalListener)
    }

    override fun getRouterName(context: Context, router: Router) = SSHUtils.getNVRamInfoFromRouter(
        context, router,
        Utils.getGlobalSharedPreferences(context), NVRAMInfo.ROUTER_NAME
    )?.getProperty(NVRAMInfo.ROUTER_NAME)

    override fun getLanIpAddress(context: Context, router: Router) = SSHUtils.getNVRamInfoFromRouter(
        context, router,
        Utils.getGlobalSharedPreferences(context), NVRAMInfo.LAN_IPADDR
    )?.getProperty(NVRAMInfo.LAN_IPADDR)

    override fun getWanIpAddress(context: Context, router: Router) = SSHUtils.getNVRamInfoFromRouter(
        context, router,
        Utils.getGlobalSharedPreferences(context), NVRAMInfo.WAN_IPADDR
    )?.getProperty(NVRAMInfo.WAN_IPADDR)

    @Throws(Exception::class)
    override fun goGetRouterModel(context: Context, router: Router): String? {
        val output = SSHUtils.getManualProperty(
            context, router,
            Utils.getGlobalSharedPreferences(context),
            String.format(
                "/usr/sbin/nvram show 2>/dev/null | grep \"%s\" | awk -F'=' '{print $2}'",
                MODEL
            )
        )
        if (output != null && output.isNotEmpty()) {
            return output[0]
        }
        return null
    }

    @Throws(Exception::class)
    override fun getDataForWANTotalTrafficOverviewTile(
        context: Context,
        router: Router,
        cycleItem: MonthlyCycleItem,
        dataRetrievalListener: RemoteDataRetrievalListener?
    ): NVRAMInfo {

        val dao = RouterManagementActivity.getDao(context)
        val globalSharedPreferences = Utils.getGlobalSharedPreferences(context)

        dataRetrievalListener?.onProgressUpdate(20)

        val today = Date()
        val mCurrentMonth = DDWRT_TRAFF_DATA_SIMPLE_DATE_FORMAT.format(today)

        // Also retrieve data for previous month and next month
        val cal1 = Calendar.getInstance()
        cal1.add(Calendar.MONTH, -1)
        val mPrevMonth = DDWRT_TRAFF_DATA_SIMPLE_DATE_FORMAT.format(cal1.time)

        val cal2 = Calendar.getInstance()
        cal2.add(Calendar.MONTH, 1)
        val mNextMonth = DDWRT_TRAFF_DATA_SIMPLE_DATE_FORMAT.format(cal2.time)

        val traffForPreviousMonthKey = TRAFF_PREFIX + mPrevMonth
        val traffForCurrentMonthKey = TRAFF_PREFIX + mCurrentMonth
        val traffForNextMonthKey = TRAFF_PREFIX + mNextMonth

        val nvramInfo = NVRAMInfo()

        var nvramInfoTmp: NVRAMInfo? = null
        try {
            dataRetrievalListener?.onProgressUpdate(50)

            nvramInfoTmp = SSHUtils.getNVRamInfoFromRouter(
                context, router, globalSharedPreferences,
                NVRAMInfo.TTRAFF_ENABLE, traffForPreviousMonthKey, traffForCurrentMonthKey,
                traffForNextMonthKey
            )
        } finally {
            if (nvramInfoTmp != null) {
                nvramInfo.putAll(nvramInfoTmp)
            }
        }

        if (nvramInfo.isEmpty) {
            throw DDWRTNoDataException("No Data!")
        }

        retrieveAndPersistMonthlyTrafficData(router, dao, nvramInfo)

        dataRetrievalListener?.onProgressUpdate(75)

        nvramInfo.putAll(
            WANTrafficUtils.computeWANTrafficUsageBetweenDates(
                dao, router.uuid,
                cycleItem.start, cycleItem.end
            )
        )

        return nvramInfo
    }

    @Throws(Exception::class)
    override fun getDataForUptimeTile(
        context: Context,
        router: Router,
        dataRetrievalListener: RemoteDataRetrievalListener?
    ): NVRAMInfo {
        val globalSharedPreferences = Utils.getGlobalSharedPreferences(context)

        dataRetrievalListener?.onProgressUpdate(10)

        val nvramInfo = NVRAMInfo()

        val uptimeOutput = SSHUtils.getManualProperty(
            context, router, globalSharedPreferences,
            "uptime 2>/dev/null | awk -F'up ' '{print $2}' | awk -F'users' '{print $1}' | awk -F'load' '{print $1}'"
        )

        dataRetrievalListener?.onProgressUpdate(60)

        if (uptimeOutput != null && uptimeOutput.size > 0) {
            val uptime = uptimeOutput[uptimeOutput.size - 1]
            val uptimeList = COMMA_SPLITTER.splitToList(uptime)
            if (uptimeList != null) {
                val uptimeListSize = uptimeList.size
                if (uptimeListSize > 0) {
                    val first = uptimeList[0].trim { it <= ' ' }
                    if (first.contains("day", ignoreCase = true)) {
                        // day
                        nvramInfo.setProperty(
                            NVRAMInfo.UPTIME_DAYS,
                            first.replace("days".toRegex(), "").replace("day".toRegex(), "").trim { it <= ' ' }
                        )

                        if (uptimeListSize >= 2) {
                            val other = uptimeList[1]
                            if (other != null) {
                                if (other.contains(":")) {
                                    val otherList = Splitter.on(":").omitEmptyStrings().splitToList(other)
                                    if (otherList != null) {
                                        if (otherList.size >= 1) {
                                            nvramInfo.setProperty(NVRAMInfo.UPTIME_HOURS, otherList[0].trim { it <= ' ' })
                                        }
                                        if (otherList.size >= 2) {
                                            nvramInfo.setProperty(
                                                NVRAMInfo.UPTIME_MINUTES,
                                                otherList[1].trim { it <= ' ' }
                                            )
                                        }
                                    }
                                } else if (other.contains("hour", ignoreCase = true)) {
                                    nvramInfo.setProperty(
                                        NVRAMInfo.UPTIME_HOURS,
                                        other.replace("hours".toRegex(), "").replace(
                                            "hour".toRegex(),
                                            ""
                                        ).trim { it <= ' ' }
                                    )
                                } else if (other.contains("min", ignoreCase = true)) {
                                    nvramInfo.setProperty(
                                        NVRAMInfo.UPTIME_MINUTES,
                                        other.replace("mins".toRegex(), "").replace(
                                            "min".toRegex(),
                                            ""
                                        ).trim { it <= ' ' }
                                    )
                                }
                            }
                        }
                    } else if (first.contains(":")) {
                        val otherList = Splitter.on(":").omitEmptyStrings().splitToList(first)
                        if (otherList != null) {
                            if (otherList.size >= 1) {
                                nvramInfo.setProperty(NVRAMInfo.UPTIME_HOURS, otherList[0].trim { it <= ' ' })
                            }
                            if (otherList.size >= 2) {
                                nvramInfo.setProperty(NVRAMInfo.UPTIME_MINUTES, otherList[1].trim { it <= ' ' })
                            }
                        }
                    } else if (first.contains("hour", ignoreCase = true)) {
                        nvramInfo.setProperty(
                            NVRAMInfo.UPTIME_HOURS,
                            first.replace("hours".toRegex(), "").replace(
                                "hour".toRegex(),
                                ""
                            ).trim { it <= ' ' }
                        )
                    } else if (first.contains("min", ignoreCase = true)) {
                        nvramInfo.setProperty(
                            NVRAMInfo.UPTIME_MINUTES,
                            first.trim { it <= ' ' }.replace("mins".toRegex(), "").replace(
                                "min".toRegex(),
                                ""
                            ).trim { it <= ' ' }
                        )
                    }
                }
            }
        }

        return nvramInfo
    }

    @Throws(Exception::class)
    override fun getDataForMemoryAndCpuUsageTile(
        context: Context,
        router: Router,
        dataRetrievalListener: RemoteDataRetrievalListener?
    ): List<Array<String>> {

        dataRetrievalListener?.onProgressUpdate(10)

        val globalSharedPreferences = Utils.getGlobalSharedPreferences(context)

        val memData = SSHUtils.getManualProperty(
            context, router, globalSharedPreferences,
            getGrepProcMemInfo("MemTotal"), getGrepProcMemInfo("MemFree")
        )

        dataRetrievalListener?.onProgressUpdate(30)

        val cpuUsageData = SSHUtils.getManualProperty(
            context, router, globalSharedPreferences,
            "uptime | awk -F'average:' '{ print $2}'", "$GREP_MODEL_PROC_CPUINFO| wc -l"
        )

        return listOf(memData ?: emptyArray(), cpuUsageData ?: emptyArray())
    }

    @Throws(Exception::class)
    override fun getDataForStorageUsageTile(
        context: Context,
        router: Router,
        dataRetrievalListener: RemoteDataRetrievalListener?
    ): NVRAMInfo {
        dataRetrievalListener?.onProgressUpdate(10)

        val globalSharedPreferences = Utils.getGlobalSharedPreferences(context)

        val nvramSize: Array<String?>? = SSHUtils.getManualProperty(
            context, router, globalSharedPreferences,
            "/usr/sbin/nvram show 2>&1 1>/dev/null | grep \"size: \""
        )
        dataRetrievalListener?.onProgressUpdate(20)

        val jffs2Size: Array<String?>? = SSHUtils.getManualProperty(
            context, router, globalSharedPreferences,
            "/bin/df -T | grep \"jffs2\""
        )
        dataRetrievalListener?.onProgressUpdate(30)

        val cifsSize: Array<String?>? = SSHUtils.getManualProperty(
            context, router, globalSharedPreferences,
            "/bin/df -T | grep \"cifs\""
        )

        return parseDataForStorageUsageTile(listOf(nvramSize, jffs2Size, cifsSize), dataRetrievalListener)
    }

    @Throws(Exception::class)
    override fun getDataForStatusRouterStateTile(
        context: Context,
        router: Router,
        dataRetrievalListener: RemoteDataRetrievalListener?
    ): NVRAMInfo {

        val globalSharedPreferences = Utils.getGlobalSharedPreferences(context)

        updateProgressBarViewSeparator(dataRetrievalListener, 10)

        var nvramInfo = SSHUtils.getNVRamInfoFromRouter(
            context, router, globalSharedPreferences,
            NVRAMInfo.ROUTER_NAME, NVRAMInfo.WAN_IPADDR,
            NVRAMInfo.MODEL, NVRAMInfo.DIST_TYPE,
            NVRAMInfo.LAN_IPADDR
        )

        if (nvramInfo == null) {
            nvramInfo = NVRAMInfo()
        }

        updateProgressBarViewSeparator(dataRetrievalListener, 50)
        // date -d @$(( $(date +%s) - $(cut -f1 -d. /proc/uptime) ))
        // date -d @$(sed -n '/^btime /s///p' /proc/stat)
        val otherCmds = SSHUtils.getManualProperty(
            context, router, globalSharedPreferences,
            // date
            "date",
            // date since last reboot
            "date -d @$(( $(date +%s) - $(cut -f1 -d. /proc/uptime) )) || " +
                " awk -vuptimediff=\"$(( $(date +%s) - $(cut -f1 -d. /proc/uptime) ))\" " +
                " 'BEGIN { print strftime(\"%Y-%m-%d %H:%M:%S\", uptimediff); }' ",
            // elapsed from current date
            "uptime | awk -F'up' '{print $2}' | awk -F'users' '{print $1}' | awk -F'load' '{print $1}'",
            "uname -a",
            "echo \"`cat /tmp/loginprompt|grep DD-WRT|" +
                "cut -d' ' -f1` `cat /tmp/loginprompt|grep DD-WRT|" +
                "cut -d' ' -f2` (`cat /tmp/loginprompt|grep Release|cut -d' ' -f2`) " +
                "`cat /tmp/loginprompt|grep DD-WRT|" +
                "cut -d' ' -f3` - SVN rev: `/sbin/softwarerevision`\""
        )

        if (otherCmds != null) {
            if (otherCmds.isNotEmpty()) {
                // date
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
                // Kernel
                otherCmds[3]?.let {
                    val valueWithoutGnuLinux = it.replace("GNU/Linux", "")
                    nvramInfo.setProperty(
                        NVRAMInfo.KERNEL,
                        valueWithoutGnuLinux.replace(nvramInfo.getProperty(NVRAMInfo.ROUTER_NAME) ?: "", "")
                    )
                }
//        nvramInfo.setProperty(NVRAMInfo.KERNEL,
//            StringUtils.replace(StringUtils.replace(otherCmds[3], "GNU/Linux", ""),
//                nvramInfo.getProperty(NVRAMInfo.ROUTER_NAME), ""))
            }

            if (otherCmds.size >= 5) {
                // Firmware
                val fwString = otherCmds[4]
                nvramInfo.setProperty(NVRAMInfo.FIRMWARE, fwString)

                val strings = Splitter.on("rev:").omitEmptyStrings().trimResults().splitToList(fwString)
                if (strings.size >= 2) {
                    try {
                        nvramInfo.setProperty(
                            NVRAMInfo.OS_VERSION,
                            java.lang.Long.toString(java.lang.Long.parseLong(strings[1].trim { it <= ' ' }))
                        )
                    } catch (nfe: NumberFormatException) {
                        // No worries
                    }
                }
            }
        }

        val routerPreferences = router.getPreferences(context)
        val checkActualInternetConnectivity = routerPreferences == null || routerPreferences.getBoolean(
            RouterCompanionAppConstants.OVERVIEW_NTM_CHECK_ACTUAL_INTERNET_CONNECTIVITY_PREF,
            true
        )

        if (checkActualInternetConnectivity) {
            // Now get public IP Address
            updateProgressBarViewSeparator(dataRetrievalListener, 80)
            try {
                val wanPublicIp = this.getWanPublicIpAddress(context, router, null)
                if (TextUtils.isEmpty(wanPublicIp)) {
                    nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, NOK)
                } else {
                    if (Patterns.IP_ADDRESS.matcher(wanPublicIp!!).matches()) {
                        nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, wanPublicIp)

                        PublicIPChangesServiceTask.buildNotificationIfNeeded(
                            context, router,
                            arrayOf(wanPublicIp),
                            nvramInfo.getProperty(
                                NVRAMInfo.WAN_IPADDR
                            ),
                            null
                        )
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
        return String.format("%s/%s", DDWRT_SCM_CHANGESET_URL_BASE, changeset.trim { it <= ' ' })
    }

    companion object {

        val TAG: String = DDWRTFirmwareConnector::class.java.simpleName

        @JvmField
        val MODEL = "DD_BOARD"

        @JvmField
        val DDWRT_SCM_URL = "http://svn.dd-wrt.com"
        @JvmField
        val DDWRT_SCM_CHANGESET_URL_BASE = DDWRT_SCM_URL + "/changeset/"

        fun getGrepProcMemInfo(item: String): String {
            return "grep \"^$item\" /proc/meminfo "
        }

        fun parseDataForStorageUsageTile(
            dataForStorageUsageTile: List<Array<String?>?>?,
            dataRetrievalListener: RemoteDataRetrievalListener?
        ): NVRAMInfo {
            if (dataForStorageUsageTile == null || dataForStorageUsageTile.isEmpty()) {
                throw DDWRTNoDataException()
            }

            val nvramInfo = NVRAMInfo()

            val nvramSize = dataForStorageUsageTile[0]

            val jffs2Size = if (dataForStorageUsageTile.size >= 2) {
                dataForStorageUsageTile[1]
            } else {
                null
            }

            val cifsSize = if (dataForStorageUsageTile.size >= 3) {
                dataForStorageUsageTile[2]
            } else {
                null
            }

            dataRetrievalListener?.onProgressUpdate(40)

            if (nvramSize != null && nvramSize.isNotEmpty()) {
                val nvramSizeStr = nvramSize[0]
                if (nvramSizeStr?.startsWith("size:") == true) {
                    // "size: 37189 bytes (28347 left)" => "size:", "37189", "bytes", "(28347", "left)"
                    val stringList = SPACE_SPLITTER.splitToList(nvramSizeStr)
                    if (stringList.size >= 5) {
                        val nvramUsedBytes = stringList[1].trim { it <= ' ' }
                        val nvramLeftBytes = stringList[3].replace("(", "").trim { it <= ' ' }
                        try {
                            val nvramUsedBytesLong = nvramUsedBytes.toLong()
                            val nvramLeftBytesLong = nvramLeftBytes.toLong()
                            val nvramTotalBytesLong = nvramUsedBytesLong.plus(nvramLeftBytesLong)
                            FirebaseCrashlytics.getInstance().log(
                                "<nvramUsedBytesLong, nvramLeftBytesLong, nvramTotalBytesLong> = " +
                                    "<$nvramUsedBytesLong, $nvramLeftBytesLong, $nvramTotalBytesLong>"
                            )
                            if (nvramTotalBytesLong > 0L) {
                                val nvramUsedPercent =
                                    100L.coerceAtMost(100L.times(nvramUsedBytesLong).div(nvramTotalBytesLong))
                                if (nvramUsedPercent >= 0) {
                                    nvramInfo.setProperty(NVRAM_USED_PERCENT, nvramUsedPercent.toString())
                                }
                            }
                        } catch (e: NumberFormatException) {
                            e.printStackTrace()
                            FirebaseCrashlytics.getInstance().recordException(e)
                        }
                    }
                }
            }
            dataRetrievalListener?.onProgressUpdate(50)

            if (jffs2Size != null && jffs2Size.isNotEmpty()) {
                // We may have more than one mountpoint - so sum everything up
                var totalUsed: Long = 0
                var totalSize: Long = 0
                for (i in jffs2Size.indices) {
                    val jffs2SizeStr = jffs2Size[i]
                    if (jffs2SizeStr?.isNotBlank() == true) {
                        val stringList = SPACE_SPLITTER.splitToList(jffs2SizeStr)
                        if (stringList.size >= 7) {
                            try {
                                totalSize += stringList[2].toLong()
                                totalUsed += stringList[3].toLong()
                            } catch (e: NumberFormatException) {
                                e.printStackTrace()
                                FirebaseCrashlytics.getInstance().recordException(e)
                            }
                        }
                    }
                    dataRetrievalListener?.onProgressUpdate(70.coerceAtMost(50 + 5 * i))
                }
                if (totalSize > 0) {
                    nvramInfo.setProperty(
                        NVRAMInfo.STORAGE_JFFS2_USED_PERCENT,
                        100L.coerceAtMost(100 * totalUsed / totalSize).toString()
                    )
                }
            }
            dataRetrievalListener?.onProgressUpdate(75)

            if (cifsSize != null && cifsSize.isNotEmpty()) {
                // We may have more than one mountpoint - so sum everything up
                var totalUsed: Long = 0
                var totalSize: Long = 0
                for (i in cifsSize.indices) {
                    val cifsSizeStr = cifsSize[i]
                    if (cifsSizeStr?.isNotBlank() == true) {
                        val stringList = SPACE_SPLITTER.splitToList(cifsSizeStr)
                        if (stringList.size >= 7) {
                            try {
                                totalSize += stringList[2].toLong()
                                totalUsed += stringList[3].toLong()
                            } catch (e: NumberFormatException) {
                                e.printStackTrace()
                                FirebaseCrashlytics.getInstance().recordException(e)
                            }
                        }
                    }
                    dataRetrievalListener?.onProgressUpdate(87.coerceAtMost(75 + 5 * i))
                }
                if (totalSize > 0) {
                    nvramInfo.setProperty(
                        NVRAMInfo.STORAGE_CIFS_USED_PERCENT,
                        100L.coerceAtMost(100 * totalUsed / totalSize).toString()
                    )
                }
            }
            return nvramInfo
        }
    }

    override fun getWANAccessPolicies(
        context: Context,
        router: Router,
        dataRetrievalListener: RemoteDataRetrievalListener?
    ): WANAccessPoliciesRouterData? {
        val wanAccessPolicies = ArrayList<WANAccessPolicy>()

        dataRetrievalListener?.onProgressUpdate(10)
        // 1- Get all rules first

        /*
                    filter_rule10=$STAT:1$NAME:myPolicy10$DENY:1$$
                    filter_rule1=$STAT:0$NAME:myPolicy1$DENY:0$$
                    filter_rule2=$STAT:2$NAME:myPolicy2$DENY:0$$
                    filter_rule3=
                    filter_rule4=
                    filter_rule5=
                    filter_rule6=
                    filter_rule7=$STAT:1$NAME:myPolicy7$DENY:1$$

                    filter_rule1=$STAT:1$NAME:Only allow preset IP-addresses$DENY:1$$
                    filter_rule2=$STAT:1$NAME:Inget internet p▒ natten$DENY:1$$
                    filter_rule3=$STAT:1$NAME:Paus mitt p▒ dagen$DENY:1$$
                    filter_rule4=$STAT:1$NAME:Skoldag$DENY:1$$
                    filter_rule5=
                    filter_rule6=
                    filter_rule7=
                    filter_rule8=
                    filter_rule9=
                    filter_rule10=
                     */
        val globalSharedPreferences = Utils.getGlobalSharedPreferences(context)
        var nvramInfo = SSHUtils.getNVRamInfoFromRouter(context, router, globalSharedPreferences, "filter_rule.*")
        val properties: Properties? = nvramInfo?.getData()
        if (nvramInfo == null || properties == null) {
            return null
        }

        var i = 2
        var todPattern: String
        val entries = properties.entries
        for ((key, value) in entries) {
            if (key == null || value == null) {
                continue
            }
            // Skip empty rules
            val valueStr = value.toString()
            if (Strings.isNullOrEmpty(valueStr)) {
                continue
            }
            val keyStr = key.toString()
            val keyNb = Integer.parseInt(keyStr.replace("filter_rule", "").trim { it <= ' ' })

            val wanAccessPolicy = WANAccessPolicy()
            wanAccessPolicy.number = keyNb

            val statusSplitter = Splitter.on(
                "\$NAME:"
            ).omitEmptyStrings().trimResults().splitToList(valueStr)
            if (!statusSplitter.isEmpty()) {
                // myPolicy7$DENY:1$$
                wanAccessPolicy.status = statusSplitter[0].replace("\$STAT:".toRegex(), "")
                if (statusSplitter.size >= 2) {
                    val nameAndFollowingStr = statusSplitter[1]
                    val nameAndFollowingSplitter = Splitter.on("\$DENY:")
                        .omitEmptyStrings()
                        .trimResults()
                        .splitToList(nameAndFollowingStr)
                    if (!nameAndFollowingSplitter.isEmpty()) {
                        wanAccessPolicy.name = nameAndFollowingSplitter[0]
                        if (nameAndFollowingSplitter.size >= 2) {
                            // 1$$
                            val s = nameAndFollowingSplitter[1].replace("\\$\\$".toRegex(), "")
                            if ("0" == s) {
                                wanAccessPolicy.denyOrFilter = WANAccessPolicy.FILTER
                            } else {
                                wanAccessPolicy.denyOrFilter = WANAccessPolicy.DENY
                            }
                        }
                    }
                }
            } else {
                wanAccessPolicy.status = WANAccessPolicy.STATUS_UNKNOWN
            }

            // 2- For each, retrieve Time of Day (TOD)
            nvramInfo = SSHUtils.getNVRamInfoFromRouter(
                context, router,
                globalSharedPreferences, "filter_tod_buf" + keyNb
            )

            updateProgressBarViewSeparator(dataRetrievalListener, 10 + i++)

            if (nvramInfo != null && nvramInfo.getProperty("filter_tod_buf" + keyNb) != null) {

                todPattern = nvramInfo.getProperty("filter_tod_buf" + keyNb) as String
                if ("7" == todPattern) {
                    todPattern = "1 1 1 1 1 1 1"
                }
                wanAccessPolicy.daysPattern = todPattern
            }

            nvramInfo = SSHUtils.getNVRamInfoFromRouter(
                context, router,
                globalSharedPreferences, "filter_tod" + keyNb
            )

            updateProgressBarViewSeparator(dataRetrievalListener, 10 + i++)

            if (nvramInfo != null && nvramInfo.getProperty("filter_tod" + keyNb) != null) {
                /*
                                filter_tod4=0:0 23:59 0-6
                                filter_tod5=0:0 23:59 0,2,6
                                filter_tod6=0:0 23:59 0-1
                                filter_tod7=6:0 18:0 0-6
                                 */
                val filterTod = nvramInfo.getProperty("filter_tod" + keyNb)
                val list = Splitter.on(" ").omitEmptyStrings().trimResults().splitToList(
                    filterTod!!
                )
                if (list.size >= 2) {
                    val start = list[0]
                    val end = list[1]
                    if ("0:0" == start && "23:59" == end) {
                        wanAccessPolicy.timeOfDay = "24 Hours"
                    } else {
                        wanAccessPolicy.timeOfDay =
                            String.format(
                                Locale.US, "from %s to %s",
                                AccessRestrictionsWANAccessTile.getHourFormatted(start),
                                AccessRestrictionsWANAccessTile.getHourFormatted(end)
                            )
                    }
                }
            }

            FirebaseCrashlytics.getInstance().log("wanAccessPolicy: " + wanAccessPolicy)

            wanAccessPolicies.add(wanAccessPolicy)

            updateProgressBarViewSeparator(dataRetrievalListener, 10 + i++)
        }

        updateProgressBarViewSeparator(dataRetrievalListener, 80)

        val routerData = WANAccessPoliciesRouterData().setData(
            wanAccessPolicies
        ) as WANAccessPoliciesRouterData
        dataRetrievalListener?.onProgressUpdate(90)
        return routerData
    }

    override fun manuallyCheckForFirmwareUpdateAndReturnDownloadLink(currentFwVer: String?): DDWRTRelease? {
        var ftp: FTPClient? = null
        try {
            val currentFwVerLong = currentFwVer?.split("-")?.get(0)?.toLongOrNull()
            FirebaseCrashlytics.getInstance().log(
                "<currentFwVer, currentFwVerLong>=<$currentFwVer,$currentFwVerLong>"
            )
            // Now browse the DD-WRT update website and check for the most recent
            ftp = FTPClient()
            // final FTPClientConfig config = new FTPClientConfig();
            // config.setXXX(YYY); // change required options
            // ftp.configure(config );
            ftp.connect(DDWRT_RELEASE_REMOTE_HOST)
            val reply = ftp.replyCode
            FirebaseCrashlytics.getInstance().log("Connected to FTP Server: $DDWRT_RELEASE_REMOTE_HOST. replyCode=$reply")
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect()
                FirebaseCrashlytics.getInstance().log("Disconnected from FTP Server: $DDWRT_RELEASE_REMOTE_HOST")
                throw IllegalStateException("Server refused connection. Please try again later...")
            }
            ftp.login("anonymous", "anonymous")
            ftp.changeWorkingDirectory("betas")
            val directories = ftp.listDirectories()
            val newerReleases = directories
                .map {
                    FirebaseCrashlytics.getInstance().log("Found dir: ${it.name}")
                    it.name
                }
                .map { it to ftp.listDirectories(it).toList() }
                .flatMap { yearReleasesPair ->
                    val year = yearReleasesPair.first
                    yearReleasesPair.second
                        .map { releaseByDay ->
                            val releaseByDayName = releaseByDay.name
                            val releaseByDaySplitList = releaseByDayName.split(delimiters = *arrayOf("-"), limit = 4)
                            val ddwrtRelease = DDWRTRelease(
                                year.trim().toInt(),
                                "${releaseByDaySplitList[0]}-${releaseByDaySplitList[1]}-${releaseByDaySplitList[2]}",
                                releaseByDaySplitList[3]
                            )
                            FirebaseCrashlytics.getInstance().log(
                                "Found release for year: $year : $releaseByDayName =>  $ddwrtRelease"
                            )
                            ddwrtRelease
                        }
                        .toList()
                }
                .filter { ddwrtRelease ->
                    currentFwVerLong == null ||
                        (ddwrtRelease.revisionNumber != null && ddwrtRelease.revisionNumber > currentFwVerLong)
                }
                .sortedBy { it.revisionNumber }
                .toList()
            ftp.logout()
            if (newerReleases.isEmpty()) {
                // No new release
                throw NoNewFirmwareUpdate()
            }
            return newerReleases.last()
        } finally {
            if (ftp?.isConnected == true) {
                try {
                    ftp.disconnect()
                } catch (ignored: Exception) {
                    ignored.printStackTrace()
                }
            }
        }
    }
}

@SuppressLint("SimpleDateFormat")
val releaseDateFormat = SimpleDateFormat("MM-dd-yyyy")

data class DDWRTRelease(val year: Int?, val date: Date?, private val revision: String) : FirmwareRelease(revision) {
    val revisionNumber = this.revision.trim().replaceFirst("r", "").toLongOrNull()

    @Throws(ParseException::class)
    constructor(year: Int?, dateString: String, revision: String) :
        this(year, releaseDateFormat.parse(dateString), revision)

    override fun getDirectLink() = DDWRT_RELEASE_URL_FORMAT.format(year, "${releaseDateFormat.format(date)}-$revision")
}
