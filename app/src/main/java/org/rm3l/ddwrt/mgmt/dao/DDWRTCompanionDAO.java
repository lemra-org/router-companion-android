/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014  Armel Soro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact Info: Armel Soro <apps+ddwrt@rm3l.org>
 */

package org.rm3l.ddwrt.mgmt.dao;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.common.resources.audit.ActionLog;
import org.rm3l.ddwrt.resources.SpeedTestResult;
import org.rm3l.ddwrt.resources.WANTrafficData;
import org.rm3l.ddwrt.resources.conn.Router;

import java.util.Collection;
import java.util.List;

public interface DDWRTCompanionDAO {

    void destroy();

    @Nullable
    Router insertRouter(Router router);

    @Nullable
    Router updateRouter(Router router);

    void deleteRouter(String uuid);

    List<Router> getAllRouters();

    List<Router> getAllRoutersIncludingArchived();

    @Nullable
    Router getRouter(String uuid);

    @Nullable
    Router getRouter(int id);

    @NonNull
    Collection<Router> getRoutersByName(String name);

    @Nullable
    Long insertWANTrafficData(@NonNull final WANTrafficData... trafficData);

    boolean isWANTrafficDataPresent(@NonNull final String router,
                                    @NonNull final String date);

    @NonNull
    List<WANTrafficData> getWANTrafficDataByRouterByDate(@NonNull final String router,
                                                         @NonNull final String date);

    @NonNull
    List<WANTrafficData> getWANTrafficDataByRouterBetweenDates(@NonNull final String router,
                                                                @NonNull final String dateLower,
                                                               @NonNull final String dateHigher);

    void deleteWANTrafficDataByRouter(@NonNull final String router);

    @Nullable
    Long insertSpeedTestResult(@NonNull final SpeedTestResult speedTestResult);

    @NonNull
    List<SpeedTestResult> getSpeedTestResultsByRouter(@NonNull final String router);

    void deleteSpeedTestResultByRouterById(@NonNull final String router, final long id);

    void deleteAllSpeedTestResultsByRouter(@NonNull final String router);

    /**
     * Audit log of all actions performed via the Plugin
     */
    Long recordAction(ActionLog actionLog);

    Collection<ActionLog> getActionsByOrigin(String origin);

    Collection<ActionLog> getActionsByRouterByOrigin(String routerUuid, String origin);

    Collection<ActionLog> getActionsByOrigin(String origin, String predicate, String groupBy,
                                             String having, String orderBy);

    Collection<ActionLog> getActionsByRouterByOrigin(String routerUuid, String origin,
                                                     String predicate, String groupBy,
                                                     String having, String orderBy);

    void clearActionsLogByOrigin(String origin);

    void clearActionsLogByRouterByOrigin(String routerUuid, String origin);

    void clearActionsLogs();

}