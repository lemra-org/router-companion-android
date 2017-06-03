package org.rm3l.router_companion.firmwares

/**
 * Created by rm3l on 08/01/2017.
 */

interface RemoteDataRetrievalListener {

  /**
   * @param progress between 0 and 100
   */
  fun onProgressUpdate(progress: Int)

  fun doRegardlessOfStatus()
}
