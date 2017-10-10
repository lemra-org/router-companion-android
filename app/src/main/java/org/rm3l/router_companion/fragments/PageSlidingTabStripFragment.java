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

package org.rm3l.router_companion.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.common.view.SlidingTabLayout;
import com.crashlytics.android.Crashlytics;
import java.util.ArrayList;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.main.DDWRTMainActivity;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.prefs.sort.SortingStrategy;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.utils.ColorUtils;

/**
 * Page Sliding fragment
 *
 * @author <a href="mailto:apps+ddwrt@rm3l.org">Armel S.</a>
 */
public class PageSlidingTabStripFragment extends Fragment {

    public static class FragmentTabsAdapter extends FragmentStatePagerAdapter {

        @NonNull
        final AbstractBaseFragment[] tabs;

        private final int parentSectionNumber;

        private final Resources resources;

        public FragmentTabsAdapter(PageSlidingTabStripFragment pageSlidingTabStripFragment,
                final int sectionNumber, FragmentManager fm, Resources resources, String sortingStrategy,
                @Nullable final String routerUuid) {
            super(fm);
            this.parentSectionNumber = sectionNumber;
            this.resources = resources;
            this.tabs = AbstractBaseFragment.getFragments(pageSlidingTabStripFragment.getActivity(),
                    this.resources, this.parentSectionNumber, sortingStrategy, routerUuid);
        }

        public FragmentTabsAdapter(final Activity activity, final int sectionNumber, FragmentManager fm,
                Resources resources, String sortingStrategy, @Nullable final String routerUuid) {
            super(fm);
            this.parentSectionNumber = sectionNumber;
            this.resources = resources;
            this.tabs =
                    AbstractBaseFragment.getFragments(activity, this.resources, this.parentSectionNumber,
                            sortingStrategy, routerUuid);
        }

        @Override
        public int getCount() {
            return this.tabs.length;
        }

        @Nullable
        @Override
        public Fragment getItem(int position) {
            if (this.tabs.length <= position) {
                Crashlytics.log(Log.ERROR, TAG, "tabs contains less than " + position + " items");
                return null;
            }
            return this.tabs[position];
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            if (this.tabs.length <= position) {
                Crashlytics.log(Log.ERROR, TAG, "tabs contains less than " + position + " items");
                return null;
            }
            final AbstractBaseFragment tab = this.tabs[position];
            Crashlytics.log(Log.DEBUG, TAG, "Tab @position #" + position + ": " + tab);
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

    public static final String TAG = PageSlidingTabStripFragment.class.getSimpleName();

    public static final String PARENT_SECTION_TITLE = "parent_section_title";

    private static final String ARG_SECTION_NUMBER = "section_number";

    private static final String SORTING_STRATEGY = "sorting_strategy";

    @NonNull
    private FragmentTabsAdapter mFragmentTabsAdapter;

    private ViewPager.OnPageChangeListener mOnPageChangeListener;

    @NonNull
    private ViewPager mPager;

    private Router mRouter;

    /**
     * A custom {@link ViewPager} title strip which looks much like Tabs present in Android v4.0 and
     * above, but is designed to give continuous feedback to the user when scrolling.
     */
    private SlidingTabLayout mSlidingTabLayout;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    @NonNull
    public static PageSlidingTabStripFragment newInstance(
            @NonNull final ViewPager.OnPageChangeListener onPageChangeListener, int sectionNumber,
            @Nullable final String routerUuid) {
        final PageSlidingTabStripFragment fragment = new PageSlidingTabStripFragment();
        final Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        args.putString(AbstractBaseFragment.ROUTER_CONNECTION_INFO, routerUuid);
        fragment.setArguments(args);
        fragment.mOnPageChangeListener = onPageChangeListener;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        final String routerUuid = getArguments().getString(AbstractBaseFragment.ROUTER_CONNECTION_INFO);
        mRouter = RouterManagementActivity.getDao(getActivity()).getRouter(routerUuid);

        final SharedPreferences sharedPreferences =
                getActivity().getSharedPreferences(routerUuid, Context.MODE_PRIVATE);

        mFragmentTabsAdapter = new FragmentTabsAdapter(this, getArguments().getInt(ARG_SECTION_NUMBER),
                getChildFragmentManager(), getResources(),
                String.format("%s.%s", SortingStrategy.class.getPackage().getName(),
                        sharedPreferences.getString(RouterCompanionAppConstants.SORTING_STRATEGY_PREF,
                                SortingStrategy.DEFAULT)), routerUuid);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.pager, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPager = (ViewPager) view.findViewById(R.id.viewPager);
        final int mFragmentTabsAdapterCount = mFragmentTabsAdapter.getCount();
        mPager.setOffscreenPageLimit(mFragmentTabsAdapterCount - 1);
        mPager.setAdapter(mFragmentTabsAdapter);
        mPager.addOnPageChangeListener(mOnPageChangeListener);

        mSlidingTabLayout = (SlidingTabLayout) view.findViewById(R.id.tabs);
        int colorForOpenWrt = -1;
        if (mRouter != null) {
            final Router.RouterFirmware routerFirmware = this.mRouter.getRouterFirmware();
            if (routerFirmware != null) {
                switch (routerFirmware) {
                    case OPENWRT:
                        //Change background color
                        colorForOpenWrt = ContextCompat.getColor(getContext(), R.color.win8_orange);
                        mSlidingTabLayout.setBackgroundColor(colorForOpenWrt);
                        break;
                    default:
                        //Use default theme
                        break;
                }
            }
        }

        mSlidingTabLayout.setViewPager(this.mPager);
        mSlidingTabLayout.setCustomTabView(R.layout.tab_indicator, android.R.id.text1);

        final ArrayList<Integer> colorsToSkip = new ArrayList<>();
        if (colorForOpenWrt > 0) {
            colorsToSkip.add(colorForOpenWrt);
        }
        final Drawable tabsBackground = mSlidingTabLayout.getBackground();
        if (tabsBackground instanceof ColorDrawable) {
            colorsToSkip.add(((ColorDrawable) tabsBackground).getColor());
        }

        //Generate random colors to use for selection
        final AbstractBaseFragment[] tabs = mFragmentTabsAdapter.tabs;
        if (tabs.length > 0) {
            final int[] selectedIndicatorColors = new int[tabs.length];
            int i = 0;
            for (final AbstractBaseFragment tab : tabs) {
                selectedIndicatorColors[i++] =
                        (tab != null ? ColorUtils.Companion.getColor(tab.getClass().getSimpleName())
                                : ColorUtils.Companion.genColor(colorsToSkip));
            }
            mSlidingTabLayout.setSelectedIndicatorColors(selectedIndicatorColors);
        } else {
            mSlidingTabLayout.setSelectedIndicatorColors(
                    ContextCompat.getColor(getContext(), R.color.tab_selected_strip));
        }

        //        mSlidingTabLayout.setDistributeEvenly(true);
        mSlidingTabLayout.setViewPager(this.mPager);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public ViewPager.OnPageChangeListener getOnPageChangeListener() {
        return mOnPageChangeListener;
    }

    public void startActivityForResult(Intent intent, DDWRTTile.ActivityResultListener listener) {
        if (mOnPageChangeListener instanceof DDWRTMainActivity) {
            ((DDWRTMainActivity) mOnPageChangeListener).startActivityForResult(intent, listener);
        }
    }
}
