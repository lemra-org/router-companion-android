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

import com.google.common.base.Splitter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;

import java.util.List;

/**
 * Parser utilities for manipulating result of remote command execution
 */
public final class NVRAMParser {

    public static final Splitter SPLITTER = Splitter.on("=").trimResults().omitEmptyStrings();

    private NVRAMParser() {
    }

    @Nullable
    public static NVRAMInfo parseNVRAMOutput(@Nullable final String[] nvramLines) {
        if (nvramLines == null || nvramLines.length == 0) {
            return null;
        }

        @NotNull final NVRAMInfo nvramInfo = new NVRAMInfo();

        for (@NotNull final String nvramLine : nvramLines) {
            final List<String> strings = SPLITTER.splitToList(nvramLine);
            if (strings.size() >= 2) {
                nvramInfo.setProperty(strings.get(0), strings.get(1));
            }
        }

        return nvramInfo;
    }
}
