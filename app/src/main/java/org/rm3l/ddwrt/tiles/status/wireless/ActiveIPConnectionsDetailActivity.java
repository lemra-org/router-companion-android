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
import android.support.v4.app.NavUtils;
import android.support.v4.content.FileProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Joiner;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.resources.IPConntrack;
import org.rm3l.ddwrt.utils.ColorUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

public class ActiveIPConnectionsDetailActivity extends ActionBarActivity {

    public static final String ACTIVE_IP_CONNECTIONS_OUTPUT = "ACTIVE_IP_CONNECTIONS_OUTPUT";

    public static final String CONNECTED_HOST = "CONNECTED_HOST";

    public static final String OBSERVATION_DATE = "OBSERVATION_DATE";

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

        final boolean themeLight = ColorUtils.isThemeLight(this);
        if (themeLight) {
            //Light
            setTheme(R.style.AppThemeLight);
        } else {
            //Default is Dark
            setTheme(R.style.AppThemeDark);
        }

        setContentView(R.layout.tile_status_active_ip_connections);

//        if (themeLight) {
//            final Resources resources = getResources();
//            getWindow().getDecorView()
//                    .setBackgroundColor(resources.getColor(android.R.color.white));
////            ((TextView) findViewById(R.id.tile_status_active_ip_connections))
////                    .setTextColor(resources.getColor(R.color.black));
//        }

        final Intent intent = getIntent();
        mRouterRemoteIp = intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);
        mObservationDate = intent.getStringExtra(OBSERVATION_DATE);
        mConnectedHost = intent.getStringExtra(CONNECTED_HOST);

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

        mActiveIPConnections = intent.getStringArrayExtra(ACTIVE_IP_CONNECTIONS_OUTPUT);
        if (mActiveIPConnections == null || mActiveIPConnections.length == 0) {
            Toast.makeText(this, "Internal Error - No Detailed Active IP Connections list available!",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mActiveIPConnectionsMultiLine = Joiner.on("\n\n").join(mActiveIPConnections);

    }

    @Override
    protected void onStart() {
        super.onStart();

        new Handler(Looper.getMainLooper()).
                post(new Runnable() {
                    @Override
                    public void run() {
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
                        }
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tile_status_active_ip_connections_options, menu);

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
