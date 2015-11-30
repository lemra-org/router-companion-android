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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
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
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cocosw.undobar.UndoBarController;
import com.crashlytics.android.Crashlytics;
import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.sample.ToolbarActionItemTarget;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.util.RecyclerViewCacheUtil;
import com.squareup.picasso.Picasso;

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
import org.rm3l.ddwrt.utils.ReportingUtils;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.isNullOrEmpty;
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
public class DDWRTMainActivity extends AppCompatActivity
        implements ViewPager.OnPageChangeListener, UndoBarController.AdvancedUndoListener,
        RouterActionListener,
        RouterMgmtDialogListener,
        RouterRestoreDialogListener, NavigationView.OnNavigationItemSelectedListener {

    public static final String TAG = DDWRTMainActivity.class.getSimpleName();
    public static final String TAB_INDEX = "TAB_INDEX";
    public static final String SAVE_ITEM_SELECTED = "SAVE_ITEM_SELECTED";
    public static final String SAVE_ROUTER_SELECTED = "SAVE_ROUTER_SELECTED";
    public static final int ROUTER_SETTINGS_ACTIVITY_CODE = 1;
    public static final String IS_SORTING_STRATEGY_CHANGED = "isSortingStrategyChanged";
    public static final String ROUTER_ACTION = "ROUTER_ACTION";
    public static final String ADD_ROUTER_FRAGMENT_TAG = "add_router";
    private static final int LISTENED_REQUEST_CODE = 77;
    public static final String RESTORE_ROUTER_FRAGMENT_TAG = "RESTORE_ROUTER_FRAGMENT_TAG";
    public static final int ADD_NEW_ROUTER = 123;
    public static final int MANAGE_ROUTERS = 234;
    public static final String DRAWER_OPEN_FIRST_LAUNCH_PREF = "drawer_open_first_launch";

    private Toolbar mToolbar;
    @NonNull
    private DDWRTCompanionDAO dao;
    @NonNull
    private String mRouterUuid;
    private Menu optionsMenu;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;

    private int mPosition = 0;
    private int mTabPosition = 0;
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
            SSHUtils.destroySessions(mRouter);
        }
    };
    private DDWRTTile.ActivityResultListener mCurrentActivityResultListener;

    private AccountHeader mDrawerHeaderResult;
    private Drawer mDrawerResult;

    @Nullable
    private InterstitialAd mInterstitialAd;

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

    public static final String[] opts = new String[]
            {
                    "w_300",
                    "h_300",
                    "q_100",
                    "c_thumb",
                    "g_center",
                    "r_20",
                    "e_improve",
                    "e_make_transparent",
                    "e_trim"
            };

    private boolean mIsThemeLight;

    private BroadcastReceiver mMessageReceiver;
    private ViewPager mViewPager;
    private TabLayout mTabLayout;

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

        //Report
        final Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("Model", Utils.isDemoRouter(router) ? DDWRTCompanionConstants.DEMO :
                Router.getRouterModel(this, router));
        ReportingUtils.reportEvent(ReportingUtils.EVENT_ROUTER_OPENED, eventMap);

        this.mMessageReceiver = new NetworkChangeReceiver();

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
        this.mCurrentSyncInterval = mPreferences.getLong(SYNC_INTERVAL_MILLIS_PREF, -10l);

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

        mIsThemeLight = ColorUtils.isThemeLight(this);

        mInterstitialAd = AdUtils.requestNewInterstitial(this,
                R.string.interstitial_ad_unit_id_router_list_to_router_main);

        setUpToolbar();
        setUpViewPager();
        setUpNavDrawer();

        IProfile activeProfile = null;
        final List<Router> allRouters = dao.getAllRouters();
        final IProfile[] iProfiles;

        if (allRouters != null) {
            iProfiles = new IProfile[allRouters.size() + 2];
            int i = 0;
            for (final Router routerFromAll : allRouters) {
                final int routerFromAllId = routerFromAll.getId();
                final ProfileDrawerItem profileDrawerItem = new ProfileDrawerItem()
                        .withIdentifier(routerFromAllId)
                        .withNameShown(true)
                        .withName(routerFromAll.getDisplayName())
                        .withEmail(routerFromAll.getRemoteIpAddress() + ":" +
                                routerFromAll.getRemotePort());
                final String routerModel = Router.getRouterModel(this, routerFromAll);
                Crashlytics.log(Log.DEBUG,  TAG, "routerModel: " + routerModel);
                if (!(isNullOrEmpty(routerModel) || "-".equalsIgnoreCase(routerModel))) {
                    if (Utils.isDemoRouter(routerFromAll)) {
                        profileDrawerItem.withIcon(R.drawable.demo_router);
                    } else {
                        final String routerModelNormalized = routerModel.toLowerCase().replaceAll("\\s+", "");
                        try {
                            final String url = String.format("%s/%s/%s",
                                    DDWRTCompanionConstants.IMAGE_CDN_URL_PREFIX,
                                    Joiner
                                            .on(",")
                                            .skipNulls().join(opts),
                                    URLEncoder.encode(routerModelNormalized, Charsets.UTF_8.name()));
                            profileDrawerItem.withIcon(url);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                            ReportingUtils.reportException(DDWRTMainActivity.this, e);
                        }
                    }

                } else {
                    profileDrawerItem.withIcon(Utils.isDemoRouter(routerFromAll) ?
                            R.drawable.demo_router : R.drawable.router);
                }

                iProfiles[i++] = profileDrawerItem;
                if (mRouter.getUuid().equals(routerFromAll.getUuid())) {
                    activeProfile = profileDrawerItem;
                }
            }
        } else {
            iProfiles = new IProfile[2];
        }
        iProfiles[iProfiles.length-2] = //don't ask but google uses 14dp for the add account icon in gmail but 20dp for the normal icons (like manage account)
                new ProfileSettingDrawerItem().withName("Add Router").withDescription("Add new Router")
                        .withIcon(R.drawable.ic_action_content_add_drawer_header)
                        .withIdentifier(ADD_NEW_ROUTER)
                        .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                            @Override
                            public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                                openAddRouterForm();
                                return true;
                            }
                        });
        iProfiles[iProfiles.length-1] = //don't ask but google uses 14dp for the add account icon in gmail but 20dp for the normal icons (like manage account)
                new ProfileSettingDrawerItem().withName("Manage Routers").withDescription("Manage registered Routers")
                        .withIcon(android.R.drawable.ic_menu_preferences)
                        .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                            @Override
                            public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                                //Open RouterManagementActivity
                                DDWRTMainActivity.this.finish();
                                startActivity(new Intent(DDWRTMainActivity.this, RouterManagementActivity.class));
                                return true;
                            }
                        })
                        .withIdentifier(MANAGE_ROUTERS);

        //Set Drawer Header Background
        final int drawerHeaderDrawable;
        if (routerFirmware != null) {
            switch (routerFirmware) {
                case DDWRT:
                    drawerHeaderDrawable = R.drawable.logo_ddwrt;
                    break;
                //TODO Add other logos down here
                default:
                    drawerHeaderDrawable = R.drawable.router_picker_background;
                    break;
            }
        } else {
            drawerHeaderDrawable = R.drawable.router_picker_background;
        }
        mDrawerHeaderResult = new AccountHeaderBuilder()
                .withActivity(this)
                .addProfiles(iProfiles)
                .withSavedInstance(savedInstanceState)
                .withHeaderBackground(drawerHeaderDrawable)
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean current) {
                        if (profile == null) {
                            ReportingUtils.reportException(DDWRTMainActivity.this, new IllegalStateException("routerUuid is NULL or empty"));
                            Toast.makeText(DDWRTMainActivity.this, "Internal Error - please try again later", Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        final int profileIdentifier = profile.getIdentifier();
                        Crashlytics.log(Log.DEBUG,  TAG, "OnAccountHeaderListener: " + profileIdentifier);
                        if (profileIdentifier == ADD_NEW_ROUTER ||
                                profileIdentifier == MANAGE_ROUTERS) {
                            //Already handled
                            return true;
                        }
                        final Router daoRouter = dao.getRouter(profileIdentifier);

                        if (daoRouter == null) {
                            Toast.makeText(DDWRTMainActivity.this, "Internal Error - please try again later", Toast.LENGTH_SHORT).show();
                            ReportingUtils.reportException(DDWRTMainActivity.this, new IllegalStateException("daoRouter NOT found"));
                            return false;
                        }

                        final String routerUuid = daoRouter.getUuid();
                        if (routerUuid.isEmpty()) {
                            Toast.makeText(DDWRTMainActivity.this, "Internal Error - please try again later", Toast.LENGTH_SHORT).show();
                            ReportingUtils.reportException(DDWRTMainActivity.this, new IllegalStateException("routerUuid is NULL or empty"));
                            return false;
                        }

                        //Switch Router
                        final Intent intent = getIntent();
                        intent.putExtra(ROUTER_SELECTED, routerUuid);
                        intent.putExtra(SAVE_ITEM_SELECTED, mPosition);
                        intent.putExtra(TAB_INDEX, mTabPosition);

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

                        return true;
                    }
                })
                .build();

        mDrawerResult = new DrawerBuilder()
                .withActivity(this)
                .withTranslucentStatusBar(false)
                .withActionBarDrawerToggle(true)
                .withSavedInstance(savedInstanceState)
                .withToolbar(mToolbar)
                .withAccountHeader(mDrawerHeaderResult) //set the AccountHeader we created earlier for the header
                .withSavedInstance(savedInstanceState)
                .withShowDrawerOnFirstLaunch(false)
                .withCloseOnClick(true)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName("Overview")
                                .withIcon(mIsThemeLight ?
                                        R.drawable.ic_action_dashboard : R.drawable.ic_action_dashboard_dark)
                                .withIdentifier(1),

                        new SectionDrawerItem().withName("Status").withSelectable(false),
                        new PrimaryDrawerItem().withName("Status")
                                .withIcon(mIsThemeLight ?
                                        R.drawable.ic_action_action_info : R.drawable.ic_action_action_info_dark)
                                .withIdentifier(2),
                        new PrimaryDrawerItem().withName("Wireless")
                                .withIcon(mIsThemeLight ?
                                        R.drawable.ic_action_device_signal_wifi_3_bar :
                                        R.drawable.ic_action_device_signal_wifi_3_bar_dark)
                                .withIdentifier(3),
                        new PrimaryDrawerItem().withName("Clients")
                                .withIcon(mIsThemeLight ?
                                        R.drawable.ic_action_devices :
                                        R.drawable.ic_action_devices_dark)
                                .withIdentifier(4),
                        new PrimaryDrawerItem().withName("Monitoring")
                                .withIcon(mIsThemeLight ?
                                        R.drawable.ic_action_trending_up :
                                        R.drawable.ic_action_trending_up_dark)
                                .withIdentifier(5),

                        new SectionDrawerItem().withName("Services").withSelectable(false),
                        new PrimaryDrawerItem().withName("OpenVPN")
                                .withIcon(mIsThemeLight ? R.drawable.ic_action_openvpn : R.drawable.ic_action_openvpn_dark)
                                .withIdentifier(7),
                        new PrimaryDrawerItem().withName("PPTP")
                                .withIcon(mIsThemeLight ? R.drawable.ic_action_communication_vpn_key :
                                        R.drawable.ic_action_communication_vpn_key_dark)
                                .withIdentifier(8),
                        new PrimaryDrawerItem().withName("Wake On LAN")
                                .withIcon(mIsThemeLight ? R.drawable.ic_action_wol_3 : R.drawable.ic_action_wol_3_dark)
                                .withIdentifier(9),

                        new SectionDrawerItem().withName("Admin Area").withSelectable(false),
                        new PrimaryDrawerItem().withName("Commands")
                                .withIcon(mIsThemeLight ? R.drawable.ic_action_terminal : R.drawable.ic_action_terminal_dark)
                                .withIdentifier(11),
                        new PrimaryDrawerItem().withName("NVRAM")
                                .withIcon(mIsThemeLight ?
                                        R.drawable.ic_action_ram_3 : R.drawable.ic_action_ram_3_dark)
                                .withIdentifier(12),

                        new SectionDrawerItem().withName("Toolbox").withSelectable(false),
                        new PrimaryDrawerItem().withName("Network")
                                .withIcon(mIsThemeLight ?
                                        R.drawable.ic_action_network :
                                        R.drawable.ic_action_network_dark)
                                .withIdentifier(14)
                )
                .addStickyDrawerItems(
                        new PrimaryDrawerItem().withName("Help")
                                .withIcon(android.R.drawable.ic_menu_help)
                                .withIdentifier(1001),
//                        new PrimaryDrawerItem().withName("What's new").withIcon(R.drawable.ic_action_action_info_outline).withIdentifier(1002),
                        new PrimaryDrawerItem().withName("Send Feedback")
                                .withIcon(mIsThemeLight ?
                                    R.drawable.ic_action_action_thumbs_up_down :
                                        R.drawable.ic_action_action_thumbs_up_down_dark)
                                .withIdentifier(1003)
//                        new PrimaryDrawerItem().withName("About").withIcon(R.drawable.ic_action_social_person).withIdentifier(1004)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        // do something with the clicked item :D
                        if (drawerItem == null) {
                            ReportingUtils.reportException(DDWRTMainActivity.this, new IllegalStateException("drawerItem == null"));
                            return false;
                        }
                        final int identifier = drawerItem.getIdentifier();
                        if (identifier < 0) {
                            ReportingUtils.reportException(DDWRTMainActivity.this, new IllegalStateException("identifier < 0"));
                            return false;
                        }
                        if (identifier < 1000) {
                            selectItem(identifier, 0);
                        } else {
                            switch (identifier) {
                                case 1001:
                                    //Help
                                    startActivity(new Intent(DDWRTMainActivity.this, HelpActivity.class));
                                    break;
                                case 1002:
                                    //Changelog
                                    startActivity(new Intent(DDWRTMainActivity.this, ChangelogActivity.class));
                                    break;
                                case 1003:
                                    //Feedback
                                    Utils.buildFeedbackDialog(DDWRTMainActivity.this, true);
                                    break;
                                case 1004:
                                    //About
                                    new AboutDialog(DDWRTMainActivity.this).show();
                                    break;
                                default:
                                    break;
                            }
                            // Select previously selected item
                            if (mPosition >= 0) {
                                selectItem(mPosition);
                            }
                        }
                        //Force close after selection
                        if (mDrawerResult.isDrawerOpen()) {
                            mDrawerResult.closeDrawer();
                        }
                        return true;
                    }
                })
                .build();

        //if you have many different types of DrawerItems you can magically pre-cache those items to get a better scroll performance
        //make sure to init the cache after the DrawerBuilder was created as this will first clear the cache to make sure no old elements are in
        RecyclerViewCacheUtil.getInstance().withCacheSize(3).init(mDrawerResult);

        final Integer savedPosition;
        int position = intent.getIntExtra(SAVE_ITEM_SELECTED, 1);
        if (savedInstanceState != null && (savedPosition = savedInstanceState.getInt(SAVE_ITEM_SELECTED)) != null) {
            position = savedPosition;
        }
        if (position < 1) {
            position = 1;
        }

        // set the selection to the item with the identifier 11
        mDrawerResult.setSelection(position, true);

//        selectItemInDrawer(position);

        //set the active profile
        if (activeProfile != null) {
            mDrawerHeaderResult.setActiveProfile(activeProfile);
        }

        final boolean drawerOpenFirstLaunch = mGlobalPreferences
                .getBoolean(DRAWER_OPEN_FIRST_LAUNCH_PREF, false);
        if (!drawerOpenFirstLaunch) {
            if (!mDrawerResult.isDrawerOpen()) {
                mDrawerResult.openDrawer();
            }
            mGlobalPreferences.edit()
                    .putBoolean(DRAWER_OPEN_FIRST_LAUNCH_PREF, true)
                    .apply();
        }

        final int scvTheme = (mIsThemeLight ? R.style.SCV_Custom_semi_transparent :
                R.style.SCV_Custom_semi_transparent_Dark);

        final Resources resources = getResources();

        if (DDWRTCompanionConstants.SHOWCASEVIEW_ENABLED) {
            try {
                final ShowcaseView showcaseView = new ShowcaseView.Builder(this)
                        .setStyle(scvTheme)
                        .setTarget(new ViewTarget(mToolbar.getChildCount() > 0 ?
                                mToolbar.getChildAt(0) : mToolbar))
                        .setContentTitle("Tips (1/3)")
                        .setContentText("Slide left to right to get quick access to your routers")
                        .singleShot(1)
                        .hideOnTouchOutside()
                        .setShowcaseEventListener(new OnShowcaseEventListener() {
                            @Override
                            public void onShowcaseViewHide(ShowcaseView showcaseView) {

                            }

                            @Override
                            public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                                try {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (mDrawerResult.isDrawerOpen()) {
                                                mDrawerResult.closeDrawer();
                                            }
                                        }
                                    });
                                    final ShowcaseView showcaseView1 = new ShowcaseView.Builder(DDWRTMainActivity.this)
                                            .setStyle(scvTheme)
                                            .setTarget(new ToolbarActionItemTarget(mToolbar, R.id.action_ddwrt_actions))
                                            .setContentTitle("Tips (2/3)")
                                            .setContentText("Use the menu items to perform actions on the router or override the default settings.")
                                            .singleShot(11)
                                            .hideOnTouchOutside()
                                            .setShowcaseEventListener(new OnShowcaseEventListener() {
                                                @Override
                                                public void onShowcaseViewHide(ShowcaseView showcaseView) {

                                                }

                                                @Override
                                                public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                                                    try {
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                if (mDrawerResult.isDrawerOpen()) {
                                                                    mDrawerResult.closeDrawer();
                                                                }
                                                            }
                                                        });
                                                        final ShowcaseView showcaseView2 = new ShowcaseView.Builder(DDWRTMainActivity.this)
                                                                .setStyle(scvTheme)
                                                                .setContentTitle("Tips (3/3)")
                                                                .setContentText("Pull to manually refresh data.")
                                                                .singleShot(12)
                                                                .hideOnTouchOutside()
                                                                .setShowcaseEventListener(new OnShowcaseEventListener() {
                                                                    @Override
                                                                    public void onShowcaseViewHide(ShowcaseView showcaseView) {

                                                                    }

                                                                    @Override
                                                                    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {

                                                                    }

                                                                    @Override
                                                                    public void onShowcaseViewShow(ShowcaseView showcaseView) {

                                                                    }
                                                                })
                                                                .build();

                                                        final Bitmap pullTopBottomScvBitmap = BitmapFactory.decodeResource(resources,
                                                                R.drawable.pull_refresh_transparent);
                                                        final BitmapDrawable pullTopBottomScvDrawable = new BitmapDrawable(resources,
                                                                pullTopBottomScvBitmap);
                                                        final ImageView pullTopBottomScvImageView = new ImageView(DDWRTMainActivity.this);
                                                        pullTopBottomScvImageView.setLayoutParams(new
                                                                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                                                        pullTopBottomScvImageView.setImageDrawable(pullTopBottomScvDrawable);
                                                        showcaseView2.addView(pullTopBottomScvImageView);
                                                    } catch (final Exception e) {
                                                        e.printStackTrace();
                                                        Utils.reportException(DDWRTMainActivity.this, e);
                                                        //No worries
                                                    }

                                                }

                                                @Override
                                                public void onShowcaseViewShow(ShowcaseView showcaseView) {

                                                }
                                            })
                                            .build();
                                } catch (final Exception e) {
                                    e.printStackTrace();
                                    Utils.reportException(DDWRTMainActivity.this, e);
                                    //No worries
                                }
                            }

                            @Override
                            public void onShowcaseViewShow(ShowcaseView showcaseView) {

                            }
                        })
                        .build();

                final Bitmap slideLeftRightScvBitmap = BitmapFactory.decodeResource(resources,
                        R.drawable.slide_left_right_transparent);
                final BitmapDrawable slideLeftRightScvDrawable = new BitmapDrawable(resources, slideLeftRightScvBitmap);
                slideLeftRightScvDrawable.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
                final ImageView slideLeftRightScvImageView = new ImageView(DDWRTMainActivity.this);
                final ViewGroup.LayoutParams params = new
                        ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                slideLeftRightScvImageView.setLayoutParams(params);
                slideLeftRightScvImageView.setImageDrawable(slideLeftRightScvDrawable);
                showcaseView.addView(slideLeftRightScvImageView);

            } catch (final Exception e) {
                e.printStackTrace();
                Utils.reportException(this, e);
                //No worries
            }
        }

        //Recreate Default Preferences if they are no longer available
        final boolean putDefaultSortingStrategy = isNullOrEmpty(this.mCurrentSortingStrategy);
        final boolean putDefaultSyncInterval = (this.mCurrentSyncInterval < -1l);
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

    @Override
    protected void onResume() {
        super.onResume();
        try {
            registerReceiver(
                    mMessageReceiver,
                    new IntentFilter(
                            ConnectivityManager.CONNECTIVITY_ACTION));
        } catch (final Exception e) {
            Utils.reportException(this, e);
            e.printStackTrace();
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

    private void setUpNavDrawer() {
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setUpToolbar() {

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        final Resources resources = getResources();
        if (mToolbar != null) {
            final RouterFirmware routerFirmware = this.mRouter.getRouterFirmware();
            if (routerFirmware != null) {
                switch (routerFirmware) {
                    case OPENWRT:
                        //Change background color
                        final int colorForOpenWrt = ContextCompat
                                .getColor(this, R.color.win8_orange);
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
            mToolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
            mToolbar.setSubtitleTextColor(ContextCompat.getColor(this, R.color.white));
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

    @Override
    public void onBackPressed() {
        //handle the back press :D close the drawer first and if the drawer is closed close the activity
        if (mDrawerResult != null && mDrawerResult.isDrawerOpen()) {
            mDrawerResult.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(SAVE_ITEM_SELECTED, mPosition);
        savedInstanceState.putString(SAVE_ROUTER_SELECTED, mRouterUuid);
        savedInstanceState.putInt(TAB_INDEX, mTabPosition);
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
            //No worries
        } finally {
            //Drop strong reference to Picasso
            final ImageView navigationViewHeaderAvatar =
                    (ImageView) findViewById(R.id.left_drawer_router_avatar);
            if (navigationViewHeaderAvatar != null) {
                Picasso.with(this).cancelRequest(navigationViewHeaderAvatar);
            }
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

        final Map<String, Object> eventMap = new HashMap<>();

        switch (item.getItemId()) {

            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.help:
                this.startActivity(new Intent(this, HelpActivity.class));
                eventMap.put("Menu Item", "Help");
                ReportingUtils.reportEvent(ReportingUtils.EVENT_MENU_ITEM, eventMap);
                return true;
            case R.id.changelog:
                this.startActivity(new Intent(this, ChangelogActivity.class));
                eventMap.put("Menu Item", "Changelog");
                ReportingUtils.reportEvent(ReportingUtils.EVENT_MENU_ITEM, eventMap);
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
                eventMap.put("Menu Item", "About");
                ReportingUtils.reportEvent(ReportingUtils.EVENT_MENU_ITEM, eventMap);
                return true;
            case R.id.action_feedback:
                Utils.buildFeedbackDialog(this, true);
                ReportingUtils.reportEvent(ReportingUtils.EVENT_FEEDBACK, eventMap);
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
                                ReportingUtils.reportException(DDWRTMainActivity.this, new
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

        return true;
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
        Crashlytics.log(Log.DEBUG,  TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
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
                    final long prefSyncIntervalMillis = mPreferences.getLong(SYNC_INTERVAL_MILLIS_PREF, -10l);
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
                                intent.putExtra(TAB_INDEX, mTabPosition);
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


    private void setUpViewPager() {
        mViewPager = (ViewPager) findViewById(R.id.tabanim_viewpager);
        mTabLayout = (TabLayout) findViewById(R.id.tabanim_tabs);
        mTabLayout.setSelectedTabIndicatorColor(
                        ColorUtils
                            .getColor(DDWRTMainActivity.class.getCanonicalName()));

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout) {

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                mTabPosition = position;
            }
        });
    }

    private void selectItem(int position) {
        selectItem(position, 0);
    }

    private void selectItem(int position, int tabPosition) {

        Crashlytics.log(Log.DEBUG,  TAG, "selectItem @" + position);
        if (position < 0) {
            return;
        }
        this.mPosition = position;
        this.mTabPosition = tabPosition;

        final PageSlidingTabStripFragment.FragmentTabsAdapter fragmentTabsAdapter =
                new PageSlidingTabStripFragment.FragmentTabsAdapter(
                        this,
                        this.mPosition,
                        getSupportFragmentManager(),
                        getResources(),
                        String.format("%s.%s",
                                SortingStrategy.class.getPackage().getName(),
                                getSharedPreferences(mRouterUuid, Context.MODE_PRIVATE)
                                        .getString(DDWRTCompanionConstants.SORTING_STRATEGY_PREF,
                                                SortingStrategy.DEFAULT)
                        ),
                        mRouterUuid);
        final int mFragmentTabsAdapterCount = fragmentTabsAdapter.getCount();
        mViewPager.setOffscreenPageLimit(mFragmentTabsAdapterCount);
        mViewPager.setAdapter(fragmentTabsAdapter);

        if (tabPosition >= 0 && tabPosition < mFragmentTabsAdapterCount) {
            mViewPager.setCurrentItem(tabPosition);
        }

        mTabLayout.setupWithViewPager(mViewPager);

//        getSupportFragmentManager()
//                .beginTransaction()
//                .replace(R.id.content,
//                        PageSlidingTabStripFragment
//                                .newInstance(this, position, this.mRouterUuid))
//                .commit();

        if (mDrawerResult.isDrawerOpen()) {
            mDrawerResult.closeDrawer();
        }

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
        Crashlytics.log(Log.DEBUG,  TAG, "onPageScrolled @" + position);
    }

    /**
     * This method will be invoked when a new page becomes selected. Animation is not
     * necessarily complete.
     *
     * @param position Position index of the new selected page.
     */
    @Override
    public void onPageSelected(int position) {}

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
    public void onPageScrollStateChanged(int state) {}

    @Override
    public void onUndo(@android.support.annotation.Nullable Parcelable parcelable) {
        //Nothing to do
    }

    @Override
    public void onHide(@android.support.annotation.Nullable Parcelable parcelable) {
        if (parcelable instanceof Bundle) {
            final Bundle token = (Bundle) parcelable;
            final String routerAction = token.getString(ROUTER_ACTION);
            Crashlytics.log(Log.DEBUG,  TAG, "routerAction: [" + routerAction + "]");
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
                                                ReportingUtils.reportException(DDWRTMainActivity.this, new IllegalStateException(msg));
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
                                                ReportingUtils.reportException(DDWRTMainActivity.this, new IllegalStateException(msg));
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

        if (mCurrentActivityResultListener != null) {
            Crashlytics.log(Log.ERROR, TAG, "Activity trying to start more than one activity at a time...");
            return;
        }
        mCurrentActivityResultListener = listener;
        startActivityForResult(intent, LISTENED_REQUEST_CODE);
    }

    @Override
    public void onRouterAdd(final DialogFragment dialog, final Router newRouter, final boolean error) {
        if (!error) {

            final int newRouterId = newRouter.getId();

            final IProfile newProfile = new ProfileDrawerItem()
                    .withIdentifier(newRouterId)
                    .withNameShown(true)
                    .withName(newRouter.getDisplayName())
                    .withEmail(newRouter.getRemoteIpAddress() + ":" + newRouter.getRemotePort());

            final String routerModel = Router.getRouterModel(this, newRouter);
            Crashlytics.log(Log.DEBUG,  TAG, "routerModel: " + routerModel);
            if (!(isNullOrEmpty(routerModel) || "-".equalsIgnoreCase(routerModel))) {
                if (Utils.isDemoRouter(newRouter)) {
                    newProfile.withIcon(R.drawable.demo_router);
                } else {
                    final String routerModelNormalized = routerModel.toLowerCase().replaceAll("\\s+", "");
                    try {
                        final String url = String.format("%s/%s/%s",
                                DDWRTCompanionConstants.IMAGE_CDN_URL_PREFIX,
                                Joiner
                                        .on(",")
                                        .skipNulls().join(opts),
                                URLEncoder.encode(routerModelNormalized, Charsets.UTF_8.name()));
                        newProfile.withIcon(url);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                        ReportingUtils.reportException(DDWRTMainActivity.this, e);
                    }
                }

            } else {
                newProfile.withIcon(Utils.isDemoRouter(newRouter) ?
                        R.drawable.demo_router : R.drawable.router);
            }

            //Always add on top
            mDrawerHeaderResult.addProfile(newProfile, 0);

            if (mDrawerResult.isDrawerOpen()) {
                mDrawerResult.closeDrawer();
            }
        }
    }

    @Override
    public void onRouterUpdated(DialogFragment dialog, int position, Router router, boolean error) {
        //Nothing to do here, as we are not updating routers from here!
    }

    @Override
    public boolean onNavigationItemSelected(final MenuItem menuItem) {
        final Integer position = navigationViewMenuItemsPositions.get(menuItem.getItemId());
        if (position != null && position >= 0) {
            menuItem.setChecked(true);
        }
        return (position != null && position >= 0);
    }

    public void selectItemInDrawer(int position) {
        mDrawerResult.setSelection(position, true);
    }

    public class NetworkChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {

            final NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (info != null && info.isConnected()) {
                final String routerName = mRouter.getName();
                final String effectiveRemoteAddr = Router.getEffectiveRemoteAddr(mRouter, DDWRTMainActivity.this);
                final Integer effectivePort = Router.getEffectivePort(mRouter, DDWRTMainActivity.this);

                setTitle(isNullOrEmpty(routerName) ? effectiveRemoteAddr : routerName);

                mTitle = mDrawerTitle = getTitle();
                if (mToolbar != null) {
                    mToolbar.setTitle(mTitle);
                    mToolbar.setSubtitle(isNullOrEmpty(routerName) ? ("SSH Port: " + effectivePort) :
                            (effectiveRemoteAddr + ":" + effectivePort));
                }
            }
        }
    }


}
