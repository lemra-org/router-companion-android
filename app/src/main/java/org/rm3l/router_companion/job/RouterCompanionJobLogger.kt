package org.rm3l.router_companion.job

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.evernote.android.job.util.JobLogger

class RouterCompanionJobLogger : JobLogger {
    override fun log(priority: Int, tag: String, message: String, t: Throwable?) {
        if (t == null) {
            FirebaseCrashlytics.getInstance().log(message)
        } else {
            FirebaseCrashlytics.getInstance().recordException(t)
        }
    }
}