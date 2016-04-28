package org.rm3l.ddwrt.feedback;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.gson.GsonBuilder;

import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.feedback.api.DoorbellService;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.ReportingUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DOORBELL_APIKEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DOORBELL_APPID;

/**
 * Created by rm3l on 24/04/16.
 */
public class FeedbackActivity extends AppCompatActivity {

    private static final String LOG_TAG = FeedbackActivity.class.getSimpleName();

    public static final String SCREENSHOT_FILE = "SCREENSHOT_FILE";
    public static final String CALLER_ACTIVITY = "CALLER_ACTIVITY";
    public static final String FEEDBACK_API_BASE_URL = "https://doorbell.io/api/";

    private static final GsonBuilder GSON_BUILDER = new GsonBuilder();

    private boolean mIsThemeLight;

    private Bitmap mBitmap;

    private DDWRTCompanionDAO mDao;
    private Menu optionsMenu;
    private Router mRouter;

    private TextInputLayout emailInputLayout;
    private EditText email;

    private TextInputLayout contentInputLayout;
    private EditText content;

    private CheckBox includeScreenshotAndLogs;

    private ImageButton screenshotThumb;
    private ImageView screenshotExpanded;

    private EditText routerInfo;

    // Hold a reference to the current animator,
    // so that it can be canceled mid-way.
    private Animator mCurrentAnimator;

    // The system "short" animation time duration, in milliseconds. This
    // duration is ideal for subtle animations or animations that occur
    // very frequently.
    private int mShortAnimationDuration;
    private DoorbellService mDoorbellService;
    private String screenshotFilePath;

    private static final String PROPERTY_MODEL = "Model";
    private static final String PROPERTY_ANDROID_VERSION = "Android Version";
    private static final String PROPERTY_WI_FI_ENABLED = "WiFi enabled";
    private static final String PROPERTY_MOBILE_DATA_ENABLED = "Mobile Data enabled";
    private static final String PROPERTY_GPS_ENABLED = "GPS enabled";
    private static final String PROPERTY_SCREEN_RESOLUTION = "Screen Resolution";
    private static final String PROPERTY_CALLER_ACTIVITY = "CAller Activity";
    private static final String PROPERTY_APP_VERSION_NAME = "App Version Name";
    private static final String PROPERTY_APP_VERSION_CODE = "App Version Code";
    private Map<String, Object> mProperties;

    private Map<String, Object> eventMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mProperties = new HashMap<>();
        eventMap = new HashMap<>();

        final OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
        builder.readTimeout(10, TimeUnit.SECONDS);
        builder.connectTimeout(10, TimeUnit.SECONDS);
        if (BuildConfig.DEBUG) {
            final HttpLoggingInterceptor interceptor =
                    new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                @Override
                public void log(String message) {
                    Crashlytics.log(Log.DEBUG, LOG_TAG, message);
                }
            });
            interceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
            builder.addInterceptor(interceptor);
        }

        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(FEEDBACK_API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(builder.build())
                .build();
        mDoorbellService = retrofit.create(DoorbellService.class);

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

        mDao = RouterManagementActivity.getDao(this);

        setContentView(R.layout.activity_feedback);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.activity_feedback_toolbar);
        if (toolbar != null) {
            toolbar.setTitle("Send Feedback");
            toolbar.setTitleTextAppearance(getApplicationContext(),
                    R.style.ToolbarTitle);
            toolbar.setSubtitleTextAppearance(getApplicationContext(),
                    R.style.ToolbarSubtitle);
            toolbar.setTitleTextColor(ContextCompat.getColor(this,
                    R.color.white));
            toolbar.setSubtitleTextColor(ContextCompat.getColor(this,
                    R.color.white));
            setSupportActionBar(toolbar);
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        emailInputLayout = (TextInputLayout) findViewById(R.id.activity_feedback_email_input_layout);
        email = (EditText) findViewById(R.id.activity_feedback_email);

        contentInputLayout = (TextInputLayout) findViewById(R.id.activity_feedback_content_input_layout);
        content = (EditText) findViewById(R.id.activity_feedback_content);

        routerInfo = (EditText) findViewById(R.id.activity_feedback_router_information_content);

        includeScreenshotAndLogs = (CheckBox) findViewById(R.id.activity_feedback_include_screenshot_and_logs);

        screenshotThumb = (ImageButton)
                findViewById(R.id.activity_feedback_include_screenshot_and_logs_content_screenshot);
        screenshotExpanded = (ImageView)
                findViewById(R.id.activity_feedback_include_screenshot_and_logs_content_screenshot_expanded);

        // Retrieve and cache the system's default "short" animation time.
        mShortAnimationDuration = getResources().getInteger(
                android.R.integer.config_shortAnimTime);

        final Intent intent = getIntent();

        //Set user-defined email if any
        final String acraEmailAddr = getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, MODE_PRIVATE)
                .getString(DDWRTCompanionConstants.ACRA_USER_EMAIL, null);

        email.setText(acraEmailAddr);

        final String routerSelected =
                intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);
        if (routerSelected != null &&
                (mRouter = mDao.getRouter(routerSelected)) != null) {
            final SharedPreferences routerPrefs =
                    getSharedPreferences(mRouter.getUuid(), Context.MODE_PRIVATE);

            //Fill with router information
            routerInfo.setText(
                    String.format("- Model: %s\n" +
                            "- Firmware: %s\n" +
                            "- Kernel: %s\n" +
                            "- CPU Model: %s\n" +
                            "- CPU Cores: %s\n",
                            Router.getRouterModel(this, mRouter),
                            routerPrefs.getString(NVRAMInfo.LOGIN_PROMPT, "-"),
                            routerPrefs.getString(NVRAMInfo.KERNEL, "-"),
                            routerPrefs.getString(NVRAMInfo.CPU_MODEL, "-"),
                            routerPrefs.getString(NVRAMInfo.CPU_CORES_COUNT, "-")));
        }

        final View screenshotAndLogsContentView =
                findViewById(R.id.activity_feedback_include_screenshot_and_logs_content);

        screenshotFilePath = intent.getStringExtra(SCREENSHOT_FILE);
        if (!TextUtils.isEmpty(screenshotFilePath)) {
            final File file = new File(screenshotFilePath);
            if (file.exists()) {
                includeScreenshotAndLogs.setVisibility(View.VISIBLE);
                screenshotAndLogsContentView.setVisibility(View.VISIBLE);
                mBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                screenshotThumb.setImageBitmap(mBitmap);
                screenshotExpanded.setImageBitmap(mBitmap);

                // Hook up clicks on the thumbnail views.

                screenshotThumb.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        zoomImageFromThumb(screenshotThumb, mBitmap);
                    }
                });
            } else {
                includeScreenshotAndLogs.setVisibility(View.GONE);
                screenshotAndLogsContentView.setVisibility(View.GONE);
            }
        } else {
            includeScreenshotAndLogs.setVisibility(View.GONE);
            screenshotAndLogsContentView.setVisibility(View.GONE);
        }

        this.buildProperties();

        // Set app related properties
        final PackageManager manager = getPackageManager();
        try {
            final PackageInfo info = manager.getPackageInfo(getPackageName(), 0);

            this.addProperty(PROPERTY_APP_VERSION_NAME, info.versionName);
            this.addProperty(PROPERTY_APP_VERSION_CODE, info.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            //No worries
        }

        final String feedbackUniqueId = UUID.randomUUID().toString();

        this.addProperty("FEEDBACK_UUID", feedbackUniqueId);

        //Also add build related properties
        this.addProperty("BUILD_DEBUG", BuildConfig.DEBUG);
        this.addProperty("BUILD_APPLICATION_ID", BuildConfig.APPLICATION_ID);
        this.addProperty("BUILD_VERSION_CODE", BuildConfig.VERSION_CODE);
        this.addProperty("BUILD_FLAVOR", BuildConfig.FLAVOR);
        this.addProperty("BUILD_TYPE", BuildConfig.BUILD_TYPE);
        this.addProperty("BUILD_VERSION_NAME", BuildConfig.VERSION_NAME);

        if (mRouter != null) {
            final SharedPreferences routerPrefs =
                    getSharedPreferences(mRouter.getUuid(), Context.MODE_PRIVATE);

            this.addProperty("Router Model", Router.getRouterModel(this, mRouter));
            this.addProperty("Router Firmware", routerPrefs.getString(NVRAMInfo.LOGIN_PROMPT, "-"));
            this.addProperty("Router Kernel", routerPrefs.getString(NVRAMInfo.KERNEL, "-"));
            this.addProperty("Router CPU Model", routerPrefs.getString(NVRAMInfo.CPU_MODEL, "-"));
            this.addProperty("Router CPU Cores", routerPrefs.getString(NVRAMInfo.CPU_CORES_COUNT, "-"));
        }

        eventMap.put("BUILD_APPLICATION_ID", BuildConfig.APPLICATION_ID);
        eventMap.put("BUILD_FLAVOR", BuildConfig.FLAVOR);
        eventMap.put("BUILD_TYPE", BuildConfig.BUILD_TYPE);
        eventMap.put("BUILD_VERSION_NAME", BuildConfig.VERSION_NAME);

        eventMap.put("FEEDBACK_UUID", feedbackUniqueId);

        eventMap.put("Status", "Displayed");
        ReportingUtils.reportEvent(ReportingUtils.EVENT_FEEDBACK, eventMap);
    }

    private void buildProperties() {
        // Set phone related properties
        // this.addProperty("Brand", Build.BRAND); // mobile phone carrier
        this.addProperty(PROPERTY_MODEL, Build.MODEL);
        this.addProperty(PROPERTY_ANDROID_VERSION, Build.VERSION.RELEASE);

        try {
            SupplicantState supState;
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            supState = wifiInfo.getSupplicantState();

            this.addProperty(PROPERTY_WI_FI_ENABLED, supState);
        } catch (Exception e) {

        }


        boolean mobileDataEnabled = false; // Assume disabled
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            Class cmClass = Class.forName(cm.getClass().getName());
            Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true); // Make the method callable
            // get the setting for "mobile data"
            mobileDataEnabled = (Boolean) method.invoke(cm);
        } catch (Exception e) {
            // Some problem accessible private API
            // TODO do whatever error handling you want here
        }
        this.addProperty(PROPERTY_MOBILE_DATA_ENABLED, mobileDataEnabled);

        try {
            final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean gpsEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            this.addProperty(PROPERTY_GPS_ENABLED, gpsEnabled);
        } catch (Exception e) {

        }

        try {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            String resolution = Integer.toString(metrics.widthPixels) + "x" + Integer.toString(metrics.heightPixels);
            this.addProperty(PROPERTY_SCREEN_RESOLUTION, resolution);
        } catch (Exception e) {

        }

        try {
            String activityName = getClass().getSimpleName();
            this.addProperty(PROPERTY_CALLER_ACTIVITY, getIntent().getStringExtra(CALLER_ACTIVITY));
        } catch (Exception e) {
        }
    }

    public void addProperty(String key, Object value) {
        this.mProperties.put(key, value);
    }

    @Override
    public boolean onOptionsItemSelected(
            @NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                eventMap.put("Status", "Canceled");
                ReportingUtils.reportEvent(ReportingUtils.EVENT_FEEDBACK, eventMap);
                Utils.displayRatingBarIfNeeded(this);
                onBackPressed();
                return true;
            case R.id.feedback_send: {
                //Validate form
                if (TextUtils.isEmpty(email.getText())) {
                    emailInputLayout.setErrorEnabled(true);
                    emailInputLayout.setError("Must not be blank");
                    return false;
                } else {
                    emailInputLayout.setErrorEnabled(false);
                }
                if (TextUtils.isEmpty(content.getText())) {
                    contentInputLayout.setErrorEnabled(true);
                    contentInputLayout.setError("Must not be blank");
                    return false;
                } else {
                    contentInputLayout.setErrorEnabled(false);
                }

                //Check that device is actually connected to the internet prior to going any further
                final ConnectivityManager connMgr = (ConnectivityManager)
                        getSystemService(Context.CONNECTIVITY_SERVICE);
                final NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                if (networkInfo == null || !networkInfo.isConnected()) {
                    Toast.makeText(FeedbackActivity.this,
                            "An Internet connection is needed to send feedbacks.", Toast.LENGTH_SHORT).show();
                    return false;
                }
                final boolean includeScreenshot = includeScreenshotAndLogs.isChecked();
                final String emailText = email.getText().toString();
                final String contentText = content.getText().toString();

                final ProgressDialog alertDialog = ProgressDialog.show(this,
                        "Submitting Feedback", "Please hold on - submitting feedback...", true);
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {

                        try {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    alertDialog.show();
                                }
                            });
                            final Response<ResponseBody> openResponse = mDoorbellService.openApplication(DOORBELL_APPID, DOORBELL_APIKEY)
                                    .execute();
                            if (openResponse.code() != 201) {
                                Toast.makeText(FeedbackActivity.this, "Error: " + openResponse.message(),
                                        Toast.LENGTH_SHORT).show();
                                return null;
                            }

                            String[] attachments = null;
                            if (includeScreenshot) {
                                final RequestBody requestBody = RequestBody.create(
                                        MediaType.parse("image/png"),
                                        new File(screenshotFilePath));
                                final Response<String[]> uploadResponse = mDoorbellService
                                        .upload(DOORBELL_APPID, DOORBELL_APIKEY, requestBody)
                                        .execute();
                                if (uploadResponse.code() != 201) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(FeedbackActivity.this, "Error: " + openResponse.message(),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                    return null;
                                }
                                attachments = uploadResponse.body();
                            }
                            if (attachments == null) {
                                attachments = new String[0];
                            }

                            final Response<ResponseBody> response = mDoorbellService
                                    .submitFeedbackForm(
                                            DOORBELL_APPID, DOORBELL_APIKEY,
                                            emailText, 
                                            contentText + 
                                                (TextUtils.isEmpty(routerInfo.getText()) ? 
                                                    "" : 
                                                    ("\n\n-------" + routerInfo.getText() + "\n-------")), 
                                            null,
                                            GSON_BUILDER.create().toJson(mProperties),
                                            attachments)
                                    .execute();

                            if (response.code() == 201) {
                                eventMap.put("Status", "Sent");
                                ReportingUtils.reportEvent(ReportingUtils.EVENT_FEEDBACK, eventMap);

                                final String responseStr = response.body().string();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(FeedbackActivity.this,
                                                responseStr,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                            return null;
                        } catch (final Exception e) {
                            e.printStackTrace();
                        }

                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        alertDialog.dismiss();
                        finish();
                    }
                }.execute();
            }
                return true;
            default:
                break;

        }

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_feedback, menu);
        this.optionsMenu = menu;
        return true;
    }

    private void zoomImageFromThumb(final View thumbView, Bitmap bitmap) {
        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        if (mCurrentAnimator != null) {
            mCurrentAnimator.cancel();
        }

        // Load the high-resolution "zoomed-in" image.
        final ImageView expandedImageView = (ImageView) findViewById(
                R.id.activity_feedback_include_screenshot_and_logs_content_screenshot_expanded);
        expandedImageView.setImageBitmap(bitmap);

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        thumbView.getGlobalVisibleRect(startBounds);
        findViewById(R.id.container)
                .getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        float startScale;
        if ((float) finalBounds.width() / finalBounds.height()
                > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        thumbView.setAlpha(0f);
        expandedImageView.setVisibility(View.VISIBLE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        expandedImageView.setPivotX(0f);
        expandedImageView.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(expandedImageView, View.X,
                        startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.Y,
                        startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X,
                        startScale, 1f)).with(ObjectAnimator.ofFloat(expandedImageView,
                View.SCALE_Y, startScale, 1f));
        set.setDuration(mShortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mCurrentAnimator = null;
            }
        });
        set.start();
        mCurrentAnimator = set;

        // Upon clicking the zoomed-in image, it should zoom back down
        // to the original bounds and show the thumbnail instead of
        // the expanded image.
        final float startScaleFinal = startScale;
        expandedImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCurrentAnimator != null) {
                    mCurrentAnimator.cancel();
                }

                // Animate the four positioning/sizing properties in parallel,
                // back to their original values.
                AnimatorSet set = new AnimatorSet();
                set.play(ObjectAnimator
                        .ofFloat(expandedImageView, View.X, startBounds.left))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.Y,startBounds.top))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.SCALE_X, startScaleFinal))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.SCALE_Y, startScaleFinal));
                set.setDuration(mShortAnimationDuration);
                set.setInterpolator(new DecelerateInterpolator());
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        thumbView.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        mCurrentAnimator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        thumbView.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        mCurrentAnimator = null;
                    }
                });
                set.start();
                mCurrentAnimator = set;
            }
        });
    }

}
