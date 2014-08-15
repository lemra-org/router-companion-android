package org.lemra.dd_wrt.fragments.status;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.common.collect.Lists;

import org.jetbrains.annotations.Nullable;
import org.lemra.dd_wrt.fragments.DDWRTBaseFragment;
import org.lemra.dd_wrt.tiles.DDWRTTile;
import org.lemra.dd_wrt.tiles.status.StatusRouterCPUTile;
import org.lemra.dd_wrt.tiles.status.StatusRouterMemoryTile;
import org.lemra.dd_wrt.tiles.status.StatusRouterSpaceUsageTile;
import org.lemra.dd_wrt.tiles.status.StatusRouterStateTile;

import java.util.List;

/**
 * Created by armel on 8/10/14.
 */
public class StatusRouterFragment extends DDWRTBaseFragment {

    private final List<DDWRTTile> tiles = Lists.newArrayListWithCapacity(2);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Important to clear everything here!
        if (tiles.isEmpty()) {
            final SherlockFragmentActivity sherlockActivity = getSherlockActivity();
            final Bundle arguments = getArguments();
            tiles.add(new StatusRouterStateTile(sherlockActivity, arguments));
            tiles.add(new StatusRouterCPUTile(sherlockActivity, arguments));
            tiles.add(new StatusRouterMemoryTile(sherlockActivity, arguments));
            tiles.add(new StatusRouterSpaceUsageTile(sherlockActivity, arguments));
        }

    }

    @Nullable
    @Override
    protected List<DDWRTTile> getTiles() {
        return this.tiles;
    }
}
