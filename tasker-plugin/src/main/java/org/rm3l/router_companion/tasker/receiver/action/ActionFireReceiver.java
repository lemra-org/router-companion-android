package org.rm3l.router_companion.tasker.receiver.action;

import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_APP_PIN_CODE;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_CUSTOM_CMD;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_CUSTOM_IS_VARIABLE;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_CUSTOM_VARIABLE_NAME;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_IS_CUSTOM;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_SUPPORTED_NAME;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_SUPPORTED_PARAM;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_SUPPORTED_PARAM_HINT;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_SUPPORTED_PARAM_IS_VARIABLE;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_SUPPORTED_PARAM_VARIABLE_NAME;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_COMMAND_SUPPORTED_READABLE_NAME;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_EXTRA_INT_VERSION_CODE;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_OUTPUT_IS_VARIABLE;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_OUTPUT_VARIABLE_NAME;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_ROUTER_CANONICAL_READABLE_NAME;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_ROUTER_IS_VARIABLE;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_ROUTER_UUID;
import static org.rm3l.router_companion.tasker.bundle.PluginBundleValues.BUNDLE_ROUTER_VARIABLE_NAME;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.twofortyfouram.locale.sdk.client.receiver.AbstractPluginSettingReceiver;
import org.rm3l.router_companion.tasker.BuildConfig;
import org.rm3l.router_companion.tasker.bundle.PluginBundleValues;
import org.rm3l.router_companion.tasker.exception.DDWRTCompanionPackageVersionRequiredNotFoundException;
import org.rm3l.router_companion.tasker.ui.activity.action.ActionEditActivity.SupportedCommand;
import org.rm3l.router_companion.tasker.utils.Utils;

public final class ActionFireReceiver extends AbstractPluginSettingReceiver {

  @Override
  protected void firePluginSetting(@NonNull final Context context, @NonNull final Bundle bundle) {
    FirebaseCrashlytics.getInstance().log("bundle: " + bundle);

    try {
      final PackageInfo packageInfo =
          Utils.getDDWRTCompanionAppPackageLeastRequiredVersion(context.getPackageManager());
      final String appPackage = (packageInfo != null ? packageInfo.packageName : null);
      if (TextUtils.isEmpty(appPackage)) {
        Toast.makeText(context, "DD-WRT Companion App must be installed", Toast.LENGTH_SHORT)
            .show();
        return;
      }

      final int versionCode = bundle.getInt(BUNDLE_EXTRA_INT_VERSION_CODE);

      final String appPinCode = bundle.getString(BUNDLE_APP_PIN_CODE);

      final boolean routerIsVariable = bundle.getBoolean(BUNDLE_ROUTER_IS_VARIABLE, false);
      final String routerVariableName = bundle.getString(BUNDLE_ROUTER_VARIABLE_NAME);
      final String routerUuid = bundle.getString(BUNDLE_ROUTER_UUID);
      final String routerCanonicalReadableName =
          bundle.getString(BUNDLE_ROUTER_CANONICAL_READABLE_NAME);

      final boolean commandIsCustom = bundle.getBoolean(BUNDLE_COMMAND_IS_CUSTOM, false);
      final boolean commandCustomIsVariable =
          bundle.getBoolean(BUNDLE_COMMAND_CUSTOM_IS_VARIABLE, false);
      final String commandCustomVariableName =
          bundle.getString(BUNDLE_COMMAND_CUSTOM_VARIABLE_NAME);
      final String commandCustomCmd = bundle.getString(BUNDLE_COMMAND_CUSTOM_CMD);
      final String commandSupportedName = bundle.getString(BUNDLE_COMMAND_SUPPORTED_NAME);
      final String commandSupportedReadableName =
          bundle.getString(BUNDLE_COMMAND_SUPPORTED_READABLE_NAME);
      final String commandSupportedParamHint =
          bundle.getString(BUNDLE_COMMAND_SUPPORTED_PARAM_HINT);
      final String commandSupportedParam = bundle.getString(BUNDLE_COMMAND_SUPPORTED_PARAM);
      final boolean commandSupportedParamIsVariable =
          bundle.getBoolean(BUNDLE_COMMAND_SUPPORTED_PARAM_IS_VARIABLE);
      final String commandSupportedParamVariableName =
          bundle.getString(BUNDLE_COMMAND_SUPPORTED_PARAM_VARIABLE_NAME);

      final boolean outputIsVariable = bundle.getBoolean(BUNDLE_OUTPUT_IS_VARIABLE, false);
      final String outputVariableName = bundle.getString(BUNDLE_OUTPUT_VARIABLE_NAME);

      final StringBuilder deeplinkStringBuilder = new StringBuilder().append("ddwrt://routers/");
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
          FirebaseCrashlytics.getInstance().recordException(iae);
          Toast.makeText(context, "Internal Error - please try again later", Toast.LENGTH_SHORT)
              .show();
          return;
        }
      }
      deeplinkStringBuilder.append(supportedCommand.actionName.toLowerCase());

      deeplinkStringBuilder.append("?origin=").append(BuildConfig.APPLICATION_ID);

      if (!TextUtils.isEmpty(appPinCode)) {
        deeplinkStringBuilder.append("&pinCode=").append(appPinCode);
      }

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

      FirebaseCrashlytics.getInstance()
          .log(
              String.format(
                  "\n- appPackage = [%s]\n- deepLinkUrl = [%s]", appPackage, deepLinkUrl));

      final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(deepLinkUrl));
      intent.setPackage(appPackage);

      final PendingIntent pendingIntent =
          PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
      intent.putExtra("ORIGIN_INTENT", pendingIntent);
      intent.putExtra("CREATOR_PKG", BuildConfig.APPLICATION_ID);

      FirebaseCrashlytics.getInstance().log("pendingIntent: " + pendingIntent);
      try {
        pendingIntent.send();
      } catch (PendingIntent.CanceledException e) {
        FirebaseCrashlytics.getInstance().recordException(e);
        Toast.makeText(context, "Internal Error - please try again later", Toast.LENGTH_SHORT)
            .show();
        //            e.printStackTrace();
      }
      //        context.startActivity(intent);
    } catch (final DDWRTCompanionPackageVersionRequiredNotFoundException e) {
      FirebaseCrashlytics.getInstance().recordException(e);
      Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
      return;
    }
  }

  @Override
  protected boolean isAsync() {
    return false;
  }

  @Override
  protected boolean isBundleValid(@NonNull final Bundle bundle) {
    return PluginBundleValues.isBundleValid(bundle);
  }
}
