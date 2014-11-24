/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Armel S.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.rm3l.ddwrt;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.common.base.Strings;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import de.cketti.library.changelog.ChangeLog;

/**
 * App Main Entry point.
 * Leverages ACRA for capturing eventual app crashes and sending the relevant metrics for further analysis.
 */
@ReportsCrashes(
        formKey = "", //Won't be used
        mailTo = "apps+ddwrt@rm3l.org",
        mode = ReportingInteractionMode.DIALOG,
        resDialogTitle = R.string.app_name,
        resDialogIcon = R.drawable.ic_action_alert_warning,
        resDialogText = R.string.crash_toast_text,
        resDialogOkToast = R.string.crash_ok_dialog_text
)
public class DDWRTApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // The following line triggers the initialization of ACRA
        ACRA.init(this);

        //Changelog Popup
        final ChangeLogParameterized cl = new ChangeLogParameterized(this);
        if (cl.isFirstRun()) {
            cl.getLogDialog().show();
        }
    }
}

class ChangeLogParameterized extends ChangeLog {

    public ChangeLogParameterized(Context context) {
        super(context);
    }

    public ChangeLogParameterized(Context context, String css) {
        super(context, css);
    }

    public ChangeLogParameterized(Context context, SharedPreferences preferences, String css) {
        super(context, preferences, css);
    }

    @Override
    protected String getLog(boolean full) {
        final String log = super.getLog(full);
        if (Strings.isNullOrEmpty(log)) {
            return log;
        }
        return log.replaceAll("%CURRENT_VERSION_CODE%", String.valueOf(BuildConfig.VERSION_CODE));
    }
}
