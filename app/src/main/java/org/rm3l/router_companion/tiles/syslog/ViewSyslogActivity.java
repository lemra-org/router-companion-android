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
package org.rm3l.router_companion.tiles.syslog;

import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.router_companion.utils.Utils.fromHtml;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.PermissionChecker;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import needle.UiRelatedTask;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.common.utils.ViewIDUtils;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.multithreading.MultiThreadingManager;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.snackbar.SnackbarCallback;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style;
import org.rm3l.router_companion.widgets.RecyclerViewEmptySupport;

public class ViewSyslogActivity extends AppCompatActivity
        implements SwipeRefreshLayout.OnRefreshListener, SearchView.OnQueryTextListener {

    static class RouterSyslogRecyclerViewAdapter
            extends RecyclerView.Adapter<RouterSyslogRecyclerViewAdapter.ViewHolder>
            implements Filterable {

        static class ViewHolder extends RecyclerView.ViewHolder {

            public final TextView itemView;

            public ViewHolder(TextView itemView) {
                super(itemView);
                this.itemView = itemView;
            }
        }

        private final ViewSyslogActivity activity;

        private List<? extends CharSequence> mLogs;

        RouterSyslogRecyclerViewAdapter(final ViewSyslogActivity activity) {
            this(activity, null);
        }

        RouterSyslogRecyclerViewAdapter(final ViewSyslogActivity activity,
                final List<CharSequence> mLogs) {
            this.activity = activity;
            this.setLogs(mLogs);
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(final CharSequence constraint) {
                    activity.setRefreshingState(true);
                    final FilterResults oReturn = new FilterResults();
                    if (mLogs == null || mLogs.isEmpty()) {
                        return oReturn;
                    }

                    if (TextUtils.isEmpty(constraint)) {
                        oReturn.values = mLogs;
                    } else {
                        //Filter aliases list
                        oReturn.values = FluentIterable.from(mLogs).filter(new Predicate<CharSequence>() {
                            @Override
                            public boolean apply(CharSequence input) {
                                return input != null && input.toString()
                                        .toLowerCase()
                                        .contains(constraint.toString().toLowerCase());
                            }
                        }).transform(new Function<CharSequence, Spanned>() {
                            @javax.annotation.Nullable
                            @Override
                            public Spanned apply(@javax.annotation.Nullable CharSequence input) {
                                return StatusSyslogTile.findAndHighlightOutput(input == null ? "" : input,
                                        constraint.toString());
                            }
                        }).toList();
                    }

                    return oReturn;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    final Object values = results.values;
                    if (values instanceof List) {
                        //noinspection unchecked
                        setLogs((List<? extends CharSequence>) values);
                        notifyDataSetChanged();
                    }
                    activity.setRefreshingState(false);
                }
            };
        }

        @Override
        public int getItemCount() {
            return mLogs != null ? mLogs.size() : 0;
        }

        @Override
        public long getItemId(int position) {
            final CharSequence itemAt;
            if (mLogs == null || (itemAt = mLogs.get(position)) == null || itemAt.toString().isEmpty()) {
                return super.getItemId(position);
            }
            return ViewIDUtils.getStableId(ViewSyslogActivity.class, itemAt.toString());
        }

        @Override
        public void onBindViewHolder(RouterSyslogRecyclerViewAdapter.ViewHolder holder, int position) {
            if (mLogs == null || mLogs.isEmpty()) {
                Toast.makeText(activity, "No data. Please try again later", Toast.LENGTH_SHORT).show();
                return;
            }
            if (position < 0 || position >= mLogs.size()) {
                Utils.reportException(null,
                        new IllegalStateException("position(" + position + " < 0 || position >= logs.size()"));
                Toast.makeText(activity, "Internal Error. Please try again later", Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            final CharSequence log = mLogs.get(position);
            holder.itemView.setText(log);
        }

        @Override
        public RouterSyslogRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                int viewType) {
            final TextView logLineView = new TextView(parent.getContext());
            logLineView.setTextColor(ContextCompat.getColor(activity, R.color.white));
            logLineView.setTypeface(Typeface.MONOSPACE);
            return new ViewHolder(logLineView);
        }

        public RouterSyslogRecyclerViewAdapter setLogs(List<? extends CharSequence> logs) {
            this.mLogs = (logs != null ? logs : new ArrayList<CharSequence>());
            return this;
        }

        public void toggleSorting() {
            setLogs(Lists.reverse(mLogs));
        }
    }

    public static final String FILTER_TEXT = "FILTER_TEXT";

    public static final String WINDOW_TITLE = "WINDOW_TITLE";

    public static final String FULL_LOGS_RETRIEVAL_COMMAND = "FULL_LOGS_RETRIEVAL_COMMAND";

    private static final String LOG_TAG = ViewSyslogActivity.class.getSimpleName();

    private RouterSyslogRecyclerViewAdapter mAdapter;

    private File mFileToShare;

    private String mFullLogsRetrievalCommand;

    private SharedPreferences mGlobalPreferences;

    private String mInitialFilterText;

    private LinearLayoutManager mLayoutManager;

    private final AtomicReference<List<? extends CharSequence>> mLogsAtomicRef =
            new AtomicReference<>();

    private RecyclerViewEmptySupport mRecyclerView;

    private Router mRouter;

    private String mRouterUuid;

    private SearchView mSearchView;

    private ShareActionProvider mShareActionProvider;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private Toolbar mToolbar;

    private String mWindowTitleFromBundle;

    private Menu optionsMenu;

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        mRouterUuid = intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);

        mRouter = RouterManagementActivity.getDao(this).getRouter(mRouterUuid);
        if (mRouter == null) {
            Toast.makeText(this, "Whoops - Router not found. Has it been deleted?", Toast.LENGTH_SHORT)
                    .show();
            Utils.reportException(null, new IllegalStateException("Router not found"));
            finish();
            return;
        }
        this.mGlobalPreferences =
                getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);

        //        final boolean themeLight = ColorUtils.isThemeLight(this);
        ColorUtils.Companion.setAppTheme(this, mRouter.getRouterFirmware(), false);
        //        if (themeLight) {
        //            //Light
        //            setTheme(R.style.AppThemeLight);
        //        } else {
        //            //Default is Dark
        //            setTheme(R.style.AppThemeDark);
        //        }

        setContentView(R.layout.tile_status_syslog_full);

        mFullLogsRetrievalCommand = intent.getStringExtra(FULL_LOGS_RETRIEVAL_COMMAND);

        mInitialFilterText = intent.getStringExtra(FILTER_TEXT);

        handleIntent(getIntent());

        mToolbar = (Toolbar) findViewById(R.id.tile_status_syslog_full_view_toolbar);
        if (mToolbar != null) {
            mWindowTitleFromBundle = intent.getStringExtra(WINDOW_TITLE);
            mToolbar.setTitle(
                    TextUtils.isEmpty(mWindowTitleFromBundle) ? "Logs" : mWindowTitleFromBundle);
            mToolbar.setSubtitle(
                    String.format("%s (%s:%d)", mRouter.getDisplayName(), mRouter.getRemoteIpAddress(),
                            mRouter.getRemotePort()));
            mToolbar.setTitleTextAppearance(getApplicationContext(), R.style.ToolbarTitle);
            mToolbar.setSubtitleTextAppearance(getApplicationContext(), R.style.ToolbarSubtitle);
            mToolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
            mToolbar.setSubtitleTextColor(ContextCompat.getColor(this, R.color.white));
            setSupportActionBar(mToolbar);
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        mRecyclerView =
                (RecyclerViewEmptySupport) findViewById(R.id.tile_status_syslog_full_recyclerview);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        // allows for optimizations if all items are of the same size:
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.scrollToPosition(0);
        mRecyclerView.setLayoutManager(mLayoutManager);

        final TextView emptyView = (TextView) findViewById(R.id.empty_view);
        if (ColorUtils.Companion.isThemeLight(this)) {
            emptyView.setTextColor(ContextCompat.getColor(this, R.color.black));
        } else {
            emptyView.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
        mRecyclerView.setEmptyView(emptyView);

        // specify an adapter (see also next example)
        mAdapter = new RouterSyslogRecyclerViewAdapter(this);
        mAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(mAdapter);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light, android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int firstVisibleItem,
                    int visibleItemCount) {
                boolean enable = false;
                if (recyclerView != null && recyclerView.getChildCount() > 0) {
                    final LinearLayoutManager layoutManager =
                            (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null) {
                        // check if the first item of the list is visible
                        final boolean firstItemVisible = (layoutManager.findFirstVisibleItemPosition() == 0);

                        // check if the top of the first item is visible
                        final View childAt = layoutManager.getChildAt(0);
                        final boolean topOfFirstItemVisible = (childAt != null && childAt.getTop() == 0);

                        // enabling or disabling the refresh layout
                        enable = firstItemVisible && topOfFirstItemVisible;
                    }
                }
                mSwipeRefreshLayout.setEnabled(enable);
            }
        });

        refreshData();
    }

    @Override
    protected void onDestroy() {
        if (mFileToShare != null) {
            //noinspection ResultOfMethodCallIgnored
            mFileToShare.delete();
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
            int[] grantResults) {

        switch (requestCode) {
            case RouterCompanionAppConstants.Permissions.STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Yay! Permission granted for #" + requestCode);
                    if (optionsMenu != null) {
                        final MenuItem menuItem = optionsMenu.findItem(R.id.tile_status_syslog_full_share);
                        menuItem.setEnabled(true);
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Crashlytics.log(Log.WARN, LOG_TAG, "Boo! Permission denied for #" + requestCode);
                    Utils.displayMessage(this, "Sharing of Router logs will be unavailable", Style.INFO);
                    if (optionsMenu != null) {
                        final MenuItem menuItem = optionsMenu.findItem(R.id.tile_status_syslog_full_share);
                        menuItem.setEnabled(false);
                    }
                }
                return;
            }
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tile_status_syslog_full_options, menu);

        this.optionsMenu = menu;

        //Permission requests
        final int rwExternalStoragePermissionCheck =
                PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (rwExternalStoragePermissionCheck != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                SnackbarUtils.buildSnackbar(this, "Storage access is required to share Router logs.", "OK",
                        Snackbar.LENGTH_INDEFINITE, new SnackbarCallback() {
                            @Override
                            public void onDismissEventActionClick(int event, @Nullable Bundle bundle)
                                    throws Exception {
                                //Request permission
                                ActivityCompat.requestPermissions(ViewSyslogActivity.this,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        RouterCompanionAppConstants.Permissions.STORAGE);
                            }

                            @Override
                            public void onDismissEventConsecutive(int event, @Nullable Bundle bundle)
                                    throws Exception {

                            }

                            @Override
                            public void onDismissEventManual(int event, @Nullable Bundle bundle)
                                    throws Exception {

                            }

                            @Override
                            public void onDismissEventSwipe(int event, @Nullable Bundle bundle)
                                    throws Exception {

                            }

                            @Override
                            public void onDismissEventTimeout(int event, @Nullable Bundle bundle)
                                    throws Exception {

                            }

                            @Override
                            public void onShowEvent(@Nullable Bundle bundle) throws Exception {

                            }
                        }, null, true);
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        RouterCompanionAppConstants.Permissions.STORAGE);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        //Search
        final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        mSearchView = (SearchView) menu.findItem(R.id.tile_status_syslog_full_search).getActionView();

        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        mSearchView.setOnQueryTextListener(this);

        // Get the search close button image view
        final ImageView closeButton = (ImageView) mSearchView.findViewById(R.id.search_close_btn);
        if (closeButton != null) {
            // Set on click listener
            closeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Reset views
                    setRefreshingState(true);
                    mAdapter.setLogs(mLogsAtomicRef.get());
                    mAdapter.notifyDataSetChanged();
                    //Hide it now
                    mSearchView.setIconified(true);
                    setRefreshingState(false);
                }
            });
        }

        if (!TextUtils.isEmpty(mInitialFilterText)) {
            if (mSearchView != null) {
                mSearchView.setQuery(mInitialFilterText, true);
            }
        }

        final MenuItem shareMenuItem = menu.findItem(R.id.tile_status_syslog_full_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareMenuItem);
        if (mShareActionProvider == null) {
            mShareActionProvider = new ShareActionProvider(this);
            MenuItemCompat.setActionProvider(shareMenuItem, mShareActionProvider);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.tile_status_syslog_full_sort:
                setRefreshingState(true);
                //Now tell adapter to sort data
                mAdapter.toggleSorting();
                mAdapter.notifyDataSetChanged();
                setRefreshingState(false);
                return true;

            case R.id.menu_refresh:
                refreshData();
                return true;

            case R.id.action_feedback:
                Utils.openFeedbackForm(this, mRouter);
                return true;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextChange(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (TextUtils.isEmpty(query)) {
            setRefreshingState(true);
            mAdapter.setLogs(mLogsAtomicRef.get());
            mAdapter.notifyDataSetChanged();
            setRefreshingState(false);
        } else {
            mAdapter.getFilter().filter(query);
        }
        return true;
    }

    @Override
    public void onRefresh() {
        performAsyncDataLoading();
    }

    public void setRefreshingState(final boolean state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(state);
            }
        });
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            final String query = intent.getStringExtra(SearchManager.QUERY);
            if (query == null) {
                setRefreshingState(true);
                mAdapter.setLogs(mLogsAtomicRef.get());
                mAdapter.notifyDataSetChanged();
                setRefreshingState(false);
                return;
            }
            mAdapter.getFilter().filter(query);
        }
    }

    private void performAsyncDataLoading() {
        //don't forget to call #warmumpForSharing() once everything is done loading
        MultiThreadingManager.getSyslogViewTasksExecutor().execute(new UiRelatedTask<Exception>() {
            @Override
            protected Exception doWork() {
                try {
                    //noinspection ConstantConditions
                    mLogsAtomicRef.set(Arrays.asList(
                            SSHUtils.getManualProperty(ViewSyslogActivity.this, mRouter, mGlobalPreferences,
                                    TextUtils.isEmpty(mFullLogsRetrievalCommand)
                                            ? StatusSyslogTile.CAT_TMP_VAR_LOG_MESSAGES
                                            : mFullLogsRetrievalCommand)));
                    return null;
                } catch (final Exception e) {
                    return e;
                }
            }

            @Override
            protected void thenDoUiRelatedWork(Exception exception) {
                try {
                    if (exception != null) {
                        Toast.makeText(ViewSyslogActivity.this,
                                "Error: " + Throwables.getRootCause(exception).getMessage(), Toast.LENGTH_LONG)
                                .show();
                        Crashlytics.logException(exception);
                    } else {
                        mAdapter.setLogs(mLogsAtomicRef.get());
                        warmumpForSharing();
                        mAdapter.notifyDataSetChanged();
                    }
                } finally {
                    setRefreshingState(false);
                }
            }
        });
    }

    private void refreshData() {
        // Signal SwipeRefreshLayout to start the progress indicator
        setRefreshingState(true);
        this.performAsyncDataLoading();
        if (!TextUtils.isEmpty(mInitialFilterText)) {
            if (mSearchView != null) {
                mSearchView.setQuery(mInitialFilterText, true);
            }
        }
    }

    private void setShareFile(File file) {
        if (mShareActionProvider == null) {
            return;
        }

        final Uri uriForFile =
                FileProvider.getUriForFile(this, RouterCompanionAppConstants.FILEPROVIDER_AUTHORITY, file);

        mShareActionProvider.setOnShareTargetSelectedListener(
                new ShareActionProvider.OnShareTargetSelectedListener() {
                    @Override
                    public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
                        grantUriPermission(intent.getComponent().getPackageName(), uriForFile,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        return true;
                    }
                });

        final Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);

        //sendIntent.setType("text/plain");
        sendIntent.setType("text/html");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, String.format("Full %s for Router '%s'",
                TextUtils.isEmpty(mWindowTitleFromBundle) ? "Logs" : mWindowTitleFromBundle,
                mRouter.getCanonicalHumanReadableName()));

        sendIntent.putExtra(Intent.EXTRA_TEXT,
                fromHtml(String.format("%s", Utils.getShareIntentFooter()).replaceAll("\n", "<br/>")));

        sendIntent.setData(uriForFile);
        sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        setShareIntent(sendIntent);
    }

    // Call to update the share intent
    private void setShareIntent(Intent shareIntent) {
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    private void warmumpForSharing() {

        final List<? extends CharSequence> currentLogs = mLogsAtomicRef.get();
        if (currentLogs == null || currentLogs.isEmpty()) {
            Crashlytics.log(Log.DEBUG, LOG_TAG, "Sharing unavailable - no data");
            return;
        }

        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {

            // permission was granted, yay! Do the
            // contacts-related task you need to do.
            mFileToShare = new File(getCacheDir(),
                    Utils.getEscapedFileName(String.format("Logs_FULL_%s", nullToEmpty(mRouterUuid)))
                            + ".txt");

            Exception exception = null;
            OutputStream outputStream = null;
            try {
                outputStream = new BufferedOutputStream(new FileOutputStream(mFileToShare, false));
                //noinspection ConstantConditions
                outputStream.write(Joiner.on("\n").useForNull("\n").join(currentLogs).getBytes());
                outputStream.flush();
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
                Utils.displayMessage(this,
                        "Error while trying to share Router logs - please try again later", Style.ALERT);
                return;
            }

            setShareFile(mFileToShare);
        }
    }
}
