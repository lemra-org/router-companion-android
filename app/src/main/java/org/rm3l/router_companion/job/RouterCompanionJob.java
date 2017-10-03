package org.rm3l.router_companion.job;

import android.content.Context;
import com.evernote.android.job.Job;

public abstract class RouterCompanionJob extends Job {
  protected final Context mContext;

  protected RouterCompanionJob(Context mContext) {
    this.mContext = mContext;
  }
}
