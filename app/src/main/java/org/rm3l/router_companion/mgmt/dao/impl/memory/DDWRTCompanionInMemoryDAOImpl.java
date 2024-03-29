/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014-2022  Armel Soro
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
 * Contact Info: Armel Soro <armel+router_companion@rm3l.org>
 */

package org.rm3l.router_companion.mgmt.dao.impl.memory;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.router_companion.resources.conn.Router.RouterConnectionProtocol.HTTPS;
import static org.rm3l.router_companion.resources.conn.Router.RouterConnectionProtocol.SSH;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.rm3l.router_companion.common.resources.audit.ActionLog;
import org.rm3l.router_companion.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.router_companion.resources.SpeedTestResult;
import org.rm3l.router_companion.resources.WANTrafficData;
import org.rm3l.router_companion.resources.conn.Router;

/** In Memory Implementation of the Data Access Layer. To use for testing solely!!! */
public class DDWRTCompanionInMemoryDAOImpl implements DDWRTCompanionDAO {

  public static final int MAX_INIT_ENTRIES = 35;

  private static final String LOG_TAG = DDWRTCompanionInMemoryDAOImpl.class.getSimpleName();

  private final Map<String, Router> DB = new ConcurrentHashMap<>();

  private final Context ctx;

  public DDWRTCompanionInMemoryDAOImpl() {
    ctx = null;
  }

  @Override
  public void clearActionsLogByOrigin(String origin) {}

  @Override
  public void clearActionsLogByRouterByOrigin(String routerUuid, String origin) {}

  @Override
  public void clearActionsLogs() {}

  @Override
  public void deleteAllSpeedTestResultsByRouter(@NonNull String router) {
    // TODO

  }

  @Override
  public void deleteRouter(String uuid) {
    DB.remove(uuid);
  }

  @Override
  public void deleteSpeedTestResultByRouterById(@NonNull String router, long id) {
    // TODO

  }

  @Override
  public void deleteWANTrafficDataByRouter(@NonNull String router) {
    // TODO
  }

  @Override
  public void destroy() {}

  @Override
  public Collection<ActionLog> getActionsByOrigin(String origin) {
    return Collections.emptyList();
  }

  @Override
  public Collection<ActionLog> getActionsByOrigin(
      String origin, String predicate, String groupBy, String having, String orderBy) {
    return Collections.emptyList();
  }

  @Override
  public Collection<ActionLog> getActionsByRouterByOrigin(String routerUuid, String origin) {
    return Collections.emptyList();
  }

  @Override
  public Collection<ActionLog> getActionsByRouterByOrigin(
      String routerUuid,
      String origin,
      String predicate,
      String groupBy,
      String having,
      String orderBy) {
    return Collections.emptyList();
  }

  @Override
  public List<Router> getAllRouters() {
    final List<Router> allRouters = new ArrayList<>();
    for (final Router router : DB.values()) {
      if (!router.isArchived()) {
        allRouters.add(router);
      }
    }
    return allRouters;
  }

  @Override
  public List<Router> getAllRoutersIncludingArchived() {
    return Lists.newArrayList(DB.values());
  }

  @Nullable
  @Override
  public Router getRouter(String uuid) {
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

  @Nullable
  @Override
  public Collection<Router> getRoutersByName(String name) {
    if (name == null) {
      return Collections.emptyList();
    }
    final List<Router> routers = new ArrayList<>();
    for (final Router router : DB.values()) {
      if (name.equals(router.getName())) {
        routers.add(router);
      }
    }
    return routers;
  }

  @NonNull
  @Override
  public List<SpeedTestResult> getSpeedTestResultsByRouter(@NonNull String router) {
    // TODO
    return Collections.emptyList();
  }

  @NonNull
  @Override
  public List<WANTrafficData> getWANTrafficDataByRouterBetweenDates(
      @NonNull String router, @NonNull String dateLower, @NonNull String dateHigher) {
    // TODO
    return Collections.emptyList();
  }

  @NonNull
  @Override
  public List<WANTrafficData> getWANTrafficDataByRouterByDate(
      @NonNull String router, @NonNull String date) {
    // TODO
    return Collections.emptyList();
  }

  @Override
  public Router insertRouter(@NonNull Router router) {
    if (isNullOrEmpty(router.getUuid())) {
      router.setUuid(UUID.randomUUID().toString());
    }
    if (DB.containsKey(router.getUuid())) {
      throw new IllegalArgumentException("Conflict!");
    }
    DB.put(router.getUuid(), router);

    return DB.get(router.getUuid());
  }

  @Nullable
  @Override
  public Long insertSpeedTestResult(@NonNull SpeedTestResult speedTestResult) {
    // TODO
    return null;
  }

  @Override
  public Long insertWANTrafficData(@NonNull WANTrafficData... trafficData) {
    // TODO
    return null;
  }

  @Override
  public boolean isWANTrafficDataPresent(@NonNull String router, @NonNull String date) {
    // TODO
    return false;
  }

  @Override
  public Long recordAction(ActionLog actionLog) {
    return null;
  }

  @Override
  public Router updateRouter(@NonNull Router router) {
    if (isNullOrEmpty(router.getUuid())) {
      throw new IllegalArgumentException("UUID not specified for update");
    }
    DB.put(router.getUuid(), router);
    return DB.get(router.getUuid());
  }

  @NonNull
  private List<Integer> getPrimeNumbersFromEratostheneSieve(final int up) {
    final List<Integer> excluded = new ArrayList<>();
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

    return primes;
  }

  private void populateDB() {
    final List<Integer> primeNumbersFromEratostheneSieve =
        getPrimeNumbersFromEratostheneSieve(MAX_INIT_ENTRIES);

    for (int i = 1; i <= MAX_INIT_ENTRIES; i++) {
      final Router sr = new Router(ctx);
      sr.setName("router #" + i);
      sr.setRemoteIpAddress("172.17.17." + i);
      sr.setRouterConnectionProtocol(primeNumbersFromEratostheneSieve.contains(i) ? SSH : HTTPS);
      this.insertRouter(sr);
    }
  }
}
