package org.rm3l.ddwrt.actions;

import android.support.annotation.NonNull;

import org.rm3l.ddwrt.resources.conn.Router;

public interface RouterStreamActionListener extends RouterActionListener {

    public void notifyRouterActionProgress(@NonNull final RouterAction routerAction,
                                           @NonNull final Router router,
                                           final int progress,
                                           String partialOutput);

}
