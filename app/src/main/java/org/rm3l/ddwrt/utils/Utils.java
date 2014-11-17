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

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.api.conn.Router;
import org.rm3l.ddwrt.donate.DonateActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

/**
 * General utilities
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

    @NotNull
    public static AlertDialog buildAlertDialog(@NotNull final Context context, @Nullable final String title, @NotNull final String msg,
                                               final boolean cancelable, final boolean cancelableOnTouchOutside) {
        @NotNull final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        if (!Strings.isNullOrEmpty(title)) {
            alertDialog.setTitle(title);
        }
        alertDialog.setMessage(msg);
        alertDialog.setCancelable(cancelable);
        alertDialog.setCanceledOnTouchOutside(cancelableOnTouchOutside);

        return alertDialog;
    }

    public static void openDonateActivity(@NotNull final Context context) {
        final Intent donateIntent = new Intent(context, DonateActivity.class);
        context.startActivity(donateIntent);
    }

    @NotNull
    public static String intToIp(final int i) {

        return (i & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                ((i >> 24) & 0xFF);
    }

    @NotNull
    public static List<Router> dbIdsToPosition(@NotNull final List<Router> routersList) {
        final List<Router> routers = Lists.newArrayListWithCapacity(routersList.size());

        int i = 0;
        for (final Router router : routersList) {
            @NotNull final Router r = new Router(router);
            r.setId(i++);
            routers.add(r);
        }
        return routers;
    }
}
