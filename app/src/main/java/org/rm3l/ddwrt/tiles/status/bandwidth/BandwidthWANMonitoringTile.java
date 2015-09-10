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
package org.rm3l.ddwrt.tiles.status.bandwidth;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.EvictingQueue;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.resources.None;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.SSHUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;

/**
 *
 */
public class BandwidthWANMonitoringTile extends DDWRTTile<None> {

    private static final String LOG_TAG = BandwidthWANMonitoringTile.class.getSimpleName();

    @NonNull
    private final BandwidthMonitoringTile.BandwidthMonitoringIfaceData bandwidthMonitoringIfaceData =
            new BandwidthMonitoringTile.BandwidthMonitoringIfaceData();

    private String wanIface;
    private long mLastSync;

    public BandwidthWANMonitoringTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, Router router) {
        super(parentFragment, arguments, router, R.layout.tile_status_bandwidth_monitoring_iface, R.id.tile_status_bandwidth_monitoring_togglebutton);
    }

    @Override
    public int getTileHeaderViewId() {
        return R.id.tile_status_bandwidth_monitoring_hdr;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_status_bandwidth_monitoring_title;
    }

    @Nullable
    @Override
    protected Loader<None> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<None>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public None loadInBackground() {

                try {
                    Log.d(LOG_TAG, "Init background loader for " + BandwidthMonitoringTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                        //Skip run
                        Log.d(LOG_TAG, "Skip loader run");
                        return (None) new None().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    mLastSync = System.currentTimeMillis();

                    //Start by getting information about the WAN iface name
                    final NVRAMInfo nvRamInfoFromRouter = SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity, mRouter, mGlobalPreferences, NVRAMInfo.WAN_IFACE);
                    if (nvRamInfoFromRouter == null) {
                        throw new IllegalStateException("Whoops. WAN Iface could not be determined.");
                    }

                    wanIface = nvRamInfoFromRouter
                            .getProperty(NVRAMInfo.WAN_IFACE);

                    if (Strings.isNullOrEmpty(wanIface)) {
                        throw new IllegalStateException("Whoops. WAN Iface could not be determined.");
                    }

                    final String[] netDevWanIfaces = SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences, "cat /proc/net/dev | grep \"" + wanIface + "\"");
                    if (netDevWanIfaces == null || netDevWanIfaces.length == 0) {
                        return null;
                    }

                    String netDevWanIface = netDevWanIfaces[0];
                    if (netDevWanIface == null) {
                        return null;
                    }

                    netDevWanIface = netDevWanIface.replaceAll(wanIface + ":", "");

                    final List<String> netDevWanIfaceList = Splitter.on(" ").omitEmptyStrings().trimResults().splitToList(netDevWanIface);
                    if (netDevWanIfaceList == null || netDevWanIfaceList.size() <= 15) {
                        return null;
                    }

                    final long timestamp = System.currentTimeMillis();

                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_bytes", netDevWanIfaceList.get(0));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_packets", netDevWanIfaceList.get(1));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_errs", netDevWanIfaceList.get(2));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_drop", netDevWanIfaceList.get(3));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_fifo", netDevWanIfaceList.get(4));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_frame", netDevWanIfaceList.get(5));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_compressed", netDevWanIfaceList.get(6));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_multicast", netDevWanIfaceList.get(7));

                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_bytes", netDevWanIfaceList.get(8));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_packets", netDevWanIfaceList.get(9));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_errs", netDevWanIfaceList.get(10));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_drop", netDevWanIfaceList.get(11));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_fifo", netDevWanIfaceList.get(12));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_colls", netDevWanIfaceList.get(13));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_carrier", netDevWanIfaceList.get(14));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_compressed", netDevWanIfaceList.get(15));

                    //Ingress
                    double wanRcvMBytes;
                    final String wanRcvBytes = nvRamInfoFromRouter.getProperty(wanIface + "_rcv_bytes", "-1");
                    try {
                        wanRcvMBytes = Double.parseDouble(wanRcvBytes) / (1024 * 1024);
                        if (wanRcvMBytes >= 0.) {
                            wanRcvMBytes = new BigDecimal(wanRcvMBytes).setScale(2, RoundingMode.HALF_UP).doubleValue();
                            bandwidthMonitoringIfaceData.addData("IN",
                                    new BandwidthMonitoringTile.DataPoint(timestamp, wanRcvMBytes));
                        }
                    } catch (@NonNull final NumberFormatException nfe) {
                        return null;
                    }
                    nvRamInfoFromRouter.setProperty(wanIface + "_ingress_MB", Double.toString(wanRcvMBytes));

                    //Egress
                    double wanXmitMBytes;
                    final String wanXmitBytes = nvRamInfoFromRouter.getProperty(wanIface + "_xmit_bytes", "-1");
                    try {
                        wanXmitMBytes = Double.parseDouble(wanXmitBytes) / (1024 * 1024);
                        if (wanXmitMBytes >= 0.) {
                            wanXmitMBytes = new BigDecimal(wanXmitMBytes).setScale(2, RoundingMode.HALF_UP).doubleValue();
                            bandwidthMonitoringIfaceData.addData("OUT",
                                    new BandwidthMonitoringTile.DataPoint(timestamp, wanXmitMBytes));
                        }

                    } catch (@NonNull final NumberFormatException nfe) {
                        return null;
                    }

                    nvRamInfoFromRouter.setProperty(wanIface + "_egress_MB", Double.toString(wanXmitMBytes));

                    return new None();

                } catch (@NonNull final Exception e) {
                    e.printStackTrace();
                    return (None) new None().setException(e);
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
        //TODO
        return null;
    }

    @Override
    public void onLoadFinished(Loader<None> loader, None data) {
        Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

        layout.findViewById(R.id.tile_status_bandwidth_monitoring_graph_loading_view)
                .setVisibility(View.GONE);
        layout.findViewById(R.id.tile_status_bandwidth_monitoring_graph_placeholder)
                .setVisibility(View.VISIBLE);

        //noinspection ConstantConditions
        if (data == null || bandwidthMonitoringIfaceData.getData().isEmpty()) {
            data = (None) new None().setException(new DDWRTNoDataException("No Data!"));
        }

        final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_error);

        final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

            if (exception == null) {
                errorPlaceHolderView.setVisibility(View.GONE);
            }

            ((TextView) this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_title))
                    .setText(this.mParentFragmentActivity.getResources()
                            .getString(R.string.bandwidth_usage_mb) + (!Strings.isNullOrEmpty(wanIface) ?
                            (": " + wanIface) : ""));

            final LinearLayout graphPlaceHolder = (LinearLayout) this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_graph_placeholder);
            final Map<String, EvictingQueue<BandwidthMonitoringTile.DataPoint>> dataCircularBuffer = bandwidthMonitoringIfaceData.getData();

            long maxX = System.currentTimeMillis() + 5000;
            long minX = System.currentTimeMillis() - 5000;
            double maxY = 10;
            double minY = 1.;

            final XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
            final XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();

            for (final Map.Entry<String, EvictingQueue<BandwidthMonitoringTile.DataPoint>> entry : dataCircularBuffer.entrySet()) {
                final String iface = entry.getKey();
                final EvictingQueue<BandwidthMonitoringTile.DataPoint> dataPoints = entry.getValue();
                final XYSeries series = new XYSeries(iface);
                for (final BandwidthMonitoringTile.DataPoint point : dataPoints) {
                    final long x = point.getTimestamp();
                    final double y = point.getValue();
                    series.add(x, y);
                    maxX = Math.max(maxX, x);
                    minX = Math.min(minX, x);
                    maxY = Math.max(maxY, y);
                    minY = Math.min(minY, y);
                }
                // Now we add our series
                dataset.addSeries(series);

                // Now we create the renderer
                final XYSeriesRenderer renderer = new XYSeriesRenderer();
                renderer.setLineWidth(2);

                renderer.setColor(ColorUtils.getColor(iface));
                // Include low and max value
                renderer.setDisplayBoundingPoints(true);
                // we add point markers
                renderer.setPointStyle(PointStyle.POINT);
                renderer.setPointStrokeWidth(1);

                mRenderer.addSeriesRenderer(renderer);
            }

            // We want to avoid black border
            mRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00)); // transparent margins
            // Disable Pan on two axis
//            mRenderer.setPanEnabled(false, false);
//            mRenderer.setYAxisMax(maxY + 10);
//            mRenderer.setYAxisMin(minY);
//            mRenderer.setXAxisMin(minX);
//            mRenderer.setXAxisMax(maxX + 10);
//            mRenderer.setShowGrid(false);
//            mRenderer.setClickEnabled(false);
//            mRenderer.setZoomEnabled(true);
//            mRenderer.setPanEnabled(false);
//            mRenderer.setZoomRate(6.0f);
//            mRenderer.setShowLabels(true);
//            mRenderer.setFitLegend(true);
//            mRenderer.setInScroll(true);

            mRenderer.setYLabels(0);
            mRenderer.addYTextLabel(maxY, byteCountToDisplaySize(Double.valueOf(maxY).longValue())
                    .replace("bytes", "B"));
            if (maxY != 0 && maxY / 2 >= 9000) {
                mRenderer.addYTextLabel(maxY / 2, byteCountToDisplaySize(Double.valueOf(maxY / 2).longValue())
                        .replace("bytes", "B"));
            }

            // We want to avoid black border
            //setting text size of the title
            mRenderer.setChartTitleTextSize(22);
//            //setting text size of the axis title
//            mRenderer.setAxisTitleTextSize(15);
//            //setting text size of the graph label
//            mRenderer.setLabelsTextSize(15);
            mRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00)); // transparent margins
            // Disable Pan on two axis
            mRenderer.setPanEnabled(false, false);
            mRenderer.setYAxisMax(maxY + 10);
            mRenderer.setYAxisMin(minY);
            mRenderer.setXAxisMin(minX);
            mRenderer.setXAxisMax(maxX + 10);
            mRenderer.setShowGrid(false);
            mRenderer.setClickEnabled(false);
            mRenderer.setZoomEnabled(false, false);
            mRenderer.setPanEnabled(false, false);
            mRenderer.setZoomRate(6.0f);
            mRenderer.setShowLabels(true);
            mRenderer.setFitLegend(true);
            mRenderer.setInScroll(true);
            mRenderer.setXLabelsAlign(Paint.Align.CENTER);
            mRenderer.setYLabelsAlign(Paint.Align.LEFT);
            mRenderer.setTextTypeface("sans_serif", Typeface.NORMAL);
            mRenderer.setAntialiasing(true);
            mRenderer.setExternalZoomEnabled(false);
            mRenderer.setInScroll(false);
            mRenderer.setFitLegend(true);

            final GraphicalView chartView = ChartFactory.getTimeChartView(graphPlaceHolder
                    .getContext(), dataset, mRenderer, null);
            chartView.repaint();

            graphPlaceHolder.addView(chartView, 0);

            //Update last sync
            final RelativeTimeTextView lastSyncView = (RelativeTimeTextView) layout.findViewById(R.id.tile_last_sync);
            lastSyncView.setReferenceTime(mLastSync);
            lastSyncView.setPrefix("Last sync: ");
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
        } else {
            if (bandwidthMonitoringIfaceData.getData().isEmpty()) {
                errorPlaceHolderView.setText("Error: No Data!");
                errorPlaceHolderView.setVisibility(View.VISIBLE);
            }
        }

        doneWithLoaderInstance(this, loader,
                R.id.tile_status_bandwidth_monitoring_togglebutton_title, R.id.tile_status_bandwidth_monitoring_togglebutton_separator);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");
    }


}
