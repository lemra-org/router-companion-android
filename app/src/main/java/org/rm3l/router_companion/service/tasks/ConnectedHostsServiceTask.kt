package org.rm3l.router_companion.service.tasks

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import androidx.core.app.NotificationCompat
import com.google.common.base.Function
import com.google.common.base.Objects
import com.google.common.base.Splitter
import com.google.common.base.Strings
import com.google.common.base.Strings.isNullOrEmpty
import com.google.common.collect.ComparisonChain
import com.google.common.collect.FluentIterable
import com.google.common.collect.HashMultimap
import com.google.common.collect.Maps
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.rm3l.ddwrt.R
import org.rm3l.router_companion.RouterCompanionAppConstants
import org.rm3l.router_companion.main.DDWRTMainActivity
import org.rm3l.router_companion.mgmt.RouterManagementActivity.Companion.ROUTER_SELECTED
import org.rm3l.router_companion.resources.Device
import org.rm3l.router_companion.resources.Encrypted.d
import org.rm3l.router_companion.resources.Encrypted.e
import org.rm3l.router_companion.resources.conn.Router
import org.rm3l.router_companion.tiles.DDWRTTile
import org.rm3l.router_companion.tiles.services.wol.WakeOnLanTile.GSON_BUILDER
import org.rm3l.router_companion.tiles.status.wireless.WirelessClientsTile
import org.rm3l.router_companion.tiles.status.wireless.WirelessClientsTile.CASE_INSENSITIVE_STRING_ORDERING
import org.rm3l.router_companion.tiles.status.wireless.WirelessClientsTile.CONNECTED_HOSTS
import org.rm3l.router_companion.tiles.status.wireless.WirelessClientsTile.DEVICE_NAME_FOR_NOTIFICATION
import org.rm3l.router_companion.tiles.status.wireless.WirelessClientsTile.IP_ADDRESS
import org.rm3l.router_companion.tiles.status.wireless.WirelessClientsTile.MAC_ADDRESS
import org.rm3l.router_companion.tiles.status.wireless.WirelessClientsTile.MAP_KEYWORD
import org.rm3l.router_companion.utils.ImageUtils.updateNotificationIconWithRouterAvatar
import org.rm3l.router_companion.utils.ReportingUtils
import org.rm3l.router_companion.utils.SSHUtils
import org.rm3l.router_companion.utils.Utils
import java.util.Arrays
import java.util.Comparator
import java.util.HashSet
import java.util.TreeMap
import java.util.regex.Pattern

/**
 * Created by rm3l on 05/07/15.
 */
class ConnectedHostsServiceTask(context: Context) : AbstractBackgroundServiceTask(context) {

    private val routerModelUpdaterServiceTask: RouterModelUpdaterServiceTask =
        RouterModelUpdaterServiceTask(context)

    @Throws(Exception::class)
    override fun runBackgroundServiceTask(router: Router) {

        val routerPreferences = mCtx.getSharedPreferences(router.templateUuidOrUuid, Context.MODE_PRIVATE)

        val output = SSHUtils.getManualProperty(
            mCtx, router, globalPreferences,
            "grep dhcp-host /tmp/dnsmasq.conf | sed 's/.*=//' | awk -F , '{print \"" +
                MAP_KEYWORD +
                "\",$1,$3 ,$2}'",
            "awk '{print \"$MAP_KEYWORD\",$2,$3,$4}' /tmp/dnsmasq.leases",
            "awk 'NR>1{print \"$MAP_KEYWORD\",$4,$1,\"*\"}' /proc/net/arp",
            "arp -a | awk '{print \"$MAP_KEYWORD\",$4,$2,$1}'", "echo done"
        )

        FirebaseCrashlytics.getInstance().log("output: " + Arrays.toString(output))

        if (output == null || output.isEmpty()) {
            if (output == null) {
                return
            }
        }
        val macToDevice = TreeMap<String, Device>(String.CASE_INSENSITIVE_ORDER)
        val macToDeviceOutput = HashMultimap.create<String, Device>()

        val splitter = Splitter.on(" ")

        var ipAddress: String?
        val betweenParenthesisPattern = Pattern.compile("\\((.*?)\\)")

        // Active clients
        val activeClients = SSHUtils.getManualProperty(
            mCtx, router, globalPreferences,
            "arp -a 2>/dev/null"
        )

        for (stdoutLine in output) {
            if ("done" == stdoutLine) {
                break
            }
            val `as` = splitter.splitToList(stdoutLine)
            if (`as`.size >= 4 && MAP_KEYWORD == `as`[0]) {
                val macAddress = Strings.nullToEmpty(`as`[1]).toLowerCase()
                if (isNullOrEmpty(macAddress) ||
                    "00:00:00:00:00:00" == macAddress ||
                    macAddress.contains("incomplete", ignoreCase = true)
                ) {
                    // Skip clients with incomplete ARP set-up
                    continue
                }

                ipAddress = `as`[2]
                if (ipAddress != null) {
                    val matcher = betweenParenthesisPattern.matcher(ipAddress)
                    if (matcher.find()) {
                        ipAddress = matcher.group(1)
                    }
                }

                val device = Device(macAddress)
                device.ipAddress = ipAddress

                val systemName = `as`[3]
                if ("*" != systemName) {
                    device.systemName = systemName
                }

                // Alias from SharedPreferences
                if (routerPreferences != null) {
                    val deviceAlias = routerPreferences.getString(macAddress, null)
                    if (!isNullOrEmpty(deviceAlias)) {
                        device.alias = deviceAlias
                    }
                }

                if (activeClients != null) {
                    for (activeClient in activeClients) {
                        if (activeClient.contains(macAddress, ignoreCase = true)) {
                            device.isActive = true
                            break
                        }
                    }
                }

                try {
                    device.macouiVendorDetails = WirelessClientsTile.mMacOuiVendorLookupCache.get(macAddress)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Utils.reportException(mCtx, e)
                }

                macToDeviceOutput.put(macAddress, device)
            }
        }

        for ((macAddr, deviceCollection) in macToDeviceOutput.asMap()) {
            for (device in deviceCollection) {
                // Consider the one that has a Name, if any
                if (!isNullOrEmpty(device.systemName)) {
                    macToDevice.put(macAddr, device)
                    break
                }
            }
            if (deviceCollection.isEmpty() || macToDevice.containsKey(macAddr)) {
                continue
            }

            val dev = deviceCollection.iterator().next()

            macToDevice.put(macAddr, dev)
        }

        try {
            routerModelUpdaterServiceTask.runBackgroundServiceTask(router)
        } catch (e: Exception) {
            Utils.reportException(mCtx, e)
            // No worries
        } finally {
            generateConnectedHostsNotification(mCtx, router, macToDevice.values)
        }
    }

    companion object {

        private val TAG = ConnectedHostsServiceTask::class.java.simpleName

        fun generateConnectedHostsNotification(
            mCtx: Context,
            router: Router?,
            deviceCollection: Collection<Device>
        ) {

            if (!mCtx.getSharedPreferences(
                    RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY,
                    Context.MODE_PRIVATE
                )
                .getStringSet(RouterCompanionAppConstants.NOTIFICATIONS_CHOICE_PREF, HashSet<String>())!!
                .contains(ConnectedHostsServiceTask::class.java.simpleName)
            ) {
                FirebaseCrashlytics.getInstance().log("ConnectedHostsServiceTask notifications disabled")
                return
            }

            if (router == null) {
                FirebaseCrashlytics.getInstance().log("router == null")
                return
            }

            val mNotificationManager = mCtx.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            // Sets an ID for the notification, so it can be updated
            val notifyID = router.id

            val mRouterPreferences = mCtx.getSharedPreferences(router.templateUuidOrUuid, Context.MODE_PRIVATE)

            val onlyActiveHosts = mRouterPreferences.getBoolean(
                RouterCompanionAppConstants.NOTIFICATIONS_CONNECTED_HOSTS_ACTIVE_ONLY, true
            )

            FirebaseCrashlytics.getInstance().log("onlyActiveHosts=" + onlyActiveHosts)
            FirebaseCrashlytics.getInstance().log("deviceCollection=" + deviceCollection)

            val devicesCollFiltered = FluentIterable.from(
                deviceCollection
            )
                .filter { input -> !onlyActiveHosts || input != null && input.isActive }
                .toSortedSet(
                    Comparator<Device> { lhs, rhs ->
                        if (lhs === rhs) {
                            return@Comparator 0
                        }
                        if (lhs == null) {
                            return@Comparator -1
                        }
                        if (rhs == null) {
                            return@Comparator 1
                        }
                        ComparisonChain.start()
                            .compare(
                                lhs.aliasOrSystemName, rhs.aliasOrSystemName,
                                CASE_INSENSITIVE_STRING_ORDERING
                            )
                            .result()
                    }
                )
            val sizeFiltered = devicesCollFiltered.size

            val previousConnectedHosts = HashSet<Device?>()
            val devicesStringSet = HashSet(
                mRouterPreferences.getStringSet(
                    DDWRTTile.getFormattedPrefKey(WirelessClientsTile::class.java, CONNECTED_HOSTS),
                    HashSet<String>()
                )!!
            )
            for (devStrEncrypted in devicesStringSet) {
                try {
                    val devStr = d(devStrEncrypted)
                    if (isNullOrEmpty(devStr)) {
                        continue
                    }

                    val objFromJson = GSON_BUILDER.create().fromJson(devStr, Map::class.java)

                    val macAddress = objFromJson[MAC_ADDRESS]
                    if (macAddress == null || macAddress.toString().isEmpty()) {
                        continue
                    }

                    // IP Address may change as well
                    val deviceFromJson = Device(macAddress.toString())

                    val ipAddr = objFromJson[IP_ADDRESS]
                    if (ipAddr != null) {
                        deviceFromJson.ipAddress = ipAddr.toString()
                    }

                    // Display name may change too
                    val displayName = objFromJson[DEVICE_NAME_FOR_NOTIFICATION]
                    if (displayName != null) {
                        deviceFromJson.deviceNameForNotification = displayName.toString()
                    }

                    previousConnectedHosts.add(deviceFromJson)
                } catch (e: Exception) {
                    // No worries
                    e.printStackTrace()
                    ReportingUtils.reportException(
                        null,
                        IllegalStateException("Failed to decode and parse JSON: " + devStrEncrypted, e)
                    )
                }
            }

            FirebaseCrashlytics.getInstance().log(
                "<sizeFiltered,previousConnectedHosts.size()>=<" +
                    sizeFiltered +
                    "," +
                    previousConnectedHosts.size +
                    ">"
            )

            var updateNotification = false
            if (sizeFiltered != previousConnectedHosts.size) {
                updateNotification = true
            } else {

                FirebaseCrashlytics.getInstance().log("devicesCollFiltered: " + devicesCollFiltered)
                FirebaseCrashlytics.getInstance().log("previousConnectedHosts: " + previousConnectedHosts)

                // Now compare if anything has changed
                for (newDevice in devicesCollFiltered) {
                    if (newDevice == null) {
                        continue
                    }
                    val deviceMacAddress = newDevice.macAddress
                    val deviceIpAddress = newDevice.ipAddress
                    val deviceDisplayName = newDevice.aliasOrSystemName

                    var deviceFound = false
                    var deviceHasChanged = false
                    for (previousConnectedHost in previousConnectedHosts) {
                        if (previousConnectedHost == null) {
                            continue
                        }
                        if (!previousConnectedHost.macAddress.equals(deviceMacAddress, ignoreCase = true)) {
                            continue
                        }
                        deviceFound = true
                        // Device found - now analyze if something has changed
                        if (!(
                            Objects.equal(deviceIpAddress, previousConnectedHost.ipAddress) && Objects.equal(
                                    deviceDisplayName,
                                    previousConnectedHost.deviceNameForNotification
                                )
                            )
                        ) {
                            deviceHasChanged = true
                            break
                        }
                    }
                    if (deviceHasChanged || !deviceFound) {
                        // This is a new device or an updated one - so update is needed
                        updateNotification = true
                        break
                    }
                }
            }

            FirebaseCrashlytics.getInstance().log("updateNotification=" + updateNotification)

            // Build the String Set to save in preferences
            val stringImmutableSet = FluentIterable.from(devicesCollFiltered).transform(
                Function<Device, String> { device ->
                    if (device == null) {
                        return@Function e(RouterCompanionAppConstants.EMPTY_STRING)
                    }
                    val details = Maps.newHashMap<String, String>()
                    details.put(MAC_ADDRESS, device.macAddress)
                    details.put(IP_ADDRESS, device.ipAddress)
                    details.put(DEVICE_NAME_FOR_NOTIFICATION, device.aliasOrSystemName)

                    e(GSON_BUILDER.create().toJson(details))
                }
            ).toSet()

            mRouterPreferences.edit()
                .remove(DDWRTTile.getFormattedPrefKey(WirelessClientsTile::class.java, CONNECTED_HOSTS))
                .apply()

            mRouterPreferences.edit()
                .putStringSet(
                    DDWRTTile.getFormattedPrefKey(WirelessClientsTile::class.java, CONNECTED_HOSTS),
                    stringImmutableSet
                )
                .apply()

            Utils.requestBackup(mCtx)

            FirebaseCrashlytics.getInstance().log(
                "NOTIFICATIONS_ENABLE=" + mRouterPreferences.getBoolean(
                    RouterCompanionAppConstants.NOTIFICATIONS_ENABLE, true
                )
            )

            if (sizeFiltered == 0 || !mRouterPreferences.getBoolean(
                    RouterCompanionAppConstants.NOTIFICATIONS_ENABLE, true
                )
            ) {
                mNotificationManager.cancel(notifyID)
            } else {

                if (updateNotification) {
                    val largeIcon = Router.loadRouterAvatarUrlSync(mCtx, router, Router.mAvatarDownloadOpts)
                    doNotify(
                        mCtx, router,
                        largeIcon ?: BitmapFactory.decodeResource(
                            mCtx.resources,
                            R.mipmap.ic_launcher_ddwrt_companion
                        ),
                        sizeFiltered, devicesCollFiltered
                    )
                }
            }
        }

        private fun doNotify(
            mCtx: Context,
            router: Router,
            largeIcon: Bitmap,
            sizeFiltered: Int,
            devicesCollFiltered: Collection<Device>
        ) {
            val resultIntent = Intent(mCtx, DDWRTMainActivity::class.java)
            resultIntent.putExtra(ROUTER_SELECTED, router.uuid)
            resultIntent.putExtra(
                DDWRTMainActivity.SAVE_ITEM_SELECTED,
                4
            ) // Open right on Clients Section
            // Because clicking the notification opens a new ("special") activity, there's
            // no need to create an artificial back stack.
            val resultPendingIntent = PendingIntent.getActivity(
                mCtx, 0, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

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
                .setSmallIcon(R.drawable.ic_connected_hosts_notification)
                .setLargeIcon(largeIcon)
                .setAutoCancel(true)
//                    .setGroup(WirelessClientsTile::class.java.simpleName)
                .setGroupSummary(true)
                .setContentIntent(resultPendingIntent)
            //                                .setDefaults(Notification.DEFAULT_ALL);

            // Notification sound, if required
            val sharedPreferences = mCtx.getSharedPreferences(
                RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY,
                Context.MODE_PRIVATE
            )
            val ringtoneUri = sharedPreferences.getString(
                RouterCompanionAppConstants.NOTIFICATIONS_SOUND, null
            )
            if (ringtoneUri != null) {
                mBuilder.setSound(Uri.parse(ringtoneUri), AudioManager.STREAM_NOTIFICATION)
            }

            if (!sharedPreferences.getBoolean(
                    RouterCompanionAppConstants.NOTIFICATIONS_VIBRATE,
                    true
                )
            ) {
                mBuilder.setDefaults(Notification.DEFAULT_LIGHTS)
                    .setVibrate(RouterCompanionAppConstants.NO_VIBRATION_PATTERN)
                //                    if (ringtoneUri != null) {
                //                        mBuilder.setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND);
                //                    } else {
                //                        mBuilder.setDefaults(Notification.DEFAULT_LIGHTS);
                //                    }
            }

            val inboxStyle = NotificationCompat.InboxStyle().setSummaryText(summaryText)

            val newDevicesTitle = String.format(
                "%d connected host%s", sizeFiltered,
                if (sizeFiltered > 1) "s" else ""
            )

            mBuilder.setContentTitle(newDevicesTitle)

            inboxStyle.setBigContentTitle(newDevicesTitle)

            if (sizeFiltered == 1) {
                // Only one device
                val device = devicesCollFiltered.iterator().next()
                val deviceAliasOrSystemName = device.aliasOrSystemName

                //                                mBuilder.setContentTitle(deviceNameToDisplay);

                //                                inboxStyle.setBigContentTitle(deviceNameToDisplay);
                // Name
                if (!isNullOrEmpty(deviceAliasOrSystemName)) {
                    val nameLine = String.format("Name   %s", deviceAliasOrSystemName)
                    val nameSpannable = SpannableString(nameLine)
                    nameSpannable.setSpan(
                        StyleSpan(android.graphics.Typeface.BOLD), 0, "Name".length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    inboxStyle.addLine(nameSpannable)
                }

                // IP Address
                val ipLine = String.format("IP   %s", device.ipAddress)
                val ipSpannable = SpannableString(ipLine)
                ipSpannable.setSpan(
                    StyleSpan(android.graphics.Typeface.BOLD), 0, "IP".length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                inboxStyle.addLine(ipSpannable)

                // MAC Address
                val macLine = String.format("MAC   %s", device.macAddress)
                val macSpannable = SpannableString(macLine)
                macSpannable.setSpan(
                    StyleSpan(android.graphics.Typeface.BOLD), 0, "MAC".length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                inboxStyle.addLine(macSpannable)

                val macouiVendorDetails = device.macouiVendorDetails
                if (macouiVendorDetails != null) {
                    // NIC Manufacturer
                    val ouiLine = String.format(
                        "NIC Man.   %s",
                        Strings.nullToEmpty(macouiVendorDetails.company)
                    )
                    val ouiSpannable = SpannableString(ouiLine)
                    ouiSpannable.setSpan(
                        StyleSpan(android.graphics.Typeface.BOLD), 0,
                        "NIC Man.".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    inboxStyle.addLine(ouiSpannable)
                }

                mBuilder.setContentText(summaryText)
            } else {

                for (device in devicesCollFiltered) {
                    val macouiVendorDetails = device.macouiVendorDetails
                    val deviceAliasOrSystemName = device.aliasOrSystemName
                    val deviceNameToDisplay = if (isNullOrEmpty(deviceAliasOrSystemName))
                        device.macAddress
                    else
                        deviceAliasOrSystemName
                    val line = String.format(
                        "%s   %s%s%s", deviceNameToDisplay, device.ipAddress,
                        if (isNullOrEmpty(deviceAliasOrSystemName))
                            ""
                        else
                            String.format(" | %s", device.macAddress),
                        if (macouiVendorDetails != null && !Strings.isNullOrEmpty(
                                macouiVendorDetails.company
                            )
                        )
                            String.format(
                                " (%s)",
                                macouiVendorDetails.company
                            )
                        else
                            ""
                    )
                    val sb = SpannableString(line)
                    sb.setSpan(
                        StyleSpan(android.graphics.Typeface.BOLD), 0,
                        deviceNameToDisplay.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    inboxStyle.addLine(sb)
                }

                mBuilder.setContentText(summaryText)
                mBuilder.setNumber(sizeFiltered)
            }

            // Moves the expanded layout object into the notification object.
            mBuilder.setStyle(inboxStyle)

            // Because the ID remains unchanged, the existing notification is
            // updated.
            val mNotificationManager = mCtx.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            // Sets an ID for the notification, so it can be updated
            val notifyID = router.id
            val notification = mBuilder.build()
            mNotificationManager.notify(notifyID, notification)
            updateNotificationIconWithRouterAvatar(mCtx, router, notifyID, notification)
        }
    }
}
