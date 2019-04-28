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

import static org.rm3l.router_companion.actions.RouterAction.RESET_COUNTERS;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.status.wireless.WirelessClientsTile;
import org.rm3l.router_companion.utils.SSHUtils;

public class ResetBandwidthMonitoringCountersRouterAction extends AbstractRouterAction<Void> {

    @NonNull
    private final Context mContext;

    public ResetBandwidthMonitoringCountersRouterAction(Router router, @NonNull Context context,
            @Nullable RouterActionListener listener, @NonNull SharedPreferences globalSharedPreferences) {
        super(router, listener, RESET_COUNTERS, globalSharedPreferences);
        this.mContext = context;
    }

    @NonNull
    @Override
    protected RouterActionResult<Void> doActionInBackground() {
        Exception exception = null;
        try {
            final String[] exitStatus =
                    SSHUtils.getManualProperty(mContext, router, globalSharedPreferences,
                            "rm -f " + WirelessClientsTile.USAGE_DB + "; echo $?");

            if (exitStatus == null || exitStatus.length == 0) {
                throw new IllegalStateException("Unable to get the Reset Command Status.");
            }
            if (!"0".equals(exitStatus[0])) {
                throw new IllegalStateException("Command execution status: " + exitStatus[0]);
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
