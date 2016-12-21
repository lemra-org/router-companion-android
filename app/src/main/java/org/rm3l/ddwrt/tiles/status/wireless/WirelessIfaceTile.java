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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.cocosw.undobar.UndoBarController;
import com.crashlytics.android.Crashlytics;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.actions.ActionManager;
import org.rm3l.ddwrt.actions.RouterAction;
import org.rm3l.ddwrt.actions.RouterActionListener;
import org.rm3l.ddwrt.actions.SetNVRAMVariablesAction;
import org.rm3l.ddwrt.actions.TogglePhysicalInterfaceStateRouterAction;
import org.rm3l.ddwrt.actions.TogglePhysicalInterfaceStateRouterAction.PhysicalInterfaceState;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.fragments.AbstractBaseFragment;
import org.rm3l.ddwrt.main.DDWRTMainActivity;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.resources.ProcNetDevNetworkData;
import org.rm3l.ddwrt.resources.ProcNetDevReceive;
import org.rm3l.ddwrt.resources.ProcNetDevTransmit;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.status.wireless.share.WifiSharingActivity;
import org.rm3l.ddwrt.tiles.status.wireless.share.nfc.WriteWifiConfigToNfcDialog;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.ImageUtils;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import be.brunoparmentier.wifikeyshare.utils.NfcUtils;
import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.rm3l.ddwrt.tiles.status.wireless.WirelessIfaceTile.TemperatureUnit.CELSIUS;
import static org.rm3l.ddwrt.tiles.status.wireless.WirelessIfaceTile.TemperatureUnit.FAHRENHEIT;
import static org.rm3l.ddwrt.tiles.status.wireless.share.WifiSharingActivity.SSID;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.COLON;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.EMPTY_STRING;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.EMPTY_VALUE_TO_DISPLAY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.SPACE;

/**
 *
 */
public class WirelessIfaceTile extends DDWRTTile<NVRAMInfo>
        implements PopupMenu.OnMenuItemClickListener {

    public static final String CAT_SYS_CLASS_NET_S_STATISTICS = "cat /sys/class/net/%s/statistics";
    public static final Pattern HEX_ONLY_QR_CODE_PATTERN = Pattern.compile("/^[0-9a-f]+$/i");
    public static final char DEGREE_SYMBOL = '\u00B0';
    private static final String WIRELESS_IFACE = "wireless_iface";
    private static final String LOG_TAG = WirelessIfaceTile.class.getSimpleName();
    public static final String IFACE = "iface";
    public static final String PARENT_IFACE = "parent_iface";
    public static final String WL_SECURITY_NVRAMINFO = "WL_SECURITY_NVRAMINFO";
    public static final String IFACE_STATE = "_iface_state";
    public static final String PHYSICAL_IFACE_STATE_ACTION = "PHYSICAL_IFACE_STATE_ACTION";

    private WriteWifiConfigToNfcDialog mWifiToNfcDialog;

    @NonNull
    private final String iface;

    @Nullable
    private final String parentIface;

    @Nullable
    private String phyIface;

    @Nullable
    private String wifiSsid;

    @Nullable
    private String wifiPassword;

    @Nullable
    private String hwAddr;

    private NVRAMInfo mNvramInfo;

    private AtomicBoolean mWirelessSecurityFormOpened = new AtomicBoolean(false);

    @Nullable
    private WirelessEncryptionTypeForQrCode wifiEncryptionType;
//    private long mLastSync;

    public WirelessIfaceTile(@NonNull String iface, @NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        this(iface, null, parentFragment, arguments, router);
    }

    public WirelessIfaceTile(@NonNull String iface, @Nullable String parentIface, @NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
//        super(parentFragment, arguments, router, R.layout.tile_status_wireless_iface, R.id.tile_status_wireless_iface_togglebutton);
        super(parentFragment, arguments, router, R.layout.tile_status_wireless_iface, null);

        if (Looper.myLooper() == null) {
            //Check for this - otherwise it yields the following error:
            // "only one looper may be created per thread")
            //cf. http://stackoverflow.com/questions/23038682/java-lang-runtimeexception-only-one-looper-may-be-created-per-thread
            Looper.prepare();
        }

        this.iface = iface;
        this.parentIface = parentIface;
        ((TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_title)).setText(this.iface);

        //Create Options Menu
        final ImageButton tileMenu = (ImageButton) layout.findViewById(R.id.tile_status_wireless_iface_menu);

        final boolean isThemeLight = ColorUtils.isThemeLight(mParentFragmentActivity);

        if (!isThemeLight) {
            //Set menu background to white
            tileMenu.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
        }

        tileMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final PopupMenu popup = new PopupMenu(mParentFragmentActivity, v);
                popup.setOnMenuItemClickListener(WirelessIfaceTile.this);
                final MenuInflater inflater = popup.getMenuInflater();

                final Menu menu = popup.getMenu();

                inflater.inflate(R.menu.tile_wireless_iface_options, menu);

                final MenuItem shareMenuItem = menu.findItem(R.id.tile_status_wireless_iface_share);
                if (wifiEncryptionType == null || (isNullOrEmpty(wifiSsid) && wifiPassword == null)) {
                    shareMenuItem.setEnabled(false);
                } else {
                    shareMenuItem.setEnabled(true);
                }
                if (NfcUtils.hasNFCHardware(mParentFragmentActivity)) {
                    shareMenuItem.setTitle(R.string.share_via_nfc_or_qr_code);
                } else {
                    shareMenuItem.setTitle(R.string.share_via_qr_code);
                }

                popup.show();
            }
        });

    }

    @NonNull
    public String getIface() {
        return iface;
    }

    @Override
    public int getTileHeaderViewId() {
        return R.id.tile_status_wireless_iface_hdr;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_status_wireless_iface_title;
    }

    @Nullable
    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {


                try {
                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Init background loader for " + WirelessIfaceTile.class + ": routerInfo=" +
                            mRouter + " / nbRunsLoader=" + nbRunsLoader);

                    if (mRefreshing.getAndSet(true)) {
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    if (!isForceRefresh()) {
                        //Force Manual Refresh
                        if (mWirelessSecurityFormOpened.get()) {
                            //Skip run
                            Crashlytics.log(Log.DEBUG, LOG_TAG, "Skip loader run");
                            return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                        }
                    } else {
                        if (mWirelessSecurityFormOpened.get()) {
                            return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                        }
                    }
                    nbRunsLoader++;

//                    mLastSync = System.currentTimeMillis();

                    mNvramInfo = getIfaceNvramInfo(iface);
                    if (parentIface != null && !parentIface.isEmpty()) {
                        mNvramInfo.putAll(getIfaceNvramInfo(parentIface));
                    }

                    if (mNvramInfo.isEmpty()) {
                        throw new DDWRTNoDataException("No Data!");
                    }

                    return mNvramInfo;

                } catch (@NonNull final Exception e) {
                    e.printStackTrace();
                    return new NVRAMInfo().setException(e);
                }
            }

            @NonNull
            private NVRAMInfo getIfaceNvramInfo(@NonNull String wlIface) throws Exception {

//                if (DDWRTCompanionConstants.TEST_MODE) {
                if (Utils.isDemoRouter(mRouter)) {
                    return new NVRAMInfo()
                            .setProperty(WIRELESS_IFACE + (wlIface.equals(parentIface) ? "_parent" : ""), wlIface)
                            .setProperty(wlIface + "_radio", String.valueOf(new Random().nextInt()))
                            .setProperty(wlIface + "_mode", "Mode")
                            .setProperty(wlIface + "_hwaddr", "hw:ad:dr:rd:da:wh")
                            .setProperty(wlIface + "_ifname", wlIface + " ifname")
                            .setProperty(wlIface + "_net_mode", "ng-only")
                            .setProperty(wlIface + "_ssid", "SSID")
                            .setProperty(wlIface + "_channel", String.valueOf(new Random().nextInt(100)))
                            .setProperty(wlIface + "_txpwr", String.valueOf(new Random().nextInt(100)))
                            .setProperty(wlIface + "_rate", String.valueOf(new Random().nextInt(100)))
                            .setProperty(wlIface + "_security_mode", "psk psk2");
                }

                final NVRAMInfo nvramInfo = new NVRAMInfo();

                NVRAMInfo nvramInfoTmp = null;
                try {
                    nvramInfoTmp = SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity, mRouter,
                            mGlobalPreferences,
                            wlIface + "_radio",
                            wlIface + "_mode",
                            wlIface + "_hwaddr",
                            wlIface + "_ifname",
                            wlIface + "_net_mode",
                            wlIface + "_ssid",
                            wlIface + "_channel",
                            wlIface + "_txpwr",
                            wlIface + "_rate",
                            wlIface + "_akm", //FIXME Useless?
                            wlIface + "_wpa_psk",
                            wlIface + "_security_mode",
                            StringUtils.replace(wlIface, ".", "X") + "_security_mode",
                            wlIface + "_crypto",
                            wlIface + "_wl_unmask",
                            wlIface + "_wpa_gtk_rekey",
                            wlIface + "_radius_ipaddr",
                            wlIface + "_radius_port",
                            wlIface + "_radius_key",
                            wlIface + "_radius_unmask",
                            wlIface + "_radmactype",
                            wlIface + "_key",
                            wlIface + "_key1",
                            wlIface + "_key2",
                            wlIface + "_key3",
                            wlIface + "_key4",
                            wlIface + "_wep_bit",
                            wlIface + "_passphrase");
                } finally {
                    if (nvramInfoTmp != null) {
                        nvramInfo.putAll(nvramInfoTmp);
                    }

                    nvramInfo.setProperty(WIRELESS_IFACE + (wlIface.equals(parentIface) ? "_parent" : ""),
                            wlIface);

                    final List<String> activePhysicalIfacesList = new ArrayList<>();
                    //Get list of active physical ifaces
                    final String[] ifconfigResultActive = SSHUtils.getManualProperty(mParentFragmentActivity, mRouter,
                            mGlobalPreferences,
                            "/sbin/ifconfig | grep 'Link' | awk '{print $1}'");
                    if (ifconfigResultActive != null && ifconfigResultActive.length > 0) {
                        for (final String ifconfigActive : ifconfigResultActive) {
                            if (ifconfigActive == null || ifconfigActive.isEmpty()) {
                                continue;
                            }
                            activePhysicalIfacesList.add(ifconfigActive.trim());
                        }
                    }

                    final List<String> allPhysicalIfacesList = new ArrayList<>();
                    //Get list of active physical ifaces
                    final String[] ifconfigResultAll = SSHUtils.getManualProperty(mParentFragmentActivity, mRouter,
                            mGlobalPreferences,
                            "/sbin/ifconfig -a | grep 'Link' | awk '{print $1}'");
                    if (ifconfigResultAll != null && ifconfigResultAll.length > 0) {
                        for (final String ifconfigAll : ifconfigResultAll) {
                            if (ifconfigAll == null || ifconfigAll.isEmpty()) {
                                continue;
                            }
                            allPhysicalIfacesList.add(ifconfigAll.trim());
                        }
                    }

                    //Set Temp, RX and TX Network Bandwidths on Physical Interface
                    String phyIface = nvramInfo.getProperty(wlIface + "_ifname");
                    if (isNullOrEmpty(phyIface)) {
                        //Determine if current iface is actually a physical interface
                        if (allPhysicalIfacesList.contains(wlIface)) {
                            phyIface = wlIface;
                            nvramInfo.setProperty(wlIface + "_ifname", wlIface);
                        }
                    }

                    nvramInfo.setProperty(wlIface + IFACE_STATE,
                            activePhysicalIfacesList.contains(phyIface) ? "Up" : "Down");

                    WirelessIfaceTile.this.phyIface = phyIface;

                    if (!isNullOrEmpty(phyIface)) {

                        try {
                            final String[] noise = SSHUtils.getManualProperty(mParentFragmentActivity, mRouter,
                                    mGlobalPreferences,
                                    String.format("( wl -i %s noise || wl_atheros -i %s noise || wl noise ) 2>/dev/null", phyIface, phyIface));
                            if (noise != null && noise.length > 0) {
                                nvramInfo.setProperty(wlIface + "_noise", noise[0]);
                            }
                        } catch (final Exception e) {
                            //No worries
                        }

                        //Set temperature
                        //noinspection ConstantConditions
                        final Map<TemperatureUnit, String> ifaceTemperatures = getIfaceTemperature(phyIface);
                        final String celsius = ifaceTemperatures.get(CELSIUS);
                        final String fahrenheit = ifaceTemperatures.get(FAHRENHEIT);
                        if (!(isNullOrEmpty(celsius) || isNullOrEmpty(fahrenheit))) {
                            nvramInfo.setProperty(wlIface + "_temperature",
                                    celsius + DEGREE_SYMBOL + CELSIUS + " (" + fahrenheit + DEGREE_SYMBOL + FAHRENHEIT + ")");
                        }

                        //Packet Info
                        final String sysClassNetStatsFolder = String.format(CAT_SYS_CLASS_NET_S_STATISTICS, phyIface);

                        final int hasSysClassFolder = SSHUtils.runCommands(mParentFragmentActivity, mGlobalPreferences, mRouter,
                                " [ -f " + sysClassNetStatsFolder + " ]");
                        ProcNetDevNetworkData procNetDevNetworkData = null;
                        if (hasSysClassFolder != 0) {
                            //sysfs not mounted - fetch info from /proc/net/dev instead
                            procNetDevNetworkData = gatherNetworkUsageFromProcNetDev(phyIface);
                        }

                        //RX and TX Bytes
                        //noinspection ConstantConditions
                        final Map<IfaceStatsType, Long> ifaceRxAndTxRates =
                                getIfaceRxAndTxRates(phyIface, procNetDevNetworkData);
                        final Long rxBps = ifaceRxAndTxRates.get(IfaceStatsType.RX_BYTES);
                        final Long txBps = ifaceRxAndTxRates.get(IfaceStatsType.TX_BYTES);
                        if (rxBps != null) {
                            nvramInfo.setProperty(wlIface + "_rx_rate_human_readable",
                                    rxBps + " B\n(" + FileUtils.byteCountToDisplaySize(rxBps)
                                            + ")");
                        }
                        if (txBps != null) {
                            nvramInfo.setProperty(wlIface + "_tx_rate_human_readable",
                                    txBps + " B\n(" + FileUtils.byteCountToDisplaySize(txBps) + ")");
                        }

                        //Packet Info
                        if (procNetDevNetworkData == null) {
                            try {
                                final String[] packetsInfo = SSHUtils
                                        .getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                                String.format("%s/rx_packets", sysClassNetStatsFolder),
                                                String.format("%s/rx_errors", sysClassNetStatsFolder),
                                                String.format("%s/tx_packets", sysClassNetStatsFolder),
                                                String.format("%s/tx_errors", sysClassNetStatsFolder));

                                if (packetsInfo != null) {
                                    final long rxErrors = Long.parseLong(packetsInfo[1]);
                                    nvramInfo.setProperty(wlIface + "_rx_packets",
                                            String.format("%s\n(%s)",
                                                    packetsInfo[0], rxErrors <= 0 ? "no error" :
                                                            (rxErrors + String.format(" error%s", rxErrors > 1 ? "s" : ""))));
                                    final long txErrors = Long.parseLong(packetsInfo[3]);
                                    nvramInfo.setProperty(wlIface + "_tx_packets",
                                            String.format("%s\n(%s)",
                                                    packetsInfo[2], txErrors <= 0 ? "no error" :
                                                            (txErrors + String.format(" error%s", txErrors > 1 ? "s" : ""))));
                                }
                            } catch (final Exception e) {
                                e.printStackTrace();
                                //No worries
                            }
                        } else {
                            final ProcNetDevReceive receive = procNetDevNetworkData.getReceive();
                            if (receive != null) {
                                final int rxErrors = receive.getRxErrors();
                                nvramInfo.setProperty(wlIface + "_rx_packets",
                                        String.format("%s\n(%s)",
                                                receive.getRxPackets(), rxErrors <= 0 ? "no error" :
                                                        (rxErrors + String.format(" error%s", rxErrors > 1 ? "s" : ""))));
                            }
                            final ProcNetDevTransmit transmit = procNetDevNetworkData.getTransmit();
                            if (transmit != null) {
                                final int txErrors = transmit.getTxErrors();
                                nvramInfo.setProperty(wlIface + "_tx_packets",
                                        String.format("%s\n(%s)",
                                                transmit.getTxPackets(), txErrors <= 0 ? "no error" :
                                                        (txErrors + String.format(" error%s", txErrors > 1 ? "s" : ""))));
                            }
                        }
                    }
                }

                return nvramInfo;
            }

            @NonNull
            private Map<TemperatureUnit, String> getIfaceTemperature(@NonNull final String phyIface) {
                final Map<TemperatureUnit, String> result = Maps.newHashMapWithExpectedSize(2);
                final String phyIfaceVarNameInRouter = WirelessIfaceTile.class.getSimpleName() + "TemperatureCelsius";
                try {
                    final String[] temperatures = SSHUtils.getManualProperty(mParentFragmentActivity,
                            mRouter, mGlobalPreferences,
                            String.format("%s=$(echo $((`wl -i %s phy_tempsense | awk {' print $1 '}`/2+20))); " +
                                            "echo \"C:$%s\"; echo \"F:$(($%s*9/5+32))\"",
                                    phyIfaceVarNameInRouter, phyIface, phyIfaceVarNameInRouter, phyIfaceVarNameInRouter));
                    if (temperatures != null && temperatures.length >= 2) {
                        for (final String temperature : temperatures) {
                            if (temperature == null) {
                                continue;
                            }
                            if (temperature.startsWith("C:")) {
                                result.put(CELSIUS, temperature.replaceAll("C:", "").trim());
                            } else if (temperature.startsWith("F:")) {
                                result.put(FAHRENHEIT, temperature.replaceAll("F:", "").trim());
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return result;
            }

            @NonNull
            private Map<IfaceStatsType, Long> getIfaceRxAndTxRates(@NonNull final String phyIface,
                                                                   @Nullable final ProcNetDevNetworkData procNetDevNetworkData) {
                final Map<IfaceStatsType, Long> result = Maps.newHashMapWithExpectedSize(2);

                if (procNetDevNetworkData == null) {

                    final String sysClassNetStatsFolder = String.format(CAT_SYS_CLASS_NET_S_STATISTICS, phyIface);
                    final String rxBytesCmd = String
                            .format("%s/rx_bytes", sysClassNetStatsFolder);
                    final String txBytesCmd = String
                            .format("%s/tx_bytes", sysClassNetStatsFolder);
                    try {
                        final long[] bytesBeforeAndAfter = parseFloatDataFromOutput(SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                        rxBytesCmd,
                                        txBytesCmd,
                                        "sleep 1",
                                        rxBytesCmd,
                                        txBytesCmd)
                        );
                        if (bytesBeforeAndAfter.length >= 4) {
                            result.put(IfaceStatsType.RX_BYTES, Math.abs(bytesBeforeAndAfter[1] - bytesBeforeAndAfter[0]));
                            result.put(IfaceStatsType.TX_BYTES, Math.abs(bytesBeforeAndAfter[3] - bytesBeforeAndAfter[2]));
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    final ProcNetDevReceive receive = procNetDevNetworkData.getReceive();
                    final ProcNetDevTransmit transmit = procNetDevNetworkData.getTransmit();
                    if (receive != null) {
                        result.put(IfaceStatsType.RX_BYTES, receive.getRxBytes());
                    }
                    if (transmit != null) {
                        result.put(IfaceStatsType.TX_BYTES, transmit.getTxBytes());
                    }
                }

                return result;
            }

            @NonNull
            private long[] parseFloatDataFromOutput(@Nullable final String[] output) {
                if (output == null || output.length == 0) {
                    throw new IllegalArgumentException("Output null or empty");
                }
                final long[] result = new long[output.length];
                for (int i = 0; i < output.length; i++) {
                    result[i] = Long.parseLong(output[i]);

                }
                return result;
            }

            @Nullable
            private ProcNetDevNetworkData gatherNetworkUsageFromProcNetDev(@NonNull final String phyIface) {

                final Map<String, ProcNetDevNetworkData> procNetDevNetworkDataMap = new HashMap<>();

                try {
                    final String[] procNetDevLines = SSHUtils.getManualProperty(
                            mParentFragmentActivity, mRouter, mGlobalPreferences,
                            "cat /proc/net/dev | grep \"" + phyIface + "\"");

                    if (procNetDevLines == null || procNetDevLines.length < 1) {
                        return null;
                    }

                    final ArrayList<String> gatheredData = new ArrayList<>();

                    String[] tempData;
//                    //Skip the first two lines (headers)
//                    for (int i = 2; i < procNetDevLines.length; i++) {
//                        //Parse /proc/net/dev to obtain network statistics.
//                        //Line e.g.:
//                        //lo: 4852 43 0 0 0 0 0 0 4852 43 0 0 0 0 0 0
//                        tempData = procNetDevLines[i]
//                                .replace(COLON, SPACE).split(SPACE);
//                        gatheredData.addAll(Arrays.asList(tempData));
//                        gatheredData.removeAll(Collections.singleton(EMPTY_STRING));
//                    }

                    //Parse /proc/net/dev to obtain network statistics.
                    //Line e.g.:
                    //lo: 4852 43 0 0 0 0 0 0 4852 43 0 0 0 0 0 0
                    tempData = procNetDevLines[0]
                            .replace(COLON, SPACE).split(SPACE);
                    gatheredData.addAll(Arrays.asList(tempData));
                    gatheredData.removeAll(Collections.singleton(EMPTY_STRING));

                    int offset = 17;
                    for (int base = 0; base < gatheredData.size(); base += offset) {
                        // std::string, long int, int, int, int, int, int, int, int, long int, int, int, int, int, int, int, int)
                        final String ifaceName = gatheredData.get(base);
                        procNetDevNetworkDataMap.put(ifaceName, new ProcNetDevNetworkData(ifaceName,
                                Long.parseLong(gatheredData.get(base + 1)),
                                Integer.parseInt(gatheredData.get(base + 2)),
                                Integer.parseInt(gatheredData.get(base + 3)),
                                Integer.parseInt(gatheredData.get(base + 4)),
                                Integer.parseInt(gatheredData.get(base + 5)),
                                Integer.parseInt(gatheredData.get(base + 6)),
                                Integer.parseInt(gatheredData.get(base + 7)),
                                Integer.parseInt(gatheredData.get(base + 8)),
                                Long.parseLong(gatheredData.get(base + 9)),
                                Integer.parseInt(gatheredData.get(base + 10)),
                                Integer.parseInt(gatheredData.get(base + 11)),
                                Integer.parseInt(gatheredData.get(base + 12)),
                                Integer.parseInt(gatheredData.get(base + 13)),
                                Integer.parseInt(gatheredData.get(base + 14)),
                                Integer.parseInt(gatheredData.get(base + 15)),
                                Integer.parseInt(gatheredData.get(base + 16))));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Utils.reportException(null, e);
                }

                return procNetDevNetworkDataMap.get(phyIface);
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
            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished: loader=" + loader);

            buildView(data);

//        doneWithLoaderInstance(this, loader,
//                R.id.tile_status_wireless_iface_togglebutton_title, R.id.tile_status_wireless_iface_togglebutton_separator);


            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished(): done loading!");
        } finally {
            mRefreshing.set(false);
            doneWithLoaderInstance(this, loader);
        }
    }

    public void buildView(@Nullable NVRAMInfo data) {

        Crashlytics.log(Log.DEBUG, LOG_TAG, "buildView: " + iface + " / data=" + data);

        layout.findViewById(R.id.tile_status_wireless_iface_loading_view)
                .setVisibility(View.GONE);
        layout.findViewById(R.id.tile_status_wireless_iface_gridlayout)
                .setVisibility(View.VISIBLE);

        final ImageView avatarView = (ImageView) layout.findViewById(R.id.avatar);

        if (data == null) {
            data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
        }

        final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_error);

        final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

            if (exception == null) {
                errorPlaceHolderView.setVisibility(View.GONE);
            }
            final String wlSsid = data.getProperty(this.iface + "_ssid");
            final TextDrawable textDrawable = ImageUtils.getTextDrawable(wlSsid);
            if (textDrawable == null) {
                avatarView.setVisibility(View.GONE);
            } else {
                avatarView.setImageDrawable(textDrawable);
                avatarView.setVisibility(View.VISIBLE);
            }
            updateTileDisplayInfo(data, true);

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
    }

    private void updateTileDisplayInfo(@Nullable NVRAMInfo data, final boolean defaultValuesIfNotFound) {

        if (data == null) {
            return;
        }

        //Encryption
        final TextView encryptionView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_encryption);
        final String wlSecModeToDisplay = data.getProperty(this.iface + "_security_mode",
                data.getProperty(StringUtils.replace(this.iface, ".", "X") + "_security_mode",
                    defaultValuesIfNotFound ? EMPTY_VALUE_TO_DISPLAY : null));
        if (wlSecModeToDisplay != null) {
            final String wlSecurityMode = data.getProperty(this.iface + "_security_mode",
                    data.getProperty(StringUtils.replace(this.iface, ".", "X") + "_security_mode"));
            String encryption = wlSecurityMode;
            if ("disabled".equalsIgnoreCase(wlSecurityMode)) {
                encryption = "Disabled";
            } else if ("psk".equalsIgnoreCase(wlSecurityMode)) {
//            encryption = "WPA Pre-shared Key";
                encryption = "WPA Personal";
            } else if ("wpa".equalsIgnoreCase(wlSecurityMode)) {
//            encryption = "WPA RADIUS";
                encryption = "WPA Enterprise";
            } else if ("psk2".equalsIgnoreCase(wlSecurityMode)) {
//            encryption = "WPA2 Pre-shared Key";
                encryption = "WPA2 Personal";
            } else if ("wpa2".equalsIgnoreCase(wlSecurityMode)) {
//            encryption = "WPA2 RADIUS";
                encryption = "WPA2 Enterprise";
            } else if ("psk psk2".equalsIgnoreCase(wlSecurityMode)) {
//            encryption = "WPA2 Pre-shared Key Mixed";
                encryption = "WPA2 Personal Mixed";
            } else if ("wpa wpa2".equalsIgnoreCase(wlSecurityMode)) {
//            encryption = "WPA RADIUS Mixed";
                encryption = "WPA2 Enterprise Mixed";
            } else if ("radius".equalsIgnoreCase(wlSecurityMode)) {
                encryption = "RADIUS";
            } else if ("wep".equalsIgnoreCase(wlSecurityMode)) {
                encryption = "WEP";
                this.wifiEncryptionType = WirelessEncryptionTypeForQrCode.WEP;
            }

            if (startsWith(encryption, "WPA")) {
                this.wifiEncryptionType = WirelessEncryptionTypeForQrCode.WPA;
            } else if (startsWith(encryption, "WEP")) {
                this.wifiEncryptionType = WirelessEncryptionTypeForQrCode.WEP;
            } else if (!"radius".equalsIgnoreCase(encryption)) {
                this.wifiEncryptionType = WirelessEncryptionTypeForQrCode.NONE;
            }
            encryptionView.setText(isNullOrEmpty(encryption) ? "-" : encryption);
        }

        //SSID
        final TextView ssidView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_ssid);
        final String wlSsid = data.getProperty(this.iface + "_ssid", defaultValuesIfNotFound ? EMPTY_VALUE_TO_DISPLAY : null);
        if (wlSsid != null) {
            this.wifiSsid = wlSsid;
            ssidView.setText(this.wifiSsid);
            ((TextView) layout.findViewById(R.id.tile_status_wireless_iface_details_ssid))
                    .setText(this.wifiSsid);
        }

        if (this.wifiEncryptionType == WirelessEncryptionTypeForQrCode.WEP) {
            final String wlPassword = data.getProperty(this.iface + "_passphrase",
                    defaultValuesIfNotFound ? EMPTY_VALUE_TO_DISPLAY : null);
            if (wlPassword != null) {
                this.wifiPassword = wlPassword;
            }
        } else if (this.wifiEncryptionType == WirelessEncryptionTypeForQrCode.WPA) {
            final String wlWpaPsk = data.getProperty(this.iface + "_wpa_psk",
                    defaultValuesIfNotFound ? EMPTY_VALUE_TO_DISPLAY : null);
            if (wlWpaPsk != null) {
                this.wifiPassword = wlWpaPsk;
            }
        } else if (this.wifiEncryptionType == WirelessEncryptionTypeForQrCode.NONE) {
            this.wifiPassword = "";
        }
//            this.wifiPassword = data.getProperty(this.iface + "_wpa_psk", EMPTY_VALUE_TO_DISPLAY);

        //Ifname
        final TextView ifnameView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_ifname);
        final String wlIfname = data.getProperty(this.iface + "_ifname",
                defaultValuesIfNotFound ? EMPTY_VALUE_TO_DISPLAY : null);
        if (wlIfname != null) {
            ifnameView.setText(wlIfname);
        }

        //Iface State
        final TextView ifaceStateView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_state);
        final String phyIfaceState = data.getProperty(this.iface + IFACE_STATE,
                defaultValuesIfNotFound ? EMPTY_VALUE_TO_DISPLAY : null);
        if (phyIfaceState != null) {
            ifaceStateView.setText(phyIfaceState);
        }

        //MAC
        final TextView hwAddrView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_mac_address);
        final String wlHwAddr = data.getProperty(this.iface + "_hwaddr",
                defaultValuesIfNotFound ? EMPTY_VALUE_TO_DISPLAY : null);
        if (wlHwAddr != null) {
            hwAddr = data.getProperty(this.iface + "_hwaddr", "-");
            hwAddrView.setText(hwAddr);
        }

        //Radio
        final CheckBox radioView = (CheckBox) this.layout.findViewById(R.id.tile_status_wireless_iface_radio);
        radioView.setEnabled(false);
        radioView.setChecked("1".equals(data.getProperty(this.iface + "_radio", data.getProperty(this.parentIface + "_radio"))));
        //Disabled for now, as the same value seems to be returned, regardless of the actual Radio state
        radioView.setVisibility(View.GONE);

        //Mode
        final TextView modeView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_mode);
        String property = data.getProperty(this.iface + "_mode",
                defaultValuesIfNotFound ? EMPTY_VALUE_TO_DISPLAY : null);
        if (property != null) {
            modeView.setText(property.toUpperCase());
        }

        //Net Mode
        final TextView netModeView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_network);
        final String netmode = data.getProperty(this.iface + "_net_mode", data.getProperty(this.parentIface + "_net_mode",
                defaultValuesIfNotFound ? EMPTY_VALUE_TO_DISPLAY : null));
        if (netmode != null) {
            final String mode;
            if ("disabled".equalsIgnoreCase(netmode)) {
                mode = "Disabled";
            } else if ("mixed".equalsIgnoreCase(netmode)) {
                mode = "Mixed";
            } else if ("g-only".equalsIgnoreCase(netmode)) {
                mode = "G-Only";
            } else if ("b-only".equalsIgnoreCase(netmode)) {
                mode = "B-Only";
            } else if ("a-only".equalsIgnoreCase(netmode)) {
                mode = "A-Only";
            } else if ("n-only".equalsIgnoreCase(netmode)) {
                mode = "N-Only";
            }else if ("n5-only".equalsIgnoreCase(netmode)) {
                mode = "N-Only (5 GHz)";
            } else if ("na-only".equalsIgnoreCase(netmode)) {
                mode = "NA-Mixed";
            } else if ("ac-only".equalsIgnoreCase(netmode)) {
                mode = "AC-Only";
            } else if ("ng-only".equalsIgnoreCase(netmode)) {
                mode = "NG-Only";
            } else if ("n5-only".equalsIgnoreCase(netmode)) {
                mode = "N-Only (5GHz)";
            } else if ("acn-mixed".equalsIgnoreCase(netmode)) {
                mode = "AC/N-Mixed";
            } else {
                mode = netmode;
            }
            netModeView.setText(mode);
        }

        //Temperature
        final TextView temperatureView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_temperature);
        final String temperatureProperty = data.getProperty(this.iface + "_temperature", data.getProperty(this.parentIface + "_temperature",
                defaultValuesIfNotFound ? EMPTY_VALUE_TO_DISPLAY : null));
        if (temperatureProperty != null) {
            temperatureView.setText(temperatureProperty);
        }

        //Channel
        final TextView channelView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_channel);
        final String channelProperty = data.getProperty(this.iface + "_channel", data.getProperty(this.parentIface + "_channel",
                defaultValuesIfNotFound ? EMPTY_VALUE_TO_DISPLAY : null));
        if (channelProperty != null) {
            channelView.setText("0".equals(channelProperty) ? "Auto" : channelProperty);
        }

//            //Rate
//            @NonNull final TextView rateView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_rate);
//            final String rateProperty = data.getProperty(this.iface + "_rate_human_readable", "-");
//            rateView.setText(rateProperty);
        //Rate
        final TextView rxRateView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_rx_rate);
        final String rxRateProperty = data.getProperty(this.iface + "_rx_rate_human_readable",
                defaultValuesIfNotFound ? EMPTY_VALUE_TO_DISPLAY : null);
        if (rxRateProperty != null) {
            rxRateView.setText(rxRateProperty);
        }

        final TextView txRateView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_tx_rate);
        final String txRateProperty = data.getProperty(this.iface + "_tx_rate_human_readable",
                defaultValuesIfNotFound ? EMPTY_VALUE_TO_DISPLAY : null);
        if (txRateProperty != null) {
            txRateView.setText(txRateProperty);
        }

        //Packet Info
        final TextView rxPacketsView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_rx_packets);
        final String rxPacketsProperty = data.getProperty(this.iface + "_rx_packets",
                defaultValuesIfNotFound ? EMPTY_VALUE_TO_DISPLAY : null);
        if (rxPacketsProperty != null) {
            rxPacketsView.setText(rxPacketsProperty);
        }

        final TextView txPacketsView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_tx_packets);
        final String txPacketsProperty = data.getProperty(this.iface + "_tx_packets",
                defaultValuesIfNotFound ? EMPTY_VALUE_TO_DISPLAY : null);
        if (txPacketsProperty != null) {
            txPacketsView.setText(txPacketsProperty);
        }

        //TX Power
        final TextView xmitView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_tx_power);
        final String txPwrProperty = data.getProperty(this.iface + "_txpwr", data.getProperty(this.parentIface + "_txpwr",
                defaultValuesIfNotFound ? EMPTY_VALUE_TO_DISPLAY : null));
        if (txPwrProperty != null) {
            xmitView.setText(txPwrProperty);
        }

        //Noise
        final TextView noiseView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_noise_dBm);
        final String noiseProp = data.getProperty(this.iface + "_noise", data.getProperty(this.parentIface + "_noise",
                defaultValuesIfNotFound ? EMPTY_VALUE_TO_DISPLAY : null));
        if (noiseProp != null) {
            noiseView.setText(isNullOrEmpty(noiseProp) ? "-" : (noiseProp + " dBm"));
        }

//            //Update last sync
//            final RelativeTimeTextView lastSyncView = (RelativeTimeTextView) layout.findViewById(R.id.tile_last_sync);
//            lastSyncView.setReferenceTime(mLastSync);
//            lastSyncView.setPrefix("Last sync: ");
    }

    @Nullable
    @Override
    protected OnClickIntent getOnclickIntent() {
        //TODO
        return null;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final String routerUuid = mRouter.getUuid();
        final int itemId = item.getItemId();
        final String wifiSsidNullToEmpty = nullToEmpty(wifiSsid);
        switch (itemId) {
            case R.id.tile_status_wireless_iface_share: {
                if (wifiEncryptionType == null || (isNullOrEmpty(wifiSsid) && wifiPassword == null)) {
                    //menu item should have been disabled, but anyways, you never know :)
                    Toast.makeText(mParentFragmentActivity,
                            "Missing parameters to share WiFi network - try again later", Toast.LENGTH_SHORT).show();
                    return true;
                }

                final Intent intent = new Intent(mParentFragmentActivity, WifiSharingActivity.class);
                intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, routerUuid);
                intent.putExtra(SSID, wifiSsidNullToEmpty);
                intent.putExtra(WifiSharingActivity.ENC_TYPE, wifiEncryptionType.toString().toUpperCase());
                intent.putExtra(WifiSharingActivity.PWD, wifiPassword);
//                intent.putExtra(WirelessIfaceQrCodeActivity.WIFI_QR_CODE, wifiQrCodeString);

                final AlertDialog alertDialog = Utils.buildAlertDialog(mParentFragmentActivity, null,
                        String.format("Generating data to share for '%s'", wifiSsidNullToEmpty),
                        false, false);
                alertDialog.show();
                ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mParentFragmentActivity.startActivity(intent);
                        alertDialog.cancel();
                    }
                }, 2500);
            }

                return true;

            case R.id.tile_status_wireless_iface_traffic_shaping: {
                if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
                    //Download the full version to unlock this version
                    Utils.displayUpgradeMessage(mParentFragmentActivity, "Traffic Shaping");
                    return true;
                }

                //TODO
            }
                return true;
            case R.id.tile_status_wireless_iface_set_up:
            case R.id.tile_status_wireless_iface_set_down: {
                if (Strings.isNullOrEmpty(phyIface)) {
                    Utils.displayMessage(mParentFragmentActivity,
                            "Could not determine physical interface at this time - please try again later",
                            Style.ALERT);
                    return true;
                }
                final Bundle token = new Bundle();
                final PhysicalInterfaceState physicalInterfaceState = itemId == R.id.tile_status_wireless_iface_set_up ?
                        PhysicalInterfaceState.UP :
                        PhysicalInterfaceState.DOWN;

                token.putSerializable(PHYSICAL_IFACE_STATE_ACTION,
                        physicalInterfaceState);

                new UndoBarController.UndoBar(mParentFragmentActivity)
                        .message(
                                String.format(
                                        "Bringing %s physical interface %s (backing wireless network '%s').",
                                        physicalInterfaceState.toString().toLowerCase(),
                                        phyIface,
                                        wifiSsid))
                        .listener(new UndoBarController.AdvancedUndoListener() {

                            @Override
                            public void onUndo(@Nullable Parcelable token) {
                                //Nothing to do
                            }

                            @Override
                            public void onHide(@Nullable Parcelable parcelable) {
                                if (parcelable instanceof Bundle) {
                                    final Bundle token = (Bundle) parcelable;

                                    try {
                                        final Serializable phyInterfaceStateSer = token.getSerializable(PHYSICAL_IFACE_STATE_ACTION);
                                        Crashlytics.log(Log.DEBUG, LOG_TAG, "phyInterfaceStateSer: [" + phyInterfaceStateSer + "]");
                                        if (!(phyInterfaceStateSer instanceof PhysicalInterfaceState)) {
                                            return;
                                        }

                                        final PhysicalInterfaceState interfaceState =
                                                (PhysicalInterfaceState) phyInterfaceStateSer;

                                        ActionManager.runTasks(new TogglePhysicalInterfaceStateRouterAction(
                                                mRouter,
                                                mParentFragmentActivity,
                                                new RouterActionListener() {
                                                    @Override
                                                    public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router, Object returnData) {
                                                        Utils.displayMessage(mParentFragmentActivity,
                                                                "Physical Interface '" + phyIface + "' (for wireless network '" +
                                                                        wifiSsid + "') is now '" + interfaceState + "'",
                                                                Style.CONFIRM);
                                                        // Update info right away
                                                        //Run on main thread to avoid the exception:
                                                        //"Only the original thread that created a view hierarchy can touch its views."
                                                        mParentFragmentActivity.runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                ((TextView) layout.findViewById(R.id.tile_status_wireless_iface_state))
                                                                        .setText(StringUtils.capitalize(interfaceState.toString().toLowerCase()));
                                                            }
                                                        });
                                                    }

                                                    @Override
                                                    public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull Router router, @Nullable Exception exception) {
                                                        Utils.displayMessage(mParentFragmentActivity,
                                                                String.format("Error: %s",
                                                                        Utils.handleException(exception).first),
                                                                Style.ALERT);
                                                    }
                                                },
                                                mGlobalPreferences,
                                                phyIface,
                                                interfaceState));
                                    } catch (IllegalArgumentException | NullPointerException | IllegalStateException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            @Override
                            public void onClear(@NonNull Parcelable[] token) {
                                //Nothing to do
                            }
                        })
                        .token(token)
                        .show();
            }
                return true;
            case R.id.tile_status_wireless_iface_security: {

                if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
                    //Download the full version to unlock this version
                    Utils.displayUpgradeMessage(mParentFragmentActivity, "Edit Wireless Security Settings");
                    return true;
                }

                final NVRAMInfo nvramInfo = new NVRAMInfo()
                        .setProperty(WirelessIfaceTile.IFACE, this.iface)
                        .setProperty(WirelessIfaceTile.PARENT_IFACE, nullToEmpty(this.parentIface))
                        .setProperty(SSID, wifiSsidNullToEmpty)
                        .setProperty(EditWirelessSecuritySettingsActivity.HWADDR,
                                nullToEmpty(hwAddr));
                if (mNvramInfo != null) {
                    nvramInfo.putAll(mNvramInfo);
                }

                final Intent intent = new Intent(mParentFragmentActivity, EditWirelessSecuritySettingsActivity.class);
                intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, routerUuid);
                intent.putExtra(SSID, wifiSsidNullToEmpty);
                intent.putExtra(EditWirelessSecuritySettingsActivity.WIRELESS_SECURITY_NVRAMINFO,
                        nvramInfo);

                final AlertDialog alertDialog = Utils.buildAlertDialog(mParentFragmentActivity, null,
                        String.format("Loading Security Settings for '%s'", wifiSsidNullToEmpty), false, false);
                alertDialog.show();
                ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mWirelessSecurityFormOpened.set(true);
                        ((AbstractBaseFragment) mParentFragment)
                                .startActivityForResult(intent,
                                        new WirelessSecuritySettingsActivityResultListener(wifiSsidNullToEmpty));
                        alertDialog.cancel();
                    }
                }, 2500);
            }
                return true;
            default:
                break;
        }
        return false;
    }

    @NonNull
    public static String escapeString(@NonNull final String string) {
        final List<Character> toEscape = Arrays.asList('\\', ';', ',', ':', '"');
        String output = "";
        for (int i = 0; i < string.length(); i++) {
            final char charAt = string.charAt(i);
            if (toEscape.contains(charAt)) {
                output += ('\\' + charAt);
            } else {
                output += charAt;
            }
        }

        if (HEX_ONLY_QR_CODE_PATTERN.matcher(output).matches()) {
            output = ("\"" + output + "\"");
        }
        return output;
    }

    public static final int IfaceStatsType_RX_BYTES = 1;
    public static final int IfaceStatsType_TX_BYTES = 2;

    public enum IfaceStatsType {
        RX_BYTES("rx_bytes"),
        TX_BYTES("tx_bytes");

        final String metric;

        IfaceStatsType(String metric) {
            this.metric = metric;
        }

        public String getMetric() {
            return metric;
        }
    }

    public static final int WirelessEncryptionTypeForQrCode_WPA = 1;
    public static final int WirelessEncryptionTypeForQrCode_WEP = 2;
    public static final int WirelessEncryptionTypeForQrCode_NONE = 3;


    public enum WirelessEncryptionTypeForQrCode {
        WPA("WPA"),
        WEP("WEP"),
        NONE("nopass");

        private final String encType;

        WirelessEncryptionTypeForQrCode(String encType) {
            this.encType = encType;
        }

        @Override
        public String toString() {
            return encType;
        }
    }

    public static final int TemperatureUnit_CELSIUS = 1;
    public static final int TemperatureUnit_FAHRENHEIT = 2;

    public enum TemperatureUnit {
        CELSIUS("C"),
        FAHRENHEIT("F");

        private final String unitDisplay;

        TemperatureUnit(String unitDisplay) {
            this.unitDisplay = unitDisplay;
        }

        @Override
        public String toString() {
            return unitDisplay;
        }
    }

    private class WirelessSecuritySettingsActivityResultListener
            implements ActivityResultListener, UndoBarController.AdvancedUndoListener, RouterActionListener {

        @NonNull
        private final String wifiSsid;

        private WirelessSecuritySettingsActivityResultListener(@NonNull final String wifiSsid) {
            this.wifiSsid = wifiSsid;
        }

        @Override
        public void onResultCode(int resultCode, Intent data) {
            try {
                Crashlytics.log(Log.DEBUG, LOG_TAG, "onResultCode: " + resultCode);
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        final NVRAMInfo newNvramInfoData = (NVRAMInfo) data.getSerializableExtra(WL_SECURITY_NVRAMINFO);
                        if (newNvramInfoData == null || newNvramInfoData.isEmpty()) {
                            Utils.displayMessage(mParentFragmentActivity, "No change", Style.INFO);
                            break;
                        }

                        final Bundle token = new Bundle();
                        token.putString(DDWRTMainActivity.ROUTER_ACTION, RouterAction.SET_NVRAM_VARIABLES.name());
                        token.putSerializable(WL_SECURITY_NVRAMINFO, newNvramInfoData);

                        new UndoBarController.UndoBar(mParentFragmentActivity)
                                .message(
                                        String.format("Security Settings for wireless network '%s' will be updated.",
                                                wifiSsid))
                                .listener(this)
                                .token(token)
                                .show();
                        break;
                    default:
                        //Ignored
                        break;
                }
            } finally {
                mWirelessSecurityFormOpened.set(false);
            }
        }

        @Override
        public void onHide(@Nullable Parcelable parcelable) {
            if (parcelable instanceof Bundle) {
                final Bundle token = (Bundle) parcelable;
                final String routerAction = token.getString(DDWRTMainActivity.ROUTER_ACTION);
                Crashlytics.log(Log.DEBUG, LOG_TAG, "routerAction: [" + routerAction + "]");
                if (isNullOrEmpty(routerAction)) {
                    return;
                }
                try {
                    switch (RouterAction.valueOf(routerAction)) {
                        case SET_NVRAM_VARIABLES:
                            final NVRAMInfo nvramInfo = (NVRAMInfo) token.getSerializable(WL_SECURITY_NVRAMINFO);
                            if (nvramInfo == null) {
                                throw new IllegalStateException("Internal error - please try again later.");
                            }
                            ActionManager.runTasks(new SetNVRAMVariablesAction(
                                    mRouter,
                                    mParentFragmentActivity,
                                    nvramInfo,
                                    false,
                                    this,
                                    mGlobalPreferences,
                                    "/sbin/startservice wlconf" //shall we stopservice first???
                                    //Maybe /sbin/startservice lan or /sbin/startservice wan ?
//                                    ,
//                                    /*
//                                    # the next few lines will restart the interface,
//                                    # which simulates the save and apply buttons in the webGUI
//                                     */
//                                    "( /sbin/stopservice wan || true ) && sleep 2 && ( /sbin/startservice wan || true )"
                            ));

                            break;
                        default:
                            //Ignored
                            break;
                    }
                } catch (IllegalArgumentException | NullPointerException | IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onClear(@NonNull Parcelable[] parcelables) {
            //Nothing to do
        }

        @Override
        public void onUndo(@Nullable Parcelable parcelable) {
            //Nothing to do
        }

        @Override
        public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router, final Object returnData) {
            Utils.displayMessage(mParentFragmentActivity,
                    "Security Settings updated for WiFi network '" + wifiSsid + "'",
                    Style.CONFIRM);
//            Update info right away
            if (returnData instanceof NVRAMInfo) {
                //Run on main thread to avoid the exception:
                //"Only the original thread that created a view hierarchy can touch its views."
                mParentFragmentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateTileDisplayInfo((NVRAMInfo) returnData, false);
                    }
                });
            }
        }

        @Override
        public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull Router router, @Nullable Exception exception) {
            Utils.displayMessage(mParentFragmentActivity,
                    String.format("Error: %s",
                            Utils.handleException(exception).first),
                    Style.ALERT);
        }
    }
}
