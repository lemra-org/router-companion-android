/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Armel S.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.lemra.dd_wrt.prefs.sort;

import com.google.common.collect.Maps;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lemra.dd_wrt.fragments.DDWRTBaseFragment;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Abstract Sorting strategy: Indicates the strategy to use for sorting tabs in the UI
 */
public abstract class SortingStrategy {

    public static final String DEFAULT = DDWRTSortingStrategy.class.getSimpleName();

    @NotNull
    public abstract String getDisplayName();

    @NotNull
    public abstract String getShortDescription();

    @NotNull
    public final DDWRTBaseFragment[] sort(@NotNull final DDWRTBaseFragment[] tabs) {
        if (doCompare()) {

            final TreeMap<String, DDWRTBaseFragment> tabsMap = Maps.newTreeMap(this.getComparator());
            for (int i = 0; i < tabs.length; i++) {
                final DDWRTBaseFragment tab = tabs[i];
                tabsMap.put(tab.getTabTitle().toString(), tab);
            }

            @NotNull final DDWRTBaseFragment[] output = new DDWRTBaseFragment[tabsMap.size()];
            int j = 0;
            for (@NotNull Map.Entry<String, DDWRTBaseFragment> DDWRTBaseFragmentEntry : tabsMap.entrySet()) {
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
