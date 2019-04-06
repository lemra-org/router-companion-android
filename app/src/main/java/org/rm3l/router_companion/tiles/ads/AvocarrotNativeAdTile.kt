package org.rm3l.router_companion.tiles.ads

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.AsyncTaskLoader
import android.support.v4.content.Loader
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
//import com.avocarrot.sdk.mediation.ResponseStatus
//import com.avocarrot.sdk.nativeassets.NativeAssetsAd
//import com.avocarrot.sdk.nativeassets.NativeAssetsAdPool
//import com.avocarrot.sdk.nativeassets.NativeAssetsConfig
//import com.avocarrot.sdk.nativeassets.listeners.NativeAssetsAdCallback
//import com.avocarrot.sdk.nativeassets.model.NativeAssets
import com.crashlytics.android.Crashlytics
import org.jetbrains.anko.find
import org.rm3l.ddwrt.R
import org.rm3l.router_companion.RouterCompanionAppConstants
import org.rm3l.router_companion.resources.conn.Router
import org.rm3l.router_companion.tiles.DDWRTTile
import org.rm3l.router_companion.utils.kotlin.hide
import org.rm3l.router_companion.utils.kotlin.show

class AvocarrotNativeAdTile(parentFragment: Fragment, arguments: Bundle, router: Router?)
    :
//    NativeAssetsAdCallback,
    DDWRTTile<Unit>(parentFragment, arguments, router, R.layout.tile_native_ad, null) {

    override fun getTileHeaderViewId() = R.id.tile_native_ad_hdr
    override fun getTileTitleViewId() = R.id.tile_native_ad_headline
    override fun getLoader(id: Int, args: Bundle): AsyncTaskLoader<Unit> {
        return object : AsyncTaskLoader<Unit>(mParentFragmentActivity as Context) {
            override fun loadInBackground() {
//                NativeAssetsAdPool
//                    .load(
//                        mParentFragmentActivity,
//                        RouterCompanionAppConstants.AVOCARROT_LIST_PLACEMENT_KEY,
//                        adConfig,
//                        this@AvocarrotNativeAdTile
//                    )
////                .reloadAd()
            }
        }
    }

    override fun getLogTag() = LOG_TAG
    override fun getOnclickIntent() = null
    override fun isAdTile() = true

    override fun onLoadFinished(loader: Loader<Unit>, data: Unit) {
//        Avocarrot.build(mParentFragmentActivity,
//                NativeAssetsAd.Configuration.Builder()
//                        .setApiKey(RouterCompanionAppConstants.AVOCARROT_APIKEY)
//                        .setAdUnitId(RouterCompanionAppConstants.AVOCARROT_LIST_PLACEMENT_KEY)
//                        .setSandbox(BuildConfig.DEBUG)
//                        .setLogLevel(if (BuildConfig.DEBUG) DEBUG else WARN)
//                        .setCallback(object : NativeAssetsAdCallback() {
//                            override fun onAdError(error: AdError?) {
//                                try {
//                                    super.onAdError(error)
//                                    ReportingUtils.reportException(null,
//                                            AdUtils.AdFailedToShowEvent("Avocarrot: " + (error?.toString() ?: "")))
//                                    //Fallback to AdMob Banner Tile
//                                    val adView = layout.find<AdView>(R.id.admob_banner)
//                                    adView.visibility = View.VISIBLE
//                                    layout.find<View>(R.id.tile_native_ad_container).hide()
//                                    AdUtils.buildAndDisplayAdViewIfNeeded(mParentFragmentActivity, adView)
//                                } finally {
//                                    doneWithLoaderInstance(this@AvocarrotNativeAdTile, loader)
//                                }
//                            }
//
//                            override fun onAdLoaded(nativeAssetsAd: NativeAssetsAd?, ads: MutableList<NativeAssets>?) {
//                                super.onAdLoaded(nativeAssetsAd, ads)
//                                if (ads == null || ads.isEmpty()) {
//                                    this.onAdError(AdError.GENERIC)
//                                    return
//                                }
//
//                                try {
//                                    val avocarrotContainerLayout = layout.find<View>(R.id.tile_native_ad_container)
//                                    avocarrotContainerLayout.show()
//                                    layout.find<View>(R.id.admob_banner).hide()
//
//                                    val ad = ads[0]
//
//                                    /* Get References to the UI Components that will draw the Native Ad */
//                                    val title = layout.find<TextView>(R.id.tile_native_ad_headline)
//                                    val description = layout.find<TextView>(R.id.tile_native_ad_description)
//                                    val imageIconView = layout.find<ImageView>(R.id.tile_native_ad_image_view)
//                                    val ratingImageView = layout.find<ImageView>(R.id.tile_native_ad_rating_image_view)
//                                    val button = layout.find<Button>(R.id.tile_native_ad_button)
//
//                                    title.text = ad.title
//                                    button.text = ad.callToAction
//                                    description.text = ad.text
//                                    Utils.downloadImageFromUrl(mParentFragmentActivity, ad.ratingImageUrl,
//                                            ratingImageView, null, null,
//                                            object : Callback {
//                                                override fun onSuccess() {}
//
//                                                override fun onError() {
//                                                    mParentFragmentActivity.runOnUiThread {
//                                                        ratingImageView.invisible()
//                                                    }
//                                                }
//                                            })
//
//                                    val adLayout =
//                                            AdLayout.BuilderWithView(avocarrotContainerLayout)
//                                                    .setTitle(title)
//                                                    .setText(description)
//                                                    .setCallToAction(button)
//                                                    .setIcon(imageIconView)
////                      .setMediaContainer(mediaContainer)
////                      .setAdChoices(adChoices)
//                                                    .addClickableView(title)
//                                                    .addClickableView(button)
//                                                    .build()
//                                    nativeAssetsAd?.bindView(adLayout, ad)
//                                } finally {
//                                    doneWithLoaderInstance(this@AvocarrotNativeAdTile, loader)
//                                }
//                            }
//                        }))
//                .loadAd()
    }

//    override fun onAdFailed(nativeAssetsAd: NativeAssetsAd, responseStatus: ResponseStatus) {
//        Crashlytics.log(Log.DEBUG, LOG_TAG, "onAdFailed. ResponseStatus=$responseStatus")
//    }
//
//    override fun onAdClicked(nativeAssetsAd: NativeAssetsAd) {
//        Crashlytics.log(Log.DEBUG, LOG_TAG, "onAdClicked")
//    }
//
//    override fun onAdLoaded(nativeAssetsAd: NativeAssetsAd, nativeAssets: NativeAssets) {
//        Crashlytics.log(Log.DEBUG, LOG_TAG, "onAdLoaded")
//
//        val avocarrotContainerLayout = layout.find<View>(R.id.tile_native_ad_container)
//        avocarrotContainerLayout.show()
//        layout.find<View>(R.id.admob_banner).hide()
//
//        val clickableViews: MutableList<View> = mutableListOf()
//        val titleView = layout.find<TextView>(R.id.tile_native_ad_headline)
//        val bodyView = layout.find<TextView>(R.id.tile_native_ad_description)
//        val iconView = layout.find<ImageView>(R.id.tile_native_ad_image_view)
//        val imageView = layout.find<ImageView>(R.id.tile_native_ad_rating_image_view)
//        val button = layout.find<Button>(R.id.tile_native_ad_button)
//
//        titleView.text = nativeAssets.text
//        clickableViews.add(titleView)
//
//        bodyView.text = nativeAssets.text
//
//        iconView.setImageDrawable(nativeAssets.icon?.drawable)
//        clickableViews.add(iconView)
//
//        imageView.setImageDrawable(nativeAssets.image?.drawable)
//        clickableViews.add(imageView)
//
////        final TextView ctaView = ...
////        final ImageView adChoiceImageView = ...
////        final TextView adChoiceTextView = ...
//
//
////        if (ctaView != null) {
////            ctaView.setText(nativeAssets.getCallToAction());
////            clickableViews.add(ctaView);
////        }
////        final AdChoice adChoice = nativeAssets.getAdChoice();
////        if (adChoice != null) {
////            if (adChoiceImageView != null) {
////                adChoiceImageView.setImageDrawable(adChoice.getIcon().getDrawable());
////                nativeAssetsAd.registerAdChoiceViewForClick(adChoiceImageView);
////            }
////            if (adChoiceTextView != null) {
////                adChoiceTextView.setText(adChoice.getIconCaption());
////                nativeAssetsAd.registerAdChoiceViewForClick(adChoiceTextView);
////            }
////        } else {
////            adChoiceImageView.setImageDrawable(null);
////            adChoiceTextView.setText(null);
////        }
//        nativeAssetsAd.registerViewsForClick(clickableViews);
//        nativeAssetsAd.registerViewForImpression(avocarrotContainerLayout)
//    }
//
//    override fun onAdOpened(nativeAssetsAd: NativeAssetsAd) {
//        Crashlytics.log(Log.DEBUG, LOG_TAG, "onAdOpened")
//    }

    companion object {
        private val LOG_TAG = AvocarrotNativeAdTile::class.java.simpleName

//        private val adConfig = NativeAssetsConfig.Builder()
//            .prefetchIcon(true)
//            .prefetchImage(true)
//            .prefetchAdChoiceIcon(true)
    }
}
