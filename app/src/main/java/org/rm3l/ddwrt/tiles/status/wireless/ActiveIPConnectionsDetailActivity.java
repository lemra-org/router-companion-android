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
package org.rm3l.ddwrt.tiles.status.wireless;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdView;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.io.Closeables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTCompanionException;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.resources.IPConntrack;
import org.rm3l.ddwrt.resources.IPWhoisInfo;
import org.rm3l.ddwrt.utils.AdUtils;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.Utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.rm3l.ddwrt.utils.Utils.getEscapedFileName;

public class ActiveIPConnectionsDetailActivity extends AppCompatActivity {

    public static final String ACTIVE_IP_CONNECTIONS_OUTPUT = "ACTIVE_IP_CONNECTIONS_OUTPUT";

    public static final String IP_TO_HOSTNAME_RESOLVER = "IP_TO_HOSTNAME_RESOLVER";

    public static final String CONNECTED_HOST = "CONNECTED_HOST";
    public static final String CONNECTED_HOST_IP = "CONNECTED_HOST_IP";

    public static final String OBSERVATION_DATE = "OBSERVATION_DATE";
    public static final Table<Integer, Integer, String> ICMP_TYPE_CODE_DESCRIPTION_TABLE = HashBasedTable.create();
    private static final String LOG_TAG = ActiveIPConnectionsDetailActivity.class.getSimpleName();

    public static final LoadingCache<String, IPWhoisInfo> mIPWhoisInfoCache = CacheBuilder
            .newBuilder()
            .softValues()
            .maximumSize(200)
            .removalListener(new RemovalListener<String, IPWhoisInfo>() {
                @Override
                public void onRemoval(@NonNull RemovalNotification<String, IPWhoisInfo> notification) {
                    Crashlytics.log(Log.DEBUG, LOG_TAG, "onRemoval(" + notification.getKey() + ") - cause: " +
                            notification.getCause());
                }
            })
            .build(new CacheLoader<String, IPWhoisInfo>() {
                @Override
                public IPWhoisInfo load(@NonNull String ipAddr) throws Exception {
                    if (isNullOrEmpty(ipAddr)) {
                        throw new IllegalArgumentException("IP Addr is invalid");
                    }
                    //Get to MAC OUI Vendor Lookup API
                    try {
                        final String urlStr = String.format("%s/%s.json",
                                IPWhoisInfo.IP_WHOIS_INFO_API_PREFIX, ipAddr);
                        Crashlytics.log(Log.DEBUG, LOG_TAG, "--> GET " + urlStr);
                        final URL url = new URL(urlStr);
                        final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                        try {
                            final int statusCode = urlConnection.getResponseCode();
                            if (statusCode == 200) {
                                final InputStream content = new BufferedInputStream(urlConnection.getInputStream());
                                try {
                                    //Read the server response and attempt to parse it as JSON
                                    final Reader reader = new InputStreamReader(content);
                                    final GsonBuilder gsonBuilder = new GsonBuilder();
                                    final Gson gson = gsonBuilder.create();
                                    final IPWhoisInfo ipWhoisInfo = gson.fromJson(reader, IPWhoisInfo.class);
                                    Crashlytics.log(Log.DEBUG, LOG_TAG, "--> Result of GET " + urlStr + ": " + ipWhoisInfo);

                                    if (ipWhoisInfo == null) {
                                        throw new DDWRTCompanionException();
                                    }

                                    return ipWhoisInfo;

                                } finally {
                                    Closeables.closeQuietly(content);
                                }
                            } else {
                                Crashlytics.log(Log.ERROR, LOG_TAG, "<--- Server responded with status code: " + statusCode);
                                if (statusCode == 204) {
                                    //No Content found on the remote server - no need to retry later
                                    return new IPWhoisInfo();
                                }
                            }

                        } finally {
                            urlConnection.disconnect();
                        }

                    } catch (final Exception e) {
                        e.printStackTrace();
                    }

                    throw new DDWRTCompanionException();
                }
            });

    static {
        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(0, 0, "Echo Reply");

        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 0, "Network unreachable");
        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 1, "Host unreachable");
        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 2, "Protocol unreachable");
        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 3, "Port unreachable");
        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 4, "Fragmentation needed but no frag. bit set");
        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 5, "Source routing failed");
        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 6, "Dest. network unknown");
        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 7, "Dest. host unknown");
        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 8, "Source host isolated");
        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 9, "Network administratively prohibited");
        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 10, "Host administratively prohibited");
        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 11, "Network unreachable for TOS");
        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 12, "Host unreachable for TOS");
        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 13, "Communication administratively prohibited");
        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 14, "Host Precedence Violation");
        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 15, "Precedence cutoff in effect");

        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(4, 0, "Source quench");

        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(5, 0, "Redirect Datagram for Network");
        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(5, 1, "Redirect Datagram for Host");
        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(5, 2, "Redirect Datagram for TOS & network");
        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(5, 3, "Redirect Datagram for the TOS & host");

        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(8, 0, "Echo request");

        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(9, 0, "Router Advertisement (Normal)");
        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(9, 16, "Router advertisement - No traffic routing");

        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(10, 0, "Route Selection");

        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(11, 0, "TTL expired in transit");
        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(11, 1, "Fragment reassembly time exceeded");

        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(12, 0, "IP header bad");
        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(12, 1, "Required options missing");
        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(12, 2, "Bad length for IP header");

        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(13, 0, "Timestamp request");

        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(14, 0, "Timestamp reply");

        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(15, 0, "Information request");

        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(16, 0, "Information reply");

        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(17, 0, "Address Mask Request");

        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(18, 0, "Address Mask Reply");

        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(30, 0, "Traceroute");

        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(31, 0, "Datagram Conversion Error");

        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(32, 0, "Mobile Host Redirect");

        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(33, 0, "IPv6 Where-Are-You");

        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(34, 0, "IPv6 I-Am-Here");

        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(35, 0, "Mobile Registration Request");

        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(36, 0, "Mobile Registration Reply");

        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(39, 0, "SKIP Algorithm Discovery Protocol");

        ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(40, 0, "Photuris, Security failures");
    }

    private final Map<IPConntrack, CardView> ipConntrackMap = Maps.newHashMap();
    private HashMap<String, String> mLocalIpToHostname;
    private ShareActionProvider mShareActionProvider;
    private String mRouterRemoteIp;
    private File mFileToShare;
    private String[] mActiveIPConnections;
    private String mActiveIPConnectionsMultiLine;
    private String mTitle;
    private String mObservationDate;
    private String mConnectedHost;
    private Menu mMenu;
    private Multimap<String, String> mSourceIpToDestinationIp = ArrayListMultimap.create();
    private Multimap<String, String> mDestinationIpToSourceIp = ArrayListMultimap.create();
    private ProgressBar mProgressBar;
    private TextView mProgressBarDesc;
    private AtomicInteger mCurrentProgress = new AtomicInteger(0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

        handleIntent(intent);

        mActiveIPConnections = intent.getStringArrayExtra(ACTIVE_IP_CONNECTIONS_OUTPUT);
        if (mActiveIPConnections == null || mActiveIPConnections.length == 0) {
            Toast.makeText(ActiveIPConnectionsDetailActivity.this, "Internal Error - No Detailed Active IP Connections list available!",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        final boolean themeLight = ColorUtils.isThemeLight(this);
        if (themeLight) {
            //Light
            setTheme(R.style.AppThemeLight);
        } else {
            //Default is Dark
            setTheme(R.style.AppThemeDark);
        }

        setContentView(R.layout.tile_status_active_ip_connections);

        if (themeLight) {
            getWindow().getDecorView()
                    .setBackgroundColor(
                            ContextCompat.getColor(this, android.R.color.white));
        }

        AdUtils.buildAndDisplayAdViewIfNeeded(this, (AdView) findViewById(R.id.tile_status_active_ip_connections_view_adView));

        mProgressBar = (ProgressBar) findViewById(R.id.tile_status_active_ip_connections_list_container_loading);
        /*
        Multiplied by 2, because the doInBackground method first resolved WHOIS,
        and the loadFinished() method handles the views
         */
        mProgressBar.setMax(mActiveIPConnections.length + 2);

        mProgressBarDesc = (TextView) findViewById(R.id.tile_status_active_ip_connections_list_container_loading_desc);
        if (themeLight) {
            mProgressBarDesc.setTextColor(
                    ContextCompat.getColor(this, R.color.black));
        } else {
            mProgressBarDesc.setTextColor(
                    ContextCompat.getColor(this, R.color.white));
        }
        mProgressBarDesc.setText("Loading...\n\n");

        mRouterRemoteIp = intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);
        mObservationDate = intent.getStringExtra(OBSERVATION_DATE);
        mConnectedHost = intent.getStringExtra(CONNECTED_HOST);

        final String connectedHostIp = intent.getStringExtra(CONNECTED_HOST_IP);

        if (isNullOrEmpty(connectedHostIp)) {
            //All Hosts
            //noinspection unchecked
            mLocalIpToHostname = (HashMap<String, String>) intent.getSerializableExtra(IP_TO_HOSTNAME_RESOLVER);
        } else {
            //Single host
            mLocalIpToHostname = new HashMap<>();
            mLocalIpToHostname.put(connectedHostIp, intent.getStringExtra(IP_TO_HOSTNAME_RESOLVER));
        }

        mActiveIPConnectionsMultiLine = Joiner.on("\n\n").join(mActiveIPConnections);

        final Toolbar mToolbar = (Toolbar) findViewById(R.id.tile_status_active_ip_connections_view_toolbar);
        if (mToolbar != null) {
            mTitle = "Active IP Connections" + (isNullOrEmpty(mConnectedHost) ? "" :
                    (" for " + mConnectedHost));
            mToolbar.setTitle(mTitle);
            setSupportActionBar(mToolbar);
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        initLoaderTask();

    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            final String query = intent.getStringExtra(SearchManager.QUERY);
            if (Strings.isNullOrEmpty(query)) {
                return;
            }
            final LinearLayout containerLayout = (LinearLayout) findViewById(R.id.tile_status_active_ip_connections_list_container);
            if (containerLayout == null) {
                Toast.makeText(this, "Internal Error - please try again later", Toast.LENGTH_SHORT).show();
                Utils.reportException(this, new IllegalStateException("containerLayout == null"));
                return;
            }
            containerLayout.removeAllViews();

            final Map<IPConntrack, CardView> ipConntrackCardViewFilteredMap = Maps.filterKeys(ipConntrackMap, new Predicate<IPConntrack>() {
                @Override
                public boolean apply(IPConntrack input) {
                    if (input == null) {
                        return false;
                    }
                    //Filter on visible fields: source IP/port, dest. IP/port, transport protocol and TCP State, device name, dest. WHOIS
                    return (containsIgnoreCase(input.getSourceAddressOriginalSide(), query) ||
                            containsIgnoreCase("" + input.getSourcePortOriginalSide(), query) ||
                            containsIgnoreCase("" + input.getDestinationAddressOriginalSide(), query) ||
                            containsIgnoreCase("" + input.getDestinationPortOriginalSide(), query) ||
                            containsIgnoreCase(input.getTransportProtocol(), query) ||
                            containsIgnoreCase(input.getTcpConnectionState(), query) ||
                            containsIgnoreCase(input.getSourceHostname(), query) ||
                            containsIgnoreCase(input.getDestWhoisOrHostname(), query));
                }
            });
            if (ipConntrackCardViewFilteredMap != null) {
                for (final CardView cardView : ipConntrackCardViewFilteredMap.values()) {
                    containerLayout.addView(cardView);
                }
            } else {
                Toast.makeText(this, "Internal Error - please try again later", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initLoaderTask() {
        //Remove all views
        final LinearLayout containerLayout = (LinearLayout) findViewById(R.id.tile_status_active_ip_connections_list_container);
        containerLayout.removeAllViews();

        final CardView.LayoutParams cardViewLayoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        cardViewLayoutParams.rightMargin = R.dimen.marginRight;
        cardViewLayoutParams.leftMargin = R.dimen.marginLeft;
        cardViewLayoutParams.bottomMargin = R.dimen.activity_vertical_margin;

        final boolean isThemeLight = ColorUtils.isThemeLight(ActiveIPConnectionsDetailActivity.this);
        final Resources resources = getResources();

        final LoaderManager supportLoaderManager = getSupportLoaderManager();

        supportLoaderManager.initLoader(0, null, new LoaderManager.LoaderCallbacks<Map<String, String>>() {
            @Override
            public Loader<Map<String, String>> onCreateLoader(int id, Bundle args) {
                final AsyncTaskLoader<Map<String, String>> asyncTaskLoader = new AsyncTaskLoader<Map<String, String>>(ActiveIPConnectionsDetailActivity.this) {
                    @Override
                    public Map<String, String> loadInBackground() {
                        ipConntrackMap.clear();

                        final Map<String, String> result = new HashMap<>();
                        String existingRecord;

                        final int totalLength = mActiveIPConnections.length;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mProgressBarDesc.setText("Resolving IPs (0/" + totalLength + ")...\n\n");
                            }
                        });

                        int i = 1;
                        for (final String activeIPConnection : mActiveIPConnections) {
                            final IPConntrack ipConntrackRow = IPConntrack.parseIpConntrackRow(activeIPConnection);
                            if (ipConntrackRow == null) {
                                continue;
                            }

                            final int j = (i++);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mProgressBarDesc.setText("Resolving IPs ( " + j + "/" + totalLength + ")...\n\n");
                                }
                            });

                            final String sourceAddressOriginalSide = ipConntrackRow.getSourceAddressOriginalSide();
                            existingRecord = result.get(sourceAddressOriginalSide);
                            if (isNullOrEmpty(existingRecord)) {
                                //Set Source IP HostName
                                final String srcIpHostnameResolved;
                                if (mLocalIpToHostname == null) {
                                    srcIpHostnameResolved = "-";
                                } else {
                                    final String val = mLocalIpToHostname.get(sourceAddressOriginalSide);
                                    if (isNullOrEmpty(val)) {
                                        srcIpHostnameResolved = "-";
                                    } else {
                                        srcIpHostnameResolved = val;
                                    }
                                }
                                result.put(sourceAddressOriginalSide, srcIpHostnameResolved);
                            }

                            final String destinationAddressOriginalSide = ipConntrackRow.getDestinationAddressOriginalSide();
                            existingRecord = result.get(destinationAddressOriginalSide);
                            if (isNullOrEmpty(existingRecord)) {
                                final String dstIpWhoisResolved;
                                if (isNullOrEmpty(destinationAddressOriginalSide)) {
                                    dstIpWhoisResolved = "-";
                                } else {
                                    IPWhoisInfo ipWhoisInfo = null;
                                    try {
                                        ipWhoisInfo = mIPWhoisInfoCache.get(destinationAddressOriginalSide);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        Utils.reportException(null, e);
                                    }
                                    final String org;
                                    if (ipWhoisInfo == null || (org = ipWhoisInfo.getOrganization()) == null || org.isEmpty()) {
                                        dstIpWhoisResolved = "-";
                                    } else {
                                        dstIpWhoisResolved = org;
                                        if (!mLocalIpToHostname.containsKey(destinationAddressOriginalSide)) {
                                            mLocalIpToHostname.put(destinationAddressOriginalSide, org);
                                        }
                                    }
                                }
                                result.put(destinationAddressOriginalSide, dstIpWhoisResolved);
                            }

                            mSourceIpToDestinationIp.put(sourceAddressOriginalSide, destinationAddressOriginalSide);
                            mDestinationIpToSourceIp.put(destinationAddressOriginalSide, sourceAddressOriginalSide);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mProgressBar.setProgress(mCurrentProgress.incrementAndGet());
                                }
                            });
                        }

                        return result;
                    }
                };
                asyncTaskLoader.forceLoad();
                return asyncTaskLoader;
            }

            @Override
            public void onLoadFinished(Loader<Map<String, String>> loader, Map<String, String> ipToHostResolvedMap) {
                int i = 0;

                if (mMenu != null) {
                    mMenu.findItem(R.id.tile_status_active_ip_connections_stats).setVisible(true);
                    mMenu.findItem(R.id.tile_status_active_ip_connections_search).setVisible(true);
                }

                mProgressBarDesc.setText("Building Views...\n\n");

                for (final String activeIPConnection : mActiveIPConnections) {
                    final IPConntrack ipConntrackRow = IPConntrack.parseIpConntrackRow(activeIPConnection);
                    if (ipConntrackRow == null) {
                        continue;
                    }
                    final CardView cardView = (CardView) getLayoutInflater()
                            .inflate(R.layout.activity_ip_connections_cardview, null);

                    //Add padding to CardView on v20 and before to prevent intersections between the Card content and rounded corners.
                    cardView.setPreventCornerOverlap(true);
                    //Add padding in API v21+ as well to have the same measurements with previous versions.
                    cardView.setUseCompatPadding(true);

                    if (isThemeLight) {
                        //Light
                        cardView.setCardBackgroundColor(
                                ContextCompat.getColor(ActiveIPConnectionsDetailActivity.this,
                                        R.color.cardview_light_background));
                    } else {
                        //Default is Dark
                        cardView.setCardBackgroundColor(
                                ContextCompat.getColor(ActiveIPConnectionsDetailActivity.this,
                                        R.color.cardview_dark_background));
                    }

                    //Highlight CardView
//                    cardView.setCardElevation(20f);

                    final String sourceAddressOriginalSide = ipConntrackRow.getSourceAddressOriginalSide();
                    ((TextView) cardView.findViewById(R.id.activity_ip_connections_device_source_ip))
                            .setText(sourceAddressOriginalSide);
                    ((TextView) cardView.findViewById(R.id.activity_ip_connections_details_source_ip))
                            .setText(sourceAddressOriginalSide);

                    final long ttl = ipConntrackRow.getTimeout();
                    ((TextView) cardView.findViewById(R.id.activity_ip_connections_details_ttl))
                            .setText(ttl > 0 ? String.valueOf(ttl) : "-");

                    final long packets = ipConntrackRow.getPackets();
                    ((TextView) cardView.findViewById(R.id.activity_ip_connections_details_packets))
                            .setText(packets > 0 ? String.valueOf(packets) : "-");

                    final long bytes = ipConntrackRow.getBytes();
                    ((TextView) cardView.findViewById(R.id.activity_ip_connections_details_bytes))
                            .setText(bytes > 0 ? String.valueOf(bytes) : "-");

                    final String destinationAddressOriginalSide = ipConntrackRow.getDestinationAddressOriginalSide();
                    ((TextView) cardView.findViewById(R.id.activity_ip_connections_device_dest_ip))
                            .setText(destinationAddressOriginalSide);
                    ((TextView) cardView.findViewById(R.id.activity_ip_connections_details_destination_ip))
                            .setText(destinationAddressOriginalSide);

                    final String protocol = ipConntrackRow.getTransportProtocol();
                    final TextView proto = (TextView) cardView.findViewById(R.id.activity_ip_connections_device_proto);
                    proto.setText(isNullOrEmpty(protocol) ? "-" : protocol.toUpperCase());
                    ((TextView) cardView.findViewById(R.id.activity_ip_connections_details_protocol))
                            .setText(isNullOrEmpty(protocol) ? "-" : protocol.toUpperCase());

                    final String tcpConnectionState = ipConntrackRow.getTcpConnectionState();
                    final TextView tcpConnectionStateView = (TextView) cardView.findViewById(R.id.activity_ip_connections_tcp_connection_state);
                    final TextView tcpConnectionStateDetailedView = (TextView) cardView.findViewById(R.id.activity_ip_connections_details_tcp_connection_state);

                    if (!isNullOrEmpty(tcpConnectionState)) {
                        tcpConnectionStateView.setText(tcpConnectionState);
                        tcpConnectionStateDetailedView.setText(tcpConnectionState);
                        tcpConnectionStateView.setVisibility(View.VISIBLE);
                    } else {
                        tcpConnectionStateView.setVisibility(View.GONE);
                        tcpConnectionStateDetailedView.setText("-");
                    }

                    final View tcpConnectionStateDetailedViewSep = cardView
                            .findViewById(R.id.activity_ip_connections_details_tcp_connection_state_sep);
                    final View tcpConnectionStateDetailedViewTitle = cardView.findViewById(R.id.activity_ip_connections_details_tcp_connection_state_title);
                    if ("TCP".equalsIgnoreCase(protocol)) {
                        tcpConnectionStateDetailedView.setVisibility(View.VISIBLE);
                        tcpConnectionStateDetailedViewSep
                                .setVisibility(View.VISIBLE);
                        tcpConnectionStateDetailedViewTitle
                                .setVisibility(View.VISIBLE);
                    } else {
                        tcpConnectionStateDetailedView.setVisibility(View.GONE);
                        tcpConnectionStateDetailedViewSep
                                .setVisibility(View.GONE);
                        tcpConnectionStateDetailedViewTitle
                                .setVisibility(View.GONE);
                    }

                    //ICMP
                    //ID
                    final View icmpIdTitle = cardView
                            .findViewById(R.id.activity_ip_connections_details_icmp_id_title);
                    final View icmpIdSep = cardView
                            .findViewById(R.id.activity_ip_connections_details_icmp_id_sep);
                    final TextView icmpId = (TextView) cardView
                            .findViewById(R.id.activity_ip_connections_details_icmp_id);
                    //Type
                    final View icmpTypeTitle = cardView
                            .findViewById(R.id.activity_ip_connections_details_icmp_type_title);
                    final View icmpTypeSep = cardView
                            .findViewById(R.id.activity_ip_connections_details_icmp_type_sep);
                    final TextView icmpType = (TextView) cardView
                            .findViewById(R.id.activity_ip_connections_details_icmp_type);
                    //Code
                    final View icmpCodeTitle = cardView
                            .findViewById(R.id.activity_ip_connections_details_icmp_code_title);
                    final View icmpCodeSep = cardView
                            .findViewById(R.id.activity_ip_connections_details_icmp_code_sep);
                    final TextView icmpCode = (TextView) cardView
                            .findViewById(R.id.activity_ip_connections_details_icmp_code);
                    //Ctrl Message
                    final View icmpCtrlMsgTitle = cardView
                            .findViewById(R.id.activity_ip_connections_details_icmp_ctrl_msg_title);
                    final View icmpCtrlMsgSep = cardView
                            .findViewById(R.id.activity_ip_connections_details_icmp_ctrl_msg_sep);
                    final TextView icmpCtrlMsg = (TextView) cardView
                            .findViewById(R.id.activity_ip_connections_details_icmp_ctrl_msg);

                    final View[] icmpViews = new View[]{
                            icmpIdTitle, icmpIdSep, icmpId,
                            icmpTypeTitle, icmpTypeSep, icmpType,
                            icmpCodeTitle, icmpCodeSep, icmpCode,
                            icmpCtrlMsgTitle, icmpCtrlMsgSep, icmpCtrlMsg
                    };

                    if ("ICMP".equalsIgnoreCase(protocol)) {
                        final int ipConntrackRowIcmpType = ipConntrackRow.getIcmpType();
                        final int ipConntrackRowIcmpCode = ipConntrackRow.getIcmpCode();
                        icmpId.setText(isNullOrEmpty(ipConntrackRow.getIcmpId()) ?
                                "-" : ipConntrackRow.getIcmpId());
                        icmpType.setText((ipConntrackRowIcmpType < 0) ?
                                "-" : String.valueOf(ipConntrackRowIcmpType));
                        icmpCode.setText((ipConntrackRowIcmpCode < 0) ?
                                "-" : String.valueOf(ipConntrackRowIcmpCode));

                        final String ctrlMsg = ICMP_TYPE_CODE_DESCRIPTION_TABLE.get(ipConntrackRowIcmpType, ipConntrackRowIcmpCode);
                        icmpCtrlMsg.setText(isNullOrEmpty(ctrlMsg) ? "-" : ctrlMsg);

                        for (final View icmpView : icmpViews) {
                            icmpView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        for (final View icmpView : icmpViews) {
                            icmpView.setVisibility(View.GONE);
                        }
                    }

                    final int sourcePortOriginalSide = ipConntrackRow.getSourcePortOriginalSide();
                    final String srcPortToDisplay = sourcePortOriginalSide > 0 ? String.valueOf(sourcePortOriginalSide) : "-";
                    ((TextView) cardView.findViewById(R.id.activity_ip_connections_sport))
                            .setText(srcPortToDisplay);
                    ((TextView) cardView.findViewById(R.id.activity_ip_connections_details_source_port))
                            .setText(srcPortToDisplay);

                    final int destinationPortOriginalSide = ipConntrackRow.getDestinationPortOriginalSide();
                    final String dstPortToDisplay = destinationPortOriginalSide > 0 ? String.valueOf(destinationPortOriginalSide) : "-";
                    ((TextView) cardView.findViewById(R.id.activity_ip_connections_dport))
                            .setText(dstPortToDisplay);
                    ((TextView) cardView.findViewById(R.id.activity_ip_connections_details_destination_port))
                            .setText(dstPortToDisplay);

                    final TextView rawLineView = (TextView) cardView.findViewById(R.id.activity_ip_connections_raw_line);
                    rawLineView.setText(activeIPConnection);

                    if (i == 0) {
                        rawLineView.setVisibility(View.VISIBLE);
                    }
                    i++;

                    cardView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final View placeholderView = cardView.findViewById(R.id.activity_ip_connections_details_placeholder);
                            if (placeholderView.getVisibility() == View.VISIBLE) {
                                placeholderView.setVisibility(View.GONE);
                            } else {
                                placeholderView.setVisibility(View.VISIBLE);
                            }
                        }
                    });

                    containerLayout.addView(cardView);

                    final TextView srcIpHostname = (TextView) cardView.findViewById(R.id.activity_ip_connections_source_ip_hostname);
                    final TextView srcIpHostnameDetails = (TextView) cardView.findViewById(R.id.activity_ip_connections_details_source_host);
                    final ProgressBar srcIpHostnameLoading = (ProgressBar) cardView.findViewById(R.id.activity_ip_connections_source_ip_hostname_loading);
                    //Set Source IP HostName
                    if (ipToHostResolvedMap == null) {
                        srcIpHostname.setText("");
                        srcIpHostnameDetails.setText("-");
                    } else {
                        final String srcIpHostnameResolved = ipToHostResolvedMap.get(sourceAddressOriginalSide);
                        srcIpHostname.setText(isNullOrEmpty(srcIpHostnameResolved) ? "" : srcIpHostnameResolved);
                        srcIpHostnameDetails.setText(isNullOrEmpty(srcIpHostnameResolved) ? "-" : srcIpHostnameResolved);
                        ipConntrackRow.setSourceHostname(srcIpHostnameResolved);
                    }
                    srcIpHostname.setVisibility(View.VISIBLE);
                    srcIpHostnameLoading.setVisibility(View.GONE);

                    //... and Destination IP Address Organization (if available)
                    final TextView destIpOrg = (TextView) cardView.findViewById(R.id.activity_ip_connections_dest_ip_org);
                    final TextView destIpOrgDetails = (TextView) cardView.findViewById(R.id.activity_ip_connections_details_destination_whois);
                    final ProgressBar destIpOrgLoading = (ProgressBar) cardView.findViewById(R.id.activity_ip_connections_dest_ip_org_loading);
                    if (ipToHostResolvedMap == null) {
                        destIpOrg.setText("");
                        srcIpHostnameDetails.setText("-");
                    } else {
                        final String dstIpHostnameResolved = ipToHostResolvedMap.get(destinationAddressOriginalSide);
                        destIpOrg.setText(isNullOrEmpty(dstIpHostnameResolved) ? "" : dstIpHostnameResolved);
                        destIpOrgDetails.setText(isNullOrEmpty(dstIpHostnameResolved) ? "" : dstIpHostnameResolved);
                        ipConntrackRow.setDestWhoisOrHostname(dstIpHostnameResolved);
                    }

                    destIpOrg.setVisibility(View.VISIBLE);
                    destIpOrgLoading.setVisibility(View.GONE);

//                    mProgressBar.setProgress(mCurrentProgress.incrementAndGet());
                    ipConntrackMap.put(ipConntrackRow, cardView);
                }

                mProgressBar.setVisibility(View.GONE);
                mProgressBarDesc.setVisibility(View.GONE);
                containerLayout.setVisibility(View.VISIBLE);

            }

            @Override
            public void onLoaderReset(Loader<Map<String, String>> loader) {
                //Nothing to do
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tile_status_active_ip_connections_options, menu);

        this.mMenu = menu;

        //Hide 'Stats by Source' menu item (because it is the same source)
        menu.findItem(R.id.tile_status_active_ip_connections_stats_by_source_ip)
                .setVisible(isNullOrEmpty(mConnectedHost));

        //Search
        final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        final SearchView searchView = (SearchView) menu
                .findItem(R.id.tile_status_active_ip_connections_search).getActionView();

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        // Get the search close button image view
        final ImageView closeButton = (ImageView) searchView.findViewById(R.id.search_close_btn);
        if (closeButton != null) {
            // Set on click listener
            closeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Reset views
                    final LinearLayout containerLayout = (LinearLayout) findViewById(R.id.tile_status_active_ip_connections_list_container);
                    containerLayout.removeAllViews();

                    for (final CardView cardView : ipConntrackMap.values()) {
                        containerLayout.addView(cardView);
                    }
                    //Hide it now
                    searchView.setIconified(true);
                }
            });
        }

        final MenuItem shareMenuItem = menu.findItem(R.id.tile_status_active_ip_connections_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat
                .getActionProvider(shareMenuItem);
        if (mShareActionProvider == null) {
            mShareActionProvider = new ShareActionProvider(this);
            MenuItemCompat.setActionProvider(shareMenuItem, mShareActionProvider);
        }

        mFileToShare = new File(getCacheDir(),
                getEscapedFileName(String.format("%s on Router %s on %s",
                        mTitle, nullToEmpty(mRouterRemoteIp), mObservationDate)) + ".txt");

        Exception exception = null;
        OutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(mFileToShare, false));
            //noinspection ConstantConditions
            outputStream.write(mActiveIPConnectionsMultiLine.getBytes());
        } catch (IOException e) {
            exception = e;
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (exception != null) {
            Crouton.makeText(this,
                    "Error while trying to share Active IP Connections - please try again later",
                    Style.ALERT).show();
            return true;
        }

        setShareFile(mFileToShare);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.tile_status_active_ip_connections_stats_by_source_ip: {
                final AlertDialog alertDialog = Utils.buildAlertDialog(this, null,
                        "Loading Pie Chart (distribution by Source IPs)...", false, false);
                alertDialog.show();
                ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        final HashMap<String, Integer> connectionsCountBySourceIp = new HashMap<>();
                        for (final Map.Entry<String, Collection<String>> entry : mSourceIpToDestinationIp.asMap().entrySet()) {
                            final Collection<String> value = entry.getValue();
                            if (value == null) {
                                continue;
                            }
                            connectionsCountBySourceIp.put(entry.getKey(), value.size());
                        }
                        final Intent srcStatsIntent = new Intent(ActiveIPConnectionsDetailActivity.this,
                                ActiveIPConnectionsDetailStatsActivity.class);
                        srcStatsIntent.putExtra(ActiveIPConnectionsDetailActivity.OBSERVATION_DATE, mObservationDate);
                        srcStatsIntent.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouterRemoteIp);
                        srcStatsIntent.putExtra(IP_TO_HOSTNAME_RESOLVER, mLocalIpToHostname);
                        srcStatsIntent.putExtra(ActiveIPConnectionsDetailStatsActivity.BY, ActiveIPConnectionsDetailStatsActivity.ByFilter.SOURCE);
                        srcStatsIntent.putExtra(ActiveIPConnectionsDetailStatsActivity.CONNECTIONS_COUNT_MAP, connectionsCountBySourceIp);
                        startActivity(srcStatsIntent);
                        alertDialog.cancel();
                    }
                }, 1000l);
            }

                return true;

            case R.id.tile_status_active_ip_connections_stats_by_destination_ip: {
                final AlertDialog alertDialog2 = Utils.buildAlertDialog(this, null,
                        "Loading Pie Chart (distribution by Destination IPs)...", false, false);
                alertDialog2.show();
                ((TextView) alertDialog2.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        final HashMap<String, Integer> connectionsCountByDestinationIp = new HashMap<>();
                        for (final Map.Entry<String, Collection<String>> entry : mDestinationIpToSourceIp.asMap().entrySet()) {
                            final Collection<String> value = entry.getValue();
                            if (value == null) {
                                continue;
                            }
                            connectionsCountByDestinationIp.put(entry.getKey(), value.size());
                        }
                        final Intent destStatsIntent = new Intent(ActiveIPConnectionsDetailActivity.this,
                                ActiveIPConnectionsDetailStatsActivity.class);
                        destStatsIntent.putExtra(ActiveIPConnectionsDetailActivity.OBSERVATION_DATE, mObservationDate);
                        destStatsIntent.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouterRemoteIp);
                        destStatsIntent.putExtra(IP_TO_HOSTNAME_RESOLVER, mLocalIpToHostname);
                        destStatsIntent.putExtra(ActiveIPConnectionsDetailStatsActivity.BY, ActiveIPConnectionsDetailStatsActivity.ByFilter.DESTINATION);
                        destStatsIntent.putExtra(ActiveIPConnectionsDetailStatsActivity.CONNECTIONS_COUNT_MAP, connectionsCountByDestinationIp);
                        startActivity(destStatsIntent);
                        alertDialog2.cancel();
                    }
                }, 1000l);
            }

                return true;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    // Call to update the share intent
    private void setShareIntent(Intent shareIntent) {
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    private void setShareFile(File file) {
        if (mShareActionProvider == null) {
            return;
        }

        final Uri uriForFile = FileProvider
                .getUriForFile(this, DDWRTCompanionConstants.FILEPROVIDER_AUTHORITY, file);

        mShareActionProvider.setOnShareTargetSelectedListener(new ShareActionProvider.OnShareTargetSelectedListener() {
            @Override
            public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
                grantUriPermission(intent.getComponent().getPackageName(),
                        uriForFile, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                return true;
            }
        });

        final Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);
        sendIntent.setType("text/html");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Active IP Connections on Router '" +
                mRouterRemoteIp + "' on " + mObservationDate);
        String body = "";
        if (!isNullOrEmpty(mConnectedHost)) {
            body  = (mTitle + " on " + mObservationDate);
        }
        body += Utils.getShareIntentFooter();

        sendIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(body.replaceAll("\n", "<br/>")));

        sendIntent.setData(uriForFile);
//        sendIntent.setType("text/plain");
        sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        setShareIntent(sendIntent);

    }

    @Override
    protected void onDestroy() {
        if (mFileToShare != null) {
            //noinspection ResultOfMethodCallIgnored
            mFileToShare.delete();
        }
        super.onDestroy();
    }

}
