package org.rm3l.router_companion.tiles.status.wireless;

import static android.widget.TextView.BufferType.EDITABLE;
import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.router_companion.mgmt.RouterManagementActivity.ROUTER_SELECTED;
import static org.rm3l.router_companion.tiles.status.wireless.WirelessIfaceTile.WL_SECURITY_NVRAMINFO;
import static org.rm3l.router_companion.utils.Utils.getHexString;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.status.wireless.share.WifiSharingActivity;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style;

/**
 * Created by rm3l on 29/09/15.
 */
public class EditWirelessSecuritySettingsActivity extends AppCompatActivity {

    public static final String WIRELESS_SECURITY_NVRAMINFO = "WIRELESS_SECURITY_NVRAMINFO";

    public static final String HWADDR = "HWADDR";

    private static final String LOG_TAG = EditWirelessSecuritySettingsActivity.class.getSimpleName();

    private static final BiMap<String, Integer> securityModeValues = HashBiMap.create(9);

    private static final BiMap<String, Integer> wpaAlgoValues = HashBiMap.create(3);

    private static final BiMap<String, Integer> radiusMacFormatValues = HashBiMap.create(4);

    private static final BiMap<String, Integer> wepEncryptionValues = HashBiMap.create(2);

    private int[] mDetailedViews;

    private String mHwAddr;

    private NVRAMInfo mNvramInfo;

    private String mParentIface;

    private String mPhyIface;

    private String mRouterUuid;

    private String mSsid;

    private Toolbar mToolbar;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        mNvramInfo = (NVRAMInfo) intent.getSerializableExtra(WIRELESS_SECURITY_NVRAMINFO);

        if (mNvramInfo == null || mNvramInfo.getData() == null) {
            Toast.makeText(this, "Could not load Wireless Security settings", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!mNvramInfo.getData().containsKey(WirelessIfaceTile.IFACE)) {
            Toast.makeText(this, "Could not load physical iface", Toast.LENGTH_SHORT).show();
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

        ColorUtils.Companion.setAppTheme(this, router != null ? router.getRouterFirmware() : null, false);

        final boolean themeLight = ColorUtils.Companion.isThemeLight(this);
        //        if (themeLight) {
        //            //Light
        ////            setTheme(R.style.AppThemeLight);
        //            getWindow().getDecorView()
        //                    .setBackgroundColor(ContextCompat.getColor(this,
        //                            android.R.color.white));
        //        }
        //        else {
        //            //Default is Dark
        //            setTheme(R.style.AppThemeDark);
        //        }

        setContentView(R.layout.activity_wireless_security_settings);

        mPhyIface = mNvramInfo.getProperty(WirelessIfaceTile.IFACE,
                RouterCompanionAppConstants.EMPTY_VALUE_TO_DISPLAY);

        mParentIface = mNvramInfo.getProperty(WirelessIfaceTile.PARENT_IFACE);

        mSsid = mNvramInfo.getProperty(WifiSharingActivity.SSID,
                RouterCompanionAppConstants.EMPTY_VALUE_TO_DISPLAY);

        mHwAddr = mNvramInfo.getProperty(HWADDR, RouterCompanionAppConstants.EMPTY_VALUE_TO_DISPLAY);

        mToolbar = (Toolbar) findViewById(R.id.wireless_security_settings_toolbar);
        if (mToolbar != null) {
            mToolbar.setTitle(String.format("Wireless Security: %s", mPhyIface));
            mToolbar.setSubtitle(
                    String.format("%s (%s:%d)", router.getDisplayName(), router.getRemoteIpAddress(),
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

        //Preferences saved globally, to be shared across different routers
        sharedPreferences = getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);

        mDetailedViews = new int[]{
                R.id.wireless_security_settings_wpa_personal,
                R.id.wireless_security_settings_wpa_enterprise,
                R.id.wireless_security_settings_wpa2_personal,
                R.id.wireless_security_settings_wpa2_enterprise,
                R.id.wireless_security_settings_wpa2_personal_mixed,
                R.id.wireless_security_settings_wpa2_enterprise_mixed,
                R.id.wireless_security_settings_radius, R.id.wireless_security_settings_wep
        };

        hideAllDetailedViews();

        ((Spinner) findViewById(
                R.id.wireless_security_settings_security_mode)).setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Crashlytics.log(Log.DEBUG, LOG_TAG, "onItemSelected @" + position);
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
        ((CheckBox) findViewById(
                R.id.wireless_security_settings_wpa_personal_wpa_key_show_checkbox)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
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
        ((CheckBox) findViewById(
                R.id.wireless_security_settings_wpa2_personal_wpa_key_show_checkbox)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
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
        ((CheckBox) findViewById(
                R.id.wireless_security_settings_wpa2_personal_mixed_wpa_key_show_checkbox))
                .setOnCheckedChangeListener(
                        new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                if (!isChecked) {
                                    wpa2PersMixedSharedKeyEditText.setInputType(
                                            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                                } else {
                                    wpa2PersMixedSharedKeyEditText.setInputType(
                                            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                                }
                                wpa2PersMixedSharedKeyEditText.setSelection(wpa2PersMixedSharedKeyEditText.length());
                            }
                        });

        final EditText wpaEnterpriseRadiusSecretEditText =
                (EditText) findViewById(R.id.wireless_security_settings_wpa_enterprise_radius_secret);
        ((CheckBox) findViewById(
                R.id.wireless_security_settings_wpa_enterprise_radius_secret_show_checkbox))
                .setOnCheckedChangeListener(
                        new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                if (!isChecked) {
                                    wpaEnterpriseRadiusSecretEditText.setInputType(
                                            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                                } else {
                                    wpaEnterpriseRadiusSecretEditText.setInputType(
                                            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                                }
                                wpaEnterpriseRadiusSecretEditText.setSelection(
                                        wpaEnterpriseRadiusSecretEditText.length());
                            }
                        });
        final EditText wpa2EnterpriseRadiusSecretEditText =
                (EditText) findViewById(R.id.wireless_security_settings_wpa2_enterprise_radius_secret);
        ((CheckBox) findViewById(
                R.id.wireless_security_settings_wpa2_enterprise_radius_secret_show_checkbox))
                .setOnCheckedChangeListener(
                        new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                if (!isChecked) {
                                    wpa2EnterpriseRadiusSecretEditText.setInputType(
                                            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                                } else {
                                    wpa2EnterpriseRadiusSecretEditText.setInputType(
                                            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                                }
                                wpa2EnterpriseRadiusSecretEditText.setSelection(
                                        wpa2EnterpriseRadiusSecretEditText.length());
                            }
                        });
        final EditText wpa2EnterpriseMixedRadiusSecretEditText = (EditText) findViewById(
                R.id.wireless_security_settings_wpa2_enterprise_mixed_radius_secret);
        ((CheckBox) findViewById(
                R.id.wireless_security_settings_wpa2_enterprise_mixed_radius_secret_show_checkbox))
                .setOnCheckedChangeListener(
                        new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                if (!isChecked) {
                                    wpa2EnterpriseMixedRadiusSecretEditText.setInputType(
                                            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                                } else {
                                    wpa2EnterpriseMixedRadiusSecretEditText.setInputType(
                                            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                                }
                                wpa2EnterpriseMixedRadiusSecretEditText.setSelection(
                                        wpa2EnterpriseMixedRadiusSecretEditText.length());
                            }
                        });

        final EditText radiusRadiusSecretEditText =
                (EditText) findViewById(R.id.wireless_security_settings_radius_secret);
        ((CheckBox) findViewById(
                R.id.wireless_security_settings_radius_secret_show_checkbox)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!isChecked) {
                            radiusRadiusSecretEditText.setInputType(
                                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        } else {
                            radiusRadiusSecretEditText.setInputType(
                                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        }
                        radiusRadiusSecretEditText.setSelection(radiusRadiusSecretEditText.length());
                    }
                });

        final EditText wepPassphraseKey1EditText =
                (EditText) findViewById(R.id.wireless_security_settings_wep_passphrase_key1);
        final EditText wepPassphraseKey2EditText =
                (EditText) findViewById(R.id.wireless_security_settings_wep_passphrase_key2);
        final EditText wepPassphraseKey3EditText =
                (EditText) findViewById(R.id.wireless_security_settings_wep_passphrase_key3);
        final EditText wepPassphraseKey4EditText =
                (EditText) findViewById(R.id.wireless_security_settings_wep_passphrase_key4);

        final Button wepKeyGenerateButton =
                (Button) findViewById(R.id.wireless_security_settings_wep_passphrase_generate);
        wepKeyGenerateButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //Mimic'ing the behavior of the built-in WEP key generator
                //cf. http://svn.dd-wrt.com/browser/src/router/httpd/validate/wepkey.c
                //cf. https://github.com/crockpotveggies/VeggieRecipies/blob/master/VeggieRecipies/src/it/evilsocket/dsploit/wifi/algorithms/OnoKeygen.java

                final String wepPassphrase =
                        ((EditText) findViewById(R.id.wireless_security_settings_wep_passphrase)).getText()
                                .toString();
                //WEP Bit: 64 or 128
                final String wepBitStr = wepEncryptionValues.inverse()
                        .get(((Spinner) findViewById(
                                R.id.wireless_security_settings_wep_encryption)).getSelectedItemPosition());
                if (wepBitStr == null || wepBitStr.isEmpty()) {
                    Toast.makeText(EditWirelessSecuritySettingsActivity.this,
                            "Internal Error - please try again later.", Toast.LENGTH_LONG).show();
                    return;
                }
                final int wepBit;
                try {
                    wepBit = Integer.parseInt(wepBitStr);
                } catch (final NumberFormatException e) {
                    Toast.makeText(EditWirelessSecuritySettingsActivity.this,
                            "Internal Error - please try again later.", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                    Utils.reportException(null, e);
                    return;
                }

                final int[] pseed = new int[]{0, 0, 0, 0};

                int randNumber;
                char[][] key128 = new char[4][14];
                char[][] key64 = new char[4][5];

                switch (wepBit) {
                    case 64: {
                        try {
                                    /*
                                     * generate seed for random number generator using key string...
                                     */
                            for (int i = 0; i < wepPassphrase.length(); i++) {
                                pseed[i % 4] ^= (int) wepPassphrase.charAt(i);
                            }

                                    /*
                                     * init PRN generator... note that this is equivalent to the
                                     * Microsoft srand() function.
                                     */
                            randNumber = pseed[0] | (pseed[1] << 8) | (pseed[2] << 16) | (pseed[3] << 24);
                            short tmp;

                                    /*
                                     * generate keys.
                                     */
                            for (int i = 0; i < 4; i++) {
                                String key = "";
                                for (int j = 0; j < 5; j++) {
                                    //noinspection PointlessBitwiseExpression
                                    randNumber = (randNumber * 0x343fd + 0x269ec3) & 0xffffffff;
                                    tmp = (short) ((randNumber >> 16) & 0xff);
                                    key += getHexString(tmp).toUpperCase();
                                }
                                key64[i] = key.toCharArray();
                            }

                            wepPassphraseKey1EditText.setText(String.valueOf(key64[0]), EDITABLE);
                            wepPassphraseKey2EditText.setText(String.valueOf(key64[1]), EDITABLE);
                            wepPassphraseKey3EditText.setText(String.valueOf(key64[2]), EDITABLE);
                            wepPassphraseKey4EditText.setText(String.valueOf(key64[3]), EDITABLE);

                            Utils.hideSoftKeyboard(EditWirelessSecuritySettingsActivity.this);
                        } catch (Exception e) {
                            Toast.makeText(EditWirelessSecuritySettingsActivity.this,
                                    "Internal Error - please try again later", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                            Utils.reportException(null, e);
                        }
                    }
                    break;
                    case 128: {
                        try {

                            String str = wepPassphrase;
                            key128[0] = wep128Passphase(str).toCharArray();

                            str += "#$%";
                            key128[1] = wep128Passphase(str).toCharArray();

                            str += "!@#";
                            key128[2] = wep128Passphase(str).toCharArray();

                            str += "%&^";
                            key128[3] = wep128Passphase(str).toCharArray();

                            wepPassphraseKey1EditText.setText(String.valueOf(key128[0]), EDITABLE);
                            wepPassphraseKey2EditText.setText(String.valueOf(key128[1]), EDITABLE);
                            wepPassphraseKey3EditText.setText(String.valueOf(key128[2]), EDITABLE);
                            wepPassphraseKey4EditText.setText(String.valueOf(key128[3]), EDITABLE);

                            Utils.hideSoftKeyboard(EditWirelessSecuritySettingsActivity.this);
                        } catch (Exception e) {
                            Toast.makeText(EditWirelessSecuritySettingsActivity.this,
                                    "Internal Error - please try again later", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                            Utils.reportException(null, e);
                        }
                    }
                    break;

                    default:
                        //Error
                        Utils.reportException(null, new IllegalStateException("Illegal wepBit: " + wepBit));
                        break;
                }
            }

            @NonNull
            private String padto64(@Nullable String val) {
                if (val == null || val.isEmpty()) {
                    return "";
                }

                String ret = "";
                for (int i = 0; i < (1 + (64 / (val.length()))); ++i) {
                    ret += val;
                }

                return ret.substring(0, 64);
            }

            @NonNull
            private String wep128Passphase(@NonNull final String passphrase)
                    throws NoSuchAlgorithmException {
                String key = "";
                final MessageDigest md = MessageDigest.getInstance("MD5");
                md.reset();
                md.update(padto64(passphrase).getBytes(UTF_8));
                byte[] hash = md.digest();
                for (int i = 0; i < 13; ++i) {
                    key += getHexString((short) hash[i]);
                }
                return key.toUpperCase();
            }
        });

        final EditText wepPassphraseEditText =
                (EditText) findViewById(R.id.wireless_security_settings_wep_passphrase);
        ((CheckBox) findViewById(
                R.id.wireless_security_settings_wep_passphrase_show_checkbox)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
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
        //Enable "Generate" Button only if passphrase is not empty
        wepPassphraseEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                wepKeyGenerateButton.setEnabled(s != null && s.length() > 0);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });

        ((CheckBox) findViewById(
                R.id.wireless_security_settings_wep_passphrase_key1_show_checkbox)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!isChecked) {
                            wepPassphraseKey1EditText.setInputType(
                                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        } else {
                            wepPassphraseKey1EditText.setInputType(
                                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        }
                        wepPassphraseKey1EditText.setSelection(wepPassphraseKey1EditText.length());
                    }
                });

        ((CheckBox) findViewById(
                R.id.wireless_security_settings_wep_passphrase_key2_show_checkbox)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!isChecked) {
                            wepPassphraseKey2EditText.setInputType(
                                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        } else {
                            wepPassphraseKey2EditText.setInputType(
                                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        }
                        wepPassphraseKey2EditText.setSelection(wepPassphraseKey2EditText.length());
                    }
                });

        ((CheckBox) findViewById(
                R.id.wireless_security_settings_wep_passphrase_key3_show_checkbox)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!isChecked) {
                            wepPassphraseKey3EditText.setInputType(
                                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        } else {
                            wepPassphraseKey3EditText.setInputType(
                                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        }
                        wepPassphraseKey3EditText.setSelection(wepPassphraseKey3EditText.length());
                    }
                });

        ((CheckBox) findViewById(
                R.id.wireless_security_settings_wep_passphrase_key4_show_checkbox)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!isChecked) {
                            wepPassphraseKey4EditText.setInputType(
                                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        } else {
                            wepPassphraseKey4EditText.setInputType(
                                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        }
                        wepPassphraseKey4EditText.setSelection(wepPassphraseKey4EditText.length());
                    }
                });

        //Fill form
        fillForm();
    }

    @Override
    public void finish() {
        final Intent data = new Intent();

        //Resulting intent: NVRAM Info edited with user info
        data.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouterUuid);

        final NVRAMInfo nvramVarsToUpdate = new NVRAMInfo();
        //Compare each variable

        boolean applyNewPrefs = false;

        if (sharedPreferences == null) {
            setResult(RESULT_CANCELED, data);
            super.finish();
            return;
        }

        final SharedPreferences.Editor editor = sharedPreferences.edit();

        final int securityModeSelectedItemPosition = ((Spinner) findViewById(
                R.id.wireless_security_settings_security_mode)).getSelectedItemPosition();
        final String securityModeSelectedItem =
                securityModeValues.inverse().get(securityModeSelectedItemPosition);
        if (securityModeSelectedItem != null && !securityModeSelectedItem.equals(
                mNvramInfo.getProperty(this.mPhyIface + "_security_mode", mNvramInfo.getProperty(
                        this.mPhyIface.replace(".", "X") + "_security_mode")))) {
            final Properties props = mNvramInfo.getData();
            if (props == null) {
                Toast.makeText(this, "Internal Error - please try again later.", Toast.LENGTH_SHORT).show();
                Utils.reportException(null, new IllegalStateException("mNvramInfo.getData() == NULL"));
                setResult(RESULT_CANCELED, data);
                super.finish();
                return;
            }
            if (props.containsKey(this.mPhyIface + "_security_mode")) {
                nvramVarsToUpdate.setProperty(this.mPhyIface + "_security_mode", securityModeSelectedItem);
            } else {
                nvramVarsToUpdate.setProperty(this.mPhyIface.replace(".", "X"),
                        securityModeSelectedItem);
            }
        }
        switch (securityModeSelectedItemPosition) {
            case 0:
                //Disabled
                break;
            case 1:
                //WPA Personal
            {
                final String wpaAlgoSelectedItem = wpaAlgoValues.inverse()
                        .get(((Spinner) findViewById(
                                R.id.wireless_security_settings_wpa_personal_wpa_algorithms))
                                .getSelectedItemPosition());
                final String wpaAlgoFromStart = mNvramInfo.getProperty(this.mPhyIface + "_crypto");
                if (wpaAlgoSelectedItem != null && !wpaAlgoSelectedItem.equals(wpaAlgoFromStart)) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_crypto", wpaAlgoSelectedItem);
                }

                final String wpaSharedKey = \"fake-key\";
                        R.id.wireless_security_settings_wpa_personal_wpa_shared_key)).getText().toString();
                if (!wpaSharedKey.equals(mNvramInfo.getProperty(this.mPhyIface + "_wpa_psk"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_wpa_psk", wpaSharedKey);
                }

                //                final String wlUnmask = ((CheckBox) findViewById(R.id.wireless_security_settings_wpa_personal_wpa_key_show_checkbox))
                //                        .isChecked() ? "1" : "0";
                //                if (!wlUnmask.equals(mNvramInfo.getProperty(this.mPhyIface + "_wl_unmask"))) {
                //                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_wl_unmask", wlUnmask);
                //                }

                final String wpaKeyRenewalInterval = ((EditText) findViewById(
                        R.id.wireless_security_settings_wpa_personal_key_renewal)).getText().toString();
                if (!wpaKeyRenewalInterval.equals(
                        mNvramInfo.getProperty(this.mPhyIface + "_wpa_gtk_rekey"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_wpa_gtk_rekey", wpaKeyRenewalInterval);
                }
            }
            break;
            case 2:
                //WPA Enterprise
            {
                //Sanity Checks
                final String radiusAuthServer = ((EditText) findViewById(
                        R.id.wireless_security_settings_wpa_enterprise_radius_ip)).getText().toString();
                if (!isNullOrEmpty(radiusAuthServer)) {
                    if (!Patterns.IP_ADDRESS.matcher(radiusAuthServer).matches()) {
                        Utils.displayMessage(this, "Invalid Radius IP Address: " + radiusAuthServer,
                                Style.ALERT);
                        return;
                    }
                }

                final String wpaAlgoSelectedItem = wpaAlgoValues.inverse()
                        .get(((Spinner) findViewById(
                                R.id.wireless_security_settings_wpa_enterprise_wpa_algorithms))
                                .getSelectedItemPosition());
                final String wpaAlgoFromStart = mNvramInfo.getProperty(this.mPhyIface + "_crypto");
                if (wpaAlgoSelectedItem != null && !wpaAlgoSelectedItem.equals(wpaAlgoFromStart)) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_crypto", wpaAlgoSelectedItem);
                }

                final String wpaKeyRenewalInterval = ((EditText) findViewById(
                        R.id.wireless_security_settings_wpa_enterprise_key_renewal_port)).getText().toString();
                if (!wpaKeyRenewalInterval.equals(
                        mNvramInfo.getProperty(this.mPhyIface + "_wpa_gtk_rekey"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_wpa_gtk_rekey", wpaKeyRenewalInterval);
                }

                if (!radiusAuthServer.equals(mNvramInfo.getProperty(this.mPhyIface + "_radius_ipaddr"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radius_ipaddr", radiusAuthServer);
                }

                final String radiusPort = ((EditText) findViewById(
                        R.id.wireless_security_settings_wpa_enterprise_radius_port)).getText().toString();
                if (!radiusPort.equals(mNvramInfo.getProperty(this.mPhyIface + "_radius_port"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radius_port", radiusPort);
                }

                final String radiusKey = \"fake-key\";
                        R.id.wireless_security_settings_wpa_enterprise_radius_secret)).getText().toString();
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
                final String wpaAlgoSelectedItem = wpaAlgoValues.inverse()
                        .get(((Spinner) findViewById(
                                R.id.wireless_security_settings_wpa2_personal_wpa_algorithms))
                                .getSelectedItemPosition());
                final String wpaAlgoFromStart = mNvramInfo.getProperty(this.mPhyIface + "_crypto");
                if (wpaAlgoSelectedItem != null && !wpaAlgoSelectedItem.equals(wpaAlgoFromStart)) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_crypto", wpaAlgoSelectedItem);
                }

                final String wpaSharedKey = \"fake-key\";
                        R.id.wireless_security_settings_wpa2_personal_wpa_shared_key)).getText().toString();
                if (!wpaSharedKey.equals(mNvramInfo.getProperty(this.mPhyIface + "_wpa_psk"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_wpa_psk", wpaSharedKey);
                }

                //                final String wlUnmask = ((CheckBox) findViewById(R.id.wireless_security_settings_wpa2_personal_wpa_key_show_checkbox))
                //                        .isChecked() ? "1" : "0";
                //                if (!wlUnmask.equals(mNvramInfo.getProperty(this.mPhyIface + "_wl_unmask"))) {
                //                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_wl_unmask", wlUnmask);
                //                }

                final String wpaKeyRenewalInterval = ((EditText) findViewById(
                        R.id.wireless_security_settings_wpa2_personal_key_renewal)).getText().toString();
                if (!wpaKeyRenewalInterval.equals(
                        mNvramInfo.getProperty(this.mPhyIface + "_wpa_gtk_rekey"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_wpa_gtk_rekey", wpaKeyRenewalInterval);
                }
            }
            break;
            case 4:
                //WPA2 Enterprise
            {
                //Sanity Checks
                final String radiusAuthServer = ((EditText) findViewById(
                        R.id.wireless_security_settings_wpa2_enterprise_radius_ip)).getText().toString();
                if (!isNullOrEmpty(radiusAuthServer)) {
                    if (!Patterns.IP_ADDRESS.matcher(radiusAuthServer).matches()) {
                        Utils.displayMessage(this, "Invalid Radius IP Address: " + radiusAuthServer,
                                Style.ALERT);
                        return;
                    }
                }

                final String wpaAlgoSelectedItem = wpaAlgoValues.inverse()
                        .get(((Spinner) findViewById(
                                R.id.wireless_security_settings_wpa2_enterprise_wpa_algorithms))
                                .getSelectedItemPosition());
                final String wpaAlgoFromStart = mNvramInfo.getProperty(this.mPhyIface + "_crypto");
                if (wpaAlgoSelectedItem != null && !wpaAlgoSelectedItem.equals(wpaAlgoFromStart)) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_crypto", wpaAlgoSelectedItem);
                }

                final String wpaKeyRenewalInterval = ((EditText) findViewById(
                        R.id.wireless_security_settings_wpa2_enterprise_key_renewal_port)).getText().toString();
                if (!wpaKeyRenewalInterval.equals(
                        mNvramInfo.getProperty(this.mPhyIface + "_wpa_gtk_rekey"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_wpa_gtk_rekey", wpaKeyRenewalInterval);
                }

                if (!radiusAuthServer.equals(mNvramInfo.getProperty(this.mPhyIface + "_radius_ipaddr"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radius_ipaddr", radiusAuthServer);
                }

                final String radiusPort = ((EditText) findViewById(
                        R.id.wireless_security_settings_wpa2_enterprise_radius_port)).getText().toString();
                if (!radiusPort.equals(mNvramInfo.getProperty(this.mPhyIface + "_radius_port"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radius_port", radiusPort);
                }

                final String radiusKey = \"fake-key\";
                        ((EditText) findViewById(R.id.wireless_security_settings_wpa2_enterprise_radius_secret))
                                .getText()
                                .toString();
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
                final String wpaAlgoSelectedItem = wpaAlgoValues.inverse()
                        .get(((Spinner) findViewById(
                                R.id.wireless_security_settings_wpa2_personal_mixed_wpa_algorithms))
                                .getSelectedItemPosition());
                final String wpaAlgoFromStart = mNvramInfo.getProperty(this.mPhyIface + "_crypto");
                if (wpaAlgoSelectedItem != null && !wpaAlgoSelectedItem.equals(wpaAlgoFromStart)) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_crypto", wpaAlgoSelectedItem);
                }

                final String wpaSharedKey = \"fake-key\";
                        R.id.wireless_security_settings_wpa2_personal_mixed_wpa_shared_key)).getText()
                        .toString();
                if (!wpaSharedKey.equals(mNvramInfo.getProperty(this.mPhyIface + "_wpa_psk"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_wpa_psk", wpaSharedKey);
                }

                //                final String wlUnmask = ((CheckBox) findViewById(R.id.wireless_security_settings_wpa2_personal_mixed_wpa_key_show_checkbox))
                //                        .isChecked() ? "1" : "0";
                //                if (!wlUnmask.equals(mNvramInfo.getProperty(this.mPhyIface + "_wl_unmask"))) {
                //                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_wl_unmask", wlUnmask);
                //                }

                final String wpaKeyRenewalInterval = ((EditText) findViewById(
                        R.id.wireless_security_settings_wpa2_personal_mixed_key_renewal)).getText().toString();
                if (!wpaKeyRenewalInterval.equals(
                        mNvramInfo.getProperty(this.mPhyIface + "_wpa_gtk_rekey"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_wpa_gtk_rekey", wpaKeyRenewalInterval);
                }
            }
            break;
            case 6:
                //WPA2 Enterprise Mixed
            {
                //Sanity Checks
                final String radiusAuthServer = ((EditText) findViewById(
                        R.id.wireless_security_settings_wpa2_enterprise_mixed_radius_ip)).getText().toString();
                if (!isNullOrEmpty(radiusAuthServer)) {
                    if (!Patterns.IP_ADDRESS.matcher(radiusAuthServer).matches()) {
                        Utils.displayMessage(this, "Invalid Radius IP Address: " + radiusAuthServer,
                                Style.ALERT);
                        return;
                    }
                }

                final String wpaAlgoSelectedItem = wpaAlgoValues.inverse()
                        .get(((Spinner) findViewById(
                                R.id.wireless_security_settings_wpa2_enterprise_mixed_wpa_algorithms))
                                .getSelectedItemPosition());
                final String wpaAlgoFromStart = mNvramInfo.getProperty(this.mPhyIface + "_crypto");
                if (wpaAlgoSelectedItem != null && !wpaAlgoSelectedItem.equals(wpaAlgoFromStart)) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_crypto", wpaAlgoSelectedItem);
                }

                final String wpaKeyRenewalInterval = ((EditText) findViewById(
                        R.id.wireless_security_settings_wpa2_enterprise_mixed_key_renewal_port)).getText()
                        .toString();
                if (!wpaKeyRenewalInterval.equals(
                        mNvramInfo.getProperty(this.mPhyIface + "_wpa_gtk_rekey"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_wpa_gtk_rekey", wpaKeyRenewalInterval);
                }

                if (!radiusAuthServer.equals(mNvramInfo.getProperty(this.mPhyIface + "_radius_ipaddr"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radius_ipaddr", radiusAuthServer);
                }

                final String radiusPort = ((EditText) findViewById(
                        R.id.wireless_security_settings_wpa2_enterprise_mixed_radius_port)).getText()
                        .toString();
                if (!radiusPort.equals(mNvramInfo.getProperty(this.mPhyIface + "_radius_port"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radius_port", radiusPort);
                }

                final String radiusKey = \"fake-key\";
                        R.id.wireless_security_settings_wpa2_enterprise_mixed_radius_secret)).getText()
                        .toString();
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
                        ((EditText) findViewById(R.id.wireless_security_settings_radius_ip)).getText()
                                .toString();
                if (!isNullOrEmpty(radiusAuthServer)) {
                    if (!Patterns.IP_ADDRESS.matcher(radiusAuthServer).matches()) {
                        Utils.displayMessage(this, "Invalid Radius IP Address: " + radiusAuthServer,
                                Style.ALERT);
                        return;
                    }
                }

                if (!radiusAuthServer.equals(mNvramInfo.getProperty(this.mPhyIface + "_radius_ipaddr"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radius_ipaddr", radiusAuthServer);
                }

                final String radiusPort =
                        ((EditText) findViewById(R.id.wireless_security_settings_radius_port)).getText()
                                .toString();
                if (!radiusPort.equals(mNvramInfo.getProperty(this.mPhyIface + "_radius_port"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radius_port", radiusPort);
                }

                final String radiusKey = \"fake-key\";
                        ((EditText) findViewById(R.id.wireless_security_settings_radius_secret)).getText()
                                .toString();
                if (!radiusKey.equals(mNvramInfo.getProperty(this.mPhyIface + "_radius_key"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radius_key", radiusKey);
                }

                //                final String radiusUnmask = ((CheckBox) findViewById(R.id.wireless_security_settings_radius_secret_show_checkbox))
                //                        .isChecked() ? "1" : "0";
                //                if (!radiusUnmask.equals(mNvramInfo.getProperty(this.mPhyIface + "_radius_unmask"))) {
                //                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radius_unmask", radiusUnmask);
                //                }

                final String radiusMacFormatSelectedItem = radiusMacFormatValues.inverse()
                        .get(((Spinner) findViewById(
                                R.id.wireless_security_settings_radius_mac_format)).getSelectedItemPosition());
                if (radiusMacFormatSelectedItem != null && !radiusMacFormatSelectedItem.equals(
                        mNvramInfo.getProperty(this.mPhyIface + "_radmactype"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_radmactype",
                            radiusMacFormatSelectedItem);
                }
            }
            break;
            case 8:
                //WEP
            {
                final int transmitKeyRadioGroup = ((RadioGroup) findViewById(
                        R.id.wireless_security_settings_wep_default_transmit_key)).getCheckedRadioButtonId();
                final String transmitKeySelectedItem;
                if (transmitKeyRadioGroup == R.id.wireless_security_settings_wep_default_transmit_key_1) {
                    transmitKeySelectedItem = "1";

                } else if (transmitKeyRadioGroup == R.id.wireless_security_settings_wep_default_transmit_key_2) {
                    transmitKeySelectedItem = "2";

                } else if (transmitKeyRadioGroup == R.id.wireless_security_settings_wep_default_transmit_key_3) {
                    transmitKeySelectedItem = "3";

                } else if (transmitKeyRadioGroup == R.id.wireless_security_settings_wep_default_transmit_key_4) {
                    transmitKeySelectedItem = "4";

                } else {
                    transmitKeySelectedItem = null;

                }
                if (transmitKeySelectedItem != null && !transmitKeySelectedItem.equals(
                        mNvramInfo.getProperty(this.mPhyIface + "_key"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_key", transmitKeySelectedItem);
                }

                final String wepEncryptionSelectedItem = wepEncryptionValues.inverse()
                        .get(((Spinner) findViewById(
                                R.id.wireless_security_settings_wep_encryption)).getSelectedItemPosition());
                if (wepEncryptionSelectedItem != null && !wepEncryptionSelectedItem.equals(
                        mNvramInfo.getProperty(this.mPhyIface + "_wep_bit"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_wep_bit", wepEncryptionSelectedItem);
                }

                final String passphrase =
                        ((EditText) findViewById(R.id.wireless_security_settings_wep_passphrase)).getText()
                                .toString();
                if (!passphrase.equals(mNvramInfo.getProperty(this.mPhyIface + "_passphrase"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_passphrase", passphrase);
                }

                final String key1 =
                        ((EditText) findViewById(R.id.wireless_security_settings_wep_passphrase_key1)).getText()
                                .toString();
                if (!key1.equals(mNvramInfo.getProperty(this.mPhyIface + "_key1"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_key1", key1);
                }

                final String key2 =
                        ((EditText) findViewById(R.id.wireless_security_settings_wep_passphrase_key2)).getText()
                                .toString();
                if (!key2.equals(mNvramInfo.getProperty(this.mPhyIface + "_key2"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_key2", key2);
                }

                final String key3 =
                        ((EditText) findViewById(R.id.wireless_security_settings_wep_passphrase_key3)).getText()
                                .toString();
                if (!key3.equals(mNvramInfo.getProperty(this.mPhyIface + "_key3"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_key3", key3);
                }

                final String key4 =
                        ((EditText) findViewById(R.id.wireless_security_settings_wep_passphrase_key4)).getText()
                                .toString();
                if (!key4.equals(mNvramInfo.getProperty(this.mPhyIface + "_key4"))) {
                    nvramVarsToUpdate.setProperty(this.mPhyIface + "_key4", key4);
                }
            }
            break;

            default:
                Utils.reportException(null, new IllegalStateException(
                        "Unknown position selected in wireless_security_settings_security_mode spinner: "
                                + securityModeSelectedItemPosition));
                break;
        }

        if (applyNewPrefs) {
            editor.apply();
        }

        Crashlytics.log(Log.DEBUG, LOG_TAG, "vars that have changed: " + nvramVarsToUpdate);

        //Set extra
        data.putExtra(WL_SECURITY_NVRAMINFO, nvramVarsToUpdate);

        if (!nvramVarsToUpdate.isEmpty()) {
            //Something changed - prompt confirmation dialog
            new AlertDialog.Builder(this).setIcon(R.drawable.ic_action_alert_warning)
                    //                    .setTitle("Review Changes")
                    .setMessage("Some settings have been updated. Do you want to save them?\n"
                            + "If you choose to apply the new settings, your router will be rebooted, "
                            + "and you might have to wait some time before connection is re-established.")
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
        getMenuInflater().inflate(R.menu.menu_activity_edit_wireless_security_settings, menu);
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
            //                final Intent intent = new Intent(EditWirelessSecuritySettingsActivity.this,
            //                        FeedbackActivity.class);
            //                intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouterUuid);
            //                final File screenshotFile = new File(getCacheDir(), "feedback_screenshot.png");
            //                ViewGroupUtils.exportViewToFile(EditWirelessSecuritySettingsActivity.this,
            //                        getWindow().getDecorView(), screenshotFile);
            //                intent.putExtra(FeedbackActivity.SCREENSHOT_FILE, screenshotFile.getAbsolutePath());
            //                intent.putExtra(FeedbackActivity.CALLER_ACTIVITY, this.getClass().getCanonicalName());
            //                startActivity(intent);
            ////                Utils.buildFeedbackDialog(this, true);
            return true;
        } else {
        }

        return super.onOptionsItemSelected(item);
    }

    private void fillForm() {
        //Fill form with data loaded
        ((TextView) findViewById(R.id.wireless_security_settings_phy_iface)).setText(
                mPhyIface + (isNullOrEmpty(mParentIface) ? ""
                        : (" (virt. interface of " + mParentIface + ")")));
        ((TextView) findViewById(R.id.wireless_security_settings_ssid)).setText(mSsid);
        ((TextView) findViewById(R.id.wireless_security_settings_hw_address)).setText(mHwAddr);

        //        final String securityMode = mNvramInfo.getProperty(isNullOrEmpty(this.mParentIface) ?
        //                (this.mPhyIface + "_security_mode") : (this.mPhyIface + "_akm"));
        final String securityMode = mNvramInfo.getProperty(this.mPhyIface + "_security_mode",
                mNvramInfo.getProperty(this.mPhyIface.replace(".", "X") + "_security_mode"));
        Integer position = (securityMode != null ? securityModeValues.get(securityMode) : null);
        if (!securityModeValues.containsKey(securityMode)) {
            Utils.reportException(null,
                    new IllegalStateException("Unknown securityMode: " + securityMode));
        }
        position = (position != null ? position : 0);
        ((Spinner) findViewById(R.id.wireless_security_settings_security_mode)).setSelection(position);

        final String wpaAlgo = mNvramInfo.getProperty(this.mPhyIface + "_crypto");
        position = (wpaAlgo != null ? wpaAlgoValues.get(wpaAlgo) : null);
        if (!wpaAlgoValues.containsKey(wpaAlgo)) {
            Utils.reportException(null, new IllegalStateException("Unknown wpaAlgo: " + wpaAlgo));
        }
        position = (position != null ? position : 0);
        ((Spinner) findViewById(
                R.id.wireless_security_settings_wpa_personal_wpa_algorithms)).setSelection(position);
        ((Spinner) findViewById(
                R.id.wireless_security_settings_wpa_enterprise_wpa_algorithms)).setSelection(position);
        ((Spinner) findViewById(
                R.id.wireless_security_settings_wpa2_personal_wpa_algorithms)).setSelection(position);
        ((Spinner) findViewById(
                R.id.wireless_security_settings_wpa2_enterprise_wpa_algorithms)).setSelection(position);
        ((Spinner) findViewById(
                R.id.wireless_security_settings_wpa2_personal_mixed_wpa_algorithms)).setSelection(position);
        ((Spinner) findViewById(
                R.id.wireless_security_settings_wpa2_enterprise_mixed_wpa_algorithms)).setSelection(
                position);

        final String wpaSharedKey = \"fake-key\";
        if (!isNullOrEmpty(wpaSharedKey)) {
            ((EditText) findViewById(
                    R.id.wireless_security_settings_wpa_personal_wpa_shared_key)).setText(wpaSharedKey,
                    EDITABLE);
            ((EditText) findViewById(
                    R.id.wireless_security_settings_wpa2_personal_wpa_shared_key)).setText(wpaSharedKey,
                    EDITABLE);
            ((EditText) findViewById(
                    R.id.wireless_security_settings_wpa2_personal_mixed_wpa_shared_key)).setText(wpaSharedKey,
                    EDITABLE);
        }

        final String wpaKeyRenewalInterval = mNvramInfo.getProperty(this.mPhyIface + "_wpa_gtk_rekey");
        if (!isNullOrEmpty(wpaKeyRenewalInterval)) {
            ((EditText) findViewById(R.id.wireless_security_settings_wpa_personal_key_renewal)).setText(
                    wpaKeyRenewalInterval, EDITABLE);
            ((EditText) findViewById(
                    R.id.wireless_security_settings_wpa_enterprise_key_renewal_port)).setText(
                    wpaKeyRenewalInterval, EDITABLE);
            ((EditText) findViewById(R.id.wireless_security_settings_wpa2_personal_key_renewal)).setText(
                    wpaKeyRenewalInterval, EDITABLE);
            ((EditText) findViewById(
                    R.id.wireless_security_settings_wpa2_enterprise_key_renewal_port)).setText(
                    wpaKeyRenewalInterval, EDITABLE);
            ((EditText) findViewById(
                    R.id.wireless_security_settings_wpa2_personal_mixed_key_renewal)).setText(
                    wpaKeyRenewalInterval, EDITABLE);
            ((EditText) findViewById(
                    R.id.wireless_security_settings_wpa2_enterprise_mixed_key_renewal_port)).setText(
                    wpaKeyRenewalInterval, EDITABLE);
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
            ((EditText) findViewById(R.id.wireless_security_settings_wpa_enterprise_radius_ip)).setText(
                    radiusIpAddr, EDITABLE);
            ((EditText) findViewById(R.id.wireless_security_settings_wpa2_enterprise_radius_ip)).setText(
                    radiusIpAddr, EDITABLE);
            ((EditText) findViewById(
                    R.id.wireless_security_settings_wpa2_enterprise_mixed_radius_ip)).setText(radiusIpAddr,
                    EDITABLE);
            ((EditText) findViewById(R.id.wireless_security_settings_radius_ip)).setText(radiusIpAddr,
                    EDITABLE);
        }

        final String radiusPort = mNvramInfo.getProperty(this.mPhyIface + "_radius_port");
        if (!isNullOrEmpty(radiusPort)) {
            ((EditText) findViewById(R.id.wireless_security_settings_wpa_enterprise_radius_port)).setText(
                    radiusPort, EDITABLE);
            ((EditText) findViewById(
                    R.id.wireless_security_settings_wpa2_enterprise_radius_port)).setText(radiusPort,
                    EDITABLE);
            ((EditText) findViewById(
                    R.id.wireless_security_settings_wpa2_enterprise_mixed_radius_port)).setText(radiusPort,
                    EDITABLE);
            ((EditText) findViewById(R.id.wireless_security_settings_radius_port)).setText(radiusPort,
                    EDITABLE);
        }

        final String radiusKey = \"fake-key\";
        if (!isNullOrEmpty(radiusKey)) {
            ((EditText) findViewById(
                    R.id.wireless_security_settings_wpa_enterprise_radius_secret)).setText(radiusKey,
                    EDITABLE);
            ((EditText) findViewById(
                    R.id.wireless_security_settings_wpa2_enterprise_radius_secret)).setText(radiusKey,
                    EDITABLE);
            ((EditText) findViewById(R.id.wireless_security_settings_wpa2_enterprise_mixed_radius_secret))
                    .setText(radiusKey, EDITABLE);
            ((EditText) findViewById(R.id.wireless_security_settings_radius_secret)).setText(radiusKey,
                    EDITABLE);
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
            Utils.reportException(null,
                    new IllegalStateException("Unknown radiusMacFormat: " + radiusMacFormat));
        }
        position = (position != null ? position : 0);
        ((Spinner) findViewById(R.id.wireless_security_settings_radius_mac_format)).setSelection(
                position);

        final String wepTransmitKey = \"fake-key\";
        if (!isNullOrEmpty(wepTransmitKey)) {
            switch (wepTransmitKey) {
                case "1":
                    ((RadioButton) findViewById(
                            R.id.wireless_security_settings_wep_default_transmit_key_1)).setChecked(true);
                    break;
                case "2":
                    ((RadioButton) findViewById(
                            R.id.wireless_security_settings_wep_default_transmit_key_2)).setChecked(true);
                    break;
                case "3":
                    ((RadioButton) findViewById(
                            R.id.wireless_security_settings_wep_default_transmit_key_3)).setChecked(true);
                    break;
                case "4":
                    ((RadioButton) findViewById(
                            R.id.wireless_security_settings_wep_default_transmit_key_4)).setChecked(true);
                    break;
                default:
                    Utils.reportException(null,
                            new IllegalStateException("Unknown wepTransmitKey: " + wepTransmitKey));
                    break;
            }
        }

        final String wepEncryption = mNvramInfo.getProperty(this.mPhyIface + "_wep_bit");
        position = (wepEncryption != null ? wepEncryptionValues.get(wepEncryption) : null);
        if (!wepEncryptionValues.containsKey(wepEncryption)) {
            Crashlytics.log(Log.DEBUG, LOG_TAG, "mNvramInfo: " + mNvramInfo);
            Utils.reportException(null,
                    new IllegalStateException("Unknown wepEncryption: " + wepEncryption));
        }
        position = (position != null ? position : 0);
        ((Spinner) findViewById(R.id.wireless_security_settings_wep_encryption)).setSelection(position);

        final String wepPassphrase = mNvramInfo.getProperty(this.mPhyIface + "_passphrase");
        if (!isNullOrEmpty(wepPassphrase)) {
            ((EditText) findViewById(R.id.wireless_security_settings_wep_passphrase)).setText(
                    wepPassphrase, EDITABLE);
            findViewById(R.id.wireless_security_settings_wep_passphrase_generate).setEnabled(true);
        } else {
            findViewById(R.id.wireless_security_settings_wep_passphrase_generate).setEnabled(false);
        }

        final String wepPassphraseKey1 = mNvramInfo.getProperty(this.mPhyIface + "_key1");
        if (!isNullOrEmpty(wepPassphraseKey1)) {
            ((EditText) findViewById(R.id.wireless_security_settings_wep_passphrase_key1)).setText(
                    wepPassphraseKey1, EDITABLE);
        }

        final String wepPassphraseKey2 = mNvramInfo.getProperty(this.mPhyIface + "_key2");
        if (!isNullOrEmpty(wepPassphraseKey2)) {
            ((EditText) findViewById(R.id.wireless_security_settings_wep_passphrase_key2)).setText(
                    wepPassphraseKey2, EDITABLE);
        }

        final String wepPassphraseKey3 = mNvramInfo.getProperty(this.mPhyIface + "_key3");
        if (!isNullOrEmpty(wepPassphraseKey3)) {
            ((EditText) findViewById(R.id.wireless_security_settings_wep_passphrase_key3)).setText(
                    wepPassphraseKey3, EDITABLE);
        }

        final String wepPassphraseKey4 = mNvramInfo.getProperty(this.mPhyIface + "_key4");
        if (!isNullOrEmpty(wepPassphraseKey4)) {
            ((EditText) findViewById(R.id.wireless_security_settings_wep_passphrase_key4)).setText(
                    wepPassphraseKey4, EDITABLE);
        }
    }

    private void hideAllDetailedViews() {
        findViewById(R.id.wireless_security_settings_wep_alert).setVisibility(View.GONE);
        for (final int detailedViewId : mDetailedViews) {
            final View detailedView = findViewById(detailedViewId);
            if (detailedView == null) {
                continue;
            }
            detailedView.setVisibility(View.GONE);
        }
    }

    private void showDetailedViewAt(final int position) {
        Crashlytics.log(Log.DEBUG, LOG_TAG, "showDetailedViewAt @" + position);

        boolean wepVisible = false;
        for (int i = 0; i < mDetailedViews.length; i++) {
            final int detailedViewId = mDetailedViews[i];
            Crashlytics.log(Log.DEBUG, LOG_TAG, "<i,detailedViewId>=<" + i + "," + detailedViewId + ">");

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

        findViewById(R.id.wireless_security_settings_wep_alert).setVisibility(
                wepVisible ? View.VISIBLE : View.GONE);
    }

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
}
