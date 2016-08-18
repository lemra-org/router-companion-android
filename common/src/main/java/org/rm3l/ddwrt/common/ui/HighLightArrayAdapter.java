package org.rm3l.ddwrt.common.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

/**
 * Created by rm3l on 17/08/16.
 */
public class HighLightArrayAdapter<T> extends ArrayAdapter<T> {

    public static final int DEFAULT_HIGHLIGHT_BG_COLOR = Color.rgb(56, 184, 226);
    private int mSelectedIndex = -1;

    private final int mHighlightBgColor;

    public HighLightArrayAdapter(Context context, int resource,
                                 int highlightBgColor, T[] objects) {
        super(context, resource, objects);
        this.mHighlightBgColor = highlightBgColor;
    }

    public HighLightArrayAdapter(Context context, int resource, T[] objects) {
        this(context, resource, DEFAULT_HIGHLIGHT_BG_COLOR, objects);
    }

    public void setSelection(int position) {
        mSelectedIndex =  position;
        notifyDataSetChanged();
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View itemView =  super.getDropDownView(position, convertView, parent);

        if (position == mSelectedIndex) {
            itemView.setBackgroundColor(mHighlightBgColor);
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        return itemView;
    }
}