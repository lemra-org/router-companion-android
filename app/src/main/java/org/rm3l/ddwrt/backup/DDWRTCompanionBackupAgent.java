package org.rm3l.ddwrt.backup;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteOpenHelper;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class DDWRTCompanionBackupAgent extends BackupAgentHelper {

    public static final String ROUTERS_DB = "routersDB";
    public static final String PREFERENCES = "preferences";
    public static final String USAGE_DATA = "usageData";
    public static final String ROUTER_PREFERENCES = "routerPreferences";
    private static final String LOG_TAG = DDWRTCompanionBackupAgent.class.getSimpleName();

    private DDWRTCompanionDAO dao;

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "onCreate called");

        dao = RouterManagementActivity.getDao(this);

        //Database
        final FileBackupHelper routers = new FileBackupHelper(this, "../databases/"
                + DDWRTCompanionSqliteOpenHelper.DATABASE_NAME);
        addHelper(ROUTERS_DB, routers);

        //Preferences
        final SharedPreferencesBackupHelper prefs = new SharedPreferencesBackupHelper(this,
                DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY);

        addHelper(PREFERENCES, prefs);

    }

    private void addFileHelper(@NonNull final String keyPrefix, @NonNull final String... files) {
        if (files.length == 0) {
            return;
        }
        addHelper(keyPrefix, new FileBackupHelper(this, files));
    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
                         ParcelFileDescriptor newState) throws IOException {
        Log.d(LOG_TAG, "onBackup called");
        final List<Router> allRouters = dao.getAllRouters();

        if (allRouters != null) {
            final Collection<String> routerUuids = Collections2.transform(allRouters, new Function<Router, String>() {
                @Override
                public String apply(Router input) {
                    // We use router uuid as shared preference group name for this router.
                    return input.getUuid();
                }
            });
            if (routerUuids != null) {
                addFileHelper(ROUTER_PREFERENCES,
                        routerUuids.toArray(new String[routerUuids.size()]));

                //Usage Data
                final Collection<String> pathsToRoutersUsageDataFiles = Collections2.transform(routerUuids, new Function<String, String>() {
                    @Override
                    public String apply(String input) {
                        return String.format("../files/%s_Usage_%s.bak", BuildConfig.APPLICATION_ID, input);
                    }
                });
                if (pathsToRoutersUsageDataFiles != null) {
                    addFileHelper(USAGE_DATA,
                            pathsToRoutersUsageDataFiles.toArray(new String[pathsToRoutersUsageDataFiles.size()]));
                }
            }
        }

        synchronized (DDWRTCompanionSqliteOpenHelper.dbLock) {
            Log.d(LOG_TAG, "onBackup called after synchronized block");
            super.onBackup(oldState, data, newState);
        }
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode,
                          ParcelFileDescriptor newState) throws IOException {
        Log.d(LOG_TAG, "onRestore called");
        synchronized (DDWRTCompanionSqliteOpenHelper.dbLock) {
            Log.d(LOG_TAG, "onRestore called after synchronized synchronized block");
            super.onRestore(data, appVersionCode, newState);
        }
    }

}
