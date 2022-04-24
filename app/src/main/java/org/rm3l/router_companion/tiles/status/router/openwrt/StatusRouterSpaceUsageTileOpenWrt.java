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

import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.resources.ProcMountPoint;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.status.router.StatusRouterSpaceUsageTile;
import org.rm3l.router_companion.utils.SSHUtils;

public class StatusRouterSpaceUsageTileOpenWrt extends StatusRouterSpaceUsageTile {

  private static final String LOG_TAG = StatusRouterSpaceUsageTileOpenWrt.class.getSimpleName();

  public StatusRouterSpaceUsageTileOpenWrt(
      @NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
    super(parentFragment, arguments, router);
  }

  @Override
  public void onLoadFinished(@NonNull final Loader<NVRAMInfo> loader, @Nullable NVRAMInfo data) {
    super.onLoadFinished(loader, data);
    ((TextView) this.layout.findViewById(R.id.tile_status_router_router_space_usage_jffs2_title))
        .setText("Overlay");
  }

  @Override
  protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
    return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

      @NonNull
      @Override
      public NVRAMInfo loadInBackground() {

        try {
          FirebaseCrashlytics.getInstance()
              .log(
                  "Init background loader for "
                      + StatusRouterSpaceUsageTile.class
                      + ": routerInfo="
                      + mRouter
                      + " / nbRunsLoader="
                      + nbRunsLoader);

          if (mRefreshing.getAndSet(true)) {
            return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
          }
          nbRunsLoader++;

          final NVRAMInfo nvramInfo = new NVRAMInfo();

          final Map<String, ProcMountPoint> mountPointMap = new HashMap<String, ProcMountPoint>();
          final Map<String, List<ProcMountPoint>> mountTypes =
              new HashMap<String, List<ProcMountPoint>>();

          final String[] nvramInfoOutput =
              SSHUtils.getManualProperty(
                  mParentFragmentActivity, mRouter, mGlobalPreferences, "/usr/sbin/nvram info");
          if (nvramInfoOutput != null && nvramInfoOutput.length >= 1) {
            final String str = Strings.nullToEmpty(nvramInfoOutput[nvramInfoOutput.length - 1]);
            nvramInfo.setProperty(
                "nvram_space", str.replaceAll("bytes", "B").replace(" available", " left"));
          }

          final String[] catProcMounts =
              SSHUtils.getManualProperty(
                  mParentFragmentActivity, mRouter, mGlobalPreferences, "/bin/cat /proc/mounts");
          String cifsMountPoint = null;
          if (catProcMounts != null && catProcMounts.length >= 1) {

            int i = 0;
            for (final String procMountLine : catProcMounts) {
              if (i == 0 || procMountLine == null) {
                i++;
                continue;
              }
              final List<String> procMountLineItem =
                  Splitter.on(" ").omitEmptyStrings().trimResults().splitToList(procMountLine);

              if (procMountLineItem != null) {
                if (procMountLineItem.size() >= 6) {
                  final ProcMountPoint procMountPoint = new ProcMountPoint();
                  procMountPoint.setDeviceType(procMountLineItem.get(0));
                  procMountPoint.setMountPoint(procMountLineItem.get(1));
                  procMountPoint.setFsType(procMountLineItem.get(2));

                  if ("cifs".equalsIgnoreCase(procMountPoint.getFsType())) {
                    cifsMountPoint = procMountPoint.getMountPoint();
                  }

                  final List<String> procMountLineItemPermissions =
                      Splitter.on(",")
                          .omitEmptyStrings()
                          .trimResults()
                          .splitToList(procMountLineItem.get(3));
                  if (procMountLineItemPermissions != null) {
                    for (String procMountLineItemPermission : procMountLineItemPermissions) {
                      procMountPoint.addPermission(procMountLineItemPermission);
                    }
                  }
                  procMountPoint.addOtherAttr(procMountLineItem.get(4));

                  mountPointMap.put(procMountPoint.getMountPoint(), procMountPoint);

                  if (mountTypes.get(procMountPoint.getFsType()) == null) {
                    mountTypes.put(procMountPoint.getFsType(), new ArrayList<ProcMountPoint>());
                  }
                }
              }
            }
          }

          final List<String> itemsToDf = new ArrayList<String>();

          // JFFS Space: "jffs_space"
          final ProcMountPoint jffsProcMountPoint = mountPointMap.get("/overlay");
          if (jffsProcMountPoint != null) {
            itemsToDf.add(jffsProcMountPoint.getMountPoint());
          }

          // CIFS: "cifs_space"
          if (cifsMountPoint != null) {
            final ProcMountPoint cifsProcMountPoint = mountPointMap.get(cifsMountPoint);
            if (cifsProcMountPoint != null) {
              itemsToDf.add(cifsProcMountPoint.getMountPoint());
            }
          }

          for (final String itemToDf : itemsToDf) {
            final String[] itemToDfResult =
                SSHUtils.getManualProperty(
                    mParentFragmentActivity,
                    mRouter,
                    mGlobalPreferences,
                    "df -h " + itemToDf + " | grep -v Filessytem | grep \"" + itemToDf + "\"");
            FirebaseCrashlytics.getInstance()
                .log("catProcMounts: " + Arrays.toString(catProcMounts));
            if (itemToDfResult != null && itemToDfResult.length > 0) {
              final List<String> procMountLineItem =
                  Splitter.on(" ").omitEmptyStrings().trimResults().splitToList(itemToDfResult[0]);
              if (procMountLineItem == null) {
                continue;
              }

              if ("/overlay".equalsIgnoreCase(itemToDf)) {
                if (procMountLineItem.size() >= 4) {
                  nvramInfo.setProperty("jffs_space_max", procMountLineItem.get(1));
                  nvramInfo.setProperty("jffs_space_used", procMountLineItem.get(2));
                  nvramInfo.setProperty("jffs_space_available", procMountLineItem.get(3));
                  nvramInfo.setProperty(
                      "jffs_space",
                      procMountLineItem.get(1) + " (" + procMountLineItem.get(3) + " left)");
                }
              } else if (cifsMountPoint != null && cifsMountPoint.equalsIgnoreCase(itemToDf)) {
                if (procMountLineItem.size() >= 3) {
                  nvramInfo.setProperty("cifs_space_max", procMountLineItem.get(0));
                  nvramInfo.setProperty("cifs_space_used", procMountLineItem.get(1));
                  nvramInfo.setProperty("cifs_space_available", procMountLineItem.get(2));
                  nvramInfo.setProperty(
                      "cifs_space",
                      procMountLineItem.get(0) + " (" + procMountLineItem.get(2) + " left)");
                }
              }
            }
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
