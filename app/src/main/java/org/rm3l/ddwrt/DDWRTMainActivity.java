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

package org.rm3l.ddwrt;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
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
import com.suredigit.inappfeedback.FeedbackDialog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.about.AboutDialog;
import org.rm3l.ddwrt.feedback.SendFeedbackDialog;
import org.rm3l.ddwrt.fragments.PageSlidingTabStripFragment;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.prefs.sort.DDWRTSortingStrategy;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.settings.RouterSettingsActivity;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.Utils;

import java.util.List;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Main Android Activity
 * <p/>
 */
public class DDWRTMainActivity extends SherlockFragmentActivity implements ViewPager.OnPageChangeListener {

    public static final String TAG = DDWRTMainActivity.class.getSimpleName();
    public static final String SAVE_ITEM_SELECTED = "SAVE_ITEM_SELECTED";
    public static final int ROUTER_SETTINGS_ACTIVITY_CODE = 1;
    public static final String IS_SORTING_STRATEGY_CHANGED = "isSortingStrategyChanged";

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

    @NotNull
    private SharedPreferences mPreferences;

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
        this.mCurrentSortingStrategy = this.mPreferences.getString(DDWRTCompanionConstants.SORTING_STRATEGY_PREF,"");
        this.mCurrentSyncInterval = this.mPreferences.getLong(DDWRTCompanionConstants.SYNC_INTERVAL_MILLIS_PREF, -1l);
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(SAVE_ITEM_SELECTED, mPosition);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mFeedbackDialog.dismiss();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFeedbackDialog.dismiss();
    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        this.optionsMenu = menu;
        getSupportMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(
            @NotNull com.actionbarsherlock.view.MenuItem item) {

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
                mFeedbackDialog.show();
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
                if (this.mCurrentSyncInterval != this.mPreferences.getLong(DDWRTCompanionConstants.SYNC_INTERVAL_MILLIS_PREF,-1l) ||
                        !this.mCurrentSortingStrategy
                        .equals(this.mPreferences.getString(DDWRTCompanionConstants.SORTING_STRATEGY_PREF,""))) {
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
