package org.lemra.dd_wrt.fragments.status;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import org.jetbrains.annotations.Nullable;
import org.lemra.dd_wrt.fragments.DDWRTBaseFragment;
import org.lemra.dd_wrt.tiles.DDWRTTile;
import org.lemra.dd_wrt.tiles.status.wan.WANConfigTile;
import org.lemra.dd_wrt.tiles.status.wan.WANTrafficTile;

import java.util.Arrays;
import java.util.List;

/**
 * Created by armel on 8/10/14.
 */
public class StatusWANFragment extends DDWRTBaseFragment {

    @Nullable
    @Override
    protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
        final SherlockFragmentActivity sherlockActivity = getSherlockActivity();
        return Arrays.<DDWRTTile>asList(
                new WANConfigTile(sherlockActivity, savedInstanceState, this.router),
                new WANTrafficTile(sherlockActivity, savedInstanceState, this.router)
        );
    }
}
