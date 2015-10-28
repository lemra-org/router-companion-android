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

package org.rm3l.ddwrt.mgmt.dao.impl.memory;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.collect.Lists;

import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.ddwrt.resources.conn.Router.RouterConnectionProtocol.HTTPS;
import static org.rm3l.ddwrt.resources.conn.Router.RouterConnectionProtocol.SSH;

/**
 * In Memory Implementation of the Data Access Layer. To use for testing solely!!!
 */
public class DDWRTCompanionInMemoryDAOImpl implements DDWRTCompanionDAO {

    public static final int MAX_INIT_ENTRIES = 35;
    private static final String LOG_TAG = DDWRTCompanionInMemoryDAOImpl.class.getSimpleName();
    private final Map<String, Router> DB = new ConcurrentHashMap<>();

    public DDWRTCompanionInMemoryDAOImpl() {
        if (DDWRTCompanionConstants.TEST_MODE) {
            populateDB();
        }
    }

    private void populateDB() {
        final List<Integer> primeNumbersFromEratostheneSieve = getPrimeNumbersFromEratostheneSieve(MAX_INIT_ENTRIES);

        for (int i = 1; i <= MAX_INIT_ENTRIES; i++) {
            final Router sr = new Router();
            sr.setName("router #" + i);
            sr.setRemoteIpAddress("172.17.17." + i);
            sr.setRouterConnectionProtocol(primeNumbersFromEratostheneSieve.contains(i) ? SSH : HTTPS);
            this.insertRouter(sr);
        }
    }

    @NonNull
    private List<Integer> getPrimeNumbersFromEratostheneSieve(final int up) {
        final List<Integer> excluded = new ArrayList<Integer>();
        for (int i = 2; i <= up; i++) {
            if (excluded.contains(i)) {
                continue;
            }
            for (int j = i + 1; j <= up; j++) {
                if (j % i == 0) {
                    excluded.add(j);
                }
            }
        }

        final List<Integer> primes = new ArrayList<>();
        for (int l = 1; l <= up; l++) {
            if (excluded.contains(l)) {
                continue;
            }
            primes.add(l);
        }

        Log.d(LOG_TAG, "primes: " + primes);

        return primes;
    }

    @Override
    public void destroy() {
        Log.d(LOG_TAG, "close");
    }

    @Override
    public Router insertRouter(@NonNull Router router) {
        Log.d(LOG_TAG, "createRouter(" + router + ")");
        if (isNullOrEmpty(router.getUuid())) {
            router.setUuid(UUID.randomUUID().toString());
        }
        if (DB.containsKey(router.getUuid())) {
            throw new IllegalArgumentException("Conflict!");
        }
        DB.put(router.getUuid(), router);

        return DB.get(router.getUuid());
    }

    @Override
    public Router updateRouter(@NonNull Router router) {
        if (isNullOrEmpty(router.getUuid())) {
            throw new IllegalArgumentException("UUID not specified for update");
        }
        DB.put(router.getUuid(), router);
        return DB.get(router.getUuid());
    }

    @Override
    public void deleteRouter(String uuid) {
        Log.d(LOG_TAG, "deleteRouter(" + uuid + ")");
        DB.remove(uuid);
    }

    @Override
    public List<Router> getAllRouters() {
        Log.d(LOG_TAG, "getAllRouters()");
        return Lists.newArrayList(DB.values());
    }

    @Nullable
    @Override
    public Router getRouter(String uuid) {
        Log.d(LOG_TAG, "getRouter(" + uuid + ")");
        return DB.get(uuid);
    }

    @Nullable
    @Override
    public Router getRouter(int id) {
        for (final Router router : DB.values()) {
            if (router == null) {
                continue;
            }
            if (router.getId() == id) {
                return router;
            }
        }
        return null;
    }
}
