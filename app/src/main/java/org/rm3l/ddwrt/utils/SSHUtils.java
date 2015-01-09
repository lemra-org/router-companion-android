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
import com.google.common.io.Closeables;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

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

    private static final LruCache<String, Session> sshSessionsCache = new LruCache<String, Session>(2) {
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
    public static final int CONNECT_TIMEOUT_MILLIS = 30000;

    private SSHUtils() {
    }

    public static synchronized void destroySession(@NotNull final Router router) {
        try {
            sshSessionsCache.remove(router.getUuid());
        } catch (final Exception e) {
            //No worries
            e.printStackTrace();
        }
    }

    public static synchronized void destroySession(@NotNull final String uuid) {
        try {
            sshSessionsCache.remove(uuid);
        } catch (final Exception e) {
            //No worries
            e.printStackTrace();
        }
    }

    @Nullable
    private static synchronized Session getSSHSession(@NotNull final SharedPreferences globalSharedPreferences,
                                                      @NotNull final Router router,
                                                      @Nullable final Integer connectTimeout) throws Exception {

        final String uuid = router.getUuid();

        final Session sessionCached = sshSessionsCache.get(uuid);

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
        jsch.setHostKeyRepository(SharedPreferencesBasedHostKeyRepository.getInstance(globalSharedPreferences));

        @NotNull final Properties config = new Properties();
        config.put(STRICT_HOST_KEY_CHECKING, strictHostKeyChecking ? YES : NO);
        jschSession.setConfig(config);

        jschSession.connect(connectTimeout != null ? connectTimeout : CONNECT_TIMEOUT_MILLIS);

        sshSessionsCache.put(uuid, jschSession);

        return sshSessionsCache.get(uuid);
    }

    public static synchronized void checkConnection(@NotNull SharedPreferences globalSharedPreferences,
                                                    @NotNull final Router router, final int connectTimeoutMillis) throws Exception {
        // This is used that for a temporary connection check
        // at this point, we can just make a copy of the existing router and assign it a random UUID
        final Router routerCopy = new Router(router);
        final String tempUuid = UUID.randomUUID().toString();
        try {
            routerCopy.setUuid(tempUuid);

            @Nullable Session jschSession = getSSHSession(globalSharedPreferences, routerCopy, connectTimeoutMillis);
            if (jschSession == null) {
                throw new IllegalStateException("Unable to retrieve session - please retry again later!");
            }
            if (!jschSession.isConnected()) {
                jschSession.connect(connectTimeoutMillis);
            }
        } finally {
            //Now drop from LRU Cache
            sshSessionsCache.remove(tempUuid);
        }
    }

    public static synchronized int runCommands(@NotNull SharedPreferences globalSharedPreferences,
                                               @NotNull final Router router, @NotNull final Joiner commandsJoiner, @NotNull final String... cmdToExecute)
            throws Exception {
        Log.d(TAG, "getManualProperty: <router=" + router + " / cmdToExecute=" + Arrays.toString(cmdToExecute) + ">");

        @Nullable ChannelExec channelExec = null;
        @Nullable InputStream in = null;
        @Nullable InputStream err = null;
        try {
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

        } finally {
            Closeables.closeQuietly(in);
            Closeables.closeQuietly(err);
            if (channelExec != null) {
                channelExec.disconnect();
            }
        }

    }

    public static synchronized int runCommands(@NotNull SharedPreferences globalSharedPreferences,
                                               @NotNull final Router router, @NotNull final String... cmdToExecute)
            throws Exception {
        return runCommands(globalSharedPreferences, router, Joiner.on(" && ").skipNulls(), cmdToExecute);
    }

    @Nullable
    public static synchronized String[] getManualProperty(@NotNull final Router router, SharedPreferences globalPreferences, @NotNull final String... cmdToExecute) throws Exception {
        Log.d(TAG, "getManualProperty: <router=" + router + " / cmdToExecute=" + Arrays.toString(cmdToExecute) + ">");

        @Nullable ChannelExec channelExec = null;
        @Nullable InputStream in = null;
        @Nullable InputStream err = null;
        try {
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

        } finally {
            Closeables.closeQuietly(in);
            Closeables.closeQuietly(err);
            if (channelExec != null) {
                channelExec.disconnect();
            }
        }

    }

    @Nullable
    public static synchronized NVRAMInfo getNVRamInfoFromRouter(@Nullable final Router router, SharedPreferences globalPreferences, @Nullable final String... fieldsToFetch) throws Exception {

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
                globalPreferences, "nvram show" + (grep.isEmpty() ? "" : (" | grep -E \"" +
                        Joiner.on("|").join(grep) + "\"")));
        final String[] sshdRsaHostKey = \"fake-key\";
                SSHUtils.getManualProperty(router, globalPreferences, "nvram get sshd_rsa_host_key") : null;
        final String[] sshdDsaHostKey = \"fake-key\";
                SSHUtils.getManualProperty(router, globalPreferences, "nvram get sshd_dss_host_key") : null;

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

    //TODO
    private static class SharedPreferencesBasedHostKeyRepository implements HostKeyRepository {

        @NotNull final SharedPreferences sharedPreferences;

        private static final String TAG = SharedPreferencesBasedHostKeyRepository.class.getSimpleName();

        private static SharedPreferencesBasedHostKeyRepository instance = null;

        private SharedPreferencesBasedHostKeyRepository(@NotNull final SharedPreferences sharedPreferences) {
            this.sharedPreferences = sharedPreferences;
        }

        public static synchronized SharedPreferencesBasedHostKeyRepository getInstance(@NotNull final SharedPreferences sharedPreferences) {
            if (instance == null) {
                instance = new SharedPreferencesBasedHostKeyRepository(sharedPreferences);
            }
            return instance;
        }

        @Override
        public int check(String host, byte[] key) {
            if(host == null) {
                return NOT_INCLUDED;
            }
            String keyFromPrefs = sharedPreferences.getString(host, null);
            if (keyFromPrefs == null) {
                //Add host
                try {
                    add(new HostKey(host, key), null);
                } catch (JSchException e) {
                    e.printStackTrace();
                    return NOT_INCLUDED;
                }
                keyFromPrefs = sharedPreferences.getString(host, null);
                if (keyFromPrefs == null) {
                    return NOT_INCLUDED;
                }
            }

            if (Arrays.equals(keyFromPrefs.getBytes(), key)) {
                return OK;
            }

            return CHANGED;
        }

        @Override
        public void add(HostKey hostkey, UserInfo ui) {
            if (hostkey == null) {
                return;
            }
            final String host = hostkey.getHost();
            //Save host fingerprint into Android preferences
            final SharedPreferences.Editor editor = sharedPreferences.edit();

            final Map<String, String> aMap = new HashMap<>();
            final String type = hostkey.getType();

            aMap.put("type", type);
            aMap.put("marker", hostkey.getMarker());
            aMap.put("host", host);
            aMap.put("comment", hostkey.getComment());
            aMap.put("key", hostkey.getKey());
            aMap.put("fingerprint", hostkey.getFingerPrint(new JSch()));

            final JSONObject jsonObject = new JSONObject(aMap);
            final String jsonString = jsonObject.toString();
            editor.remove(host).apply();
            editor.putString(host + "_" + type, jsonString);

            final Set<String> hostsManaged = sharedPreferences.getStringSet(TAG, new HashSet<String>());
            hostsManaged.add(host);
            hostsManaged.add(host + "_" + type);
            editor.putStringSet(TAG, hostsManaged);

            editor.apply();
        }

        @Override
        public void remove(String host, String type) {
            remove(host, type, null);
        }

        @Override
        public void remove(String host, String type, byte[] key) {
            final SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(host + "_" + type);

            final Set<String> hostsManaged = sharedPreferences.getStringSet(TAG, new HashSet<String>());
            hostsManaged.remove(host);
            hostsManaged.remove(host + "_" + type);
            editor.putStringSet(TAG, hostsManaged);

            editor.apply();
        }

        @Override
        public String getKnownHostsRepositoryID() {
            return this.getClass().getSimpleName();
        }

        @Override
        public HostKey[] getHostKey() {
            final Set<String> hostsManaged = sharedPreferences.getStringSet(TAG, new HashSet<String>());
            final HostKey[] hostKeys = new HostKey[hostsManaged.size()];
            int i = 0;
            for (String hostManaged : hostsManaged) {
                final String hostManagedType = sharedPreferences.getString(hostManaged + "_type", null);
                if (hostManagedType == null) {
                    continue;
                }
                final HostKey[] hostKey = \"fake-key\";
                if (hostKey = \"fake-key\";
                    continue;
                }
                hostKeys[i++] = hostKey[0];
            }

            return hostKeys;
        }

        @Override
        public HostKey[] getHostKey(String host, String type) {
            final String sharedPreferencesString = sharedPreferences.getString(host + "_" + type, null);
            if (sharedPreferencesString == null) {
                return new HostKey[0];
            }
            try {
                final JSONObject jsonObject = new JSONObject(sharedPreferencesString);
                return new HostKey[] {
                        new HostKey(
                            jsonObject.getString("marker"),
                            jsonObject.getString("host"),
                            jsonObject.getInt("type"),
                            jsonObject.getString("key").getBytes(),
                            jsonObject.getString("comment")
                        )
                };
            } catch (Exception e) {
                e.printStackTrace();
                return new HostKey[0];
            }
        }
    }

}
