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

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import com.airbnb.deeplinkdispatch.DeepLink;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;

import static org.rm3l.router_companion.RouterCompanionAppConstants.ROUTER_SPEED_TEST_DURATION_THRESHOLD_SECONDS;
import static org.rm3l.router_companion.RouterCompanionAppConstants.ROUTER_SPEED_TEST_MAX_FILE_SIZE_MB;
import static org.rm3l.router_companion.RouterCompanionAppConstants.ROUTER_SPEED_TEST_SERVER;

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
    "dd-wrt://routers/{routerUuid}/speedtest/settings",
    "ddwrt://routers/{routerUuid}/speedtest/settings"
}) public class RouterSpeedTestSettingsActivity extends AbstractRouterSettingsActivity {

  @Override protected void onCreate(Bundle savedInstanceState) {
    final Intent intent = getIntent();
    if (intent.getBooleanExtra(DeepLink.IS_DEEP_LINK, false)) {
      //Deep link
      final Bundle parameters = intent.getExtras();

      final String routerUuid = parameters.getString("routerUuid");
      intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, routerUuid);
    }
    super.onCreate(savedInstanceState);
  }

  @NonNull @Override protected String getToolbarTitle() {
    return "Speed Test Settings";
  }

  @NonNull @Override protected PreferenceFragment getPreferenceFragment() {
    return new RouterSpeedTestSettingsFragment();
  }

  public static class RouterSpeedTestSettingsFragment extends PreferenceFragment {

    @Override public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      // Load the preferences from an XML resource
      addPreferencesFromResource(R.xml.router_speed_test_settings);

      // Bind the summaries of EditText/List/Dialog/Ringtone preferences
      // to their values. When their values change, their summaries are
      // updated to reflect the new value, per the Android Design
      // guidelines.
      bindPreferenceSummaryToValue(findPreference(ROUTER_SPEED_TEST_SERVER));
      bindPreferenceSummaryToValue(findPreference(ROUTER_SPEED_TEST_MAX_FILE_SIZE_MB));
      bindPreferenceSummaryToValue(findPreference(ROUTER_SPEED_TEST_DURATION_THRESHOLD_SECONDS));
      //            bindPreferenceSummaryToValue(findPreference(ROUTER_SPEED_TEST_WITH_CURRENT_CONNECTION));
    }
  }
}
