package org.rm3l.ddwrt.utils;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.rm3l.ddwrt.exceptions.StorageException;

import java.io.File;

/**
 * Created by rm3l on 10/12/15.
 */
public final class StorageUtils {

    private static final String TAG = StorageUtils.class.getSimpleName();

    private StorageUtils() {}

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));
    }

    @Nullable
    public static File getAppDirectory(@Nullable final Context ctx) {
        if (ctx == null) {
            return null;
        }
        final File containerDir;
        final CharSequence applicationName = Utils.getApplicationName(ctx);
        final String outputFileName =
                (applicationName != null ? applicationName.toString() : "DD-WRT Companion");

        if (isExternalStorageWritable()) {
            //Store in the primary (top-level or root) external storage directory
            containerDir = new File(Environment.getExternalStorageDirectory(), outputFileName);
        } else { //SDCard not available at this time - try with internal storage
            containerDir = ctx.getFilesDir();
//                    this.getDir(outputFileName, Context.MODE_PRIVATE);
        }
        if (!(containerDir.mkdirs() || containerDir.isDirectory())) {
            final String absolutePath = containerDir.getAbsolutePath();
            Crashlytics.log(Log.ERROR, TAG,
                    "Directory " + absolutePath + " not created");
            throw new StorageException("Failed to create directory " +
                    absolutePath);
        }
        return containerDir;
    }

    @Nullable
    public static File getExportDirectory(@Nullable final Context ctx) {
        final File appDirectory = getAppDirectory(ctx);
        if (appDirectory == null) {
            return null;
        }
        final File exportDir = new File(appDirectory, "export");

        if (!(exportDir.mkdirs() || exportDir.isDirectory())) {
            final String absolutePath = exportDir.getAbsolutePath();
            Crashlytics.log(Log.ERROR, TAG,
                    "Directory " + absolutePath + " not created");
            throw new StorageException("Failed to create directory " +
                    absolutePath);
        }
        return exportDir;
    }

    public static void createDirectoryOrRaiseException(@NonNull final File dir) {
        if (!(dir.mkdirs() || dir.isDirectory())) {
            throw new StorageException("Directory not created: " +
                    dir.getAbsolutePath());
        }
    }
}
