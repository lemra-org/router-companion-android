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

package org.rm3l.ddwrt.main;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cocosw.undobar.UndoBarController;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.about.AboutDialog;
import org.rm3l.ddwrt.actions.BackupRouterAction;
import org.rm3l.ddwrt.actions.RebootRouterAction;
import org.rm3l.ddwrt.actions.RestoreRouterDefaultsAction;
import org.rm3l.ddwrt.actions.RestoreRouterDialogFragment;
import org.rm3l.ddwrt.actions.RouterAction;
import org.rm3l.ddwrt.actions.RouterActionListener;
import org.rm3l.ddwrt.actions.RouterRestoreDialogListener;
import org.rm3l.ddwrt.fragments.PageSlidingTabStripFragment;
import org.rm3l.ddwrt.help.ChangelogActivity;
import org.rm3l.ddwrt.help.HelpActivity;
import org.rm3l.ddwrt.mgmt.RouterAddDialogFragment;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.mgmt.RouterMgmtDialogListener;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.prefs.sort.SortingStrategy;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.settings.RouterSettingsActivity;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.utils.AdUtils;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.ddwrt.mgmt.RouterManagementActivity.ROUTER_SELECTED;
import static org.rm3l.ddwrt.resources.conn.Router.RouterFirmware;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_THEME;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.MAX_ROUTERS_FREE_VERSION;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.SORTING_STRATEGY_PREF;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.SYNC_INTERVAL_MILLIS_PREF;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.THEMING_PREF;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.TILE_REFRESH_MILLIS;

/**
 * Main Android Activity
 * <p/>
 */
public class DDWRTMainActivity extends ActionBarActivity
        implements ViewPager.OnPageChangeListener, UndoBarController.AdvancedUndoListener,
        RouterActionListener,
        RouterMgmtDialogListener,
        RouterRestoreDialogListener, NavigationView.OnNavigationItemSelectedListener {

    public static final String TAG = DDWRTMainActivity.class.getSimpleName();
    public static final String SAVE_ITEM_SELECTED = "SAVE_ITEM_SELECTED";
    public static final String SAVE_ROUTER_SELECTED = "SAVE_ROUTER_SELECTED";
    public static final int ROUTER_SETTINGS_ACTIVITY_CODE = 1;
    public static final String IS_SORTING_STRATEGY_CHANGED = "isSortingStrategyChanged";
    public static final String ROUTER_ACTION = "ROUTER_ACTION";
    public static final String ADD_ROUTER_FRAGMENT_TAG = "add_router";
    private static final int LISTENED_REQUEST_CODE = 77;
    public static final String RESTORE_ROUTER_FRAGMENT_TAG = "RESTORE_ROUTER_FRAGMENT_TAG";
    DrawerLayout mDrawerLayout;
//    ListView mDrawerList;
    ActionBarDrawerToggle mDrawerToggle;
    private Toolbar mToolbar;
    @NonNull
    private DDWRTCompanionDAO dao;
    @NonNull
    private String mRouterUuid;
    private Menu optionsMenu;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
//    private String[] mDDWRTNavigationMenuSections;
    //    private PageSlidingTabStripFragment currentPageSlidingTabStripFragment;
    private int mPosition = 0;
    private String mCurrentSortingStrategy;
    private long mCurrentSyncInterval;
    @NonNull
    private SharedPreferences mGlobalPreferences;
    @NonNull
    private SharedPreferences mPreferences;
    @NonNull
    private Router mRouter;
    private final Runnable mDestroySessionRunnable = new Runnable() {
        @Override
        public void run() {
            SSHUtils.destroySession(DDWRTMainActivity.this, mRouter);
        }
    };
    private DDWRTTile.ActivityResultListener mCurrentActivityResultListener;
//    private NavigationDrawerArrayAdapter mNavigationDrawerAdapter;
    private ArrayAdapter<String> mRoutersListAdapter;
    private ArrayList<Router> mRoutersListForPicker;

    @Nullable
    private InterstitialAd mInterstitialAd;
    private NavigationView mNavigationView;
//    private GoogleApiClient mGoogleApiClient;

    private static final BiMap<Integer, Integer> navigationViewMenuItemsPositions = HashBiMap.create(11);
    static {
        navigationViewMenuItemsPositions.put(R.id.activity_main_nav_drawer_overview, 1);
        navigationViewMenuItemsPositions.put(R.id.activity_main_nav_drawer_status_status, 2);
        navigationViewMenuItemsPositions.put(R.id.activity_main_nav_drawer_status_wireless, 3);
        navigationViewMenuItemsPositions.put(R.id.activity_main_nav_drawer_status_clients, 4);
        navigationViewMenuItemsPositions.put(R.id.activity_main_nav_drawer_status_monitoring, 5);
        navigationViewMenuItemsPositions.put(R.id.activity_main_nav_drawer_services_openvpn, 7);
        navigationViewMenuItemsPositions.put(R.id.activity_main_nav_drawer_services_pptp, 8);
        navigationViewMenuItemsPositions.put(R.id.activity_main_nav_drawer_services_wol, 9);
        navigationViewMenuItemsPositions.put(R.id.activity_main_nav_drawer_admin_commands, 11);
        navigationViewMenuItemsPositions.put(R.id.activity_main_nav_drawer_admin_nvram, 12);
        navigationViewMenuItemsPositions.put(R.id.activity_main_nav_drawer_toolbox_network, 14);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //SQLite
        this.dao = RouterManagementActivity.getDao(this);
        final Intent intent = getIntent();
        String uuid = intent.getStringExtra(ROUTER_SELECTED);
        if (uuid == null) {
            if (savedInstanceState != null) {
                uuid = savedInstanceState.getString(SAVE_ROUTER_SELECTED);
            }
        }
        final Router router = this.dao.getRouter(uuid);

        if (router == null) {
            Toast.makeText(this, "No router set or router no longer exists", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        this.mRouterUuid = router.getUuid();
        this.mRouter = router;

        final RouterFirmware routerFirmware = this.mRouter.getRouterFirmware();

        //FIXME Uncomment once support for other firmwares is provided
//        if (routerFirmware == null || RouterFirmware.UNKNOWN.equals(routerFirmware)) {
//            Utils.displayMessage(this, "Router Firmware unknown or not supported! " +
//                    "Consider editing the Router record to manually specify a supported firmware.", Style.ALERT);
//            finish();
//        }
        //FIXME End

        mPreferences = this.getSharedPreferences(this.mRouterUuid, Context.MODE_PRIVATE);
        this.mGlobalPreferences = this.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);

        //Load from Shared Preferences
        this.mCurrentSortingStrategy = mPreferences.getString(SORTING_STRATEGY_PREF, "");
        this.mCurrentSyncInterval = mPreferences.getLong(SYNC_INTERVAL_MILLIS_PREF, -1l);

        final long mCurrentTheme = this.mGlobalPreferences.getLong(THEMING_PREF, DDWRTCompanionConstants.DEFAULT_THEME);
        if (mCurrentTheme == ColorUtils.LIGHT_THEME) {
            //Light
            setTheme(R.style.AppThemeLight);
        } else {
            //Default is Dark
            setTheme(R.style.AppThemeDark);
        }
        // Inherit theme for router - this is for SettingsActivity,
        // because we are overriding the getSharedPreferences() method
        mPreferences.edit()
                .putLong(THEMING_PREF, mCurrentTheme)
                .apply();

        setContentView(R.layout.activity_main);

        mInterstitialAd = AdUtils.requestNewInterstitial(this, R.string.interstitial_ad_unit_id_router_list_to_router_main);

        setUpToolbar();
        setUpNavDrawer();

        final Integer savedPosition;
        int position = intent.getIntExtra(SAVE_ITEM_SELECTED, 1);
        if (savedInstanceState != null && (savedPosition = savedInstanceState.getInt(SAVE_ITEM_SELECTED)) != null) {
            position = savedPosition;
        }
        if (position < 1) {
            position = 1;
        }

        selectItemInDrawer(position);

//        mDrawerList.performItemClick(
//                mDrawerList.getChildAt(position),
//                position,
//                mDrawerList.getAdapter().getItemId(position));

        //Recreate Default Preferences if they are no longer available
        final boolean putDefaultSortingStrategy = isNullOrEmpty(this.mCurrentSortingStrategy);
        final boolean putDefaultSyncInterval = (this.mCurrentSyncInterval <= 0l);
        final boolean putDefaultTheme = (mCurrentTheme <= 0l);
        if (putDefaultSortingStrategy || putDefaultSyncInterval || putDefaultTheme) {
            //Add default preferences values
            final SharedPreferences.Editor editor = mPreferences.edit();
            if (putDefaultSortingStrategy) {
                editor.putString(SORTING_STRATEGY_PREF, SortingStrategy.DEFAULT);
            }
            if (putDefaultSyncInterval) {
                editor.putLong(SYNC_INTERVAL_MILLIS_PREF, TILE_REFRESH_MILLIS);
            }
            if (putDefaultTheme) {
                editor.putLong(THEMING_PREF, DEFAULT_THEME);
            }
            editor.apply();
        }

        Utils.displayRatingBarIfNeeded(this);
    }

    private BroadcastReceiver mMessageReceiver = new NetworkChangeReceiver();

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(
                mMessageReceiver,
                new IntentFilter(
                        ConnectivityManager.CONNECTIVITY_ACTION));
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

    private void setUpNavDrawer() {
        if (mToolbar != null) {

            mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
            // set a custom shadow that overlays the main content when the drawer
            // opens
//            mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
//        mDDWRTNavigationMenuSections = resources.getStringArray(R.array.navigation_drawer_items_array);

//        initNavigationDrawerAdapter();

//            mDrawerLayout = (DrawerLayout)     findViewById(R.id.drawer_layout);

            //noinspection ConstantConditions
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//            mToolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                }
            });

            mNavigationView = (NavigationView) findViewById(R.id.activity_main_nav_drawer);
            mNavigationView.setNavigationItemSelectedListener(this);

            //Init Header Avatar
            final ImageView navigationViewHeaderAvatar =
                    (ImageView) findViewById(R.id.left_drawer_router_avatar);
            final String routerModel = Router.getRouterModel(this, mRouter);
            if (!(Strings.isNullOrEmpty(routerModel) || "-".equalsIgnoreCase(routerModel))) {
                Utils.downloadImageForRouter(this, routerModel, navigationViewHeaderAvatar);
            }

            initDrawer();

        }
    }

    private void setUpToolbar() {
//        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        final Resources resources = getResources();
        if (mToolbar != null) {
            final RouterFirmware routerFirmware = this.mRouter.getRouterFirmware();
            if (routerFirmware != null) {
                switch (routerFirmware) {
                    case OPENWRT:
                        //Change background color
                        final int colorForOpenWrt = resources.getColor(R.color.win8_orange);
                        mToolbar.setBackgroundColor(colorForOpenWrt);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            final Window window = getWindow();
                            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                            window.setStatusBarColor(colorForOpenWrt);
                        }
                        break;
                    default:
                        //Use default theme
                        break;
                }
            }
        }

        //Routers picker
//        final Spinner routersPicker = (Spinner) findViewById(R.id.left_drawer_routers_dropdown);
        final Spinner routersPicker = (Spinner) mToolbar.findViewById(R.id.toolbar_routers_list_spinner);
        final List<Router> allRouters = dao.getAllRouters();
        if (allRouters == null || allRouters.isEmpty()) {
            Utils.reportException(new
                    IllegalStateException("allRouters is empty, while trying to populate routers picker in main activity drawer"));
            routersPicker.setVisibility(View.GONE);
        } else {
            routersPicker.setVisibility(View.VISIBLE);

            final int allRoutersSize = allRouters.size();
//            final ArrayList<String> routersNamesList = Lists.newArrayListWithCapacity(allRoutersSize);
            mRoutersListForPicker = Lists.newArrayListWithCapacity(allRoutersSize);
            for (final Router router : allRouters) {
                //FIXME Uncomment once full support of other firmwares is implemented
//                final RouterFirmware routerFirmware;
//                if (router == null ||
//                        (routerFirmware = router.getRouterFirmware()) == null ||
//                        RouterFirmware.UNKNOWN.equals(routerFirmware)) {
//                    continue;
//                }
                //FIXME End
                mRoutersListForPicker.add(router);
            }

            final String[] routersNamesArray = new String[mRoutersListForPicker.size() + 1];
            routersNamesArray[0] = "--- ADD NEW ---";

            int i = 1;
            int currentItem = -1;

            for (final Router router : mRoutersListForPicker) {
                if (nullToEmpty(mRouterUuid).equals(router.getUuid())) {
                    currentItem = i;
                }
                final String routerName = router.getName();
                final String effectiveRemoteAddr = Router.getEffectiveRemoteAddr(router, DDWRTMainActivity.this);
                final Integer effectivePort = Router.getEffectivePort(router, DDWRTMainActivity.this);

                final String title = (isNullOrEmpty(routerName) ? effectiveRemoteAddr : routerName);
                final String subTitle =
                        (isNullOrEmpty(routerName) ? ("SSH Port: " + effectivePort) :
                                (effectiveRemoteAddr + ":" + effectivePort));

                routersNamesArray[i++] = (title + "\n(" +
                        subTitle + ")");
            }

            mRoutersListAdapter = new ArrayAdapter<>(this,
                    R.layout.routers_picker_spinner_item, new ArrayList<>(Arrays.asList(routersNamesArray)));
            mRoutersListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            routersPicker.setAdapter(mRoutersListAdapter);
            if (currentItem >= 0) {
                routersPicker.setSelection(currentItem);
            }
            final int currentItemPos = currentItem;
            routersPicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    //Recreate UI with new Router selected
                    final int size = mRoutersListForPicker.size();
                    if (position < 0 || position > size) {
                        return;
                    }
                    if (position == 0) {
                        //Add New Button
                        openAddRouterForm();
                        if (currentItemPos >= 0) {
                            routersPicker.setSelection(currentItemPos);
                        }
                        return;
                    }

                    final Router selectedRouter = mRoutersListForPicker.get(position - 1);
                    if (selectedRouter == null) {
                        return;
                    }
                    final String selectedRouterUuid = selectedRouter.getUuid();
                    final RouterFirmware selectedRouterFirmware = selectedRouter.getRouterFirmware();
                    if (StringUtils.equals(mRouterUuid, selectedRouterUuid)
                            || selectedRouterFirmware == null || RouterFirmware.UNKNOWN.equals(selectedRouterFirmware)) {
                        return;
                    }

                    final Intent intent = getIntent();
                    intent.putExtra(ROUTER_SELECTED, selectedRouterUuid);
                    intent.putExtra(SAVE_ITEM_SELECTED, mPosition);

                    if (BuildConfig.WITH_ADS &&
                            mInterstitialAd != null && AdUtils.canDisplayInterstialAd(DDWRTMainActivity.this)) {

                        mInterstitialAd.setAdListener(new AdListener() {
                            @Override
                            public void onAdClosed() {
                                finish();
                                startActivity(intent);
                            }

                            @Override
                            public void onAdOpened() {
                                //Save preference
                                mGlobalPreferences.edit()
                                        .putLong(
                                                DDWRTCompanionConstants.AD_LAST_INTERSTITIAL_PREF,
                                                System.currentTimeMillis())
                                        .apply();
                            }
                        });

                        if (mInterstitialAd.isLoaded()) {
                            mInterstitialAd.show();
                        } else {
                            //Reload UI
//                            final AlertDialog alertDialog = Utils.
//                                    buildAlertDialog(DDWRTMainActivity.this, null, "Loading...", false, false);
//                            alertDialog.show();
//                            ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                            final ProgressDialog alertDialog = ProgressDialog.show(DDWRTMainActivity.this,
                                    "Switching Routers", "Please wait...", true);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    finish();
                                    startActivity(intent);
                                    alertDialog.cancel();
                                }
                            }, 2000);
                        }

                    } else {
                        //Reload UI
//                        final AlertDialog alertDialog = Utils.
//                                buildAlertDialog(DDWRTMainActivity.this, null, "Loading...", false, false);
//                        alertDialog.show();
//                        ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                        final ProgressDialog alertDialog = ProgressDialog.show(DDWRTMainActivity.this,
                                "Switching Routers", "Please wait...", true);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                finish();
                                startActivity(intent);
                                alertDialog.cancel();
                            }
                        }, 2000);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

//            routersPicker.setOnLongClickListener(new View.OnLongClickListener() {
//                @Override
//                public boolean onLongClick(View v) {
//                    //On long click open up Router Management activity by finishing this activity
//                    finish();
//                    return true;
//                }
//            });


        }

        mTitle = mDrawerTitle = getTitle();
        if (mToolbar != null) {

            final String routerName = mRouter.getName();
            final String effectiveRemoteAddr = Router.getEffectiveRemoteAddr(mRouter, this);
            final Integer effectivePort = Router.getEffectivePort(mRouter, this);

            mToolbar.setTitle(mTitle);
            mToolbar.setSubtitle(isNullOrEmpty(routerName) ? ("SSH Port: " + effectivePort) :
                    (effectiveRemoteAddr + ":" + effectivePort));
            mToolbar.setTitleTextAppearance(getApplicationContext(), R.style.ToolbarTitle);
            mToolbar.setSubtitleTextAppearance(getApplicationContext(), R.style.ToolbarSubtitle);
            mToolbar.setTitleTextColor(getResources().getColor(R.color.white));
            mToolbar.setSubtitleTextColor(getResources().getColor(R.color.white));
            setSupportActionBar(mToolbar);
        }

        // enable ActionBar app icon to behave as action to toggle nav drawer
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(false);
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
        }

    }

//    private void initNavigationDrawerAdapter() {
//        final Resources resources = getResources();
//
//        mNavigationDrawerAdapter = new NavigationDrawerArrayAdapter(this);
//
//        // Section 1: Status: { Status, Wireless, Clients, Monitoring}
//        mNavigationDrawerAdapter.addHeader(resources.getString(R.string.navigation_drawer_items_array_item1_header));
//        String[] menuItems = resources.getStringArray(
//                R.array.navigation_drawer_items_array_item1_items);
//        for (final String item : menuItems) {
//            mNavigationDrawerAdapter.addItem(item, false);
//        }
//
//        //Section 2: Services: {OpenVPN, ...}
//        mNavigationDrawerAdapter.addHeader(resources.getString(R.string.navigation_drawer_items_array_item2_header));
//        menuItems = resources.getStringArray(
//                R.array.navigation_drawer_items_array_item2_items);
//        for (final String item : menuItems) {
//            mNavigationDrawerAdapter.addItem(item, false);
//        }
//
//        //Section 3: Admin Area: {Commands, NVRAM, ...}
//        mNavigationDrawerAdapter.addHeader(resources.getString(R.string.navigation_drawer_items_array_item3_header));
//        menuItems = resources.getStringArray(
//                R.array.navigation_drawer_items_array_item3_items);
//        for (final String item : menuItems) {
//            mNavigationDrawerAdapter.addItem(item, false);
//        }
//
//        //Section 4: Toolbox: {Network, System, ...}
//        mNavigationDrawerAdapter.addHeader(resources.getString(R.string.navigation_drawer_items_array_item4_header));
//        menuItems = resources.getStringArray(
//                R.array.navigation_drawer_items_array_item4_items);
//        for (final String item : menuItems) {
//            mNavigationDrawerAdapter.addItem(item, false);
//        }
//    }

    private void openAddRouterForm() {
        final Fragment addRouter = getSupportFragmentManager().findFragmentByTag(ADD_ROUTER_FRAGMENT_TAG);
        if (addRouter instanceof DialogFragment) {
            ((DialogFragment) addRouter).dismiss();
        }

        //Display Donate Message if trying to add more than the max routers for Free version
        final List<Router> allRouters = dao.getAllRouters();
        //noinspection PointlessBooleanExpression,ConstantConditions
        if ((BuildConfig.DONATIONS || BuildConfig.WITH_ADS) &&
                allRouters != null && allRouters.size() >= MAX_ROUTERS_FREE_VERSION) {
            //Download the full version to unlock this version
            Utils.displayUpgradeMessage(this, "Manage a new Router");
            return;
        }

        final DialogFragment addFragment = new RouterAddDialogFragment();
        addFragment.show(getSupportFragmentManager(), ADD_ROUTER_FRAGMENT_TAG);
    }

    private void initDrawer() {
        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
                mDrawerLayout, /* DrawerLayout object */
                mToolbar,
//                R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
                R.string.navigation_drawer_open, /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
//                if (actionBar != null)
//                    actionBar.setTitle(mTitle);
//                invalidateOptionsMenu(); // creates call to
//                // onPrepareOptionsMenu()
                super.onDrawerClosed(view);
            }

            public void onDrawerOpened(View drawerView) {
//                if (actionBar != null)
//                    actionBar.setTitle(mDrawerTitle);
//                invalidateOptionsMenu(); // creates call to
//                // onPrepareOptionsMenu()
                super.onDrawerOpened(drawerView);
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawers();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(SAVE_ITEM_SELECTED, mPosition);
        savedInstanceState.putString(SAVE_ROUTER_SELECTED, mRouterUuid);
        super.onSaveInstanceState(savedInstanceState);
    }


    @Override
    protected void onPause() {
        //Close SSH Session as well
        destroySSHSession();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mCurrentActivityResultListener = null;
        //Close SSH Session as well
        destroySSHSession();
        try {
            unregisterReceiver(mMessageReceiver);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    private void destroySSHSession() {
        //Async to avoid ANR because SSHUtils#destroySession makes use of locking mechanisms
        new Thread(mDestroySessionRunnable).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.optionsMenu = menu;

        //TODO If pref is not available, hide "Open Web Interface" menu

        getMenuInflater().inflate(R.menu.main_menu, menu);
        if (!BuildConfig.DONATIONS) {
            final MenuItem item = menu.findItem(R.id.action_donate);
            if (item != null) {
                item.setVisible(false);
            }
        }

        if (!StringUtils.containsIgnoreCase(BuildConfig.FLAVOR, "google")) {
            //Only available on Google Play Store
            final MenuItem item = menu.findItem(R.id.action_take_bug_report);
            if (item != null) {
                item.setVisible(false);
            }
        }

        if (!BuildConfig.WITH_ADS) {
            final MenuItem removeAdsMenuItem = menu.findItem(R.id.action_remove_ads);
            if (removeAdsMenuItem != null) {
                removeAdsMenuItem.setVisible(false);
            }
        }

        if (!RouterFirmware.DDWRT.equals(mRouter.getRouterFirmware())) {
            //FIXME Command used to restore factory defaults works best on DD-WRT, not on OpenWRT and other firmwares
            // So hide this menu item until we find a better way to achieve this!
            final MenuItem item = menu.findItem(R.id.action_ddwrt_actions_restore_factory_defaults);
            if (item != null) {
                item.setVisible(false);
            }
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(
            @NonNull MenuItem item) {

        final String displayName = mRouter.getDisplayName();

        switch (item.getItemId()) {

            case android.R.id.home:
                onBackPressed();
                return true;

//            case android.R.id.home: {
//                if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
//                    mDrawerLayout.closeDrawer(mDrawerList);
//                } else {
//                    mDrawerLayout.openDrawer(mDrawerList);
//                }
//                return true;
//            }
//            case R.id.action_refresh:
//                //Disabled for now
//                Toast.makeText(this.getApplicationContext(), "[FIXME] Hold on. Refresh in progress...", Toast.LENGTH_SHORT).show();
//                //FIXME Refresh all tiles currently visible
//                setRefreshActionButtonState(true);
////                if (this.mCurrentRefreshAsyncTask != null) {
////                    this.mCurrentRefreshAsyncTask.cancel(true);
////                }
////                this.mCurrentRefreshAsyncTask = new RefreshAsyncTask();
////                this.mCurrentRefreshAsyncTask.execute();
//                return true;

            case R.id.help:
                this.startActivity(new Intent(this, HelpActivity.class));
                return true;
            case R.id.changelog:
                this.startActivity(new Intent(this, ChangelogActivity.class));
                return true;
            case R.id.action_settings:
                //Open Settings activity for this item
                final Intent ddWrtMainIntent = new Intent(this, RouterSettingsActivity.class);
                ddWrtMainIntent.putExtra(ROUTER_SELECTED, this.mRouterUuid);
                this.startActivityForResult(ddWrtMainIntent, ROUTER_SETTINGS_ACTIVITY_CODE);
                return true;
            case R.id.action_take_bug_report:
                Utils.takeBugReport(this);
                return true;
            case R.id.action_donate:
                Utils.openDonateActivity(this);
                return true;
            case R.id.action_about:
                new AboutDialog(this).show();
                return true;
            case R.id.action_feedback:
                Utils.buildFeedbackDialog(this, true);
                return true;
            case R.id.action_remove_ads:
                Utils.displayUpgradeMessageForAdsRemoval(this);
                return true;
            case R.id.action_ddwrt_actions_open_webinterface:
                //TODO Open Dialog allowing user to set port (defaulting to what we loaded from the Router)
                return true;
            case R.id.action_ddwrt_actions_reboot_router:

                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.ic_action_alert_warning)
                        .setTitle(String.format("Reboot '%s' (%s)", displayName, mRouter.getRemoteIpAddress()))
                        .setMessage(String.format("Are you sure you wish to continue?\n'%s' (%s) will be rebooted, " +
                                "and you might have to wait some time before connection is re-established.",
                                displayName, mRouter.getRemoteIpAddress()))
                        .setCancelable(true)
                        .setPositiveButton("Proceed!", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialogInterface, final int i) {
                                final Bundle token = new Bundle();
                                token.putString(ROUTER_ACTION, RouterAction.REBOOT.name());

                                new UndoBarController.UndoBar(DDWRTMainActivity.this)
                                        .message(String.format("Router '%s' (%s) will be rebooted",
                                                displayName, mRouter.getRemoteIpAddress()))
                                        .listener(DDWRTMainActivity.this)
                                        .token(token)
                                        .show();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Cancelled - nothing more to do!
                            }
                        }).create().show();

                return true;
            case R.id.action_ddwrt_actions_restore_factory_defaults:
                if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
                    //Download the full version to unlock this version
                    Utils.displayUpgradeMessage(this, "Restore Factory Defaults");
                    return true;
                }

                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.ic_action_alert_warning)
                        .setTitle(String.format("Reset '%s' (%s)", displayName, mRouter.getRemoteIpAddress()))
                        .setMessage(String.format("Are you sure you wish to continue?\n" +
                                "This will erase the entire NVRAM, thus resetting all settings back to factory defaults. " +
                                "All of your settings will be erased and '%s' will be rebooted. " +
                                "You might have to wait some time before connection is re-established.\n\n" +
                                "[CAUTION]\n" +
                                "- Make sure to *backup* your settings first!!!\n" +
                                "- After resetting DD-WRT, you need to login with the default user name \"root\" and default password \"admin\".\n" +
                                "- Some devices may not boot properly after being reset. In this case, you will have to reflash them.", displayName))
                        .setCancelable(true)
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Cancelled - nothing more to do!
                            }
                        })
                        .setNeutralButton("*Backup*", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                displayBackupDialog(displayName);
                            }
                        })
                        .setPositiveButton("Got it!\nProceed!", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialogInterface, final int i) {

                                //For reporting
                                Utils.reportException(new
                                        RestoreRouterDefaultsAction.AgreementToResetRouter(DDWRTMainActivity.this));

                                final Bundle token = new Bundle();
                                token.putString(ROUTER_ACTION, RouterAction.RESTORE_FACTORY_DEFAULTS.name());

                                new UndoBarController.UndoBar(DDWRTMainActivity.this)
                                        .message(String.format("Router '%s' (%s) will be reset",
                                                displayName, mRouter.getRemoteIpAddress()))
                                        .listener(DDWRTMainActivity.this)
                                        .token(token)
                                        .show();
                            }
                        })
                        .create().show();

                return true;
            case R.id.action_ddwrt_actions_backup_restore_router_backup:
                if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
                    //Download the full version to unlock this version
                    Utils.displayUpgradeMessage(this, "Backup Router");
                    return true;
                }
                displayBackupDialog(displayName);
                return true;
            case R.id.action_ddwrt_actions_backup_restore_router_restore:
                if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
                    //Download the full version to unlock this version
                    Utils.displayUpgradeMessage(this, "Restore Router");
                    return true;
                }

                final Fragment restoreRouter = getSupportFragmentManager()
                        .findFragmentByTag(RESTORE_ROUTER_FRAGMENT_TAG);
                if (restoreRouter instanceof DialogFragment) {
                    ((DialogFragment) restoreRouter).dismiss();
                }
                final DialogFragment restoreFragment = RestoreRouterDialogFragment.newInstance(mRouterUuid);
                restoreFragment.show(getSupportFragmentManager(), RESTORE_ROUTER_FRAGMENT_TAG);

                return true;
            case R.id.action_ddwrt_actions_firmware_upgrade:
                if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
                    //Download the full version to unlock this version
                    Utils.displayUpgradeMessage(this, "Upgrade Firmware");
                    return true;
                }
                //TODO
                return true;
            default:
                break;

        }

        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);

    }

    public void displayBackupDialog(final String displayName) {
        new AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_action_alert_warning)
                .setTitle(String.format("Backup '%s' (%s)", displayName, mRouter.getRemoteIpAddress()))
                .setMessage(String.format(
                        "You may backup the current configuration in case you need to reset " +
                                "the router back to its factory default settings.\n\n" +
                                "Click the \"Backup\" button to download the configuration backup file of " +
                                "'%s' (%s) to this device.\n" +
                                "You will be able to share the file once the operation is done.",
                        displayName, mRouter.getRemoteIpAddress()))
                .setCancelable(true)
                .setPositiveButton("Backup!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialogInterface, final int i) {
                        final Bundle token = new Bundle();
                        token.putString(ROUTER_ACTION, RouterAction.BACKUP.name());

                        new UndoBarController.UndoBar(DDWRTMainActivity.this)
                                .message(String.format("Backup of Router '%s' (%s) is going to start...",
                                        displayName, mRouter.getRemoteIpAddress()))
                                .listener(DDWRTMainActivity.this)
                                .token(token)
                                .show();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Cancelled - nothing more to do!
                    }
                }).create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        // Check which request we're responding to
        switch (requestCode) {
            case LISTENED_REQUEST_CODE:
                //Forward to listener
                if (mCurrentActivityResultListener != null) {
                    final DDWRTTile.ActivityResultListener listener = mCurrentActivityResultListener;
                    mCurrentActivityResultListener = null;
                    listener.onResultCode(resultCode, data);
                }
                break;
            case ROUTER_SETTINGS_ACTIVITY_CODE:
                // Make sure the request was successful and reload UI if necessary
                if (resultCode == RESULT_OK) {
                    final SharedPreferences mPreferences = this.getSharedPreferences(this.mRouterUuid, Context.MODE_PRIVATE);
                    final long prefSyncIntervalMillis = mPreferences.getLong(SYNC_INTERVAL_MILLIS_PREF, -1l);
                    final String prefSortingStrategy = mPreferences.getString(SORTING_STRATEGY_PREF, "");
                    if (this.mCurrentSyncInterval != prefSyncIntervalMillis ||
                            !this.mCurrentSortingStrategy.equals(prefSortingStrategy)) {
                        //Reload UI
                        final AlertDialog alertDialog = Utils.buildAlertDialog(this, null, "Reloading UI...", false, false);
                        alertDialog.show();
                        ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                finish();
                                final Intent intent = getIntent();
                                intent.putExtra(SAVE_ITEM_SELECTED, mPosition);
                                startActivity(intent);
                                alertDialog.cancel();
                            }
                        }, 2000);
                    }
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void setRefreshActionButtonState(final boolean refreshing) {
        if (optionsMenu != null) {
            final MenuItem refreshItem = optionsMenu
                    .findItem(R.id.action_refresh);
            if (refreshItem != null) {
                if (refreshing) {
                    refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
                } else {
                    refreshItem.setActionView(null);
                }
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private void selectItem(int position) {

        Log.d(TAG, "selectItem @" + position);
        if (position < 0) {
            return;
        }
        this.mPosition = position;

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content,
                        PageSlidingTabStripFragment
                                .newInstance(this, position, this.mRouterUuid))
                .commit();

//        mDrawerLayout.closeDrawer(mDrawerList);
//        mDrawerLayout.closeDrawers();
        mDrawerLayout.closeDrawer(GravityCompat.START);
    }

    public Toolbar getToolbar() {
        return mToolbar;
    }

    /**
     * This method will be invoked when the current page is scrolled, either as part
     * of a programmatically initiated smooth scroll or a user initiated touch scroll.
     *
     * @param position             Position index of the first page currently being displayed.
     *                             Page position+1 will be visible if positionOffset is nonzero.
     * @param positionOffset       Value from [0, 1) indicating the offset from the page at position.
     * @param positionOffsetPixels Value in pixels indicating the offset from position.
     */
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        Log.d(TAG, "onPageScrolled @" + position);
    }

    /**
     * This method will be invoked when a new page becomes selected. Animation is not
     * necessarily complete.
     *
     * @param position Position index of the new selected page.
     */
    @Override
    public void onPageSelected(int position) {
        Log.d(TAG, "onPageSelected (" + position + ")");
    }

    /**
     * Called when the scroll state changes. Useful for discovering when the user
     * begins dragging, when the pager is automatically settling to the current page,
     * or when it is fully stopped/idle.
     *
     * @param state The new scroll state.
     * @see android.support.v4.view.ViewPager#SCROLL_STATE_IDLE
     * @see android.support.v4.view.ViewPager#SCROLL_STATE_DRAGGING
     * @see android.support.v4.view.ViewPager#SCROLL_STATE_SETTLING
     */
    @Override
    public void onPageScrollStateChanged(int state) {
        Log.d(TAG, "onPageScrollStateChanged (" + state + ")");
    }

    @Override
    public void onUndo(@android.support.annotation.Nullable Parcelable parcelable) {
        //Nothing to do
    }

    @Override
    public void onHide(@android.support.annotation.Nullable Parcelable parcelable) {
        if (parcelable instanceof Bundle) {
            final Bundle token = (Bundle) parcelable;
            final String routerAction = token.getString(ROUTER_ACTION);
            Log.d(TAG, "routerAction: [" + routerAction + "]");
            if (isNullOrEmpty(routerAction)) {
                return;
            }
            try {
                switch (RouterAction.valueOf(routerAction)) {
                    case REBOOT:
                        new RebootRouterAction(this, this, mGlobalPreferences).execute(mRouter);
                        break;
                    case RESTORE_FACTORY_DEFAULTS:
                        new RestoreRouterDefaultsAction(this, this, mGlobalPreferences).execute(mRouter);
                        break;
                    case UPGRADE_FIRMWARE:
                        //TODO
                        break;
                    case BACKUP:
                        final AlertDialog alertDialog = Utils.
                                buildAlertDialog(DDWRTMainActivity.this, null, "Backing up - please hold on...", false, false);
                        alertDialog.show();
                        ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                new BackupRouterAction(DDWRTMainActivity.this, new RouterActionListener() {
                                    @Override
                                    public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router, Object returnData) {
                                        try {
                                            String msg;
                                            if (!((returnData instanceof Object[]) &&
                                                    ((Object[]) returnData).length >= 2)) {
                                                msg = String.format("Action '%s' executed " +
                                                                "successfully on host '%s', but an internal error occurred. " +
                                                                "The issue will be reported. Please try again later.",
                                                        routerAction.toString(),
                                                        router.getRemoteIpAddress());
                                                Utils.displayMessage(DDWRTMainActivity.this,
                                                        msg,
                                                        Style.INFO);
                                                Utils.reportException(new IllegalStateException(msg));
                                                return;
                                            }

                                            final Object[] returnDataObjectArray = ((Object[]) returnData);
                                            final Object backupDateObject = returnDataObjectArray[0];
                                            final Object localBackupFileObject = returnDataObjectArray[1];

                                            if (!((backupDateObject instanceof Date) &&
                                                    (localBackupFileObject instanceof File))) {
                                                msg = String.format("Action '%s' executed " +
                                                                "successfully on host '%s', but could not determine where " +
                                                                "local backup file has been saved. Please try again later.",
                                                        routerAction.toString(),
                                                        router.getRemoteIpAddress());
                                                Utils.displayMessage(DDWRTMainActivity.this,
                                                        msg,
                                                        Style.INFO);
                                                Utils.reportException(new IllegalStateException(msg));
                                                return;
                                            }

                                            Utils.displayMessage(DDWRTMainActivity.this,
                                                    String.format("Action '%s' executed successfully on host '%s'. " +
                                                            "Now loading the file sharing activity chooser...",
                                                            routerAction.toString(), router.getRemoteIpAddress()),
                                                    Style.CONFIRM);

                                            final File localBackupFile = (File) (((Object[]) returnData)[1]);
                                            final Date backupDate = (Date) (((Object[]) returnData)[0]);

                                            final Uri uriForFile = FileProvider.getUriForFile(DDWRTMainActivity.this,
                                                    DDWRTCompanionConstants.FILEPROVIDER_AUTHORITY,
                                                    localBackupFile);
                                            grantUriPermission(getPackageName(),
                                                    uriForFile, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                                            final Intent shareIntent = new Intent();
                                            shareIntent.setAction(Intent.ACTION_SEND);
                                            shareIntent.putExtra(Intent.EXTRA_SUBJECT,
                                                    String.format("Backup of Router '%s' (%s)",
                                                            mRouter.getDisplayName(), mRouter.getRemoteIpAddress()));
                                            shareIntent.setType("text/html");
                                            shareIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(
                                                    ("Backup Date: " + backupDate + "\n\n" +
                                                            "You may restore your router later using this Backup Configuration file.\n" +
                                                            "Restoring can be performed either via the 'DD-WRT Companion' app, or using " +
                                                            "the Web Management Interface.\n" +
                                                            "Restoring will overwrite all current configurations " +
                                                            "with the ones in this backup file.\n" +
                                                            "\n" +
                                                            "Please also note that you must only " +
                                                            "restore configurations with files " +
                                                            "backed up using the same firmware and " +
                                                            "the same model of router.\n\n\n").replaceAll("\n", "<br/>") +
                                                            Utils.getShareIntentFooter()));
                                            shareIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);
//                                            shareIntent.setType("*/*");
                                            startActivity(Intent.createChooser(shareIntent,
                                                    getResources().getText(R.string.share_backup)));

                                        } finally {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    alertDialog.cancel();
                                                }
                                            });
                                        }
                                    }

                                    @Override
                                    public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull Router router,
                                                                      @Nullable Exception exception) {
                                        try {
                                            Utils.displayMessage(DDWRTMainActivity.this,
                                                    String.format("Error on action '%s': %s",
                                                            routerAction.toString(),
                                                            ExceptionUtils.getRootCauseMessage(exception)),
                                                    Style.ALERT);
                                        } finally {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    alertDialog.cancel();
                                                }
                                            });
                                        }
                                    }
                                }, mGlobalPreferences)
                                        .execute(mRouter);
                            }
                        }, 1500);
                        break;
                    case RESTORE:
                        //TODO
                        break;
                    default:
                        //Ignored
                        break;
                }
            } catch (IllegalArgumentException | NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClear(@NonNull Parcelable[] parcelables) {
        //Nothing to do
    }

    @Override
    public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router, Object returnData) {
        Utils.displayMessage(this,
                String.format("Action '%s' executed successfully on host '%s'", routerAction.toString(), router.getRemoteIpAddress()),
                Style.CONFIRM);
    }

    @Override
    public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull Router router, @Nullable Exception exception) {
        Utils.displayMessage(this,
                String.format("Error on action '%s': %s", routerAction.toString(), ExceptionUtils.getRootCauseMessage(exception)),
                Style.ALERT);
    }

    public void startActivityForResult(Intent intent, DDWRTTile.ActivityResultListener listener) {
        Log.d(TAG, "startActivityForResult(" + intent + "," + listener +
                ") / mCurrentActivityResultListener=" + mCurrentActivityResultListener);

        if (mCurrentActivityResultListener != null) {
            Log.e(TAG, "Activity trying to start more than one activity at a time...");
            return;
        }
        mCurrentActivityResultListener = listener;
        startActivityForResult(intent, LISTENED_REQUEST_CODE);
    }

    @Override
    public void onRouterAdd(DialogFragment dialog, Router newRouter, boolean error) {
        if (!error) {
            final List<Router> allRouters = dao.getAllRouters();
            final int allRoutersSize = allRouters.size();
            mRoutersListForPicker = Lists.newArrayListWithCapacity(allRoutersSize);
            for (final Router wrt : allRouters) {
                //FIXME Uncomment once other firmwares are fully supported
//                final RouterFirmware routerFirmware;
//                if (wrt == null ||
//                        (routerFirmware = wrt.getRouterFirmware()) == null ||
//                        RouterFirmware.UNKNOWN.equals(routerFirmware)) {
//                    continue;
//                }
                //FIXME End
                mRoutersListForPicker.add(wrt);
            }

            final String[] routersNamesArray = new String[mRoutersListForPicker.size() + 1];
            routersNamesArray[0] = "--- ADD NEW ---";

            int i = 1;
            int currentItem = -1;
            for (final Router router : mRoutersListForPicker) {
                if (nullToEmpty(mRouterUuid).equals(router.getUuid())) {
                    currentItem = i;
                }
//                final String routerName = router.getName();
//                routersNamesArray[i++] = ((isNullOrEmpty(routerName) ? "-" : routerName) + "\n(" +
//                        router.getRemoteIpAddress() + ":" + router.getRemotePort() + ")");
                final String routerName = router.getName();
                final String effectiveRemoteAddr = Router.getEffectiveRemoteAddr(router, DDWRTMainActivity.this);
                final Integer effectivePort = Router.getEffectivePort(router, DDWRTMainActivity.this);

                final String title = (isNullOrEmpty(routerName) ? effectiveRemoteAddr : routerName);
                final String subTitle =
                        (isNullOrEmpty(routerName) ? ("SSH Port: " + effectivePort) :
                                (effectiveRemoteAddr + ":" + effectivePort));

                routersNamesArray[i++] = (title + "\n(" +
                        subTitle + ")");
            }

            mRoutersListAdapter = new ArrayAdapter<>(this,
                    R.layout.routers_picker_spinner_item, routersNamesArray);
            mRoutersListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

//            final Spinner routersPicker = (Spinner) findViewById(R.id.left_drawer_routers_dropdown);
            final Spinner routersPicker = (Spinner) mToolbar.findViewById(R.id.toolbar_routers_list_spinner);
            routersPicker.setAdapter(mRoutersListAdapter);

            if (currentItem >= 0) {
                routersPicker.setSelection(currentItem);
            }

            //Open Spinner right away
            routersPicker.performClick();
        }
    }

    @Override
    public void onRouterUpdated(DialogFragment dialog, int position, Router router, boolean error) {
        //Nothing to do here, as we are not updating routers from here!
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        menuItem.setChecked(true);
        final Integer position = navigationViewMenuItemsPositions.get(menuItem.getItemId());
        if (position != null && position >= 0) {
            selectItem(position);
        }
        return (position != null && position >= 0);
    }

    // The click listener for ListView in the navigation drawer
//    private class DrawerItemClickListener implements
//            ListView.OnItemClickListener {
//        @Override
//        public void onItemClick(AdapterView<?> parent, View view, int position,
//                                long id) {
//            selectItemInDrawer(position);
////            selectItem(position);
////            mNavigationDrawerAdapter.setSelectedItem(position);
////            mNavigationDrawerAdapter.notifyDataSetChanged();
////            mDrawerLayout.invalidate();
//        }
//    }

    public void selectItemInDrawer(int position) {
        final Integer menuItemId = navigationViewMenuItemsPositions.inverse().get(position);
        Log.d(TAG, "selectItemInDrawer: <position,menuItemId>=<" + position + "," + menuItemId + ">");
        if (menuItemId != null) {
            final MenuItem menuItem = mNavigationView.getMenu().findItem(menuItemId);
            if (menuItem != null) {
                menuItem.setChecked(true);
            }
//        mNavigationDrawerAdapter.setSelectedItem(position);
//        mNavigationDrawerAdapter.notifyDataSetChanged();
//            mDrawerLayout.invalidate();
        }
    }

    public class NetworkChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {

            final NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (info != null && info.isConnected()) {
//                final String routerName = mRouter.getName();
//                final String effectiveRemoteAddr = Router.getEffectiveRemoteAddr(mRouter, DDWRTMainActivity.this);
//                final Integer effectivePort = Router.getEffectivePort(mRouter, DDWRTMainActivity.this);
//
//                setTitle(isNullOrEmpty(routerName) ? effectiveRemoteAddr : routerName);
//
//                mTitle = mDrawerTitle = getTitle();
//                if (mToolbar != null) {
//                    mToolbar.setTitle(mTitle);
//                    mToolbar.setSubtitle(isNullOrEmpty(routerName) ? ("SSH Port: " + effectivePort) :
//                            (effectiveRemoteAddr + ":" + effectivePort));
//                }
                final Spinner routersPicker = (Spinner) mToolbar.findViewById(R.id.toolbar_routers_list_spinner);
                final List<Router> allRouters = dao.getAllRouters();
                if (allRouters == null || allRouters.isEmpty()) {
                    Utils.reportException(new
                            IllegalStateException("allRouters is empty, while trying to populate routers picker in main activity drawer"));
                    routersPicker.setVisibility(View.GONE);
                } else {
                    routersPicker.setVisibility(View.VISIBLE);

                    final int allRoutersSize = allRouters.size();
//            final ArrayList<String> routersNamesList = Lists.newArrayListWithCapacity(allRoutersSize);
                    mRoutersListForPicker = Lists.newArrayListWithCapacity(allRoutersSize);
                    for (final Router router : allRouters) {
                        //FIXME Uncomment once full support of other firmwares is implemented
//                final RouterFirmware routerFirmware;
//                if (router == null ||
//                        (routerFirmware = router.getRouterFirmware()) == null ||
//                        RouterFirmware.UNKNOWN.equals(routerFirmware)) {
//                    continue;
//                }
                        //FIXME End
                        mRoutersListForPicker.add(router);
                    }

                    final String[] routersNamesArray = new String[mRoutersListForPicker.size() + 1];
                    routersNamesArray[0] = "--- ADD NEW ---";

                    int i = 1;
                    int currentItem = -1;

                    for (final Router router : mRoutersListForPicker) {
                        if (nullToEmpty(mRouterUuid).equals(router.getUuid())) {
                            currentItem = i;
                        }
                        final String routerName = router.getName();
                        final String effectiveRemoteAddr = Router.getEffectiveRemoteAddr(router, DDWRTMainActivity.this);
                        final Integer effectivePort = Router.getEffectivePort(router, DDWRTMainActivity.this);

                        final String title = (isNullOrEmpty(routerName) ? effectiveRemoteAddr : routerName);
                        final String subTitle =
                                (isNullOrEmpty(routerName) ? ("SSH Port: " + effectivePort) :
                                        (effectiveRemoteAddr + ":" + effectivePort));

                        routersNamesArray[i++] = (title + "\n(" +
                                subTitle + ")");
                    }

                    mRoutersListAdapter = new ArrayAdapter<>(DDWRTMainActivity.this,
                            R.layout.routers_picker_spinner_item, new ArrayList<>(Arrays.asList(routersNamesArray)));
                    mRoutersListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    routersPicker.setAdapter(mRoutersListAdapter);
                    if (currentItem >= 0) {
                        routersPicker.setSelection(currentItem);
                    }
                    final int currentItemPos = currentItem;
                    routersPicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            //Recreate UI with new Router selected
                            final int size = mRoutersListForPicker.size();
                            if (position < 0 || position > size) {
                                return;
                            }
                            if (position == 0) {
                                //Add New Button
                                openAddRouterForm();
                                if (currentItemPos >= 0) {
                                    routersPicker.setSelection(currentItemPos);
                                }
                                return;
                            }

                            final Router selectedRouter = mRoutersListForPicker.get(position - 1);
                            if (selectedRouter == null) {
                                return;
                            }
                            final String selectedRouterUuid = selectedRouter.getUuid();
                            final RouterFirmware selectedRouterFirmware = selectedRouter.getRouterFirmware();
                            if (StringUtils.equals(mRouterUuid, selectedRouterUuid)
                                    || selectedRouterFirmware == null || RouterFirmware.UNKNOWN.equals(selectedRouterFirmware)) {
                                return;
                            }

                            final Intent intent = getIntent();
                            intent.putExtra(ROUTER_SELECTED, selectedRouterUuid);
                            intent.putExtra(SAVE_ITEM_SELECTED, mPosition);

                            if (BuildConfig.WITH_ADS &&
                                    mInterstitialAd != null && AdUtils.canDisplayInterstialAd(DDWRTMainActivity.this)) {

                                mInterstitialAd.setAdListener(new AdListener() {
                                    @Override
                                    public void onAdClosed() {
                                        finish();
                                        startActivity(intent);
                                    }

                                    @Override
                                    public void onAdOpened() {
                                        //Save preference
                                        mGlobalPreferences.edit()
                                                .putLong(
                                                        DDWRTCompanionConstants.AD_LAST_INTERSTITIAL_PREF,
                                                        System.currentTimeMillis())
                                                .apply();
                                    }
                                });

                                if (mInterstitialAd.isLoaded()) {
                                    mInterstitialAd.show();
                                } else {
                                    //Reload UI
//                                    final AlertDialog alertDialog = Utils.
//                                            buildAlertDialog(DDWRTMainActivity.this, null, "Loading...", false, false);
//                                    alertDialog.show();
                                    final ProgressDialog alertDialog = ProgressDialog.show(DDWRTMainActivity.this,
                                            "Switching Routers", "Please wait...", true);
//                                    ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            finish();
                                            startActivity(intent);
                                            alertDialog.cancel();
                                        }
                                    }, 2000);
                                }

                            } else {
                                //Reload UI
//                                final AlertDialog alertDialog = Utils.
//                                        buildAlertDialog(DDWRTMainActivity.this, null, "Loading...", false, false);
//                                alertDialog.show();
//                                ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                                final ProgressDialog alertDialog = ProgressDialog.show(DDWRTMainActivity.this,
                                        "Switching Routers", "Please wait...", true);
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        finish();
                                        startActivity(intent);
                                        alertDialog.cancel();
                                    }
                                }, 2000);
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });

//            routersPicker.setOnLongClickListener(new View.OnLongClickListener() {
//                @Override
//                public boolean onLongClick(View v) {
//                    //On long click open up Router Management activity by finishing this activity
//                    finish();
//                    return true;
//                }
//            });
                }
            }
        }
    }


}
