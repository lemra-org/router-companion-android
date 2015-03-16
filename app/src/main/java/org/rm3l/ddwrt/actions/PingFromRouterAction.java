package org.rm3l.ddwrt.actions;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.SSHUtils;

public class PingFromRouterAction extends AbstractRouterAction<Void> {

    private static final int MAX_PING_PACKETS_TO_SEND = 5;
    @NonNull
    private final Context mContext;
    @NonNull
    private final String mHostToPing;

    public PingFromRouterAction(@NonNull Context context, @Nullable RouterStreamActionListener listener,
                                @NonNull final SharedPreferences globalSharedPreferences,
                                @NonNull final String hostToPing) {
        super(listener, RouterAction.REBOOT, globalSharedPreferences);
        this.mContext = context;
        this.mHostToPing = hostToPing;
    }

    @NonNull
    @Override
    protected RouterActionResult doActionInBackground(@NonNull Router router) {
        Exception exception = null;
        try {
            final int exitStatus = SSHUtils.execStreamableCommand(mContext, router, globalSharedPreferences,
                    routerAction,
                    (RouterStreamActionListener) listener,
                    String.format("/bin/ping -c %d %s 2>&1", MAX_PING_PACKETS_TO_SEND, mHostToPing));

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
