/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Armel S.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.lemra.dd_wrt.mgmt;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lemra.dd_wrt.DDWRTMainActivity;
import org.lemra.dd_wrt.R;
import org.lemra.dd_wrt.about.AboutDialog;
import org.lemra.dd_wrt.api.conn.Router;
import org.lemra.dd_wrt.mgmt.adapters.RouterListRecycleViewAdapter;
import org.lemra.dd_wrt.mgmt.dao.DDWRTCompanionDAO;
import org.lemra.dd_wrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteDAOImpl;
import org.lemra.dd_wrt.utils.Utils;

import java.util.List;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;


public class RouterManagementActivity
        extends SherlockFragmentActivity
        implements View.OnClickListener, RouterMgmtDialogListener, ActionMode.Callback, RecyclerView.OnItemTouchListener {

    public static final String ROUTER_SELECTED = "ROUTER_SELECTED";
    private static final String LOG_TAG = RouterManagementActivity.class.getSimpleName();
    boolean addRouterDialogOpen;
    @Nullable
    ActionMode actionMode;
    GestureDetectorCompat gestureDetector;
    int itemCount;
    ImageButton addNewButton;
    private DDWRTCompanionDAO dao;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private Menu optionsMenu;

    @NotNull
    public static DDWRTCompanionDAO getDao(Context context) {
        return new DDWRTCompanionSqliteDAOImpl(context);
        //FIXME TESTS ONLY
//        return new DDWRTCompanionTestDAOImpl();
        //FIXME END TESTS

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_router_management);


        this.dao = getDao(this);

        mRecyclerView = (RecyclerView) findViewById(R.id.routersListView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // allows for optimizations if all items are of the same size:
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.scrollToPosition(0);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new RouterListRecycleViewAdapter(this, this.dao.getAllRouters());
        mRecyclerView.setAdapter(mAdapter);

        /*
         * onClickDetection is done in this Activity's onItemTouchListener
         * with the help of a GestureDetector;
         * Tip by Ian Lake on G+ in a comment to this post:
         * https://plus.google.com/+LucasRocha/posts/37U8GWtYxDE
         */
        mRecyclerView.addOnItemTouchListener(this);
        gestureDetector = new GestureDetectorCompat(this, new RouterManagementViewOnGestureListener());

        addNewButton = (ImageButton) findViewById(R.id.router_list_add);
        addNewButton.setOnClickListener(this);

    }

    @Override
    protected void onResume() {
        if (!addRouterDialogOpen && this.dao.getAllRouters().isEmpty()) {
                openAddRouterForm();
        }
        super.onResume();
    }

    private void openAddRouterForm() {
        @NotNull final DialogFragment addFragment = new RouterAddDialogFragment();
        addFragment.show(getSupportFragmentManager(), "add_router");
        addRouterDialogOpen = true;
    }

    private void openUpdateRouterForm(@Nullable Router router) {
        if (router != null) {
            @NotNull final DialogFragment updateFragment = new RouterUpdateDialogFragment();
            @NotNull final Bundle args = new Bundle();
            args.putString(ROUTER_SELECTED, router.getUuid());
            updateFragment.setArguments(args);
            updateFragment.show(getSupportFragmentManager(), "update_router");
        } else {
            Crouton.makeText(this, "Entry no longer exists!", Style.ALERT).show();
        }
    }

    private void openDuplicateRouterForm(@Nullable Router router) {
        if (router != null) {
            @NotNull final DialogFragment copyFragment = new RouterDuplicateDialogFragment();
            @NotNull final Bundle args = new Bundle();
            args.putString(ROUTER_SELECTED, router.getUuid());
            copyFragment.setArguments(args);
            copyFragment.show(getSupportFragmentManager(), "copy_router");
        } else {
            Crouton.makeText(this, "Entry no longer exists!", Style.ALERT).show();
        }
    }

    @Override
    protected void onPause() {
        addRouterDialogOpen = false;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        addRouterDialogOpen = false;
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        this.optionsMenu = menu;
        getSupportMenuInflater().inflate(R.menu.menu_router_management, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NotNull com.actionbarsherlock.view.MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.router_list_refresh:
                doRefreshRoutersListWithSpinner(RoutersListRefreshCause.DATA_SET_CHANGED, null);
                return true;
            case R.id.router_list_donate:
                Toast.makeText(this, "[TODO] RouterManagementActivity - Donate", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.router_list_about:
                new AboutDialog(this).show();
                return true;
            case R.id.router_list_settings:
                Toast.makeText(this, "[TODO] RouterManagementActivity - Settings", Toast.LENGTH_SHORT).show();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void doRefreshRoutersListWithSpinner(@NotNull final RoutersListRefreshCause cause, final Integer position) {
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
        }, 2000);
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
    public void onClick(@NotNull View view) {
        if (view.getId() == R.id.router_list_add) {
            this.openAddRouterForm();
        } else if (view.getId() == R.id.container_list_item) {
            // item click
            final int idx = mRecyclerView.getChildPosition(view);
            if (actionMode != null) {
                myToggleSelection(idx);
                return;
            }

            final AlertDialog alertDialog = Utils.buildAlertDialog(this, null, "Loading...", false, false);
            alertDialog.show();
            ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    //No action mode - normal mode => open up main activity for this router
                    final List<Router> routersList = ((RouterListRecycleViewAdapter) mAdapter).getRoutersList();
                    final Router router;
                    if (idx < 0 || idx >= routersList.size() || (router = routersList.get(idx)) == null) {
                        Crouton.makeText(RouterManagementActivity.this,
                                "Unknown router - please refresh list or add a new one.", Style.ALERT).show();
                        return;
                    }
                    final Intent ddWrtMainIntent = new Intent(RouterManagementActivity.this, DDWRTMainActivity.class);
                    ddWrtMainIntent.putExtra(ROUTER_SELECTED, router.getUuid());
                    RouterManagementActivity.this.startActivity(ddWrtMainIntent);
                    alertDialog.cancel();
                }
            }, 2000);

        }
    }

    @Override
    public void onRouterAdd(SherlockDialogFragment dialog, boolean error) {
        if (!error) {
            //Always added to the top
            doRefreshRoutersListWithSpinner(RoutersListRefreshCause.INSERTED, 0);
            mLayoutManager.scrollToPosition(0);
        }
    }

    @Override
    public void onRouterUpdated(SherlockDialogFragment dialog, int position, boolean error) {
        if (!error) {
            //Always added to the top
            doRefreshRoutersListWithSpinner(RoutersListRefreshCause.UPDATED, position);
            mLayoutManager.scrollToPosition(position);
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
    public boolean onCreateActionMode(@NotNull ActionMode actionMode, Menu menu) {
        // Inflate a menu resource providing context menu items
        MenuInflater inflater = actionMode.getMenuInflater();
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
    public boolean onActionItemClicked(@NotNull final ActionMode actionMode, @NotNull final MenuItem menuItem) {
        @NotNull final RouterListRecycleViewAdapter adapter = (RouterListRecycleViewAdapter) mAdapter;
        switch (menuItem.getItemId()) {
            case R.id.menu_router_list_delete:
                final int selectedItemCount = adapter.getSelectedItemCount();
                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.ic_action_alert_warning)
                        .setTitle(String.format("Delete %s Router(s)?", selectedItemCount))
                        .setMessage("You'll lose those records!")
                        .setCancelable(true)
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialogInterface, final int i) {
                                @NotNull final List<Integer> selectedItemPositions = adapter.getSelectedItems();
                                for (int itemPosition = selectedItemPositions.size() - 1; itemPosition >= 0; itemPosition--) {
                                    adapter.removeData(selectedItemPositions.get(itemPosition));
                                }
                                actionMode.finish();
                                Crouton.makeText(RouterManagementActivity.this, selectedItemCount + " item(s) deleted", Style.CONFIRM).show();
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
                final List<Router> routersList = ((RouterListRecycleViewAdapter) this.mAdapter).getRoutersList();
                final Integer itemPos = adapter.getSelectedItems().get(0);
                this.openUpdateRouterForm(
                        (itemPos == null || itemPos < 0 || itemPos >= routersList.size()) ? null : routersList.get(itemPos)
                );
            }
                return true;
            case R.id.menu_router_item_copy: {
                final List<Router> routersList = ((RouterListRecycleViewAdapter) this.mAdapter).getRoutersList();
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
        ((RouterListRecycleViewAdapter) mAdapter).clearSelections();
        addNewButton.setVisibility(View.VISIBLE);
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
        @NotNull final RouterListRecycleViewAdapter adapter = (RouterListRecycleViewAdapter) mAdapter;
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

    public enum RoutersListRefreshCause {
        INSERTED, REMOVED, DATA_SET_CHANGED, UPDATED
    }

    private class RouterManagementViewOnGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(@NotNull MotionEvent e) {
            View view = mRecyclerView.findChildViewUnder(e.getX(), e.getY());
            onClick(view);
            return super.onSingleTapConfirmed(e);
        }

        public void onLongPress(@NotNull MotionEvent e) {
            View view = mRecyclerView.findChildViewUnder(e.getX(), e.getY());
            if (actionMode != null) {
                return;
            }
            // Start the CAB using the ActionMode.Callback defined above
            actionMode = startActionMode(RouterManagementActivity.this);
            int idx = mRecyclerView.getChildPosition(view);
            myToggleSelection(idx);
            super.onLongPress(e);
        }
    }
}
