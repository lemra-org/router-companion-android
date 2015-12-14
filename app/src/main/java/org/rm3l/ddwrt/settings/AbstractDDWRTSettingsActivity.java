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

import android.content.Context;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;

import com.google.common.base.Strings;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.Utils;
import org.rm3l.ddwrt.widgets.NumberPickerPreference;

import java.util.Map;
import java.util.Set;

import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;

public abstract class AbstractDDWRTSettingsActivity extends AppCompatActivity {

    public static final String SETTINGS = "Settings";

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     */
    protected static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
//        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        final PrefChangeListener sBindPreferenceSummaryToValueListener = new PrefChangeListener(preference.getContext());

        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        final SharedPreferences sharedPreferences = preference.getContext()
                .getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
//        final SharedPreferences sharedPreferences = PreferenceManager
//                .getDefaultSharedPreferences(preference.getContext());
        final Map<String, ?> preferencesMap = sharedPreferences.getAll();

        final Object value = preferencesMap.get(preference.getKey());
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                value != null ? value : "");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        final boolean themeLight = ColorUtils.isThemeLight(this);
        if (themeLight) {
            //Light
            setTheme(R.style.AppThemeLight);
        } else {
            //Default is Dark
            setTheme(R.style.AppThemeDark);
        }

        setContentView(R.layout.settings);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.settings_toolbar);
        if (toolbar != null) {
            final String subTitle = this.getToolbarSubtitle();
            toolbar.setTitle(SETTINGS);
            toolbar.setSubtitle(Strings.nullToEmpty(subTitle));
            toolbar.setTitleTextAppearance(getApplicationContext(),
                    R.style.ToolbarTitle);
            toolbar.setSubtitleTextAppearance(getApplicationContext(),
                    R.style.ToolbarSubtitle);
            toolbar.setTitleTextColor(ContextCompat.getColor(this,
                    R.color.white));
            toolbar.setSubtitleTextColor(ContextCompat.getColor(this,
                    R.color.white));
            setSupportActionBar(toolbar);
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        if (themeLight) {
            getWindow().getDecorView()
                    .setBackgroundColor(ContextCompat.getColor(this, android.R.color.white));
        }

        getFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_content_frame, this.getPreferenceFragment()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @NonNull
    protected abstract PreferenceFragment getPreferenceFragment();

    @Nullable
    protected String getToolbarSubtitle() {
        return null;
    }

    private static class PrefChangeListener implements Preference.OnPreferenceChangeListener {

        @NonNull
        private final Context mCtx;

        private PrefChangeListener(@NonNull final Context ctx) {
            mCtx = ctx;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof MultiSelectListPreference) {

                final MultiSelectListPreference multiSelectListPreference =
                        (MultiSelectListPreference) preference;
                multiSelectListPreference.setValues((Set<String>) value);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else if (preference instanceof NumberPickerPreference) {
                final NumberPickerPreference numberPickerPreference = (NumberPickerPreference) preference;
                preference.setSummary(numberPickerPreference.getMDescription() + stringValue);

            } else if (!(preference instanceof CheckBoxPreference ||
                preference instanceof SwitchPreference)) {
                // For all other preferences (but CheckBoxPreferences), set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }

            //Request Backup
            Utils.requestBackup(mCtx);

            return true;
        }
    }
}
