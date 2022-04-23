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

package org.rm3l.router_companion.tiles.status.router;

import static org.rm3l.router_companion.RouterCompanionAppConstants.NOK;
import static org.rm3l.router_companion.RouterCompanionAppConstants.UNKNOWN;
import static org.rm3l.router_companion.tiles.dashboard.network.NetworkTopologyMapTile.INTERNET_CONNECTIVITY_PUBLIC_IP;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.actions.activity.OpenWebManagementPageActivity;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.firmwares.RemoteDataRetrievalListener;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.customtabs.CustomTabActivityHelper;

/** */
public class StatusRouterStateTile extends DDWRTTile<NVRAMInfo> {

  public static final Splitter SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();

  private static final String LOG_TAG = StatusRouterStateTile.class.getSimpleName();

  private boolean checkActualInternetConnectivity = true;

  private long mLastSync;

  public StatusRouterStateTile(
      @NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
    super(parentFragment, arguments, router, R.layout.tile_status_router_router_state, null);
  }

  @Override
  public Integer getTileHeaderViewId() {
    return R.id.tile_status_router_router_state_hdr;
  }

  @Override
  public Integer getTileTitleViewId() {
    return R.id.tile_status_router_router_state_title;
  }

  @Override
  public void onLoadFinished(@NonNull final Loader<NVRAMInfo> loader, @Nullable NVRAMInfo data) {
    try {
      // Set tiles
      FirebaseCrashlytics.getInstance().log("onLoadFinished: loader=" + loader + " / data=" + data);

      layout
          .findViewById(R.id.tile_status_router_router_state_loading_view)
          .setVisibility(View.GONE);
      layout
          .findViewById(R.id.tile_status_router_router_state_header_loading_view)
          .setVisibility(View.GONE);
      layout
          .findViewById(R.id.tile_status_router_router_state_gridLayout)
          .setVisibility(View.VISIBLE);

      if (data == null) {
        data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
      }

      final TextView errorPlaceHolderView =
          this.layout.findViewById(R.id.tile_status_router_router_state_error);

      final Exception exception = data.getException();

      if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

        if (exception == null) {
          errorPlaceHolderView.setVisibility(View.GONE);
        }

        // Router Name
        final TextView routerNameView =
            this.layout.findViewById(R.id.tile_status_router_router_state_title);
        final String routerName = data.getProperty(NVRAMInfo.Companion.getROUTER_NAME());
        final boolean routerNameNull = (routerName == null);
        String routerNameToSet = routerName;
        if (routerNameNull) {
          routerNameToSet = "(empty)";
        }
        routerNameView.setTypeface(null, routerNameNull ? Typeface.ITALIC : Typeface.NORMAL);

        routerNameView.setText(routerNameToSet);

        ((TextView) layout.findViewById(R.id.tile_status_router_router_state_name))
            .setText(routerNameNull ? "-" : routerName);

        // OS Version
        final TextView osVersionTv =
            this.layout.findViewById(R.id.tile_status_router_router_state_os_version);
        final String osVersion = data.getProperty(NVRAMInfo.Companion.getOS_VERSION());
        if (TextUtils.isEmpty(osVersion)) {
          osVersionTv.setText("-");
          osVersionTv.setOnClickListener(null);
        } else {

          osVersionTv.setMovementMethod(LinkMovementMethod.getInstance());
          osVersionTv.setText(osVersion, TextView.BufferType.SPANNABLE);

          final Spannable osVersionAsSpannable = (Spannable) osVersionTv.getText();

          final String scmChangesetUrl = mRouterConnector.getScmChangesetUrl(osVersion);

          if (TextUtils.isEmpty(scmChangesetUrl)) {
            osVersionTv.setOnClickListener(null);
          } else {
            osVersionAsSpannable.setSpan(
                new ClickableSpan() {
                  @Override
                  public void onClick(View view) {
                    // Open link to Changeset
                    final String routerUuid = mRouter.getUuid();
                    CustomTabActivityHelper.openCustomTab(
                        mParentFragmentActivity,
                        null,
                        scmChangesetUrl,
                        routerUuid,
                        null,
                        new CustomTabActivityHelper.CustomTabFallback() {
                          @Override
                          public void openUri(Activity activity, Uri uri) {
                            // Otherwise, default to a classic WebView implementation
                            final Intent webManagementIntent =
                                new Intent(
                                    mParentFragmentActivity, OpenWebManagementPageActivity.class);
                            webManagementIntent.putExtra(
                                RouterManagementActivity.ROUTER_SELECTED, routerUuid);
                            webManagementIntent.putExtra(
                                OpenWebManagementPageActivity.URL_TO_OPEN, scmChangesetUrl);
                            activity.startActivity(webManagementIntent);
                          }
                        },
                        false);
                  }
                },
                0,
                osVersion.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
          }
        }

        // WAN IP
        final String wanIpText = data.getProperty(NVRAMInfo.Companion.getWAN_IPADDR(), "-");
        final TextView wanIpViewDetail =
            this.layout.findViewById(R.id.tile_status_router_router_state_wan_ip_detail);
        wanIpViewDetail.setText(wanIpText);

        final TextView internetIpTitle =
            this.layout.findViewById(R.id.tile_status_router_router_state_internet_ip_title);
        final TextView internetIpTextView =
            this.layout.findViewById(R.id.tile_status_router_router_state_internet_ip);
        if (!checkActualInternetConnectivity) {
          internetIpTitle.setVisibility(View.GONE);
          internetIpTextView.setVisibility(View.GONE);
        } else {
          final String publicIp = data.getProperty(INTERNET_CONNECTIVITY_PUBLIC_IP, null);
          if (publicIp != null && !(UNKNOWN.equals(publicIp) || NOK.equals(publicIp))) {
            internetIpTextView.setText(publicIp);
          } else {
            internetIpTextView.setText("-");
          }
          if (publicIp != null && publicIp.equalsIgnoreCase(wanIpText)) {
            // Hide public IP in this case
            internetIpTitle.setVisibility(View.GONE);
            internetIpTextView.setVisibility(View.GONE);
          }
        }

        final TextView routerModelView =
            this.layout.findViewById(R.id.tile_status_router_router_state_model);
        final String routerModel = data.getProperty(NVRAMInfo.Companion.getMODEL(), "-");
        routerModelView.setText(routerModel);
        if (mParentFragmentPreferences != null) {
          final String routerModelFromPrefs =
              mParentFragmentPreferences.getString(NVRAMInfo.Companion.getMODEL(), "-");
          //noinspection ConstantConditions
          if (!("-".equals(routerModel) || routerModelFromPrefs.equals(routerModel))) {
            mParentFragmentPreferences
                .edit()
                .putString(NVRAMInfo.Companion.getMODEL(), routerModel)
                .apply();
            Utils.requestBackup(mParentFragmentActivity);
          }
        }

        final TextView lanIpView =
            this.layout.findViewById(R.id.tile_status_router_router_state_lan_ip);
        lanIpView.setText(data.getProperty(NVRAMInfo.Companion.getLAN_IPADDR(), "-"));

        final TextView fwView =
            this.layout.findViewById(R.id.tile_status_router_router_state_firmware);
        fwView.setText(data.getProperty(NVRAMInfo.Companion.getFIRMWARE(), "-"));

        final TextView kernelView =
            this.layout.findViewById(R.id.tile_status_router_router_state_kernel);
        kernelView.setText(data.getProperty(NVRAMInfo.Companion.getKERNEL(), "-"));

        final TextView uptimeView =
            this.layout.findViewById(R.id.tile_status_router_router_state_uptime);
        uptimeView.setText(data.getProperty(NVRAMInfo.Companion.getUPTIME(), "-"));

        final TextView currentDateView =
            this.layout.findViewById(R.id.tile_status_router_router_state_datetime);
        currentDateView.setText(data.getProperty(NVRAMInfo.Companion.getCURRENT_DATE(), "-"));

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
  protected Loader<NVRAMInfo> getLoader(final int id, final Bundle args) {
    return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

      @Nullable
      @Override
      public NVRAMInfo loadInBackground() {

        try {

          if (mParentFragmentPreferences != null) {
            checkActualInternetConnectivity =
                mParentFragmentPreferences.getBoolean(
                    RouterCompanionAppConstants
                        .OVERVIEW_NTM_CHECK_ACTUAL_INTERNET_CONNECTIVITY_PREF,
                    true);
          }

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

          updateProgressBarViewSeparator(0);

          mLastSync = System.currentTimeMillis();

          final NVRAMInfo nvramInfo =
              mRouterConnector.getDataFor(
                  mParentFragmentActivity,
                  mRouter,
                  StatusRouterStateTile.class,
                  new RemoteDataRetrievalListener() {
                    @Override
                    public void doRegardlessOfStatus() {
                      if (checkActualInternetConnectivity) {
                        runBgServiceTaskAsync();
                      }
                    }

                    @Override
                    public void onProgressUpdate(int progress) {
                      updateProgressBarViewSeparator(progress);
                    }
                  });

          if (nvramInfo == null || nvramInfo.isEmpty()) {
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

  @Nullable
  @Override
  protected OnClickIntent getOnclickIntent() {
    // TODO
    return null;
  }
}
