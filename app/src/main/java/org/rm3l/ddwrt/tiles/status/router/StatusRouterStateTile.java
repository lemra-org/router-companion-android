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

package org.rm3l.ddwrt.tiles.status.router;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;

import org.apache.commons.lang3.StringUtils;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.util.Date;

import static org.rm3l.ddwrt.utils.Utils.isDemoRouter;

/**
 *
 */
public class StatusRouterStateTile extends DDWRTTile<NVRAMInfo> {

    public static final Splitter SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();
    private static final String LOG_TAG = StatusRouterStateTile.class.getSimpleName();

    public StatusRouterStateTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_status_router_router_state, R.id.tile_status_router_router_state_togglebutton);
    }

    @Override
    public int getTileHeaderViewId() {
        return R.id.tile_status_router_router_state_hdr;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_status_router_router_state_title;
    }

    @Nullable
    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param id   The ID whose loader is to be created.
     * @param args Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Override
    protected Loader<NVRAMInfo> getLoader(final int id, final Bundle args) {
        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {

                try {
                    Log.d(LOG_TAG, "Init background loader for " + StatusRouterStateTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                        //Skip run
                        Log.d(LOG_TAG, "Skip loader run");
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    final NVRAMInfo nvramInfo = new NVRAMInfo();

                    NVRAMInfo nvramInfoTmp = null;
                    try {
                        if (isDemoRouter(mRouter)) {
                            nvramInfoTmp = new NVRAMInfo()
                                    .setProperty(NVRAMInfo.ROUTER_NAME, "Demo Router")
                                    .setProperty(NVRAMInfo.WAN_IPADDR, "0.0.0.0")
                                    .setProperty(NVRAMInfo.MODEL, "Router Model Family")
                                    .setProperty(NVRAMInfo.DIST_TYPE, "Linux 2.4.37 #7583 Sat Oct 10 mips")
                                    .setProperty(NVRAMInfo.LAN_IPADDR, "255.255.255.255");
                        } else {
                            nvramInfoTmp =
                                    SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity, mRouter,
                                            mGlobalPreferences, NVRAMInfo.ROUTER_NAME,
                                            NVRAMInfo.WAN_IPADDR,
                                            NVRAMInfo.MODEL,
                                            NVRAMInfo.DIST_TYPE,
                                            NVRAMInfo.LAN_IPADDR);
                        }
                    } finally {
                        if (nvramInfoTmp != null) {
                            nvramInfo.putAll(nvramInfoTmp);
                        }
                        //date -d @$(( $(date +%s) - $(cut -f1 -d. /proc/uptime) ))
                        //date -d @$(sed -n '/^btime /s///p' /proc/stat)
                        final String[] otherCmds;
                        if (isDemoRouter(mRouter)) {
                            otherCmds = new String[5];
                            otherCmds[0] = new Date().toString();
                            otherCmds[1] = new Date().toString();
                            otherCmds[2] = " 2 days, 11:00,  2 users, ";
                            otherCmds[3] = "Linux 2.4.37 #7583 Sat Oct 10 mips";
                            otherCmds[4] = "DD-WRT v24-sp2 (10/10/09) - rev 7583";
                        } else {
                            //Add FW, Kernel and Uptime
                            otherCmds = SSHUtils
                                    .getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                            //date
                                            "date",
                                            //date since last reboot
                                            "date -d @$(( $(date +%s) - $(cut -f1 -d. /proc/uptime) )) || " +
                                                    " awk -vuptimediff=\"$(( $(date +%s) - $(cut -f1 -d. /proc/uptime) ))\" " +
                                                    " 'BEGIN { print strftime(\"%Y-%m-%d %H:%M:%S\", uptimediff); }' ",
                                            //elapsed from current date
                                            "uptime | awk -F'up' '{print $2}' | awk -F'load' '{print $1}'",
                                            "uname -a",
                                            "echo \"`cat /tmp/loginprompt|grep DD-WRT|cut -d' ' -f1` `cat /tmp/loginprompt|grep DD-WRT|cut -d' ' -f2` (`cat /tmp/loginprompt|grep Release|cut -d' ' -f2`) " +
                                                    "`cat /tmp/loginprompt|grep DD-WRT|cut -d' ' -f3` - SVN rev: `/sbin/softwarerevision`\"");
                        }

                        if (otherCmds != null) {
                            if (otherCmds.length >= 1) {
                                //date
                                nvramInfo.setProperty(NVRAMInfo.CURRENT_DATE, otherCmds[0]);
                            }
                            if (otherCmds.length >= 3) {
                                String uptime = otherCmds[1];
                                final String uptimeCmd = otherCmds[2];
                                if (!Strings.isNullOrEmpty(uptimeCmd)) {
                                    final String elapsedFromUptime = Utils.removeLastChar(uptimeCmd.trim());
                                    if (!Strings.isNullOrEmpty(elapsedFromUptime)) {
                                        uptime += ("\n(up " + elapsedFromUptime + ")");
                                    }
                                }
                                nvramInfo.setProperty(NVRAMInfo.UPTIME, uptime);
                            }

                            if (otherCmds.length >= 4) {
                                //Kernel
                                nvramInfo.setProperty(NVRAMInfo.KERNEL,
                                        StringUtils.replace(
                                                StringUtils.replace(otherCmds[3], "GNU/Linux", ""),
                                                nvramInfo.getProperty(NVRAMInfo.ROUTER_NAME), ""));
                            }

                            if (otherCmds.length >= 5) {
                                //Firmware
                                nvramInfo.setProperty(NVRAMInfo.FIRMWARE, otherCmds[4]);
                            }
                        }
                    }

                    if (nvramInfo.isEmpty()) {
                        throw new DDWRTNoDataException("No Data!");
                    }

                    return nvramInfo;
                } catch (@NonNull final Exception e) {
                    e.printStackTrace();
                    return new NVRAMInfo().setException(e);
                }

            }
        };
    }

    /**
     * Called when a previously created loader has finished its load.  Note
     * that normally an application is <em>not</em> allowed to commit fragment
     * transactions while in this call, since it can happen after an
     * activity's state is saved.  See {@link android.support.v4.app.FragmentManager#beginTransaction()
     * FragmentManager.openTransaction()} for further discussion on this.
     * <p/>
     * <p>This function is guaranteed to be called prior to the release of
     * the last data that was supplied for this Loader.  At this point
     * you should remove all use of the old data (since it will be released
     * soon), but should not do your own release of the data since its Loader
     * owns it and will take care of that.  The Loader will take care of
     * management of its data so you don't have to.  In particular:
     * <p/>
     * <ul>
     * <li> <p>The Loader will monitor for changes to the data, and report
     * them to you through new calls here.  You should not monitor the
     * data yourself.  For example, if the data is a {@link android.database.Cursor}
     * and you place it in a {@link android.widget.CursorAdapter}, use
     * the {@link android.widget.CursorAdapter#CursorAdapter(android.content.Context,
     * android.database.Cursor, int)} constructor <em>without</em> passing
     * in either {@link android.widget.CursorAdapter#FLAG_AUTO_REQUERY}
     * or {@link android.widget.CursorAdapter#FLAG_REGISTER_CONTENT_OBSERVER}
     * (that is, use 0 for the flags argument).  This prevents the CursorAdapter
     * from doing its own observing of the Cursor, which is not needed since
     * when a change happens you will get a new Cursor throw another call
     * here.
     * <li> The Loader will release the data once it knows the application
     * is no longer using it.  For example, if the data is
     * a {@link android.database.Cursor} from a {@link android.content.CursorLoader},
     * you should not call close() on it yourself.  If the Cursor is being placed in a
     * {@link android.widget.CursorAdapter}, you should use the
     * {@link android.widget.CursorAdapter#swapCursor(android.database.Cursor)}
     * method so that the old Cursor is not closed.
     * </ul>
     *
     * @param loader The Loader that has finished.
     * @param data   The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(@NonNull final Loader<NVRAMInfo> loader, @Nullable NVRAMInfo data) {
        //Set tiles
        Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

        layout.findViewById(R.id.tile_status_router_router_state_loading_view)
                .setVisibility(View.GONE);
        layout.findViewById(R.id.tile_status_router_router_state_header_loading_view)
                .setVisibility(View.GONE);
        layout.findViewById(R.id.tile_status_router_router_state_gridLayout)
                .setVisibility(View.VISIBLE);
        layout.findViewById(R.id.tile_status_router_router_state_header_layout)
                .setVisibility(View.VISIBLE);

        if (data == null) {
            data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
        }

        final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_state_error);

        final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

            if (exception == null) {
                errorPlaceHolderView.setVisibility(View.GONE);
            }

            //Router Name
            final TextView routerNameView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_state_title);
            final String routerName = data.getProperty(NVRAMInfo.ROUTER_NAME);
            final boolean routerNameNull = (routerName == null);
            String routerNameToSet = routerName;
            if (routerNameNull) {
                routerNameToSet = "(empty)";
            }
            routerNameView.setTypeface(null, routerNameNull ? Typeface.ITALIC : Typeface.NORMAL);

            routerNameView.setText(routerNameToSet);

            ((TextView) layout.findViewById(R.id.tile_status_router_router_state_name))
                    .setText(routerNameNull ? "-" : routerName);

            //WAN IP
            final TextView wanIpView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_state_wan_ip);
            wanIpView.setText(data.getProperty(NVRAMInfo.WAN_IPADDR, "-"));

            final TextView routerModelView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_state_model);
            final String routerModel = data.getProperty(NVRAMInfo.MODEL, "-");
            routerModelView.setText(routerModel);
            if (mParentFragmentPreferences != null) {
                final String routerModelFromPrefs =
                        mParentFragmentPreferences.getString(NVRAMInfo.MODEL, "-");
                //noinspection ConstantConditions
                if (!("-".equals(routerModel) ||
                        routerModelFromPrefs.equals(routerModel))) {
                    mParentFragmentPreferences
                            .edit()
                            .putString(NVRAMInfo.MODEL, routerModel)
                            .apply();
                    Utils.requestBackup(mParentFragmentActivity);
                }
            }

            final TextView lanIpView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_state_lan_ip);
            lanIpView.setText(data.getProperty(NVRAMInfo.LAN_IPADDR, "-"));

            final TextView fwView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_state_firmware);
            fwView.setText(data.getProperty(NVRAMInfo.FIRMWARE, "-"));

            final TextView kernelView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_state_kernel);
            kernelView.setText(data.getProperty(NVRAMInfo.KERNEL, "-"));

            final TextView uptimeView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_state_uptime);
            uptimeView.setText(data.getProperty(NVRAMInfo.UPTIME, "-"));

            final TextView currentDateView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_state_datetime);
            currentDateView.setText(data.getProperty(NVRAMInfo.CURRENT_DATE, "-"));
        }

        if (exception != null && !(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {
            //noinspection ThrowableResultOfMethodCallIgnored
            final Throwable rootCause = Throwables.getRootCause(exception);
            errorPlaceHolderView.setText("Error: " + (rootCause != null ? rootCause.getMessage() : "null"));
            final Context parentContext = this.mParentFragmentActivity;
            errorPlaceHolderView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    //noinspection ThrowableResultOfMethodCallIgnored
                    if (rootCause != null) {
                        Toast.makeText(parentContext,
                                rootCause.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
            errorPlaceHolderView.setVisibility(View.VISIBLE);
        }

        doneWithLoaderInstance(this, loader,
                R.id.tile_status_router_router_state_togglebutton_title, R.id.tile_status_router_router_state_togglebutton_separator);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");

    }

    @Nullable
    @Override
    protected OnClickIntent getOnclickIntent() {
        //TODO
        return null;
    }
}
