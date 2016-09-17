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

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.view.MenuItem;

import com.airbnb.deeplinkdispatch.DeepLink;

import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.wordpress.passcodelock.PasscodePreferenceFragment;

import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.ACRA_ENABLE;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.ACRA_USER_EMAIL;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DATA_USAGE_NETWORK_PREF;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.NOTIFICATIONS_BG_SERVICE_ENABLE;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.NOTIFICATIONS_CHOICE_PREF;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.NOTIFICATIONS_SOUND;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.NOTIFICATIONS_SYNC_INTERVAL_MINUTES_PREF;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.NOTIFICATIONS_VIBRATE;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.SECURITY_PIN_LOCK_PREF;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.THEMING_PREF;

@DeepLink({"dd-wrt://settings",
        "ddwrt://settings"})
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
            final Preference themingPreference = findPreference(THEMING_PREF);
            if (BuildConfig.WITH_ADS) {
                themingPreference.setSummary("Upgrade to switch app theme");
                themingPreference.setEnabled(false);
            } else {
                themingPreference.setSummary("Theme");
                themingPreference.setEnabled(true);
            }

            bindPreferenceSummaryToValue(themingPreference);

            findPreference(SECURITY_PIN_LOCK_PREF)
                    .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            final PinLockFragment pinLockFragment = new PinLockFragment();
                            final FragmentTransaction transaction = getFragmentManager().beginTransaction();
                            transaction.replace(R.id.settings_content_frame, pinLockFragment );
                            transaction.addToBackStack(null);  // if written, this transaction will be added to backstack
                            transaction.commit();
//
//                            // Display the fragment as the main content.
//                            getFragmentManager().beginTransaction()
//                                    .replace(R.id.settings_content_frame, new PinLockFragment())
//                                    .commit();
                            return true;
                        }
                    });

            bindPreferenceSummaryToValue(findPreference(DATA_USAGE_NETWORK_PREF));
//            bindPreferenceSummaryToValue(findPreference(DATA_SYNC_BACKUP_PREF));

//            bindPreferenceSummaryToValue(findPreference(STORAGE_LOCATION_PREF));

            bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_SOUND));
            bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_VIBRATE));
            bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_BG_SERVICE_ENABLE));
            bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_SYNC_INTERVAL_MINUTES_PREF));
            bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_CHOICE_PREF));

            bindPreferenceSummaryToValue(findPreference(ACRA_ENABLE));
//            bindPreferenceSummaryToValue(findPreference(DEBUG_MODE));
//            bindPreferenceSummaryToValue(findPreference("acra.syslog.enable"));
//            bindPreferenceSummaryToValue(findPreference(ACRA_DEVICEID_ENABLE));
            bindPreferenceSummaryToValue(findPreference(ACRA_USER_EMAIL));
        }
    }

    public static class PinLockFragment extends PasscodePreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_pin_lock);
            setHasOptionsMenu(true);
        }

        @Override
        public void onStart() {
            super.onStart();

            setPreferences(findPreference(getString(R.string.pref_key_passcode_toggle)),
                    findPreference(getString(R.string.pref_key_change_passcode)));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                getActivity().finish();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
