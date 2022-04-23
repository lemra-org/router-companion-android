package org.rm3l.router_companion.actions.activity;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.router_companion.RouterCompanionAppConstants.CHARSET;
import static org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.router_companion.RouterCompanionAppConstants.MAX_ROUTER_SPEEDTEST_RESULTS_FREE_VERSION;
import static org.rm3l.router_companion.RouterCompanionAppConstants.ROUTER_SPEED_TEST_AUTO_MEASUREMENTS;
import static org.rm3l.router_companion.RouterCompanionAppConstants.ROUTER_SPEED_TEST_AUTO_MEASUREMENTS_SCHEDULE;
import static org.rm3l.router_companion.RouterCompanionAppConstants.ROUTER_SPEED_TEST_DURATION_THRESHOLD_SECONDS;
import static org.rm3l.router_companion.RouterCompanionAppConstants.ROUTER_SPEED_TEST_DURATION_THRESHOLD_SECONDS_DEFAULT;
import static org.rm3l.router_companion.RouterCompanionAppConstants.ROUTER_SPEED_TEST_MAX_FILE_SIZE_MB;
import static org.rm3l.router_companion.RouterCompanionAppConstants.ROUTER_SPEED_TEST_MAX_FILE_SIZE_MB_DEFAULT;
import static org.rm3l.router_companion.RouterCompanionAppConstants.ROUTER_SPEED_TEST_MEASUREMENT_UNIT;
import static org.rm3l.router_companion.RouterCompanionAppConstants.ROUTER_SPEED_TEST_SERVER;
import static org.rm3l.router_companion.RouterCompanionAppConstants.ROUTER_SPEED_TEST_SERVER_AUTO;
import static org.rm3l.router_companion.RouterCompanionAppConstants.ROUTER_SPEED_TEST_SERVER_RANDOM;
import static org.rm3l.router_companion.RouterCompanionAppConstants.UNIT_BIT;
import static org.rm3l.router_companion.RouterCompanionAppConstants.UNIT_BYTE;
import static org.rm3l.router_companion.job.speedtest.RouterSpeedTestAutoRunnerJob.runPing;
import static org.rm3l.router_companion.utils.Utils.fromHtml;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.io.Files;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.jakewharton.byteunits.BitUnit;
import com.jakewharton.byteunits.DecimalByteUnit;
import com.squareup.picasso.Callback;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import mbanje.kurt.fabbutton.FabButton;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.actions.AbstractRouterAction;
import org.rm3l.router_companion.exceptions.SpeedTestException;
import org.rm3l.router_companion.job.speedtest.RouterSpeedTestAutoRunnerJob;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.router_companion.resources.SpeedTestResult;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.settings.RouterSpeedTestSettingsActivity;
import org.rm3l.router_companion.utils.AdUtils;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.ImageUtils;
import org.rm3l.router_companion.utils.PermissionsUtils;
import org.rm3l.router_companion.utils.ReportingUtils;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.snackbar.SnackbarCallback;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style;
import org.rm3l.router_companion.widgets.RecyclerViewEmptySupport;

public class SpeedTestActivity extends AppCompatActivity
    implements SwipeRefreshLayout.OnRefreshListener, SnackbarCallback {

  public static final String SERVER_DISPLAY_NAME = "server_display_name";

  public static class NetworkChangeReceiver extends BroadcastReceiver {

    private final SpeedTestActivity activity;

    public NetworkChangeReceiver(final SpeedTestActivity activity) {
      this.activity = activity;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {

      final NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
      if (info != null && info.isConnected()) {
        activity.updateToolbarTitleAndSubTitle();
      }
    }
  }

  static class SpeedTestAsyncTask
      extends AsyncTask<Void, Integer, AbstractRouterAction.RouterActionResult<Void>> {

    private final SpeedTestActivity activity;

    private Date executionDate;

    private String pingServerCountry;

    private String server;

    private SpeedTestResult speedTestResult;

    SpeedTestAsyncTask(SpeedTestActivity activity) {
      this.activity = activity;
    }

    void cancelAction() {
      try {
        final Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("Action", "Cancel");
        ReportingUtils.reportEvent(ReportingUtils.EVENT_SPEEDTEST, eventMap);
      } catch (final Exception e) {
        // No worries
      }

      activity.mRouterCopy.destroyAllSessions();
    }

    @Override
    protected AbstractRouterAction.RouterActionResult<Void> doInBackground(Void... params) {

      try {
        final Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("Action", "Run");
        ReportingUtils.reportEvent(ReportingUtils.EVENT_SPEEDTEST, eventMap);
      } catch (final Exception e) {
        // No worries
      }

      executionDate = new Date();

      FirebaseCrashlytics.getInstance().log("executionDate: " + executionDate);

      Exception exception = null;
      try {

        //                if (mSpeedTestRunning.get()) {
        //                    throw new SpeedTestException("Already Running");
        //                }
        //                mSpeedTestRunning.set(true);

        if (isCancelled()) {
          throw new InterruptedException();
        }
        activity.runOnUiThread(
            new Runnable() {
              @Override
              public void run() {
                resetEverything(false);
                activity.errorPlaceholder.setVisibility(View.GONE);
                //                        mRunFab.setIcon(
                //                                R.drawable.ic_close_white_24dp,
                //                                R.drawable.ic_play_arrow_white_24dp
                //                        );

                activity.mRunFab.setVisibility(View.GONE);
                activity.mCancelFab.setVisibility(View.VISIBLE);

                activity.mRunFab.setProgress(0);
                activity.mCancelFab.setProgress(0);

                activity
                    .findViewById(R.id.speedtest_latency_pb_internet)
                    .setVisibility(View.VISIBLE);
                activity.findViewById(R.id.speedtest_dl_pb_internet).setVisibility(View.VISIBLE);
                //                        findViewById(R.id.speedtest_ul_pb_internet)
                //                                .setVisibility(View.VISIBLE);
                activity.findViewById(R.id.speedtest_pb_wifi).setVisibility(View.VISIBLE);
                //                        findViewById(R.id.speedtest_pb_wifi_efficiency)
                //                                .setVisibility(View.VISIBLE);
              }
            });

        // 1- Determine if we need to select the closest server
        publishProgress(SELECT_SERVER);
        final String serverSetting =
            activity.mRouterPreferences.getString(
                ROUTER_SPEED_TEST_SERVER, ROUTER_SPEED_TEST_SERVER_RANDOM);

        String wanDLSpeedUrlToFormat = null;
        String wanULSpeedUrlToFormat = null;

        pingServerCountry = serverSetting;
        PingRTT wanLatencyResults = null;
        if (ROUTER_SPEED_TEST_SERVER_AUTO.equals(serverSetting)) {
          // Iterate over each server to determine the closest one,
          // in terms of ping latency
          float minLatency = Float.MAX_VALUE;
          String serverCountry = null;

          int i = 1;
          for (final Map.Entry<String, Map<String, String>> entry : SERVERS.rowMap().entrySet()) {
            if (isCancelled()) {
              throw new InterruptedException();
            }
            final String country = entry.getKey();
            final Map<String, String> value = entry.getValue();
            final String pingServer = value.get(PING_SERVER);
            wanDLSpeedUrlToFormat = value.get(HTTP_DL_URL);
            wanULSpeedUrlToFormat = value.get(HTTP_UL_URL);
            if (isNullOrEmpty(pingServer)) {
              continue;
            }

            i += 2;

            final int j = i;

            activity.runOnUiThread(
                () -> {
                  activity.mCancelFab.setProgress(j + (100 * 1 / 4));
                  activity.noticeTextView.setText(
                      String.format(
                          Locale.US,
                          "1/3 - Selecting remote test server...\n" + " Contacting '%s'...",
                          getServerLocationDisplayFromCountryCode(country)));
                });

            final PingRTT pingRTT =
                runPing(activity, activity.mOriginalRouter, activity.mRouterCopy, pingServer);
            final float avg = pingRTT.getAvg();
            if (avg < 0) {
              continue;
            }
            if (avg <= minLatency) {
              minLatency = avg;
              server = pingServer;
              serverCountry = country;
              wanLatencyResults = pingRTT;
            }
          }
          pingServerCountry = serverCountry;
          if (!isNullOrEmpty(pingServerCountry)) {
            activity.runOnUiThread(() -> activity.refreshSpeedTestParameters(pingServerCountry));
          }
        } else {
          pingServerCountry = serverSetting;
          if (ROUTER_SPEED_TEST_SERVER_RANDOM.equals(serverSetting)) {
            // Pick one randomly
            final Set<String> rowKeySet = SERVERS.rowKeySet();
            pingServerCountry = Lists.newArrayList(rowKeySet).get(RANDOM.nextInt(rowKeySet.size()));
          }
          activity.refreshSpeedTestParameters(pingServerCountry);
          server = SERVERS.get(pingServerCountry, PING_SERVER);
          wanDLSpeedUrlToFormat = SERVERS.get(pingServerCountry, HTTP_DL_URL);
          wanULSpeedUrlToFormat = SERVERS.get(pingServerCountry, HTTP_UL_URL);
        }

        if (isNullOrEmpty(server) || isNullOrEmpty(wanDLSpeedUrlToFormat)) {
          throw new SpeedTestException("Invalid server");
        }

        speedTestResult = new SpeedTestResult();

        // 2- Now measure ping latency
        publishProgress(MEASURE_PING_LATENCY);
        if (wanLatencyResults == null) {
          wanLatencyResults =
              runPing(activity, activity.mOriginalRouter, activity.mRouterCopy, server);
        }
        speedTestResult.setWanPingRTT(wanLatencyResults);
        publishProgress(PING_LATENCY_MEASURED);

        // WAN DL / UL: algorithm here: https://speedof.me/howitworks.html
        final long userDefinedRouterSpeedTestMaxFileSizeMB =
            activity.mRouterPreferences.getLong(
                ROUTER_SPEED_TEST_MAX_FILE_SIZE_MB, ROUTER_SPEED_TEST_MAX_FILE_SIZE_MB_DEFAULT);
        final long userDefinedRouterSpeedTestDurationThresholdSeconds =
            Long.parseLong(
                activity.mRouterPreferences.getString(
                    ROUTER_SPEED_TEST_DURATION_THRESHOLD_SECONDS,
                    ROUTER_SPEED_TEST_DURATION_THRESHOLD_SECONDS_DEFAULT));

        Pair<Long, Long> pairAcceptedForComputation = null;

        FirebaseCrashlytics.getInstance()
            .log("mPossibleFileSizes: " + Arrays.toString(activity.mPossibleFileSizes));

        int pg = 2;
        for (final Long possibleFileSize : activity.mPossibleFileSizes) {
          if (isCancelled()) {
            throw new InterruptedException();
          }
          // Measure time to download file of the specified type
          //
          //                    final String remoteFileName = getRemoteFileName(possibleFileSize);

          final String remoteFileName = Long.toString(possibleFileSize);

          @SuppressLint("DefaultLocale")
          final String completeServerUrl =
              String.format(
                  "%s?_=%d",
                  String.format(wanDLSpeedUrlToFormat, remoteFileName), System.currentTimeMillis());

          pg += 3;

          final int pgForFile = pg;
          activity.runOnUiThread(
              new Runnable() {
                @Override
                public void run() {
                  // Display message to user
                  activity.mCancelFab.setProgress((100 * 3 / 4) + pgForFile);
                  activity.noticeTextView.setText(
                      "3/3 - Downloading data: " + remoteFileName + "MB...");
                  // final int netDlColor = ColorUtils.getColor(NET_DL);
                  // internetRouterLink.setBackgroundColor(netDlColor);
                  // highlightTitleTextView(mSpeedtestWanDlTitle);
                }
              });

          final String[] cmdExecOutput;
          if (Utils.isDemoRouter(activity.mOriginalRouter)) {
            cmdExecOutput =
                new String[] {
                  Integer.toString(Math.min(77, new Random().nextInt(possibleFileSize.intValue()))),
                  Integer.toString(new Random().nextInt(1))
                };
          } else {
            cmdExecOutput =
                SSHUtils.getManualProperty(
                    activity,
                    activity.mRouterCopy,
                    activity.getSharedPreferences(
                        DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE),
                    Joiner.on(" && ").skipNulls(),
                    "DATE_START=$(/bin/date +\"%s\")",
                    // seconds since 1970-01-01 00:00:00 UTC
                    String.format(
                        "/usr/bin/wget -qO /dev/null \"%s\" > /dev/null 2>&1 ", completeServerUrl),
                    "DATE_END=$(/bin/date +\"%s\")",
                    // seconds since 1970-01-01 00:00:00 UTC
                    "/bin/echo $((${DATE_END}-${DATE_START}))", // number of seconds
                    "/bin/echo $?");
          }

          if (cmdExecOutput == null
              || cmdExecOutput.length < 2
              || !"0".equals(nullToEmpty(cmdExecOutput[cmdExecOutput.length - 1]).trim())) {
            final SpeedTestException speedTestException =
                new SpeedTestException("Failed to download data: " + remoteFileName + "MB");
            ReportingUtils.reportException(activity, speedTestException);
            throw speedTestException;
          }

          final long elapsedSeconds;
          try {
            elapsedSeconds =
                Long.parseLong(nullToEmpty(cmdExecOutput[cmdExecOutput.length - 2]).trim());
          } catch (final NumberFormatException nfe) {
            ReportingUtils.reportException(activity, nfe);
            throw new SpeedTestException("Unexpected output - please try again later.");
          }

          if (elapsedSeconds < 0) {
            throw new SpeedTestException("Unexpected output - please try again later.");
          }

          FirebaseCrashlytics.getInstance()
              .log(
                  String.format(
                      Locale.US,
                      "[SpeedTest] Downloaded %d MB of data in %d seconds. Download URL is: \"%s\"",
                      possibleFileSize,
                      elapsedSeconds,
                      completeServerUrl));

          speedTestResult.setWanDLFileSize(possibleFileSize);
          speedTestResult.setWanDLDuration(elapsedSeconds);

          pairAcceptedForComputation = Pair.create(possibleFileSize, elapsedSeconds);
          // Stop conditions: time_to_dl >= threshold or fileSize >= possibleFileSize
          if (possibleFileSize >= userDefinedRouterSpeedTestMaxFileSizeMB
              || elapsedSeconds >= userDefinedRouterSpeedTestDurationThresholdSeconds) {
            break;
          }
        }

        if (isCancelled()) {
          throw new InterruptedException();
        }

        // 3- WAN DL
        publishProgress(TEST_WAN_DL);
        if (pairAcceptedForComputation != null) {
          final long timeElapsedSeconds = pairAcceptedForComputation.second;
          final long wanDl =
              (timeElapsedSeconds != 0
                  ? ((pairAcceptedForComputation.first * 1024 * 1024) / timeElapsedSeconds)
                  : (pairAcceptedForComputation.first * 1024 * 1024));
          speedTestResult.setWanDl(wanDl);
        }
        publishProgress(WAN_DL_MEASURED);

        if (isCancelled()) {
          throw new InterruptedException();
        }

        // 3- WAN UL
        //                publishProgress(TEST_WAN_UL);
        // TODO //FIXME Use real data
        speedTestResult.setWanUl(new Random().nextInt(27) * 1024 ^ 5);
        publishProgress(WAN_UL_MEASURED);
      } catch (Exception e) {
        ReportingUtils.reportException(activity, e);
        exception = e;
      }

      return new AbstractRouterAction.RouterActionResult<>(
          null,
          exception,
          ImmutableMap.of(
              SERVER_DISPLAY_NAME, getServerLocationDisplayFromCountryCode(pingServerCountry)));
    }

    @Override
    protected void onCancelled(
        AbstractRouterAction.RouterActionResult<Void> voidRouterActionResult) {
      FirebaseCrashlytics.getInstance().log("onCancelled");
      activity.errorPlaceholder.setText("Aborted");
      activity.errorPlaceholder.setVisibility(View.VISIBLE);
      resetEverything(true);
    }

    @Override
    protected void onPostExecute(
        AbstractRouterAction.RouterActionResult<Void> voidRouterActionResult) {
      FirebaseCrashlytics.getInstance().log("onPostExecute");

      if (voidRouterActionResult != null) {
        final Exception exception = voidRouterActionResult.getException();
        //                mRunFab.setIcon(
        //                        R.drawable.ic_play_arrow_white_24dp,
        //                        R.drawable.ic_play_arrow_white_24dp
        //                );
        if (exception != null) {
          activity.errorPlaceholder.setVisibility(View.VISIBLE);
          final Pair<String, String> exceptionPair = Utils.handleException(exception);
          activity.errorPlaceholder.setText(String.format("Error: %s", exceptionPair.first));
          if (!isNullOrEmpty(exceptionPair.second)) {
            final Object serverDisplayNameInMetadata =
                voidRouterActionResult.metadata.get(SERVER_DISPLAY_NAME);
            activity.errorPlaceholder.setOnClickListener(
                v ->
                    Toast.makeText(
                            activity,
                            String.format(
                                "%s : %s",
                                serverDisplayNameInMetadata != null
                                    ? serverDisplayNameInMetadata
                                    : "-",
                                exceptionPair.second),
                            Toast.LENGTH_SHORT)
                        .show());
          }
        } else {
          // Persist speed test result
          final SpeedTestResult speedTestResultToPersist =
              new SpeedTestResult(
                  activity.mOriginalRouter.getUuid(),
                  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(executionDate),
                  server,
                  this.speedTestResult.getWanPing(),
                  this.speedTestResult.getWanDl(),
                  this.speedTestResult.getWanUl(),
                  null,
                  null,
                  null,
                  pingServerCountry);
          final PingRTT speedTestResultToPersistWanPingRTT = new PingRTT();
          if (this.speedTestResult.getWanPing() != null) {
            speedTestResultToPersistWanPingRTT.setAvg(
                this.speedTestResult.getWanPing().floatValue());
          }
          if (this.speedTestResult.getWanPingRTT() != null) {
            speedTestResultToPersistWanPingRTT.setPacketLoss(
                this.speedTestResult.getWanPingRTT().getPacketLoss());
            speedTestResultToPersistWanPingRTT.setStddev(
                this.speedTestResult.getWanPingRTT().getStddev());
            speedTestResultToPersistWanPingRTT.setMax(
                this.speedTestResult.getWanPingRTT().getMax());
            speedTestResultToPersistWanPingRTT.setMin(
                this.speedTestResult.getWanPingRTT().getMin());
          }
          speedTestResultToPersist.setWanPingRTT(speedTestResultToPersistWanPingRTT);

          speedTestResultToPersist.setWanDLFileSize(speedTestResult.getWanDLFileSize());
          speedTestResultToPersist.setWanDLDuration(speedTestResult.getWanDLDuration());
          speedTestResultToPersist.setWanULFileSize(speedTestResult.getWanULFileSize());
          speedTestResultToPersist.setWanULDuration(speedTestResult.getWanULDuration());

          speedTestResultToPersist.setConnectionDLFileSize(
              speedTestResult.getConnectionDLFileSize());
          speedTestResultToPersist.setConnectionDLDuration(
              speedTestResult.getConnectionDLDuration());
          speedTestResultToPersist.setConnectionULFileSize(
              speedTestResult.getConnectionULFileSize());
          speedTestResultToPersist.setConnectionULDuration(
              speedTestResult.getConnectionULDuration());

          activity.mDao.insertSpeedTestResult(speedTestResultToPersist);

          // Request Backup
          Utils.requestBackup(activity);

          final List<SpeedTestResult> speedTestResultsByRouter =
              activity.mDao.getSpeedTestResultsByRouter(activity.mOriginalRouter.getUuid());

          activity.updateNbSpeedTestResults(speedTestResultsByRouter);
          activity.mAdapter.setSpeedTestResults(speedTestResultsByRouter);
          activity.mAdapter.notifyItemInserted(0);
          // Scroll to top
          activity.mLayoutManager.scrollToPosition(0);

          activity.errorPlaceholder.setVisibility(View.GONE);
        }
      } else {
        activity.errorPlaceholder.setVisibility(View.GONE);
      }
      resetEverything(true);
      super.onPostExecute(voidRouterActionResult);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onProgressUpdate(Integer... values) {
      // Runs on main thread
      if (values == null) {
        return;
      }
      final Integer progressCode = values[0];

      FirebaseCrashlytics.getInstance().log("progressCode: " + progressCode);

      if (progressCode == null) {
        return;
      }
      switch (progressCode) {
        case SELECT_SERVER:
          activity.mCancelFab.setProgress(100 * 1 / 4);
          activity.noticeTextView.setText("1/3 - Selecting remote test server...");
          activity.noticeTextView.startAnimation(
              AnimationUtils.loadAnimation(activity, android.R.anim.slide_in_left));
          activity.noticeTextView.setVisibility(View.VISIBLE);
          break;

        case MEASURE_PING_LATENCY:
          activity.mCancelFab.setProgress(100 * 2 / 4);
          activity.noticeTextView.setText("2/3 - Measuring Internet (WAN) Latency...");
          final int latencyColor = ColorUtils.Companion.getColor(NET_LATENCY);
          activity.internetRouterLink.setBackgroundColor(latencyColor);
          activity.highlightTitleTextView(activity.mSpeedtestLatencyTitle);
          break;

        case TEST_WAN_DL:
          activity.mCancelFab.setProgress(100 * 3 / 4);
          activity.noticeTextView.setText("3/3 - Measuring Internet (WAN) Download Speed...");
          final int netDlColor = ColorUtils.Companion.getColor(NET_DL);
          activity.internetRouterLink.setBackgroundColor(netDlColor);
          activity.highlightTitleTextView(activity.mSpeedtestWanDlTitle);
          break;

        case TEST_WAN_UL:
          activity.mCancelFab.setProgress(100 * 4 / 4);
          //                    noticeTextView
          //                            .setText("4/4 - Measuring Internet (WAN) Upload Speed...");
          final int netUlColor = ColorUtils.Companion.getColor(NET_UL);
          activity.internetRouterLink.setBackgroundColor(netUlColor);
          //                    highlightTitleTextView(mSpeedtestWanUlTitle);
          break;

        case PING_LATENCY_MEASURED:
          // Display results
          //noinspection ConstantConditions
          activity.findViewById(R.id.speedtest_latency_pb_internet).setVisibility(View.GONE);
          final TextView wanLatencyTextView =
              activity.findViewById(R.id.speedtest_internet_latency);
          wanLatencyTextView.setVisibility(View.VISIBLE);
          if (speedTestResult != null && speedTestResult.getWanPing() != null) {
            wanLatencyTextView.setText(
                String.format(Locale.US, "%.2f ms", speedTestResult.getWanPing().floatValue()));
          } else {
            wanLatencyTextView.setText("-");
          }

          final TextView wanLatencyPacketLossTextView =
              activity.findViewById(R.id.speedtest_internet_latency_packet_loss);
          final TextView wanLatencyMaxTextView =
              activity.findViewById(R.id.speedtest_internet_latency_max);
          final PingRTT wanPingRTT;
          if (speedTestResult != null && (wanPingRTT = speedTestResult.getWanPingRTT()) != null) {
            if (wanPingRTT.getPacketLoss() >= 0) {
              wanLatencyPacketLossTextView.setVisibility(View.VISIBLE);
              wanLatencyPacketLossTextView.setText(
                  String.format(
                      Locale.US,
                      "(%d%% packet loss)",
                      Float.valueOf(wanPingRTT.getPacketLoss()).intValue()));
            } else {
              wanLatencyPacketLossTextView.setVisibility(View.INVISIBLE);
            }
            if (wanPingRTT.getMax() >= 0) {
              wanLatencyMaxTextView.setVisibility(View.VISIBLE);
              wanLatencyMaxTextView.setText(
                  String.format(Locale.US, "(max: %.2f ms)", wanPingRTT.getMax()));
            } else {
              wanLatencyMaxTextView.setVisibility(View.INVISIBLE);
            }
          } else {
            wanLatencyPacketLossTextView.setVisibility(View.INVISIBLE);
            wanLatencyMaxTextView.setVisibility(View.INVISIBLE);
          }

          break;

        case WAN_DL_MEASURED:
          //noinspection ConstantConditions
          activity.findViewById(R.id.speedtest_dl_pb_internet).setVisibility(View.GONE);
          activity.mWanDlTextView.setVisibility(View.VISIBLE);
          if (speedTestResult != null && speedTestResult.getWanDl() != null) {
            activity.mWanDlTextView.setText(
                String.format(
                    "%s%s",
                    activity.toHumanReadableSize(speedTestResult.getWanDl().longValue()), PER_SEC));
          } else {
            activity.mWanDlTextView.setText("-");
          }

          final TextView wanDLSizeAndDuration =
              activity.findViewById(R.id.speedtest_internet_dl_speed_size_and_duration);
          if (speedTestResult != null && speedTestResult.getWanDLFileSize() != null) {
            wanDLSizeAndDuration.setVisibility(View.VISIBLE);
            activity.mSpeedTestWanDlRaw.setText(
                Long.toString(speedTestResult.getWanDl().longValue()));
            wanDLSizeAndDuration.setText(
                String.format(
                    Locale.US,
                    "(%d MB in %d s)",
                    speedTestResult.getWanDLFileSize().longValue(),
                    speedTestResult.getWanDLDuration()));
          } else {
            wanDLSizeAndDuration.setVisibility(View.INVISIBLE);
            activity.mSpeedTestWanDlRaw.setText(null);
          }

          break;

        case WAN_UL_MEASURED:
          //noinspection ConstantConditions
          activity.findViewById(R.id.speedtest_ul_pb_internet).setVisibility(View.GONE);
          //                    mWanUlTextView.setVisibility(View.VISIBLE);
          if (speedTestResult != null && speedTestResult.getWanUl() != null) {
            activity.mSpeedTestWanUlRaw.setText(
                Long.toString(speedTestResult.getWanUl().longValue()));
            activity.mWanUlTextView.setText(
                String.format(
                    "%s%s",
                    activity.toHumanReadableSize(speedTestResult.getWanUl().longValue()), PER_SEC));
          } else {
            activity.mWanUlTextView.setText("-");
            activity.mSpeedTestWanUlRaw.setText(null);
          }
          break;

        default:
          break;
      }
      super.onProgressUpdate(values);
    }

    private void resetEverything(final boolean enableSwipeRefresh) {
      FirebaseCrashlytics.getInstance().log("resetEverything(" + enableSwipeRefresh + ")");
      try {
        activity.internetRouterLink.setBackgroundColor(activity.defaultColorForPaths);
        activity.routerLanLink.setBackgroundColor(activity.defaultColorForPaths);

        activity.noticeTextView.startAnimation(
            AnimationUtils.loadAnimation(activity, android.R.anim.slide_out_right));
        activity.noticeTextView.setVisibility(View.GONE);
        activity.resetAllTitleViews();

        activity.mRunFab.resetIcon();
        activity.mCancelFab.resetIcon();
        //                mRunFab.setIcon(
        //                        R.drawable.ic_play_arrow_white_24dp,
        //                        R.drawable.ic_play_arrow_white_24dp
        //                );
        activity.mCancelFab.setProgress(0);
        activity.mRunFab.setProgress(0);

        activity.mRunFab.setVisibility(View.VISIBLE);
        activity.mCancelFab.setVisibility(View.GONE);

        activity.setRefreshActionButtonState(!enableSwipeRefresh);
      } finally {
        //                mSpeedTestRunning.set(false);
      }
    }
  }

  public static final String PER_SEC = "ps";

  public static final String NET_LATENCY = "net_latency";

  public static final String NET_DL = "net_dl";

  public static final String NET_UL = "net_ul";

  public static final String NET_WIFI = "net_wifi";

  public static final int SELECT_SERVER = 1;

  public static final Splitter EQUAL_SPLITTER = Splitter.on("=").omitEmptyStrings().trimResults();

  public static final Splitter SLASH_SPLITTER = Splitter.on("/").omitEmptyStrings().trimResults();

  public static final String AUTO_DETECTED = "Auto-detected";

  public static final String RANDOM_SELECTED = "Random";

  public static final Random RANDOM = new Random();

  public static final Splitter HYPHEN_SPLITTER = Splitter.on("-").omitEmptyStrings().trimResults();

  public static final String PING_SERVER = "PING_SERVER";

  public static final String HTTP_DL_URL = "HTTP_DL_URL";

  public static final String HTTP_UL_URL = "HTTP_UL_URL";

  private static final String LOG_TAG = SpeedTestActivity.class.getSimpleName();

  private static final int MEASURE_PING_LATENCY = 2;

  private static final int PING_LATENCY_MEASURED = 21;

  private static final int TEST_WAN_DL = 4;

  private static final int WAN_DL_MEASURED = 41;

  private static final int TEST_WAN_UL = 5;

  private static final int WAN_UL_MEASURED = 51;

  public static final Table<String, String, String> SERVERS = HashBasedTable.create();

  private int defaultColorForPaths;

  private TextView errorPlaceholder;

  private View internetRouterLink;

  private SpeedTestResultRecyclerViewAdapter mAdapter;

  private FabButton mCancelFab;

  private DDWRTCompanionDAO mDao;

  private File mFileToShare;

  private SharedPreferences mGlobalPreferences;

  @Nullable private InterstitialAd mInterstitialAd;

  private boolean mIsThemeLight;

  private RecyclerView.LayoutManager mLayoutManager;

  private RadioGroup mMeasurementUnitRadioGroup;

  private BroadcastReceiver mMessageReceiver;

  private Router mOriginalRouter;

  private Long[] mPossibleFileSizes;

  private boolean mPreviousSettingAutoMeasurements;

  private String mPreviousSettingAutoMeasurementsSchedule;

  private RecyclerViewEmptySupport mRecyclerView;

  private Router mRouterCopy;

  private SharedPreferences mRouterPreferences;

  private FabButton mRunFab;

  //    private AtomicBoolean mSpeedTestRunning;
  private ImageView mServerCountryFlag;

  private TextView mServerLabel;

  private SpeedTestAsyncTask mSpeedTestAsyncTask;

  private TextView mSpeedTestWanDlRaw;

  private TextView mSpeedTestWanUlRaw;

  private TextView mSpeedtestLatencyTitle;

  private TextView mSpeedtestWanDlTitle;

  private TextView mSpeedtestWifiEfficiencyTitle;

  //    private TextView mSpeedtestWanUlTitle;
  private TextView mSpeedtestWifiSpeedTitle;

  private TextView[] mTitleTextViews;

  private Toolbar mToolbar;

  private TextView mWanDlTextView;

  private TextView mWanUlTextView;

  private boolean mWithCurrentConnectionTesting;

  private TextView noticeTextView;

  private Menu optionsMenu;

  private View routerLanLink;

  @NonNull
  public static String toHumanReadableSize(
      @NonNull final Context context, @NonNull final Router router, final long bytes) {
    return toHumanReadableSize(router.getPreferences(context), bytes);
  }

  @NonNull
  public static String toHumanReadableSize(
      @Nullable final SharedPreferences routerPreferences, final long bytes) {
    if (routerPreferences == null) {
      return "";
    }
    return UNIT_BIT.equals(
            routerPreferences.getString(ROUTER_SPEED_TEST_MEASUREMENT_UNIT, UNIT_BYTE))
        ? BitUnit.format(bytes * 8L)
        : DecimalByteUnit.format(bytes);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final Intent intent = getIntent();

    final String routerSelectedUuid =
        intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);
    if (isNullOrEmpty(routerSelectedUuid)
        || (mOriginalRouter =
                RouterManagementActivity.Companion.getDao(this).getRouter(routerSelectedUuid))
            == null) {
      Toast.makeText(this, "Missing Router - might have been removed?", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    mIsThemeLight = ColorUtils.Companion.isThemeLight(this);

    ColorUtils.Companion.setAppTheme(this, mOriginalRouter.getRouterFirmware(), false);

    //        if (mIsThemeLight) {
    //            //Light
    //            setTheme(R.style.AppThemeLight);
    ////            getWindow().getDecorView()
    ////                    .setBackgroundColor(ContextCompat.getColor(this,
    ////                            android.R.color.white));
    //        } else {
    //            //Default is Dark
    //            setTheme(R.style.AppThemeDark);
    //        }

    setContentView(R.layout.activity_speedtest);

    //        mSpeedTestRunning = new AtomicBoolean(false);

    // Establish a brand-new connection to the Router
    mRouterCopy = new Router(this, mOriginalRouter).setUuid(UUID.randomUUID().toString());
    //        //In order for router avatar to be correctly fetched, we have to copy router model
    //        //from original preferences
    //        mRouterCopy.setRouterModel(Router.getRouterModel(this, mOriginalRouter));

    final String[] maxFileSizeValuesStrArr =
        getResources().getStringArray(R.array.routerSpeedTestMaxFileSize_values);
    mPossibleFileSizes = new Long[maxFileSizeValuesStrArr.length];
    int i = 0;
    try {
      for (final String maxFileSizeValuesStr : maxFileSizeValuesStrArr) {
        mPossibleFileSizes[i++] = Long.parseLong(maxFileSizeValuesStr);
      }
    } catch (final NumberFormatException nfe) {
      nfe.printStackTrace();
      Utils.reportException(this, nfe);
      Toast.makeText(this, "Internal error - thanks for reporting the issue.", Toast.LENGTH_SHORT)
          .show();
      finish();
      return;
    }
    if (mPossibleFileSizes.length == 0) {
      mPossibleFileSizes = new Long[] {100L};
      Utils.reportException(
          this,
          new SpeedTestException("R.array.routerSpeedTestMaxFileSize_values is NULL or empty"));
    }
    Arrays.sort(mPossibleFileSizes);

    mRouterPreferences = getSharedPreferences(routerSelectedUuid, Context.MODE_PRIVATE);
    mGlobalPreferences =
        getSharedPreferences(
            RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);

    this.mPreviousSettingAutoMeasurements =
        mRouterPreferences.getBoolean(ROUTER_SPEED_TEST_AUTO_MEASUREMENTS, false);
    this.mPreviousSettingAutoMeasurementsSchedule =
        mRouterPreferences.getString(
            ROUTER_SPEED_TEST_AUTO_MEASUREMENTS_SCHEDULE, RouterSpeedTestAutoRunnerJob.DAILY);

    this.mMessageReceiver = new NetworkChangeReceiver(this);

    mInterstitialAd =
        AdUtils.requestNewInterstitial(
            this, R.string.interstitial_ad_unit_id_transtion_to_wan_monthly_chart);

    AdUtils.buildAndDisplayAdViewIfNeeded(this, findViewById(R.id.router_speedtest_adView));

    mToolbar = findViewById(R.id.routerSpeedTestToolbar);
    if (mToolbar != null) {
      mToolbar.setTitle("Speed Test");
      updateToolbarTitleAndSubTitle();

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

    Router.doFetchAndSetRouterAvatarInImageView(
        this, mRouterCopy, findViewById(R.id.speedtest_router_imageView));

    mWanDlTextView = findViewById(R.id.speedtest_internet_dl_speed);
    mSpeedTestWanDlRaw = findViewById(R.id.speedtest_internet_dl_speed_raw_bytes);

    mSpeedTestWanUlRaw = findViewById(R.id.speedtest_internet_ul_speed_raw_bytes);
    mWanUlTextView = findViewById(R.id.speedtest_internet_ul_speed);

    mDao = RouterManagementActivity.Companion.getDao(this);

    mRecyclerView = findViewById(R.id.speedtest_results_recycler_view);

    // use this setting to improve performance if you know that changes
    // in content do not change the layout size of the RecyclerView
    // allows for optimizations if all items are of the same size:
    mRecyclerView.setHasFixedSize(true);

    // use a linear layout manager
    mLayoutManager = new LinearLayoutManager(this);
    mLayoutManager.scrollToPosition(0);
    mRecyclerView.setLayoutManager(mLayoutManager);

    final View emptyView = findViewById(R.id.empty_view);
    final TextView emptyTextView = emptyView.findViewById(R.id.empty_view_text);
    if (mIsThemeLight) {
      emptyTextView.setTextColor(ContextCompat.getColor(this, R.color.black));
    } else {
      emptyTextView.setTextColor(ContextCompat.getColor(this, R.color.white));
    }
    mRecyclerView.setEmptyView(emptyView);

    // specify an adapter (see also next example)
    mAdapter =
        new SpeedTestResultRecyclerViewAdapter(this, mOriginalRouter)
            .setSpeedTestResults(mDao.getSpeedTestResultsByRouter(mOriginalRouter.getUuid()));

    mRecyclerView.setAdapter(mAdapter);

    final RecyclerView.ItemDecoration itemDecoration =
        new DividerItemDecoration(this, LinearLayoutManager.VERTICAL);
    mRecyclerView.addItemDecoration(itemDecoration);

    mSpeedtestLatencyTitle = findViewById(R.id.speedtest_latency_title);
    mSpeedtestWanDlTitle = findViewById(R.id.speedtest_wan_dl_title);
    //        mSpeedtestWanUlTitle = (TextView) findViewById(R.id.speedtest_wan_ul_title);
    mSpeedtestWifiSpeedTitle = findViewById(R.id.speedtest_lan_title);
    mSpeedtestWifiEfficiencyTitle = findViewById(R.id.speedtest_wifi_efficiency_title);

    mTitleTextViews =
        new TextView[] {
          mSpeedtestLatencyTitle, mSpeedtestWanDlTitle,
          //                mSpeedtestWanUlTitle,
          mSpeedtestWifiSpeedTitle, mSpeedtestWifiEfficiencyTitle
        };

    mServerCountryFlag = findViewById(R.id.speedtest_server_country_flag);
    mServerLabel = findViewById(R.id.speedtest_server);

    errorPlaceholder = findViewById(R.id.router_speedtest_error);
    errorPlaceholder.setVisibility(View.GONE);

    noticeTextView = findViewById(R.id.router_speedtest_notice);

    internetRouterLink = findViewById(R.id.speedtest_internet_line);
    routerLanLink = findViewById(R.id.speedtest_router_lan_path_vertical);

    defaultColorForPaths =
        ContextCompat.getColor(SpeedTestActivity.this, R.color.network_link_color);

    final ImageButton speedtestResultsRefreshImageButton =
        findViewById(R.id.speedtest_results_refresh);
    final ImageButton speedtestResultsClearAllImageButton =
        findViewById(R.id.speedtest_results_clear_all);

    if (mIsThemeLight) {
      speedtestResultsRefreshImageButton.setImageDrawable(
          ContextCompat.getDrawable(this, R.drawable.ic_refresh_black_24dp));
      speedtestResultsClearAllImageButton.setImageDrawable(
          ContextCompat.getDrawable(this, R.drawable.ic_clear_all_black_24dp));
    } else {
      speedtestResultsRefreshImageButton.setImageDrawable(
          ContextCompat.getDrawable(this, R.drawable.ic_refresh_white_24dp));
      speedtestResultsClearAllImageButton.setImageDrawable(
          ContextCompat.getDrawable(this, R.drawable.ic_clear_all_white_24dp));
    }

    speedtestResultsRefreshImageButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            refreshSpeedTestResults();
          }
        });

    mMeasurementUnitRadioGroup = findViewById(R.id.speedtest_measurement_unit);
    mMeasurementUnitRadioGroup.setOnCheckedChangeListener(
        new OnCheckedChangeListener() {
          @SuppressLint("SetTextI18n")
          @Override
          public void onCheckedChanged(final RadioGroup group, final int checkedId) {
            final String value;
            if (checkedId == R.id.speedtest_measurement_unit_bits) {
              value = UNIT_BIT;

            } else if (checkedId == R.id.speedtest_measurement_unit_bytes) {
              value = UNIT_BYTE;

            } else {
              value = null;
            }
            if (value != null) {
              mRouterPreferences
                  .edit()
                  .putString(ROUTER_SPEED_TEST_MEASUREMENT_UNIT, value)
                  .apply();
              refreshSpeedTestResults();
              final CharSequence currentRawWanDl = mSpeedTestWanDlRaw.getText();
              if (!TextUtils.isEmpty(currentRawWanDl)) {
                try {
                  mWanDlTextView.setText(
                      toHumanReadableSize(Long.valueOf(currentRawWanDl.toString())) + PER_SEC);
                } catch (final NumberFormatException nfe) {
                  // No worries
                  ReportingUtils.reportException(SpeedTestActivity.this, nfe);
                }
              }
              final CharSequence currentRawWanUl = mSpeedTestWanUlRaw.getText();
              if (!TextUtils.isEmpty(currentRawWanUl)) {
                try {
                  mWanUlTextView.setText(
                      toHumanReadableSize(Long.valueOf(currentRawWanUl.toString())) + PER_SEC);
                } catch (final NumberFormatException nfe) {
                  // No worries
                  ReportingUtils.reportException(SpeedTestActivity.this, nfe);
                }
              }
              Utils.requestBackup(SpeedTestActivity.this);
            }
          }
        });

    speedtestResultsClearAllImageButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            new AlertDialog.Builder(SpeedTestActivity.this)
                .setIcon(R.drawable.ic_action_alert_warning)
                .setTitle("Delete All Results?")
                .setMessage("You'll lose all Speed Test records!")
                .setCancelable(true)
                .setPositiveButton(
                    "Delete",
                    new DialogInterface.OnClickListener() {
                      @Override
                      public void onClick(final DialogInterface dialogInterface, final int i) {
                        mDao.deleteAllSpeedTestResultsByRouter(mOriginalRouter.getUuid());

                        refreshSpeedTestResults();

                        // Request Backup
                        Utils.requestBackup(SpeedTestActivity.this);
                      }
                    })
                .setNegativeButton(
                    "Cancel",
                    new DialogInterface.OnClickListener() {
                      @Override
                      public void onClick(DialogInterface dialogInterface, int i) {
                        // Cancelled - nothing more to do!
                      }
                    })
                .create()
                .show();
          }
        });

    mRunFab = findViewById(R.id.speedtest_run_action);
    mCancelFab = findViewById(R.id.speedtest_cancel_action);

    mCancelFab.setVisibility(View.GONE);
    mRunFab.setVisibility(View.VISIBLE);

    mRunFab.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            final boolean isDemoRouter = Utils.isDemoRouter(mOriginalRouter);
            if (isDemoRouter || BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
              if (mDao.getSpeedTestResultsByRouter(mOriginalRouter.getUuid()).size()
                  >= MAX_ROUTER_SPEEDTEST_RESULTS_FREE_VERSION) {
                if (isDemoRouter) {
                  Toast.makeText(
                          SpeedTestActivity.this,
                          "You cannot have more than "
                              + MAX_ROUTER_SPEEDTEST_RESULTS_FREE_VERSION
                              + " Speed Test results for the Demo Router",
                          Toast.LENGTH_SHORT)
                      .show();
                } else {
                  Utils.displayUpgradeMessage(SpeedTestActivity.this, "Save more SpeedTest runs");
                }
                return;
              }
            }
            onRefresh();
          }
        });
    mCancelFab.setOnClickListener(
        v -> {
          if (mSpeedTestAsyncTask != null) {
            mSpeedTestAsyncTask.cancelAction();
          } else {
            mCancelFab.setVisibility(View.GONE);
            mRunFab.setVisibility(View.VISIBLE);
          }
        });

    final String mRouterPreferencesString =
        mRouterPreferences.getString(ROUTER_SPEED_TEST_SERVER, ROUTER_SPEED_TEST_SERVER_RANDOM);
    if (ROUTER_SPEED_TEST_SERVER_AUTO.equals(mRouterPreferencesString)) {
      mServerLabel.setText(AUTO_DETECTED);
    } else if (ROUTER_SPEED_TEST_SERVER_RANDOM.equals(mRouterPreferencesString)) {
      mServerLabel.setText(RANDOM_SELECTED);
    } else {
      refreshSpeedTestParameters(mRouterPreferencesString);
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    refreshSpeedTestResults();
  }

  @Override
  protected void onResume() {
    super.onResume();
    final String userDefinedMeasurementUnit =
        mRouterPreferences.getString(ROUTER_SPEED_TEST_MEASUREMENT_UNIT, UNIT_BYTE);
    switch (userDefinedMeasurementUnit) {
      case UNIT_BIT:
        mMeasurementUnitRadioGroup.check(R.id.speedtest_measurement_unit_bits);
        break;
      case UNIT_BYTE:
      default:
        mMeasurementUnitRadioGroup.check(R.id.speedtest_measurement_unit_bytes);
        break;
    }
    try {
      registerReceiver(mMessageReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    } catch (final Exception e) {
      Utils.reportException(this, e);
      e.printStackTrace();
    }
    // Auto-measurements
    final boolean currentSettingAutoMeasurements =
        mRouterPreferences.getBoolean(ROUTER_SPEED_TEST_AUTO_MEASUREMENTS, false);
    final String currentSettingAutoMeasurementsSchedule =
        mRouterPreferences.getString(
            ROUTER_SPEED_TEST_AUTO_MEASUREMENTS_SCHEDULE, RouterSpeedTestAutoRunnerJob.DAILY);
    if (mPreviousSettingAutoMeasurements != currentSettingAutoMeasurements
        || !currentSettingAutoMeasurementsSchedule.equals(
            mPreviousSettingAutoMeasurementsSchedule)) {
      if (mPreviousSettingAutoMeasurements != currentSettingAutoMeasurements) {
        this.mPreviousSettingAutoMeasurements = currentSettingAutoMeasurements;
      }
      if (!currentSettingAutoMeasurementsSchedule.equals(
          mPreviousSettingAutoMeasurementsSchedule)) {
        this.mPreviousSettingAutoMeasurementsSchedule = currentSettingAutoMeasurementsSchedule;
      }
      RouterSpeedTestAutoRunnerJob.schedule(
          this.mOriginalRouter.getUuid(),
          this.mPreviousSettingAutoMeasurements,
          this.mPreviousSettingAutoMeasurementsSchedule);
    }
  }

  @Override
  protected void onStop() {
    try {
      unregisterReceiver(mMessageReceiver);
    } catch (final Exception e) {
      e.printStackTrace();
    } finally {
      super.onStop();
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    // No call for super(). Bug on API Level > 11.
  }

  @Override
  protected void onDestroy() {
    try {
      unregisterReceiver(mMessageReceiver);
    } catch (final Exception e) {
      e.printStackTrace();
    } finally {
      try {
        try {
          mRouterCopy.destroyAllSessions();
        } finally {
          if (mFileToShare != null) {
            //noinspection ResultOfMethodCallIgnored
            mFileToShare.delete();
          }
        }
      } catch (final Exception e) {
        ReportingUtils.reportException(SpeedTestActivity.this, e);
      } finally {
        super.onDestroy();
      }
    }
  }

  @Override
  public void finish() {
    if (BuildConfig.WITH_ADS && mInterstitialAd != null && AdUtils.canDisplayInterstialAd(this)) {

      mInterstitialAd.setAdListener(
          new AdListener() {
            @Override
            public void onAdClosed() {
              SpeedTestActivity.super.finish();
            }

            @Override
            public void onAdOpened() {
              // Save preference
              getSharedPreferences(
                      RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY,
                      Context.MODE_PRIVATE)
                  .edit()
                  .putLong(
                      RouterCompanionAppConstants.AD_LAST_INTERSTITIAL_PREF,
                      System.currentTimeMillis())
                  .apply();
            }
          });

      if (mInterstitialAd.isLoaded()) {
        mInterstitialAd.show();
      } else {
        SpeedTestActivity.super.finish();
      }
    } else {
      super.finish();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_activity_speed_test, menu);
    this.optionsMenu = menu;
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public void onDismissEventTimeout(int event, @Nullable Bundle bundle) throws Exception {
    onRefresh();
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    final int itemId = item.getItemId();
    if (itemId == android.R.id.home) {
      onBackPressed();
      return true;
    } else if (itemId == R.id.action_feedback) {
      Utils.openFeedbackForm(this, mOriginalRouter);
      return true;
    } else if (itemId == R.id.router_speedtest_refresh) {
      final boolean isDemoRouter = Utils.isDemoRouter(mOriginalRouter);
      if (isDemoRouter || BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
        if (mDao.getSpeedTestResultsByRouter(mOriginalRouter.getUuid()).size()
            >= MAX_ROUTER_SPEEDTEST_RESULTS_FREE_VERSION) {
          if (isDemoRouter) {
            Toast.makeText(
                    SpeedTestActivity.this,
                    "You cannot have more than "
                        + MAX_ROUTER_SPEEDTEST_RESULTS_FREE_VERSION
                        + " Speed Test results for the Demo Router",
                    Toast.LENGTH_SHORT)
                .show();
          } else {
            Utils.displayUpgradeMessage(SpeedTestActivity.this, "Save more SpeedTest runs");
          }
          return true;
        }
      }
      SnackbarUtils.buildSnackbar(
          this, "Going to start Speed Test...", "Undo", Snackbar.LENGTH_SHORT, this, null, true);
      return true;
    } else if (itemId == R.id.router_speedtest_settings) { // Open Settings activity
      final Intent settingsActivity = new Intent(this, RouterSpeedTestSettingsActivity.class);
      settingsActivity.putExtra(
          RouterManagementActivity.ROUTER_SELECTED, mOriginalRouter.getUuid());
      this.startActivity(settingsActivity);
      return true;
    } else if (itemId == R.id.router_speedtest_share) {
      PermissionsUtils.requestPermissions(
          this,
          Collections.singletonList(permission.WRITE_EXTERNAL_STORAGE),
          () -> {
            final List<SpeedTestResult> speedTestResultsByRouter =
                mDao.getSpeedTestResultsByRouter(mOriginalRouter.getUuid());
            mFileToShare =
                new File(
                    getCacheDir(),
                    Utils.getEscapedFileName(
                            String.format(
                                "Speed Test Results on Router '%s'",
                                mOriginalRouter.getCanonicalHumanReadableName()))
                        + ".csv");

            final List<String> csvTextOutput = new ArrayList<>();

            try {
              final String hdr =
                  "Test Date,Server Location,WAN Ping,WAN Ping (Readable),WAN Download,WAN Download (Readable)";
              csvTextOutput.add(hdr);
              Files.write(hdr + "\n", mFileToShare, CHARSET);
              for (final SpeedTestResult speedTestResult : speedTestResultsByRouter) {
                if (speedTestResult == null) {
                  continue;
                }
                final Number wanPing = speedTestResult.getWanPing();
                final Number wanDl = speedTestResult.getWanDl();
                final Number wanUl = speedTestResult.getWanUl();

                final String speedTestLine =
                    String.format(
                        Locale.US,
                        "%s,%s,%.2f,%.2f ms,%.2f,%s%s",
                        speedTestResult.getDate(),
                        getServerLocationDisplayFromCountryCode(
                            speedTestResult.getServerCountryCode()),
                        wanPing.floatValue(),
                        wanPing.floatValue(),
                        wanDl.floatValue(),
                        toHumanReadableSize(wanDl.longValue()),
                        PER_SEC);

                csvTextOutput.add(speedTestLine);
                Files.append(speedTestLine + "\n", mFileToShare, CHARSET);
              }

              final Uri uriForFile =
                  FileProvider.getUriForFile(
                      this, RouterCompanionAppConstants.FILEPROVIDER_AUTHORITY, mFileToShare);

              final Intent sendIntent = new Intent();
              sendIntent.setAction(Intent.ACTION_SEND);
              sendIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);
              sendIntent.putExtra(
                  Intent.EXTRA_SUBJECT,
                  String.format(
                      "Speed Test Results for Router '%s'",
                      mOriginalRouter.getCanonicalHumanReadableName()));
              sendIntent.putExtra(
                  Intent.EXTRA_TEXT,
                  fromHtml(
                      String.format(
                              "%s\n\n%s",
                              Joiner.on("\n").skipNulls().join(csvTextOutput),
                              Utils.getShareIntentFooter())
                          .replaceAll("\n", "<br/>")));
              sendIntent.setDataAndType(uriForFile, "text/html");
              sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

              startActivity(
                  Intent.createChooser(sendIntent, getResources().getText(R.string.send_to)));

            } catch (IOException e) {
              ReportingUtils.reportException(SpeedTestActivity.this, e);
              Utils.displayMessage(
                  this,
                  "Failed to export file - sharing will be unavailable. Please try again later",
                  Style.ALERT);
              return null;
            }
            return null;
          },
          () -> null,
          "Storage access is required to share Speed Test Results.");
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onRefresh() {
    doPerformSpeedTest();
  }

  @Override
  public void onShowEvent(@Nullable Bundle bundle) throws Exception {}

  public void setRefreshActionButtonState(final boolean refreshing) {
    if (optionsMenu != null) {
      final MenuItem refreshItem = optionsMenu.findItem(R.id.router_speedtest_refresh);
      if (refreshItem != null) {
        if (refreshing) {
          refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
        } else {
          refreshItem.setActionView(null);
        }
      }
    }
  }

  protected void notifyDataSetChanged() {
    this.mAdapter.notifyDataSetChanged();
  }

  protected void setSpeedTestResults(List<SpeedTestResult> results) {
    this.mAdapter.setSpeedTestResults(results);
  }

  protected void updateNbSpeedTestResults(List<SpeedTestResult> speedTestResultsByRouter) {
    final int size = speedTestResultsByRouter.size();
    final TextView nbResultsView = findViewById(R.id.speedtest_results_nb_results);
    nbResultsView.setText(String.format(Locale.US, "%d", size));
  }

  private void doPerformSpeedTest() {

    mRunFab.setVisibility(View.GONE);
    mCancelFab.setVisibility(View.VISIBLE);

    setRefreshActionButtonState(true);

    refreshSpeedTestParameters(
        mRouterPreferences.getString(ROUTER_SPEED_TEST_SERVER, ROUTER_SPEED_TEST_SERVER_RANDOM));

    mSpeedTestAsyncTask = new SpeedTestAsyncTask(this);

    mSpeedTestAsyncTask.execute();
  }

  private void highlightTitleTextView(@Nullable final TextView... tvs) {
    if (tvs == null) {
      // Reset everything
      for (final TextView textView : mTitleTextViews) {
        textView.setTypeface(null, Typeface.NORMAL);
      }
    } else {
      for (final TextView tv : tvs) {
        if (tv == null) {
          continue;
        }
        tv.setTypeface(null, Typeface.BOLD);
      }
      final List<TextView> textViewList = Arrays.asList(tvs);
      for (final TextView textView : mTitleTextViews) {
        if (textViewList.contains(textView)) {
          continue;
        }
        textView.setTypeface(null, Typeface.NORMAL);
      }
    }
  }

  private void refreshServerLocationFlag(final String countryCode) {
    refreshServerLocationFlag(this, countryCode, mServerCountryFlag);
  }

  private void refreshSpeedTestParameters(@NonNull final String serverSetting) {
    runOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            //        final String routerText;
            boolean doUpdateServerTextLabel = true;

            final String routerText = getServerLocationDisplayFromCountryCode(serverSetting);
            switch (nullToEmpty(routerText)) {
              case RANDOM_SELECTED:
              case AUTO_DETECTED:
                final String serverLabelStr = mServerLabel.getText().toString();
                if (!(isNullOrEmpty(serverLabelStr) || routerText.equals(serverLabelStr))) {
                  doUpdateServerTextLabel = false;
                }
                break;
              default:
                break;
            }

            if (doUpdateServerTextLabel) {
              mServerLabel.setText(routerText);
            }
            if (!AUTO_DETECTED.equalsIgnoreCase(routerText)) {
              // Load flag in the background
              refreshServerLocationFlag(serverSetting);
            } else {
              mServerCountryFlag.setVisibility(View.GONE);
            }

            // TODO Disabled for now
            //        mWithCurrentConnectionTesting =
            //
            // mRouterPreferences.getBoolean(ROUTER_SPEED_TEST_WITH_CURRENT_CONNECTION, true);
            mWithCurrentConnectionTesting = false;

            final View devices = findViewById(R.id.speedtest_connection_devices);
            final View connectionLink = findViewById(R.id.speedtest_connection_link);

            if (mWithCurrentConnectionTesting) {
              connectionLink.setVisibility(View.VISIBLE);
              devices.setVisibility(View.VISIBLE);
            } else {
              connectionLink.setVisibility(View.GONE);
              devices.setVisibility(View.GONE);
            }
          }
        });
  }

  private void refreshSpeedTestResults() {
    final List<SpeedTestResult> speedTestResultsByRouter =
        mDao.getSpeedTestResultsByRouter(mOriginalRouter.getUuid());
    mAdapter.setSpeedTestResults(speedTestResultsByRouter);
    mAdapter.notifyDataSetChanged();
    updateNbSpeedTestResults(speedTestResultsByRouter);
  }

  private void resetAllTitleViews() {
    for (final TextView textView : mTitleTextViews) {
      textView.setTypeface(null, Typeface.NORMAL);
    }

    findViewById(R.id.speedtest_latency_pb_internet).setVisibility(View.GONE);
    findViewById(R.id.speedtest_dl_pb_internet).setVisibility(View.GONE);
    findViewById(R.id.speedtest_ul_pb_internet).setVisibility(View.GONE);
    findViewById(R.id.speedtest_pb_wifi).setVisibility(View.GONE);
    findViewById(R.id.speedtest_pb_wifi_efficiency).setVisibility(View.GONE);
  }

  @NonNull
  private String toHumanReadableSize(final long bytes) {
    return toHumanReadableSize(mRouterPreferences, bytes);
  }

  private void updateToolbarTitleAndSubTitle() {
    final String effectiveRemoteAddr =
        Router.getEffectiveRemoteAddr(mOriginalRouter, SpeedTestActivity.this);
    final Integer effectivePort = Router.getEffectivePort(mOriginalRouter, SpeedTestActivity.this);

    if (mToolbar != null) {
      mToolbar.setSubtitle(
          String.format(
              Locale.US,
              "%s (%s:%d)",
              mOriginalRouter.getDisplayName(),
              effectiveRemoteAddr,
              effectivePort));
    }
  }

  @NonNull
  protected static String getServerLocationDisplayFromCountryCode(
      @Nullable final String serverCountryCode) {
    switch (nullToEmpty(serverCountryCode)) {
      case ROUTER_SPEED_TEST_SERVER_AUTO:
        return AUTO_DETECTED;
      case ROUTER_SPEED_TEST_SERVER_RANDOM:
        // Pick one randomly
        final Set<String> rowKeySet = SERVERS.rowKeySet();
        final String randomCountryCode =
            Lists.newArrayList(rowKeySet).get(RANDOM.nextInt(rowKeySet.size()));
        if (ROUTER_SPEED_TEST_SERVER_RANDOM.equals(randomCountryCode)) {
          // Should not happen, but hey, you never know!
          return "Frankfurt (Germany)";
        }
        return getServerLocationDisplayFromCountryCode(randomCountryCode);
      case "NL":
        return "Amsterdam (The Netherlands)";
      case "IN":
        return "Chennai (India)";
      case "US-DAL":
        return "Dallas (USA)";
      case "HK":
        return "Hong Kong (China)";
      case "US-HOU":
        return "Houston (USA)";
      case "GB":
        return "London (United Kingdom)";
      case "AU-MEL":
        return "Melbourne (Australia)";
      case "IT":
        return "Milan (Italy)";
      case "CA-MON":
        return "Montreal (Canada)";
      case "FR":
        return "Paris (France)";
      case "MX":
        return "Querétaro (Mexico)";
      case "US-SJC":
        return "San Jose (USA)";
      case "US-SEA":
        return "Seattle (USA)";
      case "SG":
        return "Singapore (Singapore)";
      case "AU-SYD":
      case "AU":
        return "Sydney (Australia)";
      case "CA-TOR":
        return "Toronto (Canada)";
      case "US-WDC":
        return "Washington, D.C. (USA)";
      case "DE":
        return "Frankfurt (Germany)";
      case "US":
        return "California (USA)";
      case "BR":
        return "Sao Paulo (Brazil)";
      case "KR":
        return "Seoul (South Korea)";
      case "JP":
        return "Tokyo (Japan)";
      default:
        return isNullOrEmpty(serverCountryCode) ? "-" : serverCountryCode;
    }
  }

  protected static void refreshServerLocationFlag(
      @NonNull final Context ctx,
      @NonNull final String countryCode,
      @NonNull final ImageView imageView) {

    final List<String> countryCodeSplitted = HYPHEN_SPLITTER.splitToList(countryCode);

    ImageUtils.downloadImageFromUrl(
        ctx,
        String.format(
            "%s/%s.png",
            RouterCompanionAppConstants.COUNTRY_API_SERVER_FLAG,
            countryCodeSplitted.isEmpty() ? countryCode : countryCodeSplitted.get(0)),
        imageView,
        null,
        null,
        new Callback() {
          @Override
          public void onError(Exception e) {
            Utils.reportException(ctx, e);
            imageView.setVisibility(View.GONE);
          }

          @Override
          public void onSuccess() {
            imageView.setVisibility(View.VISIBLE);
          }
        });
  }

  @Nullable
  private static String getRemoteFileName(@Nullable final Long fileSizeKB) {
    if (fileSizeKB == null) {
      return null;
    }
    switch (fileSizeKB.intValue()) {
      case 128:
        return "128KB";
      case 256:
        return "256KB";
      case 512:
        return "512KB";
      case 1024:
        return "1MB";
      case 2048:
        return "2MB";
      case 4096:
        return "4MB";
      case 8192:
        return "8MB";
      case 16384:
        return "16MB";
      case 32768:
        return "32MB";
      case 65536:
        return "64MB";
      case 131072:
        return "128MB";
      case 262144:
        return "256MB";
      case 524288:
        return "512MB";
      case 1048576:
        return "1GB";
      default:
        return null;
    }
  }

  static {
    // TODO Those are just SoftLayer servers, but we may want to consider a much more exhaustive
    //  list: http://www.speedtest.net/speedtest-servers.php
    SERVERS.put("NL", PING_SERVER, "speedtest.ams01.softlayer.com"); // OK
    SERVERS.put("NL", HTTP_DL_URL, "http://speedtest.ams01.softlayer.com/downloads/test%s.zip");

    SERVERS.put("IN", PING_SERVER, "speedtest.che01.softlayer.com"); // OK (in router)
    SERVERS.put("IN", HTTP_DL_URL, "http://speedtest.che01.softlayer.com/downloads/test%s.zip");

    // TODO Find a way to auto-handle this, or at least in a way that does not require publishing a
    // new version of the app
    //    SERVERS.put("US-DAL", PING_SERVER, "speedtest.dal01.softlayer.com"); // UNHEALTHY
    //    SERVERS.put("US-DAL", HTTP_DL_URL,
    // "http://speedtest.dal01.softlayer.com/downloads/test%s.zip");

    SERVERS.put("DE", PING_SERVER, "speedtest.fra02.softlayer.com"); // OK
    SERVERS.put("DE", HTTP_DL_URL, "http://speedtest.fra02.softlayer.com/downloads/test%s.zip");

    SERVERS.put("HK", PING_SERVER, "speedtest.hkg02.softlayer.com"); // OK
    SERVERS.put("HK", HTTP_DL_URL, "http://speedtest.hkg02.softlayer.com/downloads/test%s.zip");

    SERVERS.put("US-HOU", PING_SERVER, "speedtest.hou02.softlayer.com"); // OK
    SERVERS.put("US-HOU", HTTP_DL_URL, "http://speedtest.hou02.softlayer.com/downloads/test%s.zip");

    SERVERS.put("GB", PING_SERVER, "speedtest.lon02.softlayer.com"); // OK
    SERVERS.put("GB", HTTP_DL_URL, "http://speedtest.lon02.softlayer.com/downloads/test%s.zip");

    SERVERS.put("AU-MEL", PING_SERVER, "speedtest.mel01.softlayer.com"); // OK
    SERVERS.put("AU-MEL", HTTP_DL_URL, "http://speedtest.mel01.softlayer.com/downloads/test%s.zip");

    SERVERS.put("IT", PING_SERVER, "speedtest.mil01.softlayer.com"); // OK
    SERVERS.put("IT", HTTP_DL_URL, "http://speedtest.mil01.softlayer.com/downloads/test%s.zip");

    SERVERS.put("CA-MON", PING_SERVER, "speedtest.mon01.softlayer.com"); // OK
    SERVERS.put("CA-MON", HTTP_DL_URL, "http://speedtest.mon01.softlayer.com/downloads/test%s.zip");

    SERVERS.put("FR", PING_SERVER, "speedtest.par01.softlayer.com"); // OK
    SERVERS.put("FR", HTTP_DL_URL, "http://speedtest.par01.softlayer.com/downloads/test%s.zip");

    SERVERS.put("MX", PING_SERVER, "speedtest.mex01.softlayer.com"); // OK
    SERVERS.put("MX", HTTP_DL_URL, "http://speedtest.mex01.softlayer.com/downloads/test%s.zip");

    SERVERS.put("US-SJC", PING_SERVER, "speedtest.sjc01.softlayer.com"); // OK
    SERVERS.put("US-SJC", HTTP_DL_URL, "http://speedtest.sjc01.softlayer.com/downloads/test%s.zip");

    SERVERS.put("BR", PING_SERVER, "speedtest.sao01.softlayer.com"); // OK
    SERVERS.put("BR", HTTP_DL_URL, "http://speedtest.sao01.softlayer.com/downloads/test%s.zip");

    SERVERS.put("US-SEA", PING_SERVER, "speedtest.sea01.softlayer.com"); // OK
    SERVERS.put("US-SEA", HTTP_DL_URL, "http://speedtest.sea01.softlayer.com/downloads/test%s.zip");

    SERVERS.put("SG", PING_SERVER, "speedtest.sng01.softlayer.com"); // OK
    SERVERS.put("SG", HTTP_DL_URL, "http://speedtest.sng01.softlayer.com/downloads/test%s.zip");

    SERVERS.put("AU-SYD", PING_SERVER, "speedtest.syd01.softlayer.com"); // OK
    SERVERS.put("AU-SYD", HTTP_DL_URL, "http://speedtest.syd01.softlayer.com/downloads/test%s.zip");

    SERVERS.put("JP", PING_SERVER, "speedtest.tok02.softlayer.com"); // OK
    SERVERS.put("JP", HTTP_DL_URL, "http://speedtest.tok02.softlayer.com/downloads/test%s.zip");

    SERVERS.put("CA-TOR", PING_SERVER, "speedtest.tor01.softlayer.com"); // OK
    SERVERS.put("CA-TOR", HTTP_DL_URL, "http://speedtest.tor01.softlayer.com/downloads/test%s.zip");

    SERVERS.put("US-WDC", PING_SERVER, "speedtest.wdc01.softlayer.com"); // OK
    SERVERS.put("US-WDC", HTTP_DL_URL, "http://speedtest.wdc01.softlayer.com/downloads/test%s.zip");
  }
}
