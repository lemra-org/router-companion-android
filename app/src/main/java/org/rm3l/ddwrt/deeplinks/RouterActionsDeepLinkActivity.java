package org.rm3l.ddwrt.deeplinks;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.airbnb.deeplinkdispatch.DeepLink;
import com.crashlytics.android.Crashlytics;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import org.rm3l.ddwrt.actions.AbstractRouterAction;
import org.rm3l.ddwrt.actions.ClearARPCacheRouterAction;
import org.rm3l.ddwrt.actions.ClearDNSCacheRouterAction;
import org.rm3l.ddwrt.actions.DHCPClientRouterAction;
import org.rm3l.ddwrt.actions.DisableWANAccessRouterAction;
import org.rm3l.ddwrt.actions.EnableWANAccessRouterAction;
import org.rm3l.ddwrt.actions.EraseWANMonthlyTrafficRouterAction;
import org.rm3l.ddwrt.actions.ExecuteCommandRouterAction;
import org.rm3l.ddwrt.actions.ManageHTTPdRouterAction;
import org.rm3l.ddwrt.actions.RebootRouterAction;
import org.rm3l.ddwrt.actions.ResetBandwidthMonitoringCountersRouterAction;
import org.rm3l.ddwrt.actions.RouterAction;
import org.rm3l.ddwrt.actions.RouterActionListener;
import org.rm3l.ddwrt.actions.SetNVRAMVariablesAction;
import org.rm3l.ddwrt.actions.ToggleWANAccessPolicyRouterAction;
import org.rm3l.ddwrt.actions.WakeOnLANRouterAction;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.resources.Device;
import org.rm3l.ddwrt.resources.WANAccessPolicy;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.NVRAMParser;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import static org.rm3l.ddwrt.actions.ToggleWANAccessPolicyRouterAction.DISABLE;
import static org.rm3l.ddwrt.actions.ToggleWANAccessPolicyRouterAction.ENABLE_1;
import static org.rm3l.ddwrt.actions.ToggleWANAccessPolicyRouterAction.ENABLE_2;

/**
 * Created by rm3l on 14/02/16.
 */
@DeepLink({
        "ddwrt://routers/{routerUuidOrRouterName}/actions/{action}",
        "dd-wrt://routers/{routerUuidOrRouterName}/actions/{action}"})
public class RouterActionsDeepLinkActivity extends Activity {

    private static final String LOG_TAG = RouterActionsDeepLinkActivity.class
            .getSimpleName();

    private DDWRTCompanionDAO mDao;

    private Collection<Router> mRouters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        this.mDao = RouterManagementActivity.getDao(this);

        final Intent intent = getIntent();
        if (intent.getBooleanExtra(DeepLink.IS_DEEP_LINK, false)) {
            //Deep link
            final Bundle parameters = intent.getExtras();

            final String routerUuidOrRouterName = parameters.getString("routerUuidOrRouterName");
            boolean isUuid;
            try {
                //noinspection ResultOfMethodCallIgnored
                UUID.fromString(routerUuidOrRouterName);
                isUuid = true;
            } catch (final Exception e) {
                //No worries
                Crashlytics.logException(e);
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
                Toast.makeText(this,
                        "No routers found matching this query: " + routerUuidOrRouterName,
                        Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            final SharedPreferences globalPrefs = getSharedPreferences(DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY,
                    Context.MODE_PRIVATE);

            final String action = Strings.nullToEmpty(parameters.getString("action"))
                    .toLowerCase();

            AbstractRouterAction<?> routerAction = null;

            switch (action) {

                case "reboot":
                case "restart":
                    routerAction = new RebootRouterAction(RouterActionsDeepLinkActivity.this,
                            null,
                            globalPrefs);
                    break;

                case "clear-arp-cache":
                    routerAction = new ClearARPCacheRouterAction(RouterActionsDeepLinkActivity.this,
                            null,
                            globalPrefs);
                    break;

                case "clear-dns-cache":
                    routerAction = new ClearDNSCacheRouterAction(RouterActionsDeepLinkActivity.this,
                            null,
                            globalPrefs);
                    break;

                case "dhcp-release":
                    routerAction = new DHCPClientRouterAction(RouterActionsDeepLinkActivity.this,
                            null,
                            globalPrefs,
                            DHCPClientRouterAction.DHCPClientAction.RELEASE);
                    break;

                case "dhcp-renew":
                    routerAction = new DHCPClientRouterAction(RouterActionsDeepLinkActivity.this,
                            null,
                            globalPrefs,
                            DHCPClientRouterAction.DHCPClientAction.RENEW);
                    break;

                case "erase-wan-traffic":
                    routerAction = new EraseWANMonthlyTrafficRouterAction(RouterActionsDeepLinkActivity.this,
                            null,
                            globalPrefs);
                    break;

                case "stop-httpd":
                    routerAction = new ManageHTTPdRouterAction(RouterActionsDeepLinkActivity.this,
                            null,
                            globalPrefs,
                            ManageHTTPdRouterAction.STOP);
                    break;

                case "start-httpd":
                    routerAction = new ManageHTTPdRouterAction(RouterActionsDeepLinkActivity.this,
                            null,
                            globalPrefs,
                            ManageHTTPdRouterAction.START);
                    break;

                case "restart-httpd":
                    routerAction = new ManageHTTPdRouterAction(RouterActionsDeepLinkActivity.this,
                            null,
                            globalPrefs,
                            ManageHTTPdRouterAction.RESTART);
                    break;

                case "reset-bandwidth-counters":
                    routerAction = new ResetBandwidthMonitoringCountersRouterAction(RouterActionsDeepLinkActivity.this,
                            null,
                            globalPrefs);
                    break;

                case "wake-on-lan":
                case "wol": {
                    final String deviceMac = Strings.nullToEmpty(parameters.getString("mac"))
                            .toLowerCase();
                    if (deviceMac.isEmpty()) {
                        Crashlytics.log(Log.WARN, LOG_TAG, "Missing MAC");
                        Toast.makeText(this, "Missing MAC", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    final String deviceWolPortStr = parameters.getString("port");
                    int wolPort = -1;
                    try {
                        wolPort = Integer.parseInt(deviceWolPortStr);
                    } catch (final NumberFormatException e) {
                        //No worries
                    }
                    final int deviceWolPort = wolPort;

                    //Fetch broadcast addresses
                    routerAction = new ExecuteCommandRouterAction(
                            RouterActionsDeepLinkActivity.this,
                            new RouterActionListener() {
                                @Override
                                public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router, Object returnData) {
                                    if (!(returnData instanceof Map)) {
                                        Crashlytics.log(Log.ERROR, LOG_TAG, "returnData is NOT an instance of Map");
                                        return;
                                    }
                                    final Map resultMap = (Map) returnData;
                                    final Object resultForRouter = resultMap.get(router.getUuid());
                                    if (!(resultForRouter instanceof String[])) {
                                        Crashlytics.log(Log.ERROR, LOG_TAG, "resultForRouter is NOT an instance of String[]");
                                        return;
                                    }
                                    final String[] wanAndLanBroadcast = (String[]) resultForRouter;
                                    if (wanAndLanBroadcast.length == 0) {
                                        Crashlytics.log(Log.ERROR, LOG_TAG, "No broadcast address found");
                                        return;
                                    }

                                    //Now send the actual WOL packet
                                    final Device device = new Device(deviceMac);
                                    device.setWolPort(deviceWolPort);
                                    new WakeOnLANRouterAction(RouterActionsDeepLinkActivity.this,
                                            null,
                                            globalPrefs,
                                            device,
                                            wanAndLanBroadcast).execute(router);
                                }

                                @Override
                                public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull Router router, @Nullable Exception exception) {
                                    Crashlytics.log(Log.ERROR, LOG_TAG, "Error on action: " + routerAction);
                                }
                            },
                            globalPrefs,
                            "/sbin/ifconfig `/usr/sbin/nvram get wan_iface` | grep Bcast | /usr/bin/awk -F'Bcast:' '{print $2}' | /usr/bin/awk -F'Mask:' '{print $1}'",
                            "/sbin/ifconfig `/usr/sbin/nvram get lan_ifname` | grep Bcast | /usr/bin/awk -F'Bcast:' '{print $2}' | /usr/bin/awk -F'Mask:' '{print $1}'");
                }
                    break;

                case "enable-openvpn-client":
                case "enable-openvpnc":
                case "disable-openvpn-client":
                case "disable-openvpnc": {
                    final NVRAMInfo nvramInfo = new NVRAMInfo();
                    nvramInfo.setProperty(NVRAMInfo.OPENVPNCL_ENABLE,
                            action.startsWith("enable") ? "1" : "0");
                    routerAction = new SetNVRAMVariablesAction(
                            RouterActionsDeepLinkActivity.this,
                            nvramInfo,
                            true,
                            null,
                            globalPrefs);
                }
                    break;

                case "enable-openvpn-server":
                case "enable-openvpnd":
                case "disable-openvpn-server":
                case "disable-openvpnd": {
                    final NVRAMInfo nvramInfo = new NVRAMInfo();
                    nvramInfo.setProperty(NVRAMInfo.OPENVPN_ENABLE,
                            action.startsWith("enable") ? "1" : "0");
                    routerAction = new SetNVRAMVariablesAction(
                            RouterActionsDeepLinkActivity.this,
                            nvramInfo,
                            true,
                            null,
                            globalPrefs);
                }
                    break;

                case "enable-pptp-client":
                case "enable-pptpc":
                case "disable-pptp-client":
                case "disable-pptpc":
                {
                    final NVRAMInfo nvramInfo = new NVRAMInfo();
                    nvramInfo.setProperty(NVRAMInfo.PPTPD_CLIENT_ENABLE,
                            action.startsWith("enable") ? "1" : "0");
                    routerAction = new SetNVRAMVariablesAction(
                            RouterActionsDeepLinkActivity.this,
                            nvramInfo,
                            true,
                            null,
                            globalPrefs);
                }
                    break;

                case "enable-pptp-server":
                case "enable-pptpd":
                case "disable-pptp-server":
                case "disable-pptpd":
                {
                    final NVRAMInfo nvramInfo = new NVRAMInfo();
                    nvramInfo.setProperty(NVRAMInfo.PPTPD_ENABLE,
                            action.startsWith("enable") ? "1" : "0");
                    routerAction = new SetNVRAMVariablesAction(
                            RouterActionsDeepLinkActivity.this,
                            nvramInfo,
                            true,
                            null,
                            globalPrefs);
                }
                    break;

                case "enable-wake-on-lan-daemon":
                case "enable-wol-daemon":
                case "enable-wold":
                case "disable-wake-on-lan-daemon":
                case "disable-wol-daemon":
                case "disable-wold":
                {
                    final NVRAMInfo nvramInfo = new NVRAMInfo();
                    nvramInfo.setProperty(NVRAMInfo.WOL_ENABLE,
                            action.startsWith("enable") ? "1" : "0");
                    routerAction = new SetNVRAMVariablesAction(
                            RouterActionsDeepLinkActivity.this,
                            nvramInfo,
                            true,
                            null,
                            globalPrefs);
                }
                    break;

                case "enable-wan-traffic-counters":
                case "disable-wan-traffic-counters":
                {
                    final NVRAMInfo nvramInfo = new NVRAMInfo();
                    nvramInfo.setProperty(NVRAMInfo.TTRAFF_ENABLE,
                            action.startsWith("enable") ? "1" : "0");
                    routerAction = new SetNVRAMVariablesAction(
                            RouterActionsDeepLinkActivity.this,
                            nvramInfo,
                            false,
                            null,
                            globalPrefs);
                }
                    break;

                case "enable-syslog":
                case "disable-syslog":
                {
                    final NVRAMInfo nvramInfo = new NVRAMInfo();
                    nvramInfo.setProperty(NVRAMInfo.SYSLOGD_ENABLE,
                            action.startsWith("enable") ? "1" : "0");
                    routerAction = new SetNVRAMVariablesAction(
                            RouterActionsDeepLinkActivity.this,
                            nvramInfo,
                            true,
                            null,
                            globalPrefs);
                }
                    break;

                case "enable-wan-access-policy":
                case "enable-wan-policy":
                case "disable-wan-access-policy":
                case "disable-wan-policy":
                    final String policyName = Strings.nullToEmpty(parameters.getString("policy"))
                            .toLowerCase();
                    if (policyName.isEmpty()) {
                        Crashlytics.log(Log.WARN, LOG_TAG, "Missing policy");
                        Toast.makeText(this, "Missing Policy", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    //Fetch Policies
                    routerAction = new ExecuteCommandRouterAction(
                            RouterActionsDeepLinkActivity.this,
                            new RouterActionListener() {
                                @Override
                                public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router, Object returnData) {
                                    if (!(returnData instanceof Map)) {
                                        Crashlytics.log(Log.ERROR, LOG_TAG, "returnData is NOT an instance of Map");
                                        return;
                                    }
                                    final Map resultMap = (Map) returnData;
                                    final Object resultForRouter = resultMap.get(router.getUuid());
                                    if (!(resultForRouter instanceof String[])) {
                                        Crashlytics.log(Log.ERROR, LOG_TAG, "resultForRouter is NOT an instance of String[]");
                                        return;
                                    }
                                    final String[] policies = (String[]) resultForRouter;
                                    if (policies.length == 0) {
                                        Crashlytics.log(Log.ERROR, LOG_TAG, "No Policy found");
                                        return;
                                    }

                                    final NVRAMInfo nvramInfo =
                                            NVRAMParser.parseNVRAMOutput(policies);
                                    Properties properties;
                                    if (nvramInfo == null
                                            || (properties = nvramInfo.getData()) == null) {
                                        Crashlytics.log(Log.ERROR, LOG_TAG, "No Policy found");
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
                                        final int keyNb = Integer.parseInt(
                                                keyStr.replace("filter_rule", "").trim());

                                        final WANAccessPolicy wanAccessPolicy = new WANAccessPolicy()
                                                .setNumber(keyNb);

                                        final List<String> statusSplitter =
                                                Splitter.on("$NAME:").omitEmptyStrings().trimResults()
                                                        .splitToList(valueStr);
                                        if (!statusSplitter.isEmpty()) {
                                            //myPolicy7$DENY:1$$
                                            wanAccessPolicy.setStatus(
                                                    statusSplitter.get(0).replaceAll("$STAT:", ""));
                                            if (statusSplitter.size() >= 2) {
                                                final String nameAndFollowingStr = statusSplitter.get(1);
                                                final List<String> nameAndFollowingSplitter =
                                                        Splitter.on("$DENY:").omitEmptyStrings().trimResults()
                                                                .splitToList(nameAndFollowingStr);
                                                if (!nameAndFollowingSplitter.isEmpty()) {
                                                    wanAccessPolicy.setName(nameAndFollowingSplitter.get(0));
                                                    if (nameAndFollowingSplitter.size() >= 2) {
                                                        //1$$
                                                        final String s =
                                                                nameAndFollowingSplitter.get(1).replaceAll("$$", "");
                                                        if ("0".equals(s)) {
                                                            wanAccessPolicy.setDenyOrFilter(WANAccessPolicy.FILTER);
                                                        } else {
                                                            wanAccessPolicy.setDenyOrFilter(WANAccessPolicy.DENY);
                                                        }

                                                    }
                                                }
                                            }
                                        } else {
                                            wanAccessPolicy.setStatus(WANAccessPolicy.STATUS_UNKNOWN);
                                        }

                                        final boolean enable = (action.startsWith("enable"));
                                        final int enableStatus = !enable ? DISABLE :
                                                WANAccessPolicy.DENY.equals(wanAccessPolicy.getDenyOrFilter()) ?
                                                        ENABLE_1 :
                                                        ENABLE_2;
                                        new ToggleWANAccessPolicyRouterAction(RouterActionsDeepLinkActivity.this,
                                                null,
                                                globalPrefs,
                                                wanAccessPolicy,
                                                enableStatus).execute(router);
                                    }
                                }

                                @Override
                                public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull Router router, @Nullable Exception exception) {
                                    Crashlytics.log(Log.ERROR, LOG_TAG, "Error on action: " + routerAction);
                                }
                            },
                            globalPrefs,
                            "/usr/sbin/nvram show | grep -E \"filter_rule.*\" | grep \"" + policyName + "\"");

                    break;

                case "enable-device-wan-access":
                case "disable-device-wan-access": {
                    final String deviceMac = Strings.nullToEmpty(parameters.getString("mac"))
                            .toLowerCase();
                    if (deviceMac.isEmpty()) {
                        Crashlytics.log(Log.WARN, LOG_TAG, "Missing MAC");
                        Toast.makeText(this, "Missing MAC", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    final Device device = new Device(deviceMac);
                    if (action.startsWith("enable")) {
                        routerAction = new EnableWANAccessRouterAction(
                                RouterActionsDeepLinkActivity.this,
                                null,
                                globalPrefs,
                                device
                        );
                    } else {
                        routerAction = new DisableWANAccessRouterAction(
                                RouterActionsDeepLinkActivity.this,
                                null,
                                globalPrefs,
                                device
                        );
                    }
                }
                    break;

                default:
                    Crashlytics.log(Log.WARN, LOG_TAG, "Unknown action: [" + action + "]");
                    Toast.makeText(this,
                            "Unknown action: [" + action + "]",
                            Toast.LENGTH_SHORT).show();
                    finish();
                    return;
            }

            //Execute action right on each router
            if (routerAction != null) {
                Toast.makeText(RouterActionsDeepLinkActivity.this,
                        "Executing action: " + action + "...",
                        Toast.LENGTH_SHORT).show();
                for (final Router router : mRouters) {
                    routerAction.execute(router);
                }
            } else {
                Toast.makeText(RouterActionsDeepLinkActivity.this,
                        "[TODO] Action to be implemented. =)",
                        Toast.LENGTH_SHORT).show();
            }
        }

        finish();
    }

}
