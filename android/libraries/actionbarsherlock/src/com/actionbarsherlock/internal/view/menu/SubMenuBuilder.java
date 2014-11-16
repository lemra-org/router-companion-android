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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;

/**
 * The model for a sub menu, which is an extension of the menu.  Most methods are proxied to
 * the parent menu.
 */
public class SubMenuBuilder extends MenuBuilder implements SubMenu {
    private MenuBuilder mParentMenu;
    private MenuItemImpl mItem;

    public SubMenuBuilder(Context context, MenuBuilder parentMenu, MenuItemImpl item) {
        super(context);

        mParentMenu = parentMenu;
        mItem = item;
    }

    @Override
    public boolean isQwertyMode() {
        return mParentMenu.isQwertyMode();
    }

    @Override
    public void setQwertyMode(boolean isQwerty) {
        mParentMenu.setQwertyMode(isQwerty);
    }

    @Override
    public boolean isShortcutsVisible() {
        return mParentMenu.isShortcutsVisible();
    }

    @Override
    public void setShortcutsVisible(boolean shortcutsVisible) {
        mParentMenu.setShortcutsVisible(shortcutsVisible);
    }

    public Menu getParentMenu() {
        return mParentMenu;
    }

    public MenuItem getItem() {
        return mItem;
    }

    @Override
    public void setCallback(Callback callback) {
        mParentMenu.setCallback(callback);
    }

    @Override
    public MenuBuilder getRootMenu() {
        return mParentMenu;
    }

    @Override
    boolean dispatchMenuItemSelected(MenuBuilder menu, MenuItem item) {
        return super.dispatchMenuItemSelected(menu, item) ||
                mParentMenu.dispatchMenuItemSelected(menu, item);
    }

    public SubMenu setIcon(Drawable icon) {
        mItem.setIcon(icon);
        return this;
    }

    public SubMenu setIcon(int iconRes) {
        mItem.setIcon(iconRes);
        return this;
    }

    public SubMenu setHeaderIcon(Drawable icon) {
        return (SubMenu) super.setHeaderIconInt(icon);
    }

    public SubMenu setHeaderIcon(int iconRes) {
        return (SubMenu) super.setHeaderIconInt(iconRes);
    }

    public SubMenu setHeaderTitle(CharSequence title) {
        return (SubMenu) super.setHeaderTitleInt(title);
    }

    public SubMenu setHeaderTitle(int titleRes) {
        return (SubMenu) super.setHeaderTitleInt(titleRes);
    }

    public SubMenu setHeaderView(View view) {
        return (SubMenu) super.setHeaderViewInt(view);
    }

    @Override
    public boolean expandItemActionView(MenuItemImpl item) {
        return mParentMenu.expandItemActionView(item);
    }

    @Override
    public boolean collapseItemActionView(MenuItemImpl item) {
        return mParentMenu.collapseItemActionView(item);
    }

    @Override
    public String getActionViewStatesKey() {
        final int itemId = mItem != null ? mItem.getItemId() : 0;
        if (itemId == 0) {
            return null;
        }
        return super.getActionViewStatesKey() + ":" + itemId;
    }
}
