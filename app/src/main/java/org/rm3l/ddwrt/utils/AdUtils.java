package org.rm3l.ddwrt.utils;

import android.content.Context;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.rm3l.ddwrt.BuildConfig;

import java.math.BigInteger;
import java.security.MessageDigest;

public final class AdUtils {

    public static final String TAG = AdUtils.class.getSimpleName();

    private AdUtils() {
    }

    @Nullable
    public static String getDeviceIdForAdMob(@Nullable final Context context) {
        if (context == null) {
            return null;
        }

        final String aid = Settings.Secure.getString(context.getContentResolver(), "android_id");
        Object obj;
        try {
            ((MessageDigest) (obj = MessageDigest.getInstance("MD5"))).update(
                    aid.getBytes(), 0, aid.length());
            obj = String.format("%032X", new BigInteger(1, ((MessageDigest) obj).digest()));
        } catch (Exception e) {
            obj = aid.substring(0, 32);
            Utils.reportException(e);
        }

        Log.d(TAG, "deviceIdForAdMob: [" + obj + "]");

        return (obj != null ? obj.toString() : null);
    }

    public static void buildAndDisplayAdViewIfNeeded(@Nullable final Context ctx,
                                                     @Nullable final AdView adView) {
        if (ctx == null || adView == null) {
            return;
        }
        if (BuildConfig.WITH_ADS) {
            adView.setVisibility(View.VISIBLE);
            final AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
            if (BuildConfig.DEBUG) {
                //Get Device ID Programmatically
                adRequestBuilder
                        .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                        .addTestDevice(getDeviceIdForAdMob(ctx));
            }

            adView.loadAd(adRequestBuilder.build());
        } else {
            adView.setVisibility(View.GONE);
        }
    }
}
