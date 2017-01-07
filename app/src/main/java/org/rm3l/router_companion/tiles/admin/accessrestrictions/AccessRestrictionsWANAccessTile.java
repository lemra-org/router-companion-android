package org.rm3l.router_companion.tiles.admin.accessrestrictions;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.crashlytics.android.Crashlytics;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;

import org.rm3l.router_companion.BuildConfig;
import org.rm3l.router_companion.R;
import org.rm3l.router_companion.actions.ActionManager;
import org.rm3l.router_companion.actions.RouterAction;
import org.rm3l.router_companion.actions.RouterActionListener;
import org.rm3l.router_companion.actions.SetNVRAMVariablesAction;
import org.rm3l.router_companion.actions.ToggleWANAccessPolicyRouterAction;
import org.rm3l.router_companion.exceptions.DDWRTCompanionException;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.resources.RouterData;
import org.rm3l.router_companion.resources.WANAccessPolicy;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.ImageUtils;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.snackbar.SnackbarCallback;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils;
import org.rm3l.router_companion.widgets.RecyclerViewEmptySupport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import de.keyboardsurfer.android.widget.crouton.Style;

import static org.rm3l.router_companion.actions.ToggleWANAccessPolicyRouterAction.DISABLE;
import static org.rm3l.router_companion.actions.ToggleWANAccessPolicyRouterAction.ENABLE_1;
import static org.rm3l.router_companion.actions.ToggleWANAccessPolicyRouterAction.ENABLE_2;
import static org.rm3l.router_companion.RouterCompanionAppConstants.EMPTY_STRING;

/**
 * WAN Access Policies tile
 *
 * https://github.com/mirror/dd-wrt/blob/master/src/router/httpd/visuals/filters.c
 *
 * See http://www.dd-wrt.com/phpBB2/viewtopic.php?p=460996 for instructions on how to manipulate WAN Access Policies:
 *
 * <pre>
 Ok I have it working. Here is what I did in case some else wants to use it.
 Set your access policy with the web interface and save it. In my case I saved it disabled to rule 1.Then to enable or disable it I telnet to the router and use the following commands.
 note: rule1 = rule1, rule2 = 2 etc.
 STAT:1 = enable STAT:2 = disable

 To disable access policy
 root@DD-WRT:~# nvram set filter_rule1=\$STAT:2\$NAME:NoInet\$DENY:1\$$
 nvram commit (if you want the change to be permanent)
 root@DD-WRT:~# stopservice firewall
 root@DD-WRT:~# startservice firewall

 To enable enable access policy
 root@DD-WRT:~# nvram set filter_rule1=\$STAT:1\$NAME:NoInet\$DENY:1\$$
 nvram commit (if you want the change to be permanent)
 root@DD-WRT:~# stopservice firewall
 root@DD-WRT:~# startservice firewall
 * </pre>
 *
 * Created by rm3l on 20/01/16.
 */
public class AccessRestrictionsWANAccessTile extends
        DDWRTTile<AccessRestrictionsWANAccessTile.WANAccessPoliciesRouterData> {

    private static final String LOG_TAG =
            AccessRestrictionsWANAccessTile.class.getSimpleName();

    private static final Splitter todHoursSplitter = Splitter.on(":").omitEmptyStrings().trimResults();

    private RecyclerViewEmptySupport mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private long mLastSync;

    public AccessRestrictionsWANAccessTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_admin_access_restrictions_wan_access, null);

        mRecyclerView = (RecyclerViewEmptySupport) layout.findViewById(R.id.tile_admin_access_restrictions_wan_access_ListView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        // allows for optimizations if all items are of the same size:
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(mParentFragmentActivity,
                LinearLayoutManager.VERTICAL, false);
        mLayoutManager.scrollToPosition(0);
        mRecyclerView.setLayoutManager(mLayoutManager);
//
        final TextView emptyView = (TextView) layout.findViewById(R.id.empty_view);
        if (ColorUtils.isThemeLight(mParentFragmentActivity)) {
            emptyView.setTextColor(ContextCompat.getColor(mParentFragmentActivity, R.color.black));
        } else {
            emptyView.setTextColor(ContextCompat.getColor(mParentFragmentActivity, R.color.white));
        }
        mRecyclerView.setEmptyView(emptyView);

        // specify an adapter (see also next example)
        mAdapter = new WANAccessRulesRecyclerViewAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        final Display display = mParentFragmentActivity
                .getWindowManager()
                .getDefaultDisplay();
        final Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        Log.d(LOG_TAG, "<width,height> = <" + width + "," + height + ">");
        mRecyclerView.setMinimumHeight(size.y);

    }

    public boolean canChildScrollUp() {
        final boolean canScrollVertically = ViewCompat
                .canScrollVertically(mRecyclerView, -1);
        if (!canScrollVertically) {
            return canScrollVertically;
        }

        //TODO ScrollView can scroll vertically,
        // but detect whether the touch was done outside of the scroll view
        // (in which case we should return false)

        return canScrollVertically;
    }

    @Override
    public boolean isEmbeddedWithinScrollView() {
        return false;
    }

    @Override
    public int getTileHeaderViewId() {
        return R.id.tile_admin_access_restrictions_wan_access_hdr;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_admin_access_restrictions_wan_access_title;
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

    @Nullable
    @Override
    protected Loader<AccessRestrictionsWANAccessTile.WANAccessPoliciesRouterData> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<WANAccessPoliciesRouterData>(this.mParentFragmentActivity) {
            @Override
            public WANAccessPoliciesRouterData loadInBackground() {
                try {
                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Init background loader for " +
                            AccessRestrictionsWANAccessTile.class + ": routerInfo=" +
                            mRouter + " / nbRunsLoader=" + nbRunsLoader);

                    if (mRefreshing.getAndSet(true)) {
                        throw new DDWRTTileAutoRefreshNotAllowedException();
                    }
                    nbRunsLoader++;

                    updateProgressBarViewSeparator(0);

                    mLastSync = System.currentTimeMillis();

                    final List<WANAccessPolicy> wanAccessPolicies = new ArrayList<>();

                    if (Utils.isDemoRouter(mRouter)) {
                        for (int i = 1; i <= 10; i++) {
                            final WANAccessPolicy wanAccessPolicy =
                                    new WANAccessPolicy()
                                    .setNumber(i)
                                    .setName("myWanPolicy " + i);
                            //TODO Add other properties here
                            wanAccessPolicies.add(wanAccessPolicy);
                        }
                    } else {
                        updateProgressBarViewSeparator(10);

                        //1- Get all rules first

                        /*
                        filter_rule10=$STAT:1$NAME:myPolicy10$DENY:1$$
                        filter_rule1=$STAT:0$NAME:myPolicy1$DENY:0$$
                        filter_rule2=$STAT:2$NAME:myPolicy2$DENY:0$$
                        filter_rule3=
                        filter_rule4=
                        filter_rule5=
                        filter_rule6=
                        filter_rule7=$STAT:1$NAME:myPolicy7$DENY:1$$

                        filter_rule1=$STAT:1$NAME:Only allow preset IP-addresses$DENY:1$$
                        filter_rule2=$STAT:1$NAME:Inget internet p▒ natten$DENY:1$$
                        filter_rule3=$STAT:1$NAME:Paus mitt p▒ dagen$DENY:1$$
                        filter_rule4=$STAT:1$NAME:Skoldag$DENY:1$$
                        filter_rule5=
                        filter_rule6=
                        filter_rule7=
                        filter_rule8=
                        filter_rule9=
                        filter_rule10=
                         */
                        NVRAMInfo nvramInfo =
                                SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity,
                                        mRouter,
                                        mGlobalPreferences,
                                        "filter_rule.*");
                        Properties properties;
                        if (nvramInfo == null
                                || (properties = nvramInfo.getData()) == null) {
                            return null;
                        }

                        int i = 2;
                        String todPattern;
                        final Set<Map.Entry<Object, Object>> entries = properties.entrySet();
                        for (final Map.Entry<Object, Object> entry : entries) {
                            final Object key = entry.getKey();
                            final Object value = entry.getValue();
                            if (key == null || value == null) {
                                continue;
                            }
                            //Skip empty rules
                            final String valueStr = value.toString();
                            if (Strings.isNullOrEmpty(valueStr)) {
                                continue;
                            }
                            final String keyStr = key.toString();
                            final int keyNb = Integer.parseInt(
                                    keyStr.replace("filter_rule", "").trim());

                            final WANAccessPolicy wanAccessPolicy = new WANAccessPolicy()
                                    .setNumber(keyNb);

                            final List<String> statusSplitter =
                                    Splitter.on("$NAME:").omitEmptyStrings().trimResults()
                                    .splitToList(valueStr);
                            if (!statusSplitter.isEmpty()) {
                                //myPolicy7$DENY:1$$
                                wanAccessPolicy.setStatus(
                                        statusSplitter.get(0).replaceAll("$STAT:", ""));
                                if (statusSplitter.size() >= 2) {
                                    final String nameAndFollowingStr = statusSplitter.get(1);
                                    final List<String> nameAndFollowingSplitter =
                                            Splitter.on("$DENY:").omitEmptyStrings().trimResults()
                                            .splitToList(nameAndFollowingStr);
                                    if (!nameAndFollowingSplitter.isEmpty()) {
                                        wanAccessPolicy.setName(nameAndFollowingSplitter.get(0));
                                        if (nameAndFollowingSplitter.size() >= 2) {
                                            //1$$
                                            final String s =
                                                    nameAndFollowingSplitter.get(1).replaceAll("\\$\\$", "");
                                            if ("0".equals(s)) {
                                                wanAccessPolicy.setDenyOrFilter(WANAccessPolicy.FILTER);
                                            } else {
                                                wanAccessPolicy.setDenyOrFilter(WANAccessPolicy.DENY);
                                            }

                                        }
                                    }
                                }
                            } else {
                                wanAccessPolicy.setStatus(WANAccessPolicy.STATUS_UNKNOWN);
                            }

                            //2- For each, retrieve Time of Day (TOD)
                            nvramInfo =
                                    SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity,
                                            mRouter,
                                            mGlobalPreferences,
                                            "filter_tod_buf" + keyNb);

                            updateProgressBarViewSeparator(10 + (i++));

                            if (nvramInfo != null
                                    && nvramInfo.getProperty("filter_tod_buf" + keyNb) != null) {

                                todPattern = nvramInfo.getProperty("filter_tod_buf" + keyNb);
                                if ("7".equals(todPattern)) {
                                    todPattern = "1 1 1 1 1 1 1";
                                }
                                wanAccessPolicy.setDaysPattern(todPattern);
                            }

                            nvramInfo =
                                    SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity,
                                            mRouter,
                                            mGlobalPreferences,
                                            "filter_tod" + keyNb);

                            updateProgressBarViewSeparator(10 + (i++));

                            if (nvramInfo != null
                                    && nvramInfo.getProperty("filter_tod" + keyNb) != null) {
                                /*
                                filter_tod4=0:0 23:59 0-6
                                filter_tod5=0:0 23:59 0,2,6
                                filter_tod6=0:0 23:59 0-1
                                filter_tod7=6:0 18:0 0-6
                                 */
                                final String filterTod = nvramInfo.getProperty("filter_tod" + keyNb);
                                final List<String> list = Splitter.on(" ").omitEmptyStrings().trimResults()
                                        .splitToList(filterTod);
                                if (list.size() >= 2) {
                                    final String start = list.get(0);
                                    final String end = list.get(1);
                                    if ("0:0".equals(start)
                                            && "23:59".equals(end)) {
                                        wanAccessPolicy.setTimeOfDay("24 Hours");
                                    } else {
                                        wanAccessPolicy.setTimeOfDay(
                                                String.format(Locale.US, "from %s to %s",
                                                        getHourFormatted(start),
                                                        getHourFormatted(end)));
                                    }
                                }
                            }

                            Crashlytics.log(Log.DEBUG, LOG_TAG, "wanAccessPolicy: " + wanAccessPolicy);

                            wanAccessPolicies.add(wanAccessPolicy);

                            updateProgressBarViewSeparator(10 + (i++));
                        }

                        updateProgressBarViewSeparator(80);
                    }

                    final WANAccessPoliciesRouterData routerData =
                            (WANAccessPoliciesRouterData) new WANAccessPoliciesRouterData()
                                .setData(wanAccessPolicies);

                    updateProgressBarViewSeparator(90);

                    return routerData;

                } catch (@NonNull final Exception e) {
                    e.printStackTrace();
                    return (WANAccessPoliciesRouterData)
                            new WANAccessPoliciesRouterData().setException(e);
                }
            }
        };
    }

    @NonNull
    private static String getHourFormatted(@NonNull final String todHour) {
        final List<String> stringList = todHoursSplitter.splitToList(todHour);
        if (stringList.size() < 2) {
            return todHour;
        }
        String hour = stringList.get(0);
        String minutes = stringList.get(1);
        if (hour.length() == 1) {
            hour = ("0" + hour);
        }
        if (minutes.length() == 1) {
            minutes = ("0" + minutes);
        }
        return (hour + ":" + minutes);
    }

    @Override
    public void onLoadFinished(Loader<WANAccessPoliciesRouterData> loader,
                               WANAccessPoliciesRouterData data) {

        try {
            //Set tiles
            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

            if (data == null) {
                data = (WANAccessPoliciesRouterData) new WANAccessPoliciesRouterData()
                        .setException(new DDWRTNoDataException("No Data!"));
            }

            List<WANAccessPolicy> wanAccessPolicies = data.getData();
            Exception exception = data.getException();
            if (exception == null && wanAccessPolicies == null) {
                data = (WANAccessPoliciesRouterData) new WANAccessPoliciesRouterData()
                            .setException(new DDWRTNoDataException("No Data!"));
            }
            exception = data.getException();

            layout.findViewById(R.id.tile_admin_access_restrictions_wan_access_loading_view).setVisibility(View.GONE);

            final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_admin_access_restrictions_wan_access_error);

            if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

                if (exception == null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }

                ((WANAccessRulesRecyclerViewAdapter) mAdapter)
                        .setWanAccessPolicies(wanAccessPolicies);
                mAdapter.notifyDataSetChanged();

                //Update last sync
                final RelativeTimeTextView lastSyncView = (RelativeTimeTextView) layout.findViewById(R.id.tile_last_sync);
                lastSyncView.setReferenceTime(mLastSync);
                lastSyncView.setPrefix("Last sync: ");

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
                updateProgressBarWithError();
            } else if (exception == null){
                updateProgressBarWithSuccess();
            }


            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished(): done loading!");
        } finally {
            mRefreshing.set(false);
            doneWithLoaderInstance(this, loader);
        }

    }

    static class WANAccessRulesRecyclerViewAdapter
            extends RecyclerView.Adapter<WANAccessRulesRecyclerViewAdapter.ViewHolder> {

        @NonNull
        private final AccessRestrictionsWANAccessTile tile;

        @NonNull
        private final List<WANAccessPolicy> wanAccessPolicies = new ArrayList<>();

        public WANAccessRulesRecyclerViewAdapter(@NonNull final AccessRestrictionsWANAccessTile tile) {
            this.tile = tile;
        }

        @NonNull
        public List<WANAccessPolicy> getWanAccessPolicies() {
            return wanAccessPolicies;
        }

        public WANAccessRulesRecyclerViewAdapter setWanAccessPolicies(
                @Nullable final List<WANAccessPolicy> wanAccessPolicies) {
            this.wanAccessPolicies.clear();
            if (wanAccessPolicies != null) {
                this.wanAccessPolicies.addAll(wanAccessPolicies);
                Collections.sort(this.wanAccessPolicies, new Comparator<WANAccessPolicy>() {
                    @Override
                    public int compare(WANAccessPolicy lhs, WANAccessPolicy rhs) {
                        return Integer.valueOf(lhs.getNumber()).compareTo(rhs.getNumber());
                    }
                });
            }
            return this;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.tile_admin_access_restriction, parent, false);
            // set the view's size, margins, paddings and layout parameters
            // ...
            return new ViewHolder(this.tile, v);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            if (position < 0 || position >= wanAccessPolicies.size()) {
                Crashlytics.log(Log.DEBUG, LOG_TAG,
                        "invalid position for WAN Access Policy Adapter");
                return;
            }
            final WANAccessPolicy wanAccessPolicy = wanAccessPolicies.get(position);
            if (wanAccessPolicy == null) {
                //Report error
                Crashlytics.log(Log.DEBUG, LOG_TAG,
                        "wanAccessPolicy @" + position + " is NULL");
                return;
            }

            if (ColorUtils.isThemeLight(this.tile.mParentFragmentActivity)) {
                holder.cardView.setCardBackgroundColor(ContextCompat
                        .getColor(this.tile.mParentFragmentActivity, R.color.cardview_light_background));
            } else {
                holder.cardView.setCardBackgroundColor(ContextCompat
                        .getColor(this.tile.mParentFragmentActivity, R.color.cardview_dark_background));
            }

            final TextDrawable textDrawable = ImageUtils.getTextDrawable(wanAccessPolicy.getName());
            if (textDrawable == null) {
                holder.avatarImageView.setVisibility(View.GONE);
            } else {
                holder.avatarImageView.setVisibility(View.VISIBLE);
                holder.avatarImageView.setImageDrawable(textDrawable);
            }

            holder.policyNb.setText(String.valueOf(wanAccessPolicy.getNumber()));
            holder.policyName.setText(wanAccessPolicy.getName());

            String daysPattern = wanAccessPolicy.getDaysPattern();
            if (daysPattern != null) {
                if ("7".equals(daysPattern)) {
                    daysPattern = "1 1 1 1 1 1 1"; //Everyday
                }
                final List<String> timeOfDayBufferList = Splitter.on(" ").omitEmptyStrings().trimResults()
                        .splitToList(daysPattern);
                for (int i = 0; i < timeOfDayBufferList.size(); i++) {
                    final String todStatus = timeOfDayBufferList.get(i);
                    if (i >= holder.daysTextViews.length) {
                        continue;
                    }
                    final TextView daysTextView = holder.daysTextViews[i];
                    daysTextView.setEnabled(true);
                    if ("1".equals(todStatus)) {
                        daysTextView
                                .setBackgroundResource(R.drawable.table_border_selected);
                    } else {
                        daysTextView
                                .setBackgroundResource(R.drawable.table_border);
                        if (!"0".equals(todStatus)) {
                            daysTextView.setEnabled(false);
                        }
                    }
                }
            }

            holder.internetPolicyDuringSelectedTimeOfDay
                    .setText(wanAccessPolicy.getDenyOrFilter());

            holder.policyHours.setText(wanAccessPolicy.getTimeOfDay());

            //Disable switch button listener
            holder.statusSwitchButton.setEnabled(true);
            final String status = Strings.nullToEmpty(wanAccessPolicy.getStatus()).trim();
            switch (status) {
                case "0":
                case "$STAT:0":
                    //Disabled
                    holder.statusSwitchButton.setChecked(false);
                    break;
                case "1":
                case "$STAT:1":
                case "2":
                case "$STAT:2":
                    holder.statusSwitchButton.setChecked(true);
                    break;
                default:
                    Utils.reportException(tile.mParentFragmentActivity,
                            new WANAccessPolicyException("status=[" + status + "]"));
                    holder.statusSwitchButton.setChecked(false);
                    holder.statusSwitchButton.setEnabled(false);
                    break;
            }


            holder.statusSwitchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
                        Utils.displayUpgradeMessage(tile.mParentFragmentActivity,
                                "Toggle WAN Access Policy Restriction");
                        holder.statusSwitchButton.toggle();
                        return;
                    }

                    holder.statusSwitchButton.setEnabled(false);
                    final boolean isChecked = holder.statusSwitchButton.isChecked();
                    SnackbarUtils.buildSnackbar(tile.mParentFragmentActivity,
                            tile.mParentFragmentActivity.findViewById(android.R.id.content),
                            String.format("Going to %sable WAN Access Policy: '%s'",
                                    isChecked ? "en" : "dis", wanAccessPolicy.getName()),
                            "Undo",
                            Snackbar.LENGTH_LONG,
                            new SnackbarCallback() {
                                @Override
                                public void onShowEvent(@Nullable Bundle bundle) throws Exception {
                                }

                                @Override
                                public void onDismissEventSwipe(int event, @Nullable Bundle bundle) throws Exception {
                                    //revert
                                    tile.mParentFragmentActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            holder.statusSwitchButton.toggle();
                                            holder.statusSwitchButton.setEnabled(true);
                                        }
                                    });
                                }

                                @Override
                                public void onDismissEventActionClick(int event, @Nullable Bundle bundle) throws Exception {
                                    //revert
                                    tile.mParentFragmentActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            holder.statusSwitchButton.toggle();
                                            holder.statusSwitchButton.setEnabled(true);
                                        }
                                    });
                                }

                                @Override
                                public void onDismissEventTimeout(int event, @Nullable Bundle bundle) throws Exception {
                                    Utils.displayMessage(tile.mParentFragmentActivity,
                                            String.format("%sabling WAN Access Policy: '%s'...",
                                                    isChecked ?"En":"Dis", wanAccessPolicy.getName()),
                                            Style.INFO);
                                    final int enableStatus = !isChecked ? DISABLE :
                                            WANAccessPolicy.DENY.equals(wanAccessPolicy.getDenyOrFilter()) ?
                                                    ENABLE_1 :
                                                    ENABLE_2;
                                    ActionManager.runTasks(
                                        new ToggleWANAccessPolicyRouterAction(
                                                tile.mRouter,
                                                tile.mParentFragmentActivity,
                                                new RouterActionListener() {
                                                    @Override
                                                    public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router, Object returnData) {
                                                        tile.mParentFragmentActivity.runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {

                                                                try {
                                                                    Utils.displayMessage(tile.mParentFragmentActivity,
                                                                            String.format("WAN Access Policy '%s' successfully %s on host '%s'. ",
                                                                                    wanAccessPolicy.getName(),
                                                                                    isChecked ? "enabled" : "disabled",
                                                                                    tile.mRouter.getCanonicalHumanReadableName()),
                                                                            Style.CONFIRM);
                                                                } finally {
                                                                    holder.statusSwitchButton.setEnabled(true);
                                                                    wanAccessPolicy.setStatus(Integer.toString(enableStatus));
                                                                    WANAccessRulesRecyclerViewAdapter.this.
                                                                            notifyItemChanged(holder.getAdapterPosition());
                                                                }
                                                            }

                                                        });
                                                    }

                                                    @Override
                                                    public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull Router router, @Nullable final Exception exception) {
                                                        tile.mParentFragmentActivity.runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                try {
                                                                    Utils.displayMessage(tile.mParentFragmentActivity,
                                                                            String.format("Error while trying to %s WAN Access Policy '%s' on '%s': %s",
                                                                                    isChecked ? "enable" : "disable",
                                                                                    wanAccessPolicy.getName(),
                                                                                    tile.mRouter.getCanonicalHumanReadableName(),
                                                                                    Utils.handleException(exception).first),
                                                                            Style.ALERT);
                                                                } finally {
                                                                    //Revert
                                                                    holder.statusSwitchButton.toggle();
                                                                    holder.statusSwitchButton.setEnabled(true);
                                                                }
                                                            }
                                                        });
                                                    }
                                                },
                                                tile.mGlobalPreferences,
                                                wanAccessPolicy,
                                                enableStatus));
                                }

                                @Override
                                public void onDismissEventManual(int event, @Nullable Bundle bundle) throws Exception {
                                    //Revert
                                    tile.mParentFragmentActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            holder.statusSwitchButton.toggle();
                                            holder.statusSwitchButton.setEnabled(true);
                                        }
                                    });
                                }

                                @Override
                                public void onDismissEventConsecutive(int event, @Nullable Bundle bundle) throws Exception {
                                    //revert
                                    tile.mParentFragmentActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            holder.statusSwitchButton.toggle();
                                            holder.statusSwitchButton.setEnabled(true);
                                        }
                                    });
                                }
                            },
                            null,
                            true);
                }
            });

            final AlertDialog removeWanPolicyDialog = new AlertDialog.Builder(tile.mParentFragmentActivity)
                    .setIcon(R.drawable.ic_action_alert_warning)
                    .setTitle(String.format("Remove WAN Access Policy: '%s'?",
                            wanAccessPolicy.getName()))
                    .setMessage("Are you sure you wish to continue? ")
                    .setCancelable(true)
                    .setPositiveButton("Proceed!", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            holder.statusSwitchButton.setEnabled(false);

                            Utils.displayMessage(tile.mParentFragmentActivity,
                                    String.format("Deleting WAN Access Policy: '%s'...",
                                            wanAccessPolicy.getName()),
                                    Style.INFO);

                            final int wanAccessPolicyNumber = wanAccessPolicy.getNumber();

                            final NVRAMInfo nvramVarsToSet = new NVRAMInfo()
                                    .setProperty("filter_dport_grp"+wanAccessPolicyNumber, EMPTY_STRING)
                                    .setProperty("filter_ip_grp"+wanAccessPolicyNumber, EMPTY_STRING)
                                    .setProperty("filter_mac_grp"+wanAccessPolicyNumber, EMPTY_STRING)
                                    .setProperty("filter_p2p_grp"+wanAccessPolicyNumber, EMPTY_STRING)
                                    .setProperty("filter_port_grp"+wanAccessPolicyNumber, EMPTY_STRING)
                                    .setProperty("filter_rule"+wanAccessPolicyNumber, EMPTY_STRING)
                                    .setProperty("filter_tod"+wanAccessPolicyNumber, EMPTY_STRING)
                                    .setProperty("filter_tod_buf"+wanAccessPolicyNumber, EMPTY_STRING)
                                    .setProperty("filter_web_host"+wanAccessPolicyNumber, EMPTY_STRING)
                                    .setProperty("filter_web_url"+wanAccessPolicyNumber, EMPTY_STRING);

                            ActionManager.runTasks(
                                new SetNVRAMVariablesAction(
                                        tile.mRouter,
                                        tile.mParentFragmentActivity,
                                        nvramVarsToSet,
                                        false,
                                        new RouterActionListener() {
                                            @Override
                                            public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router, Object returnData) {
                                                tile.mParentFragmentActivity.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        try {
                                                            Utils.displayMessage(tile.mParentFragmentActivity,
                                                                    String.format("WAN Access Policy '%s' successfully deleted on host '%s'. ",
                                                                            wanAccessPolicy.getName(),
                                                                            tile.mRouter.getCanonicalHumanReadableName()),
                                                                    Style.CONFIRM);
                                                        } finally {
                                                            holder.statusSwitchButton.setEnabled(true);
                                                            wanAccessPolicies.remove(wanAccessPolicy);
                                                            //Success- update recycler view
                                                            notifyItemRemoved(holder.getAdapterPosition());
                                                        }
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull Router router, @Nullable final Exception exception) {
                                                tile.mParentFragmentActivity.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        try {
                                                            Utils.displayMessage(tile.mParentFragmentActivity,
                                                                    String.format("Error while trying to delete WAN Access Policy '%s' on '%s': %s",
                                                                            wanAccessPolicy.getName(),
                                                                            tile.mRouter.getCanonicalHumanReadableName(),
                                                                            Utils.handleException(exception).first),
                                                                    Style.ALERT);
                                                        } finally {
                                                            holder.statusSwitchButton.setEnabled(true);
                                                        }
                                                    }
                                                });
                                            }
                                        },
                                        tile.mGlobalPreferences,
                                        "/sbin/stopservice firewall",
                                        "/sbin/startservice firewall"));

                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //Cancelled - nothing more to do!
                        }
                    }).create();

            if (!ColorUtils.isThemeLight(tile.mParentFragmentActivity)) {
                //Set menu background to white
                holder.menuImageButton.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
            }

            holder.menuImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final PopupMenu popup = new PopupMenu(tile.mParentFragmentActivity, v);
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.tile_wan_access_policy_toggle:
                                    holder.statusSwitchButton.performClick();
                                    return true;
                                case R.id.tile_wan_access_policy_remove:
                                    removeWanPolicyDialog.show();
                                    return true;
                                case R.id.tile_wan_access_policy_edit:
                                    //TODO Edit: open up edit popup or, better, a completely different setting activity
                                    Toast.makeText(tile.mParentFragmentActivity,
                                            "[TODO] Edit WAN Access Policy #" + wanAccessPolicy.getNumber() +
                                                    " (" + wanAccessPolicy.getName() + ")", Toast.LENGTH_SHORT).show();
                                    return true;
                                default:
                                    break;
                            }
                            return false;
                        }
                    });
                    final MenuInflater inflater = popup.getMenuInflater();
                    final Menu menu = popup.getMenu();
                    inflater.inflate(R.menu.tile_wan_access_policy_options, menu);
                    popup.show();
                }
            });

            holder.removeImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeWanPolicyDialog.show();
                }
            });

        }

        @Override
        public int getItemCount() {
            return wanAccessPolicies.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            final CardView cardView;
            final TextView policyNb;
            final TextView policyName;
            final TextView policyHours;
            final TextView internetPolicyDuringSelectedTimeOfDay;

            final TextView[] daysTextViews = new TextView[7];

            final SwitchCompat statusSwitchButton;
            final ImageButton menuImageButton;
            final ImageButton removeImageButton;

            final ImageView avatarImageView;

            private final AccessRestrictionsWANAccessTile tile;

            public ViewHolder(AccessRestrictionsWANAccessTile tile, View itemView) {
                super(itemView);
                this.tile = tile;
                this.cardView = (CardView)
                        itemView.findViewById(R.id.access_restriction_policy_cardview);

                this.avatarImageView = (ImageView) itemView.findViewById(R.id.avatar);
                this.policyNb = (TextView)
                        itemView.findViewById(R.id.access_restriction_policy_cardview_number);
                this.policyName = (TextView)
                        itemView.findViewById(R.id.access_restriction_policy_cardview_name);
                this.policyHours = (TextView)
                        itemView.findViewById(R.id.access_restriction_policy_cardview_hours);
                this.internetPolicyDuringSelectedTimeOfDay = (TextView)
                        itemView.findViewById(R.id.access_restriction_policy_cardview_internet_policy);

                this.daysTextViews[0] = (TextView)
                        itemView.findViewById(R.id.access_restriction_policy_cardview_days_sunday);
                this.daysTextViews[1] = (TextView)
                        itemView.findViewById(R.id.access_restriction_policy_cardview_days_monday);
                this.daysTextViews[2] = (TextView)
                        itemView.findViewById(R.id.access_restriction_policy_cardview_days_tuesday);
                this.daysTextViews[3] = (TextView)
                        itemView.findViewById(R.id.access_restriction_policy_cardview_days_wednesday);
                this.daysTextViews[4] = (TextView)
                        itemView.findViewById(R.id.access_restriction_policy_cardview_days_thursday);
                this.daysTextViews[5] = (TextView)
                        itemView.findViewById(R.id.access_restriction_policy_cardview_days_friday);
                this.daysTextViews[6] = (TextView)
                        itemView.findViewById(R.id.access_restriction_policy_cardview_days_saturday);

                this.statusSwitchButton = (SwitchCompat)
                        itemView.findViewById(R.id.access_restriction_policy_cardview_status);
                this.menuImageButton = (ImageButton)
                        itemView.findViewById(R.id.access_restriction_policy_cardview_menu);
                this.removeImageButton = (ImageButton)
                        itemView.findViewById(R.id.access_restriction_policy_remove_btn);
            }
        }
    }

    static class WANAccessPoliciesRouterData extends
            RouterData<List<WANAccessPolicy>> {

    }

    static class WANAccessPolicyException extends DDWRTCompanionException {
        public WANAccessPolicyException() {
        }

        public WANAccessPolicyException(@Nullable String detailMessage) {
            super(detailMessage);
        }

        public WANAccessPolicyException(@Nullable String detailMessage, @Nullable Throwable throwable) {
            super(detailMessage, throwable);
        }

        public WANAccessPolicyException(@Nullable Throwable throwable) {
            super(throwable);
        }
    }
}
