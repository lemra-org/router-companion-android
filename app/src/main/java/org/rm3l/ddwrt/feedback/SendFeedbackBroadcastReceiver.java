package org.rm3l.ddwrt.feedback;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import org.rm3l.ddwrt.DDWRTApplication;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.utils.Utils;
import org.rm3l.ddwrt.utils.customtabs.CustomTabActivityHelper;

/**
 * Created by rm3l on 22/06/16.
 */
public class SendFeedbackBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = SendFeedbackBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        if (!intent.getBooleanExtra(CustomTabActivityHelper.class.getSimpleName(), false)) {
            //Ignore origin
            return;
        }

        final Activity currentActivity = DDWRTApplication.getCurrentActivity();
        if (currentActivity == null) {
            Toast.makeText(context, "Internal Error - please try again later", Toast.LENGTH_SHORT).show();
            Crashlytics.log(Log.WARN, TAG, "Unable to retrieve current activity");
            return;
        }
        Utils.openFeedbackForm(currentActivity,
                intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED));
    }
}