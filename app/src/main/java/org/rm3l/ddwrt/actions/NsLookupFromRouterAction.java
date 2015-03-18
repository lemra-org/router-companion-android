package org.rm3l.ddwrt.actions;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class NsLookupFromRouterAction extends ExecStreamableCommandRouterAction {

    public NsLookupFromRouterAction(@NonNull Context context, @Nullable RouterStreamActionListener listener,
                                    @NonNull final SharedPreferences globalSharedPreferences,
                                    @NonNull final String host) {
        super(RouterAction.NSLOOKUP, context, listener, globalSharedPreferences,
                String.format("/usr/bin/nslookup %s 2>&1", host));
    }

}
