package org.rm3l.ddwrt.fragments.dashboard;

import android.os.Bundle;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.fragments.AbstractBaseFragment;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.dashboard.bandwidth.WANTotalTrafficOverviewTile;

import java.util.Arrays;
import java.util.List;

/**
 * Created by rm3l on 29/12/15.
 */
public class DashboardBandwidthFragment extends AbstractBaseFragment {

    @Nullable
    @Override
    protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
        return Arrays.<DDWRTTile>asList(
                new WANTotalTrafficOverviewTile(this, savedInstanceState, this.router)
        );
    }
}
