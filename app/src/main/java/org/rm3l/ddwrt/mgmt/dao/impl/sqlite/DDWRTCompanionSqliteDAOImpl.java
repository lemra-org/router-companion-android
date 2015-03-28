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

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

public class DDWRTCompanionSqliteDAOImpl implements DDWRTCompanionDAO {

    private static final String LOG_TAG = DDWRTCompanionSqliteDAOImpl.class.getSimpleName();
    final Map<String, Integer> routersToIds = Maps.newConcurrentMap();
    @NonNull
    private final DDWRTCompanionSqliteOpenHelper dbHelper;
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

    public DDWRTCompanionSqliteDAOImpl(Context context) {
        dbHelper = new DDWRTCompanionSqliteOpenHelper(context);
    }

    @NonNull
    private static Router cursorToRouter(@NonNull Cursor cursor) {

        final Router router = new Router();
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

    private void updateRouterIds() {
        final List<Router> allRouters = this.getAllRouters();
        int i = 0;
        for (Router aRouter : allRouters) {
            routersToIds.put(aRouter.getUuid(), i++);
        }
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
                final String uuid = (Strings.isNullOrEmpty(router.getUuid()) ?
                        UUID.randomUUID().toString() : router.getUuid());
                final long insertId = database.insertOrThrow(TABLE_ROUTERS, null, getContentValues(uuid, router));
                Log.d(LOG_TAG, "insertRouter(" + uuid + " => " + insertId + ")");

                return getRouter(uuid);
            }
        } catch (final RuntimeException e) {
            Utils.reportException(e);
            throw e;
        } finally {
            if (database != null && database.isOpen()) {
                Log.d(LOG_TAG, "insertRouter: close db");
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
                final int update = database.update(TABLE_ROUTERS, getContentValues(uuid, router), String.format(ROUTER_UUID + "='%s'", uuid), null);
                Log.d(LOG_TAG, "updateRouter(" + uuid + " => " + update + ")");
                return getRouter(uuid);
            }
        } catch (final RuntimeException e) {
            Utils.reportException(e);
            throw e;
        } finally {
            if (database != null && database.isOpen()) {
                Log.d(LOG_TAG, "updateRouter: close db");
                database.close();
            }
        }
    }

    @NonNull
    private ContentValues getContentValues(String uuid, @NonNull Router router) {
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

    public void deleteRouter(String uuid) {
        SQLiteDatabase database = null;
        try {
            synchronized (DDWRTCompanionSqliteOpenHelper.dbLock) {

                database = dbHelper.getWritableDatabase();

                System.out.println("Delete Router with uuid: " + uuid);
                database.delete(TABLE_ROUTERS, String.format(ROUTER_UUID + "='%s'", uuid), null);
            }
        } catch (final RuntimeException e) {
            Utils.reportException(e);
            throw e;
        } finally {
            if (database != null && database.isOpen()) {
                Log.d(LOG_TAG, "deleteRouter: close db");
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
                final List<Router> routers = new ArrayList<Router>();
                final Cursor cursor = database.query(TABLE_ROUTERS,
                        allColumns, null, null, null, null, COLUMN_ID + " DESC");

                try {
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        while (!cursor.isAfterLast()) {
                            Router router = cursorToRouter(cursor);
                            routers.add(router);
                            cursor.moveToNext();
                        }
                    }
                } finally {
                    // make sure to close the cursor
                    cursor.close();
                }

                return Utils.dbIdsToPosition(routers);
            }
        } catch (final RuntimeException e) {
            Utils.reportException(e);
            throw e;
        } finally {
            if (database != null && database.isOpen()) {
                Log.d(LOG_TAG, "getAllRouters: close db");
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
                        allColumns, String.format(ROUTER_UUID + "='%s'", uuid), null, null, null, COLUMN_ID + " DESC");
                try {
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        final Router router = cursorToRouter(cursor);
                        updateRouterIds();

                        router.setId(routersToIds.get(router.getUuid()));
                        return router;
                    }

                } finally {
                    cursor.close();
                }

                return null;
            }
        } catch (final RuntimeException e) {
            Utils.reportException(e);
            throw e;
        } finally {
            if (database != null && database.isOpen()) {
                Log.d(LOG_TAG, "getAllRouters: close db");
                database.close();
            }
        }
    }

}
