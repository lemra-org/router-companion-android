package org.rm3l.router_companion.job;

import android.content.Context;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;
import org.rm3l.router_companion.service.firebase.DDWRTCompanionFirebaseMessagingHandlerJob;

public class RouterCompanionJobCreator implements JobCreator {

  private final Context mContext;

  public RouterCompanionJobCreator(Context context) {
    this.mContext = context;
  }

  @Override public Job create(String tag) {
    final RouterCompanionJob job;
    if (DDWRTCompanionFirebaseMessagingHandlerJob.TAG.equals(tag)) {
      job = new DDWRTCompanionFirebaseMessagingHandlerJob(this.mContext);
    } else {
      job = null;
    }
    return job;
  }
}
