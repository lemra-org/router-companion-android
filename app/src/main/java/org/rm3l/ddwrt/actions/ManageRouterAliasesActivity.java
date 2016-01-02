package org.rm3l.ddwrt.actions;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.supportv7.widget.decorator.DividerItemDecoration;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdView;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import org.json.JSONObject;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.StorageException;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.resources.MACOUIVendor;
import org.rm3l.ddwrt.resources.RecyclerViewRefreshCause;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.status.wireless.WirelessClientsTile;
import org.rm3l.ddwrt.utils.AdUtils;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.StorageUtils;
import org.rm3l.ddwrt.utils.Utils;
import org.rm3l.ddwrt.utils.snackbar.SnackbarCallback;
import org.rm3l.ddwrt.utils.snackbar.SnackbarUtils;
import org.rm3l.ddwrt.widgets.RecyclerViewEmptySupport;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static de.keyboardsurfer.android.widget.crouton.Style.ALERT;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.rm3l.ddwrt.main.DDWRTMainActivity.IMPORT_ALIASES_FRAGMENT_TAG;
import static org.rm3l.ddwrt.main.DDWRTMainActivity.MAIN_ACTIVITY_ACTION;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.THEMING_PREF;

/**
 * Created by rm3l on 13/12/15.
 */
public class ManageRouterAliasesActivity 
    extends AppCompatActivity 
    implements View.OnClickListener, 
        SwipeRefreshLayout.OnRefreshListener, 
        SearchView.OnQueryTextListener, 
        SnackbarCallback {

    private static final String LOG_TAG = ManageRouterAliasesActivity
            .class.getSimpleName();

    private static final String ADD_OR_EDIT_ROUTER_ALIAS_FRAGMENT = "ADD_OR_EDIT_ROUTER_ALIAS_FRAGMENT";

    private boolean mIsThemeLight;
    private Router mRouter;

    private Toolbar mToolbar;
    private SharedPreferences mRouterPreferences;

    private RecyclerViewEmptySupport mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private FloatingActionButton addNewButton;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private Menu optionsMenu;

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            final RouterAliasesListRecyclerViewAdapter adapter =
                    (RouterAliasesListRecyclerViewAdapter) mAdapter;
            final String query = intent.getStringExtra(SearchManager.QUERY);
            if (query == null) {
                adapter.setAliasesColl(FluentIterable.from(
                        Router.getAliases(this, mRouter))
                .toList());
                adapter.notifyDataSetChanged();
                return;
            }
            adapter.getFilter().filter(query);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIsThemeLight = ColorUtils.isThemeLight(this);
        if (mIsThemeLight) {
            //Light
            setTheme(R.style.AppThemeLight);
            getWindow().getDecorView()
                    .setBackgroundColor(ContextCompat.getColor(this,
                            android.R.color.white));
        } else {
            //Default is Dark
            setTheme(R.style.AppThemeDark);
        }

        setContentView(R.layout.activity_manage_router_aliases);

        final Intent intent = getIntent();

        final String routerSelected =
                intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);
        if (Strings.isNullOrEmpty(routerSelected) ||
                (mRouter = RouterManagementActivity.getDao(this)
                    .getRouter(routerSelected)) == null) {
            Toast.makeText(
                    this, "Missing Router - might have been removed?",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        handleIntent(getIntent());

        mRouterPreferences = getSharedPreferences(routerSelected, Context.MODE_PRIVATE);

        AdUtils.buildAndDisplayAdViewIfNeeded(this, (AdView) findViewById(R.id.router_aliases_list_adView));

        mToolbar = (Toolbar) findViewById(R.id.manageRouterAliasesToolbar);
        if (mToolbar != null) {
            mToolbar.setTitle("Manage Aliases");
            mToolbar.setSubtitle(String.format("%s (%s:%d)",
                    mRouter.getDisplayName(), 
                    mRouter.getRemoteIpAddress(),
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

        mRecyclerView = (RecyclerViewEmptySupport) findViewById(R.id.routerAliasesListView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        // allows for optimizations if all items are of the same size:
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.scrollToPosition(0);
        mRecyclerView.setLayoutManager(mLayoutManager);

        final TextView emptyView = (TextView) findViewById(R.id.empty_view);
        if (mIsThemeLight) {
            emptyView.setTextColor(ContextCompat.getColor(this, R.color.black));
        } else {
            emptyView.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
        mRecyclerView.setEmptyView(emptyView);

        // specify an adapter (see also next example)
        mAdapter = new RouterAliasesListRecyclerViewAdapter(this, mRouter);
        mRecyclerView.setAdapter(mAdapter);

        final RecyclerView.ItemDecoration itemDecoration =
                new DividerItemDecoration(this, LinearLayoutManager.VERTICAL);
        mRecyclerView.addItemDecoration(itemDecoration);

        addNewButton = (FloatingActionButton) findViewById(R.id.router_alias_add);

        addNewButton.setOnClickListener(this);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int firstVisibleItem, int visibleItemCount) {
                boolean enable = false;
                if(recyclerView != null && recyclerView.getChildCount() > 0){
                    final LinearLayoutManager layoutManager = (LinearLayoutManager)
                            recyclerView.getLayoutManager();
                    if (layoutManager != null) {
                        // check if the first item of the list is visible
                        final boolean firstItemVisible =
                                (layoutManager.findFirstVisibleItemPosition() == 0);

                        // check if the top of the first item is visible
                        final View childAt = layoutManager.getChildAt(0);
                        final boolean topOfFirstItemVisible = (childAt != null &&
                                childAt.getTop() == 0);

                        // enabling or disabling the refresh layout
                        enable = firstItemVisible && topOfFirstItemVisible;
                    }
                }
                mSwipeRefreshLayout.setEnabled(enable);
//                super.onScrolled(recyclerView, dx, dy);
            }
        });
    }

    private static void displayRouterAliasDialog(final ManageRouterAliasesActivity activity,
                                                 final String macAddress,
                                          final String currentAlias,
                                          final DialogInterface.OnClickListener onClickListener) {
        final AddOrUpdateRouterAliasDialogFragment fragment = AddOrUpdateRouterAliasDialogFragment
                .newInstance(activity.mRouter, macAddress, currentAlias, onClickListener);
        fragment.show(activity.getSupportFragmentManager(), ADD_OR_EDIT_ROUTER_ALIAS_FRAGMENT);

    }

    @Override
    public void onClick(View view) {
        if (view == null) {
            return;
        }
        if (view.getId() == R.id.router_alias_add) {
            displayRouterAliasDialog(this,
                    null, null,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            doRefreshRoutersListWithSpinner(RecyclerViewRefreshCause.DATA_SET_CHANGED,
                                    null);
                        }
                    });
        }
    }

    @Override
    public void onRefresh() {
        doRefreshRoutersListWithSpinner(RecyclerViewRefreshCause.DATA_SET_CHANGED, null);
    }

    private void doRefreshRoutersListWithSpinner(@NonNull final RecyclerViewRefreshCause cause, final Integer position) {
        mSwipeRefreshLayout.setEnabled(false);
        setRefreshActionButtonState(true);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    final ImmutableList<Pair<String, String>> allAliases = FluentIterable
                        .from(Router.getAliases(ManageRouterAliasesActivity.this, mRouter))
                            .toList();
                    ((RouterAliasesListRecyclerViewAdapter) ManageRouterAliasesActivity.this.mAdapter)
                            .setAliasesColl(allAliases);
                    switch (cause) {
                        case DATA_SET_CHANGED:
                            ManageRouterAliasesActivity.this.mAdapter.notifyDataSetChanged();
                            break;
                        case INSERTED:
                            ManageRouterAliasesActivity.this.mAdapter.notifyItemInserted(position);
                            break;
                        case REMOVED:
                            ManageRouterAliasesActivity.this.mAdapter.notifyItemRemoved(position);
                        case UPDATED:
                            ManageRouterAliasesActivity.this.mAdapter.notifyItemChanged(position);
                    }
                } finally {
                    setRefreshActionButtonState(false);
                    mSwipeRefreshLayout.setEnabled(true);
                }
            }
        }, 1000);
    }

    public void setRefreshActionButtonState(final boolean refreshing) {
        mSwipeRefreshLayout.setRefreshing(refreshing);
        if (optionsMenu != null) {
            final MenuItem refreshItem = optionsMenu.findItem(R.id.router_aliases_list_refresh);
            if (refreshItem != null) {
                if (refreshing) {
                    refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
                } else {
                    refreshItem.setActionView(null);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_manage_router_aliases, menu);

        this.optionsMenu = menu;

        //Permission requests
        final int rwExternalStoragePermissionCheck = ContextCompat
                .checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (rwExternalStoragePermissionCheck != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat
                    .shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                SnackbarUtils.buildSnackbar(this,
                        "Storage access is required to import / export local aliases.",
                        "OK",
                        Snackbar.LENGTH_INDEFINITE,
                        new SnackbarCallback() {
                            @Override
                            public void onShowEvent(@Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventSwipe(int event, @Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventActionClick(int event, @Nullable Bundle bundle) throws Exception {
                                //Request permission
                                ActivityCompat.requestPermissions(ManageRouterAliasesActivity.this,
                                        new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        DDWRTCompanionConstants.Permissions.STORAGE);
                            }

                            @Override
                            public void onDismissEventTimeout(int event, @Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventManual(int event, @Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventConsecutive(int event, @Nullable Bundle bundle) throws Exception {

                            }
                        },
                        null,
                        true);
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        DDWRTCompanionConstants.Permissions.STORAGE);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        //Search
        final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        final SearchView searchView = (SearchView) menu
                .findItem(R.id.router_aliases_list_refresh_search).getActionView();

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnQueryTextListener(this);

        // Get the search close button image view
        final ImageView closeButton = (ImageView) searchView.findViewById(R.id.search_close_btn);
        if (closeButton != null) {
            // Set on click listener
            closeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Reset views
                    final RouterAliasesListRecyclerViewAdapter adapter =
                            (RouterAliasesListRecyclerViewAdapter) mAdapter;
                    adapter.setAliasesColl(FluentIterable
                        .from(Router.getAliases(ManageRouterAliasesActivity.this, mRouter))
                        .toList());
                    adapter.notifyDataSetChanged();
                    //Hide it now
                    searchView.setIconified(true);
                }
            });
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        switch (requestCode) {
            case DDWRTCompanionConstants.Permissions.STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Yay! Permission granted for #" + requestCode);
                    if (optionsMenu != null) {
                        final MenuItem importMenuItem = optionsMenu
                                .findItem(R.id.router_aliases_import);
                        final MenuItem exportMenuItem = optionsMenu
                                .findItem(R.id.router_aliases_export_all);
                        importMenuItem.setEnabled(true);
                        exportMenuItem.setEnabled(true);
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Crashlytics.log(Log.WARN, LOG_TAG, "Boo! Permission denied for #" + requestCode);
                    Utils.displayMessage(this,
                            "Export/Import of local aliases will be unavailable",
                            Style.INFO);
                    if (optionsMenu != null) {
                        final MenuItem importMenuItem = optionsMenu
                                .findItem(R.id.router_aliases_import);
                        final MenuItem exportMenuItem = optionsMenu
                                .findItem(R.id.router_aliases_export_all);
                        importMenuItem.setEnabled(false);
                        exportMenuItem.setEnabled(false);
                    }
                }
                return;
            }
            default:
                break;
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.router_aliases_add:
                displayRouterAliasDialog(this,
                        null, null,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                doRefreshRoutersListWithSpinner(RecyclerViewRefreshCause.DATA_SET_CHANGED,
                                        null);
                            }
                        });
                return true;

            case R.id.router_aliases_import:
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                        PackageManager.PERMISSION_GRANTED) {
                    //Permission denied
                    Utils.displayMessage(this, "Storage access required!", Style.ALERT);
                    return true;

                }
                final Fragment importAliasesFragment = getSupportFragmentManager()
                        .findFragmentByTag(IMPORT_ALIASES_FRAGMENT_TAG);
                if (importAliasesFragment instanceof DialogFragment) {
                    ((DialogFragment) importAliasesFragment).dismiss();
                }
                final DialogFragment importAliases = ImportAliasesDialogFragment
                        .newInstance(mRouter.getUuid());
                importAliases.show(getSupportFragmentManager(), IMPORT_ALIASES_FRAGMENT_TAG);
                return true;

            case R.id.router_aliases_export_all:
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                        PackageManager.PERMISSION_GRANTED) {
                    //Permission denied
                    Utils.displayMessage(this, "Storage access required!", Style.ALERT);
                    return true;

                }
                final Bundle token = new Bundle();
                token.putInt(MAIN_ACTIVITY_ACTION, RouterActions.EXPORT_ALIASES);

                SnackbarUtils.buildSnackbar(this,
                        String.format("Going to start exporting aliases for '%s' (%s)...",
                                mRouter.getDisplayName(), mRouter.getRemoteIpAddress()),
                        "Undo",
                        Snackbar.LENGTH_SHORT,
                        ManageRouterAliasesActivity.this,
                        token,
                        true);
                return true;

            case R.id.router_aliases_clear_all:
                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.ic_action_alert_warning)
                        .setTitle("Drop all aliases?")
                        .setMessage("You'll lose all your local aliases for this router!")
                        .setCancelable(true)
                        .setPositiveButton("Proceed!", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final SharedPreferences.Editor editor = mRouterPreferences.edit();
                                //Iterate over all possible aliases
                                final Set<Pair<String, String>> aliases =
                                        Router.getAliases(ManageRouterAliasesActivity.this, mRouter);
                                for (final Pair<String, String> alias : aliases) {
                                    if (alias == null) {
                                        continue;
                                    }
                                    editor.remove(alias.first);
                                }
                                editor.apply();
                                doRefreshRoutersListWithSpinner(RecyclerViewRefreshCause.DATA_SET_CHANGED,
                                        null);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Cancelled - nothing more to do!
                            }
                        }).create().show();
                return true;

            case R.id.router_aliases_list_refresh:
                doRefreshRoutersListWithSpinner(RecyclerViewRefreshCause.DATA_SET_CHANGED,
                        null);
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        final RouterAliasesListRecyclerViewAdapter adapter =
                (RouterAliasesListRecyclerViewAdapter) mAdapter;
        if (TextUtils.isEmpty(s)) {
            adapter.setAliasesColl(
                    FluentIterable.from(
                            Router.getAliases(this, mRouter)
                    )
                    .toList()
            );
            adapter.notifyDataSetChanged();
        } else {
            adapter.getFilter().filter(s);
        }
        return true;
    }

    @Override
    public void onShowEvent(@Nullable Bundle bundle) throws Exception {

    }

    @Override
    public void onDismissEventSwipe(int event, @Nullable Bundle bundle) throws Exception {

    }

    @Override
    public void onDismissEventActionClick(int event, @Nullable Bundle bundle) throws Exception {

    }

    @Override
    public void onDismissEventTimeout(int event, @Nullable Bundle bundle) throws Exception {
        final Integer action = bundle != null ?
                bundle.getInt(MAIN_ACTIVITY_ACTION) : null;

        if (action == null) {
            return;
        }

        switch (action) {
            case RouterActions.EXPORT_ALIASES:
                //Load all aliases from preferences
                if (mRouterPreferences == null) {
                    break;
                }
                final Map<String, ?> allRouterPrefs = mRouterPreferences.getAll();
                if (allRouterPrefs == null || allRouterPrefs.isEmpty()) {
                    return;
                }

                final Map<String, String> aliases = new HashMap<>();
                for (final Map.Entry<String, ?> entry : allRouterPrefs.entrySet()) {
                    final String key = entry.getKey();
                    final Object value = entry.getValue();
                    if (isNullOrEmpty(key) || value == null) {
                        continue;
                    }
                    //Check whether key is a MAC-Address
                    if (!Utils.MAC_ADDRESS.matcher(key).matches()) {
                        continue;
                    }
                    //This is a MAC Address - collect it right away!
                    aliases.put(key, nullToEmpty(value.toString()));
                }

                final File exportDirectory = StorageUtils.getExportDirectory(this);
                if (exportDirectory == null) {
                    throw new StorageException("Could not retrieve or create export directory!");
                }

                final File aliasesDir = new File(exportDirectory, "alias");

                StorageUtils.createDirectoryOrRaiseException(aliasesDir);

                final File outputFile = new File(aliasesDir,
                        Utils.getEscapedFileName(
                                String.format("Aliases_for_%s_%s_%s",
                                        mRouter.getDisplayName(),
                                        mRouter.getRemoteIpAddress(),
                                        mRouter.getUuid())) + ".json");

                final Date backupDate = new Date();
                final String aliasesStr = new JSONObject(aliases)
                        .toString(2);

                FileOutputStream fileOutputStream = null;
                try {
                    fileOutputStream = new FileOutputStream(outputFile);
                    fileOutputStream.write(aliasesStr.getBytes());
                } finally {
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                }

                SnackbarUtils.buildSnackbar(this,
                        findViewById(android.R.id.content),
                        Color.DKGRAY,
                        String.format("File '%s' created!",
                                outputFile.getAbsolutePath()),
                        Color.GREEN,
                        "Share", Color.WHITE,
                        Snackbar.LENGTH_LONG,
                        new SnackbarCallback() {
                            @Override
                            public void onShowEvent(@Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventSwipe(int event, @Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventActionClick(int event, @Nullable Bundle bundle) throws Exception {
                                //Share button clicked - share file
                                try {
                                    if (!outputFile.exists()) {
                                        Utils.displayMessage(ManageRouterAliasesActivity.this,
                                                String.format("File '%s' no longer exists",
                                                        outputFile.getAbsolutePath()),
                                                Style.ALERT);
                                        return;
                                    }
                                    //Now allow user to share file if needed
                                    final Uri uriForFile = FileProvider.getUriForFile(
                                            ManageRouterAliasesActivity.this,
                                            DDWRTCompanionConstants.FILEPROVIDER_AUTHORITY,
                                            outputFile);
                                    ManageRouterAliasesActivity.this.grantUriPermission(
                                            ManageRouterAliasesActivity.this.getPackageName(),
                                            uriForFile, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                                    final Intent shareIntent = new Intent();
                                    shareIntent.setAction(Intent.ACTION_SEND);
                                    shareIntent.putExtra(Intent.EXTRA_SUBJECT,
                                            String.format("Aliases Backup for Router '%s' (%s)",
                                                    mRouter.getDisplayName(), mRouter.getRemoteIpAddress()));
                                    shareIntent.setType("text/html");
                                    shareIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(
                                            ("Backup Date: " + backupDate + "\n\n" +
                                                    aliasesStr + "\n\n\n").replaceAll("\n", "<br/>") +
                                                    Utils.getShareIntentFooter()));
                                    shareIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);
//                                            shareIntent.setType("*/*");
                                    ManageRouterAliasesActivity.this.startActivity(Intent.createChooser(shareIntent,
                                            ManageRouterAliasesActivity.this.getResources().getText(R.string.share_backup)));
                                } catch (final Exception e) {
                                    e.printStackTrace();
                                    Utils.reportException(ManageRouterAliasesActivity.this, e);
                                    //No worries, but notify user
                                    Utils.displayMessage(ManageRouterAliasesActivity.this,
                                            "Internal Error - please try again later or share file manually!",
                                            Style.ALERT);
                                }
                            }

                            @Override
                            public void onDismissEventTimeout(int event, @Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventManual(int event, @Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventConsecutive(int event, @Nullable Bundle bundle) throws Exception {

                            }
                        },
                        null,
                        true);

                break;
            default:
                //Ignored
                break;
        }
    }

    @Override
    public void onDismissEventManual(int event, @Nullable Bundle bundle) throws Exception {

    }

    @Override
    public void onDismissEventConsecutive(int event, @Nullable Bundle bundle) throws Exception {

    }

    static class RouterAliasesListRecyclerViewAdapter
            extends RecyclerView.Adapter<RouterAliasesListRecyclerViewAdapter.ViewHolder>
            implements Filterable {

        private final ManageRouterAliasesActivity context;
        private final Filter mFilter;
        private List<Pair<String, String>> aliasesColl;
        private final Router mRouter;
        private final SharedPreferences mPreferences;

        public RouterAliasesListRecyclerViewAdapter(final ManageRouterAliasesActivity activity,
                                                    final Router mRouter) {
            this.context = activity;
            this.mRouter = mRouter;
            this.mPreferences = this.context
                    .getSharedPreferences(this.mRouter.getUuid(), Context.MODE_PRIVATE);
            this.aliasesColl = FluentIterable
                    .from(Router.getAliases(this.context, this.mRouter))
                    .toList();

            this.mFilter = new Filter() {
                @Override
                protected FilterResults performFiltering(final CharSequence constraint) {
                    final FilterResults oReturn = new FilterResults();
                    final List<Pair<String, String>> aliases =
                            FluentIterable
                                    .from(RouterAliasesListRecyclerViewAdapter.this.mRouter
                                        .getAliases(RouterAliasesListRecyclerViewAdapter.this.context))
                                    .toList();

                    if (aliases.isEmpty()) {
                        return oReturn;
                    }

                    if (TextUtils.isEmpty(constraint)) {
                        oReturn.values = aliases;
                    } else {
                        //Filter aliases list
                        oReturn.values = FluentIterable
                                .from(aliases)
                                .filter(new Predicate<Pair<String, String>>() {
                                    @Override
                                    public boolean apply(Pair<String, String> input) {
                                        if (input == null) {
                                            return false;
                                        }
                                        final String macAddr = input.first;
                                        final String alias = input.second;

                                        final boolean containsIgnoreCase = containsIgnoreCase(macAddr, constraint)
                                                || containsIgnoreCase(alias, constraint);

                                        if (containsIgnoreCase) {
                                            return true;
                                        }

                                        //Otherwise check OUI
                                        MACOUIVendor macouiVendor = null;
                                        try {
                                            macouiVendor =
                                                    WirelessClientsTile.mMacOuiVendorLookupCache
                                                            .getIfPresent(macAddr);
                                        } catch (final Exception e) {
                                            //No worries
                                        }

                                        return (macouiVendor != null
                                                && containsIgnoreCase(macouiVendor.getCompany(),
                                                        constraint));
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
                        setAliasesColl((List<Pair<String, String>>) values);
                        notifyDataSetChanged();
                    }
                }
            };

        }

        public void setAliasesColl(final List<Pair<String, String>> aliasesColl) {
            this.aliasesColl = aliasesColl;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.router_aliases_list_layout, parent, false);
            // set the view's size, margins, paddings and layout parameters
            // ...
            final long currentTheme = context
                    .getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
                    .getLong(THEMING_PREF, DDWRTCompanionConstants.DEFAULT_THEME);
            final CardView cardView = (CardView) v.findViewById(R.id.router_alias_item_cardview);
            if (currentTheme == ColorUtils.LIGHT_THEME) {
                //Light
                cardView.setCardBackgroundColor(ContextCompat
                        .getColor(context, R.color.cardview_light_background));
            } else {
                //Default is Dark
                cardView.setCardBackgroundColor(ContextCompat
                        .getColor(context, R.color.cardview_dark_background));
            }

//        return new ViewHolder(this.context,
//                RippleViewCreator.addRippleToView(v));
            return new ViewHolder(this.context, v);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            if (position < 0 || position >= aliasesColl.size()) {
                Utils.reportException(null, new IllegalStateException());
                Toast.makeText(context,
                        "Internal Error. Please try again later",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            final Pair<String, String> aliasPairAt =
                    aliasesColl.get(position);
            final String mac = aliasPairAt.first;
            final String aliasStr = aliasPairAt.second;
            holder.macAddress.setText(mac);
            holder.alias.setText(aliasStr);

            //Update OUI in a background thread - as this is likely to infer network call
            new AsyncTask<String, Void, Void>() {
                @Override
                protected Void doInBackground(String... params) {
                    try {
                        final MACOUIVendor macouiVendor =
                                WirelessClientsTile.mMacOuiVendorLookupCache
                                    .get(mac);
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (macouiVendor != null) {
                                    holder.oui.setText(macouiVendor.getCompany());
                                    holder.oui.setVisibility(View.VISIBLE);
                                } else {
                                    holder.oui.setVisibility(View.INVISIBLE);
                                }
                            }
                        });

                    } catch (final Exception e) {
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                holder.oui.setVisibility(View.INVISIBLE);
                            }
                        });
                    } finally {
                        //Hide loading wheel
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                holder.ouiLoadingSpinner.setVisibility(View.GONE);
                            }
                        });
                    }
                    return null;
                }
            }.execute(mac);

            final AlertDialog removeAliasEntryDialog = new AlertDialog.Builder(context)
                    .setIcon(R.drawable.ic_action_alert_warning)
                    .setTitle(String.format("Drop Alias for '%s'?",
                            mac))
                    .setMessage("Are you sure you wish to continue? ")
                    .setCancelable(true)
                    .setPositiveButton("Proceed!", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mPreferences.edit()
                                    .remove(mac)
                                    .apply();
                            setAliasesColl(FluentIterable.from(mRouter.getAliases(context))
                                    .toList());
                            notifyItemRemoved(position);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //Cancelled - nothing more to do!
                        }
                    }).create();

            holder.removeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeAliasEntryDialog.show();
                }
            });

            holder.containerView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    displayRouterAliasDialog(
                            context,
                            mac,
                            aliasStr,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (context != null) {
                                        context.doRefreshRoutersListWithSpinner(
                                                RecyclerViewRefreshCause.DATA_SET_CHANGED,
                                                null);
                                    }
                                }
                            });
                }
            });

            holder.containerView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showRouterAliasPopupMenu(v, mac, aliasStr, removeAliasEntryDialog);
                    return true;
                }
            });

            final boolean isThemeLight = ColorUtils.isThemeLight(this.context);

            if (!isThemeLight) {
                //Set menu background to white
                holder.aliasMenu
                        .setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
            }

            holder.aliasMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showRouterAliasPopupMenu(v,
                            mac,
                            aliasStr,
                            removeAliasEntryDialog);
                }
            });

        }

        private void showRouterAliasPopupMenu(final View v,
                                              final String mac,
                                              final String aliasStr,
                                              final AlertDialog removeAliasEntryDialog) {
            final PopupMenu popup = new PopupMenu(context, v);
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.menu_router_alias_edit:
                            displayRouterAliasDialog(
                                    context,
                                    mac,
                                    aliasStr,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (context != null) {
                                                context.doRefreshRoutersListWithSpinner(
                                                        RecyclerViewRefreshCause.DATA_SET_CHANGED,
                                                        null);
                                            }
                                        }
                                    });
                            return true;
                        case R.id.menu_router_alias_remove:
                            removeAliasEntryDialog.show();
                            return true;
                        default:
                            break;
                    }
                    return false;
                }
            });
            final MenuInflater inflater = popup.getMenuInflater();
            final Menu menu = popup.getMenu();
            inflater.inflate(R.menu.menu_manage_router_alias, menu);
            popup.show();
        }

        @Override
        public int getItemCount() {
            return aliasesColl.size();
        }

        @Override
        public Filter getFilter() {
            return mFilter;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            private final View itemView;

            @NonNull
            final TextView macAddress;
            @NonNull
            final TextView alias;
            final TextView oui;
            final ProgressBar ouiLoadingSpinner;

            @NonNull
            final ImageButton removeButton;

            @NonNull
            final ImageButton aliasMenu;

            final View containerView;

            private final Context mContext;

            public ViewHolder(Context context, View itemView) {
                super(itemView);
                this.mContext = context;
                this.itemView = itemView;

                this.containerView = this.itemView
                        .findViewById(R.id.router_alias_detail_container);

                this.macAddress =
                        (TextView) this.itemView.findViewById(
                                R.id.router_alias_mac_addr);
                this.alias =
                        (TextView) this.itemView.findViewById(
                                R.id.router_alias_alias);
                this.removeButton =
                        (ImageButton) this.itemView.findViewById(
                                R.id.router_alias_remove_btn);
                this.oui = (TextView)
                        this.itemView.findViewById(R.id.router_alias_oui);
                this.ouiLoadingSpinner = (ProgressBar)
                        this.itemView.findViewById(R.id.router_alias_oui_loading);

                this.aliasMenu =
                        (ImageButton) this.itemView.findViewById(
                                R.id.router_alias_menu);
            }
        }
    }

    public static class AddOrUpdateRouterAliasDialogFragment
            extends DialogFragment {

        public static final String MAC_ADDRESS = "macAddress";
        public static final String ALIAS = "alias";

        private CharSequence mMacAddr;
        private CharSequence mAlias;
        private SharedPreferences routerPreferences;
        private DialogInterface.OnClickListener onClickListener;

        @NonNull
        public static AddOrUpdateRouterAliasDialogFragment
        newInstance(@NonNull final Router router,
                    CharSequence mMacAddr,
                    CharSequence mAlias,
                    final DialogInterface.OnClickListener onClickListener){
            final AddOrUpdateRouterAliasDialogFragment addOrUpdateRouterAliasDialogFragment = 
                new AddOrUpdateRouterAliasDialogFragment();
            final Bundle args = new Bundle();
            args.putCharSequence(MAC_ADDRESS, mMacAddr);
            args.putCharSequence(ALIAS, mAlias);
            args.putString(RouterManagementActivity.ROUTER_SELECTED, router.getUuid());

            addOrUpdateRouterAliasDialogFragment.setArguments(args);
            addOrUpdateRouterAliasDialogFragment.onClickListener = onClickListener;

            return addOrUpdateRouterAliasDialogFragment;

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            final FragmentActivity fragmentActivity = getActivity();

            final long currentTheme = fragmentActivity
                .getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
                    .getLong(THEMING_PREF, DDWRTCompanionConstants.DEFAULT_THEME);
            if (currentTheme == ColorUtils.LIGHT_THEME) {
                //Light
                fragmentActivity.setTheme(R.style.AppThemeLight);
            } else {
                //Default is Dark
                fragmentActivity.setTheme(R.style.AppThemeDark);
            }

            final Bundle arguments = getArguments();
            this.mMacAddr = arguments.getCharSequence(MAC_ADDRESS);
            this.mAlias = arguments.getCharSequence(ALIAS);

            final String routerUuid = arguments.getString(RouterManagementActivity.ROUTER_SELECTED);
            if (isNullOrEmpty(routerUuid)) {
                Toast.makeText(fragmentActivity, "Invalid Router", Toast.LENGTH_SHORT).show();
                dismiss();
                return;
            }
            routerPreferences = fragmentActivity
                    .getSharedPreferences(routerUuid, Context.MODE_PRIVATE);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final FragmentActivity activity = getActivity();

            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

            // Get the layout inflater
            final LayoutInflater inflater = activity.getLayoutInflater();

            final View view = inflater.inflate(R.layout.add_or_edit_router_alias, null);
            final boolean isNewAlias = isEmpty(this.mMacAddr);
            builder
                    .setTitle((isNewAlias ? "Set" : "Update") +
                            " Device Alias")
                    .setMessage("Note that the Alias you define here is stored locally only, not on the router.")
                    .setIcon(android.R.drawable.stat_sys_warning)
                    .setView(view)
                    // Add action buttons
                    .setPositiveButton(isNewAlias ? "Set Alias" : "Update Alias", 
                     new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            //Do nothing here because we override this button later to change the close behaviour.
                            //However, we still need this because on older versions of Android unless we
                            //pass a handler the button doesn't get instantiated
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            getDialog().cancel();
                        }
                    });

            return builder.create();
        }

        @Override
        public void onStart() {
            super.onStart();    //super.onStart() is where dialog.show() is actually called on the underlying dialog, so we have to do it after this point

            final AlertDialog d = (AlertDialog) getDialog();
            if (d != null) {
                final EditText mMacAddrView = (EditText) d.findViewById(R.id.add_or_edit_router_alias_mac);
                mMacAddrView.setText(this.mMacAddr, TextView.BufferType.EDITABLE);

                final EditText mAliasView = (EditText) d.findViewById(R.id.add_or_edit_router_alias_value);
                mAliasView.setText(this.mAlias, TextView.BufferType.EDITABLE);

                d.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Validate data
                        final EditText macEditText = (EditText) d.findViewById(R.id.add_or_edit_router_alias_mac);
                        final Editable macValue = macEditText.getText();

                        final String macValueToPersist = nullToEmpty(macValue.toString()).toLowerCase();

                        if (isEmpty(macValueToPersist)) {
                            //Crouton
                            Crouton.makeText(getActivity(), "MAC Address is required", ALERT,
                                    (ViewGroup) (d.findViewById(R.id.add_or_edit_router_alias_notification_viewgroup))).show();
                            macEditText.requestFocus();
                            //Open Keyboard
                            final InputMethodManager imm = (InputMethodManager) getActivity()
                                    .getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null) {
                                // only will trigger it if no physical keyboard is open
                                imm.showSoftInput(macEditText, 0);
                            }
                            return;
                        }
                        if (!Utils.MAC_ADDRESS.matcher(macValueToPersist).matches()) {
                            Crouton.makeText(getActivity(), "MAC Address format required", ALERT,
                                    (ViewGroup) (d.findViewById(R.id.add_or_edit_router_alias_notification_viewgroup))).show();
                            macEditText.requestFocus();
                            //Open Keyboard
                            final InputMethodManager imm = (InputMethodManager) getActivity()
                                    .getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null) {
                                // only will trigger it if no physical keyboard is open
                                imm.showSoftInput(macEditText, 0);
                            }
                            return;
                        }
                        final EditText aliasEditText = (EditText)
                                d.findViewById(R.id.add_or_edit_router_alias_value);

                        routerPreferences.edit()
                                .putString(macValueToPersist,
                                        nullToEmpty(aliasEditText.getText().toString()))
                                .apply();

                        if (onClickListener != null) {
                            onClickListener.onClick(d, view.getId());
                        }

                        d.dismiss();

                    }
                });


            }
        }


    }

}