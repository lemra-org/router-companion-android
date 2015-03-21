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

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.FileProvider;
import android.support.v4.content.Loader;
import android.support.v4.util.LruCache;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Joiner;
import com.google.common.io.Closeables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.resources.IPConntrack;
import org.rm3l.ddwrt.resources.IPWhoisInfo;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

public class ActiveIPConnectionsDetailActivity extends ActionBarActivity {

    public static final String ACTIVE_IP_CONNECTIONS_OUTPUT = "ACTIVE_IP_CONNECTIONS_OUTPUT";

    public static final String IP_TO_HOSTNAME_RESOLVER = "IP_TO_HOSTNAME_RESOLVER";

    public static final String CONNECTED_HOST = "CONNECTED_HOST";
    public static final String CONNECTED_HOST_IP = "CONNECTED_HOST_IP";

    public static final String OBSERVATION_DATE = "OBSERVATION_DATE";
    public static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static final String LOG_TAG = ActiveIPConnectionsDetailActivity.class.getSimpleName();
    private static final LruCache<String, IPWhoisInfo> mIPWhoisInfoCache = new LruCache<String, IPWhoisInfo>(200) {
        @Override
        protected IPWhoisInfo create(String ipAddr) {
            if (isNullOrEmpty(ipAddr)) {
                return null;
            }
            //Get to MAC OUI Vendor Lookup API
            try {
                final String url = String.format("%s/%s.json",
                        IPWhoisInfo.IP_WHOIS_INFO_API_PREFIX, ipAddr);
                Log.d(LOG_TAG, "--> GET " + url);
                final HttpGet httpGet = new HttpGet(url);
                final HttpResponse httpResponse = Utils.getThreadSafeClient().execute(httpGet);
                final StatusLine statusLine = httpResponse.getStatusLine();
                final int statusCode = statusLine.getStatusCode();

                if (statusCode == 200) {
                    final HttpEntity entity = httpResponse.getEntity();
                    final InputStream content = entity.getContent();
                    try {
                        //Read the server response and attempt to parse it as JSON
                        final Reader reader = new InputStreamReader(content);
                        final GsonBuilder gsonBuilder = new GsonBuilder();
                        final Gson gson = gsonBuilder.create();
                        final IPWhoisInfo ipWhoisInfo = gson.fromJson(reader, IPWhoisInfo.class);
                        Log.d(LOG_TAG, "--> Result of GET " + url + ": " + ipWhoisInfo);
                        return ipWhoisInfo;

                    } finally {
                        Closeables.closeQuietly(content);
                        entity.consumeContent();
                    }
                } else {
                    Log.e(LOG_TAG, "<--- Server responded with status code: " + statusCode);
                    if (statusCode == 204) {
                        //No Content found on the remote server - no need to retry later
                        return new IPWhoisInfo();
                    }
                }

            } catch (final Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    };
    private HashMap<String, String> mLocalIpToHostname;
    private ShareActionProvider mShareActionProvider;
    private String mRouterRemoteIp;
    private File mFileToShare;
    private String[] mActiveIPConnections;
    private String mActiveIPConnectionsMultiLine;
    private String mTitle;
    private String mObservationDate;
    private String mConnectedHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

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
        }

        initLoaderTask();

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
                        final Map<String, String> result = new HashMap<>();
                        String existingRecord;
                        for (final String activeIPConnection : mActiveIPConnections) {
                            final IPConntrack ipConntrackRow = IPConntrack.parseIpConntrackRow(activeIPConnection);
                            if (ipConntrackRow == null) {
                                continue;
                            }
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
                                    final IPWhoisInfo ipWhoisInfo = mIPWhoisInfoCache.get(destinationAddressOriginalSide);
                                    final String org;
                                    if (ipWhoisInfo == null || (org = ipWhoisInfo.getOrganization()) == null || org.isEmpty()) {
                                        dstIpWhoisResolved = "-";
                                    } else {
                                        dstIpWhoisResolved = org;
                                    }
                                }
                                result.put(destinationAddressOriginalSide, dstIpWhoisResolved);
                            }
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
                        cardView.setCardBackgroundColor(resources.getColor(R.color.cardview_light_background));
                    } else {
                        //Default is Dark
                        cardView.setCardBackgroundColor(resources.getColor(R.color.cardview_dark_background));
                    }

                    //Highlight CardView
                    cardView.setCardElevation(20f);

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

                    final TextView proto = (TextView) cardView.findViewById(R.id.activity_ip_connections_device_proto);
                    final String protocol = ipConntrackRow.getTransportProtocol().getDisplayName();
                    proto.setText(protocol);
                    ((TextView) cardView.findViewById(R.id.activity_ip_connections_details_protocol))
                            .setText(protocol.toUpperCase());

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
                    }

                    destIpOrg.setVisibility(View.VISIBLE);
                    destIpOrgLoading.setVisibility(View.GONE);

                }

                findViewById(R.id.tile_status_active_ip_connections_list_container_loading)
                        .setVisibility(View.GONE);
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

        //Hide 'Stats by Source' menu item (because it is the same source)
        menu.findItem(R.id.tile_status_active_ip_connections_stats_by_source_ip)
                .setVisible(isNullOrEmpty(mConnectedHost));

        final MenuItem shareMenuItem = menu.findItem(R.id.tile_status_active_ip_connections_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat
                .getActionProvider(shareMenuItem);
        if (mShareActionProvider == null) {
            mShareActionProvider = new ShareActionProvider(this);
            MenuItemCompat.setActionProvider(shareMenuItem, mShareActionProvider);
        }

        mFileToShare = new File(getCacheDir(),
                String.format("%s on Router %s on %s.txt", mTitle, nullToEmpty(mRouterRemoteIp), mObservationDate));

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
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.tile_status_active_ip_connections_stats_by_source_ip:
                //TODO
                Toast.makeText(this, "[FIXME] Stats by Source IPs", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.tile_status_active_ip_connections_stats_by_destination_ip:
                //TODO
                Toast.makeText(this, "[FIXME] Stats by Destination IPs", Toast.LENGTH_SHORT).show();
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
                .getUriForFile(this, "org.rm3l.fileprovider", file);

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
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Active IP Connections on Router '" +
                mRouterRemoteIp + "' on " + mObservationDate);
        if (!isNullOrEmpty(mConnectedHost)) {
            sendIntent.putExtra(Intent.EXTRA_TEXT, mTitle + " on " + mObservationDate);
        }

        sendIntent.setData(uriForFile);
        sendIntent.setType("text/plain");
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
