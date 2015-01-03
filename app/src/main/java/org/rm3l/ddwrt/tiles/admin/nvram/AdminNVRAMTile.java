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
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.FileProvider;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import org.apache.commons.lang3.StringUtils;
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.EMPTY_STRING;

public class AdminNVRAMTile extends DDWRTTile<None> implements PopupMenu.OnMenuItemClickListener {

    private static final String LOG_TAG = AdminNVRAMTile.class.getSimpleName();
    public static final String NVRAM_SIZE = AdminNVRAMTile.class.getSimpleName() + "::nvram_size";
    private static final String LAST_SEARCH = "lastSearch";
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
    public static final String SORT = "sort";
    public static final Joiner.MapJoiner PROPERTIES_JOINER_TO_FILE = Joiner.on('\n').withKeyValueSeparator("=");

    private final RecyclerView mRecyclerView;
    private final RecyclerView.Adapter mAdapter;
    private final RecyclerView.LayoutManager mLayoutManager;

    private final NVRAMInfo mNvramInfoDefaultSorting;
    private Map<Object, Object> mNvramInfoToDisplay = new HashMap<>();

    private ShareActionProvider mShareActionProvider;

    private final BiMap<Integer,Integer> sortIds = HashBiMap.create();

    public AdminNVRAMTile(@NotNull SherlockFragment parentFragment, @NotNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_admin_nvram, R.id.tile_admin_nvram_togglebutton);

        sortIds.put(R.id.tile_admin_nvram_sort_default, 11);
        sortIds.put(R.id.tile_admin_nvram_sort_asc, 12);
        sortIds.put(R.id.tile_admin_nvram_sort_desc, 13);

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
        mAdapter = new NVRAMDataRecyclerViewAdapter(mParentFragmentActivity, router, mNvramInfoDefaultSorting);
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
                Integer currentSort = null;
                if (mParentFragmentPreferences != null) {
                    currentSort = sortIds.inverse()
                            .get(mParentFragmentPreferences.getInt(getFormattedPrefKey(SORT),
                                    sortIds.get(R.id.tile_admin_nvram_sort_default)));
                }

                if (currentSort == null) {
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
        final EditText filterEditText = (EditText) this.layout.findViewById(R.id.tile_admin_nvram_filter);
        //Initialize with existing search data
        filterEditText.setText(mParentFragmentPreferences != null ?
                mParentFragmentPreferences.getString(getFormattedPrefKey(LAST_SEARCH), EMPTY_STRING) : EMPTY_STRING);

        filterEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_LEFT = 0;
                final int DRAWABLE_TOP = 1;
                final int DRAWABLE_RIGHT = 2;
                final int DRAWABLE_BOTTOM = 3;

                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (filterEditText.getRight() - filterEditText.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        //'Clear' button - clear data, and reset everything out
                        //Reset everything
                        filterEditText.setText(EMPTY_STRING);

                        final Properties mNvramInfoDefaultSortingData = mNvramInfoDefaultSorting.getData();
                        //Update adapter in the preferences
                        if (mParentFragmentPreferences != null) {
                            final Integer currentSort = sortIds.inverse()
                                    .get(mParentFragmentPreferences.getInt(getFormattedPrefKey(SORT), -1));
                            if (currentSort == null || currentSort <= 0) {
                                mNvramInfoToDisplay = new HashMap<>(mNvramInfoDefaultSortingData);
                            } else {
                                switch (currentSort) {
                                    case R.id.tile_admin_nvram_sort_asc:
                                        //asc
                                        mNvramInfoToDisplay = new TreeMap<>(COMPARATOR_STRING_CASE_INSENSITIVE);
                                        break;
                                    case R.id.tile_admin_nvram_sort_desc:
                                        //desc
                                        mNvramInfoToDisplay = new TreeMap<>(COMPARATOR_REVERSE_STRING_CASE_INSENSITIVE);
                                        break;
                                    case R.id.tile_admin_nvram_sort_default:
                                    default:
                                        mNvramInfoToDisplay = new HashMap<>();
                                        break;
                                }
                                mNvramInfoToDisplay.putAll(mNvramInfoDefaultSortingData);
                            }
                        } else {
                            mNvramInfoToDisplay = new HashMap<>(mNvramInfoDefaultSortingData);
                        }

                        ((NVRAMDataRecyclerViewAdapter) mAdapter).setEntryList(mNvramInfoToDisplay);
                        mAdapter.notifyDataSetChanged();

                        if (mParentFragmentPreferences != null) {
                            final SharedPreferences.Editor editor = mParentFragmentPreferences.edit();
                            editor.putString(getFormattedPrefKey(LAST_SEARCH), EMPTY_STRING);
                            editor.apply();
                        }
                        return true;
                    }
                }
                return false;
            }
        });

        filterEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    final String textToFind = filterEditText.getText().toString();
                    if (isNullOrEmpty(textToFind)) {
                        //extra-check, even though we can be pretty sure the button is enabled only if textToFind is present
                        return true;
                    }
                    final String existingSearch = mParentFragmentPreferences != null ?
                            mParentFragmentPreferences.getString(getFormattedPrefKey(LAST_SEARCH), null) : null;

                    if (mParentFragmentPreferences != null) {
                        if (textToFind.equalsIgnoreCase(existingSearch)) {
                            //No need to go further as this is already the string we are looking for
                            return true;
                        }
                        final SharedPreferences.Editor editor = mParentFragmentPreferences.edit();
                        editor.putString(getFormattedPrefKey(LAST_SEARCH), textToFind);
                        editor.apply();
                    }

                    //Filter out (and sort by user-preference)
                    final Properties mNvramInfoDefaultSortingData = mNvramInfoDefaultSorting.getData();
                    //Update adapter in the preferences
                    final Map<Object, Object> mNvramInfoToDisplayCopy;
                    if (mParentFragmentPreferences != null) {
                        final Integer currentSort = sortIds.inverse()
                                .get(mParentFragmentPreferences.getInt(getFormattedPrefKey(SORT), -1));
                        if (currentSort == null || currentSort <= 0) {
                            mNvramInfoToDisplayCopy = new HashMap<>(mNvramInfoDefaultSortingData);
                        } else {
                            switch (currentSort) {
                                case R.id.tile_admin_nvram_sort_asc:
                                    //asc
                                    mNvramInfoToDisplayCopy = new TreeMap<>(COMPARATOR_STRING_CASE_INSENSITIVE);
                                    break;
                                case R.id.tile_admin_nvram_sort_desc:
                                    //desc
                                    mNvramInfoToDisplayCopy = new TreeMap<>(COMPARATOR_REVERSE_STRING_CASE_INSENSITIVE);
                                    break;
                                case R.id.tile_admin_nvram_sort_default:
                                default:
                                    mNvramInfoToDisplayCopy = new HashMap<>();
                                    break;
                            }
                            //noinspection ConstantConditions
                            for (Map.Entry<Object, Object> entry : mNvramInfoDefaultSortingData.entrySet()) {
                                final Object key = entry.getKey();
                                final Object value = entry.getValue();
                                if (key == null) {
                                    continue;
                                }
                                if (containsIgnoreCase(key.toString(), textToFind) || containsIgnoreCase(value.toString(), textToFind)) {
                                    mNvramInfoToDisplayCopy.put(key,value);
                                }
                            }
                        }
                    } else {
                        mNvramInfoToDisplayCopy = new HashMap<>();
                        //noinspection ConstantConditions
                        for (Map.Entry<Object, Object> entry : mNvramInfoDefaultSortingData.entrySet()) {
                            final Object key = entry.getKey();
                            final Object value = entry.getValue();
                            if (key == null) {
                                continue;
                            }
                            if (containsIgnoreCase(key.toString(), textToFind) || containsIgnoreCase(value.toString(), textToFind)) {
                                mNvramInfoToDisplayCopy.put(key,value);
                            }
                        }
                    }

                    ((NVRAMDataRecyclerViewAdapter) mAdapter).setEntryList(mNvramInfoToDisplayCopy);
                    mAdapter.notifyDataSetChanged();

                    return true;
                }
                return false;
            }
        });

    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_admin_nvram_title;
    }

    // Call to update the share intent
    private void setShareIntent(Intent shareIntent) {
        //TODO
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    private void setShareFile(File file) {
        if (mShareActionProvider == null) {
            return;
        }

        final Uri uriForFile = FileProvider
                .getUriForFile(mParentFragmentActivity, "org.rm3l.fileprovider", file);

        mShareActionProvider.setOnShareTargetSelectedListener(new ShareActionProvider.OnShareTargetSelectedListener() {
            @Override
            public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
                mParentFragmentActivity
                        .grantUriPermission(intent.getComponent().getPackageName(),
                                uriForFile, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                return true;
            }
        });

        final Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);
        sendIntent.setData(uriForFile);
        sendIntent.setType("text/plain");
        sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        setShareIntent(sendIntent);

    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final int itemId = item.getItemId();
        //Store current value in preferences
        switch(itemId) {
            case R.id.tile_admin_nvram_sort_default:
            case R.id.tile_admin_nvram_sort_asc:
            case R.id.tile_admin_nvram_sort_desc:
                item.setEnabled(false);
                //Store in preferences if any
                final Integer currentSort;
                if (mParentFragmentPreferences != null) {
                    currentSort = sortIds.inverse()
                            .get(mParentFragmentPreferences.getInt(getFormattedPrefKey(SORT), -1));
                } else {
                    currentSort = null;
                }

                if (mParentFragmentPreferences != null && (currentSort == null || currentSort != itemId)) {
                    //No pref on file or different pref on file => store in preferences
                    final SharedPreferences.Editor editor = mParentFragmentPreferences.edit();
                    editor.putInt(getFormattedPrefKey(SORT), sortIds.get(itemId));
                    editor.apply();
                }
                break;
            default:
                break;
        }

        if (itemId != R.id.tile_admin_nvram_share) {
            Map<Object, Object> mNvramInfoToDisplayCopy = null;

            boolean notifyDatasetChanged = false;

            //Filter out by search and sort preferences
            final String textToFind = mParentFragmentPreferences != null ?
                    mParentFragmentPreferences.getString(getFormattedPrefKey(LAST_SEARCH), null) : null;
            if (isNullOrEmpty(textToFind)) {
                //Filter on while dataset
                final Properties mNvramInfoDefaultSortingData = mNvramInfoDefaultSorting.getData();
                switch (itemId) {
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
                    default:
                        break;
                }
                mNvramInfoToDisplayCopy = mNvramInfoToDisplay;
            } else {
                //Already filtered data
                final Map<Object, Object> adapterNvramInfo = ((NVRAMDataRecyclerViewAdapter) mAdapter).getNvramInfo();
                switch (itemId) {
                    case R.id.tile_admin_nvram_sort_default:
                        mNvramInfoToDisplayCopy = new HashMap<>(adapterNvramInfo);
                        notifyDatasetChanged = true;
                        break;
                    case R.id.tile_admin_nvram_sort_asc:
                        mNvramInfoToDisplayCopy = new TreeMap<>(COMPARATOR_STRING_CASE_INSENSITIVE);
                        mNvramInfoToDisplayCopy.putAll(adapterNvramInfo);
                        notifyDatasetChanged = true;
                        break;
                    case R.id.tile_admin_nvram_sort_desc:
                        mNvramInfoToDisplayCopy = new TreeMap<>(COMPARATOR_REVERSE_STRING_CASE_INSENSITIVE);
                        mNvramInfoToDisplayCopy.putAll(adapterNvramInfo);
                        notifyDatasetChanged = true;
                        break;
                    default:
                        break;
                }
            }

            if (mNvramInfoToDisplayCopy != null && notifyDatasetChanged) {
                ((NVRAMDataRecyclerViewAdapter) mAdapter).setEntryList(mNvramInfoToDisplayCopy);
                mAdapter.notifyDataSetChanged();
            }
        } else {
            //Share action
            final Map<Object, Object> nvramInfo = ((NVRAMDataRecyclerViewAdapter) mAdapter).getNvramInfo();
            if (nvramInfo == null || nvramInfo.isEmpty()) {
                Crouton.makeText(mParentFragmentActivity, "Nothing to share!", Style.ALERT).show();
                return true;
            }

            Exception exception = null;
            File file = new File(mParentFragmentActivity.getCacheDir(),
                    String.format("nvram_data_%s_%s_%s.txt",
                            mRouter.getUuid(), mRouter.getName(), mRouter.getRemoteIpAddress()));
            OutputStream outputStream = null;
            try {
                outputStream = new BufferedOutputStream(new FileOutputStream(file, false));
                outputStream.write(PROPERTIES_JOINER_TO_FILE.join(nvramInfo).getBytes());
            } catch (IOException e) {
                exception = e;
                e.printStackTrace();
            } finally {
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (exception != null) {
                Crouton.makeText(mParentFragmentActivity,
                        "Error while trying to share file - please try again later",
                        Style.ALERT).show();
                return true;
            }

            setShareFile(file);

            return true;
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
                        final Integer currentSort = sortIds.inverse().get(mParentFragmentPreferences.getInt(getFormattedPrefKey(SORT), -1));
                        if (currentSort == null || currentSort <= 0) {
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

        layout.findViewById(R.id.tile_admin_nvram_menu).setVisibility(View.VISIBLE);
        layout.findViewById(R.id.tile_admin_nvram_toolbar).setVisibility(View.VISIBLE);

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

            //Filter out by search and sort preferences
            final String textToFind = mParentFragmentPreferences != null ?
                    mParentFragmentPreferences.getString(getFormattedPrefKey(LAST_SEARCH), null) : null;

            if (isNullOrEmpty(textToFind)) {
                ((NVRAMDataRecyclerViewAdapter) mAdapter).setEntryList(mNvramInfoToDisplay);
                mAdapter.notifyDataSetChanged();
            } else {

                //Filter out (and sort by user-preference)
                final Properties mNvramInfoDefaultSortingData = mNvramInfoDefaultSorting.getData();
                //Update adapter in the preferences
                final Map<Object, Object> mNvramInfoToDisplayCopy;
                if (mParentFragmentPreferences != null) {
                    final Integer currentSort = sortIds.inverse()
                            .get(mParentFragmentPreferences.getInt(getFormattedPrefKey(SORT), -1));
                    if (currentSort == null || currentSort <= 0) {
                        mNvramInfoToDisplayCopy = new HashMap<>();
                    } else {
                        switch (currentSort) {
                            case R.id.tile_admin_nvram_sort_asc:
                                //asc
                                mNvramInfoToDisplayCopy = new TreeMap<>(COMPARATOR_STRING_CASE_INSENSITIVE);
                                break;
                            case R.id.tile_admin_nvram_sort_desc:
                                //desc
                                mNvramInfoToDisplayCopy = new TreeMap<>(COMPARATOR_REVERSE_STRING_CASE_INSENSITIVE);
                                break;
                            case R.id.tile_admin_nvram_sort_default:
                            default:
                                mNvramInfoToDisplayCopy = new HashMap<>();
                                break;
                        }
                    }
                } else {
                    mNvramInfoToDisplayCopy = new HashMap<>();
                }

                //noinspection ConstantConditions
                for (Map.Entry<Object, Object> entry :
                        (mParentFragmentPreferences == null ? mNvramInfoToDisplay : mNvramInfoDefaultSortingData).entrySet()) {
                    final Object key = entry.getKey();
                    final Object value = entry.getValue();
                    if (key == null) {
                        continue;
                    }
                    if (containsIgnoreCase(key.toString(), textToFind) || containsIgnoreCase(value.toString(), textToFind)) {
                        mNvramInfoToDisplayCopy.put(key, value);
                    }
                }

                ((NVRAMDataRecyclerViewAdapter) mAdapter).setEntryList(mNvramInfoToDisplayCopy);
                mAdapter.notifyDataSetChanged();
            }

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
