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
package org.rm3l.router_companion.tiles.services.vpn.client;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.router_companion.RouterCompanionAppConstants.VPN_PPTP_TOGGLES_MUTUALLY_EXCLUSIVE;
import static org.rm3l.router_companion.tiles.services.vpn.client.EditOpenVPNClientSettingsActivity.EDIT_OPEN_VPN_CLIENT_SETTINGS_LOCAL_IPS_AUTOCOMPLETE_PREF;
import static org.rm3l.router_companion.tiles.services.vpn.client.EditOpenVPNClientSettingsActivity.EDIT_OPEN_VPN_CLIENT_SETTINGS_LOCAL_SUBNETS_AUTOCOMPLETE_PREF;
import static org.rm3l.router_companion.tiles.services.vpn.client.EditOpenVPNClientSettingsActivity.EDIT_OPEN_VPN_CLIENT_SETTINGS_MTU_SETTINGS_AUTOCOMPLETE_PREF;
import static org.rm3l.router_companion.tiles.services.vpn.client.EditOpenVPNClientSettingsActivity.EDIT_OPEN_VPN_CLIENT_SETTINGS_SERVER_PORTS_AUTOCOMPLETE_PREF;
import static org.rm3l.router_companion.tiles.services.vpn.client.EditOpenVPNClientSettingsActivity.EDIT_OPEN_VPN_CLIENT_SETTINGS_TARGET_SERVERS_AUTOCOMPLETE_PREF;
import static org.rm3l.router_companion.tiles.services.vpn.client.EditOpenVPNClientSettingsActivity.EDIT_OPEN_VPN_CLIENT_SETTINGS_UDP_FRAGMENTS_AUTOCOMPLETE_PREF;
import static org.rm3l.router_companion.tiles.status.wireless.WirelessIfaceTile.CAT_SYS_CLASS_NET_S_STATISTICS;
import static org.rm3l.router_companion.tiles.status.wireless.WirelessIfaceTile.IfaceStatsType.RX_BYTES;
import static org.rm3l.router_companion.tiles.status.wireless.WirelessIfaceTile.IfaceStatsType.TX_BYTES;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import androidx.appcompat.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.actions.ActionManager;
import org.rm3l.router_companion.actions.RouterAction;
import org.rm3l.router_companion.actions.RouterActionListener;
import org.rm3l.router_companion.actions.SetNVRAMVariablesAction;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.main.DDWRTMainActivity;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.tiles.status.wireless.WirelessIfaceTile;
import org.rm3l.router_companion.utils.ReportingUtils;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.snackbar.SnackbarCallback;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style;

public class OpenVPNClientTile extends DDWRTTile<NVRAMInfo>
        implements DDWRTTile.ActivityResultListener, SnackbarCallback,
        RouterActionListener {

    private class DDWRTOpenvpnClStateUnknown extends DDWRTNoDataException {

        public DDWRTOpenvpnClStateUnknown(@Nullable String detailMessage) {
            super(detailMessage);
        }
    }

    private class ManageOpenVpnClientToggle implements View.OnClickListener {

        private boolean enable;

        @Override
        public void onClick(View view) {

            isToggleStateActionRunning.set(true);

            if (!(view instanceof CompoundButton)) {
                ReportingUtils.reportException(null, new IllegalStateException(
                        "ManageOpenVpnClientToggle#onClick: " + "view is NOT an instance of CompoundButton!"));
                isToggleStateActionRunning.set(false);
                return;
            }

            final CompoundButton compoundButton = (CompoundButton) view;

            mParentFragmentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    compoundButton.setEnabled(false);
                }
            });

            this.enable = compoundButton.isChecked();

            if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
                Utils.displayUpgradeMessage(mParentFragmentActivity, "Toggle OpenVPN Client");
                isToggleStateActionRunning.set(false);
                mParentFragmentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        compoundButton.setChecked(!enable);
                        compoundButton.setEnabled(true);
                    }
                });
                return;
            }

            final NVRAMInfo nvramInfoToSet = new NVRAMInfo();

            nvramInfoToSet.setProperty(NVRAMInfo.Companion.getOPENVPNCL_ENABLE(), enable ? "1" : "0");

            SnackbarUtils.buildSnackbar(mParentFragmentActivity,
                    String.format("OpenVPN Client will be %s on '%s' (%s). ", enable ? "enabled" : "disabled",
                            mRouter.getDisplayName(), mRouter.getRemoteIpAddress()), "CANCEL",
                    Snackbar.LENGTH_LONG, new SnackbarCallback() {
                        @Override
                        public void onDismissEventActionClick(int event, @Nullable Bundle bundle)
                                throws Exception {
                            cancel();
                        }

                        @Override
                        public void onDismissEventConsecutive(int event, @Nullable Bundle bundle)
                                throws Exception {
                            cancel();
                        }

                        @Override
                        public void onDismissEventManual(int event, @Nullable Bundle bundle)
                                throws Exception {
                            cancel();
                        }

                        @Override
                        public void onDismissEventSwipe(int event, @Nullable Bundle bundle)
                                throws Exception {
                            cancel();
                        }

                        @Override
                        public void onDismissEventTimeout(int event, @Nullable Bundle bundle)
                                throws Exception {
                            final RouterActionListener listener = new RouterActionListener() {
                                @Override
                                public void onRouterActionFailure(@NonNull RouterAction routerAction,
                                        @NonNull final Router router, @Nullable final Exception exception) {
                                    mParentFragmentActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                compoundButton.setChecked(!enable);
                                                Utils.displayMessage(mParentFragmentActivity, String.format(
                                                        "Error while trying to %s OpenVPN Client on '%s' (%s): %s",
                                                        enable ? "enable" : "disable", router.getDisplayName(),
                                                        router.getRemoteIpAddress(),
                                                        Utils.handleException(exception).first),
                                                        Style.ALERT);
                                            } finally {
                                                compoundButton.setEnabled(true);
                                                isToggleStateActionRunning.set(false);
                                            }
                                        }
                                    });
                                }

                                @Override
                                public void onRouterActionSuccess(@NonNull RouterAction routerAction,
                                        @NonNull final Router router, Object returnData) {
                                    mParentFragmentActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            try {
                                                compoundButton.setChecked(enable);
                                                Utils.displayMessage(mParentFragmentActivity,
                                                        String.format(
                                                                "OpenVPN Client %s successfully on host '%s' (%s). ",
                                                                enable ? "enabled" : "disabled",
                                                                router.getDisplayName(),
                                                                router.getRemoteIpAddress()), Style.CONFIRM);
                                            } finally {
                                                compoundButton.setEnabled(true);
                                                isToggleStateActionRunning.set(false);
                                                if (mLoader != null) {
                                                    //Reload everything right away
                                                    doneWithLoaderInstance(OpenVPNClientTile.this, mLoader, 1l);
                                                }
                                            }
                                        }
                                    });
                                }
                            };

                            final boolean pptpClStatusToSet = !enable;

                            if (mParentFragmentPreferences != null && mParentFragmentPreferences.getBoolean(
                                    VPN_PPTP_TOGGLES_MUTUALLY_EXCLUSIVE, false)) {
                                new AlertDialog.Builder(mParentFragmentActivity).setIcon(
                                        R.drawable.ic_action_alert_warning)
                                        .setTitle("Toggle PPTP Client status")
                                        .setMessage(String.format(Locale.US,
                                                "Router will be rebooted. Do you wish to %s PPTP Client at the same time?",
                                                pptpClStatusToSet ? "start" : "stop"))
                                        .setCancelable(true)
                                        .setPositiveButton("Yes!", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                nvramInfoToSet.setProperty(
                                                        NVRAMInfo.Companion.getPPTPD_CLIENT_ENABLE(),
                                                        pptpClStatusToSet ? "1" : "0");

                                                Utils.displayMessage(mParentFragmentActivity,
                                                        String.format("%s OpenVPN Client (and %s PPTP Client) ...",
                                                                enable ? "Enabling" : "Disabling",
                                                                pptpClStatusToSet ? "Enabling" : "Disabling"),
                                                        Style.INFO);

                                                ActionManager.runTasks(
                                                        new SetNVRAMVariablesAction(mRouter, mParentFragmentActivity,
                                                                nvramInfoToSet, true, listener, mGlobalPreferences));
                                            }
                                        })
                                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {

                                                Utils.displayMessage(mParentFragmentActivity,
                                                        String.format("%s OpenVPN Client...",
                                                                enable ? "Enabling" : "Disabling"), Style.INFO);

                                                ActionManager.runTasks(
                                                        new SetNVRAMVariablesAction(mRouter, mParentFragmentActivity,
                                                                nvramInfoToSet, true, listener, mGlobalPreferences));
                                            }
                                        })
                                        .create()
                                        .show();

                                //                                      Utils.displayMessage(mParentFragmentActivity,
                                //                                              String.format("%s OpenVPN Client...",
                                //                                                      enable ? "Enabling" : "Disabling"),
                                //                                              Style.INFO);

                                //                                      new SetNVRAMVariablesAction(mParentFragmentActivity,
                                //                                              nvramInfoToSet,
                                //                                              true,
                                //                                              listener,
                                //                                              mGlobalPreferences).
                                //
                                //                                              execute(mRouter);
                            } else {
                                Utils.displayMessage(mParentFragmentActivity,
                                        String.format("%s OpenVPN Client...", enable ? "Enabling" : "Disabling"),
                                        Style.INFO);

                                ActionManager.runTasks(
                                        new SetNVRAMVariablesAction(mRouter, mParentFragmentActivity, nvramInfoToSet,
                                                true, listener, mGlobalPreferences));
                            }
                        }

                        private void cancel() {
                            mParentFragmentActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        compoundButton.setChecked(!enable);
                                        compoundButton.setEnabled(true);
                                    } finally {
                                        isToggleStateActionRunning.set(false);
                                    }
                                }
                            });
                        }

                    }, new Bundle(), true);

            //new UndoBarController.UndoBar(mParentFragmentActivity).message(
            //    String.format("OpenVPN Client will be %s on '%s' (%s). ", enable ? "enabled" : "disabled",
            //        mRouter.getDisplayName(), mRouter.getRemoteIpAddress()))
            //    .listener(new UndoBarController.AdvancedUndoListener() {
            //                @Override public void onHide(@Nullable Parcelable parcelable) {
            //
            //
            //                }
            //
            //                @Override public void onClear(@NonNull Parcelable[] parcelables) {
            //
            //                }
            //
            //                @Override public void onUndo(@Nullable Parcelable parcelable) {
            //                  mParentFragmentActivity.runOnUiThread(new Runnable() {
            //                    @Override public void run() {
            //                      try {
            //                        compoundButton.setChecked(!enable);
            //                        compoundButton.setEnabled(true);
            //                      } finally {
            //                        isToggleStateActionRunning.set(false);
            //                      }
            //                    }
            //                  });
            //                }
            //              }
            //
            //    )
            //    .
            //
            //        token(new Bundle()
            //
            //        )
            //    .
            //
            //        show();
        }
    }

    public static final String OPENVPNCL__DEV = "___openvpncl__dev";

    public static final String OPENVPNCL__DEV_RX_RATE = OPENVPNCL__DEV + "_rx_rate_human_readable";

    public static final String OPENVPNCL__DEV_TX_RATE = OPENVPNCL__DEV + "_tx_rate_human_readable";

    public static final String OPENVPNCL__DEV_RX_PACKETS = OPENVPNCL__DEV + "_rx_packets";

    public static final String OPENVPNCL__DEV_TX_PACKETS = OPENVPNCL__DEV + "_tx_packets";

    public static final String N_A = "-";

    public static final String OPENVPNCL_NVRAMINFO = "OPENVPNCL_NVRAMINFO";

    private static final String LOG_TAG = OpenVPNClientTile.class.getSimpleName();

    private AtomicBoolean isToggleStateActionRunning = new AtomicBoolean(false);

    private long mLastSync;

    private AsyncTaskLoader<NVRAMInfo> mLoader;

    private NVRAMInfo mNvramInfo;

    public OpenVPNClientTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments,
            @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_services_openvpn_client, null);
    }

    @Override
    public Integer getTileHeaderViewId() {
        return R.id.tile_services_openvpn_client_hdr;
    }

    @Override
    public Integer getTileTitleViewId() {
        return R.id.tile_services_openvpn_client_title;
    }

    @Override
    public void onDismissEventTimeout(int event, @Nullable Bundle token) throws Exception {
        final String routerAction =
                token != null ? token.getString(DDWRTMainActivity.ROUTER_ACTION) : null;
        Crashlytics.log(Log.DEBUG, LOG_TAG, "routerAction: [" + routerAction + "]");
        if (isNullOrEmpty(routerAction)) {
            return;
        }
        try {
            switch (RouterAction.valueOf(routerAction)) {
                case SET_NVRAM_VARIABLES:
                    final NVRAMInfo nvramInfo = (NVRAMInfo) token.getSerializable(OPENVPNCL_NVRAMINFO);
                    final String openvpnClEnable = nullToEmpty(nvramInfo.getProperty(
                            NVRAMInfo.Companion.getOPENVPNCL_ENABLE()));

                    if (mParentFragmentPreferences != null && mParentFragmentPreferences.getBoolean(
                            VPN_PPTP_TOGGLES_MUTUALLY_EXCLUSIVE, false) && Arrays.asList("0", "1")
                            .contains(openvpnClEnable)) {
                        //Display a popup inviting user to toggle PPTP Client status at the same time
                        final String pptpClientStatusToSet;
                        if ("0".equals(openvpnClEnable)) {
                            pptpClientStatusToSet = "1";
                        } else {
                            pptpClientStatusToSet = "0";
                        }
                        new AlertDialog.Builder(mParentFragmentActivity).setIcon(
                                R.drawable.ic_action_alert_warning)
                                .setTitle("Toggle PPTP Client status")
                                .setMessage(String.format(Locale.US,
                                        "Router will be rebooted. Do you wish to %s PPTP Client at the same time?",
                                        "0".equals(pptpClientStatusToSet) ? "stop" : "start"))
                                .setCancelable(true)
                                .setPositiveButton("Yes!", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        nvramInfo.setProperty(NVRAMInfo.Companion.getPPTPD_CLIENT_ENABLE(),
                                                pptpClientStatusToSet);
                                        ActionManager.runTasks(
                                                new SetNVRAMVariablesAction(mRouter, mParentFragmentActivity,
                                                        nvramInfo,
                                                        true, OpenVPNClientTile.this, mGlobalPreferences));
                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        ActionManager.runTasks(
                                                new SetNVRAMVariablesAction(mRouter, mParentFragmentActivity,
                                                        nvramInfo,
                                                        true, OpenVPNClientTile.this, mGlobalPreferences));
                                    }
                                })
                                .create()
                                .show();
                    } else {
                        ActionManager.runTasks(
                                new SetNVRAMVariablesAction(mRouter, mParentFragmentActivity, nvramInfo, true,
                                        this, mGlobalPreferences));
                    }
                    break;
                default:
                    //Ignored
                    break;
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLoadFinished(Loader<NVRAMInfo> loader, NVRAMInfo data) {
        try {
            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

            layout.findViewById(R.id.tile_services_openvpn_client_header_loading_view)
                    .setVisibility(View.GONE);
            layout.findViewById(R.id.tile_services_openvpn_client_loading_view).setVisibility(View.GONE);
            layout.findViewById(R.id.tile_services_openvpn_client_grid_layout)
                    .setVisibility(View.VISIBLE);
            layout.findViewById(R.id.tile_services_openvpn_client_note).setVisibility(View.VISIBLE);

            Exception preliminaryCheckException = null;
            if (data == null) {
                //noinspection ThrowableInstanceNeverThrown
                preliminaryCheckException = new DDWRTNoDataException("No Data!");
            } else //noinspection ThrowableResultOfMethodCallIgnored
                if (data.getException() == null) {
                    final String openvpnClEnabled = data.getProperty(
                            NVRAMInfo.Companion.getOPENVPNCL_ENABLE());
                    if (openvpnClEnabled == null || !Arrays.asList("0", "1").contains(openvpnClEnabled)) {
                        //noinspection ThrowableInstanceNeverThrown
                        preliminaryCheckException = new DDWRTOpenvpnClStateUnknown("Unknown state");
                    }
                }

            final SwitchCompat enableTraffDataButton =
                    (SwitchCompat) this.layout.findViewById(R.id.tile_services_openvpn_client_status);
            enableTraffDataButton.setVisibility(View.VISIBLE);

            final boolean makeToogleEnabled = (data != null && data.getData() != null && data.getData()
                    .containsKey(NVRAMInfo.Companion.getOPENVPNCL_ENABLE()));

            if (!isToggleStateActionRunning.get()) {
                if (makeToogleEnabled) {
                    if ("1".equals(data.getProperty(NVRAMInfo.Companion.getOPENVPNCL_ENABLE()))) {
                        //Enabled
                        enableTraffDataButton.setChecked(true);
                    } else {
                        //Disabled
                        enableTraffDataButton.setChecked(false);
                    }
                    enableTraffDataButton.setEnabled(true);
                } else {
                    enableTraffDataButton.setChecked(false);
                    enableTraffDataButton.setEnabled(false);
                }

                enableTraffDataButton.setOnClickListener(new ManageOpenVpnClientToggle());
            }

            if (preliminaryCheckException != null) {
                data = new NVRAMInfo().setException(preliminaryCheckException);
            }

            final TextView errorPlaceHolderView =
                    (TextView) this.layout.findViewById(R.id.tile_services_openvpn_client_error);

            final Exception exception = data.getException();

            if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

                mNvramInfo = new NVRAMInfo();
                mNvramInfo.putAll(data);

                if (exception == null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }

                updateTileDisplayData(data, true);

                //Update last sync
                final RelativeTimeTextView lastSyncView =
                        (RelativeTimeTextView) layout.findViewById(R.id.tile_last_sync);
                lastSyncView.setReferenceTime(mLastSync);
                lastSyncView.setPrefix("Last sync: ");
            }

            if (exception != null && !(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {
                //noinspection ThrowableResultOfMethodCallIgnored
                final Throwable rootCause = Throwables.getRootCause(exception);
                errorPlaceHolderView.setText(
                        "Error: " + (rootCause != null ? rootCause.getMessage() : "null"));
                final Context parentContext = this.mParentFragmentActivity;
                errorPlaceHolderView.setOnClickListener(new View.OnClickListener() {
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

            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished(): done loading!");
        } finally {
            mRefreshing.set(false);
            doneWithLoaderInstance(this, loader);
        }
    }

    @Override
    public void onResultCode(int resultCode, Intent data) {
        switch (resultCode) {
            case Activity.RESULT_OK:
                final NVRAMInfo newNvramInfoData =
                        (NVRAMInfo) data.getSerializableExtra(OPENVPNCL_NVRAMINFO);
                if (newNvramInfoData == null || newNvramInfoData.isEmpty()) {
                    Utils.displayMessage(mParentFragmentActivity, "No change", Style.INFO);
                    break;
                }

                final Bundle token = new Bundle();
                token.putString(DDWRTMainActivity.ROUTER_ACTION, RouterAction.SET_NVRAM_VARIABLES.name());
                token.putSerializable(OPENVPNCL_NVRAMINFO, newNvramInfoData);

                SnackbarUtils.buildSnackbar(mParentFragmentActivity,
                        "OpenVPN Client Settings will be updated",
                        "CANCEL",
                        Snackbar.LENGTH_LONG,
                        this,
                        token, true);

                //new UndoBarController.UndoBar(mParentFragmentActivity).message(
                //    "OpenVPN Client Settings will be updated").listener(this).token(token).show();
                break;
            default:
                //Ignored
                break;
        }
    }

    @Override
    public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull Router router,
            @Nullable Exception exception) {
        Utils.displayMessage(mParentFragmentActivity,
                String.format("Error: %s", Utils.handleException(exception).first), Style.ALERT);
    }

    @Override
    public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router,
            @Nullable final Object returnData) {
        Utils.displayMessage(mParentFragmentActivity, "Success", Style.CONFIRM);
        //Update info right away
        if (returnData instanceof NVRAMInfo) {
            //Run on main thread to avoid the exception:
            //"Only the original thread that created a view hierarchy can touch its views."
            mParentFragmentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateTileDisplayData((NVRAMInfo) returnData, false);
                }
            });
        }
    }

    @Nullable
    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        mLoader = new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {

                try {
                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Init background loader for "
                            + OpenVPNClientTile.class
                            + ": routerInfo="
                            + mRouter
                            + " / nbRunsLoader="
                            + nbRunsLoader);

                    if (mRefreshing.getAndSet(true)) {
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    updateProgressBarViewSeparator(0);
                    mLastSync = System.currentTimeMillis();

                    mNvramInfo = null;

                    final NVRAMInfo nvramInfo = new NVRAMInfo();

                    NVRAMInfo nvramInfoTmp = null;

                    try {
                        updateProgressBarViewSeparator(10);
                        nvramInfoTmp = SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity, mRouter,
                                mGlobalPreferences,
                                //Status: {1,0}
                                NVRAMInfo.Companion.getOPENVPNCL_ENABLE(),
                                //Server IP Name
                                NVRAMInfo.Companion.getOPENVPNCL_REMOTEIP(),
                                //Port (from 1 to 65535): default=1194
                                NVRAMInfo.Companion.getOPENVPNCL_REMOTEPORT(),
                                //Tunnel Device: {"tun", "tap"}
                                NVRAMInfo.Companion.getOPENVPNCL_TUNTAP(),
                                //Tunnel Protocol: { "udp", "tcp-client"}
                                NVRAMInfo.Companion.getOPENVPNCL_PROTO(),
                                //Encryption Cipher:
                                //{aes-512-cbc, aes-256-cbc, aes-192-cbc, aes-128-cbc, bf-cbc, none}
                                NVRAMInfo.Companion.getOPENVPNCL_CIPHER(),
                                //Hash Algorithm
                                //{sha512, sha256, sha1, md5, md4, none}
                                NVRAMInfo.Companion.getOPENVPNCL_AUTH(),
                                //TLS Cipher
                                //{TLS-DHE-RSA-WITH-AES-256-GCM-SHA384,
                                //TLS-DHE-RSA-WITH-AES-256-CBC-SHA256,
                                //TLS-DHE-RSA-WITH-AES-128-CBC-SHA,
                                //TLS-RSA-WITH-AES-256-GCM-SHA384,
                                //TLS-RSA-WITH-AES-256-CBC-SHA256,
                                //TLS-RSA-WITH-AES-128-CBC-SHA,
                                //TLS-RSA-WITH-RC4-128-MD5,
                                //0}
                                //Advanced Options: {1,0}
                                NVRAMInfo.Companion.getOPENVPNCL_ADV(), NVRAMInfo.Companion.getOPENVPNCL_TLSCIP(),
                                //LZO Compression: {yes, adaptive, no, off}
                                NVRAMInfo.Companion.getOPENVPNCL_LZO(),
                                //NAT: {1, 0}
                                NVRAMInfo.Companion.getOPENVPNCL_NAT(),
                                //Firewall Protection (enabled if openvpncl_nat==1): {1,0}
                                NVRAMInfo.Companion.getOPENVPNCL_SEC(),
                                //Bridge TAP to br0 (enabled if openvpncl_nat==0)"{1,0}
                                NVRAMInfo.Companion.getOPENVPNCL_BRIDGE(),
                                //IP Address
                                NVRAMInfo.Companion.getOPENVPNCL_IP(),
                                //Subnet Mask
                                NVRAMInfo.Companion.getOPENVPNCL_MASK(),
                                //Tunnel MTU Setting: (from 1 to 65535)
                                NVRAMInfo.Companion.getOPENVPNCL_MTU(),
                                //Tunnel UDP Fragment (num max chars=5): if nothing => Disabled
                                NVRAMInfo.Companion.getOPENVPNCL_FRAGMENT(),
                                //Tunnel UDP MSS-Fix: {1,0}
                                NVRAMInfo.Companion.getOPENVPNCL_MSSFIX(),
                                //nsCertType verification: {1,0}
                                NVRAMInfo.Companion.getOPENVPNCL_CERTTYPE(),
                                //TLS Auth Key
                                NVRAMInfo.Companion.getOPENVPNCL_TLSAUTH(),
                                //Additional Config
                                NVRAMInfo.Companion.getOPENVPNCL_CONFIG(),
                                //Policy based Routing
                                NVRAMInfo.Companion.getOPENVPNCL_ROUTE(),
                                //PKCS12 Key
                                NVRAMInfo.Companion.getOPENVPNCL_PKCS_12(),
                                //Static Key
                                NVRAMInfo.Companion.getOPENVPNCL_STATIC(),
                                //CA Cert
                                NVRAMInfo.Companion.getOPENVPNCL_CA(),
                                //Public Client Cert
                                NVRAMInfo.Companion.getOPENVPNCL_CLIENT(),
                                //Private Client Key
                                NVRAMInfo.Companion.getOPENVPNCL_KEY());
                    } finally {
                        if (nvramInfoTmp != null) {
                            nvramInfo.putAll(nvramInfoTmp);
                        }

                        boolean applyNewPrefs = false;
                        String property = nvramInfo.getProperty(NVRAMInfo.Companion.getOPENVPNCL_REMOTEIP());
                        final SharedPreferences.Editor editor = mGlobalPreferences.edit();
                        if (!Strings.isNullOrEmpty(property)) {
                            final Set<String> mGlobalPreferencesStringSet = new HashSet<>(
                                    mGlobalPreferences.getStringSet(
                                            EDIT_OPEN_VPN_CLIENT_SETTINGS_TARGET_SERVERS_AUTOCOMPLETE_PREF,
                                            new HashSet<String>()));
                            if (!mGlobalPreferencesStringSet.contains(property)) {
                                mGlobalPreferencesStringSet.add(property);
                                editor.putStringSet(EDIT_OPEN_VPN_CLIENT_SETTINGS_TARGET_SERVERS_AUTOCOMPLETE_PREF,
                                        mGlobalPreferencesStringSet);
                                applyNewPrefs = true;
                            }
                        }

                        property = nvramInfo.getProperty(NVRAMInfo.Companion.getOPENVPNCL_REMOTEPORT());
                        if (!Strings.isNullOrEmpty(property)) {
                            final Set<String> mGlobalPreferencesStringSet = new HashSet<>(
                                    mGlobalPreferences.getStringSet(
                                            EDIT_OPEN_VPN_CLIENT_SETTINGS_SERVER_PORTS_AUTOCOMPLETE_PREF,
                                            new HashSet<String>()));
                            if (!mGlobalPreferencesStringSet.contains(property)) {
                                mGlobalPreferencesStringSet.add(property);
                                editor.putStringSet(EDIT_OPEN_VPN_CLIENT_SETTINGS_SERVER_PORTS_AUTOCOMPLETE_PREF,
                                        mGlobalPreferencesStringSet);
                                applyNewPrefs = true;
                            }
                        }

                        property = nvramInfo.getProperty(NVRAMInfo.Companion.getOPENVPNCL_IP());
                        if (!Strings.isNullOrEmpty(property)) {
                            final Set<String> mGlobalPreferencesStringSet = new HashSet<>(
                                    mGlobalPreferences.getStringSet(
                                            EDIT_OPEN_VPN_CLIENT_SETTINGS_LOCAL_IPS_AUTOCOMPLETE_PREF,
                                            new HashSet<String>()));
                            if (!mGlobalPreferencesStringSet.contains(property)) {
                                mGlobalPreferencesStringSet.add(property);
                                editor.putStringSet(EDIT_OPEN_VPN_CLIENT_SETTINGS_LOCAL_IPS_AUTOCOMPLETE_PREF,
                                        mGlobalPreferencesStringSet);
                                applyNewPrefs = true;
                            }
                        }

                        property = nvramInfo.getProperty(NVRAMInfo.Companion.getOPENVPNCL_MASK());
                        if (!Strings.isNullOrEmpty(property)) {
                            final Set<String> mGlobalPreferencesStringSet = new HashSet<>(
                                    mGlobalPreferences.getStringSet(
                                            EDIT_OPEN_VPN_CLIENT_SETTINGS_LOCAL_SUBNETS_AUTOCOMPLETE_PREF,
                                            new HashSet<String>()));
                            if (!mGlobalPreferencesStringSet.contains(property)) {
                                mGlobalPreferencesStringSet.add(property);
                                editor.putStringSet(EDIT_OPEN_VPN_CLIENT_SETTINGS_LOCAL_SUBNETS_AUTOCOMPLETE_PREF,
                                        mGlobalPreferencesStringSet);
                                applyNewPrefs = true;
                            }
                        }

                        property = nvramInfo.getProperty(NVRAMInfo.Companion.getOPENVPNCL_MTU());
                        if (!Strings.isNullOrEmpty(property)) {
                            final Set<String> mGlobalPreferencesStringSet = new HashSet<>(
                                    mGlobalPreferences.getStringSet(
                                            EDIT_OPEN_VPN_CLIENT_SETTINGS_MTU_SETTINGS_AUTOCOMPLETE_PREF,
                                            new HashSet<String>()));
                            if (!mGlobalPreferencesStringSet.contains(property)) {
                                mGlobalPreferencesStringSet.add(property);
                                editor.putStringSet(EDIT_OPEN_VPN_CLIENT_SETTINGS_MTU_SETTINGS_AUTOCOMPLETE_PREF,
                                        mGlobalPreferencesStringSet);
                                applyNewPrefs = true;
                            }
                        }

                        property = nvramInfo.getProperty(NVRAMInfo.Companion.getOPENVPNCL_FRAGMENT());
                        if (!Strings.isNullOrEmpty(property)) {
                            final Set<String> mGlobalPreferencesStringSet = new HashSet<>(
                                    mGlobalPreferences.getStringSet(
                                            EDIT_OPEN_VPN_CLIENT_SETTINGS_UDP_FRAGMENTS_AUTOCOMPLETE_PREF,
                                            new HashSet<String>()));
                            if (!mGlobalPreferencesStringSet.contains(property)) {
                                mGlobalPreferencesStringSet.add(property);
                                editor.putStringSet(EDIT_OPEN_VPN_CLIENT_SETTINGS_UDP_FRAGMENTS_AUTOCOMPLETE_PREF,
                                        mGlobalPreferencesStringSet);
                                applyNewPrefs = true;
                            }
                        }

                        if (applyNewPrefs) {
                            editor.apply();
                        }

                        updateProgressBarViewSeparator(55);

                        final String[] devDeviceLine =
                                SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                        "cat /tmp/openvpncl/openvpn.conf | grep \"dev \"");

                        String openvpnclIface = null;
                        if (devDeviceLine != null && devDeviceLine.length > 0) {
                            openvpnclIface = devDeviceLine[0].replace("dev ", "").trim();
                        }

                        updateProgressBarViewSeparator(75);

                        if (!Strings.isNullOrEmpty(openvpnclIface)) {
                            nvramInfo.setProperty(OPENVPNCL__DEV, openvpnclIface);
                            //noinspection ConstantConditions
                            final Map<WirelessIfaceTile.IfaceStatsType, Long> ifaceRxAndTxRates =
                                    getIfaceRxAndTxRates(openvpnclIface);

                            final Long rxBps = ifaceRxAndTxRates.get(RX_BYTES);
                            final Long txBps = ifaceRxAndTxRates.get(TX_BYTES);
                            if (rxBps != null) {
                                nvramInfo.setProperty(OPENVPNCL__DEV_RX_RATE, rxBps
                                        + " B ("
                                        + org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(rxBps)
                                        + ")");
                            }
                            if (txBps != null) {
                                nvramInfo.setProperty(OPENVPNCL__DEV_TX_RATE, txBps
                                        + " B ("
                                        + org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(txBps)
                                        + ")");
                            }

                            //Packet Info
                            final String sysClassNetStatsFolder =
                                    String.format(CAT_SYS_CLASS_NET_S_STATISTICS, openvpnclIface);
                            try {
                                final String[] packetsInfo =
                                        SSHUtils.getManualProperty(mParentFragmentActivity, mRouter,
                                                mGlobalPreferences,
                                                String.format("%s/rx_packets", sysClassNetStatsFolder),
                                                String.format("%s/rx_errors", sysClassNetStatsFolder),
                                                String.format("%s/tx_packets", sysClassNetStatsFolder),
                                                String.format("%s/tx_errors", sysClassNetStatsFolder));

                                if (packetsInfo != null) {
                                    final long rxErrors = Long.parseLong(packetsInfo[1]);
                                    nvramInfo.setProperty(OPENVPNCL__DEV_RX_PACKETS,
                                            String.format("%s (%s)", packetsInfo[0], rxErrors <= 0 ? "no error"
                                                    : (rxErrors + String
                                                            .format("error%s", rxErrors > 1 ? "s" : ""))));
                                    final long txErrors = Long.parseLong(packetsInfo[3]);
                                    nvramInfo.setProperty(OPENVPNCL__DEV_TX_PACKETS,
                                            String.format("%s (%s)", packetsInfo[2], txErrors <= 0 ? "no error"
                                                    : (txErrors + String
                                                            .format(" error%s", txErrors > 1 ? "s" : ""))));
                                }
                            } catch (final Exception e) {
                                e.printStackTrace();
                                //No worries
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

            @NonNull
            private Map<WirelessIfaceTile.IfaceStatsType, Long> getIfaceRxAndTxRates(
                    @NonNull final String phyIface) {
                final Map<WirelessIfaceTile.IfaceStatsType, Long> result =
                        Maps.newHashMapWithExpectedSize(2);
                final String sysClassNetStatsFolder =
                        String.format(CAT_SYS_CLASS_NET_S_STATISTICS, phyIface);
                final String rxBytesCmd = String.format("%s/rx_bytes", sysClassNetStatsFolder);
                final String txBytesCmd = String.format("%s/tx_bytes", sysClassNetStatsFolder);

                try {
                    final long[] bytesBeforeAndAfter = parseFloatDataFromOutput(
                            SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                    rxBytesCmd, txBytesCmd, "sleep 1", rxBytesCmd, txBytesCmd));
                    if (bytesBeforeAndAfter.length >= 4) {
                        result.put(RX_BYTES, Math.abs(bytesBeforeAndAfter[1] - bytesBeforeAndAfter[0]));
                        result.put(TX_BYTES, Math.abs(bytesBeforeAndAfter[3] - bytesBeforeAndAfter[2]));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
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
        };

        return mLoader;
    }

    @Nullable
    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Nullable
    @Override
    protected OnClickIntent getOnclickIntent() {

        if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
            //Not allowed
            Utils.displayUpgradeMessage(mParentFragmentActivity, "Edit OpenVPN Client Settings");
            return null;
        }

        if (mNvramInfo == null) {
            //Loading
            Utils.displayMessage(mParentFragmentActivity,
                    "Loading data from router - please wait a few seconds.", Style.ALERT);
            return null;
        }

        if (mNvramInfo.isEmpty()) {
            //No data!
            Utils.displayMessage(mParentFragmentActivity, "No data available - please retry later.",
                    Style.ALERT);
            return null;
        }

        final String mRouterUuid = mRouter.getUuid();
        final Intent editOpenVPNClSettingsIntent =
                new Intent(mParentFragment.getActivity(), EditOpenVPNClientSettingsActivity.class);
        editOpenVPNClSettingsIntent.putExtra(OPENVPNCL_NVRAMINFO, mNvramInfo);
        editOpenVPNClSettingsIntent.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouterUuid);

        return new OnClickIntent("Loading OpenVPN Client Settings...", editOpenVPNClSettingsIntent,
                this);
    }

    private void updateTileDisplayData(@NonNull final NVRAMInfo data,
            final boolean defaultValuesIfNotFound) {

        //State
        final String statusKey = \"fake-key\";
                defaultValuesIfNotFound ? RouterCompanionAppConstants.EMPTY_STRING : null);
        if (statusKey != null) {
            final String statusValue;
            switch (statusKey) {
                case "1":
                    statusValue = "Enabled";
                    break;
                case "0":
                    statusValue = "Disabled";
                    break;
                default:
                    statusValue = N_A;
                    break;
            }

            ((TextView) layout.findViewById(R.id.tile_services_openvpn_client_state)).setText(
                    statusValue);
        }

        //Server IP/Name
        String property = data.getProperty(
                NVRAMInfo.Companion.getOPENVPNCL_REMOTEIP(), defaultValuesIfNotFound ? N_A : null);
        if (property != null) {
            ((TextView) layout.findViewById(R.id.tile_services_openvpn_client_server_ip_name)).setText(
                    property);
        }

        //Port
        property = data
                .getProperty(NVRAMInfo.Companion.getOPENVPNCL_REMOTEPORT(), defaultValuesIfNotFound ? N_A : null);
        if (property != null) {
            ((TextView) layout.findViewById(R.id.tile_services_openvpn_client_server_port)).setText(
                    property);
        }

        //BW In
        property = data.getProperty(OPENVPNCL__DEV_RX_RATE, defaultValuesIfNotFound ? N_A : null);
        if (property != null) {
            ((TextView) layout.findViewById(R.id.tile_services_openvpn_client_rx_rate)).setText(property);
        }

        //BW Out
        property = data.getProperty(OPENVPNCL__DEV_TX_RATE, defaultValuesIfNotFound ? N_A : null);
        if (property != null) {
            ((TextView) layout.findViewById(R.id.tile_services_openvpn_client_tx_rate)).setText(property);
        }

        //RX Packets
        property = data.getProperty(OPENVPNCL__DEV_RX_PACKETS, defaultValuesIfNotFound ? N_A : null);
        if (property != null) {
            ((TextView) layout.findViewById(R.id.tile_services_openvpn_client_rx_packets)).setText(
                    property);
        }

        //TX Packets
        property = data.getProperty(OPENVPNCL__DEV_TX_PACKETS, defaultValuesIfNotFound ? N_A : null);
        if (property != null) {
            ((TextView) layout.findViewById(R.id.tile_services_openvpn_client_tx_packets)).setText(
                    property);
        }

        //Tunnel Device
        //noinspection ConstantConditions
        property = data.getProperty(NVRAMInfo.Companion.getOPENVPNCL_TUNTAP(), defaultValuesIfNotFound ? N_A : null);
        if (property != null) {
            ((TextView) layout.findViewById(R.id.tile_services_openvpn_client_tunnel_device)).setText(
                    property.toUpperCase());
        }

        //Tunnel Proto
        final String protoProp =
                data.getProperty(NVRAMInfo.Companion.getOPENVPNCL_PROTO(), defaultValuesIfNotFound ? N_A : null);
        if (protoProp != null) {
            final String tunnelProto;
            if ("tcp-client".equalsIgnoreCase(protoProp)) {
                tunnelProto = "TCP";
            } else {
                //noinspection ConstantConditions
                tunnelProto = protoProp.toUpperCase();
            }
            ((TextView) layout.findViewById(R.id.tile_services_openvpn_client_tunnel_protocol)).setText(
                    tunnelProto);
        }

        //Encryption Cipher
        final String encCipherProp =
                data.getProperty(NVRAMInfo.Companion.getOPENVPNCL_CIPHER(), defaultValuesIfNotFound ? N_A : null);
        if (encCipherProp != null) {
            final String encCipher;
            //noinspection ConstantConditions
            switch (encCipherProp.toLowerCase()) {
                case "aes-512-cbc":
                    encCipher = "AES-512 CBC";
                    break;
                case "aes-256-cbc":
                    encCipher = "AES-256 CBC";
                    break;
                case "aes-192-cbc":
                    encCipher = "AES-192 CBC";
                    break;
                case "aes-128-cbc":
                    encCipher = "AES-128 CBC";
                    break;
                case "bf-cbc":
                    encCipher = "Blowfish CBC";
                    break;
                default:
                    encCipher = encCipherProp;
                    break;
            }
            ((TextView) layout.findViewById(R.id.tile_services_openvpn_client_encryption_cipher)).setText(
                    encCipher);
        }

        //Hash Algo
        final String hashAlgoProp =
                data.getProperty(NVRAMInfo.Companion.getOPENVPNCL_AUTH(), defaultValuesIfNotFound ? N_A : null);
        if (hashAlgoProp != null) {
            final String hashAlgo;
            if (!"none".equalsIgnoreCase(hashAlgoProp)) {
                //noinspection ConstantConditions
                hashAlgo = hashAlgoProp.toUpperCase();
            } else {
                hashAlgo = "None";
            }
            ((TextView) layout.findViewById(R.id.tile_services_openvpn_client_hash_algorithm)).setText(
                    hashAlgo);
        }
    }
}
