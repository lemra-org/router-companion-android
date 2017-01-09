package org.rm3l.router_companion.fragments.toolbox;

import android.os.Bundle;
import android.support.annotation.Nullable;

import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.tiles.toolbox.ToolboxTracerouteTile;

import java.util.Arrays;
import java.util.List;

public class ToolboxTracerouteFragment extends AbstractToolboxFragment {
    @Nullable
    @Override
    protected List<DDWRTTile> doGetTiles(@Nullable Bundle savedInstanceState) {
        return Arrays.<DDWRTTile>asList(new ToolboxTracerouteTile(this, savedInstanceState, this.router));
    }
}