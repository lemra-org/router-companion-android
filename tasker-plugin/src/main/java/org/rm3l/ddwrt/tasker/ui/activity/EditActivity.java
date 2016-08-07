package org.rm3l.ddwrt.tasker.ui.activity;

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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.twofortyfouram.locale.sdk.client.ui.activity.AbstractAppCompatPluginActivity;
import com.twofortyfouram.log.Lumberjack;

import net.jcip.annotations.NotThreadSafe;

import org.rm3l.ddwrt.common.IRouterService;
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

import static com.google.common.base.Strings.isNullOrEmpty;

@NotThreadSafe
public class EditActivity extends AbstractAppCompatPluginActivity {

    /** Service to which this client will bind */
    private IRouterService routerService;

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

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        final PackageManager packageManager = getPackageManager();

        // connect to the service
        conn = new RouterServiceConnection();
        // name must match the service's Intent filter in the Service Manifest file
        final Intent intent = new Intent("org.rm3l.ddwrt.IRouterService");
        String ddwrtCompanionAppPackage;
        if (Utils.isPackageInstalled("org.rm3l.ddwrt", packageManager)) {
            ddwrtCompanionAppPackage = "org.rm3l.ddwrt";
        } else if (Utils.isPackageInstalled("org.rm3l.ddwrt.amzn.underground", packageManager)) {
            ddwrtCompanionAppPackage = "org.rm3l.ddwrt.amzn.underground";
        } else if (Utils.isPackageInstalled("org.rm3l.ddwrt.lite", packageManager)) {
            ddwrtCompanionAppPackage = "org.rm3l.ddwrt.lite";
        } else {
            ddwrtCompanionAppPackage = "org.rm3l.ddwrt";
        }
        Crashlytics.log(Log.DEBUG, Constants.TAG,
                "ddwrtCompanionAppPackage=" + ddwrtCompanionAppPackage);

        intent.setPackage(ddwrtCompanionAppPackage);
        // bind to the Service, create it if it's not already there
        bindService(intent, conn, Context.BIND_AUTO_CREATE);

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
            Lumberjack.e("Calling package couldn't be found%s", e); //$NON-NLS-1$
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
                Toast.makeText(EditActivity.this,
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
    }

    @Override
    public void onPostCreateWithPreviousResult(@NonNull final Bundle previousBundle,
                                               @NonNull final String previousBlurb) {
        final String message = PluginBundleValues.getMessage(previousBundle);
//        ((EditText) findViewById(android.R.id.text1)).setText(message);
    }

    @Override
    public boolean isBundleValid(@NonNull final Bundle bundle) {
        return PluginBundleValues.isBundleValid(bundle);
    }

    @Nullable
    @Override
    public Bundle getResultBundle() {
        Bundle result = null;

//        final String message = ((EditText) findViewById(android.R.id.text1)).getText().toString();
//        if (!TextUtils.isEmpty(message)) {
//            result = PluginBundleValues.generateBundle(getApplicationContext(), message);
//        }

        return result;
    }

    @NonNull
    @Override
    public String getResultBlurb(@NonNull final Bundle bundle) {
        final String message = PluginBundleValues.getMessage(bundle);

        final int maxBlurbLength = getResources().getInteger(
                R.integer.com_twofortyfouram_locale_sdk_client_maximum_blurb_length);

        if (message.length() > maxBlurbLength) {
            return message.substring(0, maxBlurbLength);
        }

        return message;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }



    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
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
                //TODO About
                Toast.makeText(this,
                        "[TODO] About", Toast.LENGTH_SHORT).show();
                break;
            case R.id.menu_discard_changes:
                // Signal to AbstractAppCompatPluginActivity that the user canceled.
                mIsCancelled = true;
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
        routerService = null;
    }

    /** Inner class used to connect to UserDataService */
    class RouterServiceConnection implements ServiceConnection {

        /** is called once the bind succeeds */
        public void onServiceConnected(ComponentName name, IBinder service) {
            Crashlytics.log(Log.DEBUG, Constants.TAG, "Service connected");
            routerService = IRouterService.Stub.asInterface(service);

            mErrorPlaceholder.setVisibility(View.GONE);
            mLoadingView.setVisibility(View.GONE);
            mMainContentView.setVisibility(View.VISIBLE);

            final List<RouterInfo> allRouters;
            try {
                allRouters = routerService.getAllRouters();
            } catch (RemoteException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
                Toast.makeText(EditActivity.this, "Internal Error - please try again later", Toast.LENGTH_SHORT)
                        .show();
                finish();
                return;
            }

            if (allRouters == null || allRouters.isEmpty()) {
                //Error - redirect to "Launch DD-WRT Companion to add a new Router"
                return;
            }

            final String[] routersNamesArray = new String[allRouters.size()];
            int i = 0;
            for (final RouterInfo router : allRouters) {
                final String routerName = router.getName();
                routersNamesArray[i++] = ((isNullOrEmpty(routerName) ? "-" : routerName) + "\n(" +
                        router.getRemoteIpAddress() + ":" + router.getRemotePort() + ")");
            }

            mRoutersListAdapter = new ArrayAdapter<>(EditActivity.this,
                    R.layout.spinner_item, new ArrayList<>(Arrays.asList(routersNamesArray)));
            mRoutersListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            mRoutersDropdown.setAdapter(mRoutersListAdapter);

            mRoutersDropdown.setSelection(0);

            mRoutersDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                    final RouterInfo routerInfo = allRouters.get(position);
                    if (routerInfo == null) {
                        return;
                    }
                    mSelectedRouterUuid.setText(routerInfo.getUuid());
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            final SupportedCommand[] supportedCommands = SupportedCommand.values();
            final String[] supportedCommandsArr = new String[supportedCommands.length];
            int j = 0;
            for (final SupportedCommand cmd : supportedCommands) {
                supportedCommandsArr[j++] = cmd.humanReadableName;
            }
            final ArrayAdapter<String> cmdAdapter = new ArrayAdapter<>(EditActivity.this,
                    R.layout.spinner_item, supportedCommandsArr);
            cmdAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mCommandsDropdown.setAdapter(cmdAdapter);

            mCommandConfiguration.setVisibility(View.GONE);

            mCommandsDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                    //TODO
                    final SupportedCommand supportedCommand = supportedCommands[position];
                    if (supportedCommand == null) {
                        return;
                    }
                    switch (supportedCommand) {
                        case CUSTOM_COMMAND:
                            mCommandConfiguration.setVisibility(View.VISIBLE);
                            break;
                        default:
                            mCommandConfiguration.setVisibility(View.GONE);
                            mCommandConfiguration.setText("", TextView.BufferType.EDITABLE);
                            break;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
        }

        /*** is called once the remote service is no longer available */
        public void onServiceDisconnected(ComponentName name) { //
            Crashlytics.log(Log.WARN, Constants.TAG, "Service has unexpectedly disconnected");
            mErrorPlaceholder.setText("Unexpected disconnection to remote service - please try again later");
            mErrorPlaceholder.setVisibility(View.VISIBLE);
            mLoadingView.setVisibility(View.VISIBLE);
            mMainContentView.setVisibility(View.GONE);
            routerService = null;
        }

    }

    enum SupportedCommand {

        CUSTOM_COMMAND("-- CUSTOM COMMAND --", false, "", null),
        REBOOT("Reboot", false, "reboot", null),
        CLEAR_ARP_CACHE("Clear ARP Cache", false, "clear-arp-cache", null),
        CLEAR_DNS_CACHE("Clear DNS Cache", false, "clear-dns-cache", null),
        DHCP_RELEASE("DHCP Release", false, "dhcp-release", null),
        DHCP_RENEW("DHCP Renew", false, "dhcp-renew", null),
        ERASE_WAN_TRAFFIC("Erase WAN Traffic Data", false, "erase-wan-traffic", null),
        STOP_HTTPD("Stop HTTP Server", false, "stop-httpd", null),
        START_HTTPD("Start HTTP Server", false, "start-httpd", null),
        RESTART_HTTPD("Restart HTTP Server", false, "restart-httpd", null),
        RESET_BANDWIDTH_COUNTERS("Reset Bandwidth Counters", false, "reset-bandwidth-counters", null),
        WAKE_ON_LAN("Wake On LAN", false, "wol", "mac"), //TODO Add port
        ENABLE_OPENVPNC("Enable OpenVPN Client", false, "enable-openvpn-client", null),
        DISABLE_OPENVPNC("Disable OpenVPN Client", false, "disable-openvpn-client", null),
        ENABLE_OPENVPND("Enable OpenVPN Server", false, "enable-openvpn-server", null),
        DISABLE_OPENVPND("Disable OpenVPN Server", false, "disable-openvpn-server", null),
        ENABLE_PPTPC("Enable PPTP Client", false, "enable-pptp-client", null),
        DISABLE_PPTPC("Disable PPTP Client", false, "disable-pptp-client", null),
        ENABLE_PPTPD("Enable PPTP Server", false, "enable-pptp-server", null),
        DISABLE_PPTPD("Disable PPTP Server", false, "disable-pptp-server", null),
        ENABLE_WOLD("Enable Wake On LAN Daemon", false, "enable-wol-daemon", null),
        DISABLE_WOLD("Disable Wake On LAN Daemon", false, "disable-wol-daemon", null),
        ENABLE_WAN_TRAFFIC_COUNTERS("Enable WAN Traffic counters", false, "enable-wan-traffic-counters", null),
        DISABLE_WAN_TRAFFIC_COUNTERS("Disable WAN Traffic counters", false, "diable-wan-traffic-counters", null),
        ENABLE_SYSLOGD("Enable Syslog", false, "enable-syslog", null),
        DISABLE_SYSLOGD("Disable Syslog", false, "disable-syslog", null),
        ENABLE_DEVICE_WAN_ACCESS("Enable WAN Access for Device", false, "enable-device-wan-access", "mac"),
        DISABLE_DEVICE_WAN_ACCESS("Disable WAN Access for Device", false, "disable-device-wan-access", "mac"),
        ESABLE_WAN_ACCESS_POLICY("Enable WAN Access Policy", false, "ensable-wan-access-policy", "policy"),
        DISABLE_WAN_ACCESS_POLICY("Disable WAN Access Policy", false, "disable-pptp-server", "policy");

        @Nullable
        public final String humanReadableName;
        public final boolean isConfigurable;
        @NonNull
        public final String actionName;
        @Nullable
        public final String paramName;

        SupportedCommand(String humanReadableName,
                         boolean isConfigurable,
                         String actionName,
                         String paramName) {
            this.humanReadableName = humanReadableName;
            this.isConfigurable = isConfigurable;
            this.actionName = actionName;
            this.paramName = paramName;
        }
    }
}