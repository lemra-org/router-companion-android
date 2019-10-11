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

package org.rm3l.router_companion.mgmt

import org.rm3l.router_companion.RouterCompanionAppConstants.ACRA_ENABLE
import org.rm3l.router_companion.RouterCompanionAppConstants.DEBUG_MODE
import org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY
import org.rm3l.router_companion.RouterCompanionAppConstants.MAX_ROUTERS_FREE_VERSION
import org.rm3l.router_companion.RouterCompanionAppConstants.NOTIFICATIONS_BG_SERVICE_ENABLE
import org.rm3l.router_companion.RouterCompanionAppConstants.NOTIFICATIONS_SYNC_INTERVAL_MINUTES_PREF
import org.rm3l.router_companion.RouterCompanionAppConstants.THEMING_PREF
import org.rm3l.router_companion.RouterCompanionApplication.Companion.DEBUG_RESOURCE_INSPECTOR_PREF_KEY

import android.app.Activity
import android.app.AlertDialog.Builder
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.DialogFragment
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.ItemTouchHelper
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import co.paulburke.android.itemtouchhelperdemo.helper.ItemTouchHelperAdapter
import co.paulburke.android.itemtouchhelperdemo.helper.OnStartDragListener
import com.airbnb.deeplinkdispatch.DeepLink
import com.crashlytics.android.Crashlytics
import com.evernote.android.job.JobManager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.InterstitialAd
import com.google.common.base.Joiner
import com.readystatesoftware.chuck.Chuck
import com.stephentuso.welcome.WelcomeHelper
import com.stephentuso.welcome.WelcomeActivity
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicInteger
import org.rm3l.ddwrt.BuildConfig
import org.rm3l.ddwrt.R
import org.rm3l.router_companion.RouterCompanionAppConstants
import org.rm3l.router_companion.actions.ActionManager
import org.rm3l.router_companion.actions.RebootRouterAction
import org.rm3l.router_companion.actions.RouterAction
import org.rm3l.router_companion.actions.RouterActionListener
import org.rm3l.router_companion.help.ChangelogActivity
import org.rm3l.router_companion.help.HelpActivity
import org.rm3l.router_companion.job.RouterCompanionJobCreator
import org.rm3l.router_companion.job.speedtest.RouterSpeedTestAutoRunnerJob
import org.rm3l.router_companion.main.DDWRTMainActivity
import org.rm3l.router_companion.mgmt.RouterManagementActivity.RoutersListRefreshCause.DATA_SET_CHANGED
import org.rm3l.router_companion.mgmt.RouterManagementActivity.RoutersListRefreshCause.INSERTED
import org.rm3l.router_companion.mgmt.RouterManagementActivity.RoutersListRefreshCause.REMOVED
import org.rm3l.router_companion.mgmt.RouterManagementActivity.RoutersListRefreshCause.UPDATED
import org.rm3l.router_companion.mgmt.adapters.RouterListItemTouchHelperCallback
import org.rm3l.router_companion.mgmt.adapters.RouterListRecycleViewAdapter
import org.rm3l.router_companion.mgmt.dao.DDWRTCompanionDAO
import org.rm3l.router_companion.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteDAOImpl
import org.rm3l.router_companion.mgmt.register.ManageRouterFragmentActivity
import org.rm3l.router_companion.resources.conn.Router
import org.rm3l.router_companion.settings.RouterManagementSettingsActivity
import org.rm3l.router_companion.utils.AdUtils
import org.rm3l.router_companion.utils.ColorUtils
import org.rm3l.router_companion.utils.ImageUtils
import org.rm3l.router_companion.utils.Utils
import org.rm3l.router_companion.utils.customtabs.CustomTabActivityHelper
import org.rm3l.router_companion.utils.kotlin.*
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style
import org.rm3l.router_companion.welcome.GettingStartedActivity
import org.rm3l.router_companion.widgets.RecyclerViewEmptySupport

@DeepLink("dd-wrt://management", "ddwrt://management")
class RouterManagementActivity : AppCompatActivity(), View.OnClickListener, RouterMgmtDialogListener,
    SearchView.OnQueryTextListener, SwipeRefreshLayout.OnRefreshListener, CustomTabActivityHelper.ConnectionCallback,
    OnStartDragListener {

    private var addNewButton: FloatingActionButton? = null

    private var dao: DDWRTCompanionDAO? = null

    private var mAdapter: RecyclerView.Adapter<*>? = null

    private var mAutoCrashReports: Boolean = false

    private var mBackgroundServiceEnabled: Boolean = false

    private var mBackgroundServiceFrequency: Long = 0

    private var mCloseOnActionDone: Boolean = false

    private var mCurrentTheme: Long = 0

    private var mCustomTabActivityHelper: CustomTabActivityHelper? = null

    //    private Pusher mPusher;
    private var mInterstitialAd: InterstitialAd? = null

    private var mItemTouchHelper: ItemTouchHelper? = null

    private var mLayoutManager: RecyclerView.LayoutManager? = null

    private var mPreferences: SharedPreferences? = null

    private var mRecyclerView: RecyclerViewEmptySupport? = null

    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null

    private var mToolbar: Toolbar? = null

    private var optionsMenu: Menu? = null

    private var welcomeScreen: WelcomeHelper? = null

    enum class RoutersListRefreshCause {
        INSERTED, REMOVED, DATA_SET_CHANGED, UPDATED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Dynamic shortcuts are not preserved during backup/restore
        if (VERSION.SDK_INT >= VERSION_CODES.N_MR1) {
            val shortcutManager = getSystemService(ShortcutManager::class.java)
            // Application restored. Need to re-publish dynamic shortcuts.
            val pinnedShortcuts = shortcutManager!!.pinnedShortcuts
            if (pinnedShortcuts.isNotEmpty()) {
                // Pinned shortcuts have been restored. Use
                // updateShortcuts(List) to make sure they
                // contain up-to-date information.
                val dynamicPinnedShortcutsToUpdate = ArrayList<ShortcutInfo>()
                for (pinnedShortcut in pinnedShortcuts) {
                    if (pinnedShortcut == null) {
                        continue
                    }
                    if (pinnedShortcut.isDynamic) {
                        dynamicPinnedShortcutsToUpdate.add(pinnedShortcut)
                    }
                }
                shortcutManager.updateShortcuts(dynamicPinnedShortcutsToUpdate)
            }
        }

        val intent = intent
        handleIntent(intent)

        //Default values are not set by default
        //Android bug workaround: http://code.google.com/p/android/issues/detail?id=6641
        PreferenceManager.setDefaultValues(
            this, DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE,
            R.xml.router_management_settings, false
        )

        mPreferences = getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
        mCurrentTheme = mPreferences!!.getLong(THEMING_PREF, RouterCompanionAppConstants.DEFAULT_THEME)
        mAutoCrashReports = mPreferences!!.getBoolean(ACRA_ENABLE, true)

        ColorUtils.setAppTheme(this, null, false)

        mBackgroundServiceEnabled = mPreferences!!.getBoolean(NOTIFICATIONS_BG_SERVICE_ENABLE, false)
        mBackgroundServiceFrequency = mPreferences!!.getLong(NOTIFICATIONS_SYNC_INTERVAL_MINUTES_PREF, -1)

        setContentView(R.layout.activity_router_management)

        setupCustomTabHelper(this)

        AdUtils.buildAndDisplayAdViewIfNeeded(this, findViewById<View>(R.id.router_list_adView) as AdView)

        mInterstitialAd = AdUtils.requestNewInterstitial(
            this,
            R.string.interstitial_ad_unit_id_router_list_to_router_main
        )

        mToolbar = findViewById<View>(R.id.routerManagementActivityToolbar) as Toolbar
        if (mToolbar != null) {
            mToolbar!!.title = "Routers"
            mToolbar!!.setTitleTextAppearance(applicationContext, R.style.ToolbarTitle)
            mToolbar!!.setSubtitleTextAppearance(applicationContext, R.style.ToolbarSubtitle)
            mToolbar!!.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
            mToolbar!!.setSubtitleTextColor(ContextCompat.getColor(this, R.color.white))
            setSupportActionBar(mToolbar)
        }

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false)
            actionBar.setHomeButtonEnabled(false)
            actionBar.setDisplayShowHomeEnabled(true)
            actionBar.setIcon(R.mipmap.ic_launcher_ddwrt_companion)
        }

        this.dao = getDao(this)

        val allRouters = this.dao!!.allRouters

        mRecyclerView = findViewById(R.id.routersListView)

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        // allows for optimizations if all items are of the same size:
        mRecyclerView!!.setHasFixedSize(true)

        // use a linear layout manager
        mLayoutManager = LinearLayoutManager(this)
        mLayoutManager!!.scrollToPosition(0)
        mRecyclerView!!.layoutManager = mLayoutManager

        val emptyView = findViewById<View>(R.id.empty_view) as TextView
        if (ColorUtils.isThemeLight(this)) {
            emptyView.setTextColor(ContextCompat.getColor(this, R.color.black))
        } else {
            emptyView.setTextColor(ContextCompat.getColor(this, R.color.white))
        }
        mRecyclerView!!.setEmptyView(emptyView)

        // specify an adapter (see also next example)
        mAdapter = RouterListRecycleViewAdapter(this, allRouters)
        mRecyclerView!!.adapter = mAdapter

        val callback = RouterListItemTouchHelperCallback(mRecyclerView, mAdapter as ItemTouchHelperAdapter?)
        this.mItemTouchHelper = ItemTouchHelper(callback)
        mItemTouchHelper!!.attachToRecyclerView(mRecyclerView)

        //        final RecyclerView.ItemDecoration itemDecoration =
        //                new DividerItemDecoration(this, LinearLayoutManager.VERTICAL);
        //        mRecyclerView.addItemDecoration(itemDecoration);

        /*
         * onClickDetection is done in this Activity's onItemTouchListener
         * with the help of a GestureDetector;
         * Tip by Ian Lake on G+ in a comment to this post:
         * https://plus.google.com/+LucasRocha/posts/37U8GWtYxDE
         */
        //        mRecyclerView.addOnItemTouchListener(this);
        //        gestureDetector = new GestureDetectorCompat(this, new RouterManagementViewOnGestureListener());

        addNewButton = findViewById<View>(R.id.router_list_add) as FloatingActionButton

        //Attach to recyclerview for scrolling effect
        //        addNewButton.attachToRecyclerView(mRecyclerView);

        addNewButton!!.setOnClickListener(this)

        //        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        //            addNewButton.setOutlineProvider(new ViewOutlineProvider() {
        //                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        //                @Override
        //                public void getOutline(View view, Outline outline) {
        //                    final int diameter = getResources().getDimensionPixelSize(R.dimen.diameter);
        //                    outline.setOval(0, 0, diameter, diameter);
        //                }
        //            });
        //            addNewButton.setClipToOutline(true);
        //        }

        mSwipeRefreshLayout = findViewById<View>(R.id.swipeRefreshLayout) as SwipeRefreshLayout
        mSwipeRefreshLayout!!.setOnRefreshListener(this)
        mSwipeRefreshLayout!!.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light, android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        //        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
        //            @Override
        //            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        //            }
        //
        //            @Override
        //            public void onScrolled(RecyclerView recyclerView, int firstVisibleItem, int visibleItemCount) {
        //                boolean enable = false;
        //                if(recyclerView != null && recyclerView.getChildCount() > 0){
        //                    final LinearLayoutManager layoutManager = (LinearLayoutManager)
        //                            recyclerView.getLayoutManager();
        //                    if (layoutManager != null) {
        //                        // check if the first item of the list is visible
        //                        final boolean firstItemVisible =
        //                                (layoutManager.findFirstVisibleItemPosition() == 0);
        //
        //                        // check if the top of the first item is visible
        //                        final View childAt = layoutManager.getChildAt(0);
        //                        final boolean topOfFirstItemVisible = (childAt != null &&
        //                                childAt.getTop() == 0);
        //
        //                        // enabling or disabling the refresh layout
        //                        enable = firstItemVisible && topOfFirstItemVisible;
        //                    }
        //                }
        //                mSwipeRefreshLayout.setEnabled(enable);
        ////                super.onScrolled(recyclerView, dx, dy);
        //            }
        //        });

        ////If needed, register to DD-WRT Build Updates
        //final Set<String> notificationChoices =
        //    this.mPreferences.getStringSet(NOTIFICATIONS_CHOICE_PREF, new HashSet<String>());
        //if (notificationChoices.contains(CLOUD_MESSAGING_TOPIC_DDWRT_BUILD_UPDATES)) {
        //  //Subscribe to topic
        //  FirebaseMessaging.getInstance().subscribeToTopic(CLOUD_MESSAGING_TOPIC_DDWRT_BUILD_UPDATES);
        //} else {
        //  //Unsubscribe from topic
        //  FirebaseMessaging.getInstance()
        //      .unsubscribeFromTopic(CLOUD_MESSAGING_TOPIC_DDWRT_BUILD_UPDATES);
        //}

        Utils.requestAppPermissions(this)

        //No need to restart the background service each time - this is correctly handled by Android
        //        BootReceiver.doStartBackgroundServiceIfNeeded(this);

        /* Use this when you want to run a background update check */
        //        final UpdateRunnable updateRunnable =
        //                new UpdateRunnable(this, new Handler());
        //        if (StringUtils.startsWithIgnoreCase(FLAVOR, "google")) {
        //            //This library currently supports Google Play only
        //            updateRunnable.start();
        //        }

        Utils.displayRatingBarIfNeeded(this)

        welcomeScreen = WelcomeHelper(this, GettingStartedActivity::class.java)

        if (ACTION_OPEN_ADD_ROUTER_WIZARD == intent.action) {
            mCloseOnActionDone = intent.getBooleanExtra(CLOSE_ON_ACTION_DONE, false)
            this.openAddRouterForm()
        } else {
            welcomeScreen!!.show(savedInstanceState)
        }
    }

    override fun onStart() {
        super.onStart()
        //        if (this.mPusher != null) {
        //            this.mPusher.connect();
        //        }
        mCustomTabActivityHelper!!.bindCustomTabsService(this)

        //#199: app shortcuts
        setDynamicAppShortcuts()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (mPreferences!!.getBoolean(DEBUG_MODE, false)) {
            Crashlytics.log(
                Log.DEBUG, LOG_TAG,
                "onActivityResult($requestCode,$resultCode,$data"
            )
        }
        // Check which request we're responding to
        when (requestCode) {
            ROUTER_MANAGEMENT_SETTINGS_ACTIVITY_CODE -> {
                // Make sure the request was successful and reload U if necessary
                if (resultCode == Activity.RESULT_OK) {
                    //If needed, register to DD-WRT Build Updates
                    //final Set<String> notificationChoices =
                    //    this.mPreferences.getStringSet(NOTIFICATIONS_CHOICE_PREF, new HashSet<String>());
                    //if (notificationChoices.contains(CLOUD_MESSAGING_TOPIC_DDWRT_BUILD_UPDATES)) {
                    //  //Subscribe to topic
                    //  FirebaseMessaging.getInstance()
                    //      .subscribeToTopic(CLOUD_MESSAGING_TOPIC_DDWRT_BUILD_UPDATES);
                    //} else {
                    //  //Unsubscribe from topic
                    //  FirebaseMessaging.getInstance()
                    //      .unsubscribeFromTopic(CLOUD_MESSAGING_TOPIC_DDWRT_BUILD_UPDATES);
                    //}

                    //Reset Crashlytics user email addr
                    val acraEmailAddr =
                        this.mPreferences!!.getString(RouterCompanionAppConstants.ACRA_USER_EMAIL, null)
                    Crashlytics.setUserEmail(acraEmailAddr)

                    val currentUserChoiceForAutoCrashReporting = this.mPreferences!!.getBoolean(ACRA_ENABLE, true)
                    if (this.mAutoCrashReports != currentUserChoiceForAutoCrashReporting) {
                        //Restart activity
                        Crashlytics.log(
                            Log.DEBUG, LOG_TAG,
                            "<mAutoCrashReports,currentUserChoiceForAutoCrashReporting>=<" +
                                    mAutoCrashReports + "," + currentUserChoiceForAutoCrashReporting + ">"
                        )
                        val waitMessage = String.format(
                            "%sabling automatic crash reporting and app usage analytics",
                            if (currentUserChoiceForAutoCrashReporting) "En" else "Dis"
                        )
                        this.restartWholeApplication(waitMessage, null)
                    } else if (this.mCurrentTheme != this.mPreferences!!.getLong(THEMING_PREF, -1L)
                        || this.mBackgroundServiceEnabled != this.mPreferences!!.getBoolean(
                            NOTIFICATIONS_BG_SERVICE_ENABLE, false
                        )
                        || this.mBackgroundServiceFrequency != this.mPreferences!!.getLong(
                            NOTIFICATIONS_SYNC_INTERVAL_MINUTES_PREF, -1L
                        )
                    ) {
                        //Reload UI
                        this.finishAndReload("Reloading UI", null, null)
                    }
                }
            }
            NEW_ROUTER_ADDED -> {
                // Make sure the request was successful and reload U if necessary
                if (resultCode == Activity.RESULT_OK) {
                    onRouterAdd()
                }
            }
            ROUTER_UPDATED -> {
                if (resultCode == Activity.RESULT_OK) {
                    doRefreshRoutersListWithSpinner(DATA_SET_CHANGED, null)
                }
            }
            WelcomeHelper.DEFAULT_WELCOME_SCREEN_REQUEST -> {
                //TODO
                val welcomeKey = \"fake-key\";
                if (resultCode == Activity.RESULT_OK) {
                    // Code here will run if the welcome screen was completed
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(
                            applicationContext, "[DEBUG] $welcomeKey completed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(
                            applicationContext, "[DEBUG] $welcomeKey canceled",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                initOpenAddRouterFormIfNecessary()
            }
            else -> {
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        mCustomTabActivityHelper!!.unbindCustomTabsService(this)
        //        if (this.mPusher != null) {
        //            this.mPusher.disconnect();
        //        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        welcomeScreen!!.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        //Dismiss existing dialog fragments, if any
        var fragment = supportFragmentManager.findFragmentByTag(ADD_ROUTER_FRAGMENT_TAG)
        if (fragment is DialogFragment) {
            fragment.dismiss()
        }

        fragment = supportFragmentManager.findFragmentByTag(UPDATE_ROUTER_FRAGMENT_TAG)
        if (fragment is DialogFragment) {
            fragment.dismiss()
        }

        super.onDestroy()
    }

    override fun onBackPressed() {
        if (BuildConfig.WITH_ADS && mInterstitialAd != null && AdUtils.canDisplayInterstialAd(this)) {

            mInterstitialAd!!.adListener = object : AdListener() {
                override fun onAdClosed() {
                    super@RouterManagementActivity.onBackPressed()
                }

                override fun onAdOpened() {
                    //Save preference
                    getSharedPreferences(
                        DEFAULT_SHARED_PREFERENCES_KEY,
                        Context.MODE_PRIVATE
                    ).edit()
                        .putLong(
                            RouterCompanionAppConstants.AD_LAST_INTERSTITIAL_PREF,
                            System.currentTimeMillis()
                        )
                        .apply()
                }
            }

            if (mInterstitialAd!!.isLoaded) {
                mInterstitialAd!!.show()
            } else {
                super@RouterManagementActivity.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onClick(view: View?) {
        if (view == null) {
            return
        }

        if (view.id == R.id.router_list_add) {
            this.openAddRouterForm()
        }
        //        else if (view.getId() == R.id.router_item_cardview_content) {
        //            // item click
        //            final int idx = mRecyclerView.getChildPosition(view);
        //            final RouterListRecycleViewAdapter adapter = (RouterListRecycleViewAdapter) mAdapter;
        //            if (actionMode != null) {
        //                final int previousSelectedItemCount = adapter.getSelectedItemCount();
        //                myToggleSelection(idx);
        //                //Set background color, depending on whether this is a selection or a de-selection
        //                final int currentSelectedItemCount = adapter.getSelectedItemCount();
        //                if (currentSelectedItemCount == previousSelectedItemCount - 1) {
        //                    //De-selection: remove background
        //                    view.setBackgroundResource(android.R.color.transparent);
        //                } else if (currentSelectedItemCount == previousSelectedItemCount + 1) {
        //                    //Selection: apply background
        //                    if (ColorUtils.isThemeLight(this)) {
        //                        view.setBackgroundResource(R.color.DarkOrange);
        //                    } else {
        //                        view.setBackgroundResource(R.color.yellow);
        //                    }
        //                } //other cases should not occur (as this is a single selection)
        //
        //                //Now hide ActionMode if selected items count falls to 0
        //                if (currentSelectedItemCount == 0) {
        //                    actionMode.finish();
        //                }
        //                return;
        //            }
        //
        //            //No action mode - normal mode => open up main activity for this router
        //            final List<Router> routersList = adapter.getRoutersList();
        //            final Router router;
        //            if (idx < 0 || idx >= routersList.size() || (router = routersList.get(idx)) == null) {
        //                Crouton.makeText(RouterManagementActivity.this,
        //                        "Unknown router - please refresh list or add a new one.", Style.ALERT).show();
        //                return;
        //            }
        //
        //            //FIXME Uncomment once other firmwares are fully supported
        ////            final Router.RouterFirmware routerFirmware = router.getRouterFirmware();
        ////            if (routerFirmware == null || Router.RouterFirmware.UNKNOWN.equals(routerFirmware)) {
        ////                Utils.displayMessage(this, "Router Firmware unknown or not supported (yet!). " +
        ////                        "You may manually force the router firmware to use by editing this entry.", Style.ALERT);
        ////                return;
        ////            }
        //            //FIXME End
        //
        //            final String routerUuid = router.getUuid();
        //
        //            final Intent ddWrtMainIntent = new Intent(RouterManagementActivity.this, DDWRTMainActivity.class);
        //            ddWrtMainIntent.putExtra(ROUTER_SELECTED, routerUuid);
        //
        //            final SharedPreferences routerSharedPreferences = getSharedPreferences(routerUuid, Context.MODE_PRIVATE);
        //            if (!routerSharedPreferences.getBoolean(OPENED_AT_LEAST_ONCE_PREF_KEY, false)) {
        //                routerSharedPreferences.edit()
        //                        .putBoolean(OPENED_AT_LEAST_ONCE_PREF_KEY, true)
        //                        .apply();
        //            }
        //
        //            if (BuildConfig.WITH_ADS &&
        //                    mInterstitialAd != null &&
        //                    AdUtils.canDisplayInterstialAd(this)) {
        //                mInterstitialAd.setAdListener(new AdListener() {
        //                        @Override
        //                        public void onAdClosed() {
        //                            startActivity(view, ddWrtMainIntent);
        //                        }
        //
        //                        @Override
        //                        public void onAdOpened() {
        //                            mPreferences.edit()
        //                                    .putLong(
        //                                            DDWRTCompanionConstants.AD_LAST_INTERSTITIAL_PREF,
        //                                            System.currentTimeMillis())
        //                                    .apply();
        //                        }
        //                });
        //
        //                if (mInterstitialAd.isLoaded()) {
        //                    mInterstitialAd.show();
        //                } else {
        ////                    final AlertDialog alertDialog = Utils.buildAlertDialog(this, null, "Loading...", false, false);
        ////                    alertDialog.show();
        ////                    ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
        //                    final ProgressDialog alertDialog = ProgressDialog.show(RouterManagementActivity.this,
        //                            "Loading Router details", "Please wait...", true);
        //                    new Handler().postDelayed(new Runnable() {
        //                        @Override
        //                        public void run() {
        //                            startActivity(view, ddWrtMainIntent);
        //                            alertDialog.cancel();
        //                        }
        //                    }, 1000);
        //                }
        //
        //            } else {
        ////                final AlertDialog alertDialog = Utils.buildAlertDialog(this, null, "Loading...", false, false);
        ////                alertDialog.show();
        ////                ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
        //                final ProgressDialog alertDialog = ProgressDialog.show(RouterManagementActivity.this,
        //                        "Loading Router details", "Please wait...", true);
        //                new Handler().postDelayed(new Runnable() {
        //                    @Override
        //                    public void run() {
        //                        startActivity(view, ddWrtMainIntent);
        //                        alertDialog.cancel();
        //                    }
        //                }, 1000);
        //            }
        //
        //        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.optionsMenu = menu
        menuInflater.inflate(R.menu.menu_router_management, menu)

        val debugOnlyTools = menu.findItem(R.id.debug_only)
        debugOnlyTools.isEnabled = BuildConfig.DEBUG
        debugOnlyTools.isVisible = BuildConfig.DEBUG

        val debugResourceInspector = mPreferences!!.getBoolean(
            DEBUG_RESOURCE_INSPECTOR_PREF_KEY, false
        )
        Crashlytics.log(Log.DEBUG, LOG_TAG, "XXX debug_resourceInspector: $debugResourceInspector")
        menu.findItem(R.id.debug_resourceinspector).isChecked = debugResourceInspector

        val donateMenuItem = menu.findItem(R.id.router_list_donate)
        if (donateMenuItem != null) {
            donateMenuItem.isVisible = BuildConfig.DONATIONS
        }

        val removeAdsMenuItem = menu.findItem(R.id.router_list_remove_ads)
        if (removeAdsMenuItem != null) {
            removeAdsMenuItem.isVisible = BuildConfig.WITH_ADS
        }

        //Search
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager

        val searchView = menu.findItem(R.id.router_list_refresh_search).actionView as SearchView

        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))

        searchView.setOnQueryTextListener(this)

        // Get the search close button image view
        val closeButton = searchView.findViewById<View>(R.id.search_close_btn) as ImageView
        closeButton.setOnClickListener {
            //Reset views
            val adapter = mAdapter as RouterListRecycleViewAdapter?
            adapter!!.routersList = dao!!.allRouters
            adapter.notifyDataSetChanged()
            //Hide it now
            searchView.isIconified = true
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onCustomTabsConnected() {
        //We may make UI changes
        Crashlytics.log(Log.DEBUG, LOG_TAG, "onCustomTabsConnected")
    }

    override fun onCustomTabsDisconnected() {
        //We may make UI changes
        Crashlytics.log(Log.DEBUG, LOG_TAG, "onCustomTabsDisconnected")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val adapter = mAdapter as RouterListRecycleViewAdapter?
        val routersList = adapter!!.routersList

        val itemId = item.itemId
        if (itemId == android.R.id.home) {
            onBackPressed()
            return true
        } else if (itemId == R.id.exit) {
            finish()
            return true
        } else if (itemId == R.id.help) {
            CustomTabActivityHelper.openCustomTab(this@RouterManagementActivity, null,
                RouterCompanionAppConstants.REMOTE_HELP_WEBSITE, null, null,
                { activity, uri ->
                    activity.startActivity(
                        Intent(this@RouterManagementActivity, HelpActivity::class.java)
                    )
                }, false
            )
            return true
        } else if (itemId == R.id.changelog) {
            CustomTabActivityHelper.openCustomTab(this@RouterManagementActivity, null,
                RouterCompanionAppConstants.REMOTE_HELP_WEBSITE_CHANGELOG, null, null,
                { activity, uri ->
                    activity.startActivity(
                        Intent(this@RouterManagementActivity, ChangelogActivity::class.java)
                    )
                }, false
            )
            return true
        } else if (itemId == R.id.debug_open_sharedprefs) {
            if (BuildConfig.DEBUG) {
                Toast.makeText(this, "[Chuck] Not implemented", Toast.LENGTH_SHORT).show()
                //Preferator.launch(this);
            } else {
                Crashlytics.log(
                    Log.WARN, LOG_TAG,
                    "[DEBUG] SharedPreferences menu option should not be visible..."
                )
            }
            return true
        } else if (itemId == R.id.debug_open_chuck) {
            if (BuildConfig.DEBUG) {
                startActivity(Chuck.getLaunchIntent(this))
            } else {
                Crashlytics.log(
                    Log.WARN, LOG_TAG,
                    "[DEBUG] 'Chuck' menu option should not be visible..."
                )
            }
            return true
        } else if (itemId == R.id.debug_resourceinspector) {
            if (BuildConfig.DEBUG) {
                val checked = item.isChecked
                item.isChecked = !checked
                val commit = mPreferences!!.edit().putBoolean(DEBUG_RESOURCE_INSPECTOR_PREF_KEY, !checked).commit()
                Utils.requestBackup(this@RouterManagementActivity)
                //Restart activity
                val waitMessage = String.format(
                    "%sabling ResourceInspector. Pref. update commit=%s",
                    if (checked) "Dis" else "En", commit
                )
                this.restartWholeApplication(waitMessage, null)
            } else {
                Crashlytics.log(
                    Log.WARN, LOG_TAG,
                    "[DEBUG] ResourceInspector menu option should not be visible..."
                )
            }
            return true
        } else if (itemId == R.id.debug_welcome_screen) {
            if (BuildConfig.DEBUG) {
                if (welcomeScreen != null) {
                    welcomeScreen!!.forceShow()
                }
            } else {
                Crashlytics.log(
                    Log.WARN, LOG_TAG,
                    "[DEBUG] 'Force-show welcome screen' menu option should not be visible..."
                )
            }
            return true
        } else if (itemId == R.id.debug_restore_deleted_routers) {
            if (BuildConfig.DEBUG) {
                val allRoutersIncludingArchived = dao!!.allRoutersIncludingArchived
                var nbRoutersRestored = 0
                for (potentiallyArchivedRouter in allRoutersIncludingArchived) {
                    if (!potentiallyArchivedRouter.isArchived) {
                        continue
                    }
                    potentiallyArchivedRouter.isArchived = false
                    dao!!.updateRouter(potentiallyArchivedRouter)
                    nbRoutersRestored++
                }
                if (nbRoutersRestored > 0) {
                    doRefreshRoutersListWithSpinner(DATA_SET_CHANGED, null)
                    val msg = "[DEBUG] Restored $nbRoutersRestored routers."
                    Crashlytics.log(Log.DEBUG, LOG_TAG, msg)
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
            } else {
                Crashlytics.log(
                    Log.WARN, LOG_TAG,
                    "[DEBUG] 'Restore deleted routers' menu option should not be visible..."
                )
            }
            return true
        } else if (itemId == R.id.debug_run_jobs_right_away) {
            if (BuildConfig.DEBUG) {
                val jobTags = RouterCompanionJobCreator.getOneShotJobTags()
                if (jobTags.isEmpty()) {
                    Toast.makeText(this, "Jobs Tags set is empty", Toast.LENGTH_SHORT).show()
                } else {
                    Builder(this)
                        .setTitle("Select Job to trigger")
                        .setItems(jobTags.toTypedArray()) { _, which ->
                            RouterCompanionJobCreator.runJobImmediately(
                                jobTags[which]
                            )
                        }
                        .show()
                }
            } else {
                Crashlytics.log(
                    Log.WARN, LOG_TAG,
                    "[DEBUG] 'Run Jobs right away' menu option should not be visible..."
                )
            }
            return true
        } else if (itemId == R.id.debug_cancel_all_jobs) {
            if (BuildConfig.DEBUG) {
                val jobTags = RouterCompanionJobCreator.JOB_MAP.keys
                val allRouters = dao!!.allRouters
                for (jobTag in jobTags) {
                    JobManager.instance().cancelAllForTag(jobTag)
                    //Also for speed-test jobs
                    for (router in allRouters) {
                        JobManager.instance()
                            .cancelAllForTag(
                                RouterSpeedTestAutoRunnerJob.getActualRouterJobTag(
                                    jobTag,
                                    router.uuid
                                )
                            )
                    }
                }
                Toast.makeText(this, "Requested cancellation for: $jobTags", Toast.LENGTH_LONG)
                    .show()
            } else {
                Crashlytics.log(
                    Log.WARN, LOG_TAG,
                    "[DEBUG] 'Cancel ALL jobs' menu option should not be visible..."
                )
            }
            return true
        } else if (itemId == R.id.router_list_actionbar_add) {
            this.openAddRouterForm()
            return true
        } else if (itemId == R.id.router_list_take_bug_report) {
            Utils.takeBugReport(this)
            return true
        } else if (itemId == R.id.router_list_refresh) {
            doRefreshRoutersListWithSpinner(DATA_SET_CHANGED, null)
            return true
        } else if (itemId == R.id.router_list_donate) {
            Utils.openDonateActivity(this)
            return true
        } else if (itemId == R.id.router_list_about) {
            Utils.launchAboutActivity(this@RouterManagementActivity)
            return true
        } else if (itemId == R.id.router_list_settings) {
            this.startActivityForResult(
                Intent(this, RouterManagementSettingsActivity::class.java),
                ROUTER_MANAGEMENT_SETTINGS_ACTIVITY_CODE
            )
            return true
        } else if (itemId == R.id.router_list_feedback) {
            Utils.openFeedbackForm(this, "")
            return true
        } else if (itemId == R.id.router_list_actions_restore_factory_defaults) {//TODO Hidden for now
            return true
        } else if (itemId == R.id.router_list_actions_firmwares_upgrade) {//TODO Hidden for now
            return true
        } else if (itemId == R.id.router_list_remove_ads) {
            Utils.displayUpgradeMessageForAdsRemoval(this)
            return true
        } else if (itemId == R.id.router_list_actions_reboot_routers) {
            if (routersList == null || routersList.isEmpty()) {
                Utils.displayMessage(
                    this@RouterManagementActivity,
                    "Empty Router list - action not submitted.", Style.INFO
                )
                return true
            }
            val allRoutersStr = ArrayList<String>()
            for (router in routersList) {
                if (router == null) {
                    continue
                }
                allRoutersStr.add(
                    String.format("'%s' (%s)", router.displayName, router.remoteIpAddress)
                )
            }
            Builder(this).setIcon(R.drawable.ic_action_alert_warning)
                .setTitle("Reboot All Router(s)?")
                .setMessage(
                    String.format(
                        "Are you sure you wish to continue? " + "The following Routers will be rebooted: \n\n%s",
                        Joiner.on("\n").skipNulls().join(allRoutersStr)
                    )
                )
                .setCancelable(true)
                .setPositiveButton("Proceed!") { dialogInterface, i ->
                    Utils.displayMessage(
                        this@RouterManagementActivity,
                        String.format("Rebooting %d Router(s)....", routersList.size), Style.INFO
                    )

                    val currentNum = AtomicInteger(0)
                    val numActionsWithNoSuccess = AtomicInteger(0)
                    val totalNumOfDevices = routersList.size

                    for (selectedRouter in routersList) {
                        ActionManager.runTasks(
                            RebootRouterAction(selectedRouter, this@RouterManagementActivity,
                                object : RouterActionListener {
                                    override fun onRouterActionFailure(
                                        routerAction: RouterAction,
                                        router: Router,
                                        exception: Exception?
                                    ) {
                                        val incrementAndGet = currentNum.incrementAndGet()
                                        numActionsWithNoSuccess.incrementAndGet()
                                        if (incrementAndGet >= totalNumOfDevices) {
                                            //An error occurred
                                            Utils.displayMessage(
                                                this@RouterManagementActivity,
                                                String.format(
                                                    "Action '%s' executed but %d error(s) occurred: %s",
                                                    routerAction.toString(),
                                                    numActionsWithNoSuccess.get(),
                                                    Utils.handleException(
                                                        exception
                                                    ).first
                                                ),
                                                Style.INFO
                                            )
                                        }
                                    }

                                    override fun onRouterActionSuccess(
                                        routerAction: RouterAction,
                                        router: Router, returnData: Any
                                    ) {
                                        val incrementAndGet = currentNum.incrementAndGet()
                                        if (incrementAndGet >= totalNumOfDevices) {
                                            val numActionsThatDidNotSucceed = numActionsWithNoSuccess.get()
                                            if (numActionsThatDidNotSucceed > 0) {
                                                //An error occurred
                                                if (numActionsThatDidNotSucceed < totalNumOfDevices) {
                                                    Utils.displayMessage(
                                                        this@RouterManagementActivity,
                                                        String.format(
                                                            "Action '%s' executed but %d error(s) occurred",
                                                            routerAction.toString(),
                                                            numActionsThatDidNotSucceed
                                                        ),
                                                        Style.INFO
                                                    )
                                                } else {
                                                    //No action succeeded
                                                    Utils.displayMessage(
                                                        this@RouterManagementActivity,
                                                        String.format(
                                                            "None of the '%s' actions submitted succeeded - please try again later.",
                                                            routerAction.toString()
                                                        ),
                                                        Style.INFO
                                                    )
                                                }
                                            } else {
                                                //No error
                                                Utils.displayMessage(
                                                    this@RouterManagementActivity,
                                                    String.format(
                                                        "Action '%s' executed successfully on %d Routers",
                                                        routerAction.toString(),
                                                        routersList.size
                                                    ),
                                                    Style.CONFIRM
                                                )
                                            }
                                        }
                                    }
                                }, mPreferences!!
                            )
                        )
                    }
                }
                .setNegativeButton("Cancel") { _, i ->
                    //Cancelled - nothing more to do!
                }
                .create()
                .show()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onQueryTextChange(s: String): Boolean {
        val adapter = mAdapter as RouterListRecycleViewAdapter?
        if (TextUtils.isEmpty(s)) {
            adapter!!.routersList = dao!!.allRouters
            adapter.notifyDataSetChanged()
        } else {
            adapter!!.filter.filter(s)
        }
        return true
    }

    override fun onQueryTextSubmit(s: String): Boolean {
        return false
    }

    override fun onRefresh() {
        doRefreshRoutersListWithSpinner(DATA_SET_CHANGED, null)
    }

    override fun onRouterAdd(dialog: DialogFragment?, router: Router?, error: Boolean) {
        if (!error) {
            //Always added to the top
            doRefreshRoutersListWithSpinner(INSERTED, 0)
            mLayoutManager!!.scrollToPosition(0)
            //Request Backup
            Utils.requestBackup(this)
        }
    }

    override fun onRouterUpdated(dialog: DialogFragment, position: Int, router: Router, error: Boolean) {
        if (!error) {
            //Refresh everything, as the order actually should remain the same
            //Always added to the top
            //            doRefreshRoutersListWithSpinner(RoutersListRefreshCause.UPDATED, position);
            doRefreshRoutersListWithSpinner(DATA_SET_CHANGED, position)
            mLayoutManager!!.scrollToPosition(position)
            //Request Backup
            Utils.requestBackup(this)
        }
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        mItemTouchHelper!!.startDrag(viewHolder)
    }

    //    /**
    //     * Called when action mode is first created. The menu supplied will be used to
    //     * generate action buttons for the action mode.
    //     *
    //     * @param actionMode ActionMode being created
    //     * @param menu       Menu used to populate action buttons
    //     * @return true if the action mode should be created, false if entering this
    //     * mode should be aborted.
    //     */
    //    @Override
    //    public boolean onCreateActionMode(@NonNull ActionMode actionMode, Menu menu) {
    //        // Inflate a menu resource providing context menu items
    //        if (mToolbar != null) {
    //            mToolbar.setVisibility(View.GONE);
    //        }
    //        final MenuInflater inflater = actionMode.getMenuInflater();
    //        inflater.inflate(R.menu.menu_router_list_selection_menu, menu);
    //
    //        menu.findItem(R.id.menu_router_item_open)
    //                .setVisible(false);
    //        menu.findItem(R.id.menu_router_item_open)
    //                .setEnabled(false);
    //
    //        addNewButton.setVisibility(View.GONE);
    //        return true;
    //    }

    //    /**
    //     * Called to refresh an action mode's action menu whenever it is invalidated.
    //     *
    //     * @param actionMode ActionMode being prepared
    //     * @param menu       Menu used to populate action buttons
    //     * @return true if the menu or action mode was updated, false otherwise.
    //     */
    //    @Override
    //    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
    //        return false;
    //    }

    //    /**
    //     * Called to report a user click on an action button.
    //     *
    //     * @param actionMode The current ActionMode
    //     * @param menuItem   The item that was clicked
    //     * @return true if this callback handled the event, false if the standard MenuItem
    //     * invocation should continue.
    //     */
    //    @Override
    //    public boolean onActionItemClicked(@NonNull final ActionMode actionMode, @NonNull final MenuItem menuItem) {
    //        final RouterListRecycleViewAdapter adapter = (RouterListRecycleViewAdapter) mAdapter;
    //        final List<Router> routersList = adapter.getRoutersList();
    //
    //        switch (menuItem.getItemId()) {
    //            case R.id.action_actions_reboot_routers: {
    //                final List<Integer> selectedItems = adapter.getSelectedItems();
    //
    //                if (selectedItems.isEmpty()) {
    //                    Toast.makeText(this, "No Router selected", Toast.LENGTH_SHORT).show();
    //                    return true;
    //                }
    //
    //                final List<Router> selectedRouters = new ArrayList<>();
    //                final List<String> selectedRoutersStr = new ArrayList<>();
    //                for (Integer selectedItem : selectedItems) {
    //                    if (selectedItem == null || selectedItem < 0 || selectedItem >= routersList.size()) {
    //                        continue;
    //                    }
    //                    final Router selectedRouter;
    //                    if ((selectedRouter = routersList.get(selectedItem)) == null) {
    //                        continue;
    //                    }
    //                    selectedRouters.add(selectedRouter);
    //                    selectedRoutersStr.add(String.format("- '%s' (%s)",
    //                            selectedRouter.getDisplayName(), selectedRouter.getRemoteIpAddress()));
    //                }
    //
    //                new AlertDialog.Builder(this)
    //                        .setIcon(R.drawable.ic_action_alert_warning)
    //                        .setTitle(String.format("Reboot %d Router(s)?", selectedItems.size()))
    //                        .setMessage(String.format("Are you sure you wish to continue? " +
    //                                        "The following Routers will be rebooted: \n\n%s",
    //                                Joiner.on("\n\n").skipNulls().join(selectedRoutersStr)))
    //                        .setCancelable(true)
    //                        .setPositiveButton("Proceed!", new DialogInterface.OnClickListener() {
    //                            @Override
    //                            public void onClick(final DialogInterface dialogInterface, final int i) {
    //
    //                                Utils.displayMessage(RouterManagementActivity.this,
    //                                        String.format("Rebooting %d Router(s)....", selectedItems.size()),
    //                                        Style.INFO);
    //
    //                                final AtomicInteger currentNum = new AtomicInteger(0);
    //                                final AtomicInteger numActionsWithNoSuccess = new AtomicInteger(0);
    //                                final int totalNumOfDevices = selectedRouters.size();
    //
    //                                final RouterActionListener rebootRouterActionListener = new RouterActionListener() {
    //                                    @Override
    //                                    public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router, Object returnData) {
    //                                        final int incrementAndGet = currentNum.incrementAndGet();
    //                                        if (incrementAndGet >= totalNumOfDevices) {
    //                                            final int numActionsThatDidNotSucceed = numActionsWithNoSuccess.get();
    //                                            if (numActionsThatDidNotSucceed > 0) {
    //                                                //An error occurred
    //                                                if (numActionsThatDidNotSucceed < totalNumOfDevices) {
    //                                                    Utils.displayMessage(RouterManagementActivity.this,
    //                                                            String.format("Action '%s' executed but %d error(s) occurred",
    //                                                                    routerAction.toString(), numActionsThatDidNotSucceed),
    //                                                            Style.INFO);
    //                                                } else {
    //                                                    //No action succeeded
    //                                                    Utils.displayMessage(RouterManagementActivity.this,
    //                                                            String.format("None of the '%s' actions submitted succeeded - please try again later.",
    //                                                                    routerAction.toString()),
    //                                                            Style.INFO);
    //                                                }
    //
    //                                            } else {
    //                                                //No error
    //                                                Utils.displayMessage(RouterManagementActivity.this,
    //                                                        String.format("Action '%s' executed successfully on %d Routers",
    //                                                                routerAction.toString(), selectedItems.size()),
    //                                                        Style.CONFIRM);
    //                                            }
    //                                        }
    //                                    }
    //
    //                                    @Override
    //                                    public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull Router router, @Nullable Exception exception) {
    //                                        final int incrementAndGet = currentNum.incrementAndGet();
    //                                        numActionsWithNoSuccess.incrementAndGet();
    //                                        if (incrementAndGet >= totalNumOfDevices) {
    //                                            //An error occurred
    //                                            Utils.displayMessage(RouterManagementActivity.this,
    //                                                    String.format("Action '%s' executed but %d error(s) occurred: %s",
    //                                                            routerAction.toString(), numActionsWithNoSuccess.get(),
    //                                                            ExceptionUtils.getRootCauseMessage(exception)),
    //                                                    Style.INFO);
    //                                        }
    //                                    }
    //                                };
    //
    //                                for (final Router selectedRouter : selectedRouters) {
    //                                    new RebootRouterAction(RouterManagementActivity.this,
    //                                            rebootRouterActionListener,
    //                                            mPreferences).execute(selectedRouter);
    //                                }
    //                            }
    //                        })
    //                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
    //                            @Override
    //                            public void onClick(DialogInterface dialogInterface, int i) {
    //                                //Cancelled - nothing more to do!
    //                            }
    //                        }).create().show();
    //            }
    //                return true;
    //            case R.id.action_actions_restore_factory_defaults: {
    //                //TODO Hidden for now
    //
    //            }
    //                return true;
    //            case R.id.action_actions_firmwares_upgrade:
    //                //TODO Hidden for now
    //                return true;
    //            case R.id.menu_router_list_delete:
    //                final int selectedItemCount = adapter.getSelectedItemCount();
    //                new AlertDialog.Builder(this)
    //                        .setIcon(R.drawable.ic_action_alert_warning)
    //                        .setTitle(String.format("Delete %d Router(s)?", selectedItemCount))
    //                        .setMessage("You'll lose those records!")
    //                        .setCancelable(true)
    //                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
    //                            @Override
    //                            public void onClick(final DialogInterface dialogInterface, final int i) {
    //                                final List<Integer> selectedItemPositions = adapter.getSelectedItems();
    //                                int numberOfItems = -1;
    //                                for (int itemPosition = selectedItemPositions.size() - 1; itemPosition >= 0; itemPosition--) {
    //                                    numberOfItems = adapter.removeData(selectedItemPositions.get(itemPosition));
    //                                }
    //                                actionMode.finish();
    //                                if (numberOfItems == 0) {
    //                                    //All items dropped = open up 'Add Router' diaalog
    //                                    RouterManagementActivity.this.openAddRouterForm();
    //                                }
    //
    //                                Crouton.makeText(RouterManagementActivity.this, selectedItemCount + " item(s) deleted", Style.CONFIRM).show();
    //                                //Request Backup
    //                                Utils.requestBackup(RouterManagementActivity.this);
    //                            }
    //                        })
    //                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
    //                            @Override
    //                            public void onClick(DialogInterface dialogInterface, int i) {
    //                                //Cancelled - nothing more to do!
    //                            }
    //                        }).create().show();
    //
    //                return true;
    //            case R.id.menu_router_item_edit: {
    //                final Integer itemPos = adapter.getSelectedItems().get(0);
    //                this.openUpdateRouterForm(
    //                        (itemPos == null || itemPos < 0 || itemPos >= routersList.size()) ? null : routersList.get(itemPos)
    //                );
    //            }
    //            return true;
    //            case R.id.menu_router_item_copy: {
    //                final Integer itemPos = adapter.getSelectedItems().get(0);
    //                this.openDuplicateRouterForm(
    //                        (itemPos == null || itemPos < 0 || itemPos >= routersList.size()) ? null : routersList.get(itemPos)
    //                );
    //            }
    //            return true;
    //            default:
    //                return false;
    //        }
    //    }

    //    /**
    //     * Called when an action mode is about to be exited and destroyed.
    //     *
    //     * @param actionMode The current ActionMode being destroyed
    //     */
    //    @Override
    //    public void onDestroyActionMode(ActionMode actionMode) {
    //        this.actionMode = null;
    //        //Reset background for selected items
    //        for (int i = 0; i < this.dao.getAllRouters().size(); i++) {
    //            final View childAt;
    //            if ((childAt = mRecyclerView.getLayoutManager().getChildAt(i)) == null) {
    //                continue;
    //            }
    //            childAt.setBackgroundResource(android.R.color.transparent);
    //        }
    //        ((RouterListRecycleViewAdapter) mAdapter).clearSelections();
    //        addNewButton.setVisibility(View.VISIBLE);
    //        if (mToolbar != null) {
    //            mToolbar.setVisibility(View.VISIBLE);
    //        }
    //    }
    //
    //    @Override
    //    public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
    //        gestureDetector.onTouchEvent(motionEvent);
    //        return false;
    //    }

    //    @Override
    //    public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
    //
    //    }
    //
    //    @Override
    //    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    //
    //    }

    //    private void myToggleSelection(int idx) {
    //        if (actionMode == null) {
    //            return;
    //        }
    //        final RouterListRecycleViewAdapter adapter = (RouterListRecycleViewAdapter) mAdapter;
    //        adapter.toggleSelection(idx);
    //        final int selectedItemCount = adapter.getSelectedItemCount();
    //
    //        String title = getString(R.string.selected_count, selectedItemCount);
    //        actionMode.setTitle(title);
    //
    //        //Show 'Edit' button only if one item is selected
    //        final MenuItem editButton = actionMode.getMenu().getItem(0);
    //        final boolean menuItemRelevantForOneItemSelectedOnly = (selectedItemCount == 1);
    //        if (editButton != null) {
    //            editButton.setVisible(menuItemRelevantForOneItemSelectedOnly);
    //        }
    //        final MenuItem copyButton = actionMode.getMenu().getItem(1);
    //        if (copyButton != null) {
    //            copyButton.setVisible(menuItemRelevantForOneItemSelectedOnly);
    //        }
    //        final MenuItem deleteButton = actionMode.getMenu().getItem(2);
    //        if (deleteButton != null) {
    //            deleteButton.setVisible(selectedItemCount > 0);
    //        }
    //    }

    private fun setRefreshActionButtonState(refreshing: Boolean) {
        mSwipeRefreshLayout!!.isRefreshing = refreshing
        if (optionsMenu != null) {
            val refreshItem = optionsMenu!!.findItem(R.id.router_list_refresh)
            if (refreshItem != null) {
                if (refreshing) {
                    refreshItem.setActionView(R.layout.actionbar_indeterminate_progress)
                    //                    addNewButton.setVisibility(View.GONE);
                } else {
                    refreshItem.actionView = null
                    //                    addNewButton.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Utils.getBaseContextToAttach(this, newBase))
    }

    private fun doRefreshRoutersListWithSpinner(
        cause: RoutersListRefreshCause,
        position: Int?
    ) {
        //        mSwipeRefreshLayout.setEnabled(false);
        setRefreshActionButtonState(true)
        Handler().postDelayed({
            try {
                val allRouters = this@RouterManagementActivity.dao!!.allRouters
                (this@RouterManagementActivity.mAdapter as RouterListRecycleViewAdapter).routersList = allRouters
                when (cause) {
                    DATA_SET_CHANGED -> this@RouterManagementActivity.mAdapter!!.notifyDataSetChanged()
                    INSERTED -> {
                        //Rebuild list so as to add the new router on top
                        //We assume the item with the highest id is the latest added,
                        // and, as such, should be on top of the list
                        val routerList = ArrayList(allRouters)
                        routerList.sortWith(Comparator { o1, o2 -> o2.id - o1.id })
                        for (i in routerList.indices) {
                            val router = routerList[i]
                            val previousOrderIdx = router.orderIndex
                            if (i == position) {
                                router.orderIndex = i
                            } else {
                                router.orderIndex = previousOrderIdx + 1
                            }
                            val newOrderIndex = router.orderIndex
                            Crashlytics.log(
                                Log.DEBUG, LOG_TAG, "XXX Router '"
                                        + router.canonicalHumanReadableName
                                        + "' "
                                        + "new position: "
                                        + previousOrderIdx
                                        + " => "
                                        + newOrderIndex
                            )

                            dao!!.updateRouter(router)
                        }

                        this@RouterManagementActivity.mAdapter!!.notifyItemInserted(position!!)
                        mRecyclerView!!.scrollToPosition(position)
                    }
                    REMOVED -> this@RouterManagementActivity.mAdapter!!.notifyItemRemoved(
                        position!!
                    )
                    UPDATED -> this@RouterManagementActivity.mAdapter!!.notifyItemChanged(
                        position!!
                    )
                }

                setDynamicAppShortcuts()
            } finally {
                setRefreshActionButtonState(false)
                //                    mSwipeRefreshLayout.setEnabled(true);
            }
        }, 1000)
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            val adapter = mAdapter as RouterListRecycleViewAdapter?
            val query = intent.getStringExtra(SearchManager.QUERY)
            if (query == null) {
                adapter!!.routersList = dao!!.allRouters
                adapter.notifyDataSetChanged()
                return
            }
            adapter!!.filter.filter(query)
        }
    }

    private fun initOpenAddRouterFormIfNecessary() {
        if (mAdapter!!.itemCount == 0) {
            this.openAddRouterForm()
        }
    }

    private fun onRouterAdd() {
        onRouterAdd(null, null, false)
        if (this.mCloseOnActionDone) {
            val allRouters = this@RouterManagementActivity.dao!!.allRouters
            if (allRouters.isNotEmpty()) {
                val newRouter = allRouters[0]
                val data = Intent()
                data.putExtra(ROUTER_SELECTED, newRouter.uuid)
                setResult(Activity.RESULT_OK, data)
            } else {
                setResult(Activity.RESULT_CANCELED)
            }

            finish()
        }
    }

    private fun openAddRouterForm() {
        //        final Fragment addRouter = getSupportFragmentManager().findFragmentByTag(ADD_ROUTER_FRAGMENT_TAG);
        //        if (addRouter instanceof DialogFragment) {
        //            ((DialogFragment) addRouter).dismiss();
        //        }

        //Display Donate Message if trying to add more than the max routers for Free version
        val allRouters = dao!!.allRouters
        //noinspection PointlessBooleanExpression,ConstantConditions
        if ((BuildConfig.DONATIONS || BuildConfig.WITH_ADS) && allRouters.size >= MAX_ROUTERS_FREE_VERSION) {
            //Download the full version to unlock this version
            Utils.displayUpgradeMessage(this, "Manage a new Router")
            return
        }

        //        final DialogFragment addFragment = new RouterAddDialogFragment();
        //        addFragment.show(getSupportFragmentManager(), ADD_ROUTER_FRAGMENT_TAG);
        startActivityForResult(Intent(this, ManageRouterFragmentActivity::class.java), NEW_ROUTER_ADDED)
    }

    private fun setDynamicAppShortcuts() {
        if (VERSION.SDK_INT >= VERSION_CODES.N_MR1) {
            val shortcutManager = getSystemService(ShortcutManager::class.java)
            val allRouters = dao!!.allRouters
            if (allRouters.isNotEmpty()) {
                val nbRoutersOnFile = allRouters.size

                val maxRoutersEligibleForAppShortcuts = ArrayList<Router>(4)
                val nonDemoRoutersEligibleForAppShortcuts = ArrayList<Router>(4)
                for (i in 0..3) {
                    //We have a limit of 5 app shortcuts (dynamic and static combined),
                    // and a static one is already added. So keep the 4 most recent only
                    if (i >= nbRoutersOnFile) {
                        break
                    }
                    val router = allRouters[i]
                    maxRoutersEligibleForAppShortcuts.add(router)
                    if (!Utils.isDemoRouter(router)) {
                        nonDemoRoutersEligibleForAppShortcuts.add(router)
                    }
                }

                val shortcutInfoList = ArrayList<ShortcutInfo>(4)
                for (router in maxRoutersEligibleForAppShortcuts) {
                    val routerUuid = router.uuid
                    val demoRouter = Utils.isDemoRouter(router)
                    val routerName = router.name
                    val routerCanonicalHumanReadableName = router.canonicalHumanReadableName

                    val shortcutIntent = Intent(this, DDWRTMainActivity::class.java)
                    shortcutIntent.action = Intent.ACTION_VIEW
                    shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    shortcutIntent.putExtra(ROUTER_SELECTED, routerUuid)

                    val shortcut = ShortcutInfo.Builder(this, routerUuid).setShortLabel(
                        if (routerName?.isBlank() == true)
                            router.remoteIpAddress
                                    + ":"
                                    + router.remotePort
                        else
                            routerName?:""
                    )
                        .setLongLabel(routerCanonicalHumanReadableName)
                        .setIcon(
                            Icon.createWithResource(
                                this,
                                if (demoRouter) R.drawable.demo_router else R.drawable.router
                            )
                        )
                        .setIntent(shortcutIntent)
                        .build()

                    shortcutInfoList.add(shortcut)
                }
                shortcutManager!!.dynamicShortcuts = shortcutInfoList

                //Trigger download of avatars (for non-demo routers)
                for (router in nonDemoRoutersEligibleForAppShortcuts) {
                    //Leverage Picasso to fetch router icon, if available
                    try {

                        ImageUtils.downloadImageFromUrl(
                            this,
                            Router.getRouterAvatarUrl(
                                this,
                                router,
                                Router.mAvatarDownloadOpts
                            ),
                            RouterAvatarDownloadTargetForAppShortcut(this, router, true), null, null, null
                        )
                    } catch (e: Exception) {
                        //No worries
                        Utils.reportException(this, e)
                    }

                }
            }
        }
    }

    private fun setupCustomTabHelper(cb: CustomTabActivityHelper.ConnectionCallback) {
        mCustomTabActivityHelper = CustomTabActivityHelper()
        mCustomTabActivityHelper!!.setConnectionCallback(cb)
        mCustomTabActivityHelper!!.mayLaunchUrl(
            Uri.parse(RouterCompanionAppConstants.REMOTE_HELP_WEBSITE), null, null
        )
        mCustomTabActivityHelper!!.mayLaunchUrl(
            Uri.parse(RouterCompanionAppConstants.REMOTE_HELP_WEBSITE_CHANGELOG), null, null
        )
    }

    companion object {

        const val ROUTER_MANAGEMENT_SETTINGS_ACTIVITY_CODE = 111

        const val ROUTER_SELECTED = "ROUTER_SELECTED"

        const val ADD_ROUTER_FRAGMENT_TAG = "add_router"

        const val UPDATE_ROUTER_FRAGMENT_TAG = "update_router"

        const val COPY_ROUTER = "copy_router"

        const val ACTION_OPEN_ADD_ROUTER_WIZARD = "org.rm3l.ddwrt.OPEN_ADD_ROUTER_WIZARD"

        const val CLOSE_ON_ACTION_DONE = "CLOSE_ON_ACTION_DONE"

        const val NEW_ROUTER_ADDED = 987

        const val ROUTER_UPDATED = 876

        private val LOG_TAG = RouterManagementActivity::class.java.simpleName

        @JvmStatic
        fun getDao(context: Context): DDWRTCompanionDAO {
            if (!DDWRTCompanionSqliteDAOImpl.isInitialized()) {
                DDWRTCompanionSqliteDAOImpl.initialize(context)
            }
            return DDWRTCompanionSqliteDAOImpl.getInstance()
        }

        fun startActivity(activity: Activity, view: View?, ddWrtMainIntent: Intent) {
            val transitionName = activity.getString(R.string.transition_router)
            val options = if (view != null)
                ActivityOptionsCompat.makeSceneTransitionAnimation(
                    activity, view,
                    transitionName
                )
            else
                null

            ActivityCompat.startActivity(
                activity, ddWrtMainIntent,
                options?.toBundle()
            )
        }
    }

    //    private class RouterManagementViewOnGestureListener extends GestureDetector.SimpleOnGestureListener {
    //        @Override
    //        public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
    //            final View view = mRecyclerView.findChildViewUnder(e.getX(), e.getY());
    //            onClick(view);
    //            return super.onSingleTapConfirmed(e);
    //        }
    //
    //        public void onLongPress(@NonNull MotionEvent e) {
    //            if (actionMode != null) {
    //                return;
    //            }
    //            final View view = mRecyclerView.findChildViewUnder(e.getX(), e.getY());
    //            //Item long-pressed: set background
    //            if (view == null) {
    //                return;
    //            }
    //            //Selection: apply background
    //            if (ColorUtils.isThemeLight(RouterManagementActivity.this)) {
    //                view.setBackgroundResource(R.color.DarkOrange);
    //            } else {
    //                view.setBackgroundResource(R.color.yellow);
    //            }
    //            // Start the CAB using the ActionMode.Callback defined above
    //            actionMode = startActionMode(RouterManagementActivity.this);
    //            int idx = mRecyclerView.getChildPosition(view);
    //            myToggleSelection(idx);
    //            super.onLongPress(e);
    //        }
    //    }
}