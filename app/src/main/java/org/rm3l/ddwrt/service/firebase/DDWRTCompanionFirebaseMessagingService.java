package org.rm3l.ddwrt.service.firebase;

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
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Splitter;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.api.urlshortener.goo_gl.GooGlService;
import org.rm3l.ddwrt.api.urlshortener.goo_gl.resources.GooGlData;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.NetworkUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.GOOGLE_API_KEY;

/**
 * This is required if you want to do any message handling beyond receiving notifications on
 * apps in the background.
 * To receive notifications in foregrounded apps,
 * to receive data payload, to send upstream messages, and so on, you must extend this service.
 *
 * Created by rm3l on 26/09/2016.
 */
public class DDWRTCompanionFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = DDWRTCompanionFirebaseMessagingService.class.getSimpleName();

    public static final int FCM_NOTIFICATION_ID = 27635;

    public static final String FTP_DDWRT_FORMAT_BASE = "ftp://ftp.dd-wrt.com/betas";
    public static final String FTP_DDWRT_FORMAT = (FTP_DDWRT_FORMAT_BASE + "/%s/%s");

    private Context mApplicationContext;
    private GooGlService mGooGlService;

    @Override
    public void onCreate() {
        super.onCreate();
        mApplicationContext = getApplicationContext();
        mGooGlService = NetworkUtils
                .createApiService(DDWRTCompanionConstants.URL_SHORTENER_API_BASE_URL,
                        GooGlService.class);
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        try {

            //TODO Check whether notifications are enabled

            // [START_EXCLUDE]
            // There are two types of messages data messages and notification messages. Data messages are handled
            // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
            // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
            // is in the foreground. When the app is in the background an automatically generated notification is displayed.
            // When the user taps on the notification they are returned to the app. Messages containing both notification
            // and data payloads are treated as notification messages. The Firebase console always sends notification
            // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
            // [END_EXCLUDE]

            // TODO(developer): Handle FCM messages here.
            // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
            Log.d(TAG, "From: " + remoteMessage.getFrom());

            // Check if message contains a data payload.
            final Map<String, String> remoteMessageData = remoteMessage.getData();
            if (remoteMessageData.size() > 0) {
                Log.d(TAG, "Message data payload: " + remoteMessageData);
            }

            // Check if message contains a notification payload.
            if (remoteMessage.getNotification() != null) {
                Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            }

            // Also if you intend on generating your own notifications as a result of a received FCM
            // message, here is where that should be initiated. See sendNotification method below.
            sendNotification("A new DD-WRT Build is available", remoteMessageData);

        } catch(final Exception ignored) {
            //No worries
            ignored.printStackTrace();
            Crashlytics.logException(ignored);
        }
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param data FCM message body received.
     */
    private void sendNotification(String title, final Map<String, String> data) throws IOException {

        final String messageBody = data.get("releases");

        final List<String> releases = Splitter.on("\n")
                .omitEmptyStrings().splitToList(messageBody);
        final String releaseLink;
        if (releases == null || releases.isEmpty()) {
            releaseLink = FTP_DDWRT_FORMAT_BASE;
        } else {
            final String release = releases.get(0);
            final List<String> strings = Splitter.on("-").omitEmptyStrings().splitToList(release);
            if (strings == null || strings.size() < 3) {
                releaseLink = FTP_DDWRT_FORMAT_BASE;
            } else {
                final String year = strings.get(2);
                releaseLink = String.format(FTP_DDWRT_FORMAT, year, release);
            }
        }

        //Shorten FTP link so it can be opened with the browser
        final String releaseLinkShortened = mGooGlService.shortenLongUrl(
                GOOGLE_API_KEY,
                new GooGlData()
                        .setLongUrl(releaseLink))
                .execute().body().getId();

        Crashlytics.log(Log.DEBUG, TAG, "releaseLinkShortened: [" + releaseLinkShortened + "]");

        // pending implicit intent to view url
        final Intent resultIntent = new Intent(Intent.ACTION_VIEW);
        resultIntent.setData(Uri.parse(releaseLinkShortened));

        final PendingIntent pendingIntent = PendingIntent.getActivity(this,
                FCM_NOTIFICATION_ID /* Request code */,
                resultIntent, PendingIntent.FLAG_ONE_SHOT);

//        Intent intent = new Intent(this, RouterManagementActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this,
//                FCM_NOTIFICATION_ID /* Request code */, intent,
//                PendingIntent.FLAG_ONE_SHOT);

        final Bitmap largeIcon = BitmapFactory.decodeResource(
                mApplicationContext.getResources(),
                R.mipmap.ic_launcher_ddwrt_companion);

//        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setLargeIcon(largeIcon)
                .setSmallIcon(R.mipmap.ic_launcher_ddwrt_companion)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true);

        //Notification sound, if required
        final SharedPreferences sharedPreferences = mApplicationContext.getSharedPreferences(
                DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY,
                Context.MODE_PRIVATE);
        final String ringtoneUri = sharedPreferences
                .getString(DDWRTCompanionConstants.NOTIFICATIONS_SOUND, null);
        if (ringtoneUri != null) {
            notificationBuilder.setSound(Uri.parse(ringtoneUri), AudioManager.STREAM_NOTIFICATION);
        }

        if (!sharedPreferences
                .getBoolean(DDWRTCompanionConstants.NOTIFICATIONS_VIBRATE, true)) {
            notificationBuilder
                    .setDefaults(Notification.DEFAULT_LIGHTS)
                    .setVibrate(DDWRTCompanionConstants.NO_VIBRATION_PATTERN);
//                    if (ringtoneUri != null) {
//                        mBuilder.setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND);
//                    } else {
//                        mBuilder.setDefaults(Notification.DEFAULT_LIGHTS);
//                    }
        }
        notificationBuilder.setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(FCM_NOTIFICATION_ID /* ID of notification */, notificationBuilder.build());
    }

}
