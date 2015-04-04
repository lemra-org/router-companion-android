package org.rm3l.ddwrt.utils;

import android.content.Context;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

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
            final AdRequest adRequest = buildAdRequest(ctx);
            if (adRequest == null) {
                adView.setVisibility(View.GONE);
            } else {
                adView.setVisibility(View.VISIBLE);
                adView.loadAd(adRequest);
            }
        } else {
            adView.setVisibility(View.GONE);
        }
    }

    @Nullable
    public static AdRequest buildAdRequest(@Nullable final Context ctx) {
        if (ctx == null) {
            return null;
        }
        final AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
        if (BuildConfig.DEBUG) {
            //Get Device ID Programmatically
            adRequestBuilder
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .addTestDevice(getDeviceIdForAdMob(ctx));
        }
        return adRequestBuilder.build();
    }

    @Nullable
    public static InterstitialAd requestNewInterstitial(@Nullable final Context ctx, int unitId) {
        return (ctx != null ? requestNewInterstitial(ctx, ctx.getResources().getString(unitId)) : null);
    }

    @Nullable
    public static InterstitialAd requestNewInterstitial(@Nullable final Context ctx, @NonNull final String unitId) {
        //noinspection PointlessBooleanExpression
        if (ctx == null || !BuildConfig.WITH_ADS) {
            return null;
        }
        final AdRequest adRequest = buildAdRequest(ctx);
        if (adRequest == null) {
            return null;
        }

        final InterstitialAd interstitialAd = new InterstitialAd(ctx);
        interstitialAd.setAdUnitId(unitId);
        interstitialAd.loadAd(adRequest);

        return interstitialAd;
    }
}
