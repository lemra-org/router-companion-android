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
 * Contact Info: Armel Soro <armel+router_companion@rm3l.org>
 */

package org.rm3l.router_companion.tiles.status.bandwidth;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.resources.None;
import org.rm3l.router_companion.resources.RouterData;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;

// import com.esotericsoftware.kryo.Kryo;
// import com.esotericsoftware.kryo.KryoSerializable;
// import com.esotericsoftware.kryo.io.Input;
// import com.esotericsoftware.kryo.io.Output;

/** */
public class BandwidthMonitoringTile extends DDWRTTile<None> {

  public static class BandwidthMonitoringIfaceData
      extends RouterData<Map<String, EvictingQueue<DataPoint>>> {
    //            implements KryoSerializable {

    public BandwidthMonitoringIfaceData() {
      super();
      super.setData(Maps.newConcurrentMap());
    }

    public BandwidthMonitoringIfaceData addData(final String iface, final DataPoint point) {
      final Map<String, EvictingQueue<DataPoint>> data = super.getData();
      @SuppressWarnings("ConstantConditions")
      final EvictingQueue<DataPoint> dataPointsForIface = data.get(iface);
      if (dataPointsForIface == null) {
        data.put(iface, EvictingQueue.create(MAX_DATA_POINTS));
      }
      data.get(iface).add(point);
      return this;
    }

    @Nullable
    public Map<String, Collection<DataPoint>> toStringListMap() {
      final Map<String, EvictingQueue<DataPoint>> data = super.getData();
      if (data == null) {
        return null;
      }
      final Multimap<String, DataPoint> multimap = ArrayListMultimap.create();
      for (Map.Entry<String, EvictingQueue<DataPoint>> entry : data.entrySet()) {
        final String key = entry.getKey();
        final EvictingQueue<DataPoint> value = entry.getValue();
        if (value != null) {
          for (final DataPoint dataPoint : value) {
            multimap.put(key, dataPoint);
          }
        }
      }
      return multimap.asMap();
    }

    //        @Override
    //        public void write(Kryo kryo, Output output) {
    //            final Map<String, EvictingQueue<DataPoint>> data = super.getData();
    //            final int dataSize = data.size();
    //            output.writeInt(dataSize, true);
    //            for (final Map.Entry<String, EvictingQueue<DataPoint>> entry : data.entrySet()) {
    //                final String key = entry.getKey();
    //                final EvictingQueue<DataPoint> value = entry.getValue();
    //
    //                output.writeString(key);
    //                final int size = value.size();
    //                output.writeInt(size, true);
    //                for (final DataPoint dataPoint : value) {
    //                    kryo.writeObjectOrNull(output, dataPoint, DataPoint.class);
    //                }
    //            }
    //        }
    //
    //        @Override
    //        public void read(Kryo kryo, Input input) {
    //            final int length = input.readInt(true);
    //            final Map<String, EvictingQueue<DataPoint>> data = new
    // ConcurrentHashMap<>(length);
    //            for (int i = 0 ; i < length ; i++) {
    //                final String key = input.readString();
    //
    //                final EvictingQueue<DataPoint> dataPoints =
    // EvictingQueue.<DataPoint>create(MAX_DATA_POINTS);
    //                final int evictingQueueTotalElements = input.readInt(true);
    //                for (int j = 0; j < evictingQueueTotalElements; j++) {
    //                    final DataPoint dataPoint = kryo.readObjectOrNull(input, DataPoint.class);
    //                    if (dataPoint == null) {
    //                        continue;
    //                    }
    //                    dataPoints.add(dataPoint);
    //                }
    //
    //                data.put(key, dataPoints);
    //            }
    //
    //            setData(data);
    //        }
  }

  public static class DataPoint {

    //            implements KryoSerializable {
    private long timestamp;

    private double value;

    public DataPoint() {}

    public DataPoint(long timestamp, double value) {
      this.timestamp = timestamp;
      this.value = value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      DataPoint dataPoint = (DataPoint) o;

      return Objects.equal(dataPoint.timestamp, timestamp) && Objects.equal(dataPoint.value, value);
    }

    public long getTimestamp() {
      return timestamp;
    }

    public DataPoint setTimestamp(long timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public double getValue() {
      return value;
    }

    public DataPoint setValue(double value) {
      this.value = value;
      return this;
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
      return "DataPoint{" + "timestamp=" + timestamp + ", value=" + value + '}';
    }

    //        @Override
    //        public void write(Kryo kryo, Output output) {
    //            output.writeLong(timestamp);
    //            output.writeDouble(value);
    //        }
    //
    //        @Override
    //        public void read(Kryo kryo, Input input) {
    //            timestamp = input.readLong();
    //            value = input.readDouble();
    //        }
  }

  // TODO Could be a user preference, actually
  public static final int MAX_DATA_POINTS = 100;

  // END TESTS ONLY
  // TODO TESTS ONLY
  private static final boolean BW_MONIT_TEST = true;

  private static final String LOG_TAG = BandwidthMonitoringTile.class.getSimpleName();

  @NonNull
  private final BandwidthMonitoringIfaceData bandwidthMonitoringIfaceData =
      new BandwidthMonitoringIfaceData();

  private long mLastSync;

  public BandwidthMonitoringTile(
      @NonNull Fragment parentFragment, @NonNull Bundle arguments, Router router) {
    super(parentFragment, arguments, router, R.layout.tile_status_bandwidth_monitoring_iface, null);
  }

  public void fillIfaceDataPoint(@NonNull final String iface) {

    if (RouterCompanionAppConstants.TEST_MODE || BW_MONIT_TEST || Utils.isDemoRouter(mRouter)) {
      // FIXME TEST MODE
      final double random = new Random().nextDouble() * 1024;

      bandwidthMonitoringIfaceData.addData(
          iface, new DataPoint(System.currentTimeMillis(), random * Math.sqrt(random * Math.E)));
    }

    // FIXME Add real data down this line
  }

  @Override
  public Integer getTileHeaderViewId() {
    return R.id.tile_status_bandwidth_monitoring_hdr;
  }

  @Override
  public Integer getTileTitleViewId() {
    return R.id.tile_status_bandwidth_monitoring_title;
  }

  @Override
  public void onLoadFinished(@NonNull Loader<None> loader, @Nullable None data) {
    // Set tiles
    FirebaseCrashlytics.getInstance().log("onLoadFinished: loader=" + loader + " / data=" + data);

    try {
      //noinspection ConstantConditions
      if (data == null || bandwidthMonitoringIfaceData.getData().isEmpty()) {
        data = (None) new None().setException(new DDWRTNoDataException("No Data!"));
      }

      final TextView errorPlaceHolderView =
              this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_error);

      final Exception exception = data.getException();

      if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

        if (exception == null) {
          errorPlaceHolderView.setVisibility(View.GONE);
        }

        final LinearLayout graphPlaceHolder =
                this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_graph_placeholder);
        final Map<String, EvictingQueue<DataPoint>> dataCircularBuffer =
            bandwidthMonitoringIfaceData.getData();

        long maxX = System.currentTimeMillis() + 5000;
        long minX = System.currentTimeMillis() - 5000;
        double maxY = 10;
        double minY = 1.;

        final XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        final XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();

        for (final Map.Entry<String, EvictingQueue<DataPoint>> entry :
            dataCircularBuffer.entrySet()) {
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

          final Integer ifaceColor = ColorUtils.Companion.getColor(iface);

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

        final GraphicalView chartView =
            ChartFactory.getTimeChartView(graphPlaceHolder.getContext(), dataset, mRenderer, null);
        chartView.repaint();

        graphPlaceHolder.addView(chartView, 0);

        // Update last sync
        final RelativeTimeTextView lastSyncView =
                layout.findViewById(R.id.tile_last_sync);
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
        if (bandwidthMonitoringIfaceData.getData().isEmpty()) {
          errorPlaceHolderView.setText("Error: No Data!");
          errorPlaceHolderView.setVisibility(View.VISIBLE);
        }
      }

      FirebaseCrashlytics.getInstance().log("onLoadFinished(): done loading!");
    } finally {
      mRefreshing.set(false);
      doneWithLoaderInstance(this, loader);
    }
  }

  @Override
  protected Loader<None> getLoader(int id, Bundle args) {
    return new AsyncTaskLoader<None>(this.mParentFragmentActivity) {

      @Nullable
      @Override
      public None loadInBackground() {

        try {
          FirebaseCrashlytics.getInstance()
              .log(
                  "Init background loader for "
                      + BandwidthMonitoringTile.class
                      + ": routerInfo="
                      + mRouter
                      + " / nbRunsLoader="
                      + nbRunsLoader);

          if (mRefreshing.getAndSet(true)) {
            return (None) new None().setException(new DDWRTTileAutoRefreshNotAllowedException());
          }
          nbRunsLoader++;

          mLastSync = System.currentTimeMillis();

          // Get ifaces and fetch data points for each of these ifaces
          fillIfacesDataPoints(getIfaces());

          return new None();
        } catch (@NonNull final Exception e) {
          e.printStackTrace();
          return (None) new None().setException(e);
        }
      }
    };
  }

  @Override
  protected String getLogTag() {
    return LOG_TAG;
  }

  @Nullable
  @Override
  protected OnClickIntent getOnclickIntent() {
    // TODO
    return null;
  }

  private void fillIfacesDataPoints(final Collection<String> ifaces) {

    for (String iface : ifaces) {
      fillIfaceDataPoint(iface);
    }
  }

  @NonNull
  private Collection<String> getIfaces() throws Exception {

    // TODO TESTS: Real ifaces for DD-WRT Routers
    //noinspection PointlessBooleanExpression,ConstantConditions
    //        if (DDWRTCompanionConstants.TEST_MODE && (this.mRouter == null ||
    // !StringUtils.containsIgnoreCase(this.mRouter.getName(), "ddwrt"))) {
    if (Utils.isDemoRouter(mRouter)) {
      // FIXME TEST MODE
      return Sets.newTreeSet(Arrays.asList("wlan0", "lan1", "eth2"));
    }

    final Set<String> ifacesConsidered = Sets.newHashSet();

    final NVRAMInfo nvramInfo =
        SSHUtils.getNVRamInfoFromRouter(
            mParentFragmentActivity,
            mRouter,
            mGlobalPreferences,
            NVRAMInfo.Companion.getLAN_IFNAME(),
            NVRAMInfo.Companion.getWAN_IFNAME(),
            NVRAMInfo.Companion.getLANDEVS());

    if (nvramInfo == null) {
      return ifacesConsidered;
    }

    final String lanIfname = nvramInfo.getProperty(NVRAMInfo.Companion.getLAN_IFNAME());
    if (lanIfname != null) {
      ifacesConsidered.add(lanIfname);
    }

    final String wanIfname = nvramInfo.getProperty(NVRAMInfo.Companion.getWAN_IFNAME());
    if (wanIfname != null) {
      ifacesConsidered.add(wanIfname);
    }

    final String landevs = nvramInfo.getProperty(NVRAMInfo.Companion.getLANDEVS());
    if (landevs != null) {
      final List<String> splitToList =
          Splitter.on(" ").omitEmptyStrings().trimResults().splitToList(landevs);
      if (splitToList != null && !splitToList.isEmpty()) {
        for (final String landev : splitToList) {
          if (landev == null) {
            continue;
          }
          ifacesConsidered.add(landev);
        }
      }
    }

    return ifacesConsidered;
  }
}
