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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.supportv7.widget.decorator.DividerItemDecoration;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdView;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.io.Files;
import com.squareup.picasso.Callback;

import org.apache.commons.io.FileUtils;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.actions.AbstractRouterAction;
import org.rm3l.ddwrt.actions.PingFromRouterAction;
import org.rm3l.ddwrt.exceptions.SpeedTestException;
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
import java.util.UUID;

import de.keyboardsurfer.android.widget.crouton.Style;
import mbanje.kurt.fabbutton.FabButton;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.CHARSET;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.MAX_ROUTER_SPEEDTEST_RESULTS_FREE_VERSION;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.ROUTER_SPEED_TEST_DURATION_THRESHOLD_SECONDS;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.ROUTER_SPEED_TEST_DURATION_THRESHOLD_SECONDS_DEFAULT;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.ROUTER_SPEED_TEST_MAX_FILE_SIZE_KB;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.ROUTER_SPEED_TEST_MAX_FILE_SIZE_KB_DEFAULT;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.ROUTER_SPEED_TEST_SERVER;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.ROUTER_SPEED_TEST_SERVER_AUTO;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.ROUTER_SPEED_TEST_SERVER_RANDOM;

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

    public static final Splitter EQUAL_SPLITTER = Splitter
            .on("=").omitEmptyStrings().trimResults();
    public static final Splitter SLASH_SPLITTER = Splitter
            .on("/").omitEmptyStrings().trimResults();
    public static final String AUTO_DETECTED = "Auto-detected";
    public static final String RANDOM_SELECTED = "Random";

    public static final Random RANDOM = new Random();

    private boolean mIsThemeLight;
    private Router mOriginalRouter;
    private Router mRouterCopy;

    private Toolbar mToolbar;

    private BroadcastReceiver mMessageReceiver;

    private TextView mSpeedtestLatencyTitle;
    private TextView mSpeedtestWanDlTitle;
    private TextView mSpeedtestWanUlTitle;
    private TextView mSpeedtestWifiSpeedTitle;
    private TextView mSpeedtestWifiEfficiencyTitle;

    private TextView[] mTitleTextViews;

    private ImageView mServerCountryFlag;
    private TextView mServerLabel;

    private FabButton mRunFab;
    private FabButton mCancelFab;

//    private AtomicBoolean mSpeedTestRunning;
    
    private SharedPreferences mRouterPreferences;

    private SpeedTestAsyncTask mSpeedTestAsyncTask;

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

    private Long[] mPossibleFileSizes;
    private SharedPreferences mGlobalPreferences;
    private boolean mWithCurrentConnectionTesting;

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

        final String routerSelectedUuid =
                intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);
        if (isNullOrEmpty(routerSelectedUuid) ||
                (mOriginalRouter = RouterManagementActivity.getDao(this)
                        .getRouter(routerSelectedUuid)) == null) {
            Toast.makeText(
                    this, "Missing Router - might have been removed?",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

//        mSpeedTestRunning = new AtomicBoolean(false);
        
        //Establish a brand-new connection to the Router
        mRouterCopy = new Router(this, mOriginalRouter)
            .setUuid(UUID.randomUUID().toString());
        //In order for router avatar to be correctly fetched, we have to copy router model
        //from original preferences
        mRouterCopy.setRouterModel(Router.getRouterModel(this, mOriginalRouter));

        final String[] maxFileSizeValuesStrArr = getResources().getStringArray(
                R.array.routerSpeedTestMaxFileSize_values);
        if (maxFileSizeValuesStrArr == null) {
            mPossibleFileSizes = new Long[] {128L};
            Utils.reportException(this,
                    new SpeedTestException("R.array.routerSpeedTestMaxFileSize_values is NULL"));
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
                        this, "Internal error - thanks for reporting the issue.",
                        Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }
        if (mPossibleFileSizes.length == 0) {
            mPossibleFileSizes = new Long[] {128L};
            Utils.reportException(this,
                    new SpeedTestException("R.array.routerSpeedTestMaxFileSize_values is NULL or empty"));
        }
        Arrays.sort(mPossibleFileSizes);

        mRouterPreferences = getSharedPreferences(routerSelectedUuid, Context.MODE_PRIVATE);
        mGlobalPreferences =
                getSharedPreferences(DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY,
                        Context.MODE_PRIVATE);

        this.mMessageReceiver = new NetworkChangeReceiver();

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

        Router.doFetchAndSetRouterAvatarInImageView(this, mRouterCopy,
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
        mAdapter = new SpeedTestResultRecyclerViewAdapter(this, mOriginalRouter)
            .setSpeedTestResults(mDao.getSpeedTestResultsByRouter(mOriginalRouter.getUuid()));

        mRecyclerView.setAdapter(mAdapter);

        final RecyclerView.ItemDecoration itemDecoration =
                new DividerItemDecoration(this, LinearLayoutManager.VERTICAL);
        mRecyclerView.addItemDecoration(itemDecoration);

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

        errorPlaceholder = (TextView) findViewById(R.id.router_speedtest_error);
        errorPlaceholder.setVisibility(View.GONE);

        noticeTextView = (TextView) findViewById(R.id.router_speedtest_notice);

        internetRouterLink = findViewById(R.id.speedtest_internet_line);
        routerLanLink = findViewById(R.id.speedtest_router_lan_path_vertical);

        defaultColorForPaths = ContextCompat.getColor(SpeedTestActivity.this,
                R.color.network_link_color);

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
                                mDao.deleteAllSpeedTestResultsByRouter(mOriginalRouter.getUuid());

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
        
        
        mRunFab = (FabButton)
                findViewById(R.id.speedtest_run_action);
        mCancelFab = (FabButton)
                findViewById(R.id.speedtest_cancel_action);

        mCancelFab.setVisibility(View.GONE);
        mRunFab.setVisibility(View.VISIBLE);

        mRunFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
                    if (mDao.getSpeedTestResultsByRouter(mOriginalRouter.getUuid()).size()
                            >= MAX_ROUTER_SPEEDTEST_RESULTS_FREE_VERSION) {
                        Utils.displayUpgradeMessage(SpeedTestActivity.this,
                                "Save more SpeedTest runs");
                        return;
                    }
                }
                onRefresh();
            }
        });
        mCancelFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSpeedTestAsyncTask != null) {
                    mSpeedTestAsyncTask.cancelAction();
                } else {
                    mCancelFab.setVisibility(View.GONE);
                    mRunFab.setVisibility(View.VISIBLE);
                }
            }
        });

        final String mRouterPreferencesString = mRouterPreferences.getString(
                ROUTER_SPEED_TEST_SERVER, ROUTER_SPEED_TEST_SERVER_AUTO);
        if (ROUTER_SPEED_TEST_SERVER_AUTO.equals(mRouterPreferencesString)) {
            mServerLabel.setText(AUTO_DETECTED);
        } else if (ROUTER_SPEED_TEST_SERVER_RANDOM.equals(mRouterPreferencesString)) {
            mServerLabel.setText(RANDOM_SELECTED);
        } else {
            refreshSpeedTestParameters(mRouterPreferencesString);
        }
    }

    private void startOrCancelAction() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Run
                onRefresh();
            }
        });

//        Toast.makeText(SpeedTestActivity.this,
//                "[REMOVEME] onClick(mRunFab) - mSpeedTestRunning.get(): " + mSpeedTestRunning.get(),
//                Toast.LENGTH_LONG).show();
//        Crashlytics.log(Log.DEBUG, LOG_TAG,
//                "onClick(mRunFab) - mSpeedTestRunning.get(): " + mSpeedTestRunning.get());
//        if (mSpeedTestRunning.get()) {
//            //Cancel
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    errorPlaceholder.setVisibility(View.VISIBLE);
//                    errorPlaceholder.setText(" Stopping...");
//                }
//            });
//            if (mSpeedTestAsyncTask == null) {
//                mSpeedTestRunning.set(false);
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        errorPlaceholder.setText(" Aborted!");
//                        mRunFab.setIcon(
//                                R.drawable.ic_play_arrow_white_24dp,
//                                R.drawable.ic_play_arrow_white_24dp
//                        );
//                    }
//                });
//                return;
//            }
//            mSpeedTestAsyncTask.cancelAction();
//
//        } else {
//            onRefresh();
//        }
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

    protected void setSpeedTestResults(List<SpeedTestResult> results) {
        this.mAdapter.setSpeedTestResults(results);
    }

    protected void notifyDataSetChanged() {
        this.mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshSpeedTestResults();
    }

    protected void updateNbSpeedTestResults(List<SpeedTestResult> speedTestResultsByRouter) {
        final int size = speedTestResultsByRouter.size();
        final TextView nbResultsView = (TextView) findViewById(R.id.speedtest_results_nb_results);
        nbResultsView
                .setText(Integer.toString(size));
    }

    private void refreshSpeedTestResults() {
        final List<SpeedTestResult> speedTestResultsByRouter = mDao.getSpeedTestResultsByRouter(mOriginalRouter.getUuid());
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
                try {
                    mRouterCopy.destroyAllSessions();
                } finally {
                    if (mFileToShare != null) {
                        //noinspection ResultOfMethodCallIgnored
                        mFileToShare.delete();
                    }
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

    protected static void refreshServerLocationFlag(
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
    protected static String getServerLocationDisplayFromCountryCode(@NonNull final String serverCountryCode) {
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
    private static String getRemoteFileName(@Nullable  final Long fileSizeKB) {
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //        final String routerText;
                boolean doUpdateServerTextLabel = true;

                final String routerText = getServerLocationDisplayFromCountryCode(serverSetting);
                switch (nullToEmpty(routerText)) {
                    case RANDOM_SELECTED:
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
        });

    }

    private void doPerformSpeedTest() {

        mRunFab.setVisibility(View.GONE);
        mCancelFab.setVisibility(View.VISIBLE);

        setRefreshActionButtonState(true);

        refreshSpeedTestParameters(mRouterPreferences.getString(
                ROUTER_SPEED_TEST_SERVER, ROUTER_SPEED_TEST_SERVER_AUTO));

        mSpeedTestAsyncTask = new SpeedTestAsyncTask();

        mSpeedTestAsyncTask.execute();
    }

    public void setRefreshActionButtonState(final boolean refreshing) {
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

            final List<SpeedTestResult> speedTestResultsByRouter = mDao.getSpeedTestResultsByRouter(mOriginalRouter.getUuid());
            if (speedTestResultsByRouter.isEmpty()) {
                shareMenuItem.setEnabled(false);
            } else {
                shareMenuItem.setEnabled(true);
                mFileToShare = new File(getCacheDir(),
                        Utils.getEscapedFileName(String.format("Speed Test Results on Router '%s'",
                                mOriginalRouter.getCanonicalHumanReadableName())) + ".csv");

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

                        final String speedTestLine = String.format(Locale.US,
                                "%s,%s,%s,%.2f,%.2f ms,%.2f,%s%s,%.2f,%s%s",
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
                String.format("Speed Test Results for Router '%s'",
                        mOriginalRouter.getCanonicalHumanReadableName()));
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
                        mOriginalRouter.getUuid());
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
        onRefresh();
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
        final String effectiveRemoteAddr = Router.getEffectiveRemoteAddr(mOriginalRouter, SpeedTestActivity.this);
        final Integer effectivePort = Router.getEffectivePort(mOriginalRouter, SpeedTestActivity.this);

        if (mToolbar != null) {
            mToolbar.setSubtitle(
                    String.format(Locale.US, "%s (%s:%d)",
                            mOriginalRouter.getDisplayName(),
                            effectiveRemoteAddr,
                            effectivePort));
        }
    }

    class SpeedTestAsyncTask extends
            AsyncTask<Void, Integer, AbstractRouterAction.RouterActionResult<Void>> {

        private SpeedTestResult speedTestResult;

        private Date executionDate;

        private String pingServerCountry;

        private String server;

        public void cancelAction() {
            mRouterCopy.destroyAllSessions();
        }

        @Override
        protected AbstractRouterAction.RouterActionResult<Void> doInBackground(Void... params) {

            executionDate = new Date();

            Crashlytics.log(Log.DEBUG, LOG_TAG, "executionDate: " + executionDate);

            Exception exception = null;
            try {

//                if (mSpeedTestRunning.get()) {
//                    throw new SpeedTestException("Already Running");
//                }
//                mSpeedTestRunning.set(true);

                if (isCancelled()) {
                    throw new InterruptedException();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        resetEverything(false);
                        errorPlaceholder.setVisibility(View.GONE);
//                        mRunFab.setIcon(
//                                R.drawable.ic_close_white_24dp,
//                                R.drawable.ic_play_arrow_white_24dp
//                        );

                        mRunFab.setVisibility(View.GONE);
                        mCancelFab.setVisibility(View.VISIBLE);

                        mRunFab.setProgress(0);
                        mCancelFab.setProgress(0);

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
                    pingServerCountry =  serverSetting;
                    if (ROUTER_SPEED_TEST_SERVER_RANDOM.equals(serverSetting)) {
                        //Pick one randomly
                        final Set<String> rowKeySet = SERVERS.rowKeySet();
                        pingServerCountry = Lists.newArrayList(rowKeySet).get(
                                RANDOM.nextInt(rowKeySet.size()));
                    }
                    refreshSpeedTestParameters(pingServerCountry);
                    server = SERVERS.get(pingServerCountry, PING_SERVER);
                    wanSpeedUrl = SERVERS.get(pingServerCountry, HTTP_DL_URL);
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

                    final String remoteFileName = getRemoteFileName(possibleFileSize);
                    final int pgForFile = (pg++);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Display message to user
                            mCancelFab.setProgress((100 * 3/4) + pgForFile);
                            noticeTextView
                                    .setText("3/4 - Downloading data: " + remoteFileName + "...");
                            //final int netDlColor = ColorUtils.getColor(NET_DL);
                            //internetRouterLink.setBackgroundColor(netDlColor);
                            //highlightTitleTextView(mSpeedtestWanDlTitle);
                        }
                    });

                    final String[] cmdExecOutput = SSHUtils.getManualProperty(
                            SpeedTestActivity.this,
                            mRouterCopy,
                            getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY,
                                    Context.MODE_PRIVATE),
                            "DATE_START=$(/bin/date +\"%s\")", //seconds since 1970-01-01 00:00:00 UTC
                            String.format("/usr/bin/wget -qO /dev/null \"%s/%s?_=%s\" > /dev/null 2>&1 ",
                                    wanSpeedUrl,
                                    remoteFileName,
                                    System.currentTimeMillis()),
                            "DATE_END=$(/bin/date +\"%s\")", //seconds since 1970-01-01 00:00:00 UTC
                            "/bin/echo $((${DATE_END}-${DATE_START}))" //number of seconds
                    );

                    if (cmdExecOutput == null || cmdExecOutput.length == 0) {
                        final SpeedTestException speedTestException = new
                                SpeedTestException("Failed to download data: " + remoteFileName);
                        Crashlytics.logException(speedTestException);
                        throw speedTestException;
                    }

                    final long elapsedSeconds;
                    try {
                        elapsedSeconds = Long.parseLong(
                                nullToEmpty(cmdExecOutput[cmdExecOutput.length - 1])
                                        .trim());
                    } catch (final NumberFormatException nfe) {
                        Crashlytics.logException(nfe);
                        throw new SpeedTestException("Unexpected output - please try again later.");
                    }

                    if (elapsedSeconds < 0) {
                        throw new SpeedTestException("Unexpected output - please try again later.");
                    }

                    pairAcceptedForComputation = Pair.create(
                            possibleFileSize,
                            elapsedSeconds);
                    //Stop conditions: time_to_dl >= threshold or fileSize >= possibleFileSize
                    if (possibleFileSize >= userDefinedRouterSpeedTestMaxFileSizeKB
                            || elapsedSeconds >= userDefinedRouterSpeedTestDurationThresholdSeconds) {
                        break;
                    }
                }

                if (isCancelled()) {
                    throw new InterruptedException();
                }

                //3- WAN DL
                publishProgress(TEST_WAN_DL);
                if (pairAcceptedForComputation != null) {
                    final long timeElapsedSeconds = pairAcceptedForComputation.second;
                    final long wanDl = (timeElapsedSeconds != 0 ?
                            ((pairAcceptedForComputation.first * 1024) / timeElapsedSeconds) :
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
                speedTestResult.setWanUl(new Random().nextInt(27) * 1024^5);
                publishProgress(WAN_UL_MEASURED);

            } catch (Exception e) {
                Crashlytics.logException(e);
                exception = e;
            }

            return new AbstractRouterAction.RouterActionResult<>(null, exception);
        }

        @NonNull
        private PingRTT runPing(@NonNull final String server) throws Exception {
            Crashlytics.log(Log.DEBUG, LOG_TAG, "runPing: " + server);

            if (Strings.isNullOrEmpty(server)) {
                throw new IllegalArgumentException("No Server specified");
            }
            final String[] pingOutput = SSHUtils.getManualProperty(SpeedTestActivity.this, mRouterCopy, null,
                    String.format(Locale.US,
                            PingFromRouterAction.PING_CMD_TO_FORMAT + " | grep \"round-trip\"",
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
            Crashlytics.log(Log.DEBUG, LOG_TAG, "onPostExecute");

            if (voidRouterActionResult != null) {
                final Exception exception = voidRouterActionResult.getException();
//                mRunFab.setIcon(
//                        R.drawable.ic_play_arrow_white_24dp,
//                        R.drawable.ic_play_arrow_white_24dp
//                );
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
                                    mOriginalRouter.getUuid(),
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
                            mDao.getSpeedTestResultsByRouter(mOriginalRouter.getUuid());

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

            Crashlytics.log(Log.DEBUG, LOG_TAG, "progressCode: " + progressCode);

            if (progressCode == null) {
                return;
            }
            switch (progressCode) {
                case SELECT_SERVER:
                    mCancelFab.setProgress(100 * 1/4);
                    noticeTextView
                            .setText("1/4 - Selecting remote test Server...");
                    noticeTextView.startAnimation(AnimationUtils.loadAnimation(SpeedTestActivity.this,
                            android.R.anim.slide_in_left));
                    noticeTextView.setVisibility(View.VISIBLE);
                    break;

                case MEASURE_PING_LATENCY:
                    mCancelFab.setProgress(100 * 2/4);
                    noticeTextView
                            .setText("2/4 - Measuring Internet (WAN) Latency...");
                    final int latencyColor = ColorUtils.getColor(NET_LATENCY);
                    internetRouterLink.setBackgroundColor(latencyColor);
                    highlightTitleTextView(mSpeedtestLatencyTitle);
                    break;

                case TEST_WAN_DL:
                    mCancelFab.setProgress(100 * 3/4);
                    noticeTextView
                            .setText("3/4 - Measuring Internet (WAN) Download Speed...");
                    final int netDlColor = ColorUtils.getColor(NET_DL);
                    internetRouterLink.setBackgroundColor(netDlColor);
                    highlightTitleTextView(mSpeedtestWanDlTitle);
                    break;

                case TEST_WAN_UL:
                    mCancelFab.setProgress(100 * 4/4);
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
                    break;

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
            Crashlytics.log(Log.DEBUG, LOG_TAG, "onCancelled");
            errorPlaceholder.setText("Aborted");
            errorPlaceholder.setVisibility(View.VISIBLE);
            resetEverything(true);
        }

        private void resetEverything(final boolean enableSwipeRefresh) {
            Crashlytics.log(Log.DEBUG, LOG_TAG, "resetEverything(" + enableSwipeRefresh + ")");
            try {
                internetRouterLink.setBackgroundColor(defaultColorForPaths);
                routerLanLink.setBackgroundColor(defaultColorForPaths);

                noticeTextView.startAnimation(AnimationUtils.loadAnimation(
                        SpeedTestActivity.this,
                        android.R.anim.slide_out_right));
                noticeTextView.setVisibility(View.GONE);
                resetAllTitleViews();

                mRunFab.resetIcon();
                mCancelFab.resetIcon();
//                mRunFab.setIcon(
//                        R.drawable.ic_play_arrow_white_24dp,
//                        R.drawable.ic_play_arrow_white_24dp
//                );
                mCancelFab.setProgress(0);
                mRunFab.setProgress(0);

                mRunFab.setVisibility(View.VISIBLE);
                mCancelFab.setVisibility(View.GONE);

                setRefreshActionButtonState(!enableSwipeRefresh);
            } finally {
//                mSpeedTestRunning.set(false);
            }
        }
    }

}