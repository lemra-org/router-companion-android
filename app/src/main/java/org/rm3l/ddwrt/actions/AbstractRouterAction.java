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
package org.rm3l.ddwrt.actions;

import android.content.SharedPreferences;
import android.os.AsyncTask;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.resources.conn.Router;

/**
 * Abstract Router Action async task
 * @param <T> Type of async task result
 */
public abstract class AbstractRouterAction<T> extends AsyncTask<Router, Void, AbstractRouterAction.AbstractRouterActionResult> {

    @Nullable private final RouterActionListener listener;
    @NotNull private final RouterAction routerAction;
    @NotNull protected final SharedPreferences globalSharedPreferences;

    protected AbstractRouterAction(@Nullable final RouterActionListener listener, @NotNull final RouterAction routerAction,
                                   @NotNull final SharedPreferences globalSharedPreferences) {
        this.listener = listener;
        this.routerAction = routerAction;
        this.globalSharedPreferences = globalSharedPreferences;
    }

    @Override
    protected final AbstractRouterActionResult doInBackground(Router... params) {
        final Router router = params[0];
        AbstractRouterActionResult actionResult = null;
        try {
            actionResult = this.doActionInBackground(router);
        } catch (final Exception e) {
            actionResult = new AbstractRouterActionResult(null, e);
        } finally {
            if (actionResult != null && listener != null) {
                final Exception exception = actionResult.getException();
                if (exception == null) {
                    listener.onRouterActionSuccess(routerAction, router);
                } else {
                    listener.onRouterActionFailure(routerAction, router, exception);
                }
            }
        }
        return actionResult;
    }

    @NotNull
    protected abstract AbstractRouterActionResult doActionInBackground(@NotNull final Router router);

    protected class AbstractRouterActionResult {
        private final T result;
        private final Exception exception;

        protected AbstractRouterActionResult(T result, Exception exception) {
            this.result = result;
            this.exception = exception;
        }

        public T getResult() {
            return result;
        }

        public Exception getException() {
            return exception;
        }
    }
}
