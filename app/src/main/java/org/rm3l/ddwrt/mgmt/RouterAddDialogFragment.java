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

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.widget.EditText;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.prefs.sort.DDWRTSortingStrategy;
import org.rm3l.ddwrt.prefs.sort.SortingStrategy;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.Utils;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.SORTING_STRATEGY_PREF;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.SYNC_INTERVAL_MILLIS_PREF;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.TILE_REFRESH_MILLIS;


public class RouterAddDialogFragment extends AbstractRouterMgmtDialogFragment {

    @Override
    protected CharSequence getDialogMessage() {
        return getString(R.string.router_add_msg);
    }

    @Nullable
    @Override
    protected CharSequence getDialogTitle() {
        return null;
    }

    @Override
    protected CharSequence getPositiveButtonMsg() {
        return getString(R.string.add_router);
    }

    @Override
    protected void onPositiveButtonActionSuccess(@NotNull final RouterMgmtDialogListener mListener,
                                                 @Nullable final Router router, final boolean error) {
        mListener.onRouterAdd(this, error);
        if (!error) {
            if (router != null) {
                //Add default preferences values
                final SharedPreferences sharedPreferences = this.getSherlockActivity()
                        .getSharedPreferences(router.getUuid(), Context.MODE_PRIVATE);
                final SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putLong(SYNC_INTERVAL_MILLIS_PREF, TILE_REFRESH_MILLIS);
                editor.putString(SORTING_STRATEGY_PREF, SortingStrategy.DEFAULT);
                editor.apply();
            }
            Crouton.makeText(getActivity(), "Item added", Style.CONFIRM).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();    //super.onStart() is where dialog.show() is actually called on the underlying dialog, so we have to do it after this point

        @NotNull final AlertDialog d = (AlertDialog) getDialog();
        //Fill the router IP Address with the current gateway address, if any
        try {
            final WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                final DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
                if (dhcpInfo != null) {
                    ((EditText) d.findViewById(R.id.router_add_ip)).setText(Utils.intToIp(dhcpInfo.gateway));
                }
            }
        } catch (@NotNull final Exception e) {
            e.printStackTrace();
            //No worries
        }
    }

    @Nullable
    @Override
    protected Router onPositiveButtonClickHandler(@NotNull Router router) {
        return this.dao.insertRouter(router);
    }

}
