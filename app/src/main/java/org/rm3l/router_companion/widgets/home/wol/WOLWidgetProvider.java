package org.rm3l.router_companion.widgets.home.wol;

import static org.rm3l.router_companion.mgmt.RouterManagementActivity.ROUTER_SELECTED;
import static org.rm3l.router_companion.widgets.home.wol.RouterWolWidgetConfirmationDialogFromWidgetActivity.HOSTS_TO_WAKE;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.main.DDWRTMainActivity;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.router_companion.resources.Device;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.services.wol.WakeOnLanTile;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.widgets.ConfirmDialogAsActivity;
import org.rm3l.router_companion.widgets.UpgradeDialogAsActivity;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link WOLWidgetConfigureActivity
 * WOLWidgetConfigureActivity}
 */
public class WOLWidgetProvider extends AppWidgetProvider {

    public static final String EXTRA_ITEM = "org.rm3l.ddwrt.widgets.home.wol.EXTRA_ITEM";

    public static final String ACTION_WAKE_HOST = "org.rm3l.ddwrt.widgets.home.wol.ACTION_WAKE_HOST";

    public static final String ACTION_REFRESH = "org.rm3l.ddwrt.widgets.home.wol.ACTION_REFRESH";

    private static final String LOG_TAG = WOLWidgetProvider.class.getSimpleName();

    /**
     * Build {@link ComponentName} describing this specific
     * {@link AppWidgetProvider}
     */
    public static ComponentName getComponentName(Context context) {
        return new ComponentName(context, WOLWidgetProvider.class);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            WOLWidgetConfigureActivity.deleteRouterUuidPref(context, appWidgetIds[i]);
        }
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {

        Crashlytics.log(Log.DEBUG, LOG_TAG, "onReceive: " + intent);

        final int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        final String intentAction;

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                && (intentAction = intent.getAction()) != null) {
            final AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
            final String routerUuid = intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);
            if (routerUuid != null) {
                final Router router = RouterManagementActivity.Companion.getDao(context).getRouter(routerUuid);
                if (router == null) {
                    Toast.makeText(context, "Invalid Router - entry may have been dropped!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    switch (intentAction) {
                        case ACTION_REFRESH:
                            Toast.makeText(context, "Refreshing list...", Toast.LENGTH_SHORT).show();
                            //Notify widgets that data has changed
                            widgetManager.notifyAppWidgetViewDataChanged(
                                    widgetManager.getAppWidgetIds(getComponentName(context)),
                                    R.id.wol_widget_hosts_list);
                            break;
                        case ACTION_WAKE_HOST:
                            final String deviceToWakeStr = intent.getStringExtra(HOSTS_TO_WAKE);
                            if (deviceToWakeStr != null) {
                                try {
                                    final ArrayList<Device> devices = WakeOnLanTile.GSON_BUILDER.create()
                                            .fromJson(deviceToWakeStr, new TypeToken<ArrayList<Device>>() {
                                            }.getType());
                                    if (!(devices == null || devices.isEmpty())) {
                                        final Device device = devices.get(0);
                                        if (device != null) {
                                            final Intent wakeIntent = new Intent(context,
                                                    RouterWolWidgetConfirmationDialogFromWidgetActivity.class);
                                            wakeIntent.putExtra(RouterManagementActivity.ROUTER_SELECTED, routerUuid);
                                            wakeIntent.putExtra(HOSTS_TO_WAKE, intent.getStringExtra(HOSTS_TO_WAKE));
                                            wakeIntent.putExtra(ConfirmDialogAsActivity.TITLE, "Wake on LAN");
                                            wakeIntent.putExtra(ConfirmDialogAsActivity.MESSAGE, String.format(
                                                    "Are you sure you wish to attempt waking this host: '%s' (%s)?",
                                                    device.getName(), device.getMacAddress()));
                                            wakeIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                                            wakeIntent.setAction(
                                                    routerUuid + "-wakeHost-" + System.currentTimeMillis());
                                            wakeIntent.setFlags(
                                                    Intent.FLAG_ACTIVITY_NEW_TASK
                                                            | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                                            context.startActivity(wakeIntent);
                                        }
                                    }
                                } catch (final Exception e) {
                                    e.printStackTrace();
                                    Utils.reportException(null, e);
                                }
                            }

                            break;
                        default:
                            break;
                    }
                }
            }
        }

        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

        final DDWRTCompanionDAO dao = RouterManagementActivity.Companion.getDao(context);

        final String routerUuid = WOLWidgetConfigureActivity.loadRouterUuidPref(context, appWidgetId);
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
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.wolwidget);
        views.setTextViewText(R.id.wol_widget_router_name, routerName);
        views.setTextViewText(R.id.wol_widget_router_ip_or_dns, routerIp);

        if (router == null) {
            views.setViewVisibility(R.id.wol_widget_launch_action, View.GONE);
            views.setViewVisibility(R.id.wol_widget_wake_all_action, View.GONE);
            views.setViewVisibility(R.id.wol_widget_hosts_list, View.GONE);
            views.setViewVisibility(R.id.wol_widget_hosts_list_empty_view, View.GONE);
            views.setViewVisibility(R.id.wol_widget_refresh, View.GONE);
        } else {

            // Set up the intent that starts the ListView service, which will
            // provide the views for this collection.
            final Intent intent = new Intent(context, WOLWidgetService.class);
            // Add the app widget ID to the intent extras.
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, routerUuid);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            // Set up the RemoteViews object to use a RemoteViews adapter.
            // This adapter connects
            // to a RemoteViewsService  through the specified intent.
            // This is how you populate the data.
            views.setRemoteAdapter(R.id.wol_widget_hosts_list, intent);
            // The empty view is displayed when the collection has no items.
            // It should be in the same layout used to instantiate the RemoteViews
            // object above.
            views.setEmptyView(R.id.wol_widget_hosts_list, R.id.wol_widget_hosts_list_empty_view);

            // Here we setup the a pending intent template. Individuals items of
            // a collection
            // cannot setup their own pending intents, instead, the collection
            // as a whole can
            // setup a pending intent template, and the individual items can set
            // a fillInIntent
            // to create unique before on an item to item basis.
            // Adding collection list item handler
            final Intent onItemClick = new Intent(context, WOLWidgetProvider.class);
            onItemClick.setAction(ACTION_WAKE_HOST);
            onItemClick.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            onItemClick.setData(Uri.parse(onItemClick.toUri(Intent.URI_INTENT_SCHEME)));
            final PendingIntent onClickPendingIntent =
                    PendingIntent.getBroadcast(context, 0, onItemClick, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setPendingIntentTemplate(R.id.wol_widget_hosts_list, onClickPendingIntent);

            //Logo Intent
            final Intent logoIntent = new Intent(context, RouterManagementActivity.class);
            logoIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            logoIntent.setAction(routerUuid + "-logo-" + System.currentTimeMillis());
            final PendingIntent logoPendingIntent =
                    PendingIntent.getActivity(context, 0, logoIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.wol_widget_Logo, logoPendingIntent);

            //Refresh Intent
            final Intent refreshIntent = new Intent(context, WOLWidgetProvider.class);
            refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            refreshIntent.setAction(ACTION_REFRESH);
            refreshIntent.setData(Uri.parse(refreshIntent.toUri(Intent.URI_INTENT_SCHEME)));
            final PendingIntent refreshPendingIntent =
                    PendingIntent.getBroadcast(context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.wol_widget_refresh, refreshPendingIntent);

            //Launch Intent
            final Intent launchIntent = new Intent(context, DDWRTMainActivity.class);
            launchIntent.putExtra(RouterManagementActivity.ROUTER_SELECTED, routerUuid);
            launchIntent.putExtra(DDWRTMainActivity.SAVE_ITEM_SELECTED, 9); //Open right on WOL Menu Item
            launchIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            launchIntent.setAction(routerUuid + "-launch-" + System.currentTimeMillis());
            final PendingIntent launchPendingIntent =
                    PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.wol_widget_launch_action, launchPendingIntent);

            //Wake all Intent
            final Intent wakeAllIntent;
            if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
                wakeAllIntent = new Intent(context, UpgradeDialogAsActivity.class);
            } else {
                wakeAllIntent =
                        new Intent(context, RouterWolWidgetConfirmationDialogFromWidgetActivity.class);
                wakeAllIntent.putExtra(
                        RouterWolWidgetConfirmationDialogFromWidgetActivity.LOAD_HOSTS_FROM_PREFS, true);
                wakeAllIntent.putExtra(ConfirmDialogAsActivity.TITLE, "Wake all hosts");
                wakeAllIntent.putExtra(ConfirmDialogAsActivity.MESSAGE,
                        "Are you sure you wish to attempt waking all hosts?");
            }
            wakeAllIntent.putExtra(RouterManagementActivity.ROUTER_SELECTED, routerUuid);
            wakeAllIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            wakeAllIntent.setAction(routerUuid + "-wakeAll-" + System.currentTimeMillis());
            final PendingIntent wakeAllPendingIntent =
                    PendingIntent.getActivity(context, 0, wakeAllIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.wol_widget_wake_all_action, wakeAllPendingIntent);
        }

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}


