package org.rm3l.router_companion.tasker;

import com.amazonaws.regions.Regions;

/** Created by rm3l on 06/08/16. */
public final class Constants {

  public static final String TAG = "DDWRTCompanionTaskerPlugin";

  // This means we need at least the app built from tag '7.1.0'
  public static final int DDWRT_COMPANION_MIN_VERSION_REQUIRED = 710000;

  public static final String DDWRT_COMPANION_MIN_VERSION_REQUIRED_STR = "7.1.0";

  public static final String FILEPROVIDER_AUTHORITY =
      (BuildConfig.APPLICATION_ID + ".fileprovider");

  public static final String FIREBASE_DYNAMIC_LINKS_BASE_URL =
      "https://firebasedynamiclinks.googleapis.com/v1/";

  //TODO Externalize
  public static final String FIREBASE_API_KEY = \"fake-api-key\";

  public static final String AWS_S3_FEEDBACK_PENDING_TRANSFER_PREF = "feedbacks_pending_transfer";

  public static final String Q_A_WEBSITE = "https://www.codewake.com/p/ddwrt-companion";

  public static final String DEFAULT_SHARED_PREFERENCES_KEY = \"fake-key\";

  public static final int MAX_ACTION_RUNS_FREE_VERSION = 10;

  private Constants() {
    throw new UnsupportedOperationException("Not instantiable");
  }
}
