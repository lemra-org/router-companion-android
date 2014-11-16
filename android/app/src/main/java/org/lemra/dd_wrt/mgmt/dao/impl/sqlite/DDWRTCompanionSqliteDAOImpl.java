/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Armel S.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.lemra.dd_wrt.mgmt.dao.impl.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lemra.dd_wrt.api.conn.Router;
import org.lemra.dd_wrt.mgmt.dao.DDWRTCompanionDAO;
import org.lemra.dd_wrt.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.lemra.dd_wrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.COLUMN_ID;
import static org.lemra.dd_wrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.ROUTER_IP;
import static org.lemra.dd_wrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.ROUTER_NAME;
import static org.lemra.dd_wrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.ROUTER_PASSWORD;
import static org.lemra.dd_wrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.ROUTER_PORT;
import static org.lemra.dd_wrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.ROUTER_PRIVKEY;
import static org.lemra.dd_wrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.ROUTER_PRIVKEY_PATH;
import static org.lemra.dd_wrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.ROUTER_PROTOCOL;
import static org.lemra.dd_wrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.ROUTER_SSH_STRICT_HOST_KEY_CHECKING;
import static org.lemra.dd_wrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.ROUTER_USERNAME;
import static org.lemra.dd_wrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.ROUTER_UUID;
import static org.lemra.dd_wrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.TABLE_ROUTERS;

public class DDWRTCompanionSqliteDAOImpl implements DDWRTCompanionDAO {

    private static final String LOG_TAG = DDWRTCompanionSqliteDAOImpl.class.getSimpleName();
    final Map<String, Integer> routersToIds = Maps.newConcurrentMap();
    @NotNull
    private final DDWRTCompanionSqliteOpenHelper dbHelper;
    // Database fields
//    private SQLiteDatabase database;
    @NotNull
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
            ROUTER_PRIVKEY_PATH};

    public DDWRTCompanionSqliteDAOImpl(Context context) {
        dbHelper = new DDWRTCompanionSqliteOpenHelper(context);
    }

    @NotNull
    private static Router cursorToRouter(@NotNull Cursor cursor) {

        @NotNull final Router router = new Router();
        router.setId(cursor.getInt(0));
        router.setUuid(cursor.getString(1));
        router.setName(cursor.getString(2));
        router.setRemoteIpAddress(cursor.getString(3));
        router.setRouterConnectionProtocol(Router.RouterConnectionProtocol
                .valueOf(cursor.getString(4)));
        router.setRemotePort(cursor.getInt(5));
        router.setStrictHostKeyChecking(cursor.getInt(6) == 1);
        router.setUsername(cursor.getString(7));
        router.setPassword(cursor.getString(8));
        router.setPrivKey(cursor.getString(9));
        router.setPrivkeyPath(cursor.getString(10));

        return router;
    }

    private void updateRouterIds() {
        @NotNull final List<Router> allRouters = this.getAllRouters();
        int i = 0;
        for (@NotNull Router aRouter : allRouters) {
            routersToIds.put(aRouter.getUuid(), i++);
        }
    }

    public void destroy() {
        dbHelper.close();
    }

    @Nullable
    public Router insertRouter(@NotNull Router router) {
        SQLiteDatabase database = null;
        try {
            database = dbHelper.getWritableDatabase();
            @NotNull final String uuid = (Strings.isNullOrEmpty(router.getUuid()) ?
                    UUID.randomUUID().toString() : router.getUuid());
            long insertId = database.insertOrThrow(TABLE_ROUTERS, null, getContentValues(uuid, router));
            Log.d(LOG_TAG, "insertRouter(" + uuid + " => " + insertId + ")");
            return getRouter(uuid);
        } finally {
            if (database != null && database.isOpen()) {
                Log.d(LOG_TAG, "insertRouter: close db");
                database.close();
            }
        }
    }

    @Nullable
    public Router updateRouter(@NotNull Router router) {
        SQLiteDatabase database = null;
        try {
            database = dbHelper.getWritableDatabase();
            @NotNull final String uuid = router.getUuid();
            final int update = database.update(TABLE_ROUTERS, getContentValues(uuid, router), String.format(ROUTER_UUID + "='%s'", uuid), null);
            Log.d(LOG_TAG, "updateRouter(" + uuid + " => " + update + ")");
            return getRouter(uuid);
        } finally {
            if (database != null && database.isOpen()) {
                Log.d(LOG_TAG, "updateRouter: close db");
                database.close();
            }
        }
    }

    @NotNull
    private ContentValues getContentValues(String uuid, @NotNull Router router) {
        @NotNull ContentValues values = new ContentValues();
        values.put(ROUTER_UUID, uuid);
        values.put(ROUTER_IP, router.getRemoteIpAddress());
        values.put(ROUTER_NAME, router.getName());
        values.put(ROUTER_PASSWORD, router.getPassword());
        values.put(ROUTER_PORT, router.getRemotePort());
        values.put(ROUTER_PRIVKEY, router.getPrivKey());
        values.put(ROUTER_PRIVKEY_PATH, router.getPrivkeyPath());
        values.put(ROUTER_SSH_STRICT_HOST_KEY_CHECKING,
                router.isStrictHostKeyChecking() ? 1 : 0);
        values.put(ROUTER_USERNAME, router.getUsername());
        values.put(ROUTER_PROTOCOL, router.getRouterConnectionProtocol().toString());
        return values;
    }

    public void deleteRouter(String uuid) {
        SQLiteDatabase database = null;
        try {
            database = dbHelper.getWritableDatabase();

            System.out.println("Delete Router with uuid: " + uuid);
            database.delete(TABLE_ROUTERS, String.format(ROUTER_UUID + "='%s'", uuid), null);
        } finally {
            if (database != null && database.isOpen()) {
                Log.d(LOG_TAG, "deleteRouter: close db");
                database.close();
            }
        }
    }

    @NotNull
    public List<Router> getAllRouters() {
        SQLiteDatabase database = null;
        try {
            database = dbHelper.getWritableDatabase();


            @NotNull List<Router> routers = new ArrayList<Router>();

            Cursor cursor = database.query(TABLE_ROUTERS,
                    allColumns, null, null, null, null, COLUMN_ID + " DESC");

            try {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        @NotNull Router router = cursorToRouter(cursor);
                        routers.add(router);
                        cursor.moveToNext();
                    }
                }
            } finally {
                // make sure to close the cursor
                cursor.close();
            }

            return Utils.dbIdsToPosition(routers);
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
            database = dbHelper.getWritableDatabase();


            final Cursor cursor = database.query(TABLE_ROUTERS,
                    allColumns, String.format(ROUTER_UUID + "='%s'", uuid), null, null, null, COLUMN_ID + " DESC");
            try {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    @NotNull final Router router = cursorToRouter(cursor);
                    updateRouterIds();

                    router.setId(routersToIds.get(router.getUuid()));
                    return router;
                }

            } finally {
                cursor.close();
            }

            return null;
        } finally {
            if (database != null && database.isOpen()) {
                Log.d(LOG_TAG, "getAllRouters: close db");
                database.close();
            }
        }
    }

}
