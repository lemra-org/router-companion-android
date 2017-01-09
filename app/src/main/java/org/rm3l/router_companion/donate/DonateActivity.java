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

package org.rm3l.router_companion.donate;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;

import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.utils.ColorUtils;
//import org.sufficientlysecure.donations.DonationsFragment;


/**
 * Donation Activity: leverages the <a href="https://github.com/dschuermann/android-donations-lib" target="_blank">android-donations-lib</a> library.
 *
 * @author <a href="mailto:apps+ddwrt@rm3l.org">Armel S.</a>
 */
public class DonateActivity extends FragmentActivity {

    /**
     * Google
     */
    private static final String GOOGLE_PUBKEY = \"fake-key\";
    private static final String[] GOOGLE_CATALOG = new String[]{
            "ntpsync.donation.1", "ntpsync.donation.2", "ntpsync.donation.3", "ntpsync.donation.5", "ntpsync.donation.8",
            "ntpsync.donation.13", "ntpsync.donation.21", "ntpsync.donation.34", "ntpsync.donation.55", "ntpsync.donation.89"};

    /**
     * PayPal
     */
    private static final String PAYPAL_USER = "armel.soro@gmail.com";
    private static final String PAYPAL_CURRENCY_CODE = "EUR";

    /**
     * Flattr
     */
    private static final String FLATTR_PROJECT_URL = "https://github.com/rm3l/ddwrt-companion";
    // FLATTR_URL without http:// !
    private static final String FLATTR_URL = "flattr.com/thing/320749440571624a971910e970a638d7";


    /**
     * Bitcoin
     */
    private static final String BITCOIN_ADDRESS = "3NuYX1cWymrCNdMEF11fzyj2dcYvH4zniR";

    private static final String DONATIONS_FRAGMENT = "donationsFragment";

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ColorUtils.isThemeLight(this)) {
            //Light
            setTheme(R.style.AppThemeLight);
//            getWindow().getDecorView()
//                    .setBackgroundColor(ContextCompat.getColor(this,
//                            android.R.color.white));
        } else {
            //Default is Dark
            setTheme(R.style.AppThemeDark);
        }

        setContentView(R.layout.donations_activity);

        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//        final DonationsFragment donationsFragment;

        if (BuildConfig.DONATIONS_GOOGLE) {
            //Activate Google Play In-App Billing solely
//            donationsFragment = DonationsFragment.newInstance(BuildConfig.DEBUG, true, GOOGLE_PUBKEY, GOOGLE_CATALOG,
//                    getResources().getStringArray(R.array.donation_google_catalog_values), false, null, null,
//                    null, false, null, null, false, null);
        } else {
//            donationsFragment = DonationsFragment.newInstance(BuildConfig.DEBUG, false, null, null, null, true, PAYPAL_USER,
//                    PAYPAL_CURRENCY_CODE, getString(R.string.donation_paypal_item), true, FLATTR_PROJECT_URL, FLATTR_URL, true, BITCOIN_ADDRESS);
        }

//        ft.replace(R.id.donations_activity_container, donationsFragment, DONATIONS_FRAGMENT);
        ft.commit();
    }

    /**
     * Needed for Google Play In-app Billing. It uses startIntentSenderForResult(). The result is not propagated to
     * the Fragment like in startActivityForResult(). Thus we need to propagate manually to our Fragment.
     *
     * @param requestCode the request code
     * @param resultCode  the result code
     * @param data        the data we got in return
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        final FragmentManager fragmentManager = getSupportFragmentManager();
        final Fragment fragment = fragmentManager.findFragmentByTag(DONATIONS_FRAGMENT);
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
