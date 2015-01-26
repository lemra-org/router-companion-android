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

import android.content.SharedPreferences;
import android.util.Log;
import android.util.LruCache;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_CA;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_CLIENT;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_KEY;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_ROUTE;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_STATIC;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPNCL_TLSAUTH;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPN_CA;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPN_CLIENT;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPN_CRL;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPN_CRT;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPN_KEY;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPN_STATIC;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.OPENVPN_TLSAUTH;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.SSHD_DSS_HOST_KEY;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.SSHD_RSA_HOST_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.EMPTY_STRING;

/**
 * SSH Utilities
 */
public final class SSHUtils {

    public static final String STRICT_HOST_KEY_CHECKING = "StrictHostKeyChecking";
    public static final String YES = "yes";
    public static final String NO = "no";
    public static final Joiner JOINER_CARRIAGE_RETURN = Joiner.on("\n");
    public static final int CONNECT_TIMEOUT_MILLIS = 10000;
    private static final List<String> MULTI_OUTPUT_NVRAM_VARS =
            Arrays.asList(SSHD_RSA_HOST_KEY,
                    SSHD_DSS_HOST_KEY,
                    OPENVPNCL_CA, OPENVPNCL_CLIENT, OPENVPNCL_KEY, OPENVPNCL_TLSAUTH, OPENVPNCL_STATIC, OPENVPNCL_ROUTE,
                    OPENVPN_CA, OPENVPN_CLIENT, OPENVPN_KEY, OPENVPN_TLSAUTH, OPENVPN_CRT, OPENVPN_CRL, OPENVPN_STATIC);

    static {
        JSch.setLogger(SSHLogger.getInstance());
    }

    private static final String TAG = SSHUtils.class.getSimpleName();
    private static final LruCache<String, Session> SSH_SESSIONS_LRU_CACHE = new LruCache<String, Session>(5) {
        @Override
        protected void entryRemoved(boolean evicted, String key, Session oldValue, Session newValue) {
            Log.d(TAG, "entryRemoved @" + key + " / evicted? " + evicted);
            super.entryRemoved(evicted, key, oldValue, newValue);
            try {
                if (oldValue != null && oldValue.isConnected()) {
                    Log.d(TAG, "Disconnect SSH session for router " + key);
                    oldValue.disconnect();
                }
            } catch (final Exception e) {
                //No worries
                e.printStackTrace();
            }
        }
    };
    private static final AtomicReference<Long> cacheStatsElapsedTimeForDebugging =
            new AtomicReference<>(null);

    private SSHUtils() {
    }

    public static void destroySession(@Nullable final Router router) {
        if (router != null) {
            destroySession(router.getUuid());
        }
    }

    public static void destroySession(@Nullable final String uuid) {
        if (uuid != null) {
            try {
                synchronized (SSH_SESSIONS_LRU_CACHE) {
                    SSH_SESSIONS_LRU_CACHE.remove(uuid);
                }
            } catch (final Exception e) {
                //No worries
                e.printStackTrace();
            }
        }
    }

    @Nullable
    private static Session getSSHSession(@NotNull final SharedPreferences globalSharedPreferences,
                                         @Nullable final Router router,
                                         @Nullable final Integer connectTimeout) throws Exception {

        final String uuid;
        if (router == null || (uuid = router.getUuid()) == null) {
            return null;
        }

        try {
            synchronized (SSH_SESSIONS_LRU_CACHE) {
                final Session sessionCached = SSH_SESSIONS_LRU_CACHE.get(uuid);

                if (sessionCached != null) {
                    if (!sessionCached.isConnected()) {
                        sessionCached.connect(connectTimeout != null ? connectTimeout : CONNECT_TIMEOUT_MILLIS);
                    }
                    return sessionCached;
                }

                @Nullable final String privKey = \"fake-key\";
                @NotNull final JSch jsch = new JSch();

                final String passwordPlain = router.getPasswordPlain();
                final Session jschSession;

                final Router.SSHAuthenticationMethod sshAuthenticationMethod = router.getSshAuthenticationMethod();
                switch (sshAuthenticationMethod) {
                    case PUBLIC_PRIVATE_KEY:
                        //noinspection ConstantConditions
                        jsch.addIdentity(uuid,
                                !isNullOrEmpty(privKey) ? privKey.getBytes() : null,
                                null,
                                !isNullOrEmpty(passwordPlain) ? passwordPlain.getBytes() : null);
                        jschSession = jsch.getSession(router.getUsernamePlain(), router.getRemoteIpAddress(), router.getRemotePort());
                        break;
                    case PASSWORD:
                        jschSession = jsch.getSession(router.getUsernamePlain(), router.getRemoteIpAddress(), router.getRemotePort());
                        jschSession.setPassword(passwordPlain);
                        break;
//            case NONE:
//                jschSession = jsch.getSession(router.getUsernamePlain(), router.getRemoteIpAddress(), router.getRemotePort());
//                jschSession.setPassword(DDWRTCompanionConstants.EMPTY_STRING);
//                break;
                    default:
                        jschSession = jsch.getSession(router.getUsernamePlain(), router.getRemoteIpAddress(), router.getRemotePort());
                        break;
                }

                final boolean strictHostKeyChecking = router.isStrictHostKeyChecking();
                //Set known hosts file to preferences file
                //        jsch.setKnownHosts();

                @NotNull final Properties config = new Properties();
                config.put(STRICT_HOST_KEY_CHECKING, strictHostKeyChecking ? YES : NO);
                jschSession.setConfig(config);

                jschSession.connect(connectTimeout != null ? connectTimeout : CONNECT_TIMEOUT_MILLIS);

                SSH_SESSIONS_LRU_CACHE.put(uuid, jschSession);
                return SSH_SESSIONS_LRU_CACHE.get(uuid);
            }
        } catch (final JSchException jsche) {
            //Disconnect session, so a new one can be reconstructed next time
            destroySession(uuid);
            throw jsche;
        } finally {
            //Display stats every 1h
            final Long lastUpdate = cacheStatsElapsedTimeForDebugging.get();
            final long currentTimeMillis = System.currentTimeMillis();
            if (lastUpdate == null || (currentTimeMillis - lastUpdate) >= (1 * 60 * 60 * 1000l)) {
                Log.d(TAG, "=== SSH_SESSIONS_LRU_CACHE stats ===\n" +
                        "create_count=" + SSH_SESSIONS_LRU_CACHE.createCount() + ", " +
                        "eviction_count=" + SSH_SESSIONS_LRU_CACHE.evictionCount() + ", " +
                        "hit_count=" + SSH_SESSIONS_LRU_CACHE.hitCount() + ", " +
                        "miss_count=" + SSH_SESSIONS_LRU_CACHE.missCount() + ", " +
                        "put_count=" + SSH_SESSIONS_LRU_CACHE.putCount() + ", " +
                        "cache size=" + SSH_SESSIONS_LRU_CACHE.size() + ", \n" +
                        "snapshot=" + SSH_SESSIONS_LRU_CACHE.snapshot());
                cacheStatsElapsedTimeForDebugging.set(currentTimeMillis);
            }
        }
    }

    public static void checkConnection(@NotNull SharedPreferences globalSharedPreferences,
                                       @NotNull final Router router, final int connectTimeoutMillis) throws Exception {
        // This is used that for a temporary connection check
        // at this point, we can just make a copy of the existing router and assign it a random UUID
        final Router routerCopy = new Router(router);
        final String tempUuid = UUID.randomUUID().toString();
        try {
            routerCopy.setUuid(tempUuid);

            synchronized (SSH_SESSIONS_LRU_CACHE) {
                @Nullable Session jschSession = getSSHSession(globalSharedPreferences, routerCopy, connectTimeoutMillis);
                if (jschSession == null) {
                    throw new IllegalStateException("Unable to retrieve session - please retry again later!");
                }
                if (!jschSession.isConnected()) {
                    jschSession.connect(connectTimeoutMillis);
                }
            }
        } finally {
            //Now drop from LRU Cache
            destroySession(tempUuid);
        }
    }

    public static int runCommands(@NotNull SharedPreferences globalSharedPreferences,
                                  @NotNull final Router router, @NotNull final Joiner commandsJoiner, @NotNull final String... cmdToExecute)
            throws Exception {
        Log.d(TAG, "runCommands: <router=" + router + " / cmdToExecute=" + Arrays.toString(cmdToExecute) + ">");

        @Nullable ChannelExec channelExec = null;
        @Nullable InputStream in = null;
        @Nullable InputStream err = null;
        try {
            synchronized (SSH_SESSIONS_LRU_CACHE) {
                @Nullable Session jschSession = getSSHSession(globalSharedPreferences, router, CONNECT_TIMEOUT_MILLIS);
                if (jschSession == null) {
                    throw new IllegalStateException("Unable to retrieve session - please retry again later!");
                }
                if (!jschSession.isConnected()) {
                    jschSession.connect(CONNECT_TIMEOUT_MILLIS);
                }

                channelExec = (ChannelExec) jschSession.openChannel("exec");

                channelExec.setCommand(commandsJoiner.join(cmdToExecute));
                channelExec.setInputStream(null);
                in = channelExec.getInputStream();
                err = channelExec.getErrStream();
                channelExec.connect();

                final String[] output = Utils.getLines(new BufferedReader(new InputStreamReader(in)));
                Log.d(TAG, "output: " + Arrays.toString(output));

                //FIXME does not return the actual status
//            return channelExec.getExitStatus();
                return 0;
            }

        } finally {
            Closeables.closeQuietly(in);
            Closeables.closeQuietly(err);
            if (channelExec != null) {
                channelExec.disconnect();
            }
        }

    }

    public static int runCommands(@NotNull SharedPreferences globalSharedPreferences,
                                  @NotNull final Router router, @NotNull final String... cmdToExecute)
            throws Exception {
        return runCommands(globalSharedPreferences, router, Joiner.on(" && ").skipNulls(), cmdToExecute);
    }

    public static String[] execCommandOverTelnet(@NotNull final Router router, SharedPreferences globalPreferences,
                                                 final int telnetPort, @NotNull final String... cmdToExecute) throws Exception {
        //( echo "log 15"; sleep 1 ) | telnet localhost 16
        final List<String> cmdToRun = Lists.newArrayList();
        if (cmdToExecute.length > 0) {
            for (String cmdToExec : cmdToExecute) {
                cmdToRun.add(String.format("echo \"%s\"", cmdToExec));
            }
        }

        return getManualProperty(router, globalPreferences,
                String.format("( %s ; sleep 1 ) | telnet localhost %d", Joiner.on(";").skipNulls().join(cmdToRun), telnetPort));
    }

    @Nullable
    public static String[] getManualProperty(@NotNull final Router router, SharedPreferences globalPreferences, @NotNull final String... cmdToExecute) throws Exception {
        Log.d(TAG, "getManualProperty: <router=" + router + " / cmdToExecute=" + Arrays.toString(cmdToExecute) + ">");

        @Nullable ChannelExec channelExec = null;
        @Nullable InputStream in = null;
        @Nullable InputStream err = null;
        try {
            synchronized (SSH_SESSIONS_LRU_CACHE) {
                @Nullable Session jschSession = getSSHSession(globalPreferences, router, CONNECT_TIMEOUT_MILLIS);
                if (jschSession == null) {
                    throw new IllegalStateException("Unable to retrieve session - please retry again later!");
                }
                if (!jschSession.isConnected()) {
                    jschSession.connect(CONNECT_TIMEOUT_MILLIS);
                }

                channelExec = (ChannelExec) jschSession.openChannel("exec");

                channelExec.setCommand(Joiner.on(" && ").skipNulls().join(cmdToExecute));
                channelExec.setInputStream(null);
                in = channelExec.getInputStream();
                err = channelExec.getErrStream();
                channelExec.connect();

                return Utils.getLines(new BufferedReader(new InputStreamReader(in)));
            }

        } finally {
            Closeables.closeQuietly(in);
            Closeables.closeQuietly(err);
            if (channelExec != null) {
                channelExec.disconnect();
            }
        }

    }

    @Nullable
    public static NVRAMInfo getNVRamInfoFromRouter(@Nullable final Router router, SharedPreferences globalPreferences, @Nullable final String... fieldsToFetch) throws Exception {

        if (router == null) {
            throw new IllegalArgumentException("No connection parameters");
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
                for (final String multiOutputNvramVar : MULTI_OUTPUT_NVRAM_VARS) {
                    if (containsIgnoreCase(fieldToFetch, multiOutputNvramVar)) {
                        getMultiOutput = true;
                        break;
                    }
                }
                grep.add("^" + fieldToFetch + "=.*");
            }
        }

        final String[] nvramShow = SSHUtils.getManualProperty(router,
                globalPreferences, "nvram show" + (grep.isEmpty() ? "" : (" | grep -E \"" +
                        Joiner.on("|").join(grep) + "\"")));

        final String[] varsToFix = new String[MULTI_OUTPUT_NVRAM_VARS.size()];
        int i = 0;
        for (final String multiOutputNvramVar : MULTI_OUTPUT_NVRAM_VARS) {
            final String[] completeValue = getMultiOutput ?
                    SSHUtils.getManualProperty(router, globalPreferences, "nvram get " + multiOutputNvramVar) : null;
            varsToFix[i++] = (multiOutputNvramVar + "=" + (completeValue != null ? JOINER_CARRIAGE_RETURN.join(completeValue) : EMPTY_STRING));
        }

        String[] outputArray = null;
        if (nvramShow != null) {
            outputArray = new String[nvramShow.length + varsToFix.length];
            int k = 0;
            for (final String varToFix : varsToFix) {
                if (isNullOrEmpty(varToFix)) {
                    continue;
                }
                outputArray[k++] = varToFix;
            }
            for (int j = 0; j < nvramShow.length; j++) {
                boolean skip = false;
                final String nvramAtPositionJ = nvramShow[j];
                for (final String varToFix : varsToFix) {
                    if (StringUtils.contains(varToFix, nvramAtPositionJ)) {
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


    public static boolean scpTo(@Nullable final Router router, SharedPreferences globalPreferences,
                                @NotNull final String fromLocalPath, @NotNull final String toRemotePath)
            throws Exception {
        Log.d(TAG, "scpTo: <router=" + router + " / fromLocalPath=" + fromLocalPath +
                ", toRemotePath=" + toRemotePath + ">");
        if (router == null) {
            throw new IllegalArgumentException("No connection parameters");
        }

        FileInputStream fis = null;
        ChannelExec channelExec = null;
        OutputStream out = null;
        InputStream in = null;

        String command =
                "scp -q -o StrictHostKeyChecking=no -t " + toRemotePath;
        Log.d(TAG, "scpTo: command=[" + command + "]");

        try {
            synchronized (SSH_SESSIONS_LRU_CACHE) {
                @Nullable Session jschSession = getSSHSession(globalPreferences, router, CONNECT_TIMEOUT_MILLIS);
                if (jschSession == null) {
                    throw new IllegalStateException("Unable to retrieve session - please retry again later!");
                }
                if (!jschSession.isConnected()) {
                    jschSession.connect(CONNECT_TIMEOUT_MILLIS);
                }

                channelExec = (ChannelExec) jschSession.openChannel("exec");
                channelExec.setCommand(command);

                /*
                 * Forcing allocation of a pseudo-TTY prevents SCP from working
                 * correctly
                 */
                channelExec.setPty(false);

                // get I/O streams for remote scp
                out = channelExec.getOutputStream();
                in = channelExec.getInputStream();

                channelExec.connect();

                final int checkAck = checkAck(in);
                if (checkAck != 0) {
                    return closeChannel(channelExec, out);
                }

                final File _lfile = new File(fromLocalPath);
                // send "C0644 filesize filename", where filename should not include
                // '/'
                final long filesize = _lfile.length();
                command = "C0644 " + filesize + " ";
                if (fromLocalPath.lastIndexOf('/') > 0) {
                    command += fromLocalPath
                            .substring(fromLocalPath.lastIndexOf('/') + 1);
                } else {
                    command += fromLocalPath;
                }
                command += "\n";

                out.write(command.getBytes(Charset.forName("UTF-8")));
                out.flush();
                if (checkAck(in) != 0) {
                    return closeChannel(channelExec, out);
                }

                // send a content of lfile
                fis = new FileInputStream(fromLocalPath);
                final byte[] buf = new byte[1024];
                while (true) {
                    final int len = fis.read(buf, 0, buf.length);
                    if (len <= 0) {
                        break;
                    }
                    out.write(buf, 0, len); // out.flush();
                }
                fis.close();
                fis = null;
                // send '\0'
                buf[0] = 0;
                out.write(buf, 0, 1);
                out.flush();

                // checkAck(in);
                if (checkAck(in) != 0) {
                    return closeChannel(channelExec, out);
                }
                out.close();
            }

        } catch (final IOException ioe) {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (final IOException ee) {
                ee.printStackTrace();
                //No worries
            }
            return false;

        } finally {
            Closeables.closeQuietly(in);
//            Closeables.closeQuietly(err);
            if (channelExec != null && channelExec.isConnected()) {
                channelExec.disconnect();
            }
        }

        return true;

    }

    public static boolean scpFrom(@Nullable final Router router, SharedPreferences globalPreferences,
                                  @NotNull final String fromRemotePath, @NotNull final String toLocalPath)
            throws Exception {
        Log.d(TAG, "scpFrom: <router=" + router + " / fromRemotePath=" + fromRemotePath +
                ", toLocalPath=" + toLocalPath + ">");
        if (router == null) {
            throw new IllegalArgumentException("No connection parameters");
        }

        FileInputStream fis = null;
        FileOutputStream fos = null;
        ChannelExec channelExec = null;
        OutputStream out = null;
        InputStream in = null;

        String command =
                "scp -q -o StrictHostKeyChecking=no -f " + fromRemotePath;
        Log.d(TAG, "scpTo: command=[" + command + "]");

        try {
            synchronized (SSH_SESSIONS_LRU_CACHE) {
                @Nullable Session jschSession = getSSHSession(globalPreferences, router, CONNECT_TIMEOUT_MILLIS);
                if (jschSession == null) {
                    throw new IllegalStateException("Unable to retrieve session - please retry again later!");
                }
                if (!jschSession.isConnected()) {
                    jschSession.connect(CONNECT_TIMEOUT_MILLIS);
                }

                channelExec = (ChannelExec) jschSession.openChannel("exec");
                channelExec.setCommand(command);

                /*
                 * Forcing allocation of a pseudo-TTY prevents SCP from working
                 * correctly
                 */
                channelExec.setPty(false);

                // get I/O streams for remote scp
                out = channelExec.getOutputStream();
                in = channelExec.getInputStream();

                channelExec.connect();

                byte[] buf = new byte[1024];

                // send '\0'
                buf[0] = 0;
                out.write(buf, 0, 1);
                out.flush();

                while (true) {
                    int c = checkAck(in);
                    if (c != 'C') {
                        break;
                    }

                    // read '0644 '
                    in.read(buf, 0, 5);

                    long filesize = 0L;
                    while (true) {
                        if (in.read(buf, 0, 1) < 0) {
                            // error
                            break;
                        }
                        if (buf[0] == ' ') {
                            break;
                        }
                        filesize = filesize * 10L + (long) (buf[0] - '0');
                    }

                    String file = null;
                    for (int i = 0; ; i++) {
                        in.read(buf, i, 1);
                        if (buf[i] == (byte) 0x0a) {
                            file = new String(buf, 0, i, Charset.defaultCharset());
                            break;
                        }
                    }

                    //System.out.println("filesize="+filesize+", file="+file);

                    // send '\0'
                    buf[0] = 0;
                    out.write(buf, 0, 1);
                    out.flush();

                    // read a content of lfile
                    fos = new FileOutputStream(toLocalPath);
                    int foo;
                    while (true) {
                        if (buf.length < filesize) {
                            foo = buf.length;
                        } else {
                            foo = (int) filesize;
                        }
                        foo = in.read(buf, 0, foo);
                        if (foo < 0) {
                            // error
                            break;
                        }
                        fos.write(buf, 0, foo);
                        filesize -= foo;
                        if (filesize == 0L) {
                            break;
                        }
                    }
                    fos.close();
                    fos = null;

                    if (checkAck(in) != 0) {
                        return false;
                    }

                    // send '\0'
                    buf[0] = 0;
                    out.write(buf, 0, 1);
                    out.flush();
                }

                out.close();
                in.close();
                channelExec.disconnect();

            }
        } catch (final IOException ioe) {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (final IOException ee) {
                ee.printStackTrace();
                //no worries
            }
            return false;
        } finally {
            Closeables.closeQuietly(in);
//            Closeables.closeQuietly(err);
            if (channelExec != null && channelExec.isConnected()) {
                channelExec.disconnect();
            }
        }

        return true;
    }

    /**
     * @param channel
     * @param out
     * @return
     * @throws java.io.IOException
     */
    private static boolean closeChannel(@NotNull final Channel channel, @Nullable final OutputStream out)
            throws IOException {

        if (out != null) {
            out.close();
        }
        channel.disconnect();
        return false;
    }

    private static int checkAck(@NotNull final InputStream in) throws IOException {
        final int b = in.read();
        // b may be 0 for success,
        // 1 for error,
        // 2 for fatal error,
        // -1
        if (b == 0) {
            return b;
        }
        if (b == -1) {
            return b;
        }

        if (b == 1 || b == 2) {
            final StringBuilder sb = new StringBuilder();
            int c;
            do {
                c = in.read();
                sb.append((char) c);
            } while (c != '\n');
            if (b == 1) { // error
                Log.e(TAG, sb.toString());
            }
            if (b == 2) { // fatal error
                Log.wtf(TAG, sb.toString());
            }
        }
        return b;
    }

    private static class SSHLogger implements com.jcraft.jsch.Logger {
        static final Map<Integer, String> name = Maps.newHashMapWithExpectedSize(5);
        private static SSHLogger instance = null;

        private SSHLogger() {
        }

        public static SSHLogger getInstance() {
            if (instance == null) {
                instance = new SSHLogger();
            }
            return instance;
        }

        public boolean isEnabled(int level) {
            switch (level) {
                case DEBUG:
                    return BuildConfig.DEBUG;
                case INFO:
                case WARN:
                case ERROR:
                case FATAL:
                    return true;
            }
            return false;
        }

        public void log(int level, String message) {
            final String levelTag = name.get(level);
            final String messageToDisplay = String.format("%s%s\n",
                    isNullOrEmpty(levelTag) ? "???" : levelTag, message);
            switch (level) {
                case INFO:
                    Log.i(LOG_TAG, messageToDisplay);
                    break;
                case WARN:
                    Log.w(LOG_TAG, messageToDisplay);
                    break;
                case ERROR:
                    Log.e(LOG_TAG, messageToDisplay);
                    break;
                case FATAL:
                    Log.wtf(LOG_TAG, messageToDisplay);
                    break;
                case DEBUG:
                default:
                    Log.d(LOG_TAG, messageToDisplay);
                    break;

            }
        }

        private static final String LOG_TAG = TAG + "." + SSHLogger.class.getSimpleName();

        static {
            name.put(DEBUG, "[DEBUG] ");
            name.put(INFO, "[INFO] ");
            name.put(WARN, "[WARN] ");
            name.put(ERROR, "[ERROR] ");
            name.put(FATAL, "[FATAL] ");
        }


    }

}
