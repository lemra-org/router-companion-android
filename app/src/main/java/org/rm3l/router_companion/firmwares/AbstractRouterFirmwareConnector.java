package org.rm3l.router_companion.firmwares;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.crashlytics.android.Crashlytics;

import org.rm3l.router_companion.resources.MonthlyCycleItem;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.utils.Utils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Created by rm3l on 08/01/2017.
 */

public abstract class AbstractRouterFirmwareConnector {

    @Nullable
    public final String getRouterModel(@NonNull Context context, @NonNull Router router)
            throws Exception {
        final String routerModel = this.goGetRouterModel(context, router);
        final SharedPreferences routerPreferences = router.getPreferences(context);
        if (routerPreferences != null) {
            routerPreferences
                    .edit()
                    .putString(NVRAMInfo.MODEL, routerModel)
                    .apply();
            Utils.requestBackup(context);
        }
        return routerModel;
    }

    @SuppressWarnings("TryWithIdenticalCatches")
    public <T extends DDWRTTile> NVRAMInfo getDataFor(@NonNull Context context,
                                                      @NonNull Router router,
                                                      @NonNull Class<T> tile,
                                                      @Nullable RemoteDataRetrievalListener dataRetrievalListener) {
        try {
            return (NVRAMInfo) this.getClass()
                    .getDeclaredMethod("getDataFor" + tile.getSimpleName(),
                            Context.class, Router.class, RemoteDataRetrievalListener.class)
                    .invoke(this, context, router, dataRetrievalListener);
        } catch (IllegalAccessException e) {
            Crashlytics.logException(e);
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            Crashlytics.logException(e);
            throw new IllegalStateException(e);
        } catch (NoSuchMethodException e) {
            Crashlytics.logException(e);
            throw new IllegalStateException(e);
        }
    }

    protected abstract NVRAMInfo getDataForNetworkTopologyMapTile(@NonNull Context context,
                                                                  @NonNull Router router,
                                                                  @Nullable RemoteDataRetrievalListener dataRetrievalListener)
            throws Exception;

    @Nullable
    public abstract String getWanPublicIpAddress(@NonNull Context context,
                                                 @NonNull Router router,
                                                 @Nullable RemoteDataRetrievalListener dataRetrievalListener)
        throws Exception;


    @Nullable
    protected abstract String goGetRouterModel(@NonNull Context context, @NonNull Router router)
            throws Exception;

    public abstract NVRAMInfo getDataForWANTotalTrafficOverviewTile(@NonNull Context context,
                                                                  @NonNull Router router,
                                                                   MonthlyCycleItem cycleItem,
                                                                  @Nullable RemoteDataRetrievalListener dataRetrievalListener)
            throws Exception;


    protected abstract NVRAMInfo getDataForUptimeTile(@NonNull Context context,
                                                      @NonNull Router router,
                                                      @Nullable RemoteDataRetrievalListener dataRetrievalListener)
            throws Exception;

    public abstract List<String[]> getDataForMemoryAndCpuUsageTile(@NonNull Context context,
                                                                   @NonNull Router router,
                                                                   @Nullable RemoteDataRetrievalListener dataRetrievalListener)
            throws Exception;
}
