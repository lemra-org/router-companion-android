package org.rm3l.router_companion.actions;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.common.base.Joiner;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import java.io.File;
import java.io.InputStream;
import java.util.Date;
import org.rm3l.router_companion.exceptions.DDWRTCompanionException;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.ReportingUtils;
import org.rm3l.router_companion.utils.SSHUtils;

/** Created by rm3l on 10/05/15. */
public class RestoreRouterFromBackupAction extends AbstractRouterAction<Void> {

  public static class AgreementToRestoreRouterFromBackup extends DDWRTCompanionException {

    private final Date mClickDate;

    public AgreementToRestoreRouterFromBackup(@NonNull Context context) {
      mClickDate = new Date();
    }

    public Date getClickDate() {
      return mClickDate;
    }
  }

  private static final String TO_REMOTE_PATH = "/tmp/.DDWRTCompanion_nvrambak_torestore.bin";

  private final InputStream mBackupFileInputStream;

  private final Context mContext;

  public RestoreRouterFromBackupAction(
      Router router,
      @NonNull Context context,
      @Nullable RouterActionListener listener,
      @NonNull final SharedPreferences globalSharedPreferences,
      @NonNull final InputStream backupFileInputStream) {
    super(router, listener, RouterAction.RESTORE, globalSharedPreferences);
    this.mContext = context;
    this.mBackupFileInputStream = backupFileInputStream;
  }

  @NonNull
  @Override
  protected RouterActionResult<Void> doActionInBackground() {
    Exception exception = null;
    File tempFile = null;
    try {
      tempFile =
          File.createTempFile(
              "nvrambak_to_restore_" + router.getUuid(), ".bin", mContext.getCacheDir());

      Files.write(ByteStreams.toByteArray(mBackupFileInputStream), tempFile);
      // FileUtils.copyInputStreamToFile(mBackupFileInputStream, tempFile);

      if (!SSHUtils.scpTo(
          mContext, router, globalSharedPreferences, tempFile.getAbsolutePath(), TO_REMOTE_PATH)) {
        throw new IllegalStateException("Failed to copy file onto remote Router");
      }

      final int exitStatus =
          SSHUtils.runCommands(
              mContext,
              globalSharedPreferences,
              router,
              Joiner.on(" && ").skipNulls(),
              String.format("/usr/sbin/nvram restore %s", TO_REMOTE_PATH),
              "/sbin/reboot");

      if (exitStatus != 0) {
        throw new IllegalStateException(
            "Restore command execution did not succeed. Please try again later.");
      }
    } catch (Exception e) {
      e.printStackTrace();
      exception = e;
    } finally {
      try {
        SSHUtils.runCommands(
            mContext,
            globalSharedPreferences,
            router,
            String.format("/bin/rm -rf %s", TO_REMOTE_PATH));
      } catch (final Exception e) {
        ReportingUtils.reportException(mContext, e);
        // No worries
      } finally {
        if (tempFile != null) {
          //noinspection ResultOfMethodCallIgnored
          tempFile.delete();
        }
      }
    }

    return new RouterActionResult<>(null, exception);
  }

  @Nullable
  @Override
  protected Context getContext() {
    return mContext;
  }
}
