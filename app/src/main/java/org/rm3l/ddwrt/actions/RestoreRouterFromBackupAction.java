package org.rm3l.ddwrt.actions;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Joiner;

import org.apache.commons.io.FileUtils;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.io.File;
import java.io.InputStream;

/**
 * Created by rm3l on 10/05/15.
 */
public class RestoreRouterFromBackupAction extends AbstractRouterAction<Void> {

    private final Context mContext;
    private final InputStream mBackupFileInputStream;

    private static final String TO_REMOTE_PATH = "/tmp/.DDWRTCompanion_nvrambak_torestore.bin";

    public RestoreRouterFromBackupAction(@NonNull Context context, @Nullable RouterActionListener listener,
                              @NonNull final SharedPreferences globalSharedPreferences,
                                         @NonNull final InputStream backupFileInputStream) {
        super(listener, RouterAction.RESTORE, globalSharedPreferences);
        this.mContext = context;
        this.mBackupFileInputStream = backupFileInputStream;
    }

    @NonNull
    @Override
    protected RouterActionResult doActionInBackground(@NonNull Router router) {
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
                Utils.reportException(e);
                //No worries
            } finally {
                if (tempFile != null) {
                    //noinspection ResultOfMethodCallIgnored
                    tempFile.delete();
                }
            }
        }

        return new RouterActionResult(null, exception);
    }
}
