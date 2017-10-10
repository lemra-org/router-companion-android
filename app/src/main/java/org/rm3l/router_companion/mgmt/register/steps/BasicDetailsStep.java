package org.rm3l.router_companion.mgmt.register.steps;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.router_companion.RouterCompanionAppConstants.MAX_CUSTOM_ICON_SIZE_BYTES;
import static org.rm3l.router_companion.resources.conn.Router.RouterIcon_Auto;
import static org.rm3l.router_companion.resources.conn.Router.RouterIcon_Custom;
import static org.rm3l.router_companion.utils.Utils.toHumanReadableByteCount;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import com.crashlytics.android.Crashlytics;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import org.codepond.wizardroid.Wizard;
import org.codepond.wizardroid.persistence.ContextVariable;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.router_companion.mgmt.register.resources.RouterWizardAction;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.ReportingUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.ViewGroupUtils;
import org.rm3l.router_companion.widgets.wizard.MaterialWizardStep;

/**
 * Created by rm3l on 15/03/16.
 */
public class BasicDetailsStep extends MaterialWizardStep {

    private static final String LOG_TAG = BasicDetailsStep.class.getSimpleName();

    private static final int FILE_SELECTOR_REQUEST_CODE = 420;

    private boolean alreadyFilled;

    @ContextVariable
    private String customIconButtonHint;

    private Button customIconButtonView;

    @ContextVariable
    private String customIconErrorMsg;

    private TextView customIconErrorMsgView;

    @ContextVariable
    private String customIconPath;

    private TextView customIconPathView;

    private DDWRTCompanionDAO dao;

    private RadioGroup iconMethodRg;

    @ContextVariable
    private String isDemoModeStr;

    @ContextVariable
    private String routerFirmware;

    private Spinner routerFirmwareSpinner;

    @ContextVariable
    private String routerIconMethod;

    @ContextVariable
    private String routerIpOrDns;

    private EditText routerIpOrDnsEt;

    private TextInputLayout routerIpTil;

    @ContextVariable
    private String routerName;

    private EditText routerNameEt;

    private Router routerSelected = null;

    /**
     * Tell WizarDroid that these are context variables.
     * These values will be automatically bound to any field annotated with {@link ContextVariable}.
     * NOTE: Context Variable names are unique and therefore must
     * have the same name wherever you wish to use them.
     */
    @ContextVariable
    private String uuid;

    private TextView uuidTv;

    public static String getTitle() {
        return "Basic xxx";
    }

    //Wire the layout to the step
    public BasicDetailsStep() {
    }

    //Set your layout here
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        final Context context = getContext();
        this.dao = RouterManagementActivity.getDao(context);

        final Object tag = container.getTag();
        if (tag != null) {
            try {
                final RouterWizardAction routerWizardAction = RouterWizardAction.GSON_BUILDER.create()
                        .fromJson(tag.toString(), RouterWizardAction.class);
                routerSelected = dao.getRouter(routerWizardAction.getRouterUuid());
            } catch (final Exception e) {
                //No worries
                e.printStackTrace();
            }
        }

        load();

        final View rootView =
                inflater.inflate(R.layout.wizard_manage_router_1_basic_details_step, container, false);
        uuidTv = rootView.findViewById(R.id.router_add_uuid);
        routerNameEt = rootView.findViewById(R.id.router_add_name);

        routerIpOrDnsEt = rootView.findViewById(R.id.router_add_ip);
        routerIpTil = rootView.findViewById(R.id.router_add_ip_input_layout);

        final TextView demoText = rootView.findViewById(R.id.router_add_ip_demo_text);
        demoText.setText(
                demoText.getText().toString().replace("%PACKAGE_NAME%", BuildConfig.APPLICATION_ID));

        //DEMO Button
        rootView.findViewById(R.id.router_add_ip_demo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                routerIpOrDnsEt.setText(BuildConfig.APPLICATION_ID);
            }
        });

        //and set default values by using Context Variables
        uuidTv.setText(uuid);
        routerNameEt.setText(routerName);

        if (!TextUtils.isEmpty(routerIpOrDns)) {
            routerIpOrDnsEt.setText(routerIpOrDns);
        } else {
            if (TextUtils.isEmpty(routerIpOrDnsEt.getText())) {
                //Do this only if nothing has been filled in the EditText by the user
                final WifiManager wifiManager =
                        (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wifiManager != null) {
                    final DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
                    if (dhcpInfo != null) {
                        routerIpOrDns = Utils.decimalToIp4(dhcpInfo.gateway);
                        routerIpOrDnsEt.setText(routerIpOrDns);
                    }
                }
            }
        }

        routerFirmwareSpinner = rootView.findViewById(R.id.router_add_firmware);
        if (routerFirmware != null) {
            routerFirmwareSpinner.setSelection(
                    ViewGroupUtils.getSpinnerIndex(routerFirmwareSpinner, routerFirmware), true);
        }

        iconMethodRg = rootView.findViewById(R.id.router_add_icon_method);

        customIconErrorMsgView = rootView.findViewById(R.id.router_add_icon_custom_error_msg);
        customIconButtonView = rootView.findViewById(R.id.router_add_icon_custom_fileselector_btn);
        customIconPathView = rootView.findViewById(R.id.router_add_icon_custom_path);

        customIconErrorMsgView.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s)) {
                    customIconErrorMsgView.setVisibility(View.GONE);
                } else {
                    customIconErrorMsgView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        final View customIconContainer = rootView.findViewById(R.id.router_add_icon_custom_container);
        ((RadioGroup) rootView.findViewById(
                R.id.router_add_icon_method)).setOnCheckedChangeListener(
                new RadioGroup.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {

                        switch (checkedId) {
                            case R.id.router_add_icon_auto:
                                customIconPathView.setText(null);
                                customIconErrorMsgView.setText(null);
                                customIconContainer.setVisibility(View.GONE);
                                break;
                            case R.id.router_add_icon_custom:
                                customIconButtonView.setHint(getString(R.string.router_icon));
                                customIconContainer.setVisibility(View.VISIBLE);
                                break;
                            default:
                                break;
                        }
                    }
                });
        customIconButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Open up file picker

                // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
                // browser.
                final Intent intent = new Intent();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                } else {
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                }

                // Filter to only show results that can be "opened", such as a
                // file (as opposed to a list of contacts or timezones)
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                // search for all documents available via installed storage providers
                intent.setType("*/*");

                BasicDetailsStep.this.startActivityForResult(intent, FILE_SELECTOR_REQUEST_CODE);
            }
        });

        //and set default values by using Context Variables
        try {
            switch (Integer.parseInt(routerIconMethod)) {
                case RouterIcon_Auto:
                    iconMethodRg.check(R.id.router_add_icon_auto);
                    break;
                case Router.RouterIcon_Custom:
                    iconMethodRg.check(R.id.router_add_icon_custom);
                    break;
                default:
                    break;
            }
        } catch (final NumberFormatException nfe) {
            nfe.printStackTrace();
        }

        if (customIconButtonHint != null) {
            customIconButtonView.setHint(customIconButtonHint);
        } else {
            customIconButtonView.setHint("Select Router Icon");
        }
        customIconPathView.setText(customIconPath);
        customIconErrorMsgView.setText(customIconErrorMsg);

        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
            @Nullable Intent resultData) {
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == FILE_SELECTOR_REQUEST_CODE && resultCode == android.app.Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            Uri uri;
            if (resultData != null) {
                uri = resultData.getData();
                Crashlytics.log(Log.INFO, LOG_TAG, "Uri: " + uri.toString());
                Cursor uriCursor = null;

                try {
                    final ContentResolver contentResolver = this.getActivity().getContentResolver();

                    if (contentResolver == null
                            || (uriCursor = contentResolver.query(uri, null, null, null, null)) == null) {
                        customIconErrorMsgView.setText(
                                "Unknown Content Provider - please select a different location or auth method!");
                        return;
                    }

          /*
           * Get the column indexes of the data in the Cursor,
           * move to the first row in the Cursor, get the data,
           * and display it.
           */
                    final int nameIndex = uriCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    final int sizeIndex = uriCursor.getColumnIndex(OpenableColumns.SIZE);

                    uriCursor.moveToFirst();

                    //File size in bytes
                    final long fileSize = uriCursor.getLong(sizeIndex);
                    final String filename = uriCursor.getString(nameIndex);

                    //Check file size
                    if (fileSize > MAX_CUSTOM_ICON_SIZE_BYTES) {
                        customIconErrorMsgView.setText(
                                String.format("File '%s' too big (%s). Limit is %s", filename,
                                        toHumanReadableByteCount(fileSize),
                                        toHumanReadableByteCount(MAX_CUSTOM_ICON_SIZE_BYTES)));
                        return;
                    }

                    //Replace button hint message with file name
                    final CharSequence fileSelectorOriginalHint = customIconButtonView.getHint();
                    if (!Strings.isNullOrEmpty(filename)) {
                        customIconButtonView.setHint(filename);
                    }

                    //Set file actual content in hidden field
                    try {
                        customIconPathView.setText(
                                new String(ByteStreams.toByteArray(contentResolver.openInputStream(uri))));
                        customIconErrorMsgView.setText(null);
                    } catch (final Exception e) {
                        e.printStackTrace();
                        customIconErrorMsgView.setText("Error: " + e.getMessage());
                        customIconButtonView.setHint(fileSelectorOriginalHint);
                    }
                } finally {
                    if (uriCursor != null) {
                        try {
                            uriCursor.close();
                        } catch (final Exception e) {
                            e.printStackTrace();
                            ReportingUtils.reportException(null, e);
                        }
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, resultData);
    }

    @Override
    public String getWizardStepTitle() {
        return getTitle();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public Boolean validateStep(@Nullable final Wizard wizard) {
        //In a LAN, some names might be resolvable, but not valid DNS names.
        //So just make sure input data is not empty
        final String routerReachableAddr = routerIpOrDnsEt.getText().toString();
        final boolean stepValidated;
        if (routerReachableAddr == null || "".equals(routerReachableAddr.trim())) {
            routerIpTil.setErrorEnabled(true);
            routerIpTil.setError("Must not be blank");
            stepValidated = false;
        } else {
            routerIpTil.setErrorEnabled(false);
            stepValidated = true;
        }

        final int checkedIconMethodRadioButtonId = iconMethodRg.getCheckedRadioButtonId();
        switch (checkedIconMethodRadioButtonId) {
            case R.id.router_add_icon_custom: {
                //Check file submitted
                if (isNullOrEmpty(customIconPathView.getText().toString())) {
                    customIconErrorMsgView.setText("Please specify an image file");
                    return false;
                }
            }
            break;
            default:
                break;
        }

        return stepValidated;
    }

    protected void onExitNext() {
        //The values of these fields will be automatically stored in the wizard context
        //and will be populated in the next steps only if the same field names are used.
        uuid = uuidTv.getText().toString();
        routerName = routerNameEt.getText().toString();
        routerIpOrDns = routerIpOrDnsEt.getText().toString();
        routerFirmware = routerFirmwareSpinner.getSelectedItem().toString();
        isDemoModeStr = Boolean.toString(Utils.isDemoRouter(routerIpOrDns));
        final int checkedRadioButtonId = iconMethodRg.getCheckedRadioButtonId();
        switch (checkedRadioButtonId) {
            case R.id.router_add_icon_auto:
                routerIconMethod = Integer.toString(Router.RouterIcon_Auto);
                break;
            case R.id.router_add_icon_custom:
                routerIconMethod = Integer.toString(Router.RouterIcon_Custom);
                break;
            default:
                break;
        }
        customIconErrorMsg = customIconErrorMsgView.getText().toString();
        customIconPath = customIconPathView.getText().toString();
        customIconButtonHint = customIconButtonView.getHint().toString();
    }

    @Override
    protected void onVisibleToUser() {
        //Nothing to do - we are not reusing any context variable from previous steps
        load();
        if (isViewShown && !alreadyFilled) {
            uuidTv.setText(uuid);
            routerNameEt.setText(routerName);
            routerIpOrDnsEt.setText(routerIpOrDns);
            if (routerFirmware != null) {
                routerFirmwareSpinner.setSelection(
                        ViewGroupUtils.getSpinnerIndex(routerFirmwareSpinner, routerFirmware), true);
            }

            try {
                switch (Integer.parseInt(routerIconMethod)) {
                    case Router.RouterIcon_Auto:
                        iconMethodRg.check(R.id.router_add_icon_auto);
                        break;
                    case Router.SSHAuthenticationMethod_PASSWORD:
                        iconMethodRg.check(R.id.router_add_icon_custom);
                        break;
                    default:
                        break;
                }
            } catch (final NumberFormatException nfe) {
                nfe.printStackTrace();
            }
            if (customIconButtonHint != null) {
                customIconButtonView.setHint(customIconButtonHint);
            } else {
                customIconButtonView.setHint("Select Router Icon");
            }
            customIconPathView.setText(customIconPath);
            customIconErrorMsgView.setText(customIconErrorMsg);

            alreadyFilled = true;
        }
    }

    private void load() {
        if (routerSelected != null && !alreadyFilled) {
            this.uuid = routerSelected.getUuid();
            routerName = routerSelected.getName();
            routerIpOrDns = routerSelected.getRemoteIpAddress();
            if (routerSelected.getRouterFirmware() != null) {
                routerFirmware = routerSelected.getRouterFirmware().getDisplayName();
            }
            isDemoModeStr = Boolean.toString(Utils.isDemoRouter(routerSelected));

            this.customIconPath = routerSelected.getIconPath();
            this.customIconButtonHint = "File selected";
            switch (routerSelected.getIconMethod()) {
                case RouterIcon_Auto:
                    routerIconMethod = Integer.toString(Router.RouterIcon_Auto);
                    break;
                case RouterIcon_Custom:
                    routerIconMethod = Integer.toString(Router.RouterIcon_Custom);
                    break;
            }
        }
    }
}
