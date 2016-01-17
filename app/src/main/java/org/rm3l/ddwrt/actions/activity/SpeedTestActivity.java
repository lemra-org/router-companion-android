package org.rm3l.ddwrt.actions.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.util.Pair;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdView;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.io.Files;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.squareup.picasso.Callback;

import org.apache.commons.io.FileUtils;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.actions.AbstractRouterAction;
import org.rm3l.ddwrt.actions.PingFromRouterAction;
import org.rm3l.ddwrt.exceptions.DDWRTCompanionException;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.resources.SpeedTestResult;
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
import org.rm3l.ddwrt.widgets.RecyclerViewEmptySupport;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import de.keyboardsurfer.android.widget.crouton.Style;
import mbanje.kurt.fabbutton.FabButton;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.CHARSET;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.ROUTER_SPEED_TEST_DURATION_THRESHOLD_SECONDS;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.ROUTER_SPEED_TEST_DURATION_THRESHOLD_SECONDS_DEFAULT;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.ROUTER_SPEED_TEST_MAX_FILE_SIZE_KB;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.ROUTER_SPEED_TEST_MAX_FILE_SIZE_KB_DEFAULT;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.ROUTER_SPEED_TEST_SERVER;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.ROUTER_SPEED_TEST_SERVER_AUTO;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.ROUTER_SPEED_TEST_SERVER_RANDOM;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.THEMING_PREF;

/**
 * Created by rm3l on 20/12/15.
 */
public class SpeedTestActivity extends AppCompatActivity
        implements SwipeRefreshLayout.OnRefreshListener, SnackbarCallback {

    private static final String LOG_TAG = SpeedTestActivity
            .class.getSimpleName();
            
    public static final String PER_SEC = "/s";
            
    public static final String NET_LATENCY = "net_latency";
    public static final String NET_DL = "net_dl";
    public static final String NET_UL = "net_ul";
    public static final String NET_WIFI = "net_wifi";

    public static final int SELECT_SERVER = 1;
    private static final int MEASURE_PING_LATENCY = 2;
    private static final int PING_LATENCY_MEASURED = 21;
    private static final int TEST_WAN_DL = 4;
    private static final int WAN_DL_MEASURED = 41;
    private static final int TEST_WAN_UL = 5;
    private static final int WAN_UL_MEASURED = 51;

    public static final Splitter EQUAL_SPLITTER = Splitter.on("=").omitEmptyStrings().trimResults();
    public static final Splitter SLASH_SPLITTER = Splitter.on("/").omitEmptyStrings().trimResults();
    public static final String AUTO_DETECTED = "Auto-detected";
    public static final String RANDOM_SELECTED = "Random";
    public static final Random RANDOM = new Random();

    private boolean mIsThemeLight;
    private Router mRouter;

    private Toolbar mToolbar;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private BroadcastReceiver mMessageReceiver;

    private TextView mSpeedtestLatencyTitle;
    private TextView mSpeedtestWanDlTitle;
    private TextView mSpeedtestWanUlTitle;
    private TextView mSpeedtestWifiSpeedTitle;
    private TextView mSpeedtestWifiEfficiencyTitle;

    private TextView[] mTitleTextViews;

    private ImageView mServerCountryFlag;
    private TextView mServerLabel;

    private boolean mWithCurrentConnectionTesting;

    private FabButton mRunCancelFab;

    private AtomicBoolean mSpeedTestRunning = new AtomicBoolean(false);

    private SharedPreferences mRouterPreferences;

    private AsyncTask<Void, Integer, AbstractRouterAction.RouterActionResult<Void>>
            mSpeedTestAsyncTask;

    private RecyclerViewEmptySupport mRecyclerView;
    private SpeedTestResultRecyclerViewAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private static final Table<String, String, String> SERVERS = HashBasedTable.create();

    public static final String PING_SERVER = "PING_SERVER";

    public static final String HTTP_DL_URL = "HTTP_DL_URL";

    static {
        /*
            Public Access for S3 requires the following bucket policy:

            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Sid":"AddPerm",
                  "Effect":"Allow",
                  "Principal": "*",
                  "Action":["s3:GetObject"],
                  "Resource":["arn:aws:s3:::speed-test--frankfurt/*"]
                }
              ]
            }
         */

        SERVERS.put("DE", PING_SERVER, "s3.eu-central-1.amazonaws.com");
        SERVERS.put("DE", HTTP_DL_URL, "http://speed-test--frankfurt.s3-website.eu-central-1.amazonaws.com");

        SERVERS.put("US", PING_SERVER, "s3-us-west-1.amazonaws.com");
        SERVERS.put("US", HTTP_DL_URL, "http://speed-test--northern-california.s3-website-us-west-1.amazonaws.com");

        SERVERS.put("BR", PING_SERVER, "s3-sa-east-1.amazonaws.com");
        SERVERS.put("BR", HTTP_DL_URL, "http://speed-test--sao-paulo.s3-website-sa-east-1.amazonaws.com");

        SERVERS.put("KR", PING_SERVER, "s3.ap-northeast-2.amazonaws.com");
        SERVERS.put("KR", HTTP_DL_URL, "http://speed-test--seoul.s3-website.ap-northeast-2.amazonaws.com");

        SERVERS.put("JP", PING_SERVER, "s3-ap-northeast-1.amazonaws.com");
        SERVERS.put("JP", HTTP_DL_URL, "http://speed-test--tokyo.s3-website-ap-northeast-1.amazonaws.com");

        SERVERS.put("AU", PING_SERVER, "s3-ap-southeast-2.amazonaws.com");
        SERVERS.put("AU", HTTP_DL_URL, "http://speed-test--sydney.s3-website-ap-southeast-2.amazonaws.com");
    }

    private TextView noticeTextView;
    private View internetRouterLink;
    private View routerLanLink;
    private int defaultColorForPaths;
    private TextView errorPlaceholder;

    private DDWRTCompanionDAO mDao;

    private ShareActionProvider mShareActionProvider;
    private Menu optionsMenu;
    private File mFileToShare;

    private SlidingUpPanelLayout mSlidingUpPanelLayout;

    private Long[] mPossibleFileSizes;

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

        mSlidingUpPanelLayout =
                (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);

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

        final String[] maxFileSizeValuesStrArr = getResources().getStringArray(
                R.array.routerSpeedTestMaxFileSize_values);
        if (maxFileSizeValuesStrArr == null) {
            mPossibleFileSizes = new Long[] {128L};
        } else {
            mPossibleFileSizes = new Long[maxFileSizeValuesStrArr.length];
            int i = 0;
            try {
                for (final String maxFileSizeValuesStr : maxFileSizeValuesStrArr) {
                    mPossibleFileSizes[i++] = Long.parseLong(maxFileSizeValuesStr);
                }
            } catch (final NumberFormatException nfe) {
                nfe.printStackTrace();
                Utils.reportException(this, nfe);
                Toast.makeText(
                        this, "Internal Error - thanks for reporting the issue.",
                        Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }
        Arrays.sort(mPossibleFileSizes);

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

        mDao = RouterManagementActivity.getDao(this);

        mRecyclerView = (RecyclerViewEmptySupport) findViewById(R.id.speedtest_results_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        // allows for optimizations if all items are of the same size:
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.scrollToPosition(0);
        mRecyclerView.setLayoutManager(mLayoutManager);

        final TextView emptyView = (TextView) findViewById(R.id.empty_view);
        if (mIsThemeLight) {
            emptyView.setTextColor(ContextCompat.getColor(this, R.color.black));
        } else {
            emptyView.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
        mRecyclerView.setEmptyView(emptyView);

        // specify an adapter (see also next example)
        mAdapter = new SpeedTestResultRecyclerViewAdapter(this, mRouter)
            .setSpeedTestResults(mDao.getSpeedTestResultsByRouter(mRouter.getUuid()));
        mRecyclerView.setAdapter(mAdapter);

//        final RecyclerView.ItemDecoration itemDecoration =
//                new DividerItemDecoration(this, LinearLayoutManager.VERTICAL);
//        mRecyclerView.addItemDecoration(itemDecoration);

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

        mRunCancelFab = (FabButton)
                findViewById(R.id.speedtest_run_cancel);
        mRunCancelFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Crashlytics.log(Log.DEBUG, LOG_TAG, "onClick(mRunCancelFab)");
                if (mSpeedTestRunning.get()) {
                    //Running - cancel task
                    //Add stopping line
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            errorPlaceholder.setVisibility(View.VISIBLE);
                            errorPlaceholder.setText(" Stopping...");
                        }
                    });
                    mSpeedTestAsyncTask.cancel(true);
                    Toast.makeText(SpeedTestActivity.this,
                            "Stopping...", Toast.LENGTH_SHORT)
                        .show();
                } else {
                    //Not running - do run
                    onRefresh();
                }
            }
        });

        final ImageButton speedtestResultsRefreshImageButton = (ImageButton)
                findViewById(R.id.speedtest_results_refresh);
        final ImageButton speedtestResultsClearAllImageButton = (ImageButton)
                findViewById(R.id.speedtest_results_clear_all);

        if (mIsThemeLight) {
            speedtestResultsRefreshImageButton.setImageDrawable(ContextCompat.getDrawable(this,
                    R.drawable.ic_refresh_black_24dp));
            speedtestResultsClearAllImageButton.setImageDrawable(ContextCompat.getDrawable(this,
                    R.drawable.ic_clear_all_black_24dp));
        } else {
            speedtestResultsRefreshImageButton.setImageDrawable(ContextCompat.getDrawable(this,
                    R.drawable.ic_refresh_white_24dp));
            speedtestResultsClearAllImageButton.setImageDrawable(ContextCompat.getDrawable(this,
                    R.drawable.ic_clear_all_white_24dp));
        }

        speedtestResultsRefreshImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshSpeedTestResults();
            }
        });

        //Permission requests
        final int rwExternalStoragePermissionCheck = ContextCompat
                .checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (rwExternalStoragePermissionCheck != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat
                    .shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                SnackbarUtils.buildSnackbar(this,
                        "Storage access is required to share Speed Test results.",
                        "OK",
                        Snackbar.LENGTH_INDEFINITE,
                        new SnackbarCallback() {
                            @Override
                            public void onShowEvent(@Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventSwipe(int event, @Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventActionClick(int event, @Nullable Bundle bundle) throws Exception {
                                //Request permission
                                ActivityCompat.requestPermissions(SpeedTestActivity.this,
                                        new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        DDWRTCompanionConstants.Permissions.STORAGE);
                            }

                            @Override
                            public void onDismissEventTimeout(int event, @Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventManual(int event, @Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventConsecutive(int event, @Nullable Bundle bundle) throws Exception {

                            }
                        },
                        null,
                        true);
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        DDWRTCompanionConstants.Permissions.STORAGE);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        speedtestResultsClearAllImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(SpeedTestActivity.this)
                        .setIcon(R.drawable.ic_action_alert_warning)
                        .setTitle("Delete All Results?")
                        .setMessage("You'll lose all Speed Test records!")
                        .setCancelable(true)
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialogInterface, final int i) {
                                mDao.deleteAllSpeedTestResultsByRouter(mRouter.getUuid());

                                refreshSpeedTestResults();

                                //Request Backup
                                Utils.requestBackup(SpeedTestActivity.this);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Cancelled - nothing more to do!
                            }
                        }).create().show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        switch (requestCode) {
            case DDWRTCompanionConstants.Permissions.STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Yay! Permission granted for #" + requestCode);
                    if (optionsMenu != null) {
                        final MenuItem menuItem = optionsMenu.findItem(R.id.tile_status_wireless_iface_qrcode_share);
                        menuItem.setEnabled(true);
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Crashlytics.log(Log.WARN, LOG_TAG, "Boo! Permission denied for #" + requestCode);
                    Utils.displayMessage(this,
                            "Sharing of SpeedTest Results will be unavailable",
                            Style.INFO);
                    if (optionsMenu != null) {
                        final MenuItem menuItem = optionsMenu.findItem(R.id.tile_status_wireless_iface_qrcode_share);
                        menuItem.setEnabled(false);
                    }
                }
                return;
            }
            default:
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshSpeedTestResults();
    }

    private void updateNbSpeedTestResults(List<SpeedTestResult> speedTestResultsByRouter) {
        final int size = speedTestResultsByRouter.size();
        final TextView nbResultsView = (TextView) findViewById(R.id.speedtest_results_nb_results);
        nbResultsView
                .setText(Integer.toString(size));
    }

    private void refreshSpeedTestResults() {
        final List<SpeedTestResult> speedTestResultsByRouter = mDao.getSpeedTestResultsByRouter(mRouter.getUuid());
        mAdapter.setSpeedTestResults(speedTestResultsByRouter);
        mAdapter.notifyDataSetChanged();
        updateNbSpeedTestResults(speedTestResultsByRouter);
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
            try {
                if (mFileToShare != null) {
                    //noinspection ResultOfMethodCallIgnored
                    mFileToShare.delete();
                }
            } catch (final Exception e) {
                Crashlytics.logException(e);
            } finally {
                super.onDestroy();
            }
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

    private static void refreshServerLocationFlag(
            @NonNull final Context ctx, @NonNull final String countryCode,
                                           @NonNull final ImageView imageView) {
        ImageUtils.downloadImageFromUrl(ctx,
                String.format("%s/%s.png",
                        DDWRTCompanionConstants.COUNTRY_API_SERVER_FLAG,
                        countryCode),
                imageView,
                null,
                null,
                new Callback() {
                    @Override
                    public void onSuccess() {
                        imageView.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onError() {
                        imageView.setVisibility(View.GONE);
                    }
                });
    }

    private void refreshServerLocationFlag(final String countryCode) {
        refreshServerLocationFlag(this, countryCode, mServerCountryFlag);
    }

    @NonNull
    private static String getServerLocationDisplayFromCountryCode(@NonNull final String serverCountryCode) {
        switch (nullToEmpty(serverCountryCode)) {
            case ROUTER_SPEED_TEST_SERVER_AUTO:
                return AUTO_DETECTED;
            case ROUTER_SPEED_TEST_SERVER_RANDOM:
                //Pick one randomly
                final Set<String> rowKeySet = SERVERS.rowKeySet();
                final String randomCountryCode = Lists.newArrayList(rowKeySet).get(
                        RANDOM.nextInt(rowKeySet.size()));
                if (ROUTER_SPEED_TEST_SERVER_RANDOM.equals(randomCountryCode)) {
                    //Should not happen, but hey, ou never know!
                    return "DE";
                }
                return getServerLocationDisplayFromCountryCode(randomCountryCode);
            case "DE":
                return "Frankfurt (Germany)";
            case "US":
                return "California (USA)";
            case "BR":
                return "Sao Paulo (Brazil)";
            case "KR":
                return "Seoul (South Korea)";
            case "JP":
                return "Tokyo (Japan)";
            case "AU":
                return "Sydney (Australia)";
            default:
                return isNullOrEmpty(serverCountryCode) ?
                        "-" : serverCountryCode;
        }
    }

    @Nullable
    private static String getRemoteFileExtension(@Nullable  final Long fileSizeKB) {
        if (fileSizeKB == null) {
            return null;
        }
        switch (fileSizeKB.intValue()) {
            case 128:
                return "128KB";
            case 256:
                return "256KB";
            case 512:
                return "512KB";
            case 1024:
                return "1MB";
            case 2048:
                return "2MB";
            case 4096:
                return "4MB";
            case 8192:
                return "8MB";
            case 16384:
                return "16MB";
            case 32768:
                return "32MB";
            case 65536:
                return "64MB";
            case 131072:
                return "128MB";
            case 262144:
                return "256MB";
            case 524288:
                return "512MB";
            case 1048576:
                return "1GB";
            default:
                return null;
        }
    }

    private void refreshSpeedTestParameters(@NonNull final String serverSetting) {
//        final String routerText;
        boolean doUpdateServerTextLabel = true;

        final String routerText = getServerLocationDisplayFromCountryCode(serverSetting);
        switch (nullToEmpty(routerText)) {
            case AUTO_DETECTED:
                final String serverLabelStr = mServerLabel.getText().toString();
                if (!(isNullOrEmpty(serverLabelStr)
                        || routerText.equals(serverLabelStr))) {
                    doUpdateServerTextLabel = false;
                }
                break;
            default:
                break;
        }

        if (doUpdateServerTextLabel) {
            mServerLabel.setText(routerText);
        }
        if (!AUTO_DETECTED.equalsIgnoreCase(routerText)) {
            //Load flag in the background
            refreshServerLocationFlag(serverSetting);
        } else {
            mServerCountryFlag.setVisibility(View.GONE);
        }

        //TODO Disabled for now
//        mWithCurrentConnectionTesting =
//                mRouterPreferences.getBoolean(ROUTER_SPEED_TEST_WITH_CURRENT_CONNECTION, true);
        mWithCurrentConnectionTesting = false;

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

        mSpeedTestAsyncTask = new AsyncTask<Void, Integer, AbstractRouterAction.RouterActionResult<Void>>() {

            private SpeedTestResult speedTestResult;

            private Date executionDate;

            private String pingServerCountry;

            private String server;

            @Override
            protected AbstractRouterAction.RouterActionResult<Void> doInBackground(Void... params) {

                executionDate = new Date();
                Exception exception = null;
                try {
                    if (isCancelled()) {
                        throw new InterruptedException();
                    }
                    mSpeedTestRunning.set(true);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            resetEverything(false);
                            errorPlaceholder.setVisibility(View.GONE);
                            mRunCancelFab.setIcon(
                                    R.drawable.ic_close_white_24dp,
                                    R.drawable.ic_play_arrow_white_24dp
                            );
                            mRunCancelFab.setProgress(0);
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

                    String wanSpeedUrl = null;

                    pingServerCountry = serverSetting;
                    if (ROUTER_SPEED_TEST_SERVER_AUTO.equals(serverSetting)) {
                        // Iterate over each server to determine the closest one,
                        // in terms of ping latency
                        float minLatency = Float.MAX_VALUE;
                        String serverCountry = null;
                        for (final Map.Entry<String, Map<String, String>> entry :
                                SERVERS.rowMap().entrySet()) {
                            if (isCancelled()) {
                                throw new InterruptedException();
                            }
                            final String country = entry.getKey();
                            final Map<String, String> value = entry.getValue();
                            final String pingServer = value.get(PING_SERVER);
                            wanSpeedUrl = value.get(HTTP_DL_URL);
                            if (isNullOrEmpty(pingServer)) {
                                continue;
                            }
                            final PingRTT pingRTT = runPing(pingServer);
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
                        pingServerCountry = serverCountry;
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
                        wanSpeedUrl = SERVERS.get(serverSetting, HTTP_DL_URL);
                    }
                    if (isNullOrEmpty(server) || isNullOrEmpty(wanSpeedUrl)) {
                        throw new SpeedTestException("Invalid server");
                    }

                    speedTestResult = new SpeedTestResult();

                    //2- Now measure ping latency
                    publishProgress(MEASURE_PING_LATENCY);
                    final PingRTT wanLatencyResults = runPing(server);
                    speedTestResult.setWanPingRTT(wanLatencyResults);
                    publishProgress(PING_LATENCY_MEASURED);

                    //WAN DL / UL: algorithm here: https://speedof.me/howitworks.html
                    final long userDefinedRouterSpeedTestMaxFileSizeKB =
                            mRouterPreferences.getLong(
                                    ROUTER_SPEED_TEST_MAX_FILE_SIZE_KB, ROUTER_SPEED_TEST_MAX_FILE_SIZE_KB_DEFAULT);
                    final long userDefinedRouterSpeedTestDurationThresholdSeconds =
                            Long.parseLong(mRouterPreferences.getString(ROUTER_SPEED_TEST_DURATION_THRESHOLD_SECONDS,
                                ROUTER_SPEED_TEST_DURATION_THRESHOLD_SECONDS_DEFAULT));

                    Pair<Long, Long> pairAcceptedForComputation = null;

                    Crashlytics.log(Log.DEBUG, LOG_TAG,
                            "mPossibleFileSizes: " + Arrays.toString(mPossibleFileSizes));

                    int pg = 2;
                    for (final Long possibleFileSize : mPossibleFileSizes) {
                        if (isCancelled()) {
                            throw new InterruptedException();
                        }
                        //Measure time to download file of the specified type
                        /*
                        String.format("echo -e \"" +
                                                        "GET / HTTP/1.1\\r\\n" +
                                                        "Host:%s\\r\\n" +
                                                        "User-Agent:%s/%s\\r\\n\" " +
                                                        "| /usr/bin/nc %s %d"
                         */
                        //"wget -qO /dev/null http://cachefly.cachefly.net/10mb.test?id="+System.currentTimeMillis()
                        
                        final String remoteFileName = getRemoteFileExtension(possibleFileSize);
                        final int pgForFile = (pg++);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //Display message to user
                                mRunCancelFab.setProgress((100 * 3/4) + pgForFile);
                                noticeTextView
                                        .setText("3/4 - Downloading " + remoteFileName + " data...");
                                //final int netDlColor = ColorUtils.getColor(NET_DL);
                                //internetRouterLink.setBackgroundColor(netDlColor);
                                //highlightTitleTextView(mSpeedtestWanDlTitle);
                            }
                        });

                        final long t0 = System.nanoTime();
                        final String[] cmdExecOutput = SSHUtils.getManualProperty(SpeedTestActivity.this,
                                mRouter,
                                getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY,
                                        Context.MODE_PRIVATE),
                                String.format("/usr/bin/wget -qO /dev/null %s/%s?id=%s 2>&1 ",
                                        wanSpeedUrl,
                                        remoteFileName,
                                        Long.toString(t0)),
                                "/bin/echo $?"
                        );
                        final long t1 = System.nanoTime();

                        if (cmdExecOutput == null || cmdExecOutput.length == 0) {
                            final SpeedTestException speedTestException = new
                                    SpeedTestException("Failed to download " + remoteFileName + " data");
                            Crashlytics.logException(speedTestException);
                            throw speedTestException;
                        }
                        final String exitStatus = nullToEmpty(cmdExecOutput[cmdExecOutput.length - 1]).trim();
                        if (!"0".equals(exitStatus)) {
                            final SpeedTestException speedTestException = new
                                    SpeedTestException("Failed to download " + remoteFileName + " data: unexpected command exit status");
                            Crashlytics.log(Log.DEBUG, LOG_TAG, Arrays.toString(cmdExecOutput));
                            Crashlytics.logException(speedTestException);
                            throw speedTestException;
                        }

                        final long elapsed = t1 - t0;
                        pairAcceptedForComputation = Pair.create(
                                possibleFileSize,
                                elapsed);
                        //Stop conditions: time_to_dl >= threshold or
                        // fileSize >= possibleFileSize
                        if (possibleFileSize >= userDefinedRouterSpeedTestMaxFileSizeKB
                            || TimeUnit.NANOSECONDS.toMillis(elapsed) >=
                                TimeUnit.SECONDS.toMillis(userDefinedRouterSpeedTestDurationThresholdSeconds)) {
                            break;
                        }
                    }

                    if (isCancelled()) {
                        throw new InterruptedException();
                    }

                    //3- WAN DL
                    publishProgress(TEST_WAN_DL);
                    if (pairAcceptedForComputation != null) {
                        final long timeElapsed = TimeUnit.NANOSECONDS.toSeconds(pairAcceptedForComputation.second);
                        final long wanDl = (timeElapsed != 0 ?
                                ((pairAcceptedForComputation.first * 1024) / timeElapsed) :
                                (pairAcceptedForComputation.first * 1024));
                        speedTestResult.setWanDl(wanDl);
                    }
                    publishProgress(WAN_DL_MEASURED);

                    if (isCancelled()) {
                        throw new InterruptedException();
                    }

                    //3- WAN UL
                    publishProgress(TEST_WAN_UL);
                    //TODO //FIXME Use real data
                    speedTestResult.setWanUl(new Random().nextInt(27) * 1024^4);
                    publishProgress(WAN_UL_MEASURED);

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
                        String.format(PingFromRouterAction.PING_CMD_TO_FORMAT + " | grep \"round-trip\"",
                                PingFromRouterAction.MAX_PING_PACKETS_TO_SEND, server));
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
                    mRunCancelFab.setIcon(
                            R.drawable.ic_play_arrow_white_24dp,
                            R.drawable.ic_play_arrow_white_24dp
                    );
                    if (exception != null) {
                        errorPlaceholder.setVisibility(View.VISIBLE);
                        final Pair<String, String> exceptionPair = Utils.handleException(exception);
                        errorPlaceholder.setText("Error: " + exceptionPair.first);
                        if (!isNullOrEmpty(exceptionPair.second)) {
                            errorPlaceholder.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Toast.makeText(SpeedTestActivity.this, exceptionPair.second,
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        //Persist speed test result
                        mDao.insertSpeedTestResult(
                                new SpeedTestResult(
                                        mRouter.getUuid(),
                                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                                                Locale.US)
                                            .format(executionDate),
                                        server,
                                        speedTestResult.getWanPing(),
                                        speedTestResult.getWanDl(),
                                        speedTestResult.getWanUl(),
                                        null,
                                        null,
                                        null,
                                        pingServerCountry));

                        //Request Backup
                        Utils.requestBackup(SpeedTestActivity.this);

                        final List<SpeedTestResult> speedTestResultsByRouter =
                                mDao.getSpeedTestResultsByRouter(mRouter.getUuid());

                        updateNbSpeedTestResults(speedTestResultsByRouter);
                        mAdapter.setSpeedTestResults(speedTestResultsByRouter);
                        mAdapter.notifyItemInserted(0);
                        //Scroll to top
                        mLayoutManager.scrollToPosition(0);

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
                        mRunCancelFab.setProgress(100 * 1/4);
                        noticeTextView
                                .setText("1/4 - Selecting remote test Server...");
                        noticeTextView.startAnimation(AnimationUtils.loadAnimation(SpeedTestActivity.this,
                                android.R.anim.slide_in_left));
                        noticeTextView.setVisibility(View.VISIBLE);
                        break;

                    case MEASURE_PING_LATENCY:
                        mRunCancelFab.setProgress(100 * 2/4);
                        noticeTextView
                                .setText("2/4 - Measuring Internet (WAN) Latency...");
                        final int latencyColor = ColorUtils.getColor(NET_LATENCY);
                        internetRouterLink.setBackgroundColor(latencyColor);
                        highlightTitleTextView(mSpeedtestLatencyTitle);
                        break;

                    case TEST_WAN_DL:
                        mRunCancelFab.setProgress(100 * 3/4);
                        noticeTextView
                                .setText("3/4 - Measuring Internet (WAN) Download Speed...");
                        final int netDlColor = ColorUtils.getColor(NET_DL);
                        internetRouterLink.setBackgroundColor(netDlColor);
                        highlightTitleTextView(mSpeedtestWanDlTitle);
                        break;

                    case TEST_WAN_UL:
                        mRunCancelFab.setProgress(100 * 4/4);
                        noticeTextView
                                .setText("4/4 - Measuring Internet (WAN) Upload Speed...");
                        final int netUlColor = ColorUtils.getColor(NET_UL);
                        internetRouterLink.setBackgroundColor(netUlColor);
                        highlightTitleTextView(mSpeedtestWanUlTitle);
                        break;

                    case PING_LATENCY_MEASURED:
                        //Display results
                        //noinspection ConstantConditions
                        findViewById(R.id.speedtest_latency_pb_internet)
                                .setVisibility(View.GONE);
                        final TextView wanLatencyTextView =
                                (TextView) findViewById(R.id.speedtest_internet_latency);
                        wanLatencyTextView.setVisibility(View.VISIBLE);
                        if (speedTestResult != null
                                && speedTestResult.getWanPing() != null) {
                            wanLatencyTextView
                                    .setText(String.format("%.2f ms",
                                            speedTestResult.getWanPing().floatValue()));
                        } else {
                            wanLatencyTextView.setText("-");
                        }

                    case WAN_DL_MEASURED:
                        //noinspection ConstantConditions
                        findViewById(R.id.speedtest_dl_pb_internet)
                                .setVisibility(View.GONE);
                        final TextView wanDlTextView =
                                (TextView) findViewById(R.id.speedtest_internet_dl_speed);
                        wanDlTextView.setVisibility(View.VISIBLE);
                        if (speedTestResult != null
                                && speedTestResult.getWanDl() != null) {
                            wanDlTextView
                                    .setText(String.format("%s%s",
                                            FileUtils.byteCountToDisplaySize(
                                                    speedTestResult.getWanDl().longValue()),
                                            PER_SEC));
                        } else {
                            wanDlTextView.setText("-");
                        }
                        break;

                    case WAN_UL_MEASURED:
                        //noinspection ConstantConditions
                        findViewById(R.id.speedtest_ul_pb_internet)
                                .setVisibility(View.GONE);
                        final TextView wanUlTextView =
                                (TextView) findViewById(R.id.speedtest_internet_ul_speed);
                        wanUlTextView.setVisibility(View.VISIBLE);
                        if (speedTestResult != null
                                && speedTestResult.getWanUl() != null) {
                            wanUlTextView
                                    .setText(String.format("%s%s",
                                            FileUtils.byteCountToDisplaySize(
                                                    speedTestResult.getWanUl().longValue()),
                                            PER_SEC));
                        } else {
                            wanUlTextView.setText("-");
                        }
                        break;

                    default:
                        break;
                }
                super.onProgressUpdate(values);
            }

            @Override
            protected void onCancelled(AbstractRouterAction.RouterActionResult<Void> voidRouterActionResult) {
                errorPlaceholder.setText("Aborted");
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

                mRunCancelFab.resetIcon();
                mRunCancelFab.setIcon(
                        R.drawable.ic_play_arrow_white_24dp,
                        R.drawable.ic_play_arrow_white_24dp
                );
                mRunCancelFab.setProgress(0);

                setRefreshActionButtonState(!enableSwipeRefresh);

                mSwipeRefreshLayout.setEnabled(enableSwipeRefresh);

                mSpeedTestRunning.set(false);

            }
        };

        mSpeedTestAsyncTask.execute();

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

        //Permission requests
        final int rwExternalStoragePermissionCheck = ContextCompat
                .checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (rwExternalStoragePermissionCheck != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat
                    .shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                SnackbarUtils.buildSnackbar(this,
                        "Storage access is required to share Speed Test Results.",
                        "OK",
                        Snackbar.LENGTH_INDEFINITE,
                        new SnackbarCallback() {
                            @Override
                            public void onShowEvent(@Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventSwipe(int event, @Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventActionClick(int event, @Nullable Bundle bundle) throws Exception {
                                //Request permission
                                ActivityCompat.requestPermissions(SpeedTestActivity.this,
                                        new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        DDWRTCompanionConstants.Permissions.STORAGE);
                            }

                            @Override
                            public void onDismissEventTimeout(int event, @Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventManual(int event, @Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventConsecutive(int event, @Nullable Bundle bundle) throws Exception {

                            }
                        },
                        null,
                        true);
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        DDWRTCompanionConstants.Permissions.STORAGE);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        /* Getting the actionprovider associated with the menu item whose id is share */
        final MenuItem shareMenuItem = menu.findItem(R.id.router_speedtest_share);

        mShareActionProvider = (ShareActionProvider)
                MenuItemCompat.getActionProvider(shareMenuItem);
        if (mShareActionProvider == null) {
            mShareActionProvider = new ShareActionProvider(this);
            MenuItemCompat.setActionProvider(shareMenuItem, mShareActionProvider);
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            shareMenuItem.setEnabled(false);
        } else {

            final List<SpeedTestResult> speedTestResultsByRouter = mDao.getSpeedTestResultsByRouter(mRouter.getUuid());
            if (speedTestResultsByRouter.isEmpty()) {
                shareMenuItem.setEnabled(false);
            } else {
                shareMenuItem.setEnabled(true);
                mFileToShare = new File(getCacheDir(),
                        Utils.getEscapedFileName(String.format("Speed Test Results on Router '%s'",
                                mRouter.getCanonicalHumanReadableName())) + ".csv");

                final List<String> csvTextOutput = new ArrayList<>();

                try {
                    final String hdr = "Test Date,Server,Server Location,WAN Ping,WAN Ping (Readable),WAN Download,WAN Download (Readable),WAN Upload,WAN Upload (Readable)";
                    csvTextOutput.add(hdr);
                    Files.write(hdr + "\n",
                            mFileToShare,
                            CHARSET);
                    for (final SpeedTestResult speedTestResult : speedTestResultsByRouter) {
                        if (speedTestResult == null) {
                            continue;
                        }
                        final Number wanPing = speedTestResult.getWanPing();
                        final Number wanDl = speedTestResult.getWanDl();
                        final Number wanUl = speedTestResult.getWanUl();

                        final String speedTestLine = String.format("%s,%s,%s,%.2f,%.2f ms,%.2f,%s%s,%.2f,%s%s",
                                speedTestResult.getDate(),
                                speedTestResult.getServer(),
                                speedTestResult.getServerCountryCode(),
                                wanPing.floatValue(),
                                wanPing.floatValue(),
                                wanDl.floatValue(),
                                FileUtils.byteCountToDisplaySize(wanDl.longValue()),
                                PER_SEC,
                                wanUl.floatValue(),
                                FileUtils.byteCountToDisplaySize(wanUl.longValue()),
                                PER_SEC);

                        csvTextOutput.add(speedTestLine);
                        Files.append(speedTestLine + "\n",
                                mFileToShare, CHARSET);
                    }

                    setShareFile(csvTextOutput, mFileToShare);

                } catch (IOException e) {
                    Crashlytics.logException(e);
                    Utils.displayMessage(this, "Failed to export file - sharing will be unavailable. Please try again later", Style.ALERT);
                    shareMenuItem.setEnabled(false);
                    return super.onCreateOptionsMenu(menu);
                }
            }
        }

        return super.onCreateOptionsMenu(menu);

    }

    // Call to update the share intent
    private void setShareIntent(Intent shareIntent) {
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    private void setShareFile(Collection<String> csvTextOutput, File file) {
        if (mShareActionProvider == null) {
            return;
        }

        final Uri uriForFile = FileProvider
                .getUriForFile(this, DDWRTCompanionConstants.FILEPROVIDER_AUTHORITY, file);

        mShareActionProvider.setOnShareTargetSelectedListener(new ShareActionProvider.OnShareTargetSelectedListener() {
            @Override
            public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
                grantUriPermission(intent.getComponent().getPackageName(),
                        uriForFile, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                return true;
            }
        });

        final Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);
        sendIntent.setType("text/html");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT,
                String.format("Speed Test Results for Router '%s'", mRouter.getDisplayName()));
        sendIntent.putExtra(Intent.EXTRA_TEXT,
                Html.fromHtml(String.format("%s\n\n%s",
                        Joiner.on("\n").skipNulls().join(csvTextOutput),
                        Utils.getShareIntentFooter())
                        .replaceAll("\n", "<br/>")));

        sendIntent.setData(uriForFile);
//        sendIntent.setType("image/png");
        sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        setShareIntent(sendIntent);
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

    static class SpeedTestResultRecyclerViewAdapter
        extends RecyclerView.Adapter<SpeedTestResultRecyclerViewAdapter.ViewHolder> {

        private final SpeedTestActivity activity;
        private List<SpeedTestResult> speedTestResults;
        private final Router mRouter;

        public SpeedTestResultRecyclerViewAdapter(final SpeedTestActivity activity,
                                                  final Router mRouter) {
            this.activity = activity;
            this.mRouter = mRouter;
        }

        public List<SpeedTestResult> getSpeedTestResults() {
            return speedTestResults;
        }

        public SpeedTestResultRecyclerViewAdapter setSpeedTestResults(List<SpeedTestResult> speedTestResults) {
            this.speedTestResults = speedTestResults;
            return this;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.speed_test_result_list_layout, parent, false);
            // set the view's size, margins, paddings and layout parameters
            // ...
            final long currentTheme = activity
                    .getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
                    .getLong(THEMING_PREF, DDWRTCompanionConstants.DEFAULT_THEME);
            final CardView cardView = (CardView) v.findViewById(R.id.speed_test_result_item_cardview);
            final ImageButton deleteImageButton = (ImageButton) cardView.findViewById(R.id.speedtest_result_delete);
            if (currentTheme == ColorUtils.LIGHT_THEME) {
                //Light
                cardView.setCardBackgroundColor(ContextCompat
                        .getColor(activity, R.color.cardview_light_background));

                deleteImageButton
                        .setImageDrawable(ContextCompat.getDrawable(activity,
                                R.drawable.ic_delete_black_24dp));
            } else {
                //Default is Dark
                cardView.setCardBackgroundColor(ContextCompat
                        .getColor(activity, R.color.cardview_dark_background));
                deleteImageButton
                        .setImageDrawable(ContextCompat.getDrawable(activity,
                                R.drawable.ic_delete_white_24dp));
            }

//        return new ViewHolder(this.context,
//                RippleViewCreator.addRippleToView(v));
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            if (position < 0 || position >= speedTestResults.size()) {
                Utils.reportException(null, new IllegalStateException());
                Toast.makeText(activity,
                        "Internal Error. Please try again later",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            final SpeedTestResult speedTestResult = speedTestResults.get(position);
            if (speedTestResult == null) {
                return;
            }

            final boolean isThemeLight = ColorUtils.isThemeLight(activity);

            final View containerView = holder.containerView;

            containerView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final View placeholderView = containerView
                            .findViewById(R.id.speed_test_result_details_placeholder);
                    if (placeholderView.getVisibility() == View.VISIBLE) {
                        placeholderView.setVisibility(View.GONE);
                    } else {
                        placeholderView.setVisibility(View.VISIBLE);
                    }
                }
            });

            final TextView testDateView =
                    (TextView) containerView.findViewById(R.id.speed_test_result_test_date);
            String speedTestResultDate = speedTestResult.getDate();
            if (!isNullOrEmpty(speedTestResultDate)) {
                speedTestResultDate = speedTestResultDate.replaceAll(" ", "\n");
            }
            testDateView.setText(speedTestResultDate);

            ((TextView) containerView.findViewById(R.id.speed_test_result_detail_test_date))
                    .setText(speedTestResult.getDate());

            final String serverCountryCode = speedTestResult.getServerCountryCode();
            final ImageView imageView = (ImageView) containerView.findViewById(R.id.speed_test_result_server_country_flag);
            if (serverCountryCode == null || serverCountryCode.isEmpty()) {
                imageView.setVisibility(View.GONE);
            } else {
                refreshServerLocationFlag(activity,
                        serverCountryCode,
                        imageView);
            }

            final TextView wanPingView =
                    (TextView) containerView.findViewById(R.id.speed_test_result_wanPing);
            wanPingView.setCompoundDrawablesWithIntrinsicBounds(
                    0, isThemeLight ? R.drawable.ic_settings_ethernet_black_24dp : R.drawable.ic_settings_ethernet_white_24dp,
                    0, 0
            );
            final Number ping = speedTestResult.getWanPing();
            wanPingView.setText(String.format("%.2f\nms", ping.floatValue()));

            final TextView wanDlView =
                    (TextView) containerView.findViewById(R.id.speed_test_result_wanDl);
            wanDlView.setCompoundDrawablesWithIntrinsicBounds(
                    0, isThemeLight ? R.drawable.ic_file_download_black_24dp : R.drawable.ic_file_download_white_24dp,
                    0, 0
            );
            final String wanDlByteCountDisplaySize = (FileUtils.byteCountToDisplaySize(speedTestResult.getWanDl().longValue()) + PER_SEC);
            final String wanDl = wanDlByteCountDisplaySize.replaceAll(" ", "\n");
            wanDlView.setText(wanDl);

            final TextView wanUlView =
                    (TextView) containerView.findViewById(R.id.speed_test_result_wanUl);
            wanUlView.setCompoundDrawablesWithIntrinsicBounds(
                    0, isThemeLight ? R.drawable.ic_file_upload_black_24dp : R.drawable.ic_file_upload_white_24dp,
                    0, 0
            );
            final String wanUlByteCountToDisplaySize = (FileUtils.byteCountToDisplaySize(speedTestResult.getWanUl().longValue()) + PER_SEC);
            final String wanUl = wanUlByteCountToDisplaySize.replaceAll(" ", "\n");
            wanUlView.setText(wanUl);

            ((TextView) containerView.findViewById(R.id.speed_test_result_details_server))
                    .setText(speedTestResult.getServer());

            final TextView serverLocationView = (TextView) containerView.findViewById(R.id.speed_test_result_details_server_location);
            if (isNullOrEmpty(serverCountryCode)) {
                serverLocationView.setText("-");
            } else {
                serverLocationView
                        .setText(getServerLocationDisplayFromCountryCode(serverCountryCode));
            }

            ((TextView) containerView.findViewById(R.id.speed_test_result_details_wanPing))
                    .setText(String.format("%.2f ms", ping.floatValue()));
            ((TextView) containerView.findViewById(R.id.speed_test_result_details_wanDownload))
                    .setText(wanDlByteCountDisplaySize);
            ((TextView) containerView.findViewById(R.id.speed_test_result_details_wanUpload))
                    .setText(wanUlByteCountToDisplaySize);

            containerView.findViewById(R.id.speedtest_result_delete)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new AlertDialog.Builder(activity)
                                    .setIcon(R.drawable.ic_action_alert_warning)
                                    .setTitle("Delete Speed Test Result?")
                                    .setMessage("You'll lose this record!")
                                    .setCancelable(true)
                                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(final DialogInterface dialogInterface, final int i) {
                                            final String mRouterUuid = mRouter.getUuid();

                                            activity.mDao.deleteSpeedTestResultByRouterById(mRouterUuid,
                                                    speedTestResult.getId());

                                            final List<SpeedTestResult> speedTestResultsByRouter = activity.mDao.getSpeedTestResultsByRouter(mRouterUuid);
                                            activity.mAdapter.setSpeedTestResults(
                                                    speedTestResultsByRouter
                                            );
                                            activity.mAdapter.notifyDataSetChanged();

                                            activity.updateNbSpeedTestResults(speedTestResultsByRouter);

                                            //Request Backup
                                            Utils.requestBackup(activity);
                                        }
                                    })
                                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            //Cancelled - nothing more to do!
                                        }
                                    }).create().show();
                        }
                    });

        }

        @Override
        public int getItemCount() {
            return speedTestResults != null ?
                    speedTestResults.size() : 0;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            private final View itemView;

            final View containerView;

            public ViewHolder(View itemView) {
                super(itemView);
                this.itemView = itemView;
                containerView = itemView.findViewById(R.id.speed_test_result_container);

            }
        }
    }

}