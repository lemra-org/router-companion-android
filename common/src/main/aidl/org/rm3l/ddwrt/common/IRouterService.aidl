// IRouterService.aidl
package org.rm3l.ddwrt.common;

import org.rm3l.ddwrt.common.resources.RouterInfo;

// Declare any non-default types here with import statements

interface IRouterService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
//    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
//            double aDouble, String aString);

    List<RouterInfo> getAllRouters();

    RouterInfo getRouterByUuid(String routerUuid);

    RouterInfo getRouterById(int routerId);

    List<RouterInfo> lookupRoutersByName(String name);
}
