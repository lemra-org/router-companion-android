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

public class PingFromRouterAction extends ExecStreamableCommandRouterAction {

    public static final int MAX_PING_PACKETS_TO_SEND = 5;
    public static final String PING_CMD_TO_FORMAT = "/bin/ping -c %d %s 2>&1";

    public PingFromRouterAction(Router router, @NonNull Context context, @Nullable RouterStreamActionListener listener,
                                @NonNull final SharedPreferences globalSharedPreferences,
                                @NonNull final String hostToPing) {
        this(router, context, listener, globalSharedPreferences, hostToPing, MAX_PING_PACKETS_TO_SEND);
    }

    public PingFromRouterAction(Router router, @NonNull Context context, @Nullable RouterStreamActionListener listener,
                                @NonNull final SharedPreferences globalSharedPreferences,
                                @NonNull final String hostToPing,
                                @Nullable final Integer packetsCount) {
        super(router, RouterAction.PING, context, listener, globalSharedPreferences,
                String.format(PING_CMD_TO_FORMAT, (packetsCount == null || packetsCount <= 0) ?
                        MAX_PING_PACKETS_TO_SEND : packetsCount, hostToPing));
    }

}
