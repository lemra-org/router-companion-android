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

import static android.content.Context.MODE_PRIVATE;
import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.router_companion.RouterCompanionAppConstants.AD_FREE_APP_APPLICATION_ID;
import static org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.router_companion.RouterCompanionAppConstants.EMPTY_STRING;
import static org.rm3l.router_companion.RouterCompanionAppConstants.FIRST_APP_LAUNCH_PREF_KEY;
import static org.rm3l.router_companion.RouterCompanionAppConstants.IS_FIRST_LAUNCH_PREF_KEY;
import static org.rm3l.router_companion.RouterCompanionAppConstants.OLD_IS_FIRST_LAUNCH_PREF_KEY;
import static org.rm3l.router_companion.RouterCompanionAppConstants.WAN_CYCLE_DAY_PREF;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ApplicationErrorReport;
import android.app.backup.BackupManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v4.util.Pair;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Toast;
import cn.nekocode.resinspector.ResourceInspector;
import com.crashlytics.android.Crashlytics;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Transformation;
import fr.nicolaspomepuy.discreetapprate.AppRate;
import fr.nicolaspomepuy.discreetapprate.AppRateTheme;
import fr.nicolaspomepuy.discreetapprate.RetryPolicy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.maoni.Maoni;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.RouterCompanionApplication;
import org.rm3l.router_companion.donate.DonateActivity;
import org.rm3l.router_companion.exceptions.DDWRTCompanionException;
import org.rm3l.router_companion.exceptions.DDWRTDataSyncOnMobileNetworkNotAllowedException;
import org.rm3l.router_companion.feedback.maoni.MaoniFeedbackHandler;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.resources.PublicIPInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.kotlin.ContextUtils;
import org.rm3l.router_companion.utils.snackbar.SnackbarCallback;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style;

/**
 * General utilities
 */
public final class Utils {

    protected static final class BugReportException extends DDWRTCompanionException {

        public BugReportException() {
        }

        public BugReportException(@Nullable String detailMessage, @Nullable Throwable throwable) {
            super(detailMessage, throwable);
        }
    }

    protected static final class RateAppClickedException extends DDWRTCompanionException {

    }

    protected static final class RateAppDismissedException extends DDWRTCompanionException {

    }

    public static final String TAG = Utils.class.getSimpleName();

    public static final Random RANDOM = new Random();

    public static final String BEHAVIOR = "Behavior";

    public static final String UTF_8 = "UTF-8";

    public static final Pattern MAC_ADDRESS = Pattern.compile("([\\da-fA-F]{2}(?:\\:|-|$)){6}");

    public static final Splitter COMMA_SPLITTER = Splitter.on(",").omitEmptyStrings().trimResults();

    public static final Splitter SPACE_SPLITTER = Splitter.on(" ").omitEmptyStrings().trimResults();

    static final byte[] HEX_CHAR_TABLE = {
            (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6',
            (byte) '7', (byte) '8', (byte) '9', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd',
            (byte) 'e', (byte) 'f'
    };

    private static AtomicLong nextLoaderId = new AtomicLong(1);

    @NonNull
    public static AlertDialog buildAlertDialog(@NonNull final Context context,
            @Nullable final String title, @NonNull final String msg, final boolean cancelable,
            final boolean cancelableOnTouchOutside) {
        final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        if (!isNullOrEmpty(title)) {
            alertDialog.setTitle(title);
        }
        alertDialog.setMessage(msg);
        alertDialog.setCancelable(cancelable);
        alertDialog.setCanceledOnTouchOutside(cancelableOnTouchOutside);

        return alertDialog;
    }

    public static boolean canUseDataConnection(@Nullable final Context ctx) {
        if (ctx == null) {
            return true;
        }
        final long dataUsageCtrl =
                ctx.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, MODE_PRIVATE)
                        .getLong(RouterCompanionAppConstants.DATA_USAGE_NETWORK_PREF, 444);
        if (dataUsageCtrl == 333) {
            //Only On Wi-Fi
            final ConnectivityManager connMgr = (ConnectivityManager) ctx.
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo activeNetworkInfo =
                    connMgr.getActiveNetworkInfo(); //default route to outgoing connections
            if (activeNetworkInfo == null) {
                Crashlytics.log(Log.DEBUG, TAG, "No active connection");
                throw new DDWRTCompanionException("An active network connection is needed");
            }

            //Just for debugging
            if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                Crashlytics.log(Log.DEBUG, TAG, "Active Network Connection Type: WIFI");
            } else if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                Crashlytics.log(Log.DEBUG, TAG, "Active Network Connection Type: MOBILE");
            }
            //END Debugging

            if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                //Forbid even if network is not connected,
                // because user has expressed the requirement not to use such network
                return false;
            }

            //            final NetworkInfo wifiNetworkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            //            final boolean isWifiConn = wifiNetworkInfo.isConnected();
            //            final NetworkInfo mobileNetworkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            //            final boolean isMobileConn = mobileNetworkInfo.isConnected();
            //            return !(isMobileConn && !isWifiConn);
        }
        return true;
    }

    public static void checkDataSyncAlllowedByUsagePreference(@Nullable final Context ctx) {
        if (ctx == null) {
            Utils.reportException(null, new IllegalStateException("ctx is NULL"));
            return;
        }

        if (!canUseDataConnection(ctx)) {
            throw new DDWRTDataSyncOnMobileNetworkNotAllowedException(
                    "Data Sync on Mobile Networks disabled!");
        }

        //        final long dataUsageCtrl = ctx.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, MODE_PRIVATE)
        //                .getLong(DDWRTCompanionConstants.DATA_USAGE_NETWORK_PREF, 444);
        //        if (dataUsageCtrl == 333) {
        //            //Only On Wi-Fi
        //            final ConnectivityManager connMgr = (ConnectivityManager) ctx.
        //                    getSystemService(Context.CONNECTIVITY_SERVICE);
        //            final NetworkInfo wifiNetworkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        //            final boolean isWifiConn = wifiNetworkInfo.isConnected();
        //            final NetworkInfo mobileNetworkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        //            final boolean isMobileConn = mobileNetworkInfo.isConnected();
        //            Crashlytics.log(Log.DEBUG, TAG, "Wifi connected: " + isWifiConn);
        //            Crashlytics.log(Log.DEBUG, TAG, "Mobile connected: " + isMobileConn);
        //            if (isMobileConn && !isWifiConn) {
        //                throw new DDWRTDataSyncOnMobileNetworkNotAllowedException
        //                        ("Data Sync on Mobile Networks disabled!");
        //            }
        //        }
        Crashlytics.log(Log.DEBUG, TAG, "Data Sync Allowed By Usage Preference!");
    }

    @NonNull
    public static String decimalToIp4(long ip) {
        return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24)
                & 0xFF);
        //        final StringBuilder result = new StringBuilder(15);
        //        for (int i = 0; i < 4; i++) {
        //            result.insert(0,Long.toString(ip & 0xff));
        //            if (i < 3) {
        //                result.insert(0,'.');
        //            }
        //            ip = ip >> 8;
        //        }
        //        return result.toString();
    }

    public static void displayMessage(@NonNull final Activity activity, final String msg,
            final Style style) {
        displayMessage(activity, msg, style, null);
    }

    public static void displayMessage(@NonNull final Activity activity, final String msg,
            final Style style, final ViewGroup viewGroup) {
        final int bgColor;
        if (style == null) {
            bgColor = R.color.win8_blue;
        } else {
            bgColor = style.bgColor;
        }

        displayMessage(activity, msg, bgColor, viewGroup);
    }

    public static void displayMessage(@NonNull final Activity activity, final String msg,
            final int bgColor) {
        displayMessage(activity, msg, bgColor, null);
    }

    public static void displayMessage(@NonNull final Activity activity, final String msg,
            final int bgColor, final ViewGroup viewGroup) {

        activity.runOnUiThread(new Runnable() {
            public void run() {
                SnackbarUtils.buildSnackbar(activity,
                        viewGroup != null ? viewGroup : activity.findViewById(android.R.id.content),
                        bgColor,
                        msg, Color.WHITE, null, Color.YELLOW, Snackbar.LENGTH_LONG, null, null, true);

                //                makeText(activity, msg, style, android.R.id.content).show();
            }
        });
    }

    public static void displayRatingBarIfNeeded(@Nullable final Activity activity) {
        if (activity == null) {
            return;
        }

        AppRate.with(activity)
                .fromTop(false)
                .debug(BuildConfig.DEBUG)
                .initialLaunchCount(RouterCompanionAppConstants.RATING_INITIAL_LAUNCH_COUNT)
                .retryPolicy(RetryPolicy.EXPONENTIAL)
                .listener(new AppRate.OnShowListener() {
                    @Override
                    public void onRateAppClicked() {
                        final Map<String, Object> eventMap = new HashMap<>();
                        eventMap.put(BEHAVIOR, "Clicked");
                        ReportingUtils.reportEvent(ReportingUtils.EVENT_RATING_INVITATON, eventMap);
                    }

                    @Override
                    public void onRateAppDismissed() {
                        final Map<String, Object> eventMap = new HashMap<>();
                        eventMap.put(BEHAVIOR, "Dismissed");
                        ReportingUtils.reportEvent(ReportingUtils.EVENT_RATING_INVITATON, eventMap);
                    }

                    @Override
                    public void onRateAppShowing(AppRate appRate, View view) {
                        Crashlytics.log(Log.DEBUG, TAG, "onRateAppShowing");
                    }
                })
                .theme(ColorUtils.Companion.isThemeLight(activity) ? AppRateTheme.DARK : AppRateTheme.LIGHT)
                .text(R.string.app_rate)
                .checkAndShow();
    }

    public static void displayUpgradeMessage(@NonNull final Context ctx, String featureTitle) {
        //Download the full version to unlock this version
        displayUpgradeMessage(ctx, featureTitle,
                "Unlock this feature by upgrading to the full-featured version "
                        + (BuildConfig.WITH_ADS ? " (ad-free)" : "")
                        + " on Google Play Store. \n\n"
                        + "Thank you for supporting this initiative!");
    }

    public static void displayUpgradeMessage(@NonNull final Context ctx,
            @Nullable final String featureTitle, @NonNull final String message) {

        final String dialogMsg =
                (message.replace("Thank you for supporting this initiative!", "")
                        + "More details on https://goo.gl/QnJB01\n\n"
                        + "Thank you for supporting this initiative!");

        //Download the full version to unlock this version
        new AlertDialog.Builder(ctx).setTitle(featureTitle)
                .setMessage(dialogMsg)
                .setCancelable(true)
                .setPositiveButton("Upgrade!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialogInterface, final int i) {
                        String url;
                        try {
                            //Check whether Google Play store is installed or not:
                            ctx.getPackageManager().getPackageInfo("com.android.vending", 0);
                            url = "market://details?id=" + AD_FREE_APP_APPLICATION_ID;
                        } catch (final Exception e) {
                            url = "https://play.google.com/store/apps/details?id=" + AD_FREE_APP_APPLICATION_ID;
                        }

                        //Open the app page in Google Play store:
                        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                            intent.addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
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
                })
                .create()
                .show();
    }

    public static void displayUpgradeMessageForAdsRemoval(@NonNull final Context ctx) {
        //Download the full version to unlock this version
        displayUpgradeMessage(ctx, "Go Premium",
                "Unlock all premium features "
                        + "by upgrading to the full-featured version "
                        + (BuildConfig.WITH_ADS ? " (ad-free)" : "")
                        + " on Google Play Store. \n\n"
                        + "Thank you for supporting this initiative!");
    }

    public static void downloadImageForRouter(@Nullable Context context,
            @NonNull final Router router, @Nullable final ImageView imageView,
            @Nullable final List<Transformation> transformations, @Nullable final Integer placeHolderRes,
            @Nullable final Integer errorPlaceHolderRes, @Nullable final String[] opts) {

        ImageUtils.downloadImageForRouter(context, router, imageView, transformations,
                placeHolderRes, errorPlaceHolderRes, opts);
    }

    public static void downloadImageFromUrl(@Nullable Context context, @Nullable final Uri url,
            @Nullable final ImageView imageView, @Nullable final Integer placeHolderDrawable,
            @Nullable final Integer errorPlaceHolderDrawable, @Nullable final Callback callback) {
        ImageUtils.downloadImageFromUrl(context, url, imageView, placeHolderDrawable,
                errorPlaceHolderDrawable, callback);
    }

    public static void downloadImageFromUrl(@Nullable Context context, @Nullable final String url,
            @Nullable final ImageView imageView, @Nullable final Integer placeHolderDrawable,
            @Nullable final Integer errorPlaceHolderDrawable, @Nullable final Callback callback) {
        ImageUtils.downloadImageFromUrl(context, url != null ? Uri.parse(url) : null, imageView, placeHolderDrawable,
                errorPlaceHolderDrawable, callback);
    }

    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String source) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(source);
        }
    }

    @Nullable
    public static String getAppOriginInstallerPackageName(@NonNull final Context context) {
        try {
            return context.getPackageManager().getInstallerPackageName(context.getPackageName());
        } catch (final Exception e) {
            //just in case...
            Utils.reportException(null, e);
            return null;
        }
    }

    @Nullable
    public static CharSequence getApplicationName(@Nullable Context context) {
        if (context == null) {
            return null;
        }
        return context.getApplicationInfo().loadLabel(context.getPackageManager());
    }

    public static Context getBaseContextToAttach(final Context rootCtx, @Nullable final Context base) {
        final Context newBase;
        if (BuildConfig.DEBUG && RouterCompanionApplication.isDebugResourceInspectorEnabled()) {
            newBase = ResourceInspector.wrap(base);
        } else {
            newBase = base;
        }
        return newBase;
    }

    @Nullable
    public static InetAddress getBroadcastAddress(@Nullable final WifiManager wifiManager)
            throws IOException {
        if (wifiManager == null) {
            return null;
        }
        final DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        if (dhcpInfo == null) {
            return null;
        }
        final int broadcast = (dhcpInfo.ipAddress & dhcpInfo.netmask) | ~dhcpInfo.netmask;
        final byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++) {
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        }
        return InetAddress.getByAddress(quads);
    }

    public static String getCommandForInternetIPResolution(Context context) {
        return getCommandForInternetIPResolution(context, "/usr/bin/nc");
    }

    public static String getCommandForInternetIPResolution(Context context, @Nullable final String ncCmdPath) {
        final CharSequence applicationName = Utils.getApplicationName(context);
        //"echo -e \"GET / HTTP/1.1\\r\\nHost:icanhazip.com\\r\\nUser-Agent:DD-WRT Companion/3.3.0\\r\\n\" | nc icanhazip.com 80"
        return String.format("(echo -e \""
                        + "GET / HTTP/1.1\\r\\n"
                        + "Host:%s\\r\\n"
                        + "User-Agent:%s/%s\\r\\n\"; sleep 1) "
                        + "| %s %s %d", PublicIPInfo.Companion.getICANHAZIP_HOST(),
                applicationName != null ? applicationName : BuildConfig.APPLICATION_ID,
                BuildConfig.VERSION_NAME,
                (ncCmdPath == null || ncCmdPath.trim().isEmpty()) ? "/usr/bin/nc" : ncCmdPath,
                PublicIPInfo.Companion.getICANHAZIP_HOST(), PublicIPInfo.Companion.getICANHAZIP_PORT());
    }

    public static String getCommandForRevDnsPtrResolution(Context context) {
        return getCommandForRevDnsPtrResolution(context, "/usr/bin/nc");
    }

    public static String getCommandForRevDnsPtrResolution(Context context, @Nullable final String ncCmdPath) {
        final CharSequence applicationName = Utils.getApplicationName(context);
        //"echo -e \"GET / HTTP/1.1\\r\\nHost:icanhazip.com\\r\\nUser-Agent:DD-WRT Companion/3.3.0\\r\\n\" | nc icanhazip.com 80"
        return String.format("(echo -e \""
                        + "GET / HTTP/1.1\\r\\n"
                        + "Host:%s\\r\\n"
                        + "User-Agent:%s/%s\\r\\n\"; sleep 1) "
                        + "| %s %s %d", PublicIPInfo.Companion.getICANHAZPTR_HOST(),
                applicationName != null ? applicationName : BuildConfig.APPLICATION_ID,
                BuildConfig.VERSION_NAME,
                (ncCmdPath == null || ncCmdPath.trim().isEmpty()) ? "/usr/bin/nc" : ncCmdPath,
                PublicIPInfo.Companion.getICANHAZPTR_HOST(), PublicIPInfo.Companion.getICANHAZPTR_PORT());
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

    @NonNull
    public static SharedPreferences getGlobalSharedPreferences(@NonNull final Context ctx) {
        return ctx.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, MODE_PRIVATE);
    }

    public static String getHexString(byte[] raw) {
        byte[] hex = new byte[2 * raw.length];
        int index = 0;

        for (byte b : raw) {
            int v = b & 0xFF;
            hex[index++] = HEX_CHAR_TABLE[v >>> 4];
            hex[index++] = HEX_CHAR_TABLE[v & 0xF];
        }
        return new String(hex, com.google.common.base.Charsets.UTF_8);
    }

    public static String getHexString(short[] raw) {
        byte[] hex = new byte[2 * raw.length];
        int index = 0;

        for (short b : raw) {
            int v = b & 0xFF;
            hex[index++] = HEX_CHAR_TABLE[v >>> 4];
            hex[index++] = HEX_CHAR_TABLE[v & 0xF];
        }
        return new String(hex, com.google.common.base.Charsets.UTF_8);
    }

    public static String getHexString(short raw) {
        byte[] hex = new byte[2];
        int v = raw & 0xFF;
        hex[0] = HEX_CHAR_TABLE[v >>> 4];
        hex[1] = HEX_CHAR_TABLE[v & 0xF];
        return new String(hex, com.google.common.base.Charsets.UTF_8);
    }

    @Nullable
    public static View getLineView(@Nullable Context ctx) {

        if (ctx == null) {
            return null;
        }

        final Resources resources = ctx.getResources();

        final DisplayMetrics displayMetrics = resources.getDisplayMetrics();

        final float height = TypedValue.applyDimension(COMPLEX_UNIT_DIP, 1.0f, displayMetrics);

        final View lineView = new View(ctx);
        lineView.setLayoutParams(
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) height));
        lineView.setBackgroundColor(ContextCompat.getColor(ctx, R.color.line_view_color));
        return lineView;
    }

    @NonNull
    public static String[] getLines(@NonNull BufferedReader bufferedReader)
            throws IOException {
        final List<String> lines = Lists.newArrayList();
        for (String line; (line = bufferedReader.readLine()) != null; ) {
            lines.add(line);
        }
        return lines.toArray(new String[lines.size()]);
    }

    public static long getNextLoaderId() {
        return nextLoaderId.getAndIncrement();
    }

    public static int getRandomIntId(final int upperLimit) {
        return RANDOM.nextInt(upperLimit);
    }

    public static int getResId(String resourceName, Class<?> clazz) {
        try {
            final Field idField = clazz.getDeclaredField(resourceName);
            return idField.getInt(idField);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Field not found: " + clazz + "#" + resourceName);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Access exception: " + clazz + "#" + resourceName);
        }
    }

    public static String getShareIntentFooter() {
        return String.format("<br/><br/>-- Generated by '<a href=\"%s\">DD-WRT Companion</a>'",
                RouterCompanionAppConstants.SUPPORT_WEBSITE);
    }

    // A method to find height of the status bar
    @Nullable
    public static Integer getStatusBarHeight(@NonNull final Context context) {
        Integer result = null;
        final Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static int getWanCycleDay(final SharedPreferences routerPreferences) {
        final int wanCycleDay;
        if (routerPreferences != null) {
            final int cycleDay = routerPreferences.getInt(WAN_CYCLE_DAY_PREF, 1);
            wanCycleDay = (cycleDay < 1 ? 1 : (cycleDay > 31 ? 31 : cycleDay));
        } else {
            wanCycleDay = 1;
        }
        return wanCycleDay;
    }

    @Nullable
    public static String getWifiName(Context context) {
        if (context == null) {
            return null;
        }
        final ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return null;
        }

        final NetworkInfo myNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (myNetworkInfo == null || myNetworkInfo.getType() != ConnectivityManager.TYPE_WIFI) {
            return null;
        }

        //        final NetworkInfo myNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (!myNetworkInfo.isConnected()) {
            return null;
        }

        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return null;
        }
        final WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            return null;
        }
        String ssid = wifiInfo.getSSID();
        if (ssid == null || "<unknown ssid>".equals(ssid)) {
            //Try using extra-info
            final String extraInfo = myNetworkInfo.getExtraInfo();
            final int length;
            if (extraInfo != null && (length = extraInfo.length()) >= 2) {
                ssid = extraInfo.substring(1, length -1);
            }
        }
        return ssid;
    }

    @Nullable
    public static String guessAppropriateEncoding(@NonNull final CharSequence contents) {
        // Very crude at the moment
        for (int i = 0; i < contents.length(); i++) {
            if (contents.charAt(i) > 0xFF) {
                return UTF_8;
            }
        }
        return null;
    }

    @NonNull
    public static Pair<String, String> handleException(@Nullable final Exception exception) {
        if (exception == null) {
            return Pair.create(EMPTY_STRING, EMPTY_STRING);
        }
        final Throwable rootCause = Throwables.getRootCause(exception);
        final String exceptionMessage = nullToEmpty(exception.getMessage());
        if (rootCause == null) {
            return Pair.create(exceptionMessage, exceptionMessage);
        }
        final String rootCauseMessage = rootCause.getMessage();
        if (isNullOrEmpty(rootCauseMessage)) {
            return Pair.create(exceptionMessage, exceptionMessage);
        }
        return Pair.create(rootCauseMessage, Throwables.getRootCause(exception).getMessage());
    }

    public static void hideSoftKeyboard(@Nullable final Activity activity) {
        if (activity == null) {
            return;
        }
        final InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager == null) {
            return;
        }
        final View currentFocus = activity.getCurrentFocus();
        if (currentFocus == null) {
            return;
        }
        inputMethodManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
    }

    @NonNull
    public static long ip4ToDecimal(@NonNull final int[] ipParts) {
        if (ipParts.length != 4) {
            throw new IllegalArgumentException("Invalid IPv4");
        }
        int result = 0;

        for (int i = 3; i >= 0; i--) {
            final int ip = ipParts[3 - i];
            checkIp4Part(ip);

            //left shifting 24,16,8,0 and bitwise OR

            //1. 192 << 24
            //1. 168 << 16
            //1. 1   << 8
            //1. 2   << 0
            result |= ip << (i * 8);
        }

        /*
        //Another way of doing this
        for (int i = 0, ipPartsLength = ipParts.length; i < ipPartsLength; i++) {
            final int ipPart = ipParts[i];
            checkIp4Part(ipPart);
            final int power = 3 - i;
            result += ipPart * Math.pow(256, power);
        }

         */
        return result;
    }

    /**
     * Check if Chrome CustomTabs are supported.
     * Some devices don't have Chrome or it may not be
     * updated to a version where custom tabs is supported.
     *
     * @param context the context
     * @return whether custom tabs are supported
     */
    public static boolean isChromeCustomTabsSupported(@NonNull final Context context) {
        final Intent serviceIntent = new Intent("android.support.customtabs.action.CustomTabsService");
        serviceIntent.setPackage("com.android.chrome");

        final CustomTabsServiceConnection serviceConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(final ComponentName componentName,
                    final CustomTabsClient customTabsClient) {
            }

            @Override
            public void onServiceDisconnected(final ComponentName name) {
            }
        };

        final boolean customTabsSupported = context.bindService(serviceIntent, serviceConnection,
                Context.BIND_AUTO_CREATE | Context.BIND_WAIVE_PRIORITY);
        context.unbindService(serviceConnection);

        return customTabsSupported;
    }

    public static boolean isDemoRouter(@Nullable final Router router) {
        return (router != null && isDemoRouter(router.getRemoteIpAddress()));
    }

    public static boolean isDemoRouter(@Nullable final String routerReachableAddr) {
        return RouterCompanionAppConstants.DEMO_ROUTER_DNS.equals(routerReachableAddr);
    }

    public static boolean isNonDemoRouter(@Nullable final Router router) {
        return !isDemoRouter(router);
    }

    public static boolean isNonDemoRouter(@Nullable final String routerReachableAddr) {
        return !isDemoRouter(routerReachableAddr);
    }

    public static boolean isFirstLaunch(@NonNull final Context context) {
        final SharedPreferences defaultSharedPreferences =
                context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, MODE_PRIVATE);

        final boolean isFirstLaunch =
                defaultSharedPreferences.getBoolean(FIRST_APP_LAUNCH_PREF_KEY, true);
        Crashlytics.log(Log.INFO, TAG, "isFirstLaunch: " + isFirstLaunch);
        final SharedPreferences.Editor editor = defaultSharedPreferences.edit();
        if (isFirstLaunch) {
            //Store flag
            editor.remove(OLD_IS_FIRST_LAUNCH_PREF_KEY)
                    .remove(IS_FIRST_LAUNCH_PREF_KEY)
                    .putBoolean(FIRST_APP_LAUNCH_PREF_KEY, false);
        }
        editor.putString(RouterCompanionAppConstants.LAST_KNOWN_VERSION, BuildConfig.VERSION_NAME)
                .apply();

        return isFirstLaunch;
    }

    public static boolean isOnline(@NonNull final Context ctx) {
        final ConnectivityManager connMgr =
                (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    public static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    public static void launchAboutActivity(@NonNull final Context context) {
        new LibsBuilder().withFields(R.string.class.getFields())
                .withActivityTitle("About")
                //provide a style (optional) (LIGHT, DARK, LIGHT_DARK_TOOLBAR)
                .withActivityStyle(ContextUtils.isThemeLight(context) ?
                        Libs.ActivityStyle.LIGHT : Libs.ActivityStyle.DARK)
                //start the activity
                .start(context);
    }

    public static Spannable linkifyHtml(@NonNull final String html, final int linkifyMask) {
        final Spanned text = fromHtml(html);
        final URLSpan[] currentSpans = text.getSpans(0, text.length(), URLSpan.class);

        final SpannableString buffer = new SpannableString(text);

        Linkify.addLinks(buffer, linkifyMask);

        for (final URLSpan span : currentSpans) {
            int end = text.getSpanEnd(span);
            int start = text.getSpanStart(span);
            buffer.setSpan(span, start, end, 0);
        }

        return buffer;
    }

    public static void openDonateActivity(@NonNull final Context context) {
        final Intent donateIntent = new Intent(context, DonateActivity.class);
        context.startActivity(donateIntent);
    }

    public static void openFeedbackForm(final Activity activity, final Router router) {
        final MaoniFeedbackHandler handlerForMaoni = new MaoniFeedbackHandler(activity, router);
        final List<String> sharedPreferencesList = new ArrayList<>();
        sharedPreferencesList.add(RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY);
        if (router != null) {
            sharedPreferencesList.add(router.getPreferencesFile());
        }
        new Maoni.Builder(activity, RouterCompanionAppConstants.FILEPROVIDER_AUTHORITY).withTheme(
                ColorUtils.Companion.isThemeLight(activity) ? R.style.AppThemeLight_StatusBarTransparent
                        : R.style.AppThemeDark_StatusBarTransparent)
                .withSharedPreferences(sharedPreferencesList.toArray(new String[sharedPreferencesList.size()]))
                .withWindowTitle("Send Feedback")
                .withExtraLayout(R.layout.activity_feedback_maoni)
                .withHandler(handlerForMaoni)
                .build()
                .start(activity);
    }

    public static void openFeedbackForm(final Activity activity, final String routerUuid) {
        openFeedbackForm(activity, RouterManagementActivity.getDao(activity).getRouter(routerUuid));
    }

    public static void openFeedbackForm(final Activity activity) {
        openFeedbackForm(activity, (Router) null);
    }

    public static void openKeyboard(@Nullable final Activity activity, @Nullable final View view) {
        if (activity == null || view == null) {
            return;
        }
        final InputMethodManager imm =
                (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            // only will trigger it if no physical keyboard is open
            imm.showSoftInput(view, 0);
        }
    }

    public static void readAll(@NonNull BufferedReader bufferedReader, @NonNull StringBuffer result)
            throws IOException {
        for (String line; (line = bufferedReader.readLine()) != null; ) {
            result.append(line);
        }
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

    public static void reportException(@Nullable final Context context,
            @NonNull final Throwable error) {
        ReportingUtils.reportException(context, error);
    }

    public static void requestAppPermissions(@NonNull final Activity activity) {
        //Permission requests

        // WRITE_EXTERNAL_STORAGE (includes READ_EXTERNAL_STORAGE)
        final int rwExternalStoragePermissionCheck =
                PermissionChecker.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (rwExternalStoragePermissionCheck != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                SnackbarUtils.buildSnackbar(activity,
                        "Storage access is needed to reduce data usage and enable sharing.", "OK",
                        Snackbar.LENGTH_INDEFINITE, new SnackbarCallback() {
                            @Override
                            public void onDismissEventActionClick(int event, @Nullable Bundle bundle)
                                    throws Exception {
                                //Request permission
                                ActivityCompat.requestPermissions(activity,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        RouterCompanionAppConstants.Permissions.STORAGE);
                            }

                            @Override
                            public void onDismissEventConsecutive(int event, @Nullable Bundle bundle)
                                    throws Exception {

                            }

                            @Override
                            public void onDismissEventManual(int event, @Nullable Bundle bundle)
                                    throws Exception {

                            }

                            @Override
                            public void onDismissEventSwipe(int event, @Nullable Bundle bundle)
                                    throws Exception {

                            }

                            @Override
                            public void onDismissEventTimeout(int event, @Nullable Bundle bundle)
                                    throws Exception {

                            }

                            @Override
                            public void onShowEvent(@Nullable Bundle bundle) throws Exception {

                            }
                        }, null, true);
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        RouterCompanionAppConstants.Permissions.STORAGE);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    public static void requestBackup(@Nullable final Context ctx) {
        if (ctx == null) {
            return;
        }
        new BackupManager(ctx).dataChanged();
        //        if (ctx.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, MODE_PRIVATE)
        //                .getBoolean(DATA_SYNC_BACKUP_PREF, true)) {
        //            new BackupManager(ctx).dataChanged();
        //        } else {
        //            Crashlytics.log(Log.DEBUG, TAG, "Backup disabled by user!");
        //        }
    }

    public static void scrollToView(@Nullable final ScrollView scrollView,
            @Nullable final View view) {

        if (scrollView == null || view == null) {
            return;
        }

        // View needs a focus
        view.requestFocus();

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                scrollView.smoothScrollTo(0, view.getBottom());
            }
        });
    }

    public static void takeBugReport(@NonNull final Activity activity) {

        BugReportException bugReportException = new BugReportException();

        try {

            final ApplicationErrorReport report = new ApplicationErrorReport();
            report.packageName = report.processName = activity.getApplication().getPackageName();
            report.time = System.currentTimeMillis();
            report.type = ApplicationErrorReport.TYPE_CRASH;
            report.systemApp = false;

            final ApplicationErrorReport.CrashInfo crash = new ApplicationErrorReport.CrashInfo();
            crash.exceptionClassName = bugReportException.getClass().getSimpleName();
            crash.exceptionMessage = bugReportException.getMessage();

            final StringWriter writer = new StringWriter();
            PrintWriter printer = new PrintWriter(writer);
            bugReportException.printStackTrace(printer);

            crash.stackTrace = writer.toString();

            final StackTraceElement stack = bugReportException.getStackTrace()[0];
            crash.throwClassName = stack.getClassName();
            crash.throwFileName = stack.getFileName();
            crash.throwLineNumber = stack.getLineNumber();
            crash.throwMethodName = stack.getMethodName();

            report.crashInfo = crash;

            final Intent intent = new Intent(Intent.ACTION_APP_ERROR);
            intent.putExtra(Intent.EXTRA_BUG_REPORT, report);
            activity.startActivity(intent);
        } catch (final Exception e) {
            Toast.makeText(activity, "Internal Error - please try again later.", Toast.LENGTH_SHORT)
                    .show();
            bugReportException = new BugReportException(e.getMessage(), e);
        } finally {
            Utils.reportException(null, bugReportException);
        }
    }

    public static String toHumanReadableByteCount(final long sizeInBytes) {
        return org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(sizeInBytes);
    }

    @Nullable
    public static String truncateText(@Nullable final String str, final int maxLen,
            final int lenFromEnd) {
        int len;
        if (str != null && (len = str.length()) > maxLen) {
            return str.substring(0, maxLen - lenFromEnd - 3) + "..." + str.substring(len - lenFromEnd,
                    len);
        }
        return str;
    }

    @Nullable
    public static String truncateText(@Nullable final String str, final int maxLen) {
        return truncateText(str, maxLen, 0);
    }

    @Nullable
    public static String nullTo(@Nullable final String str, @Nullable final String to) {
        return str != null ? str : to;
    }

    @Nullable
    public static String nullOrEmptyTo(@Nullable final String str, @Nullable final String to) {
        return isNullOrEmpty(str) ? to : str;
    }

    private Utils() {
    }

    private static void checkIp4Part(final int ipPart) {
        if (ipPart < 0 || ipPart > 255) {
            throw new IllegalArgumentException("Invalid IPv4 part");
        }
    }
}