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

import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.AdUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.Utils;

import java.util.List;

/**
 * Created by rm3l on 19/08/15.
 */
public class AvocarrotNativeAdTile extends DDWRTTile<Void> {

    public AvocarrotNativeAdTile(@NonNull Fragment parentFragment,
                                 @NonNull Bundle arguments,
                                 @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_native_ad, null);
    }

    @Override
    public int getTileHeaderViewId() {
        return -1;
    }

    @Override
    public int getTileTitleViewId() {
        return -1;
    }

    @Override
    @Nullable
    public Integer getTileBackgroundColor() {
        return mParentFragmentActivity.getResources().getColor(android.R.color.transparent);
    }

    @Nullable
    @Override
    protected Loader<Void> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<Void>(mParentFragmentActivity) {
            @Override
            public Void loadInBackground() {
                //Nothing to do
                return null;
            }
        };
    }

    @Nullable
    @Override
    protected String getLogTag() {
        return null;
    }

    @Nullable
    @Override
    protected OnClickIntent getOnclickIntent() {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Void> loader, Void data) {
        final com.avocarrot.androidsdk.AvocarrotCustom avocarrotCustom =
                new com.avocarrot.androidsdk.AvocarrotCustom(
                        mParentFragmentActivity,
                        DDWRTCompanionConstants.AVOCARROT_APIKEY,
                        DDWRTCompanionConstants.AVOCARROT_PLACEMENT_KEY
                );
        avocarrotCustom.setSandbox(BuildConfig.DEBUG);
        avocarrotCustom.setLogger(true, "ALL");

        // Setup a CustomAd Listener (required)
        avocarrotCustom.setListener(
                new com.avocarrot.androidsdk.AvocarrotCustomListener() {

                    @Override
                    public void onAdError(AdError error) {
                        Utils.reportException(
                                new AdUtils.AdFailedToShowEvent("Avocarrot: " + (error != null ? error.toString() : "")));
                        //Fallback to AdMob Banner Tile
                        final AdView adView = (AdView) layout.findViewById(R.id.admob_banner);
                        if (adView != null) {
                            adView.setVisibility(View.VISIBLE);
                            layout.findViewById(R.id.tile_native_ad_container).setVisibility(View.GONE);
                            AdUtils.buildAndDisplayAdViewIfNeeded(mParentFragmentActivity,
                                    adView);
                        }
                        super.onAdError(error);
                    }

                    @Override
                    public void onAdLoaded(List<CustomModel> ads) {
                        super.onAdLoaded(ads);
                        if ((ads == null) || (ads.size() < 1)) {
                            return;
                        }
                        final CustomModel ad = ads.get(0);

                        /* Get References to the UI Components that will draw the Native Ad */

                        final TextView title = (TextView) layout.findViewById(R.id.tile_native_ad_headline);
                        final TextView description = (TextView) layout.findViewById(R.id.tile_native_ad_description);
                        final ImageView imageView = (ImageView) layout.findViewById(R.id.tile_native_ad_image_view);
                        final Button button = (Button) layout.findViewById(R.id.tile_native_ad_button);

                        // Fill in details in your view
                        title.setText(ad.getTitle());
                        button.setText(ad.getCTAText());
                        description.setText(ad.getDescription());

                        // Load the advertisement's creative into your ImageView
                        avocarrotCustom.loadImage(ad, imageView);

                        // Bind view
                        avocarrotCustom.bindView(ad, layout);

                        // Set click listener
                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                avocarrotCustom.handleClick(ad);
                            }
                        });
                    }
                });
        // Load the ads(s)
        avocarrotCustom.loadAd();
    }

}
