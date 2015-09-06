package org.rm3l.ddwrt.tiles.services.vpn.server;

import android.content.Context;
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
import com.google.common.base.Strings;
import com.google.common.base.Throwables;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.actions.RouterAction;
import org.rm3l.ddwrt.actions.RouterActionListener;
import org.rm3l.ddwrt.actions.SetNVRAMVariablesAction;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import de.keyboardsurfer.android.widget.crouton.Style;

import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.PPTPD_ACCTPORT;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.PPTPD_AUTH;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.PPTPD_BCRELAY;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.PPTPD_ENABLE;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.PPTPD_FORCEMPPE;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.PPTPD_LIP;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.PPTPD_RADIUS;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.PPTPD_RADPASS;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.PPTPD_RADPORT;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.PPTPD_RADSERVER;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.PPTPD_RIP;

/**
 * Created by rm3l on 06/09/15.
 */
public class PPTPServerTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = PPTPServerTile.class.getSimpleName();

    public static final String N_A = "-";

    private NVRAMInfo mNvramInfo;
    private AtomicBoolean isToggleStateActionRunning = new AtomicBoolean(false);
    private AsyncTaskLoader<NVRAMInfo> mLoader;

    public PPTPServerTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_services_pptp_server,
                R.id.tile_services_pptp_server_togglebutton);
    }

    @Override
    public int getTileHeaderViewId() {
        return R.id.tile_services_pptp_server_hdr;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_services_pptp_server_title;
    }


    @Nullable
    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        mLoader = new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {

                try {
                    Log.d(LOG_TAG, "Init background loader for " + PPTPServerTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                        //Skip run
                        Log.d(LOG_TAG, "Skip loader run");
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    mNvramInfo = null;

                    final NVRAMInfo nvramInfo = new NVRAMInfo();

                    NVRAMInfo nvramInfoTmp = null;

                    try {
                        nvramInfoTmp =
                                SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity, mRouter,
                                        mGlobalPreferences,
                                        //Status: {1,0}
                                        PPTPD_ENABLE,
                                        //Broadcast Support {1,0}
                                        PPTPD_BCRELAY,
                                        //Force MPPE Enc {1,0}
                                        PPTPD_FORCEMPPE,
                                        //Server IP
                                        PPTPD_LIP,
                                        //Client IP(s)
                                        PPTPD_RIP,
                                        //CHAP-Secrets
                                        PPTPD_AUTH,
                                        //Radius {1,0}
                                        PPTPD_RADIUS,
                                        //Radius Server IP
                                        PPTPD_RADSERVER,
                                        //Radius Auth Port - default: 1812
                                        PPTPD_RADPORT,
                                        //Radius Accounting Port - default: 1813
                                        PPTPD_ACCTPORT,
                                        //Radius Shared Key
                                        PPTPD_RADPASS);
                    } finally {
                        if (nvramInfoTmp != null) {
                            nvramInfo.putAll(nvramInfoTmp);
                        }

                        boolean applyNewPrefs = false;
                        String property = nvramInfo.getProperty(PPTPD_LIP);
                        final SharedPreferences.Editor editor = mGlobalPreferences.edit();
                        if (!Strings.isNullOrEmpty(property)) {
                            final Set<String> mGlobalPreferencesStringSet = new HashSet<>(
                                    mGlobalPreferences.getStringSet("EditPPTPServerSettingsServerIp",
                                    new HashSet<String>()));
                            if (!mGlobalPreferencesStringSet.contains(property)) {
                                mGlobalPreferencesStringSet.add(property);
                                editor
                                        .putStringSet("EditPPTPServerSettingsServerIp", mGlobalPreferencesStringSet);
                                applyNewPrefs = true;
                            }
                        }

                        property = nvramInfo.getProperty(PPTPD_RIP);
                        if (!Strings.isNullOrEmpty(property)) {
                            final Set<String> mGlobalPreferencesStringSet = new HashSet<>(
                                    mGlobalPreferences.getStringSet("EditPPTPServerSettingsClientIps",
                                    new HashSet<String>()));
                            if (!mGlobalPreferencesStringSet.contains(property)) {
                                mGlobalPreferencesStringSet.add(property);
                                editor
                                        .putStringSet("EditPPTPServerSettingsClientIps", mGlobalPreferencesStringSet);
                                applyNewPrefs = true;
                            }
                        }

                        property = nvramInfo.getProperty(PPTPD_RADSERVER);
                        if (!Strings.isNullOrEmpty(property)) {
                            final Set<String> mGlobalPreferencesStringSet = new HashSet<>(
                                    mGlobalPreferences.getStringSet("EditPPTPServerSettingsRadiusServerIp",
                                    new HashSet<String>()));
                            if (!mGlobalPreferencesStringSet.contains(property)) {
                                mGlobalPreferencesStringSet.add(property);
                                editor
                                        .putStringSet("EditPPTPServerSettingsRadiusServerIp", mGlobalPreferencesStringSet);
                                applyNewPrefs = true;
                            }
                        }

                        property = nvramInfo.getProperty(PPTPD_RADPORT);
                        if (!Strings.isNullOrEmpty(property)) {
                            final Set<String> mGlobalPreferencesStringSet = new HashSet<>(
                                    mGlobalPreferences.getStringSet("EditPPTPServerSettingsRadiusServerAuthPort",
                                    new HashSet<String>()));
                            if (!mGlobalPreferencesStringSet.contains(property)) {
                                mGlobalPreferencesStringSet.add(property);
                                editor
                                        .putStringSet("EditPPTPServerSettingsRadiusServerAuthPort", mGlobalPreferencesStringSet);
                                applyNewPrefs = true;
                            }
                        }

                        property = nvramInfo.getProperty(PPTPD_ACCTPORT);
                        if (!Strings.isNullOrEmpty(property)) {
                            final Set<String> mGlobalPreferencesStringSet = new HashSet<>(
                                    mGlobalPreferences.getStringSet("EditPPTPServerSettingsRadiusServerAcctPort",
                                    new HashSet<String>()));
                            if (!mGlobalPreferencesStringSet.contains(property)) {
                                mGlobalPreferencesStringSet.add(property);
                                editor
                                        .putStringSet("EditPPTPServerSettingsRadiusServerAcctPort", mGlobalPreferencesStringSet);
                                applyNewPrefs = true;
                            }
                        }

                        if (applyNewPrefs) {
                            editor.apply();
                        }

                    }

                    if (nvramInfo.isEmpty()) {
                        throw new DDWRTNoDataException("No Data!");
                    }

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
        Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

        layout.findViewById(R.id.tile_services_pptp_server_header_loading_view)
                .setVisibility(View.GONE);
        layout.findViewById(R.id.tile_services_pptp_server_loading_view)
                .setVisibility(View.GONE);
        layout.findViewById(R.id.tile_services_pptp_server_grid_layout)
                .setVisibility(View.VISIBLE);
        //FIXME Disabled for now
//        layout.findViewById(R.id.tile_services_pptp_server_note)
//                .setVisibility(View.VISIBLE);

        Exception preliminaryCheckException = null;
        if (data == null) {
            //noinspection ThrowableInstanceNeverThrown
            preliminaryCheckException = new DDWRTNoDataException("No Data!");
        } else //noinspection ThrowableResultOfMethodCallIgnored
            if (data.getException() == null) {
                final String pptpdServerEnabled = data.getProperty(PPTPD_ENABLE);
                if (pptpdServerEnabled == null || !Arrays.asList("0", "1").contains(pptpdServerEnabled)) {
                    //noinspection ThrowableInstanceNeverThrown
                    preliminaryCheckException = new DDWRTPPTPdClienStateUnknown("Unknown state");
                }
            }

        final SwitchCompat enableTraffDataButton =
                (SwitchCompat) this.layout.findViewById(R.id.tile_services_pptp_server_status);
        enableTraffDataButton.setVisibility(View.VISIBLE);

        final boolean makeToogleEnabled = (data != null &&
                data.getData() != null &&
                data.getData().containsKey(PPTPD_ENABLE));

        if (!isToggleStateActionRunning.get()) {
            if (makeToogleEnabled) {
                if ("1".equals(data.getProperty(PPTPD_ENABLE))) {
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

        final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_services_pptp_server_error);

        final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

            mNvramInfo = new NVRAMInfo();
            mNvramInfo.putAll(data);

            if (exception == null) {
                errorPlaceHolderView.setVisibility(View.GONE);
            }

            updateTileDisplayData(data, true);
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
                R.id.tile_services_pptp_server_togglebutton_title, R.id.tile_services_pptp_server_togglebutton_separator);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");
    }

    private void updateTileDisplayData(@NonNull final NVRAMInfo data, final boolean defaultValuesIfNotFound) {

        //State
        String statusKey = \"fake-key\";
                defaultValuesIfNotFound ? DDWRTCompanionConstants.EMPTY_STRING : null);
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
                defaultValuesIfNotFound ? DDWRTCompanionConstants.EMPTY_STRING : null);
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

            ((TextView) layout.findViewById(R.id.tile_services_pptp_server_broadcast_support)).setText(statusValue);
        }

        //Force MPPE Encryption
        statusKey = \"fake-key\";
                defaultValuesIfNotFound ? DDWRTCompanionConstants.EMPTY_STRING : null);
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

            ((TextView) layout.findViewById(R.id.tile_services_pptp_server_force_mppe_encryption)).setText(statusValue);
        }

        //Server IP
        String property = data.getProperty(PPTPD_LIP, defaultValuesIfNotFound ? N_A : null);
        if (property != null) {
            ((TextView) layout.findViewById(R.id.tile_services_pptp_server_server_ip))
                    .setText(property);
        }

        //Client IP(s)
        property = data.getProperty(PPTPD_RIP, defaultValuesIfNotFound ? N_A : null);
        if (property != null) {
            ((TextView) layout.findViewById(R.id.tile_services_pptp_server_client_ips))
                    .setText(property);
        }

        //Radius
        statusKey = \"fake-key\";
                defaultValuesIfNotFound ? DDWRTCompanionConstants.EMPTY_STRING : null);
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

        final int[] radiusInformation = new int[] {
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
            property = data.getProperty(PPTPD_RADSERVER, defaultValuesIfNotFound ? N_A : null);
            if (property != null) {
                ((TextView) layout.findViewById(R.id.tile_services_pptp_server_radius_server_ip))
                        .setText(property);
            }

            //Radius Auth Port
            property = data.getProperty(PPTPD_RADPORT, defaultValuesIfNotFound ? N_A : null);
            if (property != null) {
                ((TextView) layout.findViewById(R.id.tile_services_pptp_server_radius_auth_port))
                        .setText(property);
            }

            //Radius Accounting Port
            property = data.getProperty(PPTPD_ACCTPORT, defaultValuesIfNotFound ? N_A : null);
            if (property != null) {
                ((TextView) layout.findViewById(R.id.tile_services_pptp_server_radius_accounting_port))
                        .setText(property);
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

    private class DDWRTPPTPdClienStateUnknown extends DDWRTNoDataException {

        public DDWRTPPTPdClienStateUnknown(@Nullable String detailMessage) {
            super(detailMessage);
        }
    }

    private class ManagePPTPServerToggle implements  View.OnClickListener {

        private boolean enable;

        @Override
        public void onClick(View view) {

            isToggleStateActionRunning.set(true);

            if (!(view instanceof CompoundButton)) {
                Utils.reportException(new IllegalStateException("ManagePPTPServerToggle#onClick: " +
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

            if (BuildConfig.WITH_ADS) {
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

            nvramInfoToSet.setProperty(PPTPD_ENABLE, enable ? "1" : "0");

            new UndoBarController.UndoBar(mParentFragmentActivity)
                    .message(String.format("PPTP Server will be %s on '%s' (%s). ",
                            enable ? "enabled" : "disabled",
                            mRouter.getDisplayName(),
                            mRouter.getRemoteIpAddress()))
                    .listener(new UndoBarController.AdvancedUndoListener() {
                                  @Override
                                  public void onHide(@Nullable Parcelable parcelable) {

                                      Utils.displayMessage(mParentFragmentActivity,
                                              String.format("%s PPTP Server...",
                                                      enable ? "Enabling" : "Disabling"),
                                              Style.INFO);

                                      new SetNVRAMVariablesAction(mParentFragmentActivity,
                                              nvramInfoToSet,
                                              true,
                                              new RouterActionListener() {
                                                  @Override
                                                  public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull final Router router, Object returnData) {
                                                      mParentFragmentActivity.runOnUiThread(new Runnable() {
                                                          @Override
                                                          public void run() {

                                                              try {
                                                                  compoundButton.setChecked(enable);
                                                                  Utils.displayMessage(mParentFragmentActivity,
                                                                          String.format("PPTP Server %s successfully on host '%s' (%s). ",
                                                                                  enable ? "enabled" : "disabled",
                                                                                  router.getDisplayName(),
                                                                                  router.getRemoteIpAddress()),
                                                                          Style.CONFIRM);
                                                              } finally {
                                                                  compoundButton.setEnabled(true);
                                                                  isToggleStateActionRunning.set(false);
                                                                  if (mLoader != null) {
                                                                      //Reload everything right away
                                                                      doneWithLoaderInstance(PPTPServerTile.this,
                                                                              mLoader,
                                                                              1l,
                                                                              R.id.tile_services_pptp_server_togglebutton_title,
                                                                              R.id.tile_services_pptp_server_togglebutton_separator);
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
                                                                          String.format("Error while trying to %s PPTP Server on '%s' (%s): %s",
                                                                                  enable ? "enable" : "disable",
                                                                                  router.getDisplayName(),
                                                                                  router.getRemoteIpAddress(),
                                                                                  ExceptionUtils.getRootCauseMessage(exception)),
                                                                          Style.ALERT);
                                                              } finally {
                                                                  compoundButton.setEnabled(true);
                                                                  isToggleStateActionRunning.set(false);
                                                              }
                                                          }
                                                      });


                                                  }
                                              }

                                              ,
                                              mGlobalPreferences).

                                              execute(mRouter);

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
