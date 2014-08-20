package org.lemra.dd_wrt;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.jetbrains.annotations.NotNull;
import org.lemra.dd_wrt.android.common.view.SlidingTabLayout;
import org.lemra.dd_wrt.fragments.DDWRTBaseFragment;
import org.lemra.dd_wrt.prefs.sort.SortingStrategy;

@Deprecated
public class DDWRTManagementActivity extends FragmentActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    public static final String TAG = DDWRTManagementActivity.class.getSimpleName();

    private SharedPreferences preferences;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(TAG, Context.MODE_PRIVATE);
        setContentView(R.layout.activity_ddwrtmanagement);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1, preferences))
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.status);
                break;
            case 2:
                mTitle = getString(R.string.setup);
                break;
            case 3:
                mTitle = getString(R.string.wireless);
                break;
            case 4:
                mTitle = getString(R.string.services);
                break;
            case 5:
                mTitle = getString(R.string.security);
                break;
            case 6:
                mTitle = getString(R.string.access_restrictions);
                break;
            case 7:
                mTitle = getString(R.string.nat_qos);
                break;
            case 8:
                mTitle = getString(R.string.admin_area);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.ddwrtmanagement, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        public static final String PARENT_SECTION_TITLE = "parent_section_title";
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private static final String SORTING_STRATEGY = "sorting_strategy";
        private FragmentTabsAdapter fragmentTabsAdapter;

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber, SharedPreferences preferences) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            args.putString(SORTING_STRATEGY, preferences.getString("sortingStrategy", SortingStrategy.DEFAULT));
            fragment.setArguments(args);
            return fragment;
        }

        /**
         * Called to do initial creation of a fragment.  This is called after
         * {@link #onAttach(android.app.Activity)} and before
         * {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)}.
         * <p/>
         * <p>Note that this can be called while the fragment's activity is
         * still in the process of being created.  As such, you can not rely
         * on things like the activity's content view hierarchy being initialized
         * at this point.  If you want to do work once the activity itself is
         * created, see {@link #onActivityCreated(android.os.Bundle)}.
         *
         * @param savedInstanceState If the fragment is being re-created from
         *                           a previous saved state, this is the state.
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            fragmentTabsAdapter = new FragmentTabsAdapter(getArguments().getInt(ARG_SECTION_NUMBER),
                    getChildFragmentManager(), getResources(), SortingStrategy.class.getPackage().getName()+"."+getArguments().getString(SORTING_STRATEGY));
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_ddwrtmanagement, container, false);

            final ViewPager mViewPager = (ViewPager) rootView.findViewById(R.id.viewpager);
            mViewPager.setAdapter(fragmentTabsAdapter);

            final SlidingTabLayout slidingTabs = (SlidingTabLayout) rootView.findViewById(R.id.sliding_tabs);
            slidingTabs.setSelectedIndicatorColors(getResources().getColor(R.color.ddwrt_green));
            slidingTabs.setViewPager(mViewPager);

            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((DDWRTManagementActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

    private static class FragmentTabsAdapter extends FragmentStatePagerAdapter {

        @NotNull
        final DDWRTBaseFragment[] tabs;
        private final Resources resources;
        private final int parentSectionNumber;

        public FragmentTabsAdapter(final int sectionNumber, FragmentManager fm, Resources resources, String sortingStrategy) {
            super(fm);
            this.parentSectionNumber = sectionNumber;
            this.resources = resources;
            //FIXME
            this.tabs = DDWRTBaseFragment.getFragments(this.resources, this.parentSectionNumber, sortingStrategy, null);
        }

        @Override
        public int getCount() {
            return this.tabs.length;
        }

        @Override
        public Fragment getItem(int position) {
            if (this.tabs.length <= position) {
                Log.d(TAG, "tabs contains less than " + position + " items");
                return null;
            }
            return this.tabs[position];
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (this.tabs.length <= position) {
                Log.d(TAG, "tabs contains less than " + position + " items");
                return null;
            }
            final DDWRTBaseFragment tab = this.tabs[position];
            Log.d(TAG, "Tab @position #" + position + ": " + tab);
            if (tab == null) {
                return null;
            }
            return tab.getTabTitle();
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            // Hack to allow the non-primary fragments to start properly
            if (object != null) {
                ((Fragment) object).setUserVisibleHint(false);
            }
        }

    }

}
