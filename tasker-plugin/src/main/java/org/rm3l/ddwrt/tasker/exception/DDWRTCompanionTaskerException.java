package org.rm3l.ddwrt.tasker.exception;

import android.annotation.TargetApi;
import android.os.Build;

/**
 * Created by rm3l on 11/09/16.
 */
public class DDWRTCompanionTaskerException extends RuntimeException {

    public DDWRTCompanionTaskerException() {
    }

    public DDWRTCompanionTaskerException(String message) {
        super(message);
    }

    public DDWRTCompanionTaskerException(String message, Throwable cause) {
        super(message, cause);
    }

    public DDWRTCompanionTaskerException(Throwable cause) {
        super(cause);
    }

}
