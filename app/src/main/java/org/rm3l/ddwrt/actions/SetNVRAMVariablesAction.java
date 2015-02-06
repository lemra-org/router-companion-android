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

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.SSHUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

public class SetNVRAMVariablesAction extends AbstractRouterAction<Void> {

    public static final String LOG_TAG = SetNVRAMVariablesAction.class.getSimpleName();
    @NonNull
    private final NVRAMInfo nvramInfo;

    private final boolean withReboot;

    public SetNVRAMVariablesAction(@NonNull NVRAMInfo nvramInfo,
                                   @Nullable RouterActionListener listener,
                                   @NonNull SharedPreferences globalSharedPreferences) {
        this(nvramInfo, false, listener, globalSharedPreferences);
    }

    public SetNVRAMVariablesAction(@NonNull NVRAMInfo nvramInfo, boolean withReboot,
                                   @Nullable RouterActionListener listener,
                                   @NonNull SharedPreferences globalSharedPreferences) {
        super(listener, RouterAction.SET_NVRAM_VARIABLES, globalSharedPreferences);
        this.nvramInfo = nvramInfo;
        this.withReboot = withReboot;
    }

    @NonNull
    @Override
    protected RouterActionResult doActionInBackground(@NonNull Router router) {
        Exception exception = null;
        try {
            if (nvramInfo.isEmpty()) {
                throw new IllegalArgumentException("No vars to set");
            }
            final Properties data = nvramInfo.getData();
            //noinspection ConstantConditions
            String[] cmd = new String[data.size() + 2];
            int i = 0;
            for (final Map.Entry<Object, Object> entry : data.entrySet()) {
                cmd[i++] = String.format("nvram set %s=\"%s\"", entry.getKey(), entry.getValue());
            }
            cmd[cmd.length - 2] = "nvram commit";
            cmd[cmd.length - 1] = withReboot ? "reboot" : "";

            Log.d(LOG_TAG, "cmd: [" + Arrays.toString(cmd) + "]");

            final int exitStatus = SSHUtils.runCommands(globalSharedPreferences, router, cmd);
            if (exitStatus != 0) {
                throw new IllegalStateException("Error when running command.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            exception = e;
        }

        return new RouterActionResult(null, exception);
    }

    @Nullable
    protected Object getDataToReturnOnSuccess() {
        return this.nvramInfo;
    }
}
