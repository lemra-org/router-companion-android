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
package org.rm3l.router_companion.settings;

import static org.rm3l.router_companion.RouterCompanionAppConstants.AUTO_REFRESH_INTERVAL_SECONDS_PREF;
import static org.rm3l.router_companion.RouterCompanionAppConstants.AUTO_REFRESH_PREF;
import static org.rm3l.router_companion.RouterCompanionAppConstants.NOTIFICATIONS_CONNECTED_HOSTS_ACTIVE_ONLY;
import static org.rm3l.router_companion.RouterCompanionAppConstants.NOTIFICATIONS_ENABLE;
import static org.rm3l.router_companion.RouterCompanionAppConstants.ROUTER_SPEED_TEST_DURATION_THRESHOLD_SECONDS;
import static org.rm3l.router_companion.RouterCompanionAppConstants.ROUTER_SPEED_TEST_MAX_FILE_SIZE_MB;
import static org.rm3l.router_companion.RouterCompanionAppConstants.ROUTER_SPEED_TEST_SERVER;
import static org.rm3l.router_companion.RouterCompanionAppConstants.SORTING_STRATEGY_PREF;
import static org.rm3l.router_companion.RouterCompanionAppConstants.VPN_PPTP_TOGGLES_MUTUALLY_EXCLUSIVE;
import static org.rm3l.router_companion.RouterCompanionAppConstants.WAN_CYCLE_DAY_PREF;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import com.airbnb.deeplinkdispatch.DeepLink;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
@DeepLink({
        "dd-wrt://routers/{routerUuid}/settings", "ddwrt://routers/{routerUuid}/settings"
})
public class RouterSettingsActivity extends AbstractRouterSettingsActivity {

    public static class RouterSettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.router_settings);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference(SORTING_STRATEGY_PREF));
            bindPreferenceSummaryToValue(findPreference(AUTO_REFRESH_PREF));
            bindPreferenceSummaryToValue(findPreference(AUTO_REFRESH_INTERVAL_SECONDS_PREF));
            bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_ENABLE));
            //            bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_SOUND));
            //            bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_SYNC_INTERVAL_MINUTES_PREF));
            //            bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_DISPLAY_ITEMS));
            //            bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_CONNECTED_HOSTS_SYNC_INTERVAL_MINUTES_PREF));
            bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_CONNECTED_HOSTS_ACTIVE_ONLY));
            //            bindPreferenceSummaryToValue(findPreference(OVERVIEW_NTM_CHECK_ACTUAL_INTERNET_CONNECTIVITY_PREF));
            //            bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_WAN_CONNECTIVITY_SYNC_INTERVAL_MINUTES_PREF));
            //            bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_OPENVPNCLIENT_SYNC_INTERVAL_MINUTES_PREF));

            bindPreferenceSummaryToValue(findPreference(WAN_CYCLE_DAY_PREF));

            bindPreferenceSummaryToValue(findPreference(ROUTER_SPEED_TEST_SERVER));
            bindPreferenceSummaryToValue(findPreference(ROUTER_SPEED_TEST_MAX_FILE_SIZE_MB));
            bindPreferenceSummaryToValue(findPreference(ROUTER_SPEED_TEST_DURATION_THRESHOLD_SECONDS));
            bindPreferenceSummaryToValue(findPreference(VPN_PPTP_TOGGLES_MUTUALLY_EXCLUSIVE));
            //            bindPreferenceSummaryToValue(findPreference(ROUTER_SPEED_TEST_WITH_CURRENT_CONNECTION));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final Intent intent = getIntent();
        if (intent.getBooleanExtra(DeepLink.IS_DEEP_LINK, false)) {
            //Deep link
            final Bundle parameters = intent.getExtras();

            final String routerUuid = parameters.getString("routerUuid");
            intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, routerUuid);
        }
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    protected PreferenceFragment getPreferenceFragment() {
        return new RouterSettingsFragment();
    }
}
