package org.rm3l.router_companion.actions;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.SSHUtils;

/**
 * Created by rm3l on 02/06/15.
 */
public class EraseWANMonthlyTrafficRouterAction extends AbstractRouterAction<Void> {

  @NonNull private final Context mContext;

  public EraseWANMonthlyTrafficRouterAction(Router router, @NonNull Context context,
      @Nullable RouterActionListener listener,
      @NonNull final SharedPreferences globalSharedPreferences) {
    super(router, listener, RouterAction.DELETE_WAN_TRAFF, globalSharedPreferences);
    this.mContext = context;
  }

  @Nullable @Override protected Context getContext() {
    return mContext;
  }

  @NonNull @Override protected RouterActionResult<Void> doActionInBackground() {
    Exception exception = null;
    try {

      final NVRAMInfo nvramInfo =
          SSHUtils.getNVRamInfoFromRouter(mContext, router, globalSharedPreferences, "traff-.*");

      if (nvramInfo == null) {
        throw new IllegalStateException("Failed to fetch WAN Traffic Data from Router");
      }

      final List<String> nvramKeysToUnset = new ArrayList<>();

      @SuppressWarnings("ConstantConditions") final Set<Object> keys = nvramInfo.getData().keySet();

      for (final Object key : keys) {
        if (key == null) {
          continue;
        }

        final String keyToString = key.toString();
        if (!StringUtils.startsWithIgnoreCase(keyToString, "traff-")) {
          continue;
        }

        if (keyToString.length() != 13) {
          continue;
        }

        nvramKeysToUnset.add("nvram unset \"" + keyToString + "\"");
      }

      if (!nvramKeysToUnset.isEmpty()) {

        final int exitStatus = SSHUtils.runCommands(mContext, globalSharedPreferences, router,
            nvramKeysToUnset.toArray(new String[nvramKeysToUnset.size()]));

        if (exitStatus != 0) {
          throw new IllegalStateException();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      exception = e;
    }

    return new RouterActionResult<>(null, exception);
  }
}
