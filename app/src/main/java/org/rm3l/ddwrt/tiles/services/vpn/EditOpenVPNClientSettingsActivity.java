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
package org.rm3l.ddwrt.tiles.services.vpn;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static android.widget.TextView.BufferType.EDITABLE;
import static org.rm3l.ddwrt.mgmt.RouterManagementActivity.ROUTER_SELECTED;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_ADV;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_AUTH;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_BRIDGE;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_CA;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_CERTTYPE;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_CIPHER;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_CLIENT;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_CONFIG;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_ENABLE;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_FRAGMENT;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_IP;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_KEY;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_LZO;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_MASK;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_MSSFIX;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_MTU;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_NAT;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_PKCS_12;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_PROTO;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_REMOTEIP;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_REMOTEPORT;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_ROUTE;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_SEC;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_STATIC;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_TLSAUTH;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_TLSCIP;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_TUNTAP;
import static org.rm3l.ddwrt.tiles.services.vpn.OpenVPNClientTile.OPENVPNCL_NVRAMINFO;

public class EditOpenVPNClientSettingsActivity extends SherlockFragmentActivity {

    public static final String VARIABLES_CHANGED = "VARIABLES_CHANGED";
    private NVRAMInfo mNvramInfo;

    private String mRouterUuid;

    private static final Map<String, Integer> tunnelDeviceSpinnerValues = Maps.newHashMapWithExpectedSize(2);
    private static final Map<String, Integer> tunnelProtoSpinnerValues = Maps.newHashMapWithExpectedSize(2);
    private static final Map<String, Integer> encryptionCipherSpinnerValues = Maps.newHashMapWithExpectedSize(6);
    private static final Map<String, Integer> hashAlgoSpinnerValues = Maps.newHashMapWithExpectedSize(6);
    private static final Map<String, Integer> tlsCipherSpinnerValues = Maps.newHashMapWithExpectedSize(8);
    private static final Map<String, Integer> lzoCompressionSpinnerValues = Maps.newHashMapWithExpectedSize(4);

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_openvpn_client_settings);

        final Intent intent = getIntent();
        mNvramInfo = (NVRAMInfo) intent.getSerializableExtra(OPENVPNCL_NVRAMINFO);
        mRouterUuid = intent.getStringExtra(ROUTER_SELECTED);

        if (mNvramInfo == null) {
            Toast.makeText(this, "Could not load OpenVPN Client settings", Toast.LENGTH_SHORT).show();
            finish();
        }


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

    }

    @Override
    protected void onStart() {
        super.onStart();
        //Fill form with data loaded
        ((CheckBox) findViewById(R.id.openvpn_client_settings_status_flag))
                .setChecked("1".equals(mNvramInfo.getProperty(OPENVPNCL_ENABLE)));

        ((CheckBox) findViewById(R.id.openvpn_client_settings_advanced_options_flag))
                .setChecked("1".equals(mNvramInfo.getProperty(OPENVPNCL_ADV)));

        ((EditText) findViewById(R.id.openvpn_client_settings_server_ip_name))
                .setText(mNvramInfo.getProperty(OPENVPNCL_REMOTEIP), EDITABLE);

        ((EditText) findViewById(R.id.openvpn_client_settings_port))
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
        position =encryptionCipher != null ? encryptionCipherSpinnerValues.get(encryptionCipher.toLowerCase()) : null;
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
        position = tlsCipher != null ? tlsCipherSpinnerValues.get(tlsCipher.toLowerCase()) : null;
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

        ((EditText) findViewById(R.id.openvpn_client_settings_ip_address))
                .setText(mNvramInfo.getProperty(OPENVPNCL_IP), EDITABLE);

        ((EditText) findViewById(R.id.openvpn_client_settings_subnet_mask))
                .setText(mNvramInfo.getProperty(OPENVPNCL_MASK), EDITABLE);

        ((EditText) findViewById(R.id.openvpn_client_settings_tunnel_mtu_setting))
                .setText(mNvramInfo.getProperty(OPENVPNCL_MTU, "1400"), EDITABLE);

        ((EditText) findViewById(R.id.openvpn_client_settings_tunnel_udp_fragment))
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

        //TODO
    }



    @Override
    public void finish() {
        final Intent data = new Intent();

        //Resulting intent: NVRAM Info edited with user info
        data.putExtra(ROUTER_SELECTED, mRouterUuid);

        final NVRAMInfo nvramVarsToUpdate = new NVRAMInfo();
        data.putExtra(OPENVPNCL_NVRAMINFO, nvramVarsToUpdate);

        //TODO Set variables that have changed and which need commit in NVRAM
        final ArrayList<String> variablesChanged = new ArrayList<>();
        data.putStringArrayListExtra(VARIABLES_CHANGED, variablesChanged);

        if (!variablesChanged.isEmpty()) {
            //Something changed - prompt confirmation dialog
            new AlertDialog.Builder(this)
                    .setIcon(R.drawable.ic_action_alert_warning)
                    .setMessage("Some settings have been updated. Do you want to save them or not?\n" +
                            "If you choose to save them, they will be applied right away.")
                    .setCancelable(true)
                    .setPositiveButton("Proceed!", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialogInterface, final int i) {
                            setResult(RESULT_OK, data);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //Cancelled - nothing more to do!
                            setResult(RESULT_CANCELED, data);
                        }
                    }).create().show();
        }

        super.finish();
    }

}
