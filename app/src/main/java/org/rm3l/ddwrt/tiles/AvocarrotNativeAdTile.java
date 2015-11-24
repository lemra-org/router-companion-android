package org.rm3l.ddwrt.tiles;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.avocarrot.androidsdk.AdError;
import com.avocarrot.androidsdk.CustomModel;
import com.google.android.gms.ads.AdView;
import com.squareup.picasso.Callback;

import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.AdUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.ReportingUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.util.List;

/**
 * Created by rm3l on 19/08/15.
 */
public class AvocarrotNativeAdTile extends DDWRTTile<Void> {

    private static final String LOG_TAG = AvocarrotNativeAdTile.class.getSimpleName();

//    private final com.avocarrot.androidsdk.AvocarrotInterstitial mAvocarrotInterstitial;

    public AvocarrotNativeAdTile(@NonNull Fragment parentFragment,
                                 @NonNull Bundle arguments,
                                 @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_native_ad, null);
//        this.mAvocarrotInterstitial =
//                new com.avocarrot.androidsdk.AvocarrotInterstitial(
//                        mParentFragmentActivity,                     /* reference to your Activity */
//                        DDWRTCompanionConstants.AVOCARROT_APIKEY, /* this is your Avocarrot API Key */
//                        DDWRTCompanionConstants.AVOCARROT_INTERSTITIAL_PLACEMENT_KEY /* this is your Avocarrot Placement Key */
//                );
//        this.mAvocarrotInterstitial.setSandbox(BuildConfig.DEBUG);
//        this.mAvocarrotInterstitial.setLogger(true, "ALL");
    }

    @Override
    public int getTileHeaderViewId() {
        return R.id.tile_native_ad_hdr;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_native_ad_headline;
    }

    @Nullable
    @Override
    protected Loader<Void> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<Void>(mParentFragmentActivity) {
            @Override
            public Void loadInBackground() {

//                 Preload ad
//                mAvocarrotInterstitial.loadAd();

                //Nothing to do
                return null;
            }
        };
    }

    @Nullable
    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Nullable
    @Override
    protected OnClickIntent getOnclickIntent() {
//        mAvocarrotInterstitial.showAd();
        return null;
    }

    @Override
    public boolean isAdTile() {
        return true;
    }

    @Override
    public void onLoadFinished(final Loader<Void> loader, Void data) {
        final com.avocarrot.androidsdk.AvocarrotCustom avocarrotCustom =
                new com.avocarrot.androidsdk.AvocarrotCustom(
                        mParentFragmentActivity,
                        DDWRTCompanionConstants.AVOCARROT_APIKEY,
                        DDWRTCompanionConstants.AVOCARROT_LIST_PLACEMENT_KEY
                );
        avocarrotCustom.setSandbox(BuildConfig.DEBUG);
        avocarrotCustom.setLogger(true, "ALL");

        // Setup a CustomAd Listener (required)
        avocarrotCustom.setListener(
                new com.avocarrot.androidsdk.AvocarrotCustomListener() {

                    @Override
                    public void onAdError(AdError error) {
                        try {
                            super.onAdError(error);
                            ReportingUtils.reportException(
                                    null, new AdUtils.AdFailedToShowEvent("Avocarrot: " + (error != null ? error.toString() : "")));
                            //Fallback to AdMob Banner Tile
                            final AdView adView = (AdView) layout.findViewById(R.id.admob_banner);
                            if (adView != null) {
                                adView.setVisibility(View.VISIBLE);
                                layout.findViewById(R.id.tile_native_ad_container).setVisibility(View.GONE);
                                AdUtils.buildAndDisplayAdViewIfNeeded(mParentFragmentActivity,
                                        adView);
                            }
                        } finally {
                            doneWithLoaderInstance(AvocarrotNativeAdTile.this, loader);
                        }
                    }

                    @Override
                    public void onAdLoaded(List<CustomModel> ads) {
                        super.onAdLoaded(ads);
                        if ((ads == null) || (ads.size() < 1)) {
                            this.onAdError(AdError.GENERIC);
                            return;
                        }

                        try {
                            final View avocarrotContainerLayout = layout.findViewById(R.id.tile_native_ad_container);
                            avocarrotContainerLayout.setVisibility(View.VISIBLE);
                            layout.findViewById(R.id.admob_banner).setVisibility(View.GONE);

                            final CustomModel ad = ads.get(0);

                        /* Get References to the UI Components that will draw the Native Ad */

                            final TextView title = (TextView) layout.findViewById(R.id.tile_native_ad_headline);
                            final TextView description = (TextView) layout.findViewById(R.id.tile_native_ad_description);
                            final ImageView imageIconView = (ImageView) layout.findViewById(R.id.tile_native_ad_image_view);
                            final ImageView ratingImageView = (ImageView) layout.findViewById(R.id.tile_native_ad_rating_image_view);
                            final Button button = (Button) layout.findViewById(R.id.tile_native_ad_button);

                            // Fill in details in your view
                            title.setText(ad.getTitle());
                            button.setText(ad.getCTAText());
                            description.setText(ad.getDescription());
                            Utils.downloadImageFromUrl(mParentFragmentActivity,
                                    ad.getRatingImageUrl(),
                                    ratingImageView,
                                    null,
                                    null,
                                    new Callback() {
                                        @Override
                                        public void onSuccess() {

                                        }

                                        @Override
                                        public void onError() {
                                            mParentFragmentActivity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ratingImageView.setVisibility(View.INVISIBLE);
                                                }
                                            });
                                        }
                                    });

                            // Load the advertisement's creative into your ImageView
                            avocarrotCustom.loadIcon(ad, imageIconView);

                            // Bind view
                            avocarrotCustom.bindView(ad, layout);

                            // Set click listener
                            final View.OnClickListener clickListener = new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    avocarrotCustom.handleClick(ad);
                                }
                            };
                            button.setOnClickListener(clickListener);
                            avocarrotContainerLayout.setOnClickListener(clickListener);

                        } finally {
                            doneWithLoaderInstance(AvocarrotNativeAdTile.this, loader);
                        }
                    }
                });
        // Load the ads(s)
        avocarrotCustom.loadAd();
    }

}
