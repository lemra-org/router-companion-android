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

package org.rm3l.ddwrt.utils;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.util.LruCache;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import org.rm3l.ddwrt.R;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_THEME;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.THEMING_PREF;

public final class ColorUtils {

    public static final long LIGHT_THEME = 30l;
    public static final long DARK_THEME = DEFAULT_THEME;
    @Deprecated
    private static final BiMap<Integer, Integer> colorToTheme = HashBiMap.create();

    static {
        colorToTheme.put(R.color.cardview_light_background, 30); //Light
        colorToTheme.put(R.color.cardview_shadow_end_color, Long.valueOf(DEFAULT_THEME).intValue()); //Dark
    }

    private static final Random RANDOM_COLOR_GEN = new Random();
    private static final double COLOR_SIMILARITY_TOLERANCE = 100;
    private static final LruCache<String, Integer> colorsCache = new LruCache<String, Integer>(20) {
        @Override
        protected Integer create(final String key) {
            final Map<String, Integer> currentItems = snapshot();
            final Set<Integer> colorsToSkip = new HashSet<>();

            //We want our new color not to be similar to white or black
            colorsToSkip.add(Color.argb(255, 0, 0, 0));
            colorsToSkip.add(Color.argb(255, 255, 255, 255));

            if (!currentItems.isEmpty()) {
                colorsToSkip.addAll(currentItems.values());
            }

            return genColor(colorsToSkip);
        }
    };

    private ColorUtils() {
    }

    public static int getColor(@NonNull final String keyInCache) {
        return colorsCache.get(keyInCache);
    }

    public static int genColor(@NonNull final Collection<Integer> colorsToSkip) {
        //Generate a Random Color, excluding colors similar to the colors specified
        int aNextColor;
        int rNextColor;
        int gNextColor;
        int bNextColor;

        int newColor;
        do {
            aNextColor = 255;
            rNextColor = 1 + RANDOM_COLOR_GEN.nextInt(254);
            gNextColor = 1 + RANDOM_COLOR_GEN.nextInt(254);
            bNextColor = 1 + RANDOM_COLOR_GEN.nextInt(254);
            newColor = Color.argb(aNextColor, rNextColor, gNextColor, bNextColor);

        } while (isColorSimilarToAtLeastOne(newColor, colorsToSkip));

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

    public static boolean isThemeLight(@NonNull final Context context) {
        return (context.getSharedPreferences(DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
                .getLong(THEMING_PREF, DEFAULT_THEME) == LIGHT_THEME);
    }
}
