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
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.resources.conn.Router;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

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
    protected void onPositiveButtonActionSuccess(@NotNull RouterMgmtDialogListener mListener, @Nullable Router router, boolean error) {
        final int position = (router != null ? router.getId() : -1);
        if (position >= 0) {
            mListener.onRouterUpdated(this, position, error);
        }
        if (!error) {
            Crouton.makeText(getActivity(), "Item updated", Style.CONFIRM).show();
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

        @NotNull final AlertDialog d = (AlertDialog) getDialog();

        if (router == null) {
            Toast.makeText(getActivity(), "Router not found - closing form...", Toast.LENGTH_LONG).show();
            if (d != null) {
                d.cancel();
            }
            return;
        }

        //This is an update - fill the form with the items fetched from the Router selected
        if (isUpdate()) {
            ((EditText) d.findViewById(R.id.router_add_uuid)).setText(router.getUuid());
        }
        ((EditText) d.findViewById(R.id.router_add_name)).setText(router.getName());
        ((EditText) d.findViewById(R.id.router_add_ip)).setText(router.getRemoteIpAddress());
        ((EditText) d.findViewById(R.id.router_add_port)).setText(String.valueOf(router.getRemotePort()));
        @NotNull final Spinner protoDropdown = (Spinner) d.findViewById(R.id.router_add_proto);
        switch (router.getRouterConnectionProtocol()) {
            case SSH:
                protoDropdown.setSelection(0);
                break;
            default:
                break;
        }
        ((EditText) d.findViewById(R.id.router_add_username)).setText(router.getUsernamePlain());
        ((EditText) d.findViewById(R.id.router_add_password)).setText(router.getPasswordPlain());
        ((TextView) d.findViewById(R.id.router_add_privkey_path)).setText(router.getPrivKeyPlain());

        final Router.SSHAuthenticationMethod sshAuthenticationMethod = router.getSshAuthenticationMethod();
        if (sshAuthenticationMethod != null) {
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
        }
//            ((CheckBox) d.findViewById(R.id.router_add_is_strict_host_key_checking))
//                    .setChecked(router.isStrictHostKeyChecking());

    }

    @Nullable
    @Override
    protected Router onPositiveButtonClickHandler(@NotNull Router router) {
        return this.dao.updateRouter(router);
    }
}
