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
package org.rm3l.ddwrt.tiles.services.vpn;

import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.common.base.Splitter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.syslog.StatusSyslogTile;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.SSHUtils;

import java.util.List;
import java.util.Random;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.SYSLOG;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.SYSLOGD_ENABLE;

public class OpenVPNLogsTile extends StatusSyslogTile {

    public static final String OPENVPN = "openvpn";

    public OpenVPNLogsTile(@NotNull SherlockFragment parentFragment, @Nullable ViewGroup parentViewGroup, @NotNull Bundle arguments, Router router) {
        super(parentFragment, parentViewGroup, arguments, "OpenVPN Logs", false, router, OPENVPN);
    }

    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {
                try {
                    Log.d(LOG_TAG, "Init background loader for " + OpenVPNLogsTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                        //Skip run
                        Log.d(LOG_TAG, "Skip loader run");
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    @NotNull final NVRAMInfo nvramInfo = new NVRAMInfo();
                    NVRAMInfo nvramInfoTmp = null;
                    String[] logs = null;
                    String logsStr = "";
                    int openvpnclMgmtPort = -1;
                    try {
                        //Find OpenVPN Management Port
                        final String[] openvpnclConf = SSHUtils.getManualProperty(mRouter,
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
                            //Telnet on Management Port to retrieve latest logs
                            if (openvpnclMgmtPort > 0) {
                                logs = SSHUtils.execCommandOverTelnet(mRouter, mGlobalPreferences, openvpnclMgmtPort,
                                        String.format("log %s", MAX_LOG_LINES));
                                if (logs != null) {
                                    logsStr = LOGS_JOINER.join(logs);
                                }
                            }
                        } finally {
                            try {
                                nvramInfoTmp = SSHUtils.getNVRamInfoFromRouter(mRouter, mGlobalPreferences, SYSLOGD_ENABLE);
                            } finally {

                                if (nvramInfoTmp != null) {
                                    nvramInfo.putAll(nvramInfoTmp);
                                }

                                try {
                                    //Get last log lines
                                    logs = SSHUtils.getManualProperty(mRouter,
                                            mGlobalPreferences, String.format("tail -n %d /tmp/var/log/messages %s",
                                                    MAX_LOG_LINES, " | grep -i -E \"" + OPENVPN + "\""));
                                } finally {
                                    if (logs != null) {
                                        nvramInfo.setProperty(SYSLOG, logsStr + "\n" + LOGS_JOINER.join(logs));
                                    }
                                }
                            }
                        }
                    }

                    return nvramInfo;

                } catch (@NotNull final Exception e) {
                    e.printStackTrace();
                    return new NVRAMInfo().setException(e);
                }
            }
        };
    }

}
