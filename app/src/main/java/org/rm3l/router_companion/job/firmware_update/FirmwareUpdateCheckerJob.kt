package org.rm3l.router_companion.job.firmware_update

import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.evernote.android.job.DailyJob
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import needle.UiRelatedTask
import org.rm3l.ddwrt.BuildConfig
import org.rm3l.ddwrt.R
import org.rm3l.router_companion.RouterCompanionAppConstants
import org.rm3l.router_companion.RouterCompanionAppConstants.CLOUD_MESSAGING_TOPIC_DDWRT_BUILD_UPDATES
import org.rm3l.router_companion.RouterCompanionAppConstants.NOTIFICATIONS_CHOICE_PREF
import org.rm3l.router_companion.RouterCompanionAppConstants.NOTIFICATIONS_ENABLE
import org.rm3l.router_companion.api.urlshortener.firebase.dynamiclinks.resources.DynamicLinkInfo
import org.rm3l.router_companion.api.urlshortener.firebase.dynamiclinks.resources.ShortLinksDataRequest
import org.rm3l.router_companion.common.utils.ExceptionUtils
import org.rm3l.router_companion.firmwares.FirmwareRelease
import org.rm3l.router_companion.firmwares.NoNewFirmwareUpdate
import org.rm3l.router_companion.firmwares.RouterFirmwareConnectorManager
import org.rm3l.router_companion.job.RouterCompanionJob
import org.rm3l.router_companion.main.DDWRTMainActivity.SAVE_ROUTER_SELECTED
import org.rm3l.router_companion.mgmt.RouterManagementActivity
import org.rm3l.router_companion.mgmt.RouterManagementActivity.Companion.ROUTER_SELECTED
import org.rm3l.router_companion.mgmt.dao.DDWRTCompanionDAO
import org.rm3l.router_companion.multithreading.MultiThreadingManager
import org.rm3l.router_companion.resources.conn.NVRAMInfo
import org.rm3l.router_companion.resources.conn.Router
import org.rm3l.router_companion.tiles.status.router.StatusRouterStateTile
import org.rm3l.router_companion.utils.FirebaseUtils
import org.rm3l.router_companion.utils.NetworkUtils
import org.rm3l.router_companion.utils.Utils
import org.rm3l.router_companion.utils.customtabs.CustomTabActivityHelper
import org.rm3l.router_companion.utils.notifications.NOTIFICATION_GROUP_GENERAL_UPDATES
import org.rm3l.router_companion.utils.snackbar.SnackbarCallback
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils
import org.rm3l.router_companion.web.WebActivity
import java.util.Date
import java.util.concurrent.TimeUnit

const val LAST_RELEASE_CHECKED = "lastReleaseChecked"
const val MANUAL_REQUEST = "MANUAL_REQUEST"

class FirmwareUpdateCheckerJob : DailyJob(), RouterCompanionJob {

    companion object {
        @JvmField
        val TAG = FirmwareUpdateCheckerJob::class.java.simpleName

        private const val LATEST_FIRMWARE_RELEASE_NOTIFIED_PREF = "latestFirmwareReleaseNotified"

        @JvmStatic
        fun schedule() {
            // This is a premium feature
            if (BuildConfig.DONATIONS) {
                FirebaseCrashlytics.getInstance().log("Firmware Build Updates feature is *premium*!")
                return
            }

            if (JobManager.instance().getAllJobRequestsForTag(TAG).isNotEmpty()) {
                // job already scheduled, nothing to do
                FirebaseCrashlytics.getInstance().log("job $TAG already scheduled => nothing to do!")
                return
            }
            val builder = JobRequest.Builder(TAG)
                .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
//                    .setRequiresCharging(true)
            // run job between 9am and 9pm
            schedule(
                builder,
                TimeUnit.HOURS.toMillis(9),
                TimeUnit.HOURS.toMillis(21)
            )
        }

        @JvmStatic
        fun manualCheckForFirmwareUpdate(
            activity: Activity,
            router: Router
        ) {
            val alertDialog = ProgressDialog.show(
                activity, "Checking for firmware updates",
                "Please wait...",
                true
            )
            MultiThreadingManager.getWebTasksExecutor().execute(
                object : UiRelatedTask<Pair<String?, Exception?>>() {
                    private var mNewerRelease: FirmwareRelease? = null
                    override fun doWork(): Pair<String?, Exception?> {
                        // First determine current version
                        try {
                            @Suppress("USELESS_ELVIS")
                            val nvramInfo = RouterFirmwareConnectorManager.getConnector(router)
                                .getDataFor(
                                    activity, router,
                                    StatusRouterStateTile::class.java, null
                                )
                                ?: throw IllegalStateException("Could not retrieve local data")

                            val currentFwVer = nvramInfo.getProperty(
                                NVRAMInfo.OS_VERSION,
                                ""
                            )
                            if (currentFwVer.isNullOrBlank()) {
                                throw IllegalStateException("Could not retrieve current firmware version")
                            }
                            mNewerRelease = RouterFirmwareConnectorManager.getConnector(router)
                                .manuallyCheckForFirmwareUpdateAndReturnDownloadLink(currentFwVer)
                            if (mNewerRelease == null) {
                                // No new update
                                throw IllegalStateException("Could not retrieve current firmware version")
                            }
                            var newReleaseDLLink: String? = mNewerRelease!!.getDirectLink()
                            if (newReleaseDLLink != null && Utils.isNonDemoRouter(router)) {
                                var shortened = false
                                if (NetworkUtils.getFirebaseDynamicLinksService() != null) {
                                    try {
                                        val shortLinksDataRequest = ShortLinksDataRequest(
                                            dynamicLinkInfo = DynamicLinkInfo(link = newReleaseDLLink)
                                        )
                                        val response = NetworkUtils.getFirebaseDynamicLinksService().shortLinks(
                                            FirebaseUtils.getFirebaseApiKey(activity),
                                            shortLinksDataRequest
                                        ).execute()
                                        NetworkUtils.checkResponseSuccessful(response)
                                        newReleaseDLLink = response.body()!!.shortLink
                                        shortened = true
                                    } catch (e: Exception) {
                                        // Do not worry about that => fallback to the original DL link
                                        shortened = false
                                    }
                                }
                                if (!shortened) {
                                    if (NetworkUtils.getIsGdService() != null) {
                                        try {
                                            val response = NetworkUtils.getIsGdService()
                                                .shortLinks(newReleaseDLLink!!).execute()
                                            NetworkUtils.checkResponseSuccessful(response)
                                            newReleaseDLLink = response.body()!!.shorturl
                                            shortened = true
                                        } catch (e1: Exception) {
                                            // Do not worry about that => fallback to the original DL link
                                            shortened = false
                                        }
                                    }
                                }
                            }
                            return newReleaseDLLink to null
                        } catch (e: Exception) {
                            FirebaseCrashlytics.getInstance().recordException(e)
                            return null to e
                        }
                    }

                    override fun thenDoUiRelatedWork(result: Pair<String?, Exception?>?) {
                        FirebaseCrashlytics.getInstance().log("result: $result")
                        alertDialog.cancel()
                        if (result == null) {
                            Utils.displayMessage(
                                activity,
                                "Internal Error. Please try again later.",
                                SnackbarUtils.Style.ALERT
                            )
                            return
                        }
                        val exception = result.second
                        if (exception != null) {
                            when (exception) {
                                is NoNewFirmwareUpdate -> Utils.displayMessage(
                                    activity,
                                    "Your router (${router.canonicalHumanReadableName}) is up-to-date.",
                                    SnackbarUtils.Style.CONFIRM
                                )
                                else -> Utils.displayMessage(
                                    activity,
                                    "Could not check for update: ${ExceptionUtils.getRootCause(exception).message}",
                                    SnackbarUtils.Style.ALERT
                                )
                            }
                        } else if (mNewerRelease != null && result.first != null) {
                            val routerFirmware = router.routerFirmware
                            SnackbarUtils.buildSnackbar(
                                activity,
                                activity.findViewById(android.R.id.content),
                                ContextCompat.getColor(activity, R.color.win8_blue),
                                "A new ${routerFirmware?.officialName ?: ""} Build (${mNewerRelease!!.version}) is available for '${router.canonicalHumanReadableName}'",
                                Color.WHITE,
                                "View", // TODO Reconsider once we have an auto-upgrade firmware feature. Add link to perform the upgrade right away
                                Color.YELLOW,
                                Snackbar.LENGTH_LONG,
                                object : SnackbarCallback {
                                    @Throws(Exception::class)
                                    override fun onDismissEventActionClick(event: Int, bundle: Bundle?) {
                                        if (result.first == null) {
                                            Toast.makeText(
                                                activity, "Internal Error - please try again later",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            FirebaseCrashlytics.getInstance().recordException(FirmwareUpdateFoundButWithNoUrlException("Firmware update: no URL"))
                                        } else {
                                            val url = result.first!!
                                            CustomTabActivityHelper.openCustomTab(
                                                activity,
                                                null, url, router.uuid, null,
                                                { activity, _ ->
                                                    // Otherwise, default to a classic WebView implementation
                                                    val intent = Intent(
                                                        activity,
                                                        FirmwareReleaseDownloadPageActivity::class.java
                                                    )
                                                    intent.putExtra(ROUTER_SELECTED, router.uuid)
                                                    intent.putExtra(FirmwareReleaseDownloadPageActivity.RELEASE_URL, url)
                                                    activity.startActivity(intent)
                                                },
                                                false
                                            )
                                        }
                                    }
                                },
                                null, true
                            )
                        } else {
                            Utils.displayMessage(
                                activity,
                                "Internal Error. Please try again later.",
                                SnackbarUtils.Style.ALERT
                            )
                        }
                    }
                })
        }

        @JvmStatic
        fun handleJob(context: Context, params: Params?): Boolean {
            val routerDao = RouterManagementActivity.getDao(context)
            val globalPreferences = context.getSharedPreferences(
                RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE
            )
            val firebaseDynamicLinksService = NetworkUtils.getFirebaseDynamicLinksService()
            val isGdService = NetworkUtils.getIsGdService()

            // First check if user is interested in getting updates
            val notificationChoices = globalPreferences?.getStringSet(NOTIFICATIONS_CHOICE_PREF, emptySet())
            if (notificationChoices?.contains(CLOUD_MESSAGING_TOPIC_DDWRT_BUILD_UPDATES) != true) {
                FirebaseCrashlytics.getInstance().log("Not interested at this time in Firmware Build Updates!")
                // Check next day
                return true
            }

            val firebaseApiKey = FirebaseUtils.getFirebaseApiKey(context)

            // Now keep only routers for which the user has accepted notifications
            val forceCheck = params?.extras?.getBoolean(MANUAL_REQUEST, false)
            val releaseAndGooGlLinksMap: MutableMap<String, String?> = mutableMapOf()
            routerDao.allRouters
                .filter { it.getPreferences(context)?.getBoolean(NOTIFICATIONS_ENABLE, true) == true }
                .map { router ->
                    val mapped = router to try {
                        val nvramInfo = RouterFirmwareConnectorManager.getConnector(router)
                            .getDataFor(context, router, StatusRouterStateTile::class.java, null)
                        //noinspection ConstantConditions
                        val currentFwVer = nvramInfo.getProperty(NVRAMInfo.OS_VERSION, "")!!.trim()
                        if (currentFwVer.isBlank()) null
                        else RouterFirmwareConnectorManager.getConnector(router)
                            .manuallyCheckForFirmwareUpdateAndReturnDownloadLink(currentFwVer)
                    } catch (e: Exception) {
                        FirebaseCrashlytics.getInstance().recordException(e)
                        null
                    }
                    mapped
                }
                .filter { routerAndNewestReleaseUpdatePair -> routerAndNewestReleaseUpdatePair.second != null }
                .filter { routerAndNewestReleaseUpdatePair ->
                    val router = routerAndNewestReleaseUpdatePair.first
                    val release = routerAndNewestReleaseUpdatePair.second
                    val routerPreferences = router.getPreferences(context)
                    val newReleaseVersion = release!!.version
                    if (forceCheck == true) {
                        true
                    } else {
                        val toProcess = routerPreferences?.getString(
                            LAST_RELEASE_CHECKED,
                            ""
                        )!! != newReleaseVersion
                        if (toProcess) {
                            routerPreferences.edit().putString(LAST_RELEASE_CHECKED, newReleaseVersion).apply()
                            Utils.requestBackup(context)
                        }
                        toProcess
                    }
                }
                .forEach { routerAndNewestReleaseUpdatePair ->
                    try {
                        val router = routerAndNewestReleaseUpdatePair.first
                        val release = routerAndNewestReleaseUpdatePair.second
                        val newReleaseVersion = release!!.version
                        val newReleaseDownloadLink = release.getDirectLink()
                        val downloadLink = releaseAndGooGlLinksMap.getOrPut(
                            "${router.routerFirmware!!.name}.$newReleaseVersion",
                            {
                                var newReleaseDLLink: String? = newReleaseDownloadLink
                                if (newReleaseDLLink != null && Utils.isNonDemoRouter(router)) {
                                    var shortened: Boolean
                                    try {
                                        val shortLinksDataRequest = ShortLinksDataRequest(
                                            dynamicLinkInfo = DynamicLinkInfo(link = newReleaseDLLink)
                                        )
                                        val response = firebaseDynamicLinksService.shortLinks(
                                            firebaseApiKey,
                                            shortLinksDataRequest
                                        ).execute()
                                        NetworkUtils.checkResponseSuccessful(response)
                                        newReleaseDLLink = response.body()!!.shortLink
                                        shortened = true
                                    } catch (e: Exception) {
                                        // Do not worry about that => fallback to the original DL link
                                        shortened = false
                                    }

                                    if (!shortened) {
                                        try {
                                            val response = isGdService.shortLinks(newReleaseDLLink!!).execute()
                                            NetworkUtils.checkResponseSuccessful(response)
                                            newReleaseDLLink = response.body()!!.shorturl
                                            shortened = true
                                        } catch (e: Exception) {
                                            // Do not worry about that => fallback to the original DL link
                                            shortened = false
                                        }
                                    }
                                }
                                newReleaseDLLink
                            }
                        ) ?: newReleaseDownloadLink

                        //        Intent intent = new Intent(this, RouterManagementActivity.class);
                        //        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        //        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                        //                FCM_NOTIFICATION_ID /* Request code */, intent,
                        //                PendingIntent.FLAG_ONE_SHOT);

                        val largeIcon = Router.loadRouterAvatarUrlSync(context, router, Router.mAvatarDownloadOpts)
                        doNotify(
                            context, router,
                            largeIcon ?: BitmapFactory.decodeResource(
                                context.resources,
                                R.mipmap.ic_launcher_ddwrt_companion
                            ),
                            downloadLink, newReleaseVersion
                        )
                    } catch (e: Exception) {
                        // No worries - go on with the next
                        FirebaseCrashlytics.getInstance().recordException(e)
                    }
                }
            return true
        }

        private fun doNotify(
            context: Context,
            router: Router,
            largeIcon: Bitmap,
            downloadLink: String,
            newReleaseVersion: String?
        ) {

            if (newReleaseVersion.isNullOrBlank()) {
                FirebaseCrashlytics.getInstance().log("newReleaseVersion is NULL or blank - skipping notification.")
                return
            }

            val routerPreferences = router.getPreferences(context)
            val latestFirmwareReleaseNotified = routerPreferences?.getString(
                LATEST_FIRMWARE_RELEASE_NOTIFIED_PREF,
                null
            )
            if (newReleaseVersion.equals(latestFirmwareReleaseNotified)) {
                FirebaseCrashlytics.getInstance().log(
                    "Firmware release ${router.canonicalHumanReadableName}/$newReleaseVersion already notified" +
                        " => skipping notification."
                )
                return
            }

            // pending implicit intent to view url
            val resultIntent = Intent(Intent.ACTION_VIEW)
            resultIntent.data = Uri.parse(downloadLink)

            val pendingIntent = PendingIntent.getActivity(
                context,
                router.id /* Request code */, resultIntent,
                PendingIntent.FLAG_ONE_SHOT
            )

            //        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            val notificationBuilder = NotificationCompat.Builder(
                context,
                "$NOTIFICATION_GROUP_GENERAL_UPDATES-${router.routerFirmware!!.name}"
            )
                .setGroup(NOTIFICATION_GROUP_GENERAL_UPDATES)
                .setLargeIcon(largeIcon)
                .setSmallIcon(R.mipmap.ic_launcher_ddwrt_companion)
                .setContentTitle(
                    "A new ${router.routerFirmware!!.officialName} Build is available for '${router.canonicalHumanReadableName}'"
                )
                .setContentText(newReleaseVersion)
                .setAutoCancel(true)

            val globalPreferences = context.getSharedPreferences(
                RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE
            )

            // Notification sound, if required
            val ringtoneUri = globalPreferences.getString(
                RouterCompanionAppConstants.NOTIFICATIONS_SOUND, null
            )
            if (ringtoneUri != null) {
                notificationBuilder.setSound(
                    Uri.parse(ringtoneUri),
                    AudioManager.STREAM_NOTIFICATION
                )
            }

            if (!globalPreferences.getBoolean(
                    RouterCompanionAppConstants.NOTIFICATIONS_VIBRATE,
                    true
                )
            ) {
                notificationBuilder.setDefaults(Notification.DEFAULT_LIGHTS)
                    .setVibrate(RouterCompanionAppConstants.NO_VIBRATION_PATTERN)
                //                    if (ringtoneUri != null) {
                //                        mBuilder.setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND);
                //                    } else {
                //                        mBuilder.setDefaults(Notification.DEFAULT_LIGHTS);
                //                    }
            }
            notificationBuilder.setContentIntent(pendingIntent)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

            notificationManager.notify(
                router.id + 99999 /* ID of notification */,
                notificationBuilder.build()
            )

            routerPreferences?.edit()
                ?.putString(LATEST_FIRMWARE_RELEASE_NOTIFIED_PREF, newReleaseVersion)
                ?.apply()
            Utils.requestBackup(context)
        }
    }

    override fun isOneShotJob() = false

    override fun onRunDailyJob(params: Params): DailyJobResult {
        try {
            if (!handleJob(context, params)) {
                FirebaseCrashlytics.getInstance().log(
                    "Today (${Date()}) execution did not succeed => hopefully it will succeeed tomorrow..."
                )
            }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
        return DailyJobResult.SUCCESS
    }
}

class FirmwareUpdateCheckerOneShotJob : Job(), RouterCompanionJob {

    companion object {
        val TAG = FirmwareUpdateCheckerOneShotJob::class.java.simpleName
    }

    override fun onRunJob(params: Params): Result {
        return try {
            params.extras.putBoolean(MANUAL_REQUEST, true)
            if (FirmwareUpdateCheckerJob.handleJob(context, params)) Result.SUCCESS else Result.FAILURE
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            Result.FAILURE
        }
    }
}

class FirmwareUpdateFoundButWithNoUrlException(message: String?, throwable: Throwable? = null) :
    RuntimeException(message, throwable)

class FirmwareReleaseDownloadPageActivity : WebActivity() {

    private var mDao: DDWRTCompanionDAO? = null
    private var mUrl: String? = null
    private var mRouter: Router? = null

    companion object {
        const val RELEASE = "RELEASE"
        const val RELEASE_URL = "RELEASE_URL"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mDao = RouterManagementActivity.getDao(this)
        var uuid: String? = intent.getStringExtra(ROUTER_SELECTED)
        if (uuid == null) {
            if (savedInstanceState != null) {
                uuid = savedInstanceState.getString(SAVE_ROUTER_SELECTED)
            }
        }
        this.mRouter = mDao?.getRouter(uuid!!)
        if (this.mRouter == null) {
            Toast.makeText(this, "No router set or router no longer exists", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (intent.hasExtra(RELEASE_URL)) {
            mUrl = intent.getStringExtra(RELEASE_URL)
        }

        mToolbar.subtitle = Router.getCanonicalHumanReadableNameWithEffectiveInfo(this, this.mRouter, false)
    }

    override fun isJavascriptEnabled() = false

    override fun getUrl(): String {
        return this.mUrl ?: ""
    }

    override fun getTitleResId() = null

    override fun getTitleStr() = "Firmware Release"
}
