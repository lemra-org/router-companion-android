package org.rm3l.router_companion.exceptions;

import android.support.annotation.Nullable;

/**
 * Created by rm3l on 10/05/15.
 */
public class RouterActionException extends DDWRTCompanionException {
  public RouterActionException(@Nullable String detailMessage, @Nullable Throwable throwable) {
    super(detailMessage, throwable);
  }
}
