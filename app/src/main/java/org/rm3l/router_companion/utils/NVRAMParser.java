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

import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.router_companion.RouterCompanionAppConstants.EMPTY_STRING;

import android.support.annotation.Nullable;
import com.google.common.base.Splitter;
import java.util.List;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;

/**
 * Parser utilities for manipulating result of remote command execution
 */
public final class NVRAMParser {

    public static final Splitter SPLITTER = Splitter.on("=").limit(2).trimResults();

    @Nullable
    public static NVRAMInfo parseNVRAMOutput(@Nullable final String[] nvramLines) {
        if (nvramLines == null || nvramLines.length == 0) {
            return null;
        }

        final NVRAMInfo nvramInfo = new NVRAMInfo();

        int size;
        for (final String nvramLine : nvramLines) {
            if (nvramLine == null) {
                continue;
            }
            final List<String> strings = SPLITTER.splitToList(nvramLine);
            size = strings.size();
            if (size == 1) {
                nvramInfo.setProperty(strings.get(0), EMPTY_STRING);
            } else if (size >= 2) {
                nvramInfo.setProperty(strings.get(0), nullToEmpty(strings.get(1)));
            }
        }

        return nvramInfo;
    }

    private NVRAMParser() {
    }
}
