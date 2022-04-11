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
 * Contact Info: Armel Soro <armel+router_companion@rm3l.org>
 */

package org.rm3l.router_companion.tiles.status.wan

import android.Manifest
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CompoundButton
import android.widget.DatePicker
import android.widget.ImageButton
import android.widget.NumberPicker
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.FileProvider
import androidx.core.content.PermissionChecker
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.loader.content.AsyncTaskLoader
import androidx.loader.content.Loader
import com.github.curioustechizen.ago.RelativeTimeTextView
import com.google.android.material.snackbar.Snackbar
import com.google.common.base.Optional
import com.google.common.base.Strings.isNullOrEmpty
import com.google.common.base.Throwables
import com.google.common.collect.FluentIterable
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import org.rm3l.ddwrt.BuildConfig
import org.rm3l.ddwrt.R
import org.rm3l.router_companion.RouterCompanionAppConstants
import org.rm3l.router_companion.RouterCompanionAppConstants.EMPTY_STRING
import org.rm3l.router_companion.RouterCompanionAppConstants.WAN_CYCLE_DAY_PREF
import org.rm3l.router_companion.actions.ActionManager
import org.rm3l.router_companion.actions.BackupWANMonthlyTrafficRouterAction
import org.rm3l.router_companion.actions.EraseWANMonthlyTrafficRouterAction
import org.rm3l.router_companion.actions.RouterAction
import org.rm3l.router_companion.actions.RouterActionListener
import org.rm3l.router_companion.actions.SetNVRAMVariablesAction
import org.rm3l.router_companion.exceptions.DDWRTNoDataException
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException
import org.rm3l.router_companion.mgmt.RouterManagementActivity
import org.rm3l.router_companion.mgmt.dao.DDWRTCompanionDAO
import org.rm3l.router_companion.resources.Encrypted.d
import org.rm3l.router_companion.resources.MonthlyCycleItem
import org.rm3l.router_companion.resources.WANTrafficData
import org.rm3l.router_companion.resources.conn.NVRAMInfo
import org.rm3l.router_companion.resources.conn.Router
import org.rm3l.router_companion.tiles.DDWRTTile
import org.rm3l.router_companion.utils.ColorUtils
import org.rm3l.router_companion.utils.SSHUtils
import org.rm3l.router_companion.utils.Utils
import org.rm3l.router_companion.utils.Utils.fromHtml
import org.rm3l.router_companion.utils.WANTrafficUtils
import org.rm3l.router_companion.utils.WANTrafficUtils.HIDDEN_
import org.rm3l.router_companion.utils.WANTrafficUtils.TOTAL_DL_CURRENT_MONTH
import org.rm3l.router_companion.utils.WANTrafficUtils.TOTAL_DL_CURRENT_MONTH_MB
import org.rm3l.router_companion.utils.WANTrafficUtils.TOTAL_UL_CURRENT_MONTH
import org.rm3l.router_companion.utils.WANTrafficUtils.TOTAL_UL_CURRENT_MONTH_MB
import org.rm3l.router_companion.utils.WANTrafficUtils.getTrafficDataNvramInfoAndPersistIfNeeded
import org.rm3l.router_companion.utils.snackbar.SnackbarCallback
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style
import org.rm3l.router_companion.widgets.AbstractDatePickerListener
import org.rm3l.router_companion.widgets.DatePickerFragment
import java.io.File
import java.util.Calendar
import java.util.Date
import java.util.HashSet
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class WANMonthlyTrafficTile(
    parentFragment: Fragment,
    arguments: Bundle?,
    router: Router?
) : DDWRTTile<NVRAMInfo>(parentFragment, arguments, router, R.layout.tile_status_wan_monthly_traffic, null),
    SnackbarCallback,
    RouterActionListener {

    private val dao: DDWRTCompanionDAO = RouterManagementActivity.getDao(mParentFragmentActivity)

    private val isToggleStateActionRunning = AtomicBoolean(false)

    private val mCurrentCycle: AtomicReference<MonthlyCycleItem>

    private var mCycleOfTheDay: MonthlyCycleItem

    private val mGson: Gson

    private var mIsThemeLight: Boolean = false

    private var mLastSync: Long = 0

    private var mLoader: AsyncTaskLoader<NVRAMInfo>? = null

    class DDWRTTraffDataDisabled(detailMessage: String?) : DDWRTNoDataException(detailMessage)

    private inner class ManageWANTrafficCounterToggle : View.OnClickListener {

        private var enable: Boolean = false

        override fun onClick(view: View) {

            isToggleStateActionRunning.set(true)

            if (view !is CompoundButton) {
                Utils.reportException(
                    null,
                    IllegalStateException(
                        "ManageWANTrafficCounterToggle#onClick: " + "view is NOT an instance of CompoundButton!"
                    )
                )
                isToggleStateActionRunning.set(false)
                return
            }

            mParentFragmentActivity.runOnUiThread { view.isEnabled = false }

            this.enable = view.isChecked

            val nvramInfoToSet = NVRAMInfo()

            nvramInfoToSet.setProperty(NVRAMInfo.TTRAFF_ENABLE, if (enable) "1" else "0")

            // Also set traff data loaded from preferences, if any
            if (enable && mParentFragmentPreferences != null) {
                // Also restore traffic data we had in preferences
                val traffMonths = FluentIterable.from(
                    Optional.fromNullable(
                        mParentFragmentPreferences.getStringSet(WAN_MONTHLY_TRAFFIC, HashSet())
                    )
                        .or(HashSet())
                ).transform { input -> d(input) }.toSet()

                for (traffMonth in traffMonths) {
                    if (traffMonth == null || traffMonth.isEmpty()) {
                        continue
                    }
                    val traffMonthDataSaved = d(mParentFragmentPreferences.getString(traffMonth, null))
                    if (traffMonthDataSaved == null || traffMonthDataSaved.isEmpty()) {
                        continue
                    }
                    nvramInfoToSet.setProperty(traffMonth, traffMonthDataSaved)
                }
            }

            SnackbarUtils.buildSnackbar(
                mParentFragmentActivity,
                String.format(
                    "WAN Traffic Counter will be %s on '%s' (%s). ",
                    //                                    "Router will be rebooted at the end of the operation.",
                    if (enable) "enabled" else "disabled", mRouter!!.displayName,
                    mRouter.remoteIpAddress
                ),
                "CANCEL", Snackbar.LENGTH_LONG,
                object : SnackbarCallback {
                    @Throws(Exception::class)
                    override fun onDismissEventActionClick(event: Int, bundle: Bundle?) {
                        cancel()
                    }

                    @Throws(Exception::class)
                    override fun onDismissEventConsecutive(event: Int, bundle: Bundle?) {
                        cancel()
                    }

                    @Throws(Exception::class)
                    override fun onDismissEventManual(event: Int, bundle: Bundle?) {
                        cancel()
                    }

                    @Throws(Exception::class)
                    override fun onDismissEventSwipe(event: Int, bundle: Bundle?) {
                        cancel()
                    }

                    @Throws(Exception::class)
                    override fun onDismissEventTimeout(event: Int, bundle: Bundle?) {
                        Utils.displayMessage(
                            mParentFragmentActivity,
                            String.format("%s WAN Traffic Counter...", if (enable) "Enabling" else "Disabling"),
                            Style.INFO
                        )

                        ActionManager.runTasks(
                            SetNVRAMVariablesAction(
                                mRouter, mParentFragmentActivity, nvramInfoToSet,
                                false,
                                object : RouterActionListener {
                                    override fun onRouterActionFailure(
                                        routerAction: RouterAction,
                                        router: Router,
                                        exception: Exception?
                                    ) {
                                        mParentFragmentActivity.runOnUiThread {
                                            try {
                                                view.isChecked = !enable
                                                Utils.displayMessage(
                                                    mParentFragmentActivity,
                                                    String.format(
                                                        "Error while trying to %s WAN Traffic Counter on '%s' (%s): %s",
                                                        if (enable) "enable" else "disable",
                                                        router.displayName,
                                                        router.remoteIpAddress,
                                                        Utils.handleException(
                                                            exception
                                                        ).first
                                                    ),
                                                    Style.ALERT
                                                )
                                            } finally {
                                                view.isEnabled = true
                                                isToggleStateActionRunning.set(false)
                                            }
                                        }
                                    }

                                    override fun onRouterActionSuccess(
                                        routerAction: RouterAction,
                                        router: Router,
                                        returnData: Any
                                    ) {
                                        mParentFragmentActivity.runOnUiThread {
                                            try {

                                                view.isChecked = enable
                                                Utils.displayMessage(
                                                    mParentFragmentActivity,
                                                    String.format(
                                                        "WAN Traffic Counter %s successfully on host '%s' (%s)",
                                                        if (enable) "enabled" else "disabled",
                                                        router.displayName,
                                                        router.remoteIpAddress
                                                    ),
                                                    Style.CONFIRM
                                                )
                                            } finally {
                                                view.isEnabled = true
                                                isToggleStateActionRunning.set(false)
                                                if (mLoader != null) {
                                                    // Reload everything right away
                                                    doneWithLoaderInstance(
                                                        this@WANMonthlyTrafficTile,
                                                        mLoader as AsyncTaskLoader<NVRAMInfo>, 1L
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                mGlobalPreferences
                            )
                        )
                    }

                    private fun cancel() {
                        mParentFragmentActivity.runOnUiThread {
                            try {
                                view.isChecked = !enable
                                view.isEnabled = true
                            } finally {
                                isToggleStateActionRunning.set(false)
                            }
                        }
                    }
                },
                Bundle(), true
            )

            // new UndoBarController.UndoBar(mParentFragmentActivity).message(
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
            // ).
            //
            //    token(new Bundle()
            //
            //    ).
            //
            //    show();
        }
    }

    init {

        mIsThemeLight = ColorUtils.isThemeLight(mParentFragmentActivity)

        mGson = Gson()

        // Initialize w/ current cycle
        mCycleOfTheDay = WANTrafficData.getCurrentWANCycle(mParentFragmentActivity, mParentFragmentPreferences)

        mCurrentCycle = AtomicReference(mCycleOfTheDay)

        val monthYearTextViewToDisplay = this.layout.findViewById<View>(R.id.tile_status_wan_monthly_month_displayed) as TextView
        val dateFromPickerButton = this.layout.findViewById<Button>(R.id.tile_status_wan_monthly_traffic_graph_placeholder_date_from_picker)
        val dateToPickerButton = this.layout.findViewById<Button>(R.id.tile_status_wan_monthly_traffic_graph_placeholder_date_to_picker)
        monthYearTextViewToDisplay.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {

                val currentCycleItem = mCurrentCycle.get() ?: return

                dateFromPickerButton.text = DateUtils.formatDateTime(
                    mParentFragmentActivity,
                    currentCycleItem.start, DateUtils.FORMAT_ABBREV_MONTH
                )
                dateToPickerButton.text = DateUtils.formatDateTime(
                    mParentFragmentActivity,
                    currentCycleItem.end, DateUtils.FORMAT_ABBREV_MONTH
                )

                val data = WANTrafficUtils.computeWANTrafficUsageBetweenDates(
                    dao, mRouter!!.uuid,
                    currentCycleItem.start, currentCycleItem.end
                )

                //                final boolean isCurrentMonthYear = mCycleOfTheDay.equals(currentCycleItem);

                this@WANMonthlyTrafficTile.layout.findViewById<View>(
                    R.id.tile_status_wan_monthly_traffic_graph_placeholder_current
                ).isEnabled = true
                this@WANMonthlyTrafficTile.layout.findViewById<View>(
                    R.id.tile_status_wan_monthly_traffic_graph_placeholder_date_to_picker
                ).isEnabled = true

                // Display traffic data for this month
                if (data.isEmpty) {
                    Toast.makeText(
                        this@WANMonthlyTrafficTile.mParentFragmentActivity,
                        String.format(
                            "No traffic data for '%s'. Please try again later.",
                            currentCycleItem.getLabelWithYears()
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                val dlDrawable: Int
                val ulDrawable: Int
                if (mIsThemeLight) {
                    dlDrawable = R.drawable.ic_dl_dark
                    ulDrawable = R.drawable.ic_ul_dark
                } else {
                    dlDrawable = R.drawable.ic_dl_white
                    ulDrawable = R.drawable.ic_ul_light
                }

                val wanDLView = layout.findViewById<View>(R.id.tile_status_wan_monthly_traffic_dl) as TextView
                wanDLView.setCompoundDrawablesWithIntrinsicBounds(dlDrawable, 0, 0, 0)
                wanDLView.text = data.getProperty(TOTAL_DL_CURRENT_MONTH, "-")

                val wanULView = layout.findViewById<View>(R.id.tile_status_wan_monthly_traffic_ul) as TextView
                wanULView.setCompoundDrawablesWithIntrinsicBounds(ulDrawable, 0, 0, 0)
                wanULView.text = data.getProperty(TOTAL_UL_CURRENT_MONTH, "-")

                val dlMB = layout.findViewById<View>(R.id.tile_status_wan_monthly_traffic_dl_mb) as TextView
                val dlMBytesFromNvram = data.getProperty(TOTAL_DL_CURRENT_MONTH_MB)
                if (HIDDEN_ == dlMBytesFromNvram) {
                    dlMB.visibility = View.INVISIBLE
                } else {
                    dlMB.visibility = View.VISIBLE
                }
                dlMB.text = if (dlMBytesFromNvram != null) "($dlMBytesFromNvram MB)" else "-"

                val ulMB = layout.findViewById<View>(R.id.tile_status_wan_monthly_traffic_ul_mb) as TextView
                val ulMBytesFromNvram = data.getProperty(TOTAL_UL_CURRENT_MONTH_MB)
                if (HIDDEN_ == ulMBytesFromNvram) {
                    ulMB.visibility = View.INVISIBLE
                } else {
                    ulMB.visibility = View.VISIBLE
                }
                ulMB.text = if (ulMBytesFromNvram != null) "($ulMBytesFromNvram MB)" else "-"
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })

        // Create Options Menu
        val tileMenu = layout.findViewById<View>(R.id.tile_status_wan_monthly_traffic_menu) as ImageButton

        if (!mIsThemeLight) {
            // Set menu background to white
            tileMenu.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark)
        }

        // Permission requests
        val rwExternalStoragePermissionCheck = PermissionChecker.checkSelfPermission(
            mParentFragmentActivity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (rwExternalStoragePermissionCheck != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    mParentFragmentActivity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                SnackbarUtils.buildSnackbar(
                    mParentFragmentActivity,
                    "Storage access is required to be able to backup and restore WAN traffic data.", "OK",
                    Snackbar.LENGTH_INDEFINITE,
                    object : SnackbarCallback {
                        @Throws(Exception::class)
                        override fun onDismissEventActionClick(event: Int, bundle: Bundle?) {
                            // Request permission
                            ActivityCompat.requestPermissions(
                                mParentFragmentActivity,
                                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                RouterCompanionAppConstants.Permissions.STORAGE
                            )
                        }
                    },
                    null, true
                )
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    mParentFragmentActivity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    RouterCompanionAppConstants.Permissions.STORAGE
                )
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        val displayName = String.format("'%s' (%s)", router?.displayName, router?.remoteIpAddress)

        tileMenu.setOnClickListener(
            View.OnClickListener { v ->
                val popup = PopupMenu(mParentFragmentActivity, v)
                popup.setOnMenuItemClickListener(
                    PopupMenu.OnMenuItemClickListener { menuItem ->
                        val itemId = menuItem.itemId
                        // Store current value in preferences
                        when (itemId) {
                            R.id.tile_wan_monthly_traffic_backup_raw -> {
                                if (PermissionChecker.checkSelfPermission(
                                        mParentFragmentActivity,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    Utils.displayMessage(
                                        mParentFragmentActivity, "Storage access required",
                                        Style.ALERT
                                    )
                                    return@OnMenuItemClickListener false
                                }
                                // Allowed for all
                                displayBackupDialog(
                                    displayName,
                                    BackupWANMonthlyTrafficRouterAction.BackupFileType_RAW
                                )
                                return@OnMenuItemClickListener true
                            }
                            R.id.tile_wan_monthly_traffic_backup_csv -> {
                                if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
                                    // Download the full version to unlock this version
                                    Utils.displayUpgradeMessage(
                                        mParentFragmentActivity,
                                        "Backup WAN Traffic Data as CSV"
                                    )
                                    return@OnMenuItemClickListener true
                                }
                                if (PermissionChecker.checkSelfPermission(
                                        mParentFragmentActivity,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    Utils.displayMessage(
                                        mParentFragmentActivity, "Storage access required",
                                        Style.ALERT
                                    )
                                    return@OnMenuItemClickListener false
                                }
                                displayBackupDialog(
                                    displayName,
                                    BackupWANMonthlyTrafficRouterAction.BackupFileType_CSV
                                )
                                return@OnMenuItemClickListener true
                            }
                            R.id.tile_wan_monthly_traffic_restore -> {
                                if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
                                    // Download the full version to unlock this version
                                    Utils.displayUpgradeMessage(
                                        mParentFragmentActivity,
                                        "Restore WAN Monthly Traffic Data"
                                    )
                                    return@OnMenuItemClickListener true
                                }

                                if (PermissionChecker.checkSelfPermission(
                                        mParentFragmentActivity,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    Utils.displayMessage(
                                        mParentFragmentActivity, "Storage access required",
                                        Style.ALERT
                                    )
                                    return@OnMenuItemClickListener false
                                }

                                val supportFragmentManager = mParentFragmentActivity.supportFragmentManager
                                val restoreWANTraffic = supportFragmentManager.findFragmentByTag(
                                    RESTORE_WAN_MONTHLY_TRAFFIC_FRAGMENT_TAG
                                )
                                (restoreWANTraffic as? DialogFragment)?.dismiss()
                                val restoreFragment = RestoreWANMonthlyTrafficDialogFragment.newInstance(mRouter!!.uuid)
                                restoreFragment.show(
                                    supportFragmentManager,
                                    RESTORE_WAN_MONTHLY_TRAFFIC_FRAGMENT_TAG
                                )

                                return@OnMenuItemClickListener true
                            }
                            R.id.tile_wan_monthly_traffic_delete -> {
                                val token = Bundle()
                                token.putString(WAN_MONTHLY_TRAFFIC_ACTION, RouterAction.DELETE_WAN_TRAFF.name)

                                SnackbarUtils.buildSnackbar(
                                    mParentFragmentActivity,
                                    String.format(
                                        "Going to erase WAN Monthly Traffic Data on %s...",
                                        displayName
                                    ),
                                    "CANCEL",
                                    Snackbar.LENGTH_LONG,
                                    this@WANMonthlyTrafficTile,
                                    token, true
                                )

                                // new UndoBarController.UndoBar(mParentFragmentActivity).message(
                                //    String.format("Going to erase WAN Monthly Traffic Data on %s...", displayName))
                                //    .listener(WANMonthlyTrafficTile.this)
                                //    .token(token)
                                //    .show();
                                return@OnMenuItemClickListener true
                            }
                            R.id.tile_wan_monthly_traffic_change_cycle -> {
                                val builder = androidx.appcompat.app.AlertDialog.Builder(mParentFragmentActivity)
                                val dialogInflater = LayoutInflater.from(builder.context)

                                val view = dialogInflater.inflate(R.layout.data_usage_cycle_editor, null, false)
                                val cycleDayPicker = view.findViewById<View>(R.id.wan_cycle_day) as NumberPicker

                                val wanCycleDay: Int
                                if (mParentFragmentPreferences != null) {
                                    val cycleDay = mParentFragmentPreferences.getInt(WAN_CYCLE_DAY_PREF, 1)
                                    wanCycleDay = if (cycleDay < 1) 1 else if (cycleDay > 31) 31 else cycleDay
                                } else {
                                    wanCycleDay = 1
                                }

                                cycleDayPicker.minValue = 1
                                cycleDayPicker.maxValue = 31
                                cycleDayPicker.value = wanCycleDay
                                cycleDayPicker.wrapSelectorWheel = true

                                builder.setTitle(R.string.data_usage_cycle_editor_title)
                                builder.setView(view)

                                builder.setCancelable(true)

                                builder.setPositiveButton(
                                    R.string.data_usage_cycle_editor_positive,
                                    DialogInterface.OnClickListener { dialog, which ->
                                        // clear focus to finish pending text edits
                                        cycleDayPicker.clearFocus()

                                        val wanCycleDay = cycleDayPicker.value

                                        // Update preferences
                                        if (mParentFragmentPreferences == null) {
                                            return@OnClickListener
                                        }
                                        mParentFragmentPreferences.edit()
                                            .putInt(WAN_CYCLE_DAY_PREF, wanCycleDay)
                                            .apply()

                                        mCycleOfTheDay = WANTrafficData
                                            .getCurrentWANCycle(
                                                mParentFragmentActivity,
                                                mParentFragmentPreferences
                                            )

                                        mCurrentCycle.set(mCycleOfTheDay)

                                        // Update
                                        val monthYearDisplayed = layout.findViewById<View>(
                                            R.id.tile_status_wan_monthly_month_displayed
                                        ) as TextView
                                        monthYearDisplayed.text = mCurrentCycle.get().getLabelWithYears()
                                    }
                                )

                                builder.create().show()

                                return@OnMenuItemClickListener true
                            }
                            else -> {
                            }
                        }
                        false
                    }
                )
                val inflater = popup.menuInflater

                val menu = popup.menu

                inflater.inflate(R.menu.tile_wan_monthly_traffic_options, menu)

                popup.show()
            }
        )
    }

    fun displayBackupDialog(displayName: String, backupFileType: Int) {
        val token = Bundle()
        token.putString(WAN_MONTHLY_TRAFFIC_ACTION, RouterAction.BACKUP_WAN_TRAFF.name)
        token.putInt(WAN_MONTHLY_TRAFFIC_BACKUP_FILETYPE, backupFileType)

        SnackbarUtils.buildSnackbar(
            mParentFragmentActivity,
            String.format(
                "Backup of WAN Traffic Data (as %s) is going to start on %s...",
                backupFileType, displayName
            ),
            "CANCEL",
            Snackbar.LENGTH_LONG,
            this@WANMonthlyTrafficTile,
            token, true
        )

        // new UndoBarController.UndoBar(mParentFragmentActivity).message(
        //    String.format("Backup of WAN Traffic Data (as %s) is going to start on %s...",
        //        backupFileType, displayName)).listener(WANMonthlyTrafficTile.this).token(token).show();
    }

    override fun getTileHeaderViewId(): Int {
        return R.id.tile_status_wan_monthly_traffic_hdr
    }

    override fun getTileTitleViewId(): Int {
        return R.id.tile_status_wan_monthly_traffic_title
    }

    @Throws(Exception::class)
    override fun onDismissEventTimeout(event: Int, token: Bundle?) {
        val routerAction = token?.getString(WAN_MONTHLY_TRAFFIC_ACTION)
        FirebaseCrashlytics.getInstance().log(
            "WAN Monthly Traffic Data Action: [$routerAction]"
        )
        if (routerAction.isNullOrBlank()) {
            return
        }
        try {
            when (RouterAction.valueOf(routerAction)) {
                RouterAction.DELETE_WAN_TRAFF -> {
                    run {
                        val alertDialog = Utils.buildAlertDialog(
                            mParentFragmentActivity, null,
                            "Erasing WAN Traffic Data - please hold on...", false, false
                        )
                        alertDialog.show()
                        (alertDialog.findViewById<View>(android.R.id.message) as TextView).gravity = Gravity.CENTER_HORIZONTAL
                        ActionManager.runTasks(
                            EraseWANMonthlyTrafficRouterAction(
                                mRouter, mParentFragmentActivity,
                                object : RouterActionListener {
                                    override fun onRouterActionFailure(
                                        routerAction: RouterAction,
                                        router: Router,
                                        exception: Exception?
                                    ) {
                                        try {
                                            this@WANMonthlyTrafficTile.onRouterActionFailure(
                                                routerAction, router,
                                                exception
                                            )
                                        } finally {
                                            mParentFragmentActivity.runOnUiThread { alertDialog.cancel() }
                                        }
                                    }

                                    override fun onRouterActionSuccess(
                                        routerAction: RouterAction,
                                        router: Router,
                                        returnData: Any
                                    ) {
                                        try {
                                            // dao delete everything
                                            dao.deleteWANTrafficDataByRouter(mRouter!!.uuid)
                                            this@WANMonthlyTrafficTile.onRouterActionSuccess(
                                                routerAction, router,
                                                returnData
                                            )
                                        } finally {
                                            mParentFragmentActivity.runOnUiThread { alertDialog.cancel() }
                                        }
                                        if (mLoader != null) {
                                            // Reload everything right away
                                            doneWithLoaderInstance(
                                                this@WANMonthlyTrafficTile,
                                                mLoader as AsyncTaskLoader<NVRAMInfo>, 1L
                                            )
                                        }
                                    }
                                },
                                mGlobalPreferences
                            )
                        )
                    }
                    return
                }
                RouterAction.BACKUP_WAN_TRAFF -> {
                    val fileType = token.getInt(
                        WAN_MONTHLY_TRAFFIC_BACKUP_FILETYPE,
                        BackupWANMonthlyTrafficRouterAction.BackupFileType_RAW
                    )
                    val alertDialog = Utils.buildAlertDialog(
                        mParentFragmentActivity, null,
                        "Backing up WAN Traffic Data - please hold on...", false, false
                    )
                    alertDialog.show()
                    (alertDialog.findViewById<View>(android.R.id.message) as TextView).gravity = Gravity.CENTER_HORIZONTAL
                    ActionManager.runTasks(
                        BackupWANMonthlyTrafficRouterAction(
                            mRouter, fileType, mParentFragmentActivity,
                            object : RouterActionListener {

                                override fun onRouterActionFailure(
                                    routerAction: RouterAction,
                                    router: Router,
                                    exception: Exception?
                                ) {
                                    try {
                                        Utils.displayMessage(
                                            mParentFragmentActivity,
                                            String.format(
                                                "Error on action '%s': %s",
                                                routerAction.toString(),
                                                Utils.handleException(exception).first
                                            ),
                                            Style.ALERT
                                        )
                                    } finally {
                                        mParentFragmentActivity.runOnUiThread { alertDialog.cancel() }
                                    }
                                }

                                override fun onRouterActionSuccess(
                                    routerAction: RouterAction,
                                    router: Router,
                                    returnData: Any
                                ) {
                                    try {
                                        val msg: String
                                        if (!(returnData is Array<*> && returnData.size >= 2)) {
                                            msg = String.format(
                                                "Action '%s' executed " +
                                                    "successfully on host '%s', but an internal error occurred. " +
                                                    "The issue will be reported. Please try again later.",
                                                routerAction.toString(), router.remoteIpAddress
                                            )
                                            Utils.displayMessage(mParentFragmentActivity, msg, Style.INFO)
                                            Utils.reportException(null, IllegalStateException(msg))
                                            return
                                        }

                                        val backupDateObject = returnData[0]
                                        val localBackupFileObject = returnData[1]

                                        if (!(backupDateObject is Date && localBackupFileObject is File)) {
                                            msg = String.format(
                                                "Action '%s' executed " +
                                                    "successfully on host '%s', but could not determine where " +
                                                    "local backup file has been saved. Please try again later.",
                                                routerAction.toString(), router.remoteIpAddress
                                            )
                                            Utils.displayMessage(mParentFragmentActivity, msg, Style.INFO)
                                            Utils.reportException(null, IllegalStateException(msg))
                                            return
                                        }

                                        Utils.displayMessage(
                                            mParentFragmentActivity,
                                            String.format(
                                                "Action '%s' executed successfully on host '%s'. " + "Now loading the file sharing activity chooser...",
                                                routerAction.toString(), router.remoteIpAddress
                                            ),
                                            Style.CONFIRM
                                        )

                                        val localBackupFile = returnData[1] as File
                                        val backupDate = returnData[0] as Date

                                        val uriForFile = FileProvider
                                            .getUriForFile(
                                                mParentFragmentActivity,
                                                RouterCompanionAppConstants.FILEPROVIDER_AUTHORITY,
                                                localBackupFile
                                            )
                                        mParentFragmentActivity.grantUriPermission(
                                            mParentFragmentActivity.packageName, uriForFile,
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        )

                                        val shareIntent = Intent()
                                        shareIntent.action = Intent.ACTION_SEND
                                        shareIntent.putExtra(
                                            Intent.EXTRA_SUBJECT,
                                            String.format(
                                                "Backup of WAN Monthly Traffic on Router '%s'",
                                                mRouter!!.canonicalHumanReadableName
                                            )
                                        )
                                        shareIntent.type = "text/html"
                                        shareIntent.putExtra(
                                            Intent.EXTRA_TEXT,
                                            fromHtml(
                                                ("Backup Date: " + backupDate + "\n\n")
                                                    .replace("\n".toRegex(), "<br/>") + Utils.getShareIntentFooter()
                                            )
                                        )
                                        shareIntent.putExtra(Intent.EXTRA_STREAM, uriForFile)
                                        mParentFragmentActivity
                                            .startActivity(
                                                Intent.createChooser(
                                                    shareIntent,
                                                    mParentFragmentActivity.resources
                                                        .getText(R.string.share_backup)
                                                )
                                            )
                                    } finally {
                                        mParentFragmentActivity.runOnUiThread { alertDialog.cancel() }
                                    }
                                }
                            },
                            mGlobalPreferences
                        )
                    )
                    return
                }
                else -> {
                }
            }
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            Utils.reportException(null, e)
        } catch (e: NullPointerException) {
            e.printStackTrace()
            Utils.reportException(null, e)
        }
    }

    override fun onLoadFinished(loader: Loader<NVRAMInfo>, data: NVRAMInfo?) {
        var data = data
        try {
            FirebaseCrashlytics.getInstance().log(
                "onLoadFinished: loader=$loader / data=$data / data=$data"
            )

            setLoadingViewVisibility(View.GONE)
            layout.findViewById<View>(R.id.tile_status_wan_monthly_traffic_header_loading_view).visibility = View.GONE
            layout.findViewById<View>(R.id.tile_status_wan_monthly_traffic_title).visibility = View.VISIBLE

            var preliminaryCheckException: Exception? = null
            if (data == null) {
                preliminaryCheckException = DDWRTNoDataException("No Data!")
            } else if (data.getException() == null) {
                if ("1" != data.getProperty(NVRAMInfo.TTRAFF_ENABLE)) {
                    preliminaryCheckException = DDWRTTraffDataDisabled("Traffic monitoring disabled!")
                } else if (data.isEmpty) {
                    preliminaryCheckException = DDWRTNoDataException("No Traffic Data!")
                }
            }

            val enableTraffDataButton = this.layout.findViewById<View>(R.id.tile_status_wan_monthly_traffic_status) as SwitchCompat
            enableTraffDataButton.visibility = View.VISIBLE

            val makeToogleEnabled = data != null && data.getData() != null && data.getData()!!
                .containsKey(NVRAMInfo.TTRAFF_ENABLE)

            if (!isToggleStateActionRunning.get()) {
                if (makeToogleEnabled) {
                    enableTraffDataButton.isChecked = "1" == data!!.getProperty(NVRAMInfo.TTRAFF_ENABLE)
                    enableTraffDataButton.isEnabled = true
                } else {
                    enableTraffDataButton.isChecked = false
                    enableTraffDataButton.isEnabled = false
                }
                enableTraffDataButton.setOnClickListener(ManageWANTrafficCounterToggle())
            }

            if (preliminaryCheckException != null) {
                data = NVRAMInfo().setException(preliminaryCheckException)
            }

            val errorPlaceHolderView = this.layout.findViewById<View>(R.id.tile_status_wan_monthly_traffic_error) as TextView

            val exception = data!!.getException()

            val displayButton = this.layout.findViewById<View>(
                R.id.tile_status_wan_monthly_traffic_graph_placeholder_display_button
            )
            val currentButton = this.layout.findViewById<Button>(R.id.tile_status_wan_monthly_traffic_graph_placeholder_current)
            val dateFromPickerButton = this.layout.findViewById<Button>(R.id.tile_status_wan_monthly_traffic_graph_placeholder_date_from_picker)
            val dateToPickerButton = this.layout.findViewById<Button>(R.id.tile_status_wan_monthly_traffic_graph_placeholder_date_to_picker)
            val monthYearDisplayed = this.layout.findViewById<View>(R.id.tile_status_wan_monthly_month_displayed) as TextView

            val ctrlViews = arrayOf(monthYearDisplayed, displayButton, currentButton, dateFromPickerButton, dateToPickerButton)

            // Create Options Menu
            val tileMenu = layout.findViewById<View>(R.id.tile_status_wan_monthly_traffic_menu) as ImageButton

            if (!ColorUtils.isThemeLight(mParentFragmentActivity)) {
                // Set menu background to white
                tileMenu.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark)
            }

            tileMenu.visibility = View.VISIBLE

            if (exception == null) {
                errorPlaceHolderView.visibility = View.GONE

                monthYearDisplayed.text = mCurrentCycle.get().getLabelWithYears()

                displayButton.setOnClickListener { v ->
                    val monthYearDisplayedText = monthYearDisplayed.text

                    val cycleItem = mCurrentCycle.get()
                    if (cycleItem == null) {
                        Toast.makeText(
                            this@WANMonthlyTrafficTile.mParentFragmentActivity,
                            String.format("No traffic data for '%s'", monthYearDisplayedText),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {

                        val intent = Intent(mParentFragmentActivity, WANMonthlyTrafficActivity::class.java)
                        intent.putExtra(
                            RouterManagementActivity.ROUTER_SELECTED,
                            mRouter?.uuid ?: EMPTY_STRING
                        )
                        intent.putExtra(WANMonthlyTrafficActivity.WAN_CYCLE, mGson.toJson(cycleItem))

                        val alertDialog = ProgressDialog.show(
                            mParentFragmentActivity,
                            String.format("Loading traffic data for '%s'", monthYearDisplayedText),
                            "Please Wait...", true
                        )

                        Handler().postDelayed(
                            {
                                val options = ActivityOptionsCompat.makeScaleUpAnimation(
                                    v, 0, 0, v.width,
                                    v.height
                                )
                                ActivityCompat.startActivity(mParentFragmentActivity, intent, options.toBundle())
                                //
                                //                                    mParentFragmentActivity.startActivity(intent);
                                //                                    mParentFragmentActivity.overridePendingTransition(
                                //                                            R.anim.zoom_enter, R.anim.zoom_exit);
                                alertDialog.cancel()
                            },
                            1000
                        )
                    }
                }

                currentButton.setOnClickListener {
                    mCycleOfTheDay = WANTrafficData.getCurrentWANCycle(
                        mParentFragmentActivity,
                        mParentFragmentPreferences
                    )
                    mCurrentCycle.set(mCycleOfTheDay)
                    monthYearDisplayed.text = mCycleOfTheDay.getLabelWithYears()
                    if (mParentFragmentPreferences != null) {
                        try {
                            mParentFragmentPreferences.edit()
                                .remove(getFormattedPrefKey(WAN_CYCLE_DISPLAYED))
                                .apply()
                        } catch (e: Exception) {
                            // No worries
                        }
                    }
                }

                dateFromPickerButton.setOnClickListener {
                    val datePickerListener = object : AbstractDatePickerListener() {
                        override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
                            val calendar = Calendar.getInstance()
                            calendar.set(year, month, dayOfMonth)
                            val currentCycle = mCurrentCycle.get()
                            currentCycle.start = calendar.timeInMillis
                            monthYearDisplayed.text = currentCycle.refreshLabelWithYears()
                            if (mParentFragmentPreferences != null) {
                                try {
                                    mParentFragmentPreferences.edit()
                                        .putString(
                                            getFormattedPrefKey(WAN_CYCLE_DISPLAYED),
                                            mGson.toJson(currentCycle)
                                        )
                                        .apply()
                                    Utils.requestBackup(mParentFragmentActivity)
                                } catch (e: Exception) {
                                    // No worries
                                }
                            }
                        }
                    }
                    val datePickerFragment = DatePickerFragment.newInstance(
                        datePickerListener,
                        mCurrentCycle.get().start,
                        maxMillis = mCurrentCycle.get().end
                    )
                    datePickerFragment.show(mParentFragmentActivity.supportFragmentManager, "dateFromPicker")
                }

                dateToPickerButton.setOnClickListener {
                    val datePickerListener = object : AbstractDatePickerListener() {
                        override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
                            val calendar = Calendar.getInstance()
                            calendar.set(year, month, dayOfMonth)
                            val currentCycle = mCurrentCycle.get()
                            currentCycle.end = calendar.timeInMillis
                            monthYearDisplayed.text = currentCycle.refreshLabelWithYears()
                            if (mParentFragmentPreferences != null) {
                                try {
                                    mParentFragmentPreferences.edit()
                                        .putString(
                                            getFormattedPrefKey(WAN_CYCLE_DISPLAYED),
                                            mGson.toJson(currentCycle)
                                        )
                                        .apply()
                                    Utils.requestBackup(mParentFragmentActivity)
                                } catch (e: Exception) {
                                    // No worries
                                }
                            }
                        }
                    }
                    val datePickerFragment = DatePickerFragment.newInstance(
                        datePickerListener = datePickerListener,
                        startFromMillis = mCurrentCycle.get().end,
                        minMillis = mCurrentCycle.get().start
                    )
                    datePickerFragment.show(mParentFragmentActivity.supportFragmentManager, "dateToPicker")
                }

                setVisibility(ctrlViews, View.VISIBLE)

                // Update last sync
                val lastSyncView = layout.findViewById<View>(R.id.tile_last_sync) as RelativeTimeTextView
                lastSyncView.setReferenceTime(mLastSync)
                lastSyncView.prefix = "Last sync: "
            }

            if (exception != null && exception !is DDWRTTileAutoRefreshNotAllowedException) {

                val rootCause = Throwables.getRootCause(exception)
                errorPlaceHolderView.text = "Error: ${rootCause?.message}"
                val parentContext = this.mParentFragmentActivity
                errorPlaceHolderView.setOnClickListener {
                    if (rootCause != null) {
                        Toast.makeText(parentContext, rootCause.message, Toast.LENGTH_LONG).show()
                    }
                }
                errorPlaceHolderView.visibility = View.VISIBLE
                setVisibility(ctrlViews, View.GONE)
                updateProgressBarWithError()
            } else if (exception == null) {
                updateProgressBarWithSuccess()
                if (data == null || data.isEmpty) {
                    errorPlaceHolderView.text = "Error: No Data!"
                    errorPlaceHolderView.visibility = View.VISIBLE
                    setVisibility(ctrlViews, View.GONE)
                }
            }

            FirebaseCrashlytics.getInstance().log("onLoadFinished(): done loading!")
        } finally {
            mRefreshing.set(false)
            doneWithLoaderInstance(this, loader)
        }
    }

    override fun onRouterActionFailure(
        routerAction: RouterAction,
        router: Router,
        exception: Exception?
    ) {
        Utils.displayMessage(
            mParentFragmentActivity,
            String.format(
                "Error on action '%s': %s", routerAction.toString(),
                Utils.handleException(exception).first
            ),
            Style.ALERT
        )
    }

    override fun onRouterActionSuccess(
        routerAction: RouterAction,
        router: Router,
        returnData: Any
    ) {
        Utils.displayMessage(
            mParentFragmentActivity,
            String.format(
                "Action '%s' executed successfully on host '%s'", routerAction.toString(),
                router.remoteIpAddress
            ),
            Style.CONFIRM
        )
    }

    override fun getLoader(id: Int, args: Bundle?): Loader<NVRAMInfo>? {

        if (nbRunsLoader <= 0) {
            setLoadingViewVisibility(View.VISIBLE)
        }

        mLoader = object : AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            override fun loadInBackground(): NVRAMInfo? {

                try {

                    mIsThemeLight = ColorUtils.isThemeLight(mParentFragmentActivity)

                    FirebaseCrashlytics.getInstance().log(
                        "Init background loader for " +
                            WANMonthlyTrafficTile::class.java +
                            ": routerInfo=" +
                            mRouter +
                            " / nbRunsLoader=" +
                            nbRunsLoader
                    )

                    if (mRefreshing.getAndSet(true)) {
                        return NVRAMInfo().setException(DDWRTTileAutoRefreshNotAllowedException())
                    }
                    if (!isForceRefresh) {
                        // Force Manual Refresh
                        if (isToggleStateActionRunning.get()) {
                            // Skip run
                            FirebaseCrashlytics.getInstance().log("Skip loader run")
                            throw DDWRTTileAutoRefreshNotAllowedException()
                        }
                    } else {
                        if (isToggleStateActionRunning.get()) {
                            // Action running - skip
                            throw DDWRTTileAutoRefreshNotAllowedException()
                        }
                    }
                    nbRunsLoader++

                    updateProgressBarViewSeparator(0)

                    mLastSync = System.currentTimeMillis()

                    updateProgressBarViewSeparator(10)
                    // Get TTRAFF_ENABLE
                    val ttraffEnableNVRAMInfo = SSHUtils.getNVRamInfoFromRouter(
                        mParentFragmentActivity, mRouter, mGlobalPreferences,
                        NVRAMInfo.TTRAFF_ENABLE
                    )

                    updateProgressBarViewSeparator(20)
                    mCycleOfTheDay = WANTrafficData.getCurrentWANCycle(
                        mParentFragmentActivity,
                        mParentFragmentPreferences
                    )

                    var cycleItem: MonthlyCycleItem? = null
                    if (mParentFragmentPreferences != null) {
                        val wanCycleDisplayed = mParentFragmentPreferences.getString(getFormattedPrefKey(WAN_CYCLE_DISPLAYED), null)
                        if (!isNullOrEmpty(wanCycleDisplayed)) {
                            try {
                                cycleItem = mGson.fromJson(wanCycleDisplayed, MonthlyCycleItem::class.java)
                            } catch (e: Exception) {
                                // No worries
                            }
                        }
                    }
                    mCurrentCycle.set(if (cycleItem != null) cycleItem else mCycleOfTheDay)

                    getTrafficDataNvramInfoAndPersistIfNeeded(
                        mParentFragmentActivity, mRouter,
                        mGlobalPreferences, dao
                    )

                    updateProgressBarViewSeparator(55)
                    val nvramInfo = WANTrafficUtils.computeWANTrafficUsageBetweenDates(
                        dao, mRouter!!.uuid,
                        mCurrentCycle.get().start, mCurrentCycle.get().end
                    )

                    if (ttraffEnableNVRAMInfo != null) {
                        nvramInfo.putAll(ttraffEnableNVRAMInfo)
                    }

                    updateProgressBarViewSeparator(90)

                    return nvramInfo
                } catch (e: Exception) {
                    e.printStackTrace()
                    return NVRAMInfo().setException(e)
                }
            }
        }

        return mLoader
    }

    override fun getLogTag() = LOG_TAG

    override fun getOnclickIntent() = null

    private fun setLoadingViewVisibility(visibility: Int) {
        this.layout.findViewById<View>(R.id.tile_status_wan_monthly_month_loading).visibility = visibility
    }

    companion object {

        @JvmField
        val WAN_MONTHLY_TRAFFIC = "WANMonthlyTraffic"

        @JvmField
        val WAN_MONTHLY_TRAFFIC_BACKUP_FILETYPE = "WAN_MONTHLY_TRAFFIC_BACKUP_FILETYPE"

        @JvmField
        val RESTORE_WAN_MONTHLY_TRAFFIC_FRAGMENT_TAG = "RESTORE_WAN_MONTHLY_TRAFFIC_FRAGMENT_TAG"

        @JvmField
        val WAN_MONTHLY_TRAFFIC_ACTION = "WAN_MONTHLY_TRAFFIC_ACTION"

        @JvmField
        val WAN_CYCLE_DISPLAYED = "wanCycleDisplayed"

        private val LOG_TAG = WANMonthlyTrafficTile::class.java.simpleName

        private fun setVisibility(views: Array<View>, visibility: Int) {
            for (view in views) {
                view.visibility = visibility
            }
        }
    }
}
