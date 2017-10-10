package org.rm3l.router_companion.actions;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import com.crashlytics.android.Crashlytics;
import com.google.common.base.Strings;
import java.util.UUID;
import org.rm3l.router_companion.common.resources.audit.ActionLog;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.SSHUtils;

/**
 * Created by rm3l on 22/08/16.
 */
public class UploadAndExecuteScriptRouterAction extends AbstractRouterAction<String[]> {

    private static final String REMOTE_DEST_FILE_FORMAT = "/tmp/.%s";

    private static final String LOG_TAG = UploadAndExecuteScriptRouterAction.class.getSimpleName();

    @Nullable
    private final String mAdditionalArguments;

    @NonNull
    private final Context mContext;

    @NonNull
    private final String mFileAbsolutePath;

    @NonNull
    private final String mRemoteFileAbsolutePath;

    private String[] mResult;

    public UploadAndExecuteScriptRouterAction(Router router, @NonNull Context context,
            @Nullable RouterActionListener listener,
            @NonNull final SharedPreferences globalSharedPreferences,
            @NonNull final String fileAbsolutePath, @Nullable final String additionalArguments) {
        super(router, listener, RouterAction.EXEC_FROM_FILE, globalSharedPreferences);
        this.mContext = context;
        this.mFileAbsolutePath = fileAbsolutePath;
        this.mRemoteFileAbsolutePath =
                String.format(REMOTE_DEST_FILE_FORMAT, UUID.randomUUID().toString());
        this.mAdditionalArguments = additionalArguments;
    }

    @NonNull
    @Override
    protected RouterActionResult<String[]> doActionInBackground() {

        Exception exception = null;
        try {
            Crashlytics.log(Log.INFO, LOG_TAG,
                    String.format("File upload: [%s] => [%s]", mFileAbsolutePath, mRemoteFileAbsolutePath));

            if (!SSHUtils.scpTo(mContext, router, globalSharedPreferences, mFileAbsolutePath,
                    mRemoteFileAbsolutePath)) {
                throw new IllegalStateException("Failed to upload script to the router");
            }

            mResult = SSHUtils.getManualProperty(mContext, router, globalSharedPreferences,
                    "chmod 700 " + mRemoteFileAbsolutePath,
                    mRemoteFileAbsolutePath + " " + Strings.nullToEmpty(mAdditionalArguments));
        } catch (Exception e) {
            e.printStackTrace();
            exception = e;
        } finally {
            //Delete file uploaded right away
            try {
                SSHUtils.getManualProperty(mContext, router, globalSharedPreferences,
                        "rm -rf " + mRemoteFileAbsolutePath);
            } catch (Exception e) {
                e.printStackTrace();
                exception = e;
            }
        }

        return new RouterActionResult<>(mResult, exception);
    }

    @Override
    protected ActionLog getActionLog() {
        return super.getActionLog()
                .setActionData(
                        String.format("- Local file: %s\n" + "- Remote file: %s\n" + "- Additional args: %s",
                                mFileAbsolutePath, mRemoteFileAbsolutePath, mAdditionalArguments));
    }

    @Nullable
    @Override
    protected Context getContext() {
        return mContext;
    }

    @Nullable
    @Override
    protected String[] getDataToReturnOnSuccess() {
        return mResult;
    }
}
