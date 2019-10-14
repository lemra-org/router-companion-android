package org.rm3l.router_companion.utils

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.INSTALL_SHORTCUT
import android.Manifest.permission.NFC
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.UNINSTALL_SHORTCUT
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.listener.multi.BaseMultiplePermissionsListener
import org.rm3l.router_companion.utils.snackbar.SnackbarCallback
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.CATEGORY_DEFAULT
import android.net.Uri
import android.provider.Settings

class PermissionsUtils private constructor() {

    init {
        throw UnsupportedOperationException("Not instantiable")
    }

    companion object {

        private val TAG: String = PermissionsUtils::class.java.simpleName

        private val REQUESTABLE_PERMISSIONS = listOf(
            WRITE_EXTERNAL_STORAGE,
            READ_EXTERNAL_STORAGE,
            ACCESS_COARSE_LOCATION,
            NFC,
            INSTALL_SHORTCUT,
            UNINSTALL_SHORTCUT
        )

        @JvmStatic
        fun requestAppPermissions(activity: Activity) {
            requestPermission(
                activity = activity,
                permissions = REQUESTABLE_PERMISSIONS,
                rationaleMessage = "Storage access is needed to reduce data usage and enable sharing.",
                snackbarActionText = "Settings",
                snackbarDuration = Snackbar.LENGTH_INDEFINITE,
                snackbarCb = object: SnackbarCallback {
                    override fun onDismissEventActionClick(event: Int, bundle: Bundle?) {
                        val myAppSettings = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${activity.packageName}")
                        )
                        myAppSettings.addCategory(CATEGORY_DEFAULT)
                        myAppSettings.flags = FLAG_ACTIVITY_NEW_TASK
                        activity.startActivity(myAppSettings)
                    }
                }
            )
        }

        @JvmStatic
        fun requestPermission(activity: Activity, permissions: Collection<String>, rationaleMessage: String,
                              snackbarActionText: String? = null,
                              @Snackbar.Duration snackbarDuration: Int = Snackbar.LENGTH_LONG,
                              snackbarCb : SnackbarCallback? = null) {
            Dexter.withActivity(activity)
                .withPermissions(permissions)
                .withListener(object: BaseMultiplePermissionsListener() {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report?.areAllPermissionsGranted() == false) {
                            SnackbarUtils.buildSnackbar(activity, rationaleMessage,
                                snackbarActionText,
                                snackbarDuration,
                                snackbarCb,
                                null,
                                true)
                        }
                    }
                })
                .withErrorListener {error -> Crashlytics.log(Log.WARN, TAG, "Dexter reported an error: $error") }
                .check()
        }
    }
}