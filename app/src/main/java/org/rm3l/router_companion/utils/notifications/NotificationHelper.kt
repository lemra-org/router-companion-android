package org.rm3l.router_companion.utils.notifications

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import org.rm3l.ddwrt.R
import org.rm3l.router_companion.mgmt.RouterManagementActivity
import org.rm3l.router_companion.resources.conn.Router
import org.rm3l.router_companion.resources.conn.Router.RouterFirmware
import org.rm3l.router_companion.resources.conn.Router.RouterFirmware.DEMO
import org.rm3l.router_companion.resources.conn.Router.RouterFirmware.DDWRT
import org.rm3l.router_companion.resources.conn.Router.RouterFirmware.TOMATO

const val NOTIFICATION_GROUP_GENERAL_UPDATES = "general-fw-updates"

fun Router.createNotificationChannelGroup(context: Context) {
    if (VERSION.SDK_INT >= VERSION_CODES.O) {
        // The id of the group.
        val group = this.uuid
        // The user-visible name of the group.
        val name = this.canonicalHumanReadableName
        val mNotificationManager = context
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.createNotificationChannelGroup(NotificationChannelGroup(group, name))

        // Now create notification channels: Router Notifications (Connected Hosts, WAN IP Updates, ...)
        val channelId = this.notificationChannelId
        val channelName = "Router Events"
        val channelDescription = "Events occurring in Router"
        val channelImportance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, channelImportance)
        channel.setShowBadge(true)
        channel.description = channelDescription
        channel.enableLights(true)
        val lightColor: Int? = when (this.routerFirmware) {
            DDWRT -> R.color.ddwrt_primary
            TOMATO -> R.color.tomato_primary
            else -> null
        }
        lightColor?.let { channel.lightColor = it }
        channel.enableVibration(true)
        channel.vibrationPattern = longArrayOf(100)
        channel.group = group
        mNotificationManager.createNotificationChannel(channel)
    }
}

fun Context.createGeneralNotificationChannelGroup() {
    if (VERSION.SDK_INT >= VERSION_CODES.O) {
        // Firmware Updates
        // The user-visible name of the group.
        val name = "Firmware Updates"
        val mNotificationManager = this
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.createNotificationChannelGroup(
                NotificationChannelGroup(NOTIFICATION_GROUP_GENERAL_UPDATES, name))

        // Create channels: for each type of router fw supported (DD-WRT, Tomato, ...)
        val generalChannels = RouterFirmware.values()
                .filter {
                    when (it) {
                        DDWRT, TOMATO, DEMO -> true
                        else -> false // TODO not supported for now
                    }
                }
                .map {
                    val channelId = "$NOTIFICATION_GROUP_GENERAL_UPDATES-${it.name}"
                    val channelName = it.displayName
                    val channelDescription = "${it.displayName} Firmware Build Updates"
                    val channelImportance = NotificationManager.IMPORTANCE_DEFAULT
                    val channel = NotificationChannel(channelId, channelName, channelImportance)
                    channel.description = channelDescription
                    channel.setShowBadge(true)
                    channel.enableLights(true)
                    val lightColor: Int? = when (it) {
                        DDWRT -> R.color.ddwrt_primary
                        TOMATO -> R.color.tomato_primary
                        else -> null
                    }
                    lightColor?.let { channel.lightColor = it }
                    channel.enableVibration(true)
                    channel.vibrationPattern = longArrayOf(100)

                    channel.group = NOTIFICATION_GROUP_GENERAL_UPDATES
                    channel
                }

        mNotificationManager.createNotificationChannels(generalChannels)
    }
}

fun Context.createOrUpdateNotificationChannels() {
    this.createGeneralNotificationChannelGroup()
    RouterManagementActivity.getDao(this).allRouters
            .forEach { it.createNotificationChannelGroup(this) }
}