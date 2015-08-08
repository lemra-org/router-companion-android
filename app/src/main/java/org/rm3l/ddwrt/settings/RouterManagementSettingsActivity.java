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
package org.rm3l.ddwrt.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;

import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.ACRA_DEVICEID_ENABLE;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.ACRA_ENABLE;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.ACRA_USER_EMAIL;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DATA_USAGE_NETWORK_PREF;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.NOTIFICATIONS_BG_SERVICE_ENABLE;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.NOTIFICATIONS_SOUND;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.NOTIFICATIONS_SYNC_INTERVAL_MINUTES_PREF;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.THEMING_PREF;

public class RouterManagementSettingsActivity extends AbstractDDWRTSettingsActivity {

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return super.getSharedPreferences(DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY, mode);
    }

    @NonNull
    @Override
    protected PreferenceFragment getPreferenceFragment() {
        return new RouterManagementSettingsFragment();
    }

    @Nullable
    @Override
    protected String getToolbarTitle() {
        return "Settings";
    }

    @Override
    public void finish() {
        final Intent data = new Intent();
        setResult(RESULT_OK, data);

        super.finish();
    }

    public static class RouterManagementSettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.router_management_settings);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
//            bindPreferenceSummaryToValue(findPreference(ALWAYS_CHECK_CONNECTION_PREF_KEY));
            bindPreferenceSummaryToValue(findPreference(THEMING_PREF));
            bindPreferenceSummaryToValue(findPreference(DATA_USAGE_NETWORK_PREF));

            bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_SOUND));
            bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_BG_SERVICE_ENABLE));
            bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_SYNC_INTERVAL_MINUTES_PREF));

            bindPreferenceSummaryToValue(findPreference(ACRA_ENABLE));
//            bindPreferenceSummaryToValue(findPreference("acra.syslog.enable"));
            bindPreferenceSummaryToValue(findPreference(ACRA_DEVICEID_ENABLE));
            bindPreferenceSummaryToValue(findPreference(ACRA_USER_EMAIL));
        }
    }
}
