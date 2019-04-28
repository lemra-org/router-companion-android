package org.rm3l.router_companion.tasker.ui.activity.action;

import static android.widget.TextView.BufferType.EDITABLE;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.router_companion.tasker.Constants.MAX_ACTION_RUNS_FREE_VERSION;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_APP_PIN_CODE;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_CUSTOM_CMD;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_CUSTOM_IS_VARIABLE;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_CUSTOM_VARIABLE_NAME;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_IS_CUSTOM;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_SUPPORTED_NAME;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_SUPPORTED_PARAM;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_SUPPORTED_PARAM_HINT;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_SUPPORTED_PARAM_IS_VARIABLE;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_SUPPORTED_PARAM_VARIABLE_NAME;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_SUPPORTED_READABLE_NAME;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_EXTRA_INT_VERSION_CODE;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_OUTPUT_IS_VARIABLE;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_OUTPUT_VARIABLE_NAME;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_ROUTER_CANONICAL_READABLE_NAME;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_ROUTER_IS_VARIABLE;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_ROUTER_UUID;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_ROUTER_VARIABLE_NAME;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.text.InputType;
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
import com.google.common.base.Throwables;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.twofortyfouram.locale.sdk.client.ui.activity.AbstractAppCompatPluginActivity;
import com.twofortyfouram.log.Lumberjack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.jcip.annotations.NotThreadSafe;
import org.rm3l.maoni.Maoni;
import org.rm3l.router_companion.common.IRouterCompanionService;
import org.rm3l.router_companion.common.resources.RouterInfo;
import org.rm3l.router_companion.common.resources.audit.ActionLog;
import org.rm3l.router_companion.common.utils.ActivityUtils;
import org.rm3l.router_companion.tasker.BuildConfig;
import org.rm3l.router_companion.tasker.Constants;
import org.rm3l.router_companion.tasker.R;
import org.rm3l.router_companion.tasker.bundle.PluginBundleValues;
import org.rm3l.router_companion.tasker.exception.DDWRTCompanionPackageVersionRequiredNotFoundException;
import org.rm3l.router_companion.tasker.feedback.maoni.FeedbackHandler;
import org.rm3l.router_companion.tasker.utils.Utils;

@NotThreadSafe
public class ActionEditActivity extends AbstractAppCompatPluginActivity {

    /**
     * Inner class used to connect to UserDataService
     */
    class RouterServiceConnection implements ServiceConnection {

        private final boolean reloading;

        RouterServiceConnection(boolean reloading) {
            this.reloading = reloading;
        }

        /**
         * is called once the bind succeeds
         */
        public void onServiceConnected(ComponentName name, IBinder service) {
            Crashlytics.log(Log.DEBUG, Constants.TAG, "Service connected");
            routerCompanionService = IRouterCompanionService.Stub.asInterface(service);

            mErrorPlaceholder.setVisibility(View.GONE);
            mLoadingView.setVisibility(View.GONE);
            mMainContentView.setVisibility(View.VISIBLE);
            mMainContentView.setEnabled(true);

            if ("org.rm3l.ddwrt.free".equalsIgnoreCase(ddwrtCompanionAppPackage)) {
                //Limit the number of actions that can be sent to the Lite Package
                final List<ActionLog> actionsByOrigin;
                try {
                    actionsByOrigin = routerCompanionService.getActionsByOrigin(BuildConfig.APPLICATION_ID);
                } catch (RemoteException e) {
                    Crashlytics.logException(e);
                    e.printStackTrace();
                    Toast.makeText(ActionEditActivity.this, "Internal Error - please try again later",
                            Toast.LENGTH_LONG).show();
                    mErrorPlaceholder.setText("Error: " + Throwables.getRootCause(e).getMessage());
                    mErrorPlaceholder.setVisibility(View.VISIBLE);
                    mIsCancelled = true;
                    finish();
                    return;
                }
                final int nbActionsForThisPackage = (actionsByOrigin != null ? actionsByOrigin.size() : 0);
                Crashlytics.log(Log.DEBUG, Constants.TAG, "Found " + nbActionsForThisPackage +
                        " action runs against DD-WRT Companion Lite app.");
                if (nbActionsForThisPackage > MAX_ACTION_RUNS_FREE_VERSION) {
                    final String text =
                            ("When using this plugin with DD-WRT Companion Lite app, you can have at most "
                                    + MAX_ACTION_RUNS_FREE_VERSION
                                    + " action runs. "
                                    + "Please consider upgrading to DD-WRT Companion premium app to support this initiative!");
                    Crashlytics.log(Log.DEBUG, Constants.TAG, text);
                    mErrorPlaceholder.setText(text);
                    mErrorPlaceholder.setVisibility(View.VISIBLE);
                    Toast.makeText(ActionEditActivity.this, text, Toast.LENGTH_LONG).show();
                    mIsCancelled = true;
                    finish();
                    return;
                }
            }

            final List<RouterInfo> allRouters;
            try {
                allRouters = routerCompanionService.getAllRouters();
            } catch (RemoteException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
                Toast.makeText(ActionEditActivity.this, "Internal Error - please try again later",
                        Toast.LENGTH_SHORT).show();
                mErrorPlaceholder.setText("Error: " + Throwables.getRootCause(e).getMessage());
                mErrorPlaceholder.setVisibility(View.VISIBLE);
                mIsCancelled = true;
                finish();
                return;
            }

            //            if (allRouters == null || allRouters.isEmpty()) {
            //                //Error - redirect to "Launch DD-WRT Companion to add a new Router"
            //                mErrorPlaceholder.setText("Open DD-WRT Companion to register routers");
            //                mErrorPlaceholder.setVisibility(View.VISIBLE);
            //                return;
            //            }
            //            mErrorPlaceholder.setVisibility(View.GONE);

            mIsCancelled = false;

            final String[] routersNamesArray = new String[allRouters.size() + 2];
            int i = 0;
            Integer selectedRouterIndex = null;
            for (final RouterInfo router : allRouters) {
                if (mPreviousBundleRouterUuid != null && mPreviousBundleRouterUuid.equals(
                        router.getUuid())) {
                    selectedRouterIndex = i;
                }
                final String routerName = router.getName();
                routersNamesArray[i++] = ((isNullOrEmpty(routerName) ? "-" : routerName)
                        + "\n("
                        + router.getRemoteIpAddress()
                        + ":"
                        + router.getRemotePort()
                        + ")");
            }
            routersNamesArray[i] = "-- VARIABLE --";
            routersNamesArray[i + 1] = ">> ADD NEW ROUTER <<";

            mRoutersListAdapter = new ArrayAdapter<>(ActionEditActivity.this, R.layout.spinner_item,
                    new ArrayList<>(Arrays.asList(routersNamesArray)));
            mRoutersListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            mRoutersDropdown.setAdapter(mRoutersListAdapter);

            mRoutersDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                @SuppressLint("DefaultLocale")
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {

                    if (position == routersNamesArray.length - 1) {
                        //Open DD-WRT Companion Android app to register a new router
                        try {
                            final PackageInfo packageInfo =
                                    Utils.getDDWRTCompanionAppPackageLeastRequiredVersion(mPackageManager);
                            final String ddwrtCompanionAppPackage =
                                    (packageInfo != null ? packageInfo.packageName : null);
                            if (TextUtils.isEmpty(ddwrtCompanionAppPackage)) {
                                ActivityUtils.openPlayStoreForPackage(ActionEditActivity.this, "org.rm3l.ddwrt");
                                mRoutersDropdown.setSelection(0);
                                return;
                            }

                            final Intent intent = new Intent(ACTION_OPEN_ADD_ROUTER_WIZARD);
                            intent.setPackage(ddwrtCompanionAppPackage);
                            intent.putExtra(CLOSE_ON_ACTION_DONE, true);

                            try {
                                ActionEditActivity.this.startActivityForResult(intent, REGISTER_ROUTER_REQUEST);
                            } catch (final ActivityNotFoundException anfe) {
                                Crashlytics.logException(anfe);
                                ActivityUtils.openPlayStoreForPackage(ActionEditActivity.this, "org.rm3l.ddwrt");
                                mRoutersDropdown.setSelection(0);
                            }
                        } catch (final DDWRTCompanionPackageVersionRequiredNotFoundException e) {
                            Toast.makeText(ActionEditActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                            Crashlytics.logException(e);
                            mErrorPlaceholder.setText(e.getMessage());
                            mErrorPlaceholder.setVisibility(View.VISIBLE);
                            mRoutersDropdown.setSelection(0);
                        }
                    } else if (position == routersNamesArray.length - 2) {
                        //Variable
                        mSelectedRouterVariable.setVisibility(View.VISIBLE);
                        mSelectedRouterVariable.setHint("e.g., %router_name");
                        mSelectedRouterUuid.setText(null);
                        mSelectedRouterReadableName = null;
                        mErrorPlaceholder.setVisibility(View.GONE);
                    } else {
                        mErrorPlaceholder.setVisibility(View.GONE);
                        mSelectedRouterVariable.setVisibility(View.GONE);
                        mSelectedRouterVariable.setHint(null);
                        mSelectedRouterVariable.setText(null);
                        final RouterInfo routerInfo = allRouters.get(position);
                        if (routerInfo == null) {
                            return;
                        }
                        mSelectedRouterUuid.setText(routerInfo.getUuid());
                        mSelectedRouterReadableName = String.format("%s (%s)",
                                TextUtils.isEmpty(routerInfo.getName()) ? "-" : routerInfo.getName(),
                                routerInfo.isDemoRouter() ? "DEMO"
                                        : String.format("%s:%d", routerInfo.getRemoteIpAddress(),
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
                mSelectedRouterVariable.setText(mPreviousBundleRouterVariableName, EDITABLE);
            }

            if (!reloading) {
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
                final ArrayAdapter<String> cmdAdapter =
                        new ArrayAdapter<>(ActionEditActivity.this, R.layout.spinner_item,
                                supportedCommandsArr);
                cmdAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mCommandsDropdown.setAdapter(cmdAdapter);

                mCommandConfiguration.setVisibility(View.GONE);

                mCommandParamVariable.setOnCheckedChangeListener(
                        new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                                if (mCommand != null) {
                                    if (TextUtils.isEmpty(mCommand.paramHumanReadableHint)) {
                                        mCommandParamEditText.setHint("Variable Name");
                                    } else {
                                        if (b) {
                                            mCommandParamEditText.setHint(
                                                    "Variable for '" + mCommand.paramHumanReadableHint + "'");
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
                Crashlytics.log(Log.DEBUG, Constants.TAG, "mPreviousBundleCommandCustomVariableName: "
                        + mPreviousBundleCommandCustomVariableName);
                Crashlytics.log(Log.DEBUG, Constants.TAG,
                        "mPreviousBundleCommandCustomCmd: " + mPreviousBundleCommandCustomCmd);

                if (mPreviousBundleCommandCustomIsVariable) {
                    mCommandConfiguration.setText(mPreviousBundleCommandCustomVariableName, EDITABLE);
                } else {
                    mCommandConfiguration.setText(mPreviousBundleCommandCustomCmd, EDITABLE);
                }

                if (mPreviousBundleCommandSupportedParamHint != null) {
                    mCommandParamEditText.setHint(mPreviousBundleCommandSupportedParamHint);
                    mCommandParamVariable.setChecked(mPreviousBundleCommandSupportedParamIsVariable);
                    if (mPreviousBundleCommandSupportedParamIsVariable) {
                        mCommandParamEditText.setText(mPreviousBundleCommandSupportedParamVariableName,
                                EDITABLE);
                    } else {
                        mCommandParamEditText.setText(mPreviousBundleCommandSupportedParam, EDITABLE);
                    }
                }

                mReturnOutputCheckbox.setChecked(mPreviousBundleOutputIsVariable);

                mReturnOutputVariable.setText(mPreviousBundleOutputVariableName, EDITABLE);
            }
        }

        /*** is called once the remote service is no longer available */
        public void onServiceDisconnected(ComponentName name) { //
            Crashlytics.log(Log.WARN, Constants.TAG, "Service has unexpectedly disconnected");
            mErrorPlaceholder.setText(
                    "Connection to DD-WRT Companion application unexpectedly disconnected. Please reload and try again.");
            mErrorPlaceholder.setVisibility(View.VISIBLE);
            mLoadingView.setVisibility(View.VISIBLE);
            mMainContentView.setEnabled(false);
            routerCompanionService = null;
        }
    }

    public enum SupportedCommand {

        CUSTOM_COMMAND("-- CUSTOM COMMAND --", false, "exec-custom", null,
                null), //        CUSTOM_SCRIPT("-- SCRIPT FILE --", false, "exec-file", null, null),
        REBOOT("Reboot", false, "reboot", null, null), CLEAR_ARP_CACHE("Clear ARP Cache", false,
                "clear-arp-cache", null, null), CLEAR_DNS_CACHE("Clear DNS Cache", false, "clear-dns-cache",
                null, null), DHCP_RELEASE("DHCP Release", false, "dhcp-release", null, null), DHCP_RENEW(
                "DHCP Renew", false, "dhcp-renew", null, null), ERASE_WAN_TRAFFIC("Erase WAN Traffic Data",
                false, "erase-wan-traffic", null, null), STOP_HTTPD("Stop HTTP Server", false, "stop-httpd",
                null, null), START_HTTPD("Start HTTP Server", false, "start-httpd", null,
                null), RESTART_HTTPD("Restart HTTP Server", false, "restart-httpd", null,
                null), RESET_BANDWIDTH_COUNTERS("Reset Bandwidth Counters", false,
                "reset-bandwidth-counters", null, null), WAKE_ON_LAN("Wake On LAN", true, "wol", "mac",
                "MAC Address"), //TODO Add port
        ENABLE_OPENVPNC("Enable OpenVPN Client", false, "enable-openvpn-client", null,
                null), DISABLE_OPENVPNC("Disable OpenVPN Client", false, "disable-openvpn-client", null,
                null), ENABLE_OPENVPND("Enable OpenVPN Server", false, "enable-openvpn-server", null,
                null), DISABLE_OPENVPND("Disable OpenVPN Server", false, "disable-openvpn-server", null,
                null), ENABLE_PPTPC("Enable PPTP Client", false, "enable-pptp-client", null,
                null), DISABLE_PPTPC("Disable PPTP Client", false, "disable-pptp-client", null,
                null), ENABLE_PPTPD("Enable PPTP Server", false, "enable-pptp-server", null,
                null), DISABLE_PPTPD("Disable PPTP Server", false, "disable-pptp-server", null,
                null), ENABLE_WOLD("Enable Wake On LAN Daemon", false, "enable-wol-daemon", null,
                null), DISABLE_WOLD("Disable Wake On LAN Daemon", false, "disable-wol-daemon", null,
                null), ENABLE_WAN_TRAFFIC_COUNTERS("Enable WAN Traffic counters", false,
                "enable-wan-traffic-counters", null, null), DISABLE_WAN_TRAFFIC_COUNTERS(
                "Disable WAN Traffic counters", false, "diable-wan-traffic-counters", null,
                null), ENABLE_SYSLOGD("Enable Syslog", false, "enable-syslog", null, null), DISABLE_SYSLOGD(
                "Disable Syslog", false, "disable-syslog", null, null), ENABLE_DEVICE_WAN_ACCESS(
                "Enable WAN Access for Device", true, "enable-device-wan-access", "mac",
                "Device MAC Address"), DISABLE_DEVICE_WAN_ACCESS("Disable WAN Access for Device", true,
                "disable-device-wan-access", "mac", "Device MAC Address"), ESABLE_WAN_ACCESS_POLICY(
                "Enable WAN Access Policy", true, "enable-wan-access-policy", "policy",
                "Policy Name"), DISABLE_WAN_ACCESS_POLICY("Disable WAN Access Policy", true,
                "disable-wan-access-policy", "policy", "Policy Name");

        @NonNull
        public final String actionName;

        @Nullable
        public final String humanReadableName;

        public final boolean isConfigurable;

        @Nullable
        public final String paramHumanReadableHint;

        @Nullable
        public final String paramName;

        SupportedCommand(@Nullable String humanReadableName, boolean isConfigurable,
                @NonNull String actionName, @Nullable String paramName,
                @Nullable String paramHumanReadableHint) {
            this.humanReadableName = humanReadableName;
            this.isConfigurable = isConfigurable;
            this.actionName = actionName;
            this.paramName = paramName;
            this.paramHumanReadableHint = paramHumanReadableHint;
        }
    }

    public static final String DDWRT_COMPANION_SERVICE_NAME =
            "org.rm3l.router_companion.IDDWRTCompanionService";

    public static final String ACTION_OPEN_ADD_ROUTER_WIZARD =
            "org.rm3l.ddwrt.OPEN_ADD_ROUTER_WIZARD";

    public static final String CLOSE_ON_ACTION_DONE = "CLOSE_ON_ACTION_DONE";

    public static final String ROUTER_SELECTED = "ROUTER_SELECTED";

    private static final int REGISTER_ROUTER_REQUEST = 1;

    /**
     * Connection to the service (inner class)
     */
    private RouterServiceConnection conn;

    private String ddwrtCompanionAppPackage;

    private SupportedCommand mCommand;

    private EditText mCommandConfiguration;

    private CheckBox mCommandConfigurationVariable;

    private EditText mCommandParamEditText;

    private CheckBox mCommandParamVariable;

    private Spinner mCommandsDropdown;

    private TextView mErrorPlaceholder;

    private Button mFileSelectionButton;

    private ProgressBar mLoadingView;

    private View mMainContentView;

    private PackageManager mPackageManager;

    private EditText mPinCodeEditText;

    private String mPreviousBundleAppPinCode;

    private String mPreviousBundleCommandCustomCmd;

    private boolean mPreviousBundleCommandCustomIsVariable;

    private String mPreviousBundleCommandCustomVariableName;

    private boolean mPreviousBundleCommandIsCustom;

    private String mPreviousBundleCommandSupportedName;

    private String mPreviousBundleCommandSupportedParam;

    private String mPreviousBundleCommandSupportedParamHint;

    private boolean mPreviousBundleCommandSupportedParamIsVariable;

    private String mPreviousBundleCommandSupportedParamVariableName;

    private String mPreviousBundleCommandSupportedReadableName;

    private int mPreviousBundleIntVersionCode;

    private boolean mPreviousBundleOutputIsVariable;

    private String mPreviousBundleOutputVariableName;

    private String mPreviousBundleRouterCanonicalReadableName;

    private boolean mPreviousBundleRouterIsVariable;

    private String mPreviousBundleRouterUuid;

    private String mPreviousBundleRouterVariableName;

    private CheckBox mReturnOutputCheckbox;

    private EditText mReturnOutputVariable;

    private Spinner mRoutersDropdown;

    private ArrayAdapter<String> mRoutersListAdapter;

    private String mSelectedRouterReadableName;

    private TextView mSelectedRouterUuid;

    private EditText mSelectedRouterVariable;

    /**
     * Service to which this client will bind
     */
    private IRouterCompanionService routerCompanionService;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_action_edit);

        mPackageManager = getPackageManager();

        /*
         * To help the user keep context, the title shows the host's name and the subtitle
         * shows the plug-in's name.
         */
        CharSequence callingApplicationLabel = null;
        try {
            callingApplicationLabel = mPackageManager.getApplicationLabel(
                    mPackageManager.getApplicationInfo(getCallingPackage(), 0));
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
                Toast.makeText(ActionEditActivity.this, mErrorPlaceholder.getText(), Toast.LENGTH_SHORT)
                        .show();
            }
        });

        mLoadingView = (ProgressBar) findViewById(R.id.loading_view);
        mMainContentView = findViewById(R.id.main_content_view);

        mPinCodeEditText = (EditText) findViewById(R.id.pin_code);
        ((CheckBox) findViewById(R.id.pin_code_show_checkbox)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!isChecked) {
                            mPinCodeEditText.setInputType(
                                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        } else {
                            mPinCodeEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        }
                        mPinCodeEditText.requestFocus();
                        mPinCodeEditText.setSelection(mPinCodeEditText.length());
                    }
                });

        mSelectedRouterUuid = (TextView) findViewById(R.id.selected_router_uuid);
        mRoutersDropdown = (Spinner) findViewById(R.id.select_router_dropdown);

        mCommandsDropdown = (Spinner) findViewById(R.id.select_command_dropdown);
        mCommandConfiguration = (EditText) findViewById(R.id.command_configuration_input);

        mSelectedRouterVariable = (EditText) findViewById(R.id.selected_router_variable);

        mCommandConfigurationVariable =
                (CheckBox) findViewById(R.id.command_configuration_input_variable);
        mCommandConfigurationVariable.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
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

        mCommandParamVariable =
                (CheckBox) findViewById(R.id.command_configuration_input_param_variable);

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

        try {
            final PackageInfo packageInfo =
                    Utils.getDDWRTCompanionAppPackageLeastRequiredVersion(mPackageManager);

            if (packageInfo == null
                    || (this.ddwrtCompanionAppPackage = packageInfo.packageName) == null) {
                mErrorPlaceholder.setText("DD-WRT Companion app *not* found !");
                mErrorPlaceholder.setVisibility(View.VISIBLE);
                //TODO Add button that opens up the Play Store
                return;
            }
        } catch (final DDWRTCompanionPackageVersionRequiredNotFoundException e) {
            mErrorPlaceholder.setText(e.getMessage());
            mErrorPlaceholder.setVisibility(View.VISIBLE);
            return;
        }

        // connect to the service
        conn = new RouterServiceConnection(false);

        // name must match the service's Intent filter in the Service Manifest file
        final Intent intent = new Intent(DDWRT_COMPANION_SERVICE_NAME);
        intent.setPackage(this.ddwrtCompanionAppPackage);
        // bind to the Service, create it if it's not already there
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REGISTER_ROUTER_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user registered a router
                //Set it and reload everything
                mPreviousBundleRouterUuid = data.getStringExtra(ROUTER_SELECTED); //the new router UUID
                refresh();
            } else {
                if (mRoutersDropdown != null) {
                    mRoutersDropdown.setSelection(0);
                }
            }
        }
    }

    /**
     * Clean up before Activity is destroyed
     */
    @Override
    protected void onDestroy() {
        // Signal to AbstractAppCompatPluginActivity that the user canceled.
        mIsCancelled = true;
        super.onDestroy();
        if (conn != null) {
            try {
                unbindService(conn);
            } catch (final Exception ignored) {
                ignored.printStackTrace();
            }
        }
        routerCompanionService = null;
    }

    @NonNull
    @Override
    public String getResultBlurb(@NonNull final Bundle bundle) {
        return PluginBundleValues.getBundleBlurb(bundle);
    }

    @Nullable
    @Override
    public Bundle getResultBundle() {
        final boolean isVariableRouter = TextUtils.isEmpty(mSelectedRouterUuid.getText());
        final boolean isCustomCommand = (mCommand == null);
        final String appPinCode = mPinCodeEditText.getText().toString();

        return PluginBundleValues.generateBundle(getApplicationContext(),

                appPinCode,

                isVariableRouter, mSelectedRouterVariable.getText(), mSelectedRouterUuid.getText(),
                mSelectedRouterReadableName,

                isCustomCommand, mCommandConfigurationVariable.isChecked(), mCommandConfiguration.getText(),
                mCommand, mCommandParamEditText.getText(), mCommandParamVariable.isChecked(),

                mReturnOutputCheckbox.isChecked(), mReturnOutputVariable.getText());
    }

    @Override
    public boolean isBundleValid(@NonNull final Bundle bundle) {
        return PluginBundleValues.isBundleValid(bundle);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_action_edit_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int i = item.getItemId();
        if (i == android.R.id.home) {// Signal to AbstractAppCompatPluginActivity that the user canceled.
            mIsCancelled = true;
            onBackPressed();

        } else if (i == R.id.menu_refresh) {
            refresh();

        } else if (i == R.id.ddwrt_companion_tasker_feedback) {
            new Maoni.Builder(this, Constants.FILEPROVIDER_AUTHORITY)
                    .withSharedPreferences(Utils.getDefaultSharedPreferencesName(this))
                    .withTheme(R.style.AppThemeLight_StatusBarTransparent)
                    .withWindowTitle("Send Feedback")
                    .withExtraLayout(R.layout.activity_feedback_maoni)
                    .withHandler(new FeedbackHandler(this))
                    .build()
                    .start(this);

        } else if (i == R.id.ddwrt_companion_tasker_about) {
            new LibsBuilder().withFields(R.string.class.getFields()).withActivityTitle("About")
                    //provide a style (optional) (LIGHT, DARK, LIGHT_DARK_TOOLBAR)
                    .withActivityStyle(Libs.ActivityStyle.LIGHT)
                    //start the activity
                    .start(this);

        } else if (i
                == R.id.menu_discard_changes) {// Signal to AbstractAppCompatPluginActivity that the user canceled.
            mIsCancelled = true;
            finish();

        } else if (i == R.id.menu_save_changes) {
            mIsCancelled = false;
            finish();

        } else {
        }

        return true;
    }

    @Override
    public void onPostCreateWithPreviousResult(@NonNull final Bundle previousBundle,
            @NonNull final String previousBlurb) {

        Crashlytics.log(Log.DEBUG, Constants.TAG, "previousBundle: " + previousBundle);

        mPreviousBundleIntVersionCode = previousBundle.getInt(BUNDLE_EXTRA_INT_VERSION_CODE);

        mPreviousBundleAppPinCode = previousBundle.getString(BUNDLE_APP_PIN_CODE);

        mPreviousBundleRouterIsVariable = previousBundle.getBoolean(BUNDLE_ROUTER_IS_VARIABLE, false);
        mPreviousBundleRouterVariableName = previousBundle.getString(BUNDLE_ROUTER_VARIABLE_NAME);
        mPreviousBundleRouterUuid = previousBundle.getString(BUNDLE_ROUTER_UUID);
        mPreviousBundleRouterCanonicalReadableName =
                previousBundle.getString(BUNDLE_ROUTER_CANONICAL_READABLE_NAME);

        mPreviousBundleCommandIsCustom = previousBundle.getBoolean(BUNDLE_COMMAND_IS_CUSTOM, false);
        mPreviousBundleCommandCustomIsVariable =
                previousBundle.getBoolean(BUNDLE_COMMAND_CUSTOM_IS_VARIABLE, false);
        mPreviousBundleCommandCustomVariableName =
                previousBundle.getString(BUNDLE_COMMAND_CUSTOM_VARIABLE_NAME);
        mPreviousBundleCommandCustomCmd = previousBundle.getString(BUNDLE_COMMAND_CUSTOM_CMD);
        mPreviousBundleCommandSupportedName = previousBundle.getString(BUNDLE_COMMAND_SUPPORTED_NAME);
        mPreviousBundleCommandSupportedReadableName =
                previousBundle.getString(BUNDLE_COMMAND_SUPPORTED_READABLE_NAME);
        mPreviousBundleCommandSupportedParamHint =
                previousBundle.getString(BUNDLE_COMMAND_SUPPORTED_PARAM_HINT);
        mPreviousBundleCommandSupportedParam = previousBundle.getString(BUNDLE_COMMAND_SUPPORTED_PARAM);
        mPreviousBundleCommandSupportedParamIsVariable =
                previousBundle.getBoolean(BUNDLE_COMMAND_SUPPORTED_PARAM_IS_VARIABLE);
        mPreviousBundleCommandSupportedParamVariableName =
                previousBundle.getString(BUNDLE_COMMAND_SUPPORTED_PARAM_VARIABLE_NAME);

        mPreviousBundleOutputIsVariable = previousBundle.getBoolean(BUNDLE_OUTPUT_IS_VARIABLE, false);
        mPreviousBundleOutputVariableName = previousBundle.getString(BUNDLE_OUTPUT_VARIABLE_NAME);

        //Reconnect to the remote service
        if (conn != null) {
            unbindService(conn);
        }
        routerCompanionService = null;

        try {
            final PackageInfo packageInfo =
                    Utils.getDDWRTCompanionAppPackageLeastRequiredVersion(mPackageManager);

            if (packageInfo == null
                    || (this.ddwrtCompanionAppPackage = packageInfo.packageName) == null) {
                mErrorPlaceholder.setText("DD-WRT Companion app *not* found !");
                mErrorPlaceholder.setVisibility(View.VISIBLE);
                //TODO Add button that opens up the Play Store
                return;
            }
        } catch (final DDWRTCompanionPackageVersionRequiredNotFoundException e) {
            mErrorPlaceholder.setText(e.getMessage());
            mErrorPlaceholder.setVisibility(View.VISIBLE);
            return;
        }

        mPinCodeEditText.setText(mPreviousBundleAppPinCode, TextView.BufferType.EDITABLE);

        // connect to the service
        conn = new RouterServiceConnection(false);

        // name must match the service's Intent filter in the Service Manifest file
        final Intent intent = new Intent(DDWRT_COMPANION_SERVICE_NAME);
        intent.setPackage(ddwrtCompanionAppPackage);
        // bind to the Service, create it if it's not already there
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }

    private void refresh() {

        try {
            final PackageInfo packageInfo =
                    Utils.getDDWRTCompanionAppPackageLeastRequiredVersion(mPackageManager);

            if (packageInfo == null
                    || (this.ddwrtCompanionAppPackage = packageInfo.packageName) == null) {
                mErrorPlaceholder.setText("DD-WRT Companion app *not* found !");
                mErrorPlaceholder.setVisibility(View.VISIBLE);
                //TODO Add button that opens up the Play Store
                return;
            }
        } catch (final DDWRTCompanionPackageVersionRequiredNotFoundException e) {
            mErrorPlaceholder.setText(e.getMessage());
            mErrorPlaceholder.setVisibility(View.VISIBLE);
            return;
        }

        mLoadingView.setVisibility(View.VISIBLE);
        mMainContentView.setEnabled(false);
        //Reconnect to the remote service
        if (conn != null) {
            unbindService(conn);
        }
        routerCompanionService = null;

        // connect to the service
        conn = new RouterServiceConnection(true);

        // name must match the service's Intent filter in the Service Manifest file
        final Intent intent = new Intent(DDWRT_COMPANION_SERVICE_NAME);
        intent.setPackage(ddwrtCompanionAppPackage);
        // bind to the Service, create it if it's not already there
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }
}