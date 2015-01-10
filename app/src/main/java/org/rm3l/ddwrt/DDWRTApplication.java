/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014  Armel Soro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact Info: Armel Soro <apps+ddwrt@rm3l.org>
 */

package org.rm3l.ddwrt;

import android.app.Application;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;

/**
 * App Main Entry point.
 * Leverages ACRA for capturing eventual app crashes and sending the relevant metrics for further analysis.
 */
@ReportsCrashes(
        formKey = \"fake-key\";
        formUri = DDWRTCompanionConstants.ACRA_BACKEND_URL,
//        mailTo = "apps+ddwrt@rm3l.org",
        mode = ReportingInteractionMode.SILENT
//        mode = ReportingInteractionMode.DIALOG,
//        resDialogTitle = R.string.app_name,
//        resDialogIcon = R.drawable.ic_action_alert_warning,
//        resDialogText = R.string.crash_toast_text,
//        resDialogOkToast = R.string.crash_ok_dialog_text
)
public class DDWRTApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // The following line triggers the initialization of ACRA
        ACRA.init(this);

        ACRA.getErrorReporter().putCustomData("TRACEPOT_DEVELOP_MODE",
                BuildConfig.DEBUG ? "1" : "0");
    }
}