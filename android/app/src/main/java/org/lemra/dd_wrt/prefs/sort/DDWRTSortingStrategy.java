package org.lemra.dd_wrt.prefs.sort;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lemra.dd_wrt.DDWRTManagementActivity;

import java.util.Comparator;

/**
 * Created by armel on 8/10/14.
 */
public class DDWRTSortingStrategy extends SortingStrategy {
    @NotNull
    @Override
    public String getDisplayName() {
        return "DD-WRT style";
    }

    @NotNull
    @Override
    public String getShortDescription() {
        return "Same order as in DD-WRT Web Gui";
    }

    @Nullable
    @Override
    protected Comparator<String> getComparator() {
        return null;
    }

    @Override
    protected boolean doCompare() {
        /**
         * Already in the order we want
         */
        return false;
    }

}
