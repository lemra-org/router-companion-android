package org.rm3l.ddwrt.help;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.View;

import com.madx.updatechecker.lib.UpdateRunnable;

import org.apache.commons.lang3.StringUtils;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.web.WebActivity;

import static org.rm3l.ddwrt.BuildConfig.FLAVOR;

/**
 * Created by rm3l on 04/07/15.
 */
public class ChangelogActivity extends WebActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        final View checkForUpdatesButton = findViewById(R.id.button_check_updates);
        if (checkForUpdatesButton != null &&
                StringUtils.startsWithIgnoreCase(FLAVOR, "google")) {
            //This library currently supports Google Play only
            checkForUpdatesButton.setVisibility(View.VISIBLE);
            checkForUpdatesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    /* Use this if an update check is explicitly requested by a user action */
                    new UpdateRunnable(ChangelogActivity.this, new Handler())
                            .force(true)
                            .start();
                }
            });

        } else {
            if (checkForUpdatesButton != null) {
                checkForUpdatesButton.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected CharSequence getTitleStr() {
        return null;
    }

    @Override
    protected int getTitleResId() {
        return R.string.what_s_new;
    }

    @NonNull
    @Override
    protected String getUrl() {
        return DDWRTCompanionConstants.REMOTE_HELP_WEBSITE_CHANGELOG;
    }
}
