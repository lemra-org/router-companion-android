/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014-2022  Armel Soro
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
package org.rm3l.router_companion.tiles.status.wireless;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.router_companion.tiles.status.wireless.stats.ActiveIPConnectionsStatsAdapter.BY_DESTINATION_COUNTRY;
import static org.rm3l.router_companion.tiles.status.wireless.stats.ActiveIPConnectionsStatsAdapter.BY_DESTINATION_HOSTNAME;
import static org.rm3l.router_companion.tiles.status.wireless.stats.ActiveIPConnectionsStatsAdapter.BY_DESTINATION_IP;
import static org.rm3l.router_companion.tiles.status.wireless.stats.ActiveIPConnectionsStatsAdapter.BY_DESTINATION_ORG;
import static org.rm3l.router_companion.tiles.status.wireless.stats.ActiveIPConnectionsStatsAdapter.BY_DESTINATION_PORT;
import static org.rm3l.router_companion.tiles.status.wireless.stats.ActiveIPConnectionsStatsAdapter.BY_PROTOCOL;
import static org.rm3l.router_companion.tiles.status.wireless.stats.ActiveIPConnectionsStatsAdapter.BY_SOURCE;
import static org.rm3l.router_companion.tiles.status.wireless.stats.ActiveIPConnectionsStatsAdapter.SEPARATOR;
import static org.rm3l.router_companion.utils.Utils.fromHtml;
import static org.rm3l.router_companion.utils.Utils.getEscapedFileName;
import static org.rm3l.router_companion.utils.Utils.nullOrEmptyTo;
import static org.rm3l.router_companion.utils.Utils.truncateText;

import android.Manifest.permission;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.RowSortedTable;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.JsonElement;
import com.squareup.picasso.Callback;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.RouterCompanionApplication;
import org.rm3l.router_companion.api.iana.Data;
import org.rm3l.router_companion.api.iana.Protocol;
import org.rm3l.router_companion.api.iana.Record;
import org.rm3l.router_companion.api.iana.RecordListResponse;
import org.rm3l.router_companion.api.iana.ServiceNamePortNumbersServiceKt;
import org.rm3l.router_companion.api.proxy.NetWhoisInfoProxyApiResponse;
import org.rm3l.router_companion.api.proxy.ProxyData;
import org.rm3l.router_companion.api.proxy.RequestMethod;
import org.rm3l.router_companion.exceptions.DDWRTCompanionException;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.resources.IPConntrack;
import org.rm3l.router_companion.resources.IPWhoisInfo;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.status.wireless.stats.ActiveIPConnectionsStatsAdapter;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.ImageUtils;
import org.rm3l.router_companion.utils.NetworkUtils;
import org.rm3l.router_companion.utils.PermissionsUtils;
import org.rm3l.router_companion.utils.ReportingUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.kotlin.JsonElementUtils;
import org.rm3l.router_companion.utils.kotlin.ViewUtils;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style;
import org.rm3l.router_companion.utils.tuple.Pair;
import org.rm3l.router_companion.widgets.RecyclerViewEmptySupport;
import retrofit2.Response;

public class ActiveIPConnectionsDetailActivity extends AppCompatActivity {

  static class AsyncTaskResult<T> {

    private final Exception exception;

    private final T result;

    AsyncTaskResult(T result, Exception exception) {
      this.result = result;
      this.exception = exception;
    }

    public Exception getException() {
      return exception;
    }

    public T getResult() {
      return result;
    }
  }

  static class BgAsyncTask extends AsyncTask<Void, Void, AsyncTaskResult<?>> {

    final RowSortedTable<Integer, String, Integer> statsTable = TreeBasedTable.create();

    private final ActiveIPConnectionsDetailActivity activeIPConnectionsDetailActivity;

    public BgAsyncTask(final ActiveIPConnectionsDetailActivity activeIPConnectionsDetailActivity) {
      this.activeIPConnectionsDetailActivity = activeIPConnectionsDetailActivity;
    }

    @Override
    protected AsyncTaskResult<?> doInBackground(Void... params) {
      Exception exception = null;
      try {

        // First step : bulk resolve all IP addresses / hosts
        activeIPConnectionsDetailActivity.runOnUiThread(
            new Runnable() {
              @Override
              public void run() {
                activeIPConnectionsDetailActivity.loadingView.setProgress(30);
                activeIPConnectionsDetailActivity.loadingViewText.setText(
                    "Analyzing a total of "
                        + activeIPConnectionsDetailActivity.mActiveIPConnections.size()
                        + " connections...");
              }
            });

        final Set<Pair<Long, Protocol>> serviceNamesToResolve = new HashSet<>();
        final Set<String> toResolve = new HashSet<>();
        for (final IPConntrack ipConntrackRow :
            activeIPConnectionsDetailActivity.mActiveIPConnections) {
          if (ipConntrackRow == null) {
            continue;
          }
          final String sourceAddressOriginalSide = ipConntrackRow.getSourceAddressOriginalSide();
          if (!TextUtils.isEmpty(sourceAddressOriginalSide)
              && mIPWhoisInfoCache.getIfPresent(sourceAddressOriginalSide) == null) {
            toResolve.add(sourceAddressOriginalSide);
          }
          final String sourceAddressReplySide = ipConntrackRow.getSourceAddressReplySide();
          if (!TextUtils.isEmpty(sourceAddressReplySide)
              && mIPWhoisInfoCache.getIfPresent(sourceAddressReplySide) == null) {
            toResolve.add(sourceAddressReplySide);
          }
          final String sourceHostname = ipConntrackRow.getSourceHostname();
          if (!TextUtils.isEmpty(sourceHostname)
              && mIPWhoisInfoCache.getIfPresent(sourceHostname) == null) {
            toResolve.add(sourceHostname);
          }
          final String destinationAddressOriginalSide =
              ipConntrackRow.getDestinationAddressOriginalSide();
          if (!TextUtils.isEmpty(destinationAddressOriginalSide)
              && mIPWhoisInfoCache.getIfPresent(destinationAddressOriginalSide) == null) {
            toResolve.add(destinationAddressOriginalSide);
          }
          final String destinationAddressReplySide =
              ipConntrackRow.getDestinationAddressReplySide();
          if (!TextUtils.isEmpty(destinationAddressReplySide)
              && mIPWhoisInfoCache.getIfPresent(destinationAddressReplySide) == null) {
            toResolve.add(destinationAddressReplySide);
          }
          final String destWhoisOrHostname = ipConntrackRow.getDestWhoisOrHostname();
          if (!TextUtils.isEmpty(destWhoisOrHostname)
              && mIPWhoisInfoCache.getIfPresent(destWhoisOrHostname) == null) {
            toResolve.add(destWhoisOrHostname);
          }
        }

        boolean skipIndividualIPGeoLocationRequests = false;
        try {
          final Response<List<NetWhoisInfoProxyApiResponse>> response =
              NetworkUtils.getProxyService(activeIPConnectionsDetailActivity)
                  .bulkNetworkGeoLocation(new ArrayList<>(toResolve))
                  .execute();
          NetworkUtils.checkResponseSuccessful(response);
          final List<NetWhoisInfoProxyApiResponse> body = response.body();
          if (body != null) {
            for (final NetWhoisInfoProxyApiResponse proxyApiResponse : body) {
              final IPWhoisInfo info;
              if (proxyApiResponse == null || (info = proxyApiResponse.getInfo()) == null) {
                continue;
              }
              mIPWhoisInfoCache.put(proxyApiResponse.getHost(), info);
            }
          }
        } catch (final Exception e) {
          Utils.reportException(this.activeIPConnectionsDetailActivity, e);
          skipIndividualIPGeoLocationRequests = true;
        }

        // Now try to resolve service names and descriptions
        for (final IPConntrack ipConntrackRow :
            activeIPConnectionsDetailActivity.mActiveIPConnections) {
          if (ipConntrackRow == null) {
            continue;
          }
          try {
            final int destinationPortOriginalSideAsInt =
                ipConntrackRow.getDestinationPortOriginalSide();
            final String transportProtocol = ipConntrackRow.getTransportProtocol();
            if (transportProtocol != null) {
              serviceNamesToResolve.add(
                  Pair.create(
                      (long) destinationPortOriginalSideAsInt,
                      Protocol.valueOf(transportProtocol.toUpperCase())));
            }
          } catch (final Exception e) {
            // No worries
            e.printStackTrace();
          }
        }
        if (!serviceNamesToResolve.isEmpty()) {
          final Set<Long> portNumbersToResolve = new HashSet<>();
          final Set<Protocol> protocolsToResolve = new HashSet<>();
          for (final Pair<Long, Protocol> portProtocolPair : serviceNamesToResolve) {
            if (portProtocolPair == null) {
              continue;
            }
            if (portProtocolPair.first != null) {
              portNumbersToResolve.add(portProtocolPair.first);
            }
            if (portProtocolPair.second != null) {
              protocolsToResolve.add(portProtocolPair.second);
            }
          }
          if (BuildConfig.DONATIONS) {
            FirebaseCrashlytics.getInstance()
                .log("Service names / port numbers lookup is a premium feature");
          } else {
            try {
              final Response<RecordListResponse> response =
                  ServiceNamePortNumbersServiceKt.query(
                          NetworkUtils.getServiceNamePortNumbersService(
                              activeIPConnectionsDetailActivity),
                          portNumbersToResolve,
                          protocolsToResolve,
                          null)
                      .execute();
              NetworkUtils.checkResponseSuccessful(response);
              final Data data = response.body().getData();
              final List<Record> records = data.getRecords();
              final Multimap<Pair<Long, Protocol>, Record> responseMultimap =
                  ArrayListMultimap.create();
              for (final Record record : records) {
                if (record == null
                    || record.getPortNumber() == null
                    || record.getTransportProtocol() == null) {
                  continue;
                }
                responseMultimap.put(
                    Pair.create(record.getPortNumber(), record.getTransportProtocol()), record);
              }
              for (final Entry<Pair<Long, Protocol>, Collection<Record>> pairCollectionEntry :
                  responseMultimap.asMap().entrySet()) {
                SERVICE_NAMES_PORT_NUMBERS_CACHE.put(
                    pairCollectionEntry.getKey(), pairCollectionEntry.getValue());
              }
            } catch (final Exception e) {
              // No worries
              ReportingUtils.reportException(null, e);
            }
          }
        }

        final int totalConnectionsCount =
            activeIPConnectionsDetailActivity.mActiveIPConnections.size();
        int index = 1;
        String existingRecord;

        for (final IPConntrack ipConntrackRow :
            activeIPConnectionsDetailActivity.mActiveIPConnections) {
          if (ipConntrackRow == null) {
            continue;
          }
          // total=200,
          // i=100 => 50% = (1- (200 - 100)/200)
          // i=0 => (1-(200-0)/200) = 0%
          // i=50 => (1- (200 - 50)/200) => 25%
          //
          // total = 50
          // i = 25 => 25/50 => 50%
          // i = 50 => 50/50 = 1%
          final int currentIdx = (index++);
          final int progress =
              35
                  + Double.valueOf(
                          100
                              * (totalConnectionsCount > 100
                                  ? (1
                                      - ((double) (totalConnectionsCount - currentIdx)
                                          / (double) totalConnectionsCount))
                                  : ((double) currentIdx / (double) totalConnectionsCount)))
                      .intValue();
          FirebaseCrashlytics.getInstance()
              .log(
                  String.format(
                      "<currentIdx=%d , totalConnectionsCount=%d , progress=%d%%>",
                      currentIdx, totalConnectionsCount, progress));
          activeIPConnectionsDetailActivity.runOnUiThread(
              new Runnable() {
                @Override
                public void run() {
                  activeIPConnectionsDetailActivity.loadingView.setProgress(progress);
                  activeIPConnectionsDetailActivity.loadingViewText.setText(
                      String.format(
                          "Analysing IP Connection (%d / %d)...",
                          currentIdx, totalConnectionsCount));
                }
              });
          final String sourceAddressOriginalSide = ipConntrackRow.getSourceAddressOriginalSide();
          existingRecord =
              activeIPConnectionsDetailActivity.ipToHostResolvedMap.get(sourceAddressOriginalSide);
          if (isNullOrEmpty(existingRecord)) {
            // Set Source IP HostName
            final String srcIpHostnameResolved;
            if (activeIPConnectionsDetailActivity.mLocalIpToHostname == null) {
              srcIpHostnameResolved = "-";
            } else {
              final String val =
                  activeIPConnectionsDetailActivity.mLocalIpToHostname.get(
                      sourceAddressOriginalSide);
              if (isNullOrEmpty(val)) {
                srcIpHostnameResolved = "-";
              } else {
                srcIpHostnameResolved = val;
              }
            }
            activeIPConnectionsDetailActivity.ipToHostResolvedMap.put(
                sourceAddressOriginalSide, srcIpHostnameResolved);
          }

          final String destinationAddressOriginalSide =
              ipConntrackRow.getDestinationAddressOriginalSide();
          existingRecord =
              activeIPConnectionsDetailActivity.ipToHostResolvedMap.get(
                  destinationAddressOriginalSide);
          if (isNullOrEmpty(existingRecord)) {
            final String dstIpWhoisResolved;
            if (isNullOrEmpty(destinationAddressOriginalSide)) {
              dstIpWhoisResolved = "-";
            } else {
              IPWhoisInfo ipWhoisInfo = null;
              try {
                if (!skipIndividualIPGeoLocationRequests) {
                  ipWhoisInfo = mIPWhoisInfoCache.get(destinationAddressOriginalSide);
                }
              } catch (Exception e) {
                e.printStackTrace();
                Utils.reportException(null, e);
              }
              if (ipWhoisInfo != null) {
                final String country = ipWhoisInfo.getCountry();
                if (!isNullOrEmpty(country)) {
                  activeIPConnectionsDetailActivity.mDestinationIpToCountry.put(
                      destinationAddressOriginalSide, country);
                  Integer countryStats = statsTable.get(BY_DESTINATION_COUNTRY, country);
                  if (countryStats == null) {
                    countryStats = 0;
                  }
                  statsTable.put(BY_DESTINATION_COUNTRY, country, countryStats + 1);
                }

                final String hostname = ipWhoisInfo.getHostname();
                if (!isNullOrEmpty(hostname)) {
                  Integer hostnameStats = statsTable.get(BY_DESTINATION_HOSTNAME, hostname);
                  if (hostnameStats == null) {
                    hostnameStats = 0;
                  }
                  statsTable.put(BY_DESTINATION_HOSTNAME, hostname, hostnameStats + 1);
                }
              }
              final String org;
              if (ipWhoisInfo == null
                  || (org = ipWhoisInfo.getOrganization()) == null
                  || org.isEmpty()) {
                dstIpWhoisResolved = "-";
              } else {
                dstIpWhoisResolved = org;
                if (!activeIPConnectionsDetailActivity.mLocalIpToHostname.containsKey(
                    destinationAddressOriginalSide)) {
                  activeIPConnectionsDetailActivity.mLocalIpToHostname.put(
                      destinationAddressOriginalSide, org);
                }
                Integer orgStats = statsTable.get(BY_DESTINATION_ORG, org);
                if (orgStats == null) {
                  orgStats = 0;
                }
                statsTable.put(BY_DESTINATION_ORG, org, orgStats + 1);
              }
            }
            activeIPConnectionsDetailActivity.ipToHostResolvedMap.put(
                destinationAddressOriginalSide, dstIpWhoisResolved);
          }

          activeIPConnectionsDetailActivity.runOnUiThread(
              new Runnable() {
                @Override
                public void run() {
                  activeIPConnectionsDetailActivity.loadingView.setProgress(progress);
                  activeIPConnectionsDetailActivity.loadingViewText.setText("Computing stats...");
                }
              });

          final String transportProtocol = ipConntrackRow.getTransportProtocol();
          if (transportProtocol != null) {
            Integer protoStats = statsTable.get(BY_PROTOCOL, transportProtocol);
            if (protoStats == null) {
              protoStats = 0;
            }
            statsTable.put(BY_PROTOCOL, transportProtocol, protoStats + 1);
          }

          final int destinationPortOriginalSideAsInt =
              ipConntrackRow.getDestinationPortOriginalSide();
          final String destinationPortOriginalSide =
              Integer.toString(destinationPortOriginalSideAsInt);
          Record record = null;
          try {
            final Collection<Record> records =
                SERVICE_NAMES_PORT_NUMBERS_CACHE.get(
                    Pair.create(
                        (long) destinationPortOriginalSideAsInt,
                        Protocol.valueOf(transportProtocol.toUpperCase())));
            if (records != null && !records.isEmpty()) {
              record = records.iterator().next();
            }
          } catch (final Exception e) {
            e.printStackTrace();
          }
          final String keyInTablePort;
          if (record == null || Strings.isNullOrEmpty(record.getServiceName())) {
            keyInTablePort = destinationPortOriginalSide;
          } else {
            keyInTablePort = (destinationPortOriginalSide + " (" + record.getServiceName() + ")");
          }
          Integer destPortStats = statsTable.get(BY_DESTINATION_PORT, keyInTablePort);
          if (destPortStats == null) {
            destPortStats = 0;
          }
          statsTable.put(BY_DESTINATION_PORT, keyInTablePort, destPortStats + 1);

          final String sourceInStats =
              String.format(
                  "%s%s%s",
                  activeIPConnectionsDetailActivity.ipToHostResolvedMap.get(
                      sourceAddressOriginalSide),
                  SEPARATOR,
                  sourceAddressOriginalSide);
          Integer sourceStats = statsTable.get(BY_SOURCE, sourceInStats);
          if (sourceStats == null) {
            sourceStats = 0;
          }
          statsTable.put(BY_SOURCE, sourceInStats, sourceStats + 1);

          final String destinationInStats =
              String.format(
                  "%s%s%s",
                  destinationAddressOriginalSide,
                  SEPARATOR,
                  activeIPConnectionsDetailActivity.ipToHostResolvedMap.get(
                      destinationAddressOriginalSide));
          Integer destinationStats = statsTable.get(BY_DESTINATION_IP, destinationInStats);
          if (destinationStats == null) {
            destinationStats = 0;
          }
          statsTable.put(BY_DESTINATION_IP, destinationInStats, destinationStats + 1);
        }

      } catch (final Exception e) {
        exception = e;
      }
      return new AsyncTaskResult<>(null, exception);
    }

    @Override
    protected void onPostExecute(AsyncTaskResult<?> result) {
      super.onPostExecute(result);
      Exception exception = null;
      if (result == null || (exception = result.getException()) != null) {
        // Error - hide stats sliding layout
        FirebaseCrashlytics.getInstance()
            .log(
                "Error: "
                    + (exception != null ? exception.getMessage() : "No data or Result is NULL"));
        if (exception != null) {
          exception.printStackTrace();
        }
        activeIPConnectionsDetailActivity.slidingUpPanel.setVisibility(View.GONE);
      } else {
        if (activeIPConnectionsDetailActivity.optionsMenu != null) {
          activeIPConnectionsDetailActivity
              .optionsMenu
              .findItem(R.id.tile_status_active_ip_connections_search)
              .setVisible(true);
          activeIPConnectionsDetailActivity
              .optionsMenu
              .findItem(R.id.tile_status_active_ip_connections_share)
              .setVisible(true);
        }
        // No error
        activeIPConnectionsDetailActivity.slidingUpPanel.setVisibility(View.VISIBLE);
        activeIPConnectionsDetailActivity.loadingView.setVisibility(View.GONE);
        activeIPConnectionsDetailActivity.loadingViewText.setVisibility(View.GONE);

        ((ActiveIPConnectionsDetailRecyclerViewAdapter) activeIPConnectionsDetailActivity.mAdapter)
            .setActiveIPConnections(activeIPConnectionsDetailActivity.mActiveIPConnections);
        activeIPConnectionsDetailActivity.mAdapter.notifyDataSetChanged();

        activeIPConnectionsDetailActivity.contentView.setVisibility(View.VISIBLE);

        ((ActiveIPConnectionsStatsAdapter) activeIPConnectionsDetailActivity.mStatsAdapter)
            .setStatsTable(statsTable);
        activeIPConnectionsDetailActivity.mStatsAdapter.notifyDataSetChanged();

        activeIPConnectionsDetailActivity.slidingUpPanel.setVisibility(View.VISIBLE);

        activeIPConnectionsDetailActivity.slidingUpPanelLoading.setVisibility(View.GONE);
        activeIPConnectionsDetailActivity.slidingUpPanelStatsTitle.setText(
            "Stats (Connections Count)");
      }
    }
  }

  class ActiveIPConnectionsDetailRecyclerViewAdapter
      extends RecyclerView.Adapter<ActiveIPConnectionsDetailRecyclerViewAdapter.ViewHolder>
      implements Filterable {

    class ViewHolder extends RecyclerView.ViewHolder {

      final CardView cardView;

      final ImageButton expandCollapseButton;

      final View itemView;

      final Context mContext;

      public ViewHolder(Context context, View itemView) {
        super(itemView);
        this.mContext = context;
        this.itemView = itemView;
        this.cardView = itemView.findViewById(R.id.activity_ip_connections_card_view);
        this.expandCollapseButton = cardView.findViewById(R.id.expand_collapse);
      }
    }

    private final ActiveIPConnectionsDetailActivity activity;

    private List<IPConntrack> mActiveIPConnections;

    private final int mAsyncLoaderId;

    private final Filter mFilter;

    private final LoaderManager supportLoaderManager;

    ActiveIPConnectionsDetailRecyclerViewAdapter(ActiveIPConnectionsDetailActivity activity) {
      this.activity = activity;
      this.supportLoaderManager = getSupportLoaderManager();
      this.mAsyncLoaderId = Long.valueOf(Utils.getNextLoaderId()).intValue();
      this.mFilter =
          new Filter() {
            @Override
            protected FilterResults performFiltering(final CharSequence constraint) {
              final FilterResults oReturn = new FilterResults();
              if (mActiveIPConnections == null || mActiveIPConnections.isEmpty()) {
                return oReturn;
              }
              if (TextUtils.isEmpty(constraint)) {
                oReturn.values = mActiveIPConnections;
              } else {
                // Filter list
                oReturn.values =
                    FluentIterable.from(mActiveIPConnections)
                        .filter(
                            new Predicate<IPConntrack>() {
                              @Override
                              public boolean apply(IPConntrack input) {
                                if (input == null) {
                                  return false;
                                }
                                // Filter on visible fields: source IP/port, dest. IP/port,
                                // transport protocol and TCP State, device name, dest. WHOIS
                                final String constraintLowerCase =
                                    constraint.toString().toLowerCase();
                                return (input.getSourceAddressOriginalSide() != null
                                        && input
                                            .getSourceAddressOriginalSide()
                                            .toLowerCase()
                                            .contains(constraintLowerCase))
                                    || Integer.toString(input.getSourcePortOriginalSide())
                                        .contains(constraintLowerCase)
                                    || (input.getDestinationAddressOriginalSide() != null
                                        && input
                                            .getDestinationAddressOriginalSide()
                                            .toLowerCase()
                                            .contains(constraintLowerCase))
                                    || Integer.toString(input.getDestinationPortOriginalSide())
                                        .contains(constraintLowerCase)
                                    || (input.getTransportProtocol() != null
                                        && input
                                            .getTransportProtocol()
                                            .toLowerCase()
                                            .contains(constraintLowerCase))
                                    || (input.getTcpConnectionState() != null
                                        && input
                                            .getTcpConnectionState()
                                            .toLowerCase()
                                            .contains(constraintLowerCase))
                                    || (input.getSourceHostname() != null
                                        && input
                                            .getSourceHostname()
                                            .toLowerCase()
                                            .contains(constraintLowerCase))
                                    || (input.getDestWhoisOrHostname() != null
                                        && input
                                            .getDestWhoisOrHostname()
                                            .toLowerCase()
                                            .contains(constraintLowerCase));
                              }
                            })
                        .toList();
              }
              return oReturn;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
              final Object values = results.values;
              if (values instanceof List) {
                //noinspection unchecked
                setActiveIPConnections((List<IPConntrack>) values);
                notifyDataSetChanged();
              }
            }
          };
    }

    @Override
    public Filter getFilter() {
      return mFilter;
    }

    @Override
    public int getItemCount() {
      return mActiveIPConnections != null ? mActiveIPConnections.size() : 0;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
      if (position < 0 || position >= mActiveIPConnections.size()) {
        Utils.reportException(null, new IllegalStateException());
        Toast.makeText(activity, "Internal Error. Please try again later", Toast.LENGTH_SHORT)
            .show();
        return;
      }
      final CardView cardView = holder.cardView;
      final IPConntrack ipConntrackRow = mActiveIPConnections.get(position);
      if (ipConntrackRow == null) {
        FirebaseCrashlytics.getInstance().log("Invalid active IP Connection @ " + position);
        cardView.setVisibility(View.GONE);
        return;
      }

      final boolean isThemeLight = ColorUtils.Companion.isThemeLight(activity);

      cardView.setVisibility(View.VISIBLE);

      // Add padding to CardView on v20 and before to prevent intersections between the Card content
      // and rounded corners.
      cardView.setPreventCornerOverlap(true);
      // Add padding in API v21+ as well to have the same measurements with previous versions.
      cardView.setUseCompatPadding(true);

      final LinearLayout detailsPlaceholderView =
          cardView.findViewById(R.id.activity_ip_connections_details_placeholder);
      final boolean detailsPlaceholderVisible =
          (detailsPlaceholderView.getVisibility() == View.VISIBLE);

      if (isThemeLight) {
        // Light
        cardView.setCardBackgroundColor(
            ContextCompat.getColor(activity, R.color.cardview_light_background));
        holder.expandCollapseButton.setImageResource(
            detailsPlaceholderVisible
                ? R.drawable.ic_expand_less_black_24dp
                : R.drawable.ic_expand_more_black_24dp);
      } else {
        // Default is Dark
        cardView.setCardBackgroundColor(
            ContextCompat.getColor(activity, R.color.cardview_dark_background));
        holder.expandCollapseButton.setImageResource(
            detailsPlaceholderVisible
                ? R.drawable.ic_expand_less_white_24dp
                : R.drawable.ic_expand_more_white_24dp);
      }

      // Highlight CardView
      //                    cardView.setCardElevation(20f);

      final String sourceAddressOriginalSide = ipConntrackRow.getSourceAddressOriginalSide();
      ((TextView) cardView.findViewById(R.id.activity_ip_connections_device_source_ip))
          .setText(sourceAddressOriginalSide);
      ((TextView) cardView.findViewById(R.id.activity_ip_connections_details_source_ip))
          .setText(sourceAddressOriginalSide);

      final long ttl = ipConntrackRow.getTimeout();
      ((TextView) cardView.findViewById(R.id.activity_ip_connections_details_ttl))
          .setText(ttl > 0 ? String.valueOf(ttl) : "-");

      final long packets = ipConntrackRow.getPackets();
      ((TextView) cardView.findViewById(R.id.activity_ip_connections_details_packets))
          .setText(packets > 0 ? String.valueOf(packets) : "-");

      final long bytes = ipConntrackRow.getBytes();
      ((TextView) cardView.findViewById(R.id.activity_ip_connections_details_bytes))
          .setText(bytes > 0 ? String.valueOf(bytes) : "-");

      final String destinationAddressOriginalSide =
          ipConntrackRow.getDestinationAddressOriginalSide();
      ((TextView) cardView.findViewById(R.id.activity_ip_connections_device_dest_ip))
          .setText(destinationAddressOriginalSide);
      ((TextView) cardView.findViewById(R.id.activity_ip_connections_details_destination_ip))
          .setText(destinationAddressOriginalSide);

      final String protocol = ipConntrackRow.getTransportProtocol();
      final TextView proto = cardView.findViewById(R.id.activity_ip_connections_device_proto);
      proto.setText(isNullOrEmpty(protocol) ? "-" : protocol.toUpperCase());
      ((TextView) cardView.findViewById(R.id.activity_ip_connections_details_protocol))
          .setText(isNullOrEmpty(protocol) ? "-" : protocol.toUpperCase());

      final String tcpConnectionState = ipConntrackRow.getTcpConnectionState();
      final TextView tcpConnectionStateView =
          cardView.findViewById(R.id.activity_ip_connections_tcp_connection_state);
      final TextView tcpConnectionStateDetailedView =
          cardView.findViewById(R.id.activity_ip_connections_details_tcp_connection_state);

      if (!isNullOrEmpty(tcpConnectionState)) {
        tcpConnectionStateView.setText(tcpConnectionState);
        tcpConnectionStateDetailedView.setText(tcpConnectionState);
        tcpConnectionStateView.setVisibility(View.VISIBLE);
        Integer detailsResId = null;
        try {
          detailsResId =
              Utils.getResId(
                  String.format(
                      "tcp_connection_state_details_%s",
                      tcpConnectionState.replaceAll("-", "_").replaceAll(" ", "_")),
                  R.string.class);
        } catch (final Exception e) {
          FirebaseCrashlytics.getInstance()
              .log(
                  "No resource ID found in string.xml for TCP Connection State: "
                      + tcpConnectionState);
          ReportingUtils.reportException(getApplicationContext(), e);
        }
        final Integer tcpConnectionDetailsResourceId = detailsResId;
        final Function1<View, Unit> onClickFunction =
            view -> {
              Utils.buildAlertDialog(
                      ActiveIPConnectionsDetailActivity.this,
                      tcpConnectionState,
                      tcpConnectionDetailsResourceId != null
                          ? getResources().getString(tcpConnectionDetailsResourceId)
                          : tcpConnectionState,
                      true,
                      true)
                  .show();
              return null;
            };
        ViewUtils.setClickable(tcpConnectionStateView, onClickFunction);
        //                ViewUtils.setClickable(tcpConnectionStateDetailedView, onClickFunction);
      } else {
        tcpConnectionStateView.setVisibility(View.GONE);
        tcpConnectionStateDetailedView.setText("-");
        tcpConnectionStateView.setOnClickListener(null);
      }

      final View tcpConnectionStateDetailedViewTitle =
          cardView.findViewById(R.id.activity_ip_connections_details_tcp_connection_state_title);
      if (protocol != null && "TCP".equalsIgnoreCase(protocol.trim())) {
        tcpConnectionStateDetailedView.setVisibility(View.VISIBLE);
        tcpConnectionStateDetailedViewTitle.setVisibility(View.VISIBLE);
      } else {
        tcpConnectionStateDetailedView.setVisibility(View.GONE);
        tcpConnectionStateDetailedViewTitle.setVisibility(View.GONE);
      }

      // ICMP
      // ID
      final View icmpIdTitle =
          cardView.findViewById(R.id.activity_ip_connections_details_icmp_id_title);
      final TextView icmpId = cardView.findViewById(R.id.activity_ip_connections_details_icmp_id);
      // Type
      final View icmpTypeTitle =
          cardView.findViewById(R.id.activity_ip_connections_details_icmp_type_title);
      final TextView icmpType =
          cardView.findViewById(R.id.activity_ip_connections_details_icmp_type);
      // Code
      final View icmpCodeTitle =
          cardView.findViewById(R.id.activity_ip_connections_details_icmp_code_title);
      final TextView icmpCode =
          cardView.findViewById(R.id.activity_ip_connections_details_icmp_code);
      // Ctrl Message
      final View icmpCtrlMsgTitle =
          cardView.findViewById(R.id.activity_ip_connections_details_icmp_ctrl_msg_title);
      final TextView icmpCtrlMsg =
          cardView.findViewById(R.id.activity_ip_connections_details_icmp_ctrl_msg);

      final View[] icmpViews =
          new View[] {
            icmpIdTitle,
            icmpId,
            icmpTypeTitle,
            icmpType,
            icmpCodeTitle,
            icmpCode,
            icmpCtrlMsgTitle,
            icmpCtrlMsg
          };

      if ("ICMP".equalsIgnoreCase(protocol)) {
        final int ipConntrackRowIcmpType = ipConntrackRow.getIcmpType();
        final int ipConntrackRowIcmpCode = ipConntrackRow.getIcmpCode();
        icmpId.setText(
            isNullOrEmpty(ipConntrackRow.getIcmpId()) ? "-" : ipConntrackRow.getIcmpId());
        icmpType.setText(
            (ipConntrackRowIcmpType < 0) ? "-" : String.valueOf(ipConntrackRowIcmpType));
        icmpCode.setText(
            (ipConntrackRowIcmpCode < 0) ? "-" : String.valueOf(ipConntrackRowIcmpCode));

        final String ctrlMsg =
            ICMP_TYPE_CODE_DESCRIPTION_TABLE.get(ipConntrackRowIcmpType, ipConntrackRowIcmpCode);
        icmpCtrlMsg.setText(isNullOrEmpty(ctrlMsg) ? "-" : ctrlMsg);

        for (final View icmpView : icmpViews) {
          icmpView.setVisibility(View.VISIBLE);
        }
      } else {
        for (final View icmpView : icmpViews) {
          icmpView.setVisibility(View.GONE);
        }
      }

      final int sourcePortOriginalSide = ipConntrackRow.getSourcePortOriginalSide();
      final String srcPortToDisplay =
          sourcePortOriginalSide > 0 ? String.valueOf(sourcePortOriginalSide) : "-";
      ((TextView) cardView.findViewById(R.id.activity_ip_connections_sport))
          .setText(srcPortToDisplay);
      ((TextView) cardView.findViewById(R.id.activity_ip_connections_details_source_port))
          .setText(srcPortToDisplay);

      final int destinationPortOriginalSide = ipConntrackRow.getDestinationPortOriginalSide();
      final String dstPortToDisplay =
          destinationPortOriginalSide > 0 ? String.valueOf(destinationPortOriginalSide) : "-";
      ((TextView) cardView.findViewById(R.id.activity_ip_connections_dport))
          .setText(dstPortToDisplay);
      ((TextView) cardView.findViewById(R.id.activity_ip_connections_details_destination_port))
          .setText(dstPortToDisplay);

      final TextView rawLineView = cardView.findViewById(R.id.activity_ip_connections_raw_line);
      rawLineView.setText(ipConntrackRow.getRawLine());

      holder.expandCollapseButton.setOnClickListener(
          new OnClickListener() {
            @Override
            public void onClick(final View v) {
              cardView.performClick();
            }
          });

      cardView.setOnClickListener(
          new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              if (detailsPlaceholderView.getVisibility() == View.VISIBLE) {
                ViewUtils.collapse(detailsPlaceholderView, holder.expandCollapseButton);
              } else {
                ViewUtils.expand(detailsPlaceholderView, holder.expandCollapseButton);
              }
            }
          });

      final ImageView destCountryFlag =
          cardView.findViewById(R.id.activity_ip_connections_destination_country_flag);

      // Fetch IP Whois info
      supportLoaderManager
          .initLoader(
              Long.valueOf(Utils.getNextLoaderId()).intValue(),
              null,
              new LoaderCallbacks<Void>() {

                @Override
                public Loader<Void> onCreateLoader(int id, Bundle args) {
                  final AsyncTaskLoader<Void> asyncTaskLoader =
                      new AsyncTaskLoader<Void>(ActiveIPConnectionsDetailActivity.this) {

                        @Override
                        public Void loadInBackground() {
                          try {
                            if (destinationPortOriginalSide > 0) {
                              final Collection<Record> records =
                                  SERVICE_NAMES_PORT_NUMBERS_CACHE.get(
                                      Pair.create(
                                          (long) destinationPortOriginalSide,
                                          Protocol.valueOf(protocol.toUpperCase())));
                              if (records != null && !records.isEmpty()) {
                                final Record record = records.iterator().next();
                                if (record != null) {
                                  runOnUiThread(
                                      new Runnable() {
                                        @Override
                                        public void run() {
                                          final int destinationPortOriginalSide =
                                              ipConntrackRow.getDestinationPortOriginalSide();
                                          final String dstPortToDisplay;
                                          if (destinationPortOriginalSide < 0) {
                                            dstPortToDisplay = "-";
                                          } else if (destinationPortOriginalSide == 0) {
                                            dstPortToDisplay = "0";
                                          } else {
                                            if (Strings.isNullOrEmpty(record.getServiceName())) {
                                              dstPortToDisplay =
                                                  String.valueOf(destinationPortOriginalSide);
                                              ((TextView)
                                                      cardView.findViewById(
                                                          R.id
                                                              .activity_ip_connections_details_destination_service_name))
                                                  .setText("-");
                                            } else {
                                              dstPortToDisplay =
                                                  (destinationPortOriginalSide
                                                      + " ("
                                                      + record.getServiceName()
                                                      + ")");
                                              ((TextView)
                                                      cardView.findViewById(
                                                          R.id
                                                              .activity_ip_connections_details_destination_service_name))
                                                  .setText(record.getServiceName());
                                            }
                                          }
                                          ((TextView)
                                                  cardView.findViewById(
                                                      R.id.activity_ip_connections_dport))
                                              .setText(dstPortToDisplay);
                                          ((TextView)
                                                  cardView.findViewById(
                                                      R.id
                                                          .activity_ip_connections_details_destination_service_description))
                                              .setText(
                                                  Strings.isNullOrEmpty(record.getDescription())
                                                      ? "-"
                                                      : record.getDescription());
                                        }
                                      });
                                }
                              }
                            }
                          } catch (final Exception e) {
                            e.printStackTrace();
                            // No worries
                          }

                          try {
                            final IPWhoisInfo whoisInfo;
                            if (destinationAddressOriginalSide == null
                                || (whoisInfo =
                                        mIPWhoisInfoCache.get(destinationAddressOriginalSide))
                                    == null) {
                              runOnUiThread(
                                  new Runnable() {
                                    @Override
                                    public void run() {
                                      destCountryFlag.setVisibility(View.GONE);
                                    }
                                  });
                            } else {
                              final String countryCode = whoisInfo.getCountry_code();
                              runOnUiThread(
                                  new Runnable() {
                                    @Override
                                    public void run() {
                                      final String hostname = whoisInfo.getHostname();
                                      final TextView destIpGlanceView =
                                          cardView.findViewById(
                                              R.id.activity_ip_connections_device_dest_ip);
                                      final boolean hostnameDisplayed =
                                          (hostname != null
                                              && !hostname.isEmpty()
                                              && !hostname.equalsIgnoreCase(
                                                  destinationAddressOriginalSide));
                                      if (hostnameDisplayed) {
                                        destIpGlanceView.setOnClickListener(
                                            new OnClickListener() {
                                              @Override
                                              public void onClick(final View v) {
                                                Toast.makeText(
                                                        activity, hostname, Toast.LENGTH_SHORT)
                                                    .show();
                                              }
                                            });
                                      }
                                      destIpGlanceView.setText(
                                          hostnameDisplayed
                                              ? String.format(
                                                  "%s\n(%s)",
                                                  nullOrEmptyTo(
                                                      // Truncate to the length of a complete IP
                                                      // address
                                                      truncateText(
                                                          hostname, "255.255.255.255".length()),
                                                      "-"),
                                                  destinationAddressOriginalSide)
                                              : destinationAddressOriginalSide);
                                      final TextView destIpHostDetail =
                                          cardView.findViewById(
                                              R.id
                                                  .activity_ip_connections_details_destination_ip_host);
                                      destIpHostDetail.setText(hostnameDisplayed ? hostname : "-");
                                      destIpHostDetail.setVisibility(
                                          hostnameDisplayed ? View.VISIBLE : View.GONE);
                                      cardView
                                          .findViewById(
                                              R.id
                                                  .activity_ip_connections_details_destination_ip_host_title)
                                          .setVisibility(
                                              hostnameDisplayed ? View.VISIBLE : View.GONE);

                                      final String country = whoisInfo.getCountry();
                                      ((TextView)
                                              cardView.findViewById(
                                                  R.id
                                                      .activity_ip_connections_details_destination_whois_country))
                                          .setText(
                                              (country != null && countryCode != null)
                                                  ? String.format("%s (%s)", country, countryCode)
                                                  : "-");
                                      ((TextView)
                                              cardView.findViewById(
                                                  R.id
                                                      .activity_ip_connections_details_destination_whois_region))
                                          .setText(nullOrEmptyTo(whoisInfo.getRegion(), "-"));
                                      ((TextView)
                                              cardView.findViewById(
                                                  R.id
                                                      .activity_ip_connections_details_destination_whois_city))
                                          .setText(nullOrEmptyTo(whoisInfo.getCity(), "-"));
                                    }
                                  });

                              if (isNullOrEmpty(countryCode)) {
                                runOnUiThread(
                                    new Runnable() {
                                      @Override
                                      public void run() {
                                        destCountryFlag.setVisibility(View.GONE);
                                      }
                                    });
                              } else {
                                runOnUiThread(
                                    new Runnable() {
                                      @Override
                                      public void run() {
                                        ImageUtils.downloadImageFromUrl(
                                            ActiveIPConnectionsDetailActivity.this,
                                            String.format(
                                                "%s/%s.png",
                                                RouterCompanionAppConstants.COUNTRY_API_SERVER_FLAG,
                                                countryCode),
                                            destCountryFlag,
                                            null,
                                            null,
                                            new Callback() {
                                              @Override
                                              public void onError(Exception e) {
                                                destCountryFlag.setVisibility(View.GONE);
                                              }

                                              @Override
                                              public void onSuccess() {
                                                destCountryFlag.setVisibility(View.VISIBLE);
                                              }
                                            });
                                      }
                                    });
                              }
                            }
                          } catch (final Exception e) {
                            e.printStackTrace();
                            runOnUiThread(
                                new Runnable() {
                                  @Override
                                  public void run() {
                                    destCountryFlag.setVisibility(View.GONE);
                                  }
                                });
                            // No worries
                          }
                          return null;
                        }
                      };
                  asyncTaskLoader.forceLoad();
                  return asyncTaskLoader;
                }

                @Override
                public void onLoadFinished(Loader<Void> loader, Void data) {}

                @Override
                public void onLoaderReset(Loader<Void> loader) {}
              })
          .forceLoad();

      final TextView srcIpHostname =
          cardView.findViewById(R.id.activity_ip_connections_source_ip_hostname);
      final TextView srcIpHostnameDetails =
          cardView.findViewById(R.id.activity_ip_connections_details_source_host);
      final ProgressBar srcIpHostnameLoading =
          cardView.findViewById(R.id.activity_ip_connections_source_ip_hostname_loading);
      // Set Source IP HostName
      if (ipToHostResolvedMap == null) {
        srcIpHostname.setText("");
        srcIpHostnameDetails.setText("-");
        srcIpHostname.setOnClickListener(null);
      } else {
        final String srcIpHostnameResolved = ipToHostResolvedMap.get(sourceAddressOriginalSide);
        srcIpHostname.setText(
            isNullOrEmpty(srcIpHostnameResolved)
                ? ""
                : Utils.truncateText(srcIpHostnameResolved, 30));
        srcIpHostname.setOnClickListener(
            new OnClickListener() {
              @Override
              public void onClick(final View v) {
                Toast.makeText(activity, srcIpHostnameResolved, Toast.LENGTH_SHORT).show();
              }
            });
        srcIpHostnameDetails.setText(
            isNullOrEmpty(srcIpHostnameResolved) ? "-" : srcIpHostnameResolved);
        ipConntrackRow.setSourceHostname(srcIpHostnameResolved);
      }
      srcIpHostname.setVisibility(View.VISIBLE);
      srcIpHostnameLoading.setVisibility(View.GONE);

      // ... and Destination IP Address Organization (if available)
      final TextView destIpOrg = cardView.findViewById(R.id.activity_ip_connections_dest_ip_org);
      final TextView destIpOrgDetails =
          cardView.findViewById(R.id.activity_ip_connections_details_destination_whois);
      final ProgressBar destIpOrgLoading =
          cardView.findViewById(R.id.activity_ip_connections_dest_ip_org_loading);
      if (ipToHostResolvedMap == null) {
        new Handler()
            .post(
                new Runnable() {
                  @Override
                  public void run() {
                    final String destinationAddressOriginalSide =
                        ipConntrackRow.getDestinationAddressOriginalSide();
                    final String dstIpWhoisResolved;
                    if (isNullOrEmpty(destinationAddressOriginalSide)) {
                      dstIpWhoisResolved = "-";
                    } else {
                      IPWhoisInfo ipWhoisInfo = null;
                      try {
                        ipWhoisInfo =
                            mIPWhoisInfoCache.getIfPresent(destinationAddressOriginalSide);
                      } catch (Exception e) {
                        e.printStackTrace();
                        Utils.reportException(null, e);
                      }
                      //                            if (ipWhoisInfo != null &&
                      // !isNullOrEmpty(ipWhoisInfo.getCountry())) {
                      //
                      // mDestinationIpToCountry.put(destinationAddressOriginalSide,
                      // ipWhoisInfo.getCountry());
                      //                            }
                      final String org;
                      if (ipWhoisInfo == null
                          || (org = ipWhoisInfo.getOrganization()) == null
                          || org.isEmpty()) {
                        dstIpWhoisResolved = "-";
                      } else {
                        dstIpWhoisResolved = org;
                        if (!mLocalIpToHostname.containsKey(destinationAddressOriginalSide)) {
                          mLocalIpToHostname.put(destinationAddressOriginalSide, org);
                        }
                      }
                    }
                    ipToHostResolvedMap.put(destinationAddressOriginalSide, dstIpWhoisResolved);
                    runOnUiThread(
                        new Runnable() {
                          @Override
                          public void run() {
                            final String dstIpHostnameResolved =
                                ipToHostResolvedMap.get(destinationAddressOriginalSide);
                            destIpOrg.setText(
                                isNullOrEmpty(dstIpHostnameResolved) ? "" : dstIpHostnameResolved);
                            destIpOrgDetails.setText(
                                isNullOrEmpty(dstIpHostnameResolved) ? "" : dstIpHostnameResolved);
                            ipConntrackRow.setDestWhoisOrHostname(dstIpHostnameResolved);
                          }
                        });
                  }
                });
      } else {
        final String dstIpHostnameResolved =
            ipToHostResolvedMap.get(destinationAddressOriginalSide);
        destIpOrg.setText(isNullOrEmpty(dstIpHostnameResolved) ? "-" : dstIpHostnameResolved);
        destIpOrgDetails.setText(isNullOrEmpty(dstIpHostnameResolved) ? "" : dstIpHostnameResolved);
        ipConntrackRow.setDestWhoisOrHostname(dstIpHostnameResolved);
      }

      destIpOrg.setVisibility(View.VISIBLE);
      destIpOrgLoading.setVisibility(View.GONE);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      final View v =
          LayoutInflater.from(parent.getContext())
              .inflate(R.layout.activity_ip_connections_cardview, parent, false);
      final CardView cardView = v.findViewById(R.id.activity_ip_connections_card_view);
      if (ColorUtils.Companion.isThemeLight(activity)) {
        // Light
        cardView.setCardBackgroundColor(
            ContextCompat.getColor(activity, R.color.cardview_light_background));
      } else {
        // Default is Dark
        cardView.setCardBackgroundColor(
            ContextCompat.getColor(activity, R.color.cardview_dark_background));
      }

      //        return new ViewHolder(this.context,
      //                RippleViewCreator.addRippleToView(v));
      return new ViewHolder(this.activity, v);
    }

    public ActiveIPConnectionsDetailRecyclerViewAdapter setActiveIPConnections(
        List<IPConntrack> activeIPConnections) {
      this.mActiveIPConnections = activeIPConnections;
      return this;
    }
  }

  public static final String ROUTER_REMOTE_IP = "ROUTER_REMOTE_IP";

  public static final String ACTIVE_IP_CONNECTIONS_OUTPUT = "ACTIVE_IP_CONNECTIONS_OUTPUT";

  public static final String IP_TO_HOSTNAME_RESOLVER = "IP_TO_HOSTNAME_RESOLVER";

  public static final String CONNECTED_HOST = "CONNECTED_HOST";

  public static final String CONNECTED_HOST_IP = "CONNECTED_HOST_IP";

  public static final String OBSERVATION_DATE = "OBSERVATION_DATE";

  public static final Table<Integer, Integer, String> ICMP_TYPE_CODE_DESCRIPTION_TABLE =
      HashBasedTable.create();

  private static final String LOG_TAG = ActiveIPConnectionsDetailActivity.class.getSimpleName();

  public static final LoadingCache<Pair<Long, Protocol>, Collection<Record>>
      SERVICE_NAMES_PORT_NUMBERS_CACHE =
          CacheBuilder.newBuilder()
              .maximumSize(50)
              .removalListener(
                  (RemovalListener<Pair<Long, Protocol>, Collection<Record>>)
                      notification ->
                          FirebaseCrashlytics.getInstance()
                              .log(
                                  "onRemoval("
                                      + notification.getKey()
                                      + ") - cause: "
                                      + notification.getCause()))
              .expireAfterAccess(1L, TimeUnit.DAYS)
              .expireAfterWrite(1L, TimeUnit.DAYS)
              .build(
                  new CacheLoader<Pair<Long, Protocol>, Collection<Record>>() {
                    @Override
                    public Collection<Record> load(@NonNull final Pair<Long, Protocol> key)
                        throws Exception {
                      final Long portNumber = key.first;
                      final Protocol protocol = key.second;
                      if (portNumber == null || protocol == null) {
                        throw new IllegalArgumentException("Invalid pair: " + key);
                      }
                      if (BuildConfig.DONATIONS) {
                        // Premium feature only
                        FirebaseCrashlytics.getInstance()
                            .log("Service names / port numbers lookup is a premium feature");
                        return Collections.emptyList();
                      }
                      try {
                        final Response<RecordListResponse> response =
                            ServiceNamePortNumbersServiceKt.query(
                                    NetworkUtils.getServiceNamePortNumbersService(
                                        RouterCompanionApplication.getCurrentActivity()),
                                    Collections.singleton(portNumber),
                                    Collections.singleton(protocol),
                                    null)
                                .execute();
                        NetworkUtils.checkResponseSuccessful(response);
                        return response.body().getData().getRecords();
                      } catch (final Exception e) {
                        e.printStackTrace();
                        throw new DDWRTCompanionException(e);
                      }
                    }
                  });

  public static final LoadingCache<String, IPWhoisInfo> mIPWhoisInfoCache =
      CacheBuilder.newBuilder()
          .maximumSize(50)
          .removalListener(
              (RemovalListener<String, IPWhoisInfo>)
                  notification ->
                      FirebaseCrashlytics.getInstance()
                          .log(
                              "onRemoval("
                                  + notification.getKey()
                                  + ") - cause: "
                                  + notification.getCause()))
          .build(
              new CacheLoader<>() {
                @Override
                public IPWhoisInfo load(@NonNull String ipAddr) {
                  if (isNullOrEmpty(ipAddr)) {
                    throw new IllegalArgumentException("IP Addr is invalid");
                  }
                  // Get to IP Geo Lookup API (via Proxy)
                  try {
                    final ProxyData proxyData =
                        new ProxyData(
                            String.format(
                                "%s/%s.json", IPWhoisInfo.IP_WHOIS_INFO_API_PREFIX, ipAddr),
                            RequestMethod.GET);
                    final Response<JsonElement> response =
                        NetworkUtils.getProxyService(
                                RouterCompanionApplication.getCurrentActivity())
                            .proxy(proxyData)
                            .execute();
                    NetworkUtils.checkResponseSuccessful(response);
                    return JsonElementUtils.parseAs(response.body(), IPWhoisInfo.class);
                  } catch (final Exception e) {
                    e.printStackTrace();
                    throw new DDWRTCompanionException(e);
                  }
                }
              });

  private LinearLayout contentView;

  private Map<String, String> ipToHostResolvedMap;

  private ProgressBar loadingView;

  private TextView loadingViewText;

  private List<IPConntrack> mActiveIPConnections;

  private String mActiveIPConnectionsMultiLine;

  private RecyclerView.Adapter mAdapter;

  private String mConnectedHost;

  private HashMap<String, String> mDestinationIpToCountry;

  private File mFileToShare;

  private RecyclerView.LayoutManager mLayoutManager;

  private Map<String, String> mLocalIpToHostname;

  private String mObservationDate;

  private RecyclerViewEmptySupport mRecyclerView;

  private String mRouterLanIp;

  private String mRouterName;

  private String mRouterRemoteIp;

  private String mRouterUuid;

  private String mRouterWanIp;

  private String mRouterWanPublicIp;

  private RecyclerView.Adapter mStatsAdapter;

  private RecyclerView.LayoutManager mStatsLayoutManager;

  private RecyclerViewEmptySupport mStatsRecyclerView;

  private String mTitle;

  private Menu optionsMenu;

  private LinearLayout slidingUpPanel;

  private View slidingUpPanelLoading;

  private TextView slidingUpPanelStatsTitle;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final Intent intent = getIntent();

    final String[] activeIpConnArray = intent.getStringArrayExtra(ACTIVE_IP_CONNECTIONS_OUTPUT);
    if (activeIpConnArray == null || activeIpConnArray.length == 0) {
      Toast.makeText(
              ActiveIPConnectionsDetailActivity.this,
              "Internal Error - No Detailed Active IP Connections list available!",
              Toast.LENGTH_SHORT)
          .show();
      finish();
      return;
    }

    mRouterUuid = intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);
    final Router router = RouterManagementActivity.Companion.getDao(this).getRouter(mRouterUuid);
    if (router == null) {
      Toast.makeText(
              ActiveIPConnectionsDetailActivity.this,
              "Internal Error - Unknown router! Please try again later",
              Toast.LENGTH_SHORT)
          .show();
      finish();
      return;
    }
    ColorUtils.Companion.setAppTheme(
        this, router != null ? router.getRouterFirmware() : null, false);

    mRouterRemoteIp = intent.getStringExtra(ROUTER_REMOTE_IP);
    mRouterName = intent.getStringExtra(NVRAMInfo.Companion.getROUTER_NAME());
    if (TextUtils.isEmpty(mRouterName)) {
      mRouterName = router.getCanonicalHumanReadableName();
    }
    mRouterLanIp = intent.getStringExtra(NVRAMInfo.Companion.getLAN_IPADDR());
    mRouterWanIp = intent.getStringExtra(NVRAMInfo.Companion.getWAN_IPADDR());
    mRouterWanPublicIp = intent.getStringExtra(NVRAMInfo.PUBLIC_IPADDR);
    mObservationDate = intent.getStringExtra(OBSERVATION_DATE);
    mConnectedHost = intent.getStringExtra(CONNECTED_HOST);

    final String connectedHostIp = intent.getStringExtra(CONNECTED_HOST_IP);

    final boolean singleHost = !isNullOrEmpty(connectedHostIp);
    if (!singleHost) {
      // All Hosts
      final Serializable serializableExtra = intent.getSerializableExtra(IP_TO_HOSTNAME_RESOLVER);
      //noinspection unchecked
      mLocalIpToHostname =
          (serializableExtra != null && serializableExtra instanceof HashMap)
              ? ((HashMap<String, String>) serializableExtra)
              : new HashMap<String, String>();
    } else {
      // Single host
      mLocalIpToHostname = new HashMap<>();
      final String intentStringExtra = intent.getStringExtra(IP_TO_HOSTNAME_RESOLVER);
      //noinspection ConstantConditions
      if (connectedHostIp != null && intentStringExtra != null) {
        mLocalIpToHostname.put(connectedHostIp, intentStringExtra);
      }
    }

    mActiveIPConnections = new ArrayList<>();
    ipToHostResolvedMap = new HashMap<>();
    if (mRouterName != null) {
      if (mRouterLanIp != null) ipToHostResolvedMap.put(mRouterLanIp, mRouterName);
      if (mRouterWanIp != null) ipToHostResolvedMap.put(mRouterWanIp, mRouterName);
      if (mRouterRemoteIp != null) ipToHostResolvedMap.put(mRouterRemoteIp, mRouterName);
      if (mRouterWanPublicIp != null) ipToHostResolvedMap.put(mRouterWanPublicIp, mRouterName);
    }

    for (final String activeIpConn : activeIpConnArray) {
      try {
        final IPConntrack ipConntrackRow = IPConntrack.Companion.parseIpConntrackRow(activeIpConn);
        if (ipConntrackRow == null) {
          continue;
        }
        mActiveIPConnections.add(ipConntrackRow);
      } catch (final Exception e) {
        ReportingUtils.reportException(getApplicationContext(), e);
      }
    }

    handleIntent(intent);

    mDestinationIpToCountry = new HashMap<>();

    final boolean themeLight = ColorUtils.Companion.isThemeLight(this);

    setContentView(R.layout.tile_status_active_ip_connections);

    mActiveIPConnectionsMultiLine = Joiner.on("\n\n").join(mActiveIPConnections);

    final Toolbar mToolbar = findViewById(R.id.tile_status_active_ip_connections_view_toolbar);
    if (mToolbar != null) {
      mTitle = "Active IP Connections";
      mToolbar.setTitle(mTitle);
      mToolbar.setSubtitle(isNullOrEmpty(mConnectedHost) ? "" : mConnectedHost);
      mToolbar.setTitleTextAppearance(getApplicationContext(), R.style.ToolbarTitle);
      mToolbar.setSubtitleTextAppearance(getApplicationContext(), R.style.ToolbarSubtitle);
      mToolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
      mToolbar.setSubtitleTextColor(ContextCompat.getColor(this, R.color.white));
      setSupportActionBar(mToolbar);
    }
    final ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
      actionBar.setHomeButtonEnabled(true);
    }

    this.loadingView = findViewById(R.id.tile_status_active_ip_connections_view_loadingview);
    this.loadingViewText =
        findViewById(R.id.tile_status_active_ip_connections_view_loadingview_text);

    loadingView.setProgress(3);
    loadingViewText.setText("Initializing...");

    this.contentView =
        findViewById(R.id.tile_status_active_ip_connections_view_recyclerview_linearlayout);

    mRecyclerView = findViewById(R.id.tile_status_active_ip_connections_recycler_view);
    // use this setting to improve performance if you know that changes
    // in content do not change the layout size of the RecyclerView
    // allows for optimizations if all items are of the same size:
    mRecyclerView.setHasFixedSize(true);
    // use a linear layout manager
    mLayoutManager = new LinearLayoutManager(this);
    mLayoutManager.scrollToPosition(0);
    mRecyclerView.setLayoutManager(mLayoutManager);
    final TextView emptyView = findViewById(R.id.empty_view);
    if (themeLight) {
      emptyView.setTextColor(ContextCompat.getColor(this, R.color.black));
    } else {
      emptyView.setTextColor(ContextCompat.getColor(this, R.color.white));
    }
    mRecyclerView.setEmptyView(emptyView);
    // specify an adapter (see also next example)
    mAdapter = new ActiveIPConnectionsDetailRecyclerViewAdapter(this);
    mRecyclerView.setAdapter(mAdapter);

    // Stats
    this.slidingUpPanel = findViewById(R.id.active_ip_connections_stats);
    this.slidingUpPanelStatsTitle = findViewById(R.id.active_ip_connections_stats_title);
    if (themeLight) {
      slidingUpPanel.setBackgroundColor(
          ContextCompat.getColor(this, R.color.black_semi_transparent));
      this.slidingUpPanelStatsTitle.setTextColor(ContextCompat.getColor(this, R.color.white));
    } else {
      slidingUpPanel.setBackgroundColor(
          ContextCompat.getColor(this, R.color.white_semi_transparent));
      this.slidingUpPanelStatsTitle.setTextColor(ContextCompat.getColor(this, R.color.black));
    }

    this.slidingUpPanelLoading = findViewById(R.id.activity_ip_connections_stats_loading);
    this.slidingUpPanelLoading.setVisibility(View.VISIBLE);
    this.slidingUpPanelStatsTitle.setText("Computing stats...");

    mStatsRecyclerView = findViewById(R.id.tile_status_active_ip_connections_stats_recycler_view);
    // use this setting to improve performance if you know that changes
    // in content do not change the layout size of the RecyclerView
    // allows for optimizations if all items are of the same size:
    mStatsRecyclerView.setHasFixedSize(true);
    // use a linear layout manager
    mStatsLayoutManager = new LinearLayoutManager(this);
    mStatsLayoutManager.scrollToPosition(0);
    mStatsRecyclerView.setLayoutManager(mStatsLayoutManager);
    final TextView statsEmptyView =
        findViewById(R.id.tile_status_active_ip_connections_stats_recycler_view_empty_view);
    if (themeLight) {
      statsEmptyView.setTextColor(ContextCompat.getColor(this, R.color.black));
    } else {
      statsEmptyView.setTextColor(ContextCompat.getColor(this, R.color.white));
    }
    mStatsRecyclerView.setEmptyView(statsEmptyView);
    //        // specify an adapter (see also next example)
    mStatsAdapter = new ActiveIPConnectionsStatsAdapter(this, singleHost);
    mStatsRecyclerView.setAdapter(mStatsAdapter);

    new BgAsyncTask(this).execute();
  }

  @Override
  protected void onNewIntent(Intent intent) {
    handleIntent(intent);
  }

  @Override
  protected void onDestroy() {
    if (mFileToShare != null) {
      //noinspection ResultOfMethodCallIgnored
      mFileToShare.delete();
    }
    super.onDestroy();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.tile_status_active_ip_connections_options, menu);

    this.optionsMenu = menu;

    // Search
    final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

    final SearchView searchView =
        (SearchView) menu.findItem(R.id.tile_status_active_ip_connections_search).getActionView();

    searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

    // Get the search close button image view
    final ImageView closeButton = searchView.findViewById(R.id.search_close_btn);
    if (closeButton != null) {
      // Set on click listener
      closeButton.setOnClickListener(
          v -> {
            // Reset views
            // Hide it now
            searchView.setIconified(true);
          });
    }
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int i = item.getItemId();
    if (i == android.R.id.home) {
      onBackPressed();
      return true;
    } else if (i == R.id.action_feedback) {
      Utils.openFeedbackForm(this, mRouterUuid);
      return true;
    } else if (i == R.id.tile_status_active_ip_connections_share) {
      PermissionsUtils.requestPermissions(
          this,
          Collections.singletonList(permission.WRITE_EXTERNAL_STORAGE),
          () -> {
            mFileToShare =
                new File(
                    getCacheDir(),
                    getEscapedFileName(
                            String.format(
                                "%s on Router %s on %s",
                                mTitle, nullToEmpty(mRouterRemoteIp), mObservationDate))
                        + ".txt");

            Exception exception = null;
            OutputStream outputStream = null;
            try {
              outputStream = new BufferedOutputStream(new FileOutputStream(mFileToShare, false));
              //noinspection ConstantConditions
              outputStream.write(mActiveIPConnectionsMultiLine.getBytes());
            } catch (IOException e) {
              exception = e;
              e.printStackTrace();
            } finally {
              try {
                if (outputStream != null) {
                  outputStream.close();
                }
              } catch (IOException e) {
                e.printStackTrace();
              }
            }

            if (exception != null) {
              Utils.displayMessage(
                  this,
                  "Error while trying to share Active IP Connections - please try again later",
                  Style.ALERT);
              return null;
            }

            final Uri uriForFile =
                FileProvider.getUriForFile(
                    this, RouterCompanionAppConstants.FILEPROVIDER_AUTHORITY, mFileToShare);

            final Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);
            sendIntent.putExtra(
                Intent.EXTRA_SUBJECT,
                "Active IP Connections on Router '" + mRouterRemoteIp + "' on " + mObservationDate);
            String body = "";
            if (!isNullOrEmpty(mConnectedHost)) {
              body = (mTitle + " on " + mObservationDate);
            }
            body += Utils.getShareIntentFooter();

            sendIntent.putExtra(Intent.EXTRA_TEXT, fromHtml(body.replaceAll("\n", "<br/>")));

            sendIntent.setDataAndType(uriForFile, "text/html");
            //        sendIntent.setType("text/plain");
            sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(
                Intent.createChooser(sendIntent, getResources().getText(R.string.send_to)));

            return null;
          },
          () -> null,
          "Storage access is required to share data about active IP connections");
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void handleIntent(Intent intent) {

    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
      final String query = intent.getStringExtra(SearchManager.QUERY);
      final ActiveIPConnectionsDetailRecyclerViewAdapter adapter =
          (ActiveIPConnectionsDetailRecyclerViewAdapter) mAdapter;
      if (Strings.isNullOrEmpty(query)) {
        return;
      }
      adapter.getFilter().filter(query);
    }
  }

  static {
    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(0, 0, "Echo Reply");

    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 0, "Network unreachable");
    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 1, "Host unreachable");
    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 2, "Protocol unreachable");
    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 3, "Port unreachable");
    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 4, "Fragmentation needed but no frag. bit set");
    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 5, "Source routing failed");
    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 6, "Dest. network unknown");
    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 7, "Dest. host unknown");
    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 8, "Source host isolated");
    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 9, "Network administratively prohibited");
    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 10, "Host administratively prohibited");
    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 11, "Network unreachable for TOS");
    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 12, "Host unreachable for TOS");
    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 13, "Communication administratively prohibited");
    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 14, "Host Precedence Violation");
    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(3, 15, "Precedence cutoff in effect");

    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(4, 0, "Source quench");

    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(5, 0, "Redirect Datagram for Network");
    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(5, 1, "Redirect Datagram for Host");
    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(5, 2, "Redirect Datagram for TOS & network");
    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(5, 3, "Redirect Datagram for the TOS & host");

    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(8, 0, "Echo request");

    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(9, 0, "Router Advertisement (Normal)");
    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(9, 16, "Router advertisement - No traffic routing");

    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(10, 0, "Route Selection");

    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(11, 0, "TTL expired in transit");
    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(11, 1, "Fragment reassembly time exceeded");

    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(12, 0, "IP header bad");
    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(12, 1, "Required options missing");
    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(12, 2, "Bad length for IP header");

    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(13, 0, "Timestamp request");

    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(14, 0, "Timestamp reply");

    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(15, 0, "Information request");

    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(16, 0, "Information reply");

    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(17, 0, "Address Mask Request");

    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(18, 0, "Address Mask Reply");

    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(30, 0, "Traceroute");

    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(31, 0, "Datagram Conversion Error");

    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(32, 0, "Mobile Host Redirect");

    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(33, 0, "IPv6 Where-Are-You");

    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(34, 0, "IPv6 I-Am-Here");

    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(35, 0, "Mobile Registration Request");

    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(36, 0, "Mobile Registration Reply");

    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(39, 0, "SKIP Algorithm Discovery Protocol");

    ICMP_TYPE_CODE_DESCRIPTION_TABLE.put(40, 0, "Photuris, Security failures");
  }
}
