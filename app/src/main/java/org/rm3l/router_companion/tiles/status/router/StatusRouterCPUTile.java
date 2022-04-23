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

import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.router_companion.utils.Utils.isDemoRouter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.utils.ReportingUtils;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style;

/** */
public class StatusRouterCPUTile extends DDWRTTile<NVRAMInfo> {

  public static final String GREP_MODEL_PROC_CPUINFO =
      "grep -i -E \".*model\" /proc/cpuinfo | awk -F':' '{print $2}' ";

  private static final String LOG_TAG = StatusRouterCPUTile.class.getSimpleName();

  private String[] cpuInfoContents;

  private long mLastSync;

  //    Drawable icon;

  public StatusRouterCPUTile(
      @NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
    super(parentFragment, arguments, router, R.layout.tile_status_router_router_cpu, null);
  }

  @Override
  public Integer getTileHeaderViewId() {
    return R.id.tile_status_router_router_cpu_hdr;
  }

  @Override
  public Integer getTileTitleViewId() {
    return R.id.tile_status_router_router_cpu_title;
  }

  /**
   * Called when a previously created loader has finished its load. Note that normally an
   * application is <em>not</em> allowed to commit fragment transactions while in this call, since
   * it can happen after an activity's state is saved. See {@link
   * androidx.fragment.app.FragmentManager#beginTransaction() FragmentManager.openTransaction()} for
   * further discussion on this.
   *
   * <p>
   *
   * <p>This function is guaranteed to be called prior to the release of the last data that was
   * supplied for this Loader. At this point you should remove all use of the old data (since it
   * will be released soon), but should not do your own release of the data since its Loader owns it
   * and will take care of that. The Loader will take care of management of its data so you don't
   * have to. In particular:
   *
   * <p>
   *
   * <ul>
   *   <li>
   *       <p>The Loader will monitor for changes to the data, and report them to you through new
   *       calls here. You should not monitor the data yourself. For example, if the data is a
   *       {@link android.database.Cursor} and you place it in a {@link
   *       android.widget.CursorAdapter}, use the {@link
   *       android.widget.CursorAdapter#CursorAdapter(android.content.Context, *
   *       android.database.Cursor, int)} constructor <em>without</em> passing in either {@link
   *       android.widget.CursorAdapter#FLAG_AUTO_REQUERY} or {@link
   *       android.widget.CursorAdapter#FLAG_REGISTER_CONTENT_OBSERVER} (that is, use 0 for the
   *       flags argument). This prevents the CursorAdapter from doing its own observing of the
   *       Cursor, which is not needed since when a change happens you will get a new Cursor throw
   *       another call here.
   *   <li>The Loader will release the data once it knows the application is no longer using it. For
   *       example, if the data is a {@link android.database.Cursor} from a {@link
   *       android.content.CursorLoader}, you should not call close() on it yourself. If the Cursor
   *       is being placed in a {@link android.widget.CursorAdapter}, you should use the {@link
   *       android.widget.CursorAdapter#swapCursor(android.database.Cursor)} method so that the old
   *       Cursor is not closed.
   * </ul>
   *
   * @param loader The Loader that has finished.
   * @param data The data generated by the Loader.
   */
  @Override
  public void onLoadFinished(@NonNull final Loader<NVRAMInfo> loader, @Nullable NVRAMInfo data) {
    try {
      // Set tiles
      FirebaseCrashlytics.getInstance().log("onLoadFinished: loader=" + loader + " / data=" + data);

      layout.findViewById(R.id.tile_status_router_router_cpu_loading_view).setVisibility(View.GONE);
      layout.findViewById(R.id.tile_status_router_router_cpu_speed).setVisibility(View.VISIBLE);
      layout
          .findViewById(R.id.tile_status_router_router_cpu_grid_layout)
          .setVisibility(View.VISIBLE);

      if (data == null) {
        data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
      }

      final TextView errorPlaceHolderView =
          this.layout.findViewById(R.id.tile_status_router_router_cpu_error);

      final Exception exception = data.getException();

      if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {
        if (exception == null) {
          errorPlaceHolderView.setVisibility(View.GONE);
        }

        // Clock Frequency
        final TextView cpuSpeedView =
            this.layout.findViewById(R.id.tile_status_router_router_cpu_speed);
        final String property = data.getProperty(NVRAMInfo.Companion.getCPU_CLOCK_FREQ());
        cpuSpeedView.setText(Strings.isNullOrEmpty(property) ? null : (property + " MHz"));

        // Model
        final TextView cpuModelView =
            this.layout.findViewById(R.id.tile_status_router_router_cpu_model);
        cpuModelView.setText(data.getProperty(NVRAMInfo.Companion.getCPU_MODEL(), "-"));

        // Cores Count
        final TextView cpuCountView =
            this.layout.findViewById(R.id.tile_status_router_router_cpu_cores);
        cpuCountView.setText(data.getProperty(NVRAMInfo.Companion.getCPU_CORES_COUNT(), "-"));

        // Load Avg
        final TextView loadAvgView =
            this.layout.findViewById(R.id.tile_status_router_router_cpu_load_avg);
        loadAvgView.setText(data.getProperty(NVRAMInfo.Companion.getLOAD_AVERAGE(), "-"));

        // Load Avg Usage
        final ProgressBar pb = layout.findViewById(R.id.tile_status_router_router_cpu_load_usage);
        final TextView pbText =
            layout.findViewById(R.id.tile_status_router_router_cpu_load_avg_usage_text);
        try {
          final int propertyUtilization =
              Integer.parseInt(data.getProperty(NVRAMInfo.Companion.getCPU_USED_PERCENT()));
          if (propertyUtilization >= 0) {
            pb.setProgress(propertyUtilization);
            pbText.setText(propertyUtilization + "%");
            pb.setVisibility(View.VISIBLE);
            pbText.setVisibility(View.VISIBLE);
          } else {
            pb.setVisibility(View.GONE);
            pbText.setVisibility(View.GONE);
          }
        } catch (NumberFormatException e) {
          ReportingUtils.reportException(mParentFragmentActivity, e);
          pb.setVisibility(View.GONE);
          pbText.setVisibility(View.GONE);
        }

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
                      + StatusRouterCPUTile.class
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

          cpuInfoContents = null;

          final NVRAMInfo nvramInfo = new NVRAMInfo();

          NVRAMInfo nvramInfoTmp = null;

          try {
            updateProgressBarViewSeparator(10);
            if (isDemoRouter(mRouter)) {
              nvramInfoTmp =
                  new NVRAMInfo().setProperty(NVRAMInfo.Companion.getCPU_CLOCK_FREQ(), "100");
            } else {
              nvramInfoTmp =
                  SSHUtils.getNVRamInfoFromRouter(
                      mParentFragmentActivity,
                      mRouter,
                      mGlobalPreferences,
                      NVRAMInfo.Companion.getCPU_CLOCK_FREQ());
            }
            updateProgressBarViewSeparator(45);
          } finally {
            if (nvramInfoTmp != null) {
              nvramInfo.putAll(nvramInfoTmp);
            }

            List<String> strings =
                Splitter.on(",")
                    .omitEmptyStrings()
                    .trimResults()
                    .splitToList(
                        nullToEmpty(
                            nvramInfo.getProperty(NVRAMInfo.Companion.getCPU_CLOCK_FREQ())));
            FirebaseCrashlytics.getInstance().log("strings for cpu clock: " + strings);
            if (strings != null && strings.size() > 0) {
              nvramInfo.setProperty(NVRAMInfo.Companion.getCPU_CLOCK_FREQ(), strings.get(0));
            }

            final String[] otherCmds;
            if (isDemoRouter(mRouter)) {
              otherCmds = new String[3];
              otherCmds[0] = " 0.14, 0.24, 0.28";
              otherCmds[1] = "BCM3302 V0.8";
              otherCmds[2] = "1";
            } else {
              otherCmds =
                  SSHUtils.getManualProperty(
                      mParentFragmentActivity,
                      mRouter,
                      mGlobalPreferences,
                      "uptime | awk -F'average:' '{ print $2}'",
                      GREP_MODEL_PROC_CPUINFO + "| tr '\\n' '#'; echo",
                      GREP_MODEL_PROC_CPUINFO + "| wc -l");
            }

            updateProgressBarViewSeparator(65);

            if (otherCmds != null) {

              if (otherCmds.length >= 1) {
                // Load Avg
                nvramInfo.setProperty(
                    NVRAMInfo.Companion.getLOAD_AVERAGE(),
                    Strings.nullToEmpty(otherCmds[0]).trim());
              }
              if (otherCmds.length >= 2) {
                // Model
                final String modelNameLine = otherCmds[1];
                if (modelNameLine != null) {
                  final Set<String> list =
                      new HashSet<>(
                          Splitter.on("#")
                              .trimResults()
                              .omitEmptyStrings()
                              .splitToList(modelNameLine));
                  if (!list.isEmpty()) {
                    nvramInfo.setProperty(
                        NVRAMInfo.Companion.getCPU_MODEL(), list.iterator().next());
                  }
                }
              }
              if (otherCmds.length >= 3) {
                // Nb Cores
                final String nbCoresStr = otherCmds[2];
                nvramInfo.setProperty(NVRAMInfo.Companion.getCPU_CORES_COUNT(), nbCoresStr);

                // Compute usage as well
                final String loadAvg = otherCmds[0];
                if (loadAvg != null) {
                  final List<String> stringList =
                      Splitter.on(",").omitEmptyStrings().trimResults().splitToList(loadAvg);
                  if (stringList.size() >= 3) {
                    try {
                      final float loadAvgTotal =
                          Float.parseFloat(stringList.get(0))
                              + Float.parseFloat(stringList.get(1))
                              + Float.parseFloat(stringList.get(2));
                      final int coresCount = Integer.parseInt(nbCoresStr);

                      if (coresCount > 0) {
                        nvramInfo.setProperty(
                            NVRAMInfo.Companion.getCPU_USED_PERCENT(),
                            Integer.toString(
                                Math.min(
                                    100,
                                    Double.valueOf(loadAvgTotal / coresCount * 33.3).intValue())));
                      }
                    } catch (final NumberFormatException e) {
                      ReportingUtils.reportException(mParentFragmentActivity, e);
                    }
                  }
                }
              }
            }

            // Now cache whole /proc/cpuinfo, for detailed activity
            if (isDemoRouter(mRouter)) {
              cpuInfoContents = new String[3];
              cpuInfoContents[0] = "system type:\tBroadcom BCM5352 chip rev 0\n";
              cpuInfoContents[1] = "Lorem Ipsum\n";
              cpuInfoContents[2] = "Dolor sit amet...\n";
            } else {
              cpuInfoContents =
                  SSHUtils.getManualProperty(
                      mParentFragmentActivity, mRouter, mGlobalPreferences, "cat /proc/cpuinfo");
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

  @Nullable
  @Override
  protected String getLogTag() {
    return LOG_TAG;
  }

  @Nullable
  @Override
  protected OnClickIntent getOnclickIntent() {
    if (cpuInfoContents == null) {
      // Loading
      Utils.displayMessage(
          mParentFragmentActivity,
          "Loading data from router - please wait a few seconds.",
          Style.ALERT);
      return null;
    }

    if (cpuInfoContents.length == 0) {
      // No data!
      Utils.displayMessage(
          mParentFragmentActivity, "No data available - please retry later.", Style.ALERT);
      return null;
    }

    final String mRouterUuid = mRouter.getUuid();
    final Intent cpuInfoIntent =
        new Intent(mParentFragment.getActivity(), RouterCpuInfoActivity.class);
    cpuInfoIntent.putExtra(RouterCpuInfoActivity.CPU_INFO_OUTPUT, cpuInfoContents);
    cpuInfoIntent.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouterUuid);

    return new OnClickIntent("Loading CPU Info...", cpuInfoIntent, null);
  }
}
