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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.actions.AbstractRouterAction;
import org.rm3l.router_companion.actions.NsLookupFromRouterAction;
import org.rm3l.router_companion.resources.conn.Router;

public class ToolboxNsLookupTile extends AbstractToolboxTile {

    public ToolboxNsLookupTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments,
            @Nullable Router router) {
        super(parentFragment, arguments, router);
    }

    @Override
    protected Integer getEditTextHint() {
        return R.string.nslookup_edit_text_hint;
    }

    @Nullable
    @Override
    protected Integer getInfoText() {
        return R.string.nslookup_info;
    }

    @NonNull
    @Override
    protected AbstractRouterAction getRouterAction(String textToFind) {
        return new NsLookupFromRouterAction(mRouter, mParentFragmentActivity, mRouterActionListener,
                mGlobalPreferences, textToFind);
    }

    @Override
    protected int getSubmitButtonText() {
        return R.string.toolbox_nslookup;
    }

    @Override
    protected int getTileTitle() {
        return R.string.nslookup;
    }
}
