package org.rm3l.ddwrt.multithreading;

import java.util.concurrent.Executor;

import needle.Needle;

/**
 * Created by rm3l on 15/06/16.
 */
public final class MultiThreadingManager {

    private static final String FEEDBACK_TASK_TYPE = "Feedback";
    private static final String ACTION_TASK_TYPE = "Action";
    private static final String MISC_TASK_TYPE = "Misc";

    private MultiThreadingManager() {
    }

    public static Executor getFeedbackExecutor() {
        return Needle
                .onBackgroundThread()
                .withTaskType(FEEDBACK_TASK_TYPE)
                //Has to be executed serially, one thread at a time
                .serially();
    }

    public static Executor getActionExecutor() {
        return Needle
                .onBackgroundThread()
                .withTaskType(ACTION_TASK_TYPE)
                .withThreadPoolSize(3);
    }

    public static Executor getMiscTasksExecutor() {
        return Needle
                .onBackgroundThread()
                .withTaskType(MISC_TASK_TYPE)
                .serially();
    }

}