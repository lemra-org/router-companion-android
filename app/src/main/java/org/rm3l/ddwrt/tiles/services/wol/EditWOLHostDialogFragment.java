package org.rm3l.ddwrt.tiles.services.wol;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.actions.RouterAction;
import org.rm3l.ddwrt.actions.RouterActionListener;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.resources.Device;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * Created by rm3l on 08/04/15.
 */
public class EditWOLHostDialogFragment extends AddWOLHostDialogFragment {

    private static final String LOG_TAG =  EditWOLHostDialogFragment.class.getSimpleName();
    public static final String WOL_HOST = "WOL_HOST";

    private Device mDeviceToEdit;

    private String mDeviceToEditUuid;

    public static EditWOLHostDialogFragment newInstance(@NonNull final String routerUuid,
                                                        @NonNull final ArrayList<String> bcastAddresses,
                                                        @NonNull final String preferenceKey,
                                                        @NonNull final String deviceJsonString) {
        final EditWOLHostDialogFragment fragment = new EditWOLHostDialogFragment();

        final Bundle args = new Bundle();
        args.putString(RouterManagementActivity.ROUTER_SELECTED, routerUuid);
        args.putStringArrayList(BROADCAST_ADDRESSES, bcastAddresses);
        args.putString(WOL_PREF_KEY, preferenceKey);
        args.putString(WOL_HOST, deviceJsonString);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle arguments = getArguments();
        try {
            final String wolHostString = arguments.getString(WOL_HOST);
            mDeviceToEdit = WakeOnLanTile.GSON_BUILDER.create().fromJson(wolHostString, Device.class);
            if (mDeviceToEdit == null) {
                throw new IllegalArgumentException("Device string malformed: " + wolHostString);
            }
            mDeviceToEditUuid = mDeviceToEdit.getDeviceUuidForWol();
        } catch (final Exception e) {
            Utils.reportException(e);
            Toast.makeText(getActivity(), "Internal Error - please try again later.", Toast.LENGTH_SHORT).show();
            dismiss();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        final AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            d.setTitle("Edit WOL Host");
            ((EditText) d.findViewById(R.id.wol_host_add_name)).setText(mDeviceToEdit.getName(), TextView.BufferType.EDITABLE);
            ((EditText) d.findViewById(R.id.wol_host_add_mac_addr)).setText(mDeviceToEdit.getMacAddress(), TextView.BufferType.EDITABLE);
            final int wolPort = mDeviceToEdit.getWolPort();
            if (wolPort > 0) {
                ((EditText) d.findViewById(R.id.wol_host_add_port)).setText(String.valueOf(wolPort),
                        TextView.BufferType.EDITABLE);
            }
        }
    }

    @NonNull
    @Override
    protected RouterActionListener getRouterActionListener() {
        return new RouterActionListener() {
            @Override
            public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router, Object returnData) {
                //Save (and dismiss)
                try {
                    final AlertDialog d = (AlertDialog) getDialog();
                    final FragmentActivity activity = getActivity();
                    if (d == null) {
                        Utils.displayMessage(activity,
                                "WOL Internal Error. Action succeeded, but failed to save entry",
                                Style.INFO);
                        Utils.reportException(new IllegalStateException("WOL Internal Error: Dialog is NULL."));
                        return;
                    }

                    Utils.displayMessage(activity,
                            String.format("Action '%s' executed successfully on host '%s'", routerAction.toString(), router.getRemoteIpAddress()),
                            Style.CONFIRM);

                    if (mRouterPreferences == null) {
                        return;
                    }

                    mDeviceToEdit = new Device(((EditText) d.findViewById(R.id.wol_host_add_mac_addr)).getText().toString());
                    mDeviceToEdit.setAlias(((EditText) d.findViewById(R.id.wol_host_add_name)).getText().toString());
                    try {
                        mDeviceToEdit.setWolPort(Integer.parseInt(((EditText) d.findViewById(R.id.wol_host_add_port)).getText().toString()));
                    } catch (@NonNull final Exception e) {
                        e.printStackTrace();
                    }
                    mDeviceToEdit.setDeviceUuidForWol(mDeviceToEditUuid);

                    final Set<String> wolHosts = mRouterPreferences.getStringSet(mPreferencesKey, new HashSet<String>());
                    final Set<String> newWolHosts = new HashSet<>();
                    newWolHosts.add(WakeOnLanTile.GSON_BUILDER.create().toJson(mDeviceToEdit));
                    if (wolHosts != null) {
                        for (final String wolHost : wolHosts) {
                            if (StringUtils.containsIgnoreCase(wolHost, mDeviceToEditUuid)) {
                                continue;
                            }
                            newWolHosts.add(wolHost);
                        }
                    }

                    mRouterPreferences.edit()
                            .putStringSet(mPreferencesKey, newWolHosts)
                            .apply();

                    Utils.requestBackup(activity);

                } catch (final Exception e) {
                    Utils.reportException(e);
                } finally {
                    if (mWaitingDialog != null) {
                        mWaitingDialog.cancel();
                    }
                    dismiss();
                }
            }

            @Override
            public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull Router router, @Nullable Exception exception) {
                try {
                    Utils.displayMessage(getActivity(),
                            String.format("Error on action '%s': %s", routerAction.toString(), ExceptionUtils.getRootCauseMessage(exception)),
                            Style.ALERT);
                    Utils.reportException(exception);
                } finally {
                    if (mWaitingDialog != null) {
                        mWaitingDialog.cancel();
                    }
                }
            }
        };
    }

}
