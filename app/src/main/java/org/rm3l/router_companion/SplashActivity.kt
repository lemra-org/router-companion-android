package org.rm3l.router_companion

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.MobileAds
import org.rm3l.ddwrt.BuildConfig
import org.rm3l.router_companion.mgmt.RouterManagementActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("ConstantConditionIf")
        if (BuildConfig.WITH_ADS) {
            MobileAds.initialize(this, RouterCompanionAppConstants.ADMOB_APP_ID)
        }

        val intent = Intent(this, RouterManagementActivity::class.java)
        startActivity(intent)
        finish()
    }
}
