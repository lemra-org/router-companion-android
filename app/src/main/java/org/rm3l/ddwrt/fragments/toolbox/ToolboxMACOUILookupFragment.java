package org.rm3l.ddwrt.fragments.toolbox;

import android.os.Bundle;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.toolbox.ToolboxMACOUITile;

import java.util.Arrays;
import java.util.List;

public class ToolboxMACOUILookupFragment extends AbstractToolboxFragment {

    @Nullable
    @Override
    protected List<DDWRTTile> doGetTiles(@Nullable Bundle savedInstanceState) {
        return Arrays.<DDWRTTile>asList(new ToolboxMACOUITile(this, savedInstanceState, this.router));
    }
}

