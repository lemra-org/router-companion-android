package org.rm3l.router_companion.tiles.services.wol;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style.ALERT;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.actions.ActionManager;
import org.rm3l.router_companion.actions.RouterAction;
import org.rm3l.router_companion.actions.RouterActionListener;
import org.rm3l.router_companion.actions.WakeOnLANRouterAction;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.resources.Device;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style;

public class AddWOLHostDialogFragment extends DialogFragment {

    public static final String BROADCAST_ADDRESSES = "BROADCAST_ADDRESSES";

    public static final String WOL_PREF_KEY = \"fake-key\";

    public static final String ADD_OR_EDIT_WOLHOSTS_HOSTNAMES = "AddOrEditWOLHosts.Hostnames";

    public static final String ADD_OR_EDIT_WOL_HOSTS_PORTS = "AddOrEditWolHosts.Ports";

    public static final String ADD_OR_EDIT_WOL_HOSTS_MAC_ADDRESSES = "AddOrEditWolHosts.MacAddresses";

    private static final String LOG_TAG = AddWOLHostDialogFragment.class.getSimpleName();

    protected SharedPreferences mGlobalPreferences;

    protected String mPreferencesKey;

    @Nullable
    protected SharedPreferences mRouterPreferences;

    protected AlertDialog mWaitingDialog = null;

    @Nullable
    private ArrayList<String> bcastAddresses;

    private Device mDevice;

    @Nullable
    private Router mRouter;

    public static AddWOLHostDialogFragment newInstance(@NonNull final String routerUuid,
            @NonNull final ArrayList<String> bcastAddresses, @NonNull final String preferenceKey) {
        final AddWOLHostDialogFragment addWOLHostDialogFragment = new AddWOLHostDialogFragment();

        final Bundle args = new Bundle();
        args.putString(RouterManagementActivity.ROUTER_SELECTED, routerUuid);
        args.putStringArrayList(BROADCAST_ADDRESSES, bcastAddresses);
        args.putString(WOL_PREF_KEY, preferenceKey);

        addWOLHostDialogFragment.setArguments(args);

        return addWOLHostDialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final FragmentActivity activity = getActivity();

        mGlobalPreferences =
                activity.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);

        final Bundle arguments = getArguments();

        final String routerSelected = arguments.getString(RouterManagementActivity.ROUTER_SELECTED);

        if (routerSelected == null || routerSelected.isEmpty()) {
            Toast.makeText(activity, "Internal Error: unknown router. Please try again later",
                    Toast.LENGTH_SHORT).show();
            Utils.reportException(null, new IllegalStateException("Internal Error: unknown router"));
            dismiss();
        }

        mWaitingDialog =
                Utils.buildAlertDialog(getActivity(), null, "Sending magic packet...", false, false);

        mRouterPreferences = (routerSelected != null ? activity.getSharedPreferences(routerSelected,
                Context.MODE_PRIVATE) : null);

        mRouter = RouterManagementActivity.getDao(activity).getRouter(routerSelected);

        bcastAddresses = arguments.getStringArrayList(BROADCAST_ADDRESSES);

        mPreferencesKey = \"fake-key\";
    }

    @Override
    public void onStart() {
        super.onStart();    //super.onStart() is where dialog.show() is actually called on the underlying dialog, so we have to do it after this point

        final AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {

            if (mRouterPreferences != null) {
                //Set AutoComplete Fields data

                final AutoCompleteTextView wolHostNameView =
                        (AutoCompleteTextView) d.findViewById(R.id.wol_host_add_name);
                final Set<String> wolHostNames =
                        mRouterPreferences.getStringSet(ADD_OR_EDIT_WOLHOSTS_HOSTNAMES, new HashSet<String>());
                if (wolHostNames != null) {
                    wolHostNameView.setAdapter(
                            new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1,
                                    wolHostNames.toArray(new String[wolHostNames.size()])));
                }

                final AutoCompleteTextView wolHostMacAddrView =
                        (AutoCompleteTextView) d.findViewById(R.id.wol_host_add_mac_addr);
                final Set<String> wolHostMacAddresses =
                        mRouterPreferences.getStringSet(ADD_OR_EDIT_WOL_HOSTS_MAC_ADDRESSES,
                                new HashSet<String>());
                if (wolHostMacAddresses != null) {
                    wolHostMacAddrView.setAdapter(
                            new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1,
                                    wolHostMacAddresses.toArray(new String[wolHostMacAddresses.size()])));
                }

                final AutoCompleteTextView wolHostPortView =
                        (AutoCompleteTextView) d.findViewById(R.id.wol_host_add_port);
                final Set<String> wolHostPorts =
                        mRouterPreferences.getStringSet(ADD_OR_EDIT_WOL_HOSTS_PORTS, new HashSet<String>());
                if (wolHostPorts != null) {
                    wolHostPortView.setAdapter(
                            new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1,
                                    wolHostPorts.toArray(new String[wolHostPorts.size()])));
                }
            }

            final Button positiveButton = d.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    positiveButton.setEnabled(false);

                    try {
                        //Validate form
                        boolean validForm = validateForm(d);

                        if (validForm) {

                            mDevice = new Device(((EditText) d.findViewById(R.id.wol_host_add_mac_addr)).getText()
                                    .toString()
                                    .toLowerCase());
                            mDevice.setAlias(
                                    ((EditText) d.findViewById(R.id.wol_host_add_name)).getText().toString());
                            try {
                                mDevice.setWolPort(Integer.parseInt(
                                        ((EditText) d.findViewById(R.id.wol_host_add_port)).getText().toString()));
                            } catch (@NonNull final Exception e) {
                                //No worries
                            }

                            if (mRouterPreferences != null) {
                                final Set<String> existingHostNames = new HashSet<>(
                                        mRouterPreferences.getStringSet(ADD_OR_EDIT_WOLHOSTS_HOSTNAMES,
                                                new HashSet<String>()));
                                existingHostNames.add(mDevice.getAlias());

                                final Set<String> existingHostMacs = new HashSet<>(
                                        mRouterPreferences.getStringSet(ADD_OR_EDIT_WOL_HOSTS_MAC_ADDRESSES,
                                                new HashSet<String>()));
                                existingHostMacs.add(mDevice.getMacAddress());

                                final Set<String> existingHostPorts = new HashSet<>(
                                        mRouterPreferences.getStringSet(ADD_OR_EDIT_WOL_HOSTS_PORTS,
                                                new HashSet<String>()));
                                if (mDevice.getWolPort() > 0) {
                                    existingHostPorts.add(String.valueOf(mDevice.getWolPort()));
                                }

                                mRouterPreferences.edit()
                                        .putStringSet(ADD_OR_EDIT_WOLHOSTS_HOSTNAMES, existingHostNames)
                                        .putStringSet(ADD_OR_EDIT_WOL_HOSTS_MAC_ADDRESSES, existingHostMacs)
                                        .putStringSet(ADD_OR_EDIT_WOL_HOSTS_PORTS, existingHostPorts)
                                        .apply();

                                Utils.requestBackup(getActivity());
                            }

                            if (bcastAddresses == null) {
                                Utils.displayMessage(getActivity(),
                                        "WOL Internal Error: unable to fetch broadcast addresses. Try again later.",
                                        Style.ALERT);
                                Utils.reportException(null, new IllegalStateException(
                                        "WOL Internal Error: unable to fetch broadcast addresses. Try again later."));
                                return;
                            }

                            final AlertDialog d = (AlertDialog) getDialog();
                            if (d == null) {
                                displayMessage("WOL Internal Error. Try again later.", Style.ALERT);
                                Utils.reportException(null,
                                        new IllegalStateException("WOL Internal Error: Dialog is NULL."));
                                return;
                            }

                            if (mWaitingDialog == null) {
                                mWaitingDialog =
                                        Utils.buildAlertDialog(getActivity(), null, "Sending magic packet...", false,
                                                false);
                            }
                            mWaitingDialog.show();

                            ((TextView) mWaitingDialog.findViewById(android.R.id.message)).setGravity(
                                    Gravity.CENTER_HORIZONTAL);
                            ActionManager.runTasks(
                                    new WakeOnLANRouterAction(mRouter, getActivity(), getRouterActionListener(),
                                            mGlobalPreferences, mDevice, mDevice.getWolPort(),
                                            bcastAddresses.toArray(new String[bcastAddresses.size()])));
                        }
                        ///else dialog stays open. 'Cancel' button can still close it.
                    } catch (final Exception e) {
                        Utils.reportException(null, e);
                    } finally {
                        if (positiveButton != null) {
                            positiveButton.setEnabled(true);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        try {
            if (mWaitingDialog != null) {
                mWaitingDialog.cancel();
            }
        } catch (final Exception e) {
            Utils.reportException(null, e);
        }
        super.onCancel(dialog);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        // Get the layout inflater
        final LayoutInflater inflater = activity.getLayoutInflater();
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final View view = inflater.inflate(R.layout.activity_wol_host_add, null);

        builder.setMessage("For this to work properly:\n"
                + "- Target hardware must support WOL, which can be enabled in the BIOS or in the System Settings.\n"
                + "- WOL magic packet will be sent from the router. To wake over the Internet, "
                + "you must forward packets from any port you want to the device to wake.\n"
                + "Note: some devices support WOL only in Sleep or Hibernated mode, "
                + "not powered off. Some may also require a SecureOn password, which is not supported (yet)!")
                .setView(view)
                // Add action buttons
                .setPositiveButton("Send Magic Packet!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //Do nothing here because we override this button later to change the close behaviour.
                        //However, we still need this because on older versions of Android unless we
                        //pass a handler the button doesn't get instantiated
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        AddWOLHostDialogFragment.this.getDialog().cancel();
                    }
                });
        builder.setTitle("Add a new WOL Host");

        return builder.create();
    }

    @NonNull
    protected RouterActionListener getRouterActionListener() {
        return new RouterActionListener() {
            @Override
            public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull Router router,
                    @Nullable Exception exception) {
                try {
                    displayMessage(String.format("Error on action '%s': %s", routerAction.toString(),
                            Utils.handleException(exception).first), Style.ALERT);
                    Crashlytics.logException(exception);
                } finally {
                    if (mWaitingDialog != null) {
                        mWaitingDialog.cancel();
                    }
                }
            }

            @Override
            public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router,
                    Object returnData) {
                try {
                    final AlertDialog d = (AlertDialog) getDialog();
                    if (d == null) {
                        displayMessage("WOL Internal Error. Action succeeded, but failed to save entry",
                                Style.INFO);
                        Utils.reportException(null,
                                new IllegalStateException("WOL Internal Error: Dialog is NULL."));
                        return;
                    }

                    displayMessage(String.format("Action '%s' executed successfully on host '%s'",
                            routerAction.toString(), router.getRemoteIpAddress()), Style.CONFIRM);

                    if (mRouterPreferences == null) {
                        return;
                    }

                    final HashSet<String> wolHosts = new HashSet<>(
                            mRouterPreferences.getStringSet(mPreferencesKey, new HashSet<String>()));

                    mDevice.setDeviceUuidForWol(UUID.randomUUID().toString());

                    final Gson gson = WakeOnLanTile.GSON_BUILDER.create();
                    wolHosts.add(gson.toJson(mDevice));

                    mRouterPreferences.edit().putStringSet(mPreferencesKey, wolHosts).apply();
                } finally {
                    if (mWaitingDialog != null) {
                        mWaitingDialog.cancel();
                    }
                    dismiss();
                }
            }
        };
    }

    private void displayMessage(final String msg, final Style style) {
        if (isNullOrEmpty(msg)) {
            return;
        }
        final AlertDialog d = (AlertDialog) getDialog();
        Utils.displayMessage(getActivity(), msg, style, (ViewGroup) (d == null ? getView()
                : d.findViewById(R.id.wol_host_add_notification_viewgroup)));
    }

    private void openKeyboard(final TextView mTextView) {
        final InputMethodManager imm =
                (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            // only will trigger it if no physical keyboard is open
            imm.showSoftInput(mTextView, 0);
        }
    }

    private boolean validateForm(AlertDialog d) {
        final EditText macAddrView = (EditText) d.findViewById(R.id.wol_host_add_mac_addr);

        final Editable macAddrViewText = macAddrView.getText();
        if (Strings.isNullOrEmpty(macAddrViewText.toString())) {
            displayMessage("Invalid MAC Address:" + macAddrViewText, ALERT);
            macAddrView.requestFocus();
            openKeyboard(macAddrView);
            return false;
        }

        return true;
    }
}
