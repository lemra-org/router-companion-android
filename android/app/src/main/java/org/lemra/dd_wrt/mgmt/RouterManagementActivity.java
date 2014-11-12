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

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import org.jetbrains.annotations.NotNull;
import org.lemra.dd_wrt.R;
import org.lemra.dd_wrt.api.conn.Router;
import org.lemra.dd_wrt.mgmt.adapters.RouterListRecycleViewAdapter;
import org.lemra.dd_wrt.mgmt.dao.DDWRTCompanionDAO;
import org.lemra.dd_wrt.mgmt.dao.impl.sqlite.DDWRTCompanionSqliteDAOImpl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.lemra.dd_wrt.api.conn.Router.RouterConnectionProtocol.HTTPS;
import static org.lemra.dd_wrt.api.conn.Router.RouterConnectionProtocol.SSH;


public class RouterManagementActivity extends SherlockFragmentActivity implements View.OnClickListener, View.OnLongClickListener {

    public static final String ROUTER_SELECTED = "ROUTER_SELECTED";
    private static final String LOG_TAG = RouterManagementActivity.class.getSimpleName();

    private DDWRTCompanionDAO dao;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_router_management);

        //SQLite
        this.dao = new DDWRTCompanionSqliteDAOImpl(this);
        try {
            this.dao.open();
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

        final List<Router> routersRegistered = getRoutersRegistered();

        mRecyclerView = (RecyclerView) findViewById(R.id.routersListView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new RouterListRecycleViewAdapter(this, routersRegistered);
        mRecyclerView.setAdapter(mAdapter);

        final ImageButton addNewButton = (ImageButton) findViewById(R.id.router_list_add);
        addNewButton.setOnClickListener(this);
        addNewButton.setOnLongClickListener(this);

    }

    @Override
    protected void onResume() {
        try {
            this.dao.open();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        this.dao.close();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        this.dao.close();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
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
            //TODO Refresh list
            Toast.makeText(getApplicationContext(),
                    "[FIXME] : refresh list",
                    Toast.LENGTH_LONG).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @NotNull
    private List<Router> getRoutersRegistered() {
//        return this.dao.getAllRouters();

        ArrayList<Router> results = new ArrayList<Router>();

        //FIXME TESTS ONLY
        //TODO Get items from SharedPreferences. If none registered, load activity for adding a new one
        final List<Integer> primeNumbersFromEratostheneSieve = getPrimeNumbersFromEratostheneSieve(33);

        for (int i = 1; i <= 33; i++) {
            final Router sr = new Router();
            sr.setName("router #" + i);
            sr.setRemoteIpAddress("172.17.17." + i);
            sr.setRouterConnectionProtocol(primeNumbersFromEratostheneSieve.contains(i) ? SSH : HTTPS);
            results.add(sr);
        }
        //FIXME TESTS

        return results;
    }

    @NotNull
    private List<Integer> getPrimeNumbersFromEratostheneSieve(final int up) {
        final List<Integer> excluded = new ArrayList<Integer>();
        for (int i = 2; i <= up; i++) {
            if (excluded.contains(i)) {
                continue;
            }
            for (int j = i + 1; j <= up; j++) {
                if (j % i == 0) {
                    excluded.add(j);
                }
            }
        }

        final List<Integer> primes = new ArrayList<Integer>();
        for (int l = 1; l <= up; l++) {
            if (excluded.contains(l)) {
                continue;
            }
            primes.add(l);
        }

        Log.d(LOG_TAG, "primes: " + primes);

        return primes;
    }

    @Override
    public void onClick(View view) {
        final DialogFragment addFragment = new RouterAddDialogFragment();
        addFragment.show(getSupportFragmentManager(), "add_router");
    }

    @Override
    public boolean onLongClick(View view) {
        this.onClick(view);
        return true;
    }
}
