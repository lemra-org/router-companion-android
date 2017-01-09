package org.rm3l.router_companion.widgets;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.rm3l.ddwrt.R;

/**
 * Created by rm3l on 10/10/15.
 */
public class TwoTextViewsArrayAdapter extends ArrayAdapter {

    private LayoutInflater inflater;

    public TwoTextViewsArrayAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        inflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if(convertView == null){
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.spinner_text_layout, null);
            holder.text1 = (TextView)convertView.findViewById(R.id.title);
            holder.text2 = (TextView)convertView.findViewById(R.id.subtitle);
            convertView.setTag(R.layout.spinner_text_layout, holder);
        } else{
            holder = (ViewHolder)convertView.getTag(R.layout.spinner_text_layout);
        }

        holder.text1.setText("Position: " );
        holder.text2.setText(position);

        return convertView;
    }

    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        ViewHolder2 holder;

        if(convertView == null){
            holder = new ViewHolder2();
            convertView = inflater.inflate(R.layout.spinner_text_layout, null);
            holder.text1 = (TextView)convertView.findViewById(R.id.title);
            holder.text2 = (TextView)convertView.findViewById(R.id.subtitle);
            convertView.setTag(R.layout.spinner_text_layout, holder);
        } else{
            holder = (ViewHolder2) convertView.getTag(R.layout.spinner_text_layout);
        }

        holder.text1.setText("Position: " );
        holder.text2.setText(position);

        return convertView;
    }

    static class ViewHolder{
        TextView text1;
        TextView text2;
    }

    static class ViewHolder2{
        TextView text1;
        TextView text2;
    }

}
