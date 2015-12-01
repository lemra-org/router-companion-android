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

package org.rm3l.ddwrt.resources.conn;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.util.concurrent.Striped;
import com.google.gson.Gson;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import org.rm3l.ddwrt.tiles.services.wol.WakeOnLanTile;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.ReportingUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.ddwrt.resources.Encrypted.d;
import static org.rm3l.ddwrt.resources.Encrypted.e;
import static org.rm3l.ddwrt.utils.SSHUtils.CONNECT_TIMEOUT_MILLIS;
import static org.rm3l.ddwrt.utils.SSHUtils.MAX_NUMBER_OF_CONCURRENT_SSH_SESSIONS_PER_ROUTER;
import static org.rm3l.ddwrt.utils.SSHUtils.NO;
import static org.rm3l.ddwrt.utils.SSHUtils.STRICT_HOST_KEY_CHECKING;
import static org.rm3l.ddwrt.utils.SSHUtils.YES;

/**
 * Encapsulates everything needed to establish a connection to a given router.
 * <p/>
 * Connections can be either SSH-based or HTTP(S)-based.
 *
 * @author <a href="mailto:apps+ddwrt@rm3l.org">Armel S.</a>
 */
public class Router implements Serializable {

    private static final String TAG = Router.class.getSimpleName();

    public static final String USE_LOCAL_SSID_LOOKUP = "useLocalSSIDLookup";
    public static final String LOCAL_SSID_LOOKUPS = "localSSIDLookups";
    public static final String FALLBACK_TO_PRIMARY_ADDR = "fallbackToPrimaryAddr";
    /**
     * the router UUID
     */
    @NonNull
    private String uuid;

    /**
     * the internal id (in DB)
     */
    private int id = -1;

    /**
     * the router name
     */
    @Nullable
    private String name;

    /**
     * the connection protocol
     */
    @NonNull
    private RouterConnectionProtocol routerConnectionProtocol;

    /**
     * the router IP or DNS
     */
    @NonNull
    private String remoteIpAddress;

    /**
     * the port to connect on
     */
    private int remotePort = -1;

    /**
     * the login username
     */
    @NonNull
    private String username;

    /**
     * the password
     */
    @Nullable
    private String password;

    /**
     * the private key, applicable only if connection channel is SSH
     */
    @Nullable
    private String privKey;

    private boolean strictHostKeyChecking = false;

    @Nullable
    private RouterFirmware routerFirmware;

    private final LoadingCache<RouterForSessionCache, Session> sessionsCache;

    private final Striped<Lock> sessionsStripes;

    private final Context context;

    public static class RouterForSessionCache {
        @NonNull
        private final Router router;
        private final String ipAddr;
        private final Integer port;
        private final String login;

        public RouterForSessionCache(@NonNull final Router router) {
            this.router = router;
            final Pair<String, Integer> effectiveIpAndPortTuple =
                    router.getEffectiveIpAndPortTuple();
            this.ipAddr = effectiveIpAndPortTuple.first;
            this.port = effectiveIpAndPortTuple.second;
            this.login = router.getUsernamePlain();
        }

        @NonNull
        public Router getRouter() {
            return router;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final RouterForSessionCache that = (RouterForSessionCache) o;

            if (ipAddr != null ? !ipAddr.equals(that.ipAddr) : that.ipAddr != null) return false;
            if (port != null ? !port.equals(that.port) : that.port != null) return false;
            return !(login != null ? !login.equals(that.login) : that.login != null);

        }

        @Override
        public int hashCode() {
            int result = ipAddr != null ? ipAddr.hashCode() : 0;
            result = 31 * result + (port != null ? port.hashCode() : 0);
            result = 31 * result + (login != null ? login.hashCode() : 0);
            return result;
        }
    }

    /**
     * Default constructor
     */
    public Router(@Nullable final Context ctx) {
        this(ctx, null);
    }

    /**
     * Constructor
     *
     * @param router the router to copy
     */
    public Router(@Nullable final Context ctx, @Nullable final Router router) {
        this.context = ctx;
        if (router != null) {
            this.id = router.id;
            this.name = router.name;
            this.uuid = router.uuid;
            this.routerConnectionProtocol = router.routerConnectionProtocol;
            this.remoteIpAddress = router.remoteIpAddress;
            this.remotePort = router.remotePort;
            this.username = router.username;
            this.password = router.password;
            this.privKey = \"fake-key\";
            this.strictHostKeyChecking = router.strictHostKeyChecking;
            this.routerFirmware = router.routerFirmware;
        }

        this.sessionsStripes = Striped.lock(MAX_NUMBER_OF_CONCURRENT_SSH_SESSIONS_PER_ROUTER);

        //Init sessions cache
        this.sessionsCache = CacheBuilder.newBuilder()
                .maximumSize(MAX_NUMBER_OF_CONCURRENT_SSH_SESSIONS_PER_ROUTER)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .removalListener(new RemovalListener<RouterForSessionCache, Session>() {
                    @Override
                    public void onRemoval(@NonNull RemovalNotification<RouterForSessionCache, Session> notification) {
                        final RemovalCause removalCause = notification.getCause();
                        final RouterForSessionCache routerForSessionCache = notification.getKey();
                        if (routerForSessionCache != null) {
                            Crashlytics.log(Log.INFO, TAG,
                                    "Removal Notification for <" + routerForSessionCache.router +
                                            ">. Cause : " + removalCause);
                        }

                        final Session session = notification.getValue();
                        if (session == null) {
                            return;
                        }
                        if (session.isConnected()) {
                            session.disconnect();
                        }
                    }
                })
                .build(new CacheLoader<RouterForSessionCache, Session>() {
                    @Override
                    public Session load(@NonNull RouterForSessionCache key) throws Exception {

                        final String ip = key.ipAddr;
                        Integer port = key.port;
                        if (port == null || port <= 0 || Strings.isNullOrEmpty(ip)) {
                            throw new IllegalArgumentException("port is NULL");
                        }

                        final String login = key.login;

                        final String privKey = \"fake-key\";
                        final JSch jsch = new JSch();

                        final String passwordPlain = key.router.getPasswordPlain();

                        final Router.SSHAuthenticationMethod sshAuthenticationMethod =
                                key.router.getSshAuthenticationMethod();

                        final Session sshSession;
                        switch (sshAuthenticationMethod) {
                            case PUBLIC_PRIVATE_KEY:
                                //noinspection ConstantConditions
                                jsch.addIdentity(key.router.getUuid() + "_" + login + "_" + port + "_" + ip,
                                        !isNullOrEmpty(privKey) ? privKey.getBytes() : null,
                                        null,
                                        !isNullOrEmpty(passwordPlain) ? passwordPlain.getBytes() : null);
                                sshSession = jsch.getSession(login, ip, port);
                                break;
                            case PASSWORD:
                                sshSession = jsch.getSession(login, ip, port);
                                sshSession.setPassword(passwordPlain);
                                break;
                            default:
                                sshSession = jsch.getSession(login, ip, port);
                                break;
                        }

                        final boolean strictHostKeyChecking = key.router.isStrictHostKeyChecking();
                        //Set known hosts file to preferences file
                        //        jsch.setKnownHosts();

                        final Properties config = new Properties();
                        config.put(STRICT_HOST_KEY_CHECKING, strictHostKeyChecking ? YES : NO);

                        sshSession.setConfig(config);

//                        sshSession
//                                .setServerAliveInterval(CONNECTION_KEEP_ALIVE_INTERVAL_MILLIS);

                        sshSession
                                .connect(CONNECT_TIMEOUT_MILLIS);

                        return sshSession;
                    }
                });
    }

    /**
     * @return the RouterConnectionProtocol
     */
    @NonNull
    public RouterConnectionProtocol getRouterConnectionProtocol() {
        return routerConnectionProtocol;
    }

    /**
     * Set the RouterConnectionProtocol
     *
     * @param routerConnectionProtocol the RouterConnectionProtocol to set
     * @return this object
     */
    @NonNull
    public Router setRouterConnectionProtocol(@NonNull final RouterConnectionProtocol routerConnectionProtocol) {
        this.routerConnectionProtocol = routerConnectionProtocol;
        return this;
    }

    /**
     * @return the username
     */
    @NonNull
    public String getUsername() {
        return username;
    }

    /**
     * @return the unencrypted username
     */
    @Nullable
    public String getUsernamePlain() {
        return d(username);
    }

    /**
     * Set the username
     *
     * @param username the username to set
     * @param encrypt  whether to encrypt data.
     *                 To avoid encrypting twice, set this to <code>false</code> if <code>privKey</code>
     *                 is known to be encrypted (e.g., when retrieved from the DB)
     * @return this object
     */
    @NonNull
    public Router setUsername(@NonNull final String username, final boolean encrypt) {
        //noinspection ConstantConditions
        this.username = encrypt ? e(username) : username;
        return this;
    }

    /**
     * @return the password
     */
    @Nullable
    public String getPassword() {
        return password;
    }

    /**
     * @return the password
     */
    @Nullable
    public String getPasswordPlain() {
        return password != null ? d(password) : null;
    }

    /**
     * Set the password
     *
     * @param password the password to set
     * @param encrypt  whether to encrypt data.
     *                 To avoid encrypting twice, set this to <code>false</code> if <code>privKey</code>
     *                 is known to be encrypted (e.g., when retrieved from the DB)
     * @return this object
     */
    @NonNull
    public Router setPassword(@Nullable final String password, final boolean encrypt) {
        this.password = encrypt ? e(password) : password;
        return this;
    }

    /**
     * @return the name
     */
    @Nullable
    public String getName() {
        return name;
    }

    @NonNull
    public String getDisplayName() {
        return (isNullOrEmpty(name) ? "-" : name);
    }

    /**
     * Set the name
     *
     * @param name the name to set
     * @return this object
     */
    @NonNull
    public Router setName(@Nullable final String name) {
        this.name = name;
        return this;
    }

    /**
     * @return the remoteIpAddress
     */
    @NonNull
    public String getRemoteIpAddress() {
        return remoteIpAddress;
    }

    /**
     * Set the remoteIpAddress
     *
     * @param remoteIpAddress the remoteIpAddress to set
     * @return this object
     */
    @NonNull
    public Router setRemoteIpAddress(@NonNull final String remoteIpAddress) {
        this.remoteIpAddress = remoteIpAddress;
        return this;
    }

    /**
     * @return the remotePort, if any, or the default port for the routerConnectionProtocol
     */
    public int getRemotePort() {
        return remotePort <= 0 ? this.routerConnectionProtocol.getDefaultPort() : remotePort;
    }

    /**
     * Set the remotePort
     *
     * @param remotePort the remotePort to set
     * @return this object
     */
    @NonNull
    public Router setRemotePort(final int remotePort) {
        this.remotePort = remotePort;
        return this;
    }

    /**
     * @return the uuid
     */
    @NonNull
    public String getUuid() {
        return uuid;
    }

    /**
     * Set the uuid
     *
     * @param uuid the uuid to set
     * @return this object
     */
    @NonNull
    public Router setUuid(@NonNull final String uuid) {
        this.uuid = uuid;
        return this;
    }

    /**
     * @return the privKey
     */
    @Nullable
    public String getPrivKey() {
        return privKey;
    }

    /**
     * @return the decrypted privKey
     */
    @Nullable
    public String getPrivKeyPlain() {
        return privKey != null ? d(privKey) : null;
    }

    /**
     * Set the privKey
     *
     * @param privKey the privKey to set
     * @param encrypt whether to encrypt data.
     *                To avoid encrypting twice, set this to <code>false</code> if <code>privKey</code>
     *                is known to be encrypted (e.g., when retrieved from the B)
     * @return this object
     */
    @NonNull
    public Router setPrivKey(@Nullable final String privKey, final boolean encrypt) {
        this.privKey = \"fake-key\";
        return this;
    }

    /**
     * @return whether the strictHostKeyChecking flag is on or off
     */
    public boolean isStrictHostKeyChecking() {
        return strictHostKeyChecking;
    }

    /**
     * Set the strictHostKeyChecking
     *
     * @param strictHostKeyChecking the strictHostKeyChecking to set
     * @return this object
     */
    @NonNull
    public Router setStrictHostKeyChecking(final boolean strictHostKeyChecking) {
        this.strictHostKeyChecking = strictHostKeyChecking;
        return this;
    }

    /**
     * @return the Router string representation
     */
    @Override
    @NonNull
    public String toString() {
        return "Router{" +
                "sshAuth=Type=" + getSshAuthenticationMethod() +
                ", routerConnectionProtocol=" + routerConnectionProtocol +
                ", name='" + name + '\'' +
                ", remoteIpAddress='" + remoteIpAddress + '\'' +
                ", remotePort=" + remotePort +
                ", id=" + id +
                ", strictHostKeyChecking=" + strictHostKeyChecking +
                ", uuid='" + uuid + '\'' +
                ", routerFirmware=" + routerFirmware +
                '}';
    }

    /**
     * @return the internal DB id
     */
    public int getId() {
        return id;
    }

    /**
     * Set the internal DB id
     *
     * @param id the internal DB id to set
     * @return this object
     */
    @NonNull
    public Router setId(int id) {
        this.id = id;
        return this;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Router router = (Router) o;

        if (id != router.id) return false;
        if (remotePort != router.remotePort) return false;
        if (strictHostKeyChecking != router.strictHostKeyChecking) return false;
        if (name != null ? !name.equals(router.name) : router.name != null) return false;
        if (password != null ? !password.equals(router.password) : router.password != null)
            return false;
        if (privKey != null ? !privKey.equals(router.privKey) : router.privKey != null)
            return false;
        if (!remoteIpAddress.equals(router.remoteIpAddress)) return false;
        if (routerConnectionProtocol != router.routerConnectionProtocol) return false;
        if (routerFirmware != router.routerFirmware) return false;
        if (!username.equals(router.username)) return false;
        if (!uuid.equals(router.uuid)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = routerConnectionProtocol.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + remoteIpAddress.hashCode();
        result = 31 * result + remotePort;
        result = 31 * result + username.hashCode();
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (privKey != null ? privKey.hashCode() : 0);
        result = 31 * result + id;
        result = 31 * result + (strictHostKeyChecking ? 1 : 0);
        result = 31 * result + uuid.hashCode();
        return result;
    }

    @NonNull
    public SSHAuthenticationMethod getSshAuthenticationMethod() {
        if (!isNullOrEmpty(privKey)) {
            return SSHAuthenticationMethod.PUBLIC_PRIVATE_KEY;
        }
        if (!isNullOrEmpty(password)) {
            return SSHAuthenticationMethod.PASSWORD;
        }
        return SSHAuthenticationMethod.NONE;
    }

    @Nullable
    public RouterFirmware getRouterFirmware() {
        return routerFirmware;
    }

    public void setRouterFirmware(@Nullable RouterFirmware routerFirmware) {
        this.routerFirmware = routerFirmware;
    }

    public void setRouterFirmware(@Nullable String routerFirmwareStr) {
        if (isNullOrEmpty(routerFirmwareStr)) {
            return;
        }
        try {
            setRouterFirmware(RouterFirmware.valueOf(routerFirmwareStr));
        } catch (final Exception e) {
            ReportingUtils.reportException(null, e);
        }
    }

    @NonNull
    private Pair<String, Integer> getEffectiveIpAndPortTuple() {
        final LocalSSIDLookup ssidLookup = getEffectiveLocalSSIDLookup(this, context);
        String ip = remoteIpAddress;
        Integer port = remotePort;
        if (ssidLookup != null) {
            ip = ssidLookup.getReachableAddr();
            port = ssidLookup.getPort();
        }
        return Pair.create(ip, port);
    }

    @Nullable
    public Session getSSHSession() throws Exception {
        final RouterForSessionCache routerForSessionCache = new RouterForSessionCache(this);
//        final Pair<String, Integer> effectiveIpAndPortTuple = getEffectiveIpAndPortTuple();
        final Lock lock = this.sessionsStripes.get(routerForSessionCache);
        try {
            lock.lock();
            final Session session = this.sessionsCache.get(routerForSessionCache);
            if (session != null && !session.isConnected()) {
                session.connect(CONNECT_TIMEOUT_MILLIS);
            }
            return session;
        } catch (final Exception e) {
            /*
             * Invalidate record right away so it can be retried again later
             */
            this.sessionsCache.invalidate(routerForSessionCache);
            throw e;
        } finally {
            lock.unlock();
        }
    }

    public void destroyActiveSession() {
        final RouterForSessionCache routerForSessionCache = new RouterForSessionCache(this);
//        final Pair<String, Integer> effectiveIpAndPortTuple = getEffectiveIpAndPortTuple();
        final Lock lock = this.sessionsStripes.get(routerForSessionCache);
        try {
            lock.lock();
            this.sessionsCache.invalidate(routerForSessionCache);
        } finally {
            lock.unlock();
        }
    }

    public void destroyAllSessions() {
        this.sessionsCache.invalidateAll();
    }

    @Nullable
    public static String getEffectiveRemoteAddr(@Nullable final Router router,
                                                @Nullable final Context ctx) {
        if (router == null || ctx == null) {
            return null;
        }
        final String primaryRemoteIpAddress = router.getRemoteIpAddress();
        if (!isUseLocalSSIDLookup(router, ctx)) {
            return primaryRemoteIpAddress;
        }
        //else get alternate depending on current network
        final LocalSSIDLookup effectiveLocalSSIDLookup = getEffectiveLocalSSIDLookup(router, ctx);
        if (effectiveLocalSSIDLookup == null) {
            return primaryRemoteIpAddress;
        }
        return effectiveLocalSSIDLookup.getReachableAddr();
    }

    @Nullable
    public static Integer getEffectivePort(@Nullable final Router router,
                                                @Nullable final Context ctx) {
        if (router == null || ctx == null) {
            return null;
        }
        final int primaryRemotePort = router.getRemotePort();
        if (!isUseLocalSSIDLookup(router, ctx)) {
            return primaryRemotePort;
        }
        //else get alternate depending on current network
        final LocalSSIDLookup effectiveLocalSSIDLookup = getEffectiveLocalSSIDLookup(router, ctx);
        if (effectiveLocalSSIDLookup == null) {
            return primaryRemotePort;
        }
        return effectiveLocalSSIDLookup.getPort();
    }

    @Nullable
    public static LocalSSIDLookup getEffectiveLocalSSIDLookup(@Nullable final Router router,
                                                     @Nullable final Context ctx) {
        if (router == null || ctx == null) {
            return null;
        }
        final String currentNetworkSSID = Utils.getWifiName(ctx);
        //Detect network and use the corresponding IP Address if required
        final Collection<Router.LocalSSIDLookup> localSSIDLookupData = router.getLocalSSIDLookupData(ctx);
        if (!localSSIDLookupData.isEmpty()) {
            for (final Router.LocalSSIDLookup localSSIDLookup : localSSIDLookupData) {
                if (localSSIDLookup == null) {
                    continue;
                }
                final String networkSsid = localSSIDLookup.getNetworkSsid();
                if (networkSsid == null || networkSsid.isEmpty()) {
                    continue;
                }
                if (networkSsid.equals(currentNetworkSSID) ||
                        ("\"" + networkSsid + "\"").equals(currentNetworkSSID)) {
                    return localSSIDLookup;
                }
            }
        }
        return null;
    }

    public static boolean isUseLocalSSIDLookup(@Nullable final Router router,
                                               @Nullable final Context ctx) {
        if (router == null || ctx == null) {
            return false;
        }
        final SharedPreferences sharedPreferences = ctx.getSharedPreferences(router.getUuid(), Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(USE_LOCAL_SSID_LOOKUP, false);
    }

    public boolean isUseLocalSSIDLookup(@Nullable final Context ctx) {
        return isUseLocalSSIDLookup(this, ctx);
    }

    public void setUseLocalSSIDLookup(@NonNull final Context ctx, final boolean value) {
        final SharedPreferences sharedPreferences = ctx.getSharedPreferences(this.getUuid(), Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(USE_LOCAL_SSID_LOOKUP, value).apply();
        Utils.requestBackup(ctx);
    }

    public boolean isFallbackToPrimaryAddr(@Nullable final Context ctx) {
        if (ctx == null) {
            return false;
        }
        final SharedPreferences sharedPreferences = ctx.getSharedPreferences(this.getUuid(), Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(FALLBACK_TO_PRIMARY_ADDR, false);
    }

    public void setFallbackToPrimaryAddr(@NonNull final Context ctx, final boolean value) {
        final SharedPreferences sharedPreferences = ctx.getSharedPreferences(this.getUuid(), Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(FALLBACK_TO_PRIMARY_ADDR, value).apply();
                Utils.requestBackup(ctx);
    }

    @NonNull
    public Collection<LocalSSIDLookup> getLocalSSIDLookupData(@NonNull final Context ctx) {
        final SharedPreferences sharedPreferences = ctx.getSharedPreferences(this.getUuid(), Context.MODE_PRIVATE);
        final Set<String> localSSIDLookupStringSet = sharedPreferences.getStringSet(LOCAL_SSID_LOOKUPS, new HashSet<String>());
        final List<LocalSSIDLookup> localSSIDLookups = new ArrayList<>(localSSIDLookupStringSet.size());
        final Gson gson = WakeOnLanTile.GSON_BUILDER.create();
        for (final String localSSIDLookupString : localSSIDLookupStringSet) {
            if (localSSIDLookupString == null || localSSIDLookupString.isEmpty()) {
                continue;
            }
            try {
                localSSIDLookups.add(gson.fromJson(localSSIDLookupString, LocalSSIDLookup.class));
            } catch (final Exception e) {
                e.printStackTrace();
                //No worries
            }
        }
        return localSSIDLookups;
    }

    public void setLocalSSIDLookupData(@NonNull final Context ctx, Collection<LocalSSIDLookup> localSSIDLookups) {
        final SharedPreferences sharedPreferences = ctx.getSharedPreferences(this.getUuid(), Context.MODE_PRIVATE);
        sharedPreferences.edit().remove(LOCAL_SSID_LOOKUPS).apply();
        final Set<String> localSSIDLookupStringSet = new HashSet<>();
        if (localSSIDLookups != null) {
            final Gson gson = WakeOnLanTile.GSON_BUILDER.create();
            for (final LocalSSIDLookup localSSIDLookup : localSSIDLookups) {
                localSSIDLookupStringSet.add(gson.toJson(localSSIDLookup));
            }
        }
        sharedPreferences.edit().putStringSet(LOCAL_SSID_LOOKUPS, localSSIDLookupStringSet).apply();
        Utils.requestBackup(ctx);
    }

    @Nullable
    public static String getRouterModel(@Nullable final Context context, @Nullable final Router routerAt) {
        if (context == null || routerAt == null) {
            return null;
        }
        return context.getSharedPreferences(routerAt.getUuid(), Context.MODE_PRIVATE)
                .getString(NVRAMInfo.MODEL, null);
    }

    public static String getRouterAvatarUrl(@Nullable final String routerModel,
                                            @Nullable final String[] opts) throws UnsupportedEncodingException {
        return String.format("%s/%s/%s.jpg", DDWRTCompanionConstants.IMAGE_CDN_URL_PREFIX,
                Joiner
                        .on(",")
                        .skipNulls().join(opts != null ?
                        opts : DDWRTCompanionConstants.CLOUDINARY_OPTS),
                URLEncoder.encode(nullToEmpty(routerModel).toLowerCase().replaceAll("\\s+", ""),
                        Charsets.UTF_8.name()));
    }

    public static class LocalSSIDLookup {

        private String networkSsid;
        private String reachableAddr;
        private int port = 22;

        public String getNetworkSsid() {
            return networkSsid;
        }

        public void setNetworkSsid(String networkSsid) {
            this.networkSsid = networkSsid;
        }

        public String getReachableAddr() {
            return reachableAddr;
        }

        public void setReachableAddr(String reachableAddr) {
            this.reachableAddr = reachableAddr;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public static final int RouterConnectionProtocol_SSH = 22;
    public static final int RouterConnectionProtocol_HTTP = 80;
    public static final int RouterConnectionProtocol_HTTPS = 443;

    /**
     * RouterConnectionProtocol enum
     */
    public enum RouterConnectionProtocol {

        SSH("ssh", 22, "root", ""),

        HTTP("http", 80, "admin", null),

        HTTPS("https", 443, "admin", null);

        @NonNull
        private final String channel;

        private final int defaultPort;

        @Nullable
        private final String defaultUsername;

        @Nullable
        private final String defaultPassword;

        /**
         * Constructor
         *
         * @param channel         the channel
         * @param defaultPort     the default port
         * @param defaultUsername the default username
         * @param defaultPassword the default password
         */
        RouterConnectionProtocol(@NonNull final String channel,
                                         final int defaultPort,
                                         @Nullable final String defaultUsername,
                                         @Nullable final String defaultPassword) {
            this.channel = channel;
            this.defaultPort = defaultPort;
            this.defaultUsername = defaultUsername;
            this.defaultPassword = defaultPassword;
        }

        /**
         * @return the channel
         */
        @NonNull
        public String getChannel() {
            return channel;
        }

        /**
         * @return the defaultUsername
         */
        @Nullable
        public String getDefaultUsername() {
            return defaultUsername;
        }

        /**
         * @return the defaultPassword
         */
        @Nullable
        public String getDefaultPassword() {
            return defaultPassword;
        }

        /**
         * @return the defaultPort
         */
        public int getDefaultPort() {
            return defaultPort;
        }
    }

    public static final int SSHAuthenticationMethod_NONE = 1;
    public static final int SSHAuthenticationMethod_PASSWORD = 2;
    public static final int SSHAuthenticationMethod_PUBLIC_PRIVATE_KEY = \"fake-key\";

    public enum SSHAuthenticationMethod {
        NONE,
        PASSWORD,
        PUBLIC_PRIVATE_KEY;
    }

    public static final int RouterFirmware_DDWRT = 1;
    public static final int RouterFirmware_OPENWRT = 2;
    public static final int RouterFirmware_DEMO = 3;
    public static final int RouterFirmware_UNKNOWN = 4;

    public enum RouterFirmware {
        DDWRT("DD-WRT"),
        OPENWRT("OpenWrt (Beta)"),
        DEMO("Demo"),
        UNKNOWN("???");

        @NonNull
        final String displayName;

        RouterFirmware(@NonNull final String displayName) {
            this.displayName = displayName;
        }

        @NonNull
        public String getDisplayName() {
            return displayName;
        }
    }
}
