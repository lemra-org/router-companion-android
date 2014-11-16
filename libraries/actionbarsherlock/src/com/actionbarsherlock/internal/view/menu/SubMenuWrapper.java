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

package com.actionbarsherlock.internal.view.menu;

import android.graphics.drawable.Drawable;
import android.view.View;

import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;

public class SubMenuWrapper extends MenuWrapper implements SubMenu {
    private final android.view.SubMenu mNativeSubMenu;
    private MenuItem mItem = null;

    public SubMenuWrapper(android.view.SubMenu nativeSubMenu) {
        super(nativeSubMenu);
        mNativeSubMenu = nativeSubMenu;
    }


    @Override
    public SubMenu setHeaderTitle(int titleRes) {
        mNativeSubMenu.setHeaderTitle(titleRes);
        return this;
    }

    @Override
    public SubMenu setHeaderTitle(CharSequence title) {
        mNativeSubMenu.setHeaderTitle(title);
        return this;
    }

    @Override
    public SubMenu setHeaderIcon(int iconRes) {
        mNativeSubMenu.setHeaderIcon(iconRes);
        return this;
    }

    @Override
    public SubMenu setHeaderIcon(Drawable icon) {
        mNativeSubMenu.setHeaderIcon(icon);
        return this;
    }

    @Override
    public SubMenu setHeaderView(View view) {
        mNativeSubMenu.setHeaderView(view);
        return this;
    }

    @Override
    public void clearHeader() {
        mNativeSubMenu.clearHeader();
    }

    @Override
    public SubMenu setIcon(int iconRes) {
        mNativeSubMenu.setIcon(iconRes);
        return this;
    }

    @Override
    public SubMenu setIcon(Drawable icon) {
        mNativeSubMenu.setIcon(icon);
        return this;
    }

    @Override
    public MenuItem getItem() {
        if (mItem == null) {
            mItem = new MenuItemWrapper(mNativeSubMenu.getItem());
        }
        return mItem;
    }
}
