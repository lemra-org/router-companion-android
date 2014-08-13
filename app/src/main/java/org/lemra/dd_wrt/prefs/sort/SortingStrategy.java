package org.lemra.dd_wrt.prefs.sort;

import com.google.common.collect.Maps;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lemra.dd_wrt.fragments.DDWRTBaseFragment;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by armel on 8/10/14.
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
