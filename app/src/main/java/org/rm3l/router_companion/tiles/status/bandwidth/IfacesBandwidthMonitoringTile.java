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
package org.rm3l.router_companion.tiles.status.bandwidth;

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
import android.text.TextUtils;
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
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.BiMap;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer.FillOutsideLine;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.resources.None;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;

//import com.esotericsoftware.kryo.Kryo;
//import com.esotericsoftware.kryo.io.Input;
//import com.esotericsoftware.kryo.io.Output;

/**
 *
 */
public class IfacesBandwidthMonitoringTile extends DDWRTTile<None> {

  public static final String RT_GRAPHS = "rt_graphs";
  public static final String IFACE_DISPLAYED = "iface_displayed";

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
  private static final String LOG_TAG = IfacesBandwidthMonitoringTile.class.getSimpleName();
  //Pivot table of all data
  private static final String READ_IFACE_DATA_FROM_PROC_NET_DEV_FMT =
      "cat /proc/net/dev | grep \"%s\" | awk '{for( i=2; i<=NF; i++ ){printf( \"%%s\\n\", $i )}; printf( \"--XXX--\\n\");}'";
  private final BiMap<String, Integer> ifacesMenuIds = HashBiMap.create();

  @NonNull private final Map<String, BandwidthMonitoringTile.BandwidthMonitoringIfaceData>
      bandwidthMonitoringIfaceDataMap = new HashMap<>();

  private final Map<String, NVRAMInfo> nvRamInfoFromRouterPerIface = new HashMap<>();
  private final File mBandwidthMonitoringData;
  private final AtomicReference<String> mIfacePreviouslySelectedForDisplay =
      new AtomicReference<>(null);
  private long mLastSync;
  private Loader<None> mCurrentLoader;

  //    private Kryo mKryo;

  public IfacesBandwidthMonitoringTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments,
      Router router) {
    super(parentFragment, arguments, router, R.layout.tile_status_bandwidth_monitoring_iface, null);

    mBandwidthMonitoringData = new File(mParentFragmentActivity.getCacheDir(),
        this.getClass().getSimpleName() + ".tmp.dat");

    //        this.mKryo = new Kryo();
    //        mKryo.setReferences(false);
    //        mKryo.setRegistrationRequired(false);
    //        mKryo.register(Map.class);
    //        mKryo.register(HashMap.class);
    //        mKryo.register(BandwidthMonitoringTile.DataPoint.class);
    //        mKryo.register(BandwidthMonitoringTile.BandwidthMonitoringIfaceData.class);

    if (mParentFragmentPreferences != null && !mParentFragmentPreferences.contains(
        getFormattedPrefKey(RT_GRAPHS))) {
      mParentFragmentPreferences.edit().putBoolean(getFormattedPrefKey(RT_GRAPHS), true).apply();
    }
  }

  @Override public int getTileHeaderViewId() {
    return R.id.tile_status_bandwidth_monitoring_hdr;
  }

  @Override public int getTileTitleViewId() {
    return R.id.tile_status_bandwidth_monitoring_title;
  }

  @Nullable @Override protected Loader<None> getLoader(int id, Bundle args) {
    this.mCurrentLoader = new AsyncTaskLoader<None>(this.mParentFragmentActivity) {

      @Nullable @Override public None loadInBackground() {

        try {
          Crashlytics.log(Log.DEBUG, LOG_TAG, "Init background loader for "
              + BandwidthMonitoringTile.class
              + ": routerInfo="
              + mRouter
              + " / nbRunsLoader="
              + nbRunsLoader);

          if (mRefreshing.getAndSet(true)) {
            return (None) new None().setException(new DDWRTTileAutoRefreshNotAllowedException());
          }
          nbRunsLoader++;

          updateProgressBarViewSeparator(0);

          mLastSync = System.currentTimeMillis();

          //TODO Disabled for now
          //                    try {
          //                            //Try loading from cache
          //                        final Gson gson = new GsonBuilder().create();
          //                        final JsonReader reader = new JsonReader(new FileReader(mBandwidthMonitoringData));
          //                        final Map<String, Map<String, Collection<BandwidthMonitoringTile.DataPoint>>> result =
          //                                gson.fromJson(reader, Map.class);
          //                        if (!result.isEmpty()) {
          //                            bandwidthMonitoringIfaceDataMap.clear();
          //                            final Comparator<BandwidthMonitoringTile.DataPoint> comparator = new Comparator<BandwidthMonitoringTile.DataPoint>() {
          //                                @Override
          //                                public int compare(BandwidthMonitoringTile.DataPoint lhs, BandwidthMonitoringTile.DataPoint rhs) {
          //                                    if (lhs == rhs) {
          //                                        return 0;
          //                                    }
          //                                    if (rhs == null) {
          //                                        return -1;
          //                                    }
          //                                    if (lhs == null) {
          //                                        return 1;
          //                                    }
          //                                    return Long.valueOf(rhs.getTimestamp()).compareTo(lhs.getTimestamp());
          //                                }
          //                            };
          //                            for (Map.Entry<String, Map<String, Collection<BandwidthMonitoringTile.DataPoint>>> entry : result.entrySet()) {
          //                                final String key = entry.getKey();
          //                                final Map<String, Collection<BandwidthMonitoringTile.DataPoint>> value = entry.getValue();
          //                                if (key == null || value == null) {
          //                                    continue;
          //                                }
          //                                BandwidthMonitoringTile.BandwidthMonitoringIfaceData bandwidthMonitoringIfaceData =
          //                                        bandwidthMonitoringIfaceDataMap.get(key);
          //                                if (bandwidthMonitoringIfaceData == null) {
          //                                    bandwidthMonitoringIfaceData = new BandwidthMonitoringTile.BandwidthMonitoringIfaceData();
          //                                }
          //                                for (final Map.Entry<String, Collection<BandwidthMonitoringTile.DataPoint>> valueEntry : value.entrySet()) {
          //                                    final String valueEntryKey = \"fake-key\";
          //                                    final Collection valueEntryValue = valueEntry.getValue();
          //                                    //Order datapoints (timestamp ordering)
          //                                    final SortedSet<BandwidthMonitoringTile.DataPoint> dataPoints = new TreeSet<>(comparator);
          //                                    for (final Object datapoint : valueEntryValue) {
          //                                        final JsonObject jsonObject = gson.toJsonTree(datapoint).getAsJsonObject();
          //                                        dataPoints.add(new BandwidthMonitoringTile.DataPoint(jsonObject.get("timestamp").getAsLong(),
          //                                                jsonObject.get("value").getAsDouble()));
          //                                    }
          //                                    for (final BandwidthMonitoringTile.DataPoint dataPoint : dataPoints) {
          //                                        bandwidthMonitoringIfaceData.addData(valueEntryKey, dataPoint);
          //                                    }
          //                                }
          //
          //                                bandwidthMonitoringIfaceDataMap.put(key, bandwidthMonitoringIfaceData);
          //                            }
          //                        }
          //
          //                    } catch (final Exception ignored) {
          //                        //No worries
          //                        ignored.printStackTrace();
          //                    }

          updateProgressBarViewSeparator(10);

          if (mIfacePreviouslySelectedForDisplay.get() == null) {
            mIfacePreviouslySelectedForDisplay.set(
                (mParentFragmentPreferences != null ? mParentFragmentPreferences.getString(
                    getFormattedPrefKey(IFACE_DISPLAYED), null) : null));
          }

          String wanIface = null;
          if (TextUtils.isEmpty(mIfacePreviouslySelectedForDisplay.get())) {
            //Try to get the WAN iface
            try {
              //Start by getting information about the WAN iface name
              final NVRAMInfo nvRamInfoFromRouter =
                  SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity, mRouter,
                      mGlobalPreferences, NVRAMInfo.Companion.getWAN_IFACE());
              updateProgressBarViewSeparator(45);
              if (nvRamInfoFromRouter == null) {
                throw new IllegalStateException("Whoops. WAN Iface could not be determined.");
              }

              wanIface = nvRamInfoFromRouter.getProperty(NVRAMInfo.Companion.getWAN_IFACE());
            } catch (final Exception e) {
              Crashlytics.logException(e);
              //No worries
            }
          }

          //Start by fetching all the possible interfaces monitored
          final String[] allMonitoredIfaces =
              SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                  "awk -F ':' 'NR>2{print $1}' /proc/net/dev 2>/dev/null | sort");
          if (allMonitoredIfaces == null || allMonitoredIfaces.length == 0) {
            throw new DDWRTNoDataException();
          }

          String ifacePreviouslySelectedForDisplay = mIfacePreviouslySelectedForDisplay.get();

          //First try to (quickly) fetch the interface selected details.
          //The other interfaces will follow-up in the background
          for (final String ifaceRaw : allMonitoredIfaces) {
            if (ifaceRaw == null || ifaceRaw.isEmpty()) {
              continue;
            }
            final String iface = ifaceRaw.trim();
            if (iface.isEmpty()) {
              continue;
            }

            if (!TextUtils.isEmpty(ifacePreviouslySelectedForDisplay)) {
              break;
            }

            if (!TextUtils.isEmpty(wanIface)) {
              //Find the corresponding line here
              if (ifacePreviouslySelectedForDisplay == null && iface.equals(wanIface.trim())) {
                mIfacePreviouslySelectedForDisplay.set(iface);
                ifacePreviouslySelectedForDisplay = iface;
                if (mParentFragmentPreferences != null) {
                  mParentFragmentPreferences.edit()
                      .putString(getFormattedPrefKey(IFACE_DISPLAYED),
                          ifacePreviouslySelectedForDisplay)
                      .apply();
                }
              }
            } else {
              //Take the first one
              if (ifacePreviouslySelectedForDisplay == null) {
                ifacePreviouslySelectedForDisplay = iface;
                mIfacePreviouslySelectedForDisplay.set(iface);
                if (mParentFragmentPreferences != null) {
                  mParentFragmentPreferences.edit()
                      .putString(getFormattedPrefKey(IFACE_DISPLAYED),
                          ifacePreviouslySelectedForDisplay)
                      .apply();
                }
              }
            }
          }

          //At this point, mIfacePreviouslySelectedForDisplay is not blank.
          if (fetchBandwidthDataForIface(ifacePreviouslySelectedForDisplay)) {
            //Update title and graph right away
            final String iface = ifacePreviouslySelectedForDisplay;
            mParentFragmentActivity.runOnUiThread(new Runnable() {
              @Override public void run() {
                if (iface.equals(mIfacePreviouslySelectedForDisplay.get())) {
                  //Compare again, just in case user selected something else
                  displayGraphForIface(iface);
                }
              }
            });
          }

          int i = 5;
          int j = 100;
          for (final String ifaceRaw : allMonitoredIfaces) {
            if (ifaceRaw == null || ifaceRaw.isEmpty()) {
              continue;
            }
            final String iface = ifaceRaw.trim();
            if (iface.isEmpty()) {
              continue;
            }

            updateProgressBarViewSeparator(60 + (i++));
            ifacesMenuIds.forcePut(iface, j++);

            if (iface.equals(mIfacePreviouslySelectedForDisplay.get())) {
              continue;
            }

            if (!fetchBandwidthDataForIface(iface)) {
              return null;
            }
          }

          updateProgressBarViewSeparator(90);

          //Save data at each run
          Writer writer = null;
          try {
            final Map<String, Map<String, Collection<BandwidthMonitoringTile.DataPoint>>>
                resultToSave = new HashMap<>();
            for (Map.Entry<String, BandwidthMonitoringTile.BandwidthMonitoringIfaceData> entry : bandwidthMonitoringIfaceDataMap
                .entrySet()) {
              final BandwidthMonitoringTile.BandwidthMonitoringIfaceData value = entry.getValue();
              if (value == null) {
                continue;
              }
              final Map<String, Collection<BandwidthMonitoringTile.DataPoint>> stringListMap =
                  value.toStringListMap();
              if (stringListMap == null || stringListMap.isEmpty()) {
                continue;
              }
              resultToSave.put(entry.getKey(), stringListMap);
            }

            if (!resultToSave.isEmpty()) {
              writer = new FileWriter(mBandwidthMonitoringData, false);
              final Gson gson = new GsonBuilder().create();
              gson.toJson(resultToSave, writer);
            }
          } catch (final Exception ignored) {
            //No worries
            ignored.printStackTrace();
          } finally {
            try {
              if (writer != null) {
                writer.close();
              }
            } catch (final Exception ignored) {
              ignored.printStackTrace();
            }
          }

          return new None();
        } catch (@NonNull final Exception e) {
          e.printStackTrace();
          return (None) new None().setException(e);
        }
      }

      private boolean fetchBandwidthDataForIface(final String iface) throws Exception {
        NVRAMInfo nvramInfo = nvRamInfoFromRouterPerIface.get(iface);
        if (nvramInfo == null) {
          nvRamInfoFromRouterPerIface.put(iface, new NVRAMInfo());
        }
        nvramInfo = nvRamInfoFromRouterPerIface.get(iface);

        BandwidthMonitoringTile.BandwidthMonitoringIfaceData bandwidthMonitoringIfaceData =
            bandwidthMonitoringIfaceDataMap.get(iface);
        if (bandwidthMonitoringIfaceData == null) {
          bandwidthMonitoringIfaceDataMap.put(iface,
              new BandwidthMonitoringTile.BandwidthMonitoringIfaceData());
        }
        bandwidthMonitoringIfaceData = bandwidthMonitoringIfaceDataMap.get(iface);

        @SuppressWarnings("MalformedFormatString") final String[] netDevDataForIface =
            SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                String.format(READ_IFACE_DATA_FROM_PROC_NET_DEV_FMT, iface), "sleep 1",
                String.format(READ_IFACE_DATA_FROM_PROC_NET_DEV_FMT, iface));
        if (netDevDataForIface == null || netDevDataForIface.length < 33) {
          return false;
        }

        final List<String> netDevWanIfaceList = Arrays.asList(netDevDataForIface);

        final long timestamp = System.currentTimeMillis();

        nvramInfo.setProperty(iface + "_rcv_bytes", netDevWanIfaceList.get(0));
        nvramInfo.setProperty(iface + "_rcv_packets", netDevWanIfaceList.get(1));
        nvramInfo.setProperty(iface + "_rcv_errs", netDevWanIfaceList.get(2));
        nvramInfo.setProperty(iface + "_rcv_drop", netDevWanIfaceList.get(3));
        nvramInfo.setProperty(iface + "_rcv_fifo", netDevWanIfaceList.get(4));
        nvramInfo.setProperty(iface + "_rcv_frame", netDevWanIfaceList.get(5));
        nvramInfo.setProperty(iface + "_rcv_compressed", netDevWanIfaceList.get(6));
        nvramInfo.setProperty(iface + "_rcv_multicast", netDevWanIfaceList.get(7));

        nvramInfo.setProperty(iface + "_xmit_bytes", netDevWanIfaceList.get(8));
        nvramInfo.setProperty(iface + "_xmit_packets", netDevWanIfaceList.get(9));
        nvramInfo.setProperty(iface + "_xmit_errs", netDevWanIfaceList.get(10));
        nvramInfo.setProperty(iface + "_xmit_drop", netDevWanIfaceList.get(11));
        nvramInfo.setProperty(iface + "_xmit_fifo", netDevWanIfaceList.get(12));
        nvramInfo.setProperty(iface + "_xmit_colls", netDevWanIfaceList.get(13));
        nvramInfo.setProperty(iface + "_xmit_carrier", netDevWanIfaceList.get(14));
        nvramInfo.setProperty(iface + "_xmit_compressed", netDevWanIfaceList.get(15));

        nvramInfo.setProperty(iface + "_rcv_bytes_t1", netDevWanIfaceList.get(17));
        nvramInfo.setProperty(iface + "_rcv_packets_t1", netDevWanIfaceList.get(18));
        nvramInfo.setProperty(iface + "_rcv_errs_t1", netDevWanIfaceList.get(19));
        nvramInfo.setProperty(iface + "_rcv_drop_t1", netDevWanIfaceList.get(20));
        nvramInfo.setProperty(iface + "_rcv_fifo_t1", netDevWanIfaceList.get(21));
        nvramInfo.setProperty(iface + "_rcv_frame_t1", netDevWanIfaceList.get(22));
        nvramInfo.setProperty(iface + "_rcv_compressed_t1", netDevWanIfaceList.get(23));
        nvramInfo.setProperty(iface + "_rcv_multicast_t1", netDevWanIfaceList.get(24));

        nvramInfo.setProperty(iface + "_xmit_bytes_t1", netDevWanIfaceList.get(25));
        nvramInfo.setProperty(iface + "_xmit_packets_t1", netDevWanIfaceList.get(26));
        nvramInfo.setProperty(iface + "_xmit_errs_t1", netDevWanIfaceList.get(27));
        nvramInfo.setProperty(iface + "_xmit_drop_t1", netDevWanIfaceList.get(28));
        nvramInfo.setProperty(iface + "_xmit_fifo_t1", netDevWanIfaceList.get(29));
        nvramInfo.setProperty(iface + "_xmit_colls_t1", netDevWanIfaceList.get(30));
        nvramInfo.setProperty(iface + "_xmit_carrier_t1", netDevWanIfaceList.get(31));
        nvramInfo.setProperty(iface + "_xmit_compressed_t1", netDevWanIfaceList.get(32));

        //Ingress
        double wanRcvBytes;
        try {
          wanRcvBytes = (Double.parseDouble(nvramInfo.getProperty(iface + "_rcv_bytes_t1", "-255"))
              - Double.parseDouble(nvramInfo.getProperty(iface + "_rcv_bytes", "-1")));
          if (wanRcvBytes >= 0.) {
            bandwidthMonitoringIfaceData.addData("IN",
                new BandwidthMonitoringTile.DataPoint(timestamp, wanRcvBytes));
          }
        } catch (@NonNull final NumberFormatException nfe) {
          return false;
        }
        nvramInfo.setProperty(iface + "_ingress_MB",
            org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(
                Double.valueOf(wanRcvBytes).longValue()) + "ps");

        //Egress
        double wanXmitBytes;
        try {
          wanXmitBytes =
              (Double.parseDouble(nvramInfo.getProperty(iface + "_xmit_bytes_t1", "-255"))
                  - Double.parseDouble(nvramInfo.getProperty(iface + "_xmit_bytes", "-1")));
          if (wanXmitBytes >= 0.) {
            bandwidthMonitoringIfaceData.addData("OUT",
                new BandwidthMonitoringTile.DataPoint(timestamp, wanXmitBytes));
          }
        } catch (@NonNull final NumberFormatException nfe) {
          return false;
        }

        nvramInfo.setProperty(iface + "_egress_MB",
            org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(
                Double.valueOf(wanXmitBytes).longValue()) + "ps");

        return true;
      }
    };
    return mCurrentLoader;
  }

  @Nullable @Override protected String getLogTag() {
    return LOG_TAG;
  }

  @Nullable @Override protected OnClickIntent getOnclickIntent() {
    //TODO
    return null;
  }

  @Override public void onLoadFinished(Loader<None> loader, None data) {
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

      //noinspection ConstantConditions
      if (data == null || bandwidthMonitoringIfaceDataMap.isEmpty()) {
        data = (None) new None().setException(new DDWRTNoDataException("No Data!"));
      }

      final TextView errorPlaceHolderView =
          (TextView) this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_error);

      final Exception exception = data.getException();

      final View legendView =
          this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_graph_legend);

      if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

        if (exception == null) {
          errorPlaceHolderView.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(mIfacePreviouslySelectedForDisplay.get())) {
          displayGraphForIface(mIfacePreviouslySelectedForDisplay.get());
        }

        tileMenu.setOnClickListener(new View.OnClickListener() {
          @Override public void onClick(View v) {
            final PopupMenu popup = new PopupMenu(mParentFragmentActivity, v);

            final MenuInflater inflater = popup.getMenuInflater();
            final Menu menu = popup.getMenu();
            inflater.inflate(R.menu.tile_status_bandwidth_monitoring_options, menu);

            int orderIdx = 10;
            //Sort alphabetically (natural ordering)
            for (final String iface : new TreeSet<>(nvRamInfoFromRouterPerIface.keySet())) {
              final Integer ifaceMenuItemId = ifacesMenuIds.get(iface);
              if (ifaceMenuItemId == null) {
                continue;
              }
              menu.add(R.id.tile_status_bandwidth_iface_selection, ifaceMenuItemId, orderIdx++,
                  iface);
            }

            final MenuItem rtMenuItem = menu.findItem(R.id.tile_status_bandwidth_realtime_graphs);
            if (mParentFragmentPreferences != null) {
              rtMenuItem.setVisible(true);
              rtMenuItem.setEnabled(
                  mParentFragmentPreferences.contains(getFormattedPrefKey(RT_GRAPHS)));
              rtMenuItem.setChecked(
                  mParentFragmentPreferences.getBoolean(getFormattedPrefKey(RT_GRAPHS), false));
            } else {
              rtMenuItem.setVisible(false);
            }

            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
              @Override public boolean onMenuItemClick(MenuItem item) {
                final int itemId = item.getItemId();
                if (itemId == R.id.tile_status_bandwidth_realtime_graphs) {
                  final boolean rtGraphsEnabled = !item.isChecked();
                  if (rtGraphsEnabled) {
                    //Restart loader
                    if (mSupportLoaderManager != null && mCurrentLoader != null) {
                      mSupportLoaderManager.restartLoader(mCurrentLoader.getId(),
                          mFragmentArguments, IfacesBandwidthMonitoringTile.this);
                    }
                  }
                  if (mParentFragmentPreferences != null) {
                    mParentFragmentPreferences.edit()
                        .putBoolean(getFormattedPrefKey(RT_GRAPHS), rtGraphsEnabled)
                        .apply();
                    Utils.requestBackup(mParentFragmentActivity);
                  }
                  return true;
                }
                final String ifaceFromMenuId = ifacesMenuIds.inverse().get(itemId);
                if (!TextUtils.isEmpty(ifaceFromMenuId)) {
                  mIfacePreviouslySelectedForDisplay.set(ifaceFromMenuId);
                  //Save it in preferences
                  if (mParentFragmentPreferences != null) {
                    mParentFragmentPreferences.edit()
                        .putString(getFormattedPrefKey(IFACE_DISPLAYED), ifaceFromMenuId)
                        .apply();
                    Utils.requestBackup(mParentFragmentActivity);
                  }
                  displayGraphForIface(ifaceFromMenuId);
                  return true;
                }

                return false;
              }
            });

            popup.show();
          }
        });

        //Update last sync
        final RelativeTimeTextView lastSyncView =
            (RelativeTimeTextView) layout.findViewById(R.id.tile_last_sync);
        lastSyncView.setReferenceTime(mLastSync);
        lastSyncView.setPrefix("Last sync: ");
      }

      if (exception != null && !(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {
        legendView.setVisibility(View.GONE);
        //noinspection ThrowableResultOfMethodCallIgnored
        final Throwable rootCause = Throwables.getRootCause(exception);
        errorPlaceHolderView.setText(
            "Error: " + (rootCause != null ? rootCause.getMessage() : "null"));
        final Context parentContext = this.mParentFragmentActivity;
        errorPlaceHolderView.setOnClickListener(new View.OnClickListener() {
          @Override public void onClick(final View v) {
            //noinspection ThrowableResultOfMethodCallIgnored
            if (rootCause != null) {
              Toast.makeText(parentContext, rootCause.getMessage(), Toast.LENGTH_LONG).show();
            }
          }
        });
        errorPlaceHolderView.setVisibility(View.VISIBLE);
        updateProgressBarWithError();
      } else if (exception == null) {
        legendView.setVisibility(View.VISIBLE);
        updateProgressBarWithSuccess();
        if (bandwidthMonitoringIfaceDataMap.isEmpty()) {
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

  private void displayGraphForIface(String ifaceStr) {

    if (ifaceStr == null || "".equals(ifaceStr.trim())) {
      return;
    }

    final boolean isThemeLight = ColorUtils.isThemeLight(mParentFragmentActivity);

    ((TextView) this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_title)).setText(
        this.mParentFragmentActivity.getResources().getString(R.string.bandwidth_usage_mb) + (
            !Strings.isNullOrEmpty(ifaceStr) ? (": " + ifaceStr) : ""));

    final NVRAMInfo nvramInfo = nvRamInfoFromRouterPerIface.get(ifaceStr);
    final BandwidthMonitoringTile.BandwidthMonitoringIfaceData bandwidthMonitoringIfaceData =
        bandwidthMonitoringIfaceDataMap.get(ifaceStr);
    if (nvramInfo == null) {
      //TODO Display No Data
      return;
    }

    final View legendView =
        this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_graph_legend);
    legendView.setVisibility(View.VISIBLE);

    final TextView inTextView =
        (TextView) this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_in);
    final TextView outTextView =
        (TextView) this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_out);

    inTextView.setText(nvramInfo.getProperty(ifaceStr + "_ingress_MB", "-").replace("bytes", "B"));
    outTextView.setText(nvramInfo.getProperty(ifaceStr + "_egress_MB", "-").replace("bytes", "B"));

    final LinearLayout graphPlaceHolder = (LinearLayout) this.layout.findViewById(
        R.id.tile_status_bandwidth_monitoring_graph_placeholder);
    final Map<String, EvictingQueue<BandwidthMonitoringTile.DataPoint>> dataCircularBuffer =
        bandwidthMonitoringIfaceData.getData();

    long maxX = System.currentTimeMillis() + 5000;
    long minX = System.currentTimeMillis() - 5000;
    double maxY = 10;
    double minY = 1.;

    final XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
    final XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();

    int i = 0;
    for (final Map.Entry<String, EvictingQueue<BandwidthMonitoringTile.DataPoint>> entry : dataCircularBuffer
        .entrySet()) {
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
      //Fill with a slightly transparent version of the original color
      fill.setColor(android.support.v4.graphics.ColorUtils.setAlphaComponent(colorForIface, 30));
      renderer.addFillOutsideLine(fill);

      if (i == 0) {
        this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_graph_legend_series1_bar)
            .setBackgroundColor(colorForIface);
        final TextView series1TextView = (TextView) this.layout.findViewById(
            R.id.tile_status_bandwidth_monitoring_graph_legend_series1_text);
        series1TextView.setText(iface);
        series1TextView.setTextColor(colorForIface);
      } else if (i == 1) {
        this.layout.findViewById(R.id.tile_status_bandwidth_monitoring_graph_legend_series2_bar)
            .setBackgroundColor(colorForIface);
        final TextView series2TextView = (TextView) this.layout.findViewById(
            R.id.tile_status_bandwidth_monitoring_graph_legend_series2_text);
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
    mRenderer.addYTextLabel(maxY, org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(
        Double.valueOf(maxY).longValue()).replace("bytes", "B") + "ps");
    if (maxY != 0 && maxY / 2 >= 9000) {
      mRenderer.addYTextLabel(maxY / 2,
          org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(
              Double.valueOf(maxY / 2).longValue()).replace("bytes", "B") + "ps");
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
    mRenderer.setYAxisMax(maxY);
    mRenderer.setYAxisMin(minY);
    mRenderer.setXAxisMin(minX);
    mRenderer.setXAxisMax(maxX);
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

    final GraphicalView chartView =
        ChartFactory.getTimeChartView(graphPlaceHolder.getContext(), dataset, mRenderer, null);
    chartView.repaint();

    graphPlaceHolder.addView(chartView, 0);
  }

  private void doneLoading(Loader<None> loader) {
    if (mParentFragmentPreferences != null && mParentFragmentPreferences.getBoolean(
        getFormattedPrefKey(RT_GRAPHS), false)) {
      //Reschedule next run right away, to have a pseudo realtime effect, regardless of the actual sync pref!
      //TODO Check how much extra load that represents on the router
      doneWithLoaderInstance(this, loader, TimeUnit.SECONDS.toMillis(10));
    } else {
      //Use classical sync
      doneWithLoaderInstance(this, loader);
    }
  }
}
