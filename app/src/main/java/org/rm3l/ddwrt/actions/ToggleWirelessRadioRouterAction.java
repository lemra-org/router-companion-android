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

import org.rm3l.ddwrt.common.resources.audit.ActionLog;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.SSHUtils;

public class ToggleWirelessRadioRouterAction extends AbstractRouterAction<Void> {

    @NonNull
    private final Context mContext;

    private final boolean mEnable;

    public ToggleWirelessRadioRouterAction(Router router, @NonNull Context context,
                                           @Nullable RouterActionListener listener,
                                           @NonNull final SharedPreferences globalSharedPreferences,
                                           final boolean mEnable) {
        super(router, listener, RouterAction.TOGGLE_WL_RADIO, globalSharedPreferences);
        this.mContext = context;
        this.mEnable = mEnable;
    }

    @Override
    protected ActionLog getActionLog() {
        return super.getActionLog()
                .setActionData("- Status: " + mEnable);
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
            final int exitStatus = SSHUtils
                    .runCommands(mContext, globalSharedPreferences, router,
                            String.format("/usr/sbin/wl radio %s",
                                    mEnable ? "on" : "off"));

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
