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

package org.rm3l.ddwrt.prefs.sort;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.collect.Maps;

import org.rm3l.ddwrt.fragments.DDWRTBaseFragment;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Abstract Sorting strategy: Indicates the strategy to use for sorting tabs in the UI
 */
public abstract class SortingStrategy {

    public static final String DEFAULT = DDWRTSortingStrategy.class.getSimpleName();

    @NonNull
    public abstract String getDisplayName();

    @NonNull
    public abstract String getShortDescription();

    @NonNull
    public final DDWRTBaseFragment[] sort(@NonNull final DDWRTBaseFragment[] tabs) {
        if (doCompare()) {

            final TreeMap<String, DDWRTBaseFragment> tabsMap = Maps.newTreeMap(this.getComparator());
            for (int i = 0; i < tabs.length; i++) {
                final DDWRTBaseFragment tab = tabs[i];
                tabsMap.put(tab.getTabTitle().toString(), tab);
            }

            final DDWRTBaseFragment[] output = new DDWRTBaseFragment[tabsMap.size()];
            int j = 0;
            for (Map.Entry<String, DDWRTBaseFragment> DDWRTBaseFragmentEntry : tabsMap.entrySet()) {
                output[j++] = DDWRTBaseFragmentEntry.getValue();
            }
            return output;
        }

        return tabs;

    }

    @Nullable
    protected abstract Comparator<String> getComparator();

    protected abstract boolean doCompare();
}
