/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Armel S.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.lemra.dd_wrt.api.conn;

import android.content.SharedPreferences;

import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lemra.dd_wrt.config.ConfigurationBase;

import java.io.Serializable;
import java.util.UUID;

/**
 * Encapsulates everything needed to establish a connection to a given router.
 * Connections can be either SSH-based or HTTP(S)-based.
 */
public class Router implements Serializable {

    private static final String REMOTE_IP_ADDRESS = "ipAddress";
    private static final String REMOTE_PORT = "port";
    @NotNull
    @SerializedName("channel")
    private RouterConnectionProtocol routerConnectionProtocol;
    @NotNull
    @SerializedName("router_name")
    private String name;
    @NotNull
    private String remoteIpAddress;
    private int remotePort = 22;
    @Nullable
    private String username;
    @Nullable
    private String password;
    @Nullable
    private String privKey;
    @Nullable
    private String privkeyPath;
    private int id = -1;

    private boolean strictHostKeyChecking = false;

    private boolean useDefault = true;
    @NotNull
    @SerializedName("uuid")
    private String uuid;

    public Router() {
    }

    public Router(@Nullable final Router router) {
        this();
        if (router != null) {
            this.id = router.id;
            this.name = router.name;
            this.uuid = router.uuid;
            this.remoteIpAddress = router.remoteIpAddress;
            this.remotePort = router.remotePort;
            this.username = router.username;
            this.password = router.password;
            this.privKey = \"fake-key\";
            this.privkeyPath = router.privkeyPath;
            this.routerConnectionProtocol = router.routerConnectionProtocol;
            this.strictHostKeyChecking = router.strictHostKeyChecking;
            this.useDefault = router.useDefault;
        }
    }

    @NotNull
    public static Router loadFromPreferences(@NotNull ConfigurationBase paramConfigurationBase, String paramString) {
        @NotNull final Router routerInfo = new Router();

        final SharedPreferences localSharedPreferences = paramConfigurationBase.getPreferences(paramString);
        routerInfo.uuid = localSharedPreferences.getString("uuid", UUID.randomUUID().toString());
        routerInfo.routerConnectionProtocol = RouterConnectionProtocol.valueOf(localSharedPreferences.getString("channel",
                RouterConnectionProtocol.SSH.toString()));
        routerInfo.name = localSharedPreferences.getString("router_name", "DD-WRT Router");
        routerInfo.remoteIpAddress = localSharedPreferences.getString("ipAddress", "192.168.1.1");
        routerInfo.remotePort = localSharedPreferences.getInt("port", -1) <= 0 ?
                (RouterConnectionProtocol.SSH.equals(routerInfo.routerConnectionProtocol) ? 22 :
                        (RouterConnectionProtocol.HTTP.equals(routerInfo.routerConnectionProtocol) ? 80 : 443)) :
                localSharedPreferences.getInt("port", 0);
        routerInfo.useDefault = localSharedPreferences.getBoolean("useDefault", true);
        routerInfo.username = routerInfo.useDefault ? routerInfo.routerConnectionProtocol.defaultUsername :
                localSharedPreferences.getString("username", null);
        routerInfo.password = routerInfo.useDefault ? routerInfo.routerConnectionProtocol.defaultPassword :
                localSharedPreferences.getString("password", null);

        return routerInfo;
    }

    @NotNull
    public RouterConnectionProtocol getRouterConnectionProtocol() {
        return routerConnectionProtocol;
    }

    public void setRouterConnectionProtocol(@NotNull RouterConnectionProtocol routerConnectionProtocol) {
        this.routerConnectionProtocol = routerConnectionProtocol;
    }

    @Nullable
    public String getUsername() {
        return username;
    }

    public void setUsername(@Nullable String username) {
        this.username = username;
    }

    @Nullable
    public String getPassword() {
        return password;
    }

    public void setPassword(@Nullable String password) {
        this.password = password;
    }

    public boolean isUseDefault() {
        return useDefault;
    }

    public void setUseDefault(boolean useDefault) {
        this.useDefault = useDefault;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    public String getRemoteIpAddress() {
        return remoteIpAddress;
    }

    public void setRemoteIpAddress(@NotNull String remoteIpAddress) {
        this.remoteIpAddress = remoteIpAddress;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }

    @NotNull
    public String getUuid() {
        return uuid;
    }

    public void setUuid(@NotNull String uuid) {
        this.uuid = uuid;
    }

    public void saveToPreferences(@NotNull final ConfigurationBase paramConfigurationBase) {
        paramConfigurationBase
                .getPreferences(this.uuid)
                .edit()
                .putString("uuid", this.uuid)
                .putString("channel", this.routerConnectionProtocol.channel)
                .putString("router_name", this.name)
                .putString("ipAddress", this.remoteIpAddress)
                .putInt("port", this.remotePort)
                .putBoolean("useDefault", this.useDefault)
                .putString("username", this.username)
                .putString("password", this.password)
                .commit();
    }

    @Nullable
    public String getPrivKey() {
        return privKey;
    }

    public void setPrivKey(@Nullable String privKey) {
        this.privKey = \"fake-key\";
    }

    public boolean isStrictHostKeyChecking() {
        return strictHostKeyChecking;
    }

    public void setStrictHostKeyChecking(boolean strictHostKeyChecking) {
        this.strictHostKeyChecking = strictHostKeyChecking;
    }


    @NotNull
    public String toString() {
        return this.name;
    }

    @Nullable
    public String getPrivkeyPath() {
        return privkeyPath;
    }

    public void setPrivkeyPath(@Nullable String privkeyPath) {
        this.privkeyPath = privkeyPath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        @NotNull Router router = (Router) o;

        if (id != router.id) return false;
        if (remotePort != router.remotePort) return false;
        if (strictHostKeyChecking != router.strictHostKeyChecking) return false;
        if (useDefault != router.useDefault) return false;
        if (name != null ? !name.equals(router.name) : router.name != null) return false;
        if (password != null ? !password.equals(router.password) : router.password != null)
            return false;
        if (privKey != null ? !privKey.equals(router.privKey) : router.privKey != null)
            return false;
        if (privkeyPath != null ? !privkeyPath.equals(router.privkeyPath) : router.privkeyPath != null)
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
        result = 31 * result + (privkeyPath != null ? privkeyPath.hashCode() : 0);
        result = 31 * result + id;
        result = 31 * result + (strictHostKeyChecking ? 1 : 0);
        result = 31 * result + (useDefault ? 1 : 0);
        result = 31 * result + uuid.hashCode();
        return result;
    }

    public enum RouterConnectionProtocol {
        SSH("ssh", "root", "ddwrt"),

        HTTP("http", "admin", ""),

        HTTPS("https", "admin", "");

        @NotNull
        final String channel;

        @Nullable
        final String defaultUsername;

        @Nullable
        final String defaultPassword;

        RouterConnectionProtocol(@NotNull final String channel,
                                 @Nullable final String defaultUsername,
                                 @Nullable final String defaultPassword) {
            this.channel = channel;
            this.defaultUsername = defaultUsername;
            this.defaultPassword = defaultPassword;
        }

        @NotNull
        public String getChannel() {
            return channel;
        }

        @Nullable
        public String getDefaultUsername() {
            return defaultUsername;
        }

        @Nullable
        public String getDefaultPassword() {
            return defaultPassword;
        }
    }
}
