package org.rm3l.ddwrt.resources;

import android.support.annotation.NonNull;

/**
 * Created by rm3l on 11/11/15.
 */
public class WANTrafficData {

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
    private final Number traffIn;

    @NonNull
    private final Number traffOut;

    public WANTrafficData(@NonNull final String router,
                          @NonNull final String date,
                          @NonNull final Number traffIn,
                          @NonNull final Number traffOut) {

        this.router = router;
        this.date = date;
        this.traffIn = traffIn;
        this.traffOut = traffOut;
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
    public Number getTraffIn() {
        return traffIn;
    }

    @NonNull
    public Number getTraffOut() {
        return traffOut;
    }

    public long getId() {
        return id;
    }

    public WANTrafficData setId(long id) {
        this.id = id;
        return this;
    }
}
