package org.rm3l.ddwrt.exceptions.acra;

import android.support.annotation.Nullable;

import org.rm3l.ddwrt.exceptions.DDWRTCompanionException;

/**
 * Created by rm3l on 10/05/15.
 */
public class RouterActionTriggered extends DDWRTCompanionException {

    public RouterActionTriggered(@Nullable String detailMessage) {
        super(detailMessage);
    }

}
