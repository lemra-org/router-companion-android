package org.rm3l.ddwrt.tiles.overview;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;

import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.main.DDWRTMainActivity;
import org.rm3l.ddwrt.resources.PublicIPInfo;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.service.tasks.PublicIPChangesServiceTask;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.ddwrt.mgmt.RouterManagementActivity.ROUTER_SELECTED;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.NOK;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.UNKNOWN;
import static org.rm3l.ddwrt.utils.Utils.isDemoRouter;

public class NetworkTopologyMapTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = NetworkTopologyMapTile.class.getSimpleName();
    public static final String INTERNET_CONNECTIVITY_PUBLIC_IP = "INTERNET_CONNECTIVITY_PUBLIC_IP";
    private final View.OnClickListener routerStateClickListener;
    private final View.OnClickListener clientsOnClickListener;

    private boolean isThemeLight;
    private final AtomicInteger nbActiveClients = new AtomicInteger(-1);
    private final AtomicInteger nbDhcpLeases = new AtomicInteger(-1);

    private Router mRouterCopy;
    private String mTempRouterUuid;

    private boolean checkActualInternetConnectivity = true;

    private long mLastSync;

    public NetworkTopologyMapTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_network_map_overview,
                R.id.tile_network_map_togglebutton);
        isThemeLight = ColorUtils.isThemeLight(mParentFragmentActivity);

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

                    if (mParentFragmentPreferences != null) {
                        checkActualInternetConnectivity = mParentFragmentPreferences
                                .getBoolean(DDWRTCompanionConstants.OVERVIEW_NTM_CHECK_ACTUAL_INTERNET_CONNECTIVITY_PREF, true);
                    }

                    Log.d(LOG_TAG, "Init background loader for " + NetworkTopologyMapTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (mRefreshing.getAndSet(true)) {
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    if (!isForceRefresh()) {
                        //Force Manual Refresh
                        if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                            //Skip run
                            Log.d(LOG_TAG, "Skip loader run");
                            return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                        }
                    }
                    nbRunsLoader++;

                    mLastSync = System.currentTimeMillis();

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
                                    .setProperty(NVRAMInfo.LAN_IPADDR, "255.255.255.255")
                                    .setProperty(NVRAMInfo.OPENVPNCL_ENABLE,
                                            Integer.toString(new Random().nextInt(2)))
                                    .setProperty(NVRAMInfo.OPENVPNCL_REMOTEIP, "my.remote.vpn.servi.ce")
                                    .setProperty(NVRAMInfo.OPENVPNCL_REMOTEPORT, "1234");
                        } else {
                            nvramInfoTmp =
                                    SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity, mRouter,
                                            mGlobalPreferences,
                                            NVRAMInfo.ROUTER_NAME,
                                            NVRAMInfo.WAN_IPADDR,
                                            NVRAMInfo.LAN_IPADDR,
                                            NVRAMInfo.OPENVPNCL_ENABLE,
                                            NVRAMInfo.OPENVPNCL_REMOTEIP,
                                            NVRAMInfo.OPENVPNCL_REMOTEPORT);
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

                        if (checkActualInternetConnectivity) {
                            try {

                                if (isDemoRouter(mRouter)) {
                                    final long nbRunsLoaderModulo = (nbRunsLoader % 5);
                                    if (nbRunsLoaderModulo == 0) {
                                        //nbRunsLoader = 5k
                                        nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP,
                                                "52.64." +
                                                        (1 + new Random().nextInt(252))
                                                        + "." +
                                                        (1 + new Random().nextInt(252)));
                                    } else if (nbRunsLoaderModulo == 1) {
                                        //nbRunsLoader = 5k + 1
                                        nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, NOK);
                                    } else if (nbRunsLoaderModulo == 2) {
                                        //nbRunsLoader = 5k + 2
                                        nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, UNKNOWN);
                                    }
                                } else {
                                    //Check actual connections to the outside from the router
                                    final CharSequence applicationName = Utils.getApplicationName(mParentFragmentActivity);
                                    final String[] wanPublicIpCmdStatus = SSHUtils.getManualProperty(mParentFragmentActivity,
                                            mRouterCopy, mGlobalPreferences,
//                                        "echo -e \"GET / HTTP/1.1\\r\\nHost:icanhazip.com\\r\\nUser-Agent:DD-WRT Companion/3.3.0\\r\\n\" | nc icanhazip.com 80"
                                            String.format("echo -e \"" +
                                                            "GET / HTTP/1.1\\r\\n" +
                                                            "Host:%s\\r\\n" +
                                                            "User-Agent:%s/%s\\r\\n\" " +
                                                            "| /usr/bin/nc %s %d",
                                                    PublicIPInfo.ICANHAZIP_HOST,
                                                    applicationName != null ? applicationName : BuildConfig.APPLICATION_ID,
                                                    BuildConfig.VERSION_NAME,
                                                    PublicIPInfo.ICANHAZIP_HOST,
                                                    PublicIPInfo.ICANHAZIP_PORT));
                                    Log.d(LOG_TAG, "wanPublicIpCmdStatus: " + Arrays.toString(wanPublicIpCmdStatus));
                                    if (wanPublicIpCmdStatus == null || wanPublicIpCmdStatus.length == 0) {
                                        nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, NOK);
                                    } else {
                                        final String wanPublicIp = wanPublicIpCmdStatus[wanPublicIpCmdStatus.length - 1]
                                                .trim();
                                        if (Patterns.IP_ADDRESS.matcher(wanPublicIp).matches()) {
                                            nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, wanPublicIp);

                                            PublicIPChangesServiceTask.buildNotificationIfNeeded(mParentFragmentActivity,
                                                    mRouterCopy, mParentFragmentPreferences,
                                                    wanPublicIpCmdStatus,
                                                    nvramInfo.getProperty(NVRAMInfo.WAN_IPADDR));

                                        } else {
                                            nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, NOK);
                                        }
                                    }
                                }
                            } catch (final Exception e) {
                                e.printStackTrace();
                                nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, UNKNOWN);
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

//            final LinearLayout mapContainerView = (LinearLayout) layout.findViewById(R.id.tile_network_map_container);

            final TextView devicesCountTextView
                    = (TextView) layout.findViewById(R.id.tile_network_map_wan_lan_textView);

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
                    routerNameToSet = "-";
                }
                routerNameView.setTypeface(null,
                        isNullOrEmpty(routerNameToSet) ? Typeface.ITALIC : Typeface.NORMAL);
                routerNameView.setText(isNullOrEmpty(routerNameToSet) ? "(empty)" : routerNameToSet);

                //WAN IP
                final TextView wanIpView = (TextView) this.layout.findViewById(R.id.tile_network_map_router_wan_ip);
                wanIpView.setText("WAN IP: " + data.getProperty(NVRAMInfo.WAN_IPADDR, "-"));

                //LAN IP
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

                devicesCountTextView.setText(nbActiveClientsInt < 0 ? "-" : Integer.toString(nbActiveClientsInt));

                ((TextView) layout.findViewById(R.id.tile_network_map_wan_lan_textView_devices))
                        .setText("Device" + (nbActiveClientsInt > 1 ? "s" : ""));

                final boolean openvpnClEnabled = "1".equals(data.getProperty(NVRAMInfo.OPENVPNCL_ENABLE));
                final String vpnClRemoteServerIp = data.getProperty(NVRAMInfo.OPENVPNCL_REMOTEIP);
                final String vpnClRemoteServerPort = data.getProperty(NVRAMInfo.OPENVPNCL_REMOTEPORT);
                final View vpnClImageView = layout.findViewById(R.id.tile_network_map_wan_vpn);
                if (openvpnClEnabled) {
                    vpnClImageView.setVisibility(View.VISIBLE);
                            vpnClImageView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Utils.displayMessage(mParentFragmentActivity,
                                            isNullOrEmpty(vpnClRemoteServerIp) ?
                                                    "OpenVPN Client connected." :
                                                    String.format("Secure VPN Connection established with %s%s",
                                                            vpnClRemoteServerIp,
                                                            isNullOrEmpty(vpnClRemoteServerPort) ?
                                                                    "" : (", on port " + vpnClRemoteServerPort)),
                                            Style.INFO);
                                }
                            });
                } else {
                    vpnClImageView.setVisibility(View.INVISIBLE);
                    vpnClImageView.setOnClickListener(null);
                }
//
//                layout.findViewById(R.id.tile_network_map_wan)
//                        .setOnClickListener(routerStateClickListener);

                layout.findViewById(R.id.tile_network_map_router)
                        .setOnClickListener(routerStateClickListener);

                devicesCountTextView.setOnClickListener(clientsOnClickListener);

                final TextView publicIpView = (TextView) layout.findViewById(R.id.tile_network_map_wan_public_ip);
                final View statusWarningView = layout.findViewById(R.id.tile_network_map_wan_status_warning);
                final View statusUnknownView = layout.findViewById(R.id.tile_network_map_wan_status_unknown);
                final View wanPathHorizontalView = layout.findViewById(R.id.tile_network_map_wan_path_horizontal);
                final Resources resources = mParentFragmentActivity.getResources();

                final int wanPathColor;
                if (checkActualInternetConnectivity) {
                    final String publicIp = data.getProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, null);
                    final String statusToastMsg;

                    if (publicIp == null || UNKNOWN.equals(publicIp)) {
                        wanPathColor = R.color.line_view_color;
                        publicIpView
                                .setVisibility(View.GONE);
                        statusWarningView
                                .setVisibility(View.GONE);
                        statusUnknownView
                                .setVisibility(View.VISIBLE);
                        statusToastMsg = "Couldn't test connectivity to the Internet!";

                    } else if (NOK.equals(publicIp)) {
                        wanPathColor = R.color.win8_orange;
                        publicIpView
                                .setVisibility(View.GONE);
                        statusWarningView
                                .setVisibility(View.VISIBLE);
                        statusUnknownView
                                .setVisibility(View.GONE);
                        statusToastMsg = ("Your router seems not to be able to reach the Internet. " +
                                "No public IP Address was found on the Internet.");

                    } else {
                        //Valid IP Address
                        wanPathColor = R.color.android_green;
                        final String publicIpViewText = ("Public IP:\n" + publicIp);
                        final Spannable publicIpViewTextSpannable = new SpannableString(publicIpViewText);
                        publicIpViewTextSpannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                                "Public IP:\n".length(), publicIpViewText.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        publicIpView.setText(publicIpViewTextSpannable);
                        publicIpView
                                .setVisibility(View.VISIBLE);
                        statusWarningView
                                .setVisibility(View.GONE);
                        statusUnknownView
                                .setVisibility(View.GONE);
                        statusToastMsg = ("Your router seems to be able to reach the Internet. " +
                                "Public IP Address on the Internet is: " + publicIp);

                        if (Objects.equal(publicIp, data.getProperty(NVRAMInfo.WAN_IPADDR))) {
                            wanIpView.setVisibility(View.INVISIBLE);
                        }
                    }

                    final View.OnClickListener statusViewOnClickListener = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Utils.displayMessage(mParentFragmentActivity,
                                    statusToastMsg,
                                    new Style.Builder()
                                            .setBackgroundColorValue(wanPathColor)
                                            .build());
                        }
                    };
                    publicIpView.setOnClickListener(statusViewOnClickListener);
                    statusWarningView.setOnClickListener(statusViewOnClickListener);
                    statusUnknownView.setOnClickListener(statusViewOnClickListener);
                    wanPathHorizontalView.setOnClickListener(statusViewOnClickListener);

                } else {
                    publicIpView
                            .setVisibility(View.GONE);
                    statusWarningView
                            .setVisibility(View.GONE);
                    statusUnknownView
                            .setVisibility(View.GONE);
                    wanPathColor = R.color.line_view_color;

                    wanPathHorizontalView.setOnClickListener(null);
                }

                publicIpView
                        .setTextColor(resources.getColor(wanPathColor));

                final int[] wanPathElements = new int[]{
                        R.id.tile_network_map_wan_path_vertical,
                        R.id.tile_network_map_wan_path_horizontal,
                        R.id.tile_network_map_router_wan_path_vertical
                };
                for (final int wanPathElement : wanPathElements) {
                    final View wanPathElementView = layout.findViewById(wanPathElement);
                    if (wanPathElementView == null) {
                        continue;
                    }
                    wanPathElementView.setBackgroundColor(resources.getColor(wanPathColor));
                }

                //Update last sync
                final RelativeTimeTextView lastSyncView = (RelativeTimeTextView) layout.findViewById(R.id.tile_last_sync);
                lastSyncView.setReferenceTime(mLastSync);
                lastSyncView.setPrefix("Last sync: ");

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
                //Hide NTM
//                mapContainerView.setVisibility(View.GONE);
            }
//            else {
//                mapContainerView.setVisibility(View.VISIBLE);
//            }

        } finally {
            Log.d(LOG_TAG, "onLoadFinished(): done loading!");
            mRefreshing.set(false);
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

}
