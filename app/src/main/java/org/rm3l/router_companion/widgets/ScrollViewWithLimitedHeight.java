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

package org.rm3l.router_companion.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.ScrollView;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static android.util.TypedValue.applyDimension;
import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.makeMeasureSpec;

/**
 * Custom ScrollView
 *
 * @see <a href="http://stackoverflow.com/questions/25791057/android-textview-inside-scrollview-how-to-limit-height">http://stackoverflow.com/questions/25791057/android-textview-inside-scrollview-how-to-limit-height</a>
 */
public class ScrollViewWithLimitedHeight extends ScrollView {

    public static final int MAX_HEIGHT = 500; // 500dp

    public ScrollViewWithLimitedHeight(Context context) {
        super(context);
    }

    public ScrollViewWithLimitedHeight(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrollViewWithLimitedHeight(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ScrollViewWithLimitedHeight(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        heightMeasureSpec = makeMeasureSpec(dpToPx(getResources(), MAX_HEIGHT), AT_MOST);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private int dpToPx(@NonNull final Resources res, final int dp) {
        return (int) applyDimension(COMPLEX_UNIT_DIP, dp, res.getDisplayMetrics());
    }

}
