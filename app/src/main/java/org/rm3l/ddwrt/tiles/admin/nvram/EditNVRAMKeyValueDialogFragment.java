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

package org.rm3l.ddwrt.tiles.admin.nvram;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockDialogFragment;

import org.jetbrains.annotations.NotNull;
import org.rm3l.ddwrt.R;

public class EditNVRAMKeyValueDialogFragment extends SherlockDialogFragment {

    private NVRAMDataRecyclerViewAdapter nvramDataRecyclerViewAdapter;

    private static final String POSITION = "position";
    private static final String KEY = \"fake-key\";
    private static final String VALUE = "value";

    private int mPosition;
    private CharSequence mKey;
    private CharSequence mValue;

    @NotNull
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
        final Bundle arguments = getArguments();
        this.mKey = \"fake-key\";
        this.mValue = arguments.getCharSequence(VALUE);
        this.mPosition = arguments.getInt(POSITION);
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();

        @NotNull AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        // Get the layout inflater
        @NotNull final LayoutInflater inflater = activity.getLayoutInflater();

        final View view = inflater.inflate(R.layout.tile_admin_nvram_edit, null);
        builder
                .setTitle(R.string.edit_nvram)
                .setMessage("NVRAM is the permanent settings storage. This includes: " +
                        "i) settings that you normally change using Web Interface, and " +
                        "ii) settings for user Startup Scripts. \n" +
                        "Some variables may not be editable at all. \n\n" +
                        "* DO NOT EDIT UNLESS YOU KNOW WHAT YOU ARE DOING! *")
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
            valueEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    ((TextView) d.findViewById(R.id.tile_admin_nvram_edit_value_textview))
                            .setTypeface(null, hasFocus ? Typeface.BOLD_ITALIC : Typeface.NORMAL);
                }
            });
            valueEditText.setText(this.mValue);

            d.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //TODO
                    //Validate data
                    final String value = ((EditText) d.findViewById(R.id.tile_admin_nvram_edit_value)).getText().toString();

                    //nvram set data changed

                    Toast.makeText(getSherlockActivity(), "Positive Button clicked", Toast.LENGTH_SHORT).show();
                    d.cancel();
                }
            });
        }
    }
}
