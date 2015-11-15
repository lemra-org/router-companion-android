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
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.ReportingUtils;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.NOTIFICATIONS_CONNECTED_HOSTS_ACTIVE_ONLY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.NOTIFICATIONS_ENABLE;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.OVERVIEW_NTM_CHECK_ACTUAL_INTERNET_CONNECTIVITY_PREF;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.SORTING_STRATEGY_PREF;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.SYNC_INTERVAL_MILLIS_PREF;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.WAN_CYCLE_DAY_PREF;

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
public class RouterSettingsActivity extends AbstractDDWRTSettingsActivity {

    @NonNull
    private String mRouterUuid;

    @NonNull
    private Router mRouter;

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        if (isNullOrEmpty(this.mRouterUuid)) {
            Toast.makeText(this, "Whoops - internal error. Issue will be reported!", Toast.LENGTH_LONG).show();
            ReportingUtils.reportException(null, new IllegalStateException("RouterSettingsActivity: Router UUID is null: " + this.mRouterUuid));
            finish();
        }
        return super.getSharedPreferences(this.mRouterUuid, mode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.mRouterUuid = getIntent().getStringExtra(RouterManagementActivity.ROUTER_SELECTED);

        boolean doFinish = false;
        //noinspection ConstantConditions
        if ((mRouter = RouterManagementActivity.getDao(this).getRouter(this.mRouterUuid)) == null) {
            Toast.makeText(this, "No router set or router no longer exists", Toast.LENGTH_LONG).show();
            doFinish = true;
        }

        //Need to call super.onCreate prior to calling finish()
        super.onCreate(savedInstanceState);

        if (doFinish) {
            finish();
        }

    }

    @NonNull
    @Override
    protected PreferenceFragment getPreferenceFragment() {
        return new RouterSettingsFragment();
    }

    @Nullable
    @Override
    protected String getToolbarTitle() {
        if (mRouter == null) {
            return "Router Settings";
        }
        final String mRouterName = mRouter.getName();
        return String.format("Settings for '%s'",
                isNullOrEmpty(mRouterName) ? mRouter.getRemoteIpAddress() : mRouterName);
    }

    @Override
    public void finish() {
        final Intent data = new Intent();
        setResult(RESULT_OK, data);

        super.finish();
    }

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
            bindPreferenceSummaryToValue(findPreference(SYNC_INTERVAL_MILLIS_PREF));
            bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_ENABLE));
//            bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_SOUND));
//            bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_SYNC_INTERVAL_MINUTES_PREF));
//            bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_DISPLAY_ITEMS));
//            bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_CONNECTED_HOSTS_SYNC_INTERVAL_MINUTES_PREF));
            bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_CONNECTED_HOSTS_ACTIVE_ONLY));
            bindPreferenceSummaryToValue(findPreference(OVERVIEW_NTM_CHECK_ACTUAL_INTERNET_CONNECTIVITY_PREF));
//            bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_WAN_CONNECTIVITY_SYNC_INTERVAL_MINUTES_PREF));
//            bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_OPENVPNCLIENT_SYNC_INTERVAL_MINUTES_PREF));

            bindPreferenceSummaryToValue(findPreference(WAN_CYCLE_DAY_PREF));
        }

    }
}
