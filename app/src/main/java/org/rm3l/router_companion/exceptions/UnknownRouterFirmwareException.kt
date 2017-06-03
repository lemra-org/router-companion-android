package org.rm3l.router_companion.exceptions

/**
 * Created by rm3l on 08/01/2017.
 */

class UnknownRouterFirmwareException : DDWRTCompanionException {

  constructor() : super() 

  constructor(detailMessage: String?) : super(detailMessage) 

  constructor(detailMessage: String?, throwable: Throwable?) : super(detailMessage, throwable)

  constructor(throwable: Throwable?) : super(throwable) 
}
