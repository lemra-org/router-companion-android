package org.rm3l.ddwrt.tiles.admin.nvram;

import android.content.Context;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static java.util.Map.Entry;

public class NVRAMDataRecyclerViewAdapter extends RecyclerView.Adapter<NVRAMDataRecyclerViewAdapter.ViewHolder> {

    private final Context context;
    private final List<Entry<Object,Object>> entryList = new ArrayList<>();
    private Map<Object, Object> nvramInfo;
    private final FragmentManager fragmentManager;

    public NVRAMDataRecyclerViewAdapter(FragmentActivity context, NVRAMInfo nvramInfo) {
        this.context = context;
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
        holder.value.setText(nullToEmpty(value != null ? value.toString() : null));
        holder.position = position;
    }

    @Override
    public int getItemCount() {
        return entryList.size();
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
            //TODO
            Toast.makeText(this.context,
                    "[FIXME] Edit nvram with key '" + key.getText()+"' @" + position,
                    Toast.LENGTH_LONG).show();
            @NotNull final DialogFragment editFragment =
                    EditNVRAMKeyValueDialogFragment.newInstance(NVRAMDataRecyclerViewAdapter.this, position, key.getText(), value.getText());
            editFragment.show(fragmentManager, EDIT_NVRAM_DATA_FRAGMENT_TAG);
        }

    }
}
