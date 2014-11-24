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

package org.rm3l.ddwrt.donate;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.sufficientlysecure.donations.DonationsFragment;

public class DonateActivity extends SherlockFragmentActivity {

    /**
     * Google
     */
    private static final String GOOGLE_PUBKEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmOd0e3n5XgmOCL19uESJwDbWKKYE/8g4lZfQfoh6e604vHKUVu6wUPAe3ijIAZYa3KTsU3W3D24++e1HH5fyjYJ+J/2YR0PYmab5TuFKvMaK6X8GKyRlQhjfCBq19LIZ9gRsmjTLe8zDvoUcJuwzsKZ3J6d2z0y64vpvc+ZFNTpnYlmFyX1H2BQDyc2LR0l1IW7AhExlu4T5S6f1J7nDDvCjAQ0lVz5qakU/M+GDPffg8b+AnoB0+TTQJvOevNm10tiVhJmdaOw8gWeoPDFbdKDm+w7nBWOvOPtFwq2isj0urFl8kuP2rJqrwOIsUoo02r5I2KbuOXsbnTfobH6zRQIDAQAB";
    private static final String[] GOOGLE_CATALOG = new String[]{
            "ntpsync.donation.1", "ntpsync.donation.2", "ntpsync.donation.3", "ntpsync.donation.5", "ntpsync.donation.8",
            "ntpsync.donation.13", "ntpsync.donation.21", "ntpsync.donation.34", "ntpsync.donation.55", "ntpsync.donation.89"};

    /**
     * PayPal
     */
    private static final String PAYPAL_USER = "armel.soro@gmail.com";
    private static final String PAYPAL_CURRENCY_CODE = "EUR";

    /**
     * FIXME Flattr
     */
    private static final String FLATTR_PROJECT_URL = "https://github.com/rm3l/ddwrt-companion";
    // FLATTR_URL without http:// !
    private static final String FLATTR_URL = "flattr.com/thing/712895/dschuermannandroid-donations-lib-on-GitHub";


    /**
     * Bitcoin
     */
    private static final String BITCOIN_ADDRESS = "3NuYX1cWymrCNdMEF11fzyj2dcYvH4zniR";

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.donations_activity);

        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        final DonationsFragment donationsFragment;
        if (BuildConfig.DONATIONS_GOOGLE) {
            donationsFragment = DonationsFragment.newInstance(BuildConfig.DEBUG, true, GOOGLE_PUBKEY, GOOGLE_CATALOG,
                    getResources().getStringArray(R.array.donation_google_catalog_values), false, null, null,
                    null, false, null, null, false, null);
        } else {
            donationsFragment = DonationsFragment.newInstance(BuildConfig.DEBUG, false, null, null, null, true, PAYPAL_USER,
                    PAYPAL_CURRENCY_CODE, getString(R.string.donation_paypal_item), false, FLATTR_PROJECT_URL, FLATTR_URL, true, BITCOIN_ADDRESS);
        }

        ft.replace(R.id.donations_activity_container, donationsFragment, "donationsFragment");
        ft.commit();
    }

    /**
     * Needed for Google Play In-app Billing. It uses startIntentSenderForResult(). The result is not propagated to
     * the Fragment like in startActivityForResult(). Thus we need to propagate manually to our Fragment.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        final FragmentManager fragmentManager = getSupportFragmentManager();
        final Fragment fragment = fragmentManager.findFragmentByTag("donationsFragment");
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

}
