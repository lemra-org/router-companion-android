package org.rm3l.router_companion.service.tasks

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.util.Patterns
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.common.base.Objects
import com.google.common.base.Strings.isNullOrEmpty
import org.rm3l.ddwrt.R
import org.rm3l.router_companion.RouterCompanionAppConstants
import org.rm3l.router_companion.firmwares.RouterFirmwareConnectorManager
import org.rm3l.router_companion.main.DDWRTMainActivity
import org.rm3l.router_companion.mgmt.RouterManagementActivity.Companion.ROUTER_SELECTED
import org.rm3l.router_companion.resources.Encrypted.d
import org.rm3l.router_companion.resources.Encrypted.e
import org.rm3l.router_companion.resources.conn.NVRAMInfo.Companion.WAN_IPADDR
import org.rm3l.router_companion.resources.conn.Router
import org.rm3l.router_companion.utils.ImageUtils.updateNotificationIconWithRouterAvatar
import org.rm3l.router_companion.utils.ReportingUtils
import org.rm3l.router_companion.utils.SSHUtils
import org.rm3l.router_companion.utils.Utils

class PublicIPChangesServiceTask(ctx: Context) : AbstractBackgroundServiceTask(ctx) {
    private val routerModelUpdaterServiceTask: RouterModelUpdaterServiceTask =
            RouterModelUpdaterServiceTask(ctx)

    @Throws(Exception::class)
    override fun runBackgroundServiceTask(router: Router) {
//    val routerPreferences = mCtx.getSharedPreferences(router.uuid, Context.MODE_PRIVATE) ?: return

        val nvramInfo = SSHUtils.getNVRamInfoFromRouter(mCtx, router, globalPreferences, WAN_IPADDR)

        val wanPublicIpAddress = RouterFirmwareConnectorManager.getConnector(router)
                .getWanPublicIpAddress(mCtx, router, null)
        val wanPublicIpCmdStatus: Array<String>
        if (wanPublicIpAddress != null) {
            wanPublicIpCmdStatus = arrayOf(wanPublicIpAddress)
        } else {
            wanPublicIpCmdStatus = emptyArray()
        }

        var wanIp: String? = nvramInfo?.getProperty(WAN_IPADDR)

        if (wanIp != null && !Patterns.IP_ADDRESS.matcher(wanIp).matches()) {
            wanIp = null
        }

        try {
            routerModelUpdaterServiceTask.runBackgroundServiceTask(router)
        } catch (e: Exception) {
            Utils.reportException(mCtx, e)
            //No worries
        } finally {
            buildNotificationIfNeeded(mCtx, router, wanPublicIpCmdStatus, wanIp, null)
        }
    }

    companion object {

        val LAST_PUBLIC_IP_PREF_PREFIX = "lastPublicIp_"
        val LAST_PUBLIC_IP = "lastPublicIp"
        val LAST_WAN_IP = "lastWanIp"
        val IS_FIRST_TIME_PREF_PREFIX = "isFirstTime_"
        private val LOG_TAG = PublicIPChangesServiceTask::class.java.simpleName

        fun buildNotificationIfNeeded(
                mCtx: Context,
                router: Router,
                wanPublicIpCmdStatus: Array<String>?,
                wanIp: String?,
                exception: Exception?) {

            if (exception != null) {
                exception.printStackTrace()
                return
            }

            val globalSharedPreferences = Utils.getGlobalSharedPreferences(mCtx)

            if (!globalSharedPreferences
                    .getStringSet(RouterCompanionAppConstants.NOTIFICATIONS_CHOICE_PREF, emptySet())!!
                    .contains(PublicIPChangesServiceTask::class.java.simpleName)) {
                FirebaseCrashlytics.getInstance().log( "PublicIPChangesServiceTask notifications disabled")
                return
            }

            val routerPreferences = mCtx.getSharedPreferences(router.templateUuidOrUuid, Context.MODE_PRIVATE)

            val rawWanIpFromPrefs: String? = routerPreferences.getString(LAST_WAN_IP, null)
            val wanIpFromPrefs: String? = d(rawWanIpFromPrefs)
            val rawWanPublicIpFromPrefs: String? = routerPreferences.getString(LAST_PUBLIC_IP, null)
            val wanPublicIpFromPrefs: String? = d(rawWanPublicIpFromPrefs)

            var wanPublicIp: String?
            if (wanPublicIpCmdStatus == null || wanPublicIpCmdStatus.isEmpty()) {
                //Couldn't determine IP Address
                wanPublicIp = null
            } else {
                wanPublicIp = wanPublicIpCmdStatus[wanPublicIpCmdStatus.size - 1].trim { it <= ' ' }
                if (!Patterns.IP_ADDRESS.matcher(wanPublicIp).matches()) {
                    wanPublicIp = null
                }
            }

            FirebaseCrashlytics.getInstance().log( "(wanIpFromPrefs,wanIp)=($wanIpFromPrefs,$wanIp)")
            FirebaseCrashlytics.getInstance().log( "(wanPublicIpFromPrefs,wanPublicIp)=($wanPublicIpFromPrefs,$wanPublicIp)")

            if (!(Objects.equal(wanPublicIp, wanPublicIpFromPrefs) && Objects.equal(wanIp,
                    wanIpFromPrefs))) {

                //Save last value into preferences and display notification
                val editor = routerPreferences.edit()
                editor.putString(LAST_PUBLIC_IP, e(wanPublicIp)).putString(LAST_WAN_IP, e(wanIp))

                //Also retrieve notification ID or set one
                // Sets an ID for the notification, so it can be updated
                var notifyID = routerPreferences.getInt(LAST_PUBLIC_IP_PREF_PREFIX + router.id, -1)
                if (notifyID == -1) {
                    try {
                        notifyID = 1 + Integer.parseInt(router.id.toString() + "00" + router.id)
                        editor.putInt(LAST_PUBLIC_IP_PREF_PREFIX + router.id, notifyID)
                    } catch (e: Exception) {
                        ReportingUtils.reportException(null, e)
                        return
                    }

                }

                //Now display notification
                val notificationsEnabled = routerPreferences.getBoolean(
                        RouterCompanionAppConstants.NOTIFICATIONS_ENABLE, true)

                FirebaseCrashlytics.getInstance().log( "NOTIFICATIONS_ENABLE=" + notificationsEnabled)

                val mNotificationManager = mCtx.getSystemService(
                        Context.NOTIFICATION_SERVICE) as NotificationManager

                if (!notificationsEnabled) {
                    mNotificationManager.cancel(notifyID)
                } else {

                    val wanPublicIpNullOrEmpty = isNullOrEmpty(wanPublicIp)
                    val wanIpNullOrEmpty = isNullOrEmpty(wanIp)
                    if (!(wanPublicIpNullOrEmpty && wanIpNullOrEmpty)) {
                        val largeIcon = Router.loadRouterAvatarUrlSync(mCtx, router, Router.mAvatarDownloadOpts)
                        doNotify(mCtx, router, largeIcon?:BitmapFactory.decodeResource(mCtx.resources,
                                R.mipmap.ic_launcher_ddwrt_companion), notifyID, wanPublicIp, wanIp)
                        editor.putBoolean(IS_FIRST_TIME_PREF_PREFIX + notifyID, false).apply()
                        Utils.requestBackup(mCtx)
                    }
                }
            }
        }

        private fun doNotify(mCtx: Context, router: Router, largeIcon: Bitmap, notifyID: Int,
                             wanPublicIp: String? = null, wanIp: String? = null) {
            val resultIntent = Intent(mCtx, DDWRTMainActivity::class.java)
            resultIntent.putExtra(ROUTER_SELECTED, router.uuid)
            resultIntent.putExtra(DDWRTMainActivity.SAVE_ITEM_SELECTED,
                    1) //Open right on the Public IP status
            // Because clicking the notification opens a new ("special") activity, there's
            // no need to create an artificial back stack.
            val resultPendingIntent = PendingIntent.getActivity(mCtx, 0, resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT)

            val mRouterName = router.name
            val mRouterNameNullOrEmpty = isNullOrEmpty(mRouterName)
            var summaryText = ""
            if (!mRouterNameNullOrEmpty) {
                summaryText = mRouterName!! + " ("
            }
            summaryText += router.remoteIpAddress
            if (!mRouterNameNullOrEmpty) {
                summaryText += ")"
            }

            val mBuilder = NotificationCompat.Builder(mCtx, router.notificationChannelId)
                    .setGroup(router.uuid)
                    .setSmallIcon(R.drawable.ic_stat_ip)
                    .setLargeIcon(largeIcon)
                    .setAutoCancel(true)
                    .setGroup(PublicIPChangesServiceTask::class.java.simpleName)
                    .setGroupSummary(true)
                    .setContentIntent(resultPendingIntent)
                    .setContentTitle("New IP Address")

            //Notification sound, if required
            val sharedPreferences = mCtx.getSharedPreferences(
                    RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
            val ringtoneUri = sharedPreferences.getString(
                    RouterCompanionAppConstants.NOTIFICATIONS_SOUND, null)
            if (ringtoneUri != null) {
                mBuilder.setSound(Uri.parse(ringtoneUri), AudioManager.STREAM_NOTIFICATION)
            }
            if (!sharedPreferences.getBoolean(RouterCompanionAppConstants.NOTIFICATIONS_VIBRATE,
                    true)) {
                mBuilder.setDefaults(Notification.DEFAULT_LIGHTS)
                        .setVibrate(RouterCompanionAppConstants.NO_VIBRATION_PATTERN)
                //                    if (ringtoneUri != null) {
                //                        mBuilder.setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND);
                //                    } else {
                //                        mBuilder.setDefaults(Notification.DEFAULT_LIGHTS);
                //                    }
            }

            val routerPreferences = mCtx.getSharedPreferences(router.templateUuidOrUuid, Context.MODE_PRIVATE)

            val isFirstTimeForNotification = routerPreferences.getBoolean(
                    IS_FIRST_TIME_PREF_PREFIX + notifyID, true)

            val bigContentTitle = if (isFirstTimeForNotification) "New IP Address" else "IP Address change"

            val inboxStyle = NotificationCompat.InboxStyle().setSummaryText(summaryText)
                    .setBigContentTitle(bigContentTitle)

            //Public IP Address
            val wanPublicIpNullOrEmpty = isNullOrEmpty(wanPublicIp)
            val wanIpNullOrEmpty = isNullOrEmpty(wanIp)
            val publicIpLine = String.format("Public IP   %s",
                    if (wanPublicIpNullOrEmpty) "-" else wanPublicIp)
            val publicIpSpannable = SpannableString(publicIpLine)
            publicIpSpannable.setSpan(StyleSpan(android.graphics.Typeface.BOLD), 0,
                    "Public IP".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            inboxStyle.addLine(publicIpSpannable)

            //WAN IP Address
            val wanIpLine = String.format("WAN IP   %s", if (wanIpNullOrEmpty) "-" else wanIp)
            val wanIpSpannable = SpannableString(wanIpLine)
            wanIpSpannable.setSpan(StyleSpan(android.graphics.Typeface.BOLD), 0,
                    "WAN IP".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            inboxStyle.addLine(wanIpSpannable)

            mBuilder.setContentText(summaryText)

            // Moves the expanded layout object into the notification object.
            mBuilder.setStyle(inboxStyle)

            // Because the ID remains unchanged, the existing notification is
            // updated.
            val mNotificationManager = mCtx.getSystemService(
                    Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = mBuilder.build()
            mNotificationManager.notify(notifyID, notification)
            updateNotificationIconWithRouterAvatar(mCtx, router, notifyID, notification)
        }
    }
}
