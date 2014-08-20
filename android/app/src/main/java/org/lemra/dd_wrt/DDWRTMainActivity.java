package org.lemra.dd_wrt;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import org.lemra.dd_wrt.api.conn.Router;
import org.lemra.dd_wrt.fragments.PageSlidingTabStripFragment;

/**
 * Created by armel on 8/9/14.
 */
public class DDWRTMainActivity extends SherlockFragmentActivity {

    public static final String TAG = DDWRTMainActivity.class.getSimpleName();
    //TESTS
    private static final Router router = new Router();

    static {
        router.setRemoteIpAddress("172.17.17.1");
        router.setUsername("root");
        router.setName("@home");
        router.setRouterConnectionProtocol(Router.RouterConnectionProtocol.SSH);
//        router.setPassword("2315netgearOrange");
        router.setPrivKey("-----BEGIN DSA PRIVATE KEY-----\n" +
                "MIIBuwIBAAKBgQDDbsDj7rujyw6GNMLCJ0Tv1/JvsLNdNeHGwNlSj/pjxD6yVxmX\n" +
                "a/KDGaqvXWOWm+TW7ugPeocF2fl8DEC31yLfaY6GrjhyHozqmjut/RcwbLQYsLyp\n" +
                "HB9XT4zd5ShV4fAXF1Unhg1RCa/yCKJkE3zI+8UZ3HfHv8vg0C1m3ryhNQIVAOk3\n" +
                "G4IafJlRtyG1B3oO+uayDvH1AoGAU+tLC50aytDDBsqxKY/q2AWcLBoKDUr2DAY+\n" +
                "5fb2m1Hl8A0/YWwiEISXCOReEkXjvPfUgZg3e3UMCED7ULEu5dGjFLNKaN4dmP6q\n" +
                "C8NLKornh4tugk+geNq9qW9HmDwPEr2i8gAQxYPmSPCiKv3cW6wt0c3h7zyDfYPT\n" +
                "Vrf1Cu0CgYBXoU4+bljVUxzXkBsqwdZgKckfp/mABgAz8nEcmvklLRiM0bbMBxtn\n" +
                "4fQqkh1ENGrk8H+Rx6UNgaFdYg596gFCA8cSJiouwS+xBO8FT79DAeiLN5xTsH32\n" +
                "7KzlIfgcsnniYZDLD9pS/x58G7/loUD9lH53JAa81THNwZeaaEaHwQIVAN8WSYc6\n" +
                "JJuAZoyITiuIO2VDTsP4\n" +
                "-----END DSA PRIVATE KEY-----");
        router.setStrictHostKeyChecking(false);
    }
    DrawerLayout mDrawerLayout;
    ListView mDrawerList;
    ActionBarDrawerToggle mDrawerToggle;
    private SharedPreferences preferences;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private String[] mDDWRTNavigationMenuSections;
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
        getSupportMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
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
            case R.id.action_settings:
                Toast.makeText(this.getApplicationContext(), "Settings selected", Toast.LENGTH_SHORT).show();
                return true;

//            case R.id.action_contact:
//                QuickContactFragment dialog = new QuickContactFragment();
//                dialog.show(getSupportFragmentManager(), "QuickContactFragment");
//                return true;

        }

        return super.onOptionsItemSelected(item);
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

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content,
                        PageSlidingTabStripFragment.newInstance(position, router, preferences),
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
