package org.rm3l.router_companion.service.tasks

import android.content.Context
import android.content.SharedPreferences
import com.google.common.base.Objects
import org.rm3l.router_companion.resources.conn.NVRAMInfo
import org.rm3l.router_companion.resources.conn.Router
import org.rm3l.router_companion.utils.SSHUtils
import org.rm3l.router_companion.utils.Utils

import org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_HTTP_PORT
import org.rm3l.router_companion.resources.Encrypted.d
import org.rm3l.router_companion.resources.Encrypted.e
import org.rm3l.router_companion.resources.conn.NVRAMInfo.HTTPS_ENABLE
import org.rm3l.router_companion.resources.conn.NVRAMInfo.HTTP_ENABLE
import org.rm3l.router_companion.resources.conn.NVRAMInfo.HTTP_LANPORT
import org.rm3l.router_companion.resources.conn.NVRAMInfo.HTTP_WANPORT
import org.rm3l.router_companion.resources.conn.NVRAMInfo.REMOTE_MGT_HTTPS

/**
 * Created by rm3l on 30/07/15.
 */
class RouterWebInterfaceParametersUpdaterServiceTask(ctx: Context) : AbstractBackgroundServiceTask(
    ctx) {

  @Throws(Exception::class)
  override fun runBackgroundServiceTask(router: Router) {

    val routerPreferences = mCtx.getSharedPreferences(router.templateUuidOrUuid, Context.MODE_PRIVATE) ?: return

    val nvramInfo = SSHUtils.getNVRamInfoFromRouter(mCtx, router, globalPreferences, HTTP_ENABLE,
        HTTP_LANPORT,
        HTTP_WANPORT, HTTPS_ENABLE, REMOTE_MGT_HTTPS) ?: return

    val httpEnabled = nvramInfo.getProperty(HTTP_ENABLE)
    val httpsEnabled = nvramInfo.getProperty(HTTPS_ENABLE)
    val httpLanPortStr = nvramInfo.getProperty(HTTP_LANPORT, DEFAULT_HTTP_PORT)
    val httpWanPortStr = nvramInfo.getProperty(HTTP_WANPORT, DEFAULT_HTTP_PORT)
    val remoteMgmtHttps = nvramInfo.getProperty(REMOTE_MGT_HTTPS)

    val editor = routerPreferences.edit()

    var changed = false

    val httpEnabledFromPrefs = d(routerPreferences.getString(HTTP_ENABLE, null))
    if (!Objects.equal(httpEnabled, httpEnabledFromPrefs)) {
      editor.putString(HTTP_ENABLE, e(httpEnabled))
      changed = true
    }

    val httpsEnabledFromPrefs = d(routerPreferences.getString(HTTPS_ENABLE, null))
    if (!Objects.equal(httpsEnabled, httpsEnabledFromPrefs)) {
      editor.putString(HTTPS_ENABLE, e(httpsEnabled))
      changed = true
    }

    val remoteMgmtHttpsFromPrefs = d(routerPreferences.getString(REMOTE_MGT_HTTPS, null))
    if (!Objects.equal(remoteMgmtHttps, remoteMgmtHttpsFromPrefs)) {
      editor.putString(REMOTE_MGT_HTTPS, e(remoteMgmtHttps))
      changed = true
    }

    val httpLanPortFromPrefs = d(routerPreferences.getString(HTTP_LANPORT, DEFAULT_HTTP_PORT))
    if (!Objects.equal(httpLanPortStr, httpLanPortFromPrefs)) {
      editor.putString(HTTP_LANPORT, e(httpLanPortStr))
      changed = true
    }

    val httpWanPortFromPrefs = d(routerPreferences.getString(HTTP_WANPORT, DEFAULT_HTTP_PORT))
    if (!Objects.equal(httpWanPortStr, httpWanPortFromPrefs)) {
      editor.putString(HTTP_WANPORT, e(httpWanPortStr))
      changed = true
    }

    if (changed) {
      editor.apply()
      Utils.requestBackup(mCtx)
    }
  }
}
