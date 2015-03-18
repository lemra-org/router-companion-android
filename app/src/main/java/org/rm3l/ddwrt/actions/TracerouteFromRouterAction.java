package org.rm3l.ddwrt.actions;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class TracerouteFromRouterAction extends ExecStreamableCommandRouterAction {

    public TracerouteFromRouterAction(@NonNull Context context, @Nullable RouterStreamActionListener listener,
                                      @NonNull final SharedPreferences globalSharedPreferences,
                                      @NonNull final String host) {
        super(RouterAction.TRACEROUTE, context, listener, globalSharedPreferences,
                String.format("/usr/bin/traceroute -l -v %s 2>&1", host));
    }
}
