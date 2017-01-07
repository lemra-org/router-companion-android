package org.rm3l.router_companion.service.tasks;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.Patterns;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Objects;

import org.rm3l.router_companion.BuildConfig;
import org.rm3l.router_companion.R;
import org.rm3l.router_companion.main.DDWRTMainActivity;
import org.rm3l.router_companion.resources.PublicIPInfo;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.utils.ReportingUtils;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;

import java.util.HashSet;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.router_companion.mgmt.RouterManagementActivity.ROUTER_SELECTED;
import static org.rm3l.router_companion.resources.Encrypted.d;
import static org.rm3l.router_companion.resources.Encrypted.e;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.WAN_IPADDR;
import static org.rm3l.router_companion.utils.ImageUtils.updateNotificationIconWithRouterAvatar;

/**
 * Created by rm3l on 08/09/15.
 */
public class PublicIPChangesServiceTask extends AbstractBackgroundServiceTask {

    public static final String LAST_PUBLIC_IP_PREF_PREFIX = "lastPublicIp_";
    private static final String LOG_TAG = PublicIPChangesServiceTask.class.getSimpleName();
    public static final String LAST_PUBLIC_IP = "lastPublicIp";
    public static final String LAST_WAN_IP = "lastWanIp";
    public static final String IS_FIRST_TIME_PREF_PREFIX = "isFirstTime_";

    private final RouterModelUpdaterServiceTask routerModelUpdaterServiceTask;

    public PublicIPChangesServiceTask(@NonNull Context ctx) {
        super(ctx);
        this.routerModelUpdaterServiceTask =
                new RouterModelUpdaterServiceTask(ctx);
    }

    @Override
    public void runBackgroundServiceTask(@NonNull Router router) throws Exception {
        final SharedPreferences routerPreferences = mCtx.getSharedPreferences(
                router.getUuid(), Context.MODE_PRIVATE);
        if (routerPreferences == null) {
            return;
        }

        final CharSequence applicationName = Utils.getApplicationName(mCtx);

        final NVRAMInfo nvramInfo = SSHUtils.getNVRamInfoFromRouter(mCtx, router,
                globalPreferences, WAN_IPADDR);

        final String[] wanPublicIpCmdStatus = SSHUtils.getManualProperty(mCtx,
                router, globalPreferences,
                //"echo -e \"GET / HTTP/1.1\\r\\nHost:icanhazip.com\\r\\nUser-Agent:DD-WRT Companion/3.3.0\\r\\n\" | nc icanhazip.com 80"
                String.format("echo -e \"" +
                                "GET / HTTP/1.1\\r\\n" +
                                "Host:%s\\r\\n" +
                                "User-Agent:%s/%s\\r\\n\" " +
                                "| /usr/bin/nc %s %d",
                        PublicIPInfo.ICANHAZIP_HOST,
                        applicationName != null ? applicationName : BuildConfig.APPLICATION_ID,
                        BuildConfig.VERSION_NAME,
                        PublicIPInfo.ICANHAZIP_HOST,
                        PublicIPInfo.ICANHAZIP_PORT));

        String wanIp = (nvramInfo != null ? nvramInfo.getProperty(WAN_IPADDR) : null);

        if (wanIp != null && !Patterns.IP_ADDRESS.matcher(wanIp).matches()) {
            wanIp = null;
        }

        try {
            routerModelUpdaterServiceTask
                    .runBackgroundServiceTask(router);
        } catch (final Exception e) {
            Utils.reportException(mCtx, e);
            //No worries
        } finally {
            buildNotificationIfNeeded(mCtx, router, routerPreferences, wanPublicIpCmdStatus, wanIp, null);
        }

    }

    public static void buildNotificationIfNeeded(Context mCtx,
                                                 @NonNull Router router,
                                                 SharedPreferences routerPreferences,
                                                 String[] wanPublicIpCmdStatus,
                                                 String wanIp,
                                                 @Nullable final Exception exception) {

        if (exception != null) {
            exception.printStackTrace();
            return;
        }

        if (!mCtx.getSharedPreferences(RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY,
                Context.MODE_PRIVATE).getStringSet(RouterCompanionAppConstants.NOTIFICATIONS_CHOICE_PREF,
                new HashSet<String>()).contains(PublicIPChangesServiceTask.class.getSimpleName())) {
            Crashlytics.log(Log.DEBUG,  LOG_TAG, "PublicIPChangesServiceTask notifications disabled");
            return;
        }

        final String wanIpFromPrefs = d(
                routerPreferences.getString(LAST_WAN_IP, null));

        final String wanPublicIpFromPrefs = d(
                routerPreferences.getString(LAST_PUBLIC_IP, null));

        String wanPublicIp;
        if (wanPublicIpCmdStatus == null || wanPublicIpCmdStatus.length == 0) {
            //Couldn't determine IP Address
            wanPublicIp = null;
        } else {
            wanPublicIp = wanPublicIpCmdStatus[wanPublicIpCmdStatus.length - 1]
                    .trim();
            if (!Patterns.IP_ADDRESS.matcher(wanPublicIp).matches()) {
                wanPublicIp = null;
            }
        }

        if (!(Objects.equal(wanPublicIp, wanPublicIpFromPrefs) &&
                Objects.equal(wanIp, wanIpFromPrefs))) {

            final SharedPreferences.Editor editor = routerPreferences.edit();
            try {
                //Save last value into preferences and display notification
                editor
                        .putString(LAST_PUBLIC_IP, e(wanPublicIp))
                        .putString(LAST_WAN_IP, e(wanIp));

                //Also retrieve notification ID or set one
                // Sets an ID for the notification, so it can be updated
                int notifyID = routerPreferences.getInt(LAST_PUBLIC_IP_PREF_PREFIX + router.getId(), -1);
                if (notifyID == -1) {
                    try {
                        notifyID = 1 + Integer.parseInt(router.getId() + "00" + router.getId());
                        editor.putInt(LAST_PUBLIC_IP_PREF_PREFIX + router.getId(), notifyID);
                    } catch (final Exception e) {
                        ReportingUtils.reportException(null, e);
                        return;
                    }
                }

                //Now display notification
                final boolean notificationsEnabled = routerPreferences
                        .getBoolean(RouterCompanionAppConstants.NOTIFICATIONS_ENABLE, true);

                Crashlytics.log(Log.DEBUG,  LOG_TAG, "NOTIFICATIONS_ENABLE=" + notificationsEnabled);

                final NotificationManager mNotificationManager = (NotificationManager)
                        mCtx.getSystemService(Context.NOTIFICATION_SERVICE);

                if (!notificationsEnabled) {
                    mNotificationManager.cancel(notifyID);
                } else {

                    final boolean wanPublicIpNullOrEmpty = isNullOrEmpty(wanPublicIp);
                    final boolean wanIpNullOrEmpty = isNullOrEmpty(wanIp);
                    if (!(wanPublicIpNullOrEmpty && wanIpNullOrEmpty)) {

                        final Bitmap largeIcon = BitmapFactory.decodeResource(
                                mCtx.getResources(),
                                R.mipmap.ic_launcher_ddwrt_companion);

                        final Intent resultIntent = new Intent(mCtx,
                                DDWRTMainActivity.class);
                        resultIntent.putExtra(ROUTER_SELECTED, router.getUuid());
                        resultIntent.putExtra(DDWRTMainActivity.SAVE_ITEM_SELECTED, 1); //Open right on the Public IP status
                        // Because clicking the notification opens a new ("special") activity, there's
                        // no need to create an artificial back stack.
                        final PendingIntent resultPendingIntent =
                                PendingIntent.getActivity(
                                        mCtx,
                                        0,
                                        resultIntent,
                                        PendingIntent.FLAG_UPDATE_CURRENT
                                );

                        final String mRouterName = router.getName();
                        final boolean mRouterNameNullOrEmpty = isNullOrEmpty(mRouterName);
                        String summaryText = "";
                        if (!mRouterNameNullOrEmpty) {
                            summaryText = (mRouterName + " (");
                        }
                        summaryText += router.getRemoteIpAddress();
                        if (!mRouterNameNullOrEmpty) {
                            summaryText += ")";
                        }

                        final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                                mCtx)
                                .setSmallIcon(R.drawable.ic_stat_ip)
                                .setLargeIcon(largeIcon)
                                .setAutoCancel(true)
                                .setGroup(PublicIPChangesServiceTask.class.getSimpleName())
                                .setGroupSummary(true)
                                .setContentIntent(resultPendingIntent)
                                .setContentTitle("New IP Address");

                        //Notification sound, if required
                        final SharedPreferences sharedPreferences = mCtx.getSharedPreferences(
                                RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY,
                                Context.MODE_PRIVATE);
                        final String ringtoneUri = sharedPreferences
                                .getString(RouterCompanionAppConstants.NOTIFICATIONS_SOUND, null);
                        if (ringtoneUri != null) {
                            mBuilder.setSound(Uri.parse(ringtoneUri), AudioManager.STREAM_NOTIFICATION);
                        }
                        if (!sharedPreferences
                                .getBoolean(RouterCompanionAppConstants.NOTIFICATIONS_VIBRATE, true)) {
                            mBuilder
                                    .setDefaults(Notification.DEFAULT_LIGHTS)
                                    .setVibrate(RouterCompanionAppConstants.NO_VIBRATION_PATTERN);
    //                    if (ringtoneUri != null) {
    //                        mBuilder.setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND);
    //                    } else {
    //                        mBuilder.setDefaults(Notification.DEFAULT_LIGHTS);
    //                    }
                        }

                        final boolean isFirstTimeForNotification = routerPreferences
                                .getBoolean(IS_FIRST_TIME_PREF_PREFIX + notifyID, true);

                        final String bigContentTitle = (isFirstTimeForNotification ?
                                "New IP Address" : "IP Address change");

                        final NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle()
                                .setSummaryText(summaryText)
                                .setBigContentTitle(bigContentTitle);

                        //Public IP Address
                        final String publicIpLine = String.format("Public IP   %s",
                                wanPublicIpNullOrEmpty ? "-" : wanPublicIp);
                        final Spannable publicIpSpannable = new SpannableString(publicIpLine);
                        publicIpSpannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                                0, "Public IP".length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        inboxStyle.addLine(publicIpSpannable);

                        //WAN IP Address
                        final String wanIpLine = String.format("WAN IP   %s",
                                wanIpNullOrEmpty ? "-" : wanIp);
                        final Spannable wanIpSpannable = new SpannableString(wanIpLine);
                        wanIpSpannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                                0, "WAN IP".length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        inboxStyle.addLine(wanIpSpannable);

                        mBuilder.setContentText(summaryText);

                        // Moves the expanded layout object into the notification object.
                        mBuilder.setStyle(inboxStyle);

                        // Because the ID remains unchanged, the existing notification is
                        // updated.
                        final Notification notification = mBuilder.build();
                        mNotificationManager.notify(notifyID, notification);
                        updateNotificationIconWithRouterAvatar(mCtx, router, notifyID, notification);

                        editor.putBoolean(IS_FIRST_TIME_PREF_PREFIX + notifyID, false);
                    }
                }

            } finally {
                editor.apply();
                Utils.requestBackup(mCtx);
            }
        }
    }
}
