package org.rm3l.router_companion.utils;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.router_companion.RouterCompanionAppConstants.ACRA_ENABLE;
import static org.rm3l.router_companion.RouterCompanionAppConstants.ACRA_USER_EMAIL;
import static org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import java.util.Map;
import org.rm3l.ddwrt.BuildConfig;

// import org.acra.ACRA;

/** Created by rm3l on 07/11/15. */
public final class ReportingUtils {

  public static final String TAG = ReportingUtils.class.getSimpleName();

  public static final String EVENT_FIRST_LAUNCH = "New install or update";

  public static final String EVENT_ACTION_TRIGGERED = "Action triggered";

  public static final String EVENT_AGREEMENT_TO_RESTORE_ROUTER = "Agreement to restore router";

  public static final String EVENT_ROUTER_ADDED = "Router added";

  public static final String EVENT_ROUTER_UPDATED = "Router updated";

  public static final String EVENT_ROUTER_DELETED = "Router deleted";

  public static final String EVENT_WIDGET_INSTALLED = "Widget installed";

  public static final String EVENT_MANUAL_REFRESH = "Manual Refresh";

  public static final String EVENT_MENU_ITEM = "Menu item selected";

  public static final String EVENT_RATING_INVITATON = "Rating bar";

  public static final String EVENT_IMAGE_DOWNLOAD = "Image download";

  public static final String EVENT_ROUTER_OPENED = "Router opened";

  public static final String EVENT_SPEEDTEST = "SpeedTest";

  public static final String EVENT_FEEDBACK = "Feedback";

  public static void reportEvent(
      @NonNull final String eventName, @Nullable final Map<String, Object> attributes) {

    FirebaseCrashlytics.getInstance().log("eventName: [" + eventName + "]");
    if (isNullOrEmpty(eventName)) {
      return;
    }
    //        final CustomEvent customEvent =
    //                new CustomEvent(eventName).putCustomAttribute("DEBUG",
    // Boolean.toString(BuildConfig.DEBUG))
    //                        .putCustomAttribute("WITH_ADS",
    // Boolean.toString(BuildConfig.WITH_ADS));
    //
    //        if (attributes != null) {
    //            for (final Map.Entry<String, Object> entry : attributes.entrySet()) {
    //                final String key = entry.getKey();
    //                if (key == null) {
    //                    continue;
    //                }
    //                final Object value = entry.getValue();
    //                if (value instanceof Number) {
    //                    customEvent.putCustomAttribute(key, (Number) value);
    //                } else if (value instanceof String) {
    //                    customEvent.putCustomAttribute(key, (String) value);
    //                } else if (value != null) {
    //                    customEvent.putCustomAttribute(key, value.toString());
    //                }
    //            }
    //        }
    //        Answers.getInstance().logCustom(customEvent);
    // TODO cf.
    // https://firebase.google.com/docs/crashlytics/switch-to-analytics.md?authuser=1&platform=android
    //        Bundle bundle = new Bundle();
    //        bundle.putString(Param.ITEM_NAME, onClickIntent.getComponent() != null ?
    // onClickIntent.getComponent()
    //                .getShortClassName() : "???");
    //        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Tile OnClick");
    //        bundle.putString(Param.ITEM_ID, this.getClass().getSimpleName());
  }

  public static void reportException(
      @Nullable final Context context, @NonNull final Throwable error) {

    if (BuildConfig.DEBUG) {
      // Crashlytics is disabled in DEBUG builds
      Log.e(TAG, error.getMessage());
      error.printStackTrace();
    } else {
      if (context != null) {
        final SharedPreferences sharedPreferences =
            context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        if (sharedPreferences.getBoolean(ACRA_ENABLE, true)) {

          // ACRA Notification
          //            ACRA.getErrorReporter().handleSilentException(error);

          // Crashlytics Notification
          final String acraEmailAddr = sharedPreferences.getString(ACRA_USER_EMAIL, null);
          if (!isNullOrEmpty(acraEmailAddr)) {
            FirebaseCrashlytics.getInstance().setUserId(acraEmailAddr);
          }
        }
      }
      FirebaseCrashlytics.getInstance().recordException(error);
    }
  }

  //    public static void reportRatingEvent(@NonNull final RatingEvent ratingEvent) {
  //        Answers.getInstance().logRating(ratingEvent);
  //    }

  private ReportingUtils() {}
}
