package org.lemra.dd_wrt.prefs.sort;

import com.google.common.collect.Maps;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import static org.lemra.dd_wrt.DDWRTManagementActivity.DDWRTSectionTabFragment;

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
    public final DDWRTSectionTabFragment[] sort(@NotNull final DDWRTSectionTabFragment[] tabs) {
        if (doCompare()) {

            final TreeMap<String, DDWRTSectionTabFragment> tabsMap = Maps.newTreeMap(this.getComparator());
            for (int i = 0; i < tabs.length; i++) {
                final DDWRTSectionTabFragment tab = tabs[i];
                tabsMap.put(tab.getTabTitle().toString(), tab);
            }

            final DDWRTSectionTabFragment[] output = new DDWRTSectionTabFragment[tabsMap.size()];
            int j = 0;
            for (Map.Entry<String, DDWRTSectionTabFragment> ddwrtSectionTabFragmentEntry : tabsMap.entrySet()) {
                output[j++] = ddwrtSectionTabFragmentEntry.getValue();
            }
            return output;
        }

        return tabs;

    }

    @Nullable
    protected abstract Comparator<String> getComparator();

    protected abstract boolean doCompare();
}
