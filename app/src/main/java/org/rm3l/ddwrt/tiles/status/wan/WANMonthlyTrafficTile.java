/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Armel S.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.rm3l.ddwrt.tiles.status.wan;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

import org.achartengine.ChartFactory;
import org.achartengine.chart.BarChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.NVRAMParser;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 *
 */
public class WANMonthlyTrafficTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = WANMonthlyTrafficTile.class.getSimpleName();
    public static final Splitter MONTHLY_TRAFF_DATA_SPLITTER = Splitter.on(" ").omitEmptyStrings();
    private static final Splitter DAILY_TRAFF_DATA_SPLITTER = Splitter.on(":").omitEmptyStrings();
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("MM-yyyy");

    @NotNull
    private final Table<String, Integer, ArrayList<Double>> traffData = HashBasedTable.create(36, 31);

    public WANMonthlyTrafficTile(@NotNull SherlockFragment parentFragment, @NotNull Bundle arguments, Router router) {
        super(parentFragment, arguments, router, R.layout.tile_status_wan_monthly_traffic, R.id.tile_status_wan_monthly_traffic_togglebutton);
        final TextView monthYearTextViewToDisplay = (TextView) this.layout.findViewById(R.id.tile_status_wan_monthly_month_displayed);
        monthYearTextViewToDisplay.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                final String toDisplay = monthYearTextViewToDisplay.getText().toString();
                final boolean isCurrentMonthYear = SIMPLE_DATE_FORMAT.format(new Date()).equals(toDisplay);

                WANMonthlyTrafficTile.this.layout.findViewById(R.id.tile_status_wan_monthly_traffic_graph_placeholder_current)
                        .setEnabled(!isCurrentMonthYear);
                WANMonthlyTrafficTile.this.layout.findViewById(R.id.tile_status_wan_monthly_traffic_graph_placeholder_next)
                        .setEnabled(!isCurrentMonthYear);
                WANMonthlyTrafficTile.this.layout.findViewById(R.id.tile_status_wan_monthly_traffic_graph_placeholder_display_button)
                        .setVisibility(isNullOrEmpty(toDisplay) ? View.GONE : View.VISIBLE);

            }
        });
    }

    @Nullable
    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {

        if (nbRunsLoader <= 0 || mAutoRefreshToggle) {
            setLoadingViewVisibility(View.VISIBLE);
        }

        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {

                try {

                    Log.d(LOG_TAG, "Init background loader for " + WANMonthlyTrafficTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                        //Skip run
                        Log.d(LOG_TAG, "Skip loader run");
                        throw new DDWRTTileAutoRefreshNotAllowedException();
                    }
                    nbRunsLoader++;

                    @NotNull final NVRAMInfo nvramInfo = new NVRAMInfo();

                    NVRAMInfo nvramInfoTmp = null;
                    try {
                        //noinspection ConstantConditions
                        nvramInfoTmp = NVRAMParser.parseNVRAMOutput(
                                SSHUtils.getManualProperty(mRouter, "nvram show 2>/dev/null | grep traff[-_]"));
                    } finally {
                        if (nvramInfoTmp != null) {
                            nvramInfo.putAll(nvramInfoTmp);
                        }

                    }

                    traffData.clear();

                    if (nvramInfo.isEmpty()) {
                        throw new DDWRTNoDataException("No Data!");
                    }

                    @SuppressWarnings("ConstantConditions")
                    final Set<Map.Entry<Object, Object>> entries = nvramInfo.getData().entrySet();

                    for (final Map.Entry<Object, Object> entry : entries) {
                        final Object key;
                        final Object value;
                        if (entry == null || (key = entry.getKey()) == null || (value = entry.getValue()) == null) {
                            continue;
                        }

                        if (!StringUtils.startsWithIgnoreCase(key.toString(), "traff-")) {
                            continue;
                        }

                        final String month = key.toString().replace("traff-", DDWRTCompanionConstants.EMPTY_STRING);

                        final String yearlyTraffData = value.toString();
                        final List<String> yearlyTraffDataList = MONTHLY_TRAFF_DATA_SPLITTER.splitToList(yearlyTraffData);
                        if (yearlyTraffDataList == null || yearlyTraffDataList.isEmpty()) {
                            continue;
                        }

                        int dayNum = 0;
                        for (final String dailyInOutTraffData : yearlyTraffDataList) {
                            if (StringUtils.contains(dailyInOutTraffData, "[")) {
                                continue;
                            }
                            dayNum++;
                            final List<String> dailyInOutTraffDataList = DAILY_TRAFF_DATA_SPLITTER.splitToList(dailyInOutTraffData);
                            if (dailyInOutTraffDataList == null || dailyInOutTraffDataList.size() < 2) {
                                continue;
                            }
                            final String inTraff = dailyInOutTraffDataList.get(0);
                            final String outTraff = dailyInOutTraffDataList.get(1);

                            traffData.put(month, dayNum, Lists.newArrayList(
                                    Double.parseDouble(inTraff), Double.parseDouble(outTraff)
                            ));

                        }
                    }

                    return nvramInfo;

                } catch (@NotNull final Exception e) {
                    e.printStackTrace();
                    return new NVRAMInfo().setException(e);
                }

            }
        };
    }

    @Nullable
    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Nullable
    @Override
    protected Intent getOnclickIntent() {
        //TODO
        return null;
    }

    private void setLoadingViewVisibility(final int visibility) {
        this.layout.findViewById(R.id.tile_status_wan_monthly_month_loading).setVisibility(visibility);
    }

    @Override
    public void onLoadFinished(Loader<NVRAMInfo> loader, NVRAMInfo data) {
        Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data + " / traffData=" + traffData);

        setLoadingViewVisibility(View.GONE);

        Exception preliminaryCheckException = null;
        if (data == null) {
            preliminaryCheckException = new DDWRTNoDataException("No Data!");
        } else //noinspection ThrowableResultOfMethodCallIgnored
            if (data.getException() == null) {
                if (!"1".equals(data.getProperty("ttraff_enable"))) {
                    preliminaryCheckException = new IllegalStateException("Traffic monitoring disabled!");
                } else if (traffData.isEmpty()) {
                    preliminaryCheckException = new DDWRTNoDataException("No Traffic Data!");
            }
        }

        if (preliminaryCheckException != null) {
            data = new NVRAMInfo().setException(preliminaryCheckException);
        }

        @NotNull final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_wan_monthly_traffic_error);

        @Nullable final Exception exception = data.getException();

        final View displayButton = this.layout.findViewById(R.id.tile_status_wan_monthly_traffic_graph_placeholder_display_button);
        final View currentButton = this.layout.findViewById(R.id.tile_status_wan_monthly_traffic_graph_placeholder_current);
        final View previousButton = this.layout.findViewById(R.id.tile_status_wan_monthly_traffic_graph_placeholder_previous);
        final View nextButton = this.layout.findViewById(R.id.tile_status_wan_monthly_traffic_graph_placeholder_next);
        final TextView monthYearDisplayed = (TextView) this.layout.findViewById(R.id.tile_status_wan_monthly_month_displayed);

        final View[] ctrlViews = new View[] {monthYearDisplayed, displayButton, currentButton, previousButton, nextButton};

        if (exception == null) {
            errorPlaceHolderView.setVisibility(View.GONE);

            final String currentMonthYearAlreadyDisplayed = monthYearDisplayed.getText().toString();

            final Date currentDate = new Date();
            final String currentMonthYear = (isNullOrEmpty(currentMonthYearAlreadyDisplayed) ?
                    SIMPLE_DATE_FORMAT.format(currentDate) : currentMonthYearAlreadyDisplayed);

            //TODO Load last value from preferences
            monthYearDisplayed.setText(currentMonthYear);

            displayButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Intent intent = WANMonthlyTrafficTile.this.renderTraffDateForMonth(monthYearDisplayed.getText().toString());
                    if (intent == null) {
                        Toast.makeText(WANMonthlyTrafficTile.this.mParentFragmentActivity,
                                String.format("No traffic data for '%s'", monthYearDisplayed.getText()), Toast.LENGTH_SHORT).show();
                    } else {
                        final AlertDialog alertDialog = Utils.buildAlertDialog(mParentFragmentActivity, null,
                                String.format("Loading traffic data for '%s'", monthYearDisplayed.getText()), false, false);
                        alertDialog.show();
                        ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                WANMonthlyTrafficTile.this.mParentFragmentActivity.startActivity(intent);
                                alertDialog.cancel();
                            }
                        }, 2500);
                    }
                }
            });

            currentButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    monthYearDisplayed.setText(SIMPLE_DATE_FORMAT.format(currentDate));
                }
            });

            previousButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final int[] currentYearMonth = getCurrentYearAndMonth(currentDate, monthYearDisplayed.getText().toString());
                    if (currentYearMonth.length < 2) {
                        return;
                    }

                    final int currentMonth = currentYearMonth[1];
                    final int currentYear = currentYearMonth[0];

                    final int previousMonth = currentMonth - 1;
                    final String previousMonthYear = ((previousMonth <= 0) ? ("12-" + (currentYear - 1)) :
                            (((previousMonth <= 9) ? ("0" + previousMonth) : previousMonth) + "-" + currentYear));

                    monthYearDisplayed.setText(previousMonthYear);
                }
            });

            nextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    final int[] currentYearMonth = getCurrentYearAndMonth(currentDate, monthYearDisplayed.getText().toString());
                    if (currentYearMonth.length < 2) {
                        return;
                    }

                    final int currentMonth = currentYearMonth[1];
                    final int currentYear = currentYearMonth[0];
                    final int nextMonth = currentMonth + 1;
                    final String nextMonthYear = ((nextMonth >= 13) ? ("01-" + (currentYear + 1)) :
                            (((nextMonth <= 9) ? ("0" + nextMonth) : nextMonth) + "-" + currentYear));

                    monthYearDisplayed.setText(nextMonthYear);
                }
            });

            setVisibility(ctrlViews, View.VISIBLE);
        }

        if (exception != null && !(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {
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
            setVisibility(ctrlViews, View.GONE);
        } else {
            if (traffData.isEmpty()) {
                errorPlaceHolderView.setText("Error: No Data!");
                errorPlaceHolderView.setVisibility(View.VISIBLE);
                setVisibility(ctrlViews, View.GONE);
            }
        }

        doneWithLoaderInstance(this, loader,
                R.id.tile_status_wan_monthly_traffic_togglebutton_title, R.id.tile_status_wan_monthly_traffic_togglebutton_separator);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");

    }

    private static void setVisibility(@NotNull final View[] views, final int visibility) {
        for (final View view : views) {
            view.setVisibility(visibility);
        }
    }

    @NotNull
    private int[] getCurrentYearAndMonth(final Date currentDate, final String monthYearDisplayed) {
        final int[] currentYearAndMonth = new int[2];

        String monthDisplayed = null;
        String yearDisplayed = null;
        final List<String> monthYearTextViewSplit = Splitter.on("-").omitEmptyStrings().splitToList(monthYearDisplayed);
        if (monthYearTextViewSplit != null && monthYearTextViewSplit.size() >= 2) {
            monthDisplayed = monthYearTextViewSplit.get(0);
            yearDisplayed = monthYearTextViewSplit.get(1);
        }

        currentYearAndMonth[0] = Integer.parseInt(isNullOrEmpty(yearDisplayed) ? new SimpleDateFormat("yyyy").format(currentDate) : yearDisplayed);
        currentYearAndMonth[1] = Integer.parseInt(isNullOrEmpty(monthDisplayed) ? new SimpleDateFormat("MM").format(currentDate) : monthDisplayed);

        return currentYearAndMonth;
    }

    private Intent renderTraffDateForMonth(@NotNull final String monthFormatted) {

        Log.d(LOG_TAG, "renderTraffDateForMonth: " + monthFormatted);

        final Map<Integer, ArrayList<Double>> dailyTraffMap = traffData.row(monthFormatted);

        if (dailyTraffMap == null || dailyTraffMap.isEmpty()) {
            return null;
        }

        Log.d(LOG_TAG, "renderTraffDateForMonth: " + monthFormatted + " / dailyTraffMap=" + dailyTraffMap);

        final int size = dailyTraffMap.size();
        final int[] days = new int[size];
        final double[] inData = new double[size];
        final double[] outData = new double[size];

        int i = 0;
        for (final Map.Entry<Integer, ArrayList<Double>> dailyTraffMapEntry : dailyTraffMap.entrySet()) {
            final ArrayList<Double> dailyTraffMapEntryValue = dailyTraffMapEntry.getValue();
            if (dailyTraffMapEntryValue == null || dailyTraffMapEntryValue.size() < 2) {
                continue;
            }
            final Double in = dailyTraffMapEntryValue.get(0);
            final Double out = dailyTraffMapEntryValue.get(1);
            if (in == null || in <= 0. || out == null || out <= 0.) {
                continue;
            }
            days[i] = dailyTraffMapEntry.getKey();
            inData[i] = in;
            outData[i] = out;
            i++;
        }

        // Creating an  XYSeries for Inbound
        final XYSeries inboundSeries = new XYSeries("Inbound");
        // Creating an  XYSeries for Outbound
        final XYSeries outboundSeries = new XYSeries("Outbound");
        // Adding data to In and Out Series
        for(int j = 0; j < i; j++){
            inboundSeries.add(j, inData[j]);
            outboundSeries.add(j, outData[j]);
        }

        // Creating a dataset to hold each series
        final XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        // Adding inbound Series to the dataset
        dataset.addSeries(inboundSeries);
        // Adding outbound Series to dataset
        dataset.addSeries(outboundSeries);

        // Creating XYSeriesRenderer to customize inboundSeries
        final XYSeriesRenderer inboundRenderer = new XYSeriesRenderer();
        inboundRenderer.setColor(Color.rgb(130, 130, 230));
        inboundRenderer.setFillPoints(true);
        inboundRenderer.setLineWidth(2);
        inboundRenderer.setDisplayChartValues(true);

        // Creating XYSeriesRenderer to customize outboundSeries
        final XYSeriesRenderer outboundRenderer = new XYSeriesRenderer();
        outboundRenderer.setColor(Color.rgb(220, 80, 80));
        outboundRenderer.setFillPoints(true);
        outboundRenderer.setLineWidth(2);
        outboundRenderer.setDisplayChartValues(true);

        // Creating a XYMultipleSeriesRenderer to customize the whole chart
        final XYMultipleSeriesRenderer multiRenderer = new XYMultipleSeriesRenderer();
        multiRenderer.setXLabels(i);
        multiRenderer.setXAxisMin(1);
        multiRenderer.setChartTitle("Traffic Data for '" + monthFormatted + "'");
        multiRenderer.setXTitle("Days");
        multiRenderer.setYTitle("Traffic (MB)");
        multiRenderer.setZoomButtonsVisible(true);
        for (int k = 0; k < i; k++) {
            multiRenderer.addXTextLabel(k, String.valueOf(days[k]));
        }
//        for(int k = 0; k < days.length; k++) {
//            multiRenderer.addXTextLabel(k, String.valueOf(k));
//        }

        // Adding inboundRenderer and outboundRenderer to multipleRenderer
        // Note: The order of adding dataseries to dataset and renderers to multipleRenderer
        // should be same
        multiRenderer.addSeriesRenderer(inboundRenderer);
        multiRenderer.addSeriesRenderer(outboundRenderer);

        return ChartFactory.getBarChartIntent(this.mParentFragmentActivity, dataset,
                multiRenderer, BarChart.Type.DEFAULT);

    }
}
