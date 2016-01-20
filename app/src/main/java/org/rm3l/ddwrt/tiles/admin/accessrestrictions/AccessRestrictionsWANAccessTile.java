package org.rm3l.ddwrt.tiles.admin.accessrestrictions;

import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Display;
import android.widget.TextView;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.resources.None;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.widgets.RecyclerViewEmptySupport;

/**
 * Created by rm3l on 20/01/16.
 */
public class AccessRestrictionsWANAccessTile extends DDWRTTile<None> {

    private static final String LOG_TAG =
            AccessRestrictionsWANAccessTile.class.getSimpleName();

    private RecyclerViewEmptySupport mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    public AccessRestrictionsWANAccessTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_admin_access_restrictions_wan_access, null);

        mRecyclerView = (RecyclerViewEmptySupport) layout.findViewById(R.id.tile_admin_access_restrictions_wan_access_ListView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        // allows for optimizations if all items are of the same size:
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(mParentFragmentActivity,
                LinearLayoutManager.VERTICAL, false);
        mLayoutManager.scrollToPosition(0);
        mRecyclerView.setLayoutManager(mLayoutManager);
//
        final TextView emptyView = (TextView) layout.findViewById(R.id.empty_view);
        if (ColorUtils.isThemeLight(mParentFragmentActivity)) {
            emptyView.setTextColor(ContextCompat.getColor(mParentFragmentActivity, R.color.black));
        } else {
            emptyView.setTextColor(ContextCompat.getColor(mParentFragmentActivity, R.color.white));
        }
        mRecyclerView.setEmptyView(emptyView);

////TODO

//        // specify an adapter (see also next example)
//        mAdapter = new NVRAMDataRecyclerViewAdapter(mParentFragmentActivity, router, mNvramInfoDefaultSorting);
//        mRecyclerView.setAdapter(mAdapter);
//
        final Display display = mParentFragmentActivity
                .getWindowManager()
                .getDefaultDisplay();
        final Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        Log.d(LOG_TAG, "<width,height> = <" + width + "," + height + ">");
        mRecyclerView.setMinimumHeight(size.y);

    }

    public boolean canChildScrollUp() {
        final boolean canScrollVertically = ViewCompat
                .canScrollVertically(mRecyclerView, -1);
        if (!canScrollVertically) {
            return canScrollVertically;
        }

        //TODO ScrollView can scroll vertically,
        // but detect whether the touch was done outside of the scroll view
        // (in which case we should return false)

        return canScrollVertically;
    }

    @Override
    public boolean isEmbeddedWithinScrollView() {
        return false;
    }

    @Override
    public int getTileHeaderViewId() {
        return R.id.tile_admin_access_restrictions_wan_access_hdr;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_admin_access_restrictions_wan_access_title;
    }

    @Nullable
    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Nullable
    @Override
    protected OnClickIntent getOnclickIntent() {
        return null;
    }

    @Nullable
    @Override
    protected Loader<None> getLoader(int id, Bundle args) {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<None> loader, None data) {

    }
}
