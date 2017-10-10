package org.rm3l.router_companion.tasker.feedback.maoni;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import com.facebook.device.yearclass.YearClass;
import com.google.common.base.Throwables;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import needle.UiRelatedProgressTask;
import okhttp3.ResponseBody;
import org.rm3l.maoni.common.contract.Handler;
import org.rm3l.maoni.common.model.DeviceInfo;
import org.rm3l.maoni.common.model.Feedback;
import org.rm3l.router_companion.tasker.BuildConfig;
import org.rm3l.router_companion.tasker.Constants;
import org.rm3l.router_companion.tasker.R;
import org.rm3l.router_companion.tasker.api.feedback.DoorbellService;
import org.rm3l.router_companion.tasker.api.urlshortener.goo_gl.GooGlService;
import org.rm3l.router_companion.tasker.api.urlshortener.goo_gl.resources.GooGlData;
import org.rm3l.router_companion.tasker.multithreading.MultiThreadingManager;
import org.rm3l.router_companion.tasker.utils.AWSUtils;
import org.rm3l.router_companion.tasker.utils.NetworkUtils;
import org.rm3l.router_companion.tasker.utils.Utils;
import retrofit2.Response;

/**
 * Created by rm3l on 13/05/16.
 */
public class FeedbackHandler implements Handler {

    private static class InterruptedTaskException extends RuntimeException {

        public InterruptedTaskException(@Nullable String detailMessage) {
            super(detailMessage);
        }
    }

    private class FeedbackSenderTask extends
            UiRelatedProgressTask<Map.Entry<Response<ResponseBody>, ? extends Exception>, Integer> {

        private static final int STARTED = 1;

        private static final int UPLOADING_ATTACHMENT = 2;

        private static final int UPLOADING_LOGS = 3;

        private static final int OPENING_APPLICATION = 4;

        private static final int SUBMITTING_FEEDBACK = 5;

        private final ProgressDialog alertDialog;

        private final Feedback feedback;

        private final Map<String, Object> properties;

        public FeedbackSenderTask(Feedback feedback, Map<String, Object> properties) {
            this.feedback = feedback;
            this.properties = properties;

            alertDialog = new ProgressDialog(mContext);
            alertDialog.setTitle("Please hold on...");
            alertDialog.setMessage("Submitting feedback...");
            alertDialog.setIndeterminate(false);
            alertDialog.setCancelable(false);
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FeedbackSenderTask.this.cancel();
                        }
                    });
            alertDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            //            alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            //                @Override
            //                public void onCancel(DialogInterface dialog) {
            //                    FeedbackSenderTask.this.cancel();
            //                }
            //            });
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    alertDialog.show();
                }
            });
        }

        @Override
        protected Map.Entry<Response<ResponseBody>, ? extends Exception> doWork() {
            publishProgress(STARTED);
            final boolean includeScreenshot = feedback.includeScreenshot;
            final boolean includeLogs = feedback.includeLogs;
            final String contentText = feedback.userComment.toString();

            try {
                //1. Upload screenshot and shorten S3 URL
                String screenshotCaptureUploadUrl = null;
                if (includeScreenshot) {
                    publishProgress(UPLOADING_ATTACHMENT);

                    final TransferUtility transferUtility = AWSUtils.getTransferUtility(mContext);

                    final SharedPreferences preferences =
                            mContext.getSharedPreferences(Constants.DEFAULT_SHARED_PREFERENCES_KEY,
                                    Context.MODE_PRIVATE);
                    final Integer pendingTransferId;
                    if (preferences.contains(Constants.AWS_S3_FEEDBACK_PENDING_TRANSFER_PREF)) {
                        pendingTransferId =
                                preferences.getInt(Constants.AWS_S3_FEEDBACK_PENDING_TRANSFER_PREF, -1);
                    } else {
                        pendingTransferId = null;
                    }
                    if (pendingTransferId != null && pendingTransferId != -1) {
                        if (transferUtility.cancel(pendingTransferId)) {
                            preferences.edit().remove(Constants.AWS_S3_FEEDBACK_PENDING_TRANSFER_PREF).apply();
                        }
                    }

                    //Upload to AWS S3
                    final TransferObserver uploadObserver =
                            transferUtility.upload(Constants.AWS_S3_BUCKET_NAME,
                                    String.format("%s/%s.png", Constants.AWS_S3_FEEDBACKS_FOLDER_NAME, feedback.id),
                                    feedback.screenshotFile);

                    //Save transfer ID
                    preferences.edit()
                            .putInt(Constants.AWS_S3_FEEDBACK_PENDING_TRANSFER_PREF, uploadObserver.getId())
                            .apply();
                    Utils.requestBackup(mContext);

                    uploadObserver.setTransferListener(new TransferListener() {
                        @Override
                        public void onError(int id, Exception ex) {

                        }

                        @Override
                        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {

                        }

                        @Override
                        public void onStateChanged(int id, TransferState state) {

                        }
                    }); // required, or else you need to call refresh()

                    while (true) {
                        if (FeedbackSenderTask.this.isCanceled()) {
                            transferUtility.cancel(uploadObserver.getId());
                            throw new InterruptedTaskException("User interruption");
                        }
                        final TransferState transferState = uploadObserver.getState();
                        if (TransferState.COMPLETED.equals(transferState) || TransferState.FAILED.equals(
                                transferState)) {
                            if (TransferState.FAILED.equals(transferState)) {
                                return new AbstractMap.SimpleImmutableEntry<Response<ResponseBody>, Exception>(
                                        null,
                                        new IllegalStateException("Failed to upload screenshot capture"));
                            } else {
                                //Set URL TO S3
                                final Response<GooGlData> response =
                                        mGooGlService.shortenLongUrl(Constants.GOOGLE_API_KEY,
                                                new GooGlData().setLongUrl(
                                                        String.format("https://%s.s3.amazonaws.com/%s/%s.png",
                                                                Constants.AWS_S3_BUCKET_NAME,
                                                                Constants.AWS_S3_FEEDBACKS_FOLDER_NAME, feedback.id)))
                                                .execute();
                                NetworkUtils.checkResponseSuccessful(response);
                                screenshotCaptureUploadUrl = response.body().getId();
                            }
                            break;
                        }
                        Thread.sleep(TimeUnit.SECONDS.toMillis(2));
                    }
                }

                String logsUrl = null;
                if (includeLogs) {
                    publishProgress(UPLOADING_LOGS);

                    final TransferUtility transferUtility = AWSUtils.getTransferUtility(mContext);

                    final SharedPreferences preferences =
                            mContext.getSharedPreferences(Constants.DEFAULT_SHARED_PREFERENCES_KEY,
                                    Context.MODE_PRIVATE);
                    final Integer pendingTransferId;
                    if (preferences.contains(Constants.AWS_S3_FEEDBACK_PENDING_TRANSFER_PREF)) {
                        pendingTransferId =
                                preferences.getInt(Constants.AWS_S3_FEEDBACK_PENDING_TRANSFER_PREF, -1);
                    } else {
                        pendingTransferId = null;
                    }
                    if (pendingTransferId != null && pendingTransferId != -1) {
                        if (transferUtility.cancel(pendingTransferId)) {
                            preferences.edit().remove(Constants.AWS_S3_FEEDBACK_PENDING_TRANSFER_PREF).apply();
                        }
                    }

                    //Upload to AWS S3
                    final TransferObserver uploadObserver =
                            transferUtility.upload(Constants.AWS_S3_BUCKET_NAME,
                                    String.format("%s/%s/%s.txt", Constants.AWS_S3_FEEDBACKS_FOLDER_NAME,
                                            Constants.AWS_S3_LOGS_FOLDER_NAME, feedback.id), feedback.logsFile);

                    //Save transfer ID
                    preferences.edit()
                            .putInt(Constants.AWS_S3_FEEDBACK_PENDING_TRANSFER_PREF, uploadObserver.getId())
                            .apply();
                    Utils.requestBackup(mContext);

                    uploadObserver.setTransferListener(new TransferListener() {
                        @Override
                        public void onError(int id, Exception ex) {

                        }

                        @Override
                        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {

                        }

                        @Override
                        public void onStateChanged(int id, TransferState state) {

                        }
                    }); // required, or else you need to call refresh()

                    while (true) {
                        if (FeedbackSenderTask.this.isCanceled()) {
                            transferUtility.cancel(uploadObserver.getId());
                            throw new InterruptedTaskException("User interruption");
                        }
                        final TransferState transferState = uploadObserver.getState();
                        if (TransferState.COMPLETED.equals(transferState) || TransferState.FAILED.equals(
                                transferState)) {
                            if (TransferState.FAILED.equals(transferState)) {
                                return new AbstractMap.SimpleImmutableEntry<Response<ResponseBody>, Exception>(null,
                                        new IllegalStateException("Failed to upload logs"));
                            } else {
                                //Set URL TO S3
                                final Response<GooGlData> response =
                                        mGooGlService.shortenLongUrl(Constants.GOOGLE_API_KEY,
                                                new GooGlData().setLongUrl(
                                                        String.format("https://%s.s3.amazonaws.com/%s/%s/%s.txt",
                                                                Constants.AWS_S3_BUCKET_NAME,
                                                                Constants.AWS_S3_FEEDBACKS_FOLDER_NAME,
                                                                Constants.AWS_S3_LOGS_FOLDER_NAME, feedback.id)))
                                                .execute();
                                NetworkUtils.checkResponseSuccessful(response);
                                logsUrl = response.body().getId();
                            }
                            break;
                        }
                        Thread.sleep(TimeUnit.SECONDS.toMillis(2));
                    }
                }

                //2. Open App in Doorbell
                publishProgress(OPENING_APPLICATION);
                final Response<ResponseBody> openResponse =
                        mDoorbellService.openApplication(Constants.DOORBELL_APPID, Constants.DOORBELL_APIKEY)
                                .execute();
                NetworkUtils.checkResponseSuccessful(openResponse);

                if (openResponse.code() != 201) {
                    return new AbstractMap.SimpleImmutableEntry<Response<ResponseBody>, Exception>(
                            openResponse, new IllegalStateException());
                }

                //3. Submit the actual feedback
                publishProgress(SUBMITTING_FEEDBACK);

                //Add device info retrieved from the Feedback object
                final DeviceInfo deviceInfo = feedback.deviceInfo;
                if (deviceInfo != null) {
                    properties.putAll(feedback.getDeviceAndAppInfoAsHumanReadableMap());
                }
                if (screenshotCaptureUploadUrl != null) {
                    properties.put("Screenshot", screenshotCaptureUploadUrl);
                }
                if (logsUrl != null) {
                    properties.put("Logs", logsUrl);
                }

                final String emailText = mEmail.getText().toString();

                final Response<ResponseBody> response =
                        mDoorbellService.submitFeedbackForm(Constants.DOORBELL_APPID, Constants.DOORBELL_APIKEY,
                                emailText, String.format("%s\n\n"
                                                + "-------\n"
                                                + "%s"
                                                + "- Android Version: %s (SDK %s)\n"
                                                + "- Device: %s (%s)\n"
                                                + "-------\n\n"
                                                + ">>> NOTE: Ask questions and discuss/vote for features on %s <<<",
                                        contentText, TextUtils.isEmpty(screenshotCaptureUploadUrl) ? ""
                                                : String.format("Screenshot: %s\n\n", screenshotCaptureUploadUrl),
                                        deviceInfo != null ? deviceInfo.androidReleaseVersion : UNKNOWN,
                                        deviceInfo != null ? deviceInfo.sdkVersion : UNKNOWN,
                                        deviceInfo != null ? deviceInfo.model : UNKNOWN,
                                        deviceInfo != null ? deviceInfo.manufacturer : UNKNOWN,
                                        Constants.Q_A_WEBSITE),
                                null, GSON_BUILDER.create().toJson(properties), new String[0]).execute();
                NetworkUtils.checkResponseSuccessful(response);

                return new AbstractMap.SimpleImmutableEntry<>(response, null);
            } catch (final Exception e) {
                return new AbstractMap.SimpleImmutableEntry<>(null, e);
            }
        }

        @Override
        protected void onProgressUpdate(Integer progress) {
            if (FeedbackSenderTask.this.isCanceled()) {
                throw new InterruptedTaskException("User interruption");
            }
            if (progress == null) {
                return;
            }
            switch (progress) {
                case STARTED:
                    alertDialog.setProgress(20);
                    break;
                case UPLOADING_ATTACHMENT:
                    alertDialog.setProgress(40);
                    alertDialog.setMessage("Uploading screen capture...");
                    break;
                case UPLOADING_LOGS:
                    alertDialog.setProgress(60);
                    alertDialog.setMessage("Uploading application logs...");
                    break;
                case OPENING_APPLICATION:
                    alertDialog.setProgress(80);
                    alertDialog.setMessage("Connecting to the remote feedback service...");
                    break;
                case SUBMITTING_FEEDBACK:
                    alertDialog.setProgress(97);
                    alertDialog.setMessage("Submitting your valuable feedback...");
                    break;
                default:
                    break;
            }
        }

        @Override
        protected void thenDoUiRelatedWork(
                Map.Entry<Response<ResponseBody>, ? extends Exception> result) {
            try {
                if (FeedbackSenderTask.this.isCanceled()) {
                    Toast.makeText(mContext, "Cancelled.", Toast.LENGTH_SHORT).show();
                }
                if (result == null) {
                    return;
                }
                final Response<ResponseBody> response = result.getKey();
                final Exception exception = result.getValue();

                if (exception != null) {
                    String errorMsg = null;
                    if (response != null) {
                        errorMsg = response.message();
                    }
                    if (TextUtils.isEmpty(errorMsg)) {
                        errorMsg = Throwables.getRootCause(exception).getMessage();
                    }
                    Toast.makeText(mContext, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
                    return;
                }

                if (response != null) {
                    final ResponseBody responseBody = response.body();
                    if (response.code() == 201 || response.code() == 502) {
                        //FIXME Check with Doorbell support why an 502 Bad Gateway s being returned by their API,
                        //even if feedback is properly stored
                        try {
                            Toast.makeText(mContext, responseBody != null ? responseBody.string()
                                    : "Thank you - we'll get back to you as soon as possible", Toast.LENGTH_SHORT)
                                    .show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(mContext,
                                "Error" + (responseBody != null ? (": " + responseBody.toString()) : ""),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            } finally {
                alertDialog.dismiss();
            }
        }
    }

    public static final String FEEDBACK_API_BASE_URL = "https://doorbell.io/api/";

    public static final String UNKNOWN = "???";

    public static final String MAONI_EMAIL = "maoni_email";

    public static final String PROPERTY_BUILD_DEBUG = "BUILD_DEBUG";

    public static final String PROPERTY_BUILD_APPLICATION_ID = "BUILD_APPLICATION_ID";

    public static final String PROPERTY_BUILD_VERSION_CODE = "BUILD_VERSION_CODE";

    public static final String PROPERTY_BUILD_FLAVOR = "BUILD_FLAVOR";

    public static final String PROPERTY_BUILD_TYPE = "BUILD_TYPE";

    public static final String PROPERTY_BUILD_VERSION_NAME = "BUILD_VERSION_NAME";

    public static final String PROPERTY_DEVICE_YEAR_CLASS = "Device Year Class";

    private static final GsonBuilder GSON_BUILDER = new GsonBuilder();

    private Activity mContext;

    private DoorbellService mDoorbellService;

    private EditText mEmail;

    private TextInputLayout mEmailInputLayout;

    private final SharedPreferences mGlobalPreferences;

    private GooGlService mGooGlService;

    public FeedbackHandler(Activity context) {
        this.mContext = context;
        mGlobalPreferences = context.getSharedPreferences(Constants.DEFAULT_SHARED_PREFERENCES_KEY,
                Context.MODE_PRIVATE);
        mDoorbellService = NetworkUtils.createApiService(FEEDBACK_API_BASE_URL, DoorbellService.class);
        mGooGlService =
                NetworkUtils.createApiService(Constants.URL_SHORTENER_API_BASE_URL, GooGlService.class);
    }

    @Override
    public void onCreate(@NonNull View rootView, Bundle savedInstanceState) {
        mEmailInputLayout =
                (TextInputLayout) rootView.findViewById(R.id.activity_feedback_email_input_layout);
        mEmail = (EditText) rootView.findViewById(R.id.activity_feedback_email);

        //Load previously used email addr
        String emailAddr = null;
        if (mGlobalPreferences.contains(MAONI_EMAIL)) {
            emailAddr = mGlobalPreferences.getString(MAONI_EMAIL, "");
        }
        mEmail.setText(emailAddr, TextView.BufferType.EDITABLE);
    }

    @Override
    public void onDismiss() {
        //Nothing to do actually
    }

    @Override
    public boolean onSendButtonClicked(@NonNull final Feedback feedback) {
        //Check that device is actually connected to the internet prior to going any further
        final ConnectivityManager connMgr =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            Toast.makeText(mContext, "An Internet connection is needed to send feedbacks.",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        final Map<String, Object> properties = new HashMap<>();
        properties.put("UUID", feedback.id);

        //Also add build related properties
        properties.put(PROPERTY_BUILD_FLAVOR, BuildConfig.FLAVOR);
        properties.put(PROPERTY_BUILD_TYPE, BuildConfig.BUILD_TYPE);

        properties.put(PROPERTY_DEVICE_YEAR_CLASS, YearClass.get(mContext.getApplicationContext()));

        final String emailText = mEmail.getText().toString();

        //Save last value, so it can be prefill next time
        mGlobalPreferences.edit().putString(MAONI_EMAIL, emailText).apply();

        MultiThreadingManager.getFeedbackExecutor()
                .execute(new FeedbackSenderTask(feedback, properties));

        return true;
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
