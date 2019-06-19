package org.rm3l.router_companion

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import org.rm3l.ddwrt.BuildConfig
import org.rm3l.router_companion.RouterCompanionAppConstants.ADMOB_INTERSTITIAL_SPLASH_ACTIVITY_AD_UNIT_ID
import org.rm3l.router_companion.RouterCompanionAppConstants.ADMOB_INTERSTITIAL_AD_UNIT_ID_DEBUG
import org.rm3l.router_companion.mgmt.RouterManagementActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var mInterstitialAd: InterstitialAd

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("ConstantConditionIf")
        if (BuildConfig.WITH_ADS) {
            MobileAds.initialize(this, RouterCompanionAppConstants.ADMOB_APP_ID)

            mInterstitialAd = InterstitialAd(this)
            mInterstitialAd.adUnitId =
                if (BuildConfig.DEBUG) ADMOB_INTERSTITIAL_AD_UNIT_ID_DEBUG
                else ADMOB_INTERSTITIAL_SPLASH_ACTIVITY_AD_UNIT_ID

            mInterstitialAd.loadAd(AdRequest.Builder().build())
        }

        val intent = Intent(this, RouterManagementActivity::class.java)
        startActivity(intent)
        finish()
    }
}
