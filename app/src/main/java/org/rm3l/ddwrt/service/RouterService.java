package org.rm3l.ddwrt.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.rm3l.ddwrt.common.IRouterService;
import org.rm3l.ddwrt.common.resources.RouterInfo;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.resources.conn.Router;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rm3l on 07/08/16.
 */
public class RouterService extends Service {

    private static final String TAG = RouterService.class.getSimpleName();

    private DDWRTCompanionDAO mDao;

    @Override
    public void onCreate() {
        super.onCreate();
        this.mDao = RouterManagementActivity.getDao(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Crashlytics.log(Log.DEBUG, TAG, "onBind");
        return mBinder;
    }

    private final IRouterService.Stub mBinder = new IRouterService.Stub() {

        @Override
        public List<RouterInfo> getAllRouters() throws RemoteException {
            final List<Router> allRouters = mDao.getAllRouters();
            final List<RouterInfo> routerInfoList =
                    new ArrayList<>(allRouters.size());
            for (final Router router : allRouters) {
                if (router == null) {
                    continue;
                }
                routerInfoList.add(router.toRouterInfo());
            }

            return routerInfoList;
        }

        @Override
        public RouterInfo getRouterByUuid(String routerUuid) throws RemoteException {
            return null;
        }

        @Override
        public RouterInfo getRouterById(int routerId) throws RemoteException {
            return null;
        }

        @Override
        public List<RouterInfo> lookupRoutersByName(String name) throws RemoteException {
            return null;
        }
    };

}

