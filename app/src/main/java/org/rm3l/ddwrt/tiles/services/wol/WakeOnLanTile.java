package org.rm3l.ddwrt.tiles.services.wol;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.resources.Device;
import org.rm3l.ddwrt.resources.None;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.utils.ColorUtils;

import java.util.HashSet;
import java.util.Set;

public class WakeOnLanTile extends DDWRTTile<None> {

    private static final String LOG_TAG = WakeOnLanTile.class.getSimpleName();
    private final Set<Device> mDevices = new HashSet<>();
    private boolean isThemeLight;

    public WakeOnLanTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        //TODO
        super(parentFragment, arguments, router, R.layout.tile_status_wireless_clients,
                R.id.tile_status_wireless_clients_togglebutton);
        isThemeLight = ColorUtils.isThemeLight(mParentFragmentActivity);

    }

    @Override
    public int getTileHeaderViewId() {
        //TODO
        return -1;
    }

    @Override
    public int getTileTitleViewId() {
        //TODO
        return -1;
    }

    @Nullable
    @Override
    protected Loader<None> getLoader(int id, Bundle args) {
        return null;
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

    @Override
    public void onLoadFinished(Loader<None> loader, None data) {

    }

}
