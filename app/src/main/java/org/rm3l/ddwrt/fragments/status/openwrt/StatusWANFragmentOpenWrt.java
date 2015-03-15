package org.rm3l.ddwrt.fragments.status.openwrt;

import android.os.Bundle;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.fragments.status.StatusWANFragment;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.status.wan.WANConfigTile;
import org.rm3l.ddwrt.tiles.status.wan.WANMonthlyTrafficTile;
import org.rm3l.ddwrt.tiles.status.wan.WANTrafficTile;

import java.util.Arrays;
import java.util.List;

public class StatusWANFragmentOpenWrt extends StatusWANFragment {

    //TODO
    @Nullable
    @Override
    protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
        return Arrays.<DDWRTTile>asList(
                new WANConfigTile(this, savedInstanceState, this.router),
                new WANTrafficTile(this, savedInstanceState, this.router),
                new WANMonthlyTrafficTile(this, savedInstanceState, this.router)
        );
    }
}
