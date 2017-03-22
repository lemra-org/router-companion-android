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
package org.rm3l.router_companion.actions;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import org.rm3l.router_companion.resources.conn.Router;

public class ArpPingFromRouterAction extends ExecStreamableCommandRouterAction {

  private static final int MAX_ARPING_PACKETS_TO_SEND = 5;

  public ArpPingFromRouterAction(Router router, @NonNull Context context,
      @Nullable RouterStreamActionListener listener,
      @NonNull final SharedPreferences globalSharedPreferences, @NonNull final String hostToPing) {
    super(router, RouterAction.ARPING, context, listener, globalSharedPreferences, String.format(
        "for ifname in `/sbin/ifconfig | grep -i 'HWaddr' | awk '{print $1}'`; do "
            + "arping -c %s -I ${ifname} %s; done", MAX_ARPING_PACKETS_TO_SEND, hostToPing));
  }
}
