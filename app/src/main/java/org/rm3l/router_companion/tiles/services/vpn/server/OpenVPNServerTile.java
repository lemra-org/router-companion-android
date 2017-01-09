package org.rm3l.router_companion.tiles.services.vpn.server;

import android.content.Context;
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
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import de.keyboardsurfer.android.widget.crouton.Style;

import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPN_CA;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPN_CLIENT;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPN_CONFIG;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPN_CRL;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPN_DH;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPN_ENABLE;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPN_KEY;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPN_ONWAN;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.OPENVPN_TLSAUTH;

/**
 * Created by rm3l on 05/09/15.
 */
public class OpenVPNServerTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = OpenVPNServerTile.class.getSimpleName();

    public static final String N_A = "-";

    private NVRAMInfo mNvramInfo;
    private AtomicBoolean isToggleStateActionRunning = new AtomicBoolean(false);
    private AsyncTaskLoader<NVRAMInfo> mLoader;
    private long mLastSync;

    public OpenVPNServerTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_services_openvpn_server,null);
    }

    @Override
    public int getTileHeaderViewId() {
        return R.id.tile_services_openvpn_server_hdr;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_services_openvpn_server_title;
    }


    @Nullable
    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        mLoader = new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {

                try {
                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Init background loader for " + OpenVPNServerTile.class + ": routerInfo=" +
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
                                        OPENVPN_ENABLE,
                                        //Start Type: {1=Wan Up, 0=System}
                                        OPENVPN_ONWAN,
                                        //Public Server Cert
                                        OPENVPN_CA,
                                        //Certificate Revoke List
                                        OPENVPN_CRL,
                                        //Public Client Cert
                                        OPENVPN_CLIENT,
                                        //Private Client Key
                                        OPENVPN_KEY,
                                        //DH PEM
                                        OPENVPN_DH,
                                        //OpenVPN Config
                                        OPENVPN_CONFIG,
                                        //OpenVPN TLS Auth
                                        OPENVPN_TLSAUTH);
                    } finally {
                        if (nvramInfoTmp != null) {
                            nvramInfo.putAll(nvramInfoTmp);
                        }
                    }

                    updateProgressBarViewSeparator(90);

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
        try {
            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

            layout.findViewById(R.id.tile_services_openvpn_server_header_loading_view)
                    .setVisibility(View.GONE);
            layout.findViewById(R.id.tile_services_openvpn_server_loading_view)
                    .setVisibility(View.GONE);
            layout.findViewById(R.id.tile_services_openvpn_server_grid_layout)
                    .setVisibility(View.VISIBLE);
            //FIXME Disabled for now
//        layout.findViewById(R.id.tile_services_openvpn_server_note)
//                .setVisibility(View.VISIBLE);

            Exception preliminaryCheckException = null;
            if (data == null) {
                //noinspection ThrowableInstanceNeverThrown
                preliminaryCheckException = new DDWRTNoDataException("No Data!");
            } else //noinspection ThrowableResultOfMethodCallIgnored
                if (data.getException() == null) {
                    final String pptpdServerEnabled = data.getProperty(OPENVPN_ENABLE);
                    if (pptpdServerEnabled == null || !Arrays.asList("0", "1").contains(pptpdServerEnabled)) {
                        //noinspection ThrowableInstanceNeverThrown
                        preliminaryCheckException = new DDWRTOpenVPNdClienStateUnknown("Unknown state");
                    }
                }

            final SwitchCompat enableTraffDataButton =
                    (SwitchCompat) this.layout.findViewById(R.id.tile_services_openvpn_server_status);
            enableTraffDataButton.setVisibility(View.VISIBLE);

            final boolean makeToogleEnabled = (data != null &&
                    data.getData() != null &&
                    data.getData().containsKey(OPENVPN_ENABLE));

            if (!isToggleStateActionRunning.get()) {
                if (makeToogleEnabled) {
                    if ("1".equals(data.getProperty(OPENVPN_ENABLE))) {
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

                enableTraffDataButton.setOnClickListener(new ManageOpenVPNServerToggle());
            }

            if (preliminaryCheckException != null) {
                data = new NVRAMInfo().setException(preliminaryCheckException);
            }

            final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_services_openvpn_server_error);

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

            ((TextView) layout.findViewById(R.id.tile_services_openvpn_server_state)).setText(statusValue);
        }

        //Start Type
        statusKey = \"fake-key\";
                defaultValuesIfNotFound ? RouterCompanionAppConstants.EMPTY_STRING : null);
        if (statusKey != null) {
            final String statusValue;
            switch (statusKey) {
                case "1":
                    statusValue = "Wan Up";
                    break;
                case "0":
                    statusValue = "System";
                    break;
                default:
                    statusValue = N_A;
                    break;
            }

            ((TextView) layout.findViewById(R.id.tile_services_openvpn_server_start_type)).setText(statusValue);
        }

    }

    private class DDWRTOpenVPNdClienStateUnknown extends DDWRTNoDataException {

        public DDWRTOpenVPNdClienStateUnknown(@Nullable String detailMessage) {
            super(detailMessage);
        }
    }

    private class ManageOpenVPNServerToggle implements View.OnClickListener {

        private boolean enable;

        @Override
        public void onClick(View view) {

            isToggleStateActionRunning.set(true);

            if (!(view instanceof CompoundButton)) {
                Utils.reportException(null, new IllegalStateException("ManageOpenVPNServerToggle#onClick: " +
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
                Utils.displayUpgradeMessage(mParentFragmentActivity, "Toggle OpenVPN Server");
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

            nvramInfoToSet.setProperty(OPENVPN_ENABLE, enable ? "1" : "0");

            new UndoBarController.UndoBar(mParentFragmentActivity)
                    .message(String.format("OpenVPN Server will be %s on '%s' (%s). ",
                            enable ? "enabled" : "disabled",
                            mRouter.getDisplayName(),
                            mRouter.getRemoteIpAddress()))
                    .listener(new UndoBarController.AdvancedUndoListener() {
                                  @Override
                                  public void onHide(@Nullable Parcelable parcelable) {

                                      Utils.displayMessage(mParentFragmentActivity,
                                              String.format("%s OpenVPN Server...",
                                                      enable ? "Enabling" : "Disabling"),
                                              Style.INFO);

                                      ActionManager.runTasks(
                                      new SetNVRAMVariablesAction(
                                              mRouter,
                                              mParentFragmentActivity,
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
                                                                          String.format("OpenVPN Server %s successfully on host '%s' (%s). ",
                                                                                  enable ? "enabled" : "disabled",
                                                                                  router.getDisplayName(),
                                                                                  router.getRemoteIpAddress()),
                                                                          Style.CONFIRM);
                                                              } finally {
                                                                  compoundButton.setEnabled(true);
                                                                  isToggleStateActionRunning.set(false);
                                                                  if (mLoader != null) {
                                                                      //Reload everything right away
                                                                      doneWithLoaderInstance(OpenVPNServerTile.this,
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
                                                                          String.format("Error while trying to %s OpenVPN Server on '%s' (%s): %s",
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
                                              }

                                              ,
                                              mGlobalPreferences)
                                      );

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