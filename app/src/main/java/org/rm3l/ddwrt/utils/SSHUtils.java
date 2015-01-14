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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
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

    static {
        JSch.setLogger(SSHLogger.getInstance());
    }


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

    public static final int CONNECT_TIMEOUT_MILLIS = 10000;

    private SSHUtils() {
    }

    public static void destroySession(@NotNull final Router router) {
        destroySession(router.getUuid());
    }

    public static void destroySession(@NotNull final String uuid) {
        try {
            synchronized (SSH_SESSIONS_LRU_CACHE) {
                SSH_SESSIONS_LRU_CACHE.remove(uuid);
            }
        } catch (final Exception e) {
            //No worries
            e.printStackTrace();
        }
    }

    @Nullable
    private static Session getSSHSession(@NotNull final SharedPreferences globalSharedPreferences,
                                                      @NotNull final Router router,
                                                      @Nullable final Integer connectTimeout) throws Exception {

        final String uuid = router.getUuid();

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
            Log.d(TAG, "=== SSH_SESSIONS_LRU_CACHE stats ===\n" +
                    "create_count=" + SSH_SESSIONS_LRU_CACHE.createCount() + ", " +
                    "eviction_count" + SSH_SESSIONS_LRU_CACHE.evictionCount() + ", " +
                    "hit_count" + SSH_SESSIONS_LRU_CACHE.hitCount() + ", " +
                    "miss_count" + SSH_SESSIONS_LRU_CACHE. missCount() + ", " +
                    "put_count" + SSH_SESSIONS_LRU_CACHE.putCount() + ", " +
                    "cache size" + SSH_SESSIONS_LRU_CACHE.size() + ", \n" +
                    "snapshot" + SSH_SESSIONS_LRU_CACHE.snapshot());
        }
    }

    public static  void checkConnection(@NotNull SharedPreferences globalSharedPreferences,
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

    public static  int runCommands(@NotNull SharedPreferences globalSharedPreferences,
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

    public static  int runCommands(@NotNull SharedPreferences globalSharedPreferences,
                                               @NotNull final Router router, @NotNull final String... cmdToExecute)
            throws Exception {
        return runCommands(globalSharedPreferences, router, Joiner.on(" && ").skipNulls(), cmdToExecute);
    }

    @Nullable
    public static  String[] getManualProperty(@NotNull final Router router, SharedPreferences globalPreferences, @NotNull final String... cmdToExecute) throws Exception {
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
    public static  NVRAMInfo getNVRamInfoFromRouter(@Nullable final Router router, SharedPreferences globalPreferences, @Nullable final String... fieldsToFetch) throws Exception {

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
                if (containsIgnoreCase(fieldToFetch, "sshd_rsa_host_key")
                        || containsIgnoreCase(fieldToFetch, "sshd_dss_host_key")
                        || containsIgnoreCase(fieldToFetch, "openvpncl_ca")
                        || containsIgnoreCase(fieldToFetch, "openvpncl_client")
                        || containsIgnoreCase(fieldToFetch, "openvpncl_key")
                        || containsIgnoreCase(fieldToFetch, "openvpn_ca")
                        || containsIgnoreCase(fieldToFetch, "openvpn_client")
                        || containsIgnoreCase(fieldToFetch, "openvpn_key")) {
                    getMultiOutput = true;
                }
                grep.add("^" + fieldToFetch + "=.*");
            }
        }

        final String[] nvramShow = SSHUtils.getManualProperty(router,
                globalPreferences, "nvram show" + (grep.isEmpty() ? "" : (" | grep -E \"" +
                        Joiner.on("|").join(grep) + "\"")));
        final String[] sshdRsaHostKey = \"fake-key\";
                SSHUtils.getManualProperty(router, globalPreferences, "nvram get sshd_rsa_host_key") : null;
        final String[] sshdDsaHostKey = \"fake-key\";
                SSHUtils.getManualProperty(router, globalPreferences, "nvram get sshd_dss_host_key") : null;
        final String[] openvpnclCaKey = \"fake-key\";
                SSHUtils.getManualProperty(router, globalPreferences, "nvram get openvpncl_ca") : null;
        final String[] openvpnclClientKey = \"fake-key\";
                SSHUtils.getManualProperty(router, globalPreferences, "nvram get openvpncl_client") : null;
        final String[] openvpnclKeyKey = \"fake-key\";
                SSHUtils.getManualProperty(router, globalPreferences, "nvram get openvpncl_key") : null;
        final String[] openvpnCaKey = \"fake-key\";
                SSHUtils.getManualProperty(router, globalPreferences, "nvram get openvpn_ca") : null;
        final String[] openvpnClientKey = \"fake-key\";
                SSHUtils.getManualProperty(router, globalPreferences, "nvram get openvpn_client") : null;
        final String[] openvpnKeyKey = \"fake-key\";
                SSHUtils.getManualProperty(router, globalPreferences, "nvram get openvpn_key") : null;

        //Fix multi-line output for some variables defined below
        final String[] varsToFix =
                new String[] {
                        "sshd_rsa_host_key=" + (sshdRsaHostKey != null ? JOINER_CARRIAGE_RETURN.join(sshdRsaHostKey) : EMPTY_STRING),
                        "sshd_dss_host_key=" + (sshdDsaHostKey != null ? JOINER_CARRIAGE_RETURN.join(sshdDsaHostKey) : EMPTY_STRING),
                        "openvpncl_ca=" + (openvpnclCaKey != null ? JOINER_CARRIAGE_RETURN.join(openvpnclCaKey) : EMPTY_STRING),
                        "openvpncl_client=" + (openvpnclClientKey != null ? JOINER_CARRIAGE_RETURN.join(openvpnclClientKey) : EMPTY_STRING),
                        "openvpncl_key=" + (openvpnclKeyKey != null ? JOINER_CARRIAGE_RETURN.join(openvpnclKeyKey) : EMPTY_STRING),
                        "openvpn_ca=" + (openvpnCaKey != null ? JOINER_CARRIAGE_RETURN.join(openvpnCaKey) : EMPTY_STRING),
                        "openvpn_client=" + (openvpnClientKey != null ? JOINER_CARRIAGE_RETURN.join(openvpnClientKey) : EMPTY_STRING),
                        "openvpn_key=" + (openvpnKeyKey != null ? JOINER_CARRIAGE_RETURN.join(openvpnKeyKey) : EMPTY_STRING)};

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

    private static class SSHLogger implements com.jcraft.jsch.Logger {
        private static final String LOG_TAG = TAG + "." + SSHLogger.class.getSimpleName();

        static final Map<Integer, String> name = Maps.newHashMapWithExpectedSize(5);
        static {
            name.put(DEBUG, "[DEBUG] ");
            name.put(INFO, "[INFO] ");
            name.put(WARN, "[WARN] ");
            name.put(ERROR, "[ERROR] ");
            name.put(FATAL, "[FATAL] ");
        }

        private static SSHLogger instance = null;

        private SSHLogger() {}

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

        public void log(int level, String message){
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
    }

}
