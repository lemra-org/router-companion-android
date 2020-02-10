package org.rm3l.router_companion.tiles.dashboard.system;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.github.lzyzsd.circleprogress.ArcProgress;
import com.google.common.base.Throwables;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.firmwares.RemoteDataRetrievalListener;
import org.rm3l.router_companion.main.DDWRTMainActivity;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.tiles.status.router.StatusRouterMemoryTile;
import org.rm3l.router_companion.utils.ColorUtils;

/** Created by rm3l on 03/01/16. */
public class StorageUsageTile extends DDWRTTile<NVRAMInfo> {

  private static final String LOG_TAG = StorageUsageTile.class.getSimpleName();

  final int orange;

  final int red;

  private boolean isThemeLight;

  private final ArcProgress mCIFSArcProgress;

  private final ArcProgress mJFFS2ArcProgress;

  private long mLastSync;

  private final ArcProgress mNVRAMArcProgress;

  public StorageUsageTile(
      @NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
    super(parentFragment, arguments, router, R.layout.tile_dashboard_storage, null);
    isThemeLight = ColorUtils.Companion.isThemeLight(mParentFragmentActivity);

    red = ContextCompat.getColor(mParentFragmentActivity, R.color.win8_red);
    orange = ContextCompat.getColor(mParentFragmentActivity, R.color.win8_orange);

    final View.OnClickListener onClickListener =
        v -> {
          // Open Router State tab
          if (mParentFragmentActivity instanceof DDWRTMainActivity) {
            ((DDWRTMainActivity) mParentFragmentActivity).selectItemInDrawer(2);
          } else {
            // TODO Set proper flags ???
            final Intent intent = new Intent(mParentFragmentActivity, DDWRTMainActivity.class);
            intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouter.getUuid());
            intent.putExtra(DDWRTMainActivity.SAVE_ITEM_SELECTED, 2);
            mParentFragmentActivity.startActivity(intent);
          }
        };

    this.mNVRAMArcProgress =
        (ArcProgress) layout.findViewById(R.id.tile_dashboard_storage_nvram_arcprogress);
    this.mJFFS2ArcProgress =
        (ArcProgress) layout.findViewById(R.id.tile_dashboard_storage_jffs2_arcprogress);
    this.mCIFSArcProgress =
        (ArcProgress) layout.findViewById(R.id.tile_dashboard_storage_cifs_arcprogress);

    this.mNVRAMArcProgress.setOnClickListener(onClickListener);
    this.mJFFS2ArcProgress.setOnClickListener(onClickListener);
    this.mCIFSArcProgress.setOnClickListener(onClickListener);
  }

  @Override
  public Integer getTileHeaderViewId() {
    return -1;
  }

  @Override
  public Integer getTileTitleViewId() {
    return R.id.tile_dashboard_storage_title;
  }

  @Override
  public void onLoadFinished(Loader<NVRAMInfo> loader, NVRAMInfo data) {
    try {
      // Set tiles
      FirebaseCrashlytics.getInstance().log("onLoadFinished: loader=" + loader + " / data=" + data);

      layout.findViewById(R.id.tile_dashboard_storage_loading_view).setVisibility(View.GONE);
      mNVRAMArcProgress.setVisibility(View.VISIBLE);
      mJFFS2ArcProgress.setVisibility(View.VISIBLE);
      mCIFSArcProgress.setVisibility(View.VISIBLE);

      if (data == null) {
        data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
      }

      Exception exception = data.getException();

      final TextView errorPlaceHolderView =
          (TextView) this.layout.findViewById(R.id.tile_dashboard_storage_error);

      if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

        if (exception == null) {
          errorPlaceHolderView.setVisibility(View.GONE);
        }

        Integer arcProgressFinishedColor = null;
        if (mRouter != null) {
          final Integer primaryColor =
              ColorUtils.Companion.getPrimaryColor(mRouter.getRouterFirmware());
          if (primaryColor != null) {
            arcProgressFinishedColor =
                ContextCompat.getColor(mParentFragmentActivity, primaryColor);
          }
        }
        if (arcProgressFinishedColor != null) {
          mNVRAMArcProgress.setFinishedStrokeColor(arcProgressFinishedColor);
          mCIFSArcProgress.setUnfinishedStrokeColor(arcProgressFinishedColor);
          mJFFS2ArcProgress.setFinishedStrokeColor(arcProgressFinishedColor);
        }
        final int textColor =
            ContextCompat.getColor(
                mParentFragmentActivity, isThemeLight ? R.color.black : R.color.white);
        mNVRAMArcProgress.setTextColor(textColor);
        mCIFSArcProgress.setTextColor(textColor);
        mJFFS2ArcProgress.setTextColor(textColor);

        try {
          final String nvramUsedStr = data.getProperty(NVRAMInfo.Companion.getNVRAM_USED_PERCENT());
          final int nvramUsed = Integer.parseInt(nvramUsedStr);
          FirebaseCrashlytics.getInstance().log("nvramUsedStr=" + nvramUsedStr);

          // Update colors as per the usage
          // TODO Make these thresholds user-configurable (and perhaps display notifications if
          // needed - cf. g service task)
          if (nvramUsed >= 95) {
            // Red
            mNVRAMArcProgress.setFinishedStrokeColor(red);
          } else if (nvramUsed >= 80) {
            // Orange
            mNVRAMArcProgress.setFinishedStrokeColor(orange);
          }
          mNVRAMArcProgress.setProgress(nvramUsed);
        } catch (final NumberFormatException e) {
          mNVRAMArcProgress.setVisibility(View.GONE);
        }

        try {
          final String jffs2UsedStr =
              data.getProperty(NVRAMInfo.Companion.getSTORAGE_JFFS2_USED_PERCENT());
          final int jffs2Used = Integer.parseInt(jffs2UsedStr);

          // Update colors as per the usage
          // TODO Make these thresholds user-configurable (and perhaps display notifications if
          // needed - cf. g service task)
          if (jffs2Used >= 95) {
            // Red
            mJFFS2ArcProgress.setFinishedStrokeColor(red);
          } else if (jffs2Used >= 80) {
            // Orange
            mJFFS2ArcProgress.setFinishedStrokeColor(orange);
          }
          mJFFS2ArcProgress.setProgress(jffs2Used);
        } catch (final NumberFormatException e) {
          mJFFS2ArcProgress.setVisibility(View.GONE);
        }

        try {
          final String cifsUsedStr =
              data.getProperty(NVRAMInfo.Companion.getSTORAGE_CIFS_USED_PERCENT());
          final int cifsUsed = Integer.parseInt(cifsUsedStr);

          // Update colors as per the usage
          // TODO Make these thresholds user-configurable (and perhaps display notifications if
          // needed - cf. g service task)
          if (cifsUsed >= 95) {
            // Red
            mCIFSArcProgress.setFinishedStrokeColor(red);
          } else if (cifsUsed >= 80) {
            // Orange
            mCIFSArcProgress.setFinishedStrokeColor(orange);
          }
          mCIFSArcProgress.setProgress(cifsUsed);
        } catch (final NumberFormatException e) {
          mCIFSArcProgress.setVisibility(View.GONE);
        }

        // Update last sync
        final RelativeTimeTextView lastSyncView =
            (RelativeTimeTextView) layout.findViewById(R.id.tile_last_sync);
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
    } finally {
      FirebaseCrashlytics.getInstance().log("onLoadFinished(): done loading!");
      mRefreshing.set(false);
      doneWithLoaderInstance(this, loader);
    }
  }

  @Nullable
  @Override
  protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
    return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

      @Nullable
      @Override
      public NVRAMInfo loadInBackground() {

        try {
          FirebaseCrashlytics.getInstance()
              .log(
                  "Init background loader for "
                      + StatusRouterMemoryTile.class
                      + ": routerInfo="
                      + mRouter
                      + " / nbRunsLoader="
                      + nbRunsLoader);

          isThemeLight = ColorUtils.Companion.isThemeLight(mParentFragmentActivity);

          if (mRefreshing.getAndSet(true)) {
            return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
          }
          nbRunsLoader++;

          updateProgressBarViewSeparator(0);

          mLastSync = System.currentTimeMillis();

          updateProgressBarViewSeparator(10);

          final NVRAMInfo nvramInfo =
              mRouterConnector.getDataFor(
                  mParentFragmentActivity,
                  mRouter,
                  StorageUsageTile.class,
                  new RemoteDataRetrievalListener() {
                    @Override
                    public void doRegardlessOfStatus() {}

                    @Override
                    public void onProgressUpdate(int progress) {
                      updateProgressBarViewSeparator(progress);
                    }
                  });

          updateProgressBarViewSeparator(90);

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
    return null;
  }
}
