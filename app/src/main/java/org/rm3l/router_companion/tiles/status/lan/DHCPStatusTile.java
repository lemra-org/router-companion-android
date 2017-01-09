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

package org.rm3l.router_companion.tiles.status.lan;

import android.content.Context;
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

import com.crashlytics.android.Crashlytics;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;

import org.apache.commons.net.util.SubnetUtils;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;

import java.util.List;

/**
 *
 */
public class DHCPStatusTile extends DDWRTTile<NVRAMInfo> {

    public static final String DHCP_END_IP = "dhcp_end_ip";
    public static final String DHCP_START_IP = "dhcp_start_ip";
    public static final Splitter IP_ADDR_SPLITTER = Splitter
            .on(".")
            .omitEmptyStrings()
            .trimResults();
    private static final String LOG_TAG = DHCPStatusTile.class.getSimpleName();
    private long mLastSync;

    public DHCPStatusTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_status_lan_dhcp_status,
                null);
    }

    @Override
    public int getTileHeaderViewId() {
        return R.id.tile_status_lan_dhcp_status_hdr;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_status_lan_dhcp_status_title;
    }

    @Nullable
    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {

                try {
                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Init background loader for " + DHCPStatusTile.class + ": routerInfo=" +
                            mRouter + " / nbRunsLoader=" + nbRunsLoader);

                    if (mRefreshing.getAndSet(true)) {
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    updateProgressBarViewSeparator(0);

                    mLastSync = System.currentTimeMillis();

                    final NVRAMInfo nvramInfo = new NVRAMInfo();

                    NVRAMInfo nvramInfoTmp = null;
                    try {
                        updateProgressBarViewSeparator(10);
                        if (Utils.isDemoRouter(mRouter)) {
                            nvramInfoTmp = new NVRAMInfo()
                                    .setProperty(NVRAMInfo.LAN_PROTO, "dhcp")
                                    .setProperty(NVRAMInfo.DHCP_DNSMASQ, "1")
                                    .setProperty(NVRAMInfo.DHCP_START, "30")
                                    .setProperty(NVRAMInfo.DHCP_NUM, "50")
                                    .setProperty(NVRAMInfo.DHCP_LEASE, "1440")
                                    .setProperty(NVRAMInfo.LAN_IPADDR, "172.17.17.1")
                                    .setProperty(NVRAMInfo.LAN_NETMASK, "255.255.255.0");
                        } else {
                            nvramInfoTmp = SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity, mRouter,
                                    mGlobalPreferences, NVRAMInfo.LAN_PROTO,
                                    NVRAMInfo.DHCP_DNSMASQ,
                                    NVRAMInfo.DHCP_START,
                                    NVRAMInfo.DHCP_NUM,
                                    NVRAMInfo.DHCP_LEASE,
                                    NVRAMInfo.LAN_IPADDR,
                                    NVRAMInfo.LAN_NETMASK);
                        }
                        updateProgressBarViewSeparator(45);
                    } finally {
                        if (nvramInfoTmp != null) {
                            nvramInfo.putAll(nvramInfoTmp);
                        }

                        //Manually compute Start and End IP Addresses
                        final String lanAddr = nvramInfo.getProperty(NVRAMInfo.LAN_IPADDR);
                        final String dhcpStart = nvramInfo.getProperty(NVRAMInfo.DHCP_START);

                        String dhcpStartIp = null;
                        if (dhcpStart != null && lanAddr != null) {
                            final List<String> lowAddressSplit = IP_ADDR_SPLITTER
                                    .splitToList(lanAddr);
                            if (lowAddressSplit != null && lowAddressSplit.size() >= 3) {
                                dhcpStartIp = String.format("%s.%s.%s.%s",
                                        lowAddressSplit.get(0),
                                        lowAddressSplit.get(1),
                                        lowAddressSplit.get(2),
                                        dhcpStart);
                                nvramInfo.setProperty(DHCP_START_IP, dhcpStartIp);
                            }
                        }

                        final String dhcpHostCountStr = nvramInfo.getProperty(NVRAMInfo.DHCP_NUM);
                        final String netmask = nvramInfo.getProperty(NVRAMInfo.LAN_NETMASK);
                        if (netmask != null) {
                            if (dhcpStartIp != null) {
                                final SubnetUtils subnetUtils = new SubnetUtils(dhcpStartIp, netmask);
                                final SubnetUtils.SubnetInfo subnetInfo = subnetUtils.getInfo();

                                if (dhcpHostCountStr != null) {
                                    try {
                                        final int dhcpHostCount = Integer.parseInt(dhcpHostCountStr);
                                        final String[] allAddresses = subnetInfo.getAllAddresses();
                                        //Compute number of hosts between low ip and dhcp_start ip
                                        if (allAddresses != null) {
                                            int distFromLowIpToStartIp = 0;
                                            for (String address : allAddresses) {
                                                if (address.equals(dhcpStartIp)) {
                                                    break;
                                                }
                                                distFromLowIpToStartIp++;
                                            }
                                            //Then get end address, starting from DHCP Start IP
                                            if (allAddresses.length >= distFromLowIpToStartIp + dhcpHostCount) {
                                                nvramInfo.setProperty(DHCP_END_IP, allAddresses[distFromLowIpToStartIp + dhcpHostCount - 1]);
                                            }
                                        }

                                    } catch (final NumberFormatException nfe) {
                                        //No worries
                                        nfe.printStackTrace();
                                    }
                                }
                            }
                        }
                    }

                    if (nvramInfo.isEmpty()) {
                        throw new DDWRTNoDataException("No Data!");
                    }

                    updateProgressBarViewSeparator(90);

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
    public void onLoadFinished(@NonNull Loader<NVRAMInfo> loader, @Nullable NVRAMInfo data) {

        try {
            //Set tiles
            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

            layout.findViewById(R.id.tile_status_lan_dhcp_status_loading_view)
                    .setVisibility(View.GONE);
            layout.findViewById(R.id.tile_status_lan_dhcp_status_gridLayout)
                    .setVisibility(View.VISIBLE);


            if (data == null) {
                data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
            }

            final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_lan_dhcp_status_error);

            final Exception exception = data.getException();

            if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

                if (exception == null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }

                //Server Status
                final TextView dhcpServerView = (TextView) this.layout.findViewById(R.id.tile_status_lan_dhcp_status_server);
                final String lanProto = data.getProperty(NVRAMInfo.LAN_PROTO);
                final String lanProtoTxt;
                if (lanProto == null) {
                    lanProtoTxt = "-";
                } else if ("dhcp".equalsIgnoreCase(lanProto)) {
                    lanProtoTxt = "Enabled";
                } else {
                    lanProtoTxt = "Disabled";
                }
                dhcpServerView.setText(lanProtoTxt);

                //Daemon
                final TextView dhcpDaemonView = (TextView) this.layout.findViewById(R.id.tile_status_lan_dhcp_status_daemon);
                final String dhcpDnsmasq = data.getProperty(NVRAMInfo.DHCP_DNSMASQ);
                final String dhcpDnsmasqTxt;
                if ("1".equalsIgnoreCase(dhcpDnsmasq)) {
                    dhcpDnsmasqTxt = "DNSMasq";
                } else {
                    dhcpDnsmasqTxt = "-";
                }
                dhcpDaemonView.setText(dhcpDnsmasqTxt);

                //Start IP
                ((TextView) this.layout.findViewById(R.id.tile_status_lan_dhcp_status_start_ip))
                        .setText(data.getProperty(DHCP_START_IP, "-"));

                //End IP
                ((TextView) this.layout.findViewById(R.id.tile_status_lan_dhcp_status_end_ip))
                        .setText(data.getProperty(DHCP_END_IP, "-"));

                final TextView clientLeaseView = (TextView) this.layout.findViewById(R.id.tile_status_lan_dhcp_status_client_lease_time);
                final String dhcpClientLeaseTime = data.getProperty(NVRAMInfo.DHCP_LEASE);
                clientLeaseView.setText(Strings.isNullOrEmpty(dhcpClientLeaseTime) ? "-" :
                        (dhcpClientLeaseTime + " min"));

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


            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished(): done loading!");
        } finally {
            mRefreshing.set(false);
            doneWithLoaderInstance(this, loader);
        }
    }

    @Nullable
    @Override
    protected OnClickIntent getOnclickIntent() {
        //TODO
        return null;
    }
}
