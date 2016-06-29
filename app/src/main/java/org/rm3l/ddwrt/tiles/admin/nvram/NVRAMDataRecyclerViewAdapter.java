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
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.cocosw.undobar.UndoBarController;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.multithreading.MultiThreadingManager;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.keyboardsurfer.android.widget.crouton.Style;
import needle.UiRelatedTask;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static java.util.Map.Entry;
import static org.rm3l.ddwrt.tiles.admin.nvram.EditNVRAMKeyValueDialogFragment.KEY;
import static org.rm3l.ddwrt.tiles.admin.nvram.EditNVRAMKeyValueDialogFragment.POSITION;
import static org.rm3l.ddwrt.tiles.admin.nvram.EditNVRAMKeyValueDialogFragment.VALUE;

public class NVRAMDataRecyclerViewAdapter extends RecyclerView.Adapter<NVRAMDataRecyclerViewAdapter.ViewHolder>
        implements UndoBarController.AdvancedUndoListener {

    private final FragmentActivity context;
    private final List<Entry<Object, Object>> entryList = new ArrayList<>();
    private final FragmentManager fragmentManager;
    private final Router router;
    private final SharedPreferences mGlobalPreferences;
    private Map<Object, Object> nvramInfo;

    public NVRAMDataRecyclerViewAdapter(FragmentActivity context, Router router, NVRAMInfo nvramInfo) {
        this.context = context;
        this.mGlobalPreferences = context.getSharedPreferences(DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        this.router = router;
        this.fragmentManager = context.getSupportFragmentManager();
        //noinspection ConstantConditions
        this.setEntryList(nvramInfo.getData());
    }

    @Nullable
    public Map<Object, Object> getNvramInfo() {
        return nvramInfo;
    }

    public void setEntryList(@NonNull final Map<Object, Object> nvramInfo) {
        this.entryList.clear();
        this.nvramInfo = nvramInfo;
        //Not needed at this point - so make sure value has been read prior to calling this method
        nvramInfo.remove(AdminNVRAMTile.NVRAM_SIZE);
        this.nvramInfo = nvramInfo;
        //noinspection ConstantConditions
        for (final Entry<Object, Object> entry : nvramInfo.entrySet()) {
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
                .inflate(R.layout.tile_admin_nvram_row_view, parent, false);
        // set the view's size, margins, paddings and layout parameters
        // ...
        final ViewHolder vh = new ViewHolder(this.context, this.fragmentManager, v);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final Entry<Object, Object> entryAt = entryList.get(position);

        final boolean themeLight = ColorUtils.isThemeLight(this.context);
        if (themeLight) {
            holder.cardView.setCardBackgroundColor(ContextCompat
                    .getColor(context, R.color.cardview_light_background));
        } else {
            holder.cardView.setCardBackgroundColor(ContextCompat
                    .getColor(context, R.color.cardview_dark_background));
        }

        final String nvramKey = \"fake-key\";
        holder.key.setText(nvramKey);
        final Object value = entryAt.getValue();
        holder.value.setText(nullToEmpty(value != null ? value.toString() : ""));
        holder.position = holder.getAdapterPosition();

        final AlertDialog deleteDialog = new AlertDialog.Builder(context)
                .setIcon(R.drawable.ic_action_alert_warning)
                .setTitle("Drop NVRAM variable: '" + nvramKey + "'?")
                .setMessage("You'll lose this record!")
                .setCancelable(true)
                .setPositiveButton("Proceed!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MultiThreadingManager.getActionExecutor().execute(new UiRelatedTask<Exception>() {
                            @Override
                            protected Exception doWork() {
                                Exception exception = null;
                                try {
                                    final int exitStatus = SSHUtils
                                            .runCommands(context, mGlobalPreferences, router,
                                                    String.format("/usr/sbin/nvram unset \"%s\"", nvramKey),
                                                    "/usr/sbin/nvram commit");
                                    if (exitStatus != 0) {
                                        throw new IllegalStateException("Failed to unset NVRAM data: " + nvramKey);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    exception = e;
                                }
                                return exception;
                            }

                            @Override
                            protected void thenDoUiRelatedWork(Exception exception) {
                                if (exception != null) {
                                    Utils.displayMessage(context,
                                            "Error while trying to unset NVRAM variable: " + nvramKey + ": " +
                                                    ExceptionUtils.getRootCauseMessage(exception),
                                            Style.ALERT);
                                } else {
                                    Utils.displayMessage(context, "Done unsetting NVRAM variable: " + nvramKey, Style.CONFIRM);
                                    notifyItemRemoved(holder.position);
                                }
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Cancelled - nothing more to do!
                    }
                }).create();

        holder.removeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Display confirmation dialog
                deleteDialog.show();
            }
        });

        if (!themeLight) {
            //Set menu background to white
            holder.menuBtn
                    .setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
        }

        holder.menuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final PopupMenu popup = new PopupMenu(context, v);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.menu_nvram_var_edit:
                                holder.cardView.performClick();
                                return true;
                            case R.id.menu_nvram_var_remove:
                                deleteDialog.show();
                                return true;
                            default:
                                break;
                        }
                        return false;
                    }
                });
                final MenuInflater inflater = popup.getMenuInflater();
                final Menu menu = popup.getMenu();
                inflater.inflate(R.menu.menu_manage_nvram_var, menu);
                popup.show();
            }
        });

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
            MultiThreadingManager.getActionExecutor()
                    .execute(new EditNVRAMVariableTask((Bundle) parcelable));
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

    private void displayMessage(String msg, Style style) {
        Utils.displayMessage(context, msg, style);
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private static final String EDIT_NVRAM_DATA_FRAGMENT_TAG = "edit_nvram_data_fragment_tag";

        final CardView cardView;

        @NonNull
        final TextView key;

        @NonNull
        final TextView value;
        private final Context context;
        private final View itemView;
        private final FragmentManager fragmentManager;

        final ImageButton menuBtn;
        final ImageButton removeBtn;

        int position;

        public ViewHolder(Context context, FragmentManager fragmentManager, View itemView) {
            super(itemView);
            this.context = context;
            this.fragmentManager = fragmentManager;
            this.itemView = itemView;
            this.itemView.setOnClickListener(this);
            this.cardView = (CardView)
                    this.itemView.findViewById(R.id.nvram_entry_cardview);
            this.cardView.setOnClickListener(this);

            this.menuBtn = (ImageButton) this.itemView.findViewById(R.id.nvram_var_menu);
            this.removeBtn = (ImageButton) this.itemView.findViewById(R.id.nvram_var_remove_btn);

            this.key = (TextView) this.itemView.findViewById(R.id.nvram_key);
            this.value = (TextView) this.itemView.findViewById(R.id.nvram_value);
        }

        @Override
        public void onClick(View v) {
            if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
                Utils.displayUpgradeMessage(context, "Update NVRAM Variable");
                return;
            }
            final DialogFragment editFragment =
                    EditNVRAMKeyValueDialogFragment.newInstance(NVRAMDataRecyclerViewAdapter.this, position, key.getText(), value.getText());
            editFragment.show(fragmentManager, EDIT_NVRAM_DATA_FRAGMENT_TAG);
        }

    }
    
    private class EditNVRAMVariableTask extends
            UiRelatedTask<EditNVRAMVariableTask.EditNVRAMVariableTaskResult<CharSequence>> {

        final Bundle token;
        final int position;
        final CharSequence key;
        final CharSequence value;

        private EditNVRAMVariableTask(Bundle token) {
            this.token = token;
            this.position = token.getInt(POSITION);
            this.key = token.getCharSequence(KEY);
            this.value = token.getCharSequence(VALUE);
        }

        @Override
        protected EditNVRAMVariableTaskResult<CharSequence> doWork() {
            Exception exception = null;
            try {
                final int exitStatus = SSHUtils
                        .runCommands(context, mGlobalPreferences, router,
                                String.format("/usr/sbin/nvram set %s=\"%s\"", key, value), "/usr/sbin/nvram commit");
                if (exitStatus != 0) {
                    throw new IllegalStateException("Failed to update NVRAM data");
                }
            } catch (Exception e) {
                e.printStackTrace();
                exception = e;
            }
            return new EditNVRAMVariableTask.EditNVRAMVariableTaskResult<>(key, exception);
        }

        @Override
        protected void thenDoUiRelatedWork(EditNVRAMVariableTaskResult<CharSequence> result) {
            final Exception exception = result.getException();
            try {
                if (exception == null) {
                    nvramInfo.put(key, value);
                    notifyItemChanged(position);
                    displayMessage("Variable '" + result.getResult() + "' updated", Style.CONFIRM);
                } else {
                    displayMessage("Variable '" + result.getResult() + "' NOT updated" +
                            (exception.getMessage() != null ? (": " + exception.getMessage()) : ""),
                            Style.ALERT);
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
