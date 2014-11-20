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
import com.google.common.base.Throwables;
import com.google.common.collect.EvictingQueue;

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
import org.rm3l.ddwrt.api.conn.NVRAMInfo;
import org.rm3l.ddwrt.api.conn.Router;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.tiles.DDWRTTile;

import java.util.Random;

//import com.androidplot.Plot;
//import com.androidplot.xy.LineAndPointFormatter;
//import com.androidplot.xy.SimpleXYSeries;
//import com.androidplot.xy.XYPlot;
//import com.androidplot.xy.XYSeries;
//import com.androidplot.xy.XYStepMode;

/**
 *
 */
public class BandwidthMonitoringTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = BandwidthMonitoringTile.class.getSimpleName();

    private final String iface;
    private final EvictingQueue<DataPoint> points = EvictingQueue.create(10);

    public BandwidthMonitoringTile(@NotNull SherlockFragmentActivity parentFragmentActivity, @NotNull Bundle arguments, Router router, final String iface) {
        super(parentFragmentActivity, arguments, router, R.layout.tile_status_bandwidth_monitoring_iface, R.id.tile_status_bandwidth_monitoring_togglebutton);
        this.iface = iface;
        ((TextView) this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_title)).setText(this.iface);
    }

    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {

                final String iface = BandwidthMonitoringTile.this.iface;

                try {
                    Log.d(LOG_TAG, "Init background loader for " + BandwidthMonitoringTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                        //Skip run
                        Log.d(LOG_TAG, "Skip loader run");
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;


                    //TODO
                    return null;

                } catch (@NotNull final Exception e) {
                    e.printStackTrace();
                    return new NVRAMInfo().setException(e);
                }
            }
        };
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    private double getNextTestPoint() {
        return new Random().nextDouble() * 1024;
    }

    @Override
    public void onLoadFinished(@NotNull Loader<NVRAMInfo> loader, @Nullable NVRAMInfo data) {
        //Set tiles
        Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

        if (data == null) {
            //FIXME Just commented out for tests
            data = new NVRAMInfo();
//            data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
            //END FIXME
        }

        @NotNull final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_error);

        @Nullable final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

            if (exception == null) {
                if (errorPlaceHolderView != null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }
            }

            @NotNull final LinearLayout graphPlaceHolder = (LinearLayout) this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_graph_placeholder);

            final XYSeries series = new XYSeries("Bandwidth Usage (MB): " + this.iface);

            //Add new point to the Circular Buffer
            points.add(new DataPoint(nbRunsLoader, this.getNextTestPoint()));

            double maxX = 10;
            double minX = 1;
            double maxY = 10;
            double minY = 1;
            for (final DataPoint point : points) {
                final double x = point.getX();
                final double y = point.getY();
                series.add(x, y);
                maxX = Math.max(maxX, x);
                minX = Math.min(minX, x);
                maxY = Math.max(maxY, y);
                minY = Math.min(minY, y);
            }

            if (minX <= 0) {
                minX = 1.;
            }

            if (minY <= 0) {
                minY = 1.;
            }

            // Now we add our series
            final XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
            dataset.addSeries(series);

            // Now we create the renderer
            final XYSeriesRenderer renderer = new XYSeriesRenderer();
            renderer.setLineWidth(2);
            renderer.setColor(Color.GREEN);
            // Include low and max value
            renderer.setDisplayBoundingPoints(true);
            // we add point markers
            renderer.setPointStyle(PointStyle.CIRCLE);
            renderer.setPointStrokeWidth(3);

            final XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
            mRenderer.addSeriesRenderer(renderer);
            // We want to avoid black border
            mRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00)); // transparent margins
            // Disable Pan on two axis
            mRenderer.setPanEnabled(false, false);
            mRenderer.setYAxisMax(maxY, Double.valueOf(maxY / minY).intValue());
            mRenderer.setYAxisMin(minY);
            mRenderer.setXAxisMin(minX);
            mRenderer.setXAxisMax(maxX, Double.valueOf(maxX / minX).intValue());
            mRenderer.setShowGrid(false); // we don't the grid

            final GraphicalView chartView = ChartFactory.getLineChartView(mParentFragmentActivity, dataset, mRenderer);
            chartView.repaint();

            graphPlaceHolder.addView(chartView, 0);

//            //TODO
//            //TEST
//            final int MAX = 40;
//            @NotNull final Number[] x = new Number[MAX];
//            @NotNull final Number[] y = new Number[MAX];
//            for (int i = 0; i < MAX; i++) {
//                x[i] = i;
//                y[i] = Math.random() * MAX + Math.sin(i / MAX);
//            }
//
//            // create our series from our array of nums:
//            @NotNull final XYSeries series2 = new SimpleXYSeries(
//                    Arrays.asList(x),
//                    Arrays.asList(y),
//                    "Bandwidth Usage");
//
//            mySimpleXYPlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.WHITE);
////            mySimpleXYPlot.getGraphWidget().getGridLinePaint().setColor(Color.BLACK);
////            mySimpleXYPlot.getGraphWidget().getGridLinePaint().setPathEffect(new DashPathEffect(new float[]{1,1}, 1));
//            mySimpleXYPlot.getGraphWidget().getDomainOriginLinePaint().setColor(Color.WHITE);
//            mySimpleXYPlot.getGraphWidget().getRangeOriginLinePaint().setColor(Color.WHITE);
//
//            mySimpleXYPlot.setBorderStyle(Plot.BorderStyle.SQUARE, null, null);
//            mySimpleXYPlot.getBorderPaint().setStrokeWidth(1);
//            mySimpleXYPlot.getBorderPaint().setAntiAlias(false);
//            mySimpleXYPlot.getBorderPaint().setColor(Color.BLACK);
//
//            // Create a formatter to use for drawing a series using LineAndPointRenderer:
//            @NotNull LineAndPointFormatter series1Format = new LineAndPointFormatter(
//                    Color.rgb(0, 100, 0),                   // line color
//                    Color.rgb(0, 100, 0),                   // point color
//                    Color.rgb(100, 200, 0), null);                // fill color
//
//            // setup our line fill paint to be a slightly transparent gradient:
//            @NotNull Paint lineFill = new Paint();
//            lineFill.setAlpha(200);
//            lineFill.setShader(new LinearGradient(0, 0, 0, 250, Color.WHITE, Color.GREEN, Shader.TileMode.MIRROR));
//
//            @NotNull LineAndPointFormatter formatter = new LineAndPointFormatter(Color.rgb(0, 0, 0), Color.BLUE, Color.RED, null);
//            formatter.setFillPaint(lineFill);
//            mySimpleXYPlot.getGraphWidget().setPaddingRight(2);
//            mySimpleXYPlot.addSeries(series2, formatter);
//
//            // draw a domain tick for each year:
//            mySimpleXYPlot.setDomainStep(XYStepMode.SUBDIVIDE, x.length);
//
//            // customize our domain/range labels
//            mySimpleXYPlot.setDomainLabel("X");
//            mySimpleXYPlot.setRangeLabel("Bandwidth (MBps)");
//
//            // get rid of decimal points in our range labels:
//            mySimpleXYPlot.setRangeValueFormat(new DecimalFormat("0"));

            //END TEST

        }

        if (exception != null) {
            errorPlaceHolderView.setText(exception.getClass().getSimpleName() + ": " + Throwables.getRootCause(exception).getMessage());
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

    private class DataPoint {
        private final double x;
        private final double y;

        private DataPoint(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DataPoint dataPoint = (DataPoint) o;

            if (Double.compare(dataPoint.x, x) != 0) return false;
            if (Double.compare(dataPoint.y, y) != 0) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            temp = Double.doubleToLongBits(x);
            result = (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(y);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return "DataPoint{" +
                    "x=" + x +
                    ", y=" + y +
                    '}';
        }
    }
}
