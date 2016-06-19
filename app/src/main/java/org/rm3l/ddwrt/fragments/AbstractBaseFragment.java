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

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.fragments.access.AccessRestrictionsWANAccessFragment;
import org.rm3l.ddwrt.fragments.admin.AdminCommandsFragment;
import org.rm3l.ddwrt.fragments.admin.AdminNVRAMFragment;
import org.rm3l.ddwrt.fragments.dashboard.DashboardBandwidthFragment;
import org.rm3l.ddwrt.fragments.dashboard.DashboardNetworkFragment;
import org.rm3l.ddwrt.fragments.dashboard.DashboardSystemFragment;
import org.rm3l.ddwrt.fragments.services.ServicesOpenVPNClientFragment;
import org.rm3l.ddwrt.fragments.services.ServicesOpenVPNLogsFragment;
import org.rm3l.ddwrt.fragments.services.ServicesOpenVPNServerFragment;
import org.rm3l.ddwrt.fragments.services.ServicesPPTPClientFragment;
import org.rm3l.ddwrt.fragments.services.ServicesPPTPServerFragment;
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
import org.rm3l.ddwrt.utils.ReportingUtils;
import org.rm3l.ddwrt.utils.Utils;
import org.rm3l.ddwrt.widgets.RecyclerViewEmptySupport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Abstract base fragment
 *
 * @author <a href="mailto:apps+ddwrt@rm3l.org">Armel S.</a>
 */
public abstract class AbstractBaseFragment<T> extends Fragment
        implements LoaderManager.LoaderCallbacks<T>, SwipeRefreshLayout.OnRefreshListener {

    public static final String TAB_TITLE = "fragment_tab_title";
    public static final String FRAGMENT_CLASS = "fragment_class";
    public static final String ROUTER_CONNECTION_INFO = "router_info";
    public static final String PARENT_SECTION_TITLE = "parent_section_title";
    public static final String STATE_LOADER_IDS = "loaderIds";
    private static final String LOG_TAG = AbstractBaseFragment.class.getSimpleName();
    public static final Random RANDOM = new Random();
    private static AbstractBaseFragment mNoDataFragment;
    protected final Handler mHandler = new Handler();
    private Map<Integer, Object> mLoaderIdsInUse;
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
//    @NonNull
//    private PageSlidingTabStripFragment parentFragment;

    private ViewGroup mRootViewGroup;

    @Nullable
    protected SwipeRefreshLayout mSwipeRefreshLayout;

    public static final int RootViewType_RECYCLER_VIEW = 1;
    public static final int RootViewType_LINEAR_LAYOUT = 2;

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
    public static AbstractBaseFragment newInstance(Activity activity, @NonNull final Class<? extends AbstractBaseFragment> clazz,
                                                   @NonNull final CharSequence parentSectionTitle, @NonNull final CharSequence tabTitle,
                                                   @Nullable final String router) {
        try {
            final AbstractBaseFragment fragment = clazz.newInstance()
                    .setTabTitle(tabTitle)
                    .setParentSectionTitle(parentSectionTitle);
            fragment.mClazz = clazz;
//            fragment.parentFragment = parentFragment;

//            final ViewPager.OnPageChangeListener parentFragmentOnPageChangeListener = parentFragment.getOnPageChangeListener();
            if (activity instanceof DDWRTMainActivity) {
                fragment.ddwrtMainActivity = (DDWRTMainActivity) activity;
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
            ReportingUtils.reportException(null, ie);
        } catch (IllegalAccessException iae) {
            ReportingUtils.reportException(null, iae);
            iae.printStackTrace();
        }
        return null;
    }

    @NonNull
    public static AbstractBaseFragment[] getFragments(@NonNull Activity activity, @NonNull final Resources resources, int parentSectionNumber,
                                                      String sortingStrategy,
                                                      @Nullable final String router) {
        Crashlytics.log(Log.DEBUG,  LOG_TAG, "getFragments(" + parentSectionNumber + ", " + sortingStrategy + ")");

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
            Crashlytics.log(Log.WARN, LOG_TAG, "An error occurred - using DDWRTSortingStrategy default strategy: " + exception);
            sortingStrategyInstance = new DDWRTSortingStrategy();
        }

        final AbstractBaseFragment[] tabsToSort;

        RouterFirmware routerFirmwareForFragments;
        final Router routerFromDao = RouterManagementActivity
                .getDao(activity).getRouter(router);
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

        //FIXME Once full support of other firmwares is implemented
        if (routerFirmwareForFragments == null ||
                RouterFirmware.UNKNOWN.equals(routerFirmwareForFragments)) {
            routerFirmwareForFragments = RouterFirmware.DDWRT;
        }
        //FIXME End

        if (mNoDataFragment == null) {
            mNoDataFragment = AbstractBaseFragment.newInstance(
                    activity,
                    NoDataFragment.class,
                    (resources.getString(R.string.unknown) + " (" + parentSectionNumber + ")"),
                    resources.getString(R.string.unknown), router);
        }

        final ArrayListMultimap<Integer, FragmentTabDescription<? extends AbstractBaseFragment>> tabDescriptionMultimap =
                allTabs.get(routerFirmwareForFragments);
        if (tabDescriptionMultimap == null) {
            //Unknown
            ReportingUtils.reportException(
                    null, new IllegalArgumentException("Router Firmware unknown or not supported"));
            tabsToSort = new AbstractBaseFragment[0];
        } else {
            final List<FragmentTabDescription<? extends AbstractBaseFragment>> fragmentTabDescriptions =
                    tabDescriptionMultimap.get(parentSectionNumber);
            if (fragmentTabDescriptions == null || fragmentTabDescriptions.isEmpty()) {
                ReportingUtils.reportException(
                        null, new IllegalArgumentException("Not implemented yet: " + parentSectionNumber));
                //This should NOT happen => Error
                tabsToSort = new AbstractBaseFragment[1];
                tabsToSort[0] = mNoDataFragment;

            } else {
                tabsToSort = new AbstractBaseFragment[fragmentTabDescriptions.size()];
                int i = 0;
                for (final FragmentTabDescription<? extends AbstractBaseFragment> fragmentTabDescription : fragmentTabDescriptions) {
                    tabsToSort[i++] =  AbstractBaseFragment.newInstance(
                            activity,
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
        //1- Dashboard: {Network, Bandwidth, System}
        final ArrayList<FragmentTabDescription<? extends AbstractBaseFragment>> dashboardTabs
                = new ArrayList<>();
        dashboardTabs.add(new FragmentTabDescription<DashboardNetworkFragment>
                (DashboardNetworkFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.dashboard_network;
            }
        });
        dashboardTabs.add(new FragmentTabDescription<DashboardBandwidthFragment>
                (DashboardBandwidthFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.dashboard_bandwidth;
            }
        });
        dashboardTabs.add(new FragmentTabDescription<DashboardSystemFragment>
                (DashboardSystemFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.dashboard_system;
            }
        });
        tabsForDDWRT.putAll(1, dashboardTabs);

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
        servicesOpenVpnTabs.add(new FragmentTabDescription<ServicesOpenVPNServerFragment>
                (ServicesOpenVPNServerFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.services_openvpn_server;
            }
        });
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
        servicesPPTPTabs.add(new FragmentTabDescription<ServicesPPTPServerFragment>
                (ServicesPPTPServerFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.services_pptp_server;
            }
        });
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

        //11- Admin > Access Restrictions
        final ArrayList<FragmentTabDescription<? extends AbstractBaseFragment>> adminAccessRestrictionsTabs = new ArrayList<>();
        adminAccessRestrictionsTabs.add(new FragmentTabDescription<AccessRestrictionsWANAccessFragment>
                (AccessRestrictionsWANAccessFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.access_restrictions;
            }
        });
        tabsForDDWRT.putAll(11, adminAccessRestrictionsTabs);

        //12- Admin > Commands
        final ArrayList<FragmentTabDescription<? extends AbstractBaseFragment>> adminCmdTabs = new ArrayList<>();
        adminCmdTabs.add(new FragmentTabDescription<AdminCommandsFragment>
                (AdminCommandsFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.command_shell;
            }
        });
        tabsForDDWRT.putAll(12, adminCmdTabs);

        //13- Admin > NVRAM
        final ArrayList<FragmentTabDescription<? extends AbstractBaseFragment>> adminNvramTabs = new ArrayList<>();
        adminNvramTabs.add(new FragmentTabDescription<AdminNVRAMFragment>
                (AdminNVRAMFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.admin_area_nvram;
            }
        });
        tabsForDDWRT.putAll(13, adminNvramTabs);

        //15- Toolbox > Network
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
        tabsForDDWRT.putAll(15, toolboxNetworkTabs);
        return tabsForDDWRT;
    }

    @NonNull
    private static ArrayListMultimap<Integer, FragmentTabDescription<? extends AbstractBaseFragment>> getTabsForOpenWRT() {
        final ArrayListMultimap<Integer, FragmentTabDescription<? extends AbstractBaseFragment>> tabsForOpenWRT = ArrayListMultimap.create();
        //1- Dashboard: {Network, Bandwidth, System}
        final ArrayList<FragmentTabDescription<? extends AbstractBaseFragment>> dashboardTabs
                = new ArrayList<>();
        dashboardTabs.add(new FragmentTabDescription<DashboardNetworkFragment>
                (DashboardNetworkFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.dashboard_network;
            }
        });
        dashboardTabs.add(new FragmentTabDescription<DashboardBandwidthFragment>
                (DashboardBandwidthFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.dashboard_bandwidth;
            }
        });
        dashboardTabs.add(new FragmentTabDescription<DashboardSystemFragment>
                (DashboardSystemFragment.class) {
            @Override
            public int getTitleRes() {
                return R.string.dashboard_system;
            }
        });
        tabsForOpenWRT.putAll(1, dashboardTabs);

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

        this.mLoaderIdsInUse = Maps.newConcurrentMap();

        this.router = RouterManagementActivity.getDao(this.getActivity()).getRouter(getArguments().getString(ROUTER_CONNECTION_INFO));
        Crashlytics.log(Log.DEBUG, LOG_TAG, "onCreate() loaderIdsInUse: " + mLoaderIdsInUse);
//        if (savedInstanceState != null) {
//            final ArrayList<Integer> loaderIdsSaved = savedInstanceState.getIntegerArrayList(STATE_LOADER_IDS);
//            Crashlytics.log(Log.DEBUG,  LOG_TAG, "onCreate() loaderIdsSaved: " + loaderIdsSaved);
//            if (loaderIdsSaved != null) {
//                //Destroy existing IDs, if any, as new loaders will get created in onResume()
//                final LoaderManager loaderManager = getLoaderManager();
//                for (final Integer loaderId : loaderIdsSaved) {
//                    if (loaderId == null) {
//                        continue;
//                    }
//                    loaderManager.destroyLoader(loaderId);
//                }
//            }
//        }

        final List<DDWRTTile> tiles = this.getTiles(savedInstanceState);
        if (BuildConfig.WITH_ADS) {

            this.fragmentTiles = new ArrayList<>();
            if (tiles != null) {
                final int size = tiles.size();
                if (size >= 2) {
                    final int randomMin;
                    if (size >= 3) {
//                        this.fragmentTiles.add(new AvocarrotNativeAdTile(this, savedInstanceState, this.router));
                        randomMin = 2;
                    } else {
                        randomMin = 1;
                    }
                    this.fragmentTiles.addAll(tiles);
                    //insert banner ad randomly
                    if (RANDOM.nextBoolean()) {
                        this.fragmentTiles.add(
                                Math.max(randomMin, new Random().nextInt(size)),
                                new AvocarrotNativeAdTile(this, savedInstanceState, this.router));
                    }
                } else {
                    if (RANDOM.nextBoolean()) {
                        if (size == 1 && tiles.get(0) != null && !tiles.get(0).isEmbeddedWithinScrollView()) {
                            //Add banner add first, then all other tiles (issue with AdminNVRAMTile)
                            this.fragmentTiles.add(new BannerAdTile(this, savedInstanceState, this.router));
                        } else {
                            //Add banner add first, then all other tiles
                            this.fragmentTiles.add(new AvocarrotNativeAdTile(this, savedInstanceState, this.router));
                        }
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

    protected boolean canChildScrollUp() {
        return (mRootViewGroup != null && ViewCompat.canScrollVertically(mRootViewGroup, -1));
    }

    protected boolean isSwipeRefreshLayoutEnabled() {
        return true;
    }

    @NonNull
    protected int getRootViewType() {
        return RootViewType_RECYCLER_VIEW;
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

        final FragmentActivity activity = getActivity();

        final View rootView =
                inflater
                        .inflate(R.layout.base_tiles_container_recyclerview,
                                null);

        final int rootViewType = getRootViewType();
        final RecyclerViewEmptySupport recyclerView = (RecyclerViewEmptySupport) rootView
                .findViewById(R.id.tiles_container_recyclerview);
        final LinearLayout linearLayout = (LinearLayout) rootView
                .findViewById(R.id.tiles_container_linearlayout);
        final View recyclerViewEmptyView = rootView.findViewById(R.id.empty_view);
        switch (rootViewType) {
            case RootViewType_LINEAR_LAYOUT:
                mRootViewGroup = linearLayout;
                recyclerView.setVisibility(View.GONE);
                if (recyclerViewEmptyView != null) {
                    recyclerViewEmptyView.setVisibility(View.GONE);
                }
                linearLayout.setVisibility(View.VISIBLE);
                linearLayout.removeAllViews();
                if (fragmentTiles != null) {
                    final boolean isThemeLight = ColorUtils.isThemeLight(activity);

                    for (final DDWRTTile ddwrtTile : fragmentTiles) {
                        if (ddwrtTile == null) {
                            continue;
                        }
                        final ViewGroup viewGroupLayout = ddwrtTile.getViewGroupLayout();
                        if (viewGroupLayout == null) {
                            continue;
                        }

                        final FrameLayout.LayoutParams cardViewLayoutParams = new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT);
                        cardViewLayoutParams.rightMargin = R.dimen.cardview_margin_right;
                        cardViewLayoutParams.leftMargin = R.dimen.cardview_margin_left;
                        cardViewLayoutParams.topMargin = R.dimen.cardview_margin_top;
                        cardViewLayoutParams.bottomMargin = R.dimen.cardview_margin_bottom;

                        final CardView cardView = new CardView(activity);
                        cardView.setLayoutParams(cardViewLayoutParams);
                        cardView.setFocusable(true);
                        cardView.setClickable(true);
                        cardView.setContentPadding(
                                R.dimen.cardview_contentPadding,
                                R.dimen.cardview_contentPadding,
                                R.dimen.cardview_contentPadding,
                                R.dimen.cardview_contentPadding);

                        //Add padding to CardView on v20 and before to prevent intersections between the Card content and rounded corners.
                        cardView.setPreventCornerOverlap(true);
                        //Add padding in API v21+ as well to have the same measurements with previous versions.
                        cardView.setUseCompatPadding(true);

                        //Highlight CardView
//                cardView.setCardElevation(10f);

                        cardView.setCardBackgroundColor(
                                ContextCompat.getColor(activity,
                                        isThemeLight ?
                                                R.color.cardview_light_background :
                                                R.color.cardview_dark_background));


                        final TextView titleTextView = (TextView) viewGroupLayout.findViewById(ddwrtTile.getTileTitleViewId());
                        if (isThemeLight) {
                            if (titleTextView != null) {
                                titleTextView.setTextColor(ContextCompat.getColor(activity,
                                        android.R.color.holo_blue_dark));
                            }
                        }
                        viewGroupLayout.setBackgroundColor(ContextCompat
                                .getColor(activity, android.R.color.transparent));

                        cardView.addView(viewGroupLayout);

                        linearLayout.addView(cardView);
                    }
                }
                break;
            case RootViewType_RECYCLER_VIEW:
                mRootViewGroup = recyclerView;
            default:
                linearLayout.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);

                // use this setting to improve performance if you know that changes
                // in content do not change the layout size of the RecyclerView
                // allows for optimizations if all items are of the same size:
                recyclerView.setHasFixedSize(true);

                // use a linear layout manager
                final RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(activity);
                mLayoutManager.scrollToPosition(0);
                recyclerView.setLayoutManager(mLayoutManager);

                if (recyclerViewEmptyView instanceof TextView) {
                    final TextView emptyView = (TextView) recyclerViewEmptyView;
                    if (ColorUtils.isThemeLight(activity)) {
                        emptyView.setTextColor(ContextCompat.getColor(activity, R.color.black));
                    } else {
                        emptyView.setTextColor(ContextCompat.getColor(activity, R.color.white));
                    }
                }
                recyclerView.setEmptyView(recyclerViewEmptyView);

                final RecyclerView.Adapter mAdapter =
                        new AbstractBaseFragmentRecyclerViewAdapter(activity, router, fragmentTiles);

                recyclerView.setAdapter(mAdapter);

                break;
        }

        mSwipeRefreshLayout = new SwipeRefreshLayout(activity) {
            @Override
            public boolean canChildScrollUp() {
                return AbstractBaseFragment.this.canChildScrollUp();
            }
        };

        mSwipeRefreshLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
//        getActivity().getLayoutInflater()
//                .inflate(R.layout.swipe_refresh, null);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
//        mSwipeRefreshLayout.addView(this.getLayout(inflater, container, savedInstanceState));
        mSwipeRefreshLayout.addView(rootView);

        mSwipeRefreshLayout.setEnabled(isSwipeRefreshLayoutEnabled());

        initLoaders();

        return mSwipeRefreshLayout;
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
        if (!mLoaderIdsInUse.isEmpty()) {
            stopLoaders();
        }
        // initiate the loaders to do the background work
        final LoaderManager loaderManager = getLoaderManager();

        loaderManager.initLoader(0, null, this);
        this.setLoaderStopped(false);
        mLoaderIdsInUse.put(0, this);

        if (this.fragmentTiles != null) {
            for (final DDWRTTile ddwrtTile : fragmentTiles) {
                if (ddwrtTile == null) {
                    continue;
                }
                final int nextLoaderId = Long.valueOf(Utils.getNextLoaderId()).intValue();
                loaderManager.initLoader(nextLoaderId, null, ddwrtTile);
                ddwrtTile.setLoaderStopped(false);
                mLoaderIdsInUse.put(nextLoaderId, ddwrtTile);
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
        for (final Map.Entry<Integer, Object> loaderIdInUse : mLoaderIdsInUse.entrySet()) {
            loaderManager.destroyLoader(loaderIdInUse.getKey());
            final Object value = loaderIdInUse.getValue();
            if (value == this) {
                //Mark this as stopped
                this.setLoaderStopped(true);
            } else if (value instanceof DDWRTTile) {
                ((DDWRTTile) value).setLoaderStopped(true);
            }
        }
        mLoaderIdsInUse.clear();
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        //save the loader ids on file
        outState.putIntegerArrayList(STATE_LOADER_IDS,
                Lists.newArrayList(mLoaderIdsInUse.keySet()));

        super.onSaveInstanceState(outState);
    }

//    @Override
//    public void onResume() {
//        initLoaders();
//        super.onResume();
//    }
//
//    @Override
//    public void onPause() {
//        stopLoaders();
//        super.onPause();
//    }

    @Override
    public void onDestroy() {
        stopLoaders();
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.removeAllViews();
        }
        super.onDestroyView();
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
        if (ddwrtMainActivity == null) {
            Utils.reportException(getContext(), new IllegalStateException("ddwrtMainActivity is NULL"));
            Toast.makeText(getContext(), "Internal Error - please try again later.", Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        ddwrtMainActivity.startActivityForResult(intent, listener);
    }

    @Override
    public void onRefresh() {

        final Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("View", this.getClass().getSimpleName());
        ReportingUtils.reportEvent(ReportingUtils.EVENT_MANUAL_REFRESH, eventMap);

        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setEnabled(false);
            mSwipeRefreshLayout.setRefreshing(true);
        }

        final int totalNbTiles = (this.fragmentTiles != null ?
                Collections2.filter(this.fragmentTiles, new Predicate<DDWRTTile>() {
                    @Override
                    public boolean apply(@Nullable DDWRTTile input) {
                        return (input != null && !input.isAdTile());
                    }
                }).size() : 0);

        stopLoaders();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                //Set forceRefresh flag for all tiles composing this fragment
                if (AbstractBaseFragment.this.fragmentTiles != null && totalNbTiles > 0) {
                    final AtomicInteger nbRefreshes = new AtomicInteger(0);
                    final DDWRTTile.DDWRTTileRefreshListener refreshListener = new DDWRTTile.DDWRTTileRefreshListener() {
                        @Override
                        public void onTileRefreshed(@NonNull final DDWRTTile tile) {
                            try {
                                final int currentNbOfRefreshes = nbRefreshes.incrementAndGet();
                                if (currentNbOfRefreshes >= totalNbTiles) {
                                    if (mSwipeRefreshLayout != null) {
                                        mSwipeRefreshLayout.setEnabled(true);
                                        mSwipeRefreshLayout.setRefreshing(false);
                                    }
                                }
                            } finally {
                                tile.setForceRefresh(false);
                                tile.setRefreshListener(null);
                            }
                        }
                    };

                    for (final DDWRTTile fragmentTile : fragmentTiles) {
                        if (fragmentTile == null || fragmentTile.isAdTile()) {
                            continue;
                        }
                        fragmentTile.setForceRefresh(true);
                        fragmentTile.setRefreshListener(refreshListener);
                    }

                    initLoaders();

                } else {
                    if (mSwipeRefreshLayout != null) {
                        mSwipeRefreshLayout.setEnabled(true);
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                }

            }
        }, 1500l);

    }

}