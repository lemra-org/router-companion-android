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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import com.cocosw.undobar.UndoBarController;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.Utils;

import static org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style.ALERT;
import static org.rm3l.router_companion.tiles.admin.nvram.EditNVRAMKeyValueDialogFragment.ACTION;

public class AddNVRAMKeyValueDialogFragment extends DialogFragment {

  public static final String KEY = \"fake-key\";
  public static final String VALUE = "value";
  private UndoBarController.UndoListener undoListener;

  @NonNull public static AddNVRAMKeyValueDialogFragment newInstance(
      UndoBarController.UndoListener undoListener) {
    final AddNVRAMKeyValueDialogFragment fragment = new AddNVRAMKeyValueDialogFragment();

    fragment.undoListener = undoListener;

    final Bundle args = new Bundle();
    fragment.setArguments(args);

    return fragment;
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final FragmentActivity fragmentActivity = getActivity();

    ColorUtils.Companion.setAppTheme(fragmentActivity, null, false);
    //
    //        if (ColorUtils.isThemeLight(fragmentActivity)) {
    //            //Light
    //            fragmentActivity.setTheme(R.style.AppThemeLight);
    //        } else {
    //            //Default is Dark
    //            fragmentActivity.setTheme(R.style.AppThemeDark);
    //        }

    final Bundle arguments = getArguments();
  }

  @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    final FragmentActivity activity = getActivity();

    final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

    // Get the layout inflater
    final LayoutInflater inflater = activity.getLayoutInflater();

    final View view = inflater.inflate(R.layout.tile_admin_nvram_add, null);
    builder.setTitle(R.string.add_nvram)
        .setMessage("NVRAM is the permanent settings storage. This includes: "
            + "i) settings that you normally change using Web Interface, and "
            + "ii) settings for user Startup Scripts. \n"
            + "Variables edited here will be persisted in NVRAM right away. \n\n"
            + "* SO DO NOT EDIT UNLESS YOU REALLY KNOW WHAT YOU ARE DOING! *")
        .setIcon(android.R.drawable.stat_sys_warning)
        .setView(view)
        // Add action buttons
        .setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int id) {
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

  @Override public void onStart() {
    super.onStart();    //super.onStart() is where dialog.show() is actually called on the underlying dialog, so we have to do it after this point

    final AlertDialog d = (AlertDialog) getDialog();
    if (d != null) {

      d.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View view) {
          //Validate data
          final TextView varKeyTV = (TextView) d.findViewById(R.id.tile_admin_nvram_add_key);
          final CharSequence variableKey = \"fake-key\";
          if (TextUtils.isEmpty(variableKey)) {
            //Error
            Utils.displayMessage(getActivity(), "Missing key for NVRAM variable", ALERT,
                (ViewGroup) (d.findViewById(R.id.tile_admin_nvram_add_notification_viewgroup)));
            varKeyTV.requestFocus();
            //Open Keyboard
            final InputMethodManager imm =
                (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
              // only will trigger it if no physical keyboard is open
              imm.showSoftInput(varKeyTV, 0);
            }
            return;
          }

          final CharSequence variableValue =
              ((EditText) d.findViewById(R.id.tile_admin_nvram_add_value)).getText();

          final Bundle token = new Bundle();
          token.putCharSequence(VALUE, variableValue);
          token.putCharSequence(KEY, variableKey);
          token.putInt(ACTION, EditNVRAMKeyValueDialogFragment.ADD);

          new UndoBarController.UndoBar(getActivity()).message(
              String.format("Variable '%s' will be added", variableKey))
              .listener(undoListener)
              .token(token)
              .show();

          d.cancel();
        }
      });
    }
  }
}
