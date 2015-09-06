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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.fragments.admin.AdminCommandsFragment;
import org.rm3l.ddwrt.fragments.admin.AdminNVRAMFragment;
import org.rm3l.ddwrt.fragments.overview.OverviewNetworkTopologyMapFragment;
import org.rm3l.ddwrt.fragments.services.ServicesOpenVPNClientFragment;
import org.rm3l.ddwrt.fragments.services.ServicesOpenVPNLogsFragment;
import org.rm3l.ddwrt.fragments.services.ServicesPPTPClientFragment;
import org.rm3l.ddwrt.fragments.services.ServicesWakeOnLanDaemonFragment;
import org.rm3l.ddwrt.fragments.services.ServicesWakeOnLanFragment;
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
import org.rm3l.ddwrt.main.DDWRTMainActivity;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.prefs.sort.DDWRTSortingStrategy;
import org.rm3l.ddwrt.prefs.sort.SortingStrategy;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.resources.conn.Router.RouterFirmware;
import org.rm3l.ddwrt.tiles.AvocarrotNativeAdTile;
import org.rm3l.ddwrt.tiles.BannerAdTile;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
    private static AbstractBaseFragment mNoDataFragment;
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

    private static final Map<RouterFirmware,
            ArrayListMultimap<Integer, FragmentTabDescription<? extends AbstractBaseFragment>>>
            allTabs = new HashMap<>();
    static {
        //DD-WRT
        final ArrayListMultimap<Integer, FragmentTabDescription<? extends AbstractBaseFragment>> tabsForDDWRT = getTabsForDDWRT();
        allTabs.put(RouterFirmware.DDWRT, tabsForDDWRT);

        //Demo (same as DD-WRT, actually)
        allTabs.put(RouterFirmware.DEMO, tabsForDDWRT);

        //OpenWRT - TODO
        allTabs.put(RouterFirmware.OPENWRT, getTabsForOpenWRT());
    }

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

        RouterFirmware routerFirmwareForFragments;
        final ViewPager.OnPageChangeListener parentFragmentOnPageChangeListener = parentFragment.getOnPageChangeListener();
        if (parentFragmentOnPageChangeListener instanceof Context) {
            final Router routerFromDao = RouterManagementActivity
                    .getDao((Context) parentFragmentOnPageChangeListener).getRouter(router);
            if (routerFromDao == null) {
                routerFirmwareForFragments = RouterFirmware.UNKNOWN;
            } else {
                final RouterFirmware routerFirmware = routerFromDao.getRouterFirmware();
                if (routerFirmware == null) {
                    routerFirmwareForFragments = RouterFirmware.UNKNOWN;
                } else {
                    routerFirmwareForFragments = routerFirmware;
                }
            }
        } else {
            routerFirmwareForFragments = RouterFirmware.UNKNOWN;
            Utils.reportException(
                    new IllegalArgumentException("parentFragmentOnPageChangeListener NOT instanceof Context"));
        }

        //FIXME Once full support of other firmwares is implemented
        if (routerFirmwareForFragments == null ||
                RouterFirmware.UNKNOWN.equals(routerFirmwareForFragments)) {
            routerFirmwareForFragments = RouterFirmware.DDWRT;
        }
        //FIXME End

        if (mNoDataFragment == null) {
            mNoDataFragment = AbstractBaseFragment.newInstance(parentFragment,
                    NoDataFragment.class,
                    (resources.getString(R.string.unknown) + " (" + parentSectionNumber + ")"),
                    resources.getString(R.string.unknown), router);
        }

        final ArrayListMultimap<Integer, FragmentTabDescription<? extends AbstractBaseFragment>> tabDescriptionMultimap =
                allTabs.get(routerFirmwareForFragments);
        if (tabDescriptionMultimap == null) {
            //Unknown
            if (parentFragmentOnPageChangeListener instanceof Context) {
                Toast.makeText((Context) parentFragmentOnPageChangeListener, "Router Firmware unknown or not supported!", Toast.LENGTH_SHORT)
                        .show();
            }
            Utils.reportException(
                    new IllegalArgumentException("Router Firmware unknown or not supported"));
            tabsToSort = new AbstractBaseFragment[0];
        } else {
            final List<FragmentTabDescription<? extends AbstractBaseFragment>> fragmentTabDescriptions =
                    tabDescriptionMultimap.get(parentSectionNumber);
            if (fragmentTabDescriptions == null || fragmentTabDescriptions.isEmpty()) {
                Utils.reportException(
                        new IllegalArgumentException("Not implemented yet: " + parentSectionNumber));
                //This should NOT happen => Error
                tabsToSort = new AbstractBaseFragment[1];
                tabsToSort[0] = mNoDataFragment;

            } else {
                tabsToSort = new AbstractBaseFragment[fragmentTabDescriptions.size()];
                int i = 0;
                for (final FragmentTabDescription<? extends AbstractBaseFragment> fragmentTabDescription : fragmentTabDescriptions) {
                    tabsToSort[i++] =  AbstractBaseFragment.newInstance(parentFragment,
                            fragmentTabDescription.getClazz(), "???",
                            resources.getString(fragmentTabDescription.getTitleRes()), router);
                }
            }
        }

        return (tabsToSort.length > 0 ? sortingStrategyInstance.sort(tabsToSort) : tabsToSort);
    }

    @NonNull
    private static ArrayListMultimap<Integer, FragmentTabDescription<? extends AbstractBaseFragment>> getTabsForDDWRT() {
        final ArrayListMultimap<Integer, FragmentTabDescription<? extends AbstractBaseFragment>> tabsForDDWRT = ArrayListMultimap.create();
        //1- Overview
        final ArrayList<FragmentTabDescription<? extends AbstractBaseFragment>> overviewTabs = new ArrayList<>();
        overviewTabs.add(new FragmentTabDescription<OverviewNetworkTopologyMapFragment>
                (OverviewNetworkTopologyMapFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.overview_ntm;
            }
        });
        tabsForDDWRT.putAll(1, overviewTabs);

        //2- Status: {Status, Wireless, Clients, Monitoring}
        final ArrayList<FragmentTabDescription<? extends AbstractBaseFragment>> statusTabs = new ArrayList<>();
        statusTabs.add(new FragmentTabDescription<StatusRouterFragment>
                (StatusRouterFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.status_router;
            }
        });
        statusTabs.add(new FragmentTabDescription<StatusTimeFragment>
                (StatusTimeFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.status_time;
            }
        });
        statusTabs.add(new FragmentTabDescription<StatusWANFragment>
                (StatusWANFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.status_wan;
            }
        });
        statusTabs.add(new FragmentTabDescription<StatusLANFragment>
                (StatusLANFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.status_lan;
            }
        });
        statusTabs.add(new FragmentTabDescription<StatusSyslogFragment>
                (StatusSyslogFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.status_syslog;
            }
        });
        tabsForDDWRT.putAll(2, statusTabs);

        //3- Status > Wireless
        final ArrayList<FragmentTabDescription<? extends AbstractBaseFragment>> wirelessTabs = new ArrayList<>();
        wirelessTabs.add(new FragmentTabDescription<StatusWirelessFragment>
                (StatusWirelessFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.status_wireless;
            }
        });
        tabsForDDWRT.putAll(3, wirelessTabs);

        //4- Status > Clients
        final ArrayList<FragmentTabDescription<? extends AbstractBaseFragment>> clientsTabs = new ArrayList<>();
        clientsTabs.add(new FragmentTabDescription<StatusClientsFragment>
                (StatusClientsFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.status_clients;
            }
        });
        tabsForDDWRT.putAll(4, clientsTabs);

        //5- Status > Monitoring
        final ArrayList<FragmentTabDescription<? extends AbstractBaseFragment>> monitoringTabs = new ArrayList<>();
        monitoringTabs.add(new FragmentTabDescription<StatusMonitoringWANFragment>
                (StatusMonitoringWANFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.status_wan;
            }
        });
        monitoringTabs.add(new FragmentTabDescription<StatusBandwidthFragment>
                (StatusBandwidthFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.status_bandwidth;
            }
        });
        tabsForDDWRT.putAll(5, monitoringTabs);

        //7- Services > OpenVPN
        final ArrayList<FragmentTabDescription<? extends AbstractBaseFragment>> servicesOpenVpnTabs = new ArrayList<>();
        //TODO Disabled for now
//        servicesOpenVpnTabs.add(new FragmentTabDescription<ServicesOpenVPNServerFragment>
//                (ServicesOpenVPNServerFragment.class) {
//            @Override
//            public int getTitleRes() {
//                return R.string.services_openvpn_server;
//            }
//        });
        servicesOpenVpnTabs.add(new FragmentTabDescription<ServicesOpenVPNClientFragment>
                (ServicesOpenVPNClientFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.services_openvpn_client;
            }
        });
        servicesOpenVpnTabs.add(new FragmentTabDescription<ServicesOpenVPNLogsFragment>
                (ServicesOpenVPNLogsFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.services_openvpn_logs;
            }
        });
        tabsForDDWRT.putAll(7, servicesOpenVpnTabs);

        //8- Services > PPTP
        final ArrayList<FragmentTabDescription<? extends AbstractBaseFragment>> servicesPPTPTabs = new ArrayList<>();
        //TODO Disabled for now
//        servicesOpenVpnTabs.add(new FragmentTabDescription<ServicesOpenVPNServerFragment>
//                (ServicesOpenVPNServerFragment.class) {
//            @Override
//            public int getTitleRes() {
//                return R.string.services_openvpn_server;
//            }
//        });
        servicesPPTPTabs.add(new FragmentTabDescription<ServicesPPTPClientFragment>
                (ServicesPPTPClientFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.services_pptp_client;
            }
        });
        tabsForDDWRT.putAll(8, servicesPPTPTabs);

        //9- Services > WOL
        final ArrayList<FragmentTabDescription<? extends AbstractBaseFragment>> servicesWolTabs = new ArrayList<>();
        servicesWolTabs.add(new FragmentTabDescription<ServicesWakeOnLanFragment>
                (ServicesWakeOnLanFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.services_wol;
            }
        });
        servicesWolTabs.add(new FragmentTabDescription<ServicesWakeOnLanDaemonFragment>
                (ServicesWakeOnLanDaemonFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.services_wol_daemon;
            }
        });
        tabsForDDWRT.putAll(9, servicesWolTabs);

        //11- Admin > Commands
        final ArrayList<FragmentTabDescription<? extends AbstractBaseFragment>> adminCmdTabs = new ArrayList<>();
        adminCmdTabs.add(new FragmentTabDescription<AdminCommandsFragment>
                (AdminCommandsFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.command_shell;
            }
        });
        tabsForDDWRT.putAll(11, adminCmdTabs);

        //12- Admin > NVRAM
        final ArrayList<FragmentTabDescription<? extends AbstractBaseFragment>> adminNvramTabs = new ArrayList<>();
        adminNvramTabs.add(new FragmentTabDescription<AdminNVRAMFragment>
                (AdminNVRAMFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.admin_area_nvram;
            }
        });
        tabsForDDWRT.putAll(12, adminNvramTabs);

        //14- Toolbox > Network
        //FIXME Add "netstat" also (auto-refreshable)
//                tabsToSort[3] = AbstractBaseFragment.newInstance(parentFragment, ToolboxSubnetCalculatorFragment.class, parentSectionTitle,
//                        resources.getString(R.string.toolbox_subnet_calculator), router);
        final ArrayList<FragmentTabDescription<? extends AbstractBaseFragment>> toolboxNetworkTabs = new ArrayList<>();
        toolboxNetworkTabs.add(new FragmentTabDescription<ToolboxPingFragment>
                (ToolboxPingFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.toolbox_ping;
            }
        });
        toolboxNetworkTabs.add(new FragmentTabDescription<ToolboxTracerouteFragment>
                (ToolboxTracerouteFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.toolbox_traceroute;
            }
        });
        toolboxNetworkTabs.add(new FragmentTabDescription<ToolboxNsLookupFragment>
                (ToolboxNsLookupFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.toolbox_nslookup;
            }
        });
        toolboxNetworkTabs.add(new FragmentTabDescription<ToolboxArpingFragment>
                (ToolboxArpingFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.toolbox_arping;
            }
        });
        toolboxNetworkTabs.add(new FragmentTabDescription<ToolboxWhoisFragment>
                (ToolboxWhoisFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.toolbox_whois;
            }
        });
        toolboxNetworkTabs.add(new FragmentTabDescription<ToolboxMACOUILookupFragment>
                (ToolboxMACOUILookupFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.toolbox_oui_lookup;
            }
        });
        tabsForDDWRT.putAll(14, toolboxNetworkTabs);
        return tabsForDDWRT;
    }

    @NonNull
    private static ArrayListMultimap<Integer, FragmentTabDescription<? extends AbstractBaseFragment>> getTabsForOpenWRT() {
        final ArrayListMultimap<Integer, FragmentTabDescription<? extends AbstractBaseFragment>> tabsForOpenWRT = ArrayListMultimap.create();
        //1- Overview //TODO Add something specific to OpenWrt
        final ArrayList<FragmentTabDescription<? extends AbstractBaseFragment>> overviewTabs = new ArrayList<>();
        overviewTabs.add(new FragmentTabDescription<OverviewNetworkTopologyMapFragment>
                (OverviewNetworkTopologyMapFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.overview_ntm;
            }
        });
        tabsForOpenWRT.putAll(1, overviewTabs);

        //2- Status: {Status, Wireless, Clients, Monitoring}
        final ArrayList<FragmentTabDescription<? extends AbstractBaseFragment>> statusTabs = new ArrayList<>();
        statusTabs.add(new FragmentTabDescription<StatusRouterFragmentOpenWrt>
                (StatusRouterFragmentOpenWrt.class) {
            @Override
            public int getTitleRes() {
                return R.string.status_router;
            }
        });
//        statusTabs.add(new FragmentTabDescription<StatusTimeFragment>
//                (StatusTimeFragment.class) {
//            @Override
//            public int getTitleRes() {
//                return R.string.status_time;
//            }
//        });
        statusTabs.add(new FragmentTabDescription<StatusWANFragmentOpenWrt>
                (StatusWANFragmentOpenWrt.class) {
            @Override
            public int getTitleRes() {
                return R.string.status_wan;
            }
        });
//        statusTabs.add(new FragmentTabDescription<StatusLANFragment>
//                (StatusLANFragment.class) {
//            @Override
//            public int getTitleRes() {
//                return R.string.status_lan;
//            }
//        });
//        statusTabs.add(new FragmentTabDescription<StatusSyslogFragment>
//                (StatusSyslogFragment.class) {
//            @Override
//            public int getTitleRes() {
//                return R.string.status_syslog;
//            }
//        });
//        tabsForOpenWRT.putAll(2, statusTabs);

        //3- Status > Wireless //TODO
//        final ArrayList<FragmentTabDescription> wirelessTabs = new ArrayList<>();
//        wirelessTabs.add(new FragmentTabDescription<StatusWirelessFragment>
//                (StatusWirelessFragment.class) {
//            @Override
//            public int getTitleRes() {
//                return R.string.status_wireless;
//            }
//        });
//        tabsForOpenWRT.putAll(3, wirelessTabs);

        //4- Status > Clients //TODO
//        final ArrayList<FragmentTabDescription> clientsTabs = new ArrayList<>();
//        clientsTabs.add(new FragmentTabDescription<StatusClientsFragment>
//                (StatusClientsFragment.class) {
//            @Override
//            public int getTitleRes() {
//                return R.string.status_clients;
//            }
//        });
//        tabsForOpenWRT.putAll(4, clientsTabs);

        //5- Status > Monitoring TODO
//        final ArrayList<FragmentTabDescription> monitoringTabs = new ArrayList<>();
//        monitoringTabs.add(new FragmentTabDescription<StatusMonitoringWANFragment>
//                (StatusMonitoringWANFragment.class) {
//            @Override
//            public int getTitleRes() {
//                return R.string.status_wan;
//            }
//        });
//        monitoringTabs.add(new FragmentTabDescription<StatusBandwidthFragment>
//                (StatusBandwidthFragment.class) {
//            @Override
//            public int getTitleRes() {
//                return R.string.status_bandwidth;
//            }
//        });
//        tabsForOpenWRT.putAll(5, monitoringTabs);

        //7- Services > OpenVPN TODO
//        final ArrayList<FragmentTabDescription> servicesOpenVpnTabs = new ArrayList<>();
//        servicesOpenVpnTabs.add(new FragmentTabDescription<ServicesOpenVPNClientFragment>
//                (ServicesOpenVPNClientFragment.class) {
//            @Override
//            public int getTitleRes() {
//                return R.string.services_openvpn_client;
//            }
//        });
//        servicesOpenVpnTabs.add(new FragmentTabDescription<ServicesOpenVPNLogsFragment>
//                (ServicesOpenVPNLogsFragment.class) {
//            @Override
//            public int getTitleRes() {
//                return R.string.services_openvpn_logs;
//            }
//        });
//        tabsForOpenWRT.putAll(7, servicesOpenVpnTabs);

        //8- Services > OpenVPN TODO
//        final ArrayList<FragmentTabDescription> servicesWolTabs = new ArrayList<>();
//        servicesWolTabs.add(new FragmentTabDescription<ServicesWakeOnLanFragment>
//                (ServicesWakeOnLanFragment.class) {
//            @Override
//            public int getTitleRes() {
//                return R.string.services_wol;
//            }
//        });
//        servicesWolTabs.add(new FragmentTabDescription<ServicesWakeOnLanDaemonFragment>
//                (ServicesWakeOnLanDaemonFragment.class) {
//            @Override
//            public int getTitleRes() {
//                return R.string.services_wol_daemon;
//            }
//        });
//        tabsForOpenWRT.putAll(8, servicesWolTabs);

        //10- Admin > Commands TODO
//        final ArrayList<FragmentTabDescription> adminCmdTabs = new ArrayList<>();
//        adminCmdTabs.add(new FragmentTabDescription<AdminCommandsFragment>
//                (AdminCommandsFragment.class) {
//            @Override
//            public int getTitleRes() {
//                return R.string.command_shell;
//            }
//        });
//        tabsForOpenWRT.putAll(10, adminCmdTabs);

        //11- Admin > NVRAM TODO
//        final ArrayList<FragmentTabDescription> adminNvramTabs = new ArrayList<>();
//        adminNvramTabs.add(new FragmentTabDescription<AdminNVRAMFragment>
//                (AdminNVRAMFragment.class) {
//            @Override
//            public int getTitleRes() {
//                return R.string.admin_area_nvram;
//            }
//        });
//        tabsForOpenWRT.putAll(11, adminNvramTabs);

        //13- Toolbox > Network
        //FIXME Add "netstat" also (auto-refreshable)
//                tabsToSort[3] = AbstractBaseFragment.newInstance(parentFragment, ToolboxSubnetCalculatorFragment.class, parentSectionTitle,
//                        resources.getString(R.string.toolbox_subnet_calculator), router);
        final ArrayList<FragmentTabDescription<? extends AbstractBaseFragment>> toolboxNetworkTabs = new ArrayList<>();
        toolboxNetworkTabs.add(new FragmentTabDescription<ToolboxPingFragment>
                (ToolboxPingFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.toolbox_ping;
            }
        });
        toolboxNetworkTabs.add(new FragmentTabDescription<ToolboxTracerouteFragment>
                (ToolboxTracerouteFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.toolbox_traceroute;
            }
        });
        toolboxNetworkTabs.add(new FragmentTabDescription<ToolboxNsLookupFragment>
                (ToolboxNsLookupFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.toolbox_nslookup;
            }
        });
        toolboxNetworkTabs.add(new FragmentTabDescription<ToolboxArpingFragment>
                (ToolboxArpingFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.toolbox_arping;
            }
        });
        toolboxNetworkTabs.add(new FragmentTabDescription<ToolboxWhoisFragment>
                (ToolboxWhoisFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.toolbox_whois;
            }
        });
        toolboxNetworkTabs.add(new FragmentTabDescription<ToolboxMACOUILookupFragment>
                (ToolboxMACOUILookupFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.toolbox_oui_lookup;
            }
        });
        tabsForOpenWRT.putAll(13, toolboxNetworkTabs);
        return tabsForOpenWRT;
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

            this.fragmentTiles = new ArrayList<>();
            if (tiles != null) {
                final int size = tiles.size();
                if (size >= 2) {
                    final int randomMin;
                    if (size >= 3) {
                        this.fragmentTiles.add(new AvocarrotNativeAdTile(this, savedInstanceState, this.router));
                        randomMin = 3;
                    } else {
                        randomMin = 1;
                    }
                    this.fragmentTiles.addAll(tiles);
                    //insert banner ad randomly
                    this.fragmentTiles.add(
                            Math.max(randomMin, new Random().nextInt(size)),
                            new AvocarrotNativeAdTile(this, savedInstanceState, this.router));
                } else {
                    if (size == 1 && tiles.get(0) != null && !tiles.get(0).isEmbeddedWithinScrollView()) {
                        //Add banner add first, then all other tiles (issue with AdminNVRAMTile)
                        this.fragmentTiles.add(new BannerAdTile(this, savedInstanceState, this.router));
                    } else {
                        //Add banner add first, then all other tiles
                        this.fragmentTiles.add(new AvocarrotNativeAdTile(this, savedInstanceState, this.router));
                    }

                    this.fragmentTiles.addAll(tiles);
                }
            } else {
                this.fragmentTiles.add(new AvocarrotNativeAdTile(this, savedInstanceState, this.router));
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
