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
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.lemra.dd_wrt.DDWRTMainActivity;
import org.lemra.dd_wrt.R;
import org.lemra.dd_wrt.api.conn.Router;

import java.util.List;

import static org.lemra.dd_wrt.mgmt.RouterManagementActivity.ROUTER_SELECTED;

public class RouterListRecycleViewAdapter extends RecyclerView.Adapter<RouterListRecycleViewAdapter.ViewHolder> {

    private final List<Router> routersList;
    private Context context;

//    private LayoutInflater mInflater;

    public RouterListRecycleViewAdapter(Context context, List<Router> results) {
        routersList = results;
        this.context = context;
//        mInflater = LayoutInflater.from(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.router_mgmt_layout_row_view, parent, false);
        // set the view's size, margins, paddings and layout parameters
        // ...
        ViewHolder vh = new ViewHolder(this.context, v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final Router routerAt = routersList.get(position);

        holder.routerUuid.setText(routerAt.getUuid());
        holder.routerName.setText(routerAt.getName());
        holder.routerIp.setText(routerAt
                .getRemoteIpAddress());
        holder.routerConnProto.setText(routerAt.getRouterConnectionProtocol().toString());
    }

    @Override
    public int getItemCount() {
        return routersList.size();
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        final TextView routerName;
        final TextView routerIp;
        final TextView routerConnProto;
        final TextView routerUuid;

        private final Context context;
        private final View itemView;

        public ViewHolder(Context context, View itemView) {
            super(itemView);
            this.context = context;
            this.itemView = itemView;
            this.itemView.setOnClickListener(this);
            this.itemView.setOnLongClickListener(this);

            this.routerName = (TextView) this.itemView.findViewById(R.id.router_name);
            this.routerIp = (TextView) this.itemView.findViewById(R.id.router_ip_address);
            this.routerConnProto = (TextView) this.itemView.findViewById(R.id.router_connection_protocol);
            this.routerUuid = (TextView) this.itemView.findViewById(R.id.router_uuid);
        }

        @Override
        public void onClick(View view) {
            if (routerUuid == null) {
                Toast.makeText(this.context,
                        "Click on Unknown router - please refresh list or add a new one.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            //Opens up main activity with the router selected
            final Intent ddWrtMainIntent = new Intent(this.context, DDWRTMainActivity.class);
            ddWrtMainIntent.putExtra(ROUTER_SELECTED, routerUuid.getText());
            this.context.startActivity(ddWrtMainIntent);
        }

        @Override
        public boolean onLongClick(View view) {
            if (routerUuid == null) {
                Toast.makeText(this.context,
                        "Long click on Unknown router - please refresh list or add a new one.",
                        Toast.LENGTH_LONG).show();
                return false;
            }
            //TODO
            Toast.makeText(this.context,
                    "[onLongClick] router: " + routerUuid.getText(),
                    Toast.LENGTH_LONG).show();
            return false;
        }
    }

}
