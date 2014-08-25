package org.lemra.dd_wrt.fragments.status;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import org.jetbrains.annotations.Nullable;
import org.lemra.dd_wrt.fragments.DDWRTBaseFragment;
import org.lemra.dd_wrt.tiles.DDWRTTile;
import org.lemra.dd_wrt.tiles.status.lan.DHCPStatusTile;

import java.util.Arrays;
import java.util.List;

/**
 * Created by armel on 8/25/14.
 */
public class StatusDHCPFragment extends DDWRTBaseFragment {

    @Nullable
    @Override
    protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
        final SherlockFragmentActivity sherlockActivity = getSherlockActivity();
        return Arrays.<DDWRTTile>asList(
                new DHCPStatusTile(sherlockActivity, savedInstanceState, this.router)
        );
    }
}
