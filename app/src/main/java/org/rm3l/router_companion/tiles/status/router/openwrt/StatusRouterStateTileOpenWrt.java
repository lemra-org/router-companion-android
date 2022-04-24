/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014-2022  Armel Soro
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
 * Contact Info: Armel Soro <armel+router_companion@rm3l.org>
 */
package org.rm3l.router_companion.tiles.status.router.openwrt;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.resources.conn.openwrt.UCIInfo;
import org.rm3l.router_companion.tiles.status.router.StatusRouterStateTile;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;

public class StatusRouterStateTileOpenWrt extends StatusRouterStateTile {

  private static final String LOG_TAG = StatusRouterStateTileOpenWrt.class.getSimpleName();

  public StatusRouterStateTileOpenWrt(
      @NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
    super(parentFragment, arguments, router);
  }

  /**
   * Instantiate and return a new Loader for the given ID.
   *
   * @param id The ID whose loader is to be created.
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
          FirebaseCrashlytics.getInstance()
              .log(
                  "Init background loader for "
                      + StatusRouterStateTile.class
                      + ": routerInfo="
                      + mRouter
                      + " / nbRunsLoader="
                      + nbRunsLoader);

          if (mRefreshing.getAndSet(true)) {
            return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
          }
          nbRunsLoader++;

          final NVRAMInfo nvramInfo = new NVRAMInfo();

          NVRAMInfo nvramInfoTmp = null;
          try {
            nvramInfoTmp =
                SSHUtils.getNVRamInfoFromRouter(
                    mParentFragmentActivity,
                    mRouter,
                    mGlobalPreferences,
                    NVRAMInfo.Companion.getROUTER_NAME(),
                    NVRAMInfo.Companion.getDIST_TYPE(),
                    NVRAMInfo.Companion.getLAN_IPADDR());
          } finally {
            if (nvramInfoTmp != null) {
              nvramInfo.putAll(nvramInfoTmp);
            }

            // WAN IPAddress, Router Model
            final String[] infoFromUci =
                SSHUtils.getManualProperty(
                    mParentFragmentActivity,
                    mRouter,
                    mGlobalPreferences,
                    "/sbin/uci -P/var/state show network | grep \""
                        + UCIInfo.Companion.getNETWORK_WAN_IPADDR()
                        + "\" | /usr/bin/awk -F'=' '{print $2}' ; "
                        + "cat /proc/diag/model");
            if (infoFromUci != null) {
              if (infoFromUci.length >= 1) {
                nvramInfo.setProperty(NVRAMInfo.Companion.getWAN_IPADDR(), infoFromUci[0]);
              }
              if (infoFromUci.length >= 2) {
                nvramInfo.setProperty(NVRAMInfo.Companion.getMODEL(), infoFromUci[1]);
              }
            }

            String firmwareInfo = "";
            // FW
            String[] fwInfo =
                SSHUtils.getManualProperty(
                    mParentFragmentActivity,
                    mRouter,
                    mGlobalPreferences,
                    "cat /etc/openwrt_release | grep -E \"DISTRIB_ID.*|DISTRIB_CODENAME.*\";");
            String codename = "";
            // if (fwInfo != null) {
            //  for (int i = 0; i < fwInfo.length; i++) {
            //    final String str = fwInfo[i];
            //    if (StringUtils.startsWith(str, "DISTRIB_ID")) {
            //      firmwareInfo = str.replace("DISTRIB_ID=", "").replaceAll("\"", "");
            //    } else if (StringUtils.startsWith(str, "DISTRIB_CODENAME")) {
            //      codename = str.replace("DISTRIB_CODENAME=", "").replaceAll("\"", "");
            //    }
            //    if (!(isNullOrEmpty(codename) || isNullOrEmpty(firmwareInfo))) {
            //      break;
            //    }
            //  }
            // }
            if (!isNullOrEmpty(codename)) {
              fwInfo =
                  SSHUtils.getManualProperty(
                      mParentFragmentActivity,
                      mRouter,
                      mGlobalPreferences,
                      "cat /etc/banner | grep -i \"" + codename + "\";");
              if (fwInfo != null && fwInfo.length > 0) {
                firmwareInfo += (nullToEmpty(fwInfo[0]).replaceAll("-", ""));
              }
            }
            nvramInfo.setProperty(NVRAMInfo.Companion.getFIRMWARE(), firmwareInfo);

            // Add Kernel and Uptime
            final String[] otherCmds =
                SSHUtils.getManualProperty(
                    mParentFragmentActivity,
                    mRouter,
                    mGlobalPreferences,
                    // date
                    "/bin/date",
                    // date since last reboot
                    "/bin/date -D '%s' -d $(( $(/bin/date +%s) - $(/usr/bin/cut -f1 -d. /proc/uptime) ))",
                    // elapsed from current date
                    "/usr/bin/uptime | /usr/bin/awk -F'up' '{print $2}' | /usr/bin/awk -F'load' '{print $1}'",
                    "/bin/uname -a");

            if (otherCmds != null) {
              if (otherCmds.length >= 1) {
                // date
                nvramInfo.setProperty(NVRAMInfo.Companion.getCURRENT_DATE(), otherCmds[0]);
              }
              if (otherCmds.length >= 3) {
                String uptime = otherCmds[1];
                final String uptimeCmd = otherCmds[2];
                if (!isNullOrEmpty(uptimeCmd)) {
                  final String elapsedFromUptime = Utils.removeLastChar(uptimeCmd.trim());
                  if (!isNullOrEmpty(elapsedFromUptime)) {
                    uptime += ("\n(up " + elapsedFromUptime + ")");
                  }
                }
                nvramInfo.setProperty(NVRAMInfo.Companion.getUPTIME(), uptime);
              }

              // if (otherCmds.length >= 4) {
              //  //Kernel
              //  nvramInfo.setProperty(NVRAMInfo.Companion.getKERNEL(),
              //      StringUtils.replace(StringUtils.replace(otherCmds[3], "GNU/Linux", ""),
              //          nvramInfo.getProperty(NVRAMInfo.Companion.getROUTER_NAME()), ""));
              // }
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

  @Nullable
  @Override
  protected String getLogTag() {
    return LOG_TAG;
  }
}
