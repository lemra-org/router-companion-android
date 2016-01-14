package org.rm3l.ddwrt.resources;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.actions.activity.PingRTT;

/**
 * Created by rm3l on 12/01/16.
 */
public class SpeedTestResult {

    /**
     * the internal id (in DB)
     */
    private long id = -1l;

    @NonNull
    private String router;

    @NonNull
    //YYYY-MM-dd
    private String date;

    @NonNull
    private Number wanPing;

    @NonNull
    private Number wanDl;

    @NonNull
    private Number wanUl;

    /**
     * Useless ?
     */
    @Nullable
    private String connectionType;

    @Nullable
    private Number connectionDl;

    @Nullable
    private Number connectionUl;

    @NonNull
    private String server;

    @Nullable
    private String serverCountryCode;

    private PingRTT wanPingRTT;

    public SpeedTestResult(@NonNull String router,
                           @NonNull String date,
                           @NonNull String server,
                           @NonNull Number wanPing,
                           @NonNull Number wanDl,
                           @NonNull Number wanUl,
                           @Nullable String connectionType,
                           @Nullable Number connectionDl,
                           @Nullable Number connectionUl,
                           @Nullable String serverCountryCode) {

        this.router = router;
        this.date = date;
        this.wanPing = wanPing;
        this.wanDl = wanDl;
        this.wanUl = wanUl;
        this.connectionType = connectionType;
        this.connectionDl = connectionDl;
        this.connectionUl = connectionUl;
        this.server = server;
        this.serverCountryCode = serverCountryCode;
    }

    public SpeedTestResult() {}

    public SpeedTestResult setRouter(@NonNull String router) {
        this.router = router;
        return this;
    }

    public SpeedTestResult setDate(@NonNull String date) {
        this.date = date;
        return this;
    }

    public SpeedTestResult setWanPing(@NonNull Number wanPing) {
        this.wanPing = wanPing;
        return this;
    }

    public SpeedTestResult setWanDl(@NonNull Number wanDl) {
        this.wanDl = wanDl;
        return this;
    }

    public SpeedTestResult setWanUl(@NonNull Number wanUl) {
        this.wanUl = wanUl;
        return this;
    }

    public SpeedTestResult setConnectionType(@Nullable String connectionType) {
        this.connectionType = connectionType;
        return this;
    }

    public SpeedTestResult setConnectionDl(@Nullable Number connectionDl) {
        this.connectionDl = connectionDl;
        return this;
    }

    public SpeedTestResult setConnectionUl(@Nullable Number connectionUl) {
        this.connectionUl = connectionUl;
        return this;
    }

    public SpeedTestResult setServer(@NonNull String server) {
        this.server = server;
        return this;
    }

    public SpeedTestResult setServerCountryCode(@Nullable String serverCountryCode) {
        this.serverCountryCode = serverCountryCode;
        return this;
    }

    public long getId() {
        return id;
    }

    public SpeedTestResult setId(long id) {
        this.id = id;
        return this;
    }

    @Nullable
    public String getServerCountryCode() {
        return serverCountryCode;
    }

    @NonNull
    public String getRouter() {
        return router;
    }

    @NonNull
    public String getDate() {
        return date;
    }

    @NonNull
    public Number getWanPing() {
        return wanPing;
    }

    @NonNull
    public Number getWanDl() {
        return wanDl;
    }

    @NonNull
    public Number getWanUl() {
        return wanUl;
    }

    @Nullable
    public String getConnectionType() {
        return connectionType;
    }

    @Nullable
    public Number getConnectionDl() {
        return connectionDl;
    }

    @Nullable
    public Number getConnectionUl() {
        return connectionUl;
    }

    @NonNull
    public String getServer() {
        return server;
    }

    public PingRTT getWanPingRTT() {
        return wanPingRTT;
    }

    public SpeedTestResult setWanPingRTT(PingRTT wanPingRTT) {
        this.wanPingRTT = wanPingRTT;
        if (wanPingRTT != null) {
            this.setWanPing(wanPingRTT.getAvg());
        }
        return this;
    }
}

