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
import com.google.common.base.Strings;
import org.rm3l.router_companion.common.resources.audit.ActionLog;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.SSHUtils;

public class TogglePhysicalInterfaceStateRouterAction extends AbstractRouterAction<Void> {

  public enum PhysicalInterfaceState {
    UP,
    DOWN
  }

  public static final int PhysicalInterfaceState_UP = 1;

  public static final int PhysicalInterfaceState_DOWN = 2;

  @NonNull private final Context mContext;

  private final String mPhyIface;

  private final PhysicalInterfaceState mState;

  public TogglePhysicalInterfaceStateRouterAction(
      Router router,
      @NonNull Context context,
      @Nullable RouterActionListener listener,
      @NonNull final SharedPreferences globalSharedPreferences,
      @Nullable final String mPhyIface,
      @NonNull final PhysicalInterfaceState mState) {
    super(router, listener, RouterAction.TOGGLE_PHY_IFACE_STATE, globalSharedPreferences);
    this.mContext = context;
    this.mPhyIface = mPhyIface;
    this.mState = mState;
  }

  @NonNull
  @Override
  protected RouterActionResult<Void> doActionInBackground() {

    Exception exception = null;
    try {
      if (Strings.isNullOrEmpty(this.mPhyIface)) {
        throw new IllegalArgumentException("Unspecified physical interface");
      }

      final int exitStatus =
          SSHUtils.runCommands(
              mContext,
              globalSharedPreferences,
              router,
              String.format("/sbin/ifconfig %s %s", mPhyIface, mState.toString().toLowerCase()));

      if (exitStatus != 0) {
        throw new IllegalStateException();
      }
    } catch (Exception e) {
      e.printStackTrace();
      exception = e;
    }

    return new RouterActionResult<>(null, exception);
  }

  @Override
  protected ActionLog getActionLog() {
    return super.getActionLog()
        .setActionData(String.format("- Physical iface: %s\n" + "- State: %s", mPhyIface, mState));
  }

  @Nullable
  @Override
  protected Context getContext() {
    return mContext;
  }
}
