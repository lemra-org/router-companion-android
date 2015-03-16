package org.rm3l.ddwrt.actions;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.SSHUtils;

public class TracerouteFromRouterAction extends AbstractRouterAction<Void> {

    @NonNull
    private final Context mContext;
    @NonNull
    private final String mHost;

    public TracerouteFromRouterAction(@NonNull Context context, @Nullable RouterStreamActionListener listener,
                                      @NonNull final SharedPreferences globalSharedPreferences,
                                      @NonNull final String hostToPing) {
        super(listener, RouterAction.TRACEROUTE, globalSharedPreferences);
        this.mContext = context;
        this.mHost = hostToPing;
    }

    @NonNull
    @Override
    protected RouterActionResult doActionInBackground(@NonNull Router router) {
        Exception exception = null;
        try {
            final int exitStatus = SSHUtils.execStreamableCommand(mContext, router, globalSharedPreferences,
                    routerAction,
                    (RouterStreamActionListener) listener,
                    String.format("/usr/bin/traceroute -l -v %s 2>&1", mHost));

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
