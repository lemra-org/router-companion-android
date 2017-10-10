package org.rm3l.router_companion.job;

import android.content.Context;
import android.support.annotation.NonNull;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;
import org.rm3l.router_companion.job.firmware_update.FirmwareUpdateCheckerJob;

public class RouterCompanionJobCreator implements JobCreator {

    private final Context mContext;

    public RouterCompanionJobCreator(Context context) {
        this.mContext = context;
    }

    @Override
    public Job create(@NonNull String tag) {
        final Job job;
        if (FirmwareUpdateCheckerJob.TAG.equals(tag)) {
            job = new FirmwareUpdateCheckerJob(this.mContext);
        } else {
            job = null;
        }
        return job;
    }
}
