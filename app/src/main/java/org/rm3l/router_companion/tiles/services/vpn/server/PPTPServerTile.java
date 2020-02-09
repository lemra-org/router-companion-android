package org.rm3l.router_companion.tiles.services.vpn.server;

import android.content.Context;
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
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import java.util.Arrays;
import java.util.HashSet;
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
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.snackbar.SnackbarCallback;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style;

/**
 * Created by rm3l on 06/09/15.
 */
public class PPTPServerTile extends DDWRTTile<NVRAMInfo> {

    private class DDWRTPPTPdClienStateUnknown extends DDWRTNoDataException {

        public DDWRTPPTPdClienStateUnknown(@Nullable String detailMessage) {
            super(detailMessage);
        }
    }

    private class ManagePPTPServerToggle implements View.OnClickListener {

        private boolean enable;

        @Override
        public void onClick(View view) {

            isToggleStateActionRunning.set(true);

            if (!(view instanceof CompoundButton)) {
                Utils.reportException(null, new IllegalStateException(
                        "ManagePPTPServerToggle#onClick: " + "view is NOT an instance of CompoundButton!"));
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
                Utils.displayUpgradeMessage(mParentFragmentActivity, "Toggle PPTP Server");
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

            nvramInfoToSet.setProperty(NVRAMInfo.Companion.getPPTPD_ENABLE(), enable ? "1" : "0");

            SnackbarUtils.buildSnackbar(mParentFragmentActivity,
                    String.format("PPTP Server will be %s on '%s' (%s). ", enable ? "enabled" : "disabled",
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
                            Utils.displayMessage(mParentFragmentActivity,
                                    String.format("%s PPTP Server...", enable ? "Enabling" : "Disabling"),
                                    Style.INFO);

                            ActionManager.runTasks(
                                    new SetNVRAMVariablesAction(mRouter, mParentFragmentActivity, nvramInfoToSet,
                                            true, new RouterActionListener() {
                                        @Override
                                        public void onRouterActionFailure(@NonNull RouterAction routerAction,
                                                @NonNull final Router router, @Nullable final Exception exception) {
                                            mParentFragmentActivity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        compoundButton.setChecked(!enable);
                                                        Utils.displayMessage(mParentFragmentActivity, String.format(
                                                                "Error while trying to %s PPTP Server on '%s' (%s): %s",
                                                                enable ? "enable" : "disable",
                                                                router.getDisplayName(),
                                                                router.getRemoteIpAddress(),
                                                                Utils.handleException(exception).first), Style.ALERT);
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
                                                                        "PPTP Server %s successfully on host '%s' (%s). ",
                                                                        enable ? "enabled" : "disabled",
                                                                        router.getDisplayName(),
                                                                        router.getRemoteIpAddress()), Style.CONFIRM);
                                                    } finally {
                                                        compoundButton.setEnabled(true);
                                                        isToggleStateActionRunning.set(false);
                                                        if (mLoader != null) {
                                                            //Reload everything right away
                                                            doneWithLoaderInstance(PPTPServerTile.this, mLoader, 1l);
                                                        }
                                                    }
                                                }
                                            });
                                        }
                                    }

                                            , mGlobalPreferences));
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
            //    String.format("PPTP Server will be %s on '%s' (%s). ", enable ? "enabled" : "disabled",
            //        mRouter.getDisplayName(), mRouter.getRemoteIpAddress()))
            //    .listener(new UndoBarController.AdvancedUndoListener() {
            //                @Override public void onHide(@Nullable Parcelable parcelable) {
            //                }
            //
            //                @Override public void onClear(@NonNull Parcelable[] parcelables) {
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

    public static final String N_A = "-";

    private static final String LOG_TAG = PPTPServerTile.class.getSimpleName();

    private AtomicBoolean isToggleStateActionRunning = new AtomicBoolean(false);

    private long mLastSync;

    private AsyncTaskLoader<NVRAMInfo> mLoader;

    private NVRAMInfo mNvramInfo;

    public PPTPServerTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments,
            @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_services_pptp_server, null);
    }

    @Override
    public Integer getTileHeaderViewId() {
        return R.id.tile_services_pptp_server_hdr;
    }

    @Override
    public Integer getTileTitleViewId() {
        return R.id.tile_services_pptp_server_title;
    }

    @Override
    public void onLoadFinished(Loader<NVRAMInfo> loader, NVRAMInfo data) {
        try {
            FirebaseCrashlytics.getInstance().log( "onLoadFinished: loader=" + loader + " / data=" + data);

            layout.findViewById(R.id.tile_services_pptp_server_header_loading_view)
                    .setVisibility(View.GONE);
            layout.findViewById(R.id.tile_services_pptp_server_loading_view).setVisibility(View.GONE);
            layout.findViewById(R.id.tile_services_pptp_server_grid_layout).setVisibility(View.VISIBLE);
            //FIXME Disabled for now
            //        layout.findViewById(R.id.tile_services_pptp_server_note)
            //                .setVisibility(View.VISIBLE);

            Exception preliminaryCheckException = null;
            if (data == null) {
                //noinspection ThrowableInstanceNeverThrown
                preliminaryCheckException = new DDWRTNoDataException("No Data!");
            } else //noinspection ThrowableResultOfMethodCallIgnored
                if (data.getException() == null) {
                    final String pptpdServerEnabled = data.getProperty(NVRAMInfo.Companion.getPPTPD_ENABLE());
                    if (pptpdServerEnabled == null || !Arrays.asList("0", "1").contains(pptpdServerEnabled)) {
                        //noinspection ThrowableInstanceNeverThrown
                        preliminaryCheckException = new DDWRTPPTPdClienStateUnknown("Unknown state");
                    }
                }

            final SwitchCompat enableTraffDataButton =
                    (SwitchCompat) this.layout.findViewById(R.id.tile_services_pptp_server_status);
            enableTraffDataButton.setVisibility(View.VISIBLE);

            final boolean makeToogleEnabled =
                    (data != null && data.getData() != null && data.getData().containsKey(
                            NVRAMInfo.Companion.getPPTPD_ENABLE()));

            if (!isToggleStateActionRunning.get()) {
                if (makeToogleEnabled) {
                    if ("1".equals(data.getProperty(NVRAMInfo.Companion.getPPTPD_ENABLE()))) {
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

                enableTraffDataButton.setOnClickListener(new ManagePPTPServerToggle());
            }

            if (preliminaryCheckException != null) {
                data = new NVRAMInfo().setException(preliminaryCheckException);
            }

            final TextView errorPlaceHolderView =
                    (TextView) this.layout.findViewById(R.id.tile_services_pptp_server_error);

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

            FirebaseCrashlytics.getInstance().log( "onLoadFinished(): done loading!");
        } finally {
            mRefreshing.set(false);
            doneWithLoaderInstance(this, loader);
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
                    FirebaseCrashlytics.getInstance().log( "Init background loader for "
                            + PPTPServerTile.class
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
                                NVRAMInfo.Companion.getPPTPD_ENABLE(),
                                //Broadcast Support {1,0}
                                NVRAMInfo.Companion.getPPTPD_BCRELAY(),
                                //Force MPPE Enc {1,0}
                                NVRAMInfo.Companion.getPPTPD_FORCEMPPE(),
                                //Server IP
                                NVRAMInfo.Companion.getPPTPD_LIP(),
                                //Client IP(s)
                                NVRAMInfo.Companion.getPPTPD_RIP(),
                                //CHAP-Secrets
                                NVRAMInfo.Companion.getPPTPD_AUTH(),
                                //Radius {1,0}
                                NVRAMInfo.Companion.getPPTPD_RADIUS(),
                                //Radius Server IP
                                NVRAMInfo.Companion.getPPTPD_RADSERVER(),
                                //Radius Auth Port - default: 1812
                                NVRAMInfo.Companion.getPPTPD_RADPORT(),
                                //Radius Accounting Port - default: 1813
                                NVRAMInfo.Companion.getPPTPD_ACCTPORT(),
                                //Radius Shared Key
                                NVRAMInfo.Companion.getPPTPD_RADPASS());

                        updateProgressBarViewSeparator(55);
                    } finally {
                        if (nvramInfoTmp != null) {
                            nvramInfo.putAll(nvramInfoTmp);
                        }

                        boolean applyNewPrefs = false;
                        String property = nvramInfo.getProperty(NVRAMInfo.Companion.getPPTPD_LIP());
                        final SharedPreferences.Editor editor = mGlobalPreferences.edit();
                        if (!Strings.isNullOrEmpty(property)) {
                            final Set<String> mGlobalPreferencesStringSet = new HashSet<>(
                                    mGlobalPreferences.getStringSet("EditPPTPServerSettingsServerIp",
                                            new HashSet<String>()));
                            if (!mGlobalPreferencesStringSet.contains(property)) {
                                mGlobalPreferencesStringSet.add(property);
                                editor.putStringSet("EditPPTPServerSettingsServerIp", mGlobalPreferencesStringSet);
                                applyNewPrefs = true;
                            }
                        }

                        property = nvramInfo.getProperty(NVRAMInfo.Companion.getPPTPD_RIP());
                        if (!Strings.isNullOrEmpty(property)) {
                            final Set<String> mGlobalPreferencesStringSet = new HashSet<>(
                                    mGlobalPreferences.getStringSet("EditPPTPServerSettingsClientIps",
                                            new HashSet<String>()));
                            if (!mGlobalPreferencesStringSet.contains(property)) {
                                mGlobalPreferencesStringSet.add(property);
                                editor.putStringSet("EditPPTPServerSettingsClientIps", mGlobalPreferencesStringSet);
                                applyNewPrefs = true;
                            }
                        }

                        property = nvramInfo.getProperty(NVRAMInfo.Companion.getPPTPD_RADSERVER());
                        if (!Strings.isNullOrEmpty(property)) {
                            final Set<String> mGlobalPreferencesStringSet = new HashSet<>(
                                    mGlobalPreferences.getStringSet("EditPPTPServerSettingsRadiusServerIp",
                                            new HashSet<String>()));
                            if (!mGlobalPreferencesStringSet.contains(property)) {
                                mGlobalPreferencesStringSet.add(property);
                                editor.putStringSet("EditPPTPServerSettingsRadiusServerIp",
                                        mGlobalPreferencesStringSet);
                                applyNewPrefs = true;
                            }
                        }

                        property = nvramInfo.getProperty(NVRAMInfo.Companion.getPPTPD_RADPORT());
                        if (!Strings.isNullOrEmpty(property)) {
                            final Set<String> mGlobalPreferencesStringSet = new HashSet<>(
                                    mGlobalPreferences.getStringSet("EditPPTPServerSettingsRadiusServerAuthPort",
                                            new HashSet<String>()));
                            if (!mGlobalPreferencesStringSet.contains(property)) {
                                mGlobalPreferencesStringSet.add(property);
                                editor.putStringSet("EditPPTPServerSettingsRadiusServerAuthPort",
                                        mGlobalPreferencesStringSet);
                                applyNewPrefs = true;
                            }
                        }

                        property = nvramInfo.getProperty(NVRAMInfo.Companion.getPPTPD_ACCTPORT());
                        if (!Strings.isNullOrEmpty(property)) {
                            final Set<String> mGlobalPreferencesStringSet = new HashSet<>(
                                    mGlobalPreferences.getStringSet("EditPPTPServerSettingsRadiusServerAcctPort",
                                            new HashSet<String>()));
                            if (!mGlobalPreferencesStringSet.contains(property)) {
                                mGlobalPreferencesStringSet.add(property);
                                editor.putStringSet("EditPPTPServerSettingsRadiusServerAcctPort",
                                        mGlobalPreferencesStringSet);
                                applyNewPrefs = true;
                            }
                        }

                        if (applyNewPrefs) {
                            editor.apply();
                        }
                    }

                    updateProgressBarViewSeparator(70);
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
        return null;
    }

    private void updateTileDisplayData(@NonNull final NVRAMInfo data,
            final boolean defaultValuesIfNotFound) {

        //State
        String statusKey = \"fake-key\";
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

            ((TextView) layout.findViewById(R.id.tile_services_pptp_server_state)).setText(statusValue);
        }

        //Broadcast Support
        statusKey = \"fake-key\";
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

            ((TextView) layout.findViewById(R.id.tile_services_pptp_server_broadcast_support)).setText(
                    statusValue);
        }

        //Force MPPE Encryption
        statusKey = \"fake-key\";
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

            ((TextView) layout.findViewById(
                    R.id.tile_services_pptp_server_force_mppe_encryption)).setText(statusValue);
        }

        //Server IP
        String property = data.getProperty(NVRAMInfo.Companion.getPPTPD_LIP(), defaultValuesIfNotFound ? N_A : null);
        if (property != null) {
            ((TextView) layout.findViewById(R.id.tile_services_pptp_server_server_ip)).setText(property);
        }

        //Client IP(s)
        property = data.getProperty(NVRAMInfo.Companion.getPPTPD_RIP(), defaultValuesIfNotFound ? N_A : null);
        if (property != null) {
            ((TextView) layout.findViewById(R.id.tile_services_pptp_server_client_ips)).setText(property);
        }

        //Radius
        statusKey = \"fake-key\";
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

            ((TextView) layout.findViewById(R.id.tile_services_pptp_server_radius)).setText(statusValue);
        }

        final int[] radiusInformation = new int[]{
                R.id.tile_services_pptp_server_radius_server_ip_title,
                R.id.tile_services_pptp_server_radius_server_ip_sep,
                R.id.tile_services_pptp_server_radius_server_ip,
                R.id.tile_services_pptp_server_radius_auth_port_title,
                R.id.tile_services_pptp_server_radius_auth_port_sep,
                R.id.tile_services_pptp_server_radius_auth_port,
                R.id.tile_services_pptp_server_radius_accounting_port_title,
                R.id.tile_services_pptp_server_radius_accounting_port_sep,
                R.id.tile_services_pptp_server_radius_accounting_port
        };

        if ("1".equals(statusKey)) {
            //Radius Server IP
            property = data
                    .getProperty(NVRAMInfo.Companion.getPPTPD_RADSERVER(), defaultValuesIfNotFound ? N_A : null);
            if (property != null) {
                ((TextView) layout.findViewById(R.id.tile_services_pptp_server_radius_server_ip)).setText(
                        property);
            }

            //Radius Auth Port
            property = data.getProperty(NVRAMInfo.Companion.getPPTPD_RADPORT(), defaultValuesIfNotFound ? N_A : null);
            if (property != null) {
                ((TextView) layout.findViewById(R.id.tile_services_pptp_server_radius_auth_port)).setText(
                        property);
            }

            //Radius Accounting Port
            property = data
                    .getProperty(NVRAMInfo.Companion.getPPTPD_ACCTPORT(), defaultValuesIfNotFound ? N_A : null);
            if (property != null) {
                ((TextView) layout.findViewById(
                        R.id.tile_services_pptp_server_radius_accounting_port)).setText(property);
            }

            //Display Radius Info
            for (final int radiusInfo : radiusInformation) {
                final View viewById = layout.findViewById(radiusInfo);
                if (viewById == null) {
                    continue;
                }
                viewById.setVisibility(View.VISIBLE);
            }
        } else {
            //Hide any other Radius information
            for (final int radiusInfo : radiusInformation) {
                final View viewById = layout.findViewById(radiusInfo);
                if (viewById == null) {
                    continue;
                }
                viewById.setVisibility(View.GONE);
            }
        }
    }
}
