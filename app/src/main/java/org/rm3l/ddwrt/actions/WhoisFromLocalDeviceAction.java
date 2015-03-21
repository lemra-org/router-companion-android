package org.rm3l.ddwrt.actions;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.resources.IPWhoisInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.status.wireless.ActiveIPConnectionsDetailActivity;

public class WhoisFromLocalDeviceAction extends AbstractRouterAction<Void> {

    @NonNull
    private final String mHostOrIp;

    public WhoisFromLocalDeviceAction(@NonNull Context context, @Nullable RouterActionListener listener,
                                      @NonNull final SharedPreferences globalSharedPreferences, @NonNull final String hostOrIp) {
        super(listener, RouterAction.WHOIS, globalSharedPreferences);
        this.mHostOrIp = hostOrIp;
    }

    @NonNull
    @Override
    protected RouterActionResult doActionInBackground(@NonNull Router router) {
        final RouterStreamActionListener routerStreamActionListener = (listener instanceof RouterStreamActionListener) ?
                (RouterStreamActionListener) listener : null;
        Exception exception = null;
        try {
            if (routerStreamActionListener != null) {
                routerStreamActionListener.notifyRouterActionProgress(RouterAction.WHOIS, router, 0, null);
            }
            final IPWhoisInfo ipWhoisInfo = ActiveIPConnectionsDetailActivity.mIPWhoisInfoCache.get(mHostOrIp);
            if (ipWhoisInfo == null) {
                throw new IllegalArgumentException("Failed to fetch WHOIS info - check your input or connectivity!");
            }
            if (routerStreamActionListener != null) {
                routerStreamActionListener.notifyRouterActionProgress(RouterAction.WHOIS, router, 100, ipWhoisInfo
                        .toCommandOutputString());
            }

        } catch (Exception e) {
            e.printStackTrace();
            exception = e;
        }

        return new RouterActionResult(null, exception);
    }
}
