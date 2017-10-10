package org.rm3l.router_companion.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import mbanje.kurt.fabbutton.FabButton;

/**
 * Created by rm3l on 29/01/16.
 */
public class FabButtonMutuallyExclusive extends FabButton {

    private View[] mDependentViews;

    public FabButtonMutuallyExclusive(Context context) {
        super(context);
    }

    public FabButtonMutuallyExclusive(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FabButtonMutuallyExclusive(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public View[] getDependentViews() {
        return mDependentViews;
    }

    public FabButtonMutuallyExclusive setDependentViews(View... mDependentViews) {
        this.mDependentViews = mDependentViews;
        return this;
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        final boolean isThisViewVisible = (this.getVisibility() == View.VISIBLE);
        if (mDependentViews != null) {
            for (final View dependentView : mDependentViews) {
                if (dependentView == null) {
                    continue;
                }
                dependentView.setVisibility(isThisViewVisible ? View.GONE : View.VISIBLE);
            }
        }
    }
}
