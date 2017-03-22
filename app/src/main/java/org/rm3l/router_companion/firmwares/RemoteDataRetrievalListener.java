package org.rm3l.router_companion.firmwares;

/**
 * Created by rm3l on 08/01/2017.
 */

public interface RemoteDataRetrievalListener {

  /**
   * @param progress between 0 and 100
   */
  void onProgressUpdate(int progress);

  void doRegardlessOfStatus();
}
