package org.lemra.dd_wrt.utils;

import android.content.res.Resources;
import android.support.v4.app.DialogFragment;
import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.lemra.dd_wrt.DDWRTManagementActivity;
import org.lemra.dd_wrt.R;
import org.lemra.dd_wrt.fragments.status.StatusBandwidthFragment;
import org.lemra.dd_wrt.fragments.status.StatusLANFragment;
import org.lemra.dd_wrt.fragments.status.StatusRouterFragment;
import org.lemra.dd_wrt.fragments.status.StatusSysinfoFragment;
import org.lemra.dd_wrt.fragments.status.StatusSyslogFragment;
import org.lemra.dd_wrt.fragments.status.StatusWANFragment;
import org.lemra.dd_wrt.fragments.status.StatusWirelessFragment;
import org.lemra.dd_wrt.prefs.sort.SortingStrategy;

import static org.lemra.dd_wrt.DDWRTManagementActivity.DDWRTSectionTabFragment;

/**
 * Created by armel on 8/9/14.
 */
public final class Utils {

    private Utils() {
    }

    @NotNull
    public static DDWRTSectionTabFragment[] getFragments(@NotNull final Resources resources, final int parentSectionNumber,
                                                         String sortingStrategy) {
        Log.d(DDWRTManagementActivity.TAG, "getFragments("+parentSectionNumber+", " + sortingStrategy + ")");

        final Class sortingStrategyClass;
        SortingStrategy sortingStrategyInstance;
        try {
            sortingStrategyClass = Class.forName(sortingStrategy);
            sortingStrategyInstance = (SortingStrategy) sortingStrategyClass.newInstance();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (InstantiationException e) {
            e.printStackTrace();
            return null;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }

        //FIXME Replace DialogFragment with appropriate fragments
        final DDWRTSectionTabFragment[] tabsToSort;
        switch (parentSectionNumber) {
            case 1:
                //1 = Status => {Router, WAN, LAN, Wireless, Bandwidth, Syslog, Sysinfo}
                tabsToSort = new DDWRTSectionTabFragment[7];
                tabsToSort[0] = new DDWRTSectionTabFragment(new StatusRouterFragment(), resources.getString(R.string.status_router));
                tabsToSort[1] = new DDWRTSectionTabFragment(new StatusWANFragment(), resources.getString(R.string.status_wan));
                tabsToSort[2] = new DDWRTSectionTabFragment(new StatusLANFragment(), resources.getString(R.string.status_lan));
                tabsToSort[3] = new DDWRTSectionTabFragment(new StatusWirelessFragment(), resources.getString(R.string.status_wireless));
                tabsToSort[4] = new DDWRTSectionTabFragment(new StatusBandwidthFragment(), resources.getString(R.string.status_bandwidth));
                tabsToSort[5] = new DDWRTSectionTabFragment(new StatusSyslogFragment(), resources.getString(R.string.status_syslog));
                tabsToSort[6] = new DDWRTSectionTabFragment(new StatusSysinfoFragment(), resources.getString(R.string.status_sysinfo));
                break;
            case 2:
                //2 = Setup => {Basic, IPv6, DDNS, MAC Cloning, Routing, VLANs, Networking, EoIP}
                tabsToSort = new DDWRTSectionTabFragment[8];
                tabsToSort[0] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.setup_basic));
                tabsToSort[1] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.setup_ipv6));
                tabsToSort[2] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.setup_ddns));
                tabsToSort[3] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.setup_mac_cloning));
                tabsToSort[4] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.setup_routing));
                tabsToSort[5] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.setup_vlans));
                tabsToSort[6] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.setup_networking));
                tabsToSort[7] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.setup_eoip));
                break;
            case 3:
                //3 = Wireless => {Basic, Radius, Security, MAC Filter, WL0, WL1, ...}
                tabsToSort = new DDWRTSectionTabFragment[4];
                tabsToSort[0] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.wireless_basic_settings));
                tabsToSort[1] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.wireless_radius));
                tabsToSort[2] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.wireless_security));
                tabsToSort[3] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.wireless_mac_filter));
                break;
            case 4:
                //4 = Services => {Services, FreeRadius, PPoE, VPN, USB, NAS, HotSpot, SIP Proxy, Adblocking, Webserver}
                tabsToSort = new DDWRTSectionTabFragment[10];
                tabsToSort[0] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.services_services));
                tabsToSort[1] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.services_freeradius));
                tabsToSort[2] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.services_ppoe));
                tabsToSort[3] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.services_vpn));
                tabsToSort[4] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.services_usb));
                tabsToSort[5] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.services_nas));
                tabsToSort[6] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.services_hostspot));
                tabsToSort[7] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.services_sip));
                tabsToSort[8] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.services_adblocking));
                tabsToSort[9] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.services_webserver));
                break;
            case 5:
                //5 = Security => {Firewall, VPN Passthrough}
                tabsToSort = new DDWRTSectionTabFragment[2];
                tabsToSort[0] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.security_firewall));
                tabsToSort[1] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.security_vpn_passthrough));
                break;
            case 6:
                //6 = Access => {WAN}
                tabsToSort = new DDWRTSectionTabFragment[1];
                tabsToSort[0] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.access_restrictions_wan));
                break;
            case 7:
                //7 = NAT/QoS => {Port Fwding, Port Range Fwding, Port Triggerring, UPnP, DMZ, QoS}
                tabsToSort = new DDWRTSectionTabFragment[6];
                tabsToSort[0] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.nat_qos_port_forwarding));
                tabsToSort[1] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.nat_qos_port_range_forwarding));
                tabsToSort[2] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.nat_qos_port_trigger));
                tabsToSort[3] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.nat_qos_upnp));
                tabsToSort[4] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.nat_qos_dmz));
                tabsToSort[5] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.nat_qos_qos));
                break;
            case 8:
                //8 => Admin => {Management, Keep Alive, Commands, WOL, Factory, Upgrade, Backup}
                tabsToSort = new DDWRTSectionTabFragment[7];
                tabsToSort[0] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.admin_area_mgmt));
                tabsToSort[1] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.admin_area_keep_alive));
                tabsToSort[2] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.admin_area_cmds));
                tabsToSort[3] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.admin_area_wol));
                tabsToSort[4] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.admin_area_factory));
                tabsToSort[5] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.admin_area_upgrade));
                tabsToSort[6] = new DDWRTSectionTabFragment(new DialogFragment(), resources.getString(R.string.admin_area_backup));
                break;
            default:
                tabsToSort = new DDWRTSectionTabFragment[0];
                break;

        }

        return tabsToSort.length > 0 ? sortingStrategyInstance.sort(tabsToSort) : tabsToSort;

    }
}
