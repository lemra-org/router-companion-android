package org.rm3l.ddwrt.tasker.bundle;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.twofortyfouram.spackle.AppBuildInfo;

import net.jcip.annotations.ThreadSafe;

import org.rm3l.ddwrt.tasker.BuildConfig;
import org.rm3l.ddwrt.tasker.ui.activity.EditActivity;

import static android.text.TextUtils.isEmpty;
import static com.twofortyfouram.assertion.Assertions.assertNotNull;


/**
 * Manages the {@link com.twofortyfouram.locale.api.Intent#EXTRA_BUNDLE EXTRA_BUNDLE} for this
 * plug-in.
 */
@ThreadSafe
public final class PluginBundleValues {

    /**
     * Type: {@code String}.
     * <p>
     * String message to display in a Toast message.
     */
    @NonNull
    public static final String BUNDLE_EXTRA_STRING_SELECTED_ROUTER_UUID
            = BuildConfig.APPLICATION_ID + ".extra.SELECTED_ROUTER_UUID"; //$NON-NLS-1$
    @NonNull
    public static final String BUNDLE_EXTRA_STRING_SELECTED_ROUTER_CANONICAL_NAME
            = BuildConfig.APPLICATION_ID + ".extra.SELECTED_ROUTER_CANONICAL_NAME"; //$NON-NLS-1$
    @NonNull
    public static final String BUNDLE_EXTRA_STRING_SELECTED_ROUTER_FROM_VAR
            = BuildConfig.APPLICATION_ID + ".extra.SELECTED_ROUTER_FROM_VAR"; //$NON-NLS-1$

    @NonNull
    public static final String BUNDLE_EXTRA_STRING_CMD_OUTPUT_VAR
            = BuildConfig.APPLICATION_ID + ".extra.OUTPUT_VARIABLE"; //$NON-NLS-1$

    @NonNull
    public static final String BUNDLE_EXTRA_STRING_CMD_VAR
            = BuildConfig.APPLICATION_ID + ".extra.CMD_VARIABLE"; //$NON-NLS-1$

    @NonNull
    public static final String BUNDLE_EXTRA_STRING_CMD
            = BuildConfig.APPLICATION_ID + ".extra.CMD"; //$NON-NLS-1$


    @NonNull
    public static final String BUNDLE_EXTRA_STRING_CMD_CUSTOM
            = BuildConfig.APPLICATION_ID + ".extra.CUSTOM_CMD"; //$NON-NLS-1$

    /**
     * Type: {@code int}.
     * <p>
     * versionCode of the plug-in that saved the Bundle.
     */
    /*
     * This extra is not strictly required, however it makes backward and forward compatibility
     * significantly easier. For example, suppose a bug is found in how some version of the plug-in
     * stored its Bundle. By having the version, the plug-in can better detect when such bugs occur.
     */
    @NonNull
    public static final String BUNDLE_EXTRA_INT_VERSION_CODE
            = BuildConfig.APPLICATION_ID + ".extra.INT_VERSION_CODE"; //$NON-NLS-1$

    /**
     * Method to verify the content of the bundle are correct.
     * <p>
     * This method will not mutate {@code bundle}.
     *
     * @param bundle bundle to verify. May be null, which will always return false.
     * @return true if the Bundle is valid, false if the bundle is invalid.
     */
    public static boolean isBundleValid(@Nullable final Bundle bundle) {
        if (null == bundle) {
            return false;
        }

        //TODO

//        try {
//            BundleAssertions.assertHasString(bundle, BUNDLE_EXTRA_STRING_MESSAGE, false, false);
//            BundleAssertions.assertHasInt(bundle, BUNDLE_EXTRA_INT_VERSION_CODE);
//            BundleAssertions.assertKeyCount(bundle, 2);
//        } catch (final AssertionError e) {
//            Lumberjack.e("Bundle failed verification%s", e); //$NON-NLS-1$
//            return false;
//        }

        return true;
    }

    /**
     * @param bundle A valid plug-in bundle.
     * @return The message inside the plug-in bundle.
     */
//    @NonNull
//    public static String getMessage(@NonNull final Bundle bundle) {
//        return bundle.getString(BUNDLE_EXTRA_STRING_MESSAGE);
//    }

    /**
     * Private constructor prevents instantiation
     *
     * @throws UnsupportedOperationException because this class cannot be instantiated.
     */
    private PluginBundleValues() {
        throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
    }

    public static String getBundleBlurb(Bundle bundle) {
        final StringBuilder stringBuilder = new StringBuilder();
        if (bundle.getString(BUNDLE_EXTRA_STRING_SELECTED_ROUTER_FROM_VAR) != null) {
            stringBuilder.append("- Router Variable Name : ")
                    .append(bundle.getString(BUNDLE_EXTRA_STRING_SELECTED_ROUTER_FROM_VAR));
        } else if (bundle.getString(BUNDLE_EXTRA_STRING_SELECTED_ROUTER_CANONICAL_NAME) != null) {
            stringBuilder.append("- Router : ")
                    .append(bundle.getString(BUNDLE_EXTRA_STRING_SELECTED_ROUTER_CANONICAL_NAME));
        }
        stringBuilder.append("\n\n");
        if (bundle.getString(BUNDLE_EXTRA_STRING_CMD_VAR) != null) {
            stringBuilder.append("- Command Variable Name : ")
                    .append(bundle.getString(BUNDLE_EXTRA_STRING_CMD_VAR));
        } else {
            stringBuilder.append("- Command : ");
            if (bundle.getBoolean(BUNDLE_EXTRA_STRING_CMD_CUSTOM, false)) {
                stringBuilder.append("Custom");
            } else {
                final String cmdStr = bundle.getString(BUNDLE_EXTRA_STRING_CMD);
                try {
                    final EditActivity.SupportedCommand supportedCommand =
                            EditActivity.SupportedCommand.valueOf(cmdStr);
                    stringBuilder.append(supportedCommand.humanReadableName);
                    if (supportedCommand.isConfigurable) {
                        if (supportedCommand.paramName != null) {
                            //Get param value
                            stringBuilder.append("\n").append(" - ")
                                    .append(supportedCommand.paramName).append(" : TODO");
                            //TODO Append actual param value
                        }
                    }
                } catch (final Exception e) {
                    //No worries
                }
            }
        }

        if (bundle.getString(BUNDLE_EXTRA_STRING_CMD_OUTPUT_VAR) != null) {
            stringBuilder.append("\n\n");
            stringBuilder.append("- Output Variable Name: ")
                    .append(bundle.getString(BUNDLE_EXTRA_STRING_CMD_OUTPUT_VAR));
        }

        return stringBuilder.toString();
    }

    public static Bundle generateBundle(Context context,
                                        CharSequence selectedRouterUuid,
                                        String selectedRouterReadableName,
                                        CharSequence routerVariableName,
                                        boolean commandVariableChecked,
                                        CharSequence commandName,
                                        boolean isCustomCommand,
                                        EditActivity.SupportedCommand command,
                                        boolean returnOutputChecked,
                                        CharSequence returnVarName) {

        assertNotNull(context, "context"); //$NON-NLS-1$

        final Bundle result = new Bundle();
        result.putInt(BUNDLE_EXTRA_INT_VERSION_CODE, AppBuildInfo.getVersionCode(context));
        if (isEmpty(selectedRouterUuid) && !isEmpty(routerVariableName)) {
            //Variable backed
            result.putString(BUNDLE_EXTRA_STRING_SELECTED_ROUTER_FROM_VAR, routerVariableName.toString());
        } else {
            //Router UUID
            result.putString(BUNDLE_EXTRA_STRING_SELECTED_ROUTER_UUID, selectedRouterUuid.toString());
            result.putString(BUNDLE_EXTRA_STRING_SELECTED_ROUTER_CANONICAL_NAME, selectedRouterReadableName);
        }

        if (commandVariableChecked && !isEmpty(commandName)) {
            //Variable backed
            result.putString(BUNDLE_EXTRA_STRING_CMD_VAR, commandName.toString());
        } else {
            result.putBoolean(BUNDLE_EXTRA_STRING_CMD_CUSTOM, isCustomCommand);
            if (isCustomCommand && commandName != null) {
                result.putString(BUNDLE_EXTRA_STRING_CMD, commandName.toString());
            } else {
                //Actual command along with the parameters
                if (command != null) {
                    result.putString(BUNDLE_EXTRA_STRING_CMD, command.toString());
                    //TODO Add parameters values
                }
            }
        }

        if (returnOutputChecked && !isEmpty(returnVarName)) {
            //Variable
            result.putString(BUNDLE_EXTRA_STRING_CMD_OUTPUT_VAR, returnVarName.toString());
        }

        return result;

    }
}
