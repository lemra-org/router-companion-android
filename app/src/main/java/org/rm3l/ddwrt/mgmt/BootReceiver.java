package org.rm3l.ddwrt.mgmt;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;

import org.rm3l.ddwrt.service.BackgroundService;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;

public class BootReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {

        // in our case intent will always be BOOT_COMPLETED, so we can just set
        // the alarm
        // Note that a BroadcastReceiver is *NOT* a Context. Thus, we can't use
        // "this" whenever we need to pass a reference to the current context.
        // Thankfully, Android will supply a valid Context as the first parameter

        final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        final SharedPreferences preferences = context
                .getSharedPreferences(DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY,
                        Context.MODE_PRIVATE);

        final boolean bgServiceEnabled = preferences
                .getBoolean(DDWRTCompanionConstants.NOTIFICATIONS_BG_SERVICE_ENABLE, true);
        final long minutes = preferences.getLong(
                DDWRTCompanionConstants.NOTIFICATIONS_SYNC_INTERVAL_MINUTES_PREF, -1l);

        final Intent backgroundServiceIntent = new Intent(context, BackgroundService.class);
        final PendingIntent pi = PendingIntent.getService(context, 0, backgroundServiceIntent, 0);
        am.cancel(pi);

        if (!bgServiceEnabled || minutes <= 0l) {
            //Skip
            return;
        }

        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + minutes * 60 * 1000,
                minutes * 60 * 1000, pi);

    }
}
