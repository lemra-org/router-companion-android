package org.rm3l.ddwrt.tiles.services.wol;

import android.content.Context;
import android.content.res.Resources;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.gson.GsonBuilder;

import org.apache.commons.lang3.StringUtils;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.resources.Device;
import org.rm3l.ddwrt.resources.MACOUIVendor;
import org.rm3l.ddwrt.resources.None;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.status.wireless.WirelessClientsTile;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.ddwrt.tiles.status.wireless.WirelessClientsTile.MAP_KEYWORD;
import static org.rm3l.ddwrt.tiles.status.wireless.WirelessClientsTile.TEMP_ROUTER_UUID;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.EMPTY_VALUE_TO_DISPLAY;


public class WakeOnLanTile extends DDWRTTile<None> {

    private static final String LOG_TAG = WakeOnLanTile.class.getSimpleName();
    public static final GsonBuilder GSON_BUILDER = new GsonBuilder();
    @NonNull
    private final ArrayList<Device> mDevices = new ArrayList<>();
    private final String wolHostsPrefKey;
    private boolean isThemeLight;
    private String mCurrentIpAddress;
    private String mCurrentMacAddress;
    private ArrayList<String> broadcastAddresses;

    final Router mRouterCopy;

    public static final String ADD_HOST_FRAGMENT_TAG = "add_wol_host";

    public WakeOnLanTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_services_wol_clients,
                R.id.tile_services_wol_clients_togglebutton);
        isThemeLight = ColorUtils.isThemeLight(mParentFragmentActivity);
        //We are cloning the Router, with a new UUID, so as to have a different key into the SSH Sessions Cache
        //This is because we are fetching in a quite real-time manner, and we don't want to block other async tasks.
        mRouterCopy = new Router(mRouter).setUuid(TEMP_ROUTER_UUID);

        wolHostsPrefKey = \"fake-key\";

        // Create Options Menu
        final ImageButton tileMenu = (ImageButton) layout.findViewById(R.id.tile_services_wol_clients_menu);

        if (mRouter == null || mParentFragmentPreferences == null) {
            tileMenu.setVisibility(View.GONE);
        } else {
            tileMenu.setVisibility(View.VISIBLE);
            tileMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    //FIXME Uncomment for final release
//                    if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
//                        Utils.displayUpgradeMessage(mParentFragmentActivity);
//                        return;
//                    }
                    //FIXME End Uncomment

                    final FragmentManager supportFragmentManager = mParentFragmentActivity
                            .getSupportFragmentManager();

                    final Fragment addFragmentExisting = supportFragmentManager.findFragmentByTag(ADD_HOST_FRAGMENT_TAG);
                    if (addFragmentExisting instanceof DialogFragment) {
                        ((DialogFragment) addFragmentExisting).dismiss();
                    }
                    final DialogFragment addFragment = AddWOLHostDialogFragment.newInstance(mRouter.getUuid(), broadcastAddresses, wolHostsPrefKey);
                    addFragment.show(supportFragmentManager, ADD_HOST_FRAGMENT_TAG);

                }
            });
        }

    }

    @Override
    public int getTileHeaderViewId() {
        return R.id.tile_services_wol_clients_hdr;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_services_wol_clients_title;
    }

    @Nullable
    @Override
    protected Loader<None> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<None>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public None loadInBackground() {

                isThemeLight = ColorUtils.isThemeLight(mParentFragmentActivity);

                Log.d(LOG_TAG, "Init background loader for " + WakeOnLanTile.class + ": routerInfo=" +
                        mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                //Determine broadcast address at each run (because that might change if connected to another network)
                try {
                    final WifiManager wifiManager = (WifiManager) mParentFragmentActivity.getSystemService(Context.WIFI_SERVICE);
                    final WifiInfo connectionInfo = wifiManager.getConnectionInfo();

                    mCurrentIpAddress = Utils.intToIp(connectionInfo.getIpAddress());
                    mCurrentMacAddress = connectionInfo.getMacAddress();
                } catch (@NonNull final Exception e) {
                    e.printStackTrace();
                    //No worries
                }

                if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                    //Skip run
                    Log.d(LOG_TAG, "Skip loader run");
                    return (None) new None().setException(new DDWRTTileAutoRefreshNotAllowedException());
                }
                nbRunsLoader++;

                mDevices.clear();

                broadcastAddresses = Lists.newArrayList();

                try {

                    //Get Broadcast Addresses (for WOL)
                    try {
                        final String[] wanAndLanBroadcast = SSHUtils.getManualProperty(mParentFragmentActivity, mRouterCopy, mGlobalPreferences,
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

                    final String[] output = SSHUtils.getManualProperty(mParentFragmentActivity, mRouterCopy,
                            mGlobalPreferences, "grep dhcp-host /tmp/dnsmasq.conf | sed 's/.*=//' | awk -F , '{print \"" +
                                    MAP_KEYWORD +
                                    "\",$1,$3 ,$2}'",
                            "awk '{print \"" +
                                    MAP_KEYWORD +
                                    "\",$2,$3,$4}' /tmp/dnsmasq.leases",
                            "awk 'NR>1{print \"" +
                                    MAP_KEYWORD +
                                    "\",$4,$1,\"*\"}' /proc/net/arp",
                            "arp -a | awk '{print \"" +
                                    MAP_KEYWORD +
                                    "\",$4,$2,$1}'",
                            "echo done");

                    Log.d(LOG_TAG, "output: " + Arrays.toString(output));

                    if (output == null || output.length == 0) {
                        if (output == null) {
                            return new None();
                        }
                    }
                    final Map<String, Device> macToDevice = Maps.newHashMap();
                    final Multimap<String, Device> macToDeviceOutput = HashMultimap.create();

                    final Splitter splitter = Splitter.on(" ");

                    String ipAddress;
                    final Pattern betweenParenthesisPattern = Pattern.compile("\\((.*?)\\)");
                    for (final String stdoutLine : output) {
                        if ("done".equals(stdoutLine)) {
                            break;
                        }
                        final List<String> as = splitter.splitToList(stdoutLine);
                        if (as != null && as.size() >= 4 && MAP_KEYWORD.equals(as.get(0))) {
                            final String macAddress = as.get(1);
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

                            device.setMacouiVendorDetails(WirelessClientsTile.mMacOuiVendorLookupCache.get(macAddress));

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

                        final Device dev = deviceCollection.iterator().next();

                        macToDevice.put(macAddr, dev);
                    }


                    //Load user-defined hosts
                    if (mParentFragmentPreferences != null) {
                        final Set<String> userDefinedWolHosts = mParentFragmentPreferences.getStringSet(wolHostsPrefKey,
                                new HashSet<String>());
                        //noinspection ConstantConditions
                        for (final String userDefinedWolHost : userDefinedWolHosts) {
                            if (userDefinedWolHost == null || userDefinedWolHost.isEmpty()) {
                                continue;
                            }

                            try {
                                final Map objFromJson = GSON_BUILDER.create()
                                        .fromJson(userDefinedWolHost, Map.class);

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

                                deviceFromJson.setMacouiVendorDetails(WirelessClientsTile
                                        .mMacOuiVendorLookupCache.get(macAddress.toString()));

                                mDevices.add(deviceFromJson);

                            } catch (final Exception e) {
                                //No worries
                                e.printStackTrace();
                                Utils.reportException(new
                                        IllegalStateException("Failed to parse JSON: " + userDefinedWolHost, e));
                            }
                        }
                    }

                    //Final operation
                    for (final Device device : macToDevice.values()) {
                        mDevices.add(device);
                    }

                    return new None();

                } catch (@NonNull final Exception e) {
                    Log.e(LOG_TAG, e.getMessage() + ": " + Throwables.getStackTraceAsString(e));
                    return (None) new None().setException(e);
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
    public void onLoadFinished(Loader<None> loader, None data) {
        Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data +
            " / mDevices=" + mDevices);

        //noinspection ThrowableResultOfMethodCallIgnored
        if (data == null ||
                (mDevices.isEmpty() &&
                        !(data.getException() instanceof DDWRTTileAutoRefreshNotAllowedException))) {
            if (data == null) {
                data = (None) new None().setException(new DDWRTNoDataException("No Data!"));
            }
        }

        final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_services_wol_clients_error);

        final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {
            if (exception == null) {
                errorPlaceHolderView.setVisibility(View.GONE);
            }

            final GridLayout clientsContainer = (GridLayout) this.layout.findViewById(R.id.tile_services_wol_clients_layout_list_container);
            clientsContainer.removeAllViews();

            final Resources resources = mParentFragmentActivity.getResources();
            clientsContainer.setBackgroundColor(resources.getColor(android.R.color.transparent));


            final CardView.LayoutParams cardViewLayoutParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT);
            cardViewLayoutParams.rightMargin = R.dimen.marginRight;
            cardViewLayoutParams.leftMargin = R.dimen.marginLeft;
            cardViewLayoutParams.bottomMargin = R.dimen.activity_vertical_margin;

            for (final Device device : mDevices) {
                final CardView cardView = (CardView) mParentFragmentActivity.getLayoutInflater()
                        .inflate(R.layout.tile_services_wol_client, null);

                //Create Options Menu
                final ImageButton tileMenu = (ImageButton) cardView.findViewById(R.id.tile_services_wol_client_device_menu);

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
                    cardView.setCardBackgroundColor(resources.getColor(R.color.cardview_light_background));
                } else {
                    //Default is Dark
                    cardView.setCardBackgroundColor(resources.getColor(R.color.cardview_dark_background));
                }

                //Highlight CardView
                cardView.setCardElevation(20f);

                final String macAddress = device.getMacAddress();

                final TextView deviceNameView = (TextView) cardView.findViewById(R.id.tile_services_wol_client_device_name);
                final String name = device.getName();
                if (isNullOrEmpty(device.getAlias()) &&
                        isNullOrEmpty(device.getSystemName()) &&
                        StringUtils.equals(name, macAddress)) {
                    deviceNameView.setText(EMPTY_VALUE_TO_DISPLAY);
                } else {
                    deviceNameView.setText(name);
                }

                final TextView deviceMac = (TextView) cardView.findViewById(R.id.tile_services_wol_client_device_mac);
                deviceMac.setText(macAddress);

                final TextView deviceIp = (TextView) cardView.findViewById(R.id.tile_services_wol_client_device_ip);
                final String ipAddress = device.getIpAddress();
                deviceIp.setText(ipAddress);

                final boolean isThisDevice = (nullToEmpty(macAddress).equalsIgnoreCase(mCurrentMacAddress) &&
                        nullToEmpty(ipAddress).equals(mCurrentIpAddress));
                if (isThisDevice) {
                    final View thisDevice = cardView.findViewById(R.id.tile_services_wol_client_device_this);
                    if (isThemeLight) {
                        //Set text color to blue
                        ((TextView) thisDevice)
                                .setTextColor(resources.getColor(R.color.blue));
                    }
                    thisDevice.setVisibility(View.VISIBLE);
                }

                //OUI Addr
                final TextView nicManufacturerView = (TextView) cardView.findViewById(R.id.tile_services_wol_client_device_details_nic_manufacturer);
                final MACOUIVendor macouiVendorDetails = device.getMacouiVendorDetails();
                final String company;
                if (macouiVendorDetails == null || (company = macouiVendorDetails.getCompany()) == null || company.isEmpty()) {
                    nicManufacturerView.setVisibility(View.GONE);
                } else {
                    nicManufacturerView.setText(company);
                    nicManufacturerView.setVisibility(View.VISIBLE);
                }

                clientsContainer.addView(cardView);

            }
        }

        layout.findViewById(R.id.tile_services_wol_clients_loading_view)
                .setVisibility(View.GONE);
        layout.findViewById(R.id.tile_services_wol_clients_layout_list_container)
                .setVisibility(View.VISIBLE);
        layout.findViewById(R.id.tile_services_wol_clients_togglebutton_container)
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
        }

        doneWithLoaderInstance(this, loader,
                R.id.tile_services_wol_clients_togglebutton_title, R.id.tile_services_wol_clients_togglebutton_separator);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");
        
    }

}
