package org.rm3l.ddwrt.help;

import android.support.annotation.NonNull;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.web.WebActivity;

/**
 * Created by rm3l on 04/07/15.
 */
public class ChangelogActivity extends WebActivity {
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
