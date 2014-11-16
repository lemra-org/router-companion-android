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

package com.actionbarsherlock.internal.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.TextView;

import java.util.Locale;

public class CapitalizingTextView extends TextView {
    private static final boolean SANS_ICE_CREAM = Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    private static final boolean IS_GINGERBREAD = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;

    private static final int[] R_styleable_TextView = new int[]{
            android.R.attr.textAllCaps
    };
    private static final int R_styleable_TextView_textAllCaps = 0;

    private boolean mAllCaps;

    public CapitalizingTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CapitalizingTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R_styleable_TextView, defStyle, 0);
        mAllCaps = a.getBoolean(R_styleable_TextView_textAllCaps, true);
        a.recycle();
    }

    public void setTextCompat(CharSequence text) {
        if (SANS_ICE_CREAM && mAllCaps && text != null) {
            if (IS_GINGERBREAD) {
                try {
                    setText(text.toString().toUpperCase(Locale.ROOT));
                } catch (NoSuchFieldError e) {
                    //Some manufacturer broke Locale.ROOT. See #572.
                    setText(text.toString().toUpperCase());
                }
            } else {
                setText(text.toString().toUpperCase());
            }
        } else {
            setText(text);
        }
    }
}
