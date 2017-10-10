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
import org.rm3l.router_companion.resources.MACOUIVendor;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.status.wireless.WirelessClientsTile;

public class MACOUILookupAction extends AbstractRouterAction<Void> {

    @NonNull
    private final String mMacAddress;

    public MACOUILookupAction(Router router, @NonNull Context context,
            @Nullable RouterActionListener listener,
            @NonNull final SharedPreferences globalSharedPreferences, @NonNull final String macAddr) {
        super(router, listener, RouterAction.MAC_OUI_LOOKUP, globalSharedPreferences);
        this.mMacAddress = macAddr;
    }

    @NonNull
    @Override
    protected RouterActionResult<Void> doActionInBackground() {
        final RouterStreamActionListener routerStreamActionListener =
                (listener instanceof RouterStreamActionListener) ? (RouterStreamActionListener) listener
                        : null;
        Exception exception = null;
        try {
            if (routerStreamActionListener != null) {
                routerStreamActionListener.notifyRouterActionProgress(RouterAction.MAC_OUI_LOOKUP, router,
                        0, null);
            }
            final MACOUIVendor macouiVendor =
                    WirelessClientsTile.mMacOuiVendorLookupCache.get(mMacAddress);
            if (macouiVendor == null || macouiVendor.isNone()) {
                throw new IllegalArgumentException(
                        "Failed to fetch OUI info - check your input or connectivity!");
            }
            if (routerStreamActionListener != null) {
                routerStreamActionListener.notifyRouterActionProgress(RouterAction.MAC_OUI_LOOKUP, router,
                        100, macouiVendor.toCommandOutputString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            exception = e;
        }

        return new RouterActionResult<>(null, exception);
    }
}
