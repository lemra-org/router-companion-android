package org.rm3l.ddwrt.backup;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;

import java.io.IOException;
import java.util.List;

public class DDWRTCompanionBackupAgent extends BackupAgentHelper {

    private static final String LOG_TAG = DDWRTCompanionBackupAgent.class.getSimpleName();

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "onCreate called");

        //Global Preferences
        final SharedPreferencesBackupHelper prefs = new SharedPreferencesBackupHelper(this,
                DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY);
        addHelper("globalPrefs", prefs);

        //Per-router preferences
        final DDWRTCompanionDAO dao = RouterManagementActivity.getDao(this);
        final List<Router> allRouters = dao.getAllRouters();
        if (allRouters != null) {
            for (final Router router : allRouters) {
                final String uuid;
                if (router == null || (uuid = router.getUuid()) == null) {
                    continue;
                }
                final SharedPreferencesBackupHelper routerPrefs = new SharedPreferencesBackupHelper(this, uuid);
                addHelper(uuid, routerPrefs);
            }
        }

        //Database
        final FileBackupHelper routers = new FileBackupHelper(this, "../databases/" + DDWRTCompanionSqliteOpenHelper.DATABASE_NAME);
        addHelper("routersDB", routers);

    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
                         ParcelFileDescriptor newState) throws IOException {
        Log.d(LOG_TAG, "onBackup called");
        synchronized (DDWRTCompanionSqliteOpenHelper.dbLock) {
            Log.d(LOG_TAG, "onBackup called after synchronized block");
            super.onBackup(oldState, data, newState);
        }
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode,
                          ParcelFileDescriptor newState) throws IOException {
        Log.d(LOG_TAG, "onRestore called after synchronized block");
        synchronized (DDWRTCompanionSqliteOpenHelper.dbLock) {
            super.onRestore(data, appVersionCode, newState);
        }
    }

}
