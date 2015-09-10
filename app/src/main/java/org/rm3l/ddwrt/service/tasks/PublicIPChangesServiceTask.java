package org.rm3l.ddwrt.service.tasks;

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
import android.support.v4.app.NotificationCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.Patterns;

import com.google.common.base.Objects;

import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.main.DDWRTMainActivity;
import org.rm3l.ddwrt.resources.PublicIPInfo;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.util.Arrays;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.ddwrt.mgmt.RouterManagementActivity.ROUTER_SELECTED;
import static org.rm3l.ddwrt.resources.Encrypted.d;
import static org.rm3l.ddwrt.resources.Encrypted.e;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.WAN_IPADDR;

/**
 * Created by rm3l on 08/09/15.
 */
public class PublicIPChangesServiceTask extends AbstractBackgroundServiceTask {

    private static final String LOG_TAG = PublicIPChangesServiceTask.class.getSimpleName();
    public static final String LAST_PUBLIC_IP = "lastPublicIp";
    public static final String LAST_WAN_IP = "lastWanIp";

    public PublicIPChangesServiceTask(@NonNull Context ctx) {
        super(ctx);
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

        Log.d(LOG_TAG, "wanPublicIpCmdStatus: " + Arrays.toString(wanPublicIpCmdStatus));

        String wanIp = (nvramInfo != null ? nvramInfo.getProperty(WAN_IPADDR) : null);
        Log.d(LOG_TAG, "wanIp: " + wanIp);

        if (wanIp != null && !Patterns.IP_ADDRESS.matcher(wanIp).matches()) {
            wanIp = null;
        }

        buildNotificationIfNeeded(mCtx, router, routerPreferences, wanPublicIpCmdStatus, wanIp);

    }

    public static void buildNotificationIfNeeded(Context mCtx,
                                                 @NonNull Router router,
                                                 SharedPreferences routerPreferences,
                                                 String[] wanPublicIpCmdStatus,
                                                 String wanIp) {

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

                if (!isNullOrEmpty(wanPublicIp) && !isNullOrEmpty(wanIp)) {

                    //Also retrieve notification ID or set one
                    // Sets an ID for the notification, so it can be updated
                    int notifyID = routerPreferences.getInt("lastPublicIp_" + router.getId(), -1);
                    if (notifyID == -1) {
                        try {
                            notifyID = 1 + Integer.parseInt(router.getId() + "00" + router.getId());
                            editor.putInt("lastPublicIp_" + router.getId(), notifyID);
                        } catch (final Exception e) {
                            Utils.reportException(e);
                            return;
                        }
                    }

                    Log.d(LOG_TAG, "notifyID=" + notifyID);

                    //Now display notification
                    final boolean notificationsEnabled = routerPreferences
                            .getBoolean(DDWRTCompanionConstants.NOTIFICATIONS_ENABLE, true);

                    Log.d(LOG_TAG, "NOTIFICATIONS_ENABLE=" + notificationsEnabled);

                    final NotificationManager mNotificationManager = (NotificationManager)
                            mCtx.getSystemService(Context.NOTIFICATION_SERVICE);

                    if (!notificationsEnabled) {
                        mNotificationManager.cancel(notifyID);
                    } else {
                        final Bitmap largeIcon = BitmapFactory.decodeResource(
                                mCtx.getResources(),
                                R.drawable.ic_launcher_ddwrt_companion);

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
                                .setSmallIcon(R.drawable.ic_launcher_ddwrt_companion)
                                .setLargeIcon(largeIcon)
                                .setAutoCancel(true)
                                .setGroup(PublicIPChangesServiceTask.class.getSimpleName())
                                .setGroupSummary(true)
                                .setContentIntent(resultPendingIntent)
                                .setContentTitle("New Public IP Address");

                        //Notification sound, if required
                        final String ringtoneUri = mCtx.getSharedPreferences(
                                DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY,
                                Context.MODE_PRIVATE)
                                .getString(DDWRTCompanionConstants.NOTIFICATIONS_SOUND, null);
                        if (ringtoneUri != null) {
                            mBuilder.setSound(Uri.parse(ringtoneUri), AudioManager.STREAM_NOTIFICATION);
                        }

                        final NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle()
                                .setSummaryText(summaryText)
                                .setBigContentTitle("Public IP Address change");

                        //Public IP Address
                        String publicIpLine = String.format("Public IP   %s", wanPublicIp);
                        final Spannable publicIpSpannable = new SpannableString(publicIpLine);
                        publicIpSpannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                                0, "Public IP".length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        inboxStyle.addLine(publicIpSpannable);

                        //WAN IP Address
                        String wanIpLine = String.format("WAN IP   %s", wanIp);
                        final Spannable wanIpSpannable = new SpannableString(wanIpLine);
                        wanIpSpannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                                0, "WAN IP".length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        inboxStyle.addLine(wanIpSpannable);

                        mBuilder.setContentText(publicIpSpannable);

                        // Moves the expanded layout object into the notification object.
                        mBuilder.setStyle(inboxStyle);

                        // Because the ID remains unchanged, the existing notification is
                        // updated.
                        mNotificationManager.notify(notifyID, mBuilder.build());
                    }
                }

            } finally {
                editor.apply();
                Utils.requestBackup(mCtx);
            }
        }
    }
}
