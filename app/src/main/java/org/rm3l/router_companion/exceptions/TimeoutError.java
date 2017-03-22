package org.rm3l.router_companion.exceptions;

import java.io.IOException;

/**
 * Created by rm3l on 2/10/17.
 */

public class TimeoutError extends IOException {

  public TimeoutError() {
    super();
  }

  public TimeoutError(Throwable cause) {
    super(cause);
  }
}
