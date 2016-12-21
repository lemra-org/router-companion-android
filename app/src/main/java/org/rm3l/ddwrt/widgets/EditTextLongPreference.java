package org.rm3l.ddwrt.widgets;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

/**
 * Created by rm3l on 21/12/2016.
 */

public class EditTextLongPreference extends EditTextPreference {

    public EditTextLongPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTextLongPreference(Context context) {
        super(context);
    }

    @Override
    protected boolean persistString(String value) {
        return value != null && persistLong(Long.valueOf(value));
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        if(getSharedPreferences().contains(getKey())) {
            long longValue = getPersistedLong(0);
            return String.valueOf(longValue);
        } else {
            return defaultReturnValue;
        }
    }
}
