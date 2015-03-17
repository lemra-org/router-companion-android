package org.rm3l.ddwrt.actions;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.SSHUtils;

public class ArpPingFromRouterAction extends AbstractRouterAction<Void> {

    private static final int MAX_ARPING_PACKETS_TO_SEND = 5;

    @NonNull
    private final Context mContext;
    @NonNull
    private final String mHost;

    public ArpPingFromRouterAction(@NonNull Context context, @Nullable RouterStreamActionListener listener,
                                   @NonNull final SharedPreferences globalSharedPreferences,
                                   @NonNull final String hostToPing) {
        super(listener, RouterAction.ARPING, globalSharedPreferences);
        this.mContext = context;
        this.mHost = hostToPing;
    }

    @NonNull
    @Override
    protected RouterActionResult doActionInBackground(@NonNull Router router) {
        Exception exception = null;
        try {
            //Don't care about the exit status
            SSHUtils.execStreamableCommand(mContext, router, globalSharedPreferences,
                    routerAction,
                    (RouterStreamActionListener) listener,
                    String.format("for ifname in `/sbin/ifconfig | grep -i 'HWaddr' | awk '{print $1}'`; do " +
                            "arping -c %s -I ${ifname} %s; done", MAX_ARPING_PACKETS_TO_SEND, mHost));

        } catch (Exception e) {
            e.printStackTrace();
            exception = e;
        }

        return new RouterActionResult(null, exception);
    }
}
