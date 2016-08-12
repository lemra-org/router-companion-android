package org.rm3l.ddwrt.tasker.receiver;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.twofortyfouram.locale.sdk.client.receiver.AbstractPluginSettingReceiver;

import org.rm3l.ddwrt.tasker.bundle.PluginBundleValues;

public final class FireReceiver extends AbstractPluginSettingReceiver {

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
        //TODO
//        Toast.makeText(context, PluginBundleValues.getMessage(bundle), Toast.LENGTH_LONG).show();
    }
}