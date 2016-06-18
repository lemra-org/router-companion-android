package org.rm3l.ddwrt.actions;

import org.rm3l.ddwrt.multithreading.MultiThreadingManager;

import java.util.concurrent.Executor;

/**
 * Created by rm3l on 16/06/16.
 */
public final class ActionManager {

    private ActionManager() {
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
}
