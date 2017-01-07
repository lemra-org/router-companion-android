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

package org.rm3l.router_companion.tiles.status.router;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;

import org.apache.commons.lang3.StringUtils;
import org.rm3l.router_companion.BuildConfig;
import org.rm3l.router_companion.R;
import org.rm3l.router_companion.actions.activity.OpenWebManagementPageActivity;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.resources.PublicIPInfo;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.service.tasks.PublicIPChangesServiceTask;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.customtabs.CustomTabActivityHelper;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static org.rm3l.router_companion.mgmt.RouterManagementActivity.ROUTER_SELECTED;
import static org.rm3l.router_companion.tiles.dashboard.network.NetworkTopologyMapTile.INTERNET_CONNECTIVITY_PUBLIC_IP;
import static org.rm3l.router_companion.RouterCompanionAppConstants.DDWRT_SCM_CHANGESET_URL_BASE;
import static org.rm3l.router_companion.RouterCompanionAppConstants.NOK;
import static org.rm3l.router_companion.RouterCompanionAppConstants.UNKNOWN;
import static org.rm3l.router_companion.utils.Utils.isDemoRouter;

/**
 *
 */
public class StatusRouterStateTile extends DDWRTTile<NVRAMInfo> {

    public static final Splitter SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();
    private static final String LOG_TAG = StatusRouterStateTile.class.getSimpleName();
    private long mLastSync;
    private boolean checkActualInternetConnectivity = true;

    public StatusRouterStateTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_status_router_router_state,
                null);
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

                    if (mParentFragmentPreferences != null) {
                        checkActualInternetConnectivity = mParentFragmentPreferences
                                .getBoolean(RouterCompanionAppConstants.OVERVIEW_NTM_CHECK_ACTUAL_INTERNET_CONNECTIVITY_PREF, true);
                    }

                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Init background loader for " + StatusRouterStateTile.class + ": routerInfo=" +
                            mRouter + " / nbRunsLoader=" + nbRunsLoader);

                    if (mRefreshing.getAndSet(true)) {
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    updateProgressBarViewSeparator(0);

                    mLastSync = System.currentTimeMillis();

                    final NVRAMInfo nvramInfo = new NVRAMInfo();

                    NVRAMInfo nvramInfoTmp = null;
                    try {
                        updateProgressBarViewSeparator(10);
                        if (isDemoRouter(mRouter)) {
                            nvramInfoTmp = new NVRAMInfo()
                                    .setProperty(NVRAMInfo.ROUTER_NAME, "Demo Router (Test Data)")
                                    .setProperty(NVRAMInfo.WAN_IPADDR, "1.2.3.4")
                                    .setProperty(NVRAMInfo.MODEL, "Router Model Family")
                                    .setProperty(NVRAMInfo.DIST_TYPE, "Linux 2.4.37 #7583 Sat Oct 10 mips")
                                    .setProperty(NVRAMInfo.LAN_IPADDR, "255.255.255.255")
                                    .setProperty(NVRAMInfo.OS_VERSION,
                                            Integer.toString(1 + new Random().nextInt(65535)));
                        } else {
                            nvramInfoTmp =
                                    SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity, mRouter,
                                            mGlobalPreferences, NVRAMInfo.ROUTER_NAME,
                                            NVRAMInfo.WAN_IPADDR,
                                            NVRAMInfo.MODEL,
                                            NVRAMInfo.DIST_TYPE,
                                            NVRAMInfo.LAN_IPADDR);
                        }
                        updateProgressBarViewSeparator(50);
                    } finally {
                        if (nvramInfoTmp != null) {
                            nvramInfo.putAll(nvramInfoTmp);
                        }
                        //date -d @$(( $(date +%s) - $(cut -f1 -d. /proc/uptime) ))
                        //date -d @$(sed -n '/^btime /s///p' /proc/stat)
                        final String[] otherCmds;
                        if (isDemoRouter(mRouter)) {
                            otherCmds = new String[5];
                            final Date date = new Date();
                            otherCmds[0] = date.toString(); //current date
                            final Calendar cal = Calendar.getInstance();
                            cal.setTime(date);
                            cal.add(Calendar.DATE, -77);
                            otherCmds[1] = cal.getTime().toString(); //date since last reboot
                            otherCmds[2] = " 77 days, 11:00,  2 users, ";
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
                                            "uptime | awk -F'up' '{print $2}' | awk -F'users' '{print $1}' | awk -F'load' '{print $1}'",
                                            "uname -a",
                                            "echo \"`cat /tmp/loginprompt|grep DD-WRT|cut -d' ' -f1` `cat /tmp/loginprompt|grep DD-WRT|cut -d' ' -f2` (`cat /tmp/loginprompt|grep Release|cut -d' ' -f2`) " +
                                                    "`cat /tmp/loginprompt|grep DD-WRT|cut -d' ' -f3` - SVN rev: `/sbin/softwarerevision`\"");
                        }
                        updateProgressBarViewSeparator(75);

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
                                        uptime += (" (up " + elapsedFromUptime + ")");
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
                                final String fwString = otherCmds[4];
                                nvramInfo.setProperty(NVRAMInfo.FIRMWARE, fwString);

                                final List<String> strings = Splitter.on("rev:")
                                        .omitEmptyStrings().trimResults().splitToList(fwString);
                                if (strings.size() >= 2) {
                                    try {
                                        nvramInfo.setProperty(NVRAMInfo.OS_VERSION,
                                                Long.toString(Long.parseLong(
                                                        strings.get(1).trim())));
                                    } catch (final NumberFormatException nfe) {
                                        //No worries
                                    }
                                }
                            }
                        }

                        updateProgressBarViewSeparator(80);
                        if (checkActualInternetConnectivity) {
                            try {

                                if (isDemoRouter(mRouter)) {
                                    final long nbRunsLoaderModulo = (nbRunsLoader % 5);
                                    if (nbRunsLoaderModulo == 0) {
                                        //nbRunsLoader = 5k
                                        nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP,
                                                "52.64." +
                                                        (1 + new Random().nextInt(252))
                                                        + "." +
                                                        (1 + new Random().nextInt(252)));
                                    } else if (nbRunsLoaderModulo == 1) {
                                        //nbRunsLoader = 5k + 1
                                        nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, NOK);
                                    } else if (nbRunsLoaderModulo == 2) {
                                        //nbRunsLoader = 5k + 2
                                        nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, UNKNOWN);
                                    }
                                } else {
                                    //Check actual connections to the outside from the router
                                    final CharSequence applicationName = Utils.getApplicationName(mParentFragmentActivity);
                                    final String[] wanPublicIpCmdStatus = SSHUtils.getManualProperty(mParentFragmentActivity,
                                            mRouter, mGlobalPreferences,
//                                        "echo -e \"GET / HTTP/1.1\\r\\nHost:icanhazip.com\\r\\nUser-Agent:DD-WRT Companion/3.3.0\\r\\n\" | nc icanhazip.com 80"
                                            String.format("echo -e \"" +
                                                            "GET / HTTP/1.1\\r\\n" +
                                                            "Host:%s\\r\\n" +
                                                            "User-Agent:%s/%s\\r\\n\" " +
                                                            "| /usr/bin/nc %s %d",
                                                    PublicIPInfo.ICANHAZIP_HOST,
                                                    applicationName != null ? applicationName : BuildConfig.APPLICATION_ID,
                                                    BuildConfig.VERSION_NAME,
                                                    PublicIPInfo.ICANHAZIP_HOST,
                                                    PublicIPInfo.ICANHAZIP_PORT));
                                    Crashlytics.log(Log.DEBUG, LOG_TAG, "wanPublicIpCmdStatus: " + Arrays.toString(wanPublicIpCmdStatus));
                                    if (wanPublicIpCmdStatus == null || wanPublicIpCmdStatus.length == 0) {
                                        nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, NOK);
                                    } else {
                                        final String wanPublicIp = wanPublicIpCmdStatus[wanPublicIpCmdStatus.length - 1]
                                                .trim();
                                        if (Patterns.IP_ADDRESS.matcher(wanPublicIp).matches()) {
                                            nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, wanPublicIp);

                                            PublicIPChangesServiceTask.buildNotificationIfNeeded(mParentFragmentActivity,
                                                    mRouter, mParentFragmentPreferences,
                                                    wanPublicIpCmdStatus,
                                                    nvramInfo.getProperty(NVRAMInfo.WAN_IPADDR), null);

                                        } else {
                                            nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, NOK);
                                        }
                                    }
                                }
                            } catch (final Exception e) {
                                e.printStackTrace();
                                nvramInfo.setProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, UNKNOWN);
                            } finally {
                                runBgServiceTaskAsync();
                            }
                        }
                        updateProgressBarViewSeparator(90);
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
        try {
            //Set tiles
            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

            layout.findViewById(R.id.tile_status_router_router_state_loading_view)
                    .setVisibility(View.GONE);
            layout.findViewById(R.id.tile_status_router_router_state_header_loading_view)
                    .setVisibility(View.GONE);
            layout.findViewById(R.id.tile_status_router_router_state_gridLayout)
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

                //OS Version
                final TextView osVersionTv = (TextView)
                        this.layout.findViewById(R.id.tile_status_router_router_state_os_version);
                final String osVersion = data.getProperty(NVRAMInfo.OS_VERSION);
                if (TextUtils.isEmpty(osVersion)) {
                    osVersionTv.setText("-");
                    osVersionTv.setOnClickListener(null);
                } else {

                    osVersionTv.setMovementMethod(LinkMovementMethod.getInstance());
                    osVersionTv.setText(osVersion, TextView.BufferType.SPANNABLE);

                    final Spannable osVersionAsSpannable = (Spannable) osVersionTv.getText();

                    final String changesetUrl = String.format("%s/%s",
                            DDWRT_SCM_CHANGESET_URL_BASE, osVersion);
                    osVersionAsSpannable.setSpan(
                            new ClickableSpan() {
                                @Override
                                public void onClick(View view) {
                                    //Open link to Changeset
                                    final String routerUuid = mRouter.getUuid();
                                    CustomTabActivityHelper.openCustomTab(mParentFragmentActivity, null,
                                            changesetUrl, routerUuid, null,
                                            new CustomTabActivityHelper.CustomTabFallback() {
                                                @Override
                                                public void openUri(Activity activity, Uri uri) {
                                                    //Otherwise, default to a classic WebView implementation
                                                    final Intent webManagementIntent = new Intent(
                                                            mParentFragmentActivity, OpenWebManagementPageActivity.class);
                                                    webManagementIntent.putExtra(ROUTER_SELECTED,
                                                            routerUuid);
                                                    webManagementIntent
                                                            .putExtra(OpenWebManagementPageActivity.URL_TO_OPEN, changesetUrl);
                                                    activity.startActivity(webManagementIntent);
                                                }
                                            }, false);
                                }
                            },
                            0,
                            osVersion.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                //WAN IP
                final String wanIpText = data.getProperty(NVRAMInfo.WAN_IPADDR, "-");
                final TextView wanIpViewDetail = (TextView) this.layout.findViewById(R.id.tile_status_router_router_state_wan_ip_detail);
                wanIpViewDetail.setText(wanIpText);

                final TextView internetIpTitle = (TextView) this.layout.findViewById(R.id.tile_status_router_router_state_internet_ip_title);
                final TextView internetIpTextView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_state_internet_ip);
                if (!checkActualInternetConnectivity) {
                    internetIpTitle.setVisibility(View.GONE);
                    internetIpTextView.setVisibility(View.GONE);
                } else {
                    final String publicIp = data.getProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, null);
                    if (publicIp != null &&
                            !(UNKNOWN.equals(publicIp) || NOK.equals(publicIp))) {
                        internetIpTextView.setText(publicIp);
                    } else {
                        internetIpTextView.setText("-");
                    }
                    if (publicIp != null && publicIp.equalsIgnoreCase(wanIpText)) {
                        //Hide public IP in this case
                        internetIpTitle.setVisibility(View.GONE);
                        internetIpTextView.setVisibility(View.GONE);
                    }

                }

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

                //Update last sync
                final RelativeTimeTextView lastSyncView = (RelativeTimeTextView) layout.findViewById(R.id.tile_last_sync);
                lastSyncView.setReferenceTime(mLastSync);
                lastSyncView.setPrefix("Last sync: ");
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
                updateProgressBarWithError();
            } else if (exception == null){
                updateProgressBarWithSuccess();
            }


            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished(): done loading!");
        } finally {
            mRefreshing.set(false);
            doneWithLoaderInstance(this, loader);
        }
    }

    @Nullable
    @Override
    protected OnClickIntent getOnclickIntent() {
        //TODO
        return null;
    }
}
