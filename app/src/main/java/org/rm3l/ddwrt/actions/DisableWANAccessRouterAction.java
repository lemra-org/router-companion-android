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
package org.rm3l.ddwrt.actions;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.resources.Device;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.SSHUtils;

import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DDWRTCOMPANION_WANACCESS_IPTABLES_CHAIN;

public class DisableWANAccessRouterAction extends AbstractRouterAction<Void> {

    @NonNull
    private final Device mDevice;

    @NonNull
    private final Context mContext;

    public DisableWANAccessRouterAction(@NonNull Context context, @Nullable RouterActionListener listener,
                                        @NonNull SharedPreferences globalSharedPreferences,
                                        @NonNull Device device) {
        super(listener, RouterAction.DISABLE_WAN_ACCESS, globalSharedPreferences);
        this.mContext = context;
        this.mDevice = device;
    }

    @NonNull
    @Override
    protected RouterActionResult<Void> doActionInBackground(@NonNull Router router) {
        Exception exception = null;
        try {
            final String macAddr = mDevice.getMacAddress();
            final int exitStatus = SSHUtils.runCommands(mContext, globalSharedPreferences, router,
                    "iptables -L " + DDWRTCOMPANION_WANACCESS_IPTABLES_CHAIN + " | grep -i \"" + macAddr + "\" > /dev/null 2>&1; " +
                            "if [ $? -ne 0 ]; then " +
                            "iptables -A " + DDWRTCOMPANION_WANACCESS_IPTABLES_CHAIN + " -m mac --mac-source \"" + macAddr + "\" -j DROP; " +
                            "fi");
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
