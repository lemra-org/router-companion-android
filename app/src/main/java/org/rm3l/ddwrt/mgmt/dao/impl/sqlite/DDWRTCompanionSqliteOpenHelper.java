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
import android.support.annotation.NonNull;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import org.rm3l.ddwrt.resources.conn.Router;

import java.util.Map;

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
    public static final String ROUTER_FIRMWARE = "firmware";
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
            ROUTER_PRIVKEY + " TEXT DEFAULT NULL, " +
            ROUTER_FIRMWARE + " TEXT DEFAULT NULL" +
            ");";
    private static final String DATABASE_NAME = "routers.db";

    /*
     TODO In case of DB upgrades, don't forget to increment this field,
     update DATABASE_CREATE (for newer installs), and
     add an entry into DATABASE_UPGRADES map
    */
    private static final int DATABASE_VERSION = 2;

    private static final Map<Integer, String> DATABASE_UPGRADES = Maps.newHashMap();

    static {
        //V2: Add Router Firmware
        DATABASE_UPGRADES.put(2,
                String.format("ALTER TABLE %s ADD COLUMN %s TEXT DEFAULT NULL; " +
                                "UPDATE %s SET %s=\"%s\";",
                        TABLE_ROUTERS, ROUTER_FIRMWARE,
                        TABLE_ROUTERS, ROUTER_FIRMWARE, Router.RouterFirmware.DDWRT));
    }

    //10 au 11 avril => compact diesel
    public DDWRTCompanionSqliteOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(@NonNull SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //this assumes monotonically increasing version numbers for upgrades.

        /*
         Leverage Incremental Updates, as described here:
         http://blog.adamsbros.org/2012/02/28/upgrade-android-sqlite-database/
         */
        int upgradeTo = oldVersion + 1;
        while (upgradeTo <= newVersion) {
            //Loop because we do not know what version users will be converted from or to
            final String upgradeToSql = DATABASE_UPGRADES.get(upgradeTo);
            if (!Strings.isNullOrEmpty(upgradeToSql)) {
                db.execSQL(upgradeToSql);
            }
            upgradeTo++;
//            }
//            switch (upgradeTo)
//            {
//                case 2:
//                    db.execSQL(SQLiteSet.V5_ADD_LAST_CARD);
//                    db.execSQL(SQLiteCard.V5_ADD_FAILED);
//                    break;
//                case 6:
//                    db.execSQL(SQLiteSet.V6_ADD_IMPORT_TYPE);
//                    break;
//                case 7:
//                    db.execSQL(SQLiteSet.V7_ADD_SHORT_FNAME);
//                    break;
//            }
//            upgradeTo++;
        }
    }
}
