package org.rm3l.router_companion.firmwares;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.rm3l.router_companion.resources.conn.Router;

/**
 * Created by rm3l on 08/01/2017.
 */

public interface RouterFirmwareConnector {

    @Nullable
    String getRouterModel(@NonNull Context ctx, @NonNull Router router) throws Exception;
}
