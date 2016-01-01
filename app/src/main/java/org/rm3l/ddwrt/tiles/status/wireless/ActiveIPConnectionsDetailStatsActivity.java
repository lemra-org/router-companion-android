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
package org.rm3l.ddwrt.tiles.status.wireless;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdView;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.CategorySeries;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.utils.AdUtils;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.Utils;
import org.rm3l.ddwrt.utils.snackbar.SnackbarCallback;
import org.rm3l.ddwrt.utils.snackbar.SnackbarUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

public class ActiveIPConnectionsDetailStatsActivity extends AppCompatActivity {

    private static final String LOG_TAG = ActiveIPConnectionsDetailStatsActivity.class.getSimpleName();

    public static final String BY = "BY";

    public static final String CONNECTIONS_COUNT_MAP = "CONNECTIONS_COUNT_MAP";

    public static final int COMPRESSION_QUALITY = 100;
    public static final int DEFAULT_BITMAP_WIDTH = 640;
    public static final int DEFAULT_BITMAP_HEIGHT = 480;
    private static final int MAX_ITEMS_IN_PIE_CHART = 10;
    private boolean themeLight;
    private String mRouter;
    private Toolbar mToolbar;
    private ByFilter mByFilter;
    private Map<String, Integer> mConnectionsCountMap;
    private Exception mException;
    private Menu optionsMenu;
    private File mFileToShare;
    private ShareActionProvider mShareActionProvider;
    private String mObservationDate;
    private HashMap<String, String> mLocalIpToHostname;
    private ProgressBar mLoadingView;

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

        setContentView(R.layout.active_ip_connections_detail_pie_chart);

        if (themeLight) {
            getWindow().getDecorView()
                    .setBackgroundColor(
                            ContextCompat.getColor(this, android.R.color.white));

        }

        AdUtils.buildAndDisplayAdViewIfNeeded(this, (AdView) findViewById(R.id.active_ip_connections_detail_pie_chart_view_adView));

        final Intent intent = getIntent();
        mRouter = intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);
        mObservationDate = intent.getStringExtra(ActiveIPConnectionsDetailActivity.OBSERVATION_DATE);

        //noinspection unchecked
        final HashMap<String, Integer> connectionsCountMap = (HashMap<String, Integer>) intent.getSerializableExtra(CONNECTIONS_COUNT_MAP);

        if (connectionsCountMap == null || connectionsCountMap.isEmpty()) {
            Toast.makeText(this, "Internal Error - No Data available!", Toast.LENGTH_SHORT).show();
            Utils.reportException(null, new IllegalStateException("connectionsCountMap NULL or empty"));
            finish();
            return;
        }

        final Ordering<String> reverseValuesAndNaturalKeysOrdering =
                Ordering.natural().reverse().nullsLast().onResultOf(Functions.forMap(connectionsCountMap, null)) // natural for values
                        .compound(Ordering.natural()); // secondary - natural ordering of keys

        this.mConnectionsCountMap = ImmutableSortedMap.copyOf(connectionsCountMap, reverseValuesAndNaturalKeysOrdering);

        //noinspection unchecked
        mLocalIpToHostname = (HashMap<String, String>) intent
                .getSerializableExtra(ActiveIPConnectionsDetailActivity.IP_TO_HOSTNAME_RESOLVER);
        if (mLocalIpToHostname == null) {
            mLocalIpToHostname = new HashMap<>();
        }

        mByFilter = (ByFilter) intent.getSerializableExtra(BY);

        mToolbar = (Toolbar) findViewById(R.id.active_ip_connections_detail_pie_chart_view_toolbar);
        if (mToolbar != null) {
            mToolbar.setTitle("IP Connections Pie Chart");
            setSupportActionBar(mToolbar);
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        mLoadingView = (ProgressBar) findViewById(R.id.active_ip_connections_detail_pie_chart_loading_view);

        doPaintPieChart();

    }

    private void doPaintPieChart() {
        mLoadingView.setVisibility(View.VISIBLE);

        final LinearLayout chartPlaceholderView = (LinearLayout)
                findViewById(R.id.active_ip_connections_detail_pie_chart_placeholder);

        try {
            final int size = mConnectionsCountMap.size();

            final int limit = Math.min(size, MAX_ITEMS_IN_PIE_CHART);

            final CategorySeries distributionSeries =
                    new CategorySeries("Connections Count (by " + mByFilter.getDisplayName() + ")");

            int j = 1;
            final List<Integer> colorsList = new ArrayList<>();
            for (final Map.Entry<String, Integer> entry : mConnectionsCountMap.entrySet()) {
                if (j > limit) {
                    break;
                }

                final String ip = entry.getKey();
                final String name = mLocalIpToHostname.get(ip);
                colorsList.add(ColorUtils.getColor(ip));
                distributionSeries.add(
                        isNullOrEmpty(name) ? ip : (name + " (" + ip + ")"),
                        entry.getValue());

                j++;
            }

            // Instantiating a renderer for the Pie Chart
            final DefaultRenderer defaultRenderer = new DefaultRenderer();
            for (int i = 0; i < limit; i++) {
                final SimpleSeriesRenderer seriesRenderer = new SimpleSeriesRenderer();
                if (i < colorsList.size()) {
                    seriesRenderer.setColor(colorsList.get(i));
                }
//                seriesRenderer.setDisplayChartValues(true);
                defaultRenderer.addSeriesRenderer(seriesRenderer);
            }

            defaultRenderer.setDisplayValues(true);

            defaultRenderer.setChartTitle("Connections count (by " +
                    mByFilter.getDisplayName() + ") \n" +
                    "on " + mObservationDate);
            defaultRenderer.setZoomButtonsVisible(false);

            //setting text size of the title
            defaultRenderer.setChartTitleTextSize(26);
            //setting text size of the graph label
            defaultRenderer.setLabelsTextSize(24);
            //setting pan ability which uses graph to move on both axis
            defaultRenderer.setPanEnabled(false);
            //setting click false on graph
            defaultRenderer.setClickEnabled(false);
            //setting lines to display on y axis
            defaultRenderer.setShowGridY(false);
            //setting lines to display on x axis
            defaultRenderer.setShowGridX(false);
            //setting legend to fit the screen size
            defaultRenderer.setFitLegend(true);
            //setting displaying line on grid
            defaultRenderer.setShowGrid(false);
            //setting zoom
            defaultRenderer.setZoomEnabled(false);
            //setting external zoom functions to false
//            defaultRenderer.setZoomRate(1.1f);
            defaultRenderer.setExternalZoomEnabled(false);
            //setting displaying lines on graph to be formatted(like using graphics)
            defaultRenderer.setAntialiasing(true);
            //setting to in scroll to false
            defaultRenderer.setInScroll(false);
            //setting text style
            defaultRenderer.setTextTypeface("sans_serif", Typeface.NORMAL);

            defaultRenderer.setLabelsColor(
                    ContextCompat.getColor(this,
                            themeLight ? R.color.black : R.color.white));

            //Setting background color of the graph to transparent
            defaultRenderer.setBackgroundColor(Color.TRANSPARENT);
            defaultRenderer.setApplyBackgroundColor(true);

            //setting the margin size for the graph in the order top, left, bottom, right
            defaultRenderer.setMargins(new int[]{30, 30, 30, 30});

            final GraphicalView chartView = ChartFactory
                    .getPieChartView(this, distributionSeries, defaultRenderer);
//                            chartView.repaint();
            chartPlaceholderView.removeAllViews();
            chartPlaceholderView.addView(chartView);

            chartPlaceholderView.setVisibility(View.VISIBLE);
            mLoadingView.setVisibility(View.GONE);
            findViewById(R.id.active_ip_connections_detail_pie_chart_error).setVisibility(View.GONE);

        } catch (final Exception e) {
            mException = e;
            e.printStackTrace();
            Utils.reportException(null, e);
            findViewById(R.id.active_ip_connections_detail_pie_chart_error)
                    .setVisibility(View.VISIBLE);
            mLoadingView.setVisibility(View.GONE);
            chartPlaceholderView.setVisibility(View.GONE);
            if (optionsMenu != null) {
                optionsMenu.findItem(R.id.active_ip_connections_pie_chart_share)
                        .setEnabled(false);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.active_ip_connections_pie_chart_options, menu);

        this.optionsMenu = menu;

        //Permission requests
        final int rwExternalStoragePermissionCheck = ContextCompat
                .checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (rwExternalStoragePermissionCheck != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat
                    .shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                SnackbarUtils.buildSnackbar(this, findViewById(android.R.id.content),
                        "Storage access is required to share stats about active IP connections.",
                        "OK",
                        Snackbar.LENGTH_LONG,
                        new SnackbarCallback() {
                            @Override
                            public void onShowEvent(@Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventSwipe(int event, @Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventActionClick(int event, @Nullable Bundle bundle) throws Exception {
                                //Request permission
                                ActivityCompat.requestPermissions(ActiveIPConnectionsDetailStatsActivity.this,
                                        new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        DDWRTCompanionConstants.Permissions.STORAGE);
                            }

                            @Override
                            public void onDismissEventTimeout(int event, @Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventManual(int event, @Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventConsecutive(int event, @Nullable Bundle bundle) throws Exception {

                            }
                        },
                        null,
                        true);
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        DDWRTCompanionConstants.Permissions.STORAGE);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        /* Getting the actionprovider associated with the menu item whose id is share */
        final MenuItem shareMenuItem = menu.findItem(R.id.active_ip_connections_pie_chart_share);
        shareMenuItem.setEnabled(mException == null);

        mShareActionProvider = (ShareActionProvider)
                MenuItemCompat.getActionProvider(shareMenuItem);
        if (mShareActionProvider == null) {
            mShareActionProvider = new ShareActionProvider(this);
            MenuItemCompat.setActionProvider(shareMenuItem, mShareActionProvider);
        }

        final View viewToShare = findViewById(R.id.active_ip_connections_detail_pie_chart_placeholder);
        //Construct Bitmap and share it
        final int width = viewToShare.getWidth();
        final int height = viewToShare.getHeight();
        final Bitmap bitmapToExport = Bitmap
                .createBitmap(width > 0 ? width : DEFAULT_BITMAP_WIDTH,
                        height > 0 ? height : DEFAULT_BITMAP_HEIGHT,
                        Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmapToExport);
        viewToShare.draw(canvas);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {

            mFileToShare = new File(getCacheDir(),
                    Utils.getEscapedFileName(String.format("Active IP Connections Chart By %s on Router '%s' (on %s)",
                            mByFilter, nullToEmpty(mRouter), mObservationDate)) + ".png");
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
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        switch (requestCode) {
            case DDWRTCompanionConstants.Permissions.STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Yay! Permission granted for #" + requestCode);

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Crashlytics.log(Log.WARN, LOG_TAG, "Boo! Permission denied for #" + requestCode);
                    Utils.displayMessage(this,
                            "Sharing of IP Connections Stats will be unavailable",
                            Style.INFO);
                    if (optionsMenu != null) {
                        final MenuItem menuItem = optionsMenu
                                .findItem(R.id.active_ip_connections_pie_chart_share);
                        menuItem.setEnabled(false);
                        menuItem.setVisible(false);
                    }
                }
                return;
            }
            default:
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
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
                .getUriForFile(this, DDWRTCompanionConstants.FILEPROVIDER_AUTHORITY, file);

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
        sendIntent.setType("text/html");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT,
                String.format("Active IP Connections Chart By %s on Router '%s' (on %s)",
                        mByFilter.getDisplayName(), nullToEmpty(mRouter), mObservationDate));

        final String fullConnectionCountMap = Joiner.on("\n").withKeyValueSeparator(": ").useForNull("???").join(mConnectionsCountMap);

        sendIntent.putExtra(Intent.EXTRA_TEXT,
                Html.fromHtml(String.format("Connections Count Breakdown (by %s) on %s\n\n%s" +
                                "%s",
                        mByFilter.getDisplayName(), mObservationDate,
                        fullConnectionCountMap, Utils.getShareIntentFooter())
                        .replaceAll("\n", "<br/>")));

        sendIntent.setData(uriForFile);
//        sendIntent.setType("image/png");
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

    public static final int ByFilter_SOURCE = 1;
    public static final int ByFilter_DESTINATION_IP = 2;
    public static final int ByFilter_DESTINATION_COUNTRY = 3;

    public enum ByFilter {
        SOURCE("source"),
        DESTINATION_IP("destination ip"),
        DESTINATION_COUNTRY("destination country");

        private final String displayName;

        ByFilter(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
