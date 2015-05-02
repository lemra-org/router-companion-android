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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.utils.Utils;

import java.io.Serializable;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.ddwrt.resources.Encrypted.d;
import static org.rm3l.ddwrt.resources.Encrypted.e;

/**
 * Encapsulates everything needed to establish a connection to a given router.
 * <p/>
 * Connections can be either SSH-based or HTTP(S)-based.
 *
 * @author <a href="mailto:apps+ddwrt@rm3l.org">Armel S.</a>
 */
public class Router implements Serializable {

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
            this.routerFirmware = router.routerFirmware;
        }
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
            Utils.reportException(e);
        }
    }

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
        private RouterConnectionProtocol(@NonNull final String channel,
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

    public enum SSHAuthenticationMethod {
        NONE,
        PASSWORD,
        PUBLIC_PRIVATE_KEY;
    }

    public enum RouterFirmware {
        DDWRT("DD-WRT"),
        OPENWRT("OpenWrt (Beta)"),
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
