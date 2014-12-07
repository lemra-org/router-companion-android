/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Armel S.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.rm3l.ddwrt.fragments;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.DDWRTMainActivity;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.fragments.access.AccessWANAccessFragment;
import org.rm3l.ddwrt.fragments.admin.AdminBackupFragment;
import org.rm3l.ddwrt.fragments.admin.AdminCommandsFragment;
import org.rm3l.ddwrt.fragments.admin.AdminFactoryDefaultsFragment;
import org.rm3l.ddwrt.fragments.admin.AdminKeepAliveFragment;
import org.rm3l.ddwrt.fragments.admin.AdminManagementFragment;
import org.rm3l.ddwrt.fragments.admin.AdminUpgradeFragment;
import org.rm3l.ddwrt.fragments.admin.AdminWOLFragment;
import org.rm3l.ddwrt.fragments.nat_qos.NATQoSDMZFragment;
import org.rm3l.ddwrt.fragments.nat_qos.NATQoSPortForwardingFragment;
import org.rm3l.ddwrt.fragments.nat_qos.NATQoSPortRangeForwardingFragment;
import org.rm3l.ddwrt.fragments.nat_qos.NATQoSPortTriggeringFragment;
import org.rm3l.ddwrt.fragments.nat_qos.NATQoSQoSFragment;
import org.rm3l.ddwrt.fragments.nat_qos.NATQoSUPnPFragment;
import org.rm3l.ddwrt.fragments.security.SecurityFirewallFragment;
import org.rm3l.ddwrt.fragments.security.SecurityVPNPassthroughFragment;
import org.rm3l.ddwrt.fragments.services.ServicesAdBlockingFragment;
import org.rm3l.ddwrt.fragments.services.ServicesFreeRadiusFragment;
import org.rm3l.ddwrt.fragments.services.ServicesHotSpotFragment;
import org.rm3l.ddwrt.fragments.services.ServicesNASFragment;
import org.rm3l.ddwrt.fragments.services.ServicesPPoEFragment;
import org.rm3l.ddwrt.fragments.services.ServicesSIPFragment;
import org.rm3l.ddwrt.fragments.services.ServicesServicesFragment;
import org.rm3l.ddwrt.fragments.services.ServicesUSBFragment;
import org.rm3l.ddwrt.fragments.services.ServicesVPNFragment;
import org.rm3l.ddwrt.fragments.services.ServicesWebServerFragment;
import org.rm3l.ddwrt.fragments.setup.SetupBasicFragment;
import org.rm3l.ddwrt.fragments.setup.SetupDDNSFragment;
import org.rm3l.ddwrt.fragments.setup.SetupEoIPFragment;
import org.rm3l.ddwrt.fragments.setup.SetupIPv6Fragment;
import org.rm3l.ddwrt.fragments.setup.SetupMacCloningFragment;
import org.rm3l.ddwrt.fragments.setup.SetupNetworkingFragment;
import org.rm3l.ddwrt.fragments.setup.SetupRoutingFragment;
import org.rm3l.ddwrt.fragments.setup.SetupVLANFragment;
import org.rm3l.ddwrt.fragments.status.StatusBandwidthFragment;
import org.rm3l.ddwrt.fragments.status.StatusLANFragment;
import org.rm3l.ddwrt.fragments.status.StatusRouterFragment;
import org.rm3l.ddwrt.fragments.status.StatusSysinfoFragment;
import org.rm3l.ddwrt.fragments.status.StatusSyslogFragment;
import org.rm3l.ddwrt.fragments.status.StatusWANFragment;
import org.rm3l.ddwrt.fragments.status.StatusWirelessFragment;
import org.rm3l.ddwrt.fragments.wireless.WirelessBasicFragment;
import org.rm3l.ddwrt.fragments.wireless.WirelessMACFilteringFragment;
import org.rm3l.ddwrt.fragments.wireless.WirelessRadiusFragment;
import org.rm3l.ddwrt.fragments.wireless.WirelessSecurityFragment;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.prefs.sort.DDWRTSortingStrategy;
import org.rm3l.ddwrt.prefs.sort.SortingStrategy;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static android.widget.FrameLayout.LayoutParams;

/**
 * Abstract base fragment
 *
 * @author <a href="mailto:apps+ddwrt@rm3l.org">Armel S.</a>
 */
public abstract class DDWRTBaseFragment<T> extends SherlockFragment implements LoaderManager.LoaderCallbacks<T> {

    public static final String TAB_TITLE = "fragment_tab_title";
    public static final String FRAGMENT_CLASS = "fragment_class";
    public static final String ROUTER_CONNECTION_INFO = "router_info";
    public static final String PARENT_SECTION_TITLE = "parent_section_title";

    private static final String LOG_TAG = DDWRTBaseFragment.class.getSimpleName();

    //    private static final AtomicInteger LOADERS_IDS = new AtomicInteger(1);
//    private final Map<Class<?>, Loader> loadersMap = Maps.newHashMap();
    private final Multimap<Class<?>, Loader> loaders = ArrayListMultimap.create();

    protected LinearLayout mLayout;
    protected Router router;
    private CharSequence mTabTitle;
    private CharSequence mParentSectionTitle;
    @Nullable
    private List<DDWRTTile> fragmentTiles;
    @NotNull
    private DDWRTMainActivity ddwrtMainActivity;
    @Nullable
    private Loader<T> mLoader;
    private Class<? extends DDWRTBaseFragment> mClazz;

    protected static final AtomicInteger LOADERS_IDS = new AtomicInteger(1);

    @Nullable
    public static DDWRTBaseFragment newInstance(@NotNull final Class<? extends DDWRTBaseFragment> clazz,
                                                @NotNull final CharSequence parentSectionTitle, @NotNull final CharSequence tabTitle,
                                                @Nullable final String router) {
        try {
            @NotNull final DDWRTBaseFragment fragment = clazz.newInstance()
                    .setTabTitle(tabTitle)
                    .setParentSectionTitle(parentSectionTitle);
            fragment.mClazz = clazz;

            @NotNull Bundle args = new Bundle();
            args.putCharSequence(TAB_TITLE, tabTitle);
            args.putCharSequence(PARENT_SECTION_TITLE, parentSectionTitle);
            args.putString(FRAGMENT_CLASS, clazz.getCanonicalName());
            args.putString(ROUTER_CONNECTION_INFO, router);
            fragment.setArguments(args);

            return fragment;

        } catch (java.lang.InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @NotNull
    public static DDWRTBaseFragment[] getFragments(@NotNull final Resources resources, int parentSectionNumber,
                                                   String sortingStrategy,
                                                   @Nullable final String router) {
        Log.d(LOG_TAG, "getFragments(" + parentSectionNumber + ", " + sortingStrategy + ")");

        final Class sortingStrategyClass;
        @Nullable SortingStrategy sortingStrategyInstance = null;
        @Nullable Exception exception = null;
        try {
            sortingStrategyClass = Class.forName(sortingStrategy);
            sortingStrategyInstance = (SortingStrategy) sortingStrategyClass.newInstance();
        } catch (@NotNull final Exception e) {
            e.printStackTrace();
            exception = e;
        }

        if (exception != null) {
            //Default one
            Log.d(LOG_TAG, "An error occurred - using DDWRTSortingStrategy default strategy: " + exception);
            sortingStrategyInstance = new DDWRTSortingStrategy();
        }

        String parentSectionTitle;

        @NotNull final DDWRTBaseFragment[] tabsToSort;
        switch (parentSectionNumber) {
            case 0:
                parentSectionTitle = resources.getString(R.string.status);
                //1 = Status => {Router, WAN, LAN, Wireless, Bandwidth, Syslog, Sysinfo}
                tabsToSort = new DDWRTBaseFragment[6];
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
//                tabsToSort[6] = DDWRTBaseFragment.newInstance(StatusSysinfoFragment.class, parentSectionTitle,
//                        resources.getString(R.string.status_sysinfo), router);
                break;
            case 1:
                parentSectionTitle = resources.getString(R.string.setup);
                //2 = Setup => {Basic, IPv6, DDNS, MAC Cloning, Routing, VLANs, Networking, EoIP}
                tabsToSort = new DDWRTBaseFragment[8];
                tabsToSort[0] = DDWRTBaseFragment.newInstance(SetupBasicFragment.class, parentSectionTitle,
                        resources.getString(R.string.setup_basic), router);
                tabsToSort[1] = DDWRTBaseFragment.newInstance(SetupIPv6Fragment.class, parentSectionTitle,
                        resources.getString(R.string.setup_ipv6), router);
                tabsToSort[2] = DDWRTBaseFragment.newInstance(SetupDDNSFragment.class, parentSectionTitle,
                        resources.getString(R.string.setup_ddns), router);
                tabsToSort[3] = DDWRTBaseFragment.newInstance(SetupMacCloningFragment.class, parentSectionTitle,
                        resources.getString(R.string.setup_mac_cloning), router);
                tabsToSort[4] = DDWRTBaseFragment.newInstance(SetupRoutingFragment.class, parentSectionTitle,
                        resources.getString(R.string.setup_routing), router);
                tabsToSort[5] = DDWRTBaseFragment.newInstance(SetupVLANFragment.class, parentSectionTitle,
                        resources.getString(R.string.setup_vlans), router);
                tabsToSort[6] = DDWRTBaseFragment.newInstance(SetupNetworkingFragment.class, parentSectionTitle,
                        resources.getString(R.string.setup_networking), router);
                tabsToSort[7] = DDWRTBaseFragment.newInstance(SetupEoIPFragment.class, parentSectionTitle,
                        resources.getString(R.string.setup_eoip), router);
                break;
            case 2:
                parentSectionTitle = resources.getString(R.string.wireless);
                //3 = Wireless => {Basic, Radius, Security, MAC Filter, WL0, WL1, ...}
                tabsToSort = new DDWRTBaseFragment[4];
                tabsToSort[0] = DDWRTBaseFragment.newInstance(WirelessBasicFragment.class, parentSectionTitle,
                        resources.getString(R.string.wireless_basic_settings), router);
                tabsToSort[1] = DDWRTBaseFragment.newInstance(WirelessRadiusFragment.class, parentSectionTitle,
                        resources.getString(R.string.wireless_radius), router);
                tabsToSort[2] = DDWRTBaseFragment.newInstance(WirelessSecurityFragment.class, parentSectionTitle,
                        resources.getString(R.string.wireless_security), router);
                tabsToSort[3] = DDWRTBaseFragment.newInstance(WirelessMACFilteringFragment.class, parentSectionTitle,
                        resources.getString(R.string.wireless_mac_filter), router);
                break;
            case 3:
                parentSectionTitle = resources.getString(R.string.services);
                //4 = Services => {Services, FreeRadius, PPoE, VPN, USB, NAS, HotSpot, SIP Proxy, Adblocking, Webserver}
                tabsToSort = new DDWRTBaseFragment[10];
                tabsToSort[0] = DDWRTBaseFragment.newInstance(ServicesServicesFragment.class, parentSectionTitle,
                        resources.getString(R.string.services_services), router);
                tabsToSort[1] = DDWRTBaseFragment.newInstance(ServicesFreeRadiusFragment.class, parentSectionTitle,
                        resources.getString(R.string.services_freeradius), router);
                tabsToSort[2] = DDWRTBaseFragment.newInstance(ServicesPPoEFragment.class, parentSectionTitle,
                        resources.getString(R.string.services_ppoe), router);
                tabsToSort[3] = DDWRTBaseFragment.newInstance(ServicesVPNFragment.class, parentSectionTitle,
                        resources.getString(R.string.services_vpn), router);
                tabsToSort[4] = DDWRTBaseFragment.newInstance(ServicesUSBFragment.class, parentSectionTitle,
                        resources.getString(R.string.services_usb), router);
                tabsToSort[5] = DDWRTBaseFragment.newInstance(ServicesNASFragment.class, parentSectionTitle,
                        resources.getString(R.string.services_nas), router);
                tabsToSort[6] = DDWRTBaseFragment.newInstance(ServicesHotSpotFragment.class, parentSectionTitle,
                        resources.getString(R.string.services_hostspot), router);
                tabsToSort[7] = DDWRTBaseFragment.newInstance(ServicesSIPFragment.class, parentSectionTitle,
                        resources.getString(R.string.services_sip), router);
                tabsToSort[8] = DDWRTBaseFragment.newInstance(ServicesAdBlockingFragment.class, parentSectionTitle,
                        resources.getString(R.string.services_adblocking), router);
                tabsToSort[9] = DDWRTBaseFragment.newInstance(ServicesWebServerFragment.class, parentSectionTitle,
                        resources.getString(R.string.services_webserver), router);
                break;
            case 4:
                //5 = Security => {Firewall, VPN Passthrough}
                parentSectionTitle = resources.getString(R.string.security);
                tabsToSort = new DDWRTBaseFragment[2];
                tabsToSort[0] = DDWRTBaseFragment.newInstance(SecurityFirewallFragment.class, parentSectionTitle,
                        resources.getString(R.string.security_firewall), router);
                tabsToSort[1] = DDWRTBaseFragment.newInstance(SecurityVPNPassthroughFragment.class, parentSectionTitle,
                        resources.getString(R.string.security_vpn_passthrough), router);
                break;
            case 5:
                //6 = Access => {WAN}
                parentSectionTitle = resources.getString(R.string.access_restrictions);
                tabsToSort = new DDWRTBaseFragment[1];
                tabsToSort[0] = DDWRTBaseFragment.newInstance(AccessWANAccessFragment.class, parentSectionTitle,
                        resources.getString(R.string.access_restrictions_wan), router);
                break;
            case 6:
                parentSectionTitle = resources.getString(R.string.nat_qos);
                //7 = NAT/QoS => {Port Fwding, Port Range Fwding, Port Triggerring, UPnP, DMZ, QoS}
                tabsToSort = new DDWRTBaseFragment[6];
                tabsToSort[0] = DDWRTBaseFragment.newInstance(NATQoSPortForwardingFragment.class, parentSectionTitle,
                        resources.getString(R.string.nat_qos_port_forwarding), router);
                tabsToSort[1] = DDWRTBaseFragment.newInstance(NATQoSPortRangeForwardingFragment.class, parentSectionTitle,
                        resources.getString(R.string.nat_qos_port_range_forwarding), router);
                tabsToSort[2] = DDWRTBaseFragment.newInstance(NATQoSPortTriggeringFragment.class, parentSectionTitle,
                        resources.getString(R.string.nat_qos_port_trigger), router);
                tabsToSort[3] = DDWRTBaseFragment.newInstance(NATQoSUPnPFragment.class, parentSectionTitle,
                        resources.getString(R.string.nat_qos_upnp), router);
                tabsToSort[4] = DDWRTBaseFragment.newInstance(NATQoSDMZFragment.class, parentSectionTitle,
                        resources.getString(R.string.nat_qos_dmz), router);
                tabsToSort[5] = DDWRTBaseFragment.newInstance(NATQoSQoSFragment.class, parentSectionTitle,
                        resources.getString(R.string.nat_qos_qos), router);
                break;
            case 7:
                parentSectionTitle = resources.getString(R.string.admin_area);
                //8 => Admin => {Management, Keep Alive, Commands, WOL, Factory, Upgrade, Backup}
                tabsToSort = new DDWRTBaseFragment[7];
                tabsToSort[0] = DDWRTBaseFragment.newInstance(AdminManagementFragment.class, parentSectionTitle,
                        resources.getString(R.string.admin_area_mgmt), router);
                tabsToSort[1] = DDWRTBaseFragment.newInstance(AdminKeepAliveFragment.class, parentSectionTitle,
                        resources.getString(R.string.admin_area_keep_alive), router);
                tabsToSort[2] = DDWRTBaseFragment.newInstance(AdminCommandsFragment.class, parentSectionTitle,
                        resources.getString(R.string.admin_area_cmds), router);
                tabsToSort[3] = DDWRTBaseFragment.newInstance(AdminWOLFragment.class, parentSectionTitle,
                        resources.getString(R.string.admin_area_wol), router);
                tabsToSort[4] = DDWRTBaseFragment.newInstance(AdminFactoryDefaultsFragment.class, parentSectionTitle,
                        resources.getString(R.string.admin_area_factory), router);
                tabsToSort[5] = DDWRTBaseFragment.newInstance(AdminUpgradeFragment.class, parentSectionTitle,
                        resources.getString(R.string.admin_area_upgrade), router);
                tabsToSort[6] = DDWRTBaseFragment.newInstance(AdminBackupFragment.class, parentSectionTitle,
                        resources.getString(R.string.admin_area_backup), router);
                break;
            default:
                //This should NOT happen => Error
                parentSectionTitle = (resources.getString(R.string.unknown) + " (" + parentSectionNumber + ")");
                tabsToSort = new DDWRTBaseFragment[1];
                tabsToSort[0] = DDWRTBaseFragment.newInstance(NoDataFragment.class, parentSectionTitle,
                        resources.getString(R.string.unknown), null);
                break;

        }

        return tabsToSort.length > 0 ? sortingStrategyInstance.sort(tabsToSort) : tabsToSort;

    }


    @NotNull
    public final DDWRTBaseFragment setParentSectionTitle(@NotNull final CharSequence parentSectionTitle) {
        this.mParentSectionTitle = parentSectionTitle;
        return this;
    }

    public final CharSequence getTabTitle() {
        return mTabTitle;
    }

    @NotNull
    public final DDWRTBaseFragment setTabTitle(@NotNull final CharSequence tabTitle) {
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.router = RouterManagementActivity.getDao(this.getActivity()).getRouter(getArguments().getString(ROUTER_CONNECTION_INFO));
        this.fragmentTiles = this.getTiles(savedInstanceState);
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
    @NotNull
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

        // initiate the loaders to do the background work
        loaders.put(this.mClazz, getLoaderManager().initLoader(LOADERS_IDS.getAndIncrement(), savedInstanceState, this));

        if (this.fragmentTiles != null && !this.fragmentTiles.isEmpty()) {
            final LoaderManager loaderManager = getLoaderManager();
            for (final DDWRTTile ddwrtTile : this.fragmentTiles) {
                if (ddwrtTile == null) {
                    continue;
                }
                loaders.put(this.mClazz,
                        loaderManager.initLoader(LOADERS_IDS.getAndIncrement(), savedInstanceState, ddwrtTile));
            }
        }
    }

    /**
     * Called when the view previously created by {@link #onCreateView} has
     * been detached from the fragment.  The next time the fragment needs
     * to be displayed, a new view will be created.  This is called
     * after {@link #onStop()} and before {@link #onDestroy()}.  It is called
     * <em>regardless</em> of whether {@link #onCreateView} returned a
     * non-null view.  Internally it is called after the view's state has
     * been saved but before it has been removed from its parent.
     */
    @Override
    public void onDestroyView() {

        super.onDestroyView();

        final LoaderManager loaderManager = this.getLoaderManager();

        for (final Loader loader : loaders.values()) {
            if (loader == null) {
                continue;
            }
            final int loaderId = loader.getId();
            Log.d(LOG_TAG, "destroy loader #" + loaderId);
            loaderManager.destroyLoader(loaderId);
        }
        loaders.clear();
    }

    @NotNull
    private ViewGroup getLayout() {
        @NotNull final LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        final int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources()
                .getDisplayMetrics());
        params.setMargins(margin, margin, margin, margin);

        @Nullable ViewGroup viewGroup = null;

        boolean atLeastOneTileAdded = false;

        if (this.fragmentTiles != null && !this.fragmentTiles.isEmpty()) {

            @NotNull final LayoutInflater layoutInflater = getSherlockActivity().getLayoutInflater();
            viewGroup = (ScrollView) layoutInflater.inflate(R.layout.base_tiles_container_scrollview, null);

            @NotNull final List<CardView> cards = new ArrayList<CardView>();

            final CardView.LayoutParams cardViewLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

            for (@NotNull final DDWRTTile ddwrtTile : this.fragmentTiles) {
                @Nullable final ViewGroup viewGroupLayout = ddwrtTile.getViewGroupLayout();
                atLeastOneTileAdded |= (viewGroupLayout != null);

                if (viewGroupLayout == null) {
                    continue;
                }

                //Detach this from Parent
                final ViewParent parent = viewGroupLayout.getParent();
                if (parent instanceof ViewGroup) {
                    ((ViewGroup) parent).removeView(viewGroupLayout);
                }

                final CardView cardView = new CardView(getSherlockActivity());
                cardView.setOnClickListener(ddwrtTile);
                cardView.setLayoutParams(cardViewLayoutParams);
                cardView.addView(viewGroupLayout);

                cards.add(cardView);
            }

            atLeastOneTileAdded = (!cards.isEmpty());

            Log.d(LOG_TAG, "atLeastOneTileAdded: " + atLeastOneTileAdded + ", rows: " + cards.size());

            if (atLeastOneTileAdded) {
                //Drop Everything
//                viewGroup.removeAllViews();

                mLayout = (LinearLayout) viewGroup.findViewById(R.id.tiles_container_scrollview_layout);

                for (@NotNull final CardView card : cards) {
//                    mTableLayout.removeView(row);
                    mLayout.addView(card);
                }

            }

            ((ScrollView) viewGroup).setFillViewport(true);
        }

        if (viewGroup == null || !atLeastOneTileAdded) {
            viewGroup = new FrameLayout(getSherlockActivity());
            @NotNull final TextView view = new TextView(getSherlockActivity());
            view.setGravity(Gravity.CENTER);
            view.setText(getResources().getString(R.string.no_data));
            view.setBackgroundResource(R.drawable.background_card);
            view.setLayoutParams(params);

            viewGroup.addView(view);
        }

        viewGroup.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        viewGroup.setLayoutParams(params);

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
        @Nullable final Loader<T> loader = this.getLoader(id, args);
        this.mLoader = loader;
        if (mLoader == null) {
            return null;
        }
        mLoader.forceLoad();
        return mLoader;
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

}
