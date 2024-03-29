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

package org.rm3l.router_companion.fragments.status;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.content.Loader;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.fragments.AbstractBaseFragment;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.tiles.status.wireless.WirelessIfaceTile;
import org.rm3l.router_companion.tiles.status.wireless.WirelessIfacesTile;
import org.rm3l.router_companion.utils.NVRAMParser;
import org.rm3l.router_companion.utils.SSHUtils;

/** */
public class StatusWirelessFragment extends AbstractBaseFragment<Collection<WirelessIfaceTile>> {

  public static final Splitter SPLITTER = Splitter.on(" ").omitEmptyStrings().trimResults();

  private static final String LOG_TAG = StatusWirelessFragment.class.getSimpleName();

  // Save views for the very first time
  private final List<String> ifacesViews = Lists.newArrayList();

  @Nullable private final Collection<WirelessIfaceTile> mIfaceTiles = null;

  private Loader<Collection<WirelessIfaceTile>> mLoader;

  @Nullable
  public static Collection<WirelessIfaceTile> getWirelessIfaceTiles(
      @NonNull final Bundle args,
      FragmentActivity activity,
      Fragment parentFragment,
      Router router) {
    try {
      FirebaseCrashlytics.getInstance()
          .log(
              "Init background loader for "
                  + StatusWirelessFragment.class
                  + ": routerInfo="
                  + router);

      SharedPreferences sharedPreferences =
          activity.getSharedPreferences(
              RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
      final NVRAMInfo nvramInfo =
          SSHUtils.getNVRamInfoFromRouter(
              activity,
              router,
              sharedPreferences,
              NVRAMInfo.Companion.getLANDEVS(),
              NVRAMInfo.Companion.getLAN_IFNAMES());

      if (nvramInfo == null) {
        return null;
      }

      String landevs = nvramInfo.getProperty(NVRAMInfo.Companion.getLANDEVS());
      if (Strings.isNullOrEmpty(landevs)) {
        // Atheros
        landevs = nvramInfo.getProperty(NVRAMInfo.Companion.getLAN_IFNAMES(), null);
        if (!Strings.isNullOrEmpty(landevs)) {
          //noinspection ConstantConditions
          nvramInfo.setProperty(NVRAMInfo.Companion.getLANDEVS(), landevs);
        }
      }

      final String[] wirelessSsids =
          SSHUtils.getManualProperty(
              activity, router, sharedPreferences, "nvram show | grep 'ssid='");
      if (wirelessSsids == null || wirelessSsids.length == 0) {
        return null;
      }

      final List<String> splitToList = new ArrayList<>();

      for (final String wirelessSsid : wirelessSsids) {
        if (wirelessSsid == null || wirelessSsid.startsWith("af_")) {
          // skip AnchorFree SSID
          continue;
        }
        final List<String> strings = NVRAMParser.SPLITTER.splitToList(wirelessSsid);
        final int size = strings.size();
        if (size == 1) {
          continue;
        }

        if (size >= 2) {
          //                    if (Strings.isNullOrEmpty(strings.get(1))) {
          //                        //skip iterms with no name
          //                        continue;
          //                    }
          final String wlIface = strings.get(0).replace("_ssid", "");
          if (wlIface.contains(".")) {
            // Skip vifs as well, as they will be considered later on
            continue;
          }

          splitToList.add(wlIface);
        }
      }

      //            final List<String> splitToList = SPLITTER.splitToList(landevs != null ? landevs
      // : "");
      if (splitToList.isEmpty()) {
        return null;
      }

      final List<WirelessIfaceTile> tiles = Lists.newArrayList();

      for (final String landevRaw : splitToList) {
        if (landevRaw == null || landevRaw.isEmpty()) {
          continue;
        }
        final String landev = landevRaw.trim();
        if (landev.toLowerCase().startsWith("vlan")) {
          continue;
        }

        final String hwAddrNVRAMProperty = landev + "_hwaddr";
        final NVRAMInfo hwAddrNVRAMInfo =
            SSHUtils.getNVRamInfoFromRouter(
                activity, router, sharedPreferences, hwAddrNVRAMProperty);

        if (hwAddrNVRAMInfo == null
            || Strings.isNullOrEmpty(hwAddrNVRAMInfo.getProperty(hwAddrNVRAMProperty))) {
          // ignore
          continue;
        }

        // Check that iface has a "Security Mode" explicitly set (at least "disabled")
        final String securityModeKeyword = (landev + "_security_mode");
        final NVRAMInfo securityModeFromNvram =
            SSHUtils.getNVRamInfoFromRouter(
                activity, router, sharedPreferences, securityModeKeyword);
        if (securityModeFromNvram == null
            || Strings.isNullOrEmpty(securityModeFromNvram.getProperty(securityModeKeyword))) {
          // Ignore iface
          continue;
        }

        tiles.add(new WirelessIfaceTile(landev, parentFragment, args, router));
        // Also get Virtual Interfaces
        try {
          final String landevVifsKeyword = landev + "_vifs";
          final NVRAMInfo landevVifsNVRAMInfo =
              SSHUtils.getNVRamInfoFromRouter(
                  activity, router, sharedPreferences, landevVifsKeyword);
          if (landevVifsNVRAMInfo == null) {
            continue;
          }
          final String landevVifsNVRAMInfoProp =
              landevVifsNVRAMInfo.getProperty(
                  landevVifsKeyword, RouterCompanionAppConstants.EMPTY_STRING);
          if (landevVifsNVRAMInfoProp == null) {
            continue;
          }
          final List<String> list = SPLITTER.splitToList(landevVifsNVRAMInfoProp);
          for (final String landevVif : list) {
            if (landevVif == null || landevVif.isEmpty()) {
              continue;
            }
            tiles.add(new WirelessIfaceTile(landevVif, landev, parentFragment, args, router));
          }
        } catch (final Exception e) {
          e.printStackTrace();
          // No worries
        }
      }

      if (tiles.isEmpty()) {
        return null;
      }

      return tiles;
    } catch (@NonNull final Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  @Nullable
  @Override
  protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
    return Collections.singletonList(new WirelessIfacesTile(this, savedInstanceState, router));
  }
}
