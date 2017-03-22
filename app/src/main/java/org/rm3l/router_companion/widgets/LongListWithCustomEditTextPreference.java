package org.rm3l.router_companion.widgets;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Created by rm3l on 26/08/15.
 */
public class LongListWithCustomEditTextPreference extends LongListPreference {

  //    private CharSequence[] entries;
  //    private CharSequence[] entryValues;
  private final Context mContext;

  //    private CustomListPreferenceAdapter customListPreferenceAdapter;
  //    private ArrayList<RadioButton> rButtonList;
  //    private SharedPreferences prefs;
  //    private SharedPreferences.Editor editor;
  //    private LayoutInflater mInflater;
  //
  public LongListWithCustomEditTextPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.mContext = context;
  }

  public LongListWithCustomEditTextPreference(Context context) {
    super(context);
    this.mContext = context;
  }
  //
  //    @Override
  //    protected void onPrepareDialogBuilder(AlertDialog.Builder builder)
  //    {
  //        entries = getEntries();
  //        entryValues = getEntryValues();
  //
  //        if (entries == null || entryValues == null || entries.length != entryValues.length )
  //        {
  //            throw new IllegalStateException(
  //                    "ListPreference requires an entries array and an entryValues array which are both the same length");
  //        }
  //
  //        customListPreferenceAdapter = new CustomListPreferenceAdapter(mContext);
  //
  //        builder.setAdapter(customListPreferenceAdapter, new DialogInterface.OnClickListener() {
  //            public void onClick(DialogInterface dialog, int which) {
  //                //TODO
  //            }
  //        });
  //
  //        /*
  //         * The typical interaction for list-based dialogs is to have
  //         * click-on-an-item dismiss the dialog instead of the user having to
  //         * press 'Ok'.
  //         */
  //        builder.setPositiveButton(null, null);
  //    }
  //
  //    private class CustomListPreferenceAdapter extends BaseAdapter
  //    {
  //        private SharedPreferences prefs;
  //        public CustomListPreferenceAdapter(Context context)
  //        {
  //            this.prefs = context
  //                    .getSharedPreferences(DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
  //        }
  //
  //        /*
  //         * removed usual adapter stuff (getCount, getItem...)
  //         */
  //        @Override
  //        public int getCount() {
  //            return entries.length;
  //        }
  //
  //        @Override
  //        public Object getItem(int position) {
  //            return (position >= 0 && position < entries.length) ? entries[position] : position;
  //        }
  //
  //        @Override
  //        public long getItemId(int position) {
  //            return position;
  //        }
  //
  //        public View getView(final int position, View convertView, ViewGroup parent)
  //        {
  //            View row = convertView;
  //
  //            if(row == null)
  //            {
  //                //If it's not the last one: use a normal holder...
  //                if(position < 2)
  //                {
  //                    NormalHolder holder = null;
  //
  //                    row = mInflater.inflate(R.layout.normal_list_preference_row, parent, false);
  //                    if(prefs.getString(mContext.getString(R.string.SP_address), "0").equals(entryValues[position])) {
  //                        holder = new NormalHolder(row, position,true);
  //                    } else {
  //                        holder = new NormalHolder(row, position,false);
  //                    }
  //
  //
  //                    row.setTag(holder);
  //                    row.setClickable(true);
  //                    row.setOnClickListener(new View.OnClickListener()
  //                    {
  //                        public void onClick(View v)
  //                        {
  //                            for(RadioButton rb : rButtonList)
  //                            {
  //                                if(rb.getId() != position)
  //                                    rb.setChecked(false);
  //                            }
  //
  //                            int index = position;
  //                            String value = entryValues[index].toString();
  //                            Log.v("Editor", "putting string" + value);
  //                            editor.putString(mContext.getString(R.string.SP_address), value);
  //                            editor.apply();
  //                            final Dialog mDialog = getDialog();
  //                            mDialog.dismiss();
  //                        }
  //                    });
  //                    //Otherwise, if it is the last one...
  //                } else {
  //                    //Use the custom row
  //                    row = mInflater.inflate(R.layout.custom_list_preference_row, parent, false);
  //                    String fromPref = prefs.getString(mContext.getString(R.string.SP_address), "0");
  //                    boolean flag=false;
  //                    for(CharSequence entry  : entryValues) {
  //                        if(entry.toString().equals(fromPref)) {
  //                            flag=true;
  //                        }
  //                    }
  //                    //And use a "custom holder"
  //                    CustomHolder holder;
  //                    if(!flag) {
  //                        holder = new CustomHolder(row, position, fromPref, true);
  //                    } else {
  //                        holder = new CustomHolder(row, position, "", false);
  //
  //                    }
  //                    row.setTag(holder);
  //                }
  //            }
  //
  //            return row;
  //        }
  //        /*
  //         * This class just shows the information in row from the position and the PreferenceList entries
  //         */
  //        class NormalHolder
  //        {
  //            private TextView text = null;
  //            private RadioButton rButton = null;
  //
  //            NormalHolder(View row, int position, boolean isChecked)
  //            {
  //                text = (TextView)row.findViewById(R.id.custom_list_view_row_text_view);
  //                text.setText(entries[position]);
  //                rButton = (RadioButton)row.findViewById(R.id.custom_list_view_row_radio_button);
  //                rButton.setId(position);
  //                rButton.setChecked(isChecked);
  //
  //                // also need to do something to check your preference and set the right button as checked
  //
  //                rButtonList.add(rButton);
  //                rButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
  //                {
  //                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
  //                    {
  //                        if(isChecked)
  //                        {
  //                        /*
  //                         * Put stuff into SharedPreference
  //                         */
  //                        }
  //                    }
  //                });
  //            }
  //
  //        }
  //        /*
  //         * This class display the text within the EditText
  //         */
  //        class CustomHolder
  //        {
  //            private EditText text = null;
  //            private RadioButton rButton = null;
  //
  //            CustomHolder(View row, int position, String pref, boolean checked)
  //            {
  //                text = (EditText)row.findViewById(R.id.ET_prefs_customText);
  //                text.setText(pref);
  //
  //                rButton = (RadioButton)row.findViewById(R.id.custom_list_view_row_radio_button);
  //                rButton.setId(position);
  //                rButton.setChecked(checked);
  //
  //                rButtonList.add(rButton);
  //                rButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
  //                {
  //                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
  //                    {
  //                        if(isChecked)
  //                        {
  //                        /*
  //                         * Put stuff into SharedPreference
  //                         */
  //                        }
  //                    }
  //                });
  //            }
  //        }
  //    }
}
