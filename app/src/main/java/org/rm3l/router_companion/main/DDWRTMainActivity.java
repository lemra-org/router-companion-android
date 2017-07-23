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

package org.rm3l.router_companion.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import cn.nekocode.resinspector.ResourceInspector;
import com.airbnb.deeplinkdispatch.DeepLink;
import com.cocosw.undobar.UndoBarController;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
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
import com.squareup.picasso.Picasso;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style;
import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HttpsURLConnection;
import needle.UiRelatedTask;
import org.json.JSONObject;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.about.AboutDialog;
import org.rm3l.router_companion.actions.ActionManager;
import org.rm3l.router_companion.actions.BackupRouterAction;
import org.rm3l.router_companion.actions.ClearARPCacheRouterAction;
import org.rm3l.router_companion.actions.ClearDNSCacheRouterAction;
import org.rm3l.router_companion.actions.ManageHTTPdRouterAction;
import org.rm3l.router_companion.actions.ManageRouterAliasesActivity;
import org.rm3l.router_companion.actions.RebootRouterAction;
import org.rm3l.router_companion.actions.RestoreRouterDefaultsAction;
import org.rm3l.router_companion.actions.RestoreRouterDialogFragment;
import org.rm3l.router_companion.actions.RouterAction;
import org.rm3l.router_companion.actions.RouterActionListener;
import org.rm3l.router_companion.actions.RouterActions;
import org.rm3l.router_companion.actions.RouterRestoreDialogListener;
import org.rm3l.router_companion.actions.activity.OpenWebManagementPageActivity;
import org.rm3l.router_companion.actions.activity.SpeedTestActivity;
import org.rm3l.router_companion.exceptions.DDWRTCompanionException;
import org.rm3l.router_companion.exceptions.StorageException;
import org.rm3l.router_companion.fragments.PageSlidingTabStripFragment;
import org.rm3l.router_companion.help.ChangelogActivity;
import org.rm3l.router_companion.help.HelpActivity;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.mgmt.RouterMgmtDialogListener;
import org.rm3l.router_companion.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.router_companion.mgmt.register.ManageRouterFragmentActivity;
import org.rm3l.router_companion.multithreading.MultiThreadingManager;
import org.rm3l.router_companion.prefs.sort.SortingStrategy;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.settings.RouterSettingsActivity;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.utils.AdUtils;
import org.rm3l.router_companion.utils.AppShortcutUtils;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.ReportingUtils;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.StorageUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.customtabs.CustomTabActivityHelper;
import org.rm3l.router_companion.utils.snackbar.SnackbarCallback;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.router_companion.RouterCompanionAppConstants.AUTO_REFRESH_INTERVAL_SECONDS_PREF;
import static org.rm3l.router_companion.RouterCompanionAppConstants.AUTO_REFRESH_PREF;
import static org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_THEME;
import static org.rm3l.router_companion.RouterCompanionAppConstants.EMPTY_STRING;
import static org.rm3l.router_companion.RouterCompanionAppConstants.MAX_ROUTERS_FREE_VERSION;
import static org.rm3l.router_companion.RouterCompanionAppConstants.SORTING_STRATEGY_PREF;
import static org.rm3l.router_companion.RouterCompanionAppConstants.THEMING_PREF;
import static org.rm3l.router_companion.RouterCompanionAppConstants.TILE_REFRESH_SECONDS;
import static org.rm3l.router_companion.mgmt.RouterManagementActivity.NEW_ROUTER_ADDED;
import static org.rm3l.router_companion.mgmt.RouterManagementActivity.ROUTER_SELECTED;
import static org.rm3l.router_companion.resources.conn.Router.RouterFirmware;
import static org.rm3l.router_companion.utils.Utils.fromHtml;
import static org.rm3l.router_companion.web.WebUtils.DO_NOT_VERIFY;
import static org.rm3l.router_companion.web.WebUtils.trustAllHosts;

//import com.mikepenz.materialdrawer.util.RecyclerViewCacheUtil;

/**
 * Main Android Activity
 * <p/>
 */
@DeepLink({
    "ddwrt://routers/{routerUuid}", "dd-wrt://routers/{routerUuid}"
}) public class DDWRTMainActivity extends AppCompatActivity
    implements ViewPager.OnPageChangeListener, UndoBarController.AdvancedUndoListener,
    RouterActionListener, RouterMgmtDialogListener, RouterRestoreDialogListener,
    NavigationView.OnNavigationItemSelectedListener, SnackbarCallback,
    CustomTabActivityHelper.ConnectionCallback {

  public static final String TAG = DDWRTMainActivity.class.getSimpleName();
  public static final String TAB_INDEX = "TAB_INDEX";
  public static final String SAVE_ITEM_SELECTED = "SAVE_ITEM_SELECTED";
  public static final String SAVE_ROUTER_SELECTED = "SAVE_ROUTER_SELECTED";
  public static final int ROUTER_SETTINGS_ACTIVITY_CODE = 1;
  public static final String IS_SORTING_STRATEGY_CHANGED = "isSortingStrategyChanged";
  public static final String ROUTER_ACTION = "ROUTER_ACTION";
  public static final String ADD_ROUTER_FRAGMENT_TAG = "add_router";
  public static final String RESTORE_ROUTER_FRAGMENT_TAG = "RESTORE_ROUTER_FRAGMENT_TAG";
  public static final String IMPORT_ALIASES_FRAGMENT_TAG = "IMPORT_ALIASES_FRAGMENT_TAG";
  public static final int ADD_NEW_ROUTER = 123;
  public static final int MANAGE_ROUTERS = 234;
  public static final String DRAWER_OPEN_FIRST_LAUNCH_PREF = "drawer_open_first_launch";
  public static final String MAIN_ACTIVITY_ACTION = "WirelessClientsTileAction";
  public static final String MAIN_ACTIVITY_ACTION_EXPORT_ALIASES_MAX_RETRIES =
      "MAIN_ACTIVITY_ACTION_EXPORT_ALIASES_MAX_RETRIES";
  public static final String MAIN_ACTIVITY_ACTION_EXPORT_ALIASES_NB_RETRIES =
      "MAIN_ACTIVITY_ACTION_EXPORT_ALIASES_NB_RETRIES";
  public static final String[] opts = new String[] {
      "w_300", "h_300", "q_100", "c_thumb", "g_center", "r_20", "e_improve", "e_make_transparent",
      "e_trim"
  };
  private static final int LISTENED_REQUEST_CODE = 77;
  private static final BiMap<Integer, Integer> navigationViewMenuItemsPositions =
      HashBiMap.create(11);
  private static final Uri BASE_APP_URI = Uri.parse(
      "android-app://" + BuildConfig.APPLICATION_ID + "/http/ddwrt-companion.rm3l.org/routers");

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

  private Toolbar mToolbar;
  @NonNull private DDWRTCompanionDAO dao;
  @NonNull private String mRouterUuid;
  private Menu optionsMenu;
  private CharSequence mDrawerTitle;
  private CharSequence mTitle;
  private int mPosition = 0;
  private int mTabPosition = 0;
  private String mCurrentSortingStrategy;
  private long mCurrentSyncInterval;
  private boolean mWithAutoRefresh;
  @NonNull private SharedPreferences mGlobalPreferences;
  @NonNull private SharedPreferences mPreferences;
  @NonNull private Router mRouter;
  private final Runnable mDestroySessionRunnable = new Runnable() {
    @Override public void run() {
      SSHUtils.destroySessions(mRouter);
    }
  };
  private DDWRTTile.ActivityResultListener mCurrentActivityResultListener;
  private AccountHeader mDrawerHeaderResult;
  private Drawer mDrawerResult;
  @Nullable private InterstitialAd mInterstitialAd;
  private boolean mIsThemeLight;
  private BroadcastReceiver mMessageReceiver;
  private ViewPager mViewPager;
  private TabLayout mTabLayout;
  private GoogleApiClient mClient;
  private CustomTabActivityHelper mCustomTabActivityHelper;

  @Override
  protected void attachBaseContext(Context newBase) {
    super.attachBaseContext(Utils.getBaseContextToAttach(this, newBase));
  }

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mClient = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

    //SQLite
    this.dao = RouterManagementActivity.getDao(this);
    final Intent intent = getIntent();
    String uuid = null;
    if (intent.getBooleanExtra(DeepLink.IS_DEEP_LINK, false)) {
      //Deep link
      final Bundle parameters = intent.getExtras();
      uuid = parameters.getString("routerUuid");
    } else {
      //            final String action = intent.getAction();
      //            final String data = intent.getDataString();
      //            if (Intent.ACTION_VIEW.equals(action) && data != null) {
      //                uuid = data.substring(data.lastIndexOf("/") + 1);
      //            } else {
      //                uuid = intent.getStringExtra(ROUTER_SELECTED);
      //                if (uuid == null) {
      //                    if (savedInstanceState != null) {
      //                        uuid = savedInstanceState.getString(SAVE_ROUTER_SELECTED);
      //                    }
      //                }
      //            }
      uuid = intent.getStringExtra(ROUTER_SELECTED);
      if (uuid == null) {
        if (savedInstanceState != null) {
          uuid = savedInstanceState.getString(SAVE_ROUTER_SELECTED);
        }
        if (uuid == null) {
          final String action = intent.getAction();
          final String data = intent.getDataString();
          if (Intent.ACTION_VIEW.equals(action) && data != null) {
            uuid = data.substring(data.lastIndexOf("/") + 1);
          }
        }
      }
    }

    final Router router = this.dao.getRouter(uuid);

    if (router == null) {
      Toast.makeText(this, "No router set or router no longer exists", Toast.LENGTH_LONG).show();
      finish();
      return;
    }

    //#199: report app shortcut
    AppShortcutUtils.reportShortcutUsed(this, uuid);

    // Connect your client
    mClient.connect();

    // Define a title for your current page, shown in autocompletion UI
    final String TITLE = (router.getName() + " (" + router.getRemoteIpAddress() + ")");
    final Uri APP_URI = BASE_APP_URI.buildUpon().appendPath(router.getUuid()).build();

    final Action viewAction = Action.newAction(Action.TYPE_VIEW, TITLE, APP_URI);

    // Call the App Indexing API view method
    PendingResult<Status> result = AppIndex.AppIndexApi.start(mClient, viewAction);

    result.setResultCallback(new ResultCallback<Status>() {
      @Override public void onResult(Status status) {
        if (status.isSuccess()) {
          Log.d(TAG, "App Indexing API: Recorded recipe "
              + router.getCanonicalHumanReadableName()
              + " view successfully.");
        } else {
          Log.e(TAG, "App Indexing API: There was an error recording the recipe view."
              + status.toString());
        }
      }
    });

    //        Utils.requestAppPermissions(this);

    //Report
    final Map<String, Object> eventMap = new HashMap<>();
    eventMap.put("Model", Utils.isDemoRouter(router) ? RouterCompanionAppConstants.DEMO
        : Router.getRouterModel(this, router));
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
    this.mGlobalPreferences =
        this.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);

    //Load from Shared Preferences
    this.mCurrentSortingStrategy = mPreferences.getString(SORTING_STRATEGY_PREF, "");
    this.mCurrentSyncInterval = mPreferences.getLong(AUTO_REFRESH_INTERVAL_SECONDS_PREF, -10l);
    this.mWithAutoRefresh = mPreferences.getBoolean(AUTO_REFRESH_PREF, false);

    mIsThemeLight = ColorUtils.Companion.isThemeLight(this);
    ColorUtils.Companion.setAppTheme(this, mRouter.getRouterFirmware(), true);
    //        if (mIsThemeLight) {
    //            //Light
    //            setTheme(R.style.AppThemeLight_StatusBarTransparent);
    //        } else {
    //            //Default is Dark
    //            setTheme(R.style.AppThemeDark_StatusBarTransparent);
    //        }

    // Inherit theme for router - this is for SettingsActivity,
    // because we are overriding the getSharedPreferences() method
    final long mCurrentTheme =
        this.mGlobalPreferences.getLong(THEMING_PREF, RouterCompanionAppConstants.DEFAULT_THEME);
    mPreferences.edit().putLong(THEMING_PREF, mCurrentTheme).apply();

    setContentView(R.layout.activity_main);

    mInterstitialAd = AdUtils.requestNewInterstitial(this,
        R.string.interstitial_ad_unit_id_router_list_to_router_main);

    setupCustomTabHelper(this);
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
        final ProfileDrawerItem profileDrawerItem =
            new ProfileDrawerItem().withIdentifier(routerFromAllId)
                .withNameShown(true)
                .withName(routerFromAll.getDisplayName())
                .withEmail(
                    routerFromAll.getRemoteIpAddress() + ":" + routerFromAll.getRemotePort());
        final String routerModel = Router.getRouterModel(this, routerFromAll);
        Crashlytics.log(Log.DEBUG, TAG, "routerModel: " + routerModel);
        if (!(isNullOrEmpty(routerModel) || "-".equalsIgnoreCase(routerModel))) {
          if (Utils.isDemoRouter(routerFromAll)) {
            profileDrawerItem.withIcon(R.drawable.demo_router);
          } else {
            final String routerModelNormalized = routerModel.toLowerCase().replaceAll("\\s+", "");
            try {
              final String url =
                  String.format("%s/%s/%s", RouterCompanionAppConstants.IMAGE_CDN_URL_PREFIX,
                      Joiner.on(",").skipNulls().join(opts),
                      URLEncoder.encode(routerModelNormalized, Charsets.UTF_8.name()));
              profileDrawerItem.withIcon(url);
            } catch (UnsupportedEncodingException e) {
              e.printStackTrace();
              ReportingUtils.reportException(DDWRTMainActivity.this, e);
            }
          }
        } else {
          profileDrawerItem.withIcon(
              Utils.isDemoRouter(routerFromAll) ? R.drawable.demo_router : R.drawable.router);
        }

        iProfiles[i++] = profileDrawerItem;
        if (mRouter.getUuid().equals(routerFromAll.getUuid())) {
          activeProfile = profileDrawerItem;
        }
      }
    } else {
      iProfiles = new IProfile[2];
    }
    iProfiles[iProfiles.length - 2] =
        //don't ask but google uses 14dp for the add account icon in gmail but 20dp for the normal icons (like manage account)
        new ProfileSettingDrawerItem().withName("Add Router")
            .withDescription("Add new Router")
            .withIcon(R.drawable.ic_action_content_add_drawer_header)
            .withIdentifier(ADD_NEW_ROUTER)
            .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
              @Override
              public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                openAddRouterForm();
                return true;
              }
            });
    iProfiles[iProfiles.length - 1] =
        //don't ask but google uses 14dp for the add account icon in gmail but 20dp for the normal icons (like manage account)
        new ProfileSettingDrawerItem().withName("Manage Routers")
            .withDescription("Manage registered Routers")
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
    final int drawerHeaderDrawable =
        (routerFirmware != null ? routerFirmware.drawerHeaderBackgroundDrawable
            : R.drawable.router_picker_background);
    mDrawerHeaderResult = new AccountHeaderBuilder().withActivity(this)
        .addProfiles(iProfiles)
        .withSavedInstance(savedInstanceState)
        .withHeaderBackground(drawerHeaderDrawable)
        .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
          @Override public boolean onProfileChanged(View view, IProfile profile, boolean current) {
            if (profile == null) {
              ReportingUtils.reportException(DDWRTMainActivity.this,
                  new IllegalStateException("routerUuid is NULL or empty"));
              Toast.makeText(DDWRTMainActivity.this, "Internal Error - please try again later",
                  Toast.LENGTH_SHORT).show();
              return false;
            }
            final long profileIdentifier = profile.getIdentifier();
            Crashlytics.log(Log.DEBUG, TAG, "OnAccountHeaderListener: " + profileIdentifier);
            if (profileIdentifier == ADD_NEW_ROUTER || profileIdentifier == MANAGE_ROUTERS) {
              //Already handled
              return true;
            }
            final Router daoRouter = dao.getRouter(Long.valueOf(profileIdentifier).intValue());

            if (daoRouter == null) {
              Toast.makeText(DDWRTMainActivity.this, "Internal Error - please try again later",
                  Toast.LENGTH_SHORT).show();
              ReportingUtils.reportException(DDWRTMainActivity.this,
                  new IllegalStateException("daoRouter NOT found"));
              return false;
            }

            final String routerUuid = daoRouter.getUuid();
            if (routerUuid.isEmpty()) {
              Toast.makeText(DDWRTMainActivity.this, "Internal Error - please try again later",
                  Toast.LENGTH_SHORT).show();
              ReportingUtils.reportException(DDWRTMainActivity.this,
                  new IllegalStateException("routerUuid is NULL or empty"));
              return false;
            }

            //Switch Router
            final Intent intent = getIntent();
            intent.putExtra(ROUTER_SELECTED, routerUuid);
            intent.putExtra(SAVE_ITEM_SELECTED, mPosition);
            intent.putExtra(TAB_INDEX, mTabPosition);

            if (BuildConfig.WITH_ADS && mInterstitialAd != null && AdUtils.canDisplayInterstialAd(
                DDWRTMainActivity.this)) {

              mInterstitialAd.setAdListener(new AdListener() {
                @Override public void onAdClosed() {
                  finish();
                  startActivity(intent);
                }

                @Override public void onAdOpened() {
                  //Save preference
                  mGlobalPreferences.edit()
                      .putLong(RouterCompanionAppConstants.AD_LAST_INTERSTITIAL_PREF,
                          System.currentTimeMillis())
                      .apply();
                }
              });

              if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
              } else {
                //Reload UI
                final ProgressDialog alertDialog =
                    ProgressDialog.show(DDWRTMainActivity.this, "Switching Routers",
                        "Please wait...", true);
                new Handler().postDelayed(new Runnable() {
                  @Override public void run() {
                    finish();
                    startActivity(intent);
                    alertDialog.cancel();
                  }
                }, 2000);
              }
            } else {
              //Reload UI
              final ProgressDialog alertDialog =
                  ProgressDialog.show(DDWRTMainActivity.this, "Switching Routers", "Please wait...",
                      true);
              new Handler().postDelayed(new Runnable() {
                @Override public void run() {
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

    mDrawerResult = new DrawerBuilder().withActivity(this)
        .withTranslucentStatusBar(false)
        .withActionBarDrawerToggle(true)
        .withSavedInstance(savedInstanceState)
        .withToolbar(mToolbar)
        .withAccountHeader(
            mDrawerHeaderResult) //set the AccountHeader we created earlier for the header
        .withSavedInstance(savedInstanceState)
        .withShowDrawerOnFirstLaunch(false)
        .withCloseOnClick(true)
        .addDrawerItems(new PrimaryDrawerItem().withName("Dashboard")
                .withIcon(mIsThemeLight ? R.drawable.ic_dashboard_black_24dp
                    : R.drawable.ic_dashboard_white_24dp)
                .withIdentifier(1),

            new SectionDrawerItem().withName("Status").withSelectable(false),
            new PrimaryDrawerItem().withName("Status")
                .withIcon(
                    mIsThemeLight ? R.drawable.ic_info_black_24dp : R.drawable.ic_info_white_24dp)
                .withIdentifier(2), new PrimaryDrawerItem().withName("Wireless")
                .withIcon(mIsThemeLight ? R.drawable.ic_network_wifi_black_24dp
                    : R.drawable.ic_network_wifi_white_24dp)
                .withIdentifier(3), new PrimaryDrawerItem().withName("Clients")
                .withIcon(mIsThemeLight ? R.drawable.ic_devices_black_24dp
                    : R.drawable.ic_devices_white_24dp)
                .withIdentifier(4), new PrimaryDrawerItem().withName("Monitoring")
                .withIcon(mIsThemeLight ? R.drawable.ic_timeline_black_24dp
                    : R.drawable.ic_timeline_white_24dp)
                .withIdentifier(5),

            new SectionDrawerItem().withName("Services").withSelectable(false),
            new PrimaryDrawerItem().withName("OpenVPN")
                .withIcon(mIsThemeLight ? R.drawable.ic_openvpn_black : R.drawable.ic_openvpn_white)
                .withIdentifier(7), new PrimaryDrawerItem().withName("PPTP")
                .withIcon(mIsThemeLight ? R.drawable.ic_vpn_key_black_24dp
                    : R.drawable.ic_vpn_key_white_24dp)
                .withIdentifier(8), new PrimaryDrawerItem().withName("Wake On LAN")
                .withIcon(mIsThemeLight ? R.drawable.ic_settings_power_black_24dp
                    : R.drawable.ic_settings_power_white_24dp)
                .withIdentifier(9),

            new SectionDrawerItem().withName("Admin Area").withSelectable(false),
            new PrimaryDrawerItem().withName(getResources().getString(R.string.access_restrictions))
                .withIcon(
                    mIsThemeLight ? R.drawable.ic_block_black_24dp : R.drawable.ic_block_white_24dp)
                .withIdentifier(11), new PrimaryDrawerItem().withName("Commands")
                .withIcon(
                    mIsThemeLight ? R.drawable.ic_action_cli_black : R.drawable.ic_action_cli_white)
                .withIdentifier(12), new PrimaryDrawerItem().withName("NVRAM")
                .withIcon(mIsThemeLight ? R.drawable.ic_memory_black_24dp
                    : R.drawable.ic_memory_white_24dp)
                .withIdentifier(13),

            new SectionDrawerItem().withName("Toolbox").withSelectable(false),
            new PrimaryDrawerItem().withName("Network")
                .withIcon(mIsThemeLight ? R.drawable.ic_device_hub_black_24dp
                    : R.drawable.ic_device_hub_white_24dp)
                .withIdentifier(15))
        .addStickyDrawerItems(new PrimaryDrawerItem().withName("Help")
                .withIcon(mIsThemeLight ? R.drawable.ic_help_outline_black_24dp
                    : R.drawable.ic_help_outline_white_24dp)
                .withIdentifier(1001),
            //                        new PrimaryDrawerItem().withName("What's new").withIcon(R.drawable.ic_action_action_info_outline).withIdentifier(1002),
            new PrimaryDrawerItem().withName("Send Feedback")
                .withIcon(mIsThemeLight ? R.drawable.ic_feedback_black_24dp
                    : R.drawable.ic_feedback_white_24dp)
                .withIdentifier(1003)
            //                        new PrimaryDrawerItem().withName("About").withIcon(R.drawable.ic_action_social_person).withIdentifier(1004)
        )
        .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
          @Override public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
            // do something with the clicked item :D
            if (drawerItem == null) {
              ReportingUtils.reportException(DDWRTMainActivity.this,
                  new IllegalStateException("drawerItem == null"));
              return false;
            }
            final int identifier = Long.valueOf(drawerItem.getIdentifier()).intValue();
            if (identifier < 0) {
              ReportingUtils.reportException(DDWRTMainActivity.this,
                  new IllegalStateException("identifier < 0"));
              return false;
            }
            if (identifier < 1000) {
              selectItem(identifier, 0);
            } else {
              switch (identifier) {
                case 1001:
                  //Help
                  CustomTabActivityHelper.openCustomTab(DDWRTMainActivity.this, null,
                      RouterCompanionAppConstants.REMOTE_HELP_WEBSITE, mRouterUuid, null,
                      new CustomTabActivityHelper.CustomTabFallback() {
                        @Override public void openUri(Activity activity, Uri uri) {
                          activity.startActivity(
                              new Intent(DDWRTMainActivity.this, HelpActivity.class));
                        }
                      }, false);
                  break;
                case 1002:
                  //Changelog
                  CustomTabActivityHelper.openCustomTab(DDWRTMainActivity.this, null,
                      RouterCompanionAppConstants.REMOTE_HELP_WEBSITE_CHANGELOG, mRouterUuid, null,
                      new CustomTabActivityHelper.CustomTabFallback() {
                        @Override public void openUri(Activity activity, Uri uri) {
                          activity.startActivity(
                              new Intent(DDWRTMainActivity.this, ChangelogActivity.class));
                        }
                      }, false);
                  break;
                case 1003:
                  //Close drawer and open feedback activity
                  if (mDrawerResult.isDrawerOpen()) {
                    mDrawerResult.closeDrawer();
                  }
                  //Feedback
                  Utils.openFeedbackForm(DDWRTMainActivity.this, mRouter);
                  //                                    final Intent intent = new Intent(DDWRTMainActivity.this, FeedbackActivity.class);
                  //                                    intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouterUuid);
                  //                                    final File screenshotFile = new File(getCacheDir(), "feedback_screenshot.png");
                  //                                    ViewGroupUtils.exportViewToFile(DDWRTMainActivity.this, getWindow().getDecorView(), screenshotFile);
                  //                                    intent.putExtra(FeedbackActivity.SCREENSHOT_FILE, screenshotFile.getAbsolutePath());
                  //                                    intent.putExtra(FeedbackActivity.CALLER_ACTIVITY, this.getClass().getCanonicalName());
                  //                                    startActivity(intent);
                  ////                                    Utils.buildFeedbackDialog(DDWRTMainActivity.this, true);
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
    //        RecyclerViewCacheUtil.getInstance().withCacheSize(3).init(mDrawerResult);

    final Integer savedPosition;
    int position = intent.getIntExtra(SAVE_ITEM_SELECTED, 1);
    if (savedInstanceState != null
        && (savedPosition = savedInstanceState.getInt(SAVE_ITEM_SELECTED)) != null) {
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

    final boolean drawerOpenFirstLaunch =
        mGlobalPreferences.getBoolean(DRAWER_OPEN_FIRST_LAUNCH_PREF, false);
    if (!drawerOpenFirstLaunch) {
      if (!mDrawerResult.isDrawerOpen()) {
        mDrawerResult.openDrawer();
      }
      mGlobalPreferences.edit().putBoolean(DRAWER_OPEN_FIRST_LAUNCH_PREF, true).apply();
    }

    final Resources resources = getResources();

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
        editor.putLong(AUTO_REFRESH_INTERVAL_SECONDS_PREF, TILE_REFRESH_SECONDS);
      }
      if (putDefaultTheme) {
        editor.putLong(THEMING_PREF, DEFAULT_THEME);
      }
      editor.apply();
    }

    Utils.displayRatingBarIfNeeded(this);
  }

  private void setupCustomTabHelper(final CustomTabActivityHelper.ConnectionCallback cb) {
    mCustomTabActivityHelper = new CustomTabActivityHelper();
    mCustomTabActivityHelper.setConnectionCallback(cb);
    mCustomTabActivityHelper.mayLaunchUrl(
        Uri.parse(RouterCompanionAppConstants.REMOTE_HELP_WEBSITE), null, null);
    mCustomTabActivityHelper.mayLaunchUrl(
        Uri.parse(RouterCompanionAppConstants.REMOTE_HELP_WEBSITE_CHANGELOG), null, null);
  }

  @Override protected void onResume() {
    super.onResume();
    try {
      registerReceiver(mMessageReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    } catch (final Exception e) {
      Utils.reportException(this, e);
      e.printStackTrace();
    }
  }

  @Override protected void onStart() {
    super.onStart();
    mCustomTabActivityHelper.bindCustomTabsService(this);
  }

  @Override protected void onStop() {
    try {
      unregisterReceiver(mMessageReceiver);
    } catch (final Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (mRouter != null) {
          final String TITLE = mRouter.getName();
          final Uri APP_URI = BASE_APP_URI.buildUpon().appendPath(mRouter.getUuid()).build();

          Action viewAction = Action.newAction(Action.TYPE_VIEW, TITLE, APP_URI);
          PendingResult<Status> result = AppIndex.AppIndexApi.end(mClient, viewAction);

          result.setResultCallback(new ResultCallback<Status>() {
            @Override public void onResult(Status status) {
              if (status.isSuccess()) {
                Log.d(TAG, "App Indexing API: Recorded recipe "
                    + mRouter.getCanonicalHumanReadableName()
                    + " view end successfully.");
              } else {
                Log.e(TAG, "App Indexing API: There was an error recording the recipe view."
                    + status.toString());
              }
            }
          });

          mClient.disconnect();
        }
      } catch (final Exception e) {
        e.printStackTrace();
      } finally {
        super.onStop();
        mCustomTabActivityHelper.unbindCustomTabsService(this);
      }
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
            final int colorForOpenWrt = ContextCompat.getColor(this, R.color.win8_orange);
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
      mToolbar.setSubtitle(isNullOrEmpty(routerName) ? ("SSH Port: " + effectivePort)
          : (effectiveRemoteAddr + ":" + effectivePort));
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

    startActivityForResult(new Intent(this, ManageRouterFragmentActivity.class), NEW_ROUTER_ADDED);

    //        final DialogFragment addFragment = new RouterAddDialogFragment();
    //        addFragment.show(getSupportFragmentManager(), ADD_ROUTER_FRAGMENT_TAG);
  }

  @Override public void onBackPressed() {
    //handle the back press :D close the drawer first and if the drawer is closed close the activity
    if (mDrawerResult != null && mDrawerResult.isDrawerOpen()) {
      mDrawerResult.closeDrawer();
    } else {
      super.onBackPressed();
    }
  }

  @Override protected void onSaveInstanceState(Bundle savedInstanceState) {
    savedInstanceState.putInt(SAVE_ITEM_SELECTED, mPosition);
    savedInstanceState.putString(SAVE_ROUTER_SELECTED, mRouterUuid);
    savedInstanceState.putInt(TAB_INDEX, mTabPosition);
    super.onSaveInstanceState(savedInstanceState);
  }

  @Override protected void onPause() {
    //Close SSH Session as well
    destroySSHSession();
    super.onPause();
  }

  @Override protected void onDestroy() {
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

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    this.optionsMenu = menu;

    //TODO If pref is not available, hide "Open Web Interface" menu

    getMenuInflater().inflate(R.menu.main_menu, menu);
    if (!BuildConfig.DONATIONS) {
      final MenuItem item = menu.findItem(R.id.action_donate);
      if (item != null) {
        item.setVisible(false);
      }
    }

    if (!BuildConfig.FLAVOR.toLowerCase().contains("google")) {
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

    final MenuItem item = menu.findItem(R.id.action_ddwrt_actions_restore_factory_defaults);
    if (item != null) {
      //FIXME Command used to restore factory defaults works best on DD-WRT, not on OpenWRT and other firmwares
      // So hide this menu item until we find a better way to achieve this!
      item.setVisible(RouterFirmware.DDWRT.equals(mRouter.getRouterFirmware()));
    }

    return super.onCreateOptionsMenu(menu);
  }

  @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {

    final String displayName = mRouter.getDisplayName();

    final Map<String, Object> eventMap = new HashMap<>();

    switch (item.getItemId()) {

      case android.R.id.home:
        onBackPressed();
        return true;

      case R.id.close:
        finish();
        return true;

      case R.id.help:
        CustomTabActivityHelper.openCustomTab(DDWRTMainActivity.this, null,
            RouterCompanionAppConstants.REMOTE_HELP_WEBSITE, mRouterUuid, null,
            new CustomTabActivityHelper.CustomTabFallback() {
              @Override public void openUri(Activity activity, Uri uri) {
                activity.startActivity(new Intent(DDWRTMainActivity.this, HelpActivity.class));
              }
            }, false);
        eventMap.put("Menu Item", "Help");
        ReportingUtils.reportEvent(ReportingUtils.EVENT_MENU_ITEM, eventMap);
        return true;
      case R.id.changelog:
        CustomTabActivityHelper.openCustomTab(DDWRTMainActivity.this, null,
            RouterCompanionAppConstants.REMOTE_HELP_WEBSITE_CHANGELOG, mRouterUuid, null,
            new CustomTabActivityHelper.CustomTabFallback() {
              @Override public void openUri(Activity activity, Uri uri) {
                activity.startActivity(new Intent(DDWRTMainActivity.this, ChangelogActivity.class));
              }
            }, false);
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
        Utils.openFeedbackForm(this, mRouter);
        //                final Intent intent = new Intent(DDWRTMainActivity.this, FeedbackActivity.class);
        //                intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouterUuid);
        //                final File screenshotFile = new File(getCacheDir(), "feedback_screenshot.png");
        //                ViewGroupUtils.exportViewToFile(DDWRTMainActivity.this, getWindow().getDecorView(), screenshotFile);
        //                intent.putExtra(FeedbackActivity.SCREENSHOT_FILE, screenshotFile.getAbsolutePath());
        //                intent.putExtra(FeedbackActivity.CALLER_ACTIVITY, this.getClass().getCanonicalName());
        //                startActivity(intent);
        ////                Utils.buildFeedbackDialog(this, true);
        return true;
      case R.id.action_remove_ads:
        Utils.displayUpgradeMessageForAdsRemoval(this);
        return true;
      case R.id.action_ddwrt_actions_open_webinterface: {
        final ProgressDialog alertDialog =
            ProgressDialog.show(this, "Looking for an appropriate IP Address", "Please wait...",
                true);
        MultiThreadingManager.getWebTasksExecutor().execute(new UiRelatedTask<String>() {

          private String mRealm;

          private boolean canConnect(@NonNull final String urlStr) {
            Crashlytics.log(Log.DEBUG, TAG, "--> Trying GET '" + urlStr + "'");
            HttpURLConnection urlConnection = null;
            try {
              final URL url = new URL(urlStr + "/Management.asp");
              if (url.getProtocol().toLowerCase().equals("https")) {
                trustAllHosts();
                final HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
                https.setHostnameVerifier(DO_NOT_VERIFY);
                urlConnection = https;
              } else {
                urlConnection = (HttpURLConnection) url.openConnection();
              }
              //FIXME Add a user-preference
              urlConnection.setConnectTimeout(5000);
              final int statusCode = urlConnection.getResponseCode();
              String wwwAuthenticateHeaderField = urlConnection.getHeaderField("WWW-Authenticate");
              if (wwwAuthenticateHeaderField != null) {
                final List<String> stringList =
                    Splitter.on("=").omitEmptyStrings().splitToList(wwwAuthenticateHeaderField);
                if (stringList.size() >= 2) {
                  final String realm = stringList.get(0);
                  if (realm != null) {
                    mRealm = realm.replaceAll("\"", "").replaceAll("'", "");
                  }
                }
              }
              Crashlytics.log(Log.DEBUG, TAG, "GET " + urlStr + " : " + statusCode);
              return true;
            } catch (Exception e) {
              e.printStackTrace();
              Crashlytics.log(Log.DEBUG, TAG, "Didn't succeed in GET'ing " + urlStr);
              return false;
            } finally {
              if (urlConnection != null) {
                urlConnection.disconnect();
              }
            }
          }

          @Override protected String doWork() {
            try {
              //Give time to the ProgressDialog to show up
              Thread.sleep(TimeUnit.SECONDS.toMillis(3));
              final NVRAMInfo nvRamInfoFromRouter =
                  SSHUtils.getNVRamInfoFromRouter(DDWRTMainActivity.this, mRouter,
                      getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE),
                      NVRAMInfo.Companion.getLAN_IPADDR(), NVRAMInfo.Companion.getWAN_IPADDR(),
                      NVRAMInfo.Companion.getHTTP_LANPORT(), NVRAMInfo.Companion.getHTTP_WANPORT(),
                      NVRAMInfo.Companion.getHTTP_USERNAME(), NVRAMInfo.Companion.getHTTP_PASSWD(),
                      NVRAMInfo.Companion.getHTTPS_ENABLE(), NVRAMInfo.Companion.getREMOTE_MGT_HTTPS());

              if (nvRamInfoFromRouter == null || nvRamInfoFromRouter.isEmpty()) {
                throw new DDWRTCompanionException("Unable to retrieve info about HTTPd service");
              }

              String lanUrl = "http";
              String wanUrl = "http";
              final String lanIpAddr = nvRamInfoFromRouter.getProperty(NVRAMInfo.Companion.getLAN_IPADDR(), EMPTY_STRING);
              final String lanPort = nvRamInfoFromRouter.getProperty(NVRAMInfo.Companion.getHTTP_LANPORT(), EMPTY_STRING);

              final String wanIpAddr = nvRamInfoFromRouter.getProperty(NVRAMInfo.Companion.getWAN_IPADDR(), EMPTY_STRING);
              final String wanPort = nvRamInfoFromRouter.getProperty(NVRAMInfo.Companion.getHTTP_WANPORT(), EMPTY_STRING);

              if ("1".equals(nvRamInfoFromRouter.getProperty(NVRAMInfo.Companion.getHTTPS_ENABLE()))) {
                lanUrl += "s";
              }
              if ("1".equals(nvRamInfoFromRouter.getProperty(NVRAMInfo.Companion.getREMOTE_MGT_HTTPS()))) {
                wanUrl += "s";
              }
              lanUrl += ("://" + lanIpAddr + (TextUtils.isEmpty(lanPort) ? "" : (":" + lanPort)));
              wanUrl += ("://" + wanIpAddr + (TextUtils.isEmpty(wanPort) ? "" : (":" + wanPort)));

              String mUrl = null;

              if (canConnect(lanUrl)) {
                mUrl = lanUrl;
              } else if (canConnect(wanUrl)) {
                mUrl = wanUrl;
              } else {
                //Try with router IP / DNS
                String urlFromRouterRemoteIpOrDns = "http";
                final String remoteIpAddress = mRouter.getRemoteIpAddress();
                if ("1".equals(nvRamInfoFromRouter.getProperty(NVRAMInfo.Companion.getHTTPS_ENABLE()))) {
                  urlFromRouterRemoteIpOrDns += "s";
                }
                urlFromRouterRemoteIpOrDns +=
                    ("://" + remoteIpAddress + (TextUtils.isEmpty(lanPort) ? "" : (":" + lanPort)));
                if (canConnect(urlFromRouterRemoteIpOrDns)) {
                  mUrl = urlFromRouterRemoteIpOrDns;
                } else {
                  //WAN
                  urlFromRouterRemoteIpOrDns = "http";
                  if ("1".equals(nvRamInfoFromRouter.getProperty(NVRAMInfo.Companion.getREMOTE_MGT_HTTPS()))) {
                    urlFromRouterRemoteIpOrDns += "s";
                  }
                  urlFromRouterRemoteIpOrDns +=
                      ("://" + remoteIpAddress + (TextUtils.isEmpty(wanPort) ? ""
                          : (":" + wanPort)));
                  if (canConnect(urlFromRouterRemoteIpOrDns)) {
                    mUrl = urlFromRouterRemoteIpOrDns;
                  } else {
                    //TODO Maybe display dialog where user can explicitly provide the information
                    throw new DDWRTCompanionException("Could not connect to router");
                  }
                }
              }
              return mUrl;
            } catch (final Exception e) {
              e.printStackTrace();
              return null;
            }
          }

          @Override protected void thenDoUiRelatedWork(@Nullable final String url) {
            alertDialog.cancel();
            if (TextUtils.isEmpty(url)) {
              Toast.makeText(DDWRTMainActivity.this,
                  "Unable to determine an IP address for opening the Web Management Interface",
                  Toast.LENGTH_SHORT).show();
              return;
            }

            CustomTabActivityHelper.openCustomTab(DDWRTMainActivity.this, null, url, mRouterUuid,
                null, new CustomTabActivityHelper.CustomTabFallback() {
                  @Override public void openUri(Activity activity, Uri uri) {
                    //Otherwise, default to a classic WebView implementation
                    final Intent webManagementIntent =
                        new Intent(DDWRTMainActivity.this, OpenWebManagementPageActivity.class);
                    webManagementIntent.putExtra(ROUTER_SELECTED,
                        DDWRTMainActivity.this.mRouterUuid);
                    webManagementIntent.putExtra(OpenWebManagementPageActivity.URL_TO_OPEN, url);
                    activity.startActivity(webManagementIntent);
                  }
                }, false);
          }
        });
      }
      return true;
      case R.id.main_add_home_shortcut: {
        mRouter.addHomeScreenShortcut(this);
      }
      return true;

      case R.id.action_ddwrt_actions_ssh_router: {
        Router.openSSHConsole(mRouter, this);
      }
      return true;

      case R.id.action_ddwrt_actions_reboot_router:

        new AlertDialog.Builder(this).setIcon(R.drawable.ic_action_alert_warning)
            .setTitle(String.format("Reboot '%s' (%s)", displayName, mRouter.getRemoteIpAddress()))
            .setMessage(String.format(
                "Are you sure you wish to continue?\n'%s' (%s) will be rebooted, "
                    + "and you might have to wait some time before connection is re-established.",
                displayName, mRouter.getRemoteIpAddress()))
            .setCancelable(true)
            .setPositiveButton("Proceed!", new DialogInterface.OnClickListener() {
              @Override public void onClick(final DialogInterface dialogInterface, final int i) {
                final Bundle token = new Bundle();
                token.putString(ROUTER_ACTION, RouterAction.REBOOT.name());

                new UndoBarController.UndoBar(DDWRTMainActivity.this).message(
                    String.format("Router '%s' (%s) will be rebooted", displayName,
                        mRouter.getRemoteIpAddress()))
                    .listener(DDWRTMainActivity.this)
                    .token(token)
                    .show();
              }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
              @Override public void onClick(DialogInterface dialogInterface, int i) {
                //Cancelled - nothing more to do!
              }
            })
            .create()
            .show();

        return true;
      case R.id.action_ddwrt_actions_restore_factory_defaults:
        if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
          //Download the full version to unlock this version
          Utils.displayUpgradeMessage(this, "Restore Factory Defaults");
          return true;
        }

        new AlertDialog.Builder(this).setIcon(R.drawable.ic_action_alert_warning)
            .setTitle(String.format("Reset '%s' (%s)", displayName, mRouter.getRemoteIpAddress()))
            .setMessage(String.format("Are you sure you wish to continue?\n"
                    + "This will erase the entire NVRAM, thus resetting all settings back to factory defaults. "
                    + "All of your settings will be erased and '%s' will be rebooted. "
                    + "You might have to wait some time before connection is re-established.\n\n"
                    + "[CAUTION]\n"
                    + "- Make sure to *backup* your settings first!!!\n"
                    + "- After resetting DD-WRT, you need to login with the default user name \"root\" and default password \"admin\".\n"
                    + "- Some devices may not boot properly after being reset. In this case, you will have to reflash them.",
                displayName))
            .setCancelable(true)
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
              @Override public void onClick(DialogInterface dialogInterface, int i) {
                //Cancelled - nothing more to do!
              }
            })
            .setNeutralButton("*Backup*", new DialogInterface.OnClickListener() {
              @Override public void onClick(DialogInterface dialogInterface, int i) {
                displayBackupDialog(displayName);
              }
            })
            .setPositiveButton("Got it!\nProceed!", new DialogInterface.OnClickListener() {
              @Override public void onClick(final DialogInterface dialogInterface, final int i) {

                //For reporting
                ReportingUtils.reportException(DDWRTMainActivity.this,
                    new RestoreRouterDefaultsAction.AgreementToResetRouter(DDWRTMainActivity.this));

                final Bundle token = new Bundle();
                token.putString(ROUTER_ACTION, RouterAction.RESTORE_FACTORY_DEFAULTS.name());

                new UndoBarController.UndoBar(DDWRTMainActivity.this).message(
                    String.format("Router '%s' (%s) will be reset", displayName,
                        mRouter.getRemoteIpAddress()))
                    .listener(DDWRTMainActivity.this)
                    .token(token)
                    .show();
              }
            })
            .create()
            .show();

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

        final Fragment restoreRouter =
            getSupportFragmentManager().findFragmentByTag(RESTORE_ROUTER_FRAGMENT_TAG);
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
      case R.id.action_ddwrt_actions_speedtest: {
        final Intent speedTestIntent = new Intent(this, SpeedTestActivity.class);
        speedTestIntent.putExtra(ROUTER_SELECTED, this.mRouterUuid);
        this.startActivity(speedTestIntent);
      }
      return true;
      case R.id.action_ddwrt_actions_manage_aliases: {
        final Intent manageAliasIntent = new Intent(this, ManageRouterAliasesActivity.class);
        manageAliasIntent.putExtra(ROUTER_SELECTED, this.mRouterUuid);
        this.startActivity(manageAliasIntent);
      }
      return true;

      case R.id.action_ddwrt_actions_clear_arp_cache: {
        SnackbarUtils.buildSnackbar(this,
            String.format("ARP Cache will be flushed on '%s' (%s)...", mRouter.getDisplayName(),
                mRouter.getRemoteIpAddress()), "Undo", Snackbar.LENGTH_SHORT,
            new SnackbarCallback() {
              @Override public void onShowEvent(@Nullable Bundle bundle) throws Exception {

              }

              @Override public void onDismissEventSwipe(int event, @Nullable Bundle bundle)
                  throws Exception {

              }

              @Override public void onDismissEventActionClick(int event, @Nullable Bundle bundle)
                  throws Exception {

              }

              @Override public void onDismissEventTimeout(int event, @Nullable Bundle bundle)
                  throws Exception {
                ActionManager.runTasks(
                    new ClearARPCacheRouterAction(mRouter, DDWRTMainActivity.this,
                        DDWRTMainActivity.this, mGlobalPreferences));
              }

              @Override public void onDismissEventManual(int event, @Nullable Bundle bundle)
                  throws Exception {

              }

              @Override public void onDismissEventConsecutive(int event, @Nullable Bundle bundle)
                  throws Exception {

              }
            }, null, true);
      }
      return true;

      case R.id.action_ddwrt_actions_clear_dns_cache: {
        SnackbarUtils.buildSnackbar(this,
            String.format("DNS Cache will be flushed on '%s' (%s)...", mRouter.getDisplayName(),
                mRouter.getRemoteIpAddress()), "Undo", Snackbar.LENGTH_SHORT,
            new SnackbarCallback() {
              @Override public void onShowEvent(@Nullable Bundle bundle) throws Exception {

              }

              @Override public void onDismissEventSwipe(int event, @Nullable Bundle bundle)
                  throws Exception {

              }

              @Override public void onDismissEventActionClick(int event, @Nullable Bundle bundle)
                  throws Exception {

              }

              @Override public void onDismissEventTimeout(int event, @Nullable Bundle bundle)
                  throws Exception {
                ActionManager.runTasks(
                    new ClearDNSCacheRouterAction(mRouter, DDWRTMainActivity.this,
                        DDWRTMainActivity.this, mGlobalPreferences));
              }

              @Override public void onDismissEventManual(int event, @Nullable Bundle bundle)
                  throws Exception {

              }

              @Override public void onDismissEventConsecutive(int event, @Nullable Bundle bundle)
                  throws Exception {

              }
            }, null, true);
      }
      return true;
      case R.id.action_ddwrt_actions_httpd_start:
      case R.id.action_ddwrt_actions_httpd_stop:
      case R.id.action_ddwrt_actions_httpd_restart: {
        final int action;
        final String actionMessage;
        switch (item.getItemId()) {
          case R.id.action_ddwrt_actions_httpd_start:
            action = ManageHTTPdRouterAction.START;
            actionMessage = "started";
            break;
          case R.id.action_ddwrt_actions_httpd_stop:
            action = ManageHTTPdRouterAction.STOP;
            actionMessage = "stopped";
            break;
          default:
            action = ManageHTTPdRouterAction.RESTART;
            actionMessage = "restarted";
            break;
        }
        SnackbarUtils.buildSnackbar(this,
            String.format("Web Server (httpd) will be %s on '%s' (%s)...", actionMessage,
                mRouter.getDisplayName(), mRouter.getRemoteIpAddress()), "Undo",
            Snackbar.LENGTH_SHORT, new SnackbarCallback() {
              @Override public void onShowEvent(@Nullable Bundle bundle) throws Exception {

              }

              @Override public void onDismissEventSwipe(int event, @Nullable Bundle bundle)
                  throws Exception {

              }

              @Override public void onDismissEventActionClick(int event, @Nullable Bundle bundle)
                  throws Exception {

              }

              @Override public void onDismissEventTimeout(int event, @Nullable Bundle bundle)
                  throws Exception {
                ActionManager.runTasks(new ManageHTTPdRouterAction(mRouter, DDWRTMainActivity.this,
                    DDWRTMainActivity.this, mGlobalPreferences, action));
              }

              @Override public void onDismissEventManual(int event, @Nullable Bundle bundle)
                  throws Exception {

              }

              @Override public void onDismissEventConsecutive(int event, @Nullable Bundle bundle)
                  throws Exception {

              }
            }, null, true);
      }
      return true;
      default:
        break;
    }

    return true;
  }

  public void displayBackupDialog(final String displayName) {
    new AlertDialog.Builder(this).setIcon(R.drawable.ic_action_alert_warning)
        .setTitle(String.format("Backup '%s' (%s)", displayName, mRouter.getRemoteIpAddress()))
        .setMessage(String.format(
            "You may backup the current configuration in case you need to reset "
                + "the router back to its factory default settings.\n\n"
                + "Click the \"Backup\" button to download the configuration backup file of "
                + "'%s' (%s) to this device.\n"
                + "You will be able to share the file once the operation is done.", displayName,
            mRouter.getRemoteIpAddress()))
        .setCancelable(true)
        .setPositiveButton("Backup!", new DialogInterface.OnClickListener() {
          @Override public void onClick(final DialogInterface dialogInterface, final int i) {
            final Bundle token = new Bundle();
            token.putString(ROUTER_ACTION, RouterAction.BACKUP.name());

            new UndoBarController.UndoBar(DDWRTMainActivity.this).message(
                String.format("Backup of Router '%s' (%s) is going to start...", displayName,
                    mRouter.getRemoteIpAddress()))
                .listener(DDWRTMainActivity.this)
                .token(token)
                .show();
          }
        })
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialogInterface, int i) {
            //Cancelled - nothing more to do!
          }
        })
        .create()
        .show();
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    Crashlytics.log(Log.DEBUG, TAG,
        "onActivityResult(" + requestCode + "," + resultCode + "," + data);
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
          final SharedPreferences mPreferences =
              this.getSharedPreferences(this.mRouterUuid, Context.MODE_PRIVATE);
          final long prefSyncIntervalMillis =
              mPreferences.getLong(AUTO_REFRESH_INTERVAL_SECONDS_PREF, -1l);
          final String prefSortingStrategy = mPreferences.getString(SORTING_STRATEGY_PREF, "");
          final boolean withAutoRefresh = mPreferences.getBoolean(AUTO_REFRESH_PREF, false);
          if (this.mWithAutoRefresh != withAutoRefresh
              || this.mCurrentSyncInterval != prefSyncIntervalMillis
              || !this.mCurrentSortingStrategy.equals(prefSortingStrategy)) {
            //Reload UI
            final AlertDialog alertDialog =
                Utils.buildAlertDialog(this, null, "Reloading UI...", false, false);
            alertDialog.show();
            ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(
                Gravity.CENTER_HORIZONTAL);
            new Handler().postDelayed(new Runnable() {
              @Override public void run() {
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
      case NEW_ROUTER_ADDED: {
        final String newRouterUuid;
        Router newRouter = null;
        final boolean error = (data == null
            || (newRouterUuid = data.getStringExtra(RouterManagementActivity.ROUTER_SELECTED))
            == null
            || (newRouter = dao.getRouter(newRouterUuid)) == null);
        onRouterAdd(null, newRouter, error);
      }
      break;
    }

    super.onActivityResult(requestCode, resultCode, data);
  }

  private void setUpViewPager() {
    mViewPager = (ViewPager) findViewById(R.id.tabanim_viewpager);
    mTabLayout = (TabLayout) findViewById(R.id.tabanim_tabs);
    mTabLayout.setSelectedTabIndicatorColor(
        ColorUtils.Companion.getColor(DDWRTMainActivity.class.getCanonicalName()));

    mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout) {

      @Override public void onPageSelected(int position) {
        super.onPageSelected(position);
        mTabPosition = position;
      }
    });
  }

  private void selectItem(int position) {
    selectItem(position, 0);
  }

  private void selectItem(int position, int tabPosition) {

    Crashlytics.log(Log.DEBUG, TAG, "selectItem @" + position);
    if (position < 0) {
      return;
    }
    this.mPosition = position;
    this.mTabPosition = tabPosition;

    final PageSlidingTabStripFragment.FragmentTabsAdapter fragmentTabsAdapter =
        new PageSlidingTabStripFragment.FragmentTabsAdapter(this, this.mPosition,
            getSupportFragmentManager(), getResources(),
            String.format("%s.%s", SortingStrategy.class.getPackage().getName(),
                getSharedPreferences(mRouterUuid, Context.MODE_PRIVATE).getString(
                    RouterCompanionAppConstants.SORTING_STRATEGY_PREF, SortingStrategy.DEFAULT)),
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
   * @param position Position index of the first page currently being displayed.
   * Page position+1 will be visible if positionOffset is nonzero.
   * @param positionOffset Value from [0, 1) indicating the offset from the page at position.
   * @param positionOffsetPixels Value in pixels indicating the offset from position.
   */
  @Override public void onPageScrolled(int position, float positionOffset,
      int positionOffsetPixels) {
    Crashlytics.log(Log.DEBUG, TAG, "onPageScrolled @" + position);
  }

  /**
   * This method will be invoked when a new page becomes selected. Animation is not
   * necessarily complete.
   *
   * @param position Position index of the new selected page.
   */
  @Override public void onPageSelected(int position) {
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
  @Override public void onPageScrollStateChanged(int state) {
  }

  @Override public void onUndo(@android.support.annotation.Nullable Parcelable parcelable) {
    //Nothing to do
  }

  @Override public void onHide(@android.support.annotation.Nullable Parcelable parcelable) {
    if (parcelable instanceof Bundle) {
      final Bundle token = (Bundle) parcelable;
      final String routerAction = token.getString(ROUTER_ACTION);
      Crashlytics.log(Log.DEBUG, TAG, "routerAction: [" + routerAction + "]");
      if (isNullOrEmpty(routerAction)) {
        return;
      }
      try {
        switch (RouterAction.valueOf(routerAction)) {
          case REBOOT:
            ActionManager.runTasks(new RebootRouterAction(mRouter, this, this, mGlobalPreferences));
            break;
          case RESTORE_FACTORY_DEFAULTS:
            ActionManager.runTasks(
                new RestoreRouterDefaultsAction(mRouter, this, this, mGlobalPreferences));
            break;
          case UPGRADE_FIRMWARE:
            //TODO
            break;
          case BACKUP:
            final AlertDialog alertDialog = Utils.
                buildAlertDialog(DDWRTMainActivity.this, null, "Backing up - please hold on...",
                    false, false);
            alertDialog.show();
            ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(
                Gravity.CENTER_HORIZONTAL);
            ActionManager.runTasks(
                new BackupRouterAction(mRouter, DDWRTMainActivity.this, new RouterActionListener() {
                  @Override public void onRouterActionSuccess(@NonNull RouterAction routerAction,
                      @NonNull Router router, Object returnData) {
                    try {
                      String msg;
                      if (!((returnData instanceof Object[])
                          && ((Object[]) returnData).length >= 2)) {
                        msg = String.format("Action '%s' executed "
                                + "successfully on host '%s', but an internal error occurred. "
                                + "The issue will be reported. Please try again later.",
                            routerAction.toString(), router.getRemoteIpAddress());
                        Utils.displayMessage(DDWRTMainActivity.this, msg, Style.INFO);
                        ReportingUtils.reportException(DDWRTMainActivity.this,
                            new IllegalStateException(msg));
                        return;
                      }

                      final Object[] returnDataObjectArray = ((Object[]) returnData);
                      final Object backupDateObject = returnDataObjectArray[0];
                      final Object localBackupFileObject = returnDataObjectArray[1];

                      if (!((backupDateObject instanceof Date)
                          && (localBackupFileObject instanceof File))) {
                        msg = String.format("Action '%s' executed "
                                + "successfully on host '%s', but could not determine where "
                                + "local backup file has been saved. Please try again later.",
                            routerAction.toString(), router.getRemoteIpAddress());
                        Utils.displayMessage(DDWRTMainActivity.this, msg, Style.INFO);
                        ReportingUtils.reportException(DDWRTMainActivity.this,
                            new IllegalStateException(msg));
                        return;
                      }

                      Utils.displayMessage(DDWRTMainActivity.this, String.format(
                          "Action '%s' executed successfully on host '%s'. "
                              + "Now loading the file sharing activity chooser...",
                          routerAction.toString(), router.getRemoteIpAddress()), Style.CONFIRM);

                      final File localBackupFile = (File) (((Object[]) returnData)[1]);
                      final Date backupDate = (Date) (((Object[]) returnData)[0]);

                      final Uri uriForFile = FileProvider.getUriForFile(DDWRTMainActivity.this,
                          RouterCompanionAppConstants.FILEPROVIDER_AUTHORITY, localBackupFile);
                      grantUriPermission(getPackageName(), uriForFile,
                          Intent.FLAG_GRANT_READ_URI_PERMISSION);

                      final Intent shareIntent = new Intent();
                      shareIntent.setAction(Intent.ACTION_SEND);
                      shareIntent.putExtra(Intent.EXTRA_SUBJECT,
                          String.format("Backup of Router '%s'",
                              mRouter.getCanonicalHumanReadableName()));
                      shareIntent.setType("text/html");
                      shareIntent.putExtra(Intent.EXTRA_TEXT, fromHtml(("Backup Date: "
                          + backupDate
                          + "\n\n"
                          + "You may restore your router later using this Backup Configuration file.\n"
                          + "Restoring can be performed either via the 'DD-WRT Companion' app, or using "
                          + "the Web Management Interface.\n"
                          + "Restoring will overwrite all current configurations "
                          + "with the ones in this backup file.\n"
                          + "\n"
                          + "Please also note that you must only "
                          + "restore configurations with files "
                          + "backed up using the same firmware and "
                          + "the same model of router.\n\n\n").replaceAll("\n", "<br/>")
                          + Utils.getShareIntentFooter()));
                      shareIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);
                      //                                            shareIntent.setType("*/*");
                      startActivity(Intent.createChooser(shareIntent,
                          getResources().getText(R.string.share_backup)));
                    } finally {
                      runOnUiThread(new Runnable() {
                        @Override public void run() {
                          alertDialog.cancel();
                        }
                      });
                    }
                  }

                  @Override public void onRouterActionFailure(@NonNull RouterAction routerAction,
                      @NonNull Router router, @Nullable Exception exception) {
                    try {
                      Utils.displayMessage(DDWRTMainActivity.this,
                          String.format("Error on action '%s': %s", routerAction.toString(),
                              Utils.handleException(exception).first), Style.ALERT);
                    } finally {
                      runOnUiThread(new Runnable() {
                        @Override public void run() {
                          alertDialog.cancel();
                        }
                      });
                    }
                  }
                }, mGlobalPreferences));
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

  @Override public void onClear(@NonNull Parcelable[] parcelables) {
    //Nothing to do
  }

  @Override
  public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router,
      Object returnData) {
    Utils.displayMessage(this,
        String.format("Action '%s' executed successfully on host '%s'", routerAction.toString(),
            router.getRemoteIpAddress()), Style.CONFIRM);
  }

  @Override
  public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull Router router,
      @Nullable Exception exception) {
    Utils.displayMessage(this, String.format("Error on action '%s': %s", routerAction.toString(),
        Utils.handleException(exception).first), Style.ALERT);
  }

  public void startActivityForResult(Intent intent, DDWRTTile.ActivityResultListener listener) {

    if (mCurrentActivityResultListener != null) {
      Crashlytics.log(Log.ERROR, TAG,
          "Activity trying to start more than one activity at a time...");
      return;
    }
    mCurrentActivityResultListener = listener;
    startActivityForResult(intent, LISTENED_REQUEST_CODE);
  }

  @Override public void onRouterAdd(final DialogFragment dialog, final Router newRouter,
      final boolean error) {
    if (!error) {

      final int newRouterId = newRouter.getId();

      final IProfile newProfile = new ProfileDrawerItem().withIdentifier(newRouterId)
          .withNameShown(true)
          .withName(newRouter.getDisplayName())
          .withEmail(newRouter.getRemoteIpAddress() + ":" + newRouter.getRemotePort());

      final String routerModel = Router.getRouterModel(this, newRouter);
      Crashlytics.log(Log.DEBUG, TAG, "routerModel: " + routerModel);
      if (!(isNullOrEmpty(routerModel) || "-".equalsIgnoreCase(routerModel))) {
        if (Utils.isDemoRouter(newRouter)) {
          newProfile.withIcon(R.drawable.demo_router);
        } else {
          final String routerModelNormalized = routerModel.toLowerCase().replaceAll("\\s+", "");
          try {
            final String url =
                String.format("%s/%s/%s", RouterCompanionAppConstants.IMAGE_CDN_URL_PREFIX,
                    Joiner.on(",").skipNulls().join(opts),
                    URLEncoder.encode(routerModelNormalized, Charsets.UTF_8.name()));
            newProfile.withIcon(url);
          } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            ReportingUtils.reportException(DDWRTMainActivity.this, e);
          }
        }
      } else {
        newProfile.withIcon(
            Utils.isDemoRouter(newRouter) ? R.drawable.demo_router : R.drawable.router);
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

  @Override public boolean onNavigationItemSelected(final MenuItem menuItem) {
    final Integer position = navigationViewMenuItemsPositions.get(menuItem.getItemId());
    if (position != null && position >= 0) {
      menuItem.setChecked(true);
    }
    return (position != null && position >= 0);
  }

  public void selectItemInDrawer(int position) {
    mDrawerResult.setSelection(position, true);
  }

  @Override public void onShowEvent(@Nullable Bundle bundle) throws Exception {

  }

  @Override public void onDismissEventSwipe(int event, @Nullable Bundle bundle) throws Exception {

  }

  @Override public void onDismissEventActionClick(int event, @Nullable Bundle bundle)
      throws Exception {

  }

  @Override public void onDismissEventTimeout(int event, @Nullable Bundle bundle) throws Exception {

    final Integer action = bundle != null ? bundle.getInt(MAIN_ACTIVITY_ACTION) : null;

    if (action == null) {
      return;
    }

    switch (action) {
      case RouterActions.IMPORT_ALIASES:
        //TODO
        Toast.makeText(this, "[TODO] Import aliases", Toast.LENGTH_SHORT).show();
        break;
      case RouterActions.EXPORT_ALIASES:
        //Load all aliases from preferences
        if (mPreferences == null) {
          break;
        }
        final Map<String, ?> allRouterPrefs = mPreferences.getAll();
        if (allRouterPrefs == null || allRouterPrefs.isEmpty()) {
          return;
        }

        final Map<String, String> aliases = new HashMap<>();
        for (final Map.Entry<String, ?> entry : allRouterPrefs.entrySet()) {
          final String key = entry.getKey();
          final Object value = entry.getValue();
          if (isNullOrEmpty(key) || value == null) {
            continue;
          }
          //Check whether key is a MAC-Address
          if (!Utils.MAC_ADDRESS.matcher(key).matches()) {
            continue;
          }
          //This is a MAC Address - collect it right away!
          aliases.put(key, nullToEmpty(value.toString()));
        }

        final File exportDirectory = StorageUtils.getExportDirectory(this);
        if (exportDirectory == null) {
          throw new StorageException("Could not retrieve or create export directory!");
        }

        final File aliasesDir = new File(exportDirectory, "alias");

        StorageUtils.createDirectoryOrRaiseException(aliasesDir);

        final File outputFile = new File(aliasesDir, Utils.getEscapedFileName(
            String.format("Aliases_for_%s_%s_%s", mRouter.getDisplayName(),
                mRouter.getRemoteIpAddress(), mRouter.getUuid())) + ".json");

        final Date backupDate = new Date();
        final String aliasesStr = new JSONObject(aliases).toString(2);

        FileOutputStream fileOutputStream = null;
        try {
          fileOutputStream = new FileOutputStream(outputFile);
          fileOutputStream.write(aliasesStr.getBytes());
        } finally {
          if (fileOutputStream != null) {
            fileOutputStream.close();
          }
        }

        Utils.displayMessage(this, String.format(
            "Action 'Export Aliases' executed successfully on host '%s'. " + "Path: '%s'",
            mRouter.getRemoteIpAddress(), outputFile.getAbsolutePath()), Style.CONFIRM);

        try {
          //Now allow user to share file if needed
          final Uri uriForFile =
              FileProvider.getUriForFile(this, RouterCompanionAppConstants.FILEPROVIDER_AUTHORITY,
                  outputFile);
          this.grantUriPermission(this.getPackageName(), uriForFile,
              Intent.FLAG_GRANT_READ_URI_PERMISSION);

          final Intent shareIntent = new Intent();
          shareIntent.setAction(Intent.ACTION_SEND);
          shareIntent.putExtra(Intent.EXTRA_SUBJECT, String.format("Aliases Backup for Router '%s'",
              mRouter.getCanonicalHumanReadableName()));
          shareIntent.setType("text/html");
          shareIntent.putExtra(Intent.EXTRA_TEXT, fromHtml(
              ("Backup Date: " + backupDate + "\n\n" + aliasesStr + "\n\n\n").replaceAll("\n",
                  "<br/>") + Utils.getShareIntentFooter()));
          shareIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);
          //                                            shareIntent.setType("*/*");
          this.startActivity(Intent.createChooser(shareIntent,
              this.getResources().getText(R.string.share_backup)));
        } catch (final Exception e) {
          e.printStackTrace();
          Utils.reportException(this, e);
          //No worries
        }

        break;
      default:
        //Ignored
        break;
    }
  }

  @Override public void onDismissEventManual(int event, @Nullable Bundle bundle) throws Exception {

  }

  @Override public void onDismissEventConsecutive(int event, @Nullable Bundle bundle)
      throws Exception {

  }

  @Override public void onCustomTabsConnected() {
    //We may make UI changes
    Crashlytics.log(Log.DEBUG, TAG, "onCustomTabsConnected");
  }

  @Override public void onCustomTabsDisconnected() {
    //We may make UI changes
    Crashlytics.log(Log.DEBUG, TAG, "onCustomTabsDisconnected");
  }

  @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {

    switch (requestCode) {
      case RouterCompanionAppConstants.Permissions.STORAGE: {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          // permission was granted, yay!
          Crashlytics.log(Log.DEBUG, TAG, "Yay! Permission granted for #" + requestCode);
        } else {
          // permission denied, boo! Disable the
          // functionality that depends on this permission.
          Crashlytics.log(Log.WARN, TAG, "Boo! Permission denied for #" + requestCode);
          Utils.displayMessage(this, "Data caching will be unavailable", Style.INFO);
        }
        return;
      }
      //TODO Add other codes down here
      default:
        break;
    }
  }

  public class NetworkChangeReceiver extends BroadcastReceiver {

    @Override public void onReceive(final Context context, final Intent intent) {

      final NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
      if (info != null && info.isConnected()) {
        final String routerName = mRouter.getName();
        final String effectiveRemoteAddr =
            Router.getEffectiveRemoteAddr(mRouter, DDWRTMainActivity.this);
        final Integer effectivePort = Router.getEffectivePort(mRouter, DDWRTMainActivity.this);

        setTitle(isNullOrEmpty(routerName) ? effectiveRemoteAddr : routerName);

        mTitle = mDrawerTitle = getTitle();
        if (mToolbar != null) {
          mToolbar.setTitle(mTitle);
          mToolbar.setSubtitle(isNullOrEmpty(routerName) ? ("SSH Port: " + effectivePort)
              : (effectiveRemoteAddr + ":" + effectivePort));
        }
      }
    }
  }
}
