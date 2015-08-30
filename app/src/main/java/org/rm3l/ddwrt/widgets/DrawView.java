package org.rm3l.ddwrt.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import org.rm3l.ddwrt.utils.ColorUtils;

/**
 * Created by rm3l on 30/08/15.
 */
public class DrawView extends View {

    private Paint mPaint;
    private float startX;
    private float startY;
    private float stopX;
    private float stopY;

    public DrawView(Context context) {
        super(context);
        mPaint = new Paint();
        final boolean themeLight = ColorUtils.isThemeLight(context);
        if (themeLight) {
            mPaint.setColor(Color.BLACK);
        } else {
            mPaint.setColor(Color.WHITE);
        }
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStrokeWidth(5);
    }

    public DrawView(Context context, float startX, float startY, float stopX, float stopY) {
        this(context);
        this.startX = startX;
        this.startY = startY;
        this.stopX = stopX;
        this.stopY = stopY;
    }

    public DrawView(Context context, float startX, float startY, float stopX, float stopY, Paint paint) {
        this(context, startX, startY, stopX, stopY);
        this.mPaint = paint;
    }

    public Paint getPaint() {
        return mPaint;
    }

    public DrawView setPaint(Paint mPaint) {
        this.mPaint = mPaint;
        return this;
    }

    public float getStartX() {
        return startX;
    }

    public DrawView setStartX(float startX) {
        this.startX = startX;
        return this;
    }

    public float getStartY() {
        return startY;
    }

    public DrawView setStartY(float startY) {
        this.startY = startY;
        return this;
    }

    public float getStopX() {
        return stopX;
    }

    public DrawView setStopX(float stopX) {
        this.stopX = stopX;
        return this;
    }

    public float getStopY() {
        return stopY;
    }

    public DrawView setStopY(float stopY) {
        this.stopY = stopY;
        return this;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawLine(startX, startY, stopX, stopY, mPaint);
    }
}
