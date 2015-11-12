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

import org.rm3l.ddwrt.resources.WANTrafficData;
import org.rm3l.ddwrt.resources.conn.Router;

import java.util.List;

public interface DDWRTCompanionDAO {

    void destroy();

    @Nullable
    Router insertRouter(Router router);

    @Nullable
    Router updateRouter(Router router);

    void deleteRouter(String uuid);

    List<Router> getAllRouters();

    @Nullable
    Router getRouter(String uuid);

    @Nullable
    Router getRouter(int id);

    @Nullable
    Long insertWANTrafficData(@NonNull final WANTrafficData trafficData);

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

}
