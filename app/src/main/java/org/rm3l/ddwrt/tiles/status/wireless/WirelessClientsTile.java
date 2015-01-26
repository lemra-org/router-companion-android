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
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.util.LruCache;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.cocosw.undobar.UndoBarController;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.actions.RouterAction;
import org.rm3l.ddwrt.actions.RouterActionListener;
import org.rm3l.ddwrt.actions.WakeOnLANRouterAction;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.resources.ClientDevices;
import org.rm3l.ddwrt.resources.Device;
import org.rm3l.ddwrt.resources.MACOUIVendor;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.status.bandwidth.BandwidthMonitoringTile;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;
import org.rm3l.ddwrt.widgets.NetworkTrafficView;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;
import static org.rm3l.ddwrt.DDWRTMainActivity.ROUTER_ACTION;
import static org.rm3l.ddwrt.tiles.status.bandwidth.BandwidthMonitoringTile.BandwidthMonitoringIfaceData;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_NAME;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_PATH_REMOTE;
import static org.rm3l.ddwrt.utils.Utils.getThemeBackgroundColor;
import static org.rm3l.ddwrt.utils.Utils.isThemeLight;

/**
 *
 */
public class WirelessClientsTile extends DDWRTTile<ClientDevices> {

    public static final Comparator<Device> COMPARATOR = new Comparator<Device>() {
        @Override
        public int compare(Device device, Device device2) {
            return Ordering.natural().compare(nullToEmpty(device.getName()).toLowerCase(),
                    nullToEmpty(device2.getName()).toLowerCase());
        }
    };
    private static final String LOG_TAG = WirelessClientsTile.class.getSimpleName();
    private static final int MAX_CLIENTS_TO_SHOW_IN_TILE = 199;
    private static final String PER_IP_MONITORING_IP_TABLES_CHAIN = "DDWRTCompanion";
    public static final String USAGE_DB = "/tmp/." + PER_IP_MONITORING_IP_TABLES_CHAIN + "_usage.db";
    public static final String USAGE_DB_OUT = "/tmp/." + PER_IP_MONITORING_IP_TABLES_CHAIN + "_usage.db.out";

    private final Random randomColorGen = new Random();
    private final Map<String, Integer> colorsCache = new HashMap<>();
    //Generate a random string, to use as discriminator for determining dhcp clients
    private final String MAP_KEYWORD;
    private final LruCache<String, MACOUIVendor> mMacOuiVendorLookupCache;
    @NotNull
    private final Map<String, BandwidthMonitoringIfaceData> bandwidthMonitoringIfaceDataPerDevice =
            Maps.newConcurrentMap();
    private String mCurrentIpAddress;
    private String mCurrentMacAddress;
    private String[] activeClients;
    private String[] activeDhcpLeases;

    @Nullable
    private List<String> broadcastAddresses;

    private File wrtbwmonScriptPath;

    public WirelessClientsTile(@NotNull SherlockFragment parentFragment, @NotNull Bundle arguments, Router router) {
        super(parentFragment, arguments, router, R.layout.tile_status_wireless_clients, R.id.tile_status_wireless_clients_togglebutton);
        MAP_KEYWORD = WirelessClientsTile.class.getSimpleName() + UUID.randomUUID().toString();

        mMacOuiVendorLookupCache = new LruCache<String, MACOUIVendor>(MAX_CLIENTS_TO_SHOW_IN_TILE) {

            @Override
            protected void entryRemoved(boolean evicted, String key, MACOUIVendor oldValue, MACOUIVendor newValue) {
                super.entryRemoved(evicted, key, oldValue, newValue);
                Log.d(LOG_TAG, "entryRemoved(" + evicted + ", " + key + ")");
            }

            @Override
            protected MACOUIVendor create(final String macAddr) {
                if (isNullOrEmpty(macAddr)) {
                    return null;
                }
                //Get to MAC OUI Vendor Lookup API
                try {
                    final String url = String.format("%s/%s",
                            MACOUIVendor.MAC_VENDOR_LOOKUP_API_PREFIX, macAddr.toUpperCase());
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
                            final MACOUIVendor[] macouiVendors = gson.fromJson(reader, MACOUIVendor[].class);
                            Log.d(LOG_TAG, "--> Result of GET " + url + ": " + Arrays.toString(macouiVendors));
                            if (macouiVendors == null || macouiVendors.length == 0) {
                                //Returning null so we can try again later
                                return null;
                            }
                            return macouiVendors[0];

                        } finally {
                            Closeables.closeQuietly(content);
                            entity.consumeContent();
                        }
                    } else {
                        Log.e(LOG_TAG, "<--- Server responded with status code: " + statusCode);
                        if (statusCode == 204) {
                            //No Content found on the remote server - no need to retry later
                            return new MACOUIVendor();
                        }
                    }

                } catch (final Exception e) {
                    e.printStackTrace();
                }

                return null;
            }
        };

        //Create Options Menu
//        final ImageButton tileMenu = (ImageButton) layout.findViewById(R.id.tile_status_wireless_clients_menu);
//        if (!isThemeLight(mParentFragmentActivity, mRouter.getUuid())) {
//            //Set menu background to white
//            tileMenu.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
//        }
//        tileMenu.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                final PopupMenu popup = new PopupMenu(mParentFragmentActivity, v);
//
//            }
//        });
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_status_wireless_clients_title;
    }

    @Nullable
    @Override
    protected Loader<ClientDevices> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<ClientDevices>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public ClientDevices loadInBackground() {

                Log.d(LOG_TAG, "Init background loader for " + WirelessClientsTile.class + ": routerInfo=" +
                        mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                //Determine broadcast address at each run (because that might change if connected to another network)
                try {
                    final WifiManager wifiManager = (WifiManager) mParentFragmentActivity.getSystemService(Context.WIFI_SERVICE);
                    final WifiInfo connectionInfo = wifiManager.getConnectionInfo();

                    mCurrentIpAddress = Utils.intToIp(connectionInfo.getIpAddress());
                    mCurrentMacAddress = connectionInfo.getMacAddress();
                } catch (@NotNull final Exception e) {
                    e.printStackTrace();
                    //No worries
                }

                if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                    //Skip run
                    Log.d(LOG_TAG, "Skip loader run");
                    return new ClientDevices().setException(new DDWRTTileAutoRefreshNotAllowedException());
                }
                nbRunsLoader++;

                final ClientDevices devices = new ClientDevices();

                broadcastAddresses = Lists.newArrayList();

                try {

                    //Get Broadcast Addresses (for WOL)
                    try {
                        final String[] wanBroadcast = SSHUtils.getManualProperty(mRouter, mGlobalPreferences,
                                "ifconfig `nvram get wan_iface` | grep Bcast | awk -F'Bcast:' '{print $2}' | awk -F'Mask:' '{print $1}'");
                        if (wanBroadcast != null && wanBroadcast.length > 0) {
                            final String wanBcast = wanBroadcast[0];
                            if (wanBcast != null) {
                                //noinspection ConstantConditions
                                broadcastAddresses.add(wanBcast.trim());
                            }
                        }
                        final String[] lanBroadcast = SSHUtils.getManualProperty(mRouter, mGlobalPreferences,
                                "ifconfig `nvram get lan_ifname` | grep Bcast | awk -F'Bcast:' '{print $2}' | awk -F'Mask:' '{print $1}'");
                        if (lanBroadcast != null && lanBroadcast.length > 0) {
                            final String lanBcast = lanBroadcast[0];
                            if (lanBcast != null) {
                                //noinspection ConstantConditions
                                broadcastAddresses.add(lanBcast.trim());
                            }
                        }
                    } catch (final Exception e) {
                        //No worries
                        e.printStackTrace();
                    }

                    //Active clients
                    activeClients = SSHUtils.getManualProperty(mRouter, mGlobalPreferences, "arp -a");
                    if (activeClients != null) {
                        devices.setActiveClientsNum(activeClients.length);
                    }

                    //Active DHCP Leases
                    activeDhcpLeases = SSHUtils.getManualProperty(mRouter, mGlobalPreferences, "cat /tmp/dnsmasq.leases");
                    if (activeDhcpLeases != null) {
                        devices.setActiveDhcpLeasesNum(activeDhcpLeases.length);
                    }

                    @Nullable final String[] output = SSHUtils.getManualProperty(mRouter,
                            mGlobalPreferences, "grep dhcp-host /tmp/dnsmasq.conf | sed 's/.*=//' | awk -F , '{print \"" +
                                    MAP_KEYWORD +
                                    "\",$1,$3 ,$2}'",
                            "awk '{print \"" +
                                    MAP_KEYWORD +
                                    "\",$2,$3,$4}' /tmp/dnsmasq.leases",
                            "awk 'NR>1{print \"" +
                                    MAP_KEYWORD +
                                    "\",$4,$1,\"*\"}' /proc/net/arp",
                            "echo done");

                    Log.d(LOG_TAG, "output: " + Arrays.toString(output));

                    if (output == null) {
                        return devices;
                    }

                    final Map<String, Device> macToDevice = Maps.newHashMap();
                    final Multimap<String, Device> macToDeviceOutput = HashMultimap.create();

                    for (final String stdoutLine : output) {
                        if ("done".equals(stdoutLine)) {
                            break;
                        }
                        final List<String> as = Splitter.on(" ").splitToList(stdoutLine);
                        if (as != null && as.size() >= 4 && MAP_KEYWORD.equals(as.get(0))) {
                            final String macAddress = as.get(1);
                            if (isNullOrEmpty(macAddress) || "00:00:00:00:00:00".equals(macAddress)) {
                                //Skip clients with incomplete ARP set-up
                                continue;
                            }
                            final Device device = new Device(macAddress);

                            if (activeClients != null) {
                                for (final String activeClient : activeClients) {
                                    if (StringUtils.containsIgnoreCase(activeClient, macAddress)) {
                                        device.setActive(true);
                                        break;
                                    }
                                }
                            }

                            device.setIpAddress(as.get(2));

                            final String systemName = as.get(3);
                            if (!"*".equals(systemName)) {
                                device.setSystemName(systemName);
                            }

                            //Alias from SharedPreferences
                            if (mParentFragmentPreferences != null) {
                                final String deviceAlias = mParentFragmentPreferences.getString(macAddress, null);
                                if (!isNullOrEmpty(deviceAlias)) {
                                    device.setAlias(deviceAlias);
                                }
                            }

                            device.setMacouiVendorDetails(mMacOuiVendorLookupCache.get(macAddress));

                            macToDeviceOutput.put(macAddress, device);
                        }
                    }

                    for (final Map.Entry<String, Collection<Device>> deviceEntry : macToDeviceOutput.asMap().entrySet()) {
                        final String macAddr = deviceEntry.getKey();
                        final Collection<Device> deviceCollection = deviceEntry.getValue();
                        for (final Device device : deviceCollection) {
                            //Consider the one that has a Name, if any
                            if (!isNullOrEmpty(device.getSystemName())) {
                                macToDevice.put(macAddr, device);
                                break;
                            }
                        }
                        if (deviceCollection.isEmpty() || macToDevice.containsKey(macAddr)) {
                            continue;
                        }

                        macToDevice.put(macAddr,
                                deviceCollection.iterator().next());
                    }

                    /** http://www.dd-wrt.com/phpBB2/viewtopic.php?t=75275 */

                    //Copy wrtbwmon file to remote host (/tmp/)
                    Log.d(LOG_TAG, "[COPY] Copying monitoring script to remote router...");
                    wrtbwmonScriptPath = new File(mParentFragmentActivity.getCacheDir(), WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_NAME);

                    FileUtils.copyInputStreamToFile(mParentFragmentActivity.getResources().openRawResource(R.raw.wrtbwmon_ddwrtcompanion),
                            wrtbwmonScriptPath);
                    SSHUtils.scpTo(mRouter, mGlobalPreferences,
                            wrtbwmonScriptPath.getAbsolutePath(),
                            WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_PATH_REMOTE);

                    //Run Setup (does not matter if already done)
                    Log.d(LOG_TAG, "[EXEC] Running per-IP bandwidth monitoring...");

                    SSHUtils.runCommands(mGlobalPreferences, mRouter,
                            "chmod 700 " + WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_PATH_REMOTE,
                            WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_PATH_REMOTE + " setup",
                            WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_PATH_REMOTE + " read",
                            "sleep 3",
                            WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_PATH_REMOTE + " update " + USAGE_DB,
                            WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_PATH_REMOTE + " publish-raw " + USAGE_DB + " " + USAGE_DB_OUT);

                    final String[] usageDbOutLines = SSHUtils.getManualProperty(mRouter, mGlobalPreferences,
                            "cat " + USAGE_DB_OUT,
                            "rm -f " + USAGE_DB_OUT);

                    if (usageDbOutLines != null) {
                        for (final String usageDbOutLine : usageDbOutLines) {
                            if (isNullOrEmpty(usageDbOutLine)) {
                                continue;
                            }
                            final List<String> splitToList = Splitter.on(",").omitEmptyStrings().splitToList(usageDbOutLine);
                            if (splitToList == null || splitToList.size() < 6) {
                                Log.w(LOG_TAG, "Line split should have more than 6 elements: " + splitToList);
                                continue;
                            }
                            final String macAddress = splitToList.get(0);
                            if (isNullOrEmpty(macAddress)) {
                                continue;
                            }
                            final Device device = macToDevice.get(macAddress.trim());
                            if (device == null) {
                                continue;
                            }
                            if (!bandwidthMonitoringIfaceDataPerDevice.containsKey(macAddress)) {
                                bandwidthMonitoringIfaceDataPerDevice.put(macAddress, new BandwidthMonitoringIfaceData());
                            }
                            final BandwidthMonitoringIfaceData bandwidthMonitoringIfaceData =
                                    bandwidthMonitoringIfaceDataPerDevice.get(macAddress);

                            try {
                                device.setRxTotal(Double.parseDouble(splitToList.get(2)) * 1024);
                            } catch (final NumberFormatException nfe) {
                                nfe.printStackTrace();
                                //no worries
                            }
                            try {
                                device.setTxTotal(Double.parseDouble(splitToList.get(3)) * 1024);
                            } catch (final NumberFormatException nfe) {
                                nfe.printStackTrace();
                                //no worries
                            }

                            try {
                                final long lastSeen = Long.parseLong(splitToList.get(1)) * 1000l;
                                final double rxRate = Double.parseDouble(splitToList.get(4)) * 1024;
                                final double txRate = Double.parseDouble(splitToList.get(5)) * 1024;
                                device.setLastSeen(lastSeen);
                                device.setRxRate(rxRate);
                                device.setTxRate(txRate);
                                bandwidthMonitoringIfaceData.addData("IN",
                                        new BandwidthMonitoringTile.DataPoint(lastSeen, rxRate));
                                bandwidthMonitoringIfaceData.addData("OUT",
                                        new BandwidthMonitoringTile.DataPoint(lastSeen, txRate));
                            } catch (final NumberFormatException nfe) {
                                nfe.printStackTrace();
                                //no worries
                            }

                        }
                    }

                    //TODO Backup usage DB (and maybe back it to external storage (scpFrom))
                    try {
                        SSHUtils.scpFrom(mRouter, mGlobalPreferences,
                                USAGE_DB,
                                new File(mParentFragmentActivity.getExternalFilesDir(null),
                                        "usage_" + mRouter.getUuid()).getAbsolutePath());
                    } catch (final Exception e) {
                        e.printStackTrace();
                        //No worries
                    } finally {
                        //Final operation
                        for (final Device device : macToDevice.values()) {
                            devices.addDevice(device);
                        }
                    }

                    return devices;

                } catch (@NotNull final Exception e) {
                    Log.e(LOG_TAG, e.getMessage() + ": " + Throwables.getStackTraceAsString(e));
                    return new ClientDevices().setException(e);
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
    public void onLoadFinished(Loader<ClientDevices> loader, ClientDevices data) {
        Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

        layout.findViewById(R.id.tile_status_wireless_clients_loading_view)
                .setVisibility(View.GONE);
        layout.findViewById(R.id.tile_status_wireless_clients_layout_list_container)
                .setVisibility(View.VISIBLE);
        layout.findViewById(R.id.tile_status_wireless_clients_togglebutton_container)
                .setVisibility(View.VISIBLE);

        //noinspection ThrowableResultOfMethodCallIgnored
        if (data == null ||
                (data.getDevices().isEmpty() &&
                        !(data.getException() instanceof DDWRTTileAutoRefreshNotAllowedException))) {
            if (data == null) {
                data = new ClientDevices().setException(new DDWRTNoDataException("No Data!"));
            }
        }

        @NotNull final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_clients_error);

        @Nullable final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

            if (exception == null) {
                errorPlaceHolderView.setVisibility(View.GONE);
            }

            final GridLayout clientsContainer = (GridLayout) this.layout.findViewById(R.id.tile_status_wireless_clients_layout_list_container);
            clientsContainer.removeAllViews();

            final Resources resources = mParentFragmentActivity.getResources();
            clientsContainer.setBackgroundColor(resources.getColor(android.R.color.transparent));

            //Number of Active Clients
            final int numActiveClients = data.getActiveClientsNum();
            ((TextView) layout.findViewById(R.id.tile_status_wireless_clients_active_clients_num))
                    .setText(numActiveClients >= 0 ? String.valueOf(numActiveClients) : "-");

            //Number of Active DHCP Leases
            final int numActiveDhcpLeases = data.getActiveDhcpLeasesNum();
            ((TextView) layout.findViewById(R.id.tile_status_wireless_clients_active_dhcp_leases_num))
                    .setText(numActiveDhcpLeases >= 0 ? String.valueOf(numActiveDhcpLeases) : "-");

            final Set<Device> devices = data.getDevices(MAX_CLIENTS_TO_SHOW_IN_TILE);
            final int themeBackgroundColor = getThemeBackgroundColor(mParentFragmentActivity, mRouter.getUuid());
            final boolean isThemeLight = isThemeLight(mParentFragmentActivity, mRouter.getUuid());

            final String expandedClientsPrefKey = \"fake-key\";

            Set<String> expandedClients;
            for (final Device device : devices) {

                expandedClients = mParentFragmentPreferences.getStringSet(expandedClientsPrefKey, null);
                if (expandedClients == null) {
                    //Add first item right away
                    mParentFragmentPreferences.edit()
                            .putStringSet(expandedClientsPrefKey, Sets.newHashSet(device.getMacAddress()))
                            .apply();
                }

                final CardView cardView = (CardView) mParentFragmentActivity.getLayoutInflater()
                        .inflate(R.layout.tile_status_wireless_client, null);

                //Create Options Menu
                final ImageButton tileMenu = (ImageButton) cardView.findViewById(R.id.tile_status_wireless_client_device_menu);

                if (!isThemeLight) {
                    //Set menu background to white
                    tileMenu.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
                }

                cardView.setCardBackgroundColor(themeBackgroundColor);

                //Add padding to CardView on v20 and before to prevent intersections between the Card content and rounded corners.
                cardView.setPreventCornerOverlap(true);
                //Add padding in API v21+ as well to have the same measurements with previous versions.
                cardView.setUseCompatPadding(true);

                final String macAddress = device.getMacAddress();

                final TextView deviceNameView = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_name);
                final String name = device.getName();
                deviceNameView.setText(name);
                if (device.isActive()) {
                    deviceNameView.setTextColor(resources.getColor(R.color.ddwrt_green));
                }

                final TextView deviceMac = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_mac);
                deviceMac.setText(macAddress);

                final TextView deviceIp = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_ip);
                final String ipAddress = device.getIpAddress();
                deviceIp.setText(ipAddress);

                final boolean isThisDevice = (nullToEmpty(macAddress).equalsIgnoreCase(mCurrentMacAddress) &&
                        nullToEmpty(ipAddress).equals(mCurrentIpAddress));
                if (isThisDevice) {
                    final View thisDevice = cardView.findViewById(R.id.tile_status_wireless_client_device_this);
                    if (isThemeLight) {
                        //Set text color to blue
                        ((TextView) thisDevice)
                                .setTextColor(resources.getColor(R.color.blue));
                    }
                    thisDevice.setVisibility(View.VISIBLE);
                }

                final LinearLayout deviceDetailsPlaceHolder = (LinearLayout) cardView
                        .findViewById(R.id.tile_status_wireless_client_device_details_graph_placeholder);
                deviceDetailsPlaceHolder.removeAllViews();

                final BandwidthMonitoringIfaceData bandwidthMonitoringIfaceData = bandwidthMonitoringIfaceDataPerDevice.get(macAddress);
                if (bandwidthMonitoringIfaceData != null) {
                    final Map<String, EvictingQueue<BandwidthMonitoringTile.DataPoint>> dataCircularBuffer =
                            bandwidthMonitoringIfaceData.getData();

                    long maxX = System.currentTimeMillis() + 5000;
                    long minX = System.currentTimeMillis() - 5000;
                    double maxY = 10;
                    double minY = 1.;

                    final XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
                    final XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();

                    //noinspection ConstantConditions
                    for (final Map.Entry<String, EvictingQueue<BandwidthMonitoringTile.DataPoint>> entry : dataCircularBuffer.entrySet()) {
                        final String inOrOut = entry.getKey();
                        final EvictingQueue<BandwidthMonitoringTile.DataPoint> dataPoints = entry.getValue();
                        final XYSeries series = new XYSeries(inOrOut);
                        for (final BandwidthMonitoringTile.DataPoint point : dataPoints) {
                            final long x = point.getTimestamp();
                            final double y = point.getValue();
                            series.add(x, y);
                            maxX = Math.max(maxX, x);
                            minX = Math.min(minX, x);
                            maxY = Math.max(maxY, y);
                            minY = Math.min(minY, y);
                        }

                        // Now we add our series
                        dataset.addSeries(series);

                        // Now we create the renderer
                        final XYSeriesRenderer renderer = new XYSeriesRenderer();
                        renderer.setLineWidth(2);

                        Integer seriesColor = colorsCache.get(inOrOut);
                        if (seriesColor == null) {
                            //Generate a Random Color, excluding 'white' and 'black' (because graph background may be white or black)
                            seriesColor = Color.argb(255,
                                    1 + randomColorGen.nextInt(254),
                                    1 + randomColorGen.nextInt(254),
                                    1 + randomColorGen.nextInt(254));
                            colorsCache.put(inOrOut, seriesColor);
                        }
                        renderer.setColor(seriesColor);
                        // Include low and max value
                        renderer.setDisplayBoundingPoints(true);
                        // we add point markers
                        renderer.setPointStyle(PointStyle.POINT);
                        renderer.setPointStrokeWidth(1);

                        mRenderer.addSeriesRenderer(renderer);
                    }

                    // We want to avoid black border
                    mRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00)); // transparent margins
                    // Disable Pan on two axis
                    mRenderer.setPanEnabled(false, false);
                    mRenderer.setYAxisMax(maxY + 10);
                    mRenderer.setYAxisMin(minY);
                    mRenderer.setXAxisMin(minX);
                    mRenderer.setXAxisMax(maxX + 10);
                    mRenderer.setShowGrid(false);
                    mRenderer.setClickEnabled(false);
                    mRenderer.setZoomEnabled(true);
                    mRenderer.setPanEnabled(false);
                    mRenderer.setZoomRate(6.0f);
                    mRenderer.setShowLabels(true);
                    mRenderer.setFitLegend(true);
                    mRenderer.setInScroll(true);

                    final GraphicalView chartView = ChartFactory.getTimeChartView(mParentFragmentActivity, dataset, mRenderer, null);
                    chartView.repaint();

                    deviceDetailsPlaceHolder.addView(chartView, 0);
                }

                final NetworkTrafficView networkTrafficView =
                        new NetworkTrafficView(mParentFragmentActivity, isThemeLight, mRouter.getUuid(), device);
                networkTrafficView.setRxAndTxBytes(Double.valueOf(device.getRxRate()).longValue(),
                        Double.valueOf(device.getTxRate()).longValue());

                final LinearLayout trafficViewPlaceHolder = (LinearLayout) cardView
                        .findViewById(R.id.tile_status_wireless_client_network_traffic_placeholder);
                trafficViewPlaceHolder.removeAllViews();
                trafficViewPlaceHolder.addView(networkTrafficView);

                //OUI Addr
                final TextView ouiVendorRowView = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_details_oui_addr);
                final MACOUIVendor macouiVendorDetails = device.getMacouiVendorDetails();
                if (macouiVendorDetails == null || isNullOrEmpty(macouiVendorDetails.getCompany())) {
                    ouiVendorRowView.setText("-");
                } else {
                    ouiVendorRowView.setText(macouiVendorDetails.getCompany());
                }

                final RelativeTimeTextView lastSeenRowView = (RelativeTimeTextView) cardView.findViewById(R.id.tile_status_wireless_client_device_details_lastseen);
                final long lastSeen = device.getLastSeen();
                if (lastSeen <= 0l) {
                    lastSeenRowView.setText("-");
                } else {
                    lastSeenRowView.setReferenceTime(lastSeen);
                    lastSeenRowView.setPrefix(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(lastSeen)) + "\n(");
                    lastSeenRowView.setSuffix(")");
                }

                final TextView totalDownloadRowView = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_details_total_download);
                final double rxTotal = device.getRxTotal();
                if (rxTotal < 0.) {
                    totalDownloadRowView.setText("-");
                } else {
                    final long value = Double.valueOf(rxTotal).longValue();
                    totalDownloadRowView.setText(value + " B (" + byteCountToDisplaySize(value) + ")");
                }

                final TextView totalUploadRowView = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_details_total_upload);
                final double txTotal = device.getTxTotal();
                if (txTotal < 0.) {
                    totalUploadRowView.setText("-");
                } else {
                    final long value = Double.valueOf(txTotal).longValue();
                    totalUploadRowView.setText(value + " B (" + byteCountToDisplaySize(value) + ")");
                }

                final View ouiAndLastSeenView = cardView.findViewById(R.id.tile_status_wireless_client_device_details_oui_lastseen_table);
                final View trafficGraphPlaceHolderView = cardView.findViewById(R.id.tile_status_wireless_client_device_details_graph_placeholder);

                cardView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final Set<String> clientsExpanded = new HashSet<>(mParentFragmentPreferences
                                .getStringSet(expandedClientsPrefKey, new HashSet<String>()));

                        if (ouiAndLastSeenView.getVisibility() == View.VISIBLE) {
                            ouiAndLastSeenView.setVisibility(View.GONE);
                            clientsExpanded.remove(macAddress);
                            cardView.setCardElevation(20f);
                        } else {
                            ouiAndLastSeenView.setVisibility(View.VISIBLE);
                            clientsExpanded.add(macAddress);
                            cardView.setCardElevation(0f);
                        }
                        if (trafficGraphPlaceHolderView.getVisibility() == View.VISIBLE) {
                            trafficGraphPlaceHolderView.setVisibility(View.GONE);
                        } else {
                            trafficGraphPlaceHolderView.setVisibility(View.VISIBLE);
                        }
                        mParentFragmentPreferences.edit()
                                .putStringSet(expandedClientsPrefKey, clientsExpanded)
                                .apply();
                    }
                });

                expandedClients = mParentFragmentPreferences.getStringSet(expandedClientsPrefKey,
                        new HashSet<String>());
                if (expandedClients.contains(macAddress)) {
                    //Expand detailed view for the first item only
                    ouiAndLastSeenView.setVisibility(View.VISIBLE);
                    trafficGraphPlaceHolderView.setVisibility(View.VISIBLE);
                    //Highlight CardView
                    cardView.setCardElevation(20f);
                } else {
                    //Collapse detailed view for the first item only
                    ouiAndLastSeenView.setVisibility(View.GONE);
                    trafficGraphPlaceHolderView.setVisibility(View.GONE);
                    cardView.setCardElevation(0f);
                }

//                cardView.setOnClickListener(new DeviceOnClickListener(device));

                clientsContainer.addView(cardView);

                tileMenu.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final PopupMenu popup = new PopupMenu(mParentFragmentActivity, v);
                        popup.setOnMenuItemClickListener(new DeviceOnMenuItemClickListener(deviceNameView, device));
                        final MenuInflater inflater = popup.getMenuInflater();

                        final Menu menu = popup.getMenu();

                        inflater.inflate(R.menu.tile_status_wireless_client_options, menu);

                        if (isThisDevice) {
                            //WOL not needed as this is the current device
                            menu.findItem(R.id.tile_status_wireless_client_wol).setEnabled(false);
                        }

                        popup.show();
                    }
                });

            }

            final Button showMore = (Button) this.layout.findViewById(R.id.tile_status_wireless_clients_show_more);
            //Whether to display 'Show more' button
            if (data.getDevicesCount() > MAX_CLIENTS_TO_SHOW_IN_TILE) {
                showMore.setVisibility(View.VISIBLE);
                showMore.setOnClickListener(this);
            } else {
                showMore.setVisibility(View.GONE);
            }

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

        doneWithLoaderInstance(this, loader,
                R.id.tile_status_wireless_clients_togglebutton_title, R.id.tile_status_wireless_clients_togglebutton_separator);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");
    }

    @Nullable
    @Override
    protected OnClickIntent getOnclickIntent() {
        //TODO
        return null;
    }

    private class DeviceOnMenuItemClickListener implements
            PopupMenu.OnMenuItemClickListener, UndoBarController.AdvancedUndoListener, RouterActionListener {

        @NotNull
        private final TextView deviceNameView;
        @NotNull
        private final Device device;

        private DeviceOnMenuItemClickListener(@NotNull TextView deviceNameView, @NotNull final Device device) {
            this.deviceNameView = deviceNameView;
            this.device = device;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            final String macAddress = device.getMacAddress();
            switch(item.getItemId()) {
                case R.id.tile_status_wireless_client_wol:
                    //TODO Support Secure On Password????
                    final String deviceName = nullToEmpty(device.getName());
                    new AlertDialog.Builder(mParentFragmentActivity)
                            .setIcon(R.drawable.ic_action_alert_warning)
                            .setTitle(String.format("Wake up '%s' (%s)", deviceName, macAddress))
                            .setMessage(String.format("This lets you turn on a computer via the network.\n" +
                                            "For this to work properly:\n" +
                                            "- '%s' (%s) hardware must support Wake-on-LAN (WOL). You can enable it in the BIOS or in the Operating System Settings.\n" +
                                            "- WOL magic packet will be sent from the router to '%s' (%s). To wake over the Internet, " +
                                            "you must forward packets from any port you want to the device you wish to wake.\n" +
                                            "Note that some computers support WOL only when they are in Sleep mode or Hibernated, " +
                                            "not powered off. Some may also require a SecureOn password, which is not supported (yet)!",
                                    deviceName, macAddress, deviceName, macAddress))
                            .setCancelable(true)
                            .setPositiveButton("Send Magic Packet!", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(final DialogInterface dialogInterface, final int i) {

                                    final Bundle token = new Bundle();
                                    token.putString(ROUTER_ACTION, RouterAction.WAKE_ON_LAN.toString());

                                    new UndoBarController.UndoBar(mParentFragmentActivity)
                                            .message(String.format("WOL Request will be sent from router to '%s' (%s)",
                                                    deviceName, macAddress))
                                            .listener(DeviceOnMenuItemClickListener.this)
                                            .token(token)
                                            .show();

//                                    new WoLUtils.SendWoLMagicPacketAsyncTask(mParentFragmentActivity, device).execute(macAddress, mBroadcastAddress);
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //Cancelled - nothing more to do!
                                }
                            }).create().show();
                    return true;
                case R.id.tile_status_wireless_client_rename:
                    if (mParentFragmentPreferences == null) {
                        Toast.makeText(mParentFragmentActivity, "Internal Error: ", Toast.LENGTH_SHORT).show();
                        Utils.reportException(new
                                IllegalStateException("Click on R.id.tile_status_wireless_client_rename - mParentFragmentPreferences == null"));
                    } else {
                        final String currentAlias = mParentFragmentPreferences.getString(macAddress, null);
                        final boolean isNewAlias = isNullOrEmpty(currentAlias);
                        final EditText aliasInputText = new EditText(mParentFragmentActivity);
                        aliasInputText.setText(currentAlias, TextView.BufferType.EDITABLE);
                        aliasInputText.setHint("e.g., \"Mom's PC\"");
                        new AlertDialog.Builder(mParentFragmentActivity)
                                .setTitle((isNewAlias ? "Set device alias" : "Update device alias") + ": " + macAddress)
                                .setMessage("Note that the Alias you define here is stored locally only, not on the router.")
                                .setView(aliasInputText)
                                .setCancelable(true)
                                .setPositiveButton(isNewAlias ? "Set Alias" : "Update Alias", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(final DialogInterface dialogInterface, final int i) {
                                        try {
                                            final String newAlias = nullToEmpty(aliasInputText.getText().toString());
                                            if (newAlias.equals(currentAlias)) {
                                                return;
                                            }
                                            mParentFragmentPreferences.edit()
                                                    .putString(macAddress, newAlias)
                                                    .apply();
                                            //Update device name immediately
                                            device.setAlias(newAlias);
                                            deviceNameView.setText(device.getName());
                                            Utils.displayMessage(mParentFragmentActivity, "Alias set! Changes will appear upon next sync.", Style.CONFIRM);
                                        } catch (final Exception e) {
                                            Utils.reportException(new
                                                    IllegalStateException("Error: Click on R.id.tile_status_wireless_client_rename", e));
                                            Utils.displayMessage(mParentFragmentActivity, "Internal Error - please try again later", Style.ALERT);
                                        }

                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        //Cancelled - nothing more to do!
                                    }
                                }).create().show();
                    }
                    return true;
                default:
                    break;
            }
            return false;
        }

        @Override
        public void onHide(@android.support.annotation.Nullable Parcelable parcelable) {
            if (parcelable instanceof Bundle) {
                final Bundle token = (Bundle) parcelable;
                final String routerAction = token.getString(ROUTER_ACTION);
                Log.d(LOG_TAG, "routerAction: [" + routerAction + "]");
                if (isNullOrEmpty(routerAction)) {
                    return;
                }
                try {
                    switch (RouterAction.valueOf(routerAction)) {
                        case WAKE_ON_LAN:
                            if (broadcastAddresses == null) {
                                Utils.displayMessage(mParentFragmentActivity,
                                        "WOL Internal Error: unable to fetch broadcast addresses. Try again later.",
                                        Style.ALERT);
                                Utils.reportException(new IllegalStateException("WOL Internal Error: unable to fetch broadcast addresses. Try again later."));
                                return;
                            }
                            new WakeOnLANRouterAction(this, mGlobalPreferences, device,
                                    broadcastAddresses.toArray(new String[broadcastAddresses.size()]))
                                    .execute(mRouter);
                            break;
                        default:
                            //Ignored
                            break;
                    }
                } catch (IllegalArgumentException | NullPointerException e) {
                    e.printStackTrace();
                    Utils.reportException(e);
                }
            }
        }

        @Override
        public void onClear(@NonNull Parcelable[] parcelables) {
            //Nothing to do
        }

        @Override
        public void onRouterActionSuccess(@NotNull RouterAction routerAction, @NotNull Router router, Object returnData) {
            Utils.displayMessage(mParentFragmentActivity,
                    String.format("Action '%s' executed successfully on host '%s'", routerAction.toString().toLowerCase(), router.getRemoteIpAddress()),
                    Style.CONFIRM);
        }

        @Override
        public void onRouterActionFailure(@NotNull RouterAction routerAction, @NotNull Router router, @Nullable Exception exception) {
            Utils.displayMessage(mParentFragmentActivity,
                    String.format("Error on action '%s': %s", routerAction.toString().toLowerCase(), ExceptionUtils.getRootCauseMessage(exception)),
                    Style.ALERT);
        }

        @Override
        public void onUndo(@android.support.annotation.Nullable Parcelable parcelable) {
            //Nothing to do
        }
    }

}
