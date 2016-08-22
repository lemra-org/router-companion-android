package org.rm3l.ddwrt.tasker.ui.activity.action;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.twofortyfouram.locale.sdk.client.ui.activity.AbstractAppCompatPluginActivity;
import com.twofortyfouram.log.Lumberjack;

import net.jcip.annotations.NotThreadSafe;

import org.rm3l.ddwrt.common.IDDWRTCompanionService;
import org.rm3l.ddwrt.common.resources.RouterInfo;
import org.rm3l.ddwrt.tasker.Constants;
import org.rm3l.ddwrt.tasker.R;
import org.rm3l.ddwrt.tasker.bundle.PluginBundleValues;
import org.rm3l.ddwrt.tasker.feedback.maoni.FeedbackHandler;
import org.rm3l.ddwrt.tasker.utils.Utils;
import org.rm3l.maoni.Maoni;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.widget.TextView.BufferType.*;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.ddwrt.tasker.bundle.PluginBundleValues.*;

@NotThreadSafe
public class ActionEditActivity extends AbstractAppCompatPluginActivity {

    public static final String DDWRT_COMPANION_SERVICE_NAME = "org.rm3l.ddwrt.IDDWRTCompanionService";
    /** Service to which this client will bind */
    private IDDWRTCompanionService ddwrtCompanionService;

    private String ddwrtCompanionAppPackage;

    /** Connection to the service (inner class) */
    private RouterServiceConnection conn;

    private ArrayAdapter<String> mRoutersListAdapter;

    private TextView mErrorPlaceholder;
    private ProgressBar mLoadingView;
    private View mMainContentView;

    private TextView mSelectedRouterUuid;
    private Spinner mRoutersDropdown;

    private Spinner mCommandsDropdown;
    private EditText mCommandConfiguration;
    private EditText mSelectedRouterVariable;
    private CheckBox mCommandConfigurationVariable;
    private EditText mReturnOutputVariable;
    private CheckBox mReturnOutputCheckbox;
    private EditText mCommandParamEditText;
    private CheckBox mCommandParamVariable;
    private SupportedCommand mCommand;

    private Button mFileSelectionButton;

    private String mSelectedRouterReadableName;

    
    private int mPreviousBundleIntVersionCode;
    private boolean mPreviousBundleRouterIsVariable;
    private String mPreviousBundleRouterUuid;
    private String mPreviousBundleRouterCanonicalReadableName;
    private boolean mPreviousBundleCommandIsCustom;
    private boolean mPreviousBundleCommandCustomIsVariable;
    private String mPreviousBundleCommandCustomVariableName;
    private String mPreviousBundleCommandCustomCmd;
    private String mPreviousBundleCommandSupportedName;
    private String mPreviousBundleCommandSupportedReadableName;
    private String mPreviousBundleCommandSupportedParamHint;
    private String mPreviousBundleCommandSupportedParam;
    private boolean mPreviousBundleCommandSupportedParamIsVariable;
    private String mPreviousBundleCommandSupportedParamVariableName;
    private boolean mPreviousBundleOutputIsVariable;
    private String mPreviousBundleOutputVariableName;
    private String mPreviousBundleRouterVariableName;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        final PackageManager packageManager = getPackageManager();

        /*
         * To help the user keep context, the title shows the host's name and the subtitle
         * shows the plug-in's name.
         */
        CharSequence callingApplicationLabel = null;
        try {
            callingApplicationLabel =
                    packageManager.getApplicationLabel(
                            packageManager.getApplicationInfo(getCallingPackage(),
                                    0));
        } catch (final PackageManager.NameNotFoundException e) {
            Lumberjack.e("Calling package couldn't be found: %s", e); //$NON-NLS-1$
        }

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitleTextAppearance(getApplicationContext(), R.style.ToolbarTitle);
            toolbar.setSubtitleTextAppearance(getApplicationContext(), R.style.ToolbarSubtitle);
            toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
            toolbar.setSubtitleTextColor(ContextCompat.getColor(this, R.color.white));
            toolbar.setTitle(callingApplicationLabel != null ? callingApplicationLabel : null);
            toolbar.setSubtitle(R.string.plugin_name);
            setSupportActionBar(toolbar);
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        mErrorPlaceholder = (TextView) findViewById(R.id.error_placeholder);
        mErrorPlaceholder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(ActionEditActivity.this,
                        mErrorPlaceholder.getText(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        mLoadingView = (ProgressBar) findViewById(R.id.loading_view);
        mMainContentView = findViewById(R.id.main_content_view);

        mSelectedRouterUuid = (TextView) findViewById(R.id.selected_router_uuid);
        mRoutersDropdown = (Spinner) findViewById(R.id.select_router_dropdown);

        mCommandsDropdown = (Spinner) findViewById(R.id.select_command_dropdown);
        mCommandConfiguration = (EditText) findViewById(R.id.command_configuration_input);

        mSelectedRouterVariable = (EditText) findViewById(R.id.selected_router_variable);

        mCommandConfigurationVariable = (CheckBox) findViewById(R.id.command_configuration_input_variable);
        mCommandConfigurationVariable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean newValue) {
                if (newValue) {
                    mCommandConfiguration.setHint("e.g, %command");
                } else {
                    mCommandConfiguration.setHint("Configure your command here");
                }
            }
        });

        mReturnOutputVariable = (EditText) findViewById(R.id.return_output_variable);
        mReturnOutputCheckbox = (CheckBox) findViewById(R.id.return_output_variable_checkbox);

        mReturnOutputCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean newValue) {
                mReturnOutputVariable.setText(null, EDITABLE);
                mReturnOutputVariable.setVisibility(newValue ? View.VISIBLE : View.GONE);
            }
        });
        
        mCommandParamEditText = (EditText) findViewById(R.id.command_configuration_input_param);

        mCommandParamVariable = (CheckBox)
                findViewById(R.id.command_configuration_input_param_variable);

        mCommandParamVariable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    mCommandConfigurationVariable.setVisibility(View.GONE);
                }
            }
        });

        mFileSelectionButton = (Button) findViewById(R.id.custom_cmd_file_selection_button);
//        mFileSelectionErrorTextView = (TextView) findViewById(R.id.custom_cmd_file_error_msg);
//        mSelectedFilePathTextView = (TextView) findViewById(R.id.custom_cmd_file_path);


        this.ddwrtCompanionAppPackage = Utils.getDDWRTCompanionAppPackage(packageManager);
        Crashlytics.log(Log.DEBUG, Constants.TAG,
                "ddwrtCompanionAppPackage=" + ddwrtCompanionAppPackage);

        if (ddwrtCompanionAppPackage == null) {
            mErrorPlaceholder.setText("You must install DD-WRT Companion App !");
            mErrorPlaceholder.setVisibility(View.VISIBLE);
            //TODO Add button that opens up the Play Store
            return;
        }

        // connect to the service
        conn = new RouterServiceConnection();

        // name must match the service's Intent filter in the Service Manifest file
        final Intent intent = new Intent(DDWRT_COMPANION_SERVICE_NAME);
        intent.setPackage(ddwrtCompanionAppPackage);
        // bind to the Service, create it if it's not already there
        bindService(intent, conn, Context.BIND_AUTO_CREATE);

    }

    @Override
    public void onPostCreateWithPreviousResult(@NonNull final Bundle previousBundle,
                                               @NonNull final String previousBlurb) {

        Crashlytics.log(Log.DEBUG, Constants.TAG, "previousBundle: " + previousBundle);

        mPreviousBundleIntVersionCode = previousBundle.getInt(BUNDLE_EXTRA_INT_VERSION_CODE);
        mPreviousBundleRouterIsVariable = previousBundle.getBoolean(BUNDLE_ROUTER_IS_VARIABLE, false);
        mPreviousBundleRouterVariableName = previousBundle.getString(BUNDLE_ROUTER_VARIABLE_NAME);
        mPreviousBundleRouterUuid = previousBundle.getString(BUNDLE_ROUTER_UUID);
        mPreviousBundleRouterCanonicalReadableName = previousBundle.getString(BUNDLE_ROUTER_CANONICAL_READABLE_NAME);
        
        mPreviousBundleCommandIsCustom = previousBundle.getBoolean(BUNDLE_COMMAND_IS_CUSTOM, false);
        mPreviousBundleCommandCustomIsVariable = previousBundle.getBoolean(BUNDLE_COMMAND_CUSTOM_IS_VARIABLE, false);
        mPreviousBundleCommandCustomVariableName = previousBundle.getString(BUNDLE_COMMAND_CUSTOM_VARIABLE_NAME);
        mPreviousBundleCommandCustomCmd = previousBundle.getString(BUNDLE_COMMAND_CUSTOM_CMD);
        mPreviousBundleCommandSupportedName = previousBundle.getString(BUNDLE_COMMAND_SUPPORTED_NAME);
        mPreviousBundleCommandSupportedReadableName = previousBundle.getString(BUNDLE_COMMAND_SUPPORTED_READABLE_NAME);
        mPreviousBundleCommandSupportedParamHint = previousBundle.getString(BUNDLE_COMMAND_SUPPORTED_PARAM_HINT);
        mPreviousBundleCommandSupportedParam = previousBundle.getString(BUNDLE_COMMAND_SUPPORTED_PARAM);
        mPreviousBundleCommandSupportedParamIsVariable =
                previousBundle.getBoolean(BUNDLE_COMMAND_SUPPORTED_PARAM_IS_VARIABLE);
        mPreviousBundleCommandSupportedParamVariableName = previousBundle
                .getString(BUNDLE_COMMAND_SUPPORTED_PARAM_VARIABLE_NAME);

        mPreviousBundleOutputIsVariable = previousBundle.getBoolean(BUNDLE_OUTPUT_IS_VARIABLE, false);
        mPreviousBundleOutputVariableName = previousBundle.getString(BUNDLE_OUTPUT_VARIABLE_NAME);

        //Reconnect to the remote service
        unbindService(conn);
        ddwrtCompanionService = null;

        // connect to the service
        conn = new RouterServiceConnection();

        // name must match the service's Intent filter in the Service Manifest file
        final Intent intent = new Intent(DDWRT_COMPANION_SERVICE_NAME);
        intent.setPackage(ddwrtCompanionAppPackage);
        // bind to the Service, create it if it's not already there
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean isBundleValid(@NonNull final Bundle bundle) {
        return PluginBundleValues.isBundleValid(bundle);
    }

    @Nullable
    @Override
    public Bundle getResultBundle() {
        final boolean isVariableRouter = TextUtils.isEmpty(mSelectedRouterUuid.getText());
        final boolean isCustomCommand = (mCommand == null);
        return PluginBundleValues.generateBundle(getApplicationContext(),
                
                isVariableRouter,
                mSelectedRouterVariable.getText(),
                mSelectedRouterUuid.getText(),
                mSelectedRouterReadableName,

                isCustomCommand,
                mCommandConfigurationVariable.isChecked(),
                mCommandConfiguration.getText(),
                mCommand,
                mCommandParamEditText.getText(),
                mCommandParamVariable.isChecked(),
                
                mReturnOutputCheckbox.isChecked(),
                mReturnOutputVariable.getText());
    }

    @NonNull
    @Override
    public String getResultBlurb(@NonNull final Bundle bundle) {
        return PluginBundleValues.getBundleBlurb(bundle);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_action_edit_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Signal to AbstractAppCompatPluginActivity that the user canceled.
                mIsCancelled = true;
                onBackPressed();
                break;

            case R.id.menu_refresh:
                mLoadingView.setVisibility(View.VISIBLE);
                mMainContentView.setEnabled(false);
                //Reconnect to the remote service
                unbindService(conn);
                ddwrtCompanionService = null;

                // connect to the service
                conn = new RouterServiceConnection();

                // name must match the service's Intent filter in the Service Manifest file
                final Intent intent = new Intent(DDWRT_COMPANION_SERVICE_NAME);
                intent.setPackage(ddwrtCompanionAppPackage);
                // bind to the Service, create it if it's not already there
                bindService(intent, conn, Context.BIND_AUTO_CREATE);
                break;

            case R.id.ddwrt_companion_tasker_feedback:
                new Maoni.Builder(Constants.FILEPROVIDER_AUTHORITY)
                        .withTheme(R.style.AppThemeLight_StatusBarTransparent)
                        .withWindowTitle("Send Feedback")
                        .withExtraLayout(R.layout.activity_feedback_maoni)
                        .withHandler(new FeedbackHandler(this))
                        .build()
                        .start(this);
                break;
            case R.id.ddwrt_companion_tasker_about:
                new LibsBuilder()
                        .withActivityTitle("About")
                        //provide a style (optional) (LIGHT, DARK, LIGHT_DARK_TOOLBAR)
                        .withActivityStyle(Libs.ActivityStyle.LIGHT)
                        //start the activity
                        .start(this);
                break;
            case R.id.menu_discard_changes:
                // Signal to AbstractAppCompatPluginActivity that the user canceled.
                mIsCancelled = true;
                finish();
                break;
            case R.id.menu_save_changes:
                mIsCancelled = false;
                finish();
                break;
            default:
                break;
        }

        return true;
    }

    /** Clean up before Activity is destroyed */
    @Override
    protected void onDestroy() {
        // Signal to AbstractAppCompatPluginActivity that the user canceled.
        mIsCancelled = true;
        super.onDestroy();
        unbindService(conn);
        ddwrtCompanionService = null;
    }

    /** Inner class used to connect to UserDataService */
    class RouterServiceConnection implements ServiceConnection {

        /** is called once the bind succeeds */
        public void onServiceConnected(ComponentName name, IBinder service) {
            Crashlytics.log(Log.DEBUG, Constants.TAG, "Service connected");
            ddwrtCompanionService = IDDWRTCompanionService.Stub.asInterface(service);

            mErrorPlaceholder.setVisibility(View.GONE);
            mLoadingView.setVisibility(View.GONE);
            mMainContentView.setVisibility(View.VISIBLE);
            mMainContentView.setEnabled(true);

            final List<RouterInfo> allRouters;
            try {
                allRouters = ddwrtCompanionService.getAllRouters();
            } catch (RemoteException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
                Toast.makeText(ActionEditActivity.this, "Internal Error - please try again later", Toast.LENGTH_SHORT)
                        .show();
                finish();
                return;
            }

            if (allRouters == null || allRouters.isEmpty()) {
                //Error - redirect to "Launch DD-WRT Companion to add a new Router"
                mErrorPlaceholder.setText("Open DD-WRT Companion to register routers");
                mErrorPlaceholder.setVisibility(View.VISIBLE);
                return;
            }
            mErrorPlaceholder.setVisibility(View.GONE);

            final String[] routersNamesArray = new String[allRouters.size() + 1];
            int i = 0;
            Integer selectedRouterIndex = null;
            for (final RouterInfo router : allRouters) {
                if (mPreviousBundleRouterUuid != null &&
                        mPreviousBundleRouterUuid.equals(router.getUuid())) {
                    selectedRouterIndex = i;
                }
                final String routerName = router.getName();
                routersNamesArray[i++] = ((isNullOrEmpty(routerName) ? "-" : routerName) + "\n(" +
                        router.getRemoteIpAddress() + ":" + router.getRemotePort() + ")");
            }
            routersNamesArray[i] = "-- VARIABLE --";

            mRoutersListAdapter = new ArrayAdapter<>(ActionEditActivity.this,
                    R.layout.spinner_item, new ArrayList<>(Arrays.asList(routersNamesArray)));
            mRoutersListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            mRoutersDropdown.setAdapter(mRoutersListAdapter);

            mRoutersDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                    if (position == routersNamesArray.length - 1) {
                        //Variable
                        mSelectedRouterVariable.setVisibility(View.VISIBLE);
                        mSelectedRouterVariable.setText("%router_name");
                        mSelectedRouterUuid.setText(null);
                        mSelectedRouterReadableName = null;
                    } else {
                        mSelectedRouterVariable.setVisibility(View.GONE);
                        mSelectedRouterVariable.setText(null);
                        final RouterInfo routerInfo = allRouters.get(position);
                        if (routerInfo == null) {
                            return;
                        }
                        mSelectedRouterUuid.setText(routerInfo.getUuid());
                        mSelectedRouterReadableName = String.format("%s (%s)",
                                TextUtils.isEmpty(routerInfo.getName()) ? "-" : routerInfo.getName(),
                                routerInfo.isDemoRouter() ? "DEMO" :
                                        String.format("%s:%d",
                                                routerInfo.getRemoteIpAddress(),
                                                routerInfo.getRemotePort()));
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            mRoutersDropdown.setSelection(
                    (selectedRouterIndex != null && selectedRouterIndex < routersNamesArray.length)
                            ? selectedRouterIndex : 0);

            if (mPreviousBundleRouterIsVariable) {
                mRoutersDropdown.setSelection(i);
                mSelectedRouterVariable
                        .setText(mPreviousBundleRouterVariableName, EDITABLE);
            }


            final SupportedCommand[] supportedCommands = SupportedCommand.values();
            final String[] supportedCommandsArr = new String[supportedCommands.length];
            int j = 0;
            Integer selectedCommandIndex = null;

            for (final SupportedCommand cmd : supportedCommands) {
                if (cmd.name().equals(mPreviousBundleCommandSupportedName)) {
                    selectedCommandIndex = j;
                }
                supportedCommandsArr[j++] = cmd.humanReadableName;
            }
            final ArrayAdapter<String> cmdAdapter = new ArrayAdapter<>(ActionEditActivity.this,
                    R.layout.spinner_item, supportedCommandsArr);
            cmdAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mCommandsDropdown.setAdapter(cmdAdapter);

            mCommandConfiguration.setVisibility(View.GONE);

            mCommandParamVariable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (mCommand != null) {
                        if (TextUtils.isEmpty(mCommand.paramHumanReadableHint)) {
                            mCommandParamEditText.setHint("Variable Name");
                        } else {
                            if (b) {
                                mCommandParamEditText
                                        .setHint("Variable for '" + mCommand.paramHumanReadableHint + "'");
                            } else {
                                mCommandParamEditText.setHint(mCommand.paramHumanReadableHint);
                            }
                        }
                    }
                }
            });

            mCommandsDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                    final SupportedCommand supportedCommand = supportedCommands[position];
                    if (supportedCommand == null) {
                        return;
                    }
                    switch (supportedCommand) {
                        case CUSTOM_COMMAND:
                            mCommandConfigurationVariable.setChecked(mPreviousBundleCommandCustomIsVariable);
                            mCommandConfiguration.setVisibility(View.VISIBLE);
                            mCommandConfigurationVariable.setVisibility(View.VISIBLE);
                            mCommandParamEditText.setText(null);
                            mCommandParamEditText.setHint("Command Input");
                            mCommandParamEditText.setVisibility(View.GONE);
                            mCommandParamVariable.setVisibility(View.GONE);
                            mCommand = null;
                            break;
                        default:
                            mCommandConfiguration.setVisibility(View.GONE);
                            mCommandConfigurationVariable.setChecked(false);
                            mCommandConfigurationVariable.setVisibility(View.GONE);
                            mCommand = supportedCommand;
                            if (!TextUtils.isEmpty(supportedCommand.paramName)) {
                                mCommandParamEditText.setHint(supportedCommand.paramHumanReadableHint);
                                mCommandParamEditText.setVisibility(View.VISIBLE);
                                mCommandParamVariable.setVisibility(View.VISIBLE);
                            } else {
                                mCommandParamEditText.setVisibility(View.GONE);
                                mCommandParamVariable.setVisibility(View.GONE);
                            }
                            break;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            if (selectedCommandIndex != null && selectedCommandIndex < supportedCommandsArr.length) {
                mCommandsDropdown.setSelection(selectedCommandIndex);
            }

            mCommandConfigurationVariable.setChecked(mPreviousBundleCommandCustomIsVariable);

            Crashlytics.log(Log.DEBUG, Constants.TAG,
                    "mPreviousBundleCommandCustomIsVariable: " + mPreviousBundleCommandCustomIsVariable);
            Crashlytics.log(Log.DEBUG, Constants.TAG,
                    "mPreviousBundleCommandCustomVariableName: " + mPreviousBundleCommandCustomVariableName);
            Crashlytics.log(Log.DEBUG, Constants.TAG,
                    "mPreviousBundleCommandCustomCmd: " + mPreviousBundleCommandCustomCmd);

            if (mPreviousBundleCommandCustomIsVariable) {
                mCommandConfiguration.setText(mPreviousBundleCommandCustomVariableName,
                        EDITABLE);
            } else {
                mCommandConfiguration.setText(mPreviousBundleCommandCustomCmd,
                        EDITABLE);
            }

            if (mPreviousBundleCommandSupportedParamHint != null) {
                mCommandParamEditText.setHint(mPreviousBundleCommandSupportedParamHint);
                mCommandParamVariable.setChecked(mPreviousBundleCommandSupportedParamIsVariable);
                if (mPreviousBundleCommandSupportedParamIsVariable) {
                    mCommandParamEditText.setText(mPreviousBundleCommandSupportedParamVariableName, EDITABLE);
                } else {
                    mCommandParamEditText.setText(mPreviousBundleCommandSupportedParam, EDITABLE);
                }
            }

            mReturnOutputCheckbox.setChecked(mPreviousBundleOutputIsVariable);

            mReturnOutputVariable.setText(mPreviousBundleOutputVariableName, EDITABLE);

        }

        /*** is called once the remote service is no longer available */
        public void onServiceDisconnected(ComponentName name) { //
            Crashlytics.log(Log.WARN, Constants.TAG, "Service has unexpectedly disconnected");
            mErrorPlaceholder.setText("Unexpected disconnection to remote service - please try again later");
            mErrorPlaceholder.setVisibility(View.VISIBLE);
            mLoadingView.setVisibility(View.VISIBLE);
            mMainContentView.setEnabled(false);
            ddwrtCompanionService = null;
        }

    }

    public enum SupportedCommand {

        CUSTOM_COMMAND("-- CUSTOM COMMAND --", false, "exec-custom", null, null),
//        CUSTOM_SCRIPT("-- SCRIPT FILE --", false, "exec-file", null, null),
        REBOOT("Reboot", false, "reboot", null, null),
        CLEAR_ARP_CACHE("Clear ARP Cache", false, "clear-arp-cache", null, null),
        CLEAR_DNS_CACHE("Clear DNS Cache", false, "clear-dns-cache", null, null),
        DHCP_RELEASE("DHCP Release", false, "dhcp-release", null, null),
        DHCP_RENEW("DHCP Renew", false, "dhcp-renew", null, null),
        ERASE_WAN_TRAFFIC("Erase WAN Traffic Data", false, "erase-wan-traffic", null, null),
        STOP_HTTPD("Stop HTTP Server", false, "stop-httpd", null, null),
        START_HTTPD("Start HTTP Server", false, "start-httpd", null, null),
        RESTART_HTTPD("Restart HTTP Server", false, "restart-httpd", null, null),
        RESET_BANDWIDTH_COUNTERS("Reset Bandwidth Counters", false, "reset-bandwidth-counters", 
                null, null),
        WAKE_ON_LAN("Wake On LAN", true, "wol", "mac", "MAC Address"), //TODO Add port
        ENABLE_OPENVPNC("Enable OpenVPN Client", false, "enable-openvpn-client", null, null),
        DISABLE_OPENVPNC("Disable OpenVPN Client", false, "disable-openvpn-client", null, null),
        ENABLE_OPENVPND("Enable OpenVPN Server", false, "enable-openvpn-server", null, null),
        DISABLE_OPENVPND("Disable OpenVPN Server", false, "disable-openvpn-server", null, null),
        ENABLE_PPTPC("Enable PPTP Client", false, "enable-pptp-client", null, null),
        DISABLE_PPTPC("Disable PPTP Client", false, "disable-pptp-client", null, null),
        ENABLE_PPTPD("Enable PPTP Server", false, "enable-pptp-server", null, null),
        DISABLE_PPTPD("Disable PPTP Server", false, "disable-pptp-server", null, null),
        ENABLE_WOLD("Enable Wake On LAN Daemon", false, "enable-wol-daemon", null, null),
        DISABLE_WOLD("Disable Wake On LAN Daemon", false, "disable-wol-daemon", null, null),
        ENABLE_WAN_TRAFFIC_COUNTERS("Enable WAN Traffic counters", false, 
                "enable-wan-traffic-counters", null, null),
        DISABLE_WAN_TRAFFIC_COUNTERS("Disable WAN Traffic counters", false, 
                "diable-wan-traffic-counters", null, null),
        ENABLE_SYSLOGD("Enable Syslog", false, "enable-syslog", null, null),
        DISABLE_SYSLOGD("Disable Syslog", false, "disable-syslog", null, null),
        ENABLE_DEVICE_WAN_ACCESS("Enable WAN Access for Device", true, "enable-device-wan-access", 
                "mac", "Device MAC Address"),
        DISABLE_DEVICE_WAN_ACCESS("Disable WAN Access for Device", true, "disable-device-wan-access", 
                "mac", "Device MAC Address"),
        ESABLE_WAN_ACCESS_POLICY("Enable WAN Access Policy", true, "ensable-wan-access-policy", 
                "policy", "Policy Name"),
        DISABLE_WAN_ACCESS_POLICY("Disable WAN Access Policy", true, "disable-pptp-server", 
                "policy", "Policy Name");

        @Nullable
        public final String humanReadableName;
        public final boolean isConfigurable;
        @NonNull
        public final String actionName;
        @Nullable
        public final String paramName;
        @Nullable
        public final String paramHumanReadableHint;

        SupportedCommand(@Nullable String humanReadableName,
                         boolean isConfigurable,
                         @NonNull String actionName,
                         @Nullable String paramName,
                         @Nullable String paramHumanReadableHint) {
            this.humanReadableName = humanReadableName;
            this.isConfigurable = isConfigurable;
            this.actionName = actionName;
            this.paramName = paramName;
            this.paramHumanReadableHint = paramHumanReadableHint;
        }
    }
}