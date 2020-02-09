package org.rm3l.router_companion.utils

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.INSTALL_SHORTCUT
import android.Manifest.permission.NFC
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.UNINSTALL_SHORTCUT
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.listener.multi.BaseMultiplePermissionsListener
import org.rm3l.router_companion.utils.snackbar.SnackbarCallback
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import org.rm3l.router_companion.common.utils.ActivityUtils
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style

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
            requestPermissions(
                activity = activity,
                permissions = REQUESTABLE_PERMISSIONS,
                rationaleMessage = "Storage access is needed to reduce data usage and enable sharing.",
                snackbarActionText = "Settings",
                snackbarDuration = Snackbar.LENGTH_INDEFINITE,
                snackbarCb = object: SnackbarCallback {
                    override fun onDismissEventActionClick(event: Int, bundle: Bundle?) =
                        ActivityUtils.openApplicationSettings(activity)
                }
            )
        }

        @JvmStatic
        fun requestPermissions(activity: Activity, permissions: Collection<String>, rationaleMessage: String,
                              snackbarActionText: String? = null,
                              @Snackbar.Duration snackbarDuration: Int = Snackbar.LENGTH_LONG,
                              snackbarCb : SnackbarCallback? = null) {
            requestPermissions(activity, *permissions.toTypedArray(), listener=object: BaseMultiplePermissionsListener() {
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
        }

        @JvmStatic
        fun requestPermissions(activity: Activity, vararg permissions: String, listener: MultiplePermissionsListener) {
            Dexter.withActivity(activity)
                .withPermissions(*permissions)
                .withListener(listener)
                .withErrorListener {error -> FirebaseCrashlytics.getInstance().log( "Dexter reported an error: $error") }
                .check()
        }

        @JvmStatic
        fun requestPermission(activity: Activity, permission: String, listener: PermissionListener) {
            Dexter.withActivity(activity)
                .withPermission(permission)
                .withListener(listener)
                .withErrorListener {error -> FirebaseCrashlytics.getInstance().log( "Dexter reported an error: $error") }
                .check()
        }

        @JvmStatic
        fun requestPermissionWithNoCallback(activity: Activity,
                                             permission: String,
                                             onPermissionDeniedMessage: String? = null) =
            requestPermissions(activity, listOf(permission), {Unit}, {Unit}, onPermissionDeniedMessage)

        @JvmStatic
        fun requestPermissionsWithNoCallback(activity: Activity,
                               permissions: Collection<String>,
                               onPermissionDeniedMessage: String? = null) =
            requestPermissions(activity, permissions, {Unit}, {Unit}, onPermissionDeniedMessage)

        @JvmStatic
        fun requestPermissions(activity: Activity,
                               permissions: Collection<String>,
                               onPermissionGranted: ()->Unit?,
                               onPermissionDenied: ()->Unit?,
                               onPermissionDeniedMessage: String? = null) =
            requestPermissions(activity, permissions, onPermissionGranted,
                onPermissionDeniedMessage, onPermissionDeniedMessage, onPermissionDenied)

        @JvmStatic
        fun requestPermissions(activity: Activity, permissions: Collection<String>,
                                        onPermissionGranted: ()->Unit?,
                                        onPermissionDeniedMessage: String? = null,
                                        onPermissionPermantentlyDeniedMessage: String? = null,
                                        onPermissionDenied: ()->Unit? ) {
            requestPermissions(activity, *permissions.toTypedArray(), listener=object: BaseMultiplePermissionsListener() {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report?.areAllPermissionsGranted() == true) {
                        onPermissionGranted()
                    } else {
                        if (report?.isAnyPermissionPermanentlyDenied == true) {
                            SnackbarUtils.buildSnackbar(
                                activity,
                                onPermissionPermantentlyDeniedMessage,
                                "Settings",
                                Snackbar.LENGTH_LONG,
                                object : SnackbarCallback {
                                    override fun onDismissEventActionClick(event: Int, bundle: Bundle?) {
                                        ActivityUtils.openApplicationSettings(activity)
                                    }
                                }, null, true
                            )
                        } else {
                            Utils.displayMessage(activity, onPermissionDeniedMessage, Style.INFO)
                        }
                        onPermissionDenied()
                    }
                }
            })

        }

        @JvmStatic
        fun isPermissionGranted(context: Context, permission: String) =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}