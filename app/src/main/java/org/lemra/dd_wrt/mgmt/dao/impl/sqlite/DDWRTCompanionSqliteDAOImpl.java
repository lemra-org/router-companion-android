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

import org.jetbrains.annotations.Nullable;
import org.lemra.dd_wrt.api.conn.Router;
import org.lemra.dd_wrt.mgmt.dao.DDWRTCompanionDAO;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.lemra.dd_wrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper.*;

public class DDWRTCompanionSqliteDAOImpl implements DDWRTCompanionDAO {

    private final DDWRTCompanionSqliteOpenHelper dbHelper;

    // Database fields
    private SQLiteDatabase database;
    private String[] allColumns = { COLUMN_ID, ROUTER_UUID, ROUTER_PROTOCOL,
            ROUTER_IP, ROUTER_NAME, ROUTER_PASSWORD,
            ROUTER_PORT, ROUTER_PRIVKEY, ROUTER_SSH_STRICT_HOST_KEY_CHECKING, ROUTER_USERNAME};

    public DDWRTCompanionSqliteDAOImpl(Context context) {
        dbHelper = new DDWRTCompanionSqliteOpenHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public Router createRouter(Router router) {
        ContentValues values = new ContentValues();
        values.put(ROUTER_UUID, UUID.randomUUID().toString());
        values.put(ROUTER_IP, router.getRemoteIpAddress());
        values.put(ROUTER_NAME, router.getName());
        values.put(ROUTER_PASSWORD, router.getPassword());
        values.put(ROUTER_PORT, router.getRemotePort());
        values.put(ROUTER_PRIVKEY, router.getPrivKey());
        values.put(ROUTER_SSH_STRICT_HOST_KEY_CHECKING, router.isStrictHostKeyChecking());
        values.put(ROUTER_USERNAME, router.getUsername());
        values.put(ROUTER_PROTOCOL, router.getRouterConnectionProtocol().toString());

        long insertId = database.insert(TABLE_ROUTERS, null,
                values);

        Cursor cursor = database.query(TABLE_ROUTERS,
                allColumns, COLUMN_ID + " = " + insertId, null,
                null, null, null);
        cursor.moveToFirst();
        Router newRouter = cursorToRouter(cursor);
        cursor.close();
        return newRouter;
    }

    public void deleteRouter(String uuid) {
        System.out.println("Delete Router with uuid: " + uuid);
        database.delete(TABLE_ROUTERS, ROUTER_UUID
                + " = " + uuid, null);
    }

    public List<Router> getAllRouters() {
        List<Router> routers = new ArrayList<Router>();

        Cursor cursor = database.query(TABLE_ROUTERS,
                allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Router router = cursorToRouter(cursor);
            routers.add(router);
            cursor.moveToNext();
        }
        // make sure to close the cursor 
        cursor.close();
        return routers;
    }

    @Nullable
    public Router getRouter(String uuid) {
        final Cursor cursor = database.query(TABLE_ROUTERS,
                allColumns, ROUTER_UUID + " = " + uuid, null, null, null, null);
        cursor.moveToFirst();
        Router router = cursorToRouter(cursor);
        cursor.close();
        return router;
    }

    private Router cursorToRouter(Cursor cursor) {

        final Router router = new Router();
        router.setUuid(cursor.getString(1));
        router.setName(cursor.getString(2));
        router.setRemoteIpAddress(cursor.getString(3));
        router.setRouterConnectionProtocol(Router.RouterConnectionProtocol.valueOf(cursor.getString(4)));
        router.setRemotePort(cursor.getInt(5));
        router.setStrictHostKeyChecking(cursor.getInt(6) == 1);
        router.setUsername(cursor.getString(7));
        router.setPassword(cursor.getString(8));

        return router;
    }

}
