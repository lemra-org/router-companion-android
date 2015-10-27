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
package org.rm3l.ddwrt.tiles.services.wol;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;

import java.util.HashSet;
import java.util.Set;

import static android.widget.TextView.BufferType.EDITABLE;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.ddwrt.mgmt.RouterManagementActivity.ROUTER_SELECTED;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.WOL_ENABLE;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.WOL_HOSTNAME;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.WOL_INTERVAL;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.WOL_MACS;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.WOL_PASSWD;
import static org.rm3l.ddwrt.tiles.services.wol.WakeOnLanDaemonTile.WOL_DAEMON_NVRAMINFO;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;

public class EditWOLDaemonSettingsActivity extends ActionBarActivity {

    public static final String WOL_DAEMON_HOSTNAMES_PREF_KEY = \"fake-key\";
    private static final String TAG = EditWOLDaemonSettingsActivity.class.getSimpleName();

    private NVRAMInfo mNvramInfo;
    private String mRouterUuid;
    private SharedPreferences sharedPreferences;

    private Toolbar mToolbar;

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

        setContentView(R.layout.activity_wol_daemon_settings);

        if (themeLight) {
            final Resources resources = getResources();
            getWindow().getDecorView()
                    .setBackgroundColor(resources.getColor(android.R.color.white));
        }

        mToolbar = (Toolbar) findViewById(R.id.wol_daemon_settings_toolbar);
        if (mToolbar != null) {
            mToolbar.setTitle("Automatic WOL Settings");
            setSupportActionBar(mToolbar);
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        final Intent intent = getIntent();
        mNvramInfo = (NVRAMInfo) intent.getSerializableExtra(WOL_DAEMON_NVRAMINFO);

        if (mNvramInfo == null) {
            Toast.makeText(this, "Could not load WOL Daemon settings", Toast.LENGTH_SHORT).show();
            finish();
        }

        mRouterUuid = intent.getStringExtra(ROUTER_SELECTED);
        if (isNullOrEmpty(mRouterUuid)) {
            Toast.makeText(this, "Internal Error: Router could not be determined", Toast.LENGTH_SHORT).show();
            finish();
        }

        //Preferences saved globally, to be shared across different routers
        sharedPreferences = getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY,
                Context.MODE_PRIVATE);

        final EditText secureOnPasswordEditText = (EditText) findViewById(R.id.wol_daemon_settings_secure_on_password);
        ((CheckBox) findViewById(R.id.wol_daemon_settings_secure_on_password_show_checkbox))
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!isChecked) {
                            secureOnPasswordEditText.setInputType(
                                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        } else {
                            secureOnPasswordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        }
                        secureOnPasswordEditText.setSelection(secureOnPasswordEditText.length());
                    }
                });

    }

    @Override
    protected void onStart() {
        super.onStart();
        //Fill form with data loaded
        ((CheckBox) findViewById(R.id.wol_daemon_settings_status_flag))
                .setChecked("1".equals(mNvramInfo.getProperty(WOL_ENABLE)));

        ((EditText) findViewById(R.id.wol_daemon_settings_interval))
                .setText(mNvramInfo.getProperty(WOL_INTERVAL), EDITABLE);

        final AutoCompleteTextView hostnameAutoComplete = (AutoCompleteTextView) findViewById(R.id.wol_daemon_settings_hostname);
        final Set<String> hostnames = sharedPreferences
                .getStringSet(WOL_DAEMON_HOSTNAMES_PREF_KEY, new HashSet<String>());
        //noinspection ConstantConditions
        hostnameAutoComplete
                .setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                        hostnames.toArray(new String[hostnames.size()])));
        hostnameAutoComplete
                .setText(mNvramInfo.getProperty(WOL_HOSTNAME), EDITABLE);

        ((EditText) findViewById(R.id.wol_daemon_settings_secure_on_password))
                .setText(mNvramInfo.getProperty(WOL_PASSWD), EDITABLE);

        //noinspection ConstantConditions
        ((EditText) findViewById(R.id.wol_daemon_settings_mac_addresses))
                .setText(Joiner.on("\n").skipNulls().join(
                        Splitter.on(" ").omitEmptyStrings().split(
                                mNvramInfo.getProperty(WOL_MACS, DDWRTCompanionConstants.EMPTY_STRING))), EDITABLE);

    }

    @Override
    public void finish() {
        final Intent data = new Intent();

        //Resulting intent: NVRAM Info edited with user info
        data.putExtra(ROUTER_SELECTED, mRouterUuid);

        final NVRAMInfo nvramVarsToUpdate = new NVRAMInfo();
        //Compare each variable

        boolean applyNewPrefs = false;

        if (sharedPreferences == null) {
            setResult(RESULT_CANCELED, data);
            super.finish();
            return;
        }

        final SharedPreferences.Editor editor = sharedPreferences.edit();

        final String isWolDaemonOn = ((CheckBox) findViewById(R.id.wol_daemon_settings_status_flag))
                .isChecked() ? "1" : "0";
        if (!isWolDaemonOn.equals(mNvramInfo.getProperty(WOL_ENABLE))) {
            nvramVarsToUpdate.setProperty(WOL_ENABLE, isWolDaemonOn);
        }

        final String hostname = ((EditText) findViewById(R.id.wol_daemon_settings_hostname)).getText().toString();
        if (!hostname.equals(mNvramInfo.getProperty(WOL_HOSTNAME))) {
            nvramVarsToUpdate.setProperty(WOL_HOSTNAME, hostname);
            if (!isNullOrEmpty(hostname)) {
                final Set<String> mSharedPreferencesStringSet = new HashSet<>(sharedPreferences.getStringSet(WOL_DAEMON_HOSTNAMES_PREF_KEY,
                        new HashSet<String>()));
                if (!mSharedPreferencesStringSet.contains(hostname)) {
                    mSharedPreferencesStringSet.add(hostname);
                    editor
                            .putStringSet(WOL_DAEMON_HOSTNAMES_PREF_KEY, mSharedPreferencesStringSet);
                    applyNewPrefs = true;
                }
            }
        }

        final String interval = ((EditText) findViewById(R.id.wol_daemon_settings_interval)).getText().toString();
        if (!interval.equals(mNvramInfo.getProperty(WOL_INTERVAL))) {
            nvramVarsToUpdate.setProperty(WOL_INTERVAL, interval);
        }

        final String secureOnPassword = ((EditText) findViewById(R.id.wol_daemon_settings_secure_on_password)).getText().toString();
        if (!secureOnPassword.equals(mNvramInfo.getProperty(WOL_PASSWD))) {
            nvramVarsToUpdate.setProperty(WOL_PASSWD, secureOnPassword);
        }

        final String macAddresses = ((EditText) findViewById(R.id.wol_daemon_settings_mac_addresses)).getText().toString().replaceAll("\n", " ");
        if (!macAddresses.equals(mNvramInfo.getProperty(WOL_MACS))) {
            nvramVarsToUpdate.setProperty(WOL_MACS, macAddresses);
        }

        if (applyNewPrefs) {
            editor.apply();
        }

        Log.d(TAG, "vars that have changed: " + nvramVarsToUpdate);

        //Set extra
        data.putExtra(WOL_DAEMON_NVRAMINFO, nvramVarsToUpdate);

        if (!nvramVarsToUpdate.isEmpty()) {
            //Something changed - prompt confirmation dialog
            new AlertDialog.Builder(this)
                    .setIcon(R.drawable.ic_action_alert_warning)
                    .setMessage("Some settings have been updated. Do you want to save them?\n" +
                            "If you choose to apply the new settings, your router will be rebooted, " +
                            "and you might have to wait some time before connection is re-established.")
                    .setCancelable(true)
                    .setPositiveButton("Proceed!", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialogInterface, final int i) {
                            setResult(RESULT_OK, data);
                            EditWOLDaemonSettingsActivity.super.finish();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //Cancelled - nothing more to do!
                            setResult(RESULT_CANCELED, data);
                            EditWOLDaemonSettingsActivity.super.finish();
                        }
                    }).create().show();
        } else {
            setResult(RESULT_CANCELED, data);
            super.finish();
        }
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
}
