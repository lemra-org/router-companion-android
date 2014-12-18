/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Armel S.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.rm3l.ddwrt.tiles.status.bandwidth;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Maps;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.resources.None;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.utils.SSHUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 *
 */
public class BandwidthWANMonitoringTile extends DDWRTTile<None> {

    private static final String LOG_TAG = BandwidthWANMonitoringTile.class.getSimpleName();

    private final Random randomColorGen = new Random();
    private final Map<String, Integer> colorsCache = Maps.newHashMap();
    @NotNull
    private final BandwidthMonitoringTile.BandwidthMonitoringIfaceData bandwidthMonitoringIfaceData =
            new BandwidthMonitoringTile.BandwidthMonitoringIfaceData();

    private String wanIface;

    public BandwidthWANMonitoringTile(@NotNull SherlockFragment parentFragment, @NotNull Bundle arguments, Router router) {
        super(parentFragment, arguments, router, R.layout.tile_status_bandwidth_monitoring_iface, R.id.tile_status_bandwidth_monitoring_togglebutton);
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

                    //Start by getting information about the WAN iface name
                    @Nullable final NVRAMInfo nvRamInfoFromRouter = SSHUtils.getNVRamInfoFromRouter(mRouter, NVRAMInfo.WAN_DEFAULT);
                    if (nvRamInfoFromRouter == null) {
                        throw new IllegalStateException("Whoops. WAN Iface could not be determined.");
                    }

                    wanIface = nvRamInfoFromRouter
                            .getProperty(NVRAMInfo.WAN_DEFAULT);

                    if (Strings.isNullOrEmpty(wanIface)) {
                        throw new IllegalStateException("Whoops. WAN Iface could not be determined.");
                    }

                    @Nullable final String[] netDevWanIfaces = SSHUtils.getManualProperty(mRouter, "cat /proc/net/dev | grep \"" + wanIface + "\"");
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
                    } catch (@NotNull final NumberFormatException nfe) {
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

                    } catch (@NotNull final NumberFormatException nfe) {
                        return null;
                    }

                    nvRamInfoFromRouter.setProperty(wanIface + "_egress_MB", Double.toString(wanXmitMBytes));

                    return new None();

                } catch (@NotNull final Exception e) {
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
    protected Intent getOnclickIntent() {
        //TODO
        return null;
    }

    @Override
    public void onLoadFinished(Loader<None> loader, None data) {
        Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

        //noinspection ConstantConditions
        if (data == null || bandwidthMonitoringIfaceData.getData().isEmpty()) {
            data = (None) new None().setException(new DDWRTNoDataException("No Data!"));
        }

        @NotNull final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_error);

        @Nullable final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

            if (exception == null) {
                errorPlaceHolderView.setVisibility(View.GONE);
            }

            ((TextView) this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_title))
                    .setText(this.mParentFragmentActivity.getResources()
                            .getString(R.string.bandwidth_usage_mb) + ": " + wanIface);

            @NotNull final LinearLayout graphPlaceHolder = (LinearLayout) this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_graph_placeholder);
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

                Integer ifaceColor = colorsCache.get(iface);
                if (ifaceColor == null) {
                    //Generate a Random Color, excluding 'white' (because graph background is already white)
                    ifaceColor = Color.argb(255,
                            randomColorGen.nextInt(255), randomColorGen.nextInt(255), randomColorGen.nextInt(255));
                    colorsCache.put(iface, ifaceColor);
                }
                renderer.setColor(ifaceColor);
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
            mRenderer.setPanEnabled(false, false);
            mRenderer.setYAxisMax(maxY + 10);
            mRenderer.setYAxisMin(minY);
            mRenderer.setXAxisMin(minX);
            mRenderer.setXAxisMax(maxX + 10);
            mRenderer.setShowGrid(false);
            mRenderer.setClickEnabled(false);
            mRenderer.setZoomEnabled(true);
            mRenderer.setPanEnabled(false);
            mRenderer.setZoomRate(6.0f);
            mRenderer.setShowLabels(true);
            mRenderer.setFitLegend(true);
            mRenderer.setInScroll(true);

            final GraphicalView chartView = ChartFactory.getTimeChartView(graphPlaceHolder
                    .getContext(), dataset, mRenderer, null);
            chartView.repaint();

            graphPlaceHolder.addView(chartView, 0);
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
