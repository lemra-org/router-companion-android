package org.rm3l.router_companion.job.speedtest

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.util.Pair
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.evernote.android.job.DailyJob
import com.evernote.android.job.DailyJob.DailyJobResult.CANCEL
import com.evernote.android.job.DailyJob.DailyJobResult.SUCCESS
import com.evernote.android.job.Job
import com.evernote.android.job.Job.Result.FAILURE
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import com.evernote.android.job.util.support.PersistableBundleCompat
import com.google.common.base.Joiner
import com.google.common.base.Splitter
import com.google.common.base.Strings
import com.google.common.collect.Lists
import org.rm3l.ddwrt.BuildConfig
import org.rm3l.ddwrt.R
import org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY
import org.rm3l.router_companion.RouterCompanionAppConstants.MAX_ROUTER_SPEEDTEST_RESULTS_FREE_VERSION
import org.rm3l.router_companion.RouterCompanionAppConstants.ROUTER_SPEED_TEST_AUTO_MEASUREMENTS_SERVER
import org.rm3l.router_companion.RouterCompanionAppConstants.ROUTER_SPEED_TEST_AUTO_MEASUREMENTS_SERVER_DEFAULT
import org.rm3l.router_companion.RouterCompanionAppConstants.ROUTER_SPEED_TEST_DURATION_THRESHOLD_SECONDS
import org.rm3l.router_companion.RouterCompanionAppConstants.ROUTER_SPEED_TEST_DURATION_THRESHOLD_SECONDS_DEFAULT
import org.rm3l.router_companion.RouterCompanionAppConstants.ROUTER_SPEED_TEST_MAX_FILE_SIZE_MB
import org.rm3l.router_companion.RouterCompanionAppConstants.ROUTER_SPEED_TEST_MAX_FILE_SIZE_MB_DEFAULT
import org.rm3l.router_companion.RouterCompanionAppConstants.ROUTER_SPEED_TEST_SERVER_AUTO
import org.rm3l.router_companion.RouterCompanionAppConstants.ROUTER_SPEED_TEST_SERVER_RANDOM
import org.rm3l.router_companion.actions.PingFromRouterAction
import org.rm3l.router_companion.actions.activity.PingRTT
import org.rm3l.router_companion.actions.activity.SpeedTestActivity.EQUAL_SPLITTER
import org.rm3l.router_companion.actions.activity.SpeedTestActivity.HTTP_DL_URL
import org.rm3l.router_companion.actions.activity.SpeedTestActivity.HTTP_UL_URL
import org.rm3l.router_companion.actions.activity.SpeedTestActivity.PING_SERVER
import org.rm3l.router_companion.actions.activity.SpeedTestActivity.RANDOM
import org.rm3l.router_companion.actions.activity.SpeedTestActivity.SERVERS
import org.rm3l.router_companion.actions.activity.SpeedTestActivity.SLASH_SPLITTER
import org.rm3l.router_companion.exceptions.SpeedTestException
import org.rm3l.router_companion.job.RouterCompanionJob
import org.rm3l.router_companion.job.speedtest.RouterSpeedTestAutoRunnerJob.Companion.EVERY_12_HOURS
import org.rm3l.router_companion.job.speedtest.RouterSpeedTestAutoRunnerJob.Companion.EVERY_2_DAYS
import org.rm3l.router_companion.job.speedtest.RouterSpeedTestAutoRunnerJob.Companion.EVERY_3_HOURS
import org.rm3l.router_companion.job.speedtest.RouterSpeedTestAutoRunnerJob.Companion.EVERY_6_HOURS
import org.rm3l.router_companion.job.speedtest.RouterSpeedTestAutoRunnerJob.Companion.EVERY_HOUR
import org.rm3l.router_companion.job.speedtest.RouterSpeedTestAutoRunnerJob.Companion.MONTHLY
import org.rm3l.router_companion.job.speedtest.RouterSpeedTestAutoRunnerJob.Companion.WEEKLY
import org.rm3l.router_companion.mgmt.RouterManagementActivity
import org.rm3l.router_companion.mgmt.RouterManagementActivity.ROUTER_SELECTED
import org.rm3l.router_companion.resources.SpeedTestResult
import org.rm3l.router_companion.resources.conn.Router
import org.rm3l.router_companion.utils.ReportingUtils
import org.rm3l.router_companion.utils.SSHUtils
import org.rm3l.router_companion.utils.Utils
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Created by rm3l on 26/10/17.
 */
class RouterSpeedTestAutoRunnerJob {

    companion object {
        val LOG_TAG = RouterSpeedTestAutoRunnerJob::class.java.simpleName!!

        @JvmField
        val JOB_TAG_PREFIX = RouterSpeedTestAutoRunnerJob::class.java.simpleName!!

        @JvmField
        val JOB_TAG_SEPARATOR = "::"

        /*
        <string-array name="speedtest_auto_schedule_values">
            <item>1H</item>
            <item>3H</item>
            <item>6H</item>
            <item>12H</item>
            <item>1D</item>
            <item>2D</item>
            <item>1W</item>
            <item>1M</item>
        </string-array>
         */
        const val EVERY_HOUR = "1H"
        const val EVERY_3_HOURS = "3H"
        const val EVERY_6_HOURS = "6H"
        const val EVERY_12_HOURS = "12H"
        const val DAILY = "1D"
        const val EVERY_2_DAYS = "2D"
        const val WEEKLY = "1W"
        const val MONTHLY = "1M"

        private fun cancelAllSchedules(routerUuid: String) {
            JobManager.instance().cancelAllForTag(
                    getActualRouterJobTag(RouterSpeedTestRunnerDailyJob.TAG, routerUuid))
            JobManager.instance().cancelAllForTag(
                    getActualRouterJobTag(RouterSpeedTestRunnerPeriodicJob.TAG, routerUuid))
        }

        @JvmStatic
        fun getActualRouterJobTag(jobTag: String, routerUuid: String) =
                "$JOB_TAG_PREFIX$jobTag$JOB_TAG_SEPARATOR$routerUuid"

        @JvmStatic
        fun schedule(routerUuid: String, autoFlag: Boolean, schedule: String) {
            cancelAllSchedules(routerUuid)
            //This is a premium feature
            if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
                Crashlytics.log(Log.DEBUG, LOG_TAG, "Speed Test auto measures feature is *premium*!")
                return
            }

            val extras = PersistableBundleCompat()
            extras.putString(ROUTER_SELECTED, routerUuid)

            if (autoFlag) {
                when (schedule) {
                    DAILY -> RouterSpeedTestRunnerDailyJob.schedule(routerUuid, extras)
                    EVERY_HOUR, EVERY_3_HOURS, EVERY_6_HOURS, EVERY_12_HOURS, EVERY_2_DAYS, WEEKLY, MONTHLY ->
                        RouterSpeedTestRunnerPeriodicJob.schedule(routerUuid, schedule, extras)
                    else -> throw IllegalArgumentException("Illegal schedule: $schedule")
                }
            }
        }

        @JvmStatic
        @Throws(Exception::class)
        fun runPing(context: Context, mOriginalRouter: Router, mRouterCopy: Router, server: String?): PingRTT {
            Crashlytics.log(Log.DEBUG, LOG_TAG, "runPing: " + server)

            if (server.isNullOrBlank()) {
                throw IllegalArgumentException("No Server specified")
            }

            if (Utils.isDemoRouter(mOriginalRouter)) {
                return PingRTT().setStddev(0.01f)
                        .setPacketLoss(Random().nextInt(100).toFloat())
                        .setMin(Random().nextFloat())
                        .setMax(Random().nextFloat() * 1024)
                        .setAvg(Random().nextFloat() * 512)
            }

            val pingOutput = SSHUtils.getManualProperty(context, mRouterCopy, null,
                    String.format(Locale.US, PingFromRouterAction.PING_CMD_TO_FORMAT,
                            PingFromRouterAction.MAX_PING_PACKETS_TO_SEND, server))
            if (pingOutput == null || pingOutput.size < 2) {
                //Nothing - abort right now with an error message
                throw SpeedTestException("Unable to contact remote server")
            }

            val pingRTT = PingRTT()

            val packetsStatusLine = pingOutput[pingOutput.size - 2]
            val packetsStatusLineSplitList = Splitter.on(",").trimResults().omitEmptyStrings().splitToList(packetsStatusLine)
            if (packetsStatusLineSplitList.size >= 3) {
                val packetLossStr = packetsStatusLineSplitList[2]
                if (packetLossStr.isNotBlank()) {
                    try {
                        pingRTT.packetLoss = Integer.parseInt(packetLossStr.replace("% packet loss".toRegex(), "")).toFloat()
                    } catch (nfe: NumberFormatException) {
                        nfe.printStackTrace()
                        //No worries
                    }

                }
            }

            val pingRttOutput = pingOutput[pingOutput.size - 1]
            val pingRttOutputList = EQUAL_SPLITTER.splitToList(pingRttOutput)
            if (pingRttOutputList.size < 2) {
                throw SpeedTestException("Unable to contact remote server")
            }
            val pingRtt = pingRttOutputList[1].replace("ms".toRegex(), "").trim({ it <= ' ' })
            val pingRttSplitResult = SLASH_SPLITTER.splitToList(pingRtt)
            val size = pingRttSplitResult.size
            if (size >= 1) {
                pingRTT.min = java.lang.Float.parseFloat(pingRttSplitResult[0])
            }
            if (size >= 2) {
                pingRTT.avg = java.lang.Float.parseFloat(pingRttSplitResult[1])
            }
            if (size >= 3) {
                pingRTT.max = java.lang.Float.parseFloat(pingRttSplitResult[2])
            }
            return pingRTT
        }

        @JvmStatic
        @Throws(Exception::class)
        fun doRunSpeedTest(context: Context, routerUuid: String?) {
            val executionDate = Date()
            Crashlytics.log(Log.DEBUG, LOG_TAG, "executionDate: " + executionDate)
            if (routerUuid == null) {
                throw IllegalArgumentException("routerUuid must not be NULL")
            }
            try {
                ReportingUtils.reportEvent(ReportingUtils.EVENT_SPEEDTEST, mapOf("Action" to "Run"))
            } catch (e: Exception) {
                //No worries
            }

            val mDao = RouterManagementActivity.getDao(context)
            val mOriginalRouter = mDao.getRouter(routerUuid)
            if (mOriginalRouter == null || mOriginalRouter.isArchived) {
                Crashlytics.log(Log.WARN, LOG_TAG, "router NOT found (NULL or archived): $routerUuid")
                return
            }
            val isDemoRouter = Utils.isDemoRouter(mOriginalRouter)
            if (isDemoRouter || BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
                if (mDao.getSpeedTestResultsByRouter(mOriginalRouter.uuid).size >= MAX_ROUTER_SPEEDTEST_RESULTS_FREE_VERSION) {
                    if (isDemoRouter) {
                        Crashlytics.log(Log.DEBUG, LOG_TAG, "You cannot have more than $MAX_ROUTER_SPEEDTEST_RESULTS_FREE_VERSION " +
                                "Speed Test results for the Demo Router: $routerUuid")
                    } else {
                        Crashlytics.log(Log.DEBUG, LOG_TAG, "[PREMIUM] Save more SpeedTest runs: $routerUuid")
                    }
                    return
                }
            }

            val mRouterCopy = Router(context, mOriginalRouter).setUuid(UUID.randomUUID().toString())
            try {
                val mRouterPreferences = mOriginalRouter.getPreferences(context)
                val serverSetting = mRouterPreferences?.getString(ROUTER_SPEED_TEST_AUTO_MEASUREMENTS_SERVER,
                        ROUTER_SPEED_TEST_AUTO_MEASUREMENTS_SERVER_DEFAULT)
                var server: String? = null
                val speedTestResult: SpeedTestResult?

                var wanDLSpeedUrlToFormat: String? = null
                var wanULSpeedUrlToFormat: String? = null
                var pingServerCountry = serverSetting
                var wanLatencyResults: PingRTT? = null
                if (ROUTER_SPEED_TEST_SERVER_AUTO == serverSetting) {
                    //TODO Remove this block now that users can pick a server from a separate list
                    // (intended for auto-measurements solely)
                    // Iterate over each server to determine the closest one,
                    // in terms of ping latency
                    var minLatency = java.lang.Float.MAX_VALUE
                    var serverCountry: String? = null

                    for (entry in SERVERS.rowMap().entries) {
                        val country = entry.key
                        val value = entry.value
                        val pingServer = value[PING_SERVER]
                        wanDLSpeedUrlToFormat = value[HTTP_DL_URL]
                        wanULSpeedUrlToFormat = value[HTTP_UL_URL]
                        if (pingServer.isNullOrBlank()) {
                            continue
                        }

                        val pingRTT = runPing(context, mOriginalRouter, mRouterCopy, pingServer)
                        val avg = pingRTT.avg
                        if (avg < 0) {
                            continue
                        }
                        if (avg <= minLatency) {
                            minLatency = avg
                            server = pingServer
                            serverCountry = country
                            wanLatencyResults = pingRTT
                        }
                    }
                    pingServerCountry = serverCountry
                } else {
//                    pingServerCountry = serverSetting
                    if (ROUTER_SPEED_TEST_SERVER_RANDOM == serverSetting) {
                        //TODO Remove this block now that users can pick a server from a separate list
                        //Pick one randomly
                        val rowKeySet = SERVERS.rowKeySet()
                        pingServerCountry = Lists.newArrayList<String>(rowKeySet)[RANDOM.nextInt(rowKeySet.size)]
                    }
                    server = SERVERS.get(pingServerCountry, PING_SERVER)
                    wanDLSpeedUrlToFormat = SERVERS.get(pingServerCountry, HTTP_DL_URL)
                    wanULSpeedUrlToFormat = SERVERS.get(pingServerCountry, HTTP_UL_URL)
                }

                if (server.isNullOrBlank() || wanDLSpeedUrlToFormat.isNullOrBlank()) {
                    throw SpeedTestException("Invalid server")
                }

                speedTestResult = SpeedTestResult()

                if (wanLatencyResults == null) {
                    wanLatencyResults = runPing(context, mOriginalRouter, mRouterCopy, server)
                }
                speedTestResult.setWanPingRTT(wanLatencyResults)

                //WAN DL / UL: algorithm here: https://speedof.me/howitworks.html
                val userDefinedRouterSpeedTestMaxFileSizeMB = mRouterPreferences?.getLong(ROUTER_SPEED_TEST_MAX_FILE_SIZE_MB,
                        ROUTER_SPEED_TEST_MAX_FILE_SIZE_MB_DEFAULT) ?: ROUTER_SPEED_TEST_MAX_FILE_SIZE_MB_DEFAULT
                val userDefinedRouterSpeedTestDurationThresholdSeconds =
                        mRouterPreferences?.getString(ROUTER_SPEED_TEST_DURATION_THRESHOLD_SECONDS,
                                ROUTER_SPEED_TEST_DURATION_THRESHOLD_SECONDS_DEFAULT)?.toLong() ?:
                                ROUTER_SPEED_TEST_DURATION_THRESHOLD_SECONDS_DEFAULT.toLong()

                var pairAcceptedForComputation: Pair<Long, Long>? = null

                var mPossibleFileSizes: Array<Long?>? = null
                val maxFileSizeValuesStrArr = context.resources.getStringArray(R.array.routerSpeedTestMaxFileSize_values)
                mPossibleFileSizes = arrayOfNulls(maxFileSizeValuesStrArr.size)
                var i = 0
                try {
                    for (maxFileSizeValuesStr in maxFileSizeValuesStrArr) {
                        mPossibleFileSizes[i++] = java.lang.Long.parseLong(maxFileSizeValuesStr)
                    }
                } catch (nfe: NumberFormatException) {
                    nfe.printStackTrace()
                    Utils.reportException(context, nfe)
                    return
                }

                if (mPossibleFileSizes.isEmpty()) {
                    mPossibleFileSizes = arrayOf(100L)
                    Utils.reportException(context,
                            SpeedTestException("R.array.routerSpeedTestMaxFileSize_values is NULL or empty"))
                }
                Arrays.sort(mPossibleFileSizes)

                Crashlytics.log(Log.DEBUG, LOG_TAG,
                        "mPossibleFileSizes: " + Arrays.toString(mPossibleFileSizes))

                for (possibleFileSize in mPossibleFileSizes) {
                    //Measure time to download file of the specified type
                    //
                    //                    final String remoteFileName = getRemoteFileName(possibleFileSize);

                    val remoteFileName = java.lang.Long.toString(possibleFileSize!!)

                    @SuppressLint("DefaultLocale") val completeServerUrl = String.format("%s?_=%d", String.format(wanDLSpeedUrlToFormat ?: "", remoteFileName),
                            System.currentTimeMillis())

                    val cmdExecOutput: Array<String>?
                    if (Utils.isDemoRouter(mOriginalRouter)) {
                        cmdExecOutput = arrayOf(Integer.toString(Math.min(77, Random().nextInt(possibleFileSize.toInt()))), Integer.toString(Random().nextInt(1)))
                    } else {
                        cmdExecOutput = SSHUtils.getManualProperty(context, mRouterCopy,
                                context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE),
                                Joiner.on(" && ").skipNulls(), "DATE_START=$(/bin/date +\"%s\")",
                                //seconds since 1970-01-01 00:00:00 UTC
                                String.format("/usr/bin/wget -qO /dev/null \"%s\" > /dev/null 2>&1 ",
                                        completeServerUrl), "DATE_END=$(/bin/date +\"%s\")",
                                //seconds since 1970-01-01 00:00:00 UTC
                                "/bin/echo $((\${DATE_END}-\${DATE_START}))", //number of seconds
                                "/bin/echo $?")
                    }

                    if (cmdExecOutput == null || cmdExecOutput.size < 2 || "0" != Strings.nullToEmpty(cmdExecOutput[cmdExecOutput.size - 1]).trim({ it <= ' ' })) {
                        val speedTestException = SpeedTestException("Failed to download data: " + remoteFileName + "MB")
                        Crashlytics.logException(speedTestException)
                        throw speedTestException
                    }

                    val elapsedSeconds: Long
                    try {
                        elapsedSeconds = java.lang.Long.parseLong(Strings.nullToEmpty(cmdExecOutput[cmdExecOutput.size - 2]).trim({ it <= ' ' }))
                    } catch (nfe: NumberFormatException) {
                        Crashlytics.logException(nfe)
                        throw SpeedTestException("Unexpected output - please try again later.")
                    }

                    if (elapsedSeconds < 0) {
                        throw SpeedTestException("Unexpected output - please try again later.")
                    }

                    Crashlytics.log(Log.DEBUG, LOG_TAG, String.format(Locale.US,
                            "[SpeedTest] Downloaded %d MB of data in %d seconds. Download URL is: \"%s\"",
                            possibleFileSize, elapsedSeconds, completeServerUrl))

                    speedTestResult.setWanDLFileSize(possibleFileSize)
                    speedTestResult.setWanDLDuration(elapsedSeconds)

                    pairAcceptedForComputation = Pair.create(possibleFileSize, elapsedSeconds)
                    //Stop conditions: time_to_dl >= threshold or fileSize >= possibleFileSize
                    if (possibleFileSize >= userDefinedRouterSpeedTestMaxFileSizeMB || elapsedSeconds >= userDefinedRouterSpeedTestDurationThresholdSeconds) {
                        break
                    }
                }

                if (pairAcceptedForComputation != null) {
                    val timeElapsedSeconds = pairAcceptedForComputation.second
                    val wanDl = if (timeElapsedSeconds != null && timeElapsedSeconds != 0L)
                        (pairAcceptedForComputation.first as Long) * 1024 * 1024 / timeElapsedSeconds
                    else
                        (pairAcceptedForComputation.first as Long) * 1024 * 1024
                    speedTestResult.setWanDl(wanDl)
                }

                //3- WAN UL
                //                publishProgress(TEST_WAN_UL);
                //TODO //FIXME Use real data
                speedTestResult.setWanUl(Random().nextInt(27) * 1024 xor 5)

                //Persist speed test result
                val speedTestResultToPersist = SpeedTestResult(mOriginalRouter.uuid,
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(executionDate),
                        server ?: "", speedTestResult.getWanPing()!!, speedTestResult.getWanDl()!!,
                        speedTestResult.getWanUl()!!, null, null, null, pingServerCountry)
                val speedTestResultToPersistWanPingRTT = PingRTT()
                if (speedTestResult.getWanPing() != null) {
                    speedTestResultToPersistWanPingRTT.avg = speedTestResult.getWanPing()!!.toFloat()
                }
                if (speedTestResult.getWanPingRTT() != null) {
                    speedTestResultToPersistWanPingRTT.packetLoss = speedTestResult.getWanPingRTT()!!.packetLoss
                    speedTestResultToPersistWanPingRTT.stddev = speedTestResult.getWanPingRTT()!!.stddev
                    speedTestResultToPersistWanPingRTT.max = speedTestResult.getWanPingRTT()!!.max
                    speedTestResultToPersistWanPingRTT.min = speedTestResult.getWanPingRTT()!!.min
                }
                speedTestResultToPersist.setWanPingRTT(speedTestResultToPersistWanPingRTT)

                speedTestResultToPersist.setWanDLFileSize(speedTestResult.getWanDLFileSize())
                speedTestResultToPersist.setWanDLDuration(speedTestResult.getWanDLDuration())
                speedTestResultToPersist.setWanULFileSize(speedTestResult.getWanULFileSize())
                speedTestResultToPersist.setWanULDuration(speedTestResult.getWanULDuration())

                speedTestResultToPersist.setConnectionDLFileSize(
                        speedTestResult.getConnectionDLFileSize())
                speedTestResultToPersist.setConnectionDLDuration(
                        speedTestResult.getConnectionDLDuration())
                speedTestResultToPersist.setConnectionULFileSize(
                        speedTestResult.getConnectionULFileSize())
                speedTestResultToPersist.setConnectionULDuration(
                        speedTestResult.getConnectionULDuration())

                mDao.insertSpeedTestResult(speedTestResultToPersist)

                //Request Backup
                Utils.requestBackup(context)

            } finally {
                mRouterCopy.destroyAllSessions()
            }

        }
    }
}

class RouterSpeedTestRunnerDailyJob : DailyJob(), RouterCompanionJob {

    override fun isOneShotJob() = false

    companion object {
        @JvmField
        val TAG = RouterSpeedTestRunnerDailyJob::class.java.simpleName!!

        @JvmStatic
        fun schedule(routerUuid: String, extras: PersistableBundleCompat) {
            val builder = JobRequest.Builder(RouterSpeedTestAutoRunnerJob.getActualRouterJobTag(TAG, routerUuid))
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
//                    .setRequiresCharging(true)
                    .setExtras(extras)
            // run job between 1am and 11pm
            DailyJob.schedule(builder,
                    TimeUnit.HOURS.toMillis(1),
                    TimeUnit.HOURS.toMillis(23))
        }
    }

    override fun onRunDailyJob(params: Params): DailyJobResult {
        try {
            val routerUuid = params.extras.getString(ROUTER_SELECTED, null) ?: return CANCEL
            val router = RouterManagementActivity.getDao(context).getRouter(routerUuid)
            if (router == null || router.isArchived) {
                Crashlytics.log(Log.WARN, TAG, "router is NULL or archived => cancelling daily job")
                return CANCEL
            }
            RouterSpeedTestAutoRunnerJob.doRunSpeedTest(context, routerUuid)
        } catch (e: Exception) {
            //Reschedule
            Crashlytics.logException(e)
        }
        return SUCCESS
    }
}

class RouterSpeedTestRunnerPeriodicJob : Job(), RouterCompanionJob {

    override fun isOneShotJob() = false

    companion object {
        val TAG = RouterSpeedTestRunnerPeriodicJob::class.java.simpleName!!

        @JvmStatic
        fun schedule(routerUuid: String, schedule: String, extras: PersistableBundleCompat) {
            val intervalMs =
                    when (schedule) {
                        EVERY_HOUR -> TimeUnit.HOURS.toMillis(1L)
                        EVERY_3_HOURS -> TimeUnit.HOURS.toMillis(3L)
                        EVERY_6_HOURS -> TimeUnit.HOURS.toMillis(6L)
                        EVERY_12_HOURS -> TimeUnit.HOURS.toMillis(12L)
                        EVERY_2_DAYS -> TimeUnit.DAYS.toMillis(2L)
                        WEEKLY -> TimeUnit.DAYS.toMillis(7L)
                        MONTHLY -> TimeUnit.DAYS.toMillis(31L)
                        else -> null
                    }
            Crashlytics.log(Log.WARN, TAG, "intervalMs : $intervalMs")
            if (intervalMs == null) {
                Crashlytics.log(Log.WARN, TAG, "intervalMs is NULL => nothing scheduled")
                return
            }
            JobRequest.Builder(RouterSpeedTestAutoRunnerJob.getActualRouterJobTag(TAG, routerUuid))
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
//                    .setRequiresCharging(true)
                    .setExtras(extras)
                    .setPeriodic(intervalMs, TimeUnit.MINUTES.toMillis(5))
                    .build()
                    .schedule()
        }
    }

    override fun onRunJob(params: Params): Result {
        try {
            val routerUuid = params.extras.getString(ROUTER_SELECTED, null) ?: return Result.FAILURE
            val router = RouterManagementActivity.getDao(context).getRouter(routerUuid)
            if (router == null || router.isArchived) {
                Crashlytics.log(Log.WARN, TAG, "router is NULL or archived => cancelling periodic job")
                return Result.FAILURE
            }
            RouterSpeedTestAutoRunnerJob.doRunSpeedTest(context,
                    params.extras.getString(ROUTER_SELECTED, null))
        } catch (e: Exception) {
            //Reschedule
            Crashlytics.logException(e)
        }
        return Result.SUCCESS
    }
}


class RouterSpeedTestRunnerOneShotJob : Job(), RouterCompanionJob {
    override fun onRunJob(params: Params): Result {
        return try {
            RouterManagementActivity.getDao(context).allRouters.forEach {
                try {
                    RouterSpeedTestAutoRunnerJob.doRunSpeedTest(context, it.uuid)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            Result.SUCCESS
        } catch (e: Exception) {
            FAILURE
        }
    }

    companion object {
        val TAG = RouterSpeedTestRunnerOneShotJob::class.java.simpleName!!
    }
}