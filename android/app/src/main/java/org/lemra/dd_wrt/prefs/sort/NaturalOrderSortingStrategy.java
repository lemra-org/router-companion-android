package org.lemra.dd_wrt.prefs.sort;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

/**
 * Created by armel on 8/10/14.
 */
@SuppressWarnings("unused")
public class NaturalOrderSortingStrategy extends SortingStrategy {
    @NotNull
    @Override
    public String getDisplayName() {
        return "Natural Ordering";
    }

    @NotNull
    @Override
    public String getShortDescription() {
        return "Alphabetical";
    }

    @Nullable
    @Override
    protected Comparator<String> getComparator() {
        return null;
    }

    @Override
    protected boolean doCompare() {
        return true;
    }
}
