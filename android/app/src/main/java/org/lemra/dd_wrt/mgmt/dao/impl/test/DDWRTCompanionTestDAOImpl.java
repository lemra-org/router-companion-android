/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Armel S.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.lemra.dd_wrt.mgmt.dao.impl.test;

import android.util.Log;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lemra.dd_wrt.api.conn.Router;
import org.lemra.dd_wrt.mgmt.dao.DDWRTCompanionDAO;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.lemra.dd_wrt.api.conn.Router.RouterConnectionProtocol.HTTPS;
import static org.lemra.dd_wrt.api.conn.Router.RouterConnectionProtocol.SSH;

public class DDWRTCompanionTestDAOImpl implements DDWRTCompanionDAO {

    public static final int MAX_INIT_ENTRIES = 35;
    private static final String LOG_TAG = DDWRTCompanionTestDAOImpl.class.getSimpleName();
    private final Map<String, Router> DB = Maps.newHashMap();

    public DDWRTCompanionTestDAOImpl() {
        populateDB();
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

    @NotNull
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

        final List<Integer> primes = new ArrayList<Integer>();
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
    public void open() throws SQLException {
        Log.d(LOG_TAG, "open");
    }

    @Override
    public void close() {
        Log.d(LOG_TAG, "close");
    }

    @Override
    public Router insertRouter(Router router) {
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
    public Router updateRouter(Router router) {
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
}
