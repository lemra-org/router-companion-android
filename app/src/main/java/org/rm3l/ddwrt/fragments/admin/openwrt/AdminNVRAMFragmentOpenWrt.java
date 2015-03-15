package org.rm3l.ddwrt.fragments.admin.openwrt;

import android.os.Bundle;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.fragments.admin.AdminNVRAMFragment;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.admin.nvram.AdminNVRAMTile;

import java.util.Arrays;
import java.util.List;

public class AdminNVRAMFragmentOpenWrt extends AdminNVRAMFragment {

    @Nullable
    @Override
    protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
        //TODO
        return Arrays.<DDWRTTile>asList(new AdminNVRAMTile(this, savedInstanceState, this.router));
    }
}
