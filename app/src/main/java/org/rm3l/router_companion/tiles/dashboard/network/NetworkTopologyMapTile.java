package org.rm3l.router_companion.tiles.dashboard.network;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;

import org.rm3l.router_companion.BuildConfig;
import org.rm3l.router_companion.R;
import org.rm3l.router_companion.actions.activity.SpeedTestActivity;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.firmwares.RemoteDataRetrievalListener;
import org.rm3l.router_companion.firmwares.RouterFirmwareConnectorManager;
import org.rm3l.router_companion.main.DDWRTMainActivity;
import org.rm3l.router_companion.resources.PublicIPInfo;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.service.tasks.PublicIPChangesServiceTask;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;

import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.router_companion.mgmt.RouterManagementActivity.ROUTER_SELECTED;
import static org.rm3l.router_companion.RouterCompanionAppConstants.NOK;
import static org.rm3l.router_companion.RouterCompanionAppConstants.UNKNOWN;
import static org.rm3l.router_companion.utils.Utils.isDemoRouter;

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

    private long mLastSync;

    public NetworkTopologyMapTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_network_map_overview,null);
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

        final FloatingActionButton speedtestFab =
                (FloatingActionButton) layout.findViewById(R.id.tile_network_map_speedtest);

        speedtestFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent speedTestIntent = new Intent(mParentFragmentActivity,
                        SpeedTestActivity.class);
                speedTestIntent.putExtra(ROUTER_SELECTED, mRouter.getUuid());
                mParentFragmentActivity.startActivity(speedTestIntent);
            }
        });

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

                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Init background loader for " + NetworkTopologyMapTile.class + ": routerInfo=" +
                            mRouter + " / nbRunsLoader=" + nbRunsLoader);

                    if (mRefreshing.getAndSet(true)) {
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    updateProgressBarViewSeparator(0);

                    mLastSync = System.currentTimeMillis();

                    nbActiveClients.set(-1);
                    nbDhcpLeases.set(-1);

                    //We are cloning the Router, with a new UUID, so as to have a different key into the SSH Sessions Cache
                    //This is because we are fetching in a quite real-time manner, and we don't want to block other async tasks.
                    mTempRouterUuid = UUID.randomUUID().toString();
                    mRouterCopy = new Router(mParentFragmentActivity, mRouter).setUuid(mTempRouterUuid);

                    updateProgressBarViewSeparator(20);

                    final NVRAMInfo nvramInfo = RouterFirmwareConnectorManager.getConnector(mRouterCopy)
                            .getDataForNetworkTopologyMapTile(mParentFragmentActivity, mRouter,
                                    new RemoteDataRetrievalListener() {
                                        @Override
                                        public void onProgressUpdate(int progress) {
                                            updateProgressBarViewSeparator(progress);
                                        }

                                        @Override
                                        public void doRegardlessOfStatus() {
                                            runBgServiceTaskAsync();
                                        }
                                    });
                    if (nvramInfo == null) {
                        throw new DDWRTNoDataException();
                    }

                    final Properties nvramInfoData = nvramInfo.getData();
                    if (nvramInfoData != null) {
                        //Active clients
                        if (nvramInfoData.containsKey("NB_ACTIVE_CLIENTS")) {
                            final String activeClientsProperty =
                                    (String) nvramInfoData.remove("NB_ACTIVE_CLIENTS");
                            if (!TextUtils.isEmpty(activeClientsProperty)) {
                                nbActiveClients.set(Integer.parseInt(activeClientsProperty));
                            }
                        }

                        //Active DHCP Leases
                        if (nvramInfoData.containsKey("NB_DHCP_LEASES")) {
                            final String dhcpLeasesProperty =
                                    (String) nvramInfoData.remove("NB_DHCP_LEASES");
                            if (!TextUtils.isEmpty(dhcpLeasesProperty)) {
                                nbDhcpLeases.set(Integer.parseInt(dhcpLeasesProperty));
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
            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

            final TextView wanInternetTextView = (TextView) layout.findViewById(R.id.tile_network_map_wan_internet_text);
            final Drawable drawable = ContextCompat.getDrawable(mParentFragmentActivity, R.drawable.internet);
//            drawable.setBounds(0, 0, (int)(drawable.getIntrinsicWidth() * 0.5),
//                    (int)(drawable.getIntrinsicHeight() * 0.5));
            //Scale to 70x70
            final Drawable d = new BitmapDrawable(mParentFragmentActivity.getResources(),
                    Bitmap.createScaledBitmap(((BitmapDrawable) drawable).getBitmap(), 170, 170, true));

            wanInternetTextView.setCompoundDrawablesWithIntrinsicBounds(d, null, null, null);

            layout.findViewById(R.id.tile_network_map_loading_view)
                    .setVisibility(View.GONE);
            layout.findViewById(R.id.tile_network_map_gridLayout)
                    .setVisibility(View.VISIBLE);

            if (data == null) {
                data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
            }

            final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_network_map_error);

            final Exception exception = data.getException();

            Router.doFetchAndSetRouterAvatarInImageView(
                    mParentFragmentActivity,
                    mRouter,
                    (ImageView)
                            layout.findViewById(R.id.tile_network_map_router_imageView));

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


                publicIpView
                        .setTextColor(ContextCompat.getColor(mParentFragmentActivity, wanPathColor));

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
                    wanPathElementView.setBackgroundColor(
                            ContextCompat.getColor(mParentFragmentActivity, wanPathColor));
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
                updateProgressBarWithError();
            } else if (exception == null){
                updateProgressBarWithSuccess();
            }

        } finally {
            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished(): done loading!");
            mRefreshing.set(false);
            try {
                //Destroy temporary SSH session
                if (mRouterCopy != null) {
                    SSHUtils.destroySessions(mRouterCopy);
                }
            } finally {
                doneWithLoaderInstance(this, loader);
            }
        }
    }

}
