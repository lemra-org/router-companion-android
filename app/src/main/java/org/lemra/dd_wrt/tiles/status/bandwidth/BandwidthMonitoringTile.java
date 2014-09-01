package org.lemra.dd_wrt.tiles.status.bandwidth;

import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.common.base.Throwables;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewStyle;
import com.jjoe64.graphview.LineGraphView;

import org.lemra.dd_wrt.R;
import org.lemra.dd_wrt.api.conn.NVRAMInfo;
import org.lemra.dd_wrt.api.conn.Router;
import org.lemra.dd_wrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.lemra.dd_wrt.tiles.DDWRTTile;

import static com.jjoe64.graphview.GraphView.GraphViewData;

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

            //TODO
            //TEST
            // init example series data
            GraphViewSeries exampleSeries = new GraphViewSeries(new GraphViewData[]{
                    new GraphViewData(1, 2.0d)
                    , new GraphViewData(2, 1.5d)
                    , new GraphViewData(3, 2.5d)
                    , new GraphViewData(4, 1.0d)
            });

            GraphView graphView = new LineGraphView(
                    this.layout.getContext() // context
                    , "GraphViewDemo" // heading
            );
            graphView.getGraphViewStyle().setGridStyle(GraphViewStyle.GridStyle.HORIZONTAL);
            graphView.addSeries(exampleSeries); // data

            ((LinearLayout) this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_graph_placeholder))
                    .addView(graphView);
            ;
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
