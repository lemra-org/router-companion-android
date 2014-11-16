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

package org.sufficientlysecure.donations.google.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a block of information about in-app items.
 * An Inventory is returned by such methods as {@link IabHelper#queryInventory}.
 */
public class Inventory {
    Map<String, SkuDetails> mSkuMap = new HashMap<String, SkuDetails>();
    Map<String, Purchase> mPurchaseMap = new HashMap<String, Purchase>();

    Inventory() {
    }

    /**
     * Returns the listing details for an in-app product.
     */
    public SkuDetails getSkuDetails(String sku) {
        return mSkuMap.get(sku);
    }

    /**
     * Returns purchase information for a given product, or null if there is no purchase.
     */
    public Purchase getPurchase(String sku) {
        return mPurchaseMap.get(sku);
    }

    /**
     * Returns whether or not there exists a purchase of the given product.
     */
    public boolean hasPurchase(String sku) {
        return mPurchaseMap.containsKey(sku);
    }

    /**
     * Return whether or not details about the given product are available.
     */
    public boolean hasDetails(String sku) {
        return mSkuMap.containsKey(sku);
    }

    /**
     * Erase a purchase (locally) from the inventory, given its product ID. This just
     * modifies the Inventory object locally and has no effect on the server! This is
     * useful when you have an existing Inventory object which you know to be up to date,
     * and you have just consumed an item successfully, which means that erasing its
     * purchase data from the Inventory you already have is quicker than querying for
     * a new Inventory.
     */
    public void erasePurchase(String sku) {
        if (mPurchaseMap.containsKey(sku)) mPurchaseMap.remove(sku);
    }

    /**
     * Returns a list of all owned product IDs.
     */
    List<String> getAllOwnedSkus() {
        return new ArrayList<String>(mPurchaseMap.keySet());
    }

    /**
     * Returns a list of all owned product IDs of a given type
     */
    List<String> getAllOwnedSkus(String itemType) {
        List<String> result = new ArrayList<String>();
        for (Purchase p : mPurchaseMap.values()) {
            if (p.getItemType().equals(itemType)) result.add(p.getSku());
        }
        return result;
    }

    /**
     * Returns a list of all purchases.
     */
    List<Purchase> getAllPurchases() {
        return new ArrayList<Purchase>(mPurchaseMap.values());
    }

    void addSkuDetails(SkuDetails d) {
        mSkuMap.put(d.getSku(), d);
    }

    void addPurchase(Purchase p) {
        mPurchaseMap.put(p.getSku(), p);
    }
}
