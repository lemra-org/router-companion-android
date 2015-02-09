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

package org.rm3l.ddwrt.fragments.status;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.fragments.DDWRTBaseFragment;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.status.wireless.WirelessIfaceTile;
import org.rm3l.ddwrt.tiles.status.wireless.WirelessIfacesTile;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.startsWithIgnoreCase;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.TILE_REFRESH_MILLIS;

/**
 *
 */
public class StatusWirelessFragment extends DDWRTBaseFragment<Collection<DDWRTTile>> {

    public static final Splitter SPLITTER = Splitter.on(" ").omitEmptyStrings().trimResults();
    private static final String LOG_TAG = StatusWirelessFragment.class.getSimpleName();
    @Nullable
    private Collection<DDWRTTile> mIfaceTiles = null;

    private Loader<Collection<DDWRTTile>> mLoader;

    @Nullable
    @Override
    protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
        return Arrays.<DDWRTTile>asList(new WirelessIfacesTile(this, savedInstanceState, router));
    }

    @Nullable
    @Override
    protected Loader<Collection<DDWRTTile>> getLoader(final int id, @NonNull final Bundle args) {

        mLoader = new AsyncTaskLoader<Collection<DDWRTTile>>(getActivity()) {

            @Nullable
            @Override
            public Collection<DDWRTTile> loadInBackground() {

                try {
                    Log.d(LOG_TAG, "Init background loader for " + StatusWirelessFragment.class + ": routerInfo=" +
                            router);

                    final Fragment parentFragment = StatusWirelessFragment.this;

                    if (DDWRTCompanionConstants.TEST_MODE) {
                        return Arrays.<DDWRTTile>asList(new WirelessIfaceTile("eth0.test", parentFragment, args, router),
                                new WirelessIfaceTile("eth1.test", parentFragment, args, router),
                                new WirelessIfaceTile("eth2.test", parentFragment, args, router));
                    }

                    final NVRAMInfo nvramInfo = SSHUtils.getNVRamInfoFromRouter(StatusWirelessFragment.this.router,
                            getActivity()
                                    .getSharedPreferences(DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE), NVRAMInfo.LANDEVS);

                    if (nvramInfo == null) {
                        return null;
                    }

                    final String landevs = nvramInfo.getProperty(NVRAMInfo.LANDEVS);
                    if (landevs == null) {
                        return null;
                    }

                    final List<String> splitToList = SPLITTER.splitToList(landevs);
                    if (splitToList == null || splitToList.isEmpty()) {
                        return null;
                    }

                    final List<DDWRTTile> tiles = Lists.newArrayList();

                    for (final String landevRaw : splitToList) {
                        if (landevRaw == null) {
                            continue;
                        }
                        final String landev = landevRaw.trim();
                        if (!(startsWithIgnoreCase(landev, "wl") || startsWithIgnoreCase(landev, "wl"))) {
                            continue;
                        }

                        tiles.add(new WirelessIfaceTile(landev, parentFragment, args, router));
                        //Also get Virtual Interfaces
                        try {
                            final String landevVifsKeyword = landev + "_vifs";
                            final NVRAMInfo landevVifsNVRAMInfo = SSHUtils.getNVRamInfoFromRouter(StatusWirelessFragment.this.router,
                                    getActivity()
                                            .getSharedPreferences(DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE),
                                    landevVifsKeyword);
                            if (landevVifsNVRAMInfo == null) {
                                continue;
                            }
                            final String landevVifsNVRAMInfoProp = landevVifsNVRAMInfo.getProperty(landevVifsKeyword, DDWRTCompanionConstants.EMPTY_STRING);
                            if (landevVifsNVRAMInfoProp == null) {
                                continue;
                            }
                            final List<String> list = SPLITTER.splitToList(landevVifsNVRAMInfoProp);
                            if (list == null) {
                                continue;
                            }
                            for (final String landevVif : list) {
                                if (landevVif == null || landevVif.isEmpty()) {
                                    continue;
                                }
                                tiles.add(new WirelessIfaceTile(landevVif, landev, parentFragment, args, router));
                            }
                        } catch (final Exception e) {
                            e.printStackTrace();
                            //No worries
                        }
                    }

                    if (tiles.isEmpty()) {
                        return null;
                    }

                    return tiles;

                } catch (@NonNull final Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };
        return mLoader;

    }

    /**
     * Called when a previously created loader has finished its load.  Note
     * that normally an application is <em>not</em> allowed to commit fragment
     * transactions while in this call, since it can happen after an
     * activity's state is saved.  See {@link android.support.v4.app.FragmentManager#beginTransaction()
     * FragmentManager.openTransaction()} for further discussion on this.
     * <p/>
     * <p>This function is guaranteed to be called prior to the release of
     * the last data that was supplied for this Loader.  At this point
     * you should remove all use of the old data (since it will be released
     * soon), but should not do your own release of the data since its Loader
     * owns it and will take care of that.  The Loader will take care of
     * management of its data so you don't have to.  In particular:
     * <p/>
     * <ul>
     * <li> <p>The Loader will monitor for changes to the data, and report
     * them to you through new calls here.  You should not monitor the
     * data yourself.  For example, if the data is a {@link android.database.Cursor}
     * and you place it in a {@link android.widget.CursorAdapter}, use
     * the {@link android.widget.CursorAdapter#CursorAdapter(android.content.Context,
     * android.database.Cursor, int)} constructor <em>without</em> passing
     * in either {@link android.widget.CursorAdapter#FLAG_AUTO_REQUERY}
     * or {@link android.widget.CursorAdapter#FLAG_REGISTER_CONTENT_OBSERVER}
     * (that is, use 0 for the flags argument).  This prevents the CursorAdapter
     * from doing its own observing of the Cursor, which is not needed since
     * when a change happens you will get a new Cursor throw another call
     * here.
     * <li> The Loader will release the data once it knows the application
     * is no longer using it.  For example, if the data is
     * a {@link android.database.Cursor} from a {@link android.content.CursorLoader},
     * you should not call close() on it yourself.  If the Cursor is being placed in a
     * {@link android.widget.CursorAdapter}, you should use the
     * {@link android.widget.CursorAdapter#swapCursor(android.database.Cursor)}
     * method so that the old Cursor is not closed.
     * </ul>
     *
     * @param loader The Loader that has finished.
     * @param tiles  The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(final Loader<Collection<DDWRTTile>> loader, @Nullable final Collection<DDWRTTile> tiles) {
        Log.d(LOG_TAG, "Done loading background task for " + StatusWirelessFragment.class.getCanonicalName());
        this.mIfaceTiles = tiles;

        if (viewGroup == null || tiles == null || tiles.isEmpty()) {
            return;
        }

        final FragmentActivity fragmentActivity = getActivity();

//        final int themeBackgroundColor = getThemeBackgroundColor(fragmentActivity, router.getUuid());
        final boolean isThemeLight = ColorUtils.isThemeLight(fragmentActivity);

        final LinearLayout dynamicTilessViewGroup =
                (LinearLayout) viewGroup.findViewById(R.id.tiles_container_scrollview_layout_dynamic_items);

        //Remove everything first
        dynamicTilessViewGroup.removeAllViews();

        final CardView.LayoutParams cardViewLayoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        cardViewLayoutParams.rightMargin = R.dimen.marginRight;
        cardViewLayoutParams.leftMargin = R.dimen.marginLeft;
        cardViewLayoutParams.bottomMargin = R.dimen.activity_vertical_margin;

        final Resources resources = fragmentActivity.getResources();

        for (final DDWRTTile tile : tiles) {
            final ViewGroup tileViewGroupLayout = tile.getViewGroupLayout();
            if (tileViewGroupLayout == null) {
                continue;
            }

            //Set header background color
//            final View hdrView = tileViewGroupLayout.findViewById(tile.getTileHeaderViewId());
//            if (hdrView != null) {
//                hdrView.setBackgroundColor(fragmentColor);
//            }

            if (isThemeLight) {
                final View titleView = tileViewGroupLayout.findViewById(tile.getTileTitleViewId());
                if (titleView instanceof TextView) {
                    ((TextView) titleView)
                            .setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                }
            }

            tileViewGroupLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));

            //Init loaders for these tiles
            getActivity().getSupportLoaderManager()
                    .initLoader(Long.valueOf(Utils.getNextLoaderId()).intValue(), null, tile);

            //Add row for this iface
            final CardView cardView = new CardView(fragmentActivity);
//            cardView.setCardBackgroundColor(themeBackgroundColor);

            cardView.setOnClickListener(tile);
            tileViewGroupLayout.setOnClickListener(tile);

            cardView.setContentPadding(15, 5, 15, 5);
            cardView.setLayoutParams(cardViewLayoutParams);
//                cardView.setCardBackgroundColor(themeBackgroundColor);
            //Add padding to CardView on v20 and before to prevent intersections between the Card content and rounded corners.
            cardView.setPreventCornerOverlap(true);
            //Add padding in API v21+ as well to have the same measurements with previous versions.
            cardView.setUseCompatPadding(true);

            if (isThemeLight) {
                //Light
                cardView.setCardBackgroundColor(resources.getColor(R.color.cardview_light_background));
            } else {
                //Default is Dark
                cardView.setCardBackgroundColor(resources.getColor(R.color.cardview_dark_background));
            }

            cardView.addView(tileViewGroupLayout);

            //Remove view prior to adding it again to parent
            dynamicTilessViewGroup.addView(cardView);
        }

        //Make it visible now
        dynamicTilessViewGroup.setVisibility(View.VISIBLE);

        //Schedule next run
        mHandler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (ddwrtMainActivity != null) {
                                ddwrtMainActivity.getSupportLoaderManager()
                                        .restartLoader(loader.getId(), null, StatusWirelessFragment.this);
                            } else {
                                Log.w(LOG_TAG, "ddwrtMainActivity == null => next loader run will NOT be scheduled!");
                            }
                        } catch (final Exception e) {
                            //No worries
                            Log.w(LOG_TAG, e);
                        }

                    }
                },
                getActivity().getSharedPreferences(router.getUuid(), Context.MODE_PRIVATE).
                        getLong(DDWRTCompanionConstants.SYNC_INTERVAL_MILLIS_PREF, TILE_REFRESH_MILLIS)
        );
    }


    /**
     * Called when a previously created loader is being reset, and thus
     * making its data unavailable.  The application should at this point
     * remove any references it has to the Loader's data.
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(Loader<Collection<DDWRTTile>> loader) {
        super.onLoaderReset(loader);
    }

    /**
     * Called when the view previously created by {@link #onCreateView} has
     * been detached from the fragment.  The next time the fragment needs
     * to be displayed, a new view will be created.  This is called
     * after {@link #onStop()} and before {@link #onDestroy()}.  It is called
     * <em>regardless</em> of whether {@link #onCreateView} returned a
     * non-null view.  Internally it is called after the view's state has
     * been saved but before it has been removed from its parent.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mIfaceTiles != null) {
            int l = 100;
            final LoaderManager supportLoaderManager = getLoaderManager();
            for (int i = l; i < l + mIfaceTiles.size(); i++) {
                supportLoaderManager.destroyLoader(i);
            }
        }
    }
}
