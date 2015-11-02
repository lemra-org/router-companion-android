package org.rm3l.ddwrt.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.rm3l.ddwrt.mgmt.BootReceiver;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.service.tasks.AbstractBackgroundServiceTask;
import org.rm3l.ddwrt.service.tasks.ConnectedHostsServiceTask;
import org.rm3l.ddwrt.service.tasks.PublicIPChangesServiceTask;
import org.rm3l.ddwrt.service.tasks.RouterModelUpdaterServiceTask;
import org.rm3l.ddwrt.service.tasks.RouterWebInterfaceParametersUpdaterServiceTask;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by rm3l on 05/07/15.
 */
public class BackgroundService extends IntentService {

    private static final String TAG = BackgroundService.class.getSimpleName();

//    private PowerManager.WakeLock mWakeLock;

    public BackgroundService() {
        super(BackgroundService.class.getSimpleName());
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public BackgroundService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
        // check the global background data setting
            final ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            if (intent == null || cm == null || cm.getActiveNetworkInfo() == null) {
            /*
             * If getActiveNetworkInfo() is null, you do not have a network connection,
             * either because the device does not have a network connection,
             * or because user settings (e.g., bandwidth caps) prevent your app
             * from having a network connection.
             */
                stopSelf();
                return;
            }

            //Do the actual work - no need to do so in a separate thread
            final List<AbstractBackgroundServiceTask> tasks = new ArrayList<>();
            tasks.add(new RouterModelUpdaterServiceTask(BackgroundService.this));
            tasks.add(new RouterWebInterfaceParametersUpdaterServiceTask(BackgroundService.this));
            //According to user preference
            final Set<String> notificationsChoiceSet =
                    getSharedPreferences(DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
                            .getStringSet(DDWRTCompanionConstants.NOTIFICATIONS_CHOICE_PREF, new HashSet<String>());
            Crashlytics.log(Log.DEBUG,  TAG, "notificationsChoiceSet: " + notificationsChoiceSet);
            if (notificationsChoiceSet.contains(ConnectedHostsServiceTask.class.getSimpleName())) {
                tasks.add(new ConnectedHostsServiceTask(BackgroundService.this));
            }
            if (notificationsChoiceSet.contains(PublicIPChangesServiceTask.class.getSimpleName())) {
                tasks.add(new PublicIPChangesServiceTask(BackgroundService.this));
            }
            //Add any other tasks over here

            final DDWRTCompanionDAO dao = RouterManagementActivity.getDao(BackgroundService.this);
            final List<Router> allRouters = dao.getAllRouters();
            if (allRouters == null) {
                return;
            }
            for (final Router router : allRouters) {
                if (router == null) {
                    continue;
                }

                //Execute tasks available for notifications
                for (final AbstractBackgroundServiceTask backgroundServiceTask : tasks) {
                    if (backgroundServiceTask == null) {
                        continue;
                    }
                    Crashlytics.log(Log.DEBUG,  TAG, ">>> Running task: " + backgroundServiceTask.getClass() + " on router " + router);
                    try {
                        backgroundServiceTask.runBackgroundServiceTask(router);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Utils.reportException(this, e);
                        //No worries
                    }
                }
            }

        } finally {
            try {
                BackgroundService.this.stopSelf();
            } finally {
                // Release the wake lock provided by the WakefulBroadcastReceiver.
                if (intent != null) {
                    BootReceiver.completeWakefulIntent(intent);
                }
            }
        }
    }
}
