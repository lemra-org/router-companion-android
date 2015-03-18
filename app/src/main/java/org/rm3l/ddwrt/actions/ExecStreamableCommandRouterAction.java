package org.rm3l.ddwrt.actions;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Strings;

import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.SSHUtils;

public class ExecStreamableCommandRouterAction extends AbstractRouterAction<Void> {

    @NonNull
    private final Context mContext;
    @NonNull
    private final String mCmd;

    protected ExecStreamableCommandRouterAction(@NonNull final RouterAction routerAction, @NonNull Context context, @Nullable RouterStreamActionListener listener,
                                                @NonNull final SharedPreferences globalSharedPreferences,
                                                @NonNull final String cmd) {
        super(listener, routerAction, globalSharedPreferences);
        this.mContext = context;
        this.mCmd = cmd;
    }

    public ExecStreamableCommandRouterAction(@NonNull Context context, @Nullable RouterStreamActionListener listener,
                                             @NonNull final SharedPreferences globalSharedPreferences,
                                             @NonNull final String cmd) {
        this(RouterAction.CMD_SHELL, context, listener, globalSharedPreferences, cmd);
    }

    @NonNull
    @Override
    protected final RouterActionResult doActionInBackground(@NonNull Router router) {
        Exception exception = null;
        try {
            final int exitStatus = SSHUtils.execStreamableCommand(mContext, router, globalSharedPreferences,
                    routerAction,
                    (RouterStreamActionListener) listener,
                    Strings.nullToEmpty(mCmd).replace("\n", ";"));

            if (exitStatus != 0) {
                throw new IllegalStateException("Command execution status: " + exitStatus);
            }

        } catch (Exception e) {
            e.printStackTrace();
            exception = e;
        }

        return new RouterActionResult(null, exception);
    }

}
