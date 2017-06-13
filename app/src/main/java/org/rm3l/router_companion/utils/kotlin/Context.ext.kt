package org.rm3l.router_companion.utils.kotlin

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.ContextWrapper
import android.support.annotation.ColorRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY
import org.rm3l.router_companion.RouterCompanionAppConstants.THEMING_PREF
import org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_THEME
import org.rm3l.router_companion.resources.conn.Router.RouterFirmware
import org.rm3l.router_companion.utils.AndroidHelper
import org.rm3l.router_companion.utils.ColorUtils

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

fun Context.inflate(res: Int, parent: ViewGroup? = null) : View {
  return LayoutInflater.from(this).inflate(res, parent, false)
}
