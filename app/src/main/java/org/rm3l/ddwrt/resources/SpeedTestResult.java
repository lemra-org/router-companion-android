package org.rm3l.ddwrt.resources;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by rm3l on 12/01/16.
 */
public class SpeedTestResult {

    /**
     * the internal id (in DB)
     */
    private long id = -1l;

    @NonNull
    private final String router;

    @NonNull
    //YYYY-MM-dd
    private final String date;

    @NonNull
    private final Number wanPing;

    @NonNull
    private final Number wanDl;

    @NonNull
    private final Number wanUl;

    /**
     * Useless ?
     */
    @Nullable
    private final String connectionType;

    @Nullable
    private final Number connectionDl;

    @Nullable
    private final Number connectionUl;

    @NonNull
    private final String server;

    @Nullable
    private final String serverCountryCode;

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
}
