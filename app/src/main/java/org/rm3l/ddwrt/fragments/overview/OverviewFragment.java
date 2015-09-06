package org.rm3l.ddwrt.fragments.overview;

import android.os.Bundle;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.fragments.AbstractBaseFragment;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.overview.NetworkTopologyMapTile;

import java.util.Collections;
import java.util.List;

/**
 * Created by rm3l on 29/08/15.
 */
public class OverviewFragment extends AbstractBaseFragment {

    @Nullable
    @Override
    protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
        return Collections.<DDWRTTile>
                singletonList(new NetworkTopologyMapTile(this, savedInstanceState, this.router));
    }
}
