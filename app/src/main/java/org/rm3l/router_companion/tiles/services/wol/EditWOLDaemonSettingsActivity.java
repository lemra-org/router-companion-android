/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014-2022  Armel Soro
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
package org.rm3l.router_companion.tiles.services.wol;

import static android.widget.TextView.BufferType.EDITABLE;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.router_companion.tiles.services.wol.WakeOnLanDaemonTile.WOL_DAEMON_NVRAMINFO;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import java.util.HashSet;
import java.util.Set;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.Utils;

public class EditWOLDaemonSettingsActivity extends AppCompatActivity {

  public static final String WOL_DAEMON_HOSTNAMES_PREF_KEY = \"fake-key\";

  private static final String TAG = EditWOLDaemonSettingsActivity.class.getSimpleName();

  private NVRAMInfo mNvramInfo;

  private String mRouterUuid;

  private Toolbar mToolbar;

  private SharedPreferences sharedPreferences;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final Intent intent = getIntent();
    mNvramInfo = (NVRAMInfo) intent.getSerializableExtra(WOL_DAEMON_NVRAMINFO);

    if (mNvramInfo == null) {
      Toast.makeText(this, "Could not load WOL Daemon settings", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    mRouterUuid = intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);
    if (isNullOrEmpty(mRouterUuid)) {
      Toast.makeText(this, "Internal Error: Router could not be determined", Toast.LENGTH_SHORT)
          .show();
      finish();
      return;
    }

    final DDWRTCompanionDAO dao = RouterManagementActivity.Companion.getDao(this);
    final Router router;
    if ((router = dao.getRouter(mRouterUuid)) == null) {
      Toast.makeText(this, "Internal Error: Router could not be determined", Toast.LENGTH_SHORT)
          .show();
      finish();
      return;
    }

    ColorUtils.Companion.setAppTheme(this, router.getRouterFirmware(), false);

    final boolean themeLight = ColorUtils.Companion.isThemeLight(this);
    //        if (themeLight) {
    //            //Light
    //            setTheme(R.style.AppThemeLight);
    ////            getWindow().getDecorView()
    ////                    .setBackgroundColor(ContextCompat.getColor(this,
    ////                            android.R.color.white));
    //        } else {
    //            //Default is Dark
    //            setTheme(R.style.AppThemeDark);
    //        }

    setContentView(R.layout.activity_wol_daemon_settings);

    mToolbar = findViewById(R.id.wol_daemon_settings_toolbar);
    if (mToolbar != null) {
      mToolbar.setTitle("Automatic WOL Settings");
      mToolbar.setSubtitle(
          String.format(
              "%s (%s:%d)",
              router.getDisplayName(), router.getRemoteIpAddress(), router.getRemotePort()));
      mToolbar.setTitleTextAppearance(getApplicationContext(), R.style.ToolbarTitle);
      mToolbar.setSubtitleTextAppearance(getApplicationContext(), R.style.ToolbarSubtitle);
      mToolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
      mToolbar.setSubtitleTextColor(ContextCompat.getColor(this, R.color.white));
      setSupportActionBar(mToolbar);
    }

    final ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
      actionBar.setHomeButtonEnabled(true);
    }

    // Preferences saved globally, to be shared across different routers
    sharedPreferences = getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);

    final EditText secureOnPasswordEditText =
        findViewById(R.id.wol_daemon_settings_secure_on_password);
    ((CheckBox) findViewById(R.id.wol_daemon_settings_secure_on_password_show_checkbox))
        .setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener() {
              @Override
              public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                  secureOnPasswordEditText.setInputType(
                      InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                } else {
                  secureOnPasswordEditText.setInputType(
                      InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                }
                secureOnPasswordEditText.setSelection(secureOnPasswordEditText.length());
              }
            });

    fillForm();
  }

  @Override
  public void finish() {
    final Intent data = new Intent();

    // Resulting intent: NVRAM Info edited with user info
    data.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouterUuid);

    final NVRAMInfo nvramVarsToUpdate = new NVRAMInfo();
    // Compare each variable

    boolean applyNewPrefs = false;

    if (sharedPreferences == null) {
      setResult(RESULT_CANCELED, data);
      super.finish();
      return;
    }

    final SharedPreferences.Editor editor = sharedPreferences.edit();

    final String isWolDaemonOn =
        ((CheckBox) findViewById(R.id.wol_daemon_settings_status_flag)).isChecked() ? "1" : "0";
    if (!isWolDaemonOn.equals(mNvramInfo.getProperty(NVRAMInfo.Companion.getWOL_ENABLE()))) {
      nvramVarsToUpdate.setProperty(NVRAMInfo.Companion.getWOL_ENABLE(), isWolDaemonOn);
    }

    final String hostname =
        ((EditText) findViewById(R.id.wol_daemon_settings_hostname)).getText().toString();
    if (!hostname.equals(mNvramInfo.getProperty(NVRAMInfo.Companion.getWOL_HOSTNAME()))) {
      nvramVarsToUpdate.setProperty(NVRAMInfo.Companion.getWOL_HOSTNAME(), hostname);
      if (!isNullOrEmpty(hostname)) {
        final Set<String> mSharedPreferencesStringSet =
            new HashSet<>(
                sharedPreferences.getStringSet(
                    WOL_DAEMON_HOSTNAMES_PREF_KEY, new HashSet<String>()));
        if (!mSharedPreferencesStringSet.contains(hostname)) {
          mSharedPreferencesStringSet.add(hostname);
          editor.putStringSet(WOL_DAEMON_HOSTNAMES_PREF_KEY, mSharedPreferencesStringSet);
          applyNewPrefs = true;
        }
      }
    }

    final String interval =
        ((EditText) findViewById(R.id.wol_daemon_settings_interval)).getText().toString();
    if (!interval.equals(mNvramInfo.getProperty(NVRAMInfo.Companion.getWOL_INTERVAL()))) {
      nvramVarsToUpdate.setProperty(NVRAMInfo.Companion.getWOL_INTERVAL(), interval);
    }

    final String secureOnPassword =
        ((EditText) findViewById(R.id.wol_daemon_settings_secure_on_password)).getText().toString();
    if (!secureOnPassword.equals(mNvramInfo.getProperty(NVRAMInfo.Companion.getWOL_PASSWD()))) {
      nvramVarsToUpdate.setProperty(NVRAMInfo.Companion.getWOL_PASSWD(), secureOnPassword);
    }

    final String macAddresses =
        ((EditText) findViewById(R.id.wol_daemon_settings_mac_addresses))
            .getText()
            .toString()
            .replaceAll("\n", " ");
    if (!macAddresses.equals(mNvramInfo.getProperty(NVRAMInfo.Companion.getWOL_MACS()))) {
      nvramVarsToUpdate.setProperty(NVRAMInfo.Companion.getWOL_MACS(), macAddresses);
    }

    if (applyNewPrefs) {
      editor.apply();
    }

    FirebaseCrashlytics.getInstance().log("vars that have changed: " + nvramVarsToUpdate);

    // Set extra
    data.putExtra(WOL_DAEMON_NVRAMINFO, nvramVarsToUpdate);

    if (!nvramVarsToUpdate.isEmpty()) {
      // Something changed - prompt confirmation dialog
      new AlertDialog.Builder(this)
          .setIcon(R.drawable.ic_action_alert_warning)
          .setMessage(
              "Some settings have been updated. Do you want to save them?\n"
                  + "If you choose to apply the new settings, your router will be rebooted, "
                  + "and you might have to wait some time before connection is re-established.")
          .setCancelable(true)
          .setPositiveButton(
              "Proceed!",
              new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialogInterface, final int i) {
                  setResult(RESULT_OK, data);
                  EditWOLDaemonSettingsActivity.super.finish();
                }
              })
          .setNegativeButton(
              "Cancel",
              new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                  // Cancelled - nothing more to do!
                  setResult(RESULT_CANCELED, data);
                  EditWOLDaemonSettingsActivity.super.finish();
                }
              })
          .create()
          .show();
    } else {
      setResult(RESULT_CANCELED, data);
      super.finish();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_activity_edit_wol_daemon_settings, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int i = item.getItemId();
    if (i == android.R.id.home) {
      onBackPressed();
      return true;
    } else if (i == R.id.action_feedback) {
      Utils.openFeedbackForm(this, mRouterUuid);
      //                final Intent intent = new Intent(EditWOLDaemonSettingsActivity.this,
      // FeedbackActivity.class);
      //                intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouterUuid);
      //                final File screenshotFile = new File(getCacheDir(),
      // "feedback_screenshot.png");
      //                ViewGroupUtils.exportViewToFile(EditWOLDaemonSettingsActivity.this,
      //                        getWindow().getDecorView(), screenshotFile);
      //                intent.putExtra(FeedbackActivity.SCREENSHOT_FILE,
      // screenshotFile.getAbsolutePath());
      //                intent.putExtra(FeedbackActivity.CALLER_ACTIVITY,
      // this.getClass().getCanonicalName());
      //                startActivity(intent);
      ////                Utils.buildFeedbackDialog(this, true);
      return true;
    } else {
    }

    return super.onOptionsItemSelected(item);
  }

  private void fillForm() {
    // Fill form with data loaded
    ((CheckBox) findViewById(R.id.wol_daemon_settings_status_flag))
        .setChecked("1".equals(mNvramInfo.getProperty(NVRAMInfo.Companion.getWOL_ENABLE())));

    ((EditText) findViewById(R.id.wol_daemon_settings_interval))
        .setText(mNvramInfo.getProperty(NVRAMInfo.Companion.getWOL_INTERVAL()), EDITABLE);

    final AutoCompleteTextView hostnameAutoComplete =
        findViewById(R.id.wol_daemon_settings_hostname);
    final Set<String> hostnames =
        sharedPreferences.getStringSet(WOL_DAEMON_HOSTNAMES_PREF_KEY, new HashSet<String>());
    //noinspection ConstantConditions
    hostnameAutoComplete.setAdapter(
        new ArrayAdapter<>(
            this,
            android.R.layout.simple_list_item_1,
            hostnames.toArray(new String[hostnames.size()])));
    hostnameAutoComplete.setText(
        mNvramInfo.getProperty(NVRAMInfo.Companion.getWOL_HOSTNAME()), EDITABLE);

    ((EditText) findViewById(R.id.wol_daemon_settings_secure_on_password))
        .setText(mNvramInfo.getProperty(NVRAMInfo.Companion.getWOL_PASSWD()), EDITABLE);

    //noinspection ConstantConditions
    ((EditText) findViewById(R.id.wol_daemon_settings_mac_addresses))
        .setText(
            Joiner.on("\n")
                .skipNulls()
                .join(
                    Splitter.on(" ")
                        .omitEmptyStrings()
                        .split(
                            mNvramInfo.getProperty(
                                NVRAMInfo.Companion.getWOL_MACS(),
                                RouterCompanionAppConstants.EMPTY_STRING))),
            EDITABLE);
  }
}
