package org.lemra.dd_wrt.utils;

import android.util.Log;

import com.google.common.collect.Lists;

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

    public static void readAll(BufferedReader bufferedReader, StringBuffer result) throws IOException {
        for (String line; (line = bufferedReader.readLine()) != null; ) {
            Log.d(TAG, "readAll: line=[" + line + "]");
            result.append(line);
        }
    }

    @Nullable
    public static String[] getLines(BufferedReader bufferedReader) throws IOException {
        final List<String> lines = Lists.newArrayList();
        for (String line; (line = bufferedReader.readLine()) != null; ) {
            Log.d(TAG, "readAll: line=[" + line + "]");
            lines.add(line);
        }
        return lines.toArray(new String[lines.size()]);
    }
}
