package org.rm3l.ddwrt.main;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.rm3l.ddwrt.R;

public class NavigationDrawerArrayAdapter extends ArrayAdapter<NavigationDrawerMenuItem> {

    private final Resources mResources;

    /**
     * Selected item position
     */
    private int mSelectedItem;

    public NavigationDrawerArrayAdapter(@NonNull final Context context) {
        super(context, 0);
        mResources = getContext().getResources();
    }

    public int getSelectedItem() {
        return mSelectedItem;
    }

    public void setSelectedItem(int selectedItem) {
        mSelectedItem = selectedItem;
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
    public int getViewTypeCount() {
        return 2;
    }


    @Override
    public int getItemViewType(int position) {
        return getItem(position).isHeader ? 0 : 1;
    }


    @Override
    public boolean isEnabled(int position) {
        return !getItem(position).isHeader;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        final NavigationDrawerMenuItem item = getItem(position);
        ViewHolder holder = null;
        View view = convertView;

        if (view == null) {
            int layout = R.layout.navigation_drawer_menu_row;
            if (item.isHeader)
                layout = R.layout.navigation_drawer_menu_row_header;

            view = LayoutInflater.from(getContext()).inflate(layout, null);

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
            if (holder.textHolder != null)
                holder.textHolder.setText(item.title);
        }

        if (holder != null && holder.textHolder != null) {
            if (position == mSelectedItem) {
                holder.textHolder.setTextColor(mResources.getColor(R.color.GreenYellow));
            } else {
                holder.textHolder.setTextColor(mResources.getColor(R.color.white));
            }
        }

        return view;
    }

    public static class ViewHolder {
        public final TextView textHolder;

        public ViewHolder(TextView text1) {
            this.textHolder = text1;
        }

    }
}
