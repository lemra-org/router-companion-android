package org.rm3l.router_companion.events.bus;

import android.support.annotation.NonNull;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

/**
 * Created by rm3l on 30/03/16.
 */
public final class BusSingleton {

  private static Bus bus = null;

  private BusSingleton() {
  }

  @NonNull public static Bus getBusInstance() {
    if (bus == null) {
      bus = new Bus(ThreadEnforcer.ANY);
    }
    return bus;
  }
}
