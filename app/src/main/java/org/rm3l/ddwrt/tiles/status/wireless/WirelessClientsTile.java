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
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Ordering;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.resources.ClientDevices;
import org.rm3l.ddwrt.resources.Device;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;
import org.rm3l.ddwrt.utils.WoLUtils;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.ddwrt.utils.Utils.getThemeBackgroundColor;
import static org.rm3l.ddwrt.utils.Utils.isThemeLight;

/**
 *
 */
public class WirelessClientsTile extends DDWRTTile<ClientDevices> {

    public static final Comparator<Device> COMPARATOR = new Comparator<Device>() {
        @Override
        public int compare(Device device, Device device2) {
            return Ordering.natural().compare(device.getName(), device2.getName());
        }
    };
    private static final String LOG_TAG = WirelessClientsTile.class.getSimpleName();
    private static final int MAX_CLIENTS_TO_SHOW_IN_TILE = 99;

    private String mBroadcastAddress;
    private String mCurrentIpAddress;

    //Generate a random string, to use as discriminator for determining dhcp clients
    private final String MAP_KEYWORD;

    public WirelessClientsTile(@NotNull SherlockFragment parentFragment, @NotNull Bundle arguments, Router router) {
        super(parentFragment, arguments, router, R.layout.tile_status_wireless_clients, R.id.tile_status_wireless_clients_togglebutton);
        MAP_KEYWORD = WirelessClientsTile.class.getSimpleName() + UUID.randomUUID().toString();
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

                    mCurrentIpAddress = Utils.intToIp(wifiManager.getConnectionInfo().getIpAddress());

                    final InetAddress broadcastAddress = Utils.getBroadcastAddress(wifiManager);
                    if (broadcastAddress != null) {
                        mBroadcastAddress = broadcastAddress.getHostAddress();
                    }
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

                if (DDWRTCompanionConstants.TEST_MODE) {
                    //FIXME TEST MODE
                    for (int i = 1, j = i + 1; i <= 15; i++, j++) {
                        final int randomI = new Random().nextInt(i);
                        final int randomJ = new Random().nextInt(j);
                        devices
                                .addDevice(new Device(String.format("A%1$s:B%1$s:C%1$s:D%2$s:E%2$s:F%2$s", randomI, randomJ))
                                        .setIpAddress(String.format("172.17.1%1$s.2%2$s", randomI, randomJ))
                                        .setSystemName(String.format("Device %1$s-%2$s", randomI, randomJ)));
                    }
                    Log.d(LOG_TAG, "wireless client devices: " + devices);
                    return devices;
                    //FIXME END TEST MODE
                }

                try {
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

                    Log.d(LOG_TAG, "output: " + (output == null ? "NULL" : Arrays.toString(output)));

                    if (output == null) {
                        return null;
                    }


                    for (final String stdoutLine : output) {
                        if ("done".equals(stdoutLine)) {
                            break;
                        }
                        final List<String> as = Splitter.on(" ").splitToList(stdoutLine);
                        if (as != null && as.size() >= 4 && MAP_KEYWORD.equals(as.get(0))) {
                            final String macAddress = as.get(1);
                            if ("00:00:00:00:00:00".equals(macAddress)) {
                                //Skip clients with incomplete ARP set-up
                                continue;
                            }
                            final Device device = new Device(macAddress);
                            device.setIpAddress(as.get(2));

                            final String systemName = as.get(3);
                            if (!"*".equals(systemName)) {
                                device.setSystemName(systemName);
                            }
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

        if (data == null ||
                (data.getDevices().isEmpty() &&
                        !(data.getException() instanceof DDWRTTileAutoRefreshNotAllowedException))) {
            data = new ClientDevices().setException(new DDWRTNoDataException("No Data!"));
        }

        @NotNull final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_clients_error);

        @Nullable final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

            if (exception == null) {
                errorPlaceHolderView.setVisibility(View.GONE);
            }

            final GridLayout clientsContainer = (GridLayout) this.layout.findViewById(R.id.tile_status_wireless_clients_layout_list_container);
            clientsContainer.removeAllViews();
            clientsContainer.setBackgroundColor(mParentFragmentActivity.getResources().getColor(android.R.color.transparent));

            final Set<Device> devices = data.getDevices(MAX_CLIENTS_TO_SHOW_IN_TILE);
            final int themeBackgroundColor = getThemeBackgroundColor(mParentFragmentActivity, mRouter.getUuid());
            final boolean isThemeLight = isThemeLight(mParentFragmentActivity, mRouter.getUuid());
            for (final Device device : devices) {

                final CardView cardView = (CardView) mParentFragmentActivity.getLayoutInflater().inflate(R.layout.tile_status_wireless_client, null);

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

                final TextView deviceName = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_name);
                final String name = device.getName();
                deviceName.setText(name);

                final TextView deviceMac = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_mac);
                final String macAddress = device.getMacAddress();
                deviceMac.setText(macAddress);

                final TextView deviceIp = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_ip);
                final String ipAddress = device.getIpAddress();
                final boolean isThisDevice = (ipAddress != null && ipAddress.equals(mCurrentIpAddress));
                deviceIp.setText(ipAddress);
                if (isThisDevice) {
                    final View thisDevice = cardView.findViewById(R.id.tile_status_wireless_client_device_this);
                    if (isThemeLight) {
                        //Set text color to blue
                        ((TextView) thisDevice)
                                .setTextColor(mParentFragmentActivity.getResources().getColor(R.color.blue));
                    }
                    thisDevice.setVisibility(View.VISIBLE);
                }

                cardView.setOnClickListener(new DeviceOnClickListener(device));

                clientsContainer.addView(cardView);

                tileMenu.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final PopupMenu popup = new PopupMenu(mParentFragmentActivity, v);
                        popup.setOnMenuItemClickListener(new DeviceOnMenuItemClickListener(device));
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
    protected Intent getOnclickIntent() {
        //TODO
        return null;
    }

    private class DeviceOnMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {

        @NotNull
        private final Device device;

        private DeviceOnMenuItemClickListener(@NotNull final Device device) {
            this.device = device;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch(item.getItemId()) {
                case R.id.tile_status_wireless_client_wol:
                    final String deviceName = nullToEmpty(device.getName());
                    final String macAddress = device.getMacAddress();
                    new AlertDialog.Builder(mParentFragmentActivity)
                            .setIcon(R.drawable.ic_action_alert_warning)
                            .setTitle(String.format("Wake up %s (%s)", deviceName, macAddress))
                            .setMessage(String.format("This lets you turn on a computer via the network.\n" +
                                            "For this to work properly:\n" +
                                            "- '%s' (%s) must support Wake-on-LAN. You can enable it in the BIOS.\n" +
                                            "- '%s' (%s) and this mobile device must be on the same network. To wake over the Internet, " +
                                            "you must forward packets from any port you want to the computer you wish to wake.",
                                    deviceName, macAddress, deviceName, macAddress))
                            .setCancelable(true)
                            .setPositiveButton("Send Magic Packet!", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(final DialogInterface dialogInterface, final int i) {
                                    //Send Magic Packet
                                    new WoLUtils.SendWoLMagicPacketAsyncTask(mParentFragmentActivity, device).execute(macAddress, mBroadcastAddress);
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //Cancelled - nothing more to do!
                                }
                            }).create().show();
                    return true;
                default:
                    break;
            }
            return false;
        }
    }

    private class DeviceOnClickListener implements View.OnClickListener {

        @NotNull
        private final Device device;

        private DeviceOnClickListener(@NotNull final Device device) {
            this.device = device;
        }

        @Override
        public void onClick(View v) {
            //Open WebView with MAC OUI Search: http://standards.ieee.org/cgi-bin/ouisearch?f0-b4-79
            final String macAddress = device.getMacAddress();
            if (Strings.isNullOrEmpty(macAddress)) {
                return;
            }

            final String manufacturerPart = macAddress
                    .toUpperCase()
                    .substring(0, 8)
                    .replaceAll(":", "-");

            final WebView webView = new WebView(mParentFragmentActivity);
            webView.loadUrl(String.format("%s%s", DDWRTCompanionConstants.MAC_OUI_SEARCH_URL, manufacturerPart));
            final AlertDialog.Builder dialog = new AlertDialog.Builder(mParentFragmentActivity);
            dialog.setView(webView);
            dialog.setPositiveButton("Close", null);
            dialog.setTitle("OUI Lookup Tool");
            dialog.show();
        }
    }

}
