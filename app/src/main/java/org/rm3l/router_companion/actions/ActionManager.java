package org.rm3l.router_companion.actions;

import java.util.concurrent.Executor;
import org.rm3l.router_companion.multithreading.MultiThreadingManager;

/** Created by rm3l on 16/06/16. */
public final class ActionManager {

  public static void cancelTasks(final AbstractRouterAction... tasks) {
    if (tasks != null) {
      for (final AbstractRouterAction task : tasks) {
        if (task == null) {
          continue;
        }
        task.cancel();
      }
    }
  }

  public static void runTasks(final AbstractRouterAction... tasks) {
    final Executor actionExecutor = MultiThreadingManager.getActionExecutor();
    if (tasks != null) {
      for (final AbstractRouterAction task : tasks) {
        if (task == null) {
          continue;
        }
        actionExecutor.execute(task);
      }
    }
  }

  private ActionManager() {}
}
