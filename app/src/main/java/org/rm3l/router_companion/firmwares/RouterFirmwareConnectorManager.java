package org.rm3l.router_companion.firmwares;

import android.support.annotation.NonNull;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.rm3l.router_companion.firmwares.impl.ddwrt.DDWRTFirmwareConnector;
import org.rm3l.router_companion.firmwares.impl.demo.DemoFirmwareConnector;
import org.rm3l.router_companion.firmwares.impl.tomato.TomatoFirmwareConnector;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.resources.conn.Router.RouterFirmware;

/**
 * Created by rm3l on 08/01/2017.
 */
public final class RouterFirmwareConnectorManager {

    private static final LoadingCache<RouterFirmware, AbstractRouterFirmwareConnector> CONNECTORS =
            CacheBuilder.newBuilder()
                    .maximumSize(RouterFirmware.values().length)
                    .build(new CacheLoader<RouterFirmware, AbstractRouterFirmwareConnector>() {
                        @Override
                        public AbstractRouterFirmwareConnector load(@NonNull RouterFirmware firmware)
                                throws Exception {
                            switch (firmware) {
                                case DEMO:
                                    return new DemoFirmwareConnector();

                                case TOMATO:
                                    return new TomatoFirmwareConnector();

                                case OPENWRT:
                                    //TODO
                                    throw new UnsupportedOperationException("OpenWRT Not Supported Yet");

                                case AUTO:
                                case UNKNOWN:
                                case DDWRT:
                                default:
                                    return new DDWRTFirmwareConnector();
                            }
                        }
                    });

    public static AbstractRouterFirmwareConnector getConnector(@NonNull final Router router) {
        final RouterFirmware routerFirmware = router.getRouterFirmware();
        if (routerFirmware == null) {
            throw new IllegalArgumentException("routerFirmware is NULL");
        }
        return getConnector(routerFirmware);
    }

    public static AbstractRouterFirmwareConnector getConnector(
            @NonNull final RouterFirmware routerFirmware) {
        return CONNECTORS.getUnchecked(routerFirmware);
    }

    public static AbstractRouterFirmwareConnector getConnector(@NonNull final String routerFirmware) {
        return getConnector(RouterFirmware.valueOf(routerFirmware));
    }

    private RouterFirmwareConnectorManager() {
    }
}
