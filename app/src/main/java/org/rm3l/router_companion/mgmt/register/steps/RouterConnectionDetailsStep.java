package org.rm3l.router_companion.mgmt.register.steps;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.router_companion.RouterCompanionAppConstants.MAX_PRIVKEY_SIZE_BYTES;
import static org.rm3l.router_companion.utils.Utils.toHumanReadableByteCount;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputLayout;
import androidx.core.widget.NestedScrollView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import com.crashlytics.android.Crashlytics;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import java.util.Map;
import org.codepond.wizardroid.Wizard;
import org.codepond.wizardroid.persistence.ContextVariable;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.firmwares.impl.ddwrt.DDWRTFirmwareConnector;
import org.rm3l.router_companion.firmwares.impl.ddwrt.DDWRTFirmwareConnectorKt;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.router_companion.mgmt.register.resources.RouterWizardAction;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.ReportingUtils;
import org.rm3l.router_companion.utils.ViewGroupUtils;
import org.rm3l.router_companion.widgets.wizard.MaterialWizard;
import org.rm3l.router_companion.widgets.wizard.MaterialWizardStep;

/**
 * Created by rm3l on 15/03/16.
 */
public class RouterConnectionDetailsStep extends MaterialWizardStep {

    private static final String LOG_TAG = RouterConnectionDetailsStep.class.getSimpleName();

    private static final int READ_REQUEST_CODE = 42;

    private boolean alreadyFilled;

    @ContextVariable
    private String authMethod;

    private RadioGroup authMethodRg;

    @ContextVariable
    private String connectionProtocol;

    private Spinner connectionProtocolView;

    private DDWRTCompanionDAO dao;

    @ContextVariable
    private String password;

    @ContextVariable
    private String port;

    private EditText portEt;

    @ContextVariable
    private String privkeyButtonHint;

    private Button privkeyButtonView;

    @ContextVariable
    private String privkeyErrorMsg;

    private TextView privkeyErrorMsgView;

    @ContextVariable
    private String privkeyPath;

    private TextView privkeyPathView;

    private EditText pwdView;

    private View rootView;

    private Router routerSelected;

    @ContextVariable
    private String username;

    private TextInputLayout usernameTIL;
    private EditText usernameEt;

    private String routerFirmware;

    //Wire the layout to the step
    public RouterConnectionDetailsStep() {
    }

    //Set your layout here
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        this.dao = RouterManagementActivity.getDao(getContext());

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

        rootView =
                inflater.inflate(R.layout.wizard_manage_router_2_router_connection_details_step, container,
                        false);

        usernameTIL = rootView.findViewById(R.id.router_add_username_input_layout);
        usernameEt = (EditText) rootView.findViewById(R.id.router_add_username);
        usernameEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            }

            @Override
            public void afterTextChanged(final Editable s) {
                if (TextUtils.isEmpty(s)) {
                    usernameTIL.setErrorTextAppearance(R.style.InputLayoutErrorHint);
                    usernameTIL.setErrorEnabled(true);
                    usernameTIL.setError(getString(R.string.router_add_username_invalid));
                } else {
                    handleSSHLoginEditText();
                }
            }
        });
        portEt = (EditText) rootView.findViewById(R.id.router_add_port);
        connectionProtocolView = (Spinner) rootView.findViewById(R.id.router_add_proto);
        authMethodRg = (RadioGroup) rootView.findViewById(R.id.router_add_ssh_auth_method);

        pwdView = (EditText) rootView.findViewById(R.id.router_add_password);

        final CheckBox pwdShowCheckBox =
                (CheckBox) rootView.findViewById(R.id.router_add_password_show_checkbox);

        pwdShowCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    pwdView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    pwdView.requestFocus();
                } else {
                    pwdView.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    pwdView.requestFocus();
                }
                pwdView.setSelection(pwdView.length());
            }
        });

        privkeyErrorMsgView = (TextView) rootView.findViewById(R.id.router_add_privkey_error_msg);
        privkeyButtonView = (Button) rootView.findViewById(R.id.router_add_privkey);
        privkeyPathView = (TextView) rootView.findViewById(R.id.router_add_privkey_path);

        privkeyErrorMsgView.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s)) {
                    privkeyErrorMsgView.setVisibility(View.GONE);
                } else {
                    privkeyErrorMsgView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        final View privkeyHdrView = rootView.findViewById(R.id.router_add_privkey_hdr);
        final TextInputLayout pwdInputLayout =
                (TextInputLayout) rootView.findViewById(R.id.router_add_password_input_layout);

        ((RadioGroup) rootView.findViewById(
                R.id.router_add_ssh_auth_method)).setOnCheckedChangeListener(
                new RadioGroup.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {

                        if (checkedId == R.id.router_add_ssh_auth_method_none) {
                            privkeyPathView.setText(null);
                            privkeyHdrView.setVisibility(View.GONE);
                            privkeyButtonView.setVisibility(View.GONE);
                            privkeyErrorMsgView.setVisibility(View.GONE);
                            pwdView.setText(null);
                            pwdView.setVisibility(View.GONE);
                            pwdShowCheckBox.setVisibility(View.GONE);
                            pwdInputLayout.setErrorEnabled(false);
                            privkeyErrorMsgView.setText(null);

                        } else if (checkedId == R.id.router_add_ssh_auth_method_password) {
                            privkeyPathView.setText(null);
                            privkeyHdrView.setVisibility(View.GONE);
                            privkeyButtonView.setVisibility(View.GONE);
                            privkeyErrorMsgView.setText(null);
                            pwdView.setVisibility(View.VISIBLE);
                            //                                pwdView.setHint("e.g., 'default' (may be empty) ");
                            pwdShowCheckBox.setVisibility(View.VISIBLE);
                            pwdInputLayout.setErrorEnabled(false);

                        } else if (checkedId == R.id.router_add_ssh_auth_method_privkey) {
                            pwdView.setText(null);
                            privkeyButtonView.setHint(getString(R.string.router_add_path_to_privkey));
                            pwdView.setVisibility(View.VISIBLE);
                            //                                pwdView.setHint("Key passphrase, if applicable");
                            pwdShowCheckBox.setVisibility(View.VISIBLE);
                            privkeyHdrView.setVisibility(View.VISIBLE);
                            privkeyButtonView.setVisibility(View.VISIBLE);
                            pwdInputLayout.setErrorEnabled(false);

                        } else {
                        }
                    }
                });

        privkeyButtonView.setOnClickListener(new View.OnClickListener() {
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

                RouterConnectionDetailsStep.this.startActivityForResult(intent, READ_REQUEST_CODE);
            }
        });

        load();

        //and set default values by using Context Variables
        try {
            switch (Integer.parseInt(authMethod)) {
                case Router.SSHAuthenticationMethod_NONE:
                    authMethodRg.check(R.id.router_add_ssh_auth_method_none);
                    break;
                case Router.SSHAuthenticationMethod_PASSWORD:
                    authMethodRg.check(R.id.router_add_ssh_auth_method_password);
                    break;
                case Router.SSHAuthenticationMethod_PUBLIC_PRIVATE_KEY:
                    authMethodRg.check(R.id.router_add_ssh_auth_method_privkey);
                    break;
                default:
                    break;
            }
        } catch (final NumberFormatException nfe) {
            nfe.printStackTrace();
        }

        usernameEt.setText(username != null ? username : "root");
        portEt.setText(port != null ? port : "22");
        if (password != null) {
            pwdView.setText(password);
        }

        if (privkeyButtonHint != null) {
            privkeyButtonView.setHint(privkeyButtonHint);
        } else {
            privkeyButtonView.setHint("Select SSH Private Key (if any)");
        }
        privkeyPathView.setText(privkeyPath);
        privkeyErrorMsgView.setText(privkeyErrorMsg);

        if (connectionProtocol != null) {
            connectionProtocolView.setSelection(
                    ViewGroupUtils.getSpinnerIndex(connectionProtocolView, connectionProtocol), true);
        }

        return rootView;
    }

    /**
     * Receive the result from a previous call to
     * {@link #startActivityForResult(android.content.Intent, int)}.  This follows the
     * related Activity API as described there in
     * {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}.
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param resultData  An Intent, which can return result data to the caller
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode,
            @Nullable Intent resultData) {
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
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
                        privkeyErrorMsgView.setText(
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
                    if (fileSize > MAX_PRIVKEY_SIZE_BYTES) {
                        privkeyErrorMsgView.setText(
                                String.format("File '%s' too big (%s). Limit is %s", filename,
                                        toHumanReadableByteCount(fileSize),
                                        toHumanReadableByteCount(MAX_PRIVKEY_SIZE_BYTES)));
                        return;
                    }

                    //Replace button hint message with file name
                    final CharSequence fileSelectorOriginalHint = privkeyButtonView.getHint();
                    if (!Strings.isNullOrEmpty(filename)) {
                        privkeyButtonView.setHint(filename);
                    }

                    //Set file actual content in hidden field
                    try {
                        privkeyPathView.setText(
                                new String(ByteStreams.toByteArray(contentResolver.openInputStream(uri))));
                        //privkeyPathView.setText(IOUtils.toString(contentResolver.openInputStream(uri)));
                        privkeyErrorMsgView.setText(null);
                    } catch (final Exception e) {
                        e.printStackTrace();
                        privkeyErrorMsgView.setText("Error: " + e.getMessage());
                        privkeyButtonView.setHint(fileSelectorOriginalHint);
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
        return "Connection Details";
    }

    @Override
    public Boolean validateStep(Wizard wizard) {
        //        final FragmentActivity activity = getActivity();
        final NestedScrollView contentScrollView =
                (NestedScrollView) rootView.findViewById(R.id.router_add_content_scroll_view);

        boolean validPort;
        final TextInputLayout portInputLayout =
                (TextInputLayout) rootView.findViewById(R.id.router_add_port_input_layout);
        try {
            final String portStr = portEt.getText().toString();
            validPort = (!isNullOrEmpty(portStr) && (Integer.parseInt(portStr) > 0));
        } catch (@NonNull final Exception e) {
            e.printStackTrace();
            validPort = false;
        }
        portInputLayout.setErrorEnabled(!validPort);
        if (!validPort) {
            portInputLayout.setError(getString(R.string.router_add_port_invalid));
            //            Utils.scrollToView(contentScrollView, portEt);
            //            portEt.requestFocus();
            //            openKeyboard(activity, portEt);
            return false;
        }
        portInputLayout.setErrorEnabled(false);

        final TextInputLayout sshLoginInputLayout =
                (TextInputLayout) rootView.findViewById(R.id.router_add_username_input_layout);
        if (isNullOrEmpty(usernameEt.getText().toString())) {
            usernameTIL.setErrorTextAppearance(R.style.InputLayoutErrorHint);
            sshLoginInputLayout.setErrorEnabled(true);
            sshLoginInputLayout.setError(getString(R.string.router_add_username_invalid));
            //            Utils.scrollToView(contentScrollView, usernameEt);
            //            usernameEt.requestFocus();
            //            openKeyboard(activity, usernameEt);
            return false;
        }
        sshLoginInputLayout.setErrorEnabled(false);

        final int checkedAuthMethodRadioButtonId = authMethodRg.getCheckedRadioButtonId();
        if (checkedAuthMethodRadioButtonId == R.id.router_add_ssh_auth_method_privkey) {
            if (isNullOrEmpty(privkeyPathView.getText().toString())) {
                privkeyErrorMsgView.setText(getString(R.string.router_add_privkey_invalid));
                //                    privkeyButtonView.requestFocus();
                return false;
            }

        } else {
        }

        return true;
    }

    protected void onExitNext() {
        //The values of these fields will be automatically stored in the wizard context
        //and will be populated in the next steps only if the same field names are used.
        username = usernameEt.getText().toString();
        password = pwdView.getText().toString();
        port = portEt.getText().toString();
        final int checkedRadioButtonId = authMethodRg.getCheckedRadioButtonId();
        if (checkedRadioButtonId == R.id.router_add_ssh_auth_method_none) {
            authMethod = Integer.toString(Router.SSHAuthenticationMethod_NONE);

        } else if (checkedRadioButtonId == R.id.router_add_ssh_auth_method_password) {
            authMethod = Integer.toString(Router.SSHAuthenticationMethod_PASSWORD);

        } else if (checkedRadioButtonId == R.id.router_add_ssh_auth_method_privkey) {
            authMethod = Integer.toString(Router.SSHAuthenticationMethod_PUBLIC_PRIVATE_KEY);

        } else {
        }
        privkeyErrorMsg = privkeyErrorMsgView.getText().toString();
        privkeyPath = privkeyPathView.getText().toString();
        privkeyButtonHint = privkeyButtonView.getHint().toString();
        connectionProtocol = connectionProtocolView.getSelectedItem().toString();
    }

    private void loadFromWizardContext() {
        final Map wizardContext = MaterialWizard.getWizardContext(getContext());
        final Object routerFirmwareObj = wizardContext.get("routerFirmware");
        routerFirmware = routerFirmwareObj != null ? routerFirmwareObj.toString() : null;
    }

    @Override
    protected void onVisibleToUser() {
        //Nothing to do - we are not re-using any context variable field from previous steps
        loadFromWizardContext();
        load();
        if (isViewShown && !alreadyFilled) {
            alreadyFilled = true;
            try {
                switch (Integer.parseInt(authMethod)) {
                    case Router.SSHAuthenticationMethod_NONE:
                        authMethodRg.check(R.id.router_add_ssh_auth_method_none);
                        break;
                    case Router.SSHAuthenticationMethod_PASSWORD:
                        authMethodRg.check(R.id.router_add_ssh_auth_method_password);
                        break;
                    case Router.SSHAuthenticationMethod_PUBLIC_PRIVATE_KEY:
                        authMethodRg.check(R.id.router_add_ssh_auth_method_privkey);
                        break;
                    default:
                        break;
                }
            } catch (final NumberFormatException nfe) {
                nfe.printStackTrace();
            }
            usernameEt.setText(username != null ? username : "root");
            portEt.setText(port != null ? port : "22");
            if (password != null) {
                pwdView.setText(password);
            }
            if (privkeyButtonHint != null) {
                privkeyButtonView.setHint(privkeyButtonHint);
            } else {
                privkeyButtonView.setHint("Select SSH Private Key (if any)");
            }
            privkeyPathView.setText(privkeyPath);
            privkeyErrorMsgView.setText(privkeyErrorMsg);
            if (connectionProtocol != null) {
                connectionProtocolView.setSelection(
                        ViewGroupUtils.getSpinnerIndex(connectionProtocolView, connectionProtocol), true);
            }
            handleSSHLoginEditText();
        }
    }

    private void load() {
        if (routerSelected != null && !alreadyFilled) {
            this.connectionProtocol = routerSelected.getRouterConnectionProtocol().toString();
            this.username = routerSelected.getUsernamePlain();
            handleSSHLoginEditText();
            this.password = routerSelected.getPasswordPlain();
            this.port = Integer.toString(routerSelected.getRemotePort());
            this.privkeyPath = routerSelected.getPrivKeyPlain();
            this.privkeyButtonHint = "File selected";
            switch (routerSelected.getSshAuthenticationMethod()) {
                case NONE:
                    authMethod = Integer.toString(Router.SSHAuthenticationMethod_NONE);
                    break;
                case PASSWORD:
                    authMethod = Integer.toString(Router.SSHAuthenticationMethod_PASSWORD);
                    break;
                case PUBLIC_PRIVATE_KEY:
                    authMethod = Integer.toString(Router.SSHAuthenticationMethod_PUBLIC_PRIVATE_KEY);
                    break;
            }
        }
    }

    private void handleSSHLoginEditText() {
        if ("DD-WRT".equalsIgnoreCase(routerFirmware)) {
            //Read-only + hint
            this.username = "root";
            this.usernameTIL.setErrorTextAppearance(R.style.InputLayoutInfoHint);
            this.usernameTIL.setError(DDWRTFirmwareConnectorKt.SSH_LOGIN_ALWAYS_ROOT_MESSAGE);
            this.usernameTIL.setErrorEnabled(true);
            this.usernameEt.setEnabled(false);
        } else if ("-Auto-detect-".equalsIgnoreCase(routerFirmware)) {
            //Just a hint
            this.usernameTIL.setErrorTextAppearance(R.style.InputLayoutInfoHint);
            this.usernameTIL.setError(DDWRTFirmwareConnectorKt.SSH_LOGIN_ALWAYS_ROOT_MESSAGE);
            this.usernameTIL.setErrorEnabled(true);
            this.usernameEt.setEnabled(true);
        } else {
            //Hide hint
            this.usernameTIL.setErrorTextAppearance(R.style.InputLayoutErrorHint);
            this.usernameTIL.setError(null);
            this.usernameTIL.setErrorEnabled(false);
            this.usernameEt.setEnabled(true);
        }
    }
}
