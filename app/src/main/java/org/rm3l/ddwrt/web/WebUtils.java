package org.rm3l.ddwrt.web;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.v4.content.ContextCompat;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.feedback.SendFeedbackBroadcastReceiver;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.utils.ColorUtils;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by rm3l on 21/06/16.
 */
public final class WebUtils {

    // always verify the host - dont check for certificate
    public final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    /**
     * Trust every server - dont check for any certificate
     */
    public static void trustAllHosts() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[] {};
            }

            public void checkClientTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }
        } };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
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

        CustomTabsServiceConnection serviceConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(final ComponentName componentName,
                                                     final CustomTabsClient customTabsClient) { }

            @Override
            public void onServiceDisconnected(final ComponentName name) { }
        };

        final boolean customTabsSupported =
                context.bindService(serviceIntent, serviceConnection,
                        Context.BIND_AUTO_CREATE | Context.BIND_WAIVE_PRIORITY);
        context.unbindService(serviceConnection);

        return customTabsSupported;
    }

    public static void openChromeCustomTab(@NonNull final Activity context,
                                           @Nullable final CustomTabsSession customTabsSession,
                                           @NonNull final String url,
                                           @Nullable final String routerUuid) {

        final CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder(customTabsSession);

        builder.setShowTitle(true);
        builder.enableUrlBarHiding();
        // Changes the background color for the omnibox. colorInt is an int
        // that specifies a Color.
        builder.setToolbarColor(ContextCompat.getColor(context, ColorUtils.isThemeLight(context) ?
                R.color.lightTheme_primary : R.color.darkTheme_primary));

        builder.setStartAnimations(context, R.anim.right_in, R.anim.left_out);
        builder.setExitAnimations(context, R.anim.left_out, R.anim.right_in);

        builder.setCloseButtonIcon(BitmapFactory.decodeResource(
                context.getResources(), R.drawable.ic_arrow_back_white_24dp));

        //Menu items
        builder.addDefaultShareMenuItem();

        //FIXME Send Feedback
        final Intent intent = new Intent(context, SendFeedbackBroadcastReceiver.class);
        if (routerUuid != null) {
            intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, routerUuid);
        }
        builder.addMenuItem("Send Feedback", PendingIntent.getBroadcast(context, 0, intent, 0));

        final CustomTabsIntent customTabsIntent = builder.build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                //Add app as the referrer
                customTabsIntent.intent.putExtra(Intent.EXTRA_REFERRER,
                        Uri.parse(Intent.URI_ANDROID_APP_SCHEME + "//" + context.getPackageName()));
            }
        }
        customTabsIntent.launchUrl(context, Uri.parse(url));
    }
}
