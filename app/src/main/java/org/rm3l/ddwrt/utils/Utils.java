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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ApplicationErrorReport;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
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
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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

import com.crashlytics.android.Crashlytics;
import com.google.common.collect.Lists;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Transformation;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.donate.DonateActivity;
import org.rm3l.ddwrt.exceptions.DDWRTCompanionException;
import org.rm3l.ddwrt.exceptions.DDWRTDataSyncOnMobileNetworkNotAllowedException;
import org.rm3l.ddwrt.exceptions.UserGeneratedReportException;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.snackbar.SnackbarCallback;
import org.rm3l.ddwrt.utils.snackbar.SnackbarUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import de.keyboardsurfer.android.widget.crouton.Style;
import fr.nicolaspomepuy.discreetapprate.AppRate;
import fr.nicolaspomepuy.discreetapprate.AppRateTheme;
import fr.nicolaspomepuy.discreetapprate.RetryPolicy;
import io.doorbell.android.Doorbell;

import static android.content.Context.MODE_PRIVATE;
import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Strings.isNullOrEmpty;
import static de.keyboardsurfer.android.widget.crouton.Crouton.makeText;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.AD_FREE_APP_APPLICATION_ID;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DOORBELL_APIKEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DOORBELL_APPID;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.FIRST_APP_LAUNCH_PREF_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.IS_FIRST_LAUNCH_PREF_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.OLD_IS_FIRST_LAUNCH_PREF_KEY;

/**
 * General utilities
 */
public final class Utils {

    public static final String TAG = Utils.class.getSimpleName();
    public static final Random RANDOM = new Random();
    public static final String BEHAVIOR = "Behavior";

    private static AtomicLong nextLoaderId = new AtomicLong(1);

    static final byte[] HEX_CHAR_TABLE = {(byte) '0', (byte) '1', (byte) '2',
            (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7',
            (byte) '8', (byte) '9', (byte) 'a', (byte) 'b', (byte) 'c',
            (byte) 'd', (byte) 'e', (byte) 'f'};

    public static final Pattern MAC_ADDRESS =
            Pattern.compile("([\\da-fA-F]{2}(?:\\:|-|$)){6}");

    private Utils() {
    }

    @Nullable
    public static CharSequence getApplicationName(@Nullable Context context) {
        if (context == null) {
            return null;
        }
        return context.getApplicationInfo().loadLabel(context.getPackageManager());
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
        if (!isNullOrEmpty(title)) {
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

    public static String toHumanReadableByteCount(final long sizeInBytes) {
        return FileUtils.byteCountToDisplaySize(sizeInBytes);
    }

    public static void reportException(
            @Nullable final Context context,
            @NonNull final Throwable error) {
        ReportingUtils.reportException(context, error);

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

        final boolean isFirstLaunch = defaultSharedPreferences.getBoolean(FIRST_APP_LAUNCH_PREF_KEY, true);
        Crashlytics.log(Log.INFO, TAG, "isFirstLaunch: " + isFirstLaunch);
        final SharedPreferences.Editor editor = defaultSharedPreferences.edit();
        if (isFirstLaunch) {
            //Store flag
            editor
                    .remove(OLD_IS_FIRST_LAUNCH_PREF_KEY)
                    .remove(IS_FIRST_LAUNCH_PREF_KEY)
                    .putBoolean(FIRST_APP_LAUNCH_PREF_KEY, false);
        }
        editor
                .putString(DDWRTCompanionConstants.LAST_KNOWN_VERSION, BuildConfig.VERSION_NAME)
                .apply();

        return isFirstLaunch;
    }

    @Nullable
    public static String getAppOriginInstallerPackageName(@NonNull final Context context) {
        try {
            return context.getPackageManager()
                    .getInstallerPackageName(context.getPackageName());
        } catch (final Exception e) {
            //just in case...
            Utils.reportException(null, e);
            return null;
        }
    }

    public static void displayUpgradeMessageForAdsRemoval(@NonNull final Context ctx) {
        //Download the full version to unlock this version
        displayUpgradeMessage(ctx, "Go Premium",
                "Unlock all premium features " +
                "by upgrading to the full-featured version " +
                (BuildConfig.WITH_ADS ? " (ad-free)" : "") + " on Google Play Store. \n\n" +
                "Thank you for supporting this initiative!");
    }

    public static void displayUpgradeMessage(@NonNull final Context ctx, String featureTitle) {
        //Download the full version to unlock this version
        displayUpgradeMessage(ctx, featureTitle, "Unlock this feature by upgrading to the full-featured version " +
                (BuildConfig.WITH_ADS ? " (ad-free)" : "") + " on Google Play Store. \n\n" +
                "Thank you for supporting this initiative!");
    }

    public static void displayUpgradeMessage(@NonNull final Context ctx, @Nullable final String featureTitle,
                                             @NonNull final String message) {

        final String dialogMsg =
                (StringUtils.replace(message, "Thank you for supporting this initiative!", "") +
                        "More details on https://goo.gl/QnJB01\n\n" +
                        "Thank you for supporting this initiative!");

        //Download the full version to unlock this version
        new AlertDialog.Builder(ctx)
                .setTitle(featureTitle)
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
            Utils.reportException(null, new IllegalStateException("ctx is NULL"));
            return;
        }

        if (!canUseDataConnection(ctx)) {
            throw new DDWRTDataSyncOnMobileNetworkNotAllowedException
                    ("Data Sync on Mobile Networks disabled!");
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

    public static boolean canUseDataConnection(@Nullable final Context ctx) {
        if (ctx == null) {
            return true;
        }
        final long dataUsageCtrl = ctx.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, MODE_PRIVATE)
                .getLong(DDWRTCompanionConstants.DATA_USAGE_NETWORK_PREF, 444);
        if (dataUsageCtrl == 333) {
            //Only On Wi-Fi
            final ConnectivityManager connMgr = (ConnectivityManager) ctx.
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo wifiNetworkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            final boolean isWifiConn = wifiNetworkInfo.isConnected();
            final NetworkInfo mobileNetworkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            final boolean isMobileConn = mobileNetworkInfo.isConnected();
            Crashlytics.log(Log.DEBUG, TAG, "Wifi connected: " + isWifiConn);
            Crashlytics.log(Log.DEBUG, TAG, "Mobile connected: " + isMobileConn);
            return !(isMobileConn && !isWifiConn);
        }
        return true;
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

    public static int getRandomIntId(final int upperLimit) {
        return RANDOM.nextInt(upperLimit);
    }

    public static Spannable linkifyHtml(@NonNull final String html, final int linkifyMask) {
        final Spanned text = Html.fromHtml(html);
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

    public static String getShareIntentFooter() {
        return String.format("<br/><br/>-- Generated by '<a href=\"%s\">DD-WRT Companion</a>'",
                DDWRTCompanionConstants.SUPPORT_WEBSITE);
    }

    @Nullable
    public static String getWifiName(Context context) {
        if (context == null) {
            return null;
        }
        final ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return null;
        }
        final NetworkInfo myNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (myNetworkInfo == null || !myNetworkInfo.isConnected()) {
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
        return wifiInfo.getSSID();
    }

    @Nullable
    public static View getLineView(@Nullable Context ctx) {

        if (ctx == null) {
            return null;
        }

        final Resources resources = ctx.getResources();

        final DisplayMetrics displayMetrics = resources.getDisplayMetrics();

        final float height = TypedValue.applyDimension(COMPLEX_UNIT_DIP, 1.0f,
                displayMetrics);

        final View lineView = new View(ctx);
        lineView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) height
        ));
        lineView.setBackgroundColor(
                ContextCompat.getColor(ctx, R.color.line_view_color));
        return lineView;
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

    public static AlertDialog.Builder buildFeedbackDialog(final Activity activity,
                                                          final boolean showDialog) {
        final Doorbell doorbellDialog = new Doorbell(activity,
                DOORBELL_APPID, DOORBELL_APIKEY); // Create the Doorbell object
        doorbellDialog.setPoweredByVisibility(View.GONE); // Hide the "Powered by Doorbell.io" text
        doorbellDialog.setTitle(R.string.send_feedback);
        doorbellDialog.setMessage(R.string.feedback_dialog_text);
        doorbellDialog.setEmailHint("Your email address");
        //Set user-defined email if any
        final String acraEmailAddr = activity
                .getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, MODE_PRIVATE)
                .getString(DDWRTCompanionConstants.ACRA_USER_EMAIL, null);
        doorbellDialog.setEmail(acraEmailAddr);
        doorbellDialog.setEmailFieldVisibility(View.VISIBLE);
        doorbellDialog.setMessageHint(R.string.feedback_dialog_comments_text);
        doorbellDialog.setPositiveButtonText(R.string.feedback_send);
        doorbellDialog.setNegativeButtonText(R.string.feedback_cancel);

        // Optionally add some properties
        final UUID randomUUID = UUID.randomUUID();
        doorbellDialog.addProperty("DIALOG_ID", randomUUID);
        doorbellDialog.addProperty("BUILD_DEBUG", BuildConfig.DEBUG);
        doorbellDialog.addProperty("BUILD_APPLICATION_ID", BuildConfig.APPLICATION_ID);
        doorbellDialog.addProperty("BUILD_VERSION_CODE", BuildConfig.VERSION_CODE);
        doorbellDialog.addProperty("BUILD_FLAVOR", BuildConfig.FLAVOR);
        doorbellDialog.addProperty("BUILD_TYPE", BuildConfig.BUILD_TYPE);
        doorbellDialog.addProperty("BUILD_VERSION_NAME", BuildConfig.VERSION_NAME);

        // Callback for when the dialog is shown
        doorbellDialog.setOnShowCallback(new io.doorbell.android.callbacks.OnShowCallback() {
            @Override
            public void handle() {
                //Generate a custom error-report (for ACRA)
                Utils.reportException(null, new
                        UserGeneratedReportException("Feedback " + randomUUID));
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            doorbellDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    Utils.displayRatingBarIfNeeded(activity);
                }
            });
        } else {
            Utils.displayRatingBarIfNeeded(activity);
        }

        if (showDialog) {
            doorbellDialog.show();
        }

        return doorbellDialog;
    }

    public static boolean isDemoRouter(@Nullable final Router router) {
        return (router != null && isDemoRouter(router.getRemoteIpAddress()));
    }

    public static boolean isDemoRouter(@Nullable final String routerReachableAddr) {
        return DDWRTCompanionConstants.DEMO_ROUTER_DNS.equals(routerReachableAddr);
    }

    public static void displayRatingBarIfNeeded(@Nullable final Activity activity) {
        if (activity == null) {
            return;
        }

        AppRate
                .with(activity)
                .fromTop(true)
                .debug(BuildConfig.DEBUG)
                .initialLaunchCount(DDWRTCompanionConstants.RATING_INITIAL_LAUNCH_COUNT)
                .retryPolicy(RetryPolicy.EXPONENTIAL)
                .listener(new AppRate.OnShowListener() {
                    @Override
                    public void onRateAppShowing(AppRate appRate, View view) {
                        Crashlytics.log(Log.DEBUG, TAG, "onRateAppShowing");
                    }

                    @Override
                    public void onRateAppDismissed() {
                        final Map<String, Object> eventMap = new HashMap<>();
                        eventMap.put(BEHAVIOR, "Dismissed");
                        ReportingUtils.reportEvent(ReportingUtils.EVENT_RATING_INVITATON, eventMap);
                    }

                    @Override
                    public void onRateAppClicked() {
                        final Map<String, Object> eventMap = new HashMap<>();
                        eventMap.put(BEHAVIOR, "Clicked");
                        ReportingUtils.reportEvent(ReportingUtils.EVENT_RATING_INVITATON, eventMap);
                    }
                })
                .theme(ColorUtils.isThemeLight(activity) ? AppRateTheme.DARK : AppRateTheme.LIGHT)
                .text(R.string.app_rate)
                .checkAndShow();
    }

    public static String getHexString(byte[] raw) {
        byte[] hex = new byte[2 * raw.length];
        int index = 0;

        for(byte b : raw){
            int v = b & 0xFF;
            hex[index++] = HEX_CHAR_TABLE[v >>> 4];
            hex[index++] = HEX_CHAR_TABLE[v & 0xF];
        }
        return new String(hex, UTF_8);
    }

    public static String getHexString(short[] raw) {
        byte[] hex = new byte[2 * raw.length];
        int index = 0;

        for(short b : raw){
            int v = b & 0xFF;
            hex[index++] = HEX_CHAR_TABLE[v >>> 4];
            hex[index++] = HEX_CHAR_TABLE[v & 0xF];
        }
        return new String(hex, UTF_8);
    }

    public static String getHexString(short raw){
        byte[] hex = new byte[2];
        int v = raw & 0xFF;
        hex[0] = HEX_CHAR_TABLE[v >>> 4];
        hex[1] = HEX_CHAR_TABLE[v & 0xF];
        return new String(hex, UTF_8);
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

    public static void downloadImageForRouter(@Nullable Context context,
                                              @NonNull final String routerModel,
                                              @Nullable final ImageView imageView,
                                              @Nullable final List<Transformation> transformations,
                                              @Nullable final Integer placeHolderRes,
                                              @Nullable final Integer errorPlaceHolderRes,
                                              @Nullable final String[] opts) {

        ImageUtils.downloadImageForRouter(context, routerModel, imageView,
                transformations, placeHolderRes, errorPlaceHolderRes, opts);

    }

    public static void downloadImageFromUrl(@Nullable Context context, @NonNull final String url,
                                            @Nullable final ImageView imageView,
                                            @Nullable final Integer placeHolderDrawable,
                                            @Nullable final Integer errorPlaceHolderDrawable,
                                            @Nullable final Callback callback) {
        ImageUtils.downloadImageFromUrl(context, url, imageView, placeHolderDrawable, errorPlaceHolderDrawable, callback);
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

    public static void requestAppPermissions(@NonNull final Activity activity) {
        //Permission requests

        // WRITE_EXTERNAL_STORAGE (includes READ_EXTERNAL_STORAGE)
        final int rwExternalStoragePermissionCheck = ContextCompat
                .checkSelfPermission(
                        activity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (rwExternalStoragePermissionCheck != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat
                    .shouldShowRequestPermissionRationale(
                            activity,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                SnackbarUtils.buildSnackbar(activity,
                        "Storage access is needed to reduce data usage and enable sharing.",
                        "OK",
                        Snackbar.LENGTH_INDEFINITE,
                        new SnackbarCallback() {
                            @Override
                            public void onShowEvent(@Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventSwipe(int event, @Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventActionClick(int event, @Nullable Bundle bundle) throws Exception {
                                //Request permission
                                ActivityCompat.requestPermissions(activity,
                                        new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        DDWRTCompanionConstants.Permissions.STORAGE);
                            }

                            @Override
                            public void onDismissEventTimeout(int event, @Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventManual(int event, @Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventConsecutive(int event, @Nullable Bundle bundle) throws Exception {

                            }
                        },
                        null,
                        true);
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(activity,
                        new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        DDWRTCompanionConstants.Permissions.STORAGE);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

}