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

package com.actionbarsherlock.internal.view;

import android.view.View;

import com.actionbarsherlock.internal.view.menu.SubMenuWrapper;
import com.actionbarsherlock.view.ActionProvider;

public class ActionProviderWrapper extends android.view.ActionProvider {
    private final ActionProvider mProvider;


    public ActionProviderWrapper(ActionProvider provider) {
        super(null/*TODO*/); //XXX this *should* be unused
        mProvider = provider;
    }


    public ActionProvider unwrap() {
        return mProvider;
    }

    @Override
    public View onCreateActionView() {
        return mProvider.onCreateActionView();
    }

    @Override
    public boolean hasSubMenu() {
        return mProvider.hasSubMenu();
    }

    @Override
    public boolean onPerformDefaultAction() {
        return mProvider.onPerformDefaultAction();
    }

    @Override
    public void onPrepareSubMenu(android.view.SubMenu subMenu) {
        mProvider.onPrepareSubMenu(new SubMenuWrapper(subMenu));
    }
}
