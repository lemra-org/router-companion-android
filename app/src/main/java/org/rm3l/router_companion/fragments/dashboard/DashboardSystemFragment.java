package org.rm3l.router_companion.fragments.dashboard;

import android.os.Bundle;
import android.support.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import org.rm3l.router_companion.fragments.AbstractBaseFragment;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.tiles.dashboard.system.MemoryAndCpuUsageTile;
import org.rm3l.router_companion.tiles.dashboard.system.StorageUsageTile;
import org.rm3l.router_companion.tiles.dashboard.system.UptimeTile;

/**
 * Created by rm3l on 29/12/15.
 */
public class DashboardSystemFragment extends AbstractBaseFragment {

    @Nullable
    @Override
    protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
        return Arrays.<DDWRTTile>asList(new UptimeTile(this, savedInstanceState, this.router),
                new MemoryAndCpuUsageTile(this, savedInstanceState, this.router),
                new StorageUsageTile(this, savedInstanceState, this.router));
    }
}
