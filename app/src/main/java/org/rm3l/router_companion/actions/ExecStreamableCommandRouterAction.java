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
package org.rm3l.router_companion.actions;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.common.base.Strings;
import org.rm3l.router_companion.common.resources.audit.ActionLog;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.SSHUtils;

public class ExecStreamableCommandRouterAction extends AbstractRouterAction<Void> {

  @NonNull private final String mCmd;

  @NonNull private final Context mContext;

  protected ExecStreamableCommandRouterAction(
      Router router,
      @NonNull final RouterAction routerAction,
      @NonNull Context context,
      @Nullable RouterStreamActionListener listener,
      @NonNull final SharedPreferences globalSharedPreferences,
      @NonNull final String cmd) {
    super(router, listener, routerAction, globalSharedPreferences);
    this.mContext = context;
    this.mCmd = cmd;
  }

  public ExecStreamableCommandRouterAction(
      Router router,
      @NonNull Context context,
      @Nullable RouterStreamActionListener listener,
      @NonNull final SharedPreferences globalSharedPreferences,
      @NonNull final String cmd) {
    this(router, RouterAction.CMD_SHELL, context, listener, globalSharedPreferences, cmd);
  }

  @NonNull
  @Override
  protected final RouterActionResult<Void> doActionInBackground() {
    Exception exception = null;
    try {
      if (isCanceled()) {
        throw new InterruptedException();
      }
      final int exitStatus =
          SSHUtils.execStreamableCommand(
              mContext,
              router,
              globalSharedPreferences,
              routerAction,
              (RouterStreamActionListener) listener,
              Strings.nullToEmpty(mCmd).replace("\n", ";"));

      if (exitStatus != 0) {
        throw new IllegalStateException("Command execution status: " + exitStatus);
      }
    } catch (Exception e) {
      e.printStackTrace();
      exception = e;
    }

    return new RouterActionResult<>(null, exception);
  }

  @Nullable
  @Override
  protected ActionLog getActionLog() {
    return new ActionLog().setActionName(routerAction.toString()).setActionData(this.mCmd);
  }

  @Nullable
  @Override
  protected Context getContext() {
    return mContext;
  }
}
