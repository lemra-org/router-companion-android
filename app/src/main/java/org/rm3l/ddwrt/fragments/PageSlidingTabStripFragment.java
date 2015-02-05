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

package org.rm3l.ddwrt.fragments;

import android.content.Context;
import android.content.Intent;
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

import com.android.common.view.SlidingTabLayout;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.DDWRTMainActivity;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.prefs.sort.SortingStrategy;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;

/**
 * Page Sliding fragment
 *
 * @author <a href="mailto:apps+ddwrt@rm3l.org">Armel S.</a>
 */
public class PageSlidingTabStripFragment extends Fragment {

    public static final String TAG = PageSlidingTabStripFragment.class
            .getSimpleName();
    public static final String PARENT_SECTION_TITLE = "parent_section_title";
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String SORTING_STRATEGY = "sorting_strategy";
    @NotNull
    private FragmentTabsAdapter mFragmentTabsAdapter;
    private ViewPager.OnPageChangeListener mOnPageChangeListener;

    @NotNull
    private ViewPager mPager;

    /**
     * A custom {@link ViewPager} title strip which looks much like Tabs present in Android v4.0 and
     * above, but is designed to give continuous feedback to the user when scrolling.
     */
    private SlidingTabLayout mSlidingTabLayout;


    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    @NotNull
    public static PageSlidingTabStripFragment newInstance(@NotNull final ViewPager.OnPageChangeListener onPageChangeListener,
                                                          int sectionNumber,
                                                          @Nullable final String routerUuid) {
        @NotNull final PageSlidingTabStripFragment fragment = new PageSlidingTabStripFragment();
        @NotNull Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        args.putString(DDWRTBaseFragment.ROUTER_CONNECTION_INFO, routerUuid);
        fragment.setArguments(args);
        fragment.mOnPageChangeListener = onPageChangeListener;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        final String routerUuid = getArguments().getString(DDWRTBaseFragment.ROUTER_CONNECTION_INFO);
        final SharedPreferences sharedPreferences = getActivity().getSharedPreferences(routerUuid, Context.MODE_PRIVATE);

        mFragmentTabsAdapter = new FragmentTabsAdapter(
                getArguments().getInt(ARG_SECTION_NUMBER),
                getChildFragmentManager(), getResources(),
                String.format("%s.%s",
                        SortingStrategy.class.getPackage().getName(),
                        sharedPreferences.getString(DDWRTCompanionConstants.SORTING_STRATEGY_PREF, SortingStrategy.DEFAULT)
                ),
                routerUuid);
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.pager, container, false);
    }

    @Override
    public void onViewCreated(@NotNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPager = (ViewPager) view.findViewById(R.id.viewPager);
        mPager.setOffscreenPageLimit(mFragmentTabsAdapter.getCount() - 1);
        mPager.setAdapter(mFragmentTabsAdapter);
        mPager.setOnPageChangeListener(mOnPageChangeListener);

        mSlidingTabLayout = (SlidingTabLayout) view.findViewById(R.id.tabs);
        mSlidingTabLayout.setViewPager(this.mPager);
        mSlidingTabLayout.setCustomTabView(R.layout.tab_indicator, android.R.id.text1);
        mSlidingTabLayout.setSelectedIndicatorColors(
                getResources().getColor(R.color.tab_selected_strip));
//        mSlidingTabLayout.setDistributeEvenly(true);
        mSlidingTabLayout.setViewPager(this.mPager);
    }

    public void startActivityForResult(Intent intent, DDWRTTile.ActivityResultListener listener) {
        if (mOnPageChangeListener instanceof DDWRTMainActivity) {
            ((DDWRTMainActivity) mOnPageChangeListener).startActivityForResult(intent, listener);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private class FragmentTabsAdapter extends FragmentStatePagerAdapter {

        @NotNull
        final DDWRTBaseFragment[] tabs;

        private final Resources resources;
        private final int parentSectionNumber;

        public FragmentTabsAdapter(final int sectionNumber, FragmentManager fm, Resources resources, String sortingStrategy,
                                   @Nullable final String routerUuid) {
            super(fm);
            this.parentSectionNumber = sectionNumber;
            this.resources = resources;
            this.tabs = DDWRTBaseFragment.getFragments(PageSlidingTabStripFragment.this, this.resources, this.parentSectionNumber, sortingStrategy, routerUuid);
        }

        @Override
        public int getCount() {
            return this.tabs.length;
        }

        @Nullable
        @Override
        public Fragment getItem(int position) {
            if (this.tabs.length <= position) {
                Log.d(TAG, "tabs contains less than " + position + " items");
                return null;
            }
            return this.tabs[position];
        }

        @Nullable
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
        public void setPrimaryItem(ViewGroup container, int position, @Nullable Object object) {
            super.setPrimaryItem(container, position, object);
            // Hack to allow the non-primary fragments to start properly
            if (object != null) {
                ((Fragment) object).setUserVisibleHint(false);
            }
        }

    }

}
