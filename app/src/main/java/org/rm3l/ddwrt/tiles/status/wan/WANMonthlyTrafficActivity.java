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
package org.rm3l.ddwrt.tiles.status.wan;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.content.FileProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.common.base.Joiner;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;

public class WANMonthlyTrafficActivity extends ActionBarActivity {

    public static final String WAN_MONTHLY_TRAFFIC = "WAN Monthly Traffic";
    public static final String MONTHLY_TRAFFIC_DATA_UNSORTED = "MONTHLY_TRAFFIC_DATA_UNSORTED";
    public static final String MONTH_DISPLAYED = "MONTH_DISPLAYED";
    public static final int COMPRESSION_QUALITY = 100;
    public static final int DEFAULT_BITMAP_WIDTH = 640;
    public static final int DEFAULT_BITMAP_HEIGHT = 480;
    private static final String LOG_TAG = WANMonthlyTrafficActivity.class.getSimpleName();
    private static int MB = 1024 * 1024;
    private final String[] breakdownLines = new String[31];
    private Toolbar mToolbar;
    private String mRouter;
    private String mMonthDisplayed;
    private ShareActionProvider mShareActionProvider;
    private Menu optionsMenu;
    private File mFileToShare;
    private SortedMap<Integer, ArrayList<Double>> mTrafficDataForMonth;
    private Exception mException;
    private boolean themeLight;
    private double totalIn;
    private double totalOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        themeLight = ColorUtils.isThemeLight(this);
        if (themeLight) {
            //Light
            setTheme(R.style.AppThemeLight);
        } else {
            //Default is Dark
            setTheme(R.style.AppThemeDark);
        }

        setContentView(R.layout.tile_status_wan_monthly_traffic_chart);

        if (themeLight) {
            final Resources resources = getResources();
            getWindow().getDecorView()
                    .setBackgroundColor(resources.getColor(android.R.color.darker_gray));

        }

        final Intent intent = getIntent();
        mRouter = intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);

        mToolbar = (Toolbar) findViewById(R.id.tile_status_wan_monthly_traffic_chart_view_toolbar);
        if (mToolbar != null) {
            mToolbar.setTitle(WAN_MONTHLY_TRAFFIC + " on '" + mRouter + "'");
            setSupportActionBar(mToolbar);
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        @SuppressWarnings("unchecked")
        final HashMap<Integer, ArrayList<Double>> traffDataRaw = (HashMap<Integer, ArrayList<Double>>)
                intent.getSerializableExtra(MONTHLY_TRAFFIC_DATA_UNSORTED);
        if (traffDataRaw == null || traffDataRaw.isEmpty()) {
            Toast.makeText(this, "Internal Error - No Traffic Data available!", Toast.LENGTH_SHORT).show();
            Utils.reportException(new IllegalStateException("traffDataRaw NULL or empty"));
            finish();
            return;
        }
        mMonthDisplayed = intent.getStringExtra(MONTH_DISPLAYED);

        if (mToolbar != null && !isNullOrEmpty(mMonthDisplayed)) {
            mToolbar.setTitle(WAN_MONTHLY_TRAFFIC + " on '" + mRouter + "': " + mMonthDisplayed);
        }

        mTrafficDataForMonth = new TreeMap<>(traffDataRaw);

        doPaintBarChart();
    }

    private void doPaintBarChart() {
        final View loadingView = findViewById(R.id.tile_status_wan_monthly_traffic_chart_loading_view);
        loadingView.setVisibility(View.VISIBLE);

        final LinearLayout chartPlaceholderView = (LinearLayout) findViewById(R.id.tile_status_wan_monthly_traffic_chart_placeholder);

        try {
            Log.d(LOG_TAG, "renderTraffDateForMonth: " + mMonthDisplayed + " / dailyTraffMap=" + mTrafficDataForMonth);

            final int size = mTrafficDataForMonth.size();
            final int[] days = new int[size];
            final double[] inData = new double[size];
            final double[] outData = new double[size];

            double maxY = 0;
            int maxX = 0;

            // Creating an  XYSeries for Inbound
            final XYSeries inboundSeries = new XYSeries("Inbound");
            // Creating an  XYSeries for Outbound
            final XYSeries outboundSeries = new XYSeries("Outbound");

            int i = 0;
            totalIn = 0;
            totalOut = 0;
            for (final Map.Entry<Integer, ArrayList<Double>> dailyTraffMapEntry : mTrafficDataForMonth.entrySet()) {
                final ArrayList<Double> dailyTraffMapEntryValue = dailyTraffMapEntry.getValue();
                if (dailyTraffMapEntryValue == null || dailyTraffMapEntryValue.size() < 2) {
                    continue;
                }
                final Double in = dailyTraffMapEntryValue.get(0);
                final Double out = dailyTraffMapEntryValue.get(1);
                if (in == null || out == null) {
                    continue;
                }
                // Adding data to In and Out Series
                days[i] = dailyTraffMapEntry.getKey();

                inData[i] = in;
                inboundSeries.add(i, inData[i]);
                totalIn += in;

                outData[i] = out;
                outboundSeries.add(i, outData[i]);
                totalOut += out;

                maxX = Math.max(maxX, days[i]);
                maxY = Math.max(maxY, Math.max(in, out));

                breakdownLines[i] = String.format("- Day %d: Inbound=%s / Outbound=%s", i + 1,
                        byteCountToDisplaySize(in.longValue() * MB).replace("bytes", "B"),
                        byteCountToDisplaySize(out.longValue() * MB).replace("bytes", "B"));

                i++;
            }

            // Creating a dataset to hold each series
            final XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
            // Adding inbound Series to the dataset
            dataset.addSeries(inboundSeries);
            // Adding outbound Series to dataset
            dataset.addSeries(outboundSeries);

            // Creating XYSeriesRenderer to customize inboundSeries
            final XYSeriesRenderer inboundRenderer = new XYSeriesRenderer();
            inboundRenderer.setColor(ColorUtils.getColor("WAN_TRAFFIC_BAR_IN"));
            inboundRenderer.setFillPoints(true);
            inboundRenderer.setLineWidth(2);
            inboundRenderer.setDisplayChartValues(false);
            inboundRenderer.setDisplayChartValuesDistance(5); //setting chart value distance

            // Creating XYSeriesRenderer to customize outboundSeries
            final XYSeriesRenderer outboundRenderer = new XYSeriesRenderer();
            outboundRenderer.setColor(ColorUtils.getColor("WAN_TRAFFIC_BAR_OUT"));
            outboundRenderer.setFillPoints(true);
            outboundRenderer.setLineWidth(2);
            outboundRenderer.setDisplayChartValues(false);

            // Creating a XYMultipleSeriesRenderer to customize the whole chart
            final XYMultipleSeriesRenderer multiRenderer = new XYMultipleSeriesRenderer();
            multiRenderer.setOrientation(XYMultipleSeriesRenderer.Orientation.HORIZONTAL);
            multiRenderer.setChartTitle(String.format("Month: %s / Total IN: %s / Total OUT: %s",
                    mMonthDisplayed,
                    byteCountToDisplaySize(Double.valueOf(totalIn * MB).longValue())
                            .replace("bytes", "B"),
                    byteCountToDisplaySize(Double.valueOf(totalOut * MB).longValue())
                            .replace("bytes", "B")));
            multiRenderer.setXTitle("Days");
            multiRenderer.setYTitle("Traffic");
            multiRenderer.setZoomButtonsVisible(false);

            //Add custom labels for the values we have here
            //setting no of values to display in y axis
            multiRenderer.setYLabels(0);
            if (maxY != 0) {
                multiRenderer.addYTextLabel(maxY, byteCountToDisplaySize(Double.valueOf(maxY * MB).longValue())
                        .replace("bytes", "B"));
                multiRenderer.addYTextLabel(3 * maxY / 4, byteCountToDisplaySize(Double.valueOf(3 * maxY * MB / 4).longValue())
                        .replace("bytes", "B"));
                multiRenderer.addYTextLabel(maxY / 2, byteCountToDisplaySize(Double.valueOf(maxY * MB / 2).longValue())
                        .replace("bytes", "B"));
                multiRenderer.addYTextLabel(maxY / 4, byteCountToDisplaySize(Double.valueOf(maxY * MB / 4).longValue())
                        .replace("bytes", "B"));
            }

            multiRenderer.setXLabels(0);
            for (int d = 0; d < maxX; d++) {
                multiRenderer.addXTextLabel(d, String.valueOf(d + 1));
            }

            //setting text size of the title
            multiRenderer.setChartTitleTextSize(24);
            //setting text size of the axis title
            multiRenderer.setAxisTitleTextSize(20);
            //setting text size of the graph label
            multiRenderer.setLabelsTextSize(20);
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
            multiRenderer.setMarginsColor(getResources().getColor(R.color.transparent_background));
            multiRenderer.setApplyBackgroundColor(true);

            //setting the margin size for the graph in the order top, left, bottom, right
            multiRenderer.setMargins(new int[]{30, 30, 30, 30});


            // Adding inboundRenderer and outboundRenderer to multipleRenderer
            // Note: The order of adding dataseries to dataset and renderers to multipleRenderer
            // should be same
            multiRenderer.addSeriesRenderer(inboundRenderer);
            multiRenderer.addSeriesRenderer(outboundRenderer);

            final GraphicalView chartView = ChartFactory
                    .getBarChartView(this, dataset, multiRenderer, BarChart.Type.DEFAULT);
//                            chartView.repaint();
            chartPlaceholderView.removeAllViews();
            chartPlaceholderView.addView(chartView);

            chartPlaceholderView.setVisibility(View.VISIBLE);
            loadingView.setVisibility(View.GONE);
            findViewById(R.id.tile_status_wan_monthly_traffic_chart_error)
                    .setVisibility(View.GONE);

        } catch (final Exception e) {
            mException = e;
            e.printStackTrace();
            Utils.reportException(e);
            findViewById(R.id.tile_status_wan_monthly_traffic_chart_error)
                    .setVisibility(View.VISIBLE);
            loadingView.setVisibility(View.GONE);
            chartPlaceholderView.setVisibility(View.GONE);
            if (optionsMenu != null) {
                optionsMenu.findItem(R.id.tile_status_wan_monthly_traffic_share)
                        .setEnabled(false);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tile_status_wan_monthly_traffic_chart_options, menu);

        this.optionsMenu = menu;

        /* Getting the actionprovider associated with the menu item whose id is share */
        final MenuItem shareMenuItem = menu.findItem(R.id.tile_status_wan_monthly_traffic_share);
        shareMenuItem.setEnabled(mException == null);

        mShareActionProvider = (ShareActionProvider)
                MenuItemCompat.getActionProvider(shareMenuItem);
        if (mShareActionProvider == null) {
            mShareActionProvider = new ShareActionProvider(this);
            MenuItemCompat.setActionProvider(shareMenuItem, mShareActionProvider);
        }

        final View viewToShare = findViewById(R.id.tile_status_wan_monthly_traffic_chart_placeholder);
        //Construct Bitmap and share it
        final int width = viewToShare.getWidth();
        final int height = viewToShare.getHeight();
        final Bitmap bitmapToExport = Bitmap
                .createBitmap(width > 0 ? width : DEFAULT_BITMAP_WIDTH,
                        height > 0 ? height : DEFAULT_BITMAP_HEIGHT,
                        Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmapToExport);
        viewToShare.draw(canvas);

        mFileToShare = new File(getCacheDir(),
                String.format("WAN Monthly Traffic for '%s' on Router '%s'.png",
                        nullToEmpty(mMonthDisplayed), nullToEmpty(mRouter)));
        OutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(mFileToShare, false));
            bitmapToExport.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY, outputStream);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            Crouton.makeText(this, getString(R.string.internal_error_please_try_again), Style.ALERT)
                    .show();
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

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
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

        final Uri uriForFile = FileProvider
                .getUriForFile(this, "org.rm3l.fileprovider", file);

        mShareActionProvider.setOnShareTargetSelectedListener(new ShareActionProvider.OnShareTargetSelectedListener() {
            @Override
            public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
                grantUriPermission(intent.getComponent().getPackageName(),
                        uriForFile, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                return true;
            }
        });

        final Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT,
                String.format("WAN Monthly Traffic on '%s': %s", mRouter, mMonthDisplayed));
        sendIntent.putExtra(Intent.EXTRA_TEXT,
                String.format("Traffic Breakdown\n\n>>> Total Inbound: %s / Total Outbound: %s <<<\n\n%s",
                        byteCountToDisplaySize(Double.valueOf(totalIn * MB).longValue())
                                .replace("bytes", "B"),
                        byteCountToDisplaySize(Double.valueOf(totalOut * MB).longValue())
                                .replace("bytes", "B"),
                        Joiner.on("\n").skipNulls().join(breakdownLines)));

        sendIntent.setData(uriForFile);
        sendIntent.setType("image/png");
        sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        setShareIntent(sendIntent);
    }

    @Override
    protected void onDestroy() {
        if (mFileToShare != null) {
            //noinspection ResultOfMethodCallIgnored
            mFileToShare.delete();
        }
        super.onDestroy();
    }

}
