/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014  Armel Soro
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
 * Contact Info: Armel Soro <apps+ddwrt@rm3l.org>
 */

package org.rm3l.ddwrt.utils;

import android.content.Context;
import android.support.annotation.NonNull;

import org.apache.commons.lang3.CharEncoding;
import org.rm3l.ddwrt.BuildConfig;

import java.io.File;
import java.nio.charset.Charset;

/**
 * App Constants
 */
public final class DDWRTCompanionConstants {

    public static final Charset CHARSET = Charset.forName(CharEncoding.UTF_8);

    public static final long TILE_REFRESH_MILLIS = 3600 * 1000l;

    public static final String AD_FREE_APP_APPLICATION_ID = BuildConfig.APPLICATION_ID
            .replaceAll(".free", "");

    //FIXME JUST FOR TESTS
    public static final String MOBFOX_INVENTORY_HASH = "";
//    public static final String MOBFOX_INVENTORY_HASH = "xxx";
    public static final String MOBFOX_INVENTORY_ID = "xxx";
    public static final String MOBFOX_REQUEST_URL = "http://my.mobfox.com/request.php";

    public static final String AVOCARROT_APIKEY = \"fake-api-key\";
    public static final String AVOCARROT_FEED_PLACEMENT_KEY = \"fake-key\";
    public static final String AVOCARROT_LIST_PLACEMENT_KEY = \"fake-key\";
    public static final String AVOCARROT_INTERSTITIAL_PLACEMENT_KEY = \"fake-key\";

    //This is only used to check feedback submitted by end-users
    public static final String PUBKEY = \"fake-key\";
            "AY5ab5Nbu" +
            "6fMj7xRnc" +
            "dGgoNSvYM" +
            "BT6B42r2p" +
            "bp/mABgAz" +
            "8" +
            "I";

    public static final String REMOTE_HELP_WEBSITE = "http://ddwrt-companion.rm3l.org";

    public static final String REMOTE_HELP_WEBSITE_CHANGELOG = "http://ddwrt-companion.rm3l.org/doc/release-notes.html";

    public static final int DOORBELL_APPID = 0;
    public static final String DOORBELL_APIKEY = \"fake-api-key\";

    //FIXME Update prior to release
    public static final boolean TEST_MODE = false;
    public static final long MAX_PRIVKEY_SIZE_BYTES = 300 * 1024l;

    public static final String WAN_CYCLE_DAY_PREF = "wan_cycle_day";

    public static final String SYNC_INTERVAL_MILLIS_PREF = "syncIntervalMillis";
    public static final String SORTING_STRATEGY_PREF = "sortingStrategy";
    public static final String THEMING_PREF = "theming";

    public static final String STORAGE_LOCATION_PREF = "storage.location";

    public static final String DATA_USAGE_NETWORK_PREF = "data_usage_network";
    public static final String DATA_SYNC_BACKUP_PREF = "data.sync.backup";
    public static final String EMPTY_STRING = "";
    public static final String ALWAYS_CHECK_CONNECTION_PREF_KEY = \"fake-key\";
    public static final String NOTIFICATIONS_ENABLE = "notifications.enable";
    public static final String NOTIFICATIONS_BG_SERVICE_ENABLE = "notifications.background.service.enable";
    public static final String NOTIFICATIONS_SOUND = "notifications.sound";
    public static final String NOTIFICATIONS_VIBRATE = "notifications.vibrate";
    public static final String NOTIFICATIONS_DISPLAY_ITEMS = "notifications.displayItems";
    public static final String NOTIFICATIONS_SYNC_INTERVAL_MINUTES_PREF =
            "notifications.syncIntervalMinutes";
    public static final String NOTIFICATIONS_CONNECTED_HOSTS_SYNC_INTERVAL_MINUTES_PREF =
            "notifications.connectedHosts.syncIntervalMinutes";
    public static final String NOTIFICATIONS_CONNECTED_HOSTS_ACTIVE_ONLY = "notifications.connectedHosts.activeOnly";
    public static final String NOTIFICATIONS_WAN_CONNECTIVITY_SYNC_INTERVAL_MINUTES_PREF =
            "notifications.wanConnectivity.syncIntervalMinutes";
    public static final String NOTIFICATIONS_OPENVPNCLIENT_SYNC_INTERVAL_MINUTES_PREF =
            "notifications.openvpnClient.syncIntervalMinutes";
    public static final String OVERVIEW_NTM_CHECK_ACTUAL_INTERNET_CONNECTIVITY_PREF =
            "ntm.check.internet.connectivity";
    public static final String NOTIFICATIONS_CHOICE_PREF = "notifications.choice";

    public static final long DEFAULT_THEME = 30l;
    public static final String ACRA_BACKEND_URL = "https://collector.tracepot.com/75ca4f41";
    public static final String IS_FIRST_LAUNCH_PREF_KEY = \"fake-key\";
    public static final String FIRST_APP_LAUNCH_PREF_KEY = \"fake-key\";
    public static final String OLD_IS_FIRST_LAUNCH_PREF_KEY = \"fake-key\";

    public static final String TRACEPOT_DEVELOP_MODE = "TRACEPOT_DEVELOP_MODE";
    public static final String DEFAULT_SHARED_PREFERENCES_KEY = \"fake-key\";
    public static final String MAC_OUI_SEARCH_URL = "http://standards.ieee.org/cgi-bin/ouisearch?";
    public static final String WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_NAME = "wrtbwmon_ddwrtcompanion.sh";
    public static final String WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_PATH_REMOTE = ("/tmp/." +
            WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_NAME);
    public static final String DDWRTCOMPANION_WANACCESS_IPTABLES_CHAIN = "DDWRTCompWANAccess";
    public static final String OPENED_AT_LEAST_ONCE_PREF_KEY = \"fake-key\";
    public static final String EMPTY_VALUE_TO_DISPLAY = "-";
    public static final int MAX_ROUTERS_FREE_VERSION = 1;

    public static final String WIDGETS_PREFERENCES_KEY = \"fake-key\";
//    public static final String FILEPROVIDER_AUTHORITY = "org.rm3l.fileprovider";
    public static final String FILEPROVIDER_AUTHORITY = (BuildConfig.APPLICATION_ID + ".fileprovider");

    //Disabled - no longer needed as it does not seem to be that profitable
    @Deprecated
    public static final String ADBUDDIZ_PUBLISHER_KEY = \"fake-key\";

    public static final String SUPPORT_WEBSITE = "http://rm3l.org/apps/ddwrt-companion/ddwrt/" +
            ((BuildConfig.WITH_ADS ? "_" : "") + "home");
    public static final String ACRA_USER_EMAIL = "acra.user.email";
    public static final String ACRA_DEVICEID_ENABLE = "acra.deviceid.enable";
    public static final String ACRA_ENABLE = "acra.enable";
    public static final String DEBUG_MODE = "debug.mode";
    public static final String DEMO = "--- DEMO ---";
    public static final int RATING_INITIAL_LAUNCH_COUNT = 7;
    public static final String AD_LAST_INTERSTITIAL_PREF = "ad.lastInterstitial";
    public static final String UNKNOWN = "UNKNOWN";
    public static final String OK = "OK";
    public static final String NOK = "NOK";
    public static final String DEFAULT_HTTP_PORT = "80";
    public static final long[] NO_VIBRATION_PATTERN = new long[]{0l};

    public static final String[] CLOUDINARY_OPTS = new String[] {"w_270","h_150","e_blur"};
    public static final String IMAGE_CDN_URL_PREFIX = "http://res.cloudinary.com/rm3l/image/upload";
    public static final String LAST_KNOWN_VERSION = "lastKnownVersion";
    public static final String BG_SERVICE_LAST_HANDLE = "bgServiceLastHandle";

    public static int MB = 1024 * 1024;

    public static final String COLON = ":";
    public static final String SPACE = " ";
    public static final String SHARP = "#";
    
    public static final String TO_REMOVE = "TO_REMOVE";

    public static final String DEMO_ROUTER_DNS = BuildConfig.APPLICATION_ID;

    public static final long DELAY_BETWEEN_TWO_CONSECUTIVE_INTERSTITIAL_ADS_MINUTES = 20l;

    public static final String IP_TO_PING_TO_CHECK_INTERNET_CONNECTIVITY = "8.8.8.8";

    public static final long DRAWER_CLOSE_DELAY_MS = 10l;

    /**
     * Disabled for now (until this PR has been merged and released:
     * https://github.com/amlcurran/ShowcaseView/pull/312)
     * This appears to cause more and more issues on some devices
     */
    public static final boolean SHOWCASEVIEW_ENABLED = false;

    public static final String GEO_IP_API_SERVER = "http://api.petabyet.com/geoip/";

    public static final String COUNTRY_API_SERVER_FLAG = "http://www.geognos.com/api/en/countries/flag";

    public static class Permissions {
        public static final int INTERNET = 1;
        public static final int NETSTATE = 2;
        public static final int WIFI = 3;
        public static final int STORAGE = 4;
    }

    private DDWRTCompanionConstants() {
    }

    @NonNull
    public static File getClientsUsageDataFile(@NonNull final Context context, @NonNull final String routerUuid) {
        return new File(context.getFilesDir(),
                BuildConfig.APPLICATION_ID + "_Usage_" + routerUuid + ".bak");
    }


}