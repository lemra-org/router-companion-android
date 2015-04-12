package org.rm3l.ddwrt.widgets.home.actions;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.actions.RebootRouterAction;
import org.rm3l.ddwrt.actions.RouterAction;
import org.rm3l.ddwrt.actions.RouterActionListener;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.widgets.home.wol.WOLWidgetConfigureActivity;


/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link WOLWidgetConfigureActivity WOLWidgetConfigureActivity}
 */
public class RouterActionsWidget extends AppWidgetProvider {

    public static final String ACTION_REBOOT_ROUTER = "org.rm3l.ddwrt.widgets.home.actions.ACTION_REBOOT_ROUTER";

    private static final String LOG_TAG = RouterActionsWidget.class.getSimpleName();
    public static final Handler MAIN_THREAD_HANDLER = new Handler(Looper.getMainLooper());

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
            views.setViewVisibility(R.id.actions_widget_reboot_action, View.GONE);
        } else {
            //Reboot Intent
            final Intent rebootIntent = new Intent(context, RouterActionsWidget.class);
            rebootIntent.setAction(ACTION_REBOOT_ROUTER);
            rebootIntent.putExtra(RouterManagementActivity.ROUTER_SELECTED, routerUuid);
            rebootIntent.putExtra(WIDGET_ID, appWidgetId);
            final PendingIntent rebootPendingIntent = PendingIntent.getBroadcast(context, 0, rebootIntent, 0);
            views.setOnClickPendingIntent(R.id.actions_widget_reboot_action, rebootPendingIntent);

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

        Log.d(LOG_TAG, "onReceive: " + intent);

        if (intent == null || (intentAction = intent.getAction()) == null) {
            super.onReceive(context, intent);
            return;
        }

        switch (intentAction) {

            case ACTION_REBOOT_ROUTER:
                final String routerUuid = intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);
                final Router router;
                if (routerUuid == null || (router = RouterManagementActivity.getDao(context).getRouter(routerUuid)) == null) {

                    Toast.makeText(context,
                            "Unknown Router. May have been removed. Please reconfigure the widget!",
                            Toast.LENGTH_SHORT).show();
                    super.onReceive(context, intent);
                    return;
                }

                Log.d(LOG_TAG, "onReceive: " + intentAction);

                Toast.makeText(context,
                        String.format("Rebooting router '%s' (%s) ...", router.getName(), router.getRemoteIpAddress()),
                        Toast.LENGTH_SHORT).show();

                new RebootRouterAction(context,
                        new RouterActionListener() {
                            @Override
                            public void onRouterActionSuccess(@NonNull final RouterAction routerAction, @NonNull final Router router, Object returnData) {
                                MAIN_THREAD_HANDLER.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context,
                                                String.format("Action '%s' executed successfully on '%s'.",
                                                        routerAction.toString(), router.getRemoteIpAddress()),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onRouterActionFailure(@NonNull final RouterAction routerAction, @NonNull final Router router,
                                                              @Nullable final Exception exception) {
                                MAIN_THREAD_HANDLER.post(new Runnable() {
                                     @Override
                                     public void run() {
                                        Toast.makeText(context,
                                                String.format("Error on action '%s': %s", routerAction.toString(), ExceptionUtils.getRootCauseMessage(exception)),
                                                Toast.LENGTH_SHORT).show();
                                     }
                                });
                            }
                        },
                        context.getSharedPreferences(DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE))
                    .execute(router);

                break;
            default:
                break;
        }

        super.onReceive(context, intent);
    }
}


