package org.rm3l.router_companion.help;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import org.apache.commons.lang3.StringUtils;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.web.WebActivity;

import static org.rm3l.ddwrt.BuildConfig.FLAVOR;

//import com.madx.updatechecker.lib.UpdateRunnable;

/**
 * Created by rm3l on 04/07/15.
 */
public class ChangelogActivity extends WebActivity {

  @Override protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    final View checkForUpdatesButton = findViewById(R.id.button_check_updates);
    if (checkForUpdatesButton != null && StringUtils.startsWithIgnoreCase(FLAVOR, "google")) {
      //This library currently supports Google Play only
      checkForUpdatesButton.setVisibility(View.GONE);
      //            checkForUpdatesButton.setOnClickListener(new View.OnClickListener() {
      //                @Override
      //                public void onClick(View v) {
      //                    /* Use this if an update check is explicitly requested by a user action */
      //                    new UpdateRunnable(ChangelogActivity.this, new Handler())
      //                            .force(true)
      //                            .start();
      //                }
      //            });

    } else {
      if (checkForUpdatesButton != null) {
        checkForUpdatesButton.setVisibility(View.GONE);
      }
    }
  }

  @Override protected CharSequence getTitleStr() {
    return null;
  }

  @Override protected int getTitleResId() {
    return R.string.what_s_new;
  }

  @NonNull @Override public String getUrl() {
    return RouterCompanionAppConstants.REMOTE_HELP_WEBSITE_CHANGELOG;
  }
}
