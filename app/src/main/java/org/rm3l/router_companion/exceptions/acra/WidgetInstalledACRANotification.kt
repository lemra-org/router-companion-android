package org.rm3l.router_companion.exceptions.acra

import org.rm3l.router_companion.exceptions.DDWRTCompanionException

/**
 * Created by rm3l on 01/05/15.
 */
class WidgetInstalledACRANotification : DDWRTCompanionException {

    constructor() : super()

    constructor(detailMessage: String?) : super(detailMessage)

    constructor(detailMessage: String?, throwable: Throwable?) : super(detailMessage, throwable)

    constructor(throwable: Throwable?) : super(throwable)
}
