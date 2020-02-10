package org.rm3l.router_companion.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.utils.ColorUtils;

public class RippleViewCreator extends FrameLayout {

  private float duration = 150;

  private float endRadius = 0;

  private int frameRate = 15;

  private Handler handler = new Handler();

  private int height = 0;

  private Paint paint = new Paint();

  private float radius = 0;

  private float rippleX = 0;

  private float rippleY = 0;

  private float speed = 1;

  private int touchAction;

  private int width = 0;

  public static RippleViewCreator addRippleToView(View v) {
    ViewGroup parent = (ViewGroup) v.getParent();
    int index = -1;
    if (parent != null) {
      index = parent.indexOfChild(v);
      parent.removeView(v);
    }
    final RippleViewCreator rippleViewCreator = new RippleViewCreator(v.getContext());
    rippleViewCreator.setLayoutParams(v.getLayoutParams());
    if (parent != null) {
      if (index == -1) {
        parent.addView(rippleViewCreator, index);
      } else {
        parent.addView(rippleViewCreator);
      }
    }
    rippleViewCreator.addView(v);

    return rippleViewCreator;
  }

  public RippleViewCreator(Context context) {
    this(context, null, 0);
  }

  public RippleViewCreator(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public RippleViewCreator(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  @Override
  public final void addView(@NonNull View child, int index, ViewGroup.LayoutParams params) {
    // limit one view
    if (getChildCount() > 0) {
      throw new IllegalStateException(this.getClass().toString() + " can only have one child.");
    }
    super.addView(child, index, params);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent event) {
    return true;
  }

  @Override
  public boolean onTouchEvent(@NonNull MotionEvent event) {
    rippleX = event.getX();
    rippleY = event.getY();

    touchAction = event.getAction();
    switch (event.getAction()) {
      case MotionEvent.ACTION_UP:
        {
          getParent().requestDisallowInterceptTouchEvent(false);

          radius = 1;
          endRadius =
              Math.max(Math.max(Math.max(width - rippleX, rippleX), rippleY), height - rippleY);
          speed = endRadius / duration * frameRate;
          handler.postDelayed(
              new Runnable() {
                @Override
                public void run() {
                  if (radius < endRadius) {
                    radius += speed;
                    paint.setAlpha(90 - (int) (radius / endRadius * 90));
                    handler.postDelayed(this, frameRate);
                  } else if (getChildAt(0) != null) {
                    getChildAt(0).performClick();
                  }
                }
              },
              frameRate);
          break;
        }
      case MotionEvent.ACTION_CANCEL:
        {
          getParent().requestDisallowInterceptTouchEvent(false);
          break;
        }
      case MotionEvent.ACTION_DOWN:
        {
          getParent().requestDisallowInterceptTouchEvent(true);
          endRadius =
              Math.max(Math.max(Math.max(width - rippleX, rippleX), rippleY), height - rippleY);
          paint.setAlpha(90);
          radius = endRadius / 3;
          invalidate();
          return true;
        }
      case MotionEvent.ACTION_MOVE:
        {
          if (rippleX < 0 || rippleX > width || rippleY < 0 || rippleY > height) {
            getParent().requestDisallowInterceptTouchEvent(false);
            touchAction = MotionEvent.ACTION_CANCEL;
            break;
          } else {
            invalidate();
            return true;
          }
        }
    }
    invalidate();
    return false;
  }

  @Override
  protected void dispatchDraw(@NonNull Canvas canvas) {
    super.dispatchDraw(canvas);

    if (radius > 0 && radius < endRadius) {
      canvas.drawCircle(rippleX, rippleY, radius, paint);
      if (touchAction == MotionEvent.ACTION_UP) {
        invalidate();
      }
    }
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    width = w;
    height = h;
  }

  private void init() {
    if (isInEditMode()) {
      return;
    }

    final Context context = getContext();

    paint.setStyle(Paint.Style.FILL);
    paint.setColor(
        ContextCompat.getColor(
            context,
            ColorUtils.Companion.isThemeLight(context)
                ? R.color.control_highlight_color
                : R.color.control_highlight_color_dark));
    paint.setAntiAlias(true);

    setWillNotDraw(true);
    setDrawingCacheEnabled(true);
    setClickable(true);
  }
}
