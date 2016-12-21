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

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.content.PermissionChecker;
import android.support.v7.widget.CardView;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.cocosw.undobar.UndoBarController;
import com.crashlytics.android.Crashlytics;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer.FillOutsideLine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.actions.ActionManager;
import org.rm3l.ddwrt.actions.DisableWANAccessRouterAction;
import org.rm3l.ddwrt.actions.EnableWANAccessRouterAction;
import org.rm3l.ddwrt.actions.ResetBandwidthMonitoringCountersRouterAction;
import org.rm3l.ddwrt.actions.RouterAction;
import org.rm3l.ddwrt.actions.RouterActionListener;
import org.rm3l.ddwrt.actions.WakeOnLANRouterAction;
import org.rm3l.ddwrt.exceptions.DDWRTCompanionException;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.lookup.MACOUILookupService;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.multithreading.MultiThreadingManager;
import org.rm3l.ddwrt.resources.ClientDevices;
import org.rm3l.ddwrt.resources.Device;
import org.rm3l.ddwrt.resources.MACOUIVendor;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.service.tasks.ConnectedHostsServiceTask;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.status.bandwidth.BandwidthMonitoringTile;
import org.rm3l.ddwrt.tiles.status.wireless.filter.impl.HideInactiveClientsFilterVisitorImpl;
import org.rm3l.ddwrt.tiles.status.wireless.filter.impl.ShowOnlyHostsWithWANAccessDisabledFilterVisitorImpl;
import org.rm3l.ddwrt.tiles.status.wireless.filter.impl.ShowWirelessDevicesOnlyClientsFilterVisitorImpl;
import org.rm3l.ddwrt.tiles.status.wireless.sort.ClientsSortingVisitor;
import org.rm3l.ddwrt.tiles.status.wireless.sort.impl.ClientsAlphabeticalSortingVisitorImpl;
import org.rm3l.ddwrt.tiles.status.wireless.sort.impl.LastSeenClientsSortingVisitorImpl;
import org.rm3l.ddwrt.tiles.status.wireless.sort.impl.TopTalkersClientsSortingVisitorImpl;
import org.rm3l.ddwrt.utils.AdUtils;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.ImageUtils;
import org.rm3l.ddwrt.utils.NVRAMParser;
import org.rm3l.ddwrt.utils.NetworkUtils;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;
import org.rm3l.ddwrt.utils.snackbar.SnackbarCallback;
import org.rm3l.ddwrt.utils.snackbar.SnackbarUtils;
import org.rm3l.ddwrt.widgets.NetworkTrafficView;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.keyboardsurfer.android.widget.crouton.Style;
import needle.UiRelatedTask;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;
import static org.rm3l.ddwrt.main.DDWRTMainActivity.ROUTER_ACTION;
import static org.rm3l.ddwrt.tiles.status.bandwidth.BandwidthMonitoringTile.BandwidthMonitoringIfaceData;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DDWRTCOMPANION_WANACCESS_IPTABLES_CHAIN;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.EMPTY_VALUE_TO_DISPLAY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_NAME;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_PATH_REMOTE;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.getClientsUsageDataFile;

public class WirelessClientsTile
        extends DDWRTTile<ClientDevices>
        implements PopupMenu.OnMenuItemClickListener, SnackbarCallback {

    public static final String HIDE_INACTIVE_HOSTS = "hideInactiveHosts";
    public static final String WIRELESS_DEVICES_ONLY = "wirelessDevicesOnly";
    public static final String SORT_LAST_SEEN = "sort_last_seen";
    public static final String SORT = "sort";
    public static final String SORT_TOP_TALKERS = SORT + "_top_talkers";
    public static final String SORT_APHABETICAL = SORT + "_aphabetical";
    public static final String SORTING_STRATEGY = "sorting_strategy";
    public static final String SHOW_ONLY_WAN_ACCESS_DISABLED_HOSTS = "show_only_wan_access_disabled_hosts";
    public static final String IN = "IN";
    public static final String OUT = "OUT";
    public static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance();
    public static final String RT_GRAPHS = "rt_graphs";
    private static final String LOG_TAG = WirelessClientsTile.class.getSimpleName();
    private static final int MAX_CLIENTS_TO_SHOW_IN_TILE = 999;

    private static final int MAC_OUI_VENDOR_LOOKUP_CACHE_SIZE = 20;

    private static final MACOUILookupService mMACOUILookupService =
             NetworkUtils.createApiService(MACOUIVendor.TOOLS_RM3L_PREFIX, MACOUILookupService.class);

    public static final LoadingCache<String, MACOUIVendor> mMacOuiVendorLookupCache = CacheBuilder
            .newBuilder()
            .softValues()
            .maximumSize(MAC_OUI_VENDOR_LOOKUP_CACHE_SIZE)
            .removalListener(new RemovalListener<String, MACOUIVendor>() {
                @Override
                public void onRemoval(@NonNull RemovalNotification<String, MACOUIVendor> notification) {
                    Crashlytics.log(Log.DEBUG, LOG_TAG, "onRemoval(" + notification.getKey() + ") - cause: " +
                            notification.getCause());
                }
            })
            .build(new CacheLoader<String, MACOUIVendor>() {
                @Override
                public MACOUIVendor load(@Nullable String macAddr) throws Exception {
                    if (isNullOrEmpty(macAddr)) {
                        throw new IllegalArgumentException();
                    }
                    //Get to MAC OUI Vendor Lookup API
                    try {
                        return mMACOUILookupService
                                .lookupMACAddress(macAddr.replaceAll(":", "-"))
                                .execute()
                                .body();

                    } catch (final Exception e) {
                        e.printStackTrace();
                        throw new DDWRTCompanionException(e);
                    }
                }
            });

    private static final String PER_IP_MONITORING_IP_TABLES_CHAIN = "DDWRTCompanion";
    public static final String USAGE_DB = "/tmp/." + PER_IP_MONITORING_IP_TABLES_CHAIN + "_usage.db";
    public static final String USAGE_DB_OUT = USAGE_DB + ".out";
    //Generate a random string, to use as discriminator for determining dhcp clients
    public static final String MAP_KEYWORD = WirelessClientsTile.class.getSimpleName() + UUID.randomUUID().toString();
    private static final BiMap<Integer, Integer> sortIds = HashBiMap.create(6);
    public static final String CONNECTED_HOSTS = "connectedHosts";
    public static final String MAC_ADDRESS = "macAddress";
    public static final String IP_ADDRESS = "ipAddress";
    public static final String DEVICE_NAME_FOR_NOTIFICATION = "deviceNameForNotification";
    public static final Ordering<String> CASE_INSENSITIVE_STRING_ORDERING = Ordering.from(String.CASE_INSENSITIVE_ORDER);

    static {
        sortIds.put(R.id.tile_status_wireless_clients_sort_a_z, 72);
        sortIds.put(R.id.tile_status_wireless_clients_sort_z_a, 73);

        sortIds.put(R.id.tile_status_wireless_clients_sort_top_senders, 82);
        sortIds.put(R.id.tile_status_wireless_clients_sort_top_receivers, 83);
        sortIds.put(R.id.tile_status_wireless_clients_sort_top_senders_current_rate, 84);
        sortIds.put(R.id.tile_status_wireless_clients_sort_top_receivers_current_rate, 85);

        sortIds.put(R.id.tile_status_wireless_clients_sort_seen_recently, 92);
        sortIds.put(R.id.tile_status_wireless_clients_sort_not_seen_recently, 93);
    }

    private final Object usageDataLock = new Object();
    @NonNull
    private final Map<String, BandwidthMonitoringIfaceData> bandwidthMonitoringIfaceDataPerDevice =
            Maps.newHashMap();
    private final File mBandwidthMonitoringData;
    private String mCurrentIpAddress;
    private String mCurrentMacAddress;
    private String[] activeClients;
    private String[] activeDhcpLeases;
    private String[] activeIPConnections;
    @Nullable
    private List<String> broadcastAddresses;
    private File wrtbwmonScriptPath;
    private Map<Device, View> currentDevicesViewsMap = Maps.newTreeMap(new Comparator<Device>() {
        @Override
        public int compare(Device dev0, Device dev1) {
            if (dev0 == dev1) {
                return 0;
            }
            if (dev0 == null) {
                return 1;
            }
            if (dev1 == null) {
                return -1;
            }
            final String dev0Name = dev0.getAliasOrSystemName();
            final String dev1Name = dev1.getAliasOrSystemName();
            return nullToEmpty(dev0Name)
                    .compareToIgnoreCase(nullToEmpty(dev1Name));
        }
    });
    private String mUsageDbBackupPath = null;

    private Loader<ClientDevices> mCurrentLoader;
    private MenuItem mActiveIpConnectionsMenuItem;

    private ProgressBar mProgressBar;
    private TextView mProgressBarDesc;
    private boolean isThemeLight;

    @Nullable
    private InterstitialAd mInterstitialAdForActiveIPConnections;
    private long mLastSync;

    public WirelessClientsTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, Router router) {
        super(parentFragment, arguments,
                router,
                R.layout.tile_status_wireless_clients, null);

        isThemeLight = ColorUtils.isThemeLight(mParentFragmentActivity);

        mProgressBar = (ProgressBar) layout
                .findViewById(R.id.tile_status_wireless_clients_loading_view);
        mProgressBar.setMax(100);
        mProgressBarDesc = (TextView) layout
                .findViewById(R.id.tile_status_wireless_clients_loading_view_desc);

        if (isThemeLight) {
            mProgressBarDesc.setTextColor(
                    ContextCompat.getColor(mParentFragmentActivity, R.color.black));
        } else {
            mProgressBarDesc.setTextColor(
                    ContextCompat.getColor(mParentFragmentActivity, R.color.white));
        }
        mProgressBarDesc.setText("Loading...");

        if (mParentFragmentPreferences != null && !mParentFragmentPreferences.contains(getFormattedPrefKey(RT_GRAPHS))) {
            mParentFragmentPreferences.edit()
                    .putBoolean(getFormattedPrefKey(RT_GRAPHS), true)
                    .apply();
        }

        mParentFragmentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mInterstitialAdForActiveIPConnections = AdUtils.requestNewInterstitial(mParentFragmentActivity,
                        R.string.interstitial_ad_unit_id_open_active_ip_connections_activity);
            }
        });

        mBandwidthMonitoringData = new File(
                mParentFragmentActivity.getCacheDir(), this.getClass().getSimpleName() + ".tmp.dat");

        // Create Options Menu
        final ImageButton tileMenu = (ImageButton) layout.findViewById(R.id.tile_status_wireless_clients_menu);
        if (!isThemeLight) {
            //Set menu background to white
            tileMenu.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
        }
        tileMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final PopupMenu popup = new PopupMenu(mParentFragmentActivity, v);
                popup.setOnMenuItemClickListener(WirelessClientsTile.this);
                final MenuInflater inflater = popup.getMenuInflater();
                final Menu menu = popup.getMenu();
                inflater.inflate(R.menu.tile_status_wireless_clients_options, menu);

                //Disable menu item from preference
                if (mParentFragmentPreferences != null) {
                    if (mParentFragmentPreferences.getBoolean(getFormattedPrefKey(HIDE_INACTIVE_HOSTS), false)) {
                        menu.findItem(R.id.tile_status_wireless_clients_hide_inactive_hosts).setChecked(true);
                    }
                    if (mParentFragmentPreferences.getBoolean(getFormattedPrefKey(WIRELESS_DEVICES_ONLY), false)) {
                        menu.findItem(R.id.tile_status_wireless_clients_wireless_devices_only).setChecked(true);
                    }
                }

                final MenuItem rtMenuItem = menu.findItem(R.id.tile_status_wireless_clients_realtime_graphs);
                if (mParentFragmentPreferences != null) {
                    rtMenuItem.setVisible(true);
                    rtMenuItem
                            .setEnabled(mParentFragmentPreferences.contains(getFormattedPrefKey(RT_GRAPHS)));
                    rtMenuItem
                            .setChecked(mParentFragmentPreferences
                                    .getBoolean(getFormattedPrefKey(RT_GRAPHS), false));
                    rtMenuItem.setVisible(false);
                } else {
                    rtMenuItem.setVisible(false);
                }

                final MenuItem showOnlyHostsWithWANAccessDisabledMenuItem = menu
                        .findItem(R.id.tile_status_wireless_clients_show_only_hosts_with_wan_access_disabled);
                //If no devices with WAN Access Disabled, disable the corresponding menu item
                final boolean atLeastOneDeviceWithNoWANAccess = Sets.filter(currentDevicesViewsMap.keySet(), new Predicate<Device>() {
                    @Override
                    public boolean apply(Device input) {
                        return (input.getWanAccessState() == Device.WANAccessState.WAN_ACCESS_DISABLED);
                    }
                }).size() > 0;
                final boolean wanAccessTogglePref = mParentFragmentPreferences != null &&
                        mParentFragmentPreferences.getBoolean(getFormattedPrefKey(SHOW_ONLY_WAN_ACCESS_DISABLED_HOSTS), false);
                if (!atLeastOneDeviceWithNoWANAccess) {
                    showOnlyHostsWithWANAccessDisabledMenuItem.setChecked(false);
                    if (wanAccessTogglePref) {
                        mParentFragmentPreferences.edit()
                                .putBoolean(getFormattedPrefKey(SHOW_ONLY_WAN_ACCESS_DISABLED_HOSTS), false)
                                .apply();
                    }
                } else {
                    //Mark as checked
                    showOnlyHostsWithWANAccessDisabledMenuItem.setChecked(wanAccessTogglePref);
                }

                showOnlyHostsWithWANAccessDisabledMenuItem.setEnabled(atLeastOneDeviceWithNoWANAccess);

                if (mParentFragmentPreferences != null) {
                    final Integer currentSortStrategy = sortIds.inverse()
                            .get(mParentFragmentPreferences.getInt(getFormattedPrefKey(SORTING_STRATEGY), -1));
                    if (currentSortStrategy != null && currentSortStrategy > 0) {
                        final MenuItem currentSortMenuItem = menu.findItem(currentSortStrategy);
                        if (currentSortMenuItem != null) {
                            currentSortMenuItem.setEnabled(false);
                            currentSortMenuItem.setChecked(true);
                        }
                    }
                }

                final boolean activeIpConnectionsMenuItemEnabled =
                        (activeIPConnections != null && activeIPConnections.length > 0);
                mActiveIpConnectionsMenuItem = menu
                        .findItem(R.id.tile_status_wireless_clients_view_active_ip_connections);
                mActiveIpConnectionsMenuItem
                        .setEnabled(activeIpConnectionsMenuItemEnabled);
                if (activeIpConnectionsMenuItemEnabled) {
                    mActiveIpConnectionsMenuItem
                            .setTitle(mParentFragmentActivity.getResources().getString(R.string.view_active_ip_connections) +
                                    " (" + activeIPConnections.length + ")");
                    mActiveIpConnectionsMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            final Intent intent = new Intent(mParentFragmentActivity, ActiveIPConnectionsDetailActivity.class);
                            intent.putExtra(ActiveIPConnectionsDetailActivity.ACTIVE_IP_CONNECTIONS_OUTPUT, activeIPConnections);
                            intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouter.getUuid());
                            intent.putExtra(ActiveIPConnectionsDetailActivity.ROUTER_REMOTE_IP, mRouter.getRemoteIpAddress());
                            intent.putExtra(ActiveIPConnectionsDetailActivity.OBSERVATION_DATE, new Date().toString());

                            final HashMap<String, String> currentIpToHostNameResolverMap = new HashMap<String, String>();
                            if (currentDevicesViewsMap != null) {
                                for (Device device : currentDevicesViewsMap.keySet()) {
                                    if (device == null) {
                                        continue;
                                    }
                                    currentIpToHostNameResolverMap.put(device.getIpAddress(), device.getName());
                                }
                            }

                            intent.putExtra(ActiveIPConnectionsDetailActivity.IP_TO_HOSTNAME_RESOLVER, currentIpToHostNameResolverMap);

                            if (BuildConfig.WITH_ADS &&
                                    mInterstitialAdForActiveIPConnections != null
                                    && AdUtils.canDisplayInterstialAd(mParentFragmentActivity)) {

                                mInterstitialAdForActiveIPConnections.setAdListener(new AdListener() {
                                    @Override
                                    public void onAdClosed() {
                                        final AdRequest adRequest = AdUtils.buildAdRequest(mParentFragmentActivity);
                                        if (adRequest != null) {
                                            mInterstitialAdForActiveIPConnections.loadAd(adRequest);
                                        }
                                        mParentFragmentActivity.startActivity(intent);
                                    }

                                    @Override
                                    public void onAdOpened() {
                                        //Save preference
                                        mGlobalPreferences.edit()
                                                .putLong(
                                                        DDWRTCompanionConstants.AD_LAST_INTERSTITIAL_PREF,
                                                        System.currentTimeMillis())
                                                .apply();
                                    }
                                });

                                if (mInterstitialAdForActiveIPConnections.isLoaded()) {
                                    mInterstitialAdForActiveIPConnections.show();
                                } else {
                                    //noinspection ConstantConditions
                                    final AlertDialog alertDialog = Utils.buildAlertDialog(mParentFragmentActivity, null,
                                            "Loading...", false, false);
                                    alertDialog.show();
                                    ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            mParentFragmentActivity.startActivity(intent);
                                            alertDialog.cancel();
                                        }
                                    }, 1000);
                                }

                            } else {
                                //noinspection ConstantConditions
                                final AlertDialog alertDialog = Utils.buildAlertDialog(mParentFragmentActivity, null,
                                        "Loading...", false, false);
                                alertDialog.show();
                                ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        mParentFragmentActivity.startActivity(intent);
                                        alertDialog.cancel();
                                    }
                                }, 1000);
                            }

                            return true;
                        }
                    });
                }

                popup.show();
            }
        });

        ((TextView) layout.findViewById(R.id.tile_status_wireless_clients_active_ip_connections_num))
                .addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (mActiveIpConnectionsMenuItem != null) {
                            if (s == null || Strings.isNullOrEmpty(s.toString()) ||
                                    EMPTY_VALUE_TO_DISPLAY.equals(s.toString())) {
                                mActiveIpConnectionsMenuItem.setEnabled(false);
                            } else {
                                mActiveIpConnectionsMenuItem.setEnabled(true);
                                mActiveIpConnectionsMenuItem.setTitle(mParentFragmentActivity.getResources().getString(R.string.view_active_ip_connections) +
                                        " (" + s + ")");
                            }
                        }
                    }
                });

        //Permission requests
        final int rwExternalStoragePermissionCheck = PermissionChecker.checkSelfPermission(mParentFragmentActivity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        //Android >= 6.0 now requires to request the COARSE_LOCATION permission to read HW Mac Addresses
        final int accessCoarseLocationPermissionCheck = PermissionChecker.checkSelfPermission(mParentFragmentActivity,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        if (rwExternalStoragePermissionCheck != PackageManager.PERMISSION_GRANTED
                || accessCoarseLocationPermissionCheck != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            final boolean storageRequestRationale = ActivityCompat
                    .shouldShowRequestPermissionRationale(
                            mParentFragmentActivity,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE);
            final boolean locationRequestRationale = ActivityCompat
                    .shouldShowRequestPermissionRationale(
                            mParentFragmentActivity,
                            Manifest.permission.ACCESS_COARSE_LOCATION);
            if (storageRequestRationale
                    || locationRequestRationale) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                final StringBuilder explanation = new StringBuilder();
                if (storageRequestRationale) {
                    explanation.append("Storage access helps in caching bandwidth data.");
                }
                if (locationRequestRationale) {
                    explanation.append("Coarse location is used to detect device MAC address.");
                }
                SnackbarUtils.buildSnackbar(mParentFragmentActivity,
                        explanation.toString(),
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
                                ActivityCompat.requestPermissions(mParentFragmentActivity,
                                        new String[]{
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                Manifest.permission.ACCESS_COARSE_LOCATION},
                                        DDWRTCompanionConstants.Permissions.STORAGE);
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
                ActivityCompat.requestPermissions(mParentFragmentActivity,
                        new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.ACCESS_COARSE_LOCATION},
                        DDWRTCompanionConstants.Permissions.STORAGE_LOCATION);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_status_wireless_clients_title;
    }

    @Override
    public int getTileHeaderViewId() {
        return R.id.tile_status_wireless_clients_hdr;
    }

    @Nullable
    private Set<String> getActiveIPConnectionsForClient(@Nullable final String clientIpAddr) {
        if (activeIPConnections == null || clientIpAddr == null || clientIpAddr.isEmpty()) {
            return null;
        }
        final Set<String> activeIpConnectionsForClient = new HashSet<>();
        for (final String activeIPConnection : activeIPConnections) {
            if (StringUtils.containsIgnoreCase(activeIPConnection, clientIpAddr)) {
                activeIpConnectionsForClient.add(activeIPConnection);
            }
        }
        return activeIpConnectionsForClient;
    }

    @Nullable
    @Override
    protected Loader<ClientDevices> getLoader(int id, Bundle args) {
        this.mCurrentLoader = new AsyncTaskLoader<ClientDevices>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public ClientDevices loadInBackground() {

                isThemeLight = ColorUtils.isThemeLight(mParentFragmentActivity);

                Crashlytics.log(Log.DEBUG, LOG_TAG, "Init background loader for " + WirelessClientsTile.class + ": routerInfo=" +
                        mRouter + " / nbRunsLoader=" + nbRunsLoader);

                //Determine broadcast address at each run (because that might change if connected to another network)
                try {
                    final WifiManager wifiManager = (WifiManager) mParentFragmentActivity.getSystemService(Context.WIFI_SERVICE);
                    final WifiInfo connectionInfo = wifiManager.getConnectionInfo();

                    mCurrentIpAddress = Utils.decimalToIp4(connectionInfo.getIpAddress());
                    mCurrentMacAddress = connectionInfo.getMacAddress();
                } catch (@NonNull final Exception e) {
                    e.printStackTrace();
                    //No worries
                }

                if (mRefreshing.getAndSet(true)) {
                    return new ClientDevices().setException(new DDWRTTileAutoRefreshNotAllowedException());
                }
                nbRunsLoader++;

                updateProgressBarViewSeparator(0);

                mLastSync = System.currentTimeMillis();

                mParentFragmentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (nbRunsLoader <= 1) {
                            mProgressBar.setVisibility(View.VISIBLE);
                            mProgressBarDesc.setVisibility(View.VISIBLE);
                        } else {
                            mProgressBar.setVisibility(View.GONE);
                            mProgressBarDesc.setVisibility(View.GONE);
                        }
                    }
                });

                //TODO Disabled for now
//                try {
//                    //Try loading from cache
//                    final Gson gson = new GsonBuilder().create();
//                    final JsonReader reader = new JsonReader(new FileReader(mBandwidthMonitoringData));
//                    final Map<String, Map<String, Collection<BandwidthMonitoringTile.DataPoint>>> result =
//                            gson.fromJson(reader, Map.class);
//                    if (!result.isEmpty()) {
//                        bandwidthMonitoringIfaceDataPerDevice.clear();
//                        final Comparator<BandwidthMonitoringTile.DataPoint> comparator = new Comparator<BandwidthMonitoringTile.DataPoint>() {
//                            @Override
//                            public int compare(BandwidthMonitoringTile.DataPoint lhs, BandwidthMonitoringTile.DataPoint rhs) {
//                                if (lhs == rhs) {
//                                    return 0;
//                                }
//                                if (rhs == null) {
//                                    return -1;
//                                }
//                                if (lhs == null) {
//                                    return 1;
//                                }
//                                return Long.valueOf(rhs.getTimestamp()).compareTo(lhs.getTimestamp());
//                            }
//                        };
//                        for (Map.Entry<String, Map<String, Collection<BandwidthMonitoringTile.DataPoint>>> entry : result.entrySet()) {
//                            final String key = entry.getKey();
//                            final Map<String, Collection<BandwidthMonitoringTile.DataPoint>> value = entry.getValue();
//                            if (key == null || value == null) {
//                                continue;
//                            }
//                            BandwidthMonitoringTile.BandwidthMonitoringIfaceData bandwidthMonitoringIfaceData =
//                                    bandwidthMonitoringIfaceDataPerDevice.get(key);
//                            if (bandwidthMonitoringIfaceData == null) {
//                                bandwidthMonitoringIfaceData = new BandwidthMonitoringTile.BandwidthMonitoringIfaceData();
//                            }
//                            for (final Map.Entry<String, Collection<BandwidthMonitoringTile.DataPoint>> valueEntry : value.entrySet()) {
//                                final String valueEntryKey = \"fake-key\";
//                                final Collection valueEntryValue = valueEntry.getValue();
//                                //Order datapoints (timestamp ordering)
//                                final SortedSet<BandwidthMonitoringTile.DataPoint> dataPoints = new TreeSet<>(comparator);
//                                for (final Object datapoint : valueEntryValue) {
//                                    final JsonObject jsonObject = gson.toJsonTree(datapoint).getAsJsonObject();
//                                    dataPoints.add(new BandwidthMonitoringTile.DataPoint(jsonObject.get("timestamp").getAsLong(),
//                                            jsonObject.get("value").getAsDouble()));
//                                }
//                                for (final BandwidthMonitoringTile.DataPoint dataPoint : dataPoints) {
//                                    bandwidthMonitoringIfaceData.addData(valueEntryKey, dataPoint);
//                                }
//                            }
//
//                            bandwidthMonitoringIfaceDataPerDevice.put(key, bandwidthMonitoringIfaceData);
//                        }
//                    }
//
//                } catch (final Exception ignored) {
//                    //No worries
//                    ignored.printStackTrace();
//                }

                final ClientDevices devices = new ClientDevices();

                broadcastAddresses = Lists.newArrayList();

                try {

                    updateProgressBarViewSeparator(10);
                    mParentFragmentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressBar.setProgress(10);
                            mProgressBarDesc.setText("Fetching Broadcast address (for Wake on LAN (WOL))...");
                        }
                    });

                    //Get Broadcast Addresses (for WOL)
                    try {
                        final String[] wanAndLanBroadcast = SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                "/sbin/ifconfig `/usr/sbin/nvram get wan_iface` | grep Bcast | /usr/bin/awk -F'Bcast:' '{print $2}' | /usr/bin/awk -F'Mask:' '{print $1}'",
                                "/sbin/ifconfig `/usr/sbin/nvram get lan_ifname` | grep Bcast | /usr/bin/awk -F'Bcast:' '{print $2}' | /usr/bin/awk -F'Mask:' '{print $1}'");
                        if (wanAndLanBroadcast != null && wanAndLanBroadcast.length > 0) {
                            for (final String wanAndLanBcast : wanAndLanBroadcast) {
                                if (wanAndLanBcast == null) {
                                    continue;
                                }
                                broadcastAddresses.add(wanAndLanBcast.trim());
                            }
                        }
                    } catch (final Exception e) {
                        //No worries
                        e.printStackTrace();
                    }

                    Crashlytics.log(Log.DEBUG, LOG_TAG, "broadcastAddresses: " + broadcastAddresses);

                    final Multimap<String, String> phyToWlIfaces = ArrayListMultimap.create();
                    try {
                        final String[] wirelessSsids = SSHUtils.getManualProperty(mParentFragmentActivity,
                                mRouter,
                                mGlobalPreferences,
                                "/usr/sbin/nvram show | grep 'ssid='");
//                        if (wirelessSsids == null || wirelessSsids.length == 0) {
//                            //FIXME Should we abort right away???
//                            return null;
//                        }

                        if (wirelessSsids != null) {
                            for (final String wirelessSsid : wirelessSsids) {
                                if (wirelessSsid == null ||
                                        wirelessSsid.startsWith("af_")) {
                                    //skip AnchorFree SSID
                                    continue;
                                }
                                final List<String> strings = NVRAMParser.SPLITTER.splitToList(wirelessSsid);
                                final int size = strings.size();
                                if (size == 1) {
                                    continue;
                                }

                                if (size >= 2) {
                                    final String wlIface = strings.get(0).replace("_ssid", "");
//                                if (wlIface.contains(".")) {
//                                    //Skip vifs as well, as they will be considered later on
//                                    continue;
//                                }
                                    final String nvramKey = \"fake-key\";
                                    final NVRAMInfo phyFromNVRAM = SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity,
                                            mRouter, mGlobalPreferences,
                                            nvramKey);
                                    if (phyFromNVRAM == null || phyFromNVRAM.getProperty(nvramKey) == null) {
                                        continue;
                                    }

                                    final String phy = phyFromNVRAM.getProperty(nvramKey);
                                    if (phy.isEmpty()) {
                                        continue;
                                    }

                                    phyToWlIfaces.put(phy, wlIface);
                                }
                            }
                        }
                        Crashlytics.log(Log.DEBUG, LOG_TAG, "phyToWlIfaces: " + phyToWlIfaces);
                    } catch (final Exception e) {
                        //No worries
                        e.printStackTrace();
                    }

                    updateProgressBarViewSeparator(20);
                    mParentFragmentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressBar.setProgress(20);
                            mProgressBarDesc.setText("Loading active clients...");
                        }
                    });

                    //Active clients
                    activeClients = SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                            "/sbin/arp -a 2>/dev/null");
                    if (activeClients != null) {
                        devices.setActiveClientsNum(activeClients.length);
                    }

                    Crashlytics.log(Log.DEBUG, LOG_TAG, "activeClients: " +
                            (activeClients != null ?
                                activeClients.length : "NULL"));

                    updateProgressBarViewSeparator(30);
                    mParentFragmentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressBar.setProgress(30);
                            mProgressBarDesc.setText("Loading DHCP Leases...");
                        }
                    });
                    //Active DHCP Leases
                    activeDhcpLeases = SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                            "/bin/cat /tmp/dnsmasq.leases 2>/dev/null");
                    if (activeDhcpLeases != null) {
                        devices.setActiveDhcpLeasesNum(activeDhcpLeases.length);
                    }

                    Crashlytics.log(Log.DEBUG, LOG_TAG, "activeDhcpLeases: " + Arrays.toString(activeDhcpLeases));

                    updateProgressBarViewSeparator(40);
                    mParentFragmentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressBar.setProgress(40);
                            mProgressBarDesc.setText("Loading Active IP Connections...");
                        }
                    });
                    //Active IP Connections
                    activeIPConnections = SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                            "/bin/cat /proc/net/ip_conntrack 2>/dev/null");
                    if (activeIPConnections != null) {
                        devices.setActiveIPConnections(activeIPConnections.length);
                    }

                    Crashlytics.log(Log.DEBUG, LOG_TAG, "#activeIPConnections: " + (activeIPConnections != null ?
                            activeIPConnections.length : "NULL"));

                    //Get WAN Gateway Address (we skip it!)
//                    String gatewayAddress = EMPTY_STRING;
//                    try {
//                        final NVRAMInfo nvRamInfoFromRouter = SSHUtils
//                                .getNVRamInfoFromRouter(mParentFragmentActivity, mRouter, mGlobalPreferences, WAN_GATEWAY);
//                        if (nvRamInfoFromRouter != null) {
//                            //noinspection ConstantConditions
//                            gatewayAddress = nvRamInfoFromRouter.getProperty(WAN_GATEWAY, EMPTY_STRING).trim();
//                        }
//                    } catch (final Exception e) {
//                        e.printStackTrace();
//                        //No worries
//                    }

                    updateProgressBarViewSeparator(50);
                    mParentFragmentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressBar.setProgress(50);
                            mProgressBarDesc.setText("Loading wireless clients...");
                        }
                    });


                    //Get list of wireless clients connected
                    final Multimap<String, String> wirelessIfaceAssocList = ArrayListMultimap.create();
                    try {

                        //Detect if Atheros or not
                        final String[] manualProperty = SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                "/usr/sbin/nvram get landevs 2>/dev/null");
                        final boolean useAtheros = (manualProperty == null || manualProperty.length == 0);

                        final String[] assocList;
                        if (useAtheros) {
                            assocList = SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                    "for phy in `/usr/sbin/nvram get lan_ifnames`; do " +
                                            "if [ ! -z \"$phy\" ]; then " +
                                            "/bin/echo iface $phy ; " +
                                            "( /usr/sbin/wl -i $phy assoclist 2>/dev/null || " +
                                            " /usr/sbin/wl_atheros -i $phy assoclist 2>/dev/null ) | /usr/bin/awk '{print $2}'; " +
                                            "/bin/echo 'done';  " +
                                            "fi; " +
                                            "done; " +
                                            "for j in `/usr/sbin/nvram get lan_ifnames`; do " +
                                            "for i in `/usr/sbin/nvram get ${j}_vifs`; do " +
                                            "phy=`/usr/sbin/nvram get ${i}_ifname 2>/dev/null`; " +
                                            "if [ -z \"$phy\" ]; then " +
                                            "phy=$i ; " +
                                            "fi; " +
                                            "if [ ! -z \"$phy\" ]; then " +
                                            "/bin/echo iface $i ; " +
                                            "( /usr/sbin/wl -i $phy assoclist 2>/dev/null || " +
                                            "/usr/sbin/wl_atheros -i $phy assoclist 2>/dev/null ) | /usr/bin/awk '{print $2}'; " +
                                            "/bin/echo 'done';  " +
                                            "fi; " +
                                            "done; " +
                                            "done"
                            );
                        } else {
                            assocList = SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                    "for i in `/usr/sbin/nvram get landevs`; do " +
                                            "phy=`/usr/sbin/nvram get ${i}_ifname 2>/dev/null`; " +
                                            "if [ ! -z \"$phy\" ]; then " +
                                            "/bin/echo iface $i ; " +
                                            "( /usr/sbin/wl -i $phy assoclist 2>/dev/null || " +
                                            " /usr/sbin/wl_atheros -i $phy assoclist 2>/dev/null ) | /usr/bin/awk '{print $2}'; " +
                                            "/bin/echo 'done';  " +
                                            "fi; " +
                                            "done; " +
                                            "for j in `/usr/sbin/nvram get landevs`; do " +
                                            "for i in `/usr/sbin/nvram get ${j}_vifs`; do " +
                                            "phy=`/usr/sbin/nvram get ${i}_ifname 2>/dev/null`; " +
                                            "if [ ! -z \"$phy\" ]; then " +
                                            "/bin/echo iface $i ; " +
                                            "( /usr/sbin/wl -i $phy assoclist 2>/dev/null || " +
                                            "/usr/sbin/wl_atheros -i $phy assoclist 2>/dev/null ) | /usr/bin/awk '{print $2}'; " +
                                            "/bin/echo 'done';  " +
                                            "fi; " +
                                            "done; " +
                                            "done"
                            );
                        }

                        Crashlytics.log(Log.DEBUG, LOG_TAG, "useAtheros= " + useAtheros + " / assocList: " + Arrays.toString(assocList));

                        if (assocList != null) {
                            String iface;
                            for (int idx = 0; idx < assocList.length; idx++) {
                                final String assoc = assocList[idx];
                                iface = null;
                                if (StringUtils.startsWithIgnoreCase(assoc, "iface ")) {
                                    iface = assoc.replaceAll("iface ", "").trim();
                                }
                                if (iface == null) {
                                    continue;
                                }

                                for (int i = idx; i < assocList.length; i++) {
                                    final String assocForIface = assocList[i];
                                    if ("done".equalsIgnoreCase(assocForIface)) {
                                        break;
                                    }
                                    if (StringUtils.startsWithIgnoreCase(assocForIface, "iface ")) {
                                        continue;
                                    }
                                    wirelessIfaceAssocList.put(iface, assocForIface.toLowerCase());
                                }

                            }
                        }
                        Crashlytics.log(Log.DEBUG, LOG_TAG, "wirelessIfaceAssocList: " + wirelessIfaceAssocList);

                    } catch (final Exception e) {
                        //No worries
                        e.printStackTrace();
                        Utils.reportException(mParentFragmentActivity, e);
                    }

                    final String[] output = SSHUtils.getManualProperty(mParentFragmentActivity, mRouter,
                            mGlobalPreferences, "/bin/grep dhcp-host /tmp/dnsmasq.conf | /bin/sed 's/.*=//' | /usr/bin/awk -F , '{print \"" +
                                    MAP_KEYWORD +
                                    "\",$1,$3 ,$2}'",
                            "/usr/bin/awk '{print \"" +
                                    MAP_KEYWORD +
                                    "\",$2,$3,$4}' /tmp/dnsmasq.leases",
                            "/usr/bin/awk 'NR>1{print \"" +
                                    MAP_KEYWORD +
                                    "\",$4,$1,\"*\"}' /proc/net/arp",
                            "/sbin/arp -a | awk '{print \"" +
                                    MAP_KEYWORD +
                                    "\",$4,$2,$1}'",
                            "/bin/echo done");

                    Crashlytics.log(Log.DEBUG, LOG_TAG, "output: " + Arrays.toString(output));

//                    if (output == null || output.length == 0) {
//                        if (output == null) {
//                            return devices;
//                        }
//                    }

                    final Map<String, Collection<String>> wlAssocListMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                    wlAssocListMap.putAll(wirelessIfaceAssocList.asMap());
                    final Collection<String> wlAssocListMacAddrs = wirelessIfaceAssocList.values();

                    final Map<String, Device> macToDevice = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                    final Multimap<String, Device> macToDeviceOutput = HashMultimap.create();

                    final Splitter splitter = Splitter.on(" ");

                    updateProgressBarViewSeparator(60);
                    if (output != null) {
                        final int outputLen = output.length;
                        mParentFragmentActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mProgressBar.setProgress(60);
                                mProgressBarDesc.setText("Processing a total of " + outputLen + " connected hosts...");
                            }
                        });
                        String ipAddress;
                        final Pattern betweenParenthesisPattern = Pattern.compile("\\((.*?)\\)");
                        int u = 1;
                        for (final String stdoutLine : output) {
                            if ("done".equals(stdoutLine)) {
                                break;
                            }
                            final int v = u++;
                            final List<String> as = splitter.splitToList(stdoutLine);
                            if (as.size() >= 4 && MAP_KEYWORD.equals(as.get(0))) {
                                final String macAddress = Strings.nullToEmpty(as.get(1)).toLowerCase();
                                if (isNullOrEmpty(macAddress) ||
                                        "00:00:00:00:00:00".equals(macAddress) ||
                                        StringUtils.containsIgnoreCase(macAddress, "incomplete")) {
                                    //Skip clients with incomplete ARP set-up
                                    continue;
                                }

                                ipAddress = as.get(2);
                                if (ipAddress != null) {
                                    final Matcher matcher = betweenParenthesisPattern.matcher(ipAddress);
                                    if (matcher.find()) {
                                        ipAddress = matcher.group(1);
                                    }
                                }

//                            if (StringUtils.equalsIgnoreCase(ipAddress, gatewayAddress)) {
//                                //Skip Gateway
//                                continue;
//                            }

                                final Device device = new Device(macAddress);
                                device.setIpAddress(ipAddress);

                                device.setActiveIpConnections(getActiveIPConnectionsForClient(ipAddress));

                                boolean isOnWirelessNetwork = false;
                                for (final String wlAssocListMacAddr : wlAssocListMacAddrs) {
                                    if (StringUtils.equalsIgnoreCase(macAddress, wlAssocListMacAddr)) {
                                        isOnWirelessNetwork = true;
                                        break;
                                    }
                                }
                                if (isOnWirelessNetwork) {
                                    final Device.WirelessConnectionInfo wirelessConnectionInfo = new Device.WirelessConnectionInfo();
                                    String iface = null;
                                    for (Map.Entry<String, Collection<String>> entry : wlAssocListMap.entrySet()) {
                                        final String wlIface = entry.getKey();
                                        final Collection<String> assocList = entry.getValue();
                                        for (final String assoc : assocList) {
                                            if (StringUtils.equalsIgnoreCase(macAddress, assoc)) {
                                                iface = wlIface;
                                                break;
                                            }
                                        }
                                        if (!isNullOrEmpty(iface)) {
                                            break;
                                        }
                                    }
                                    if (!isNullOrEmpty(iface)) {
                                        String realWlIface = null;
                                        final Collection<String> stringCollection = phyToWlIfaces.get(iface);
                                        if (!stringCollection.isEmpty()) {
                                            realWlIface = stringCollection.iterator().next();
                                        }

                                        //Fetch SSID, SNR and RSSI
                                        try {
                                            String[] ssidAndrssiAndSNROutput = SSHUtils.getManualProperty(mParentFragmentActivity, mRouter,
                                                    mGlobalPreferences,
                                                    String.format("/usr/sbin/nvram get %s_ssid || /bin/echo \"-\"", iface),
                                                    String.format("/usr/sbin/wl -i `/usr/sbin/nvram get %s_ifname` rssi %s || /usr/sbin/wl_atheros -i `/usr/sbin/nvram get %s_ifname` rssi %s",
                                                            iface, macAddress.toUpperCase(), iface, macAddress.toUpperCase()),
                                                    String.format("/usr/sbin/wl -i `/usr/sbin/nvram get %s_ifname` noise || /usr/sbin/wl_atheros -i `/usr/sbin/nvram get %s_ifname` noise", iface, iface),
                                                    String.format("( /usr/sbin/wl -i `/usr/sbin/nvram get %s_ifname` rssi %s || /usr/sbin/wl_atheros -i `/usr/sbin/nvram get %s_ifname` rssi %s ; " +
                                                                    "/bin/echo \" \"; /usr/sbin/wl -i `/usr/sbin/nvram get %s_ifname` noise || /usr/sbin/wl_atheros -i `/usr/sbin/nvram get %s_ifname` noise ) | " +
                                                                    "/usr/bin/tr -d '\\n' | /usr/bin/awk '{print $1-$2}'",
                                                            iface, macAddress.toUpperCase(), iface, macAddress, iface, iface));
                                            Crashlytics.log(Log.DEBUG, LOG_TAG, "ssidAndrssiAndSNROutput: " + Arrays.toString(ssidAndrssiAndSNROutput));
                                            if (ssidAndrssiAndSNROutput == null || ssidAndrssiAndSNROutput.length == 0) {
                                                //Try again. iface might represent the actual physical interface. We must try to fetch the wl one instead
                                                if (!isNullOrEmpty(realWlIface)) {
                                                    ssidAndrssiAndSNROutput = SSHUtils.getManualProperty(mParentFragmentActivity, mRouter,
                                                            mGlobalPreferences,
                                                            String.format("/usr/sbin/nvram get %s_ssid || echo \"-\"", realWlIface),
                                                            String.format("/usr/sbin/wl -i `/usr/sbin/nvram get %s_ifname` rssi %s || /usr/sbin/wl_atheros -i `/usr/sbin/nvram get %s_ifname` rssi %s",
                                                                    realWlIface, macAddress.toUpperCase(), realWlIface, macAddress.toUpperCase()),
                                                            String.format("/usr/sbin/wl -i `/usr/sbin/nvram get %s_ifname` noise || /usr/sbin/wl_atheros -i `/usr/sbin/nvram get %s_ifname` noise", realWlIface, realWlIface),
                                                            String.format("( /usr/sbin/wl -i `/usr/sbin/nvram get %s_ifname` rssi %s || /usr/sbin/wl_atheros -i `/usr/sbin/nvram get %s_ifname` rssi %s ; " +
                                                                            "/bin/echo \" \"; /usr/sbin/wl -i `/usr/sbin/nvram get %s_ifname` noise || /usr/sbin/wl_atheros -i `/usr/sbin/nvram get %s_ifname` noise ) | " +
                                                                            "/usr/bin/tr -d '\\n' | /usr/bin/awk '{print $1-$2}'",
                                                                    realWlIface, macAddress.toUpperCase(), realWlIface, macAddress, realWlIface, realWlIface));
                                                }
                                            }
                                            Crashlytics.log(Log.DEBUG, LOG_TAG, "ssidAndrssiAndSNROutput: " + Arrays.toString(ssidAndrssiAndSNROutput));
                                            if (ssidAndrssiAndSNROutput != null) {
                                                if (ssidAndrssiAndSNROutput.length >= 1) {
                                                    wirelessConnectionInfo.setSsid(ssidAndrssiAndSNROutput[0]);
                                                }
                                                if (ssidAndrssiAndSNROutput.length >= 2) {
                                                    wirelessConnectionInfo.setRssi(ssidAndrssiAndSNROutput[1]);
                                                }
                                                if (ssidAndrssiAndSNROutput.length >= 3) {
                                                    final String noiseStr = ssidAndrssiAndSNROutput[2];
                                                    try {
                                                        final int signal = Integer.parseInt(wirelessConnectionInfo.getRssi());
                                                        final int noise = Integer.parseInt(noiseStr);
                                                        if (noise != 0) {
                                                            final int snrAbs = 100 * Math.abs(signal / noise);
                                                            wirelessConnectionInfo.setSnr(String.valueOf(snrAbs));
                                                        }
                                                    } catch (final NumberFormatException e) {
                                                        //No worries
                                                        e.printStackTrace();
                                                    }
                                                }
                                                if (ssidAndrssiAndSNROutput.length >= 4) {
                                                    wirelessConnectionInfo.setSnrMargin(ssidAndrssiAndSNROutput[3]);
                                                }
                                            }
                                        } catch (final Exception e) {
                                            //No worries
                                            e.printStackTrace();
                                        }
                                        device.setWirelessConnectionInfo(wirelessConnectionInfo);
                                    } else {
                                        Utils.reportException(null, new IllegalStateException("Found device in assocList, but with invalid iface name!"));
                                    }
                                }

                                if (activeClients != null) {
                                    for (final String activeClient : activeClients) {
                                        if (StringUtils.containsIgnoreCase(activeClient, macAddress)) {
                                            device.setActive(true);
                                            break;
                                        }
                                    }
                                }

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

//                                mParentFragmentActivity.runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        mProgressBarDesc.setText("Resolving MAC Addresses (" +
//                                                v + "/" + outputLen +
//                                                ")...");
//                                    }
//                                });
//                                try {
//                                    device.setMacouiVendorDetails(mMacOuiVendorLookupCache.get(macAddress));
//                                } catch (final Exception e) {
//                                    e.printStackTrace();
//                                    Utils.reportException(mParentFragmentActivity, e);
//                                }

                                macToDeviceOutput.put(macAddress, device);
                            }
                        }
                    }

                    for (final Map.Entry<String, Collection<Device>> deviceEntry : macToDeviceOutput.asMap().entrySet()) {
                        final String macAddr = Strings.nullToEmpty(deviceEntry.getKey());
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

                        final Device dev = deviceCollection.iterator().next();

                        macToDevice.put(macAddr, dev);
                    }

                    String remoteChecksum = DDWRTCompanionConstants.EMPTY_STRING;

                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Before usageDataLock");

                    updateProgressBarViewSeparator(70);
                    synchronized (usageDataLock) {

                        mParentFragmentActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mProgressBar.setProgress(70);
                                mProgressBarDesc.setText("Retrieving bandwidth monitoring data...");
                            }
                        });

                        try {
                            if (PermissionChecker.checkSelfPermission(mParentFragmentActivity,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                                    PackageManager.PERMISSION_GRANTED) {

                                final File file = getClientsUsageDataFile(mParentFragmentActivity, mRouter.getUuid());
                                mUsageDbBackupPath = file.getAbsolutePath();
                                Crashlytics.log(Log.DEBUG, LOG_TAG, "mUsageDbBackupPath: " + mUsageDbBackupPath);

                                //Compute checksum of remote script, and see if usage DB exists remotely
                                final String[] remoteMd5ChecksumAndUsageDBCheckOutput = SSHUtils
                                        .getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                                "[ -f " + WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_PATH_REMOTE + " ] && " +
                                                        "md5sum " + WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_PATH_REMOTE + " | awk '{print $1}'",
                                                "[ -f " + USAGE_DB + " ]; echo $?");
                                if (remoteMd5ChecksumAndUsageDBCheckOutput != null && remoteMd5ChecksumAndUsageDBCheckOutput.length > 1) {
                                    remoteChecksum = nullToEmpty(remoteMd5ChecksumAndUsageDBCheckOutput[0]).trim();
                                    final String doesUsageDataExistRemotely = remoteMd5ChecksumAndUsageDBCheckOutput[1];
                                    Crashlytics.log(Log.DEBUG, LOG_TAG, "doesUsageDataExistRemotely: " + doesUsageDataExistRemotely);
                                    if (doesUsageDataExistRemotely != null &&
                                            file.exists() &&
                                            !"0".equals(doesUsageDataExistRemotely.trim())) {
                                        //Usage Data File does not exist - restore what we have on file (if any)
                                        SSHUtils.scpTo(mParentFragmentActivity, mRouter, mGlobalPreferences, mUsageDbBackupPath, USAGE_DB);
                                    }
                                }
                            }


                        } catch (final Exception e) {
                            e.printStackTrace();
                            mUsageDbBackupPath = null;
                        }

                        /** http://www.dd-wrt.com/phpBB2/viewtopic.php?t=75275 */

                        //Copy wrtbwmon file to remote host (/tmp/), if needed
                        Crashlytics.log(Log.DEBUG, LOG_TAG, "[COPY] Copying monitoring script to remote router, if needed...");
                        wrtbwmonScriptPath = new File(mParentFragmentActivity.getCacheDir(), WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_NAME);

                        FileUtils.copyInputStreamToFile(mParentFragmentActivity.getResources().openRawResource(R.raw.wrtbwmon_ddwrtcompanion),
                                wrtbwmonScriptPath);

                        //Compare MD5 checksum locally on remotely. If any differences, overwrite the remote one
                        final String localChecksum = Files.hash(wrtbwmonScriptPath, Hashing.md5()).toString();
                        Crashlytics.log(Log.DEBUG, LOG_TAG, String.format("<localChecksum=%s , remoteChecksum=%s>", localChecksum, remoteChecksum));
                        if (!remoteChecksum.equalsIgnoreCase(localChecksum)) {
                            Crashlytics.log(Log.INFO, LOG_TAG, "Local and remote Checksums for the per-client monitoring script are different " +
                                    "=> uploading the local one...");
                            SSHUtils.scpTo(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                    wrtbwmonScriptPath.getAbsolutePath(),
                                    WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_PATH_REMOTE);
                        }

                        //Run Setup (does not matter if already done)
                        Crashlytics.log(Log.DEBUG, LOG_TAG, "[EXEC] Running per-IP bandwidth monitoring...");

                        final String[] usageDbOutLines = SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
//                                "chmod 700 " + WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_PATH_REMOTE,
                                String.format("chmod 700 %s", WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_PATH_REMOTE),
//                                WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_PATH_REMOTE + " setup 2>/dev/null",
                                String.format("%s setup 2>/dev/null", WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_PATH_REMOTE),
//                                WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_PATH_REMOTE + " read 2>/dev/null",
                                String.format("%s read 2>/dev/null", WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_PATH_REMOTE),
                                "sleep 1",
//                                WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_PATH_REMOTE + " update " + USAGE_DB + " 2>/dev/null",
                                String.format("%s update %s %d 2>/dev/null",
                                        WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_PATH_REMOTE, USAGE_DB, System.currentTimeMillis()),
//                                WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_PATH_REMOTE + " publish-raw " + USAGE_DB + " " + USAGE_DB_OUT + " 2>/dev/null",
                                String.format("%s publish-raw %s %s 2>/dev/null",
                                        WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_PATH_REMOTE, USAGE_DB, USAGE_DB_OUT),
//                                "cat " + USAGE_DB_OUT,
                                String.format("cat %s", USAGE_DB_OUT),
//                                "rm -f " + USAGE_DB_OUT,
                                String.format("rm -f %s", USAGE_DB_OUT));

                        Crashlytics.log(Log.DEBUG, LOG_TAG, "usageDbOutLines: " + Arrays.toString(usageDbOutLines));

                        if (usageDbOutLines != null) {

                            final int z = 80;
                            int t = 1;

                            updateProgressBarViewSeparator(z);
                            mParentFragmentActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mProgressBar.setProgress(z);
                                }
                            });

                            final int usageDbOutLinesLen = usageDbOutLines.length;
                            for (final String usageDbOutLine : usageDbOutLines) {
                                final int x = t;
                                mParentFragmentActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mProgressBarDesc.setText("Retrieving bandwidth monitoring data (" +
                                                x + "/" + usageDbOutLinesLen +
                                                "...");
                                    }
                                });
                                if (isNullOrEmpty(usageDbOutLine)) {
                                    continue;
                                }
                                final List<String> splitToList = Splitter.on(",").omitEmptyStrings().splitToList(usageDbOutLine);
                                if (splitToList.size() < 6) {
                                    Crashlytics.log(Log.WARN, LOG_TAG, "Line split should have more than 6 elements: " + splitToList);
                                    continue;
                                }
                                final String macAddress = nullToEmpty(splitToList.get(0)).toLowerCase();
                                if (isNullOrEmpty(macAddress)) {
                                    continue;
                                }
                                final Device device = macToDevice.get(macAddress.trim());
                                if (device == null) {
                                    continue;
                                }

                                try {
                                    device.setRxTotal(Double.parseDouble(splitToList.get(2)));
                                } catch (final NumberFormatException nfe) {
                                    nfe.printStackTrace();
                                    //no worries
                                }
                                try {
                                    device.setTxTotal(Double.parseDouble(splitToList.get(3)));
                                } catch (final NumberFormatException nfe) {
                                    nfe.printStackTrace();
                                    //no worries
                                }

                                long lastSeen = -1l;
                                try {
                                    lastSeen = Long.parseLong(splitToList.get(1));
                                    device.setLastSeen(lastSeen);
                                } catch (final NumberFormatException nfe) {
                                    nfe.printStackTrace();
                                    //no worries
                                }

                                if (!bandwidthMonitoringIfaceDataPerDevice.containsKey(macAddress)) {
                                    bandwidthMonitoringIfaceDataPerDevice.put(macAddress, new BandwidthMonitoringIfaceData());
                                }
                                final BandwidthMonitoringIfaceData bandwidthMonitoringIfaceData =
                                        bandwidthMonitoringIfaceDataPerDevice.get(macAddress);

                                try {
                                    final double rxRate = Double.parseDouble(splitToList.get(4));
                                    device.setRxRate(rxRate);
                                    if (lastSeen > 0l) {
                                        bandwidthMonitoringIfaceData.addData(IN,
                                                new BandwidthMonitoringTile.DataPoint(lastSeen, rxRate));
                                    }
                                } catch (final NumberFormatException nfe) {
                                    nfe.printStackTrace();
                                    //no worries
                                }
                                try {
                                    final double txRate = Double.parseDouble(splitToList.get(5));
                                    device.setTxRate(txRate);
                                    if (lastSeen > 0l) {
                                        bandwidthMonitoringIfaceData.addData(OUT,
                                                new BandwidthMonitoringTile.DataPoint(lastSeen, txRate));
                                    }
                                } catch (final NumberFormatException nfe) {
                                    nfe.printStackTrace();
                                    //no worries
                                }

                            }
                        }
                    }

                    Crashlytics.log(Log.DEBUG, LOG_TAG, "AFTER usageDataLock");

                    updateProgressBarViewSeparator(90);
                    mParentFragmentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressBar.setProgress(90);
                            mProgressBarDesc.setText("Getting WAN Access Status for each client...");
                        }
                    });

                    //WAN Access
                    final Collection<Device> deviceCollection = macToDevice.values();

                    try {
                        final String[] wanAccessIptablesChainDump = SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                "iptables -L " + DDWRTCOMPANION_WANACCESS_IPTABLES_CHAIN + " --line-numbers -n 2>/dev/null; echo $?");
                        if (wanAccessIptablesChainDump != null) {
                            int exitStatus = -1;
                            if (wanAccessIptablesChainDump.length >= 1) {
                                //Get Command execution status
                                try {
                                    exitStatus = Integer.parseInt(wanAccessIptablesChainDump[wanAccessIptablesChainDump.length - 1]);
                                } catch (final NumberFormatException nfe) {
                                    nfe.printStackTrace();
                                    //No Worries
                                }
                            }
                            if (exitStatus == 0) {
                                for (final Device device : deviceCollection) {
                                    final String macAddr = nullToEmpty(device.getMacAddress()).toLowerCase();
                                    boolean wanAccessDisabled = false;
                                    for (final String wanAccessIptablesChainLine : wanAccessIptablesChainDump) {
                                        if (StringUtils.containsIgnoreCase(wanAccessIptablesChainLine, macAddr)
                                                && StringUtils.containsIgnoreCase(wanAccessIptablesChainLine, "DROP")) {
                                            device.setWanAccessState(Device.WANAccessState.WAN_ACCESS_DISABLED);
                                            wanAccessDisabled = true;
                                            break;
                                        }
                                    }
                                    if (!wanAccessDisabled) {
                                        device.setWanAccessState(Device.WANAccessState.WAN_ACCESS_ENABLED);
                                    }
                                }
                            }
                            //else WAN Access States will remain to 'UNKNOWN' for all devices
                        }
                    } catch (final Exception e) {
                        e.printStackTrace();
                        //No Worries - WAN Access States will remain to 'UNKNOWN'
                    }

                    //Save usage data file
                    final boolean disableBackup = (mParentFragmentPreferences != null &&
                            mParentFragmentPreferences.getBoolean("disableUsageDataAutoBackup", false));
                    Crashlytics.log(Log.DEBUG, LOG_TAG, "disableBackup= " + disableBackup + " - mUsageDbBackupPath: " + mUsageDbBackupPath);
                    if (!disableBackup) {
                        try {
                            if (PermissionChecker.checkSelfPermission(mParentFragmentActivity,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                                    PackageManager.PERMISSION_GRANTED &&
                                    !isNullOrEmpty(mUsageDbBackupPath)) {
                                //Backup to new data file
                                synchronized (usageDataLock) {
                                    if (SSHUtils
                                            .scpFrom(mParentFragmentActivity, mRouter, mGlobalPreferences, USAGE_DB, mUsageDbBackupPath, false)) {
                                        Utils.requestBackup(mParentFragmentActivity);
                                    }

                                }
                            }
                        } catch (final Exception e) {
                            e.printStackTrace();
                            //No worries
                        }
                    }

                    for (final Device device : deviceCollection) {
                        devices.addDevice(device);
                    }

                    updateProgressBarViewSeparator(95);
                    mParentFragmentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressBar.setProgress(95);
                            mProgressBarDesc.setText("Building notification if needed...");
                        }
                    });

                    ConnectedHostsServiceTask.generateConnectedHostsNotification(mParentFragmentActivity,
                            mParentFragmentPreferences, mRouter, deviceCollection);

                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Discovered a total of " + devices.getDevicesCount() + " device(s)!");

                    //Finish by saving BW data at each run
                    Writer writer = null;
                    try {
                        final Map<String, Map<String, Collection<BandwidthMonitoringTile.DataPoint>>> resultToSave = new HashMap<>();
                        for (Map.Entry<String, BandwidthMonitoringTile.BandwidthMonitoringIfaceData> entry : bandwidthMonitoringIfaceDataPerDevice.entrySet()) {
                            final BandwidthMonitoringTile.BandwidthMonitoringIfaceData value = entry.getValue();
                            if (value == null) {
                                continue;
                            }
                            final Map<String, Collection<BandwidthMonitoringTile.DataPoint>> stringListMap = value.toStringListMap();
                            if (stringListMap == null || stringListMap.isEmpty()) {
                                continue;
                            }
                            resultToSave.put(entry.getKey(), stringListMap);
                        }

                        if (!resultToSave.isEmpty()) {
                            writer = new FileWriter(mBandwidthMonitoringData, false);
                            final Gson gson = new GsonBuilder().create();
                            gson.toJson(resultToSave, writer);
                        }

                    } catch (final Exception ignored) {
                        //No worries
                        ignored.printStackTrace();
                    } finally {
                        try {
                            if (writer != null) {
                                writer.close();
                            }
                        } catch (final Exception ignored) {
                            ignored.printStackTrace();
                        }
                    }

                    return devices;

                } catch (@NonNull final Exception e) {
                    Log.e(LOG_TAG, e.getMessage() + ": " + Throwables.getStackTraceAsString(e));
                    return new ClientDevices().setException(e);
                } finally {
                    runBgServiceTaskAsync();
                }
            }
        };

        return this.mCurrentLoader;
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

        try {

            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

            //noinspection ThrowableResultOfMethodCallIgnored
            if (data == null ||
                    (data.getDevices().isEmpty() &&
                            !(data.getException() instanceof DDWRTTileAutoRefreshNotAllowedException))) {
                if (data == null) {
                    data = new ClientDevices().setException(new DDWRTNoDataException("No Data!"));
                }
            }

            final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_clients_error);

            final Exception exception = data.getException();


            if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

                mProgressBar.setProgress(97);
                mProgressBarDesc.setText("Generating views...");

                if (exception == null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }

                mProgressBarDesc.setText("RM >>> Remove all views <<< ");

                final LinearLayout clientsContainer = (LinearLayout) this.layout.findViewById(R.id.tile_status_wireless_clients_layout_list_container);
                clientsContainer.removeAllViews();

                final Resources resources = mParentFragmentActivity.getResources();
                clientsContainer.setBackgroundColor(
                        ContextCompat.getColor(mParentFragmentActivity, android.R.color.transparent));

                //Number of Active Clients
                final int numActiveClients = data.getActiveClientsNum();
                ((TextView) layout.findViewById(R.id.tile_status_wireless_clients_active_clients_num))
                        .setText(numActiveClients >= 0 ? String.valueOf(numActiveClients) : EMPTY_VALUE_TO_DISPLAY);

                //Number of Active DHCP Leases
                final int numActiveDhcpLeases = data.getActiveDhcpLeasesNum();
                ((TextView) layout.findViewById(R.id.tile_status_wireless_clients_active_dhcp_leases_num))
                        .setText(numActiveDhcpLeases >= 0 ? String.valueOf(numActiveDhcpLeases) : EMPTY_VALUE_TO_DISPLAY);

                //Number of Active IP Connections
                final int numActiveIPConnections = data.getActiveIPConnections();
                final TextView activeIpConnectionsNumView = (TextView) layout.findViewById(R.id.tile_status_wireless_clients_active_ip_connections_num);
                activeIpConnectionsNumView
                        .setText(numActiveIPConnections >= 0 ? String.valueOf(numActiveIPConnections) : EMPTY_VALUE_TO_DISPLAY);

                if (numActiveIPConnections > 0) {
                    activeIpConnectionsNumView.setMovementMethod(LinkMovementMethod.getInstance());
                    final Spannable spans = (Spannable) activeIpConnectionsNumView.getText();
                    final ClickableSpan clickSpan = new ClickableSpan() {

                        @Override
                        public void onClick(View widget) {
                            final Intent intent = new Intent(mParentFragmentActivity, ActiveIPConnectionsDetailActivity.class);
                            intent.putExtra(ActiveIPConnectionsDetailActivity.ACTIVE_IP_CONNECTIONS_OUTPUT, activeIPConnections);
                            intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouter.getUuid());
                            intent.putExtra(ActiveIPConnectionsDetailActivity.ROUTER_REMOTE_IP, mRouter.getRemoteIpAddress());
                            intent.putExtra(ActiveIPConnectionsDetailActivity.OBSERVATION_DATE, new Date().toString());

                            final HashMap<String, String> currentIpToHostNameResolverMap = new HashMap<String, String>();
                            if (currentDevicesViewsMap != null) {
                                for (Device device : currentDevicesViewsMap.keySet()) {
                                    if (device == null) {
                                        continue;
                                    }
                                    currentIpToHostNameResolverMap.put(device.getIpAddress(), device.getName());
                                }
                            }
                            intent.putExtra(ActiveIPConnectionsDetailActivity.IP_TO_HOSTNAME_RESOLVER, currentIpToHostNameResolverMap);

                            if (BuildConfig.WITH_ADS &&
                                    mInterstitialAdForActiveIPConnections != null &&
                                    AdUtils.canDisplayInterstialAd(mParentFragmentActivity)) {

                                mInterstitialAdForActiveIPConnections.setAdListener(new AdListener() {
                                    @Override
                                    public void onAdClosed() {
                                        final AdRequest adRequest = AdUtils.buildAdRequest(mParentFragmentActivity);
                                        if (adRequest != null) {
                                            mInterstitialAdForActiveIPConnections.loadAd(adRequest);
                                        }
                                        mParentFragmentActivity.startActivity(intent);
                                    }

                                    @Override
                                    public void onAdOpened() {
                                        //Save preference
                                        mGlobalPreferences.edit()
                                                .putLong(
                                                        DDWRTCompanionConstants.AD_LAST_INTERSTITIAL_PREF,
                                                        System.currentTimeMillis())
                                                .apply();
                                    }
                                });

                                if (mInterstitialAdForActiveIPConnections.isLoaded()) {
                                    mInterstitialAdForActiveIPConnections.show();
                                } else {
                                    //noinspection ConstantConditions
                                    final AlertDialog alertDialog = Utils.buildAlertDialog(mParentFragmentActivity, null,
                                            "Loading...", false, false);
                                    alertDialog.show();
                                    ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            mParentFragmentActivity.startActivity(intent);
                                            alertDialog.cancel();
                                        }
                                    }, 1000);
                                }

                            } else {
                                //noinspection ConstantConditions
                                final AlertDialog alertDialog = Utils.buildAlertDialog(mParentFragmentActivity, null,
                                        "Loading...", false, false);
                                alertDialog.show();
                                ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        mParentFragmentActivity.startActivity(intent);
                                        alertDialog.cancel();
                                    }
                                }, 1000);
                            }
                        }
                    };
                    spans.setSpan(clickSpan, 0, spans.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                final Set<Device> devices = data.getDevices(MAX_CLIENTS_TO_SHOW_IN_TILE);

                //Compute number of wireless clients
                int nbWirelessClients = 0;

                //final int themeBackgroundColor = getThemeBackgroundColor(mParentFragmentActivity, mRouter.getUuid());

                final String expandedClientsPrefKey = \"fake-key\";

                Set<String> expandedClients;

                currentDevicesViewsMap.clear();

                final CardView.LayoutParams cardViewLayoutParams = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT);
                cardViewLayoutParams.rightMargin = R.dimen.marginRight;
                cardViewLayoutParams.leftMargin = R.dimen.marginLeft;
                cardViewLayoutParams.bottomMargin = R.dimen.activity_vertical_margin;

                for (final Device device : devices) {

                    mProgressBarDesc.setText("RM >>> Handling device: '" + device.getName() + "' (" +
                            device.getMacAddress() + ") <<< ");

                    expandedClients = mParentFragmentPreferences.getStringSet(expandedClientsPrefKey, null);
                    if (expandedClients == null) {
                        //Add first item right away
                        mParentFragmentPreferences.edit()
                                .putStringSet(expandedClientsPrefKey, Sets.newHashSet(device.getMacAddress()))
                                .apply();
                    }

                    final CardView cardView = (CardView) mParentFragmentActivity.getLayoutInflater()
                            .inflate(R.layout.tile_status_wireless_client, null);

                    final View legendView = cardView.findViewById(R.id.tile_status_wireless_client_device_details_graph_legend);
                    legendView.setVisibility(View.GONE);

                    //Create Options Menu
                    final ImageButton tileMenu = (ImageButton)
                            cardView.findViewById(R.id.tile_status_wireless_client_device_menu);

                    if (!isThemeLight) {
                        //Set menu background to white
                        tileMenu.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
                    }

                    //Add padding to CardView on v20 and before to prevent intersections between the Card content and rounded corners.
                    cardView.setPreventCornerOverlap(true);
                    //Add padding in API v21+ as well to have the same measurements with previous versions.
                    cardView.setUseCompatPadding(true);

                    if (isThemeLight) {
                        //Light
                        cardView.setCardBackgroundColor(
                                ContextCompat.getColor(mParentFragmentActivity, R.color.cardview_light_background));
                    } else {
                        //Default is Dark
                        cardView.setCardBackgroundColor(
                                ContextCompat.getColor(mParentFragmentActivity, R.color.cardview_dark_background));
                    }

                    //Highlight CardView
//                    cardView.setCardElevation(10f);

                    final ImageView avatarView = (ImageView) cardView.findViewById(R.id.avatar);

                    final String macAddress = device.getMacAddress();

                    final TextView deviceNameView = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_name);
                    final String name = device.getName();
                    final String nameForAvatar;
                    if (isNullOrEmpty(device.getAlias()) &&
                            isNullOrEmpty(device.getSystemName()) &&
                            StringUtils.equals(name, macAddress)) {
                        deviceNameView.setText(EMPTY_VALUE_TO_DISPLAY);
                        nameForAvatar = EMPTY_VALUE_TO_DISPLAY;
                    } else {
                        deviceNameView.setText(name);
                        nameForAvatar = name;
                    }
                    final TextDrawable textDrawable = ImageUtils.getTextDrawable(nameForAvatar);
                    avatarView.setImageDrawable(textDrawable);

                    final TextView rssiTitleView = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_details_wireless_network_rssi_title);
                    final TextView rssiSepView = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_details_wireless_network_rssi_sep);
                    final TextView rssiView = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_details_wireless_network_rssi);

                    final TextView ssidTitleView = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_details_wireless_network_ssid_title);
                    final TextView ssidSepView = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_details_wireless_network_ssid_sep);
                    final TextView ssidView = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_details_wireless_network_ssid);

                    final TextView signalStrengthTitleView = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_details_wireless_network_signal_strength_title);
                    final TextView signalStrengthSepView = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_details_wireless_network_signal_strength_sep);
                    final ProgressBar signalStrengthView = (ProgressBar) cardView.findViewById(R.id.tile_status_wireless_client_device_details_wireless_network_signal_strength);

                    final TextView snrMarginTitleView = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_details_wireless_network_snr_margin_title);
                    final TextView snrMarginSepView = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_details_wireless_network_snr_margin_sep);
                    final TextView snrMarginView = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_details_wireless_network_snr_margin);

                    final View[] wirelessRelatedViews = new View[]{
                            rssiTitleView, rssiSepView, rssiView,
                            ssidTitleView, ssidSepView, ssidView,
                            signalStrengthTitleView, signalStrengthSepView, signalStrengthView,
                            snrMarginTitleView, snrMarginSepView, snrMarginView
                    };

                    //Now if is wireless client or not
                    final Device.WirelessConnectionInfo wirelessConnectionInfo = device.getWirelessConnectionInfo();
                    if (wirelessConnectionInfo != null) {
                        nbWirelessClients++;
                        for (View wirelessRelatedView : wirelessRelatedViews) {
                            wirelessRelatedView.setVisibility(View.VISIBLE);
                        }

                        //SSID
                        final String ssid = wirelessConnectionInfo.getSsid();
                        ssidView.setText(isNullOrEmpty(ssid) ? EMPTY_VALUE_TO_DISPLAY : ssid);

                        //SNR Margin
                        final String snrMargin = wirelessConnectionInfo.getSnrMargin();
                        if (isNullOrEmpty(snrMargin)) {
                            snrMarginView.setText(EMPTY_VALUE_TO_DISPLAY);
                        } else {
                            snrMarginView.setText(snrMargin + " dB");
                        }

                        //Signal Strength (based upon SNR Margin)
                        try {
                            final int snr = Integer.parseInt(snrMargin);

                        /*
                        cf. http://www.wireless-nets.com/resources/tutorials/define_SNR_values.html

                        > 40dB SNR = Excellent signal (5 bars); always associated; lightening fast.

                        25dB to 40dB SNR = Very good signal (3 - 4 bars); always associated; very fast.

                        15dB to 25dB SNR = Low signal (2 bars); always associated; usually fast.

                        10dB - 15dB SNR = Very low signal (1 bar); mostly associated; mostly slow.

                        5dB to 10dB SNR = No signal; not associated; no go.

                        Added +5dB to the values above to approximate Android bar indicators
                         */
                            if (snr <= 20) {
                                //No signal; not associated; no go.
                                deviceNameView
                                        .setCompoundDrawablesWithIntrinsicBounds(
                                                isThemeLight ?
                                                        R.drawable.ic_action_device_signal_wifi_0_bar :
                                                        R.drawable.ic_action_device_signal_wifi_0_bar_white, 0, 0, 0);
                            } else if (snr <= 25) {
                                //Very low signal (1 bar); mostly associated; mostly slow.
                                deviceNameView
                                        .setCompoundDrawablesWithIntrinsicBounds(
                                                isThemeLight ?
                                                        R.drawable.ic_action_device_signal_wifi_1_bar :
                                                        R.drawable.ic_action_device_signal_wifi_1_bar_white, 0, 0, 0);
                            } else if (snr <= 35) {
                                //Low signal (2 bars); always associated; usually fast.
                                deviceNameView
                                        .setCompoundDrawablesWithIntrinsicBounds(
                                                isThemeLight ?
                                                        R.drawable.ic_action_device_signal_wifi_2_bar :
                                                        R.drawable.ic_action_device_signal_wifi_2_bar_white, 0, 0, 0);
                            } else if (snr <= 50) {
                                //Very good signal (3 - 4 bars); always associated; very fast.
                                deviceNameView
                                        .setCompoundDrawablesWithIntrinsicBounds(
                                                isThemeLight ? R.drawable.ic_action_device_signal_wifi_3_bar :
                                                        R.drawable.ic_action_device_signal_wifi_3_bar_white, 0, 0, 0);
                            } else {
                                //Excellent signal (5 bars); always associated; lightening fast.
                                deviceNameView
                                        .setCompoundDrawablesWithIntrinsicBounds(
                                                isThemeLight ? R.drawable.ic_action_device_signal_wifi_4_bar :
                                                        R.drawable.ic_action_device_signal_wifi_4_bar_white, 0, 0, 0);
                            }

                            //Postulate: we consider that a value of 55dB SNR corresponds to 100% in our progress bar
                            signalStrengthView.setProgress(Math.min(snr * 100 / 55, 100));

                            signalStrengthTitleView.setVisibility(View.VISIBLE);
                            signalStrengthSepView.setVisibility(View.VISIBLE);
                            signalStrengthView.setVisibility(View.VISIBLE);
                        } catch (final NumberFormatException nfe) {
                            nfe.printStackTrace();
                            signalStrengthTitleView.setVisibility(View.GONE);
                            signalStrengthSepView.setVisibility(View.GONE);
                            signalStrengthView.setVisibility(View.GONE);
                            deviceNameView
                                    .setCompoundDrawablesWithIntrinsicBounds(
                                            isThemeLight ? R.drawable.ic_action_device_signal_wifi_0_bar :
                                                    R.drawable.ic_action_device_signal_wifi_0_bar_white, 0, 0, 0);
                        }

                        //RSSI
                        final String rssi = wirelessConnectionInfo.getRssi();
                        if (isNullOrEmpty(rssi)) {
                            rssiView.setText(EMPTY_VALUE_TO_DISPLAY);
                        } else {
                            rssiView.setText(rssi + " dBm");
                        }

                    } else {
                        for (View wirelessRelatedView : wirelessRelatedViews) {
                            wirelessRelatedView.setVisibility(View.GONE);
                        }
                    }

                    final Set<String> deviceActiveIpConnections = device.getActiveIpConnections();
                    final TextView deviceActiveIpConnectionsView = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_details_active_ip_connections_num);

                    if (deviceActiveIpConnections == null) {
                        deviceActiveIpConnectionsView.setText(EMPTY_VALUE_TO_DISPLAY);
                    } else {
                        final int deviceActiveIpConnectionsCount = device.getActiveIpConnectionsCount();
                        deviceActiveIpConnectionsView.setText(String.valueOf(deviceActiveIpConnectionsCount));
                        if (deviceActiveIpConnectionsCount > 0) {
                            deviceActiveIpConnectionsView.setMovementMethod(LinkMovementMethod.getInstance());
                            final Spannable spans = (Spannable) deviceActiveIpConnectionsView.getText();
                            final ClickableSpan clickSpan = new ClickableSpan() {

                                @Override
                                public void onClick(View widget) {
                                    final Intent intent = new Intent(mParentFragmentActivity, ActiveIPConnectionsDetailActivity.class);
                                    intent.putExtra(ActiveIPConnectionsDetailActivity.ACTIVE_IP_CONNECTIONS_OUTPUT, deviceActiveIpConnections
                                            .toArray(new String[deviceActiveIpConnections.size()]));
                                    intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouter.getUuid());
                                    intent.putExtra(ActiveIPConnectionsDetailActivity.ROUTER_REMOTE_IP,
                                            mRouter.getRemoteIpAddress());
                                    intent.putExtra(ActiveIPConnectionsDetailActivity.CONNECTED_HOST,
                                            "'" + name + "' (" + macAddress + " - " + device.getIpAddress() + ")");
                                    intent.putExtra(ActiveIPConnectionsDetailActivity.OBSERVATION_DATE, new Date().toString());
                                    intent.putExtra(ActiveIPConnectionsDetailActivity.IP_TO_HOSTNAME_RESOLVER, device.getName());
                                    intent.putExtra(ActiveIPConnectionsDetailActivity.CONNECTED_HOST_IP, device.getIpAddress());

                                    if (BuildConfig.WITH_ADS &&
                                            mInterstitialAdForActiveIPConnections != null &&
                                            AdUtils.canDisplayInterstialAd(mParentFragmentActivity)) {

                                        mInterstitialAdForActiveIPConnections.setAdListener(new AdListener() {
                                            @Override
                                            public void onAdClosed() {
                                                final AdRequest adRequest = AdUtils.buildAdRequest(mParentFragmentActivity);
                                                if (adRequest != null) {
                                                    mInterstitialAdForActiveIPConnections.loadAd(adRequest);
                                                }
                                                mParentFragmentActivity.startActivity(intent);
                                            }

                                            @Override
                                            public void onAdOpened() {
                                                //Save preference
                                                mGlobalPreferences.edit()
                                                        .putLong(
                                                                DDWRTCompanionConstants.AD_LAST_INTERSTITIAL_PREF,
                                                                System.currentTimeMillis())
                                                        .apply();
                                            }
                                        });

                                        if (mInterstitialAdForActiveIPConnections.isLoaded()) {
                                            mInterstitialAdForActiveIPConnections.show();
                                        } else {
                                            //noinspection ConstantConditions
                                            final AlertDialog alertDialog = Utils.buildAlertDialog(mParentFragmentActivity, null,
                                                    "Loading...", false, false);
                                            alertDialog.show();
                                            ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                                            new Handler().postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    mParentFragmentActivity.startActivity(intent);
                                                    alertDialog.cancel();
                                                }
                                            }, 1000);
                                        }

                                    } else {
                                        //noinspection ConstantConditions
                                        final AlertDialog alertDialog = Utils.buildAlertDialog(mParentFragmentActivity, null,
                                                "Loading...", false, false);
                                        alertDialog.show();
                                        ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                mParentFragmentActivity.startActivity(intent);
                                                alertDialog.cancel();
                                            }
                                        }, 1000);
                                    }
                                }
                            };
                            spans.setSpan(clickSpan, 0, spans.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }

                    final Device.WANAccessState wanAccessState = device.getWanAccessState();
                    final boolean isDeviceWanAccessEnabled = (wanAccessState == Device.WANAccessState.WAN_ACCESS_ENABLED);
                    if (isDeviceWanAccessEnabled) {
                        deviceNameView.setTextColor(
                                ContextCompat.getColor(mParentFragmentActivity, R.color.ddwrt_green));
                    }
                    final TextView deviceWanAccessStateView = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_details_wan_access);
                    if (wanAccessState == null || isNullOrEmpty(wanAccessState.toString())) {
                        deviceWanAccessStateView.setText(EMPTY_VALUE_TO_DISPLAY);
                    } else {
                        deviceWanAccessStateView.setText(wanAccessState.toString());
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
                                    .setTextColor(
                                            ContextCompat.getColor(mParentFragmentActivity,
                                                    R.color.blue));
                        }
                        thisDevice.setVisibility(View.VISIBLE);
                    }

                    final LinearLayout deviceDetailsPlaceHolder = (LinearLayout) cardView
                            .findViewById(R.id.tile_status_wireless_client_device_details_graph_placeholder);
                    final View noDataView = cardView.findViewById(R.id.tile_status_wireless_client_device_details_no_data);

                    deviceDetailsPlaceHolder.removeAllViews();

                    final BandwidthMonitoringIfaceData bandwidthMonitoringIfaceData;
                    synchronized (usageDataLock) {
                        bandwidthMonitoringIfaceData = bandwidthMonitoringIfaceDataPerDevice.get(macAddress);
                    }

                    final boolean hideGraphPlaceHolder = bandwidthMonitoringIfaceData == null || bandwidthMonitoringIfaceData.getData().isEmpty();
                    if (hideGraphPlaceHolder) {
                        //Show no data
                        deviceDetailsPlaceHolder.setVisibility(View.GONE);
                        legendView.setVisibility(View.GONE);
                        noDataView.setVisibility(View.VISIBLE);

                    } else {

                        legendView.setVisibility(View.VISIBLE);

                        final Map<String, EvictingQueue<BandwidthMonitoringTile.DataPoint>> dataCircularBuffer =
                                bandwidthMonitoringIfaceData.getData();

                        long maxX = System.currentTimeMillis() + 5000;
                        long minX = System.currentTimeMillis() - 5000;
                        double maxY = 10;
                        double minY = 1.;

                        final XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
                        final XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();

                        final Map<Double, String> yLabels = new HashMap<>();

                        int i = 0;
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
                                yLabels.put(y, byteCountToDisplaySize(Double.valueOf(y).longValue())
                                        .replace("bytes", "B"));
                            }

                            // Now we add our series
                            dataset.addSeries(series);

                            // Now we create the renderer
                            final XYSeriesRenderer renderer = new XYSeriesRenderer();
                            renderer.setLineWidth(5);

                            final int color = ColorUtils.getColor(inOrOut);
                            renderer.setColor(color);
                            // Include low and max value
                            renderer.setDisplayBoundingPoints(true);
                            // we add point markers
                            renderer.setPointStyle(PointStyle.POINT);
                            renderer.setPointStrokeWidth(1);

                            final FillOutsideLine fill = new FillOutsideLine(FillOutsideLine.Type.BOUNDS_ABOVE);
                            //Fill with a slightly transparent version of the original color
                            fill.setColor(android.support.v4.graphics.ColorUtils.setAlphaComponent(color, 30));
                            renderer.addFillOutsideLine(fill);

                            if (i == 0) {
                                cardView.findViewById(R.id.tile_status_wireless_client_device_details_graph_legend_series1_bar)
                                        .setBackgroundColor(color);
                                final TextView series1TextView = (TextView)
                                        cardView.findViewById(R.id.tile_status_wireless_client_device_details_graph_legend_series1_text);
                                series1TextView.setText(inOrOut);
                                series1TextView.setTextColor(color);

                            } else if (i == 1) {
                                cardView.findViewById(R.id.tile_status_wireless_client_device_details_graph_legend_series2_bar)
                                        .setBackgroundColor(color);
                                final TextView series2TextView = (TextView)
                                        cardView.findViewById(R.id.tile_status_wireless_client_device_details_graph_legend_series2_text);
                                series2TextView.setText(inOrOut);
                                series2TextView.setTextColor(color);
                            }
                            i++;

                            mRenderer.addSeriesRenderer(renderer);
                        }

                        mRenderer.setYLabels(0);
                        mRenderer.addYTextLabel(maxY, byteCountToDisplaySize(Double.valueOf(maxY).longValue())
                                .replace("bytes", "B"));
                        if (maxY != 0 && maxY / 2 >= 9000) {
                            mRenderer.addYTextLabel(maxY / 2, byteCountToDisplaySize(Double.valueOf(maxY / 2).longValue())
                                    .replace("bytes", "B"));
                        }

                        // We want to avoid black border
                        //setting text size of the title
                        mRenderer.setChartTitleTextSize(25);
                        //setting text size of the axis title
                        mRenderer.setAxisTitleTextSize(22);
                        //setting text size of the graph label
                        mRenderer.setLabelsTextSize(22);
                        mRenderer.setLegendTextSize(22);

                        mRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00)); // transparent margins
                        // Disable Pan on two axis
                        mRenderer.setPanEnabled(false, false);
                        mRenderer.setYAxisMax(maxY);
                        mRenderer.setYAxisMin(minY);
                        mRenderer.setXAxisMin(minX);
                        mRenderer.setXAxisMax(maxX);
                        mRenderer.setShowGrid(false);
                        mRenderer.setClickEnabled(false);
                        mRenderer.setZoomEnabled(false, false);
                        mRenderer.setPanEnabled(false, false);
                        mRenderer.setZoomRate(6.0f);
                        mRenderer.setShowLabels(true);
                        mRenderer.setFitLegend(true);
                        mRenderer.setInScroll(true);
                        mRenderer.setXLabelsAlign(Paint.Align.CENTER);
                        mRenderer.setYLabelsAlign(Paint.Align.LEFT);
                        mRenderer.setTextTypeface("sans_serif", Typeface.NORMAL);
                        mRenderer.setAntialiasing(true);
                        mRenderer.setExternalZoomEnabled(false);
                        mRenderer.setInScroll(false);
                        mRenderer.setFitLegend(true);
                        mRenderer.setLabelsTextSize(30f);
                        final int blackOrWhite = ContextCompat.getColor(mParentFragmentActivity,
                                ColorUtils.isThemeLight(mParentFragmentActivity) ? R.color.black : R.color.white);
                        mRenderer.setAxesColor(blackOrWhite);
                        mRenderer.setShowLegend(false);
                        mRenderer.setXLabelsColor(blackOrWhite);
                        mRenderer.setYLabelsColor(0, blackOrWhite);

                        final GraphicalView chartView = ChartFactory.getTimeChartView(mParentFragmentActivity, dataset, mRenderer, null);
                        chartView.repaint();

                        deviceDetailsPlaceHolder.addView(chartView, 0);

                        deviceDetailsPlaceHolder.setVisibility(View.VISIBLE);
                        noDataView.setVisibility(View.GONE);
                    }

                    final NetworkTrafficView networkTrafficView =
                            new NetworkTrafficView(mParentFragmentActivity, isThemeLight, mRouter.getUuid(), device);
                    networkTrafficView.setRxAndTxBytes(Double.valueOf(device.getRxRate()).longValue(),
                            Double.valueOf(device.getTxRate()).longValue());

                    final LinearLayout trafficViewPlaceHolder = (LinearLayout) cardView
                            .findViewById(R.id.tile_status_wireless_client_network_traffic_placeholder);
                    trafficViewPlaceHolder.removeAllViews();
                    trafficViewPlaceHolder.addView(networkTrafficView);

                    final TextView deviceSystemNameView = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_details_system_name);
                    final String systemName = device.getSystemName();
                    if (isNullOrEmpty(systemName)) {
                        deviceSystemNameView.setText(EMPTY_VALUE_TO_DISPLAY);
                    } else {
                        deviceSystemNameView.setText(systemName);
                    }

                    final TextView deviceAliasView = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_details_alias);
                    final String alias = device.getAlias();
                    if (isNullOrEmpty(alias)) {
                        deviceAliasView.setText(EMPTY_VALUE_TO_DISPLAY);
                    } else {
                        deviceAliasView.setText(alias);
                    }

                    //OUI Addr
                    final TextView ouiVendorRowView = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_details_oui_addr);
                    final TextView nicManufacturerView = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_details_nic_manufacturer);

                    MultiThreadingManager.getResolutionTasksExecutor()
                            .execute(new UiRelatedTask<Void>() {
                                @Override
                                protected Void doWork() {
                                    try {
                                        device.setMacouiVendorDetails(mMacOuiVendorLookupCache.get(macAddress));
                                    } catch (final Exception e) {
                                        Crashlytics.logException(e);
                                        e.printStackTrace();
                                    }
                                    return null;
                                }

                                @Override
                                protected void thenDoUiRelatedWork(Void aVoid) {
                                    final MACOUIVendor macouiVendorDetails = device.getMacouiVendorDetails();
                                    final String company;
                                    if (macouiVendorDetails == null || (company = macouiVendorDetails.getCompany()) == null || company.isEmpty()) {
                                        if (ouiVendorRowView != null)
                                            ouiVendorRowView.setText(EMPTY_VALUE_TO_DISPLAY);
                                        if (nicManufacturerView != null)
                                            nicManufacturerView.setVisibility(View.GONE);
                                    } else {
                                        if (ouiVendorRowView != null)
                                            ouiVendorRowView.setText(company);
                                        if (nicManufacturerView != null) {
                                            nicManufacturerView.setText(company);
                                            nicManufacturerView.setVisibility(View.VISIBLE);
                                        }
                                    }
                                }
                            });

                    final RelativeTimeTextView lastSeenRowView = (RelativeTimeTextView) cardView.findViewById(R.id.tile_status_wireless_client_device_details_lastseen);
                    final long lastSeen = device.getLastSeen();
                    Crashlytics.log(Log.DEBUG, LOG_TAG, "XXX lastSeen for '" + macAddress + "' =[" + lastSeen + "]");
                    if (lastSeen <= 0) {
                        lastSeenRowView.setText(EMPTY_VALUE_TO_DISPLAY);
                        lastSeenRowView.setReferenceTime(-1l);
                    } else {
                        lastSeenRowView.setReferenceTime(lastSeen);
                        lastSeenRowView.setPrefix(DATE_FORMAT.format(new Date(lastSeen)) + "\n(");
                        lastSeenRowView.setSuffix(")");
                    }

                    final TextView totalDownloadRowView = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_details_total_download);
                    final double rxTotal = device.getRxTotal();
                    if (rxTotal < 0.) {
                        totalDownloadRowView.setText(EMPTY_VALUE_TO_DISPLAY);
                    } else {
                        final long value = Double.valueOf(rxTotal).longValue();
                        totalDownloadRowView.setText(value + " B (" + byteCountToDisplaySize(value) + ")");
                    }

                    final TextView totalUploadRowView = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_details_total_upload);
                    final double txTotal = device.getTxTotal();
                    if (txTotal < 0.) {
                        totalUploadRowView.setText(EMPTY_VALUE_TO_DISPLAY);
                    } else {
                        final long value = Double.valueOf(txTotal).longValue();
                        totalUploadRowView.setText(value + " B (" + byteCountToDisplaySize(value) + ")");
                    }

                    final View ouiAndLastSeenView = cardView.findViewById(R.id.tile_status_wireless_client_device_details_oui_lastseen_table);
                    final View trafficGraphPlaceHolderView = cardView.findViewById(R.id.tile_status_wireless_client_device_details_graph_placeholder);

                    cardView.findViewById(R.id.tile_status_wireless_client_first_glance_view)
                            .setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    final Set<String> clientsExpanded = new HashSet<>(mParentFragmentPreferences
                                            .getStringSet(expandedClientsPrefKey, new HashSet<String>()));

                                    if (ouiAndLastSeenView.getVisibility() == View.VISIBLE) {
                                        ouiAndLastSeenView.setVisibility(View.GONE);
                                        clientsExpanded.remove(macAddress);
//                                        cardView.setCardElevation(40f);
                                    } else {
                                        ouiAndLastSeenView.setVisibility(View.VISIBLE);
                                        clientsExpanded.add(macAddress);
//                                        cardView.setCardElevation(2f);
                                    }
                                    if (hideGraphPlaceHolder) {
                                        trafficGraphPlaceHolderView.setVisibility(View.GONE);
                                        legendView.setVisibility(View.GONE);
                                        if (noDataView.getVisibility() == View.VISIBLE) {
                                            noDataView.setVisibility(View.GONE);
                                        } else {
                                            noDataView.setVisibility(View.VISIBLE);
                                        }
                                    } else {
                                        noDataView.setVisibility(View.GONE);
                                        if (trafficGraphPlaceHolderView.getVisibility() == View.VISIBLE) {
                                            trafficGraphPlaceHolderView.setVisibility(View.GONE);
                                        } else {
                                            trafficGraphPlaceHolderView.setVisibility(View.VISIBLE);
                                        }
                                        if (legendView.getVisibility() == View.VISIBLE) {
                                            legendView.setVisibility(View.GONE);
                                        } else {
                                            legendView.setVisibility(View.VISIBLE);
                                        }

                                    }
                                    mParentFragmentPreferences.edit()
                                            .putStringSet(expandedClientsPrefKey, clientsExpanded)
                                            .apply();
                                }
                            });

                    expandedClients = mParentFragmentPreferences.getStringSet(expandedClientsPrefKey,
                            new HashSet<String>());
                    if (expandedClients.contains(macAddress)) {
//                        cardView.setCardElevation(40f);
                        //Expand detailed view
                        ouiAndLastSeenView.setVisibility(View.VISIBLE);
                        if (hideGraphPlaceHolder) {
                            noDataView.setVisibility(View.VISIBLE);
                            trafficGraphPlaceHolderView.setVisibility(View.GONE);
                            legendView.setVisibility(View.GONE);
                        } else {
                            trafficGraphPlaceHolderView.setVisibility(View.VISIBLE);
                            legendView.setVisibility(View.VISIBLE);
                            noDataView.setVisibility(View.GONE);
                        }
                    } else {
                        //Collapse detailed view
                        ouiAndLastSeenView.setVisibility(View.GONE);
                        trafficGraphPlaceHolderView.setVisibility(View.GONE);
                        legendView.setVisibility(View.GONE);
                        noDataView.setVisibility(View.GONE);
                    }

                    final View wanBlockedDevice = cardView.findViewById(R.id.tile_status_wireless_client_blocked);
                    if (wanAccessState == null || wanAccessState == Device.WANAccessState.WAN_ACCESS_UNKNOWN) {
                        wanBlockedDevice.setVisibility(View.GONE);
                    } else {
                        wanBlockedDevice.setVisibility(isDeviceWanAccessEnabled ? View.GONE : View.VISIBLE);
                    }

                    tileMenu.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final PopupMenu popup = new PopupMenu(mParentFragmentActivity, v);
                            popup.setOnMenuItemClickListener(
                                    new DeviceOnMenuItemClickListener(
                                            deviceNameView,
                                            deviceAliasView,
                                            device));
                            final MenuInflater inflater = popup.getMenuInflater();

                            final Menu menu = popup.getMenu();

                            inflater.inflate(R.menu.tile_status_wireless_client_options, menu);

                            if (isThisDevice) {
                                //WOL not needed as this is the current device
                                menu.findItem(R.id.tile_status_wireless_client_wol).setEnabled(false);
                            }

                            final MenuItem wanAccessStateMenuItem = menu.findItem(R.id.tile_status_wireless_client_wan_access_state);
                            if (wanAccessState == null || wanAccessState == Device.WANAccessState.WAN_ACCESS_UNKNOWN) {
                                wanAccessStateMenuItem.setEnabled(false);
                            } else {
                                wanAccessStateMenuItem.setEnabled(true);
                                wanAccessStateMenuItem.setChecked(isDeviceWanAccessEnabled);
                            }

                            final MenuItem activeIpConnectionsMenuItem = menu
                                    .findItem(R.id.tile_status_wireless_client_view_active_ip_connections);
//                        mClientsActiveConnectionsMenuMap.put(macAddress, activeIpConnectionsMenuItem);
                            final boolean activeIpConnectionsMenuItemEnabled =
                                    !(deviceActiveIpConnections == null || deviceActiveIpConnections.size() == 0);
                            activeIpConnectionsMenuItem.setEnabled(activeIpConnectionsMenuItemEnabled);
                            if (activeIpConnectionsMenuItemEnabled) {
                                activeIpConnectionsMenuItem
                                        .setTitle(mParentFragmentActivity.getResources().getString(R.string.view_active_ip_connections) +
                                                " (" + deviceActiveIpConnections.size() + ")");
                                activeIpConnectionsMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        final Intent intent = new Intent(mParentFragmentActivity, ActiveIPConnectionsDetailActivity.class);
                                        intent.putExtra(ActiveIPConnectionsDetailActivity.ACTIVE_IP_CONNECTIONS_OUTPUT, deviceActiveIpConnections
                                                .toArray(new String[deviceActiveIpConnections.size()]));
                                        intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouter.getUuid());
                                        intent.putExtra(ActiveIPConnectionsDetailActivity.ROUTER_REMOTE_IP, mRouter.getRemoteIpAddress());
                                        intent.putExtra(ActiveIPConnectionsDetailActivity.CONNECTED_HOST,
                                                "'" + name + "' (" + macAddress + " - " + device.getIpAddress() + ")");
                                        intent.putExtra(ActiveIPConnectionsDetailActivity.CONNECTED_HOST_IP, device.getIpAddress());
                                        intent.putExtra(ActiveIPConnectionsDetailActivity.IP_TO_HOSTNAME_RESOLVER, device.getName());
                                        intent.putExtra(ActiveIPConnectionsDetailActivity.OBSERVATION_DATE, new Date().toString());

                                        if (BuildConfig.WITH_ADS &&
                                                mInterstitialAdForActiveIPConnections != null &&
                                                AdUtils.canDisplayInterstialAd(mParentFragmentActivity)) {

                                            mInterstitialAdForActiveIPConnections.setAdListener(new AdListener() {
                                                @Override
                                                public void onAdClosed() {
                                                    final AdRequest adRequest = AdUtils.buildAdRequest(mParentFragmentActivity);
                                                    if (adRequest != null) {
                                                        mInterstitialAdForActiveIPConnections.loadAd(adRequest);
                                                    }
                                                    mParentFragmentActivity.startActivity(intent);
                                                }

                                                @Override
                                                public void onAdOpened() {
                                                    //Save preference
                                                    mGlobalPreferences.edit()
                                                            .putLong(
                                                                    DDWRTCompanionConstants.AD_LAST_INTERSTITIAL_PREF,
                                                                    System.currentTimeMillis())
                                                            .apply();
                                                }
                                            });

                                            if (mInterstitialAdForActiveIPConnections.isLoaded()) {
                                                mInterstitialAdForActiveIPConnections.show();
                                            } else {
                                                //noinspection ConstantConditions
                                                final AlertDialog alertDialog = Utils.buildAlertDialog(mParentFragmentActivity, null,
                                                        "Loading...", false, false);
                                                alertDialog.show();
                                                ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                                                new Handler().postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        mParentFragmentActivity.startActivity(intent);
                                                        alertDialog.cancel();
                                                    }
                                                }, 1000);
                                            }

                                        } else {
                                            //noinspection ConstantConditions
                                            final AlertDialog alertDialog = Utils.buildAlertDialog(mParentFragmentActivity, null,
                                                    "Loading...", false, false);
                                            alertDialog.show();
                                            ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                                            new Handler().postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    mParentFragmentActivity.startActivity(intent);
                                                    alertDialog.cancel();
                                                }
                                            }, 1000);
                                        }

                                        return true;
                                    }
                                });
                            }

                            popup.show();
                        }
                    });

                    currentDevicesViewsMap.put(device, cardView);
                }

                ((TextView) layout.findViewById(R.id.tile_status_wireless_clients_wireless_clients_num))
                        .setText(nbWirelessClients >= 0 ? String.valueOf(nbWirelessClients) : EMPTY_VALUE_TO_DISPLAY);

                //Filters
                Set<Device> newDevices =
                        new HideInactiveClientsFilterVisitorImpl(mParentFragmentPreferences != null &&
                                mParentFragmentPreferences.getBoolean(getFormattedPrefKey(HIDE_INACTIVE_HOSTS), false))
                                .visit(currentDevicesViewsMap.keySet());

                newDevices =
                        new ShowWirelessDevicesOnlyClientsFilterVisitorImpl(mParentFragmentPreferences != null &&
                                mParentFragmentPreferences.getBoolean(getFormattedPrefKey(WIRELESS_DEVICES_ONLY), false))
                                .visit(newDevices);

                newDevices =
                        new ShowOnlyHostsWithWANAccessDisabledFilterVisitorImpl(mParentFragmentPreferences != null &&
                                mParentFragmentPreferences.getBoolean(getFormattedPrefKey(SHOW_ONLY_WAN_ACCESS_DISABLED_HOSTS), false))
                                .visit(newDevices);

                newDevices = applyCurrentSortingStrategy(newDevices);

                int i = 0;
                for (final Device dev : newDevices) {
                    final View view = currentDevicesViewsMap.get(dev);
                    if (view != null) {
                        clientsContainer.addView(view, i++);
                    }
                }

//                final Button showMore = (Button) this.layout.findViewById(R.id.tile_status_wireless_clients_show_more);
//                //Whether to display 'Show more' button
//                if (data.getDevicesCount() > MAX_CLIENTS_TO_SHOW_IN_TILE) {
//                    showMore.setVisibility(View.VISIBLE);
//                    showMore.setOnClickListener(this);
//                } else {
//                    showMore.setVisibility(View.GONE);
//                }

                //Update last sync
                final RelativeTimeTextView lastSyncView = (RelativeTimeTextView) layout.findViewById(R.id.tile_last_sync);
                lastSyncView.setReferenceTime(mLastSync);
                lastSyncView.setPrefix("Last sync: ");
            }

            layout.findViewById(R.id.tile_status_wireless_clients_layout_list_container)
                    .setVisibility(View.VISIBLE);
            layout.findViewById(R.id.tile_status_wireless_clients_togglebutton_container)
                    .setVisibility(View.VISIBLE);


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

            final View tileMenu = layout.findViewById(R.id.tile_status_wireless_clients_menu);
            if (currentDevicesViewsMap.isEmpty()) {
                tileMenu.setVisibility(View.GONE);
            } else {
                tileMenu.setVisibility(View.VISIBLE);
            }

        } finally {
            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished(): done loading!");
            mRefreshing.set(false);

            mProgressBarDesc.setText(null);
            mProgressBarDesc.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.GONE);
            doneWithLoaderInstance(this, loader);
//            doneLoading(loader);
        }
    }

    private void doneLoading(Loader<ClientDevices> loader) {
        if (mParentFragmentPreferences != null &&
                mParentFragmentPreferences.getBoolean(getFormattedPrefKey(RT_GRAPHS), false)) {
            //Reschedule next run right away, to have a pseudo realtime effect, regardless of the actual sync pref!
            //TODO Check how much extra load that represents on the router
            doneWithLoaderInstance(this, loader, TimeUnit.SECONDS.toMillis(10));
        } else {
            //Use classical sync
            doneWithLoaderInstance(this, loader);
        }
    }

    @NonNull
    private Set<Device> applyCurrentSortingStrategy(@NonNull final Set<Device> devicesToSort) {
        Integer currentSortingStrategy = null;
        if (mParentFragmentPreferences != null) {
            currentSortingStrategy = sortIds.inverse()
                    .get(mParentFragmentPreferences.getInt(getFormattedPrefKey(SORTING_STRATEGY),
                            -1));
        }
        if (currentSortingStrategy == null || currentSortingStrategy <= 0) {
            return devicesToSort;
        }

        ClientsSortingVisitor clientsSortingVisitor = null;
        switch (currentSortingStrategy) {
            case R.id.tile_status_wireless_clients_sort_a_z:
            case R.id.tile_status_wireless_clients_sort_z_a:
                clientsSortingVisitor = new ClientsAlphabeticalSortingVisitorImpl(currentSortingStrategy);
                break;
            case R.id.tile_status_wireless_clients_sort_seen_recently:
            case R.id.tile_status_wireless_clients_sort_not_seen_recently:
                clientsSortingVisitor = new LastSeenClientsSortingVisitorImpl(currentSortingStrategy);
                break;
            case R.id.tile_status_wireless_clients_sort_top_senders:
            case R.id.tile_status_wireless_clients_sort_top_receivers:
            case R.id.tile_status_wireless_clients_sort_top_senders_current_rate:
            case R.id.tile_status_wireless_clients_sort_top_receivers_current_rate:
                clientsSortingVisitor = new TopTalkersClientsSortingVisitorImpl(currentSortingStrategy);
                break;
            default:
                break;
        }

        if (clientsSortingVisitor == null) {
            return devicesToSort;
        }

        return clientsSortingVisitor.visit(devicesToSort);
    }

    @Nullable
    @Override
    protected OnClickIntent getOnclickIntent() {
        //TODO
        return null;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final LinearLayout clientsContainer = (LinearLayout) this.layout.findViewById(R.id.tile_status_wireless_clients_layout_list_container);
        final int itemId = item.getItemId();
        switch (itemId) {
            case R.id.tile_status_wireless_clients_realtime_graphs: {
                final boolean rtGraphsEnabled = !item.isChecked();
                if (rtGraphsEnabled) {
                    //Restart loader
                    if (mSupportLoaderManager != null && mCurrentLoader != null) {
                        mSupportLoaderManager.restartLoader(
                                mCurrentLoader.getId(),
                                mFragmentArguments, this);
                    }
                }
                if (mParentFragmentPreferences != null) {
                    mParentFragmentPreferences.edit()
                            .putBoolean(getFormattedPrefKey(RT_GRAPHS), rtGraphsEnabled)
                            .apply();
                }
                return true;
            }
            case R.id.tile_status_wireless_clients_reset_counters: {
                //Reset Counters
                final Bundle token = new Bundle();
                token.putString(ROUTER_ACTION, RouterAction.RESET_COUNTERS.name());

                new UndoBarController.UndoBar(mParentFragmentActivity)
                        .message("Bandwidth Monitoring counters will be reset.")
                        .listener(new MenuActionItemClickListener())
                        .token(token)
                        .show();
                return true;
            }
            case R.id.tile_status_wireless_clients_show_only_hosts_with_wan_access_disabled: {
                //First filter (based on WAN Access State)
                final boolean showOnlyWanAccessDisabledHosts = !item.isChecked();
                Set<Device> newDevices =
                        new ShowOnlyHostsWithWANAccessDisabledFilterVisitorImpl(showOnlyWanAccessDisabledHosts)
                                .visit(currentDevicesViewsMap.keySet());

                //Apply all other visitors
                newDevices =
                        new HideInactiveClientsFilterVisitorImpl(mParentFragmentPreferences != null &&
                                mParentFragmentPreferences.getBoolean(getFormattedPrefKey(HIDE_INACTIVE_HOSTS), false))
                                .visit(newDevices);

                newDevices =
                        new HideInactiveClientsFilterVisitorImpl(mParentFragmentPreferences != null &&
                                mParentFragmentPreferences.getBoolean(getFormattedPrefKey(WIRELESS_DEVICES_ONLY), false))
                                .visit(newDevices);

                newDevices = applyCurrentSortingStrategy(newDevices);

                clientsContainer.removeAllViews();
                for (final Device device : newDevices) {
                    final View view;
                    if ((view = currentDevicesViewsMap.get(device)) != null) {
                        clientsContainer.addView(view);
                    }
                }

                //Save preference
                if (mParentFragmentPreferences != null) {
                    mParentFragmentPreferences.edit()
                            .putBoolean(getFormattedPrefKey(SHOW_ONLY_WAN_ACCESS_DISABLED_HOSTS), showOnlyWanAccessDisabledHosts)
                            .apply();
                }

                return true;
            }
            case R.id.tile_status_wireless_clients_hide_inactive_hosts: {
                final boolean hideInactive = !item.isChecked();

                //Filter
                Set<Device> newDevices =
                        new HideInactiveClientsFilterVisitorImpl(hideInactive).visit(currentDevicesViewsMap.keySet());

                newDevices =
                        new ShowOnlyHostsWithWANAccessDisabledFilterVisitorImpl(mParentFragmentPreferences != null &&
                                mParentFragmentPreferences.getBoolean(getFormattedPrefKey(WIRELESS_DEVICES_ONLY), false))
                                .visit(newDevices);

                newDevices =
                        new ShowOnlyHostsWithWANAccessDisabledFilterVisitorImpl(mParentFragmentPreferences != null &&
                                mParentFragmentPreferences.getBoolean(getFormattedPrefKey(SHOW_ONLY_WAN_ACCESS_DISABLED_HOSTS), false))
                                .visit(newDevices);

                newDevices = applyCurrentSortingStrategy(newDevices);

                clientsContainer.removeAllViews();
                for (final Device device : newDevices) {
                    final View view;
                    if ((view = currentDevicesViewsMap.get(device)) != null) {
                        clientsContainer.addView(view);
                    }
                }

                //Save preference
                if (mParentFragmentPreferences != null) {
                    mParentFragmentPreferences.edit()
                            .putBoolean(getFormattedPrefKey(HIDE_INACTIVE_HOSTS), hideInactive)
                            .apply();
                }
                return true;
            }
            case R.id.tile_status_wireless_clients_wireless_devices_only: {
                final boolean showWirelessOnly = !item.isChecked();

                //Filter
                Set<Device> newDevices =
                        new ShowWirelessDevicesOnlyClientsFilterVisitorImpl(showWirelessOnly)
                                .visit(currentDevicesViewsMap.keySet());

                newDevices =
                        new HideInactiveClientsFilterVisitorImpl(mParentFragmentPreferences != null &&
                                mParentFragmentPreferences.getBoolean(getFormattedPrefKey(HIDE_INACTIVE_HOSTS), false))
                                .visit(newDevices);

                newDevices =
                        new ShowOnlyHostsWithWANAccessDisabledFilterVisitorImpl(mParentFragmentPreferences != null &&
                                mParentFragmentPreferences.getBoolean(getFormattedPrefKey(SHOW_ONLY_WAN_ACCESS_DISABLED_HOSTS), false))
                                .visit(newDevices);

                newDevices = applyCurrentSortingStrategy(newDevices);

                clientsContainer.removeAllViews();
                for (final Device device : newDevices) {
                    final View view;
                    if ((view = currentDevicesViewsMap.get(device)) != null) {
                        clientsContainer.addView(view);
                    }
                }

                //Save preference
                if (mParentFragmentPreferences != null) {
                    mParentFragmentPreferences.edit()
                            .putBoolean(getFormattedPrefKey(WIRELESS_DEVICES_ONLY), showWirelessOnly)
                            .apply();
                }
                return true;
            }
            case R.id.tile_status_wireless_clients_sort_a_z:
            case R.id.tile_status_wireless_clients_sort_z_a:
            case R.id.tile_status_wireless_clients_sort_top_senders:
            case R.id.tile_status_wireless_clients_sort_top_receivers:
            case R.id.tile_status_wireless_clients_sort_top_senders_current_rate:
            case R.id.tile_status_wireless_clients_sort_top_receivers_current_rate:
            case R.id.tile_status_wireless_clients_sort_seen_recently:
            case R.id.tile_status_wireless_clients_sort_not_seen_recently: {
                final boolean hideInactive = (mParentFragmentPreferences != null &&
                        mParentFragmentPreferences.getBoolean(getFormattedPrefKey(HIDE_INACTIVE_HOSTS), false));

                //Filters
                Set<Device> newDevices =
                        new HideInactiveClientsFilterVisitorImpl(hideInactive).visit(currentDevicesViewsMap.keySet());
                newDevices =
                        new ShowOnlyHostsWithWANAccessDisabledFilterVisitorImpl(mParentFragmentPreferences != null &&
                                mParentFragmentPreferences.getBoolean(getFormattedPrefKey(WIRELESS_DEVICES_ONLY), false))
                                .visit(newDevices);
                newDevices =
                        new ShowOnlyHostsWithWANAccessDisabledFilterVisitorImpl(mParentFragmentPreferences != null &&
                                mParentFragmentPreferences.getBoolean(getFormattedPrefKey(SHOW_ONLY_WAN_ACCESS_DISABLED_HOSTS), false))
                                .visit(newDevices);

                ClientsSortingVisitor clientsSortingVisitor = null;
                switch (itemId) {
                    case R.id.tile_status_wireless_clients_sort_a_z:
                    case R.id.tile_status_wireless_clients_sort_z_a:
                        clientsSortingVisitor = new ClientsAlphabeticalSortingVisitorImpl(itemId);
                        break;
                    case R.id.tile_status_wireless_clients_sort_top_senders:
                    case R.id.tile_status_wireless_clients_sort_top_receivers:
                    case R.id.tile_status_wireless_clients_sort_top_senders_current_rate:
                    case R.id.tile_status_wireless_clients_sort_top_receivers_current_rate:
                        clientsSortingVisitor = new TopTalkersClientsSortingVisitorImpl(itemId);
                        break;
                    case R.id.tile_status_wireless_clients_sort_seen_recently:
                    case R.id.tile_status_wireless_clients_sort_not_seen_recently:
                        clientsSortingVisitor = new LastSeenClientsSortingVisitorImpl(itemId);
                        break;
                    default:
                        break;
                }

                newDevices = clientsSortingVisitor.visit(newDevices);

                clientsContainer.removeAllViews();
                for (final Device device : newDevices) {
                    final View view;
                    if ((view = currentDevicesViewsMap.get(device)) != null) {
                        clientsContainer.addView(view);
                    }
                }

                //Save preference
                if (mParentFragmentPreferences != null) {
                    mParentFragmentPreferences.edit()
                            .putInt(getFormattedPrefKey(SORTING_STRATEGY), sortIds.get(itemId))
                            .apply();
                }

                return true;
            }
            case R.id.tile_status_wireless_clients_reset_sort_prefs: {
                if (mParentFragmentPreferences != null) {
                    mParentFragmentPreferences.edit()
                            .remove(getFormattedPrefKey(SORT_TOP_TALKERS))
                            .remove(getFormattedPrefKey(SORT_APHABETICAL))
                            .remove(getFormattedPrefKey(SORT_LAST_SEEN))
                            .remove(getFormattedPrefKey(SORTING_STRATEGY))
                            .apply();
                }
                Utils.displayMessage(mParentFragmentActivity, "Changes will appear upon next sync.", Style.CONFIRM);
                return true;
            }
            default:
                break;
        }

        return false;
    }

    @Override
    public void onShowEvent(@Nullable Bundle bundle) throws Exception {

    }

    @Override
    public void onDismissEventSwipe(int event, @Nullable Bundle bundle) throws Exception {

    }

    @Override
    public void onDismissEventActionClick(int event, @Nullable Bundle bundle) throws Exception {

    }

    @Override
    public void onDismissEventTimeout(final int event, @Nullable final Bundle bundle) throws Exception {

    }

    @Override
    public void onDismissEventManual(int event, @Nullable Bundle bundle) throws Exception {

    }

    @Override
    public void onDismissEventConsecutive(int event, @Nullable Bundle bundle) throws Exception {

    }

    private class MenuActionItemClickListener implements UndoBarController.AdvancedUndoListener, RouterActionListener {

        @Override
        public void onHide(@android.support.annotation.Nullable Parcelable parcelable) {
            if (parcelable instanceof Bundle) {
                final Bundle token = (Bundle) parcelable;
                final String routerAction = token.getString(ROUTER_ACTION);
                Crashlytics.log(Log.DEBUG, LOG_TAG, "routerAction: [" + routerAction + "]");
                if (isNullOrEmpty(routerAction)) {
                    return;
                }
                try {
                    switch (RouterAction.valueOf(routerAction)) {
                        case RESET_COUNTERS:
                            MultiThreadingManager.getMiscTasksExecutor().execute(new Runnable() {
                                @Override
                                public void run() {
                                    synchronized (usageDataLock) {
                                        ActionManager.runTasks(new ResetBandwidthMonitoringCountersRouterAction(
                                                mRouter,
                                                mParentFragmentActivity,
                                                MenuActionItemClickListener.this,
                                                mGlobalPreferences));
                                    }
                                }
                            });
                            break;
                        default:
                            //Ignored
                            break;
                    }
                } catch (IllegalArgumentException | NullPointerException e) {
                    e.printStackTrace();
                    Utils.reportException(null, e);
                }
            }
        }

        @Override
        public void onClear(@NonNull Parcelable[] parcelables) {
            //Nothing to do
        }

        @Override
        public void onUndo(@android.support.annotation.Nullable Parcelable parcelable) {
            //Nothing to do
        }

        @Override
        public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router, Object returnData) {
            switch (routerAction) {
                case RESET_COUNTERS:
                    //Also delete local backup (so it does not get restored on the router)
                    synchronized (usageDataLock) {
                        //Drop local cache
                        //noinspection ResultOfMethodCallIgnored
                        mBandwidthMonitoringData.delete();
                        bandwidthMonitoringIfaceDataPerDevice.clear();
                        //noinspection ResultOfMethodCallIgnored
                        DDWRTCompanionConstants.getClientsUsageDataFile(mParentFragmentActivity, router.getUuid())
                                .delete();
                    }
                    break;
                default:
                    //Ignored
                    break;
            }
            Utils.displayMessage(mParentFragmentActivity,
                    String.format("Action '%s' executed successfully on host '%s'", routerAction.toString(), router.getRemoteIpAddress()),
                    Style.CONFIRM);
        }

        @Override
        public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull Router router, @Nullable Exception exception) {
            Utils.displayMessage(mParentFragmentActivity,
                    String.format("Error on action '%s': %s", routerAction.toString(),
                            Utils.handleException(exception).first),
                    Style.ALERT);
        }
    }

    private class DeviceOnMenuItemClickListener implements
            PopupMenu.OnMenuItemClickListener, UndoBarController.AdvancedUndoListener, RouterActionListener {

        @NonNull
        private final TextView deviceNameView;
        @NonNull
        private final TextView deviceAliasView;
        @NonNull
        private final Device device;

        private DeviceOnMenuItemClickListener(@NonNull TextView deviceNameView,
                                              @NonNull final TextView deviceAliasView,
                                              @NonNull final Device device) {
            this.deviceNameView = deviceNameView;
            this.deviceAliasView = deviceAliasView;
            this.device = device;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            final String macAddress = device.getMacAddress();
            final String deviceName = nullToEmpty(device.getName());

            switch (item.getItemId()) {
                case R.id.tile_status_wireless_client_wan_access_state:
                    if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
                        //Download the full version to unlock this version
                        Utils.displayUpgradeMessage(mParentFragmentActivity, "Enable/Disable Internet Access");
                        return true;
                    }
                    final boolean disableWanAccess = item.isChecked();
                    new AlertDialog.Builder(mParentFragmentActivity)
                            .setIcon(R.drawable.ic_action_alert_warning)
                            .setTitle(String.format("%s WAN Access for '%s' (%s)",
                                    disableWanAccess ? "Disable" : "Enable", deviceName, macAddress))
                            .setMessage(String.format("This allows you to %s WAN (Internet) Access for a particular device.\n" +
                                            "%s\n\n" +
                                            "Note that:\n" +
                                            "- This leverages MAC Addresses, which may be relatively easy to spoof.\n" +
                                            "- This setting will get reverted the next time the router reboots. We are working on making this persistent.",
                                    disableWanAccess ? "disable" : "enable",
                                    disableWanAccess ? String.format("'%s' (%s) will still be able to connect to the router local networks, " +
                                            "but will not be allowed to connect to the outside.", deviceName, macAddress) :
                                            String.format("'%s' (%s) will now be able to get access to the outside.", deviceName, macAddress)))
                            .setCancelable(true)
                            .setPositiveButton(String.format("%s WAN Access!",
                                    disableWanAccess ? "Disable" : "Enable"), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(final DialogInterface dialogInterface, final int i) {
                                    final Bundle token = new Bundle();
                                    token.putString(ROUTER_ACTION,
                                            disableWanAccess ? RouterAction.DISABLE_WAN_ACCESS.name() :
                                                    RouterAction.ENABLE_WAN_ACCESS.name());

                                    new UndoBarController.UndoBar(mParentFragmentActivity)
                                            .message(String.format("WAN Access will be %s for '%s' (%s)",
                                                    disableWanAccess ? "disabled" : "enabled",
                                                    deviceName, macAddress))
                                            .listener(DeviceOnMenuItemClickListener.this)
                                            .token(token)
                                            .show();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //Cancelled - nothing more to do!
                                }
                            }).create().show();
                    return true;
                case R.id.tile_status_wireless_client_wol:
                    //TODO Support SecureOn Password????
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
                                    token.putString(ROUTER_ACTION, RouterAction.WAKE_ON_LAN.name());

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
                        Utils.reportException(null, new
                                IllegalStateException("Click on R.id.tile_status_wireless_client_rename - mParentFragmentPreferences == null"));
                    } else {
                        final String currentAlias =
                                mParentFragmentPreferences.getString(macAddress, null);
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
                                            deviceAliasView.setText(newAlias);
                                            Utils.displayMessage(mParentFragmentActivity, "Alias set! Changes will appear upon next sync.", Style.CONFIRM);
                                        } catch (final Exception e) {
                                            Utils.reportException(null, new
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
                Crashlytics.log(Log.DEBUG, LOG_TAG, "routerAction: [" + routerAction + "]");
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
                                Utils.reportException(null, new IllegalStateException("WOL Internal Error: unable to fetch broadcast addresses. Try again later."));
                                return;
                            }
                            ActionManager.runTasks(new WakeOnLANRouterAction(mRouter, mParentFragmentActivity, this, mGlobalPreferences, device,
                                    broadcastAddresses.toArray(new String[broadcastAddresses.size()])));
                            break;
                        case DISABLE_WAN_ACCESS:
                            ActionManager.runTasks(new DisableWANAccessRouterAction(mRouter, mParentFragmentActivity, this, mGlobalPreferences, device));
                            break;
                        case ENABLE_WAN_ACCESS:
                            ActionManager.runTasks(new EnableWANAccessRouterAction(mRouter, mParentFragmentActivity, this, mGlobalPreferences, device));
                            break;
                        default:
                            //Ignored
                            break;
                    }
                } catch (IllegalArgumentException | NullPointerException e) {
                    e.printStackTrace();
                    Utils.reportException(null, e);
                }
            }
        }

        @Override
        public void onClear(@NonNull Parcelable[] parcelables) {
            //Nothing to do
        }

        @Override
        public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router, Object returnData) {
            Utils.displayMessage(mParentFragmentActivity,
                    String.format("Action '%s' executed successfully on host '%s'", routerAction.toString(), router.getRemoteIpAddress()),
                    Style.CONFIRM);
        }

        @Override
        public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull Router router, @Nullable Exception exception) {
            Utils.displayMessage(mParentFragmentActivity,
                    String.format("Error on action '%s': %s", routerAction.toString(),
                            Utils.handleException(exception).first),
                    Style.ALERT);
        }

        @Override
        public void onUndo(@android.support.annotation.Nullable Parcelable parcelable) {
            //Nothing to do
        }
    }

}