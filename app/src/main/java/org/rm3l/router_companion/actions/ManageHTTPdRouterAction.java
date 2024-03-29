/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014-2022  Armel Soro
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
package org.rm3l.router_companion.actions;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.SSHUtils;

public class ManageHTTPdRouterAction extends AbstractRouterAction<Void> {

  public static final int START = 1;

  public static final int STOP = 2;

  public static final int RESTART = 3;

  @NonNull private final Context mContext;

  private final int mHTTPdAction;

  public ManageHTTPdRouterAction(
      Router router,
      @NonNull Context context,
      @Nullable RouterActionListener listener,
      @NonNull final SharedPreferences globalSharedPreferences,
      final int httpdAction) {
    super(
        router,
        listener,
        httpdAction == START
            ? RouterAction.START_HTTPD
            : httpdAction == STOP
                ? RouterAction.STOP_HTTPD
                : httpdAction == RESTART
                    ? RouterAction.RESTART_HTTPD
                    : RouterAction.HTTPD_UNKNOWN_ACTION,
        globalSharedPreferences);
    this.mContext = context;
    this.mHTTPdAction = httpdAction;
  }

  @NonNull
  @Override
  protected RouterActionResult<Void> doActionInBackground() {

    Exception exception = null;
    try {

      final String action;
      switch (mHTTPdAction) {
        case STOP:
          action = "/sbin/stopservice httpd";
          break;
        case START:
          action = "/sbin/startservice httpd";
          break;
        case RESTART:
          action = "/sbin/stopservice httpd ; /sbin/startservice httpd";
          break;
        default:
          throw new IllegalArgumentException("Unknown action");
      }

      final int exitStatus =
          SSHUtils.runCommands(mContext, globalSharedPreferences, router, action);
      if (exitStatus != 0) {
        throw new IllegalStateException();
      }
    } catch (Exception e) {
      e.printStackTrace();
      exception = e;
    }

    return new RouterActionResult<>(null, exception);
  }

  @Nullable
  @Override
  protected Context getContext() {
    return mContext;
  }
}
