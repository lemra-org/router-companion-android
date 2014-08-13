package org.lemra.dd_wrt.utils;

import android.content.res.Resources;
import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.lemra.dd_wrt.R;
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

/**
 * Created by armel on 8/9/14.
 */
public final class Utils {

    public static final String TAG = Utils.class.getSimpleName();

    private Utils() {
    }

    @NotNull
    public static DDWRTBaseFragment[] getFragments(@NotNull final Resources resources, int parentSectionNumber,
                                                         String sortingStrategy) {
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
                tabsToSort[0] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.status_router));
                tabsToSort[1] = DDWRTBaseFragment.newInstance(StatusWANFragment.class, parentSectionTitle, resources.getString(R.string.status_wan));
                tabsToSort[2] = DDWRTBaseFragment.newInstance(StatusLANFragment.class, parentSectionTitle, resources.getString(R.string.status_lan));
                tabsToSort[3] = DDWRTBaseFragment.newInstance(StatusWirelessFragment.class, parentSectionTitle, resources.getString(R.string.status_wireless));
                tabsToSort[4] = DDWRTBaseFragment.newInstance(StatusBandwidthFragment.class, parentSectionTitle, resources.getString(R.string.status_bandwidth));
                tabsToSort[5] = DDWRTBaseFragment.newInstance(StatusSyslogFragment.class, parentSectionTitle, resources.getString(R.string.status_syslog));
                tabsToSort[6] = DDWRTBaseFragment.newInstance(StatusSysinfoFragment.class, parentSectionTitle, resources.getString(R.string.status_sysinfo));
                break;
            case 1:
                parentSectionTitle = resources.getString(R.string.setup);
                //2 = Setup => {Basic, IPv6, DDNS, MAC Cloning, Routing, VLANs, Networking, EoIP}
                tabsToSort = new DDWRTBaseFragment[8];
                tabsToSort[0] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.setup_basic));
                tabsToSort[1] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.setup_ipv6));
                tabsToSort[2] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.setup_ddns));
                tabsToSort[3] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.setup_mac_cloning));
                tabsToSort[4] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.setup_routing));
                tabsToSort[5] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.setup_vlans));
                tabsToSort[6] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.setup_networking));
                tabsToSort[7] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.setup_eoip));
                break;
            case 2:
                parentSectionTitle = resources.getString(R.string.wireless);
                //3 = Wireless => {Basic, Radius, Security, MAC Filter, WL0, WL1, ...}
                tabsToSort = new DDWRTBaseFragment[4];
                tabsToSort[0] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.wireless_basic_settings));
                tabsToSort[1] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.wireless_radius));
                tabsToSort[2] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.wireless_security));
                tabsToSort[3] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.wireless_mac_filter));
                break;
            case 3:
                parentSectionTitle = resources.getString(R.string.services);
                //4 = Services => {Services, FreeRadius, PPoE, VPN, USB, NAS, HotSpot, SIP Proxy, Adblocking, Webserver}
                tabsToSort = new DDWRTBaseFragment[10];
                tabsToSort[0] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.services_services));
                tabsToSort[1] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.services_freeradius));
                tabsToSort[2] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.services_ppoe));
                tabsToSort[3] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.services_vpn));
                tabsToSort[4] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.services_usb));
                tabsToSort[5] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.services_nas));
                tabsToSort[6] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.services_hostspot));
                tabsToSort[7] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.services_sip));
                tabsToSort[8] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.services_adblocking));
                tabsToSort[9] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.services_webserver));
                break;
            case 4:
                //5 = Security => {Firewall, VPN Passthrough}
                parentSectionTitle = resources.getString(R.string.security);
                tabsToSort = new DDWRTBaseFragment[2];
                tabsToSort[0] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.security_firewall));
                tabsToSort[1] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.security_vpn_passthrough));
                break;
            case 5:
                //6 = Access => {WAN}
                parentSectionTitle = resources.getString(R.string.access_restrictions);
                tabsToSort = new DDWRTBaseFragment[1];
                tabsToSort[0] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.access_restrictions_wan));
                break;
            case 6:
                parentSectionTitle = resources.getString(R.string.nat_qos);
                //7 = NAT/QoS => {Port Fwding, Port Range Fwding, Port Triggerring, UPnP, DMZ, QoS}
                tabsToSort = new DDWRTBaseFragment[6];
                tabsToSort[0] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.nat_qos_port_forwarding));
                tabsToSort[1] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.nat_qos_port_range_forwarding));
                tabsToSort[2] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.nat_qos_port_trigger));
                tabsToSort[3] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.nat_qos_upnp));
                tabsToSort[4] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.nat_qos_dmz));
                tabsToSort[5] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.nat_qos_qos));
                break;
            case 7:
                parentSectionTitle = resources.getString(R.string.admin_area);
                //8 => Admin => {Management, Keep Alive, Commands, WOL, Factory, Upgrade, Backup}
                tabsToSort = new DDWRTBaseFragment[7];
                tabsToSort[0] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.admin_area_mgmt));
                tabsToSort[1] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.admin_area_keep_alive));
                tabsToSort[2] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.admin_area_cmds));
                tabsToSort[3] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.admin_area_wol));
                tabsToSort[4] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.admin_area_factory));
                tabsToSort[5] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.admin_area_upgrade));
                tabsToSort[6] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.admin_area_backup));
                break;
            default:
                //This should NOT happen => Error
                parentSectionTitle = resources.getString(R.string.no_data);
                tabsToSort = new DDWRTBaseFragment[1];
                tabsToSort[0] = DDWRTBaseFragment.newInstance(StatusRouterFragment.class, parentSectionTitle, resources.getString(R.string.no_data));
                break;

        }

        return tabsToSort.length > 0 ? sortingStrategyInstance.sort(tabsToSort) : tabsToSort;

    }
}
