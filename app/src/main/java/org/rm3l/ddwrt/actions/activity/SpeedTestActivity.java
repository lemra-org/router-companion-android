package org.rm3l.ddwrt.actions.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdView;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.AdUtils;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.Utils;
import org.rm3l.ddwrt.utils.snackbar.SnackbarCallback;
import org.rm3l.ddwrt.utils.snackbar.SnackbarUtils;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * TODO
 * Created by rm3l on 20/12/15.
 */
public class SpeedTestActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, SnackbarCallback {

    private static final String LOG_TAG = SpeedTestActivity
            .class.getSimpleName();
    public static final String NET_LATENCY = "net_latency";
    public static final String NET_DL = "net_dl";
    public static final String NET_UL = "net_ul";
    public static final String NET_WIFI = "net_wifi";
    private Handler mHandler;

    private boolean mIsThemeLight;
    private Router mRouter;

    private Toolbar mToolbar;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private Menu optionsMenu;

    private BroadcastReceiver mMessageReceiver;

    private TextView mSpeedtestLatencyTitle;
    private TextView mSpeedtestWanDlTitle;
    private TextView mSpeedtestWanUlTitle;
    private TextView mSpeedtestWifiSpeedTitle;
    private TextView mSpeedtestWifiEfficiencyTitle;

    private TextView[] mTitleTextViews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIsThemeLight = ColorUtils.isThemeLight(this);
        if (mIsThemeLight) {
            //Light
            setTheme(R.style.AppThemeLight);
            getWindow().getDecorView()
                    .setBackgroundColor(ContextCompat.getColor(this,
                            android.R.color.white));
        } else {
            //Default is Dark
            setTheme(R.style.AppThemeDark);
        }

        setContentView(R.layout.activity_speedtest);

        final Intent intent = getIntent();

        final String routerSelected =
                intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);
        if (isNullOrEmpty(routerSelected) ||
                (mRouter = RouterManagementActivity.getDao(this)
                        .getRouter(routerSelected)) == null) {
            Toast.makeText(
                    this, "Missing Router - might have been removed?",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        this.mMessageReceiver = new NetworkChangeReceiver();

        this.mHandler = new Handler();

        AdUtils.buildAndDisplayAdViewIfNeeded(this,
                (AdView) findViewById(R.id.router_speedtest_adView));

        mToolbar = (Toolbar) findViewById(R.id.routerSpeedTestToolbar);
        if (mToolbar != null) {
            mToolbar.setTitle("Speed Test");
            updateToolbarTitleAndSubTitle();

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

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        Router.doFetchAndSetRouterAvatarInImageView(this, mRouter,
                (ImageView) findViewById(R.id.speedtest_router_imageView));

        mSpeedtestLatencyTitle = (TextView) findViewById(R.id.speedtest_latency_title);
        mSpeedtestWanDlTitle = (TextView) findViewById(R.id.speedtest_wan_dl_title);
        mSpeedtestWanUlTitle = (TextView) findViewById(R.id.speedtest_wan_ul_title);
        mSpeedtestWifiSpeedTitle = (TextView) findViewById(R.id.speedtest_lan_title);
        mSpeedtestWifiEfficiencyTitle = (TextView) findViewById(R.id.speedtest_wifi_efficiency_title);

        mTitleTextViews = new TextView[] {
                mSpeedtestLatencyTitle,
                mSpeedtestWanDlTitle,
                mSpeedtestWanUlTitle,
                mSpeedtestWifiSpeedTitle,
                mSpeedtestWifiEfficiencyTitle};

        doPerformSpeedTest();
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            registerReceiver(
                    mMessageReceiver,
                    new IntentFilter(
                            ConnectivityManager.CONNECTIVITY_ACTION));
        } catch (final Exception e) {
            Utils.reportException(this, e);
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        try {
            unregisterReceiver(mMessageReceiver);
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            super.onStop();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(mMessageReceiver);
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            super.onDestroy();
        }
    }

    @Override
    public void onRefresh() {
        doPerformSpeedTest();
    }

    private void highlightTitleTextView(int viewIdx) {
        if (viewIdx < 0 || viewIdx >= mTitleTextViews.length) {
            return;
        }
        for (int i = 0; i < mTitleTextViews.length; i++) {
            final TextView textView = mTitleTextViews[i];
            textView.setTypeface(null,
                    i == viewIdx ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    private void highlightTitleTextView(@NonNull final TextView... tvs) {
        if (tvs == null) {
            //Reset everything
            for (final TextView textView : mTitleTextViews) {
                textView.setTypeface(null, Typeface.NORMAL);
            }
        } else {
            for (final TextView textView : mTitleTextViews) {
                for (final TextView tv : tvs) {
                    textView.setTypeface(null,
                            tv == textView ? Typeface.BOLD : Typeface.NORMAL);
                }
            }
        }
    }

    private void resetAllTitleViews() {
        for (final TextView textView : mTitleTextViews) {
            textView.setTypeface(null, Typeface.NORMAL);
        }
    }

    private void doPerformSpeedTest() {
        if (!mSwipeRefreshLayout.isRefreshing()) {
            mSwipeRefreshLayout.setRefreshing(true);
        }
        mSwipeRefreshLayout.setEnabled(false);
        setRefreshActionButtonState(true);

        final TextView errorPlaceholder =
                (TextView) findViewById(R.id.router_speedtest_error);
        errorPlaceholder.setVisibility(View.GONE);

        final TextView noticeTextView =
                (TextView) findViewById(R.id.router_speedtest_notice);

        final View internetRouterLink = findViewById(R.id.speedtest_internet_line);
        final View routerLanLink = findViewById(R.id.speedtest_router_lan_path_vertical);

        final int defaultColorForPaths = ContextCompat.getColor(SpeedTestActivity.this,
                R.color.network_link_color);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    //FIXME Using a static server for now,
                    // but shouldn't we select best server (smallest ping) from a list of servers
                    // (cf. SpeedTest XML API???)
                    noticeTextView
                            .setText("1/4 - Measuring Internet (WAN) Latency...");
                    noticeTextView.startAnimation(AnimationUtils.loadAnimation(SpeedTestActivity.this,
                            android.R.anim.slide_in_left));
                    noticeTextView.setVisibility(View.VISIBLE);

                    final int latencyColor = ColorUtils.getColor(NET_LATENCY);
                    internetRouterLink.setBackgroundColor(latencyColor);
                    highlightTitleTextView(mSpeedtestLatencyTitle);
                    //Display animation: RTT
                    //TODO Perform actual measurements

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Reset color
                            internetRouterLink.setBackgroundColor(defaultColorForPaths);

                            //2
                            noticeTextView.setText("2/4 - Testing Internet (WAN) Download Speed...");
                            final int wanDLColor = ColorUtils.getColor(NET_DL);
                            internetRouterLink.setBackgroundColor(wanDLColor);
                            highlightTitleTextView(mSpeedtestWanDlTitle);
                            //Display animation: DL: WAN -> Router
                            //TODO Perform actual measurements

                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    //Reset color
                                    internetRouterLink.setBackgroundColor(defaultColorForPaths);

                                    //3
                                    noticeTextView
                                            .setText("3/4 - Testing Internet (WAN) Upload Speed...");
                                    final int wanULColor = ColorUtils.getColor(NET_UL);
                                    internetRouterLink.setBackgroundColor(wanULColor);
                                    highlightTitleTextView(mSpeedtestWanUlTitle);
                                    //Display animation: UL: Router -> WAN
                                    //TODO Perform actual measurements

                                    mHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            //Reset color
                                            internetRouterLink.setBackgroundColor(defaultColorForPaths);

                                            //FIXME Perform this test only if device is connected to a local network provided by the Router
                                            //4
                                            noticeTextView
                                                    .setText("4/4 - Measuring WiFi Speed and Efficiency...");
                                            final int lanColor = ColorUtils.getColor(NET_WIFI);
                                            routerLanLink.setBackgroundColor(lanColor);
                                            highlightTitleTextView(mSpeedtestWifiSpeedTitle,
                                                    mSpeedtestWifiEfficiencyTitle);
                                            //Display animation: UL: Router -> WAN
                                            //TODO Perform actual measurements

                                            mHandler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {

                                                    routerLanLink.setBackgroundColor(defaultColorForPaths);

                                                    noticeTextView.startAnimation(AnimationUtils.loadAnimation(
                                                            SpeedTestActivity.this,
                                                            android.R.anim.slide_out_right));
                                                    noticeTextView.setVisibility(View.GONE);
                                                    resetAllTitleViews();
                                                    setRefreshActionButtonState(false);
                                                    mSwipeRefreshLayout.setEnabled(true);
                                                }
                                            }, 5789);
                                        }
                                    }, 7000);
                                }
                            }, 5000);
                        }
                    }, 3000);

                    //TODO Run actual tests and display notice info
                    //Do not block thread

//                    noticeTextView
//                            .setText("2/3 - Testing Internet (WAN) Upload (UL) Speed...");
                    //TODO Run actual tests and display notice info


//                    noticeTextView
//                            .setText("3/3 - Testing Link Speed between this device and the Router...");
                    //TODO Run actual tests and display notice info


                } catch (final Exception e) {
                    e.printStackTrace();
                    Utils.reportException(SpeedTestActivity.this,
                            new IllegalStateException(e));
                    final String rootCauseMessage = ExceptionUtils.getRootCauseMessage(e);
                    errorPlaceholder.setText(String.format("Error%s",
                            isNullOrEmpty(rootCauseMessage) ? "" :
                                    (": " + rootCauseMessage)));
                    if (!isNullOrEmpty(rootCauseMessage)) {
                        errorPlaceholder.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast.makeText(SpeedTestActivity.this,
                                        rootCauseMessage, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                    //No worries
                } finally {
//                    setRefreshActionButtonState(false);
//                    mSwipeRefreshLayout.setEnabled(true);
//                    noticeTextView.setVisibility(View.GONE);
                }
            }
        }, 1000);
    }

    public void setRefreshActionButtonState(final boolean refreshing) {
        mSwipeRefreshLayout.setRefreshing(refreshing);
        if (optionsMenu != null) {
            final MenuItem refreshItem = optionsMenu.findItem(R.id.router_speedtest_refresh);
            if (refreshItem != null) {
                if (refreshing) {
                    refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
                } else {
                    refreshItem.setActionView(null);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_speed_test, menu);
        this.optionsMenu = menu;

        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.router_speedtest_refresh:
                SnackbarUtils.buildSnackbar(this,
                        findViewById(android.R.id.content),
                        "Going to start Speed Test...",
                        "Undo",
                        Snackbar.LENGTH_SHORT,
                        this,
                        null,
                        true);
                return true;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onShowEvent(@Nullable Bundle bundle) throws Exception {

    }

    @Override
    public void onDismissEventSwipe(int event, @Nullable Bundle bundle) throws Exception {

    }

    @Override
    public void onDismissEventActionClick(int event, @Nullable Bundle bundle) throws Exception {

    }

    @Override
    public void onDismissEventTimeout(int event, @Nullable Bundle bundle) throws Exception {
        doPerformSpeedTest();
    }

    @Override
    public void onDismissEventManual(int event, @Nullable Bundle bundle) throws Exception {

    }

    @Override
    public void onDismissEventConsecutive(int event, @Nullable Bundle bundle) throws Exception {

    }

    public class NetworkChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {

            final NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (info != null && info.isConnected()) {
                updateToolbarTitleAndSubTitle();
            }
        }
    }

    private void updateToolbarTitleAndSubTitle() {
        final String effectiveRemoteAddr = Router.getEffectiveRemoteAddr(mRouter, SpeedTestActivity.this);
        final Integer effectivePort = Router.getEffectivePort(mRouter, SpeedTestActivity.this);

        if (mToolbar != null) {
            mToolbar.setSubtitle(
                    String.format("%s (%s:%d)",
                            mRouter.getDisplayName(),
                            effectiveRemoteAddr,
                            effectivePort));
        }
    }
}
