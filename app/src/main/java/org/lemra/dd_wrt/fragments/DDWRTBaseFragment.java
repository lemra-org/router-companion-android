package org.lemra.dd_wrt.fragments;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.common.collect.Lists;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lemra.dd_wrt.DDWRTMainActivity;
import org.lemra.dd_wrt.R;
import org.lemra.dd_wrt.api.conn.Router;
import org.lemra.dd_wrt.fragments.access.AccessWANAccessFragment;
import org.lemra.dd_wrt.fragments.admin.AdminBackupFragment;
import org.lemra.dd_wrt.fragments.admin.AdminCommandsFragment;
import org.lemra.dd_wrt.fragments.admin.AdminFactoryDefaultsFragment;
import org.lemra.dd_wrt.fragments.admin.AdminKeepAliveFragment;
import org.lemra.dd_wrt.fragments.admin.AdminManagementFragment;
import org.lemra.dd_wrt.fragments.admin.AdminUpgradeFragment;
import org.lemra.dd_wrt.fragments.admin.AdminWOLFragment;
import org.lemra.dd_wrt.fragments.nat_qos.NATQoSDMZFragment;
import org.lemra.dd_wrt.fragments.nat_qos.NATQoSPortForwardingFragment;
import org.lemra.dd_wrt.fragments.nat_qos.NATQoSPortRangeForwardingFragment;
import org.lemra.dd_wrt.fragments.nat_qos.NATQoSPortTriggeringFragment;
import org.lemra.dd_wrt.fragments.nat_qos.NATQoSQoSFragment;
import org.lemra.dd_wrt.fragments.nat_qos.NATQoSUPnPFragment;
import org.lemra.dd_wrt.fragments.security.SecurityFirewallFragment;
import org.lemra.dd_wrt.fragments.security.SecurityVPNPassthroughFragment;
import org.lemra.dd_wrt.fragments.services.ServicesAdBlockingFragment;
import org.lemra.dd_wrt.fragments.services.ServicesFreeRadiusFragment;
import org.lemra.dd_wrt.fragments.services.ServicesHotSpotFragment;
import org.lemra.dd_wrt.fragments.services.ServicesNASFragment;
import org.lemra.dd_wrt.fragments.services.ServicesPPoEFragment;
import org.lemra.dd_wrt.fragments.services.ServicesSIPFragment;
import org.lemra.dd_wrt.fragments.services.ServicesServicesFragment;
import org.lemra.dd_wrt.fragments.services.ServicesUSBFragment;
import org.lemra.dd_wrt.fragments.services.ServicesVPNFragment;
import org.lemra.dd_wrt.fragments.services.ServicesWebServerFragment;
import org.lemra.dd_wrt.fragments.setup.SetupBasicFragment;
import org.lemra.dd_wrt.fragments.setup.SetupDDNSFragment;
import org.lemra.dd_wrt.fragments.setup.SetupEoIPFragment;
import org.lemra.dd_wrt.fragments.setup.SetupIPv6Fragment;
import org.lemra.dd_wrt.fragments.setup.SetupMACCloningFragment;
import org.lemra.dd_wrt.fragments.setup.SetupNetworkingFragment;
import org.lemra.dd_wrt.fragments.setup.SetupRoutingFragment;
import org.lemra.dd_wrt.fragments.setup.SetupVLANFragment;
import org.lemra.dd_wrt.fragments.status.StatusBandwidthFragment;
import org.lemra.dd_wrt.fragments.status.StatusLANFragment;
import org.lemra.dd_wrt.fragments.status.StatusRouterFragment;
import org.lemra.dd_wrt.fragments.status.StatusSysinfoFragment;
import org.lemra.dd_wrt.fragments.status.StatusSyslogFragment;
import org.lemra.dd_wrt.fragments.status.StatusWANFragment;
import org.lemra.dd_wrt.fragments.status.StatusWirelessFragment;
import org.lemra.dd_wrt.fragments.wireless.WirelessBasicFragment;
import org.lemra.dd_wrt.fragments.wireless.WirelessMACFilteringFragment;
import org.lemra.dd_wrt.fragments.wireless.WirelessRadiusFragment;
import org.lemra.dd_wrt.fragments.wireless.WirelessSecurityFragment;
import org.lemra.dd_wrt.prefs.sort.DDWRTSortingStrategy;
import org.lemra.dd_wrt.prefs.sort.SortingStrategy;
import org.lemra.dd_wrt.tiles.DDWRTTile;

import java.util.ArrayList;
import java.util.List;

import static android.widget.FrameLayout.LayoutParams;

/**
 * Created by armel on 8/10/14.
 */
public abstract class DDWRTBaseFragment extends SherlockFragment {

    public static final String TAB_TITLE = "fragment_tab_title";
    public static final String FRAGMENT_CLASS = "fragment_class";
    public static final String ROUTER_CONNECTION_INFO = "router_info";
    public static final String PARENT_SECTION_TITLE = "parent_section_title";

    private static final String LOG_TAG = DDWRTBaseFragment.class.getSimpleName();
    @Nullable
    protected Router router;
    private CharSequence mTabTitle;
    private CharSequence mParentSectionTitle;
    private List<DDWRTTile> fragmentTiles;

    @NotNull
    private DDWRTMainActivity ddwrtMainActivity;

//    WeakReference<View> mView;

    @Nullable
    public static DDWRTBaseFragment newInstance(@NotNull final Class<? extends DDWRTBaseFragment> clazz,
                                                @NotNull final CharSequence parentSectionTitle, @NotNull final CharSequence tabTitle,
                                                @Nullable final Router router) {
        try {
            final DDWRTBaseFragment fragment = clazz.newInstance()
                    .setTabTitle(tabTitle)
                    .setParentSectionTitle(parentSectionTitle);

            Bundle args = new Bundle();
            args.putCharSequence(TAB_TITLE, tabTitle);
            args.putCharSequence(PARENT_SECTION_TITLE, parentSectionTitle);
            args.putString(FRAGMENT_CLASS, clazz.getCanonicalName());
            args.putSerializable(ROUTER_CONNECTION_INFO, router);
            fragment.setArguments(args);

            fragment.router = router;

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
                                                   @Nullable final Router router) {
        Log.d(LOG_TAG, "getFragments(" + parentSectionNumber + ", " + sortingStrategy + ")");

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
            Log.d(LOG_TAG, "An error occurred - using DDWRTSortingStrategy default strategy: " + exception);
            sortingStrategyInstance = new DDWRTSortingStrategy();
        }

        String parentSectionTitle;

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
                tabsToSort[0] = DDWRTBaseFragment.newInstance(SetupBasicFragment.class, parentSectionTitle,
                        resources.getString(R.string.setup_basic), router);
                tabsToSort[1] = DDWRTBaseFragment.newInstance(SetupIPv6Fragment.class, parentSectionTitle,
                        resources.getString(R.string.setup_ipv6), router);
                tabsToSort[2] = DDWRTBaseFragment.newInstance(SetupDDNSFragment.class, parentSectionTitle,
                        resources.getString(R.string.setup_ddns), router);
                tabsToSort[3] = DDWRTBaseFragment.newInstance(SetupMACCloningFragment.class, parentSectionTitle,
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


    public final DDWRTBaseFragment setParentSectionTitle(@NotNull final CharSequence parentSectionTitle) {
        this.mParentSectionTitle = parentSectionTitle;
        return this;
    }

    public final CharSequence getTabTitle() {
        return mTabTitle;
    }

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
    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final Bundle arguments = getArguments();
        final CharSequence parentSectionTitle = arguments.getCharSequence(PARENT_SECTION_TITLE);
        String tabTitle = arguments.getString(TAB_TITLE);

        Log.d(LOG_TAG, "onCreateView: " + parentSectionTitle + " > " + tabTitle);

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
        if (this.fragmentTiles != null && !this.fragmentTiles.isEmpty()) {
            final LoaderManager loaderManager = getLoaderManager();
            int i = 0;
            for (final DDWRTTile ddwrtTile : this.fragmentTiles) {
                loaderManager.initLoader(i++, null, ddwrtTile);
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
        if (this.fragmentTiles != null && !this.fragmentTiles.isEmpty()) {
            final LoaderManager loaderManager = getLoaderManager();
            for (int i = 0; i < this.fragmentTiles.size(); i++) {
                loaderManager.destroyLoader(i);
            }
        }
    }

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param id   The ID whose loader is to be created.
     * @param args Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
//    @Override
//    public Loader<String> onCreateLoader(int id, Bundle args) {
//        final AsyncTaskLoader<String> loader = new AsyncTaskLoader<String>(getActivity()) {
//
//            @Override
//            public String loadInBackground() {
//                long i = 0l;
//                try {
//                    // TODO SSH Operation will run over here
//                    String msg;
//                    while(i < new Random().nextInt(10) + 10) {
//                        msg = "[Run #" + (++i) + "] I am " +
//                                DDWRTBaseFragment.this.getArguments().getString(FRAGMENT_CLASS) + " and I am doing time-consuming operations in the background\n";
//                        Log.d(LOG_TAG, msg);
//                        Thread.sleep(7000l);
//                    }
//                } catch (InterruptedException e) {
//                }
//
//                //FIXME Dummy texts
//                if (i % 5 == 0) {
//                    return "Spaces are the lieutenant commanders of the dead adventure.";
//                }
//
//                if (i % 5 == 1) {
//                    return "Est grandis nix, cesaris.";
//                }
//
//                if (i % 5 == 2) {
//                    return "Lotus, visitors, and separate lamas will always protect them.";
//                }
//
//                if (i % 5 == 3) {
//                    return "For a sliced quartered paste, add some bourbon and baking powder.";
//                }
//
//                if (i % 5 == 4) {
//                    return "The reef vandalizes with hunger, rob the galley before it rises.";
//                }
//
//                return null;
//            }
//        };
//        // somehow the AsyncTaskLoader doesn't want to start its job without
//        // calling this method
//        loader.forceLoad();
//        return loader;
//    }

//    @Override
//    public void onLoadFinished(Loader<String> loader, String data) {
//
//        //Use data over here
//        //
//        final View ddwrtSectionView = getDDWRTSectionView(getActivity(), getArguments());
//        if (ddwrtSectionView instanceof TextView) {
//            ((TextView) ddwrtSectionView).setText(isNullOrEmpty(data) ?
//                    getResources().getText(R.string.no_data) : data);
//        }
/*
// add the new item and let the adapter know in order to refresh the
		// views
		mItems.add(TabsFragment.TAB_WORDS.equals(mTag) ? WORDS[mPosition]
				: NUMBERS[mPosition]);
		mAdapter.notifyDataSetChanged();

		// advance in your list with one step
		mPosition++;
		if (mPosition < mTotal - 1) {
			getLoaderManager().restartLoader(0, null, this);
			Log.d(TAG, "onLoadFinished(): loading next...");
		} else {
			Log.d(TAG, "onLoadFinished(): done loading!");
		}
 */
//    }

    /**
     * Called when a previously created loader is being reset, and thus
     * making its data unavailable.  The application should at this point
     * remove any references it has to the Loader's data.
     *
//     * @param loader The Loader that is being reset.
     */
//    @Override
//    public void onLoaderReset(Loader<String> loader) {
//        final View ddwrtSectionView = getDDWRTSectionView(getActivity(), getArguments());
//        if (ddwrtSectionView instanceof TextView) {
//            ((TextView) ddwrtSectionView).setText(getResources().getText(R.string.no_data));
//        }
//    }

    @NotNull
    private ViewGroup getLayout() {
        final LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        final int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources()
                .getDisplayMetrics());
        params.setMargins(margin, margin, margin, margin);

        ViewGroup viewGroup = null;

        boolean atLeastOneTileAdded = false;

        if (this.fragmentTiles != null && !this.fragmentTiles.isEmpty()) {

            final LayoutInflater layoutInflater = getSherlockActivity().getLayoutInflater();
            viewGroup = (ScrollView) layoutInflater.inflate(R.layout.base_tiles_container_scrollview, null);

            final List<TableRow> rows = new ArrayList<TableRow>();

            final TableRow.LayoutParams tableRowParams = new TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
            for (final DDWRTTile ddwrtTile : this.fragmentTiles) {
                final ViewGroup viewGroupLayout = ddwrtTile.getViewGroupLayout();
                atLeastOneTileAdded |= (viewGroupLayout != null);

                if (viewGroupLayout == null) {
                    continue;
                }

                final TableRow tableRow = new TableRow(getSherlockActivity());
                tableRow.setOnClickListener(ddwrtTile);
                viewGroupLayout.setOnClickListener(ddwrtTile);
                tableRow.setLayoutParams(tableRowParams);
                tableRow.addView(viewGroupLayout);

                rows.add(tableRow);
            }

            atLeastOneTileAdded = (!rows.isEmpty());

            Log.d(LOG_TAG, "atLeastOneTileAdded: "+atLeastOneTileAdded+", rows: "+rows.size());

            if (atLeastOneTileAdded) {
                //Drop Everything
//                viewGroup.removeAllViews();

//                final TableLayout tableLayout = new TableLayout(getSherlockActivity());

                final TableLayout tableLayout = (TableLayout) viewGroup.findViewById(R.id.tiles_container_scrollview_table);

//                tableLayout.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.MATCH_PARENT));
//                tableLayout.setStretchAllColumns(true);
                tableLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));

                for (final TableRow row : rows) {
                    tableLayout.addView(row);
                }

//                viewGroup.addView(tableLayout);
            }

            ((ScrollView) viewGroup).setFillViewport(true);
        }

        if (viewGroup == null || !atLeastOneTileAdded) {
            viewGroup = new FrameLayout(getSherlockActivity());
            final TextView view = new TextView(getSherlockActivity());
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

    public final void forceRefreshTiles() {
        final List<DDWRTTile> tiles = this.getTiles(null);
        if (tiles == null) {
            return;
        }

        final List<DDWRTTile> tilesSubmitted = Lists.newArrayList();

        for (final DDWRTTile tile : tiles) {
            tile.forceRefresh();
            tilesSubmitted.add(tile);
        }

        for (final DDWRTTile tileSubmitted : tilesSubmitted) {
            while (!tileSubmitted.isDoneLoading()) {
                Log.d(LOG_TAG, "Waiting for tile " + tileSubmitted + " to finish loading...");
                try {
                    Thread.sleep(5000l);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    @Nullable
    protected abstract List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState);

}
