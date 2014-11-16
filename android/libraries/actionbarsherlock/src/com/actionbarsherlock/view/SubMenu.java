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

import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * Subclass of {@link Menu} for sub menus.
 * <p>
 * Sub menus do not support item icons, or nested sub menus.
 * <p/>
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For information about creating menus, read the
 * <a href="{@docRoot}guide/topics/ui/menus.html">Menus</a> developer guide.</p>
 * </div>
 */

public interface SubMenu extends Menu {
    /**
     * Sets the submenu header's title to the title given in <var>titleRes</var>
     * resource identifier.
     *
     * @param titleRes The string resource identifier used for the title.
     * @return This SubMenu so additional setters can be called.
     */
    public SubMenu setHeaderTitle(int titleRes);

    /**
     * Sets the submenu header's title to the title given in <var>title</var>.
     *
     * @param title The character sequence used for the title.
     * @return This SubMenu so additional setters can be called.
     */
    public SubMenu setHeaderTitle(CharSequence title);

    /**
     * Sets the submenu header's icon to the icon given in <var>iconRes</var>
     * resource id.
     *
     * @param iconRes The resource identifier used for the icon.
     * @return This SubMenu so additional setters can be called.
     */
    public SubMenu setHeaderIcon(int iconRes);

    /**
     * Sets the submenu header's icon to the icon given in <var>icon</var>
     * {@link Drawable}.
     *
     * @param icon The {@link Drawable} used for the icon.
     * @return This SubMenu so additional setters can be called.
     */
    public SubMenu setHeaderIcon(Drawable icon);

    /**
     * Sets the header of the submenu to the {@link View} given in
     * <var>view</var>. This replaces the header title and icon (and those
     * replace this).
     *
     * @param view The {@link View} used for the header.
     * @return This SubMenu so additional setters can be called.
     */
    public SubMenu setHeaderView(View view);

    /**
     * Clears the header of the submenu.
     */
    public void clearHeader();

    /**
     * Change the icon associated with this submenu's item in its parent menu.
     *
     * @param iconRes The new icon (as a resource ID) to be displayed.
     * @return This SubMenu so additional setters can be called.
     * @see MenuItem#setIcon(int)
     */
    public SubMenu setIcon(int iconRes);

    /**
     * Change the icon associated with this submenu's item in its parent menu.
     *
     * @param icon The new icon (as a Drawable) to be displayed.
     * @return This SubMenu so additional setters can be called.
     * @see MenuItem#setIcon(Drawable)
     */
    public SubMenu setIcon(Drawable icon);

    /**
     * Gets the {@link MenuItem} that represents this submenu in the parent
     * menu.  Use this for setting additional item attributes.
     *
     * @return The {@link MenuItem} that launches the submenu when invoked.
     */
    public MenuItem getItem();
}
