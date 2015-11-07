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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.exceptions.RouterActionException;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.ReportingUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Abstract Router Action async task
 *
 * @param <T> Type of async task result
 */
public abstract class AbstractRouterAction<T> extends
        AsyncTask<Router, Void, AbstractRouterAction.RouterActionResult<T>> {

    @NonNull
    protected final SharedPreferences globalSharedPreferences;
    @Nullable
    protected final RouterActionListener listener;
    @NonNull
    protected final RouterAction routerAction;

    protected AbstractRouterAction(@Nullable final RouterActionListener listener, @NonNull final RouterAction routerAction,
                                   @NonNull final SharedPreferences globalSharedPreferences) {
        this.listener = listener;
        this.routerAction = routerAction;
        this.globalSharedPreferences = globalSharedPreferences;
    }

    @Override
    protected final RouterActionResult<T> doInBackground(Router... params) {
        final UUID actionUuid = UUID.randomUUID();
        try {
            //To get stats over the number of actions executed
            final Map<String, Object> eventMap = new HashMap<>();
            eventMap.put("Action", routerAction);
            eventMap.put("Executor class", this.getClass().getSimpleName());
            ReportingUtils.reportEvent(ReportingUtils.EVENT_ACTION_TRIGGERED, eventMap);
        } catch (final Exception e) {
            //No worries
        }

        final Router router = params[0];
        RouterActionResult<T> actionResult = null;
        try {
            actionResult = this.doActionInBackground(router);
        } catch (final Exception e) {
            actionResult = new RouterActionResult<>(null, e);
            //Report exception
            ReportingUtils.reportException(null,
                    new RouterActionException("Exception on Action '" + routerAction + "': " +
                        actionUuid,
                    e));
        } finally {
            if (actionResult != null && listener != null) {
                final Exception exception = actionResult.getException();
                try {
                    if (exception == null) {
                        listener.onRouterActionSuccess(routerAction, router, this.getDataToReturnOnSuccess());
                    } else {
                        listener.onRouterActionFailure(routerAction, router, exception);
                    }
                } catch (final Exception listenerException) {
                    listenerException.printStackTrace();
                    //No Worries, but report exception
                    ReportingUtils.reportException(null, new RouterActionException("Listener Exception on Action '"
                            + routerAction + "': " +
                            actionUuid, listenerException));
                }
            }
        }
        return actionResult;
    }

    @Nullable
    protected Object getDataToReturnOnSuccess() {
        return null;
    }

    @NonNull
    protected abstract RouterActionResult<T> doActionInBackground(@NonNull final Router router);

    protected static class RouterActionResult<T> {
        private final T result;
        private final Exception exception;

        protected RouterActionResult(T result, Exception exception) {
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
