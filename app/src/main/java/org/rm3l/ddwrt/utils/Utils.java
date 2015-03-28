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
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import org.acra.ACRA;
import org.apache.commons.io.FileUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;
import org.rm3l.ddwrt.donate.DonateActivity;
import org.rm3l.ddwrt.exceptions.DDWRTDataSyncOnMobileNetworkNotAllowedException;
import org.rm3l.ddwrt.resources.conn.Router;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import de.keyboardsurfer.android.widget.crouton.Style;

import static android.content.Context.MODE_PRIVATE;
import static de.keyboardsurfer.android.widget.crouton.Crouton.makeText;
import static org.rm3l.ddwrt.BuildConfig.APPLICATION_ID;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.IS_FIRST_LAUNCH_PREF_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.OLD_IS_FIRST_LAUNCH_PREF_KEY;

/**
 * General utilities
 */
public final class Utils {

    public static final String TAG = Utils.class.getSimpleName();

    private static AtomicLong nextLoaderId = new AtomicLong(1);

    private Utils() {
    }

    public static long getNextLoaderId() {
        return nextLoaderId.getAndIncrement();
    }

    public static void readAll(@NonNull BufferedReader bufferedReader, @NonNull StringBuffer result) throws IOException {
        for (String line; (line = bufferedReader.readLine()) != null; ) {
            result.append(line);
        }
    }

    @Nullable
    public static String[] getLines(@NonNull BufferedReader bufferedReader) throws IOException {
        final List<String> lines = Lists.newArrayList();
        for (String line; (line = bufferedReader.readLine()) != null; ) {
            lines.add(line);
        }
        return lines.toArray(new String[lines.size()]);
    }

    @NonNull
    public static AlertDialog buildAlertDialog(@NonNull final Context context, @Nullable final String title, @NonNull final String msg,
                                               final boolean cancelable, final boolean cancelableOnTouchOutside) {
        final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        if (!Strings.isNullOrEmpty(title)) {
            alertDialog.setTitle(title);
        }
        alertDialog.setMessage(msg);
        alertDialog.setCancelable(cancelable);
        alertDialog.setCanceledOnTouchOutside(cancelableOnTouchOutside);

        return alertDialog;
    }

    public static void openDonateActivity(@NonNull final Context context) {
        final Intent donateIntent = new Intent(context, DonateActivity.class);
        context.startActivity(donateIntent);
    }

    public static void displayMessage(@NonNull final Activity activity, final String msg, final Style style) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                makeText(activity, msg, style).show();
            }
        });
    }

    @NonNull
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

    @NonNull
    public static List<Router> dbIdsToPosition(@NonNull final List<Router> routersList) {
        final List<Router> routers = Lists.newArrayListWithCapacity(routersList.size());

        int i = 0;
        for (final Router router : routersList) {
            final Router r = new Router(router);
            r.setId(i++);
            routers.add(r);
        }
        return routers;
    }

    public static String toHumanReadableByteCount(final long sizeInBytes) {
        return FileUtils.byteCountToDisplaySize(sizeInBytes);
    }

    public static void reportException(@NonNull final Throwable error) {
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

    public static boolean isFirstLaunch(@NonNull final Context context) {
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

    @NonNull
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

    public static void displayUpgradeMessage(@NonNull final Context ctx) {
        //Download the full version to unlock this version
        new AlertDialog.Builder(ctx)
                .setMessage("Unlock this feature by upgrading to the full version on Google Play Store. \n\n" +
                        "Thank you for supporting this initiative!")
                .setCancelable(true)
                .setPositiveButton("Upgrade!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialogInterface, final int i) {
                        String url;
                        try {
                            //Check whether Google Play store is installed or not:
                            ctx.getPackageManager().getPackageInfo("com.android.vending", 0);
                            url = "market://details?id=" + APPLICATION_ID;
                        } catch (final Exception e) {
                            url = "https://play.google.com/store/apps/details?id=" + APPLICATION_ID;
                        }


                        //Open the app page in Google Play store:
                        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        } else {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                        }
                        ctx.startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Cancelled - nothing more to do!
                    }
                }).create().show();
    }

    public static boolean isOnline(@NonNull final Context ctx) {
        final ConnectivityManager connMgr = (ConnectivityManager) ctx
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    public static void checkDataSyncAlllowedByUsagePreference(@Nullable final Context ctx) {
        if (ctx == null) {
            Utils.reportException(new IllegalStateException("ctx is NULL"));
            return;
        }

        final long dataUsageCtrl = ctx.getSharedPreferences(DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
                .getLong(DDWRTCompanionConstants.DATA_USAGE_NETWORK_PREF, 444);
        if (dataUsageCtrl == 333) {
            //Only On Wi-Fi
            final ConnectivityManager connMgr = (ConnectivityManager) ctx.
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo wifiNetworkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            final boolean isWifiConn = wifiNetworkInfo.isConnected();
            final NetworkInfo mobileNetworkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            final boolean isMobileConn = mobileNetworkInfo.isConnected();
            Log.d(TAG, "Wifi connected: " + isWifiConn);
            Log.d(TAG, "Mobile connected: " + isMobileConn);
            if (isMobileConn && !isWifiConn) {
                throw new DDWRTDataSyncOnMobileNetworkNotAllowedException
                        ("Data Sync on Mobile Networks disabled!");
            }
        }
        Log.d(TAG, "Data Sync Allowed By Usage Preference!");
    }

    /**
     * Replaces any character that isn't a number, letter or underscore with an underscore
     *
     * @param filename the filename to escape
     * @return the filename escaped
     */
    @NonNull
    public static String getEscapedFileName(@NonNull final String filename) {
        return filename.replaceAll("\\W+", "_");
    }

    public static void requestBackup(@NonNull final Context ctx) {
        new BackupManager(ctx).dataChanged();
    }

}
