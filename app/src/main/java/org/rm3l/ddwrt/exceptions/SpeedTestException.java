package org.rm3l.ddwrt.exceptions;

import android.support.annotation.Nullable;

/**
 * Created by rm3l on 18/01/16.
 */
public class SpeedTestException extends DDWRTCompanionException {
    public SpeedTestException() {
    }

    public SpeedTestException(@Nullable String detailMessage) {
        super(detailMessage);
    }

    public SpeedTestException(@Nullable String detailMessage, @Nullable Throwable throwable) {
        super(detailMessage, throwable);
    }

    public SpeedTestException(@Nullable Throwable throwable) {
        super(throwable);
    }
}