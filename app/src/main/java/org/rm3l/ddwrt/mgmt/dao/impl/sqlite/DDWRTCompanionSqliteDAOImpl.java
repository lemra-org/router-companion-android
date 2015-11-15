/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014  Armel Soro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact Info: Armel Soro <apps+ddwrt@rm3l.org>
 */

package org.rm3l.ddwrt.mgmt.dao.impl.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.resources.WANTrafficData;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.ReportingUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.COLUMN_ID;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.ROUTER_FIRMWARE;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.ROUTER_IP;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.ROUTER_NAME;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.ROUTER_PASSWORD;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.ROUTER_PORT;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.ROUTER_PRIVKEY;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.ROUTER_PROTOCOL;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.ROUTER_SSH_STRICT_HOST_KEY_CHECKING;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.ROUTER_USERNAME;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.ROUTER_UUID;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.TABLE_ROUTERS;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.TABLE_WAN_TRAFFIC;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.TABLE_WAN_TRAFFIC_COLUMN_ID;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.TABLE_WAN_TRAFFIC_ROUTER_UUID;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.TABLE_WAN_TRAFFIC_TRAFFIC_DATE;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.TABLE_WAN_TRAFFIC_TRAFFIC_IN;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.TABLE_WAN_TRAFFIC_TRAFFIC_OUT;

public class DDWRTCompanionSqliteDAOImpl implements DDWRTCompanionDAO {

    private static final String LOG_TAG = DDWRTCompanionSqliteDAOImpl.class.getSimpleName();
    @NonNull
    private final DDWRTCompanionSqliteOpenHelper dbHelper;
    private final Context mContext;
    // Database fields
//    private SQLiteDatabase database;
    @NonNull
    private String[] allColumns = {
            COLUMN_ID,
            ROUTER_UUID,
            ROUTER_NAME,
            ROUTER_IP,
            ROUTER_PROTOCOL,
            ROUTER_PORT,
            ROUTER_SSH_STRICT_HOST_KEY_CHECKING,
            ROUTER_USERNAME,
            ROUTER_PASSWORD,
            ROUTER_PRIVKEY,
            ROUTER_FIRMWARE};

    @NonNull
    private String[] wanTrafficAllColumns = {
            TABLE_WAN_TRAFFIC_COLUMN_ID,
            TABLE_WAN_TRAFFIC_ROUTER_UUID,
            TABLE_WAN_TRAFFIC_TRAFFIC_DATE,
            TABLE_WAN_TRAFFIC_TRAFFIC_IN,
            TABLE_WAN_TRAFFIC_TRAFFIC_OUT};

    public DDWRTCompanionSqliteDAOImpl(Context context) {
        this.mContext = context;
        dbHelper = new DDWRTCompanionSqliteOpenHelper(context);
    }

    public void destroy() {
        dbHelper.close();
    }

    @Nullable
    public Router insertRouter(@NonNull Router router) {
        SQLiteDatabase database = null;
        try {
            synchronized (DDWRTCompanionSqliteOpenHelper.dbLock) {
                database = dbHelper.getWritableDatabase();
                final String uuid = (isNullOrEmpty(router.getUuid()) ?
                        UUID.randomUUID().toString() : router.getUuid());
                final long insertId = database.insertOrThrow(TABLE_ROUTERS, null, getContentValuesFromRouter(uuid, router));
                Crashlytics.log(Log.DEBUG, LOG_TAG, "insertRouter(" + uuid + " => " + insertId + ")");
                final Router newRouter = getRouter(uuid);

                //Report
                final Map<String, Object> eventMap = new HashMap<>();
                eventMap.put("Model", Utils.isDemoRouter(newRouter) ? DDWRTCompanionConstants.DEMO :
                        Router.getRouterModel(mContext, newRouter));
                ReportingUtils.reportEvent(ReportingUtils.EVENT_ROUTER_ADDED, eventMap);

                return newRouter;
            }
        } catch (final RuntimeException e) {
            ReportingUtils.reportException(null, e);
            return null;
        } finally {
            if (database != null && database.isOpen()) {
                database.close();
            }
        }
    }

    @Nullable
    public Router updateRouter(@NonNull Router router) {
        SQLiteDatabase database = null;
        try {
            synchronized (DDWRTCompanionSqliteOpenHelper.dbLock) {
                database = dbHelper.getWritableDatabase();
                final String uuid = router.getUuid();
                final int update = database.update(TABLE_ROUTERS, getContentValuesFromRouter(uuid, router), String.format(ROUTER_UUID + "='%s'", uuid), null);
                Crashlytics.log(Log.DEBUG, LOG_TAG, "updateRouter(" + uuid + " => " + update + ")");
                final Router routerUpdated = getRouter(uuid);

                //Report
                final Map<String, Object> eventMap = new HashMap<>();
                eventMap.put("Model", Utils.isDemoRouter(routerUpdated) ? DDWRTCompanionConstants.DEMO :
                        Router.getRouterModel(mContext, routerUpdated));
                ReportingUtils.reportEvent(ReportingUtils.EVENT_ROUTER_UPDATED, eventMap);

                return routerUpdated;
            }
        } catch (final RuntimeException e) {
            ReportingUtils.reportException(null, e);
            return null;
        } finally {
            if (database != null && database.isOpen()) {
                database.close();
            }
        }
    }

    public void deleteRouter(String uuid) {
        SQLiteDatabase database = null;
        try {
            synchronized (DDWRTCompanionSqliteOpenHelper.dbLock) {

                database = dbHelper.getWritableDatabase();

                Crashlytics.log(Log.DEBUG,  LOG_TAG, "Delete Router with uuid: " + uuid);
                database.delete(TABLE_ROUTERS, String.format(ROUTER_UUID + "='%s'", uuid), null);

                //Report
                ReportingUtils.reportEvent(ReportingUtils.EVENT_ROUTER_DELETED, null);

            }
        } catch (final RuntimeException e) {
            ReportingUtils.reportException(null, e);
        } finally {
            if (database != null && database.isOpen()) {
                database.close();
            }
        }
    }

    @NonNull
    public List<Router> getAllRouters() {
        SQLiteDatabase database = null;
        try {
            synchronized (DDWRTCompanionSqliteOpenHelper.dbLock) {
                database = dbHelper.getWritableDatabase();
                final List<Router> routers = new ArrayList<>();
                final Cursor cursor = database.query(TABLE_ROUTERS,
                        allColumns, null, null, null, null, COLUMN_ID + " DESC");

                //noinspection TryFinallyCanBeTryWithResources
                try {
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        while (!cursor.isAfterLast()) {
                            final Router router = cursorToRouter(cursor);
                            routers.add(router);
                            cursor.moveToNext();
                        }
                    }
                } finally {
                    // make sure to close the cursor
                    cursor.close();
                }

                return routers;
            }
        } catch (final RuntimeException e) {
            ReportingUtils.reportException(null, e);
            throw e;
        } finally {
            if (database != null && database.isOpen()) {
                database.close();
            }
        }

    }

    @Nullable
    public Router getRouter(String uuid) {
        SQLiteDatabase database = null;
        try {
            synchronized (DDWRTCompanionSqliteOpenHelper.dbLock) {
                database = dbHelper.getWritableDatabase();

                final Cursor cursor = database.query(TABLE_ROUTERS,
                        allColumns, String.format(ROUTER_UUID + "='%s'", uuid),
                        null, null, null,
                        COLUMN_ID + " DESC");

                //noinspection TryFinallyCanBeTryWithResources
                try {
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        return cursorToRouter(cursor);
                    }
                }  finally {
                    cursor.close();
                }

                return null;
            }
        } catch (final RuntimeException e) {
            ReportingUtils.reportException(null, e);
            return null;
        } finally {
            if (database != null && database.isOpen()) {
                database.close();
            }
        }
    }

    @Nullable
    @Override
    public Router getRouter(int id) {
        SQLiteDatabase database = null;
        try {
            synchronized (DDWRTCompanionSqliteOpenHelper.dbLock) {
                database = dbHelper.getWritableDatabase();

                final Cursor cursor = database.query(TABLE_ROUTERS,
                        allColumns, String.format(COLUMN_ID + "=%d", id), null, null, null, COLUMN_ID + " DESC");
                //noinspection TryFinallyCanBeTryWithResources
                try {
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        return cursorToRouter(cursor);
                    }

                } finally {
                    cursor.close();
                }

                return null;
            }
        } catch (final RuntimeException e) {
            ReportingUtils.reportException(null, e);
            return null;
        } finally {
            if (database != null && database.isOpen()) {
                database.close();
            }
        }
    }

    @Override
    public Long insertWANTrafficData(@NonNull WANTrafficData... trafficData) {
        /*
         * Bulk insert, based upon 
         * http://stackoverflow.com/questions/3860008/bulk-insertion-on-android-device/32288474
         */
        SQLiteDatabase database = null;
        try {
            synchronized (DDWRTCompanionSqliteOpenHelper.dbLock) {
                database = dbHelper.getWritableDatabase();
                database.beginTransaction();
                for (final WANTrafficData trafficDataItem : trafficData) {
                    if (trafficDataItem == null) {
                        continue;
                    }
                    database.insert(TABLE_WAN_TRAFFIC, null,
                        getContentValuesFromWANTrafficData(trafficDataItem));
                }
                database.setTransactionSuccessful();
                
                return 1l;
            }
        } catch (final RuntimeException e) {
            ReportingUtils.reportException(null, e);
            return null;
        } finally {
            if (database != null) {
                database.endTransaction();
                if (database.isOpen()) {
                    database.close();
                }
            }
        }
    }

    @Override
    public boolean isWANTrafficDataPresent(@NonNull String router, @NonNull String date) {
        if (isNullOrEmpty(router) || isNullOrEmpty(date)) {
            return false;
        }
        SQLiteDatabase database = null;
        try {
            synchronized (DDWRTCompanionSqliteOpenHelper.dbLock) {
                database = dbHelper.getWritableDatabase();
                final Cursor cursor = database.query(TABLE_WAN_TRAFFIC,
                        wanTrafficAllColumns,
                        String.format("%s = '%s' AND %s = '%s'",
                                TABLE_WAN_TRAFFIC_ROUTER_UUID, router,
                                TABLE_WAN_TRAFFIC_TRAFFIC_DATE, date),
                        null, null, null, null);

                //noinspection TryFinallyCanBeTryWithResources
                try {
                    return cursor.getCount() > 0;
                } finally {
                    // make sure to close the cursor
                    cursor.close();
                }

            }
        } catch (final RuntimeException e) {
            ReportingUtils.reportException(null, e);
            throw e;
        } finally {
            if (database != null && database.isOpen()) {
                database.close();
            }
        }
    }

    @NonNull
    @Override
    public List<WANTrafficData> getWANTrafficDataByRouterByDate(@NonNull String router, @NonNull String date) {
        if (isNullOrEmpty(router) || isNullOrEmpty(date)) {
            return Collections.emptyList();
        }
        SQLiteDatabase database = null;
        try {
            synchronized (DDWRTCompanionSqliteOpenHelper.dbLock) {
                database = dbHelper.getWritableDatabase();
                final List<WANTrafficData> trafficData = new ArrayList<>();
                final Cursor cursor = database.query(TABLE_WAN_TRAFFIC,
                        wanTrafficAllColumns,
                        String.format("%s = '%s' AND %s = '%s%'",
                                TABLE_WAN_TRAFFIC_ROUTER_UUID, router,
                                TABLE_WAN_TRAFFIC_TRAFFIC_DATE, date),
                        null, null, null,
                        TABLE_WAN_TRAFFIC_TRAFFIC_DATE + " ASC");

                //noinspection TryFinallyCanBeTryWithResources
                try {
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        while (!cursor.isAfterLast()) {
                            final WANTrafficData traff = cursorToWANTrafficData(cursor);
                            trafficData.add(traff);
                            cursor.moveToNext();
                        }
                    }
                } finally {
                    // make sure to close the cursor
                    cursor.close();
                }

                return trafficData;
            }
        } catch (final RuntimeException e) {
            ReportingUtils.reportException(null, e);
            throw e;
        } finally {
            if (database != null && database.isOpen()) {
                database.close();
            }
        }
    }

    @NonNull
    @Override
    public List<WANTrafficData> getWANTrafficDataByRouterBetweenDates(@NonNull String router, @NonNull String dateLower, @NonNull String dateHigher) {
        if (isNullOrEmpty(router) || isNullOrEmpty(dateLower) || isNullOrEmpty(dateHigher)) {
            return Collections.emptyList();
        }
        SQLiteDatabase database = null;
        try {
            synchronized (DDWRTCompanionSqliteOpenHelper.dbLock) {
                database = dbHelper.getWritableDatabase();
                final List<WANTrafficData> trafficData = new ArrayList<>();
                final Cursor cursor = database.query(TABLE_WAN_TRAFFIC,
                        wanTrafficAllColumns,
                        String.format("%s = '%s' AND ( %s BETWEEN '%s' AND '%s' )",
                                TABLE_WAN_TRAFFIC_ROUTER_UUID, router,
                                TABLE_WAN_TRAFFIC_TRAFFIC_DATE, dateLower, dateHigher),
                        null, null, null,
                        TABLE_WAN_TRAFFIC_TRAFFIC_DATE + " ASC");

                //noinspection TryFinallyCanBeTryWithResources
                try {
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        while (!cursor.isAfterLast()) {
                            final WANTrafficData traff = cursorToWANTrafficData(cursor);
                            trafficData.add(traff);
                            cursor.moveToNext();
                        }
                    }
                } finally {
                    // make sure to close the cursor
                    cursor.close();
                }

                return trafficData;
            }
        } catch (final RuntimeException e) {
            ReportingUtils.reportException(null, e);
            throw e;
        } finally {
            if (database != null && database.isOpen()) {
                database.close();
            }
        }
    }

    @Override
    public void deleteWANTrafficDataByRouter(@NonNull String router) {
        SQLiteDatabase database = null;
        try {
            synchronized (DDWRTCompanionSqliteOpenHelper.dbLock) {

                database = dbHelper.getWritableDatabase();

                Crashlytics.log(Log.DEBUG,  LOG_TAG, "Delete WAN Traffic Data for Router: " + router);
                database.delete(TABLE_WAN_TRAFFIC, String.format(TABLE_WAN_TRAFFIC_ROUTER_UUID + "='%s'", router), null);

            }
        } catch (final RuntimeException e) {
            ReportingUtils.reportException(null, e);
        } finally {
            if (database != null && database.isOpen()) {
                database.close();
            }
        }
    }


    @NonNull
    private Router cursorToRouter(@NonNull Cursor cursor) {

        final Router router = new Router(this.mContext);
        router.setId(cursor.getInt(0));
        router.setUuid(cursor.getString(1));
        router.setName(cursor.getString(2));
        router.setRemoteIpAddress(cursor.getString(3));
        router.setRouterConnectionProtocol(Router.RouterConnectionProtocol
                .valueOf(cursor.getString(4)));
        router.setRemotePort(cursor.getInt(5));
        router.setStrictHostKeyChecking(cursor.getInt(6) == 1);
        router.setUsername(cursor.getString(7), false);
        router.setPassword(cursor.getString(8), false);
        router.setPrivKey(cursor.getString(9), false);
        router.setRouterFirmware(cursor.getString(10));

        return router;
    }

    @NonNull
    private WANTrafficData cursorToWANTrafficData(@NonNull Cursor cursor) {

        return new WANTrafficData(cursor.getString(1),
                cursor.getString(2), cursor.getDouble(3), cursor.getDouble(4))
                .setId(cursor.getInt(0));
    }


    @NonNull
    private ContentValues getContentValuesFromRouter(String uuid, @NonNull Router router) {
        ContentValues values = new ContentValues();
        values.put(ROUTER_UUID, uuid);
        values.put(ROUTER_IP, router.getRemoteIpAddress());
        values.put(ROUTER_NAME, router.getName());
        values.put(ROUTER_PASSWORD, router.getPassword());
        values.put(ROUTER_PORT, router.getRemotePort());
        values.put(ROUTER_PRIVKEY, router.getPrivKey());
        values.put(ROUTER_SSH_STRICT_HOST_KEY_CHECKING,
                router.isStrictHostKeyChecking() ? 1 : 0);
        values.put(ROUTER_USERNAME, router.getUsername());
        values.put(ROUTER_PROTOCOL, router.getRouterConnectionProtocol().toString());
        final Router.RouterFirmware routerFirmware = router.getRouterFirmware();
        if (routerFirmware != null) {
            values.put(ROUTER_FIRMWARE, routerFirmware.toString());
        }
        return values;
    }

    @NonNull
    private ContentValues getContentValuesFromWANTrafficData(@NonNull WANTrafficData wanTrafficData) {
        ContentValues values = new ContentValues();

        values.put(TABLE_WAN_TRAFFIC_ROUTER_UUID, wanTrafficData.getRouter());
        values.put(TABLE_WAN_TRAFFIC_TRAFFIC_DATE, wanTrafficData.getDate());
        values.put(TABLE_WAN_TRAFFIC_TRAFFIC_IN, wanTrafficData.getTraffIn().doubleValue());
        values.put(TABLE_WAN_TRAFFIC_TRAFFIC_OUT, wanTrafficData.getTraffOut().doubleValue());
        return values;
    }

}