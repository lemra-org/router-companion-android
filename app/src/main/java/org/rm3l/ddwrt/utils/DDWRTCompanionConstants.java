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

    //FIXME Consider increasing this value prior to release
    public static final long TILE_REFRESH_MILLIS = 30 * 1000l;

    public static final String AD_FREE_APP_APPLICATION_ID = BuildConfig.APPLICATION_ID
            .replaceAll(".free", "");

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

    public static final String REMOTE_HELP_WEBSITE_CHANGELOG = "http://ddwrt-companion.rm3l.org/dev/changelog.html";

    public static final int DOORBELL_APPID = 0;
    public static final String DOORBELL_APIKEY = \"fake-api-key\";

    //FIXME Update prior to release
    public static final boolean TEST_MODE = false;
    public static final long MAX_PRIVKEY_SIZE_BYTES = 300 * 1024l;
    public static final String SYNC_INTERVAL_MILLIS_PREF = "syncIntervalMillis";
    public static final String SORTING_STRATEGY_PREF = "sortingStrategy";
    public static final String THEMING_PREF = "theming";
    public static final String DATA_USAGE_NETWORK_PREF = "data_usage_network";
    public static final String EMPTY_STRING = "";
    public static final String ALWAYS_CHECK_CONNECTION_PREF_KEY = \"fake-key\";
    public static final String NOTIFICATIONS_ENABLE = "notifications.enable";
    public static final String NOTIFICATIONS_BG_SERVICE_ENABLE = "notifications.background.service.enable";
    public static final String NOTIFICATIONS_SOUND = "notifications.sound";
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

    public static final long DEFAULT_THEME = 31l;
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

    //Disabled - does not seem to be profitable
    @Deprecated
    public static final String ADBUDDIZ_PUBLISHER_KEY = \"fake-key\";

    public static final String SUPPORT_WEBSITE = "http://rm3l.org/apps/ddwrt-companion/ddwrt/" +
            ((BuildConfig.WITH_ADS ? "_" : "") + "home");

    public static int MB = 1024 * 1024;

    public static final String COLON = ":";
    public static final String SPACE = " ";
    public static final String SHARP = "#";

    private DDWRTCompanionConstants() {
    }

    @NonNull
    public static File getClientsUsageDataFile(@NonNull final Context context, @NonNull final String routerUuid) {
        return new File(context.getFilesDir(),
                BuildConfig.APPLICATION_ID + "_Usage_" + routerUuid + ".bak");
    }


}
