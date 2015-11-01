package org.rm3l.ddwrt.fragments.toolbox;

import android.os.Bundle;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.fragments.AbstractBaseFragment;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.toolbox.AbstractToolboxTile;

import java.util.List;

/**
 * Created by rm3l on 01/11/15.
 */
public abstract class AbstractToolboxFragment extends AbstractBaseFragment {

    @Nullable
    protected List<DDWRTTile> tiles = null;

    @Nullable
    @Override
    protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
        if (tiles == null) {
            tiles = doGetTiles(savedInstanceState);
        }
        return tiles;
    }

    @Override
    protected boolean canChildScrollUp() {
        final List<DDWRTTile> tiles = this.getTiles(null);
        if (tiles == null || tiles.isEmpty()) {
            return false;
        }
        final DDWRTTile tile = tiles.get(0);
        return (tile instanceof AbstractToolboxTile &&
                ((AbstractToolboxTile) tile).canChildScrollUp());
    }

    protected abstract List<DDWRTTile> doGetTiles(@Nullable Bundle savedInstanceState);

}
