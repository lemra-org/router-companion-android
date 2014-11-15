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

package org.lemra.dd_wrt.mgmt.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.base.Strings;

import org.jetbrains.annotations.NotNull;
import org.lemra.dd_wrt.R;
import org.lemra.dd_wrt.api.conn.Router;
import org.lemra.dd_wrt.mgmt.RouterManagementActivity;
import org.lemra.dd_wrt.mgmt.dao.DDWRTCompanionDAO;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RouterListRecycleViewAdapter extends RecyclerView.Adapter<RouterListRecycleViewAdapter.ViewHolder> {

    public static final String EMPTY = "(empty)";
    final DDWRTCompanionDAO dao;
    private final Context context;
    private List<Router> routersList;
    private SparseBooleanArray selectedItems;

    public RouterListRecycleViewAdapter(Context context, List<Router> results) {
        routersList = results;
        this.context = context;
        this.dao = RouterManagementActivity.getDao(context);
        try {
            this.dao.open();
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

        selectedItems = new SparseBooleanArray();
    }

    public List<Router> getRoutersList() {
        return routersList;
    }

    public void setRoutersList(final List<Router> results) {
        routersList = results;
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.router_list_layout, parent, false);
        // set the view's size, margins, paddings and layout parameters
        // ...
        @NotNull ViewHolder vh = new ViewHolder(this.context, v);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final Router routerAt = routersList.get(position);

        holder.routerUuid.setText(routerAt.getUuid());
        @NotNull final String routerAtName = routerAt.getName();
        if (Strings.isNullOrEmpty(routerAtName)) {
            //Italic
            holder.routerName.setText(EMPTY);
            holder.routerName.setTypeface(null, Typeface.ITALIC);
        } else {
            holder.routerName.setText(routerAtName);
        }
        holder.routerIp.setText(routerAt.getRemoteIpAddress());
        holder.routerConnProto.setText(routerAt.getRouterConnectionProtocol().toString());
        holder.routerUsername.setText(routerAt.getUsername());
    }

    @Override
    public int getItemCount() {
        return routersList.size();
    }

    public void toggleSelection(int pos) {
        if (selectedItems.get(pos, false)) {
            selectedItems.delete(pos);
        } else {
            selectedItems.put(pos, true);
        }
        setRoutersList(dao.getAllRouters());
        notifyItemChanged(pos);
    }

    public void clearSelections() {
        selectedItems.clear();
        setRoutersList(dao.getAllRouters());
        notifyDataSetChanged();
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    /**
     * Removes the item that currently is at the passed in position from the
     * underlying data set.
     *
     * @param position The index of the item to remove.
     */
    public void removeData(int position) {
        final Router router = this.routersList.get(position);
        if (router != null) {
            dao.deleteRouter(router.getUuid());
            setRoutersList(dao.getAllRouters());
            notifyItemRemoved(position);
        }
    }

    @NotNull
    public List<Integer> getSelectedItems() {
        @NotNull final List<Integer> items = new ArrayList<Integer>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
//                implements View.OnClickListener, View.OnLongClickListener {

        @NotNull
        final TextView routerName;
        @NotNull
        final TextView routerIp;
        @NotNull
        final TextView routerConnProto;
        @NotNull
        final TextView routerUuid;
        @NotNull
        final TextView routerUsername;

        private final Context context;
        private final View itemView;

        public ViewHolder(Context context, View itemView) {
            super(itemView);
            this.context = context;
            this.itemView = itemView;
//            this.itemView.setOnClickListener(this);
//            this.itemView.setOnLongClickListener(this);

            this.routerName = (TextView) this.itemView.findViewById(R.id.router_name);
            this.routerIp = (TextView) this.itemView.findViewById(R.id.router_ip_address);
            this.routerConnProto = (TextView) this.itemView.findViewById(R.id.router_connection_protocol);
            this.routerUuid = (TextView) this.itemView.findViewById(R.id.router_uuid);
            this.routerUsername = (TextView) this.itemView.findViewById(R.id.router_username);
        }

//        @Override
//        public void onClick(View view) {
//            if (routerUuid == null) {
//                Toast.makeText(this.context,
//                        "Click on Unknown router - please refresh list or add a new one.",
//                        Toast.LENGTH_LONG).show();
//                return;
//            }
//
//            //Opens up main activity with the router selected
//            final Intent ddWrtMainIntent = new Intent(this.context, DDWRTMainActivity.class);
//            ddWrtMainIntent.putExtra(ROUTER_SELECTED, routerUuid.getText());
//            this.context.startActivity(ddWrtMainIntent);
//        }
//
//        @Override
//        public boolean onLongClick(View view) {
//            if (routerUuid == null) {
//                Toast.makeText(this.context,
//                        "Long click on Unknown router - please refresh list or add a new one.",
//                        Toast.LENGTH_LONG).show();
//                return false;
//            }
//            //TODO
//            Toast.makeText(this.context,
//                    "[onLongClick] router: " + routerUuid.getText(),
//                    Toast.LENGTH_LONG).show();
//            return false;
//        }
    }

}
