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

package org.rm3l.router_companion.utils;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.exceptions.DDWRTCompanionException;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.resources.conn.Router.RouterFirmware;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_THEME;
import static org.rm3l.router_companion.RouterCompanionAppConstants.THEMING_PREF;

public final class ColorUtils {

    public static final long LIGHT_THEME = DEFAULT_THEME;
    public static final long DARK_THEME = 31l;

    private static final int MAX_ITERATIONS = 10;

    private static final Random RANDOM_COLOR_GEN = new Random();
    private static final double COLOR_SIMILARITY_TOLERANCE = 77;

    private static final String TAG = ColorUtils.class.getSimpleName();

    public static final LoadingCache<String, Integer> colorsCache = CacheBuilder
            .newBuilder()
            .maximumSize(30)
            .removalListener(new RemovalListener<String, Integer>() {
                @Override
                public void onRemoval(@NonNull RemovalNotification<String, Integer> notification) {
                    Crashlytics.log(Log.DEBUG, TAG, "onRemoval(" + notification.getKey() + ") - cause: " +
                            notification.getCause());
                }
            }).build(new CacheLoader<String, Integer>() {
                @Override
                public Integer load(@NonNull String key) throws Exception {
                    final Map<String, Integer> currentItems = colorsCache.asMap();
                    final Set<Integer> colorsToSkip = new HashSet<>();

                    //We want our new color not to be similar to white or black
                    colorsToSkip.add(Color.argb(255, 0, 0, 0));
                    colorsToSkip.add(Color.argb(255, 255, 255, 255));

                    if (!currentItems.isEmpty()) {
                        colorsToSkip.addAll(currentItems.values());
                    }

                    return genColor(colorsToSkip);
                }
            });

    private ColorUtils() {
    }

    public static int getColor(@NonNull final String keyInCache) {
        try {
            return colorsCache.get(keyInCache);
        } catch (final ExecutionException e) {
            Utils.reportException(null, e);
            return genColor(Collections.<Integer> emptyList());
        }
    }

    public static int genColor(@NonNull final Collection<Integer> colorsToSkip) {
        //Generate a Random Color, excluding colors similar to the colors specified
        int aNextColor;
        int rNextColor;
        int gNextColor;
        int bNextColor;

        int newColor;
        int iterationNb = 0;
        do {
            aNextColor = 255;
            rNextColor = 1 + RANDOM_COLOR_GEN.nextInt(254);
            gNextColor = 1 + RANDOM_COLOR_GEN.nextInt(254);
            bNextColor = 1 + RANDOM_COLOR_GEN.nextInt(254);
            newColor = Color.argb(aNextColor, rNextColor, gNextColor, bNextColor);

        }
        while ((iterationNb++) <= MAX_ITERATIONS && isColorSimilarToAtLeastOne(newColor, colorsToSkip));

        return newColor;
    }

    public static boolean isColorSimilarToAtLeastOne(int color, @NonNull final Collection<Integer> colorsColl) {

        //Apply color maths to determine a color which is not visually similar to any of the existing ones.
        //Based upon Euclidian distance in the ARGB color space.

        final int aColor = (color >> 24) & 0xff;
        final int rColor = (color >> 16) & 0xff;
        final int gColor = (color >> 8) & 0xff;
        final int bColor = color & 0xff;

        for (final Integer colorInColl : colorsColl) {

            final int aColorInColl = (colorInColl >> 24) & 0xff;
            final int rColorInColl = (colorInColl >> 16) & 0xff;
            final int gColorInColl = (colorInColl >> 8) & 0xff;
            final int bColorInColl = colorInColl & 0xff;

            final double euclidianDistance =
                    Math.sqrt(Math.pow(aColorInColl - aColor, 2) +
                            Math.pow(rColorInColl - rColor, 2) +
                            Math.pow(gColorInColl - gColor, 2) +
                            Math.pow(bColorInColl - bColor, 2));

            if (Double.compare(euclidianDistance, COLOR_SIMILARITY_TOLERANCE) <= 0) {
                return true;
            }
        }

        return false;
    }

    public static boolean isThemeLight(@Nullable final Context context) {
        if (context == null) {
            Utils.reportException(null, new DDWRTCompanionException() {
                @Override
                public String getMessage() {
                    return "Context is NULL";
                }
            });
            return false;
        }
        return (context.getSharedPreferences(RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
                .getLong(THEMING_PREF, DEFAULT_THEME) == LIGHT_THEME);
    }

    public static <T extends ContextWrapper> void setAppTheme(
            @NonNull final T activity, @Nullable final RouterFirmware routerFirmware,
            final boolean transparentStatusBar) {

        boolean useDefaultStyle = (routerFirmware == null ||
                RouterFirmware.AUTO == routerFirmware ||
                RouterFirmware.UNKNOWN == routerFirmware);
        if (useDefaultStyle) {
            setDefaultTheme(activity, transparentStatusBar);
        } else {
            final boolean themeLight = isThemeLight(activity);
            try {
                //Determine style by intropsection
                @StyleRes final int styleResId = Utils.getResId(
                        String.format("%s_AppTheme%s%s",
                                routerFirmware.name(),
                                themeLight ? "Light" : "Dark",
                                transparentStatusBar ? "_StatusBarTransparent" : ""),
                        R.style.class);
                activity.setTheme(styleResId);
            } catch (final Exception e) {
                Crashlytics.logException(e);
                setDefaultTheme(activity, transparentStatusBar);
            }
        }
    }

    public static <T extends ContextWrapper> void setDefaultTheme(
            @NonNull T activity, boolean transparentStatusBar) {

        if (isThemeLight(activity)) {
            activity.setTheme(transparentStatusBar ?
                    R.style.AppThemeLight_StatusBarTransparent : R.style.AppThemeLight);
        } else {
            activity.setTheme(transparentStatusBar ?
                    R.style.AppThemeDark_StatusBarTransparent : R.style.AppThemeDark);
        }
    }
}
