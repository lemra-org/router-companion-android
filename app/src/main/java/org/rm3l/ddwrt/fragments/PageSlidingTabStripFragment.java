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

package org.rm3l.ddwrt.fragments;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.android.common.view.SlidingTabLayout;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.prefs.sort.SortingStrategy;

/**
 * Page Sliding fragment
 *
 * @author <a href="mailto:apps+ddwrt@rm3l.org">Armel S.</a>
 */
public class PageSlidingTabStripFragment extends SherlockFragment {

    public static final String TAG = PageSlidingTabStripFragment.class
            .getSimpleName();
    public static final String PARENT_SECTION_TITLE = "parent_section_title";
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String SORTING_STRATEGY = "sorting_strategy";
    @Nullable
    private FragmentTabsAdapter mFragmentTabsAdapter;
    private ViewPager.OnPageChangeListener mOnPageChangeListener;

    @Nullable
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
                                                          @Nullable final String routerUuid, @NotNull SharedPreferences preferences) {
        @NotNull final PageSlidingTabStripFragment fragment = new PageSlidingTabStripFragment();
        @NotNull Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        args.putString(SORTING_STRATEGY, preferences.getString("sortingStrategy", SortingStrategy.DEFAULT));
        args.putString(DDWRTBaseFragment.ROUTER_CONNECTION_INFO, routerUuid);
        fragment.setArguments(args);
        fragment.mOnPageChangeListener = onPageChangeListener;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mFragmentTabsAdapter = new FragmentTabsAdapter(getArguments().getInt(ARG_SECTION_NUMBER),
                getChildFragmentManager(), getResources(),
                SortingStrategy.class.getPackage().getName() + "." + getArguments().getString(SORTING_STRATEGY),
                getArguments().getString(DDWRTBaseFragment.ROUTER_CONNECTION_INFO));
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
        mPager.setOffscreenPageLimit(1); //disable pre-loading of prev and next pages
        mPager.setAdapter(mFragmentTabsAdapter);
        mPager.setOnPageChangeListener(mOnPageChangeListener);

        mSlidingTabLayout = (SlidingTabLayout) view.findViewById(R.id.tabs);
        mSlidingTabLayout.setViewPager(this.mPager);
    }

    private static class FragmentTabsAdapter extends FragmentStatePagerAdapter {

        @NotNull
        final DDWRTBaseFragment[] tabs;

        private final Resources resources;
        private final int parentSectionNumber;

        public FragmentTabsAdapter(final int sectionNumber, FragmentManager fm, Resources resources, String sortingStrategy,
                                   @Nullable final String routerUuid) {
            super(fm);
            this.parentSectionNumber = sectionNumber;
            this.resources = resources;
            this.tabs = DDWRTBaseFragment.getFragments(this.resources, this.parentSectionNumber, sortingStrategy, routerUuid);
        }

        @Override
        public int getCount() {
            return this.tabs.length;
        }

//        /**
//         * @return true if the value returned from {@link #instantiateItem(ViewGroup, int)} is the
//         * same object as the {@link View} added to the {@link ViewPager}.
//         */
//        @Override
//        public boolean isViewFromObject(View view, Object o) {
//            return o == view;
//        }

        @Nullable
        @Override
        public SherlockFragment getItem(int position) {
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

//        @Override
//        public void setPrimaryItem(ViewGroup container, int position, @Nullable Object object) {
//            super.setPrimaryItem(container, position, object);
//            // Hack to allow the non-primary fragments to start properly
//            if (object != null) {
//                ((Fragment) object).setUserVisibleHint(false);
//            }
//        }

    }

}
