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

package org.rm3l.ddwrt.tiles.admin.nvram;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.common.base.Throwables;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.resources.None;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.status.router.StatusRouterSpaceUsageTile;
import org.rm3l.ddwrt.utils.SSHUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

public class AdminNVRAMTile extends DDWRTTile<None> implements PopupMenu.OnMenuItemClickListener {

    private static final String LOG_TAG = AdminNVRAMTile.class.getSimpleName();
    public static final String NVRAM_SIZE = AdminNVRAMTile.class.getSimpleName() + "::nvram_size";
    public static final Comparator<Object> COMPARATOR_STRING_CASE_INSENSITIVE = new Comparator<Object>() {
        @Override
        public int compare(Object o1, Object o2) {
            if (o1 == o2) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            }
            if (o2 == null) {
                return 1;
            }
            return o1.toString().compareToIgnoreCase(o2.toString());
        }
    };

    public static final Comparator<Object> COMPARATOR_REVERSE_STRING_CASE_INSENSITIVE = new Comparator<Object>() {
        @Override
        public int compare(Object o1, Object o2) {
            if (o1 == o2) {
                return 0;
            }
            if (o1 == null) {
                return 1;
            }
            if (o2 == null) {
                return -1;
            }
            return o2.toString().compareToIgnoreCase(o1.toString());
        }
    };

    private final RecyclerView mRecyclerView;
    private final RecyclerView.Adapter mAdapter;
    private final RecyclerView.LayoutManager mLayoutManager;

    private final NVRAMInfo mNvramInfoDefaultSorting;
    private Map<Object, Object> mNvramInfoToDisplay = new HashMap<>();

    private ShareActionProvider mShareActionProvider;

    public AdminNVRAMTile(@NotNull SherlockFragment parentFragment, @NotNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_admin_nvram, R.id.tile_admin_nvram_togglebutton);
        this.mNvramInfoDefaultSorting = new NVRAMInfo();
        mRecyclerView = (RecyclerView) layout.findViewById(R.id.tile_admin_nvram_ListView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        // allows for optimizations if all items are of the same size:
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(mParentFragmentActivity);
        mLayoutManager.scrollToPosition(0);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new NVRAMDataRecyclerViewAdapter(mParentFragmentActivity, mNvramInfoDefaultSorting);
        mRecyclerView.setAdapter(mAdapter);

        //Create Options Menu
        layout.findViewById(R.id.tile_admin_nvram_menu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final PopupMenu popup = new PopupMenu(mParentFragmentActivity, v);
                popup.setOnMenuItemClickListener(AdminNVRAMTile.this);
                final MenuInflater inflater = popup.getMenuInflater();

                final Menu menu = popup.getMenu();

                inflater.inflate(R.menu.tile_admin_nvram_options, menu);

                //Disable menu item from preference
                final int currentSort;
                if (mParentFragmentPreferences != null) {
                    currentSort = mParentFragmentPreferences.getInt(getFormattedPrefKey("sort"), R.id.tile_admin_nvram_sort_default);
                } else {
                    currentSort = R.id.tile_admin_nvram_sort_default;
                }
                final MenuItem currentSortMenuItem = menu.findItem(currentSort);
                if (currentSortMenuItem != null) {
                    currentSortMenuItem.setEnabled(false);
                }

                // Locate MenuItem with ShareActionProvider
                final MenuItem shareMenuItem = menu.findItem(R.id.tile_admin_nvram_share);

                // Fetch and store ShareActionProvider
                mShareActionProvider = (ShareActionProvider) shareMenuItem.getActionProvider();

                popup.show();
            }

        });

        //Handle for Search EditText
    }

    // Call to update the share intent
    private void setShareIntent(Intent shareIntent) {
        //TODO
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final int itemId = item.getItemId();
        //Store current value in preferences
        switch(itemId) {
            case R.id.tile_admin_nvram_sort_default:
            case R.id.tile_admin_nvram_sort_asc:
            case R.id.tile_admin_nvram_sort_desc:
                //Store in preferences if any
                final Integer currentSort;
                if (mParentFragmentPreferences != null) {
                    currentSort = mParentFragmentPreferences.getInt(getFormattedPrefKey("sort"), -1);
                } else {
                    currentSort = null;
                }
                if (currentSort != null && currentSort != itemId) {
                    //Store in preferences
                    final SharedPreferences.Editor editor = mParentFragmentPreferences.edit();
                    editor.putInt(getFormattedPrefKey("sort"), itemId);
                    editor.apply();
                }
                item.setEnabled(false);
                break;
            default:
                break;
        }

        boolean notifyDatasetChanged = false;
        final Properties mNvramInfoDefaultSortingData = mNvramInfoDefaultSorting.getData();
        switch(itemId) {
            case R.id.tile_admin_nvram_sort_default:
                mNvramInfoToDisplay = new HashMap<>(mNvramInfoDefaultSortingData);
                notifyDatasetChanged = true;
                break;
            case R.id.tile_admin_nvram_sort_asc:
                mNvramInfoToDisplay = new TreeMap<>(COMPARATOR_STRING_CASE_INSENSITIVE);
                mNvramInfoToDisplay.putAll(mNvramInfoDefaultSortingData);
                notifyDatasetChanged = true;
                break;
            case R.id.tile_admin_nvram_sort_desc:
                mNvramInfoToDisplay = new TreeMap<>(COMPARATOR_REVERSE_STRING_CASE_INSENSITIVE);
                mNvramInfoToDisplay.putAll(mNvramInfoDefaultSortingData);
                notifyDatasetChanged = true;
                break;
            case R.id.tile_admin_nvram_share:
                //TODO Share action
                break;
            default:
                break;
        }

        if (notifyDatasetChanged) {
            mNvramInfoToDisplay.remove(NVRAM_SIZE);
            //noinspection ConstantConditions
            ((NVRAMDataRecyclerViewAdapter) mAdapter).setEntryList(mNvramInfoToDisplay);
            mAdapter.notifyDataSetChanged();
        }

        return false;

    }

    @Nullable
    @Override
    protected Loader<None> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<None>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public None loadInBackground() {

                try {
                    Log.d(LOG_TAG, "Init background loader for " + AdminNVRAMTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                        //Skip run
                        Log.d(LOG_TAG, "Skip loader run");
                        return (None) new None().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    mNvramInfoToDisplay.clear();
                    mNvramInfoDefaultSorting.clear();

                    NVRAMInfo nvramInfoTmp = null;

                    try {
                        nvramInfoTmp =
                                SSHUtils.getNVRamInfoFromRouter(mRouter);
                    } finally {
                        if (nvramInfoTmp != null) {
                            mNvramInfoDefaultSorting.putAll(nvramInfoTmp);
                        }

                        final String[] nvramSize = SSHUtils.getManualProperty(mRouter, "nvram show 2>&1 1>/dev/null");
                        if (nvramSize != null && nvramSize.length > 0) {
                            final List<String> nvramUsageList = StatusRouterSpaceUsageTile.NVRAM_SIZE_SPLITTER
                                    .splitToList(nvramSize[0]);
                            if (nvramUsageList != null && !nvramUsageList.isEmpty()) {
                                mNvramInfoDefaultSorting.setProperty(NVRAM_SIZE, nvramUsageList.get(0));
                            }
                        }

                    }

                    if (mNvramInfoDefaultSorting.isEmpty()) {
                        throw new DDWRTNoDataException("No Data!");
                    }

                    //Now apply sorting here (as per user-preferences)

                    final Properties defaultNVRAMInfo = mNvramInfoDefaultSorting.getData();
                    if (mParentFragmentPreferences != null) {
                        final int currentSort = mParentFragmentPreferences.getInt(getFormattedPrefKey("sort"), -1);
                        if (currentSort <= 0) {
                            mNvramInfoToDisplay = new HashMap<>(defaultNVRAMInfo);
                        } else {
                            switch (currentSort) {
                                case R.id.tile_admin_nvram_sort_asc:
                                    //asc
                                    mNvramInfoToDisplay = new TreeMap<>(COMPARATOR_STRING_CASE_INSENSITIVE);
                                    mNvramInfoToDisplay.putAll(defaultNVRAMInfo);
                                    break;
                                case R.id.tile_admin_nvram_sort_desc:
                                    //desc
                                    mNvramInfoToDisplay = new TreeMap<>(COMPARATOR_REVERSE_STRING_CASE_INSENSITIVE);
                                    mNvramInfoToDisplay.putAll(defaultNVRAMInfo);
                                    break;
                                case R.id.tile_admin_nvram_sort_default:
                                default:
                                    mNvramInfoToDisplay = new HashMap<>(defaultNVRAMInfo);
                                    break;
                            }
                        }
                    } else {
                        mNvramInfoToDisplay = new HashMap<>(defaultNVRAMInfo);
                    }

                    return new None();
                } catch (@NotNull final Exception e) {
                    e.printStackTrace();
                    return (None) new None().setException(e);
                }

            }
        };
    }

    @Nullable
    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Nullable
    @Override
    protected Intent getOnclickIntent() {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<None> loader, None data) {
        //Set tiles
        Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

        if (data == null || mNvramInfoToDisplay.isEmpty()) {
            data = (None) new None().setException(new DDWRTNoDataException("No Data!"));
        }

        layout.findViewById(R.id.tile_admin_nvram_loading_view).setVisibility(View.GONE);

        @NotNull final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_admin_nvram_error);

        @Nullable final Exception exception = data.getException();

        //NVRAM
        final Object nvramSize = mNvramInfoToDisplay.remove(NVRAM_SIZE);
        ((TextView) this.layout.findViewById(R.id.tile_admin_nvram_size))
                .setText(nvramSize != null ? nvramSize.toString(): "N/A");

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

            if (exception == null) {
                errorPlaceHolderView.setVisibility(View.GONE);
            }
            ((NVRAMDataRecyclerViewAdapter) mAdapter).setEntryList(mNvramInfoToDisplay);
            mAdapter.notifyDataSetChanged();
        }

        if (exception != null && !(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {
            //noinspection ThrowableResultOfMethodCallIgnored
            final Throwable rootCause = Throwables.getRootCause(exception);
            errorPlaceHolderView.setText("Error: " + (rootCause != null ? rootCause.getMessage() : "null"));
            final Context parentContext = this.mParentFragmentActivity;
            errorPlaceHolderView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    //noinspection ThrowableResultOfMethodCallIgnored
                    if (rootCause != null) {
                        Toast.makeText(parentContext,
                                rootCause.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
            errorPlaceHolderView.setVisibility(View.VISIBLE);
        }

        doneWithLoaderInstance(this, loader,
                R.id.tile_admin_nvram_togglebutton_title, R.id.tile_admin_nvram_togglebutton_separator);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");
    }

}
