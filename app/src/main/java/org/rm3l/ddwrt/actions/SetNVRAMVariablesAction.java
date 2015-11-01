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
import android.util.Log;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import org.apache.commons.io.FileUtils;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEBUG_MODE;

public class SetNVRAMVariablesAction extends AbstractRouterAction<Void> {

    public static final String LOG_TAG = SetNVRAMVariablesAction.class.getSimpleName();
    @NonNull
    private final NVRAMInfo nvramInfo;

    private final boolean withReboot;

    @NonNull
    private final Context mContext;

    @NonNull
    private final ImmutableList<String> commandsToRunAfterCommit;

    public SetNVRAMVariablesAction(@NonNull Context context, @NonNull NVRAMInfo nvramInfo,
                                   boolean withReboot,
                                   @Nullable RouterActionListener listener,
                                   @NonNull SharedPreferences globalSharedPreferences,
                                   @Nullable String... commandsToRunAfterCommit) {
        super(listener, RouterAction.SET_NVRAM_VARIABLES, globalSharedPreferences);
        this.mContext = context;
        this.nvramInfo = nvramInfo;
        this.withReboot = withReboot;
        if (commandsToRunAfterCommit != null) {
            this.commandsToRunAfterCommit = ImmutableList
                    .copyOf(Arrays.asList(commandsToRunAfterCommit));
        } else {
            this.commandsToRunAfterCommit = ImmutableList.of();
        }
    }

    @NonNull
    @Override
    protected RouterActionResult<Void> doActionInBackground(@NonNull Router router) {
        return getRouterActionResult(mContext,
                globalSharedPreferences,
                router,
                nvramInfo,
                withReboot,
                this.commandsToRunAfterCommit);
    }

    public static RouterActionResult<Void> getRouterActionResult(@NonNull final Context mContext,
                                                                 @NonNull SharedPreferences globalSharedPreferences,
                                                                 @NonNull Router router,
                                                                 @NonNull final NVRAMInfo nvramInfo,
                                                                 final boolean withReboot,
                                                                 @Nullable final List<String> commandsToRunAfterCommit) {
        Exception exception = null;
        File outputFile = null;
        final String remotePath = "/tmp/." + SetNVRAMVariablesAction.class.getSimpleName() + "_" + UUID.randomUUID() + ".sh";

        try {
            if (nvramInfo.isEmpty()) {
                throw new IllegalArgumentException("No vars to set");
            }
            final Properties data = nvramInfo.getData();
            //noinspection ConstantConditions
            final ArrayList<String> cmdList = new ArrayList<>();
            int i = 0;
            for (final Map.Entry<Object, Object> entry : data.entrySet()) {
                cmdList.add(String.format("/usr/sbin/nvram set %s=\"%s\"", entry.getKey(), entry.getValue()));
            }
            cmdList.add("/usr/sbin/nvram commit");
            if (commandsToRunAfterCommit != null) {
                cmdList.addAll(commandsToRunAfterCommit);
            }
            if (withReboot) {
                cmdList.add("/sbin/reboot");
            }

            if (globalSharedPreferences.getBoolean(DEBUG_MODE, false)) {
                Log.d(LOG_TAG, "cmdList: " + cmdList);
            }

            // Seems there is a limit on the number of characters we can pass to the SSH server console
            // => copy all those in a temporary file, upload the file to the router and exec it
            outputFile = File.createTempFile(SetNVRAMVariablesAction.class.getSimpleName(), ".sh", mContext.getCacheDir());
            FileUtils.writeStringToFile(outputFile, Joiner.on(" && ").skipNulls().join(cmdList));

            //Now upload this file onto the remote router
            if (!SSHUtils.scpTo(mContext, router, globalSharedPreferences, outputFile.getAbsolutePath(), remotePath)) {
                throw new IllegalStateException("Failed to copy set of remote commands to the router");
            }

            final int exitStatus = SSHUtils.runCommands(mContext, globalSharedPreferences, router,
                    String.format("/bin/sh %s", remotePath));
            if (exitStatus != 0) {
                throw new IllegalStateException("Error when running command.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            exception = e;
        } finally {
            try {
                if (outputFile != null) {
                    //noinspection ResultOfMethodCallIgnored
                    outputFile.delete();
                }
            } catch (final Exception e) {
                Utils.reportException(mContext, e);
                //No worries
            } finally {
                try {
                    SSHUtils.runCommands(mContext, globalSharedPreferences, router,
                            "rm -rf " + remotePath);
                } catch (final Exception e) {
                    Utils.reportException(mContext, e);
                    //No worries
                }
            }
        }

        return new RouterActionResult<>(null, exception);
    }

    @Nullable
    protected Object getDataToReturnOnSuccess() {
        return this.nvramInfo;
    }
}
