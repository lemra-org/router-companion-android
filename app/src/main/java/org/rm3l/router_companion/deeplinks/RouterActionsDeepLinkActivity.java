package org.rm3l.router_companion.deeplinks;

import static org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.router_companion.RouterCompanionAppConstants.SECURITY_THIRD_PARTY_INTEGRATION;
import static org.rm3l.router_companion.actions.ToggleWANAccessPolicyRouterAction.DISABLE;
import static org.rm3l.router_companion.actions.ToggleWANAccessPolicyRouterAction.ENABLE_1;
import static org.rm3l.router_companion.actions.ToggleWANAccessPolicyRouterAction.ENABLE_2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.airbnb.deeplinkdispatch.DeepLink;
import com.crashlytics.android.Crashlytics;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.router_companion.actions.AbstractRouterAction;
import org.rm3l.router_companion.actions.ActionManager;
import org.rm3l.router_companion.actions.ClearARPCacheRouterAction;
import org.rm3l.router_companion.actions.ClearDNSCacheRouterAction;
import org.rm3l.router_companion.actions.DHCPClientRouterAction;
import org.rm3l.router_companion.actions.DisableWANAccessRouterAction;
import org.rm3l.router_companion.actions.EnableWANAccessRouterAction;
import org.rm3l.router_companion.actions.EraseWANMonthlyTrafficRouterAction;
import org.rm3l.router_companion.actions.ExecuteCommandRouterAction;
import org.rm3l.router_companion.actions.ManageHTTPdRouterAction;
import org.rm3l.router_companion.actions.RebootRouterAction;
import org.rm3l.router_companion.actions.ResetBandwidthMonitoringCountersRouterAction;
import org.rm3l.router_companion.actions.RouterAction;
import org.rm3l.router_companion.actions.RouterActionListener;
import org.rm3l.router_companion.actions.SetNVRAMVariablesAction;
import org.rm3l.router_companion.actions.ToggleWANAccessPolicyRouterAction;
import org.rm3l.router_companion.actions.UploadAndExecuteScriptRouterAction;
import org.rm3l.router_companion.actions.WakeOnLANRouterAction;
import org.rm3l.router_companion.common.resources.audit.ActionLog;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.router_companion.resources.Device;
import org.rm3l.router_companion.resources.WANAccessPolicy;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.NVRAMParser;
import org.wordpress.passcodelock.AppLockManager;

/**
 * Created by rm3l on 14/02/16.
 */
@DeepLink({
        "ddwrt://routers/{routerUuidOrRouterName}/actions/{action}",
        "dd-wrt://routers/{routerUuidOrRouterName}/actions/{action}"
})
public class RouterActionsDeepLinkActivity extends Activity {

    public static final String DISABLE_DEVICE_WAN_ACCESS = "disable-device-wan-access";

    public static final String ENABLE_DEVICE_WAN_ACCESS = "enable-device-wan-access";

    public static final String DISABLE_WAN_POLICY = "disable-wan-policy";

    public static final String DISABLE_WAN_ACCESS_POLICY = "disable-wan-access-policy";

    public static final String ENABLE_WAN_POLICY = "enable-wan-policy";

    public static final String ENABLE_WAN_ACCESS_POLICY = "enable-wan-access-policy";

    public static final String DISABLE_SYSLOG = "disable-syslog";

    public static final String ENABLE_SYSLOG = "enable-syslog";

    public static final String DISABLE_WAN_TRAFFIC_COUNTERS = "disable-wan-traffic-counters";

    public static final String ENABLE_WAN_TRAFFIC_COUNTERS = "enable-wan-traffic-counters";

    public static final String DISABLE_WOLD = "disable-wold";

    public static final String DISABLE_WOL_DAEMON = "disable-wol-daemon";

    public static final String DISABLE_WAKE_ON_LAN_DAEMON = "disable-wake-on-lan-daemon";

    public static final String ENABLE_WOLD = "enable-wold";

    public static final String ENABLE_WOL_DAEMON = "enable-wol-daemon";

    public static final String ENABLE_WAKE_ON_LAN_DAEMON = "enable-wake-on-lan-daemon";

    public static final String DISABLE_PPTPD = "disable-pptpd";

    public static final String DISABLE_PPTP_SERVER = "disable-pptp-server";

    public static final String ENABLE_PPTPD = "enable-pptpd";

    public static final String ENABLE_PPTP_SERVER = "enable-pptp-server";

    public static final String DISABLE_PPTPC = "disable-pptpc";

    public static final String DISABLE_PPTP_CLIENT = "disable-pptp-client";

    public static final String ENABLE_PPTPC = "enable-pptpc";

    public static final String ENABLE_PPTP_CLIENT = "enable-pptp-client";

    public static final String DISABLE_OPENVPND = "disable-openvpnd";

    public static final String DISABLE_OPENVPN_SERVER = "disable-openvpn-server";

    public static final String ENABLE_OPENVPND = "enable-openvpnd";

    public static final String ENABLE_OPENVPN_SERVER = "enable-openvpn-server";

    public static final String DISABLE_OPENVPNC = "disable-openvpnc";

    public static final String DISABLE_OPENVPN_CLIENT = "disable-openvpn-client";

    public static final String ENABLE_OPENVPNC = "enable-openvpnc";

    public static final String ENABLE_OPENVPN_CLIENT = "enable-openvpn-client";

    public static final String WOL = "wol";

    public static final String WAKE_ON_LAN = "wake-on-lan";

    public static final String RESET_BANDWIDTH_COUNTERS = "reset-bandwidth-counters";

    public static final String RESTART_HTTPD = "restart-httpd";

    public static final String START_HTTPD = "start-httpd";

    public static final String STOP_HTTPD = "stop-httpd";

    public static final String ERASE_WAN_TRAFFIC = "erase-wan-traffic";

    public static final String DHCP_RENEW = "dhcp-renew";

    public static final String DHCP_RELEASE = "dhcp-release";

    public static final String CLEAR_DNS_CACHE = "clear-dns-cache";

    public static final String CLEAR_ARP_CACHE = "clear-arp-cache";

    public static final String RESTART = "restart";

    public static final String REBOOT = "reboot";

    public static final String EXEC_FILE = "exec-file";

    public static final String EXEC_CUSTOM = "exec-custom";

    private static final String LOG_TAG = RouterActionsDeepLinkActivity.class.getSimpleName();

    private static final int PIN_LOCK_REQUEST_CODE = 1;

    private DDWRTCompanionDAO mDao;

    private Bundle mParameters;

    private Collection<Router> mRouters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (!getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE).getBoolean(
                SECURITY_THIRD_PARTY_INTEGRATION, false)) {
            //User disabled 3rd-party integration => abort right away
            Crashlytics.log(Log.WARN, LOG_TAG, "User disabled 3rd-party integration");
            Toast.makeText(this,
                    "DD-WRT Companion: Action denied because user disabled 3rd-party integration.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        this.mDao = RouterManagementActivity.getDao(this);

        final Intent intent = getIntent();
        if (intent.getBooleanExtra(DeepLink.IS_DEEP_LINK, false)) {
            //Deep link
            this.mParameters = intent.getExtras();

            if (AppLockManager.getInstance().getAppLock().isPasswordLocked()) {
                //Check password
                final String pinCode = mParameters.getString("pinCode");
                if (TextUtils.isEmpty(pinCode)) {
                    Crashlytics.log(Log.WARN, LOG_TAG, "PIN Code is blank");

                    if (BuildConfig.DEBUG) {
                        Toast.makeText(this, "PIN Code is blank", Toast.LENGTH_SHORT).show();
                    }

                    //Does not work
                    //                    final Intent i = new Intent(getApplicationContext(), PasscodeUnlockActivity.class);
                    //                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    //                    startActivityForResult(i, PIN_LOCK_REQUEST_CODE);

                    finish();
                    return;
                }
                if (!AppLockManager.getInstance().getAppLock().verifyPassword(pinCode)) {
                    Crashlytics.log(Log.WARN, LOG_TAG, "Invalid PIN Code");
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(this, "Invalid PIN Code", Toast.LENGTH_SHORT).show();
                    }
                    finish();
                    return;
                }
            }

            if (!handleParametersUponPasswordCheck()) {
                return;
            }
        }

        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PIN_LOCK_REQUEST_CODE: {
                // Make sure the request was successful
                if (resultCode == RESULT_OK) {
                    handleParametersUponPasswordCheck();
                } else {
                    Crashlytics.log(Log.WARN, LOG_TAG, "PIN Code Request did not succeed");
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(this, "PIN Code Request did not succeed", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
            default:
                break;
        }
        finish();
    }

    private boolean handleParametersUponPasswordCheck() {
        if (this.mParameters == null) {
            Crashlytics.log(Log.WARN, LOG_TAG, "Parameters NULL");
            if (BuildConfig.DEBUG) {
                Toast.makeText(this, "Parameters NULL", Toast.LENGTH_SHORT).show();
            }
            finish();
            return false;
        }
        final String origin = mParameters.getString("origin");
        if (TextUtils.isEmpty(origin)) {
            Crashlytics.log(Log.WARN, LOG_TAG, "Origin cannot be blank");
            if (BuildConfig.DEBUG) {
                Toast.makeText(this, "Origin cannot be blank", Toast.LENGTH_SHORT).show();
            }
            finish();
            return false;
        }

        final String routerUuidOrRouterName = mParameters.getString("routerUuidOrRouterName");
        boolean isUuid;
        try {
            //noinspection ResultOfMethodCallIgnored
            UUID.fromString(routerUuidOrRouterName);
            isUuid = true;
        } catch (final Exception e) {
            //No worries
            isUuid = false;
        }
        if (isUuid) {
            final Router router = mDao.getRouter(routerUuidOrRouterName);
            if (router != null) {
                mRouters = Collections.singletonList(router);
            } else {
                mRouters = Collections.emptyList();
            }
        } else {
            mRouters = mDao.getRoutersByName(routerUuidOrRouterName);
        }

        if (mRouters.isEmpty()) {
            Crashlytics.log(Log.WARN, LOG_TAG,
                    "No routers found matching this query: " + routerUuidOrRouterName);
            if (BuildConfig.DEBUG) {
                Toast.makeText(this, "No routers found matching this query: " + routerUuidOrRouterName,
                        Toast.LENGTH_SHORT).show();
            }
            finish();
            return false;
        }

        final SharedPreferences globalPrefs =
                getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);

        final String action = Strings.nullToEmpty(mParameters.getString("action")).toLowerCase();

        final List<AbstractRouterAction<?>> routerActions = new ArrayList<>();

        final Date actionDate = new Date();

        for (final Router router : mRouters) {
            if (router == null) {
                continue;
            }

            final ActionLog actionLog = new ActionLog().setUuid(UUID.randomUUID().toString())
                    .setOriginPackageName(origin)
                    .setRouter(router.getUuid())
                    .setDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(actionDate))
                    .setActionName(toHumanReadableName(action))
                    .setActionData(mParameters.toString());

            final AbstractRouterAction<?> routerAction;

            final RouterActionListener routerActionListener = new RouterActionListener() {
                @Override
                public void onRouterActionFailure(@NonNull RouterAction routerAction,
                        @NonNull Router router, @Nullable Exception exception) {
                    actionLog.setStatus(-1);
                    mDao.recordAction(actionLog);
                    finish();
                }

                @Override
                public void onRouterActionSuccess(@NonNull RouterAction routerAction,
                        @NonNull Router router, Object returnData) {
                    actionLog.setStatus(0);
                    mDao.recordAction(actionLog);
                    finish();
                }
            };

            switch (action) {

                case EXEC_CUSTOM:
                    final String cmd = Strings.nullToEmpty(mParameters.getString("cmd")).toLowerCase();
                    if (cmd.isEmpty()) {
                        Crashlytics.log(Log.WARN, LOG_TAG, "Missing Custom Command");
                        if (BuildConfig.DEBUG) {
                            Toast.makeText(this, "Missing Custom Command", Toast.LENGTH_SHORT).show();
                        }
                        finish();
                        return false;
                    }
                    routerAction = new ExecuteCommandRouterAction(router, RouterActionsDeepLinkActivity.this,
                            routerActionListener, globalPrefs, true, cmd);
                    break;

                case EXEC_FILE:
                    final String resourceFile =
                            Strings.nullToEmpty(mParameters.getString("file")).toLowerCase();
                    if (resourceFile.isEmpty()) {
                        Crashlytics.log(Log.WARN, LOG_TAG, "Missing path to file");
                        if (BuildConfig.DEBUG) {
                            Toast.makeText(this, "Missing path to file", Toast.LENGTH_SHORT).show();
                        }
                        finish();
                        return false;
                    }
                    final File filePath = new File(resourceFile);
                    if (!filePath.exists()) {
                        Crashlytics.log(Log.WARN, LOG_TAG, "File does not exist: " + resourceFile);
                        if (BuildConfig.DEBUG) {
                            Toast.makeText(this, "File does not exist: " + resourceFile, Toast.LENGTH_SHORT)
                                    .show();
                        }
                        finish();
                        return false;
                    }

                    final String args = mParameters.getString("args");

                    routerAction =
                            new UploadAndExecuteScriptRouterAction(router, RouterActionsDeepLinkActivity.this,
                                    routerActionListener, globalPrefs, filePath.getAbsolutePath(), args);
                    break;

                case REBOOT:
                case RESTART:
                    routerAction = new RebootRouterAction(router, RouterActionsDeepLinkActivity.this,
                            routerActionListener, globalPrefs);
                    break;

                case CLEAR_ARP_CACHE:
                    routerAction = new ClearARPCacheRouterAction(router, RouterActionsDeepLinkActivity.this,
                            routerActionListener, globalPrefs);
                    break;

                case CLEAR_DNS_CACHE:
                    routerAction = new ClearDNSCacheRouterAction(router, RouterActionsDeepLinkActivity.this,
                            routerActionListener, globalPrefs);
                    break;

                case DHCP_RELEASE:
                    routerAction = new DHCPClientRouterAction(router, RouterActionsDeepLinkActivity.this,
                            routerActionListener, globalPrefs, DHCPClientRouterAction.DHCPClientAction.RELEASE);
                    break;

                case DHCP_RENEW:
                    routerAction = new DHCPClientRouterAction(router, RouterActionsDeepLinkActivity.this,
                            routerActionListener, globalPrefs, DHCPClientRouterAction.DHCPClientAction.RENEW);
                    break;

                case ERASE_WAN_TRAFFIC:
                    routerAction =
                            new EraseWANMonthlyTrafficRouterAction(router, RouterActionsDeepLinkActivity.this,
                                    routerActionListener, globalPrefs);
                    break;

                case STOP_HTTPD:
                    routerAction = new ManageHTTPdRouterAction(router, RouterActionsDeepLinkActivity.this,
                            routerActionListener, globalPrefs, ManageHTTPdRouterAction.STOP);
                    break;

                case START_HTTPD:
                    routerAction = new ManageHTTPdRouterAction(router, RouterActionsDeepLinkActivity.this,
                            routerActionListener, globalPrefs, ManageHTTPdRouterAction.START);
                    break;

                case RESTART_HTTPD:
                    routerAction = new ManageHTTPdRouterAction(router, RouterActionsDeepLinkActivity.this,
                            routerActionListener, globalPrefs, ManageHTTPdRouterAction.RESTART);
                    break;

                case RESET_BANDWIDTH_COUNTERS:
                    routerAction = new ResetBandwidthMonitoringCountersRouterAction(router,
                            RouterActionsDeepLinkActivity.this, routerActionListener, globalPrefs);
                    break;

                case WAKE_ON_LAN:
                case WOL: {
                    final String deviceMac = Strings.nullToEmpty(mParameters.getString("mac")).toLowerCase();
                    if (deviceMac.isEmpty()) {
                        Crashlytics.log(Log.WARN, LOG_TAG, "Missing MAC");
                        if (BuildConfig.DEBUG) {
                            Toast.makeText(this, "Missing MAC", Toast.LENGTH_SHORT).show();
                        }
                        finish();
                        return false;
                    }
                    final String deviceWolPortStr = mParameters.getString("port");
                    int wolPort = -1;
                    try {
                        wolPort = Integer.parseInt(deviceWolPortStr);
                    } catch (final NumberFormatException e) {
                        //No worries
                    }
                    final int deviceWolPort = wolPort;

                    //Fetch broadcast addresses
                    routerAction = new ExecuteCommandRouterAction(router, RouterActionsDeepLinkActivity.this,
                            new RouterActionListener() {
                                @Override
                                public void onRouterActionFailure(@NonNull RouterAction routerAction,
                                        @NonNull Router router, @Nullable Exception exception) {
                                    Crashlytics.log(Log.ERROR, LOG_TAG, "Error on action: " + routerAction);
                                    routerActionListener.onRouterActionFailure(routerAction, router, exception);
                                }

                                @Override
                                public void onRouterActionSuccess(@NonNull RouterAction routerAction,
                                        @NonNull Router router, Object returnData) {
                                    if (!(returnData instanceof Map)) {
                                        Crashlytics.log(Log.ERROR, LOG_TAG, "returnData is NOT an instance of Map");
                                        routerActionListener.onRouterActionFailure(routerAction, router,
                                                new IllegalStateException("returnData is NOT an instance of Map"));
                                        return;
                                    }
                                    final Map resultMap = (Map) returnData;
                                    final Object resultForRouter = resultMap.get(router.getUuid());
                                    if (!(resultForRouter instanceof String[])) {
                                        Crashlytics.log(Log.ERROR, LOG_TAG,
                                                "resultForRouter is NOT an instance of String[]");
                                        routerActionListener.onRouterActionFailure(routerAction, router,
                                                new IllegalStateException(
                                                        "resultForRouter is NOT an instance of String[]"));
                                        return;
                                    }
                                    final String[] wanAndLanBroadcast = (String[]) resultForRouter;
                                    if (wanAndLanBroadcast.length == 0) {
                                        Crashlytics.log(Log.ERROR, LOG_TAG, "No broadcast address found");
                                        routerActionListener.onRouterActionFailure(routerAction, router,
                                                new IllegalStateException("No broadcast address found"));
                                        return;
                                    }

                                    //Now send the actual WOL packet
                                    final Device device = new Device(deviceMac);
                                    device.setWolPort(deviceWolPort);
                                    ActionManager.runTasks(
                                            new WakeOnLANRouterAction(router, RouterActionsDeepLinkActivity.this,
                                                    routerActionListener, globalPrefs, device, wanAndLanBroadcast));
                                }
                            }, globalPrefs,
                            "/sbin/ifconfig `/usr/sbin/nvram get wan_iface` | grep Bcast | /usr/bin/awk -F'Bcast:' '{print $2}' | /usr/bin/awk -F'Mask:' '{print $1}'",
                            "/sbin/ifconfig `/usr/sbin/nvram get lan_ifname` | grep Bcast | /usr/bin/awk -F'Bcast:' '{print $2}' | /usr/bin/awk -F'Mask:' '{print $1}'");
                }
                break;

                case ENABLE_OPENVPN_CLIENT:
                case ENABLE_OPENVPNC:
                case DISABLE_OPENVPN_CLIENT:
                case DISABLE_OPENVPNC: {
                    final NVRAMInfo nvramInfo = new NVRAMInfo();
                    nvramInfo.setProperty(NVRAMInfo.Companion.getOPENVPNCL_ENABLE(),
                            action.startsWith("enable") ? "1" : "0");
                    routerAction =
                            new SetNVRAMVariablesAction(router, RouterActionsDeepLinkActivity.this, nvramInfo,
                                    true, routerActionListener, globalPrefs);
                }
                break;

                case ENABLE_OPENVPN_SERVER:
                case ENABLE_OPENVPND:
                case DISABLE_OPENVPN_SERVER:
                case DISABLE_OPENVPND: {
                    final NVRAMInfo nvramInfo = new NVRAMInfo();
                    nvramInfo.setProperty(
                            NVRAMInfo.Companion.getOPENVPN_ENABLE(), action.startsWith("enable") ? "1" : "0");
                    routerAction =
                            new SetNVRAMVariablesAction(router, RouterActionsDeepLinkActivity.this, nvramInfo,
                                    true, routerActionListener, globalPrefs);
                }
                break;

                case ENABLE_PPTP_CLIENT:
                case ENABLE_PPTPC:
                case DISABLE_PPTP_CLIENT:
                case DISABLE_PPTPC: {
                    final NVRAMInfo nvramInfo = new NVRAMInfo();
                    nvramInfo.setProperty(NVRAMInfo.Companion.getPPTPD_CLIENT_ENABLE(),
                            action.startsWith("enable") ? "1" : "0");
                    routerAction =
                            new SetNVRAMVariablesAction(router, RouterActionsDeepLinkActivity.this, nvramInfo,
                                    true, routerActionListener, globalPrefs);
                }
                break;

                case ENABLE_PPTP_SERVER:
                case ENABLE_PPTPD:
                case DISABLE_PPTP_SERVER:
                case DISABLE_PPTPD: {
                    final NVRAMInfo nvramInfo = new NVRAMInfo();
                    nvramInfo.setProperty(
                            NVRAMInfo.Companion.getPPTPD_ENABLE(), action.startsWith("enable") ? "1" : "0");
                    routerAction =
                            new SetNVRAMVariablesAction(router, RouterActionsDeepLinkActivity.this, nvramInfo,
                                    true, routerActionListener, globalPrefs);
                }
                break;

                case ENABLE_WAKE_ON_LAN_DAEMON:
                case ENABLE_WOL_DAEMON:
                case ENABLE_WOLD:
                case DISABLE_WAKE_ON_LAN_DAEMON:
                case DISABLE_WOL_DAEMON:
                case DISABLE_WOLD: {
                    final NVRAMInfo nvramInfo = new NVRAMInfo();
                    nvramInfo.setProperty(
                            NVRAMInfo.Companion.getWOL_ENABLE(), action.startsWith("enable") ? "1" : "0");
                    routerAction =
                            new SetNVRAMVariablesAction(router, RouterActionsDeepLinkActivity.this, nvramInfo,
                                    true, routerActionListener, globalPrefs);
                }
                break;

                case ENABLE_WAN_TRAFFIC_COUNTERS:
                case DISABLE_WAN_TRAFFIC_COUNTERS: {
                    final NVRAMInfo nvramInfo = new NVRAMInfo();
                    nvramInfo.setProperty(
                            NVRAMInfo.Companion.getTTRAFF_ENABLE(), action.startsWith("enable") ? "1" : "0");
                    routerAction =
                            new SetNVRAMVariablesAction(router, RouterActionsDeepLinkActivity.this, nvramInfo,
                                    false, routerActionListener, globalPrefs);
                }
                break;

                case ENABLE_SYSLOG:
                case DISABLE_SYSLOG: {
                    final NVRAMInfo nvramInfo = new NVRAMInfo();
                    nvramInfo.setProperty(
                            NVRAMInfo.Companion.getSYSLOGD_ENABLE(), action.startsWith("enable") ? "1" : "0");
                    routerAction =
                            new SetNVRAMVariablesAction(router, RouterActionsDeepLinkActivity.this, nvramInfo,
                                    true, routerActionListener, globalPrefs);
                }
                break;

                case ENABLE_WAN_ACCESS_POLICY:
                case ENABLE_WAN_POLICY:
                case DISABLE_WAN_ACCESS_POLICY:
                case DISABLE_WAN_POLICY:
                    final String policyName =
                            Strings.nullToEmpty(mParameters.getString("policy")).toLowerCase();
                    if (policyName.isEmpty()) {
                        Crashlytics.log(Log.WARN, LOG_TAG, "Missing policy");
                        if (BuildConfig.DEBUG) {
                            Toast.makeText(this, "Missing Policy", Toast.LENGTH_SHORT).show();
                        }
                        finish();
                        return false;
                    }

                    //Fetch Policies
                    routerAction = new ExecuteCommandRouterAction(router, RouterActionsDeepLinkActivity.this,
                            new RouterActionListener() {
                                @Override
                                public void onRouterActionFailure(@NonNull RouterAction routerAction,
                                        @NonNull Router router, @Nullable Exception exception) {
                                    Crashlytics.log(Log.ERROR, LOG_TAG, "Error on action: " + routerAction);
                                    routerActionListener.onRouterActionFailure(routerAction, router, exception);
                                }

                                @Override
                                public void onRouterActionSuccess(@NonNull RouterAction routerAction,
                                        @NonNull Router router, Object returnData) {
                                    if (!(returnData instanceof Map)) {
                                        Crashlytics.log(Log.ERROR, LOG_TAG, "returnData is NOT an instance of Map");
                                        routerActionListener.onRouterActionFailure(routerAction, router,
                                                new IllegalStateException("returnData is NOT an instance of Map"));
                                        return;
                                    }
                                    final Map resultMap = (Map) returnData;
                                    final Object resultForRouter = resultMap.get(router.getUuid());
                                    if (!(resultForRouter instanceof String[])) {
                                        Crashlytics.log(Log.ERROR, LOG_TAG,
                                                "resultForRouter is NOT an instance of String[]");
                                        routerActionListener.onRouterActionFailure(routerAction, router,
                                                new IllegalStateException(
                                                        "resultForRouter is NOT an instance of String[]"));
                                        return;
                                    }
                                    final String[] policies = (String[]) resultForRouter;
                                    if (policies.length == 0) {
                                        Crashlytics.log(Log.ERROR, LOG_TAG, "No Policy found");
                                        routerActionListener.onRouterActionFailure(routerAction, router,
                                                new IllegalArgumentException("No Policy found"));
                                        return;
                                    }

                                    final NVRAMInfo nvramInfo = NVRAMParser.parseNVRAMOutput(policies);
                                    Properties properties;
                                    if (nvramInfo == null || (properties = nvramInfo.getData()) == null) {
                                        Crashlytics.log(Log.ERROR, LOG_TAG, "No Policy found");
                                        routerActionListener.onRouterActionFailure(routerAction, router,
                                                new IllegalArgumentException("No Policy found"));
                                        return;
                                    }

                                    //Build Policies
                                    final Set<Map.Entry<Object, Object>> entries = properties.entrySet();
                                    for (final Map.Entry<Object, Object> entry : entries) {
                                        final Object key = entry.getKey();
                                        final Object value = entry.getValue();
                                        if (key == null || value == null) {
                                            continue;
                                        }
                                        //Skip empty rules
                                        final String valueStr = value.toString();
                                        if (Strings.isNullOrEmpty(valueStr)) {
                                            continue;
                                        }
                                        final String keyStr = key.toString();
                                        final int keyNb = Integer.parseInt(keyStr.replace("filter_rule", "").trim());

                                        final WANAccessPolicy wanAccessPolicy = new WANAccessPolicy();
                                        wanAccessPolicy.setNumber(keyNb);

                                        final List<String> statusSplitter = Splitter.on("$NAME:")
                                                .omitEmptyStrings()
                                                .trimResults()
                                                .splitToList(valueStr);
                                        if (!statusSplitter.isEmpty()) {
                                            //myPolicy7$DENY:1$$
                                            wanAccessPolicy.setStatus(statusSplitter.get(0).replaceAll("$STAT:", ""));
                                            if (statusSplitter.size() >= 2) {
                                                final String nameAndFollowingStr = statusSplitter.get(1);
                                                final List<String> nameAndFollowingSplitter = Splitter.on("$DENY:")
                                                        .omitEmptyStrings()
                                                        .trimResults()
                                                        .splitToList(nameAndFollowingStr);
                                                if (!nameAndFollowingSplitter.isEmpty()) {
                                                    wanAccessPolicy.setName(nameAndFollowingSplitter.get(0));
                                                    if (nameAndFollowingSplitter.size() >= 2) {
                                                        //1$$
                                                        final String s =
                                                                nameAndFollowingSplitter.get(1)
                                                                        .replaceAll("\\$\\$", "");
                                                        if ("0".equals(s)) {
                                                            wanAccessPolicy.setDenyOrFilter(
                                                                    WANAccessPolicy.Companion.getFILTER());
                                                        } else {
                                                            wanAccessPolicy.setDenyOrFilter(
                                                                    WANAccessPolicy.Companion.getDENY());
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            wanAccessPolicy.setStatus(WANAccessPolicy.Companion.getSTATUS_UNKNOWN());
                                        }

                                        final boolean enable = (action.startsWith("enable"));
                                        final int enableStatus = !enable ? DISABLE
                                                : WANAccessPolicy.Companion.getDENY()
                                                        .equals(wanAccessPolicy.getDenyOrFilter()) ? ENABLE_1
                                                        : ENABLE_2;
                                        ActionManager.runTasks(new ToggleWANAccessPolicyRouterAction(router,
                                                RouterActionsDeepLinkActivity.this, routerActionListener, globalPrefs,
                                                wanAccessPolicy, enableStatus));
                                    }
                                }
                            }, globalPrefs,
                            "/usr/sbin/nvram show | grep -E \"filter_rule.*\" | grep \"" + policyName + "\"");

                    break;

                case ENABLE_DEVICE_WAN_ACCESS:
                case DISABLE_DEVICE_WAN_ACCESS: {
                    final String deviceMac = Strings.nullToEmpty(mParameters.getString("mac")).toLowerCase();
                    if (deviceMac.isEmpty()) {
                        Crashlytics.log(Log.WARN, LOG_TAG, "Missing MAC");
                        if (BuildConfig.DEBUG) {
                            Toast.makeText(this, "Missing MAC", Toast.LENGTH_SHORT).show();
                        }
                        finish();
                        return false;
                    }
                    final Device device = new Device(deviceMac);
                    if (action.startsWith("enable")) {
                        routerAction =
                                new EnableWANAccessRouterAction(router, RouterActionsDeepLinkActivity.this,
                                        routerActionListener, globalPrefs, device);
                    } else {
                        routerAction =
                                new DisableWANAccessRouterAction(router, RouterActionsDeepLinkActivity.this,
                                        routerActionListener, globalPrefs, device);
                    }
                }
                break;

                default:
                    Crashlytics.log(Log.WARN, LOG_TAG, "Unknown action: [" + action + "]");
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(this, "Unknown action: [" + action + "]", Toast.LENGTH_SHORT).show();
                    }
                    finish();
                    return false;
            }
            routerActions.add(routerAction);
        }

        //Execute action right on each router
        if (BuildConfig.DEBUG) {
            Toast.makeText(RouterActionsDeepLinkActivity.this, "Executing action: " + action + "...",
                    Toast.LENGTH_SHORT).show();
        }
        for (final AbstractRouterAction<?> routerActionTask : routerActions) {
            if (routerActionTask == null) {
                continue;
            }
            // Do not record action in the AbstractRouterAction.
            // Our own action listener will perform the appropriate actions
            routerActionTask.setRecordActionForAudit(false);
            ActionManager.runTasks(routerActionTask);
        }
        return true;
    }

    @NonNull
    private static String toHumanReadableName(@NonNull final String action) {
        switch (action) {
            case EXEC_CUSTOM:
                return "Execute custom command";
            case EXEC_FILE:
                return "Execute script from file";
            case REBOOT:
            case RESTART:
                return "Reboot";
            case CLEAR_ARP_CACHE:
                return "Clear ARP Cache";
            case CLEAR_DNS_CACHE:
                return "Clear DNS Cache";
            case DHCP_RELEASE:
                return "DHCP Release";
            case DHCP_RENEW:
                return "DHCP Renew";
            case ERASE_WAN_TRAFFIC:
                return "Erase WAN Traffic Data";
            case STOP_HTTPD:
                return "Stop HTTP Server";
            case START_HTTPD:
                return "Start HTTP Server";
            case RESTART_HTTPD:
                return "Restart HTTP Server";
            case RESET_BANDWIDTH_COUNTERS:
                return "Reset Bandwidth Counters";
            case WAKE_ON_LAN:
            case WOL:
                return "Wake On LAN";
            case ENABLE_OPENVPN_CLIENT:
            case ENABLE_OPENVPNC:
                return "Enable OpenVPN Client";
            case DISABLE_OPENVPN_CLIENT:
            case DISABLE_OPENVPNC:
                return "Disable OpenVPN Client";
            case ENABLE_OPENVPN_SERVER:
            case ENABLE_OPENVPND:
                return "Enable OpenVPN Server";
            case DISABLE_OPENVPN_SERVER:
            case DISABLE_OPENVPND:
                return "Disable OpenVPN Server";
            case ENABLE_PPTP_CLIENT:
            case ENABLE_PPTPC:
                return "Enable PPTP Client";
            case DISABLE_PPTP_CLIENT:
            case DISABLE_PPTPC:
                return "Disable PPTP Client";
            case ENABLE_PPTP_SERVER:
            case ENABLE_PPTPD:
                return "Enable PPTP Server";
            case DISABLE_PPTP_SERVER:
            case DISABLE_PPTPD:
                return "Disable PPTP Server";
            case ENABLE_WAKE_ON_LAN_DAEMON:
            case ENABLE_WOL_DAEMON:
            case ENABLE_WOLD:
                return "Enable Wake On LAN Daemon";
            case DISABLE_WAKE_ON_LAN_DAEMON:
            case DISABLE_WOL_DAEMON:
            case DISABLE_WOLD:
                return "Disable Wake On LAN Daemon";
            case ENABLE_WAN_TRAFFIC_COUNTERS:
                return "Enable WAN Traffic counters";
            case DISABLE_WAN_TRAFFIC_COUNTERS:
                return "Disable WAN Traffic counters";
            case ENABLE_SYSLOG:
                return "Enable Syslog";
            case DISABLE_SYSLOG:
                return "Disable Syslog";
            case ENABLE_WAN_ACCESS_POLICY:
            case ENABLE_WAN_POLICY:
                return "Enable WAN Access Policy";
            case DISABLE_WAN_ACCESS_POLICY:
            case DISABLE_WAN_POLICY:
                return "Disable WAN Access Policy";
            case ENABLE_DEVICE_WAN_ACCESS:
                return "Enable WAN Access for Device";
            case DISABLE_DEVICE_WAN_ACCESS:
                return "Disable WAN Access for Device";
            default:
                return TextUtils.isEmpty(action) ? "Unknown" : action;
        }
    }
}
