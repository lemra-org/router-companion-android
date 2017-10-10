package org.rm3l.router_companion.widgets;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import com.crashlytics.android.Crashlytics;

/**
 * Created by rm3l on 28/03/16.
 */
public class ViewPagerWithAllowedSwipeDirection extends ViewPager {

    public enum SwipeDirection {
        NONE, RIGHT, LEFT, ALL
    }

    private SwipeDirection direction;

    private float initialXValue;

    public ViewPagerWithAllowedSwipeDirection(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.direction = SwipeDirection.ALL;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (this.isSwipeAllowed(event)) {
            return super.onInterceptTouchEvent(event);
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.isSwipeAllowed(event)) {
            return super.onTouchEvent(event);
        }

        return false;
    }

    public void setAllowedSwipeDirection(@NonNull final SwipeDirection direction) {
        this.direction = direction;
    }

    private boolean isSwipeAllowed(MotionEvent event) {
        if (this.direction == SwipeDirection.ALL) {
            return true;
        }

        if (direction == SwipeDirection.NONE) {
            //disable any swipe
            return false;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            initialXValue = event.getX();
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            try {
                float diffX = event.getX() - initialXValue;
                if (diffX > 0 && direction == SwipeDirection.RIGHT) {
                    // swipe from LEFT to RIGHT detected
                    return false;
                } else if (diffX < 0 && direction == SwipeDirection.LEFT) {
                    // swipe from RIGHT to LEFT detected
                    return false;
                }
            } catch (Exception exception) {
                Crashlytics.logException(exception);
                exception.printStackTrace();
            }
        }

        return true;
    }
}
