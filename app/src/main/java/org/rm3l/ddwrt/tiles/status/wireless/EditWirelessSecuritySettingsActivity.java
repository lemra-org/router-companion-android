package org.rm3l.ddwrt.tiles.status.wireless;

import android.content.Context;
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
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.Utils;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.ddwrt.mgmt.RouterManagementActivity.ROUTER_SELECTED;
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
    private Spinner mSecurityModeSpinner;
    private View mWEP;
    private int[] mDetailedViews;

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

        mSecurityModeSpinner = ((Spinner) findViewById(R.id.wireless_security_settings_security_mode));

        mWEP = findViewById(R.id.wireless_security_settings_wep);

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

        mSecurityModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
                    if (detailedViewId == R.id.wireless_security_settings_wep_alert) {
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

        final Spinner securityModeSpinner =
                (Spinner) findViewById(R.id.wireless_security_settings_security_mode);
        final String securityMode = mNvramInfo.getProperty(this.mPhyIface + "_security_mode");
        if (!isNullOrEmpty(securityMode)) {
            final int position;
            switch (securityMode) {
                case "disabled":
                    position = 0;
                    break;
                case "psk":
                    position = 1;
                    break;
                case "wpa":
                    position = 2;
                    break;
                case "psk2":
                    position = 3;
                    break;
                case "wpa2":
                    position = 4;
                    break;
                case "psk psk2":
                    position = 5;
                    break;
                case "wpa wpa2":
                    position = 6;
                    break;
                case "radius":
                    position = 7;
                    break;
                case "wep":
                    position = 8;
                    break;
                default:
                    Utils.reportException(new
                            IllegalStateException("Unknown securityMode: " + securityMode));
                    position = 0;
                    break;

            }
            securityModeSpinner.setSelection(position, true);
        }

        final String wpaAlgo = mNvramInfo.getProperty(this.mPhyIface + "_crypto");
        if (!isNullOrEmpty(wpaAlgo)) {
            final int position;
            switch (wpaAlgo) {
                case "tkip":
                    position = 0;
                    break;
                case "aes":
                    position = 1;
                    break;
                case "tkip+aes":
                    position = 2;
                    break;
                default:
                    Utils.reportException(new
                            IllegalStateException("Unknown wpaAlgo: " + wpaAlgo));
                    position = 0;
                    break;

            }
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
        }

        final String wpaSharedKey = \"fake-key\";
        if (!isNullOrEmpty(wpaSharedKey)) {
            ((EditText) findViewById(R.id.wireless_security_settings_wpa_personal_wpa_shared_key))
                    .setText(wpaSharedKey);
            ((EditText) findViewById(R.id.wireless_security_settings_wpa2_personal_wpa_shared_key))
                    .setText(wpaSharedKey);
            ((EditText) findViewById(R.id.wireless_security_settings_wpa2_personal_mixed_wpa_shared_key))
                    .setText(wpaSharedKey);
        }

        final String wpaKeyRenewalInterval = mNvramInfo.getProperty(this.mPhyIface + "_wpa_gtk_rekey");
        if (!isNullOrEmpty(wpaKeyRenewalInterval)) {
            ((TextView) findViewById(R.id.wireless_security_settings_wpa_personal_key_renewal))
                    .setText(wpaKeyRenewalInterval);
            ((TextView) findViewById(R.id.wireless_security_settings_wpa_enterprise_key_renewal_port))
                    .setText(wpaKeyRenewalInterval);
            ((TextView) findViewById(R.id.wireless_security_settings_wpa2_personal_key_renewal))
                    .setText(wpaKeyRenewalInterval);
            ((TextView) findViewById(R.id.wireless_security_settings_wpa2_enterprise_key_renewal_port))
                    .setText(wpaKeyRenewalInterval);
            ((TextView) findViewById(R.id.wireless_security_settings_wpa2_personal_mixed_key_renewal))
                    .setText(wpaKeyRenewalInterval);
            ((TextView) findViewById(R.id.wireless_security_settings_wpa2_enterprise_mixed_key_renewal_port))
                    .setText(wpaKeyRenewalInterval);
        }

        final String wlUnmask = mNvramInfo.getProperty(this.mPhyIface + "_wl_unmask");
        final boolean wlUnmaskEnabled = "1".equals(wlUnmask);
        ((CheckBox) findViewById(R.id.wireless_security_settings_wpa_personal_wpa_key_show_checkbox))
                .setChecked(wlUnmaskEnabled);
        ((CheckBox) findViewById(R.id.wireless_security_settings_wpa2_personal_wpa_key_show_checkbox))
                .setChecked(wlUnmaskEnabled);
        ((CheckBox) findViewById(R.id.wireless_security_settings_wpa2_personal_mixed_wpa_key_show_checkbox))
                .setChecked(wlUnmaskEnabled);

        final String radiusIpAddr = mNvramInfo.getProperty(this.mPhyIface + "_radius_ipaddr");
        if (!isNullOrEmpty(radiusIpAddr)) {
            ((TextView) findViewById(R.id.wireless_security_settings_wpa_enterprise_radius_ip))
                    .setText(radiusIpAddr);
            ((TextView) findViewById(R.id.wireless_security_settings_wpa2_enterprise_radius_ip))
                    .setText(radiusIpAddr);
            ((TextView) findViewById(R.id.wireless_security_settings_wpa2_enterprise_mixed_radius_ip))
                    .setText(radiusIpAddr);
            ((TextView) findViewById(R.id.wireless_security_settings_radius_ip))
                    .setText(radiusIpAddr);
        }

        final String radiusPort = mNvramInfo.getProperty(this.mPhyIface + "_radius_port");
        if (!isNullOrEmpty(radiusPort)) {
            ((TextView) findViewById(R.id.wireless_security_settings_wpa_enterprise_radius_port))
                    .setText(radiusPort);
            ((TextView) findViewById(R.id.wireless_security_settings_wpa2_enterprise_radius_port))
                    .setText(radiusPort);
            ((TextView) findViewById(R.id.wireless_security_settings_wpa2_enterprise_mixed_radius_port))
                    .setText(radiusPort);
            ((TextView) findViewById(R.id.wireless_security_settings_radius_port))
                    .setText(radiusPort);
        }

        final String radiusKey = \"fake-key\";
        if (!isNullOrEmpty(radiusKey)) {
            ((TextView) findViewById(R.id.wireless_security_settings_wpa_enterprise_radius_secret))
                    .setText(radiusKey);
            ((TextView) findViewById(R.id.wireless_security_settings_wpa2_enterprise_radius_secret))
                    .setText(radiusKey);
            ((TextView) findViewById(R.id.wireless_security_settings_wpa2_enterprise_mixed_radius_secret))
                    .setText(radiusKey);
            ((TextView) findViewById(R.id.wireless_security_settings_radius_secret))
                    .setText(radiusKey);
        }

        final String radiusUnmask = mNvramInfo.getProperty(this.mPhyIface + "_radius_unmask");
        final boolean radiusUnmaskChecked = "1".equals(radiusUnmask);
        ((CheckBox) findViewById(R.id.wireless_security_settings_wpa_enterprise_radius_secret_show_checkbox))
                .setChecked(radiusUnmaskChecked);
        ((CheckBox) findViewById(R.id.wireless_security_settings_wpa2_enterprise_radius_secret_show_checkbox))
                .setChecked(radiusUnmaskChecked);
        ((CheckBox) findViewById(R.id.wireless_security_settings_wpa2_enterprise_mixed_radius_secret_show_checkbox))
                .setChecked(radiusUnmaskChecked);
        ((CheckBox) findViewById(R.id.wireless_security_settings_radius_secret_show_checkbox))
                .setChecked(radiusUnmaskChecked);

        final String radiusMacFormat = mNvramInfo.getProperty(this.mPhyIface + "_radmactype");
        if (!isNullOrEmpty(radiusMacFormat)) {
            final int position;
            switch (radiusMacFormat) {
                case "aabbcc-ddeeff":
                    position = 0;
                    break;
                case "aabbccddeeff":
                    position = 1;
                    break;
                case "aa:bb:cc:dd:ee:ff":
                    position = 2;
                    break;
                case "aa-bb-cc-dd-ee-ff":
                    position = 3;
                    break;
                default:
                    Utils.reportException(new
                            IllegalStateException("Unknown radiusMacFormat: " + radiusMacFormat));
                    position = 0;
                    break;

            }
            ((Spinner) findViewById(R.id.wireless_security_settings_radius_mac_format))
                    .setSelection(position);
        }

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
        if (!isNullOrEmpty(wepEncryption)) {
            final int position;
            switch (wepEncryption) {
                case "64":
                    position = 0;
                    break;
                case "128":
                    position = 1;
                    break;
                default:
                    Utils.reportException(new
                            IllegalStateException("Unknown wepEncryption: " + wepEncryption));
                    position = 0;
                    break;

            }
            ((Spinner) findViewById(R.id.wireless_security_settings_wep_encryption))
                    .setSelection(position);
        }

        final String wepPassphrase = mNvramInfo.getProperty(this.mPhyIface + "_passphrase");
        if (!isNullOrEmpty(wepPassphrase)) {
            ((TextView) findViewById(R.id.wireless_security_settings_wep_passphrase))
                    .setText(wepPassphrase);
        }

        final String wepPassphraseKey1 = mNvramInfo.getProperty(this.mPhyIface + "_key1");
        if (!isNullOrEmpty(wepPassphraseKey1)) {
            ((TextView) findViewById(R.id.wireless_security_settings_wep_passphrase_key1))
                    .setText(wepPassphraseKey1);
        }

        final String wepPassphraseKey2 = mNvramInfo.getProperty(this.mPhyIface + "_key2");
        if (!isNullOrEmpty(wepPassphraseKey2)) {
            ((TextView) findViewById(R.id.wireless_security_settings_wep_passphrase_key2))
                    .setText(wepPassphraseKey2);
        }

        final String wepPassphraseKey3 = mNvramInfo.getProperty(this.mPhyIface + "_key3");
        if (!isNullOrEmpty(wepPassphraseKey3)) {
            ((TextView) findViewById(R.id.wireless_security_settings_wep_passphrase_key3))
                    .setText(wepPassphraseKey3);
        }

        final String wepPassphraseKey4 = mNvramInfo.getProperty(this.mPhyIface + "_key4");
        if (!isNullOrEmpty(wepPassphraseKey4)) {
            ((TextView) findViewById(R.id.wireless_security_settings_wep_passphrase_key4))
                    .setText(wepPassphraseKey4);
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

    //TODO Add onFinish() method

}
