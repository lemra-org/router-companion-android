package org.rm3l.router_companion.service.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.Utils;

import static org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY;

/**
 * Created by rm3l on 05/07/15.
 */
public abstract class AbstractBackgroundServiceTask {

  protected final SharedPreferences globalPreferences;
  @NonNull protected Context mCtx;

  public AbstractBackgroundServiceTask(@NonNull final Context ctx) {
    mCtx = ctx;
    globalPreferences = Utils.getGlobalSharedPreferences(ctx);
  }

  public abstract void runBackgroundServiceTask(@NonNull final Router router) throws Exception;
}
