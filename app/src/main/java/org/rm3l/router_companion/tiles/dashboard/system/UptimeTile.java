package org.rm3l.router_companion.tiles.dashboard.system;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Throwables;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.firmwares.RemoteDataRetrievalListener;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.DDWRTTile;

public class UptimeTile extends DDWRTTile<NVRAMInfo> {

    public static final String N_A = "-";

    private static final String LOG_TAG = UptimeTile.class.getSimpleName();

    private long mLastSync;

    public UptimeTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments,
            @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_overview_uptime, null);
    }

    @Override
    public int getTileHeaderViewId() {
        return 0;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_overview_uptime_title;
    }

    @Override
    public void onLoadFinished(Loader<NVRAMInfo> loader, NVRAMInfo data) {

        try {
            //Set tiles
            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

            layout.findViewById(R.id.tile_overview_uptime_loading_view).setVisibility(View.GONE);
            layout.findViewById(R.id.tile_overview_uptime_gridLayout).setVisibility(View.VISIBLE);

            if (data == null) {
                data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
            }

            final TextView errorPlaceHolderView =
                    (TextView) this.layout.findViewById(R.id.tile_overview_uptime_error);

            final Exception exception = data.getException();

            //            final RelativeLayout mapContainerView =
            //                    (RelativeLayout) layout.findViewById(R.id.tile_overview_uptime_container);

            if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

                if (exception == null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }

                //Days
                String property = data.getProperty(NVRAMInfo.Companion.getUPTIME_DAYS());
                try {
                    final int value = Integer.parseInt(property);
                    ((TextView) layout.findViewById(R.id.tile_overview_uptime_days_title)).setText(
                            "day" + (value >= 2 ? "s" : ""));
                    property = String.valueOf(value);
                } catch (final NumberFormatException e) {
                    e.printStackTrace();
                    property = N_A;
                    ((TextView) layout.findViewById(R.id.tile_overview_uptime_days_title)).setText("day(s)");
                }
                ((TextView) layout.findViewById(R.id.tile_overview_uptime_days)).setText(property);

                //Hours
                property = data.getProperty(NVRAMInfo.Companion.getUPTIME_HOURS());
                try {
                    final int value = Integer.parseInt(property);
                    ((TextView) layout.findViewById(R.id.tile_overview_uptime_hours_title)).setText(
                            "hour" + (value >= 2 ? "s" : ""));
                    property = String.valueOf(value);
                } catch (final NumberFormatException e) {
                    e.printStackTrace();
                    property = N_A;
                    ((TextView) layout.findViewById(R.id.tile_overview_uptime_hours_title)).setText(
                            "hour(s)");
                }
                ((TextView) layout.findViewById(R.id.tile_overview_uptime_hours)).setText(property);

                //Minutes
                property = data.getProperty(NVRAMInfo.Companion.getUPTIME_MINUTES());
                try {
                    final int value = Integer.parseInt(property);
                    ((TextView) layout.findViewById(R.id.tile_overview_uptime_minutes_title)).setText(
                            "minute" + (value >= 2 ? "s" : ""));
                    property = String.valueOf(value);
                } catch (final NumberFormatException e) {
                    e.printStackTrace();
                    property = N_A;
                    ((TextView) layout.findViewById(R.id.tile_overview_uptime_minutes_title)).setText(
                            "minute(s)");
                }
                ((TextView) layout.findViewById(R.id.tile_overview_uptime_minutes)).setText(property);

                //Update last sync
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
                errorPlaceHolderView.setOnClickListener(new View.OnClickListener() {
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
            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished(): done loading!");
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
                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Init background loader for "
                            + UptimeTile.class
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

                    return mRouterConnector.getDataFor(mParentFragmentActivity, mRouter, UptimeTile.class,
                            new RemoteDataRetrievalListener() {
                                @Override
                                public void doRegardlessOfStatus() {

                                }

                                @Override
                                public void onProgressUpdate(int progress) {
                                    updateProgressBarViewSeparator(progress);
                                }
                            });
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
