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
 * Contact Info: Armel Soro <armel+router_companion@rm3l.org>
 */

package org.rm3l.router_companion.mgmt.dao.impl.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import java.util.Collection;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.router_companion.resources.conn.Router;

public class DDWRTCompanionSqliteOpenHelper extends SQLiteOpenHelper {

  public static final String ORDER_INDEX = "order_idx";

  /*
   * DB Table: Routers
   */
  public static final String ARCHIVED = "archived";

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

  public static final String ROUTER_ICON_METHOD = "icon_method";

  public static final String ROUTER_CUSTOM_ICON = "custom_icon";

  /*
   * DB Table: wan_traffic
   */
  public static final String TABLE_WAN_TRAFFIC = "wan_traffic";

  public static final String TABLE_WAN_TRAFFIC_COLUMN_ID = "_id";

  public static final String TABLE_WAN_TRAFFIC_ROUTER_UUID = "fk_router_uuid";

  public static final String TABLE_WAN_TRAFFIC_TRAFFIC_DATE = "traff_date";

  public static final String TABLE_WAN_TRAFFIC_TRAFFIC_IN = "traff_in";

  public static final String TABLE_WAN_TRAFFIC_TRAFFIC_OUT = "traff_out";

  /*
   * DB Table: speed_test_results
   */
  public static final String TABLE_SPEED_TEST_RESULTS = "speed_test_results";

  public static final String TABLE_SPEED_TEST_RESULTS_COLUMN_ID = "_id";

  public static final String TABLE_SPEED_TEST_RESULTS_ROUTER_UUID = "fk_router_uuid";

  public static final String TABLE_SPEED_TEST_RESULTS_TEST_DATE = "test_date";

  public static final String TABLE_SPEED_TEST_RESULTS_SERVER = "server";

  public static final String TABLE_SPEED_TEST_RESULTS_WAN_PING = "wan_ping";

  public static final String TABLE_SPEED_TEST_RESULTS_WAN_PING_MIN = "wan_ping_min";

  public static final String TABLE_SPEED_TEST_RESULTS_WAN_PING_MAX = "wan_ping_max";

  public static final String TABLE_SPEED_TEST_RESULTS_WAN_PING_STDDEV = "wan_ping_stddev";

  public static final String TABLE_SPEED_TEST_RESULTS_WAN_PING_PACKETS_LOSS =
      "wan_ping_packets_loss";

  public static final String TABLE_SPEED_TEST_RESULTS_WAN_DL = "wan_dl";

  public static final String TABLE_SPEED_TEST_RESULTS_WAN_DL_FILESIZE = "wan_dl_filesize";

  public static final String TABLE_SPEED_TEST_RESULTS_WAN_DL_DURATION = "wan_dl_duration";

  public static final String TABLE_SPEED_TEST_RESULTS_WAN_UL = "wan_ul";

  public static final String TABLE_SPEED_TEST_RESULTS_WAN_UL_FILESIZE = "wan_ul_filesize";

  public static final String TABLE_SPEED_TEST_RESULTS_WAN_UL_DURATION = "wan_ul_duration";

  public static final String TABLE_SPEED_TEST_RESULTS_CONNECTION_TYPE = "conn_type";

  public static final String TABLE_SPEED_TEST_RESULTS_CONNECTION_DL = "conn_dl";

  public static final String TABLE_SPEED_TEST_RESULTS_CONNECTION_DL_FILESIZE = "conn_dl_filesize";

  public static final String TABLE_SPEED_TEST_RESULTS_CONNECTION_DL_DURATION = "conn_dl_duration";

  public static final String TABLE_SPEED_TEST_RESULTS_CONNECTION_UL = "conn_ul";

  public static final String TABLE_SPEED_TEST_RESULTS_CONNECTION_UL_FILESIZE = "conn_ul_filesize";

  public static final String TABLE_SPEED_TEST_RESULTS_CONNECTION_UL_DURATION = "conn_ul_duration";

  public static final String TABLE_SPEED_TEST_RESULTS_SERVER_COUNTRY_CODE = "server_country_code";

  /*
   * DB Table: actions_audit_log
   */
  public static final String TABLE_ACTIONS_AUDIT_LOG = "actions_audit_log";

  public static final String TABLE_ACTIONS_AUDIT_LOG_COLUMN_ID = "_id";

  public static final String TABLE_ACTIONS_AUDIT_LOG_UUID = "uuid";

  public static final String TABLE_ACTIONS_AUDIT_LOG_ROUTER_UUID = "fk_router_uuid";

  public static final String TABLE_ACTIONS_AUDIT_LOG_ORIGIN = "origin";

  public static final String TABLE_ACTIONS_AUDIT_LOG_TRIGGER_DATE = "triggerDate";

  public static final String TABLE_ACTIONS_AUDIT_LOG_ACTION_NAME = "name";

  public static final String TABLE_ACTIONS_AUDIT_LOG_ACTION_DATA = "data";

  public static final String TABLE_ACTIONS_AUDIT_LOG_ACTION_STATUS = "status";

  /** DB */
  public static final String DATABASE_NAME = "routers.db";

  private static final String TAG = DDWRTCompanionSqliteOpenHelper.class.getSimpleName();

  // Database creation sql statement
  private static final String DATABASE_CREATE =
      "CREATE TABLE IF NOT EXISTS "
          + TABLE_ROUTERS
          + " ("
          + COLUMN_ID
          + " INTEGER PRIMARY KEY AUTOINCREMENT, "
          + ROUTER_UUID
          + " TEXT NOT NULL UNIQUE, "
          + ROUTER_NAME
          + " TEXT DEFAULT NULL, "
          + ROUTER_IP
          + " TEXT NOT NULL, "
          + ROUTER_PROTOCOL
          + " TEXT NOT NULL, "
          + ROUTER_PORT
          + " INTEGER NOT NULL DEFAULT 22 CHECK("
          + ROUTER_PORT
          + " > 0), "
          + ROUTER_SSH_STRICT_HOST_KEY_CHECKING
          + " INTEGER NOT NULL DEFAULT 0, "
          + ROUTER_USERNAME
          + " TEXT NOT NULL, "
          + ROUTER_PASSWORD
          + " TEXT DEFAULT NULL, "
          + ROUTER_PRIVKEY
          + " TEXT DEFAULT NULL, "
          + ROUTER_FIRMWARE
          + " TEXT DEFAULT NULL, "
          + ORDER_INDEX
          + " INTEGER NOT NULL DEFAULT 1000, "
          + ARCHIVED
          + " INTEGER NOT NULL DEFAULT 0 CHECK ("
          + ARCHIVED
          + " IN (0,1)),"
          + ROUTER_ICON_METHOD
          + " INTEGER NOT NULL DEFAULT "
          + Router.RouterIcon_Auto
          + ", "
          + ROUTER_CUSTOM_ICON
          + " TEXT DEFAULT NULL"
          + ");";

  // Database creation sql statement
  private static final String TABLE_WAN_TRAFFIC_CREATE =
      "CREATE TABLE IF NOT EXISTS "
          + TABLE_WAN_TRAFFIC
          + " ("
          + TABLE_WAN_TRAFFIC_COLUMN_ID
          + " INTEGER PRIMARY KEY AUTOINCREMENT, "
          + TABLE_WAN_TRAFFIC_ROUTER_UUID
          + " TEXT NOT NULL, "
          + TABLE_WAN_TRAFFIC_TRAFFIC_DATE
          + " TEXT NOT NULL, "
          + TABLE_WAN_TRAFFIC_TRAFFIC_IN
          + " REAL NOT NULL, "
          + TABLE_WAN_TRAFFIC_TRAFFIC_OUT
          + " REAL NOT NULL, "
          + "UNIQUE("
          + TABLE_WAN_TRAFFIC_ROUTER_UUID
          + ", "
          + TABLE_WAN_TRAFFIC_TRAFFIC_DATE
          + ") ON CONFLICT REPLACE, "
          + "FOREIGN KEY ("
          + TABLE_WAN_TRAFFIC_ROUTER_UUID
          + ") REFERENCES "
          + TABLE_ROUTERS
          + "("
          + ROUTER_UUID
          + ") ON DELETE CASCADE ON UPDATE CASCADE "
          + ");";

  // Database creation sql statement
  private static final String TABLE_SPEED_TEST_RESULTS_CREATE =
      "CREATE TABLE IF NOT EXISTS "
          + TABLE_SPEED_TEST_RESULTS
          + " ("
          + TABLE_SPEED_TEST_RESULTS_COLUMN_ID
          + " INTEGER PRIMARY KEY AUTOINCREMENT, "
          + TABLE_SPEED_TEST_RESULTS_ROUTER_UUID
          + " TEXT NOT NULL, "
          + TABLE_SPEED_TEST_RESULTS_TEST_DATE
          + " TEXT NOT NULL, "
          + TABLE_SPEED_TEST_RESULTS_SERVER
          + " TEXT NOT NULL, "
          + TABLE_SPEED_TEST_RESULTS_WAN_PING
          + " REAL NOT NULL CHECK("
          + TABLE_SPEED_TEST_RESULTS_WAN_PING
          + ">=0) , "
          + TABLE_SPEED_TEST_RESULTS_WAN_DL
          + " REAL CHECK("
          + TABLE_SPEED_TEST_RESULTS_WAN_DL
          + ">=0) , "
          + TABLE_SPEED_TEST_RESULTS_WAN_UL
          + " REAL CHECK("
          + TABLE_SPEED_TEST_RESULTS_WAN_UL
          + ">=0) , "
          + TABLE_SPEED_TEST_RESULTS_CONNECTION_TYPE
          + " TEXT DEFAULT NULL , "
          + TABLE_SPEED_TEST_RESULTS_CONNECTION_DL
          + " REAL, "
          + TABLE_SPEED_TEST_RESULTS_CONNECTION_UL
          + " REAL, "
          + TABLE_SPEED_TEST_RESULTS_SERVER_COUNTRY_CODE
          + " TEXT, "
          + TABLE_SPEED_TEST_RESULTS_WAN_PING_MIN
          + " REAL, "
          + TABLE_SPEED_TEST_RESULTS_WAN_PING_MAX
          + " REAL, "
          + TABLE_SPEED_TEST_RESULTS_WAN_PING_STDDEV
          + " REAL, "
          + TABLE_SPEED_TEST_RESULTS_WAN_PING_PACKETS_LOSS
          + " REAL, "
          + TABLE_SPEED_TEST_RESULTS_WAN_DL_FILESIZE
          + " REAL, "
          + TABLE_SPEED_TEST_RESULTS_WAN_DL_DURATION
          + " REAL, "
          + TABLE_SPEED_TEST_RESULTS_WAN_UL_FILESIZE
          + " REAL, "
          + TABLE_SPEED_TEST_RESULTS_WAN_UL_DURATION
          + " REAL, "
          + TABLE_SPEED_TEST_RESULTS_CONNECTION_DL_FILESIZE
          + " REAL, "
          + TABLE_SPEED_TEST_RESULTS_CONNECTION_DL_DURATION
          + " REAL, "
          + TABLE_SPEED_TEST_RESULTS_CONNECTION_UL_FILESIZE
          + " REAL, "
          + TABLE_SPEED_TEST_RESULTS_CONNECTION_UL_DURATION
          + " REAL, "
          + "FOREIGN KEY ("
          + TABLE_SPEED_TEST_RESULTS_ROUTER_UUID
          + ") REFERENCES "
          + TABLE_ROUTERS
          + "("
          + ROUTER_UUID
          + ") ON DELETE CASCADE ON UPDATE CASCADE "
          + ");";

  // Database creation sql statement
  private static final String TABLE_ACTIONS_AUDIT_LOG_CREATE =
      "CREATE TABLE IF NOT EXISTS "
          + TABLE_ACTIONS_AUDIT_LOG
          + " ("
          + TABLE_ACTIONS_AUDIT_LOG_COLUMN_ID
          + " INTEGER PRIMARY KEY AUTOINCREMENT, "
          + TABLE_ACTIONS_AUDIT_LOG_UUID
          + " TEXT NOT NULL, "
          + TABLE_ACTIONS_AUDIT_LOG_ROUTER_UUID
          + " TEXT NOT NULL, "
          + TABLE_ACTIONS_AUDIT_LOG_ORIGIN
          + " TEXT NOT NULL, "
          + TABLE_ACTIONS_AUDIT_LOG_TRIGGER_DATE
          + " TEXT NOT NULL, "
          + TABLE_ACTIONS_AUDIT_LOG_ACTION_NAME
          + " TEXT NOT NULL, "
          + TABLE_ACTIONS_AUDIT_LOG_ACTION_DATA
          + " TEXT, "
          + TABLE_ACTIONS_AUDIT_LOG_ACTION_STATUS
          + " INTEGER NOT NULL, "
          + "FOREIGN KEY ("
          + TABLE_ACTIONS_AUDIT_LOG_ROUTER_UUID
          + ") REFERENCES "
          + TABLE_ROUTERS
          + "("
          + ROUTER_UUID
          + ") ON DELETE CASCADE ON UPDATE CASCADE "
          + ");";

  /*
   TODO In case of DB upgrades, don't forget to increment (by 2) this field,
   update DATABASE_TABLES_TO_CREATE (for newer installs), and
   add an entry into DATABASE_UPGRADES map
  */
  private static final int DATABASE_VERSION = 24;

  // TODO Don't forget to add new SQL here if a new table is to be created!
  private static final String[] DATABASE_TABLES_TO_CREATE =
      new String[] {
        DATABASE_CREATE,
        TABLE_WAN_TRAFFIC_CREATE,
        TABLE_SPEED_TEST_RESULTS_CREATE,
        TABLE_ACTIONS_AUDIT_LOG_CREATE
      };

  private static final Multimap<Integer, String> DATABASE_UPGRADES = ArrayListMultimap.create();

  public DDWRTCompanionSqliteOpenHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  // called whenever the app is freshly installed
  @Override
  public void onCreate(@NonNull SQLiteDatabase sqLiteDatabase) {
    // CREATE statements
    for (final String dbTableToCreate : DATABASE_TABLES_TO_CREATE) {
      Log.d(TAG, "onCreate: execSQL: " + dbTableToCreate);
      sqLiteDatabase.execSQL(dbTableToCreate);
    }
  }

  // called whenever the app is upgraded and launched and the database version is not the same
  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // this assumes monotonically increasing version numbers for upgrades.

    /*
    Leverage Incremental Updates, as described here:
    http://blog.adamsbros.org/2012/02/28/upgrade-android-sqlite-database/
    */
    int upgradeTo = oldVersion + 1;
    while (upgradeTo <= newVersion) {
      // Loop because we do not know which version users will be converting from or to
      final Collection<String> upgradeToSqlCollection = DATABASE_UPGRADES.get(upgradeTo);
      if (!(upgradeToSqlCollection == null || upgradeToSqlCollection.isEmpty())) {
        FirebaseCrashlytics.getInstance()
            .log("\t--> Performing DB Upgrade " + oldVersion + "=>" + upgradeTo);
        for (final String upgradeToSql : upgradeToSqlCollection) {
          if (!Strings.isNullOrEmpty(upgradeToSql)) {
            FirebaseCrashlytics.getInstance().log("\t\t>>> upgradeToSql: " + upgradeToSql);
            db.execSQL(upgradeToSql);
          }
        }
      }
      upgradeTo++;
    }
  }

  static {
    // V2: Add Router Firmware
    // Queries are executed in this order
    DATABASE_UPGRADES.put(
        2,
        String.format(
            "ALTER TABLE %s ADD COLUMN %s TEXT DEFAULT NULL; ", TABLE_ROUTERS, ROUTER_FIRMWARE));
    //        DATABASE_UPGRADES.put(2,
    //                String.format("UPDATE %s SET %s=\"%s\";",
    //                        TABLE_ROUTERS, ROUTER_FIRMWARE, Router.RouterFirmware.DDWRT));

    // V4: Add Router Firmware: Fix following 3.0.0 update
    //        DATABASE_UPGRADES.put(4, String.format("UPDATE %s SET %s=\"%s\";",
    //                TABLE_ROUTERS, ROUTER_FIRMWARE, Router.RouterFirmware.DDWRT));

    // V6: Add Router Firmware: Add DB for WAN Traffic Data
    DATABASE_UPGRADES.put(6, TABLE_WAN_TRAFFIC_CREATE);

    // V8: Speed Test Results
    DATABASE_UPGRADES.put(12, TABLE_SPEED_TEST_RESULTS_CREATE);

    //        //V10
    //        final String[] v10Cols = new String[] {
    //                TABLE_SPEED_TEST_RESULTS_WAN_PING_MIN,
    //                TABLE_SPEED_TEST_RESULTS_WAN_PING_MAX,
    //                TABLE_SPEED_TEST_RESULTS_WAN_PING_STDDEV,
    //                TABLE_SPEED_TEST_RESULTS_WAN_PING_PACKETS_LOSS,
    //                TABLE_SPEED_TEST_RESULTS_WAN_DL_FILESIZE,
    //                TABLE_SPEED_TEST_RESULTS_WAN_DL_DURATION,
    //                TABLE_SPEED_TEST_RESULTS_WAN_UL_FILESIZE,
    //                TABLE_SPEED_TEST_RESULTS_WAN_UL_DURATION,
    //                TABLE_SPEED_TEST_RESULTS_CONNECTION_DL_FILESIZE,
    //                TABLE_SPEED_TEST_RESULTS_CONNECTION_DL_DURATION,
    //                TABLE_SPEED_TEST_RESULTS_CONNECTION_UL_FILESIZE,
    //                TABLE_SPEED_TEST_RESULTS_CONNECTION_UL_DURATION
    //        };
    //        for (final String v10Col : v10Cols) {
    //            DATABASE_UPGRADES.put(10,
    //                    String.format("ALTER TABLE %s ADD COLUMN %s REAL DEFAULT NULL; ",
    //                            TABLE_SPEED_TEST_RESULTS, v10Col));
    //        }

    // Actions Audit Log
    DATABASE_UPGRADES.put(14, TABLE_ACTIONS_AUDIT_LOG_CREATE);

    // Support ordering in lists
    DATABASE_UPGRADES.put(
        16,
        String.format(
            "ALTER TABLE %s ADD COLUMN %s INTEGER NOT NULL DEFAULT 1000; ",
            TABLE_ROUTERS, ORDER_INDEX));
    // Migrate
    DATABASE_UPGRADES.put(
        18, String.format("UPDATE %s SET %s = (0 - %s); ", TABLE_ROUTERS, ORDER_INDEX, COLUMN_ID));

    // Support ordering in lists
    DATABASE_UPGRADES.put(
        20,
        String.format(
            "ALTER TABLE %s ADD COLUMN %s INTEGER NOT NULL DEFAULT 0 CHECK (%s IN (0,1)); ",
            TABLE_ROUTERS, ARCHIVED, ARCHIVED));

    // Set correct value for Demo Routers
    DATABASE_UPGRADES.put(
        22,
        String.format(
            "UPDATE %s SET %s=\"DEMO\" WHERE %s=\"%s\"; ",
            TABLE_ROUTERS, ROUTER_FIRMWARE, ROUTER_IP, BuildConfig.APPLICATION_ID));

    // #254: support for custom icons
    DATABASE_UPGRADES.put(
        24,
        String.format(
            "ALTER TABLE %s ADD COLUMN %s INTEGER NOT NULL DEFAULT %d; ",
            TABLE_ROUTERS, ROUTER_ICON_METHOD, Router.RouterIcon_Auto));
    DATABASE_UPGRADES.put(
        24,
        String.format(
            "ALTER TABLE %s ADD COLUMN %s TEXT DEFAULT NULL; ", TABLE_ROUTERS, ROUTER_CUSTOM_ICON));
  }
}
