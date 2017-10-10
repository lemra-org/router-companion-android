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

package org.rm3l.router_companion.mgmt.dao

import org.rm3l.router_companion.common.resources.audit.ActionLog
import org.rm3l.router_companion.resources.SpeedTestResult
import org.rm3l.router_companion.resources.WANTrafficData
import org.rm3l.router_companion.resources.conn.Router

interface DDWRTCompanionDAO {

    fun destroy()

    fun insertRouter(router: Router): Router?

    fun updateRouter(router: Router): Router?

    fun deleteRouter(uuid: String)

    val allRouters: List<Router>

    val allRoutersIncludingArchived: List<Router>

    fun getRouter(uuid: String): Router?

    fun getRouter(id: Int): Router?

    fun getRoutersByName(name: String): Collection<Router>

    fun insertWANTrafficData(vararg trafficData: WANTrafficData): Long?

    fun isWANTrafficDataPresent(router: String, date: String): Boolean

    fun getWANTrafficDataByRouterByDate(router: String,
                                        date: String): List<WANTrafficData>

    fun getWANTrafficDataByRouterBetweenDates(router: String,
                                              dateLower: String, dateHigher: String): List<WANTrafficData>

    fun deleteWANTrafficDataByRouter(router: String)

    fun insertSpeedTestResult(speedTestResult: SpeedTestResult): Long?

    fun getSpeedTestResultsByRouter(router: String): List<SpeedTestResult>

    fun deleteSpeedTestResultByRouterById(router: String, id: Long)

    fun deleteAllSpeedTestResultsByRouter(router: String)

    /**
     * Audit log of all actions performed via the Plugin
     */
    fun recordAction(actionLog: ActionLog): Long?

    fun getActionsByOrigin(origin: String): Collection<ActionLog>

    fun getActionsByRouterByOrigin(routerUuid: String, origin: String): Collection<ActionLog>

    fun getActionsByOrigin(origin: String, predicate: String, groupBy: String,
                           having: String, orderBy: String): Collection<ActionLog>

    fun getActionsByRouterByOrigin(routerUuid: String, origin: String,
                                   predicate: String, groupBy: String, having: String, orderBy: String): Collection<ActionLog>

    fun clearActionsLogByOrigin(origin: String)

    fun clearActionsLogByRouterByOrigin(routerUuid: String, origin: String)

    fun clearActionsLogs()
}