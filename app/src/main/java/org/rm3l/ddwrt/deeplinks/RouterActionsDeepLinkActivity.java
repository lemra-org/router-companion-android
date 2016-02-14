package org.rm3l.ddwrt.deeplinks;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.airbnb.deeplinkdispatch.DeepLink;
import com.crashlytics.android.Crashlytics;
import com.google.common.base.Strings;

import org.rm3l.ddwrt.actions.AbstractRouterAction;
import org.rm3l.ddwrt.actions.ClearARPCacheRouterAction;
import org.rm3l.ddwrt.actions.ClearDNSCacheRouterAction;
import org.rm3l.ddwrt.actions.DHCPClientRouterAction;
import org.rm3l.ddwrt.actions.EraseWANMonthlyTrafficRouterAction;
import org.rm3l.ddwrt.actions.ManageHTTPdRouterAction;
import org.rm3l.ddwrt.actions.RebootRouterAction;
import org.rm3l.ddwrt.actions.ResetBandwidthMonitoringCountersRouterAction;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

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
                case "wol":
                    //TODO
                    break;
                case "toggle-openvpn-client":
                case "toggle-openvpnc":
                    //TODO
                    break;
                case "enable-openvpn-client":
                case "enable-openvpnc":
                    //TODO
                    break;
                case "disable-openvpn-client":
                case "disable-openvpnc":
                    //TODO
                    break;
                case "toggle-openvpn-server":
                case "toggle-openvpnd":
                    //TODO
                    break;
                case "enable-openvpn-server":
                case "enable-openvpnd":
                    //TODO
                    break;
                case "disable-openvpn-server":
                case "disable-openvpnd":
                    //TODO
                    break;
                case "toggle-pptp-client":
                case "toggle-pptpc":
                    //TODO
                    break;
                case "toggle-pptp-server":
                case "toggle-pptpd":
                    //TODO
                    break;
                case "enable-pptp-server":
                case "enable-pptpd":
                    //TODO
                    break;
                case "disable-pptp-server":
                case "disable-pptpd":
                    //TODO
                    break;
                case "toggle-wake-on-lan-daemon":
                case "toggle-wol-daemon":
                case "toggle-wold":
                    //TODO
                    break;
                case "enable-wake-on-lan-daemon":
                case "enable-wol-daemon":
                case "enable-wold":
                    //TODO
                    break;
                case "disable-wake-on-lan-daemon":
                case "disable-wol-daemon":
                case "disable-wold":
                    //TODO
                    break;
                case "toggle-wan-traffic":
                    //TODO
                    break;
                case "enable-wan-traffic":
                    //TODO
                    break;
                case "disable-wan-traffic":
                    //TODO
                    break;
                case "toggle-syslog":
                    //TODO
                    break;
                case "enable-syslog":
                    //TODO
                    break;
                case "disable-syslog":
                    //TODO
                    break;
                case "toggle-wan-access-policy":
                case "toggle-wan-policy":
                    //TODO
                    break;
                case "enable-wan-access-policy":
                case "enable-wan-policy":
                    //TODO
                    break;
                case "disable-wan-access-policy":
                case "disable-wan-policy":
                    //TODO
                    break;
                case "toggle-device-wan-access":
                    //TODO
                    break;
                case "enable-device-wan-access":
                    //TODO
                    break;
                case "disable-device-wan-access":
                    //TODO
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
