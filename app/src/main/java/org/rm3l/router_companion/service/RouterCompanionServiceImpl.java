package org.rm3l.router_companion.service;

import static android.text.TextUtils.isEmpty;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import androidx.annotation.Nullable;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.rm3l.router_companion.common.IRouterCompanionService;
import org.rm3l.router_companion.common.resources.RouterInfo;
import org.rm3l.router_companion.common.resources.audit.ActionLog;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.router_companion.resources.conn.Router;

/** Created by rm3l on 07/08/16. */
public class RouterCompanionServiceImpl extends Service {

  private static final String TAG = RouterCompanionServiceImpl.class.getSimpleName();

  private DDWRTCompanionDAO mDao;

  private final IRouterCompanionService.Stub mBinder =
      new IRouterCompanionService.Stub() {

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

        @Override
        public List<ActionLog> getActionsByOrigin(String origin) {
          if (isEmpty(origin)) {
            throw new IllegalArgumentException("Origin must not be blank");
          }
          final Collection<ActionLog> actionLogCollection = mDao.getActionsByOrigin(origin);
          return new ArrayList<>(actionLogCollection);
        }

        @Override
        public List<ActionLog> getActionsByOriginWithSqlConstraints(
            String origin, String predicate, String groupBy, String having, String orderBy) {
          if (isEmpty(origin)) {
            throw new IllegalArgumentException("Origin must not be blank");
          }
          final Collection<ActionLog> actionLogCollection =
              mDao.getActionsByOrigin(origin, predicate, groupBy, having, orderBy);
          return new ArrayList<>(actionLogCollection);
        }

        //        @Override
        //        public void recordAction(ActionLog actionLog) {
        //            if (isEmpty(actionLog.getOriginPackageName())) {
        //                throw new IllegalArgumentException("Origin must not be blank");
        //            }
        //            mDao.recordAction(actionLog);
        //        }

        @Override
        public List<ActionLog> getActionsByRouterByOrigin(String routerUuid, String origin) {
          if (isEmpty(origin) || isEmpty(routerUuid)) {
            throw new IllegalArgumentException("Origin and Router UUID must not be blank");
          }
          final Collection<ActionLog> actionLogCollection =
              mDao.getActionsByRouterByOrigin(routerUuid, origin);
          return new ArrayList<>(actionLogCollection);
        }

        @Override
        public List<ActionLog> getActionsByRouterByOriginWithSqlConstraints(
            String routerUuid,
            String origin,
            String predicate,
            String groupBy,
            String having,
            String orderBy) {
          if (isEmpty(origin) || isEmpty(routerUuid)) {
            throw new IllegalArgumentException("Origin and Router UUID must not be blank");
          }
          final Collection<ActionLog> actionLogCollection =
              mDao.getActionsByRouterByOrigin(
                  routerUuid, origin, predicate, groupBy, having, orderBy);
          return new ArrayList<>(actionLogCollection);
        }

        @Override
        public List<RouterInfo> getAllRouters() throws RemoteException {
          final List<Router> allRouters = mDao.getAllRouters();
          final List<RouterInfo> routerInfoList = new ArrayList<>(allRouters.size());
          for (final Router router : allRouters) {
            if (router == null) {
              continue;
            }
            routerInfoList.add(router.toRouterInfo());
          }

          return routerInfoList;
        }

        @Override
        public RouterInfo getRouterById(int routerId) throws RemoteException {
          final Router router = mDao.getRouter(routerId);
          return router != null ? router.toRouterInfo() : null;
        }

        @Override
        public RouterInfo getRouterByUuid(String routerUuid) throws RemoteException {
          final Router router = mDao.getRouter(routerUuid);
          return router != null ? router.toRouterInfo() : null;
        }

        @Override
        public List<RouterInfo> lookupRoutersByName(String name) throws RemoteException {
          final Collection<Router> routersByName = mDao.getRoutersByName(name);
          final List<RouterInfo> routerInfoList = new ArrayList<>(routersByName.size());
          for (final Router router : routersByName) {
            if (router == null) {
              continue;
            }
            routerInfoList.add(router.toRouterInfo());
          }

          return routerInfoList;
        }
      };

  @Override
  public void onCreate() {
    super.onCreate();
    this.mDao = RouterManagementActivity.Companion.getDao(this);
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    FirebaseCrashlytics.getInstance().log("onBind");
    return mBinder;
  }
}
