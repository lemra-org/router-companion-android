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
 * Contact Info: Armel Soro <armel+router_companion@rm3l.org>
 */

package org.rm3l.router_companion.tiles.admin.nvram;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.router_companion.RouterCompanionAppConstants.EMPTY_STRING;
import static org.rm3l.router_companion.utils.Utils.fromHtml;

import android.Manifest.permission;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.resources.None;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.tiles.status.router.StatusRouterSpaceUsageTile;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.PermissionsUtils;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style;
import org.rm3l.router_companion.widgets.RecyclerViewEmptySupport;

public class AdminNVRAMTile extends DDWRTTile<None> implements PopupMenu.OnMenuItemClickListener {

  static final String NVRAM_SIZE = AdminNVRAMTile.class.getSimpleName() + "::nvram_size";

  private static final Comparator<Object> COMPARATOR_STRING_CASE_INSENSITIVE =
      (o1, o2) -> {
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
      };

  private static final Comparator<Object> COMPARATOR_REVERSE_STRING_CASE_INSENSITIVE =
      (o1, o2) -> {
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
      };

  public static final String SORT = "sort";

  private static final Joiner.MapJoiner PROPERTIES_JOINER_TO_FILE =
      Joiner.on('\n').withKeyValueSeparator("=");

  private static final String LOG_TAG = AdminNVRAMTile.class.getSimpleName();

  private static final String LAST_SEARCH = "lastSearch";

  private final FloatingActionButton addNewButton;

  private final NVRAMDataRecyclerViewAdapter mAdapter;

  private long mLastSync;

  private final RecyclerView.LayoutManager mLayoutManager;

  private final NVRAMInfo mNvramInfoDefaultSorting;

  private Map<Object, Object> mNvramInfoToDisplay = new HashMap<>();

  private final RecyclerViewEmptySupport mRecyclerView;

  private final BiMap<Integer, Integer> sortIds = HashBiMap.create();

  public AdminNVRAMTile(
      @NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
    super(parentFragment, arguments, router, R.layout.tile_admin_nvram, null);

    sortIds.put(R.id.tile_admin_nvram_sort_default, 11);
    sortIds.put(R.id.tile_admin_nvram_sort_asc, 12);
    sortIds.put(R.id.tile_admin_nvram_sort_desc, 13);

    this.mNvramInfoDefaultSorting = new NVRAMInfo();
    mRecyclerView = layout.findViewById(R.id.tile_admin_nvram_ListView);

    // use this setting to improve performance if you know that changes
    // in content do not change the layout size of the RecyclerView
    // allows for optimizations if all items are of the same size:
    mRecyclerView.setHasFixedSize(true);

    // use a linear layout manager
    mLayoutManager =
        new LinearLayoutManager(mParentFragmentActivity, LinearLayoutManager.VERTICAL, false);
    mLayoutManager.scrollToPosition(0);
    mRecyclerView.setLayoutManager(mLayoutManager);

    final TextView emptyView = layout.findViewById(R.id.empty_view);
    if (ColorUtils.Companion.isThemeLight(mParentFragmentActivity)) {
      emptyView.setTextColor(ContextCompat.getColor(mParentFragmentActivity, R.color.black));
    } else {
      emptyView.setTextColor(ContextCompat.getColor(mParentFragmentActivity, R.color.white));
    }
    mRecyclerView.setEmptyView(emptyView);

    // specify an adapter (see also next example)
    mAdapter =
        new NVRAMDataRecyclerViewAdapter(mParentFragmentActivity, router, mNvramInfoDefaultSorting);
    mAdapter.setHasStableIds(true);
    mRecyclerView.setAdapter(mAdapter);

    final Display display = mParentFragmentActivity.getWindowManager().getDefaultDisplay();
    final Point size = new Point();
    display.getSize(size);
    int width = size.x;
    int height = size.y;
    Log.d(LOG_TAG, "<width,height> = <" + width + "," + height + ">");
    mRecyclerView.setMinimumHeight(size.y);

    addNewButton = layout.findViewById(R.id.nvram_var_add);

    addNewButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            final DialogFragment editFragment =
                AddNVRAMKeyValueDialogFragment.newInstance(mAdapter);
            editFragment.show(
                mParentFragmentActivity.getSupportFragmentManager(),
                "AddNVRAMKeyValueDialogFragment");
          }
        });

    // Create Options Menu
    final ImageButton tileMenu = layout.findViewById(R.id.tile_admin_nvram_menu);

    if (!ColorUtils.Companion.isThemeLight(mParentFragmentActivity)) {
      // Set menu background to white
      tileMenu.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
    }

    tileMenu.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            final PopupMenu popup = new PopupMenu(mParentFragmentActivity, v);
            popup.setOnMenuItemClickListener(AdminNVRAMTile.this);
            final MenuInflater inflater = popup.getMenuInflater();

            final Menu menu = popup.getMenu();

            inflater.inflate(R.menu.tile_admin_nvram_options, menu);

            // Disable menu item from preference
            Integer currentSort = null;
            if (mParentFragmentPreferences != null) {
              currentSort =
                  sortIds
                      .inverse()
                      .get(
                          mParentFragmentPreferences.getInt(
                              getFormattedPrefKey(SORT),
                              sortIds.get(R.id.tile_admin_nvram_sort_default)));
            }

            if (currentSort == null) {
              currentSort = R.id.tile_admin_nvram_sort_default;
            }

            final MenuItem currentSortMenuItem = menu.findItem(currentSort);
            if (currentSortMenuItem != null) {
              currentSortMenuItem.setEnabled(false);
            }
            popup.show();
          }
        });

    // Handle for Search EditText
    final EditText filterEditText =
            this.layout.findViewById(R.id.tile_admin_nvram_filter);
    // Initialize with existing search data
    filterEditText.setText(
        mParentFragmentPreferences != null
            ? mParentFragmentPreferences.getString(getFormattedPrefKey(LAST_SEARCH), EMPTY_STRING)
            : EMPTY_STRING);

    filterEditText.setOnTouchListener(
        new View.OnTouchListener() {
          @Override
          public boolean onTouch(View v, MotionEvent event) {
            final int DRAWABLE_LEFT = 0;
            final int DRAWABLE_TOP = 1;
            final int DRAWABLE_RIGHT = 2;
            final int DRAWABLE_BOTTOM = 3;

            if (event.getAction() == MotionEvent.ACTION_UP) {
              if (event.getRawX()
                  >= (filterEditText.getRight()
                      - filterEditText
                          .getCompoundDrawables()[DRAWABLE_RIGHT]
                          .getBounds()
                          .width())) {
                // 'Clear' button - clear data, and reset everything out
                // Reset everything
                filterEditText.setText(EMPTY_STRING);

                final Properties mNvramInfoDefaultSortingData = mNvramInfoDefaultSorting.getData();
                // Update adapter in the preferences
                if (mParentFragmentPreferences != null) {
                  final Integer currentSort =
                      sortIds
                          .inverse()
                          .get(mParentFragmentPreferences.getInt(getFormattedPrefKey(SORT), -1));
                  if (currentSort == null || currentSort <= 0) {
                    mNvramInfoToDisplay = new HashMap<>(mNvramInfoDefaultSortingData);
                  } else {
                    if (R.id.tile_admin_nvram_sort_asc == currentSort) { // asc
                      mNvramInfoToDisplay = new TreeMap<>(COMPARATOR_STRING_CASE_INSENSITIVE);

                    } else if (R.id.tile_admin_nvram_sort_desc == currentSort) { // desc
                      mNvramInfoToDisplay =
                          new TreeMap<>(COMPARATOR_REVERSE_STRING_CASE_INSENSITIVE);

                    } else {
                      mNvramInfoToDisplay = new HashMap<>();
                    }
                    mNvramInfoToDisplay.putAll(mNvramInfoDefaultSortingData);
                  }
                } else {
                  mNvramInfoToDisplay = new HashMap<>(mNvramInfoDefaultSortingData);
                }

                mAdapter.setEntryList(mNvramInfoToDisplay);
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

    filterEditText.setOnEditorActionListener(
        new TextView.OnEditorActionListener() {
          @Override
          public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
              ProgressDialog progress = null;
              try {
                progress = new ProgressDialog(mParentFragmentActivity);
                progress.setTitle("Searching");
                progress.setMessage("Please hold on...");
                progress.setCanceledOnTouchOutside(true);
                progress.show();

                final String textToFind = filterEditText.getText().toString();

                if (isNullOrEmpty(textToFind)) {
                  // extra-check, even though we can be pretty sure the button is enabled only if
                  // textToFind is present
                  return true;
                }
                final String existingSearch =
                    mParentFragmentPreferences != null
                        ? mParentFragmentPreferences.getString(
                            getFormattedPrefKey(LAST_SEARCH), null)
                        : null;

                if (mParentFragmentPreferences != null) {
                  if (textToFind.equalsIgnoreCase(existingSearch)) {
                    // No need to go further as this is already the string we are looking for
                    return true;
                  }
                  final SharedPreferences.Editor editor = mParentFragmentPreferences.edit();
                  editor.putString(getFormattedPrefKey(LAST_SEARCH), textToFind);
                  editor.apply();
                }

                // Filter out (and sort by user-preference)
                final Properties mNvramInfoDefaultSortingData = mNvramInfoDefaultSorting.getData();
                // Update adapter in the preferences
                final Map<Object, Object> mNvramInfoToDisplayCopy;
                if (mParentFragmentPreferences != null) {
                  final Integer currentSort =
                      sortIds
                          .inverse()
                          .get(mParentFragmentPreferences.getInt(getFormattedPrefKey(SORT), -1));
                  if (currentSort == null || currentSort <= 0) {
                    mNvramInfoToDisplayCopy = new HashMap<>(mNvramInfoDefaultSortingData);
                  } else {
                    if (R.id.tile_admin_nvram_sort_asc == currentSort) { // asc
                      mNvramInfoToDisplayCopy = new TreeMap<>(COMPARATOR_STRING_CASE_INSENSITIVE);

                    } else if (R.id.tile_admin_nvram_sort_desc == currentSort) { // desc
                      mNvramInfoToDisplayCopy =
                          new TreeMap<>(COMPARATOR_REVERSE_STRING_CASE_INSENSITIVE);

                    } else {
                      mNvramInfoToDisplayCopy = new HashMap<>();
                    }
                    //noinspection ConstantConditions
                    for (Map.Entry<Object, Object> entry :
                        mNvramInfoDefaultSortingData.entrySet()) {
                      final Object key = entry.getKey();
                      final Object value = entry.getValue();
                      if (key == null) {
                        continue;
                      }
                      if (key.toString().toLowerCase().contains(textToFind.toLowerCase())
                          || value.toString().toLowerCase().contains(textToFind.toLowerCase())) {
                        mNvramInfoToDisplayCopy.put(key, value);
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
                    if (key.toString().toLowerCase().contains(textToFind.toLowerCase())
                        || value.toString().toLowerCase().contains(textToFind.toLowerCase())) {
                      mNvramInfoToDisplayCopy.put(key, value);
                    }
                  }
                }

                mAdapter.setEntryList(mNvramInfoToDisplayCopy);
                mAdapter.notifyDataSetChanged();

                return true;
              } finally {
                if (progress != null) {
                  progress.dismiss();
                }
              }
            }
            return false;
          }
        });
  }

  public boolean canChildScrollUp() {
    final boolean canScrollVertically = ViewCompat.canScrollVertically(mRecyclerView, -1);
    if (!canScrollVertically) {
      return canScrollVertically;
    }

    // TODO ScrollView can scroll vertically,
    // but detect whether the touch was done outside of the scroll view
    // (in which case we should return false)

    return canScrollVertically;
  }

  @Override
  public Integer getTileHeaderViewId() {
    return R.id.tile_admin_nvram_hdr;
  }

  @Override
  public Integer getTileTitleViewId() {
    return R.id.tile_admin_nvram_title;
  }

  @Override
  public boolean isEmbeddedWithinScrollView() {
    return false;
  }

  @Override
  public void onLoadFinished(Loader<None> loader, None data) {

    try {
      // Set tiles
      FirebaseCrashlytics.getInstance().log("onLoadFinished: loader=" + loader + " / data=" + data);

      layout.findViewById(R.id.tile_admin_nvram_menu).setVisibility(View.VISIBLE);
      layout.findViewById(R.id.tile_admin_nvram_toolbar).setVisibility(View.VISIBLE);

      if (data == null || mNvramInfoToDisplay.isEmpty()) {
        data = (None) new None().setException(new DDWRTNoDataException("No Data!"));
      }

      layout.findViewById(R.id.tile_admin_nvram_loading_view).setVisibility(View.GONE);

      final TextView errorPlaceHolderView =
              this.layout.findViewById(R.id.tile_admin_nvram_error);

      final Exception exception = data.getException();

      // NVRAM
      final Object nvramSize = mNvramInfoToDisplay.remove(NVRAM_SIZE);
      ((TextView) this.layout.findViewById(R.id.tile_admin_nvram_size))
          .setText(nvramSize != null ? nvramSize.toString() : "-");

      if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

        if (exception == null) {
          errorPlaceHolderView.setVisibility(View.GONE);
        }

        // Filter out by search and sort preferences
        final String textToFind =
            mParentFragmentPreferences != null
                ? mParentFragmentPreferences.getString(getFormattedPrefKey(LAST_SEARCH), null)
                : null;

        if (isNullOrEmpty(textToFind)) {
          mAdapter.setEntryList(mNvramInfoToDisplay);
          mAdapter.notifyDataSetChanged();
        } else {

          // Filter out (and sort by user-preference)
          final Properties mNvramInfoDefaultSortingData = mNvramInfoDefaultSorting.getData();
          // Update adapter in the preferences
          final Map<Object, Object> mNvramInfoToDisplayCopy;
          if (mParentFragmentPreferences != null) {
            final Integer currentSort =
                sortIds
                    .inverse()
                    .get(mParentFragmentPreferences.getInt(getFormattedPrefKey(SORT), -1));
            if (currentSort == null || currentSort <= 0) {
              mNvramInfoToDisplayCopy = new HashMap<>();
            } else {
              if (R.id.tile_admin_nvram_sort_asc == currentSort) { // asc
                mNvramInfoToDisplayCopy = new TreeMap<>(COMPARATOR_STRING_CASE_INSENSITIVE);

              } else if (R.id.tile_admin_nvram_sort_desc == currentSort) { // desc
                mNvramInfoToDisplayCopy = new TreeMap<>(COMPARATOR_REVERSE_STRING_CASE_INSENSITIVE);

              } else {
                mNvramInfoToDisplayCopy = new HashMap<>();
              }
            }
          } else {
            mNvramInfoToDisplayCopy = new HashMap<>();
          }

          //noinspection ConstantConditions
          for (Map.Entry<Object, Object> entry :
              (mParentFragmentPreferences == null
                      ? mNvramInfoToDisplay
                      : mNvramInfoDefaultSortingData)
                  .entrySet()) {
            final Object key = entry.getKey();
            final Object value = entry.getValue();
            if (key == null) {
              continue;
            }
            if (key.toString().toLowerCase().contains(textToFind.toLowerCase())
                || value.toString().toLowerCase().contains(textToFind.toLowerCase())) {
              mNvramInfoToDisplayCopy.put(key, value);
            }
          }

          mAdapter.setEntryList(mNvramInfoToDisplayCopy);
          mAdapter.notifyDataSetChanged();
        }

        // Update last sync
        final RelativeTimeTextView lastSyncView =
                layout.findViewById(R.id.tile_last_sync);
        lastSyncView.setReferenceTime(mLastSync);
        lastSyncView.setPrefix("Last sync: ");
      }

      if (exception != null && !(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {
        //noinspection ThrowableResultOfMethodCallIgnored
        final Throwable rootCause = Throwables.getRootCause(exception);
        errorPlaceHolderView.setText(
            "Error: " + (rootCause != null ? rootCause.getMessage() : "null"));
        final Context parentContext = this.mParentFragmentActivity;
        errorPlaceHolderView.setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(final View v) {
                //noinspection ThrowableResultOfMethodCallIgnored
                if (rootCause != null) {
                  Toast.makeText(parentContext, rootCause.getMessage(), Toast.LENGTH_LONG).show();
                }
              }
            });
        errorPlaceHolderView.setVisibility(View.VISIBLE);
        updateProgressBarWithError();
      } else if (exception == null) {
        updateProgressBarWithSuccess();
      }

      FirebaseCrashlytics.getInstance().log("onLoadFinished(): done loading!");
    } finally {
      mRefreshing.set(false);
      doneWithLoaderInstance(this, loader);
    }
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    final int itemId = item.getItemId();
    // Store current value in preferences
    if (itemId == R.id.tile_admin_nvram_sort_default
        || itemId == R.id.tile_admin_nvram_sort_asc
        || itemId == R.id.tile_admin_nvram_sort_desc) {
      item.setEnabled(false);
      // Store in preferences if any
      final Integer currentSort;
      if (mParentFragmentPreferences != null) {
        currentSort =
            sortIds.inverse().get(mParentFragmentPreferences.getInt(getFormattedPrefKey(SORT), -1));
      } else {
        currentSort = null;
      }

      if (mParentFragmentPreferences != null && (currentSort == null || currentSort != itemId)) {
        // No pref on file or different pref on file => store in preferences
        final SharedPreferences.Editor editor = mParentFragmentPreferences.edit();
        editor.putInt(getFormattedPrefKey(SORT), sortIds.get(itemId));
        editor.apply();
      }
    }

    if (itemId != R.id.tile_admin_nvram_share) {
      Map<Object, Object> mNvramInfoToDisplayCopy = null;

      boolean notifyDatasetChanged = false;

      // Filter out by search and sort preferences
      final String textToFind =
          mParentFragmentPreferences != null
              ? mParentFragmentPreferences.getString(getFormattedPrefKey(LAST_SEARCH), null)
              : null;
      if (isNullOrEmpty(textToFind)) {
        // Filter on while dataset
        final Properties mNvramInfoDefaultSortingData = mNvramInfoDefaultSorting.getData();
        if (itemId == R.id.tile_admin_nvram_sort_default) {
          mNvramInfoToDisplay = new HashMap<>(mNvramInfoDefaultSortingData);
          notifyDatasetChanged = true;

        } else if (itemId == R.id.tile_admin_nvram_sort_asc) {
          mNvramInfoToDisplay = new TreeMap<>(COMPARATOR_STRING_CASE_INSENSITIVE);
          mNvramInfoToDisplay.putAll(mNvramInfoDefaultSortingData);
          notifyDatasetChanged = true;

        } else if (itemId == R.id.tile_admin_nvram_sort_desc) {
          mNvramInfoToDisplay = new TreeMap<>(COMPARATOR_REVERSE_STRING_CASE_INSENSITIVE);
          mNvramInfoToDisplay.putAll(mNvramInfoDefaultSortingData);
          notifyDatasetChanged = true;

        } else {
        }
        mNvramInfoToDisplayCopy = mNvramInfoToDisplay;
      } else {
        // Already filtered data
        final Map<Object, Object> adapterNvramInfo =
            mAdapter.getNvramInfo();
        if (itemId == R.id.tile_admin_nvram_sort_default) {
          mNvramInfoToDisplayCopy = new HashMap<>(adapterNvramInfo);
          notifyDatasetChanged = true;

        } else if (itemId == R.id.tile_admin_nvram_sort_asc) {
          mNvramInfoToDisplayCopy = new TreeMap<>(COMPARATOR_STRING_CASE_INSENSITIVE);
          mNvramInfoToDisplayCopy.putAll(adapterNvramInfo);
          notifyDatasetChanged = true;

        } else if (itemId == R.id.tile_admin_nvram_sort_desc) {
          mNvramInfoToDisplayCopy = new TreeMap<>(COMPARATOR_REVERSE_STRING_CASE_INSENSITIVE);
          mNvramInfoToDisplayCopy.putAll(adapterNvramInfo);
          notifyDatasetChanged = true;

        } else {
        }
      }

      if (mNvramInfoToDisplayCopy != null && notifyDatasetChanged) {
        mAdapter.setEntryList(mNvramInfoToDisplayCopy);
        mAdapter.notifyDataSetChanged();
      }
    } else {

      PermissionsUtils.requestPermissions(
          mParentFragmentActivity,
          Collections.singletonList(permission.WRITE_EXTERNAL_STORAGE),
          () -> {
            // Share action
            final Map<Object, Object> nvramInfo =
                mAdapter.getNvramInfo();
            if (nvramInfo == null || nvramInfo.isEmpty()) {
              Utils.displayMessage(mParentFragmentActivity, "Nothing to share!", Style.ALERT);
              return null;
            }

            Exception exception = null;
            File file =
                new File(
                    mParentFragmentActivity.getCacheDir(),
                    Utils.getEscapedFileName(
                            String.format(
                                "nvram_data_%s_%s_%s",
                                mRouter.getUuid(), mRouter.getName(), mRouter.getRemoteIpAddress()))
                        + ".txt");
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
              Utils.displayMessage(
                  mParentFragmentActivity,
                  "Error while trying to share file - please try again later",
                  Style.ALERT);
              return null;
            }

            final Uri uriForFile =
                FileProvider.getUriForFile(
                    mParentFragmentActivity,
                    RouterCompanionAppConstants.FILEPROVIDER_AUTHORITY,
                    file);

            final Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);
            sendIntent.putExtra(
                Intent.EXTRA_SUBJECT,
                String.format(
                    "NVRAM Variables from Router '%s'", mRouter.getCanonicalHumanReadableName()));

            sendIntent.putExtra(Intent.EXTRA_TEXT, fromHtml(Utils.getShareIntentFooter()));

            sendIntent.setDataAndType(uriForFile, "text/html");
            sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            mParentFragmentActivity.startActivity(
                Intent.createChooser(
                    sendIntent, mParentFragmentActivity.getResources().getText(R.string.send_to)));
            return null;
          },
          () -> null,
          "Storage access permission is needed, so you can easily share NVRAM data");
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
          FirebaseCrashlytics.getInstance()
              .log(
                  "Init background loader for "
                      + AdminNVRAMTile.class
                      + ": routerInfo="
                      + mRouter
                      + " / nbRunsLoader="
                      + nbRunsLoader);

          if (mRefreshing.getAndSet(true)) {
            return (None) new None().setException(new DDWRTTileAutoRefreshNotAllowedException());
          }
          nbRunsLoader++;

          updateProgressBarViewSeparator(0);

          mLastSync = System.currentTimeMillis();

          mNvramInfoToDisplay.clear();
          mNvramInfoDefaultSorting.clear();

          NVRAMInfo nvramInfoTmp = null;

          try {
            updateProgressBarViewSeparator(10);
            nvramInfoTmp =
                SSHUtils.getNVRamInfoFromRouter(
                    mParentFragmentActivity, mRouter, mGlobalPreferences);
          } finally {
            if (nvramInfoTmp != null) {
              mNvramInfoDefaultSorting.putAll(nvramInfoTmp);
            }

            updateProgressBarViewSeparator(35);
            final String[] nvramSize =
                SSHUtils.getManualProperty(
                    mParentFragmentActivity,
                    mRouter,
                    mGlobalPreferences,
                    "/usr/sbin/nvram show 2>&1 1>/dev/null");
            if (nvramSize != null && nvramSize.length > 0) {
              final List<String> nvramUsageList =
                  StatusRouterSpaceUsageTile.NVRAM_SIZE_SPLITTER.splitToList(nvramSize[0]);
              if (nvramUsageList != null && !nvramUsageList.isEmpty()) {
                final String value = nvramUsageList.get(0);
                if (value != null) {
                  mNvramInfoDefaultSorting.setProperty(NVRAM_SIZE, value);
                }
              }
            }
          }

          updateProgressBarViewSeparator(60);

          if (mNvramInfoDefaultSorting.isEmpty()) {
            throw new DDWRTNoDataException("No Data!");
          }

          // Now apply sorting here (as per user-preferences)

          final Properties defaultNVRAMInfo = mNvramInfoDefaultSorting.getData();
          if (mParentFragmentPreferences != null) {
            final Integer currentSort =
                sortIds
                    .inverse()
                    .get(mParentFragmentPreferences.getInt(getFormattedPrefKey(SORT), -1));
            if (currentSort == null || currentSort <= 0) {
              mNvramInfoToDisplay = new HashMap<>(defaultNVRAMInfo);
            } else {
              if (R.id.tile_admin_nvram_sort_asc == currentSort) { // asc
                mNvramInfoToDisplay = new TreeMap<>(COMPARATOR_STRING_CASE_INSENSITIVE);
                mNvramInfoToDisplay.putAll(defaultNVRAMInfo);

              } else if (R.id.tile_admin_nvram_sort_desc == currentSort) { // desc
                mNvramInfoToDisplay = new TreeMap<>(COMPARATOR_REVERSE_STRING_CASE_INSENSITIVE);
                mNvramInfoToDisplay.putAll(defaultNVRAMInfo);

              } else {
                mNvramInfoToDisplay = new HashMap<>(defaultNVRAMInfo);
              }
            }
          } else {
            mNvramInfoToDisplay = new HashMap<>(defaultNVRAMInfo);
          }

          updateProgressBarViewSeparator(90);

          return new None();
        } catch (@NonNull final Exception e) {
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
  protected OnClickIntent getOnclickIntent() {
    return null;
  }
}
