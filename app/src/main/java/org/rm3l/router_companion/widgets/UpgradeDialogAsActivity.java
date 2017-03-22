package org.rm3l.router_companion.widgets;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;

import static org.rm3l.router_companion.RouterCompanionAppConstants.AD_FREE_APP_APPLICATION_ID;

/**
 * Created by rm3l on 01/05/15.
 */
public class UpgradeDialogAsActivity extends ConfirmDialogAsActivity {

  @Override protected void onCreate(Bundle savedInstanceState) {

    final Intent intent = getIntent();

    intent.putExtra(TITLE, "Upgrade");
    intent.putExtra(MESSAGE,
        "Unlock this feature by upgrading to the full-featured version "
            + (BuildConfig.WITH_ADS ? " (ad-free)" : "")
            + " on Google Play Store. \n\n"
            + "Thank you for supporting this initiative!");

    super.onCreate(savedInstanceState);

    ((Button) findViewById(R.id.confirm_dialog_as_activity_yes_button)).setText("Upgrade!");
  }

  @Override protected View.OnClickListener getYesButtonOnClickListener() {
    return new View.OnClickListener() {
      @Override public void onClick(View view) {
        String url;
        try {
          //Check whether Google Play store is installed or not:
          getPackageManager().getPackageInfo("com.android.vending", 0);
          url = "market://details?id=" + AD_FREE_APP_APPLICATION_ID;
        } catch (final Exception e) {
          url = "https://play.google.com/store/apps/details?id=" + AD_FREE_APP_APPLICATION_ID;
        }

        //Open the app page in Google Play store:
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
          intent.addFlags(
              Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        } else {
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        }
        startActivity(intent);
      }
    };
  }

  @Override protected View.OnClickListener getNoButtonOnClickListener() {
    return null;
  }
}
