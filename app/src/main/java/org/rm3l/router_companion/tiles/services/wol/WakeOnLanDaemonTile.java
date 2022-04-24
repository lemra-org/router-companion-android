package org.rm3l.router_companion.tiles.services.wol;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.router_companion.RouterCompanionAppConstants.EMPTY_STRING;
import static org.rm3l.router_companion.RouterCompanionAppConstants.EMPTY_VALUE_TO_DISPLAY;
import static org.rm3l.router_companion.tiles.services.wol.EditWOLDaemonSettingsActivity.WOL_DAEMON_HOSTNAMES_PREF_KEY;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
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
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.snackbar.SnackbarCallback;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style;

/** Created by rm3l on 10/04/15. */
public class WakeOnLanDaemonTile extends DDWRTTile<NVRAMInfo>
    implements DDWRTTile.ActivityResultListener, SnackbarCallback, RouterActionListener {

  private class DDWRTWoLDaemonStateUnknown extends DDWRTNoDataException {

    public DDWRTWoLDaemonStateUnknown(@Nullable String detailMessage) {
      super(detailMessage);
    }
  }

  private class ManageWoLDaemonToggle implements View.OnClickListener {

    private boolean enable;

    @Override
    public void onClick(View view) {

      isToggleStateActionRunning.set(true);

      if (!(view instanceof CompoundButton)) {
        Utils.reportException(
            null,
            new IllegalStateException(
                "ManageWoLDaemonToggle#onClick: " + "view is NOT an instance of CompoundButton!"));
        isToggleStateActionRunning.set(false);
        return;
      }

      final CompoundButton compoundButton = (CompoundButton) view;

      mParentFragmentActivity.runOnUiThread(
          new Runnable() {
            @Override
            public void run() {
              compoundButton.setEnabled(false);
            }
          });

      this.enable = compoundButton.isChecked();

      if (BuildConfig.DONATIONS) {
        Utils.displayUpgradeMessage(mParentFragmentActivity, "Toggle Wake on LAN Daemon");
        isToggleStateActionRunning.set(false);
        mParentFragmentActivity.runOnUiThread(
            new Runnable() {
              @Override
              public void run() {
                compoundButton.setChecked(!enable);
                compoundButton.setEnabled(true);
              }
            });
        return;
      }

      final NVRAMInfo nvramInfoToSet = new NVRAMInfo();

      nvramInfoToSet.setProperty(NVRAMInfo.Companion.getWOL_ENABLE(), enable ? "1" : "0");

      SnackbarUtils.buildSnackbar(
          mParentFragmentActivity,
          String.format(
              "Wake on LAN Daemon will be %s on '%s' (%s). ",
              enable ? "enabled" : "disabled",
              mRouter.getDisplayName(),
              mRouter.getRemoteIpAddress()),
          "CANCEL",
          Snackbar.LENGTH_LONG,
          new SnackbarCallback() {
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
            public void onDismissEventManual(int event, @Nullable Bundle bundle) throws Exception {
              cancel();
            }

            @Override
            public void onDismissEventSwipe(int event, @Nullable Bundle bundle) throws Exception {
              cancel();
            }

            @Override
            public void onDismissEventTimeout(int event, @Nullable Bundle bundle) throws Exception {
              Utils.displayMessage(
                  mParentFragmentActivity,
                  String.format("%s Wake on LAN Daemon...", enable ? "Enabling" : "Disabling"),
                  Style.INFO);

              ActionManager.runTasks(
                  new SetNVRAMVariablesAction(
                      mRouter,
                      mParentFragmentActivity,
                      nvramInfoToSet,
                      true,
                      new RouterActionListener() {
                        @Override
                        public void onRouterActionFailure(
                            @NonNull RouterAction routerAction,
                            @NonNull final Router router,
                            @Nullable final Exception exception) {
                          mParentFragmentActivity.runOnUiThread(
                              new Runnable() {
                                @Override
                                public void run() {
                                  try {
                                    compoundButton.setChecked(!enable);
                                    Utils.displayMessage(
                                        mParentFragmentActivity,
                                        String.format(
                                            "Error while trying to %s Wake on LAN Daemon on '%s' (%s): %s",
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

                        @Override
                        public void onRouterActionSuccess(
                            @NonNull RouterAction routerAction,
                            @NonNull final Router router,
                            Object returnData) {
                          mParentFragmentActivity.runOnUiThread(
                              new Runnable() {
                                @Override
                                public void run() {

                                  try {
                                    compoundButton.setChecked(enable);
                                    Utils.displayMessage(
                                        mParentFragmentActivity,
                                        String.format(
                                            "Wake on LAN Daemon %s successfully on host '%s' (%s). ",
                                            enable ? "enabled" : "disabled",
                                            router.getDisplayName(),
                                            router.getRemoteIpAddress()),
                                        Style.CONFIRM);
                                  } finally {
                                    compoundButton.setEnabled(true);
                                    isToggleStateActionRunning.set(false);
                                    if (mLoader != null) {
                                      // Reload everything right away
                                      doneWithLoaderInstance(WakeOnLanDaemonTile.this, mLoader, 1l);
                                    }
                                  }
                                }
                              });
                        }
                      },
                      mGlobalPreferences));
            }

            private void cancel() {
              mParentFragmentActivity.runOnUiThread(
                  new Runnable() {
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
          },
          new Bundle(),
          true);

      // new UndoBarController.UndoBar(mParentFragmentActivity).message(
      //    String.format("Wake on LAN Daemon will be %s on '%s' (%s). ",
      //        enable ? "enabled" : "disabled", mRouter.getDisplayName(),
      //        mRouter.getRemoteIpAddress())).listener(new UndoBarController.AdvancedUndoListener()
      // {
      //                                                  @Override public void onHide(@Nullable
      // Parcelable parcelable) {
      //
      //                                                  }
      //
      //                                                  @Override public void onClear(@NonNull
      // Parcelable[] parcelables) {
      //
      //                                                  }
      //
      //                                                  @Override public void onUndo(@Nullable
      // Parcelable parcelable) {
      //
      // mParentFragmentActivity.runOnUiThread(new Runnable() {
      //                                                      @Override public void run() {
      //                                                        try {
      //
      // compoundButton.setChecked(!enable);
      //                                                          compoundButton.setEnabled(true);
      //                                                        } finally {
      //
      // isToggleStateActionRunning.set(false);
      //                                                        }
      //                                                      }
      //                                                    });
      //                                                  }
      //                                                }
      //
      // ).
      //
      //    token(new Bundle()
      //
      //    ).
      //
      //    show();
    }
  }

  public static final String WOL_DAEMON_NVRAMINFO = "WOL_DAEMON_NVRAMINFO";

  private static final String LOG_TAG = WakeOnLanDaemonTile.class.getSimpleName();

  private AtomicBoolean isToggleStateActionRunning = new AtomicBoolean(false);

  private long mLastSync;

  private AsyncTaskLoader<NVRAMInfo> mLoader;

  private NVRAMInfo mNvramInfo;

  private final SharedPreferences mSharedPreferences;

  public WakeOnLanDaemonTile(
      @NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
    super(parentFragment, arguments, router, R.layout.tile_services_wol_daemon, null);
    mSharedPreferences =
        mParentFragmentActivity.getSharedPreferences(
            DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
  }

  @Override
  public Integer getTileHeaderViewId() {
    return R.id.tile_services_wol_daemon_hdr;
  }

  @Override
  public Integer getTileTitleViewId() {
    return R.id.tile_services_wol_daemon_title;
  }

  @Override
  public void onDismissEventTimeout(int event, @Nullable Bundle token) throws Exception {
    final String routerAction =
        token != null ? token.getString(DDWRTMainActivity.ROUTER_ACTION) : null;
    FirebaseCrashlytics.getInstance().log("routerAction: [" + routerAction + "]");
    if (isNullOrEmpty(routerAction)) {
      return;
    }
    try {
      switch (RouterAction.valueOf(routerAction)) {
        case SET_NVRAM_VARIABLES:
          ActionManager.runTasks(
              new SetNVRAMVariablesAction(
                  mRouter,
                  mParentFragmentActivity,
                  (NVRAMInfo) token.getSerializable(WOL_DAEMON_NVRAMINFO),
                  true,
                  // Reboot Router at the end of the operation
                  this,
                  mGlobalPreferences));
          break;
        default:
          // Ignored
          break;
      }
    } catch (IllegalArgumentException | NullPointerException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onLoadFinished(Loader<NVRAMInfo> loader, NVRAMInfo data) {
    try {
      FirebaseCrashlytics.getInstance().log("onLoadFinished: loader=" + loader + " / data=" + data);

      layout
          .findViewById(R.id.tile_services_wol_daemon_header_loading_view)
          .setVisibility(View.GONE);
      layout.findViewById(R.id.tile_services_wol_daemon_loading_view).setVisibility(View.GONE);
      //        final View wolDaemonStatus =
      // layout.findViewById(R.id.tile_services_wol_daemon_status);
      //        wolDaemonStatus
      //                .setVisibility(View.VISIBLE);
      layout.findViewById(R.id.tile_services_wol_daemon_grid_layout).setVisibility(View.VISIBLE);
      layout.findViewById(R.id.tile_services_wol_daemon_note).setVisibility(View.VISIBLE);

      Exception preliminaryCheckException = null;
      if (data == null) {
        //noinspection ThrowableInstanceNeverThrown
        preliminaryCheckException = new DDWRTNoDataException("No Data!");
      } else //noinspection ThrowableResultOfMethodCallIgnored
      if (data.getException() == null) {
        final String wolDaemonEnabled = data.getProperty(NVRAMInfo.Companion.getWOL_ENABLE());
        if (wolDaemonEnabled == null || !Arrays.asList("0", "1").contains(wolDaemonEnabled)) {
          //noinspection ThrowableInstanceNeverThrown
          preliminaryCheckException = new DDWRTWoLDaemonStateUnknown("Unknown state");
        }
      }

      final SwitchCompat enableTraffDataButton =
          (SwitchCompat) this.layout.findViewById(R.id.tile_services_wol_daemon_status);
      enableTraffDataButton.setVisibility(View.VISIBLE);

      final boolean makeToogleEnabled =
          (data != null
              && data.getData() != null
              && data.getData().containsKey(NVRAMInfo.Companion.getWOL_ENABLE()));

      if (!isToggleStateActionRunning.get()) {
        if (makeToogleEnabled) {
          if ("1".equals(data.getProperty(NVRAMInfo.Companion.getWOL_ENABLE()))) {
            // Enabled
            enableTraffDataButton.setChecked(true);
          } else {
            // Disabled
            enableTraffDataButton.setChecked(false);
          }
          enableTraffDataButton.setEnabled(true);
        } else {
          enableTraffDataButton.setChecked(false);
          enableTraffDataButton.setEnabled(false);
        }

        enableTraffDataButton.setOnClickListener(new ManageWoLDaemonToggle());
      }

      if (preliminaryCheckException != null) {
        data = new NVRAMInfo().setException(preliminaryCheckException);
      }

      final TextView errorPlaceHolderView =
          (TextView) this.layout.findViewById(R.id.tile_services_wol_daemon_error);

      final Exception exception = data.getException();

      if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

        mNvramInfo = new NVRAMInfo();
        mNvramInfo.putAll(data);

        if (exception == null) {
          errorPlaceHolderView.setVisibility(View.GONE);
        }

        updateTileDisplayData(data, true);

        // Update last sync
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

  @Override
  public void onResultCode(int resultCode, Intent data) {
    switch (resultCode) {
      case Activity.RESULT_OK:
        final NVRAMInfo newNvramInfoData =
            (NVRAMInfo) data.getSerializableExtra(WOL_DAEMON_NVRAMINFO);
        if (newNvramInfoData == null || newNvramInfoData.isEmpty()) {
          Utils.displayMessage(mParentFragmentActivity, "No change", Style.INFO);
          break;
        }

        final Bundle token = new Bundle();
        token.putString(DDWRTMainActivity.ROUTER_ACTION, RouterAction.SET_NVRAM_VARIABLES.name());
        token.putSerializable(WOL_DAEMON_NVRAMINFO, newNvramInfoData);

        SnackbarUtils.buildSnackbar(
            mParentFragmentActivity,
            "WOL Daemon Settings will be updated on the Router.",
            "CANCEL",
            Snackbar.LENGTH_LONG,
            this,
            token,
            true);

        // new UndoBarController.UndoBar(mParentFragmentActivity).message(
        //    "WOL Daemon Settings will be updated on the Router.")
        //    .listener(this)
        //    .token(token)
        //    .show();
        break;
      default:
        // Ignored
        break;
    }
  }

  @Override
  public void onRouterActionFailure(
      @NonNull RouterAction routerAction, @NonNull Router router, @Nullable Exception exception) {
    Utils.displayMessage(
        mParentFragmentActivity,
        String.format("Error: %s", Utils.handleException(exception).first),
        Style.ALERT);
  }

  @Override
  public void onRouterActionSuccess(
      @NonNull RouterAction routerAction, @NonNull Router router, final Object returnData) {
    Utils.displayMessage(mParentFragmentActivity, "Success", Style.CONFIRM);
    // Update info right away
    if (returnData instanceof NVRAMInfo) {
      // Run on main thread to avoid the exception:
      // "Only the original thread that created a view hierarchy can touch its views."
      mParentFragmentActivity.runOnUiThread(
          new Runnable() {
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
    mLoader =
        new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

          @Nullable
          @Override
          public NVRAMInfo loadInBackground() {

            try {
              FirebaseCrashlytics.getInstance()
                  .log(
                      "Init background loader for "
                          + WakeOnLanDaemonTile.class
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
                nvramInfoTmp =
                    SSHUtils.getNVRamInfoFromRouter(
                        mParentFragmentActivity,
                        mRouter,
                        mGlobalPreferences,
                        NVRAMInfo.Companion.getWOL_ENABLE(),
                        NVRAMInfo.Companion.getWOL_INTERVAL(),
                        NVRAMInfo.Companion.getWOL_HOSTNAME(),
                        NVRAMInfo.Companion.getWOL_PASSWD(),
                        NVRAMInfo.Companion.getWOL_MACS());
                updateProgressBarViewSeparator(45);
              } finally {
                if (nvramInfoTmp != null) {
                  nvramInfo.putAll(nvramInfoTmp);
                }

                boolean applyNewPrefs = false;
                String property = nvramInfo.getProperty(NVRAMInfo.Companion.getWOL_HOSTNAME());
                final SharedPreferences.Editor editor = mSharedPreferences.edit();
                if (!Strings.isNullOrEmpty(property)) {
                  final Set<String> mSharedPreferencesStringSet =
                      new HashSet<>(
                          mSharedPreferences.getStringSet(
                              NVRAMInfo.Companion.getWOL_HOSTNAME(), new HashSet<String>()));
                  if (!mSharedPreferencesStringSet.contains(property)) {
                    mSharedPreferencesStringSet.add(property);
                    editor.putStringSet(WOL_DAEMON_HOSTNAMES_PREF_KEY, mSharedPreferencesStringSet);
                    applyNewPrefs = true;
                  }
                }

                if (applyNewPrefs) {
                  editor.apply();
                }
              }

              updateProgressBarViewSeparator(85);

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

    if (BuildConfig.DONATIONS) {
      Utils.displayUpgradeMessage(mParentFragmentActivity, "Edit WOL Settings");
      return null;
    }

    if (mNvramInfo == null) {
      // Loading
      Utils.displayMessage(
          mParentFragmentActivity,
          "Loading data from router - please wait a few seconds.",
          Style.ALERT);
      return null;
    }

    if (mNvramInfo.isEmpty()) {
      // No data!
      Utils.displayMessage(
          mParentFragmentActivity, "No data available - please retry later.", Style.ALERT);
      return null;
    }

    final String mRouterUuid = mRouter.getUuid();
    final Intent editWOLDaemonSettingsIntent =
        new Intent(mParentFragment.getActivity(), EditWOLDaemonSettingsActivity.class);
    editWOLDaemonSettingsIntent.putExtra(WOL_DAEMON_NVRAMINFO, mNvramInfo);
    editWOLDaemonSettingsIntent.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouterUuid);

    return new OnClickIntent("Loading WOL Daemon Settings...", editWOLDaemonSettingsIntent, this);
  }

  private void updateTileDisplayData(
      @NonNull final NVRAMInfo data, final boolean defaultValuesIfNotFound) {

    // State
    final String statusKey = \"fake-key\";
        data.getProperty(
            NVRAMInfo.Companion.getWOL_ENABLE(), defaultValuesIfNotFound ? EMPTY_STRING : null);
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
          statusValue = EMPTY_VALUE_TO_DISPLAY;
          break;
      }
      //            ((TextView)
      // layout.findViewById(R.id.tile_services_wol_daemon_status)).setText(statusValue);

      ((TextView) layout.findViewById(R.id.tile_services_wol_daemon_state)).setText(statusValue);
    }

    // Interval
    String property =
        data.getProperty(
            NVRAMInfo.Companion.getWOL_INTERVAL(),
            defaultValuesIfNotFound ? EMPTY_VALUE_TO_DISPLAY : null);
    if (property != null) {
      ((TextView) layout.findViewById(R.id.tile_services_wol_daemon_interval)).setText(property);
    }

    // Hostname
    property =
        data.getProperty(
            NVRAMInfo.Companion.getWOL_HOSTNAME(),
            defaultValuesIfNotFound ? EMPTY_VALUE_TO_DISPLAY : null);
    if (property != null) {
      ((TextView) layout.findViewById(R.id.tile_services_wol_daemon_hostname)).setText(property);
    }

    // MAC Addresses
    property =
        data.getProperty(
            NVRAMInfo.Companion.getWOL_MACS(),
            defaultValuesIfNotFound ? EMPTY_VALUE_TO_DISPLAY : null);
    if (property != null) {
      ((TextView) layout.findViewById(R.id.tile_services_wol_daemon_mac_addresses))
          .setText(property.replaceAll(" ", "\n"));
    }
  }
}
