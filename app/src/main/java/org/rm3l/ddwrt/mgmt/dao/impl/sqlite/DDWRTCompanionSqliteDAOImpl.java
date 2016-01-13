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
import org.rm3l.ddwrt.resources.SpeedTestResult;
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
import java.util.concurrent.atomic.AtomicInteger;

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
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.TABLE_SPEED_TEST_RESULTS;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.TABLE_SPEED_TEST_RESULTS_COLUMN_ID;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.TABLE_SPEED_TEST_RESULTS_CONNECTION_DL;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.TABLE_SPEED_TEST_RESULTS_CONNECTION_TYPE;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.TABLE_SPEED_TEST_RESULTS_CONNECTION_UL;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.TABLE_SPEED_TEST_RESULTS_ROUTER_UUID;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.TABLE_SPEED_TEST_RESULTS_SERVER;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.TABLE_SPEED_TEST_RESULTS_SERVER_COUNTRY_CODE;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.TABLE_SPEED_TEST_RESULTS_TEST_DATE;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.TABLE_SPEED_TEST_RESULTS_WAN_DL;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.TABLE_SPEED_TEST_RESULTS_WAN_PING;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.TABLE_SPEED_TEST_RESULTS_WAN_UL;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.TABLE_WAN_TRAFFIC;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.TABLE_WAN_TRAFFIC_COLUMN_ID;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.TABLE_WAN_TRAFFIC_ROUTER_UUID;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.TABLE_WAN_TRAFFIC_TRAFFIC_DATE;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.TABLE_WAN_TRAFFIC_TRAFFIC_IN;
import static org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.TABLE_WAN_TRAFFIC_TRAFFIC_OUT;

public class DDWRTCompanionSqliteDAOImpl implements DDWRTCompanionDAO {

    private static final String LOG_TAG = DDWRTCompanionSqliteDAOImpl.class.getSimpleName();
    private final Context mContext;

    private static DDWRTCompanionSqliteDAOImpl instance;
    private static DDWRTCompanionSqliteOpenHelper dbHelper;
    private SQLiteDatabase mDatabase;

    private final AtomicInteger mOpenCounter;

    // Database fields
    @NonNull
    private static final String[] routersAllColumns = {
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
    private static final String[] wanTrafficAllColumns = {
            TABLE_WAN_TRAFFIC_COLUMN_ID,
            TABLE_WAN_TRAFFIC_ROUTER_UUID,
            TABLE_WAN_TRAFFIC_TRAFFIC_DATE,
            TABLE_WAN_TRAFFIC_TRAFFIC_IN,
            TABLE_WAN_TRAFFIC_TRAFFIC_OUT};

    @NonNull
    private static final String[] speedTestResultsAllColumns = {
            TABLE_SPEED_TEST_RESULTS_COLUMN_ID,
            TABLE_SPEED_TEST_RESULTS_ROUTER_UUID,
            TABLE_SPEED_TEST_RESULTS_TEST_DATE,
            TABLE_SPEED_TEST_RESULTS_SERVER,
            TABLE_SPEED_TEST_RESULTS_WAN_PING,
            TABLE_SPEED_TEST_RESULTS_WAN_DL,
            TABLE_SPEED_TEST_RESULTS_WAN_UL,
            TABLE_SPEED_TEST_RESULTS_CONNECTION_TYPE,
            TABLE_SPEED_TEST_RESULTS_CONNECTION_DL,
            TABLE_SPEED_TEST_RESULTS_CONNECTION_UL};

    public static synchronized void initialize(Context context) {
        if (instance == null) {
            instance = new DDWRTCompanionSqliteDAOImpl(context);
        }
        if (dbHelper == null) {
            dbHelper = new DDWRTCompanionSqliteOpenHelper(context);
        }
    }

    public static synchronized DDWRTCompanionSqliteDAOImpl getInstance() {
        if (instance == null) {
            throw new IllegalStateException(DDWRTCompanionSqliteDAOImpl.class.getSimpleName() +
                    " is not initialized, call initialize(..) method first.");
        }

        return instance;
    }

    private synchronized SQLiteDatabase openDatabase() {
        if(mOpenCounter.incrementAndGet() == 1) {
            // Opening new database
            mDatabase = dbHelper.getWritableDatabase();
        }
        return mDatabase;
    }

    private synchronized void closeDatabase() {
        if(mOpenCounter.decrementAndGet() == 0) {
            // Closing database
            mDatabase.close();
        }
    }

    DDWRTCompanionSqliteDAOImpl(Context context) {
        this.mContext = context;
        this.mOpenCounter = new AtomicInteger();
    }

    public void destroy() {
        try {
            closeDatabase();
        } catch (final Exception e) {
            Utils.reportException(mContext, e);
            e.printStackTrace();
        } finally {
            dbHelper.close();
        }
    }

    @Nullable
    public Router insertRouter(@NonNull Router router) {
        final DDWRTCompanionSqliteDAOImpl instance = getInstance();
        try {
            final String uuid = (isNullOrEmpty(router.getUuid()) ?
                    UUID.randomUUID().toString() : router.getUuid());
            final long insertId = instance.openDatabase().insertOrThrow(TABLE_ROUTERS, null,
                    getContentValuesFromRouter(uuid, router));
            Crashlytics.log(Log.DEBUG, LOG_TAG, "insertRouter(" + uuid + " => " + insertId + ")");
            final Router newRouter = getRouter(uuid);

            //Report
            final Map<String, Object> eventMap = new HashMap<>();
            eventMap.put("Model", Utils.isDemoRouter(newRouter) ? DDWRTCompanionConstants.DEMO :
                    Router.getRouterModel(mContext, newRouter));
            ReportingUtils.reportEvent(ReportingUtils.EVENT_ROUTER_ADDED, eventMap);

            return newRouter;
        } catch (final RuntimeException e) {
            ReportingUtils.reportException(null, e);
            return null;

        } finally {
            instance.closeDatabase();
        }
    }

    @Nullable
    public Router updateRouter(@NonNull Router router) {
        final DDWRTCompanionSqliteDAOImpl instance = getInstance();
        try {
            final String uuid = router.getUuid();
            final int update = instance.openDatabase()
                    .update(TABLE_ROUTERS,
                            getContentValuesFromRouter(uuid, router), String.format(ROUTER_UUID + "='%s'", uuid), null);
            Crashlytics.log(Log.DEBUG, LOG_TAG, "updateRouter(" + uuid + " => " + update + ")");
            final Router routerUpdated = getRouter(uuid);

            //Report
            final Map<String, Object> eventMap = new HashMap<>();
            eventMap.put("Model", Utils.isDemoRouter(routerUpdated) ? DDWRTCompanionConstants.DEMO :
                    Router.getRouterModel(mContext, routerUpdated));
            ReportingUtils.reportEvent(ReportingUtils.EVENT_ROUTER_UPDATED, eventMap);

            return routerUpdated;
        } catch (final RuntimeException e) {
            ReportingUtils.reportException(null, e);
            return null;
        } finally {
            instance.closeDatabase();
        }
    }

    public void deleteRouter(String uuid) {
        final DDWRTCompanionSqliteDAOImpl instance = getInstance();
        try {

            Crashlytics.log(Log.DEBUG,  LOG_TAG, "Delete Router with uuid: " + uuid);
            instance.openDatabase()
                    .delete(TABLE_ROUTERS, String.format(ROUTER_UUID + "='%s'", uuid), null);

            //Report
            ReportingUtils.reportEvent(ReportingUtils.EVENT_ROUTER_DELETED, null);

        } catch (final RuntimeException e) {
            ReportingUtils.reportException(null, e);
        } finally {
            instance.closeDatabase();
        }
    }

    @NonNull
    public List<Router> getAllRouters() {
        final DDWRTCompanionSqliteDAOImpl instance = getInstance();
        try {
            final List<Router> routers = new ArrayList<>();
            final Cursor cursor = instance.openDatabase()
                    .query(TABLE_ROUTERS,
                            routersAllColumns, null, null, null, null, COLUMN_ID + " DESC");

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
        } catch (final RuntimeException e) {
            ReportingUtils.reportException(null, e);
            throw e;
        } finally {
            instance.closeDatabase();
        }

    }

    @Nullable
    public Router getRouter(String uuid) {
        final DDWRTCompanionSqliteDAOImpl instance = getInstance();

        try {

            final Cursor cursor = instance.openDatabase()
                    .query(TABLE_ROUTERS,
                            routersAllColumns, String.format(ROUTER_UUID + "='%s'", uuid),
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

        } catch (final RuntimeException e) {
            ReportingUtils.reportException(null, e);
            return null;
        } finally {
            instance.closeDatabase();
        }
    }

    @Nullable
    @Override
    public Router getRouter(int id) {
        final DDWRTCompanionSqliteDAOImpl instance = getInstance();

        try {
            final Cursor cursor = instance.openDatabase()
                    .query(TABLE_ROUTERS,
                            routersAllColumns,
                            String.format(COLUMN_ID + "=%d", id), null, null, null, COLUMN_ID + " DESC");
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

        } catch (final RuntimeException e) {
            ReportingUtils.reportException(null, e);
            return null;
        } finally {
            instance.closeDatabase();
        }
    }

    @Override
    public Long insertWANTrafficData(@NonNull WANTrafficData... trafficData) {
        /*
         * Bulk insert, based upon 
         * http://stackoverflow.com/questions/3860008/bulk-insertion-on-android-device/32288474
         */
        final DDWRTCompanionSqliteDAOImpl instance = getInstance();

        SQLiteDatabase database = null;
        try {
            database = instance.openDatabase();
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
        } catch (final RuntimeException e) {
            ReportingUtils.reportException(null, e);
            return null;
        } finally {
            if (database != null) {
                database.endTransaction();
            }
            instance.closeDatabase();
        }
    }

    @Override
    public boolean isWANTrafficDataPresent(@NonNull String router, @NonNull String date) {
        if (isNullOrEmpty(router) || isNullOrEmpty(date)) {
            return false;
        }
        final DDWRTCompanionSqliteDAOImpl instance = getInstance();

        try {

            final Cursor cursor = instance
                    .openDatabase()
                    .query(TABLE_WAN_TRAFFIC,
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

        } catch (final RuntimeException e) {
            ReportingUtils.reportException(null, e);
            throw e;
        } finally {
            instance.closeDatabase();
        }
    }

    @NonNull
    @Override
    public List<WANTrafficData> getWANTrafficDataByRouterByDate(@NonNull String router, @NonNull String date) {

        if (isNullOrEmpty(router) || isNullOrEmpty(date)) {
            return Collections.emptyList();
        }
        final DDWRTCompanionSqliteDAOImpl instance = getInstance();

        try {
            final List<WANTrafficData> trafficData = new ArrayList<>();
            final Cursor cursor = instance.openDatabase()
                    .query(TABLE_WAN_TRAFFIC,
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
        } catch (final RuntimeException e) {
            ReportingUtils.reportException(null, e);
            throw e;

        } finally {
            instance.closeDatabase();
        }
    }

    @NonNull
    @Override
    public List<WANTrafficData> getWANTrafficDataByRouterBetweenDates(@NonNull String router, @NonNull String dateLower, @NonNull String dateHigher) {
        if (isNullOrEmpty(router) || isNullOrEmpty(dateLower) || isNullOrEmpty(dateHigher)) {
            return Collections.emptyList();
        }
        final DDWRTCompanionSqliteDAOImpl instance = getInstance();

        try {
            final List<WANTrafficData> trafficData = new ArrayList<>();
            final Cursor cursor = instance.openDatabase().query(TABLE_WAN_TRAFFIC,
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

        } catch (final RuntimeException e) {
            ReportingUtils.reportException(null, e);
            throw e;

        } finally {
            instance.closeDatabase();
        }
    }

    @Override
    public void deleteWANTrafficDataByRouter(@NonNull String router) {
        final DDWRTCompanionSqliteDAOImpl instance = getInstance();
        try {

            Crashlytics.log(Log.DEBUG,  LOG_TAG, "Delete WAN Traffic Data for Router: " + router);
            instance.openDatabase()
                    .delete(TABLE_WAN_TRAFFIC, String.format(TABLE_WAN_TRAFFIC_ROUTER_UUID + "='%s'", router), null);

        } catch (final RuntimeException e) {
            ReportingUtils.reportException(null, e);

        } finally {
            instance.closeDatabase();
        }
    }

    @Nullable
    @Override
    public Long insertSpeedTestResult(@NonNull SpeedTestResult speedTestResult) {
        final DDWRTCompanionSqliteDAOImpl instance = getInstance();
        try {
            return instance.openDatabase()
                    .insertOrThrow(TABLE_SPEED_TEST_RESULTS, null,
                        getContentValuesFromSpeedTestResult(speedTestResult));
        } catch (final RuntimeException e) {
            ReportingUtils.reportException(null, e);
            return null;

        } finally {
            instance.closeDatabase();
        }
    }

    @NonNull
    @Override
    public List<SpeedTestResult> getSpeedTestResultsByRouter(@NonNull String router) {
        if (isNullOrEmpty(router)) {
            return Collections.emptyList();
        }
        final DDWRTCompanionSqliteDAOImpl instance = getInstance();

        try {
            final List<SpeedTestResult> speedTestResults = new ArrayList<>();
            final Cursor cursor = instance.openDatabase()
                    .query(TABLE_SPEED_TEST_RESULTS,
                            speedTestResultsAllColumns,
                            String.format("%s = '%s'",
                                    TABLE_SPEED_TEST_RESULTS_ROUTER_UUID, router),
                            null, null, null,
                            TABLE_SPEED_TEST_RESULTS_TEST_DATE + " DESC");

            //noinspection TryFinallyCanBeTryWithResources
            try {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        final SpeedTestResult speedTestResult = cursorToSpeedTestResult(cursor);
                        speedTestResults.add(speedTestResult);
                        cursor.moveToNext();
                    }
                }
            } finally {
                // make sure to close the cursor
                cursor.close();
            }

            return speedTestResults;
        } catch (final RuntimeException e) {
            ReportingUtils.reportException(null, e);
            throw e;

        } finally {
            instance.closeDatabase();
        }
    }

    @Override
    public void deleteSpeedTestResultByRouterById(@NonNull String router, long id) {
        final DDWRTCompanionSqliteDAOImpl instance = getInstance();
        try {

            instance.openDatabase()
                    .delete(TABLE_SPEED_TEST_RESULTS,
                            String.format("%s='%s' AND %s=%d",
                                    TABLE_SPEED_TEST_RESULTS_ROUTER_UUID, router,
                                    TABLE_SPEED_TEST_RESULTS_COLUMN_ID, (int) id), null);

        } catch (final RuntimeException e) {
            ReportingUtils.reportException(null, e);

        } finally {
            instance.closeDatabase();
        }
    }

    @Override
    public void deleteAllSpeedTestResultsByRouter(@NonNull String router) {
        final DDWRTCompanionSqliteDAOImpl instance = getInstance();
        try {

            instance.openDatabase()
                    .delete(TABLE_SPEED_TEST_RESULTS,
                            String.format("%s='%s'",
                                    TABLE_SPEED_TEST_RESULTS_ROUTER_UUID, router),
                            null);

        } catch (final RuntimeException e) {
            ReportingUtils.reportException(null, e);

        } finally {
            instance.closeDatabase();
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
    private SpeedTestResult cursorToSpeedTestResult(@NonNull Cursor cursor) {

        return new SpeedTestResult(cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3),
                cursor.getDouble(4), cursor.getDouble(5), cursor.getDouble(6),
                cursor.getString(7), cursor.getDouble(8), cursor.getDouble(9),
                cursor.getString(10))
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

    @NonNull
    private ContentValues getContentValuesFromSpeedTestResult(@NonNull SpeedTestResult speedTestResult) {
        ContentValues values = new ContentValues();

        values.put(TABLE_SPEED_TEST_RESULTS_ROUTER_UUID, speedTestResult.getRouter());
        values.put(TABLE_SPEED_TEST_RESULTS_TEST_DATE, speedTestResult.getDate());
        values.put(TABLE_SPEED_TEST_RESULTS_SERVER, speedTestResult.getServer());
        values.put(TABLE_SPEED_TEST_RESULTS_WAN_PING, speedTestResult.getWanPing().doubleValue());
        values.put(TABLE_SPEED_TEST_RESULTS_WAN_DL, speedTestResult.getWanDl().doubleValue());
        values.put(TABLE_SPEED_TEST_RESULTS_WAN_UL, speedTestResult.getWanUl().doubleValue());
        values.put(TABLE_SPEED_TEST_RESULTS_CONNECTION_TYPE, speedTestResult.getConnectionType());
        final Number connectionDl = speedTestResult.getConnectionDl();
        values.put(TABLE_SPEED_TEST_RESULTS_CONNECTION_DL, connectionDl != null ?
                connectionDl.doubleValue() : null);
        final Number connectionUl = speedTestResult.getConnectionUl();
        values.put(TABLE_SPEED_TEST_RESULTS_CONNECTION_UL,
                connectionUl != null ? connectionUl.doubleValue() : null);
        values.put(TABLE_SPEED_TEST_RESULTS_SERVER_COUNTRY_CODE, speedTestResult.getServerCountryCode());

        return values;
    }

}