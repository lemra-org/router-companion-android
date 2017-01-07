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

package org.rm3l.router_companion.mgmt.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;

import org.rm3l.router_companion.BuildConfig;
import org.rm3l.router_companion.R;
import org.rm3l.router_companion.actions.ActionManager;
import org.rm3l.router_companion.actions.RebootRouterAction;
import org.rm3l.router_companion.actions.RouterAction;
import org.rm3l.router_companion.actions.RouterActionListener;
import org.rm3l.router_companion.main.DDWRTMainActivity;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.router_companion.mgmt.register.ManageRouterFragmentActivity;
import org.rm3l.router_companion.mgmt.register.resources.RouterWizardAction;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.status.wireless.WirelessClientsTile;
import org.rm3l.router_companion.utils.AdUtils;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.utils.ReportingUtils;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import co.paulburke.android.itemtouchhelperdemo.helper.ItemTouchHelperAdapter;
import co.paulburke.android.itemtouchhelperdemo.helper.ItemTouchHelperViewHolder;
import co.paulburke.android.itemtouchhelperdemo.helper.OnStartDragListener;
import de.keyboardsurfer.android.widget.crouton.Style;

import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.rm3l.router_companion.mgmt.RouterManagementActivity.NEW_ROUTER_ADDED;
import static org.rm3l.router_companion.mgmt.RouterManagementActivity.ROUTER_SELECTED;
import static org.rm3l.router_companion.mgmt.RouterManagementActivity.ROUTER_UPDATED;
import static org.rm3l.router_companion.RouterCompanionAppConstants.DDWRTCOMPANION_WANACCESS_IPTABLES_CHAIN;
import static org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.router_companion.RouterCompanionAppConstants.MAX_ROUTERS_FREE_VERSION;
import static org.rm3l.router_companion.RouterCompanionAppConstants.OPENED_AT_LEAST_ONCE_PREF_KEY;
import static org.rm3l.router_companion.RouterCompanionAppConstants.getClientsUsageDataFile;

public class RouterListRecycleViewAdapter extends
        RecyclerView.Adapter<RouterListRecycleViewAdapter.ViewHolder>
        implements ItemTouchHelperAdapter, Filterable {

    private static final String TAG = RouterListRecycleViewAdapter.class.getSimpleName();

    public static final String EMPTY = "(empty)";
    public static final String ITEM_DISMISS_POS = "ITEM_DISMISS_POS";
    public static final String POSITION_PREF_KEY_PREF = "pos::";
    final DDWRTCompanionDAO dao;
    private final Activity activity;
    private final Resources resources;
    private final SharedPreferences mGlobalPreferences;
    private final Filter mFilter;
    private List<Router> routersList;
    private SparseBooleanArray selectedItems;

    @Nullable
    private InterstitialAd mInterstitialAd;

    private OnStartDragListener mDragStartListener;

    public RouterListRecycleViewAdapter(final Activity activity, final List<Router> results) {
        this.setRoutersList(results);
        this.activity = activity;
        this.mGlobalPreferences = activity.getSharedPreferences(
                RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        this.dao = RouterManagementActivity.getDao(activity);
        resources = activity.getResources();
        selectedItems = new SparseBooleanArray();

        mFilter = new Filter() {
            @Override
            protected FilterResults performFiltering(final CharSequence constraint) {
                final List<Router> routers = dao.getAllRouters();
                final FilterResults oReturn = new FilterResults();
                if (routers == null || routers.isEmpty()) {
                    return oReturn;
                }

                if (TextUtils.isEmpty(constraint)) {
                    oReturn.values = routers;
                } else {
                    //Filter routers list
                    oReturn.values = FluentIterable
                            .from(routers)
                            .filter(new Predicate<Router>() {
                                @Override
                                public boolean apply(Router input) {
                                    if (input == null) {
                                        return false;
                                    }
                                    //Filter on visible fields (Name, Remote IP, Firmware and SSH Username, Method, router model)
                                    final Router.RouterFirmware routerFirmware = input.getRouterFirmware();
                                    final Router.RouterConnectionProtocol routerConnectionProtocol = input.getRouterConnectionProtocol();
                                    final String inputModel = activity.getSharedPreferences(input.getUuid(), Context.MODE_PRIVATE)
                                            .getString(NVRAMInfo.MODEL, "");
                                    //noinspection ConstantConditions
                                    return containsIgnoreCase(input.getName(), constraint)
                                            || containsIgnoreCase(input.getRemoteIpAddress(), constraint)
                                            || containsIgnoreCase(inputModel, constraint)
                                            || (routerFirmware != null && containsIgnoreCase(routerFirmware.toString(), constraint))
                                            || containsIgnoreCase(input.getUsernamePlain(), constraint)
                                            || (routerConnectionProtocol != null && containsIgnoreCase(routerConnectionProtocol.toString(), constraint));
                                }
                            }).toList();
                }

                return oReturn;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                final Object values = results.values;
                if (values instanceof List) {
                    setRoutersList(new ArrayList<>((List<Router>) values));
                    notifyDataSetChanged();
                }
            }
        };

        mInterstitialAd = AdUtils.requestNewInterstitial(activity,
                R.string.interstitial_ad_unit_id_router_list_to_router_main);
    }

    public RouterListRecycleViewAdapter setDragStartListener(OnStartDragListener mDragStartListener) {
        this.mDragStartListener = mDragStartListener;
        return this;
    }

    public List<Router> getRoutersList() {
        return routersList;
    }

    public void setRoutersList(final List<Router> results) {
        routersList = results;
        if (routersList != null) {
            //Re-order just in case
            Collections.sort(routersList, new Comparator<Router>() {
                @Override
                public int compare(Router o1, Router o2) {
                    return o1.getOrderIndex() - o2.getOrderIndex();
                }
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.router_mgmt_layout_row_view, parent, false);
        // set the view's size, margins, paddings and layout parameters
        // ...
        final CardView cardView = (CardView) v.findViewById(R.id.router_item_cardview);
        if (ColorUtils.isThemeLight(activity)) {
            //Light
            cardView.setCardBackgroundColor(ContextCompat
                    .getColor(activity, R.color.cardview_light_background));
        } else {
            //Default is Dark
            cardView.setCardBackgroundColor(ContextCompat
                    .getColor(activity, R.color.cardview_dark_background));
        }

//        return new ViewHolder(this.context,
//                RippleViewCreator.addRippleToView(v));
        return new ViewHolder(this.activity, v);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final Router routerAt = routersList.get(position);

        // Start a drag whenever the handle view it touched
        if (holder.handleView != null) {
            holder.handleView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (mDragStartListener != null) {
                        if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                            mDragStartListener.onStartDrag(holder);
                        }
                    }
                    return false;
                }
            });
        }

        holder.routerUuid.setText(routerAt.getUuid());
        final String routerAtName = routerAt.getName();
        final String routerNameDisplayed = (Strings.isNullOrEmpty(routerAtName) ?
            EMPTY : routerAtName);
        if (Strings.isNullOrEmpty(routerAtName)) {
            //Italic
            holder.routerName.setText(EMPTY);
//            holder.routerName.setTypeface(null, Typeface.ITALIC);
        } else {
            holder.routerName.setText(routerAtName);
        }
        final String remoteIpAddress = routerAt.getRemoteIpAddress();
        final boolean isDemoRouter = Utils.isDemoRouter(remoteIpAddress);

        holder.routerIp.setText(isDemoRouter ?
                RouterCompanionAppConstants.DEMO :
                (remoteIpAddress + ":" + routerAt.getRemotePort()));
        holder.routerConnProto.setText(routerAt.getRouterConnectionProtocol().toString());
        holder.routerUsername.setText(routerAt.getUsernamePlain());
        if (isDemoRouter) {
            holder.routerFirmware.setVisibility(View.GONE);
        } else {
            holder.routerFirmware.setVisibility(View.VISIBLE);
            final Router.RouterFirmware routerFirmware = routerAt.getRouterFirmware();
            holder.routerFirmware.setText("Firmware: " + (routerFirmware != null ? routerFirmware.getDisplayName() : "-"));
        }

        if (isDemoRouter) {
            holder.routerUsernameAndProtoView.setVisibility(View.GONE);
        } else {
            holder.routerUsernameAndProtoView.setVisibility(View.VISIBLE);
        }

        final String routerModelStr = Router.getRouterModel(activity, routerAt);
        if (Strings.isNullOrEmpty(routerModelStr) || "-".equals(routerModelStr)) {
            holder.routerModel.setVisibility(View.GONE);
        } else {
            holder.routerModel.setText("Model: " + routerModelStr);
            holder.routerModel.setVisibility(View.VISIBLE);
        }

        final boolean isThemeLight = ColorUtils.isThemeLight(this.activity);

        if (!isThemeLight) {
            //Set menu background to white
            holder.routerMenu.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
            holder.routerOpenButton.setImageResource(R.drawable.ic_action_av_play_arrow_dark);
            holder.routerAvatarImage.setBackgroundColor(ContextCompat
                    .getColor(activity, R.color.cardview_dark_background));
        } else {
            holder.routerAvatarImage.setBackgroundColor(ContextCompat
                    .getColor(activity, R.color.cardview_light_background));
        }

        holder.routerOpenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doOpenRouterDetails(routerAt);
            }
        });

        if (Strings.isNullOrEmpty(routerModelStr) || Utils.isDemoRouter(routerAt)) {
            if (Strings.isNullOrEmpty(routerModelStr)) {
                Crashlytics.log("Router Model could not be detected");
            }
            holder.routerAvatarImage.setImageResource(
                    Utils.isDemoRouter(routerAt) ?
                        R.drawable.demo_router : R.drawable.router);
        } else {
            //final String[] opts = new String[] {"w_65","h_45", "e_sharpen"};
            final String[] opts = new String[] 
            {
                "w_300",
                "h_300",
                "q_100",
                "c_thumb",
                "g_center",
                "r_20",
                "e_improve",
                "e_make_transparent",
                "e_trim"
            };

            //Download image in the background
            Utils.downloadImageForRouter(activity,
                    routerModelStr,
                    holder.routerAvatarImage,
                    null,
                    null,
                    R.drawable.router,
                    opts);
        }

        holder.itemView.post(new Runnable() {
            // Post in the parent's message queue to make sure the parent
            // lays out its children before you call getHitRect()
            @Override
            public void run() {

                setClickListenerForNestedView(
                        holder.routerViewParent,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //Open Router
                                doOpenRouterDetails(routerAt);
                            }
                        },
//                        new View.OnLongClickListener() {
//                            @Override
//                            public boolean onLongClick(View v) {
//                                createContextualPopupMenu(v, routerAt);
//                                return true;
//                            }
//                        }
                        null
                );

                // The bounds for the delegate view (an ImageButton
                // in this example)
                setClickListenerForNestedView(holder.routerMenu, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        createContextualPopupMenu(v, routerAt);
                    }
                }, null);

                setClickListenerForNestedView(holder.routerOpenButton, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        doOpenRouterDetails(routerAt);
                    }
                }, null);
//
                if (holder.handleView != null) {
                    setClickListenerForNestedView(holder.handleView,
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (mDragStartListener != null) {
                                        mDragStartListener.onStartDrag(holder);
                                    }
                                }
                            }, null);
                }
            }
        });
    }

    private void createContextualPopupMenu(View v, Router routerAt) {
        final PopupMenu popup = new PopupMenu(activity, v);
        popup.setOnMenuItemClickListener(new RouterItemMenuOnClickListener(routerAt));
        final MenuInflater inflater = popup.getMenuInflater();
        final Menu menu = popup.getMenu();
        inflater.inflate(R.menu.menu_router_list_selection_menu, menu);
        menu.findItem(R.id.action_actions_reboot_routers)
                .setTitle("Reboot");
        menu.findItem(R.id.menu_router_item_open)
                .setVisible(true);
        menu.findItem(R.id.menu_router_item_open)
                .setEnabled(true);
        popup.show();
    }

    private void setClickListenerForNestedView(@NonNull View view,
                                               @Nullable final View.OnClickListener clickListener,
                                               @Nullable final View.OnLongClickListener longClickListener)
    {
        final Rect delegateArea = new Rect();
        view.setEnabled(true);
        if (clickListener != null) {
            view.setOnClickListener(clickListener);
        }
        if (longClickListener != null) {
            view.setOnLongClickListener(longClickListener);
        }

        // The hit rectangle for the ImageButton
        view.getHitRect(delegateArea);

        // Extend the touch area of the ImageButton beyond its bounds
        // on the right and bottom.
        delegateArea.right += 100;
        delegateArea.bottom += 100;

        // Instantiate a TouchDelegate.
        // "delegateArea" is the bounds in local coordinates of
        // the containing view to be mapped to the delegate view.
        // "myButton" is the child view that should receive motion
        // events.
        final TouchDelegate touchDelegate = new TouchDelegate(delegateArea,
                view);

        // Sets the TouchDelegate on the parent view, such that touches
        // within the touch delegate bounds are routed to the child.
        if (View.class.isInstance(view.getParent())) {
            ((View) view.getParent()).setTouchDelegate(touchDelegate);
        }
    }

    @Override
    public int getItemCount() {
        return routersList.size();
    }

    public void toggleSelection(int pos) {
        if (selectedItems.get(pos, false)) {
            selectedItems.delete(pos);
        } else {
            selectedItems.put(pos, true);
        }
        setRoutersList(dao.getAllRouters());
        notifyItemChanged(pos);
    }

    public void clearSelections() {
        selectedItems.clear();
        setRoutersList(dao.getAllRouters());
        notifyDataSetChanged();
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    @Nullable
    public Integer findRouterPosition(@Nullable final String routerUuid) {
        if (routerUuid == null || routerUuid.isEmpty()) {
            return null;
        }
        for (int i = 0; i < routersList.size(); i++) {
            final Router router = routersList.get(i);
            if (router == null) {
                continue;
            }
            if (routerUuid.equals(router.getUuid())) {
                return i;
            }
        }
        return null;
    }

    /**
     * Removes the item that currently is at the passed in position from the
     * underlying data set.
     *
     * @param position The index of the item to remove.
     * @return the number of elements in the DB
     */
    public int removeData(int position) {
        if (position >= 0 && position < this.routersList.size()) {
            final Router router = this.routersList.get(position);
            if (router != null) {
                dao.deleteRouter(router.getUuid());

                //Also Remove Usage Data Created
                //noinspection ResultOfMethodCallIgnored
                getClientsUsageDataFile(activity, router.getUuid()).delete();

                final SharedPreferences sharedPreferences = this.activity.getSharedPreferences(router.getUuid(), Context.MODE_PRIVATE);

                if (sharedPreferences.getBoolean(OPENED_AT_LEAST_ONCE_PREF_KEY, false)) {
                    //Opened at least once, meaning that usage data might have been created.
                    //If never opened, do nothing, as usage data might be used by another router record
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                //Delete iptables chains created for monitoring and wan access (in a thread)
                                SSHUtils.runCommands(activity, activity
                                                .getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE),
                                        router,
                                        Joiner.on(" ; ").skipNulls(),

                                        "iptables -D FORWARD -j " + DDWRTCOMPANION_WANACCESS_IPTABLES_CHAIN,
                                        "iptables -F " + DDWRTCOMPANION_WANACCESS_IPTABLES_CHAIN,
                                        "iptables -X " + DDWRTCOMPANION_WANACCESS_IPTABLES_CHAIN,

                                        "iptables -D FORWARD -j DDWRTCompanion",
                                        "iptables -F DDWRTCompanion",
                                        "iptables -X DDWRTCompanion",

                                        "rm -f " + RouterCompanionAppConstants.WRTBWMON_DDWRTCOMPANION_SCRIPT_FILE_PATH_REMOTE,
                                        "rm -f /tmp/.DDWRTCompanion_traffic_55.tmp",
                                        "rm -f /tmp/.DDWRTCompanion_traffic_66.tmp",
                                        "rm -f " + WirelessClientsTile.USAGE_DB,
                                        "rm -f " + WirelessClientsTile.USAGE_DB_OUT);

                            } catch (final Exception e) {
                                e.printStackTrace();
                                //No Worries
                            } finally {
                                //Disconnect session
                                destroySSHSession(router);
                            }
                        }
                    }).start();
                }

                //Drop SharedPreferences for this item too
                sharedPreferences.edit().clear().apply();

                //Now refresh list
                final List<Router> allRouters = dao.getAllRouters();
                setRoutersList(allRouters);
                notifyItemRemoved(position);
                return allRouters.size();
            }
        }
        return dao.getAllRouters().size();
    }

    private void destroySSHSession(@NonNull final Router router) {
        //Async to avoid ANR because SSHUtils#destroySession makes use of locking mechanisms
        new Thread(new Runnable() {
            @Override
            public void run() {
                SSHUtils.destroySessions(router);
            }
        }).start();
    }

    @NonNull
    public List<Integer> getSelectedItems() {
        final List<Integer> items = new ArrayList<Integer>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(routersList, i, i + 1);
            }

        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(routersList, i, i - 1);
            }
        }

        //Persist indexes
        for (int orderIdx = 0; orderIdx < routersList.size(); orderIdx++) {
            final Router router = routersList.get(orderIdx);
            final int existingOrderIndex = router.getOrderIndex();
            router.setOrderIndex(orderIdx);
            dao.updateRouter(router);
            Crashlytics.log(Log.DEBUG, TAG,
                    "XXX Router '" + router.getCanonicalHumanReadableName() + "' new position: "
                            + existingOrderIndex + " => " + orderIdx);
        }

        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onItemDismiss(final RecyclerView mRecyclerView, final RecyclerView.ViewHolder viewHolder) {

        final int position = viewHolder.getAdapterPosition();

        Crashlytics.log(Log.DEBUG, TAG, "XXX onItemDismiss: position = " + position);

        final Router router = routersList.get(position);

        final Snackbar snackbar = Snackbar.make(mRecyclerView,
                String.format("Removing Router '%s'...", router.getCanonicalHumanReadableName()),
                Snackbar.LENGTH_LONG)
                .setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
//                        final int position = viewHolder.getAdapterPosition();
                        Crashlytics.log(Log.DEBUG, TAG, "XXX onItemDismiss UNDO Click: position = " + position);

                        //Unarchive
                        router.setArchived(false);
                        dao.updateRouter(router);
                        routersList.add(position, router);
//                        setRoutersList(dao.getAllRouters());
                        notifyItemInserted(position);
                        mRecyclerView.scrollToPosition(position);
                    }
                })
                .setActionTextColor(Color.RED);

        final View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(Color.DKGRAY);
        final TextView textView = (TextView)
                snackbarView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.YELLOW);
        snackbar.show();

        router.setArchived(true);
        dao.updateRouter(router); //Actual archive
        routersList.remove(position);
//        dao.deleteRouter(router.getUuid()); //Actual delete
        notifyItemRemoved(position);
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder implements
            ItemTouchHelperViewHolder {

        @NonNull
        final TextView routerName;
        @NonNull
        final TextView routerIp;
        @NonNull
        final TextView routerConnProto;
        @NonNull
        final TextView routerUuid;
        @NonNull
        final TextView routerUsername;
        @NonNull
        final TextView routerFirmware;
        @NonNull
        final TextView routerModel;
        private final Context mContext;
        @NonNull
        private ImageButton routerMenu;
        @NonNull
        private ImageButton routerOpenButton;
        @NonNull
        private ImageView routerAvatarImage;

        @NonNull
        private View routerUsernameAndProtoView;

        private final View itemView;

        private final View routerViewParent;

        public final ImageView handleView;

        public ViewHolder(Context context, View itemView) {
            super(itemView);
            mContext = context;

            this.itemView = itemView;
            this.routerViewParent = this.itemView.findViewById(R.id.router_view_parent);

            this.handleView = (ImageView) this.itemView.findViewById(R.id.router_view_handle);

            this.routerName = (TextView) this.itemView.findViewById(R.id.router_name);
            this.routerIp = (TextView) this.itemView.findViewById(R.id.router_ip_address);
            this.routerConnProto = (TextView) this.itemView.findViewById(R.id.router_connection_protocol);
            this.routerUuid = (TextView) this.itemView.findViewById(R.id.router_uuid);
            this.routerUsername = (TextView) this.itemView.findViewById(R.id.router_username);
            this.routerFirmware = (TextView) this.itemView.findViewById(R.id.router_firmware);
            this.routerModel = (TextView) this.itemView.findViewById(R.id.router_model);

            this.routerMenu = (ImageButton) this.itemView.findViewById(R.id.router_menu);
            this.routerOpenButton = (ImageButton) this.itemView.findViewById(R.id.router_go);

            this.routerAvatarImage = (ImageView) this.itemView.findViewById(R.id.router_avatar);

            this.routerUsernameAndProtoView = this.itemView.findViewById(R.id.router_username_and_proto);
        }

        @Override
        public void onItemSelected() {
            itemView.setBackgroundColor(Color.LTGRAY);
//            if (ColorUtils.isThemeLight(mContext)) {
//                itemView.setBackgroundColor(ContextCompat
//                        .getColor(mContext, R.color.cardview_dark_background));
//            } else {
//                itemView.setBackgroundColor(ContextCompat
//                        .getColor(mContext, R.color.cardview_light_background));
//            }
        }

        @Override
        public void onItemClear() {
            if (ColorUtils.isThemeLight(mContext)) {
                itemView.setBackgroundColor(ContextCompat
                        .getColor(mContext, R.color.cardview_light_background));
            } else {
                itemView.setBackgroundColor(ContextCompat
                        .getColor(mContext, R.color.cardview_dark_background));
            }
//            itemView.setBackgroundColor(0);
        }

    }

    class RouterItemMenuOnClickListener implements PopupMenu.OnMenuItemClickListener {

        final Router mRouter;

        public RouterItemMenuOnClickListener(final Router router) {
            this.mRouter = router;
        }

        @SuppressLint("DefaultLocale")
        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            final Integer itemPos = findRouterPosition(mRouter.getUuid());

            switch (menuItem.getItemId()) {
                case R.id.menu_router_item_open: {
                    doOpenRouterDetails(mRouter);
                }
                    return true;
                case R.id.action_actions_ssh_router: {
                    //Open an SSH Client app, if any
                    Router.openSSHConsole(mRouter, activity);
                }
                    return true;
                case R.id.action_actions_reboot_routers: {

                    new AlertDialog.Builder(activity)
                            .setIcon(R.drawable.ic_action_alert_warning)
                            .setTitle("Reboot Router?")
                            .setMessage("Are you sure you wish to continue? ")
                            .setCancelable(true)
                            .setPositiveButton("Proceed!", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(final DialogInterface dialogInterface, final int i) {

                                    final String infoMsg = String.format("Rebooting '%s' (%s)...",
                                            mRouter.getDisplayName(), mRouter.getRemoteIpAddress());
                                    if (activity instanceof Activity) {
                                        Utils.displayMessage((Activity) activity,
                                                infoMsg,
                                                Style.INFO);
                                    } else {
                                        Toast.makeText(activity, infoMsg, Toast.LENGTH_SHORT).show();
                                    }


                                    final RouterActionListener rebootRouterActionListener = new RouterActionListener() {
                                        @Override
                                        public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router, Object returnData) {

                                            //No error
                                            final String msg = String.format("Action '%s' executed successfully",
                                                    routerAction.toString());
                                            if (activity instanceof Activity) {
                                                Utils.displayMessage((Activity) activity,
                                                        msg,
                                                        Style.CONFIRM);
                                            } else {
                                                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                        @Override
                                        public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull Router router, @Nullable Exception exception) {
                                            //An error occurred
                                            final String msg = String.format("Error: %s",
                                                    Utils.handleException(exception).first);

                                            if (activity instanceof Activity) {
                                                Utils.displayMessage((Activity) activity,
                                                        msg,
                                                        Style.ALERT);
                                            } else {
                                                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    };

                                    ActionManager.runTasks(new RebootRouterAction(mRouter, activity,
                                            rebootRouterActionListener,
                                            mGlobalPreferences));

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
                case R.id.menu_router_list_delete: {
                    if (itemPos == null || itemPos < 0) {
                        Toast.makeText(activity, "Internal Error - please try again later", Toast.LENGTH_SHORT)
                                .show();
                        ReportingUtils.reportException(null, new IllegalStateException("Weird routerPosition: " + itemPos));
                        return true;
                    }

                    new AlertDialog.Builder(activity)
                            .setIcon(R.drawable.ic_action_alert_warning)
                            .setTitle("Delete Router?")
                            .setMessage("You'll lose this record!")
                            .setCancelable(true)
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(final DialogInterface dialogInterface, final int i) {
                                    int numberOfItems = removeData(itemPos);
                                    if (numberOfItems == 0) {
                                        //All items dropped = open up 'Add Router' dialog
                                        openAddRouterForm();
                                    }

//                                    if (activity instanceof Activity) {
//                                        Crouton.makeText((Activity) activity,
//                                                "Record deleted", Style.CONFIRM).show();
//                                    } else {
//                                        Toast.makeText(activity, "Record deleted", Toast.LENGTH_SHORT)
//                                                .show();
//                                    }

                                    //Request Backup
                                    Utils.requestBackup(activity);
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
                case R.id.menu_router_item_edit: {
                    openUpdateRouterForm(mRouter);
                }
                return true;
                case R.id.menu_router_item_copy: {
                    openDuplicateRouterForm(mRouter);
                }
                return true;
                case R.id.menu_router_list_add_home_shortcut: {
                    mRouter.addHomeScreenShortcut(activity);
                }
                return true;
                default:
                    return false;
            }
        }

    }

    private void doOpenRouterDetails(@NonNull final Router router) {
        final String routerUuid = router.getUuid();

        final Intent ddWrtMainIntent = new Intent(activity, DDWRTMainActivity.class);
        ddWrtMainIntent.putExtra(ROUTER_SELECTED, routerUuid);

        final SharedPreferences routerSharedPreferences =
                activity.getSharedPreferences(routerUuid, Context.MODE_PRIVATE);
        if (!routerSharedPreferences.getBoolean(OPENED_AT_LEAST_ONCE_PREF_KEY, false)) {
            routerSharedPreferences.edit()
                    .putBoolean(OPENED_AT_LEAST_ONCE_PREF_KEY, true)
                    .apply();
        }

        if (BuildConfig.WITH_ADS &&
                mInterstitialAd != null &&
                AdUtils.canDisplayInterstialAd(activity)) {
            mInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    startActivity(ddWrtMainIntent);
                }

                @Override
                public void onAdOpened() {
                    mGlobalPreferences.edit()
                            .putLong(
                                    RouterCompanionAppConstants.AD_LAST_INTERSTITIAL_PREF,
                                    System.currentTimeMillis())
                            .apply();
                }
            });

            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
            } else {
//                    final AlertDialog alertDialog = Utils.buildAlertDialog(this, null, "Loading...", false, false);
//                    alertDialog.show();
//                    ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                final ProgressDialog alertDialog = ProgressDialog.show(activity,
                        "Loading Router details", "Please wait...", true);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(ddWrtMainIntent);
                        alertDialog.cancel();
                    }
                }, 1000);
            }

        } else {
//                final AlertDialog alertDialog = Utils.buildAlertDialog(this, null, "Loading...", false, false);
//                alertDialog.show();
//                ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
            final ProgressDialog alertDialog = ProgressDialog.show(activity,
                    "Loading Router details", "Please wait...", true);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivity(ddWrtMainIntent);
                    alertDialog.cancel();
                }
            }, 1000);
        }
    }

    private void startActivity(Intent ddWrtMainIntent) {
        if (activity instanceof Activity) {
            RouterManagementActivity.startActivity((Activity) activity, null, ddWrtMainIntent);
        } else {
            //Start in a much more classical way
            activity.startActivity(ddWrtMainIntent);
        }
    }

    private void openAddRouterForm() {
        if (!(activity instanceof FragmentActivity)) {
            ReportingUtils.reportException(null, new IllegalStateException("activity is NOT an FragmentActivity"));
            return;
        }
//        final FragmentActivity activity = (FragmentActivity) activity;
//        final FragmentManager fragmentManager = activity.getSupportFragmentManager();

//        final Fragment addRouter = fragmentManager
//                .findFragmentByTag(ADD_ROUTER_FRAGMENT_TAG);
//        if (addRouter instanceof DialogFragment) {
//            ((DialogFragment) addRouter).dismiss();
//        }

        //Display Donate Message if trying to add more than the max routers for Free version
        final List<Router> allRouters = dao.getAllRouters();
        //noinspection PointlessBooleanExpression,ConstantConditions
        if ((BuildConfig.DONATIONS || BuildConfig.WITH_ADS) &&
                allRouters != null && allRouters.size() >= MAX_ROUTERS_FREE_VERSION) {
            //Download the full version to unlock this version
            Utils.displayUpgradeMessage(activity, "Manage a new Router");
            return;
        }

//        final DialogFragment addFragment = new RouterAddDialogFragment();
//        addFragment.show(fragmentManager, ADD_ROUTER_FRAGMENT_TAG);

        final Intent intent = new Intent(activity, ManageRouterFragmentActivity.class);
        intent.putExtra(RouterWizardAction.ROUTER_WIZARD_ACTION, RouterWizardAction.ADD);
        activity.startActivityForResult(intent, NEW_ROUTER_ADDED);
    }

    private void openUpdateRouterForm(@Nullable Router router) {
        if (!(activity instanceof FragmentActivity)) {
            ReportingUtils.reportException(null, new IllegalStateException("activity is NOT an FragmentActivity"));
            return;
        }
//        final FragmentActivity activity = (FragmentActivity) activity;
//        final FragmentManager fragmentManager = activity.getSupportFragmentManager();

        if (router != null) {
//            final DialogFragment updateFragment = new RouterUpdateDialogFragment();
//            final Bundle args = new Bundle();
//            args.putString(ROUTER_SELECTED, router.getUuid());
//            updateFragment.setArguments(args);
//            updateFragment.show(fragmentManager, UPDATE_ROUTER_FRAGMENT_TAG);

            final Intent intent = new Intent(activity, ManageRouterFragmentActivity.class);
            intent.putExtra(ROUTER_SELECTED, router.getUuid());
            intent.putExtra(RouterWizardAction.ROUTER_WIZARD_ACTION, RouterWizardAction.EDIT);
            activity.startActivityForResult(intent, ROUTER_UPDATED);

        } else {
            Toast.makeText(activity, "Entry no longer exists", Toast.LENGTH_SHORT).show();
        }

    }

    private void openDuplicateRouterForm(@Nullable Router router) {
        if (!(activity instanceof FragmentActivity)) {
            ReportingUtils.reportException(null, new IllegalStateException("activity is NOT an FragmentActivity"));
            return;
        }
//        final FragmentActivity activity = (FragmentActivity) context;
//        final FragmentManager fragmentManager = activity.getSupportFragmentManager();

        //Display Donate Message if trying to add more than the max routers for Free version
        final List<Router> allRouters = dao.getAllRouters();
        //noinspection PointlessBooleanExpression,ConstantConditions
        if ((BuildConfig.DONATIONS || BuildConfig.WITH_ADS) &&
                allRouters != null && allRouters.size() >= MAX_ROUTERS_FREE_VERSION) {
            //Download the full version to unlock this version
            Utils.displayUpgradeMessage(activity, "Duplicate Router");
            return;
        }

        if (router != null) {
//            final DialogFragment copyFragment = new RouterDuplicateDialogFragment();
//            final Bundle args = new Bundle();
//            args.putString(ROUTER_SELECTED, router.getUuid());
//            copyFragment.setArguments(args);
//            copyFragment.show(fragmentManager, COPY_ROUTER);
            final Intent intent = new Intent(activity, ManageRouterFragmentActivity.class);
            intent.putExtra(ROUTER_SELECTED, router.getUuid());
            intent.putExtra(RouterWizardAction.ROUTER_WIZARD_ACTION, RouterWizardAction.COPY);
            activity.startActivityForResult(intent, NEW_ROUTER_ADDED);
        } else {
            Toast.makeText(activity, "Entry no longer exists", Toast.LENGTH_SHORT).show();
        }
    }

}