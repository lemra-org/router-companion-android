package org.rm3l.ddwrt.actions;

import android.support.annotation.NonNull;

import org.rm3l.ddwrt.multithreading.MultiThreadingManager;

/**
 * Created by rm3l on 16/06/16.
 */
public final class ActionManager {

    private ActionManager() {
    }

    public static void runTask(@NonNull final AbstractRouterAction task) {
        MultiThreadingManager.getActionExecutor().execute(task);
    }

    public static void cancelTask(@NonNull final AbstractRouterAction task) {
        task.cancel();
    }
}
