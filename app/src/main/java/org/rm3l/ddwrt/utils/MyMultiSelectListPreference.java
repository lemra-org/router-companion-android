package org.rm3l.ddwrt.utils;

import android.content.Context;
import android.preference.MultiSelectListPreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by rm3l on 28/06/15.
 */
public class MyMultiSelectListPreference extends MultiSelectListPreference {

    public MyMultiSelectListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyMultiSelectListPreference(Context context) {
        super(context);
    }

    @Override
    public void setValues(@NonNull final Set<String> values) {
        //Workaround for https://code.google.com/p/android/issues/detail?id=22807
        final Set<String> newValues = new HashSet<>();
        newValues.addAll( values );
        super.setValues( newValues );
    }
}
