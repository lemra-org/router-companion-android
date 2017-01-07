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

import org.rm3l.router_companion.common.resources.audit.ActionLog;
import org.rm3l.router_companion.resources.Device;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.SSHUtils;

import static org.rm3l.router_companion.RouterCompanionAppConstants.DDWRTCOMPANION_WANACCESS_IPTABLES_CHAIN;

public class EnableWANAccessRouterAction extends AbstractRouterAction<Void> {

    @NonNull
    private final Device mDevice;

    @NonNull
    private final Context mContext;

    public EnableWANAccessRouterAction(Router router, @NonNull Context context, @Nullable RouterActionListener listener,
                                       @NonNull SharedPreferences globalSharedPreferences,
                                       @NonNull Device device) {
        super(router, listener, RouterAction.ENABLE_WAN_ACCESS, globalSharedPreferences);
        this.mContext = context;
        this.mDevice = device;
    }

    @Override
    protected ActionLog getActionLog() {
        return super.getActionLog()
                .setActionData(String.format("Device: %s (%s)",
                        mDevice.getAliasOrSystemName(), mDevice.getMacAddress()));
    }

    @Nullable
    @Override
    protected Context getContext() {
        return mContext;
    }

    @NonNull
    @Override
    protected RouterActionResult<Void> doActionInBackground() {
        Exception exception = null;
        try {
            final String macAddr = mDevice.getMacAddress();
            final int exitStatus = SSHUtils.runCommands(mContext, globalSharedPreferences, router,
                    "iptables -L " + DDWRTCOMPANION_WANACCESS_IPTABLES_CHAIN + " --line-numbers -n | grep -i \"" + macAddr + "\" | while read n other ; do " +
                            "iptables -D " + DDWRTCOMPANION_WANACCESS_IPTABLES_CHAIN + " $n; " +
                            "exit 0; " +
                            "done");
            if (exitStatus != 0) {
                throw new IllegalStateException();
            }
        } catch (Exception e) {
            e.printStackTrace();
            exception = e;
        }

        return new RouterActionResult<>(null, exception);
    }
}
