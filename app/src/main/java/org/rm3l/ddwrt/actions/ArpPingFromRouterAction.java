package org.rm3l.ddwrt.actions;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class ArpPingFromRouterAction extends ExecStreamableCommandRouterAction {

    private static final int MAX_ARPING_PACKETS_TO_SEND = 5;

    public ArpPingFromRouterAction(@NonNull Context context, @Nullable RouterStreamActionListener listener,
                                   @NonNull final SharedPreferences globalSharedPreferences,
                                   @NonNull final String hostToPing) {
        super(RouterAction.ARPING, context, listener, globalSharedPreferences,
                String.format("for ifname in `/sbin/ifconfig | grep -i 'HWaddr' | awk '{print $1}'`; do " +
                        "arping -c %s -I ${ifname} %s; done", MAX_ARPING_PACKETS_TO_SEND, hostToPing));
    }
}
