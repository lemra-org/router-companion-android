package org.rm3l.router_companion.deeplinks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.airbnb.deeplinkdispatch.DeepLinkHandler;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

/** Created by rm3l on 13/02/16. */
public class DeepLinkReceiver extends BroadcastReceiver {

  private static final String LOG_TAG = DeepLinkReceiver.class.getSimpleName();

  @Override
  public void onReceive(Context context, Intent intent) {
    final String deepLinkUri = intent.getStringExtra(DeepLinkHandler.EXTRA_URI);

    if (intent.getBooleanExtra(DeepLinkHandler.EXTRA_SUCCESSFUL, false)) {
      FirebaseCrashlytics.getInstance().log("Success with deep linking: " + deepLinkUri);
    } else {
      FirebaseCrashlytics.getInstance()
          .log(
              "Error with deep linking: "
                  + deepLinkUri
                  + " , with error message: "
                  + intent.getStringExtra(DeepLinkHandler.EXTRA_ERROR_MESSAGE));
    }
  }
}
