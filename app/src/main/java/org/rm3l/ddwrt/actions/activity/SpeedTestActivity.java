package org.rm3l.ddwrt.actions.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
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

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdView;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.squareup.picasso.Callback;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.actions.AbstractRouterAction;
import org.rm3l.ddwrt.actions.PingFromRouterAction;
import org.rm3l.ddwrt.exceptions.DDWRTCompanionException;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.settings.RouterSpeedTestSettingsActivity;
import org.rm3l.ddwrt.utils.AdUtils;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.ImageUtils;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;
import org.rm3l.ddwrt.utils.snackbar.SnackbarCallback;
import org.rm3l.ddwrt.utils.snackbar.SnackbarUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.ROUTER_SPEED_TEST_SERVER;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.ROUTER_SPEED_TEST_SERVER_AUTO;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.ROUTER_SPEED_TEST_WITH_CURRENT_CONNECTION;

/**
 * Created by rm3l on 20/12/15.
 */
public class SpeedTestActivity extends AppCompatActivity
        implements SwipeRefreshLayout.OnRefreshListener, SnackbarCallback {

    private static final String LOG_TAG = SpeedTestActivity
            .class.getSimpleName();
    public static final String NET_LATENCY = "net_latency";
    public static final String NET_DL = "net_dl";
    public static final String NET_UL = "net_ul";
    public static final String NET_WIFI = "net_wifi";
    public static final int SELECT_SERVER = 1;
    private static final int MEASURE_PING_LATENCY = 2;
    private static final int PING_LATENCY_MEASURED = 3;
    public static final Splitter EQUAL_SPLITTER = Splitter.on("=").omitEmptyStrings().trimResults();
    public static final Splitter SLASH_SPLITTER = Splitter.on("/").omitEmptyStrings().trimResults();

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

    private View mServerContainer;
    private ImageView mServerCountryFlag;
    private TextView mServerLabel;

    private boolean mWithCurrentConnectionTesting;

    private FloatingActionButton mCancelFab;

    private SharedPreferences mRouterPreferences;

    private AsyncTask<Void, Integer, AbstractRouterAction.RouterActionResult<Void>>
            mSpeedTestAsyncTask;

    private static final Table<String, String, String> SERVERS = HashBasedTable.create();

    public static final String PING_SERVER = "PING_SERVER";

    public static final String HTTP_DL_URL = "HTTP_DL_URL";

    static {
        SERVERS.put("DE", PING_SERVER, "s3.eu-central-1.amazonaws.com");
        SERVERS.put("DE", HTTP_DL_URL, "https://s3.eu-central-1.amazonaws.com/speed-test--frankfurt");

        SERVERS.put("US", PING_SERVER, "s3-us-west-1.amazonaws.com");
        SERVERS.put("US", HTTP_DL_URL, "https://s3-us-west-1.amazonaws.com/speed-test--northern-california");

        SERVERS.put("BR", PING_SERVER, "s3-sa-east-1.amazonaws.com");
        SERVERS.put("BR", HTTP_DL_URL, "https://s3-sa-east-1.amazonaws.com/speed-test--sao-paulo");

        SERVERS.put("KR", PING_SERVER, "s3.ap-northeast-2.amazonaws.com");
        SERVERS.put("KR", HTTP_DL_URL, "https://s3.ap-northeast-2.amazonaws.com/speed-test--seoul");

        SERVERS.put("JP", PING_SERVER, "s3-ap-northeast-1.amazonaws.com");
        SERVERS.put("JP", HTTP_DL_URL, "https://s3-ap-northeast-1.amazonaws.com/speed-test--tokyo");

        SERVERS.put("AU", PING_SERVER, "s3-ap-southeast-2.amazonaws.com");
        SERVERS.put("AU", HTTP_DL_URL, "https://s3-ap-southeast-2.amazonaws.com/speed-test--sydney");
    }

    private TextView noticeTextView;
    private View internetRouterLink;
    private View routerLanLink;
    private int defaultColorForPaths;
    private TextView errorPlaceholder;

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

        mRouterPreferences = getSharedPreferences(routerSelected, Context.MODE_PRIVATE);

        this.mMessageReceiver = new NetworkChangeReceiver();

//        this.mHandler = new Handler();

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

        mServerContainer = findViewById(R.id.speedtest_server_container);
        mServerCountryFlag =
                (ImageView) findViewById(R.id.speedtest_server_country_flag);
        mServerLabel =
                (TextView) findViewById(R.id.speedtest_server);

        errorPlaceholder =
                (TextView) findViewById(R.id.router_speedtest_error);
        errorPlaceholder.setVisibility(View.GONE);

        noticeTextView =
                (TextView) findViewById(R.id.router_speedtest_notice);

        internetRouterLink = findViewById(R.id.speedtest_internet_line);
        routerLanLink = findViewById(R.id.speedtest_router_lan_path_vertical);

        defaultColorForPaths = ContextCompat.getColor(SpeedTestActivity.this,
                R.color.network_link_color);

        mCancelFab = (FloatingActionButton)
                findViewById(R.id.speedtest_cancel);
        mCancelFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSpeedTestAsyncTask != null && !mSpeedTestAsyncTask.isCancelled()) {
                    mSpeedTestAsyncTask.cancel(true);
                }
            }
        });

        doPerformSpeedTest();
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshSpeedTestParameters(mRouterPreferences.getString(
                ROUTER_SPEED_TEST_SERVER, ROUTER_SPEED_TEST_SERVER_AUTO));
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

    private void highlightTitleTextView(@Nullable final TextView... tvs) {
        if (tvs == null) {
            //Reset everything
            for (final TextView textView : mTitleTextViews) {
                textView.setTypeface(null, Typeface.NORMAL);
            }
        } else {
            for (final TextView tv : tvs) {
                if (tv == null) {
                    continue;
                }
                tv.setTypeface(null, Typeface.BOLD);
            }
            final List<TextView> textViewList = Arrays.asList(tvs);
            for (final TextView textView : mTitleTextViews) {
                if (textViewList.contains(textView)) {
                    continue;
                }
                textView.setTypeface(null, Typeface.NORMAL);
            }
        }
    }

    private void resetAllTitleViews() {
        for (final TextView textView : mTitleTextViews) {
            textView.setTypeface(null, Typeface.NORMAL);
        }
    }

    private void refreshServerLocationFlag(final String countryCode) {
        ImageUtils.downloadImageFromUrl(this,
                String.format("%s/%s.png",
                        DDWRTCompanionConstants.COUNTRY_API_SERVER_FLAG,
                        countryCode),
                mServerCountryFlag,
                null,
                null,
                new Callback() {
                    @Override
                    public void onSuccess() {
                        mServerCountryFlag.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onError() {
                        mServerCountryFlag.setVisibility(View.GONE);
                    }
                });
    }

    private void refreshSpeedTestParameters(final String serverSetting) {
        final String routerText;
        boolean doUpdateServerTextLabel = true;
        switch (serverSetting) {
            case ROUTER_SPEED_TEST_SERVER_AUTO:
                routerText = "Auto-detected";
                final String serverLabelStr = mServerLabel.getText().toString();
                if (!(isNullOrEmpty(serverLabelStr)
                    || routerText.equals(serverLabelStr))) {
                    doUpdateServerTextLabel = false;
                }
                break;
            case "DE":
                routerText = "Frankfurt (Germany)";
                break;
            case "US":
                routerText = "California (USA)";
                break;
            case "BR":
                routerText = "Sao Paulo (Brazil)";
                break;
            case "KR":
                routerText = "Seoul (South Korea)";
                break;
            case "JP":
                routerText = "Tokyo (Japan)";
                break;
            case "AU":
                routerText = "Sydney (Australia)";
                break;
            default:
                routerText = serverSetting;
                break;
        }
        if (doUpdateServerTextLabel) {
            mServerLabel.setText(routerText);
        }
        if (!ROUTER_SPEED_TEST_SERVER_AUTO.equals(serverSetting)) {
            //Load flag in the background
            refreshServerLocationFlag(serverSetting);
        } else {
            mServerCountryFlag.setVisibility(View.GONE);
        }

        mWithCurrentConnectionTesting =
                mRouterPreferences.getBoolean(ROUTER_SPEED_TEST_WITH_CURRENT_CONNECTION, true);

        final View devices = findViewById(R.id.speedtest_connection_devices);
        final View connectionLink = findViewById(R.id.speedtest_connection_link);

        if (mWithCurrentConnectionTesting) {
            connectionLink.setVisibility(View.VISIBLE);
            devices.setVisibility(View.VISIBLE);
        } else {
            connectionLink.setVisibility(View.GONE);
            devices.setVisibility(View.GONE);
        }
    }

    private void doPerformSpeedTest() {
        if (!mSwipeRefreshLayout.isRefreshing()) {
            mSwipeRefreshLayout.setRefreshing(true);
        }
        mSwipeRefreshLayout.setEnabled(false);
        setRefreshActionButtonState(true);

        final String serverSetting = mRouterPreferences.getString(
                ROUTER_SPEED_TEST_SERVER, ROUTER_SPEED_TEST_SERVER_AUTO);

        refreshSpeedTestParameters(serverSetting);

        if (mSpeedTestAsyncTask != null && !mSpeedTestAsyncTask.isCancelled()) {
            mSpeedTestAsyncTask.cancel(true);
        }

        mSpeedTestAsyncTask = new AsyncTask<Void, Integer, AbstractRouterAction.RouterActionResult<Void>>() {

            private PingRTT wanLatencyResults;

            @Override
            protected AbstractRouterAction.RouterActionResult<Void> doInBackground(Void... params) {

                Exception exception = null;
                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mCancelFab.setVisibility(View.VISIBLE);
                            resetEverything(false);
                            findViewById(R.id.speedtest_latency_pb_internet)
                                    .setVisibility(View.VISIBLE);
                            findViewById(R.id.speedtest_dl_pb_internet)
                                    .setVisibility(View.VISIBLE);
                            findViewById(R.id.speedtest_ul_pb_internet)
                                    .setVisibility(View.VISIBLE);
                            findViewById(R.id.speedtest_pb_wifi)
                                    .setVisibility(View.VISIBLE);
                            findViewById(R.id.speedtest_pb_wifi_efficiency)
                                    .setVisibility(View.VISIBLE);
                        }
                    });

                    //1- Determine if we need to select the closest server
                    publishProgress(SELECT_SERVER);
                    final String serverSetting = mRouterPreferences.getString(
                            ROUTER_SPEED_TEST_SERVER, ROUTER_SPEED_TEST_SERVER_AUTO);

                    String server = null;
                    if (ROUTER_SPEED_TEST_SERVER_AUTO.equals(serverSetting)) {
                        // Iterate over each server to determine the closest one,
                        // in terms of ping latency
                        float minLatency = Float.MAX_VALUE;
                        String serverCountry = null;
                        for (final Map.Entry<String, Map<String, String>> entry :
                                SERVERS.rowMap().entrySet()) {
                            final String country = entry.getKey();
                            final Map<String, String> value = entry.getValue();
                            final String pingServer = value.get(PING_SERVER);
                            if (isNullOrEmpty(pingServer)) {
                                continue;
                            }
                            final PingRTT pingRTT;
                            try {
                                pingRTT = runPing(pingServer);
                            } catch (final Exception e) {
                                Crashlytics.logException(e);
                                continue;
                            }
                            final float avg = pingRTT.getAvg();
                            if (avg < 0) {
                                continue;
                            }
                            if (avg <= minLatency) {
                                minLatency = avg;
                                server = pingServer;
                                serverCountry = country;
                            }
                        }
                        final String pingServerCountry = serverCountry;
                        if (!isNullOrEmpty(pingServerCountry)) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    refreshSpeedTestParameters(pingServerCountry);
                                }
                            });
                        }

                    } else {
                        server = SERVERS.get(serverSetting, PING_SERVER);
                    }
                    if (isNullOrEmpty(server)) {
                        throw new SpeedTestException("Invalid server");
                    }

                    //2- Now measure ping latency
                    publishProgress(MEASURE_PING_LATENCY);
                    wanLatencyResults = runPing(server);
                    publishProgress(PING_LATENCY_MEASURED);

                    //TODO - To Be Continued


                } catch (Exception e) {
                    Crashlytics.logException(e);
                    exception = e;
                }

                return new AbstractRouterAction.RouterActionResult<>(null, exception);
            }

            @NonNull
            private PingRTT runPing(@NonNull final String server) throws Exception {
                if (Strings.isNullOrEmpty(server)) {
                    throw new IllegalArgumentException("No Server specified");
                }
                final String[] pingOutput = SSHUtils.getManualProperty(SpeedTestActivity.this, mRouter, null,
                        String.format(PingFromRouterAction.PING_CMD_TO_FORMAT,
                                PingFromRouterAction.MAX_PING_PACKETS_TO_SEND, server) + " | grep \"round-trip\"");
                if (pingOutput == null || pingOutput.length < 1) {
                    //Nothing - abort right now with an error message
                    throw new SpeedTestException("Unable to contact remote server");
                }
                final String pingRttOutput = pingOutput[0];
                final List<String> pingRttOutputList = EQUAL_SPLITTER
                        .splitToList(pingRttOutput);
                if (pingRttOutputList.size() < 2) {
                    throw new SpeedTestException("Unable to contact remote server");
                }
                final String pingRtt = pingRttOutputList.get(1)
                        .replaceAll("ms","")
                        .trim();
                final List<String> pingRttSplitResult = SLASH_SPLITTER
                        .splitToList(pingRtt);
                final PingRTT pingRTT = new PingRTT();
                final int size = pingRttSplitResult.size();
                if (size >= 1) {
                    pingRTT.setMin(Float.parseFloat(pingRttSplitResult.get(0)));
                }
                if (size >= 2) {
                    pingRTT.setAvg(Float.parseFloat(pingRttSplitResult.get(1)));
                }
                if (size >= 3) {
                    pingRTT.setMax(Float.parseFloat(pingRttSplitResult.get(2)));
                }
                return pingRTT;
            }

            @Override
            protected void onPostExecute(AbstractRouterAction.RouterActionResult<Void> voidRouterActionResult) {
                if (voidRouterActionResult != null) {
                    final Exception exception = voidRouterActionResult.getException();
                    if (exception != null) {
                        errorPlaceholder.setVisibility(View.VISIBLE);
                        errorPlaceholder.setText("Error: " +
                                ExceptionUtils.getRootCauseMessage(exception));
                    } else {
                        errorPlaceholder.setVisibility(View.GONE);
                    }
                } else {
                    errorPlaceholder.setVisibility(View.GONE);
                }
                resetEverything(true);
                super.onPostExecute(voidRouterActionResult);
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                //Runs on main thread
                if (values == null) {
                    return;
                }
                final Integer progressCode = values[0];
                if (progressCode == null) {
                    return;
                }
                switch (progressCode) {
                    case SELECT_SERVER:
                        noticeTextView
                                .setText("1/4 - Selecting remote Internet Server...");
                        noticeTextView.startAnimation(AnimationUtils.loadAnimation(SpeedTestActivity.this,
                                android.R.anim.slide_in_left));
                        noticeTextView.setVisibility(View.VISIBLE);
                        break;

                    case MEASURE_PING_LATENCY:
                        noticeTextView
                                .setText("2/4 - Measuring Internet (WAN) Latency...");
                        final int latencyColor = ColorUtils.getColor(NET_LATENCY);
                        internetRouterLink.setBackgroundColor(latencyColor);
                        highlightTitleTextView(mSpeedtestLatencyTitle);
                        break;

                    case PING_LATENCY_MEASURED:
                        //Display results
                        if (wanLatencyResults != null) {
                            findViewById(R.id.speedtest_latency_pb_internet)
                                    .setVisibility(View.GONE);
                            final TextView wanLatencyTextView = (TextView) findViewById(R.id.speedtest_internet_latency);
                            wanLatencyTextView.setVisibility(View.VISIBLE);
                            wanLatencyTextView
                                    .setText(String.format("%.2f ms", wanLatencyResults.getAvg()));
                        }


                    default:
                        break;
                }
                super.onProgressUpdate(values);
            }

            @Override
            protected void onCancelled(AbstractRouterAction.RouterActionResult<Void> voidRouterActionResult) {
                errorPlaceholder.setText("Cancelled");
                errorPlaceholder.setVisibility(View.VISIBLE);
                resetEverything(true);
            }

            private void resetEverything(final boolean enableSwipeRefresh) {
                internetRouterLink.setBackgroundColor(defaultColorForPaths);
                routerLanLink.setBackgroundColor(defaultColorForPaths);

                noticeTextView.startAnimation(AnimationUtils.loadAnimation(
                        SpeedTestActivity.this,
                        android.R.anim.slide_out_right));
                noticeTextView.setVisibility(View.GONE);
                resetAllTitleViews();

                mCancelFab.setVisibility(View.GONE);

                setRefreshActionButtonState(!enableSwipeRefresh);

                mSwipeRefreshLayout.setEnabled(enableSwipeRefresh);
            }
        };

        mSpeedTestAsyncTask.execute();



//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                try {
//
//                    mCancelFab.setVisibility(View.VISIBLE);
//
//                    //FIXME Using a static server for now,
//                    // but shouldn't we select best server (smallest ping) from a list of servers
//                    // (cf. SpeedTest XML API???)
//                    noticeTextView
//                            .setText("1/4 - Measuring Internet (WAN) Latency...");
//                    noticeTextView.startAnimation(AnimationUtils.loadAnimation(SpeedTestActivity.this,
//                            android.R.anim.slide_in_left));
//                    noticeTextView.setVisibility(View.VISIBLE);
//
//                    final int latencyColor = ColorUtils.getColor(NET_LATENCY);
//                    internetRouterLink.setBackgroundColor(latencyColor);
//                    highlightTitleTextView(mSpeedtestLatencyTitle);
//                    //Display animation: RTT
//                    //TODO Perform actual measurements
//
//                    mHandler.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            //Reset color
//                            internetRouterLink.setBackgroundColor(defaultColorForPaths);
//
//                            //2
//                            noticeTextView.setText("2/4 - Testing Internet (WAN) Download Speed...");
//                            final int wanDLColor = ColorUtils.getColor(NET_DL);
//                            internetRouterLink.setBackgroundColor(wanDLColor);
//                            highlightTitleTextView(mSpeedtestWanDlTitle);
//                            //Display animation: DL: WAN -> Router
//                            //TODO Perform actual measurements
//
//                            mHandler.postDelayed(new Runnable() {
//                                @Override
//                                public void run() {
//                                    //Reset color
//                                    internetRouterLink.setBackgroundColor(defaultColorForPaths);
//
//                                    //3
//                                    noticeTextView
//                                            .setText("3/4 - Testing Internet (WAN) Upload Speed...");
//                                    final int wanULColor = ColorUtils.getColor(NET_UL);
//                                    internetRouterLink.setBackgroundColor(wanULColor);
//                                    highlightTitleTextView(mSpeedtestWanUlTitle);
//                                    //Display animation: UL: Router -> WAN
//                                    //TODO Perform actual measurements
//
//                                    mHandler.postDelayed(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            //Reset color
//                                            internetRouterLink.setBackgroundColor(defaultColorForPaths);
//
//                                            //FIXME Perform this test only if device is connected to a local network provided by the Router
//                                            //4
//                                            noticeTextView
//                                                    .setText("4/4 - Measuring Connection Speed...");
//                                            final int lanColor = ColorUtils.getColor(NET_WIFI);
//                                            routerLanLink.setBackgroundColor(lanColor);
//                                            highlightTitleTextView(mSpeedtestWifiSpeedTitle,
//                                                    mSpeedtestWifiEfficiencyTitle);
//                                            //Display animation: UL: Router -> WAN
//                                            //TODO Perform actual measurements
//
//                                            mHandler.postDelayed(new Runnable() {
//                                                @Override
//                                                public void run() {
//
//                                                    routerLanLink.setBackgroundColor(defaultColorForPaths);
//
//                                                    noticeTextView.startAnimation(AnimationUtils.loadAnimation(
//                                                            SpeedTestActivity.this,
//                                                            android.R.anim.slide_out_right));
//                                                    noticeTextView.setVisibility(View.GONE);
//                                                    resetAllTitleViews();
//
//                                                    mCancelFab.setVisibility(View.GONE);
//
//                                                    setRefreshActionButtonState(false);
//
//                                                    mSwipeRefreshLayout.setEnabled(true);
//
//                                                }
//                                            }, 5789);
//                                        }
//                                    }, 7000);
//                                }
//                            }, 5000);
//                        }
//                    }, 3000);
//
//                    //TODO Run actual tests and display notice info
//                    //Do not block thread
//
////                    noticeTextView
////                            .setText("2/3 - Testing Internet (WAN) Upload (UL) Speed...");
//                    //TODO Run actual tests and display notice info
//
//
////                    noticeTextView
////                            .setText("3/3 - Testing Link Speed between this device and the Router...");
//                    //TODO Run actual tests and display notice info
//
//
//                } catch (final Exception e) {
//                    e.printStackTrace();
//                    Utils.reportException(SpeedTestActivity.this,
//                            new IllegalStateException(e));
//                    final String rootCauseMessage = ExceptionUtils.getRootCauseMessage(e);
//                    errorPlaceholder.setText(String.format("Error%s",
//                            isNullOrEmpty(rootCauseMessage) ? "" :
//                                    (": " + rootCauseMessage)));
//                    if (!isNullOrEmpty(rootCauseMessage)) {
//                        errorPlaceholder.setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                Toast.makeText(SpeedTestActivity.this,
//                                        rootCauseMessage, Toast.LENGTH_LONG).show();
//                            }
//                        });
//                    }
//                    //No worries
//                } finally {
////                    setRefreshActionButtonState(false);
////                    mSwipeRefreshLayout.setEnabled(true);
////                    noticeTextView.setVisibility(View.GONE);
//                }
//            }
//        }, 1000);
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
                        "Going to start Speed Test...",
                        "Undo",
                        Snackbar.LENGTH_SHORT,
                        this,
                        null,
                        true);
                return true;

            case R.id.router_speedtest_settings:
                //Open Settings activity
                final Intent settingsActivity = new Intent(this,
                        RouterSpeedTestSettingsActivity.class);
                settingsActivity.putExtra(RouterManagementActivity.ROUTER_SELECTED,
                        mRouter.getUuid());
                this.startActivity(settingsActivity);
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

    public static class SpeedTestException extends DDWRTCompanionException {
        public SpeedTestException() {
        }

        public SpeedTestException(@Nullable String detailMessage) {
            super(detailMessage);
        }

        public SpeedTestException(@Nullable String detailMessage, @Nullable Throwable throwable) {
            super(detailMessage, throwable);
        }

        public SpeedTestException(@Nullable Throwable throwable) {
            super(throwable);
        }
    }
}
