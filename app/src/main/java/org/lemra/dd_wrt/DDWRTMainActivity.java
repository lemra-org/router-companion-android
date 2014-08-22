package org.lemra.dd_wrt;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import org.lemra.dd_wrt.api.conn.Router;
import org.lemra.dd_wrt.fragments.PageSlidingTabStripFragment;

/**
 * Created by armel on 8/9/14.
 */
public class DDWRTMainActivity extends SherlockFragmentActivity {

    public static final Handler HANDLER = new Handler();

    public static final String TAG = DDWRTMainActivity.class.getSimpleName();
    //TESTS
    private static final Router router = new Router();
    static {
        router.setRemoteIpAddress("172.17.17.1");
        router.setUsername("root");
        router.setName("@home");
        router.setRouterConnectionProtocol(Router.RouterConnectionProtocol.SSH);
//        router.setPassword("2315netgearOrange");
        router.setPrivKey("-----\n" +
                "M\n" +
                "a/K\n" +
                "H\n" +
                "G\n" +
                "5\n" +
                "C\n" +
                "V\n" +
                "4\n" +
                "7\n" +
                "J\n" +
                "------");
        router.setStrictHostKeyChecking(false);
    }
    DrawerLayout mDrawerLayout;
    ListView mDrawerList;
    ActionBarDrawerToggle mDrawerToggle;
    private Menu optionsMenu;
    private SharedPreferences preferences;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private String[] mDDWRTNavigationMenuSections;
    private PageSlidingTabStripFragment currentPageSlidingTabStripFragment;
    //END TESTS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(TAG, Context.MODE_PRIVATE);
        setContentView(R.layout.activity_main);

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

        if (savedInstanceState == null) {
            selectItem(0);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        this.optionsMenu = menu;
        getSupportMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(
            com.actionbarsherlock.view.MenuItem item) {

        switch (item.getItemId()) {

            case android.R.id.home: {
                if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
                    mDrawerLayout.closeDrawer(mDrawerList);
                } else {
                    mDrawerLayout.openDrawer(mDrawerList);
                }
                break;
            }
            case R.id.action_refresh:
                Toast.makeText(this.getApplicationContext(), "Hold on. Refresh in progress...", Toast.LENGTH_SHORT).show();
                //Refresh all tiles currently visible
                setRefreshActionButtonState(true);
                HANDLER.post(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            //Request refresh of current tab tiles
                            if (DDWRTMainActivity.this.currentPageSlidingTabStripFragment != null) {
                                DDWRTMainActivity.this.currentPageSlidingTabStripFragment.refreshCurrentFragment();
                            }

                        } finally {
                            //Finish by resetting flag to false
                            setRefreshActionButtonState(false);
                        }
                    }
                });

            case R.id.action_settings:
                //TODO Open Settings activity
                Toast.makeText(this.getApplicationContext(), "Settings selected", Toast.LENGTH_SHORT).show();
                return true;

        }

        return super.onOptionsItemSelected(item);
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

        if (this.currentPageSlidingTabStripFragment == null) {
            this.currentPageSlidingTabStripFragment =
                    PageSlidingTabStripFragment.newInstance(this, position, router, preferences);
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content,
                        this.currentPageSlidingTabStripFragment,
                        PageSlidingTabStripFragment.TAG).commit();

        mDrawerLayout.closeDrawer(mDrawerList);
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
