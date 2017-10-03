package org.rm3l.router_companion.service.firebase;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.crashlytics.android.Crashlytics;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.router_companion.RouterCompanionAppConstants;

import static org.rm3l.router_companion.RouterCompanionAppConstants.CLOUD_MESSAGING_TOPIC_DDWRT_BUILD_UPDATES;
import static org.rm3l.router_companion.RouterCompanionAppConstants.NOTIFICATIONS_CHOICE_PREF;

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

  private SharedPreferences mGlobalPreferences;

  @Override public void onCreate() {
    super.onCreate();
    mGlobalPreferences =
        getSharedPreferences(RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY,
            Context.MODE_PRIVATE);
  }

  /**
   * Called when message is received.
   *
   * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
   */
  // [START receive_message]
  @Override public void onMessageReceived(RemoteMessage remoteMessage) {

    try {

      //This is a premium feature
      if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
        Crashlytics.log(Log.DEBUG, TAG, "[Firebase] DD-WRT Build Updates feature is *premium*!");
        return;
      }

      //Is user interested in DD-WRT Build updates?
      final Set<String> notificationChoices =
          this.mGlobalPreferences.getStringSet(NOTIFICATIONS_CHOICE_PREF, new HashSet<String>());
      if (!notificationChoices.contains(CLOUD_MESSAGING_TOPIC_DDWRT_BUILD_UPDATES)) {
        Crashlytics.log(Log.DEBUG, TAG, "[Firebase] Not interested in DD-WRT Build Updates!");
        return;
      }

      // [START_EXCLUDE]
      // There are two types of messages data messages and notification messages. Data messages are handled
      // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
      // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
      // is in the foreground. When the app is in the background an automatically generated notification is displayed.
      // When the user taps on the notification they are returned to the app. Messages containing both notification
      // and data payloads are treated as notification messages. The Firebase console always sends notification
      // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
      // [END_EXCLUDE]

      // Handle FCM messages here.
      // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
      Crashlytics.log(Log.DEBUG, TAG, "[Firebase] From: " + remoteMessage.getFrom());

      // Check if message contains a data payload.
      final Map<String, String> remoteMessageData = remoteMessage.getData();
      if (remoteMessageData.size() > 0) {
        Crashlytics.log(Log.DEBUG, TAG, "[Firebase] Message data payload: " + remoteMessageData);
      }

      // Check if message contains a notification payload.
      if (remoteMessage.getNotification() != null) {
        Crashlytics.log(Log.DEBUG, TAG,
            "[Firebase] Message Notification Body: " + remoteMessage.getNotification().getBody());
      }

      // Also if you intend on generating your own notifications as a result of a received FCM
      // message, here is where that should be initiated. See sendNotification method below.
      //potentially long-running tasks (>=10 seconds or more) => Firebase Job Dispatcher
      // [START dispatch_job]
      DDWRTCompanionFirebaseMessagingHandlerJob
          .scheduleJob("A new DD-WRT Build is available", remoteMessageData);

    } catch (final Exception ignored) {
      //No worries
      ignored.printStackTrace();
      Crashlytics.logException(ignored);
    }
  }
  // [END receive_message]

}
