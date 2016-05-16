package org.rm3l.ddwrt.feedback.maoni;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.GsonBuilder;

import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.feedback.api.DoorbellService;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.NetworkUtils;
import org.rm3l.maoni.Maoni;
import org.rm3l.maoni.model.Feedback;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DOORBELL_APIKEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DOORBELL_APPID;

/**
 * Created by rm3l on 13/05/16.
 */
public class MaoniFeedbackHandler implements Maoni.Handler {

    public static final String FEEDBACK_API_BASE_URL = "https://doorbell.io/api/";

    private Activity mContext;
    private Router mRouter;

    private TextInputLayout mEmailInputLayout;
    private EditText mEmail;

    private EditText mRouterInfo;

    private DoorbellService mDoorbellService;

    private static final GsonBuilder GSON_BUILDER = new GsonBuilder();

    public static final String PROPERTY_BUILD_DEBUG = "BUILD_DEBUG";
    public static final String PROPERTY_BUILD_APPLICATION_ID = "BUILD_APPLICATION_ID";
    public static final String PROPERTY_BUILD_VERSION_CODE = "BUILD_VERSION_CODE";
    public static final String PROPERTY_BUILD_FLAVOR = "BUILD_FLAVOR";
    public static final String PROPERTY_BUILD_TYPE = "BUILD_TYPE";
    public static final String PROPERTY_BUILD_VERSION_NAME = "BUILD_VERSION_NAME";

    private Map<String, Object> mProperties = new ConcurrentHashMap<>();

    public MaoniFeedbackHandler(Activity context, Router router) {
        this.mContext = context;
        this.mRouter = router;

        if (this.mRouter != null) {
            final SharedPreferences routerPrefs =
                    mContext.getSharedPreferences(mRouter.getUuid(), Context.MODE_PRIVATE);

            this.mProperties.put("Router Model", Router.getRouterModel(mContext, mRouter));
            this.mProperties.put("Router Firmware", routerPrefs.getString(NVRAMInfo.LOGIN_PROMPT, "-"));
            this.mProperties.put("Router Kernel", routerPrefs.getString(NVRAMInfo.KERNEL, "-"));
            this.mProperties.put("Router CPU Model", routerPrefs.getString(NVRAMInfo.CPU_MODEL, "-"));
            this.mProperties.put("Router CPU Cores", routerPrefs.getString(NVRAMInfo.CPU_CORES_COUNT, "-"));
        }

        //Also add build related properties
        this.mProperties.put(PROPERTY_BUILD_DEBUG, BuildConfig.DEBUG);
        this.mProperties.put(PROPERTY_BUILD_APPLICATION_ID, BuildConfig.APPLICATION_ID);
        this.mProperties.put(PROPERTY_BUILD_VERSION_CODE, BuildConfig.VERSION_CODE);
        this.mProperties.put(PROPERTY_BUILD_FLAVOR, BuildConfig.FLAVOR);
        this.mProperties.put(PROPERTY_BUILD_TYPE, BuildConfig.BUILD_TYPE);
        this.mProperties.put(PROPERTY_BUILD_VERSION_NAME, BuildConfig.VERSION_NAME);

        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(FEEDBACK_API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(NetworkUtils.getHttpClientInstance())
                .build();
        mDoorbellService = retrofit.create(DoorbellService.class);
    }

    @Override
    public void onDismiss() {
        //Nothing to do actually
    }

    @Override
    public void onSendButtonClicked(@NonNull final Feedback feedback) {
        //Check that device is actually connected to the internet prior to going any further
        final ConnectivityManager connMgr = (ConnectivityManager)
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            Toast.makeText(mContext,
                    "An Internet connection is needed to send feedbacks.", Toast.LENGTH_SHORT).show();
            return;
        }

        final boolean includeScreenshot = feedback.includeScreenshot;
        final String emailText = mEmail.getText().toString();
        final String contentText = feedback.userComment.toString();
        final String routerInfoText = mRouterInfo.getText().toString();

        final ProgressDialog alertDialog = ProgressDialog.show(mContext,
                "Submitting Feedback", "Please hold on - submitting feedback...", true);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                try {
                    mContext.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            alertDialog.show();
                        }
                    });
                    final Response<ResponseBody> openResponse = mDoorbellService.openApplication(DOORBELL_APPID, DOORBELL_APIKEY)
                            .execute();
                    if (openResponse.code() != 201) {
                        Toast.makeText(mContext, "Error: " + openResponse.message(),
                                Toast.LENGTH_SHORT).show();
                        return null;
                    }

                    String[] attachments = null;
                    if (includeScreenshot) {
                        final RequestBody requestBody = RequestBody.create(
                                MediaType.parse("image/png"),
                                feedback.screenshotFilePath);
                        final Response<String[]> uploadResponse = mDoorbellService
                                .upload(DOORBELL_APPID, DOORBELL_APIKEY, requestBody)
                                .execute();
                        if (uploadResponse.code() != 201) {
                            mContext.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(mContext, "Error: " + openResponse.message(),
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
                                    String.format("%s\n\n" +
                                                    "-------\n" +
                                                    "- Feedback UUID: %s\n" +
                                                    "%s" +
                                                    "-------",
                                            contentText,
                                            feedback.id,
                                            TextUtils.isEmpty(routerInfoText) ?
                                                    "" : routerInfoText),
                                    null,
                                    GSON_BUILDER.create().toJson(mProperties),
                                    attachments)
                            .execute();

                    if (response.code() == 201) {
                        final String responseStr = response.body().string();
                        mContext.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext,
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
            }
        }.execute();
    }

    @Override
    public void onCreate(@NonNull View rootView, Bundle savedInstanceState) {
        mEmailInputLayout = (TextInputLayout) rootView.findViewById(R.id.activity_feedback_email_input_layout);
        mEmail = (EditText) rootView.findViewById(R.id.activity_feedback_email);

        mRouterInfo = (EditText) rootView.findViewById(R.id.activity_feedback_router_information_content);

        //Set user-defined email if any
        final String acraEmailAddr = mContext.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY,
                Context.MODE_PRIVATE)
                .getString(DDWRTCompanionConstants.ACRA_USER_EMAIL, null);
        mEmail.setText(acraEmailAddr, TextView.BufferType.EDITABLE);

        if (mRouter != null) {
            final SharedPreferences routerPrefs =
                    mContext.getSharedPreferences(mRouter.getUuid(), Context.MODE_PRIVATE);

            //Fill with router information
            mRouterInfo.setText(
                    String.format("- Model: %s\n" +
                                    "- Firmware: %s\n" +
                                    "- Kernel: %s\n" +
                                    "- CPU Model: %s\n" +
                                    "- CPU Cores: %s\n",
                            Router.getRouterModel(mContext, mRouter),
                            routerPrefs.getString(NVRAMInfo.LOGIN_PROMPT, "-"),
                            routerPrefs.getString(NVRAMInfo.KERNEL, "-"),
                            routerPrefs.getString(NVRAMInfo.CPU_MODEL, "-"),
                            routerPrefs.getString(NVRAMInfo.CPU_CORES_COUNT, "-")),
                    TextView.BufferType.EDITABLE);
        }
    }

    @Override
    public boolean validateForm(@NonNull View rootView) {
        if (mEmail != null) {
            if (TextUtils.isEmpty(mEmail.getText())) {
                if (mEmailInputLayout != null) {
                    mEmailInputLayout.setErrorEnabled(true);
                    mEmailInputLayout.setError("Email must not be blank");
                }
                return false;
            } else {
                if (mEmailInputLayout != null) {
                    mEmailInputLayout.setErrorEnabled(false);
                }
            }
        }
        return true;
    }
}
