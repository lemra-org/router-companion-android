package org.rm3l.router_companion.common.utils;

import androidx.annotation.NonNull;

/** Created by rm3l on 25/09/17. */
public final class ExceptionUtils {

  public static Throwable getRootCause(@NonNull final Throwable throwable) {
    Throwable cause;
    Throwable result = throwable;
    while (null != (cause = result.getCause()) && (result != cause)) {
      result = cause;
    }
    return result;
  }
}
