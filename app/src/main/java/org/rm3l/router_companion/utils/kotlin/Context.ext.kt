package org.rm3l.router_companion.utils.kotlin

import android.content.Context
import android.content.Context.MODE_PRIVATE
import org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY
import org.rm3l.router_companion.RouterCompanionAppConstants.THEMING_PREF
import org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_THEME
import org.rm3l.router_companion.utils.ColorUtils

/**
 * Set of extensions functions for {@link Context}
 */

fun Context.isThemeLight(): Boolean {
  return this.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, MODE_PRIVATE)
      .getLong(THEMING_PREF, DEFAULT_THEME) == ColorUtils.LIGHT_THEME
}
