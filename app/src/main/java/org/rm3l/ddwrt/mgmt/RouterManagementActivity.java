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

package org.rm3l.ddwrt.mgmt;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Outline;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.common.base.Joiner;
import com.suredigit.inappfeedback.FeedbackDialog;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.about.AboutDialog;
import org.rm3l.ddwrt.actions.RebootRouterAction;
import org.rm3l.ddwrt.actions.RouterAction;
import org.rm3l.ddwrt.actions.RouterActionListener;
import org.rm3l.ddwrt.exceptions.UserGeneratedReportException;
import org.rm3l.ddwrt.feedback.SendFeedbackDialog;
import org.rm3l.ddwrt.main.DDWRTMainActivity;
import org.rm3l.ddwrt.mgmt.adapters.RouterListRecycleViewAdapter;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteDAOImpl;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.settings.RouterManagementSettingsActivity;
import org.rm3l.ddwrt.utils.AdUtils;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.MAX_ROUTERS_FREE_VERSION;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.OPENED_AT_LEAST_ONCE_PREF_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.THEMING_PREF;


public class RouterManagementActivity
        extends ActionBarActivity
        implements View.OnClickListener,
        RouterMgmtDialogListener,
        ActionMode.Callback, RecyclerView.OnItemTouchListener,
        SearchView.OnQueryTextListener {

    public static final int ROUTER_MANAGEMENT_SETTINGS_ACTIVITY_CODE = 111;
    public static final String ROUTER_SELECTED = "ROUTER_SELECTED";
    public static final String ADD_ROUTER_FRAGMENT_TAG = "add_router";
    public static final String UPDATE_ROUTER_FRAGMENT_TAG = "update_router";
    private static final String LOG_TAG = RouterManagementActivity.class.getSimpleName();
    @Nullable
    ActionMode actionMode;
    GestureDetectorCompat gestureDetector;
    int itemCount;
    ImageButton addNewButton;
    private long mCurrentTheme;
    private DDWRTCompanionDAO dao;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private Menu optionsMenu;
    private FeedbackDialog mFeedbackDialog;

    private Toolbar mToolbar;
    private SharedPreferences mPreferences;

    @Nullable
    private InterstitialAd mInterstitialAd;

    @NonNull
    public static DDWRTCompanionDAO getDao(Context context) {
        return new DDWRTCompanionSqliteDAOImpl(context);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            final RouterListRecycleViewAdapter adapter = (RouterListRecycleViewAdapter) mAdapter;
            final String query = intent.getStringExtra(SearchManager.QUERY);
            if (query == null) {
                adapter.setRoutersList(dao.getAllRouters());
                adapter.notifyDataSetChanged();
                return;
            }
            adapter.getFilter().filter(query);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handleIntent(getIntent());

        //Default values are not set by default
        //Android bug workaround: http://code.google.com/p/android/issues/detail?id=6641
        PreferenceManager.setDefaultValues(this, DEFAULT_SHARED_PREFERENCES_KEY,
                Context.MODE_PRIVATE, R.xml.router_management_settings, false);

        mPreferences = getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        mCurrentTheme = mPreferences.getLong(THEMING_PREF, DDWRTCompanionConstants.DEFAULT_THEME);
        if (mCurrentTheme == ColorUtils.LIGHT_THEME) {
            //Light
            setTheme(R.style.AppThemeLight);
        } else {
            //Default is Dark
            setTheme(R.style.AppThemeDark);
        }

        setContentView(R.layout.activity_router_management);

        final Button removeAdsButton = (Button) findViewById(R.id.router_list_remove_ads);
        if (BuildConfig.WITH_ADS) {
            removeAdsButton.setVisibility(View.VISIBLE);
            removeAdsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Utils.displayUpgradeMessageForAdsRemoval(RouterManagementActivity.this);
                }
            });
        } else {
            removeAdsButton.setVisibility(View.GONE);
        }

        AdUtils.buildAndDisplayAdViewIfNeeded(this, (AdView) findViewById(R.id.router_list_adView));

        mInterstitialAd = AdUtils.requestNewInterstitial(this, R.string.interstitial_ad_unit_id_router_list_to_router_main);

        mToolbar = (Toolbar) findViewById(R.id.routerManagementActivityToolbar);
        if (mToolbar != null) {
            mToolbar.setTitle("Routers");
            setSupportActionBar(mToolbar);
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
        }

        this.dao = getDao(this);

        mRecyclerView = (RecyclerView) findViewById(R.id.routersListView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        // allows for optimizations if all items are of the same size:
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.scrollToPosition(0);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new RouterListRecycleViewAdapter(this, this.dao.getAllRouters());
        mRecyclerView.setAdapter(mAdapter);

//        mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
//            @Override
//            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
//                final ActionBar ab = getSupportActionBar();
//                switch (newState) {
//                    case RecyclerView.SCROLL_STATE_DRAGGING:
//                        //The RecyclerView is currently being dragged by outside input such as user touch input.
//                        if (ab.isShowing()) {
//                            ab.hide();
//                        }
//                        break;
//                    case RecyclerView.SCROLL_STATE_SETTLING:
//                        //The RecyclerView is currently animating to a final position while not under outside control.
//                        if (!ab.isShowing()) {
//                            ab.show();
//                        }
//                        break;
//                    default:
//                        break;
//                }
//                super.onScrollStateChanged(recyclerView, newState);
//            }
//
//            @Override
//            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
//                super.onScrolled(recyclerView, dx, dy);
//            }
//        });

        /*
         * onClickDetection is done in this Activity's onItemTouchListener
         * with the help of a GestureDetector;
         * Tip by Ian Lake on G+ in a comment to this post:
         * https://plus.google.com/+LucasRocha/posts/37U8GWtYxDE
         */
        mRecyclerView.addOnItemTouchListener(this);
        gestureDetector = new GestureDetectorCompat(this, new RouterManagementViewOnGestureListener());

        final View addNewButtonView = findViewById(R.id.router_list_add);
        addNewButton = (ImageButton) addNewButtonView;
        addNewButton.setOnClickListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            addNewButtonView.setOutlineProvider(new ViewOutlineProvider() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void getOutline(View view, Outline outline) {
                    final int diameter = getResources().getDimensionPixelSize(R.dimen.diameter);
                    outline.setOval(0, 0, diameter, diameter);
                }
            });
            addNewButtonView.setClipToOutline(true);
        }

        mFeedbackDialog = new SendFeedbackDialog(this).getFeedbackDialog();
        mFeedbackDialog.setDebug(BuildConfig.DEBUG);

        initOpenAddRouterFormIfNecessary();
    }

    private void initOpenAddRouterFormIfNecessary() {
        if (mAdapter.getItemCount() == 0) {
            this.openAddRouterForm();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mFeedbackDialog != null) {
            mFeedbackDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mFeedbackDialog != null) {
            mFeedbackDialog.dismiss();
        }

        //Dismiss existing dialog fragments, if any
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(ADD_ROUTER_FRAGMENT_TAG);
        if (fragment instanceof DialogFragment) {
            ((DialogFragment) fragment).dismiss();
        }

        fragment = getSupportFragmentManager().findFragmentByTag(UPDATE_ROUTER_FRAGMENT_TAG);
        if (fragment instanceof DialogFragment) {
            ((DialogFragment) fragment).dismiss();
        }
    }

    private void openAddRouterForm() {
        final Fragment addRouter = getSupportFragmentManager().findFragmentByTag(ADD_ROUTER_FRAGMENT_TAG);
        if (addRouter instanceof DialogFragment) {
            ((DialogFragment) addRouter).dismiss();
        }

        //Display Donate Message if trying to add more than the max routers for Free version
        final List<Router> allRouters = dao.getAllRouters();
        //noinspection PointlessBooleanExpression,ConstantConditions
        if ((BuildConfig.DONATIONS || BuildConfig.WITH_ADS) &&
                allRouters != null && allRouters.size() >= MAX_ROUTERS_FREE_VERSION) {
            //Download the full version to unlock this version
            Utils.displayUpgradeMessage(this);
            return;
        }

        final DialogFragment addFragment = new RouterAddDialogFragment();
        addFragment.show(getSupportFragmentManager(), ADD_ROUTER_FRAGMENT_TAG);
    }

    private void openUpdateRouterForm(@Nullable Router router) {
        if (router != null) {
            final DialogFragment updateFragment = new RouterUpdateDialogFragment();
            final Bundle args = new Bundle();
            args.putString(ROUTER_SELECTED, router.getUuid());
            updateFragment.setArguments(args);
            updateFragment.show(getSupportFragmentManager(), UPDATE_ROUTER_FRAGMENT_TAG);
        } else {
            Crouton.makeText(this, "Entry no longer exists!", Style.ALERT).show();
        }
    }

    private void openDuplicateRouterForm(@Nullable Router router) {
        //Display Donate Message if trying to add more than the max routers for Free version
        final List<Router> allRouters = dao.getAllRouters();
        //noinspection PointlessBooleanExpression,ConstantConditions
        if ((BuildConfig.DONATIONS || BuildConfig.WITH_ADS) &&
                allRouters != null && allRouters.size() >= MAX_ROUTERS_FREE_VERSION) {
            //Download the full version to unlock this version
            Utils.displayUpgradeMessage(this);
            return;
        }

        if (router != null) {
            final DialogFragment copyFragment = new RouterDuplicateDialogFragment();
            final Bundle args = new Bundle();
            args.putString(ROUTER_SELECTED, router.getUuid());
            copyFragment.setArguments(args);
            copyFragment.show(getSupportFragmentManager(), "copy_router");
        } else {
            Crouton.makeText(this, "Entry no longer exists!", Style.ALERT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.optionsMenu = menu;
        getMenuInflater().inflate(R.menu.menu_router_management, menu);
        //noinspection PointlessBooleanExpression
        if (!BuildConfig.DONATIONS) {
            final MenuItem item = menu.findItem(R.id.router_list_donate);
            if (item != null) {
                item.setVisible(false);
            }
        }

        //Search
        final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        final SearchView searchView = (SearchView) menu
                .findItem(R.id.router_list_refresh_search).getActionView();

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
                    final RouterListRecycleViewAdapter adapter = (RouterListRecycleViewAdapter) mAdapter;
                    adapter.setRoutersList(dao.getAllRouters());
                    adapter.notifyDataSetChanged();
                    //Hide it now
                    searchView.setIconified(true);
                }
            });
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        final RouterListRecycleViewAdapter adapter = (RouterListRecycleViewAdapter) mAdapter;
        final List<Router> routersList = adapter.getRoutersList();

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.router_list_refresh:
                doRefreshRoutersListWithSpinner(RoutersListRefreshCause.DATA_SET_CHANGED, null);
                return true;
            case R.id.router_list_donate:
                Utils.openDonateActivity(this);
                return true;
            case R.id.router_list_about:
                new AboutDialog(this).show();
                return true;
            case R.id.router_list_settings:
                this.startActivityForResult(
                        new Intent(this, RouterManagementSettingsActivity.class), ROUTER_MANAGEMENT_SETTINGS_ACTIVITY_CODE);
                return true;
            case R.id.router_list_feedback:
                if (mFeedbackDialog == null) {
                    mFeedbackDialog = new SendFeedbackDialog(this).getFeedbackDialog();
                    mFeedbackDialog.setDebug(BuildConfig.DEBUG);
                }
                mFeedbackDialog.show();
                //Generate a custom error-report (for ACRA)
                Utils.reportException(new UserGeneratedReportException("Feedback displayed"));
                return true;
            case R.id.router_list_actions_restore_factory_defaults:
                //TODO Hidden for now
                return true;
            case R.id.router_list_actions_firmwares_upgrade:
                //TODO Hidden for now
                return true;
            case R.id.router_list_actions_reboot_routers:
            {
                final List<String> allRoutersStr = new ArrayList<>();
                for (Router router : routersList) {
                    if (router == null) {
                        continue;
                    }
                    allRoutersStr.add(String.format("'%s' (%s)",
                            router.getDisplayName(), router.getRemoteIpAddress()));
                }
                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.ic_action_alert_warning)
                        .setTitle("Reboot All Router(s)?")
                        .setMessage(String.format("Are you sure you wish to continue? " +
                                        "The following Routers will be rebooted: \n\n%s",
                                Joiner.on("\n").skipNulls().join(allRoutersStr)))
                        .setCancelable(true)
                        .setPositiveButton("Proceed!", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialogInterface, final int i) {

                                Utils.displayMessage(RouterManagementActivity.this,
                                        String.format("Rebooting %d Router(s)....", routersList.size()),
                                        Style.INFO);

                                final AtomicInteger currentNum = new AtomicInteger(0);
                                final AtomicInteger numActionsWithNoSuccess = new AtomicInteger(0);
                                final int totalNumOfDevices = routersList.size();

                                for (final Router selectedRouter : routersList) {
                                    new RebootRouterAction(RouterManagementActivity.this,
                                            new RouterActionListener() {
                                                @Override
                                                public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router, Object returnData) {
                                                    final int incrementAndGet = currentNum.incrementAndGet();
                                                    if (incrementAndGet >= totalNumOfDevices) {
                                                        final int numActionsThatDidNotSucceed = numActionsWithNoSuccess.get();
                                                        if (numActionsThatDidNotSucceed > 0) {
                                                            //An error occurred
                                                            if (numActionsThatDidNotSucceed < totalNumOfDevices) {
                                                                Utils.displayMessage(RouterManagementActivity.this,
                                                                        String.format("Action '%s' executed but %d error(s) occurred",
                                                                                routerAction.toString(), numActionsThatDidNotSucceed),
                                                                        Style.INFO);
                                                            } else {
                                                                //No action succeeded
                                                                Utils.displayMessage(RouterManagementActivity.this,
                                                                        String.format("None of the '%s' actions submitted succeeded - please try again later.",
                                                                                routerAction.toString()),
                                                                        Style.INFO);
                                                            }

                                                        } else {
                                                            //No error
                                                            Utils.displayMessage(RouterManagementActivity.this,
                                                                    String.format("Action '%s' executed successfully on %d Routers",
                                                                            routerAction.toString(), routersList.size()),
                                                                    Style.CONFIRM);
                                                        }
                                                    }
                                                }

                                                @Override
                                                public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull Router router, @Nullable Exception exception) {
                                                    final int incrementAndGet = currentNum.incrementAndGet();
                                                    numActionsWithNoSuccess.incrementAndGet();
                                                    if (incrementAndGet >= totalNumOfDevices) {
                                                        //An error occurred
                                                        Utils.displayMessage(RouterManagementActivity.this,
                                                                String.format("Action '%s' executed but %d error(s) occurred: %s",
                                                                        routerAction.toString(), numActionsWithNoSuccess.get(),
                                                                        ExceptionUtils.getRootCauseMessage(exception)),
                                                                Style.INFO);
                                                    }
                                                }
                                            },
                                            mPreferences).execute(selectedRouter);
                                }
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Cancelled - nothing more to do!
                            }
                        }).create().show();
            }
            return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(LOG_TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        // Check which request we're responding to
        switch (requestCode) {
            case ROUTER_MANAGEMENT_SETTINGS_ACTIVITY_CODE:
                // Make sure the request was successful and reload U if necessary
                if (resultCode == RESULT_OK) {
                    if (this.mCurrentTheme != this.mPreferences.getLong(THEMING_PREF, -1l)) {
                        //Reload UI
                        final AlertDialog alertDialog = Utils.buildAlertDialog(this, null, "Reloading UI...", false, false);
                        alertDialog.show();
                        ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                finish();
                                startActivity(getIntent());
                                alertDialog.cancel();
                            }
                        }, 2000);
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void doRefreshRoutersListWithSpinner(@NonNull final RoutersListRefreshCause cause, final Integer position) {
        setRefreshActionButtonState(true);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                final List<Router> allRouters = RouterManagementActivity.this.dao.getAllRouters();
                ((RouterListRecycleViewAdapter) RouterManagementActivity.this.mAdapter).setRoutersList(allRouters);
                switch (cause) {
                    case DATA_SET_CHANGED:
                        RouterManagementActivity.this.mAdapter.notifyDataSetChanged();
                        break;
                    case INSERTED:
                        RouterManagementActivity.this.mAdapter.notifyItemInserted(position);
                        break;
                    case REMOVED:
                        RouterManagementActivity.this.mAdapter.notifyItemRemoved(position);
                    case UPDATED:
                        RouterManagementActivity.this.mAdapter.notifyItemChanged(position);
                }
                setRefreshActionButtonState(false);
            }
        }, 1000);
    }

    public void setRefreshActionButtonState(final boolean refreshing) {
        if (optionsMenu != null) {
            final MenuItem refreshItem = optionsMenu.findItem(R.id.router_list_refresh);
            if (refreshItem != null) {
                if (refreshing) {
                    refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
//                    addNewButton.setVisibility(View.GONE);
                } else {
                    refreshItem.setActionView(null);
//                    addNewButton.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11.
    }

    @Override
    public void onBackPressed() {
        if (BuildConfig.WITH_ADS && mInterstitialAd != null) {
            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
                mInterstitialAd.setAdListener(new AdListener() {
                    @Override
                    public void onAdClosed() {
                        RouterManagementActivity.super.onBackPressed();
                    }
                });
            } else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }

    }

    @Override
    public void onClick(@Nullable final View view) {
        if (view == null) {
            return;
        }

        if (view.getId() == R.id.router_list_add) {
            this.openAddRouterForm();
        } else if (view.getId() == R.id.container_list_item) {
            // item click
            final int idx = mRecyclerView.getChildPosition(view);
            final RouterListRecycleViewAdapter adapter = (RouterListRecycleViewAdapter) mAdapter;
            if (actionMode != null) {
                final int previousSelectedItemCount = adapter.getSelectedItemCount();
                myToggleSelection(idx);
                //Set background color, depending on whether this is a selection or a de-selection
                final int currentSelectedItemCount = adapter.getSelectedItemCount();
                if (currentSelectedItemCount == previousSelectedItemCount - 1) {
                    //De-selection: remove background
                    view.setBackgroundResource(android.R.color.transparent);
                } else if (currentSelectedItemCount == previousSelectedItemCount + 1) {
                    //Selection: apply background
                    if (ColorUtils.isThemeLight(this)) {
                        view.setBackgroundResource(R.color.DarkOrange);
                    } else {
                        view.setBackgroundResource(R.color.yellow);
                    }
                } //other cases should not occur (as this is a single selection)

                //Now hide ActionMode if selected items count falls to 0
                if (currentSelectedItemCount == 0) {
                    actionMode.finish();
                }
                return;
            }

            //No action mode - normal mode => open up main activity for this router
            final List<Router> routersList = adapter.getRoutersList();
            final Router router;
            if (idx < 0 || idx >= routersList.size() || (router = routersList.get(idx)) == null) {
                Crouton.makeText(RouterManagementActivity.this,
                        "Unknown router - please refresh list or add a new one.", Style.ALERT).show();
                return;
            }

            //FIXME Uncomment once other firmwares are fully supported
//            final Router.RouterFirmware routerFirmware = router.getRouterFirmware();
//            if (routerFirmware == null || Router.RouterFirmware.UNKNOWN.equals(routerFirmware)) {
//                Utils.displayMessage(this, "Router Firmware unknown or not supported (yet!). " +
//                        "You may manually force the router firmware to use by editing this entry.", Style.ALERT);
//                return;
//            }
            //FIXME End

            final String routerUuid = router.getUuid();

            final Intent ddWrtMainIntent = new Intent(RouterManagementActivity.this, DDWRTMainActivity.class);
            ddWrtMainIntent.putExtra(ROUTER_SELECTED, routerUuid);

            final SharedPreferences routerSharedPreferences = getSharedPreferences(routerUuid, Context.MODE_PRIVATE);
            if (!routerSharedPreferences.getBoolean(OPENED_AT_LEAST_ONCE_PREF_KEY, false)) {
                routerSharedPreferences.edit()
                        .putBoolean(OPENED_AT_LEAST_ONCE_PREF_KEY, true)
                        .apply();
            }

            //Animate
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (BuildConfig.WITH_ADS && mInterstitialAd != null) {
                    if (mInterstitialAd.isLoaded()) {
                        mInterstitialAd.show();
                        mInterstitialAd.setAdListener(new AdListener() {
                            @Override
                            public void onAdClosed() {
                                final AdRequest adRequest = AdUtils.buildAdRequest(RouterManagementActivity.this);
                                if (adRequest != null) {
                                    mInterstitialAd.loadAd(adRequest);
                                }
                                startActivity(view, ddWrtMainIntent);
                            }
                        });
                    } else {
                        startActivity(view, ddWrtMainIntent);
                    }
                } else {
                    startActivity(view, ddWrtMainIntent);
                }
            } else {
                if (BuildConfig.WITH_ADS && mInterstitialAd != null) {
                    if (mInterstitialAd.isLoaded()) {
                        mInterstitialAd.show();
                        mInterstitialAd.setAdListener(new AdListener() {
                            @Override
                            public void onAdClosed() {
                                final AdRequest adRequest = AdUtils.buildAdRequest(RouterManagementActivity.this);
                                if (adRequest != null) {
                                    mInterstitialAd.loadAd(adRequest);
                                }
                                startActivity(view, ddWrtMainIntent);
                            }
                        });
                    } else {
                        final AlertDialog alertDialog = Utils.buildAlertDialog(this, null, "Loading...", false, false);
                        alertDialog.show();
                        ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                startActivity(view, ddWrtMainIntent);
                                alertDialog.cancel();
                            }
                        }, 1000);
                    }
                } else {
                    final AlertDialog alertDialog = Utils.buildAlertDialog(this, null, "Loading...", false, false);
                    alertDialog.show();
                    ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startActivity(view, ddWrtMainIntent);
                            alertDialog.cancel();
                        }
                    }, 1000);
                }
            }

        }
    }

    private void startActivity(View view, Intent ddWrtMainIntent) {
        final String transitionName = getString(R.string.transition_router);
        final ActivityOptionsCompat options =
                ActivityOptionsCompat.makeSceneTransitionAnimation(
                        RouterManagementActivity.this, view, transitionName);

        ActivityCompat.startActivity(RouterManagementActivity.this, ddWrtMainIntent, options.toBundle());
    }

    @Override
    public void onRouterAdd(DialogFragment dialog, Router router, boolean error) {
        if (!error) {
            //Always added to the top
            doRefreshRoutersListWithSpinner(RoutersListRefreshCause.INSERTED, 0);
            mLayoutManager.scrollToPosition(0);
            //Request Backup
            Utils.requestBackup(this);
        }
    }

    @Override
    public void onRouterUpdated(DialogFragment dialog, int position, Router router, boolean error) {
        if (!error) {
            //Always added to the top
            doRefreshRoutersListWithSpinner(RoutersListRefreshCause.UPDATED, position);
            mLayoutManager.scrollToPosition(position);
            //Request Backup
            Utils.requestBackup(this);
        }
    }

    /**
     * Called when action mode is first created. The menu supplied will be used to
     * generate action buttons for the action mode.
     *
     * @param actionMode ActionMode being created
     * @param menu       Menu used to populate action buttons
     * @return true if the action mode should be created, false if entering this
     * mode should be aborted.
     */
    @Override
    public boolean onCreateActionMode(@NonNull ActionMode actionMode, Menu menu) {
        // Inflate a menu resource providing context menu items
        if (mToolbar != null) {
            mToolbar.setVisibility(View.GONE);
        }
        final MenuInflater inflater = actionMode.getMenuInflater();
        inflater.inflate(R.menu.menu_router_list_selection_menu, menu);
        addNewButton.setVisibility(View.GONE);
        return true;
    }

    /**
     * Called to refresh an action mode's action menu whenever it is invalidated.
     *
     * @param actionMode ActionMode being prepared
     * @param menu       Menu used to populate action buttons
     * @return true if the menu or action mode was updated, false otherwise.
     */
    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return false;
    }

    /**
     * Called to report a user click on an action button.
     *
     * @param actionMode The current ActionMode
     * @param menuItem   The item that was clicked
     * @return true if this callback handled the event, false if the standard MenuItem
     * invocation should continue.
     */
    @Override
    public boolean onActionItemClicked(@NonNull final ActionMode actionMode, @NonNull final MenuItem menuItem) {
        final RouterListRecycleViewAdapter adapter = (RouterListRecycleViewAdapter) mAdapter;
        final List<Router> routersList = adapter.getRoutersList();

        switch (menuItem.getItemId()) {
            case R.id.action_actions_reboot_routers: {
                final List<Integer> selectedItems = adapter.getSelectedItems();

                if (selectedItems.isEmpty()) {
                    Toast.makeText(this, "No Router selected", Toast.LENGTH_SHORT).show();
                    return true;
                }

                final List<Router> selectedRouters = new ArrayList<>();
                final List<String> selectedRoutersStr = new ArrayList<>();
                for (Integer selectedItem : selectedItems) {
                    if (selectedItem == null || selectedItem < 0 || selectedItem >= routersList.size()) {
                        continue;
                    }
                    final Router selectedRouter;
                    if ((selectedRouter = routersList.get(selectedItem)) == null) {
                        continue;
                    }
                    selectedRouters.add(selectedRouter);
                    selectedRoutersStr.add(String.format("'%s' (%s)",
                            selectedRouter.getDisplayName(), selectedRouter.getRemoteIpAddress()));
                }

                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.ic_action_alert_warning)
                        .setTitle(String.format("Reboot %d Router(s)?", selectedItems.size()))
                        .setMessage(String.format("Are you sure you wish to continue? " +
                                        "The following Routers will be rebooted: \n\n%s",
                                Joiner.on("\n").skipNulls().join(selectedRoutersStr)))
                        .setCancelable(true)
                        .setPositiveButton("Proceed!", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialogInterface, final int i) {

                                Utils.displayMessage(RouterManagementActivity.this,
                                        String.format("Rebooting %d Router(s)....", selectedItems.size()),
                                        Style.INFO);

                                final AtomicInteger currentNum = new AtomicInteger(0);
                                final AtomicInteger numActionsWithNoSuccess = new AtomicInteger(0);
                                final int totalNumOfDevices = selectedRouters.size();

                                for (final Router selectedRouter : selectedRouters) {
                                    new RebootRouterAction(RouterManagementActivity.this,
                                            new RouterActionListener() {
                                                @Override
                                                public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router, Object returnData) {
                                                    final int incrementAndGet = currentNum.incrementAndGet();
                                                    if (incrementAndGet >= totalNumOfDevices) {
                                                        final int numActionsThatDidNotSucceed = numActionsWithNoSuccess.get();
                                                        if (numActionsThatDidNotSucceed > 0) {
                                                            //An error occurred
                                                            if (numActionsThatDidNotSucceed < totalNumOfDevices) {
                                                                Utils.displayMessage(RouterManagementActivity.this,
                                                                        String.format("Action '%s' executed but %d error(s) occurred",
                                                                                routerAction.toString(), numActionsThatDidNotSucceed),
                                                                        Style.INFO);
                                                            } else {
                                                                //No action succeeded
                                                                Utils.displayMessage(RouterManagementActivity.this,
                                                                        String.format("None of the '%s' actions submitted succeeded - please try again later.",
                                                                                routerAction.toString()),
                                                                        Style.INFO);
                                                            }

                                                        } else {
                                                            //No error
                                                            Utils.displayMessage(RouterManagementActivity.this,
                                                                    String.format("Action '%s' executed successfully on %d Routers",
                                                                            routerAction.toString(), selectedItems.size()),
                                                                    Style.CONFIRM);
                                                        }
                                                    }
                                                }

                                                @Override
                                                public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull Router router, @Nullable Exception exception) {
                                                    final int incrementAndGet = currentNum.incrementAndGet();
                                                    numActionsWithNoSuccess.incrementAndGet();
                                                    if (incrementAndGet >= totalNumOfDevices) {
                                                        //An error occurred
                                                        Utils.displayMessage(RouterManagementActivity.this,
                                                                String.format("Action '%s' executed but %d error(s) occurred: %s",
                                                                        routerAction.toString(), numActionsWithNoSuccess.get(),
                                                                        ExceptionUtils.getRootCauseMessage(exception)),
                                                                Style.INFO);
                                                    }
                                                }
                                            },
                                            mPreferences).execute(selectedRouter);
                                }
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Cancelled - nothing more to do!
                            }
                        }).create().show();
            }
                return true;
            case R.id.action_actions_restore_factory_defaults: {
                //TODO Hidden for now

            }
                return true;
            case R.id.action_actions_firmwares_upgrade:
                //TODO Hidden for now
                return true;
            case R.id.menu_router_list_delete:
                final int selectedItemCount = adapter.getSelectedItemCount();
                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.ic_action_alert_warning)
                        .setTitle(String.format("Delete %d Router(s)?", selectedItemCount))
                        .setMessage("You'll lose those records!")
                        .setCancelable(true)
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialogInterface, final int i) {
                                final List<Integer> selectedItemPositions = adapter.getSelectedItems();
                                int numberOfItems = -1;
                                for (int itemPosition = selectedItemPositions.size() - 1; itemPosition >= 0; itemPosition--) {
                                    numberOfItems = adapter.removeData(selectedItemPositions.get(itemPosition));
                                }
                                actionMode.finish();
                                if (numberOfItems == 0) {
                                    //All items dropped = open up 'Add Router' diaalog
                                    RouterManagementActivity.this.openAddRouterForm();
                                }

                                Crouton.makeText(RouterManagementActivity.this, selectedItemCount + " item(s) deleted", Style.CONFIRM).show();
                                //Request Backup
                                Utils.requestBackup(RouterManagementActivity.this);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Cancelled - nothing more to do!
                            }
                        }).create().show();

                return true;
            case R.id.menu_router_item_edit: {
                final Integer itemPos = adapter.getSelectedItems().get(0);
                this.openUpdateRouterForm(
                        (itemPos == null || itemPos < 0 || itemPos >= routersList.size()) ? null : routersList.get(itemPos)
                );
            }
            return true;
            case R.id.menu_router_item_copy: {
                final Integer itemPos = adapter.getSelectedItems().get(0);
                this.openDuplicateRouterForm(
                        (itemPos == null || itemPos < 0 || itemPos >= routersList.size()) ? null : routersList.get(itemPos)
                );
            }
            return true;
            default:
                return false;
        }
    }

    /**
     * Called when an action mode is about to be exited and destroyed.
     *
     * @param actionMode The current ActionMode being destroyed
     */
    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        this.actionMode = null;
        //Reset background for selected items
        for (int i = 0; i < this.dao.getAllRouters().size(); i++) {
            final View childAt;
            if ((childAt = mRecyclerView.getLayoutManager().getChildAt(i)) == null) {
                continue;
            }
            childAt.setBackgroundResource(android.R.color.transparent);
        }
        ((RouterListRecycleViewAdapter) mAdapter).clearSelections();
        addNewButton.setVisibility(View.VISIBLE);
        if (mToolbar != null) {
            mToolbar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
        gestureDetector.onTouchEvent(motionEvent);
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {

    }

    private void myToggleSelection(int idx) {
        if (actionMode == null) {
            return;
        }
        final RouterListRecycleViewAdapter adapter = (RouterListRecycleViewAdapter) mAdapter;
        adapter.toggleSelection(idx);
        final int selectedItemCount = adapter.getSelectedItemCount();

        String title = getString(R.string.selected_count, selectedItemCount);
        actionMode.setTitle(title);

        //Show 'Edit' button only if one item is selected
        final MenuItem editButton = actionMode.getMenu().getItem(0);
        final boolean menuItemRelevantForOneItemSelectedOnly = (selectedItemCount == 1);
        if (editButton != null) {
            editButton.setVisible(menuItemRelevantForOneItemSelectedOnly);
        }
        final MenuItem copyButton = actionMode.getMenu().getItem(1);
        if (copyButton != null) {
            copyButton.setVisible(menuItemRelevantForOneItemSelectedOnly);
        }
        final MenuItem deleteButton = actionMode.getMenu().getItem(2);
        if (deleteButton != null) {
            deleteButton.setVisible(selectedItemCount > 0);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        final RouterListRecycleViewAdapter adapter = (RouterListRecycleViewAdapter) mAdapter;
        if (TextUtils.isEmpty(s)) {
            adapter.setRoutersList(dao.getAllRouters());
            adapter.notifyDataSetChanged();
        } else {
            adapter.getFilter().filter(s);
        }
        return true;
    }

    public enum RoutersListRefreshCause {
        INSERTED, REMOVED, DATA_SET_CHANGED, UPDATED
    }

    private class RouterManagementViewOnGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
            final View view = mRecyclerView.findChildViewUnder(e.getX(), e.getY());
            onClick(view);
            return super.onSingleTapConfirmed(e);
        }

        public void onLongPress(@NonNull MotionEvent e) {
            if (actionMode != null) {
                return;
            }
            final View view = mRecyclerView.findChildViewUnder(e.getX(), e.getY());
            //Item long-pressed: set background
            if (view == null) {
                return;
            }
            //Selection: apply background
            if (ColorUtils.isThemeLight(RouterManagementActivity.this)) {
                view.setBackgroundResource(R.color.DarkOrange);
            } else {
                view.setBackgroundResource(R.color.yellow);
            }
            // Start the CAB using the ActionMode.Callback defined above
            actionMode = startActionMode(RouterManagementActivity.this);
            int idx = mRecyclerView.getChildPosition(view);
            myToggleSelection(idx);
            super.onLongPress(e);
        }
    }
}
