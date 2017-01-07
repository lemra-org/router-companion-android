// IRouterCompanionService.aidl
package org.rm3l.router_companion.common;

import org.rm3l.router_companion.common.resources.RouterInfo;
import org.rm3l.router_companion.common.resources.audit.ActionLog;

interface IRouterCompanionService {

    List<RouterInfo> getAllRouters();

    RouterInfo getRouterByUuid(String routerUuid);

    RouterInfo getRouterById(int routerId);

    List<RouterInfo> lookupRoutersByName(String name);

//    void recordAction(in ActionLog actionLog);

    List<ActionLog> getActionsByOrigin(String origin);

    List<ActionLog> getActionsByOriginWithSqlConstraints(String origin, String predicate,
                                        String groupBy, String having, String orderBy);

    List<ActionLog> getActionsByRouterByOrigin(String routerUuid, String origin);

    List<ActionLog> getActionsByRouterByOriginWithSqlConstraints(String routerUuid, String origin,
                                        String predicate, String groupBy, String having,
                                        String orderBy);

    void clearActionsLogByOrigin(String origin);

    void clearActionsLogByRouterByOrigin(String routerUuid, String origin);
}
