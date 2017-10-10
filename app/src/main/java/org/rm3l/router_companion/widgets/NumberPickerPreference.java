package org.rm3l.router_companion.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;
import org.rm3l.ddwrt.R;

/**
 * Created by rm3l on 15/11/15.
 */
public class NumberPickerPreference extends DialogPreference {

    private static int DEFAULT_VAL = 0;

    private int mCurrentValue;

    private String mDescription;

    private NumberPicker mNumberPicker;

    private int mPickerMaxVal;

    private int mPickerMinVal;

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.number_picker_dialog);
        parseCustomAttrs(attrs);
    }

    public String getMDescription() {
        return this.mDescription;
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        if (getSharedPreferences().contains(getKey())) {
            final int val = getPersistedInt(DEFAULT_VAL);
            return String.valueOf(val);
        } else {
            return defaultReturnValue;
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        this.mNumberPicker = (NumberPicker) view.findViewById(R.id.number_picker);
        mNumberPicker.setMaxValue(mPickerMaxVal);
        mNumberPicker.setMinValue(mPickerMinVal);
        mNumberPicker.setValue(mCurrentValue);
        // do not allow wrap, otherwise input a value smaller than minVal will result in bug
        mNumberPicker.setWrapSelectorWheel(true);

        ((TextView) view.findViewById(R.id.number_picker_desc)).setText(mDescription);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        // When the user selects "OK", persist the new value
        if (positiveResult) {
            // clear focus on mNumberPicker so that getValue returns the updated input value
            mNumberPicker.clearFocus();
            mCurrentValue = mNumberPicker.getValue();
            persistInt(mCurrentValue);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, DEFAULT_VAL);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            this.mCurrentValue = getPersistedInt(DEFAULT_VAL);
        } else {
            this.mCurrentValue = (Integer) defaultValue;
            persistInt(mCurrentValue);
        }
    }

    private void parseCustomAttrs(AttributeSet attrs) {
        final TypedArray array = getContext().getTheme()
                .obtainStyledAttributes(attrs, R.styleable.NumberPickerPreference, 0, 0);
        try {
            mDescription = array.getString(R.styleable.NumberPickerPreference_description);
            mPickerMinVal = array.getInteger(R.styleable.NumberPickerPreference_min, 0);
            mPickerMaxVal = array.getInteger(R.styleable.NumberPickerPreference_max, 10);
        } finally {
            array.recycle();
        }
    }
}
