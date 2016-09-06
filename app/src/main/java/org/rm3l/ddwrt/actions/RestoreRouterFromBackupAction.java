package org.rm3l.ddwrt.actions;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Joiner;

import org.apache.commons.io.FileUtils;
import org.rm3l.ddwrt.exceptions.DDWRTCompanionException;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.AdUtils;
import org.rm3l.ddwrt.utils.ReportingUtils;
import org.rm3l.ddwrt.utils.SSHUtils;

import java.io.File;
import java.io.InputStream;
import java.util.Date;

/**
 * Created by rm3l on 10/05/15.
 */
public class RestoreRouterFromBackupAction extends AbstractRouterAction<Void> {

    private static final String TO_REMOTE_PATH = "/tmp/.DDWRTCompanion_nvrambak_torestore.bin";
    private final Context mContext;
    private final InputStream mBackupFileInputStream;

    public RestoreRouterFromBackupAction(Router router, @NonNull Context context, @Nullable RouterActionListener listener,
                                         @NonNull final SharedPreferences globalSharedPreferences,
                                         @NonNull final InputStream backupFileInputStream) {
        super(router, listener, RouterAction.RESTORE, globalSharedPreferences);
        this.mContext = context;
        this.mBackupFileInputStream = backupFileInputStream;
    }

    @Nullable
    @Override
    protected Context getContext() {
        return mContext;
    }

    @NonNull
    @Override
    protected RouterActionResult<Void> doActionInBackground() {
        Exception exception = null;
        File tempFile = null;
        try {
            tempFile = File.createTempFile("nvrambak_to_restore_" + router.getUuid(), ".bin",
                    mContext.getCacheDir());

            FileUtils.copyInputStreamToFile(mBackupFileInputStream, tempFile);

            if (!SSHUtils.scpTo(mContext, router, globalSharedPreferences,
                    tempFile.getAbsolutePath(),
                    TO_REMOTE_PATH)) {
                throw new IllegalStateException("Failed to copy file onto remote Router");
            }

            final int exitStatus = SSHUtils
                    .runCommands(mContext, globalSharedPreferences, router,
                            Joiner.on(" && ").skipNulls(),
                            String.format("/usr/sbin/nvram restore %s", TO_REMOTE_PATH),
                            "/sbin/reboot");

            if (exitStatus != 0) {
                throw new IllegalStateException("Restore command execution did not succeed. Please try again later.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            exception = e;

        } finally {
            try {
                SSHUtils.runCommands(mContext, globalSharedPreferences, router,
                        String.format("/bin/rm -rf %s", TO_REMOTE_PATH));
            } catch (final Exception e) {
                ReportingUtils.reportException(mContext, e);
                //No worries
            } finally {
                if (tempFile != null) {
                    //noinspection ResultOfMethodCallIgnored
                    tempFile.delete();
                }
            }
        }

        return new RouterActionResult<>(null, exception);
    }

    public static class AgreementToRestoreRouterFromBackup extends DDWRTCompanionException {

        private final Date mClickDate;

        private final String mDeviceId;

        public AgreementToRestoreRouterFromBackup(@NonNull Context context) {
            mClickDate = new Date();
            mDeviceId = AdUtils.getDeviceIdForAdMob(context);
        }

        public Date getClickDate() {
            return mClickDate;
        }

        public String getDeviceId() {
            return mDeviceId;
        }
    }
}
