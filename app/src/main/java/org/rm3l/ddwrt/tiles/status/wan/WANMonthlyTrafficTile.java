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

package org.rm3l.ddwrt.tiles.status.wan;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.FileProvider;
import android.support.v4.content.Loader;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.cocosw.undobar.UndoBarController;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.actions.BackupWANMonthlyTrafficRouterAction;
import org.rm3l.ddwrt.actions.BackupWANMonthlyTrafficRouterAction.BackupFileType;
import org.rm3l.ddwrt.actions.EraseWANMonthlyTrafficRouterAction;
import org.rm3l.ddwrt.actions.RouterAction;
import org.rm3l.ddwrt.actions.RouterActionListener;
import org.rm3l.ddwrt.actions.SetNVRAMVariablesAction;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.NVRAMParser;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.ddwrt.resources.Encrypted.d;
import static org.rm3l.ddwrt.resources.Encrypted.e;

/**
 *
 */
public class WANMonthlyTrafficTile
        extends DDWRTTile<NVRAMInfo>
        implements UndoBarController.AdvancedUndoListener, RouterActionListener {

    public static final Splitter MONTHLY_TRAFF_DATA_SPLITTER = Splitter.on(" ").omitEmptyStrings();
    public static final Splitter DAILY_TRAFF_DATA_SPLITTER = Splitter.on(":").omitEmptyStrings();
    protected static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("MM-yyyy", Locale.US);
    private static final String LOG_TAG = WANMonthlyTrafficTile.class.getSimpleName();
    public static final String WAN_MONTHLY_TRAFFIC = "WANMonthlyTraffic";
    public static final String WAN_MONTHLY_TRAFFIC_BACKUP_FILETYPE = "WAN_MONTHLY_TRAFFIC_BACKUP_FILETYPE";

    public static final String RESTORE_WAN_MONTHLY_TRAFFIC_FRAGMENT_TAG = "RESTORE_WAN_MONTHLY_TRAFFIC_FRAGMENT_TAG";

    protected ImmutableTable.Builder<String, Integer, ArrayList<Double>> traffDataTableBuilder;

    protected ImmutableTable<String, Integer, ArrayList<Double>> traffData;

    private static final String WAN_MONTHLY_TRAFFIC_ACTION = "WAN_MONTHLY_TRAFFIC_ACTION";

    private AtomicBoolean isToggleStateActionRunning = new AtomicBoolean(false);
    private AsyncTaskLoader<NVRAMInfo> mLoader;

    public WANMonthlyTrafficTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, Router router) {
        super(parentFragment, arguments, router, R.layout.tile_status_wan_monthly_traffic, R.id.tile_status_wan_monthly_traffic_togglebutton);
        final TextView monthYearTextViewToDisplay = (TextView) this.layout.findViewById(R.id.tile_status_wan_monthly_month_displayed);
        monthYearTextViewToDisplay.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                final String toDisplay = monthYearTextViewToDisplay.getText().toString();
                final boolean isCurrentMonthYear = SIMPLE_DATE_FORMAT.format(new Date()).equals(toDisplay);

                WANMonthlyTrafficTile.this.layout.findViewById(R.id.tile_status_wan_monthly_traffic_graph_placeholder_current)
                        .setEnabled(!isCurrentMonthYear);
                WANMonthlyTrafficTile.this.layout.findViewById(R.id.tile_status_wan_monthly_traffic_graph_placeholder_next)
                        .setEnabled(!isCurrentMonthYear);
                WANMonthlyTrafficTile.this.layout.findViewById(R.id.tile_status_wan_monthly_traffic_graph_placeholder_display_button)
                        .setVisibility(isNullOrEmpty(toDisplay) ? View.GONE : View.VISIBLE);

            }
        });

        //Create Options Menu
        final ImageButton tileMenu = (ImageButton) layout.findViewById(R.id.tile_status_wan_monthly_traffic_menu);

        if (!ColorUtils.isThemeLight(mParentFragmentActivity)) {
            //Set menu background to white
            tileMenu.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
        }

        final String displayName = String.format("'%s' (%s)",
                router.getDisplayName(), router.getRemoteIpAddress());

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
                            case R.id.tile_wan_monthly_traffic_backup_csv:
                                if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
                                    //Download the full version to unlock this version
                                    Utils.displayUpgradeMessage(mParentFragmentActivity,
                                            "Backup WAN Monthly Traffic Data");
                                    return true;
                                }
                                displayBackupDialog(displayName,
                                        (itemId == R.id.tile_wan_monthly_traffic_backup_csv) ?
                                                BackupFileType.CSV : BackupFileType.RAW);
                                return true;
                            case R.id.tile_wan_monthly_traffic_restore:
                                if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
                                    //Download the full version to unlock this version
                                    Utils.displayUpgradeMessage(mParentFragmentActivity,
                                            "Restore WAN Monthly Traffic Data");
                                    return true;
                                }
                                final FragmentManager supportFragmentManager = mParentFragmentActivity.getSupportFragmentManager();
                                final Fragment restoreWANTraffic = supportFragmentManager
                                        .findFragmentByTag(RESTORE_WAN_MONTHLY_TRAFFIC_FRAGMENT_TAG);
                                if (restoreWANTraffic instanceof DialogFragment) {
                                    ((DialogFragment) restoreWANTraffic).dismiss();
                                }
                                final DialogFragment restoreFragment = RestoreWANMonthlyTrafficDialogFragment
                                        .newInstance(mRouter.getUuid());
                                restoreFragment.show(supportFragmentManager, RESTORE_WAN_MONTHLY_TRAFFIC_FRAGMENT_TAG);

                                return true;
                            case R.id.tile_wan_monthly_traffic_delete:
                                final Bundle token = new Bundle();
                                token.putString(WAN_MONTHLY_TRAFFIC_ACTION, RouterAction.DELETE_WAN_TRAFF.name());

                                new UndoBarController.UndoBar(mParentFragmentActivity)
                                        .message(String.format("Going to erase WAN Monthly Traffic Data on %s...",
                                                displayName))
                                        .listener(WANMonthlyTrafficTile.this)
                                        .token(token)
                                        .show();
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

    public void displayBackupDialog(final String displayName,
                                    @NonNull final BackupFileType backupFileType) {
        final Bundle token = new Bundle();
        token.putString(WAN_MONTHLY_TRAFFIC_ACTION, RouterAction.BACKUP_WAN_TRAFF.name());
        token.putSerializable(WAN_MONTHLY_TRAFFIC_BACKUP_FILETYPE, backupFileType);

        new UndoBarController.UndoBar(mParentFragmentActivity)
                .message(String.format("Backup of WAN Traffic Data (as %s) is going to run on %s...",
                        backupFileType, displayName))
                .listener(WANMonthlyTrafficTile.this)
                .token(token)
                .show();
    }


    private static void setVisibility(@NonNull final View[] views, final int visibility) {
        for (final View view : views) {
            view.setVisibility(visibility);
        }
    }

    @Override
    public int getTileHeaderViewId() {
        return R.id.tile_status_wan_monthly_traffic_hdr;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_status_wan_monthly_traffic_title;
    }

    @Nullable
    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {

        if (nbRunsLoader <= 0 || mAutoRefreshToggle) {
            setLoadingViewVisibility(View.VISIBLE);
        }

        mLoader = new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {

                try {

                    Log.d(LOG_TAG, "Init background loader for " + WANMonthlyTrafficTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (isToggleStateActionRunning.get() || (nbRunsLoader > 0 && !mAutoRefreshToggle)) {
                        //Skip run
                        Log.d(LOG_TAG, "Skip loader run");
                        throw new DDWRTTileAutoRefreshNotAllowedException();
                    }
                    nbRunsLoader++;

                    final NVRAMInfo nvramInfo = new NVRAMInfo();

                    NVRAMInfo nvramInfoTmp = null;
                    try {
                        //noinspection ConstantConditions
                        nvramInfoTmp = NVRAMParser.parseNVRAMOutput(
                                SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                        "/usr/sbin/nvram show 2>/dev/null | grep traff[-_]"));
                    } finally {
                        if (nvramInfoTmp != null) {
                            nvramInfo.putAll(nvramInfoTmp);
                        }

                    }

                    traffDataTableBuilder = ImmutableTable.builder();

                    if (nvramInfo.isEmpty()) {
                        throw new DDWRTNoDataException("No Data!");
                    }

                    @SuppressWarnings("ConstantConditions")
                    final Set<Map.Entry<Object, Object>> entries = nvramInfo.getData().entrySet();

                    final SharedPreferences.Editor editor;
                    if (mParentFragmentPreferences != null) {
                        editor = mParentFragmentPreferences.edit();
                    } else {
                        editor = null;
                    }
                    boolean updatePreferences = false;

                    final Set<String> traffMonthsSet = new HashSet<>();

                    for (final Map.Entry<Object, Object> entry : entries) {
                        final Object key;
                        final Object value;
                        if (entry == null || (key = entry.getKey()) == null || (value = entry.getValue()) == null) {
                            continue;
                        }

                        if (!StringUtils.startsWithIgnoreCase(key.toString(), "traff-")) {
                            continue;
                        }

                        final String month = key.toString().replace("traff-", DDWRTCompanionConstants.EMPTY_STRING);

                        final String monthlyTraffData = value.toString();

                        if (editor != null) {
                            editor.putString(key.toString(), e(monthlyTraffData));
                            traffMonthsSet.add(e(key.toString()));
                            updatePreferences = true;
                        }

                        final List<String> dailyTraffDataList = MONTHLY_TRAFF_DATA_SPLITTER.splitToList(monthlyTraffData);
                        if (dailyTraffDataList == null || dailyTraffDataList.isEmpty()) {
                            continue;
                        }

                        int dayNum = 1;
                        for (final String dailyInOutTraffData : dailyTraffDataList) {
                            if (StringUtils.contains(dailyInOutTraffData, "[")) {
                                continue;
                            }
                            final List<String> dailyInOutTraffDataList = DAILY_TRAFF_DATA_SPLITTER.splitToList(dailyInOutTraffData);
                            if (dailyInOutTraffDataList.size() < 2) {
                                continue;
                            }
                            final String inTraff = dailyInOutTraffDataList.get(0);
                            final String outTraff = dailyInOutTraffDataList.get(1);

                            traffDataTableBuilder.put(month, dayNum++, Lists.newArrayList(
                                    Double.parseDouble(inTraff), Double.parseDouble(outTraff)
                            ));

                        }
                    }

                    if (updatePreferences) {
                        editor
                                .remove(WAN_MONTHLY_TRAFFIC)
                                .apply();

                        editor
                                .putStringSet(WAN_MONTHLY_TRAFFIC, traffMonthsSet)
                                .apply();
                        Utils.requestBackup(mParentFragmentActivity);
                    }

                    traffData = traffDataTableBuilder.build();

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
        //TODO
        return null;
    }

    private void setLoadingViewVisibility(final int visibility) {
        this.layout.findViewById(R.id.tile_status_wan_monthly_month_loading).setVisibility(visibility);
    }

    @Override
    public void onLoadFinished(Loader<NVRAMInfo> loader, NVRAMInfo data) {
        Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data + " / traffData=" + traffData);

        setLoadingViewVisibility(View.GONE);
        layout.findViewById(R.id.tile_status_wan_monthly_traffic_header_loading_view)
                .setVisibility(View.GONE);
        layout.findViewById(R.id.tile_status_wan_monthly_traffic_title).setVisibility(View.VISIBLE);

        Exception preliminaryCheckException = null;
        if (data == null) {
            preliminaryCheckException = new DDWRTNoDataException("No Data!");
        } else //noinspection ThrowableResultOfMethodCallIgnored
            if (data.getException() == null) {
                if (!"1".equals(data.getProperty(NVRAMInfo.TTRAFF_ENABLE))) {
                    preliminaryCheckException = new DDWRTTraffDataDisabled("Traffic monitoring disabled!");
                } else if (traffData == null || traffData.isEmpty()) {
                    preliminaryCheckException = new DDWRTNoDataException("No Traffic Data!");
                }
            }

        final SwitchCompat enableTraffDataButton =
                (SwitchCompat) this.layout.findViewById(R.id.tile_status_wan_monthly_traffic_status);
        enableTraffDataButton.setVisibility(View.VISIBLE);

        final boolean makeToogleEnabled = (data != null && data.getData() != null && data.getData().containsKey(NVRAMInfo.TTRAFF_ENABLE));

        if (!isToggleStateActionRunning.get()) {
            if (makeToogleEnabled) {
                if ("1".equals(data.getProperty(NVRAMInfo.TTRAFF_ENABLE))) {
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

        final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_wan_monthly_traffic_error);

        final Exception exception = data.getException();

        final View displayButton = this.layout.findViewById(R.id.tile_status_wan_monthly_traffic_graph_placeholder_display_button);
        final View currentButton = this.layout.findViewById(R.id.tile_status_wan_monthly_traffic_graph_placeholder_current);
        final View previousButton = this.layout.findViewById(R.id.tile_status_wan_monthly_traffic_graph_placeholder_previous);
        final View nextButton = this.layout.findViewById(R.id.tile_status_wan_monthly_traffic_graph_placeholder_next);
        final TextView monthYearDisplayed = (TextView) this.layout.findViewById(R.id.tile_status_wan_monthly_month_displayed);

        final View[] ctrlViews = new View[]{monthYearDisplayed, displayButton, currentButton, previousButton, nextButton};

        //Create Options Menu
        final ImageButton tileMenu = (ImageButton) layout.findViewById(R.id.tile_status_wan_monthly_traffic_menu);

        if (!ColorUtils.isThemeLight(mParentFragmentActivity)) {
            //Set menu background to white
            tileMenu.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
        }

        if (exception == null) {
            errorPlaceHolderView.setVisibility(View.GONE);

            tileMenu.setVisibility(View.VISIBLE);

            final String currentMonthYearAlreadyDisplayed = monthYearDisplayed.getText().toString();

            final Date currentDate = new Date();
            final String currentMonthYear = (isNullOrEmpty(currentMonthYearAlreadyDisplayed) ?
                    SIMPLE_DATE_FORMAT.format(currentDate) : currentMonthYearAlreadyDisplayed);

            //TODO Load last value from preferences
            monthYearDisplayed.setText(currentMonthYear);

            displayButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    final CharSequence monthYearDisplayedText = monthYearDisplayed.getText();

                    final String monthFormatted = monthYearDisplayedText.toString();
                    final ImmutableMap<Integer, ArrayList<Double>> traffDataForMonth =
                            getTraffDataForMonth(monthFormatted);

                    if (traffDataForMonth == null || traffDataForMonth.isEmpty()) {
                        Toast.makeText(WANMonthlyTrafficTile.this.mParentFragmentActivity,
                                String.format("No traffic data for '%s'", monthYearDisplayedText), Toast.LENGTH_SHORT).show();
                    } else {
                        final Intent intent = new Intent(mParentFragmentActivity, WANMonthlyTrafficActivity.class);
                        intent.putExtra(RouterManagementActivity.ROUTER_SELECTED,
                                mRouter != null ? mRouter.getRemoteIpAddress() : DDWRTCompanionConstants.EMPTY_STRING);
                        intent.putExtra(WANMonthlyTrafficActivity.MONTH_DISPLAYED, monthFormatted);
                        intent.putExtra(WANMonthlyTrafficActivity.MONTHLY_TRAFFIC_DATA_UNSORTED, traffDataForMonth);

                        //noinspection ConstantConditions
                        final AlertDialog alertDialog = Utils.buildAlertDialog(mParentFragmentActivity, null,
                                String.format("Loading traffic data for '%s'", monthYearDisplayedText), false, false);
                        alertDialog.show();
                        ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mParentFragmentActivity.startActivity(intent);
                                alertDialog.cancel();
                            }
                        }, 1000);
                    }

                }
            });

            currentButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    monthYearDisplayed.setText(SIMPLE_DATE_FORMAT.format(currentDate));
                }
            });

            previousButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final int[] currentYearMonth = getCurrentYearAndMonth(currentDate, monthYearDisplayed.getText().toString());
                    if (currentYearMonth.length < 2) {
                        return;
                    }

                    final int currentMonth = currentYearMonth[1];
                    final int currentYear = currentYearMonth[0];

                    final int previousMonth = currentMonth - 1;
                    final String previousMonthYear = ((previousMonth <= 0) ? ("12-" + (currentYear - 1)) :
                            (((previousMonth <= 9) ? ("0" + previousMonth) : previousMonth) + "-" + currentYear));

                    monthYearDisplayed.setText(previousMonthYear);
                }
            });

            nextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    final int[] currentYearMonth = getCurrentYearAndMonth(currentDate, monthYearDisplayed.getText().toString());
                    if (currentYearMonth.length < 2) {
                        return;
                    }

                    final int currentMonth = currentYearMonth[1];
                    final int currentYear = currentYearMonth[0];
                    final int nextMonth = currentMonth + 1;
                    final String nextMonthYear = ((nextMonth >= 13) ? ("01-" + (currentYear + 1)) :
                            (((nextMonth <= 9) ? ("0" + nextMonth) : nextMonth) + "-" + currentYear));

                    monthYearDisplayed.setText(nextMonthYear);
                }
            });

            setVisibility(ctrlViews, View.VISIBLE);
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
            setVisibility(ctrlViews, View.GONE);

            tileMenu.setVisibility(View.GONE);

        } else {
            if (traffData == null || traffData.isEmpty()) {
                errorPlaceHolderView.setText("Error: No Data!");
                errorPlaceHolderView.setVisibility(View.VISIBLE);
                setVisibility(ctrlViews, View.GONE);
                tileMenu.setVisibility(View.GONE);
            }
        }

        doneWithLoaderInstance(this, loader,
                R.id.tile_status_wan_monthly_traffic_togglebutton_title, R.id.tile_status_wan_monthly_traffic_togglebutton_separator);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");

    }

    @NonNull
    private int[] getCurrentYearAndMonth(final Date currentDate, final String monthYearDisplayed) {
        final int[] currentYearAndMonth = new int[2];

        String monthDisplayed = null;
        String yearDisplayed = null;
        final List<String> monthYearTextViewSplit = Splitter.on("-").omitEmptyStrings().splitToList(monthYearDisplayed);
        if (monthYearTextViewSplit.size() >= 2) {
            monthDisplayed = monthYearTextViewSplit.get(0);
            yearDisplayed = monthYearTextViewSplit.get(1);
        }

        currentYearAndMonth[0] = Integer.parseInt(isNullOrEmpty(yearDisplayed) ?
                new SimpleDateFormat("yyyy", Locale.US).format(currentDate) : yearDisplayed);
        currentYearAndMonth[1] = Integer.parseInt(isNullOrEmpty(monthDisplayed) ?
                new SimpleDateFormat("MM", Locale.US).format(currentDate) : monthDisplayed);

        return currentYearAndMonth;
    }

    @Nullable
    private ImmutableMap<Integer, ArrayList<Double>> getTraffDataForMonth(@NonNull final String monthFormatted) {
        Log.d(LOG_TAG, "getTraffDataForMonth: " + monthFormatted);

        if (traffData == null) {
            return null;
        }

        return traffData.row(monthFormatted);
    }

    @Override
    public void onHide(@Nullable Parcelable parcelable) {
        if (parcelable instanceof Bundle) {
            final Bundle token = (Bundle) parcelable;
            final String routerAction = token.getString(WAN_MONTHLY_TRAFFIC_ACTION);
            Log.d(LOG_TAG, "WAN Monthly Traffic Data Action: [" + routerAction + "]");
            if (isNullOrEmpty(routerAction)) {
                return;
            }
            try {
                switch (RouterAction.valueOf(routerAction)) {
                    case DELETE_WAN_TRAFF: {
                        final AlertDialog alertDialog = Utils.
                                buildAlertDialog(mParentFragmentActivity,
                                        null, "Erasing WAN Traffic Data - please hold on...", false, false);
                        alertDialog.show();
                        ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                new EraseWANMonthlyTrafficRouterAction(mParentFragmentActivity,
                                        new RouterActionListener() {
                                            @Override
                                            public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router, Object returnData) {
                                                try {
                                                    WANMonthlyTrafficTile.this.onRouterActionSuccess(routerAction, router, returnData);
                                                }  finally {
                                                    mParentFragmentActivity.runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            alertDialog.cancel();
                                                        }
                                                    });
                                                }
                                            }

                                            @Override
                                            public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull Router router, @Nullable Exception exception) {
                                                try {
                                                    WANMonthlyTrafficTile.this.onRouterActionFailure(routerAction, router, exception);
                                                }  finally {
                                                    mParentFragmentActivity.runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            alertDialog.cancel();
                                                        }
                                                    });
                                                }

                                            }
                                        }, mGlobalPreferences)
                                        .execute(mRouter);
                            }
                        }, 1500l);
                    }
                        return;
                    case BACKUP_WAN_TRAFF:
                        final BackupFileType fileType =
                                (BackupFileType) token.getSerializable(WAN_MONTHLY_TRAFFIC_BACKUP_FILETYPE);
                        final AlertDialog alertDialog = Utils.
                                buildAlertDialog(mParentFragmentActivity,
                                        null, "Backing up WAN Traffic Data - please hold on...", false, false);
                        alertDialog.show();
                        ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                new BackupWANMonthlyTrafficRouterAction(fileType, mParentFragmentActivity,
                                        new RouterActionListener() {

                                            @Override
                                            public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router, Object returnData) {
                                                try {
                                                    String msg;
                                                    if (!((returnData instanceof Object[]) &&
                                                            ((Object[]) returnData).length >= 2)) {
                                                        msg = String.format("Action '%s' executed " +
                                                                        "successfully on host '%s', but an internal error occurred. " +
                                                                        "The issue will be reported. Please try again later.",
                                                                routerAction.toString(),
                                                                router.getRemoteIpAddress());
                                                        Utils.displayMessage(mParentFragmentActivity,
                                                                msg,
                                                                Style.INFO);
                                                        Utils.reportException(new IllegalStateException(msg));
                                                        return;
                                                    }

                                                    final Object[] returnDataObjectArray = ((Object[]) returnData);
                                                    final Object backupDateObject = returnDataObjectArray[0];
                                                    final Object localBackupFileObject = returnDataObjectArray[1];

                                                    if (!((backupDateObject instanceof Date) &&
                                                            (localBackupFileObject instanceof File))) {
                                                        msg = String.format("Action '%s' executed " +
                                                                        "successfully on host '%s', but could not determine where " +
                                                                        "local backup file has been saved. Please try again later.",
                                                                routerAction.toString(),
                                                                router.getRemoteIpAddress());
                                                        Utils.displayMessage(mParentFragmentActivity,
                                                                msg,
                                                                Style.INFO);
                                                        Utils.reportException(new IllegalStateException(msg));
                                                        return;
                                                    }

                                                    Utils.displayMessage(mParentFragmentActivity,
                                                            String.format("Action '%s' executed successfully on host '%s'. " +
                                                                            "Now loading the file sharing activity chooser...",
                                                                    routerAction.toString(), router.getRemoteIpAddress()),
                                                            Style.CONFIRM);

                                                    final File localBackupFile = (File) (((Object[]) returnData)[1]);
                                                    final Date backupDate = (Date) (((Object[]) returnData)[0]);

                                                    final Uri uriForFile = FileProvider.getUriForFile(mParentFragmentActivity,
                                                            DDWRTCompanionConstants.FILEPROVIDER_AUTHORITY,
                                                            localBackupFile);
                                                    mParentFragmentActivity.grantUriPermission(
                                                            mParentFragmentActivity.getPackageName(),
                                                            uriForFile, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                                                    final Intent shareIntent = new Intent();
                                                    shareIntent.setAction(Intent.ACTION_SEND);
                                                    shareIntent.putExtra(Intent.EXTRA_SUBJECT,
                                                            String.format("Backup of WAN Monthly Traffic on Router '%s' (%s)",
                                                                    mRouter.getDisplayName(), mRouter.getRemoteIpAddress()));
                                                    shareIntent.setType("text/html");
                                                    shareIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(
                                                            ("Backup Date: " + backupDate + "\n\n").replaceAll("\n", "<br/>") +
                                                                    Utils.getShareIntentFooter()));
                                                    shareIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);
                                                    mParentFragmentActivity.startActivity(Intent.createChooser(shareIntent,
                                                            mParentFragmentActivity.getResources().getText(R.string.share_backup)));

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
                                            public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull Router router, @Nullable Exception exception) {
                                                try {
                                                    Utils.displayMessage(mParentFragmentActivity,
                                                            String.format("Error on action '%s': %s",
                                                                    routerAction.toString(),
                                                                    ExceptionUtils.getRootCauseMessage(exception)),
                                                            Style.ALERT);
                                                } finally {
                                                    mParentFragmentActivity.runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            alertDialog.cancel();
                                                        }
                                                    });
                                                }
                                            }
                                        }, mGlobalPreferences)
                                        .execute(mRouter);
                            }
                        }, 1500);
                        return;
                    default:
                        break;

                }
            } catch (IllegalArgumentException | NullPointerException e) {
                e.printStackTrace();
                Utils.reportException(e);
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
    public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router, Object returnData) {
        Utils.displayMessage(mParentFragmentActivity,
                String.format("Action '%s' executed successfully on host '%s'", routerAction.toString(), router.getRemoteIpAddress()),
                Style.CONFIRM);
    }

    @Override
    public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull Router router, @Nullable Exception exception) {
        Utils.displayMessage(mParentFragmentActivity,
                String.format("Error on action '%s': %s", routerAction.toString(), ExceptionUtils.getRootCauseMessage(exception)),
                Style.ALERT);
    }

    private class DDWRTTraffDataDisabled extends DDWRTNoDataException {

        public DDWRTTraffDataDisabled(@Nullable String detailMessage) {
            super(detailMessage);
        }

    }

    private class ManageWANTrafficCounterToggle implements  View.OnClickListener {

        private boolean enable;

        @Override
        public void onClick(View view) {

            isToggleStateActionRunning.set(true);

            if (!(view instanceof CompoundButton)) {
                Utils.reportException(new IllegalStateException("ManageWANTrafficCounterToggle#onClick: " +
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

            final NVRAMInfo nvramInfoToSet = new NVRAMInfo();

            nvramInfoToSet.setProperty(NVRAMInfo.TTRAFF_ENABLE, enable ? "1" : "0");

            //Also set traff data loaded from preferences, if any
            if (enable && mParentFragmentPreferences != null) {
                //Also restore traffic data we had in preferences
                final Set<String> traffMonths = FluentIterable
                        .from(
                                Optional
                                        .fromNullable(
                                                mParentFragmentPreferences
                                                        .getStringSet(WAN_MONTHLY_TRAFFIC,
                                                                new HashSet<String>())
                                        )
                                        .or(new HashSet<String>())
                        )
                        .transform(new Function<String, String>() {
                            @Override
                            public String apply(@Nullable String input) {
                                return d(input);
                            }
                        }).toSet();

                for (final String traffMonth : traffMonths) {
                    if (traffMonth == null || traffMonth.isEmpty()) {
                        continue;
                    }
                    final String traffMonthDataSaved = d(
                            mParentFragmentPreferences
                                    .getString(traffMonth, null));
                    if (traffMonthDataSaved == null || traffMonthDataSaved.isEmpty()) {
                        continue;
                    }
                    nvramInfoToSet.setProperty(traffMonth, traffMonthDataSaved);
                }
            }

            new UndoBarController.UndoBar(mParentFragmentActivity)
                    .message(String.format("WAN Traffic Counter will be %s on '%s' (%s). ",
//                                    "Router will be rebooted at the end of the operation.",
                            enable ? "enabled" : "disabled",
                            mRouter.getDisplayName(),
                            mRouter.getRemoteIpAddress()))
                    .listener(new UndoBarController.AdvancedUndoListener() {
                                  @Override
                                  public void onHide(@Nullable Parcelable parcelable) {

                                      Utils.displayMessage(mParentFragmentActivity,
                                              String.format("%s WAN Traffic Counter...",
                                                      enable ? "Enabling" : "Disabling"),
                                              Style.INFO);

                                      new SetNVRAMVariablesAction(mParentFragmentActivity,
                                              nvramInfoToSet,
                                              false,
                                              new RouterActionListener() {
                                                  @Override
                                                  public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull final Router router, Object returnData) {
                                                      mParentFragmentActivity.runOnUiThread(new Runnable() {
                                                          @Override
                                                          public void run() {
                                                              try {

                                                                  compoundButton.setChecked(enable);
                                                                  Utils.displayMessage(mParentFragmentActivity,
                                                                          String.format("WAN Traffic Counter %s successfully on host '%s' (%s)",
                                                                                  enable ? "enabled" : "disabled",
                                                                                  router.getDisplayName(),
                                                                                  router.getRemoteIpAddress()),
                                                                          Style.CONFIRM);
                                                              }

                                                              finally
                                                              {
                                                                  compoundButton.setEnabled(true);
                                                                  isToggleStateActionRunning.set(false);
                                                                  if (mLoader != null) {
                                                                      //Reload everything right away
                                                                      doneWithLoaderInstance(WANMonthlyTrafficTile.this,
                                                                              mLoader,
                                                                              1l,
                                                                              R.id.tile_status_wan_monthly_traffic_togglebutton_title,
                                                                              R.id.tile_status_wan_monthly_traffic_togglebutton_separator);
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
                                                                          String.format("Error while trying to %s WAN Traffic Counter on '%s' (%s): %s",
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
