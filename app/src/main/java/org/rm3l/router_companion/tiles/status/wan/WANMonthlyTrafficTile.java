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

package org.rm3l.router_companion.tiles.status.wan;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.router_companion.RouterCompanionAppConstants.EMPTY_STRING;
import static org.rm3l.router_companion.RouterCompanionAppConstants.WAN_CYCLE_DAY_PREF;
import static org.rm3l.router_companion.resources.Encrypted.d;
import static org.rm3l.router_companion.utils.Utils.fromHtml;
import static org.rm3l.router_companion.utils.WANTrafficUtils.HIDDEN_;
import static org.rm3l.router_companion.utils.WANTrafficUtils.TOTAL_DL_CURRENT_MONTH;
import static org.rm3l.router_companion.utils.WANTrafficUtils.TOTAL_DL_CURRENT_MONTH_MB;
import static org.rm3l.router_companion.utils.WANTrafficUtils.TOTAL_UL_CURRENT_MONTH;
import static org.rm3l.router_companion.utils.WANTrafficUtils.TOTAL_UL_CURRENT_MONTH_MB;
import static org.rm3l.router_companion.utils.WANTrafficUtils.getTrafficDataNvramInfoAndPersistIfNeeded;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.FileProvider;
import android.support.v4.content.Loader;
import android.support.v4.content.PermissionChecker;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.gson.Gson;
import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.actions.ActionManager;
import org.rm3l.router_companion.actions.BackupWANMonthlyTrafficRouterAction;
import org.rm3l.router_companion.actions.EraseWANMonthlyTrafficRouterAction;
import org.rm3l.router_companion.actions.RouterAction;
import org.rm3l.router_companion.actions.RouterActionListener;
import org.rm3l.router_companion.actions.SetNVRAMVariablesAction;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.router_companion.resources.MonthlyCycleItem;
import org.rm3l.router_companion.resources.WANTrafficData;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.WANTrafficUtils;
import org.rm3l.router_companion.utils.snackbar.SnackbarCallback;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style;

/**
 *
 */
public class WANMonthlyTrafficTile extends DDWRTTile<NVRAMInfo>
        implements SnackbarCallback, RouterActionListener {

    public static class DDWRTTraffDataDisabled extends DDWRTNoDataException {

        public DDWRTTraffDataDisabled(@Nullable String detailMessage) {
            super(detailMessage);
        }
    }

    private class ManageWANTrafficCounterToggle implements View.OnClickListener {

        private boolean enable;

        @Override
        public void onClick(View view) {

            isToggleStateActionRunning.set(true);

            if (!(view instanceof CompoundButton)) {
                Utils.reportException(null, new IllegalStateException(
                        "ManageWANTrafficCounterToggle#onClick: "
                                + "view is NOT an instance of CompoundButton!"));
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

            final NVRAMInfo nvramInfoToSet = new NVRAMInfo();

            nvramInfoToSet.setProperty(NVRAMInfo.Companion.getTTRAFF_ENABLE(), enable ? "1" : "0");

            //Also set traff data loaded from preferences, if any
            if (enable && mParentFragmentPreferences != null) {
                //Also restore traffic data we had in preferences
                final Set<String> traffMonths = FluentIterable.from(Optional.fromNullable(
                        mParentFragmentPreferences.getStringSet(WAN_MONTHLY_TRAFFIC, new HashSet<String>()))
                        .or(new HashSet<String>())).transform(new Function<String, String>() {
                    @Override
                    public String apply(@Nullable String input) {
                        return d(input);
                    }
                }).toSet();

                for (final String traffMonth : traffMonths) {
                    if (traffMonth == null || traffMonth.isEmpty()) {
                        continue;
                    }
                    final String traffMonthDataSaved =
                            d(mParentFragmentPreferences.getString(traffMonth, null));
                    if (traffMonthDataSaved == null || traffMonthDataSaved.isEmpty()) {
                        continue;
                    }
                    nvramInfoToSet.setProperty(traffMonth, traffMonthDataSaved);
                }
            }

            SnackbarUtils.buildSnackbar(mParentFragmentActivity,
                    String.format("WAN Traffic Counter will be %s on '%s' (%s). ",
                            //                                    "Router will be rebooted at the end of the operation.",
                            enable ? "enabled" : "disabled", mRouter.getDisplayName(),
                            mRouter.getRemoteIpAddress()), "CANCEL", Snackbar.LENGTH_LONG,
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
                                    String.format("%s WAN Traffic Counter...", enable ? "Enabling" : "Disabling"),
                                    Style.INFO);

                            ActionManager.runTasks(
                                    new SetNVRAMVariablesAction(mRouter, mParentFragmentActivity, nvramInfoToSet,
                                            false,
                                            new RouterActionListener() {
                                                @Override
                                                public void onRouterActionFailure(@NonNull RouterAction routerAction,
                                                        @NonNull final Router router,
                                                        @Nullable final Exception exception) {
                                                    mParentFragmentActivity.runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            try {
                                                                compoundButton.setChecked(!enable);
                                                                Utils.displayMessage(mParentFragmentActivity,
                                                                        String.format(
                                                                                "Error while trying to %s WAN Traffic Counter on '%s' (%s): %s",
                                                                                enable ? "enable" : "disable",
                                                                                router.getDisplayName(),
                                                                                router.getRemoteIpAddress(),
                                                                                Utils.handleException(
                                                                                        exception).first),
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
                                                                                "WAN Traffic Counter %s successfully on host '%s' (%s)",
                                                                                enable ? "enabled" : "disabled",
                                                                                router.getDisplayName(),
                                                                                router.getRemoteIpAddress()),
                                                                        Style.CONFIRM);
                                                            } finally {
                                                                compoundButton.setEnabled(true);
                                                                isToggleStateActionRunning.set(false);
                                                                if (mLoader != null) {
                                                                    //Reload everything right away
                                                                    doneWithLoaderInstance(WANMonthlyTrafficTile.this,
                                                                            mLoader, 1l);
                                                                }
                                                            }
                                                        }
                                                    });
                                                }
                                            }

                                            , mGlobalPreferences));
                        }

                        @Override
                        public void onShowEvent(@Nullable Bundle bundle) throws Exception {

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
            //    String.format("WAN Traffic Counter will be %s on '%s' (%s). ",
            //        //                                    "Router will be rebooted at the end of the operation.",
            //        enable ? "enabled" : "disabled", mRouter.getDisplayName(),
            //        mRouter.getRemoteIpAddress())).listener(new UndoBarController.AdvancedUndoListener() {
            //                                                  @Override public void onHide(@Nullable Parcelable parcelable) {
            //
            //
            //                                                  }
            //
            //                                                  @Override public void onClear(@NonNull Parcelable[] parcelables) {
            //                                                    mParentFragmentActivity.runOnUiThread(new Runnable() {
            //                                                      @Override public void run() {
            //                                                        try {
            //                                                          compoundButton.setChecked(!enable);
            //                                                          compoundButton.setEnabled(true);
            //                                                        } finally {
            //                                                          isToggleStateActionRunning.set(false);
            //                                                        }
            //                                                      }
            //                                                    });
            //                                                  }
            //
            //                                                  @Override public void onUndo(@Nullable Parcelable parcelable) {
            //
            //                                                  }
            //                                                }
            //
            //).
            //
            //    token(new Bundle()
            //
            //    ).
            //
            //    show();
        }
    }

    public static final String WAN_MONTHLY_TRAFFIC = "WANMonthlyTraffic";

    public static final String WAN_MONTHLY_TRAFFIC_BACKUP_FILETYPE =
            "WAN_MONTHLY_TRAFFIC_BACKUP_FILETYPE";

    public static final String RESTORE_WAN_MONTHLY_TRAFFIC_FRAGMENT_TAG =
            "RESTORE_WAN_MONTHLY_TRAFFIC_FRAGMENT_TAG";

    public static final String WAN_MONTHLY_TRAFFIC_ACTION = "WAN_MONTHLY_TRAFFIC_ACTION";

    public static final String WAN_CYCLE_DISPLAYED = "wanCycleDisplayed";

    private static final String LOG_TAG = WANMonthlyTrafficTile.class.getSimpleName();

    private final DDWRTCompanionDAO dao;

    private AtomicBoolean isToggleStateActionRunning = new AtomicBoolean(false);

    private final AtomicReference<MonthlyCycleItem> mCurrentCycle;

    @NonNull
    private MonthlyCycleItem mCycleOfTheDay;

    private Gson mGson;

    private boolean mIsThemeLight;

    private long mLastSync;

    private AsyncTaskLoader<NVRAMInfo> mLoader;

    public WANMonthlyTrafficTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments,
            Router router) {
        super(parentFragment, arguments, router, R.layout.tile_status_wan_monthly_traffic, null);

        dao = RouterManagementActivity.getDao(mParentFragmentActivity);
        mIsThemeLight = ColorUtils.Companion.isThemeLight(mParentFragmentActivity);

        mGson = new Gson();

        //Initialize w/ current cycle
        mCycleOfTheDay =
                WANTrafficData.Companion.getCurrentWANCycle(mParentFragmentActivity, mParentFragmentPreferences);

        mCurrentCycle = new AtomicReference<>(mCycleOfTheDay);

        final TextView monthYearTextViewToDisplay =
                (TextView) this.layout.findViewById(R.id.tile_status_wan_monthly_month_displayed);
        monthYearTextViewToDisplay.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {

                final MonthlyCycleItem currentCycleItem = mCurrentCycle.get();
                if (currentCycleItem == null) {
                    return;
                }

                final NVRAMInfo data =
                        WANTrafficUtils.computeWANTrafficUsageBetweenDates(dao, mRouter.getUuid(),
                                currentCycleItem.getStart(), currentCycleItem.getEnd());

                //                final boolean isCurrentMonthYear = mCycleOfTheDay.equals(currentCycleItem);

                WANMonthlyTrafficTile.this.layout.findViewById(
                        R.id.tile_status_wan_monthly_traffic_graph_placeholder_current).setEnabled(true);
                WANMonthlyTrafficTile.this.layout.findViewById(
                        R.id.tile_status_wan_monthly_traffic_graph_placeholder_next).setEnabled(true);

                //Display traffic data for this month
                if (data.isEmpty()) {
                    Toast.makeText(WANMonthlyTrafficTile.this.mParentFragmentActivity,
                            String.format("No traffic data for '%s'. Please try again later.",
                                    currentCycleItem.getLabelWithYears()), Toast.LENGTH_SHORT).show();
                    return;
                }

                final int dlDrawable;
                final int ulDrawable;
                if (mIsThemeLight) {
                    dlDrawable = R.drawable.ic_dl_dark;
                    ulDrawable = R.drawable.ic_ul_dark;
                } else {
                    dlDrawable = R.drawable.ic_dl_white;
                    ulDrawable = R.drawable.ic_ul_light;
                }

                final TextView wanDLView =
                        (TextView) layout.findViewById(R.id.tile_status_wan_monthly_traffic_dl);
                wanDLView.setCompoundDrawablesWithIntrinsicBounds(dlDrawable, 0, 0, 0);
                wanDLView.setText(data.getProperty(TOTAL_DL_CURRENT_MONTH, "-"));

                final TextView wanULView =
                        (TextView) layout.findViewById(R.id.tile_status_wan_monthly_traffic_ul);
                wanULView.setCompoundDrawablesWithIntrinsicBounds(ulDrawable, 0, 0, 0);
                wanULView.setText(data.getProperty(TOTAL_UL_CURRENT_MONTH, "-"));

                final TextView dlMB =
                        (TextView) layout.findViewById(R.id.tile_status_wan_monthly_traffic_dl_mb);
                final String dlMBytesFromNvram = data.getProperty(TOTAL_DL_CURRENT_MONTH_MB);
                if (HIDDEN_.equals(dlMBytesFromNvram)) {
                    dlMB.setVisibility(View.INVISIBLE);
                } else {
                    dlMB.setVisibility(View.VISIBLE);
                }
                dlMB.setText(dlMBytesFromNvram != null ? ("(" + dlMBytesFromNvram + " MB)") : "-");

                final TextView ulMB =
                        (TextView) layout.findViewById(R.id.tile_status_wan_monthly_traffic_ul_mb);
                final String ulMBytesFromNvram = data.getProperty(TOTAL_UL_CURRENT_MONTH_MB);
                if (HIDDEN_.equals(ulMBytesFromNvram)) {
                    ulMB.setVisibility(View.INVISIBLE);
                } else {
                    ulMB.setVisibility(View.VISIBLE);
                }
                ulMB.setText(ulMBytesFromNvram != null ? ("(" + ulMBytesFromNvram + " MB)") : "-");
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });

        //Create Options Menu
        final ImageButton tileMenu =
                (ImageButton) layout.findViewById(R.id.tile_status_wan_monthly_traffic_menu);

        if (!mIsThemeLight) {
            //Set menu background to white
            tileMenu.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
        }

        //Permission requests
        final int rwExternalStoragePermissionCheck =
                PermissionChecker.checkSelfPermission(mParentFragmentActivity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (rwExternalStoragePermissionCheck != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(mParentFragmentActivity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                SnackbarUtils.buildSnackbar(mParentFragmentActivity,
                        "Storage access is required to be able to backup and restore WAN traffic data.", "OK",
                        Snackbar.LENGTH_INDEFINITE, new SnackbarCallback() {
                            @Override
                            public void onDismissEventActionClick(int event, @Nullable Bundle bundle)
                                    throws Exception {
                                //Request permission
                                ActivityCompat.requestPermissions(mParentFragmentActivity,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        RouterCompanionAppConstants.Permissions.STORAGE);
                            }

                            @Override
                            public void onDismissEventConsecutive(int event, @Nullable Bundle bundle)
                                    throws Exception {

                            }

                            @Override
                            public void onDismissEventManual(int event, @Nullable Bundle bundle)
                                    throws Exception {

                            }

                            @Override
                            public void onDismissEventSwipe(int event, @Nullable Bundle bundle)
                                    throws Exception {

                            }

                            @Override
                            public void onDismissEventTimeout(int event, @Nullable Bundle bundle)
                                    throws Exception {

                            }

                            @Override
                            public void onShowEvent(@Nullable Bundle bundle) throws Exception {

                            }
                        }, null, true);
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(mParentFragmentActivity,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        RouterCompanionAppConstants.Permissions.STORAGE);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        final String displayName =
                String.format("'%s' (%s)", router.getDisplayName(), router.getRemoteIpAddress());

        tileMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final PopupMenu popup = new PopupMenu(mParentFragmentActivity, v);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        final int itemId = menuItem.getItemId();
                        //Store current value in preferences
                        switch (itemId) {
                            case R.id.tile_wan_monthly_traffic_backup_raw:
                                if (PermissionChecker.checkSelfPermission(mParentFragmentActivity,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                        != PackageManager.PERMISSION_GRANTED) {
                                    Utils.displayMessage(mParentFragmentActivity, "Storage access required",
                                            Style.ALERT);
                                    return false;
                                }
                                //Allowed for all
                                displayBackupDialog(displayName,
                                        BackupWANMonthlyTrafficRouterAction.BackupFileType_RAW);
                                return true;
                            case R.id.tile_wan_monthly_traffic_backup_csv:
                                if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
                                    //Download the full version to unlock this version
                                    Utils.displayUpgradeMessage(mParentFragmentActivity,
                                            "Backup WAN Traffic Data as CSV");
                                    return true;
                                }
                                if (PermissionChecker.checkSelfPermission(mParentFragmentActivity,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                        != PackageManager.PERMISSION_GRANTED) {
                                    Utils.displayMessage(mParentFragmentActivity, "Storage access required",
                                            Style.ALERT);
                                    return false;
                                }
                                displayBackupDialog(displayName,
                                        BackupWANMonthlyTrafficRouterAction.BackupFileType_CSV);
                                return true;
                            case R.id.tile_wan_monthly_traffic_restore:
                                if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
                                    //Download the full version to unlock this version
                                    Utils.displayUpgradeMessage(mParentFragmentActivity,
                                            "Restore WAN Monthly Traffic Data");
                                    return true;
                                }

                                if (PermissionChecker.checkSelfPermission(mParentFragmentActivity,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                        != PackageManager.PERMISSION_GRANTED) {
                                    Utils.displayMessage(mParentFragmentActivity, "Storage access required",
                                            Style.ALERT);
                                    return false;
                                }

                                final FragmentManager supportFragmentManager =
                                        mParentFragmentActivity.getSupportFragmentManager();
                                final Fragment restoreWANTraffic = supportFragmentManager.findFragmentByTag(
                                        RESTORE_WAN_MONTHLY_TRAFFIC_FRAGMENT_TAG);
                                if (restoreWANTraffic instanceof DialogFragment) {
                                    ((DialogFragment) restoreWANTraffic).dismiss();
                                }
                                final DialogFragment restoreFragment =
                                        RestoreWANMonthlyTrafficDialogFragment.newInstance(mRouter.getUuid());
                                restoreFragment.show(supportFragmentManager,
                                        RESTORE_WAN_MONTHLY_TRAFFIC_FRAGMENT_TAG);

                                return true;
                            case R.id.tile_wan_monthly_traffic_delete:
                                final Bundle token = new Bundle();
                                token.putString(WAN_MONTHLY_TRAFFIC_ACTION, RouterAction.DELETE_WAN_TRAFF.name());

                                SnackbarUtils.buildSnackbar(mParentFragmentActivity,
                                        String.format("Going to erase WAN Monthly Traffic Data on %s...",
                                                displayName),
                                        "CANCEL",
                                        Snackbar.LENGTH_LONG,
                                        WANMonthlyTrafficTile.this,
                                        token, true);

                                //new UndoBarController.UndoBar(mParentFragmentActivity).message(
                                //    String.format("Going to erase WAN Monthly Traffic Data on %s...", displayName))
                                //    .listener(WANMonthlyTrafficTile.this)
                                //    .token(token)
                                //    .show();
                                return true;
                            case R.id.tile_wan_monthly_traffic_change_cycle:
                                final android.support.v7.app.AlertDialog.Builder builder =
                                        new android.support.v7.app.AlertDialog.Builder(mParentFragmentActivity);
                                final LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());

                                final View view =
                                        dialogInflater.inflate(R.layout.data_usage_cycle_editor, null, false);
                                final NumberPicker cycleDayPicker =
                                        (NumberPicker) view.findViewById(R.id.wan_cycle_day);

                                final int wanCycleDay;
                                if (mParentFragmentPreferences != null) {
                                    final int cycleDay = mParentFragmentPreferences.getInt(WAN_CYCLE_DAY_PREF, 1);
                                    wanCycleDay = (cycleDay < 1 ? 1 : (cycleDay > 31 ? 31 : cycleDay));
                                } else {
                                    wanCycleDay = 1;
                                }

                                cycleDayPicker.setMinValue(1);
                                cycleDayPicker.setMaxValue(31);
                                cycleDayPicker.setValue(wanCycleDay);
                                cycleDayPicker.setWrapSelectorWheel(true);

                                builder.setTitle(R.string.data_usage_cycle_editor_title);
                                builder.setView(view);

                                builder.setCancelable(true);

                                builder.setPositiveButton(R.string.data_usage_cycle_editor_positive,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                // clear focus to finish pending text edits
                                                cycleDayPicker.clearFocus();

                                                final int wanCycleDay = cycleDayPicker.getValue();

                                                //Update preferences
                                                if (mParentFragmentPreferences == null) {
                                                    return;
                                                }
                                                mParentFragmentPreferences.edit()
                                                        .putInt(WAN_CYCLE_DAY_PREF, wanCycleDay)
                                                        .apply();

                                                mCycleOfTheDay = WANTrafficData.Companion
                                                        .getCurrentWANCycle(mParentFragmentActivity,
                                                                mParentFragmentPreferences);

                                                mCurrentCycle.set(mCycleOfTheDay);

                                                //Update
                                                final TextView monthYearDisplayed = (TextView) layout.findViewById(
                                                        R.id.tile_status_wan_monthly_month_displayed);
                                                monthYearDisplayed.setText(mCurrentCycle.get().getLabelWithYears());
                                            }
                                        });

                                builder.create().show();

                                return true;
                            default:
                                break;
                        }
                        return false;
                    }
                });
                final MenuInflater inflater = popup.getMenuInflater();

                final Menu menu = popup.getMenu();

                inflater.inflate(R.menu.tile_wan_monthly_traffic_options, menu);

                popup.show();
            }
        });
    }

    public void displayBackupDialog(final String displayName, final int backupFileType) {
        final Bundle token = new Bundle();
        token.putString(WAN_MONTHLY_TRAFFIC_ACTION, RouterAction.BACKUP_WAN_TRAFF.name());
        token.putInt(WAN_MONTHLY_TRAFFIC_BACKUP_FILETYPE, backupFileType);

        SnackbarUtils.buildSnackbar(mParentFragmentActivity,
                String.format("Backup of WAN Traffic Data (as %s) is going to start on %s...",
                        backupFileType, displayName),
                "CANCEL",
                Snackbar.LENGTH_LONG,
                WANMonthlyTrafficTile.this,
                token, true);

        //new UndoBarController.UndoBar(mParentFragmentActivity).message(
        //    String.format("Backup of WAN Traffic Data (as %s) is going to start on %s...",
        //        backupFileType, displayName)).listener(WANMonthlyTrafficTile.this).token(token).show();
    }

    @Override
    public int getTileHeaderViewId() {
        return R.id.tile_status_wan_monthly_traffic_hdr;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_status_wan_monthly_traffic_title;
    }

    @Override
    public void onDismissEventActionClick(int event, @Nullable Bundle bundle)
            throws Exception {

    }

    @Override
    public void onDismissEventConsecutive(int event, @Nullable Bundle bundle)
            throws Exception {

    }

    @Override
    public void onDismissEventManual(int event, @Nullable Bundle bundle) throws Exception {

    }

    @Override
    public void onDismissEventSwipe(int event, @Nullable Bundle bundle) throws Exception {

    }

    @Override
    public void onDismissEventTimeout(int event, @Nullable Bundle token) throws Exception {
        final String routerAction = token != null ? token.getString(WAN_MONTHLY_TRAFFIC_ACTION) : null;
        Crashlytics.log(Log.DEBUG, LOG_TAG,
                "WAN Monthly Traffic Data Action: [" + routerAction + "]");
        if (isNullOrEmpty(routerAction)) {
            return;
        }
        try {
            switch (RouterAction.valueOf(routerAction)) {
                case DELETE_WAN_TRAFF: {
                    final AlertDialog alertDialog = Utils.
                            buildAlertDialog(mParentFragmentActivity, null,
                                    "Erasing WAN Traffic Data - please hold on...", false, false);
                    alertDialog.show();
                    ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(
                            Gravity.CENTER_HORIZONTAL);
                    ActionManager.runTasks(
                            new EraseWANMonthlyTrafficRouterAction(mRouter, mParentFragmentActivity,
                                    new RouterActionListener() {
                                        @Override
                                        public void onRouterActionFailure(@NonNull RouterAction routerAction,
                                                @NonNull Router router, @Nullable Exception exception) {
                                            try {
                                                WANMonthlyTrafficTile.this.onRouterActionFailure(routerAction, router,
                                                        exception);
                                            } finally {
                                                mParentFragmentActivity.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        alertDialog.cancel();
                                                    }
                                                });
                                            }
                                        }

                                        @Override
                                        public void onRouterActionSuccess(@NonNull RouterAction routerAction,
                                                @NonNull Router router, Object returnData) {
                                            try {
                                                //dao delete everything
                                                dao.deleteWANTrafficDataByRouter(mRouter.getUuid());
                                                WANMonthlyTrafficTile.this.onRouterActionSuccess(routerAction, router,
                                                        returnData);
                                            } finally {
                                                mParentFragmentActivity.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        alertDialog.cancel();
                                                    }
                                                });
                                            }
                                            if (mLoader != null) {
                                                //Reload everything right away
                                                doneWithLoaderInstance(WANMonthlyTrafficTile.this, mLoader, 1l);
                                            }
                                        }
                                    }, mGlobalPreferences));
                }
                return;
                case BACKUP_WAN_TRAFF:
                    final int fileType = token.getInt(WAN_MONTHLY_TRAFFIC_BACKUP_FILETYPE,
                            BackupWANMonthlyTrafficRouterAction.BackupFileType_RAW);
                    final AlertDialog alertDialog = Utils.
                            buildAlertDialog(mParentFragmentActivity, null,
                                    "Backing up WAN Traffic Data - please hold on...", false, false);
                    alertDialog.show();
                    ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(
                            Gravity.CENTER_HORIZONTAL);
                    ActionManager.runTasks(
                            new BackupWANMonthlyTrafficRouterAction(mRouter, fileType, mParentFragmentActivity,
                                    new RouterActionListener() {

                                        @Override
                                        public void onRouterActionFailure(@NonNull RouterAction routerAction,
                                                @NonNull Router router, @Nullable Exception exception) {
                                            try {
                                                Utils.displayMessage(mParentFragmentActivity,
                                                        String.format("Error on action '%s': %s",
                                                                routerAction.toString(),
                                                                Utils.handleException(exception).first), Style.ALERT);
                                            } finally {
                                                mParentFragmentActivity.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        alertDialog.cancel();
                                                    }
                                                });
                                            }
                                        }

                                        @Override
                                        public void onRouterActionSuccess(@NonNull RouterAction routerAction,
                                                @NonNull Router router, Object returnData) {
                                            try {
                                                String msg;
                                                if (!((returnData instanceof Object[])
                                                        && ((Object[]) returnData).length >= 2)) {
                                                    msg = String.format("Action '%s' executed "
                                                                    + "successfully on host '%s', but an internal error occurred. "
                                                                    + "The issue will be reported. Please try again later.",
                                                            routerAction.toString(), router.getRemoteIpAddress());
                                                    Utils.displayMessage(mParentFragmentActivity, msg, Style.INFO);
                                                    Utils.reportException(null, new IllegalStateException(msg));
                                                    return;
                                                }

                                                final Object[] returnDataObjectArray = ((Object[]) returnData);
                                                final Object backupDateObject = returnDataObjectArray[0];
                                                final Object localBackupFileObject = returnDataObjectArray[1];

                                                if (!((backupDateObject instanceof Date)
                                                        && (localBackupFileObject instanceof File))) {
                                                    msg = String.format("Action '%s' executed "
                                                                    + "successfully on host '%s', but could not determine where "
                                                                    + "local backup file has been saved. Please try again later.",
                                                            routerAction.toString(), router.getRemoteIpAddress());
                                                    Utils.displayMessage(mParentFragmentActivity, msg, Style.INFO);
                                                    Utils.reportException(null, new IllegalStateException(msg));
                                                    return;
                                                }

                                                Utils.displayMessage(mParentFragmentActivity, String.format(
                                                        "Action '%s' executed successfully on host '%s'. "
                                                                + "Now loading the file sharing activity chooser...",
                                                        routerAction.toString(), router.getRemoteIpAddress()),
                                                        Style.CONFIRM);

                                                final File localBackupFile = (File) (((Object[]) returnData)[1]);
                                                final Date backupDate = (Date) (((Object[]) returnData)[0]);

                                                final Uri uriForFile = FileProvider
                                                        .getUriForFile(mParentFragmentActivity,
                                                                RouterCompanionAppConstants.FILEPROVIDER_AUTHORITY,
                                                                localBackupFile);
                                                mParentFragmentActivity.grantUriPermission(
                                                        mParentFragmentActivity.getPackageName(), uriForFile,
                                                        Intent.FLAG_GRANT_READ_URI_PERMISSION);

                                                final Intent shareIntent = new Intent();
                                                shareIntent.setAction(Intent.ACTION_SEND);
                                                shareIntent.putExtra(Intent.EXTRA_SUBJECT,
                                                        String.format("Backup of WAN Monthly Traffic on Router '%s'",
                                                                mRouter.getCanonicalHumanReadableName()));
                                                shareIntent.setType("text/html");
                                                shareIntent.putExtra(Intent.EXTRA_TEXT, fromHtml(
                                                        ("Backup Date: " + backupDate + "\n\n")
                                                                .replaceAll("\n", "<br/>")
                                                                + Utils.getShareIntentFooter()));
                                                shareIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);
                                                mParentFragmentActivity
                                                        .startActivity(Intent.createChooser(shareIntent,
                                                                mParentFragmentActivity.getResources()
                                                                        .getText(R.string.share_backup)));
                                            } finally {
                                                mParentFragmentActivity.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        alertDialog.cancel();
                                                    }
                                                });
                                            }
                                        }
                                    }, mGlobalPreferences));
                    return;
                default:
                    break;
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            e.printStackTrace();
            Utils.reportException(null, e);
        }
    }

    @Override
    public void onLoadFinished(Loader<NVRAMInfo> loader, NVRAMInfo data) {
        try {
            Crashlytics.log(Log.DEBUG, LOG_TAG,
                    "onLoadFinished: loader=" + loader + " / data=" + data + " / data=" + data);

            setLoadingViewVisibility(View.GONE);
            layout.findViewById(R.id.tile_status_wan_monthly_traffic_header_loading_view)
                    .setVisibility(View.GONE);
            layout.findViewById(R.id.tile_status_wan_monthly_traffic_title).setVisibility(View.VISIBLE);

            Exception preliminaryCheckException = null;
            if (data == null) {
                preliminaryCheckException = new DDWRTNoDataException("No Data!");
            } else //noinspection ThrowableResultOfMethodCallIgnored
                if (data.getException() == null) {
                    if (!"1".equals(data.getProperty(NVRAMInfo.Companion.getTTRAFF_ENABLE()))) {
                        preliminaryCheckException = new DDWRTTraffDataDisabled("Traffic monitoring disabled!");
                    } else if (data.isEmpty()) {
                        preliminaryCheckException = new DDWRTNoDataException("No Traffic Data!");
                    }
                }

            final SwitchCompat enableTraffDataButton =
                    (SwitchCompat) this.layout.findViewById(R.id.tile_status_wan_monthly_traffic_status);
            enableTraffDataButton.setVisibility(View.VISIBLE);

            final boolean makeToogleEnabled = (data != null && data.getData() != null && data.getData()
                    .containsKey(NVRAMInfo.Companion.getTTRAFF_ENABLE()));

            if (!isToggleStateActionRunning.get()) {
                if (makeToogleEnabled) {
                    if ("1".equals(data.getProperty(NVRAMInfo.Companion.getTTRAFF_ENABLE()))) {
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
                enableTraffDataButton.setOnClickListener(new ManageWANTrafficCounterToggle());
            }

            if (preliminaryCheckException != null) {
                data = new NVRAMInfo().setException(preliminaryCheckException);
            }

            final TextView errorPlaceHolderView =
                    (TextView) this.layout.findViewById(R.id.tile_status_wan_monthly_traffic_error);

            final Exception exception = data.getException();

            final View displayButton = this.layout.findViewById(
                    R.id.tile_status_wan_monthly_traffic_graph_placeholder_display_button);
            final View currentButton =
                    this.layout.findViewById(R.id.tile_status_wan_monthly_traffic_graph_placeholder_current);
            final View previousButton =
                    this.layout.findViewById(R.id.tile_status_wan_monthly_traffic_graph_placeholder_previous);
            final View nextButton =
                    this.layout.findViewById(R.id.tile_status_wan_monthly_traffic_graph_placeholder_next);
            final TextView monthYearDisplayed =
                    (TextView) this.layout.findViewById(R.id.tile_status_wan_monthly_month_displayed);

            final View[] ctrlViews = new View[]{
                    monthYearDisplayed, displayButton, currentButton, previousButton, nextButton
            };

            //Create Options Menu
            final ImageButton tileMenu =
                    (ImageButton) layout.findViewById(R.id.tile_status_wan_monthly_traffic_menu);

            if (!ColorUtils.Companion.isThemeLight(mParentFragmentActivity)) {
                //Set menu background to white
                tileMenu.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
            }

            tileMenu.setVisibility(View.VISIBLE);

            if (exception == null) {
                errorPlaceHolderView.setVisibility(View.GONE);

                monthYearDisplayed.setText(mCurrentCycle.get().getLabelWithYears());

                displayButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {

                        final CharSequence monthYearDisplayedText = monthYearDisplayed.getText();

                        final MonthlyCycleItem cycleItem = mCurrentCycle.get();
                        if (cycleItem == null) {
                            Toast.makeText(WANMonthlyTrafficTile.this.mParentFragmentActivity,
                                    String.format("No traffic data for '%s'", monthYearDisplayedText),
                                    Toast.LENGTH_SHORT).show();
                        } else {

                            final Intent intent =
                                    new Intent(mParentFragmentActivity, WANMonthlyTrafficActivity.class);
                            intent.putExtra(RouterManagementActivity.ROUTER_SELECTED,
                                    mRouter != null ? mRouter.getUuid() : EMPTY_STRING);
                            intent.putExtra(WANMonthlyTrafficActivity.WAN_CYCLE, mGson.toJson(cycleItem));

                            final ProgressDialog alertDialog = ProgressDialog.show(mParentFragmentActivity,
                                    String.format("Loading traffic data for '%s'", monthYearDisplayedText),
                                    "Please Wait...", true);

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    final ActivityOptionsCompat options =
                                            ActivityOptionsCompat.makeScaleUpAnimation(v, 0, 0, v.getWidth(),
                                                    v.getHeight());
                                    ActivityCompat.startActivity(mParentFragmentActivity, intent, options.toBundle());
                                    //
                                    //                                    mParentFragmentActivity.startActivity(intent);
                                    //                                    mParentFragmentActivity.overridePendingTransition(
                                    //                                            R.anim.zoom_enter, R.anim.zoom_exit);
                                    alertDialog.cancel();
                                }
                            }, 1000);
                        }
                    }
                });

                currentButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCycleOfTheDay = WANTrafficData.Companion.getCurrentWANCycle(mParentFragmentActivity,
                                mParentFragmentPreferences);
                        mCurrentCycle.set(mCycleOfTheDay);
                        monthYearDisplayed.setText(mCycleOfTheDay.getLabelWithYears());
                        if (mParentFragmentPreferences != null) {
                            try {
                                mParentFragmentPreferences.edit()
                                        .remove(getFormattedPrefKey(WAN_CYCLE_DISPLAYED))
                                        .apply();
                            } catch (final Exception e) {
                                //No worries
                            }
                        }
                    }
                });

                previousButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final MonthlyCycleItem prevCycleItem = mCurrentCycle.get()
                                .setContext(mParentFragmentActivity)
                                .setRouterPreferences(mRouter.getPreferences(mParentFragmentActivity))
                                .prev();
                        mCurrentCycle.set(prevCycleItem);
                        monthYearDisplayed.setText(prevCycleItem.getLabelWithYears());
                        if (mParentFragmentPreferences != null) {
                            try {
                                mParentFragmentPreferences.edit()
                                        .putString(getFormattedPrefKey(WAN_CYCLE_DISPLAYED),
                                                mGson.toJson(prevCycleItem))
                                        .apply();
                            } catch (final Exception e) {
                                //No worries
                            }
                        }
                    }
                });

                nextButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final MonthlyCycleItem nextCycleItem = mCurrentCycle.get()
                                .setContext(mParentFragmentActivity)
                                .setRouterPreferences(mRouter.getPreferences(mParentFragmentActivity))
                                .next();
                        mCurrentCycle.set(nextCycleItem);
                        monthYearDisplayed.setText(nextCycleItem.getLabelWithYears());
                        if (mParentFragmentPreferences != null) {
                            try {
                                mParentFragmentPreferences.edit()
                                        .putString(getFormattedPrefKey(WAN_CYCLE_DISPLAYED),
                                                mGson.toJson(nextCycleItem))
                                        .apply();
                            } catch (final Exception e) {
                                //No worries
                            }
                        }
                    }
                });

                setVisibility(ctrlViews, View.VISIBLE);

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
                setVisibility(ctrlViews, View.GONE);
                updateProgressBarWithError();
            } else if (exception == null) {
                updateProgressBarWithSuccess();
                if (data == null || data.isEmpty()) {
                    errorPlaceHolderView.setText("Error: No Data!");
                    errorPlaceHolderView.setVisibility(View.VISIBLE);
                    setVisibility(ctrlViews, View.GONE);
                }
            }

            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished(): done loading!");
        } finally {
            mRefreshing.set(false);
            doneWithLoaderInstance(this, loader);
        }
    }

    @Override
    public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull Router router,
            @Nullable Exception exception) {
        Utils.displayMessage(mParentFragmentActivity,
                String.format("Error on action '%s': %s", routerAction.toString(),
                        Utils.handleException(exception).first), Style.ALERT);
    }

    @Override
    public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router,
            Object returnData) {
        Utils.displayMessage(mParentFragmentActivity,
                String.format("Action '%s' executed successfully on host '%s'", routerAction.toString(),
                        router.getRemoteIpAddress()), Style.CONFIRM);
    }

    @Override
    public void onShowEvent(@Nullable Bundle bundle) throws Exception {

    }

    @Nullable
    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {

        if (nbRunsLoader <= 0) {
            setLoadingViewVisibility(View.VISIBLE);
        }

        mLoader = new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {

                try {

                    mIsThemeLight = ColorUtils.Companion.isThemeLight(mParentFragmentActivity);

                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Init background loader for "
                            + WANMonthlyTrafficTile.class
                            + ": routerInfo="
                            + mRouter
                            + " / nbRunsLoader="
                            + nbRunsLoader);

                    if (mRefreshing.getAndSet(true)) {
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    if (!isForceRefresh()) {
                        //Force Manual Refresh
                        if (isToggleStateActionRunning.get()) {
                            //Skip run
                            Crashlytics.log(Log.DEBUG, LOG_TAG, "Skip loader run");
                            throw new DDWRTTileAutoRefreshNotAllowedException();
                        }
                    } else {
                        if (isToggleStateActionRunning.get()) {
                            //Action running - skip
                            throw new DDWRTTileAutoRefreshNotAllowedException();
                        }
                    }
                    nbRunsLoader++;

                    updateProgressBarViewSeparator(0);

                    mLastSync = System.currentTimeMillis();

                    updateProgressBarViewSeparator(10);
                    //Get TTRAFF_ENABLE
                    final NVRAMInfo ttraffEnableNVRAMInfo =
                            SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                    NVRAMInfo.Companion.getTTRAFF_ENABLE());

                    updateProgressBarViewSeparator(20);
                    mCycleOfTheDay = WANTrafficData.Companion.getCurrentWANCycle(mParentFragmentActivity,
                            mParentFragmentPreferences);

                    MonthlyCycleItem cycleItem = null;
                    if (mParentFragmentPreferences != null) {
                        final String wanCycleDisplayed =
                                mParentFragmentPreferences.getString(getFormattedPrefKey(WAN_CYCLE_DISPLAYED),
                                        null);
                        if (!isNullOrEmpty(wanCycleDisplayed)) {
                            try {
                                cycleItem = mGson.fromJson(wanCycleDisplayed, MonthlyCycleItem.class);
                            } catch (final Exception e) {
                                //No worries
                            }
                        }
                    }
                    mCurrentCycle.set(cycleItem != null ? cycleItem : mCycleOfTheDay);

                    getTrafficDataNvramInfoAndPersistIfNeeded(mParentFragmentActivity, mRouter,
                            mGlobalPreferences, dao);

                    updateProgressBarViewSeparator(55);
                    final NVRAMInfo nvramInfo =
                            WANTrafficUtils.computeWANTrafficUsageBetweenDates(dao, mRouter.getUuid(),
                                    mCurrentCycle.get().getStart(), mCurrentCycle.get().getEnd());

                    if (ttraffEnableNVRAMInfo != null) {
                        nvramInfo.putAll(ttraffEnableNVRAMInfo);
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

    private void setLoadingViewVisibility(final int visibility) {
        this.layout.findViewById(R.id.tile_status_wan_monthly_month_loading).setVisibility(visibility);
    }

    private static void setVisibility(@NonNull final View[] views, final int visibility) {
        for (final View view : views) {
            view.setVisibility(visibility);
        }
    }
}
