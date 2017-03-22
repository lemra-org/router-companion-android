package org.rm3l.router_companion.utils;

import android.content.Context;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.exceptions.DDWRTCompanionException;

import static org.rm3l.router_companion.RouterCompanionAppConstants.DELAY_BETWEEN_TWO_CONSECUTIVE_INTERSTITIAL_ADS_MINUTES;

public final class AdUtils {

  public static final String TAG = AdUtils.class.getSimpleName();

  private AdUtils() {
  }

  @Nullable public static String getDeviceIdForAdMob(@Nullable final Context context) {
    if (context == null) {
      return null;
    }

    final String aid = Settings.Secure.getString(context.getContentResolver(), "android_id");
    Object obj;
    try {
      ((MessageDigest) (obj = MessageDigest.getInstance("MD5"))).update(aid.getBytes(), 0,
          aid.length());
      obj = String.format("%032X", new BigInteger(1, ((MessageDigest) obj).digest()));
    } catch (Exception e) {
      obj = aid.substring(0, 32);
      Utils.reportException(null, e);
    }

    Crashlytics.log(Log.DEBUG, TAG, "deviceIdForAdMob: [" + obj + "]");

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

  @Nullable public static AdRequest buildAdRequest(@Nullable final Context ctx) {
    if (ctx == null) {
      return null;
    }
    final AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
    if (BuildConfig.DEBUG) {
      //Get Device ID Programmatically
      adRequestBuilder.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
          .addTestDevice(getDeviceIdForAdMob(ctx));
    }
    return adRequestBuilder.build();
  }

  @Nullable
  public static InterstitialAd requestNewInterstitial(@Nullable final Context ctx, int unitId) {
    return (ctx != null ? requestNewInterstitial(ctx, ctx.getResources().getString(unitId)) : null);
  }

  @Nullable public static InterstitialAd requestNewInterstitial(@Nullable final Context ctx,
      @NonNull final String unitId) {
    //noinspection PointlessBooleanExpression
    if (ctx == null || !BuildConfig.WITH_ADS || !BuildConfig.WITH_INTERSTITIAL_ADS) {
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

  public static boolean canDisplayInterstialAd(@Nullable final Context ctx) {
    if ((!BuildConfig.WITH_ADS) || ctx == null) {
      return false;
    }
    long lastInterstitialAd =
        ctx.getSharedPreferences(RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY,
            Context.MODE_PRIVATE)
            .getLong(RouterCompanionAppConstants.AD_LAST_INTERSTITIAL_PREF, -1l);
    return lastInterstitialAd < 0l || (TimeUnit.MINUTES.convert(
        System.currentTimeMillis() - lastInterstitialAd, TimeUnit.MILLISECONDS)
        >= DELAY_BETWEEN_TWO_CONSECUTIVE_INTERSTITIAL_ADS_MINUTES);
  }

  public static class AdEvent extends DDWRTCompanionException {

    public AdEvent(@Nullable String detailMessage) {
      super(detailMessage);
    }
  }

  public static class AdClickEvent extends AdEvent {

    public AdClickEvent(@Nullable String detailMessage) {
      super(detailMessage);
    }
  }

  public static class AdFailedToShowEvent extends AdEvent {

    public AdFailedToShowEvent(@Nullable String detailMessage) {
      super(detailMessage);
    }
  }

  public static class AdHiddenEvent extends AdEvent {

    public AdHiddenEvent(@Nullable String detailMessage) {
      super(detailMessage);
    }
  }
}
