package org.rm3l.router_companion.widgets;

import android.content.Context;
import android.os.Build;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewPropertyAnimatorListener;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import org.rm3l.ddwrt.R;

/**
 * Created by rm3l on 16/12/15.
 */
public class ScrollAwareFABBehavior extends FloatingActionButton.Behavior {

    private static final String TAG = ScrollAwareFABBehavior.class.getSimpleName();

    private static final Interpolator INTERPOLATOR = new FastOutSlowInInterpolator();

    private boolean mIsAnimatingOut = false;

    public ScrollAwareFABBehavior() {
        super();
    }

    public ScrollAwareFABBehavior(Context context, AttributeSet attrs) {
        super();
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, FloatingActionButton child,
            View dependency) {
        return dependency instanceof RecyclerView;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionButton child,
            View dependency) {
        return super.onDependentViewChanged(parent, child, dependency);
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, FloatingActionButton child,
            int layoutDirection) {
        return super.onLayoutChild(parent, child, layoutDirection);
    }

    @Override
    public void onNestedScroll(final CoordinatorLayout coordinatorLayout,
            final FloatingActionButton child, final View target, final int dxConsumed,
            final int dyConsumed, final int dxUnconsumed, final int dyUnconsumed) {

        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed,
                dyUnconsumed);

        if (dyConsumed > 0 && !this.mIsAnimatingOut && child.getVisibility() == View.VISIBLE) {
            // User scrolled down and the FAB is currently visible -> hide the FAB
            animateOut(child);
        } else if (dyConsumed < 0 && child.getVisibility() != View.VISIBLE) {
            // User scrolled up and the FAB is currently not visible -> show the FAB
            animateIn(child);
        }
    }

    @Override
    public boolean onStartNestedScroll(final CoordinatorLayout coordinatorLayout,
            final FloatingActionButton child, final View directTargetChild, final View target,
            final int nestedScrollAxes) {
        // Ensure we react to vertical scrolling
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL || super.onStartNestedScroll(
                coordinatorLayout, child, directTargetChild, target, nestedScrollAxes);
    }

    // Same animation that FloatingActionButton.Behavior uses to show the FAB when the AppBarLayout enters
    private void animateIn(FloatingActionButton button) {

        button.setVisibility(View.VISIBLE);
        if (Build.VERSION.SDK_INT >= 14) {
            ViewCompat.animate(button)
                    .setDuration(300L)
                    .translationY(0.0F)
                    .scaleX(1.0F)
                    .scaleY(1.0F)
                    .alpha(1.0F)
                    .setInterpolator(INTERPOLATOR)
                    .withLayer()
                    .setListener(null)
                    .start();
        } else {
            Animation anim = AnimationUtils.loadAnimation(button.getContext(), R.anim.fab_in);
            anim.setDuration(300L);
            anim.setInterpolator(INTERPOLATOR);
            button.startAnimation(anim);
        }
    }

    // Same animation that FloatingActionButton.Behavior uses to hide the FAB when the AppBarLayout exits
    private void animateOut(final FloatingActionButton button) {
        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) button.getLayoutParams();
        int fabBottom = lp.bottomMargin;

        if (Build.VERSION.SDK_INT >= 14) {
            ViewCompat.animate(button)
                    .setDuration(300L)
                    .translationY(button.getHeight() + fabBottom)
                    .setInterpolator(INTERPOLATOR)
                    .setListener(new ViewPropertyAnimatorListener() {
                        public void onAnimationCancel(View view) {
                            ScrollAwareFABBehavior.this.mIsAnimatingOut = false;
                        }

                        public void onAnimationEnd(View view) {
                            ScrollAwareFABBehavior.this.mIsAnimatingOut = false;
                            view.setVisibility(View.GONE);
                        }

                        public void onAnimationStart(View view) {
                            ScrollAwareFABBehavior.this.mIsAnimatingOut = true;
                        }
                    })
                    .start();
        } else {
            Animation anim = AnimationUtils.loadAnimation(button.getContext(), R.anim.fab_out);
            anim.setInterpolator(INTERPOLATOR);
            anim.setDuration(300L);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationEnd(Animation animation) {
                    ScrollAwareFABBehavior.this.mIsAnimatingOut = false;
                    button.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(final Animation animation) {
                }

                public void onAnimationStart(Animation animation) {
                    ScrollAwareFABBehavior.this.mIsAnimatingOut = true;
                }
            });
            button.startAnimation(anim);
        }
    }
}
