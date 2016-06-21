package org.rm3l.ddwrt.feedback;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.utils.Utils;

/**
 * Created by rm3l on 22/06/16.
 */
public class SendFeedbackBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Utils.openFeedbackForm((Activity) context,
                intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED));
    }
}
