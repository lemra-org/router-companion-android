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

package org.rm3l.ddwrt.utils;

import android.util.Log;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * SSH Utilities
 */
public final class SSHUtils {

    private static final String TAG = SSHUtils.class.getSimpleName();

    private SSHUtils() {
    }

    public static void checkConnection(@NotNull final Router router, final int connectTimeoutMillis) throws Exception {
        @Nullable Session jschSession = null;
        try {
            @Nullable final String privKey = \"fake-key\";
            @NotNull final JSch jsch = new JSch();

            final Router.SSHAuthenticationMethod sshAuthenticationMethod = router.getSshAuthenticationMethod();
            final String passwordPlain = router.getPasswordPlain();
            switch (sshAuthenticationMethod) {
                case PUBLIC_PRIVATE_KEY:
                    if (privKey != null) {
                        //noinspection ConstantConditions
                        jsch.addIdentity(router.getUuid(), privKey.getBytes(), null,
                                isNullOrEmpty(passwordPlain) ? null : passwordPlain.getBytes());
                    }
                    break;
                default:
                    break;
            }

            jschSession = jsch.getSession(router.getUsernamePlain(), router.getRemoteIpAddress(), router.getRemotePort());
            jschSession.setPassword(passwordPlain);
            @NotNull final Properties config = new Properties();
            config.put("StrictHostKeyChecking", router.isStrictHostKeyChecking() ? "yes" : "no");
            jschSession.setConfig(config);
            jschSession.connect(connectTimeoutMillis);
        } finally {
            if (jschSession != null && jschSession.isConnected()) {
                jschSession.disconnect();
            }
        }

    }

    public static int runCommands(@NotNull final Router router, @NotNull final Joiner commandsJoiner, @NotNull final String... cmdToExecute)
            throws Exception {
        Log.d(TAG, "getManualProperty: <router=" + router + " / cmdToExecute=" + Arrays.toString(cmdToExecute) + ">");

        @Nullable Session jschSession = null;
        @Nullable ChannelExec channelExec = null;
        @Nullable InputStream in = null;
        @Nullable InputStream err = null;
        try {
            @Nullable final String privKey = \"fake-key\";
            @NotNull final JSch jsch = new JSch();
            if (privKey != null) {
                jsch.addIdentity(router.getUuid(), privKey.getBytes(), null, null);
            }
            jschSession = jsch.getSession(router.getUsernamePlain(), router.getRemoteIpAddress(), router.getRemotePort());
            jschSession.setPassword(router.getPasswordPlain());
            @NotNull final Properties config = new Properties();
            config.put("StrictHostKeyChecking", router.isStrictHostKeyChecking() ? "yes" : "no");
            jschSession.setConfig(config);
            jschSession.connect(30000);

            channelExec = (ChannelExec) jschSession.openChannel("exec");

            channelExec.setCommand(commandsJoiner.join(cmdToExecute));
            channelExec.setInputStream(null);
            in = channelExec.getInputStream();
            err = channelExec.getErrStream();
            channelExec.connect();

            //FIXME does not return the actual status
//            return channelExec.getExitStatus();
            return 0;

        } finally {
            Closeables.closeQuietly(in);
            Closeables.closeQuietly(err);
            if (channelExec != null) {
                channelExec.disconnect();
            }
            if (jschSession != null) {
                jschSession.disconnect();
            }
        }

    }

    public static int runCommands(@NotNull final Router router, @NotNull final String... cmdToExecute)
            throws Exception {
        return runCommands(router, Joiner.on(" && ").skipNulls(), cmdToExecute);
    }

    @Nullable
    public static String[] getManualProperty(@NotNull final Router router, @NotNull final String... cmdToExecute) throws Exception {
        Log.d(TAG, "getManualProperty: <router=" + router + " / cmdToExecute=" + Arrays.toString(cmdToExecute) + ">");

        @Nullable Session jschSession = null;
        @Nullable ChannelExec channelExec = null;
        @Nullable InputStream in = null;
        @Nullable InputStream err = null;
        try {
            @Nullable final String privKey = \"fake-key\";
            @NotNull final JSch jsch = new JSch();
            if (privKey != null) {
                jsch.addIdentity(router.getUuid(), privKey.getBytes(), null, null);
            }
            jschSession = jsch.getSession(router.getUsernamePlain(), router.getRemoteIpAddress(), router.getRemotePort());
            jschSession.setPassword(router.getPasswordPlain());
            @NotNull final Properties config = new Properties();
            config.put("StrictHostKeyChecking", router.isStrictHostKeyChecking() ? "yes" : "no");
            jschSession.setConfig(config);
            jschSession.connect(30000);

            channelExec = (ChannelExec) jschSession.openChannel("exec");

            channelExec.setCommand(Joiner.on(" && ").skipNulls().join(cmdToExecute));
            channelExec.setInputStream(null);
            in = channelExec.getInputStream();
            err = channelExec.getErrStream();
            channelExec.connect();

            return Utils.getLines(new BufferedReader(new InputStreamReader(in)));

        } finally {
            Closeables.closeQuietly(in);
            Closeables.closeQuietly(err);
            if (channelExec != null) {
                channelExec.disconnect();
            }
            if (jschSession != null) {
                jschSession.disconnect();
            }
        }

    }

    @Nullable
    public static NVRAMInfo getNVRamInfoFromRouter(@Nullable final Router router, @Nullable final String... fieldsToFetch) throws Exception {

        if (router == null) {
            return null;
        }

        final List<String> grep = Lists.newArrayList();
        if (fieldsToFetch != null) {
            for (final String fieldToFetch : fieldsToFetch) {
                if (isNullOrEmpty(fieldToFetch)) {
                    continue;
                }
                grep.add("^" + fieldToFetch + "=.*");
            }
        }

        return NVRAMParser.parseNVRAMOutput(SSHUtils.getManualProperty(router,
                "nvram show" + (grep.isEmpty() ? "" : (" | grep -E \"" +
                        Joiner.on("|").join(grep) + "\""))));
    }

}
