package org.rm3l.ddwrt.service.tasks;

import android.content.Context;
import android.support.annotation.NonNull;

import org.rm3l.ddwrt.resources.conn.Router;

/**
 * Created by rm3l on 05/07/15.
 */
public abstract class AbstractBackgroundServiceTask {

    @NonNull
    protected Context mCtx;

    public AbstractBackgroundServiceTask(@NonNull final Context ctx) {
        mCtx = ctx;
    }

    public abstract void buildNotification(@NonNull final Router router) throws Exception;
}
