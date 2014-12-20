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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.resources.Encrypted;

import java.io.Serializable;

/**
 * Encapsulates everything needed to establish a connection to a given router.
 * <p/>
 * Connections can be either SSH-based or HTTP(S)-based.
 *
 * @author <a href="mailto:apps+ddwrt@rm3l.org">Armel S.</a>
 */
public class Router extends Encrypted implements Serializable {

    /**
     * the router UUID
     */
    @NotNull
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
    @NotNull
    private RouterConnectionProtocol routerConnectionProtocol;

    /**
     * the router IP or DNS
     */
    @NotNull
    private String remoteIpAddress;

    /**
     * the port to connect on
     */
    private int remotePort = -1;

    /**
     * the login username
     */
    @NotNull
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

    /**
     * Default constructor
     */
    public Router() {
    }

    /**
     * Constructor
     *
     * @param router the router to copy
     */
    public Router(@Nullable final Router router) {
        this();
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
        }
    }

    /**
     * @return the RouterConnectionProtocol
     */
    @NotNull
    public RouterConnectionProtocol getRouterConnectionProtocol() {
        return routerConnectionProtocol;
    }

    /**
     * Set the RouterConnectionProtocol
     *
     * @param routerConnectionProtocol the RouterConnectionProtocol to set
     * @return this object
     */
    @NotNull
    public Router setRouterConnectionProtocol(@NotNull final RouterConnectionProtocol routerConnectionProtocol) {
        this.routerConnectionProtocol = routerConnectionProtocol;
        return this;
    }

    /**
     * @return the username
     */
    @NotNull
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
     * @param  encrypt whether to encrypt data.
     *                 To avoid encrypting twice, set this to <code>false</code> if <code>privKey</code>
     *                 is known to be encrypted (e.g., when retrieved from the DB)
     * @return this object
     */
    @NotNull
    public Router setUsername(@NotNull final String username, final boolean encrypt) {
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
     * @param  encrypt whether to encrypt data.
     *                 To avoid encrypting twice, set this to <code>false</code> if <code>privKey</code>
     *                 is known to be encrypted (e.g., when retrieved from the DB)
     * @return this object
     */
    @NotNull
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

    /**
     * Set the name
     *
     * @param name the name to set
     * @return this object
     */
    @NotNull
    public Router setName(@Nullable final String name) {
        this.name = name;
        return this;
    }

    /**
     * @return the remoteIpAddress
     */
    @NotNull
    public String getRemoteIpAddress() {
        return remoteIpAddress;
    }

    /**
     * Set the remoteIpAddress
     *
     * @param remoteIpAddress the remoteIpAddress to set
     * @return this object
     */
    @NotNull
    public Router setRemoteIpAddress(@NotNull final String remoteIpAddress) {
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
    @NotNull
    public Router setRemotePort(final int remotePort) {
        this.remotePort = remotePort;
        return this;
    }

    /**
     * @return the uuid
     */
    @NotNull
    public String getUuid() {
        return uuid;
    }

    /**
     * Set the uuid
     *
     * @param uuid the uuid to set
     * @return this object
     */
    @NotNull
    public Router setUuid(@NotNull final String uuid) {
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
     * @param  encrypt whether to encrypt data.
     *                 To avoid encrypting twice, set this to <code>false</code> if <code>privKey</code>
     *                 is known to be encrypted (e.g., when retrieved from the B)
     * @return this object
     */
    @NotNull
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
    @NotNull
    public Router setStrictHostKeyChecking(final boolean strictHostKeyChecking) {
        this.strictHostKeyChecking = strictHostKeyChecking;
        return this;
    }

    /**
     * @return the Router string representation
     */
    @Override
    @NotNull
    public String toString() {
        return "Router{" +
                "routerConnectionProtocol=" + routerConnectionProtocol +
                ", name='" + name + '\'' +
                ", remoteIpAddress='" + remoteIpAddress + '\'' +
                ", remotePort=" + remotePort +
                ", id=" + id +
                ", strictHostKeyChecking=" + strictHostKeyChecking +
                ", uuid='" + uuid + '\'' +
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
    @NotNull
    public Router setId(int id) {
        this.id = id;
        return this;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        @NotNull Router router = (Router) o;

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

    /**
     * RouterConnectionProtocol enum
     */
    public enum RouterConnectionProtocol {

        SSH("ssh", 22, "root", ""),

        HTTP("http", 80, "admin", null),

        HTTPS("https", 443, "admin", null);

        @NotNull
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
        private RouterConnectionProtocol(@NotNull final String channel,
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
        @NotNull
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
}
