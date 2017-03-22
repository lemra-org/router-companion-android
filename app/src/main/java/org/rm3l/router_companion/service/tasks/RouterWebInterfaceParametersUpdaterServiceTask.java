package org.rm3l.router_companion.service.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import com.google.common.base.Objects;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;

import static org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_HTTP_PORT;
import static org.rm3l.router_companion.resources.Encrypted.d;
import static org.rm3l.router_companion.resources.Encrypted.e;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.HTTPS_ENABLE;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.HTTP_ENABLE;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.HTTP_LANPORT;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.HTTP_WANPORT;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.REMOTE_MGT_HTTPS;

/**
 * Created by rm3l on 30/07/15.
 */
public class RouterWebInterfaceParametersUpdaterServiceTask extends AbstractBackgroundServiceTask {

  public RouterWebInterfaceParametersUpdaterServiceTask(@NonNull Context ctx) {
    super(ctx);
  }

  @Override public void runBackgroundServiceTask(@NonNull Router router) throws Exception {

    final SharedPreferences routerPreferences =
        mCtx.getSharedPreferences(router.getUuid(), Context.MODE_PRIVATE);

    if (routerPreferences == null) {
      return;
    }

    final NVRAMInfo nvramInfo =
        SSHUtils.getNVRamInfoFromRouter(mCtx, router, globalPreferences, HTTP_ENABLE, HTTP_LANPORT,
            HTTP_WANPORT, HTTPS_ENABLE, REMOTE_MGT_HTTPS);

    if (nvramInfo == null) {
      return;
    }

    final String httpEnabled = nvramInfo.getProperty(HTTP_ENABLE);
    final String httpsEnabled = nvramInfo.getProperty(HTTPS_ENABLE);
    final String httpLanPortStr = nvramInfo.getProperty(HTTP_LANPORT, DEFAULT_HTTP_PORT);
    final String httpWanPortStr = nvramInfo.getProperty(HTTP_WANPORT, DEFAULT_HTTP_PORT);
    final String remoteMgmtHttps = nvramInfo.getProperty(REMOTE_MGT_HTTPS);

    final SharedPreferences.Editor editor = routerPreferences.edit();

    boolean changed = false;

    final String httpEnabledFromPrefs = d(routerPreferences.getString(HTTP_ENABLE, null));
    if (!Objects.equal(httpEnabled, httpEnabledFromPrefs)) {
      editor.putString(HTTP_ENABLE, e(httpEnabled));
      changed = true;
    }

    final String httpsEnabledFromPrefs = d(routerPreferences.getString(HTTPS_ENABLE, null));
    if (!Objects.equal(httpsEnabled, httpsEnabledFromPrefs)) {
      editor.putString(HTTPS_ENABLE, e(httpsEnabled));
      changed = true;
    }

    final String remoteMgmtHttpsFromPrefs = d(routerPreferences.getString(REMOTE_MGT_HTTPS, null));
    if (!Objects.equal(remoteMgmtHttps, remoteMgmtHttpsFromPrefs)) {
      editor.putString(REMOTE_MGT_HTTPS, e(remoteMgmtHttps));
      changed = true;
    }

    final String httpLanPortFromPrefs =
        d(routerPreferences.getString(HTTP_LANPORT, DEFAULT_HTTP_PORT));
    if (!Objects.equal(httpLanPortStr, httpLanPortFromPrefs)) {
      editor.putString(HTTP_LANPORT, e(httpLanPortStr));
      changed = true;
    }

    final String httpWanPortFromPrefs =
        d(routerPreferences.getString(HTTP_WANPORT, DEFAULT_HTTP_PORT));
    if (!Objects.equal(httpWanPortStr, httpWanPortFromPrefs)) {
      editor.putString(HTTP_WANPORT, e(httpWanPortStr));
      changed = true;
    }

    if (changed) {
      editor.apply();
      Utils.requestBackup(mCtx);
    }
  }
}
