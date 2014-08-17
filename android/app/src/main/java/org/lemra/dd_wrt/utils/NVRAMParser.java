package org.lemra.dd_wrt.utils;

import com.google.common.base.Splitter;

import org.jetbrains.annotations.Nullable;
import org.lemra.dd_wrt.api.conn.NVRAMInfo;

import java.util.List;

/**
 * Created by armel on 8/16/14.
 */
public final class NVRAMParser {

    public static final Splitter SPLITTER = Splitter.on("=").trimResults().omitEmptyStrings();

    private NVRAMParser() {}

    @Nullable
    public static NVRAMInfo parseNVRAMOutput(@Nullable final String[] nvramLines) {
        if (nvramLines == null || nvramLines.length == 0) {
            return null;
        }

        final NVRAMInfo nvramInfo = new NVRAMInfo();

        for (final String nvramLine : nvramLines) {
            final List<String> strings = SPLITTER.splitToList(nvramLine);
            if (strings.size() >= 2) {
                nvramInfo.setProperty(strings.get(0), strings.get(1));
            }
        }

        return nvramInfo;
    }
}
