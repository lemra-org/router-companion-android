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

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import org.lemra.dd_wrt.R;
import org.lemra.dd_wrt.api.conn.Router;
import org.lemra.dd_wrt.mgmt.adapters.RouterListRecycleViewAdapter;
import org.lemra.dd_wrt.mgmt.dao.DDWRTCompanionDAO;
import org.lemra.dd_wrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteDAOImpl;

import java.sql.SQLException;
import java.util.List;


public class RouterManagementActivity extends SherlockFragmentActivity implements View.OnClickListener, View.OnLongClickListener, RouterAddDialogFragment.RouterAddDialogListener {

    public static final String ROUTER_SELECTED = "ROUTER_SELECTED";
    private static final String LOG_TAG = RouterManagementActivity.class.getSimpleName();
    boolean addRouterDialogOpen;
    private DDWRTCompanionDAO dao;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private Menu optionsMenu;

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
        try {
            this.dao.open();
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

        mRecyclerView = (RecyclerView) findViewById(R.id.routersListView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new RouterListRecycleViewAdapter(this, this.dao.getAllRouters());
        mRecyclerView.setAdapter(mAdapter);

        final ImageButton addNewButton = (ImageButton) findViewById(R.id.router_list_add);
        addNewButton.setOnClickListener(this);
        addNewButton.setOnLongClickListener(this);

    }

    @Override
    protected void onResume() {
        try {
            this.dao.open();
            if (!addRouterDialogOpen && this.dao.getAllRouters().isEmpty()) {
                openAddRouterForm();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        super.onResume();
    }

    private void openAddRouterForm() {
        final DialogFragment addFragment = new RouterAddDialogFragment();
        addFragment.show(getSupportFragmentManager(), "add_router");
        addRouterDialogOpen = true;
    }

    @Override
    protected void onPause() {
        this.dao.close();
        addRouterDialogOpen = false;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        this.dao.close();
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
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.router_list_refresh) {
            doRefreshRoutersListWithSpinner(RoutersListRefreshCause.DATA_SET_CHANGED, null);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void doRefreshRoutersListWithSpinner(final RoutersListRefreshCause cause, final Integer position) {
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
                } else {
                    refreshItem.setActionView(null);
                }
            }
        }
    }

    @Override
    public void onClick(View view) {
        this.openAddRouterForm();
    }

    @Override
    public boolean onLongClick(View view) {
        this.onClick(view);
        return true;
    }

    @Override
    public void onRouterAdd(SherlockDialogFragment dialog, boolean error) {
        if (!error) {
            //Always added to the top
            doRefreshRoutersListWithSpinner(RoutersListRefreshCause.INSERTED, 0);
        }
    }

    public enum RoutersListRefreshCause {
        INSERTED, REMOVED, DATA_SET_CHANGED
    }
}
