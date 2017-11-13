package org.rm3l.router_companion.job

import com.crashlytics.android.Crashlytics
import com.evernote.android.job.util.JobLogger

class RouterCompanionJobLogger: JobLogger {
    override fun log(priority: Int, tag: String, message: String, t: Throwable?) {
        if (t == null) {
            Crashlytics.log(priority, tag, message)
        } else {
            Crashlytics.logException(t)
        }
    }
}