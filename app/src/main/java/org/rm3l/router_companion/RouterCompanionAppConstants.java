/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014-2022  Armel Soro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact Info: Armel Soro <armel+router_companion@rm3l.org>
 */

package org.rm3l.router_companion;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.common.base.Charsets;
import java.io.File;
import java.nio.charset.Charset;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.rm3l.ddwrt.BuildConfig;

/** App Constants */
public final class RouterCompanionAppConstants {

  public static class Permissions {

    public static final int INTERNET = 1;

    public static final int NETSTATE = 2;

    public static final int WIFI = 3;

    public static final int STORAGE = 4;

    public static final int STORAGE_LOCATION = 5;

    public static final int ACCESS_COARSE_LOCATION = 6;
  }

  public static final String LINE_SEPARATOR = System.getProperty("line.separator");

  public static final Charset CHARSET = Charsets.UTF_8;

  // Defaut ato-refresh interval: 5 minutes
  public static final long TILE_REFRESH_SECONDS = 5 * 60L;

  public static final String AD_FREE_APP_APPLICATION_ID =
      BuildConfig.APPLICATION_ID.replaceAll(".free", "");

  public static final String REMOTE_HELP_WEBSITE = "https://help.ddwrt-companion.app";

  public static final String REMOTE_HELP_WEBSITE_CHANGELOG =
      "https://help.ddwrt-companion.app/doc/release-notes.html";

  public static final long MAX_PRIVKEY_SIZE_BYTES = 300 * 1024L;

  public static final long MAX_CUSTOM_ICON_SIZE_BYTES = 100 * 1024L;

  public static final String WAN_CYCLE_DAY_PREF = "wan_cycle_day";

  public static final String AUTO_REFRESH_PREF = "auto_refresh.enable";

  public static final String AUTO_REFRESH_INTERVAL_SECONDS_PREF = "routerDataPullIntervalSeconds";

  public static final String SORTING_STRATEGY_PREF = "sortingStrategy";

  public static final String THEMING_PREF = "theming";

  public static final String STORAGE_LOCATION_PREF = "storage.location";

  public static final String SECURITY_PIN_LOCK_PREF = "pref_pin_lock";

  public static final String SECURITY_THIRD_PARTY_INTEGRATION = "third_party.integration";

  public static final String DATA_USAGE_NETWORK_PREF = "data_usage_network";

  public static final String DATA_SYNC_BACKUP_PREF = "data.sync.backup";

  public static final String EMPTY_STRING = "";

  public static final String ALWAYS_CHECK_CONNECTION_PREF_KEY = "alwaysCheckConnection";

  public static final String NOTIFICATIONS_ENABLE = "notifications.enable";

  public static final String NOTIFICATIONS_BG_SERVICE_ENABLE =
      "notifications.background.service.enable";

  public static final String NOTIFICATIONS_SOUND = "notifications.sound";

  public static final String NOTIFICATIONS_VIBRATE = "notifications.vibrate";

  public static final String NOTIFICATIONS_DISPLAY_ITEMS = "notifications.displayItems";

  public static final String NOTIFICATIONS_SYNC_INTERVAL_MINUTES_PREF =
      "notifications.syncIntervalMinutes";

  public static final String NOTIFICATIONS_CONNECTED_HOSTS_SYNC_INTERVAL_MINUTES_PREF =
      "notifications.connectedHosts.syncIntervalMinutes";

  public static final String NOTIFICATIONS_CONNECTED_HOSTS_ACTIVE_ONLY =
      "notifications.connectedHosts.activeOnly";

  public static final String NOTIFICATIONS_WAN_CONNECTIVITY_SYNC_INTERVAL_MINUTES_PREF =
      "notifications.wanConnectivity.syncIntervalMinutes";

  public static final String NOTIFICATIONS_OPENVPNCLIENT_SYNC_INTERVAL_MINUTES_PREF =
      "notifications.openvpnClient.syncIntervalMinutes";

  public static final String OVERVIEW_NTM_CHECK_ACTUAL_INTERNET_CONNECTIVITY_PREF =
      "ntm.check.internet.connectivity";

  public static final String NOTIFICATIONS_CHOICE_PREF = "notifications.choice";

  public static final long DEFAULT_THEME = 30L;

  public static final String IS_FIRST_LAUNCH_PREF_KEY = ("firstLaunch-" + BuildConfig.FLAVOR);

  public static final String FIRST_APP_LAUNCH_PREF_KEY =
      ("firstAppLaunch-" + BuildConfig.VERSION_NAME);

  public static final String OLD_IS_FIRST_LAUNCH_PREF_KEY = ("isFirstLaunch-" + BuildConfig.FLAVOR);

  public static final String DEFAULT_SHARED_PREFERENCES_KEY =
      (BuildConfig.APPLICATION_ID + "___preferences");

  public static final String WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_NAME =
      "wrtbwmon_ddwrtcompanion.sh";

  public static final String WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_PATH_REMOTE =
      ("/tmp/." + WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_NAME);

  public static final String DDWRTCOMPANION_WANACCESS_IPTABLES_CHAIN = "DDWRTCompWANAccess";

  public static final String OPENED_AT_LEAST_ONCE_PREF_KEY = "openedAtLeastOnce";

  public static final String EMPTY_VALUE_TO_DISPLAY = "-";

  public static final int MAX_ROUTERS_FREE_VERSION = 1;

  public static final String WIDGETS_PREFERENCES_KEY = (BuildConfig.APPLICATION_ID + "___widget");

  public static final String FILEPROVIDER_AUTHORITY =
      (BuildConfig.APPLICATION_ID + ".fileprovider");

  public static final String WEBSITE = "https://ddwrt-companion.app";

  public static final String ACRA_USER_EMAIL = "acra.user.email";

  public static final String ACRA_ENABLE = "acra.enable";

  public static final String DEBUG_MODE = "debug.mode";

  public static final String DEMO = "--- DEMO ---";

  public static final int RATING_INITIAL_LAUNCH_COUNT = 7;

  public static final String UNKNOWN = "UNKNOWN";

  public static final String OK = "OK";

  public static final String NOK = "NOK";

  public static final String DEFAULT_HTTP_PORT = "80";

  public static final long[] NO_VIBRATION_PATTERN = new long[] {0L};

  public static final String[] CLOUDINARY_OPTS = new String[] {"w_270", "h_150", "e_blur"};

  public static final String IMAGE_CDN_URL_PREFIX = "https://res.cloudinary.com/rm3l/image/upload";

  public static final String LAST_KNOWN_VERSION = "lastKnownVersion";

  public static final String BG_SERVICE_LAST_HANDLE = "bgServiceLastHandle";

  public static final String ROUTER_SPEED_TEST_SERVER = "routerSpeedTestServer";

  public static final String ROUTER_SPEED_TEST_MAX_FILE_SIZE_MB = "routerSpeedTestMaxFileSizeMB";

  public static final long ROUTER_SPEED_TEST_MAX_FILE_SIZE_MB_DEFAULT = 100;

  public static final String ROUTER_SPEED_TEST_DURATION_THRESHOLD_SECONDS =
      "routerSpeedTestDurationThresholdSeconds";

  public static final String ROUTER_SPEED_TEST_MEASUREMENT_UNIT = "routerSpeedTestUnit";

  public static final String ROUTER_SPEED_TEST_AUTO_MEASUREMENTS =
      "routerSpeedTestAutoMeasurements";
  public static final String ROUTER_SPEED_TEST_AUTO_MEASUREMENTS_SERVER =
      "routerSpeedTestAutoMeasurementsServer";
  public static final String ROUTER_SPEED_TEST_AUTO_MEASUREMENTS_SCHEDULE =
      "routerSpeedTestAutoMeasurementsSchedule";

  public static final String ROUTER_SPEED_TEST_AUTO_MEASUREMENTS_SERVER_DEFAULT = "NL";

  public static final String ROUTER_SPEED_TEST_DURATION_THRESHOLD_SECONDS_DEFAULT = "8";

  public static final String ROUTER_SPEED_TEST_SERVER_AUTO = "_auto_";

  public static final String ROUTER_SPEED_TEST_SERVER_RANDOM = "_random_";

  // TODO Enhancement: allow user to provide their own tile source (along with any creds)
  public static final OnlineTileSourceBase TILE_SOURCE = TileSourceFactory.DEFAULT_TILE_SOURCE;

  public static final String VPN_PPTP_TOGGLES_MUTUALLY_EXCLUSIVE =
      "vpn.pptp.toggles.mutually.exclusive";

  public static final String CLOUD_MESSAGING_TOPIC_DDWRT_BUILD_UPDATES = "DDWRTBuildUpdates";

  public static final String COLON = ":";

  public static final String SPACE = " ";

  public static final String DEMO_ROUTER_DNS = BuildConfig.APPLICATION_ID;

  public static final String COUNTRY_API_SERVER_FLAG =
      "http://www.geognos.com/api/en/countries/flag";

  public static final int MAX_ROUTER_SPEEDTEST_RESULTS_FREE_VERSION = 5;

  public static final String AWS_S3_FEEDBACK_PENDING_TRANSFER_PREF = "feedbacks_pending_transfer";

  public static final String PUBLIC_ROADMAP_WEBSITE = "https://ddwrt-companion.app/roadmap";

  public static final String FIREBASE_DYNAMIC_LINKS_BASE_URL =
      "https://firebasedynamiclinks.googleapis.com/v1/";

  public static final String IS_GD_URL_SHORTENER_BASE_URL = "https://is.gd/";

  public static final String UNIT_BYTE = "B";

  public static final String UNIT_BIT = "b";

  public static int MB = 1024 * 1024;

  public static final String DDWRT_RELEASE_REMOTE_HOST = "ftp.dd-wrt.com";

  public static final String DDWRT_RELEASE_BASE_URL =
      ("ftp://" + DDWRT_RELEASE_REMOTE_HOST + "/betas");

  public static final String DDWRT_RELEASE_URL_FORMAT = (DDWRT_RELEASE_BASE_URL + "/%s/%s");

  @NonNull
  public static File getClientsUsageDataFile(
      @NonNull final Context context, @NonNull final String routerUuid) {
    return new File(
        context.getFilesDir(), BuildConfig.APPLICATION_ID + "_Usage_" + routerUuid + ".bak");
  }

  private RouterCompanionAppConstants() {}
}
