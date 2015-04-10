package org.rm3l.ddwrt.fragments.services;

import android.os.Bundle;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.fragments.AbstractBaseFragment;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.services.wol.WakeOnLanDaemonTile;

import java.util.Arrays;
import java.util.List;

/**
 * Created by rm3l on 10/04/15.
 */
public class ServicesWakeOnLanDaemonFragment extends AbstractBaseFragment {

    @Nullable
    @Override
    protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
        return Arrays.<DDWRTTile>asList(
                new WakeOnLanDaemonTile(this, savedInstanceState, this.router)
        );
    }
}
