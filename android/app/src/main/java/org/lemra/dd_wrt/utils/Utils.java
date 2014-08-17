package org.lemra.dd_wrt.utils;

import android.content.res.Resources;
import android.util.Log;

import com.google.common.collect.Lists;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lemra.dd_wrt.R;
import org.lemra.dd_wrt.api.conn.Router;
import org.lemra.dd_wrt.fragments.DDWRTBaseFragment;
import org.lemra.dd_wrt.fragments.status.StatusBandwidthFragment;
import org.lemra.dd_wrt.fragments.status.StatusLANFragment;
import org.lemra.dd_wrt.fragments.status.StatusRouterFragment;
import org.lemra.dd_wrt.fragments.status.StatusSysinfoFragment;
import org.lemra.dd_wrt.fragments.status.StatusSyslogFragment;
import org.lemra.dd_wrt.fragments.status.StatusWANFragment;
import org.lemra.dd_wrt.fragments.status.StatusWirelessFragment;
import org.lemra.dd_wrt.prefs.sort.DDWRTSortingStrategy;
import org.lemra.dd_wrt.prefs.sort.SortingStrategy;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

/**
 * Created by armel on 8/9/14.
 */
public final class Utils {

    public static final String TAG = Utils.class.getSimpleName();

    private Utils() {
    }

    @NotNull
    public static DDWRTBaseFragment[] getFragments(@NotNull final Resources resources, int parentSectionNumber,
                                                         String sortingStrategy,
                                                         @Nullable final Router router) {
        Log.d(TAG, "getFragments("+parentSectionNumber+", " + sortingStrategy + ")");

        final Class sortingStrategyClass;
        SortingStrategy sortingStrategyInstance = null;
        Exception exception = null;
        try {
            sortingStrategyClass = Class.forName(sortingStrategy);
            sortingStrategyInstance = (SortingStrategy) sortingStrategyClass.newInstance();
        } catch (final Exception e) {
            e.printStackTrace();
            exception = e;
        }

        if (exception != null) {
            //Default one
            Log.d(TAG, "An error occurred - using DDWRTSortingStrategy default strategy: " + exception);
            sortingStrategyInstance = new DDWRTSortingStrategy();
        }

        String parentSectionTitle;

        //FIXME Replace with appropriate fragments
        final DDWRTBaseFragment[] tabsToSort;
        switch (parentSectionNumber) {
            case 0:
                parentSectionTitle = resources.getString(R.string.status);
                //1 = Status => {Router, WAN, LAN, Wireless, Bandwidth, Syslog, Sysinfo}
                tabsToSort = new DDWRTBaseFragment[7];
                tabsToSort[0] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.status_router), router);
                tabsToSort[1] = DDWRTBaseFragment.newInstance(StatusWANFragment.class, parentSectionTitle,
                        resources.getString(R.string.status_wan), router);
                tabsToSort[2] = DDWRTBaseFragment.newInstance(StatusLANFragment.class, parentSectionTitle,
                        resources.getString(R.string.status_lan), router);
                tabsToSort[3] = DDWRTBaseFragment.newInstance(StatusWirelessFragment.class, parentSectionTitle,
                        resources.getString(R.string.status_wireless), router);
                tabsToSort[4] = DDWRTBaseFragment.newInstance(StatusBandwidthFragment.class, parentSectionTitle,
                        resources.getString(R.string.status_bandwidth), router);
                tabsToSort[5] = DDWRTBaseFragment.newInstance(StatusSyslogFragment.class, parentSectionTitle,
                        resources.getString(R.string.status_syslog), router);
                tabsToSort[6] = DDWRTBaseFragment.newInstance(StatusSysinfoFragment.class, parentSectionTitle,
                        resources.getString(R.string.status_sysinfo), router);
                break;
            case 1:
                parentSectionTitle = resources.getString(R.string.setup);
                //2 = Setup => {Basic, IPv6, DDNS, MAC Cloning, Routing, VLANs, Networking, EoIP}
                tabsToSort = new DDWRTBaseFragment[8];
                tabsToSort[0] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.setup_basic), router);
                tabsToSort[1] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.setup_ipv6), router);
                tabsToSort[2] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.setup_ddns), router);
                tabsToSort[3] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.setup_mac_cloning), router);
                tabsToSort[4] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.setup_routing), router);
                tabsToSort[5] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.setup_vlans), router);
                tabsToSort[6] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.setup_networking), router);
                tabsToSort[7] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.setup_eoip), router);
                break;
            case 2:
                parentSectionTitle = resources.getString(R.string.wireless);
                //3 = Wireless => {Basic, Radius, Security, MAC Filter, WL0, WL1, ...}
                tabsToSort = new DDWRTBaseFragment[4];
                tabsToSort[0] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.wireless_basic_settings), router);
                tabsToSort[1] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.wireless_radius), router);
                tabsToSort[2] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.wireless_security), router);
                tabsToSort[3] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.wireless_mac_filter), router);
                break;
            case 3:
                parentSectionTitle = resources.getString(R.string.services);
                //4 = Services => {Services, FreeRadius, PPoE, VPN, USB, NAS, HotSpot, SIP Proxy, Adblocking, Webserver}
                tabsToSort = new DDWRTBaseFragment[10];
                tabsToSort[0] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.services_services), router);
                tabsToSort[1] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.services_freeradius), router);
                tabsToSort[2] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.services_ppoe), router);
                tabsToSort[3] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.services_vpn), router);
                tabsToSort[4] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.services_usb), router);
                tabsToSort[5] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.services_nas), router);
                tabsToSort[6] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.services_hostspot), router);
                tabsToSort[7] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.services_sip), router);
                tabsToSort[8] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.services_adblocking), router);
                tabsToSort[9] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.services_webserver), router);
                break;
            case 4:
                //5 = Security => {Firewall, VPN Passthrough}
                parentSectionTitle = resources.getString(R.string.security);
                tabsToSort = new DDWRTBaseFragment[2];
                tabsToSort[0] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.security_firewall), router);
                tabsToSort[1] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.security_vpn_passthrough), router);
                break;
            case 5:
                //6 = Access => {WAN}
                parentSectionTitle = resources.getString(R.string.access_restrictions);
                tabsToSort = new DDWRTBaseFragment[1];
                tabsToSort[0] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.access_restrictions_wan), router);
                break;
            case 6:
                parentSectionTitle = resources.getString(R.string.nat_qos);
                //7 = NAT/QoS => {Port Fwding, Port Range Fwding, Port Triggerring, UPnP, DMZ, QoS}
                tabsToSort = new DDWRTBaseFragment[6];
                tabsToSort[0] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.nat_qos_port_forwarding), router);
                tabsToSort[1] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.nat_qos_port_range_forwarding), router);
                tabsToSort[2] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.nat_qos_port_trigger), router);
                tabsToSort[3] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.nat_qos_upnp), router);
                tabsToSort[4] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.nat_qos_dmz), router);
                tabsToSort[5] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.nat_qos_qos), router);
                break;
            case 7:
                parentSectionTitle = resources.getString(R.string.admin_area);
                //8 => Admin => {Management, Keep Alive, Commands, WOL, Factory, Upgrade, Backup}
                tabsToSort = new DDWRTBaseFragment[7];
                tabsToSort[0] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.admin_area_mgmt), router);
                tabsToSort[1] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.admin_area_keep_alive), router);
                tabsToSort[2] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.admin_area_cmds), router);
                tabsToSort[3] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.admin_area_wol), router);
                tabsToSort[4] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.admin_area_factory), router);
                tabsToSort[5] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.admin_area_upgrade), router);
                tabsToSort[6] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.admin_area_backup), router);
                break;
            default:
                //This should NOT happen => Error
                parentSectionTitle = resources.getString(R.string.no_data);
                tabsToSort = new DDWRTBaseFragment[1];
                tabsToSort[0] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.no_data), null);
                break;

        }

        return tabsToSort.length > 0 ? sortingStrategyInstance.sort(tabsToSort) : tabsToSort;

    }

    public static void readAll(BufferedReader bufferedReader, StringBuffer result) throws IOException {
        for (String line; (line = bufferedReader.readLine()) != null;) {
            Log.d(TAG, "readAll: line=[" + line + "]");
            result.append(line);
        }
    }

    @Nullable
    public static String[] getLines(BufferedReader bufferedReader) throws IOException {
        final List<String> lines = Lists.newArrayList();
        for (String line; (line = bufferedReader.readLine()) != null;) {
            Log.d(TAG, "readAll: line=[" + line + "]");
            lines.add(line);
        }
        return lines.toArray(new String[lines.size()]);
    }
}
