package org.rm3l.ddwrt.tiles.overview;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Throwables;

import org.apache.commons.io.FileUtils;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.status.wan.WANTrafficTile;
import org.rm3l.ddwrt.utils.ColorUtils;

/**
 * Created by rm3l on 08/09/15.
 */
public class WANTotalTrafficOverviewTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = WANTotalTrafficOverviewTile.class.getSimpleName();
    private boolean isThemeLight;

    public WANTotalTrafficOverviewTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_overview_wan_total_traffic,
                R.id.tile_overview_wan_total_traffic_togglebutton);
        isThemeLight = ColorUtils.isThemeLight(mParentFragmentActivity);
    }

    @Override
    public int getTileHeaderViewId() {
        return 0;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_overview_wan_total_traffic_title;
    }

    @Nullable
    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {

                try {

                    isThemeLight = ColorUtils.isThemeLight(mParentFragmentActivity);

                    Log.d(LOG_TAG, "Init background loader for " + WANTotalTrafficOverviewTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                        //Skip run
                        Log.d(LOG_TAG, "Skip loader run");
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    return WANTrafficTile
                            .getWANTotalTrafficNvramInfo(mParentFragmentActivity, mRouter, mGlobalPreferences);
                    
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

    @Override
    public void onLoadFinished(Loader<NVRAMInfo> loader, NVRAMInfo data) {
        try {
            //Set tiles
            Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

            layout.findViewById(R.id.tile_overview_wan_total_traffic_loading_view)
                    .setVisibility(View.GONE);
            layout.findViewById(R.id.tile_overview_wan_total_traffic_gridLayout)
                    .setVisibility(View.VISIBLE);

            if (data == null) {
                data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
            }

            final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_overview_wan_total_traffic_error);

            final Exception exception = data.getException();

            final RelativeLayout mapContainerView =
                    (RelativeLayout) layout.findViewById(R.id.tile_overview_wan_total_traffic_container);

            if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

                if (exception == null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }

                final String wanIface = data
                        .getProperty(NVRAMInfo.WAN_IFACE);

                final int dlDrawable;
                final int ulDrawable;
                if (isThemeLight) {
                    dlDrawable = R.drawable.ic_dl_dark;
                    ulDrawable = R.drawable.ic_ul_dark;
                } else {
                    dlDrawable = R.drawable.ic_dl_white;
                    ulDrawable = R.drawable.ic_ul_light;
                }

                final TextView wanDLView = (TextView) this.layout.findViewById(R.id.tile_overview_wan_total_traffic_dl);
                wanDLView.setCompoundDrawablesWithIntrinsicBounds(dlDrawable, 0, 0, 0);

                final TextView wanULView = (TextView) this.layout.findViewById(R.id.tile_overview_wan_total_traffic_ul);
                wanULView.setCompoundDrawablesWithIntrinsicBounds(ulDrawable, 0, 0, 0);

                String dlText;
                final String wanRcvBytes = data.getProperty(wanIface + "_rcv_bytes", "-1");
                try {
                    final long size = Long.parseLong(wanRcvBytes);
                    if (size >= 0l) {
                        dlText = FileUtils.byteCountToDisplaySize(size);
                    } else {
                        dlText = "-";
                    }
                } catch (@NonNull final NumberFormatException nfe) {
                    dlText = "-";
                }
                wanDLView.setText(dlText);

                String ulText;
                final String wanXmitBytes = data.getProperty(wanIface + "_xmit_bytes", "-1");
                try {
                    final long size = Long.parseLong(wanXmitBytes);
                    if (size >= 0l) {
                        ulText = FileUtils.byteCountToDisplaySize(size);
                    } else {
                        ulText = "-";
                    }
                } catch (@NonNull final NumberFormatException nfe) {
                    ulText = "-";
                }
                wanULView.setText(ulText);
            }

            if (exception != null && !(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {
                //noinspection ThrowableResultOfMethodCallIgnored
                final Throwable rootCause = Throwables.getRootCause(exception);
                errorPlaceHolderView.setText("Error: " + (rootCause != null ? rootCause.getMessage() : "null"));
                final Context parentContext = this.mParentFragmentActivity;
                errorPlaceHolderView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        //noinspection ThrowableResultOfMethodCallIgnored
                        if (rootCause != null) {
                            Toast.makeText(parentContext,
                                    rootCause.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
                errorPlaceHolderView.setVisibility(View.VISIBLE);
                //Hide NTM
                mapContainerView.setVisibility(View.GONE);
            } else {
                mapContainerView.setVisibility(View.VISIBLE);
            }

        }  finally {
            Log.d(LOG_TAG, "onLoadFinished(): done loading!");
            doneWithLoaderInstance(this, loader,
                    R.id.tile_overview_wan_total_traffic_togglebutton_title,
                    R.id.tile_overview_wan_total_traffic_togglebutton_separator);
        }
    }
}
