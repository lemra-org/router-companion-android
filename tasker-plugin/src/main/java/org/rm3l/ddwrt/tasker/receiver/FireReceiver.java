package org.rm3l.ddwrt.tasker.receiver;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.twofortyfouram.locale.sdk.client.receiver.AbstractPluginSettingReceiver;

import org.rm3l.ddwrt.common.IDDWRTCompanionService;
import org.rm3l.ddwrt.tasker.bundle.PluginBundleValues;
import org.rm3l.ddwrt.tasker.utils.Utils;

import static org.rm3l.ddwrt.tasker.Constants.TAG;

public final class FireReceiver extends AbstractPluginSettingReceiver {

    /** Service to which this client will bind */
    private IDDWRTCompanionService ddwrtCompanionService;

    /** Connection to the service (inner class) */
    private DDWRTCompanionServiceConnection conn;

    @Override
    protected boolean isBundleValid(@NonNull final Bundle bundle) {
        return PluginBundleValues.isBundleValid(bundle);
    }

    @Override
    protected boolean isAsync() {
        return false;
    }

    @Override
    protected void firePluginSetting(@NonNull final Context context, @NonNull final Bundle bundle) {
        final String ddwrtCompanionAppPackage = Utils
                .getDDWRTCompanionAppPackage(context.getPackageManager());
        Crashlytics.log(Log.DEBUG, TAG, "ddwrtCompanionAppPackage=" + ddwrtCompanionAppPackage);
        if (ddwrtCompanionAppPackage == null) {
            Crashlytics.log(Log.WARN, TAG, "DD-WRT Companion app *not* installed!");
            return;
        }

        // connect to the service
        conn = new DDWRTCompanionServiceConnection(bundle);

        // name must match the service's Intent filter in the Service Manifest file
        final Intent intent = new Intent("org.rm3l.ddwrt.IDDWRTCompanionService");
        intent.setPackage(ddwrtCompanionAppPackage);
        // bind to the Service, create it if it's not already there
        context.bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }

    /** Inner class used to connect to UserDataService */
    class DDWRTCompanionServiceConnection implements ServiceConnection {

        private final Bundle bundle;

        public DDWRTCompanionServiceConnection(Bundle bundle) {
            this.bundle = bundle;
        }

        /**
         * is called once the bind succeeds
         */
        public void onServiceConnected(ComponentName name, IBinder service) {
            Crashlytics.log(Log.DEBUG, TAG, "Service connected");
            ddwrtCompanionService = IDDWRTCompanionService.Stub.asInterface(service);

            //TODO Execute the actual action
//        Toast.makeText(context, PluginBundleValues.getMessage(bundle), Toast.LENGTH_LONG).show();
        }

        /*** is called once the remote service is no longer available */
        public void onServiceDisconnected(ComponentName name) { //
            Crashlytics.log(Log.WARN, TAG, "Service has unexpectedly disconnected");
            ddwrtCompanionService = null;
        }

    }

}