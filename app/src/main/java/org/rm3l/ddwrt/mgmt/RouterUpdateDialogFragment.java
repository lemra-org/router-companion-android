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

package org.rm3l.ddwrt.mgmt;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
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

        if (d != null) {
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
            ((CheckBox) d.findViewById(R.id.router_add_is_strict_host_key_checking))
                    .setChecked(router.isStrictHostKeyChecking());
        }
    }

    @Nullable
    @Override
    protected Router onPositiveButtonClickHandler(@NotNull Router router) {
        return this.dao.updateRouter(router);
    }
}
