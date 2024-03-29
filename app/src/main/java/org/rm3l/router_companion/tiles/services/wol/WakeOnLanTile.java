package org.rm3l.router_companion.tiles.services.wol;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.router_companion.RouterCompanionAppConstants.EMPTY_VALUE_TO_DISPLAY;
import static org.rm3l.router_companion.main.DDWRTMainActivity.ROUTER_ACTION;
import static org.rm3l.router_companion.tiles.status.wireless.WirelessClientsTile.MAP_KEYWORD;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.actions.ActionManager;
import org.rm3l.router_companion.actions.RouterAction;
import org.rm3l.router_companion.actions.RouterActionListener;
import org.rm3l.router_companion.actions.WakeOnLANRouterAction;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.resources.Device;
import org.rm3l.router_companion.resources.MACOUIVendor;
import org.rm3l.router_companion.resources.RouterData;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.service.tasks.ConnectedHostsServiceTask;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.tiles.status.wireless.WirelessClientsTile;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.snackbar.SnackbarCallback;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style;
import org.rm3l.router_companion.widgets.home.wol.WOLWidgetProvider;

public class WakeOnLanTile extends DDWRTTile<RouterData<ArrayList<Device>>> {

  public static final GsonBuilder GSON_BUILDER = new GsonBuilder();

  public static final String ADD_HOST_FRAGMENT_TAG = "add_wol_host";

  public static final String EDIT_HOST_FRAGMENT_TAG = "edit_wol_host";

  public static final Splitter SPLITTER = Splitter.on(" ").omitEmptyStrings();

  private static final String LOG_TAG = WakeOnLanTile.class.getSimpleName();

  private static final String wolHostsPrefKey =
      DDWRTTile.getFormattedPrefKey(WakeOnLanTile.class, "wolHosts");

  private static long mLastSync;

  private final ArrayList<String> broadcastAddresses = new ArrayList<>();

  private boolean isThemeLight;

  private final ArrayList<Device> mCurrentDevicesList = new ArrayList<>();

  private String mCurrentIpAddress;

  private String mCurrentMacAddress;

  public static RouterData<ArrayList<Device>> getArrayListRouterDataSync(
      @Nullable WakeOnLanTile wakeOnLanTile,
      Router mRouter,
      Context mParentFragmentActivity,
      List<String> broadcastAddresses,
      SharedPreferences mGlobalPreferences,
      SharedPreferences mParentFragmentPreferences) {

    final long nbRunsLoader = wakeOnLanTile != null ? wakeOnLanTile.getNbRunsLoader() : 0;

    FirebaseCrashlytics.getInstance()
        .log(
            "Init background loader for "
                + WakeOnLanTile.class
                + ": routerInfo="
                + mRouter
                + " / nbRunsLoader="
                + nbRunsLoader);

    if (wakeOnLanTile != null) {
      wakeOnLanTile.updateProgressBarViewSeparator(0);
    }

    // Determine broadcast address at each run (because that might change if connected to another
    // network)
    try {
      final WifiManager wifiManager =
          (WifiManager) mParentFragmentActivity.getSystemService(Context.WIFI_SERVICE);
      final WifiInfo connectionInfo = wifiManager.getConnectionInfo();

      final String mCurrentIpAddress = Utils.decimalToIp4(connectionInfo.getIpAddress());
      final String mCurrentMacAddress = connectionInfo.getMacAddress();

      if (wakeOnLanTile != null) {
        wakeOnLanTile.setmCurrentIpAddress(mCurrentIpAddress);
        wakeOnLanTile.setmCurrentMacAddress(mCurrentMacAddress);
      }
    } catch (@NonNull final Exception e) {
      e.printStackTrace();
      // No worries
    }

    if (wakeOnLanTile != null) {
      wakeOnLanTile.updateProgressBarViewSeparator(20);
      if (wakeOnLanTile.mRefreshing.getAndSet(true)) {
        return new RouterData<ArrayList<Device>>() {}.setException(
            new DDWRTTileAutoRefreshNotAllowedException());
      }
    }
    if (wakeOnLanTile != null) {
      wakeOnLanTile.setNbRunsLoader(nbRunsLoader + 1);
    }

    mLastSync = System.currentTimeMillis();

    final ArrayList<Device> mDevices = new ArrayList<>();

    final List<Device> mCurrentDevicesList =
        (wakeOnLanTile != null ? wakeOnLanTile.getmCurrentDevicesList() : new ArrayList<Device>());

    mCurrentDevicesList.clear();
    broadcastAddresses.clear();

    try {

      // Get Broadcast Addresses (for WOL)
      try {
        if (wakeOnLanTile != null) {
          wakeOnLanTile.updateProgressBarViewSeparator(30);
        }
        final String[] wanAndLanBroadcast =
            SSHUtils.getManualProperty(
                mParentFragmentActivity,
                mRouter,
                mGlobalPreferences,
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
        if (wakeOnLanTile != null) {
          wakeOnLanTile.updateProgressBarViewSeparator(40);
        }
      } catch (final Exception e) {
        // No worries
        e.printStackTrace();
      }

      final String[] output =
          SSHUtils.getManualProperty(
              mParentFragmentActivity,
              mRouter,
              mGlobalPreferences,
              "grep dhcp-host /tmp/dnsmasq.conf | sed 's/.*=//' | awk -F , '{print \""
                  + MAP_KEYWORD
                  + "\",$1,$3 ,$2}'",
              "awk '{print \"" + MAP_KEYWORD + "\",$2,$3,$4}' /tmp/dnsmasq.leases",
              "awk 'NR>1{print \"" + MAP_KEYWORD + "\",$4,$1,\"*\"}' /proc/net/arp",
              "arp -a | awk '{print \"" + MAP_KEYWORD + "\",$4,$2,$1}'",
              "echo done");

      if (wakeOnLanTile != null) {
        wakeOnLanTile.updateProgressBarViewSeparator(60);
      }

      FirebaseCrashlytics.getInstance().log("output: " + Arrays.toString(output));

      if (output == null || output.length == 0) {
        if (output == null) {
          return new RouterData<ArrayList<Device>>() {};
        }
      }
      final Map<String, Device> macToDevice = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
      final Multimap<String, Device> macToDeviceOutput = HashMultimap.create();

      final Splitter splitter = Splitter.on(" ");

      String ipAddress;
      final Pattern betweenParenthesisPattern = Pattern.compile("\\((.*?)\\)");
      for (final String stdoutLine : output) {
        if ("done".equals(stdoutLine)) {
          break;
        }
        final List<String> as = splitter.splitToList(stdoutLine);
        if (as.size() >= 4 && MAP_KEYWORD.equals(as.get(0))) {
          final String macAddress = Strings.nullToEmpty(as.get(1)).toLowerCase();
          if (isNullOrEmpty(macAddress)
              || "00:00:00:00:00:00".equals(macAddress)
              || macAddress.toLowerCase().contains("incomplete")) {
            // Skip clients with incomplete ARP set-up
            continue;
          }

          ipAddress = as.get(2);
          if (ipAddress != null) {
            final Matcher matcher = betweenParenthesisPattern.matcher(ipAddress);
            if (matcher.find()) {
              ipAddress = matcher.group(1);
            }
          }

          //                            if (StringUtils.equalsIgnoreCase(ipAddress, gatewayAddress))
          // {
          //                                //Skip Gateway
          //                                continue;
          //                            }

          final Device device = new Device(macAddress);
          device.setIpAddress(ipAddress);

          final String systemName = as.get(3);
          if (!"*".equals(systemName)) {
            device.setSystemName(systemName);
          }

          // Alias from SharedPreferences
          if (mParentFragmentPreferences != null) {
            final String deviceAlias = mParentFragmentPreferences.getString(macAddress, null);
            if (!isNullOrEmpty(deviceAlias)) {
              device.setAlias(deviceAlias);
            }
          }

          try {
            device.setMacouiVendorDetails(
                WirelessClientsTile.mMacOuiVendorLookupCache.get(macAddress));
          } catch (final Exception e) {
            e.printStackTrace();
            Utils.reportException(mParentFragmentActivity, e);
          }

          macToDeviceOutput.put(macAddress, device);
        }
      }

      for (final Map.Entry<String, Collection<Device>> deviceEntry :
          macToDeviceOutput.asMap().entrySet()) {
        final String macAddr = deviceEntry.getKey();
        final Collection<Device> deviceCollection = deviceEntry.getValue();
        for (final Device device : deviceCollection) {
          // Consider the one that has a Name, if any
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

      if (wakeOnLanTile != null) {
        wakeOnLanTile.updateProgressBarViewSeparator(70);
      }

      if (wakeOnLanTile != null) {
        wakeOnLanTile.runBgServiceTaskAsync();
        try {
          ConnectedHostsServiceTask.Companion.generateConnectedHostsNotification(
              wakeOnLanTile.mParentFragmentActivity, mRouter, macToDevice.values());
        } catch (final Exception e) {
          // No worries
          FirebaseCrashlytics.getInstance()
              .log("Failed to generate connected hosts notification: " + e.getMessage());
          Utils.reportException(mParentFragmentActivity, e);
        }
        //                new Handler(Looper.getMainLooper()).post(new Runnable() {
        //                    @Override
        //                    public void run() {
        //                        try {
        //                            tile.routerModelUpdaterServiceTask
        //                                    .runBackgroundServiceTask(router);
        //                            tile.routerInfoForFeedbackServiceTask
        //                                    .runBackgroundServiceTask(router);
        //                        } catch (final Exception e) {
        //                            //No worries
        //                            e.printStackTrace();
        //                        } finally {
        //
        // ConnectedHostsServiceTask.generateConnectedHostsNotification(tile.mParentFragmentActivity,
        //                                    tile.mParentFragmentPreferences, router,
        // macToDevice.values());
        //                        }
        //                    }
        //                });
      }

      // Load user-defined hosts
      if (mParentFragmentPreferences != null) {
        final Set<String> userDefinedWolHosts =
            mParentFragmentPreferences.getStringSet(wolHostsPrefKey, new HashSet<String>());
        //noinspection ConstantConditions
        for (final String userDefinedWolHost : userDefinedWolHosts) {
          if (userDefinedWolHost == null || userDefinedWolHost.isEmpty()) {
            continue;
          }

          try {
            final Map objFromJson = GSON_BUILDER.create().fromJson(userDefinedWolHost, Map.class);

            final Object macAddress = objFromJson.get("macAddress");
            if (macAddress == null || macToDevice.containsKey(macAddress.toString())) {
              continue;
            }

            final Device deviceFromJson = new Device(macAddress.toString());
            final Object alias = objFromJson.get("alias");
            if (alias != null) {
              deviceFromJson.setAlias(alias.toString());
            }

            final Object ipAddress1 = objFromJson.get("ipAddress");
            if (ipAddress1 != null) {
              deviceFromJson.setIpAddress(ipAddress1.toString());
            }

            final Object deviceUuidForWolFromPrefs = objFromJson.get("deviceUuidForWol");
            deviceFromJson.setDeviceUuidForWol(
                deviceUuidForWolFromPrefs != null ? deviceUuidForWolFromPrefs.toString() : null);

            deviceFromJson.setIsEditableForWol(true);

            try {
              deviceFromJson.setMacouiVendorDetails(
                  WirelessClientsTile.mMacOuiVendorLookupCache.get(macAddress.toString()));
            } catch (final Exception e) {
              e.printStackTrace();
              Utils.reportException(mParentFragmentActivity, e);
            }

            mDevices.add(deviceFromJson);
            mCurrentDevicesList.add(deviceFromJson);
          } catch (final Exception e) {
            // No worries
            e.printStackTrace();
            Utils.reportException(
                null, new IllegalStateException("Failed to parse JSON: " + userDefinedWolHost, e));
          }
        }
      }

      // Connected Hosts
      for (final Device device : macToDevice.values()) {
        mDevices.add(device);
        mCurrentDevicesList.add(device);
      }

      if (wakeOnLanTile != null) {
        wakeOnLanTile.updateProgressBarViewSeparator(80);
      }

      // Hosts defined from Admin > WOL
      final NVRAMInfo nvRamInfoFromRouter =
          SSHUtils.getNVRamInfoFromRouter(
              mParentFragmentActivity,
              mRouter,
              mGlobalPreferences,
              NVRAMInfo.Companion.getWOL_HOSTS(),
              NVRAMInfo.Companion.getMANUAL_WOL_MAC(),
              NVRAMInfo.Companion.getMANUAL_WOL_PORT());
      if (nvRamInfoFromRouter != null) {
        // Manual Hosts
        String property =
            nvRamInfoFromRouter.getProperty(
                NVRAMInfo.Companion.getMANUAL_WOL_MAC(), RouterCompanionAppConstants.EMPTY_STRING);
        final List<String> macAddresses = SPLITTER.splitToList(property);

        if (!macAddresses.isEmpty()) {
          int wolPort = -1;
          try {
            wolPort =
                Integer.parseInt(
                    nvRamInfoFromRouter.getProperty(
                        NVRAMInfo.Companion.getMANUAL_WOL_PORT(), "-1"));
          } catch (final Exception e) {
            e.printStackTrace();
          }

          for (final String mAddress : macAddresses) {
            final Device dev = new Device(mAddress);
            dev.setWolPort(wolPort);
            try {
              dev.setMacouiVendorDetails(
                  WirelessClientsTile.mMacOuiVendorLookupCache.get(mAddress));
            } catch (final Exception e) {
              e.printStackTrace();
              Utils.reportException(mParentFragmentActivity, e);
            }
            mDevices.add(dev);
            mCurrentDevicesList.add(dev);
          }
        }

        // Now Wol_Hosts
        property =
            nvRamInfoFromRouter.getProperty(
                NVRAMInfo.Companion.getWOL_HOSTS(), RouterCompanionAppConstants.EMPTY_STRING);
        final List<String> wolHosts = SPLITTER.splitToList(property);
        for (final String wolHost : wolHosts) {
          final List<String> strings = Splitter.on("=").omitEmptyStrings().splitToList(wolHost);
          if (strings.isEmpty()) {
            continue;
          }
          final Device dev = new Device(strings.get(0));
          if (strings.size() >= 2) {
            dev.setAlias(strings.get(1));
          }
          try {
            dev.setMacouiVendorDetails(
                WirelessClientsTile.mMacOuiVendorLookupCache.get(dev.getMacAddress()));
          } catch (final Exception e) {
            e.printStackTrace();
            Utils.reportException(mParentFragmentActivity, e);
          }

          mDevices.add(dev);
          mCurrentDevicesList.add(dev);
        }
      }

      if (wakeOnLanTile != null) {
        wakeOnLanTile.updateProgressBarViewSeparator(90);
      }

      return new RouterData<ArrayList<Device>>() {}.setData(mDevices);
    } catch (@NonNull final Exception e) {
      FirebaseCrashlytics.getInstance()
          .log(e.getMessage() + ": " + Throwables.getStackTraceAsString(e));
      return new RouterData<ArrayList<Device>>() {}.setException(e);
    }
  }

  @NonNull
  public static Loader<RouterData<ArrayList<Device>>> getWOLHostsLoader(
      @Nullable final WakeOnLanTile wakeOnLanTile,
      final Context mParentFragmentActivity,
      final SharedPreferences mParentFragmentPreferences,
      final SharedPreferences mGlobalPreferences,
      final Router mRouter,
      final List<String> broadcastAddresses) {
    return new AsyncTaskLoader<RouterData<ArrayList<Device>>>(mParentFragmentActivity) {

      @Nullable
      @Override
      public RouterData<ArrayList<Device>> loadInBackground() {

        return getArrayListRouterDataSync(
            wakeOnLanTile,
            mRouter,
            mParentFragmentActivity,
            broadcastAddresses,
            mGlobalPreferences,
            mParentFragmentPreferences);
      }
    };
  }

  public WakeOnLanTile(
      @NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
    super(parentFragment, arguments, router, R.layout.tile_services_wol_clients, null);
    isThemeLight = ColorUtils.Companion.isThemeLight(mParentFragmentActivity);

    // Create Options Menu

    final ImageButton addMenu = (ImageButton) layout.findViewById(R.id.tile_services_wol_add_menu);

    if (mRouter == null || mParentFragmentPreferences == null) {
      addMenu.setVisibility(View.GONE);
    } else {
      addMenu.setVisibility(View.VISIBLE);
      addMenu.setOnClickListener(
          new View.OnClickListener() {
            @Override
            public void onClick(View view) {

              if (BuildConfig.DONATIONS) {
                Utils.displayUpgradeMessage(mParentFragmentActivity, "Add new WOL Host");
                return;
              }

              final FragmentManager supportFragmentManager =
                  mParentFragmentActivity.getSupportFragmentManager();

              final Fragment addFragmentExisting =
                  supportFragmentManager.findFragmentByTag(ADD_HOST_FRAGMENT_TAG);
              if (addFragmentExisting instanceof DialogFragment) {
                ((DialogFragment) addFragmentExisting).dismiss();
              }
              final DialogFragment addFragment =
                  AddWOLHostDialogFragment.newInstance(
                      mRouter.getUuid(), broadcastAddresses, wolHostsPrefKey);
              addFragment.show(supportFragmentManager, ADD_HOST_FRAGMENT_TAG);
            }
          });
    }
  }

  @Override
  public Integer getTileHeaderViewId() {
    return R.id.tile_services_wol_clients_hdr;
  }

  @Override
  public Integer getTileTitleViewId() {
    return R.id.tile_services_wol_clients_title;
  }

  public ArrayList<Device> getmCurrentDevicesList() {
    return mCurrentDevicesList;
  }

  public String getmCurrentIpAddress() {
    return mCurrentIpAddress;
  }

  public void setmCurrentIpAddress(String mCurrentIpAddress) {
    this.mCurrentIpAddress = mCurrentIpAddress;
  }

  public String getmCurrentMacAddress() {
    return mCurrentMacAddress;
  }

  public void setmCurrentMacAddress(String mCurrentMacAddress) {
    this.mCurrentMacAddress = mCurrentMacAddress;
  }

  @Override
  public void onLoadFinished(
      Loader<RouterData<ArrayList<Device>>> loader, RouterData<ArrayList<Device>> data) {
    try {
      FirebaseCrashlytics.getInstance().log("onLoadFinished: loader=" + loader + " / data=" + data);

      isThemeLight = ColorUtils.Companion.isThemeLight(mParentFragmentActivity);

      final ImageButton globalTileMenu =
          (ImageButton) layout.findViewById(R.id.tile_services_wol_clients_menu);
      if (!isThemeLight) {
        // Set menu background to white
        globalTileMenu.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
      }
      if (mCurrentDevicesList.isEmpty()) {
        globalTileMenu.setVisibility(View.GONE);
      } else {
        globalTileMenu.setVisibility(View.VISIBLE);

        final Collection<Device> devicesEditableForWol =
            Collections2.filter(
                mCurrentDevicesList,
                new Predicate<Device>() {
                  @Override
                  public boolean apply(@Nullable Device input) {
                    return (input != null && input.isEditableForWol());
                  }
                });

        globalTileMenu.setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                final PopupMenu popup = new PopupMenu(mParentFragmentActivity, v);
                popup.setOnMenuItemClickListener(
                    new PopupMenu.OnMenuItemClickListener() {
                      @Override
                      public boolean onMenuItemClick(MenuItem menuItem) {
                        int i = menuItem.getItemId();
                        if (i == R.id.tile_services_wol_clients_wake_all) {
                          if (BuildConfig.DONATIONS) {
                            Utils.displayUpgradeMessage(mParentFragmentActivity, "Wake all hosts");
                            return true;
                          }

                          final Bundle token = new Bundle();
                          token.putString(ROUTER_ACTION, RouterAction.WAKE_ON_LAN.name());

                          SnackbarUtils.buildSnackbar(
                              mParentFragmentActivity,
                              String.format(
                                  "WOL Request will be sent from router to %d hosts",
                                  mCurrentDevicesList.size()),
                              "CANCEL",
                              Snackbar.LENGTH_LONG,
                              new SnackbarCallback() {

                                @Override
                                public void onDismissEventTimeout(int event, @Nullable Bundle token)
                                    throws Exception {
                                  final String routerAction =
                                      token != null ? token.getString(ROUTER_ACTION) : null;
                                  FirebaseCrashlytics.getInstance()
                                      .log("routerAction: [" + routerAction + "]");
                                  if (isNullOrEmpty(routerAction)) {
                                    return;
                                  }
                                  try {
                                    switch (RouterAction.valueOf(routerAction)) {
                                      case WAKE_ON_LAN:
                                        final AtomicInteger currentNum = new AtomicInteger(0);
                                        final AtomicInteger numActionsWithNoSuccess =
                                            new AtomicInteger(0);
                                        final int totalNumOfDevices = mCurrentDevicesList.size();

                                        final WakeOnLANRouterAction[] wolActions =
                                            new WakeOnLANRouterAction[totalNumOfDevices];
                                        int i = 0;
                                        for (final Device device : mCurrentDevicesList) {
                                          wolActions[i++] =
                                              new WakeOnLANRouterAction(
                                                  mRouter,
                                                  mParentFragmentActivity,
                                                  new RouterActionListener() {
                                                    @Override
                                                    public void onRouterActionFailure(
                                                        @NonNull RouterAction routerAction,
                                                        @NonNull Router router,
                                                        @Nullable Exception exception) {
                                                      final int incrementAndGet =
                                                          currentNum.incrementAndGet();
                                                      numActionsWithNoSuccess.incrementAndGet();
                                                      if (incrementAndGet >= totalNumOfDevices) {
                                                        // An error occurred
                                                        Utils.displayMessage(
                                                            mParentFragmentActivity,
                                                            String.format(
                                                                "Action '%s' executed but %d error(s) occurred: %s",
                                                                routerAction.toString(),
                                                                numActionsWithNoSuccess.get(),
                                                                Utils.handleException(exception)
                                                                    .first),
                                                            Style.INFO);
                                                      }
                                                    }

                                                    @Override
                                                    public void onRouterActionSuccess(
                                                        @NonNull RouterAction routerAction,
                                                        @NonNull Router router,
                                                        Object returnData) {
                                                      final int incrementAndGet =
                                                          currentNum.incrementAndGet();
                                                      if (incrementAndGet >= totalNumOfDevices) {
                                                        final int numActionsThatDidNotSucceed =
                                                            numActionsWithNoSuccess.get();
                                                        if (numActionsThatDidNotSucceed > 0) {
                                                          // An error occurred
                                                          Utils.displayMessage(
                                                              mParentFragmentActivity,
                                                              String.format(
                                                                  "Action '%s' executed but %d error(s) occurred",
                                                                  routerAction.toString(),
                                                                  numActionsThatDidNotSucceed),
                                                              Style.INFO);
                                                        } else {
                                                          // No error
                                                          Utils.displayMessage(
                                                              mParentFragmentActivity,
                                                              String.format(
                                                                  "Action '%s' executed successfully on host '%s'",
                                                                  routerAction.toString(),
                                                                  router.getRemoteIpAddress()),
                                                              Style.CONFIRM);
                                                        }
                                                      }
                                                    }
                                                  },
                                                  mGlobalPreferences,
                                                  device,
                                                  device.getWolPort(),
                                                  broadcastAddresses.toArray(
                                                      new String[broadcastAddresses.size()]));
                                        }
                                        ActionManager.runTasks(wolActions);
                                        break;
                                      default:
                                        // Ignored
                                        break;
                                    }
                                  } catch (IllegalArgumentException | NullPointerException e) {
                                    e.printStackTrace();
                                    Utils.displayMessage(
                                        mParentFragmentActivity,
                                        "WOL Internal Error. Try again later.",
                                        Style.ALERT);
                                    Utils.reportException(null, e);
                                  }
                                }
                              },
                              token,
                              true);

                          // new UndoBarController.UndoBar(mParentFragmentActivity).message(
                          //    String.format("WOL Request will be sent from router to %d hosts",
                          //        mCurrentDevicesList.size()))
                          //    .listener(new UndoBarController.AdvancedUndoListener() {
                          //      @Override public void onHide(@Nullable Parcelable parcelable) {
                          //
                          //        if (parcelable instanceof Bundle) {
                          //
                          //        }
                          //      }
                          //
                          //      @Override public void onClear(@NonNull Parcelable[] parcelables) {
                          //
                          //      }
                          //
                          //      @Override public void onUndo(@Nullable Parcelable parcelable) {
                          //
                          //      }
                          //    })
                          //    .token(token)
                          //    .show();
                          return true;
                        } else if (i == R.id.tile_services_wol_clients_delete_all) {
                          new AlertDialog.Builder(mParentFragmentActivity)
                              .setIcon(R.drawable.ic_action_alert_warning)
                              .setTitle("Delete WOL Hosts?")
                              .setMessage(
                                  "Only hosts defined manually ("
                                      + devicesEditableForWol.size()
                                      + ") will be removed!")
                              .setCancelable(true)
                              .setPositiveButton(
                                  "Delete",
                                  new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                        final DialogInterface dialogInterface, final int i) {
                                      if (mParentFragmentPreferences != null) {
                                        mParentFragmentPreferences
                                            .edit()
                                            .remove(wolHostsPrefKey)
                                            .apply();

                                        Utils.displayMessage(
                                            mParentFragmentActivity,
                                            devicesEditableForWol.size()
                                                + " item(s) deleted - list will refresh upon next sync",
                                            Style.CONFIRM);
                                        // Request Backup
                                        Utils.requestBackup(mParentFragmentActivity);
                                      }
                                    }
                                  })
                              .setNegativeButton(
                                  "Cancel",
                                  new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                      // Cancelled - nothing more to do!
                                    }
                                  })
                              .create()
                              .show();
                          return true;
                        } else {
                        }
                        return false;
                      }
                    });

                final MenuInflater inflater = popup.getMenuInflater();

                final Menu menu = popup.getMenu();

                inflater.inflate(R.menu.tile_services_wol_clients_options, menu);

                if (mCurrentDevicesList.isEmpty()) {
                  menu.findItem(R.id.tile_services_wol_clients_wake_all).setVisible(false);
                }
                if (devicesEditableForWol.isEmpty()) {
                  menu.findItem(R.id.tile_services_wol_clients_delete_all).setVisible(false);
                }

                popup.show();
              }
            });
      }

      //noinspection ThrowableResultOfMethodCallIgnored
      if (data == null) {
        data =
            new RouterData<ArrayList<Device>>() {}.setException(
                new DDWRTNoDataException("No Data!"));
      }

      final ArrayList<Device> mDevices =
          (data.getData() != null ? data.getData() : new ArrayList<Device>());
      if (mDevices.isEmpty()
          && !(data.getException() instanceof DDWRTTileAutoRefreshNotAllowedException)) {
        data =
            new RouterData<ArrayList<Device>>() {}.setException(
                new DDWRTNoDataException("No Data!"));
      }

      final TextView errorPlaceHolderView =
          (TextView) this.layout.findViewById(R.id.tile_services_wol_clients_error);

      final Exception exception = data.getException();

      if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {
        if (exception == null) {
          errorPlaceHolderView.setVisibility(View.GONE);
        }

        final GridLayout clientsContainer =
            (GridLayout)
                this.layout.findViewById(R.id.tile_services_wol_clients_layout_list_container);
        clientsContainer.removeAllViews();

        final Resources resources = mParentFragmentActivity.getResources();
        clientsContainer.setBackgroundColor(
            ContextCompat.getColor(mParentFragmentActivity, android.R.color.transparent));

        final CardView.LayoutParams cardViewLayoutParams =
            new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        cardViewLayoutParams.rightMargin = R.dimen.marginRight;
        cardViewLayoutParams.leftMargin = R.dimen.marginLeft;
        cardViewLayoutParams.bottomMargin = R.dimen.activity_vertical_margin;

        if (!mDevices.isEmpty()) {
          //                if (mParentFragmentPreferences != null) {
          //                    //Get all widget IDs related to this router only, and multicast to
          // that group only
          //                    final Set<String> widgetsWol = mParentFragmentPreferences
          //                            .getStringSet(WOLWidgetConfigureActivity.WIDGETS_WOL, new
          // HashSet<String>());
          //                    if (widgetsWol != null && !widgetsWol.isEmpty()) {
          //                        final Integer[] widgetIds =
          // FluentIterable.from(widgetsWol).transform(new Function<String, Integer>() {
          //                            @Override
          //                            public Integer apply(@Nullable String input) {
          //                                if (input == null) {
          //                                    return null;
          //                                }
          //                                try {
          //                                    return Integer.parseInt(input);
          //                                } catch (NumberFormatException nfe) {
          //                                    nfe.printStackTrace();
          //                                    Utils.reportException(nfe);
          //                                    return null;
          //                                }
          //                            }
          //                        }).filter(Predicates.notNull()).toArray(Integer.class);
          //
          //                        if (widgetIds != null) {
          //                            final int[] widgetIdsInt = new int[widgetIds.length];
          //                            for (int i = 0, widgetIdsLength = widgetIds.length; i <
          // widgetIdsLength; i++) {
          //                                widgetIdsInt[i] = widgetIds[i];
          //                            }
          //
          //                            final AppWidgetManager widgetManager =
          // AppWidgetManager.getInstance(mParentFragmentActivity);
          //                            widgetManager.notifyAppWidgetViewDataChanged(
          //                                    widgetIdsInt, R.id.wol_widget_hosts_list);
          //                        }
          //                    }
          //                }

          // Notify widgets that data has changed
          final AppWidgetManager widgetManager =
              AppWidgetManager.getInstance(mParentFragmentActivity);
          widgetManager.notifyAppWidgetViewDataChanged(
              widgetManager.getAppWidgetIds(
                  WOLWidgetProvider.getComponentName(mParentFragmentActivity)),
              R.id.wol_widget_hosts_list);
        }

        for (final Device device : mDevices) {
          final CardView cardView =
              (CardView)
                  mParentFragmentActivity
                      .getLayoutInflater()
                      .inflate(R.layout.tile_services_wol_client, null);

          // Add padding to CardView on v20 and before to prevent intersections between the Card
          // content and rounded corners.
          cardView.setPreventCornerOverlap(true);
          // Add padding in API v21+ as well to have the same measurements with previous versions.
          cardView.setUseCompatPadding(true);

          if (isThemeLight) {
            // Light
            cardView.setCardBackgroundColor(
                ContextCompat.getColor(mParentFragmentActivity, R.color.cardview_light_background));
          } else {
            // Default is Dark
            cardView.setCardBackgroundColor(
                ContextCompat.getColor(mParentFragmentActivity, R.color.cardview_dark_background));
          }

          // Highlight CardView
          //                cardView.setCardElevation(20f);

          final String macAddress = device.getMacAddress();

          final TextView deviceNameView =
              (TextView) cardView.findViewById(R.id.tile_services_wol_client_device_name);
          final String name = device.getName();
          if (isNullOrEmpty(device.getAlias())
              && isNullOrEmpty(device.getSystemName())
              && name.equals(macAddress)) {
            deviceNameView.setText(EMPTY_VALUE_TO_DISPLAY);
          } else {
            deviceNameView.setText(name);
          }

          if (device.isEditableForWol()) {
            deviceNameView.setTypeface(null, Typeface.ITALIC);
          } else {
            deviceNameView.setTypeface(null, Typeface.NORMAL);
          }

          final TextView deviceMac =
              (TextView) cardView.findViewById(R.id.tile_services_wol_client_device_mac);
          deviceMac.setText(macAddress);

          final TextView deviceIp =
              (TextView) cardView.findViewById(R.id.tile_services_wol_client_device_ip);
          final String ipAddress = device.getIpAddress();
          deviceIp.setText(ipAddress);

          final boolean isThisDevice =
              (nullToEmpty(macAddress).equalsIgnoreCase(mCurrentMacAddress)
                  && nullToEmpty(ipAddress).equals(mCurrentIpAddress));
          if (isThisDevice) {
            final View thisDevice =
                cardView.findViewById(R.id.tile_services_wol_client_device_this);
            if (isThemeLight) {
              // Set text color to blue
              ((TextView) thisDevice)
                  .setTextColor(ContextCompat.getColor(mParentFragmentActivity, R.color.blue));
            }
            thisDevice.setVisibility(View.VISIBLE);
          }

          // OUI Addr
          final TextView nicManufacturerView =
              (TextView)
                  cardView.findViewById(
                      R.id.tile_services_wol_client_device_details_nic_manufacturer);
          final MACOUIVendor macouiVendorDetails = device.getMacouiVendorDetails();
          final String company;
          if (macouiVendorDetails == null
              || (company = macouiVendorDetails.getCompany()) == null
              || company.isEmpty()) {
            nicManufacturerView.setVisibility(View.GONE);
          } else {
            nicManufacturerView.setText(company);
            nicManufacturerView.setVisibility(View.VISIBLE);
          }

          // Create Options Menu
          final ImageButton tileMenu =
              (ImageButton) cardView.findViewById(R.id.tile_services_wol_client_device_menu);

          if (!isThemeLight) {
            // Set menu background to white
            tileMenu.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
          }

          tileMenu.setOnClickListener(
              new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                  final PopupMenu popup = new PopupMenu(mParentFragmentActivity, v);
                  popup.setOnMenuItemClickListener(buildDeviceMenuItemClickListener(device));

                  final MenuInflater inflater = popup.getMenuInflater();

                  final Menu menu = popup.getMenu();

                  inflater.inflate(R.menu.tile_services_wol_client_options, menu);

                  final boolean editableForWol = device.isEditableForWol();

                  menu.findItem(R.id.tile_services_wol_client_delete).setVisible(editableForWol);
                  menu.findItem(R.id.tile_services_wol_client_rename).setVisible(editableForWol);

                  popup.show();
                }
              });

          cardView.setOnLongClickListener(
              new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                  final PopupMenu popup = new PopupMenu(mParentFragmentActivity, v);
                  popup.setOnMenuItemClickListener(buildDeviceMenuItemClickListener(device));

                  final MenuInflater inflater = popup.getMenuInflater();

                  final Menu menu = popup.getMenu();

                  inflater.inflate(R.menu.tile_services_wol_client_options, menu);

                  final boolean editableForWol = device.isEditableForWol();

                  menu.findItem(R.id.tile_services_wol_client_delete).setVisible(editableForWol);
                  menu.findItem(R.id.tile_services_wol_client_rename).setVisible(editableForWol);

                  popup.show();

                  return true;
                }
              });

          cardView.setOnClickListener(
              new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                  final Bundle token = new Bundle();
                  token.putString(ROUTER_ACTION, RouterAction.WAKE_ON_LAN.name());

                  SnackbarUtils.buildSnackbar(
                      mParentFragmentActivity,
                      String.format(
                          "WOL Request will be sent from router to '%s' (%s)",
                          device.getName(), device.getMacAddress()),
                      "CANCEL",
                      Snackbar.LENGTH_LONG,
                      getSnackbarCb(device),
                      token,
                      true);

                  // new UndoBarController.UndoBar(mParentFragmentActivity).message(
                  //    String.format("WOL Request will be sent from router to '%s' (%s)",
                  //        device.getName(), device.getMacAddress()))
                  //    .listener(getSnackbarCb(device))
                  //    .token(token)
                  //    .show();
                }
              });

          clientsContainer.addView(cardView);
        }

        // Update last sync
        final RelativeTimeTextView lastSyncView =
            (RelativeTimeTextView) layout.findViewById(R.id.tile_last_sync);
        lastSyncView.setReferenceTime(mLastSync);
        lastSyncView.setPrefix("Last sync: ");
      }

      layout.findViewById(R.id.tile_services_wol_clients_loading_view).setVisibility(View.GONE);

      layout
          .findViewById(R.id.tile_services_wol_clients_layout_list_container)
          .setVisibility(View.VISIBLE);

      layout
          .findViewById(R.id.tile_services_wol_clients_togglebutton_container)
          .setVisibility(View.VISIBLE);

      if (exception != null && !(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

        //noinspection ThrowableResultOfMethodCallIgnored
        final Throwable rootCause = Throwables.getRootCause(exception);
        errorPlaceHolderView.setText(
            "Error: " + (rootCause != null ? rootCause.getMessage() : "null"));
        final Context parentContext = this.mParentFragmentActivity;
        errorPlaceHolderView.setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(final View v) {
                //noinspection ThrowableResultOfMethodCallIgnored
                if (rootCause != null) {
                  Toast.makeText(parentContext, rootCause.getMessage(), Toast.LENGTH_LONG).show();
                }
              }
            });
        errorPlaceHolderView.setVisibility(View.VISIBLE);
        updateProgressBarWithError();
      } else if (exception == null) {
        updateProgressBarWithSuccess();
      }

      FirebaseCrashlytics.getInstance().log("onLoadFinished(): done loading!");
    } finally {
      mRefreshing.set(false);
      doneWithLoaderInstance(this, loader);
    }
  }

  @Nullable
  @Override
  protected Loader<RouterData<ArrayList<Device>>> getLoader(int id, Bundle args) {
    return getWOLHostsLoader(
        this,
        mParentFragmentActivity,
        mParentFragmentPreferences,
        mGlobalPreferences,
        mRouter,
        broadcastAddresses);
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

  private PopupMenu.OnMenuItemClickListener buildDeviceMenuItemClickListener(final Device device) {
    return new PopupMenu.OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(MenuItem menuItem) {
        int i = menuItem.getItemId();
        if (i == R.id.tile_services_wol_client_rename) {
          try {
            final FragmentManager supportFragmentManager =
                mParentFragmentActivity.getSupportFragmentManager();

            final Fragment editFragmentExisting =
                supportFragmentManager.findFragmentByTag(EDIT_HOST_FRAGMENT_TAG);
            if (editFragmentExisting instanceof DialogFragment) {
              ((DialogFragment) editFragmentExisting).dismiss();
            }
            final DialogFragment editFragment =
                EditWOLHostDialogFragment.newInstance(
                    mRouter.getUuid(),
                    broadcastAddresses,
                    wolHostsPrefKey,
                    GSON_BUILDER.create().toJson(device));
            editFragment.show(supportFragmentManager, EDIT_HOST_FRAGMENT_TAG);
          } catch (final Exception e) {
            Utils.reportException(null, e);
            Toast.makeText(
                    mParentFragmentActivity,
                    "Internal Error - please try again later.",
                    Toast.LENGTH_SHORT)
                .show();
          }

          return true;
        } else if (i == R.id.tile_services_wol_client_delete) {
          if (!device.isEditableForWol()) {
            Utils.displayMessage(
                mParentFragmentActivity,
                "Operation allowed for user-defined hosts only",
                Style.INFO);
            return true;
          }
          new AlertDialog.Builder(mParentFragmentActivity)
              .setIcon(R.drawable.ic_action_alert_warning)
              .setTitle("Delete?")
              .setMessage(
                  String.format(
                      "'%s' (%s) will be removed!", device.getName(), device.getMacAddress()))
              .setCancelable(true)
              .setPositiveButton(
                  "Delete",
                  new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialogInterface, final int i) {
                      if (mParentFragmentPreferences != null) {
                        final HashSet<String> stringHashSet =
                            new HashSet<>(
                                mParentFragmentPreferences.getStringSet(
                                    wolHostsPrefKey, new HashSet<String>()));
                        final HashSet<String> newSet = new HashSet<>();
                        for (final String s : stringHashSet) {
                          if (s == null) {
                            continue;
                          }
                          if (s.toLowerCase().contains(device.getMacAddress().toLowerCase())) {
                            continue;
                          }
                          newSet.add(s);
                        }
                        mParentFragmentPreferences
                            .edit()
                            .putStringSet(wolHostsPrefKey, newSet)
                            .apply();

                        Utils.displayMessage(
                            mParentFragmentActivity,
                            "Item dropped - list will refresh upon next sync",
                            Style.CONFIRM);
                        // Request Backup
                        Utils.requestBackup(mParentFragmentActivity);
                      }
                    }
                  })
              .setNegativeButton(
                  "Cancel",
                  new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                      // Cancelled - nothing more to do!
                    }
                  })
              .create()
              .show();
          return true;
        } else if (i == R.id.tile_services_wol_client_wol) {
          final Bundle token = new Bundle();
          token.putString(ROUTER_ACTION, RouterAction.WAKE_ON_LAN.name());

          SnackbarUtils.buildSnackbar(
              mParentFragmentActivity,
              String.format(
                  "WOL Request will be sent from router to '%s' (%s)",
                  device.getName(), device.getMacAddress()),
              "CANCEL",
              Snackbar.LENGTH_LONG,
              getSnackbarCb(device),
              token,
              true);

          // new UndoBarController.UndoBar(mParentFragmentActivity).message(
          //    String.format("WOL Request will be sent from router to '%s' (%s)", device.getName(),
          //        device.getMacAddress()))
          //    .listener(getSnackbarCb(device))
          //    .token(token)
          //    .show();

          return true;
        } else {
        }
        return false;
      }
    };
  }

  private SnackbarCallback getSnackbarCb(final Device device) {
    return new SnackbarCallback() {

      @Override
      public void onDismissEventTimeout(int event, @Nullable Bundle token) throws Exception {
        final String routerAction = token != null ? token.getString(ROUTER_ACTION) : null;
        FirebaseCrashlytics.getInstance().log("routerAction: [" + routerAction + "]");
        if (isNullOrEmpty(routerAction)) {
          return;
        }
        try {
          switch (RouterAction.valueOf(routerAction)) {
            case WAKE_ON_LAN:
              ActionManager.runTasks(
                  new WakeOnLANRouterAction(
                      mRouter,
                      mParentFragmentActivity,
                      new RouterActionListener() {
                        @Override
                        public void onRouterActionFailure(
                            @NonNull RouterAction routerAction,
                            @NonNull Router router,
                            @Nullable Exception exception) {
                          Utils.displayMessage(
                              mParentFragmentActivity,
                              String.format(
                                  "Error on action '%s': %s",
                                  routerAction.toString(), Utils.handleException(exception).first),
                              Style.ALERT);
                        }

                        @Override
                        public void onRouterActionSuccess(
                            @NonNull RouterAction routerAction,
                            @NonNull Router router,
                            Object returnData) {
                          Utils.displayMessage(
                              mParentFragmentActivity,
                              String.format(
                                  "Action '%s' executed successfully on host '%s'",
                                  routerAction.toString(), router.getRemoteIpAddress()),
                              Style.CONFIRM);
                        }
                      },
                      mGlobalPreferences,
                      device,
                      device.getWolPort(),
                      broadcastAddresses.toArray(new String[broadcastAddresses.size()])));
              break;
            default:
              // Ignored
              break;
          }
        } catch (IllegalArgumentException | NullPointerException e) {
          e.printStackTrace();
          Utils.displayMessage(
              mParentFragmentActivity, "WOL Internal Error. Try again later.", Style.ALERT);
          Utils.reportException(null, e);
        }
      }
    };
  }
}
