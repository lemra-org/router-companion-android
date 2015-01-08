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
import com.google.common.collect.ObjectArrays;
import com.google.common.io.Closeables;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import org.apache.commons.lang3.StringUtils;
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
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.EMPTY_STRING;

/**
 * SSH Utilities
 */
public final class SSHUtils {

    private static final String TAG = SSHUtils.class.getSimpleName();
    public static final String STRICT_HOST_KEY_CHECKING = "StrictHostKeyChecking";
    public static final String YES = "yes";
    public static final String NO = "no";
    public static final Joiner JOINER_CARRIAGE_RETURN = Joiner.on("\n");

    private SSHUtils() {
    }

    @Nullable
    private static Session getSSHSession(@NotNull final Router router) throws Exception {
        @Nullable final String privKey = \"fake-key\";
        @NotNull final JSch jsch = new JSch();

        final String passwordPlain = router.getPasswordPlain();
        final Session jschSession;

        final Router.SSHAuthenticationMethod sshAuthenticationMethod = router.getSshAuthenticationMethod();
        switch (sshAuthenticationMethod) {
            case PUBLIC_PRIVATE_KEY:
                    //noinspection ConstantConditions
                jsch.addIdentity(router.getUuid(),
                        !isNullOrEmpty(privKey) ? privKey.getBytes() : null,
                        null,
                        !isNullOrEmpty(passwordPlain) ? passwordPlain.getBytes() : null);
                jschSession = jsch.getSession(router.getUsernamePlain(), router.getRemoteIpAddress(), router.getRemotePort());
                break;
            case PASSWORD:
                jschSession = jsch.getSession(router.getUsernamePlain(), router.getRemoteIpAddress(), router.getRemotePort());
                jschSession.setPassword(passwordPlain);
                break;
            default:
                jschSession = jsch.getSession(router.getUsernamePlain(), router.getRemoteIpAddress(), router.getRemotePort());
                break;
        }

        @NotNull final Properties config = new Properties();
        config.put(STRICT_HOST_KEY_CHECKING, router.isStrictHostKeyChecking() ? YES : NO);
        jschSession.setConfig(config);

        return jschSession;
    }

    public static void checkConnection(@NotNull final Router router, final int connectTimeoutMillis) throws Exception {
        @Nullable Session jschSession = null;
        try {
            jschSession = getSSHSession(router);
            if (jschSession == null) {
                throw new IllegalStateException("Unable to retrieve session - please retry again later!");
            }
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
            jschSession = getSSHSession(router);
            if (jschSession == null) {
                throw new IllegalStateException("Unable to retrieve session - please retry again later!");
            }
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
            jschSession = getSSHSession(router);
            if (jschSession == null) {
                throw new IllegalStateException("Unable to retrieve session - please retry again later!");
            }
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

        boolean getMultiOutput = false;
        final List<String> grep = Lists.newArrayList();
        if (fieldsToFetch == null || fieldsToFetch.length == 0) {
            getMultiOutput = true;
        } else {
            for (final String fieldToFetch : fieldsToFetch) {
                if (isNullOrEmpty(fieldToFetch)) {
                    continue;
                }
                if (StringUtils.containsIgnoreCase(fieldToFetch, "sshd_rsa_host_key")) {
                    getMultiOutput = true;
                }
                grep.add("^" + fieldToFetch + "=.*");
            }
        }

        final String[] nvramShow = SSHUtils.getManualProperty(router,
                "nvram show" + (grep.isEmpty() ? "" : (" | grep -E \"" +
                        Joiner.on("|").join(grep) + "\"")));
        final String[] sshdRsaHostKey = \"fake-key\";
                SSHUtils.getManualProperty(router, "nvram get sshd_rsa_host_key") : null;
        final String[] sshdDsaHostKey = \"fake-key\";
                SSHUtils.getManualProperty(router, "nvram get sshd_dss_host_key") : null;

        //Fix multi-line output for sshd_rsa_host_key
        final String[] sshdRsaHostKeyFixed =
                new String[] {
                        "sshd_rsa_host_key=" + (sshdRsaHostKey != null ? JOINER_CARRIAGE_RETURN.join(sshdRsaHostKey) : EMPTY_STRING),
                        "sshd_dss_host_key=" + (sshdDsaHostKey != null ? JOINER_CARRIAGE_RETURN.join(sshdDsaHostKey) : EMPTY_STRING)};

        String[] outputArray = null;
        if (nvramShow != null) {
            outputArray = new String[nvramShow.length + sshdRsaHostKeyFixed.length];
            int k = 0;
            for (final String sshdHostKeyFixed : sshdRsaHostKeyFixed) {
                if (isNullOrEmpty(sshdHostKeyFixed)) {
                    continue;
                }
                outputArray[k++] = sshdHostKeyFixed;
            }
            for (int j = 0; j < nvramShow.length; j++) {
                boolean skip = false;
                final String nvramAtPositionJ = nvramShow[j];
                for (final String aSshdRsaHostKeyFixed : sshdRsaHostKeyFixed) {
                    if (StringUtils.contains(aSshdRsaHostKeyFixed, nvramAtPositionJ)) {
                        skip = true;
                        break;
                    }
                }
                if (skip) {
                    continue;
                }
                outputArray[k] = nvramAtPositionJ;
                k++;
            }
        }

        return NVRAMParser.parseNVRAMOutput(outputArray);
    }

}
