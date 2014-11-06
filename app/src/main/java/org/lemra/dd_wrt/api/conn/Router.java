package org.lemra.dd_wrt.api.conn;

import android.content.SharedPreferences;

import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lemra.dd_wrt.config.ConfigurationBase;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by armel on 8/9/14.
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

    private boolean strictHostKeyChecking = false;

    private boolean useDefault = true;
    @NotNull
    @SerializedName("uuid")
    private String uuid;

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
