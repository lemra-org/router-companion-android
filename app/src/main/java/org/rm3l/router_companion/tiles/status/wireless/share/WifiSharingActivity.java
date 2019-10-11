package org.rm3l.router_companion.tiles.status.wireless.share;

import static org.rm3l.router_companion.tiles.status.wireless.WirelessIfaceTile.WirelessEncryptionTypeForQrCode.WEP;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import be.brunoparmentier.wifikeyshare.model.WifiAuthType;
import be.brunoparmentier.wifikeyshare.model.WifiNetwork;
import be.brunoparmentier.wifikeyshare.utils.NfcUtils;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.InterstitialAd;
import com.google.common.base.Strings;
import java.util.List;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.status.wireless.share.nfc.WifiSharingNfcFragment;
import org.rm3l.router_companion.tiles.status.wireless.share.nfc.WriteWifiConfigToNfcDialog;
import org.rm3l.router_companion.utils.AdUtils;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.Utils;

/**
 * Created by rm3l on 11/11/2016.
 */
public class WifiSharingActivity extends AppCompatActivity {

    public static final String TAG = WifiSharingActivity.class.getSimpleName();

    public static final String SSID = "SSID";

    public static final String ENC_TYPE = "ENC_TYPE";

    public static final String PWD = "PWD";

    public static final String WIFI_SHARING_DATA = "WIFI_SHARING_DATA";

    private static final String SAVED_WIFI_NFC_DIALOG_STATE = "wifi_nfc_dlg_state";

    private boolean isInWriteMode;

    private InterstitialAd mInterstitialAd;

    private String mRouterUuid;

    private String mSsid;

    private String mTitle;

    //Declaring All The Variables Needed
    private Toolbar mToolbar;

    private String mWifiEncType;

    private String mWifiPassword;

    private NfcAdapter nfcAdapter;

    private IntentFilter[] nfcIntentFilters;

    private PendingIntent nfcPendingIntent;

    private BroadcastReceiver nfcStateChangeBroadcastReceiver;

    private String[][] nfcTechLists;

    private TabLayout tabLayout;

    private ViewPager viewPager;

    private WifiSharingViewPagerAdapter viewPagerAdapter;

    private WriteWifiConfigToNfcDialog writeTagDialog;

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        mRouterUuid = intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);
        final DDWRTCompanionDAO dao = RouterManagementActivity.Companion.getDao(this);
        final Router router;
        if ((router = dao.getRouter(mRouterUuid)) == null) {
            Toast.makeText(this, "Internal Error: Router could not be determined", Toast.LENGTH_SHORT)
                    .show();
            finish();
            return;
        }

        final Router.RouterFirmware routerFirmware = router.getRouterFirmware();
        ColorUtils.Companion.setAppTheme(this, routerFirmware, false);

        final boolean themeLight = ColorUtils.Companion.isThemeLight(this);

        setContentView(R.layout.activity_wifi_sharing);

    /*
    Assigning view variables to thier respective view in xml
    by findViewByID method
     */

        mToolbar = (Toolbar) findViewById(R.id.tile_status_wireless_sharing_toolbar);
        tabLayout = (TabLayout) findViewById(R.id.tile_status_wireless_sharing_tabs);
        viewPager = (ViewPager) findViewById(R.id.tile_status_wireless_sharing_viewpager);

        mInterstitialAd = AdUtils.requestNewInterstitial(this,
                R.string.interstitial_ad_unit_id_wireless_network_generate_qr_code);

        mSsid = intent.getStringExtra(SSID);
        mWifiEncType = intent.getStringExtra(ENC_TYPE);
        mWifiPassword = intent.getStringExtra(PWD);
        //        mWifiQrCodeString = intent.getStringExtra(WIFI_QR_CODE);

        mTitle = ("WiFi Sharing: " + mSsid);

        if (mToolbar != null) {
            mToolbar.setTitle(mTitle);
            mToolbar.setSubtitle(
                    String.format("%s (%s:%d)", router.getDisplayName(), router.getRemoteIpAddress(),
                            router.getRemotePort()));
            mToolbar.setTitleTextAppearance(getApplicationContext(), R.style.ToolbarTitle);
            mToolbar.setSubtitleTextAppearance(getApplicationContext(), R.style.ToolbarSubtitle);
            mToolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
            mToolbar.setSubtitleTextColor(ContextCompat.getColor(this, R.color.white));
            //            setSupportActionBar(mToolbar);
        }

        viewPager.setBackgroundColor(
                ContextCompat.getColor(this, themeLight ? R.color.white : R.color.DimGray));

        /*
        Creating Adapter and setting that adapter to the viewPager
        setSupportActionBar method takes the toolbar and sets it as
        the default action bar thus making the toolbar work like a normal
        action bar.
         */
        viewPagerAdapter =
                new WifiSharingViewPagerAdapter(this, mRouterUuid, mSsid, mWifiEncType, mWifiPassword);
        viewPager.setAdapter(viewPagerAdapter);
        setSupportActionBar(mToolbar);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        final Integer primaryColor = ColorUtils.Companion.getPrimaryColor(routerFirmware);
        if (primaryColor != null) {
            tabLayout.setBackgroundColor(ContextCompat.getColor(this, primaryColor));
        } else {
            //Default
            tabLayout.setBackgroundColor(ContextCompat.getColor(this,
                    themeLight ? R.color.lightTheme_primary : R.color.darkTheme_primary));
        }

        /*
        TabLayout.newTab() method creates a tab view, Now a Tab view is not the view
        which is below the tabs, its the tab itself.
         */

        final TabLayout.Tab qrCode = tabLayout.newTab();

        /*
        Setting Title text for our tabs respectively
         */
        //        qrCode.setText("QR Code");
        qrCode.setIcon(R.drawable.ic_qrcode_white_24dp);


        /*
        Adding the tab view to our tablayout at appropriate positions
        As I want home at first position I am passing home and 0 as argument to
        the tablayout and like wise for other tabs as well
         */
        tabLayout.addTab(qrCode, 0);

        if (NfcUtils.hasNFCHardware(this)) {
            final TabLayout.Tab nfc = tabLayout.newTab();
            nfc.setIcon(R.drawable.ic_nfc_white_24dp);
            //        nfc.setText("NFC");
            tabLayout.addTab(nfc, 1);
        }

        /*
        TabTextColor sets the color for the title of the tabs, passing a ColorStateList here makes
        tab change colors in different situations such as selected, active, inactive etc

        TabIndicatorColor sets the color for the indicator below the tabs
         */

        //        tabLayout.setTabTextColors(ContextCompat.getColorStateList(this, R.color.tab_selector));
        //        tabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(this, R.color.indicator));

        /*
        Adding a onPageChangeListener to the viewPager
        1st we add the PageChangeListener and pass a TabLayoutPageChangeListener so that Tabs Selection
        changes when a viewpager page changes.
         */

        //        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        // Now we'll add a tab selected listener to set ViewPager's current item
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager));

        writeTagDialog = new WriteWifiConfigToNfcDialog(this, mSsid, Strings.nullToEmpty(mWifiPassword),
                mWifiEncType);

        //        writeTagDialog = new AlertDialog.Builder(this)
        //                .setTitle(getString(R.string.write_to_tag))
        //                .setMessage(getString(R.string.write_to_nfc_tag))
        //                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
        //                    @Override
        //                    public void onClick(DialogInterface dialogInterface, int i) {
        //                        disableTagWriteMode();
        //                        dialogInterface.dismiss();
        //                    }
        //                })
        //                .setCancelable(false)
        //                .create();

        isInWriteMode = false;

        isInWriteMode = false;
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (isNfcAvailable()) {
            initializeNfcStateChangeListener();
            setupForegroundDispatch();
            final WifiNetwork wifiNetwork =
                    new WifiNetwork(mSsid, WifiAuthType.valueOf(mWifiEncType), mWifiPassword, false);
            nfcAdapter.setNdefPushMessage(NfcUtils.generateNdefMessage(wifiNetwork), this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isNfcAvailable()) {
            IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
            registerReceiver(nfcStateChangeBroadcastReceiver, filter);
            //            startForegroundDispatch();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isNfcAvailable()) {
            //            stopForegroundDispatch();
            unregisterReceiver(nfcStateChangeBroadcastReceiver);
        }
    }

    public void disableTagWriteMode() {
        isInWriteMode = false;
        writeTagDialog.dismiss();
    }

    public void enableTagWriteMode() {
        isInWriteMode = true;
        writeTagDialog.dismiss();
        writeTagDialog = new WriteWifiConfigToNfcDialog(this, mSsid, Strings.nullToEmpty(mWifiPassword),
                mWifiEncType);
        writeTagDialog.show();
    }

    public boolean isNfcAvailable() {
        return (nfcAdapter != null);
    }

    public boolean isNfcEnabled() {
        return (isNfcAvailable() && nfcAdapter.isEnabled());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.tile_wifi_sharing_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.action_feedback) {
            Utils.openFeedbackForm(this, mRouterUuid);
            return true;
        } else {
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onNfcDisabled() {
        if (!WEP.name().equalsIgnoreCase(mWifiEncType)) { // writing WEP config is not supported
            // Update NFC write button and status text
            final FragmentManager fm = getSupportFragmentManager();
            final List<Fragment> fragments = fm.getFragments();
            if (fragments.size() >= 2) {
                WifiSharingNfcFragment nfcFragment = (WifiSharingNfcFragment) fragments.get(1);
                nfcFragment.setNfcStateEnabled(false);
            }
        }
    }

    protected void onNfcEnabled() {
        if (!WEP.name().equalsIgnoreCase(mWifiEncType)) { // writing WEP config is not supported
            // Update NFC write button and status text
            final FragmentManager fm = getSupportFragmentManager();
            final List<Fragment> fragments = fm.getFragments();
            if (fragments.size() >= 2) {
                WifiSharingNfcFragment nfcFragment = (WifiSharingNfcFragment) fragments.get(1);
                nfcFragment.setNfcStateEnabled(true);
            }
        }
    }

    private void initializeNfcStateChangeListener() {
        nfcStateChangeBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (action.equals(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)) {
                    final int state =
                            intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF);

                    switch (state) {
                        case NfcAdapter.STATE_OFF:
                        case NfcAdapter.STATE_TURNING_OFF:
                            onNfcDisabled();
                            break;
                        case NfcAdapter.STATE_TURNING_ON:
                            break;
                        case NfcAdapter.STATE_ON:
                            onNfcEnabled();
                            break;
                    }
                }
            }
        };
    }

    private void setupForegroundDispatch() {
        /* initialize the PendingIntent to start for the dispatch */
        nfcPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        /* initialize the IntentFilters to override dispatching for */
        nfcIntentFilters = new IntentFilter[3];
        nfcIntentFilters[0] = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        nfcIntentFilters[0].addCategory(Intent.CATEGORY_DEFAULT);
        nfcIntentFilters[1] = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        nfcIntentFilters[1].addCategory(Intent.CATEGORY_DEFAULT);
        nfcIntentFilters[2] = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        nfcIntentFilters[2].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            nfcIntentFilters[0].addDataType("*/*"); // Handle all MIME based dispatches.
        } catch (IntentFilter.MalformedMimeTypeException e) {
            Crashlytics.log(Log.ERROR, TAG, "setupForegroundDispatch: " + e.getMessage());
        }

        /* Initialize the tech lists used to perform matching for dispatching of the
         * ACTION_TECH_DISCOVERED intent */
        nfcTechLists = new String[][]{};
    }

    private void startForegroundDispatch() {
        nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, nfcIntentFilters, nfcTechLists);
    }

    private void stopForegroundDispatch() {
        nfcAdapter.disableForegroundDispatch(this);
    }
}
