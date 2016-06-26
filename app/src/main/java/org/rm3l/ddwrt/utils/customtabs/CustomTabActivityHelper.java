package org.rm3l.ddwrt.utils.customtabs;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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

import java.util.List;

/**
 * Created by rm3l on 22/06/16.
 */
public class CustomTabActivityHelper {

    private CustomTabsSession mCustomTabsSession;
    private CustomTabsClient mClient;
    private CustomTabsServiceConnection mConnection;
    private ConnectionCallback mConnectionCallback;

    public static void openCustomTab(@NonNull final Activity context,
                                     @Nullable final CustomTabsSession customTabsSession,
                                     @NonNull final String url,
                                     @Nullable final String routerUuid,
                                     @Nullable final String helpLink,
                                     @Nullable final CustomTabFallback fallback,
                                     boolean withFeedbackMenuItem) {

        final CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder(customTabsSession);

        builder.setShowTitle(true);
        // Changes the background color for the omnibox. colorInt is an int
        // that specifies a Color.
        builder.setToolbarColor(ContextCompat.getColor(context, ColorUtils.isThemeLight(context) ?
                R.color.lightTheme_primary : R.color.darkTheme_primary));

        builder.setStartAnimations(context,
                R.anim.slide_in_right,
                R.anim.slide_out_left);
        builder.setExitAnimations(context,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right);

        builder.setCloseButtonIcon(BitmapFactory.decodeResource(
                context.getResources(), R.drawable.ic_arrow_back_white_24dp));

        //Menu items
        builder.addDefaultShareMenuItem();

        if (withFeedbackMenuItem) {
            final Intent intent = new Intent(context, SendFeedbackBroadcastReceiver.class);
            intent.putExtra(CustomTabActivityHelper.class.getSimpleName(), true);
            intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, routerUuid);
            if (routerUuid != null) {
                intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, routerUuid);
            }
            builder.addMenuItem("Send Feedback", PendingIntent.getBroadcast(context, 0, intent, 0));
        }

        final CustomTabsIntent customTabsIntent = builder.build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                //Add app as the referrer
                customTabsIntent.intent.putExtra(Intent.EXTRA_REFERRER,
                        Uri.parse(Intent.URI_ANDROID_APP_SCHEME + "//" + context.getPackageName()));
            }
        }

        openCustomTab(context, customTabsIntent, Uri.parse(url), fallback);
    }

    /**
     * Opens the URL on a Custom Tab if possible. Otherwise fallsback to opening it on a WebView
     *
     * @param activity The host activity
     * @param customTabsIntent a CustomTabsIntent to be used if Custom Tabs is available
     * @param uri the Uri to be opened
     * @param fallback a CustomTabFallback to be used if Custom Tabs is not available
     */
    public static void openCustomTab(Activity activity,
                                     CustomTabsIntent customTabsIntent,
                                     Uri uri,
                                     CustomTabFallback fallback) {
        String packageName = CustomTabsHelper.getPackageNameToUse(activity);

        //If we cant find a package name, it means there's no browser that supports
        //Chrome Custom Tabs installed. So, we fallback to the webview
        if (packageName == null) {
            if (fallback != null) {
                fallback.openUri(activity, uri);
            }
        } else {
            customTabsIntent.intent.setPackage(packageName);
            customTabsIntent.launchUrl(activity, uri);
        }
    }

    /**
     * Unbinds the Activity from the Custom Tabs Service
     * @param activity the activity that is connected to the service
     */
    public void unbindCustomTabsService(Activity activity) {
        if (mConnection == null) return;
        activity.unbindService(mConnection);
        mClient = null;
        mCustomTabsSession = null;
    }

    /**
     * Creates or retrieves an exiting CustomTabsSession
     *
     * @return a CustomTabsSession
     */
    public CustomTabsSession getSession() {
        if (mClient == null) {
            mCustomTabsSession = null;
        } else if (mCustomTabsSession == null) {
            mCustomTabsSession = mClient.newSession(null);
        }
        return mCustomTabsSession;
    }

    /**
     * Register a Callback to be called when connected or disconnected from the Custom Tabs Service
     * @param connectionCallback
     */
    public void setConnectionCallback(ConnectionCallback connectionCallback) {
        this.mConnectionCallback = connectionCallback;
    }

    /**
     * Binds the Activity to the Custom Tabs Service
     * @param activity the activity to be binded to the service
     */
    public void bindCustomTabsService(Activity activity) {
        if (mClient != null) return;

        String packageName = CustomTabsHelper.getPackageNameToUse(activity);
        if (packageName == null) return;
        mConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
                mClient = client;
                mClient.warmup(0L);
                if (mConnectionCallback != null) mConnectionCallback.onCustomTabsConnected();
                //Initialize a session as soon as possible.
                getSession();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mClient = null;
                if (mConnectionCallback != null) mConnectionCallback.onCustomTabsDisconnected();
            }
        };
        CustomTabsClient.bindCustomTabsService(activity, packageName, mConnection);
    }

    public boolean mayLaunchUrl(Uri uri, Bundle extras, List<Bundle> otherLikelyBundles) {
        if (mClient == null) return false;

        CustomTabsSession session = getSession();
        return session != null && session.mayLaunchUrl(uri, extras, otherLikelyBundles);
    }

    /**
     * A Callback for when the service is connected or disconnected. Use those callbacks to
     * handle UI changes when the service is connected or disconnected
     */
    public interface ConnectionCallback {
        /**
         * Called when the service is connected
         */
        void onCustomTabsConnected();

        /**
         * Called when the service is disconnected
         */
        void onCustomTabsDisconnected();
    }

    /**
     * To be used as a fallback to open the Uri when Custom Tabs is not available
     */
    public interface CustomTabFallback {
        /**
         *
         * @param activity The Activity that wants to open the Uri
         * @param uri The uri to be opened by the fallback
         */
        void openUri(Activity activity, Uri uri);
    }

}
