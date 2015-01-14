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
import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.SSHUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

public class SetNVRAMVariablesAction extends AbstractRouterAction<Void> {

    public static final String LOG_TAG = SetNVRAMVariablesAction.class.getSimpleName();
    @NotNull
    private final NVRAMInfo nvramInfo;

    public SetNVRAMVariablesAction(@NotNull NVRAMInfo nvramInfo,
                                      @Nullable RouterActionListener listener,
                                      @NotNull SharedPreferences globalSharedPreferences) {
        super(listener, RouterAction.SET_NVRAM_VARIABLES, globalSharedPreferences);
        this.nvramInfo = nvramInfo;
    }

    @NotNull
    @Override
    protected RouterActionResult doActionInBackground(@NotNull Router router) {
        Exception exception = null;
        try {
            if (nvramInfo.isEmpty()) {
                throw new IllegalArgumentException("No vars to set");
            }
            final Properties data = nvramInfo.getData();
            //noinspection ConstantConditions
            String[] cmd = new String[data.size() + 1];
            int i = 0;
            for (final Map.Entry<Object, Object> entry : data.entrySet()) {
                cmd[i++] = String.format("nvram set %s=\"%s\"", entry.getKey(), entry.getValue());
            }
            cmd[cmd.length-1] = "nvram commit";

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
}
