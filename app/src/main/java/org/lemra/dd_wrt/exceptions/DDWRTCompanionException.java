package org.lemra.dd_wrt.exceptions;

/**
 * Created by armel on 8/18/14.
 */
public class DDWRTCompanionException extends RuntimeException {

    public DDWRTCompanionException() {
    }

    public DDWRTCompanionException(String detailMessage) {
        super(detailMessage);
    }

    public DDWRTCompanionException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public DDWRTCompanionException(Throwable throwable) {
        super(throwable);
    }
}
