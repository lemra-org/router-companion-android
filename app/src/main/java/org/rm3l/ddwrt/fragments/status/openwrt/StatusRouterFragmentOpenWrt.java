package org.rm3l.ddwrt.fragments.status.openwrt;

import android.os.Bundle;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.fragments.status.StatusRouterFragment;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.status.router.StatusRouterCPUTile;
import org.rm3l.ddwrt.tiles.status.router.StatusRouterMemoryTile;
import org.rm3l.ddwrt.tiles.status.router.StatusRouterSpaceUsageTile;
import org.rm3l.ddwrt.tiles.status.router.StatusRouterStateTile;

import java.util.Arrays;
import java.util.List;

public class StatusRouterFragmentOpenWrt extends StatusRouterFragment {

    @Nullable
    @Override
    protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
        return Arrays.<DDWRTTile>asList(
                new StatusRouterStateTile(this, savedInstanceState, this.router),
                new StatusRouterCPUTile(this, savedInstanceState, this.router),
                new StatusRouterMemoryTile(this, savedInstanceState, this.router),
                new StatusRouterSpaceUsageTile(this, savedInstanceState, this.router)
        );
    }
}
