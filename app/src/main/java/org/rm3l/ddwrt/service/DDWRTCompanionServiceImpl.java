package org.rm3l.ddwrt.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.rm3l.ddwrt.common.IDDWRTCompanionService;
import org.rm3l.ddwrt.common.resources.RouterInfo;
import org.rm3l.ddwrt.common.resources.audit.ActionLog;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.resources.conn.Router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static android.text.TextUtils.isEmpty;

/**
 * Created by rm3l on 07/08/16.
 */
public class DDWRTCompanionServiceImpl extends Service {

    private static final String TAG = DDWRTCompanionServiceImpl.class.getSimpleName();

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

    private final IDDWRTCompanionService.Stub mBinder = new IDDWRTCompanionService.Stub() {

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
            final Router router = mDao.getRouter(routerUuid);
            return router != null ? router.toRouterInfo() : null;
        }

        @Override
        public RouterInfo getRouterById(int routerId) throws RemoteException {
            final Router router = mDao.getRouter(routerId);
            return router != null ? router.toRouterInfo() : null;
        }

        @Override
        public List<RouterInfo> lookupRoutersByName(String name) throws RemoteException {
            final Collection<Router> routersByName = mDao.getRoutersByName(name);
            final List<RouterInfo> routerInfoList =
                    new ArrayList<>(routersByName.size());
            for (final Router router : routersByName) {
                if (router == null) {
                    continue;
                }
                routerInfoList.add(router.toRouterInfo());
            }

            return routerInfoList;
        }

//        @Override
//        public void recordAction(ActionLog actionLog) {
//            if (isEmpty(actionLog.getOriginPackageName())) {
//                throw new IllegalArgumentException("Origin must not be blank");
//            }
//            mDao.recordAction(actionLog);
//        }

        @Override
        public List<ActionLog> getActionsByOrigin(String origin) {
            if (isEmpty(origin)) {
                throw new IllegalArgumentException("Origin must not be blank");
            }
            final Collection<ActionLog> actionLogCollection = mDao.getActionsByOrigin(origin);
            return new ArrayList<>(actionLogCollection);
        }

        @Override
        public List<ActionLog> getActionsByOriginWithSqlConstraints(String origin, String predicate,
                                                                    String groupBy,
                                                                    String having, String orderBy) {
            if (isEmpty(origin)) {
                throw new IllegalArgumentException("Origin must not be blank");
            }
            final Collection<ActionLog> actionLogCollection = mDao
                    .getActionsByOrigin(origin, predicate, groupBy, having, orderBy);
            return new ArrayList<>(actionLogCollection);
        }

        @Override
        public List<ActionLog> getActionsByRouterByOrigin(String routerUuid, String origin) {
            if (isEmpty(origin) || isEmpty(routerUuid)) {
                throw new IllegalArgumentException("Origin and Router UUID must not be blank");
            }
            final Collection<ActionLog> actionLogCollection = mDao
                    .getActionsByRouterByOrigin(routerUuid, origin);
            return new ArrayList<>(actionLogCollection);
        }

        @Override
        public List<ActionLog> getActionsByRouterByOriginWithSqlConstraints(String routerUuid,
                                                                            String origin,
                                                                            String predicate,
                                                                            String groupBy,
                                                                            String having,
                                                                            String orderBy) {
            if (isEmpty(origin) || isEmpty(routerUuid)) {
                throw new IllegalArgumentException("Origin and Router UUID must not be blank");
            }
            final Collection<ActionLog> actionLogCollection = mDao
                    .getActionsByRouterByOrigin(routerUuid, origin,
                            predicate, groupBy, having, orderBy);
            return new ArrayList<>(actionLogCollection);
        }

        @Override
        public void clearActionsLogByOrigin(String origin) {
            if (isEmpty(origin)) {
                throw new IllegalArgumentException("Origin must not be blank");
            }
            mDao.clearActionsLogByOrigin(origin);
        }

        @Override
        public void clearActionsLogByRouterByOrigin(String routerUuid, String origin) {
            if (isEmpty(origin) || isEmpty(routerUuid)) {
                throw new IllegalArgumentException("Origin and Router UUID must not be blank");
            }
            mDao.clearActionsLogByRouterByOrigin(routerUuid, origin);
        }
    };

}

