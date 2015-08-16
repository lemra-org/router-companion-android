/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014  Armel Soro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact Info: Armel Soro <apps+ddwrt@rm3l.org>
 */

package org.rm3l.ddwrt.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.fragments.access.AccessWANAccessFragment;
import org.rm3l.ddwrt.fragments.admin.AdminCommandsFragment;
import org.rm3l.ddwrt.fragments.admin.AdminNVRAMFragment;
import org.rm3l.ddwrt.fragments.admin.openwrt.AdminNVRAMFragmentOpenWrt;
import org.rm3l.ddwrt.fragments.nat_qos.NATQoSDMZFragment;
import org.rm3l.ddwrt.fragments.nat_qos.NATQoSPortForwardingFragment;
import org.rm3l.ddwrt.fragments.nat_qos.NATQoSPortRangeForwardingFragment;
import org.rm3l.ddwrt.fragments.nat_qos.NATQoSPortTriggeringFragment;
import org.rm3l.ddwrt.fragments.nat_qos.NATQoSQoSFragment;
import org.rm3l.ddwrt.fragments.nat_qos.NATQoSUPnPFragment;
import org.rm3l.ddwrt.fragments.security.SecurityFirewallFragment;
import org.rm3l.ddwrt.fragments.security.SecurityVPNPassthroughFragment;
import org.rm3l.ddwrt.fragments.services.ServicesOpenVPNClientFragment;
import org.rm3l.ddwrt.fragments.services.ServicesOpenVPNLogsFragment;
import org.rm3l.ddwrt.fragments.services.ServicesWakeOnLanDaemonFragment;
import org.rm3l.ddwrt.fragments.services.ServicesWakeOnLanFragment;
import org.rm3l.ddwrt.fragments.setup.SetupBasicFragment;
import org.rm3l.ddwrt.fragments.setup.SetupDDNSFragment;
import org.rm3l.ddwrt.fragments.setup.SetupEoIPFragment;
import org.rm3l.ddwrt.fragments.setup.SetupIPv6Fragment;
import org.rm3l.ddwrt.fragments.setup.SetupMacCloningFragment;
import org.rm3l.ddwrt.fragments.setup.SetupNetworkingFragment;
import org.rm3l.ddwrt.fragments.setup.SetupRoutingFragment;
import org.rm3l.ddwrt.fragments.setup.SetupVLANFragment;
import org.rm3l.ddwrt.fragments.status.StatusBandwidthFragment;
import org.rm3l.ddwrt.fragments.status.StatusClientsFragment;
import org.rm3l.ddwrt.fragments.status.StatusLANFragment;
import org.rm3l.ddwrt.fragments.status.StatusMonitoringWANFragment;
import org.rm3l.ddwrt.fragments.status.StatusRouterFragment;
import org.rm3l.ddwrt.fragments.status.StatusSyslogFragment;
import org.rm3l.ddwrt.fragments.status.StatusTimeFragment;
import org.rm3l.ddwrt.fragments.status.StatusWANFragment;
import org.rm3l.ddwrt.fragments.status.StatusWirelessFragment;
import org.rm3l.ddwrt.fragments.status.openwrt.StatusRouterFragmentOpenWrt;
import org.rm3l.ddwrt.fragments.status.openwrt.StatusWANFragmentOpenWrt;
import org.rm3l.ddwrt.fragments.toolbox.ToolboxArpingFragment;
import org.rm3l.ddwrt.fragments.toolbox.ToolboxMACOUILookupFragment;
import org.rm3l.ddwrt.fragments.toolbox.ToolboxNsLookupFragment;
import org.rm3l.ddwrt.fragments.toolbox.ToolboxPingFragment;
import org.rm3l.ddwrt.fragments.toolbox.ToolboxTracerouteFragment;
import org.rm3l.ddwrt.fragments.toolbox.ToolboxWhoisFragment;
import org.rm3l.ddwrt.fragments.wireless.WirelessBasicFragmentAbstract;
import org.rm3l.ddwrt.fragments.wireless.WirelessMACFilteringFragmentAbstract;
import org.rm3l.ddwrt.fragments.wireless.WirelessRadiusFragmentAbstract;
import org.rm3l.ddwrt.fragments.wireless.WirelessSecurityFragmentAbstract;
import org.rm3l.ddwrt.main.DDWRTMainActivity;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.prefs.sort.DDWRTSortingStrategy;
import org.rm3l.ddwrt.prefs.sort.SortingStrategy;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.BannerAdTile;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.widget.FrameLayout.LayoutParams;


/**
 * Abstract base fragment
 *
 * @author <a href="mailto:apps+ddwrt@rm3l.org">Armel S.</a>
 */
public abstract class AbstractBaseFragment<T> extends Fragment implements LoaderManager.LoaderCallbacks<T> {

    public static final String TAB_TITLE = "fragment_tab_title";
    public static final String FRAGMENT_CLASS = "fragment_class";
    public static final String ROUTER_CONNECTION_INFO = "router_info";
    public static final String PARENT_SECTION_TITLE = "parent_section_title";
    public static final String STATE_LOADER_IDS = "loaderIds";
    private static final String LOG_TAG = AbstractBaseFragment.class.getSimpleName();
    protected final Handler mHandler = new Handler();
    private final Map<Integer, Object> loaderIdsInUse = Maps.newHashMap();
    protected LinearLayout mLayout;
    protected Router router;
    protected boolean mLoaderStopped = true;
    protected ViewGroup viewGroup;
    @Nullable
    protected DDWRTMainActivity ddwrtMainActivity;
    private CharSequence mTabTitle;
    private CharSequence mParentSectionTitle;
    @Nullable
    private List<DDWRTTile> fragmentTiles; //fragment tiles and the ID they are assigned
    @Nullable
    private Toolbar toolbar;
    private Class<? extends AbstractBaseFragment> mClazz;
    @NonNull
    private PageSlidingTabStripFragment parentFragment;

    @Nullable
    public static AbstractBaseFragment newInstance(PageSlidingTabStripFragment parentFragment, @NonNull final Class<? extends AbstractBaseFragment> clazz,
                                                   @NonNull final CharSequence parentSectionTitle, @NonNull final CharSequence tabTitle,
                                                   @Nullable final String router) {
        try {
            final AbstractBaseFragment fragment = clazz.newInstance()
                    .setTabTitle(tabTitle)
                    .setParentSectionTitle(parentSectionTitle);
            fragment.mClazz = clazz;
            fragment.parentFragment = parentFragment;

            final ViewPager.OnPageChangeListener parentFragmentOnPageChangeListener = parentFragment.getOnPageChangeListener();
            if (parentFragmentOnPageChangeListener instanceof DDWRTMainActivity) {
                fragment.ddwrtMainActivity = (DDWRTMainActivity) parentFragmentOnPageChangeListener;
                fragment.toolbar = fragment.ddwrtMainActivity.getToolbar();
            }

            Bundle args = new Bundle();
            args.putCharSequence(TAB_TITLE, tabTitle);
            args.putCharSequence(PARENT_SECTION_TITLE, parentSectionTitle);
            args.putString(FRAGMENT_CLASS, clazz.getCanonicalName());
            args.putString(ROUTER_CONNECTION_INFO, router);
            fragment.setArguments(args);

            return fragment;

        } catch (java.lang.InstantiationException ie) {
            ie.printStackTrace();
            Utils.reportException(ie);
        } catch (IllegalAccessException iae) {
            Utils.reportException(iae);
            iae.printStackTrace();
        }
        return null;
    }

    @NonNull
    public static AbstractBaseFragment[] getFragments(@NonNull PageSlidingTabStripFragment parentFragment, @NonNull final Resources resources, int parentSectionNumber,
                                                      String sortingStrategy,
                                                      @Nullable final String router) {
        Log.d(LOG_TAG, "getFragments(" + parentSectionNumber + ", " + sortingStrategy + ")");

        final Class sortingStrategyClass;
        SortingStrategy sortingStrategyInstance = null;
        Exception exception = null;
        try {
            sortingStrategyClass = Class.forName(sortingStrategy);
            sortingStrategyInstance = (SortingStrategy) sortingStrategyClass.newInstance();
        } catch (@NonNull final Exception e) {
            e.printStackTrace();
            exception = e;
        }

        if (exception != null) {
            //Default one
            Log.d(LOG_TAG, "An error occurred - using DDWRTSortingStrategy default strategy: " + exception);
            sortingStrategyInstance = new DDWRTSortingStrategy();
        }

        final AbstractBaseFragment[] tabsToSort;

        Router.RouterFirmware routerFirmwareForFragments;
        final ViewPager.OnPageChangeListener parentFragmentOnPageChangeListener = parentFragment.getOnPageChangeListener();
        if (parentFragmentOnPageChangeListener instanceof Context) {
            final Router routerFromDao = RouterManagementActivity
                    .getDao((Context) parentFragmentOnPageChangeListener).getRouter(router);
            if (routerFromDao == null) {
                routerFirmwareForFragments = Router.RouterFirmware.UNKNOWN;
            } else {
                final Router.RouterFirmware routerFirmware = routerFromDao.getRouterFirmware();
                if (routerFirmware == null) {
                    routerFirmwareForFragments = Router.RouterFirmware.UNKNOWN;
                } else {
                    routerFirmwareForFragments = routerFirmware;
                }
            }
        } else {
            routerFirmwareForFragments = Router.RouterFirmware.UNKNOWN;
            Utils.reportException(
                    new IllegalArgumentException("parentFragmentOnPageChangeListener NOT instanceof Context"));
        }

        //FIXME Once full support of other firmwares is implemented
        if (routerFirmwareForFragments == null ||
                Router.RouterFirmware.UNKNOWN.equals(routerFirmwareForFragments)) {
            routerFirmwareForFragments = Router.RouterFirmware.DDWRT;
        }
        //FIXME End

        switch (routerFirmwareForFragments) {
            case OPENWRT:
                //Generate specific OpenWRT Fragments
                tabsToSort = getBaseFragmentsOpenWrt(parentFragment, resources, parentSectionNumber, router);
                break;
            case DDWRT:
                tabsToSort = getBaseFragmentsDDWRT(parentFragment, resources, parentSectionNumber, router);
                break;
            default:
                //Unknown
                if (parentFragmentOnPageChangeListener instanceof Context) {
                    Toast.makeText((Context) parentFragmentOnPageChangeListener, "Router Firmware unknown or not supported!", Toast.LENGTH_SHORT)
                            .show();
                }
                Utils.reportException(
                        new IllegalArgumentException("Router Firmware unknown or not supported"));
                tabsToSort = new AbstractBaseFragment[0];
                break;
        }

        return tabsToSort.length > 0 ? sortingStrategyInstance.sort(tabsToSort) : tabsToSort;
    }

    @NonNull
    private static AbstractBaseFragment[] getBaseFragmentsOpenWrt(PageSlidingTabStripFragment parentFragment, Resources resources, int parentSectionNumber, String router) {
        //TODO For OpenWRT
        String parentSectionTitle;

        final AbstractBaseFragment[] tabsToSort;

        switch (parentSectionNumber) {
            //Status: {Status, Wireless, Clients, Monitoring}
            //In DD-WRT: Status => {Router, WAN, LAN, Wireless, Bandwidth, Syslog, Sysinfo}
            case 1:
                parentSectionTitle = resources.getString(R.string.status);
                //1 = Status => {Router, WAN, LAN, Wireless, Bandwidth, Syslog, Sysinfo}
                tabsToSort = new AbstractBaseFragment[2];
                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, StatusRouterFragmentOpenWrt.class, parentSectionTitle,
                        resources.getString(R.string.status_router), router);
                //TODO Add Time
                tabsToSort[1] = AbstractBaseFragment.newInstance(parentFragment, StatusWANFragmentOpenWrt.class, parentSectionTitle,
                        resources.getString(R.string.status_wan), router);
                break;
            case 2:
                //Status > Wireless
                parentSectionTitle = resources.getString(R.string.status);
                //TODO
                tabsToSort = new AbstractBaseFragment[1];
                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, StatusWirelessFragment.class, parentSectionTitle,
                        resources.getString(R.string.status_wireless), router);
                break;
            case 3:
                //Status > Clients
                parentSectionTitle = resources.getString(R.string.status);
                //TODO
                tabsToSort = new AbstractBaseFragment[1];
                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, StatusClientsFragment.class, parentSectionTitle,
                        resources.getString(R.string.status_clients), router);
                break;
            case 4:
                //Status > Monitoring
                parentSectionTitle = resources.getString(R.string.status);
                tabsToSort = new AbstractBaseFragment[2];
                //TODO
                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, StatusMonitoringWANFragment.class, parentSectionTitle,
                        resources.getString(R.string.status_wan), router);
                tabsToSort[1] = AbstractBaseFragment.newInstance(parentFragment, StatusBandwidthFragment.class, parentSectionTitle,
                        resources.getString(R.string.status_bandwidth), router);
                break;

            //Services: {OpenVPN}
            //In DD-WRT: Services => {Services, FreeRadius, PPoE, VPN, USB, NAS, HotSpot, SIP Proxy, Adblocking, Webserver}
            case 6:
                //Services > OpenVPN
                //TODO
                parentSectionTitle = resources.getString(R.string.services);
                tabsToSort = new AbstractBaseFragment[2];
                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, ServicesOpenVPNClientFragment.class, parentSectionTitle,
                        resources.getString(R.string.services_openvpn_client), router);
                //TODO Add OpenVPN Server between Client and Logs
                tabsToSort[1] = AbstractBaseFragment.newInstance(parentFragment, ServicesOpenVPNLogsFragment.class, parentSectionTitle,
                        resources.getString(R.string.services_openvpn_logs), router);

//                tabsToSort = new DDWRTBaseFragment[10];
//                tabsToSort[0] = DDWRTBaseFragment.newInstance(ServicesServicesFragment.class, parentSectionTitle,
//                        resources.getString(R.string.services_services), router);
//                tabsToSort[1] = DDWRTBaseFragment.newInstance(ServicesFreeRadiusFragment.class, parentSectionTitle,
//                        resources.getString(R.string.services_freeradius), router);
//                tabsToSort[2] = DDWRTBaseFragment.newInstance(ServicesPPoEFragment.class, parentSectionTitle,
//                        resources.getString(R.string.services_ppoe), router);
//                tabsToSort[3] = DDWRTBaseFragment.newInstance(ServicesOpenVPNFragment.class, parentSectionTitle,
//                        resources.getString(R.string.services_vpn), router);
//                tabsToSort[4] = DDWRTBaseFragment.newInstance(ServicesUSBFragment.class, parentSectionTitle,
//                        resources.getString(R.string.services_usb), router);
//                tabsToSort[5] = DDWRTBaseFragment.newInstance(ServicesNASFragment.class, parentSectionTitle,
//                        resources.getString(R.string.services_nas), router);
//                tabsToSort[6] = DDWRTBaseFragment.newInstance(ServicesHotSpotFragment.class, parentSectionTitle,
//                        resources.getString(R.string.services_hostspot), router);
//                tabsToSort[7] = DDWRTBaseFragment.newInstance(ServicesSIPFragment.class, parentSectionTitle,
//                        resources.getString(R.string.services_sip), router);
//                tabsToSort[8] = DDWRTBaseFragment.newInstance(ServicesAdBlockingFragment.class, parentSectionTitle,
//                        resources.getString(R.string.services_adblocking), router);
//                tabsToSort[9] = DDWRTBaseFragment.newInstance(ServicesWebServerFragment.class, parentSectionTitle,
//                        resources.getString(R.string.services_webserver), router);
                break;
            case 7:
                //Services > Wake On LAN
                //TODO
                parentSectionTitle = resources.getString(R.string.services);
                tabsToSort = new AbstractBaseFragment[0];
                break;
            //Admin: {Commands, NVRAM}
            //In DD-WRT: Admin => {Management, Keep Alive, Commands, WOL, Factory, Upgrade, Backup}
            case 9:
                //Admin > Commands
                parentSectionTitle = resources.getString(R.string.commands);
                tabsToSort = new AbstractBaseFragment[1];
                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, AdminCommandsFragment.class, parentSectionTitle,
                        resources.getString(R.string.command_shell), router);
                break;
            case 10:
                //Admin > NVRAM
                parentSectionTitle = resources.getString(R.string.admin_area);
                tabsToSort = new AbstractBaseFragment[1];
                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, AdminNVRAMFragmentOpenWrt.class, parentSectionTitle,
                        resources.getString(R.string.admin_area_nvram), router);
                //TODO Also Add UCI Config if possible
                break;
            //Toolbox
            //FIXME Add "df", "ps", "dmesg", "mount", ... in "Toolbox > System"  (auto-refreshable)
            //In DD-WRT: N/A
            case 12:
                //Toolbox > Network
                //FIXME Add "netstat" also (auto-refreshable)
                parentSectionTitle = resources.getString(R.string.toolbox);
                tabsToSort = new AbstractBaseFragment[6];
                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, ToolboxPingFragment.class, parentSectionTitle,
                        resources.getString(R.string.toolbox_ping), router);
                tabsToSort[1] = AbstractBaseFragment.newInstance(parentFragment, ToolboxTracerouteFragment.class, parentSectionTitle,
                        resources.getString(R.string.toolbox_traceroute), router);
                tabsToSort[2] = AbstractBaseFragment.newInstance(parentFragment, ToolboxNsLookupFragment.class, parentSectionTitle,
                        resources.getString(R.string.toolbox_nslookup), router);
                tabsToSort[3] = AbstractBaseFragment.newInstance(parentFragment, ToolboxArpingFragment.class, parentSectionTitle,
                        resources.getString(R.string.toolbox_arping), router);
                tabsToSort[4] = AbstractBaseFragment.newInstance(parentFragment, ToolboxWhoisFragment.class, parentSectionTitle,
                        resources.getString(R.string.toolbox_whois), router);
                tabsToSort[5] = AbstractBaseFragment.newInstance(parentFragment, ToolboxMACOUILookupFragment.class, parentSectionTitle,
                        resources.getString(R.string.toolbox_oui_lookup), router);
//                tabsToSort[3] = AbstractBaseFragment.newInstance(parentFragment, ToolboxSubnetCalculatorFragment.class, parentSectionTitle,
//                        resources.getString(R.string.toolbox_subnet_calculator), router);
                break;

            default:
                //This should NOT happen => Error
                parentSectionTitle = (resources.getString(R.string.unknown) + " (" + parentSectionNumber + ")");
                tabsToSort = new AbstractBaseFragment[1];
                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, NoDataFragment.class, parentSectionTitle,
                        resources.getString(R.string.unknown), router);
                break;

        }
        return tabsToSort;


//        switch (parentSectionNumber) {
//            case 0:
//                parentSectionTitle = resources.getString(R.string.status);
//                //1 = Status => {Router, WAN, LAN, Wireless, Bandwidth, Syslog, Sysinfo}
//                tabsToSort = new AbstractBaseFragment[2];
//                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, StatusRouterFragmentOpenWrt.class, parentSectionTitle,
//                        resources.getString(R.string.status_router), router);
//                tabsToSort[1] = AbstractBaseFragment.newInstance(parentFragment, StatusWANFragmentOpenWrt.class, parentSectionTitle,
//                        resources.getString(R.string.status_wan), router);
//                break;
//            case 4:
//                parentSectionTitle = resources.getString(R.string.commands);
//                tabsToSort = new AbstractBaseFragment[1];
//                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, AdminCommandsFragment.class, parentSectionTitle,
//                        resources.getString(R.string.command_shell), router);
//                break;
//            case 5:
//                parentSectionTitle = resources.getString(R.string.admin_area);
//                tabsToSort = new AbstractBaseFragment[1];
//                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, AdminNVRAMFragmentOpenWrt.class, parentSectionTitle,
//                        resources.getString(R.string.admin_area_nvram), router);
//                //TODO Also Add UCI Config if possible
//                break;
//            case 7:
//                parentSectionTitle = resources.getString(R.string.toolbox);
//                tabsToSort = new AbstractBaseFragment[5];
//                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, ToolboxPingFragment.class, parentSectionTitle,
//                        resources.getString(R.string.toolbox_ping), router);
//                tabsToSort[1] = AbstractBaseFragment.newInstance(parentFragment, ToolboxTracerouteFragment.class, parentSectionTitle,
//                        resources.getString(R.string.toolbox_traceroute), router);
//                tabsToSort[2] = AbstractBaseFragment.newInstance(parentFragment, ToolboxNsLookupFragment.class, parentSectionTitle,
//                        resources.getString(R.string.toolbox_nslookup), router);
//                tabsToSort[3] = AbstractBaseFragment.newInstance(parentFragment, ToolboxArpingFragment.class, parentSectionTitle,
//                        resources.getString(R.string.toolbox_arping), router);
//                tabsToSort[4] = AbstractBaseFragment.newInstance(parentFragment, ToolboxWhoisFragment.class, parentSectionTitle,
//                        resources.getString(R.string.toolbox_whois), router);
////                tabsToSort[3] = AbstractBaseFragment.newInstance(parentFragment, ToolboxSubnetCalculatorFragment.class, parentSectionTitle,
////                        resources.getString(R.string.toolbox_subnet_calculator), router);
//
//                break;
//            default:
//                //This should NOT happen => Error
//                parentSectionTitle = (resources.getString(R.string.unknown) + " (" + parentSectionNumber + ")");
//                tabsToSort = new AbstractBaseFragment[1];
//                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, NoDataFragment.class, parentSectionTitle,
//                        resources.getString(R.string.unknown), router);
//                break;
//
//        }
//        return tabsToSort;
    }

    @NonNull
    private static AbstractBaseFragment[] getBaseFragmentsDDWRT(PageSlidingTabStripFragment parentFragment, Resources resources, int parentSectionNumber, String router) {
        String parentSectionTitle;

        final AbstractBaseFragment[] tabsToSort;
        switch (parentSectionNumber) {
            //Status: {Status, Wireless, Clients, Monitoring}
            //In DD-WRT: Status => {Router, WAN, LAN, Wireless, Bandwidth, Syslog, Sysinfo}
            case 1:
                parentSectionTitle = resources.getString(R.string.status);
                tabsToSort = new AbstractBaseFragment[5];
                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, StatusRouterFragment.class, parentSectionTitle,
                        resources.getString(R.string.status_router), router);
                tabsToSort[1] = AbstractBaseFragment.newInstance(parentFragment, StatusTimeFragment.class, parentSectionTitle,
                        resources.getString(R.string.status_time), router);
                tabsToSort[2] = AbstractBaseFragment.newInstance(parentFragment, StatusWANFragment.class, parentSectionTitle,
                        resources.getString(R.string.status_wan), router);
                tabsToSort[3] = AbstractBaseFragment.newInstance(parentFragment, StatusLANFragment.class, parentSectionTitle,
                        resources.getString(R.string.status_lan), router);
                tabsToSort[4] = AbstractBaseFragment.newInstance(parentFragment, StatusSyslogFragment.class, parentSectionTitle,
                        resources.getString(R.string.status_syslog), router);
                break;
            case 2:
                //Status > Wireless
                parentSectionTitle = resources.getString(R.string.status);
                tabsToSort = new AbstractBaseFragment[1];
                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, StatusWirelessFragment.class, parentSectionTitle,
                        resources.getString(R.string.status_wireless), router);
                break;
            case 3:
                //Status > Clients
                parentSectionTitle = resources.getString(R.string.status);
                tabsToSort = new AbstractBaseFragment[1];
                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, StatusClientsFragment.class, parentSectionTitle,
                        resources.getString(R.string.status_clients), router);
                break;
            case 4:
                //Status > Monitoring
                parentSectionTitle = resources.getString(R.string.status);
                tabsToSort = new AbstractBaseFragment[2];
                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, StatusMonitoringWANFragment.class, parentSectionTitle,
                        resources.getString(R.string.status_wan), router);
                tabsToSort[1] = AbstractBaseFragment.newInstance(parentFragment, StatusBandwidthFragment.class, parentSectionTitle,
                        resources.getString(R.string.status_bandwidth), router);
                break;

            //Services: {OpenVPN}
            //In DD-WRT: Services => {Services, FreeRadius, PPoE, VPN, USB, NAS, HotSpot, SIP Proxy, Adblocking, Webserver}
            case 6:
                //Services > OpenVPN
                parentSectionTitle = resources.getString(R.string.services);
                tabsToSort = new AbstractBaseFragment[2];
                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, ServicesOpenVPNClientFragment.class, parentSectionTitle,
                        resources.getString(R.string.services_openvpn_client), router);
                //TODO Add OpenVPN Server between Client and Logs
                tabsToSort[1] = AbstractBaseFragment.newInstance(parentFragment, ServicesOpenVPNLogsFragment.class, parentSectionTitle,
                        resources.getString(R.string.services_openvpn_logs), router);

//                tabsToSort = new DDWRTBaseFragment[10];
//                tabsToSort[0] = DDWRTBaseFragment.newInstance(ServicesServicesFragment.class, parentSectionTitle,
//                        resources.getString(R.string.services_services), router);
//                tabsToSort[1] = DDWRTBaseFragment.newInstance(ServicesFreeRadiusFragment.class, parentSectionTitle,
//                        resources.getString(R.string.services_freeradius), router);
//                tabsToSort[2] = DDWRTBaseFragment.newInstance(ServicesPPoEFragment.class, parentSectionTitle,
//                        resources.getString(R.string.services_ppoe), router);
//                tabsToSort[3] = DDWRTBaseFragment.newInstance(ServicesOpenVPNFragment.class, parentSectionTitle,
//                        resources.getString(R.string.services_vpn), router);
//                tabsToSort[4] = DDWRTBaseFragment.newInstance(ServicesUSBFragment.class, parentSectionTitle,
//                        resources.getString(R.string.services_usb), router);
//                tabsToSort[5] = DDWRTBaseFragment.newInstance(ServicesNASFragment.class, parentSectionTitle,
//                        resources.getString(R.string.services_nas), router);
//                tabsToSort[6] = DDWRTBaseFragment.newInstance(ServicesHotSpotFragment.class, parentSectionTitle,
//                        resources.getString(R.string.services_hostspot), router);
//                tabsToSort[7] = DDWRTBaseFragment.newInstance(ServicesSIPFragment.class, parentSectionTitle,
//                        resources.getString(R.string.services_sip), router);
//                tabsToSort[8] = DDWRTBaseFragment.newInstance(ServicesAdBlockingFragment.class, parentSectionTitle,
//                        resources.getString(R.string.services_adblocking), router);
//                tabsToSort[9] = DDWRTBaseFragment.newInstance(ServicesWebServerFragment.class, parentSectionTitle,
//                        resources.getString(R.string.services_webserver), router);
                break;
            case 7:
                //Services > Wake On LAN
                //TODO
                parentSectionTitle = resources.getString(R.string.services);
                tabsToSort = new AbstractBaseFragment[2];
                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, ServicesWakeOnLanFragment.class, parentSectionTitle,
                        resources.getString(R.string.services_wol), router);
                tabsToSort[1] = AbstractBaseFragment.newInstance(parentFragment, ServicesWakeOnLanDaemonFragment.class, parentSectionTitle,
                        resources.getString(R.string.services_wol_daemon), router);
                break;
            //Admin: {Commands, NVRAM}
            //In DD-WRT: Admin => {Management, Keep Alive, Commands, WOL, Factory, Upgrade, Backup}
            case 9:
                //Admin > Commands
                parentSectionTitle = resources.getString(R.string.commands);
                tabsToSort = new AbstractBaseFragment[1];
                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, AdminCommandsFragment.class, parentSectionTitle,
                        resources.getString(R.string.command_shell), router);
                break;
            case 10:
                //Admin > NVRAM
                parentSectionTitle = resources.getString(R.string.admin_area);
                tabsToSort = new AbstractBaseFragment[1];
//                tabsToSort[0] = DDWRTBaseFragment.newInstance(AdminCommandsFragment.class, parentSectionTitle,
//                        resources.getString(R.string.admin_area_cmds), router);
                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, AdminNVRAMFragment.class, parentSectionTitle,
                        resources.getString(R.string.admin_area_nvram), router);
//                tabsToSort = new DDWRTBaseFragment[7];
//                tabsToSort[0] = DDWRTBaseFragment.newInstance(AdminManagementFragment.class, parentSectionTitle,
//                        resources.getString(R.string.admin_area_mgmt), router);
//                tabsToSort[1] = DDWRTBaseFragment.newInstance(AdminKeepAliveFragment.class, parentSectionTitle,
//                        resources.getString(R.string.admin_area_keep_alive), router);
//                tabsToSort[2] = DDWRTBaseFragment.newInstance(AdminCommandsFragment.class, parentSectionTitle,
//                        resources.getString(R.string.admin_area_cmds), router);
//                tabsToSort[3] = DDWRTBaseFragment.newInstance(AdminWOLFragment.class, parentSectionTitle,
//                        resources.getString(R.string.admin_area_wol), router);
//                tabsToSort[4] = DDWRTBaseFragment.newInstance(AdminFactoryDefaultsFragment.class, parentSectionTitle,
//                        resources.getString(R.string.admin_area_factory), router);
//                tabsToSort[5] = DDWRTBaseFragment.newInstance(AdminUpgradeFragment.class, parentSectionTitle,
//                        resources.getString(R.string.admin_area_upgrade), router);
//                tabsToSort[6] = DDWRTBaseFragment.newInstance(AdminBackupFragment.class, parentSectionTitle,
//                        resources.getString(R.string.admin_area_backup), router);
                break;
            //Toolbox
            //FIXME Add "df", "ps", "dmesg", "mount", ... in "Toolbox > System"  (auto-refreshable)
            //In DD-WRT: N/A
            case 12:
                //Toolbox > Network
                //FIXME Add "netstat" also (auto-refreshable)
                parentSectionTitle = resources.getString(R.string.toolbox);
                tabsToSort = new AbstractBaseFragment[6];
                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, ToolboxPingFragment.class, parentSectionTitle,
                        resources.getString(R.string.toolbox_ping), router);
                tabsToSort[1] = AbstractBaseFragment.newInstance(parentFragment, ToolboxTracerouteFragment.class, parentSectionTitle,
                        resources.getString(R.string.toolbox_traceroute), router);
                tabsToSort[2] = AbstractBaseFragment.newInstance(parentFragment, ToolboxNsLookupFragment.class, parentSectionTitle,
                        resources.getString(R.string.toolbox_nslookup), router);
                tabsToSort[3] = AbstractBaseFragment.newInstance(parentFragment, ToolboxArpingFragment.class, parentSectionTitle,
                        resources.getString(R.string.toolbox_arping), router);
                tabsToSort[4] = AbstractBaseFragment.newInstance(parentFragment, ToolboxWhoisFragment.class, parentSectionTitle,
                        resources.getString(R.string.toolbox_whois), router);
                tabsToSort[5] = AbstractBaseFragment.newInstance(parentFragment, ToolboxMACOUILookupFragment.class, parentSectionTitle,
                        resources.getString(R.string.toolbox_oui_lookup), router);
//                tabsToSort[3] = AbstractBaseFragment.newInstance(parentFragment, ToolboxSubnetCalculatorFragment.class, parentSectionTitle,
//                        resources.getString(R.string.toolbox_subnet_calculator), router);

                break;


//            case 1: TODO Remove me
            case 1001:
                parentSectionTitle = resources.getString(R.string.setup);
                //2 = Setup => {Basic, IPv6, DDNS, MAC Cloning, Routing, VLANs, Networking, EoIP}
                tabsToSort = new AbstractBaseFragment[8];
                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, SetupBasicFragment.class, parentSectionTitle,
                        resources.getString(R.string.setup_basic), router);
                tabsToSort[1] = AbstractBaseFragment.newInstance(parentFragment, SetupIPv6Fragment.class, parentSectionTitle,
                        resources.getString(R.string.setup_ipv6), router);
                tabsToSort[2] = AbstractBaseFragment.newInstance(parentFragment, SetupDDNSFragment.class, parentSectionTitle,
                        resources.getString(R.string.setup_ddns), router);
                tabsToSort[3] = AbstractBaseFragment.newInstance(parentFragment, SetupMacCloningFragment.class, parentSectionTitle,
                        resources.getString(R.string.setup_mac_cloning), router);
                tabsToSort[4] = AbstractBaseFragment.newInstance(parentFragment, SetupRoutingFragment.class, parentSectionTitle,
                        resources.getString(R.string.setup_routing), router);
                tabsToSort[5] = AbstractBaseFragment.newInstance(parentFragment, SetupVLANFragment.class, parentSectionTitle,
                        resources.getString(R.string.setup_vlans), router);
                tabsToSort[6] = AbstractBaseFragment.newInstance(parentFragment, SetupNetworkingFragment.class, parentSectionTitle,
                        resources.getString(R.string.setup_networking), router);
                tabsToSort[7] = AbstractBaseFragment.newInstance(parentFragment, SetupEoIPFragment.class, parentSectionTitle,
                        resources.getString(R.string.setup_eoip), router);
                break;
            case 111:
//            case 1:
                parentSectionTitle = resources.getString(R.string.wireless);
                //3 = Wireless => {Basic, Radius, Security, MAC Filter, WL0, WL1, ...}
                tabsToSort = new AbstractBaseFragment[4];
                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, WirelessBasicFragmentAbstract.class, parentSectionTitle,
                        resources.getString(R.string.wireless_basic_settings), router);
                tabsToSort[1] = AbstractBaseFragment.newInstance(parentFragment, WirelessRadiusFragmentAbstract.class, parentSectionTitle,
                        resources.getString(R.string.wireless_radius), router);
                tabsToSort[2] = AbstractBaseFragment.newInstance(parentFragment, WirelessSecurityFragmentAbstract.class, parentSectionTitle,
                        resources.getString(R.string.wireless_security), router);
                tabsToSort[3] = AbstractBaseFragment.newInstance(parentFragment, WirelessMACFilteringFragmentAbstract.class, parentSectionTitle,
                        resources.getString(R.string.wireless_mac_filter), router);
                break;

            case 333:
//            case 3:
                //5 = Security => {Firewall, VPN Passthrough}
                parentSectionTitle = resources.getString(R.string.security);
                tabsToSort = new AbstractBaseFragment[2];
                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, SecurityFirewallFragment.class, parentSectionTitle,
                        resources.getString(R.string.security_firewall), router);
                tabsToSort[1] = AbstractBaseFragment.newInstance(parentFragment, SecurityVPNPassthroughFragment.class, parentSectionTitle,
                        resources.getString(R.string.security_vpn_passthrough), router);
                break;
            case 444:
                //6 = Access => {WAN}
                parentSectionTitle = resources.getString(R.string.access_restrictions);
                tabsToSort = new AbstractBaseFragment[1];
                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, AccessWANAccessFragment.class, parentSectionTitle,
                        resources.getString(R.string.access_restrictions_wan), router);
                break;
            case 555:
                parentSectionTitle = resources.getString(R.string.nat_qos);
                //7 = NAT/QoS => {Port Fwding, Port Range Fwding, Port Triggerring, UPnP, DMZ, QoS}
                tabsToSort = new AbstractBaseFragment[6];
                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, NATQoSPortForwardingFragment.class, parentSectionTitle,
                        resources.getString(R.string.nat_qos_port_forwarding), router);
                tabsToSort[1] = AbstractBaseFragment.newInstance(parentFragment, NATQoSPortRangeForwardingFragment.class, parentSectionTitle,
                        resources.getString(R.string.nat_qos_port_range_forwarding), router);
                tabsToSort[2] = AbstractBaseFragment.newInstance(parentFragment, NATQoSPortTriggeringFragment.class, parentSectionTitle,
                        resources.getString(R.string.nat_qos_port_trigger), router);
                tabsToSort[3] = AbstractBaseFragment.newInstance(parentFragment, NATQoSUPnPFragment.class, parentSectionTitle,
                        resources.getString(R.string.nat_qos_upnp), router);
                tabsToSort[4] = AbstractBaseFragment.newInstance(parentFragment, NATQoSDMZFragment.class, parentSectionTitle,
                        resources.getString(R.string.nat_qos_dmz), router);
                tabsToSort[5] = AbstractBaseFragment.newInstance(parentFragment, NATQoSQoSFragment.class, parentSectionTitle,
                        resources.getString(R.string.nat_qos_qos), router);
                break;

//            case 6:


            default:
                //This should NOT happen => Error
                parentSectionTitle = (resources.getString(R.string.unknown) + " (" + parentSectionNumber + ")");
                tabsToSort = new AbstractBaseFragment[1];
                tabsToSort[0] = AbstractBaseFragment.newInstance(parentFragment, NoDataFragment.class, parentSectionTitle,
                        resources.getString(R.string.unknown), router);
                break;

        }
        return tabsToSort;
    }

    public void setLoaderStopped(boolean mLoaderStopped) {
        this.mLoaderStopped = mLoaderStopped;
    }

    @NonNull
    public final AbstractBaseFragment setParentSectionTitle(@NonNull final CharSequence parentSectionTitle) {
        this.mParentSectionTitle = parentSectionTitle;
        return this;
    }

    public final CharSequence getTabTitle() {
        return mTabTitle;
    }

    @NonNull
    public final AbstractBaseFragment setTabTitle(@NonNull final CharSequence tabTitle) {
        this.mTabTitle = tabTitle;
        return this;
    }

    /**
     * Called to do initial creation of a fragment.  This is called after
     * {@link #onAttach(android.app.Activity)}} and before
     * {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)}.
     * <p/>
     * <p>Note that this can be called while the fragment's activity is
     * still in the process of being created.  As such, you can not rely
     * on things like the activity's content view hierarchy being initialized
     * at this point.  If you want to do work once the activity itself is
     * created, see {@link #onActivityCreated(android.os.Bundle)}.
     *
     * @param savedInstanceState If the fragment is being re-created from
     *                           a previous saved state, this is the state.
     */
    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setHasOptionsMenu(true);

        this.router = RouterManagementActivity.getDao(this.getActivity()).getRouter(getArguments().getString(ROUTER_CONNECTION_INFO));
        Log.d(LOG_TAG, "onCreate() loaderIdsInUse: " + loaderIdsInUse);
        if (savedInstanceState != null) {
            final ArrayList<Integer> loaderIdsSaved = savedInstanceState.getIntegerArrayList(STATE_LOADER_IDS);
            Log.d(LOG_TAG, "onCreate() loaderIdsSaved: " + loaderIdsSaved);
            if (loaderIdsSaved != null) {
                //Destroy existing IDs, if any, as new loaders will get created in onResume()
                final LoaderManager loaderManager = getLoaderManager();
                for (final Integer loaderId : loaderIdsSaved) {
                    if (loaderId == null) {
                        continue;
                    }
                    loaderManager.destroyLoader(loaderId);
                }
            }
        }
        this.loaderIdsInUse.clear();


        final FragmentActivity activity = getActivity();

        viewGroup = (ScrollView) activity.getLayoutInflater()
                .inflate(R.layout.base_tiles_container_scrollview, new ScrollView(activity));

        final List<DDWRTTile> tiles = this.getTiles(savedInstanceState);
        if (BuildConfig.WITH_ADS) {

            if (tiles != null) {
                final int size = tiles.size();
                if (size >= 2) {
                    //Add tiles then insert banner ads in between
                    this.fragmentTiles.addAll(tiles);
                    for (int i = size - 1; i <= 1; i-=2) {
                        this.fragmentTiles.add(i, new BannerAdTile(this, savedInstanceState, this.router));
                    }
                } else {
                    //Add banner add first, then all other tiles
                    this.fragmentTiles.add(new BannerAdTile(this, savedInstanceState, this.router));
                    this.fragmentTiles.addAll(tiles);
                }
            } else {
                this.fragmentTiles.add(new BannerAdTile(this, savedInstanceState, this.router));
            }
        } else {
            this.fragmentTiles = tiles;
        }
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is optional, and non-graphical fragments can return null (which
     * is the default implementation).  This will be called between
     * {@link #onCreate(android.os.Bundle)} and {@link #onActivityCreated(android.os.Bundle)}.
     * <p/>
     * <p>If you return a View from here, you will later be called in
     * {@link #onDestroyView} when the view is being released.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate
     *                           any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's
     *                           UI should be attached to.  The fragment should not add the view itself,
     *                           but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    @NonNull
    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return this.getLayout();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //Android disagrees with that: "Can't retain fragments that are nested in other fragments"
//        // this is really important in order to save the state across screen
//        // configuration changes for example
//        setRetainInstance(true);
    }

    private void initLoaders() {
        // initiate the loaders to do the background work
        final LoaderManager loaderManager = getLoaderManager();

        loaderManager.initLoader(0, null, this);
        this.setLoaderStopped(false);
        loaderIdsInUse.put(0, this);

        Log.d(LOG_TAG, "fragmentTiles: " + this.fragmentTiles);

        if (this.fragmentTiles != null) {
            for (final DDWRTTile ddwrtTile : fragmentTiles) {
                if (ddwrtTile == null) {
                    continue;
                }
                final int nextLoaderId = Long.valueOf(Utils.getNextLoaderId()).intValue();
                loaderManager.initLoader(nextLoaderId, null, ddwrtTile);
                ddwrtTile.setLoaderStopped(false);
                loaderIdsInUse.put(nextLoaderId, ddwrtTile);
            }
        }
    }

    private void stopLoaders() {

        if (this.fragmentTiles != null && !this.fragmentTiles.isEmpty()) {
            for (final DDWRTTile fragmentTile : fragmentTiles) {
                if (fragmentTile == null) {
                    continue;
                }
                fragmentTile.onStop();
            }
        }

        final LoaderManager loaderManager = getLoaderManager();
        for (final Map.Entry<Integer, Object> loaderIdInUse : loaderIdsInUse.entrySet()) {
            loaderManager.destroyLoader(loaderIdInUse.getKey());
            final Object value = loaderIdInUse.getValue();
            if (value == this) {
                //Mark this as stopped
                this.setLoaderStopped(true);
            } else if (value instanceof DDWRTTile) {
                ((DDWRTTile) value).setLoaderStopped(true);
            }
        }
        loaderIdsInUse.clear();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //save the loader ids on file
        outState.putIntegerArrayList(STATE_LOADER_IDS,
                Lists.newArrayList(loaderIdsInUse.keySet()));

        super.onSaveInstanceState(outState);
    }


    @Override
    public void onResume() {
        initLoaders();
        super.onResume();
    }

    @Override
    public void onPause() {
        stopLoaders();
        super.onPause();
    }

    @Override
    public void onStop() {
        stopLoaders();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        stopLoaders();
        super.onDestroy();
    }

    @NonNull
    private ViewGroup getLayout() {
        final LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        final int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources()
                .getDisplayMetrics());
        params.setMargins(margin, margin, margin, margin);

        boolean atLeastOneTileAdded = false;

        final FragmentActivity fragmentActivity = getActivity();
        final boolean isThemeLight = ColorUtils.isThemeLight(this.getActivity());
        final Resources resources = fragmentActivity.getResources();

        if (this.fragmentTiles != null && !this.fragmentTiles.isEmpty()) {

            final List<CardView> cards = new ArrayList<CardView>();

            final CardView.LayoutParams cardViewLayoutParams = new LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT);
            cardViewLayoutParams.rightMargin = R.dimen.marginRight;
            cardViewLayoutParams.leftMargin = R.dimen.marginLeft;
            cardViewLayoutParams.bottomMargin = R.dimen.activity_vertical_margin;

            boolean parentViewGroupRedefinedIfNotEmbeddedWithinScrollView = false;

//            final int fragmentColor = ColorUtils.getColor(this.getClass().getSimpleName());

            for (final DDWRTTile ddwrtTile : this.fragmentTiles) {

                final ViewGroup viewGroupLayout = ddwrtTile.getViewGroupLayout();
                final Integer layoutId = ddwrtTile.getLayoutId();

                atLeastOneTileAdded |= (viewGroupLayout != null);

                if (layoutId == null || viewGroupLayout == null) {
                    continue;
                }

                if (!ddwrtTile.isEmbeddedWithinScrollView()) {
                    if (!parentViewGroupRedefinedIfNotEmbeddedWithinScrollView) {
                        viewGroup = (LinearLayout) getActivity().getLayoutInflater()
                                .inflate(R.layout.base_tiles_container_linearlayout, new LinearLayout(fragmentActivity));
                        parentViewGroupRedefinedIfNotEmbeddedWithinScrollView = true;
                    }
                }

                //Set header background color
//                final View hdrView = viewGroupLayout.findViewById(ddwrtTile.getTileHeaderViewId());
//                if (hdrView != null) {
//                    hdrView.setBackgroundColor(fragmentColor);
//                }

                final TextView titleTextView = (TextView) viewGroupLayout.findViewById(ddwrtTile.getTileTitleViewId());
                if (isThemeLight) {
                    if (titleTextView != null) {
                        titleTextView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                    }
                }

                //Detach this from Parent
                final ViewParent parent = viewGroupLayout.getParent();
                if (parent instanceof ViewGroup) {
                    ((ViewGroup) parent).removeView(viewGroupLayout);
                }

                viewGroupLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));

                final CardView cardView = new CardView(fragmentActivity);

                cardView.setContentPadding(15, 5, 15, 5);
                cardView.setOnClickListener(ddwrtTile);
                cardView.setLayoutParams(cardViewLayoutParams);
//                cardView.setCardBackgroundColor(themeBackgroundColor);
                //Add padding to CardView on v20 and before to prevent intersections between the Card content and rounded corners.
                cardView.setPreventCornerOverlap(true);
                //Add padding in API v21+ as well to have the same measurements with previous versions.
                cardView.setUseCompatPadding(true);

                final Integer tileBackgroundColor = ddwrtTile.getTileBackgroundColor();
                if (tileBackgroundColor != null) {
                    cardView.setCardBackgroundColor(tileBackgroundColor);
                } else {
                    if (isThemeLight) {
                        //Light
                        cardView.setCardBackgroundColor(resources.getColor(R.color.cardview_light_background));
                    } else {
                        //Default is Dark
                        cardView.setCardBackgroundColor(resources.getColor(R.color.cardview_dark_background));
                    }
                }

                cardView.addView(viewGroupLayout);

                cards.add(cardView);
            }

            atLeastOneTileAdded = (!cards.isEmpty());


            final boolean disableNowLayoutAnim = (cards.size() <= 1);

            Log.d(LOG_TAG, "atLeastOneTileAdded: " + atLeastOneTileAdded + ", rows: " + cards.size());

            if (atLeastOneTileAdded) {
                //Drop Everything
//                viewGroup.removeAllViews();

                if (disableNowLayoutAnim) {
                    mLayout = (LinearLayout) viewGroup.findViewById(R.id.tiles_container_scrollview_layout_no_anim);
                    viewGroup.findViewById(R.id.tiles_container_scrollview_layout).setVisibility(View.GONE);
                } else {
                    mLayout = (LinearLayout) viewGroup.findViewById(R.id.tiles_container_scrollview_layout);
                    viewGroup.findViewById(R.id.tiles_container_scrollview_layout_no_anim).setVisibility(View.GONE);
                }
                mLayout.setVisibility(View.VISIBLE);

                for (final CardView card : cards) {
//                    mTableLayout.removeView(row);
                    mLayout.addView(card);
                }

            }

            if (viewGroup instanceof ScrollView) {
                ((ScrollView) viewGroup).setFillViewport(true);
            }
        }

        if (viewGroup == null || !atLeastOneTileAdded) {

            viewGroup = (LinearLayout) getActivity().getLayoutInflater()
                    .inflate(R.layout.base_tiles_container_linearlayout, new LinearLayout(fragmentActivity));

            final TextView view = new TextView(fragmentActivity);
            view.setGravity(Gravity.CENTER);
            view.setText(getResources().getString(R.string.no_data));

            if (isThemeLight) {
                //Light
                view.setBackgroundColor(resources.getColor(R.color.cardview_light_background));
            } else {
                //Default is Dark
                view.setBackgroundColor(resources.getColor(R.color.cardview_dark_background));
            }

//            view.setBackgroundResource(R.drawable.background_card);
            view.setLayoutParams(params);

//            mLayout = (LinearLayout) viewGroup.findViewById(R.id.tiles_container_scrollview_layout);
            mLayout = (LinearLayout) viewGroup.findViewById(R.id.tiles_container_scrollview_layout_no_anim);
            viewGroup.findViewById(R.id.tiles_container_scrollview_layout).setVisibility(View.GONE);
            mLayout.setVisibility(View.VISIBLE);

            mLayout.addView(view);
        }

        viewGroup.setBackgroundColor(getResources().getColor(android.R.color.transparent));
//        viewGroup.setLayoutParams(params);

        return viewGroup;
    }

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param id   The ID whose loader is to be created.
     * @param args Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Nullable
    @Override
    public final Loader<T> onCreateLoader(int id, Bundle args) {
        final Loader<T> loader = this.getLoader(id, args);
        if (loader != null) {
            loader.forceLoad();
        }
        return loader;
    }

    @Nullable
    protected Loader<T> getLoader(int id, Bundle args) {
        return null;
    }

    /**
     * Called when a previously created loader has finished its load.  Note
     * that normally an application is <em>not</em> allowed to commit fragment
     * transactions while in this call, since it can happen after an
     * activity's state is saved.  See {@link android.support.v4.app.FragmentManager#beginTransaction()
     * FragmentManager.openTransaction()} for further discussion on this.
     * <p/>
     * <p>This function is guaranteed to be called prior to the release of
     * the last data that was supplied for this Loader.  At this point
     * you should remove all use of the old data (since it will be released
     * soon), but should not do your own release of the data since its Loader
     * owns it and will take care of that.  The Loader will take care of
     * management of its data so you don't have to.  In particular:
     * <p/>
     * <ul>
     * <li> <p>The Loader will monitor for changes to the data, and report
     * them to you through new calls here.  You should not monitor the
     * data yourself.  For example, if the data is a {@link android.database.Cursor}
     * and you place it in a {@link android.widget.CursorAdapter}, use
     * the {@link android.widget.CursorAdapter#CursorAdapter(android.content.Context,
     * android.database.Cursor, int)} constructor <em>without</em> passing
     * in either {@link android.widget.CursorAdapter#FLAG_AUTO_REQUERY}
     * or {@link android.widget.CursorAdapter#FLAG_REGISTER_CONTENT_OBSERVER}
     * (that is, use 0 for the flags argument).  This prevents the CursorAdapter
     * from doing its own observing of the Cursor, which is not needed since
     * when a change happens you will get a new Cursor throw another call
     * here.
     * <li> The Loader will release the data once it knows the application
     * is no longer using it.  For example, if the data is
     * a {@link android.database.Cursor} from a {@link android.content.CursorLoader},
     * you should not call close() on it yourself.  If the Cursor is being placed in a
     * {@link android.widget.CursorAdapter}, you should use the
     * {@link android.widget.CursorAdapter#swapCursor(android.database.Cursor)}
     * method so that the old Cursor is not closed.
     * </ul>
     *
     * @param loader The Loader that has finished.
     * @param data   The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(Loader<T> loader, T data) {

    }

    /**
     * Called when a previously created loader is being reset, and thus
     * making its data unavailable.  The application should at this point
     * remove any references it has to the Loader's data.
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(Loader<T> loader) {

    }

    @Nullable
    protected abstract List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState);

    public void startActivityForResult(Intent intent, DDWRTTile.ActivityResultListener listener) {
        parentFragment.startActivityForResult(intent, listener);
    }

}
