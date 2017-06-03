package org.rm3l.router_companion.exceptions

import java.io.IOException

/**
 * Created by rm3l on 2/10/17.
 */

class TimeoutError : IOException {

  constructor() : super()

  constructor(cause: Throwable) : super(cause)
}
