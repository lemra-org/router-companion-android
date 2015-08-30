package org.rm3l.ddwrt.tiles.overview;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Throwables;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.main.DDWRTMainActivity;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.SSHUtils;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.rm3l.ddwrt.mgmt.RouterManagementActivity.ROUTER_SELECTED;
import static org.rm3l.ddwrt.utils.Utils.isDemoRouter;

public class NetworkTopologyMapTile extends DDWRTTile<NVRAMInfo> {
//        implements PopupMenu.OnMenuItemClickListener {

    private static final String LOG_TAG = NetworkTopologyMapTile.class.getSimpleName();
    private final View.OnClickListener routerStateClickListener;
    private final View.OnClickListener clientsOnClickListener;

    //    public static final String HIDE_INACTIVE_HOSTS = "hideInactiveHosts";
    private boolean isThemeLight;
    private final AtomicInteger nbActiveClients = new AtomicInteger(-1);
    private final AtomicInteger nbDhcpLeases = new AtomicInteger(-1);

    private Router mRouterCopy;
    private String mTempRouterUuid;

    public NetworkTopologyMapTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_network_map_overview,
                R.id.tile_network_map_togglebutton);
        isThemeLight = ColorUtils.isThemeLight(mParentFragmentActivity);

//        // Create Options Menu
//        final ImageButton tileMenu = (ImageButton) layout.findViewById(R.id.tile_network_map_menu);
//        if (!isThemeLight) {
//            //Set menu background to white
//            tileMenu.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
//        }
//        tileMenu.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                final PopupMenu popup = new PopupMenu(mParentFragmentActivity, v);
//                popup.setOnMenuItemClickListener(NetworkTopologyMapTile.this);
//                final MenuInflater inflater = popup.getMenuInflater();
//                final Menu menu = popup.getMenu();
//                inflater.inflate(R.menu.tile_overview_network_map_options, menu);
//
//                //Disable menu item from preference
//                if (mParentFragmentPreferences != null &&
//                        mParentFragmentPreferences.getBoolean(getFormattedPrefKey(HIDE_INACTIVE_HOSTS), false)) {
//                    //Mark as checked
//                    menu.findItem(R.id.tile_overview_network_map_hide_inactive_hosts).setChecked(true);
//                }
//
//                popup.show();
//            }
//        });

        routerStateClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Open Router State tab
                if (mParentFragmentActivity instanceof DDWRTMainActivity) {
                    ((DDWRTMainActivity) mParentFragmentActivity)
                            .selectItemInDrawer(2);
                } else {
                    //TODO Set proper flags ???
                    final Intent intent = new Intent(mParentFragmentActivity, DDWRTMainActivity.class);
                    intent.putExtra(ROUTER_SELECTED, mRouter.getUuid());
                    intent.putExtra(DDWRTMainActivity.SAVE_ITEM_SELECTED, 2);
                    mParentFragmentActivity.startActivity(intent);
                }
            }
        };

        clientsOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Open tab with wireless devices
                if (mParentFragmentActivity instanceof DDWRTMainActivity) {
                    ((DDWRTMainActivity) mParentFragmentActivity)
                            .selectItemInDrawer(4);
                } else {
                    //TODO Set proper flags ???
                    final Intent intent = new Intent(mParentFragmentActivity, DDWRTMainActivity.class);
                    intent.putExtra(ROUTER_SELECTED, mRouter.getUuid());
                    intent.putExtra(DDWRTMainActivity.SAVE_ITEM_SELECTED, 4);
                    mParentFragmentActivity.startActivity(intent);
                }
            }
        };
    }

    @Override
    public int getTileHeaderViewId() {
//        return R.id.tile_network_map_hdr;
        return -1;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_network_map_title;
    }

    @Nullable
    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {

                try {

                    isThemeLight = ColorUtils.isThemeLight(mParentFragmentActivity);

                    Log.d(LOG_TAG, "Init background loader for " + NetworkTopologyMapTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                        //Skip run
                        Log.d(LOG_TAG, "Skip loader run");
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    nbActiveClients.set(-1);
                    nbDhcpLeases.set(-1);

                    //We are cloning the Router, with a new UUID, so as to have a different key into the SSH Sessions Cache
                    //This is because we are fetching in a quite real-time manner, and we don't want to block other async tasks.
                    mTempRouterUuid = UUID.randomUUID().toString();
                    mRouterCopy = new Router(mRouter).setUuid(mTempRouterUuid);

                    final NVRAMInfo nvramInfo = new NVRAMInfo();

                    NVRAMInfo nvramInfoTmp = null;
                    try {
                        if (isDemoRouter(mRouter)) {
                            nvramInfoTmp = new NVRAMInfo()
                                    .setProperty(NVRAMInfo.ROUTER_NAME, "Demo Router (Test Data)")
                                    .setProperty(NVRAMInfo.WAN_IPADDR, "1.2.3.4")
                                    .setProperty(NVRAMInfo.LAN_IPADDR, "255.255.255.255");
                        } else {
                            nvramInfoTmp =
                                    SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity, mRouter,
                                            mGlobalPreferences,
                                            NVRAMInfo.ROUTER_NAME,
                                            NVRAMInfo.WAN_IPADDR,
                                            NVRAMInfo.LAN_IPADDR);
                        }
                    } finally {
                        if (nvramInfoTmp != null) {
                            nvramInfo.putAll(nvramInfoTmp);
                        }
                        //Active clients
                        if (isDemoRouter(mRouter)) {
                            nbActiveClients.set(new Random().nextInt(20));
                        } else {
                            final String[] activeClients = SSHUtils.getManualProperty(mParentFragmentActivity, mRouterCopy, mGlobalPreferences,
                                    "arp -a 2>/dev/null");
                            if (activeClients != null) {
                                nbActiveClients.set(activeClients.length);
                            }
                        }

                        //Active DHCP Leases
                        if (isDemoRouter(mRouter)) {
                            nbDhcpLeases.set(new Random().nextInt(30));
                        } else {
                            final String[] activeDhcpLeases = SSHUtils.getManualProperty(mParentFragmentActivity, mRouterCopy, mGlobalPreferences,
                                    "cat /tmp/dnsmasq.leases 2>/dev/null");
                            if (activeDhcpLeases != null) {
                                nbDhcpLeases.set(activeDhcpLeases.length);
                            }
                        }

                    }

                    return nvramInfo;
                } catch (@NonNull final Exception e) {
                    e.printStackTrace();
                    return new NVRAMInfo().setException(e);
                }

            }
        };
    }

    @Nullable
    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Nullable
    @Override
    protected OnClickIntent getOnclickIntent() {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<NVRAMInfo> loader, NVRAMInfo data) {

        try {
            //Set tiles
            Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

            layout.findViewById(R.id.tile_network_map_loading_view)
                    .setVisibility(View.GONE);
            layout.findViewById(R.id.tile_network_map_gridLayout)
                    .setVisibility(View.VISIBLE);

            if (data == null) {
                data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
            }

            final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_network_map_error);

            final Exception exception = data.getException();

            if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

                if (exception == null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }

                //Router Name
                final TextView routerNameView = (TextView) this.layout.findViewById(R.id.tile_network_map_router_name);
                final String routerName = data.getProperty(NVRAMInfo.ROUTER_NAME);
                final boolean routerNameNull = (routerName == null);
                String routerNameToSet = routerName;
                if (routerNameNull) {
                    routerNameToSet = "(empty)";
                }
                routerNameView.setTypeface(null, routerNameNull ? Typeface.ITALIC : Typeface.NORMAL);
                routerNameView.setText(routerNameToSet);

                //WAN IP
                final TextView wanIpView = (TextView) this.layout.findViewById(R.id.tile_network_map_router_wan_ip);
                wanIpView.setText("WAN IP: " + data.getProperty(NVRAMInfo.WAN_IPADDR, "-"));

                //WAN IP
                final TextView lanIpView = (TextView) this.layout.findViewById(R.id.tile_network_map_router_lan_ip);
                lanIpView.setText("LAN IP: " + data.getProperty(NVRAMInfo.LAN_IPADDR, "-"));

                final TextView activeClientsView =
                        (TextView) this.layout.findViewById(R.id.tile_network_map_active_clients);
                final int nbActiveClientsInt = nbActiveClients.intValue();
                activeClientsView.setText(nbActiveClientsInt < 0 ? "-" : Long.toString(nbActiveClientsInt));

                final TextView activeDhcpLeasesView =
                        (TextView) this.layout.findViewById(R.id.tile_network_map_active_dhcp_leases);
                final int nbActiveDhcpLeasesInt = nbDhcpLeases.intValue();
                activeDhcpLeasesView.setText(nbActiveDhcpLeasesInt < 0 ? "-" : Integer.toString(nbActiveDhcpLeasesInt));

                final TextView devicesCountTextView
                        = (TextView) layout.findViewById(R.id.tile_network_map_wan_lan_textView);
                devicesCountTextView.setText(nbActiveClientsInt < 0 ? "-" : Integer.toString(nbActiveClientsInt));

                ((TextView) layout.findViewById(R.id.tile_network_map_wan_lan_textView_devices))
                        .setText("device" + (nbActiveClientsInt > 1 ? "s" : ""));

                layout.findViewById(R.id.tile_network_map_wan_imageView)
                        .setOnClickListener(routerStateClickListener);

                layout.findViewById(R.id.tile_network_map_router)
                        .setOnClickListener(routerStateClickListener);

                devicesCountTextView.setOnClickListener(clientsOnClickListener);
            }

            if (exception != null && !(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {
                //noinspection ThrowableResultOfMethodCallIgnored
                final Throwable rootCause = Throwables.getRootCause(exception);
                errorPlaceHolderView.setText("Error: " + (rootCause != null ? rootCause.getMessage() : "null"));
                final Context parentContext = this.mParentFragmentActivity;
                errorPlaceHolderView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        //noinspection ThrowableResultOfMethodCallIgnored
                        if (rootCause != null) {
                            Toast.makeText(parentContext,
                                    rootCause.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
                errorPlaceHolderView.setVisibility(View.VISIBLE);
            }

//            final View tileMenu = layout.findViewById(R.id.tile_network_map_menu);
//            if ("-".equals(((TextView) layout.findViewById(R.id.tile_network_map_wan_lan_textView))
//                    .getText().toString())) {
//                tileMenu.setVisibility(View.GONE);
//            } else {
//                tileMenu.setVisibility(View.VISIBLE);
//            }
        } finally {
            Log.d(LOG_TAG, "onLoadFinished(): done loading!");

            try {
                //Destroy temporary SSH session
                if (mRouterCopy != null) {
                    SSHUtils.destroySession(mParentFragmentActivity, mRouterCopy);
                }
            } finally {
                doneWithLoaderInstance(this, loader,
                        R.id.tile_network_map_togglebutton_title,
                        R.id.tile_network_map_togglebutton_separator);
            }
        }
    }
//
//    @Override
//    public boolean onMenuItemClick(MenuItem item) {
//        final int itemId = item.getItemId();
//        switch (itemId) {
//            case R.id.tile_overview_network_map_hide_inactive_hosts: {
//                final boolean hideInactive = !item.isChecked();
//
//                mParentFragmentActivity.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        //Update with count of active clients
//                        int nbActiveClientsInt = nbActiveClients.intValue();
//                        final TextView devicesCountTextView
//                                = (TextView) layout.findViewById(R.id.tile_network_map_wan_lan_textView);
//                        devicesCountTextView.setText(nbActiveClientsInt < 0 ? "-" : Integer.toString(nbActiveClientsInt));
//
//                        ((TextView) layout.findViewById(R.id.tile_network_map_wan_lan_textView_devices))
//                                .setText("device" + (nbActiveClientsInt > 1 ? "s" : ""));
//                    }
//                });
//
//                //Save preference
//                if (mParentFragmentPreferences != null) {
//                    mParentFragmentPreferences.edit()
//                            .putBoolean(getFormattedPrefKey(HIDE_INACTIVE_HOSTS), hideInactive)
//                            .apply();
//                }
//                return true;
//            }
//            default:
//                break;
//        }
//        return false;
//    }
}
