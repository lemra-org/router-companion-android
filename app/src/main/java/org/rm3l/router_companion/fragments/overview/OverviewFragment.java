package org.rm3l.router_companion.fragments.overview;

import android.os.Bundle;
import androidx.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import org.rm3l.router_companion.fragments.AbstractBaseFragment;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.tiles.dashboard.bandwidth.WANTotalTrafficOverviewTile;
import org.rm3l.router_companion.tiles.dashboard.network.NetworkTopologyMapTile;
import org.rm3l.router_companion.tiles.dashboard.system.UptimeTile;

/**
 * Created by rm3l on 29/08/15.
 */
public class OverviewFragment extends AbstractBaseFragment {

    @Nullable
    @Override
    protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
        return Arrays.<DDWRTTile>asList(new UptimeTile(this, savedInstanceState, this.router),
                new WANTotalTrafficOverviewTile(this, savedInstanceState, this.router),
                new NetworkTopologyMapTile(this, savedInstanceState, this.router));
    }
}
