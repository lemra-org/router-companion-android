package org.lemra.dd_wrt;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.lemra.dd_wrt.android.common.view.SlidingTabLayout;


public class DDWRTManagementActivity extends FragmentActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

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
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
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
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        private FragmentTabsAdapter fragmentTabsAdapter;

        private final int sectionNumber;

        public PlaceholderFragment(int sectionNumber) {
            this.sectionNumber = sectionNumber;
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment(sectionNumber);
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
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
            fragmentTabsAdapter = new FragmentTabsAdapter(sectionNumber, getChildFragmentManager(), getResources());
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_ddwrtmanagement, container, false);

            final ViewPager mViewPager = (ViewPager) rootView.findViewById(R.id.viewpager);
            mViewPager.setAdapter(fragmentTabsAdapter);

            final SlidingTabLayout slidingTabs = (SlidingTabLayout) rootView.findViewById(R.id.sliding_tabs);
            slidingTabs.setSelectedIndicatorColors(getResources().getColor(R.color.ddwrt_purple));
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

    private static class FragmentTabsAdapter extends FragmentPagerAdapter {

        private final Resources resources;

        private final int parentSectionNumber;

        public FragmentTabsAdapter(final int sectionNumber, FragmentManager fm, Resources resources) {
            super(fm);
            this.parentSectionNumber = sectionNumber;
            this.resources = resources;
        }

        @Override
        public int getCount() {
            //FIXME Depends on parentSectionNumber
            return 2;
        }

        @Override
        public Fragment getItem(int position) {
            //FIXME Depends on parentSectionNumber
            switch (position) {
                case 0:
                    return new DialogFragment();
                case 1:
                    return new DialogFragment();
                //TODO
            }
            return null;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            //FIXME depends on parentSectionNumber
            switch (position) {
                case 0:
                    return resources.getString(R.string.status_router);
                case 1:
                    return resources.getString(R.string.status_sysinfo);
                //TODO
            }
            return null;
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
