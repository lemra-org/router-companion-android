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
package org.rm3l.router_companion.main;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import org.rm3l.ddwrt.R;

public class NavigationDrawerArrayAdapter extends ArrayAdapter<NavigationDrawerMenuItem> {

  public static class ViewHolder {

    public final TextView textHolder;

    public ViewHolder(TextView text1) {
      this.textHolder = text1;
    }
  }

  private final Resources mResources;

  /** Selected item position */
  private int mSelectedItem;

  public NavigationDrawerArrayAdapter(@NonNull final Context context) {
    super(context, 0);
    mResources = getContext().getResources();
  }

  public void addHeader(String title) {
    addItem(title, true);
  }

  public void addItem(String title, boolean isHeader) {
    addItem(new NavigationDrawerMenuItem(title, isHeader));
  }

  public void addItem(@NonNull final NavigationDrawerMenuItem itemModel) {
    add(itemModel);
  }

  @Override
  public int getItemViewType(int position) {
    return getItem(position).isHeader ? 0 : 1;
  }

  public int getSelectedItem() {
    return mSelectedItem;
  }

  public void setSelectedItem(int selectedItem) {
    mSelectedItem = selectedItem;
  }

  public View getView(int position, View convertView, ViewGroup parent) {

    final NavigationDrawerMenuItem item = getItem(position);
    ViewHolder holder = null;
    View view = convertView;

    final Context context = getContext();
    if (view == null) {
      int layout = R.layout.navigation_drawer_menu_row;
      if (item.isHeader) {
        layout = R.layout.navigation_drawer_menu_row_header;
      }

      view = LayoutInflater.from(context).inflate(layout, null);

      TextView text1 = (TextView) view.findViewById(R.id.menurow_title);
      view.setTag(new ViewHolder(text1));
    }

    //        if (holder == null && view != null) {
    Object tag = view.getTag();
    if (tag instanceof ViewHolder) {
      holder = (ViewHolder) tag;
    }
    //        }

    if (item != null && holder != null) {
      if (holder.textHolder != null) {
        holder.textHolder.setText(item.title);
      }
    }

    if (holder != null && holder.textHolder != null) {
      if (position == mSelectedItem) {
        holder.textHolder.setTextColor(ContextCompat.getColor(context, R.color.GreenYellow));
      } else {
        holder.textHolder.setTextColor(ContextCompat.getColor(context, R.color.white));
      }
    }

    return view;
  }

  @Override
  public int getViewTypeCount() {
    return 2;
  }

  @Override
  public boolean isEnabled(int position) {
    return !getItem(position).isHeader;
  }
}
