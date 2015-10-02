package org.rm3l.ddwrt.tiles.status.wireless;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;

import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.utils.AdUtils;
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
    private View mWPAPersonal;
    private View mWPA2Personal;
    private View mWPA2PersonalMixed;
    private View mWPAEnterprise;
    private View mWPA2Enterprise;
    private View mWPA2EnterpriseMixed;
    private View mRadius;
    private View mWEP;
    private View[] detailedViews;

    @Nullable
    private InterstitialAd mInterstitialAd;
    private View mWEPAlert;

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

        mWPAPersonal = findViewById(R.id.wireless_security_settings_wpa_personal);
        mWPA2Personal = mWPAPersonal;
        mWPA2PersonalMixed = mWPA2Personal;
        mWPAEnterprise = findViewById(R.id.wireless_security_settings_wpa_enterprise);
        mWPA2Enterprise = mWPAEnterprise;
        mWPA2EnterpriseMixed = mWPA2Enterprise;
        mRadius = findViewById(R.id.wireless_security_settings_radius);
        mWEP = findViewById(R.id.wireless_security_settings_wep);
        mWEPAlert = findViewById(R.id.wireless_security_settings_wep_alert);

        mWPAPersonal.setVisibility(View.GONE);
        mWPA2Personal.setVisibility(View.GONE);
        mWPA2PersonalMixed.setVisibility(View.GONE);
        mWPAEnterprise.setVisibility(View.GONE);
        mWPA2Enterprise.setVisibility(View.GONE);
        mWPA2EnterpriseMixed.setVisibility(View.GONE);
        mRadius.setVisibility(View.GONE);
        mWEP.setVisibility(View.GONE);

        detailedViews = new View[] {
                mWPAPersonal,
                mWPAEnterprise,
                mWPA2Personal,
                mWPA2Enterprise,
                mWPA2PersonalMixed,
                mWPA2EnterpriseMixed,
                mRadius,
                mWEP};

        mSecurityModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
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

        mInterstitialAd = AdUtils.requestNewInterstitial(this,
                R.string.interstitial_ad_unit_id_wireless_network_security_settings);

    }

    private void showDetailedViewAt(final int position) {
        Log.d(LOG_TAG, "showDetailedViewAt @" + position);

        final int length = detailedViews.length;

        if (position < 0 || position >= length) {
            return;
        }
        for (int i = 0 ; i < length; i++) {
            final View detailedView = detailedViews[i];
            if (detailedView == null) {
                continue;
            }
            if (i == position) {
                detailedView.setVisibility(View.VISIBLE);
                if (detailedView == mWEP) {
                    mWEPAlert.setVisibility(View.VISIBLE);
                } else {
                    mWEPAlert.setVisibility(View.GONE);
                }
            } else {
                mWEPAlert.setVisibility(View.GONE);
                detailedView.setVisibility(View.GONE);
            }
        }
    }

    private void hideAllDetailedViews() {
        for (final View detailedView : detailedViews) {
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
        }

        final String wpaSharedKey = \"fake-key\";
        if (!isNullOrEmpty(wpaSharedKey)) {
            ((TextView) findViewById(R.id.wireless_security_settings_wpa_personal_wpa_shared_key))
                    .setText(wpaSharedKey);
        }

        final String wpaKeyRenewalInterval = mNvramInfo.getProperty(this.mPhyIface + "_wpa_gtk_rekey");
        if (!isNullOrEmpty(wpaKeyRenewalInterval)) {
            ((TextView) findViewById(R.id.wireless_security_settings_wpa_personal_key_renewal))
                    .setText(wpaKeyRenewalInterval);
        }

        //TODO


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
    @Override
    public void finish() {

        if (BuildConfig.WITH_ADS &&
                mInterstitialAd != null && AdUtils.canDisplayInterstialAd(this)) {

            mInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    EditWirelessSecuritySettingsActivity.super.finish();
                }

                @Override
                public void onAdOpened() {
                    //Save preference
                    getSharedPreferences(DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY,
                            Context.MODE_PRIVATE)
                            .edit()
                            .putLong(
                                    DDWRTCompanionConstants.AD_LAST_INTERSTITIAL_PREF,
                                    System.currentTimeMillis())
                            .apply();
                }
            });

            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
            } else {
                EditWirelessSecuritySettingsActivity.super.finish();
            }

        } else {
            super.finish();
        }

    }

}
