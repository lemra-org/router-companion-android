package org.rm3l.ddwrt.tiles;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;

import com.google.android.gms.ads.AdView;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.AdUtils;

/**
 * Created by rm3l on 16/08/15.
 */
public class BannerAdTile extends DDWRTTile<Void> {

    public BannerAdTile(@NonNull Fragment parentFragment,
                        @NonNull Bundle arguments,
                        @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_adview, null);
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

    @Override
    public boolean isAdTile() {
        return true;
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
        AdUtils.buildAndDisplayAdViewIfNeeded(mParentFragmentActivity,
                (AdView) layout.findViewById(R.id.router_main_activity_tile_adView));
    }
}
