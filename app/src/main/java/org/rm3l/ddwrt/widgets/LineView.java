package org.rm3l.ddwrt.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import org.rm3l.ddwrt.utils.ColorUtils;

/**
 * Created by rm3l on 30/08/15.
 */
public class LineView extends View {

    private Paint mPaint;
    private float startX;
    private float startY;
    private float stopX;
    private float stopY;

    public LineView(Context context) {
        super(context);
        initPaint(context);
    }

    private void initPaint(Context context) {
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

    public LineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaint(context);
    }

    public LineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaint(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LineView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initPaint(context);
    }

//    public LineView(Context context, float startX, float startY, float stopX, float stopY) {
//        this(context);
//        this.startX = startX;
//        this.startY = startY;
//        this.stopX = stopX;
//        this.stopY = stopY;
//        initPaint(context);
//    }
//
//    public LineView(Context context, float startX, float startY, float stopX, float stopY, Paint paint) {
//        this(context, startX, startY, stopX, stopY);
//        this.mPaint = paint;
//    }

    public Paint getPaint() {
        return mPaint;
    }

    public LineView setPaint(Paint mPaint) {
        this.mPaint = mPaint;
        invalidate();
        requestLayout();
        return this;
    }

    public float getStartX() {
        return startX;
    }

    public LineView setStartX(float startX) {
        this.startX = startX;
        invalidate();
        requestLayout();
        return this;
    }

    public float getStartY() {
        return startY;
    }

    public LineView setStartY(float startY) {
        this.startY = startY;
        invalidate();
        requestLayout();
        return this;
    }

    public float getStopX() {
        return stopX;
    }

    public LineView setStopX(float stopX) {
        this.stopX = stopX;
        invalidate();
        requestLayout();
        return this;
    }

    public float getStopY() {
        return stopY;
    }

    public LineView setStopY(float stopY) {
        this.stopY = stopY;
        invalidate();
        requestLayout();
        return this;
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
        canvas.drawLine(startX, startY, stopX, stopY, mPaint);
    }
}
