@file:JvmName("ContextUtils")
package org.rm3l.router_companion.utils.kotlin

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.ContextWrapper
import android.content.Intent
import android.os.Handler
import android.support.annotation.ColorRes
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.crashlytics.android.Crashlytics
import org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY
import org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_THEME
import org.rm3l.router_companion.RouterCompanionAppConstants.THEMING_PREF
import org.rm3l.router_companion.resources.conn.Router
import org.rm3l.router_companion.resources.conn.Router.RouterFirmware
import org.rm3l.router_companion.utils.AndroidHelper
import org.rm3l.router_companion.utils.ColorUtils
import org.rm3l.router_companion.utils.Utils

/**
 * Set of extensions functions for {@link Context}
 */

fun Context.isThemeLight(): Boolean =
        (this.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, MODE_PRIVATE)
                .getLong(THEMING_PREF, DEFAULT_THEME) == ColorUtils.LIGHT_THEME)

fun ContextWrapper.setAppTheme(routerFirmware: RouterFirmware?, transparentStatusBar: Boolean) =
        ColorUtils.setAppTheme(this, routerFirmware, transparentStatusBar)

fun ContextWrapper.setAppTheme(routerFirmware: RouterFirmware?) =
        this.setAppTheme(routerFirmware, false)

@Suppress("DEPRECATION")
@SuppressLint("NewApi")
fun Context.color(@ColorRes id: Int) = when {
    AndroidHelper.isAtLeastMarshmallow() -> resources.getColor(id, null)
    else -> resources.getColor(id)
}

fun Context.inflate(res: Int, parent: ViewGroup? = null): View {
    return LayoutInflater.from(this).inflate(res, parent, false)
}

fun Activity.openFeedbackForm() = this.openFeedbackForm(routerUuid = null)

fun Activity.openFeedbackForm(routerUuid: String? = null) =
        this.openFeedbackForm(router = if (routerUuid == null) null
        else org.rm3l.router_companion.mgmt.RouterManagementActivity.getDao(this).getRouter(routerUuid))

fun Activity.openFeedbackForm(router: Router? = null) = Utils.openFeedbackForm(this, router)

fun Activity.restartWholeApplication(waitMessage: CharSequence? = null, delayMillis: Long? = null) {
    Crashlytics.log(Log.INFO, this::class.java.simpleName, "Restarting whole Android Application : ${waitMessage ?:""}...")
    val alertDialog = ProgressDialog.show(this, waitMessage, "App will restart. Please wait...", true)
    Handler().postDelayed({
        alertDialog.cancel()
        val intent = PendingIntent.getActivity(this.baseContext, 0, Intent(this.intent),
                PendingIntent.FLAG_CANCEL_CURRENT)
        val manager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
        manager?.set(AlarmManager.RTC, System.currentTimeMillis() + 100, intent)
        System.exit(2)
    }, delayMillis?:2_000)
}

fun Activity.finishAndReload(waitMessage: String? = null, delayMillis: Long? = null, operationBeforeRestart: Runnable? = null) {
    Crashlytics.log(Log.INFO, this::class.java.simpleName,
            "Finishing and reloading current activity (${this::class.java}): ${waitMessage ?:""}...")
    val alertDialog = ProgressDialog.show(this, waitMessage, "Please wait...", true)
//    val alertDialog = Utils.buildAlertDialog(this, null,
//            waitMessage?:"Reloading...", false, false)
//    alertDialog.show()
    alertDialog.findViewById<TextView>(android.R.id.message).gravity = Gravity.CENTER_HORIZONTAL
    Handler().postDelayed({
        finish()
        operationBeforeRestart?.run()
        startActivity(intent)
        alertDialog.cancel()
    }, delayMillis?:2_000)
}

fun Context?.getApplicationName(): CharSequence {
    return if (this == null) "" else this.applicationInfo.loadLabel(this.packageManager)
}