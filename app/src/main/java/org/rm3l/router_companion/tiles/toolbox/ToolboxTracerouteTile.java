/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014  Armel Soro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact Info: Armel Soro <apps+ddwrt@rm3l.org>
 */
package org.rm3l.router_companion.tiles.toolbox;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import org.rm3l.router_companion.R;
import org.rm3l.router_companion.actions.AbstractRouterAction;
import org.rm3l.router_companion.actions.TracerouteFromRouterAction;
import org.rm3l.router_companion.resources.conn.Router;

public class ToolboxTracerouteTile extends AbstractToolboxTile {

    public ToolboxTracerouteTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router);

    }

    @Nullable
    @Override
    protected Integer getInfoText() {
        return R.string.traceroute_info;
    }

    @Override
    protected int getEditTextHint() {
        return R.string.traceroute_edit_text_hint;
    }

    @Override
    protected int getSubmitButtonText() {
        return R.string.toolbox_traceroute;
    }

    @Override
    protected int getTileTitle() {
        return R.string.traceroute;
    }

    @NonNull
    @Override
    protected AbstractRouterAction<?> getRouterAction(String textToFind) {
        return new TracerouteFromRouterAction(mRouter, mParentFragmentActivity, mRouterActionListener, mGlobalPreferences, textToFind);
    }
}
