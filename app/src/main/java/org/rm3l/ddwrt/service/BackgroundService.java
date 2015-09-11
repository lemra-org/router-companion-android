package org.rm3l.ddwrt.service;

import android.app.IntentService;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.util.Log;

import org.rm3l.ddwrt.mgmt.BootReceiver;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.service.tasks.AbstractBackgroundServiceTask;
import org.rm3l.ddwrt.service.tasks.ConnectedHostsServiceTask;
import org.rm3l.ddwrt.service.tasks.PublicIPChangesServiceTask;
import org.rm3l.ddwrt.service.tasks.RouterModelUpdaterServiceTask;
import org.rm3l.ddwrt.service.tasks.RouterWebInterfaceParametersUpdaterServiceTask;
import org.rm3l.ddwrt.utils.Utils;

import java.util.ArrayList;
import java.util.List;

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

            // do the actual work, in a separate thread
            new BackgroundServiceTasksExecutor().execute();
        } finally {
            // Release the wake lock provided by the WakefulBroadcastReceiver.
            if (intent != null) {
                BootReceiver.completeWakefulIntent(intent);
            }
        }
    }

//    /**
//     * This is where we initialize. We call this when onStart/onStartCommand is
//     * called by the system. We won't do anything with the intent here, and you
//     * probably won't, either.
//     */
//    private void handleIntent(Intent intent) {
//
//        // obtain the wake lock
//        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
//        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
//        mWakeLock.acquire();
//
//        // check the global background data setting
//        final ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
//        if (intent == null || cm == null || cm.getActiveNetworkInfo() == null) {
//            /*
//             * If getActiveNetworkInfo() is null, you do not have a network connection,
//             * either because the device does not have a network connection,
//             * or because user settings (e.g., bandwidth caps) prevent your app
//             * from having a network connection.
//             */
//            stopSelf();
//            return;
//        }
//
//        // do the actual work, in a separate thread
//        new BackgroundServiceTasksExecutor().execute();
//    }

//    /**
//     * This is deprecated, but you have to implement it if you're planning on
//     * supporting devices with an API level lower than 5 (Android 2.0).
//     */
//    @Override
//    public void onStart(Intent intent, int startId) {
//        handleIntent(intent);
//    }
//
//    /**
//     * This is called on 2.0+ (API level 5 or higher). Returning
//     * START_NOT_STICKY tells the system to not restart the service if it is
//     * killed because of poor resource (memory/cpu) conditions.
//     */
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        handleIntent(intent);
//        return START_NOT_STICKY;
//    }
//
//    /**
//     * In onDestroy() we release our wake lock. This ensures that whenever the
//     * Service stops (killed for resources, stopSelf() called, etc.), the wake
//     * lock will be released.
//     */
//    public void onDestroy() {
//        super.onDestroy();
//        mWakeLock.release();
//    }

    private class BackgroundServiceTasksExecutor extends AsyncTask<Void, Void, Void> {

        private final List<AbstractBackgroundServiceTask> tasks = new ArrayList<>();

        public BackgroundServiceTasksExecutor() {
            //Add any other tasks over here
            tasks.add(new RouterModelUpdaterServiceTask(BackgroundService.this));
            tasks.add(new RouterWebInterfaceParametersUpdaterServiceTask(BackgroundService.this));
            tasks.add(new ConnectedHostsServiceTask(BackgroundService.this));
            tasks.add(new PublicIPChangesServiceTask(BackgroundService.this));
        }

        @Override
        protected Void doInBackground(Void... params) {
            Log.d(TAG, "doInBackground");

            final DDWRTCompanionDAO dao = RouterManagementActivity.getDao(BackgroundService.this);
            final List<Router> allRouters = dao.getAllRouters();
            if (allRouters == null) {
                return null;
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
                    Log.d(TAG, ">>> Running task: " + backgroundServiceTask.getClass() + " on router " + router);
                    try {
                        backgroundServiceTask.runBackgroundServiceTask(router);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Utils.reportException(e);
                        //No worries
                    }
                }


            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            try {
                Log.d(TAG, "onPostExecute");
            } finally {
                BackgroundService.this.stopSelf();
            }
        }
    }
}
