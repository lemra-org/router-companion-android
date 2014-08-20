package org.lemra.dd_wrt.exceptions;

/**
 * Created by armel on 8/18/14.
 */
public class DDWRTNoDataException extends DDWRTCompanionException {
    public DDWRTNoDataException() {
    }

    public DDWRTNoDataException(String detailMessage) {
        super(detailMessage);
    }

    public DDWRTNoDataException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public DDWRTNoDataException(Throwable throwable) {
        super(throwable);
    }
}
