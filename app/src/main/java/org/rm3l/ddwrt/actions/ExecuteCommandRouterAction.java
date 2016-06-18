package org.rm3l.ddwrt.actions;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.SSHUtils;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by rm3l on 15/02/16.
 */
public class ExecuteCommandRouterAction extends AbstractRouterAction<String[]> {

    @NonNull
    private final Context mContext;
    @NonNull
    private final String[] mCmd;

    private Map<String, String[]> mResultMap = new ConcurrentHashMap<>();

    public ExecuteCommandRouterAction(Router router, @NonNull Context context,
                                      @Nullable RouterActionListener listener,
                                      @NonNull final SharedPreferences globalSharedPreferences,
                                      @NonNull final String... cmd) {
        super(router, listener, RouterAction.EXEC_CMD, globalSharedPreferences);
        this.mContext = context;
        this.mCmd = cmd;
    }

    @NonNull
    @Override
    protected RouterActionResult<String[]> doActionInBackground() {
        Exception exception = null;
        String[] resultForRouter = null;
        try {
            resultForRouter = SSHUtils.getManualProperty(
                    mContext,
                    router,
                    globalSharedPreferences,
                    mCmd);

        } catch (Exception e) {
            e.printStackTrace();
            exception = e;
        }

        mResultMap.put(router.getUuid(), resultForRouter);

        return new RouterActionResult<>(resultForRouter, exception);
    }

    @Nullable
    @Override
    protected Object getDataToReturnOnSuccess() {
        return Collections.unmodifiableMap(mResultMap);
    }
}
