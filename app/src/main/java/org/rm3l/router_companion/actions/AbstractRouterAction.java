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
package org.rm3l.router_companion.actions;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import needle.UiRelatedTask;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.router_companion.common.resources.audit.ActionLog;
import org.rm3l.router_companion.exceptions.RouterActionException;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.ReportingUtils;

/**
 * Abstract Router Action async task
 *
 * @param <T> Type of async task result
 */
public abstract class AbstractRouterAction<T>
        extends UiRelatedTask<AbstractRouterAction.RouterActionResult<T>> {

    /**
     * @param <T> the result type
     */
    public static class RouterActionResult<T> {

        @Nullable
        private final Exception exception;

        @Nullable
        private final T result;

        public RouterActionResult(@Nullable T result, @Nullable Exception exception) {
            this.result = result;
            this.exception = exception;
        }

        public Exception getException() {
            return exception;
        }

        public T getResult() {
            return result;
        }
    }

    @NonNull
    protected final SharedPreferences globalSharedPreferences;

    @Nullable
    protected final RouterActionListener listener;

    protected final Router router;

    @NonNull
    protected final RouterAction routerAction;

    private final UUID actionUuid;

    private String origin;

    private boolean recordActionForAudit;

    protected AbstractRouterAction(@NonNull final Router router,
            @Nullable final RouterActionListener listener, @NonNull final RouterAction routerAction,
            @NonNull final SharedPreferences globalSharedPreferences) {
        this.actionUuid = UUID.randomUUID();
        this.router = router;
        this.listener = listener;
        this.routerAction = routerAction;
        this.globalSharedPreferences = globalSharedPreferences;
        this.recordActionForAudit = true;
    }

    public final AbstractRouterAction setOrigin(String origin) {
        this.origin = origin;
        return this;
    }

    public AbstractRouterAction setRecordActionForAudit(boolean recordActionForAudit) {
        this.recordActionForAudit = recordActionForAudit;
        return this;
    }

    @NonNull
    protected abstract RouterActionResult<T> doActionInBackground();

    @Override
    protected final RouterActionResult<T> doWork() {

        final Date actionDate = new Date();

        try {
            //To get stats over the number of actions executed
            final Map<String, Object> eventMap = new HashMap<>();
            eventMap.put("Action", routerAction);
            eventMap.put("Executor class", this.getClass().getSimpleName());
            ReportingUtils.reportEvent(ReportingUtils.EVENT_ACTION_TRIGGERED, eventMap);
        } catch (final Exception e) {
            //No worries
        }

        RouterActionResult<T> actionResult = null;
        try {
            actionResult = this.doActionInBackground();
        } catch (final Exception e) {
            actionResult = new RouterActionResult<>(null, e);
            //Report exception
            ReportingUtils.reportException(null,
                    new RouterActionException("Exception on Action '" + routerAction + "': " + actionUuid,
                            e));
        } finally {
            if (recordActionForAudit) {
                final Context context = getContext();
                if (context != null) {
                    final ActionLog actionLog = getActionLog();
                    if (actionLog != null) {
                        actionLog.setOriginPackageName(
                                TextUtils.isEmpty(this.origin) ? BuildConfig.APPLICATION_ID : this.origin);
                        actionLog.setDate(DateFormat.getDateTimeInstance().format(actionDate));
                        actionLog.setUuid(this.actionUuid.toString());
                        actionLog.setRouter(router.getUuid());
                        actionLog.setStatus(
                                actionResult == null || actionResult.getException() == null ? 0 : -1);

                        //Record action
                        RouterManagementActivity.Companion.getDao(context).recordAction(actionLog);
                    }
                }
            }
        }
        return actionResult;
    }

    protected ActionLog getActionLog() {
        return new ActionLog().setActionName(routerAction.toString());
    }

    @Nullable
    protected Context getContext() {
        return null;
    }

    @Nullable
    protected Object getDataToReturnOnSuccess() {
        return null;
    }

    @Override
    protected final void thenDoUiRelatedWork(RouterActionResult<T> actionResult) {
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
                ReportingUtils.reportException(null, new RouterActionException(
                        "Listener Exception on Action '" + routerAction + "': " + actionUuid,
                        listenerException));
            }
        }
    }
}
