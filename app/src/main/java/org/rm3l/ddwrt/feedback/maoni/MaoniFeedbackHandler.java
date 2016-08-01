package org.rm3l.ddwrt.feedback.maoni;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.google.gson.GsonBuilder;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.feedback.api.DoorbellService;
import org.rm3l.ddwrt.multithreading.MultiThreadingManager;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.AWSUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.MaoniUtils;
import org.rm3l.ddwrt.utils.NetworkUtils;
import org.rm3l.maoni.common.contract.Handler;
import org.rm3l.maoni.common.model.DeviceInfo;
import org.rm3l.maoni.common.model.Feedback;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import needle.UiRelatedProgressTask;
import okhttp3.ResponseBody;
import retrofit2.Response;

import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.AWS_S3_BUCKET_NAME;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.AWS_S3_FEEDBACKS_FOLDER_NAME;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DOORBELL_APIKEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DOORBELL_APPID;

/**
 * Created by rm3l on 13/05/16.
 */
public class MaoniFeedbackHandler implements Handler {

    public static final String FEEDBACK_API_BASE_URL = "https://doorbell.io/api/";
    public static final String UNKNOWN = "???";

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

    public MaoniFeedbackHandler(Activity context, Router router) {
        this.mContext = context;
        this.mRouter = router;
        mDoorbellService = NetworkUtils
                .createApiService(FEEDBACK_API_BASE_URL, DoorbellService.class);
    }

    @Override
    public void onDismiss() {
        //Nothing to do actually
    }

    @Override
    public boolean onSendButtonClicked(@NonNull final Feedback feedback) {
        //Check that device is actually connected to the internet prior to going any further
        final ConnectivityManager connMgr = (ConnectivityManager)
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            Toast.makeText(mContext,
                    "An Internet connection is needed to send feedbacks.", Toast.LENGTH_SHORT).show();
            return false;
        }

        final Map<String, Object> properties = new HashMap<>();
        properties.put("UUID", feedback.id);

        if (this.mRouter != null) {
            final SharedPreferences routerPrefs =
                    mContext.getSharedPreferences(mRouter.getUuid(), Context.MODE_PRIVATE);
            properties.put("Router Model", Router.getRouterModel(mContext, mRouter));
            properties.put("Router Firmware", routerPrefs.getString(NVRAMInfo.LOGIN_PROMPT, "-"));
            properties.put("Router Kernel", routerPrefs.getString(NVRAMInfo.KERNEL, "-"));
            properties.put("Router CPU Model", routerPrefs.getString(NVRAMInfo.CPU_MODEL, "-"));
            properties.put("Router CPU Cores", routerPrefs.getString(NVRAMInfo.CPU_CORES_COUNT, "-"));
        }
        //Also add build related properties
        properties.put(PROPERTY_BUILD_DEBUG, BuildConfig.DEBUG);
        properties.put(PROPERTY_BUILD_APPLICATION_ID, BuildConfig.APPLICATION_ID);
        properties.put(PROPERTY_BUILD_VERSION_CODE, BuildConfig.VERSION_CODE);
        properties.put(PROPERTY_BUILD_FLAVOR, BuildConfig.FLAVOR);
        properties.put(PROPERTY_BUILD_TYPE, BuildConfig.BUILD_TYPE);
        properties.put(PROPERTY_BUILD_VERSION_NAME, BuildConfig.VERSION_NAME);


        final boolean includeScreenshot = feedback.includeScreenshot;
        final String emailText = mEmail.getText().toString();
        final String contentText = feedback.userComment.toString();
        final String routerInfoText = mRouterInfo.getText().toString();

        final ProgressDialog alertDialog = ProgressDialog.show(mContext,
                "Please hold on...", "Submitting feedback...", true);
        MultiThreadingManager.getFeedbackExecutor()
                .execute(new UiRelatedProgressTask
                        <ImmutablePair<Response<ResponseBody>, ? extends Exception>, Integer>() {

                    private static final int STARTED = 1;
                    private static final int OPENING_APPLICATION = 2;
                    private static final int UPLOADING_ATTACHMENT = 3;
                    private static final int SUBMITTING_FEEDBACK = 4;

                    @Override
                    protected ImmutablePair<Response<ResponseBody>, ? extends Exception> doWork() {
                        publishProgress(STARTED);
                        try {
                            publishProgress(OPENING_APPLICATION);
                            final Response<ResponseBody> openResponse = mDoorbellService
                                    .openApplication(DOORBELL_APPID, DOORBELL_APIKEY)
                                    .execute();

                            if (openResponse.code() != 201) {
                                return ImmutablePair.of(openResponse, new IllegalStateException());
                            }

                            String screenshotCaptureUploadUrl = null;

                            if (includeScreenshot) {
                                publishProgress(UPLOADING_ATTACHMENT);

                                //Upload to AWS S3
                                final TransferUtility transferUtility =
                                        AWSUtils.getTransferUtility(mContext);
                                final TransferObserver uploadObserver =
                                        transferUtility.upload(AWS_S3_BUCKET_NAME,
                                        String.format("%s/%s.png",
                                                AWS_S3_FEEDBACKS_FOLDER_NAME,
                                                feedback.id),
                                        feedback.screenshotFile);
                                uploadObserver.setTransferListener(new TransferListener() {
                                    @Override
                                    public void onStateChanged(int id, TransferState state) {

                                    }

                                    @Override
                                    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {

                                    }

                                    @Override
                                    public void onError(int id, Exception ex) {

                                    }
                                }); // required, or else you need to call refresh()

                                while (true) {
                                    final TransferState transferState = uploadObserver.getState();
                                    if (TransferState.COMPLETED.equals(transferState)
                                            || TransferState.FAILED.equals(transferState)) {
                                        if (TransferState.FAILED.equals(transferState)) {
                                            return ImmutablePair.of(openResponse,
                                                    new IllegalStateException(
                                                            "Failed to upload screenshot capture"));
                                        } else {
                                            //Set URL TO S3
                                            screenshotCaptureUploadUrl = String.format(
                                                    "https://%s.s3.amazonaws.com/%s/%s.png",
                                                    AWS_S3_BUCKET_NAME,
                                                    AWS_S3_FEEDBACKS_FOLDER_NAME,
                                                    feedback.id);
                                        }
                                        break;
                                    }
                                    Thread.sleep(TimeUnit.SECONDS.toMillis(5));
                                }
                            }

                            publishProgress(SUBMITTING_FEEDBACK);

                            //Add device info retrieved from the Feedback object
                            final DeviceInfo deviceInfo = feedback.deviceInfo;
                            if (deviceInfo != null) {
                                properties.putAll(MaoniUtils.deviceInfoToMap(deviceInfo));
                            }

                            final Response<ResponseBody> response = mDoorbellService
                                    .submitFeedbackForm(
                                            DOORBELL_APPID, DOORBELL_APIKEY,
                                            emailText,
                                            String.format("%s\n\n" +
                                                            "-------\n" +
                                                            "%s" +
                                                            "- Feedback UUID: %s\n" +
                                                            "- Android Version: %s (SDK %s)\n" +
                                                            "- Device: %s (%s)\n" +
                                                            "%s" +
                                                            "-------",
                                                    contentText,
                                                    TextUtils.isEmpty(screenshotCaptureUploadUrl) ?
                                                            "" :
                                                            String.format("Screenshot: %s\n\n",
                                                                    screenshotCaptureUploadUrl),
                                                    feedback.id,
                                                    deviceInfo != null ?
                                                            deviceInfo.androidReleaseVersion :
                                                            UNKNOWN,
                                                    deviceInfo != null ? deviceInfo.sdkVersion :
                                                            UNKNOWN,
                                                    deviceInfo != null ? deviceInfo.model :
                                                            UNKNOWN,
                                                    deviceInfo != null ? deviceInfo.manufacturer :
                                                            UNKNOWN,
                                                    TextUtils.isEmpty(routerInfoText) ?
                                                            "" : routerInfoText),
                                            null,
                                            GSON_BUILDER.create().toJson(properties),
                                            new String[0])
                                    .execute();

                            return ImmutablePair.of(response, null);

                        } catch (final Exception e) {
                            return ImmutablePair.of(null, e);
                        }
                    }

                    @Override
                    protected void thenDoUiRelatedWork(ImmutablePair<Response<ResponseBody>, ? extends Exception> result) {
                        try {
                            if (result == null) {
                                return;
                            }
                            final Response<ResponseBody> response = result.left;
                            final Exception exception = result.right;

                            if (exception != null) {
                                String errorMsg = null;
                                if (response != null) {
                                    errorMsg = response.message();
                                }
                                if (TextUtils.isEmpty(errorMsg)) {
                                    errorMsg = ExceptionUtils.getRootCauseMessage(exception);
                                }
                                Toast.makeText(mContext, "Error: " + errorMsg,
                                        Toast.LENGTH_LONG).show();
                                return;
                            }

                            if (response != null) {
                                final ResponseBody responseBody = response.body();
                                if (response.code() == 201) {
                                    try {
                                        Toast.makeText(mContext,
                                                responseBody != null ?
                                                    responseBody.string() :
                                                    "Thank you - we'll get back to you as soon as possible",
                                                Toast.LENGTH_SHORT).show();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    Toast.makeText(mContext,
                                            "Error" + (responseBody != null ?
                                                    (": " + responseBody.toString()) : ""),
                                            Toast.LENGTH_SHORT).show();
                                }
                            }

                        } finally {
                            alertDialog.dismiss();
                        }
                    }

                    @Override
                    protected void onProgressUpdate(Integer progress) {
                        if (progress == null) {
                            return;
                        }
                        switch (progress) {
                            case STARTED:
                                alertDialog.show();
                                break;
                            case OPENING_APPLICATION:
                                alertDialog.setMessage("Connecting to the remote feedback service...");
                                break;
                            case UPLOADING_ATTACHMENT:
                                alertDialog.setMessage("Uploading attachment...");
                                break;
                            case SUBMITTING_FEEDBACK:
                                alertDialog.setMessage("Submitting your helpful feedback...");
                                break;
                            default:
                                break;
                        }
                    }
                });

        return true;
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
