package org.rm3l.ddwrt.actions;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class PingFromRouterAction extends ExecStreamableCommandRouterAction {

    private static final int MAX_PING_PACKETS_TO_SEND = 5;

    public PingFromRouterAction(@NonNull Context context, @Nullable RouterStreamActionListener listener,
                                @NonNull final SharedPreferences globalSharedPreferences,
                                @NonNull final String hostToPing) {
        super(RouterAction.PING, context, listener, globalSharedPreferences,
                String.format("/bin/ping -c %d %s 2>&1", MAX_PING_PACKETS_TO_SEND, hostToPing));
    }

}
