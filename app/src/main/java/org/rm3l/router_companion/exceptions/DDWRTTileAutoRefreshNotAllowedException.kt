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

package org.rm3l.router_companion.exceptions

import androidx.loader.content.AsyncTaskLoader

/**
 * By default, auto-refresh is enabled on all tiles. But this exception is raised whenever the user
 * explicitly disables a given auto-refresh flag for a given tile.
 * In this case, the [AsyncTaskLoader] still runs but does not do anything upon each run.
 *
 *
 * TODO Find a way to properly stop/reschedule [AsyncTaskLoader]s

 * @author [Armel S.](mailto:apps+ddwrt@rm3l.org)
 */
class DDWRTTileAutoRefreshNotAllowedException : DDWRTCompanionException {

    constructor()

    constructor(detailMessage: String?) : super(detailMessage)

    constructor(
        detailMessage: String?,
        throwable: Throwable?
    ) : super(detailMessage, throwable)

    constructor(throwable: Throwable?) : super(throwable)
}
