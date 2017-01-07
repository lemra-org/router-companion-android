package org.rm3l.router_companion.exceptions.acra;

import android.support.annotation.Nullable;

import org.rm3l.router_companion.exceptions.DDWRTCompanionException;

/**
 * Created by rm3l on 01/05/15.
 */
public class WidgetInstalledACRANotification extends DDWRTCompanionException {

    public WidgetInstalledACRANotification() {
        super();
    }

    public WidgetInstalledACRANotification(@Nullable String detailMessage) {
        super(detailMessage);
    }

    public WidgetInstalledACRANotification(@Nullable String detailMessage, @Nullable Throwable throwable) {
        super(detailMessage, throwable);
    }

    public WidgetInstalledACRANotification(@Nullable Throwable throwable) {
        super(throwable);
    }
}
