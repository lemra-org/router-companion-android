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
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.ProgressBar;
import org.rm3l.ddwrt.R;

public class TextProgressBar extends ProgressBar {

    private final Rect bounds = new Rect();

    private String text = "";

    private int textColor = Color.WHITE;

    private final Paint textPaint = new Paint();

    private float textSize = 15;

    public TextProgressBar(Context context) {
        super(context);
    }

    public TextProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAttrs(attrs);
    }

    public TextProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setAttrs(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TextProgressBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setAttrs(attrs);
    }

    public String getText() {
        return text;
    }

    public synchronized void setText(String text) {
        if (text != null) {
            this.text = text;
        } else {
            this.text = "";
        }
        postInvalidate();
    }

    public int getTextColor() {
        return textColor;
    }

    public synchronized void setTextColor(int textColor) {
        this.textColor = textColor;
        postInvalidate();
    }

    public float getTextSize() {
        return textSize;
    }

    public synchronized void setTextSize(float textSize) {
        this.textSize = textSize;
        postInvalidate();
    }

    @Override
    protected synchronized void onDraw(@NonNull final Canvas canvas) {
        super.onDraw(canvas);
        //create an instance of class Paint, set color and font size
        textPaint.setAntiAlias(true);
        textPaint.setColor(textColor);
        textPaint.setTextSize(textSize);
        //In order to show text in a middle, we need to know its size
        textPaint.getTextBounds(text, 0, text.length(), bounds);
        //Now we store font size in bounds variable and can calculate its position
        int x = getWidth() / 2 - bounds.centerX();
        int y = getHeight() / 2 - bounds.centerY();
        //drawing text with appropriate color and size in the center
        canvas.drawText(text, x, y, textPaint);
    }

    private void setAttrs(AttributeSet attrs) {
        if (attrs != null) {
            final TypedArray a =
                    getContext().obtainStyledAttributes(attrs, R.styleable.TextProgressBar, 0, 0);
            setText(a.getString(R.styleable.TextProgressBar_text));
            setTextColor(a.getColor(R.styleable.TextProgressBar_textColor, Color.WHITE));
            setTextSize(a.getDimension(R.styleable.TextProgressBar_textSize, 15));
            a.recycle();
        }
    }
}
