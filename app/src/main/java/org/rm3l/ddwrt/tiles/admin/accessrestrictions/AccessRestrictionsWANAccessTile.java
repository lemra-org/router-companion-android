package org.rm3l.ddwrt.tiles.admin.accessrestrictions;

import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Throwables;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.resources.RouterData;
import org.rm3l.ddwrt.resources.WANAccessPolicy;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;
import org.rm3l.ddwrt.widgets.RecyclerViewEmptySupport;

import java.util.ArrayList;
import java.util.List;

/**
 * WAN Access Policies tile
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

                    updateProgressBarViewSeparator(10);
                    final NVRAMInfo nvramInfo =
                                SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity,
                                        mRouter,
                                        mGlobalPreferences,
                                        "filter_");
                    if (nvramInfo == null) {
                        return null;
                    }
                    updateProgressBarViewSeparator(35);

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
                        //FIXME Now construct the list of policies
                    }

                    final AccessRestrictionsWANAccessTile.WANAccessPoliciesRouterData
                            routerData = (WANAccessPoliciesRouterData) new WANAccessPoliciesRouterData()
                            .setData(wanAccessPolicies);

                    updateProgressBarViewSeparator(90);

                    return routerData;

                } catch (@NonNull final Exception e) {
                    e.printStackTrace();
                    return (WANAccessPoliciesRouterData) new WANAccessPoliciesRouterData().setException(e);
                }
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<WANAccessPoliciesRouterData> loader,
                               WANAccessPoliciesRouterData data) {

        try {
            //Set tiles
            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

            List<WANAccessPolicy> wanAccessPolicies = null;
            if (data == null
                    || (wanAccessPolicies = data.getData()) == null) {
                data = (WANAccessPoliciesRouterData) new WANAccessPoliciesRouterData()
                        .setException(new DDWRTNoDataException("No Data!"));
            }

            layout.findViewById(R.id.tile_admin_access_restrictions_wan_access_loading_view).setVisibility(View.GONE);

            final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_admin_access_restrictions_wan_access_error);

            final Exception exception = data.getException();

            if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

                if (exception == null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }

                //TODO
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
            }
            return this;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.tile_admin_access_restrictions_list, parent, false);
            // set the view's size, margins, paddings and layout parameters
            // ...
            final ViewHolder vh = new ViewHolder(this.tile, v);
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
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

            //TODO
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

            final TextView[] daysTextViews = new TextView[7];

            final SwitchCompat statusSwitchButton;
            final ImageButton menuImageButton;

            private final AccessRestrictionsWANAccessTile tile;

            public ViewHolder(AccessRestrictionsWANAccessTile tile, View itemView) {
                super(itemView);
                this.tile = tile;
                this.cardView = (CardView)
                        itemView.findViewById(R.id.access_restriction_policy_cardview);
                this.policyNb = (TextView)
                        itemView.findViewById(R.id.access_restriction_policy_cardview_number);
                this.policyName = (TextView)
                        itemView.findViewById(R.id.access_restriction_policy_cardview_name);
                this.policyHours = (TextView)
                        itemView.findViewById(R.id.access_restriction_policy_cardview_hours);

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
            }
        }
    }

    static class WANAccessPoliciesRouterData extends
            RouterData<List<WANAccessPolicy>> {

    }
}
