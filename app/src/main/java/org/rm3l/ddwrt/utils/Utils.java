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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;

import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;

import org.acra.ACRA;
import org.apache.commons.io.FileUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.donate.DonateActivity;
import org.rm3l.ddwrt.resources.conn.Router;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import de.keyboardsurfer.android.widget.crouton.Style;

import static android.content.Context.MODE_PRIVATE;
import static de.keyboardsurfer.android.widget.crouton.Crouton.makeText;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_THEME;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.IS_FIRST_LAUNCH_PREF_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.OLD_IS_FIRST_LAUNCH_PREF_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.THEMING_PREF;

/**
 * General utilities
 */
public final class Utils {

    public static final String TAG = Utils.class.getSimpleName();
    private static final BiMap<Integer, Integer> colorToTheme = HashBiMap.create();

    static {
        colorToTheme.put(R.color.cardview_light_background, 30); //Light
        colorToTheme.put(R.color.cardview_shadow_end_color, Long.valueOf(DEFAULT_THEME).intValue()); //Dark
    }

    private static AtomicLong nextLoaderId = new AtomicLong(1);

    private Utils() {
    }

    public static int getThemeBackgroundColor(@NotNull final Context context, @NotNull final String routerUuid) {
        final long theme = context.getSharedPreferences(routerUuid, MODE_PRIVATE).getLong(THEMING_PREF, DEFAULT_THEME);
        final BiMap<Integer, Integer> colorToThemeInverse = colorToTheme.inverse();
        Integer color = colorToThemeInverse.get(Long.valueOf(theme).intValue());
        if (color == null) {
            color = colorToThemeInverse.get(Long.valueOf(DEFAULT_THEME).intValue());
        }
        return context.getResources().getColor(color);
    }

    public static boolean isThemeLight(@NotNull final Context context, @NotNull final String routerUuid) {
        return (getThemeBackgroundColor(context, routerUuid) ==
                context.getResources().getColor(R.color.cardview_light_background));
    }

    public static boolean isThemeLight(@NotNull final Context context, final int themeBackgroundColor) {
        return (themeBackgroundColor ==
                context.getResources().getColor(R.color.cardview_light_background));
    }

    public static long getNextLoaderId() {
        return nextLoaderId.getAndIncrement();
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

    public static void displayMessage(@NotNull final Activity activity, final String msg, final Style style) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                makeText(activity, msg, style).show();
            }
        });
    }

    @NotNull
    public static String intToIp(final int i) {

        return (i & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                ((i >> 24) & 0xFF);
    }

    @Nullable
    public static InetAddress getBroadcastAddress(@Nullable final WifiManager wifiManager) throws IOException {
        if (wifiManager == null) {
            return null;
        }
        final DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        if (dhcpInfo == null) {
            return null;
        }
        final int broadcast = (dhcpInfo.ipAddress & dhcpInfo.netmask)
                | ~dhcpInfo.netmask;
        final byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++) {
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        }
        return InetAddress.getByAddress(quads);
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

    public static String toHumanReadableByteCount(final long sizeInBytes) {
        return FileUtils.byteCountToDisplaySize(sizeInBytes);
    }

    public static void reportException(@NotNull final Throwable error) {
        ACRA.getErrorReporter().handleSilentException(error);
    }

    public static String removeLastChar(@Nullable final String str) {
        if (str == null) {
            return null;
        }
        if (str.isEmpty()) {
            return str;
        }
        return str.substring(0, str.length() - 1);
    }

    public static boolean isFirstLaunch(@NotNull final Context context) {
        final SharedPreferences defaultSharedPreferences = context
                .getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, MODE_PRIVATE);

        final boolean isFirstLaunch = defaultSharedPreferences.getBoolean(IS_FIRST_LAUNCH_PREF_KEY, true);
        Log.i(TAG, "isFirstLaunch: " + isFirstLaunch);
        if (isFirstLaunch) {
            //Store flag
            defaultSharedPreferences.edit()
                    .remove(OLD_IS_FIRST_LAUNCH_PREF_KEY)
                    .putBoolean(IS_FIRST_LAUNCH_PREF_KEY, false)
                    .apply();
        }
        return isFirstLaunch;
    }

    @NotNull
    public static DefaultHttpClient getThreadSafeClient() {
        final DefaultHttpClient client = new DefaultHttpClient();
        final ClientConnectionManager mgr = client.getConnectionManager();
        final HttpParams params = client.getParams();

        return new DefaultHttpClient(new ThreadSafeClientConnManager(params,
                mgr.getSchemeRegistry()), params);
    }

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));
    }

}
