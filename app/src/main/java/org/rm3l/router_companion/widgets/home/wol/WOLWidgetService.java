package org.rm3l.router_companion.widgets.home.wol;

import static org.rm3l.router_companion.RouterCompanionAppConstants.EMPTY_VALUE_TO_DISPLAY;
import static org.rm3l.router_companion.widgets.home.wol.RouterWolWidgetConfirmationDialogFromWidgetActivity.HOSTS_TO_WAKE;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.crashlytics.android.Crashlytics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.resources.Device;
import org.rm3l.router_companion.resources.MACOUIVendor;
import org.rm3l.router_companion.resources.RouterData;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.services.wol.WakeOnLanTile;
import org.rm3l.router_companion.utils.Utils;

/**
 * Created by rm3l on 22/04/15.
 */
public class WOLWidgetService extends RemoteViewsService {

    public static final String WOL_HOST_PREF_KEY = \"fake-key\";
            WOLWidgetService.class.getCanonicalName() + "::wol_hosts";

    public static final String WOL_BCAST_ADDRESSES_PREF_KEY = \"fake-key\";
            WOLWidgetService.class.getCanonicalName() + "::bcast_addrs";

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new WOLRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

class WOLRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory,
        Loader.OnLoadCompleteListener<RouterData<ArrayList<Device>>> {

    public static final String TAG = WOLRemoteViewsFactory.class.getSimpleName();

    private int mAppWidgetId;

    private List<String> mBcastAddresses = new ArrayList<>();

    private final Context mContext;

    private final SharedPreferences mGlobalPreferences;

    private final Intent mIntent;

    private Router mRouter;

    private final SharedPreferences mRouterPreferences;

    private final String mRouterUuid;

    private ArrayList<Device> mWidgetItems = new ArrayList<>();

    public WOLRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        mIntent = intent;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        mRouterUuid = intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);
        mRouterPreferences = context.getSharedPreferences(mRouterUuid, Context.MODE_PRIVATE);
        mGlobalPreferences =
                context.getSharedPreferences(RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY,
                        Context.MODE_PRIVATE);
    }

    @Override
    public void onCreate() {

        mRouter = RouterManagementActivity.getDao(mContext).getRouter(mRouterUuid);
        if (mRouter == null) {
            Crashlytics.log(Log.DEBUG, TAG, "onCreate: mRouter IS null");
        }
    }

    @Override
    public void onDestroy() {
        Crashlytics.log(Log.DEBUG, TAG, "onDestroy");
    }

    @Override
    public int getCount() {
        Crashlytics.log(Log.DEBUG, TAG, "getCount");

        return mWidgetItems.size();
    }

    @Override
    public long getItemId(int position) {
        Crashlytics.log(Log.DEBUG, TAG, "getItemId(" + position + ")");

        return position;
    }

    @Override
    public RemoteViews getLoadingView() {
        Crashlytics.log(Log.DEBUG, TAG, "getLoadingView");
        // We aren't going to return a default loading view in this sample
        return null;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        Crashlytics.log(Log.DEBUG, TAG, "getViewAt(" + position + ")");

        // we use getCount here so that it doesn't return null when empty
        final int count = getCount();
        if (position < 0 || position >= count) {
            Utils.reportException(null,
                    new IllegalStateException(position + "=position < 0 || position >= getCount()=" + count));
            return null;
        }

        // position will always range from 0 to getCount() - 1.

        // Construct a RemoteViews item based on the app widget item XML file, and set the
        // text based on the position.
        final Device device = mWidgetItems.get(position);

        final RemoteViews rv =
                new RemoteViews(mContext.getPackageName(), R.layout.widget_wol_host_item);

        rv.setViewVisibility(R.id.widget_wol_host_item_you_and_menu, View.GONE);

        if (device == null) {
            rv.setTextViewText(R.id.widget_wol_host_item_device_name, EMPTY_VALUE_TO_DISPLAY);
            rv.setTextViewText(R.id.widget_wol_host_item_device_mac, EMPTY_VALUE_TO_DISPLAY);
            rv.setTextViewText(R.id.widget_wol_host_item_device_ip, EMPTY_VALUE_TO_DISPLAY);
            rv.setTextViewText(R.id.widget_wol_host_item_device_details_nic_manufacturer,
                    EMPTY_VALUE_TO_DISPLAY);
        } else {

            rv.setTextViewText(R.id.widget_wol_host_item_device_name, device.getName());
            rv.setTextViewText(R.id.widget_wol_host_item_device_mac, device.getMacAddress());
            rv.setTextViewText(R.id.widget_wol_host_item_device_ip, device.getIpAddress());
            final MACOUIVendor macouiVendor = device.getMacouiVendorDetails();
            rv.setTextViewText(R.id.widget_wol_host_item_device_details_nic_manufacturer,
                    macouiVendor != null ? macouiVendor.getCompany() : EMPTY_VALUE_TO_DISPLAY);
        }

        // Next, set a fill-intent, which will be used to fill in the pending intent template
        // that is set on the collection view in StackWidgetProvider.
        final Bundle extras = new Bundle();
        extras.putInt(WOLWidgetProvider.EXTRA_ITEM, position);
        extras.putString(RouterManagementActivity.ROUTER_SELECTED, mRouterUuid);
        try {
            extras.putString(HOSTS_TO_WAKE,
                    WakeOnLanTile.GSON_BUILDER.create().toJson(Collections.singletonList(device)));
        } catch (final Exception e) {
            e.printStackTrace();
            Utils.reportException(null, e);
        }
        final Intent fillInIntent = new Intent();
        fillInIntent.putExtras(extras);
        fillInIntent.setAction(WOLWidgetProvider.ACTION_WAKE_HOST);
        // Make it possible to distinguish the individual on-click
        // action of a given item
        rv.setOnClickFillInIntent(R.id.widget_wol_host_item_card_view, fillInIntent);

        // Return the RemoteViews object.
        return rv;
    }

    @Override
    public int getViewTypeCount() {
        Crashlytics.log(Log.DEBUG, TAG, "getLoadingView");
        // Technically, we have only one type of view
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        Crashlytics.log(Log.DEBUG, TAG, "hasStableIds");

        return true;
    }

    @Override
    public void onDataSetChanged() {
        initDataLoader();
    }

    @Override
    public void onLoadComplete(@Nullable Loader<RouterData<ArrayList<Device>>> loader,
            @Nullable RouterData<ArrayList<Device>> data) {
        Crashlytics.log(Log.DEBUG, TAG, "onLoadComplete: data=" + data);

        mWidgetItems.clear();
        if (data != null) {
            final ArrayList<Device> devices = data.getData();
            if (devices != null) {

                final Set<Device> sortedDevices = new TreeSet<>(new Comparator<Device>() {
                    @Override
                    public int compare(Device d1, Device d2) {
                        if (d1 == d2) {
                            return 0;
                        }

                        if (d1 == null) {
                            return -1;
                        }

                        if (d2 == null) {
                            return 1;
                        }

                        return d1.getName().compareToIgnoreCase(d2.getName());
                    }
                });
                sortedDevices.addAll(devices);

                for (final Device sortedDevice : sortedDevices) {
                    mWidgetItems.add(sortedDevice);
                }
            }
        }

        try {
            mRouterPreferences.edit()
                    .putStringSet(WOLWidgetService.WOL_BCAST_ADDRESSES_PREF_KEY,
                            new HashSet<>(mBcastAddresses))
                    .putString(WOLWidgetService.WOL_HOST_PREF_KEY,
                            WakeOnLanTile.GSON_BUILDER.create().toJson(mWidgetItems))
                    .apply();
            Utils.requestBackup(mContext);
        } catch (final Exception e) {
            e.printStackTrace();
            Utils.reportException(null, e);
        }
    }

    private void initDataLoader() {
        //Synchronous call to retrieve all data
        final RouterData<ArrayList<Device>> routerData =
                WakeOnLanTile.getArrayListRouterDataSync(null, mRouter, mContext, mBcastAddresses,
                        mGlobalPreferences, mRouterPreferences);

        this.onLoadComplete(null, routerData);
    }
}
