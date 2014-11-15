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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lemra.dd_wrt.R;
import org.lemra.dd_wrt.api.conn.Router;

import java.util.List;

public class RouterListBaseAdapter extends BaseAdapter {

    private final List<Router> routersList;

    private LayoutInflater mInflater;

    public RouterListBaseAdapter(@NotNull Context context, List<Router> results) {
        routersList = results;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return routersList.size();
    }

    @Override
    public Object getItem(int position) {
        return routersList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Nullable
    @Override
    public View getView(int position, @Nullable View convertView, ViewGroup viewGroup) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.router_mgmt_layout_row_view, null);
            holder = new ViewHolder();
            holder.routerUuid = (TextView) convertView.findViewById(R.id.router_uuid);
            holder.routerName = (TextView) convertView.findViewById(R.id.router_name);
            holder.routerIp = (TextView) convertView
                    .findViewById(R.id.router_ip_address);
            holder.routerConnProto = (TextView) convertView.findViewById(R.id.router_connection_protocol);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final Router routerAt = routersList.get(position);
        holder.routerUuid.setText(routerAt.getUuid());
        holder.routerName.setText(routerAt.getName());
        holder.routerIp.setText(routerAt
                .getRemoteIpAddress());
        holder.routerConnProto.setText(routerAt.getRouterConnectionProtocol().toString());

        return convertView;
    }

    static class ViewHolder {
        TextView routerName;
        TextView routerIp;
        TextView routerConnProto;
        TextView routerUuid;
    }
}
