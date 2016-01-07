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

package org.rm3l.ddwrt.mgmt;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.prefs.sort.SortingStrategy;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.Utils;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.AUTO_REFRESH_INTERVAL_SECONDS_PREF;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.SORTING_STRATEGY_PREF;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.TILE_REFRESH_SECONDS;

public class RouterDuplicateDialogFragment extends RouterUpdateDialogFragment {

    @Override
    protected CharSequence getDialogMessage() {
        return getString(R.string.router_add_msg);
    }

    @Override
    protected CharSequence getPositiveButtonMsg() {
        return getString(R.string.add_router);
    }

    @Override
    protected void onPositiveButtonActionSuccess(@NonNull RouterMgmtDialogListener mListener, Router router, boolean error) {
        mListener.onRouterAdd(this, router, error);
        if (!error) {
            if (router != null) {
                //Add default preferences values
                final FragmentActivity activity = this.getActivity();

                final SharedPreferences sharedPreferences = activity
                        .getSharedPreferences(router.getUuid(), Context.MODE_PRIVATE);
                final SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putLong(AUTO_REFRESH_INTERVAL_SECONDS_PREF, TILE_REFRESH_SECONDS);
                editor.putString(SORTING_STRATEGY_PREF, SortingStrategy.DEFAULT);
                editor.apply();

                //Request Backup
                Utils.requestBackup(activity);
            }
//            Crouton.makeText(getActivity(), "Item copied as new", Style.CONFIRM).show();
        } else {
            Crouton.makeText(getActivity(), "Error while trying to copy item - please try again later.",
                    Style.ALERT).show();
        }
    }

    @Override
    protected boolean isUpdate() {
        return false;
    }

    @Nullable
    @Override
    protected Router onPositiveButtonClickHandler(@NonNull Router router) {
        return this.dao.insertRouter(router);
    }
}
