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

package org.rm3l.router_companion.utils

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.support.annotation.ColorRes
import android.support.annotation.StyleRes
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.TextView
import com.crashlytics.android.Crashlytics
import com.google.common.base.Strings
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.cache.RemovalListener
import com.google.common.cache.RemovalNotification
import java.util.Collections
import java.util.HashSet
import java.util.Random
import java.util.concurrent.ExecutionException
import org.rm3l.ddwrt.R
import org.rm3l.router_companion.RouterCompanionAppConstants
import org.rm3l.router_companion.exceptions.DDWRTCompanionException
import org.rm3l.router_companion.resources.conn.Router.RouterFirmware

import org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_THEME
import org.rm3l.router_companion.RouterCompanionAppConstants.THEMING_PREF

class ColorUtils private constructor() {

  init {
    throw UnsupportedOperationException("Not instantiable")
  }

  companion object {

    @JvmField
    val LIGHT_THEME = DEFAULT_THEME

    @JvmField
    val DARK_THEME = 31L

    private val MAX_ITERATIONS = 10

    private val RANDOM_COLOR_GEN = Random()
    private val COLOR_SIMILARITY_TOLERANCE = 77.0

    private val TAG = ColorUtils::class.java.simpleName

    private val colorsCacheMapCache = mutableMapOf<String, Int>()

    val colorsCache:LoadingCache<String, Int> = CacheBuilder.newBuilder()
        .maximumSize(30)
        .removalListener(RemovalListener<String, Int> { notification ->
          Crashlytics.log(Log.DEBUG, TAG,
              "onRemoval(" + notification.key + ") - cause: " + notification.cause)
        })
        .build(object : CacheLoader<String, Int>() {
          @Throws(Exception::class)
          override fun load(key: String): Int? {
            val colorsToSkip = HashSet<Int>()

            //We want our new color not to be similar to white or black
            colorsToSkip.add(Color.argb(255, 0, 0, 0))
            colorsToSkip.add(Color.argb(255, 255, 255, 255))

            if (!colorsCacheMapCache.isEmpty()) {
              colorsToSkip.addAll(colorsCacheMapCache.values)
            }

            val genColor = genColor(colorsToSkip)
            colorsCacheMapCache[key] = genColor
            return genColor
          }
        })

    fun getColor(keyInCache: String): Int {
      try {
        return colorsCache.get(keyInCache)
      } catch (e: ExecutionException) {
        Utils.reportException(null, e)
        return genColor(emptyList<Int>())
      }

    }

    fun genColor(colorsToSkip: Collection<Int>): Int {
      //Generate a Random Color, excluding colors similar to the colors specified
      var aNextColor: Int
      var rNextColor: Int
      var gNextColor: Int
      var bNextColor: Int

      var newColor: Int
      var iterationNb = 0
      do {
        aNextColor = 255
        rNextColor = 1 + RANDOM_COLOR_GEN.nextInt(254)
        gNextColor = 1 + RANDOM_COLOR_GEN.nextInt(254)
        bNextColor = 1 + RANDOM_COLOR_GEN.nextInt(254)
        newColor = Color.argb(aNextColor, rNextColor, gNextColor, bNextColor)
      } while (iterationNb++ <= MAX_ITERATIONS && isColorSimilarToAtLeastOne(newColor,
          colorsToSkip))

      return newColor
    }

    fun isColorSimilarToAtLeastOne(color: Int,
        colorsColl: Collection<Int>): Boolean {

      //Apply color maths to determine a color which is not visually similar to any of the existing ones.
      //Based upon Euclidian distance in the ARGB color space.

      val aColor = color shr 24 and 0xff
      val rColor = color shr 16 and 0xff
      val gColor = color shr 8 and 0xff
      val bColor = color and 0xff

      for (colorInColl in colorsColl) {

        val aColorInColl = colorInColl shr 24 and 0xff
        val rColorInColl = colorInColl shr 16 and 0xff
        val gColorInColl = colorInColl shr 8 and 0xff
        val bColorInColl = colorInColl and 0xff

        val euclidianDistance = Math.sqrt(
            Math.pow((aColorInColl - aColor).toDouble(), 2.0) + Math.pow(
                (rColorInColl - rColor).toDouble(), 2.0) + Math.pow(
                (gColorInColl - gColor).toDouble(), 2.0) + Math.pow(
                (bColorInColl - bColor).toDouble(), 2.0))

        if (java.lang.Double.compare(euclidianDistance, COLOR_SIMILARITY_TOLERANCE) <= 0) {
          return true
        }
      }

      return false
    }

    fun isThemeLight(context: Context?): Boolean {
      return context?.getSharedPreferences(
          RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY,
          Context.MODE_PRIVATE)?.getLong(THEMING_PREF, DEFAULT_THEME) == LIGHT_THEME
    }

    @ColorRes
    fun getActualColorRes(routerFirmware: RouterFirmware?,
        themeSuffix: String?): Int? {

      val useDefaultStyle = routerFirmware == null
          || RouterFirmware.AUTO == routerFirmware
          || RouterFirmware.UNKNOWN == routerFirmware
      if (useDefaultStyle) {
        //What to return here? => default behavior
        return null
      } else {
        try {
          return Utils.getResId(
              String.format("%s_%s",
                  routerFirmware!!.name.toLowerCase(),
                  Strings.nullToEmpty(themeSuffix)),
              R.color::class.java)
        } catch (e: Exception) {
          Crashlytics.logException(e)
          return null
        }

      }
    }

    @ColorRes
    fun getStatusBarColor(routerFirmware: RouterFirmware?): Int? {
      return getActualColorRes(routerFirmware, "statusbar")
    }

    @ColorRes
    fun getPrimaryColor(routerFirmware: RouterFirmware?): Int? {
      return getActualColorRes(routerFirmware, "primary")
    }

    @ColorRes
    fun getAccentColor(routerFirmware: RouterFirmware?): Int? {
      return getActualColorRes(routerFirmware, "color_accent")
    }

    fun <T : ContextWrapper> setAppTheme(activity: T,
        routerFirmware: RouterFirmware?, transparentStatusBar: Boolean) {

      val useDefaultStyle = routerFirmware == null
          || RouterFirmware.AUTO == routerFirmware
          || RouterFirmware.UNKNOWN == routerFirmware
      if (useDefaultStyle) {
        setDefaultTheme(activity, transparentStatusBar)
      } else {
        val themeLight = isThemeLight(activity)
        try {
          //Determine style by intropsection
          @StyleRes val styleResId = Utils.getResId(
              String.format("%s_AppTheme%s%s", routerFirmware!!.name,
                  if (themeLight) "Light" else "Dark",
                  if (transparentStatusBar) "_StatusBarTransparent" else ""), R.style::class.java)
          activity.setTheme(styleResId)
        } catch (e: Exception) {
          Crashlytics.logException(e)
          setDefaultTheme(activity, transparentStatusBar)
        }

      }
    }

    fun <T : ContextWrapper> setDefaultTheme(activity: T,
        transparentStatusBar: Boolean) {

      if (isThemeLight(activity)) {
        activity.setTheme(if (transparentStatusBar)
          R.style.AppThemeLight_StatusBarTransparent
        else
          R.style.AppThemeLight)
      } else {
        activity.setTheme(
            if (transparentStatusBar) R.style.AppThemeDark_StatusBarTransparent else R.style.AppThemeDark)
      }
    }

    fun <T : TextView> setTextColor(view: T?,
        routerFirmware: RouterFirmware?) {

      if (view == null) {
        return
      }

      val useDefaultStyle = routerFirmware == null
          || RouterFirmware.AUTO == routerFirmware
          || RouterFirmware.UNKNOWN == routerFirmware
      if (useDefaultStyle) {
        setDefaultTextColor(view)
      } else {
        val context = view.context
        try {
          //Determine style by intropsection
          @ColorRes val textColorResId = Utils.getResId(
              String.format("%s_tile_title", routerFirmware!!.name.toLowerCase()),
              R.color::class.java)
          view.setTextColor(ContextCompat.getColor(context, textColorResId))
        } catch (e: Exception) {
          Crashlytics.logException(e)
          setDefaultTextColor(view)
        }

      }
    }

    fun <T : TextView> setDefaultTextColor(view: T?) {
      if (view == null) {
        return
      }
      view.setTextColor(ContextCompat.getColor(view.context, R.color.tile_title))
    }
  }
}
