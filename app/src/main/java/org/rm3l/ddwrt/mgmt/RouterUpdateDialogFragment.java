/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014  Armel Soro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact Info: Armel Soro <apps+ddwrt@rm3l.org>
 */

package org.rm3l.ddwrt.mgmt;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.Utils;

import java.util.Collection;

import de.keyboardsurfer.android.widget.crouton.Style;

import static android.widget.TextView.BufferType.EDITABLE;

public class RouterUpdateDialogFragment extends AbstractRouterMgmtDialogFragment {

    @Nullable
    private Router router;

    @Override
    protected CharSequence getDialogMessage() {
        return getString(R.string.router_update_msg);
    }

    @Nullable
    @Override
    protected CharSequence getDialogTitle() {
        return null;
    }

    @Override
    protected CharSequence getPositiveButtonMsg() {
        return getString(R.string.update_router);
    }

    @Override
    protected void onPositiveButtonActionSuccess(@NonNull RouterMgmtDialogListener mListener, @Nullable Router router, boolean error) {
        final int position = (router != null ? router.getId() : -1);

        mListener.onRouterUpdated(this, position, router, error);

        final FragmentActivity activity = getActivity();

        if (error) {
            Utils.displayMessage(activity, "Error while trying to update item - please try again later.",
                    Style.ALERT);
        } else {
            //Request Backup
            Utils.requestBackup(activity);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        router = this.dao.getRouter(getArguments().getString(RouterManagementActivity.ROUTER_SELECTED));
    }

    protected boolean isUpdate() {
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();    //super.onStart() is where dialog.show() is actually called on the underlying dialog, so we have to do it after this point

        if (!mActivityCreatedAndInitialized.get()) {

            final AlertDialog d = (AlertDialog) getDialog();

            final FragmentActivity activity = getActivity();
            if (router == null) {
                Toast.makeText(activity, "Router not found - closing form...", Toast.LENGTH_LONG).show();
                d.cancel();
                return;
            }

            //This is an update - fill the form with the items fetched from the Router selected
            if (isUpdate()) {
                ((TextView) d.findViewById(R.id.router_add_uuid)).setText(router.getUuid());
            }
            ((EditText) d.findViewById(R.id.router_add_name)).setText(router.getName(), EDITABLE);
            ((EditText) d.findViewById(R.id.router_add_ip)).setText(router.getRemoteIpAddress(), EDITABLE);
            ((EditText) d.findViewById(R.id.router_add_port)).setText(String.valueOf(router.getRemotePort()), EDITABLE);
            final Spinner protoDropdown = (Spinner) d.findViewById(R.id.router_add_proto);
            switch (router.getRouterConnectionProtocol()) {
                case SSH:
                    protoDropdown.setSelection(0);
                    break;
                default:
                    break;
            }

            final Spinner fwDropdown = (Spinner) d.findViewById(R.id.router_add_firmware);
            final Router.RouterFirmware routerFirmware = router.getRouterFirmware();
            if (routerFirmware == null) {
                //Auto-detect
                fwDropdown.setSelection(0);
            } else {
                //FIXME Fix when other firmwares are supported
                fwDropdown.setSelection(0);
//            switch (routerFirmware) {
//                case DDWRT:
//                    fwDropdown.setSelection(1);
//                    break;
//                case OPENWRT:
//                    fwDropdown.setSelection(2);
//                    break;
//                default:
//                    break;
//            }
            }

            ((EditText) d.findViewById(R.id.router_add_username)).setText(router.getUsernamePlain(), EDITABLE);
            ((EditText) d.findViewById(R.id.router_add_password)).setText(router.getPasswordPlain(), EDITABLE);
            ((TextView) d.findViewById(R.id.router_add_privkey_path)).setText(router.getPrivKeyPlain());

            final Router.SSHAuthenticationMethod sshAuthenticationMethod = router.getSshAuthenticationMethod();
            switch (sshAuthenticationMethod) {
                case NONE:
                    ((RadioButton) d.findViewById(R.id.router_add_ssh_auth_method_none))
                            .setChecked(true);
                    break;
                case PASSWORD:
                    ((RadioButton) d.findViewById(R.id.router_add_ssh_auth_method_password))
                            .setChecked(true);
                    break;
                case PUBLIC_PRIVATE_KEY:
                    ((RadioButton) d.findViewById(R.id.router_add_ssh_auth_method_privkey))
                            .setChecked(true);
                    break;
            }
//            ((CheckBox) d.findViewById(R.id.router_add_is_strict_host_key_checking))
//                    .setChecked(router.isStrictHostKeyChecking());

            final boolean useLocalSSIDLookup = router.isUseLocalSSIDLookup(activity);

            ((CheckBox) d.findViewById(R.id.router_add_local_ssid_lookup))
                    .setChecked(useLocalSSIDLookup);

            ((CheckBox) d.findViewById(R.id.router_add_fallback_to_primary))
                    .setChecked(router.isFallbackToPrimaryAddr(activity));

            final LinearLayout container = (LinearLayout) d.findViewById(R.id.router_add_local_ssid_container);
            final Collection<Router.LocalSSIDLookup> localSSIDLookupData =
                    router.getLocalSSIDLookupData(activity);
            for (final Router.LocalSSIDLookup localSSIDLookup : localSSIDLookupData) {
                if (localSSIDLookup == null) {
                    continue;
                }
                final TextView localSsidView = new TextView(activity);
                localSsidView.setText(localSSIDLookup.getNetworkSsid() + "\n" +
                        localSSIDLookup.getReachableAddr() + "\n" +
                        localSSIDLookup.getPort());
                localSsidView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                localSsidView
                        .setCompoundDrawablesWithIntrinsicBounds(
                                0, 0, android.R.drawable.ic_menu_close_clear_cancel, 0);

                localSsidView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        final int DRAWABLE_RIGHT = 2;

                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            if (event.getRawX() >= (localSsidView.getRight() -
                                    localSsidView.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {

                                //Remove view from container layout
                                final ViewParent parent = localSsidView.getParent();
                                if (parent instanceof LinearLayout) {
                                    ((LinearLayout)parent).removeView(localSsidView);
                                }
                            }
                        }
                        return true;
                    }
                });

                container.addView(localSsidView);
                final View lineView = Utils.getLineView(activity);
                if (lineView != null) {
                    container.addView(lineView);
                }
            }

            if (useLocalSSIDLookup) {
                //Perform click programmatically
                d.findViewById(R.id.router_add_advanced_options_button)
                        .performClick();
            }

            mActivityCreatedAndInitialized.set(true);
        }

    }

    @Nullable
    @Override
    protected Router onPositiveButtonClickHandler(@NonNull Router router) {
        return this.dao.updateRouter(router);
    }
}
