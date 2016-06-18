package org.rm3l.ddwrt.actions;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.ReportingUtils;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.io.File;
import java.util.Date;
import java.util.UUID;

/**
 * Created by rm3l on 09/05/15.
 */
public class BackupRouterAction extends AbstractRouterAction<String> {

    @NonNull
    private final Context mContext;

    private static final String BACKUP_FILENAME_SUFFIX = ".DDWRTCompanion" + UUID.randomUUID()
            + "_nvrambak.bin";

    private File mLocalBackupFilePath = null;

    private final String mRemoteBackupFilename = "/tmp/" + BACKUP_FILENAME_SUFFIX;

    private Date mBackupDate = null;

    public BackupRouterAction(Router router, @NonNull Context context, @Nullable RouterActionListener listener,
                              @NonNull final SharedPreferences globalSharedPreferences) {
        super(router, listener, RouterAction.BACKUP, globalSharedPreferences);
        this.mContext = context;
    }

    @NonNull
    @Override
    protected RouterActionResult<String> doActionInBackground() {
        Exception exception = null;
        try {
            mBackupDate = new Date();

            final int exitStatus = SSHUtils
                    .runCommands(mContext, globalSharedPreferences, router,
                            String.format("/usr/sbin/nvram backup %s", mRemoteBackupFilename));

            if (exitStatus != 0) {
                throw new IllegalStateException("Router rejected the backup command.");
            }

            //Copy remote file locally and finish by dropping it!
            final String escapedFileName =
                    Utils.getEscapedFileName("nvrambak_" + router.getUuid() + "__" + mBackupDate) + "__.bin";

            //Write to app data storage on internal storage
            mLocalBackupFilePath = new File(mContext.getCacheDir(),
                    escapedFileName);

            if (!SSHUtils
                    .scpFrom(mContext, router,
                            globalSharedPreferences,
                            mRemoteBackupFilename,
                            mLocalBackupFilePath.getAbsolutePath(), true)) {
                throw new IllegalStateException("Backup operation succeeded, " +
                        "but could not copy the resulting file on this device. Please try again later.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            exception = e;

        } finally {
            try {
                SSHUtils.runCommands(mContext, globalSharedPreferences, router,
                        String.format("/bin/rm -rf %s", mRemoteBackupFilename));
            } catch (final Exception e) {
                ReportingUtils.reportException(mContext, e);
                //No worries
            }
        }

        return new RouterActionResult<>(null, exception);
    }

    @Nullable
    @Override
    protected Object getDataToReturnOnSuccess() {
        return new Object[]{mBackupDate, mLocalBackupFilePath};
    }
}
