package org.rm3l.ddwrt.tiles.overview;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;

/**
 * Created by rm3l on 29/08/15.
 */
public class NetworkTopologyMapTile extends DDWRTTile<Void> {

    public NetworkTopologyMapTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_network_map_overview,
                R.id.tile_network_map_togglebutton);
    }

    @Override
    public int getTileHeaderViewId() {
        return -1;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_network_map_title;
    }

    @Nullable
    @Override
    protected Loader<Void> getLoader(int id, Bundle args) {
        return null;
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
    }
}
