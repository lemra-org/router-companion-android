package org.rm3l.ddwrt.service.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import org.rm3l.ddwrt.resources.conn.Router;

import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;

/**
 * Created by rm3l on 05/07/15.
 */
public abstract class AbstractBackgroundServiceTask {

    @NonNull
    protected Context mCtx;

    protected final SharedPreferences globalPreferences;

    public AbstractBackgroundServiceTask(@NonNull final Context ctx) {
        mCtx = ctx;
        globalPreferences = mCtx.getSharedPreferences(
                DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
    }

    public abstract void runBackgroundServiceTask(@NonNull final Router router) throws Exception;
}
