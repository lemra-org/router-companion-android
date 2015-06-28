package org.rm3l.ddwrt.mgmt;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;

import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.service.ConnectedHostsService;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;

import java.util.List;

import static org.rm3l.ddwrt.mgmt.RouterManagementActivity.ROUTER_SELECTED;

public class BootReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {

        // in our case intent will always be BOOT_COMPLETED, so we can just set
        // the alarm
        // Note that a BroadcastReceiver is *NOT* a Context. Thus, we can't use
        // "this" whenever we need to pass a reference to the current context.
        // Thankfully, Android will supply a valid Context as the first parameter

        final DDWRTCompanionDAO dao = RouterManagementActivity.getDao(context);
        final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        final List<Router> allRouters = dao.getAllRouters();
        if (allRouters == null) {
            return;
        }

        for (final Router router : allRouters) {
            if (router == null) {
                continue;
            }
            final String mRouterUuid = router.getUuid();

            final SharedPreferences routerPreferences =
                    context.getSharedPreferences(mRouterUuid, Context.MODE_PRIVATE);

            final boolean notificationsEnabled =
                    routerPreferences.getBoolean(DDWRTCompanionConstants.NOTIFICATIONS_ENABLE, true);
            final long minutes = routerPreferences.getLong(
                    DDWRTCompanionConstants.NOTIFICATIONS_SYNC_INTERVAL_MINUTES_PREF, -1l);

            final Intent connectedHostsBackgroundServiceIntent =
                    new Intent(context, ConnectedHostsService.class);
            connectedHostsBackgroundServiceIntent.putExtra(ROUTER_SELECTED, mRouterUuid);
            final PendingIntent pi = PendingIntent
                    .getService(context, 0, connectedHostsBackgroundServiceIntent, 0);
            am.cancel(pi);
            // by my own convention, minutes <= 0 means notifications are disabled
            if (notificationsEnabled &&
                    minutes > 0) {
                am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + minutes * 60 * 1000,
                        minutes * 60 * 1000, pi);
            }
        }

    }
}
