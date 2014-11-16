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
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import com.actionbarsherlock.R;
import com.actionbarsherlock.internal.nineoldandroids.animation.Animator;
import com.actionbarsherlock.internal.nineoldandroids.animation.AnimatorSet;
import com.actionbarsherlock.internal.nineoldandroids.animation.ObjectAnimator;
import com.actionbarsherlock.internal.nineoldandroids.view.NineViewGroup;
import com.actionbarsherlock.internal.view.menu.ActionMenuPresenter;
import com.actionbarsherlock.internal.view.menu.ActionMenuView;

import static com.actionbarsherlock.internal.ResourcesCompat.getResources_getBoolean;

public abstract class AbsActionBarView extends NineViewGroup {
    private static final /*Time*/ Interpolator sAlphaInterpolator = new DecelerateInterpolator();
    private static final int FADE_DURATION = 200;
    protected final VisibilityAnimListener mVisAnimListener = new VisibilityAnimListener();
    final Context mContext;
    protected ActionMenuView mMenuView;
    protected ActionMenuPresenter mActionMenuPresenter;
    protected ActionBarContainer mSplitView;
    protected boolean mSplitActionBar;
    protected boolean mSplitWhenNarrow;
    protected int mContentHeight;
    protected Animator mVisibilityAnim;

    public AbsActionBarView(Context context) {
        super(context);
        mContext = context;
    }

    public AbsActionBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public AbsActionBarView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    /*
     * Must be public so we can dispatch pre-2.2 via ActionBarImpl.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            super.onConfigurationChanged(newConfig);
        } else if (mMenuView != null) {
            mMenuView.onConfigurationChanged(newConfig);
        }

        // Action bar can change size on configuration changes.
        // Reread the desired height from the theme-specified style.
        TypedArray a = getContext().obtainStyledAttributes(null, R.styleable.SherlockActionBar,
                R.attr.actionBarStyle, 0);
        setContentHeight(a.getLayoutDimension(R.styleable.SherlockActionBar_height, 0));
        a.recycle();
        if (mSplitWhenNarrow) {
            setSplitActionBar(getResources_getBoolean(getContext(),
                    R.bool.abs__split_action_bar_is_narrow));
        }
        if (mActionMenuPresenter != null) {
            mActionMenuPresenter.onConfigurationChanged(newConfig);
        }
    }

    /**
     * Sets whether the bar should be split right now, no questions asked.
     *
     * @param split true if the bar should split
     */
    public void setSplitActionBar(boolean split) {
        mSplitActionBar = split;
    }

    /**
     * Sets whether the bar should split if we enter a narrow screen configuration.
     *
     * @param splitWhenNarrow true if the bar should check to split after a config change
     */
    public void setSplitWhenNarrow(boolean splitWhenNarrow) {
        mSplitWhenNarrow = splitWhenNarrow;
    }

    public int getContentHeight() {
        return mContentHeight;
    }

    public void setContentHeight(int height) {
        mContentHeight = height;
        requestLayout();
    }

    public void setSplitView(ActionBarContainer splitView) {
        mSplitView = splitView;
    }

    /**
     * @return Current visibility or if animating, the visibility being animated to.
     */
    public int getAnimatedVisibility() {
        if (mVisibilityAnim != null) {
            return mVisAnimListener.mFinalVisibility;
        }
        return getVisibility();
    }

    public void animateToVisibility(int visibility) {
        if (mVisibilityAnim != null) {
            mVisibilityAnim.cancel();
        }
        if (visibility == VISIBLE) {
            if (getVisibility() != VISIBLE) {
                setAlpha(0);
                if (mSplitView != null && mMenuView != null) {
                    mMenuView.setAlpha(0);
                }
            }
            ObjectAnimator anim = ObjectAnimator.ofFloat(this, "alpha", 1);
            anim.setDuration(FADE_DURATION);
            anim.setInterpolator(sAlphaInterpolator);
            if (mSplitView != null && mMenuView != null) {
                AnimatorSet set = new AnimatorSet();
                ObjectAnimator splitAnim = ObjectAnimator.ofFloat(mMenuView, "alpha", 1);
                splitAnim.setDuration(FADE_DURATION);
                set.addListener(mVisAnimListener.withFinalVisibility(visibility));
                set.play(anim).with(splitAnim);
                set.start();
            } else {
                anim.addListener(mVisAnimListener.withFinalVisibility(visibility));
                anim.start();
            }
        } else {
            ObjectAnimator anim = ObjectAnimator.ofFloat(this, "alpha", 0);
            anim.setDuration(FADE_DURATION);
            anim.setInterpolator(sAlphaInterpolator);
            if (mSplitView != null && mMenuView != null) {
                AnimatorSet set = new AnimatorSet();
                ObjectAnimator splitAnim = ObjectAnimator.ofFloat(mMenuView, "alpha", 0);
                splitAnim.setDuration(FADE_DURATION);
                set.addListener(mVisAnimListener.withFinalVisibility(visibility));
                set.play(anim).with(splitAnim);
                set.start();
            } else {
                anim.addListener(mVisAnimListener.withFinalVisibility(visibility));
                anim.start();
            }
        }
    }

    @Override
    public void setVisibility(int visibility) {
        if (mVisibilityAnim != null) {
            mVisibilityAnim.end();
        }
        super.setVisibility(visibility);
    }

    public boolean showOverflowMenu() {
        if (mActionMenuPresenter != null) {
            return mActionMenuPresenter.showOverflowMenu();
        }
        return false;
    }

    public void postShowOverflowMenu() {
        post(new Runnable() {
            public void run() {
                showOverflowMenu();
            }
        });
    }

    public boolean hideOverflowMenu() {
        if (mActionMenuPresenter != null) {
            return mActionMenuPresenter.hideOverflowMenu();
        }
        return false;
    }

    public boolean isOverflowMenuShowing() {
        if (mActionMenuPresenter != null) {
            return mActionMenuPresenter.isOverflowMenuShowing();
        }
        return false;
    }

    public boolean isOverflowReserved() {
        return mActionMenuPresenter != null && mActionMenuPresenter.isOverflowReserved();
    }

    public void dismissPopupMenus() {
        if (mActionMenuPresenter != null) {
            mActionMenuPresenter.dismissPopupMenus();
        }
    }

    protected int measureChildView(View child, int availableWidth, int childSpecHeight,
                                   int spacing) {
        child.measure(MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.AT_MOST),
                childSpecHeight);

        availableWidth -= child.getMeasuredWidth();
        availableWidth -= spacing;

        return Math.max(0, availableWidth);
    }

    protected int positionChild(View child, int x, int y, int contentHeight) {
        int childWidth = child.getMeasuredWidth();
        int childHeight = child.getMeasuredHeight();
        int childTop = y + (contentHeight - childHeight) / 2;

        child.layout(x, childTop, x + childWidth, childTop + childHeight);

        return childWidth;
    }

    protected int positionChildInverse(View child, int x, int y, int contentHeight) {
        int childWidth = child.getMeasuredWidth();
        int childHeight = child.getMeasuredHeight();
        int childTop = y + (contentHeight - childHeight) / 2;

        child.layout(x - childWidth, childTop, x, childTop + childHeight);

        return childWidth;
    }

    protected class VisibilityAnimListener implements Animator.AnimatorListener {
        int mFinalVisibility;
        private boolean mCanceled = false;

        public VisibilityAnimListener withFinalVisibility(int visibility) {
            mFinalVisibility = visibility;
            return this;
        }

        @Override
        public void onAnimationStart(Animator animation) {
            setVisibility(VISIBLE);
            mVisibilityAnim = animation;
            mCanceled = false;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (mCanceled) return;

            mVisibilityAnim = null;
            setVisibility(mFinalVisibility);
            if (mSplitView != null && mMenuView != null) {
                mMenuView.setVisibility(mFinalVisibility);
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            mCanceled = true;
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }
    }
}
