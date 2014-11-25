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

import org.sufficientlysecure.donations.DonationsFragment;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * Extension of the {@link DonationsFragment} fragment, with Crouton notifications upon a successful update
 */
public class DonateFragment extends DonationsFragment {

    /**
     * Instantiate DonateFragment.
     *
     * @param debug               You can use BuildConfig.DEBUG to propagate the debug flag from your app to the Donations library
     * @param googleEnabled       Enabled Google Play donations
     * @param googlePubkey        Your Google Play public key
     * @param googleCatalog       Possible item names that can be purchased from Google Play
     * @param googleCatalogValues Values for the names
     * @param paypalEnabled       Enable PayPal donations
     * @param paypalUser          Your PayPal email address
     * @param paypalCurrencyCode  Currency code like EUR. See here for other codes:
     *                            https://developer.paypal.com/webapps/developer/docs/classic/api/currency_codes/#id09A6G0U0GYK
     * @param paypalItemName      Display item name on PayPal, like "Donation for NTPSync"
     * @param flattrEnabled       Enable Flattr donations
     * @param flattrProjectUrl    The project URL used on Flattr
     * @param flattrUrl           The Flattr URL to your thing. NOTE: Enter without http://
     * @param bitcoinEnabled      Enable bitcoin donations
     * @param bitcoinAddress      The address to receive bitcoin
     * @return DonateFragment
     */
    public static DonateFragment newInstance(boolean debug, boolean googleEnabled, String googlePubkey, String[] googleCatalog,
                                             String[] googleCatalogValues, boolean paypalEnabled, String paypalUser,
                                             String paypalCurrencyCode, String paypalItemName, boolean flattrEnabled,
                                             String flattrProjectUrl, String flattrUrl, boolean bitcoinEnabled, String bitcoinAddress) {

        final DonateFragment donationsFragment = new DonateFragment();

        final Bundle args = new Bundle();

        args.putBoolean(ARG_DEBUG, debug);

        args.putBoolean(ARG_GOOGLE_ENABLED, googleEnabled);
        args.putString(ARG_GOOGLE_PUBKEY, googlePubkey);
        args.putStringArray(ARG_GOOGLE_CATALOG, googleCatalog);
        args.putStringArray(ARG_GOOGLE_CATALOG_VALUES, googleCatalogValues);

        args.putBoolean(ARG_PAYPAL_ENABLED, paypalEnabled);
        args.putString(ARG_PAYPAL_USER, paypalUser);
        args.putString(ARG_PAYPAL_CURRENCY_CODE, paypalCurrencyCode);
        args.putString(ARG_PAYPAL_ITEM_NAME, paypalItemName);

        args.putBoolean(ARG_FLATTR_ENABLED, flattrEnabled);
        args.putString(ARG_FLATTR_PROJECT_URL, flattrProjectUrl);
        args.putString(ARG_FLATTR_URL, flattrUrl);

        args.putBoolean(ARG_BITCOIN_ENABLED, bitcoinEnabled);
        args.putString(ARG_BITCOIN_ADDRESS, bitcoinAddress);

        donationsFragment.setArguments(args);

        return donationsFragment;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Crouton.makeText(this.getActivity(), "Thank you for your support!", Style.CONFIRM).show();
        super.onActivityResult(requestCode, resultCode, data);
    }
}
