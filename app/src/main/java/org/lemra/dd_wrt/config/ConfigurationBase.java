package org.lemra.dd_wrt.config;

import android.content.SharedPreferences;

import org.jetbrains.annotations.Nullable;
import org.lemra.dd_wrt.api.conn.Router;

/**
 * Created by armel on 8/9/14.
 */
public abstract class ConfigurationBase {

    public static String mLocalRouterUid;
    protected static boolean mConnectedOnLocalRouter;
    public Router mCurrentRouter;
    public boolean mUseLocalAddress = false;
    protected boolean mNetworkValid = false;

    public static boolean isConnectedOnLocalRouter() {
        return mConnectedOnLocalRouter;
    }

    public abstract SharedPreferences getPreferences(String paramString);

    public abstract int getTrackId();

    public final boolean isNetworkValid() {
        return this.mNetworkValid;
    }

    public final void setNetworkValid(boolean paramBoolean) {
        this.mNetworkValid = paramBoolean;
    }

    public final boolean setCurrentRouterUseLocalAddress(boolean paramBoolean) {
        if (this.mCurrentRouter != null) {
            this.mUseLocalAddress = paramBoolean;
            return true;
        }
        return false;
    }

    public abstract ConfigurationBase obtainRouterConfig(String paramString);

    @Nullable
    public final ConfigurationBase obtainLocalRouterConfig() {
        ConfigurationBase localConfigurationBase = obtainRouterConfig(mLocalRouterUid);
        if (localConfigurationBase != null) {
            localConfigurationBase.setCurrentRouterUseLocalAddress(true);
            localConfigurationBase.mNetworkValid = true;
            return localConfigurationBase;
        }
        return null;
    }

    public static abstract interface AskPasswordListener {
        public abstract void onCancel();

        public abstract void onPasswordEntered(String paramString);
    }

    public static abstract interface askRebootListener {
        public abstract void onCancel();

        public abstract void onOk();
    }
}
