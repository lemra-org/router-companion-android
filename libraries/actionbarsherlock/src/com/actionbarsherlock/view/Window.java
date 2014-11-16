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

package com.actionbarsherlock.view;

import android.content.Context;

/**
 * <p>Abstract base class for a top-level window look and behavior policy. An
 * instance of this class should be used as the top-level view added to the
 * window manager. It provides standard UI policies such as a background, title
 * area, default key processing, etc.</p>
 * <p/>
 * <p>The only existing implementation of this abstract class is
 * android.policy.PhoneWindow, which you should instantiate when needing a
 * Window. Eventually that class will be refactored and a factory method added
 * for creating Window instances without knowing about a particular
 * implementation.</p>
 */
public abstract class Window extends android.view.Window {
    public static final long FEATURE_ACTION_BAR = android.view.Window.FEATURE_ACTION_BAR;
    public static final long FEATURE_ACTION_BAR_OVERLAY = android.view.Window.FEATURE_ACTION_BAR_OVERLAY;
    public static final long FEATURE_ACTION_MODE_OVERLAY = android.view.Window.FEATURE_ACTION_MODE_OVERLAY;
    public static final long FEATURE_NO_TITLE = android.view.Window.FEATURE_NO_TITLE;
    public static final long FEATURE_PROGRESS = android.view.Window.FEATURE_PROGRESS;
    public static final long FEATURE_INDETERMINATE_PROGRESS = android.view.Window.FEATURE_INDETERMINATE_PROGRESS;

    /**
     * Create a new instance for a context.
     *
     * @param context Context.
     */
    private Window(Context context) {
        super(context);
    }


    public interface Callback {
        /**
         * Called when a panel's menu item has been selected by the user.
         *
         * @param featureId The panel that the menu is in.
         * @param item      The menu item that was selected.
         * @return boolean Return true to finish processing of selection, or
         * false to perform the normal menu handling (calling its
         * Runnable or sending a Message to its target Handler).
         */
        public boolean onMenuItemSelected(int featureId, MenuItem item);
    }
}
