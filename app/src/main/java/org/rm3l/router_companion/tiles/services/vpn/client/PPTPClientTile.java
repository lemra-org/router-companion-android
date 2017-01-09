package org.rm3l.router_companion.tiles.services.vpn.client;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.cocosw.undobar.UndoBarController;
import com.crashlytics.android.Crashlytics;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;

import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.actions.ActionManager;
import org.rm3l.router_companion.actions.RouterAction;
import org.rm3l.router_companion.actions.RouterActionListener;
import org.rm3l.router_companion.actions.SetNVRAMVariablesAction;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.utils.ReportingUtils;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import de.keyboardsurfer.android.widget.crouton.Style;

import static org.rm3l.router_companion.resources.conn.NVRAMInfo.PPTPD_CLIENT_ENABLE;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.PPTPD_CLIENT_NAT;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.PPTPD_CLIENT_SRVIP;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.PPTPD_CLIENT_SRVMRU;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.PPTPD_CLIENT_SRVMTU;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.PPTPD_CLIENT_SRVPASS;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.PPTPD_CLIENT_SRVSEC;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.PPTPD_CLIENT_SRVSUB;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.PPTPD_CLIENT_SRVSUBMSK;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.PPTPD_CLIENT_SRVUSER;
import static org.rm3l.router_companion.RouterCompanionAppConstants.VPN_PPTP_TOGGLES_MUTUALLY_EXCLUSIVE;

/**
 * Created by rm3l on 06/09/15.
 */
public class PPTPClientTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = PPTPClientTile.class.getSimpleName();

    public static final String N_A = "-";
    
    private NVRAMInfo mNvramInfo;
    private AtomicBoolean isToggleStateActionRunning = new AtomicBoolean(false);
    private AsyncTaskLoader<NVRAMInfo> mLoader;
    private long mLastSync;

    public PPTPClientTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_services_pptp_client,null);
    }

    @Override
    public int getTileHeaderViewId() {
        return R.id.tile_services_pptp_client_hdr;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_services_pptp_client_title;
    }


    @Nullable
    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        mLoader = new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {

                try {
                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Init background loader for " + PPTPClientTile.class + ": routerInfo=" +
                            mRouter + " / nbRunsLoader=" + nbRunsLoader);

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
                        nvramInfoTmp =
                                SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity, mRouter,
                                        mGlobalPreferences,
                                        //Status: {1,0}
                                        PPTPD_CLIENT_ENABLE,
                                        //Server IP or DNS Name
                                        PPTPD_CLIENT_SRVIP,
                                        //Server Subnet
                                        PPTPD_CLIENT_SRVSUB,
                                        //Server Subnet Mask
                                        PPTPD_CLIENT_SRVSUBMSK,
                                        //MPPE Encruption - default: mppe required
                                        PPTPD_CLIENT_SRVSEC,
                                        //MTU - default: 1450 (0 <= mtu <= 1500)
                                        PPTPD_CLIENT_SRVMTU,
                                        //MRU - default: 1450 (0 <= mru <= 1500)
                                        PPTPD_CLIENT_SRVMRU,
                                        //NAT: {1,0}
                                        PPTPD_CLIENT_NAT,
                                        //Username-  default: DOMAIN\\Username
                                        PPTPD_CLIENT_SRVUSER,
                                        //Password
                                        PPTPD_CLIENT_SRVPASS);
                        updateProgressBarViewSeparator(45);
                    } finally {
                        if (nvramInfoTmp != null) {
                            nvramInfo.putAll(nvramInfoTmp);
                        }

                        boolean applyNewPrefs = false;
                        String property = nvramInfo.getProperty(PPTPD_CLIENT_SRVIP);
                        final SharedPreferences.Editor editor = mGlobalPreferences.edit();
                        if (!Strings.isNullOrEmpty(property)) {
                            final Set<String> mGlobalPreferencesStringSet = new HashSet<>(
                                    mGlobalPreferences.getStringSet("EditPPTPClientSettingsRemoteServer",
                                    new HashSet<String>()));
                            if (!mGlobalPreferencesStringSet.contains(property)) {
                                mGlobalPreferencesStringSet.add(property);
                                editor
                                        .putStringSet("EditPPTPClientSettingsRemoteServer", mGlobalPreferencesStringSet);
                                applyNewPrefs = true;
                            }
                        }

                        property = nvramInfo.getProperty(PPTPD_CLIENT_SRVSUB);
                        if (!Strings.isNullOrEmpty(property)) {
                            final Set<String> mGlobalPreferencesStringSet = new HashSet<>(
                                    mGlobalPreferences.getStringSet("EditPPTPClientSettingsRemoteServerSubnet",
                                    new HashSet<String>()));
                            if (!mGlobalPreferencesStringSet.contains(property)) {
                                mGlobalPreferencesStringSet.add(property);
                                editor
                                        .putStringSet("EditPPTPClientSettingsRemoteServerSubnet", mGlobalPreferencesStringSet);
                                applyNewPrefs = true;
                            }
                        }

                        property = nvramInfo.getProperty(PPTPD_CLIENT_SRVSUBMSK);
                        if (!Strings.isNullOrEmpty(property)) {
                            final Set<String> mGlobalPreferencesStringSet = new HashSet<>(
                                    mGlobalPreferences.getStringSet("EditPPTPClientSettingsRemoteServerSubnetMask",
                                    new HashSet<String>()));
                            if (!mGlobalPreferencesStringSet.contains(property)) {
                                mGlobalPreferencesStringSet.add(property);
                                editor
                                        .putStringSet("EditPPTPClientSettingsRemoteServerSubnetMask", mGlobalPreferencesStringSet);
                                applyNewPrefs = true;
                            }
                        }

                        property = nvramInfo.getProperty(PPTPD_CLIENT_SRVMTU);
                        if (!Strings.isNullOrEmpty(property)) {
                            final Set<String> mGlobalPreferencesStringSet = new HashSet<>(
                                    mGlobalPreferences.getStringSet("EditPPTPClientSettingsRemoteServerMTU",
                                    new HashSet<String>()));
                            if (!mGlobalPreferencesStringSet.contains(property)) {
                                mGlobalPreferencesStringSet.add(property);
                                editor
                                        .putStringSet("EditPPTPClientSettingsRemoteServerMTU", mGlobalPreferencesStringSet);
                                applyNewPrefs = true;
                            }
                        }

                        property = nvramInfo.getProperty(PPTPD_CLIENT_SRVMRU);
                        if (!Strings.isNullOrEmpty(property)) {
                            final Set<String> mGlobalPreferencesStringSet = new HashSet<>(
                                    mGlobalPreferences.getStringSet("EditPPTPClientSettingsRemoteServerMRU",
                                    new HashSet<String>()));
                            if (!mGlobalPreferencesStringSet.contains(property)) {
                                mGlobalPreferencesStringSet.add(property);
                                editor
                                        .putStringSet("EditPPTPClientSettingsRemoteServerMRU", mGlobalPreferencesStringSet);
                                applyNewPrefs = true;
                            }
                        }

                        property = nvramInfo.getProperty(PPTPD_CLIENT_SRVUSER);
                        if (!Strings.isNullOrEmpty(property)) {
                            final Set<String> mGlobalPreferencesStringSet = new HashSet<>(
                                    mGlobalPreferences.getStringSet("EditPPTPClientSettingsRemoteServerUsername",
                                    new HashSet<String>()));
                            if (!mGlobalPreferencesStringSet.contains(property)) {
                                mGlobalPreferencesStringSet.add(property);
                                editor
                                        .putStringSet("EditPPTPClientSettingsRemoteServerUsername", mGlobalPreferencesStringSet);
                                applyNewPrefs = true;
                            }
                        }

                        if (applyNewPrefs) {
                            editor.apply();
                        }

                    }

                    updateProgressBarViewSeparator(65);
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

    @Override
    public void onLoadFinished(Loader<NVRAMInfo> loader, NVRAMInfo data) {
        try {
            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

            layout.findViewById(R.id.tile_services_pptp_client_header_loading_view)
                    .setVisibility(View.GONE);
            layout.findViewById(R.id.tile_services_pptp_client_loading_view)
                    .setVisibility(View.GONE);
            layout.findViewById(R.id.tile_services_pptp_client_grid_layout)
                    .setVisibility(View.VISIBLE);
            //FIXME Disabled for now
//        layout.findViewById(R.id.tile_services_pptp_client_note)
//                .setVisibility(View.VISIBLE);

            Exception preliminaryCheckException = null;
            if (data == null) {
                //noinspection ThrowableInstanceNeverThrown
                preliminaryCheckException = new DDWRTNoDataException("No Data!");
            } else //noinspection ThrowableResultOfMethodCallIgnored
                if (data.getException() == null) {
                    final String pptpdClientEnabled = data.getProperty(NVRAMInfo.PPTPD_CLIENT_ENABLE);
                    if (pptpdClientEnabled == null || !Arrays.asList("0", "1").contains(pptpdClientEnabled)) {
                        //noinspection ThrowableInstanceNeverThrown
                        preliminaryCheckException = new DDWRTPPTPdClienStateUnknown("Unknown state");
                    }
                }

            final SwitchCompat enableTraffDataButton =
                    (SwitchCompat) this.layout.findViewById(R.id.tile_services_pptp_client_status);
            enableTraffDataButton.setVisibility(View.VISIBLE);

            final boolean makeToogleEnabled = (data != null &&
                    data.getData() != null &&
                    data.getData().containsKey(NVRAMInfo.PPTPD_CLIENT_ENABLE));

            if (!isToggleStateActionRunning.get()) {
                if (makeToogleEnabled) {
                    if ("1".equals(data.getProperty(NVRAMInfo.PPTPD_CLIENT_ENABLE))) {
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

                enableTraffDataButton.setOnClickListener(new ManagePPTPClientToggle());
            }

            if (preliminaryCheckException != null) {
                data = new NVRAMInfo().setException(preliminaryCheckException);
            }

            final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_services_pptp_client_error);

            final Exception exception = data.getException();

            if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

                mNvramInfo = new NVRAMInfo();
                mNvramInfo.putAll(data);

                if (exception == null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }

                updateTileDisplayData(data, true);

                //Update last sync
                final RelativeTimeTextView lastSyncView = (RelativeTimeTextView) layout.findViewById(R.id.tile_last_sync);
                lastSyncView.setReferenceTime(mLastSync);
                lastSyncView.setPrefix("Last sync: ");
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


            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished(): done loading!");
        } finally {
            mRefreshing.set(false);
            doneWithLoaderInstance(this, loader);
        }
    }

    private void updateTileDisplayData(@NonNull final NVRAMInfo data, final boolean defaultValuesIfNotFound) {

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

            ((TextView) layout.findViewById(R.id.tile_services_pptp_client_state)).setText(statusValue);
        }

        //Server IP / DNS Name
        String property = data.getProperty(PPTPD_CLIENT_SRVIP, defaultValuesIfNotFound ? N_A : null);
        if (property != null) {
            ((TextView) layout.findViewById(R.id.tile_services_pptp_client_server_ip_name))
                    .setText(property);
        }

        //Subnet
        property = data.getProperty(PPTPD_CLIENT_SRVSUB, defaultValuesIfNotFound ? N_A : null);
        if (property != null) {
            ((TextView) layout.findViewById(R.id.tile_services_pptp_client_remote_subnet))
                    .setText(property);
        }

        //Subnet Mask
        property = data.getProperty(PPTPD_CLIENT_SRVSUBMSK, defaultValuesIfNotFound ? N_A : null);
        if (property != null) {
            ((TextView) layout.findViewById(R.id.tile_services_pptp_client_remote_subnet_mask))
                    .setText(property);
        }

        //MPPE Encyption
        property = data.getProperty(PPTPD_CLIENT_SRVSEC, defaultValuesIfNotFound ? N_A : null);
        if (property != null) {
            ((TextView) layout.findViewById(R.id.tile_services_pptp_client_mppe_encryption))
                    .setText(property);
        }

        //MTU
        property = data.getProperty(PPTPD_CLIENT_SRVMTU, defaultValuesIfNotFound ? N_A : null);
        if (property != null) {
            ((TextView) layout.findViewById(R.id.tile_services_pptp_client_mtu))
                    .setText(property);
        }

        //MRU
        property = data.getProperty(PPTPD_CLIENT_SRVMRU, defaultValuesIfNotFound ? N_A : null);
        if (property != null) {
            ((TextView) layout.findViewById(R.id.tile_services_pptp_client_mru))
                    .setText(property);
        }

        //Username
        property = data.getProperty(PPTPD_CLIENT_SRVUSER, defaultValuesIfNotFound ? N_A : null);
        if (property != null) {
            ((TextView) layout.findViewById(R.id.tile_services_pptp_client_username))
                    .setText(property.toUpperCase());
        }

        //NAT
        final String natStatusKey = \"fake-key\";
                defaultValuesIfNotFound ? RouterCompanionAppConstants.EMPTY_STRING : null);
        if (natStatusKey != null) {
            final String statusValue;
            switch (natStatusKey) {
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

            ((TextView) layout.findViewById(R.id.tile_services_pptp_client_nat)).setText(statusValue);
        }
    }

    private class DDWRTPPTPdClienStateUnknown extends DDWRTNoDataException {

        public DDWRTPPTPdClienStateUnknown(@Nullable String detailMessage) {
            super(detailMessage);
        }
    }

    private class ManagePPTPClientToggle implements  View.OnClickListener {

        private boolean enable;

        @Override
        public void onClick(View view) {

            isToggleStateActionRunning.set(true);

            if (!(view instanceof CompoundButton)) {
                ReportingUtils.reportException(null, new IllegalStateException("ManagePPTPClientToggle#onClick: " +
                        "view is NOT an instance of CompoundButton!"));
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
                Utils.displayUpgradeMessage(mParentFragmentActivity, "Toggle PPTP Client");
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

            nvramInfoToSet.setProperty(NVRAMInfo.PPTPD_CLIENT_ENABLE, enable ? "1" : "0");

            new UndoBarController.UndoBar(mParentFragmentActivity)
                    .message(String.format("PPTP Client will be %s on '%s' (%s). ",
                            enable ? "enabled" : "disabled",
                            mRouter.getDisplayName(),
                            mRouter.getRemoteIpAddress()))
                    .listener(new UndoBarController.AdvancedUndoListener() {
                                  @Override
                                  public void onHide(@Nullable Parcelable parcelable) {

                                      final RouterActionListener listener = new RouterActionListener() {
                                          @Override
                                          public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull final Router router, Object returnData) {
                                              mParentFragmentActivity.runOnUiThread(new Runnable() {
                                                  @Override
                                                  public void run() {

                                                      try {
                                                          compoundButton.setChecked(enable);
                                                          Utils.displayMessage(mParentFragmentActivity,
                                                                  String.format("PPTP Client %s successfully on host '%s' (%s). ",
                                                                          enable ? "enabled" : "disabled",
                                                                          router.getDisplayName(),
                                                                          router.getRemoteIpAddress()),
                                                                  Style.CONFIRM);
                                                      } finally {
                                                          compoundButton.setEnabled(true);
                                                          isToggleStateActionRunning.set(false);
                                                          if (mLoader != null) {
                                                              //Reload everything right away
                                                              doneWithLoaderInstance(PPTPClientTile.this,
                                                                      mLoader,
                                                                      1l);
                                                          }
                                                      }
                                                  }

                                              });
                                          }

                                          @Override
                                          public void onRouterActionFailure(@NonNull RouterAction
                                                                                    routerAction, @NonNull final Router
                                                                                    router, @Nullable final Exception exception) {
                                              mParentFragmentActivity.runOnUiThread(new Runnable() {
                                                  @Override
                                                  public void run() {
                                                      try {
                                                          compoundButton.setChecked(!enable);
                                                          Utils.displayMessage(mParentFragmentActivity,
                                                                  String.format("Error while trying to %s PPTP Client on '%s' (%s): %s",
                                                                          enable ? "enable" : "disable",
                                                                          router.getDisplayName(),
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
                                      };

                                      final boolean openvpnClStatusToSet = !enable;

                                      if (mParentFragmentPreferences != null
                                              && mParentFragmentPreferences.getBoolean(VPN_PPTP_TOGGLES_MUTUALLY_EXCLUSIVE, false)) {

                                          new AlertDialog.Builder(mParentFragmentActivity)
                                                  .setIcon(R.drawable.ic_action_alert_warning)
                                                  .setTitle("Toggle OpenVPN Client status")
                                                  .setMessage(String.format(Locale.US, "Router will be rebooted. Do you wish to %s OpenVPN Client at the same time?",
                                                          openvpnClStatusToSet ? "start" : "stop"))
                                                  .setCancelable(true)
                                                  .setPositiveButton("Yes!", new DialogInterface.OnClickListener() {
                                                              @Override
                                                              public void onClick(DialogInterface dialog, int which) {
                                                                  nvramInfoToSet.setProperty(NVRAMInfo.OPENVPNCL_ENABLE,
                                                                          openvpnClStatusToSet ? "1" : "0");
                                                                  Utils.displayMessage(mParentFragmentActivity,
                                                                          String.format("%s PPTP Client (and %s OpenVPN Client) ...",
                                                                                  enable ? "Enabling" : "Disabling",
                                                                                  openvpnClStatusToSet ? "Enabling" : "Disabling"),
                                                                          Style.INFO);

                                                                  ActionManager.runTasks(
                                                                          new SetNVRAMVariablesAction(
                                                                                  mRouter,
                                                                                  mParentFragmentActivity,
                                                                                  nvramInfoToSet,
                                                                                  true,
                                                                                  listener,
                                                                                  mGlobalPreferences)
                                                                  );
                                                              }
                                                          }
                                                  ).setNegativeButton("No", new DialogInterface.OnClickListener() {
                                              @Override
                                              public void onClick(DialogInterface dialogInterface, int i) {

                                                  Utils.displayMessage(mParentFragmentActivity,
                                                          String.format("%s PPTP Client...",
                                                                  enable ? "Enabling" : "Disabling"),
                                                          Style.INFO);

                                                  ActionManager.runTasks(
                                                          new SetNVRAMVariablesAction(
                                                                  mRouter,
                                                                  mParentFragmentActivity,
                                                                  nvramInfoToSet,
                                                                  true,
                                                                  listener,
                                                                  mGlobalPreferences)
                                                  );
                                              }
                                          }).create().show();
                                      } else {
                                          Utils.displayMessage(mParentFragmentActivity,
                                                  String.format("%s PPTP Client...",
                                                          enable ? "Enabling" : "Disabling"),
                                                  Style.INFO);

                                          ActionManager.runTasks(
                                                  new SetNVRAMVariablesAction(
                                                          mRouter,
                                                          mParentFragmentActivity,
                                                          nvramInfoToSet,
                                                          true,
                                                          listener,
                                                          mGlobalPreferences)
                                          );
                                      }

//                                      Utils.displayMessage(mParentFragmentActivity,
//                                              String.format("%s PPTP Client...",
//                                                      enable ? "Enabling" : "Disabling"),
//                                              Style.INFO);


//                                      new SetNVRAMVariablesAction(mParentFragmentActivity,
//                                              nvramInfoToSet,
//                                              true,
//                                              listener,
//                                              mGlobalPreferences).execute(mRouter);

                                  }

                                  @Override
                                  public void onClear(@NonNull Parcelable[] parcelables) {
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

                                  @Override
                                  public void onUndo(@Nullable Parcelable parcelable) {
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
                              }

                    )
                    .

                            token(new Bundle()

                            )
                    .

                            show();
        }
    }
}
