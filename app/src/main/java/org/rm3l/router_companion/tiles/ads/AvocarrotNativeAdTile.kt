package org.rm3l.router_companion.tiles.ads

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.AsyncTaskLoader
import android.support.v4.content.Loader
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.avocarrot.sdk.AdError
import com.avocarrot.sdk.AdLayout
import com.avocarrot.sdk.Avocarrot
import com.avocarrot.sdk.NativeAssets
import com.avocarrot.sdk.NativeAssetsAd
import com.avocarrot.sdk.NativeAssetsAdCallback
import com.avocarrot.sdk.logger.Level.DEBUG
import com.avocarrot.sdk.logger.Level.WARN
import com.google.android.gms.ads.AdView
import com.squareup.picasso.Callback
import org.jetbrains.anko.find
import org.rm3l.ddwrt.BuildConfig
import org.rm3l.ddwrt.R
import org.rm3l.router_companion.RouterCompanionAppConstants
import org.rm3l.router_companion.resources.conn.Router
import org.rm3l.router_companion.tiles.DDWRTTile
import org.rm3l.router_companion.utils.AdUtils
import org.rm3l.router_companion.utils.ReportingUtils
import org.rm3l.router_companion.utils.Utils
import org.rm3l.router_companion.utils.kotlin.hide
import org.rm3l.router_companion.utils.kotlin.invisible
import org.rm3l.router_companion.utils.kotlin.show

class AvocarrotNativeAdTile(parentFragment: Fragment, arguments: Bundle, router: Router?)
  : DDWRTTile<Unit>(parentFragment, arguments, router, R.layout.tile_native_ad, null) {

  override fun getTileHeaderViewId(): Int {
    return R.id.tile_native_ad_hdr
  }

  override fun getTileTitleViewId(): Int {
    return R.id.tile_native_ad_headline
  }

  override fun getLoader(id: Int, args: Bundle): Loader<Unit>? {
    return object : AsyncTaskLoader<Unit>(mParentFragmentActivity) {
      override fun loadInBackground() {}
    }
  }

  override fun getLogTag(): String? {
    return LOG_TAG
  }

  override fun getOnclickIntent(): DDWRTTile<Unit>.OnClickIntent? {
    return null
  }

  override fun isAdTile(): Boolean {
    return true
  }

  override fun onLoadFinished(loader: Loader<Unit>, data: Unit) {
    Avocarrot.build(mParentFragmentActivity,
        NativeAssetsAd.Configuration.Builder()
            .setApiKey(RouterCompanionAppConstants.AVOCARROT_APIKEY)
            .setAdUnitId(RouterCompanionAppConstants.AVOCARROT_LIST_PLACEMENT_KEY)
            .setSandbox(BuildConfig.DEBUG)
            .setLogLevel(if (BuildConfig.DEBUG) DEBUG else WARN)
            .setCallback(object : NativeAssetsAdCallback() {
              override fun onAdError(error: AdError?) {
                try {
                  super.onAdError(error)
                  ReportingUtils.reportException(null,
                      AdUtils.AdFailedToShowEvent("Avocarrot: " + (error?.toString() ?: "")))
                  //Fallback to AdMob Banner Tile
                  val adView = layout.find<AdView>(R.id.admob_banner)
                  adView.visibility = View.VISIBLE
                  layout.find<View>(R.id.tile_native_ad_container).hide()
                  AdUtils.buildAndDisplayAdViewIfNeeded(mParentFragmentActivity, adView)
                } finally {
                  doneWithLoaderInstance(this@AvocarrotNativeAdTile, loader)
                }
              }

              override fun onAdLoaded(nativeAssetsAd: NativeAssetsAd?, ads: MutableList<NativeAssets>?) {
                super.onAdLoaded(nativeAssetsAd, ads)
                if (ads == null || ads.isEmpty()) {
                  this.onAdError(AdError.GENERIC)
                  return
                }

                try {
                  val avocarrotContainerLayout = layout.find<View>(R.id.tile_native_ad_container)
                  avocarrotContainerLayout.show()
                  layout.find<View>(R.id.admob_banner).hide()

                  val ad = ads[0]

                  /* Get References to the UI Components that will draw the Native Ad */
                  val title = layout.find<TextView>(R.id.tile_native_ad_headline)
                  val description = layout.find<TextView>(R.id.tile_native_ad_description)
                  val imageIconView = layout.find<ImageView>(R.id.tile_native_ad_image_view)
                  val ratingImageView = layout.find<ImageView>(R.id.tile_native_ad_rating_image_view)
                  val button = layout.find<Button>(R.id.tile_native_ad_button)

                  title.text = ad.title
                  button.text = ad.callToAction
                  description.text = ad.text
                  Utils.downloadImageFromUrl(mParentFragmentActivity, ad.ratingImageUrl,
                      ratingImageView, null, null,
                      object : Callback {
                        override fun onSuccess() {}

                        override fun onError() {
                          mParentFragmentActivity.runOnUiThread {
                            ratingImageView.invisible()
                          }
                        }
                      })

                  val adLayout =
                   AdLayout.BuilderWithView(avocarrotContainerLayout)
                      .setTitle(title)
                      .setText(description)
                      .setCallToAction(button)
                      .setIcon(imageIconView)
//                      .setMediaContainer(mediaContainer)
//                      .setAdChoices(adChoices)
                      .addClickableView(title)
                      .addClickableView(button)
                      .build()
                nativeAssetsAd?.bindView(adLayout, ad)
                } finally {
                  doneWithLoaderInstance(this@AvocarrotNativeAdTile, loader)
                }
              }
            }))
        .loadAd()
  }

  companion object {
    private val LOG_TAG = AvocarrotNativeAdTile::class.java.simpleName
  }
}
