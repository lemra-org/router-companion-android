package org.rm3l.ddwrt.tiles.status.wireless.share;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.ads.InterstitialAd;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.AdUtils;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.Utils;

/**
 * Created by rm3l on 11/11/2016.
 */
public class WifiSharingActivity extends AppCompatActivity {

    public static final String SSID = "SSID";

    //Declaring All The Variables Needed

    private Toolbar mToolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private WifiSharingViewPagerAdapter viewPagerAdapter;
    private InterstitialAd mInterstitialAd;
    private String mRouterUuid;
    private String mSsid;
    private String mTitle;


    @SuppressLint("DefaultLocale")
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

        final Intent intent = getIntent();
        mRouterUuid = intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);
        mSsid = intent.getStringExtra(SSID);
//        mWifiQrCodeString = intent.getStringExtra(WIFI_QR_CODE);

        mTitle = ("WiFi Sharing: " + mSsid);

        final DDWRTCompanionDAO dao = RouterManagementActivity.getDao(this);
        final Router router;
        if ((router = dao.getRouter(mRouterUuid)) == null) {
            Toast.makeText(this, "Internal Error: Router could not be determined", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (mToolbar != null) {
            mToolbar.setTitle(mTitle);
            mToolbar.setSubtitle(String.format("%s (%s:%d)",
                    router.getDisplayName(),
                    router.getRemoteIpAddress(),
                    router.getRemotePort()));
            mToolbar.setTitleTextAppearance(getApplicationContext(), R.style.ToolbarTitle);
            mToolbar.setSubtitleTextAppearance(getApplicationContext(), R.style.ToolbarSubtitle);
            mToolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
            mToolbar.setSubtitleTextColor(ContextCompat.getColor(this, R.color.white));
//            setSupportActionBar(mToolbar);
        }

        /*
        Creating Adapter and setting that adapter to the viewPager
        setSupportActionBar method takes the toolbar and sets it as
        the default action bar thus making the toolbar work like a normal
        action bar.
         */
        viewPagerAdapter = new WifiSharingViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        setSupportActionBar(mToolbar);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        /*
        TabLayout.newTab() method creates a tab view, Now a Tab view is not the view
        which is below the tabs, its the tab itself.
         */

        final TabLayout.Tab qrCode = tabLayout.newTab();
        final TabLayout.Tab nfc = tabLayout.newTab();

        /*
        Setting Title text for our tabs respectively
         */
        qrCode.setText("QR Code");
        nfc.setText("NFC");

        /*
        Adding the tab view to our tablayout at appropriate positions
        As I want home at first position I am passing home and 0 as argument to
        the tablayout and like wise for other tabs as well
         */
        tabLayout.addTab(qrCode, 0);
        tabLayout.addTab(nfc, 1);

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

        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.tile_wifi_sharing_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.action_feedback:
                Utils.openFeedbackForm(this, mRouterUuid);
                return true;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}
