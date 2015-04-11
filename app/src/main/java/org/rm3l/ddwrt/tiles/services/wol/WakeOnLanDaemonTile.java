package org.rm3l.ddwrt.tiles.services.wol;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
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
import org.rm3l.ddwrt.main.DDWRTMainActivity;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.util.HashSet;
import java.util.Set;

import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.WOL_ENABLE;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.WOL_HOSTNAME;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.WOL_INTERVAL;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.WOL_MACS;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.WOL_PASSWD;
import static org.rm3l.ddwrt.tiles.services.wol.EditWOLDaemonSettingsActivity.WOL_DAEMON_HOSTNAMES_PREF_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.EMPTY_STRING;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.EMPTY_VALUE_TO_DISPLAY;

/**
 * Created by rm3l on 10/04/15.
 */
public class WakeOnLanDaemonTile extends DDWRTTile<NVRAMInfo>
        implements DDWRTTile.ActivityResultListener, UndoBarController.AdvancedUndoListener, RouterActionListener {

    private static final String LOG_TAG = WakeOnLanDaemonTile.class.getSimpleName();
    public static final String WOL_DAEMON_NVRAMINFO = "WOL_DAEMON_NVRAMINFO";
    private final SharedPreferences mSharedPreferences;
    private NVRAMInfo mNvramInfo;

    public WakeOnLanDaemonTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_services_wol_daemon,
                R.id.tile_services_wol_daemon_togglebutton);
        mSharedPreferences = mParentFragmentActivity.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY,
                Context.MODE_PRIVATE);
    }

    @Override
    public int getTileHeaderViewId() {
        return R.id.tile_services_wol_daemon_hdr;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_services_wol_daemon_title;
    }

    @Nullable
    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {

                try {
                    Log.d(LOG_TAG, "Init background loader for " + WakeOnLanDaemonTile.class + ": routerInfo=" +
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
                                        WOL_ENABLE,
                                        WOL_INTERVAL,
                                        WOL_HOSTNAME,
                                        WOL_PASSWD,
                                        WOL_MACS);
                    } finally {
                        if (nvramInfoTmp != null) {
                            nvramInfo.putAll(nvramInfoTmp);
                        }

                        boolean applyNewPrefs = false;
                        String property = nvramInfo.getProperty(WOL_HOSTNAME);
                        final SharedPreferences.Editor editor = mSharedPreferences.edit();
                        if (!Strings.isNullOrEmpty(property)) {
                            final Set<String> mSharedPreferencesStringSet = new HashSet<>(mSharedPreferences.getStringSet(WOL_HOSTNAME,
                                    new HashSet<String>()));
                            if (!mSharedPreferencesStringSet.contains(property)) {
                                mSharedPreferencesStringSet.add(property);
                                editor
                                        .putStringSet(WOL_DAEMON_HOSTNAMES_PREF_KEY, mSharedPreferencesStringSet);
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
            Utils.displayUpgradeMessage(mParentFragmentActivity);
            return null;
        }

        if (mNvramInfo == null) {
            //Loading
            Utils.displayMessage(mParentFragmentActivity, "Loading data from router - please wait a few seconds.", Style.ALERT);
            return null;
        }

        if (mNvramInfo.isEmpty()) {
            //No data!
            Utils.displayMessage(mParentFragmentActivity, "No data available - please retry later.", Style.ALERT);
            return null;
        }

        final String mRouterUuid = mRouter.getUuid();
        final Intent editWOLDaemonSettingsIntent =
                new Intent(mParentFragment.getActivity(), EditWOLDaemonSettingsActivity.class);
        editWOLDaemonSettingsIntent.putExtra(WOL_DAEMON_NVRAMINFO, mNvramInfo);
        editWOLDaemonSettingsIntent.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouterUuid);

        return new OnClickIntent("Loading WOL Daemon Settings...",
                editWOLDaemonSettingsIntent, this);
    }

    @Override
    public void onLoadFinished(Loader<NVRAMInfo> loader, NVRAMInfo data) {
        Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

        layout.findViewById(R.id.tile_services_wol_daemon_header_loading_view)
                .setVisibility(View.GONE);
        layout.findViewById(R.id.tile_services_wol_daemon_loading_view)
                .setVisibility(View.GONE);
        final View openvpnclStatus = layout.findViewById(R.id.tile_services_wol_daemon_status);
        openvpnclStatus
                .setVisibility(View.VISIBLE);
        layout.findViewById(R.id.tile_services_wol_daemon_grid_layout)
                .setVisibility(View.VISIBLE);
        layout.findViewById(R.id.tile_services_wol_daemon_note)
                .setVisibility(View.VISIBLE);

        if (data == null) {
            data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
        }

        final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_services_wol_daemon_error);

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
                R.id.tile_services_wol_daemon_togglebutton_title, R.id.tile_services_wol_daemon_togglebutton_separator);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");

    }

    private void updateTileDisplayData(@NonNull final NVRAMInfo data, final boolean defaultValuesIfNotFound) {

        //State
        final String statusKey = \"fake-key\";
                defaultValuesIfNotFound ? EMPTY_STRING : null);
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
            ((TextView) layout.findViewById(R.id.tile_services_wol_daemon_status)).setText(statusValue);

            ((TextView) layout.findViewById(R.id.tile_services_wol_daemon_state)).setText(statusValue);
        }

        //Interval
        String property = data.getProperty(WOL_INTERVAL, defaultValuesIfNotFound ? EMPTY_VALUE_TO_DISPLAY : null);
        if (property != null) {
            ((TextView) layout.findViewById(R.id.tile_services_wol_daemon_interval))
                    .setText(property);
        }

        //Hostname
        property = data.getProperty(WOL_HOSTNAME, defaultValuesIfNotFound ? EMPTY_VALUE_TO_DISPLAY : null);
        if (property != null) {
            ((TextView) layout.findViewById(R.id.tile_services_wol_daemon_hostname))
                    .setText(property);
        }

        //MAC Addresses
        property = data.getProperty(WOL_MACS, defaultValuesIfNotFound ? EMPTY_VALUE_TO_DISPLAY : null);
        if (property != null) {
            ((TextView) layout.findViewById(R.id.tile_services_wol_daemon_mac_addresses))
                    .setText(property.replaceAll(" ", "\n"));
        }

    }

    @Override
    public void onResultCode(int resultCode, Intent data) {
        switch (resultCode) {
            case Activity.RESULT_OK:
                final NVRAMInfo newNvramInfoData = (NVRAMInfo) data.getSerializableExtra(WOL_DAEMON_NVRAMINFO);
                if (newNvramInfoData == null || newNvramInfoData.isEmpty()) {
                    Utils.displayMessage(mParentFragmentActivity, "No change", Style.INFO);
                    break;
                }

                final Bundle token = new Bundle();
                token.putString(DDWRTMainActivity.ROUTER_ACTION, RouterAction.SET_NVRAM_VARIABLES.name());
                token.putSerializable(WOL_DAEMON_NVRAMINFO, newNvramInfoData);

                new UndoBarController.UndoBar(mParentFragmentActivity)
                        .message("WOL Daemon Settings will be updated on the Router.")
                        .listener(this)
                        .token(token)
                        .show();
                break;
            default:
                //Ignored
                break;
        }
    }

    @Override
    public void onHide(@Nullable Parcelable parcelable) {
        if (parcelable instanceof Bundle) {
            final Bundle token = (Bundle) parcelable;
            final String routerAction = token.getString(DDWRTMainActivity.ROUTER_ACTION);
            Log.d(LOG_TAG, "routerAction: [" + routerAction + "]");
            if (isNullOrEmpty(routerAction)) {
                return;
            }
            try {
                switch (RouterAction.valueOf(routerAction)) {
                    case SET_NVRAM_VARIABLES:
                        new SetNVRAMVariablesAction(mParentFragmentActivity,
                                (NVRAMInfo) token.getSerializable(WOL_DAEMON_NVRAMINFO),
                                true, //Reboot Router at the end of the operation
                                this,
                                mGlobalPreferences)
                                .execute(mRouter);
                        break;
                    default:
                        //Ignored
                        break;
                }
            } catch (IllegalArgumentException | NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClear(@NonNull Parcelable[] parcelables) {
        //Nothing to do
    }

    @Override
    public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router, final Object returnData) {
        Utils.displayMessage(mParentFragmentActivity,
                "Success",
                Style.CONFIRM);
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

    @Override
    public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull Router router, @Nullable Exception exception) {
        Utils.displayMessage(mParentFragmentActivity,
                String.format("Error: %s", ExceptionUtils.getRootCauseMessage(exception)),
                Style.ALERT);
    }

    @Override
    public void onUndo(@Nullable Parcelable parcelable) {
        //Nothing to do
    }
}
