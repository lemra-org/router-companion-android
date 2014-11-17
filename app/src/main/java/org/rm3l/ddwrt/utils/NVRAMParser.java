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

package org.rm3l.ddwrt.utils;

import com.google.common.base.Splitter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.api.conn.NVRAMInfo;

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
