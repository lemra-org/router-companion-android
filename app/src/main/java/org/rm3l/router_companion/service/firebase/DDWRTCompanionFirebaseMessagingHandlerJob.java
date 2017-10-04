package org.rm3l.router_companion.service.firebase;

import android.annotation.SuppressLint;
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
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import com.crashlytics.android.Crashlytics;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.google.common.base.Splitter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.api.urlshortener.goo_gl.GooGlService;
import org.rm3l.router_companion.api.urlshortener.goo_gl.resources.GooGlData;
import org.rm3l.router_companion.job.RouterCompanionJob;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.NetworkUtils;
import org.rm3l.router_companion.utils.notifications.NotificationHelperKt;
import retrofit2.Response;

import static org.rm3l.router_companion.RouterCompanionAppConstants.GOOGLE_API_KEY;

@SuppressWarnings("WeakerAccess")
public class DDWRTCompanionFirebaseMessagingHandlerJob extends RouterCompanionJob {

  public static final String TAG = DDWRTCompanionFirebaseMessagingHandlerJob.class.getSimpleName();

  public static final int FCM_NOTIFICATION_ID = 27635;
  public static final String FTP_DDWRT_HOST = "ftp.dd-wrt.com";
  public static final String FTP_DDWRT_FORMAT_BASE = ("ftp://" + FTP_DDWRT_HOST + "/betas");
  public static final String FTP_DDWRT_FORMAT = (FTP_DDWRT_FORMAT_BASE + "/%s/%s");

  private static final int MAX_RETRIES = 10;
  private static final String NOTIFICATION_TITLE = "_TITLE";
  public static final String RELEASES = "releases";

  private final GooGlService mGooGlService;
  private final SharedPreferences mGlobalPreferences;

  public DDWRTCompanionFirebaseMessagingHandlerJob(Context mContext) {
    super(mContext);
    mGlobalPreferences =
        mContext.getSharedPreferences(RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY,
            Context.MODE_PRIVATE);
    mGooGlService = NetworkUtils.createApiService(mContext,
        RouterCompanionAppConstants.URL_SHORTENER_API_BASE_URL, GooGlService.class);
  }

  @NonNull @Override protected Result onRunJob(Params params) {
    try {
      final PersistableBundleCompat extras = params.getExtras();
      final Set<String> keySet = extras.keySet();
      final Map<String, String> data = new HashMap<>();
      for (final String key : keySet) {
        data.put(key, extras.getString(key, ""));
      }
      this.sendNotification(data.get(NOTIFICATION_TITLE), data);
      return Result.SUCCESS;
    } catch (final Exception e) {
      final int failureCount = params.getFailureCount();
      if (failureCount > MAX_RETRIES) {
        //Do not continue
        Crashlytics.log(Log.WARN, TAG, "Gave up after attempting "+ failureCount +
            " times building and sending notification!");
        Crashlytics.logException(e);
        return Result.FAILURE;
      }
      //Reschedule
      Crashlytics.log(Log.DEBUG, TAG, "Re-attempting building and sending notification!");
      return Result.RESCHEDULE;
    }
  }

  @SuppressWarnings("SameParameterValue")
  public static void scheduleJob(String title, Map<String, String> remoteMessageData) {
    final PersistableBundleCompat extras = new PersistableBundleCompat();
    if (title != null) {
      extras.putString(NOTIFICATION_TITLE, TextUtils.isEmpty(title) ? "New notification" : title);
    }
    if (remoteMessageData != null) {
      for (final Map.Entry<String, String> entry : remoteMessageData.entrySet()) {
        extras.putString(entry.getKey(), entry.getValue());
      }
    }
    new JobRequest.Builder(TAG)
        .setExtras(extras)
        .setBackoffCriteria(4_000, JobRequest.BackoffPolicy.LINEAR)
        .setRequiredNetworkType(JobRequest.NetworkType.UNMETERED)
        .setRequiresBatteryNotLow(true)
        .build()
        .schedule();
  }

  /**
   * Create and show a simple notification containing the received FCM message.
   *
   * @param data FCM message body received.
   */
  private void sendNotification(final String title, final Map<String, String> data) throws IOException {

    final String messageBody = data.get(RELEASES);

    final List<String> releases = Splitter.on("\n").omitEmptyStrings().splitToList(messageBody);
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
    String releaseLinkShortened = releaseLink;
    try {
      final GooGlData gooGlData = new GooGlData();
      gooGlData.setLongUrl(releaseLink);
      final Response<GooGlData> response =
          mGooGlService.shortenLongUrl(GOOGLE_API_KEY, gooGlData)
              .execute();
      NetworkUtils.checkResponseSuccessful(response);
      //noinspection ConstantConditions
      releaseLinkShortened = response.body().getId();
    } catch (final Exception ignored) {
      //No worries
      ignored.printStackTrace();
    }

    Crashlytics.log(Log.DEBUG, TAG,
        "[Firebase] releaseLinkShortened: [" + releaseLinkShortened + "]");
    notifyReleaseAvailable(title, messageBody, releaseLinkShortened, mContext, mGlobalPreferences);
  }

  public static void notifyReleaseAvailable(String title,
      String messageBody,
      String releaseLinkShortened,
      Context mContext,
      SharedPreferences mGlobalPreferences) {
    // pending implicit intent to view url
    final Intent resultIntent = new Intent(Intent.ACTION_VIEW);
    resultIntent.setData(Uri.parse(releaseLinkShortened));

    final PendingIntent pendingIntent =
        PendingIntent.getActivity(mContext, FCM_NOTIFICATION_ID /* Request code */, resultIntent,
            PendingIntent.FLAG_ONE_SHOT);

    //        Intent intent = new Intent(this, RouterManagementActivity.class);
    //        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    //        PendingIntent pendingIntent = PendingIntent.getActivity(this,
    //                FCM_NOTIFICATION_ID /* Request code */, intent,
    //                PendingIntent.FLAG_ONE_SHOT);

    final Bitmap largeIcon = BitmapFactory.decodeResource(mContext.getResources(),
        R.mipmap.ic_launcher_ddwrt_companion);

    //        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    final NotificationCompat.Builder notificationBuilder =
        new NotificationCompat.Builder(mContext, Router.RouterFirmware.DDWRT.name())
            .setGroup(NotificationHelperKt.NOTIFICATION_GROUP_GENERAL_UPDATES)
            .setLargeIcon(largeIcon)
            .setSmallIcon(R.mipmap.ic_launcher_ddwrt_companion)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true);

    //Notification sound, if required
    final String ringtoneUri =
        mGlobalPreferences.getString(RouterCompanionAppConstants.NOTIFICATIONS_SOUND, null);
    if (ringtoneUri != null) {
      notificationBuilder.setSound(Uri.parse(ringtoneUri), AudioManager.STREAM_NOTIFICATION);
    }

    if (!mGlobalPreferences.getBoolean(RouterCompanionAppConstants.NOTIFICATIONS_VIBRATE, true)) {
      notificationBuilder.setDefaults(Notification.DEFAULT_LIGHTS)
          .setVibrate(RouterCompanionAppConstants.NO_VIBRATION_PATTERN);
      //                    if (ringtoneUri != null) {
      //                        mBuilder.setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND);
      //                    } else {
      //                        mBuilder.setDefaults(Notification.DEFAULT_LIGHTS);
      //                    }
    }
    notificationBuilder.setContentIntent(pendingIntent);

    NotificationManager notificationManager =
        (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

    if (notificationManager != null) {
      notificationManager.notify(FCM_NOTIFICATION_ID /* ID of notification */,
          notificationBuilder.build());
    }
  }

  public static class DDWRTRelease {
    public final Integer year;
    public final Date date;
    public final String revision;
    public final Long revisionNumber;

    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public DDWRTRelease(Integer year, Date date, String revision) {
      this.year = year;
      this.date = date;
      this.revision = revision;
      if (this.revision != null) {
        this.revisionNumber = Long.parseLong(this.revision.replace("r", "").trim());
      } else {
        this.revisionNumber = null;
      }
    }

    public DDWRTRelease(Integer year, String dateString, String revision) throws ParseException {
      this(year, dateFormat.parse(dateString), revision);
    }

    @SuppressLint("DefaultLocale") public String getDirectLink() {
      return String.format("%s/%d/%s-%s", FTP_DDWRT_FORMAT_BASE, year, dateFormat.format(date), revision);
    }

    @Override public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      DDWRTRelease that = (DDWRTRelease) o;

      if (year != null ? !year.equals(that.year) : that.year != null) return false;
      if (date != null ? !date.equals(that.date) : that.date != null) return false;
      return revision != null ? revision.equals(that.revision) : that.revision == null;
    }

    @Override public int hashCode() {
      int result = year != null ? year.hashCode() : 0;
      result = 31 * result + (date != null ? date.hashCode() : 0);
      result = 31 * result + (revision != null ? revision.hashCode() : 0);
      return result;
    }
  }
}
