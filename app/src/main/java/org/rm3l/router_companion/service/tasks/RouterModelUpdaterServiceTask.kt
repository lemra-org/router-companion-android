package org.rm3l.router_companion.service.tasks

import android.content.Context
import org.rm3l.router_companion.resources.conn.NVRAMInfo
import org.rm3l.router_companion.resources.conn.Router
import org.rm3l.router_companion.utils.SSHUtils
import org.rm3l.router_companion.utils.Utils

/**
 * Created by rm3l on 30/07/15.
 */
class RouterModelUpdaterServiceTask(ctx: Context) : AbstractBackgroundServiceTask(ctx) {

  @Throws(Exception::class)
  override fun runBackgroundServiceTask(router: Router) {

    val routerPreferences = mCtx.getSharedPreferences(router.templateUuidOrUuid, Context.MODE_PRIVATE) ?: return

    val nvramInfo = SSHUtils.getNVRamInfoFromRouter(mCtx, router, globalPreferences,
        NVRAMInfo.MODEL) ?: return

    val routerModel = nvramInfo.getProperty(NVRAMInfo.MODEL, DEFAULT_VALUE)

    val routerModelFromPrefs = routerPreferences.getString(NVRAMInfo.MODEL, DEFAULT_VALUE)

    if (!(DEFAULT_VALUE == routerModel || routerModelFromPrefs == routerModel)) {
      routerPreferences.edit().putString(NVRAMInfo.MODEL, routerModel).apply()
      Utils.requestBackup(mCtx)
    }
  }

  companion object {
    val DEFAULT_VALUE = "-"
  }
}
