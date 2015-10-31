package org.rm3l.ddwrt.tiles;

//import com.adsdk.sdk.nativeads.NativeAd;
//import com.adsdk.sdk.nativeads.NativeAdListener;
//import com.adsdk.sdk.nativeads.NativeAdManager;
//import com.adsdk.sdk.nativeads.NativeViewBinder;
//import com.google.android.gms.ads.AdView;

/**
 * Created by rm3l on 19/08/15.
 */
public class MobFoxNativeAdTile {
//        extends DDWRTTile<Void> {
//
//    private final Queue<NativeAd> nativeAdQueue = new LinkedList<>();
//
//    public MobFoxNativeAdTile(@NonNull Fragment parentFragment,
//                        @NonNull Bundle arguments,
//                        @Nullable Router router) {
//        super(parentFragment, arguments, router, R.layout.empty_view, null);
//    }
//
//    @Override
//    public int getTileHeaderViewId() {
//        return -1;
//    }
//
//    @Override
//    public int getTileTitleViewId() {
//        return -1;
//    }
//
//    @Override
//    @Nullable
//    public Integer getTileBackgroundColor() {
//        return mParentFragmentActivity.getResources().getColor(android.R.color.transparent);
//    }
//
//    @Nullable
//    @Override
//    protected Loader<Void> getLoader(int id, Bundle args) {
//        return new AsyncTaskLoader<Void>(mParentFragmentActivity) {
//            @Override
//            public Void loadInBackground() {
//                //Nothing to do
//                return null;
//            }
//        };
//    }
//
//    @Nullable
//    @Override
//    protected String getLogTag() {
//        return null;
//    }
//
//    @Nullable
//    @Override
//    protected OnClickIntent getOnclickIntent() {
//        return null;
//    }
//
//    @Override
//    public void onLoadFinished(Loader<Void> loader, Void data) {
//        final NativeViewBinder mobFoxNativeViewBinder = AdUtils.getMobFoxNativeViewBinder();
//
//        final NativeAdManager nativeAdManager = AdUtils
//                .requestMobFoxNativeAdManager(mParentFragmentActivity, new NativeAdListener() {
//                    @Override
//                    public void adLoaded(NativeAd nativeAd) {
//                        nativeAdQueue.add(nativeAd);
//                    }
//
//                    @Override
//                    public void adFailedToLoad() {
//
//                    }
//
//                    @Override
//                    public void impression() {
//
//                    }
//
//                    @Override
//                    public void adClicked() {
//
//                    }
//                });
//
//        final NativeAd nativeAd = nativeAdQueue.poll();
//        if (nativeAd == null) {
//            return;
//        }
//        nativeAdManager.getNativeAdView(nativeAd, mobFoxNativeViewBinder);
//
//    }

//    @Override
//    public boolean isAdTile() {
//        return true;
//    }
}
