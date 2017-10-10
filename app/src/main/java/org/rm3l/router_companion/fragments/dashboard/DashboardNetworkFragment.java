package org.rm3l.router_companion.fragments.dashboard;

import android.os.Bundle;
import android.support.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import org.rm3l.router_companion.fragments.AbstractBaseFragment;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.tiles.dashboard.network.NetworkTopologyMapTile;
import org.rm3l.router_companion.tiles.dashboard.network.PublicIPGeoTile;

/**
 * Created by rm3l on 29/12/15.
 */
public class DashboardNetworkFragment extends AbstractBaseFragment {

    @Nullable
    @Override
    protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
        return Arrays.<DDWRTTile>asList(
                new NetworkTopologyMapTile(this, savedInstanceState, this.router),
                new PublicIPGeoTile(this, savedInstanceState, this.router));
    }
}
