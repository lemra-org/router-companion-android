package org.rm3l.router_companion.feedback.maoni;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.GsonBuilder;

import org.rm3l.router_companion.R;
import org.rm3l.router_companion.api.urlshortener.goo_gl.GooGlService;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.utils.NetworkUtils;
import org.rm3l.maoni.common.model.Feedback;
import org.rm3l.maoni.doorbell.MaoniDoorbellListener;
import org.rm3l.maoni.common.contract.Handler;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * TODO Not ready as yet
 * Created by rm3l on 27/11/2016.
 */
public class MaoniDoorbellFeedbackHandler extends MaoniDoorbellListener implements Handler {

    public static final String UNKNOWN = "???";
    public static final String MAONI_EMAIL = "maoni_email";

    private Activity mContext;
    private Router mRouter;

    private TextInputLayout mEmailInputLayout;
    private EditText mEmail;

    private EditText mRouterInfo;

    private GooGlService mGooGlService;

    private static final GsonBuilder GSON_BUILDER = new GsonBuilder();

    public static final String PROPERTY_BUILD_DEBUG = "BUILD_DEBUG";
    public static final String PROPERTY_BUILD_APPLICATION_ID = "BUILD_APPLICATION_ID";
    public static final String PROPERTY_BUILD_VERSION_CODE = "BUILD_VERSION_CODE";
    public static final String PROPERTY_BUILD_FLAVOR = "BUILD_FLAVOR";
    public static final String PROPERTY_BUILD_TYPE = "BUILD_TYPE";
    public static final String PROPERTY_BUILD_VERSION_NAME = "BUILD_VERSION_NAME";
    private final SharedPreferences mGlobalPreferences;

    public MaoniDoorbellFeedbackHandler(Activity activity, Router router) {
        this(activity, new Builder(activity), router);
    }

    private MaoniDoorbellFeedbackHandler(Activity activity, Builder builder, Router router) {
        //TODO Customize the builder
        super(builder
            .withAdditionalPropertiesProvider(new Callable<Map<String, Object>>() {
                @Override
                public Map<String, Object> call() throws Exception {
                    //TODO
                    return null;
                }
            })
            .withFeedbackFooterTextProvider(new Callable<CharSequence>() {
                @Override
                public CharSequence call() throws Exception {
                    return null;
                }
            }));
        this.mContext = activity;
        this.mRouter = router;
        mGlobalPreferences = activity.getSharedPreferences(
                RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY,
                Context.MODE_PRIVATE);
        mGooGlService = NetworkUtils
                .createApiService(RouterCompanionAppConstants.URL_SHORTENER_API_BASE_URL,
                        GooGlService.class);

    }

    @Override
    protected String getUserEmail() {
        return super.getUserEmail();
    }

    @Override
    protected String getUserName() {
        return super.getUserName();
    }

    @Override
    public boolean onSendButtonClicked(Feedback feedback) {
        return super.onSendButtonClicked(feedback);
    }

    @Override
    public void onCreate(View rootView, Bundle savedInstanceState) {
        mEmailInputLayout = (TextInputLayout) rootView.findViewById(R.id.activity_feedback_email_input_layout);
        mEmail = (EditText) rootView.findViewById(R.id.activity_feedback_email);

        mRouterInfo = (EditText) rootView.findViewById(R.id.activity_feedback_router_information_content);

        //Load previously used email addr
        final String emailAddr;
        if (mGlobalPreferences.contains(MAONI_EMAIL)) {
            emailAddr = mGlobalPreferences.getString(MAONI_EMAIL, "");
        } else {
            //Set user-defined email if any
            emailAddr = mGlobalPreferences
                    .getString(RouterCompanionAppConstants.ACRA_USER_EMAIL, null);
        }
        mEmail.setText(emailAddr, TextView.BufferType.EDITABLE);

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
    public boolean validateForm(View view) {
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
