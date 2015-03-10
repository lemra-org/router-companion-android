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
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.status.wireless.WirelessClientsTile;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.SSHUtils;

import java.util.ArrayList;
import java.util.List;

import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DDWRTCOMPANION_WANACCESS_IPTABLES_CHAIN;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.OPENED_AT_LEAST_ONCE_PREF_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.THEMING_PREF;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.getClientsUsageDataFile;

public class RouterListRecycleViewAdapter extends RecyclerView.Adapter<RouterListRecycleViewAdapter.ViewHolder> {

    public static final String EMPTY = "(empty)";
    final DDWRTCompanionDAO dao;
    private final Context context;
    private final Resources resources;
    private List<Router> routersList;
    private SparseBooleanArray selectedItems;

    public RouterListRecycleViewAdapter(Context context, List<Router> results) {
        routersList = results;
        this.context = context;
        this.dao = RouterManagementActivity.getDao(context);
        resources = context.getResources();
        selectedItems = new SparseBooleanArray();
    }

    public List<Router> getRoutersList() {
        return routersList;
    }

    public void setRoutersList(final List<Router> results) {
        routersList = results;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.router_list_layout, parent, false);
        // set the view's size, margins, paddings and layout parameters
        // ...
        final long currentTheme = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
                .getLong(THEMING_PREF, DDWRTCompanionConstants.DEFAULT_THEME);
        final CardView cardView = (CardView) v.findViewById(R.id.router_item_cardview);
        if (currentTheme == ColorUtils.LIGHT_THEME) {
            //Light
            cardView.setCardBackgroundColor(resources.getColor(R.color.cardview_light_background));
        } else {
            //Default is Dark
            cardView.setCardBackgroundColor(resources.getColor(R.color.cardview_dark_background));
        }

        return new ViewHolder(this.context, v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final Router routerAt = routersList.get(position);

        holder.routerUuid.setText(routerAt.getUuid());
        final String routerAtName = routerAt.getName();
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
        final Router.RouterFirmware routerFirmware = routerAt.getRouterFirmware();
        holder.routerFirmware.setText("Firmware: " + (routerFirmware != null ? routerFirmware.getDisplayName() : "-"));
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
     * @return the number of elements in the DB
     */
    public int removeData(int position) {
        final Router router = this.routersList.get(position);
        if (router != null) {
            dao.deleteRouter(router.getUuid());

            //Also Remove Usage Data Created
            //noinspection ResultOfMethodCallIgnored
            getClientsUsageDataFile(context, router.getUuid()).delete();

            final SharedPreferences sharedPreferences = this.context.getSharedPreferences(router.getUuid(), Context.MODE_PRIVATE);

            if (sharedPreferences.getBoolean(OPENED_AT_LEAST_ONCE_PREF_KEY, false)) {
                //Opened at least once, meaning that usage data might have been created.
                //If never opened, do nothing, as usage data might be used by another router record
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //Delete iptables chains created for monitoring and wan access (in a thread)
                            SSHUtils.runCommands(context, context
                                            .getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE),
                                    router,
                                    Joiner.on(" ; ").skipNulls(),

                                    "iptables -D FORWARD -j " + DDWRTCOMPANION_WANACCESS_IPTABLES_CHAIN,
                                    "iptables -F " + DDWRTCOMPANION_WANACCESS_IPTABLES_CHAIN,
                                    "iptables -X " + DDWRTCOMPANION_WANACCESS_IPTABLES_CHAIN,

                                    "iptables -D FORWARD -j DDWRTCompanion",
                                    "iptables -F DDWRTCompanion",
                                    "iptables -X DDWRTCompanion",

                                    "rm -f " + DDWRTCompanionConstants.WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_PATH_REMOTE,
                                    "rm -f /tmp/.DDWRTCompanion_traffic_55.tmp",
                                    "rm -f /tmp/.DDWRTCompanion_traffic_66.tmp",
                                    "rm -f " + WirelessClientsTile.USAGE_DB,
                                    "rm -f " + WirelessClientsTile.USAGE_DB_OUT);

                        } catch (final Exception e) {
                            e.printStackTrace();
                            //No Worries
                        } finally {
                            //Disconnect session
                            destroySSHSession(router);
                        }
                    }
                }).start();
            }

            //Drop SharedPreferences for this item too
            sharedPreferences.edit().clear().apply();

            //Now refresh list
            final List<Router> allRouters = dao.getAllRouters();
            setRoutersList(allRouters);
            notifyItemRemoved(position);
            return allRouters.size();
        }
        return dao.getAllRouters().size();
    }

    private void destroySSHSession(@NonNull final Router router) {
        //Async to avoid ANR because SSHUtils#destroySession makes use of locking mechanisms
        new Thread(new Runnable() {
            @Override
            public void run() {
                SSHUtils.destroySession(router);
            }
        }).start();
    }

    @NonNull
    public List<Integer> getSelectedItems() {
        final List<Integer> items = new ArrayList<Integer>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {

        @NonNull
        final TextView routerName;
        @NonNull
        final TextView routerIp;
        @NonNull
        final TextView routerConnProto;
        @NonNull
        final TextView routerUuid;
        @NonNull
        final TextView routerUsername;
        @NonNull
        final TextView routerFirmware;

        private final Context context;
        private final View itemView;

        public ViewHolder(Context context, View itemView) {
            super(itemView);
            this.context = context;
            this.itemView = itemView;

            this.routerName = (TextView) this.itemView.findViewById(R.id.router_name);
            this.routerIp = (TextView) this.itemView.findViewById(R.id.router_ip_address);
            this.routerConnProto = (TextView) this.itemView.findViewById(R.id.router_connection_protocol);
            this.routerUuid = (TextView) this.itemView.findViewById(R.id.router_uuid);
            this.routerUsername = (TextView) this.itemView.findViewById(R.id.router_username);
            this.routerFirmware = (TextView) this.itemView.findViewById(R.id.router_firmware);
        }

    }

}
