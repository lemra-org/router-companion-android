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

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

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
import org.rm3l.ddwrt.api.None;
import org.rm3l.ddwrt.api.RouterData;
import org.rm3l.ddwrt.api.conn.Router;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.tiles.DDWRTTile;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Random;

/**
 *
 */
public class BandwidthMonitoringTile extends DDWRTTile<None> {

    public static final int MAX_DATA_POINTS = 50;
    private static final String LOG_TAG = BandwidthMonitoringTile.class.getSimpleName();
    private final Random randomColorGen = new Random();
    private final Map<String, Integer> colorsCache = Maps.newHashMap();
    private final BandwidthMonitoringIfaceData bandwidthMonitoringIfaceData = new BandwidthMonitoringIfaceData();

    public BandwidthMonitoringTile(@NotNull SherlockFragmentActivity parentFragmentActivity, @NotNull Bundle arguments, Router router) {
        super(parentFragmentActivity, arguments, router, R.layout.tile_status_bandwidth_monitoring_iface, R.id.tile_status_bandwidth_monitoring_togglebutton);
    }

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

                    //Get ifaces and fetch data points for each of these ifaces
                    fillIfacesDataPoints(getIfaces());

                    //TODO
                    return null;

                } catch (@NotNull final Exception e) {
                    e.printStackTrace();
                    return (None) new None().setException(e);
                }
            }
        };
    }

    @NotNull
    private Collection<String> getIfaces() throws Exception {

        //TODO TEST
        return Sets.newTreeSet(Arrays.asList("etho", "eth1", "eth4"));

        //TODO TEST

//        final Set<String> ifacesConsidered = Sets.newHashSet();
//
//        final NVRAMInfo nvramInfo = SSHUtils.getNVRamInfoFromRouter(mRouter,
//                NVRAMInfo.LAN_IFNAME,
//                NVRAMInfo.WAN_IFNAME,
//                NVRAMInfo.LANDEVS);
//
//        if (nvramInfo == null) {
//            return ifacesConsidered;
//        }
//
//        final String lanIfname = nvramInfo.getProperty(NVRAMInfo.LAN_IFNAME);
//        if (lanIfname != null) {
//            ifacesConsidered.add(lanIfname);
//        }
//
//        final String wanIfname = nvramInfo.getProperty(NVRAMInfo.WAN_IFNAME);
//        if (wanIfname != null) {
//            ifacesConsidered.add(wanIfname);
//        }
//
//        final String landevs = nvramInfo.getProperty(NVRAMInfo.LANDEVS);
//        if (landevs != null) {
//            final List<String> splitToList = Splitter.on(" ").omitEmptyStrings().trimResults().splitToList(landevs);
//            if (splitToList != null && !splitToList.isEmpty()) {
//                for (final String landev : splitToList) {
//                    if (landev == null) {
//                        continue;
//                    }
//                    ifacesConsidered.add(landev);
//                }
//            }
//        }
//
//        return ifacesConsidered;

    }

    private void fillIfacesDataPoints(final Collection<String> ifaces) {

        for (String iface : ifaces) {
            fillIfaceDataPoint(iface);
        }
    }

    public void fillIfaceDataPoint(@NotNull final String iface) {

        //TODO TEST ONLY
        bandwidthMonitoringIfaceData.addData(iface,
                new DataPoint(System.currentTimeMillis(), this.getNextTestPoint()));
        //TODO TEST ONLY
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    //TODO TEST
    private double getNextTestPoint() {
        final double random = new Random().nextDouble() * 1024;
        return random + Math.sin(random * Math.E);
    }

    @Override
    public void onLoadFinished(@NotNull Loader<None> loader, @Nullable None data) {
        //Set tiles
        Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

        if (data == null) {
            //FIXME Just commented out for tests
            data = new None();
//            data = (None) new None().setException(new DDWRTNoDataException("No Data!"));
            //END FIXME
        }

        @NotNull final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_error);

        @Nullable final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

            if (exception == null) {
                errorPlaceHolderView.setVisibility(View.GONE);
            }

            @NotNull final LinearLayout graphPlaceHolder = (LinearLayout) this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_graph_placeholder);
            final Map<String, EvictingQueue<DataPoint>> dataCircularBuffer = bandwidthMonitoringIfaceData.getData();

            long maxX = System.currentTimeMillis() + 5000;
            long minX = System.currentTimeMillis() - 5000;
            double maxY = 10;
            double minY = 1.;

            final XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
            final XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();

            for (final Map.Entry<String, EvictingQueue<DataPoint>> entry : dataCircularBuffer.entrySet()) {
                final String iface = entry.getKey();
                final EvictingQueue<DataPoint> dataPoints = entry.getValue();
                final XYSeries series = new XYSeries(iface);
                for (final DataPoint point : dataPoints) {
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

        if (exception != null) {
            errorPlaceHolderView.setText("Error: " + Throwables.getRootCause(exception).getMessage());
            errorPlaceHolderView.setVisibility(View.VISIBLE);
        }

        doneWithLoaderInstance(this, loader,
                R.id.tile_status_bandwidth_monitoring_togglebutton_title, R.id.tile_status_bandwidth_monitoring_togglebutton_separator);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");
    }

    @Nullable
    @Override
    protected Intent getOnclickIntent() {
        //TODO
        return null;
    }

    class BandwidthMonitoringIfaceData extends RouterData<Map<String, EvictingQueue<DataPoint>>> {

        public BandwidthMonitoringIfaceData() {
            super();
            super.setData(Maps.<String, EvictingQueue<DataPoint>>newConcurrentMap());
        }

        public BandwidthMonitoringIfaceData addData(final String iface, final DataPoint point) {
            final Map<String, EvictingQueue<DataPoint>> data = super.getData();
            final EvictingQueue<DataPoint> dataPointsForIface = data.get(iface);
            if (dataPointsForIface == null) {
                data.put(iface, EvictingQueue.<DataPoint>create(MAX_DATA_POINTS));
            }
            data.get(iface).add(point);
            return this;
        }

    }

    private class DataPoint {
        private final long timestamp;
        private final double value;

        private DataPoint(long timestamp, double value) {
            this.timestamp = timestamp;
            this.value = value;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public double getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DataPoint dataPoint = (DataPoint) o;

            return Objects.equal(dataPoint.timestamp, timestamp)
                    && Objects.equal(dataPoint.value, value);
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            result = (int) (timestamp ^ (timestamp >>> 32));
            temp = Double.doubleToLongBits(value);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return "DataPoint{" +
                    "timestamp=" + timestamp +
                    ", value=" + value +
                    '}';
        }
    }
}
