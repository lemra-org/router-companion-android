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
 * Contact Info: Armel Soro <armel+router_companion@rm3l.org>
 */
package org.rm3l.router_companion.settings

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import com.airbnb.deeplinkdispatch.DeepLink
import org.rm3l.ddwrt.R
import org.rm3l.router_companion.RouterCompanionAppConstants
import org.rm3l.router_companion.RouterCompanionAppConstants.ACRA_ENABLE
import org.rm3l.router_companion.RouterCompanionAppConstants.ACRA_USER_EMAIL
import org.rm3l.router_companion.RouterCompanionAppConstants.DATA_USAGE_NETWORK_PREF
import org.rm3l.router_companion.RouterCompanionAppConstants.NOTIFICATIONS_BG_SERVICE_ENABLE
import org.rm3l.router_companion.RouterCompanionAppConstants.NOTIFICATIONS_CHOICE_PREF
import org.rm3l.router_companion.RouterCompanionAppConstants.NOTIFICATIONS_SOUND
import org.rm3l.router_companion.RouterCompanionAppConstants.NOTIFICATIONS_VIBRATE
import org.rm3l.router_companion.RouterCompanionAppConstants.SECURITY_PIN_LOCK_PREF
import org.rm3l.router_companion.RouterCompanionAppConstants.SECURITY_THIRD_PARTY_INTEGRATION
import org.rm3l.router_companion.RouterCompanionAppConstants.THEMING_PREF

@DeepLink("dd-wrt://settings", "ddwrt://settings")
class RouterManagementSettingsActivity : AbstractDDWRTSettingsActivity() {

    class RouterManagementSettingsFragment : PreferenceFragment() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.router_management_settings)

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            //            bindPreferenceSummaryToValue(findPreference(ALWAYS_CHECK_CONNECTION_PREF_KEY));
            val themingPreference = findPreference(THEMING_PREF)
            themingPreference.title = "Theme"
            themingPreference.isEnabled = true

            AbstractDDWRTSettingsActivity.bindPreferenceSummaryToValue(themingPreference)

            findPreference(SECURITY_PIN_LOCK_PREF).onPreferenceClickListener = Preference.OnPreferenceClickListener {
                startActivity(Intent(activity, PinLockPreferenceActivity::class.java))
                true
            }

            AbstractDDWRTSettingsActivity.bindPreferenceSummaryToValue(findPreference(SECURITY_THIRD_PARTY_INTEGRATION))

            AbstractDDWRTSettingsActivity.bindPreferenceSummaryToValue(findPreference(DATA_USAGE_NETWORK_PREF))
            //            bindPreferenceSummaryToValue(findPreference(DATA_SYNC_BACKUP_PREF));

            //            bindPreferenceSummaryToValue(findPreference(STORAGE_LOCATION_PREF));

            AbstractDDWRTSettingsActivity.bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_SOUND))
            AbstractDDWRTSettingsActivity.bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_VIBRATE))
            AbstractDDWRTSettingsActivity.bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_BG_SERVICE_ENABLE))
//            AbstractDDWRTSettingsActivity.bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_SYNC_INTERVAL_MINUTES_PREF))
            AbstractDDWRTSettingsActivity.bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_CHOICE_PREF))

            AbstractDDWRTSettingsActivity.bindPreferenceSummaryToValue(findPreference(ACRA_ENABLE))
            //            bindPreferenceSummaryToValue(findPreference(DEBUG_MODE));
            //            bindPreferenceSummaryToValue(findPreference("acra.syslog.enable"));
            //            bindPreferenceSummaryToValue(findPreference(ACRA_DEVICEID_ENABLE));
            AbstractDDWRTSettingsActivity.bindPreferenceSummaryToValue(findPreference(ACRA_USER_EMAIL))
        }
    }

    override fun finish() {
        val data = Intent()
        setResult(Activity.RESULT_OK, data)

        super.finish()
    }

    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
        return super.getSharedPreferences(
            RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY,
            mode
        )
    }

    override fun getPreferenceFragment(): PreferenceFragment {
        return RouterManagementSettingsFragment()
    }
}
