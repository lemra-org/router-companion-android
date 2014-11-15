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

package org.lemra.dd_wrt.mgmt;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.google.common.base.Throwables;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.lemra.dd_wrt.R;
import org.lemra.dd_wrt.api.conn.Router;
import org.lemra.dd_wrt.mgmt.dao.DDWRTCompanionDAO;
import org.lemra.dd_wrt.utils.SSHUtils;
import org.lemra.dd_wrt.utils.Utils;

import java.io.File;
import java.nio.charset.Charset;
import java.sql.SQLException;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.isNullOrEmpty;
import static de.keyboardsurfer.android.widget.crouton.Style.ALERT;

public abstract class AbstractRouterMgmtDialogFragment
        extends SherlockDialogFragment
        implements AdapterView.OnItemSelectedListener {

    protected DDWRTCompanionDAO dao;
    private RouterMgmtDialogListener mListener;

    protected abstract CharSequence getDialogMessage();

    protected abstract CharSequence getDialogTitle();

    protected abstract CharSequence getPositiveButtonMsg();

    protected abstract void onPositiveButtonActionSuccess(@NotNull RouterMgmtDialogListener mListener, int position, boolean error);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.dao = RouterManagementActivity.getDao(getActivity());
        try {
            this.dao.open();
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        // Get the layout inflater
        final LayoutInflater inflater = activity.getLayoutInflater();
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final View view = inflater.inflate(R.layout.activity_router_add, null);
        ((Spinner) view.findViewById(R.id.router_add_proto)).setOnItemSelectedListener(this);

        builder
                .setMessage(this.getDialogMessage())
                .setView(view)
                        // Add action buttons
                .setPositiveButton(this.getPositiveButtonMsg(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //Do nothing here because we override this button later to change the close behaviour.
                        //However, we still need this because on older versions of Android unless we
                        //pass a handler the button doesn't get instantiated
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        AbstractRouterMgmtDialogFragment.this.getDialog().cancel();
                    }
                });
        if (!(this.getDialogTitle() == null || this.getDialogTitle().toString().isEmpty())) {
            builder.setTitle(this.getDialogTitle());
        }
        return builder.create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (RouterMgmtDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();    //super.onStart() is where dialog.show() is actually called on the underlying dialog, so we have to do it after this point

        final AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            final Button positiveButton = d.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    boolean validForm = validateForm(d);

                    if (validForm) {
                        // Check connection to router ...

                        new CheckRouterConnectionAsyncTask(((EditText) d.findViewById(R.id.router_add_ip)).getText().toString()).execute(d);
                    }
                    //else dialog stays open. 'Cancel' button can still close it.
                }

            });
        }
    }


    @Override
    public void onResume() {
        try {
            this.dao.open();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        this.dao.close();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        this.dao.close();
        super.onDestroy();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        this.dao.close();
        super.onDismiss(dialog);
    }

    @Nullable
    private Router doCheckConnectionToRouter(AlertDialog d) throws Exception {
        final Router router = new Router();
        final String uuid = ((EditText) d.findViewById(R.id.router_add_uuid)).getText().toString();
        if (!isNullOrEmpty(uuid)) {
            router.setUuid(uuid);
        }
        router.setName(((EditText) d.findViewById(R.id.router_add_name)).getText().toString());
        router.setRemoteIpAddress(((EditText) d.findViewById(R.id.router_add_ip)).getText().toString());
        router.setRemotePort(Integer.parseInt(((EditText) d.findViewById(R.id.router_add_port)).getText().toString()));
        router.setRouterConnectionProtocol(Router.RouterConnectionProtocol.valueOf(
                (((Spinner) d.findViewById(R.id.router_add_proto))).getSelectedItem().toString()
        ));
        router.setUsername(((EditText) d.findViewById(R.id.router_add_username)).getText().toString());
        router.setStrictHostKeyChecking(((CheckBox) d.findViewById(R.id.router_add_is_strict_host_key_checking)).isChecked());

        final String password = ((EditText) d.findViewById(R.id.router_add_password)).getText().toString();
        final String privkey = ((EditText) d.findViewById(R.id.router_add_privkey)).getText().toString();
        if (!isNullOrEmpty(password)) {
            router.setPassword(password);
        }
        if (!isNullOrEmpty(privkey)) {
            router.setPrivKey(FileUtils.readFileToString(new File(privkey), Charset.defaultCharset()));
        }

        //This will throw an exception if connection could not be established!
        SSHUtils.checkConnection(router, 10000);

        return router;
    }

    private boolean validateForm(AlertDialog d) {
        final EditText ipAddrView = (EditText) d.findViewById(R.id.router_add_ip);

        final Editable ipAddrViewText = ipAddrView.getText();

        if (!(Patterns.IP_ADDRESS.matcher(ipAddrViewText).matches()
                || Patterns.DOMAIN_NAME.matcher(ipAddrViewText).matches())) {
            displayMessage(getString(R.string.router_add_ip_invalid) + ":" + ipAddrViewText,
                    ALERT);
            ipAddrView.requestFocus();
            return false;
        }

        boolean validPort;
        final EditText portView = (EditText) d.findViewById(R.id.router_add_port);
        try {
            final String portStr = portView.getText().toString();
            validPort = (!isNullOrEmpty(portStr) && (Integer.parseInt(portStr) > 0));
        } catch (final Exception e) {
            e.printStackTrace();
            validPort = false;
        }
        if (!validPort) {
            displayMessage(getString(R.string.router_add_port_invalid) + ":" + portView.getText(), ALERT);
            ipAddrView.requestFocus();
            return false;
        }

        final EditText sshUsernameView = (EditText) d.findViewById(R.id.router_add_username);
        if (isNullOrEmpty(sshUsernameView.getText().toString())) {
            displayMessage(getString(R.string.router_add_username_invalid), ALERT);
            sshUsernameView.requestFocus();
            return false;
        }

        final EditText sshPasswordView = (EditText) d.findViewById(R.id.router_add_password);
        final EditText sshPrivKeyView = (EditText) d.findViewById(R.id.router_add_privkey);

        final boolean isPasswordEmpty = isNullOrEmpty(sshPasswordView.getText().toString());
        final boolean isPrivKeyEmpty = isNullOrEmpty(sshPrivKeyView.getText().toString());
        if (isPasswordEmpty && isPrivKeyEmpty) {
            displayMessage(getString(R.string.router_add_password_or_privkey_invalid), ALERT);
            sshPasswordView.requestFocus();
            return false;
        }

        return true;
    }

    private void displayMessage(final String msg, final Style style) {
        if (isNullOrEmpty(msg)) {
            return;
        }
        final AlertDialog d = (AlertDialog) getDialog();
        Crouton.makeText(getActivity(), msg, style, (ViewGroup) (d == null ? getView() : d.findViewById(R.id.router_add_notification_viewgroup))).show();
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
        //Since there is only one connection method for now, we won't do anything, but we may display only the relevant
        //form items, and hide the others.
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    protected abstract Router onPositiveButtonClickHandler(@NotNull final Router router);

    protected class CheckRouterConnectionAsyncTask extends AsyncTask<AlertDialog, Void, CheckRouterConnectionAsyncTask.CheckRouterConnectionAsyncTaskResult<Router>> {

        private final String routerIpOrDns;
        private AlertDialog checkingConnectionDialog = null;

        public CheckRouterConnectionAsyncTask(String routerIpOrDns) {
            this.routerIpOrDns = routerIpOrDns;
        }

        @Override
        protected void onPreExecute() {
            checkingConnectionDialog = Utils.buildAlertDialog(getActivity(), null,
                    String.format("Hold on - checking connection to router '%s'...", routerIpOrDns), false, false);
            checkingConnectionDialog.show();
        }

        @Override
        protected CheckRouterConnectionAsyncTask.CheckRouterConnectionAsyncTaskResult<Router> doInBackground(AlertDialog... dialogs) {
            Router result = null;
            Exception exception = null;
            try {
                result = doCheckConnectionToRouter(dialogs[0]);
            } catch (Exception e) {
                e.printStackTrace();
                exception = e;
            }
            return new CheckRouterConnectionAsyncTask.CheckRouterConnectionAsyncTaskResult<Router>(result, exception);
        }

        @Override
        protected void onPostExecute(CheckRouterConnectionAsyncTask.CheckRouterConnectionAsyncTaskResult<Router> result) {
            if (checkingConnectionDialog != null) {
                checkingConnectionDialog.cancel();
            }

            final Exception e = result.getException();
            Router router = result.getResult();
            if (e != null) {
                final Throwable rootCause = Throwables.getRootCause(e);
                displayMessage(getString(R.string.router_add_connection_unsuccessful) +
                        ": " + (rootCause != null ? rootCause.getMessage() : e.getMessage()), Style.ALERT);
            } else {
                if (router != null) {
                    AlertDialog daoAlertDialog = null;
                    try {
                        //Register or update router
                        daoAlertDialog = Utils.buildAlertDialog(getActivity(), null,
                                String.format("Registering (or updating) router '%s'...", routerIpOrDns), false, false);
                        daoAlertDialog.show();
                        router = AbstractRouterMgmtDialogFragment.this.onPositiveButtonClickHandler(router);
                        dismiss();
                    } finally {
                        if (daoAlertDialog != null) {
                            daoAlertDialog.cancel();
                        }
                    }
                } else {
                    displayMessage(getString(R.string.router_add_internal_error), Style.ALERT);
                }
            }

            if (AbstractRouterMgmtDialogFragment.this.mListener != null) {
                AbstractRouterMgmtDialogFragment.this.onPositiveButtonActionSuccess(
                        AbstractRouterMgmtDialogFragment.this.mListener,
                        router != null ? router.getId() : -1,
                        e != null);
            }

        }

        @Override
        protected void onCancelled(CheckRouterConnectionAsyncTask.CheckRouterConnectionAsyncTaskResult<Router> router) {
            super.onCancelled(router);
            if (checkingConnectionDialog != null) {
                checkingConnectionDialog.cancel();
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if (checkingConnectionDialog != null) {
                checkingConnectionDialog.cancel();
            }
        }

        class CheckRouterConnectionAsyncTaskResult<T> {
            private final T result;
            private final Exception exception;

            private CheckRouterConnectionAsyncTaskResult(T result, Exception exception) {
                this.result = result;
                this.exception = exception;
            }

            public T getResult() {
                return result;
            }

            public Exception getException() {
                return exception;
            }
        }

    }
}
