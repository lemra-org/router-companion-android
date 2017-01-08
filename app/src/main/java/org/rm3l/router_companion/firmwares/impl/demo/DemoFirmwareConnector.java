package org.rm3l.router_companion.firmwares.impl.demo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.rm3l.router_companion.firmwares.AbstractRouterFirmwareConnector;
import org.rm3l.router_companion.resources.conn.Router;

/**
 * Created by rm3l on 08/01/2017.
 */

public class DemoFirmwareConnector extends AbstractRouterFirmwareConnector {

    @Override
    @Nullable
    public String goGetRouterModel(@NonNull Context context, @NonNull Router router) throws Exception {
        return "Demo Router Model";
    }
}
