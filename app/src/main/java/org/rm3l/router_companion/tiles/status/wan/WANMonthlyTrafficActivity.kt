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
package org.rm3l.router_companion.tiles.status.wan

import org.rm3l.router_companion.RouterCompanionAppConstants.EMPTY_STRING
import org.rm3l.router_companion.RouterCompanionAppConstants.MB
import org.rm3l.router_companion.resources.WANTrafficData.Companion.INBOUND
import org.rm3l.router_companion.resources.WANTrafficData.Companion.OUTBOUND
import org.rm3l.router_companion.utils.Utils.fromHtml

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v4.content.PermissionChecker
import android.support.v4.view.MenuItemCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.ShareActionProvider
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.crashlytics.android.Crashlytics
import com.github.florent37.viewtooltip.ViewTooltip
import com.github.florent37.viewtooltip.ViewTooltip.ALIGN
import com.github.florent37.viewtooltip.ViewTooltip.Position
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.InterstitialAd
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.ArrayList
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import org.achartengine.ChartFactory
import org.achartengine.GraphicalView
import org.achartengine.chart.BarChart
import org.achartengine.model.XYMultipleSeriesDataset
import org.achartengine.model.XYSeries
import org.achartengine.renderer.XYMultipleSeriesRenderer
import org.achartengine.renderer.XYSeriesRenderer
import org.achartengine.tools.ZoomEvent
import org.achartengine.tools.ZoomListener
import org.achartengine.util.MathHelper
import org.rm3l.ddwrt.BuildConfig
import org.rm3l.ddwrt.R
import org.rm3l.router_companion.RouterCompanionAppConstants
import org.rm3l.router_companion.mgmt.RouterManagementActivity
import org.rm3l.router_companion.mgmt.dao.DDWRTCompanionDAO
import org.rm3l.router_companion.resources.MonthlyCycleItem
import org.rm3l.router_companion.resources.WANTrafficData
import org.rm3l.router_companion.resources.conn.Router
import org.rm3l.router_companion.utils.AdUtils
import org.rm3l.router_companion.utils.ColorUtils
import org.rm3l.router_companion.utils.FileUtils.*
import org.rm3l.router_companion.utils.Utils
import org.rm3l.router_companion.utils.ViewGroupUtils
import org.rm3l.router_companion.utils.WANTrafficUtils
import org.rm3l.router_companion.utils.snackbar.SnackbarCallback
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style
import java.lang.Double

class WANMonthlyTrafficActivity : AppCompatActivity() {

    private val breakdownLines = ArrayList<String>()

    private var mChartView: GraphicalView? = null

    private var dao: DDWRTCompanionDAO? = null

    private var mCycleItem: MonthlyCycleItem? = null

    private var mException: Exception? = null

    private var mFileToShare: File? = null
    private var mCsvFileToShare: File? = null

    private var mInterstitialAd: InterstitialAd? = null

    private var mRouter: Router? = null

    private var mRouterDisplay: String? = null

    private var mShareActionProvider: ShareActionProvider? = null

    private var mToolbar: Toolbar? = null

    private var optionsMenu: Menu? = null

    private var themeLight: Boolean = false

    private var totalIn: Long = 0

    private var totalOut: Long = 0

    private var wanTrafficDataBreakdown: List<WANTrafficData?>? = null

    private var mTooltipPlaceholderView: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dao = RouterManagementActivity.getDao(this)
        if (dao == null) {
            Utils.reportException(this,
                    IllegalStateException("dao == null"))
            Toast.makeText(this, "Internal Error - please try again later.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val intent = intent

        val routerUuid = intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED)
        val wanCycleStr = intent.getStringExtra(WAN_CYCLE)
        if (routerUuid.isNullOrBlank() || wanCycleStr.isNullOrBlank()) {
            Utils.reportException(this,
                    IllegalStateException("isNullOrEmpty(routerUuid) || mCycleItem == null"))
            Toast.makeText(this, "Internal Error - please try again later.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        mRouter = dao!!.getRouter(routerUuid)

        if (mRouter == null) {
            Utils.reportException(this,
                    IllegalStateException("isNullOrEmpty(routerUuid) || mCycleItem == null"))
            Toast.makeText(this, "Internal Error - please try again later.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        themeLight = ColorUtils.isThemeLight(this)
        ColorUtils.setAppTheme(this, mRouter!!.routerFirmware, false)

        setContentView(R.layout.tile_status_wan_monthly_traffic_chart)

        val mRouterName = mRouter!!.name
        val mRouterNameNullOrEmpty = mRouterName.isNullOrBlank()
        mRouterDisplay = ""
        if (!mRouterNameNullOrEmpty) {
            mRouterDisplay = "${mRouterName!!}("
        }
        mRouterDisplay += mRouter!!.remoteIpAddress
        if (!mRouterNameNullOrEmpty) {
            mRouterDisplay += ")"
        }

        try {
            mCycleItem = Gson().fromJson(wanCycleStr, MonthlyCycleItem::class.java)
        } catch (jse: JsonSyntaxException) {
            Crashlytics.log(Log.ERROR, LOG_TAG,
                    "JsonSyntaxException while trying to read wanCycleStr: " + wanCycleStr)
            jse.printStackTrace()
            Utils.reportException(this, jse)
            Toast.makeText(this, "Internal Error - please try again later.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        mCycleItem!!.setContext(this)

        mInterstitialAd = AdUtils.requestNewInterstitial(this,
                R.string.interstitial_ad_unit_id_transtion_to_wan_monthly_chart)

        AdUtils.buildAndDisplayAdViewIfNeeded(this,
                findViewById<View>(R.id.tile_status_wan_monthly_traffic_chart_view_adView) as AdView)

        mToolbar = findViewById<View>(R.id.tile_status_wan_monthly_traffic_chart_view_toolbar) as Toolbar
        if (mToolbar != null) {
            mToolbar!!.title = WAN_MONTHLY_TRAFFIC + ": " + mCycleItem!!.getLabelWithYears()
            mToolbar!!.subtitle = String.format("%s (%s:%d)", mRouter!!.displayName, mRouter!!.remoteIpAddress,
                    mRouter!!.remotePort)
            mToolbar!!.setTitleTextAppearance(applicationContext, R.style.ToolbarTitle)
            mToolbar!!.setSubtitleTextAppearance(applicationContext, R.style.ToolbarSubtitle)
            mToolbar!!.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
            mToolbar!!.setSubtitleTextColor(ContextCompat.getColor(this, R.color.white))
            setSupportActionBar(mToolbar)
        }

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeButtonEnabled(true)
        }

        wanTrafficDataBreakdown = WANTrafficUtils.getWANTrafficDataByRouterBetweenDates(dao!!, mRouter!!.uuid,
                mCycleItem!!.start, mCycleItem!!.end)

        mTooltipPlaceholderView = findViewById(R.id.tile_status_wan_monthly_traffic_chart_tooltip_placeholder)

        doPaintBarChart()
    }

    @SuppressLint("MissingSuperCall")
    override fun onSaveInstanceState(outState: Bundle?) {
        //No call for super(). Bug on API Level > 11.
    }

    override fun onDestroy() {
        mFileToShare?.delete()
        mCsvFileToShare?.delete()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            RouterCompanionAppConstants.Permissions.STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Yay! Permission granted for #" + requestCode)
                    if (optionsMenu != null) {
                        val menuItem = optionsMenu!!.findItem(R.id.tile_status_wan_monthly_traffic_share)
                        menuItem.isEnabled = true
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Crashlytics.log(Log.WARN, LOG_TAG, "Boo! Permission denied for #" + requestCode)
                    Utils.displayMessage(this, "Sharing of WAN Traffic Data will be unavailable", Style.INFO)
                    if (optionsMenu != null) {
                        val menuItem = optionsMenu!!.findItem(R.id.tile_status_wan_monthly_traffic_share)
                        menuItem.isEnabled = false
                    }
                }
                return
            }
            else -> {
                //Nothing to do
            }
        }
    }

    override fun finish() {
        if (BuildConfig.WITH_ADS && mInterstitialAd != null && AdUtils.canDisplayInterstialAd(this)) {

            mInterstitialAd!!.adListener = object : AdListener() {
                override fun onAdClosed() {
                    super@WANMonthlyTrafficActivity.finish()
                }

                override fun onAdOpened() {
                    //Save preference
                    getSharedPreferences(RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY,
                            Context.MODE_PRIVATE).edit()
                            .putLong(RouterCompanionAppConstants.AD_LAST_INTERSTITIAL_PREF,
                                    System.currentTimeMillis())
                            .apply()
                }
            }

            if (mInterstitialAd!!.isLoaded) {
                mInterstitialAd!!.show()
            } else {
                super@WANMonthlyTrafficActivity.finish()
            }
        } else {
            super.finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.tile_status_wan_monthly_traffic_chart_options, menu)

        this.optionsMenu = menu

        //Permission requests
        val rwExternalStoragePermissionCheck = PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (rwExternalStoragePermissionCheck != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                SnackbarUtils.buildSnackbar(this, "Storage access is required to share WAN Traffic Data.",
                        "OK", Snackbar.LENGTH_INDEFINITE, object : SnackbarCallback {
                    @Throws(Exception::class)
                    override fun onDismissEventActionClick(event: Int, bundle: Bundle?) {
                        //Request permission
                        ActivityCompat.requestPermissions(this@WANMonthlyTrafficActivity,
                                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                RouterCompanionAppConstants.Permissions.STORAGE)
                    }

                    @Throws(Exception::class)
                    override fun onDismissEventConsecutive(event: Int, bundle: Bundle?) {

                    }

                    @Throws(Exception::class)
                    override fun onDismissEventManual(event: Int, bundle: Bundle?) {

                    }

                    @Throws(Exception::class)
                    override fun onDismissEventSwipe(event: Int, bundle: Bundle?) {

                    }

                    @Throws(Exception::class)
                    override fun onDismissEventTimeout(event: Int, bundle: Bundle?) {

                    }

                    @Throws(Exception::class)
                    override fun onShowEvent(bundle: Bundle?) {

                    }
                }, null, true)
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        RouterCompanionAppConstants.Permissions.STORAGE)
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        /* Getting the actionprovider associated with the menu item whose id is share */
        val shareMenuItem = menu.findItem(R.id.tile_status_wan_monthly_traffic_share)
        shareMenuItem.isEnabled = mException == null

        mShareActionProvider = MenuItemCompat.getActionProvider(shareMenuItem) as ShareActionProvider
        if (mShareActionProvider == null) {
            mShareActionProvider = ShareActionProvider(this)
            MenuItemCompat.setActionProvider(shareMenuItem, mShareActionProvider)
            //            shareMenuItem.setIcon(R.drawable.ic_share_white_24dp);
        }

        val viewToShare = findViewById<View>(R.id.tile_status_wan_monthly_traffic_chart_placeholder)
        //Construct Bitmap and share it
        val width = viewToShare.width
        val height = viewToShare.height
        val bitmapToExport = Bitmap.createBitmap(if (width > 0) width else DEFAULT_BITMAP_WIDTH,
                if (height > 0) height else DEFAULT_BITMAP_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmapToExport)
        viewToShare.draw(canvas)

        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

            val fileName = Utils.getEscapedFileName(
                    "WAN Monthly Traffic for '${mCycleItem!!.getLabelWithYears()}' on Router '${mRouterDisplay ?: ""}'")

            mCsvFileToShare = File(cacheDir, "$fileName.csv")
            //Output the CSV file
            mCsvFileToShare!!.bufferedWriter().use { out ->
                out.write("Date,InboundBytes,InboundReadableValue,OutboundBytes,OutboundReadableValue")
                out.newLine()
                wanTrafficDataBreakdown
                        ?.filterNotNull()
                        ?.forEach { wanTrafficData ->
                            val inBytes = wanTrafficData.traffIn.toLong() * MB
                            val outBytes = wanTrafficData.traffOut.toLong() * MB
                            out.write("${wanTrafficData.date}," +
                                    "$inBytes,\"" + byteCountToDisplaySize(inBytes).replace("bytes", "B") + "\"," +
                                    "$outBytes,\"" + byteCountToDisplaySize(outBytes).replace("bytes", "B") + "\"")
                            out.newLine()
                        }
            }

            mFileToShare = File(cacheDir, "$fileName.png")
            var outputStream: OutputStream? = null
            try {
                outputStream = BufferedOutputStream(FileOutputStream(mFileToShare!!, false))
                bitmapToExport.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY, outputStream)
                outputStream.flush()
            } catch (e: IOException) {
                e.printStackTrace()
                Utils.displayMessage(this, getString(R.string.internal_error_please_try_again),
                        Style.ALERT)
            } finally {
                try {
                    if (outputStream != null) {
                        outputStream.close()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    //No Worries
                }

            }

            setShareFiles(mFileToShare!!, mCsvFileToShare!!)
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
        // Respond to the action bar's Up/Home button
            android.R.id.home -> {
                onBackPressed()
                return true
            }

            R.id.action_feedback -> {
                Utils.openFeedbackForm(this, mRouter)
                return true
            }

            R.id.tile_status_wan_monthly_traffic_zoom_in -> {
                if (this.mChartView != null) {
                    this.mChartView!!.zoomIn()
                } else {
                    Crashlytics.log(Log.WARN, LOG_TAG, "mChartView is NULL")
                    Toast.makeText(this, "Internal Error - please try again later", Toast.LENGTH_SHORT).show()
                }
                return true
            }

            R.id.tile_status_wan_monthly_traffic_zoom_out -> {
                if (this.mChartView != null) {
                    this.mChartView!!.zoomOut()
                } else {
                    Crashlytics.log(Log.WARN, LOG_TAG, "mChartView is NULL")
                    Toast.makeText(this, "Internal Error - please try again later", Toast.LENGTH_SHORT).show()
                }
                return true
            }

            R.id.tile_status_wan_monthly_traffic_zoom_reset -> {
                if (this.mChartView != null) {
                    this.mChartView!!.zoomReset()
                } else {
                    Crashlytics.log(Log.WARN, LOG_TAG, "mChartView is NULL")
                    Toast.makeText(this, "Internal Error - please try again later", Toast.LENGTH_SHORT).show()
                }
                return true
            }

            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun doPaintBarChart() {
        val loadingView = findViewById<View>(R.id.tile_status_wan_monthly_traffic_chart_loading_view)
        loadingView.visibility = View.VISIBLE

        val chartPlaceholderView = findViewById<View>(R.id.tile_status_wan_monthly_traffic_chart_placeholder) as LinearLayout

        try {

            val size = wanTrafficDataBreakdown!!.size
            if (size == 0) {
                Toast.makeText(this, "No Data or an error occurred!", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            val days = arrayOfNulls<String>(size)
            val inData = DoubleArray(size)
            val outData = DoubleArray(size)

            var maxY = 0.0
            val maxX = wanTrafficDataBreakdown!!.size

            // Creating an  XYSeries for Inbound
            val inboundSeries = XYSeries("Inbound")
            // Creating an  XYSeries for Outbound
            val outboundSeries = XYSeries("Outbound")

            totalIn = 0
            totalOut = 0

            for ((i, wanTrafficData) in wanTrafficDataBreakdown!!.withIndex()) {
                if (wanTrafficData == null) {
                    continue
                }
                val `in` = wanTrafficData.traffIn.toDouble()
                val out = wanTrafficData.traffOut.toDouble()

                days[i] = wanTrafficData.date

                maxY = Math.max(maxY, Math.max(`in`, out))

                inData[i] = `in`
                inboundSeries.add(i.toDouble(), inData[i])

                outData[i] = out
                outboundSeries.add(i.toDouble(), outData[i])

                totalIn += `in`.toLong()
                totalOut += out.toLong()

                val inBytes = `in`.toLong() * MB
                val outBytes = out.toLong() * MB

                breakdownLines.add(i,
                        String.format("- Day %d (%s): Inbound = %d B (%s) / Outbound = %d B (%s)", i + 1,
                                wanTrafficData.date, inBytes,
                                byteCountToDisplaySize(inBytes)
                                        .replace("bytes", "B"), outBytes,
                                byteCountToDisplaySize(outBytes)
                                        .replace("bytes", "B")))

            }

            // Creating a dataset to hold each series
            val dataset = XYMultipleSeriesDataset()
            // Adding inbound Series to the dataset
            dataset.addSeries(inboundSeries)
            // Adding outbound Series to dataset
            dataset.addSeries(outboundSeries)

            val textAppearanceMediumSize = ViewGroupUtils.getTextAppearanceMediumSize(this)
            val textAppearanceSmallSize = ViewGroupUtils.getTextAppearanceSmallSize(this)

            // Creating XYSeriesRenderer to customize inboundSeries
            val inboundRenderer = XYSeriesRenderer()
            inboundRenderer.color = ColorUtils.getColor("WAN_TRAFFIC_BAR_IN")
            inboundRenderer.isFillPoints = true
            inboundRenderer.lineWidth = 2f
            inboundRenderer.isDisplayChartValues = false
            inboundRenderer.displayChartValuesDistance = 5 //setting chart value distance
            //            inboundRenderer.setChartValuesTextAlign(Align.CENTER);
            //            inboundRenderer.setChartValuesTextSize(textAppearanceSmallSize);

            // Creating XYSeriesRenderer to customize outboundSeries
            val outboundRenderer = XYSeriesRenderer()
            outboundRenderer.color = ColorUtils.getColor("WAN_TRAFFIC_BAR_OUT")
            outboundRenderer.isFillPoints = true
            outboundRenderer.lineWidth = 2f
            outboundRenderer.isDisplayChartValues = false
            //            outboundRenderer.setChartValuesTextAlign(Align.CENTER);
            //            outboundRenderer.setChartValuesTextSize(textAppearanceSmallSize);

            // Creating a XYMultipleSeriesRenderer to customize the whole chart
            val multiRenderer = XYMultipleSeriesRenderer()
            multiRenderer.margins = intArrayOf(30, 100, 10, 10) //top,left,bottom,right
            multiRenderer.barWidth = 25f
            multiRenderer.barSpacing = 0.5
            multiRenderer.orientation = XYMultipleSeriesRenderer.Orientation.HORIZONTAL
            multiRenderer.chartTitle = String.format("Date range: %s / Total IN: %s / Total OUT: %s",
                    mCycleItem!!.getLabelWithYears(),
                    byteCountToDisplaySize(totalIn * MB)
                            .replace("bytes", "B"),
                    byteCountToDisplaySize(totalOut * MB)
                            .replace("bytes", "B"))
            multiRenderer.xTitle = "Days"
            multiRenderer.yTitle = "Traffic"
            multiRenderer.isZoomButtonsVisible = false
            multiRenderer.labelsColor = ContextCompat.getColor(this, if (themeLight) R.color.black else R.color.theme_accent_1_light)

            //Add custom labels for the values we have here
            //setting no of values to display in y axis
            multiRenderer.yLabels = 0
            if (maxY != 0.0) {
                multiRenderer.addYTextLabel(maxY,
                        byteCountToDisplaySize(
                                Double.valueOf(maxY * MB)!!.toLong()).replace("bytes", "B"))
                multiRenderer.addYTextLabel(3 * maxY / 4,
                        byteCountToDisplaySize(
                                Double.valueOf(3.0 * maxY * MB.toDouble() / 4)!!.toLong()).replace("bytes", "B"))
                multiRenderer.addYTextLabel(maxY / 2,
                        byteCountToDisplaySize(
                                Double.valueOf(maxY * MB / 2)!!.toLong()).replace("bytes", "B"))
                multiRenderer.addYTextLabel(maxY / 4,
                        byteCountToDisplaySize(
                                Double.valueOf(maxY * MB / 4)!!.toLong()).replace("bytes", "B"))
            }

            multiRenderer.xLabelsAngle = 70f
            multiRenderer.xLabels = 0
            val daysLength = days.size
            var firstAndLastsDaysSet = false
            if (daysLength >= 3) {
                //                multiRenderer.addXTextLabel(0, days[0]);
                multiRenderer.addXTextLabel((daysLength - 1).toDouble(), days[daysLength - 1])
                firstAndLastsDaysSet = true
            }
            for (d in 0 until daysLength) {
                if (firstAndLastsDaysSet && (d == 0 || d == daysLength - 1)) {
                    continue
                }
                //Add labels every n days
                multiRenderer.addXTextLabel(d.toDouble(), if (d > 0 && d % (maxX / 5) == 0) days[d] else EMPTY_STRING)
            }

            //setting text size of the title
            //            multiRenderer.setChartTitleTextSize(35);
            multiRenderer.chartTitleTextSize = textAppearanceMediumSize
            //setting text size of the axis title
            multiRenderer.axisTitleTextSize = 25f
            //setting text size of the graph label
            multiRenderer.labelsTextSize = 25f
            multiRenderer.legendTextSize = textAppearanceSmallSize
            multiRenderer.isFitLegend = true
            multiRenderer.pointSize = 25f
            //setting pan ability which uses graph to move on both axis
            multiRenderer.setPanEnabled(true, false)
            //            multiRenderer.setPanLimits(new double[] {-1, maxX + 30});
            //setting click false on graph
            multiRenderer.isClickEnabled = true
            //setting lines to display on y axis
            multiRenderer.isShowGridY = false
            //setting lines to display on x axis
            multiRenderer.isShowGridX = false
            //setting legend to fit the screen size
            multiRenderer.isFitLegend = true
            //setting displaying line on grid
            multiRenderer.setShowGrid(false)
            //setting zoom
            multiRenderer.setZoomEnabled(true, false)
            //setting external zoom functions to false
            //            multiRenderer.setZoomRate(1.1f);
            multiRenderer.isExternalZoomEnabled = true
            //setting displaying lines on graph to be formatted(like using graphics)
            multiRenderer.isAntialiasing = true
            //setting to in scroll to false
            multiRenderer.isInScroll = false
            //setting x axis label align
            multiRenderer.xLabelsAlign = Paint.Align.CENTER
            //setting y axis label to align
            multiRenderer.setYLabelsAlign(Paint.Align.LEFT)
            //setting text style
            multiRenderer.setTextTypeface("sans_serif", Typeface.NORMAL)
            // setting y axis max value, Since i'm using static values inside the graph so i'm setting y max value to 4000.
            // if you use dynamic values then get the max y value and set here
            multiRenderer.yAxisMax = maxY + maxY / 10
            multiRenderer.yAxisMin = 0.0
            //setting used to move the graph on xaxiz to .5 to the right
            multiRenderer.xAxisMin = -1.0
            //setting max values to be display in x axis
            multiRenderer.xAxisMax = (maxX - 1).toDouble()
            //setting bar size or space between two bars
            multiRenderer.barSpacing = 0.5
            //Setting background color of the graph to transparent
            multiRenderer.backgroundColor = Color.TRANSPARENT
            //Setting margin color of the graph to transparent
            multiRenderer.marginsColor = ContextCompat.getColor(this, R.color.transparent_background)
            multiRenderer.isApplyBackgroundColor = true

            //setting the margin size for the graph in the order top, left, bottom, right
            multiRenderer.margins = intArrayOf(30, 30, 30, 30)

            val blackOrWhite = ContextCompat.getColor(this,
                    if (ColorUtils.isThemeLight(this)) R.color.black else R.color.white)
            multiRenderer.axesColor = blackOrWhite
            multiRenderer.xLabelsColor = blackOrWhite
            multiRenderer.setYLabelsColor(0, blackOrWhite)

            // Adding inboundRenderer and outboundRenderer to multipleRenderer
            // Note: The order of adding dataseries to dataset and renderers to multipleRenderer
            // should be same
            multiRenderer.addSeriesRenderer(inboundRenderer)
            multiRenderer.addSeriesRenderer(outboundRenderer)

            this.mChartView = ChartFactory.getBarChartView(this, dataset, multiRenderer, BarChart.Type.DEFAULT)
            //mChartView.repaint();
            val inboundRendererColor = inboundRenderer.color
            val outboundRendererColor = outboundRenderer.color
            val displayedTooltip = AtomicReference<ViewTooltip>(null)

            val positionX = AtomicReference<Float>(null)
            val positionY = AtomicReference<Float>(null)
            //Dynamic view, indicating where to place the tooltip
            val tooltipViewHolder = TextView(this@WANMonthlyTrafficActivity)
            tooltipViewHolder.visibility = View.VISIBLE

            mChartView!!.setOnTouchListener { v, event ->
                positionX.set(event.x)
                positionY.set(event.y)
                false // not consumed; forward to onClick
            }

            mChartView!!.setOnClickListener { v ->
                val currentSeriesAndPoint = mChartView!!.currentSeriesAndPoint
                if (currentSeriesAndPoint != null) {
                    val xAxisValue = java.lang.Double.valueOf(currentSeriesAndPoint.xValue)!!.toInt()
                    if (xAxisValue >= 0 && xAxisValue < size) {
                        //Series index: 0 => inbound, 1 => outbound
                        val seriesIndex = currentSeriesAndPoint.seriesIndex
                        if (seriesIndex == INBOUND || seriesIndex == OUTBOUND) {
                            //                                currentSeriesAndPoint.getPointIndex()
                            val inBytes = java.lang.Double.valueOf(inData[xAxisValue])!!.toLong() * MB
                            val outBytes = java.lang.Double.valueOf(outData[xAxisValue])!!.toLong() * MB
                            val posX = positionX.get()
                            val posY = positionY.get()
                            val touchPositionsSet = posX != null && posY != null
                            if (touchPositionsSet) {
                                tooltipViewHolder.text = " "
                                tooltipViewHolder.x = posX!!
                                tooltipViewHolder.y = posY!!
                                mTooltipPlaceholderView!!.visibility = View.VISIBLE
                                mTooltipPlaceholderView!!.removeAllViews()
                                mTooltipPlaceholderView!!.addView(tooltipViewHolder)
                            }
                            val tooltipText: String
                            if (seriesIndex == INBOUND) {
                                tooltipText = String.format(Locale.ENGLISH,
                                        "%s : Inbound : %s / Outbound : %s",
                                        days[xAxisValue],
                                        byteCountToDisplaySize(inBytes).replace("bytes", "B"),
                                        byteCountToDisplaySize(outBytes).replace("bytes", "B"))
                            } else {
                                tooltipText = String.format(Locale.ENGLISH,
                                        "%s : Outbound : %s / Inbound : %s",
                                        days[xAxisValue],
                                        byteCountToDisplaySize(outBytes).replace("bytes", "B"),
                                        byteCountToDisplaySize(inBytes).replace("bytes", "B"))
                            }
                            val viewTooltip = ViewTooltip.on(if (touchPositionsSet) tooltipViewHolder else v)
                                    .color(if (seriesIndex == INBOUND) inboundRendererColor else outboundRendererColor)
                                    .textColor(ContextCompat.getColor(
                                            this@WANMonthlyTrafficActivity, R.color.white))
                                    .autoHide(true, 10000)
                                    .clickToHide(true)
                                    .corner(30)
                                    .position(Position.TOP)
                                    .align(ALIGN.CENTER)
                                    .text(tooltipText)
                            viewTooltip.onDisplay { displayedTooltip.set(viewTooltip) }.onHide { displayedTooltip.set(null) }
                            val existingTooltip = displayedTooltip.getAndSet(viewTooltip)
                            try {
                                existingTooltip?.close()
                            } catch (e: Exception) {
                                Crashlytics.logException(e)
                            } finally {
                                viewTooltip.show()
                            }
                        } else {
                            Crashlytics.log(Log.WARN, LOG_TAG, "Unhandled series index: " + seriesIndex)
                        }
                    } else {
                        Crashlytics.log(Log.WARN, LOG_TAG,
                                "xAxisValue=$xAxisValue: out of X axis bounds: $daysLength")
                    }
                } else {
                    val existingTooltip = displayedTooltip.get()
                    try {
                        existingTooltip?.close()
                    } catch (e: Exception) {
                        Crashlytics.logException(e)
                    }

                }
            }
            mChartView!!.addPanListener {
                val start = multiRenderer.xAxisMin
                val stop = multiRenderer.xAxisMax
                multiRenderer.clearXTextLabels()
                val labels = MathHelper.getLabels(start, stop, 10)
                var labelToInt: Int
                for (label in labels) {
                    if (label == null) {
                        continue
                    }
                    labelToInt = label.toInt()
                    if (labelToInt < 0 || labelToInt >= daysLength) {
                        continue
                    }
                    multiRenderer.addXTextLabel(label, days[labelToInt])
                }
                multiRenderer.xLabels = 0
            }
            mChartView!!.addZoomListener(object : ZoomListener {
                override fun zoomApplied(zoomEvent: ZoomEvent) {
                    val start = multiRenderer.xAxisMin
                    val stop = multiRenderer.xAxisMax
                    multiRenderer.clearXTextLabels()
                    val labels = MathHelper.getLabels(start, stop, 10)
                    var labelToInt: Int
                    for (label in labels) {
                        if (label == null) {
                            continue
                        }
                        labelToInt = label.toInt()
                        if (labelToInt < 0 || labelToInt >= daysLength) {
                            continue
                        }
                        multiRenderer.addXTextLabel(label, days[labelToInt])
                    }
                    multiRenderer.xLabels = 0
                }

                override fun zoomReset() {}
            }, true, true)

            chartPlaceholderView.removeAllViews()
            chartPlaceholderView.addView(mChartView)

            chartPlaceholderView.visibility = View.VISIBLE
            loadingView.visibility = View.GONE
            findViewById<View>(R.id.tile_status_wan_monthly_traffic_chart_error).visibility = View.GONE
        } catch (e: Exception) {
            mException = e
            e.printStackTrace()
            Utils.reportException(null, e)
            val errorView = findViewById<TextView>(R.id.tile_status_wan_monthly_traffic_chart_error)
            errorView.text = "Internal Error. Please try again later. " + e.javaClass.simpleName + ": " + e
                    .message
            errorView.visibility = View.VISIBLE
            loadingView.visibility = View.GONE
            chartPlaceholderView.visibility = View.GONE
            if (optionsMenu != null) {
                optionsMenu!!.findItem(R.id.tile_status_wan_monthly_traffic_share).isEnabled = false
            }
        }

    }

    private fun setShareFiles(vararg files: File?) {
        if (mShareActionProvider == null) {
            return
        }

        val uriForFiles = files.filterNotNull().map {
            FileProvider.getUriForFile(this@WANMonthlyTrafficActivity, RouterCompanionAppConstants.FILEPROVIDER_AUTHORITY, it) }

        mShareActionProvider!!.setOnShareTargetSelectedListener { _, intent ->
            uriForFiles.forEach { grantUriPermission(intent.component!!.packageName, it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            true
        }

        val totalOutBytes = totalOut * MB
        val totalInBytes = totalIn * MB

        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND_MULTIPLE
        sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uriForFiles))
        sendIntent.type = "text/html"
        sendIntent.putExtra(Intent.EXTRA_SUBJECT,
                "WAN Traffic for Router '$mRouterDisplay': ${mCycleItem!!.getLabelWithYears()}")
        sendIntent.putExtra(Intent.EXTRA_TEXT, fromHtml(
                "Traffic Breakdown<br/><br/> >>> " +
                        "Total Inbound: $totalInBytes B (${byteCountToDisplaySize(totalInBytes)
                                .replace("bytes", "B", true)}) / " +
                        "Total Outbound: $totalOutBytes B " +
                        "(${byteCountToDisplaySize(totalOutBytes).replace("bytes", "B", true)}) " +
                        "<<<<br/><br/>${breakdownLines.joinToString("<br/>")}${Utils.getShareIntentFooter()}"))

//        sendIntent.data = uriForFile
        //        sendIntent.setType("image/png");
        sendIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        setShareIntent(sendIntent)
    }

    // Call to update the share intent
    private fun setShareIntent(shareIntent: Intent) {
        mShareActionProvider?.setShareIntent(shareIntent)
    }

    companion object {

        val WAN_CYCLE = "WAN_CYCLE"

        val WAN_MONTHLY_TRAFFIC = "WAN Traffic"

        val COMPRESSION_QUALITY = 100

        val DEFAULT_BITMAP_WIDTH = 640

        val DEFAULT_BITMAP_HEIGHT = 480

        private val LOG_TAG = WANMonthlyTrafficActivity::class.java.simpleName

//        private val DECIMAL_FORMAT = DecimalFormat("#.##")
    }
}
