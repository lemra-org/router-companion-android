package org.rm3l.router_companion.exceptions

/**
 * Created by rm3l on 18/01/16.
 */
class SpeedTestException : DDWRTCompanionException {
    constructor()

    constructor(detailMessage: String?) : super(detailMessage)

    constructor(detailMessage: String?, throwable: Throwable?) : super(detailMessage, throwable)

    constructor(throwable: Throwable?) : super(throwable)
}
