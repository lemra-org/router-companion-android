package org.rm3l.ddwrt.tasker.receiver.action;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.twofortyfouram.locale.sdk.client.receiver.AbstractPluginSettingReceiver;

import org.rm3l.ddwrt.tasker.BuildConfig;
import org.rm3l.ddwrt.tasker.Constants;
import org.rm3l.ddwrt.tasker.bundle.PluginBundleValues;
import org.rm3l.ddwrt.tasker.ui.activity.action.ActionEditActivity.SupportedCommand;
import org.rm3l.ddwrt.tasker.utils.Utils;

import static org.rm3l.ddwrt.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_CUSTOM_CMD;
import static org.rm3l.ddwrt.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_CUSTOM_IS_VARIABLE;
import static org.rm3l.ddwrt.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_CUSTOM_VARIABLE_NAME;
import static org.rm3l.ddwrt.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_IS_CUSTOM;
import static org.rm3l.ddwrt.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_SUPPORTED_NAME;
import static org.rm3l.ddwrt.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_SUPPORTED_PARAM;
import static org.rm3l.ddwrt.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_SUPPORTED_PARAM_HINT;
import static org.rm3l.ddwrt.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_SUPPORTED_PARAM_IS_VARIABLE;
import static org.rm3l.ddwrt.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_SUPPORTED_PARAM_VARIABLE_NAME;
import static org.rm3l.ddwrt.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_SUPPORTED_READABLE_NAME;
import static org.rm3l.ddwrt.tasker.bundle.PluginBundleValues.BUNDLE_EXTRA_INT_VERSION_CODE;
import static org.rm3l.ddwrt.tasker.bundle.PluginBundleValues.BUNDLE_OUTPUT_IS_VARIABLE;
import static org.rm3l.ddwrt.tasker.bundle.PluginBundleValues.BUNDLE_OUTPUT_VARIABLE_NAME;
import static org.rm3l.ddwrt.tasker.bundle.PluginBundleValues.BUNDLE_ROUTER_CANONICAL_READABLE_NAME;
import static org.rm3l.ddwrt.tasker.bundle.PluginBundleValues.BUNDLE_ROUTER_IS_VARIABLE;
import static org.rm3l.ddwrt.tasker.bundle.PluginBundleValues.BUNDLE_ROUTER_UUID;
import static org.rm3l.ddwrt.tasker.bundle.PluginBundleValues.BUNDLE_ROUTER_VARIABLE_NAME;

public final class ActionFireReceiver extends AbstractPluginSettingReceiver {

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
        Crashlytics.log(Log.DEBUG, Constants.TAG, "bundle: " + bundle);

        final String appPackage = Utils.getDDWRTCompanionAppPackage(context.getPackageManager());
        if (TextUtils.isEmpty(appPackage)) {
            Toast.makeText(context, "DD-WRT Companion App must be installed", Toast.LENGTH_SHORT).show();
            return;
        }

        final int versionCode = bundle.getInt(BUNDLE_EXTRA_INT_VERSION_CODE);
        final boolean routerIsVariable = bundle.getBoolean(BUNDLE_ROUTER_IS_VARIABLE, false);
        final String routerVariableName = bundle.getString(BUNDLE_ROUTER_VARIABLE_NAME);
        final String routerUuid = bundle.getString(BUNDLE_ROUTER_UUID);
        final String routerCanonicalReadableName = bundle.getString(BUNDLE_ROUTER_CANONICAL_READABLE_NAME);

        final boolean commandIsCustom = bundle.getBoolean(BUNDLE_COMMAND_IS_CUSTOM, false);
        final boolean commandCustomIsVariable = bundle.getBoolean(BUNDLE_COMMAND_CUSTOM_IS_VARIABLE, false);
        final String commandCustomVariableName = bundle.getString(BUNDLE_COMMAND_CUSTOM_VARIABLE_NAME);
        final String commandCustomCmd = bundle.getString(BUNDLE_COMMAND_CUSTOM_CMD);
        final String commandSupportedName = bundle.getString(BUNDLE_COMMAND_SUPPORTED_NAME);
        final String commandSupportedReadableName = bundle.getString(BUNDLE_COMMAND_SUPPORTED_READABLE_NAME);
        final String commandSupportedParamHint = bundle.getString(BUNDLE_COMMAND_SUPPORTED_PARAM_HINT);
        final String commandSupportedParam = bundle.getString(BUNDLE_COMMAND_SUPPORTED_PARAM);
        final boolean commandSupportedParamIsVariable = bundle.getBoolean(BUNDLE_COMMAND_SUPPORTED_PARAM_IS_VARIABLE);
        final String commandSupportedParamVariableName = bundle
                .getString(BUNDLE_COMMAND_SUPPORTED_PARAM_VARIABLE_NAME);

        final boolean outputIsVariable = bundle.getBoolean(BUNDLE_OUTPUT_IS_VARIABLE, false);
        final String outputVariableName = bundle.getString(BUNDLE_OUTPUT_VARIABLE_NAME);

        final StringBuilder deeplinkStringBuilder = new StringBuilder()
                .append("ddwrt://routers/");
        if (routerIsVariable) {
            deeplinkStringBuilder.append(routerVariableName);
        } else {
            deeplinkStringBuilder.append(routerUuid);
        }
        deeplinkStringBuilder.append("/actions/");

        final SupportedCommand supportedCommand;
        if (commandIsCustom) {
            supportedCommand = SupportedCommand.CUSTOM_COMMAND;
        } else {
            try {
                supportedCommand = SupportedCommand.valueOf(commandSupportedName);
            } catch (IllegalArgumentException iae) {
                Crashlytics.logException(iae);
                Toast.makeText(context, "Internal Error - please try again later",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }
        deeplinkStringBuilder.append(supportedCommand.actionName.toLowerCase());

        deeplinkStringBuilder.append("?origin=").append(BuildConfig.APPLICATION_ID);

        if (commandIsCustom) {
            deeplinkStringBuilder.append("&cmd=");
            if (commandCustomIsVariable) {
                deeplinkStringBuilder.append(commandCustomVariableName);
            } else {
                deeplinkStringBuilder.append(commandCustomCmd);
            }
        } else {
            if (!TextUtils.isEmpty(supportedCommand.paramName)) {
                deeplinkStringBuilder.append("&").append(supportedCommand.paramName).append("=");
                if (commandSupportedParamIsVariable) {
                    deeplinkStringBuilder.append(commandSupportedParamVariableName);
                } else {
                    deeplinkStringBuilder.append(commandSupportedParam);
                }
            }
        }

        final String deepLinkUrl = deeplinkStringBuilder.toString();

        Crashlytics.log(Log.DEBUG, Constants.TAG,
                String.format("\n- appPackage = [%s]\n- deepLinkUrl = [%s]",
                        appPackage, deepLinkUrl));

        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(deepLinkUrl));
        intent.setPackage(appPackage);

        final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        intent.putExtra("ORIGIN_INTENT", pendingIntent);
        intent.putExtra("CREATOR_PKG", BuildConfig.APPLICATION_ID);

        Crashlytics.log(Log.DEBUG, Constants.TAG, "pendingIntent: " + pendingIntent);
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            Crashlytics.logException(e);
            Toast.makeText(context, "Internal Error - please try again later", Toast.LENGTH_SHORT).show();
//            e.printStackTrace();
        }
//        context.startActivity(intent);
    }

}