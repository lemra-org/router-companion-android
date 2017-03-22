package org.rm3l.router_companion.common.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by rm3l on 3/17/17.
 */
public final class ViewIDUtils {
  private static final AtomicLong atomicLong = new AtomicLong(0L);
  private static final Map<Class, Map<String, Long>> idsTable =
      Collections.synchronizedMap(new HashMap<Class, Map<String, Long>>());

  private ViewIDUtils() {
  }

  public static long getStableId(final Class<?> type, final String itemUniqueId) {
    synchronized (idsTable) {
      if (!idsTable.containsKey(type)) {
        idsTable.put(type, new HashMap<String, Long>());
      }
      final Map<String, Long> ids = idsTable.get(type);
      if (!ids.containsKey(itemUniqueId)) {
        ids.put(itemUniqueId, atomicLong.incrementAndGet());
      }
      return ids.get(itemUniqueId);
    }
  }
}
