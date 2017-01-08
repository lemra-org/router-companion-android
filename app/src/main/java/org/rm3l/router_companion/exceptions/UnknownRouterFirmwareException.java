package org.rm3l.router_companion.exceptions;

import android.support.annotation.Nullable;

/**
 * Created by rm3l on 08/01/2017.
 */

public class UnknownRouterFirmwareException extends DDWRTCompanionException {

    public UnknownRouterFirmwareException() {
        super();
    }

    public UnknownRouterFirmwareException(@Nullable String detailMessage) {
        super(detailMessage);
    }

    public UnknownRouterFirmwareException(@Nullable String detailMessage, @Nullable Throwable throwable) {
        super(detailMessage, throwable);
    }

    public UnknownRouterFirmwareException(@Nullable Throwable throwable) {
        super(throwable);
    }
}
