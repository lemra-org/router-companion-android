package org.rm3l.router_companion.widgets;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import org.rm3l.router_companion.R;

@Deprecated
public class NowLayout extends LinearLayout implements ViewTreeObserver.OnGlobalLayoutListener {

    public NowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initLayoutObserver();

    }

    public NowLayout(Context context) {
        super(context);
        initLayoutObserver();
    }

    private void initLayoutObserver() {
        setOrientation(LinearLayout.VERTICAL);
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    public void onGlobalLayout() {
        final ViewTreeObserver viewTreeObserver = getViewTreeObserver();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            viewTreeObserver.removeOnGlobalLayoutListener(this);
        } else {
            viewTreeObserver.removeGlobalOnLayoutListener(this);
        }

        final int heightPx = getContext().getResources().getDisplayMetrics().heightPixels;

        boolean inversed = false;
        final int childCount = getChildCount();

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);

            int[] location = new int[2];

            child.getLocationOnScreen(location);

            if (location[1] > heightPx) {
                break;
            }

            if (!inversed) {
                child.startAnimation(AnimationUtils.loadAnimation(getContext(),
                        R.anim.slide_up_left));
            } else {
                child.startAnimation(AnimationUtils.loadAnimation(getContext(),
                        R.anim.slide_up_right));
            }

            inversed = !inversed;
        }

    }

}