package org.rm3l.ddwrt.fragments.status.openwrt;

import android.os.Bundle;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.fragments.status.StatusRouterFragment;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.status.router.StatusRouterCPUTile;
import org.rm3l.ddwrt.tiles.status.router.StatusRouterMemoryTile;
import org.rm3l.ddwrt.tiles.status.router.openwrt.StatusRouterSpaceUsageTileOpenWrt;
import org.rm3l.ddwrt.tiles.status.router.openwrt.StatusRouterStateTileOpenWrt;

import java.util.Arrays;
import java.util.List;

public class StatusRouterFragmentOpenWrt extends StatusRouterFragment {

    @Nullable
    @Override
    protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
        return Arrays.<DDWRTTile>asList(
                new StatusRouterStateTileOpenWrt(this, savedInstanceState, this.router),
                new StatusRouterCPUTile(this, savedInstanceState, this.router),
                new StatusRouterMemoryTile(this, savedInstanceState, this.router),
                new StatusRouterSpaceUsageTileOpenWrt(this, savedInstanceState, this.router)
        );
    }
}
