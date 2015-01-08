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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.io.IOException;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.isNullOrEmpty;
import static de.keyboardsurfer.android.widget.crouton.Style.ALERT;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.ALWAYS_CHECK_CONNECTION_PREF_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.MAX_PRIVKEY_SIZE_BYTES;
import static org.rm3l.ddwrt.utils.Utils.toHumanReadableByteCount;

public abstract class AbstractRouterMgmtDialogFragment
        extends SherlockDialogFragment
        implements AdapterView.OnItemSelectedListener {

    private static final String LOG_TAG = AbstractRouterMgmtDialogFragment.class.getSimpleName();
    private static final int READ_REQUEST_CODE = 42;
    protected DDWRTCompanionDAO dao;
    private RouterMgmtDialogListener mListener;

    protected abstract CharSequence getDialogMessage();

    @org.jetbrains.annotations.Nullable
    protected abstract CharSequence getDialogTitle();

    protected abstract CharSequence getPositiveButtonMsg();

    protected abstract void onPositiveButtonActionSuccess(@NotNull RouterMgmtDialogListener mListener, @Nullable Router router, boolean error);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.dao = RouterManagementActivity.getDao(getActivity());
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();

        @NotNull AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        // Get the layout inflater
        @NotNull final LayoutInflater inflater = activity.getLayoutInflater();
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final View view = inflater.inflate(R.layout.activity_router_add, null);
        ((Spinner) view.findViewById(R.id.router_add_proto)).setOnItemSelectedListener(this);

        ((RadioGroup) view.findViewById(R.id.router_add_ssh_auth_method))
                .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        final View privkeyHdrView = view.findViewById(R.id.router_add_privkey_hdr);
                        final Button privkeyView = (Button) view.findViewById(R.id.router_add_privkey);
                        final TextView privkeyPathView = (TextView) view.findViewById(R.id.router_add_privkey_path);
                        final TextView pwdHdrView = (TextView) view.findViewById(R.id.router_add_password_hdr);
                        final EditText pwdView = (EditText) view.findViewById(R.id.router_add_password);

                        switch (checkedId) {
                            case R.id.router_add_ssh_auth_method_none:
                                privkeyPathView.setText(null);
                                privkeyHdrView.setVisibility(View.GONE);
                                privkeyView.setVisibility(View.GONE);
                                pwdHdrView.setVisibility(View.GONE);
                                pwdView.setText(null);
                                pwdView.setVisibility(View.GONE);
                                break;
                            case R.id.router_add_ssh_auth_method_password:
                                privkeyPathView.setText(null);
                                privkeyHdrView.setVisibility(View.GONE);
                                privkeyView.setVisibility(View.GONE);
                                pwdHdrView.setText("Password");
                                pwdHdrView.setVisibility(View.VISIBLE);
                                pwdView.setVisibility(View.VISIBLE);
                                pwdView.setHint("e.g., 'default' (may be empty) ");
                                break;
                            case R.id.router_add_ssh_auth_method_privkey:
                                pwdView.setText(null);
                                pwdHdrView.setText("Passphrase (if applicable)");
                                pwdHdrView.setVisibility(View.VISIBLE);
                                pwdView.setVisibility(View.VISIBLE);
                                pwdView.setHint("Key passphrase, if applicable");
                                privkeyHdrView.setVisibility(View.VISIBLE);
                                privkeyView.setVisibility(View.VISIBLE);
                                break;
                            default:
                                break;
                        }
                    }
                });

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

    /**
     * Receive the result from a previous call to
     * {@link #startActivityForResult(android.content.Intent, int)}.  This follows the
     * related Activity API as described there in
     * {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}.
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param resultData  An Intent, which can return result data to the caller
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            Uri uri;
            if (resultData != null) {
                uri = resultData.getData();
                Log.i(LOG_TAG, "Uri: " + uri.toString());
                final AlertDialog d = (AlertDialog) getDialog();
                if (d != null) {

                    final ContentResolver contentResolver = this.getSherlockActivity().getContentResolver();

                    final Cursor uriCursor =
                            contentResolver.query(uri, null, null, null, null);

                    /*
                     * Get the column indexes of the data in the Cursor,
                     * move to the first row in the Cursor, get the data,
                     * and display it.
                     */
                    final int nameIndex = uriCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    final int sizeIndex = uriCursor.getColumnIndex(OpenableColumns.SIZE);

                    uriCursor.moveToFirst();

                    //File size in bytes
                    final long fileSize = uriCursor.getLong(sizeIndex);
                    final String filename = uriCursor.getString(nameIndex);

                    //Check file size
                    if (fileSize > MAX_PRIVKEY_SIZE_BYTES) {
                        displayMessage(String
                                .format("File '%s' too big (%s). Limit is %s", filename,
                                        toHumanReadableByteCount(fileSize),
                                        toHumanReadableByteCount(MAX_PRIVKEY_SIZE_BYTES)), ALERT);
                        return;
                    }

                    //Replace button hint message with file name
                    final Button fileSelectorButton = (Button) d.findViewById(R.id.router_add_privkey);
                    final CharSequence fileSelectorOriginalHint = fileSelectorButton.getHint();
                    if (!Strings.isNullOrEmpty(filename)) {
                        fileSelectorButton.setHint(filename);
                    }

                    //Set file actual content in hidden field
                    final TextView privKeyPath = (TextView) d.findViewById(R.id.router_add_privkey_path);
                    try {
                        privKeyPath.setText(IOUtils.toString(contentResolver.openInputStream(uri)));
                    } catch (IOException e) {
                        displayMessage("Error: " + e.getMessage(), ALERT);
                        e.printStackTrace();
                        fileSelectorButton.setHint(fileSelectorOriginalHint);
                    }
                }
            }
        }
    }

    @Override
    public void onAttach(@NotNull Activity activity) {
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

            d.findViewById(R.id.router_add_privkey).setOnClickListener(new View.OnClickListener() {
                @TargetApi(Build.VERSION_CODES.KITKAT)
                @Override
                public void onClick(View view) {
                    //Open up file picker

                    // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
                    // browser.
                    final Intent intent = new Intent();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                    } else {
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                    }

                    // Filter to only show results that can be "opened", such as a
                    // file (as opposed to a list of contacts or timezones)
                    intent.addCategory(Intent.CATEGORY_OPENABLE);

                    // search for all documents available via installed storage providers
                    intent.setType("*/*");

                    AbstractRouterMgmtDialogFragment.this.startActivityForResult(intent, READ_REQUEST_CODE);
                }
            });

            d.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Validate form
                    boolean validForm = validateForm(d);

                    if (validForm) {
                        // Now check actual connection to router ...
                        new CheckRouterConnectionAsyncTask(
                                ((EditText) d.findViewById(R.id.router_add_ip)).getText().toString(),
                                getSherlockActivity().getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
                                        .getBoolean(ALWAYS_CHECK_CONNECTION_PREF_KEY, true))
                             .execute(d);
                    }
                    ///else dialog stays open. 'Cancel' button can still close it.
                }
            });
        }
    }

    @Nullable
    private Router doCheckConnectionToRouter(@NotNull AlertDialog d) throws Exception {
        @NotNull final Router router = buildRouter(d);

        //This will throw an exception if connection could not be established!
        SSHUtils.checkConnection(router, 10000);

        return router;
    }

    private static Router buildRouter(AlertDialog d) {
        @NotNull final Router router = new Router();
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
        router.setUsername(((EditText) d.findViewById(R.id.router_add_username)).getText().toString(), true);
//        router.setStrictHostKeyChecking(((CheckBox) d.findViewById(R.id.router_add_is_strict_host_key_checking)).isChecked());
        router.setStrictHostKeyChecking(false);

        final String password = ((EditText) d.findViewById(R.id.router_add_password)).getText().toString();
        final String privkey = ((TextView) d.findViewById(R.id.router_add_privkey_path)).getText().toString();
        if (!isNullOrEmpty(password)) {
            router.setPassword(password, true);
        }
        if (!isNullOrEmpty(privkey)) {
            router.setPrivKey(privkey, true);
        }
        return router;
    }

    private boolean validateForm(@NotNull AlertDialog d) {
        @NotNull final EditText ipAddrView = (EditText) d.findViewById(R.id.router_add_ip);

        final Editable ipAddrViewText = ipAddrView.getText();

        if (!(Patterns.IP_ADDRESS.matcher(ipAddrViewText).matches()
                || Patterns.DOMAIN_NAME.matcher(ipAddrViewText).matches())) {
            displayMessage(getString(R.string.router_add_dns_or_ip_invalid) + ":" + ipAddrViewText,
                    ALERT);
            ipAddrView.requestFocus();
            openKeyboard(ipAddrView);
            return false;
        }

        boolean validPort;
        @NotNull final EditText portView = (EditText) d.findViewById(R.id.router_add_port);
        try {
            final String portStr = portView.getText().toString();
            validPort = (!isNullOrEmpty(portStr) && (Integer.parseInt(portStr) > 0));
        } catch (@NotNull final Exception e) {
            e.printStackTrace();
            validPort = false;
        }
        if (!validPort) {
            displayMessage(getString(R.string.router_add_port_invalid) + ":" + portView.getText(), ALERT);
            portView.requestFocus();
            openKeyboard(portView);
            return false;
        }

        @NotNull final EditText sshUsernameView = (EditText) d.findViewById(R.id.router_add_username);
        if (isNullOrEmpty(sshUsernameView.getText().toString())) {
            displayMessage(getString(R.string.router_add_username_invalid), ALERT);
            sshUsernameView.requestFocus();
            openKeyboard(sshUsernameView);
            return false;
        }

//        @NotNull final EditText sshPasswordView = (EditText) d.findViewById(R.id.router_add_password);
//        @NotNull final TextView sshPrivKeyView = (TextView) d.findViewById(R.id.router_add_privkey_path);
//
//        final boolean isPasswordEmpty = isNullOrEmpty(sshPasswordView.getText().toString());
//        final boolean isPrivKeyEmpty = isNullOrEmpty(sshPrivKeyView.getText().toString());
//        if (isPasswordEmpty && isPrivKeyEmpty) {
//            displayMessage(getString(R.string.router_add_password_or_privkey_invalid), ALERT);
//            sshPasswordView.requestFocus();
//            openKeyboard(sshPasswordView);
//            return false;
//        }

        return true;
    }

    private void openKeyboard(final TextView mTextView) {
        final InputMethodManager imm = (InputMethodManager)
                getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            // only will trigger it if no physical keyboard is open
            imm.showSoftInput(mTextView, 0);
        }
    }

    private void displayMessage(final String msg, final Style style) {
        if (isNullOrEmpty(msg)) {
            return;
        }
        @org.jetbrains.annotations.Nullable final AlertDialog d = (AlertDialog) getDialog();
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

    @org.jetbrains.annotations.Nullable
    protected abstract Router onPositiveButtonClickHandler(@NotNull final Router router);

    protected class CheckRouterConnectionAsyncTask extends AsyncTask<AlertDialog, Void, CheckRouterConnectionAsyncTask.CheckRouterConnectionAsyncTaskResult<Router>> {

        private final String routerIpOrDns;
        @org.jetbrains.annotations.Nullable
        private AlertDialog checkingConnectionDialog = null;
        private boolean checkActualConnection;

        public CheckRouterConnectionAsyncTask(String routerIpOrDns, boolean checkActualConnection) {
            this.routerIpOrDns = routerIpOrDns;
            this.checkActualConnection = checkActualConnection;
        }

        @Override
        protected void onPreExecute() {
            if (checkActualConnection) {
                checkingConnectionDialog = Utils.buildAlertDialog(getActivity(), null,
                        String.format("Hold on - checking connection to router '%s'...", routerIpOrDns), false, false);
                checkingConnectionDialog.show();
            }
        }

        @org.jetbrains.annotations.Nullable
        @Override
        protected CheckRouterConnectionAsyncTask.CheckRouterConnectionAsyncTaskResult<Router> doInBackground(AlertDialog... dialogs) {
            if (!checkActualConnection) {
                return new CheckRouterConnectionAsyncTaskResult<>(buildRouter(dialogs[0]), null);
            }
            @org.jetbrains.annotations.Nullable Router result = null;
            @org.jetbrains.annotations.Nullable Exception exception = null;
            try {
                result = doCheckConnectionToRouter(dialogs[0]);
            } catch (Exception e) {
                e.printStackTrace();
                exception = e;
            }
            return new CheckRouterConnectionAsyncTask.CheckRouterConnectionAsyncTaskResult<>(result, exception);
        }

        @Override
        protected void onPostExecute(@NotNull CheckRouterConnectionAsyncTask.CheckRouterConnectionAsyncTaskResult<Router> result) {
            if (checkingConnectionDialog != null) {
                checkingConnectionDialog.cancel();
            }

            final Exception e = result.getException();
            @org.jetbrains.annotations.Nullable Router router = result.getResult();
            if (e != null) {
                final Throwable rootCause = Throwables.getRootCause(e);
                displayMessage(getString(R.string.router_add_connection_unsuccessful) +
                        ": " + (rootCause != null ? rootCause.getMessage() : e.getMessage()), Style.ALERT);
            } else {
                if (router != null) {
                    @org.jetbrains.annotations.Nullable AlertDialog daoAlertDialog = null;
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
                        AbstractRouterMgmtDialogFragment.this.mListener, router, e != null);
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
