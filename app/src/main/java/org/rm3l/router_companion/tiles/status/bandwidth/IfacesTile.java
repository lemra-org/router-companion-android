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
 * Contact Info: Armel Soro <armel+router_companion@rm3l.org>
 */

package org.rm3l.router_companion.tiles.status.bandwidth;

import static org.rm3l.router_companion.fragments.status.StatusWirelessFragment.SPLITTER;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import java.util.List;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.tiles.status.wan.WANConfigTile;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.SSHUtils;

/** */
public class IfacesTile extends DDWRTTile<NVRAMInfo> {

  private static final String LOG_TAG = IfacesTile.class.getSimpleName();

  protected boolean isThemeLight;

  protected ProgressBar mProgressBar;

  protected TextView mProgressBarDesc;

  private long mLastSync;

  public IfacesTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, Router router) {
    super(parentFragment, arguments, router, R.layout.tile_status_bandwidth_ifaces, null);

    isThemeLight = ColorUtils.Companion.isThemeLight(mParentFragmentActivity);

    mProgressBar = layout.findViewById(R.id.tile_status_bandwidth_ifaces_loading_view);
    mProgressBar.setMax(100);
    mProgressBarDesc = layout.findViewById(R.id.tile_status_bandwidth_ifaces_loading_view_desc);

    if (isThemeLight) {
      mProgressBarDesc.setTextColor(ContextCompat.getColor(mParentFragmentActivity, R.color.black));
    } else {
      mProgressBarDesc.setTextColor(ContextCompat.getColor(mParentFragmentActivity, R.color.white));
    }
    mProgressBarDesc.setText("Loading...");
  }

  public NVRAMInfo doLoadInBackground() {
    try {
      FirebaseCrashlytics.getInstance()
          .log(
              "Init background loader for "
                  + WANConfigTile.class
                  + ": routerInfo="
                  + mRouter
                  + " / nbRunsLoader="
                  + nbRunsLoader);

      if (mRefreshing.getAndSet(true)) {
        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
      }
      nbRunsLoader++;

      updateProgressBarViewSeparator(0);

      mLastSync = System.currentTimeMillis();

      final NVRAMInfo nvramInfo = new NVRAMInfo();

      NVRAMInfo nvramInfoTmp = null;
      try {
        nvramInfoTmp =
            SSHUtils.getNVRamInfoFromRouter(
                mParentFragmentActivity,
                mRouter,
                mGlobalPreferences,
                NVRAMInfo.Companion.getLAN_IFNAME(),
                NVRAMInfo.Companion.getLAN_IFNAMES(),
                NVRAMInfo.Companion.getWAN_IFNAME(),
                NVRAMInfo.Companion.getLANDEVS());
      } finally {
        if (nvramInfoTmp != null) {
          nvramInfo.putAll(nvramInfoTmp);
        }

        String landevs = nvramInfo.getProperty(NVRAMInfo.Companion.getLANDEVS(), null);
        if (Strings.isNullOrEmpty(landevs)) {
          // Atheros
          landevs = nvramInfo.getProperty(NVRAMInfo.Companion.getLAN_IFNAMES(), null);
          if (!Strings.isNullOrEmpty(landevs)) {
            //noinspection ConstantConditions
            nvramInfo.setProperty(NVRAMInfo.Companion.getLANDEVS(), landevs);
          }
        }
        if (landevs != null) {
          final List<String> splitToList = SPLITTER.splitToList(landevs);
          if (splitToList != null && !splitToList.isEmpty()) {

            for (final String landev : splitToList) {
              if (landev == null || landev.isEmpty() || landev.toLowerCase().startsWith("vlan")) {
                continue;
              }
              // Also get Virtual Interfaces
              try {
                final String landevVifsKeyword = landev + "_vifs";
                final NVRAMInfo landevVifsNVRAMInfo =
                    SSHUtils.getNVRamInfoFromRouter(
                        mParentFragmentActivity, mRouter, mGlobalPreferences, landevVifsKeyword);
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
                if (list == null) {
                  continue;
                }
                for (final String landevVif : list) {
                  if (landevVif == null || landevVif.isEmpty()) {
                    continue;
                  }
                  landevs += (" " + landevVif);
                }
              } catch (final Exception e) {
                e.printStackTrace();
                // No worries
              }
            }
          }

          nvramInfo.setProperty(NVRAMInfo.Companion.getLANDEVS(), landevs);
        }
      }

      if (nvramInfo.isEmpty()) {
        throw new DDWRTNoDataException("No Data");
      }

      return nvramInfo;
    } catch (@NonNull final Exception e) {
      e.printStackTrace();
      return new NVRAMInfo().setException(e);
    }
  }

  @Override
  public Integer getTileHeaderViewId() {
    return R.id.tile_status_bandwidth_ifaces_hdr;
  }

  @Override
  public Integer getTileTitleViewId() {
    return R.id.tile_status_bandwidth_ifaces_title;
  }

  @Override
  public void onLoadFinished(@NonNull Loader<NVRAMInfo> loader, @Nullable NVRAMInfo data) {

    try {
      // Set tiles
      FirebaseCrashlytics.getInstance().log("onLoadFinished: loader=" + loader + " / data=" + data);

      layout.findViewById(R.id.tile_status_bandwidth_ifaces_loading_view).setVisibility(View.GONE);
      layout
          .findViewById(R.id.tile_status_bandwidth_ifaces_togglebutton_container)
          .setVisibility(View.VISIBLE);

      mProgressBar.setVisibility(View.GONE);
      mProgressBarDesc.setVisibility(View.GONE);

      if (data == null) {
        data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
      }

      final TextView errorPlaceHolderView =
          this.layout.findViewById(R.id.tile_status_bandwidth_ifaces_error);

      final Exception exception = data.getException();

      if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

        if (exception == null) {
          errorPlaceHolderView.setVisibility(View.GONE);
        }

        // LAN
        final TextView lanIfaceView =
            this.layout.findViewById(R.id.tile_status_bandwidth_ifaces_lan);
        lanIfaceView.setText(data.getProperty(NVRAMInfo.Companion.getLAN_IFNAME(), "-"));

        // WAN
        final TextView wanIfaceView =
            this.layout.findViewById(R.id.tile_status_bandwidth_ifaces_wan);
        wanIfaceView.setText(data.getProperty(NVRAMInfo.Companion.getWAN_IFNAME(), "-"));

        // Wireless
        final TextView wlIfaceView =
            this.layout.findViewById(R.id.tile_status_bandwidth_ifaces_wireless);
        wlIfaceView.setText(data.getProperty(NVRAMInfo.Companion.getLANDEVS(), "-"));

        // Update last sync
        final RelativeTimeTextView lastSyncView = layout.findViewById(R.id.tile_last_sync);
        lastSyncView.setReferenceTime(mLastSync);
        lastSyncView.setPrefix("Last sync: ");
      }

      if (exception != null && !(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {
        //noinspection ThrowableResultOfMethodCallIgnored
        final Throwable rootCause = Throwables.getRootCause(exception);
        errorPlaceHolderView.setText(
            "Error: " + (rootCause != null ? rootCause.getMessage() : "null"));
        final Context parentContext = this.mParentFragmentActivity;
        errorPlaceHolderView.setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(final View v) {
                //noinspection ThrowableResultOfMethodCallIgnored
                if (rootCause != null) {
                  Toast.makeText(parentContext, rootCause.getMessage(), Toast.LENGTH_LONG).show();
                }
              }
            });
        errorPlaceHolderView.setVisibility(View.VISIBLE);
        updateProgressBarWithError();
      } else if (exception == null) {
        updateProgressBarWithSuccess();
      }

      FirebaseCrashlytics.getInstance().log("onLoadFinished(): done loading!");
    } finally {
      mRefreshing.set(false);
      doneWithLoaderInstance(this, loader);
    }
  }

  @Override
  protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
    return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

      @Nullable
      @Override
      public NVRAMInfo loadInBackground() {

        return IfacesTile.this.doLoadInBackground();
      }
    };
  }

  @Override
  protected String getLogTag() {
    return LOG_TAG;
  }

  @Nullable
  @Override
  protected OnClickIntent getOnclickIntent() {
    // TODO
    return null;
  }
}
