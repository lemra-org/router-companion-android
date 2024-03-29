package org.rm3l.router_companion.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import org.rm3l.router_companion.utils.ColorUtils;

/** Created by rm3l on 30/08/15. */
public class LineView extends ImageView {

  private static final String LOG_TAG = LineView.class.getSimpleName();

  private Paint mPaint;

  private float startX;

  private float startY;

  private float stopX;

  private float stopY;

  public LineView(Context context) {
    super(context);
    initPaint(context);
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

  public Paint getPaint() {
    return mPaint;
  }

  public LineView setPaint(Paint mPaint) {
    this.mPaint = mPaint;
    return this;
  }

  public float getStartX() {
    return startX;
  }

  public LineView setStartX(float startX) {
    this.startX = startX;
    return this;
  }

  public float getStartY() {
    return startY;
  }

  public LineView setStartY(float startY) {
    this.startY = startY;
    return this;
  }

  public float getStopX() {
    return stopX;
  }

  public LineView setStopX(float stopX) {
    this.stopX = stopX;
    return this;
  }

  public float getStopY() {
    return stopY;
  }

  public LineView setStopY(float stopY) {
    this.stopY = stopY;
    return this;
  }

  @Override
  protected void onDraw(@NonNull Canvas canvas) {
    FirebaseCrashlytics.getInstance()
        .log(
            "onDraw: (startX, startY, stopX, stopY) = ("
                + startX
                + ", "
                + startY
                + ", "
                + stopX
                + ", "
                + stopY
                + ")");
    canvas.drawLine(startX, startY, stopX, stopY, mPaint);
    mPaint.setShadowLayer(0, 0, 0, 0);
  }

  private void initPaint(Context context) {
    mPaint = new Paint();
    final boolean themeLight = ColorUtils.Companion.isThemeLight(context);
    if (themeLight) {
      mPaint.setColor(Color.BLACK);
    } else {
      mPaint.setColor(Color.WHITE);
    }
    mPaint.setAntiAlias(true);
    mPaint.setDither(true);
    mPaint.setStrokeWidth(10);
    mPaint.setShadowLayer(4, 2, 2, 0x80000000);
  }
}
