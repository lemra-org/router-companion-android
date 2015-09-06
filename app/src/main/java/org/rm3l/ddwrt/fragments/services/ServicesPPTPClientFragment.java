package org.rm3l.ddwrt.fragments.services;

import android.os.Bundle;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.fragments.AbstractBaseFragment;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.services.vpn.client.PPTPClientTile;

import java.util.Collections;
import java.util.List;

/**
 * Created by rm3l on 06/09/15.
 */
public class ServicesPPTPClientFragment extends AbstractBaseFragment {

    @Nullable
    @Override
    protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
        return Collections.<DDWRTTile> singletonList(
                new PPTPClientTile(this, savedInstanceState, this.router));
    }
}
