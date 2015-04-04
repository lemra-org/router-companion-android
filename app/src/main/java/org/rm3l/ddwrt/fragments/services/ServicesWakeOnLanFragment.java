package org.rm3l.ddwrt.fragments.services;

import android.os.Bundle;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.fragments.AbstractBaseFragment;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.services.wol.WakeOnLanTile;

import java.util.Arrays;
import java.util.List;

public class ServicesWakeOnLanFragment extends AbstractBaseFragment {
    @Nullable
    @Override
    protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
        return Arrays.<DDWRTTile>asList(
                new WakeOnLanTile(this, savedInstanceState, this.router)
        );
    }
}
