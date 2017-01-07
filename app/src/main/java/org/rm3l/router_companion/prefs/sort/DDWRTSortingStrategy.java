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

package org.rm3l.router_companion.prefs.sort;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Comparator;

/**
 * DDWRT-style order
 */
public class DDWRTSortingStrategy extends SortingStrategy {
    @NonNull
    @Override
    public String getDisplayName() {
        return "DD-WRT style";
    }

    @NonNull
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
