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

package org.rm3l.router_companion.exceptions;

import android.support.annotation.Nullable;

/**
 * For User-generated reports: generated when user sends a Feedback
 */
public class UserGeneratedReportException extends DDWRTCompanionException {

    public UserGeneratedReportException() {
    }

    public UserGeneratedReportException(@Nullable String detailMessage) {
        super(detailMessage);
    }

    public UserGeneratedReportException(@Nullable String detailMessage, @Nullable Throwable throwable) {
        super(detailMessage, throwable);
    }

    public UserGeneratedReportException(@Nullable Throwable throwable) {
        super(throwable);
    }
}
