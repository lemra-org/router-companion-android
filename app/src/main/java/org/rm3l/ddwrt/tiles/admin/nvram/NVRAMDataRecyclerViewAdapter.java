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
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.cocosw.undobar.UndoBarController;
import com.google.common.collect.ImmutableMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static de.keyboardsurfer.android.widget.crouton.Crouton.makeText;
import static java.util.Map.Entry;
import static org.rm3l.ddwrt.tiles.admin.nvram.EditNVRAMKeyValueDialogFragment.KEY;
import static org.rm3l.ddwrt.tiles.admin.nvram.EditNVRAMKeyValueDialogFragment.POSITION;
import static org.rm3l.ddwrt.tiles.admin.nvram.EditNVRAMKeyValueDialogFragment.VALUE;

public class NVRAMDataRecyclerViewAdapter extends RecyclerView.Adapter<NVRAMDataRecyclerViewAdapter.ViewHolder>
        implements UndoBarController.AdvancedUndoListener {

    private final FragmentActivity context;
    private final List<Entry<Object,Object>> entryList = new ArrayList<>();
    private Map<Object, Object> nvramInfo;
    private final FragmentManager fragmentManager;
    private final Router router;

    public NVRAMDataRecyclerViewAdapter(FragmentActivity context, Router router, NVRAMInfo nvramInfo) {
        this.context = context;
        this.router = router;
        this.fragmentManager = context.getSupportFragmentManager();
        //noinspection ConstantConditions
        this.setEntryList(nvramInfo.getData());
    }

    @Nullable
    public Map<Object, Object> getNvramInfo() {
        return nvramInfo;
    }

    public void setEntryList(@NotNull final Map<Object, Object> nvramInfo) {
        this.entryList.clear();
        this.nvramInfo = nvramInfo;
        //Not needed at this point - so make sure value has been read prior to calling this method
        nvramInfo.remove(AdminNVRAMTile.NVRAM_SIZE);
        this.nvramInfo = nvramInfo;
        //noinspection ConstantConditions
        for (final Entry<Object,Object> entry : nvramInfo.entrySet()) {
            if (entry.getKey() == null || isNullOrEmpty(entry.getKey().toString())) {
                continue;
            }
            this.entryList.add(entry);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tile_admin_nvram_list_layout, parent, false);
        // set the view's size, margins, paddings and layout parameters
        // ...
        @NotNull ViewHolder vh = new ViewHolder(this.context, this.fragmentManager, v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final Entry<Object, Object> entryAt = entryList.get(position);

        holder.key.setText(entryAt.getKey().toString());
        final Object value = entryAt.getValue();
        holder.value.setText(nullToEmpty(value != null ? value.toString() : ""));
        holder.position = position;
    }

    @Override
    public int getItemCount() {
        return entryList.size();
    }

    @Override
    public void onHide(@android.support.annotation.Nullable Parcelable parcelable) {
        //Update entry in remote router, and notify item changed
        if (parcelable instanceof Bundle) {
            //Background task
            new EditNVRAMVariableTask().execute((Bundle) parcelable);
        }

    }

    @Override
    public void onClear(@NonNull Parcelable[] parcelables) {
        //Nothing to do
    }

    @Override
    public void onUndo(@android.support.annotation.Nullable Parcelable parcelable) {
        //Nothing to do
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private static final String EDIT_NVRAM_DATA_FRAGMENT_TAG = "edit_nvram_data_fragment_tag";

        @NotNull
        final TextView key;

        @NotNull
        final TextView value;

        int position;

        private final Context context;
        private final View itemView;
        private final FragmentManager fragmentManager;

        public ViewHolder(Context context, FragmentManager fragmentManager, View itemView) {
            super(itemView);
            this.context = context;
            this.fragmentManager = fragmentManager;
            this.itemView = itemView;
            this.itemView.setOnClickListener(this);

            this.key = (TextView) this.itemView.findViewById(R.id.nvram_key);
            this.value = (TextView) this.itemView.findViewById(R.id.nvram_value);
        }

        @Override
        public void onClick(View v) {
            @NotNull final DialogFragment editFragment =
                    EditNVRAMKeyValueDialogFragment.newInstance(NVRAMDataRecyclerViewAdapter.this, position, key.getText(), value.getText());
            editFragment.show(fragmentManager, EDIT_NVRAM_DATA_FRAGMENT_TAG);
        }

    }

    private void displayMessage(String msg, Style style) {
        Utils.displayMessage(context, msg, style);
    }

    private class EditNVRAMVariableTask extends AsyncTask<Bundle, Void, EditNVRAMVariableTask.EditNVRAMVariableTaskResult<CharSequence>> {

        @Override
        protected EditNVRAMVariableTask.EditNVRAMVariableTaskResult<CharSequence> doInBackground(Bundle... params) {
            final Bundle token = params[0];
            final int position = token.getInt(POSITION);
            final CharSequence key = token.getCharSequence(KEY);
            final CharSequence value = token.getCharSequence(VALUE);

            Exception exception = null;

            try {
                final int exitStatus = SSHUtils
                        .runCommands(router, String.format("nvram set %s=\"%s\"", key, value), "nvram commit");
                if (exitStatus == 0) {
                    //Notify item changed
                    nvramInfo.put(key, value);
                    notifyItemChanged(position);
                } else {
                    throw new IllegalStateException();
                }
            } catch (Exception e) {
                e.printStackTrace();
                exception = e;
            }

            return new EditNVRAMVariableTask.EditNVRAMVariableTaskResult<>(key, exception);
        }

        @Override
        protected void onPostExecute(EditNVRAMVariableTaskResult<CharSequence> result) {
            final Exception exception = result.getException();
            try {
                if (exception == null) {
                    displayMessage("Variable '" + result.getResult() + "' updated", Style.CONFIRM);
                } else {
                    displayMessage("Variable '" + result.getResult() + "' NOT updated" +
                            (exception.getMessage() != null ? (": " + exception.getMessage()) : ""), Style.ALERT);
                }
            } catch (Exception e) {
                //No worries
            }
        }

        class EditNVRAMVariableTaskResult<T> {
            private final T result;
            private final Exception exception;

            private EditNVRAMVariableTaskResult(T result, Exception exception) {
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
