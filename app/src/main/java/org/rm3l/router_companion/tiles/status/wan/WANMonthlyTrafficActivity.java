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
package org.rm3l.router_companion.tiles.status.wan;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.PermissionChecker;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.router_companion.resources.MonthlyCycleItem;
import org.rm3l.router_companion.resources.WANTrafficData;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.AdUtils;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.WANTrafficUtils;
import org.rm3l.router_companion.utils.snackbar.SnackbarCallback;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.router_companion.RouterCompanionAppConstants.EMPTY_STRING;
import static org.rm3l.router_companion.RouterCompanionAppConstants.MB;
import static org.rm3l.router_companion.utils.Utils.fromHtml;

public class WANMonthlyTrafficActivity extends AppCompatActivity {

  public static final String WAN_CYCLE = "WAN_CYCLE";
  public static final String WAN_MONTHLY_TRAFFIC = "WAN Monthly Traffic";
  public static final int COMPRESSION_QUALITY = 100;
  public static final int DEFAULT_BITMAP_WIDTH = 640;
  public static final int DEFAULT_BITMAP_HEIGHT = 480;
  private static final String LOG_TAG = WANMonthlyTrafficActivity.class.getSimpleName();
  private final String[] breakdownLines = new String[31];
  private Toolbar mToolbar;
  private Router mRouter;
  private String mRouterDisplay;
  private ShareActionProvider mShareActionProvider;
  private Menu optionsMenu;
  private File mFileToShare;
  private Exception mException;
  private boolean themeLight;
  private long totalIn;
  private long totalOut;

  @Nullable private InterstitialAd mInterstitialAd;

  private MonthlyCycleItem mCycleItem;

  private DDWRTCompanionDAO dao;

  private List<WANTrafficData> wanTrafficDataBreakdown;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    dao = RouterManagementActivity.getDao(this);

    final Intent intent = getIntent();
    final String routerUuid = intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);
    final String wanCycleStr = intent.getStringExtra(WAN_CYCLE);

    if (isNullOrEmpty(routerUuid)
        || wanCycleStr == null
        || (mRouter = dao.getRouter(routerUuid)) == null) {
      Utils.reportException(this,
          new IllegalStateException("isNullOrEmpty(routerUuid) || mCycleItem == null"));
      Toast.makeText(this, "Internal Error - please try again later.", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    themeLight = ColorUtils.Companion.isThemeLight(this);
    ColorUtils.Companion.setAppTheme(this, mRouter.getRouterFirmware(), false);

    //        if (themeLight) {
    //            //Light
    //            setTheme(R.style.AppThemeLight);
    ////            getWindow().getDecorView()
    ////                    .setBackgroundColor(ContextCompat.getColor(this,
    ////                            android.R.color.white));
    //        } else {
    //            //Default is Dark
    //            setTheme(R.style.AppThemeDark);
    //        }

    setContentView(R.layout.tile_status_wan_monthly_traffic_chart);

    final String mRouterName = mRouter.getName();
    final boolean mRouterNameNullOrEmpty = isNullOrEmpty(mRouterName);
    mRouterDisplay = "";
    if (!mRouterNameNullOrEmpty) {
      mRouterDisplay = (mRouterName + " (");
    }
    mRouterDisplay += mRouter.getRemoteIpAddress();
    if (!mRouterNameNullOrEmpty) {
      mRouterDisplay += ")";
    }

    try {
      mCycleItem = new Gson().fromJson(wanCycleStr, MonthlyCycleItem.class);
    } catch (final JsonSyntaxException jse) {
      Crashlytics.log(Log.ERROR, LOG_TAG,
          "JsonSyntaxException while trying to read wanCycleStr: " + wanCycleStr);
      jse.printStackTrace();
      Utils.reportException(this, jse);
      Toast.makeText(this, "Internal Error - please try again later.", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }
    mCycleItem.setContext(this);

    if (themeLight) {
      final Resources resources = getResources();
      //            getWindow().getDecorView()
      //                    .setBackgroundColor(
      //                            ContextCompat.getColor(this,
      //                                    android.R.color.white));

    }

    mInterstitialAd = AdUtils.requestNewInterstitial(this,
        R.string.interstitial_ad_unit_id_transtion_to_wan_monthly_chart);

    AdUtils.buildAndDisplayAdViewIfNeeded(this,
        (AdView) findViewById(R.id.tile_status_wan_monthly_traffic_chart_view_adView));

    mToolbar = (Toolbar) findViewById(R.id.tile_status_wan_monthly_traffic_chart_view_toolbar);
    if (mToolbar != null) {
      mToolbar.setTitle(WAN_MONTHLY_TRAFFIC + ": " + mCycleItem.getLabelWithYears());
      mToolbar.setSubtitle(
          String.format("%s (%s:%d)", mRouter.getDisplayName(), mRouter.getRemoteIpAddress(),
              mRouter.getRemotePort()));
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

    wanTrafficDataBreakdown =
        WANTrafficUtils.getWANTrafficDataByRouterBetweenDates(dao, mRouter.getUuid(),
            mCycleItem.getStart(), mCycleItem.getEnd());

    doPaintBarChart();
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    //No call for super(). Bug on API Level > 11.
  }

  @Override public void finish() {
    if (BuildConfig.WITH_ADS && mInterstitialAd != null && AdUtils.canDisplayInterstialAd(this)) {

      mInterstitialAd.setAdListener(new AdListener() {
        @Override public void onAdClosed() {
          WANMonthlyTrafficActivity.super.finish();
        }

        @Override public void onAdOpened() {
          //Save preference
          getSharedPreferences(RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY,
              Context.MODE_PRIVATE).edit()
              .putLong(RouterCompanionAppConstants.AD_LAST_INTERSTITIAL_PREF,
                  System.currentTimeMillis())
              .apply();
        }
      });

      if (mInterstitialAd.isLoaded()) {
        mInterstitialAd.show();
      } else {
        WANMonthlyTrafficActivity.super.finish();
      }
    } else {
      super.finish();
    }
  }

  private void doPaintBarChart() {
    final View loadingView = findViewById(R.id.tile_status_wan_monthly_traffic_chart_loading_view);
    loadingView.setVisibility(View.VISIBLE);

    final LinearLayout chartPlaceholderView =
        (LinearLayout) findViewById(R.id.tile_status_wan_monthly_traffic_chart_placeholder);

    try {

      final int size = wanTrafficDataBreakdown.size();
      if (size == 0) {
        Toast.makeText(this, "No Data or an error occurred!", Toast.LENGTH_SHORT).show();
        finish();
        return;
      }

      final String[] days = new String[size];
      final double[] inData = new double[size];
      final double[] outData = new double[size];

      double maxY = 0;
      int maxX = 31;

      // Creating an  XYSeries for Inbound
      final XYSeries inboundSeries = new XYSeries("Inbound");
      // Creating an  XYSeries for Outbound
      final XYSeries outboundSeries = new XYSeries("Outbound");

      int i = 0;
      totalIn = 0;
      totalOut = 0;

      for (final WANTrafficData wanTrafficData : wanTrafficDataBreakdown) {
        if (wanTrafficData == null) {
          continue;
        }
        final Double in = wanTrafficData.getTraffIn().doubleValue();
        final Double out = wanTrafficData.getTraffOut().doubleValue();

        days[i] = wanTrafficData.getDate();

        maxY = Math.max(maxY, Math.max(in, out));

        inData[i] = in;
        inboundSeries.add(i, inData[i]);

        outData[i] = out;
        outboundSeries.add(i, outData[i]);

        totalIn += in;
        totalOut += out;

        final long inBytes = in.longValue() * MB;
        final long outBytes = out.longValue() * MB;

        breakdownLines[i] =
            String.format("- Day %d (%s): Inbound = %d B (%s) / Outbound = %d B (%s)", i + 1,
                wanTrafficData.getDate(), inBytes,
                org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(inBytes)
                    .replace("bytes", "B"), outBytes,
                org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(outBytes)
                    .replace("bytes", "B"));

        i++;

        if (i >= 31) {
          break;
        }
      }

      //            for (final Map.Entry<Integer, ArrayList<Double>> dailyTraffMapEntry : mTrafficDataForMonth.entrySet()) {
      //                final ArrayList<Double> dailyTraffMapEntryValue = dailyTraffMapEntry.getValue();
      //                if (dailyTraffMapEntryValue == null || dailyTraffMapEntryValue.size() < 2) {
      //                    continue;
      //                }
      //                final Double in = dailyTraffMapEntryValue.get(0);
      //                final Double out = dailyTraffMapEntryValue.get(1);
      //                if (in == null || out == null) {
      //                    continue;
      //                }
      //                // Adding data to In and Out Series
      //                days[i] = dailyTraffMapEntry.getKey();
      //
      //                inData[i] = in;
      //                inboundSeries.add(i, inData[i]);
      //                totalIn += in;
      //
      //                outData[i] = out;
      //                outboundSeries.add(i, outData[i]);
      //                totalOut += out;
      //
      ////                maxX = Math.max(maxX, days[i]);
      //                maxY = Math.max(maxY, Math.max(in, out));
      //
      //                final long inBytes = in.longValue() * MB;
      //                final long outBytes = out.longValue() * MB;
      //
      //                breakdownLines[i] = String.format("- Day %d: Inbound = %d B (%s) / Outbound = %d B (%s)",
      //                        i + 1,
      //                        inBytes, org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(inBytes).replace("bytes", "B"),
      //                        outBytes, org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(outBytes).replace("bytes", "B"));
      //
      //                i++;
      //
      //                if (i >= 31) {
      //                    break;
      //                }
      //            }

      final Resources resources = getResources();

      // Creating a dataset to hold each series
      final XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
      // Adding inbound Series to the dataset
      dataset.addSeries(inboundSeries);
      // Adding outbound Series to dataset
      dataset.addSeries(outboundSeries);

      // Creating XYSeriesRenderer to customize inboundSeries
      final XYSeriesRenderer inboundRenderer = new XYSeriesRenderer();
      inboundRenderer.setColor(ColorUtils.Companion.getColor("WAN_TRAFFIC_BAR_IN"));
      inboundRenderer.setFillPoints(true);
      inboundRenderer.setLineWidth(2);
      inboundRenderer.setDisplayChartValues(false);
      inboundRenderer.setDisplayChartValuesDistance(5); //setting chart value distance

      // Creating XYSeriesRenderer to customize outboundSeries
      final XYSeriesRenderer outboundRenderer = new XYSeriesRenderer();
      outboundRenderer.setColor(ColorUtils.Companion.getColor("WAN_TRAFFIC_BAR_OUT"));
      outboundRenderer.setFillPoints(true);
      outboundRenderer.setLineWidth(2);
      outboundRenderer.setDisplayChartValues(false);

      // Creating a XYMultipleSeriesRenderer to customize the whole chart
      final XYMultipleSeriesRenderer multiRenderer = new XYMultipleSeriesRenderer();
      multiRenderer.setOrientation(XYMultipleSeriesRenderer.Orientation.HORIZONTAL);
      multiRenderer.setChartTitle(String.format("Monthly Cycle: %s / Total IN: %s / Total OUT: %s",
          mCycleItem.getLabelWithYears(),
          org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(totalIn * MB)
              .replace("bytes", "B"),
          org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(totalOut * MB)
              .replace("bytes", "B")));
      multiRenderer.setXTitle("Days");
      multiRenderer.setYTitle("Traffic");
      multiRenderer.setZoomButtonsVisible(false);
      multiRenderer.setLabelsColor(
          ContextCompat.getColor(this, themeLight ? R.color.black : R.color.theme_accent_1_light));

      //Add custom labels for the values we have here
      //setting no of values to display in y axis
      multiRenderer.setYLabels(0);
      if (maxY != 0) {
        multiRenderer.addYTextLabel(maxY,
            org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(
                Double.valueOf(maxY * MB).longValue()).replace("bytes", "B"));
        multiRenderer.addYTextLabel(3 * maxY / 4,
            org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(
                Double.valueOf(3 * maxY * MB / 4).longValue()).replace("bytes", "B"));
        multiRenderer.addYTextLabel(maxY / 2,
            org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(
                Double.valueOf(maxY * MB / 2).longValue()).replace("bytes", "B"));
        multiRenderer.addYTextLabel(maxY / 4,
            org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(
                Double.valueOf(maxY * MB / 4).longValue()).replace("bytes", "B"));
      }

      multiRenderer.setXLabelsAngle(70f);
      multiRenderer.setXLabels(0);
      for (int d = 0; d < days.length; d++) {
        //Add labels every 5 days
        multiRenderer.addXTextLabel(d, (d > 0 && (d % 5 == 0)) ? days[d] : EMPTY_STRING);
      }

      //setting text size of the title
      multiRenderer.setChartTitleTextSize(35);
      //setting text size of the axis title
      multiRenderer.setAxisTitleTextSize(25);
      //setting text size of the graph label
      multiRenderer.setLabelsTextSize(25);
      multiRenderer.setLegendTextSize(25);
      //setting pan enablity which uses graph to move on both axis
      multiRenderer.setPanEnabled(false, false);
      //setting click false on graph
      multiRenderer.setClickEnabled(false);
      //setting lines to display on y axis
      multiRenderer.setShowGridY(false);
      //setting lines to display on x axis
      multiRenderer.setShowGridX(false);
      //setting legend to fit the screen size
      multiRenderer.setFitLegend(true);
      //setting displaying line on grid
      multiRenderer.setShowGrid(false);
      //setting zoom
      multiRenderer.setZoomEnabled(false, false);
      //setting external zoom functions to false
      //            multiRenderer.setZoomRate(1.1f);
      multiRenderer.setExternalZoomEnabled(false);
      //setting displaying lines on graph to be formatted(like using graphics)
      multiRenderer.setAntialiasing(true);
      //setting to in scroll to false
      multiRenderer.setInScroll(false);
      //setting x axis label align
      multiRenderer.setXLabelsAlign(Paint.Align.CENTER);
      //setting y axis label to align
      multiRenderer.setYLabelsAlign(Paint.Align.LEFT);
      //setting text style
      multiRenderer.setTextTypeface("sans_serif", Typeface.NORMAL);
      // setting y axis max value, Since i'm using static values inside the graph so i'm setting y max value to 4000.
      // if you use dynamic values then get the max y value and set here
      multiRenderer.setYAxisMax(maxY);
      //setting used to move the graph on xaxiz to .5 to the right
      multiRenderer.setXAxisMin(-1);
      //setting max values to be display in x axis
      multiRenderer.setXAxisMax(maxX - 1);
      //setting bar size or space between two bars
      multiRenderer.setBarSpacing(0.5);
      //Setting background color of the graph to transparent
      multiRenderer.setBackgroundColor(Color.TRANSPARENT);
      //Setting margin color of the graph to transparent
      multiRenderer.setMarginsColor(ContextCompat.getColor(this, R.color.transparent_background));
      multiRenderer.setApplyBackgroundColor(true);

      //setting the margin size for the graph in the order top, left, bottom, right
      multiRenderer.setMargins(new int[] { 30, 30, 30, 30 });

      //            multiRenderer.setLabelsTextSize(30f);
      final int blackOrWhite = ContextCompat.getColor(this,
          ColorUtils.Companion.isThemeLight(this) ? R.color.black : R.color.white);
      multiRenderer.setAxesColor(blackOrWhite);
      multiRenderer.setXLabelsColor(blackOrWhite);
      multiRenderer.setYLabelsColor(0, blackOrWhite);

      // Adding inboundRenderer and outboundRenderer to multipleRenderer
      // Note: The order of adding dataseries to dataset and renderers to multipleRenderer
      // should be same
      multiRenderer.addSeriesRenderer(inboundRenderer);
      multiRenderer.addSeriesRenderer(outboundRenderer);

      final GraphicalView chartView =
          ChartFactory.getBarChartView(this, dataset, multiRenderer, BarChart.Type.DEFAULT);
      //                            chartView.repaint();
      chartPlaceholderView.removeAllViews();
      chartPlaceholderView.addView(chartView);

      chartPlaceholderView.setVisibility(View.VISIBLE);
      loadingView.setVisibility(View.GONE);
      findViewById(R.id.tile_status_wan_monthly_traffic_chart_error).setVisibility(View.GONE);
    } catch (final Exception e) {
      mException = e;
      e.printStackTrace();
      Utils.reportException(null, e);
      findViewById(R.id.tile_status_wan_monthly_traffic_chart_error).setVisibility(View.VISIBLE);
      loadingView.setVisibility(View.GONE);
      chartPlaceholderView.setVisibility(View.GONE);
      if (optionsMenu != null) {
        optionsMenu.findItem(R.id.tile_status_wan_monthly_traffic_share).setEnabled(false);
      }
    }
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.tile_status_wan_monthly_traffic_chart_options, menu);

    this.optionsMenu = menu;

    //Permission requests
    final int rwExternalStoragePermissionCheck =
        PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    if (rwExternalStoragePermissionCheck != PackageManager.PERMISSION_GRANTED) {
      // Should we show an explanation?
      if (ActivityCompat.shouldShowRequestPermissionRationale(this,
          Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
        // Show an explanation to the user *asynchronously* -- don't block
        // this thread waiting for the user's response! After the user
        // sees the explanation, try again to request the permission.

        SnackbarUtils.buildSnackbar(this, "Storage access is required to share WAN Traffic Data.",
            "OK", Snackbar.LENGTH_INDEFINITE, new SnackbarCallback() {
              @Override public void onShowEvent(@Nullable Bundle bundle) throws Exception {

              }

              @Override public void onDismissEventSwipe(int event, @Nullable Bundle bundle)
                  throws Exception {

              }

              @Override public void onDismissEventActionClick(int event, @Nullable Bundle bundle)
                  throws Exception {
                //Request permission
                ActivityCompat.requestPermissions(WANMonthlyTrafficActivity.this,
                    new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                    RouterCompanionAppConstants.Permissions.STORAGE);
              }

              @Override public void onDismissEventTimeout(int event, @Nullable Bundle bundle)
                  throws Exception {

              }

              @Override public void onDismissEventManual(int event, @Nullable Bundle bundle)
                  throws Exception {

              }

              @Override public void onDismissEventConsecutive(int event, @Nullable Bundle bundle)
                  throws Exception {

              }
            }, null, true);
      } else {
        // No explanation needed, we can request the permission.
        ActivityCompat.requestPermissions(this,
            new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
            RouterCompanionAppConstants.Permissions.STORAGE);
        // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
        // app-defined int constant. The callback method gets the
        // result of the request.
      }
    }

        /* Getting the actionprovider associated with the menu item whose id is share */
    final MenuItem shareMenuItem = menu.findItem(R.id.tile_status_wan_monthly_traffic_share);
    shareMenuItem.setEnabled(mException == null);

    mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareMenuItem);
    if (mShareActionProvider == null) {
      mShareActionProvider = new ShareActionProvider(this);
      MenuItemCompat.setActionProvider(shareMenuItem, mShareActionProvider);
    }

    final View viewToShare = findViewById(R.id.tile_status_wan_monthly_traffic_chart_placeholder);
    //Construct Bitmap and share it
    final int width = viewToShare.getWidth();
    final int height = viewToShare.getHeight();
    final Bitmap bitmapToExport = Bitmap.createBitmap(width > 0 ? width : DEFAULT_BITMAP_WIDTH,
        height > 0 ? height : DEFAULT_BITMAP_HEIGHT, Bitmap.Config.ARGB_8888);
    final Canvas canvas = new Canvas(bitmapToExport);
    viewToShare.draw(canvas);

    if (PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        == PackageManager.PERMISSION_GRANTED) {

      mFileToShare = new File(getCacheDir(), Utils.getEscapedFileName(
          String.format("WAN Monthly Traffic for '%s' on Router '%s'",
              mCycleItem.getLabelWithYears(), nullToEmpty(mRouterDisplay))) + ".png");
      OutputStream outputStream = null;
      try {
        outputStream = new BufferedOutputStream(new FileOutputStream(mFileToShare, false));
        bitmapToExport.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY, outputStream);
        outputStream.flush();
      } catch (IOException e) {
        e.printStackTrace();
        Utils.displayMessage(this, getString(R.string.internal_error_please_try_again),
            Style.ALERT);
      } finally {
        try {
          if (outputStream != null) {
            outputStream.close();
          }
        } catch (IOException e) {
          e.printStackTrace();
          //No Worries
        }
      }

      setShareFile(mFileToShare);
    }

    return super.onCreateOptionsMenu(menu);
  }

  @Override public void onRequestPermissionsResult(int requestCode, String permissions[],
      int[] grantResults) {

    switch (requestCode) {
      case RouterCompanionAppConstants.Permissions.STORAGE: {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          // permission was granted, yay!
          Crashlytics.log(Log.DEBUG, LOG_TAG, "Yay! Permission granted for #" + requestCode);
          if (optionsMenu != null) {
            final MenuItem menuItem =
                optionsMenu.findItem(R.id.tile_status_wan_monthly_traffic_share);
            menuItem.setEnabled(true);
          }
        } else {
          // permission denied, boo! Disable the
          // functionality that depends on this permission.
          Crashlytics.log(Log.WARN, LOG_TAG, "Boo! Permission denied for #" + requestCode);
          Utils.displayMessage(this, "Sharing of WAN Traffic Data will be unavailable", Style.INFO);
          if (optionsMenu != null) {
            final MenuItem menuItem =
                optionsMenu.findItem(R.id.tile_status_wan_monthly_traffic_share);
            menuItem.setEnabled(false);
          }
        }
        return;
      }
      default:
        break;
    }
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      // Respond to the action bar's Up/Home button
      case android.R.id.home:
        onBackPressed();
        return true;

      case R.id.action_feedback:
        Utils.openFeedbackForm(this, mRouter);
        //                final Intent intent = new Intent(WANMonthlyTrafficActivity.this, FeedbackActivity.class);
        //                intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouter.getUuid());
        //                final File screenshotFile = new File(getCacheDir(), "feedback_screenshot.png");
        //                ViewGroupUtils.exportViewToFile(WANMonthlyTrafficActivity.this, getWindow().getDecorView(), screenshotFile);
        //                intent.putExtra(FeedbackActivity.SCREENSHOT_FILE, screenshotFile.getAbsolutePath());
        //                intent.putExtra(FeedbackActivity.CALLER_ACTIVITY, this.getClass().getCanonicalName());
        //                startActivity(intent);
        ////                Utils.buildFeedbackDialog(this, true);
        return true;

      default:
        break;
    }
    return super.onOptionsItemSelected(item);
  }

  // Call to update the share intent
  private void setShareIntent(Intent shareIntent) {
    if (mShareActionProvider != null) {
      mShareActionProvider.setShareIntent(shareIntent);
    }
  }

  private void setShareFile(File file) {
    if (mShareActionProvider == null) {
      return;
    }

    final Uri uriForFile =
        FileProvider.getUriForFile(this, RouterCompanionAppConstants.FILEPROVIDER_AUTHORITY, file);

    mShareActionProvider.setOnShareTargetSelectedListener(
        new ShareActionProvider.OnShareTargetSelectedListener() {
          @Override
          public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
            grantUriPermission(intent.getComponent().getPackageName(), uriForFile,
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return true;
          }
        });

    final long totalOutBytes = totalOut * MB;
    final long totalInBytes = totalIn * MB;

    final Intent sendIntent = new Intent();
    sendIntent.setAction(Intent.ACTION_SEND);
    sendIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);
    sendIntent.setType("text/html");
    sendIntent.putExtra(Intent.EXTRA_SUBJECT,
        String.format("WAN Monthly Traffic for Router '%s': %s", mRouterDisplay,
            mCycleItem.getLabelWithYears()));
    sendIntent.putExtra(Intent.EXTRA_TEXT, fromHtml(String.format(
        "Traffic Breakdown\n\n>>> Total Inbound: %d B (%s) / Total Outbound: %d B (%s) <<<\n\n%s"
            + "%s", totalInBytes, org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(
            Double.valueOf(totalInBytes).longValue()).replace("bytes", "B"), totalOutBytes,
        org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(
            Double.valueOf(totalOutBytes).longValue()).replace("bytes", "B"),
        Joiner.on("\n").skipNulls().join(breakdownLines), Utils.getShareIntentFooter())
        .replaceAll("\n", "<br/>")));

    sendIntent.setData(uriForFile);
    //        sendIntent.setType("image/png");
    sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    setShareIntent(sendIntent);
  }

  @Override protected void onDestroy() {
    if (mFileToShare != null) {
      //noinspection ResultOfMethodCallIgnored
      mFileToShare.delete();
    }
    super.onDestroy();
  }
}
