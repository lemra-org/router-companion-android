package org.rm3l.ddwrt.help;

import android.support.annotation.NonNull;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.web.WebActivity;

/**
 * Created by rm3l on 30/05/15.
 */
public class HelpActivity extends WebActivity {

    @Override
    protected CharSequence getTitleStr() {
        return null;
    }

    @Override
    protected int getTitleResId() {
        return R.string.help;
    }

    @NonNull
    @Override
    public String getUrl() {
        return DDWRTCompanionConstants.REMOTE_HELP_WEBSITE;
    }


}