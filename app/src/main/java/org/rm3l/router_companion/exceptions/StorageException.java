package org.rm3l.router_companion.exceptions;

import android.support.annotation.Nullable;

/**
 * Created by rm3l on 13/12/15.
 */
public class StorageException extends DDWRTCompanionException {

    public StorageException() {
    }

    public StorageException(@Nullable String detailMessage) {
        super(detailMessage);
    }

    public StorageException(@Nullable String detailMessage, @Nullable Throwable throwable) {
        super(detailMessage, throwable);
    }

    public StorageException(@Nullable Throwable throwable) {
        super(throwable);
    }
}
