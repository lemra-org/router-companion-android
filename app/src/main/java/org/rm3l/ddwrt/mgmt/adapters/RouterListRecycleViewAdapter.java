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

package org.rm3l.ddwrt.mgmt.adapters;

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
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.resources.conn.Router;

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
        holder.routerUsername.setText(routerAt.getUsernamePlain());
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
     *
     *@return the number of elements in the DB
     */
    public int removeData(int position) {
        final Router router = this.routersList.get(position);
        if (router != null) {
            dao.deleteRouter(router.getUuid());
            //Drop SharedPreferences for this item too
            this.context.getSharedPreferences(router.getUuid(), Context.MODE_PRIVATE)
                    .edit().clear().commit();

            //Now refresh list
            final List<Router> allRouters = dao.getAllRouters();
            setRoutersList(allRouters);
            notifyItemRemoved(position);
            return allRouters.size();
        }
        return dao.getAllRouters().size();
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
