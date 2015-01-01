package org.rm3l.ddwrt.tiles.admin.nvram;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.Lists;

import org.jetbrains.annotations.NotNull;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;

import java.util.List;

import static java.util.Map.Entry;

public class NVRAMDataRecyclerViewAdapter extends RecyclerView.Adapter<NVRAMDataRecyclerViewAdapter.ViewHolder> {

    private final Context context;
    private final NVRAMInfo nvramInfo;
    private final List<Entry<Object,Object>> entryList;

    public NVRAMDataRecyclerViewAdapter(Context context, NVRAMInfo nvramInfo) {
        this.context = context;
        this.nvramInfo = nvramInfo;
        //noinspection ConstantConditions
        this.entryList = Lists.newArrayList(this.nvramInfo.getData().entrySet());
    }

    public List<Entry<Object, Object>> getEntryList() {
        return entryList;
    }

    public void setEntryList(@NotNull final NVRAMInfo nvramInfo) {
        this.entryList.clear();
        //noinspection ConstantConditions
        this.entryList.addAll(nvramInfo.getData().entrySet());
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tile_admin_nvram_list_layout, parent, false);
        // set the view's size, margins, paddings and layout parameters
        // ...
        @NotNull ViewHolder vh = new ViewHolder(this.context, v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final Entry<Object, Object> entryAt = entryList.get(position);

        holder.key.setText((String) entryAt.getKey());
        holder.value.setText((String) entryAt.getValue());
    }

    @Override
    public int getItemCount() {
        return entryList.size();
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        @NotNull
        final TextView key;

        @NotNull
        final TextView value;

        private final Context context;
        private final View itemView;

        public ViewHolder(Context context, View itemView) {
            super(itemView);
            this.context = context;
            this.itemView = itemView;
            this.itemView.setOnClickListener(this);

            this.key = (TextView) this.itemView.findViewById(R.id.nvram_key);
            this.value = (TextView) this.itemView.findViewById(R.id.nvram_value);
        }

        @Override
        public void onClick(View v) {
            //TODO
            Toast.makeText(this.context,
                    "[FIXME] Edit nvram with key '" + key.getText()+"'",
                    Toast.LENGTH_LONG).show();
        }

    }
}
