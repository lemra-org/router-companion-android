package org.lemra.dd_wrt.fragments;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.astuetz.PagerSlidingTabStrip;

import org.jetbrains.annotations.NotNull;
import org.lemra.dd_wrt.R;
import org.lemra.dd_wrt.prefs.sort.SortingStrategy;
import org.lemra.dd_wrt.utils.Utils;

/**
 * Created by armel on 8/13/14.
 */
public class PageSlidingTabStripFragment extends Fragment {


    public static final String TAG = PageSlidingTabStripFragment.class
            .getSimpleName();

    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String SORTING_STRATEGY = "sorting_strategy";
    public  static final String PARENT_SECTION_TITLE = "parent_section_title";

    private FragmentTabsAdapter mFragmentTabsAdapter;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static PageSlidingTabStripFragment newInstance(int sectionNumber, SharedPreferences preferences) {
        final PageSlidingTabStripFragment fragment = new PageSlidingTabStripFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        args.putString(SORTING_STRATEGY, preferences.getString("sortingStrategy", SortingStrategy.DEFAULT));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mFragmentTabsAdapter = new FragmentTabsAdapter(getArguments().getInt(ARG_SECTION_NUMBER),
                getChildFragmentManager(), getResources(), SortingStrategy.class.getPackage().getName()+"."+getArguments().getString(SORTING_STRATEGY));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.pager, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) view
                .findViewById(R.id.tabs);
        ViewPager pager = (ViewPager) view.findViewById(R.id.pager);
        pager.setAdapter(mFragmentTabsAdapter);
        tabs.setViewPager(pager);

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
            this.tabs = Utils.getFragments(this.resources, this.parentSectionNumber, sortingStrategy);
        }

        @Override
        public int getCount() {
            return this.tabs.length;
        }

        @Override
        public SherlockFragment getItem(int position) {
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
