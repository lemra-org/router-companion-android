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
package org.rm3l.router_companion.tiles.services.vpn.client;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.Utils;

import java.util.HashSet;
import java.util.Set;

import static android.widget.TextView.BufferType.EDITABLE;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.router_companion.mgmt.RouterManagementActivity.ROUTER_SELECTED;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPNCL_ADV;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPNCL_AUTH;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPNCL_BRIDGE;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPNCL_CA;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPNCL_CERTTYPE;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPNCL_CIPHER;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPNCL_CLIENT;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPNCL_CONFIG;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPNCL_ENABLE;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPNCL_FRAGMENT;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPNCL_IP;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPNCL_KEY;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPNCL_LZO;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPNCL_MASK;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPNCL_MSSFIX;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPNCL_MTU;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPNCL_NAT;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPNCL_PKCS_12;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPNCL_PROTO;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPNCL_REMOTEIP;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPNCL_REMOTEPORT;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPNCL_ROUTE;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPNCL_SEC;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPNCL_STATIC;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPNCL_TLSAUTH;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPNCL_TLSCIP;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPNCL_TUNTAP;
import static org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY;

public class EditOpenVPNClientSettingsActivity extends AppCompatActivity {

    public static final String EDIT_OPEN_VPN_CLIENT_SETTINGS_UDP_FRAGMENTS_AUTOCOMPLETE_PREF = "EditOpenVPNClientSettingsUDPFragments";
    public static final String EDIT_OPEN_VPN_CLIENT_SETTINGS_MTU_SETTINGS_AUTOCOMPLETE_PREF = "EditOpenVPNClientSettingsMTUSettings";
    public static final String EDIT_OPEN_VPN_CLIENT_SETTINGS_LOCAL_SUBNETS_AUTOCOMPLETE_PREF = "EditOpenVPNClientSettingsLocalSubnets";
    public static final String EDIT_OPEN_VPN_CLIENT_SETTINGS_LOCAL_IPS_AUTOCOMPLETE_PREF = "EditOpenVPNClientSettingsLocalIPs";
    public static final String EDIT_OPEN_VPN_CLIENT_SETTINGS_SERVER_PORTS_AUTOCOMPLETE_PREF = "EditOpenVPNClientSettingsServerPorts";
    public static final String EDIT_OPEN_VPN_CLIENT_SETTINGS_TARGET_SERVERS_AUTOCOMPLETE_PREF = "EditOpenVPNClientSettingsTargetServers";
    private static final String TAG = EditOpenVPNClientSettingsActivity.class.getSimpleName();
    private static final BiMap<String, Integer> tunnelDeviceSpinnerValues = HashBiMap.create(2);
    private static final BiMap<String, Integer> tunnelProtoSpinnerValues = HashBiMap.create(2);
    private static final BiMap<String, Integer> encryptionCipherSpinnerValues = HashBiMap.create(6);
    private static final BiMap<String, Integer> hashAlgoSpinnerValues = HashBiMap.create(6);
    private static final BiMap<String, Integer> tlsCipherSpinnerValues = HashBiMap.create(8);
    private static final BiMap<String, Integer> lzoCompressionSpinnerValues = HashBiMap.create(4);

    static {
        tunnelDeviceSpinnerValues.put("tun", 0);
        tunnelDeviceSpinnerValues.put("tap", 1);

        tunnelProtoSpinnerValues.put("udp", 0);
        tunnelProtoSpinnerValues.put("tcp-client", 1);

        encryptionCipherSpinnerValues.put("aes-512-cbc", 0);
        encryptionCipherSpinnerValues.put("aes-256-cbc", 1);
        encryptionCipherSpinnerValues.put("aes-192-cbc", 2);
        encryptionCipherSpinnerValues.put("aes-128-cbc", 3);
        encryptionCipherSpinnerValues.put("bf-cbc", 4);
        encryptionCipherSpinnerValues.put("none", 5);

        hashAlgoSpinnerValues.put("sha512", 0);
        hashAlgoSpinnerValues.put("sha256", 1);
        hashAlgoSpinnerValues.put("sha1", 2);
        hashAlgoSpinnerValues.put("md5", 3);
        hashAlgoSpinnerValues.put("md4", 4);
        hashAlgoSpinnerValues.put("none", 5);

        tlsCipherSpinnerValues.put("TLS-DHE-RSA-WITH-AES-256-GCM-SHA384", 0);
        tlsCipherSpinnerValues.put("TLS-DHE-RSA-WITH-AES-256-CBC-SHA256", 1);
        tlsCipherSpinnerValues.put("TLS-DHE-RSA-WITH-AES-128-CBC-SHA", 2);
        tlsCipherSpinnerValues.put("TLS-RSA-WITH-AES-256-GCM-SHA384", 3);
        tlsCipherSpinnerValues.put("TLS-RSA-WITH-AES-256-CBC-SHA256", 4);
        tlsCipherSpinnerValues.put("TLS-RSA-WITH-AES-128-CBC-SHA", 5);
        tlsCipherSpinnerValues.put("TLS-RSA-WITH-RC4-128-MD5", 6);
        tlsCipherSpinnerValues.put("0", 7);

        lzoCompressionSpinnerValues.put("yes", 0);
        lzoCompressionSpinnerValues.put("adaptive", 1);
        lzoCompressionSpinnerValues.put("no", 2);
        lzoCompressionSpinnerValues.put("off", 3);
    }

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
//            getWindow().getDecorView()
//                    .setBackgroundColor(ContextCompat.getColor(this,
//                            android.R.color.white));
        } else {
            //Default is Dark
            setTheme(R.style.AppThemeDark);
        }

        setContentView(R.layout.activity_openvpn_client_settings);

        if (themeLight) {
            final Resources resources = getResources();
//            getWindow().getDecorView()
//                    .setBackgroundColor(
//                            ContextCompat.getColor(this, android.R.color.white));
        }

        final Intent intent = getIntent();

        mRouterUuid = intent.getStringExtra(ROUTER_SELECTED);
        if (isNullOrEmpty(mRouterUuid)) {
            Toast.makeText(this, "Internal Error: Router could not be determined", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        final DDWRTCompanionDAO dao = RouterManagementActivity.getDao(this);
        final Router router;
        if ((router = dao.getRouter(mRouterUuid)) == null) {
            Toast.makeText(this, "Internal Error: Router could not be determined", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mToolbar = (Toolbar) findViewById(R.id.openvpn_client_settings_toolbar);
        if (mToolbar != null) {
            mToolbar.setTitle("OpenVPN Client Settings");
            mToolbar.setSubtitle(String.format("%s (%s:%d)",
                    router.getDisplayName(),
                    router.getRemoteIpAddress(),
                    router.getRemotePort()));
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

        mNvramInfo = (NVRAMInfo) intent.getSerializableExtra(OpenVPNClientTile.OPENVPNCL_NVRAMINFO);

        if (mNvramInfo == null) {
            Toast.makeText(this, "Could not load OpenVPN Client settings", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //Preferences saved globally, to be shared across different routers
        sharedPreferences = getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY,
                Context.MODE_PRIVATE);

        //Advanced Options
        ((CheckBox) findViewById(R.id.openvpn_client_settings_advanced_options_flag))
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        findViewById(R.id.openvpn_client_settings_advanced_options)
                                .setVisibility(isChecked ? View.VISIBLE : View.GONE);
                    }
                });

        //NAT
        ((CheckBox) findViewById(R.id.openvpn_client_settings_advanced_options_nat))
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            findViewById(R.id.openvpn_client_settings_advanced_options_nat_bridge_tap_to_br0)
                                    .setVisibility(View.GONE);
                            findViewById(R.id.openvpn_client_settings_advanced_options_nat_firewall)
                                    .setVisibility(View.VISIBLE);
                        } else {
                            findViewById(R.id.openvpn_client_settings_advanced_options_nat_bridge_tap_to_br0)
                                    .setVisibility(View.VISIBLE);
                            findViewById(R.id.openvpn_client_settings_advanced_options_nat_firewall)
                                    .setVisibility(View.GONE);
                        }
                    }
                });

        fillForm();

    }

    private void fillForm() {
        //Fill form with data loaded
        ((CheckBox) findViewById(R.id.openvpn_client_settings_status_flag))
                .setChecked("1".equals(mNvramInfo.getProperty(OPENVPNCL_ENABLE)));

        ((CheckBox) findViewById(R.id.openvpn_client_settings_advanced_options_flag))
                .setChecked("1".equals(mNvramInfo.getProperty(OPENVPNCL_ADV)));

        final AutoCompleteTextView serverIpOrNameAutoComplete = (AutoCompleteTextView) findViewById(R.id.openvpn_client_settings_server_ip_name);
        final Set<String> editOpenVPNClientSettingsTargetServers = sharedPreferences
                .getStringSet(EDIT_OPEN_VPN_CLIENT_SETTINGS_TARGET_SERVERS_AUTOCOMPLETE_PREF, new HashSet<String>());
        serverIpOrNameAutoComplete
                .setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                        editOpenVPNClientSettingsTargetServers.toArray(new String[editOpenVPNClientSettingsTargetServers.size()])));
        serverIpOrNameAutoComplete
                .setText(mNvramInfo.getProperty(OPENVPNCL_REMOTEIP), EDITABLE);

        final AutoCompleteTextView serverPortAutoComplete = (AutoCompleteTextView) findViewById(R.id.openvpn_client_settings_port);
        final Set<String> editOpenVPNClientSettingsServerPorts = sharedPreferences
                .getStringSet(EDIT_OPEN_VPN_CLIENT_SETTINGS_SERVER_PORTS_AUTOCOMPLETE_PREF, new HashSet<String>());
        serverPortAutoComplete
                .setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                        editOpenVPNClientSettingsServerPorts.toArray(new String[editOpenVPNClientSettingsServerPorts.size()])));
        serverPortAutoComplete
                .setText(mNvramInfo.getProperty(OPENVPNCL_REMOTEPORT), EDITABLE);

        final String tunnelDevice = mNvramInfo.getProperty(OPENVPNCL_TUNTAP);
        Integer position = tunnelDevice != null ? tunnelDeviceSpinnerValues.get(tunnelDevice.toLowerCase()) : null;
        ((Spinner) findViewById(R.id.openvpn_client_settings_tunnel_device))
                .setSelection(position != null ? position : 0);

        final String tunnelProto = mNvramInfo.getProperty(OPENVPNCL_PROTO);
        position = tunnelProto != null ? tunnelProtoSpinnerValues.get(tunnelProto.toLowerCase()) : null;
        ((Spinner) findViewById(R.id.openvpn_client_settings_tunnel_protocol))
                .setSelection(position != null ? position : 0);

        final String encryptionCipher = mNvramInfo.getProperty(OPENVPNCL_CIPHER);
        position = encryptionCipher != null ? encryptionCipherSpinnerValues.get(encryptionCipher.toLowerCase()) : null;
        ((Spinner) findViewById(R.id.openvpn_client_settings_encryption_cipher))
                .setSelection(position != null ? position : 5);

        final String hashAlgorithm = mNvramInfo.getProperty(OPENVPNCL_AUTH);
        position = hashAlgorithm != null ? hashAlgoSpinnerValues.get(hashAlgorithm.toLowerCase()) : null;
        ((Spinner) findViewById(R.id.openvpn_client_settings_hash_algorithm))
                .setSelection(position != null ? position : 5);

        ((EditText) findViewById(R.id.openvpn_client_settings_ca_cert))
                .setText(mNvramInfo.getProperty(OPENVPNCL_CA), EDITABLE);

        ((EditText) findViewById(R.id.openvpn_client_settings_public_client_cert))
                .setText(mNvramInfo.getProperty(OPENVPNCL_CLIENT), EDITABLE);

        ((EditText) findViewById(R.id.openvpn_client_settings_private_client_key))
                .setText(mNvramInfo.getProperty(OPENVPNCL_KEY), EDITABLE);

        final String tlsCipher = mNvramInfo.getProperty(OPENVPNCL_TLSCIP);
        position = tlsCipher != null ? tlsCipherSpinnerValues.get(tlsCipher) : null;
        ((Spinner) findViewById(R.id.openvpn_client_settings_tls_cipher))
                .setSelection(position != null ? position : 7);

        final String lzoCompression = mNvramInfo.getProperty(OPENVPNCL_LZO);
        position = lzoCompression != null ? lzoCompressionSpinnerValues.get(lzoCompression.toLowerCase()) : null;
        ((Spinner) findViewById(R.id.openvpn_client_settings_lzo_compression))
                .setSelection(position != null ? position : 3);

        ((CheckBox) findViewById(R.id.openvpn_client_settings_advanced_options_nat))
                .setChecked("1".equals(mNvramInfo.getProperty(OPENVPNCL_NAT)));

        ((CheckBox) findViewById(R.id.openvpn_client_settings_advanced_options_nat_firewall))
                .setChecked("1".equals(mNvramInfo.getProperty(OPENVPNCL_SEC)));

        ((CheckBox) findViewById(R.id.openvpn_client_settings_advanced_options_nat_bridge_tap_to_br0))
                .setChecked("1".equals(mNvramInfo.getProperty(OPENVPNCL_BRIDGE)));

        final AutoCompleteTextView localIps = (AutoCompleteTextView) findViewById(R.id.openvpn_client_settings_ip_address);
        final Set<String> editOpenVPNClientSettingsLocalIPs = sharedPreferences
                .getStringSet(EDIT_OPEN_VPN_CLIENT_SETTINGS_LOCAL_IPS_AUTOCOMPLETE_PREF, new HashSet<String>());
        localIps
                .setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                        editOpenVPNClientSettingsLocalIPs.toArray(new String[editOpenVPNClientSettingsLocalIPs.size()])));
        localIps
                .setText(mNvramInfo.getProperty(OPENVPNCL_IP), EDITABLE);

        final AutoCompleteTextView localSubnetMask = (AutoCompleteTextView) findViewById(R.id.openvpn_client_settings_subnet_mask);
        final Set<String> editOpenVPNClientSettingsLocalSubnets = sharedPreferences
                .getStringSet(EDIT_OPEN_VPN_CLIENT_SETTINGS_LOCAL_SUBNETS_AUTOCOMPLETE_PREF, new HashSet<String>());
        localSubnetMask
                .setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                        editOpenVPNClientSettingsLocalSubnets.toArray(new String[editOpenVPNClientSettingsLocalSubnets.size()])));
        localSubnetMask
                .setText(mNvramInfo.getProperty(OPENVPNCL_MASK), EDITABLE);

        final AutoCompleteTextView mtuSettingAutoCompleteView = (AutoCompleteTextView) findViewById(R.id.openvpn_client_settings_tunnel_mtu_setting);
        final Set<String> editOpenVPNClientSettingsMTUSettings = sharedPreferences
                .getStringSet(EDIT_OPEN_VPN_CLIENT_SETTINGS_MTU_SETTINGS_AUTOCOMPLETE_PREF, new HashSet<String>());
        mtuSettingAutoCompleteView
                .setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                        editOpenVPNClientSettingsMTUSettings.toArray(new String[editOpenVPNClientSettingsMTUSettings.size()])));
        mtuSettingAutoCompleteView
                .setText(mNvramInfo.getProperty(OPENVPNCL_MTU, "1400"), EDITABLE);

        final AutoCompleteTextView udpFragmentAutoCompleteView = (AutoCompleteTextView) findViewById(R.id.openvpn_client_settings_tunnel_udp_fragment);
        final Set<String> editOpenVPNClientSettingsUDPFragment = sharedPreferences
                .getStringSet(EDIT_OPEN_VPN_CLIENT_SETTINGS_UDP_FRAGMENTS_AUTOCOMPLETE_PREF, new HashSet<String>());
        udpFragmentAutoCompleteView
                .setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                        editOpenVPNClientSettingsUDPFragment.toArray(new String[editOpenVPNClientSettingsUDPFragment.size()])));
        udpFragmentAutoCompleteView
                .setText(mNvramInfo.getProperty(OPENVPNCL_FRAGMENT), EDITABLE);

        ((CheckBox) findViewById(R.id.openvpn_client_settings_advanced_options_tunnel_udp_mss_fix))
                .setChecked("1".equals(mNvramInfo.getProperty(OPENVPNCL_MSSFIX)));

        ((CheckBox) findViewById(R.id.openvpn_client_settings_advanced_options_nscerttype))
                .setChecked("1".equals(mNvramInfo.getProperty(OPENVPNCL_CERTTYPE)));

        ((EditText) findViewById(R.id.openvpn_client_settings_advanced_options_additional_config))
                .setText(mNvramInfo.getProperty(OPENVPNCL_CONFIG), EDITABLE);

        ((EditText) findViewById(R.id.openvpn_client_settings_advanced_options_tls_auth_key))
                .setText(mNvramInfo.getProperty(OPENVPNCL_TLSAUTH), EDITABLE);

        ((EditText) findViewById(R.id.openvpn_client_settings_advanced_options_policy_based_routing))
                .setText(mNvramInfo.getProperty(OPENVPNCL_ROUTE), EDITABLE);

        ((EditText) findViewById(R.id.openvpn_client_settings_advanced_options_pkcs12_key))
                .setText(mNvramInfo.getProperty(OPENVPNCL_PKCS_12), EDITABLE);

        ((EditText) findViewById(R.id.openvpn_client_settings_advanced_options_static_key))
                .setText(mNvramInfo.getProperty(OPENVPNCL_STATIC), EDITABLE);
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

        final String isOpenVPNClientOn = ((CheckBox) findViewById(R.id.openvpn_client_settings_status_flag))
                .isChecked() ? "1" : "0";
        if (!isOpenVPNClientOn.equals(mNvramInfo.getProperty(OPENVPNCL_ENABLE))) {
            nvramVarsToUpdate.setProperty(OPENVPNCL_ENABLE, isOpenVPNClientOn);
        }

        final String serverIpName = ((EditText) findViewById(R.id.openvpn_client_settings_server_ip_name)).getText().toString();
        if (!serverIpName.equals(mNvramInfo.getProperty(OPENVPNCL_REMOTEIP))) {
            nvramVarsToUpdate.setProperty(OPENVPNCL_REMOTEIP, serverIpName);
            if (!isNullOrEmpty(serverIpName)) {
                final Set<String> mSharedPreferencesStringSet = new HashSet<>(sharedPreferences.getStringSet(EDIT_OPEN_VPN_CLIENT_SETTINGS_TARGET_SERVERS_AUTOCOMPLETE_PREF,
                        new HashSet<String>()));
                if (!mSharedPreferencesStringSet.contains(serverIpName)) {
                    mSharedPreferencesStringSet.add(serverIpName);
                    editor
                            .putStringSet(EDIT_OPEN_VPN_CLIENT_SETTINGS_TARGET_SERVERS_AUTOCOMPLETE_PREF, mSharedPreferencesStringSet);
                    applyNewPrefs = true;
                }
            }
        }

        final String serverPort = ((EditText) findViewById(R.id.openvpn_client_settings_port)).getText().toString();
        if (!serverPort.equals(mNvramInfo.getProperty(OPENVPNCL_REMOTEPORT))) {
            nvramVarsToUpdate.setProperty(OPENVPNCL_REMOTEPORT, serverPort);
            if (!isNullOrEmpty(serverPort)) {
                final Set<String> mSharedPreferencesStringSet = new HashSet<>(sharedPreferences.getStringSet(EDIT_OPEN_VPN_CLIENT_SETTINGS_SERVER_PORTS_AUTOCOMPLETE_PREF,
                        new HashSet<String>()));
                if (!mSharedPreferencesStringSet.contains(serverPort)) {
                    mSharedPreferencesStringSet.add(serverPort);
                    editor
                            .putStringSet(EDIT_OPEN_VPN_CLIENT_SETTINGS_SERVER_PORTS_AUTOCOMPLETE_PREF, mSharedPreferencesStringSet);
                    applyNewPrefs = true;
                }
            }
        }

        final String tunnelDeviceSelectedItem = tunnelDeviceSpinnerValues.inverse().get(
                ((Spinner) findViewById(R.id.openvpn_client_settings_tunnel_device)).getSelectedItemPosition());
        if (tunnelDeviceSelectedItem != null && !tunnelDeviceSelectedItem.equals(mNvramInfo.getProperty(OPENVPNCL_TUNTAP))) {
            nvramVarsToUpdate.setProperty(OPENVPNCL_TUNTAP, tunnelDeviceSelectedItem);
        }

        final String tunnelProtocolSelectedItem = tunnelProtoSpinnerValues.inverse().get(
                ((Spinner) findViewById(R.id.openvpn_client_settings_tunnel_protocol)).getSelectedItemPosition());
        if (tunnelProtocolSelectedItem != null && !tunnelProtocolSelectedItem.equals(mNvramInfo.getProperty(OPENVPNCL_PROTO))) {
            nvramVarsToUpdate.setProperty(OPENVPNCL_PROTO, tunnelProtocolSelectedItem);
        }

        final String encryptionCipherSelectedItem = encryptionCipherSpinnerValues.inverse().get(
                ((Spinner) findViewById(R.id.openvpn_client_settings_encryption_cipher)).getSelectedItemPosition());
        if (encryptionCipherSelectedItem != null && !encryptionCipherSelectedItem.equals(mNvramInfo.getProperty(OPENVPNCL_CIPHER))) {
            nvramVarsToUpdate.setProperty(OPENVPNCL_CIPHER, encryptionCipherSelectedItem);
        }

        final String hashAlgorithmSelectedItem = hashAlgoSpinnerValues.inverse().get(
                ((Spinner) findViewById(R.id.openvpn_client_settings_hash_algorithm)).getSelectedItemPosition());
        if (hashAlgorithmSelectedItem != null && !hashAlgorithmSelectedItem.equals(mNvramInfo.getProperty(OPENVPNCL_AUTH))) {
            nvramVarsToUpdate.setProperty(OPENVPNCL_AUTH, hashAlgorithmSelectedItem);
        }

        final String caCert = ((EditText) findViewById(R.id.openvpn_client_settings_ca_cert)).getText().toString();
        if (!caCert.equals(mNvramInfo.getProperty(OPENVPNCL_CA))) {
            nvramVarsToUpdate.setProperty(OPENVPNCL_CA, caCert);
        }

        final String publicClientCert = ((EditText) findViewById(R.id.openvpn_client_settings_public_client_cert)).getText().toString();
        if (!publicClientCert.equals(mNvramInfo.getProperty(OPENVPNCL_CLIENT))) {
            nvramVarsToUpdate.setProperty(OPENVPNCL_CLIENT, publicClientCert);
        }

        final String privateClientKey = \"fake-key\";
        if (!privateClientKey.equals(mNvramInfo.getProperty(OPENVPNCL_KEY))) {
            nvramVarsToUpdate.setProperty(OPENVPNCL_KEY, privateClientKey);
        }

        final String isAdvancedOptionsOn = ((CheckBox) findViewById(R.id.openvpn_client_settings_advanced_options_flag))
                .isChecked() ? "1" : "0";
        if (!isAdvancedOptionsOn.equals(mNvramInfo.getProperty(OPENVPNCL_ADV))) {
            nvramVarsToUpdate.setProperty(OPENVPNCL_ADV, isAdvancedOptionsOn);
        }

        final String tlsCipherSelectedItem = tlsCipherSpinnerValues.inverse().get(
                ((Spinner) findViewById(R.id.openvpn_client_settings_tls_cipher)).getSelectedItemPosition());
        if (tlsCipherSelectedItem != null && !tlsCipherSelectedItem.equals(mNvramInfo.getProperty(OPENVPNCL_TLSCIP))) {
            nvramVarsToUpdate.setProperty(OPENVPNCL_TLSCIP, tlsCipherSelectedItem);
        }

        final String lzoCompressionSelectedItem = lzoCompressionSpinnerValues.inverse().get(
                ((Spinner) findViewById(R.id.openvpn_client_settings_lzo_compression)).getSelectedItemPosition());
        if (lzoCompressionSelectedItem != null && !lzoCompressionSelectedItem.equals(mNvramInfo.getProperty(OPENVPNCL_LZO))) {
            nvramVarsToUpdate.setProperty(OPENVPNCL_LZO, lzoCompressionSelectedItem);
        }

        final String isNatOn = ((CheckBox) findViewById(R.id.openvpn_client_settings_advanced_options_nat))
                .isChecked() ? "1" : "0";
        if (!isNatOn.equals(mNvramInfo.getProperty(OPENVPNCL_NAT))) {
            nvramVarsToUpdate.setProperty(OPENVPNCL_NAT, isNatOn);
        }

        final String isFWSecOn = ((CheckBox) findViewById(R.id.openvpn_client_settings_advanced_options_nat_firewall))
                .isChecked() ? "1" : "0";
        if (!isFWSecOn.equals(mNvramInfo.getProperty(OPENVPNCL_SEC))) {
            nvramVarsToUpdate.setProperty(OPENVPNCL_SEC, isFWSecOn);
        }

        final String isBridgeOn = ((CheckBox) findViewById(R.id.openvpn_client_settings_advanced_options_nat_bridge_tap_to_br0))
                .isChecked() ? "1" : "0";
        if (!isBridgeOn.equals(mNvramInfo.getProperty(OPENVPNCL_BRIDGE))) {
            nvramVarsToUpdate.setProperty(OPENVPNCL_BRIDGE, isBridgeOn);
        }

        final String ipAddr = ((EditText) findViewById(R.id.openvpn_client_settings_ip_address)).getText().toString();
        if (!ipAddr.equals(mNvramInfo.getProperty(OPENVPNCL_IP))) {
            nvramVarsToUpdate.setProperty(OPENVPNCL_IP, ipAddr);
            if (!isNullOrEmpty(ipAddr)) {
                final Set<String> mSharedPreferencesStringSet = new HashSet<>(sharedPreferences.getStringSet(EDIT_OPEN_VPN_CLIENT_SETTINGS_LOCAL_IPS_AUTOCOMPLETE_PREF,
                        new HashSet<String>()));
                if (!mSharedPreferencesStringSet.contains(ipAddr)) {
                    mSharedPreferencesStringSet.add(ipAddr);
                    editor
                            .putStringSet(EDIT_OPEN_VPN_CLIENT_SETTINGS_LOCAL_IPS_AUTOCOMPLETE_PREF, mSharedPreferencesStringSet);
                    applyNewPrefs = true;
                }
            }
        }

        final String subnetMask = ((EditText) findViewById(R.id.openvpn_client_settings_subnet_mask)).getText().toString();
        if (!subnetMask.equals(mNvramInfo.getProperty(OPENVPNCL_MASK))) {
            nvramVarsToUpdate.setProperty(OPENVPNCL_MASK, subnetMask);
            if (!isNullOrEmpty(subnetMask)) {
                final Set<String> mSharedPreferencesStringSet = new HashSet<>(sharedPreferences.getStringSet(EDIT_OPEN_VPN_CLIENT_SETTINGS_LOCAL_SUBNETS_AUTOCOMPLETE_PREF,
                        new HashSet<String>()));
                if (!mSharedPreferencesStringSet.contains(subnetMask)) {
                    mSharedPreferencesStringSet.add(subnetMask);
                    editor
                            .putStringSet(EDIT_OPEN_VPN_CLIENT_SETTINGS_LOCAL_SUBNETS_AUTOCOMPLETE_PREF, mSharedPreferencesStringSet);
                    applyNewPrefs = true;
                }
            }
        }

        final String tunnelMtu = ((EditText) findViewById(R.id.openvpn_client_settings_tunnel_mtu_setting)).getText().toString();
        if (!tunnelMtu.equals(mNvramInfo.getProperty(OPENVPNCL_MTU))) {
            nvramVarsToUpdate.setProperty(OPENVPNCL_MTU, tunnelMtu);
            if (!isNullOrEmpty(tunnelMtu)) {
                final Set<String> mSharedPreferencesStringSet = new HashSet<>(sharedPreferences.getStringSet(EDIT_OPEN_VPN_CLIENT_SETTINGS_MTU_SETTINGS_AUTOCOMPLETE_PREF,
                        new HashSet<String>()));
                if (!mSharedPreferencesStringSet.contains(tunnelMtu)) {
                    mSharedPreferencesStringSet.add(tunnelMtu);
                    editor
                            .putStringSet(EDIT_OPEN_VPN_CLIENT_SETTINGS_MTU_SETTINGS_AUTOCOMPLETE_PREF, mSharedPreferencesStringSet);
                    applyNewPrefs = true;
                }
            }
        }

        final String tunnelUdpFragment = ((EditText) findViewById(R.id.openvpn_client_settings_tunnel_udp_fragment)).getText().toString();
        if (!tunnelUdpFragment.equals(mNvramInfo.getProperty(OPENVPNCL_FRAGMENT))) {
            nvramVarsToUpdate.setProperty(OPENVPNCL_FRAGMENT, tunnelUdpFragment);
            if (!isNullOrEmpty(tunnelUdpFragment)) {
                final Set<String> mSharedPreferencesStringSet = new HashSet<>(sharedPreferences.getStringSet(EDIT_OPEN_VPN_CLIENT_SETTINGS_LOCAL_SUBNETS_AUTOCOMPLETE_PREF,
                        new HashSet<String>()));
                if (!mSharedPreferencesStringSet.contains(tunnelUdpFragment)) {
                    mSharedPreferencesStringSet.add(tunnelUdpFragment);
                    editor
                            .putStringSet(EDIT_OPEN_VPN_CLIENT_SETTINGS_LOCAL_SUBNETS_AUTOCOMPLETE_PREF, mSharedPreferencesStringSet);
                    applyNewPrefs = true;
                }
            }
        }

        if (applyNewPrefs) {
            editor.apply();
        }

        final String isTunnelUdpMssFixOn = ((CheckBox) findViewById(R.id.openvpn_client_settings_advanced_options_tunnel_udp_mss_fix))
                .isChecked() ? "1" : "0";
        if (!isTunnelUdpMssFixOn.equals(mNvramInfo.getProperty(OPENVPNCL_MSSFIX))) {
            nvramVarsToUpdate.setProperty(OPENVPNCL_MSSFIX, isTunnelUdpMssFixOn);
        }

        final String isNsCerttypeOn = ((CheckBox) findViewById(R.id.openvpn_client_settings_advanced_options_nscerttype))
                .isChecked() ? "1" : "0";
        if (!isNsCerttypeOn.equals(mNvramInfo.getProperty(OPENVPNCL_CERTTYPE))) {
            nvramVarsToUpdate.setProperty(OPENVPNCL_CERTTYPE, isNsCerttypeOn);
        }

        final String additionalConfig = ((EditText) findViewById(R.id.openvpn_client_settings_advanced_options_additional_config)).getText().toString();
        if (!additionalConfig.equals(mNvramInfo.getProperty(OPENVPNCL_CONFIG))) {
            nvramVarsToUpdate.setProperty(OPENVPNCL_CONFIG, additionalConfig);
        }

        final String tlsAuthKey = \"fake-key\";
        if (!tlsAuthKey.equals(mNvramInfo.getProperty(OPENVPNCL_TLSAUTH))) {
            nvramVarsToUpdate.setProperty(OPENVPNCL_TLSAUTH, tlsAuthKey);
        }

        final String policyBasedRouting = ((EditText) findViewById(R.id.openvpn_client_settings_advanced_options_policy_based_routing)).getText().toString();
        if (!policyBasedRouting.equals(mNvramInfo.getProperty(OPENVPNCL_ROUTE))) {
            nvramVarsToUpdate.setProperty(OPENVPNCL_ROUTE, policyBasedRouting);
        }

        final String pkcs12Key = \"fake-key\";
        if (!pkcs12Key.equals(mNvramInfo.getProperty(OPENVPNCL_PKCS_12))) {
            nvramVarsToUpdate.setProperty(OPENVPNCL_PKCS_12, pkcs12Key);
        }

        final String staticKey = \"fake-key\";
        if (!staticKey.equals(mNvramInfo.getProperty(OPENVPNCL_STATIC))) {
            nvramVarsToUpdate.setProperty(OPENVPNCL_STATIC, staticKey);
        }

        Crashlytics.log(Log.DEBUG, TAG, "vars that have changed: " + nvramVarsToUpdate);

        //Set extra
        data.putExtra(OpenVPNClientTile.OPENVPNCL_NVRAMINFO, nvramVarsToUpdate);

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
                            EditOpenVPNClientSettingsActivity.super.finish();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //Cancelled - nothing more to do!
                            setResult(RESULT_CANCELED, data);
                            EditOpenVPNClientSettingsActivity.super.finish();
                        }
                    }).create().show();
        } else {
            setResult(RESULT_CANCELED, data);
            super.finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_edit_openvpn_client_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.action_feedback:
                Utils.openFeedbackForm(this, mRouterUuid);
//                final Intent intent = new Intent(EditOpenVPNClientSettingsActivity.this, FeedbackActivity.class);
//                intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouterUuid);
//                final File screenshotFile = new File(getCacheDir(), "feedback_screenshot.png");
//                ViewGroupUtils.exportViewToFile(EditOpenVPNClientSettingsActivity.this, getWindow().getDecorView(), screenshotFile);
//                intent.putExtra(FeedbackActivity.SCREENSHOT_FILE, screenshotFile.getAbsolutePath());
//                intent.putExtra(FeedbackActivity.CALLER_ACTIVITY, this.getClass().getCanonicalName());
//                startActivity(intent);
////                Utils.buildFeedbackDialog(this, true);
                return true;

            default:
                break;

        }

        return super.onOptionsItemSelected(item);
    }
}
