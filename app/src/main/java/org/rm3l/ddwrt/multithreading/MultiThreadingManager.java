package org.rm3l.ddwrt.multithreading;

import java.util.concurrent.Executor;

import needle.Needle;

/**
 * Created by rm3l on 15/06/16.
 */
public final class MultiThreadingManager {

    private static final String FEEDBACKS_TASK_TYPE = "Feedback";

    private MultiThreadingManager() {
    }

    public static Executor getFeedbackExecutor() {
        return Needle
                .onBackgroundThread()
                .withTaskType("Feedback")
                //Has to be executed serially, one thread at a time
                .serially();
    }

    public static Executor getActionExecutor() {
        return Needle
                .onBackgroundThread()
                .withTaskType("Action")
                .withThreadPoolSize(3);
    }
}
