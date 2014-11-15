/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Armel S.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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

    @Nullable
    public abstract ConfigurationBase obtainRouterConfig(String paramString);

    @Nullable
    public final ConfigurationBase obtainLocalRouterConfig() {
        @Nullable ConfigurationBase localConfigurationBase = obtainRouterConfig(mLocalRouterUid);
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
