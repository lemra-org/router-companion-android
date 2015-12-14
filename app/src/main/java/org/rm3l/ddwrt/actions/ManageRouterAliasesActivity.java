package org.rm3l.ddwrt.actions;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import android.support.v4.content.ContextCompat;
import com.android.supportv7.widget.decorator.DividerItemDecoration;
import com.google.android.gms.ads.AdView;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.AdUtils;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.Utils;

import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.THEMING_PREF;

/**
 * Created by rm3l on 13/12/15.
 */
public class ManageRouterAliasesActivity extends AppCompatActivity implements View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String LOG_TAG = ManageRouterAliasesActivity
            .class.getSimpleName();

    private boolean mIsThemeLight;
    private Router mRouter;

    private Toolbar mToolbar;
    private SharedPreferences mRouterPreferences;


    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;


    private FloatingActionButton addNewButton;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIsThemeLight = ColorUtils.isThemeLight(this);
        if (mIsThemeLight) {
            //Light
            setTheme(R.style.AppThemeLight);
        } else {
            //Default is Dark
            setTheme(R.style.AppThemeDark);
        }
        
        if (mIsThemeLight) {
            getWindow().getDecorView()
                    .setBackgroundColor(ContextCompat.getColor(this, android.R.color.white));
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

        mRecyclerView = (RecyclerView) findViewById(R.id.routerAliasesListView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        // allows for optimizations if all items are of the same size:
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.scrollToPosition(0);
        mRecyclerView.setLayoutManager(mLayoutManager);

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

    @Override
    public void onClick(View view) {
        if (view == null) {
            return;
        }
        if (view.getId() == R.id.router_alias_add) {
            //TODO Open Dialog - PS: Might be great to allow to see MAC OUI Vendor
            Toast.makeText(ManageRouterAliasesActivity.this, "[TODO] onClick(router_alias_add)",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRefresh() {
        mSwipeRefreshLayout.setEnabled(false);
        mSwipeRefreshLayout.setRefreshing(true);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    ((ManageRouterAliasesActivity.RouterAliasesListRecyclerViewAdapter)
                            ManageRouterAliasesActivity.this.mAdapter)
                                .setAliasesColl(FluentIterable
                                    .from(Router.getAliases(ManageRouterAliasesActivity.this, mRouter))
                                    .toList());
                    ManageRouterAliasesActivity.this.mAdapter.notifyDataSetChanged();
                } finally {
                    mSwipeRefreshLayout.setRefreshing(false);
                    mSwipeRefreshLayout.setEnabled(true);
                }
            }
        }, 1000);
    }

    static class RouterAliasesListRecyclerViewAdapter
            extends RecyclerView.Adapter<RouterAliasesListRecyclerViewAdapter.ViewHolder>
            implements Filterable {

        private final Context context;
        private final Filter mFilter;
        private List<Pair<String, String>> aliasesColl;
        private final Router mRouter;
        private final SharedPreferences mPreferences;

        public RouterAliasesListRecyclerViewAdapter(final Context context,
                                                    final Router mRouter) {
            this.context = context;
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
                    final Set<Pair<String, String>> aliases = RouterAliasesListRecyclerViewAdapter.this.mRouter
                            .getAliases(RouterAliasesListRecyclerViewAdapter.this.context);
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
                                        return containsIgnoreCase(macAddr, constraint)
                                                || containsIgnoreCase(alias, constraint);
                                    }
                                }).toSet();
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
            final long currentTheme = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
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
        public void onBindViewHolder(ViewHolder holder, final int position) {
            if (position < 0 || position >= aliasesColl.size()) {
                Utils.reportException(null, new IllegalStateException());
                Toast.makeText(context,
                        "Internal Error. Please try again later",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            final Pair<String, String> aliasPairAt =
                    aliasesColl.get(position);
            holder.macAddress.setText(aliasPairAt.first);
            holder.alias.setText(aliasPairAt.second);
            holder.removeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(context)
                            .setIcon(R.drawable.ic_action_alert_warning)
                            .setTitle(String.format("Drop Alias for '%s'?",
                                    aliasPairAt.first))
                            .setMessage("Are you sure you wish to continue? ")
                            .setCancelable(true)
                            .setPositiveButton("Proceed!", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mPreferences.edit()
                                            .remove(aliasPairAt.first)
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
                            }).create().show();
                }
            });

            holder.containerView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //TODO Allow to update alias
                    Toast.makeText(context, "[TODO] onClick =? update alias", Toast.LENGTH_SHORT).show();
                }
            });
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

            @NonNull
            final ImageButton removeButton;

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
            }
        }

    }

}