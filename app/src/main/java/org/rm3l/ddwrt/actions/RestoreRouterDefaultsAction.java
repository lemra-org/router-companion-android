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

import com.google.common.base.Joiner;

import org.rm3l.ddwrt.exceptions.DDWRTCompanionException;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.AdUtils;
import org.rm3l.ddwrt.utils.SSHUtils;

import java.util.Date;

public class RestoreRouterDefaultsAction extends AbstractRouterAction<Void> {

    @NonNull
    private final Context mContext;

    public RestoreRouterDefaultsAction(@NonNull Context context, @Nullable RouterActionListener listener, @NonNull final SharedPreferences globalSharedPreferences) {
        super(listener, RouterAction.RESTORE_FACTORY_DEFAULTS, globalSharedPreferences);
        this.mContext = context;
    }

    @NonNull
    @Override
    protected RouterActionResult<Void> doActionInBackground(@NonNull Router router) {
        Exception exception = null;
        try {
            final int exitStatus = SSHUtils
                    .runCommands(mContext, globalSharedPreferences, router,
                            Joiner.on(" ; ").skipNulls(),
                            "erase nvram", "/sbin/reboot");
            if (exitStatus != 0) {
                throw new IllegalStateException();
            }
        } catch (Exception e) {
            e.printStackTrace();
            exception = e;
        }

        return new RouterActionResult<>(null, exception);
    }

    public static class AgreementToResetRouter extends DDWRTCompanionException {

        private final Date mClickDate;

        private final String mDeviceId;

        public AgreementToResetRouter(@NonNull Context context) {
            mClickDate = new Date();
            mDeviceId = AdUtils.getDeviceIdForAdMob(context);
        }

        public Date getClickDate() {
            return mClickDate;
        }

        public String getDeviceId() {
            return mDeviceId;
        }
    }
}
