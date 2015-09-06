package org.rm3l.ddwrt.tiles.services.vpn.server;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;

/**
 * Created by rm3l on 05/09/15.
 */
public class OpenVPNServerTile extends DDWRTTile<NVRAMInfo> {

    public OpenVPNServerTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        //FIXME
        super(parentFragment, arguments, router, R.layout.tile_services_openvpn_client, R.id.tile_services_openvpn_client_togglebutton);
    }

    @Override
    public int getTileHeaderViewId() {
        return 0;
    }

    @Override
    public int getTileTitleViewId() {
        return 0;
    }

    @Nullable
    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
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
    public void onLoadFinished(Loader<NVRAMInfo> loader, NVRAMInfo data) {

    }
}
