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

import android.view.View;
import android.widget.FrameLayout;

import com.actionbarsherlock.view.CollapsibleActionView;

/**
 * Wraps an ABS collapsible action view in a native container that delegates the calls.
 */
public class CollapsibleActionViewWrapper extends FrameLayout implements android.view.CollapsibleActionView {
    private final CollapsibleActionView child;

    public CollapsibleActionViewWrapper(View child) {
        super(child.getContext());
        this.child = (CollapsibleActionView) child;
        addView(child);
    }

    @Override
    public void onActionViewExpanded() {
        child.onActionViewExpanded();
    }

    @Override
    public void onActionViewCollapsed() {
        child.onActionViewCollapsed();
    }

    public View unwrap() {
        return getChildAt(0);
    }
}
