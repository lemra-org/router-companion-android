package org.lemra.dd_wrt.utils;

import com.google.common.collect.Lists;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

/**
 * Created by armel on 8/9/14.
 */
public final class Utils {

    public static final String TAG = Utils.class.getSimpleName();

    private Utils() {
    }

    public static void readAll(@NotNull BufferedReader bufferedReader, @NotNull StringBuffer result) throws IOException {
        for (String line; (line = bufferedReader.readLine()) != null; ) {
            result.append(line);
        }
    }

    @Nullable
    public static String[] getLines(@NotNull BufferedReader bufferedReader) throws IOException {
        final List<String> lines = Lists.newArrayList();
        for (String line; (line = bufferedReader.readLine()) != null; ) {
            lines.add(line);
        }
        return lines.toArray(new String[lines.size()]);
    }
}
