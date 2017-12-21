package org.rm3l.router_companion.tiles.status.wireless;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.router_companion.RouterCompanionAppConstants.EMPTY_VALUE_TO_DISPLAY;
import static org.rm3l.router_companion.tiles.status.wireless.WirelessClientsTile.DATE_FORMAT;
import static org.rm3l.router_companion.tiles.status.wireless.WirelessClientsTile.EXPANDED_CLIENTS_PREF_KEY;
import static org.rm3l.router_companion.tiles.status.wireless.WirelessClientsTile.mMacOuiVendorLookupCache;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.amulyakhare.textdrawable.TextDrawable;
import com.crashlytics.android.Crashlytics;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Sets;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import needle.UiRelatedTask;
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.common.utils.ViewIDUtils;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.multithreading.MultiThreadingManager;
import org.rm3l.router_companion.resources.Device;
import org.rm3l.router_companion.resources.Device.WANAccessState;
import org.rm3l.router_companion.resources.MACOUIVendor;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.status.bandwidth.BandwidthMonitoringTile;
import org.rm3l.router_companion.tiles.status.wireless.WirelessClientsTile.DeviceOnMenuItemClickListener;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.ImageUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.kotlin.ViewUtils;
import org.rm3l.router_companion.widgets.NetworkTrafficView;

/**
 * Created by rm3l on 2/9/17.
 */

public class WirelessClientsRecyclerViewAdapter extends
        RecyclerView.Adapter<WirelessClientsRecyclerViewAdapter.WirelessClientsRecyclerViewHolder> {

    static class WirelessClientsRecyclerViewHolder extends RecyclerView.ViewHolder {

        final ImageView avatarView;

        final CardView cardView;

        final Context context;

        final TextView deviceNameView;

        final View legendView;

        final TextView rssiTitleView;

        final ImageButton tileMenu;

        private final ImageButton actionsSetAliasButton;

        private final SwitchCompat actionsToggleInternetAccessButton;

        private final View actionsView;

        private final ImageButton actionsViewIpConnectionsButton;

        private final ImageButton actionsWakeOnLanButton;

        private final LinearLayout detailsPlaceHolderView;

        private final TextView deviceActiveIpConnectionsView;

        private final TextView deviceAliasView;

        private final LinearLayout deviceDetailsPlaceHolder;

        private final TextView deviceIp;

        private final TextView deviceMac;

        private final TextView deviceSystemNameView;

        private final TextView deviceWanAccessStateView;

        private final ImageButton expandCollapseButton;

        private final View firstGlanceView;

        private final RelativeTimeTextView lastSeenRowView;

        private final TextView nicManufacturerView;

        private final View noDataView;

        private final View ouiAndLastSeenView;

        private final TextView ouiVendorRowView;

        private final TextView rssiSepView;

        private final TextView rssiView;

        private final View series1BarView;

        private final TextView series1TextView;

        private final View series2BarView;

        private final TextView series2TextView;

        private final TextView signalStrengthSepView;

        private final TextView signalStrengthTitleView;

        private final ProgressBar signalStrengthView;

        private final TextView snrMarginSepView;

        private final TextView snrMarginTitleView;

        private final TextView snrMarginView;

        private final TextView ssidSepView;

        private final TextView ssidTitleView;

        private final TextView ssidView;

        private final View thisDevice;

        private final TextView totalDownloadRowView;

        private final TextView totalUploadRowView;

        private final View trafficGraphPlaceHolderView;

        private final LinearLayout trafficViewPlaceHolder;

        private final View wanBlockedDevice;

        private final View[] wirelessRelatedViews;

        WirelessClientsRecyclerViewHolder(Context context, View itemView) {
            super(itemView);
            this.cardView = (CardView) itemView;
            this.context = context;

            this.actionsView = cardView.findViewById(R.id.tile_status_wireless_client_card_view_actions_background);
            this.actionsSetAliasButton = this.actionsView
                    .findViewById(R.id.tile_status_wireless_client_card_view_actions_set_alias);
            this.actionsWakeOnLanButton = this.actionsView
                    .findViewById(R.id.tile_status_wireless_client_card_view_actions_wake_on_lan);
            this.actionsViewIpConnectionsButton = this.actionsView
                    .findViewById(R.id.tile_status_wireless_client_card_view_actions_view_ip_conn);
            this.actionsToggleInternetAccessButton = this.actionsView
                    .findViewById(R.id.tile_status_wireless_client_card_view_actions_toggle_internet_access);

            this.legendView =
                    cardView.findViewById(R.id.tile_status_wireless_client_device_details_graph_legend);
            this.tileMenu =
                    (ImageButton) cardView.findViewById(R.id.tile_status_wireless_client_device_menu);

            avatarView = (ImageView) cardView.findViewById(R.id.avatar);

            deviceNameView =
                    (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_name);

            this.rssiTitleView = (TextView) cardView.findViewById(
                    R.id.tile_status_wireless_client_device_details_wireless_network_rssi_title);
            this.rssiSepView = (TextView) cardView.findViewById(
                    R.id.tile_status_wireless_client_device_details_wireless_network_rssi_sep);
            this.rssiView = (TextView) cardView.findViewById(
                    R.id.tile_status_wireless_client_device_details_wireless_network_rssi);

            this.ssidTitleView = (TextView) cardView.findViewById(
                    R.id.tile_status_wireless_client_device_details_wireless_network_ssid_title);
            this.ssidSepView = (TextView) cardView.findViewById(
                    R.id.tile_status_wireless_client_device_details_wireless_network_ssid_sep);
            this.ssidView = (TextView) cardView.findViewById(
                    R.id.tile_status_wireless_client_device_details_wireless_network_ssid);

            this.signalStrengthTitleView = (TextView) cardView.findViewById(
                    R.id.tile_status_wireless_client_device_details_wireless_network_signal_strength_title);
            this.signalStrengthSepView = (TextView) cardView.findViewById(
                    R.id.tile_status_wireless_client_device_details_wireless_network_signal_strength_sep);
            this.signalStrengthView = (ProgressBar) cardView.findViewById(
                    R.id.tile_status_wireless_client_device_details_wireless_network_signal_strength);

            this.snrMarginTitleView = (TextView) cardView.findViewById(
                    R.id.tile_status_wireless_client_device_details_wireless_network_snr_margin_title);
            this.snrMarginSepView = (TextView) cardView.findViewById(
                    R.id.tile_status_wireless_client_device_details_wireless_network_snr_margin_sep);
            this.snrMarginView = (TextView) cardView.findViewById(
                    R.id.tile_status_wireless_client_device_details_wireless_network_snr_margin);

            this.wirelessRelatedViews = new View[]{
                    rssiTitleView, rssiSepView, rssiView, ssidTitleView, ssidSepView, ssidView,
                    signalStrengthTitleView, signalStrengthSepView, signalStrengthView, snrMarginTitleView,
                    snrMarginSepView, snrMarginView
            };

            deviceActiveIpConnectionsView = (TextView) cardView.findViewById(
                    R.id.tile_status_wireless_client_device_details_active_ip_connections_num);

            deviceWanAccessStateView = (TextView) cardView.findViewById(
                    R.id.tile_status_wireless_client_device_details_wan_access);

            deviceMac = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_mac);

            deviceIp = (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_ip);

            thisDevice = cardView.findViewById(R.id.tile_status_wireless_client_device_this);

            deviceDetailsPlaceHolder = cardView.findViewById(
                    R.id.tile_status_wireless_client_device_details_graph_placeholder);

            noDataView = cardView.findViewById(R.id.tile_status_wireless_client_device_details_no_data);

            series1BarView = cardView.findViewById(
                    R.id.tile_status_wireless_client_device_details_graph_legend_series1_bar);

            series1TextView = (TextView) cardView.findViewById(
                    R.id.tile_status_wireless_client_device_details_graph_legend_series1_text);

            series2BarView = cardView.findViewById(
                    R.id.tile_status_wireless_client_device_details_graph_legend_series2_bar);
            series2TextView = (TextView) cardView.findViewById(
                    R.id.tile_status_wireless_client_device_details_graph_legend_series2_text);

            trafficViewPlaceHolder = (LinearLayout) cardView.findViewById(
                    R.id.tile_status_wireless_client_network_traffic_placeholder);
            deviceSystemNameView = (TextView) cardView.findViewById(
                    R.id.tile_status_wireless_client_device_details_system_name);

            deviceAliasView =
                    (TextView) cardView.findViewById(R.id.tile_status_wireless_client_device_details_alias);

            ouiVendorRowView = (TextView) cardView.findViewById(
                    R.id.tile_status_wireless_client_device_details_oui_addr);
            nicManufacturerView = (TextView) cardView.findViewById(
                    R.id.tile_status_wireless_client_device_details_nic_manufacturer);

            lastSeenRowView = (RelativeTimeTextView) cardView.findViewById(
                    R.id.tile_status_wireless_client_device_details_lastseen);

            totalDownloadRowView = (TextView) cardView.findViewById(
                    R.id.tile_status_wireless_client_device_details_total_download);

            totalUploadRowView = (TextView) cardView.findViewById(
                    R.id.tile_status_wireless_client_device_details_total_upload);

            ouiAndLastSeenView =
                    cardView.findViewById(R.id.tile_status_wireless_client_device_details_oui_lastseen_table);

            trafficGraphPlaceHolderView =
                    cardView.findViewById(R.id.tile_status_wireless_client_device_details_graph_placeholder);

            firstGlanceView = cardView.findViewById(R.id.tile_status_wireless_client_first_glance_view);

            this.expandCollapseButton = cardView.findViewById(R.id.expand_collapse);

            this.detailsPlaceHolderView = cardView
                    .findViewById(R.id.tile_status_wireless_client_device_details_placeholder);

            wanBlockedDevice = cardView.findViewById(R.id.tile_status_wireless_client_blocked);
        }
    }

    public static final String LOG_TAG = WirelessClientsRecyclerViewAdapter.class.getSimpleName();

    private final WirelessClientsTile clientsTile;

    private List<Device> devices;

    private final Router router;

    public WirelessClientsRecyclerViewAdapter(WirelessClientsTile clientsTile, Router router) {
        this.clientsTile = clientsTile;
        this.router = router;
    }

    @Override
    public int getItemCount() {
        return devices != null ? devices.size() : 0;
    }

    @Override
    public long getItemId(int position) {
        final Device itemAt;
        if (devices == null || (itemAt = devices.get(position)) == null) {
            return super.getItemId(position);
        }
        return ViewIDUtils.getStableId(Device.class, itemAt.getMacAddress());
    }

    @Override
    public void onBindViewHolder(final WirelessClientsRecyclerViewHolder holder, final int position) {
        final Device device = devices.get(position);

        holder.actionsToggleInternetAccessButton.setOnCheckedChangeListener(null);

        final WANAccessState deviceWanAccessState = device.getWanAccessState();
        switch (deviceWanAccessState) {
            case WAN_ACCESS_ENABLED:
                ViewUtils.setBackgroundColorFromRouterFirmware(holder.actionsView, router);
                holder.actionsToggleInternetAccessButton.setEnabled(true);
                holder.actionsToggleInternetAccessButton.setChecked(true);
                break;
            case WAN_ACCESS_DISABLED:
                holder.actionsView
                        .setBackgroundColor(ContextCompat.getColor(holder.context, R.color.transparent_semi));
                holder.actionsToggleInternetAccessButton.setEnabled(true);
                holder.actionsToggleInternetAccessButton.setChecked(false);
                break;
            case WAN_ACCESS_UNKNOWN:
            default:
                holder.actionsView
                        .setBackgroundColor(ContextCompat.getColor(holder.context, R.color.transparent_semi));
                holder.actionsToggleInternetAccessButton.setEnabled(false);
                break;
        }

        final DeviceOnMenuItemClickListener deviceOnMenuItemClickListener =
                clientsTile.new DeviceOnMenuItemClickListener(holder.deviceNameView, holder.deviceAliasView, device)
                        .setToggleWanAccessSwitchCompat(holder.actionsToggleInternetAccessButton)
                        .setActionsView(holder.actionsView);

        holder.actionsSetAliasButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                deviceOnMenuItemClickListener
                        .onMenuItemClick(R.id.tile_status_wireless_client_rename, null);
            }
        });
        holder.actionsWakeOnLanButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                deviceOnMenuItemClickListener
                        .onMenuItemClick(R.id.tile_status_wireless_client_wol, null);
            }
        });
        holder.actionsViewIpConnectionsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (device.getActiveIpConnectionsCount() <= 0) {
                    Toast.makeText(holder.context, "No active IP Connections found for device: " +
                                    device.getAliasOrSystemName() + " (" + device.getMacAddress() + ") !",
                            Toast.LENGTH_SHORT).show();
                } else {
                    deviceOnMenuItemClickListener
                            .onMenuItemClick(R.id.tile_status_wireless_client_view_active_ip_connections, null);
                }
            }
        });
        holder.actionsToggleInternetAccessButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                deviceOnMenuItemClickListener
                        .onMenuItemClick(R.id.tile_status_wireless_client_wan_access_state, isChecked);
            }
        });

        final SharedPreferences routerPreferences = router.getPreferences(holder.context);
        Set<String> expandedClients = routerPreferences.getStringSet(EXPANDED_CLIENTS_PREF_KEY, null);
        if (expandedClients == null) {
            //Add first item right away
            routerPreferences.edit()
                    .putStringSet(EXPANDED_CLIENTS_PREF_KEY, Sets.newHashSet(device.getMacAddress()))
                    .apply();
        }

        holder.legendView.setVisibility(View.GONE);

        final boolean isThemeLight = ColorUtils.Companion.isThemeLight(holder.context);
        final boolean detailsPlaceholderVisible = (holder.detailsPlaceHolderView.getVisibility() == View.VISIBLE);
        if (!isThemeLight) {
            //Set menu background to white
            holder.tileMenu.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
            holder.expandCollapseButton.setImageResource(detailsPlaceholderVisible ?
                    R.drawable.ic_expand_less_black_24dp : R.drawable.ic_expand_more_black_24dp);
        } else {
            holder.expandCollapseButton.setImageResource(detailsPlaceholderVisible ?
                    R.drawable.ic_expand_less_white_24dp : R.drawable.ic_expand_more_white_24dp);
        }

        final String macAddress = device.getMacAddress();

        final String name = device.getName();
        final String nameForAvatar;
        if (isNullOrEmpty(device.getAlias()) && isNullOrEmpty(device.getSystemName()) && name.equals(
                macAddress)) {
            holder.deviceNameView.setText(EMPTY_VALUE_TO_DISPLAY);
            nameForAvatar = EMPTY_VALUE_TO_DISPLAY;
        } else {
            holder.deviceNameView.setText(name);
            nameForAvatar = name;
        }
        final TextDrawable textDrawable = ImageUtils.getTextDrawable(nameForAvatar);
        holder.avatarView.setImageDrawable(textDrawable);

        //Now if is wireless client or not
        final Device.WirelessConnectionInfo wirelessConnectionInfo = device.getWirelessConnectionInfo();
        if (wirelessConnectionInfo != null) {
            //            nbWirelessClients++;
            for (View wirelessRelatedView : holder.wirelessRelatedViews) {
                wirelessRelatedView.setVisibility(View.VISIBLE);
            }

            //SSID
            final String ssid = wirelessConnectionInfo.getSsid();
            holder.ssidView.setText(isNullOrEmpty(ssid) ? EMPTY_VALUE_TO_DISPLAY : ssid);

            //SNR Margin
            final String snrMargin = wirelessConnectionInfo.getSnrMargin();
            if (isNullOrEmpty(snrMargin)) {
                holder.snrMarginView.setText(EMPTY_VALUE_TO_DISPLAY);
            } else {
                holder.snrMarginView.setText(snrMargin + " dB");
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
                    holder.deviceNameView.setCompoundDrawablesWithIntrinsicBounds(
                            isThemeLight ? R.drawable.ic_action_device_signal_wifi_0_bar
                                    : R.drawable.ic_action_device_signal_wifi_0_bar_white, 0, 0, 0);
                } else if (snr <= 25) {
                    //Very low signal (1 bar); mostly associated; mostly slow.
                    holder.deviceNameView.setCompoundDrawablesWithIntrinsicBounds(
                            isThemeLight ? R.drawable.ic_action_device_signal_wifi_1_bar
                                    : R.drawable.ic_action_device_signal_wifi_1_bar_white, 0, 0, 0);
                } else if (snr <= 35) {
                    //Low signal (2 bars); always associated; usually fast.
                    holder.deviceNameView.setCompoundDrawablesWithIntrinsicBounds(
                            isThemeLight ? R.drawable.ic_action_device_signal_wifi_2_bar
                                    : R.drawable.ic_action_device_signal_wifi_2_bar_white, 0, 0, 0);
                } else if (snr <= 50) {
                    //Very good signal (3 - 4 bars); always associated; very fast.
                    holder.deviceNameView.setCompoundDrawablesWithIntrinsicBounds(
                            isThemeLight ? R.drawable.ic_action_device_signal_wifi_3_bar
                                    : R.drawable.ic_action_device_signal_wifi_3_bar_white, 0, 0, 0);
                } else {
                    //Excellent signal (5 bars); always associated; lightening fast.
                    holder.deviceNameView.setCompoundDrawablesWithIntrinsicBounds(
                            isThemeLight ? R.drawable.ic_action_device_signal_wifi_4_bar
                                    : R.drawable.ic_action_device_signal_wifi_4_bar_white, 0, 0, 0);
                }

                //Postulate: we consider that a value of 55dB SNR corresponds to 100% in our progress bar
                holder.signalStrengthView.setProgress(Math.min(snr * 100 / 55, 100));

                holder.signalStrengthTitleView.setVisibility(View.VISIBLE);
                holder.signalStrengthSepView.setVisibility(View.VISIBLE);
                holder.signalStrengthView.setVisibility(View.VISIBLE);
            } catch (final NumberFormatException nfe) {
                nfe.printStackTrace();
                holder.signalStrengthTitleView.setVisibility(View.GONE);
                holder.signalStrengthSepView.setVisibility(View.GONE);
                holder.signalStrengthView.setVisibility(View.GONE);
                holder.deviceNameView.setCompoundDrawablesWithIntrinsicBounds(
                        isThemeLight ? R.drawable.ic_action_device_signal_wifi_0_bar
                                : R.drawable.ic_action_device_signal_wifi_0_bar_white, 0, 0, 0);
            }

            //RSSI
            final String rssi = wirelessConnectionInfo.getRssi();
            if (isNullOrEmpty(rssi)) {
                holder.rssiView.setText(EMPTY_VALUE_TO_DISPLAY);
            } else {
                holder.rssiView.setText(rssi + " dBm");
            }
        } else {
            for (View wirelessRelatedView : holder.wirelessRelatedViews) {
                wirelessRelatedView.setVisibility(View.GONE);
            }
        }

        final Set<String> deviceActiveIpConnections = device.getActiveIpConnections();
        if (deviceActiveIpConnections == null) {
            holder.deviceActiveIpConnectionsView.setText(EMPTY_VALUE_TO_DISPLAY);
        } else {
            final int deviceActiveIpConnectionsCount = device.getActiveIpConnectionsCount();
            holder.deviceActiveIpConnectionsView.setText(String.valueOf(deviceActiveIpConnectionsCount));
            if (deviceActiveIpConnectionsCount > 0) {
                holder.deviceActiveIpConnectionsView.setMovementMethod(LinkMovementMethod.getInstance());
                final Spannable spans = (Spannable) holder.deviceActiveIpConnectionsView.getText();
                final ClickableSpan clickSpan = new ClickableSpan() {

                    @Override
                    public void onClick(View widget) {
                        deviceOnMenuItemClickListener
                                .onMenuItemClick(R.id.tile_status_wireless_client_view_active_ip_connections, null);
                    }
                };
                spans.setSpan(clickSpan, 0, spans.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        final Device.WANAccessState wanAccessState = deviceWanAccessState;
        final boolean isDeviceWanAccessEnabled =
                (wanAccessState == Device.WANAccessState.WAN_ACCESS_ENABLED);
        if (isDeviceWanAccessEnabled) {
            holder.deviceNameView.setTextColor(
                    ContextCompat.getColor(holder.context, R.color.ddwrt_green));
        }

        if (wanAccessState == null || isNullOrEmpty(wanAccessState.toString())) {
            holder.deviceWanAccessStateView.setText(EMPTY_VALUE_TO_DISPLAY);
        } else {
            holder.deviceWanAccessStateView.setText(wanAccessState.toString());
        }

        holder.deviceMac.setText(macAddress);

        final String ipAddress = device.getIpAddress();
        holder.deviceIp.setText(ipAddress);

        final boolean isThisDevice =
                (nullToEmpty(macAddress).equalsIgnoreCase(clientsTile.mCurrentMacAddress) && nullToEmpty(
                        ipAddress).equals(clientsTile.mCurrentIpAddress));
        if (isThisDevice) {
            if (isThemeLight) {
                //Set text color to blue
                ((TextView) holder.thisDevice).setTextColor(
                        ContextCompat.getColor(holder.context, R.color.blue));
            }
            holder.thisDevice.setVisibility(View.VISIBLE);
        } else {
            holder.thisDevice.setVisibility(View.INVISIBLE);
        }

        holder.deviceDetailsPlaceHolder.removeAllViews();
        final BandwidthMonitoringTile.BandwidthMonitoringIfaceData bandwidthMonitoringIfaceData;
        synchronized (clientsTile.usageDataLock) {
            bandwidthMonitoringIfaceData =
                    clientsTile.bandwidthMonitoringIfaceDataPerDevice.get(macAddress);
        }

        final boolean hideGraphPlaceHolder =
                bandwidthMonitoringIfaceData == null || bandwidthMonitoringIfaceData.getData().isEmpty();
        if (hideGraphPlaceHolder) {
            //Show no data
            holder.deviceDetailsPlaceHolder.setVisibility(View.GONE);
            holder.legendView.setVisibility(View.GONE);
            holder.noDataView.setVisibility(View.VISIBLE);
        } else {
            holder.legendView.setVisibility(View.VISIBLE);

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
            for (final Map.Entry<String, EvictingQueue<BandwidthMonitoringTile.DataPoint>> entry : dataCircularBuffer
                    .entrySet()) {
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
                    yLabels.put(y, org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(
                            Double.valueOf(y).longValue()).replace("bytes", "B"));
                }

                // Now we add our series
                dataset.addSeries(series);

                // Now we create the renderer
                final XYSeriesRenderer renderer = new XYSeriesRenderer();
                renderer.setLineWidth(5);

                final int color = ColorUtils.Companion.getColor(inOrOut);
                renderer.setColor(color);
                // Include low and max value
                renderer.setDisplayBoundingPoints(true);
                // we add point markers
                renderer.setPointStyle(PointStyle.POINT);
                renderer.setPointStrokeWidth(1);

                final XYSeriesRenderer.FillOutsideLine fill = new XYSeriesRenderer.FillOutsideLine(
                        XYSeriesRenderer.FillOutsideLine.Type.BOUNDS_ABOVE);
                //Fill with a slightly transparent version of the original color
                fill.setColor(android.support.v4.graphics.ColorUtils.setAlphaComponent(color, 30));
                renderer.addFillOutsideLine(fill);

                if (i == 0) {
                    holder.series1BarView.setBackgroundColor(color);
                    holder.series1TextView.setText(inOrOut);
                    holder.series1TextView.setTextColor(color);
                } else if (i == 1) {
                    holder.series2BarView.setBackgroundColor(color);
                    holder.series2TextView.setText(inOrOut);
                    holder.series2TextView.setTextColor(color);
                }
                i++;

                mRenderer.addSeriesRenderer(renderer);
            }

            mRenderer.setYLabels(0);
            mRenderer.addYTextLabel(maxY,
                    org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(
                            Double.valueOf(maxY).longValue()).replace("bytes", "B"));
            if (maxY != 0 && maxY / 2 >= 9000) {
                mRenderer.addYTextLabel(maxY / 2,
                        org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(
                                Double.valueOf(maxY / 2).longValue()).replace("bytes", "B"));
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
            final int blackOrWhite = ContextCompat.getColor(holder.context,
                    ColorUtils.Companion.isThemeLight(holder.context) ? R.color.black : R.color.white);
            mRenderer.setAxesColor(blackOrWhite);
            mRenderer.setShowLegend(false);
            mRenderer.setXLabelsColor(blackOrWhite);
            mRenderer.setYLabelsColor(0, blackOrWhite);

            final GraphicalView chartView =
                    ChartFactory.getTimeChartView(holder.context, dataset, mRenderer, null);
            chartView.repaint();

            holder.deviceDetailsPlaceHolder.addView(chartView, 0);

            holder.deviceDetailsPlaceHolder.setVisibility(View.VISIBLE);
            holder.noDataView.setVisibility(View.GONE);
        }

        final NetworkTrafficView networkTrafficView =
                new NetworkTrafficView(holder.context, isThemeLight, router.getUuid(), device);
        networkTrafficView.setRxAndTxBytes(Double.valueOf(device.getRxRate()).longValue(),
                Double.valueOf(device.getTxRate()).longValue());
        holder.trafficViewPlaceHolder.removeAllViews();
        holder.trafficViewPlaceHolder.addView(networkTrafficView);

        final String systemName = device.getSystemName();
        if (isNullOrEmpty(systemName)) {
            holder.deviceSystemNameView.setText(EMPTY_VALUE_TO_DISPLAY);
        } else {
            holder.deviceSystemNameView.setText(systemName);
        }

        final String alias = device.getAlias();
        if (isNullOrEmpty(alias)) {
            holder.deviceAliasView.setText(EMPTY_VALUE_TO_DISPLAY);
        } else {
            holder.deviceAliasView.setText(alias);
        }

        MultiThreadingManager.getResolutionTasksExecutor().execute(new UiRelatedTask<Void>() {
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
                if (macouiVendorDetails == null
                        || (company = macouiVendorDetails.getCompany()) == null
                        || company.isEmpty()) {
                    if (holder.ouiVendorRowView != null) {
                        holder.ouiVendorRowView.setText(EMPTY_VALUE_TO_DISPLAY);
                    }
                    if (holder.nicManufacturerView != null) {
                        holder.nicManufacturerView.setVisibility(View.GONE);
                    }
                } else {
                    if (holder.ouiVendorRowView != null) {
                        holder.ouiVendorRowView.setText(company);
                    }
                    if (holder.nicManufacturerView != null) {
                        holder.nicManufacturerView.setText(company);
                        holder.nicManufacturerView.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        final long lastSeen = device.getLastSeen();
        Crashlytics.log(Log.DEBUG, LOG_TAG,
                "XXX lastSeen for '" + macAddress + "' =[" + lastSeen + "]");
        if (lastSeen <= 0) {
            holder.lastSeenRowView.setText(EMPTY_VALUE_TO_DISPLAY);
            holder.lastSeenRowView.setReferenceTime(-1L);
        } else {
            holder.lastSeenRowView.setReferenceTime(lastSeen);
            holder.lastSeenRowView.setPrefix(DATE_FORMAT.format(new Date(lastSeen)) + "\n(");
            holder.lastSeenRowView.setSuffix(")");
        }

        final double rxTotal = device.getRxTotal();
        if (rxTotal < 0.) {
            holder.totalDownloadRowView.setText(EMPTY_VALUE_TO_DISPLAY);
        } else {
            final long value = Double.valueOf(rxTotal).longValue();
            holder.totalDownloadRowView.setText(value
                    + " B ("
                    + org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(value)
                    + ")");
        }

        final double txTotal = device.getTxTotal();
        if (txTotal < 0.) {
            holder.totalUploadRowView.setText(EMPTY_VALUE_TO_DISPLAY);
        } else {
            final long value = Double.valueOf(txTotal).longValue();
            holder.totalUploadRowView.setText(value
                    + " B ("
                    + org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(value)
                    + ")");
        }

        holder.expandCollapseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                holder.firstGlanceView.performClick();
            }
        });

        holder.firstGlanceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Set<String> clientsExpanded = new HashSet<>(
                        routerPreferences.getStringSet(EXPANDED_CLIENTS_PREF_KEY, new HashSet<String>()));

                if (holder.ouiAndLastSeenView.getVisibility() == View.VISIBLE) {
                    holder.ouiAndLastSeenView.setVisibility(View.GONE);
                    clientsExpanded.remove(macAddress);
                    //                                        cardView.setCardElevation(40f);
                } else {
                    holder.ouiAndLastSeenView.setVisibility(View.VISIBLE);
                    clientsExpanded.add(macAddress);
                    //                                        cardView.setCardElevation(2f);
                }
                if (hideGraphPlaceHolder) {
                    holder.trafficGraphPlaceHolderView.setVisibility(View.GONE);
                    holder.legendView.setVisibility(View.GONE);
                    if (holder.noDataView.getVisibility() == View.VISIBLE) {
                        holder.noDataView.setVisibility(View.GONE);
                    } else {
                        holder.noDataView.setVisibility(View.VISIBLE);
                    }
                } else {
                    holder.noDataView.setVisibility(View.GONE);
                    if (holder.trafficGraphPlaceHolderView.getVisibility() == View.VISIBLE) {
                        holder.trafficGraphPlaceHolderView.setVisibility(View.GONE);
                    } else {
                        holder.trafficGraphPlaceHolderView.setVisibility(View.VISIBLE);
                    }
                    if (holder.legendView.getVisibility() == View.VISIBLE) {
                        holder.legendView.setVisibility(View.GONE);
                    } else {
                        holder.legendView.setVisibility(View.VISIBLE);
                    }
                }
                routerPreferences.edit().putStringSet(EXPANDED_CLIENTS_PREF_KEY, clientsExpanded).apply();

                if (holder.detailsPlaceHolderView.getVisibility() == View.VISIBLE) {
                    ViewUtils.collapse(holder.detailsPlaceHolderView, holder.expandCollapseButton);
                } else {
                    ViewUtils.expand(holder.detailsPlaceHolderView, holder.expandCollapseButton);
                }
            }
        });

        expandedClients =
                routerPreferences.getStringSet(EXPANDED_CLIENTS_PREF_KEY, new HashSet<String>());
        if (expandedClients.contains(macAddress)) {
            //                        cardView.setCardElevation(40f);
            //Expand detailed view
            holder.ouiAndLastSeenView.setVisibility(View.VISIBLE);
            if (hideGraphPlaceHolder) {
                holder.noDataView.setVisibility(View.VISIBLE);
                holder.trafficGraphPlaceHolderView.setVisibility(View.GONE);
                holder.legendView.setVisibility(View.GONE);
            } else {
                holder.trafficGraphPlaceHolderView.setVisibility(View.VISIBLE);
                holder.legendView.setVisibility(View.VISIBLE);
                holder.noDataView.setVisibility(View.GONE);
            }
            holder.detailsPlaceHolderView.setVisibility(View.VISIBLE);
            holder.expandCollapseButton.setImageResource(isThemeLight ?
                    R.drawable.ic_expand_less_black_24dp : R.drawable.ic_expand_less_white_24dp);
        } else {
            //Collapse detailed view
            holder.ouiAndLastSeenView.setVisibility(View.GONE);
            holder.trafficGraphPlaceHolderView.setVisibility(View.GONE);
            holder.legendView.setVisibility(View.GONE);
            holder.noDataView.setVisibility(View.GONE);
            ViewUtils.collapse(holder.detailsPlaceHolderView, holder.expandCollapseButton);
        }

        if (wanAccessState == null || wanAccessState == Device.WANAccessState.WAN_ACCESS_UNKNOWN) {
            holder.wanBlockedDevice.setVisibility(View.GONE);
        } else {
            holder.wanBlockedDevice.setVisibility(isDeviceWanAccessEnabled ? View.GONE : View.VISIBLE);
        }

        holder.tileMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final PopupMenu popup = new PopupMenu(holder.context, v);
                popup.setOnMenuItemClickListener(deviceOnMenuItemClickListener);
                final MenuInflater inflater = popup.getMenuInflater();

                final Menu menu = popup.getMenu();

                inflater.inflate(R.menu.tile_status_wireless_client_options, menu);

                if (isThisDevice) {
                    //WOL not needed as this is the current device
                    menu.findItem(R.id.tile_status_wireless_client_wol).setEnabled(false);
                }

                final MenuItem wanAccessStateMenuItem =
                        menu.findItem(R.id.tile_status_wireless_client_wan_access_state);
                if (wanAccessState == null || wanAccessState == Device.WANAccessState.WAN_ACCESS_UNKNOWN) {
                    wanAccessStateMenuItem.setEnabled(false);
                } else {
                    wanAccessStateMenuItem.setEnabled(true);
                    wanAccessStateMenuItem.setChecked(isDeviceWanAccessEnabled);
                }

                final MenuItem activeIpConnectionsMenuItem =
                        menu.findItem(R.id.tile_status_wireless_client_view_active_ip_connections);
                //                        mClientsActiveConnectionsMenuMap.put(macAddress, activeIpConnectionsMenuItem);
                final boolean activeIpConnectionsMenuItemEnabled =
                        !(deviceActiveIpConnections == null || deviceActiveIpConnections.size() == 0);
                activeIpConnectionsMenuItem.setEnabled(activeIpConnectionsMenuItemEnabled);
                if (activeIpConnectionsMenuItemEnabled) {
                    activeIpConnectionsMenuItem.setTitle(
                            holder.context.getResources().getString(R.string.view_active_ip_connections)
                                    + " ("
                                    + deviceActiveIpConnections.size()
                                    + ")");
                }
                popup.show();
            }
        });
    }

    @Override
    public WirelessClientsRecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final Context context = parent.getContext();
        final CardView cardView = (CardView) LayoutInflater.from(context)
                .inflate(R.layout.tile_status_wireless_client, parent, false);

        //Add padding to CardView on v20 and before to prevent intersections between the Card content and rounded corners.
//        cardView.setPreventCornerOverlap(true);
        //Add padding in API v21+ as well to have the same measurements with previous versions.
        cardView.setUseCompatPadding(true);

        final boolean isThemeLight = ColorUtils.Companion.isThemeLight(context);

        if (isThemeLight) {
            //Light
            cardView.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.cardview_light_background));
        } else {
            //Default is Dark
            cardView.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.cardview_dark_background));
        }

        //Highlight CardView
        //                    cardView.setCardElevation(10f);
        return new WirelessClientsRecyclerViewHolder(context, cardView);
    }

    public WirelessClientsRecyclerViewAdapter setDevices(List<Device> devices) {
        this.devices = devices;
        return this;
    }
}
