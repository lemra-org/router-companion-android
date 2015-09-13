package org.rm3l.ddwrt.actions;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.SSHUtils;

/**
 * Created by rm3l on 13/09/15.
 */
public class DHCPClientRouterAction extends AbstractRouterAction<Void> {

    enum DHCPClientAction {
        RELEASE,
        RENEW
    }

    @NonNull
    private final Context mContext;

    private final DHCPClientAction mDhcpClientAction;

    public DHCPClientRouterAction(@NonNull Context context,
                                           @Nullable RouterActionListener listener,
                                           @NonNull final SharedPreferences globalSharedPreferences,
                                           @NonNull final DHCPClientAction dhcpClientAction) {
        super(listener,
                dhcpClientAction == DHCPClientAction.RELEASE ?
                        RouterAction.DHCP_RELEASE : RouterAction.DHCP_RENEW,
                globalSharedPreferences);
        this.mContext = context;
        this.mDhcpClientAction = dhcpClientAction;
    }

    @NonNull
    @Override
    protected RouterActionResult<Void> doActionInBackground(@NonNull final Router router) {

        Exception exception = null;
        try {

            final int exitStatus;

            switch (this.mDhcpClientAction) {
                case RELEASE:
                    exitStatus = SSHUtils
                            .runCommands(mContext, globalSharedPreferences, router,
                                    "kill -USR2 `head -n 1 /var/run/udhcpc.pid` 2>&1",
                                    "killall udhcpc 2>&1 || true");
                    break;
                case RENEW:
                    exitStatus = SSHUtils
                            .runCommands(mContext, globalSharedPreferences, router,
                                    "kill -USR2 `head -n 1 /var/run/udhcpc.pid` 2>&1 || true",
                                    "killall udhcpc 2>&1 || true",
                                    "udhcpc -i `nvram get wan_iface` -p /var/run/udhcpc.pid -s /tmp/udhcpc");
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown DHCP Router Action: " +
                            mDhcpClientAction);
            }

            if (exitStatus != 0) {
                throw new IllegalStateException();
            }

        } catch (Exception e) {
            e.printStackTrace();
            exception = e;
        }

        return new RouterActionResult<>(null, exception);
    }
}
