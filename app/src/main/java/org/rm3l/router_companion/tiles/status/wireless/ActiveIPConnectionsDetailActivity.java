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
package org.rm3l.router_companion.tiles.status.wireless;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.Loader;
import android.support.v4.content.PermissionChecker;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
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
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.RowSortedTable;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.squareup.picasso.Callback;

import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.exceptions.DDWRTCompanionException;
import org.rm3l.router_companion.lookup.IPGeoLookupService;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.resources.IPConntrack;
import org.rm3l.router_companion.resources.IPWhoisInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.status.wireless.stats.ActiveIPConnectionsStatsAdapter;
import org.rm3l.router_companion.utils.AdUtils;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.utils.ImageUtils;
import org.rm3l.router_companion.utils.NetworkUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.snackbar.SnackbarCallback;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils;
import org.rm3l.router_companion.widgets.RecyclerViewEmptySupport;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import de.keyboardsurfer.android.widget.crouton.Style;
import retrofit2.Response;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.rm3l.router_companion.tiles.status.wireless.stats.ActiveIPConnectionsStatsAdapter.BY_DESTINATION;
import static org.rm3l.router_companion.tiles.status.wireless.stats.ActiveIPConnectionsStatsAdapter.BY_DESTINATION_COUNTRY;
import static org.rm3l.router_companion.tiles.status.wireless.stats.ActiveIPConnectionsStatsAdapter.BY_DESTINATION_PORT;
import static org.rm3l.router_companion.tiles.status.wireless.stats.ActiveIPConnectionsStatsAdapter.BY_PROTOCOL;
import static org.rm3l.router_companion.tiles.status.wireless.stats.ActiveIPConnectionsStatsAdapter.BY_SOURCE;
import static org.rm3l.router_companion.tiles.status.wireless.stats.ActiveIPConnectionsStatsAdapter.SEPARATOR;
import static org.rm3l.router_companion.utils.Utils.getEscapedFileName;
import static org.rm3l.router_companion.utils.Utils.fromHtml;

public class ActiveIPConnectionsDetailActivity extends AppCompatActivity {

    public static final String ROUTER_REMOTE_IP = "ROUTER_REMOTE_IP";
    public static final String ACTIVE_IP_CONNECTIONS_OUTPUT = "ACTIVE_IP_CONNECTIONS_OUTPUT";

    public static final String IP_TO_HOSTNAME_RESOLVER = "IP_TO_HOSTNAME_RESOLVER";

    public static final String CONNECTED_HOST = "CONNECTED_HOST";
    public static final String CONNECTED_HOST_IP = "CONNECTED_HOST_IP";

    public static final String OBSERVATION_DATE = "OBSERVATION_DATE";
    public static final Table<Integer, Integer, String> ICMP_TYPE_CODE_DESCRIPTION_TABLE = HashBasedTable.create();
    private static final String LOG_TAG = ActiveIPConnectionsDetailActivity.class.getSimpleName();

    private static final IPGeoLookupService mIPGeoLookupService  =
            NetworkUtils.createApiService(null, IPWhoisInfo.IP_WHOIS_INFO_API_PREFIX, IPGeoLookupService.class);

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
                        final Response<IPWhoisInfo> response = mIPGeoLookupService.lookupIP(ipAddr).execute();
                        NetworkUtils.checkResponseSuccessful(response);
                        return response.body();
                    } catch (final Exception e) {
                        e.printStackTrace();
                        throw new DDWRTCompanionException(e);
                    }
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

    private ConcurrentMap<String, String> mLocalIpToHostname;
    private ShareActionProvider mShareActionProvider;
    private String mRouterRemoteIp;
    private File mFileToShare;
    private List<IPConntrack> mActiveIPConnections;
    private String mActiveIPConnectionsMultiLine;
    private String mTitle;
    private String mObservationDate;
    private String mConnectedHost;
    private Menu optionsMenu;

    private RecyclerViewEmptySupport mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private View slidingUpPanelLoading;
    private RecyclerViewEmptySupport mStatsRecyclerView;
    private RecyclerView.Adapter mStatsAdapter;
    private RecyclerView.LayoutManager mStatsLayoutManager;

    private Map<String, String> ipToHostResolvedMap;
    private LinearLayout slidingUpPanel;

    private ProgressBar loadingView;
    private LinearLayout contentView;
    private HashMap<String, String> mDestinationIpToCountry;
    private TextView slidingUpPanelStatsTitle;
    private TextView loadingViewText;

    private String mRouterUuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

        final String[] activeIpConnArray = intent.getStringArrayExtra(ACTIVE_IP_CONNECTIONS_OUTPUT);
        if (activeIpConnArray == null || activeIpConnArray.length == 0) {
            Toast.makeText(ActiveIPConnectionsDetailActivity.this, "Internal Error - No Detailed Active IP Connections list available!",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mRouterUuid = intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);
        mRouterRemoteIp = intent.getStringExtra(ROUTER_REMOTE_IP);
        mObservationDate = intent.getStringExtra(OBSERVATION_DATE);
        mConnectedHost = intent.getStringExtra(CONNECTED_HOST);

        final String connectedHostIp = intent.getStringExtra(CONNECTED_HOST_IP);

        final boolean singleHost = !isNullOrEmpty(connectedHostIp);
        if (!singleHost) {
            //All Hosts
            //noinspection unchecked
            mLocalIpToHostname = new ConcurrentHashMap<>((HashMap<String, String>)
                    intent.getSerializableExtra(IP_TO_HOSTNAME_RESOLVER));
        } else {
            //Single host
            mLocalIpToHostname = new ConcurrentHashMap<>();
            mLocalIpToHostname.put(connectedHostIp, intent.getStringExtra(IP_TO_HOSTNAME_RESOLVER));
        }

        mActiveIPConnections = new ArrayList<>();
        ipToHostResolvedMap = new ConcurrentHashMap<>();

        for (final String activeIpConn : activeIpConnArray) {
            try {
                final IPConntrack ipConntrackRow = IPConntrack.parseIpConntrackRow(activeIpConn);
                if (ipConntrackRow == null) {
                    continue;
                }
                mActiveIPConnections.add(ipConntrackRow);
            } catch (final Exception e) {
                Crashlytics.logException(e);
            }
        }

        handleIntent(intent);

        mDestinationIpToCountry = new HashMap<>();

        final Router router = RouterManagementActivity.getDao(this).getRouter(mRouterUuid);
        ColorUtils.setAppTheme(this, router != null ? router.getRouterFirmware() : null, false);

        final boolean themeLight = ColorUtils.isThemeLight(this);
//        if (themeLight) {
//            //Light
//            setTheme(R.style.AppThemeLight);
////            getWindow().getDecorView()
////                    .setBackgroundColor(ContextCompat.getColor(this,
////                            android.R.color.white));
//        } else {
//            //Default is Dark
//            setTheme(R.style.AppThemeDark);
//        }

        setContentView(R.layout.tile_status_active_ip_connections);

        AdUtils.buildAndDisplayAdViewIfNeeded(this, (AdView) findViewById(R.id.tile_status_active_ip_connections_view_adView));

        mActiveIPConnectionsMultiLine = Joiner.on("\n\n").join(mActiveIPConnections);

        final Toolbar mToolbar = (Toolbar) findViewById(R.id.tile_status_active_ip_connections_view_toolbar);
        if (mToolbar != null) {
            mTitle = "Active IP Connections";
            mToolbar.setTitle(mTitle);
            mToolbar.setSubtitle(isNullOrEmpty(mConnectedHost) ? "" : mConnectedHost);
            mToolbar.setTitleTextAppearance(getApplicationContext(), R.style.ToolbarTitle);
            mToolbar.setSubtitleTextAppearance(getApplicationContext(), R.style.ToolbarSubtitle);
            mToolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
            mToolbar.setSubtitleTextColor(ContextCompat.getColor(this, R.color.white));

            setSupportActionBar(mToolbar);
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        this.loadingView = (ProgressBar)
                findViewById(R.id.tile_status_active_ip_connections_view_loadingview);
        this.loadingViewText = (TextView)
                findViewById(R.id.tile_status_active_ip_connections_view_loadingview_text);

        loadingView.setProgress(3);
        loadingViewText.setText("Initializing...");

        this.contentView = (LinearLayout)
                findViewById(R.id.tile_status_active_ip_connections_view_recyclerview_linearlayout);

        mRecyclerView = (RecyclerViewEmptySupport)
                findViewById(R.id.tile_status_active_ip_connections_recycler_view);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        // allows for optimizations if all items are of the same size:
        mRecyclerView.setHasFixedSize(true);
        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.scrollToPosition(0);
        mRecyclerView.setLayoutManager(mLayoutManager);
        final TextView emptyView = (TextView) findViewById(R.id.empty_view);
        if (themeLight) {
            emptyView.setTextColor(ContextCompat.getColor(this, R.color.black));
        } else {
            emptyView.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
        mRecyclerView.setEmptyView(emptyView);
        // specify an adapter (see also next example)
        mAdapter = new ActiveIPConnectionsDetailRecyclerViewAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        //Stats
        this.slidingUpPanel = (LinearLayout)
                findViewById(R.id.active_ip_connections_stats);
        this.slidingUpPanelStatsTitle = (TextView) findViewById(R.id.active_ip_connections_stats_title);
        if (themeLight) {
            slidingUpPanel.setBackgroundColor(ContextCompat.getColor(this, R.color.black_semi_transparent));
            this.slidingUpPanelStatsTitle.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            slidingUpPanel.setBackgroundColor(ContextCompat.getColor(this, R.color.white_semi_transparent));
            this.slidingUpPanelStatsTitle.setTextColor(ContextCompat.getColor(this, R.color.black));
        }

        this.slidingUpPanelLoading = findViewById(R.id.activity_ip_connections_stats_loading);
        this.slidingUpPanelLoading.setVisibility(View.VISIBLE);
        this.slidingUpPanelStatsTitle.setText("Computing stats...");

        mStatsRecyclerView = (RecyclerViewEmptySupport)
                findViewById(R.id.tile_status_active_ip_connections_stats_recycler_view);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        // allows for optimizations if all items are of the same size:
        mStatsRecyclerView.setHasFixedSize(true);
        // use a linear layout manager
        mStatsLayoutManager = new LinearLayoutManager(this);
        mStatsLayoutManager.scrollToPosition(0);
        mStatsRecyclerView.setLayoutManager(mStatsLayoutManager);
        final TextView statsEmptyView = (TextView)
                findViewById(R.id.tile_status_active_ip_connections_stats_recycler_view_empty_view);
        if (themeLight) {
            statsEmptyView.setTextColor(ContextCompat.getColor(this, R.color.black));
        } else {
            statsEmptyView.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
        mStatsRecyclerView.setEmptyView(statsEmptyView);
//        // specify an adapter (see also next example)
        mStatsAdapter = new ActiveIPConnectionsStatsAdapter(this, singleHost);
        mStatsRecyclerView.setAdapter(mStatsAdapter);

        new BgAsyncTask().execute();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            final String query = intent.getStringExtra(SearchManager.QUERY);
            final ActiveIPConnectionsDetailRecyclerViewAdapter adapter =
                    (ActiveIPConnectionsDetailRecyclerViewAdapter) mAdapter;
            if (Strings.isNullOrEmpty(query)) {
                return;
            }
            adapter.getFilter().filter(query);
        }
    }

    class BgAsyncTask extends AsyncTask<Void, Void, AsyncTaskResult<?>> {

        final RowSortedTable<Integer, String, Integer> statsTable = TreeBasedTable.create();

        @Override
        protected AsyncTaskResult<?> doInBackground(Void... params) {
            Exception exception = null;
            try {

                final int totalConnectionsCount = mActiveIPConnections.size();
                int index = 1;
                String existingRecord;
                for (final IPConntrack ipConntrackRow : mActiveIPConnections) {
                    if (ipConntrackRow == null) {
                        continue;
                    }
                    //total=200,
                    //i=100 => 50% = (1- (200 - 100)/200)
                    //i=0 => (1-(200-0)/200) = 0%
                    //i=50 => (1- (200 - 50)/200) => 25%
                    //
                    //total = 50
                    //i = 25 => 25/50 => 50%
                    //i = 50 => 50/50 = 1%
                    final int currentIdx = (index++);
                    final int progress = Double.valueOf(100 * (totalConnectionsCount > 100 ?
                            (1 - ((double) (totalConnectionsCount - currentIdx) / (double) totalConnectionsCount)) :
                            ((double) currentIdx / (double) totalConnectionsCount))).intValue();
                    Crashlytics.log(Log.DEBUG, LOG_TAG,
                            String.format("<currentIdx=%d , totalConnectionsCount=%d , progress=%d%%>",
                                    currentIdx, totalConnectionsCount, progress));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadingView.setProgress(progress);
                            loadingViewText.setText(String.format("Analysing IP Connection (%d / %d)...",
                                    currentIdx, totalConnectionsCount));
                        }
                    });
                    final String sourceAddressOriginalSide = ipConntrackRow.getSourceAddressOriginalSide();
                    existingRecord = ipToHostResolvedMap.get(sourceAddressOriginalSide);
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
                        ipToHostResolvedMap.put(sourceAddressOriginalSide, srcIpHostnameResolved);
                    }

                    final String destinationAddressOriginalSide = ipConntrackRow.getDestinationAddressOriginalSide();
                    existingRecord = ipToHostResolvedMap.get(destinationAddressOriginalSide);
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
                            if (ipWhoisInfo != null) {
                                final String country = ipWhoisInfo.getCountry();
                                if (!isNullOrEmpty(country)) {
                                    mDestinationIpToCountry.put(destinationAddressOriginalSide, country);
                                    Integer countryStats = statsTable.get(BY_DESTINATION_COUNTRY, country);
                                    if (countryStats == null) {
                                        countryStats = 0;
                                    }
                                    statsTable.put(BY_DESTINATION_COUNTRY, country, countryStats + 1);
                                }
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
                        ipToHostResolvedMap.put(destinationAddressOriginalSide, dstIpWhoisResolved);
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadingView.setProgress(progress);
                            loadingViewText.setText("Computing stats...");
                        }
                    });

                    //TODO As an enhancement, we can resolve the port to a known service (e.g, HTTP, SSH, ...)
                    final String destinationPortOriginalSide = Integer.toString(
                            ipConntrackRow.getDestinationPortOriginalSide());
                    Integer destPortStats = statsTable.get(BY_DESTINATION_PORT, destinationPortOriginalSide);
                    if (destPortStats == null) {
                        destPortStats = 0;
                    }
                    statsTable.put(BY_DESTINATION_PORT, destinationPortOriginalSide, destPortStats + 1);

                    final String transportProtocol = ipConntrackRow.getTransportProtocol();
                    if (transportProtocol != null) {
                        Integer protoStats = statsTable.get(BY_PROTOCOL, transportProtocol);
                        if (protoStats == null) {
                            protoStats = 0;
                        }
                        statsTable.put(BY_PROTOCOL, transportProtocol, protoStats + 1);
                    }

                    final String sourceInStats = String.format("%s%s%s",
                            ipToHostResolvedMap.get(sourceAddressOriginalSide),
                            SEPARATOR, sourceAddressOriginalSide);
                    Integer sourceStats = statsTable.get(BY_SOURCE, sourceInStats);
                    if (sourceStats == null) {
                        sourceStats = 0;
                    }
                    statsTable.put(BY_SOURCE, sourceInStats, sourceStats + 1);

                    final String destinationInStats = String.format("%s%s%s",
                            ipToHostResolvedMap.get(destinationAddressOriginalSide),
                            SEPARATOR, destinationAddressOriginalSide);
                    Integer destinationStats = statsTable.get(BY_DESTINATION, destinationInStats);
                    if (destinationStats == null) {
                        destinationStats = 0;
                    }
                    statsTable.put(BY_DESTINATION, destinationInStats, destinationStats + 1);

                }

            } catch (final Exception e) {
                exception = e;
            }
            return new AsyncTaskResult<>(null, exception);
        }

        @Override
        protected void onPostExecute(AsyncTaskResult<?> result) {
            super.onPostExecute(result);
            Exception exception = null;
            if (result == null || (exception = result.getException()) != null) {
                //Error - hide stats sliding layout
                Crashlytics.log(Log.DEBUG, LOG_TAG, "Error: " + (exception != null ?
                        exception.getMessage() : "No data or Result is NULL"));
                if (exception != null) {
                    exception.printStackTrace();
                }
                slidingUpPanel.setVisibility(View.GONE);
            } else {
                if (ActiveIPConnectionsDetailActivity.this.optionsMenu != null) {
                    ActiveIPConnectionsDetailActivity.this.optionsMenu
                            .findItem(R.id.tile_status_active_ip_connections_search)
                            .setVisible(true);
                    ActiveIPConnectionsDetailActivity.this.optionsMenu
                            .findItem(R.id.tile_status_active_ip_connections_share)
                            .setVisible(true);
                }
                //No error
                slidingUpPanel.setVisibility(View.VISIBLE);
                loadingView.setVisibility(View.GONE);
                loadingViewText.setVisibility(View.GONE);

                ((ActiveIPConnectionsDetailRecyclerViewAdapter) mAdapter)
                        .setActiveIPConnections(mActiveIPConnections);
                mAdapter.notifyDataSetChanged();

                contentView.setVisibility(View.VISIBLE);

                ((ActiveIPConnectionsStatsAdapter) mStatsAdapter).setStatsTable(statsTable);
                mStatsAdapter.notifyDataSetChanged();

                slidingUpPanel.setVisibility(View.VISIBLE);

                slidingUpPanelLoading.setVisibility(View.GONE);
                slidingUpPanelStatsTitle.setText("Stats");
            }
        }

    }

    static class AsyncTaskResult<T>  {
        private final T result;
        private final Exception exception;

        public AsyncTaskResult(T result, Exception exception) {
            this.result = result;
            this.exception = exception;
        }

        public T getResult() {
            return result;
        }

        public Exception getException() {
            return exception;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tile_status_active_ip_connections_options, menu);

        this.optionsMenu = menu;

        //Permission requests
        final int rwExternalStoragePermissionCheck = PermissionChecker
                .checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (rwExternalStoragePermissionCheck != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat
                    .shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                SnackbarUtils.buildSnackbar(this,
                        "Storage access is required to share data about active IP connections.",
                        "OK",
                        Snackbar.LENGTH_INDEFINITE,
                        new SnackbarCallback() {
                            @Override
                            public void onShowEvent(@Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventSwipe(int event, @Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventActionClick(int event, @Nullable Bundle bundle) throws Exception {
                                //Request permission
                                ActivityCompat.requestPermissions(ActiveIPConnectionsDetailActivity.this,
                                        new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        RouterCompanionAppConstants.Permissions.STORAGE);
                            }

                            @Override
                            public void onDismissEventTimeout(int event, @Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventManual(int event, @Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventConsecutive(int event, @Nullable Bundle bundle) throws Exception {

                            }
                        },
                        null,
                        true);
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        RouterCompanionAppConstants.Permissions.STORAGE);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

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

        if (PermissionChecker.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {

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
                Utils.displayMessage(this,
                        "Error while trying to share Active IP Connections - please try again later",
                        Style.ALERT);
                return true;
            }

            setShareFile(mFileToShare);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        switch (requestCode) {
            case RouterCompanionAppConstants.Permissions.STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Yay! Permission granted for #" + requestCode);
                    if (optionsMenu != null) {
                        final MenuItem menuItem = optionsMenu
                                .findItem(R.id.tile_status_active_ip_connections_share);
                        menuItem.setEnabled(true);
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Crashlytics.log(Log.WARN, LOG_TAG, "Boo! Permission denied for #" + requestCode);
                    Utils.displayMessage(this,
                            "Sharing of IP Connections Data will be unavailable",
                            Style.INFO);
                    if (optionsMenu != null) {
                        final MenuItem menuItem = optionsMenu
                                .findItem(R.id.tile_status_active_ip_connections_share);
                        menuItem.setEnabled(false);
                    }
                }
                return;
            }
            default:
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;

        case R.id.action_feedback:
            Utils.openFeedbackForm(this, mRouterUuid);
//                 final Intent intent = new Intent(ActiveIPConnectionsDetailActivity.this, FeedbackActivity.class);
//                 //FIXME Router UUID should also be available
//                 intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouterUuid);
//                 final File screenshotFile = new File(getCacheDir(), "feedback_screenshot.png");
//                 ViewGroupUtils.exportViewToFile(ActiveIPConnectionsDetailActivity.this, getWindow().getDecorView(), screenshotFile);
//                 intent.putExtra(FeedbackActivity.SCREENSHOT_FILE, screenshotFile.getAbsolutePath());
//                 intent.putExtra(FeedbackActivity.CALLER_ACTIVITY, this.getClass().getCanonicalName());
//
//                 startActivity(intent);
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
                .getUriForFile(this, RouterCompanionAppConstants.FILEPROVIDER_AUTHORITY, file);

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

        sendIntent.putExtra(Intent.EXTRA_TEXT, fromHtml(body.replaceAll("\n", "<br/>")));

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

    class ActiveIPConnectionsDetailRecyclerViewAdapter
            extends RecyclerView.Adapter<ActiveIPConnectionsDetailRecyclerViewAdapter.ViewHolder>
            implements Filterable {

        private final ActiveIPConnectionsDetailActivity activity;
        private final Filter mFilter;
        private final int mAsyncLoaderId;
        private List<IPConntrack> mActiveIPConnections;

        private final LoaderManager supportLoaderManager;

        public ActiveIPConnectionsDetailRecyclerViewAdapter(ActiveIPConnectionsDetailActivity activity) {
            this.activity = activity;
            this.supportLoaderManager = getSupportLoaderManager();
            this.mAsyncLoaderId = Long.valueOf(Utils.getNextLoaderId()).intValue();
            this.mFilter = new Filter() {
                @Override
                protected FilterResults performFiltering(final CharSequence constraint) {
                    final FilterResults oReturn = new FilterResults();
                    if (mActiveIPConnections == null || mActiveIPConnections.isEmpty()) {
                        return oReturn;
                    }
                    if (TextUtils.isEmpty(constraint)) {
                        oReturn.values = mActiveIPConnections;
                    } else {
                        //Filter list
                        oReturn.values = FluentIterable.from(mActiveIPConnections)
                                .filter(new Predicate<IPConntrack>() {
                                    @Override
                                    public boolean apply(IPConntrack input) {
                                        if (input == null) {
                                            return false;
                                        }
                                        //Filter on visible fields: source IP/port, dest. IP/port, transport protocol and TCP State, device name, dest. WHOIS
                                        return (containsIgnoreCase(input.getSourceAddressOriginalSide(), constraint) ||
                                                containsIgnoreCase("" + input.getSourcePortOriginalSide(), constraint) ||
                                                containsIgnoreCase("" + input.getDestinationAddressOriginalSide(), constraint) ||
                                                containsIgnoreCase("" + input.getDestinationPortOriginalSide(), constraint) ||
                                                containsIgnoreCase(input.getTransportProtocol(), constraint) ||
                                                containsIgnoreCase(input.getTcpConnectionState(), constraint) ||
                                                containsIgnoreCase(input.getSourceHostname(), constraint) ||
                                                containsIgnoreCase(input.getDestWhoisOrHostname(), constraint));
                                    }
                                }).toList();
                    }
                    return oReturn;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    final Object values = results.values;
                    if (values instanceof List) {
                        //noinspection unchecked
                        setActiveIPConnections((List<IPConntrack>) values);
                        notifyDataSetChanged();
                    }
                }
            };
        }

        public ActiveIPConnectionsDetailRecyclerViewAdapter setActiveIPConnections(List<IPConntrack> activeIPConnections) {
            this.mActiveIPConnections = activeIPConnections;
            return this;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.activity_ip_connections_cardview, parent, false);
            final CardView cardView = (CardView) v.findViewById(R.id.activity_ip_connections_card_view);
            if (ColorUtils.isThemeLight(activity)) {
                //Light
                cardView.setCardBackgroundColor(ContextCompat
                        .getColor(activity, R.color.cardview_light_background));
            } else {
                //Default is Dark
                cardView.setCardBackgroundColor(ContextCompat
                        .getColor(activity, R.color.cardview_dark_background));
            }

//        return new ViewHolder(this.context,
//                RippleViewCreator.addRippleToView(v));
            return new ViewHolder(this.activity, v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (position < 0 || position >= mActiveIPConnections.size()) {
                Utils.reportException(null, new IllegalStateException());
                Toast.makeText(activity,
                        "Internal Error. Please try again later",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            final CardView cardView = holder.cardView;
            final IPConntrack ipConntrackRow = mActiveIPConnections.get(position);
            if (ipConntrackRow == null) {
                Crashlytics.log(Log.ERROR, LOG_TAG,
                        "Invalid active IP Connection @ " + position);
                cardView.setVisibility(View.GONE);
                return;
            }

            final boolean isThemeLight = ColorUtils.isThemeLight(activity);

            cardView.setVisibility(View.VISIBLE);

            //Add padding to CardView on v20 and before to prevent intersections between the Card content and rounded corners.
            cardView.setPreventCornerOverlap(true);
            //Add padding in API v21+ as well to have the same measurements with previous versions.
            cardView.setUseCompatPadding(true);

            if (isThemeLight) {
                //Light
                cardView.setCardBackgroundColor(
                        ContextCompat.getColor(activity,
                                R.color.cardview_light_background));
            } else {
                //Default is Dark
                cardView.setCardBackgroundColor(
                        ContextCompat.getColor(activity,
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
            rawLineView.setText(ipConntrackRow.getRawLine());

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

            final ImageView destCountryFlag = (ImageView) cardView.findViewById(R.id.activity_ip_connections_destination_country_flag);

            //Fetch IP Whois info
            supportLoaderManager.initLoader(Long.valueOf(Utils.getNextLoaderId()).intValue(),
                    null, new LoaderManager.LoaderCallbacks<Void>() {

                @Override
                public Loader<Void> onCreateLoader(int id, Bundle args) {
                    final AsyncTaskLoader<Void> asyncTaskLoader = new AsyncTaskLoader<Void>(ActiveIPConnectionsDetailActivity.this) {

                        @Override
                        public Void loadInBackground() {
                            try {
                                final IPWhoisInfo whoisInfo;
                                if (destinationAddressOriginalSide == null ||
                                        (whoisInfo = mIPWhoisInfoCache
                                                .get(destinationAddressOriginalSide)) == null) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            destCountryFlag.setVisibility(View.GONE);
                                        }
                                    });
                                } else {
                                    final String countryCode = whoisInfo.getCountry_code();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ((TextView) cardView.findViewById(R.id.activity_ip_connections_details_destination_whois_country))
                                                    .setText(String.format("%s (%s)", whoisInfo.getCountry(), countryCode));
                                            ((TextView) cardView.findViewById(R.id.activity_ip_connections_details_destination_whois_region))
                                                    .setText(whoisInfo.getRegion());
                                            ((TextView) cardView.findViewById(R.id.activity_ip_connections_details_destination_whois_city))
                                                    .setText(whoisInfo.getCity());
                                        }
                                    });

                                    if (isNullOrEmpty(countryCode)) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                destCountryFlag.setVisibility(View.GONE);
                                            }
                                        });
                                    } else {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                ImageUtils.downloadImageFromUrl(ActiveIPConnectionsDetailActivity.this,
                                                        String.format("%s/%s.png",
                                                                RouterCompanionAppConstants.COUNTRY_API_SERVER_FLAG,
                                                                countryCode),
                                                        destCountryFlag,
                                                        null,
                                                        null,
                                                        new Callback() {
                                                            @Override
                                                            public void onSuccess() {
                                                                destCountryFlag.setVisibility(View.VISIBLE);
                                                            }

                                                            @Override
                                                            public void onError() {
                                                                destCountryFlag.setVisibility(View.GONE);
                                                            }
                                                        });
                                            }
                                        });
                                    }

                                }
                            } catch (final Exception e) {
                                e.printStackTrace();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        destCountryFlag.setVisibility(View.GONE);
                                    }
                                });
                                //No worries
                            }
                            return null;
                        }
                    };
                    asyncTaskLoader.forceLoad();
                    return asyncTaskLoader;
                }

                @Override
                public void onLoadFinished(Loader<Void> loader, Void data) {

                }

                @Override
                public void onLoaderReset(Loader<Void> loader) {

                }
            }).forceLoad();

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
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        final String destinationAddressOriginalSide = ipConntrackRow.getDestinationAddressOriginalSide();
                        final String dstIpWhoisResolved;
                        if (isNullOrEmpty(destinationAddressOriginalSide)) {
                            dstIpWhoisResolved = "-";
                        } else {
                            IPWhoisInfo ipWhoisInfo = null;
                            try {
                                ipWhoisInfo = mIPWhoisInfoCache.getIfPresent(destinationAddressOriginalSide);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Utils.reportException(null, e);
                            }
//                            if (ipWhoisInfo != null && !isNullOrEmpty(ipWhoisInfo.getCountry())) {
//                                mDestinationIpToCountry.put(destinationAddressOriginalSide, ipWhoisInfo.getCountry());
//                            }
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
                        ipToHostResolvedMap.put(destinationAddressOriginalSide, dstIpWhoisResolved);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final String dstIpHostnameResolved = ipToHostResolvedMap.get(destinationAddressOriginalSide);
                                destIpOrg.setText(isNullOrEmpty(dstIpHostnameResolved) ? "" : dstIpHostnameResolved);
                                destIpOrgDetails.setText(isNullOrEmpty(dstIpHostnameResolved) ? "" : dstIpHostnameResolved);
                                ipConntrackRow.setDestWhoisOrHostname(dstIpHostnameResolved);
                            }
                        });
                    }
                });
            } else {
                final String dstIpHostnameResolved = ipToHostResolvedMap.get(destinationAddressOriginalSide);
                destIpOrg.setText(isNullOrEmpty(dstIpHostnameResolved) ? "-" : dstIpHostnameResolved);
                destIpOrgDetails.setText(isNullOrEmpty(dstIpHostnameResolved) ? "" : dstIpHostnameResolved);
                ipConntrackRow.setDestWhoisOrHostname(dstIpHostnameResolved);
            }

            destIpOrg.setVisibility(View.VISIBLE);
            destIpOrgLoading.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() {
            return mActiveIPConnections != null ? mActiveIPConnections.size() : 0;
        }

        @Override
        public Filter getFilter() {
            return mFilter;
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            final View itemView;

            final Context mContext;

            final CardView cardView;

            public ViewHolder(Context context, View itemView) {
                super(itemView);
                this.mContext = context;
                this.itemView = itemView;
                this.cardView = (CardView) itemView
                        .findViewById(R.id.activity_ip_connections_card_view);
            }
        }

    }

}
