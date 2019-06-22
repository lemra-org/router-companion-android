package org.rm3l.router_companion

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.crashlytics.android.Crashlytics
import com.mrgames13.jimdo.splashscreen.App.SplashScreenBuilder
import org.rm3l.ddwrt.R
import org.rm3l.router_companion.mgmt.RouterManagementActivity

class SplashActivity : AppCompatActivity() {

    companion object {
        private val TAG = SplashActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SplashScreenBuilder.getInstance(this)
            .setVideo(R.raw.splash_animation)
            .setImage(R.drawable.logo_ddwrt_companion)
            .skipImage(true)
            .setTitle("DD-WRT Companion")
            .setSubtitle("Router management made easy")
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SplashScreenBuilder.SPLASH_SCREEN_FINISHED) {
            if (resultCode == RESULT_OK) {
                Crashlytics.log(Log.DEBUG, TAG,
                    "SPLASH_SCREEN_FINISHED: OK => SplashScreen finished without manual canceling")
            } else if (resultCode == RESULT_CANCELED) {
                Crashlytics.log(Log.DEBUG, TAG,
                    "SPLASH_SCREEN_FINISHED: RESULT_CANCELED => SplashScreen finished through manual canceling")
            }
        }

        startActivity(Intent(this, RouterManagementActivity::class.java))
        finish()
    }
}
