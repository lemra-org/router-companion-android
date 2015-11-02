package org.rm3l.ddwrt.widgets.home.actions;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.main.DDWRTMainActivity;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.widgets.ConfirmDialogAsActivity;
import org.rm3l.ddwrt.widgets.home.wol.WOLWidgetConfigureActivity;

import static org.rm3l.ddwrt.mgmt.RouterManagementActivity.ROUTER_SELECTED;


/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link WOLWidgetConfigureActivity WOLWidgetConfigureActivity}
 */
public class RouterActionsWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_OPEN_ROUTER_ACTIVITY = "org.rm3l.ddwrt.widgets.home.actions.ACTION_OPEN_ROUTER_ACTIVITY";
    public static final String ACTION_REBOOT_ROUTER = "org.rm3l.ddwrt.widgets.home.actions.ACTION_REBOOT_ROUTER";

    private static final String LOG_TAG = RouterActionsWidgetProvider.class.getSimpleName();

    private static final String WIDGET_ID = "_WIDGET_ID";

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        final DDWRTCompanionDAO dao = RouterManagementActivity.getDao(context);

        final String routerUuid = RouterActionsWidgetConfigureActivity.loadRouterUuidPref(context, appWidgetId);
        final String routerName;
        final String routerIp;

        Router router = null;

        if (routerUuid == null || routerUuid.isEmpty()) {
            routerName = "-";
            routerIp = "-";
        } else {
            router = dao.getRouter(routerUuid);
            if (router == null) {
                routerName = "-";
                routerIp = "-";
            } else {
                routerName = router.getName();
                routerIp = router.getRemoteIpAddress();
            }
        }
        // Construct the RemoteViews object
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.actionswidget);
        views.setTextViewText(R.id.actions_widget_router_name, routerName);
        views.setTextViewText(R.id.actions_widget_router_ip_or_dns, routerIp);

        if (router == null) {
            views.setViewVisibility(R.id.actions_widget_launch_action, View.GONE);
            views.setViewVisibility(R.id.actions_widget_reboot_action, View.GONE);
        } else {

            //Logo Intent
            final Intent logoIntent = new Intent(context, RouterManagementActivity.class);
            logoIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            logoIntent.setAction(routerUuid + "-logo-" +
                    System.currentTimeMillis());
            final PendingIntent logoPendingIntent = PendingIntent
                    .getActivity(context, 0, logoIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.actions_widget_Logo, logoPendingIntent);

            //Launch Intent
            final Intent launchIntent = new Intent(context, DDWRTMainActivity.class);
            launchIntent.putExtra(ROUTER_SELECTED, routerUuid);
            launchIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            launchIntent.setAction(routerUuid + "-launch-" +
                    System.currentTimeMillis());
            final PendingIntent launchPendingIntent = PendingIntent
                    .getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.actions_widget_launch_action, launchPendingIntent);

            //Reboot Intent
            final Intent rebootIntent = new Intent(context, RouterRebootWidgetConfirmationDialogFromWidgetActivity.class);
            rebootIntent.putExtra(ROUTER_SELECTED, routerUuid);
            rebootIntent.putExtra(ConfirmDialogAsActivity.TITLE, "Reboot Router");
            rebootIntent.putExtra(ConfirmDialogAsActivity.MESSAGE,
                    String.format("Are you sure you wish to reboot router '%s' (%s)?", routerName, routerIp));
            rebootIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            rebootIntent.setAction(routerUuid + "-reboot-" +System.currentTimeMillis());
            final PendingIntent rebootPendingIntent = PendingIntent
                    .getActivity(context, 0, rebootIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.actions_widget_reboot_action, rebootPendingIntent);

//            final Intent rebootIntent = new Intent(context, RouterActionsWidgetProvider.class);
//            rebootIntent.setAction(ACTION_REBOOT_ROUTER);
//            rebootIntent.putExtra(ROUTER_SELECTED, routerUuid);
//            rebootIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
//            //cf http://stackoverflow.com/questions/3168484/pendingintent-works-correctly-for-the-first-notification-but-incorrectly-for-the
//            final PendingIntent rebootPendingIntent = PendingIntent
//                    .getBroadcast(context, Utils.getRandomIntId(Integer.MAX_VALUE),
//                            rebootIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//            views.setOnClickPendingIntent(R.id.actions_widget_reboot_action, rebootPendingIntent);
        }

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (final int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        for (final int appWidgetId : appWidgetIds) {
            RouterActionsWidgetConfigureActivity.deleteRouterUuidPref(context, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String intentAction;

        final int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        Crashlytics.log(Log.DEBUG, LOG_TAG, "onReceive: " + intent);

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            super.onReceive(context, intent);
            return;
        }

        if ((intentAction = intent.getAction()) == null) {
            Toast.makeText(context,
                    "Unknown Router Action!",
                    Toast.LENGTH_SHORT).show();
            super.onReceive(context, intent);
            return;
        }

        switch (intentAction) {
            //Handle intent actions

            default:
                break;
        }

        super.onReceive(context, intent);
    }
}


