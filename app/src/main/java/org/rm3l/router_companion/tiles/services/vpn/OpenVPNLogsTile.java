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
package org.rm3l.router_companion.tiles.services.vpn;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.ViewGroup;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Splitter;

import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.syslog.StatusSyslogTile;
import org.rm3l.router_companion.utils.SSHUtils;

import java.util.List;

import static org.rm3l.router_companion.resources.conn.NVRAMInfo.SYSLOG;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.SYSLOGD_ENABLE;

public class OpenVPNLogsTile extends StatusSyslogTile {

    public static final String OPENVPN = "openvpn";
    public static final String FULL_LOGS_RETRIEVAL_CMD = "cat /tmp/var/log/messages | grep -i -E \"" + OPENVPN + "\"";

    public OpenVPNLogsTile(@NonNull Fragment parentFragment, @Nullable ViewGroup parentViewGroup, @NonNull Bundle arguments, Router router) {
        super(parentFragment, parentViewGroup, arguments, "OpenVPN Logs", false, router, OPENVPN);
    }

    @Override
    protected String getTitle() {
        return "OpenVPN Logs";
    }

    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {
                try {
                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Init background loader for " + OpenVPNLogsTile.class + ": routerInfo=" +
                            mRouter + " / nbRunsLoader=" + nbRunsLoader);

                    if (mRefreshing.getAndSet(true)) {
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    updateProgressBarViewSeparator(0);

                    mLastSync = System.currentTimeMillis();

                    final NVRAMInfo nvramInfo = new NVRAMInfo();
                    NVRAMInfo nvramInfoTmp = null;
                    String[] logs = null;
                    String logsStr = "";
                    int openvpnclMgmtPort = -1;
                    try {
                        updateProgressBarViewSeparator(10);
                        //Find OpenVPN Management Port
                        final String[] openvpnclConf = SSHUtils.getManualProperty(mParentFragmentActivity, mRouter,
                                mGlobalPreferences, "cat /tmp/openvpncl/openvpn.conf | grep \"management \" 2>/dev/null");
                        if (openvpnclConf != null && openvpnclConf.length > 0) {
                            final String managementLine = openvpnclConf[0];
                            if (managementLine != null) {
                                final List<String> list = Splitter.on(" ").splitToList(managementLine);
                                if (list != null && list.size() >= 3) {
                                    try {
                                        openvpnclMgmtPort = Integer.parseInt(list.get(2));
                                    } catch (final NumberFormatException nfe) {
                                        nfe.printStackTrace();
                                        //No Worries
                                    }
                                }
                            }
                        }
                    } finally {
                        try {
                            updateProgressBarViewSeparator(45);
                            //Telnet on Management Port to retrieve latest logs
                            if (openvpnclMgmtPort > 0) {
                                logs = SSHUtils.execCommandOverTelnet(mParentFragmentActivity, mRouter, mGlobalPreferences, openvpnclMgmtPort,
                                        String.format("log %s", MAX_LOG_LINES));
                                if (logs != null) {
                                    logsStr = LOGS_JOINER.join(logs);
                                }
                            }
                        } finally {
                            try {
                                updateProgressBarViewSeparator(70);
                                nvramInfoTmp = SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity, mRouter, mGlobalPreferences, SYSLOGD_ENABLE);
                            } finally {

                                if (nvramInfoTmp != null) {
                                    nvramInfo.putAll(nvramInfoTmp);
                                }

                                try {
                                    updateProgressBarViewSeparator(80);
                                    //Get last log lines
                                    logs = SSHUtils.getManualProperty(mParentFragmentActivity, mRouter,
                                            mGlobalPreferences, String.format("tail -n %d /tmp/var/log/messages %s",
                                                    MAX_LOG_LINES, " | grep -i -E \"" + OPENVPN + "\""));
                                } finally {
                                    if (logs != null) {
                                        final String logsToSet = logsStr + "\n" + LOGS_JOINER.join(logs);
                                        if (!"\n".equals(logsToSet)) {
                                            nvramInfo.setProperty(SYSLOG, logsToSet);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    updateProgressBarViewSeparator(90);

                    return nvramInfo;

                } catch (@NonNull final Exception e) {
                    e.printStackTrace();
                    return new NVRAMInfo().setException(e);
                }
            }
        };
    }

    @NonNull
    @Override
    protected CharSequence getFullLogsRetrievalCommand() {
        return FULL_LOGS_RETRIEVAL_CMD;
    }
}
