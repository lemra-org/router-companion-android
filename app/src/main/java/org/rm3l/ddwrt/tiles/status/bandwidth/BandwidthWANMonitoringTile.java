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
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
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
import org.achartengine.renderer.XYSeriesRenderer.FillOutsideLine;
import org.apache.commons.io.FileUtils;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;

/**
 *
 */
public class BandwidthWANMonitoringTile extends DDWRTTile<None> {

    private static final String LOG_TAG = BandwidthWANMonitoringTile.class.getSimpleName();

    public static final String RT_GRAPHS = "rt_graphs";

    /*
    root@r7000:~# cat /proc/net/dev
Inter-|   Receive                                                |  Transmit
 face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed
   br0: 4089595951 20007416    0 3014    0     0          0         0 36941409374 31692808    0    0    0     0       0          0
 vlan1:       0       0    0    0    0     0          0         0 110064455  885773    0    0    0     0       0          0
  sit0:       0       0    0    0    0     0          0         0        0       0    0    0    0     0       0          0
    lo: 2597144    9051    0    0    0     0          0         0  2597144    9051    0    0    0     0       0          0
 wl0.1: 27876702  142125    0    0    0 28598621          0         0 306106604  990000  428    0    0     0       0          0
  eth0: 3412755119 34715262    0   22    0     0          0         0 141077098 20701272    0    0    0     0       0          0
  eth1: 2354406847 11291259    0    0    0 28598621          0         0 2776692944 16600366  193    0    0     0       0          0
  eth2: 2915477195 12548189    0    0    0 945775          0         0 1144999882 20021807 3502    0    0     0       0          0
ip6tnl0:       0       0    0    0    0     0          0         0        0       0    0    0    0     0       0          0
 vlan2: 41303011192 34714735    0    0    0     0          0    292833 4243173995 19815491    0    0    0     0       0          0
 teql0:       0       0    0    0    0     0          0         0        0       0    0    0    0     0       0          0
     */

    //Pivot table of all data
    private static final String READ_IFACE_DATA_FROM_PROC_NET_DEV_FMT =
            "cat /proc/net/dev | grep \"%s\" | awk '{for( i=2; i<=NF; i++ ){printf( \"%%s\\n\", $i )}; printf( \"--XXX--\\n\");}'";

    @NonNull
    private final BandwidthMonitoringTile.BandwidthMonitoringIfaceData bandwidthMonitoringIfaceData =
            new BandwidthMonitoringTile.BandwidthMonitoringIfaceData();

    private String wanIface;
    private long mLastSync;

    private NVRAMInfo nvRamInfoFromRouter;

    private Loader<None> mCurrentLoader;

    public BandwidthWANMonitoringTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, Router router) {
        super(parentFragment, arguments, router, R.layout.tile_status_bandwidth_monitoring_iface,
                null);

        if (mParentFragmentPreferences != null && !mParentFragmentPreferences.contains(getFormattedPrefKey(RT_GRAPHS))) {
            mParentFragmentPreferences.edit()
                    .putBoolean(getFormattedPrefKey(RT_GRAPHS), true)
                    .apply();
        }
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
        this.mCurrentLoader = new AsyncTaskLoader<None>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public None loadInBackground() {

                try {
                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Init background loader for " + BandwidthMonitoringTile.class + ": routerInfo=" +
                            mRouter + " / nbRunsLoader=" + nbRunsLoader);

                    if (mRefreshing.getAndSet(true)) {
                        return (None) new None().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    updateProgressBarViewSeparator(0);

                    mLastSync = System.currentTimeMillis();

                    updateProgressBarViewSeparator(10);

                    //Start by getting information about the WAN iface name
                    nvRamInfoFromRouter = SSHUtils
                            .getNVRamInfoFromRouter(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                    NVRAMInfo.WAN_IFACE);
                    updateProgressBarViewSeparator(45);
                    if (nvRamInfoFromRouter == null) {
                        throw new IllegalStateException("Whoops. WAN Iface could not be determined.");
                    }

                    wanIface = nvRamInfoFromRouter
                            .getProperty(NVRAMInfo.WAN_IFACE);

                    if (Strings.isNullOrEmpty(wanIface)) {
                        throw new IllegalStateException("Whoops. WAN Iface could not be determined.");
                    }

                    @SuppressWarnings("MalformedFormatString")
                    final String[] netDevDataForIface = SSHUtils.getManualProperty(mParentFragmentActivity,
                            mRouter, mGlobalPreferences,
                            String.format(READ_IFACE_DATA_FROM_PROC_NET_DEV_FMT, wanIface),
                            "sleep 1",
                            String.format(READ_IFACE_DATA_FROM_PROC_NET_DEV_FMT, wanIface));
                    updateProgressBarViewSeparator(60);
                    if (netDevDataForIface == null || netDevDataForIface.length < 33) {
                        return null;
                    }

                    final List<String> netDevWanIfaceList = Arrays.asList(netDevDataForIface);

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

                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_bytes_t1", netDevWanIfaceList.get(17));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_packets_t1", netDevWanIfaceList.get(18));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_errs_t1", netDevWanIfaceList.get(19));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_drop_t1", netDevWanIfaceList.get(20));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_fifo_t1", netDevWanIfaceList.get(21));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_frame_t1", netDevWanIfaceList.get(22));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_compressed_t1", netDevWanIfaceList.get(23));
                    nvRamInfoFromRouter.setProperty(wanIface + "_rcv_multicast_t1", netDevWanIfaceList.get(24));

                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_bytes_t1", netDevWanIfaceList.get(25));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_packets_t1", netDevWanIfaceList.get(26));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_errs_t1", netDevWanIfaceList.get(27));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_drop_t1", netDevWanIfaceList.get(28));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_fifo_t1", netDevWanIfaceList.get(29));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_colls_t1", netDevWanIfaceList.get(30));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_carrier_t1", netDevWanIfaceList.get(31));
                    nvRamInfoFromRouter.setProperty(wanIface + "_xmit_compressed_t1", netDevWanIfaceList.get(32));

                    //Ingress
                    double wanRcvBytes;
                    try {
                        wanRcvBytes = (Double.parseDouble(nvRamInfoFromRouter.getProperty(wanIface + "_rcv_bytes_t1", "-255")) -
                                Double.parseDouble(nvRamInfoFromRouter.getProperty(wanIface + "_rcv_bytes", "-1")));
                        if (wanRcvBytes >= 0.) {
                            bandwidthMonitoringIfaceData.addData("IN",
                                    new BandwidthMonitoringTile.DataPoint(timestamp, wanRcvBytes));
                        }
                    } catch (@NonNull final NumberFormatException nfe) {
                        return null;
                    }
                    nvRamInfoFromRouter.setProperty(wanIface + "_ingress_MB",
                            byteCountToDisplaySize(Double.valueOf(wanRcvBytes).longValue()) + "ps");

                    //Egress
                    double wanXmitBytes;
                    try {
                        wanXmitBytes = (Double.parseDouble(nvRamInfoFromRouter.getProperty(wanIface + "_xmit_bytes_t1", "-255")) -
                                Double.parseDouble(nvRamInfoFromRouter.getProperty(wanIface + "_xmit_bytes", "-1")));
                        if (wanXmitBytes >= 0.) {
                            bandwidthMonitoringIfaceData.addData("OUT",
                                    new BandwidthMonitoringTile.DataPoint(timestamp, wanXmitBytes));
                        }

                    } catch (@NonNull final NumberFormatException nfe) {
                        return null;
                    }

                    nvRamInfoFromRouter.setProperty(wanIface + "_egress_MB",
                            byteCountToDisplaySize(Double.valueOf(wanXmitBytes).longValue()) + "ps");

                    updateProgressBarViewSeparator(90);

                    return new None();

                } catch (@NonNull final Exception e) {
                    e.printStackTrace();
                    return (None) new None().setException(e);
                }
            }
        };
        return mCurrentLoader;
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
        Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

        try {
            final boolean isThemeLight = ColorUtils.isThemeLight(mParentFragmentActivity);

            layout.findViewById(R.id.tile_status_bandwidth_monitoring_graph_loading_view)
                    .setVisibility(View.GONE);
            layout.findViewById(R.id.tile_status_bandwidth_monitoring_graph_placeholder)
                    .setVisibility(View.VISIBLE);

            final ImageButton tileMenu =
                    (ImageButton) layout.findViewById(R.id.tile_status_bandwidth_monitoring_menu);

            if (!isThemeLight) {
                //Set menu background to white
                tileMenu.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
            }

            if (mParentFragmentPreferences == null) {
                tileMenu.setVisibility(View.INVISIBLE);
            } else {
                tileMenu.setVisibility(View.VISIBLE);
            }

            tileMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final PopupMenu popup = new PopupMenu(mParentFragmentActivity, v);
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            final int itemId = item.getItemId();
                            switch (itemId) {
                                case R.id.tile_status_bandwidth_realtime_graphs:
                                    final boolean rtGraphsEnabled = !item.isChecked();
                                    if (rtGraphsEnabled) {
                                        //Restart loader
                                        if (mSupportLoaderManager != null && mCurrentLoader != null) {
                                            mSupportLoaderManager.restartLoader(
                                                    mCurrentLoader.getId(),
                                                    mFragmentArguments,
                                                    BandwidthWANMonitoringTile.this);
                                        }
                                    }
                                    if (mParentFragmentPreferences != null) {
                                        mParentFragmentPreferences.edit()
                                                .putBoolean(getFormattedPrefKey(RT_GRAPHS), rtGraphsEnabled)
                                                .apply();
                                    }
                                    return true;
                                default:
                                    break;
                            }
                            return false;
                        }
                    });

                    final MenuInflater inflater = popup.getMenuInflater();
                    final Menu menu = popup.getMenu();
                    inflater.inflate(R.menu.tile_status_bandwidth_monitoring_options, menu);

                    final MenuItem rtMenuItem = menu.findItem(R.id.tile_status_bandwidth_realtime_graphs);
                    if (mParentFragmentPreferences != null) {
                        rtMenuItem.setVisible(true);
                        rtMenuItem
                                .setEnabled(mParentFragmentPreferences.contains(getFormattedPrefKey(RT_GRAPHS)));
                        rtMenuItem
                                .setChecked(mParentFragmentPreferences
                                        .getBoolean(getFormattedPrefKey(RT_GRAPHS), false));
                    } else {
                        rtMenuItem.setVisible(false);
                    }

                    popup.show();
                }
            });


            //noinspection ConstantConditions
            if (data == null || bandwidthMonitoringIfaceData.getData().isEmpty()) {
                data = (None) new None().setException(new DDWRTNoDataException("No Data!"));
            }

            final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_error);

            final Exception exception = data.getException();

            final View legendView = this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_graph_legend);

            if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

                if (exception == null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }

                legendView.setVisibility(View.VISIBLE);

                ((TextView) this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_title))
                        .setText(this.mParentFragmentActivity.getResources()
                                .getString(R.string.bandwidth_usage_mb) + (!Strings.isNullOrEmpty(wanIface) ?
                                (": " + wanIface) : ""));

                final TextView inTextView =
                        (TextView) this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_in);
                final TextView outTextView =
                        (TextView) this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_out);

                inTextView.setText(nvRamInfoFromRouter.getProperty(wanIface + "_ingress_MB", "-").replace("bytes", "B"));
                outTextView.setText(nvRamInfoFromRouter.getProperty(wanIface + "_egress_MB", "-").replace("bytes", "B"));

                final LinearLayout graphPlaceHolder = (LinearLayout) this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_graph_placeholder);
                final Map<String, EvictingQueue<BandwidthMonitoringTile.DataPoint>> dataCircularBuffer = bandwidthMonitoringIfaceData.getData();

                long maxX = System.currentTimeMillis() + 5000;
                long minX = System.currentTimeMillis() - 5000;
                double maxY = 10;
                double minY = 1.;

                final XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
                final XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();

                int i = 0;
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
                    renderer.setLineWidth(5);

                    final int colorForIface = ColorUtils.getColor(iface);
                    renderer.setColor(colorForIface);
                    // Include low and max value
                    renderer.setDisplayBoundingPoints(true);
                    // we add point markers
                    renderer.setPointStyle(PointStyle.POINT);
                    renderer.setPointStrokeWidth(1);

                    final FillOutsideLine fill = new FillOutsideLine(FillOutsideLine.Type.BOUNDS_ABOVE);
                    fill.setColor(colorForIface);
                    renderer.addFillOutsideLine(fill);

                    if (i == 0) {
                        this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_graph_legend_series1_bar)
                            .setBackgroundColor(colorForIface);
                        final TextView series1TextView = (TextView)
                                this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_graph_legend_series1_text);
                        series1TextView.setText(iface);
                        series1TextView.setTextColor(colorForIface);

                    } else if (i == 1) {
                        this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_graph_legend_series2_bar)
                                .setBackgroundColor(colorForIface);
                        final TextView series2TextView = (TextView)
                                this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_graph_legend_series2_text);
                        series2TextView.setText(iface);
                        series2TextView.setTextColor(colorForIface);
                    }
                    i++;

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
                        .replace("bytes", "B") + "ps");
                if (maxY != 0 && maxY / 2 >= 9000) {
                    mRenderer.addYTextLabel(maxY / 2, byteCountToDisplaySize(Double.valueOf(maxY / 2).longValue())
                            .replace("bytes", "B") + "ps");
                }

                // We want to avoid black border
                //setting text size of the title
                mRenderer.setChartTitleTextSize(25);
                //setting text size of the axis title
                mRenderer.setAxisTitleTextSize(22);
                //setting text size of the graph label
//                mRenderer.setLabelsTextSize(22);

                mRenderer.setLegendTextSize(22);

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
                mRenderer.setYAxisMax(maxY + 1024);
                mRenderer.setYAxisMin(minY);
                mRenderer.setXAxisMin(minX);
                mRenderer.setXAxisMax(maxX + 1024);
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
                mRenderer.setLabelsTextSize(30f);
                final int blackOrWhite = ContextCompat.getColor(mParentFragmentActivity,
                        isThemeLight ? R.color.black : R.color.white);
                mRenderer.setAxesColor(blackOrWhite);
                mRenderer.setShowLegend(false);
                mRenderer.setXLabelsColor(blackOrWhite);
                mRenderer.setYLabelsColor(0, blackOrWhite);

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
                legendView.setVisibility(View.GONE);
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
                updateProgressBarWithError();
            } else if (exception == null){
                legendView.setVisibility(View.VISIBLE);
                updateProgressBarWithSuccess();
                if (bandwidthMonitoringIfaceData.getData().isEmpty()) {
                    errorPlaceHolderView.setText("Error: No Data!");
                    errorPlaceHolderView.setVisibility(View.VISIBLE);
                }
            }

            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished(): done loading!");
        } finally {
            mRefreshing.set(false);
            doneLoading(loader);
        }
    }

    private void doneLoading(Loader<None> loader) {
        if (mParentFragmentPreferences != null &&
                mParentFragmentPreferences.getBoolean(getFormattedPrefKey(RT_GRAPHS), false)) {
            //Reschedule next run right away, to have a pseudo realtime effect, regardless of the actual sync pref!
            //TODO Check how much extra load that represents on the router
            doneWithLoaderInstance(this, loader, TimeUnit.SECONDS.toMillis(10));
        } else {
            //Use classical sync
            doneWithLoaderInstance(this, loader);
        }
    }


}
