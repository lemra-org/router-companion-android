package org.rm3l.ddwrt.fragments.toolbox;

import android.os.Bundle;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.fragments.BaseFragment;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.toolbox.ToolboxPingTile;

import java.util.Arrays;
import java.util.List;

public class ToolboxPingFragment extends BaseFragment {
    @Nullable
    @Override
    protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
        return Arrays.<DDWRTTile>asList(new ToolboxPingTile(this, savedInstanceState, this.router));
    }
}
