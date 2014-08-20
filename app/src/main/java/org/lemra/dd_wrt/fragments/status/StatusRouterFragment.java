package org.lemra.dd_wrt.fragments.status;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import org.jetbrains.annotations.Nullable;
import org.lemra.dd_wrt.fragments.DDWRTBaseFragment;
import org.lemra.dd_wrt.tiles.DDWRTTile;
import org.lemra.dd_wrt.tiles.status.router.StatusRouterCPUTile;
import org.lemra.dd_wrt.tiles.status.router.StatusRouterMemoryTile;
import org.lemra.dd_wrt.tiles.status.router.StatusRouterSpaceUsageTile;
import org.lemra.dd_wrt.tiles.status.router.StatusRouterStateTile;

import java.util.Arrays;
import java.util.List;

/**
 * Created by armel on 8/10/14.
 */
public class StatusRouterFragment extends DDWRTBaseFragment {

    @Nullable
    @Override
    protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
        final SherlockFragmentActivity sherlockActivity = getSherlockActivity();
        return Arrays.<DDWRTTile>asList(
                new StatusRouterStateTile(sherlockActivity, savedInstanceState, this.router),
                new StatusRouterCPUTile(sherlockActivity, savedInstanceState, this.router),
                new StatusRouterMemoryTile(sherlockActivity, savedInstanceState, this.router),
                new StatusRouterSpaceUsageTile(sherlockActivity, savedInstanceState, this.router)
        );
    }
}
