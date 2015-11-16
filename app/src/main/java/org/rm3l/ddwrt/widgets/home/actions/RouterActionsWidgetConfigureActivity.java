package org.rm3l.ddwrt.widgets.home.actions;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdView;
import com.google.common.collect.Lists;

import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.mgmt.RouterAddDialogFragment;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.mgmt.RouterMgmtDialogListener;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.AdUtils;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.ReportingUtils;
import org.rm3l.ddwrt.utils.Utils;
import org.rm3l.ddwrt.widgets.home.wol.WOLWidgetProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.MAX_ROUTERS_FREE_VERSION;


/**
 * The configuration screen for the {@link WOLWidgetProvider WOLWidgetProvider} AppWidget.
 */
public class RouterActionsWidgetConfigureActivity extends AppCompatActivity implements RouterMgmtDialogListener {

    private static final String PREFS_NAME = DDWRTCompanionConstants.WIDGETS_PREFERENCES_KEY;
    private static final String PREF_PREFIX_KEY = \"fake-key\";

    public static final String ADD_ROUTER_FRAGMENT_TAG = "add_router_from_actions_widget";
    public static final String ADD_NEW = "--- ADD NEW ---";

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private ArrayAdapter<String> mRoutersListAdapter;

    private int mCurrentItemPos;

    private List<Router> mRoutersListForPicker;

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {

            final Context context = RouterActionsWidgetConfigureActivity.this;

            final String text = mSelectedRouterUuid.getText().toString();
            if (text.isEmpty()) {
                Toast.makeText(context, "No router selected!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
                return;
//                finish();
            }

            // When the button is clicked, store the string locally
            saveRouterUuidPref(context, mAppWidgetId, text);

            // It is the responsibility of the configuration activity to update the app widget
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            RouterActionsWidgetProvider.updateAppWidget(context, appWidgetManager, mAppWidgetId);

            // Make sure we pass back the original appWidgetId
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);

            final Map<String, Object> eventMap = new HashMap<>();
            eventMap.put("Widget", "Actions");
            ReportingUtils.reportEvent(ReportingUtils.EVENT_WIDGET_INSTALLED, eventMap);

            finish();
        }
    };
    Spinner mRoutersDropdown;
    TextView mSelectedRouterUuid;

    DDWRTCompanionDAO mDao;

    // Write the prefix to the SharedPreferences object for this widget
    static void saveRouterUuidPref(Context context, int appWidgetId, String text) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(PREF_PREFIX_KEY + appWidgetId, text)
                .apply();
        Utils.requestBackup(context);
    }

    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    @Nullable
    static String loadRouterUuidPref(Context context, int appWidgetId) {
        final SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_PREFIX_KEY + appWidgetId, null);
    }

    static void deleteRouterUuidPref(Context context, int appWidgetId) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(PREF_PREFIX_KEY + appWidgetId)
                .apply();
        Utils.requestBackup(context);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        final boolean themeLight = ColorUtils.isThemeLight(this);
        if (themeLight) {
            //Light
            setTheme(R.style.AppThemeLight);
        } else {
            //Default is Dark
            setTheme(R.style.AppThemeDark);
        }

        setContentView(R.layout.actionswidget_configure);

        AdUtils.buildAndDisplayAdViewIfNeeded(this,
                (AdView) findViewById(R.id.widget_configure_adView));

        if (themeLight) {
            final Resources resources = getResources();
            getWindow().getDecorView()
                    .setBackgroundColor(resources.getColor(android.R.color.white));
        }

        final Toolbar mToolbar = (Toolbar) findViewById(R.id.actions_widget_configure_toolbar);
        if (mToolbar != null) {
            mToolbar.setTitle("Add Actions Widget");
            setSupportActionBar(mToolbar);
        }
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
        }

        findViewById(R.id.add_button).setOnClickListener(mOnClickListener);

        findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        mSelectedRouterUuid = (TextView) findViewById(R.id.selected_router_uuid);

        mDao = RouterManagementActivity.getDao(this);

        mRoutersDropdown = (Spinner) findViewById(R.id.actions_widget_routers_dropdown);

        final List<Router> allRouters = mDao.getAllRouters();
        if (allRouters == null || allRouters.isEmpty()) {
            openAddRouterForm();
            mRoutersListForPicker = new ArrayList<>();
        } else {

            final int allRoutersSize = allRouters.size();
            mRoutersListForPicker = Lists.newArrayListWithCapacity(allRoutersSize);
            for (final Router router : allRouters) {
                //FIXME Uncomment once full support of other firmwares is implemented
//                final RouterFirmware routerFirmware;
//                if (router == null ||
//                        (routerFirmware = router.getRouterFirmware()) == null ||
//                        RouterFirmware.UNKNOWN.equals(routerFirmware)) {
//                    continue;
//                }
                //FIXME End
                mRoutersListForPicker.add(router);
            }
        }

        final String[] routersNamesArray = new String[mRoutersListForPicker.size() + 1];
        routersNamesArray[0] = ADD_NEW;

        int i = 1;
        mCurrentItemPos = -1;
        final String selectedRouterUuid = loadRouterUuidPref(RouterActionsWidgetConfigureActivity.this, mAppWidgetId);
        for (final Router router : mRoutersListForPicker) {
            if (nullToEmpty(selectedRouterUuid).equals(router.getUuid())) {
                mCurrentItemPos = i;
            }
            final String routerName = router.getName();
            routersNamesArray[i++] = ((isNullOrEmpty(routerName) ? "-" : routerName) + "\n(" +
                    router.getRemoteIpAddress() + ")");
        }

        mRoutersListAdapter = new ArrayAdapter<>(this,
                R.layout.routers_picker_spinner_item, new ArrayList<>(Arrays.asList(routersNamesArray)));
        mRoutersListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mRoutersDropdown.setAdapter(mRoutersListAdapter);

        if (mCurrentItemPos >= 0) {
            mRoutersDropdown.setSelection(mCurrentItemPos);
        } else {
            if (routersNamesArray.length > 1) {
                mRoutersDropdown.setSelection(1);
            } else {
                mRoutersDropdown.setSelection(0);
            }
        }

        final View altAddrContainer = findViewById(R.id.selected_router_alternate_addresses_container);
        final LinearLayout altAddrLayoutContainer = (LinearLayout) findViewById(R.id.selected_router_use_local_ssid_container);

        mRoutersDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ((TextView) parent.getChildAt(0)).setTextColor(
                        getResources().getColor(themeLight ? R.color.black : R.color.white));

                final int size = mRoutersListForPicker.size();
                if (position < 0 || position > size) {
                    return;
                }

                if (position == 0) {
                    //Add New Button
                    openAddRouterForm();
                    if (mCurrentItemPos >= 0) {
                        mRoutersDropdown.setSelection(mCurrentItemPos);
                    }
                    return;
                }

                mCurrentItemPos = position;
                final Router selectedRouter = mRoutersListForPicker.get(position - 1);
                if (selectedRouter == null) {
                    return;
                }
                mSelectedRouterUuid.setText(selectedRouter.getUuid());
                final Collection<Router.LocalSSIDLookup> localSSIDLookupData =
                        selectedRouter.getLocalSSIDLookupData(RouterActionsWidgetConfigureActivity.this);
                if (selectedRouter.isUseLocalSSIDLookup(RouterActionsWidgetConfigureActivity.this) &&
                        !localSSIDLookupData.isEmpty()) {
                    altAddrContainer.setVisibility(View.VISIBLE);
                    altAddrLayoutContainer.removeAllViews();
                    for (final Router.LocalSSIDLookup localSSIDLookup : localSSIDLookupData) {
                        if (localSSIDLookup == null) {
                            continue;
                        }
                        final TextView localSsidView = new TextView(RouterActionsWidgetConfigureActivity.this);
                        localSsidView.setText(localSSIDLookup.getNetworkSsid() + "\n" +
                                localSSIDLookup.getReachableAddr() + "\n" +
                                localSSIDLookup.getPort());
                        localSsidView.setLayoutParams(new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT));
                        altAddrLayoutContainer.addView(localSsidView);
                        final View lineView = Utils.getLineView(RouterActionsWidgetConfigureActivity.this);
                        if (lineView != null) {
                            altAddrLayoutContainer.addView(lineView);
                        }
                    }
                } else {
                    altAddrContainer.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void openAddRouterForm() {
        final Fragment addRouter = getSupportFragmentManager().findFragmentByTag(ADD_ROUTER_FRAGMENT_TAG);
        if (addRouter instanceof DialogFragment) {
            ((DialogFragment) addRouter).dismiss();
        }

        //Display Donate Message if trying to add more than the max routers for Free version
        final List<Router> allRouters = mDao.getAllRouters();
        //noinspection PointlessBooleanExpression,ConstantConditions
        if ((BuildConfig.DONATIONS || BuildConfig.WITH_ADS) &&
                allRouters != null && allRouters.size() >= MAX_ROUTERS_FREE_VERSION) {
            //Download the full version to unlock this version
            Utils.displayUpgradeMessage(this, "Manage a new Router");
            return;
        }

        final DialogFragment addFragment = new RouterAddDialogFragment();
        addFragment.show(getSupportFragmentManager(), ADD_ROUTER_FRAGMENT_TAG);
    }

    @Override
    public void onRouterAdd(DialogFragment dialog, Router newRouter, boolean error) {
        if (newRouter != null && !error) {
            final String selectedRouterUuid = newRouter.getUuid();
            mSelectedRouterUuid.setText(selectedRouterUuid);

            final List<Router> allRouters = mDao.getAllRouters();
            final int allRoutersSize = allRouters.size();
            mRoutersListForPicker = Lists.newArrayListWithCapacity(allRoutersSize);
            for (final Router wrt : allRouters) {
                //FIXME Uncomment once other firmwares are fully supported
//                final RouterFirmware routerFirmware;
//                if (wrt == null ||
//                        (routerFirmware = wrt.getRouterFirmware()) == null ||
//                        RouterFirmware.UNKNOWN.equals(routerFirmware)) {
//                    continue;
//                }
                //FIXME End
                mRoutersListForPicker.add(wrt);
            }

            final String[] routersNamesArray = new String[mRoutersListForPicker.size() + 1];
            routersNamesArray[0] = ADD_NEW;

            int i = 1;
            int currentItem = -1;
            for (final Router router : mRoutersListForPicker) {
                if (nullToEmpty(selectedRouterUuid).equals(router.getUuid())) {
                    currentItem = i;
                }
                final String routerName = router.getName();
                routersNamesArray[i++] = ((isNullOrEmpty(routerName) ? "-" : routerName) + "\n(" +
                        router.getRemoteIpAddress() + ")");
            }

            mRoutersListAdapter = new ArrayAdapter<>(this,
                    R.layout.routers_picker_spinner_item, routersNamesArray);
            mRoutersListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            mRoutersDropdown.setAdapter(mRoutersListAdapter);

            if (currentItem >= 0) {
                mRoutersDropdown.setSelection(currentItem);
            } else {
                if (routersNamesArray.length > 1) {
                    mRoutersDropdown.setSelection(1);
                } else {
                    mRoutersDropdown.setSelection(0);
                }
            }
        }
    }

    @Override
    public void onRouterUpdated(DialogFragment dialog, int position, Router router, boolean error) {
        //Nothing to do here, as we are not updating routers from here!
    }
}



