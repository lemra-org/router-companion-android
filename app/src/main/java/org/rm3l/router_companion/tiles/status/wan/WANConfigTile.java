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

package org.rm3l.router_companion.tiles.status.wan;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.cocosw.undobar.UndoBarController;
import com.crashlytics.android.Crashlytics;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;

import org.rm3l.router_companion.BuildConfig;
import org.rm3l.router_companion.R;
import org.rm3l.router_companion.actions.ActionManager;
import org.rm3l.router_companion.actions.DHCPClientRouterAction;
import org.rm3l.router_companion.actions.DHCPClientRouterAction.DHCPClientAction;
import org.rm3l.router_companion.actions.RouterAction;
import org.rm3l.router_companion.actions.RouterActionListener;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.resources.PublicIPInfo;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.service.tasks.PublicIPChangesServiceTask;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import de.keyboardsurfer.android.widget.crouton.Style;

import static android.text.TextUtils.isEmpty;
import static org.rm3l.router_companion.tiles.dashboard.network.NetworkTopologyMapTile.INTERNET_CONNECTIVITY_PUBLIC_IP;
import static org.rm3l.router_companion.RouterCompanionAppConstants.NOK;
import static org.rm3l.router_companion.RouterCompanionAppConstants.UNKNOWN;
import static org.rm3l.router_companion.utils.Utils.isDemoRouter;

public class WANConfigTile extends DDWRTTile<NVRAMInfo> implements PopupMenu.OnMenuItemClickListener {

    private static final String LOG_TAG = WANConfigTile.class.getSimpleName();
    public static final String REVERSE_DNS_PTR = "REVERSE_DNS_PTR";
    private long mLastSync;
    private AsyncTaskLoader<NVRAMInfo> mLoader;
    private final AtomicBoolean mDhcpActionRunning = new AtomicBoolean(false);

    private boolean checkActualInternetConnectivity = true;

    public WANConfigTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_status_wan_config, null);
        //Create Options Menu
        final ImageButton tileMenu = (ImageButton) layout.findViewById(R.id.tile_status_wan_config_menu);

        if (!ColorUtils.isThemeLight(mParentFragmentActivity)) {
            //Set menu background to white
            tileMenu.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
        }

        tileMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final PopupMenu popup = new PopupMenu(mParentFragmentActivity, v);
                popup.setOnMenuItemClickListener(WANConfigTile.this);
                final MenuInflater inflater = popup.getMenuInflater();
                final Menu menu = popup.getMenu();
                inflater.inflate(R.menu.tile_wan_config_options, menu);
                popup.show();
            }
        });

    }

    @Override
    public int getTileHeaderViewId() {
        return R.id.tile_status_wan_config_hdr;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_status_wan_config_title;
    }

    @Nullable
    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param id   The ID whose loader is to be created.
     * @param args Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Override
    protected Loader<NVRAMInfo> getLoader(final int id, final Bundle args) {
        mLoader = new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {

                try {
                    if (mParentFragmentPreferences != null) {
                        checkActualInternetConnectivity = mParentFragmentPreferences
                                .getBoolean(RouterCompanionAppConstants.OVERVIEW_NTM_CHECK_ACTUAL_INTERNET_CONNECTIVITY_PREF, true);
                    }

                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Init background loader for " + WANConfigTile.class + ": routerInfo=" +
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
                        if (isDemoRouter(mRouter)) {
                            final String[] possibleWanProtos = new String[] {
                                    "ppoe",
                                    "3g",
                                    "heartbeat",
                                    "disabled",
                                    "dhcp"
                            };
                            nvramInfoTmp = new NVRAMInfo()
                                    .setProperty(NVRAMInfo.WAN_PROTO,
                                            possibleWanProtos[new Random().nextInt(possibleWanProtos.length)])
                                    .setProperty(NVRAMInfo.WAN_3_G_SIGNAL, "-10dBm")
                                    .setProperty(NVRAMInfo.WAN_HWADDR, "wa:nm:ac:hw:ad:dr")
                                    .setProperty(NVRAMInfo.WAN_LEASE, "14400")
                                    .setProperty(NVRAMInfo.WAN_IPADDR, "10.11.12.13")
                                    .setProperty(NVRAMInfo.WAN_NETMASK, "255.0.0.0")
                                    .setProperty(NVRAMInfo.WAN_GATEWAY, "10.0.0.254")
                                    .setProperty(NVRAMInfo.WAN_GET_DNS, "8.8.8.8")
                                    .setProperty(NVRAMInfo.WAN_DNS, "8.8.4.4");

                        } else {
                            nvramInfoTmp = SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity, mRouter,
                                    mGlobalPreferences, NVRAMInfo.WAN_PROTO,
                                    NVRAMInfo.WAN_3_G_SIGNAL,
                                    NVRAMInfo.WAN_HWADDR,
                                    NVRAMInfo.WAN_LEASE,
                                    NVRAMInfo.WAN_IPADDR,
                                    NVRAMInfo.WAN_NETMASK,
                                    NVRAMInfo.WAN_GATEWAY,
                                    NVRAMInfo.WAN_GET_DNS,
                                    NVRAMInfo.WAN_DNS);
                        }
                        updateProgressBarViewSeparator(50);
                    } finally {
                        if (nvramInfoTmp != null) {
                            nvramInfo.putAll(nvramInfoTmp);
                        }

                        //Connection Uptime is stored in /tmp/.wanuptime and sys uptime from /proc/uptime
                        final String[] uptimes;

                        if (isDemoRouter(mRouter)) {
                            uptimes = new String[2];
                            uptimes[0] = "14.030000";
                            uptimes[1] = "256532.59 452454.12";
                        } else {
                            uptimes = SSHUtils
                                    .getManualProperty(mParentFragmentActivity,
                                            mRouter, mGlobalPreferences,
                                            "cat /tmp/.wanuptime; echo; cat /proc/uptime");
                        }
                        updateProgressBarViewSeparator(65);
                        if (uptimes != null && uptimes.length > 1) {
                            final String wanUptimeStr = uptimes[0];

                            /*
                             * /proc/uptime Shows how long the system has been on since it was last restarted.
                             * The first number is the total number of seconds the system has been up.
                             * The second number is how much of that time the machine has spent idle, in seconds.
                             * On multi core systems (and some linux versions), the second number is the sum of
                             * the idle time accumulated by each CPU.
                             */
                            final List<String> sysUptimeSplitList = Splitter.on(" ").
                                    trimResults().omitEmptyStrings().splitToList(uptimes[1]);
                            if (sysUptimeSplitList != null && !sysUptimeSplitList.isEmpty()) {
                                try {
                                    final float uptime = Float.valueOf(sysUptimeSplitList.get(0)) - Float.valueOf(wanUptimeStr);
                                    final int days = (int) uptime / (60 * 60 * 24);

                                    String wanConnectionUptimeStr = "";
                                    if (days > 0) {
                                        wanConnectionUptimeStr += String.format("%d day%s, ",
                                                days, (days == 1 ? "" : "s"));
                                    }
                                    final int minutes = (int) uptime / 60;
                                    wanConnectionUptimeStr += String.format("%d:%02d:%02d",
                                            (minutes / 60) % 24, minutes % 60, (int) uptime % 60);

                                    nvramInfo.setProperty(NVRAMInfo.WAN_CONNECTION_UPTIME, wanConnectionUptimeStr);

                                } catch (final NumberFormatException nfe) {
                                    nfe.printStackTrace();
                                    //No Worries - WAN Uptime will be marked as "-"
                                }
                            }
                        }

                        updateProgressBarViewSeparator(75);
                        if (checkActualInternetConnectivity) {
                            final CharSequence applicationName = Utils.getApplicationName(mParentFragmentActivity);
                            try {

                                if (isDemoRouter(mRouter)) {
                                    final long nbRunsLoaderModulo = (nbRunsLoader % 5);
                                    if (nbRunsLoaderModulo == 0) {
                                        //nbRunsLoader = 5k
                                        final int p1 = 1 + new Random().nextInt(252);
                                        final int p2 = 1 + new Random().nextInt(252);
                                        final String randomPublicIp = "52.64." + p1 + "." + p2;
                                        nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP,
                                                randomPublicIp);
                                        nvramInfo.setProperty(REVERSE_DNS_PTR,
                                                p2 + "." + p1 + ".64.52.in-addr.arpa");

                                    } else if (nbRunsLoaderModulo == 1) {
                                        //nbRunsLoader = 5k + 1
                                        nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, NOK);
                                    } else if (nbRunsLoaderModulo == 2) {
                                        //nbRunsLoader = 5k + 2
                                        nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, UNKNOWN);
                                    }
                                } else {
                                    //Check actual connections to the outside from the router
                                    final String[] wanPublicIpCmdStatus = SSHUtils.getManualProperty(mParentFragmentActivity,
                                            mRouter, mGlobalPreferences,
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
                                    Crashlytics.log(Log.DEBUG, LOG_TAG, "wanPublicIpCmdStatus: " + Arrays.toString(wanPublicIpCmdStatus));
                                    if (wanPublicIpCmdStatus == null || wanPublicIpCmdStatus.length == 0) {
                                        nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, NOK);
                                    } else {
                                        final String wanPublicIp = wanPublicIpCmdStatus[wanPublicIpCmdStatus.length - 1]
                                                .trim();
                                        if (Patterns.IP_ADDRESS.matcher(wanPublicIp).matches()) {
                                            nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, wanPublicIp);

                                            PublicIPChangesServiceTask.buildNotificationIfNeeded(mParentFragmentActivity,
                                                    mRouter, mParentFragmentPreferences,
                                                    wanPublicIpCmdStatus,
                                                    nvramInfo.getProperty(NVRAMInfo.WAN_IPADDR), null);

                                        } else {
                                            nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, NOK);
                                        }
                                    }
                                }
                                updateProgressBarViewSeparator(80);
                            } catch (final Exception e) {
                                e.printStackTrace();
                                nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, UNKNOWN);
                            } finally {

                                runBgServiceTaskAsync();

                                //Get Reverse DNS Record (PTR) as well
                                try {
                                    final String[] revDnsCmdStatus = SSHUtils.getManualProperty(mParentFragmentActivity,
                                            mRouter, mGlobalPreferences,
//                                        "echo -e \"GET / HTTP/1.1\\r\\nHost:icanhazip.com\\r\\nUser-Agent:DD-WRT Companion/3.3.0\\r\\n\" | nc icanhazptr.com 80"
                                            String.format("echo -e \"" +
                                                            "GET / HTTP/1.1\\r\\n" +
                                                            "Host:%s\\r\\n" +
                                                            "User-Agent:%s/%s\\r\\n\" " +
                                                            "| /usr/bin/nc %s %d",
                                                    PublicIPInfo.ICANHAZPTR_HOST,
                                                    applicationName != null ? applicationName : BuildConfig.APPLICATION_ID,
                                                    BuildConfig.VERSION_NAME,
                                                    PublicIPInfo.ICANHAZPTR_HOST,
                                                    PublicIPInfo.ICANHAZPTR_PORT));

                                    Crashlytics.log(Log.DEBUG, LOG_TAG, "revDnsCmdStatus: " + Arrays.toString(revDnsCmdStatus));

                                    if (revDnsCmdStatus != null && revDnsCmdStatus.length > 0) {
                                        nvramInfo.setProperty(REVERSE_DNS_PTR, revDnsCmdStatus[revDnsCmdStatus.length - 1]);
                                    }
                                    updateProgressBarViewSeparator(85);
                                } catch (final Exception e) {
                                    e.printStackTrace();
                                }

                            }
                        }
                    }
                    updateProgressBarViewSeparator(95);

                    if (nvramInfo.isEmpty()) {
                        throw new DDWRTNoDataException("No Data!");
                    }

                    return nvramInfo;

                } catch (@NonNull final Exception e) {
                    e.printStackTrace();
                    return new NVRAMInfo().setException(e);
                }
            }
        };
        return mLoader;
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

            layout.findViewById(R.id.tile_status_wan_config_loading_view)
                    .setVisibility(View.GONE);
            layout.findViewById(R.id.tile_status_wan_config_gridLayout)
                    .setVisibility(View.VISIBLE);

            final ImageButton tileMenu = (ImageButton) layout.findViewById(R.id.tile_status_wan_config_menu);

            if (data == null) {
                data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
            }

            if (!ColorUtils.isThemeLight(mParentFragmentActivity)) {
                //Set menu background to white
                tileMenu.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
            }

            final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_error);

            final Exception exception = data.getException();

            if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

                if (exception == null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }

                //Connection Type
                final TextView wanConnTypeView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_connection_type);
                final String wanProto = data.getProperty(NVRAMInfo.WAN_PROTO, "-");
                final String wanConnectionTypeManual;

                if ("ppoe".equalsIgnoreCase(wanProto)) {
                    wanConnectionTypeManual = "PPoE";
                } else if ("3g".equalsIgnoreCase(wanProto)) {
                    wanConnectionTypeManual = "3G/UMTS";

                    //Unhide 3g signal text
                    final TextView wan3gSignalFieldView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_3g_signal_field);
                    final TextView wan3gSignalView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_3g_signal);
                    wan3gSignalFieldView.setVisibility(View.VISIBLE);
                    wan3gSignalView.setVisibility(View.VISIBLE);
                    wan3gSignalView.setText(data.getProperty(NVRAMInfo.WAN_3_G_SIGNAL, "-"));

                } else if ("heartbeat".equalsIgnoreCase(wanProto)) {
                    wanConnectionTypeManual = "Heartbeat Signal";
                } else if ("disabled".equalsIgnoreCase(wanProto)) {
                    wanConnectionTypeManual = "*Disabled*";
                } else {
                    wanConnectionTypeManual = (wanProto != null ? wanProto.toUpperCase() : "-");
                }

                wanConnTypeView.setText(wanConnectionTypeManual);

                //Connection Uptime
                final TextView wanUptimeView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_connection_uptime);
                wanUptimeView.setText(data.getProperty(NVRAMInfo.WAN_CONNECTION_UPTIME, "-"));

                //WAN MAC
                final TextView wanMacView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_wan_mac);
                wanMacView.setText(data.getProperty(NVRAMInfo.WAN_HWADDR, "-"));

                //WAN IP
                final String wanIpText = data.getProperty(NVRAMInfo.WAN_IPADDR, "-");
                final TextView wanIPView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_wan_ip);
                wanIPView.setText(wanIpText);

                //Subnet
                final TextView wanSubnetView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_subnet_mask);
                wanSubnetView.setText(data.getProperty(NVRAMInfo.WAN_NETMASK, "-"));

                //Gateway
                final TextView wanGatewayView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_subnet_gateway);
                wanGatewayView.setText(data.getProperty(NVRAMInfo.WAN_GATEWAY, "-"));

                //Public IP and Reverse DNS
                final TextView internetIpTitle = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_internet_ip_title);
                final TextView internetIpTextView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_internet_ip);
                final TextView rDnsTitle = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_rdns_title);
                final TextView rDnsTextView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_rdns);
                if (!checkActualInternetConnectivity) {
                    internetIpTitle.setVisibility(View.GONE);
                    internetIpTextView.setVisibility(View.GONE);
                    rDnsTitle.setVisibility(View.GONE);
                    rDnsTextView.setVisibility(View.GONE);
                } else {
                    final String publicIp = data.getProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, null);
                    if (publicIp != null &&
                            !(UNKNOWN.equals(publicIp) || NOK.equals(publicIp))) {
                        internetIpTextView.setText(publicIp);
                    } else {
                        internetIpTextView.setText("-");
                    }
                    if (publicIp != null && publicIp.equalsIgnoreCase(wanIpText)) {
                        //Hide public IP in this case
                        internetIpTitle.setVisibility(View.GONE);
                        internetIpTextView.setVisibility(View.GONE);
                    }
                    final String revDnsPtr = data.getProperty(REVERSE_DNS_PTR);
                    if (Strings.isNullOrEmpty(revDnsPtr)) {
                        rDnsTitle.setVisibility(View.GONE);
                        rDnsTextView.setVisibility(View.GONE);
                    } else {
                        rDnsTextView.setText(revDnsPtr);
                    }
                }

                //DNS
                final TextView wanDNSView = (TextView) this.layout.findViewById(R.id.tile_status_wan_config_dns);
                final String wanDns = data.getProperty(NVRAMInfo.WAN_DNS);
                final String wanGetDns = data.getProperty(NVRAMInfo.WAN_GET_DNS);
                final String property = (!isEmpty(wanDns) ? wanDns : (!isEmpty(wanGetDns) ? wanGetDns : "-"));
                wanDNSView.setText(property.replaceAll(" ", ", "));

                if ("dhcp".equalsIgnoreCase(wanProto)) {
                    tileMenu.setVisibility(View.VISIBLE);

                    tileMenu.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final PopupMenu popup = new PopupMenu(mParentFragmentActivity, v);
                            popup.setOnMenuItemClickListener(WANConfigTile.this);
                            final MenuInflater inflater = popup.getMenuInflater();
                            final Menu menu = popup.getMenu();
                            inflater.inflate(R.menu.tile_wan_config_options, menu);
                            popup.show();
                        }
                    });
                } else {
                    //Only DHCP Actions are supported for now - so hide if WANProto != DHCP
                    tileMenu.setVisibility(View.GONE);
                }

                //Update last sync
                final RelativeTimeTextView lastSyncView = (RelativeTimeTextView) layout.findViewById(R.id.tile_last_sync);
                lastSyncView.setReferenceTime(mLastSync);
                lastSyncView.setPrefix("Last sync: ");
            }

            if (exception != null && !(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {
                tileMenu.setVisibility(View.GONE);
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

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        final int itemId = menuItem.getItemId();
        switch (itemId) {
            case R.id.tile_wan_config_dhcp_release:
            case R.id.tile_wan_config_dhcp_renew:
                if (mDhcpActionRunning.get()) {
                    Utils.displayMessage(mParentFragmentActivity,
                            "Action already in progress. Please wait a few seconds...",
                            Style.INFO);
                    return true;
                }

                final boolean renew = (itemId == R.id.tile_wan_config_dhcp_renew);
                final DHCPClientAction dhcpClientRouterAction =
                        (renew ? DHCPClientAction.RENEW :
                                DHCPClientAction.RELEASE);

                mDhcpActionRunning.set(true);
                layout.findViewById(R.id.tile_status_wan_config_menu)
                        .setEnabled(false);

                new UndoBarController.UndoBar(mParentFragmentActivity)
                        .message(String.format("WAN DHCP Lease will be %s on '%s' (%s). ",
                                renew ? "renewed" : "released",
                                mRouter.getDisplayName(),
                                mRouter.getRemoteIpAddress()))
                        .listener(new UndoBarController.AdvancedUndoListener() {
                                      @Override
                                      public void onHide(@Nullable Parcelable parcelable) {
                                          Utils.displayMessage(mParentFragmentActivity,
                                                  String.format("%s WAN DHCP Lease...",
                                                          renew ? "Renewing" : "Releasing"),
                                                  Style.INFO);
                                          ActionManager.runTasks(
                                            new DHCPClientRouterAction(
                                                  mRouter,
                                                  mParentFragmentActivity,
                                                  new RouterActionListener() {
                                                      @Override
                                                      public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull final Router router, Object returnData) {
                                                          mParentFragmentActivity.runOnUiThread(new Runnable() {
                                                              @Override
                                                              public void run() {

                                                                  try {
                                                                      Utils.displayMessage(mParentFragmentActivity,
                                                                              String.format("WAN DHCP Lease %s successfully on host '%s' (%s). ",
                                                                                      renew ? "renewed" : "released",
                                                                                      router.getDisplayName(),
                                                                                      router.getRemoteIpAddress()),
                                                                              Style.CONFIRM);
                                                                  } finally {
                                                                      mDhcpActionRunning.set(false);
                                                                      if (mLoader != null) {
                                                                          //Reload everything right away
                                                                          doneWithLoaderInstance(WANConfigTile.this,
                                                                                  mLoader,
                                                                                  1l);
                                                                      }
                                                                      layout.findViewById(R.id.tile_status_wan_config_menu)
                                                                              .setEnabled(true);
                                                                  }
                                                              }

                                                          });
                                                      }

                                                      @Override
                                                      public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull final Router router, @Nullable final Exception exception) {
                                                          mParentFragmentActivity.runOnUiThread(new Runnable() {
                                                              @Override
                                                              public void run() {
                                                                  try {
                                                                      Utils.displayMessage(mParentFragmentActivity,
                                                                              String.format("Error while trying to %s WAN DHCP Lease on '%s' (%s): %s",
                                                                                      renew ? "renew" : "release",
                                                                                      router.getDisplayName(),
                                                                                      router.getRemoteIpAddress(),
                                                                                      Utils.handleException(exception).first),
                                                                              Style.ALERT);
                                                                  } finally {
                                                                      mDhcpActionRunning.set(false);
                                                                      layout.findViewById(R.id.tile_status_wan_config_menu)
                                                                              .setEnabled(true);
                                                                  }
                                                              }
                                                          });
                                                      }
                                                  },
                                                  mGlobalPreferences,
                                                  dhcpClientRouterAction)
                                          );
                                      }

                                      @Override
                                      public void onClear(@NonNull Parcelable[] parcelables) {
                                          mParentFragmentActivity.runOnUiThread(new Runnable() {
                                              @Override
                                              public void run() {
                                                  try {
                                                      layout.findViewById(R.id.tile_status_wan_config_menu)
                                                              .setEnabled(true);
                                                  } finally {
                                                      mDhcpActionRunning.set(false);
                                                  }
                                              }
                                          });
                                      }

                                      @Override
                                      public void onUndo(@Nullable Parcelable parcelable) {
                                          mParentFragmentActivity.runOnUiThread(new Runnable() {
                                              @Override
                                              public void run() {
                                                  try {
                                                      layout.findViewById(R.id.tile_status_wan_config_menu)
                                                              .setEnabled(true);
                                                  } finally {
                                                      mDhcpActionRunning.set(false);
                                                  }
                                              }
                                          });
                                      }
                                  })
                        .token(new Bundle())
                        .show();
                return true;
            default:
                break;
        }
        return false;
    }
}
