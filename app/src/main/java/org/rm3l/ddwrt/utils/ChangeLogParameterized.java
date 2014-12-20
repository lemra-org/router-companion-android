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

package org.rm3l.ddwrt.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.common.base.Strings;

import org.rm3l.ddwrt.BuildConfig;

import de.cketti.library.changelog.ChangeLog;

public class ChangeLogParameterized extends ChangeLog {

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

    public void handlePositiveButtonClick() {
        //From: https://github.com/cketti/ckChangeLog/blob/master/ckChangeLog/src/main/java/de/cketti/library/changelog/ChangeLog.java#L284
        // Action executed on click on PositiveButton: save the current version code as "last version code".
        updateVersionInPreferences();
    }
}
