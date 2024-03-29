package org.rm3l.router_companion.actions;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.common.base.Splitter;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.exceptions.DDWRTCompanionException;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;

/** Created by rm3l on 10/05/15. */
public class RestoreWANMonthlyTrafficFromBackupAction extends AbstractRouterAction<Void> {

  public static class AgreementToRestoreWANTraffDataFromBackup extends DDWRTCompanionException {

    private final Date mClickDate;

    public AgreementToRestoreWANTraffDataFromBackup(@NonNull Context context) {
      mClickDate = new Date();
    }
  }

  private final InputStream mBackupFileInputStream;

  private final Context mContext;

  public RestoreWANMonthlyTrafficFromBackupAction(
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
              "ttraffbak_to_restore_" + router.getUuid(), ".bin", mContext.getCacheDir());

      Files.write(ByteStreams.toByteArray(mBackupFileInputStream), tempFile);
      // FileUtils.copyInputStreamToFile(mBackupFileInputStream, tempFile);

      final List<String> fileLines = Files.readLines(tempFile, RouterCompanionAppConstants.CHARSET);
      final NVRAMInfo linesToNVRAM = new NVRAMInfo();

      final Splitter splitter = Splitter.on("=").omitEmptyStrings();
      for (final String fileLine : fileLines) {
        if (fileLine == null || fileLine.isEmpty()) {
          continue;
        }
        if (!fileLine.startsWith("traff-")) {
          continue;
        }
        final List<String> stringList = splitter.splitToList(fileLine);
        if (stringList.size() < 2) {
          continue;
        }
        linesToNVRAM.setProperty(stringList.get(0), stringList.get(1));
      }

      final RouterActionResult<Void> setNvramActionResult =
          SetNVRAMVariablesAction.getRouterActionResult(
              mContext, globalSharedPreferences, router, linesToNVRAM, false, null);

      if (setNvramActionResult == null) {
        throw new IllegalStateException("Failed to execute action");
      }

      exception = setNvramActionResult.getException();
    } catch (Exception e) {
      e.printStackTrace();
      exception = e;
    } finally {
      if (tempFile != null) {
        //noinspection ResultOfMethodCallIgnored
        tempFile.delete();
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
