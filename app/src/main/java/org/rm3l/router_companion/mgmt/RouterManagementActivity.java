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

package org.rm3l.router_companion.mgmt;

import static org.rm3l.router_companion.RouterCompanionAppConstants.ACRA_ENABLE;
import static org.rm3l.router_companion.RouterCompanionAppConstants.DEBUG_MODE;
import static org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.router_companion.RouterCompanionAppConstants.MAX_ROUTERS_FREE_VERSION;
import static org.rm3l.router_companion.RouterCompanionAppConstants.NOTIFICATIONS_BG_SERVICE_ENABLE;
import static org.rm3l.router_companion.RouterCompanionAppConstants.NOTIFICATIONS_SYNC_INTERVAL_MINUTES_PREF;
import static org.rm3l.router_companion.RouterCompanionAppConstants.THEMING_PREF;
import static org.rm3l.router_companion.RouterCompanionApplication.DEBUG_LEAKCANARY_PREF_KEY;
import static org.rm3l.router_companion.RouterCompanionApplication.DEBUG_RESOURCE_INSPECTOR_PREF_KEY;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import co.paulburke.android.itemtouchhelperdemo.helper.ItemTouchHelperAdapter;
import co.paulburke.android.itemtouchhelperdemo.helper.OnStartDragListener;
import com.airbnb.deeplinkdispatch.DeepLink;
import com.crashlytics.android.Crashlytics;
import com.evernote.android.job.JobManager;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.common.base.Joiner;
import com.readystatesoftware.chuck.Chuck;
import com.stephentuso.welcome.WelcomeScreenHelper;
import com.stephentuso.welcome.ui.WelcomeActivity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.actions.ActionManager;
import org.rm3l.router_companion.actions.RebootRouterAction;
import org.rm3l.router_companion.actions.RouterAction;
import org.rm3l.router_companion.actions.RouterActionListener;
import org.rm3l.router_companion.help.ChangelogActivity;
import org.rm3l.router_companion.help.HelpActivity;
import org.rm3l.router_companion.job.RouterCompanionJobCreator;
import org.rm3l.router_companion.job.speedtest.RouterSpeedTestAutoRunnerJob;
import org.rm3l.router_companion.main.DDWRTMainActivity;
import org.rm3l.router_companion.mgmt.adapters.RouterListItemTouchHelperCallback;
import org.rm3l.router_companion.mgmt.adapters.RouterListRecycleViewAdapter;
import org.rm3l.router_companion.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.router_companion.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteDAOImpl;
import org.rm3l.router_companion.mgmt.register.ManageRouterFragmentActivity;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.settings.RouterManagementSettingsActivity;
import org.rm3l.router_companion.utils.AdUtils;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.ImageUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.customtabs.CustomTabActivityHelper;
import org.rm3l.router_companion.utils.kotlin.ContextUtils;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style;
import org.rm3l.router_companion.welcome.GettingStartedActivity;
import org.rm3l.router_companion.widgets.RecyclerViewEmptySupport;

//import com.sloydev.preferator.Preferator;

//import com.madx.updatechecker.lib.UpdateRunnable;
//import com.pusher.client.Pusher;
//import com.pusher.client.channel.SubscriptionEventListener;

@DeepLink({
        "dd-wrt://management", "ddwrt://management"
})
public class RouterManagementActivity extends AppCompatActivity
        implements View.OnClickListener, RouterMgmtDialogListener, SearchView.OnQueryTextListener,
        SwipeRefreshLayout.OnRefreshListener, CustomTabActivityHelper.ConnectionCallback,
        OnStartDragListener {

    public enum RoutersListRefreshCause {
        INSERTED, REMOVED, DATA_SET_CHANGED, UPDATED
    }

    public static final int ROUTER_MANAGEMENT_SETTINGS_ACTIVITY_CODE = 111;

    public static final String ROUTER_SELECTED = "ROUTER_SELECTED";

    public static final String ADD_ROUTER_FRAGMENT_TAG = "add_router";

    public static final String UPDATE_ROUTER_FRAGMENT_TAG = "update_router";

    public static final String COPY_ROUTER = "copy_router";

    public static final String ACTION_OPEN_ADD_ROUTER_WIZARD =
            "org.rm3l.ddwrt.OPEN_ADD_ROUTER_WIZARD";

    public static final String CLOSE_ON_ACTION_DONE = "CLOSE_ON_ACTION_DONE";

    public static final int NEW_ROUTER_ADDED = 987;

    public static final int ROUTER_UPDATED = 876;

    public static final int RoutersListRefreshCause_INSERTED = 1;

    public static final int RoutersListRefreshCause_REMOVED = 2;

    public static final int RoutersListRefreshCause_DATA_SET_CHANGED = 3;

    public static final int RoutersListRefreshCause_UPDATED = 4;

    private static final String LOG_TAG = RouterManagementActivity.class.getSimpleName();

    private FloatingActionButton addNewButton;

    private DDWRTCompanionDAO dao;

    private RecyclerView.Adapter mAdapter;

    private boolean mAutoCrashReports;

    private boolean mBackgroundServiceEnabled;

    private long mBackgroundServiceFrequency;

    private boolean mCloseOnActionDone;

    private long mCurrentTheme;

    private CustomTabActivityHelper mCustomTabActivityHelper;

    //    private Pusher mPusher;
    @Nullable
    private InterstitialAd mInterstitialAd;

    private ItemTouchHelper mItemTouchHelper;

    private RecyclerView.LayoutManager mLayoutManager;

    private SharedPreferences mPreferences;

    private RecyclerViewEmptySupport mRecyclerView;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private Toolbar mToolbar;

    private Menu optionsMenu;

    private WelcomeScreenHelper welcomeScreen;

    @NonNull
    public static DDWRTCompanionDAO getDao(Context context) {
        if (!DDWRTCompanionSqliteDAOImpl.isInitialized()) {
            DDWRTCompanionSqliteDAOImpl.initialize(context);
        }
        return DDWRTCompanionSqliteDAOImpl.getInstance();
    }

    public static void startActivity(Activity activity, View view, Intent ddWrtMainIntent) {
        final String transitionName = activity.getString(R.string.transition_router);
        final ActivityOptionsCompat options =
                (view != null ? ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view,
                        transitionName) : null);

        ActivityCompat.startActivity(activity, ddWrtMainIntent,
                options != null ? options.toBundle() : null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Dynamic shortcuts are not preserved during backup/restore
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            final ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
            // Application restored. Need to re-publish dynamic shortcuts.
            final List<ShortcutInfo> pinnedShortcuts = shortcutManager.getPinnedShortcuts();
            if (!pinnedShortcuts.isEmpty()) {
                // Pinned shortcuts have been restored. Use
                // updateShortcuts(List) to make sure they
                // contain up-to-date information.
                final List<ShortcutInfo> dynamicPinnedShortcutsToUpdate = new ArrayList<>();
                for (final ShortcutInfo pinnedShortcut : pinnedShortcuts) {
                    if (pinnedShortcut == null) {
                        continue;
                    }
                    if (pinnedShortcut.isDynamic()) {
                        dynamicPinnedShortcutsToUpdate.add(pinnedShortcut);
                    }
                }
                shortcutManager.updateShortcuts(dynamicPinnedShortcutsToUpdate);
            }
        }

        final Intent intent = getIntent();
        handleIntent(intent);

        //Default values are not set by default
        //Android bug workaround: http://code.google.com/p/android/issues/detail?id=6641
        PreferenceManager.setDefaultValues(this, DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE,
                R.xml.router_management_settings, false);

        mPreferences = getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        mCurrentTheme = mPreferences.getLong(THEMING_PREF, RouterCompanionAppConstants.DEFAULT_THEME);
        mAutoCrashReports = mPreferences.getBoolean(ACRA_ENABLE, true);

        ColorUtils.Companion.setAppTheme(this, null, false);

        //        if (ColorUtils.isThemeLight(this)) {
        //            //Light
        //            setTheme(R.style.AppThemeLight);
        ////            getWindow().getDecorView()
        ////                    .setBackgroundColor(ContextCompat.getColor(this,
        ////                            R.color.GhostWhite));
        //        } else {
        //            //Default is Dark
        //            setTheme(R.style.AppThemeDark);
        //        }

        mBackgroundServiceEnabled = mPreferences.getBoolean(NOTIFICATIONS_BG_SERVICE_ENABLE, false);
        mBackgroundServiceFrequency =
                mPreferences.getLong(NOTIFICATIONS_SYNC_INTERVAL_MINUTES_PREF, -1);

        setContentView(R.layout.activity_router_management);

        //FIXME Just a simple test
        //        final Map<String, SubscriptionEventListener> pushEventListeners = new HashMap<>();
        //        pushEventListeners.put("new_update", new SubscriptionEventListener() {
        //            @Override
        //            public void onEvent(final String channelName, final String eventName, final String data) {
        //                runOnUiThread(new Runnable() {
        //                    @Override
        //                    public void run() {
        //                        Toast.makeText(RouterManagementActivity.this,
        //                                "[TEST] New DD-WRT Update: " + channelName + ", " + eventName + ", " + data,
        //                                Toast.LENGTH_SHORT).show();
        //                    }
        //                });
        //            }
        //        });
        //        this.mPusher = PushUtils.getPusher("ddwrt_updates", pushEventListeners, false);

        setupCustomTabHelper(this);

        AdUtils.buildAndDisplayAdViewIfNeeded(this, (AdView) findViewById(R.id.router_list_adView));

        mInterstitialAd = AdUtils.requestNewInterstitial(this,
                R.string.interstitial_ad_unit_id_router_list_to_router_main);

        mToolbar = (Toolbar) findViewById(R.id.routerManagementActivityToolbar);
        if (mToolbar != null) {
            mToolbar.setTitle("Routers");
            mToolbar.setTitleTextAppearance(getApplicationContext(), R.style.ToolbarTitle);
            mToolbar.setSubtitleTextAppearance(getApplicationContext(), R.style.ToolbarSubtitle);
            mToolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
            mToolbar.setSubtitleTextColor(ContextCompat.getColor(this, R.color.white));
            setSupportActionBar(mToolbar);
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setIcon(R.mipmap.ic_launcher_ddwrt_companion);
        }

        this.dao = getDao(this);

        final List<Router> allRouters = this.dao.getAllRouters();
//    for (final Router router : allRouters) {
//      if (router == null) {
//        continue;
//      }
//      NotificationHelperKt.createNotificationChannelGroup(router, this);
//    }

        mRecyclerView = findViewById(R.id.routersListView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        // allows for optimizations if all items are of the same size:
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.scrollToPosition(0);
        mRecyclerView.setLayoutManager(mLayoutManager);

        final TextView emptyView = (TextView) findViewById(R.id.empty_view);
        if (ColorUtils.Companion.isThemeLight(this)) {
            emptyView.setTextColor(ContextCompat.getColor(this, R.color.black));
        } else {
            emptyView.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
        mRecyclerView.setEmptyView(emptyView);

        // specify an adapter (see also next example)
        mAdapter = new RouterListRecycleViewAdapter(this, allRouters);
        mRecyclerView.setAdapter(mAdapter);

        final ItemTouchHelper.Callback callback =
                new RouterListItemTouchHelperCallback(mRecyclerView, (ItemTouchHelperAdapter) mAdapter);
        this.mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

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

        addNewButton = (FloatingActionButton) findViewById(R.id.router_list_add);

        //Attach to recyclerview for scrolling effect
        //        addNewButton.attachToRecyclerView(mRecyclerView);

        addNewButton.setOnClickListener(this);

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

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light, android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

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

        Utils.requestAppPermissions(this);

        //No need to restart the background service each time - this is correctly handled by Android
        //        BootReceiver.doStartBackgroundServiceIfNeeded(this);

        /* Use this when you want to run a background update check */
        //        final UpdateRunnable updateRunnable =
        //                new UpdateRunnable(this, new Handler());
        //        if (StringUtils.startsWithIgnoreCase(FLAVOR, "google")) {
        //            //This library currently supports Google Play only
        //            updateRunnable.start();
        //        }

        Utils.displayRatingBarIfNeeded(this);

        welcomeScreen = new WelcomeScreenHelper(this, GettingStartedActivity.class);

        if (ACTION_OPEN_ADD_ROUTER_WIZARD.equals(intent.getAction())) {
            mCloseOnActionDone = intent.getBooleanExtra(CLOSE_ON_ACTION_DONE, false);
            this.openAddRouterForm();
        } else {
            welcomeScreen.show(savedInstanceState);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //        if (this.mPusher != null) {
        //            this.mPusher.connect();
        //        }
        mCustomTabActivityHelper.bindCustomTabsService(this);

        //#199: app shortcuts
        setDynamicAppShortcuts();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mPreferences.getBoolean(DEBUG_MODE, false)) {
            Crashlytics.log(Log.DEBUG, LOG_TAG,
                    "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        }
        // Check which request we're responding to
        switch (requestCode) {
            case ROUTER_MANAGEMENT_SETTINGS_ACTIVITY_CODE: {
                // Make sure the request was successful and reload U if necessary
                if (resultCode == RESULT_OK) {
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
                    final String acraEmailAddr =
                            this.mPreferences.getString(RouterCompanionAppConstants.ACRA_USER_EMAIL, null);
                    Crashlytics.setUserEmail(acraEmailAddr);

                    final boolean currentUserChoiceForAutoCrashReporting = this.mPreferences.getBoolean(ACRA_ENABLE, true);
                    if (this.mAutoCrashReports != currentUserChoiceForAutoCrashReporting) {
                        //Restart activity
                        Crashlytics.log(Log.DEBUG, LOG_TAG,
                                "<mAutoCrashReports,currentUserChoiceForAutoCrashReporting>=<" +
                                        mAutoCrashReports + "," + currentUserChoiceForAutoCrashReporting + ">");
                        final String waitMessage = String.format("%sabling automatic crash reporting",
                                currentUserChoiceForAutoCrashReporting ? "En" : "Dis");
                        ContextUtils.restartWholeApplication(this, waitMessage, null);
                    } else if (this.mCurrentTheme != this.mPreferences.getLong(THEMING_PREF, -1l)
                            || this.mBackgroundServiceEnabled != this.mPreferences.getBoolean(
                            NOTIFICATIONS_BG_SERVICE_ENABLE, false)
                            || this.mBackgroundServiceFrequency != this.mPreferences.getLong(
                            NOTIFICATIONS_SYNC_INTERVAL_MINUTES_PREF, -1l)) {
                        //Reload UI
                        ContextUtils.finishAndReload(this, "Reloading UI", null, null);
                    }
                }
            }
            break;
            case NEW_ROUTER_ADDED: {
                // Make sure the request was successful and reload U if necessary
                if (resultCode == RESULT_OK) {
                    onRouterAdd();
                }
            }
            break;
            case ROUTER_UPDATED: {
                if (resultCode == RESULT_OK) {
                    doRefreshRoutersListWithSpinner(RoutersListRefreshCause.DATA_SET_CHANGED, null);
                }
            }
            break;
            case WelcomeScreenHelper.DEFAULT_WELCOME_SCREEN_REQUEST:
                //TODO
                final String welcomeKey = \"fake-key\";
                if (resultCode == RESULT_OK) {
                    // Code here will run if the welcome screen was completed
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(getApplicationContext(), "[DEBUG] " + welcomeKey + " completed",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(getApplicationContext(), "[DEBUG] " + welcomeKey + " canceled",
                                Toast.LENGTH_SHORT).show();
                    }
                }
                initOpenAddRouterFormIfNecessary();
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCustomTabActivityHelper.unbindCustomTabsService(this);
        //        if (this.mPusher != null) {
        //            this.mPusher.disconnect();
        //        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11.
        welcomeScreen.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        //Dismiss existing dialog fragments, if any
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(ADD_ROUTER_FRAGMENT_TAG);
        if (fragment instanceof DialogFragment) {
            ((DialogFragment) fragment).dismiss();
        }

        fragment = getSupportFragmentManager().findFragmentByTag(UPDATE_ROUTER_FRAGMENT_TAG);
        if (fragment instanceof DialogFragment) {
            ((DialogFragment) fragment).dismiss();
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (BuildConfig.WITH_ADS && mInterstitialAd != null && AdUtils.canDisplayInterstialAd(this)) {

            mInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    RouterManagementActivity.super.onBackPressed();
                }

                @Override
                public void onAdOpened() {
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
                RouterManagementActivity.super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(@Nullable final View view) {
        if (view == null) {
            return;
        }

        if (view.getId() == R.id.router_list_add) {
            this.openAddRouterForm();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.optionsMenu = menu;
        getMenuInflater().inflate(R.menu.menu_router_management, menu);

        final MenuItem debugOnlyTools = menu.findItem(R.id.debug_only);
        debugOnlyTools.setEnabled(BuildConfig.DEBUG);
        debugOnlyTools.setVisible(BuildConfig.DEBUG);

        final boolean debugLeakCanary = mPreferences.getBoolean(DEBUG_LEAKCANARY_PREF_KEY, false);
        Crashlytics.log(Log.DEBUG, LOG_TAG, "XXX debug_leakcanary: " + debugLeakCanary);
        menu.findItem(R.id.debug_leakcanary).setChecked(debugLeakCanary);

        final boolean debugResourceInspector = mPreferences.getBoolean(
                DEBUG_RESOURCE_INSPECTOR_PREF_KEY, false);
        Crashlytics.log(Log.DEBUG, LOG_TAG, "XXX debug_resourceInspector: " + debugResourceInspector);
        menu.findItem(R.id.debug_resourceinspector).setChecked(debugResourceInspector);

        final MenuItem donateMenuItem = menu.findItem(R.id.router_list_donate);
        if (donateMenuItem != null) {
            donateMenuItem.setVisible(BuildConfig.DONATIONS);
        }

        final MenuItem removeAdsMenuItem = menu.findItem(R.id.router_list_remove_ads);
        if (removeAdsMenuItem != null) {
            removeAdsMenuItem.setVisible(BuildConfig.WITH_ADS);
        }

        //Search
        final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        final SearchView searchView =
                (SearchView) menu.findItem(R.id.router_list_refresh_search).getActionView();

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnQueryTextListener(this);

        // Get the search close button image view
        final ImageView closeButton = (ImageView) searchView.findViewById(R.id.search_close_btn);
        if (closeButton != null) {
            // Set on click listener
            closeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Reset views
                    final RouterListRecycleViewAdapter adapter = (RouterListRecycleViewAdapter) mAdapter;
                    adapter.setRoutersList(dao.getAllRouters());
                    adapter.notifyDataSetChanged();
                    //Hide it now
                    searchView.setIconified(true);
                }
            });
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onCustomTabsConnected() {
        //We may make UI changes
        Crashlytics.log(Log.DEBUG, LOG_TAG, "onCustomTabsConnected");
    }

    @Override
    public void onCustomTabsDisconnected() {
        //We may make UI changes
        Crashlytics.log(Log.DEBUG, LOG_TAG, "onCustomTabsDisconnected");
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        final RouterListRecycleViewAdapter adapter = (RouterListRecycleViewAdapter) mAdapter;
        final List<Router> routersList = adapter.getRoutersList();

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.exit:
                finish();
                return true;

            case R.id.help:
                CustomTabActivityHelper.openCustomTab(RouterManagementActivity.this, null,
                        RouterCompanionAppConstants.REMOTE_HELP_WEBSITE, null, null,
                        new CustomTabActivityHelper.CustomTabFallback() {
                            @Override
                            public void openUri(Activity activity, Uri uri) {
                                activity.startActivity(
                                        new Intent(RouterManagementActivity.this, HelpActivity.class));
                            }
                        }, false);
                return true;
            case R.id.changelog:
                CustomTabActivityHelper.openCustomTab(RouterManagementActivity.this, null,
                        RouterCompanionAppConstants.REMOTE_HELP_WEBSITE_CHANGELOG, null, null,
                        new CustomTabActivityHelper.CustomTabFallback() {
                            @Override
                            public void openUri(Activity activity, Uri uri) {
                                activity.startActivity(
                                        new Intent(RouterManagementActivity.this, ChangelogActivity.class));
                            }
                        }, false);
                return true;

            case R.id.debug_open_sharedprefs:
                if (BuildConfig.DEBUG) {
                    Toast.makeText(this, "[Chuck] Not implemented", Toast.LENGTH_SHORT).show();
                    //Preferator.launch(this);
                } else {
                    Crashlytics.log(Log.WARN, LOG_TAG,
                            "[DEBUG] SharedPreferences menu option should not be visible...");
                }
                return true;

            case R.id.debug_open_chuck:
                if (BuildConfig.DEBUG) {
                    startActivity(Chuck.getLaunchIntent(this));
                } else {
                    Crashlytics.log(Log.WARN, LOG_TAG,
                            "[DEBUG] 'Chuck' menu option should not be visible...");
                }
                return true;

            case R.id.debug_leakcanary:

                if (BuildConfig.DEBUG) {
                    final boolean checked = item.isChecked();
                    item.setChecked(!checked);
                    final boolean commit =
                            mPreferences.edit().putBoolean(DEBUG_LEAKCANARY_PREF_KEY, !checked).commit();
                    Utils.requestBackup(RouterManagementActivity.this);
                    //Restart activity
                    final String waitMessage = String.format("%sabling LeakCanary. Pref commit=%s", checked ? "Dis" : "En", commit);
                    ContextUtils.restartWholeApplication(this, waitMessage, null);
                } else {
                    Crashlytics.log(Log.WARN, LOG_TAG,
                            "[DEBUG] LeakCanary menu option should not be visible...");
                }
                return true;

            case R.id.debug_resourceinspector:

                if (BuildConfig.DEBUG) {
                    final boolean checked = item.isChecked();
                    item.setChecked(!checked);
                    final boolean commit =
                            mPreferences.edit().putBoolean(DEBUG_RESOURCE_INSPECTOR_PREF_KEY, !checked).commit();
                    Utils.requestBackup(RouterManagementActivity.this);
                    //Restart activity
                    final String waitMessage = String.format("%sabling ResourceInspector. Pref. update commit=%s",
                            checked ? "Dis" : "En", commit);
                    ContextUtils.restartWholeApplication(this, waitMessage, null);
                } else {
                    Crashlytics.log(Log.WARN, LOG_TAG,
                            "[DEBUG] ResourceInspector menu option should not be visible...");
                }
                return true;

            case R.id.debug_welcome_screen:
                if (BuildConfig.DEBUG) {
                    if (welcomeScreen != null) {
                        welcomeScreen.forceShow();
                    }
                } else {
                    Crashlytics.log(Log.WARN, LOG_TAG,
                            "[DEBUG] 'Force-show welcome screen' menu option should not be visible...");
                }
                return true;

            case R.id.debug_restore_deleted_routers:
                if (BuildConfig.DEBUG) {
                    final List<Router> allRoutersIncludingArchived = dao.getAllRoutersIncludingArchived();
                    int nbRoutersRestored = 0;
                    for (final Router potentiallyArchivedRouter : allRoutersIncludingArchived) {
                        if (potentiallyArchivedRouter == null || !potentiallyArchivedRouter.isArchived()) {
                            continue;
                        }
                        potentiallyArchivedRouter.setArchived(false);
                        dao.updateRouter(potentiallyArchivedRouter);
                        nbRoutersRestored++;
                    }
                    if (nbRoutersRestored > 0) {
                        doRefreshRoutersListWithSpinner(RoutersListRefreshCause.DATA_SET_CHANGED, null);
                        final String msg = "[DEBUG] Restored " + nbRoutersRestored + " routers.";
                        Crashlytics.log(Log.DEBUG, LOG_TAG, msg);
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Crashlytics.log(Log.WARN, LOG_TAG,
                            "[DEBUG] 'Restore deleted routers' menu option should not be visible...");
                }
                return true;

            case R.id.debug_run_jobs_right_away: {
                if (BuildConfig.DEBUG) {
                    final List<String> jobTags = RouterCompanionJobCreator.getOneShotJobTags();
                    if (jobTags.isEmpty()) {
                        Toast.makeText(this, "Jobs Tags set is empty", Toast.LENGTH_SHORT).show();
                    } else {
                        new AlertDialog.Builder(this)
                                .setTitle("Select Job to trigger")
                                .setItems(jobTags.toArray(new String[jobTags.size()]), new OnClickListener() {
                                    @Override
                                    public void onClick(final DialogInterface dialog, final int which) {
                                        RouterCompanionJobCreator.runJobImmediately(jobTags.get(which));
                                    }
                                })
                                .show();
                    }
                } else {
                    Crashlytics.log(Log.WARN, LOG_TAG,
                            "[DEBUG] 'Run Jobs right away' menu option should not be visible...");
                }
            }
                return true;

            case R.id.debug_cancel_all_jobs: {
                if (BuildConfig.DEBUG) {
                    final Set<String> jobTags = RouterCompanionJobCreator.JOB_MAP.keySet();
                    final List<Router> allRouters = dao.getAllRouters();
                    for (final String jobTag : jobTags) {
                        JobManager.instance().cancelAllForTag(jobTag);
                        //Also for speed-test jobs
                        for (final Router router : allRouters) {
                            if (router == null) {
                                continue;
                            }
                            JobManager.instance()
                                    .cancelAllForTag(
                                            RouterSpeedTestAutoRunnerJob.getActualRouterJobTag(jobTag,
                                                    router.getUuid()));
                        }
                    }
                    Toast.makeText(this, "Requested cancellation for: " + jobTags, Toast.LENGTH_LONG)
                            .show();
                } else {
                    Crashlytics.log(Log.WARN, LOG_TAG,
                            "[DEBUG] 'Cancel ALL jobs' menu option should not be visible...");
                }
            }
                return true;

            case R.id.router_list_actionbar_add:
                this.openAddRouterForm();
                return true;

            case R.id.router_list_take_bug_report:
                Utils.takeBugReport(this);
                return true;
            case R.id.router_list_refresh:
                doRefreshRoutersListWithSpinner(RoutersListRefreshCause.DATA_SET_CHANGED, null);
                return true;
            case R.id.router_list_donate:
                Utils.openDonateActivity(this);
                return true;
            case R.id.router_list_about:
                Utils.launchAboutActivity(RouterManagementActivity.this);
                return true;
            case R.id.router_list_settings:
                this.startActivityForResult(new Intent(this, RouterManagementSettingsActivity.class),
                        ROUTER_MANAGEMENT_SETTINGS_ACTIVITY_CODE);
                return true;
            case R.id.router_list_feedback: {
                Utils.openFeedbackForm(this, "");
                //                final Intent intent = new Intent(this, FeedbackActivity.class);
                //                final File screenshotFile = new File(getCacheDir(), "feedback_screenshot.png");
                //                ViewGroupUtils.exportViewToFile(this, getWindow().getDecorView(), screenshotFile);
                //                intent.putExtra(FeedbackActivity.SCREENSHOT_FILE, screenshotFile.getAbsolutePath());
                //                intent.putExtra(FeedbackActivity.CALLER_ACTIVITY, this.getClass().getCanonicalName());
                //                startActivity(intent);
                //                Utils.buildFeedbackDialog(this, true);
            }
            return true;
            case R.id.router_list_actions_restore_factory_defaults:
                //TODO Hidden for now
                return true;
            case R.id.router_list_actions_firmwares_upgrade:
                //TODO Hidden for now
                return true;
            case R.id.router_list_remove_ads:
                Utils.displayUpgradeMessageForAdsRemoval(this);
                return true;
            case R.id.router_list_actions_reboot_routers: {
                if (routersList == null || routersList.isEmpty()) {
                    Utils.displayMessage(RouterManagementActivity.this,
                            "Empty Router list - action not submitted.", Style.INFO);
                    return true;
                }
                final List<String> allRoutersStr = new ArrayList<>();
                for (Router router : routersList) {
                    if (router == null) {
                        continue;
                    }
                    allRoutersStr.add(
                            String.format("'%s' (%s)", router.getDisplayName(), router.getRemoteIpAddress()));
                }
                new AlertDialog.Builder(this).setIcon(R.drawable.ic_action_alert_warning)
                        .setTitle("Reboot All Router(s)?")
                        .setMessage(String.format("Are you sure you wish to continue? "
                                        + "The following Routers will be rebooted: \n\n%s",
                                Joiner.on("\n").skipNulls().join(allRoutersStr)))
                        .setCancelable(true)
                        .setPositiveButton("Proceed!", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialogInterface, final int i) {

                                Utils.displayMessage(RouterManagementActivity.this,
                                        String.format("Rebooting %d Router(s)....", routersList.size()), Style.INFO);

                                final AtomicInteger currentNum = new AtomicInteger(0);
                                final AtomicInteger numActionsWithNoSuccess = new AtomicInteger(0);
                                final int totalNumOfDevices = routersList.size();

                                for (final Router selectedRouter : routersList) {
                                    ActionManager.runTasks(
                                            new RebootRouterAction(selectedRouter, RouterManagementActivity.this,
                                                    new RouterActionListener() {
                                                        @Override
                                                        public void onRouterActionFailure(
                                                                @NonNull RouterAction routerAction,
                                                                @NonNull Router router,
                                                                @Nullable Exception exception) {
                                                            final int incrementAndGet = currentNum.incrementAndGet();
                                                            numActionsWithNoSuccess.incrementAndGet();
                                                            if (incrementAndGet >= totalNumOfDevices) {
                                                                //An error occurred
                                                                Utils.displayMessage(RouterManagementActivity.this,
                                                                        String.format(
                                                                                "Action '%s' executed but %d error(s) occurred: %s",
                                                                                routerAction.toString(),
                                                                                numActionsWithNoSuccess.get(),
                                                                                Utils.handleException(
                                                                                        exception).first),
                                                                        Style.INFO);
                                                            }
                                                        }

                                                        @Override
                                                        public void onRouterActionSuccess(
                                                                @NonNull RouterAction routerAction,
                                                                @NonNull Router router, Object returnData) {
                                                            final int incrementAndGet = currentNum.incrementAndGet();
                                                            if (incrementAndGet >= totalNumOfDevices) {
                                                                final int numActionsThatDidNotSucceed =
                                                                        numActionsWithNoSuccess.get();
                                                                if (numActionsThatDidNotSucceed > 0) {
                                                                    //An error occurred
                                                                    if (numActionsThatDidNotSucceed
                                                                            < totalNumOfDevices) {
                                                                        Utils.displayMessage(
                                                                                RouterManagementActivity.this,
                                                                                String.format(
                                                                                        "Action '%s' executed but %d error(s) occurred",
                                                                                        routerAction.toString(),
                                                                                        numActionsThatDidNotSucceed),
                                                                                Style.INFO);
                                                                    } else {
                                                                        //No action succeeded
                                                                        Utils.displayMessage(
                                                                                RouterManagementActivity.this,
                                                                                String.format(
                                                                                        "None of the '%s' actions submitted succeeded - please try again later.",
                                                                                        routerAction.toString()),
                                                                                Style.INFO);
                                                                    }
                                                                } else {
                                                                    //No error
                                                                    Utils.displayMessage(
                                                                            RouterManagementActivity.this,
                                                                            String.format(
                                                                                    "Action '%s' executed successfully on %d Routers",
                                                                                    routerAction.toString(),
                                                                                    routersList.size()),
                                                                            Style.CONFIRM);
                                                                }
                                                            }
                                                        }
                                                    }, mPreferences));
                                }
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Cancelled - nothing more to do!
                            }
                        })
                        .create()
                        .show();
            }
            return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextChange(String s) {
        final RouterListRecycleViewAdapter adapter = (RouterListRecycleViewAdapter) mAdapter;
        if (TextUtils.isEmpty(s)) {
            adapter.setRoutersList(dao.getAllRouters());
            adapter.notifyDataSetChanged();
        } else {
            adapter.getFilter().filter(s);
        }
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public void onRefresh() {
        doRefreshRoutersListWithSpinner(RoutersListRefreshCause.DATA_SET_CHANGED, null);
    }

    @Override
    public void onRouterAdd(DialogFragment dialog, Router router, boolean error) {
        if (!error) {
            //Always added to the top
            doRefreshRoutersListWithSpinner(RoutersListRefreshCause.INSERTED, 0);
            mLayoutManager.scrollToPosition(0);
            //Request Backup
            Utils.requestBackup(this);
        }
    }

    @Override
    public void onRouterUpdated(DialogFragment dialog, int position, Router router, boolean error) {
        if (!error) {
            //Refresh everything, as the order actually should remain the same
            //Always added to the top
            //            doRefreshRoutersListWithSpinner(RoutersListRefreshCause.UPDATED, position);
            doRefreshRoutersListWithSpinner(RoutersListRefreshCause.DATA_SET_CHANGED, position);
            mLayoutManager.scrollToPosition(position);
            //Request Backup
            Utils.requestBackup(this);
        }
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
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

    public void setRefreshActionButtonState(final boolean refreshing) {
        mSwipeRefreshLayout.setRefreshing(refreshing);
        if (optionsMenu != null) {
            final MenuItem refreshItem = optionsMenu.findItem(R.id.router_list_refresh);
            if (refreshItem != null) {
                if (refreshing) {
                    refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
                    //                    addNewButton.setVisibility(View.GONE);
                } else {
                    refreshItem.setActionView(null);
                    //                    addNewButton.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(Utils.getBaseContextToAttach(this, newBase));
    }

    private void doRefreshRoutersListWithSpinner(@NonNull final RoutersListRefreshCause cause,
            final Integer position) {
        //        mSwipeRefreshLayout.setEnabled(false);
        setRefreshActionButtonState(true);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<Router> allRouters = RouterManagementActivity.this.dao.getAllRouters();
                    ((RouterListRecycleViewAdapter) RouterManagementActivity.this.mAdapter).setRoutersList(
                            allRouters);
                    switch (cause) {
                        case DATA_SET_CHANGED:
                            RouterManagementActivity.this.mAdapter.notifyDataSetChanged();
                            break;
                        case INSERTED:
                            //Rebuild list so as to add the new router on top
                            //We assume the item with the highest id is the latest added,
                            // and, as such, should be on top of the list
                            final List<Router> routerList = new ArrayList<>(allRouters);
                            Collections.sort(routerList, new Comparator<Router>() {
                                @Override
                                public int compare(Router o1, Router o2) {
                                    return o2.getId() - o1.getId();
                                }
                            });
                            for (int i = 0; i < routerList.size(); i++) {
                                final Router router = routerList.get(i);
                                final int previousOrderIdx = router.getOrderIndex();
                                if (i == position) {
                                    router.setOrderIndex(i);
                                } else {
                                    router.setOrderIndex(previousOrderIdx + 1);
                                }
                                final int newOrderIndex = router.getOrderIndex();
                                Crashlytics.log(Log.DEBUG, LOG_TAG, "XXX Router '"
                                        + router.getCanonicalHumanReadableName()
                                        + "' "
                                        + "new position: "
                                        + previousOrderIdx
                                        + " => "
                                        + newOrderIndex);

                                dao.updateRouter(router);
                            }

                            RouterManagementActivity.this.mAdapter.notifyItemInserted(position);
                            mRecyclerView.scrollToPosition(position);
                            break;
                        case REMOVED:
                            RouterManagementActivity.this.mAdapter.notifyItemRemoved(position);
                            break;
                        case UPDATED:
                            RouterManagementActivity.this.mAdapter.notifyItemChanged(position);
                            break;
                    }

                    setDynamicAppShortcuts();
                } finally {
                    setRefreshActionButtonState(false);
                    //                    mSwipeRefreshLayout.setEnabled(true);
                }
            }
        }, 1000);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            final RouterListRecycleViewAdapter adapter = (RouterListRecycleViewAdapter) mAdapter;
            final String query = intent.getStringExtra(SearchManager.QUERY);
            if (query == null) {
                adapter.setRoutersList(dao.getAllRouters());
                adapter.notifyDataSetChanged();
                return;
            }
            adapter.getFilter().filter(query);
        }
    }

    private void initOpenAddRouterFormIfNecessary() {
        if (mAdapter.getItemCount() == 0) {
            this.openAddRouterForm();
        }
    }

    private void onRouterAdd() {
        onRouterAdd(null, null, false);
        if (this.mCloseOnActionDone) {
            final List<Router> allRouters = RouterManagementActivity.this.dao.getAllRouters();
            if (!allRouters.isEmpty()) {
                final Router newRouter = allRouters.get(0);
                if (newRouter != null) {
                    final Intent data = new Intent();
                    data.putExtra(ROUTER_SELECTED, newRouter.getUuid());
                    setResult(RESULT_OK, data);
                } else {
                    setResult(RESULT_CANCELED);
                }
            } else {
                setResult(RESULT_CANCELED);
            }

            finish();
        }
    }

    private void openAddRouterForm() {
        //        final Fragment addRouter = getSupportFragmentManager().findFragmentByTag(ADD_ROUTER_FRAGMENT_TAG);
        //        if (addRouter instanceof DialogFragment) {
        //            ((DialogFragment) addRouter).dismiss();
        //        }

        //Display Donate Message if trying to add more than the max routers for Free version
        final List<Router> allRouters = dao.getAllRouters();
        //noinspection PointlessBooleanExpression,ConstantConditions
        if ((BuildConfig.DONATIONS || BuildConfig.WITH_ADS)
                && allRouters != null
                && allRouters.size() >= MAX_ROUTERS_FREE_VERSION) {
            //Download the full version to unlock this version
            Utils.displayUpgradeMessage(this, "Manage a new Router");
            return;
        }

        //        final DialogFragment addFragment = new RouterAddDialogFragment();
        //        addFragment.show(getSupportFragmentManager(), ADD_ROUTER_FRAGMENT_TAG);
        startActivityForResult(new Intent(this, ManageRouterFragmentActivity.class), NEW_ROUTER_ADDED);
    }

    private void setDynamicAppShortcuts() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            final ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
            final List<Router> allRouters = dao.getAllRouters();
            if (!allRouters.isEmpty()) {
                final int nbRoutersOnFile = allRouters.size();

                final List<Router> maxRoutersEligibleForAppShortcuts = new ArrayList<>(4);
                final List<Router> nonDemoRoutersEligibleForAppShortcuts = new ArrayList<>(4);
                for (int i = 0; i <= 3; i++) {
                    //We have a limit of 5 app shortcuts (dynamic and static combined),
                    // and a static one is already added. So keep the 4 most recent only
                    if (i >= nbRoutersOnFile) {
                        break;
                    }
                    final Router router = allRouters.get(i);
                    if (router == null) {
                        continue;
                    }
                    maxRoutersEligibleForAppShortcuts.add(router);
                    if (!Utils.isDemoRouter(router)) {
                        nonDemoRoutersEligibleForAppShortcuts.add(router);
                    }
                }

                final List<ShortcutInfo> shortcutInfoList = new ArrayList<>(4);
                for (final Router router : maxRoutersEligibleForAppShortcuts) {
                    final String routerUuid = router.getUuid();
                    final boolean demoRouter = Utils.isDemoRouter(router);
                    final String routerName = router.getName();
                    final String routerCanonicalHumanReadableName = router.getCanonicalHumanReadableName();

                    final Intent shortcutIntent = new Intent(this, DDWRTMainActivity.class);
                    shortcutIntent.setAction(Intent.ACTION_VIEW);
                    shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    shortcutIntent.putExtra(ROUTER_SELECTED, routerUuid);

                    final ShortcutInfo shortcut = new ShortcutInfo.Builder(this, routerUuid).setShortLabel(
                            TextUtils.isEmpty(routerName) ? (router.getRemoteIpAddress()
                                    + ":"
                                    + router.getRemotePort()) : routerName)
                            .setLongLabel(routerCanonicalHumanReadableName)
                            .setIcon(Icon.createWithResource(this,
                                    demoRouter ? R.drawable.demo_router : R.drawable.router))
                            .setIntent(shortcutIntent)
                            .build();

                    shortcutInfoList.add(shortcut);
                }
                shortcutManager.setDynamicShortcuts(shortcutInfoList);

                //Trigger download of avatars (for non-demo routers)
                for (final Router router : nonDemoRoutersEligibleForAppShortcuts) {
                    //Leverage Picasso to fetch router icon, if available
                    try {

                        ImageUtils.downloadImageFromUrl(this,
                                Router.getRouterAvatarUrl(
                                        this,
                                        router,
                                        Router.mAvatarDownloadOpts),
                                new RouterAvatarDownloadTargetForAppShortcut(this, router, true), null, null, null);
                    } catch (final Exception e) {
                        //No worries
                        Utils.reportException(this, e);
                    }
                }
            }
        }
    }

    private void setupCustomTabHelper(final CustomTabActivityHelper.ConnectionCallback cb) {
        mCustomTabActivityHelper = new CustomTabActivityHelper();
        mCustomTabActivityHelper.setConnectionCallback(cb);
        mCustomTabActivityHelper.mayLaunchUrl(
                Uri.parse(RouterCompanionAppConstants.REMOTE_HELP_WEBSITE), null, null);
        mCustomTabActivityHelper.mayLaunchUrl(
                Uri.parse(RouterCompanionAppConstants.REMOTE_HELP_WEBSITE_CHANGELOG), null, null);
    }

    private void startActivity(View view, Intent ddWrtMainIntent) {
        startActivity(RouterManagementActivity.this, view, ddWrtMainIntent);
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