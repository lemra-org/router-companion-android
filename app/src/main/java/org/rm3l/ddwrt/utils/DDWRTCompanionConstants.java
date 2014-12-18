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

package org.rm3l.ddwrt.utils;

/**
 * App Constants
 */
public final class DDWRTCompanionConstants {

    //FIXME Consider increasing this value prior to release
    public static final long TILE_REFRESH_MILLIS = 20 * 1000l;

    //This is only used to check feedback submitted by end-users
    public static final String PUBKEY = \"fake-key\";
            "AY5ab5Nbu" +
            "6fMj7xRnc" +
            "dGgoNSvYM" +
            "BT6B42r2p" +
            "bp/mABgAz" +
            "8" +
            "I";

    //FIXME Update prior to release
    public static final boolean TEST_MODE = false;
    public static final long MAX_PRIVKEY_SIZE_BYTES = 300 * 1024l;
    public static final String SYNC_INTERVAL_MILLIS_PREF = "syncIntervalMillis";
    public static final String SORTING_STRATEGY_PREF = "sortingStrategy";
    public static final String EMPTY_STRING = "";

    private DDWRTCompanionConstants() {
    }
}
