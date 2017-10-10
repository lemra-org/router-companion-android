package org.rm3l.router_companion.fragments.dashboard;

import android.os.Bundle;
import android.support.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import org.rm3l.router_companion.fragments.AbstractBaseFragment;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.tiles.dashboard.bandwidth.WANTotalTrafficOverviewTile;

/**
 * Created by rm3l on 29/12/15.
 */
public class DashboardBandwidthFragment extends AbstractBaseFragment {

    @Nullable
    @Override
    protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
        return Arrays.<DDWRTTile>asList(
                new WANTotalTrafficOverviewTile(this, savedInstanceState, this.router));
    }
}
