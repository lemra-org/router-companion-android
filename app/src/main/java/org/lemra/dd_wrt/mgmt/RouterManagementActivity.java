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

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.lemra.dd_wrt.DDWRTMainActivity;
import org.lemra.dd_wrt.R;
import org.lemra.dd_wrt.api.conn.Router;

import java.util.ArrayList;
import java.util.List;

import static android.widget.AdapterView.OnItemClickListener;
import static android.widget.AdapterView.OnItemLongClickListener;
import static org.lemra.dd_wrt.api.conn.Router.RouterConnectionProtocol.HTTPS;
import static org.lemra.dd_wrt.api.conn.Router.RouterConnectionProtocol.SSH;

public class RouterManagementActivity extends SherlockActivity implements OnItemLongClickListener, OnItemClickListener {

    public static final String ROUTER_SELECTED = "ROUTER_SELECTED";
    private static final String LOG_TAG = RouterManagementActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_router_management);

        final ArrayList<Router> routersRegistered = getRoutersRegistered();

        final ListView lv = (ListView) findViewById(R.id.routersListView);
        lv.setAdapter(new RouterListBaseAdapter(this, routersRegistered));

        lv.setOnItemClickListener(this);
        lv.setOnItemLongClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        getSupportMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        //TODO
        Object o = adapterView.getItemAtPosition(position);
        Router fullObject = (Router) o;

        final Intent ddWrtMainIntent = new Intent(this, DDWRTMainActivity.class);
        ddWrtMainIntent.putExtra(ROUTER_SELECTED, new Gson().toJson(fullObject));

        startActivity(ddWrtMainIntent);

//        Toast.makeText(getApplicationContext(),
//                "onItemClick: " + " " + fullObject.getName() + "(" + fullObject.getRemoteIpAddress() + ")",
//                Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long l) {
        //TODO Show popup menu: Edit, Delete
        Object o = adapterView.getItemAtPosition(position);
        Router fullObject = (Router) o;
        Toast.makeText(getApplicationContext(),
                "onItemLongClick: " + " " + fullObject.getName() + "(" + fullObject.getRemoteIpAddress() + ")",
                Toast.LENGTH_LONG).show();
        return true;
    }

    @NotNull
    private ArrayList<Router> getRoutersRegistered() {
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

}
