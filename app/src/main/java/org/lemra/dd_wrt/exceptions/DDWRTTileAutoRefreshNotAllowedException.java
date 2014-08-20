package org.lemra.dd_wrt.exceptions;

/**
 * Created by armel on 8/18/14.
 */
public class DDWRTTileAutoRefreshNotAllowedException extends DDWRTCompanionException {

    public DDWRTTileAutoRefreshNotAllowedException() {
    }

    public DDWRTTileAutoRefreshNotAllowedException(String detailMessage) {
        super(detailMessage);
    }

    public DDWRTTileAutoRefreshNotAllowedException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public DDWRTTileAutoRefreshNotAllowedException(Throwable throwable) {
        super(throwable);
    }
}
