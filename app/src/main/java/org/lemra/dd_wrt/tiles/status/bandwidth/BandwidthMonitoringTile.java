package org.lemra.dd_wrt.tiles.status.bandwidth;

import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.androidplot.Plot;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYStepMode;
import com.google.common.base.Throwables;

import org.lemra.dd_wrt.R;
import org.lemra.dd_wrt.api.conn.NVRAMInfo;
import org.lemra.dd_wrt.api.conn.Router;
import org.lemra.dd_wrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.lemra.dd_wrt.tiles.DDWRTTile;

import java.text.DecimalFormat;
import java.util.Arrays;

/**
 * Created by armel on 8/31/14.
 */
public class BandwidthMonitoringTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = BandwidthMonitoringTile.class.getSimpleName();

    private final String iface;

    public BandwidthMonitoringTile(SherlockFragmentActivity parentFragmentActivity, Bundle arguments, Router router, final String iface) {
        super(parentFragmentActivity, arguments, router, R.layout.tile_status_bandwidth_monitoring_iface, R.id.tile_status_bandwidth_monitoring_togglebutton);
        this.iface = iface;
    }

    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

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

                } catch (final Exception e) {
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

    @Override
    public void onLoadFinished(Loader<NVRAMInfo> loader, NVRAMInfo data) {
        //Set tiles
        Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

        if (data == null) {
            //FIXME Just commented out for tests
            data = new NVRAMInfo();
//            data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
            //END FIXME
        }

        final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_error);

        final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

            if (exception == null) {
                if (errorPlaceHolderView != null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }
            }

            ((TextView) this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_title)).setText(this.iface);

            final XYPlot mySimpleXYPlot = (XYPlot) this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_graph_placeholder);

            //TODO
            //TEST
            final int MAX = 40;
            final Number[] x = new Number[MAX];
            final Number[] y = new Number[MAX];
            for (int i = 0; i < MAX; i++) {
                x[i] = i;
                y[i] = Math.random() * MAX + Math.sin(i / MAX);
            }

            // create our series from our array of nums:
            final XYSeries series2 = new SimpleXYSeries(
                    Arrays.asList(x),
                    Arrays.asList(y),
                    "Bandwidth Usage");

            mySimpleXYPlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.WHITE);
//            mySimpleXYPlot.getGraphWidget().getGridLinePaint().setColor(Color.BLACK);
//            mySimpleXYPlot.getGraphWidget().getGridLinePaint().setPathEffect(new DashPathEffect(new float[]{1,1}, 1));
            mySimpleXYPlot.getGraphWidget().getDomainOriginLinePaint().setColor(Color.WHITE);
            mySimpleXYPlot.getGraphWidget().getRangeOriginLinePaint().setColor(Color.WHITE);

            mySimpleXYPlot.setBorderStyle(Plot.BorderStyle.SQUARE, null, null);
            mySimpleXYPlot.getBorderPaint().setStrokeWidth(1);
            mySimpleXYPlot.getBorderPaint().setAntiAlias(false);
            mySimpleXYPlot.getBorderPaint().setColor(Color.BLACK);

            // Create a formatter to use for drawing a series using LineAndPointRenderer:
            LineAndPointFormatter series1Format = new LineAndPointFormatter(
                    Color.rgb(0, 100, 0),                   // line color
                    Color.rgb(0, 100, 0),                   // point color
                    Color.rgb(100, 200, 0), null);                // fill color

            // setup our line fill paint to be a slightly transparent gradient:
            Paint lineFill = new Paint();
            lineFill.setAlpha(200);
            lineFill.setShader(new LinearGradient(0, 0, 0, 250, Color.WHITE, Color.GREEN, Shader.TileMode.MIRROR));

            LineAndPointFormatter formatter = new LineAndPointFormatter(Color.rgb(0, 0, 0), Color.BLUE, Color.RED, null);
            formatter.setFillPaint(lineFill);
            mySimpleXYPlot.getGraphWidget().setPaddingRight(2);
            mySimpleXYPlot.addSeries(series2, formatter);

            // draw a domain tick for each year:
            mySimpleXYPlot.setDomainStep(XYStepMode.SUBDIVIDE, x.length);

            // customize our domain/range labels
            mySimpleXYPlot.setDomainLabel("X");
            mySimpleXYPlot.setRangeLabel("Bandwidth (MBps)");

            // get rid of decimal points in our range labels:
            mySimpleXYPlot.setRangeValueFormat(new DecimalFormat("0"));

            //END TEST

        }

        if (exception != null) {
            if (errorPlaceHolderView != null) {
                errorPlaceHolderView.setText(Throwables.getRootCause(exception).getMessage());
                errorPlaceHolderView.setVisibility(View.VISIBLE);
            }
        }

        doneWithLoaderInstance(this, loader,
                R.id.tile_status_bandwidth_monitoring_togglebutton_title, R.id.tile_status_bandwidth_monitoring_togglebutton_separator);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");
    }

    @Override
    public void onClick(View view) {
        Toast.makeText(this.mParentFragmentActivity, this.getClass().getSimpleName() +
                " (" + this.iface + ")", Toast.LENGTH_SHORT).show();
    }
}
