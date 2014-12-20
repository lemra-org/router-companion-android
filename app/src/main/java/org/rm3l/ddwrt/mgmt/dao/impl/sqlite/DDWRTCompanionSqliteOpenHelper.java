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
    public static final String ROUTER_PRIVKEY = \"fake-key\";
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
            ROUTER_PRIVKEY + " TEXT DEFAULT NULL" +
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
