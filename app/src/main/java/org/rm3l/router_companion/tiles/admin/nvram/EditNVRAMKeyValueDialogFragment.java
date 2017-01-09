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

package org.rm3l.router_companion.tiles.admin.nvram;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.cocosw.undobar.UndoBarController;

import org.apache.commons.lang3.StringUtils;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.utils.ColorUtils;

import de.keyboardsurfer.android.widget.crouton.Crouton;

import static de.keyboardsurfer.android.widget.crouton.Style.ALERT;

public class EditNVRAMKeyValueDialogFragment extends DialogFragment {

    public static final String POSITION = "position";
    public static final String KEY = \"fake-key\";
    public static final String VALUE = "value";
    public static final String ACTION = "action";

    public static final int ADD = 1;
    public static final int EDIT = 2;

    private NVRAMDataRecyclerViewAdapter nvramDataRecyclerViewAdapter;
    private int mPosition;
    private CharSequence mKey;
    private CharSequence mValue;

    @NonNull
    public static EditNVRAMKeyValueDialogFragment newInstance(NVRAMDataRecyclerViewAdapter nvramDataRecyclerViewAdapter,
                                                              int position, CharSequence key, CharSequence value) {
        final EditNVRAMKeyValueDialogFragment fragment = new EditNVRAMKeyValueDialogFragment();

        fragment.nvramDataRecyclerViewAdapter = nvramDataRecyclerViewAdapter;

        final Bundle args = new Bundle();
        args.putInt(POSITION, position);
        args.putCharSequence(KEY, key);
        args.putCharSequence(VALUE, value);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final FragmentActivity fragmentActivity = getActivity();

        if (ColorUtils.isThemeLight(fragmentActivity)) {
            //Light
            fragmentActivity.setTheme(R.style.AppThemeLight);
//            fragmentActivity.getWindow().getDecorView()
//                    .setBackgroundColor(ContextCompat.getColor(fragmentActivity,
//                            android.R.color.white));
        } else {
            //Default is Dark
            fragmentActivity.setTheme(R.style.AppThemeDark);
        }

        final Bundle arguments = getArguments();
        this.mKey = \"fake-key\";
        this.mValue = arguments.getCharSequence(VALUE);
        this.mPosition = arguments.getInt(POSITION);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        // Get the layout inflater
        final LayoutInflater inflater = activity.getLayoutInflater();

        final View view = inflater.inflate(R.layout.tile_admin_nvram_edit, null);
        builder
                .setTitle(R.string.edit_nvram)
                .setMessage("NVRAM is the permanent settings storage. This includes: " +
                        "i) settings that you normally change using Web Interface, and " +
                        "ii) settings for user Startup Scripts. \n" +
                        "Variables edited here will be persisted in NVRAM right away. \n\n" +
                        "* SO DO NOT EDIT UNLESS YOU REALLY KNOW WHAT YOU ARE DOING! *")
                .setIcon(android.R.drawable.stat_sys_warning)
                .setView(view)
                        // Add action buttons
                .setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //Do nothing here because we override this button later to change the close behaviour.
                        //However, we still need this because on older versions of Android unless we
                        //pass a handler the button doesn't get instantiated
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        getDialog().cancel();
                    }
                });

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();    //super.onStart() is where dialog.show() is actually called on the underlying dialog, so we have to do it after this point

        final AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {

            ((TextView) d.findViewById(R.id.tile_admin_nvram_edit_key)).setText(this.mKey);
            final EditText valueEditText = (EditText) d.findViewById(R.id.tile_admin_nvram_edit_value);
//            valueEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//                @Override
//                public void onFocusChange(View v, boolean hasFocus) {
//                    ((TextView) d.findViewById(R.id.tile_admin_nvram_edit_value_textview))
//                            .setTypeface(null, hasFocus ? Typeface.BOLD_ITALIC : Typeface.NORMAL);
//                }
//            });
            valueEditText.setText(this.mValue, TextView.BufferType.EDITABLE);

            d.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Validate data
                    final EditText editText = (EditText) d.findViewById(R.id.tile_admin_nvram_edit_value);
                    final Editable newValue = editText.getText();

                    if (mValue != null && StringUtils.equals(newValue.toString(), mValue.toString())) {
                        //Crouton
                        Crouton.makeText(getActivity(), "No change", ALERT,
                                (ViewGroup) (d.findViewById(R.id.tile_admin_nvram_edit_notification_viewgroup))).show();
                        editText.requestFocus();
                        //Open Keyboard
                        final InputMethodManager imm = (InputMethodManager) getActivity()
                                .getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            // only will trigger it if no physical keyboard is open
                            imm.showSoftInput(editText, 0);
                        }
                        return;
                    }

                    final CharSequence variableKey = \"fake-key\";

                    final Bundle token = new Bundle();
                    token.putInt(POSITION, mPosition);
                    token.putCharSequence(VALUE, newValue);
                    token.putCharSequence(KEY, variableKey);
                    token.putInt(ACTION, EDIT);

                    //nvram set data changed
                    new UndoBarController.UndoBar(getActivity())
                            .message(String.format("Variable '%s' will be updated", variableKey))
                            .listener(nvramDataRecyclerViewAdapter)
                            .token(token)
                            .show();

                    d.cancel();
                }
            });
        }
    }

}
