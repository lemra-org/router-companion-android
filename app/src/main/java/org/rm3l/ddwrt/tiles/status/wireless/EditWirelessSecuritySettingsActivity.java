package org.rm3l.ddwrt.tiles.status.wireless;

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
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.Utils;

import de.keyboardsurfer.android.widget.crouton.Style;

import static android.widget.TextView.BufferType.EDITABLE;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.ddwrt.mgmt.RouterManagementActivity.ROUTER_SELECTED;
import static org.rm3l.ddwrt.tiles.status.wireless.WirelessIfaceTile.WL_SECURITY_NVRAMINFO;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;

/**
 * Created by rm3l on 29/09/15.
 */
public class EditWirelessSecuritySettingsActivity extends ActionBarActivity {
    private static final String LOG_TAG = EditWirelessSecuritySettingsActivity.class.getSimpleName();

    public static final String WIRELESS_SECURITY_NVRAMINFO = "WIRELESS_SECURITY_NVRAMINFO";
    public static final String HWADDR = "HWADDR";

    private Toolbar mToolbar;
    private NVRAMInfo mNvramInfo;
    private String mRouterUuid;
    private SharedPreferences sharedPreferences;
    private String mPhyIface;
    private String mSsid;
    private String mHwAddr;
    private int[] mDetailedViews;

    private static final BiMap<String, Integer> securityModeValues = HashBiMap.create(9);
    private static final BiMap<String, Integer> wpaAlgoValues = HashBiMap.create(3);
    private static final BiMap<String, Integer> radiusMacFormatValues = HashBiMap.create(4);
    private static final BiMap<String, Integer> wepEncryptionValues = HashBiMap.create(2);

    static {
        securityModeValues.put("disabled", 0);
        securityModeValues.put("psk", 1);
        securityModeValues.put("wpa", 2);
        securityModeValues.put("psk2", 3);
        securityModeValues.put("wpa2", 4);
        securityModeValues.put("psk psk2", 5);
        securityModeValues.put("wpa wpa2", 6);
        securityModeValues.put("radius", 7);
        securityModeValues.put("wep", 8);

        wpaAlgoValues.put("tkip", 0);
        wpaAlgoValues.put("aes", 1);
        wpaAlgoValues.put("tkip+aes", 2);

        radiusMacFormatValues.put("aabbcc-ddeeff", 0);
        radiusMacFormatValues.put("aabbccddeeff", 1);
        radiusMacFormatValues.put("aa:bb:cc:dd:ee:ff", 2);
        radiusMacFormatValues.put("aa-bb-cc-dd-ee-ff", 3);

        wepEncryptionValues.put("64", 0);
        wepEncryptionValues.put("128", 1);


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

        setContentView(R.layout.activity_wireless_security_settings);

        if (themeLight) {
            final Resources resources = getResources();
            getWindow().getDecorView()
                    .setBackgroundColor(resources.getColor(android.R.color.white));
        }

        final Intent intent = getIntent();
        mNvramInfo = (NVRAMInfo) intent.getSerializableExtra(WIRELESS_SECURITY_NVRAMINFO);

        if (mNvramInfo == null || mNvramInfo.getData() == null) {
            Toast.makeText(this, "Could not load Wireless Security settings", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (!mNvramInfo.getData().containsKey(WirelessIfaceTile.IFACE)) {
            Toast.makeText(this, "Could not load physical iface", Toast.LENGTH_SHORT).show();
            finish();
        }

        mRouterUuid = intent.getStringExtra(ROUTER_SELECTED);
        if (isNullOrEmpty(mRouterUuid)) {
            Toast.makeText(this, "Internal Error: Router could not be determined", Toast.LENGTH_SHORT).show();
            finish();
        }

        mPhyIface = mNvramInfo.getProperty(WirelessIfaceTile.IFACE,
                DDWRTCompanionConstants.EMPTY_VALUE_TO_DISPLAY);

        mSsid = mNvramInfo.getProperty(WirelessIfaceQrCodeActivity.SSID,
                DDWRTCompanionConstants.EMPTY_VALUE_TO_DISPLAY);

        mHwAddr = mNvramInfo.getProperty(HWADDR,
                DDWRTCompanionConstants.EMPTY_VALUE_TO_DISPLAY);

        mToolbar = (Toolbar) findViewById(R.id.wireless_security_settings_toolbar);
        if (mToolbar != null) {
            mToolbar.setTitle(String.format(
                    "Wireless Security: %s", mPhyIface));
            setSupportActionBar(mToolbar);
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        //Preferences saved globally, to be shared across different routers
        sharedPreferences = getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY,
                Context.MODE_PRIVATE);

        mDetailedViews = new int[] {
                R.id.wireless_security_settings_wpa_personal,
                R.id.wireless_security_settings_wpa_enterprise,
                R.id.wireless_security_settings_wpa2_personal,
                R.id.wireless_security_settings_wpa2_enterprise,
                R.id.wireless_security_settings_wpa2_personal_mixed,
                R.id.wireless_security_settings_wpa2_enterprise_mixed,
                R.id.wireless_security_settings_radius,
                R.id.wireless_security_settings_wep
        };

        hideAllDetailedViews();

        ((Spinner) findViewById(R.id.wireless_security_settings_security_mode))
                .setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Log.d(LOG_TAG, "onItemSelected @" + position);
                        if (position == 0) {
                            hideAllDetailedViews();
                        } else {
                            showDetailedViewAt(position - 1);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

        final EditText wpaSharedKeyEditText =
                (EditText) findViewById(R.id.wireless_security_settings_wpa_personal_wpa_shared_key);
        ((CheckBox) findViewById(R.id.wireless_security_settings_wpa_personal_wpa_key_show_checkbox))
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!isChecked) {
                            wpaSharedKeyEditText.setInputType(
                                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        } else {
                            wpaSharedKeyEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        }
                        wpaSharedKeyEditText.setSelection(wpaSharedKeyEditText.length());
                    }
                });
        final EditText wpa2SharedKeyEditText =
                (EditText) findViewById(R.id.wireless_security_settings_wpa2_personal_wpa_shared_key);
        ((CheckBox) findViewById(R.id.wireless_security_settings_wpa2_personal_wpa_key_show_checkbox))
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!isChecked) {
                            wpa2SharedKeyEditText.setInputType(
                                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        } else {
                            wpa2SharedKeyEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        }
                        wpa2SharedKeyEditText.setSelection(wpa2SharedKeyEditText.length());
                    }
                });
        final EditText wpa2PersMixedSharedKeyEditText =
                (EditText) findViewById(R.id.wireless_security_settings_wpa2_personal_mixed_wpa_shared_key);
        ((CheckBox) findViewById(R.id.wireless_security_settings_wpa2_personal_mixed_wpa_key_show_checkbox))
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!isChecked) {
                            wpa2PersMixedSharedKeyEditText.setInputType(
                                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        } else {
                            wpa2PersMixedSharedKeyEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        }
                        wpa2PersMixedSharedKeyEditText.setSelection(wpa2PersMixedSharedKeyEditText.length());
                    }
                });

        final EditText wpaEnterpriseRadiusSecretEditText = (EditText) findViewById(R.id.wireless_security_settings_wpa_enterprise_radius_secret);
        ((CheckBox) findViewById(R.id.wireless_security_settings_wpa_enterprise_radius_secret_show_checkbox))
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!isChecked) {
                            wpaEnterpriseRadiusSecretEditText.setInputType(
                                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        } else {
                            wpaEnterpriseRadiusSecretEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        }
                        wpaEnterpriseRadiusSecretEditText.setSelection(wpaEnterpriseRadiusSecretEditText.length());
                    }
                });
        final EditText wpa2EnterpriseRadiusSecretEditText = (EditText) findViewById(R.id.wireless_security_settings_wpa2_enterprise_radius_secret);
        ((CheckBox) findViewById(R.id.wireless_security_settings_wpa2_enterprise_radius_secret_show_checkbox))
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!isChecked) {
                            wpa2EnterpriseRadiusSecretEditText.setInputType(
                                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        } else {
                            wpa2EnterpriseRadiusSecretEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        }
                        wpa2EnterpriseRadiusSecretEditText.setSelection(wpa2EnterpriseRadiusSecretEditText.length());
                    }
                });
        final EditText wpa2EnterpriseMixedRadiusSecretEditText = (EditText) findViewById(R.id.wireless_security_settings_wpa2_enterprise_mixed_radius_secret);
        ((CheckBox) findViewById(R.id.wireless_security_settings_wpa2_enterprise_mixed_radius_secret_show_checkbox))
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!isChecked) {
                            wpa2EnterpriseMixedRadiusSecretEditText.setInputType(
                                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        } else {
                            wpa2EnterpriseMixedRadiusSecretEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        }
                        wpa2EnterpriseMixedRadiusSecretEditText.setSelection(wpa2EnterpriseMixedRadiusSecretEditText.length());
                    }
                });

        final EditText radiusRadiusSecretEditText = (EditText) findViewById(R.id.wireless_security_settings_radius_secret);
        ((CheckBox) findViewById(R.id.wireless_security_settings_radius_secret_show_checkbox))
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!isChecked) {
                            radiusRadiusSecretEditText.setInputType(
                                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        } else {
                            radiusRadiusSecretEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        }
                        radiusRadiusSecretEditText.setSelection(radiusRadiusSecretEditText.length());
                    }
                });

        final EditText wepPassphraseEditText = (EditText) findViewById(R.id.wireless_security_settings_wep_passphrase);
        ((CheckBox) findViewById(R.id.wireless_security_settings_wep_passphrase_show_checkbox))
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!isChecked) {
                            wepPassphraseEditText.setInputType(
                                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        } else {
                            wepPassphraseEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        }
                        wepPassphraseEditText.setSelection(wepPassphraseEditText.length());
                    }
                });

        findViewById(R.id.wireless_security_settings_wep_passphrase_generate)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });


        final EditText wepPassphraseKey1EditText = (EditText) findViewById(R.id.wireless_security_settings_wep_passphrase_key1);
        ((CheckBox) findViewById(R.id.wireless_security_settings_wep_passphrase_key1_show_checkbox))
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!isChecked) {
                            wepPassphraseKey1EditText.setInputType(
                                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        } else {
                            wepPassphraseKey1EditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        }
                        wepPassphraseKey1EditText.setSelection(wepPassphraseKey1EditText.length());
                    }
                });

        final EditText wepPassphraseKey2EditText = (EditText) findViewById(R.id.wireless_security_settings_wep_passphrase_key2);
        ((CheckBox) findViewById(R.id.wireless_security_settings_wep_passphrase_key2_show_checkbox))
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!isChecked) {
                            wepPassphraseKey2EditText.setInputType(
                                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        } else {
                            wepPassphraseKey2EditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        }
                        wepPassphraseKey2EditText.setSelection(wepPassphraseKey2EditText.length());
                    }
                });

        final EditText wepPassphraseKey3EditText = (EditText) findViewById(R.id.wireless_security_settings_wep_passphrase_key3);
        ((CheckBox) findViewById(R.id.wireless_security_settings_wep_passphrase_key3_show_checkbox))
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!isChecked) {
                            wepPassphraseKey3EditText.setInputType(
                                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        } else {
                            wepPassphraseKey3EditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        }
                        wepPassphraseKey3EditText.setSelection(wepPassphraseKey3EditText.length());
                    }
                });

        final EditText wepPassphraseKey4EditText = (EditText) findViewById(R.id.wireless_security_settings_wep_passphrase_key4);
        ((CheckBox) findViewById(R.id.wireless_security_settings_wep_passphrase_key4_show_checkbox))
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!isChecked) {
                            wepPassphraseKey4EditText.setInputType(
                                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        } else {
                            wepPassphraseKey4EditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        }
                        wepPassphraseKey4EditText.setSelection(wepPassphraseKey4EditText.length());
                    }
                });

    }

    private void showDetailedViewAt(final int position) {
        Log.d(LOG_TAG, "showDetailedViewAt @" + position);

        boolean wepVisible = false;
        for (int i = 0 ; i < mDetailedViews.length; i++) {
            final int detailedViewId = mDetailedViews[i];
            Log.d(LOG_TAG, "<i,detailedViewId>=<"+i+","+detailedViewId+">");

            final View detailedView = findViewById(detailedViewId);
            if (detailedView == null) {
                continue;
            }
            if (i == position) {
                detailedView.setVisibility(View.VISIBLE);
                if (!wepVisible) {
                    if (detailedViewId == R.id.wireless_security_settings_wep) {
                        wepVisible = true;
                    }
                }
            } else {
                detailedView.setVisibility(View.GONE);
            }
        }

        findViewById(R.id.wireless_security_settings_wep_alert)
                .setVisibility(wepVisible ? View.VISIBLE : View.GONE);

    }

    private void hideAllDetailedViews() {
        findViewById(R.id.wireless_security_settings_wep_alert)
                .setVisibility(View.GONE);
        for (final int detailedViewId : mDetailedViews) {
            final View detailedView = findViewById(detailedViewId);
            if (detailedView == null) {
                continue;
            }
            detailedView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Fill form with data loaded
        ((TextView) findViewById(R.id.wireless_security_settings_phy_iface))
                .setText(mPhyIface);
        ((TextView) findViewById(R.id.wireless_security_settings_ssid))
                .setText(mSsid);
        ((TextView) findViewById(R.id.wireless_security_settings_hw_address))
                .setText(mHwAddr);

        final String securityMode = mNvramInfo.getProperty(this.mPhyIface + "_security_mode");
        Integer position = (securityMode != null ? securityModeValues.get(securityMode) : null);
        if (!securityModeValues.containsKey(securityMode)) {
            Utils.reportException(new
                    IllegalStateException("Unknown securityMode: " + securityMode));
        }
        position = (position != null ? position : 0);
        ((Spinner) findViewById(R.id.wireless_security_settings_security_mode))
                .setSelection(position);

        final String wpaAlgo = mNvramInfo.getProperty(this.mPhyIface + "_crypto");
        position = (wpaAlgo != null ? wpaAlgoValues.get(wpaAlgo) : null);
        if (!wpaAlgoValues.containsKey(wpaAlgo)) {
            Utils.reportException(new
                    IllegalStateException("Unknown wpaAlgo: " + wpaAlgo));
        }
        position = (position != null ? position : 0);
        ((Spinner) findViewById(R.id.wireless_security_settings_wpa_personal_wpa_algorithms))
                .setSelection(position);
        ((Spinner) findViewById(R.id.wireless_security_settings_wpa_enterprise_wpa_algorithms))
                .setSelection(position);
        ((Spinner) findViewById(R.id.wireless_security_settings_wpa2_personal_wpa_algorithms))
                .setSelection(position);
        ((Spinner) findViewById(R.id.wireless_security_settings_wpa2_enterprise_wpa_algorithms))
                .setSelection(position);
        ((Spinner) findViewById(R.id.wireless_security_settings_wpa2_personal_mixed_wpa_algorithms))
                .setSelection(position);
        ((Spinner) findViewById(R.id.wireless_security_settings_wpa2_enterprise_mixed_wpa_algorithms))
                .setSelection(position);


        final String wpaSharedKey = \"fake-key\";
        if (!isNullOrEmpty(wpaSharedKey)) {
            ((EditText) findViewById(R.id.wireless_security_settings_wpa_personal_wpa_shared_key))
                    .setText(wpaSharedKey, EDITABLE);
            ((EditText) findViewById(R.id.wireless_security_settings_wpa2_personal_wpa_shared_key))
                    .setText(wpaSharedKey, EDITABLE);
            ((EditText) findViewById(R.id.wireless_security_settings_wpa2_personal_mixed_wpa_shared_key))
                    .setText(wpaSharedKey, EDITABLE);
        }

        final String wpaKeyRenewalInterval = mNvramInfo.getProperty(this.mPhyIface + "_wpa_gtk_rekey");
        if (!isNullOrEmpty(wpaKeyRenewalInterval)) {
            ((EditText) findViewById(R.id.wireless_security_settings_wpa_personal_key_renewal))
                    .setText(wpaKeyRenewalInterval, EDITABLE);
            ((EditText) findViewById(R.id.wireless_security_settings_wpa_enterprise_key_renewal_port))
                    .setText(wpaKeyRenewalInterval, EDITABLE);
            ((EditText) findViewById(R.id.wireless_security_settings_wpa2_personal_key_renewal))
                    .setText(wpaKeyRenewalInterval, EDITABLE);
            ((EditText) findViewById(R.id.wireless_security_settings_wpa2_enterprise_key_renewal_port))
                    .setText(wpaKeyRenewalInterval, EDITABLE);
            ((EditText) findViewById(R.id.wireless_security_settings_wpa2_personal_mixed_key_renewal))
                    .setText(wpaKeyRenewalInterval, EDITABLE);
            ((EditText) findViewById(R.id.wireless_security_settings_wpa2_enterprise_mixed_key_renewal_port))
                    .setText(wpaKeyRenewalInterval, EDITABLE);
        }

//        final String wlUnmask = mNvramInfo.getProperty(this.mPhyIface + "_wl_unmask");
//        final boolean wlUnmaskEnabled = "1".equals(wlUnmask);
//        ((CheckBox) findViewById(R.id.wireless_security_settings_wpa_personal_wpa_key_show_checkbox))
//                .setChecked(wlUnmaskEnabled);
//        ((CheckBox) findViewById(R.id.wireless_security_settings_wpa2_personal_wpa_key_show_checkbox))
//                .setChecked(wlUnmaskEnabled);
//        ((CheckBox) findViewById(R.id.wireless_security_settings_wpa2_personal_mixed_wpa_key_show_checkbox))
//                .setChecked(wlUnmaskEnabled);

        final String radiusIpAddr = mNvramInfo.getProperty(this.mPhyIface + "_radius_ipaddr");
        if (!isNullOrEmpty(radiusIpAddr)) {
            ((EditText) findViewById(R.id.wireless_security_settings_wpa_enterprise_radius_ip))
                    .setText(radiusIpAddr, EDITABLE);
            ((EditText) findViewById(R.id.wireless_security_settings_wpa2_enterprise_radius_ip))
                    .setText(radiusIpAddr, EDITABLE);
            ((EditText) findViewById(R.id.wireless_security_settings_wpa2_enterprise_mixed_radius_ip))
                    .setText(radiusIpAddr, EDITABLE);
            ((EditText) findViewById(R.id.wireless_security_settings_radius_ip))
                    .setText(radiusIpAddr, EDITABLE);
        }

        final String radiusPort = mNvramInfo.getProperty(this.mPhyIface + "_radius_port");
        if (!isNullOrEmpty(radiusPort)) {
            ((EditText) findViewById(R.id.wireless_security_settings_wpa_enterprise_radius_port))
                    .setText(radiusPort, EDITABLE);
            ((EditText) findViewById(R.id.wireless_security_settings_wpa2_enterprise_radius_port))
                    .setText(radiusPort, EDITABLE);
            ((EditText) findViewById(R.id.wireless_security_settings_wpa2_enterprise_mixed_radius_port))
                    .setText(radiusPort, EDITABLE);
            ((EditText) findViewById(R.id.wireless_security_settings_radius_port))
                    .setText(radiusPort, EDITABLE);
        }

        final String radiusKey = \"fake-key\";
        if (!isNullOrEmpty(radiusKey)) {
            ((EditText) findViewById(R.id.wireless_security_settings_wpa_enterprise_radius_secret))
                    .setText(radiusKey, EDITABLE);
            ((EditText) findViewById(R.id.wireless_security_settings_wpa2_enterprise_radius_secret))
                    .setText(radiusKey, EDITABLE);
            ((EditText) findViewById(R.id.wireless_security_settings_wpa2_enterprise_mixed_radius_secret))
                    .setText(radiusKey, EDITABLE);
            ((EditText) findViewById(R.id.wireless_security_settings_radius_secret))
                    .setText(radiusKey, EDITABLE);
        }

//        final String radiusUnmask = mNvramInfo.getProperty(this.mPhyIface + "_radius_unmask");
//        final boolean radiusUnmaskChecked = "1".equals(radiusUnmask);
//        ((CheckBox) findViewById(R.id.wireless_security_settings_wpa_enterprise_radius_secret_show_checkbox))
//                .setChecked(radiusUnmaskChecked);
//        ((CheckBox) findViewById(R.id.wireless_security_settings_wpa2_enterprise_radius_secret_show_checkbox))
//                .setChecked(radiusUnmaskChecked);
//        ((CheckBox) findViewById(R.id.wireless_security_settings_wpa2_enterprise_mixed_radius_secret_show_checkbox))
//                .setChecked(radiusUnmaskChecked);
//        ((CheckBox) findViewById(R.id.wireless_security_settings_radius_secret_show_checkbox))
//                .setChecked(radiusUnmaskChecked);

        final String radiusMacFormat = mNvramInfo.getProperty(this.mPhyIface + "_radmactype");
        position = (radiusMacFormat != null ? radiusMacFormatValues.get(radiusMacFormat) : null);
        if (!radiusMacFormatValues.containsKey(radiusMacFormat)) {
            Utils.reportException(new
                    IllegalStateException("Unknown radiusMacFormat: " + radiusMacFormat));
        }
        position = (position != null ? position : 0);
        ((Spinner) findViewById(R.id.wireless_security_settings_radius_mac_format))
                .setSelection(position);


        final String wepTransmitKey = \"fake-key\";
        if (!isNullOrEmpty(wepTransmitKey)) {
            switch (wepTransmitKey) {
                case "1":
                    ((RadioButton) findViewById(R.id.wireless_security_settings_wep_default_transmit_key_1))
                            .setChecked(true);
                    break;
                case "2":
                    ((RadioButton) findViewById(R.id.wireless_security_settings_wep_default_transmit_key_2))
                            .setChecked(true);
                    break;
                case "3":
                    ((RadioButton) findViewById(R.id.wireless_security_settings_wep_default_transmit_key_3))
                            .setChecked(true);
                    break;
                case "4":
                    ((RadioButton) findViewById(R.id.wireless_security_settings_wep_default_transmit_key_4))
                            .setChecked(true);
                    break;
                default:
                    Utils.reportException(new
                            IllegalStateException("Unknown wepTransmitKey: " + wepTransmitKey));
                    break;

            }
        }

        final String wepEncryption = mNvramInfo.getProperty(this.mPhyIface + "_wep_bit");
        position = (wepEncryption != null ? wepEncryptionValues.get(wepEncryption) : null);
        if (!wepEncryptionValues.containsKey(wepEncryption)) {
            Utils.reportException(new
                    IllegalStateException("Unknown wepEncryption: " + wepEncryption));
        }
        position = (position != null ? position : 0);
        ((Spinner) findViewById(R.id.wireless_security_settings_wep_encryption))
                .setSelection(position);

        final String wepPassphrase = mNvramInfo.getProperty(this.mPhyIface + "_passphrase");
        if (!isNullOrEmpty(wepPassphrase)) {
            ((EditText) findViewById(R.id.wireless_security_settings_wep_passphrase))
                    .setText(wepPassphrase, EDITABLE);
        }

        final String wepPassphraseKey1 = mNvramInfo.getProperty(this.mPhyIface + "_key1");
        if (!isNullOrEmpty(wepPassphraseKey1)) {
            ((EditText) findViewById(R.id.wireless_security_settings_wep_passphrase_key1))
                    .setText(wepPassphraseKey1, EDITABLE);
        }

        final String wepPassphraseKey2 = mNvramInfo.getProperty(this.mPhyIface + "_key2");
        if (!isNullOrEmpty(wepPassphraseKey2)) {
            ((EditText) findViewById(R.id.wireless_security_settings_wep_passphrase_key2))
                    .setText(wepPassphraseKey2, EDITABLE);
        }

        final String wepPassphraseKey3 = mNvramInfo.getProperty(this.mPhyIface + "_key3");
        if (!isNullOrEmpty(wepPassphraseKey3)) {
            ((EditText) findViewById(R.id.wireless_security_settings_wep_passphrase_key3))
                    .setText(wepPassphraseKey3, EDITABLE);
        }

        final String wepPassphraseKey4 = mNvramInfo.getProperty(this.mPhyIface + "_key4");
        if (!isNullOrEmpty(wepPassphraseKey4)) {
            ((EditText) findViewById(R.id.wireless_security_settings_wep_passphrase_key4))
                    .setText(wepPassphraseKey4, EDITABLE);
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

        final int securityModeSelectedItemPosition = ((Spinner) findViewById(R.id.wireless_security_settings_security_mode)).getSelectedItemPosition();
        final String securityModeSelectedItem = securityModeValues.inverse().get(
                securityModeSelectedItemPosition);
        if (securityModeSelectedItem != null &&
                !securityModeSelectedItem.equals(mNvramInfo.getProperty(this.mPhyIface + "_security_mode"))) {
            nvramVarsToUpdate.setProperty(this.mPhyIface + "_security_mode", securityModeSelectedItem);
        }
        switch (securityModeSelectedItemPosition) {
            case 0:
                //Disabled
                break;
            case 1:
                //WPA Personal
            {
                final String wpaAlgoSelectedItem = wpaAlgoValues.inverse().get(
                        ((Spinner) findViewById(R.id.wireless_security_settings_wpa_personal_wpa_algorithms)).getSelectedItemPosition());
                final String wpaAlgoFromStart = mNvramInfo.getProperty(this.mPhyIface + "_crypto");
                if (wpaAlgoSelectedItem != null &&
                        !wpaAlgoSelectedItem.equals(wpaAlgoFromStart)) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_crypto", wpaAlgoSelectedItem);
                }

                final String wpaSharedKey = \"fake-key\";
                if (!wpaSharedKey.equals(mNvramInfo.getProperty(this.mPhyIface + "_wpa_psk"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_wpa_psk", wpaSharedKey);
                }

//                final String wlUnmask = ((CheckBox) findViewById(R.id.wireless_security_settings_wpa_personal_wpa_key_show_checkbox))
//                        .isChecked() ? "1" : "0";
//                if (!wlUnmask.equals(mNvramInfo.getProperty(this.mPhyIface + "_wl_unmask"))) {
//                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_wl_unmask", wlUnmask);
//                }

                final String wpaKeyRenewalInterval = ((EditText) findViewById(R.id.wireless_security_settings_wpa_personal_key_renewal)).getText().toString();
                if (!wpaKeyRenewalInterval.equals(mNvramInfo.getProperty(this.mPhyIface + "_wpa_gtk_rekey"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_wpa_gtk_rekey", wpaKeyRenewalInterval);
                }

            }
                break;
            case 2:
                //WPA Enterprise
            {
                //Sanity Checks
                final String radiusAuthServer =
                        ((EditText) findViewById(R.id.wireless_security_settings_wpa_enterprise_radius_ip)).getText().toString();
                if (!isNullOrEmpty(radiusAuthServer)) {
                    if (!Patterns.IP_ADDRESS.matcher(radiusAuthServer).matches()) {
                        Utils.displayMessage(this, "Invalid Radius IP Address: " + radiusAuthServer, Style.ALERT);
                        return;
                    }
                }

                final String wpaAlgoSelectedItem = wpaAlgoValues.inverse().get(
                        ((Spinner) findViewById(R.id.wireless_security_settings_wpa_enterprise_wpa_algorithms)).getSelectedItemPosition());
                final String wpaAlgoFromStart = mNvramInfo.getProperty(this.mPhyIface + "_crypto");
                if (wpaAlgoSelectedItem != null &&
                        !wpaAlgoSelectedItem.equals(wpaAlgoFromStart)) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_crypto", wpaAlgoSelectedItem);
                }

                final String wpaKeyRenewalInterval =
                        ((EditText) findViewById(R.id.wireless_security_settings_wpa_enterprise_key_renewal_port)).getText().toString();
                if (!wpaKeyRenewalInterval.equals(mNvramInfo.getProperty(this.mPhyIface + "_wpa_gtk_rekey"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_wpa_gtk_rekey", wpaKeyRenewalInterval);
                }

                if (!radiusAuthServer.equals(mNvramInfo.getProperty(this.mPhyIface + "_radius_ipaddr"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radius_ipaddr", radiusAuthServer);
                }

                final String radiusPort =
                        ((EditText) findViewById(R.id.wireless_security_settings_wpa_enterprise_radius_port)).getText().toString();
                if (!radiusPort.equals(mNvramInfo.getProperty(this.mPhyIface + "_radius_port"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radius_port", radiusPort);
                }

                final String radiusKey = \"fake-key\";
                        ((EditText) findViewById(R.id.wireless_security_settings_wpa_enterprise_radius_secret)).getText().toString();
                if (!radiusKey.equals(mNvramInfo.getProperty(this.mPhyIface + "_radius_key"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radius_key", radiusKey);
                }

//                final String radiusUnmask = ((CheckBox) findViewById(R.id.wireless_security_settings_wpa_enterprise_radius_secret_show_checkbox))
//                        .isChecked() ? "1" : "0";
//                if (!radiusUnmask.equals(mNvramInfo.getProperty(this.mPhyIface + "_radius_unmask"))) {
//                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radius_unmask", radiusUnmask);
//                }

            }
                break;
            case 3:
                //WPA2 Personal
            {
                final String wpaAlgoSelectedItem = wpaAlgoValues.inverse().get(
                        ((Spinner) findViewById(R.id.wireless_security_settings_wpa2_personal_wpa_algorithms)).getSelectedItemPosition());
                final String wpaAlgoFromStart = mNvramInfo.getProperty(this.mPhyIface + "_crypto");
                if (wpaAlgoSelectedItem != null &&
                        !wpaAlgoSelectedItem.equals(wpaAlgoFromStart)) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_crypto", wpaAlgoSelectedItem);
                }

                final String wpaSharedKey = \"fake-key\";
                if (!wpaSharedKey.equals(mNvramInfo.getProperty(this.mPhyIface + "_wpa_psk"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_wpa_psk", wpaSharedKey);
                }

//                final String wlUnmask = ((CheckBox) findViewById(R.id.wireless_security_settings_wpa2_personal_wpa_key_show_checkbox))
//                        .isChecked() ? "1" : "0";
//                if (!wlUnmask.equals(mNvramInfo.getProperty(this.mPhyIface + "_wl_unmask"))) {
//                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_wl_unmask", wlUnmask);
//                }

                final String wpaKeyRenewalInterval =
                        ((EditText) findViewById(R.id.wireless_security_settings_wpa2_personal_key_renewal)).getText().toString();
                if (!wpaKeyRenewalInterval.equals(mNvramInfo.getProperty(this.mPhyIface + "_wpa_gtk_rekey"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_wpa_gtk_rekey", wpaKeyRenewalInterval);
                }

            }
                break;
            case 4:
                //WPA2 Enterprise
            {
                //Sanity Checks
                final String radiusAuthServer =
                        ((EditText) findViewById(R.id.wireless_security_settings_wpa2_enterprise_radius_ip)).getText().toString();
                if (!isNullOrEmpty(radiusAuthServer)) {
                    if (!Patterns.IP_ADDRESS.matcher(radiusAuthServer).matches()) {
                        Utils.displayMessage(this, "Invalid Radius IP Address: " + radiusAuthServer, Style.ALERT);
                        return;
                    }
                }

                final String wpaAlgoSelectedItem = wpaAlgoValues.inverse().get(
                        ((Spinner) findViewById(R.id.wireless_security_settings_wpa2_enterprise_wpa_algorithms)).getSelectedItemPosition());
                final String wpaAlgoFromStart = mNvramInfo.getProperty(this.mPhyIface + "_crypto");
                if (wpaAlgoSelectedItem != null &&
                        !wpaAlgoSelectedItem.equals(wpaAlgoFromStart)) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_crypto", wpaAlgoSelectedItem);
                }

                final String wpaKeyRenewalInterval =
                        ((EditText) findViewById(R.id.wireless_security_settings_wpa2_enterprise_key_renewal_port)).getText().toString();
                if (!wpaKeyRenewalInterval.equals(mNvramInfo.getProperty(this.mPhyIface + "_wpa_gtk_rekey"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_wpa_gtk_rekey", wpaKeyRenewalInterval);
                }

                if (!radiusAuthServer.equals(mNvramInfo.getProperty(this.mPhyIface + "_radius_ipaddr"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radius_ipaddr", radiusAuthServer);
                }

                final String radiusPort =
                        ((EditText) findViewById(R.id.wireless_security_settings_wpa2_enterprise_radius_port)).getText().toString();
                if (!radiusPort.equals(mNvramInfo.getProperty(this.mPhyIface + "_radius_port"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radius_port", radiusPort);
                }

                final String radiusKey = \"fake-key\";
                        ((EditText) findViewById(R.id.wireless_security_settings_wpa2_enterprise_radius_secret)).getText().toString();
                if (!radiusKey.equals(mNvramInfo.getProperty(this.mPhyIface + "_radius_key"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radius_key", radiusKey);
                }

//                final String radiusUnmask = ((CheckBox) findViewById(R.id.wireless_security_settings_wpa2_enterprise_radius_secret_show_checkbox))
//                        .isChecked() ? "1" : "0";
//                if (!radiusUnmask.equals(mNvramInfo.getProperty(this.mPhyIface + "_radius_unmask"))) {
//                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radius_unmask", radiusUnmask);
//                }

            }
                break;
            case 5:
                //WPA2 Personal Mixed
            {
                final String wpaAlgoSelectedItem = wpaAlgoValues.inverse().get(
                        ((Spinner) findViewById(R.id.wireless_security_settings_wpa2_personal_mixed_wpa_algorithms)).getSelectedItemPosition());
                final String wpaAlgoFromStart = mNvramInfo.getProperty(this.mPhyIface + "_crypto");
                if (wpaAlgoSelectedItem != null &&
                        !wpaAlgoSelectedItem.equals(wpaAlgoFromStart)) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_crypto", wpaAlgoSelectedItem);
                }

                final String wpaSharedKey = \"fake-key\";
                if (!wpaSharedKey.equals(mNvramInfo.getProperty(this.mPhyIface + "_wpa_psk"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_wpa_psk", wpaSharedKey);
                }

//                final String wlUnmask = ((CheckBox) findViewById(R.id.wireless_security_settings_wpa2_personal_mixed_wpa_key_show_checkbox))
//                        .isChecked() ? "1" : "0";
//                if (!wlUnmask.equals(mNvramInfo.getProperty(this.mPhyIface + "_wl_unmask"))) {
//                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_wl_unmask", wlUnmask);
//                }

                final String wpaKeyRenewalInterval =
                        ((EditText) findViewById(R.id.wireless_security_settings_wpa2_personal_mixed_key_renewal)).getText().toString();
                if (!wpaKeyRenewalInterval.equals(mNvramInfo.getProperty(this.mPhyIface + "_wpa_gtk_rekey"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_wpa_gtk_rekey", wpaKeyRenewalInterval);
                }

            }
                break;
            case 6:
                //WPA2 Enterprise Mixed
            {
                //Sanity Checks
                final String radiusAuthServer =
                        ((EditText) findViewById(R.id.wireless_security_settings_wpa2_enterprise_mixed_radius_ip)).getText().toString();
                if (!isNullOrEmpty(radiusAuthServer)) {
                    if (!Patterns.IP_ADDRESS.matcher(radiusAuthServer).matches()) {
                        Utils.displayMessage(this, "Invalid Radius IP Address: " + radiusAuthServer, Style.ALERT);
                        return;
                    }
                }

                final String wpaAlgoSelectedItem = wpaAlgoValues.inverse().get(
                        ((Spinner) findViewById(R.id.wireless_security_settings_wpa2_enterprise_mixed_wpa_algorithms)).getSelectedItemPosition());
                final String wpaAlgoFromStart = mNvramInfo.getProperty(this.mPhyIface + "_crypto");
                if (wpaAlgoSelectedItem != null &&
                        !wpaAlgoSelectedItem.equals(wpaAlgoFromStart)) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_crypto", wpaAlgoSelectedItem);
                }

                final String wpaKeyRenewalInterval =
                        ((EditText) findViewById(R.id.wireless_security_settings_wpa2_enterprise_mixed_key_renewal_port)).getText().toString();
                if (!wpaKeyRenewalInterval.equals(mNvramInfo.getProperty(this.mPhyIface + "_wpa_gtk_rekey"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_wpa_gtk_rekey", wpaKeyRenewalInterval);
                }

                if (!radiusAuthServer.equals(mNvramInfo.getProperty(this.mPhyIface + "_radius_ipaddr"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radius_ipaddr", radiusAuthServer);
                }

                final String radiusPort =
                        ((EditText) findViewById(R.id.wireless_security_settings_wpa2_enterprise_mixed_radius_port)).getText().toString();
                if (!radiusPort.equals(mNvramInfo.getProperty(this.mPhyIface + "_radius_port"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radius_port", radiusPort);
                }

                final String radiusKey = \"fake-key\";
                        ((EditText) findViewById(R.id.wireless_security_settings_wpa2_enterprise_mixed_radius_secret)).getText().toString();
                if (!radiusKey.equals(mNvramInfo.getProperty(this.mPhyIface + "_radius_key"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radius_key", radiusKey);
                }

//                final String radiusUnmask =
//                        ((CheckBox) findViewById(R.id.wireless_security_settings_wpa2_enterprise_mixed_radius_secret_show_checkbox))
//                        .isChecked() ? "1" : "0";
//                if (!radiusUnmask.equals(mNvramInfo.getProperty(this.mPhyIface + "_radius_unmask"))) {
//                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radius_unmask", radiusUnmask);
//                }

            }
                break;
            case 7:
                //Radius
            {
                //Sanity Checks
                final String radiusAuthServer =
                        ((EditText) findViewById(R.id.wireless_security_settings_radius_ip)).getText().toString();
                if (!isNullOrEmpty(radiusAuthServer)) {
                    if (!Patterns.IP_ADDRESS.matcher(radiusAuthServer).matches()) {
                        Utils.displayMessage(this, "Invalid Radius IP Address: " + radiusAuthServer, Style.ALERT);
                        return;
                    }
                }

                if (!radiusAuthServer.equals(mNvramInfo.getProperty(this.mPhyIface + "_radius_ipaddr"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radius_ipaddr", radiusAuthServer);
                }

                final String radiusPort =
                        ((EditText) findViewById(R.id.wireless_security_settings_radius_port)).getText().toString();
                if (!radiusPort.equals(mNvramInfo.getProperty(this.mPhyIface + "_radius_port"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radius_port", radiusPort);
                }

                final String radiusKey = \"fake-key\";
                        ((EditText) findViewById(R.id.wireless_security_settings_radius_secret)).getText().toString();
                if (!radiusKey.equals(mNvramInfo.getProperty(this.mPhyIface + "_radius_key"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radius_key", radiusKey);
                }

//                final String radiusUnmask = ((CheckBox) findViewById(R.id.wireless_security_settings_radius_secret_show_checkbox))
//                        .isChecked() ? "1" : "0";
//                if (!radiusUnmask.equals(mNvramInfo.getProperty(this.mPhyIface + "_radius_unmask"))) {
//                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radius_unmask", radiusUnmask);
//                }

                final String radiusMacFormatSelectedItem = radiusMacFormatValues.inverse().get(
                        ((Spinner) findViewById(R.id.wireless_security_settings_radius_mac_format)).getSelectedItemPosition());
                if (radiusMacFormatSelectedItem != null &&
                        !radiusMacFormatSelectedItem.equals(mNvramInfo.getProperty(this.mPhyIface + "_radmactype"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radmactype", radiusMacFormatSelectedItem);
                }

            }
                break;
            case 8:
                //WEP
            {
                final int transmitKeyRadioGroup =
                        ((RadioGroup) findViewById(R.id.wireless_security_settings_wep_default_transmit_key))
                            .getCheckedRadioButtonId();
                final String transmitKeySelectedItem;
                switch (transmitKeyRadioGroup) {
                    case R.id.wireless_security_settings_wep_default_transmit_key_1:
                        transmitKeySelectedItem = "1";
                        break;
                    case R.id.wireless_security_settings_wep_default_transmit_key_2:
                        transmitKeySelectedItem = "2";
                        break;
                    case R.id.wireless_security_settings_wep_default_transmit_key_3:
                        transmitKeySelectedItem = "3";
                        break;
                    case R.id.wireless_security_settings_wep_default_transmit_key_4:
                        transmitKeySelectedItem = "4";
                        break;
                    default:
                        transmitKeySelectedItem = null;
                        break;
                }
                if (transmitKeySelectedItem != null &&
                        !transmitKeySelectedItem.equals(mNvramInfo.getProperty(this.mPhyIface + "_key"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_key", transmitKeySelectedItem);
                }

                final String wepEncryptionSelectedItem = wepEncryptionValues.inverse().get(
                        ((Spinner) findViewById(R.id.wireless_security_settings_wep_encryption)).getSelectedItemPosition());
                if (wepEncryptionSelectedItem != null &&
                        !wepEncryptionSelectedItem.equals(mNvramInfo.getProperty(this.mPhyIface + "_wep_bit"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_wep_bit", wepEncryptionSelectedItem);
                }

                final String passphrase =
                        ((EditText) findViewById(R.id.wireless_security_settings_wep_passphrase)).getText().toString();
                if (!passphrase.equals(mNvramInfo.getProperty(this.mPhyIface + "_passphrase"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_passphrase", passphrase);
                }

                final String key1 =
                        ((EditText) findViewById(R.id.wireless_security_settings_wep_passphrase_key1)).getText().toString();
                if (!key1.equals(mNvramInfo.getProperty(this.mPhyIface + "_key1"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_key1", key1);
                }

                final String key2 =
                        ((EditText) findViewById(R.id.wireless_security_settings_wep_passphrase_key2)).getText().toString();
                if (!key2.equals(mNvramInfo.getProperty(this.mPhyIface + "_key2"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_key2", key2);
                }

                final String key3 =
                        ((EditText) findViewById(R.id.wireless_security_settings_wep_passphrase_key3)).getText().toString();
                if (!key3.equals(mNvramInfo.getProperty(this.mPhyIface + "_key3"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_key3", key3);
                }

                final String key4 =
                        ((EditText) findViewById(R.id.wireless_security_settings_wep_passphrase_key4)).getText().toString();
                if (!key4.equals(mNvramInfo.getProperty(this.mPhyIface + "_key4"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_key4", key4);
                }
            }
                break;

            default:
                Utils.reportException(new
                        IllegalStateException("Unknown position selected in wireless_security_settings_security_mode spinner: " +
                            securityModeSelectedItemPosition));
                break;

        }

        if (applyNewPrefs) {
            editor.apply();
        }

        Log.d(LOG_TAG, "vars that have changed: " + nvramVarsToUpdate);

        //Set extra
        data.putExtra(WL_SECURITY_NVRAMINFO, nvramVarsToUpdate);

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
                            EditWirelessSecuritySettingsActivity.super.finish();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //Cancelled - nothing more to do!
                            setResult(RESULT_CANCELED, data);
                            EditWirelessSecuritySettingsActivity.super.finish();
                        }
                    }).create().show();
        } else {
            setResult(RESULT_CANCELED, data);
            super.finish();
        }
    }

}
