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

package org.rm3l.ddwrt.mgmt.dao.impl.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.jetbrains.annotations.NotNull;

public class DDWRTCompanionSqliteOpenHelper extends SQLiteOpenHelper {

    public static final String TABLE_ROUTERS = "routers";
    public static final String COLUMN_ID = "_id";
    public static final String ROUTER_UUID = "uuid";
    public static final String ROUTER_NAME = "name";
    public static final String ROUTER_PROTOCOL = "proto";
    public static final String ROUTER_IP = "reachable_addr";
    public static final String ROUTER_USERNAME = "username";
    public static final String ROUTER_PASSWORD = "password";
    public static final String ROUTER_PRIVKEY = "privkey";
    public static final String ROUTER_PRIVKEY_PATH = "privkey_path";
    public static final String ROUTER_PORT = "port";
    public static final String ROUTER_SSH_STRICT_HOST_KEY_CHECKING = "ssh_strict_host_key_checking";
    // Database creation sql statement
    private static final String DATABASE_CREATE = "CREATE TABLE " + TABLE_ROUTERS +
            " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            ROUTER_UUID + " TEXT NOT NULL UNIQUE, " +
            ROUTER_NAME + " TEXT DEFAULT NULL, " +
            ROUTER_IP + " TEXT NOT NULL, " +
            ROUTER_PROTOCOL + " TEXT NOT NULL, " +
            ROUTER_PORT + " INTEGER NOT NULL DEFAULT 22 CHECK(" + ROUTER_PORT + " > 0), " +
            ROUTER_SSH_STRICT_HOST_KEY_CHECKING + " INTEGER NOT NULL DEFAULT 0, " +
            ROUTER_USERNAME + " TEXT NOT NULL, " +
            ROUTER_PASSWORD + " TEXT DEFAULT NULL, " +
            ROUTER_PRIVKEY + " TEXT DEFAULT NULL," +
            ROUTER_PRIVKEY_PATH + " TEXT DEFAULT NULL" +
            ");";
    private static final String DATABASE_NAME = "routers.db";
    private static final int DATABASE_VERSION = 1;

    public DDWRTCompanionSqliteOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(@NotNull SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {

    }
}
