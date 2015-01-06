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

package org.rm3l.ddwrt;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.cocosw.undobar.UndoBarController;
import com.suredigit.inappfeedback.FeedbackDialog;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.about.AboutDialog;
import org.rm3l.ddwrt.actions.RebootRouterAction;
import org.rm3l.ddwrt.actions.RestoreRouterDefaultsAction;
import org.rm3l.ddwrt.actions.RouterAction;
import org.rm3l.ddwrt.actions.RouterActionListener;
import org.rm3l.ddwrt.feedback.SendFeedbackDialog;
import org.rm3l.ddwrt.fragments.PageSlidingTabStripFragment;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.prefs.sort.SortingStrategy;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.settings.RouterSettingsActivity;
import org.rm3l.ddwrt.utils.Utils;

import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_THEME;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.SORTING_STRATEGY_PREF;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.SYNC_INTERVAL_MILLIS_PREF;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.THEMING_PREF;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.TILE_REFRESH_MILLIS;


/**
 * Main Android Activity
 * <p/>
 */
public class DDWRTMainActivity extends SherlockFragmentActivity
        implements ViewPager.OnPageChangeListener, UndoBarController.AdvancedUndoListener, RouterActionListener {

    public static final String TAG = DDWRTMainActivity.class.getSimpleName();
    public static final String SAVE_ITEM_SELECTED = "SAVE_ITEM_SELECTED";
    public static final int ROUTER_SETTINGS_ACTIVITY_CODE = 1;
    public static final String IS_SORTING_STRATEGY_CHANGED = "isSortingStrategyChanged";
    public static final String ROUTER_ACTION = "ROUTER_ACTION";

    DrawerLayout mDrawerLayout;
    ListView mDrawerList;
    ActionBarDrawerToggle mDrawerToggle;
    @NotNull
    private DDWRTCompanionDAO dao;
    @NotNull
    private String mRouterUuid;
    private Menu optionsMenu;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private String[] mDDWRTNavigationMenuSections;
    //    private PageSlidingTabStripFragment currentPageSlidingTabStripFragment;
    private int mPosition = 0;

    private FeedbackDialog mFeedbackDialog;

    private String mCurrentSortingStrategy;

    private long mCurrentSyncInterval;

    private long mCurrentTheme;

    @NotNull
    private SharedPreferences mPreferences;

    @NotNull
    private Router mRouter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //SQLite
        this.dao = RouterManagementActivity.getDao(this);
        final Router router = this.dao.getRouter(getIntent().getStringExtra(RouterManagementActivity.ROUTER_SELECTED));

        if (router == null) {
            Toast.makeText(this, "No router set or router no longer exists", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        final String routerName = router.getName();
        setTitle(isNullOrEmpty(routerName) ? router.getRemoteIpAddress() : routerName);

        this.mRouterUuid = router.getUuid();
        this.mRouter = router;

        this.mPreferences = this.getSharedPreferences(this.mRouterUuid, Context.MODE_PRIVATE);

        mTitle = mDrawerTitle = getTitle();
        mDDWRTNavigationMenuSections = getResources().getStringArray(R.array.navigation_drawer_items_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // set a custom shadow that overlays the main content when the drawer
        // opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
                GravityCompat.START);
        // set up the drawer's list view with items and click listener
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mDDWRTNavigationMenuSections));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
                mDrawerLayout, /* DrawerLayout object */
                R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
                R.string.navigation_drawer_open, /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to
                // onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to
                // onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        final Integer savedPosition;
        int position = 0;
        if (savedInstanceState != null && (savedPosition = savedInstanceState.getInt(SAVE_ITEM_SELECTED)) != null) {
            position = savedPosition;
        }
        mDrawerList.performItemClick(
                mDrawerList.getChildAt(position),
                position,
                mDrawerList.getAdapter().getItemId(position));

        mFeedbackDialog = new SendFeedbackDialog(this).getFeedbackDialog();

        //Load from Shared Preferences
        this.mCurrentSortingStrategy = this.mPreferences.getString(SORTING_STRATEGY_PREF,"");
        this.mCurrentSyncInterval = this.mPreferences.getLong(SYNC_INTERVAL_MILLIS_PREF, -1l);
        this.mCurrentTheme = this.mPreferences.getLong(THEMING_PREF, -1l);

        //Recreate Default Preferences if they are no longer available
        final boolean putDefaultSortingStrategy = isNullOrEmpty(this.mCurrentSortingStrategy);
        final boolean putDefaultSyncInterval = (this.mCurrentSyncInterval <= 0l);
        final boolean putDefaultTheme = (this.mCurrentTheme <= 0l);
        if (putDefaultSortingStrategy || putDefaultSyncInterval || putDefaultTheme) {
            //Add default preferences values
            final SharedPreferences.Editor editor = this.mPreferences.edit();
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
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(SAVE_ITEM_SELECTED, mPosition);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mFeedbackDialog != null) {
            mFeedbackDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mFeedbackDialog != null) {
            mFeedbackDialog.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        this.optionsMenu = menu;
        getSupportMenuInflater().inflate(R.menu.main_menu, menu);
        if (!BuildConfig.DONATIONS) {
            final MenuItem item = menu.findItem(R.id.action_donate);
            if (item != null) {
                item.setVisible(false);
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(
            @NotNull com.actionbarsherlock.view.MenuItem item) {

        final String routerName = mRouter.getName();
        final String displayName = isNullOrEmpty(routerName) ? mRouter.getRemoteIpAddress() : routerName;

        switch (item.getItemId()) {

            case android.R.id.home: {
                if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
                    mDrawerLayout.closeDrawer(mDrawerList);
                } else {
                    mDrawerLayout.openDrawer(mDrawerList);
                }
                return true;
            }
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

            case R.id.action_settings:
                //Open Settings activity for this item
                final Intent ddWrtMainIntent = new Intent(this, RouterSettingsActivity.class);
                ddWrtMainIntent.putExtra(RouterManagementActivity.ROUTER_SELECTED, this.mRouterUuid);
                this.startActivityForResult(ddWrtMainIntent, ROUTER_SETTINGS_ACTIVITY_CODE);
                return true;
            case R.id.action_donate:
                Utils.openDonateActivity(this);
                return true;
            case R.id.action_about:
                new AboutDialog(this).show();
                return true;
            case R.id.action_feedback:
                if (mFeedbackDialog == null) {
                    mFeedbackDialog = new SendFeedbackDialog(this).getFeedbackDialog();
                }
                mFeedbackDialog.show();
                return true;
            case R.id.action_ddwrt_actions_reboot_router:

                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.ic_action_alert_warning)
                        .setTitle(String.format("Reboot '%s'", displayName))
                        .setMessage(String.format("Are you sure you wish to continue?\n'%s' will be rebooted, " +
                                "and you might have to wait some time before connection is re-established.", displayName))
                        .setCancelable(true)
                        .setPositiveButton("Proceed!", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialogInterface, final int i) {
                                final Bundle token = new Bundle();
                                token.putString(ROUTER_ACTION, RouterAction.REBOOT.toString());

                                new UndoBarController.UndoBar(DDWRTMainActivity.this)
                                    .message(String.format("Router '%s' will be rebooted",
                                            displayName))
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

                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.ic_action_alert_warning)
                        .setTitle(String.format("Reset '%s'", displayName))
                        .setMessage(String.format("Are you sure you wish to continue?\n" +
                                "This will erase the entire NVRAM, thus resetting all settings back to factory defaults. " +
                                "All of your settings will be erased and '%s' will be rebooted. " +
                                "You might have to wait some time before connection is re-established.\n\n" +
                                "[CAUTION]\n" +
                                "- After resetting DD-WRT, you need to login with the default user name \"root\" and default password \"admin\".\n" +
                                "- Some devices may not boot properly after being reset. In this case, you will have to reflash them.", displayName))
                        .setCancelable(true)
                        .setPositiveButton("I understand and I wish to proceed!", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialogInterface, final int i) {
                                final Bundle token = new Bundle();
                                token.putString(ROUTER_ACTION, RouterAction.RESTORE_FACTORY_DEFAULTS.toString());

                                new UndoBarController.UndoBar(DDWRTMainActivity.this)
                                        .message(String.format("Router '%s' will be reset",
                                                displayName))
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
            case R.id.action_ddwrt_actions_firmware_upgrade:
                //TODO
                return true;
            default:
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == ROUTER_SETTINGS_ACTIVITY_CODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                if (this.mCurrentSyncInterval != this.mPreferences.getLong(SYNC_INTERVAL_MILLIS_PREF,-1l) ||
                        this.mCurrentTheme != this.mPreferences.getLong(THEMING_PREF, -1l) ||
                        !this.mCurrentSortingStrategy
                        .equals(this.mPreferences.getString(SORTING_STRATEGY_PREF,""))) {
                    //Reload UI
                    final AlertDialog alertDialog = Utils.buildAlertDialog(this, null, "Reloading UI...", false, false);
                    alertDialog.show();
                    ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                            startActivity(getIntent());
                            alertDialog.cancel();
                        }
                    }, 2000);
                }
            }
        }
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

        mDrawerLayout.closeDrawer(mDrawerList);
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
                        new RebootRouterAction(this).execute(mRouter);
                        break;
                    case RESTORE_FACTORY_DEFAULTS:
                        new RestoreRouterDefaultsAction(this).execute(mRouter);
                        break;
                    case UPGRADE_FIRMWARE:
                        //TODO
                        break;
                    default:
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
    public void onRouterActionSuccess(@NotNull RouterAction routerAction, @NotNull Router router) {
        Utils.displayMessage(this,
                String.format("Action '%s' executed successfully on host '%s'", routerAction.toString().toLowerCase(), router.getRemoteIpAddress()),
                Style.CONFIRM);
    }

    @Override
    public void onRouterActionFailure(@NotNull RouterAction routerAction, @NotNull Router router, @Nullable Exception exception) {
        Utils.displayMessage(this,
                String.format("Error on action '%s': %s", routerAction.toString().toLowerCase(), ExceptionUtils.getRootCauseMessage(exception)),
                Style.ALERT);
    }

    // The click listener for ListView in the navigation drawer
    private class DrawerItemClickListener implements
            ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            selectItem(position);
        }
    }

}
